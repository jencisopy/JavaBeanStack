/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*        jenciso@javabeanstack.org
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 3 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
* MA 02110-1301  USA
 */
package org.javabeanstack.sifen;

import java.io.InputStream;
import java.util.List;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.exceptions.ErrorCategory;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.http.BatchChecks;
import org.javabeanstack.sifen.simulator.SifenSimulator;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * CA5-08 por la fachada (plan JBSSIF, §5.5; M3-16, M3-27): un lote que mezcla tipos de documento o
 * emisores se rechaza en {@code ISifenClient.sendBatch} con {@code JBS-ENT-09}, sin ningún pedido al
 * simulador de SIFEN. El control mismo está en {@link BatchChecks} y lo prueba {@code LoteCicloDeVidaTest};
 * acá se prueba que {@code sendBatch} lo aplica antes de transmitir.
 */
public class LoteMezcladoSendBatchTest {
    private static SifenSimulator sim;

    @BeforeAll
    public static void levantar() throws Exception {
        sim = SifenSimulator.start();
    }

    @AfterAll
    public static void bajar() {
        if (sim != null) {
            sim.close();
        }
    }

    @BeforeEach
    public void limpiar() {
        sim.reset();
    }

    /** Cliente del simulador sin la validación previa (el control de lote no depende de ella). */
    private static ISifenClient cliente(SifenConfig cfg) throws SifenException {
        cfg.setPreValidationEnabled(false);
        return SifenClient.create(cfg);
    }

    private static DocumentoElectronico caso(String id) {
        return CasosDePrueba.documentos().stream().filter(c -> c.getId().equals(id)).findFirst().orElseThrow().construir();
    }

    private static void rechazadoSinPedidos(ISifenClient c, List<SignedDocument> lote, String esperado) {
        SifenException e = Assertions.assertThrows(SifenException.class, () -> c.sendBatch(lote));
        Assertions.assertEquals("JBS-ENT-09", e.getCode(), e.getMessage());
        Assertions.assertEquals(ErrorCategory.INPUT, e.getCategory());
        Assertions.assertTrue(e.getUserMessage().contains(esperado), e.getUserMessage());
        Assertions.assertFalse(e.isOutcomeUnknown(), "rechazo local: no salió nada");
        Assertions.assertTrue(sim.requests().isEmpty(), "ningún pedido al simulador: " + sim.requests());
    }

    @Test
    public void tiposMezcladosSeRechazanSinPedidos() throws Exception {
        try (ISifenClient c = cliente(sim.config())) {
            SignedDocument factura = c.sign(caso("DE-05"));
            SignedDocument notaCredito = c.sign(caso("DE-21"));
            Assertions.assertNotEquals(BatchChecks.typeOf(factura.getCdc()), BatchChecks.typeOf(notaCredito.getCdc()));
            rechazadoSinPedidos(c, List.of(factura, notaCredito), "documentos 1 y 2: tipo 01 y 05");
        }
    }

    @Test
    public void emisoresMezcladosSeRechazanSinPedidos() throws Exception {
        SifenConfig b;
        try (InputStream in = LoteMezcladoSendBatchTest.class.getClassLoader()
                .getResourceAsStream("certificados/jbs-sifen-empresa-b.pfx")) {
            Assertions.assertNotNull(in);
            b = sim.config("empresa-b", in.readAllBytes(), SifenSimulator.PASSWORD);
        }
        try (ISifenClient a = cliente(sim.config()); ISifenClient otra = cliente(b)) {
            SignedDocument propio = a.sign(caso("DE-05"));
            DocumentoElectronico ajeno = caso("DE-05");
            ajeno.getgDatGralOpe().getgEmis().setdRucEm("80999901");
            ajeno.getgDatGralOpe().getgEmis().setdDVEmi("3");
            SignedDocument deOtraEmpresa = otra.sign(ajeno);
            Assertions.assertEquals("80999901", BatchChecks.issuerOf(deOtraEmpresa.getCdc()));
            rechazadoSinPedidos(a, List.of(propio, deOtraEmpresa), "RUC 80123456 y 80999901");
        }
    }
}

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
package org.javabeanstack.sifen.core.validation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.javabeanstack.sifen.Sifen;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.ValidezFirmaDigital;
import org.javabeanstack.sifen.internal.cert.CertificateStore;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * JBS-CERT-11 da el mismo veredicto que la verificación de firma de la librería (plan JBSSIF, I3-59,
 * ESC-13): para cada caso de {@code CoincidenciaEmisorCertificadoTest} (más el certificado autofirmado
 * de los fixtures) se firma el DE con ese certificado, con la validación previa apagada para que llegue
 * a firmarse, y se compara {@code ConfigValidator.validateConsistency} con
 * {@code Sifen.validarFirmaDE}: el prechequeo rechaza si y solo si la firma no coincide con el emisor
 * (o, sin ningún número en el certificado, {@code JBS-FIRMA-05}).
 */
public class Cert11IgualQueLaFirmaTest {
    private static final ConfigValidator V = ConfigValidator.standard();

    @TempDir
    Path dir;

    @BeforeEach
    @AfterEach
    public void vaciarAlmacen() {
        CertificateStore.clear();
    }

    @ParameterizedTest(name = "{0}: emisor {1}-{2}")
    @CsvSource({
        "jbs-sifen-autofirmado.pfx, 80123456, 5",
        "jbs-sifen-autofirmado.pfx, 80123456, 4",
        "jbs-sifen-apoderado.p12, 80123456, 5",
        "jbs-sifen-apoderado.p12, 80123456, 4",
        "jbs-sifen-apoderado.p12, 1234567, 9",
        "jbs-sifen-apoderado.p12, 1234567, 0",
        "jbs-sifen-apoderado-varios.p12, 80123456, 5",
        "jbs-sifen-apoderado-varios.p12, 80123456, 9",
        "jbs-sifen-apoderado-varios.p12, 80999999, 1",
        "jbs-sifen-apoderado-varios.p12, 80999999, 2",
        "jbs-sifen-apoderado-varios.p12, 80654321, 3",
        "jbs-sifen-apoderado.p12, 80654321, 3",
        "jbs-sifen-sin-ruc.p12, 80123456, 5",
        "jbs-sifen-sin-ruc.p12, 1234567, 9",
        "jbs-sifen-sin-ruc.p12, 1234567, 0",
        "jbs-sifen-sin-firma.p12, 80123456, 5"
    })
    public void elPrechequeoYLaFirmaCoinciden(String certificado, String ruc, String dv) throws Exception {
        SifenConfig cfg = FixturesSifen.configuracion();
        cfg.setBaseUrl("https://127.0.0.1:9");
        cfg.setClientCertificate(FixturesSifen.rutaPfx().resolveSibling(certificado).toString());
        DocumentoElectronico de = CasosDePrueba.documentos().get(4).construir();
        de.getgDatGralOpe().getgEmis().setdRucEm(ruc);
        de.getgDatGralOpe().getgEmis().setdDVEmi(dv);

        ValidationResult r = V.validateConsistency(de, cfg);
        boolean cert11 = r.contains("JBS-CERT-11");

        Path xml = Files.createTempFile(dir, "de", ".xml");
        Files.write(xml, de.generarXml(FixturesSifen.contexto(cfg), cfg).getBytes(StandardCharsets.UTF_8));
        ValidezFirmaDigital firma = Sifen.validarFirmaDE(xml.toFile());

        Assertions.assertEquals(!firma.isValido(), cert11,
                () -> "JBS-CERT-11 " + cert11 + " / firma " + firma.isValido() + " (" + firma.getMotivoInvalidez() + "): " + r);
        if (!firma.isValido()) {
            // la firma en sí es buena: lo que la invalida es solo el cotejo del emisor
            Assertions.assertTrue("JBS-FIRMA-05".equals(firma.getCode())
                    || firma.getMotivoInvalidez().contains("no coincide"), firma::getMotivoInvalidez);
        }
    }
}

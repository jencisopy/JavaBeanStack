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
package org.javabeanstack.sifen.test.golden;

import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.internal.request.MensajesSifen;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Golden files de equivalencia con el fork (plan JBSSIF, §2.3, D8; reglas en
 * {@code src/test/resources/golden/README.md}).
 *
 * <p>Cada golden es la salida del <b>fork</b> {@code rshk-jsifenlib-2.0.0-SNAPSHOT} capturada por el
 * arnés A/B (fuera del repo) y congelada en forma C14N exclusiva, firma incluida (certificado
 * autofirmado, A3). El test genera lo mismo con {@code jbs-sifen} y exige igualdad byte a byte.
 * Los defectos conocidos del fork quedan fijados tal cual (D10): cambiarlos exige regenerar el
 * golden con la justificación caso por caso en la bitácora de la fase.</p>
 */
public class GoldenXmlTest {
    private static SifenConfig cfg;
    private static GenerationCtx ctx;

    @BeforeAll
    public static void configurar() {
        cfg = FixturesSifen.configuracion();
        ctx = FixturesSifen.contexto(cfg);
    }

    static Stream<CasosDePrueba.Caso> documentos() {
        return CasosDePrueba.documentos().stream();
    }

    static Stream<CasosDePrueba.Caso> documentosPorObjeto() {
        return CasosDePrueba.documentos().stream().filter(CasosDePrueba.Caso::tieneConstructor);
    }

    static Stream<CasosDePrueba.CasoEvento> eventos() {
        return CasosDePrueba.eventos().stream();
    }

    @ParameterizedTest(name = "{0} desde el XML de entrada")
    @MethodSource("documentos")
    public void documentoDesdeXml(CasosDePrueba.Caso caso) throws Exception {
        String xml = caso.leer(FixturesSifen.entrada(caso)).generarXml(ctx, cfg);
        afirmarGolden(caso.getId() + ".xml", xml);
    }

    @ParameterizedTest(name = "{0} armado por objeto")
    @MethodSource("documentosPorObjeto")
    public void documentoPorObjeto(CasosDePrueba.Caso caso) throws Exception {
        String golden = FixturesSifen.existe("golden/" + caso.getId() + ".obj.xml") ? caso.getId() + ".obj.xml" : caso.getId() + ".xml";
        afirmarGolden(golden, caso.construir().generarXml(ctx, cfg));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("eventos")
    public void evento(CasosDePrueba.CasoEvento caso) throws Exception {
        afirmarGolden(caso.getId() + ".xml", MensajesSifen.mensajeEvento(caso.construir(), cfg));
    }

    @Test
    public void lote() throws Exception {
        List<DocumentoElectronico> lote = new ArrayList<>();
        for (CasosDePrueba.Caso c : CasosDePrueba.documentos()) {
            if (c.getId().equals("DE-01") || c.getId().equals("DE-05")) {
                lote.add(c.leer(FixturesSifen.entrada(c)));
            }
        }
        Assertions.assertEquals(2, lote.size());
        String contenido = MensajesSifen.contenidoLote(MensajesSifen.mensajeLote(lote, ctx, cfg));
        afirmarGolden("LT-01.xml", contenido);
    }

    /** El golden detecta un cambio de un guaraní en un ítem (sensibilidad, CA2-03). */
    @Test
    public void goldenDetectaUnCambioDeImporte() throws Exception {
        DocumentoElectronico de = CasosDePrueba.documentos().get(4).construir();
        Assertions.assertEquals("DE-05", CasosDePrueba.documentos().get(4).getId());
        de.getgDtipDE().getgCamItemList().get(0).getgValorItem().setdPUniProSer(new BigDecimal("47790"));
        byte[] esperado = FixturesSifen.recursoBytes("golden/DE-05.xml");
        Assertions.assertFalse(Arrays.equals(esperado, XmlCanonico.canonico(de.generarXml(ctx, cfg))),
                "El golden de DE-05 no detecta un cambio de importe");
    }

    static void afirmarGolden(String golden, String generado) throws Exception {
        byte[] esperado = FixturesSifen.recursoBytes("golden/" + golden);
        byte[] obtenido = XmlCanonico.canonico(generado);
        if (!Arrays.equals(esperado, obtenido)) {
            String e = new String(esperado, StandardCharsets.UTF_8);
            String o = new String(obtenido, StandardCharsets.UTF_8);
            int i = 0;
            while (i < e.length() && i < o.length() && e.charAt(i) == o.charAt(i)) {
                i++;
            }
            int desde = Math.max(0, i - 120);
            Assertions.fail("Difiere del golden " + golden + " en el carácter " + i + "\n  golden : …"
                    + e.substring(desde, Math.min(e.length(), i + 120)) + "\n  jbs    : …"
                    + o.substring(desde, Math.min(o.length(), i + 120)));
        }
    }
}

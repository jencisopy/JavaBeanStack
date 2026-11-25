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
package org.javabeanstack.sifen.test.roundtrip;

import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 * Lectura/escritura (plan JBSSIF, §2.5): el XML <b>firmado</b> que genera la librería se vuelve a
 * leer con {@code new DocumentoElectronico(xml)} y se regenera. Es el camino del consumidor que
 * guarda el XML y lo reenvía (RF8): en la línea base solo era idempotente si el DE se generó con
 * redondeo; con {@code TgTotSub(false)} cambiaban el contenido y la firma (E0-01). Desde la Fase 4 (§4.2)
 * lo es siempre.
 *
 * <p>La comparación contra los golden desde el XML de entrada está en {@code GoldenXmlTest}.</p>
 */
public class RoundTripTest {
    private static SifenConfig cfg;
    private static GenerationCtx ctx;

    @BeforeAll
    public static void configurar() {
        cfg = FixturesSifen.configuracion();
        ctx = FixturesSifen.contexto(cfg);
    }

    /**
     * Casos sin redondeo cuyo total no es múltiplo de 50 (o de 0,50): en la línea base, al releerlos volvía
     * el redondeo (E0-01). Eran DE-06, DE-26 y DE-28. Desde la Fase 4 (§4.2) el flag sobrevive a la lectura y
     * el conjunto queda vacío: todos los casos son idempotentes (CA4-01).
     */
    static final java.util.Set<String> NO_IDEMPOTENTES_E001 = java.util.Set.of();

    /** Los que eran {@link #NO_IDEMPOTENTES_E001} en la línea base (E0-01). */
    static final java.util.Set<String> SIN_REDONDEO_E001 = java.util.Set.of("DE-06", "DE-26", "DE-28");

    static Stream<CasosDePrueba.Caso> idempotentes() {
        return CasosDePrueba.documentos().stream().filter(c -> c.tieneConstructor() && !NO_IDEMPOTENTES_E001.contains(c.getId()));
    }

    static Stream<CasosDePrueba.Caso> sinRedondeo() {
        return CasosDePrueba.documentos().stream().filter(c -> SIN_REDONDEO_E001.contains(c.getId()));
    }

    /** Los de {@link #SIN_REDONDEO_E001}: releídos siguen sin redondeo y con la misma firma (§4.2; la línea base: volvía el redondeo, E0-01). */
    @ParameterizedTest(name = "{0}: sin redondeo, releído sigue sin redondeo (E0-01)")
    @MethodSource("sinRedondeo")
    public void sinRedondeoSigueSinRedondeoAlReleer(CasosDePrueba.Caso caso) throws Exception {
        String primero = caso.construir().generarXml(ctx, cfg);
        String segundo = new DocumentoElectronico(primero).generarXml(ctx, cfg);
        Assertions.assertEquals("0", XmlCanonico.texto(primero, "dRedon"));
        Assertions.assertEquals("0", XmlCanonico.texto(segundo, "dRedon"));
        Assertions.assertEquals(XmlCanonico.texto(primero, "DigestValue"), XmlCanonico.texto(segundo, "DigestValue"));
        Assertions.assertEquals(caso.construir().obtenerCDC(), new DocumentoElectronico(segundo).getId());
    }

    @ParameterizedTest(name = "{0}: generar, releer el firmado y regenerar")
    @MethodSource("idempotentes")
    public void releerElFirmadoDaLoMismo(CasosDePrueba.Caso caso) throws Exception {
        String primero = caso.construir().generarXml(ctx, cfg);
        String segundo = new DocumentoElectronico(primero).generarXml(ctx, cfg);
        Assertions.assertArrayEquals(XmlCanonico.canonico(primero), XmlCanonico.canonico(segundo));
        Assertions.assertEquals(XmlCanonico.texto(primero, "DigestValue"), XmlCanonico.texto(segundo, "DigestValue"));
    }

    @Test
    public void releerElFirmadoDeUnaEntradaDaLoMismo() throws Exception {
        CasosDePrueba.Caso c = CasosDePrueba.documentos().get(0);
        String primero = c.leer(FixturesSifen.entrada(c)).generarXml(ctx, cfg);
        String segundo = new DocumentoElectronico(primero).generarXml(ctx, cfg);
        Assertions.assertArrayEquals(XmlCanonico.canonico(primero), XmlCanonico.canonico(segundo));
    }

    /**
     * DE generado sin redondeo: al releerlo sigue sin redondeo, con los mismos totales y DigestValue (§4.2).
     * La línea base volvía a redondear ({@code dRedon} 39) y firmaba otro DE con el mismo CDC (E0-01).
     */
    @Test
    public void sinRedondeoSobreviveALaLecturaE001() throws Exception {
        CasosDePrueba.Caso c = CasosDePrueba.documentos().get(5);
        Assertions.assertEquals("DE-06", c.getId());
        String primero = c.construir().generarXml(ctx, cfg);
        String segundo = new DocumentoElectronico(primero).generarXml(ctx, cfg);
        Assertions.assertEquals("0", XmlCanonico.texto(primero, "dRedon"));
        Assertions.assertEquals("0", XmlCanonico.texto(segundo, "dRedon"));
        Assertions.assertEquals(XmlCanonico.texto(primero, "DigestValue"), XmlCanonico.texto(segundo, "DigestValue"));
        Assertions.assertEquals(c.construir().obtenerCDC(), new DocumentoElectronico(segundo).getId());
    }
}

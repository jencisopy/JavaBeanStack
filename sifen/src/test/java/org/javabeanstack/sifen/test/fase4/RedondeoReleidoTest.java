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
package org.javabeanstack.sifen.test.fase4;

import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.fields.request.de.TgTotSub;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * El flag de redondeo sobrevive a la lectura del XML y regenerar el DE releído da el mismo XML firmado
 * (plan JBSSIF, §4.2, E0-01, M0-02; CA4-01 con los XML que genera esta versión).
 *
 * <p>Incluye el caso que deja al descubierto deducir el flag de {@code dRedon} sin escala fija: USD con
 * redondeo y total múltiplo de 0,50. La línea base calculaba {@code dRedon} {@code 0.000} (escala 3) y
 * {@code dTotGralOpe} {@code 3803.500}; releído, {@code dRedon} 0 ⇒ sin redondeo ⇒ {@code 3803.50}: otro
 * digest. Lo cierra la escala fija de {@code RedondeoUtil} en moneda extranjera (§4.3, E0-03).</p>
 */
public class RedondeoReleidoTest {
    private static SifenConfig cfg;
    private static GenerationCtx ctx;

    @BeforeAll
    public static void configurar() {
        cfg = FixturesSifen.configuracion();
        ctx = FixturesSifen.contexto(cfg);
    }

    static Stream<CasosDePrueba.Caso> porObjeto() {
        return CasosDePrueba.documentos().stream().filter(CasosDePrueba.Caso::tieneConstructor);
    }

    private static CasosDePrueba.Caso caso(String id) {
        return CasosDePrueba.documentos().stream().filter(c -> c.getId().equals(id)).findFirst().orElseThrow();
    }

    /** Todos los casos por objeto, con y sin redondeo: generar, releer el firmado y regenerar da lo mismo. */
    @ParameterizedTest(name = "{0}: releer el firmado y regenerar da el mismo XML")
    @MethodSource("porObjeto")
    public void releerYRegenerarDaLoMismo(CasosDePrueba.Caso caso) throws Exception {
        String primero = caso.construir().generarXml(ctx, cfg);
        String segundo = new DocumentoElectronico(primero).generarXml(ctx, cfg);
        Assertions.assertArrayEquals(XmlCanonico.canonico(primero), XmlCanonico.canonico(segundo));
        Assertions.assertEquals(XmlCanonico.texto(primero, "DigestValue"), XmlCanonico.texto(segundo, "DigestValue"));
    }

    /** USD con redondeo y total múltiplo de 0,50 (DE-07 con el segundo ítem a 99.82: 3803,50). */
    @Test
    public void usdConRedondeoYTotalMultiploDe050() throws Exception {
        DocumentoElectronico de = caso("DE-07").construir();
        de.getgDtipDE().getgCamItemList().get(1).getgValorItem().setdPUniProSer(new BigDecimal("99.82"));
        String primero = de.generarXml(ctx, cfg);
        Assertions.assertEquals("0", XmlCanonico.texto(primero, "dRedon"));
        Assertions.assertEquals("3803.50", XmlCanonico.texto(primero, "dTotGralOpe"));
        DocumentoElectronico releido = new DocumentoElectronico(primero);
        Assertions.assertFalse(releido.getgTotSub().getRedondeo(), "dRedon 0 ⇒ sin redondeo");
        String segundo = releido.generarXml(ctx, cfg);
        Assertions.assertArrayEquals(XmlCanonico.canonico(primero), XmlCanonico.canonico(segundo));
    }

    /** El DE generado sin redondeo (DE-06) vuelve sin redondeo al releerlo; antes salía con {@code dRedon} 39. */
    @Test
    public void sinRedondeoSobreviveALaLectura() throws Exception {
        String primero = caso("DE-06").construir().generarXml(ctx, cfg);
        DocumentoElectronico releido = new DocumentoElectronico(primero);
        Assertions.assertFalse(releido.getgTotSub().getRedondeo());
        Assertions.assertEquals("0", XmlCanonico.texto(releido.generarXml(ctx, cfg), "dRedon"));
    }

    /** Con redondeo (DE-05, {@code dRedon} 39) el flag releído es {@code true}. */
    @Test
    public void conRedondeoSeLeeTrue() throws Exception {
        String primero = caso("DE-05").construir().generarXml(ctx, cfg);
        Assertions.assertEquals("39", XmlCanonico.texto(primero, "dRedon"));
        Assertions.assertTrue(new DocumentoElectronico(primero).getgTotSub().getRedondeo());
    }

    /** {@code setRedondeo}: en el grupo y en el atajo del documento; {@code null} es el valor por omisión. */
    @Test
    public void setRedondeo() throws Exception {
        TgTotSub t = new TgTotSub();
        Assertions.assertTrue(t.getRedondeo());
        t.setRedondeo(false);
        Assertions.assertFalse(t.getRedondeo());
        t.setRedondeo(null);
        Assertions.assertTrue(t.getRedondeo());
        Assertions.assertTrue(new TgTotSub(null).getRedondeo(), "I4-09: el constructor también normaliza null");

        DocumentoElectronico de = new DocumentoElectronico();
        de.setRedondeo(false);
        Assertions.assertNotNull(de.getgTotSub());
        Assertions.assertFalse(de.getgTotSub().getRedondeo());

        DocumentoElectronico conRedondeo = caso("DE-05").construir();
        conRedondeo.setRedondeo(false);
        Assertions.assertEquals("0", XmlCanonico.texto(conRedondeo.generarXml(ctx, cfg), "dRedon"));
    }

    /**
     * I4-11 e I4-14: la tasa del IVA de valor entero se guarda y se escribe sin decimales ni exponente —{@code 10},
     * {@code 10.00} y {@code 1E+1} (lo que da {@code stripTrailingZeros()}) salen {@code 10}—; una tasa con decimales
     * reales se conserva (la rechaza la validación, {@code JBS-E-92}).
     */
    @Test
    public void tasaDelIvaEnteraSeEscribeSinDecimalesNiExponente() throws Exception {
        for (String t : new String[]{"10", "10.00", "1E+1", "10.0000"}) {
            org.javabeanstack.sifen.core.fields.request.de.TgCamIVA iva = new org.javabeanstack.sifen.core.fields.request.de.TgCamIVA();
            iva.setdTasaIVA(new BigDecimal(t));
            Assertions.assertEquals("10", iva.getdTasaIVA().toString(), t);
            Assertions.assertEquals(0, iva.getdTasaIVA().scale(), t);
            DocumentoElectronico de = caso("DE-05").construir();
            de.getgDtipDE().getgCamItemList().get(0).getgCamIVA().setdTasaIVA(new BigDecimal(t));
            String xml = de.generarXml(ctx, cfg);
            Assertions.assertEquals("10", XmlCanonico.texto(xml, "dTasaIVA"), t);
            Assertions.assertEquals("10", XmlCanonico.texto(de.generarXml(ctx, cfg), "dTasaIVA"), t + " (dos veces)");
            Assertions.assertEquals("10", new DocumentoElectronico(xml).getgDtipDE().getgCamItemList().get(0).getgCamIVA()
                    .getdTasaIVA().toString(), t + " releído");
        }
        Assertions.assertEquals("0", integralDe("0.00"));
        Assertions.assertEquals("5", integralDe("5.0"));
        Assertions.assertEquals("10.5", integralDe("10.5"), "con decimales reales se conserva");
    }

    private static String integralDe(String t) {
        org.javabeanstack.sifen.core.fields.request.de.TgCamIVA iva = new org.javabeanstack.sifen.core.fields.request.de.TgCamIVA();
        iva.setdTasaIVA(new BigDecimal(t));
        return iva.getdTasaIVA().toString();
    }

    // ------------------------------------------------------------------ CA4-01 reformulado (usuario, 2026-11-24)

    /**
     * Importes del DE: cada hoja numérica de {@code gOpeCom}, {@code gCamItem} y {@code gTotSub}, por ruta
     * (los ítems con su índice).
     */
    private static Map<String, String> importes(String xml) throws Exception {
        Element de = (Element) XmlCanonico.parsear(xml).getElementsByTagNameNS("http://ekuatia.set.gov.py/sifen/xsd", "DE").item(0);
        Map<String, String> m = new LinkedHashMap<>();
        juntar(de, "", m, false);
        return m;
    }

    private static void juntar(Element e, String ruta, Map<String, String> m, boolean dentro) {
        int item = 0;
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (!(n instanceof Element h)) {
                continue;
            }
            String nombre = h.getLocalName();
            String r = ruta + "/" + nombre + (nombre.equals("gCamItem") ? "[" + (item++) + "]" : "");
            boolean d = dentro || nombre.equals("gOpeCom") || nombre.equals("gCamItem") || nombre.equals("gTotSub");
            boolean hoja = h.getElementsByTagNameNS("*", "*").getLength() == 0;
            if (hoja) {
                if (d && h.getTextContent().matches("-?\\d+(\\.\\d+)?")) {
                    m.put(r, h.getTextContent());
                }
            } else {
                juntar(h, r, m, d);
            }
        }
    }

    /**
     * CA4-01 con el texto que aprobó el usuario el 2026-11-24 (I4-03): «la firma se conserva enviando el XML
     * firmado guardado; al regenerar se conservan el CDC y los importes, no necesariamente la firma». Los dos
     * XML del caso de redondeo en USD, releídos y regenerados: mismo CDC y cada importe igual por valor ({@code compareTo}; el
     * aprobado pasa de {@code 8517.500} a {@code 8517.50}, I4-03). La primera mitad —el envío del guardado
     * conserva la firma— la verifica {@code EnvioFirmadoTest.elAprobadoUsdConRedondeoSeEnviaSinRegenerar}.
     */
    @ParameterizedTest(name = "CA4-01: {0} regenerado conserva el CDC y los importes")
    @org.junit.jupiter.params.provider.ValueSource(strings = {"caso-usd-sin-redondeo-original", "caso-usd-con-redondeo-aprobado"})
    public void usdRedondeoRegeneradoConservaCdcEImportes(String archivo) throws Exception {
        String guardado = FixturesSifen.recurso("casos/" + archivo + ".xml");
        DocumentoElectronico leido = new DocumentoElectronico(guardado);
        String regenerado = leido.generarXml(ctx, cfg);
        String cdc = ((Element) XmlCanonico.parsear(guardado).getElementsByTagNameNS("*", "DE").item(0)).getAttribute("Id");
        Assertions.assertEquals(cdc, new DocumentoElectronico(regenerado).getId(), "CDC");
        Map<String, String> antes = importes(guardado);
        Map<String, String> despues = importes(regenerado);
        Assertions.assertTrue(antes.size() > 20, "importes leídos: " + antes.size());
        for (Map.Entry<String, String> e : antes.entrySet()) {
            String d = despues.get(e.getKey());
            Assertions.assertNotNull(d, "falta " + e.getKey() + " al regenerar");
            Assertions.assertEquals(0, new BigDecimal(e.getValue()).compareTo(new BigDecimal(d)),
                    e.getKey() + ": " + e.getValue() + " ≠ " + d);
        }
        Assertions.assertEquals(antes.keySet(), despues.keySet(), "los mismos importes");
    }
}

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
package org.javabeanstack.sifen.test.calculo;

import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.fields.request.de.TgTotSub;
import org.javabeanstack.sifen.core.validation.support.DEData;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.CasosFase12;
import org.javabeanstack.sifen.test.casos.FallasFachada;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * Totales de {@code TgTotSub} en la línea base del fork (plan JBSSIF, §2.4; pseudocódigo en la ficha
 * 4 del informe de línea base).
 *
 * <p>Las aserciones comparan {@link BigDecimal} con {@code equals} (sensible a la escala) o el texto
 * del XML: la escala es parte del comportamiento (E0-03, E0-13). Lo que el fork hace mal se fija
 * como está y se marca {@code @Tag("linea-base")} con el código del hallazgo (D10); la Fase 4 lo
 * cambia con justificación.</p>
 */
public class TgTotSubTest {
    private static SifenConfig cfg;
    private static GenerationCtx ctx;

    @BeforeAll
    public static void configurar() {
        cfg = FixturesSifen.configuracion();
        ctx = FixturesSifen.contexto(cfg);
    }

    private static CasosDePrueba.Caso caso(String id) {
        for (CasosDePrueba.Caso c : CasosDePrueba.documentos()) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        throw new IllegalArgumentException(id);
    }

    /** Genera desde el XML de entrada del caso y devuelve los totales calculados. */
    private static Resultado desdeXml(String id) throws Exception {
        CasosDePrueba.Caso c = caso(id);
        DocumentoElectronico de = c.leer(FixturesSifen.entrada(c));
        String xml = de.generarXml(ctx, cfg);
        return new Resultado(de.getgTotSub(), xml);
    }

    private static Resultado porObjeto(String id) throws Exception {
        DocumentoElectronico de = caso(id).construir();
        String xml = de.generarXml(ctx, cfg);
        return new Resultado(de.getgTotSub(), xml);
    }

    private static final class Resultado {
        final TgTotSub t;
        final String xml;

        Resultado(TgTotSub t, String xml) {
            this.t = t;
            this.xml = xml;
        }

        String texto(String elemento) throws Exception {
            return XmlCanonico.texto(xml, elemento);
        }
    }

    private static void igual(String esperado, BigDecimal obtenido, String campo) {
        Assertions.assertEquals(new BigDecimal(esperado), obtenido, campo + " (valor y escala)");
    }

    // ------------------------------------------------------------- caso USD del relevamiento (CA2-04)

    /**
     * El DE en USD generado sin redondeo, releído del XML, sale <b>sin</b> redondeo (§4.2, E0-01): la línea
     * base lo recalculaba con redondeo ({@code dRedon} 0.100, {@code dTotGralOpe} 8517.500,
     * {@code dTotalGs} 49870303.2000000), que eran los números del DE aprobado.
     */
    @Test
    public void usdReleidoDelXmlConservaElSinRedondeoE001() throws Exception {
        Resultado r = desdeXml("DE-02");
        Assertions.assertFalse(r.t.getRedondeo(), "dRedon 0 en el XML ⇒ sin redondeo");
        igual("8517.60", r.t.getdTotOpe(), "dTotOpe");
        igual("0", r.t.getdRedon(), "dRedon");
        igual("8517.60", r.t.getdTotGralOpe(), "dTotGralOpe");
        igual("49870888.704000", r.t.getdTotalGs(), "dTotalGs");
        Assertions.assertEquals("0", r.texto("dRedon"));
        Assertions.assertEquals("8517.60", r.texto("dTotGralOpe"));
        Assertions.assertEquals("49870888.704000", r.texto("dTotalGs"));
    }

    /** El DE aprobado (con redondeo) releído: escala fija 2 en {@code dRedon} y {@code dTotGralOpe} (§4.3, E0-03). */
    @Test
    public void usdAprobadoReleidoConEscalaFijaE003() throws Exception {
        Resultado r = desdeXml("DE-04");
        Assertions.assertTrue(r.t.getRedondeo());
        igual("0.10", r.t.getdRedon(), "dRedon (la línea base: 0.100)");
        igual("8517.50", r.t.getdTotGralOpe(), "dTotGralOpe (la línea base: 8517.500)");
        igual("49870303.200000", r.t.getdTotalGs(), "dTotalGs = 8517.50 × 5855.0400 (la línea base: escala 7)");
        // En el XML dRedon pasa por FieldFormatUtil.formattdCRed: "0.1" (I2-03).
        Assertions.assertEquals("0.1", r.texto("dRedon"));
        Assertions.assertEquals("8517.50", r.texto("dTotGralOpe"));
        Assertions.assertEquals("49870303.200000", r.texto("dTotalGs"));
    }

    /** El mismo DE con {@code TgTotSub(false)}: los totales originales sin redondeo. */
    @Test
    public void usdSinRedondeoConservaLosTotalesOriginales() throws Exception {
        Resultado r = desdeXml("DE-03");
        igual("0", r.t.getdRedon(), "dRedon");
        igual("8517.60", r.t.getdTotGralOpe(), "dTotGralOpe");
        igual("49870888.704000", r.t.getdTotalGs(), "dTotalGs");
        Assertions.assertEquals("0", r.texto("dRedon"));
        Assertions.assertEquals("49870888.704000", r.texto("dTotalGs"));
    }

    /**
     * Releer y regenerar el XML sin redondeo da el DE sin redondeo (DE-03, el flag puesto a mano), ya no
     * el aprobado con redondeo: la línea base daba byte a byte DE-04 (doble firma, E0-01; §4.2).
     */
    @Test
    public void releerElXmlSinRedondeoNoReproduceElAprobadoE001() throws Exception {
        Resultado original = desdeXml("DE-02");
        Resultado sinRedondeo = desdeXml("DE-03");
        Resultado aprobado = desdeXml("DE-04");
        Assertions.assertArrayEquals(XmlCanonico.canonico(sinRedondeo.xml), XmlCanonico.canonico(original.xml));
        Assertions.assertFalse(java.util.Arrays.equals(XmlCanonico.canonico(aprobado.xml), XmlCanonico.canonico(original.xml)));
    }

    // ------------------------------------------------------------- PYG

    @Test
    public void pygRedondeoSedecoYEscalas() throws Exception {
        Resultado r = porObjeto("DE-05");
        igual("47789.00", r.t.getdSub10(), "dSub10 (escala 2 también en PYG, E0-13)");
        igual("47789", r.t.getdTotOpe(), "dTotOpe (escala 0 en PYG)");
        igual("39", r.t.getdRedon(), "dRedon");
        igual("47750", r.t.getdTotGralOpe(), "dTotGralOpe");
        igual("4344.45", r.t.getdIVA10(), "dIVA10");
        igual("4344.45", r.t.getdTotIVA(), "dTotIVA");
        igual("43444.55", r.t.getdBaseGrav10(), "dBaseGrav10");
        Assertions.assertEquals("47789.00", XmlCanonico.texto(r.xml, "dTotOpeItem"), "dTotOpeItem con 2 decimales en PYG (E0-13)");
    }

    /** Con {@code TgTotSub(false)} no hay redondeo, por objeto y releído del XML (§4.2; la línea base, releído: 39/47750, E0-01). */
    @Test
    public void flagDeRedondeoSobreviveAlReleerE001() throws Exception {
        Resultado obj = porObjeto("DE-06");
        igual("0", obj.t.getdRedon(), "dRedon por objeto");
        igual("47789", obj.t.getdTotGralOpe(), "dTotGralOpe por objeto");
        Resultado xml = desdeXml("DE-06");
        igual("0", xml.t.getdRedon(), "dRedon releído (E0-01)");
        igual("47789", xml.t.getdTotGralOpe(), "dTotGralOpe releído (E0-01)");
    }

    @Test
    public void ivaMixto10Y5YExento() throws Exception {
        Resultado r = porObjeto("DE-09");
        igual("110000.00", r.t.getdSub10(), "dSub10");
        igual("105000.00", r.t.getdSub5(), "dSub5");
        igual("30000.00", r.t.getdSubExe(), "dSubExe");
        igual("245000", r.t.getdTotOpe(), "dTotOpe");
        igual("10000.00", r.t.getdIVA10(), "dIVA10");
        igual("5000.00", r.t.getdIVA5(), "dIVA5");
        igual("15000.00", r.t.getdTotIVA(), "dTotIVA");
        igual("200000.00", r.t.getdTBasGraIVA(), "dTBasGraIVA");
        igual("245000", r.t.getdTotGralOpe(), "dTotGralOpe");
    }

    /** Gravado parcial (NT 13): la base exenta suma a dSubExe y se resta de dSub10. */
    @Test
    public void gravadoParcialSumaLaBaseExenta() throws Exception {
        Resultado r = porObjeto("DE-10");
        igual("679611.65", r.t.getdSubExe(), "dSubExe = dBasExe");
        igual("320388.35", r.t.getdSub10(), "dSub10 = dTotOpeItem - dBasExe");
        igual("1000000", r.t.getdTotOpe(), "dTotOpe");
        igual("29126.21", r.t.getdIVA10(), "dIVA10");
        igual("291262.14", r.t.getdBaseGrav10(), "dBaseGrav10");
    }

    @Test
    public void descuentoPorItemYGlobal() throws Exception {
        Resultado r = porObjeto("DE-13");
        igual("225000", r.t.getdTotOpe(), "dTotOpe");
        igual("20000", r.t.getdTotDesc(), "dTotDesc = 10000 × 2");
        igual("5000", r.t.getdTotDescGlotem(), "dTotDescGlotem = 2000 × 2 + 1000 × 1 (NT 001, F033; I12-21: el descuento global prorrateado en cada ítem, EA004)");
        igual("25000", r.t.getdDescTotal(), "dDescTotal");
        igual("2.00000000", r.t.getdPorcDescTotal(), "dPorcDescTotal = global / (total + descuento), escala 8");
        igual("20454.55", r.t.getdIVA10(), "dIVA10");
    }

    /**
     * Anticipo por ítem: la línea base no multiplicaba {@code dTotAntItem} por la cantidad (I0-13); desde
     * E3-25 (§3.8, ajuste de app-developer) sigue la NT 001, F034 = Σ EA006 × E711.
     */
    @Test
    public void anticipoMultiplicadoPorCantidadE325() throws Exception {
        Resultado r = porObjeto("DE-14");
        igual("450000", r.t.getdTotOpe(), "dTotOpe = (200000 - 50000) × 3");
        igual("150000", r.t.getdTotAntItem(), "dTotAntItem = 50000 × 3 (NT 001, F034)");
        igual("150000", r.t.getdAnticipo(), "dAnticipo = F034 + F035");
    }

    /** dTasaIVA = 10.00 suma como 10 (§4.5, E0-17): la línea base no lo sumaba (equals sensible a la escala) y daba todo en 0. */
    @Test
    public void tasaConEscalaSumaE017() throws Exception {
        CasosDePrueba.Caso c = caso("DE-18");
        DocumentoElectronico de = c.construir();
        String xml = de.generarXml(ctx, cfg);
        Assertions.assertEquals(new BigDecimal("10000.00"), de.getgDtipDE().getgCamItemList().get(0).getgCamIVA().getdLiqIVAItem(),
                "TgCamIVA ya no depende de la escala (E0-04 resuelto en el fork)");
        igual("110000.00", de.getgTotSub().getdSub10(), "dSub10 (E0-17)");
        igual("10000.00", de.getgTotSub().getdIVA10(), "dIVA10 (E0-17)");
        igual("110000", de.getgTotSub().getdTotOpe(), "dTotOpe (E0-17)");
        igual("110000", de.getgTotSub().getdTotGralOpe(), "dTotGralOpe (E0-17)");
        Assertions.assertEquals("110000", XmlCanonico.texto(xml, "dTotGralOpe"));
    }

    // ------------------------------------------------------------- USD

    @Test
    public void usdCambioPorItemSumaDTotOpeGs() throws Exception {
        Resultado r = porObjeto("DE-08");
        igual("380.60", r.t.getdTotOpe(), "dTotOpe");
        igual("2779296.1750", r.t.getdTotalGs(), "dTotalGs = Σ dTotOpeItem × dTiCamIt");
        igual("0.10", r.t.getdRedon(), "dRedon 380.60 → 380.50 (escala fija 2, §4.3; la línea base: 0.100, E0-03)");
        igual("380.50", r.t.getdTotGralOpe(), "dTotGralOpe (escala fija 2, §4.3; la línea base: 380.500, E0-03)");
    }

    @Test
    public void usdCambioGlobal() throws Exception {
        Resultado r = porObjeto("DE-07");
        // 3 × 1234.56 + 99.99 = 3803.67 → redondeo a 0.50: 3803.50, dRedon 0.17
        igual("3803.67", r.t.getdTotOpe(), "dTotOpe");
        igual("0.17", r.t.getdRedon(), "dRedon (la línea base: 0.170)");
        igual("3803.50", r.t.getdTotGralOpe(), "dTotGralOpe (escala fija 2, §4.3; la línea base: 3803.500, E0-03)");
        igual("27766500.8750", r.t.getdTotalGs(), "dTotalGs = 3803.50 × 7300.25 (escala 2 + 2; la línea base: 3 + 2, E0-03)");
    }

    // ------------------------------------------------------------- NPE de la línea base

    /**
     * Sin descuento cargado (null) TgTotSub lanza NPE: dDescItem/dDescGloItem obligatorios de hecho (E2-03).
     * Desde §3.8 la NPE sale envuelta en {@code JBS-INT-00} (la red de la fachada; ajuste de app-developer).
     */
    @Test
    @Tag("linea-base")
    public void descuentoNuloDaNullPointerE203() {
        DocumentoElectronico sinDescItem = caso("DE-05").construir();
        sinDescItem.getgDtipDE().getgCamItemList().get(0).getgValorItem().getgValorRestaItem().setdDescItem(null);
        FallasFachada.npeInterna(() -> sinDescItem.generarXml(ctx, cfg));

        DocumentoElectronico sinDescGlo = caso("DE-05").construir();
        sinDescGlo.getgDtipDE().getgCamItemList().get(0).getgValorItem().getgValorRestaItem().setdDescGloItem(null);
        FallasFachada.npeInterna(() -> sinDescGlo.generarXml(ctx, cfg));
    }

    /**
     * §6.3 (D25): con un contexto armado a mano (sin la configuración de la NT 13, que ya no existe) se genera igual y
     * {@code dBasExe} sale siempre. La línea base, con la NT 13 apagada, daba NPE por objeto y omitía {@code dBasExe}
     * desde el XML (E0-16).
     */
    @Test
    public void dBasExeSiempreSinFlagE016() throws Exception {
        GenerationCtx aMano = new GenerationCtx();
        aMano.setSifenConfig(cfg);
        String porObjeto = caso("DE-05").construir().generarXml(aMano, cfg);
        Assertions.assertEquals("0", XmlCanonico.texto(porObjeto, "dBasExe"));

        CasosDePrueba.Caso c = caso("DE-01");
        String xml = c.leer(FixturesSifen.entrada(c)).generarXml(aMano, cfg);
        Assertions.assertNotNull(XmlCanonico.texto(xml, "dBasExe"), "dBasExe se escribe siempre (NT 13)");
    }

    // ------------------------------------------------------------- comisión (Fase 12, E12-01)

    /**
     * E12-01: la comisión cargada con {@code setdComi} llega al XML (F025) y suma al total (F014 = F008 − F013 + F025);
     * generar no la borra del objeto y generar dos veces da lo mismo. La línea base la perdía en {@code resetValues}
     * ({@code dComi 0}, {@code dIVAComi 0.00}, {@code dTotGralOpe} 1100000). {@code dIVAComi} es el IVA incluido en la
     * comisión, {@code dComi / 11} (I3-20, decisión del usuario del 2026-11-25), y {@code dTotIVA} lo suma.
     */
    @Test
    public void comisionLlegaAlXmlYSumaAlTotalE1201() throws Exception {
        DocumentoElectronico de = CasosFase12.todos().get("DE-62").get();
        String xml = de.generarXml(ctx, cfg);
        TgTotSub t = de.getgTotSub();
        igual("110000", t.getdComi(), "dComi se conserva tras generar");
        igual("1210000", t.getdTotGralOpe(), "dTotGralOpe = dTotOpe − dRedon + dComi");
        igual("10000.00", t.getdIVAComi(), "dIVAComi = dComi / 11 (I3-20; la línea base: / 1,1 = 100000.00)");
        igual("110000.00", t.getdTotIVA(), "dTotIVA = dIVA10 + dIVAComi");
        Assertions.assertEquals("110000", XmlCanonico.texto(xml, "dComi"));
        Assertions.assertEquals("1210000", XmlCanonico.texto(xml, "dTotGralOpe"));
        Assertions.assertEquals("10000.00", XmlCanonico.texto(xml, "dIVAComi"));
        Assertions.assertEquals("110000.00", XmlCanonico.texto(xml, "dTotIVA"));
        Assertions.assertEquals(0, new BigDecimal("1210000").compareTo(DEData.operationTotal(de)), "la validación previa ve el mismo total");
        String otraVez = de.generarXml(ctx, cfg);
        Assertions.assertArrayEquals(XmlCanonico.canonico(xml), XmlCanonico.canonico(otraVez), "generar dos veces da lo mismo");
        igual("110000", de.getgTotSub().getdComi(), "dComi tras la segunda generación");
    }

    /** E12-01: releída del XML, la comisión se conserva y regenerar da el mismo DE. */
    @Test
    public void comisionReleidaDelXmlSeConservaE1201() throws Exception {
        String xml = CasosFase12.todos().get("DE-62").get().generarXml(ctx, cfg);
        DocumentoElectronico leido = new DocumentoElectronico(xml);
        String regenerado = leido.generarXml(ctx, cfg);
        igual("110000", leido.getgTotSub().getdComi(), "dComi releído");
        Assertions.assertArrayEquals(XmlCanonico.canonico(xml), XmlCanonico.canonico(regenerado));
    }

    /**
     * E12-01 (F001: con C002 = 4 no se informan F025 ni F026): en la autofactura la comisión no se escribe ni suma al
     * total, ni en el XML ni en la validación previa; el dato cargado queda en el objeto. El DE sale igual que sin ella.
     */
    @Test
    public void autofacturaNoInformaNiSumaLaComisionE1201() throws Exception {
        DocumentoElectronico sin = caso("DE-20").construir();
        String xmlSin = sin.generarXml(ctx, cfg);
        DocumentoElectronico con = caso("DE-20").construir();
        con.getgTotSub().setdComi(new BigDecimal("1000"));
        String xmlCon = con.generarXml(ctx, cfg);
        Assertions.assertNull(XmlCanonico.texto(xmlCon, "dComi"));
        Assertions.assertNull(XmlCanonico.texto(xmlCon, "dIVAComi"));
        Assertions.assertEquals(sin.getgTotSub().getdTotGralOpe(), con.getgTotSub().getdTotGralOpe());
        Assertions.assertEquals(DEData.operationTotal(sin), DEData.operationTotal(con));
        Assertions.assertArrayEquals(XmlCanonico.canonico(xmlSin), XmlCanonico.canonico(xmlCon));
        igual("1000", con.getgTotSub().getdComi(), "el dato cargado se conserva");
    }

    /** Una comisión nula se escribe en 0 ({@code dComi 0}, {@code dIVAComi 0.00}), como en la línea base. */
    @Test
    public void comisionNulaSeEscribeEnCero() throws Exception {
        DocumentoElectronico de = caso("DE-05").construir();
        String sinTocar = de.generarXml(ctx, cfg);
        DocumentoElectronico nula = caso("DE-05").construir();
        nula.getgTotSub().setdComi(null);
        String xml = nula.generarXml(ctx, cfg);
        Assertions.assertEquals("0", XmlCanonico.texto(xml, "dComi"));
        Assertions.assertEquals("0.00", XmlCanonico.texto(xml, "dIVAComi"));
        Assertions.assertArrayEquals(XmlCanonico.canonico(sinTocar), XmlCanonico.canonico(xml));
    }

    // ------------------------------------------------------------- redondeo de dIVAComi (Fase 12, M12-05)

    /**
     * M12-05: {@code dIVAComi} = {@code dComi / 11} con 2 decimales y {@code HALF_UP} (I3-20) en guaraníes, con
     * divisiones no exactas. 100000 / 11 = 9090.909… da 9090.91 ({@code DOWN} daría 9090.90) y 1 / 11 = 0.0909… da
     * 0.09 ({@code UP} daría 0.10); {@code dTotIVA} y {@code dTotGralOpe} siguen al valor redondeado.
     */
    @Test
    public void ivaDeLaComisionNoExactaRedondeaHalfUpEnGuaraniesM1205() throws Exception {
        String[][] casos = {
            // dComi, dIVAComi, dTotIVA (dIVA10 100000 + dIVAComi), dTotGralOpe (1100000 + dComi)
            {"100000", "9090.91", "109090.91", "1200000"},
            {"1", "0.09", "100000.09", "1100001"}};
        for (String[] k : casos) {
            DocumentoElectronico de = CasosFase12.todos().get("DE-62").get();
            de.getgTotSub().setdComi(new BigDecimal(k[0]));
            String xml = de.generarXml(ctx, cfg);
            Assertions.assertEquals(k[1], XmlCanonico.texto(xml, "dIVAComi"), "dIVAComi de " + k[0]);
            Assertions.assertEquals(k[2], XmlCanonico.texto(xml, "dTotIVA"), "dTotIVA con la comisión " + k[0]);
            Assertions.assertEquals(k[3], XmlCanonico.texto(xml, "dTotGralOpe"), "dTotGralOpe con la comisión " + k[0]);
            igual(k[1], de.getgTotSub().getdIVAComi(), "dIVAComi de " + k[0]);
        }
    }

    /**
     * M12-05 en moneda extranjera (DE-07, USD con cambio global): 10.00 / 11 = 0.909… da 0.91 ({@code DOWN}: 0.90);
     * 1.375 / 11 = 0.125 exacto da 0.13 ({@code HALF_EVEN} y {@code HALF_DOWN}: 0.12); 1.00 / 11 = 0.0909… da 0.09
     * ({@code UP}: 0.10). {@code dTotIVA} es el del mismo DE sin comisión más {@code dIVAComi}, {@code dTotGralOpe}
     * suma la comisión y {@code dTotalGs} = {@code dTotGralOpe} × {@code dTiCam}.
     */
    @Test
    public void ivaDeLaComisionNoExactaRedondeaHalfUpEnMonedaExtranjeraM1205() throws Exception {
        DocumentoElectronico sin = caso("DE-07").construir();
        String xmlSin = sin.generarXml(ctx, cfg);
        Assertions.assertEquals("USD", XmlCanonico.texto(xmlSin, "cMoneOpe"));
        Assertions.assertEquals("1", XmlCanonico.texto(xmlSin, "dCondTiCam"), "cambio global");
        BigDecimal ivaSin = new BigDecimal(XmlCanonico.texto(xmlSin, "dTotIVA"));
        BigDecimal totalSin = new BigDecimal(XmlCanonico.texto(xmlSin, "dTotGralOpe"));
        BigDecimal cambio = new BigDecimal(XmlCanonico.texto(xmlSin, "dTiCam"));
        String[][] casos = {{"10.00", "0.91"}, {"1.375", "0.13"}, {"1.00", "0.09"}};
        for (String[] k : casos) {
            DocumentoElectronico de = caso("DE-07").construir();
            BigDecimal comision = new BigDecimal(k[0]);
            de.getgTotSub().setdComi(comision);
            String xml = de.generarXml(ctx, cfg);
            BigDecimal iva = new BigDecimal(k[1]);
            Assertions.assertEquals(k[1], XmlCanonico.texto(xml, "dIVAComi"), "dIVAComi de " + k[0] + " USD");
            Assertions.assertEquals(ivaSin.add(iva).toPlainString(), XmlCanonico.texto(xml, "dTotIVA"), "dTotIVA con la comisión " + k[0]);
            BigDecimal total = totalSin.add(comision);
            Assertions.assertEquals(total.toPlainString(), XmlCanonico.texto(xml, "dTotGralOpe"), "dTotGralOpe con la comisión " + k[0]);
            Assertions.assertEquals(0, total.multiply(cambio).compareTo(new BigDecimal(XmlCanonico.texto(xml, "dTotalGs"))),
                    "dTotalGs = dTotGralOpe × dTiCam con la comisión " + k[0]);
        }
    }
}

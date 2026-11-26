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
package org.javabeanstack.sifen.test.fase12;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.internal.request.MensajesSifen;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.CasosFase12;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.NodeList;

/**
 * XML de entrada y XML de referencia <b>candidato</b> de los casos de la Fase 12 (plan JBSSIF, §12.2.2,
 * D53; CA12-02). No son golden del arnés A/B: los genera {@code jbs-sifen} y pasan a «aceptado» cuando
 * SIFEN-test los aprueba (§12.3).
 *
 * <p>Por caso: (1) los campos del MT que §12.1 nombra tienen el valor esperado en el XML generado
 * ({@link #CAMPOS}: la revisión campo por campo, que corre <b>antes</b> de congelar); (2) el XML armado por
 * objeto y el regenerado desde el XML de entrada son iguales byte a byte (C14N) al candidato
 * {@code golden/candidatos/<ID>.xml}. Con {@code -Djbs.sifen.f12.congelar=<src/test/resources>} escribe la
 * entrada y el candidato en vez de compararlos.</p>
 */
public class CandidatosFase12Test {
    private static final String CONGELAR = System.getProperty("jbs.sifen.f12.congelar");
    private static SifenConfig cfg;
    private static GenerationCtx ctx;

    /**
     * Campos esperados por caso (de §12.1). {@code tag} = texto del primer elemento; {@code tag*} = textos de
     * todos, unidos con {@code |}; {@code #tag} = cantidad; {@code !tag} = ausente. En el valor,
     * {@code {cdc:ID}} es el CDC del caso {@code ID}.
     */
    static final Map<String, String[][]> CAMPOS = Map.ofEntries(
            Map.entry("ID-F08", new String[][] {{"iNatRec", "2"}, {"iTiOpe", "4"}, {"cPaisRec", "ARG"}, {"iTipIDRec", "3"},
                {"dNumIDRec", "12345678"}, {"dDirRec", "AVENIDA FICTICIA 123"}, {"!cDepRec", ""}, {"!cCiuRec", ""}}),
            Map.entry("ID-F09", new String[][] {{"iTiOpe", "4"}, {"iTipIDRec", "4"}, {"dNumIDRec", "R-12345"}, {"dDirRec", "AVENIDA FICTICIA 123"}}),
            Map.entry("DE-41", new String[][] {{"iTiDE", "5"}, {"iMotEmi", "2"}, {"iTipDocAso", "1"}, {"dCdCDERef", "{cdc:DE-16}"},
                {"iNatRec", "2"}, {"iTipIDRec", "1"}, {"dNumIDRec", "1234567"}, {"dNomRec", "PERSONA FICTICIA"}}),
            Map.entry("DE-39", new String[][] {{"iTiDE", "5"}, {"dCdCDERef", "{cdc:DE-09}"}, {"#gCamItem", "1"}, {"dTotGralOpe", "110000"}}),
            Map.entry("DE-39b", new String[][] {{"iTiDE", "5"}, {"dCdCDERef", "{cdc:DE-09}"}, {"#gCamItem", "2"}, {"dTotGralOpe", "135000"}}),
            Map.entry("DE-40", new String[][] {{"iTiDE", "5"}, {"iTipDocAso", "2"}, {"dNTimDI", "12345679"}, {"dEstDocAso", "001"},
                {"dPExpDocAso", "001"}, {"dNumDocAso", "0000100"}, {"iTipoDocAso", "1"}, {"dFecEmiDI", "2026-10-15"}, {"!dCdCDERef", ""}}),
            Map.entry("DE-36", new String[][] {{"iTipTra", "9"}, {"cMoneOpe", "USD"}, {"dCondTiCam", "1"}, {"dTiCam", "7300.25"},
                {"dTotGralOpe", "1000.00"}, {"!dCDCAnticipo", ""}, {"!iCondAnt", ""}}),
            Map.entry("DE-37", new String[][] {{"iCondAnt", "2"}, {"dAntPreUniIt", "1000.00"}, {"dCDCAnticipo", "{cdc:DE-36}"},
                {"dCdCDERef", "{cdc:DE-36}"}, {"dTotAntItem", "1000.00"}, {"dTotGralOpe", "1500.00"}}),
            Map.entry("DE-37b", new String[][] {{"dCantProSer", "2"}, {"dAntPreUniIt", "500.00"}, {"dTotAntItem", "1000.00"}, {"dTotGralOpe", "4000.00"}}),
            Map.entry("DE-38", new String[][] {{"iTiDE", "5"}, {"cMoneOpe", "USD"}, {"dCdCDERef", "{cdc:DE-37}"}, {"dTotGralOpe", "500.00"}}),
            Map.entry("DE-42", new String[][] {{"iTiDE", "4"}, {"iTiOpe", "2"}, {"iNatVen", "2"}, {"iTipIDVen", "2"}, {"dNumIDVen", "XK998877"},
                {"dDirVen", "MERCADO FICTICIO DE ABASTO"}, {"cDepVen", "1"}, {"cCiuVen", "1"}, {"iTipDocAso", "3"}, {"iTipCons", "1"}, {"iCondOpe", "1"}}),
            Map.entry("DE-43", new String[][] {{"iNatVen", "2"}, {"iTipIDVen", "3"}, {"dNumIDVen", "98765432"}}),
            Map.entry("DE-33", new String[][] {{"iTiOpe", "3"}, {"!gCompPub", ""}, {"dCodConDncp", "DNCP-2026-000123"}, {"!dDncpG", ""}}),
            Map.entry("DE-34", new String[][] {{"iTiDE", "1"}, {"iTipTra", "1"}, {"dInfoFisc", "{infoFisc}"}, {"cMoneOpe", "USD"},
                {"iNatRec", "2"}, {"iTiOpe", "4"}, {"cPaisRec", "BRA"}, {"iTipIDRec", "9"}, {"dDTipIDRec", CasosFase12.IDENTIFICACION_TRIBUTARIA},
                {"dNumIDRec", "12345678000190"}, {"dDirRec", "RUA FICTICIA 100"}, {"!cDepRec", ""}, {"iCondOpe", "2"}, {"iCondCred", "1"},
                {"dNCM", "12019000"}, {"cPaisOrig", "PRY"}, {"iAfecIVA", "3"}, {"dPropIVA", "0"}, {"dTasaIVA", "0"},
                {"iModTrans", "2"}, {"iRespFlete", "2"}, {"cCondNeg", "FOB"}, {"dNuManif", "MANIF-0001"}, {"dIniTras", "2026-11-24"},
                {"dFinTras", "2026-12-05"}, {"cPaisDest", "BRA"}, {"dTotPesMerc", "25000"}, {"dTotVolMerc", "32"}, {"dTotGralOpe", "10000.00"}}),
            Map.entry("DE-35", new String[][] {{"iTipTra", "2"}, {"iTiOpe", "4"}, {"iTipIDRec", "9"}, {"!gTransp", ""}, {"!dNCM", ""},
                {"iTiPago", "5"}, {"cMoneTiPag", "USD"}, {"dTiCamTiPag", "7300.25"}, {"iAfecIVA", "3"}}),
            Map.entry("DE-44", new String[][] {{"iTiPago", "3"}, {"iDenTarj", "1"}, {"dRSProTar", "PROCESADORA FICTICIA S.A."},
                {"dRUCProTar", "80123456"}, {"dDVProTar", "5"}, {"iForProPa", "1"}, {"dCodAuOpe", "123456"}, {"dNomTit", "TITULAR FICTICIO"},
                {"dNumTarj", "1234"}}),
            Map.entry("DE-45", new String[][] {{"iTiPago", "2"}, {"dNumCheq", "00012345"}, {"dBcoEmi", "BANCO FICTICIO"}}),
            Map.entry("DE-46", new String[][] {{"#gPaConEIni", "4"}, {"iTiPago*", "1|4|5|7"}, {"#gPagTarCD", "1"}, {"iDenTarj", "4"}}),
            Map.entry("DE-47", new String[][] {{"#gPaConEIni", "2"}, {"cMoneTiPag*", "PYG|USD"}, {"#dTiCamTiPag", "1"}, {"dTiCamTiPag", "7300.25"}}),
            Map.entry("DE-48", new String[][] {{"iCondOpe", "2"}, {"iCondCred", "2"}, {"dCuotas", "2"}, {"dMonEnt", "100000"}, {"#gCuotas", "2"},
                {"#gPaConEIni", "1"}}),
            Map.entry("DE-49", new String[][] {{"iAfecIVA*", "3|3"}, {"dTotGralOpe", "100000"}}),
            Map.entry("DE-50", new String[][] {{"dDescGloItem*", "3333.30|2222.20|1111.10"}, {"dTotGralOpe", "79950"}}),
            Map.entry("DE-51", new String[][] {{"dDescItem*", "0|20000"}, {"dTotGralOpe", "100000"}}),
            Map.entry("DE-52", new String[][] {{"cMoneOpe", "EUR"}, {"dCondTiCam", "1"}, {"dTiCam", "8000.50"}, {"dTotGralOpe", "250.00"}}),
            Map.entry("DE-53", new String[][] {{"cMoneOpe", "BRL"}, {"dCondTiCam", "2"}, {"dTiCamIt*", "1350.25|1360.10"}}),
            Map.entry("DE-54", new String[][] {{"cMoneOpe", "ARS"}, {"dTiCam", "7.1234"}, {"dTotGralOpe", "15000.00"}}),
            Map.entry("DE-57", new String[][] {{"iTiDE", "7"}, {"iMotEmiNR", "7"}, {"dRucRec", "80123456"}, {"dKmR", "25"}, {"!gTotSub", ""}}),
            Map.entry("DE-58", new String[][] {{"iTiDE", "7"}, {"iMotEmiNR", "2"}, {"dRucRec", "80654321"}}),
            Map.entry("DE-59", new String[][] {{"iMotEmiNR", "3"}, {"iTiOpe", "4"}, {"cPaisRec", "BRA"}, {"dDirRec", "RUA FICTICIA 100"},
                {"cCondNeg", "FOB"}, {"cPaisDest", "BRA"}}),
            Map.entry("DE-60", new String[][] {{"iMotEmiNR", "99"}, {"dDesMotEmiNR", "TRASLADO PARA PRUEBA DE CALIDAD"}}),
            Map.entry("DE-61", new String[][] {{"iTiDE", "6"}, {"iMotEmi", "8"}, {"iTipDocAso", "1"}, {"dCdCDERef", CasosDePrueba.CDC_FACTURA_SIMPLE}}),
            // DE-62: F025, F014, F026 y F017 con la comisión (E12-01, I3-20); el resto lo fija comisionLlegaAlXmlE1201
            Map.entry("DE-62", new String[][] {{"iTipTra", "2"}, {"dTotOpe", "1100000"}, {"dComi", "110000"}, {"dTotGralOpe", "1210000"},
                {"dIVAComi", "10000.00"}, {"dTotIVA", "110000.00"}}),
            Map.entry("DE-63", new String[][] {{"iTipOpVN", "2"}, {"dChasis", "ABCDEFGH123456789"}, {"dColor", "BLANCO"}, {"iTipCom", "1"},
                {"dNroMotor", "MOTOR123456"}, {"dAnoFab", "2026"}, {"cTipVeh", "AUTOMOVIL"}, {"dCilin", "1600"}}),
            Map.entry("DE-64", new String[][] {{"#gGrupEner", "1"}, {"dNroMed", "MED-0001"}, {"dActiv", "1"}, {"dCateg", "RES"}, {"dLecAnt", "1000"},
                {"dLecAct", "1250"}, {"dConKwh", "250"}}),
            Map.entry("DE-65", new String[][] {{"dCodEmpSeg", "ASEG0000000000000001"}, {"#gGrupPolSeg", "1"}, {"dNumPoliza", "POL-000123"},
                {"dCodInt*", "POL-0651|POL-0651"}}),
            Map.entry("DE-66", new String[][] {{"dNomCaj", "CAJERO FICTICIO"}, {"dEfectivo", "100000"}, {"dVuelto", "5000"}, {"dDonac", "500"},
                {"dDesDonac", "DONACION FICTICIA"}}),
            Map.entry("DE-67", new String[][] {{"iIndPres", "6"}, {"dCiclo", "NOVIEMBRE 2026"}, {"dFecIniC", "2026-11-01"}, {"dFecFinC", "2026-11-30"},
                {"dVencPag*", "2026-12-10|2026-12-20|2026-12-30"}, {"dContrato", "CONTRATO-0001"}, {"dSalAnt", "150000"}}),
            Map.entry("DE-68", new String[][] {{"dNumLote", "LOTE-2026-01"}, {"dVencMerc", "2027-06-30"}, {"dNSerie", "SERIE-0001"},
                {"dNumPedi", "PEDIDO-0001"}, {"dNumSegui", "SEGUI-0001"}, {"!dNomImp", ""}, {"!dDirImp", ""},
                {"!dNumFir", ""}, {"dNumReg", "REG-0000000000000001"}, {"dNumRegEntCom", "RC-00000000000000001"}}),
            Map.entry("EV-07", new String[][] {{"rGeVeNotRec", null}, {"Id", CasosDePrueba.CDC_FACTURA_SIMPLE}, {"iTipRec", "1"}, {"dRucRec", "80654321"},
                {"dDVRec", "3"}, {"dTotalGs", "110000"}}),
            Map.entry("EV-08", new String[][] {{"rGeVeDescon", null}, {"iTipRec", "1"}, {"mOtEve", "Desconocimiento de prueba del receptor"}}),
            Map.entry("EV-09", new String[][] {{"rGeVeConf", null}, {"iTipConf", "1"}}));

    @BeforeAll
    public static void configurar() {
        cfg = FixturesSifen.configuracion();
        ctx = FixturesSifen.contexto(cfg);
    }

    static Stream<CasosFase12.Caso> documentos() {
        return CasosFase12.documentos().stream();
    }

    static Stream<CasosFase12.CasoEvento> eventos() {
        return CasosFase12.eventos().stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("documentos")
    public void elCandidatoTieneLosCamposDelMtYNoCambia(CasosFase12.Caso caso) throws Exception {
        String xml = caso.construir().generarXml(ctx, cfg);
        afirmarCampos(caso.id(), xml);
        if (CONGELAR != null) {
            Path raiz = Paths.get(CONGELAR);
            Files.createDirectories(raiz.resolve("casos/fase12"));
            Files.createDirectories(raiz.resolve("golden/candidatos"));
            Files.write(raiz.resolve("casos/fase12/" + caso.archivo() + ".xml"), XmlCanonico.entradaDesdeSalida(xml).getBytes(StandardCharsets.UTF_8));
            Files.write(raiz.resolve("golden/candidatos/" + caso.id() + ".xml"), XmlCanonico.canonico(xml));
            return;
        }
        afirmarCandidato(caso.id(), xml);
        String entrada = FixturesSifen.recurso("casos/fase12/" + caso.archivo() + ".xml");
        afirmarCandidato(caso.id(), new DocumentoElectronico(entrada).generarXml(ctx, cfg));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("eventos")
    public void elCandidatoDelEventoTieneSusCamposYNoCambia(CasosFase12.CasoEvento caso) throws Exception {
        String xml = MensajesSifen.mensajeEvento(caso.construir(), cfg);
        afirmarCampos(caso.id(), xml);
        if (CONGELAR != null) {
            Path raiz = Paths.get(CONGELAR);
            Files.createDirectories(raiz.resolve("golden/candidatos"));
            Files.write(raiz.resolve("golden/candidatos/" + caso.id() + ".xml"), XmlCanonico.canonico(xml));
            return;
        }
        afirmarCandidato(caso.id(), xml);
    }

    /** Cada caso con candidato tiene su revisión de campos (y no hay revisiones de casos que no existen). */
    @Test
    public void cadaCandidatoTieneSuRevisionDeCampos() {
        List<String> ids = new ArrayList<>();
        CasosFase12.documentos().forEach(k -> ids.add(k.id()));
        CasosFase12.eventos().forEach(k -> ids.add(k.id()));
        Assertions.assertEquals(ids.stream().sorted().toList(), CAMPOS.keySet().stream().sorted().toList());
    }

    /**
     * E12-01 (S13, I3-20): la comisión cargada con {@code setdComi} llega al XML. Hasta la corrección,
     * {@code TgTotSub.setupSOAPElements} empezaba con {@code resetValues()}, que ponía {@code dComi} en 0: salía
     * {@code dComi 0}, {@code dIVAComi 0.00} y el total sin la comisión (1100000), mientras la validación previa sumaba
     * la comisión al total. Ahora sale lo que pide el MT (F025 110000; F014 · 2365 = F008 − F013 + F025 = 1210000,
     * también en el QR) y {@code dIVAComi} = {@code dComi / 11}, el IVA incluido en la comisión (I3-20, decisión del
     * usuario del 2026-11-25), que {@code dTotIVA} suma.
     */
    @Test
    public void comisionLlegaAlXmlE1201() throws Exception {
        DocumentoElectronico de = CasosFase12.todos().get("DE-62").get();
        Assertions.assertEquals(0, CasosFase12.COMISION.compareTo(de.getgTotSub().getdComi()), "la comisión está cargada antes de generar");
        String xml = de.generarXml(ctx, cfg);
        Assertions.assertEquals("110000", XmlCanonico.texto(xml, "dComi"), "E12-01: F025");
        Assertions.assertEquals("10000.00", XmlCanonico.texto(xml, "dIVAComi"), "F026 = dComi / 11 (I3-20)");
        Assertions.assertEquals("1210000", XmlCanonico.texto(xml, "dTotGralOpe"), "E12-01: el total suma la comisión (F014 · 2365)");
        Assertions.assertEquals("110000.00", XmlCanonico.texto(xml, "dTotIVA"), "F017 incluye F026");
        String qr = XmlCanonico.texto(xml, "dCarQR");
        Assertions.assertTrue(qr.contains("&dTotGralOpe=1210000&") && qr.contains("&dTotIVA=110000.00&"), qr);
        Assertions.assertEquals(0, CasosFase12.COMISION.compareTo(de.getgTotSub().getdComi()), "generar no borra la comisión del objeto");
        Assertions.assertArrayEquals(XmlCanonico.canonico(xml), XmlCanonico.canonico(de.generarXml(ctx, cfg)), "generar dos veces da lo mismo");
        // releída del XML, la comisión se conserva y regenerar da lo mismo
        DocumentoElectronico leido = new DocumentoElectronico(xml);
        Assertions.assertArrayEquals(XmlCanonico.canonico(xml), XmlCanonico.canonico(leido.generarXml(ctx, cfg)));
    }

    // ------------------------------------------------------------------ apoyo

    private static void afirmarCandidato(String id, String xml) throws Exception {
        byte[] esperado = FixturesSifen.recursoBytes("golden/candidatos/" + id + ".xml");
        byte[] obtenido = XmlCanonico.canonico(xml);
        if (!Arrays.equals(esperado, obtenido)) {
            String e = new String(esperado, StandardCharsets.UTF_8);
            String o = new String(obtenido, StandardCharsets.UTF_8);
            int i = 0;
            while (i < e.length() && i < o.length() && e.charAt(i) == o.charAt(i)) {
                i++;
            }
            int desde = Math.max(0, i - 120);
            Assertions.fail("Difiere del candidato " + id + " en el carácter " + i + "\n  candidato: …"
                    + e.substring(desde, Math.min(e.length(), i + 120)) + "\n  jbs      : …" + o.substring(desde, Math.min(o.length(), i + 120)));
        }
    }

    static void afirmarCampos(String id, String xml) throws Exception {
        String[][] campos = CAMPOS.get(id);
        Assertions.assertNotNull(campos, "sin revisión de campos: " + id);
        org.w3c.dom.Document doc = XmlCanonico.parsear(xml);
        List<String> fallas = new ArrayList<>();
        for (String[] c : campos) {
            String tag = c[0];
            String esperado = resolver(c[1]);
            if (tag.startsWith("#")) {
                int n = doc.getElementsByTagNameNS("*", tag.substring(1)).getLength();
                if (n != Integer.parseInt(esperado)) {
                    fallas.add(tag + " = " + n + " (se espera " + esperado + ")");
                }
            } else if (tag.startsWith("!")) {
                if (doc.getElementsByTagNameNS("*", tag.substring(1)).getLength() != 0) {
                    fallas.add(tag.substring(1) + " presente (no debe estar)");
                }
            } else if (tag.endsWith("*")) {
                NodeList l = doc.getElementsByTagNameNS("*", tag.substring(0, tag.length() - 1));
                List<String> v = new ArrayList<>();
                for (int i = 0; i < l.getLength(); i++) {
                    v.add(l.item(i).getTextContent());
                }
                if (!String.join("|", v).equals(esperado)) {
                    fallas.add(tag + " = " + String.join("|", v) + " (se espera " + esperado + ")");
                }
            } else {
                NodeList l = doc.getElementsByTagNameNS("*", tag);
                if (l.getLength() == 0) {
                    fallas.add(tag + " ausente");
                } else if (esperado != null && !esperado.equals(l.item(0).getTextContent())) {
                    fallas.add(tag + " = «" + l.item(0).getTextContent() + "» (se espera «" + esperado + "»)");
                }
            }
        }
        Assertions.assertEquals(List.of(), fallas, id + ": campos del MT (§12.1)");
    }

    private static final Pattern CDC = Pattern.compile("\\{cdc:([^}]+)}");

    private static String resolver(String valor) {
        if (valor == null) {
            return null;
        }
        if ("{infoFisc}".equals(valor)) {
            return CasosFase12.INFO_FISCAL_EXPORTACION;
        }
        Matcher m = CDC.matcher(valor);
        if (!m.matches()) {
            return valor;
        }
        String id = m.group(1);
        try {
            if (CasosFase12.todos().containsKey(id)) {
                return CasosFase12.cdcDe(id);
            }
            return CasosDePrueba.documentos().stream().filter(k -> k.getId().equals(id)).findFirst().orElseThrow().construir().obtenerCDC();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

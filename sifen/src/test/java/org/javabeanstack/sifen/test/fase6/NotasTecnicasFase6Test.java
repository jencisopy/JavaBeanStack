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
package org.javabeanstack.sifen.test.fase6;

import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.fields.request.de.TgCamDEAsoc;
import org.javabeanstack.sifen.core.fields.request.de.TgOblAfe;
import org.javabeanstack.sifen.core.fields.request.de.TgOpeCom;
import org.javabeanstack.sifen.core.types.TTipTra;
import org.javabeanstack.sifen.core.types.TcOblAfe;
import org.javabeanstack.sifen.core.types.TdTipCons;
import org.javabeanstack.sifen.core.types.TiMotivTras;
import org.javabeanstack.sifen.core.types.TiTipDocAso;
import org.javabeanstack.sifen.core.validation.DEValidator;
import org.javabeanstack.sifen.core.validation.ValidationResult;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.internal.helpers.SoapHelper;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Brecha de notas técnicas (plan JBSSIF, Fase 6; CA6-02, CA6-03): obligaciones afectadas (NT 018/022, §6.1), RUC
 * fusionado (NT 023), texto libre del motivo de la remisión (I3-17), orden de {@code gCamDEAsoc} (E6-01), retiro del
 * flag de la NT 013 (§6.3, D25) y horas desde la firma en A004b (§6.5).
 *
 * <p>El golden {@code golden/F6-01.xml} (factura con dos {@code gOblAfe}) no es del catálogo del arnés A/B: el fork
 * no tiene {@code gOblAfe} y {@code CasosDePrueba} se compila también contra él. Se congela con
 * {@code -Djbs.sifen.f6.congelar=<carpeta>}.</p>
 */
public class NotasTecnicasFase6Test {
    private static final String NS = "http://ekuatia.set.gov.py/sifen/xsd";
    private static SifenConfig cfg;
    private static GenerationCtx ctx;

    @BeforeAll
    public static void configurar() {
        cfg = FixturesSifen.configuracion();
        ctx = FixturesSifen.contexto(cfg);
    }

    private static DocumentoElectronico caso(String id) {
        return CasosDePrueba.documentos().stream().filter(c -> c.getId().equals(id)).findFirst().orElseThrow().construir();
    }

    private static DocumentoElectronico conObligaciones(TcOblAfe... codigos) {
        DocumentoElectronico de = caso("DE-05");
        List<TgOblAfe> l = new ArrayList<>();
        for (TcOblAfe c : codigos) {
            l.add(new TgOblAfe(c));
        }
        de.getgDatGralOpe().getgOpeCom().setgOblAfeList(l);
        return de;
    }

    private static List<String> hijos(Element e) {
        List<String> l = new ArrayList<>();
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element h) {
                l.add(h.getLocalName());
            }
        }
        return l;
    }

    private static Element primero(String xml, String nombre) throws Exception {
        return (Element) XmlCanonico.parsear(xml).getElementsByTagNameNS(NS, nombre).item(0);
    }

    private static ValidationResult validar(DocumentoElectronico de) {
        return DEValidator.standard().validate(de, ctx);
    }

    // ------------------------------------------------------------------ NT 018 / NT 022 (§6.1)

    @Test
    public void tabla12TieneLosOnceCodigosDeLaNt018() {
        Assertions.assertEquals(11, TcOblAfe.values().length);
        Assertions.assertEquals("IMPUESTO A LA RENTA EMPRESARIAL - RÉGIMEN GENERAL", TcOblAfe.getByVal((short) 700).getDescripcion());
        Assertions.assertEquals("IMPUESTO AL VALOR AGREGADO - GRAVADAS Y EXONERADAS - EXPORTADORES", TcOblAfe.getByVal((short) 211).getDescripcion());
        Assertions.assertEquals("TRIBUTO UNICO MAQUILA", TcOblAfe.getByVal((short) 143).getDescripcion());
        Assertions.assertNull(TcOblAfe.getByVal((short) 999));
        for (TcOblAfe t : TcOblAfe.values()) {
            int largo = t.getDescripcion().length();
            Assertions.assertTrue(largo >= 21 && largo <= 65, t + ": dDesOblAfe es A21-65");
        }
    }

    /** CA6-02: gOblAfe al final de gOpeCom (D030 bajo D010), con código y descripción de la Tabla 12. */
    @Test
    public void obligacionesAfectadasAlFinalDeGOpeCom() throws Exception {
        String xml = conObligaciones(TcOblAfe.IRE_REGIMEN_GENERAL, TcOblAfe.IVA_GRAVADAS_EXONERADAS_EXPORTADORES).generarXml(ctx, cfg);
        List<String> h = hijos(primero(xml, "gOpeCom"));
        Assertions.assertEquals(List.of("gOblAfe", "gOblAfe"), h.subList(h.size() - 2, h.size()), "gOblAfe va después de los campos de gOpeCom: " + h);
        Element o = primero(xml, "gOblAfe");
        Assertions.assertEquals(List.of("cOblAfe", "dDesOblAfe"), hijos(o));
        Assertions.assertEquals("700", XmlCanonico.texto(xml, "cOblAfe"));
        Assertions.assertEquals("IMPUESTO A LA RENTA EMPRESARIAL - RÉGIMEN GENERAL", XmlCanonico.texto(xml, "dDesOblAfe"));
        // releer y regenerar da lo mismo
        DocumentoElectronico leido = new DocumentoElectronico(xml);
        Assertions.assertEquals(2, leido.getgDatGralOpe().getgOpeCom().getgOblAfeList().size());
        Assertions.assertEquals(TcOblAfe.IVA_GRAVADAS_EXONERADAS_EXPORTADORES, leido.getgDatGralOpe().getgOpeCom().getgOblAfeList().get(1).getcOblAfe());
        Assertions.assertArrayEquals(XmlCanonico.canonico(xml), XmlCanonico.canonico(leido.generarXml(ctx, cfg)));
        // sin obligaciones, gOpeCom no cambia (golden del catálogo intactos)
        Assertions.assertFalse(caso("DE-05").generarXml(ctx, cfg).contains("gOblAfe"));
    }

    /** Golden F6-01 (factura PYG con dos gOblAfe), generado con jbs-sifen. */
    @Test
    public void goldenF601() throws Exception {
        String xml = conObligaciones(TcOblAfe.IRE_REGIMEN_GENERAL, TcOblAfe.IVA_GRAVADAS_EXONERADAS_EXPORTADORES).generarXml(ctx, cfg);
        byte[] obtenido = XmlCanonico.canonico(xml);
        String congelar = System.getProperty("jbs.sifen.f6.congelar");
        if (congelar != null) {
            Path dir = Paths.get(congelar);
            Files.createDirectories(dir);
            Files.write(dir.resolve("F6-01.xml"), obtenido);
        }
        Assertions.assertArrayEquals(FixturesSifen.recursoBytes("golden/F6-01.xml"), obtenido, "golden F6-01 (gOblAfe)");
    }

    @Test
    public void validacionDeLasObligaciones() {
        Assertions.assertFalse(hayError(validar(conObligaciones(TcOblAfe.IRE_SIMPLE, TcOblAfe.ISC_GENERAL)), "D031", "D031a", "JBS-D-62"));
        Assertions.assertTrue(validar(conObligaciones(TcOblAfe.IRE_SIMPLE, TcOblAfe.IRE_SIMPLE)).contains("D031a"), "código repetido (NT 022)");
        Assertions.assertTrue(validar(conObligaciones((TcOblAfe) null)).contains("D031"), "sin código");
        TcOblAfe[] doce = new TcOblAfe[12];
        for (int i = 0; i < 12; i++) {
            doce[i] = TcOblAfe.values()[i % 11];
        }
        Assertions.assertTrue(validar(conObligaciones(doce)).contains("JBS-D-62"), "más de 11");
    }

    private static boolean hayError(ValidationResult r, String... codigos) {
        for (String c : codigos) {
            if (r.contains(c)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ NT 023 dRucFus (§6.2)

    @Test
    public void rucFusionadoEnLaNotaDeCredito() throws Exception {
        DocumentoElectronico nc = caso("DE-21");
        nc.getgCamDEAsocList().get(0).setdRucFus(CasosDePrueba.RUC_EMISOR);
        Assertions.assertFalse(hayError(validar(nc), "H018", "H018a", "JBS-H-12"));
        String xml = nc.generarXml(ctx, cfg);
        List<String> h = hijos(primero(xml, "gCamDEAsoc"));
        Assertions.assertEquals("dRucFus", h.get(h.size() - 1), "H018 es el último del grupo: " + h);
        Assertions.assertEquals(CasosDePrueba.RUC_EMISOR, new DocumentoElectronico(xml).getgCamDEAsocList().get(0).getdRucFus());
        nc.getgCamDEAsocList().get(0).setdRucFus("80000005");
        Assertions.assertTrue(validar(nc).contains("H018"), "no es el RUC del CDC asociado");
        DocumentoElectronico fe = caso("DE-20");
        fe.getgCamDEAsocList().get(0).setdRucFus(CasosDePrueba.RUC_EMISOR);
        Assertions.assertTrue(validar(fe).contains("H018a"), "solo NC, ND y NR");
        Assertions.assertFalse(caso("DE-21").generarXml(ctx, cfg).contains("dRucFus"));
    }

    /**
     * El caso real de la fusión (NT 023, H004g/H018): la NC asocia un CDC de la empresa absorbida —otro RUC que el del
     * emisor— e informa ese RUC en {@code dRucFus}. Ninguna regla local lo frena.
     */
    @Test
    public void rucFusionadoDeOtraEmpresa() throws Exception {
        String absorbida = "80000005";
        String base = CasosDePrueba.CDC_FACTURA_SIMPLE.substring(0, 2) + absorbida
                + org.javabeanstack.sifen.internal.util.SifenUtil.generateDv(absorbida)
                + CasosDePrueba.CDC_FACTURA_SIMPLE.substring(11, 43);
        String cdc = base + org.javabeanstack.sifen.internal.util.SifenUtil.generateDv(base);
        DocumentoElectronico nc = caso("DE-21");
        nc.getgCamDEAsocList().get(0).setdCdCDERef(cdc);
        nc.getgCamDEAsocList().get(0).setdRucFus(absorbida);
        ValidationResult r = validar(nc);
        Assertions.assertTrue(r.getErrors().stream().noneMatch(e -> e.getPath().contains("gCamDEAsoc")), r.toString());
        Assertions.assertEquals(absorbida, XmlCanonico.texto(nc.generarXml(ctx, cfg), "dRucFus"));
    }

    /** Un gOblAfe nulo o sin código no da NPE sin la validación previa: no se escribe (D031 lo reporta al validar). */
    @Test
    public void obligacionNulaNoDaNpe() throws Exception {
        DocumentoElectronico de = conObligaciones(TcOblAfe.IRE_SIMPLE);
        de.getgDatGralOpe().getgOpeCom().getgOblAfeList().add(null);
        de.getgDatGralOpe().getgOpeCom().getgOblAfeList().add(new TgOblAfe());
        String xml = de.generarXml(ctx, cfg);
        Assertions.assertEquals(1, XmlCanonico.parsear(xml).getElementsByTagNameNS(NS, "gOblAfe").getLength());
        Assertions.assertTrue(validar(de).contains("D031"));
    }

    /**
     * I6-09 (NT 026, §1.1, p. 1): en B2G cada código DNCP se escribe si viene, sin depender del otro; antes un
     * {@code dDncpE} sin {@code dDncpG} se perdía sin aviso. Fuera de B2G no se escriben y JBS-E-194 avisa.
     */
    @Test
    public void dncpEspecificoSinGeneralEnB2G() throws Exception {
        DocumentoElectronico b2g = caso("DE-15");
        b2g.getgDtipDE().getgCamItemList().get(0).setdDncpG(null);
        Assertions.assertFalse(hayError(validar(b2g), "JBS-E-63", "JBS-E-65"));
        String xml = b2g.generarXml(ctx, cfg);
        Assertions.assertEquals("1234", XmlCanonico.texto(xml, "dDncpE"));
        Assertions.assertNull(XmlCanonico.texto(xml, "dDncpG"));
        b2g.getgDtipDE().getgCamItemList().get(0).setdDncpE("12");
        Assertions.assertTrue(validar(b2g).contains("JBS-E-65"), "largo de dDncpE aunque falte dDncpG");
        DocumentoElectronico b2c = caso("DE-05");
        b2c.getgDtipDE().getgCamItemList().get(0).setdDncpE("1234");
        Assertions.assertTrue(validar(b2c).getWarnings().stream().anyMatch(e -> e.getCode().equals("JBS-E-194")));
        Assertions.assertNull(XmlCanonico.texto(b2c.generarXml(ctx, cfg), "dDncpE"), "fuera de B2G no se escribe");
        // los dos juntos, como antes (golden DE-15 sin cambio)
        Assertions.assertEquals("12345678", XmlCanonico.texto(caso("DE-15").generarXml(ctx, cfg), "dDncpG"));
    }

    /**
     * E8-01: la nominación con ciudad pasa la validación local (JBS-EVENTO-43, la guarda de E0-12, está de baja) y la
     * descripción de la ciudad la exige JBS-EVENTO-56 (GENFE022) si hay {@code cCiuRec}.
     */
    @Test
    public void nominacionConCiudadPasaLaValidacionLocal() throws Exception {
        org.javabeanstack.sifen.core.beans.EventosDE ev = CasosDePrueba.eventoNominacionConDireccion((short) 1);
        ValidationResult r = org.javabeanstack.sifen.core.validation.EventValidator.standard().validate(ev, ctx);
        Assertions.assertFalse(hayError(r, "JBS-EVENTO-43", "JBS-EVENTO-56"), r.toString());
        ev.getrGesEveList().get(0).getgGroupTiEvt().getrGeVeNom().setdDesCiuRec(null);
        Assertions.assertTrue(org.javabeanstack.sifen.core.validation.EventValidator.standard().validate(ev, ctx).contains("JBS-EVENTO-56"));
    }

    /** E6-01: dNumComRet, dNumResCF y después la constancia, como el XSD v150. */
    @Test
    public void ordenDeGCamDEAsoc() throws Exception {
        TgCamDEAsoc a = new TgCamDEAsoc();
        a.setiTipDocAso(TiTipDocAso.CONSTANCIA_ELECTRONICA);
        a.setiTipCons(TdTipCons.CONSTANCIA_MICROPRODUCTORES);
        a.setdNumCons(new BigInteger("12345678901"));
        a.setdNumControl("12345678");
        a.setdNumComRet("001-001-0000001");
        a.setdNumResCF("123456");
        a.setdRucFus("80123456");
        SOAPElement de = SoapHelper.createSoapMessage().getSOAPBody().addChildElement("DE");
        a.setupSOAPElements(de, TTipTra.getByVal((short) 12), true);
        Element g = (Element) de.getFirstChild();
        Assertions.assertEquals(List.of("iTipDocAso", "dDesTipDocAso", "dNumComRet", "dNumResCF", "iTipCons", "dDesTipCons",
                "dNumCons", "dNumControl", "dRucFus"), hijos(g));
    }

    // ------------------------------------------------------------------ I3-17 dDesMotEmiNR (§6.2)

    @Test
    public void motivoOtroDeLaRemisionLlevaElTextoLibre() throws Exception {
        DocumentoElectronico nr = caso("DE-23");
        nr.getgDtipDE().getgCamNRE().setiMotEmiNR(TiMotivTras.OTRO);
        Assertions.assertTrue(validar(nr).contains("E502"), "«Otro» sin texto");
        nr.getgDtipDE().getgCamNRE().setdDesMotEmiNR("TRASLADO A DEPOSITO PROPIO");
        Assertions.assertFalse(validar(nr).contains("E502"));
        String xml = nr.generarXml(ctx, cfg);
        Assertions.assertEquals("TRASLADO A DEPOSITO PROPIO", XmlCanonico.texto(xml, "dDesMotEmiNR"));
        // con otro motivo sale la descripción del código aunque haya texto
        nr.getgDtipDE().getgCamNRE().setiMotEmiNR(caso("DE-23").getgDtipDE().getgCamNRE().getiMotEmiNR());
        Assertions.assertEquals(caso("DE-23").getgDtipDE().getgCamNRE().getiMotEmiNR().getDescripcion(),
                XmlCanonico.texto(nr.generarXml(ctx, cfg), "dDesMotEmiNR"));
    }

    // ------------------------------------------------------------------ NT 013 (§6.3, D25)

    @Test
    @SuppressWarnings("deprecation")
    public void laNotaTecnica13NoSePuedeApagar() throws Exception {
        Properties p = new Properties();
        p.setProperty("sifen.ambiente", "DEV");
        p.setProperty("sifen.certificado_cliente.usar", "true");
        p.setProperty("sifen.certificado_cliente.tipo", "PFX");
        p.setProperty("sifen.certificado_cliente.archivo", FixturesSifen.rutaPfx().toString());
        p.setProperty("sifen.certificado_cliente.contrasena", FixturesSifen.PFX_CLAVE);
        p.setProperty("sifen.csc.id", "0001");
        p.setProperty("sifen.csc", "ABCD0000000000000000000000000000");
        p.setProperty("sifen.habilitar_nota_tecnica_13", "false");
        SifenConfig c = SifenConfig.load(p);
        Assertions.assertTrue(c.isHabilitarNotaTecnica13(), "D25: siempre true");
        c.setHabilitarNotaTecnica13(false);
        Assertions.assertTrue(c.isHabilitarNotaTecnica13(), "el setter no tiene efecto");
        Assertions.assertTrue(c.getUnknownProperties().isEmpty(), "la clave retirada no es desconocida (JBS-CFG-11)");
        Assertions.assertNotNull(XmlCanonico.texto(caso("DE-05").generarXml(ctx, cfg), "dBasExe"));
    }

    // ------------------------------------------------------------------ §6.5 A004b

    @Test
    public void a004bMuestraLasHorasDesdeLaFirma() {
        DocumentoElectronico de = caso("DE-05");
        de.setdFecFirma(java.time.LocalDateTime.now().minusHours(80).truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        ValidationResult r = validar(de);
        Assertions.assertTrue(r.contains("A004b"));
        String valor = r.getWarnings().stream().filter(e -> e.getCode().equals("A004b")).findFirst().orElseThrow().getValue().orElse("");
        Assertions.assertTrue(valor.endsWith("(80 h; plazo: 72 h)"), valor);
    }
}

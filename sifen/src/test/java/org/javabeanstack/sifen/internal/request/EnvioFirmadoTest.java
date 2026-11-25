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
package org.javabeanstack.sifen.internal.request;

import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.EventosDE;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.types.TTiDE;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.internal.helpers.SignatureHelper;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.javabeanstack.sifen.internal.util.SifenUtil;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jakarta.xml.soap.SOAPMessage;
import java.util.List;

/**
 * Emisión en dos pasos del carril A (plan JBSSIF, §4.1, §4.4, parte A de §10.3; CA4-02, CA4-04, CA10-05,
 * CA10-07): firmar una vez, leer el firmado sin regenerarlo, enviarlo tal cual y armar los eventos de
 * cancelación e inutilización. El cuerpo del pedido se arma con {@code setupSoapMessage}, el mismo mensaje
 * que escribe {@code SoapHelper} en la conexión; el envío por {@code ISifenClient} con el servidor embebido
 * es de los carriles F y C.
 */
public class EnvioFirmadoTest {
    private static SifenConfig cfg;
    private static GenerationCtx ctx;

    @BeforeAll
    public static void configurar() {
        cfg = FixturesSifen.configuracion();
        ctx = FixturesSifen.contexto(cfg);
    }

    private static CasosDePrueba.Caso caso(String id) {
        return CasosDePrueba.documentos().stream().filter(c -> c.getId().equals(id)).findFirst().orElseThrow();
    }

    /** El documento del caso: por objeto si tiene armador; si no, leído de su XML de entrada (DE-01). */
    private static DocumentoElectronico doc(String id) throws Exception {
        CasosDePrueba.Caso c = caso(id);
        return c.tieneConstructor() ? c.construir() : c.leer(FixturesSifen.entrada(c));
    }

    /** C14N del {@code rDE} (el primero) de un XML. */
    private static byte[] canonicoRde(String xml) throws Exception {
        return XmlCanonico.canonico(SifenUtil.serialize(SifenUtil.parseRDE(xml)));
    }

    private static String cuerpo(SOAPMessage m) {
        return ResponseUtil.getXmlFromMessage(m, false);
    }

    private static void codigo(String esperado, ThrowingRunnable r) {
        SifenException e = Assertions.assertThrows(SifenException.class, r::run);
        Assertions.assertEquals(esperado, e.getCode(), e.getMessage());
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }

    // ------------------------------------------------------------------ firmar una vez (§4.1)

    @Test
    public void firmaUnaVezYElTextoValida() throws Exception {
        DocumentoElectronico de = doc("DE-05");
        String xml = de.generateSignedXml(cfg);
        Assertions.assertTrue(xml.startsWith("<rDE xmlns=\"http://ekuatia.set.gov.py/sifen/xsd\""), xml.substring(0, 80));
        Assertions.assertSame(xml, de.getSignedXml());
        Assertions.assertTrue(SignatureHelper.validateSignature(xml, "XML").isValido(), "la firma del texto guardado valida tal cual");
        Assertions.assertEquals(XmlCanonico.texto(xml, "dCarQR"), de.getEnlaceQR(), "QR de esta firma");
        // el mismo DE por el camino heredado: el rDE es el mismo (misma firma: RSA-SHA256 determinista)
        String sobre = doc("DE-05").generarXml(ctx, cfg);
        Assertions.assertArrayEquals(canonicoRde(sobre), canonicoRde(xml));
    }

    @Test
    public void volverAFirmarPorElCaminoHeredadoInvalidaElGuardado() throws Exception {
        DocumentoElectronico de = doc("DE-01");
        de.generateSignedXml(cfg);
        Assertions.assertNotNull(de.getSignedXml());
        de.generarXml(ctx, cfg);
        Assertions.assertNull(de.getSignedXml());
    }

    @Test
    public void sinConfiguracionEsJbsCfg01() {
        codigo("JBS-CFG-01", () -> doc("DE-01").generateSignedXml(null));
    }

    // ------------------------------------------------------------------ leer el firmado (§4.1, §4.4)

    @Test
    public void leerElFirmadoNoRecalcula() throws Exception {
        DocumentoElectronico de = doc("DE-05");
        String xml = de.generateSignedXml(cfg);
        SifenUtil.SignedRDE r = SifenUtil.readSignedRDE(xml);
        Assertions.assertSame(xml, r.xml(), "con el rDE como raíz, el mismo texto");
        Assertions.assertEquals(de.getId(), r.cdc());
        Assertions.assertEquals(de.getEnlaceQR(), r.qrLink());
        Assertions.assertEquals(XmlCanonico.texto(xml, "DigestValue"), r.digestValue());
        Assertions.assertEquals(CasosDePrueba.FECHA_FIRMA, r.signatureDate());
        Assertions.assertEquals(TTiDE.FACTURA_ELECTRONICA, r.documentType());
        Assertions.assertEquals("001-001-0000005", r.number());
        Assertions.assertEquals(CasosDePrueba.RUC_EMISOR + "-" + CasosDePrueba.DV_EMISOR, r.issuerRuc());
    }

    /** CA4-04: el QR y los datos del KuDE salen del XML firmado, no de una firma nueva. */
    @Test
    public void elDocumentoLeidoTraeElQrYLosTotalesDelFirmado() throws Exception {
        DocumentoElectronico de = doc("DE-05");
        String xml = de.generateSignedXml(cfg);
        DocumentoElectronico leido = new DocumentoElectronico(xml);
        Assertions.assertEquals(XmlCanonico.texto(xml, "dCarQR"), leido.getEnlaceQR());
        Assertions.assertEquals(de.getId(), leido.getId());
        Assertions.assertEquals(CasosDePrueba.FECHA_FIRMA, leido.getdFecFirma());
        Assertions.assertEquals(XmlCanonico.texto(xml, "dTotGralOpe"), leido.getgTotSub().getdTotGralOpe().toPlainString());
        Assertions.assertEquals(XmlCanonico.texto(xml, "dTotIVA"), leido.getgTotSub().getdTotIVA().toPlainString());
    }

    /** El sobre de {@code generarXml} también se lee: se toma su {@code rDE}, que valida solo. */
    @Test
    public void leeElRdeDentroDelSobreHeredado() throws Exception {
        String sobre = doc("DE-01").generarXml(ctx, cfg);
        SifenUtil.SignedRDE r = SifenUtil.readSignedRDE(sobre);
        Assertions.assertTrue(r.xml().startsWith("<rDE"));
        Assertions.assertTrue(SignatureHelper.validateSignature(r.xml(), "XML").isValido());
        Assertions.assertArrayEquals(canonicoRde(sobre), XmlCanonico.canonico(r.xml()));
    }

    @Test
    public void lecturaRechazaLoQueNoEsUnRdeFirmado() throws Exception {
        String xml = doc("DE-05").generateSignedXml(cfg);
        codigo("JBS-XML-01", () -> SifenUtil.readSignedRDE(" "));
        codigo("JBS-XML-03", () -> SifenUtil.readSignedRDE("<rDE"));
        codigo("JBS-XML-03", () -> SifenUtil.readSignedRDE("<!DOCTYPE r [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><r>&x;</r>"));
        codigo("JBS-XML-02", () -> SifenUtil.readSignedRDE("<otro/>"));
        codigo("JBS-XML-06", () -> SifenUtil.readSignedRDE(xml.replaceAll("(?s)<Signature .*</Signature>", "")));
        codigo("JBS-XML-06", () -> SifenUtil.readSignedRDE(xml.replaceAll("<gCamFuFD>.*</gCamFuFD>", "")));
        // QR de otra firma (E0-02)
        String otro = doc("DE-01").generateSignedXml(cfg);
        String qrAjeno = xml.replaceAll("<dCarQR>[^<]*</dCarQR>", java.util.regex.Matcher.quoteReplacement(
                "<dCarQR>" + XmlCanonico.texto(otro, "dCarQR").replace("&", "&amp;") + "</dCarQR>"));
        codigo("JBS-XML-07", () -> SifenUtil.readSignedRDE(qrAjeno));
        // CDC que no corresponde a los datos
        codigo("JBS-XML-07", () -> SifenUtil.readSignedRDE(xml.replace("<dNumDoc>0000005</dNumDoc>", "<dNumDoc>0000006</dNumDoc>")));
    }

    // ------------------------------------------------------------------ enviar tal cual (§4.1, CA4-02, CA10-05)

    @Test
    public void elPedidoLlevaElRdeFirmadoTalCual() throws Exception {
        String xml = doc("DE-05").generateSignedXml(cfg);
        ReqRecDe req = new ReqRecDe(7L, cfg);
        req.setSignedXml(xml);
        String body = cuerpo(req.setupSoapMessage(ctx));
        Assertions.assertEquals("7", XmlCanonico.texto(body, "dId"));
        Assertions.assertEquals(XmlCanonico.texto(xml, "DigestValue"), XmlCanonico.texto(body, "DigestValue"));
        Assertions.assertEquals(XmlCanonico.texto(xml, "SignatureValue"), XmlCanonico.texto(body, "SignatureValue"));
        Assertions.assertArrayEquals(XmlCanonico.canonico(xml), canonicoRde(body), "C14N del rDE enviado = el guardado");
        Assertions.assertTrue(SignatureHelper.validateSignature(body, "XML").isValido(), "la firma valida dentro del sobre");
    }

    /**
     * El DE aprobado del caso de redondeo en USD (escalas del fork: {@code dTotGralOpe 8517.500}) se envía sin regenerarlo:
     * regenerado saldría con {@code 8517.50} y otra firma (E0-01, E0-03).
     */
    @Test
    public void elAprobadoUsdConRedondeoSeEnviaSinRegenerar() throws Exception {
        String aprobado = FixturesSifen.recurso("casos/caso-usd-con-redondeo-aprobado.xml");
        SifenUtil.SignedRDE r = SifenUtil.readSignedRDE(aprobado);
        ReqRecDe req = new ReqRecDe(1L, cfg);
        req.setSignedXml(r.xml());
        String body = cuerpo(req.setupSoapMessage(ctx));
        Assertions.assertArrayEquals(canonicoRde(aprobado), canonicoRde(body));
        Assertions.assertEquals("8517.500", XmlCanonico.texto(body, "dTotGralOpe"));
        Assertions.assertEquals(XmlCanonico.texto(aprobado, "DigestValue"), XmlCanonico.texto(body, "DigestValue"));
        String regenerado = new DocumentoElectronico(aprobado).generarXml(ctx, cfg);
        Assertions.assertEquals("8517.50", XmlCanonico.texto(regenerado, "dTotGralOpe"));
    }

    @Test
    public void loteConLosRdeFirmadosTalCual() throws Exception {
        String a = doc("DE-01").generateSignedXml(cfg);
        String b = doc("DE-05").generateSignedXml(cfg);
        ReqRecLoteDe req = new ReqRecLoteDe(3L, cfg);
        req.setSignedXmlList(List.of(a, b));
        String lote = MensajesSifen.contenidoLote(cuerpo(req.setupSoapMessage(ctx)));
        NodeList rdes = XmlCanonico.parsear(lote).getElementsByTagNameNS("http://ekuatia.set.gov.py/sifen/xsd", "rDE");
        Assertions.assertEquals(2, rdes.getLength());
        Assertions.assertArrayEquals(XmlCanonico.canonico(a), XmlCanonico.canonico(SifenUtil.serialize((Element) rdes.item(0))));
        Assertions.assertArrayEquals(XmlCanonico.canonico(b), XmlCanonico.canonico(SifenUtil.serialize((Element) rdes.item(1))));
        // el mismo lote por el camino heredado da los mismos rDE (golden LT-01 sin cambio)
        ReqRecLoteDe heredado = new ReqRecLoteDe(3L, cfg);
        heredado.setDEList(List.of(doc("DE-01"), doc("DE-05")));
        Assertions.assertEquals(XmlCanonico.canonicoTexto(lote), XmlCanonico.canonicoTexto(MensajesSifen.contenidoLote(cuerpo(heredado.setupSoapMessage(ctx)))));
    }

    // ------------------------------------------------------------------ eventos (parte A de §10.3, CA10-07)

    @Test
    public void cancelacionIgualAlGoldenEv01() throws Exception {
        EventosDE ev = ReqRecEventoDe.cancellation("1", CasosDePrueba.FECHA_FIRMA, CasosDePrueba.CDC_FACTURA_SIMPLE,
                "Cancelacion de prueba del documento");
        Assertions.assertArrayEquals(FixturesSifen.recursoBytes("golden/EV-01.xml"),
                XmlCanonico.canonico(MensajesSifen.mensajeEvento(ev, cfg)));
    }

    @Test
    public void inutilizacionIgualAlGoldenEv02() throws Exception {
        EventosDE ev = ReqRecEventoDe.numberInvalidation("2", CasosDePrueba.FECHA_FIRMA, CasosDePrueba.TIMBRADO, "1", "1",
                "50", "55", TTiDE.FACTURA_ELECTRONICA, "Inutilizacion de prueba de numeracion");
        Assertions.assertArrayEquals(FixturesSifen.recursoBytes("golden/EV-02.xml"),
                XmlCanonico.canonico(MensajesSifen.mensajeEvento(ev, cfg)));
    }
}

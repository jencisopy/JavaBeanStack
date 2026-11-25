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

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.javabeanstack.sifen.api.BatchSendResult;
import org.javabeanstack.sifen.api.BatchStatus;
import org.javabeanstack.sifen.api.EventResult;
import org.javabeanstack.sifen.api.NumberRange;
import org.javabeanstack.sifen.api.ResultStatus;
import org.javabeanstack.sifen.api.SendResult;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.exceptions.ErrorCategory;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.types.TTiDE;
import org.javabeanstack.sifen.core.validation.SifenValidationException;
import org.javabeanstack.sifen.core.validation.ValidationContext;
import org.javabeanstack.sifen.simulator.RecordedRequest;
import org.javabeanstack.sifen.simulator.Scenario;
import org.javabeanstack.sifen.simulator.SifenSimulator;
import org.javabeanstack.sifen.simulator.SimulatedService;
import org.javabeanstack.sifen.spi.ISendRegistry;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Emisión en dos pasos, lote, eventos e idempotencia de {@link SifenClient} contra el simulador (plan
 * JBSSIF, §10.3 y §10.4; CA10-05, CA10-06, CA10-07; D41). Sin red: el simulador escucha en
 * {@code 127.0.0.1} y nunca se contacta a SIFEN (D28).
 */
public class SifenClientEmissionTest {
    private static final Pattern ID = Pattern.compile("[0-9a-f]{8}");
    private static final String MOTIVO = "Cancelacion de prueba del documento";
    private static SifenSimulator sim;
    private static final AtomicInteger NUMERO = new AtomicInteger(100);

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

    private static ISifenClient cliente() throws SifenException {
        return SifenClient.create(sim.config());
    }

    /** El DE del caso 4 del catálogo con un número propio y las leyendas de DEV (Q14). */
    private static DocumentoElectronico documento() {
        DocumentoElectronico de = CasosDePrueba.documentos().get(4).construir();
        de.getgDatGralOpe().getgEmis().setdNomEmi("DE generado en ambiente de prueba - sin valor comercial ni fiscal");
        de.getgDtipDE().getgCamItemList().get(0).setdDesProSer(
                "DOCUMENTO ELECTRÓNICO SIN VALOR COMERCIAL NI FISCAL - GENERADO EN AMBIENTE DE PRUEBA");
        de.getgTimb().setdNumDoc(String.format("%07d", NUMERO.incrementAndGet()));
        return de;
    }

    private static List<RecordedRequest> pedidos(SimulatedService s) {
        return sim.requests(s);
    }

    // ------------------------------------------------------------------ §10.3: firmar, leer, enviar

    @Test
    public void firmarDevuelveElDocumentoFirmadoCompleto() throws Exception {
        ISifenClient c = cliente();
        DocumentoElectronico de = documento();
        SignedDocument doc = c.sign(de);
        Assertions.assertEquals(44, doc.getCdc().length());
        Assertions.assertEquals(XmlCanonico.texto(doc.getXml(), "DigestValue"), doc.getDigestValue());
        Assertions.assertEquals(XmlCanonico.texto(doc.getXml(), "dCarQR"), doc.getQRLink(), "el QR es el dCarQR del XML (CA10-05)");
        Assertions.assertEquals(de.getdFecFirma(), doc.getSignatureDate());
        Assertions.assertEquals(doc.getSignatureDate().plusHours(72), doc.getSubmissionDeadline());
        Assertions.assertEquals(TTiDE.FACTURA_ELECTRONICA, doc.getDocumentType());
        Assertions.assertEquals("80123456-5", doc.getIssuerRuc());
        Assertions.assertEquals(SifenConfig.Environment.DEV, doc.getEnvironment());
        Assertions.assertTrue(doc.getNumber().matches("\\d{3}-\\d{3}-\\d{7}"), doc.getNumber());
        Assertions.assertEquals(doc.getCdc(), doc.getDocument().getId(), "getDocument() relee el XML");
        Assertions.assertTrue(pedidos(SimulatedService.RECEIVE_DE).isEmpty(), "firmar no contacta a SIFEN");
    }

    @Test
    public void firmarCompletaLaFechaDeFirmaYElCodigoDeSeguridadSiFaltan() throws Exception {
        DocumentoElectronico de = documento();
        de.setdFecFirma(null);
        de.getgOpeDE().setdCodSeg(null);
        SignedDocument doc = cliente().sign(de);
        Assertions.assertNotNull(de.getdFecFirma());
        Assertions.assertTrue(de.getgOpeDE().getdCodSeg().matches("\\d{9}"), de.getgOpeDE().getdCodSeg());
        Assertions.assertEquals(de.getgOpeDE().getdCodSeg(), doc.getCdc().substring(34, 43), "el dCodSeg va en el CDC");
        DocumentoElectronico fijo = documento();
        String cdcFijo = cliente().sign(fijo).getCdc();
        Assertions.assertEquals(CasosDePrueba.documentos().get(4).construir().getgOpeDE().getdCodSeg(),
                cdcFijo.substring(34, 43), "respeta el dCodSeg cargado");
    }

    @Test
    public void firmarSinDocumentoOConContenidoInvalidoLanzaLaLista() throws Exception {
        ISifenClient c = cliente();
        SifenValidationException nulo = Assertions.assertThrows(SifenValidationException.class, () -> c.sign(null));
        Assertions.assertEquals("JBS-VAL-01", nulo.getCode());
        DocumentoElectronico de = documento();
        de.getgTimb().setdNumDoc(null);
        SifenValidationException e = Assertions.assertThrows(SifenValidationException.class, () -> c.sign(de));
        Assertions.assertFalse(e.getErrors().isEmpty());
        Assertions.assertEquals(ErrorCategory.DE_CONTENT, e.getCategory());
        Assertions.assertTrue(ID.matcher(e.getCorrelationId()).matches());
    }

    @Test
    public void leerFirmadoDevuelveElMismoDocumento() throws Exception {
        ISifenClient c = cliente();
        SignedDocument doc = c.sign(documento());
        SignedDocument leido = c.readSigned(doc.getXml());
        Assertions.assertEquals(doc.getXml(), leido.getXml(), "getXml() byte a byte");
        Assertions.assertEquals(doc.getCdc(), leido.getCdc());
        Assertions.assertEquals(doc.getDigestValue(), leido.getDigestValue());
        Assertions.assertEquals(doc.getQRLink(), leido.getQRLink());
        Assertions.assertEquals(doc.getSignatureDate(), leido.getSignatureDate());
        Assertions.assertEquals(doc.getNumber(), leido.getNumber());
        Assertions.assertEquals(SifenConfig.Environment.DEV, leido.getEnvironment(), "por la URL del QR");
        SifenException sinFirma = Assertions.assertThrows(SifenException.class,
                () -> c.readSigned(doc.getXml().replaceAll("(?s)<Signature .*</Signature>", "")));
        Assertions.assertEquals("JBS-XML-06", sinFirma.getCode());
        SifenException vacio = Assertions.assertThrows(SifenException.class, () -> c.readSigned(" "));
        Assertions.assertEquals("JBS-XML-01", vacio.getCode());
    }

    /** CA10-05: firmar → leer el XML guardado → enviar: llega el mismo {@code DigestValue} y {@code SignatureValue}. */
    @Test
    public void enviarTransmiteElXmlFirmadoTalCual() throws Exception {
        ISifenClient c = cliente();
        SignedDocument doc = c.sign(documento());
        SendResult r = c.send(c.readSigned(doc.getXml()));
        Assertions.assertEquals(ResultStatus.APPROVED, r.getStatus());
        Assertions.assertFalse(r.isDigestMismatch());
        Assertions.assertFalse(r.isAlreadyRegistered());
        Assertions.assertEquals(doc.getCdc(), r.getCdc());
        Assertions.assertEquals("0260", r.getResponseCode());
        Assertions.assertTrue(ID.matcher(r.getOperationId()).matches());
        List<RecordedRequest> p = pedidos(SimulatedService.RECEIVE_DE);
        Assertions.assertEquals(1, p.size());
        Assertions.assertEquals(XmlCanonico.texto(doc.getXml(), "DigestValue"), p.get(0).text("DigestValue").trim());
        // el simulador da el texto sin decodificar las referencias (&#13; de los saltos de línea del Base64)
        Assertions.assertEquals(XmlCanonico.texto(doc.getXml(), "SignatureValue").replaceAll("\\s", ""),
                p.get(0).text("SignatureValue").replace("&#13;", "").replaceAll("\\s", ""));
        Assertions.assertEquals(doc.getQRLink(), p.get(0).text("dCarQR").replace("&amp;", "&"), "el QR del documento es el transmitido");
        // CA4-02 (app-developer, 2026-11-24): el rDE recibido tiene el mismo C14N que el SignedDocument enviado
        Assertions.assertArrayEquals(XmlCanonico.canonico(doc.getXml()),
                XmlCanonico.canonico(org.javabeanstack.sifen.internal.util.SifenUtil.serialize(
                        org.javabeanstack.sifen.internal.util.SifenUtil.parseRDE(p.get(0).getBody()))),
                "C14N del rDE recibido = el del documento firmado (CA4-02)");
    }

    @Test
    public void unRechazoVuelveEnElResultado() throws Exception {
        sim.on(SimulatedService.RECEIVE_DE, Scenario.rejected());
        ISifenClient c = cliente();
        SendResult r = c.send(c.sign(documento()));
        Assertions.assertEquals(ResultStatus.REJECTED, r.getStatus());
        Assertions.assertEquals(Scenario.DEFAULT_REJECTION_CODE, r.getResponseCode());
        Assertions.assertFalse(r.isAlreadyRegistered());
    }

    @Test
    public void unDocumentoFirmadoAlteradoNoSeTransmite() throws Exception {
        ISifenClient c = cliente();
        SignedDocument doc = c.sign(documento());
        SignedDocument otroCdc = SignedDocument.builder().xml(doc.getXml()).cdc(CasosDePrueba.CDC_FACTURA_SIMPLE).build();
        Assertions.assertEquals("JBS-XML-07", Assertions.assertThrows(SifenException.class, () -> c.send(otroCdc)).getCode());
        SignedDocument editado = SignedDocument.builder()
                .xml(doc.getXml().replaceFirst("<dNumDoc>\\d{7}</dNumDoc>", "<dNumDoc>9999999</dNumDoc>")).build();
        Assertions.assertEquals("JBS-XML-07", Assertions.assertThrows(SifenException.class, () -> c.send(editado)).getCode());
        Assertions.assertEquals("JBS-XML-01", Assertions.assertThrows(SifenException.class, () -> c.send(null)).getCode());
        Assertions.assertTrue(pedidos(SimulatedService.RECEIVE_DE).isEmpty());
    }

    // ------------------------------------------------------------------ §10.4: idempotencia (CA10-06, D41)

    /** 1001 «CDC duplicado» ⇒ una consulta y el estado real; SIFEN tiene otro XML ⇒ {@code isDigestMismatch()} verdadero (D43). */
    @Test
    public void cdcDuplicadoDevuelveElEstadoReal() throws Exception {
        sim.on(SimulatedService.RECEIVE_DE, Scenario.rejected("1001", "CDC duplicado"));
        ISifenClient c = cliente();
        SignedDocument doc = c.sign(documento());
        SendResult r = c.send(doc);
        Assertions.assertEquals(ResultStatus.APPROVED, r.getStatus());
        Assertions.assertTrue(r.isAlreadyRegistered());
        Assertions.assertTrue(r.isDigestMismatch(), "el simulador tiene el rDE de otro documento (caso de redondeo en USD)");
        Assertions.assertEquals(1, pedidos(SimulatedService.RECEIVE_DE).size());
        Assertions.assertEquals(1, pedidos(SimulatedService.QUERY_DE).size(), "una sola consulta, sin bucle");
    }

    /** D41: con reintentos configurados, una recepción cortada sale una vez y queda como resultado desconocido. */
    @Test
    public void unEnvioCortadoNoSeReintentaYQuedaDesconocido() throws Exception {
        SifenConfig cfg = sim.config();
        cfg.setMaxRetries(3);
        cfg.setRetryBackoffMs(0);
        ISifenClient c = SifenClient.create(cfg);
        SignedDocument doc = c.sign(documento());
        sim.once(SimulatedService.RECEIVE_DE, Scenario.connectionCut());
        SifenException e = Assertions.assertThrows(SifenException.class, () -> c.send(doc));
        Assertions.assertTrue(e.isOutcomeUnknown(), e.getCode());
        Assertions.assertEquals(ErrorCategory.CONNECTION, e.getCategory());
        Assertions.assertEquals(1, pedidos(SimulatedService.RECEIVE_DE).size(), "la librería no reintenta una recepción (D41)");

        // resolve: SIFEN no lo tiene (0420) ⇒ lo envía una vez
        sim.once(SimulatedService.QUERY_DE, Scenario.deNotFound());
        SendResult r = c.resolve(doc);
        Assertions.assertEquals(ResultStatus.APPROVED, r.getStatus());
        Assertions.assertFalse(r.isAlreadyRegistered());
        Assertions.assertEquals(2, pedidos(SimulatedService.RECEIVE_DE).size());
        Assertions.assertEquals(1, pedidos(SimulatedService.QUERY_DE).size());
    }

    /** {@code resolve} con el documento en SIFEN: devuelve ese estado y no reenvía. */
    @Test
    public void resolverNoReenviaSiSifenLoTiene() throws Exception {
        ISifenClient c = cliente();
        SignedDocument doc = c.sign(documento());
        SendResult r = c.resolve(doc);
        Assertions.assertEquals(ResultStatus.APPROVED, r.getStatus());
        Assertions.assertTrue(r.isAlreadyRegistered());
        Assertions.assertEquals("1000000001", r.getAuthorizationProtocol());
        Assertions.assertTrue(pedidos(SimulatedService.RECEIVE_DE).isEmpty());
        Assertions.assertEquals(1, pedidos(SimulatedService.QUERY_DE).size());
    }

    /** Un segundo envío simultáneo del mismo CDC en el mismo cliente ⇒ {@code JBS-ENT-08} sin contactar a SIFEN. */
    @Test
    public void unSegundoEnvioSimultaneoDelMismoCdcSeRechaza() throws Exception {
        ISifenClient c = cliente();
        SignedDocument doc = c.sign(documento());
        sim.once(SimulatedService.RECEIVE_DE, Scenario.delayed(Duration.ofSeconds(5), Scenario.approved()));
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<SendResult> primero = pool.submit(() -> c.send(doc));
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (pedidos(SimulatedService.RECEIVE_DE).isEmpty() && System.nanoTime() < limite) {
                Thread.sleep(20);
            }
            SifenException e = Assertions.assertThrows(SifenException.class, () -> c.send(doc));
            Assertions.assertEquals("JBS-ENT-08", e.getCode());
            Assertions.assertFalse(e.isOutcomeUnknown());
            SifenException r = Assertions.assertThrows(SifenException.class, () -> c.resolve(doc));
            Assertions.assertEquals("JBS-ENT-08", r.getCode(), "resolve comparte el bloqueo");
            Assertions.assertEquals(ResultStatus.APPROVED, primero.get(30, TimeUnit.SECONDS).getStatus());
        } finally {
            pool.shutdownNow();
        }
        Assertions.assertEquals(1, pedidos(SimulatedService.RECEIVE_DE).size());
        Assertions.assertEquals(ResultStatus.APPROVED, c.send(doc).getStatus(), "terminado el primero, se libera");
    }

    @Test
    public void elRegistroDeEnviosDeLaAplicacionAutorizaCadaTransmision() throws Exception {
        List<String> preguntas = Collections.synchronizedList(new ArrayList<>());
        ISendRegistry no = (cdc, opId) -> {
            preguntas.add(cdc + "/" + opId);
            return false;
        };
        ISifenClient c = SifenClient.create(sim.config(), no);
        SignedDocument doc = c.sign(documento());
        SifenException e = Assertions.assertThrows(SifenException.class, () -> c.send(doc));
        Assertions.assertEquals("JBS-ENT-13", e.getCode(), "D43: la negativa del registro tiene su código");
        Assertions.assertEquals(1, preguntas.size());
        Assertions.assertTrue(preguntas.get(0).startsWith(doc.getCdc() + "/" + e.getCorrelationId()), preguntas.toString());
        Assertions.assertTrue(pedidos(SimulatedService.RECEIVE_DE).isEmpty());

        ISendRegistry falla = (cdc, opId) -> {
            throw new IllegalStateException("base caída");
        };
        ISifenClient c2 = SifenClient.create(sim.config(), falla);
        SifenException e2 = Assertions.assertThrows(SifenException.class, () -> c2.send(doc));
        Assertions.assertEquals("JBS-INT-00", e2.getCode());
        Assertions.assertFalse(e2.isOutcomeUnknown(), "no se transmitió nada");
        Assertions.assertTrue(pedidos(SimulatedService.RECEIVE_DE).isEmpty());

        SifenClientRegistry registro = new SifenClientRegistry((cdc, opId) -> true);
        Assertions.assertEquals(ResultStatus.APPROVED, registro.get("e", sim::config).send(doc).getStatus());
    }

    // ------------------------------------------------------------------ §10.7 (D43): 1001 sin confirmar, ambiente, resolve

    private static final String CONSULTA_0421 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><env:Envelope "
            + "xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"><env:Header/><env:Body><ns2:rEnviConsDeResponse "
            + "xmlns:ns2=\"http://ekuatia.set.gov.py/sifen/xsd\"><ns2:dFecProc>2026-11-24T10:06:00-03:00</ns2:dFecProc>"
            + "<ns2:dCodRes>0421</ns2:dCodRes><ns2:dMsgRes>Código no previsto</ns2:dMsgRes></ns2:rEnviConsDeResponse>"
            + "</env:Body></env:Envelope>";

    /** E10-02 (D43): 1001 y la consulta da 0420 ⇒ {@code JBS-RESP-08} con resultado desconocido, nunca {@code REJECTED}. */
    @Test
    public void cdcDuplicadoSinConfirmarEsResultadoDesconocido() throws Exception {
        sim.on(SimulatedService.RECEIVE_DE, Scenario.rejected("1001", "CDC duplicado"));
        sim.on(SimulatedService.QUERY_DE, Scenario.deNotFound());
        ISifenClient c = cliente();
        SignedDocument doc = c.sign(documento());
        SifenException e = Assertions.assertThrows(SifenException.class, () -> c.send(doc));
        Assertions.assertEquals("JBS-RESP-08", e.getCode());
        Assertions.assertTrue(e.isOutcomeUnknown(), "no es un rechazo: SIFEN tiene el CDC");
        Assertions.assertEquals("1001", e.getSifenResponseCode());
        Assertions.assertEquals(ErrorCategory.SIFEN_RESPONSE, e.getCategory());
        Assertions.assertTrue(e.getMessage().contains("1001"), e.getMessage());
        Assertions.assertEquals(1, pedidos(SimulatedService.RECEIVE_DE).size());
        Assertions.assertEquals(1, pedidos(SimulatedService.QUERY_DE).size());

        // resolve: 0420 → reenvío → 1001 → 0420 ⇒ JBS-RESP-08 otra vez, sin bucle
        SifenException r = Assertions.assertThrows(SifenException.class, () -> c.resolve(doc));
        Assertions.assertEquals("JBS-RESP-08", r.getCode());
        Assertions.assertTrue(r.isOutcomeUnknown());
        Assertions.assertEquals(2, pedidos(SimulatedService.RECEIVE_DE).size());
        Assertions.assertEquals(3, pedidos(SimulatedService.QUERY_DE).size());
    }

    /** E10-04 (D43): {@code resolve} con un código de consulta que no es 0422 ni 0420 ⇒ {@code JBS-RESP-08}, sin reenviar. */
    @Test
    public void resolverConUnCodigoNoPrevistoNoReenviaYQuedaDesconocido() throws Exception {
        ISifenClient c = cliente();
        SignedDocument doc = c.sign(documento());
        sim.once(SimulatedService.QUERY_DE, Scenario.body(200, "application/soap+xml; charset=utf-8", CONSULTA_0421));
        SifenException e = Assertions.assertThrows(SifenException.class, () -> c.resolve(doc));
        Assertions.assertEquals("JBS-RESP-08", e.getCode());
        Assertions.assertEquals("0421", e.getSifenResponseCode());
        Assertions.assertTrue(e.isOutcomeUnknown());
        Assertions.assertTrue(pedidos(SimulatedService.RECEIVE_DE).isEmpty());
    }

    /** M10-01 (D43): un documento de otro ambiente no se transmite; en {@code resolve} la excepción queda en duda. */
    @Test
    public void unDocumentoDeOtroAmbienteNoSeTransmite() throws Exception {
        SignedDocument dev = cliente().sign(documento());
        SifenConfig prod = sim.config();
        prod.setEnvironment(SifenConfig.Environment.PROD);
        prod.setCsc("A1B2C3D4E5F6A7B8C9D0E1F2A3B4C5D6");
        ISifenClient cp = SifenClient.create(prod);
        SifenException e = Assertions.assertThrows(SifenException.class, () -> cp.send(dev));
        Assertions.assertEquals("JBS-ENT-12", e.getCode());
        Assertions.assertFalse(e.isOutcomeUnknown(), "rechazo local");
        Assertions.assertTrue(e.getMessage().contains("DEV") && e.getMessage().contains("PROD"), e.getMessage());
        Assertions.assertEquals("JBS-ENT-12", Assertions.assertThrows(SifenException.class,
                () -> cp.sendBatch(List.of(dev))).getCode());
        SifenException r = Assertions.assertThrows(SifenException.class, () -> cp.resolve(dev));
        Assertions.assertEquals("JBS-ENT-12", r.getCode());
        Assertions.assertTrue(r.isOutcomeUnknown(), "D43: toda excepción de resolve queda en duda");
        Assertions.assertTrue(sim.requests().isEmpty(), "nada llegó al simulador");
    }

    /** E10-05 (D43): {@code sendEvents} admite un evento por llamada. */
    @Test
    public void enviarDosEventosSeRechazaSinContactar() throws Exception {
        org.javabeanstack.sifen.core.beans.EventosDE uno = org.javabeanstack.sifen.internal.request.ReqRecEventoDe
                .cancellation("1", CasosDePrueba.FECHA_FIRMA, CasosDePrueba.CDC_FACTURA_SIMPLE, MOTIVO);
        org.javabeanstack.sifen.core.beans.EventosDE dos = org.javabeanstack.sifen.internal.request.ReqRecEventoDe
                .cancellation("2", CasosDePrueba.FECHA_FIRMA, CasosDePrueba.CDC_FACTURA_SIMPLE, MOTIVO);
        uno.getrGesEveList().addAll(dos.getrGesEveList());
        SifenException e = Assertions.assertThrows(SifenException.class, () -> cliente().sendEvents(uno));
        Assertions.assertEquals("JBS-ENT-14", e.getCode());
        Assertions.assertTrue(e.getMessage().contains("2"), e.getMessage());
        Assertions.assertTrue(pedidos(SimulatedService.EVENT).isEmpty());
    }

    /** AB:I5-08 (D43): un corte durante el saludo TLS no envió nada ⇒ {@code isOutcomeUnknown()} falso. */
    @Test
    public void unCorteEnElSaludoTlsNoEsResultadoDesconocido() throws Exception {
        ISifenClient firmador = cliente();
        SignedDocument doc = firmador.sign(documento());
        try (java.net.ServerSocket ss = new java.net.ServerSocket(0, 5, java.net.InetAddress.getLoopbackAddress())) {
            Thread corta = new Thread(() -> {
                try (java.net.Socket so = ss.accept()) {
                    so.setSoLinger(true, 0);   // RST apenas llega el ClientHello: ningún byte del pedido HTTP
                    so.getInputStream().read(new byte[64]);
                } catch (java.io.IOException ignorada) {
                    // fin de la prueba
                }
            });
            corta.setDaemon(true);
            corta.start();
            SifenConfig cfg = sim.config();
            cfg.setBaseUrl("https://127.0.0.1:" + ss.getLocalPort());
            ISifenClient c = SifenClient.create(cfg);
            SifenException e = Assertions.assertThrows(SifenException.class, () -> c.send(doc));
            Assertions.assertEquals(ErrorCategory.CONNECTION, e.getCategory(), e.getCode());
            Assertions.assertFalse(e.isOutcomeUnknown(), e.getCode() + ": el pedido no se escribió");
            corta.join(5000);
        }
    }

    /** E10-06 (D43): una falla después de transmitir (la consulta posterior al 1001 cortada) queda en duda. */
    @Test
    public void unaFallaDespuesDeTransmitirQuedaEnDuda() throws Exception {
        sim.on(SimulatedService.RECEIVE_DE, Scenario.rejected("1001", "CDC duplicado"));
        sim.once(SimulatedService.QUERY_DE, Scenario.badGateway());
        ISifenClient c = cliente();
        SignedDocument doc = c.sign(documento());
        SifenException e = Assertions.assertThrows(SifenException.class, () -> c.send(doc));
        Assertions.assertTrue(e.isOutcomeUnknown(), e.getCode());
    }

    /** M10-02: si la firma falla, el DE queda como estaba (sin la fecha ni el código que completó el cliente). */
    @Test
    public void firmarQueFallaNoDejaElDocumentoModificado() throws Exception {
        DocumentoElectronico de = documento();
        de.setdFecFirma(null);
        de.getgOpeDE().setdCodSeg(null);
        de.getgTimb().setdNumDoc(null);
        Assertions.assertThrows(SifenValidationException.class, () -> cliente().sign(de));
        Assertions.assertNull(de.getdFecFirma());
        Assertions.assertNull(de.getgOpeDE().getdCodSeg());
    }

    /** E4-02 / F:E10-03: el XML con DOCTYPE no se resuelve en {@code verifySignature} ni en {@code getDocument()}. */
    @Test
    public void unXmlConDoctypeSeRechazaSinResolverEntidades() throws Exception {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE r [<!ENTITY x SYSTEM \"file:///no-existe-jbs-sifen.txt\">]><rDE>&x;</rDE>";
        org.javabeanstack.sifen.api.SignatureVerification v = cliente().verifySignature(xxe);
        Assertions.assertTrue(v.isFailure());
        Assertions.assertFalse(v.isValid());
        Assertions.assertEquals("JBS-XML-03", v.getCode());
        SifenException e = Assertions.assertThrows(SifenException.class,
                () -> SignedDocument.builder().xml(xxe).build().getDocument());
        Assertions.assertEquals("JBS-XML-03", e.getCode());
        Assertions.assertFalse(String.valueOf(e.getTechnicalDetail()).contains("FileNotFound"), e.getTechnicalDetail());
    }

    /** I10-19 y M10-03: los clientes de un registro comparten la secuencia de eventos y el bloqueo por CDC. */
    @Test
    public void losClientesDeUnRegistroCompartenLaSecuenciaYElBloqueo() throws Exception {
        SifenClientRegistry registro = new SifenClientRegistry();
        ISifenClient a = registro.get("a", sim::config);
        ISifenClient b = registro.get("b", sim::config);
        long ia = Long.parseLong(a.cancel(CasosDePrueba.CDC_FACTURA_SIMPLE, MOTIVO).getEventId());
        long ib = Long.parseLong(b.cancel(CasosDePrueba.CDC_FACTURA_SIMPLE, MOTIVO).getEventId());
        registro.invalidate("a");
        ISifenClient a2 = registro.get("a", sim::config);
        long ia2 = Long.parseLong(a2.cancel(CasosDePrueba.CDC_FACTURA_SIMPLE, MOTIVO).getEventId());
        Assertions.assertTrue(ib > ia && ia2 > ib, ia + " " + ib + " " + ia2);
        Field cerrado = SifenClient.class.getDeclaredField("closed");
        cerrado.setAccessible(true);
        Assertions.assertTrue(((java.util.concurrent.atomic.AtomicBoolean) cerrado.get(a)).get(),
                "el cliente viejo se cerró al invalidar (sin operaciones en curso)");

        SignedDocument doc = a2.sign(documento());
        sim.once(SimulatedService.RECEIVE_DE, Scenario.delayed(Duration.ofSeconds(5), Scenario.approved()));
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<SendResult> primero = pool.submit(() -> a2.send(doc));
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (pedidos(SimulatedService.RECEIVE_DE).isEmpty() && System.nanoTime() < limite) {
                Thread.sleep(20);
            }
            Assertions.assertEquals("JBS-ENT-08", Assertions.assertThrows(SifenException.class, () -> b.send(doc)).getCode(),
                    "otro cliente del mismo registro ve el envío en curso");
            primero.get(30, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
            registro.closeAll();
        }
    }

    /** M10-06: los tipos de {@code api} sobreviven a la serialización con los mismos valores. */
    @Test
    public void losTiposDeApiSeSerializan() throws Exception {
        ISifenClient c = cliente();
        SignedDocument doc = c.sign(documento());
        SendResult r = c.send(doc);
        Object[] valores = {doc, r, c.queryDE(doc.getCdc()), c.queryRuc("80123456"), c.queryBatch(Scenario.BATCH_NUMBER),
            c.cancel(doc.getCdc(), MOTIVO), c.getCertificateInfo(), c.checkConnection(), c.verifySignature(doc.getXml()),
            NumberRange.builder().timbrado("1").build(), c.sendBatch(List.of(doc))};
        for (Object v : valores) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(bytes)) {
                out.writeObject(v);
            }
            Object leido;
            try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
                leido = in.readObject();
            }
            for (java.lang.reflect.Method m : v.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && (m.getName().startsWith("get") || m.getName().startsWith("is"))
                        && m.getDeclaringClass() == v.getClass() && !m.getName().equals("getDocument")) {
                    Assertions.assertEquals(String.valueOf(m.invoke(v)), String.valueOf(m.invoke(leido)),
                            v.getClass().getSimpleName() + "." + m.getName());
                }
            }
        }
    }

    // ------------------------------------------------------------------ lote

    @Test
    public void elLoteSeEnviaConUnaListaInmutable() throws Exception {
        ISifenClient c = cliente();
        SignedDocument a = c.sign(documento());
        SignedDocument b = c.sign(documento());
        BatchSendResult r = c.sendBatch(List.of(a, b));   // E10-01: List.of no rompe
        Assertions.assertEquals(BatchStatus.RECEIVED, r.getStatus());
        Assertions.assertEquals(Scenario.BATCH_NUMBER, r.getBatchNumber());
        Assertions.assertEquals(List.of(a.getCdc(), b.getCdc()), r.getCdcs());
        Assertions.assertTrue(ID.matcher(r.getOperationId()).matches());
        Assertions.assertEquals(1, pedidos(SimulatedService.RECEIVE_BATCH).size());
    }

    @Test
    public void elLoteControlaSusArgumentosAntesDeEnviar() throws Exception {
        ISifenClient c = cliente();
        SignedDocument a = c.sign(documento());
        Assertions.assertEquals("JBS-ENT-11", Assertions.assertThrows(SifenException.class, () -> c.sendBatch(List.of())).getCode());
        Assertions.assertEquals("JBS-ENT-11", Assertions.assertThrows(SifenException.class, () -> c.sendBatch(null)).getCode());
        Assertions.assertEquals("JBS-ENT-11", Assertions.assertThrows(SifenException.class,
                () -> c.sendBatch(Arrays.asList(a, null))).getCode());
        Assertions.assertEquals("JBS-ENT-02", Assertions.assertThrows(SifenException.class,
                () -> c.sendBatch(List.of(a, a))).getCode());
        Assertions.assertEquals("JBS-ENT-01", Assertions.assertThrows(SifenException.class,
                () -> c.sendBatch(Collections.nCopies(51, a))).getCode());
        Assertions.assertTrue(pedidos(SimulatedService.RECEIVE_BATCH).isEmpty());
    }

    // ------------------------------------------------------------------ eventos (CA10-07)

    private static ISifenClient clienteDeEventos(String eventId) throws SifenException {
        Clock fijo = Clock.fixed(CasosDePrueba.FECHA_FIRMA.atZone(ValidationContext.SIFEN_ZONE).toInstant(), ZoneOffset.UTC);
        return SifenClient.create(sim.config(), null, fijo, () -> eventId, null);
    }

    /** CA10-07: {@code cancel} transmite el mismo XML que el golden EV-01 (armado con el modelo). */
    @Test
    public void cancelarTransmiteElMismoXmlQueElGoldenEv01() throws Exception {
        EventResult r = clienteDeEventos("1").cancel(CasosDePrueba.CDC_FACTURA_SIMPLE, MOTIVO);
        Assertions.assertEquals(ResultStatus.APPROVED, r.getStatus(), r.toString());
        Assertions.assertEquals("1", r.getEventId());
        Assertions.assertTrue(r.getEventXml().contains("rGeVeCan"), r.getEventXml());
        Assertions.assertTrue(ID.matcher(r.getOperationId()).matches());
        List<RecordedRequest> p = pedidos(SimulatedService.EVENT);
        Assertions.assertEquals(1, p.size());
        Assertions.assertArrayEquals(FixturesSifen.recursoBytes("golden/EV-01.xml"), XmlCanonico.canonico(p.get(0).getBody()));
    }

    /** CA10-07: {@code invalidateNumbers} transmite el mismo XML que el golden EV-02. */
    @Test
    public void inutilizarTransmiteElMismoXmlQueElGoldenEv02() throws Exception {
        NumberRange rango = NumberRange.builder().timbrado(String.valueOf(CasosDePrueba.TIMBRADO)).establishment("1")
                .issuePoint("1").documentType(TTiDE.FACTURA_ELECTRONICA).from("50").to("55")
                .reason("Inutilizacion de prueba de numeracion").build();
        EventResult r = clienteDeEventos("2").invalidateNumbers(rango);
        Assertions.assertEquals("2", r.getEventId());
        Assertions.assertArrayEquals(FixturesSifen.recursoBytes("golden/EV-02.xml"),
                XmlCanonico.canonico(pedidos(SimulatedService.EVENT).get(0).getBody()));
    }

    @Test
    public void losEventosControlanSusArgumentos() throws Exception {
        ISifenClient c = cliente();
        Assertions.assertEquals("JBS-ENT-03", Assertions.assertThrows(SifenException.class, () -> c.cancel("123", MOTIVO)).getCode());
        SifenValidationException corto = Assertions.assertThrows(SifenValidationException.class,
                () -> c.cancel(CasosDePrueba.CDC_FACTURA_SIMPLE, "abc"));
        Assertions.assertFalse(corto.getErrors().isEmpty());
        Assertions.assertEquals("JBS-ENT-10", Assertions.assertThrows(SifenException.class, () -> c.invalidateNumbers(null)).getCode());
        Assertions.assertEquals("JBS-ENT-10", Assertions.assertThrows(SifenException.class,
                () -> c.invalidateNumbers(NumberRange.builder().timbrado("x").documentType(TTiDE.FACTURA_ELECTRONICA).build())).getCode());
        Assertions.assertEquals("JBS-ENT-10", Assertions.assertThrows(SifenException.class,
                () -> c.invalidateNumbers(NumberRange.builder().timbrado("12345678").build())).getCode());
        Assertions.assertEquals("JBS-ENT-14", Assertions.assertThrows(SifenException.class, () -> c.sendEvents(null)).getCode());
        Assertions.assertTrue(pedidos(SimulatedService.EVENT).isEmpty());
    }

    /** I10-19 (D43): el {@code Id} sale de los milisegundos del reloj (módulo 10¹⁰), crece, y vuelve al pasar de diez dígitos. */
    @Test
    public void losIdsDeEventoSalenDeLosMilisegundosYCrecen() {
        long ms = 1_795_532_400_123L;
        SifenClientShared sh = new SifenClientShared();
        Clock fijo = Clock.fixed(java.time.Instant.ofEpochMilli(ms), ZoneOffset.UTC);
        long a = Long.parseLong(sh.nextEventId(fijo));
        Assertions.assertEquals(ms % 10_000_000_000L, a);
        Assertions.assertEquals(a + 1, Long.parseLong(sh.nextEventId(fijo)), "mismo milisegundo: el siguiente");
        Clock despues = Clock.fixed(java.time.Instant.ofEpochMilli(ms + 1000), ZoneOffset.UTC);
        Assertions.assertEquals(a + 1000, Long.parseLong(sh.nextEventId(despues)), "vuelve al reloj cuando lo alcanza");

        SifenClientShared borde = new SifenClientShared();
        Clock ultimo = Clock.fixed(java.time.Instant.ofEpochMilli(30_000_000_000L - 1), ZoneOffset.UTC);
        Assertions.assertEquals("9999999999", borde.nextEventId(ultimo));
        String siguiente = borde.nextEventId(ultimo);
        Assertions.assertEquals("1", siguiente, "al pasar de diez dígitos vuelve a empezar");
        Assertions.assertTrue(siguiente.length() <= 10);
    }

    // ------------------------------------------------------------------ conexión propia y diagnóstico

    /** El cliente pasa su conexión a cada pedido: no usa las conexiones por configuración de la fachada heredada. */
    @Test
    public void elClienteNoUsaLasConexionesPorConfiguracion() throws Exception {
        ISifenClient c = cliente();
        SignedDocument doc = c.sign(documento());
        c.send(doc);
        c.queryRuc("80123456");
        c.queryDE(doc.getCdc());
        c.queryBatch(Scenario.BATCH_NUMBER);
        c.cancel(doc.getCdc(), MOTIVO);
        c.checkConnection();
        Field cfg = SifenClient.class.getDeclaredField("config");
        cfg.setAccessible(true);
        Object propia = cfg.get(c);
        Class<?> dc = Class.forName("org.javabeanstack.sifen.internal.http.DefaultConnections");
        Field by = dc.getDeclaredField("BY_CONFIG");
        by.setAccessible(true);
        Map<?, ?> mapa = (Map<?, ?>) by.get(null);
        synchronized (mapa) {
            Assertions.assertFalse(mapa.containsKey(propia), "DefaultConnections no tiene la configuración del cliente");
        }
        c.close();
        c.close();
    }

    @Test
    public void checkConnectionDiagnosticaPorEtapas() throws Exception {
        ISifenClient c = cliente();
        org.javabeanstack.sifen.api.ConnectionDiagnosis d = c.checkConnection();
        Assertions.assertTrue(d.isOk(), d.toString());
        Assertions.assertFalse(d.getSteps().isEmpty());
        Assertions.assertTrue(ID.matcher(d.getOperationId()).matches());
        for (org.javabeanstack.sifen.api.ConnectionDiagnosis.Step s : d.getSteps()) {
            Assertions.assertNotEquals("JBS-INT-04", s.getCode());
        }
    }
}

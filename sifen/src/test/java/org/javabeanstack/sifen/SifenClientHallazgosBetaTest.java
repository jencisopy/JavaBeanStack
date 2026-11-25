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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.javabeanstack.sifen.api.BatchQueryResult;
import org.javabeanstack.sifen.api.BatchSendResult;
import org.javabeanstack.sifen.api.BatchStatus;
import org.javabeanstack.sifen.api.DEQueryResult;
import org.javabeanstack.sifen.api.EventResult;
import org.javabeanstack.sifen.api.ResultStatus;
import org.javabeanstack.sifen.api.RucQueryResult;
import org.javabeanstack.sifen.api.SendResult;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.simulator.Scenario;
import org.javabeanstack.sifen.simulator.SifenSimulator;
import org.javabeanstack.sifen.simulator.SimulatedService;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Hallazgos de la batería en vivo del beta (plan JBSSIF, §8.7) que corrige el carril F, contra el simulador:
 * I8-03 (el 0160 intermitente de la consulta de RUC se conserva y se reintenta), I8-08 (un 0100 en un evento no
 * es un rechazo), I8-04 (la consulta de DE con el {@code rDE} en línea no deja SEVERE ni WARN en el log) e I8-10
 * (0100, 0161 o 0162 en la recepción de un DE es resultado desconocido, no un rechazo; decisión del usuario del
 * 2026-11-25).
 */
public class SifenClientHallazgosBetaTest {
    private static final String MOTIVO = "Cancelacion de prueba del documento";
    private static final String SOAP = "application/soap+xml; charset=utf-8";
    private static final String RUC_0160 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><env:Envelope "
            + "xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"><env:Header/><env:Body><ns2:rRetEnviDe "
            + "xmlns:ns2=\"http://ekuatia.set.gov.py/sifen/xsd\"><ns2:rProtDe><ns2:dFecProc>2026-11-25T01:15:29-03:00"
            + "</ns2:dFecProc><ns2:dEstRes>Rechazado</ns2:dEstRes><ns2:gResProc><ns2:dCodRes>0160</ns2:dCodRes>"
            + "<ns2:dMsgRes>XML Mal Formado.</ns2:dMsgRes></ns2:gResProc></ns2:rProtDe></ns2:rRetEnviDe></env:Body>"
            + "</env:Envelope>";

    private static SifenSimulator sim;

    private static final List<LogEvent> LOG4J = Collections.synchronizedList(new ArrayList<>());
    private static final List<LogRecord> JUL = Collections.synchronizedList(new ArrayList<>());
    private static final String RESPONSE_UTIL = "org.javabeanstack.sifen.internal.util.ResponseUtil";
    private static AbstractAppender captura;
    private static final Handler JUL_CAPTURA = new Handler() {
        @Override
        public void publish(LogRecord r) {
            JUL.add(r);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    @BeforeAll
    public static void levantar() throws Exception {
        sim = SifenSimulator.start();
        LoggerContext contexto = (LoggerContext) LogManager.getContext(false);
        captura = new AbstractAppender("hallazgos-beta", null, null, true, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                LOG4J.add(event.toImmutable());
            }
        };
        captura.start();
        LoggerConfig propio = LoggerConfig.newBuilder().withLoggerName(RESPONSE_UTIL).withLevel(Level.WARN)
                .withAdditivity(false).withConfig(contexto.getConfiguration()).build();
        propio.addAppender(captura, Level.ALL, null);
        contexto.getConfiguration().addLogger(RESPONSE_UTIL, propio);
        contexto.updateLoggers();
        java.util.logging.Logger.getLogger("").addHandler(JUL_CAPTURA);
    }

    @AfterAll
    public static void bajar() {
        java.util.logging.Logger.getLogger("").removeHandler(JUL_CAPTURA);
        LoggerContext contexto = (LoggerContext) LogManager.getContext(false);
        contexto.getConfiguration().removeLogger(RESPONSE_UTIL);
        contexto.updateLoggers();
        captura.stop();
        if (sim != null) {
            sim.close();
        }
    }

    @BeforeEach
    public void limpiar() {
        sim.reset();
        LOG4J.clear();
        JUL.clear();
    }

    @AfterEach
    public void sinColas() {
        sim.reset();
    }

    private static ISifenClient cliente(int reintentos) throws SifenException {
        SifenConfig c = sim.config();
        c.setMaxRetries(reintentos);
        c.setRetryBackoffMs(0);
        return SifenClient.create(c);
    }

    // ------------------------------------------------------------------ I8-03

    /** I8-03: sin reintentos (por omisión) Maker recibe {@code JBS-RESP-02} con el 0160 y el mensaje de SIFEN. */
    @Test
    public void elRucCon0160ConservaElCodigoYElMensajeDeSifen() throws Exception {
        sim.on(SimulatedService.QUERY_RUC, Scenario.body(200, SOAP, RUC_0160));
        SifenException e = Assertions.assertThrows(SifenException.class, () -> cliente(0).queryRuc("80123456"));
        Assertions.assertEquals("JBS-RESP-02", e.getCode());
        Assertions.assertEquals("0160", e.getSifenResponseCode());
        Assertions.assertTrue(e.getTechnicalDetail().contains("0160"), e.getTechnicalDetail());
        Assertions.assertTrue(e.getTechnicalDetail().contains("XML Mal Formado."), e.getTechnicalDetail());
        Assertions.assertFalse(e.isOutcomeUnknown(), "una consulta no deja nada en duda");
        Assertions.assertEquals(1, sim.requests(SimulatedService.QUERY_RUC).size());
    }

    /** I8-03: con {@code sifen.reintentos.max} la consulta repite el pedido y Maker recibe el 0502. */
    @Test
    public void elRucCon0160SeReintentaYSeRecupera() throws Exception {
        sim.once(SimulatedService.QUERY_RUC, Scenario.body(200, SOAP, RUC_0160));
        sim.once(SimulatedService.QUERY_RUC, Scenario.body(200, SOAP, RUC_0160));
        RucQueryResult r = cliente(2).queryRuc("80123456");
        Assertions.assertTrue(r.isFound(), r.toString());
        Assertions.assertEquals(3, sim.requests(SimulatedService.QUERY_RUC).size());
    }

    /** I8-03: agotados los reintentos, la misma excepción con el 0160. */
    @Test
    public void elRucCon0160AgotaLosReintentos() throws Exception {
        sim.on(SimulatedService.QUERY_RUC, Scenario.body(200, SOAP, RUC_0160));
        SifenException e = Assertions.assertThrows(SifenException.class, () -> cliente(1).queryRuc("80123456"));
        Assertions.assertEquals("JBS-RESP-02", e.getCode());
        Assertions.assertEquals("0160", e.getSifenResponseCode());
        Assertions.assertEquals(2, sim.requests(SimulatedService.QUERY_RUC).size());
    }

    /**
     * I8-03: en la consulta de DE el 0160 también llega con su código, pero no se reintenta (en otras operaciones
     * un 0160 puede ser un valor inválido: E3-24, E3-26).
     */
    @Test
    public void laConsultaDeDeCon0160NoSeReintenta() throws Exception {
        sim.on(SimulatedService.QUERY_DE, Scenario.body(200, SOAP, RUC_0160));
        SifenException e = Assertions.assertThrows(SifenException.class,
                () -> cliente(2).queryDE(CasosDePrueba.CDC_FACTURA_SIMPLE));
        Assertions.assertEquals("JBS-RESP-02", e.getCode());
        Assertions.assertEquals("0160", e.getSifenResponseCode());
        Assertions.assertEquals(1, sim.requests(SimulatedService.QUERY_DE).size());
    }

    // ------------------------------------------------------------------ I8-08

    /** I8-08: «Rechazado» con 0100 «Error Inesperado» no es un rechazo del evento: {@code UNRECOGNIZED}. */
    @Test
    public void laCancelacionCon0100NoEsUnRechazo() throws Exception {
        sim.on(SimulatedService.EVENT, Scenario.eventRejected("0100", "Error Inesperado"));
        EventResult r = cliente(0).cancel(CasosDePrueba.CDC_FACTURA_SIMPLE, MOTIVO);
        Assertions.assertEquals(ResultStatus.UNRECOGNIZED, r.getStatus(), r.toString());
        Assertions.assertEquals("0100", r.getResponseCode());
        Assertions.assertEquals("Error Inesperado", r.getMessage());
        Assertions.assertEquals(1, sim.requests(SimulatedService.EVENT).size(), "un evento nunca se reintenta solo");
    }

    /** I8-08: 0161 y 0162 (servidor sin respuesta, MT AE02/AE03) tampoco son rechazos. */
    @Test
    public void laCancelacionCon0161O0162NoEsUnRechazo() throws Exception {
        for (String codigo : List.of("0161", "0162")) {
            sim.reset();
            sim.on(SimulatedService.EVENT, Scenario.eventRejected(codigo, "Servidor de procesamiento sin respuesta"));
            EventResult r = cliente(0).cancel(CasosDePrueba.CDC_FACTURA_SIMPLE, MOTIVO);
            Assertions.assertEquals(ResultStatus.UNRECOGNIZED, r.getStatus(), codigo + ": " + r);
            Assertions.assertEquals(codigo, r.getResponseCode());
        }
    }

    /** I8-08: un rechazo de datos del evento (otro código, también 0160) sigue siendo {@code REJECTED}. */
    @Test
    public void unRechazoDeDatosDelEventoSigueSiendoRechazo() throws Exception {
        for (String codigo : List.of("4003", "0160")) {
            sim.reset();
            sim.on(SimulatedService.EVENT, Scenario.eventRejected(codigo, "Rechazo de prueba"));
            EventResult r = cliente(0).cancel(CasosDePrueba.CDC_FACTURA_SIMPLE, MOTIVO);
            Assertions.assertEquals(ResultStatus.REJECTED, r.getStatus(), codigo + ": " + r);
            Assertions.assertEquals(codigo, r.getResponseCode());
        }
    }

    // ------------------------------------------------------------------ I8-10

    private static final AtomicInteger NUMERO = new AtomicInteger(700);

    /** El DE del caso 4 del catálogo con un número propio y las leyendas de DEV (como en SifenClientEmissionTest). */
    private static DocumentoElectronico documento() {
        DocumentoElectronico de = CasosDePrueba.documentos().get(4).construir();
        de.getgDatGralOpe().getgEmis().setdNomEmi("DE generado en ambiente de prueba - sin valor comercial ni fiscal");
        de.getgDtipDE().getgCamItemList().get(0).setdDesProSer(
                "DOCUMENTO ELECTRÓNICO SIN VALOR COMERCIAL NI FISCAL - GENERADO EN AMBIENTE DE PRUEBA");
        de.getgTimb().setdNumDoc(String.format("%07d", NUMERO.incrementAndGet()));
        return de;
    }

    /**
     * I8-10: «Rechazado» con 0100, 0161 o 0162 en {@code send} es {@code UNRECOGNIZED} (resultado desconocido):
     * Maker no refirma; {@code resolve} consulta y reenvía <b>el mismo XML</b>.
     */
    @Test
    public void elEnvioCon0100_0161O0162EsResultadoDesconocidoYResolveReenviaElMismoXml() throws Exception {
        for (String codigo : List.of("0100", "0161", "0162")) {
            sim.reset();
            sim.on(SimulatedService.RECEIVE_DE, Scenario.rejected(codigo, "Servidor de procesamiento sin respuesta"));
            ISifenClient c = cliente(0);
            SignedDocument doc = c.sign(documento());
            SendResult r = c.send(doc);
            Assertions.assertEquals(ResultStatus.UNRECOGNIZED, r.getStatus(), codigo + ": " + r);
            Assertions.assertEquals(codigo, r.getResponseCode());
            Assertions.assertEquals(1, sim.requests(SimulatedService.RECEIVE_DE).size(), "una recepción nunca se reintenta sola");

            sim.on(SimulatedService.QUERY_DE, Scenario.deNotFound());
            sim.on(SimulatedService.RECEIVE_DE, Scenario.approved());
            SendResult resuelto = c.resolve(doc);
            Assertions.assertEquals(ResultStatus.APPROVED, resuelto.getStatus(), codigo + ": " + resuelto);
            List<org.javabeanstack.sifen.simulator.RecordedRequest> envios = sim.requests(SimulatedService.RECEIVE_DE);
            Assertions.assertEquals(2, envios.size());
            Assertions.assertEquals(rde(envios.get(0).getBody()), rde(envios.get(1).getBody()), "resolve reenvía el mismo rDE (solo cambia el dId del pedido)");
        }
    }

    /** El {@code rDE} de un pedido de recepción. */
    private static String rde(String pedido) {
        int i = pedido.indexOf("<rDE");
        int f = pedido.indexOf("</rDE>");
        Assertions.assertTrue(i >= 0 && f > i, "pedido sin rDE");
        return pedido.substring(i, f);
    }

    /** I8-10: el reenvío de {@code resolve} con 0161 también queda como resultado desconocido. */
    @Test
    public void elReenvioDeResolveCon0161EsResultadoDesconocido() throws Exception {
        ISifenClient c = cliente(0);
        SignedDocument doc = c.sign(documento());
        sim.on(SimulatedService.QUERY_DE, Scenario.deNotFound());
        sim.on(SimulatedService.RECEIVE_DE, Scenario.rejected("0161", "Servidor de procesamiento sin respuesta"));
        SendResult r = c.resolve(doc);
        Assertions.assertEquals(ResultStatus.UNRECOGNIZED, r.getStatus(), r.toString());
        Assertions.assertEquals("0161", r.getResponseCode());
    }

    /** I8-10: un rechazo de datos del DE (1107, también 0160) sigue siendo {@code REJECTED}. */
    @Test
    public void unRechazoDeDatosDelEnvioSigueSiendoRechazo() throws Exception {
        for (String codigo : List.of(Scenario.DEFAULT_REJECTION_CODE, "0160")) {
            sim.reset();
            sim.on(SimulatedService.RECEIVE_DE, Scenario.rejected(codigo, "Rechazo de prueba"));
            ISifenClient c = cliente(0);
            SendResult r = c.send(c.sign(documento()));
            Assertions.assertEquals(ResultStatus.REJECTED, r.getStatus(), codigo + ": " + r);
            Assertions.assertEquals(codigo, r.getResponseCode());
        }
    }

    /** I8-10: un lote con 0161 en la recepción no queda recibido ni «no encolado»: {@code BatchStatus.UNRECOGNIZED}. */
    @Test
    public void elLoteCon0161EnLaRecepcionEsNoReconocido() throws Exception {
        sim.on(SimulatedService.RECEIVE_BATCH, Scenario.body(200, SOAP, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"><env:Header/><env:Body>"
                + "<ns2:rResEnviLoteDe xmlns:ns2=\"http://ekuatia.set.gov.py/sifen/xsd\"><ns2:dFecProc>2026-11-25T10:00:07-03:00"
                + "</ns2:dFecProc><ns2:dCodRes>0161</ns2:dCodRes><ns2:dMsgRes>Servidor de procesamiento momentáneamente sin "
                + "respuesta</ns2:dMsgRes><ns2:dTpoProces>0</ns2:dTpoProces></ns2:rResEnviLoteDe></env:Body></env:Envelope>"));
        ISifenClient c = cliente(0);
        BatchSendResult r = c.sendBatch(List.of(c.sign(documento())));
        Assertions.assertEquals(BatchStatus.UNRECOGNIZED, r.getStatus(), r.toString());
        Assertions.assertEquals("0161", r.getResponseCode());
        Assertions.assertEquals(1, sim.requests(SimulatedService.RECEIVE_BATCH).size());
    }

    /** I8-10: en la consulta de un lote, el DE «Rechazado» con 0162 vuelve como {@code UNRECOGNIZED}. */
    @Test
    public void elDeDeUnLoteCon0162EsResultadoDesconocido() throws Exception {
        String cdc = "01801234565001001000000122026112310000000010";
        sim.on(SimulatedService.QUERY_BATCH, Scenario.batchCompletedWith(Map.of(cdc, "0162")));
        BatchQueryResult r = cliente(0).queryBatch(Scenario.BATCH_NUMBER);
        Assertions.assertEquals(BatchStatus.COMPLETED, r.getStatus(), r.toString());
        Assertions.assertEquals(1, r.getResults().size());
        Assertions.assertEquals(ResultStatus.UNRECOGNIZED, r.getResults().get(0).getStatus());
        Assertions.assertEquals(cdc, r.getResults().get(0).getCdc());
        Assertions.assertEquals("0162", r.getResults().get(0).getResponseCode());
    }

    // ------------------------------------------------------------------ I8-04

    /**
     * I8-04: la 0422 con el {@code rDE} en línea (como la manda SIFEN-test) y un {@code &amp;} en el
     * {@code dCarQR} se usa tal cual: ni el {@code SAAJ0511} SEVERE (JUL) ni el {@code JBS-RESP-03} WARN (log4j).
     */
    @Test
    public void laConsultaDeDeEnLineaNoEnsuciaElLog() throws Exception {
        sim.on(SimulatedService.QUERY_DE, Scenario.deFound());
        DEQueryResult r = cliente(0).queryDE(CasosDePrueba.CDC_FACTURA_SIMPLE);
        Assertions.assertEquals(ResultStatus.APPROVED, r.getStatus(), r.toString());
        Assertions.assertTrue(r.getXml().contains("<rDE") || r.getXml().contains(":rDE"), r.getXml());
        Assertions.assertTrue(r.getXml().contains("&amp;"), "el dCarQR conserva su &amp; escapado");
        Assertions.assertFalse(r.getXml().contains("&lt;"), "el rDE vino en línea");
        Assertions.assertEquals(List.of(), LOG4J.stream().map(e -> e.getMessage().getFormattedMessage()).toList());
        Assertions.assertEquals(List.of(), JUL.stream()
                .filter(x -> x.getLevel().intValue() >= java.util.logging.Level.WARNING.intValue())
                .map(x -> x.getLoggerName() + ": " + x.getMessage()).toList());
    }

    /** I8-04: la variante escapada (la que describe el MT) se sigue desescapando y leyendo. */
    @Test
    public void laConsultaDeDeEscapadaSeSigueLeyendo() throws Exception {
        sim.on(SimulatedService.QUERY_DE, Scenario.deFoundEscaped());
        DEQueryResult r = cliente(0).queryDE(CasosDePrueba.CDC_FACTURA_SIMPLE);
        Assertions.assertEquals(ResultStatus.APPROVED, r.getStatus(), r.toString());
        Assertions.assertTrue(r.getXml().contains("DigestValue"), r.getXml());
        Assertions.assertFalse(r.getDigestValue().isEmpty());
    }
}

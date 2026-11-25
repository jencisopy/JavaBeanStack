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

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.javabeanstack.sifen.api.BatchQueryResult;
import org.javabeanstack.sifen.api.BatchSendResult;
import org.javabeanstack.sifen.api.BatchStatus;
import org.javabeanstack.sifen.api.CertificateInfo;
import org.javabeanstack.sifen.api.ConnectionDiagnosis;
import org.javabeanstack.sifen.api.DEQueryResult;
import org.javabeanstack.sifen.api.EventResult;
import org.javabeanstack.sifen.api.NumberRange;
import org.javabeanstack.sifen.api.ResultStatus;
import org.javabeanstack.sifen.api.RucQueryResult;
import org.javabeanstack.sifen.api.SendResult;
import org.javabeanstack.sifen.api.SifenMessage;
import org.javabeanstack.sifen.api.SignatureVerification;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.types.TTiDE;
import org.javabeanstack.sifen.core.exceptions.ErrorCategory;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.validation.ValidationResult;
import org.javabeanstack.sifen.simulator.RecordedRequest;
import org.javabeanstack.sifen.simulator.Scenario;
import org.javabeanstack.sifen.simulator.SifenSimulator;
import org.javabeanstack.sifen.simulator.SimulatedService;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Contrato v1 de lo implementado (plan JBSSIF, §10.5, carril C; armazón desde §10.2): cada operación
 * de {@link SifenClient} contra el simulador de SIFEN ({@link SifenSimulator}, §8.6), con su
 * {@code *Result} completo, sin {@code null} donde el javadoc no lo admite y con el id de operación;
 * y el contador de pedidos ({@code dId}) <b>por cliente</b>. Sin red: el simulador escucha en
 * {@code 127.0.0.1} y nunca se contacta a SIFEN (D28).
 *
 * <p>Desde §10.3/§10.4 (F) cubre también la emisión ({@code sign}, {@code readSigned}, {@code send}
 * aprobado y rechazado, {@code resolve}), el lote ({@code sendBatch} 0300/0301) y los eventos
 * ({@code cancel}, {@code invalidateNumbers}, {@code sendEvents}) con su {@code SendResult},
 * {@code BatchSendResult} y {@code EventResult} (CA10-02). Lo fino de la emisión —mismo XML, CDC
 * duplicado, bloqueo por CDC, golden de eventos— está en {@code SifenClientEmissionTest} (CA10-05..07). El barrido de
 * {@code null} ({@link #sinNulos}) revisa por reflexión todos los getters públicos del resultado, así un
 * getter nuevo queda cubierto sin tocar este test.</p>
 */
public class ApiContractTest {
    private static final Pattern ID = Pattern.compile("[0-9a-f]{8}");
    /** Getters que el javadoc del contrato permite en {@code null} (fechas que la respuesta puede no traer). */
    private static final Set<String> PUEDEN_SER_NULL = Set.of("getProcessingDate", "getSignatureDate",
            "getSubmissionDeadline", "getDocumentType", "getEnvironment", "getCategory");
    private static SifenSimulator sim;

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

    private static ISifenClient cliente(String empresa) throws SifenException {
        return SifenClient.create(sim.config(empresa));
    }

    private static ISifenClient cliente() throws SifenException {
        return cliente("");
    }

    private static Properties esperadoConsultaDE() throws Exception {
        Properties p = new Properties();
        try (InputStream in = ApiContractTest.class.getClassLoader()
                .getResourceAsStream("org/javabeanstack/sifen/simulator/responses/query-de-found.properties")) {
            p.load(in);
        }
        return p;
    }

    /**
     * Ningún getter público sin parámetros del resultado devuelve {@code null}, salvo los de
     * {@link #PUEDEN_SER_NULL}; recorre las listas de resultados anidados ({@code SendResult},
     * {@code SifenMessage}…).
     */
    static void sinNulos(Object resultado) throws Exception {
        Assertions.assertNotNull(resultado);
        for (Method m : resultado.getClass().getMethods()) {
            if (m.getParameterCount() != 0 || Modifier.isStatic(m.getModifiers()) || m.getDeclaringClass() == Object.class
                    || !(m.getName().startsWith("get") || m.getName().startsWith("is"))) {
                continue;
            }
            Object v = m.invoke(resultado);
            if (v == null) {
                Assertions.assertTrue(PUEDEN_SER_NULL.contains(m.getName()),
                        resultado.getClass().getSimpleName() + "." + m.getName() + "() devolvió null");
                continue;
            }
            if (v instanceof Collection<?> c) {
                for (Object e : c) {
                    Assertions.assertNotNull(e, resultado.getClass().getSimpleName() + "." + m.getName() + "(): elemento null");
                    if (e.getClass().getPackageName().equals(SendResult.class.getPackageName())) {
                        sinNulos(e);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ identidad y configuración

    @Test
    public void identidadConfiguracionYCertificadoSinNulos() throws Exception {
        ISifenClient c = cliente();
        Assertions.assertFalse(c.getVersion().isBlank());
        SifenConfig copia = c.getConfig();
        Assertions.assertEquals(sim.getBaseUrl(), copia.getBaseUrl());
        copia.setBaseUrl("https://otra.invalid");
        Assertions.assertEquals(sim.getBaseUrl(), c.getConfig().getBaseUrl(), "getConfig devuelve una copia");
        ValidationResult v = c.validateConfig();
        Assertions.assertTrue(v.isValid(), v.getErrors().toString());
        CertificateInfo info = c.getCertificateInfo();
        sinNulos(info);
        Assertions.assertTrue(info.isLoaded());
        Assertions.assertTrue(info.getRucs().contains(SifenSimulator.ISSUER_RUC + "-" + SifenSimulator.ISSUER_DV), info.getRucs().toString());
        Assertions.assertNotNull(info.getNotAfter());
        Assertions.assertTrue(info.getDaysRemaining() > 0);
        Assertions.assertTrue(sim.requests().isEmpty(), "ninguna de estas operaciones contacta a SIFEN");
    }

    @Test
    public void validacionPreviaDeDocumentoYEventosSinContactarASifen() throws Exception {
        ISifenClient c = cliente();
        ValidationResult de = c.validateDE(CasosDePrueba.documentos().get(4).construir());
        Assertions.assertNotNull(de);
        Assertions.assertNotNull(de.getErrors());
        Assertions.assertNotNull(de.getWarnings());
        ValidationResult nulo = c.validateDE(null);
        Assertions.assertFalse(nulo.isValid(), "un DE nulo es un error de la lista, no una excepción");
        ValidationResult ev = c.validateEvents(CasosDePrueba.eventos().get(0).construir());
        Assertions.assertNotNull(ev);
        Assertions.assertTrue(sim.requests().isEmpty());
    }

    @Test
    public void checkConnectionNoLanza() throws Exception {
        ConnectionDiagnosis d = cliente().checkConnection();
        sinNulos(d);
        Assertions.assertFalse(d.getSteps().isEmpty());
        Assertions.assertTrue(ID.matcher(d.getOperationId()).matches());
    }

    // ------------------------------------------------------------------ queryRuc

    @Test
    public void queryRucEncontrado() throws Exception {
        RucQueryResult r = cliente().queryRuc("80123456");
        sinNulos(r);
        Assertions.assertTrue(r.isFound());
        Assertions.assertEquals("80123456", r.getRuc());
        Assertions.assertEquals("EMPRESA FICTICIA DE PRUEBA S.A.", r.getBusinessName());
        Assertions.assertEquals("ACT", r.getTaxpayerStatusCode());
        Assertions.assertEquals("ACTIVO", r.getTaxpayerStatus());
        Assertions.assertTrue(r.isElectronicInvoicer());
        Assertions.assertEquals("0502", r.getResponseCode());
        Assertions.assertEquals("RUC encontrado", r.getMessage());
        Assertions.assertTrue(ID.matcher(r.getOperationId()).matches(), r.getOperationId());
        Assertions.assertTrue(r.getRawResponse().contains("rResEnviConsRuc"));
        Assertions.assertTrue(r.getDurationMs() >= 0);
        List<RecordedRequest> pedidos = sim.requests();
        Assertions.assertEquals(1, pedidos.size());
        Assertions.assertEquals(SimulatedService.QUERY_RUC, pedidos.get(0).getService());
        Assertions.assertEquals("80123456", XmlCanonico.texto(pedidos.get(0).getBody(), "dRUCCons"));
    }

    @Test
    public void queryRucNoEncontradoSinNulos() throws Exception {
        sim.on(SimulatedService.QUERY_RUC, Scenario.rucNotFound());
        RucQueryResult r = cliente().queryRuc("80000000");
        sinNulos(r);
        Assertions.assertFalse(r.isFound());
        Assertions.assertEquals("0500", r.getResponseCode());
        Assertions.assertFalse(r.isElectronicInvoicer());
    }

    @Test
    public void queryRucInvalidoNoContactaAlServidor() throws Exception {
        SifenException e = Assertions.assertThrows(SifenException.class, () -> cliente().queryRuc("80123456-5"));
        Assertions.assertEquals("JBS-ENT-04", e.getCode());
        Assertions.assertEquals(ErrorCategory.INPUT, e.getCategory());
        Assertions.assertTrue(ID.matcher(e.getCorrelationId()).matches());
        Assertions.assertTrue(sim.requests().isEmpty());
    }

    // ------------------------------------------------------------------ queryDE

    /** I10-11: respuesta grabada con el {@code rDE} firmado, su protocolo y un evento de cancelación. */
    @Test
    public void queryDEEncontrado() throws Exception {
        Properties esperado = esperadoConsultaDE();
        String cdc = esperado.getProperty("cdc");
        DEQueryResult r = cliente().queryDE(cdc);
        sinNulos(r);
        Assertions.assertEquals(ResultStatus.APPROVED, r.getStatus());
        Assertions.assertEquals(cdc, r.getCdc());
        Assertions.assertEquals(esperado.getProperty("protocol"), r.getAuthorizationProtocol());
        Assertions.assertNotNull(r.getProcessingDate());
        Assertions.assertEquals("0422", r.getResponseCode());
        Assertions.assertTrue(r.getXml().startsWith("<rDE"), "el rDE que tiene SIFEN: " + abreviado(r.getXml()));
        Assertions.assertTrue(r.getXml().contains("Id=\"" + cdc + "\""));
        Assertions.assertEquals(esperado.getProperty("digest"), r.getDigestValue(), "DigestValue del rDE de la respuesta");
        Assertions.assertEquals(Integer.parseInt(esperado.getProperty("events")), r.getEvents().size());
        Assertions.assertTrue(r.getEvents().get(0).contains("rContEv"), r.getEvents().get(0));
        Assertions.assertTrue(r.getEvents().get(0).contains(cdc), "el evento cancela este CDC");
        Assertions.assertTrue(ID.matcher(r.getOperationId()).matches());
        Assertions.assertEquals(SimulatedService.QUERY_DE, sim.requests().get(0).getService());
        Assertions.assertEquals(cdc, sim.requests().get(0).text("dCDC"));
    }

    /** El {@code rDE} que devuelve la consulta trae la firma del emisor: {@code verifySignature} la da por válida. */
    @Test
    public void elXmlDeLaConsultaPasaLaVerificacionDeFirma() throws Exception {
        ISifenClient c = cliente();
        DEQueryResult r = c.queryDE(esperadoConsultaDE().getProperty("cdc"));
        SignatureVerification v = c.verifySignature(r.getXml());
        sinNulos(v);
        Assertions.assertFalse(v.isFailure(), v.toString());
        Assertions.assertTrue(v.isValid(), v.getReason());
    }

    @Test
    public void queryDENoEncontradoSinNulos() throws Exception {
        sim.on(SimulatedService.QUERY_DE, Scenario.deNotFound());
        DEQueryResult r = cliente().queryDE(CasosDePrueba.CDC_FACTURA_SIMPLE);
        sinNulos(r);
        Assertions.assertEquals(ResultStatus.NOT_FOUND, r.getStatus());
        Assertions.assertEquals("0420", r.getResponseCode());
        Assertions.assertEquals("", r.getXml());
        Assertions.assertEquals("", r.getDigestValue());
        Assertions.assertTrue(r.getEvents().isEmpty());
        Assertions.assertEquals("", r.getAuthorizationProtocol());
    }

    @Test
    public void queryDEConErrorDelServidorLlevaElIdDeLaOperacion() throws Exception {
        sim.on(SimulatedService.QUERY_DE, Scenario.badGateway());
        SifenException e = Assertions.assertThrows(SifenException.class,
                () -> cliente().queryDE(CasosDePrueba.CDC_FACTURA_SIMPLE));
        Assertions.assertEquals(ErrorCategory.CONNECTION, e.getCategory(), e.getCode());
        Assertions.assertTrue(ID.matcher(e.getCorrelationId()).matches());
        Assertions.assertTrue(e.getMessage().endsWith("(ref. " + e.getCorrelationId() + ")"));
        Assertions.assertEquals(CasosDePrueba.CDC_FACTURA_SIMPLE, e.getReference());
        Assertions.assertFalse(e.isOutcomeUnknown(), "una consulta no deja un envío desconocido");
    }

    // ------------------------------------------------------------------ queryBatch

    @Test
    public void queryBatchConcluido() throws Exception {
        BatchQueryResult r = cliente().queryBatch(Scenario.BATCH_NUMBER);
        sinNulos(r);
        Assertions.assertEquals(BatchStatus.COMPLETED, r.getStatus());
        Assertions.assertEquals(Scenario.BATCH_NUMBER, r.getBatchNumber());
        Assertions.assertEquals(Duration.ZERO, r.getSuggestedWait());
        Assertions.assertEquals("0362", r.getResponseCode());
        Assertions.assertEquals(1, r.getResults().size());
        SendResult doc = r.getResults().get(0);
        Assertions.assertEquals(ResultStatus.APPROVED, doc.getStatus());
        Assertions.assertEquals("1000000002", doc.getAuthorizationProtocol());
        Assertions.assertEquals("0260", doc.getResponseCode());
        Assertions.assertEquals(r.getOperationId(), doc.getOperationId());
        Assertions.assertFalse(doc.isDigestMismatch(), "sin XML local con qué comparar (D43)");
        Assertions.assertEquals(SimulatedService.QUERY_BATCH, sim.requests().get(0).getService());
    }

    @Test
    public void queryBatchEnProcesoSugiereEsperar() throws Exception {
        sim.on(SimulatedService.QUERY_BATCH, Scenario.batchInProcess());
        BatchQueryResult r = cliente().queryBatch(Scenario.BATCH_NUMBER);
        sinNulos(r);
        Assertions.assertEquals(BatchStatus.IN_PROCESS, r.getStatus());
        Assertions.assertEquals("0361", r.getResponseCode());
        Assertions.assertTrue(r.getSuggestedWait().compareTo(Duration.ZERO) > 0, r.getSuggestedWait().toString());
        Assertions.assertTrue(r.getResults().isEmpty());
    }

    /** Respuesta real anonimizada: 8 DE, 7 aprobados y uno rechazado con 1306, repartidos por CDC. */
    @Test
    public void queryBatchRealRepartePorCdc() throws Exception {
        sim.on(SimulatedService.QUERY_BATCH, Scenario.batchCompletedReal());
        BatchQueryResult r = cliente().queryBatch(Scenario.BATCH_NUMBER);
        sinNulos(r);
        Assertions.assertEquals(8, r.getResults().size());
        long aprobados = r.getResults().stream().filter(s -> s.getStatus() == ResultStatus.APPROVED).count();
        List<SendResult> rechazados = r.getResults().stream().filter(s -> s.getStatus() == ResultStatus.REJECTED).toList();
        Assertions.assertEquals(7, aprobados);
        Assertions.assertEquals(1, rechazados.size());
        Assertions.assertEquals("1306", rechazados.get(0).getResponseCode());
        for (SendResult s : r.getResults()) {
            Assertions.assertEquals(44, s.getCdc().length(), s.getCdc());
            for (SifenMessage m : s.getMessages()) {
                Assertions.assertFalse(m.getCode().isBlank());
            }
        }
    }

    // ------------------------------------------------------------------ emisión, lote y eventos (§10.3/§10.4, CA10-02)

    private static final AtomicInteger NUMERO = new AtomicInteger(500);

    /** El DE del caso 4 del catálogo con un número propio y las leyendas de DEV (Q14), como en {@code SifenClientEmissionTest}. */
    private static DocumentoElectronico documento() {
        DocumentoElectronico de = CasosDePrueba.documentos().get(4).construir();
        de.getgDatGralOpe().getgEmis().setdNomEmi("DE generado en ambiente de prueba - sin valor comercial ni fiscal");
        de.getgDtipDE().getgCamItemList().get(0).setdDesProSer(
                "DOCUMENTO ELECTRÓNICO SIN VALOR COMERCIAL NI FISCAL - GENERADO EN AMBIENTE DE PRUEBA");
        de.getgTimb().setdNumDoc(String.format("%07d", NUMERO.incrementAndGet()));
        return de;
    }

    @Test
    public void firmarYReleerDevuelvenUnDocumentoSinNulos() throws Exception {
        ISifenClient c = cliente();
        SignedDocument doc = c.sign(documento());
        sinNulos(doc);
        Assertions.assertEquals(44, doc.getCdc().length());
        Assertions.assertNotNull(doc.getSignatureDate(), "sign completa la fecha de firma");
        Assertions.assertNotNull(doc.getSubmissionDeadline());
        SignedDocument releido = c.readSigned(doc.getXml());
        sinNulos(releido);
        Assertions.assertEquals(doc.getXml(), releido.getXml(), "readSigned no regenera");
        Assertions.assertTrue(sim.requests().isEmpty(), "firmar y releer no contactan a SIFEN");
    }

    @Test
    public void enviarAprobadoSinNulos() throws Exception {
        ISifenClient c = cliente();
        SignedDocument doc = c.sign(documento());
        SendResult r = c.send(doc);
        sinNulos(r);
        Assertions.assertEquals(ResultStatus.APPROVED, r.getStatus());
        Assertions.assertEquals(doc.getCdc(), r.getCdc());
        Assertions.assertEquals(Scenario.AUTHORIZATION_PROTOCOL, r.getAuthorizationProtocol());
        Assertions.assertEquals(doc.getDigestValue(), r.getDigestValue(), "dDigVal es el DigestValue enviado");
        Assertions.assertFalse(r.isDigestMismatch(), "D43: el digest de SIFEN es el del XML local");
        Assertions.assertFalse(r.isAlreadyRegistered());
        Assertions.assertEquals("0260", r.getResponseCode());
        Assertions.assertTrue(ID.matcher(r.getOperationId()).matches());
        Assertions.assertTrue(r.getRawResponse().contains("rRetEnviDe"));
        Assertions.assertEquals(1, sim.requests(SimulatedService.RECEIVE_DE).size());
    }

    @Test
    public void enviarRechazadoSinNulos() throws Exception {
        sim.on(SimulatedService.RECEIVE_DE, Scenario.rejected("1306", "RUC del receptor inexistente"));
        ISifenClient c = cliente();
        SendResult r = c.send(c.sign(documento()));
        sinNulos(r);
        Assertions.assertEquals(ResultStatus.REJECTED, r.getStatus());
        Assertions.assertEquals("1306", r.getResponseCode());
        Assertions.assertEquals("RUC del receptor inexistente", r.getMessage());
        Assertions.assertEquals("", r.getAuthorizationProtocol(), "un rechazo no trae dProtAut");
        Assertions.assertFalse(r.getMessages().isEmpty());
    }

    @Test
    public void resolverSinNulos() throws Exception {
        ISifenClient c = cliente();
        SendResult r = c.resolve(c.sign(documento()));
        sinNulos(r);
        Assertions.assertTrue(r.isAlreadyRegistered(), "SIFEN lo tiene: sale de la consulta");
        Assertions.assertTrue(sim.requests(SimulatedService.RECEIVE_DE).isEmpty());
    }

    @Test
    public void loteRecibidoYNoEncoladoSinNulos() throws Exception {
        ISifenClient c = cliente();
        SignedDocument a = c.sign(documento());
        SignedDocument b = c.sign(documento());
        BatchSendResult r = c.sendBatch(List.of(a, b));
        sinNulos(r);
        Assertions.assertEquals(BatchStatus.RECEIVED, r.getStatus());
        Assertions.assertEquals(Scenario.BATCH_NUMBER, r.getBatchNumber());
        Assertions.assertEquals(List.of(a.getCdc(), b.getCdc()), r.getCdcs());
        Assertions.assertEquals("0300", r.getResponseCode());
        sim.on(SimulatedService.RECEIVE_BATCH, Scenario.batchNotQueued());
        BatchSendResult n = c.sendBatch(List.of(c.sign(documento())));
        sinNulos(n);
        Assertions.assertEquals(BatchStatus.NOT_QUEUED, n.getStatus());
        Assertions.assertEquals("", n.getBatchNumber());
    }

    @Test
    public void eventosSinNulos() throws Exception {
        ISifenClient c = cliente();
        EventResult can = c.cancel(CasosDePrueba.CDC_FACTURA_SIMPLE, "Cancelacion de prueba del documento");
        sinNulos(can);
        Assertions.assertEquals(ResultStatus.APPROVED, can.getStatus(), can.toString());
        Assertions.assertTrue(can.getEventXml().contains("rGeVeCan"));
        Assertions.assertEquals("2000000001", can.getAuthorizationProtocol());
        NumberRange rango = NumberRange.builder().timbrado(String.valueOf(CasosDePrueba.TIMBRADO)).establishment("1")
                .issuePoint("1").documentType(TTiDE.FACTURA_ELECTRONICA).from("50").to("55")
                .reason("Inutilizacion de prueba de numeracion").build();
        sinNulos(rango);
        EventResult inu = c.invalidateNumbers(rango);
        sinNulos(inu);
        Assertions.assertTrue(inu.getEventXml().contains("rGeVeInu"));
        EventResult otros = c.sendEvents(CasosDePrueba.eventos().get(0).construir());
        sinNulos(otros);
        Assertions.assertEquals(3, sim.requests(SimulatedService.EVENT).size());
        Assertions.assertNotEquals(can.getEventId(), inu.getEventId());
    }

    // ------------------------------------------------------------------ contador de pedidos por cliente

    @Test
    public void cadaClienteNumeraSusPedidosDesdeUno() throws Exception {
        ISifenClient a = cliente("empresa-a");
        ISifenClient b = cliente("empresa-b");
        int n = 5;
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> futuros = new ArrayList<>();
            for (ISifenClient c : List.of(a, b)) {
                futuros.add(pool.submit(() -> {
                    largada.await();
                    for (int i = 0; i < n; i++) {
                        Assertions.assertTrue(c.queryRuc("80123456").isFound());
                    }
                    return null;
                }));
            }
            largada.countDown();
            for (Future<?> f : futuros) {
                f.get(120, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        List<Long> esperado = List.of(1L, 2L, 3L, 4L, 5L);
        Assertions.assertEquals(esperado, ids("empresa-a"), "empresa A");
        Assertions.assertEquals(esperado, ids("empresa-b"), "empresa B: su propio contador, no el de A ni el estático de Sifen");
    }

    private static List<Long> ids(String empresa) {
        List<Long> ids = new ArrayList<>();
        for (RecordedRequest p : sim.requests(empresa)) {
            ids.add(p.getRequestId());
        }
        Collections.sort(ids);
        return ids;
    }

    private static String abreviado(String s) {
        return s.length() > 120 ? s.substring(0, 120) + "…" : s;
    }
}

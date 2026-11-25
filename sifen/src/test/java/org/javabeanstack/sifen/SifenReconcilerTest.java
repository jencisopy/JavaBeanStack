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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.javabeanstack.sifen.api.DEQueryResult;
import org.javabeanstack.sifen.api.ReconciliationItem;
import org.javabeanstack.sifen.api.ReconciliationResult;
import org.javabeanstack.sifen.api.ReconciliationStatus;
import org.javabeanstack.sifen.api.ResultStatus;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.exceptions.ErrorCategory;
import org.javabeanstack.sifen.core.exceptions.Operation;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.simulator.RecordedRequest;
import org.javabeanstack.sifen.simulator.Scenario;
import org.javabeanstack.sifen.simulator.SifenSimulator;
import org.javabeanstack.sifen.simulator.SimulatedService;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Conciliación local ↔ SIFEN contra el simulador, sin red (plan JBSSIF, §8.8; M3-30).
 *
 * <p>La 0422 del simulador es fija (M8-03): contesta el {@code rDE} firmado de {@code golden/DE-01.xml} para
 * cualquier CDC. Por eso el documento <b>coincidente</b> es el propio DE-01 releído con {@code readSigned}, y un
 * DE recién firmado es el de <b>otro digest</b> bajo la 0422 por omisión; el <b>ausente</b> se encola con
 * {@code once(QUERY_DE, deNotFound())}. En todos los casos se comprueba que la conciliación solo consulta: ningún
 * pedido a la recepción de DE, de lote ni de eventos (D41).</p>
 */
public class SifenReconcilerTest {
    private static final String SOAP = "application/soap+xml; charset=utf-8";
    private static final Duration PAUSA = Duration.ofMillis(250);

    private final AtomicInteger numero = new AtomicInteger(900);
    private final List<Duration> pausas = new ArrayList<>();
    private SifenSimulator sim;
    private ISifenClient c;

    @BeforeEach
    public void abrir() throws Exception {
        sim = SifenSimulator.start();
        SifenConfig cfg = sim.config();
        cfg.setHttpReadTimeout(500);
        cfg.setMaxRetries(0);
        c = SifenClient.create(cfg);
    }

    @AfterEach
    public void cerrar() {
        if (c != null) {
            c.close();
        }
        if (sim != null) {
            sim.close();
        }
    }

    // ------------------------------------------------------------------ el caso del plan

    /** §8.8: uno coincidente, uno ausente y uno con otro digest; ritmo controlado y sin reenvíos. */
    @Test
    public void coincidenteAusenteYDigestDistinto() throws Exception {
        SignedDocument coincide = golden();
        SignedDocument ausente = c.sign(dev(armado("DE-05")));
        SignedDocument otroDigest = c.sign(dev(armado("DE-05")));
        sim.reset();
        sim.once(SimulatedService.QUERY_DE, Scenario.deFound());
        sim.once(SimulatedService.QUERY_DE, Scenario.deNotFound());
        // el tercero cae en la 0422 por omisión: el rDE de DE-01, con otro digest (M8-03)

        ReconciliationResult r = conciliador().reconcile(List.of(coincide, ausente, otroDigest));

        Assertions.assertEquals(List.of(ReconciliationStatus.MATCH, ReconciliationStatus.NOT_FOUND,
                ReconciliationStatus.DIGEST_MISMATCH), estados(r), r.toString());
        Assertions.assertEquals(List.of(coincide.getCdc(), ausente.getCdc(), otroDigest.getCdc()),
                r.getItems().stream().map(ReconciliationItem::getCdc).toList(), "el orden de la entrada");
        Assertions.assertEquals(List.of(ausente.getCdc(), otroDigest.getCdc()),
                r.getDiscrepancies().stream().map(ReconciliationItem::getCdc).toList());
        Assertions.assertEquals("0420", r.getItems().get(1).getResponseCode());
        Assertions.assertEquals("0422", r.getItems().get(2).getResponseCode());
        Assertions.assertNotEquals(otroDigest.getDigestValue(), r.getItems().get(2).getQueryResult().getDigestValue());
        Assertions.assertTrue(r.isComplete());
        Assertions.assertEquals(ReconciliationResult.StopReason.COMPLETED, r.getStopReason());
        Assertions.assertEquals(3, r.getQueryCount());
        Assertions.assertEquals(List.of(coincide.getCdc(), ausente.getCdc(), otroDigest.getCdc()), cdcsConsultados());
        Assertions.assertEquals(List.of(PAUSA, PAUSA), pausas, "una pausa entre cada par de consultas, no antes ni después");
        sinEnvios();
    }

    // ------------------------------------------------------------------ no se pudo determinar (D43)

    /** Una falla de transporte, un código desconocido y un 0422 sin rDE: ninguno es «ausente». */
    @Test
    public void lasFallasNoSonAusentes() throws Exception {
        SignedDocument corte = c.sign(dev(armado("DE-05")));
        SignedDocument raro = c.sign(dev(armado("DE-05")));
        SignedDocument sinRde = c.sign(dev(armado("DE-05")));
        SignedDocument bien = golden();
        sim.reset();
        sim.once(SimulatedService.QUERY_DE, Scenario.connectionCut());
        sim.once(SimulatedService.QUERY_DE, Scenario.body(200, SOAP, respuesta("0421", "Codigo no previsto", "")));
        sim.once(SimulatedService.QUERY_DE, Scenario.body(200, SOAP, respuesta("0422", "CDC encontrado", "")));

        ReconciliationResult r = conciliador().reconcile(List.of(corte, raro, sinRde, bien));

        Assertions.assertEquals(List.of(ReconciliationStatus.UNDETERMINED, ReconciliationStatus.UNDETERMINED,
                ReconciliationStatus.UNDETERMINED, ReconciliationStatus.MATCH), estados(r), r.toString());
        ReconciliationItem cortado = r.getItems().get(0);
        Assertions.assertNotNull(cortado.getError(), "la falla de transporte viaja en el veredicto");
        Assertions.assertEquals(ErrorCategory.CONNECTION, cortado.getError().getCategory(), cortado.getError().getCode());
        Assertions.assertNull(cortado.getQueryResult());
        Assertions.assertEquals("0421", r.getItems().get(1).getResponseCode());
        Assertions.assertEquals(ResultStatus.UNRECOGNIZED, r.getItems().get(1).getQueryResult().getStatus());
        ReconciliationItem vacio = r.getItems().get(2);
        Assertions.assertEquals("0422", vacio.getResponseCode(), "0422 sin rDE: SIFEN lo tiene pero no hay con qué comparar");
        Assertions.assertNull(vacio.getError(), "la consulta respondió: es el veredicto, no una falla");
        Assertions.assertTrue(vacio.getQueryResult().getDigestValue().isEmpty());
        Assertions.assertFalse(SifenClientResults.fromQuery(vacio.getQueryResult(), sinRde.getDigestValue(), 0).isDigestMismatch(),
                "isDigestMismatch() falso no prueba la coincidencia");
        Assertions.assertTrue(r.getDiscrepancies().isEmpty(), "ninguna falla es una discrepancia");
        Assertions.assertEquals(4, r.getQueryCount(), "una falla no corta la pasada");
        sinEnvios();

        // serializable, también con la excepción de transporte adentro (la aplicación puede encolarlo)
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(r);
        }
        ReconciliationResult copia;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copia = (ReconciliationResult) in.readObject();
        }
        Assertions.assertEquals(estados(r), estados(copia));
        Assertions.assertEquals(cortado.getError().getCode(), copia.getItems().get(0).getError().getCode());
    }

    /** Un documento de otro ambiente no se consulta: en el ambiente equivocado daría 0420 (JBS-ENT-12). */
    @Test
    public void otroAmbienteNoSeConsulta() throws Exception {
        SignedDocument dev = golden();
        SignedDocument prod = SignedDocument.builder().xml(dev.getXml()).cdc(dev.getCdc()).digestValue(dev.getDigestValue())
                .qrLink(dev.getQRLink()).environment(SifenConfig.Environment.PROD).build();
        SignedDocument sinAmbiente = SignedDocument.builder().xml(dev.getXml()).cdc(dev.getCdc())
                .digestValue(dev.getDigestValue()).build();
        sim.reset();

        ReconciliationResult r = conciliador().reconcile(List.of(prod, dev, sinAmbiente));

        Assertions.assertEquals(List.of(ReconciliationStatus.UNDETERMINED, ReconciliationStatus.MATCH,
                ReconciliationStatus.UNDETERMINED), estados(r), r.toString());
        Assertions.assertEquals("JBS-ENT-12", r.getItems().get(0).getError().getCode());
        Assertions.assertEquals("JBS-ENT-12", r.getItems().get(2).getError().getCode());
        SifenException otro = r.getItems().get(0).getError();
        conId(otro);
        Assertions.assertEquals(ErrorCategory.INPUT, otro.getCategory());
        Assertions.assertEquals(prod.getCdc(), otro.getReference(), "la referencia es el CDC");
        Assertions.assertTrue(otro.getUserMessage().contains("PROD") && otro.getUserMessage().contains("DEV"),
                otro.getUserMessage());
        sinTextoDeEnvio(otro);
        Assertions.assertTrue(r.getItems().get(2).getError().getUserMessage().contains("desconocido"),
                r.getItems().get(2).getError().getUserMessage());
        Assertions.assertNotEquals(otro.getCorrelationId(), r.getItems().get(2).getError().getCorrelationId(),
                "un id por error");
        Assertions.assertEquals(1, r.getQueryCount());
        Assertions.assertEquals(List.of(dev.getCdc()), cdcsConsultados(), "solo se consultó el del ambiente del cliente");
        Assertions.assertTrue(pausas.isEmpty(), "sin pausa por los que no se consultan");
    }

    /** D41: la consulta reintenta según {@code sifen.reintentos.*}; la conciliación no agrega reintentos. */
    @Test
    public void laConsultaReintentaSegunLaConfiguracion() throws Exception {
        SifenConfig cfg = sim.config();
        cfg.setHttpReadTimeout(500);
        cfg.setMaxRetries(1);
        cfg.setRetryBackoffMs(10);
        try (ISifenClient conReintento = SifenClient.create(cfg)) {
            SignedDocument doc = conReintento.readSigned(FixturesSifen.recurso("golden/DE-01.xml"));
            sim.reset();
            sim.once(SimulatedService.QUERY_DE, Scenario.badGateway());
            ReconciliationResult r = SifenReconciler.builder(conReintento).pause(PAUSA).sleeper(pausas::add).build()
                    .reconcile(List.of(doc));
            Assertions.assertEquals(List.of(ReconciliationStatus.MATCH), estados(r), r.toString());
            Assertions.assertEquals(1, r.getQueryCount(), "una consulta para la conciliación");
            Assertions.assertEquals(2, sim.requests(SimulatedService.QUERY_DE).size(), "la 502 y su reintento");
            sinEnvios();
        }
    }

    // ------------------------------------------------------------------ ritmo: tope e interrupción

    /** El tope corta la pasada y deja el resto pendiente, sin veredicto. */
    @Test
    public void elTopeDejaPendientes() throws Exception {
        SignedDocument a = golden();
        SignedDocument b = c.sign(dev(armado("DE-05")));
        SignedDocument k = c.sign(dev(armado("DE-05")));
        sim.reset();

        ReconciliationResult r = SifenReconciler.builder(c).pause(PAUSA).maxQueries(2).sleeper(pausas::add).build()
                .reconcile(List.of(a, b, k));

        Assertions.assertEquals(2, r.getItems().size());
        Assertions.assertEquals(List.of(k), r.getPending());
        Assertions.assertFalse(r.isComplete());
        Assertions.assertEquals(ReconciliationResult.StopReason.MAX_QUERIES, r.getStopReason());
        Assertions.assertEquals(2, sim.requests(SimulatedService.QUERY_DE).size());
        Assertions.assertEquals(1, pausas.size());
    }

    /** Interrumpido en la pausa: termina, deja pendientes y el hilo sigue marcado como interrumpido. */
    @Test
    public void laInterrupcionDejaPendientes() throws Exception {
        SignedDocument a = golden();
        SignedDocument b = c.sign(dev(armado("DE-05")));
        sim.reset();
        ReconciliationResult r;
        try {
            r = SifenReconciler.builder(c).pause(PAUSA).sleeper(p -> {
                throw new InterruptedException("prueba");
            }).build().reconcile(List.of(a, b));
            Assertions.assertTrue(Thread.currentThread().isInterrupted(), "se restituye la marca de interrupción");
        } finally {
            Thread.interrupted();
        }
        Assertions.assertEquals(ReconciliationResult.StopReason.INTERRUPTED, r.getStopReason());
        Assertions.assertEquals(List.of(ReconciliationStatus.MATCH), estados(r));
        Assertions.assertEquals(List.of(b), r.getPending());
        Assertions.assertEquals(1, sim.requests(SimulatedService.QUERY_DE).size());
    }

    /** Sin pausa no se duerme; una lista vacía da un resultado vacío y completo. */
    @Test
    public void sinPausaYListaVacia() throws Exception {
        SignedDocument a = golden();
        sim.reset();
        ReconciliationResult r = SifenReconciler.builder(c).pause(Duration.ZERO).sleeper(pausas::add).build()
                .reconcile(List.of(a, a));
        Assertions.assertEquals(List.of(ReconciliationStatus.MATCH, ReconciliationStatus.MATCH), estados(r),
                "un CDC repetido se consulta cada vez");
        Assertions.assertTrue(pausas.isEmpty());
        ReconciliationResult vacio = conciliador().reconcile(List.of());
        Assertions.assertTrue(vacio.isComplete());
        Assertions.assertTrue(vacio.getItems().isEmpty());
        Assertions.assertEquals(0, vacio.getQueryCount());
    }

    // ------------------------------------------------------------------ argumentos

    /** Lista nula o con un nulo: {@code JBS-ENT-11} antes de consultar nada. */
    @Test
    public void listaNulaOConNulos() throws Exception {
        SignedDocument a = golden();
        sim.reset();
        SifenReconciler rec = conciliador();
        SifenException nula = Assertions.assertThrows(SifenException.class, () -> rec.reconcile(null));
        SifenException conNulo = Assertions.assertThrows(SifenException.class, () -> rec.reconcile(Arrays.asList(a, null)));
        for (SifenException e : List.of(nula, conNulo)) {
            Assertions.assertEquals("JBS-ENT-11", e.getCode());
            Assertions.assertEquals(ErrorCategory.INPUT, e.getCategory());
            conId(e);
            sinTextoDeEnvio(e);
            Assertions.assertTrue(e.getUserMessage().contains("conciliar"), e.getUserMessage());
        }
        Assertions.assertTrue(conNulo.getUserMessage().contains("documento 2"), conNulo.getUserMessage());
        Assertions.assertTrue(sim.requests().isEmpty(), "no se consultó nada");
        Assertions.assertThrows(NullPointerException.class, () -> SifenReconciler.builder(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SifenReconciler.builder(c).pause(Duration.ofMillis(-1)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SifenReconciler.builder(c).maxQueries(-1));
        SifenReconciler omision = SifenReconciler.builder(c).build();
        Assertions.assertEquals(SifenReconciler.DEFAULT_PAUSE, omision.getPause());
        Assertions.assertEquals(0, omision.getMaxQueries());
    }

    /** Veredicto sin red: «no hay digest distinto» no es «coincide». */
    @Test
    public void veredictoSinDigestNoEsCoincidencia() {
        DEQueryResult sinDigest = DEQueryResult.builder().status(ResultStatus.APPROVED).build();
        DEQueryResult conDigest = DEQueryResult.builder().status(ResultStatus.APPROVED).digestValue("abc=").build();
        Assertions.assertEquals(ReconciliationStatus.UNDETERMINED, SifenReconciler.verdict(sinDigest, "abc="));
        Assertions.assertEquals(ReconciliationStatus.UNDETERMINED, SifenReconciler.verdict(conDigest, ""));
        Assertions.assertEquals(ReconciliationStatus.UNDETERMINED, SifenReconciler.verdict(conDigest, null));
        Assertions.assertEquals(ReconciliationStatus.MATCH, SifenReconciler.verdict(conDigest, " abc= "));
        Assertions.assertEquals(ReconciliationStatus.DIGEST_MISMATCH, SifenReconciler.verdict(conDigest, "xyz="));
        Assertions.assertEquals(ReconciliationStatus.NOT_FOUND,
                SifenReconciler.verdict(DEQueryResult.builder().status(ResultStatus.NOT_FOUND).build(), "abc="));
        Assertions.assertEquals(ReconciliationStatus.UNDETERMINED,
                SifenReconciler.verdict(DEQueryResult.builder().status(ResultStatus.REJECTED).build(), "abc="));
        Assertions.assertEquals(ReconciliationStatus.UNDETERMINED,
                SifenReconciler.verdict(DEQueryResult.builder().build(), "abc="));
    }

    /** M8-10: una pausa no nula se redondea hacia arriba al milisegundo; una que desbordaría {@code toMillis()}, no. */
    @Test
    public void pausaRedondeadaYAcotada() {
        Assertions.assertEquals(Duration.ofMillis(1), SifenReconciler.builder(c).pause(Duration.ofNanos(1)).build().getPause());
        Assertions.assertEquals(Duration.ofMillis(1), SifenReconciler.builder(c).pause(Duration.ofNanos(500_000)).build().getPause());
        Assertions.assertEquals(Duration.ofMillis(2), SifenReconciler.builder(c).pause(Duration.ofNanos(1_200_000)).build().getPause());
        Assertions.assertEquals(PAUSA, SifenReconciler.builder(c).pause(PAUSA).build().getPause(), "lo entero no cambia");
        Assertions.assertEquals(Duration.ZERO, SifenReconciler.builder(c).pause(null).build().getPause());
        Assertions.assertEquals(Duration.ofMillis(Long.MAX_VALUE),
                SifenReconciler.builder(c).pause(Duration.ofMillis(Long.MAX_VALUE)).build().getPause(), "el máximo que cabe");
        Assertions.assertThrows(IllegalArgumentException.class, () -> SifenReconciler.builder(c).pause(Duration.ofSeconds(Long.MAX_VALUE)));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> SifenReconciler.builder(c).pause(Duration.ofMillis(Long.MAX_VALUE).plusNanos(1)), "redondeado no cabe");
    }

    /** M8-10: un cliente con {@code getConfig()} nulo no da un NPE: nada se consulta y todo queda sin determinar. */
    @Test
    public void clienteSinConfiguracion() throws Exception {
        SignedDocument a = golden();
        ISifenClient roto = (ISifenClient) Proxy.newProxyInstance(ISifenClient.class.getClassLoader(),
                new Class<?>[] {ISifenClient.class}, (proxy, m, args) -> {
                    if (m.getName().equals("getConfig")) {
                        return null;
                    }
                    if (m.getName().equals("toString")) {
                        return "cliente sin configuración";
                    }
                    throw new AssertionError("la conciliación no debía llamar a " + m.getName());
                });
        ReconciliationResult r = SifenReconciler.builder(roto).pause(PAUSA).sleeper(pausas::add).build().reconcile(List.of(a, a));
        Assertions.assertEquals(List.of(ReconciliationStatus.UNDETERMINED, ReconciliationStatus.UNDETERMINED), estados(r));
        Assertions.assertTrue(r.isComplete());
        Assertions.assertEquals(0, r.getQueryCount());
        SifenException e = r.getItems().get(0).getError();
        Assertions.assertEquals("JBS-ENT-12", e.getCode());
        conId(e);
        Assertions.assertTrue(e.getUserMessage().contains("desconocido"), e.getUserMessage());
        Assertions.assertTrue(SifenReconciler.builder(roto).build().reconcile(List.of()).isComplete(), "sin documentos no hace falta");
    }

    /** I8-25: la constante se llama como la consulta (0420) y su texto no dice «ausente». */
    @Test
    public void descripcionDelNoEncontrado() {
        Assertions.assertEquals("NOT_FOUND", ReconciliationStatus.NOT_FOUND.name());
        Assertions.assertEquals("Sin aprobación en SIFEN (0420)", ReconciliationStatus.NOT_FOUND.getDescription());
        Assertions.assertEquals(ReconciliationStatus.NOT_FOUND,
                SifenReconciler.verdict(DEQueryResult.builder().status(ResultStatus.NOT_FOUND).build(), "abc="));
    }

    /** I8-20 (2): el motivo y los pendientes no se pueden contradecir. */
    @Test
    public void resultadoCoherente() throws Exception {
        SignedDocument a = golden();
        Assertions.assertThrows(IllegalArgumentException.class, () -> ReconciliationResult.of(List.of(), List.of(a), null, 0, 0));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ReconciliationResult.of(List.of(), List.of(a), ReconciliationResult.StopReason.COMPLETED, 0, 0));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ReconciliationResult.of(List.of(), List.of(), ReconciliationResult.StopReason.MAX_QUERIES, 0, 0));
        ReconciliationResult vacio = ReconciliationResult.of(null, null, null, 0, 0);
        Assertions.assertEquals(ReconciliationResult.StopReason.COMPLETED, vacio.getStopReason());
        Assertions.assertTrue(vacio.isComplete());
        ReconciliationResult tope = ReconciliationResult.of(List.of(), List.of(a), ReconciliationResult.StopReason.MAX_QUERIES, 0, 0);
        Assertions.assertFalse(tope.isComplete());
    }

    // ------------------------------------------------------------------ apoyo

    /** I8-18: el error salió de la conciliación con operación, id de 8 hexadecimales y {@code (ref. <id>)}. */
    private static void conId(SifenException e) {
        Assertions.assertEquals(Operation.QUERY_DE, e.getOperation(), e.toString());
        Assertions.assertNotNull(e.getCorrelationId(), e.toString());
        Assertions.assertTrue(e.getCorrelationId().matches("[0-9a-f]{8}"), e.getCorrelationId());
        Assertions.assertTrue(e.getMessage().endsWith("(ref. " + e.getCorrelationId() + ")"), e.getMessage());
    }

    /** I8-19: en la conciliación el mensaje y la acción del código no hablan de enviar ni del lote de 1 a 50. */
    private static void sinTextoDeEnvio(SifenException e) {
        String accion = org.javabeanstack.sifen.core.exceptions.ErrorCode.byCode(e.getCode()).orElseThrow().getAction();
        for (String texto : List.of(e.getUserMessage(), accion)) {
            String m = texto.toLowerCase(java.util.Locale.ROOT);
            Assertions.assertFalse(m.contains("enví") || m.contains("envia") || m.contains("lote") || m.contains("50"), m);
        }
    }

    private SifenReconciler conciliador() {
        return SifenReconciler.builder(c).pause(PAUSA).sleeper(pausas::add).build();
    }

    private SignedDocument golden() throws Exception {
        return c.readSigned(FixturesSifen.recurso("golden/DE-01.xml"));
    }

    private static List<ReconciliationStatus> estados(ReconciliationResult r) {
        return r.getItems().stream().map(ReconciliationItem::getStatus).toList();
    }

    private List<String> cdcsConsultados() {
        return sim.requests(SimulatedService.QUERY_DE).stream().map(RecordedRequest::getCdc).toList();
    }

    /** D41: la conciliación solo consulta; nada llega a la recepción de DE, de lote ni a los eventos. */
    private void sinEnvios() {
        Assertions.assertTrue(sim.requests(SimulatedService.RECEIVE_DE).isEmpty(), "ningún envío de DE");
        Assertions.assertTrue(sim.requests(SimulatedService.RECEIVE_BATCH).isEmpty(), "ningún lote");
        Assertions.assertTrue(sim.requests(SimulatedService.EVENT).isEmpty(), "ningún evento");
    }

    /** Respuesta de consulta de DE con el código dado y, opcionalmente, un contenido. */
    private static String respuesta(String code, String message, String content) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\">"
                + "<env:Header/><env:Body><ns2:rEnviConsDeResponse xmlns:ns2=\"http://ekuatia.set.gov.py/sifen/xsd\">"
                + "<ns2:dFecProc>2026-11-24T10:06:00-03:00</ns2:dFecProc><ns2:dCodRes>" + code + "</ns2:dCodRes>"
                + "<ns2:dMsgRes>" + message + "</ns2:dMsgRes>" + content
                + "</ns2:rEnviConsDeResponse></env:Body></env:Envelope>";
    }

    private static DocumentoElectronico armado(String id) {
        return CasosDePrueba.documentos().stream().filter(k -> k.getId().equals(id)).findFirst().orElseThrow().construir();
    }

    /** Leyendas del ambiente de pruebas (Q14) y un número propio, como el recorrido del beta. */
    private DocumentoElectronico dev(DocumentoElectronico de) {
        de.getgDatGralOpe().getgEmis().setdNomEmi("DE generado en ambiente de prueba - sin valor comercial ni fiscal");
        de.getgDtipDE().getgCamItemList().get(0).setdDesProSer(
                "DOCUMENTO ELECTRÓNICO SIN VALOR COMERCIAL NI FISCAL - GENERADO EN AMBIENTE DE PRUEBA");
        de.getgTimb().setdNumDoc(String.format("%07d", numero.incrementAndGet()));
        return de;
    }
}

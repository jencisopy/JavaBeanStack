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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.javabeanstack.sifen.ISifenClient;
import org.javabeanstack.sifen.SifenClient;
import org.javabeanstack.sifen.api.BatchQueryResult;
import org.javabeanstack.sifen.api.BatchSendResult;
import org.javabeanstack.sifen.api.BatchStatus;
import org.javabeanstack.sifen.api.EventResult;
import org.javabeanstack.sifen.api.NumberRange;
import org.javabeanstack.sifen.api.ResultStatus;
import org.javabeanstack.sifen.api.SendResult;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.EventosDE;
import org.javabeanstack.sifen.core.exceptions.ErrorCategory;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.fields.request.event.TrGesEve;
import org.javabeanstack.sifen.core.types.TTiDE;
import org.javabeanstack.sifen.core.validation.ValidationError;
import org.javabeanstack.sifen.simulator.RecordedRequest;
import org.javabeanstack.sifen.simulator.Scenario;
import org.javabeanstack.sifen.simulator.SifenSimulator;
import org.javabeanstack.sifen.simulator.SimulatedService;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.CasosFase12;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * La batería de la Fase 12 por {@link ISifenClient} contra el simulador, sin red (plan JBSSIF, §12.2.6; CA12-04):
 * cada positivo de {@code esperados.tsv} que va en vivo se firma, se «graba», se relee y se envía; los recorridos
 * encadenados (anticipo en USD, factura final y su NC; tres NC parciales con el 2417); lote con un rechazado, lote
 * de 50, de 51, vacío y mezclado; cancelación fuera de plazo (4009 y 4010), 4065, 4003 y 4002; eventos del
 * receptor (0600 y 4156) y aprobado con observación (OP-20).
 *
 * <p>El simulador contesta lo que se le pide: estas pruebas fijan cómo <b>interpreta</b> la librería cada respuesta
 * y que la petición lleve lo que el caso dice (CDC asociado, anticipo). Lo que decide SIFEN se ve en §12.3. Deja
 * cada paso en {@code target/fase12/simulador.tsv}.</p>
 */
public class BateriaFacturacionSimulatorTest {
    private static final List<String> PASOS = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger numero = new AtomicInteger(900);
    private SifenSimulator sim;
    private ISifenClient c;

    @BeforeEach
    public void levantar() throws Exception {
        sim = SifenSimulator.start();
        c = SifenClient.create(sim.config());
    }

    @AfterEach
    public void bajar() {
        if (c != null) {
            c.close();
        }
        if (sim != null) {
            sim.close();
        }
    }

    @AfterAll
    public static void escribir() throws Exception {
        Path destino = Paths.get("target", "fase12", "simulador.tsv");
        Files.createDirectories(destino.getParent());
        List<String> l = new ArrayList<>();
        l.add("paso\toperacion\tresultado\tdetalle");
        List<String> p = new ArrayList<>(PASOS);
        Collections.sort(p);
        l.addAll(p);
        Files.write(destino, l, StandardCharsets.UTF_8);
    }

    private static void paso(String id, String operacion, Object resultado, String detalle) {
        PASOS.add(id + "\t" + operacion + "\t" + resultado + "\t" + detalle);
    }

    /** Positivos con número en vivo (vivo = V), salvo los grandes y el de plazo, que tienen su propio test. */
    static Stream<EsperadosFase12.Esperado> positivosEnVivo() {
        return EsperadosFase12.leer().stream()
                .filter(e -> !e.esEvento() && !e.esOperacion() && "V".equals(e.vivo()) && e.errores().isEmpty()
                        && !"grande".equals(e.clase()) && !"plazo".equals(e.clase()));
    }

    /**
     * Lo que §12.3 va a enviar pasa por la fachada con la validación activa: firmar, grabar, releer y enviar. Antes de
     * firmar se afirma lo que el envío en vivo exige del dato (I12-08: transportista = emisor).
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("positivosEnVivo")
    public void cadaPositivoDeLaBateriaSeEmiteEnDosPasos(EsperadosFase12.Esperado e) throws Exception {
        DocumentoElectronico de = e.documento();
        PreparacionVivoFase12.afirmarTransportistaDePrueba(de);   // I12-08: antes de firmar
        SendResult r = emitir(de, e.caso());
        paso("A-" + e.caso(), "sign + readSigned + send", r.getStatus(), r.getCdc());
    }

    @Test
    public void anticipoEnUsdFacturaFinalYSuNotaDeCredito() throws Exception {
        SendResult anticipo = emitir(f12("DE-36"), "DE-36");
        Assertions.assertEquals(CasosFase12.cdcDe("DE-36"), anticipo.getCdc());
        SendResult fin = emitir(f12("DE-37"), "DE-37");
        RecordedRequest pedido = ultimo(SimulatedService.RECEIVE_DE);
        Assertions.assertEquals(anticipo.getCdc(), pedido.text("dCDCAnticipo"), "la final lleva el CDC del anticipo (E719)");
        Assertions.assertEquals(anticipo.getCdc(), pedido.text("dCdCDERef"), "y lo asocia (H004)");
        SendResult nc = emitir(f12("DE-38"), "DE-38");
        Assertions.assertEquals(fin.getCdc(), ultimo(SimulatedService.RECEIVE_DE).text("dCdCDERef"), "la NC asocia la final");
        paso("B-01", "anticipo USD → final → NC", nc.getStatus(), anticipo.getCdc() + " → " + fin.getCdc() + " → " + nc.getCdc());
    }

    @Test
    public void tresNotasDeCreditoParcialesLaTerceraRechazadaPor2417() throws Exception {
        SendResult fe = emitir(CasosFase12.leyendas(caso("DE-09")), "DE-09");
        emitir(f12("DE-39"), "DE-39");
        emitir(f12("DE-39b"), "DE-39b");
        Assertions.assertEquals(fe.getCdc(), ultimo(SimulatedService.RECEIVE_DE).text("dCdCDERef"));
        sim.once(SimulatedService.RECEIVE_DE, Scenario.rejected("2417", "La suma de las NC supera el total del DE asociado"));
        SendResult tercera = c.send(c.sign(f12("DE-39c")));
        Assertions.assertEquals(ResultStatus.REJECTED, tercera.getStatus());
        Assertions.assertEquals("2417", tercera.getResponseCode());
        paso("B-02", "tres NC parciales sobre DE-09", tercera.getStatus(), "la tercera: " + tercera.getResponseCode());
    }

    @Test
    public void loteDeCincoConUnRechazado() throws Exception {
        List<SignedDocument> lote = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            lote.add(c.sign(numerado(CasosFase12.leyendas(caso("DE-05")))));
        }
        BatchSendResult r = c.sendBatch(lote);
        Assertions.assertEquals(BatchStatus.RECEIVED, r.getStatus());
        Map<String, String> codigos = new LinkedHashMap<>();
        for (int i = 0; i < 5; i++) {
            codigos.put(lote.get(i).getCdc(), i == 3 ? Scenario.DEFAULT_REJECTION_CODE : Scenario.APPROVED_CODE);
        }
        sim.on(SimulatedService.QUERY_BATCH, Scenario.batchCompletedWith(codigos));
        BatchQueryResult q = c.queryBatch(r.getBatchNumber());
        Assertions.assertEquals(BatchStatus.COMPLETED, q.getStatus());
        Assertions.assertEquals(5, q.getResults().size());
        for (int i = 0; i < 5; i++) {
            SendResult s = q.getResults().get(i);
            Assertions.assertEquals(lote.get(i).getCdc(), s.getCdc());
            Assertions.assertEquals(i == 3 ? ResultStatus.REJECTED : ResultStatus.APPROVED, s.getStatus(), s.toString());
        }
        Assertions.assertEquals("1107", q.getResults().get(3).getResponseCode());
        paso("L-01", "sendBatch(5) + queryBatch", q.getStatus(), "cuatro 0260 y un 1107, cada uno por su CDC");
    }

    @Test
    public void loteDe50() throws Exception {
        List<SignedDocument> lote = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            lote.add(c.sign(numerado(CasosFase12.leyendas(caso("DE-05")))));
        }
        BatchSendResult r = c.sendBatch(lote);
        Assertions.assertEquals(BatchStatus.RECEIVED, r.getStatus());
        Assertions.assertEquals(50, r.getCdcs().size());
        Assertions.assertEquals(1, sim.requests(SimulatedService.RECEIVE_BATCH).size(), "un solo envío");
        sim.on(SimulatedService.QUERY_BATCH, Scenario.batchCompleted(r.getCdcs()));
        BatchQueryResult q = c.queryBatch(r.getBatchNumber());
        Assertions.assertEquals(50, q.getResults().size());
        Assertions.assertTrue(q.getResults().stream().allMatch(s -> s.getStatus() == ResultStatus.APPROVED));
        paso("L-02", "sendBatch(50) + queryBatch", q.getStatus(), "cincuenta 0260");
    }

    @Test
    public void loteDe51VacioYMezcladoSeRechazanSinPedidos() throws Exception {
        List<SignedDocument> lote = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            lote.add(c.sign(numerado(CasosFase12.leyendas(caso("DE-05")))));
        }
        sim.reset();
        Assertions.assertEquals("JBS-ENT-01", rechazoLocal(lote));
        Assertions.assertEquals("JBS-ENT-11", rechazoLocal(List.of()));
        List<SignedDocument> mezclado = new ArrayList<>(lote.subList(0, 2));
        mezclado.add(c.sign(f12("DE-39")));
        Assertions.assertEquals("JBS-ENT-09", rechazoLocal(mezclado));
        paso("L-03", "sendBatch(51), sendBatch(vacío), sendBatch(FE+NC)", "rechazo local", "JBS-ENT-01, JBS-ENT-11, JBS-ENT-09; ningún pedido");
    }

    private String rechazoLocal(List<SignedDocument> lote) {
        SifenException e = Assertions.assertThrows(SifenException.class, () -> c.sendBatch(lote));
        Assertions.assertEquals(ErrorCategory.INPUT, e.getCategory(), e.toString());
        Assertions.assertFalse(e.isOutcomeUnknown());
        Assertions.assertTrue(sim.requests().isEmpty(), "rechazo local sin pedidos: " + sim.requests());
        return e.getCode();
    }

    @Test
    public void eventosDelEmisorRechazados() throws Exception {
        SendResult fe = emitir(numerado(CasosFase12.leyendas(caso("DE-05"))), "DE-05");
        String[][] casos = {
            {"OP-01", "4009", "Plazo de cancelación de la FE vencido (48 h)"},
            {"OP-02", "4010", "Plazo de cancelación de la NC vencido (168 h)"},
            {"OP-04", "4003", "El DE ya fue cancelado"},
            {"OP-05", "4002", "CDC inexistente"}};
        for (String[] k : casos) {
            sim.once(SimulatedService.EVENT, Scenario.eventRejected(k[1], k[2]));
            EventResult r = c.cancel(fe.getCdc(), "Cancelacion de prueba del documento");
            Assertions.assertEquals(ResultStatus.REJECTED, r.getStatus(), k[0] + ": " + r);
            Assertions.assertEquals(k[1], r.getResponseCode(), k[0]);
            paso("E-" + k[0], "cancel", r.getStatus(), k[1]);
        }
        sim.once(SimulatedService.EVENT, Scenario.eventRejected("4065", "El número ya tiene un DE aprobado"));
        NumberRange rango = NumberRange.builder().timbrado(String.valueOf(CasosDePrueba.TIMBRADO)).establishment("1").issuePoint("1")
                .documentType(TTiDE.FACTURA_ELECTRONICA).from(fe.getCdc().substring(17, 24)).to(fe.getCdc().substring(17, 24))
                .reason("Inutilizacion de prueba de numeracion").build();
        EventResult inu = c.invalidateNumbers(rango);
        Assertions.assertEquals(ResultStatus.REJECTED, inu.getStatus());
        Assertions.assertEquals("4065", inu.getResponseCode());
        paso("E-OP-03", "invalidateNumbers de un número aprobado", inu.getStatus(), "4065");
    }

    @Test
    public void inutilizacionDeMasDe1000OInvertidaSeRechazaLocal() {
        for (String id : List.of("OP-05b", "OP-05c")) {
            SifenException e = Assertions.assertThrows(SifenException.class, () -> c.sendEvents(CasosFase12.eventosNegativos().get(id).get()));
            String esperado = "OP-05b".equals(id) ? "GEI006" : "GEI006a";
            Assertions.assertTrue(e.getErrors().stream().map(ValidationError::getCode).anyMatch(esperado::equals), id + ": " + e);
            Assertions.assertTrue(sim.requests().isEmpty(), id + ": sin pedidos");
            paso("E-" + id, "sendEvents", e.getCode(), esperado);
        }
    }

    @Test
    public void eventosDelReceptor() throws Exception {
        String cdc = emitir(numerado(CasosFase12.leyendas(caso("DE-05"))), "DE-05").getCdc();
        Assertions.assertEquals(ResultStatus.APPROVED, receptor("EV-07", f12Evento("EV-07"), cdc).getStatus());
        Assertions.assertEquals(ResultStatus.APPROVED, receptor("EV-03", catalogoEvento("EV-03"), cdc).getStatus());
        Assertions.assertEquals(ResultStatus.APPROVED, receptor("EV-09", f12Evento("EV-09"), cdc).getStatus());
        Assertions.assertEquals(ResultStatus.APPROVED, receptor("EV-04", catalogoEvento("EV-04"), cdc).getStatus());
        Assertions.assertEquals(ResultStatus.APPROVED, receptor("EV-04+conf", f12Evento("EV-09"), cdc).getStatus());
        Assertions.assertEquals(ResultStatus.APPROVED, receptor("EV-08", f12Evento("EV-08"), cdc).getStatus());
        sim.once(SimulatedService.EVENT, Scenario.eventRejected("4156", "El DE tiene un evento de desconocimiento"));
        EventResult r = receptor("EV-08+conf", f12Evento("EV-09"), cdc);
        Assertions.assertEquals(ResultStatus.REJECTED, r.getStatus());
        Assertions.assertEquals("4156", r.getResponseCode());
        EventResult can = c.cancel(cdc, "Cancelacion de prueba del documento");
        Assertions.assertEquals(ResultStatus.APPROVED, can.getStatus(), "OP-06: la NT 025 quitó 4004");
        paso("R-OP-06", "cancel de una FE conformada", can.getStatus(), "0600");
    }

    private EventResult receptor(String id, EventosDE ev, String cdc) throws Exception {
        TrGesEve g = ev.getrGesEveList().get(0);
        g.setdFecFirma(null);   // la pone sendEvents (I8-34)
        if (g.getgGroupTiEvt().getrGeVeConf() != null) {
            g.getgGroupTiEvt().getrGeVeConf().setId(cdc);
        } else if (g.getgGroupTiEvt().getrGeVeDisconf() != null) {
            g.getgGroupTiEvt().getrGeVeDisconf().setId(cdc);
        } else if (g.getgGroupTiEvt().getrGeVeDescon() != null) {
            g.getgGroupTiEvt().getrGeVeDescon().setId(cdc);
        } else if (g.getgGroupTiEvt().getrGeVeNotRec() != null) {
            g.getgGroupTiEvt().getrGeVeNotRec().setId(cdc);
        }
        EventResult r = c.sendEvents(ev);
        Assertions.assertTrue(ultimo(SimulatedService.EVENT).getBody().contains(cdc), id + ": el evento lleva el CDC");
        paso("R-" + id, "sendEvents", r.getStatus(), r.getResponseCode());
        return r;
    }

    @Test
    public void aprobadoConObservacionPorFirmaDeMasDe72Horas() throws Exception {
        DocumentoElectronico op20 = CasosFase12.todos().get("OP-20").get();
        Assertions.assertTrue(c.validateDE(op20).contains("A004b"), "aviso local A004b (D56)");
        SignedDocument firmado = c.sign(op20);
        Assertions.assertEquals(CasosDePrueba.FECHA_FIRMA.minusHours(73), firmado.getSignatureDate(), "sign conserva la fecha atrasada");
        sim.once(SimulatedService.RECEIVE_DE, Scenario.approvedWithObservation());
        SendResult r = c.send(c.readSigned(firmado.getXml()));
        Assertions.assertEquals(ResultStatus.APPROVED_WITH_OBSERVATION, r.getStatus(), r.toString());
        Assertions.assertEquals(Scenario.DEFAULT_OBSERVATION_CODE, r.getResponseCode());
        Assertions.assertNotNull(r.getAuthorizationProtocol(), "aprobado: tiene protocolo");
        paso("P-OP-20", "send de un DE firmado hace 73 h", r.getStatus(), r.getResponseCode());
    }

    // ------------------------------------------------------------------ OP-30 a OP-32 (pedido del usuario, 2026-11-25)

    /** Cada documento de OP-30..OP-32 pasa la validación previa sin errores ni avisos (local: «acepta»). */
    @Test
    public void losDocumentosDeOp30aOp32PasanLaValidacionPrevia() {
        Map<String, java.util.function.Supplier<DocumentoElectronico>> docs = CasosFase12.cancelacionConNotas();
        Assertions.assertEquals(List.of("OP-30-FE", "OP-30-NC", "OP-31-FE", "OP-31-NC", "OP-32-FE", "OP-32-NC"), List.copyOf(docs.keySet()));
        for (Map.Entry<String, java.util.function.Supplier<DocumentoElectronico>> e : docs.entrySet()) {
            org.javabeanstack.sifen.core.validation.ValidationResult v = EsperadosFase12.validador().validate(e.getValue().get(),
                    org.javabeanstack.sifen.test.casos.FixturesSifen.contexto(org.javabeanstack.sifen.test.casos.FixturesSifen.configuracion()));
            Assertions.assertTrue(v.getAll().isEmpty(), e.getKey() + ": " + v.getAll());
        }
        // FE y NC propias: ningún CDC de OP-30..OP-32 es el de otro caso de la batería
        java.util.Set<String> otros = new java.util.HashSet<>();
        for (String id : CasosFase12.todos().keySet()) {
            otros.add(CasosFase12.cdcDe(id));
        }
        for (CasosDePrueba.Caso k : CasosDePrueba.documentos()) {
            if (k.tieneConstructor()) {
                otros.add(cdc(k.construir()));
            }
        }
        for (Map.Entry<String, java.util.function.Supplier<DocumentoElectronico>> e : docs.entrySet()) {
            Assertions.assertFalse(otros.contains(cdc(e.getValue().get())), e.getKey() + ": el CDC es de otro caso");
        }
        paso("S9-OP-30..32", "validación previa", "acepta", "seis documentos propios, sin errores ni avisos");
    }

    /** OP-30 y OP-31: FE + NC (parcial o total) aprobadas y después el cancel de la FE (el MT no lo frena con un código). */
    @Test
    public void cancelarUnaFacturaConNotaDeCreditoParcialOTotal() throws Exception {
        for (String op : List.of("OP-30", "OP-31")) {
            SendResult fe = emitir(cancelacion(op + "-FE"), op + "-FE");
            DocumentoElectronico notaDe = cancelacion(op + "-NC");
            SendResult nc = emitir(notaDe, op + "-NC");
            Assertions.assertEquals(fe.getCdc(), ultimo(SimulatedService.RECEIVE_DE).text("dCdCDERef"), op + ": la NC asocia su FE");
            java.math.BigDecimal totalFe = new java.math.BigDecimal(ultimoTotal(fe));
            java.math.BigDecimal totalNc = new java.math.BigDecimal(ultimo(SimulatedService.RECEIVE_DE).text("dTotGralOpe"));
            if ("OP-30".equals(op)) {
                Assertions.assertTrue(totalNc.compareTo(totalFe) < 0, "OP-30: NC parcial " + totalNc + " < " + totalFe);
            } else {
                Assertions.assertEquals(0, totalNc.compareTo(totalFe), "OP-31: NC por el total");
            }
            EventResult can = c.cancel(fe.getCdc(), "Cancelacion de prueba de una FE con NC aprobada");
            Assertions.assertEquals(ResultStatus.APPROVED, can.getStatus(), op + ": " + can);
            Assertions.assertTrue(ultimo(SimulatedService.EVENT).getBody().contains(fe.getCdc()), op + ": el cancel es de la FE");
            Assertions.assertFalse(ultimo(SimulatedService.EVENT).getBody().contains(nc.getCdc()), op + ": la NC no se cancela");
            paso("S9-" + op, "FE → NC " + ("OP-30".equals(op) ? "parcial" : "total") + " → cancel de la FE", can.getStatus(),
                    "NC " + totalNc + " de " + totalFe + "; cancel 0600 (esperado 0600?)");
        }
    }

    /** OP-32: FE aprobada y cancelada; la NC sobre ella la rechaza SIFEN con 2404 (H004b), que la librería informa. */
    @Test
    public void notaDeCreditoSobreUnaFacturaCanceladaRechazadaPor2404() throws Exception {
        SendResult fe = emitir(cancelacion("OP-32-FE"), "OP-32-FE");
        EventResult can = c.cancel(fe.getCdc(), "Cancelacion de prueba antes de la NC");
        Assertions.assertEquals(ResultStatus.APPROVED, can.getStatus());
        sim.once(SimulatedService.RECEIVE_DE, Scenario.rejected("2404", "El DTE referenciado se encuentra cancelado en SIFEN"));
        SignedDocument nc = c.sign(cancelacion("OP-32-NC"));
        Assertions.assertEquals(TTiDE.NOTA_DE_CREDITO_ELECTRONICA, nc.getDocumentType(), "tipo 5 para la inutilización de 12.3.11");
        SendResult r = c.send(c.readSigned(nc.getXml()));
        Assertions.assertEquals(ResultStatus.REJECTED, r.getStatus(), r.toString());
        Assertions.assertEquals("2404", r.getResponseCode());
        Assertions.assertEquals(fe.getCdc(), ultimo(SimulatedService.RECEIVE_DE).text("dCdCDERef"), "la NC asocia la FE cancelada");
        paso("S9-OP-32", "FE → cancel → NC", r.getStatus(), "2404; la NC (tipo " + nc.getDocumentType().getVal() + ") va a 12.3.11");
    }

    private static DocumentoElectronico cancelacion(String id) {
        return CasosFase12.cancelacionConNotas().get(id).get();
    }

    private String ultimoTotal(SendResult r) {
        for (RecordedRequest p : sim.requests(SimulatedService.RECEIVE_DE)) {
            if (p.getBody().contains(r.getCdc())) {
                return p.text("dTotGralOpe");
            }
        }
        throw new IllegalStateException("sin pedido de " + r.getCdc());
    }

    private static String cdc(DocumentoElectronico de) {
        try {
            return de.obtenerCDC();
        } catch (SifenException e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------ apoyo

    private SendResult emitir(DocumentoElectronico de, String nombre) throws Exception {
        SignedDocument doc = c.sign(de);
        SendResult r = c.send(c.readSigned(doc.getXml()));
        Assertions.assertEquals(ResultStatus.APPROVED, r.getStatus(), nombre + ": " + r.getMessage());
        Assertions.assertFalse(r.isDigestMismatch(), nombre);
        Assertions.assertEquals(doc.getCdc(), r.getCdc());
        Assertions.assertEquals(doc.getDigestValue(), ultimo(SimulatedService.RECEIVE_DE).text("DigestValue").trim(), nombre + ": mismo XML");
        return r;
    }

    private RecordedRequest ultimo(SimulatedService servicio) {
        List<RecordedRequest> p = sim.requests(servicio);
        return p.get(p.size() - 1);
    }

    private DocumentoElectronico numerado(DocumentoElectronico de) {
        de.getgTimb().setdNumDoc(String.format("%07d", numero.incrementAndGet()));
        return de;
    }

    private static DocumentoElectronico f12(String id) {
        return CasosFase12.todos().get(id).get();
    }

    private static DocumentoElectronico caso(String id) {
        return CasosDePrueba.documentos().stream().filter(k -> k.getId().equals(id)).findFirst().orElseThrow().construir();
    }

    private static EventosDE f12Evento(String id) {
        return CasosFase12.eventos().stream().filter(k -> k.id().equals(id)).findFirst().orElseThrow().construir();
    }

    private static EventosDE catalogoEvento(String id) {
        return CasosDePrueba.eventos().stream().filter(k -> k.getId().equals(id)).findFirst().orElseThrow().construir();
    }
}

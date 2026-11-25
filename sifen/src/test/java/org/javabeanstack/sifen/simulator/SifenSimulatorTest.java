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
package org.javabeanstack.sifen.simulator;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.javabeanstack.sifen.ISifenClient;
import org.javabeanstack.sifen.Sifen;
import org.javabeanstack.sifen.SifenClient;
import org.javabeanstack.sifen.api.BatchQueryResult;
import org.javabeanstack.sifen.api.BatchSendResult;
import org.javabeanstack.sifen.api.BatchStatus;
import org.javabeanstack.sifen.api.EventResult;
import org.javabeanstack.sifen.api.ResultStatus;
import org.javabeanstack.sifen.api.RucQueryResult;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionDE;
import org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionEvento;
import org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionLoteDE;
import org.javabeanstack.sifen.core.exceptions.ErrorCategory;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Escenarios del simulador de SIFEN (plan JBSSIF, §8.6): cada respuesta grabada y cada falla de
 * transporte, vistas desde la librería. Las consultas van por {@link SifenClient}; la recepción, el lote
 * y los eventos todavía van por la fachada heredada {@link Sifen} porque {@code send}, {@code sendBatch}
 * y {@code sendEvents} de {@code SifenClient} terminan con {@code JBS-INT-04} hasta §10.3 (F). Cada
 * test levanta y baja su propio simulador (prueba también el ciclo de vida). Sin red.
 */
@SuppressWarnings("deprecation")
public class SifenSimulatorTest {

    /** Configuración para la fachada heredada: sin validación previa (como los golden, D20). */
    private static SifenConfig heredada(SifenSimulator sim) {
        SifenConfig c = sim.config();
        c.setPreValidationEnabled(false);
        return c;
    }

    private static DocumentoElectronico factura() {
        return CasosDePrueba.documentos().get(4).construir();
    }

    /**
     * Lista mutable: {@code Sifen.recepcionLoteDE} hace {@code deList.contains(null)}, que con una lista
     * inmutable ({@code List.of}) lanza {@code NullPointerException} y sale como {@code JBS-INT-00} (E10-01).
     */
    private static List<DocumentoElectronico> lote(DocumentoElectronico... des) {
        return new java.util.ArrayList<>(List.of(des));
    }

    private static SifenException falla(Executable llamada) {
        return Assertions.assertThrows(SifenException.class, llamada);
    }

    // ------------------------------------------------------------------ configuración lista

    @Test
    public void laConfiguracionTraeElCertificadoEnLineaYValida() throws Exception {
        try (SifenSimulator sim = SifenSimulator.start()) {
            SifenConfig cfg = sim.config();
            Assertions.assertEquals(SifenConfig.Environment.DEV, cfg.getEnvironment());
            Assertions.assertEquals(sim.getBaseUrl(), cfg.getBaseUrl());
            Assertions.assertTrue(sim.getBaseUrl().startsWith("https://127.0.0.1:"));
            String cert = cfg.getClientCertificate();
            Assertions.assertTrue(cert.length() > 1000, "PKCS#12 en Base64, no una ruta");
            Assertions.assertFalse(Files.exists(Paths.get(cert.substring(0, 50))), "no apunta a un archivo");
            ISifenClient c = SifenClient.create(cfg);
            Assertions.assertTrue(c.validateConfig().isValid(), c.validateConfig().getErrors().toString());
            Assertions.assertEquals(sim.getBaseUrl() + "/empresa-a", sim.config("empresa-a").getBaseUrl());
            Assertions.assertThrows(IllegalArgumentException.class, () -> sim.config("a/b"));
        }
    }

    @Test
    public void alCerrarRestauraLaConfianzaDeLaJvm() throws Exception {
        String antes = System.getProperty("javax.net.ssl.trustStore");
        String durante;
        try (SifenSimulator sim = SifenSimulator.start()) {
            durante = System.getProperty("javax.net.ssl.trustStore");
            Assertions.assertTrue(Files.exists(Paths.get(durante)), "almacén de confianza temporal");
            try (SifenSimulator otro = SifenSimulator.start()) {
                Assertions.assertNotEquals(sim.getPort(), otro.getPort());
            }
            Assertions.assertEquals(durante, System.getProperty("javax.net.ssl.trustStore"), "sigue abierto uno");
        }
        Assertions.assertEquals(antes, System.getProperty("javax.net.ssl.trustStore"));
        Assertions.assertFalse(Files.exists(Paths.get(durante)), "el temporal se borra");
    }

    // ------------------------------------------------------------------ recepción (fachada heredada hasta §10.3)

    @Test
    public void aprobadoDevuelveElCdcYElDigestDelDeRecibido() throws Exception {
        try (SifenSimulator sim = SifenSimulator.start()) {
            RespuestaRecepcionDE r = Sifen.recepcionDE(factura(), heredada(sim));
            RecordedRequest pedido = sim.requests(SimulatedService.RECEIVE_DE).get(0);
            Assertions.assertEquals("Aprobado", r.getxProtDE().getdEstRes());
            Assertions.assertEquals("0260", r.getxProtDE().getgResProc().get(0).getdCodRes());
            Assertions.assertEquals(Scenario.AUTHORIZATION_PROTOCOL, r.getxProtDE().getdProtAut());
            Assertions.assertEquals(pedido.getCdc(), r.getxProtDE().getId());
            Assertions.assertEquals(pedido.text("DigestValue").trim(), r.getxProtDE().getdDigVal());
            Assertions.assertTrue(pedido.getClientSubject().contains("RUC80123456-5"), pedido.getClientSubject());
        }
    }

    @Test
    public void rechazadoPorOmisionEsElTimbrado1107() throws Exception {
        try (SifenSimulator sim = SifenSimulator.start()) {
            sim.on(SimulatedService.RECEIVE_DE, Scenario.rejected());
            RespuestaRecepcionDE r = Sifen.recepcionDE(factura(), heredada(sim));
            Assertions.assertEquals("Rechazado", r.getxProtDE().getdEstRes());
            Assertions.assertEquals(Scenario.DEFAULT_REJECTION_CODE, r.getxProtDE().getgResProc().get(0).getdCodRes());
            Assertions.assertEquals(Scenario.DEFAULT_REJECTION_MESSAGE, r.getxProtDE().getgResProc().get(0).getdMsgRes());
            Assertions.assertEquals(sim.requests().get(0).getCdc(), r.getxProtDE().getId());
        }
    }

    @Test
    public void rechazadoConUnCodigoAEleccion() throws Exception {
        try (SifenSimulator sim = SifenSimulator.start()) {
            sim.on(SimulatedService.RECEIVE_DE, Scenario.rejected("1306", "RUC del receptor inexistente & <inactivo>"));
            RespuestaRecepcionDE r = Sifen.recepcionDE(factura(), heredada(sim));
            Assertions.assertEquals("1306", r.getxProtDE().getgResProc().get(0).getdCodRes());
            Assertions.assertEquals("RUC del receptor inexistente & <inactivo>", r.getxProtDE().getgResProc().get(0).getdMsgRes());
        }
    }

    @Test
    public void loteRecibidoYNoEncolado() throws Exception {
        try (SifenSimulator sim = SifenSimulator.start()) {
            RespuestaRecepcionLoteDE r = Sifen.recepcionLoteDE(lote(factura()), heredada(sim));
            Assertions.assertEquals("0300", r.getdCodRes());
            Assertions.assertEquals(Scenario.BATCH_NUMBER, r.getdProtConsLote());
            Assertions.assertEquals(SimulatedService.RECEIVE_BATCH, sim.requests().get(0).getService());
            sim.on(SimulatedService.RECEIVE_BATCH, Scenario.batchNotQueued());
            RespuestaRecepcionLoteDE n = Sifen.recepcionLoteDE(lote(factura()), heredada(sim));
            Assertions.assertEquals("0301", n.getdCodRes());
        }
    }

    @Test
    public void eventoRegistrado() throws Exception {
        try (SifenSimulator sim = SifenSimulator.start()) {
            RespuestaRecepcionEvento r = Sifen.recepcionEvento(CasosDePrueba.eventos().get(0).construir(), heredada(sim));
            Assertions.assertEquals("0600", r.getgResProcEVe().get(0).getgResProc().get(0).getdCodRes());
            Assertions.assertEquals(SimulatedService.EVENT, sim.requests().get(0).getService());
        }
    }

    // ------------------------------------------------------------------ fallas de transporte

    @Test
    public void fallasDeTransporteEnUnaConsulta() throws Exception {
        try (SifenSimulator sim = SifenSimulator.start()) {
            ISifenClient c = SifenClient.create(sim.config());
            Object[][] filas = {
                {Scenario.connectionCut(), "JBS-CONEX-07"},
                {Scenario.badGateway(), "JBS-CONEX-09"},
                {Scenario.gatewayTimeout(), "JBS-CONEX-09"},
                {Scenario.notSoap(), "JBS-CONEX-10"},
                {Scenario.soapFault(), "JBS-RESP-01"},
            };
            for (Object[] f : filas) {
                sim.once(SimulatedService.QUERY_RUC, (Scenario) f[0]);
                SifenException e = falla(() -> c.queryRuc("80123456"));
                Assertions.assertEquals(f[1], e.getCode(), f[0] + ": " + e.getMessage());
                Assertions.assertFalse(e.isOutcomeUnknown(), f[0] + ": una consulta no deja un envío desconocido");
            }
            Assertions.assertTrue(c.queryRuc("80123456").isFound(), "la cola se consumió: vuelve al escenario fijado");
            Assertions.assertEquals(filas.length + 1, sim.requests().size());
        }
    }

    @Test
    public void corteEnUnEnvioDejaElResultadoDesconocido() throws Exception {
        try (SifenSimulator sim = SifenSimulator.start()) {
            sim.on(SimulatedService.RECEIVE_DE, Scenario.connectionCut());
            SifenException e = falla(() -> Sifen.recepcionDE(factura(), heredada(sim)));
            Assertions.assertEquals("JBS-CONEX-07", e.getCode());
            Assertions.assertEquals(ErrorCategory.CONNECTION, e.getCategory());
            Assertions.assertTrue(e.isOutcomeUnknown(), "el DE pudo haber llegado");
            // E5-01: el JDK reenviaba solo el POST cortado (sun.net.http.retryPost); D41: una recepción, un pedido
            Assertions.assertEquals(1, sim.requests(SimulatedService.RECEIVE_DE).size(),
                    "una sola conexión por recepción: nadie reenvió el POST cortado");
        }
    }

    /**
     * Tiempo de lectura agotado por la fachada heredada, con el tiempo configurable de §5.2
     * ({@code setHttpReadTimeout}; I5-01). D41: la recepción no se reintenta, un solo pedido.
     */
    @Test
    public void sifenTardaMasQueElTiempoDeLectura() throws Exception {
        try (SifenSimulator sim = SifenSimulator.start()) {
            sim.on(SimulatedService.RECEIVE_DE, Scenario.delayed(Duration.ofMillis(1500), Scenario.approved()));
            SifenConfig cfg = heredada(sim);
            cfg.setHttpReadTimeout(300);
            SifenException e = falla(() -> Sifen.recepcionDE(factura(), cfg));
            Assertions.assertEquals("JBS-CONEX-05", e.getCode());
            Assertions.assertTrue(e.isOutcomeUnknown());
            Assertions.assertEquals(1, sim.requests(SimulatedService.RECEIVE_DE).size(), "D41: no se reintenta la recepción");
        }
    }

    /** El mismo tiempo agotado por {@code SifenClient}: la copia de la configuración conserva el tiempo de §5.2. */
    @Test
    public void sifenTardaPorElCliente() throws Exception {
        try (SifenSimulator sim = SifenSimulator.start()) {
            sim.on(SimulatedService.QUERY_RUC, Scenario.delayed(Duration.ofMillis(1500), Scenario.rucFound()));
            SifenConfig cfg = sim.config();
            cfg.setHttpReadTimeout(300);
            ISifenClient c = SifenClient.create(cfg);
            SifenException e = falla(() -> c.queryRuc("80123456"));
            Assertions.assertEquals("JBS-CONEX-05", e.getCode());
        }
    }

    @Test
    public void demoraMenorQueElTiempoDeLecturaContesta() throws Exception {
        try (SifenSimulator sim = SifenSimulator.start()) {
            sim.on(SimulatedService.QUERY_RUC, Scenario.delayed(Duration.ofMillis(200), Scenario.rucFound()));
            RucQueryResult r = SifenClient.create(sim.config()).queryRuc("80123456");
            Assertions.assertTrue(r.isFound());
            Assertions.assertTrue(r.getDurationMs() >= 200, "duración " + r.getDurationMs());
        }
    }

    // ------------------------------------------------------------------ atajos para MKSIF: eventos rechazados y lote por código

    /** Un DE del caso 4 con las leyendas de DEV (Q14) y el número dado, para firmar con la validación previa activa. */
    private static DocumentoElectronico facturaDev(int numero) {
        DocumentoElectronico de = factura();
        de.getgDatGralOpe().getgEmis().setdNomEmi("DE generado en ambiente de prueba - sin valor comercial ni fiscal");
        de.getgDtipDE().getgCamItemList().get(0).setdDesProSer(
                "DOCUMENTO ELECTRÓNICO SIN VALOR COMERCIAL NI FISCAL - GENERADO EN AMBIENTE DE PRUEBA");
        de.getgTimb().setdNumDoc(String.format("%07d", numero));
        return de;
    }

    /** {@code eventRejected}: el código y el mensaje llegan tal cual; uno de datos es rechazo, uno de servidor no (I8-08). */
    @Test
    public void eventoRechazadoConElCodigoQueSePida() throws Exception {
        try (SifenSimulator sim = SifenSimulator.start()) {
            ISifenClient c = SifenClient.create(sim.config());
            Object[][] filas = {
                {"4066", "Plazo de cancelación vencido & <otro>", ResultStatus.REJECTED},
                {"4009", "Rechazo de prueba", ResultStatus.REJECTED},
                {"0100", "Error Inesperado", ResultStatus.UNRECOGNIZED},
                {"0161", "Servidor de procesamiento momentáneamente sin respuesta", ResultStatus.UNRECOGNIZED},
                {"0162", "Servidor de procesamiento paralizado", ResultStatus.UNRECOGNIZED},
            };
            for (Object[] f : filas) {
                sim.once(SimulatedService.EVENT, Scenario.eventRejected((String) f[0], (String) f[1]));
                EventResult r = c.cancel(CasosDePrueba.CDC_FACTURA_SIMPLE, "Cancelacion de prueba del documento");
                Assertions.assertEquals(f[2], r.getStatus(), f[0] + ": " + r);
                Assertions.assertEquals(f[0], r.getResponseCode());
                Assertions.assertEquals(f[1], r.getMessage(), "el mensaje se escapa al armar el XML y vuelve igual");
                Assertions.assertEquals("", r.getAuthorizationProtocol(), "un rechazo no trae dProtAut");
            }
            Assertions.assertEquals(ResultStatus.APPROVED,
                    c.cancel(CasosDePrueba.CDC_FACTURA_SIMPLE, "Cancelacion de prueba del documento").getStatus(),
                    "consumidos los de una sola vez, vuelve el evento registrado");
            Assertions.assertEquals(filas.length + 1, sim.requests(SimulatedService.EVENT).size(), "ningún evento se reintenta");
        }
    }

    /** {@code batchCompletedWith}: un resultado por CDC, en el orden del mapa, aprobado con 0260 y rechazado con otro código. */
    @Test
    public void loteConcluidoConUnCodigoPorCdc() throws Exception {
        try (SifenSimulator sim = SifenSimulator.start()) {
            ISifenClient c = SifenClient.create(sim.config());
            SignedDocument a = c.sign(facturaDev(901));
            SignedDocument b = c.sign(facturaDev(902));
            SignedDocument d = c.sign(facturaDev(903));
            BatchSendResult lote = c.sendBatch(List.of(a, b, d));
            Map<String, String> codigos = new LinkedHashMap<>();
            codigos.put(a.getCdc(), Scenario.APPROVED_CODE);
            codigos.put(b.getCdc(), "1306");
            codigos.put(d.getCdc(), "0162");
            sim.on(SimulatedService.QUERY_BATCH, Scenario.batchCompletedWith(codigos));
            BatchQueryResult r = c.queryBatch(lote.getBatchNumber());
            Assertions.assertEquals(BatchStatus.COMPLETED, r.getStatus());
            Assertions.assertEquals(List.of(a.getCdc(), b.getCdc(), d.getCdc()), r.getResults().stream().map(x -> x.getCdc()).toList());
            Assertions.assertEquals(ResultStatus.APPROVED, r.getResults().get(0).getStatus());
            Assertions.assertFalse(r.getResults().get(0).getAuthorizationProtocol().isEmpty(), "el aprobado trae protocolo");
            Assertions.assertEquals(ResultStatus.REJECTED, r.getResults().get(1).getStatus());
            Assertions.assertEquals("1306", r.getResults().get(1).getResponseCode());
            Assertions.assertEquals("", r.getResults().get(1).getAuthorizationProtocol());
            Assertions.assertEquals(ResultStatus.UNRECOGNIZED, r.getResults().get(2).getStatus(), "0162 es de servidor (I8-10)");
            Assertions.assertEquals("0162", r.getResults().get(2).getResponseCode());
            // batchCompleted(cdcs) es el mismo atajo con todos en 0260
            sim.on(SimulatedService.QUERY_BATCH, Scenario.batchCompleted(List.of(a.getCdc(), b.getCdc())));
            Assertions.assertTrue(c.queryBatch(lote.getBatchNumber()).getResults().stream()
                    .allMatch(x -> x.getStatus() == ResultStatus.APPROVED));
        }
    }

    // ------------------------------------------------------------------ empresas y rutas

    @Test
    public void escenarioPorEmpresa() throws Exception {
        try (SifenSimulator sim = SifenSimulator.start()) {
            sim.on("empresa-b", SimulatedService.QUERY_RUC, Scenario.rucNotFound());
            ISifenClient a = SifenClient.create(sim.config("empresa-a"));
            ISifenClient b = SifenClient.create(sim.config("empresa-b"));
            Assertions.assertTrue(a.queryRuc("80123456").isFound());
            Assertions.assertFalse(b.queryRuc("80123456").isFound());
            Assertions.assertEquals(1, sim.requests("empresa-a").size());
            Assertions.assertEquals(1, sim.requests("empresa-b").size());
            sim.reset();
            Assertions.assertTrue(sim.requests().isEmpty());
            Assertions.assertTrue(b.queryRuc("80123456").isFound(), "reset vuelve a los escenarios por omisión");
        }
    }

    @Test
    public void rutaDesconocidaEs404YQuedaRegistrada() throws Exception {
        try (SifenSimulator sim = SifenSimulator.start()) {
            SifenConfig cfg = sim.config();
            cfg.setQueryRucPath("/de/ws/consultas/otra.wsdl");
            SifenException e = falla(() -> SifenClient.create(cfg).queryRuc("80123456"));
            Assertions.assertEquals("JBS-CONEX-08", e.getCode());
            Assertions.assertNull(sim.requests().get(0).getService());
        }
    }

    @Test
    public void elCertificadoDeClienteQuedaEnCadaPedido() throws Exception {
        try (SifenSimulator sim = SifenSimulator.start()) {
            SifenClient.create(sim.config()).queryRuc("80123456");
            RecordedRequest p = sim.requests().get(0);
            Assertions.assertNotNull(p.getClientCertificate(), "TLS mutuo: el cliente presentó su certificado");
            Assertions.assertTrue(p.getClientSubject().contains("RUC80123456-5"), p.getClientSubject());
            Assertions.assertEquals(1L, p.getRequestId());
            Assertions.assertEquals("", p.getTenant());
        }
    }
}

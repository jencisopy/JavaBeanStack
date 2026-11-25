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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.javabeanstack.sifen.api.NumberRange;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.exceptions.ErrorCategory;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.types.TTiDE;
import org.javabeanstack.sifen.simulator.Scenario;
import org.javabeanstack.sifen.simulator.SifenSimulator;
import org.javabeanstack.sifen.simulator.SimulatedService;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Matriz de fallas de transporte y de respuesta <b>por {@link ISifenClient}</b> contra el simulador (plan
 * JBSSIF, §8.7, CA8-12; la de la fachada heredada es {@code FachadaFallasTest}, CA3-09). Cada operación del
 * contrato que habla con SIFEN, por cada escenario: corte sin responder, 502 y 504 de un proxy, HTTP 200 que
 * no es SOAP, tiempo de lectura agotado, SOAP Fault, HTTP 200 sin cuerpo y la respuesta de otra operación.
 * Sin red.
 *
 * <p>En cada celda: sale una {@code SifenException} (nada más), con el código y la categoría esperados, el
 * mensaje para el usuario con {@code (ref. <id>)} al final, el id de correlación de 8 hexadecimales, ningún
 * getter en {@code null} salvo los que el contrato admite, sin {@code NullPointerException} en las causas;
 * {@code isOutcomeUnknown()} verdadero en las recepciones (la falla llega después de transmitir, D43) y en
 * {@code resolve} (también cuando falla su consulta, que además no reenvía), falso en las consultas; y la cantidad de pedidos: <b>una recepción nunca se reintenta</b>
 * (D41, E5-01), aunque la configuración pida reintentos (acá {@code sifen.reintentos.max} = 2); una consulta
 * reintenta solo las fallas rápidas de transporte ({@code JBS-CONEX-03/06/07/09}, {@code Retries}). La tabla
 * resultante queda en {@code target/fallas/ca8_12_matriz_sifenclient.tsv}.</p>
 */
public class SifenClientFaultMatrixTest {
    private static final Pattern ID = Pattern.compile("[0-9a-f]{8}");
    private static final int REINTENTOS = 2;
    /** Códigos que una consulta reintenta ({@code Retries}). */
    private static final Set<String> REINTENTABLES = Set.of("JBS-CONEX-03", "JBS-CONEX-06", "JBS-CONEX-07", "JBS-CONEX-09");
    /** Getters de la excepción que pueden ser {@code null}. */
    private static final Set<String> PUEDEN_SER_NULL = Set.of("getSifenResponseCode", "getReference", "getLegacyCode",
            "getTechnicalDetail", "getCause", "getLocalizedMessage");
    private static final AtomicInteger NUMERO = new AtomicInteger(700);
    private static final List<String> TABLA = new ArrayList<>();
    private static SifenSimulator sim;

    @BeforeAll
    public static void levantar() throws Exception {
        sim = SifenSimulator.start();
    }

    @AfterAll
    public static void bajar() throws Exception {
        if (sim != null) {
            sim.close();
        }
        Path destino = Paths.get("target", "fallas", "ca8_12_matriz_sifenclient.tsv");
        Files.createDirectories(destino.getParent());
        List<String> l = new ArrayList<>();
        l.add("operacion\tfalla\tcodigo\tcategoria\tresultado_desconocido\tpedidos\tid");
        l.addAll(TABLA);
        Files.write(destino, l, StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ operaciones y escenarios

    /** Una operación del contrato que habla con SIFEN: dónde se inyecta la falla y cómo se la llama. */
    /**
     * @param recepcion   el pedido que falla es una recepción: no se reintenta nunca (D41).
     * @param desconocido lo esperado de {@code isOutcomeUnknown()}: recepciones y todo {@code resolve} (D43).
     */
    private record Op(String nombre, SimulatedService servicio, boolean recepcion, boolean desconocido, Llamada llamada) {
    }

    @FunctionalInterface
    private interface Llamada {
        Object hacer(ISifenClient c, SignedDocument doc) throws Exception;
    }

    /** Un escenario de falla y lo que se espera de él. */
    private record Falla(String nombre, Scenario escenario, String codigo, ErrorCategory categoria) {
    }

    private static List<Op> operaciones() {
        NumberRange rango = NumberRange.builder().timbrado(String.valueOf(CasosDePrueba.TIMBRADO)).establishment("1")
                .issuePoint("1").documentType(TTiDE.FACTURA_ELECTRONICA).from("50").to("55")
                .reason("Inutilizacion de prueba de numeracion").build();
        return List.of(
                new Op("send", SimulatedService.RECEIVE_DE, true, true, (c, d) -> c.send(d)),
                new Op("resolve (consulta)", SimulatedService.QUERY_DE, false, true, (c, d) -> c.resolve(d)),
                new Op("resolve (reenvío tras 0420)", SimulatedService.RECEIVE_DE, true, true, (c, d) -> {
                    sim.once(SimulatedService.QUERY_DE, Scenario.deNotFound());
                    return c.resolve(d);
                }),
                new Op("sendBatch", SimulatedService.RECEIVE_BATCH, true, true, (c, d) -> c.sendBatch(List.of(d))),
                new Op("cancel", SimulatedService.EVENT, true, true, (c, d) -> c.cancel(d.getCdc(), "Cancelacion de prueba del documento")),
                new Op("invalidateNumbers", SimulatedService.EVENT, true, true, (c, d) -> c.invalidateNumbers(rango)),
                new Op("sendEvents", SimulatedService.EVENT, true, true, (c, d) -> c.sendEvents(CasosDePrueba.eventos().get(0).construir())),
                new Op("queryDE", SimulatedService.QUERY_DE, false, false, (c, d) -> c.queryDE(d.getCdc())),
                new Op("queryRuc", SimulatedService.QUERY_RUC, false, false, (c, d) -> c.queryRuc("80123456")),
                new Op("queryBatch", SimulatedService.QUERY_BATCH, false, false, (c, d) -> c.queryBatch(Scenario.BATCH_NUMBER)));
    }

    /** Respuesta grabada de <b>otra</b> operación para cada servicio. */
    private static Scenario deOtraOperacion(SimulatedService s) {
        return s == SimulatedService.QUERY_RUC ? Scenario.eventRegistered() : Scenario.rucFound();
    }

    private static List<Falla> fallas(SimulatedService s) {
        return List.of(
                new Falla("corte sin responder", Scenario.connectionCut(), "JBS-CONEX-07", ErrorCategory.CONNECTION),
                new Falla("HTTP 502 de un proxy", Scenario.badGateway(), "JBS-CONEX-09", ErrorCategory.CONNECTION),
                new Falla("HTTP 504 de un proxy", Scenario.gatewayTimeout(), "JBS-CONEX-09", ErrorCategory.CONNECTION),
                new Falla("HTTP 200 que no es SOAP", Scenario.notSoap(), "JBS-CONEX-10", ErrorCategory.CONNECTION),
                new Falla("tiempo de lectura agotado", Scenario.delayed(Duration.ofMillis(1500), Scenario.approved()), "JBS-CONEX-05",
                        ErrorCategory.CONNECTION),
                new Falla("SOAP Fault (HTTP 500)", Scenario.soapFault(), "JBS-RESP-01", ErrorCategory.SIFEN_RESPONSE),
                new Falla("HTTP 200 sin cuerpo", Scenario.emptyBody(), "JBS-RESP-02", ErrorCategory.SIFEN_RESPONSE),
                new Falla("respuesta de otra operación", deOtraOperacion(s), "JBS-RESP-02", ErrorCategory.SIFEN_RESPONSE));
    }

    // ------------------------------------------------------------------ matriz

    @TestFactory
    public Stream<DynamicTest> matriz() {
        List<DynamicTest> l = new ArrayList<>();
        for (Op op : operaciones()) {
            for (Falla f : fallas(op.servicio())) {
                l.add(DynamicTest.dynamicTest(op.nombre() + " · " + f.nombre(), () -> verificar(op, f)));
            }
        }
        return l.stream();
    }

    private static ISifenClient cliente() throws SifenException {
        SifenConfig cfg = sim.config();
        cfg.setHttpReadTimeout(300);
        cfg.setMaxRetries(REINTENTOS);
        cfg.setRetryBackoffMs(10);
        return SifenClient.create(cfg);
    }

    /** El DE del caso 4 del catálogo con un número propio y las leyendas de DEV (Q14). */
    static DocumentoElectronico documento(AtomicInteger numero) {
        DocumentoElectronico de = CasosDePrueba.documentos().get(4).construir();
        de.getgDatGralOpe().getgEmis().setdNomEmi("DE generado en ambiente de prueba - sin valor comercial ni fiscal");
        de.getgDtipDE().getgCamItemList().get(0).setdDesProSer(
                "DOCUMENTO ELECTRÓNICO SIN VALOR COMERCIAL NI FISCAL - GENERADO EN AMBIENTE DE PRUEBA");
        de.getgTimb().setdNumDoc(String.format("%07d", numero.incrementAndGet()));
        return de;
    }

    private static void verificar(Op op, Falla f) throws Exception {
        sim.reset();
        ISifenClient c = cliente();
        SignedDocument doc = c.sign(documento(NUMERO));
        sim.on(op.servicio(), f.escenario());
        Throwable t = Assertions.assertThrows(Throwable.class, () -> op.llamada().hacer(c, doc), "tiene que fallar");
        Assertions.assertInstanceOf(SifenException.class, t, "solo SifenException: " + t);
        SifenException e = (SifenException) t;
        int pedidos = sim.requests(op.servicio()).size();
        TABLA.add(op.nombre() + "\t" + f.nombre() + "\t" + e.getCode() + "\t" + e.getCategory() + "\t" + e.isOutcomeUnknown()
                + "\t" + pedidos + "\t" + e.getCorrelationId());

        Assertions.assertEquals(f.codigo(), e.getCode(), e.getMessage());
        Assertions.assertEquals(f.categoria(), e.getCategory(), e.getCode());
        Assertions.assertTrue(ID.matcher(e.getCorrelationId()).matches(), e.getCorrelationId());
        Assertions.assertFalse(e.getUserMessage().isBlank());
        Assertions.assertTrue(e.getMessage().endsWith("(ref. " + e.getCorrelationId() + ")"), e.getMessage());
        Assertions.assertNotNull(e.getOperation());
        sinNulos(e);
        for (Throwable k = e; k != null; k = k.getCause()) {
            Assertions.assertFalse(k instanceof NullPointerException, "NPE en las causas");
        }
        Assertions.assertEquals(op.desconocido(), e.isOutcomeUnknown(), "isOutcomeUnknown (D43: después de transmitir y en resolve)");
        if (op.nombre().startsWith("resolve (consulta)")) {
            Assertions.assertTrue(sim.requests(SimulatedService.RECEIVE_DE).isEmpty(), "resolve no reenvía si la consulta falla");
        }
        if (op.recepcion()) {
            Assertions.assertEquals(1, pedidos, "D41/E5-01: una recepción nunca se reintenta");
        } else {
            Assertions.assertEquals(REINTENTABLES.contains(e.getCode()) ? REINTENTOS + 1 : 1, pedidos,
                    "una consulta reintenta solo las fallas rápidas de transporte");
        }
    }

    private static void sinNulos(SifenException e) throws Exception {
        for (Method m : e.getClass().getMethods()) {
            if (m.getParameterCount() != 0 || Modifier.isStatic(m.getModifiers()) || m.getDeclaringClass() == Object.class
                    || !(m.getName().startsWith("get") || m.getName().startsWith("is")) || m.getName().equals("getStackTrace")
                    || m.getName().equals("getSuppressed")) {
                continue;
            }
            if (m.invoke(e) == null) {
                Assertions.assertTrue(PUEDEN_SER_NULL.contains(m.getName()), m.getName() + "() devolvió null");
            }
        }
    }

    /** Resumen por operación (para el informe): operación → códigos vistos. */
    static Map<String, List<String>> resumen() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        for (String fila : TABLA) {
            String[] c = fila.split("\t");
            m.computeIfAbsent(c[0], k -> new ArrayList<>()).add(c[2]);
        }
        return m;
    }
}

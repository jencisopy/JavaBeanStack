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

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.javabeanstack.sifen.api.DEQueryResult;
import org.javabeanstack.sifen.api.ReconciliationItem;
import org.javabeanstack.sifen.api.ReconciliationResult;
import org.javabeanstack.sifen.api.ReconciliationStatus;
import org.javabeanstack.sifen.api.ResultStatus;
import org.javabeanstack.sifen.api.SifenMessage;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.exceptions.ErrorCategory;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.Operation;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.facade.Operations;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Ramas del conciliador que la suite no ejecutaba (plan JBSSIF, §8.8; M8-09): la espera real entre consultas, la
 * interrupción antes de consultar, un {@link ISifenClient} que lanza una {@code RuntimeException}, el tope
 * combinado con documentos de otro ambiente y el uso desde varios hilos.
 *
 * <p>Sin red y sin simulador: el cliente es un {@link Proxy} sin E/S cuyo veredicto sale solo del CDC (sin estado
 * compartido entre llamadas), y su {@code getConfig()} es un {@code new SifenConfig()} (ambiente DEV, nunca se
 * usa para conectarse). Los CDC son ficticios, con el RUC de prueba 80123456.</p>
 */
public class SifenReconcilerRamasTest {
    private static final String LOGGER_OPERACIONES = Operations.class.getName();
    private static final SifenConfig CONFIG_DEV = new SifenConfig();

    // ------------------------------------------------------------------ (1) la espera real

    /**
     * Sin inyectar el {@code sleeper} se usa {@code Thread.sleep}: el hueco entre dos consultas es al menos la pausa,
     * y no hay pausa antes de la primera ni después de la última. Se mide el hueco entre la salida de una consulta y
     * la entrada de la siguiente (no el total), así el tiempo de las consultas no entra en la cota.
     */
    @Test
    public void laPausaRealSeCumpleEntreConsultas() throws Exception {
        final long pausaMs = 80;
        List<long[]> marcas = Collections.synchronizedList(new ArrayList<>());
        ISifenClient cliente = cliente(cdc -> {
            long entra = System.nanoTime();
            DEQueryResult q = aprobado(cdc, digest(cdc));
            marcas.add(new long[] {entra, System.nanoTime()});
            return q;
        });
        SifenReconciler rec = SifenReconciler.builder(cliente).pause(Duration.ofMillis(pausaMs)).build();
        List<SignedDocument> docs = List.of(dev(1), dev(2), dev(3));

        rec.reconcile(List.of(dev(9))); // calentamiento: carga de clases fuera de la medición
        marcas.clear();

        long inicio = System.nanoTime();
        ReconciliationResult r = rec.reconcile(docs);
        long fin = System.nanoTime();

        Assertions.assertEquals(List.of(ReconciliationStatus.MATCH, ReconciliationStatus.MATCH, ReconciliationStatus.MATCH),
                estados(r), r.toString());
        Assertions.assertEquals(3, marcas.size());
        long umbral = TimeUnit.MILLISECONDS.toNanos(pausaMs - 1); // tolerancia de 1 ms por el redondeo del reloj
        for (int i = 1; i < marcas.size(); i++) {
            long hueco = marcas.get(i)[0] - marcas.get(i - 1)[1];
            Assertions.assertTrue(hueco >= umbral, "hueco entre la consulta " + i + " y la " + (i + 1) + ": "
                    + TimeUnit.NANOSECONDS.toMillis(hueco) + " ms < " + pausaMs + " ms");
        }
        long antes = marcas.get(0)[0] - inicio;
        long despues = fin - marcas.get(marcas.size() - 1)[1];
        long pausa = TimeUnit.MILLISECONDS.toNanos(pausaMs);
        Assertions.assertTrue(antes < pausa, "pausa antes de la primera: " + TimeUnit.NANOSECONDS.toMillis(antes) + " ms");
        Assertions.assertTrue(despues < pausa, "pausa después de la última: " + TimeUnit.NANOSECONDS.toMillis(despues) + " ms");
        Assertions.assertTrue(r.getDurationMs() >= 2 * (pausaMs - 1), "dos pausas en la duración: " + r.getDurationMs());
    }

    // ------------------------------------------------------------------ (2) interrupción antes de consultar

    /**
     * Hilo ya interrumpido al llamar a {@code reconcile}, con pausa y sin pausa: ninguna consulta, todo pendiente,
     * {@code INTERRUPTED} y la marca restituida. Sin el chequeo previo a la consulta, con pausa la primera se haría
     * igual (la pausa solo va entre dos consultas).
     */
    @Test
    public void interrumpidoAntesDeLaPrimeraConsulta() throws Exception {
        for (Duration pausa : List.of(Duration.ofMillis(50), Duration.ZERO)) {
            AtomicInteger consultas = new AtomicInteger();
            ISifenClient cliente = cliente(cdc -> {
                consultas.incrementAndGet();
                return aprobado(cdc, digest(cdc));
            });
            SifenReconciler rec = SifenReconciler.builder(cliente).pause(pausa).build();
            List<SignedDocument> docs = List.of(dev(1), dev(2));
            ReconciliationResult r;
            boolean marca;
            Thread.currentThread().interrupt();
            try {
                r = rec.reconcile(docs);
                marca = Thread.currentThread().isInterrupted();
            } finally {
                Thread.interrupted();
            }
            Assertions.assertTrue(marca, "pausa " + pausa + ": se restituye la marca de interrupción");
            Assertions.assertEquals(ReconciliationResult.StopReason.INTERRUPTED, r.getStopReason(), "pausa " + pausa);
            Assertions.assertEquals(0, consultas.get(), "pausa " + pausa + ": ninguna consulta");
            Assertions.assertEquals(0, r.getQueryCount());
            Assertions.assertTrue(r.getItems().isEmpty(), r.toString());
            Assertions.assertEquals(docs, r.getPending());
            Assertions.assertFalse(r.isComplete());
        }
    }

    /** Interrumpido durante una consulta, con pausa cero: la siguiente no se hace (el corte sin pausa). */
    @Test
    public void interrumpidoEntreConsultasSinPausa() throws Exception {
        AtomicInteger consultas = new AtomicInteger();
        ISifenClient cliente = cliente(cdc -> {
            consultas.incrementAndGet();
            Thread.currentThread().interrupt();
            return aprobado(cdc, digest(cdc));
        });
        SignedDocument a = dev(1);
        SignedDocument b = dev(2);
        SignedDocument k = dev(3);
        ReconciliationResult r;
        boolean marca;
        try {
            r = SifenReconciler.builder(cliente).pause(Duration.ZERO).build().reconcile(List.of(a, b, k));
            marca = Thread.currentThread().isInterrupted();
        } finally {
            Thread.interrupted();
        }
        Assertions.assertTrue(marca);
        Assertions.assertEquals(ReconciliationResult.StopReason.INTERRUPTED, r.getStopReason());
        Assertions.assertEquals(1, consultas.get());
        Assertions.assertEquals(List.of(ReconciliationStatus.MATCH), estados(r));
        Assertions.assertEquals(List.of(b, k), r.getPending());
    }

    // ------------------------------------------------------------------ (3) cliente que no cumple el contrato

    /**
     * {@code queryDE} lanza una {@code RuntimeException}: el ítem queda {@code UNDETERMINED} con {@code JBS-INT-00},
     * id de correlación (I8-18) y una línea ERROR en el log con ese id y sin el mensaje de la excepción (que solo va al
     * DEBUG del detalle técnico); la pasada
     * sigue. Una {@code SifenException} sin id de otro cliente también recibe id y su línea (WARN), y su
     * {@code dCodRes} sale en {@code getResponseCode()}.
     */
    @Test
    public void clienteQueLanzaRuntimeException() throws Exception {
        SignedDocument roto = dev(1);
        SignedDocument sinId = dev(2);
        SignedDocument bien = dev(3);
        String secreto = "texto-interno-que-no-va-al-log";
        ISifenClient cliente = cliente(cdc -> {
            if (cdc.equals(roto.getCdc())) {
                throw new IllegalStateException(secreto);
            }
            if (cdc.equals(sinId.getCdc())) {
                throw SifenException.ofResponse(ErrorCode.JBS_RESP_08, "prueba sin id", "0160");
            }
            return aprobado(cdc, digest(cdc));
        });
        List<LogEvent> log = Collections.synchronizedList(new ArrayList<>());
        ReconciliationResult r;
        capturar(log);
        try {
            r = SifenReconciler.builder(cliente).pause(Duration.ZERO).build().reconcile(List.of(roto, sinId, bien));
        } finally {
            soltar();
        }

        Assertions.assertEquals(List.of(ReconciliationStatus.UNDETERMINED, ReconciliationStatus.UNDETERMINED,
                ReconciliationStatus.MATCH), estados(r), r.toString());
        Assertions.assertTrue(r.isComplete(), "la falla no corta la pasada");
        Assertions.assertEquals(3, r.getQueryCount(), "la consulta que falló cuenta");

        SifenException e = r.getItems().get(0).getError();
        Assertions.assertNotNull(e);
        Assertions.assertEquals("JBS-INT-00", e.getCode(), e.toString());
        Assertions.assertEquals(ErrorCategory.INTERNAL, e.getCategory());
        conId(e);
        Assertions.assertEquals(roto.getCdc(), e.getReference(), "la referencia es el CDC");
        Assertions.assertInstanceOf(IllegalStateException.class, e.getCause());
        Assertions.assertNull(r.getItems().get(0).getQueryResult());
        Assertions.assertEquals("", r.getItems().get(0).getResponseCode(), "sin respuesta de SIFEN");
        List<LogEvent> deE = conId(log, e.getCorrelationId(), Level.ERROR);
        Assertions.assertEquals(1, deE.size(), "una línea ERROR con el id: " + textos(log));
        Assertions.assertTrue(deE.get(0).getMessage().getFormattedMessage().contains("JBS-INT-00 QUERY_DE ref=" + roto.getCdc()),
                deE.get(0).getMessage().getFormattedMessage());
        // el texto de la excepción solo puede ir en el DEBUG del detalle técnico, no en INFO o más (traza sin mensajes)
        for (LogEvent ev : log) {
            if (ev.getLevel().isMoreSpecificThan(Level.INFO)) {
                Assertions.assertFalse(ev.getMessage().getFormattedMessage().contains(secreto),
                        "el mensaje de la excepción no va al log: " + ev.getMessage().getFormattedMessage());
                for (Throwable t = ev.getThrown(); t != null; t = t.getCause() == t ? null : t.getCause()) {
                    Assertions.assertFalse(String.valueOf(t.getMessage()).contains(secreto), "la traza va sin mensajes");
                }
            }
        }

        SifenException s = r.getItems().get(1).getError();
        Assertions.assertEquals("JBS-RESP-08", s.getCode());
        conId(s);
        Assertions.assertEquals(sinId.getCdc(), s.getReference());
        Assertions.assertEquals("0160", r.getItems().get(1).getResponseCode(), "el dCodRes sale de la excepción");
        Assertions.assertEquals(1, conId(log, s.getCorrelationId(), Level.WARN).size(), String.valueOf(textos(log)));
        Assertions.assertNotEquals(e.getCorrelationId(), s.getCorrelationId());

        Assertions.assertEquals(List.of(roto.getCdc(), sinId.getCdc()),
                r.getItems(ReconciliationStatus.UNDETERMINED).stream().map(ReconciliationItem::getCdc).toList());
        Assertions.assertEquals(List.of(bien.getCdc()),
                r.getItems(ReconciliationStatus.MATCH).stream().map(ReconciliationItem::getCdc).toList());
        Assertions.assertTrue(r.getDiscrepancies().isEmpty(), "una falla no es una discrepancia");
    }

    // ------------------------------------------------------------------ (4) tope y otro ambiente

    /**
     * Comportamiento real: los documentos de otro ambiente <b>no consumen</b> consultas del tope ni pausa. Reciben su
     * {@code JBS-ENT-12} mientras la pasada no se corta, aun con el tope ya alcanzado; el corte ocurre recién al
     * llegar al siguiente documento consultable, y lo de otro ambiente que queda detrás de ese punto va a
     * pendientes, sin veredicto. Un tope igual a la cantidad de consultables termina {@code COMPLETED}.
     */
    @Test
    public void elTopeNoCuentaLosDeOtroAmbiente() throws Exception {
        AtomicInteger consultas = new AtomicInteger();
        ISifenClient cliente = cliente(cdc -> {
            consultas.incrementAndGet();
            return aprobado(cdc, digest(cdc));
        });
        List<Duration> pausas = Collections.synchronizedList(new ArrayList<>());
        SignedDocument a = dev(1);
        SignedDocument x = prod(2);
        SignedDocument b = dev(3);
        SignedDocument y = prod(4);
        SignedDocument k = dev(5);
        SignedDocument z = prod(6);

        // tope 2 con 3 consultables: a, x (sin consumir), b, y (ya con el tope alcanzado), corte en k
        ReconciliationResult r = SifenReconciler.builder(cliente).pause(Duration.ofMillis(10)).maxQueries(2)
                .sleeper(pausas::add).build().reconcile(List.of(a, x, b, y, k, z));
        Assertions.assertEquals(List.of(a.getCdc(), x.getCdc(), b.getCdc(), y.getCdc()),
                r.getItems().stream().map(ReconciliationItem::getCdc).toList(), r.toString());
        Assertions.assertEquals(List.of(ReconciliationStatus.MATCH, ReconciliationStatus.UNDETERMINED,
                ReconciliationStatus.MATCH, ReconciliationStatus.UNDETERMINED), estados(r));
        Assertions.assertEquals("JBS-ENT-12", r.getItems().get(1).getError().getCode());
        Assertions.assertEquals("JBS-ENT-12", r.getItems().get(3).getError().getCode());
        Assertions.assertEquals(List.of(k, z), r.getPending(), "el de otro ambiente detrás del corte queda pendiente");
        Assertions.assertEquals(ReconciliationResult.StopReason.MAX_QUERIES, r.getStopReason());
        Assertions.assertEquals(2, r.getQueryCount());
        Assertions.assertEquals(2, consultas.get());
        Assertions.assertEquals(1, pausas.size(), "una pausa: entre a y b; ninguna por x ni por y");

        // tope igual a la cantidad de consultables: completa, con los de otro ambiente juzgados
        consultas.set(0);
        pausas.clear();
        ReconciliationResult justo = SifenReconciler.builder(cliente).pause(Duration.ofMillis(10)).maxQueries(3)
                .sleeper(pausas::add).build().reconcile(List.of(a, x, b, y, k, z));
        Assertions.assertEquals(ReconciliationResult.StopReason.COMPLETED, justo.getStopReason(), justo.toString());
        Assertions.assertTrue(justo.isComplete());
        Assertions.assertTrue(justo.getPending().isEmpty());
        Assertions.assertEquals(6, justo.getItems().size());
        Assertions.assertEquals(3, justo.getQueryCount());
        Assertions.assertEquals(3, justo.getItems(ReconciliationStatus.UNDETERMINED).size());
        Assertions.assertEquals(2, pausas.size());

        // tope 1 y todos los de otro ambiente al principio: ninguno consume el tope
        consultas.set(0);
        ReconciliationResult primero = SifenReconciler.builder(cliente).pause(Duration.ZERO).maxQueries(1).build()
                .reconcile(List.of(x, y, a, b));
        Assertions.assertEquals(List.of(x.getCdc(), y.getCdc(), a.getCdc()),
                primero.getItems().stream().map(ReconciliationItem::getCdc).toList());
        Assertions.assertEquals(List.of(b), primero.getPending());
        Assertions.assertEquals(1, consultas.get());
    }

    // ------------------------------------------------------------------ (5) varios hilos

    /**
     * El mismo conciliador desde dos hilos a la vez: cada pasada da su propio resultado, completo y en el orden de su
     * entrada. Una barrera en la primera consulta de cada hilo garantiza que las dos pasadas se solapen; el cliente
     * no tiene estado (el veredicto sale del CDC).
     */
    @Test
    public void elMismoConciliadorDesdeDosHilos() throws Exception {
        CyclicBarrier barrera = new CyclicBarrier(2);
        ThreadLocal<boolean[]> primera = ThreadLocal.withInitial(() -> new boolean[] {true});
        ISifenClient cliente = cliente(cdc -> {
            if (primera.get()[0]) {
                primera.get()[0] = false;
                try {
                    barrera.await(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new AssertionError("las dos pasadas no se solaparon", e);
                }
            }
            return veredictoPorCdc(cdc);
        });
        SifenReconciler rec = SifenReconciler.builder(cliente).pause(Duration.ofMillis(1)).build();
        List<SignedDocument> uno = new ArrayList<>();
        List<SignedDocument> dos = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            uno.add(dev(100 + i));
        }
        for (int i = 1; i <= 17; i++) {
            dos.add(i % 5 == 0 ? prod(200 + i) : dev(200 + i));
        }
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<ReconciliationResult> f1 = hilos.submit(() -> rec.reconcile(uno));
            Future<ReconciliationResult> f2 = hilos.submit(() -> rec.reconcile(dos));
            ReconciliationResult r1 = f1.get(30, TimeUnit.SECONDS);
            ReconciliationResult r2 = f2.get(30, TimeUnit.SECONDS);
            verificarIndependiente(uno, r1);
            verificarIndependiente(dos, r2);
            Assertions.assertEquals(12, r1.getQueryCount());
            Assertions.assertEquals(14, r2.getQueryCount(), "17 menos los 3 de otro ambiente");
        } finally {
            hilos.shutdownNow();
        }
    }

    /** El resultado de una pasada corresponde exactamente a su entrada, como si hubiera corrido sola. */
    private static void verificarIndependiente(List<SignedDocument> docs, ReconciliationResult r) {
        Assertions.assertEquals(ReconciliationResult.StopReason.COMPLETED, r.getStopReason(), r.toString());
        Assertions.assertTrue(r.getPending().isEmpty());
        Assertions.assertEquals(docs.stream().map(SignedDocument::getCdc).toList(),
                r.getItems().stream().map(ReconciliationItem::getCdc).toList(), "orden e identidad de la entrada");
        for (int i = 0; i < docs.size(); i++) {
            SignedDocument d = docs.get(i);
            ReconciliationItem it = r.getItems().get(i);
            Assertions.assertSame(d, it.getDocument());
            ReconciliationStatus esperado = d.getEnvironment() != SifenConfig.Environment.DEV
                    ? ReconciliationStatus.UNDETERMINED : esperadoPorCdc(d.getCdc());
            Assertions.assertEquals(esperado, it.getStatus(), it.toString());
            if (it.getQueryResult() != null) {
                Assertions.assertEquals(d.getCdc(), it.getQueryResult().getCdc(), "la respuesta es la de su CDC");
            }
        }
    }

    // ------------------------------------------------------------------ apoyo

    /** Consulta del cliente falso. */
    @FunctionalInterface
    private interface Consulta {
        DEQueryResult queryDE(String cdc) throws SifenException;
    }

    /** Cliente sin E/S: {@code getConfig()} y {@code queryDE()}; cualquier otra llamada hace fallar el test. */
    private static ISifenClient cliente(Consulta consulta) {
        return (ISifenClient) Proxy.newProxyInstance(ISifenClient.class.getClassLoader(), new Class<?>[] {ISifenClient.class},
                (proxy, m, args) -> {
                    switch (m.getName()) {
                        case "getConfig":
                            return CONFIG_DEV;
                        case "queryDE":
                            return consulta.queryDE((String) args[0]);
                        case "toString":
                            return "cliente falso de conciliación";
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        default:
                            throw new AssertionError("la conciliación no debía llamar a " + m.getName());
                    }
                });
    }

    /** CDC ficticio de 44 dígitos con el RUC de prueba 80123456 y el número dado. */
    private static String cdc(int numero) {
        return "01" + "80123456" + "7" + "001" + "001" + String.format("%07d", numero) + "1" + "20261125" + "1"
                + "123456789" + "0";
    }

    /** Digest local ficticio derivado del número (no es un digest real). */
    private static String digest(String cdc) {
        return "digest-" + cdc.substring(17, 24) + "=";
    }

    private static SignedDocument dev(int numero) {
        String cdc = cdc(numero);
        return SignedDocument.builder().cdc(cdc).digestValue(digest(cdc)).environment(SifenConfig.Environment.DEV).build();
    }

    private static SignedDocument prod(int numero) {
        String cdc = cdc(numero);
        return SignedDocument.builder().cdc(cdc).digestValue(digest(cdc)).environment(SifenConfig.Environment.PROD).build();
    }

    private static DEQueryResult aprobado(String cdc, String digest) {
        return DEQueryResult.builder().status(ResultStatus.APPROVED).cdc(cdc).digestValue(digest)
                .messages(List.of(mensaje("0422"))).build();
    }

    /** Veredicto sin estado: el último dígito del número decide 0420, 0422 igual o 0422 con otro digest. */
    private static DEQueryResult veredictoPorCdc(String cdc) {
        return switch (esperadoPorCdc(cdc)) {
            case NOT_FOUND -> DEQueryResult.builder().status(ResultStatus.NOT_FOUND).cdc(cdc)
                    .messages(List.of(mensaje("0420"))).build();
            case DIGEST_MISMATCH -> aprobado(cdc, "otro-" + digest(cdc));
            default -> aprobado(cdc, digest(cdc));
        };
    }

    private static ReconciliationStatus esperadoPorCdc(String cdc) {
        int n = Integer.parseInt(cdc.substring(17, 24)) % 3;
        return n == 0 ? ReconciliationStatus.MATCH : n == 1 ? ReconciliationStatus.NOT_FOUND : ReconciliationStatus.DIGEST_MISMATCH;
    }

    private static SifenMessage mensaje(String codigo) {
        return SifenMessage.of(codigo, "mensaje de prueba " + codigo);
    }

    private static List<ReconciliationStatus> estados(ReconciliationResult r) {
        return r.getItems().stream().map(ReconciliationItem::getStatus).toList();
    }

    /** I8-18: operación, id de 8 hexadecimales y {@code (ref. <id>)} al final del mensaje. */
    private static void conId(SifenException e) {
        Assertions.assertEquals(Operation.QUERY_DE, e.getOperation(), e.toString());
        Assertions.assertNotNull(e.getCorrelationId(), e.toString());
        Assertions.assertTrue(e.getCorrelationId().matches("[0-9a-f]{8}"), e.getCorrelationId());
        Assertions.assertTrue(e.getMessage().endsWith("(ref. " + e.getCorrelationId() + ")"), e.getMessage());
    }

    /** Eventos del nivel dado que llevan el id (el DEBUG del detalle técnico no se cuenta). */
    private static List<LogEvent> conId(List<LogEvent> log, String id, Level nivel) {
        synchronized (log) {
            return log.stream().filter(ev -> ev.getLevel() == nivel)
                    .filter(ev -> ev.getMessage().getFormattedMessage().startsWith("[" + id + "]")).toList();
        }
    }

    private static List<String> textos(List<LogEvent> log) {
        synchronized (log) {
            return log.stream().map(ev -> ev.getLevel() + " " + ev.getMessage().getFormattedMessage()).toList();
        }
    }

    private static AbstractAppender captura;

    /** Captura el log de {@code Operations} (sin aditividad, para no ensuciar la salida), como FachadaFallasTest. */
    private static void capturar(List<LogEvent> destino) {
        LoggerContext contexto = (LoggerContext) LogManager.getContext(false);
        captura = new AbstractAppender("conciliacion-m809", null, null, true, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                destino.add(event.toImmutable());
            }
        };
        captura.start();
        LoggerConfig propio = LoggerConfig.newBuilder().withLoggerName(LOGGER_OPERACIONES).withLevel(Level.ALL)
                .withAdditivity(false).withConfig(contexto.getConfiguration()).build();
        propio.addAppender(captura, Level.ALL, null);
        contexto.getConfiguration().addLogger(LOGGER_OPERACIONES, propio);
        contexto.updateLoggers();
    }

    private static void soltar() {
        LoggerContext contexto = (LoggerContext) LogManager.getContext(false);
        contexto.getConfiguration().removeLogger(LOGGER_OPERACIONES);
        contexto.updateLoggers();
        captura.stop();
    }
}

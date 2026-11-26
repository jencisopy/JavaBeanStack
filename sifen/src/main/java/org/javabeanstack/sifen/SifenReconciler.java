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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.sifen.api.DEQueryResult;
import org.javabeanstack.sifen.api.ReconciliationItem;
import org.javabeanstack.sifen.api.ReconciliationResult;
import org.javabeanstack.sifen.api.ReconciliationStatus;
import org.javabeanstack.sifen.api.ResultStatus;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.Operation;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.facade.Operations;

/**
 * Conciliación de documentos firmados locales contra SIFEN (plan JBSSIF, §8.8; M3-30). <b>PROVISIONAL</b>.
 *
 * <p>Recorre una lista de {@link SignedDocument} —la arma la aplicación, por ejemplo los emitidos en un rango
 * de fechas— y consulta cada CDC con {@link ISifenClient#queryDE(String)}, con una pausa entre consultas y un
 * tope de consultas por pasada. Devuelve, por documento, si SIFEN lo tiene aprobado con el mismo digest
 * ({@link ReconciliationStatus#MATCH}), si SIFEN no lo tiene aprobado ({@link ReconciliationStatus#NOT_FOUND},
 * 0420: no lo tiene, lo rechazó o todavía lo procesa), si lo tiene con otro digest
 * ({@link ReconciliationStatus#DIGEST_MISMATCH}) o si no se pudo determinar
 * ({@link ReconciliationStatus#UNDETERMINED}).</p>
 *
 * <p><b>Qué lee de cada documento</b> (M8-08): solo {@link SignedDocument#getCdc()} (lo que se consulta),
 * {@link SignedDocument#getDigestValue()} (lo que se compara) y {@link SignedDocument#getEnvironment()} (el
 * control de ambiente). El XML firmado no se usa: un {@code SignedDocument} armado con
 * {@link SignedDocument#builder()} con esos tres datos, sin XML, concilia igual, y la aplicación puede armar la
 * lista desde sus columnas sin cargar los XML.</p>
 *
 * <p>Reglas (D41, D43):</p>
 * <ul>
 *   <li><b>Solo consulta</b>: nunca llama a {@code send}, {@code resolve}, {@code sendBatch} ni a los eventos.
 *       Qué hacer con una discrepancia lo decide la aplicación (un 0420 se resuelve con {@code resolve}, con la
 *       precondición de {@link ReconciliationStatus#NOT_FOUND}; un digest distinto lo revisa una persona).</li>
 *   <li>Los reintentos ante una falla rápida de transporte son los de la consulta ({@code sifen.reintentos.*}
 *       del cliente); la conciliación no agrega otros.</li>
 *   <li>Una falla de la consulta, un código que no es 0420 ni 0422 y un 0422 sin {@code DigestValue} son
 *       {@link ReconciliationStatus#UNDETERMINED}, <b>nunca</b> «no encontrado». Un documento de otro ambiente
 *       que el cliente, o de ambiente desconocido, también, con {@code JBS-ENT-12} y sin consultar: consultado
 *       en el ambiente equivocado daría 0420. Si el ambiente del cliente no se puede leer ({@code getConfig()}
 *       nulo, sin ambiente o que lanza: un cliente que no cumple el contrato), todos los documentos quedan así,
 *       sin consultar, y la falla del cliente va una vez al log como {@code JBS-INT-00}, con su id.</li>
 *   <li>Lo que no se llegó a consultar (tope o interrupción del hilo) queda en
 *       {@link ReconciliationResult#getPending()}, sin veredicto.</li>
 *   <li><b>La lista la filtra la aplicación</b>: sin documentos de un lote todavía en proceso ni con un envío
 *       en curso o reciente. Para ellos SIFEN contesta 0420 mientras procesa, y ese 0420 no es «ausente»
 *       (ver {@link ReconciliationStatus#NOT_FOUND}).</li>
 *   <li><b>Se recomienda fijar un tope por pasada</b> ({@link Builder#maxQueries(int)}; el valor por omisión,
 *       {@code 0}, es sin tope): sin él, una pasada larga contra un SIFEN que no contesta consume el tiempo de
 *       lectura completo por documento (60 s por omisión; los tiempos agotados no se reintentan) y el resultado
 *       retiene la respuesta de SIFEN de cada documento consultado ({@link ReconciliationItem#getQueryResult()}).
 *       Lo que quede pendiente se concilia en la pasada siguiente.</li>
 * </ul>
 *
 * <p><b>Errores, id y log</b> (I8-18): la conciliación no es una operación propia del catálogo
 * ({@code Operation} no tiene una): sus errores salen con la operación {@code Operation.QUERY_DE}, un id de
 * correlación de 8 hexadecimales que aparece en {@code getMessage()} como {@code (ref. <id>)} y una línea en el
 * log con ese id, como los de cualquier operación del cliente. Lo imprevisto ({@code JBS-INT-00}) se registra a
 * ERROR con la traza sin mensajes; lo de entrada ({@code JBS-ENT-11}, {@code JBS-ENT-12}), a WARN. Los errores
 * de la consulta son los del propio cliente con su id ({@code SifenClient} los registra); si un
 * {@code ISifenClient} de la aplicación lanza una {@code SifenException} sin id, la conciliación le asigna uno y la
 * registra.</p>
 *
 * <p>Bloquea durante toda la pasada (consultas y pausas): se corre fuera de una transacción de base y fuera
 * del hilo de un usuario. Es inmutable y seguro entre hilos; cada {@link #reconcile(List)} es independiente.
 * No depende de la implementación del cliente: sirve con cualquier {@link ISifenClient}.</p>
 *
 * <pre>{@code
 * SifenReconciler r = SifenReconciler.builder(sifen).pause(Duration.ofSeconds(1)).maxQueries(500).build();
 * ReconciliationResult res = r.reconcile(firmadosDelRango);
 * for (ReconciliationItem i : res.getDiscrepancies()) { ... }
 * }</pre>
 */
@Provisional
public final class SifenReconciler {
    private static final Logger LOGGER = LogManager.getLogger(SifenReconciler.class);
    /** Pausa entre consultas por omisión. */
    public static final Duration DEFAULT_PAUSE = Duration.ofSeconds(1);
    /** Operación con la que salen los errores de la conciliación (I8-18): una pasada es una serie de consultas. */
    private static final Operation OPERATION = Operation.QUERY_DE;
    private static final String UNKNOWN = "desconocido";

    /** Espera entre consultas; reemplazable en las pruebas del paquete. */
    @FunctionalInterface
    interface Sleeper {
        void sleep(Duration pause) throws InterruptedException;
    }

    private static final Sleeper THREAD_SLEEP = p -> Thread.sleep(p.toMillis());

    private final ISifenClient client;
    private final Duration pause;
    private final int maxQueries;
    private final Sleeper sleeper;

    private SifenReconciler(Builder b) {
        this.client = b.client;
        this.pause = b.pause;
        this.maxQueries = b.maxQueries;
        this.sleeper = b.sleeper;
    }

    /**
     * Crea un constructor para el cliente dado, con la pausa {@link #DEFAULT_PAUSE} y sin tope de consultas.
     *
     * @param client cliente de la empresa cuyos documentos se concilian.
     * @return el constructor.
     * @throws NullPointerException si {@code client} es {@code null}.
     */
    public static Builder builder(ISifenClient client) {
        return new Builder(Objects.requireNonNull(client, "client"));
    }

    /**
     * Devuelve la pausa entre dos consultas consecutivas.
     *
     * @return la pausa: {@link Duration#ZERO} = sin pausa; si no es cero, 1 ms como mínimo y entera en
     *         milisegundos (ver {@link Builder#pause(Duration)}).
     */
    public Duration getPause() {
        return pause;
    }

    /**
     * Devuelve el tope de consultas por pasada.
     *
     * @return el tope; {@code 0} = sin tope.
     */
    public int getMaxQueries() {
        return maxQueries;
    }

    /**
     * Concilia los documentos, en orden. La pausa va <b>entre</b> dos consultas (no antes de la primera ni
     * después de la última, y no por los documentos que no se consultan). Un CDC repetido se consulta cada vez.
     *
     * @param documents documentos firmados locales: cualquier {@link SignedDocument} con CDC, digest y ambiente
     *                  —de {@code sign}, de {@code readSigned} o armado por la aplicación con
     *                  {@link SignedDocument#builder()}, sin XML—; solo se leen {@code getCdc()},
     *                  {@code getDigestValue()} y {@code getEnvironment()}. Una lista vacía da un resultado vacío
     *                  y completo.
     * @return el resultado; nunca {@code null}. Si el hilo está interrumpido —antes de la primera consulta, entre
     *         dos consultas o durante la pausa—, el resultado sale con
     *         {@link ReconciliationResult.StopReason#INTERRUPTED}, lo no consultado en
     *         {@link ReconciliationResult#getPending()} y el hilo sigue marcado como interrumpido. Con el tope
     *         alcanzado o con una interrupción la pasada se corta en el primer documento que habría que consultar:
     *         un documento de otro ambiente (que no se consulta ni cuenta para el tope) recibe su
     *         {@code JBS-ENT-12} si está antes del corte y queda en {@code getPending()}, sin veredicto, si está
     *         después.
     * @throws SifenException {@code JBS-ENT-11} si la lista es {@code null} o tiene un elemento nulo (antes de
     *                        consultar nada), con id de correlación y la operación {@code QUERY_DE}. Las fallas de
     *                        cada consulta <b>no</b> se lanzan: van al veredicto.
     */
    public ReconciliationResult reconcile(List<SignedDocument> documents) throws SifenException {
        long start = System.nanoTime();
        List<SignedDocument> docs = Operations.execute(OPERATION, () -> null, () -> copy(documents));
        SifenConfig.Environment env = docs.isEmpty() ? null : clientEnvironment();
        List<ReconciliationItem> items = new ArrayList<>(docs.size());
        ReconciliationResult.StopReason stop = ReconciliationResult.StopReason.COMPLETED;
        int queries = 0;
        int next = 0;
        for (; next < docs.size(); next++) {
            SignedDocument d = docs.get(next);
            if (env == null || d.getEnvironment() == null || d.getEnvironment() != env) {
                // consultado en el ambiente equivocado daría 0420: no se consulta (D43)
                String local = d.getEnvironment() == null ? UNKNOWN : d.getEnvironment().name();
                String remote = env == null ? UNKNOWN : env.name();
                items.add(ReconciliationItem.of(d, ReconciliationStatus.UNDETERMINED, null, registered(d,
                        SifenException.ofRule(ErrorCode.JBS_ENT_12.getCode(), environmentMessage(local, remote),
                                "conciliación: ambiente del documento " + local + ", del cliente " + remote, null))));
                continue;
            }
            if (maxQueries > 0 && queries >= maxQueries) {
                stop = ReconciliationResult.StopReason.MAX_QUERIES;
                break;
            }
            if (queries > 0 && !pause.isZero()) {
                try {
                    sleeper.sleep(pause);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    stop = ReconciliationResult.StopReason.INTERRUPTED;
                    break;
                }
            }
            if (Thread.currentThread().isInterrupted()) {
                stop = ReconciliationResult.StopReason.INTERRUPTED;
                break;
            }
            queries++;
            items.add(check(d));
        }
        List<SignedDocument> pending = new ArrayList<>(docs.subList(next, docs.size()));
        ReconciliationResult r = ReconciliationResult.of(items, pending, stop, queries, elapsedMs(start));
        LOGGER.info("Conciliación con SIFEN: {} documentos, {} consultas, {} discrepancias, {} sin determinar, "
                + "{} pendientes ({})", docs.size(), queries, r.getDiscrepancies().size(),
                r.getItems(ReconciliationStatus.UNDETERMINED).size(), pending.size(), stop);
        return r;
    }

    /**
     * Copia la entrada validándola. El texto de {@code JBS-ENT-11} es el de la conciliación (I8-19): el del
     * catálogo habla del lote de un envío.
     */
    private static List<SignedDocument> copy(List<SignedDocument> documents) throws SifenException {
        if (documents == null) {
            throw SifenException.ofRule(ErrorCode.JBS_ENT_11.getCode(),
                    "La lista de documentos a conciliar es nula.", "conciliación sin lista", null);
        }
        List<SignedDocument> docs = new ArrayList<>(documents.size());
        for (int i = 0; i < documents.size(); i++) {
            SignedDocument d = documents.get(i);
            if (d == null) {
                throw SifenException.ofRule(ErrorCode.JBS_ENT_11.getCode(), "La lista de documentos a conciliar "
                        + "tiene un elemento nulo (el documento " + (i + 1) + ").",
                        "conciliación: elemento nulo en " + (i + 1), null);
            }
            docs.add(d);
        }
        return docs;
    }

    /** Texto de {@code JBS-ENT-12} en la conciliación (I8-19): el del catálogo dice «no se envía». */
    private static String environmentMessage(String local, String remote) {
        return "El documento se firmó para el ambiente " + local + " y este cliente opera en " + remote
                + ": no se consulta en SIFEN.";
    }

    /**
     * Ambiente del cliente; {@code null} si no se puede leer ({@code getConfig()} nulo, sin ambiente o que lanza:
     * un cliente que no cumple el contrato). La falla se registra una vez, con su id (M8-10).
     */
    private SifenConfig.Environment clientEnvironment() {
        return Operations.evaluate(OPERATION, () -> null, () -> {
            SifenConfig cfg = client.getConfig();
            if (cfg == null || cfg.getEnvironment() == null) {
                throw SifenException.of(ErrorCode.JBS_INT_00, "conciliación: el cliente "
                        + client.getClass().getName() + " devolvió una configuración "
                        + (cfg == null ? "nula" : "sin ambiente"), null);
            }
            return cfg.getEnvironment();
        }, e -> null);
    }

    /**
     * Una consulta y su veredicto; la falla de la consulta va al veredicto, no se lanza. La llamada al cliente
     * <b>no</b> va dentro de {@code Operations}: {@code SifenClient} asigna el id y registra sus propias fallas;
     * solo una falla sin id (de otro {@code ISifenClient}) se registra acá.
     */
    private ReconciliationItem check(SignedDocument d) {
        DEQueryResult q;
        try {
            q = client.queryDE(d.getCdc());
        } catch (SifenException e) {
            // la de SifenClient ya trae su id y su línea de log; la de otro cliente, sin id, se registra acá
            return ReconciliationItem.of(d, ReconciliationStatus.UNDETERMINED, null,
                    e.getCorrelationId() == null ? registered(d, e) : e);
        } catch (RuntimeException e) {
            // un cliente que no cumple el contrato (solo SifenException): no corta la pasada; se traduce y se
            // registra con id como en la fachada (JBS-INT-00 a ERROR si no se reconoce)
            return ReconciliationItem.of(d, ReconciliationStatus.UNDETERMINED, null, registered(d, e));
        }
        return ReconciliationItem.of(d, verdict(q, d.getDigestValue()), q, null);
    }

    /**
     * Pasa una falla por {@code Operations} para que lleve operación, referencia (el CDC) e id y quede en el log
     * una vez (I8-18). Una {@code SifenException} vuelve la misma instancia; otra excepción, traducida.
     */
    private static SifenException registered(SignedDocument d, Exception failure) {
        return Operations.evaluate(OPERATION, d::getCdc, () -> {
            if (failure instanceof SifenException se) {
                throw se;
            }
            throw (RuntimeException) failure;
        }, e -> e);
    }

    /** 0420 → no encontrado; 0422 con los dos digest → coincide o distinto; todo lo demás → sin determinar (D43). */
    static ReconciliationStatus verdict(DEQueryResult q, String localDigest) {
        if (q == null) {
            return ReconciliationStatus.UNDETERMINED;
        }
        if (q.getStatus() == ResultStatus.NOT_FOUND) {
            return ReconciliationStatus.NOT_FOUND;
        }
        if (q.getStatus() != ResultStatus.APPROVED) {
            return ReconciliationStatus.UNDETERMINED;
        }
        String sifen = q.getDigestValue().trim();
        String local = localDigest == null ? "" : localDigest.trim();
        if (sifen.isEmpty() || local.isEmpty()) {
            return ReconciliationStatus.UNDETERMINED;
        }
        return sifen.equals(local) ? ReconciliationStatus.MATCH : ReconciliationStatus.DIGEST_MISMATCH;
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    /** Constructor de {@link SifenReconciler}. No es seguro entre hilos. */
    public static final class Builder {
        private final ISifenClient client;
        private Duration pause = DEFAULT_PAUSE;
        private int maxQueries;
        private Sleeper sleeper = THREAD_SLEEP;

        private Builder(ISifenClient client) {
            this.client = client;
        }

        /**
         * Fija la pausa entre dos consultas consecutivas, para no saturar a SIFEN. La espera es en milisegundos:
         * una pausa que no es cero se <b>redondea hacia arriba</b> al milisegundo entero (una de 1 ns o de
         * 0,5 ms espera 1 ms; una de 1,2 ms, 2 ms), así que nunca se espera menos de lo pedido (M8-10).
         *
         * @param pause pausa; {@code null} o {@link Duration#ZERO} = sin pausa.
         * @return este constructor.
         * @throws IllegalArgumentException si es negativa o si no se puede expresar en milisegundos con un
         *                                  {@code long} (más de unos 292 millones de años).
         */
        public Builder pause(Duration pause) {
            if (pause != null && pause.isNegative()) {
                throw new IllegalArgumentException("pausa negativa: " + pause);
            }
            this.pause = pause == null || pause.isZero() ? Duration.ZERO : wholeMillis(pause);
            return this;
        }

        /** Redondea hacia arriba al milisegundo; rechaza lo que desbordaría {@code toMillis()} al esperar. */
        private static Duration wholeMillis(Duration pause) {
            try {
                Duration ms = pause.truncatedTo(ChronoUnit.MILLIS);
                if (ms.compareTo(pause) < 0) {
                    ms = ms.plusMillis(1);
                }
                ms.toMillis();
                return ms;
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("pausa demasiado grande: " + pause, e);
            }
        }

        /**
         * Fija el tope de consultas por pasada: alcanzado, el resto queda pendiente
         * ({@link ReconciliationResult.StopReason#MAX_QUERIES}).
         *
         * @param maxQueries tope; {@code 0} = sin tope.
         * @return este constructor.
         * @throws IllegalArgumentException si es negativo.
         */
        public Builder maxQueries(int maxQueries) {
            if (maxQueries < 0) {
                throw new IllegalArgumentException("tope negativo: " + maxQueries);
            }
            this.maxQueries = maxQueries;
            return this;
        }

        /** Reemplaza la espera (pruebas del paquete). */
        Builder sleeper(Sleeper sleeper) {
            this.sleeper = Objects.requireNonNull(sleeper, "sleeper");
            return this;
        }

        /**
         * Arma el conciliador.
         *
         * @return el conciliador inmutable.
         */
        public SifenReconciler build() {
            return new SifenReconciler(this);
        }
    }
}

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
package org.javabeanstack.sifen.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.javabeanstack.sifen.Provisional;

/**
 * Resultado de una conciliación de documentos firmados locales contra SIFEN (plan JBSSIF, §8.8; M3-30).
 * <b>PROVISIONAL</b>.
 *
 * <p>Inmutable y serializable; las listas son inmodificables y conservan el orden de entrada. Cada documento
 * de la entrada está en <b>una</b> de dos listas: {@link #getItems()} (tiene veredicto) o {@link #getPending()}
 * (no se llegó a consultar: tope de consultas o hilo interrumpido; se concilia en otra pasada).</p>
 */
@Provisional
public final class ReconciliationResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Por qué terminó la conciliación. Enum abierto: un valor desconocido se trata como «no completó». Siempre
     * {@link #COMPLETED} si y solo si no hay pendientes (lo garantiza {@link ReconciliationResult#of}).
     */
    public enum StopReason {
        /** Se concilió toda la entrada. */
        COMPLETED,
        /** Se alcanzó el tope de consultas de la pasada: el resto queda en {@link #getPending()}. */
        MAX_QUERIES,
        /**
         * El hilo se interrumpió —antes de la primera consulta, entre dos consultas o durante la pausa—: el resto
         * queda en {@link #getPending()}.
         */
        INTERRUPTED
    }

    private final List<ReconciliationItem> items;
    private final List<SignedDocument> pending;
    private final StopReason stopReason;
    private final int queryCount;
    private final long durationMs;

    private ReconciliationResult(List<ReconciliationItem> items, List<SignedDocument> pending, StopReason stopReason,
            int queryCount, long durationMs) {
        this.items = ApiValues.list(items);
        this.pending = ApiValues.list(pending);
        this.stopReason = stopReason == null ? StopReason.COMPLETED : stopReason;
        if ((this.stopReason == StopReason.COMPLETED) != this.pending.isEmpty()) {
            throw new IllegalArgumentException("motivo " + this.stopReason + " con " + this.pending.size()
                    + " pendientes: COMPLETED va sin pendientes y los demás motivos con alguno");
        }
        this.queryCount = queryCount;
        this.durationMs = durationMs;
    }

    /**
     * Arma el resultado (la librería en la conciliación; la aplicación en sus pruebas).
     *
     * @param items      veredictos, en el orden de la entrada.
     * @param pending    documentos sin consultar, en el orden de la entrada.
     * @param stopReason por qué terminó; {@code null} = {@link StopReason#COMPLETED}.
     * @param queryCount consultas a SIFEN hechas.
     * @param durationMs duración total, en milisegundos.
     * @return el resultado.
     * @throws IllegalArgumentException si el motivo y los pendientes se contradicen: {@code COMPLETED} (o
     *                                  {@code null}) con pendientes, o {@code MAX_QUERIES} / {@code INTERRUPTED}
     *                                  sin ninguno. Así {@link #getStopReason()} e {@link #isComplete()} nunca
     *                                  dicen cosas distintas.
     */
    public static ReconciliationResult of(List<ReconciliationItem> items, List<SignedDocument> pending,
            StopReason stopReason, int queryCount, long durationMs) {
        return new ReconciliationResult(items, pending, stopReason, queryCount, durationMs);
    }

    /**
     * Devuelve los veredictos, uno por documento conciliado, en el orden de la entrada.
     *
     * @return lista inmodificable; vacía si no se concilió ninguno.
     */
    public List<ReconciliationItem> getItems() {
        return items;
    }

    /**
     * Devuelve los veredictos con un estado dado, en el orden de la entrada.
     *
     * @param status estado buscado.
     * @return lista inmodificable; vacía si no hay o si {@code status} es {@code null}.
     */
    public List<ReconciliationItem> getItems(ReconciliationStatus status) {
        List<ReconciliationItem> out = new ArrayList<>();
        for (ReconciliationItem i : items) {
            if (i.getStatus() == status) {
                out.add(i);
            }
        }
        return List.copyOf(out);
    }

    /**
     * Devuelve las discrepancias confirmadas por SIFEN: los que SIFEN no tiene aprobados
     * ({@link ReconciliationStatus#NOT_FOUND}, 0420) y los que tiene con otro digest. No incluye los
     * {@link ReconciliationStatus#UNDETERMINED}.
     *
     * @return lista inmodificable, en el orden de la entrada.
     */
    public List<ReconciliationItem> getDiscrepancies() {
        List<ReconciliationItem> out = new ArrayList<>();
        for (ReconciliationItem i : items) {
            if (i.isDiscrepancy()) {
                out.add(i);
            }
        }
        return List.copyOf(out);
    }

    /**
     * Devuelve los documentos que no se llegaron a consultar (ver {@link #getStopReason()}). No tienen veredicto:
     * <b>no</b> son ausentes.
     *
     * <p>Son todos los documentos desde el corte (el primero que habría que consultar con el tope alcanzado o
     * con el hilo interrumpido) hasta el final, en el orden de la entrada. Un documento de otro ambiente no se
     * consulta: si está antes del corte tiene veredicto ({@code UNDETERMINED} con {@code JBS-ENT-12}); si está
     * después, queda acá sin veredicto, como los demás.</p>
     *
     * @return lista inmodificable; vacía si la conciliación completó.
     */
    public List<SignedDocument> getPending() {
        return pending;
    }

    /**
     * Devuelve por qué terminó la conciliación.
     *
     * @return el motivo; nunca {@code null}.
     */
    public StopReason getStopReason() {
        return stopReason;
    }

    /**
     * Indica si se concilió toda la entrada.
     *
     * @return {@code true} si no quedó ningún documento pendiente; equivale a
     *         {@code getStopReason() == StopReason.COMPLETED}.
     */
    public boolean isComplete() {
        return pending.isEmpty();
    }

    /**
     * Devuelve la cantidad de consultas a SIFEN que se hicieron (los reintentos de transporte de una consulta no
     * se cuentan aparte).
     *
     * @return la cantidad.
     */
    public int getQueryCount() {
        return queryCount;
    }

    /**
     * Devuelve la duración total, pausas incluidas.
     *
     * @return la duración, en milisegundos.
     */
    public long getDurationMs() {
        return durationMs;
    }

    @Override
    public String toString() {
        return "ReconciliationResult[items=" + items.size() + ", discrepancies=" + getDiscrepancies().size()
                + ", undetermined=" + getItems(ReconciliationStatus.UNDETERMINED).size() + ", pending=" + pending.size()
                + ", stop=" + stopReason + ", queries=" + queryCount + "]";
    }
}

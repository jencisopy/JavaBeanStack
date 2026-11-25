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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado de la consulta de un lote: su estado y un {@link SendResult} por documento.
 *
 * <p>Contrato v1 (plan JBSSIF, §10.2). Inmutable y serializable; los textos nunca son {@code null} y
 * las listas son inmodificables. Se arma con {@link #builder()}: la librería lo hace en sus operaciones
 * y la aplicación puede hacerlo en sus pruebas.</p>
 */
public final class BatchQueryResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final BatchStatus status;
    private final String batchNumber;
    private final LocalDateTime processingDate;
    private final List<SendResult> results;
    private final Duration suggestedWait;
    private final List<SifenMessage> messages;
    private final String operationId;
    private final String rawResponse;
    private final long durationMs;

    private BatchQueryResult(Builder b) {
        this.status = b.status == null ? BatchStatus.UNRECOGNIZED : b.status;
        this.batchNumber = ApiValues.text(b.batchNumber);
        this.processingDate = b.processingDate;
        this.results = ApiValues.list(b.results);
        this.suggestedWait = b.suggestedWait == null ? Duration.ZERO : b.suggestedWait;
        this.messages = ApiValues.messages(b.messages);
        this.operationId = ApiValues.text(b.operationId);
        this.rawResponse = ApiValues.text(b.rawResponse);
        this.durationMs = b.durationMs;
    }

    /**
     * Crea un constructor.
     *
     * @return el constructor, con todo vacío.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Devuelve el estado del lote ({@code IN_PROCESS} 0361, {@code COMPLETED} 0362, {@code NOT_FOUND} 0360, {@code EXPIRED} 0364); nunca {@code null}.
     *
     * @return el estado del lote ({@code IN_PROCESS} 0361, {@code COMPLETED} 0362, {@code NOT_FOUND} 0360, {@code EXPIRED} 0364); nunca {@code null}.
     */
    public BatchStatus getStatus() {
        return status;
    }

    /**
     * Devuelve el número de lote consultado.
     *
     * @return el número de lote consultado.
     */
    public String getBatchNumber() {
        return batchNumber;
    }

    /**
     * Devuelve la fecha de proceso ({@code dFecProc}), o {@code null} si la respuesta no la trae.
     *
     * @return la fecha de proceso ({@code dFecProc}), o {@code null} si la respuesta no la trae.
     */
    public LocalDateTime getProcessingDate() {
        return processingDate;
    }

    /**
     * Devuelve un resultado por documento del lote (lista inmodificable; vacía mientras el lote está en proceso).
     *
     * @return un resultado por documento del lote (lista inmodificable; vacía mientras el lote está en proceso).
     */
    public List<SendResult> getResults() {
        return results;
    }

    /**
     * Devuelve cuánto esperar antes de volver a consultar (10 minutos mientras el lote está en proceso, Guía de Mejores Prácticas); {@link Duration#ZERO} si no hay que volver a consultar.
     *
     * @return cuánto esperar antes de volver a consultar (10 minutos mientras el lote está en proceso, Guía de Mejores Prácticas); {@link Duration#ZERO} si no hay que volver a consultar.
     */
    public Duration getSuggestedWait() {
        return suggestedWait;
    }

    /**
     * Devuelve lista inmodificable de los mensajes de SIFEN, hasta 100; vacía si no hubo.
     *
     * @return lista inmodificable de los mensajes de SIFEN, hasta 100; vacía si no hubo.
     */
    public List<SifenMessage> getMessages() {
        return messages;
    }

    /**
     * Devuelve ocho caracteres hexadecimales: el mismo id que encabeza las líneas del log.
     *
     * @return ocho caracteres hexadecimales: el mismo id que encabeza las líneas del log.
     */
    public String getOperationId() {
        return operationId;
    }

    /**
     * Devuelve el XML de la respuesta de SIFEN, para auditoría (tiene datos del documento: no se registra en el log).
     *
     * @return el XML de la respuesta de SIFEN, para auditoría (tiene datos del documento: no se registra en el log).
     */
    public String getRawResponse() {
        return rawResponse;
    }

    /**
     * Devuelve la duración de la llamada, en milisegundos.
     *
     * @return la duración de la llamada, en milisegundos.
     */
    public long getDurationMs() {
        return durationMs;
    }

    /**
     * Devuelve el código del primer mensaje, para mostrar en una línea.
     *
     * @return el código; vacío si no hubo mensajes.
     */
    public String getResponseCode() {
        return messages.isEmpty() ? "" : messages.get(0).getCode();
    }

    /**
     * Devuelve el texto del primer mensaje, para mostrar en una línea.
     *
     * @return el texto; vacío si no hubo mensajes.
     */
    public String getMessage() {
        return messages.isEmpty() ? "" : messages.get(0).getText();
    }

    @Override
    public String toString() {
        return "BatchQueryResult[status=" + status + ", op=" + operationId + "]";
    }

    /** Constructor de {@link BatchQueryResult}. No es seguro entre hilos. */
    public static final class Builder {
        private BatchStatus status;
        private String batchNumber;
        private LocalDateTime processingDate;
        private List<SendResult> results;
        private Duration suggestedWait;
        private List<SifenMessage> messages;
        private String operationId;
        private String rawResponse;
        private long durationMs;

        private Builder() {
        }

        /**
         * Fija el valor de {@code status}.
         *
         * @param status estado; {@code null} = {@link BatchStatus#UNRECOGNIZED}.
         * @return este constructor.
         */
        public Builder status(BatchStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Fija el valor de {@code batchNumber}.
         *
         * @param batchNumber número de lote.
         * @return este constructor.
         */
        public Builder batchNumber(String batchNumber) {
            this.batchNumber = batchNumber;
            return this;
        }

        /**
         * Fija el valor de {@code processingDate}.
         *
         * @param processingDate fecha de proceso.
         * @return este constructor.
         */
        public Builder processingDate(LocalDateTime processingDate) {
            this.processingDate = processingDate;
            return this;
        }

        /**
         * Fija el valor de {@code results}.
         *
         * @param results resultados por documento.
         * @return este constructor.
         */
        public Builder results(List<SendResult> results) {
            this.results = results;
            return this;
        }

        /**
         * Fija el valor de {@code suggestedWait}.
         *
         * @param suggestedWait espera sugerida; {@code null} = cero.
         * @return este constructor.
         */
        public Builder suggestedWait(Duration suggestedWait) {
            this.suggestedWait = suggestedWait;
            return this;
        }

        /**
         * Fija el valor de {@code messages}.
         *
         * @param messages mensajes de SIFEN (se conservan los primeros 100).
         * @return este constructor.
         */
        public Builder messages(List<SifenMessage> messages) {
            this.messages = messages;
            return this;
        }

        /**
         * Fija el valor de {@code operationId}.
         *
         * @param operationId id de la operación.
         * @return este constructor.
         */
        public Builder operationId(String operationId) {
            this.operationId = operationId;
            return this;
        }

        /**
         * Fija el valor de {@code rawResponse}.
         *
         * @param rawResponse respuesta bruta.
         * @return este constructor.
         */
        public Builder rawResponse(String rawResponse) {
            this.rawResponse = rawResponse;
            return this;
        }

        /**
         * Fija el valor de {@code durationMs}.
         *
         * @param durationMs duración en milisegundos.
         * @return este constructor.
         */
        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        /**
         * Arma el valor.
         *
         * @return el valor inmutable.
         */
        public BatchQueryResult build() {
            return new BatchQueryResult(this);
        }
    }
}

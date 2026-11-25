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
import java.util.List;

/**
 * Resultado del envío de un lote (asíncrono): el número de lote para consultarlo después.
 *
 * <p>Contrato v1 (plan JBSSIF, §10.2). Inmutable y serializable; los textos nunca son {@code null} y
 * las listas son inmodificables. Se arma con {@link #builder()}: la librería lo hace en sus operaciones
 * y la aplicación puede hacerlo en sus pruebas.</p>
 */
public final class BatchSendResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final BatchStatus status;
    private final String batchNumber;
    private final List<String> cdcs;
    private final Duration estimatedProcessingTime;
    private final List<SifenMessage> messages;
    private final String operationId;
    private final String rawResponse;
    private final long durationMs;

    private BatchSendResult(Builder b) {
        this.status = b.status == null ? BatchStatus.UNRECOGNIZED : b.status;
        this.batchNumber = ApiValues.text(b.batchNumber);
        this.cdcs = ApiValues.list(b.cdcs);
        this.estimatedProcessingTime = b.estimatedProcessingTime == null ? Duration.ZERO : b.estimatedProcessingTime;
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
     * Devuelve el estado de la recepción ({@link BatchStatus#RECEIVED} 0300 o {@link BatchStatus#NOT_QUEUED} 0301); nunca {@code null}.
     *
     * @return el estado de la recepción ({@link BatchStatus#RECEIVED} 0300 o {@link BatchStatus#NOT_QUEUED} 0301); nunca {@code null}.
     */
    public BatchStatus getStatus() {
        return status;
    }

    /**
     * Devuelve el número de lote ({@code dProtConsLote}); vacío si SIFEN no lo encoló.
     *
     * @return el número de lote ({@code dProtConsLote}); vacío si SIFEN no lo encoló.
     */
    public String getBatchNumber() {
        return batchNumber;
    }

    /**
     * Devuelve los CDC del lote en el orden enviado (lista inmodificable).
     *
     * @return los CDC del lote en el orden enviado (lista inmodificable).
     */
    public List<String> getCdcs() {
        return cdcs;
    }

    /**
     * Devuelve el tiempo estimado de procesamiento que informó SIFEN ({@code dTpoProces}); {@link Duration#ZERO} si no lo informó.
     *
     * @return el tiempo estimado de procesamiento que informó SIFEN ({@code dTpoProces}); {@link Duration#ZERO} si no lo informó.
     */
    public Duration getEstimatedProcessingTime() {
        return estimatedProcessingTime;
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
        return "BatchSendResult[status=" + status + ", op=" + operationId + "]";
    }

    /** Constructor de {@link BatchSendResult}. No es seguro entre hilos. */
    public static final class Builder {
        private BatchStatus status;
        private String batchNumber;
        private List<String> cdcs;
        private Duration estimatedProcessingTime;
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
         * Fija el valor de {@code cdcs}.
         *
         * @param cdcs CDC en el orden enviado.
         * @return este constructor.
         */
        public Builder cdcs(List<String> cdcs) {
            this.cdcs = cdcs;
            return this;
        }

        /**
         * Fija el valor de {@code estimatedProcessingTime}.
         *
         * @param estimatedProcessingTime tiempo estimado; {@code null} = cero.
         * @return este constructor.
         */
        public Builder estimatedProcessingTime(Duration estimatedProcessingTime) {
            this.estimatedProcessingTime = estimatedProcessingTime;
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
        public BatchSendResult build() {
            return new BatchSendResult(this);
        }
    }
}

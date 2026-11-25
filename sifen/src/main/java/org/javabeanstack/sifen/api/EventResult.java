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
import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado del envío de un evento (cancelación, inutilización y demás eventos del emisor). Ante un resultado desconocido, {@code queryDE(cdc)} muestra si el evento quedó registrado.
 *
 * <p>Contrato v1 (plan JBSSIF, §10.2). Inmutable y serializable; los textos nunca son {@code null} y
 * las listas son inmodificables. Se arma con {@link #builder()}: la librería lo hace en sus operaciones
 * y la aplicación puede hacerlo en sus pruebas.</p>
 */
public final class EventResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ResultStatus status;
    private final String eventId;
    private final String authorizationProtocol;
    private final LocalDateTime processingDate;
    private final String eventXml;
    private final List<SifenMessage> messages;
    private final String operationId;
    private final String rawResponse;
    private final long durationMs;

    private EventResult(Builder b) {
        this.status = b.status == null ? ResultStatus.UNRECOGNIZED : b.status;
        this.eventId = ApiValues.text(b.eventId);
        this.authorizationProtocol = ApiValues.text(b.authorizationProtocol);
        this.processingDate = b.processingDate;
        this.eventXml = ApiValues.text(b.eventXml);
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
     * Devuelve el estado del evento; nunca {@code null}.
     *
     * @return el estado del evento; nunca {@code null}.
     */
    public ResultStatus getStatus() {
        return status;
    }

    /**
     * Devuelve el id del evento ({@code Id} de {@code rEve}).
     *
     * @return el id del evento ({@code Id} de {@code rEve}).
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Devuelve el número de autorización del evento ({@code dProtAut}); vacío si no se aprobó.
     *
     * @return el número de autorización del evento ({@code dProtAut}); vacío si no se aprobó.
     */
    public String getAuthorizationProtocol() {
        return authorizationProtocol;
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
     * Devuelve el evento firmado que se envió.
     *
     * @return el evento firmado que se envió.
     */
    public String getEventXml() {
        return eventXml;
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
        return "EventResult[status=" + status + ", op=" + operationId + "]";
    }

    /** Constructor de {@link EventResult}. No es seguro entre hilos. */
    public static final class Builder {
        private ResultStatus status;
        private String eventId;
        private String authorizationProtocol;
        private LocalDateTime processingDate;
        private String eventXml;
        private List<SifenMessage> messages;
        private String operationId;
        private String rawResponse;
        private long durationMs;

        private Builder() {
        }

        /**
         * Fija el valor de {@code status}.
         *
         * @param status estado; {@code null} = {@link ResultStatus#UNRECOGNIZED}.
         * @return este constructor.
         */
        public Builder status(ResultStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Fija el valor de {@code eventId}.
         *
         * @param eventId id del evento.
         * @return este constructor.
         */
        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        /**
         * Fija el valor de {@code authorizationProtocol}.
         *
         * @param authorizationProtocol número de autorización.
         * @return este constructor.
         */
        public Builder authorizationProtocol(String authorizationProtocol) {
            this.authorizationProtocol = authorizationProtocol;
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
         * Fija el valor de {@code eventXml}.
         *
         * @param eventXml XML del evento enviado.
         * @return este constructor.
         */
        public Builder eventXml(String eventXml) {
            this.eventXml = eventXml;
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
        public EventResult build() {
            return new EventResult(this);
        }
    }
}

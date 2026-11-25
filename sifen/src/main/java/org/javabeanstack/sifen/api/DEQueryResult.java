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
 * Resultado de la consulta de un documento por CDC. 0420 significa «no existe <b>o no está aprobado</b>»: un rechazado también da {@link ResultStatus#NOT_FOUND}.
 *
 * <p>Contrato v1 (plan JBSSIF, §10.2). Inmutable y serializable; los textos nunca son {@code null} y
 * las listas son inmodificables. Se arma con {@link #builder()}: la librería lo hace en sus operaciones
 * y la aplicación puede hacerlo en sus pruebas.</p>
 */
public final class DEQueryResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ResultStatus status;
    private final String cdc;
    private final String authorizationProtocol;
    private final LocalDateTime processingDate;
    private final String xml;
    private final String digestValue;
    private final List<String> events;
    private final List<SifenMessage> messages;
    private final String operationId;
    private final String rawResponse;
    private final long durationMs;

    private DEQueryResult(Builder b) {
        this.status = b.status == null ? ResultStatus.UNRECOGNIZED : b.status;
        this.cdc = ApiValues.text(b.cdc);
        this.authorizationProtocol = ApiValues.text(b.authorizationProtocol);
        this.processingDate = b.processingDate;
        this.xml = ApiValues.text(b.xml);
        this.digestValue = ApiValues.text(b.digestValue);
        this.events = ApiValues.list(b.events);
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
     * Devuelve el estado ({@link ResultStatus#APPROVED} con 0422, {@link ResultStatus#NOT_FOUND} con 0420); nunca {@code null}.
     *
     * @return el estado ({@link ResultStatus#APPROVED} con 0422, {@link ResultStatus#NOT_FOUND} con 0420); nunca {@code null}.
     */
    public ResultStatus getStatus() {
        return status;
    }

    /**
     * Devuelve el CDC consultado.
     *
     * @return el CDC consultado.
     */
    public String getCdc() {
        return cdc;
    }

    /**
     * Devuelve el número de autorización ({@code dProtAut}); vacío si SIFEN no tiene el documento.
     *
     * @return el número de autorización ({@code dProtAut}); vacío si SIFEN no tiene el documento.
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
     * Devuelve el {@code rDE} que tiene SIFEN, como XML; vacío si no lo tiene.
     *
     * @return el {@code rDE} que tiene SIFEN, como XML; vacío si no lo tiene.
     */
    public String getXml() {
        return xml;
    }

    /**
     * Devuelve el {@code DigestValue} de la firma del {@code rDE} que tiene SIFEN; vacío si no lo tiene.
     *
     * @return el {@code DigestValue} de la firma del {@code rDE} que tiene SIFEN; vacío si no lo tiene.
     */
    public String getDigestValue() {
        return digestValue;
    }

    /**
     * Devuelve el XML de cada evento registrado sobre el documento ({@code rContEv}: cancelación, conformidad…), en el orden de la respuesta; lista inmodificable.
     *
     * @return el XML de cada evento registrado sobre el documento ({@code rContEv}: cancelación, conformidad…), en el orden de la respuesta; lista inmodificable.
     */
    public List<String> getEvents() {
        return events;
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
        return "DEQueryResult[status=" + status + ", op=" + operationId + "]";
    }

    /** Constructor de {@link DEQueryResult}. No es seguro entre hilos. */
    public static final class Builder {
        private ResultStatus status;
        private String cdc;
        private String authorizationProtocol;
        private LocalDateTime processingDate;
        private String xml;
        private String digestValue;
        private List<String> events;
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
         * Fija el valor de {@code cdc}.
         *
         * @param cdc CDC consultado.
         * @return este constructor.
         */
        public Builder cdc(String cdc) {
            this.cdc = cdc;
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
         * Fija el valor de {@code xml}.
         *
         * @param xml XML del {@code rDE} que tiene SIFEN.
         * @return este constructor.
         */
        public Builder xml(String xml) {
            this.xml = xml;
            return this;
        }

        /**
         * Fija el valor de {@code digestValue}.
         *
         * @param digestValue digest del {@code rDE} de SIFEN.
         * @return este constructor.
         */
        public Builder digestValue(String digestValue) {
            this.digestValue = digestValue;
            return this;
        }

        /**
         * Fija el valor de {@code events}.
         *
         * @param events XML de los eventos registrados.
         * @return este constructor.
         */
        public Builder events(List<String> events) {
            this.events = events;
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
        public DEQueryResult build() {
            return new DEQueryResult(this);
        }
    }
}

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
 * Resultado del envío de un documento, o del estado de un documento dentro de un lote o de una
 * recuperación (contrato v1, plan JBSSIF, §10.2). Inmutable y serializable.
 *
 * <p>Un rechazo de SIFEN <b>no</b> es una excepción: vuelve acá con {@link ResultStatus#REJECTED} y
 * sus mensajes. Los textos nunca son {@code null} (vacíos si SIFEN no los informó); la fecha de
 * proceso es {@code null} solo si la respuesta no la trae. Se arma con {@link #builder()}: la
 * librería lo hace en sus operaciones y la aplicación puede hacerlo en sus pruebas.</p>
 */
public final class SendResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ResultStatus status;
    private final String cdc;
    private final String authorizationProtocol;
    private final LocalDateTime processingDate;
    private final String digestValue;
    private final boolean digestMismatch;
    private final boolean alreadyRegistered;
    private final List<SifenMessage> messages;
    private final String operationId;
    private final String rawResponse;
    private final long durationMs;

    private SendResult(Builder b) {
        this.status = b.status == null ? ResultStatus.UNRECOGNIZED : b.status;
        this.cdc = ApiValues.text(b.cdc);
        this.authorizationProtocol = ApiValues.text(b.authorizationProtocol);
        this.processingDate = b.processingDate;
        this.digestValue = ApiValues.text(b.digestValue);
        this.digestMismatch = b.digestMismatch;
        this.alreadyRegistered = b.alreadyRegistered;
        this.messages = ApiValues.messages(b.messages);
        this.operationId = ApiValues.text(b.operationId);
        this.rawResponse = ApiValues.text(b.rawResponse);
        this.durationMs = b.durationMs;
    }

    /**
     * Crea un constructor de resultados.
     *
     * @return el constructor, con estado {@link ResultStatus#UNRECOGNIZED} y todo vacío.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Devuelve el estado interpretado.
     *
     * @return el estado; nunca {@code null}.
     */
    public ResultStatus getStatus() {
        return status;
    }

    /**
     * Devuelve el CDC del documento.
     *
     * @return el CDC de 44 dígitos.
     */
    public String getCdc() {
        return cdc;
    }

    /**
     * Devuelve el número de autorización ({@code dProtAut}).
     *
     * @return el número; vacío si SIFEN no lo dio (rechazo, en proceso).
     */
    public String getAuthorizationProtocol() {
        return authorizationProtocol;
    }

    /**
     * Devuelve la fecha y hora de proceso ({@code dFecProc}).
     *
     * @return la fecha, o {@code null} si la respuesta no la trae.
     */
    public LocalDateTime getProcessingDate() {
        return processingDate;
    }

    /**
     * Devuelve el {@code dDigVal} que informó SIFEN.
     *
     * @return el digest en Base64; vacío si SIFEN no lo informó.
     */
    public String getDigestValue() {
        return digestValue;
    }

    /**
     * Indica si SIFEN tiene <b>otro</b> XML para ese CDC (un DE reenviado con otra firma, como en el caso de redondeo en USD): SIFEN informó un {@code dDigVal}
     * y es distinto del {@code DigestValue} del XML local (D43; reemplaza a {@code isLocalMatch()}, con el
     * sentido inverso).
     *
     * @return {@code true} solo con un digest informado y distinto; {@code false} si coinciden, si SIFEN no
     *         informó digest o si no hay XML local con qué comparar (resultados de una consulta de lote).
     */
    public boolean isDigestMismatch() {
        return digestMismatch;
    }

    /**
     * Indica si el estado salió de una consulta (respuesta 1001 o {@code resolve}) y no de este envío.
     *
     * @return {@code true} si el documento ya estaba registrado en SIFEN.
     */
    public boolean isAlreadyRegistered() {
        return alreadyRegistered;
    }

    /**
     * Devuelve los mensajes de SIFEN ({@code gResProc}), hasta 100.
     *
     * @return lista inmodificable; vacía si no hubo.
     */
    public List<SifenMessage> getMessages() {
        return messages;
    }

    /**
     * Devuelve el código del primer mensaje, para mostrar en una línea.
     *
     * @return el {@code dCodRes}; vacío si no hubo mensajes.
     */
    public String getResponseCode() {
        return messages.isEmpty() ? "" : messages.get(0).getCode();
    }

    /**
     * Devuelve el texto del primer mensaje, para mostrar en una línea.
     *
     * @return el {@code dMsgRes}; vacío si no hubo mensajes.
     */
    public String getMessage() {
        return messages.isEmpty() ? "" : messages.get(0).getText();
    }

    /**
     * Devuelve el id de la operación: el mismo que encabeza las líneas del log.
     *
     * @return ocho caracteres hexadecimales.
     */
    public String getOperationId() {
        return operationId;
    }

    /**
     * Devuelve el XML de la respuesta de SIFEN, para auditoría. Contiene datos del documento: no se
     * registra en el log ni se muestra al usuario final.
     *
     * @return el XML; vacío si no aplica (un documento dentro de un lote lo tiene en el resultado
     *         del lote).
     */
    public String getRawResponse() {
        return rawResponse;
    }

    /**
     * Devuelve la duración de la llamada.
     *
     * @return milisegundos.
     */
    public long getDurationMs() {
        return durationMs;
    }

    @Override
    public String toString() {
        return "SendResult[" + status + ", cdc=" + cdc + ", " + getResponseCode() + ", op=" + operationId + "]";
    }

    /** Constructor de {@link SendResult}. No es seguro entre hilos. */
    public static final class Builder {
        private ResultStatus status;
        private String cdc;
        private String authorizationProtocol;
        private LocalDateTime processingDate;
        private String digestValue;
        private boolean digestMismatch;
        private boolean alreadyRegistered;
        private List<SifenMessage> messages;
        private String operationId;
        private String rawResponse;
        private long durationMs;

        private Builder() {
        }

        /**
         * Fija el estado.
         *
         * @param status estado; {@code null} = {@link ResultStatus#UNRECOGNIZED}.
         * @return este constructor.
         */
        public Builder status(ResultStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Fija el CDC.
         *
         * @param cdc CDC.
         * @return este constructor.
         */
        public Builder cdc(String cdc) {
            this.cdc = cdc;
            return this;
        }

        /**
         * Fija el número de autorización.
         *
         * @param authorizationProtocol {@code dProtAut}.
         * @return este constructor.
         */
        public Builder authorizationProtocol(String authorizationProtocol) {
            this.authorizationProtocol = authorizationProtocol;
            return this;
        }

        /**
         * Fija la fecha de proceso.
         *
         * @param processingDate {@code dFecProc}.
         * @return este constructor.
         */
        public Builder processingDate(LocalDateTime processingDate) {
            this.processingDate = processingDate;
            return this;
        }

        /**
         * Fija el digest informado por SIFEN.
         *
         * @param digestValue {@code dDigVal}.
         * @return este constructor.
         */
        public Builder digestValue(String digestValue) {
            this.digestValue = digestValue;
            return this;
        }

        /**
         * Fija si SIFEN informó un digest distinto del del XML local.
         *
         * @param digestMismatch {@code true} si SIFEN tiene otro XML para el CDC.
         * @return este constructor.
         */
        public Builder digestMismatch(boolean digestMismatch) {
            this.digestMismatch = digestMismatch;
            return this;
        }

        /**
         * Fija si el estado salió de una consulta.
         *
         * @param alreadyRegistered {@code true} si el documento ya estaba registrado.
         * @return este constructor.
         */
        public Builder alreadyRegistered(boolean alreadyRegistered) {
            this.alreadyRegistered = alreadyRegistered;
            return this;
        }

        /**
         * Fija los mensajes de SIFEN (se conservan los primeros 100).
         *
         * @param messages mensajes; {@code null} y los elementos nulos se ignoran.
         * @return este constructor.
         */
        public Builder messages(List<SifenMessage> messages) {
            this.messages = messages;
            return this;
        }

        /**
         * Fija el id de la operación.
         *
         * @param operationId id de ocho hexadecimales.
         * @return este constructor.
         */
        public Builder operationId(String operationId) {
            this.operationId = operationId;
            return this;
        }

        /**
         * Fija la respuesta bruta.
         *
         * @param rawResponse XML de la respuesta.
         * @return este constructor.
         */
        public Builder rawResponse(String rawResponse) {
            this.rawResponse = rawResponse;
            return this;
        }

        /**
         * Fija la duración.
         *
         * @param durationMs milisegundos.
         * @return este constructor.
         */
        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        /**
         * Arma el resultado.
         *
         * @return el resultado inmutable.
         */
        public SendResult build() {
            return new SendResult(this);
        }
    }
}

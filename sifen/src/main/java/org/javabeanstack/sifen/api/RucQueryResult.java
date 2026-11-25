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
import java.util.List;

/**
 * Resultado de la consulta de un RUC.
 *
 * <p>Contrato v1 (plan JBSSIF, §10.2). Inmutable y serializable; los textos nunca son {@code null} y
 * las listas son inmodificables. Se arma con {@link #builder()}: la librería lo hace en sus operaciones
 * y la aplicación puede hacerlo en sus pruebas.</p>
 */
public final class RucQueryResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean found;
    private final String ruc;
    private final String businessName;
    private final String taxpayerStatusCode;
    private final String taxpayerStatus;
    private final boolean electronicInvoicer;
    private final List<SifenMessage> messages;
    private final String operationId;
    private final String rawResponse;
    private final long durationMs;

    private RucQueryResult(Builder b) {
        this.found = b.found;
        this.ruc = ApiValues.text(b.ruc);
        this.businessName = ApiValues.text(b.businessName);
        this.taxpayerStatusCode = ApiValues.text(b.taxpayerStatusCode);
        this.taxpayerStatus = ApiValues.text(b.taxpayerStatus);
        this.electronicInvoicer = b.electronicInvoicer;
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
     * Devuelve {@code true} si SIFEN encontró el RUC (0502); {@code false} con 0500.
     *
     * @return {@code true} si SIFEN encontró el RUC (0502); {@code false} con 0500.
     */
    public boolean isFound() {
        return found;
    }

    /**
     * Devuelve el RUC informado por SIFEN ({@code dRUCCons}), sin DV; vacío si no se encontró.
     *
     * @return el RUC informado por SIFEN ({@code dRUCCons}), sin DV; vacío si no se encontró.
     */
    public String getRuc() {
        return ruc;
    }

    /**
     * Devuelve la razón social ({@code dRazCons}); vacía si no se encontró.
     *
     * @return la razón social ({@code dRazCons}); vacía si no se encontró.
     */
    public String getBusinessName() {
        return businessName;
    }

    /**
     * Devuelve el código del estado del contribuyente ({@code dCodEstCons}: ACT, SUS…); vacío si no se encontró.
     *
     * @return el código del estado del contribuyente ({@code dCodEstCons}: ACT, SUS…); vacío si no se encontró.
     */
    public String getTaxpayerStatusCode() {
        return taxpayerStatusCode;
    }

    /**
     * Devuelve la descripción del estado del contribuyente ({@code dDesEstCons}); vacía si no se encontró.
     *
     * @return la descripción del estado del contribuyente ({@code dDesEstCons}); vacía si no se encontró.
     */
    public String getTaxpayerStatus() {
        return taxpayerStatus;
    }

    /**
     * Devuelve {@code true} si el contribuyente es facturador electrónico ({@code dRUCFactElec} = S).
     *
     * @return {@code true} si el contribuyente es facturador electrónico ({@code dRUCFactElec} = S).
     */
    public boolean isElectronicInvoicer() {
        return electronicInvoicer;
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
        return "RucQueryResult[found=" + found + ", op=" + operationId + "]";
    }

    /** Constructor de {@link RucQueryResult}. No es seguro entre hilos. */
    public static final class Builder {
        private boolean found;
        private String ruc;
        private String businessName;
        private String taxpayerStatusCode;
        private String taxpayerStatus;
        private boolean electronicInvoicer;
        private List<SifenMessage> messages;
        private String operationId;
        private String rawResponse;
        private long durationMs;

        private Builder() {
        }

        /**
         * Fija el valor de {@code found}.
         *
         * @param found si se encontró.
         * @return este constructor.
         */
        public Builder found(boolean found) {
            this.found = found;
            return this;
        }

        /**
         * Fija el valor de {@code ruc}.
         *
         * @param ruc RUC.
         * @return este constructor.
         */
        public Builder ruc(String ruc) {
            this.ruc = ruc;
            return this;
        }

        /**
         * Fija el valor de {@code businessName}.
         *
         * @param businessName razón social.
         * @return este constructor.
         */
        public Builder businessName(String businessName) {
            this.businessName = businessName;
            return this;
        }

        /**
         * Fija el valor de {@code taxpayerStatusCode}.
         *
         * @param taxpayerStatusCode código del estado.
         * @return este constructor.
         */
        public Builder taxpayerStatusCode(String taxpayerStatusCode) {
            this.taxpayerStatusCode = taxpayerStatusCode;
            return this;
        }

        /**
         * Fija el valor de {@code taxpayerStatus}.
         *
         * @param taxpayerStatus descripción del estado.
         * @return este constructor.
         */
        public Builder taxpayerStatus(String taxpayerStatus) {
            this.taxpayerStatus = taxpayerStatus;
            return this;
        }

        /**
         * Fija el valor de {@code electronicInvoicer}.
         *
         * @param electronicInvoicer si es facturador electrónico.
         * @return este constructor.
         */
        public Builder electronicInvoicer(boolean electronicInvoicer) {
            this.electronicInvoicer = electronicInvoicer;
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
        public RucQueryResult build() {
            return new RucQueryResult(this);
        }
    }
}

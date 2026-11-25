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
import org.javabeanstack.sifen.core.types.TTiDE;

/**
 * Rango de numeración a inutilizar (evento de inutilización, MT §11): timbrado, establecimiento, punto de expedición, tipo de documento, número desde y hasta (hasta 1000 números) y motivo. Solo para números que SIFEN <b>no</b> aprobó. Los valores se validan al enviarlo ({@code invalidateNumbers}), no al armarlo.
 *
 * <p>Contrato v1 (plan JBSSIF, §10.2). Inmutable y serializable; los textos nunca son {@code null} y
 * las listas son inmodificables. Se arma con {@link #builder()}: la librería lo hace en sus operaciones
 * y la aplicación puede hacerlo en sus pruebas.</p>
 */
public final class NumberRange implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String timbrado;
    private final String establishment;
    private final String issuePoint;
    private final TTiDE documentType;
    private final String from;
    private final String to;
    private final String reason;

    private NumberRange(Builder b) {
        this.timbrado = ApiValues.text(b.timbrado);
        this.establishment = ApiValues.text(b.establishment);
        this.issuePoint = ApiValues.text(b.issuePoint);
        this.documentType = b.documentType;
        this.from = ApiValues.text(b.from);
        this.to = ApiValues.text(b.to);
        this.reason = ApiValues.text(b.reason);
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
     * Devuelve el número de timbrado ({@code dNumTim}).
     *
     * @return el número de timbrado ({@code dNumTim}).
     */
    public String getTimbrado() {
        return timbrado;
    }

    /**
     * Devuelve el establecimiento ({@code dEst}, tres dígitos).
     *
     * @return el establecimiento ({@code dEst}, tres dígitos).
     */
    public String getEstablishment() {
        return establishment;
    }

    /**
     * Devuelve el punto de expedición ({@code dPunExp}, tres dígitos).
     *
     * @return el punto de expedición ({@code dPunExp}, tres dígitos).
     */
    public String getIssuePoint() {
        return issuePoint;
    }

    /**
     * Devuelve el tipo de documento ({@code iTiDE}), o {@code null} si no se indicó.
     *
     * @return el tipo de documento ({@code iTiDE}), o {@code null} si no se indicó.
     */
    public TTiDE getDocumentType() {
        return documentType;
    }

    /**
     * Devuelve el primer número del rango ({@code dNumIn}, siete dígitos).
     *
     * @return el primer número del rango ({@code dNumIn}, siete dígitos).
     */
    public String getFrom() {
        return from;
    }

    /**
     * Devuelve el último número del rango ({@code dNumFin}, siete dígitos).
     *
     * @return el último número del rango ({@code dNumFin}, siete dígitos).
     */
    public String getTo() {
        return to;
    }

    /**
     * Devuelve el motivo ({@code mOtEve}, de 5 a 500 caracteres).
     *
     * @return el motivo ({@code mOtEve}, de 5 a 500 caracteres).
     */
    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "NumberRange[timbrado=" + timbrado + "]";
    }

    /** Constructor de {@link NumberRange}. No es seguro entre hilos. */
    public static final class Builder {
        private String timbrado;
        private String establishment;
        private String issuePoint;
        private TTiDE documentType;
        private String from;
        private String to;
        private String reason;

        private Builder() {
        }

        /**
         * Fija el valor de {@code timbrado}.
         *
         * @param timbrado número de timbrado.
         * @return este constructor.
         */
        public Builder timbrado(String timbrado) {
            this.timbrado = timbrado;
            return this;
        }

        /**
         * Fija el valor de {@code establishment}.
         *
         * @param establishment establecimiento.
         * @return este constructor.
         */
        public Builder establishment(String establishment) {
            this.establishment = establishment;
            return this;
        }

        /**
         * Fija el valor de {@code issuePoint}.
         *
         * @param issuePoint punto de expedición.
         * @return este constructor.
         */
        public Builder issuePoint(String issuePoint) {
            this.issuePoint = issuePoint;
            return this;
        }

        /**
         * Fija el valor de {@code documentType}.
         *
         * @param documentType tipo de documento.
         * @return este constructor.
         */
        public Builder documentType(TTiDE documentType) {
            this.documentType = documentType;
            return this;
        }

        /**
         * Fija el valor de {@code from}.
         *
         * @param from número desde.
         * @return este constructor.
         */
        public Builder from(String from) {
            this.from = from;
            return this;
        }

        /**
         * Fija el valor de {@code to}.
         *
         * @param to número hasta.
         * @return este constructor.
         */
        public Builder to(String to) {
            this.to = to;
            return this;
        }

        /**
         * Fija el valor de {@code reason}.
         *
         * @param reason motivo.
         * @return este constructor.
         */
        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        /**
         * Arma el valor.
         *
         * @return el valor inmutable.
         */
        public NumberRange build() {
            return new NumberRange(this);
        }
    }
}

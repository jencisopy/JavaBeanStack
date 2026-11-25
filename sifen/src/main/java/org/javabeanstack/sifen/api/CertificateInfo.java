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
import java.time.Instant;
import java.util.List;

/**
 * Datos del certificado de la configuración de un cliente, para la pantalla de diagnóstico. Si el certificado no se pudo cargar, {@link #isLoaded()} es {@code false}, {@link #getFailureCode()} dice por qué ({@code JBS-CERT-*}) y las fechas son {@code null}.
 *
 * <p>Contrato v1 (plan JBSSIF, §10.2). Inmutable y serializable; los textos nunca son {@code null} y
 * las listas son inmodificables. Se arma con {@link #builder()}: la librería lo hace en sus operaciones
 * y la aplicación puede hacerlo en sus pruebas.</p>
 */
public final class CertificateInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean loaded;
    private final String failureCode;
    private final String holder;
    private final String documentNumber;
    private final List<String> rucs;
    private final Instant notBefore;
    private final Instant notAfter;
    private final long daysRemaining;
    private final boolean digitalSignature;

    private CertificateInfo(Builder b) {
        this.loaded = b.loaded;
        this.failureCode = ApiValues.text(b.failureCode);
        this.holder = ApiValues.text(b.holder);
        this.documentNumber = ApiValues.text(b.documentNumber);
        this.rucs = ApiValues.list(b.rucs);
        this.notBefore = b.notBefore;
        this.notAfter = b.notAfter;
        this.daysRemaining = b.daysRemaining;
        this.digitalSignature = b.digitalSignature;
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
     * Devuelve {@code true} si el certificado y su clave privada se cargaron.
     *
     * @return {@code true} si el certificado y su clave privada se cargaron.
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Devuelve el código de la regla que explica por qué no se cargó ({@code JBS-CERT-01}…); vacío si se cargó.
     *
     * @return el código de la regla que explica por qué no se cargó ({@code JBS-CERT-01}…); vacío si se cargó.
     */
    public String getFailureCode() {
        return failureCode;
    }

    /**
     * Devuelve el titular ({@code CN} del sujeto); vacío si no se cargó.
     *
     * @return el titular ({@code CN} del sujeto); vacío si no se cargó.
     */
    public String getHolder() {
        return holder;
    }

    /**
     * Devuelve el número de documento principal del titular ({@code SERIALNUMBER} normalizado); vacío si no lo tiene.
     *
     * @return el número de documento principal del titular ({@code SERIALNUMBER} normalizado); vacío si no lo tiene.
     */
    public String getDocumentNumber() {
        return documentNumber;
    }

    /**
     * Devuelve los RUC (con DV si el certificado lo trae) del sujeto y del SAN; lista inmodificable.
     *
     * @return los RUC (con DV si el certificado lo trae) del sujeto y del SAN; lista inmodificable.
     */
    public List<String> getRucs() {
        return rucs;
    }

    /**
     * Devuelve el inicio de la vigencia, o {@code null} si no se cargó.
     *
     * @return el inicio de la vigencia, o {@code null} si no se cargó.
     */
    public Instant getNotBefore() {
        return notBefore;
    }

    /**
     * Devuelve el fin de la vigencia, o {@code null} si no se cargó.
     *
     * @return el fin de la vigencia, o {@code null} si no se cargó.
     */
    public Instant getNotAfter() {
        return notAfter;
    }

    /**
     * Devuelve los días que faltan para el vencimiento (negativo si venció; 0 si no se cargó).
     *
     * @return los días que faltan para el vencimiento (negativo si venció; 0 si no se cargó).
     */
    public long getDaysRemaining() {
        return daysRemaining;
    }

    /**
     * Devuelve {@code true} si el uso de clave permite firmar ({@code digitalSignature}).
     *
     * @return {@code true} si el uso de clave permite firmar ({@code digitalSignature}).
     */
    public boolean isDigitalSignature() {
        return digitalSignature;
    }

    @Override
    public String toString() {
        return "CertificateInfo[loaded=" + loaded + "]";
    }

    /** Constructor de {@link CertificateInfo}. No es seguro entre hilos. */
    public static final class Builder {
        private boolean loaded;
        private String failureCode;
        private String holder;
        private String documentNumber;
        private List<String> rucs;
        private Instant notBefore;
        private Instant notAfter;
        private long daysRemaining;
        private boolean digitalSignature;

        private Builder() {
        }

        /**
         * Fija el valor de {@code loaded}.
         *
         * @param loaded si se cargó.
         * @return este constructor.
         */
        public Builder loaded(boolean loaded) {
            this.loaded = loaded;
            return this;
        }

        /**
         * Fija el valor de {@code failureCode}.
         *
         * @param failureCode código de la falla.
         * @return este constructor.
         */
        public Builder failureCode(String failureCode) {
            this.failureCode = failureCode;
            return this;
        }

        /**
         * Fija el valor de {@code holder}.
         *
         * @param holder titular.
         * @return este constructor.
         */
        public Builder holder(String holder) {
            this.holder = holder;
            return this;
        }

        /**
         * Fija el valor de {@code documentNumber}.
         *
         * @param documentNumber número de documento.
         * @return este constructor.
         */
        public Builder documentNumber(String documentNumber) {
            this.documentNumber = documentNumber;
            return this;
        }

        /**
         * Fija el valor de {@code rucs}.
         *
         * @param rucs RUC del certificado.
         * @return este constructor.
         */
        public Builder rucs(List<String> rucs) {
            this.rucs = rucs;
            return this;
        }

        /**
         * Fija el valor de {@code notBefore}.
         *
         * @param notBefore inicio de vigencia.
         * @return este constructor.
         */
        public Builder notBefore(Instant notBefore) {
            this.notBefore = notBefore;
            return this;
        }

        /**
         * Fija el valor de {@code notAfter}.
         *
         * @param notAfter fin de vigencia.
         * @return este constructor.
         */
        public Builder notAfter(Instant notAfter) {
            this.notAfter = notAfter;
            return this;
        }

        /**
         * Fija el valor de {@code daysRemaining}.
         *
         * @param daysRemaining días restantes.
         * @return este constructor.
         */
        public Builder daysRemaining(long daysRemaining) {
            this.daysRemaining = daysRemaining;
            return this;
        }

        /**
         * Fija el valor de {@code digitalSignature}.
         *
         * @param digitalSignature si sirve para firmar.
         * @return este constructor.
         */
        public Builder digitalSignature(boolean digitalSignature) {
            this.digitalSignature = digitalSignature;
            return this;
        }

        /**
         * Arma el valor.
         *
         * @return el valor inmutable.
         */
        public CertificateInfo build() {
            return new CertificateInfo(this);
        }
    }
}

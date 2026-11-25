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
import org.javabeanstack.sifen.core.exceptions.ErrorCategory;

/**
 * Resultado de verificar la firma de un XML recibido de un tercero (contrato v1, plan JBSSIF, §10.2;
 * {@code ISifenClient.verifySignature}). Inmutable y serializable.
 *
 * <p>Dos casos: un <b>veredicto</b> ({@link #isValid()} y, si no es válida, {@link #getReason()}) o una
 * <b>falla</b> de la verificación (XML vacío o mal formado, error interno): {@link #isFailure()}
 * verdadero, con {@link #getCode()}, {@link #getCategory()} y {@link #getCorrelationId()}. Un veredicto
 * puede llevar código sin ser una falla: {@code JBS-FIRMA-05} (el certificado no informa el RUC del
 * titular) trae código y categoría, pero no id de correlación. Límite
 * conocido: comprueba la integridad y el RUC del certificado, no la cadena contra las autoridades
 * habilitadas (E3-08, Fase 7).</p>
 */
public final class SignatureVerification implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean valid;
    private final String reason;
    private final List<Signer> subjects;
    private final String code;
    private final ErrorCategory category;
    private final String correlationId;

    private SignatureVerification(Builder b) {
        this.valid = b.valid;
        this.reason = ApiValues.text(b.reason);
        this.subjects = ApiValues.list(b.subjects);
        this.code = ApiValues.text(b.code);
        this.category = b.category;
        this.correlationId = ApiValues.text(b.correlationId);
    }

    /**
     * Crea un constructor.
     *
     * @return el constructor (firma no válida, sin falla).
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Indica si la firma es válida.
     *
     * @return {@code true} si la firma verifica y el certificado corresponde al emisor.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Devuelve por qué la firma no es válida.
     *
     * @return el motivo; vacío si es válida.
     */
    public String getReason() {
        return reason;
    }

    /**
     * Devuelve los titulares del certificado de la firma (número de documento y nombre).
     *
     * @return lista inmodificable; vacía si no se pudo leer el certificado.
     */
    public List<Signer> getSubjects() {
        return subjects;
    }

    /**
     * Indica si la verificación <b>falló</b> en vez de dar un veredicto: la falla pasó por el envoltorio
     * de la librería y tiene id de correlación.
     *
     * @return {@code true} si hay id de correlación.
     */
    public boolean isFailure() {
        return !correlationId.isEmpty();
    }

    /**
     * Devuelve el código de la falla ({@code JBS-XML-01}, {@code JBS-XML-03}…) o del veredicto con código
     * ({@code JBS-FIRMA-05}).
     *
     * @return el código; vacío si el veredicto no lleva código.
     */
    public String getCode() {
        return code;
    }

    /**
     * Devuelve la categoría del código.
     *
     * @return la categoría, o {@code null} si no hay código.
     */
    public ErrorCategory getCategory() {
        return category;
    }

    /**
     * Devuelve el id de correlación de la falla, para buscarla en el log.
     *
     * @return ocho caracteres hexadecimales; vacío si no hubo falla.
     */
    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public String toString() {
        return "SignatureVerification[valid=" + valid + (code.isEmpty() ? "" : ", " + code) + "]";
    }

    /** Un titular del certificado de la firma. Inmutable y serializable. */
    public static final class Signer implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String documentNumber;
        private final String name;

        private Signer(String documentNumber, String name) {
            this.documentNumber = ApiValues.text(documentNumber);
            this.name = ApiValues.text(name);
        }

        /**
         * Crea un titular.
         *
         * @param documentNumber número de documento (RUC o cédula, como lo trae el certificado).
         * @param name           nombre o razón social.
         * @return el titular.
         */
        public static Signer of(String documentNumber, String name) {
            return new Signer(documentNumber, name);
        }

        /**
         * Devuelve el número de documento del titular.
         *
         * @return el número; vacío si el certificado no lo trae.
         */
        public String getDocumentNumber() {
            return documentNumber;
        }

        /**
         * Devuelve el nombre o la razón social del titular.
         *
         * @return el nombre; vacío si el certificado no lo trae.
         */
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return documentNumber + " " + name;
        }
    }

    /** Constructor de {@link SignatureVerification}. No es seguro entre hilos. */
    public static final class Builder {
        private boolean valid;
        private String reason;
        private List<Signer> subjects;
        private String code;
        private ErrorCategory category;
        private String correlationId;

        private Builder() {
        }

        /**
         * Fija el veredicto.
         *
         * @param valid {@code true} si la firma es válida.
         * @return este constructor.
         */
        public Builder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        /**
         * Fija el motivo de invalidez.
         *
         * @param reason motivo.
         * @return este constructor.
         */
        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        /**
         * Fija los titulares del certificado.
         *
         * @param subjects titulares.
         * @return este constructor.
         */
        public Builder subjects(List<Signer> subjects) {
            this.subjects = subjects;
            return this;
        }

        /**
         * Fija el código de la falla.
         *
         * @param code código.
         * @return este constructor.
         */
        public Builder code(String code) {
            this.code = code;
            return this;
        }

        /**
         * Fija la categoría de la falla.
         *
         * @param category categoría.
         * @return este constructor.
         */
        public Builder category(ErrorCategory category) {
            this.category = category;
            return this;
        }

        /**
         * Fija el id de correlación de la falla.
         *
         * @param correlationId id.
         * @return este constructor.
         */
        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        /**
         * Arma el resultado.
         *
         * @return el valor inmutable.
         */
        public SignatureVerification build() {
            return new SignatureVerification(this);
        }
    }
}

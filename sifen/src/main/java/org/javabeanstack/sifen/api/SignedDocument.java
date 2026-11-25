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
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.types.TTiDE;

/**
 * Documento electrónico ya firmado: el {@code rDE} completo con su CDC, QR y digest (contrato v1,
 * plan JBSSIF, §10.2; lo producen {@code ISifenClient.sign} y {@code readSigned} desde §10.3).
 *
 * <p>Es lo que la aplicación <b>graba antes de enviar</b> y lo que {@code send} transmite tal cual,
 * sin regenerarlo ni volver a firmarlo. Inmutable y serializable: guarda el XML y los datos escalares;
 * {@link #getDocument()} vuelve a leer el XML en cada llamada. Los textos nunca son {@code null}.</p>
 *
 * <p>Se arma con {@link #builder()} (la librería en {@code sign}/{@code readSigned}; la aplicación en
 * sus pruebas). Un {@code SignedDocument} armado a mano no se transmite a ciegas: {@code send}
 * comprueba el XML igual que {@code readSigned}.</p>
 */
public final class SignedDocument implements Serializable {
    private static final long serialVersionUID = 1L;
    /** Plazo de transmisión desde la firma (MT v150, §6.2). */
    private static final long SUBMISSION_HOURS = 72;

    private final String xml;
    private final String cdc;
    private final String qrLink;
    private final String digestValue;
    private final LocalDateTime signatureDate;
    private final TTiDE documentType;
    private final String number;
    private final String issuerRuc;
    private final SifenConfig.Environment environment;

    private SignedDocument(Builder b) {
        this.xml = ApiValues.text(b.xml);
        this.cdc = ApiValues.text(b.cdc);
        this.qrLink = ApiValues.text(b.qrLink);
        this.digestValue = ApiValues.text(b.digestValue);
        this.signatureDate = b.signatureDate;
        this.documentType = b.documentType;
        this.number = ApiValues.text(b.number);
        this.issuerRuc = ApiValues.text(b.issuerRuc);
        this.environment = b.environment;
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
     * Devuelve el {@code rDE} firmado completo: exactamente lo que se envía.
     *
     * @return el XML (UTF-8).
     */
    public String getXml() {
        return xml;
    }

    /**
     * Devuelve el CDC (el {@code Id} del {@code DE}).
     *
     * @return el CDC de 44 dígitos.
     */
    public String getCdc() {
        return cdc;
    }

    /**
     * Devuelve la URL del QR ({@code dCarQR}) del XML firmado.
     *
     * @return la URL.
     */
    public String getQRLink() {
        return qrLink;
    }

    /**
     * Devuelve el {@code DigestValue} de la firma: lo que SIFEN devuelve como {@code dDigVal}.
     *
     * @return el digest en Base64.
     */
    public String getDigestValue() {
        return digestValue;
    }

    /**
     * Devuelve la fecha y hora de la firma ({@code dFecFirma}).
     *
     * @return la fecha, o {@code null} si no se indicó al armarlo.
     */
    public LocalDateTime getSignatureDate() {
        return signatureDate;
    }

    /**
     * Devuelve el límite para transmitir el documento: la firma más 72 horas (MT v150, §6.2). Pasado
     * ese plazo SIFEN lo aprueba con observación por extemporaneidad.
     *
     * @return la fecha límite, o {@code null} si no hay fecha de firma.
     */
    public LocalDateTime getSubmissionDeadline() {
        return signatureDate == null ? null : signatureDate.plusHours(SUBMISSION_HOURS);
    }

    /**
     * Devuelve el tipo de documento ({@code iTiDE}).
     *
     * @return el tipo, o {@code null} si no se indicó al armarlo.
     */
    public TTiDE getDocumentType() {
        return documentType;
    }

    /**
     * Devuelve el número del documento.
     *
     * @return {@code EST-PTO-NUMERO}.
     */
    public String getNumber() {
        return number;
    }

    /**
     * Devuelve el RUC del emisor.
     *
     * @return el RUC con DV.
     */
    public String getIssuerRuc() {
        return issuerRuc;
    }

    /**
     * Devuelve el ambiente de la configuración con la que se firmó.
     *
     * @return {@code DEV} o {@code PROD}, o {@code null} si no se indicó al armarlo.
     */
    public SifenConfig.Environment getEnvironment() {
        return environment;
    }

    /**
     * Devuelve el documento <b>leído del XML firmado</b> (totales, ítems y fechas para el KuDE). Cada
     * llamada lee el XML de nuevo y devuelve un objeto nuevo: cambiarlo no cambia este documento
     * firmado, que es el que se envía.
     *
     * @return el documento leído.
     * @throws SifenException {@code JBS-XML-*} si el XML no se puede leer.
     */
    public DocumentoElectronico getDocument() throws SifenException {
        return new DocumentoElectronico(xml);
    }

    @Override
    public String toString() {
        return "SignedDocument[cdc=" + cdc + ", " + documentType + " " + number + "]";
    }

    /** Constructor de {@link SignedDocument}. No es seguro entre hilos. */
    public static final class Builder {
        private String xml;
        private String cdc;
        private String qrLink;
        private String digestValue;
        private LocalDateTime signatureDate;
        private TTiDE documentType;
        private String number;
        private String issuerRuc;
        private SifenConfig.Environment environment;

        private Builder() {
        }

        /**
         * Fija el XML firmado.
         *
         * @param xml el {@code rDE} firmado.
         * @return este constructor.
         */
        public Builder xml(String xml) {
            this.xml = xml;
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
         * Fija la URL del QR.
         *
         * @param qrLink {@code dCarQR}.
         * @return este constructor.
         */
        public Builder qrLink(String qrLink) {
            this.qrLink = qrLink;
            return this;
        }

        /**
         * Fija el digest de la firma.
         *
         * @param digestValue {@code DigestValue}.
         * @return este constructor.
         */
        public Builder digestValue(String digestValue) {
            this.digestValue = digestValue;
            return this;
        }

        /**
         * Fija la fecha de firma.
         *
         * @param signatureDate {@code dFecFirma}.
         * @return este constructor.
         */
        public Builder signatureDate(LocalDateTime signatureDate) {
            this.signatureDate = signatureDate;
            return this;
        }

        /**
         * Fija el tipo de documento.
         *
         * @param documentType {@code iTiDE}.
         * @return este constructor.
         */
        public Builder documentType(TTiDE documentType) {
            this.documentType = documentType;
            return this;
        }

        /**
         * Fija el número del documento.
         *
         * @param number {@code EST-PTO-NUMERO}.
         * @return este constructor.
         */
        public Builder number(String number) {
            this.number = number;
            return this;
        }

        /**
         * Fija el RUC del emisor.
         *
         * @param issuerRuc RUC con DV.
         * @return este constructor.
         */
        public Builder issuerRuc(String issuerRuc) {
            this.issuerRuc = issuerRuc;
            return this;
        }

        /**
         * Fija el ambiente.
         *
         * @param environment ambiente de la configuración con la que se firmó.
         * @return este constructor.
         */
        public Builder environment(SifenConfig.Environment environment) {
            this.environment = environment;
            return this;
        }

        /**
         * Arma el documento firmado.
         *
         * @return el valor inmutable.
         */
        public SignedDocument build() {
            return new SignedDocument(this);
        }
    }
}

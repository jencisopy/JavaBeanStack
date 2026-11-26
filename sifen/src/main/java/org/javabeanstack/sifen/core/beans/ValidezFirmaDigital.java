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
package org.javabeanstack.sifen.core.beans;

import org.javabeanstack.sifen.core.exceptions.ErrorCategory;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.cert.CertificateSubjects;
import org.javabeanstack.sifen.internal.util.SifenUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase que representa la respuesta a la validación de la firma digital de un DE.
 *
 * <p>Desde §3.8 del plan JBSSIF (D33 Parte C): {@code validarFirmaDE*} no lanza, así que una
 * <b>falla</b> (archivo inexistente, XML vacío o mal formado, error interno) vuelve acá con
 * {@link #getCode() código}, {@link #getCategory() categoría} e {@link #getCorrelationId() id de
 * correlación}, igual que en una {@link SifenException}. Un <b>veredicto</b> (firma inválida, RUC que
 * no coincide, documento sin firma) no lleva código, salvo {@code JBS-FIRMA-05} (certificado sin
 * número de documento del titular).</p>
 *
 * <p>Deprecada en favor de {@link org.javabeanstack.sifen.ISifenClient} (D42): se mantiene sin cambios de
 * firma en 2.x y se retira en 3.0; no recibe operaciones nuevas. Su reemplazo es {@link
 * org.javabeanstack.sifen.api.SignatureVerification}.</p>
 */
public class ValidezFirmaDigital {
    private boolean esValido;
    private String motivoInvalidez;
    private String codigo;
    private ErrorCategory categoria;
    private String idCorrelacion;

    /**
     * Datos del sujeto encontrados en el certificado utilizado para la firma digital.
     */
    private List<SujetoCertificado> sujetosCertificado;

    private ValidezFirmaDigital(boolean esValido, String motivoInvalidez, List<SujetoCertificado> sujetosCertificado) {
        this.esValido = esValido;
        this.motivoInvalidez = motivoInvalidez;
        this.sujetosCertificado = sujetosCertificado;
    }

    /**
     * Método interno, no usar.
     */
    public static ValidezFirmaDigital create(boolean isValid, List<SujetoCertificado> certificateSubjects) {
        return new ValidezFirmaDigital(isValid, null, certificateSubjects);
    }

    /**
     * Método interno, no usar.
     */
    public static ValidezFirmaDigital create(boolean isValid, String invalidityReason) {
        return new ValidezFirmaDigital(isValid, invalidityReason, new ArrayList<>());
    }

    /**
     * Método interno, no usar.
     */
    public static ValidezFirmaDigital create(boolean isValid, String invalidityReason, List<SujetoCertificado> certificateSubjects) {
        return new ValidezFirmaDigital(isValid, invalidityReason, certificateSubjects);
    }

    /**
     * Resultado de una validación que no se pudo hacer (uso interno de la fachada).
     *
     * @param e error ya registrado por la fachada, con su id de correlación.
     * @return el resultado inválido con el código, la categoría, el id y el mensaje (que incluye el id).
     */
    public static ValidezFirmaDigital ofError(SifenException e) {
        ValidezFirmaDigital v = new ValidezFirmaDigital(false, e.getMessage(), new ArrayList<>());
        v.codigo = e.getCode();
        v.categoria = e.getCategory();
        v.idCorrelacion = e.getCorrelationId();
        return v;
    }

    /**
     * Veredicto con un código propio (uso interno).
     *
     * @param codigo              código del veredicto.
     * @param certificateSubjects sujetos del certificado.
     * @return el resultado inválido.
     */
    public static ValidezFirmaDigital ofCode(ErrorCode codigo, List<SujetoCertificado> certificateSubjects) {
        ValidezFirmaDigital v = new ValidezFirmaDigital(false, codigo.message(), certificateSubjects);
        v.codigo = codigo.getCode();
        v.categoria = codigo.getCategory();
        return v;
    }

    /**
     * Devuelve el código de la falla o del veredicto con código propio.
     *
     * @return el código ({@code JBS-XML-03}, {@code JBS-FIRMA-01}, {@code JBS-INT-00}…), o {@code null}
     *         si la firma es válida o el veredicto no lleva código.
     */
    public String getCode() {
        return codigo;
    }

    /**
     * Devuelve la categoría de la falla.
     *
     * @return la categoría, o {@code null} si no hay código.
     */
    public ErrorCategory getCategory() {
        return categoria;
    }

    /**
     * Devuelve el id de correlación de la falla (el mismo del log).
     *
     * @return el id, o {@code null} si no hubo falla.
     */
    public String getCorrelationId() {
        return idCorrelacion;
    }

    public static class SujetoCertificado {
        private String numeroDocumento;
        private String razonSocial;

        private SujetoCertificado(String numeroDocumento, String razonSocial) {
            this.numeroDocumento = numeroDocumento;
            this.razonSocial = razonSocial;
        }

        /**
         * Método interno, no usar.
         */
        public static SujetoCertificado create(String documentNumberParam, String registeredNameParam) {
            // I3-59: una sola normalización para la firma y para JBS-CERT-11 (la del fork, con el DV calculado)
            String documentNumber = CertificateSubjects.normalizeDocumentNumber(documentNumberParam), registeredName = null;

            if (SifenUtil.isNotBlank(registeredNameParam)) {
                registeredName = registeredNameParam.trim();
            }

            return new SujetoCertificado(documentNumber, registeredName);
        }

        public String getNumeroDocumento() {
            return numeroDocumento;
        }

        public void setNumeroDocumento(String numeroDocumento) {
            this.numeroDocumento = numeroDocumento;
        }

        public String getRazonSocial() {
            return razonSocial;
        }

        public void setRazonSocial(String razonSocial) {
            this.razonSocial = razonSocial;
        }
    }

    public boolean isValido() {
        return esValido;
    }

    public void setEsValido(boolean esValido) {
        this.esValido = esValido;
    }

    public String getMotivoInvalidez() {
        return motivoInvalidez;
    }

    public void setMotivoInvalidez(String motivoInvalidez) {
        this.motivoInvalidez = motivoInvalidez;
    }

    public List<SujetoCertificado> getSujetosCertificado() {
        return sujetosCertificado;
    }

    public void setSujetosCertificado(List<SujetoCertificado> sujetosCertificado) {
        this.sujetosCertificado = sujetosCertificado;
    }
}

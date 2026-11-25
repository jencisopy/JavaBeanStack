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
package org.javabeanstack.sifen.core.validation.support;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Resultado de intentar cargar el certificado de la configuración (plan JBSSIF, §3.7, D33 Parte B):
 * los <b>metadatos</b> del certificado, o la causa por la que no se pudo cargar. Es el sujeto de las
 * reglas {@code JBS-CERT-*}; la carga nunca lanza, las reglas leen la {@link Cause}.
 *
 * <p>No guarda la contraseña ni el material de la clave (eso queda en el almacén interno de la
 * librería) y su {@link #toString()} no muestra el contenido Base64 de un certificado en línea.
 * Inmutable salvo la marca de cambio, que se consume una vez ({@link #consumeChange()}).</p>
 */
public final class LoadedCertificate {

    /** Por qué no se cargó el certificado ({@link #NONE} si se cargó). */
    public enum Cause {
        /** Se cargó: hay certificado y clave privada. */
        NONE(null),
        /** No se indicó el certificado ({@code JBS-CERT-01}). */
        MISSING("JBS-CERT-01"),
        /** El archivo no existe ({@code JBS-CERT-01}). */
        NOT_FOUND("JBS-CERT-01"),
        /** Es un directorio o no se puede leer ({@code JBS-CERT-02}). */
        UNREADABLE("JBS-CERT-02"),
        /** La contraseña no abre el almacén o la clave ({@code JBS-CERT-03}). */
        WRONG_PASSWORD("JBS-CERT-03"),
        /** Falta la contraseña de un PFX ({@code JBS-CERT-04}). */
        NO_PASSWORD("JBS-CERT-04"),
        /** El contenido no es un PKCS#12 ({@code JBS-CERT-05}). */
        NOT_PKCS12("JBS-CERT-05"),
        /** El almacén no tiene ninguna entrada con clave privada ({@code JBS-CERT-06}). */
        NO_PRIVATE_KEY("JBS-CERT-06"),
        /** Tipo de certificado no indicado o no soportado ({@code JBS-CERT-13}). */
        UNSUPPORTED_TYPE("JBS-CERT-13"),
        /** El valor no es un archivo existente ni un contenido Base64 válido ({@code JBS-CERT-14}). */
        INVALID_BASE64("JBS-CERT-14");

        private final String codigo;

        Cause(String codigo) {
            this.codigo = codigo;
        }

        /**
         * Devuelve el código de la regla que informa la causa.
         *
         * @return el código ({@code JBS-CERT-<nn>}), o {@code null} para {@link #NONE}.
         */
        public String getCode() {
            return codigo;
        }
    }

    private final Cause causa;
    private final String ruta;
    private final boolean enLinea;
    private final int cantidadAlias;
    private final X509Certificate certificado;
    private final String numeroDocumento;
    private final String titular;
    private final AtomicBoolean cambio;

    private LoadedCertificate(Cause causa, String ruta, boolean enLinea, int cantidadAlias, X509Certificate certificado,
            String numeroDocumento, String titular, boolean cambio) {
        this.causa = causa;
        this.ruta = ruta;
        this.enLinea = enLinea;
        this.cantidadAlias = cantidadAlias;
        this.certificado = certificado;
        this.numeroDocumento = numeroDocumento;
        this.titular = titular;
        this.cambio = new AtomicBoolean(cambio);
    }

    /**
     * Uso interno: un intento de carga fallido.
     *
     * @param causa   la causa (distinta de {@link Cause#NONE}).
     * @param ruta    ruta del archivo, o {@code null} si el certificado viene en línea o falta.
     * @param enLinea {@code true} si el valor de la configuración es el contenido Base64.
     * @return el resultado.
     */
    public static LoadedCertificate failed(Cause causa, String ruta, boolean enLinea) {
        return new LoadedCertificate(causa, ruta, enLinea, 0, null, null, null, false);
    }

    /**
     * Uso interno: un certificado cargado.
     *
     * @param ruta            ruta del archivo, o {@code null} si viene en línea.
     * @param enLinea         {@code true} si viene en línea.
     * @param cantidadAlias   entradas del almacén.
     * @param certificado     el certificado de la entrada que se usa.
     * @param numeroDocumento número de documento del sujeto sin prefijo ({@code 80123456-5}), o {@code null}.
     * @param titular         nombre del sujeto ({@code CN} u {@code O}), o {@code null}.
     * @param cambio          {@code true} si reemplaza a otra carga del mismo archivo que cambió.
     * @return el resultado.
     */
    public static LoadedCertificate loaded(String ruta, boolean enLinea, int cantidadAlias, X509Certificate certificado,
            String numeroDocumento, String titular, boolean cambio) {
        return new LoadedCertificate(Cause.NONE, ruta, enLinea, cantidadAlias, certificado, numeroDocumento, titular,
                cambio);
    }

    /**
     * Devuelve la causa por la que no se cargó.
     *
     * @return la causa; {@link Cause#NONE} si se cargó.
     */
    public Cause getCause() {
        return causa;
    }

    /**
     * Indica si se cargó el certificado con su clave privada.
     *
     * @return {@code true} si se cargó.
     */
    public boolean isLoaded() {
        return causa == Cause.NONE;
    }

    /**
     * Devuelve la ruta del archivo.
     *
     * @return la ruta, o {@code null} si el certificado viene en línea o falta.
     */
    public String getPath() {
        return ruta;
    }

    /**
     * Indica si el certificado viene en línea (Base64 en la configuración) en vez de en un archivo.
     *
     * @return {@code true} si viene en línea.
     */
    public boolean isInline() {
        return enLinea;
    }

    /**
     * Devuelve la cantidad de entradas del almacén (se usa la primera con clave privada).
     *
     * @return la cantidad; 0 si no se cargó.
     */
    public int getAliasCount() {
        return cantidadAlias;
    }

    /**
     * Devuelve el certificado X.509 que se usa para firmar y para el TLS.
     *
     * @return el certificado, o {@code null} si no se cargó.
     */
    public X509Certificate getCertificate() {
        return certificado;
    }

    /**
     * Devuelve el inicio de la vigencia.
     *
     * @return el instante, o {@code null} si no se cargó.
     */
    public Instant getNotBefore() {
        return certificado == null ? null : certificado.getNotBefore().toInstant();
    }

    /**
     * Devuelve el fin de la vigencia.
     *
     * @return el instante, o {@code null} si no se cargó.
     */
    public Instant getNotAfter() {
        return certificado == null ? null : certificado.getNotAfter().toInstant();
    }

    /**
     * Devuelve el <b>primer</b> número de documento del certificado ({@code SERIALNUMBER} del sujeto o, si
     * falta, del primer {@code dirName} del SAN, sin el prefijo {@code RUC}/{@code CI}).
     *
     * <p>E3-32: un certificado puede traer varios (el de una persona física apoderada lleva la CI del
     * titular en el sujeto y el RUC de la empresa en el SAN); para comparar con el emisor usar
     * {@link #matchesIssuer(String, String)} o {@link #getDocumentNumbers()}.</p>
     *
     * @return el número ({@code 80123456-5}), o {@code null} si el certificado no lo trae.
     */
    public String getDocumentNumber() {
        return numeroDocumento;
    }

    /**
     * Devuelve todos los números de documento del certificado, sin prefijo, con DV (calculado si no lo
     * trae) y sin repetir: el {@code SERIALNUMBER} del sujeto y el de cada {@code dirName} del SAN (E3-32;
     * normalización del fork, I3-59).
     *
     * @return lista inmodificable; vacía si no se cargó o no trae ninguno.
     */
    public java.util.List<String> getDocumentNumbers() {
        java.util.LinkedHashSet<String> l = new java.util.LinkedHashSet<>();
        if (certificado != null) {
            for (org.javabeanstack.sifen.internal.cert.CertificateSubjects.CertificateSubject s
                    : org.javabeanstack.sifen.internal.cert.CertificateSubjects.of(certificado)) {
                String n = org.javabeanstack.sifen.internal.cert.CertificateSubjects.normalizeDocumentNumber(s.documentNumber());
                if (n != null) {
                    l.add(n);
                }
            }
        }
        return java.util.List.copyOf(l);
    }

    /**
     * Devuelve los RUC del certificado (los {@code SERIALNUMBER} con prefijo {@code RUC}, del sujeto o del
     * SAN), sin prefijo. A diferencia de una CI, el RUC de una empresa no es un dato personal y se puede
     * mostrar en un mensaje (E3-32).
     *
     * @return lista inmodificable; vacía si no trae ninguno.
     */
    public java.util.List<String> getRucs() {
        java.util.LinkedHashSet<String> l = new java.util.LinkedHashSet<>();
        if (certificado != null) {
            for (org.javabeanstack.sifen.internal.cert.CertificateSubjects.CertificateSubject s
                    : org.javabeanstack.sifen.internal.cert.CertificateSubjects.of(certificado)) {
                String n = org.javabeanstack.sifen.internal.cert.CertificateSubjects.normalizeDocumentNumber(s.documentNumber());
                if (n != null && s.documentNumber().startsWith("RUC")) { // I3-59: el prefijo del fork, con mayúsculas
                    l.add(n);
                }
            }
        }
        return java.util.List.copyOf(l);
    }

    /**
     * Indica si el emisor del documento es alguno de los sujetos del certificado: el {@code SERIALNUMBER}
     * del sujeto <b>o</b> el de cualquier {@code dirName} del SAN, comparado con RUC y DV (la semántica de
     * {@code SignatureHelper.checkDocumentIssuer} del fork, que SIFEN valida; E3-32, I3-59, null-safe como E0-07).
     *
     * @param ruc RUC del emisor ({@code dRucEm}).
     * @param dv  dígito verificador ({@code dDVEmi}); puede ser nulo.
     * @return {@code true} si alguno coincide.
     */
    public boolean matchesIssuer(String ruc, String dv) {
        for (String n : getDocumentNumbers()) {
            if (org.javabeanstack.sifen.internal.cert.CertificateSubjects.matchesIssuer(n, ruc, dv)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Devuelve el nombre del sujeto.
     *
     * @return el {@code CN} (o el {@code O}), o {@code null}.
     */
    public String getHolder() {
        return titular;
    }

    /**
     * Indica si el uso de clave admite la firma digital (sin extensión {@code keyUsage}, sí).
     *
     * @return {@code true} si admite firmar; {@code false} si no se cargó.
     */
    public boolean isDigitalSignature() {
        if (certificado == null) {
            return false;
        }
        boolean[] uso = certificado.getKeyUsage();
        return uso == null || (uso.length > 0 && uso[0]);
    }

    /**
     * Uso interno ({@code JBS-CERT-15}): indica, una sola vez, que esta carga reemplazó a otra del mismo
     * archivo porque el archivo cambió.
     *
     * @return {@code true} la primera vez que se consulta tras un cambio.
     */
    public boolean consumeChange() {
        return cambio.getAndSet(false);
    }

    @Override
    public String toString() {
        return "LoadedCertificate{causa=" + causa + (enLinea ? ", en línea" : ", ruta=" + ruta)
                + (certificado == null ? "" : ", alias=" + cantidadAlias + ", rucs=" + getRucs()
                + ", vigencia=" + getNotBefore() + "→" + getNotAfter()) + '}';
    }
}

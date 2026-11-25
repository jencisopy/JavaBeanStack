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
package org.javabeanstack.sifen.core.exceptions;

/**
 * Error de conexión con SIFEN (categoría {@link ErrorCategory#CONNECTION}, códigos {@code JBS-CONEX-*}).
 *
 * <p>Plan JBSSIF, §3.8 (D33 Parte C): la clase entra ahora como subclase del modelo de errores, con
 * el {@link Type} de RF12; la Fase 5 (§5.4) la completa con los casos del laboratorio de §5.1
 * (proxy con autenticación, cambio de certificado del servidor) y la traza activable. La relación
 * código ↔ tipo no es uno a uno (I3-30): {@code JBS-CONEX-06} cubre {@code TLS} y
 * {@code CERTIFICATE}, {@code JBS-CONEX-07} es un corte {@code TCP} y el {@code SOAP_FAULT} no es de
 * conexión sino {@code JBS-RESP-01}.</p>
 *
 * <p>Fase 5 (§5.4, D37): el código {@code JBS-CONEX-nn} es el contrato y el {@link Type} es la
 * clasificación gruesa, muchos a uno. Dentro de {@code JBS-CONEX-06} el tipo separa
 * {@link Type#CERTIFICATE} (el servidor rechazó el certificado de cliente: alerta TLS
 * {@code bad_certificate}, {@code certificate_required}, {@code unknown_ca}…) de {@link Type#TLS}
 * (el resto, p. ej. el certificado del servidor no es de confianza), mirando la cadena de causas.
 * Argumentos del mensaje que pasa el transporte: {@code {0}} = servidor en {@code JBS-CONEX-01/03},
 * {@code host:puerto} del proxy en {@code JBS-CONEX-02}, segundos de espera en {@code JBS-CONEX-04/05}
 * y estado HTTP en {@code JBS-CONEX-08/09/10} (los textos que no usan el argumento lo ignoran).</p>
 */
public class SifenConnectionException extends SifenException {
    private static final long serialVersionUID = 1L;

    /** Tipo de falla de conexión (RF12). */
    public enum Type {
        /** El proxy no responde o rechaza. */
        PROXY,
        /** El nombre no se resuelve. */
        DNS,
        /** Conexión TCP rechazada, sin ruta o cortada. */
        TCP,
        /** Falla del TLS. */
        TLS,
        /** Certificado de cliente rechazado en el TLS. */
        CERTIFICATE,
        /** Tiempo agotado al conectar. */
        CONNECT_TIMEOUT,
        /** Tiempo agotado esperando la respuesta. */
        READ_TIMEOUT,
        /** HTTP 5xx. */
        HTTP_5XX,
        /** HTTP 4xx. */
        HTTP_4XX,
        /** SOAP Fault (no lo emite esta clase: es {@code JBS-RESP-01}; se conserva por RF12). */
        SOAP_FAULT
    }

    private final Type tipo;

    SifenConnectionException(ErrorCode codigo, String detalle, Throwable causa, Object... args) {
        super(codigo.getCode(), ErrorCategory.CONNECTION, codigo.message(args), detalle, null, null, causa);
        this.tipo = typeOf(codigo, causa);
        // §10.7 (D43, I5-08): el constructor ya no marca JBS-CONEX-05/07 como enviados. Si el documento pudo
        // haber llegado lo decide el transporte (los bytes escritos): un corte o un tiempo agotado en el
        // saludo TLS no envió nada. Sin transporte que lo sepa, Operations.translate lo marca (en la duda, sí).
    }

    /**
     * Devuelve el tipo de falla.
     *
     * @return el tipo.
     */
    public Type getType() {
        return tipo;
    }

    /**
     * Tipo que corresponde a un código de conexión.
     *
     * @param codigo código {@code JBS-CONEX-*}.
     * @return el tipo.
     */
    public static Type typeOf(ErrorCode codigo) {
        return typeOf(codigo, null);
    }

    /**
     * Tipo que corresponde a un código de conexión y su causa (§5.4): un {@code JBS-CONEX-06} cuyo
     * origen es una alerta TLS de rechazo del certificado de cliente es {@link Type#CERTIFICATE}.
     *
     * @param codigo código {@code JBS-CONEX-*}.
     * @param causa  excepción original; puede ser {@code null}.
     * @return el tipo.
     */
    public static Type typeOf(ErrorCode codigo, Throwable causa) {
        if (codigo == ErrorCode.JBS_CONEX_06 && certificadoRechazado(causa)) {
            return Type.CERTIFICATE;
        }
        switch (codigo) {
            case JBS_CONEX_01:
                return Type.DNS;
            case JBS_CONEX_02:
            case JBS_CONEX_11:
                return Type.PROXY;
            case JBS_CONEX_04:
                return Type.CONNECT_TIMEOUT;
            case JBS_CONEX_05:
                return Type.READ_TIMEOUT;
            case JBS_CONEX_06:
                return Type.TLS;
            case JBS_CONEX_08:
                return Type.HTTP_4XX;
            case JBS_CONEX_09:
                return Type.HTTP_5XX;
            case JBS_CONEX_10:
                return Type.HTTP_4XX;
            default:
                return Type.TCP;
        }
    }

    /** Alertas TLS con las que un servidor rechaza el certificado de cliente (RFC 8446 §6.2). */
    private static final java.util.List<String> ALERTAS_CERTIFICADO = java.util.List.of("bad_certificate", "certificate_required", "unknown_ca",
        "certificate_unknown", "certificate_revoked", "certificate_expired", "unsupported_certificate", "access_denied");

    private static boolean certificadoRechazado(Throwable causa) {
        int n = 0;
        for (Throwable t = causa; t != null && n < 10; t = t.getCause() == t ? null : t.getCause(), n++) {
            String m = t.getMessage();
            if (t instanceof javax.net.ssl.SSLException && m != null && m.contains("Received fatal alert")) {
                for (String alerta : ALERTAS_CERTIFICADO) {
                    if (m.contains(alerta)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

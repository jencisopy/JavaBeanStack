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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Códigos {@code iNNN} de la línea base.
 *
 * @deprecated desde §3.8 del plan JBSSIF (D33 Parte C): los errores llevan un código estable
 * {@code JBS-<CAT>-<nn>} ({@link ErrorCode} o una regla del catálogo) y una {@link ErrorCategory}; el
 * {@code iNNN} queda en {@link SifenException#getLegacyCode()}. {@link #INVALID_DE_CODE}
 * ({@code i405}) e {@link #INVALID_CONFIGURATION_CODE} ({@code i101}) son el
 * {@code getLegacyCode()} de {@code SifenValidationException} (contrato de §3.5, A9); su
 * {@code getCode()} devuelve el código de la primera regla con error (D42, §11.4). El mapa
 * {@link #newCodes(String)} parte de lo que la línea base <b>emitía de hecho</b> (I3-22):
 * {@code i200} e {@code i404} existían como constantes pero no se construían nunca (el contexto TLS
 * y el certificado salían como {@code i100}; {@code invalidSignatureError(String)} como {@code i403}).
 */
@Deprecated
public class SifenExceptionCodes {
    public static final String UNSET_ERROR_MESSAGE = "No hay mensaje de error";

    public static final String UNEXPECTED_ERROR_CODE = "i100";
    public static final String UNEXPECTED_ERROR_MESSAGE = "Error Inesperado";
    public static final String INVALID_CONFIGURATION_CODE = "i101";
    public static final String INVALID_CONFIGURATION_MESSAGE = "Configuración inválida";
    public static final String INVALID_SSL_CONTEXT_CODE = "i200";
    public static final String INVALID_SSL_CONTEXT_MESSAGE = "Contexto SSL Inválido";
    public static final String INVALID_SOAP_REQUEST_CODE = "i300";
    public static final String INVALID_SOAP_REQUEST_MESSAGE = "Llamada SOAP inválida";
    public static final String INVALID_SOAP_RESPONSE_CODE = "i301";
    public static final String INVALID_SOAP_RESPONSE_MESSAGE = "Respuesta SOAP inválida";
    public static final String REQUEST_PREPARATION_ERROR_CODE = "i400";
    public static final String REQUEST_PREPARATION_ERROR_MESSAGE = "Error al preparar el cuerpo de la petición";
    public static final String REQUEST_SIGNING_ERROR_CODE = "i401";
    public static final String REQUEST_SIGNING_ERROR_MESSAGE = "Error al firmar la petición";
    public static final String FIELD_NOT_FOUND_CODE = "i402";
    public static final String FIELD_NOT_FOUND_MESSAGE = "Campo no encontrado";
    public static final String XML_PARSING_ERROR_CODE = "i403";
    public static final String XML_PARSING_ERROR_MESSAGE = "Error al parsear el XML";
    public static final String INVALID_SIGNATURE_ERROR_CODE = "i404";
    public static final String INVALID_SIGNATURE_ERROR_MESSAGE = "La firma del Documento Electrónico es inválida";
    /** El documento no superó la validación previa (plan JBSSIF, §3.2); lo lleva {@code SifenValidationException}. */
    public static final String INVALID_DE_CODE = "i405";
    public static final String INVALID_DE_MESSAGE = "El documento no superó la validación previa";

    private static final Map<String, String> errorMessages;

    static {
        // §10.7 (I10-20): mapas armados una vez e inmodificables, sin estado estático mutable
        Map<String, String> errorMessagesLocal = new HashMap<>();
        errorMessagesLocal.put(UNEXPECTED_ERROR_CODE, UNEXPECTED_ERROR_MESSAGE);
        errorMessagesLocal.put(INVALID_CONFIGURATION_CODE, INVALID_CONFIGURATION_MESSAGE);
        errorMessagesLocal.put(INVALID_SSL_CONTEXT_CODE, INVALID_SSL_CONTEXT_MESSAGE);
        errorMessagesLocal.put(INVALID_SOAP_REQUEST_CODE, INVALID_SOAP_REQUEST_MESSAGE);
        errorMessagesLocal.put(INVALID_SOAP_RESPONSE_CODE, INVALID_SOAP_RESPONSE_MESSAGE);
        errorMessagesLocal.put(REQUEST_PREPARATION_ERROR_CODE, REQUEST_PREPARATION_ERROR_MESSAGE);
        errorMessagesLocal.put(REQUEST_SIGNING_ERROR_CODE, REQUEST_SIGNING_ERROR_MESSAGE);
        errorMessagesLocal.put(FIELD_NOT_FOUND_CODE, FIELD_NOT_FOUND_MESSAGE);
        errorMessagesLocal.put(XML_PARSING_ERROR_CODE, XML_PARSING_ERROR_MESSAGE);
        errorMessagesLocal.put(INVALID_SIGNATURE_ERROR_CODE, INVALID_SIGNATURE_ERROR_MESSAGE);
        errorMessagesLocal.put(INVALID_DE_CODE, INVALID_DE_MESSAGE);
        errorMessages = Collections.unmodifiableMap(errorMessagesLocal);
    }

    /** iNNN emitido por la línea base → códigos nuevos que lo reemplazan (I3-22: lo emitido, no lo declarado). */
    private static final Map<String, List<String>> CODIGOS_NUEVOS;

    static {
        Map<String, List<String>> codigosNuevos = new LinkedHashMap<>();
        codigosNuevos.put(UNEXPECTED_ERROR_CODE, List.of("JBS-INT-00", "JBS-CERT-01", "JBS-CERT-02", "JBS-CERT-03",
                "JBS-CERT-05", "JBS-CERT-06", "JBS-CERT-14", "JBS-CONEX-06"));
        codigosNuevos.put(INVALID_CONFIGURATION_CODE, List.of("JBS-CFG-01", "JBS-CFG-02", "JBS-CFG-03", "JBS-CFG-04",
                "JBS-CFG-05", "JBS-CFG-06", "JBS-CFG-07", "JBS-CFG-08", "JBS-CFG-09", "JBS-CFG-10", "JBS-CFG-11",
                "JBS-CFG-12", "JBS-CFG-13", "JBS-CFG-14", "JBS-CERT-04", "JBS-CERT-13"));
        codigosNuevos.put(INVALID_SSL_CONTEXT_CODE, List.of());
        codigosNuevos.put(INVALID_SOAP_REQUEST_CODE, List.of("JBS-CONEX-01", "JBS-CONEX-02", "JBS-CONEX-03",
                "JBS-CONEX-04", "JBS-CONEX-05", "JBS-CONEX-06", "JBS-CONEX-07", "JBS-CONEX-10", "JBS-CFG-03"));
        codigosNuevos.put(INVALID_SOAP_RESPONSE_CODE, List.of("JBS-RESP-01", "JBS-RESP-02", "JBS-RESP-03"));
        codigosNuevos.put(REQUEST_PREPARATION_ERROR_CODE, List.of("JBS-INT-01"));
        codigosNuevos.put(REQUEST_SIGNING_ERROR_CODE, List.of("JBS-FIRMA-02", "JBS-FIRMA-03"));
        codigosNuevos.put(FIELD_NOT_FOUND_CODE, List.of("JBS-XML-04", "JBS-INT-00"));
        codigosNuevos.put(XML_PARSING_ERROR_CODE, List.of("JBS-XML-03"));
        codigosNuevos.put(INVALID_SIGNATURE_ERROR_CODE, List.of());
        codigosNuevos.put(INVALID_DE_CODE, List.of("(código de la primera regla con error del DE o del evento)"));
        CODIGOS_NUEVOS = Collections.unmodifiableMap(codigosNuevos);
    }

    /**
     * Devuelve los códigos nuevos que reemplazan a un {@code iNNN} de la línea base.
     *
     * @param codigoLegado código {@code iNNN}.
     * @return los códigos {@code JBS-*} (vacío para {@code i200} e {@code i404}, que no se emitían, o
     *         para un código desconocido).
     */
    public static List<String> newCodes(String codigoLegado) {
        List<String> l = CODIGOS_NUEVOS.get(codigoLegado);
        return l == null ? Collections.emptyList() : l;
    }

    /**
     * Categoría que corresponde a un {@code iNNN} de la línea base (lo usa {@link SifenException}
     * para una excepción construida con el código heredado).
     *
     * @param codigoLegado código {@code iNNN}.
     * @return la categoría.
     */
    static ErrorCategory categoriaLegado(String codigoLegado) {
        if (codigoLegado == null) {
            return ErrorCategory.INTERNAL;
        }
        switch (codigoLegado) {
            case INVALID_CONFIGURATION_CODE:
                return ErrorCategory.CONFIGURATION;
            case INVALID_SSL_CONTEXT_CODE:
                return ErrorCategory.CERTIFICATE;
            case INVALID_SOAP_REQUEST_CODE:
                return ErrorCategory.CONNECTION;
            case INVALID_SOAP_RESPONSE_CODE:
                return ErrorCategory.SIFEN_RESPONSE;
            case FIELD_NOT_FOUND_CODE:
            case XML_PARSING_ERROR_CODE:
                return ErrorCategory.INPUT;
            case INVALID_SIGNATURE_ERROR_CODE:
                return ErrorCategory.CERTIFICATE;
            case INVALID_DE_CODE:
                return ErrorCategory.DE_CONTENT;
            default:
                return ErrorCategory.INTERNAL;
        }
    }

    public static String getErrorMessage(String code) {
        if (errorMessages.containsKey(code)) {
            return errorMessages.get(code);
        }
        return UNSET_ERROR_MESSAGE;
    }
}
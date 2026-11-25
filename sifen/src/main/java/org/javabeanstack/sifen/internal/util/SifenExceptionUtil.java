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
package org.javabeanstack.sifen.internal.util;

import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.exceptions.SifenExceptionCodes;

/**
 * Util encargado de crear las excepciones (códigos {@code iNNN} de la línea base).
 *
 * @deprecated desde §3.8 del plan JBSSIF (D33 Parte C): la librería ya no las usa; los errores se
 * crean con {@code SifenException.of(ErrorCode, …)} o con el código de su regla. Se conserva sin
 * cambios para quien la hubiera usado (I3-22: {@code invalidSSLContext} emite {@code i100} y
 * {@code invalidSignatureError(String)} emite {@code i403}, como en la línea base).
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("deprecation")
public class SifenExceptionUtil {
    public static SifenException invalidConfiguration(String message) {
        return SifenExceptionUtil.invalidConfiguration(message, null);
    }

    public static SifenException invalidConfiguration(String message, Throwable t) {
        return new SifenException(
                SifenExceptionCodes.INVALID_CONFIGURATION_CODE,
                message,
                t
        );
    }

    public static SifenException invalidSSLContext(String message, Throwable t) {
        return new SifenException(
                SifenExceptionCodes.UNEXPECTED_ERROR_CODE,
                message,
                t
        );
    }

    public static SifenException invalidSOAPRequest(String message) {
        return SifenExceptionUtil.invalidSOAPRequest(message, null);
    }

    public static SifenException invalidSOAPRequest(String message, Throwable t) {
        return new SifenException(
                SifenExceptionCodes.INVALID_SOAP_REQUEST_CODE,
                message,
                t
        );
    }

    public static SifenException invalidSOAPResponse(String message) {
        return invalidSOAPResponse(message, null);
    }

    public static SifenException invalidSOAPResponse(String message, Throwable t) {
        return new SifenException(
                SifenExceptionCodes.INVALID_SOAP_RESPONSE_CODE,
                message,
                t
        );
    }

    public static SifenException unexpectedError(String message, Throwable t) {
        return new SifenException(
                SifenExceptionCodes.UNEXPECTED_ERROR_CODE,
                message,
                t
        );
    }

    public static SifenException requestPreparationError(String message, Throwable t) {
        return new SifenException(
                SifenExceptionCodes.REQUEST_PREPARATION_ERROR_CODE,
                message,
                t
        );
    }

    public static SifenException requestSigningError(String message, Throwable t) {
        return new SifenException(
                SifenExceptionCodes.REQUEST_SIGNING_ERROR_CODE,
                message,
                t
        );
    }

    public static SifenException fieldNotFound(String message) {
        return fieldNotFound(message, null);
    }

    public static SifenException fieldNotFound(String message, Throwable t) {
        return new SifenException(
                SifenExceptionCodes.FIELD_NOT_FOUND_CODE,
                message,
                t
        );
    }

    public static SifenException xmlParsingError(String message) {
        return xmlParsingError(message, null);
    }

    public static SifenException xmlParsingError(String message, Throwable t) {
        return new SifenException(
                SifenExceptionCodes.XML_PARSING_ERROR_CODE,
                message,
                t
        );
    }

    public static SifenException invalidSignatureError(String message) {
        return xmlParsingError(message, null);
    }

    public static SifenException invalidSignatureError(String message, Throwable t) {
        return new SifenException(
                SifenExceptionCodes.INVALID_SIGNATURE_ERROR_CODE,
                message,
                t
        );
    }
}
/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
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
package org.javabeanstack.web.rest.security;

import org.javabeanstack.web.rest.model.SessionCredential;

/**
 * Reglas CSRF de los recursos REST, compartidas por el filtro
 * {@link SessionRequiredFilter} y por {@code SessionWebResource}.
 *
 * <p>La protección solo aplica a una credencial que viajó en la <b>cookie</b>
 * y a un método que <b>muta</b>: el navegador manda la cookie solo, mientras
 * que un encabezado {@code Authorization} lo tuvo que escribir el cliente a
 * propósito y no puede salir de otro sitio.</p>
 */
public final class CsrfRules {

    /** Encabezado que acompaña toda petición mutadora hecha por cookie. */
    public static final String CSRF_HEADER = "X-Requested-With";
    /** Valor esperado del encabezado. */
    public static final String CSRF_HEADER_VALUE = "JavaBeanStack";

    private CsrfRules() {
    }

    /**
     * Indica si la petición necesita el encabezado CSRF: credencial por cookie
     * y método mutador.
     *
     * @param httpMethod método HTTP.
     * @param credential credencial resuelta.
     * @return verdadero si hay que exigir el encabezado.
     */
    public static boolean isCsrfProtectionRequired(String httpMethod, SessionCredential credential) {
        if (credential == null || !credential.isFromCookie()) {
            return false;
        }
        return isMutation(httpMethod);
    }

    /**
     * Indica si el método HTTP modifica estado. Un método nulo se trata como
     * mutador: ante la duda se exige la protección.
     *
     * @param httpMethod método HTTP.
     * @return verdadero salvo GET, HEAD, OPTIONS y TRACE.
     */
    public static boolean isMutation(String httpMethod) {
        if (httpMethod == null) {
            return true;
        }
        String method = httpMethod.trim().toUpperCase();
        return !("GET".equals(method)
                || "HEAD".equals(method)
                || "OPTIONS".equals(method)
                || "TRACE".equals(method));
    }

    /**
     * Compara el encabezado recibido con el valor esperado.
     *
     * @param headerValue valor recibido.
     * @param expectedValue valor esperado.
     * @return verdadero si coinciden exactamente.
     */
    public static boolean isCsrfHeaderValid(String headerValue, String expectedValue) {
        if (headerValue == null || expectedValue == null) {
            return false;
        }
        return expectedValue.equals(headerValue);
    }

    /**
     * Evalúa la petición completa: si la protección aplica, el encabezado tiene
     * que valer lo esperado.
     *
     * @param httpMethod método HTTP.
     * @param credential credencial resuelta.
     * @param headerValue valor recibido del encabezado CSRF.
     * @param expectedValue valor esperado.
     * @return verdadero si la petición es segura.
     */
    public static boolean isCsrfSafe(String httpMethod, SessionCredential credential,
            String headerValue, String expectedValue) {
        if (!isCsrfProtectionRequired(httpMethod, credential)) {
            return true;
        }
        return isCsrfHeaderValid(headerValue, expectedValue);
    }
}

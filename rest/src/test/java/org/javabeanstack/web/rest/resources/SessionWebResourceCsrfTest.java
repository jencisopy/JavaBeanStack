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
package org.javabeanstack.web.rest.resources;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import org.javabeanstack.web.rest.model.SessionCredential;

/**
 * Pruebas de la regla anti CSRF, en su parte decidible sin contenedor.
 *
 * <p>La regla tiene dos condiciones y las dos importan: se exige el encabezado
 * propio solo cuando la petición <b>modifica</b> datos y llegó autenticada
 * <b>por cookie</b>. Si alcanzara a las peticiones autenticadas por
 * {@code Authorization} rompería a los clientes de integración sin ganar nada;
 * si no alcanzara a las de cookie, cualquier sitio podría disparar operaciones
 * en nombre del usuario logueado.</p>
 *
 * @author Jorge Enciso
 */
public class SessionWebResourceCsrfTest {

    private static final SessionCredential COOKIE = SessionCredential.fromCookie("sess-1");
    private static final SessionCredential HEADER = SessionCredential.fromAuthHeader("Bearer sess-1");
    private static final SessionCredential SIN_CREDENCIAL = SessionCredential.EMPTY;

    @Test
    public void testMutacionPorCookieExigeElEncabezado() {
        assertTrue(SessionWebResource.isCsrfProtectionRequired("POST", COOKIE));
        assertTrue(SessionWebResource.isCsrfProtectionRequired("PUT", COOKIE));
        assertTrue(SessionWebResource.isCsrfProtectionRequired("DELETE", COOKIE));
        assertTrue(SessionWebResource.isCsrfProtectionRequired("patch", COOKIE));
    }

    @Test
    public void testLecturaPorCookieNoLoExige() {
        assertFalse(SessionWebResource.isCsrfProtectionRequired("GET", COOKIE));
        assertFalse(SessionWebResource.isCsrfProtectionRequired("HEAD", COOKIE));
        assertFalse(SessionWebResource.isCsrfProtectionRequired("OPTIONS", COOKIE));
    }

    @Test
    public void testCredencialPorEncabezadoQuedaExenta() {
        assertFalse(SessionWebResource.isCsrfProtectionRequired("POST", HEADER));
        assertFalse(SessionWebResource.isCsrfProtectionRequired("DELETE", HEADER));
        assertFalse(SessionWebResource.isCsrfProtectionRequired("POST", SIN_CREDENCIAL));
        assertFalse(SessionWebResource.isCsrfProtectionRequired("POST", null));
    }

    @Test
    public void testMetodoDesconocidoSeTrataComoMutacion() {
        assertTrue(SessionWebResource.isMutation(null));
        assertTrue(SessionWebResource.isMutation("FOO"));
        assertFalse(SessionWebResource.isMutation("get"));
    }

    @Test
    public void testValidacionDelEncabezado() {
        assertTrue(SessionWebResource.isCsrfHeaderValid("JavaBeanStack",
                SessionWebResource.CSRF_HEADER_VALUE));
        //Comparación exacta: el valor es una constante que emite el cliente de
        //la aplicación, no un dato que el usuario escriba.
        assertFalse(SessionWebResource.isCsrfHeaderValid("javabeanstack",
                SessionWebResource.CSRF_HEADER_VALUE));
        assertFalse(SessionWebResource.isCsrfHeaderValid(" JavaBeanStack ",
                SessionWebResource.CSRF_HEADER_VALUE));
        assertFalse(SessionWebResource.isCsrfHeaderValid(null,
                SessionWebResource.CSRF_HEADER_VALUE));
        assertFalse(SessionWebResource.isCsrfHeaderValid("XMLHttpRequest",
                SessionWebResource.CSRF_HEADER_VALUE));
    }
}

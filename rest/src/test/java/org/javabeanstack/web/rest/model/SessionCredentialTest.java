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
package org.javabeanstack.web.rest.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de la lectura de la credencial del encabezado de autorización y de la
 * cookie de sesión.
 *
 * <p>Interesa sobre todo lo que <b>no</b> tiene que pasar: un encabezado
 * ausente o mal formado no puede lanzar excepción, porque en ese momento
 * todavía falta mirar la cookie, y la credencial tiene que recordar por dónde
 * llegó —de ese dato depende la regla anti CSRF—.</p>
 *
 * @author Jorge Enciso
 */
public class SessionCredentialTest {

    @Test
    public void testEncabezadoBearer() {
        SessionCredential credential = SessionCredential.fromAuthHeader("Bearer abc123");
        assertTrue(credential.isPresent());
        assertTrue(credential.isFromHeader());
        assertFalse(credential.isFromCookie());
        assertEquals("abc123", credential.getValue());
    }

    @Test
    public void testEncabezadoConOtroEsquema() {
        //No se valida el esquema: la clase base tampoco lo hace y hay clientes
        //en producción que envían otros valores.
        SessionCredential credential = SessionCredential.fromAuthHeader("Token abc123");
        assertTrue(credential.isPresent());
        assertEquals("abc123", credential.getValue());
    }

    @Test
    public void testEncabezadoConEspaciosAlrededor() {
        SessionCredential credential = SessionCredential.fromAuthHeader("  Bearer abc123  ");
        assertEquals("abc123", credential.getValue());
    }

    @Test
    public void testCriterioDeCorteIgualAlDeLaClaseBase() {
        //La clase base corta por espacio simple y toma el segundo segmento. Si
        //acá se aceptara cualquier blanco, un mismo encabezado sería válido
        //para una capa e inválido para la otra en la misma petición.
        assertFalse(SessionCredential.fromAuthHeader("Bearer\tabc123").isPresent());
        assertFalse(SessionCredential.fromAuthHeader("Bearer  abc123").isPresent());
    }

    @Test
    public void testEncabezadoConTresSegmentos() {
        //Comportamiento fijado por test: se toma el segundo segmento y el resto
        //se descarta, igual que en la clase base.
        SessionCredential credential = SessionCredential.fromAuthHeader("Bearer abc def");
        assertTrue(credential.isPresent());
        assertEquals("abc", credential.getValue());
    }

    @Test
    public void testEncabezadoAusenteOInvalidoNoLanza() {
        assertFalse(SessionCredential.fromAuthHeader(null).isPresent());
        assertFalse(SessionCredential.fromAuthHeader("").isPresent());
        assertFalse(SessionCredential.fromAuthHeader("   ").isPresent());
        //Sin esquema no es una credencial válida
        assertFalse(SessionCredential.fromAuthHeader("abc123").isPresent());
        assertFalse(SessionCredential.fromAuthHeader("Bearer ").isPresent());
        assertNull(SessionCredential.fromAuthHeader(null).getValue());
        assertEquals(SessionCredential.Source.NONE,
                SessionCredential.fromAuthHeader(null).getSource());
    }

    @Test
    public void testCookie() {
        SessionCredential credential = SessionCredential.fromCookie("sess-1");
        assertTrue(credential.isPresent());
        assertTrue(credential.isFromCookie());
        assertFalse(credential.isFromHeader());
        assertEquals("sess-1", credential.getValue());
    }

    @Test
    public void testCookieVacia() {
        assertFalse(SessionCredential.fromCookie(null).isPresent());
        assertFalse(SessionCredential.fromCookie("   ").isPresent());
    }
}

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
package org.javabeanstack.web.rest.util;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de la traducción y validación de los parámetros de listado.
 *
 * <p>El caso que importa es el del ordenamiento: un campo fuera de la lista
 * blanca tiene que ser un pedido <b>inválido</b> y no un pedido ignorado en
 * silencio, porque la lista blanca es lo único que impide que el ordenamiento
 * se convierta en un punto de inyección o exponga campos que el recurso no
 * publica.</p>
 *
 * @author Jorge Enciso
 */
public class RestQueryTest {

    private static final List<String> WHITELIST = Arrays.asList("codigo", "nombre", "inactivo");

    @Test
    public void testValidacionDePagina() {
        assertTrue(RestQuery.isValidPage(null));
        assertTrue(RestQuery.isValidPage(1));
        assertTrue(RestQuery.isValidPage(500));
        assertTrue(RestQuery.isValidPage(RestQuery.MAX_PAGE));
        assertFalse(RestQuery.isValidPage(0));
        assertFalse(RestQuery.isValidPage(-3));
        //Sin techo, una página enorme desborda el cálculo de la primera fila y
        //la consulta falla con un error interno donde el contrato pide un 400.
        assertFalse(RestQuery.isValidPage(RestQuery.MAX_PAGE + 1));
        assertFalse(RestQuery.isValidPage(Integer.MAX_VALUE));
    }

    @Test
    public void testPrimeraFilaNuncaDesborda() {
        //Aunque el recurso omitiera la validación, el resultado no puede ser
        //negativo: setFirstResult con un valor negativo termina en 500.
        assertTrue(RestQuery.getFirstRow(Integer.MAX_VALUE, 100) > 0);
        assertEquals(Integer.MAX_VALUE, RestQuery.getFirstRow(Integer.MAX_VALUE, 100));
        assertTrue(RestQuery.getFirstRow(RestQuery.MAX_PAGE, 100) > 0);
        assertEquals(99_999_900, RestQuery.getFirstRow(RestQuery.MAX_PAGE, 100));
    }

    @Test
    public void testValidacionDeTamanho() {
        assertTrue(RestQuery.isValidSize(null));
        assertTrue(RestQuery.isValidSize(1));
        assertTrue(RestQuery.isValidSize(100));
        assertFalse(RestQuery.isValidSize(0));
        assertFalse(RestQuery.isValidSize(200));
        assertTrue(RestQuery.isValidSize(200, 500));
    }

    @Test
    public void testValoresPorOmision() {
        assertEquals(1, RestQuery.getPage(null));
        assertEquals(20, RestQuery.getSize(null));
        assertEquals(20, RestQuery.getMaxRows(null));
        //Un tamaño excesivo se recorta al máximo: el rechazo con 400 es
        //decisión del recurso, pero la consulta nunca debe salir sin techo.
        assertEquals(100, RestQuery.getSize(5000));
    }

    @Test
    public void testPrimeraFilaDeLaPagina() {
        assertEquals(0, RestQuery.getFirstRow(null, null));
        assertEquals(0, RestQuery.getFirstRow(1, 20));
        assertEquals(20, RestQuery.getFirstRow(2, 20));
        assertEquals(40, RestQuery.getFirstRow(3, 20));
        assertEquals(50, RestQuery.getFirstRow(2, 50));
    }

    @Test
    public void testOrdenSinPedido() {
        assertEquals("", RestQuery.parseOrderBy(null, WHITELIST));
        assertEquals("", RestQuery.parseOrderBy("   ", WHITELIST));
    }

    @Test
    public void testOrdenValido() {
        assertEquals("codigo asc", RestQuery.parseOrderBy("codigo", WHITELIST));
        assertEquals("codigo asc", RestQuery.parseOrderBy("codigo,asc", WHITELIST));
        assertEquals("nombre desc", RestQuery.parseOrderBy("nombre,desc", WHITELIST));
        assertEquals("nombre desc", RestQuery.parseOrderBy(" nombre , DESC ", WHITELIST));
        //La lista blanca decide cómo se escribe el campo en la consulta
        assertEquals("inactivo asc", RestQuery.parseOrderBy("INACTIVO", WHITELIST));
    }

    @Test
    public void testOrdenInvalido() {
        assertNull(RestQuery.parseOrderBy("clave", WHITELIST));
        assertNull(RestQuery.parseOrderBy("codigo,arriba", WHITELIST));
        assertNull(RestQuery.parseOrderBy("codigo,asc,nombre", WHITELIST));
        assertNull(RestQuery.parseOrderBy("codigo; drop table persona", WHITELIST));
        //Sin lista blanca no se ordena por nada
        assertNull(RestQuery.parseOrderBy("codigo", (List<String>) null));
    }

    @Test
    public void testOrdenConMapeoDeCampos() {
        Map<String, String> whitelist = new LinkedHashMap<>();
        whitelist.put("persona", "persona.nombre");
        whitelist.put("codigo", "codigo");
        assertEquals("persona.nombre asc", RestQuery.parseOrderBy("persona", whitelist));
        assertEquals("persona.nombre desc", RestQuery.parseOrderBy("persona,desc", whitelist));
        assertEquals("codigo asc", RestQuery.parseOrderBy("codigo", whitelist));
        assertNull(RestQuery.parseOrderBy("nombre", whitelist));
    }
}

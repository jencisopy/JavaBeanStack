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
package org.javabeanstack.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Prueba pura (sin servidor) del reconocimiento de parámetros nombrados en una
 * sentencia, usado por {@code AbstractDAO.populateQueryParameters} para no
 * asignar parámetros que la consulta no declara.
 */
public class AbstractDAONamedParameterTest {

    private static final String FN = "select datos.fn_idctacteruta(:idctacteruta,:ctacteruta,:idempresa)";

    @Test
    public void testParametroPresente() {
        assertTrue(AbstractDAO.isNamedParameterInQuery("idctacteruta", FN));
        assertTrue(AbstractDAO.isNamedParameterInQuery("ctacteruta", FN));
        assertTrue(AbstractDAO.isNamedParameterInQuery("idempresa", FN));
    }

    @Test
    public void testPrefijoDeOtroParametroNoCuenta() {
        //Observado el 2026-09-16: ":idctacte" es prefijo de ":idctacteruta" y la
        //búsqueda por subcadena lo daba por presente.
        assertFalse(AbstractDAO.isNamedParameterInQuery("idctacte", FN));
        assertFalse(AbstractDAO.isNamedParameterInQuery("ctacte", FN));
        assertFalse(AbstractDAO.isNamedParameterInQuery("id", FN));
    }

    @Test
    public void testMismoPrefijoEnDosPosiciones() {
        String q = "select o from X o where o.a = :ctacteruta and o.b = :ctacte";
        assertTrue(AbstractDAO.isNamedParameterInQuery("ctacte", q));
        assertTrue(AbstractDAO.isNamedParameterInQuery("ctacteruta", q));
        assertFalse(AbstractDAO.isNamedParameterInQuery("ctacter", q));
    }

    @Test
    public void testFinDeSentenciaYNulos() {
        assertTrue(AbstractDAO.isNamedParameterInQuery("x", "where a = :x"));
        assertFalse(AbstractDAO.isNamedParameterInQuery("x", "where a = :x_1"));
        assertFalse(AbstractDAO.isNamedParameterInQuery("x", null));
        assertFalse(AbstractDAO.isNamedParameterInQuery("", "where a = :x"));
        assertFalse(AbstractDAO.isNamedParameterInQuery(null, "where a = :x"));
    }
}

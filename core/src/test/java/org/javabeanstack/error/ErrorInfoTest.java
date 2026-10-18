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
package org.javabeanstack.error;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de ErrorInfo.
 *
 * @author Jorge Enciso
 */
public class ErrorInfoTest {

    public ErrorInfoTest() {
    }

    /**
     * Verifica los valores por defecto del constructor vacio.
     */
    @Test
    public void testDefaultValues() {
        System.out.println("errorInfo defaultValues");
        ErrorInfo errorInfo = new ErrorInfo();
        assertEquals("", errorInfo.getMessage());
        assertEquals(0, (int) errorInfo.getErrorNumber());
        assertEquals("", errorInfo.getEntity());
        assertEquals("noFieldSet", errorInfo.getFieldName());
        assertNull(errorInfo.getFieldNames());
        assertNull(errorInfo.getException());
        assertFalse(errorInfo.isWarning());
        assertEquals("", errorInfo.getIpRequest());
        assertEquals("ERROR", errorInfo.getEvent());
        assertEquals("E", errorInfo.getLevel());
        assertNull(errorInfo.getInfo());
    }

    /**
     * Test del constructor con mensaje, nro. de error y nombre del campo.
     */
    @Test
    public void testConstructorFieldName() {
        System.out.println("errorInfo constructorFieldName");
        ErrorInfo errorInfo = new ErrorInfo("mensaje de error", 100, "campo1");
        assertEquals("mensaje de error", errorInfo.getMessage());
        assertEquals(100, (int) errorInfo.getErrorNumber());
        assertEquals("campo1", errorInfo.getFieldName());
    }

    /**
     * Test del constructor con mensaje, nro. de error y lista de campos.
     */
    @Test
    public void testConstructorFieldNames() {
        System.out.println("errorInfo constructorFieldNames");
        String[] fields = {"campo1", "campo2"};
        ErrorInfo errorInfo = new ErrorInfo("mensaje de error", 200, fields);
        assertEquals("mensaje de error", errorInfo.getMessage());
        assertEquals(200, (int) errorInfo.getErrorNumber());
        assertArrayEquals(fields, errorInfo.getFieldNames());
    }

    /**
     * Test de los setters y getters.
     */
    @Test
    public void testSettersGetters() {
        System.out.println("errorInfo settersGetters");
        ErrorInfo errorInfo = new ErrorInfo();
        errorInfo.setMessage("otro mensaje");
        errorInfo.setErrorNumber(300);
        errorInfo.setEntity("entidad");
        errorInfo.setFieldName("campo");
        errorInfo.setFieldNames(new String[]{"a", "b"});
        Exception exception = new Exception("error");
        errorInfo.setException(exception);
        errorInfo.setWarning(true);
        errorInfo.setIpRequest("127.0.0.1");
        errorInfo.setEvent("EVENTO");
        errorInfo.setLevel("W");
        errorInfo.setInfo("info adicional");

        assertEquals("otro mensaje", errorInfo.getMessage());
        assertEquals(300, (int) errorInfo.getErrorNumber());
        assertEquals("entidad", errorInfo.getEntity());
        assertEquals("campo", errorInfo.getFieldName());
        assertEquals(2, errorInfo.getFieldNames().length);
        assertSame(exception, errorInfo.getException());
        assertTrue(errorInfo.isWarning());
        assertEquals("127.0.0.1", errorInfo.getIpRequest());
        assertEquals("EVENTO", errorInfo.getEvent());
        assertEquals("W", errorInfo.getLevel());
        assertEquals("info adicional", errorInfo.getInfo());
    }
}

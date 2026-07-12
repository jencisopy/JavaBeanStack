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
 * Pruebas unitarias de ErrorReg.
 *
 * @author Jorge Enciso
 */
public class ErrorRegTest {

    public ErrorRegTest() {
    }

    /**
     * Verifica los valores por defecto del constructor vacio.
     */
    @Test
    public void testDefaultValues() {
        System.out.println("errorReg defaultValues");
        ErrorReg errorReg = new ErrorReg();
        assertEquals("", errorReg.getMessage());
        assertEquals(0, (int) errorReg.getErrorNumber());
        assertEquals("", errorReg.getEntity());
        assertEquals("noFieldSet", errorReg.getFieldName());
        assertNull(errorReg.getFieldNames());
        assertNull(errorReg.getException());
        assertFalse(errorReg.isWarning());
        assertEquals("", errorReg.getIpRequest());
        assertEquals("ERROR", errorReg.getEvent());
        assertEquals("E", errorReg.getLevel());
        assertNull(errorReg.getInfo());
    }

    /**
     * Test del constructor con mensaje, nro. de error y nombre del campo.
     */
    @Test
    public void testConstructorFieldName() {
        System.out.println("errorReg constructorFieldName");
        ErrorReg errorReg = new ErrorReg("mensaje de error", 100, "campo1");
        assertEquals("mensaje de error", errorReg.getMessage());
        assertEquals(100, (int) errorReg.getErrorNumber());
        assertEquals("campo1", errorReg.getFieldName());
    }

    /**
     * Test del constructor con mensaje, nro. de error y lista de campos.
     */
    @Test
    public void testConstructorFieldNames() {
        System.out.println("errorReg constructorFieldNames");
        String[] fields = {"campo1", "campo2"};
        ErrorReg errorReg = new ErrorReg("mensaje de error", 200, fields);
        assertEquals("mensaje de error", errorReg.getMessage());
        assertEquals(200, (int) errorReg.getErrorNumber());
        assertArrayEquals(fields, errorReg.getFieldNames());
    }

    /**
     * Test de los setters y getters.
     */
    @Test
    public void testSettersGetters() {
        System.out.println("errorReg settersGetters");
        ErrorReg errorReg = new ErrorReg();
        errorReg.setMessage("otro mensaje");
        errorReg.setErrorNumber(300);
        errorReg.setEntity("entidad");
        errorReg.setFieldName("campo");
        errorReg.setFieldNames(new String[]{"a", "b"});
        Exception exception = new Exception("error");
        errorReg.setException(exception);
        errorReg.setWarning(true);
        errorReg.setIpRequest("127.0.0.1");
        errorReg.setEvent("EVENTO");
        errorReg.setLevel("W");
        errorReg.setInfo("info adicional");

        assertEquals("otro mensaje", errorReg.getMessage());
        assertEquals(300, (int) errorReg.getErrorNumber());
        assertEquals("entidad", errorReg.getEntity());
        assertEquals("campo", errorReg.getFieldName());
        assertEquals(2, errorReg.getFieldNames().length);
        assertSame(exception, errorReg.getException());
        assertTrue(errorReg.isWarning());
        assertEquals("127.0.0.1", errorReg.getIpRequest());
        assertEquals("EVENTO", errorReg.getEvent());
        assertEquals("W", errorReg.getLevel());
        assertEquals("info adicional", errorReg.getInfo());
    }
}

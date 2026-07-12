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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de los métodos estáticos puros de ErrorManager. Los
 * métodos que requieren ILogManager (EJB) se validan en los tests de
 * integración.
 *
 * @author Jorge Enciso
 */
public class ErrorManagerTest {

    private static final Logger LOGGER = LogManager.getLogger(ErrorManagerTest.class);

    public ErrorManagerTest() {
    }

    /**
     * Test of getStackTrace method, of class ErrorManager.
     */
    @Test
    public void testGetStackTrace() {
        System.out.println("errorManager getStackTrace");
        Exception exception = new IllegalStateException("falla de prueba");
        String result = ErrorManager.getStackTrace(exception);
        assertNotNull(result);
        assertTrue(result.contains("IllegalStateException"));
        assertTrue(result.contains("falla de prueba"));
        assertTrue(result.contains(ErrorManagerTest.class.getName()));
    }

    /**
     * Test of getStackCause method, of class ErrorManager. Debe devolver
     * primero el mensaje de la causa más profunda y luego los de las
     * excepciones que la envuelven.
     */
    @Test
    public void testGetStackCause() {
        System.out.println("errorManager getStackCause");
        Exception inner = new RuntimeException("causa interna");
        Exception outer = new Exception("mensaje externo", inner);
        String result = ErrorManager.getStackCause(outer);
        assertNotNull(result);
        assertEquals("causa interna\nmensaje externo\n", result);
    }

    /**
     * Test of getStackCause method sin causa anidada.
     */
    @Test
    public void testGetStackCauseSimple() {
        System.out.println("errorManager getStackCauseSimple");
        Exception exception = new Exception("mensaje simple");
        String result = ErrorManager.getStackCause(exception);
        assertEquals("mensaje simple\n", result);
        // Con mensaje nulo no debe fallar
        String resultNull = ErrorManager.getStackCause(new Exception());
        assertEquals("\n", resultNull);
    }

    /**
     * Test of getMessageToShow method, of class ErrorManager.
     */
    @Test
    public void testGetMessageToShow() {
        System.out.println("errorManager getMessageToShow");
        Exception inner = new RuntimeException("causa interna");
        Exception outer = new Exception("mensaje externo", inner);
        String result = ErrorManager.getMessageToShow(outer);
        assertNotNull(result);
        assertTrue(result.contains("causa interna"));
        assertTrue(result.contains("mensaje externo"));
        // Excepción sin mensaje: debe devolver el stack trace sin fallar
        String resultShort = ErrorManager.getMessageToShow(new Exception());
        assertNotNull(resultShort);
    }

    /**
     * Test of showError method, of class ErrorManager. Solo verifica que no
     * lance excepción al registrar por log4j.
     */
    @Test
    public void testShowError() {
        System.out.println("errorManager showError");
        Exception exception = new Exception("error a loguear", new RuntimeException("causa"));
        assertDoesNotThrow(() -> ErrorManager.showError(exception, LOGGER));
        assertDoesNotThrow(() -> ErrorManager.showError(new Exception(), LOGGER));
        assertDoesNotThrow(() -> ErrorManager.showError(exception, LOGGER, 1));
        assertDoesNotThrow(() -> ErrorManager.showError(exception, LOGGER, 0));
    }
}

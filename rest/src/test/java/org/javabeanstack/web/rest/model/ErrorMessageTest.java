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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de ErrorMessage: armado del mensaje de error estándar de
 * los recursos REST.
 *
 * @author Jorge Enciso
 */
public class ErrorMessageTest {

    public ErrorMessageTest() {
    }

    /**
     * Test of setters/getters, of class ErrorMessage.
     */
    @Test
    public void testErrorMessage() {
        System.out.println("errorMessage");
        ErrorMessage error = new ErrorMessage();
        assertNull(error.getErrorMessage());
        assertEquals(0, error.getErrorCode());
        assertNull(error.getDocumentation());

        error.setErrorMessage("Token inválido");
        error.setErrorCode(401);
        error.setDocumentation("https://ejemplo.com/docs/errores#401");

        assertEquals("Token inválido", error.getErrorMessage());
        assertEquals(401, error.getErrorCode());
        assertEquals("https://ejemplo.com/docs/errores#401", error.getDocumentation());
    }
}

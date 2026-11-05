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
package org.javabeanstack.web.util;

import org.javabeanstack.error.ErrorInfo;
import org.javabeanstack.error.IErrorInfo;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de FacesContextUtil. Solo se cubre el formato de los
 * mensajes con número, que no depende de un FacesContext activo.
 *
 * @author Jorge Enciso
 */
public class FacesContextUtilTest {

    private static IErrorInfo error(int number, String message) {
        IErrorInfo error = new ErrorInfo();
        error.setErrorNumber(number);
        error.setMessage(message);
        return error;
    }

    /**
     * El número del catálogo va al final, en el formato {@code mensaje - (nro)}.
     */
    @Test
    public void testMessageWithNumber() {
        assertEquals("Ud. no esta asignado a ninguna caja - (50241)",
                FacesContextUtil.messageWithNumber(error(50241, "Ud. no esta asignado a ninguna caja")));
        assertEquals(" - (51406)", FacesContextUtil.messageWithNumber(error(51406, null)));
    }

    /**
     * Sin número (cero) o sin registro no se agrega nada.
     */
    @Test
    public void testMessageWithoutNumber() {
        assertEquals("Sin número", FacesContextUtil.messageWithNumber(error(0, "Sin número")));
        assertEquals("", FacesContextUtil.messageWithNumber(null));
    }
}

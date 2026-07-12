/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2018 - 2027 Jorge Enciso
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
package org.javabeanstack.web.jsf.converters;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de LocalDateTimeConverter. Solo se cubren los caminos que
 * no dependen del componente Calendar: instanciar cualquier UIComponent de
 * Faces exige un FacesContext activo (runtime de Faces), que excede el
 * alcance de un test unitario. La conversión con patrón se valida en el ciclo
 * funcional JSF.
 *
 * @author Jorge Enciso
 */
public class LocalDateTimeConverterTest {

    public LocalDateTimeConverterTest() {
    }

    /**
     * Test of getAsString method con valores nulos o vacios: debe devolver
     * cadena vacia sin consultar el componente.
     */
    @Test
    public void testGetAsStringVacio() {
        System.out.println("localDateTimeConverter getAsStringVacio");
        LocalDateTimeConverter converter = new LocalDateTimeConverter();
        assertEquals("", converter.getAsString(null, null, null));
        assertEquals("", converter.getAsString(null, null, ""));
        assertEquals("", converter.getAsString(null, null, "   "));
    }
}

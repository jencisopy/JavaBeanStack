/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*        jenciso@javabeanstack.org
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
package org.javabeanstack.sifen.test.format;

import org.javabeanstack.sifen.core.fields.util.FieldFormatUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FieldFormatUtilsTests {

    @Test
    public void testFormatdCRed00() {
        String expected = "1";
        assertEquals(expected, FieldFormatUtil.formattdCRed(new BigDecimal("1")));
    }

    @Test
    public void testFormatdCRed01() {
        String expected = "-1912";
        assertEquals(expected, FieldFormatUtil.formattdCRed(new BigDecimal("-1912")));
    }

    @Test
    public void testFormatdCRed02() {
        String expected = "0.18";
        assertEquals(expected, FieldFormatUtil.formattdCRed(new BigDecimal("0.18")));
    }

    @Test
    public void testFormatdCRed03() {
        Locale currentLocale = Locale.getDefault();
        // probamos que con un LOCALE con separador "," para los decimales, se mantenga el PUNTO
        // en el formateador
        Locale.setDefault(Locale.GERMAN);
        String expected = "0.18";
        assertEquals(expected, FieldFormatUtil.formattdCRed(new BigDecimal("0.18")));
        Locale.setDefault(currentLocale);
    }



}

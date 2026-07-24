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

package org.javabeanstack.util;

import java.math.BigDecimal;
import java.util.Locale;
import org.efaps.number2words.Converter;
import org.efaps.number2words.IConverter;


/**
 * Utilidad que convierte un número ({@link java.math.BigDecimal}) a su
 * representación en letras, con soporte de idioma (español por defecto).
 * Se apoya en la librería {@code org.efaps.number2words}.
 *
 * @author Jorge Enciso
 */
public class NumberToLetterConverter {
    /**
     * Convierte un número a su representación en letras (en español).
     *
     * @param number número a convertir.
     * @return número escrito en letras.
     */
    public static String convert(BigDecimal number){
        return convert(number, new Locale("es"));
    }
    
    /**
     *
     * @param number numero a convertir en letras.
     * @param locale valores posibles Locales("es"), Locale.ENGLISH, Locale.GERMAN.
     * @return devuelve number en letras
     */
    public static String convert(BigDecimal number, Locale locale){
        if (number == null){
            return "";
        }
        Long numberInt = number.longValue();
        BigDecimal numberDecimal = number.remainder( BigDecimal.ONE ).multiply(new BigDecimal("100")); 
        Long numberDec = numberDecimal.longValue();
        IConverter converter = Converter.getMaleConverter(locale);
        String retornar = converter.convert(numberInt);
        if (numberDec > 0L){
            retornar += " CON "+numberDec+"/100";
        }
        return retornar.toUpperCase();
    }
}

package org.javabeanstack.util;

import java.math.BigDecimal;
import java.util.Locale;
import org.efaps.number2words.Converter;
import org.efaps.number2words.IConverter;


public class NumberToLetterConverter {
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

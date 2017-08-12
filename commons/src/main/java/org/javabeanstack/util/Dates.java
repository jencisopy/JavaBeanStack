/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 Jorge Enciso
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import org.apache.log4j.Logger;

/**
 * Funciones wrapper que facilitan el manejo de variables Date
 * @author Jorge Enciso
 */
public class Dates {
    /**
     * Convierte una cadena a una fecha
     * @param dateString
     * @return 
     */
    public static Date toDate(String dateString) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        return toDate(dateString, formatter);
    }

    /**
     * Convierte una cadena a una fecha
     * @param dateString
     * @param format ejemplo dd/mm/yyyy
     * @return 
     */
    public static Date toDate(String dateString, String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return toDate(dateString, formatter);
    }
    
    /**
     * Convierte una cadena a una fecha
     * @param dateString
     * @param formatter
     * @return 
     */
    public static Date toDate(String dateString, SimpleDateFormat formatter) {
        Date date = null;
        try {
            date = formatter.parse(dateString);
        } catch (ParseException ex) {
            Logger.getLogger(Fn.class).error(ex.getMessage());
        }
        return date;
    }

    /**
     * Convierte una fecha a variable de cadena, según formato deseado.
     * 
     * @param date    
     * @param formater    
     * @return  string con el formato deseado.   
    */
    public static String toString(Date date, SimpleDateFormat formater) {
        String result = formater.format(date);
        return result;
    }

    /**
     * Convierte una fecha a variable de cadena, según formato deseado.
     * 
     * @param date    
     * @param format     ejemplo dd/mm/yyyy
     * @return  string con el formato deseado.   
    */
    public static String toString(Date date, String format) {
        SimpleDateFormat formater = new SimpleDateFormat(format);
        String result = formater.format(date);
        return result;
    }

    /**
     * Devuelve una variable Date con la fecha y hora del momento
     * 
     * @return  fecha y hora de ahora.
    */
    public static Date now() {
        return Calendar.getInstance().getTime();
    }

    /**
     * Devuelve una variable Date con el valor de la fecha del día
     * @return fecha de hoy
     */
    public static Date today() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);        
        calendar.set(Calendar.MILLISECOND,0);        

        Date today = calendar.getTime();
        return today;
    }
}

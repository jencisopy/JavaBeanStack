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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import org.apache.log4j.Logger;
/**
 * Funciones wrapper que facilitan el manejo de variables Date
 *
 * @author Jorge Enciso
 */
public class Dates {

    private Dates() {
    }

    /**
     * Convierte una cadena a una fecha
     *
     * @param dateString
     * @return un dato fecha resultante de los parámetros introducidos.
     */
    public static Date toDate(String dateString) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        return toDate(dateString, formatter);
    }

    /**
     * Convierte una cadena a una fecha
     *
     * @param dateString
     * @param format ejemplo dd/mm/yyyy
     * @return un dato fecha resultante de los parámetros introducidos.
     */
    public static Date toDate(String dateString, String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return toDate(dateString, formatter);
    }

    /**
     * Convierte una fecha hora a una fecha
     *
     * @param date
     * @return un dato fecha resultante de los parámetros introducidos.
     */
    public static Date toDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }

    public static Date toDateTime(LocalDateTime dateTime){
        if (dateTime == null){
            return null;
        }
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());        
    }
    
    /**
     * Convierte una cadena a una fecha
     *
     * @param dateString
     * @param formatter
     * @return un dato fecha resultante de los parámetros introducidos.
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
     * @return string con el formato deseado.
     */
    public static String toString(Date date, SimpleDateFormat formater) {
        return formater.format(date);
    }

    /**
     * Convierte una fecha a variable de cadena, según formato deseado.
     *
     * @param date
     * @param format ejemplo dd/MM/yyyy
     * @return string con el formato deseado.
     */
    public static String toString(Date date, String format) {
        SimpleDateFormat formater = new SimpleDateFormat(format);
        return formater.format(date);
    }

    public static String toString(Object date, String format) {
        if (date instanceof LocalDateTime){
            return LocalDates.toString((LocalDateTime)date, format);
        }
        SimpleDateFormat formater = new SimpleDateFormat(format);
        return formater.format(date);
    }
    
    /**
     * Devuelve una variable Date con la fecha y hora del momento
     *
     * @return fecha y hora de ahora.
     */
    public static Date now() {
        return Calendar.getInstance().getTime();
    }

    /**
     * Devuelve una variable Date con el valor de la fecha del día
     *
     * @return fecha de hoy
     */
    public static Date today() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }

    /**
     * Suma un periodo a una fecha
     *
     * @param date fecha
     * @param quantity periodo
     * @param interval tipo de intervalo (dias, segundos, meses) ejemplo
     * Calendar.DAY_OF_YEAR
     * @return fecha resultado de la suma del periodo
     */
    public static Date sum(Date date, int quantity, int interval) {
        if (quantity == 0) {
            return date;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(interval, quantity);
        return calendar.getTime();
    }

    /**
     * Suma días a una fecha
     *
     * @param date fecha
     * @param days dias a sumar
     * @return fecha resultante de la suma de los días
     */
    public static Date sumDays(Date date, int days) {
        if (days == 0) {
            return date;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, days);
        return calendar.getTime();
    }

    /**
     * Suma segundos a una fecha
     *
     * @param date fecha
     * @param seconds segundos
     * @return fecha resultante de la suma de los segundos.
     */
    public static Date sumSeconds(Date date, int seconds) {
        if (seconds == 0) {
            return date;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.SECOND, seconds);
        return calendar.getTime();
    }

    /**
     * Devuelve el ultimo momento de una fecha
     *
     * @param date fecha
     * @return ultimo momento de una fecha ejemplo 31/12/2017 23:59:59
     */
    public static Date getLastTimeOfDay(Date date) {
        date = Dates.sum(date, 1, Calendar.DAY_OF_YEAR);
        date = Dates.sum(date, -1, Calendar.SECOND);
        return date;
    }

    /**
     * Dias entre dos fechas
     * @param start fecha inicial
     * @param end fecha final
     * @return cantidad de dias entre las fechas dadas
     */
    public static Long daysInterval(Date start, Date end) {
        if (start == null || end == null){
            return null;
        }
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(start);
        cal2.setTime(end);
        long time1 = cal1.getTimeInMillis();
        long time2 = cal2.getTimeInMillis();
        // Diferencias en dias
        long days = (time2 - time1) / (60 * 1000 * 60 * 24);
        return days;
    }
}

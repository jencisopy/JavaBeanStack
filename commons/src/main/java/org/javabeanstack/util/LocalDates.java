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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;


/**
 * Funciones wrapper que facilitan el manejo de variables Date
 *
 * @author Jorge Enciso
 */
public class LocalDates {

    private LocalDates() {
    }

    /**
     * Convierte una cadena a una fecha
     *
     * @param dateString
     * @return un dato fecha resultante de los parámetros introducidos.
     */
    public static LocalDate toDate(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return toDate(dateString, formatter);
    }

    
    
    /**
     * Convierte una cadena a una fecha
     *
     * @param dateString
     * @param format ejemplo dd/MM/yyyy
     * @return un dato fecha resultante de los parámetros introducidos.
     */
    public static LocalDate toDate(String dateString, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return toDate(dateString, formatter);
    }

    /**
     * Convierte una fecha hora a una fecha
     *
     * @param dateTime
     * @return un dato fecha resultante de los parámetros introducidos.
     */
    public static LocalDate toDate(LocalDateTime dateTime) {
        LocalDate result = dateTime.toLocalDate();
        return result;
    }

    /**
     * Convierte una cadena a una fecha
     *
     * @param dateString
     * @param formatter
     * @return un dato fecha resultante de los parámetros introducidos.
     */
    public static LocalDate toDate(String dateString, DateTimeFormatter formatter) {
        return LocalDate.parse(dateString, formatter);
    }

    /**
     * Convierte una cadena a una fecha y hora
     *
     * @param dateString
     * @return un dato fecha resultante de los parámetros introducidos.
     */
    public static LocalDateTime toDateTime(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return toDateTime(dateString, formatter);
    }

    /**
     * Convierte una cadena a una fecha y hora
     *
     * @param dateString
     * @param format
     * @return un dato fecha resultante de los parámetros introducidos.
     */
    public static LocalDateTime toDateTime(String dateString, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return toDateTime(dateString, formatter);
    }
    
    /**
     * Convierte una cadena a una fecha
     *
     * @param dateString
     * @param formatter
     * @return un dato fecha resultante de los parámetros introducidos.
     */
    public static LocalDateTime toDateTime(String dateString, DateTimeFormatter formatter) {
        return LocalDateTime.parse(dateString, formatter);
    }

    
    /**
     * Convierte una fecha a variable de cadena, según formato deseado.
     *
     * @param date
     * @param formatter
     * @return string con el formato deseado.
     */
    public static String toString(LocalDate date, DateTimeFormatter formatter) {
        return date.format(formatter);
    }

    /**
     * Convierte una fecha a variable de cadena, según formato deseado.
     *
     * @param dateTime
     * @param formatter
     * @return string con el formato deseado.
     */
    public static String toString(LocalDateTime dateTime, DateTimeFormatter formatter) {
        return dateTime.format(formatter);
    }
    
    
    /**
     * Convierte una fecha a variable de cadena, según formato deseado.
     *
     * @param date
     * @param format ejemplo dd/mm/yyyy
     * @return string con el formato deseado.
     */
    public static String toString(LocalDate date, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return date.format(formatter);
    }

    /**
     * Convierte una fecha a variable de cadena, según formato deseado.
     *
     * @param dateTime
     * @param format ejemplo dd/mm/yyyy
     * @return string con el formato deseado.
     */
    public static String toString(LocalDateTime dateTime, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return dateTime.format(formatter);
    }
    
    /**
     * Devuelve una variable Date con la fecha y hora del momento
     *
     * @return fecha y hora de ahora.
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * Devuelve una variable Date con el valor de la fecha del día
     *
     * @return fecha de hoy
     */
    public static LocalDateTime today() {
        return LocalDate.now().atStartOfDay();
    }


    /**
     * Devuelve el ultimo momento de una fecha
     *
     * @param dateTime fecha
     * @return ultimo momento de una fecha ejemplo 31/12/2017 23:59:59
     */
    public static LocalDateTime getLastTimeOfDay(LocalDateTime dateTime) {
        dateTime = dateTime.truncatedTo(ChronoUnit.DAYS);
        dateTime = dateTime.plusDays(1).minusSeconds(1);
        return dateTime;
    }

    /**
     * Dias entre dos fechas
     * @param start fecha inicial
     * @param end fecha final
     * @return cantidad de dias entre las fechas dadas
     */
    public static Long daysInterval(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null){
            return null;
        }
        return Duration.between(start, end).toDays();
    }
}
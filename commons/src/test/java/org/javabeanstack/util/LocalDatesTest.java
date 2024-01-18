/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2018 Jorge Enciso
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
package org.javabeanstack.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jorge Enciso
 */
public class LocalDatesTest {
    
    public LocalDatesTest() {
    }

    /**
     * Test of toDate method, of class LocalDates.
     */
    @Test
    public void testToDate_String() {
        System.out.println("toDate");
        String dateString = "19/05/1972";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate expResult = LocalDate.parse(dateString, formatter);
        LocalDate result = LocalDates.toDate(dateString);
        
        assertEquals(expResult, result);
        
    }

    /**
     * Test of toDate method, of class LocalDates.
     */
    @Test
    public void testToDate_String_String() {
        System.out.println("toDate");
        String dateString = "1972/05/19";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        LocalDate expResult = LocalDate.parse(dateString, formatter);        
        LocalDate result = LocalDates.toDate(dateString, formatter);
        
        assertEquals(expResult, result);
    }

    /**
     * Test of toDate method, of class LocalDates.
     */
    @Test
    public void testToDate_LocalDateTime() {
        System.out.println("toDate");
        String dateString = "1972/05/19";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        LocalDate expResult = LocalDate.parse(dateString, formatter);        
        LocalDateTime dateTime = LocalDateTime.of(1972, 5, 19, 0, 0);
        LocalDate result = LocalDates.toDate(dateTime);        
        assertEquals(expResult, result);        
    }

    /**
     * Test of toDate method, of class LocalDates.
     */
    @Test
    public void testToDate_String_DateTimeFormatter() {
        System.out.println("toDate");
        String dateString = "1972/05/19";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        LocalDate expResult = LocalDate.parse(dateString, formatter);        
        LocalDate result = LocalDates.toDate(dateString, formatter);
        assertEquals(expResult, result);
    }

    /**
     * Test of toDateTime method, of class LocalDates.
     */
    @Test
    public void testToDateTime_String() {
        System.out.println("toDateTime");
        String dateString = "19/05/1972 15:00:00";
        LocalDateTime expResult = LocalDateTime.of(1972, 5, 19, 15, 0);        
        LocalDateTime result = LocalDates.toDateTime(dateString);
        assertEquals(expResult, result);
    }

    /**
     * Test of toDateTime method, of class LocalDates.
     */
    @Test
    public void testToDateTime_String_DateTimeFormatter() {
        System.out.println("toDateTime");
        String dateString = "1972/05/19 00:00";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        LocalDateTime expResult = LocalDateTime.of(1972, 5, 19, 0, 0);        
        LocalDateTime result = LocalDates.toDateTime(dateString, formatter);
        assertEquals(expResult, result);
        
        dateString = "20191231T00:00:00";
        expResult = LocalDateTime.of(2019, 12, 31, 0, 0);        
        formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HH:mm:ss");        
        result = LocalDates.toDateTime(dateString, formatter);
        assertEquals(expResult, result);
        
    }

    /**
     * Test of toString method, of class LocalDates.
     */
    @Test
    public void testToString_LocalDate_DateTimeFormatter() {
        System.out.println("toString");
        LocalDate date = LocalDates.toDate("1972/05/19","yyyy/MM/dd");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        String expResult = "1972/05/19";
        String result = LocalDates.toString(date, formatter);
        assertEquals(expResult, result);
    }

    /**
     * Test of toString method, of class LocalDates.
     */
    @Test
    public void testToString_LocalDateTime_DateTimeFormatter() {
        System.out.println("toString");
        LocalDateTime dateTime = LocalDates.toDateTime("1972/05/19 15:00:15","yyyy/MM/dd HH:mm:ss");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        String expResult = "1972/05/19 15:00:15";
        String result = LocalDates.toString(dateTime, formatter);
        assertEquals(expResult, result);
    }

    @Test
    public void testToString_LocalDateTime_DateTimeFormatter2() {
        System.out.println("toString");
        LocalDateTime dateTime = LocalDates.toDateTime("1972/05/19T15:00:15","yyyy/MM/dd'T'HH:mm:ss");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss");
        String expResult = "1972/05/19T15:00:15";
        String result = LocalDates.toString(dateTime, formatter);
        assertEquals(expResult, result);
    }
    
    /**
     * Test of toString method, of class LocalDates.
     */
    @Test
    public void testToString_LocalDate_String() {
        System.out.println("toString");
        LocalDate date = LocalDates.toDate("1972/05/19","yyyy/MM/dd");
        String format = "yyyy/MM/dd";
        String expResult = "1972/05/19";
        String result = LocalDates.toString(date, format);
        assertEquals(expResult, result);
    }

    /**
     * Test of toString method, of class LocalDates.
     */
    @Test
    public void testToString_LocalDateTime_String() {
        System.out.println("toString");
        LocalDateTime dateTime = LocalDateTime.of(1972, 5, 19, 15, 20);
        String format = "yyyy/MM/dd HH:mm";
        String expResult = "1972/05/19 15:20";
        String result = LocalDates.toString(dateTime, format);
        assertEquals(expResult, result);
        
        format = "yyyyMMdd";
        expResult = "19720519";
        result = LocalDates.toString(dateTime, format);
        assertEquals(expResult, result);
        
    }

    /**
     * Test of now method, of class LocalDates.
     */
    @Test
    public void testNow() {
        System.out.println("now");
        LocalDateTime expResult = LocalDateTime.now();
        LocalDateTime result = LocalDates.now();
        assertEquals(expResult.truncatedTo(ChronoUnit.SECONDS), result.truncatedTo(ChronoUnit.SECONDS));
    }

    /**
     * Test of today method, of class LocalDates.
     */
    @Test
    public void testToday() {
        System.out.println("today");
        LocalDateTime expResult = LocalDate.now().atStartOfDay();
        LocalDateTime result = LocalDates.today();
        assertEquals(expResult, result);
    }

    /**
     * Test of getLastTimeOfDay method, of class LocalDates.
     */
    @Test
    public void testGetLastTimeOfDay() {
        System.out.println("getLastTimeOfDay");
        LocalDateTime dateTime = LocalDateTime.of(1972, 5, 19, 14, 0, 0);
        LocalDateTime expResult = LocalDateTime.of(1972, 5, 19, 23, 59, 59);
        LocalDateTime result = LocalDates.getLastTimeOfDay(dateTime);
        assertEquals(expResult, result);
    }

    /**
     * Test of daysInterval method, of class LocalDates.
     */
    @Test
    public void testDaysInterval() {
        System.out.println("daysInterval");
        LocalDateTime start = LocalDateTime.of(1972, 5, 19,0,0);
        LocalDateTime end = LocalDateTime.of(1972, 5, 25,0,0);
        Long expResult = 6L;
        Long result = LocalDates.daysInterval(start, end);
        assertEquals(expResult, result);
    }
    
}

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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Jorge Enciso
 */
public class DatesTest {
    /**
     * Test of toDate method, of class Fn.
     * @throws java.text.ParseException
     */
    @Test
    public void testToDate() throws ParseException {
        System.out.println("toDate");
        String dateString = "19/05/1972";
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date expResult = formatter.parse(dateString);
        
        Date result = Dates.toDate(dateString);
        
        System.out.println(result.getTime());
        System.out.println(expResult.getTime());
        assertEquals(expResult, result);
        
        expResult = java.sql.Date.valueOf("1972-05-19");
        assertEquals(expResult, result);        
    }

    /**
     * Test of toDate method, of class Fn.
     */
    @Test
    public void testToDate_String() {
        System.out.println("toDate");
        String dateString = "1972/05/19";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        Date expResult = java.sql.Date.valueOf("1972-05-19");
        Date result = Dates.toDate(dateString, formatter);
        
        assertEquals(expResult, result);
    }

    /**
     * Test of toString method, of class Fn.
     */
    @Test
    public void testToString_Date_SimpleDateFormat() {
        System.out.println("toString");
        Date date = Dates.toDate("1972/05/19","yyyy/MM/dd");
        SimpleDateFormat formater = new SimpleDateFormat("yyyy/MM/dd");
        String expResult = "1972/05/19";
        String result = Dates.toString(date, formater);
        assertEquals(expResult, result);
    }

    /**
     * Test of toString method, of class Fn.
     */
    @Test
    public void testToString_Date_String() {
        System.out.println("toString");
        Date date = java.sql.Date.valueOf("1972-05-19");
        String format = "yyyy-MM-dd";
        String expResult = "1972-05-19";
        String result = Dates.toString(date, format);
        assertEquals(expResult, result); 
        
        format = "yyyy-MM-dd'T'HH:mm:ss";
        expResult = "1972-05-19T00:00:00";
        result = Dates.toString(date, format);
        assertEquals(expResult, result);
        
    }

    /**
     * Test of now method, of class Fn.
     */
    //@Test
    public void testNow() {
        System.out.println("now");
        Date expResult = new Date();
        Date result = Dates.now();
        assertEquals(expResult, result);
        
        System.out.println(expResult);
        System.out.println(result);
    }

    /**
     * Test of today method, of class Fn.
     */
    @Test
    public void testToday() {
        System.out.println("today");
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date expResult = calendar.getTime();
        Date result = Dates.today();
        System.out.println(expResult);
        System.out.println(result);
        
        assertEquals(expResult, result);
    }



    /**
     * Test of toDate method, of class Dates.
     */
    @Test
    public void testToDate_Date() {
        System.out.println("toDate");
        Date date = Dates.now();
        Date expResult = Dates.today();
        Date result = Dates.toDate(date);
        assertEquals(expResult, result);
    }


    /**
     * Test of sum method, of class Dates.
     */
    @Test
    public void testSum() {
        System.out.println("sum");
        Date date = Dates.today();
        int quantity = 1;
        int interval = Calendar.HOUR_OF_DAY;
        
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(interval, quantity);
        Date expResult = calendar.getTime();
        
        Date result = Dates.sum(date, quantity, interval);
        assertEquals(expResult, result);
    }

    /**
     * Test of sumDays method, of class Dates.
     */
    @Test
    public void testSumDays() {
        System.out.println("sumDays");
        Date date = Dates.today();
        int days = 2;
        
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, days);
        Date expResult = calendar.getTime();
        
        Date result = Dates.sumDays(date, days);
        assertEquals(expResult, result);
    }

    /**
     * Test of sumSeconds method, of class Dates.
     */
    @Test
    public void testSumSeconds() {
        System.out.println("sumSeconds");
        Date date = Dates.today();
        int seconds = 10;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, seconds);
        calendar.set(Calendar.MILLISECOND, 0);
        
        Date expResult = calendar.getTime();
        Date result = Dates.sumSeconds(date, seconds);
        assertEquals(expResult, result);
    }

    /**
     * Test of getLastTimeOfDay method, of class Dates.
     */
    @Test
    public void testGetLastTimeOfDay() {
        System.out.println("getLastTimeOfDay");
        Date date = Dates.today();
        Date expResult = Dates.sum(date, 1, Calendar.DAY_OF_YEAR);
        expResult = Dates.sum(expResult, -1, Calendar.SECOND);
        Date result = Dates.getLastTimeOfDay(date);
        assertEquals(expResult, result);
    }
    
    @Test
    public void testNumberToDate() {
        System.out.println("numberToDate");
        Date date = new Date(1516239022);
        System.out.println(date);
    }
}

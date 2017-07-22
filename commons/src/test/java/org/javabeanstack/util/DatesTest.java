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
import static org.junit.Assert.assertEquals;
import org.junit.Test;

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
        //fail("The test case is a prototype.");
    }

    /**
     * Test of toDate method, of class Fn.
     */
    @Test
    public void testToDate2() {
        System.out.println("toDate");
        String dateString = "1972/05/19";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        Date expResult = java.sql.Date.valueOf("1972-05-19");
        Date result = Dates.toDate(dateString, formatter);
        
        assertEquals(expResult, result);
        //fail("The test case is a prototype.");
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
        //fail("The test case is a prototype.");
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
        //fail("The test case is a prototype.");
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
        //fail("The test case is a prototype.");
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

        
        //fail("The test case is a prototype.");
    }
}

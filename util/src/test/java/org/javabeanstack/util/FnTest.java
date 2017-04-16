/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javabeanstack.util;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jorge Enciso
 */
public class FnTest {
    
    public FnTest() {
    }

    /**
     * Test of toObject method, of class Fn.
     * @throws java.lang.Exception
     */
    @Test
    public void testToObject() throws Exception {
        System.out.println("toObject");
        String source = "1";
        Integer target = 0;
        Integer expResult = 1;
        Object result = Fn.toObject(source, target);
        assertEquals(expResult, result);
        
        source = "1";
        Long target2 = 0L;
        Long expResult2 = 1L;
        result = Fn.toObject(source, target2);        
        assertEquals(expResult2, result);
        
        source = "1.01";
        Double target3 = 0D;
        Double expResult3 = 1.01D;
        result = Fn.toObject(source, target3);        
        assertEquals(expResult3, result);

        source = "1.01";
        Float target4 = 0F;
        Float expResult4 = 1.01F;
        result = Fn.toObject(source, target4);        
        assertEquals(expResult4, result);

        source = "1";
        Short target5 = 0;
        Short expResult5 = 1;
        result = Fn.toObject(source, target5);        
        assertEquals(expResult5, result);
        
        /*
        source = "1";
        BigDecimal target6 = BigDecimal.valueOf(0L);
        BigDecimal expResult6 = BigDecimal.valueOf(1L);
        result = Fn.toObject(source, target6);        
        assertEquals(expResult6, result);
        */
    }

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
        
        Date result = Fn.toDate(dateString);
        
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
        Date result = Fn.toDate(dateString, formatter);
        
        assertEquals(expResult, result);
        //fail("The test case is a prototype.");
    }

    /**
     * Test of toString method, of class Fn.
     */
    @Test
    public void testToString_Date_SimpleDateFormat() {
        System.out.println("toString");
        Date date = Fn.toDate("1972/05/19","yyyy/MM/dd");
        SimpleDateFormat formater = new SimpleDateFormat("yyyy/MM/dd");
        String expResult = "1972/05/19";
        String result = Fn.toString(date, formater);
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
        String result = Fn.toString(date, format);
        assertEquals(expResult, result);
        //fail("The test case is a prototype.");
    }

    /**
     * Test of inList method, of class Fn.
     */
    @Test
    public void testInList_String_StringArr() {
        System.out.println("inList");
        String obj = "existe";
        String[] list = {"EXISTE","exist","exist2","existe"};
        boolean expResult = true;
        boolean result = Fn.inList(obj, list);
        assertEquals(expResult, result);
        //fail("The test case is a prototype.");
    }


    /**
     * Test of inList method, of class Fn.
     */
    @Test
    public void testInList_Integer_intArr() {
        System.out.println("inList");
        Integer obj = 1;
        int[] list = {1,2,4,5,1};
        boolean expResult = true;
        boolean result = Fn.inList(obj, list);
        assertEquals(expResult, result);
        //fail("The test case is a prototype.");
    }

    /**
     * Test of findInMatrix method, of class Fn.
     */
    @Test
    public void testFindInMatrix_ObjectArr_Object() {
        System.out.println("findInMatrix");
        Object[] matrix = {3,2,3,4,1};
        Object search = 1;
        Integer expResult = 4; // Posición en la matriz
        Integer result = Fn.findInMatrix(matrix, search);
        assertEquals(expResult, result);
        //fail("The test case is a prototype.");
    }

    /**
     * Test of findInMatrix method, of class Fn.
     */
    @Test
    public void testFindInMatrix_3args() {
        System.out.println("findInMatrix");
        String[] matrix = {"EXISTE","existe"};
        String search = "existe";
        Boolean caseSensitive = false;
        Integer expResult = 0;
        Integer result = Fn.findInMatrix(matrix, search, caseSensitive);
        assertEquals(expResult, result);

        caseSensitive = true;
        expResult = 1;
        result = Fn.findInMatrix(matrix, search, caseSensitive);
        assertEquals(expResult, result);
        //fail("The test case is a prototype.");
    }

    /**
     * Test of toLogical method, of class Fn.
     */
    @Test
    public void testToLogical() {
        System.out.println("toLogical");
        Object value = "1";
        Boolean expResult = true;
        Boolean result = Fn.toLogical(value);
        assertEquals(expResult, result);
        
        value = 1;
        expResult = true;
        result = Fn.toLogical(value);
        assertEquals(expResult, result);

        value = "0";
        expResult = false;
        result = Fn.toLogical(value);
        assertEquals(expResult, result);

        value = 0;
        expResult = false;
        result = Fn.toLogical(value);
        assertEquals(expResult, result);
        //fail("The test case is a prototype.");
    }


    /**
     * Test of getMD5 method, of class Fn.
     */
    //@Test
    public void testGetMD5_String() {
        System.out.println("getMD5");
        String msg = "";
        String expResult = "";
        String result = Fn.getMD5(msg);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    /**
     * Test of now method, of class Fn.
     */
    @Test
    public void testNow() {
        System.out.println("now");
        Date expResult = new Date();
        Date result = Fn.now();
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
        Date result = Fn.today();
        System.out.println(expResult);
        System.out.println(result);
        
        assertEquals(expResult, result);

        
        //fail("The test case is a prototype.");
    }

    /**
     * Test of iif method, of class Fn.
     */
    @Test
    public void testIif() {
        System.out.println("iif");
        boolean condition = (1 == 1);
        Object value1 = 1;
        Object value2 = 2;
        Object expResult = 1;
        Object result = Fn.iif(condition, value1, value2);
        assertEquals(expResult, result);
        //fail("The test case is a prototype.");
    }

    /**
     * Test of nvl method, of class Fn.
     */
    @Test
    public void testNvl() {
        System.out.println("nvl");
        Object value = null;
        Object alternateValue = "es nulo";
        Object expResult = "es nulo";
        Object result = Fn.nvl(value, alternateValue);
        assertEquals(expResult, result);
        //fail("The test case is a prototype.");
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javabeanstack.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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

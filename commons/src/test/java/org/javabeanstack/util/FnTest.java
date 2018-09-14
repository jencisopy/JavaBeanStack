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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.javabeanstack.crypto.CipherUtil;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 *
 * @author Jorge Enciso
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
        
        result = Fn.inList(obj,"EXISTE","exist","exist2","existe");
        assertEquals(expResult, result);
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
    }

    /**
     * Test of bytesToHex method, of class Fn.
     * @throws java.io.UnsupportedEncodingException
     * @throws java.security.NoSuchAlgorithmException
     */
    @Test
    public void testBytesToHex() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        System.out.println("bytesToHex");
        String expResult = "5ad6f23da25b3a54cd5ae716c401732d";        
        
        String msg = "abcdefghijklmnñopqrstuvwxyzáéíóú";        
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] bytes = digest.digest(msg.getBytes("UTF-8"));
        String result = Fn.bytesToHex(bytes);
        
        assertEquals(expResult, result);
    }

    /**
     * Test of hexToByte method, of class Fn.
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.io.UnsupportedEncodingException
     */
    @Test
    public void testHexToByte() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        System.out.println("hexToByte");
        String hexText = "5ad6f23da25b3a54cd5ae716c401732d";
        
        String msg = "abcdefghijklmnñopqrstuvwxyzáéíóú";        
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] expResult = digest.digest(msg.getBytes("UTF-8"));
        
        byte[] result = Fn.hexToByte(hexText);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of base64ToBytes method, of class Fn.
     * @throws java.lang.Exception
     */
    @Test
    public void testBase64ToBytes() throws Exception {
        System.out.println("base64ToBytes");
        String clearText = "abcdefghijklmnñopqrstuvwxyzáéíóú";
        String key = "123456á";
        
        SecretKeySpec keyspec = new SecretKeySpec(key.getBytes(), CipherUtil.BLOWFISH);
        Cipher cipher = Cipher.getInstance(CipherUtil.BLOWFISH);
        cipher.init(Cipher.ENCRYPT_MODE, keyspec);
        byte[] expResult = cipher.doFinal(clearText.getBytes());
        
        String encrypted64 = CipherUtil.encryptBlowfishToBase64(clearText, key);
        byte[] result = Fn.base64ToBytes(encrypted64);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of bytesToBase64 method, of class Fn.
     * @throws java.lang.Exception
     */
    @Test
    public void testBytesToBase64() throws Exception {
        System.out.println("bytesToBase64");
        String clearText = "abcdefghijklmnñopqrstuvwxyzáéíóú";
        String key = "123456á";
        
        SecretKeySpec keyspec = new SecretKeySpec(key.getBytes(), CipherUtil.BLOWFISH);
        Cipher cipher = Cipher.getInstance(CipherUtil.BLOWFISH);
        cipher.init(Cipher.ENCRYPT_MODE, keyspec);
        byte[] bytes = cipher.doFinal(clearText.getBytes());
        
        String expResult = CipherUtil.encryptBlowfishToBase64(clearText, key);
        String result = Fn.bytesToBase64(bytes);
        
        assertEquals(expResult, result);
    }
}

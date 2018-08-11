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
package org.javabeanstack.crypto;

import org.javabeanstack.util.Fn;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author JORGE
 */
public class CipherUtilTest {

    public CipherUtilTest() {
    }

    /**
     * Test of encryptBlowfish method, of class CipherUtil.
     * @throws java.lang.Exception
     */
    @Test
    public void testEncryptBlowfish() throws Exception {
        System.out.println("encryptBlowfish");
        String clearText = "abcdefghijklmnñopqrstuvwxyzáéíóú";
        String key = "123456";
        String expResult = clearText;
        String encrypted = CipherUtil.encryptBlowfish(clearText, key);
        String decrypted = CipherUtil.decryptBlowfish(encrypted, key);
        assertEquals(expResult, decrypted);
    }

    /**
     * Test of decryptBlowfish method, of class CipherUtil.
     * @throws java.lang.Exception
     */
    @Test
    public void testDecryptBlowfish() throws Exception{
        System.out.println("decryptBlowfish");
        byte[] text = "abcdefghijklmnñopqrstuvwxyzáéíóú".getBytes("UTF-8");
        String clearText = new String(text,"UTF-8");
        String key = "123456";
        String expResult = clearText;
        String encrypted = CipherUtil.encryptBlowfish(clearText, key);
        String decrypted = CipherUtil.decryptBlowfish(encrypted, key);
        assertEquals(expResult, decrypted);
    }

   /**
     * Test of encryptBlowfishToHex method, of class CipherUtil.
     * @throws java.lang.Exception
     */
    @Test
    public void testEncryptBlowfishToHex() throws Exception {
        System.out.println("encryptBlowfishToHex");
        String clearText = "abcdefghijklmnñopqrstuvwxyzáéíóú";
        String key = "123456";
        String expResult = clearText;
        String encrypted = CipherUtil.encryptBlowfishToHex(clearText, key);
        String decrypted = CipherUtil.decryptBlowfishFromHex(encrypted, key);
        assertEquals(expResult, decrypted);
    }

    /**
     * Test of decryptBlowfishFromHex method, of class CipherUtil.
     * @throws java.lang.Exception
     */
    @Test
    public void testDecryptBlowfishFromHex() throws Exception {
        System.out.println("decryptBlowfishFromHex");
        String clearText = "abcdefghijklmnñopqrstuvwxyzáéíóú";
        String key = "123456"; 
        String expResult = clearText;
        String encrypted = CipherUtil.encryptBlowfishToHex(clearText, key);
        String decrypted = CipherUtil.decryptBlowfishFromHex(encrypted, key);
        assertEquals(expResult, decrypted);
    }

    /**
     * Test of encryptBlowfishToBase64 method, of class CipherUtil.
     * @throws java.lang.Exception
     */
    @Test
    public void testEncryptBlowfishToBase64() throws Exception {
        System.out.println("encryptBlowfishToBase64");
        String clearText = "abcdefghijklmnñopqrstuvwxyzáéíóú";
        String key = "123456";
        String expResult = clearText;
        String encrypted = CipherUtil.encryptBlowfishToBase64(clearText, key);
        System.out.println(encrypted);        
        
        String decrypted = CipherUtil.decryptBlowfishFromBase64(encrypted, key);
        assertEquals(expResult, decrypted);
    }

    /**
     * Test of decryptBlowfishFromBase64 method, of class CipherUtil.
     * @throws java.lang.Exception
     */
    @Test
    public void testDecryptBlowfishFromBase64() throws Exception {
        System.out.println("decryptBlowfishFromBase64");
        String clearText = "abcdefghijklmnñopqrstuvwxyzáéíóú";
        String key = "123456";
        String expResult = clearText;
        String encrypted = CipherUtil.encryptBlowfishToBase64(clearText, key);
        String decrypted = CipherUtil.decryptBlowfishFromBase64(encrypted, key);
        assertEquals(expResult, decrypted);
    }
    
    /**
     * Test of encryptAES method, of class CipherUtil.
     * @throws java.lang.Exception
     */
    @Test
    public void testEncryptAES() throws Exception{
        System.out.println("encryptAES");
        String clearText = "abcdefghijklmnñopqrstuvwxyzáéíóú";
        String key = "012345678901234567890123456789";
        String expResult = clearText;
        byte[] encrypted = CipherUtil.encryptAES(clearText, key);
        String decrypted = CipherUtil.decryptAES(encrypted, key);
        assertEquals(expResult, decrypted);
        
        String hex = Fn.bytesToHex(encrypted);
        Assert.assertArrayEquals(encrypted, Fn.hexToByte(hex));
    }

    
    /**
     * Test of decryptAES method, of class CipherUtil.
     * @throws java.lang.Exception
     */
    @Test
    public void testDecryptAES() throws Exception{
        System.out.println("decryptAES");
        byte[] text = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZabcdefghijklmnñopqrstuvwxyzáéíóú".getBytes();
        String clearText = new String(text);
        String key = "012345678901234567890123456789";
        String expResult = clearText;
        byte[] encrypted = CipherUtil.encryptAES(clearText, key);

        String hex = Fn.bytesToHex(encrypted);
        byte[] encrypted2 = Fn.hexToByte(hex);

        String decrypted = CipherUtil.decryptAES(encrypted2, key);
        assertEquals(expResult, decrypted);
    }

 
    /**
     * Test of encryptAES_ToHex method, of class CipherUtil.
     * @throws java.lang.Exception
     */
    @Test
    public void testEncryptAES_ToHex() throws Exception {
        System.out.println("encryptAES_ToHex");
        String clearText = "abcdefghijklmnñopqrstuvwxyzáéíóú";
        String key = "012345678901234567890123456789";
        String expResult = clearText;
        String encrypted = CipherUtil.encryptAES_ToHex(clearText, key);
        String decrypted = CipherUtil.decryptAES(Fn.hexToByte(encrypted), key);
        assertEquals(expResult, decrypted);
    }

    /**
     * Test of encryptAES_ToBase64 method, of class CipherUtil.
     * @throws java.lang.Exception
     */
    @Test
    public void testEncryptAES_ToBase64() throws Exception {
        System.out.println("encryptAES_ToBase64");
        String clearText = "abcdefghijklmnñopqrstuvwxyzáéíóú";
        String key = "012345678901234567890123456789";
        String expResult = clearText;
        String encrypted = CipherUtil.encryptAES_ToBase64(clearText, key);
        String decrypted = CipherUtil.decryptAES(Fn.base64ToBytes(encrypted), key);
        assertEquals(expResult, decrypted);
    }
    
    /**
     * Test of decryptAES_FromHex method, of class CipherUtil.
     * @throws java.lang.Exception
     */
    @Test
    public void testDecryptAES_FromHex() throws Exception {
        System.out.println("decryptAES_FromHex");
        String clearText = "abcdefghijklmnñopqrstuvwxyzáéíóú";
        String key = "012345678901234567890123456789";
        String expResult = clearText;
        String encrypted = CipherUtil.encryptAES_ToHex(clearText, key);
        String decrypted = CipherUtil.decryptAES_FromHex(encrypted, key);
        assertEquals(expResult, decrypted);
    }
}

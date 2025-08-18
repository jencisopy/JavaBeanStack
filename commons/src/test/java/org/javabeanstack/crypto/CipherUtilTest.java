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

import java.security.KeyFactory;
import javax.crypto.SecretKey;
import org.javabeanstack.util.Fn;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Jorge Enciso
 */
public class CipherUtilTest {

    public CipherUtilTest() {
    }

    
    @Test
    public void testEncryptBlowfishToHex() throws Exception {
        System.out.println("encryptBlowfishToHex");
        String clearText = "abcdefghijklmnñopqrstuvwxyzáéíóú";
        String key = "123456á";
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
        String key = "123456Á"; 
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
        String key = "123456á";
        String expResult = clearText;
        String encrypted = CipherUtil.encryptBlowfishToBase64(clearText, key);
        System.out.println(encrypted);        
        
        String decrypted = CipherUtil.decryptBlowfishFromBase64(encrypted, key);
        assertEquals(expResult, decrypted);
    }

    /**
     * Test of encryptBlowfishToBase64 method, of class CipherUtil.
     * @throws java.lang.Exception
     */
    @Test
    public void testEncryptBlowfishToBase64_2() throws Exception {
        System.out.println("encryptBlowfishToBase64");
        String clearText = "{appName=MakerCloud, ip1=localhost, ip2=server.oym.com.py}";
        String key = "123456á";
        String expResult = clearText;
        String encrypted = CipherUtil.encryptBlowfishToBase64(clearText, key);
        System.out.println(encrypted);        
        
        String decrypted = CipherUtil.decryptBlowfishFromBase64(encrypted, key);
        assertEquals(expResult, decrypted);
        System.out.println(decrypted);   
    }
    
    /**
     * Test of decryptBlowfishFromBase64 method, of class CipherUtil.
     * @throws java.lang.Exception
     */
    @Test
    public void testDecryptBlowfishFromBase64() throws Exception {
        System.out.println("decryptBlowfishFromBase64");
        String clearText = "abcdefghijklmnñopqrstuvwxyzáéíóú";
        String key = "123456á";
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
        String key = "012345678901234567890123456789á";
        String expResult = clearText;
        byte[] encrypted = CipherUtil.encryptAES(clearText, key);
        String decrypted = CipherUtil.decryptAES(encrypted, key);
        assertEquals(expResult, decrypted);
        
        String hex = Fn.bytesToHex(encrypted);
        assertArrayEquals(encrypted, Fn.hexToByte(hex));
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
        String key = "012345678901234567890123456789á";
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

    /**
     * Test of getSecureRandomKey method, of class CipherUtil.
     */
    @Test
    public void testGetSecureRandomKey_String_int() throws Exception {
        System.out.println("getSecureRandomKey");
        String algoritm = "AES";
        int keyBitSize = 256;
        SecretKey result = CipherUtil.getSecureRandomKey(algoritm, keyBitSize);
        assertNotNull(result);
    }

    /**
     * Test of getSecureRandomKey method, of class CipherUtil.
     */
    @Test
    public void testGetSecureRandomKey_String() throws Exception {
        System.out.println("getSecureRandomKey");
        String algoritm = "Blowfish";
        SecretKey result = CipherUtil.getSecureRandomKey(algoritm);
        assertNotNull(result);
    }
    

    @Test
    public void testRSA() throws Exception {

        // Step 1: Come up with a message we want to encrypt
        String messageStr = "Hello, World!";
        byte[] message = messageStr.getBytes();

        // Step 2: Create a KeyGenerator object
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

        // Step 3: Initialize the KeyGenerator with a certain keysize
        keyPairGenerator.initialize(512);

        // Step 4: Generate the key pairs
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // Step 5: Extract the keys
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        //Desde byte
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
        PublicKey publicKey2 = keyFactory.generatePublic(publicKeySpec);        
        assertEquals(publicKey, publicKey2);

        //Desde hexadecimal
        String publicKeyHex = Fn.bytesToHex(publicKey.getEncoded());
        publicKeySpec = new X509EncodedKeySpec(Fn.hexToByte(publicKeyHex));
        PublicKey publicKey3 = keyFactory.generatePublic(publicKeySpec);
        assertEquals(publicKey, publicKey3);

        //Desde base64
        String publicKeyBase64 = Fn.bytesToBase64(publicKey.getEncoded());
        publicKeySpec = new X509EncodedKeySpec(Fn.base64ToBytes(publicKeyBase64));
        PublicKey publicKey4 = keyFactory.generatePublic(publicKeySpec);
        assertEquals(publicKey, publicKey4);
        
        // Step 6: Create a Cipher object
        Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");

        // Step 7: Initialize the Cipher object
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        // Step 8: Give the Cipher our message
        cipher.update(message);

        // Step 9: Encrypt the message
        byte[] ciphertext = cipher.doFinal();

        // Step 10: Print the ciphertext
        System.out.println("message: " + new String(message, "UTF8"));
        System.out.println("ciphertext: " + new String(ciphertext, "UTF8"));

        
        // Step 11: Change the Cipher object's mode
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        // Step 12: Give the Cipher objectour ciphertext
        cipher.update(ciphertext);

        // Step 13: Decrypt the ciphertext
        byte[] decrypted = cipher.doFinal();
        String messageStrDecrypted = new String(decrypted);
        System.out.println("decrypted: " + messageStrDecrypted);                
        assertTrue(messageStr.equals(messageStrDecrypted));
    }

    @Test
    public void testRSA2() throws Exception {
        String message = "Hello, World!";
        //
        KeyPair keyPair = CipherUtil.generateRSAKeyPair(1024);
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        //
        byte[] ciphertext = CipherUtil.encryptRSA(message, publicKey);
        // Decrypt the ciphertext
        byte[] decrypted = CipherUtil.decryptRSA(ciphertext, privateKey);
        //
        String messageDecrypted = new String(decrypted);
        System.out.println("decrypted: " + messageDecrypted);                
        assertTrue(message.equals(messageDecrypted));
    }
    
}

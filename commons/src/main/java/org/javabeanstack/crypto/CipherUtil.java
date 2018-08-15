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

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.javabeanstack.util.Fn;

/**
 * Clase que abstrae funciones de encriptación. Utiliza los algoritmos
 * simetricos Blowfish y Aes
 *
 * @author Jorge Enciso
 */
public class CipherUtil {

    public static final String BLOWFISH = "Blowfish";
    public static final String AES_CBC = "AES/CBC/PKCS5Padding";

    /**
     * Encripta un texto utilizando el algoritmo Blowfish y devuelve el
     * resultado en formato string hexadecimal.
     *
     * @param clearText texto a encriptar
     * @param key clave de encriptación
     * @return texto encriptado
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static String encryptBlowfishToHex(String clearText, String key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
        return encryptToHex(CipherUtil.BLOWFISH, clearText, key);
    }

    /**
     * Encripta un texto utilizando el algoritmo Blowfish y devuelve el
     * resultado en formato string base 64.
     *
     * @param clearText texto a encriptar
     * @param key clave de encriptación
     * @return texto encriptado
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static String encryptBlowfishToBase64(String clearText, String key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
        return encryptToBase64(CipherUtil.BLOWFISH, clearText, key);
    }

    /**
     * Desencripta un mensaje cifrado con el algoritmo blowfish, el mensaje esta
     * en formato string hexadecimal.
     *
     * @param encrypted mensaje encriptado
     * @param key clave de encriptación
     * @return mensaje descifrado.
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static String decryptBlowfishFromHex(String encrypted, String key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
        return decryptFromHex(CipherUtil.BLOWFISH, encrypted, key);
    }

   
    /**
     * Desencripta un mensaje cifrado con el algoritmo blowfish, el mensaje esta
     * en formato string base 64.
     *
     * @param encrypted mensaje encriptado
     * @param key clave de encriptación
     * @return mensaje descifrado.
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static String decryptBlowfishFromBase64(String encrypted, String key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
        return decryptFromBase64(CipherUtil.BLOWFISH, encrypted, key);
    }

    /**
     * Cifra un mensaje utilizando el algoritmo AES.
     *
     * @param clearText mensaje
     * @param key clave de cifrado.
     * @return mensaje cifrado.
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static byte[] encryptAES(String clearText, String key) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        return encrypt_AES_CBC(clearText, key);
    }

    /**
     * Encripta un texto utilizando el algoritmo AES y devuelve el resultado en
     * formato string hexadecimal.
     *
     * @param clearText texto a encriptar
     * @param key clave de encriptación
     * @return texto encriptado
     * @throws java.io.UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws java.security.InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static String encryptAES_ToHex(String clearText, String key) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        return Fn.bytesToHex(encrypt_AES_CBC(clearText, key));
    }

    /**
     * Encripta un texto utilizando el algoritmo AES y devuelve el resultado en
     * formato string base 64.
     *
     * @param clearText texto a encriptar
     * @param key clave de encriptación
     * @return texto encriptado
     * @throws java.io.UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws java.security.InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static String encryptAES_ToBase64(String clearText, String key) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        return Fn.bytesToBase64(encrypt_AES_CBC(clearText, key));
    }

    /**
     * Desencripta un mensaje cifrado con el algoritmo AES.
     *
     * @param encrypted mensaje encriptado
     * @param key clave de encriptación
     * @return mensaje descifrado.
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws java.security.InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static String decryptAES(byte[] encrypted, String key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        return new String(decrypt_AES_CBC(encrypted, key));
    }

    /**
     * Desencripta un mensaje cifrado en formato hexadecimal con el algoritmo
     * AES.
     *
     * @param encryptedHex mensaje encriptado en formato hexadecimal.
     * @param key clave de encriptación
     * @return mensaje descifrado.
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws java.security.InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static String decryptAES_FromHex(String encryptedHex, String key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        return new String(decrypt_AES_CBC(Fn.hexToByte(encryptedHex), key));
    }

    /**
     * Encripta un mensaje
     *
     * @param algorithm tipo de algoritmo a utilizar
     * @param clearText mensaje
     * @param key clave de encriptación
     * @return mensaje cifrado en formato byte
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    private static byte[] encryptToByte(String algorithm, String clearText, String key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
        SecretKeySpec keyspec = new SecretKeySpec(key.getBytes(), algorithm);
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, keyspec);
        return cipher.doFinal(clearText.getBytes());
    }


    /**
     * Encripta un mensaje
     *
     * @param algorithm tipo de algoritmo a utilizar
     * @param clearText mensaje
     * @param key clave de encriptación
     * @return mensaje cifrado en formato string hexadecimal
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    private static String encryptToHex(String algorithm, String clearText, String key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
        byte[] encrypted = encryptToByte(algorithm, clearText, key);
        return Fn.bytesToHex(encrypted);
    }

    /**
     * Encripta un mensaje
     *
     * @param algorithm tipo de algoritmo a utilizar
     * @param clearText mensaje
     * @param key clave de encriptación
     * @return mensaje cifrado en formato string base 64
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    private static String encryptToBase64(String algorithm, String clearText, String key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
        byte[] encrypted = encryptToByte(algorithm, clearText, key);
        return Fn.bytesToBase64(encrypted);
    }

    /**
     * Descifra un mensaje
     *
     * @param algorithm tipo de algoritmo a utilizar
     * @param encriptedText mensaje
     * @param key clave de encriptación
     * @return mensaje descifrado en formato byte
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    private static byte[] decryptFromByte(String algorithm, byte[] encryptedText, String key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
        SecretKeySpec keyspec = new SecretKeySpec(key.getBytes(), algorithm);
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, keyspec);
        return cipher.doFinal(encryptedText);
    }

    /**
     * Descifra un mensaje
     *
     * @param algorithm tipo de algoritmo a utilizar
     * @param encriptedText mensaje formato hexadecimal
     * @param key clave de encriptación
     * @return mensaje descifrado desde un string formato hexadecimal
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    private static String decryptFromHex(String algorithm, String encryptedHexText, String key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
        byte[] encrypted = Fn.hexToByte(encryptedHexText);
        byte[] result = decryptFromByte(algorithm, encrypted, key);
        return new String(result);
    }

    /**
     * Descifra un mensaje
     *
     * @param algorithm tipo de algoritmo a utilizar
     * @param encriptedText mensaje formato base 64
     * @param key clave de encriptación
     * @return mensaje descifrado desde formato base 64
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    private static String decryptFromBase64(String algorithm, String encryptedText, String key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
        byte[] encrypted = Fn.base64ToBytes(encryptedText);
        byte[] result = decryptFromByte(algorithm, encrypted, key);
        return new String(result);
    }

    /**
     * Encripta un mensaje utilizando el algoritmo AES complementado con la
     * técnica CBC
     *
     * @param plainText mensaje a cifrar
     * @param key clave para encriptar
     * @return mensaje cifrado en formato binario.
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws UnsupportedEncodingException
     */
    private static byte[] encrypt_AES_CBC(String plainText, String key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
        byte[] clean = plainText.getBytes();
        // Generating IV.
        int ivSize = 16;
        byte[] iv = new byte[ivSize];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // Hashing key.
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(key.getBytes());
        byte[] keyBytes = new byte[16];
        System.arraycopy(digest.digest(), 0, keyBytes, 0, keyBytes.length);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

        // Encrypt.
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
        byte[] encrypted = cipher.doFinal(clean);

        // Combine IV and encrypted part.
        byte[] encryptedIVAndText = new byte[ivSize + encrypted.length];
        System.arraycopy(iv, 0, encryptedIVAndText, 0, ivSize);
        System.arraycopy(encrypted, 0, encryptedIVAndText, ivSize, encrypted.length);

        return encryptedIVAndText;
    }

    /**
     * Descrifra un mensaje encriptado con el algoritmo AES/CBC
     *
     * @param encryptedIvTextBytes mensaje a desencriptar en formato binario
     * @param key clave de descifrado
     * @return mensaje desencriptado
     *
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    private static byte[] decrypt_AES_CBC(byte[] encryptedIvTextBytes, String key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        int ivSize = 16;
        int keySize = 16;

        // Extract IV.
        byte[] iv = new byte[ivSize];
        System.arraycopy(encryptedIvTextBytes, 0, iv, 0, iv.length);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // Extract encrypted part.
        int encryptedSize = encryptedIvTextBytes.length - ivSize;
        byte[] encryptedBytes = new byte[encryptedSize];
        System.arraycopy(encryptedIvTextBytes, ivSize, encryptedBytes, 0, encryptedSize);

        // Hash key.
        byte[] keyBytes = new byte[keySize];
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(key.getBytes());
        System.arraycopy(md.digest(), 0, keyBytes, 0, keyBytes.length);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

        // Decrypt.
        Cipher cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
        byte[] decrypted = cipherDecrypt.doFinal(encryptedBytes);

        return decrypted;
    }

    /**
     * Devuelve una clave simetrica aleatoria a ser utilizada en un proceso de cifrado
     * @param algoritm algoritmo (blowfish, aes etc)
     * @param keyBitSize tamaño de la clave
     * @return clave simetrica aleatoria
     * @throws NoSuchAlgorithmException 
     */
    public static SecretKey getSecureRandomKey(String algoritm, int keyBitSize) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(algoritm);
        SecureRandom secureRandom = new SecureRandom();
        keyGenerator.init(keyBitSize, secureRandom);
        return keyGenerator.generateKey();
    }
    
    /**
     * Devuelve una clave simetrica aleatoria a ser utilizada en un proceso de cifrado
     * @param algoritm algoritmo (blowfish, aes etc)
     * @return clave simetrica aleatoria
     * @throws NoSuchAlgorithmException 
     */
    public static SecretKey getSecureRandomKey(String algoritm) throws NoSuchAlgorithmException {
        return getSecureRandomKey(algoritm, 256);
    }
}

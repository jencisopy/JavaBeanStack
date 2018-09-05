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
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.javabeanstack.util.Fn;
import org.apache.log4j.Logger;

/**
 * 
 * @author Jorge Enciso
 */
public class DigestUtil {
    public static final String MD5 = "MD5";
    public static final String SHA1 = "SHA-1";
    public static final String SHA256 = "SHA-256";
    public static final String SHA384 = "SHA-384";
    public static final String SHA512 = "SHA-512";

    public static final String HMACMD5 = "HmacMD5";
    public static final String HMACSHA1 = "HmacSHA1";
    public static final String HMACSHA256 = "HmacSHA256";
    public static final String HMACSHA384 = "HmacSHA384";
    public static final String HMACSHA512 = "HmacSHA512";
    
    /**
     * Devuelve la firma md5 en formato String hexadecimal
     * @param msg mensaje
     * @return firma md5 en formato String hexadecimal
     */
    public static String md5(String msg)  {
        try {
            return digestToHex("MD5", msg);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            Logger.getLogger(DigestUtil.class).error(ex.getMessage());                        
        }
        return null;
    }

    /**
     * Devuelve la firma sha256 en formato String hexadecimal
     * @param msg mensaje
     * @return firma 256 en formato String hexadecimal
     */
    public static String sha256(String msg){
        try {
            return digestToHex("SHA-256", msg);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            Logger.getLogger(DigestUtil.class).error(ex.getMessage());
        }
        return null;
    }

    /**
     * Devuelve la firma sha-512 en formato String hexadecimal
     * @param msg mensaje
     * @return firma sha-512 en formato String hexadecimal
     */
    public static String sha512(String msg) {
        try {
            return digestToHex("SHA-512", msg);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            Logger.getLogger(DigestUtil.class).error(ex.getMessage());
        }
        return null;
    }

    /**
     * Devuelve una firma dependiendo del algoritmo solicitado en formato String hexadecimal
     * @param algorithm md5, sha1, sha256, sha384, sha512
     * @param msg mensaje
     * @return firma en formato String hexadecimal
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException 
     */
    public static String digestToHex(String algorithm, String msg) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        byte[] digestMessage = digest.digest(msg.getBytes("UTF-8"));
        return Fn.bytesToHex(digestMessage);
    }

    /**
     * Firma un mensaje y devuelve la firma en formato string hexadecimal
     * @param algorithm tipo de algoritmo.(HmacSHA1,HmacSHA256,HmacSHA384,HmacSHA512,HmacMD5)
     * @param msg mensaje a firmar
     * @param privateKey clave privada
     * @return firma en formato string hexadecimal.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @throws InvalidKeyException 
     */
    public static String digestHmacToHex(String algorithm, String msg, byte[] privateKey) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
        Mac mac = Mac.getInstance(algorithm);
        SecretKeySpec key = new SecretKeySpec(privateKey, algorithm);
        mac.init(key);
        byte[] macBytes = mac.doFinal(msg.getBytes("UTF-8"));
        return Fn.bytesToHex(macBytes);
    }
    
    /**
     * Firma un mensaje y devuelve la firma en formato string base 64 url
     * @param algorithm tipo de algoritmo.(HmacSHA1,HmacSHA256, HmacSHA384, HmacSHA512,HmacMD5)
     * @param msg mensaje a firmar
     * @param privateKey clave privada
     * @return firma en formato string base 64 url.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @throws InvalidKeyException 
     */
    public static String digestHmacToBase64Url(String algorithm, String msg, byte[] privateKey) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
        Mac mac = Mac.getInstance(algorithm);
        SecretKeySpec key = new SecretKeySpec(privateKey, algorithm);
        mac.init(key);
        byte[] macBytes = mac.doFinal(msg.getBytes("UTF-8"));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(macBytes);
    }

    /**
     * Firma un mensaje y devuelve la firma en formato string base 64
     * @param algorithm tipo de algoritmo.(HmacSHA1,HmacSHA256,HmacSHA384, HmacSHA512,HmacMD5)
     * @param msg mensaje a firmar
     * @param privateKey clave privada
     * @return firma en formato string base 64.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @throws InvalidKeyException 
     */
    public static String digestHmacToBase64(String algorithm, String msg, byte[] privateKey) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
        Mac mac = Mac.getInstance(algorithm);
        SecretKeySpec key = new SecretKeySpec(privateKey, algorithm);
        mac.init(key);
        byte[] macBytes = mac.doFinal(msg.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(macBytes);
    }


    /**
     * Devuelve la firma md5 en formato String hexadecimal
     * @param msg mensaje
     * @param key clave privada
     * @return firma en formato String hexadecimal
     */
    public static String hmacMD5(String msg, String key) {
        try {
            return digestHmacToHex("HmacMD5", msg, key.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException ex) {
            Logger.getLogger(DigestUtil.class).error(ex.getMessage());
        }
        return null;
    }

    
    /**
     * Devuelve la firma sha1 en formato String hexadecimal
     * @param msg mensaje
     * @param key clave privada
     * @return firma en formato String hexadecimal
     */
    public static String hmacSHA1(String msg, String key) {
        try {
            return digestHmacToHex("HmacSHA1", msg, key.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException ex) {
            Logger.getLogger(DigestUtil.class).error(ex.getMessage());
        }
        return null;
    }

    /**
     * Devuelve la firma sha1 en formato String base 64
     * @param msg mensaje
     * @param key clave privada
     * @return firma en formato String base 64
     */
    public static String hmacSHA1ToBase64(String msg, String key) {
        try {
            return digestHmacToBase64("HmacSHA1", msg, key.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException ex) {
            Logger.getLogger(DigestUtil.class).error(ex.getMessage());
        }
        return null;
    }

    /**
     * Devuelve la firma sha256 en formato String hexadecimal
     * @param msg mensaje
     * @param key clave privada
     * @return firma en formato String hexadecimal
     */
    public static String hmacSHA256(String msg, String key) {
        try {
            return digestHmacToHex("HmacSHA256", msg, key.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException ex) {
            Logger.getLogger(DigestUtil.class).error(ex.getMessage());
        }
        return null;
    }

    /**
     * Devuelve la firma sha256 en formato String base 64
     * @param msg mensaje
     * @param key clave privada
     * @return firma en formato String base 64
     */
    public static String hmacSHA256ToBase64(String msg, String key) {
        try {
            return digestHmacToBase64("HmacSHA256", msg, key.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException ex) {
            Logger.getLogger(DigestUtil.class).error(ex.getMessage());
        }
        return null;
    }
    
    /**
     * Devuelve la firma sha384 en formato String hexadecimal
     * @param msg mensaje
     * @param key clave privada
     * @return firma en formato String hexadecimal
     */
    public static String hmacSHA384(String msg, String key) {
        try {
            return digestHmacToHex("HmacSHA384", msg, key.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException ex) {
            Logger.getLogger(DigestUtil.class).error(ex.getMessage());
        }
        return null;
    }

    
    /**
     * Devuelve la firma sha384 en formato String base 64
     * @param msg mensaje
     * @param key clave privada
     * @return firma en formato String base 64
     */
    public static String hmacSHA384ToBase64(String msg, String key) {
        try {
            return digestHmacToBase64("HmacSHA384", msg, key.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException ex) {
            Logger.getLogger(DigestUtil.class).error(ex.getMessage());
        }
        return null;
    }
    
    /**
     * Devuelve la firma sha512 en formato String hexadecimal
     * @param msg mensaje
     * @param key clave privada
     * @return firma en formato String hexadecimal
     */
    public static String hmacSHA512(String msg, String key) {
        try {
            return digestHmacToHex("HmacSHA512", msg, key.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException ex) {
            Logger.getLogger(DigestUtil.class).error(ex.getMessage());
        }
        return null;
    }

    /**
     * Devuelve la firma sha512 en formato String base 64
     * @param msg mensaje
     * @param key clave privada
     * @return firma en formato String base 64
     */
    public static String hmacSHA512ToBase64(String msg, String key) {
        try {
            return digestHmacToBase64("HmacSHA512", msg, key.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException ex) {
            Logger.getLogger(DigestUtil.class).error(ex.getMessage());
        }
        return null;
    }
}

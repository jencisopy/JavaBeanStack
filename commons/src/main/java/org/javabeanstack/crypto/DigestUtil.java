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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
}

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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jorge Enciso
 */
public class DigestUtilTest {
    public DigestUtilTest() {
    }
    
    /**
     * Test of md5 method, of class DigestUtil.
     */
    @Test
    public void testMd5() throws Exception {
        System.out.println("md5");
        String msg = "abcdefghijklmnñopqrstuvwxyzáéíóú";
        String expResult = "5AD6F23DA25B3A54CD5AE716C401732D";
        String result = DigestUtil.md5(msg).toUpperCase();
        assertEquals(expResult, result);
    }

    /**
     * Test of sha256 method, of class DigestUtil.
     */
    @Test
    public void testSha256() throws Exception {
        System.out.println("sha256");
        String msg = "abcdefghijklmnñopqrstuvwxyzáéíóú";
        String expResult = "4D9DC750077F316B7A9D2D5C4890B1D79ABC7B7C4261D7090CBF58501E2F723B";
        String result = DigestUtil.sha256(msg).toUpperCase();
        System.out.println(result);
        assertEquals(expResult, result);
    }

    /**
     * Test of sha512 method, of class DigestUtil.
     */
    @Test
    public void testSha512() throws Exception {
        System.out.println("sha512");
        String msg = "abcdefghijklmnñopqrstuvwxyzáéíóú";
        String expResult = "8CABE293B89951C158AC9B56ECDD518060BE89CB760ED7AD04CF72B9F10EB38401A8959E6CFCD3146A6A02AC126D76B3DAD653500239EF8A6BB4106238B9C37C";
        String result = DigestUtil.sha512(msg).toUpperCase();
        System.out.println(result);
        assertEquals(expResult, result);
    }

    /**
     * Test of digestToHex method, of class DigestUtil.
     */
    @Test
    public void testDigestToHex() throws Exception {
        System.out.println("digestToHex");

        String msg = "abcdefghijklmnñopqrstuvwxyzáéíóú";
        String expResult = "8DD69CB915630B3DDE248FC13DBA97547BCED31E";
        String result = DigestUtil.digestToHex(DigestUtil.SHA1, msg).toUpperCase();
        assertEquals(expResult, result);
        //
        expResult = "5AD6F23DA25B3A54CD5AE716C401732D";
        result = DigestUtil.digestToHex(DigestUtil.MD5, msg).toUpperCase();
        assertEquals(expResult, result);
        //
        expResult = "4D9DC750077F316B7A9D2D5C4890B1D79ABC7B7C4261D7090CBF58501E2F723B";
        result = DigestUtil.digestToHex(DigestUtil.SHA256, msg).toUpperCase();
        assertEquals(expResult, result);
        //
        expResult = "868333E2AB3B4329D6656EE63B4CD77F03AA58EEB924F4920F5894BBF1340660FB1BEB591CD7D0A67478A0768FD4790A";
        result = DigestUtil.digestToHex(DigestUtil.SHA384, msg).toUpperCase();
        System.out.println(result);
        assertEquals(expResult, result);
        //
        expResult = "8CABE293B89951C158AC9B56ECDD518060BE89CB760ED7AD04CF72B9F10EB38401A8959E6CFCD3146A6A02AC126D76B3DAD653500239EF8A6BB4106238B9C37C";
        result = DigestUtil.digestToHex(DigestUtil.SHA512, msg).toUpperCase();
        assertEquals(expResult, result);
    }
    
    @Test
    public void testDigestAuth_MD5() throws Exception{
        System.out.println("DIGEST AUTH");
        //HA1=MD5(username:realm:password) md5
        //HA1=MD5(MD5(username:realm:password):nonce:cnonce)  md5-sess
        //HA2=MD5(method:digestURI)   
        //response=MD5(HA1:nonce:HA2)
        String nonce = "";
        String cnonce = "";
        
        String ha1 = DigestUtil.md5("admin:ldap:password");
        String ha2 = DigestUtil.md5("GET:/basic-auth");
        String response = DigestUtil.md5(ha1+":"+nonce+":"+ha2);
        
        String expResult = "e99498dd8f94775f92dffdebd06496b3";
        assertEquals(expResult, response);
    }
}

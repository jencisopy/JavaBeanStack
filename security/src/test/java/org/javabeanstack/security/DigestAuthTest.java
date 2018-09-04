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
package org.javabeanstack.security;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author JORGE
 */
public class DigestAuthTest {

    public DigestAuthTest() {
    }

    @Test
    public void testProperties() {
        
        String header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"nonceprueba\", uri=\"/rest/v2/name/eesti\", "
                + "qop=auth, nc=11, cnonce=\"prueba\", response=\"e2d568923b2cf97cc782e66f0049af6f\", opaque=\"\"";
        DigestAuth instance = new DigestAuth(header);

        String expected = "admin";
        assertEquals(expected, instance.getUsername());

        expected = "ldap";
        assertEquals(expected, instance.getRealm());

        expected = "nonceprueba";
        assertEquals(expected, instance.getNonce());

        expected = "/rest/v2/name/eesti";
        assertEquals(expected, instance.getUri());

        expected = "auth";
        assertEquals(expected, instance.getQop());

        expected = "11";
        assertEquals(expected, instance.getNonceCount());

        expected = "prueba";
        assertEquals(expected, instance.getCnonce());

        expected = "e2d568923b2cf97cc782e66f0049af6f";
        assertEquals(expected, instance.getResponse());

        expected = "";
        assertEquals(expected, instance.getOpaque());         
    }
   
    
    /**
     * Test of check method, of class DigestAuth.
     */
    @Test
    public void testCheck() {
        System.out.println("check");
        
        // Basic 
        String header = "Basic YWRtaW46cGFzc3dvcmQ=";
        DigestAuth instance = new DigestAuth(header);
        
        instance.setUsername("admin");
        instance.setPassword("password");
        boolean expResult = true;
        boolean result = true;
       // boolean result = instance.checkBasic();
        /*String response = instance.getResponse();
        System.out.print(response);*/
       // assertEquals(expResult, result);
        
        // MD5
        header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"\", uri=\"/digest-auth\", response=\"885d2f93dceb6077d8167e00d8111902\", opaque=\"\"";
        instance = new DigestAuth(header);
        instance.setPassword("password");
        result = instance.checkMD5();
        assertEquals(expResult, result);

        //MD5 + auth
        header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"\", uri=\"/digest-auth\", qop=auth, nc=, cnonce=\"\", response=\"7197e78bdbbf865c75d8d0b0e3e8466f\", opaque=\"\"";
        instance = new DigestAuth(header);
        instance.setPassword("password");
        result = instance.checkMD5();
        assertEquals(expResult, result);
        
        //MD5 + auth-int
//        header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"\", uri=\"/rest/v2/all\", qop=auth-int, nc=, cnonce=\"\", response=\"1d7c2f25767d3f1efdd85a409319894a\", opaque=\"\"";
//        instance = new DigestAuth(header);
//        instance.setPassword("password");
//        result = instance.checkMD5();
//        assertEquals(expResult, result);
        
        //MD5_sess
        header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"\", uri=\"/rest/v2/all\", nc=, cnonce=\"\", response=\"0f20da8e35fdf53cd4e730edbe4c992f\", opaque=\"\"";
        instance = new DigestAuth(header);
        instance.setPassword("password");
        result = instance.checkMD5_Sess();
        assertEquals(expResult, result);
        

        //MD5_sess + auth
        header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"\", uri=\"/rest/v2/all\", qop=auth, nc=, cnonce=\"\", response=\"d3a8fb5bb9ed6923a48bf32f8d6a3683\", opaque=\"\"";
        instance = new DigestAuth(header);
        instance.setPassword("password");
        result = instance.checkMD5_Sess();
        assertEquals(expResult, result);
        
        //MD5_sess + auth-int
//        header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"\", uri=\"/rest/v2/all\", qop=auth-int, nc=, cnonce=\"\", response=\"83a34e7b2c7c9a796782e86d157cdf23\", opaque=\"\"";
//        instance = new DigestAuth(header);
//        instance.setPassword("password");
//        result = instance.checkMD5_Sess();
//        assertEquals(expResult, result);
    }

    /**
     * Test of checkMD5 method, of class DigestAuth.
     */
    @Test
    public void testCheckMD5() {
        String header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"\","
                + " uri=\"/rest/v2/name/eesti\", response=\"f09cecf114d72dcb1ba99e02ac448687\", opaque=\"\"";
        DigestAuth instance = new DigestAuth(header);
        instance.setPassword("password");
        boolean expResult = true;
        boolean result = instance.checkMD5();
        assertEquals(expResult, result);
    }

    @Test
    public void testCheckMD5_auth() {
        System.out.println("checkMD5_auth");
        String header = "Digest username=\"admin\", realm=\"ldap\", uri=\"/rest/v2/name/eesti\", nonce=\"\",  qop=auth,"
                + " nc=, cnonce=\"\","
                + " response=\"959346459acf46fe3671a60a297e5e35\", opaque=\"\"";
        DigestAuth instance = new DigestAuth(header);
        instance.setPassword("password");
        boolean expResult = true;
        boolean result = instance.checkMD5();
        assertEquals(expResult, result);
    }

    @Test
    public void testCheckMD5_sess_auth() {
        System.out.println("checkMD5_sess_auth");
        String header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"\", uri=\"/rest/v2/name/eesti\", "
                + "qop=auth, nc=, cnonce=\"\", response=\"e2d568923b2cf97cc782e66f0049af6f\", opaque=\"\"";
        DigestAuth instance = new DigestAuth(header);
        instance.setPassword("password");
        boolean expResult = true;
        boolean result = instance.checkMD5_Sess();
        assertEquals(expResult, result);
    }
}



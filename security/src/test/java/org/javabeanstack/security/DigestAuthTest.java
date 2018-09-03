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

//import org.javabeanstack.crypto.DigestUtil;
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
    public void testProperties(){
       String header = "username=\"admin\", realm=\"ldap\", nonce=\"\", uri=\"/rest/v2/name/eesti\", "
                + "qop=auth, nc=, cnonce=\"\", response=\"e2d568923b2cf97cc782e66f0049af6f\", opaque=\"\"";
        DigestAuth instance = new DigestAuth(header);
        
        String expected = "admin";
        assertEquals(expected, instance.getUsername());
        
        expected = "ldap";
        assertEquals(expected, instance.getRealm());
        
        expected = "";
        assertEquals(expected, instance.getNonce());
        
        expected = "/rest/v2/name/eesti";
        assertEquals(expected, instance.getUri());
        
        expected = "auth";
        assertEquals(expected, instance.getQop());
        
        expected = "";
        assertEquals(expected, instance.getNonceCount());
        
        expected = "";
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
       // DigestAuth instance = new DigestAuth();
        boolean expResult = false;
        //boolean result = instance.check();
        //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
            
    /**
     * Test of checkMD5 method, of class DigestAuth.
     */
    @Test
    public void testCheckMD5() {
       String header = "username=\"admin\", realm=\"ldap\", nonce=\"\","
               + " uri=\"/rest/v2/name/eesti\", response=\"f09cecf114d72dcb1ba99e02ac448687\", opaque=\"\"";
        DigestAuth instance = new DigestAuth(header);
        boolean expResult = true;
        boolean result = instance.checkMD5();
        String response = instance.getResponse();
        //System.out.println(response);
        assertEquals(expResult, result);
    }
    
    @Test
    public void testCheckMD5_auth() {
        //System.out.println("checkMD5");
        String header = "username=\"admin\", realm=\"ldap\", uri=\"/rest/v2/name/eesti\", nonce=\"\",  qop=auth,"
                + " nc=, cnonce=\"\","
                + " response=\"959346459acf46fe3671a60a297e5e35\", opaque=\"\"";
        DigestAuth instance = new DigestAuth(header);
        boolean expResult = true;
        boolean result = instance.checkMD5_auth();
        assertEquals(expResult, result);
    }
    @Test
    public void testCheckMD5_sess_auth() {
        //System.out.println("checkMD5");
        String header = "username=\"admin\", realm=\"ldap\", nonce=\"\", uri=\"/rest/v2/name/eesti\", "
                + "qop=auth, nc=, cnonce=\"\", response=\"e2d568923b2cf97cc782e66f0049af6f\", opaque=\"\"";
        DigestAuth instance = new DigestAuth(header);
        boolean expResult = true;
        boolean result = instance.checkMD5_sess_auth();
        assertEquals(expResult, result);
    }
    /**
     * Test of checkMD5_Sess method, of class DigestAuth.
     */
    
}
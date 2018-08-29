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

    /**
     * Test of check method, of class DigestAuth.
     */
    @Test
    public void testCheck() {
        System.out.println("check");
        DigestAuth instance = new DigestAuth();
        boolean expResult = false;
        boolean result = instance.check();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of checkMD5 method, of class DigestAuth.
     */
    @Test
    public void testCheckMD5() {
        System.out.println("checkMD5");
        String header = "username=\"admin\", realm=\"ldap\", nonce=\"\", uri=\"/rest/v2/all\", qop=auth-int, nc=, cnonce=\"\", response=\"0b81376cdc50c54e3240db9bd9b20f79\", opaque=\"\"";
        DigestAuth instance = new DigestAuth(header);
        boolean expResult = false;
        boolean result = instance.checkMD5();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of checkMD5_Sess method, of class DigestAuth.
     */
    @Test
    public void testCheckMD5_Sess() {
        System.out.println("checkMD5_Sess");
        DigestAuth instance = new DigestAuth();
        boolean expResult = false;
        boolean result = instance.checkMD5_Sess();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getUsername method, of class DigestAuth.
     */
    @Test
    public void testGetUsername() {
        System.out.println("getUsername");
        DigestAuth instance = new DigestAuth();
        String expResult = "";
        String result = instance.getUsername();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setUsername method, of class DigestAuth.
     */
    @Test
    public void testSetUsername() {
        System.out.println("setUsername");
        String username = "";
        DigestAuth instance = new DigestAuth();
        instance.setUsername(username);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getRealm method, of class DigestAuth.
     */
    @Test
    public void testGetRealm() {
        System.out.println("getRealm");
        DigestAuth instance = new DigestAuth();
        String expResult = "";
        String result = instance.getRealm();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setRealm method, of class DigestAuth.
     */
    @Test
    public void testSetRealm() {
        System.out.println("setRealm");
        String realm = "";
        DigestAuth instance = new DigestAuth();
        instance.setRealm(realm);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getNonce method, of class DigestAuth.
     */
    @Test
    public void testGetNonce() {
        System.out.println("getNonce");
        DigestAuth instance = new DigestAuth();
        String expResult = "";
        String result = instance.getNonce();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setNonce method, of class DigestAuth.
     */
    @Test
    public void testSetNonce() {
        System.out.println("setNonce");
        String nonce = "";
        DigestAuth instance = new DigestAuth();
        instance.setNonce(nonce);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCnonce method, of class DigestAuth.
     */
    @Test
    public void testGetCnonce() {
        System.out.println("getCnonce");
        DigestAuth instance = new DigestAuth();
        String expResult = "";
        String result = instance.getCnonce();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setCnonce method, of class DigestAuth.
     */
    @Test
    public void testSetCnonce() {
        System.out.println("setCnonce");
        String cnonce = "";
        DigestAuth instance = new DigestAuth();
        instance.setCnonce(cnonce);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getNonceCount method, of class DigestAuth.
     */
    @Test
    public void testGetNonceCount() {
        System.out.println("getNonceCount");
        DigestAuth instance = new DigestAuth();
        String expResult = "";
        String result = instance.getNonceCount();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setNonceCount method, of class DigestAuth.
     */
    @Test
    public void testSetNonceCount() {
        System.out.println("setNonceCount");
        String nonceCount = "";
        DigestAuth instance = new DigestAuth();
        instance.setNonceCount(nonceCount);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getMethod method, of class DigestAuth.
     */
    @Test
    public void testGetMethod() {
        System.out.println("getMethod");
        DigestAuth instance = new DigestAuth();
        String expResult = "";
        String result = instance.getMethod();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setMethod method, of class DigestAuth.
     */
    @Test
    public void testSetMethod() {
        System.out.println("setMethod");
        String method = "";
        DigestAuth instance = new DigestAuth();
        instance.setMethod(method);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getUri method, of class DigestAuth.
     */
    @Test
    public void testGetUri() {
        System.out.println("getUri");
        DigestAuth instance = new DigestAuth();
        String expResult = "";
        String result = instance.getUri();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setUri method, of class DigestAuth.
     */
    @Test
    public void testSetUri() {
        System.out.println("setUri");
        String uri = "";
        DigestAuth instance = new DigestAuth();
        instance.setUri(uri);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getQop method, of class DigestAuth.
     */
    @Test
    public void testGetQop() {
        System.out.println("getQop");
        DigestAuth instance = new DigestAuth();
        String expResult = "";
        String result = instance.getQop();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setQop method, of class DigestAuth.
     */
    @Test
    public void testSetQop() {
        System.out.println("setQop");
        String qop = "";
        DigestAuth instance = new DigestAuth();
        instance.setQop(qop);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getOpaque method, of class DigestAuth.
     */
    @Test
    public void testGetOpaque() {
        System.out.println("getOpaque");
        DigestAuth instance = new DigestAuth();
        String expResult = "";
        String result = instance.getOpaque();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setOpaque method, of class DigestAuth.
     */
    @Test
    public void testSetOpaque() {
        System.out.println("setOpaque");
        String opaque = "";
        DigestAuth instance = new DigestAuth();
        instance.setOpaque(opaque);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}

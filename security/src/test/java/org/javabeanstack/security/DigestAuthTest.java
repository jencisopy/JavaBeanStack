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

import org.javabeanstack.crypto.DigestUtil;
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
        String header = "username=\"admin\", realm=\"ldap\", nonce=\"\", uri=\"/rest/v2/all\", qop=auth-int, nc=, cnonce=\"\", response=\"b81f6922b4282bbe24620a4800cb3f08\", opaque=\"\"";
        DigestAuth instance = new DigestAuth(header);
        String expResult = header.split(",")[7].split("=")[1]; //guarda el response esperado
        //String result = ;
         //HA1=MD5(username:realm:password) md5 HA2=MD5(METHOD:URI)
        String ha1 = DigestUtil.md5(instance.getUsername() + ":" + instance.getRealm() + ":password");
        String ha2 = DigestUtil.md5(instance.getMethod()+":"+instance.getUri());
        String result = DigestUtil.md5(ha1 + ":" + instance.getNonce() + ":" + instance.getNonceCount() + ":" +
                instance.getCnonce() + ":" + instance.getQop() + ":"+ha2);
        System.out.println(expResult);
        System.out.println(result);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
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
}

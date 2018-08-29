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
        //System.out.println("checkMD5");
        String header = "username=\"admin\", realm=\"ldap\", nonce=\"\", uri=\"/rest/v2/name/eesti\", qop=auth,"
                + " nc=, cnonce=\"\","
                + " response=\"959346459acf46fe3671a60a297e5e35\", opaque=\"\"";
        header = header.replace("\"", "");
        DigestAuth instance = new DigestAuth(header);
        String[] partHeader = header.split(",");
        String expResult = partHeader[7].split("=",2)[1].replace("\"", "");
        //guarda el response esperado
        String result = instance.getResponse();
        
        System.out.println(expResult);
        System.out.println(result);
        assertEquals(expResult, result);
       
    }
    /**
     * Test of checkMD5_Sess method, of class DigestAuth.
     */
    @Test
    public void testCheckMD5_Sess() {
        System.out.println("checkMD5_Sess");
        //DigestAuth instance = new DigestAuth();
        boolean expResult = false;
        //boolean result = instance.checkMD5_Sess();
        //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
}

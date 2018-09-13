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

import org.javabeanstack.security.model.ClientAuth;
import org.javabeanstack.security.model.ServerAuth;
import org.javabeanstack.util.Strings;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jorge Enciso
 */
public class DigestAuthTest {
    public DigestAuthTest() {
    }
   
    @Test
    public void testCheckWrong(){
        System.out.println("check fallidos");
        String header = "Digest username=\"adminerror\", realm=\"ldap\", nonce=\"\", uri=\"/digest-auth\", response=\"885d2f93dceb6077d8167e00d8111902\", opaque=\"\"";
        DigestAuth auth = new DigestAuth();
        auth.createResponseAuth(DigestAuth.DIGEST,"","ldap");        
        auth.getResponseAuth("").setUsername("admin");
        auth.getResponseAuth("").setPassword("password");
        ClientAuth requestAuth = new ClientAuth("GET",header,"");        
        // Equivocarse al proposito más de las 10 veces permitidas
        for (int i = 0;i <= 10;i++){
            auth.check(requestAuth);
        }
        assertNull(auth.getResponseAuth(""));
    }
    
    /**
     * Test of check method, of class DigestAuth.
     */
    @Test
    public void testCheck() {
        System.out.println("check");

        boolean expResult = true;
        DigestAuth auth = new DigestAuth();
        auth.getResponseAuth("basic").setUsername("admin");
        auth.getResponseAuth("basic").setPassword("password");

        // Basic 
        String header = "Basic YWRtaW46cGFzc3dvcmQ=";
        ClientAuth requestAuth = new ClientAuth("GET",header,"");
        boolean result = auth.checkBasic(requestAuth);
        assertEquals(expResult, result);
        
        // MD5
        header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"\", uri=\"/digest-auth\", response=\"885d2f93dceb6077d8167e00d8111902\", opaque=\"\"";
        auth.createResponseAuth(DigestAuth.DIGEST,"","ldap");        
        auth.getResponseAuth("").setUsername("admin");
        auth.getResponseAuth("").setPassword("password");
        requestAuth = new ClientAuth("GET",header,"");        
        result = auth.checkMD5(requestAuth);
        assertEquals(expResult, result);

        //MD5 + auth
        header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"\", uri=\"/digest-auth\", qop=auth, nc=, cnonce=\"\", response=\"7197e78bdbbf865c75d8d0b0e3e8466f\", opaque=\"\"";
        requestAuth = new ClientAuth("GET",header,"");
        result = auth.checkMD5(requestAuth);
        assertEquals(expResult, result);
        
        //MD5 + auth-int
        header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"\", uri=\"/digest-auth\", qop=auth-int, nc=, cnonce=\"\", response=\"00f1eed3553116bc77e5d9721cf746b6\", opaque=\"\"";
        requestAuth = new ClientAuth("POST",header,"prueba entity body\n");
        result = auth.checkMD5(requestAuth);
        assertEquals(expResult, result);
        
        //MD5_sess
        header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"\", uri=\"/rest/v2/all\", nc=, cnonce=\"\", response=\"0f20da8e35fdf53cd4e730edbe4c992f\", opaque=\"\"";
        requestAuth = new ClientAuth("GET",header,"");        
        result = auth.checkMD5_Sess(requestAuth);
        assertEquals(expResult, result);
        
        //MD5_sess + auth
        header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"\", uri=\"/rest/v2/all\", qop=auth, nc=, cnonce=\"\", response=\"d3a8fb5bb9ed6923a48bf32f8d6a3683\", opaque=\"\"";
        requestAuth = new ClientAuth("GET",header,"");
        result = auth.checkMD5_Sess(requestAuth);
        assertEquals(expResult, result);
        
        //MD5_sess + auth-int
        header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"\", uri=\"/digest-auth\", qop=auth-int, nc=, cnonce=\"\", response=\"ae5f7169f53d646ae4b34b4a2904f45a\", opaque=\"\"";
        requestAuth = new ClientAuth("POST",header,"prueba entity body\n");
        result = auth.checkMD5_Sess(requestAuth);
        assertEquals(expResult, result);
        
        //Check Generico
        result = auth.check(requestAuth);
        assertEquals(expResult, result);
        // Una vez realizado el check elimina el objeto autenticador de la lista de los válidos
        // si es tipo "Digest"
        result = auth.check(requestAuth);
        assertFalse(result);
        
    }

    /**
     * Test of checkMD5 method, of class DigestAuth.
     */
    @Test
    public void testCheckMD5() {
        String header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"f09cecf114d72dcb1ba99e02ac448687\", "
                + "uri=\"/digest-auth\", response=\"aac60f90b7712c712735645b9cbbc767\", opaque=\"\"";
        DigestAuth auth = new DigestAuth();
        auth.createResponseAuth(DigestAuth.DIGEST,"f09cecf114d72dcb1ba99e02ac448687","ldap");
        auth.getResponseAuth("f09cecf114d72dcb1ba99e02ac448687").setUsername("admin");
        auth.getResponseAuth("f09cecf114d72dcb1ba99e02ac448687").setPassword("password");
        
        ClientAuth requestAuth = new ClientAuth("GET",header,"");
        boolean expResult = true;
        boolean result = auth.checkMD5(requestAuth);
        assertEquals(expResult, result);
    }

    /**
     * Test of checkMD5_auth method, of class DigestAuth.
     */
    @Test
    public void testCheckMD5_auth() {
        System.out.println("checkMD5_auth");
        String header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"f09cecf114d72dcb1ba99e02ac448687\", "
                + "uri=\"/digest-auth\", qop=auth, nc=00000000, cnonce=\"f09cecf114d72dcb1ba99e02ac448688\", "
                + "response=\"74a2827a07111f627cf619e66860739f\", opaque=\"\"";
        DigestAuth auth = new DigestAuth();
        auth.createResponseAuth(DigestAuth.DIGEST,"f09cecf114d72dcb1ba99e02ac448687","ldap");
        auth.getResponseAuth("f09cecf114d72dcb1ba99e02ac448687").setUsername("admin");
        auth.getResponseAuth("f09cecf114d72dcb1ba99e02ac448687").setPassword("password");
        
        ClientAuth requestAuth = new ClientAuth("GET",header,"");        
        boolean expResult = true;
        boolean result = auth.checkMD5(requestAuth);
        assertEquals(expResult, result);
    }

    /**
     * Test of checkMD5_sess_auth method, of class DigestAuth.
     */
    @Test
    public void testCheckMD5_sess_auth() {
        System.out.println("checkMD5_sess_auth");
        String header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"f09cecf114d72dcb1ba99e02ac448687\", "
                + "uri=\"/digest-auth\", qop=auth, nc=00000000, cnonce=\"f09cecf114d72dcb1ba99e02ac448688\", "
                + "response=\"68374c0fb98f888c11902a7122f233c4\", opaque=\"\"";
        DigestAuth auth = new DigestAuth();
        auth.createResponseAuth(DigestAuth.DIGEST,"f09cecf114d72dcb1ba99e02ac448687","ldap");
        auth.getResponseAuth("f09cecf114d72dcb1ba99e02ac448687").setUsername("admin");
        auth.getResponseAuth("f09cecf114d72dcb1ba99e02ac448687").setPassword("password");
        
        ClientAuth requestAuth = new ClientAuth("GET",header,"");        
        boolean expResult = true;
        boolean result = auth.checkMD5_Sess(requestAuth);
        assertEquals(expResult, result);
    }
    

    /**
     * Test of getResponseHeader method, of class DigestAuth.
     */
    @Test
    public void testGetResponseHeader(){
        DigestAuth auth = new DigestAuth(DigestAuth.BASIC, "", "");
        String expResult = "Basic realm=\"Restricted\"";
        ServerAuth serverAuth = auth.createResponseAuth();
        String result = auth.getResponseHeader(serverAuth);
        assertEquals(expResult, result);
        
        auth = new DigestAuth(DigestAuth.DIGEST, "ldap", "");
        serverAuth = auth.createResponseAuth();
        result = auth.getResponseHeader(serverAuth);
        expResult = "Digest realm=\""+auth.getRealm()
                + "\" qop=\""+auth.getQop()
                + "\" nonce=\""+serverAuth.getNonce()
                + "\" opaque=\""+serverAuth.getOpaque();

        assertEquals(expResult, Strings.left(result, expResult.length()));

        auth = new DigestAuth(DigestAuth.DIGEST, "ldap", "auth,auth-int");
        serverAuth = auth.createResponseAuth();
        result = auth.getResponseHeader(serverAuth);
        expResult = "Digest realm=\""+auth.getRealm()
                + "\" qop=\""+auth.getQop()
                + "\" nonce=\""+serverAuth.getNonce()
                + "\" opaque=\""+serverAuth.getOpaque();

        assertEquals(expResult, Strings.left(result, expResult.length()));
    }
}

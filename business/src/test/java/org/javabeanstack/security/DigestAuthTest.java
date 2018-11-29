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


import java.util.ArrayList;
import java.util.List;
import org.javabeanstack.security.exceptions.TypeAuthInvalid;
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
    public void testCheckWrong() throws Exception{
        System.out.println("check fallidos");
        String header = "Digest username=\"adminerror\", realm=\"ldap\", nonce=\"\", uri=\"/digest-auth\", response=\"885d2f93dceb6077d8167e00d8111902\", opaque=\"\"";
        DigestAuth auth = new DigestAuth();
        auth.createResponseAuth(DigestAuth.DIGEST,"","ldap");        
        auth.getResponseAuth("").setUsername("admin");
        auth.getResponseAuth("").setPassword("password");
        ClientAuth requestAuth = new ClientAuth("GET",header,"");        
        // Equivocarse al proposito más de las 10 veces permitidas
        // Por defecto se puede equivocar 10 veces
        for (int i = 0;i <= 10;i++){
            auth.check(requestAuth);
        }
        assertNull(auth.getResponseAuth(""));
        
        // Basic 
        header = "Basic YWRtaW46cGFzc3dvcmQ=";
        auth.createResponseAuth(DigestAuth.BASIC,"admin","");
        auth.getResponseAuth("admin").setUsername("admin");
        auth.getResponseAuth("admin").setPassword("passwordwrong");
        requestAuth = new ClientAuth("GET",header,"");
        // Equivocarse al proposito más de las 10 veces permitidas
        // Por defecto se puede equivocar 10 veces
        for (int i = 0;i <= 10;i++){
            auth.check(requestAuth);
        }
        assertNull(auth.getResponseAuth("basic admin"));        
    }
    
    /**
     * Test of check method, of class DigestAuth.
     */
    @Test
    public void testCheck() throws Exception {
        System.out.println("check");

        boolean expResult = true;
        DigestAuth auth = new DigestAuth();

        // Basic 
        String header = "Basic YWRtaW46cGFzc3dvcmQ=";
        auth.createResponseAuth(DigestAuth.BASIC,"admin","");
        auth.getResponseAuth("admin").setUsername("admin");
        auth.getResponseAuth("admin").setPassword("password");
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
        header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"\", uri=\"/digest-auth\", qop=auth-int, nc=, cnonce=\"\", response=\"d638e3ff4888119971d73d73005759b1\", opaque=\"\"";
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
        header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"\", uri=\"/digest-auth\", qop=auth-int, nc=, cnonce=\"\", response=\"976f31cf01fbeca7a277326e0085ec92\", opaque=\"\"";
        requestAuth = new ClientAuth("POST",header,"prueba entity body\n");
        result = auth.checkMD5_Sess(requestAuth);
        assertEquals(expResult, result);
        
        //Check Generico
        result = auth.check(requestAuth);
        assertEquals(expResult, result);
        // Una vez realizado el check exitoso se elimina el objeto autenticador de la lista de los válidos
        result = auth.check(requestAuth);
        assertFalse(result);
    }

    /**
     * Test of checkMD5 method, of class DigestAuth.
     */
    @Test
    public void testCheckMD5() throws Exception{
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
    public void testCheckMD5_auth() throws Exception{
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
    public void testCheckMD5_sess_auth() throws Exception{
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
    public void testGetResponseHeader() throws Exception{
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
    
    /**
     * Test metodo purgeResponseAuth()
     * @throws InterruptedException 
     */
    @Test
    public void purgeResponseAuth() throws Exception{
        System.out.println("purgeResponseAuth");        
        DigestAuth auth = new DigestAuth();
        auth.setSecondsIdle(1); // 1 segundo de vida
        
        // Probar con basic
        for (int i = 0;i <= 500;i++){
            auth.createResponseAuth(DigestAuth.BASIC,""+i,"");                    
        }
        Thread.sleep(2000);
        auth.purgeResponseAuth();
        for (int i = 0;i <= 500;i++){
            ServerAuth serverAuth = auth.getResponseAuth(""+i);
            if (serverAuth != null){
                fail("Error purga");
                break;
            }
        }
        // Probar con tipo auth = Digest
        for (int i = 0;i <= 500;i++){
            auth.createResponseAuth(DigestAuth.DIGEST,"digest "+i,"");                    
        }
        Thread.sleep(2000);
        auth.purgeResponseAuth();
        for (int i = 0;i <= 500;i++){
            ServerAuth serverAuth = auth.getResponseAuth("digest "+i);
            if (serverAuth != null){
                fail("Error purga");
                break;
            }
        }
    }
    
    @Test
    public void testTypeInvalid() throws Exception{
        System.out.println("TypeAuthInvalid");        
        DigestAuth auth = new DigestAuth();
        List<String> typeValids = new ArrayList();
        typeValids.add("Basic");
        auth.setTypeAuthValids(typeValids);
        auth.createResponseAuth(DigestAuth.BASIC,"basic ","");
        try {
            auth.createResponseAuth(DigestAuth.DIGEST,"","");
            fail("Error validación");
        } catch (TypeAuthInvalid e) {
            System.out.println("Prueba exitosa "+e.getMessage());
        }
        
        try {
            auth = new DigestAuth("cualquier cosa", "", "");
            fail("Error validación");
        } catch (TypeAuthInvalid e) {
            System.out.println("Prueba exitosa "+e.getMessage());
        }
    }

    @Test
    public void testQopInvalid() throws Exception{
        System.out.println("QopInvalid");        
        DigestAuth auth = new DigestAuth();
        List<String> qopValids = new ArrayList();
        qopValids.add("");
        qopValids.add("auth-int");
        auth.setQopValids(qopValids);
        
        String header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"f09cecf114d72dcb1ba99e02ac448687\", "
                + "uri=\"/digest-auth\", qop=auth, nc=00000000, cnonce=\"f09cecf114d72dcb1ba99e02ac448688\", "
                + "response=\"74a2827a07111f627cf619e66860739f\", opaque=\"\"";

        auth.createResponseAuth(DigestAuth.DIGEST,"f09cecf114d72dcb1ba99e02ac448687","ldap");
        auth.getResponseAuth("f09cecf114d72dcb1ba99e02ac448687").setUsername("admin");
        auth.getResponseAuth("f09cecf114d72dcb1ba99e02ac448687").setPassword("password");
        
        ClientAuth requestAuth = new ClientAuth("GET",header,"");        
        boolean expResult = false;
        boolean result = auth.checkMD5(requestAuth);
        assertEquals(expResult, result);

        result = auth.checkMD5_Sess(requestAuth);
        assertEquals(expResult, result);

        //MD5 + qop vacio
        header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"\", uri=\"/digest-auth\", response=\"885d2f93dceb6077d8167e00d8111902\", opaque=\"\"";
        auth = new DigestAuth(DigestAuth.DIGEST, header, null);                
        auth.createResponseAuth(DigestAuth.DIGEST,"","ldap");        
        auth.getResponseAuth("").setUsername("admin");
        auth.getResponseAuth("").setPassword("password");
        requestAuth = new ClientAuth("GET",header,"");        
        expResult = true;
        result = auth.checkMD5(requestAuth);
        assertEquals(expResult, result);

        //MD5 y MD5-sess + qop auth valido
        header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"f09cecf114d72dcb1ba99e02ac448687\", "
                + "uri=\"/digest-auth\", qop=auth, nc=00000000, cnonce=\"f09cecf114d72dcb1ba99e02ac448688\", "
                + "response=\"74a2827a07111f627cf619e66860739f\", opaque=\"\"";

        auth = new DigestAuth(DigestAuth.DIGEST,header,"auth-int, ");

        auth.createResponseAuth(DigestAuth.DIGEST,"f09cecf114d72dcb1ba99e02ac448687","ldap");
        auth.getResponseAuth("f09cecf114d72dcb1ba99e02ac448687").setUsername("admin");
        auth.getResponseAuth("f09cecf114d72dcb1ba99e02ac448687").setPassword("password");
        
        requestAuth = new ClientAuth("GET",header,"");        
        expResult = false;
        result = auth.checkMD5(requestAuth);
        assertEquals(expResult, result);

        result = auth.checkMD5_Sess(requestAuth);
        assertEquals(expResult, result);
        
        //MD5 + qop auth valido
        auth = new DigestAuth(DigestAuth.DIGEST,header,"auth , auth-int");

        auth.createResponseAuth(DigestAuth.DIGEST,"f09cecf114d72dcb1ba99e02ac448687","ldap");
        auth.getResponseAuth("f09cecf114d72dcb1ba99e02ac448687").setUsername("admin");
        auth.getResponseAuth("f09cecf114d72dcb1ba99e02ac448687").setPassword("password");
        
        requestAuth = new ClientAuth("GET",header,"");        
        expResult = true;
        result = auth.checkMD5(requestAuth);
        assertEquals(expResult, result);
    }
    
    @Test
    public void testAlgorithmInvalid() throws Exception{
        System.out.println("AlgorithmInvalid");        
        DigestAuth auth = new DigestAuth();
        List<String> algorithmValids = new ArrayList();
        algorithmValids.add("XXX");
        auth.setQopValids(algorithmValids);
        
        String header = "Digest username=\"admin\", realm=\"ldap\", nonce=\"f09cecf114d72dcb1ba99e02ac448687\", "
                + "uri=\"/digest-auth\", qop=auth, nc=00000000, cnonce=\"f09cecf114d72dcb1ba99e02ac448688\", "
                + "response=\"74a2827a07111f627cf619e66860739f\", opaque=\"\"";

        auth.createResponseAuth(DigestAuth.DIGEST,"f09cecf114d72dcb1ba99e02ac448687","ldap");
        auth.getResponseAuth("f09cecf114d72dcb1ba99e02ac448687").setUsername("admin");
        auth.getResponseAuth("f09cecf114d72dcb1ba99e02ac448687").setPassword("password");
        
        ClientAuth requestAuth = new ClientAuth("GET",header,"");        
        boolean expResult = false;
        boolean result = auth.checkMD5(requestAuth);
        assertEquals(expResult, result);

        result = auth.checkMD5_Sess(requestAuth);
        assertEquals(expResult, result);
    }
}

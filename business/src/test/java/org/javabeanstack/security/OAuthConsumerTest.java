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



import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import org.javabeanstack.data.TestClass;
import org.javabeanstack.model.IAppAuthConsumer;
import org.javabeanstack.model.IAppAuthConsumerToken;
import org.javabeanstack.data.services.IDataService;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 *
 * @author Jorge Enciso
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OAuthConsumerTest extends TestClass {
    private static IDataService dao;
    private static String consumerKey;
    private static String token;
    private static String tokenSecret;
    
    public OAuthConsumerTest() {
    }

    @BeforeClass
    public static void setUpClass2() {
        try {
            dao = (IDataService) context.lookup(jndiProject + "DataService!org.javabeanstack.data.services.IDataServiceRemote");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }    

    @Test
    public void test00InstanceClass() throws InstantiationException, IllegalAccessException{
        OAuthConsumer instance = new OAuthConsumerImpl();
        IAppAuthConsumer consumer = instance.getAuthConsumerClass().newInstance();
        assertNotNull(consumer);
    }


    /**
     * Test of createAuthConsumer method, of class OAuthConsumer.
     */
    @Test
    public void test01CreateAuthConsumer() {
        System.out.println("1-oAuthConsumer createAuthConsumer");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        String consumerName = "OYM";
        Date expiredDate = new Date();
        OAuthConsumer instance = new OAuthConsumerImpl();
        instance.setDao(dao);
        boolean expResult = true;
        boolean result = instance.createAuthConsumer(consumerName, expiredDate);
        assertEquals(expResult, result);
        consumerKey = instance.getLastAuthConsumer().getConsumerKey();
        // Ya existe debe dar error
        result = instance.createAuthConsumer(consumerName, expiredDate);
        assertFalse(result);        
    }

    /**
     * Test of createToken method, of class OAuthConsumer.
     */
    @Test
    public void test02CreateToken_String_String() {
        System.out.println("2-oAuthConsumer createToken");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        String data = "dato1=prueba\ndato2=prueba2\n";
        OAuthConsumer instance = new OAuthConsumerImpl();
        instance.setDao(dao);
        token = instance.createToken(consumerKey, data);
        assertFalse(token.isEmpty());
        tokenSecret = instance.getLastAuthConsumerToken().getTokenSecret();
    }

    /**
     * Test of createToken method, of class OAuthConsumer.
     */
    @Test
    public void test03CreateToken_String_Map() {
        System.out.println("3-oAuthConsumer createToken");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        Map<String, String> data = new HashMap();
        data.put("dato1", "prueba");
        data.put("dato2", "prueba2");
        
        OAuthConsumer instance = new OAuthConsumerImpl();
        instance.setDao(dao);        
        String result = instance.createToken(consumerKey, data);
        assertFalse(result.isEmpty());
    }

    /**
     * Test of getToken method, of class OAuthConsumer.
     */
    @Test
    public void test05GetToken() {
        System.out.println("4-oAuthConsumer getToken");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        OAuthConsumer instance = new OAuthConsumerImpl();
        instance.setDao(dao);
        String result = instance.getToken(consumerKey, tokenSecret);
        assertFalse(result.isEmpty());        
    }

    /**
     * Test of requestToken method, of class OAuthConsumer.
     */
    //@Test
    public void test06RequestToken() {
        System.out.println("requestToken");
        String consumerKey = "";
        OAuthConsumer instance = new OAuthConsumerImpl();
        boolean expResult = false;
        boolean result = instance.requestToken(consumerKey);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }


    /**
     * Test of createConsumerKey method, of class OAuthConsumer.
     */
    //@Test
    public void test09CreateConsumerKey() throws Exception {
        System.out.println("createConsumerKey");
        IAppAuthConsumer authConsumer = null;
        OAuthConsumer instance = new OAuthConsumerImpl();
        String expResult = "";
        String result = instance.createConsumerKey(authConsumer);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }


    /**
     * Test of getSecretKey method, of class OAuthConsumer.
     */
    //@Test
    public void test10GetSecretKey() {
        System.out.println("getSecretKey");
        String encodeKey = "";
        String algorithm = "";
        OAuthConsumer instance = new OAuthConsumerImpl();
        SecretKey expResult = null;
        SecretKey result = instance.getSecretKey(encodeKey, algorithm);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of signTokenData method, of class OAuthConsumer.
     */
    //@Test
    public void test11SignTokenData() throws Exception {
        System.out.println("signTokenData");
        IAppAuthConsumerToken model = null;
        OAuthConsumer instance = new OAuthConsumerImpl();
        String expResult = "";
        String result = instance.signTokenData(model);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getTokenSecret method, of class OAuthConsumer.
     */
    //@Test
    public void test12GetTokenSecret() throws Exception {
        System.out.println("getTokenSecret");
        IAppAuthConsumerToken model = null;
        OAuthConsumer instance = new OAuthConsumerImpl();
        String expResult = "";
        String result = instance.getTokenSecret(model);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getRandomToken method, of class OAuthConsumer.
     */
    //@Test
    public void test13GetRandomToken() throws Exception {
        System.out.println("getRandomToken");
        OAuthConsumer instance = new OAuthConsumerImpl();
        String expResult = "";
        String result = instance.getRandomToken();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }


    /**
     * Test of findAuthConsumer method, of class OAuthConsumer.
     */
    //@Test
    public void test02FindAuthConsumer() {
        System.out.println("2-oAuthConsumer findAuthConsumer");
        String consumerKey = "";
        OAuthConsumer instance = new OAuthConsumerImpl();
        IAppAuthConsumer expResult = null;
        IAppAuthConsumer result = instance.findAuthConsumer(consumerKey);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findAuthToken method, of class OAuthConsumer.
     */
    //@Test
    public void test03FindAuthToken_String() {
        System.out.println("3-oAuthConsumer findAuthToken");
        String token = "";
        OAuthConsumer instance = new OAuthConsumerImpl();
        IAppAuthConsumerToken expResult = null;
        IAppAuthConsumerToken result = instance.findAuthToken(token);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findAuthToken method, of class OAuthConsumer.
     */
    //@Test
    public void test04FindAuthToken_String_String() {
        System.out.println("findAuthToken");
        String consumerKey = "";
        String tokenSecret = "";
        OAuthConsumer instance = new OAuthConsumerImpl();
        IAppAuthConsumerToken expResult = null;
        IAppAuthConsumerToken result = instance.findAuthToken(consumerKey, tokenSecret);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    
    /**
     * Test of dropToken method, of class OAuthConsumer.
     */
    //@Test
    public void test14DropToken() {
        System.out.println("dropToken");
        String consumerKey = "";
        String tokenSecret = "";
        OAuthConsumer instance = new OAuthConsumerImpl();
        boolean expResult = false;
        boolean result = instance.dropToken(consumerKey, tokenSecret);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
    /**
     * Test of dropAuthConsumer method, of class OAuthConsumer.
     */
    //@Test
    public void test15DropAuthConsumer() {
        System.out.println("dropAuthConsumer");
        String consumerKey = "";
        OAuthConsumer instance = new OAuthConsumerImpl();
        boolean expResult = false;
        boolean result = instance.dropAuthConsumer(consumerKey);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
    public class OAuthConsumerImpl extends OAuthConsumer {
        @Override
        public Class<IAppAuthConsumer> getAuthConsumerClass() {
            try {
                return (Class<IAppAuthConsumer>)Class.forName("org.javabeanstack.model.appcatalog.AppAuthConsumer");
            } catch (ClassNotFoundException ex) {
                System.out.println(ex.getMessage());                
            }
            return null;
        }

        @Override
        public Class<IAppAuthConsumerToken> getAuthConsumerTokenClass() {
            try {            
                return (Class<IAppAuthConsumerToken>)Class.forName("org.javabeanstack.model.appcatalog.AppAuthConsumerToken");
            } catch (ClassNotFoundException ex) {
                System.out.println(ex.getMessage());
            }
            return null;
        }
    }
}

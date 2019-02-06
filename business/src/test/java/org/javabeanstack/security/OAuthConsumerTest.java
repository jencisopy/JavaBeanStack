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

import org.javabeanstack.security.model.OAuthConsumerData;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.crypto.SecretKey;
import org.javabeanstack.config.IAppConfig;
import org.javabeanstack.crypto.CipherUtil;
import org.javabeanstack.data.DBLinkInfo;
import org.javabeanstack.data.IDBLinkInfo;
import org.javabeanstack.data.TestClass;
import org.javabeanstack.data.services.IAppCompanySrv;
import org.javabeanstack.model.IAppAuthConsumer;
import org.javabeanstack.model.IAppAuthConsumerToken;
import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.data.services.IDataServiceRemote;
import org.javabeanstack.model.appcatalog.AppAuthConsumer;
import org.javabeanstack.model.appcatalog.AppAuthConsumerToken;
import org.javabeanstack.model.tables.Moneda;
import org.javabeanstack.util.Dates;
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
    private static String uuidDevice = "xxxx1111133333";
    private static IAppAuthConsumerToken appConsumerToken;
    
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
        OAuthConsumerBase instance = new OAuthConsumerImpl();
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
        expiredDate = Dates.sumDays(expiredDate, 5000);
        OAuthConsumerBase instance = new OAuthConsumerImpl();
        instance.setDao(dao);
        String result = instance.createAuthConsumer(consumerName, expiredDate);
        assertNotNull(result);
        consumerKey = instance.getLastAuthConsumer().getConsumerKey();
        // Ya existe debe dar error
        result = instance.createAuthConsumer(consumerName, expiredDate);
        assertNull(result);
        //
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
        IOAuthConsumerData data = new OAuthConsumerData();
        data.setIdAppUser(0L);
        data.setUserLogin("J");
        data.setUserPass("");
        data.setIdCompany(2L);
        data.addOtherDataValue("dato1", "dato1");
        data.addOtherDataValue("dato2", "dato2");
        OAuthConsumerBase instance = new OAuthConsumerImpl();
        instance.setDao(dao);
        token = instance.createToken(consumerKey, data, uuidDevice);
        assertFalse(token.isEmpty());
        tokenSecret = instance.getLastAuthConsumerToken().getTokenSecret();
        //
        appConsumerToken = instance.getLastAuthConsumerToken();
        //
        data.addOtherDataValue("dato3", "dato3");
        String token2 = instance.createToken(consumerKey, data);
        assertFalse(token2.isEmpty());        
        //
        
    }

    /**
     * Test of getToken method, of class OAuthConsumer.
     */
    @Test
    public void test04GetToken() {
        System.out.println("4-oAuthConsumer getToken");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        OAuthConsumerBase instance = new OAuthConsumerImpl();
        instance.setDao(dao);
        String result = instance.getToken(consumerKey, tokenSecret);
        assertFalse(result.isEmpty());        
        result = instance.getToken(consumerKey, uuidDevice);
        assertFalse(result.isEmpty());        
    }

    /**
     * Test of requestToken method, of class OAuthConsumer.
     */
    @Test
    public void test05RequestToken() {
        System.out.println("5-oAuthConsumer requestToken");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        OAuthConsumerBase instance = new OAuthConsumerImpl();
        instance.setDao(dao);        
        boolean expResult = true;
        boolean result = instance.requestToken(consumerKey);
        assertEquals(expResult, result);
    }


    /**
     * Test of getSecretKey method, of class OAuthConsumer.
     */
    @Test
    public void test06GetSecretKey() {
        System.out.println("6-oAuthConsumer getSecretKey");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        OAuthConsumerBase instance = new OAuthConsumerImpl();
        instance.setDao(dao);
        IAppAuthConsumer authConsumer = instance.findAuthConsumer(consumerKey);
        String encodeKey = authConsumer.getPrivateKey();
        String algorithm = CipherUtil.BLOWFISH;
        SecretKey result = instance.getSecretKey(encodeKey, algorithm);
        assertNotNull(result);
    }


    /**
     * Test of getTokenSecret method, of class OAuthConsumer.
     */
    @Test
    public void test07GetTokenSecret() throws Exception {
        System.out.println("7-oAuthConsumer getTokenSecret");
        assertTrue(tokenSecret != null && !tokenSecret.isEmpty());
    }

    /**
     * Test of getRandomToken method, of class OAuthConsumer.
     */
    @Test
    public void test08GetRandomToken() throws Exception {
        System.out.println("8-oAuthConsumer getRandomToken");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        OAuthConsumerBase instance = new OAuthConsumerImpl();
        instance.setDao(dao);        
        String result = instance.getRandomToken();
        assertFalse(result.isEmpty());
    }


    /**
     * Test of findAuthConsumer method, of class OAuthConsumer.
     */
    @Test
    public void test09FindAuthConsumer() {
        System.out.println("9-oAuthConsumer findAuthConsumer");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        OAuthConsumerBase instance = new OAuthConsumerImpl();
        instance.setDao(dao);
        IAppAuthConsumer result = instance.findAuthConsumer(consumerKey);
        assertNotNull(result);
    }

    /**
     * Test of findAuthToken method, of class OAuthConsumer.
     */
    @Test
    public void test10FindAuthToken_String() {
        System.out.println("10-oAuthConsumer findAuthToken");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        OAuthConsumerBase instance = new OAuthConsumerImpl();
        instance.setDao(dao);
        IAppAuthConsumerToken result = instance.findAuthToken(token);
        assertNotNull(result);
    }

    /**
     * Test of findAuthToken method, of class OAuthConsumer.
     */
    @Test
    public void test11FindAuthToken_String_String() {
        System.out.println("11-oAuthConsumer findAuthToken");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        OAuthConsumerBase instance = new OAuthConsumerImpl();
        instance.setDao(dao);        
        IAppAuthConsumerToken result = instance.findAuthToken(consumerKey, tokenSecret);
        assertNotNull(result);
    }


    /**
     * Test of load property from data, load map from property, load map from data
     */
    @Test
    public void test12LoadData() throws IOException {
        System.out.println("12-oAuthConsumer load data");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        OAuthConsumerBase instance = new OAuthConsumerImpl();
        instance.setDao(dao);        
        IAppAuthConsumerToken result = instance.findAuthToken(consumerKey, tokenSecret);
        assertNotNull(result);
        Properties prop = new Properties();
        prop.load(new StringReader(result.getData()));
        assertNotNull(prop.getProperty("dato1"));
        assertNull(prop.getProperty("noexiste"));
        Map<String, String> data = new HashMap(prop);
        assertNotNull(data.get("dato1"));
    }


    @Test
    public void test13IsValidToken(){
        System.out.println("13-oAuthConsumer isValidToken");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        OAuthConsumerBase instance = new OAuthConsumerImpl();
        instance.setDao(dao);        
        assertTrue(instance.isValidToken(token));
        assertFalse(instance.isValidToken("noexiste"));
    }
    
    @Test
    public void test14GetDataKeyValue(){
        System.out.println("14-oAuthConsumer getDataKeyValue");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        OAuthConsumerBase instance = new OAuthConsumerImpl();
        instance.setDao(dao);        
        assertFalse(instance.getDataKeyValue(token,"idappuser").isEmpty());
        assertTrue(instance.getDataKeyValue(token,"noexiste").isEmpty());
    }

    @Test
    public void test15GetUserMapped(){
        System.out.println("15-oAuthConsumer getUserMapped");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        OAuthConsumerBase instance = new OAuthConsumerImpl();
        instance.setDao(dao);        
        assertNotNull(instance.getUserMapped(token));
        assertNull(instance.getUserMapped("noexiste"));
    }

    @Test
    public void test16GetDBLinkInfo() throws Exception{
        System.out.println("16-oAuthConsumer getDBLinkInfo");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        OAuthConsumerBase instance = new OAuthConsumerImpl();
        instance.setDao(dao);        
        IDBLinkInfo info = new DBLinkInfo();
        IAppAuthConsumerToken tokenRecord = instance.findAuthToken(token);
        info.setToken(tokenRecord,instance);
        assertNotNull(info.getSessionOrTokenId());
        assertNotNull(info.getAppUserId());
        assertNotNull(info.getIdCompany());
    }
    
    
    @Test
    public void test17GetDataRows() throws Exception{
        System.out.println("17-oAuthConsumer getDataRows");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataServiceRemote dataService = 
                (IDataServiceRemote) context.lookup(jndiProject+"DataService!org.javabeanstack.data.services.IDataServiceRemote");
        
        List<Moneda> monedas = dataService.getDataRows(token, Moneda.class,"" , "", null,0,1000);
        assertFalse(monedas.isEmpty());
    }    
    /**
     * Test of dropToken method, of class OAuthConsumer.
     */
    @Test
    public void test18DropToken() {
        System.out.println("18-oAuthConsumer dropToken");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        OAuthConsumerBase instance = new OAuthConsumerImpl();
        instance.setDao(dao);                
        boolean result = instance.dropToken(consumerKey, tokenSecret);
        assertTrue(result);
    }
    
    /**
     * Test of dropAuthConsumer method, of class OAuthConsumer.
     */
    @Test
    public void test19DropAuthConsumer() {
        System.out.println("19-oAuthConsumer dropAuthConsumer");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        OAuthConsumerBase instance = new OAuthConsumerImpl();
        instance.setDao(dao);
        boolean result = instance.dropAuthConsumer(consumerKey);
        assertTrue(result);
    }
    
    
    @Test
    public void test20CreateToken() {
        System.out.println("20-oAuthConsumer createToken");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        OAuthConsumerBase instance = new OAuthConsumerImpl();
        instance.setDao(dao);
        ((AppAuthConsumer)appConsumerToken.getAppAuthConsumerKey()).setIdappauthconsumer(0L);
        ((AppAuthConsumer)appConsumerToken.getAppAuthConsumerKey()).setAppauthconsumertokenList(null);
        ((AppAuthConsumerToken)appConsumerToken).setIdappauthconsumertoken(0L);
        assertTrue(instance.createToken(appConsumerToken) != null);
        test19DropAuthConsumer();
    }
   
    public class OAuthConsumerImpl extends OAuthConsumerBase {
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
        
        @Override
        protected IAppCompanySrv getAppCompanySrv() {
            return null;
        }
    }
}

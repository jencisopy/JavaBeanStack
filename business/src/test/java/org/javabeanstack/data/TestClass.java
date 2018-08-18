/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javabeanstack.data;

import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.javabeanstack.security.ISecManager;
import org.javabeanstack.security.IUserSession;

/**
 *
 * @author Jorge Enciso
 */
public class TestClass {

    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(TestClass.class);
    static protected Context context;
    static protected IDataLink dataLink;
    static protected IDataLink dataLinkCat;
    static protected String sessionId;
    static protected String error;
    static String jndiProject = "/TestProjects-ear/TestProjects-ejb/";

    public TestClass() {
    }

    @BeforeClass
    public static void setUpClass() throws NamingException, Exception {
        try {
            String server = (System.getenv("SERVER_TEST") != null) ? System.getenv("SERVER_TEST") : "localhost";
            String port = (System.getenv("SERVER_TEST_PORT") != null) ? System.getenv("SERVER_TEST_PORT") : "8080";
            String user = (System.getenv("SECURITY_PRINCIPAL") != null) ? System.getenv("SECURITY_PRINCIPAL") : "";
            String password = (System.getenv("SECURITY_CREDENTIALS") != null) ? System.getenv("SECURITY_CREDENTIALS") : "";
            
            System.out.println(server);
            System.out.println(port);
            System.out.println(user);
            System.out.println(password);
            
            Properties p = new Properties();
            p.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
            p.put(Context.PROVIDER_URL, "http-remoting://"+server+":"+port);
            if (!user.isEmpty()){
                p.put(Context.SECURITY_PRINCIPAL, user);
                p.put(Context.SECURITY_CREDENTIALS, password);
            }
            p.put("jboss.naming.client.ejb.context", true);
            context = new InitialContext(p);

            ISecManager secMngr = (ISecManager) context.lookup(jndiProject + "SecManager!org.javabeanstack.security.ISecManagerRemote");
            //TODO cambiar a empresas tests
            //IUserSession userSession = secMngr.createSession("test1", "test1", 2L, null);        
            IUserSession userSession = secMngr.createSession("J", "", 2L, null);
            sessionId = userSession.getSessionId();

            IGenericDAO dao = (IGenericDAO) context.lookup(jndiProject + "GenericDAO!org.javabeanstack.data.IGenericDAORemote");
            dataLinkCat = new DataLink(dao);

            dataLink = new DataLink(dao);
            dataLink.setUserSession(userSession);
            //dataLink.getUserSession().getDBFilter().setModelPackagePath("net.makerapp.model.tables;net.makerapp.model.views");
        } catch (Exception e) {
            LOGGER.error(e);
            error = e.getMessage();
        }
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

}

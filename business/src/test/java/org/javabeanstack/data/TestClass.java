package org.javabeanstack.data;

import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.javabeanstack.security.ISecManager;
import org.javabeanstack.security.model.IUserSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 *
 * @author Jorge Enciso
 */
public class TestClass {
    private static final Logger LOGGER = LogManager.getLogger(TestClass.class);
    static protected Context context;
    static protected IDataLink dataLink;
    static protected IDataLink dataLinkCat;
    static protected String sessionId;
    static protected String error;
    static protected String jndiProject = "/TestProjects-ear/TestProjects-ejb/";
    //static protected String jndiProject = "/Maker-ear/Maker-ejb/";
    //static protected String jndiProject = "/Maker-web/";

    public TestClass() {
    }

    @BeforeAll
    public static void setUpClass() throws NamingException, Exception {
        try {
            String server = (System.getenv("SERVER_TEST") != null) ? System.getenv("SERVER_TEST") : "localhost";
            String port = (System.getenv("SERVER_TEST_PORT") != null) ? System.getenv("SERVER_TEST_PORT") : "8080";
            String user = (System.getenv("SECURITY_PRINCIPAL") != null) ? System.getenv("SECURITY_PRINCIPAL") : "";
            String password = (System.getenv("SECURITY_CREDENTIALS") != null) ? System.getenv("SECURITY_CREDENTIALS") : "";
            
            String appuser_login = (System.getenv("APP_USER_LOGIN") != null) ? System.getenv("APP_USER_LOGIN") : "test1";
            String appuser_pass = (System.getenv("APP_USER_PASS") != null) ? System.getenv("APP_USER_PASS") : "test1";
            Long app_idcompany = Long.valueOf((System.getenv("APP_IDCOMPANY") != null) ? System.getenv("APP_IDCOMPANY") : "2");
            
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
            IUserSession userSession = secMngr.createSession(appuser_login, appuser_pass, app_idcompany, null, null);        
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

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
    }
}

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
package org.javabeanstack.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.javabeanstack.model.tables.AppResource;
import org.javabeanstack.model.tables.AppUser;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 *
 * @author JORGE
 */
public class AbstractDAOTest extends TestClass {

    private static IGenericDAORemote dao;

    public AbstractDAOTest() {
    }

    @BeforeClass
    public static void setUpClass2() {
        try {
            dao = (IGenericDAORemote) context.lookup(jndiProject + "GenericDAO!org.javabeanstack.data.IGenericDAORemote");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        
    }

    /**
     * Test of findAll method, of class AbstractDAO.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testFindAll() throws Exception {
        System.out.println("findAll");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<AppUser> users = dao.findAll(AppUser.class, null);
        assertTrue(users.size() > 0);
    }

    /**
     * Test of findById method, of class AbstractDAO.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testFindById() throws Exception {
        System.out.println("findById");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        AppUser user = dao.findById(AppUser.class, null, 1L);
        assertNotNull(user);
        
        user = dao.findById(AppUser.class, null, 0L);
        assertNull(user);
    }

    /**
     * Test of findByUk method, of class AbstractDAO.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testFindByUk() throws Exception {
        System.out.println("findByUk");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        AppResource appResource = new AppResource();
        appResource.setCode("no se encuentra");
        appResource = dao.findByUk(null, appResource);
        assertNull(appResource);
        
        appResource = new AppResource();
        appResource.setCode("clasemaker.xml");
        appResource = dao.findByUk(null, appResource);
        assertNotNull(appResource);
    }

    /**
     * Test of find method, of class AbstractDAO.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testFind() throws Exception {
        System.out.println("find");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<AppUser> users = dao.find(AppUser.class, null);
        assertTrue(users.size() > 0);
        
        String order = "code desc";
        String filter = "";
        users = dao.find(AppUser.class, null, order, filter, null);
        assertTrue(users.size() > 0);
        
        filter = "code = 'Administrador'";
        users = dao.find(AppUser.class, null, order, filter, null);
        assertTrue(users.size() == 1);
        
        filter = "code = :code";
        Map<String, Object> params = new HashMap();
        params.put("code", "Administrador");
        users = dao.find(AppUser.class, null, order, filter, params);
        assertTrue(users.size() == 1);
        
        users = dao.find(AppUser.class, null, null, "", null, 0, 4);        
        assertTrue(users.size() == 4);
    }

    /**
     * Test of findByQuery method, of class AbstractDAO.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testFindByQuery() throws Exception {
        System.out.println("findByQuery");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        
        AppUser user = dao.findByQuery(null, "select o from AppUser o where iduser = 1L", null);
        assertNotNull(user);

        user = dao.findByQuery(null, "select o from AppUser o where iduser = 0L", null);
        assertNull(user);
        
        Map<String, Object> params = new HashMap();
        params.put("iduser", 1L);
        user = dao.findByQuery(null, "select o from AppUser o where iduser = :iduser", params);
        assertNotNull(user);
    }

    /**
     * Test of findListByQuery method, of class AbstractDAO.
     */
    @Test
    public void testFindListByQuery() throws Exception {
        System.out.println("findListByQuery");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<AppUser> users = dao.findListByQuery(null, "select o from AppUser o where iduser = 1L", null);
        assertNotNull(users);

        users = dao.findListByQuery(null, "select o from AppUser o where iduser = 0L", null);
        assertTrue(users.isEmpty());
        
        Map<String, Object> params = new HashMap();
        params.put("iduser", 1L);
        users = dao.findListByQuery(null, "select o from AppUser o where iduser = :iduser", params);
        assertNotNull(users);
    }

    /**
     * Test of findByNamedQuery method, of class AbstractDAO.
     */
    @Test
    public void testFindByNamedQuery() throws Exception {
        System.out.println("findByNamedQuery");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
    }

    /**
     * Test of findByNativeQuery method, of class AbstractDAO.
     */
    @Test
    public void testFindByNativeQuery() throws Exception {
        System.out.println("findByNativeQuery");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
    }


    /**
     * Test of update method, of class AbstractDAO.
     */
    @Test
    public void testUpdate() {
        System.out.println("update");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
    }

    /**
     * Test of persist method, of class AbstractDAO.
     */
    @Test
    public void testPersist() {
        System.out.println("persist");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
    }

    /**
     * Test of merge method, of class AbstractDAO.
     */
    @Test
    public void testMerge() {
        System.out.println("merge");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
    }

    /**
     * Test of remove method, of class AbstractDAO.
     */
    @Test
    public void testRemove() {
        System.out.println("remove");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
    }

    /**
     * Test of refreshRow method, of class AbstractDAO.
     */
    @Test
    public void testRefreshRow() throws Exception {
        System.out.println("refreshRow");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        AppResource resource = dao.findByQuery(null, "select o from AppResource o where code = 'clasemaker.xml'", null);
        resource = dao.refreshRow(null, resource);
        assertNotNull(resource);
    }


    /**
     * Test of getCount method, of class AbstractDAO.
     */
    @Test
    public void testGetCount() throws Exception {
        System.out.println("getCount");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        Long rec = dao.getCount(null, "select o FROM AppCompany o", null);
        assertTrue(rec > 0L);
    }

    /**
     * Test of getCount2 method, of class AbstractDAO.
     * 
     */
    @Test
    public void testGetCount2() throws Exception {
        System.out.println("getCount2");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        Long rec = dao.getCount2(null, "select * FROM catalogo.empresa", null);
        assertTrue(rec > 0L);
    }


    /**
     * Test of getEntityManagerProp method, of class AbstractDAO.
     */
    @Test
    public void testGetEntityManagerProp() {
        System.out.println("getEntityManagerProp");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        Map<String, Object> props = dao.getEntityManagerProp("PU1");
        assertNotNull(props);
    }

    /**
     * Test of getPersistUnitProp method, of class AbstractDAO.
     */
    @Test
    public void testGetPersistUnitProp() {
        System.out.println("getPersistUnitProp");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        Map<String, Object> props = dao.getPersistUnitProp("PU1");
        assertNotNull(props);        
    }

    /**
     * Test of getDataEngine method, of class AbstractDAO.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testGetDataEngine() throws Exception {
        System.out.println("getDataEngine");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        System.out.println(dao.getDataEngine("PU1"));

        String expResult = dao.getDataEngine("PU1");
        assertNotNull(expResult);
    }

    /**
     * Test of getSchema method, of class AbstractDAO.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testGetSchema() throws Exception {
        System.out.println("getSchema");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }

        System.out.println(dao.getSchema("PU1"));

        String expResult = dao.getSchema("PU1");
        assertNotNull(expResult);
    }


    /**
     * Test of getUserSession method, of class AbstractDAO.
     */
    @Test
    public void testGetUserSession() {
        System.out.println("getUserSession");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        assertNotNull(dao.getUserSession(sessionId));
    }

    /**
     * Test of getDBLinkInfo method, of class AbstractDAO.
     */
    @Test
    public void testGetDBLinkInfo() {
        System.out.println("getDBLinkInfo");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDBLinkInfo info = dao.getUserSession(sessionId).getDbLinkInfo();
        assertNotNull(info);
    }

}

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

import java.util.List;
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
        List<AppUser> users = dao.findAll(AppUser.class, sessionId);
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
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
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
    }

    /**
     * Test of findListByQuery method, of class AbstractDAO.
     */
    @Test
    public void testFindListByQuery_4args() throws Exception {
        System.out.println("findListByQuery");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
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
     * Test of sqlExec method, of class AbstractDAO.
     */
    @Test
    public void testSqlExec() throws Exception {
        System.out.println("sqlExec");
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
    }

    /**
     * Test of refreshAll method, of class AbstractDAO.
     */
    @Test
    public void testRefreshAll() throws Exception {
        System.out.println("refreshAll");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
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
    }

    /**
     * Test of getCount2 method, of class AbstractDAO.
     */
    @Test
    public void testGetCount2() throws Exception {
        System.out.println("getCount2");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
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

        IGenericDAORemote dao
                = (IGenericDAORemote) context.lookup(jndiProject + "GenericDAO!org.javabeanstack.data.IGenericDAORemote");
        System.out.println(dao.getSchema("PU1"));

        String expResult = dao.getSchema("PU1");
        assertNotNull(expResult);
    }

    /**
     * Test of getConnection method, of class AbstractDAO.
     */
    @Test
    public void testGetConnection_String() {
        System.out.println("getConnection");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
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
    }

    public class AbstractDAOImpl extends AbstractDAO {
    }

}

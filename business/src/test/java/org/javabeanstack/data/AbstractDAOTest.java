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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.javabeanstack.model.tables.AppResource;
import org.javabeanstack.model.tables.AppTablesRelation;
import org.javabeanstack.model.tables.AppUser;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 *
 * @author Jorge Enciso
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
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<AppUser> users = dao.findAll(AppUser.class, sessionid);
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
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        AppUser user = dao.findById(AppUser.class, sessionid, 1L);
        assertNotNull(user);
        
        user = dao.findById(AppUser.class, sessionid, 0L);
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
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        AppResource appResource = new AppResource();
        appResource.setCode("no se encuentra");
        appResource = dao.findByUk(sessionid, appResource);
        assertNull(appResource);
        
        appResource = new AppResource();
        appResource.setCode("clasemaker.xml");
        appResource = dao.findByUk(sessionid, appResource);
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
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<AppUser> users = dao.find(AppUser.class, sessionid);
        assertTrue(users.size() > 0);
        
        String order = "code desc";
        String filter = "";
        users = dao.find(AppUser.class, sessionid, order, filter, null);
        assertTrue(users.size() > 0);
        
        filter = "code = 'Administrador'";
        users = dao.find(AppUser.class, sessionid, order, filter, null);
        assertTrue(users.size() == 1);
        
        filter = "code = :code";
        Map<String, Object> params = new HashMap();
        params.put("code", "Administrador");
        users = dao.find(AppUser.class, sessionid, order, filter, params);
        assertTrue(users.size() == 1);
        
        users = dao.find(AppUser.class, sessionid, null, "", null, 0, 4);        
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
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        AppUser user = dao.findByQuery(sessionid, "select o from AppUser o where iduser = 1L", null);
        assertNotNull(user);

        user = dao.findByQuery(sessionid, "select o from AppUser o where iduser = 0L", null);
        assertNull(user);
        
        Map<String, Object> params = new HashMap();
        params.put("iduser", 1L);
        user = dao.findByQuery(sessionid, "select o from AppUser o where iduser = :iduser", params);
        assertNotNull(user);
    }

    /**
     * Test of findListByQuery method, of class AbstractDAO.
     */
    @Test
    public void testFindListByQuery() throws Exception {
        System.out.println("findListByQuery");
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<AppUser> users = dao.findListByQuery(sessionid, "select o from AppUser o where iduser = 1L", null);
        assertNotNull(users);

        users = dao.findListByQuery(sessionid, "select o from AppUser o where iduser = 0L", null);
        assertTrue(users.isEmpty());
        
        Map<String, Object> params = new HashMap();
        params.put("iduser", 1L);
        users = dao.findListByQuery(sessionid, "select o from AppUser o where iduser = :iduser", params);
        assertNotNull(users);
    }

    /**
     * Test of findByNamedQuery method, of class AbstractDAO.
     */
    @Test
    public void testFindByNamedQuery() throws Exception {
        System.out.println("findByNamedQuery");
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<AppTablesRelation> prueba = dao.findListByNamedQuery(sessionid, "AppTablesRelation.findAll", null);
        assertTrue(!prueba.isEmpty());
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
        String sqlSentence = "select * from {schema}.moneda where idmoneda > :id";
        Map<String, Object> params = new HashMap();
        params.put("id",0);
        List<Object> query1 = dataLink.findByNativeQuery(sqlSentence, params);
        assertTrue(!query1.isEmpty());
        
        // Un grupo de registros first, max
        query1 = dataLink.findByNativeQuery(sqlSentence, params,0,10);
        assertTrue(!query1.isEmpty());
    }


    /**
     * Test of update method, of class AbstractDAO.
     */
    @Test
    public void testUpdate() throws Exception{
        System.out.println("update");
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        //Persist
        AppTablesRelation relation = new AppTablesRelation();
        relation.setEntityPK("xx1");
        relation.setEntityFK("xx2");
        relation.setFechacreacion(new Date());
        relation.setFechamodificacion(new Date());
        relation.setFieldsFK("id");
        relation.setFieldsPK("id");
        relation.setIncluded(false);
        relation.setAction(IDataRow.INSERT);
        dao.update(null, relation);
        
        // Merge
        List<AppTablesRelation>  relations = dao.findListByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1'", null);
        relations.get(0).setIncluded(true);
        relations.get(0).setAction(IDataRow.UPDATE);
        dao.update(null, relations);

        relation = dao.findByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1' and entityFK = 'xx2' and included = true", null);
        assertNotNull(relation);

        //Remove
        relation.setAction(IDataRow.DELETE);
        dao.update(null, relation);
        
        relation = dao.findByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1' and entityFK = 'xx2' and included = true", null);
        assertNull(relation);
    }

    /**
     * Test of persist method, of class AbstractDAO.
     */
    @Test
    public void testPersist() throws Exception {
        System.out.println("persist");
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        //Persist
        AppTablesRelation relation = new AppTablesRelation();
        relation.setEntityPK("xx1");
        relation.setEntityFK("xx2");
        relation.setFechacreacion(new Date());
        relation.setFechamodificacion(new Date());
        relation.setFieldsFK("id");
        relation.setFieldsPK("id");
        IDataResult dataResult = dao.persist(sessionid, relation);
        
        AppTablesRelation rowResult = dataResult.getRowResult();
        assertEquals(relation.getEntityPK(),rowResult.getEntityPK());
        
        List<AppTablesRelation>  relations = dao.findListByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1'", null);

        assertTrue(relations.get(0).getEntityPK().trim().equals("xx1"));

        //Remove
        dao.remove(sessionid, relations.get(0));
    }

    /**
     * Test of merge method, of class AbstractDAO.
     */
    @Test
    public void testMerge() throws Exception {
        System.out.println("merge");
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        //Persist
        AppTablesRelation relation = new AppTablesRelation();
        relation.setEntityPK("xx1");
        relation.setEntityFK("xx2");
        relation.setFechacreacion(new Date());
        relation.setFechamodificacion(new Date());
        relation.setFieldsFK("id");
        relation.setFieldsPK("id");
        relation.setIncluded(false);
        dao.persist(sessionid, relation);
        
        relation = dao.findByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1'", null);
        relation.setIncluded(true);
        dao.merge(sessionid, relation);

        relation = dao.findByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1' and entityFK = 'xx2' and included = true", null);
        assertNotNull(relation);

        //Remove
        dao.remove(sessionid, relation);
    }

    /**
     * Test of remove method, of class AbstractDAO.
     */
    @Test
    public void testRemove() throws Exception{
        System.out.println("remove");
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        //Persist
        AppTablesRelation relation = new AppTablesRelation();
        relation.setEntityPK("xx1");
        relation.setEntityFK("xx2");
        relation.setFechacreacion(new Date());
        relation.setFechamodificacion(new Date());
        relation.setFieldsFK("id");
        relation.setFieldsPK("id");
        relation.setIncluded(false);
        dao.persist(sessionid, relation);
        
        relation = dao.findByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1' and entityFK = 'xx2'", null);
        assertNotNull(relation);

        //Remove
        dao.remove(sessionid, relation);
        
        relation = dao.findByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1' and entityFK = 'xx2'", null);
        assertNull(relation);
    }

    /**
     * Test of refreshRow method, of class AbstractDAO.
     */
    @Test
    public void testRefreshRow() throws Exception {
        System.out.println("refreshRow");
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        AppResource resource = dao.findByQuery(sessionid, "select o from AppResource o where code = 'clasemaker.xml'", null);
        resource = dao.refreshRow(sessionid, resource);
        assertNotNull(resource);
    }


    /**
     * Test of getCount method, of class AbstractDAO.
     */
    @Test
    public void testGetCount() throws Exception {
        System.out.println("getCount");
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        Long rec = dao.getCount(sessionid, "select o FROM AppCompany o", null);
        assertTrue(rec > 0L);
    }

    /**
     * Test of getCount2 method, of class AbstractDAO.
     * 
     */
    @Test
    public void testGetCount2() throws Exception {
        System.out.println("getCount2");
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        // {schema} se reemplaza automáticamente por el nombre del schema
        Long rec = dao.getCount2(sessionid, "select * FROM {schema}.empresa", null);
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

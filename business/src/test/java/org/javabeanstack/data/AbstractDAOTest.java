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
import org.javabeanstack.model.appcatalog.AppResource;
import org.javabeanstack.model.appcatalog.AppTablesRelation;
import org.javabeanstack.model.appcatalog.AppUser;
import org.javabeanstack.model.tables.Moneda;
import org.javabeanstack.model.views.CtactemovimientodetalleView;
import org.javabeanstack.util.LocalDates;
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
public class AbstractDAOTest extends TestClass {
    private static IGenericDAO dao;

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
    public void test01FindAll() throws Exception {
        System.out.println("1-AbstractDAO - findAll");
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<AppUser> users = dao.findAll(AppUser.class, sessionid);
        assertTrue(!users.isEmpty());
    }

    /**
     * Test of findById method, of class AbstractDAO.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test02FindById() throws Exception {
        System.out.println("2-AbstractDAO - findById");
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
    public void test03FindByUk() throws Exception {
        System.out.println("3-AbstractDAO - findByUk");
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
    public void test04Find() throws Exception {
        System.out.println("4-AbstractDAO - find");
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<AppUser> users = dao.find(AppUser.class, sessionid);
        assertTrue(!users.isEmpty());
        
        String order = "code desc";
        String filter = "";
        users = dao.find(AppUser.class, sessionid, order, filter, null);
        assertTrue(!users.isEmpty());
        
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
    public void test05FindByQuery() throws Exception {
        System.out.println("5-AbstractDAO - findByQuery");
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
    public void test06FindListByQuery() throws Exception {
        System.out.println("6-AbstractDAO - findListByQuery");
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
    public void test07FindByNamedQuery() throws Exception {
        System.out.println("7-AbstractDAO - findByNamedQuery");
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
    public void test08FindByNativeQuery() throws Exception {
        System.out.println("8-AbstractDAO - findByNativeQuery");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = dataLink.getUserSession().getSessionId();
        String sqlSentence = "select * from {schema}.moneda where idmoneda > :id";
        Map<String, Object> params = new HashMap();
        params.put("id",0);
        List<Object> query1 = dao.findByNativeQuery(sessionid, sqlSentence, params);
        assertTrue(!query1.isEmpty());
        
        // Un grupo de registros first, max
        query1 = dao.findByNativeQuery(sessionid, sqlSentence, params,0,10);
        assertTrue(!query1.isEmpty());
        
        // Un grupo de registros first, max
        List<Moneda> query2 = dao.findByNativeQuery(Moneda.class, sessionid, sqlSentence, params,0,10);
        assertTrue(!query2.isEmpty());
        
        sqlSentence = "SELECT b.*, a.fecha "
                    + " FROM {schema}.ctactemovimientodetalle_view b "
                    + " INNER JOIN {schema}.ctactemovimiento a ON a.idctactemovimiento = b.idctactemovimiento ";
        
        // Un grupo de registros first, max
        List<CtactemovimientodetalleView> query3 = dao.findByNativeQuery(CtactemovimientodetalleView.class, sessionid, sqlSentence, params,0,10);
        assertTrue(!query3.isEmpty());
    }


    /**
     * Test of update method, of class AbstractDAO.
     */
    @Test
    public void test09Update() throws Exception{
        System.out.println("9-AbstractDAO - update");
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
        relation.setFechacreacion(LocalDates.now());
        relation.setFechamodificacion(LocalDates.now());
        relation.setFieldsFK("id");
        relation.setFieldsPK("id");
        relation.setIncluded(false);
        relation.setAction(IDataRow.INSERT);
        dao.update(sessionid, relation);
        
        // Merge
        List<AppTablesRelation>  relations = dao.findListByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1'", null);
        relations.get(0).setIncluded(true);
        relations.get(0).setAction(IDataRow.UPDATE);
        dao.update(sessionid, relations);

        relation = dao.findByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1' and entityFK = 'xx2' and included = true", null);
        assertNotNull(relation);

        //Remove
        relation.setAction(IDataRow.DELETE);
        dao.update(sessionid, relation);
        
        relation = dao.findByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1' and entityFK = 'xx2' and included = true", null);
        assertNull(relation);
    }

    /**
     * Test of persist method, of class AbstractDAO.
     */
    @Test
    public void test10Persist() throws Exception {
        System.out.println("10-AbstractDAO - persist");
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
        relation.setFechacreacion(LocalDates.now());
        relation.setFechamodificacion(LocalDates.now());
        relation.setFieldsFK("id");
        relation.setFieldsPK("id");
        IDataResult dataResult = dao.persist(sessionid, relation);
        
        AppTablesRelation rowResult = dataResult.getRowUpdated();
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
    public void test11Merge() throws Exception {
        System.out.println("11-AbstractDAO - merge");
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
        relation.setFechacreacion(LocalDates.now());
        relation.setFechamodificacion(LocalDates.now());
        relation.setFieldsFK("id");
        relation.setFieldsPK("id");
        relation.setIncluded(false);
        dao.persist(sessionid, relation);
        
        relation = dao.findByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1'", null);
        relation.setIncluded(true);
        
        IDataResult dataResult = dao.merge(sessionid, relation);
        AppTablesRelation rowResult = dataResult.getRowUpdated();
        assertEquals(relation.isIncluded(),rowResult.isIncluded());

        relation = dao.findByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1' and entityFK = 'xx2' and included = true", null);
        assertNotNull(relation);

        //Remove
        dao.remove(sessionid, relation);
    }

    /**
     * Test of remove method, of class AbstractDAO.
     */
    @Test
    public void test12Remove() throws Exception{
        System.out.println("12-AbstractDAO - remove");
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
        relation.setFechacreacion(LocalDates.now());
        relation.setFechamodificacion(LocalDates.now());
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
    public void test13RefreshRow() throws Exception {
        System.out.println("13-AbstractDAO - refreshRow");
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
    public void test14GetCount() throws Exception {
        System.out.println("14-AbstractDAO - getCount");
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
    public void test15GetCount2() throws Exception {
        System.out.println("15-AbstractDAO - getCount2");
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
    public void test16GetEntityManagerProp() {
        System.out.println("16-AbstractDAO - getEntityManagerProp");
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
    public void test17GetPersistUnitProp() {
        System.out.println("17-AbstractDAO - getPersistUnitProp");
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
    public void test18GetDataEngine() throws Exception {
        System.out.println("18-AbstractDAO - getDataEngine");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        String expResult = dao.getDataEngine("PU1");
        assertNotNull(expResult);
    }

    /**
     * Test of getSchema method, of class AbstractDAO.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test19GetSchema() throws Exception {
        System.out.println("19-AbstractDAO - getSchema");
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
    public void test20GetUserSession() {
        System.out.println("20-AbstractDAO - getUserSession");
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
    public void test21GetDBLinkInfo() {
        System.out.println("21-AbstractDAO - getDBLinkInfo");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDBLinkInfo info = new DBLinkInfo();
        info.setUserSession(dao.getUserSession(sessionId));
        assertNotNull(info.getUserSession());
        assertNotNull(info.getSessionOrTokenId());
        assertNotNull(info.getAppUserId());
        assertNotNull(info.getIdCompany());
    }
    
    public class AbstractDAOImpl extends AbstractDAO {
    }
}

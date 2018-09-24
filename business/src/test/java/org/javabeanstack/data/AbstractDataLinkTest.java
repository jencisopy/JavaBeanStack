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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.javabeanstack.data.TestClass.error;
import org.javabeanstack.model.tables.AppResource;
import org.javabeanstack.model.tables.AppTablesRelation;
import org.javabeanstack.model.tables.AppUser;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jorge Enciso
 */
public class AbstractDataLinkTest extends TestClass {
    
    public AbstractDataLinkTest() {
    }

    /**
     * Test of getPersistUnit method, of class AbstractDataLink.
     */
    @Test
    public void testGetPersistUnit() {
        System.out.println("getPersistUnit");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        String result = dataLinkCat.getPersistUnit();
        String expResult = "PU1";
        assertNotNull(expResult, result);
    }

    
    /**
     * Test of find method, of class AbstractDataLink.
     */
    @Test
    public void testFind() throws Exception {
        System.out.println("find");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        AppUser user = dataLinkCat.find(AppUser.class, 1L);
        assertNotNull(user);
    }

    /**
     * Test of findByUk method, of class AbstractDataLink.
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
        appResource = dataLinkCat.findByUk(appResource);
        assertNull(appResource);
        
        appResource = new AppResource();
        appResource.setCode("clasemaker.xml");
        appResource = dataLinkCat.findByUk(appResource);
        assertNotNull(appResource);
    }

    /**
     * Test of findByQuery method, of class AbstractDataLink.
     */
    @Test
    public void testFindByQuery() throws Exception {
        System.out.println("findByQuery");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        AppUser user = dataLinkCat.findByQuery("select o from AppUser o where iduser = 1L", null);
        assertNotNull(user);

        user = dataLinkCat.findByQuery("select o from AppUser o where iduser = 0L", null);
        assertNull(user);
        
        Map<String, Object> params = new HashMap();
        params.put("iduser", 1L);
        user = dataLinkCat.findByQuery("select o from AppUser o where iduser = :iduser", params);
        assertNotNull(user);
    }

    /**
     * Test of findListByQuery method, of class AbstractDataLink.
     */
    @Test
    public void testFindListByQuery() throws Exception {
        System.out.println("findListByQuery");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<AppUser> users = dataLinkCat.findListByQuery("select o from AppUser o where iduser = 1L", null);
        assertNotNull(users);

        users = dataLinkCat.findListByQuery("select o from AppUser o where iduser = 0L", null);
        assertTrue(users.isEmpty());
        
        Map<String, Object> params = new HashMap();
        params.put("iduser", 1L);
        users = dataLinkCat.findListByQuery("select o from AppUser o where iduser = :iduser", params);
        assertNotNull(users);
    }

    
    /**
     * Test of findByNamedQuery method, of class AbstractDataLink.
     */
    @Test
    public void testFindByNamedQuery() throws Exception {
        System.out.println("findByNamedQuery");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<AppTablesRelation> prueba = dataLinkCat.findListByNamedQuery("AppTablesRelation.findAll", null);
        assertTrue(!prueba.isEmpty());
    }


    /**
     * Test of findByNativeQuery method, of class AbstractDataLink.
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
     * Test of persist method, of class AbstractDataLink.
     */
    @Test
    public void testPersist() throws Exception {
        System.out.println("persist");
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
        IDataResult dataResult = dataLinkCat.persist(relation);
        
        AppTablesRelation rowResult = dataResult.getRowResult();
        assertEquals(relation.getEntityPK(),rowResult.getEntityPK());

        List<AppTablesRelation>  relations = dataLinkCat.findListByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);
        assertTrue(relations.get(0).getEntityPK().trim().equals("xx1"));

        //Remove
        dataLinkCat.remove(relations.get(0));
    }

    /**
     * Test of persist method, of class AbstractDataLink.
     */
    @Test
    public void testPersist_List() throws Exception {
        System.out.println("persist");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<AppTablesRelation> relations = new ArrayList();
        //Persist
        for (int i = 0;i<5;i++){
            AppTablesRelation relation = new AppTablesRelation();
            relation.setEntityPK("xx1");
            relation.setEntityFK("xx"+i);
            relation.setFechacreacion(new Date());
            relation.setFechamodificacion(new Date());
            relation.setFieldsFK("id");
            relation.setFieldsPK("id");
            relations.add(relation);
        }
        
        dataLinkCat.persist(relations);        

        relations = dataLinkCat.findListByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);
        assertTrue(relations.get(0).getEntityPK().trim().equals("xx1"));

        //Remove
        dataLinkCat.remove(relations);
    }

    /**
     * Test of merge method, of class AbstractDataLink.
     */
    @Test
    public void testMerge_GenericType() throws Exception {
        System.out.println("merge");
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
        dataLinkCat.persist(relation);
        
        relation = dataLinkCat.findByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);
        relation.setIncluded(true);
        
        IDataResult dataResult = dataLinkCat.merge(relation);
        AppTablesRelation rowResult = dataResult.getRowResult();
        assertEquals(relation.isIncluded(),rowResult.isIncluded());

        relation = dataLinkCat.findByQuery("select o from AppTablesRelation o where entityPK = 'xx1' and entityFK = 'xx2' and included = true", null);
        assertNotNull(relation);

        //Remove
        dataLinkCat.remove(relation);
    }
    
    /**
     * Test of merge method, of class AbstractDataLink.
     */
    @Test
    public void testMerge_List() throws Exception {
        System.out.println("merge");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<AppTablesRelation> relations = new ArrayList();
        //Persist
        for (int i = 0;i<5;i++){
            AppTablesRelation relation = new AppTablesRelation();
            relation.setEntityPK("xx1");
            relation.setEntityFK("xx"+i);
            relation.setFechacreacion(new Date());
            relation.setFechamodificacion(new Date());
            relation.setFieldsFK("id");
            relation.setFieldsPK("id");
            relations.add(relation);
        }
        
        IDataResult dataResult = dataLinkCat.persist(relations);

        // Merge
        relations = dataLinkCat.findListByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);
        for (int i = 0;i<5;i++){
            relations.get(i).setIncluded(true);
        }
        dataLinkCat.merge(relations);

        relations = dataLinkCat.findListByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);
        for (int i = 0;i<5;i++){
            assertTrue(relations.get(i).isIncluded());
        }
        //Remove
        dataLinkCat.remove(relations);
    }

    /**
     * Test of remove method, of class AbstractDataLink.
     */
    @Test
    public void testRemove_GenericType() throws Exception {
        System.out.println("remove");
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
        dataLinkCat.persist(relation);
        
        relation = dataLinkCat.findByQuery("select o from AppTablesRelation o where entityPK = 'xx1' and entityFK = 'xx2'", null);
        assertNotNull(relation);

        //Remove
        dataLinkCat.remove(relation);
        
        relation = dataLinkCat.findByQuery("select o from AppTablesRelation o where entityPK = 'xx1' and entityFK = 'xx2'", null);
        assertNull(relation);
    }
    
    /**
     * Test of remove method, of class AbstractDataLink.
     */
    @Test
    public void testRemove_List() throws Exception {
        System.out.println("remove");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<AppTablesRelation> relations = new ArrayList();
        //Persist
        for (int i = 0;i<5;i++){
            AppTablesRelation relation = new AppTablesRelation();
            relation.setEntityPK("xx1");
            relation.setEntityFK("xx"+i);
            relation.setFechacreacion(new Date());
            relation.setFechamodificacion(new Date());
            relation.setFieldsFK("id");
            relation.setFieldsPK("id");
            relations.add(relation);
        }
        dataLinkCat.persist(relations);

        relations = dataLinkCat.findListByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);

        //Remove
        dataLinkCat.remove(relations);    
        
        relations = dataLinkCat.findListByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);        
        assertTrue(relations.isEmpty());
    }

    /**
     * Test of update method, of class AbstractDataLink.
     */
    @Test
    public void testUpdate_GenericType() throws Exception {
        System.out.println("update");
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
        dataLinkCat.update(relation);
        
        // Merge
        List<AppTablesRelation>  relations = dataLinkCat.findListByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);
        relations.get(0).setIncluded(true);
        relations.get(0).setAction(IDataRow.UPDATE);
        dataLinkCat.update(relations);

        relation = dataLinkCat.findByQuery("select o from AppTablesRelation o where entityPK = 'xx1' and entityFK = 'xx2' and included = true", null);
        assertNotNull(relation);

        //Remove
        relation.setAction(IDataRow.DELETE);
        dataLinkCat.update(relation);
        
        relation = dataLinkCat.findByQuery("select o from AppTablesRelation o where entityPK = 'xx1' and entityFK = 'xx2' and included = true", null);
        assertNull(relation);
    }

    /**
     * Test of update method, of class AbstractDataLink.
     */
    @Test
    public void testUpdate_List() throws Exception {
        System.out.println("update");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<AppTablesRelation> relations = new ArrayList();
        //Persist
        for (int i = 0;i<5;i++){
            AppTablesRelation relation = new AppTablesRelation();
            relation.setEntityPK("xx1");
            relation.setEntityFK("xx"+i);
            relation.setFechacreacion(new Date());
            relation.setFechamodificacion(new Date());
            relation.setFieldsFK("id");
            relation.setFieldsPK("id");
            relation.setAction(IDataRow.INSERT);
            relations.add(relation);
        }
        
        dataLinkCat.update(relations);

        // Merge
        relations = dataLinkCat.findListByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);
        for (int i = 0;i<5;i++){
            relations.get(i).setIncluded(true);
            relations.get(i).setAction(IDataRow.UPDATE);
        }
        dataLinkCat.update(relations);

        relations = dataLinkCat.findListByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);
        for (int i = 0;i<5;i++){
            assertTrue(relations.get(i).isIncluded());
            relations.get(i).setAction(IDataRow.DELETE);
        }
        //Remove
        dataLinkCat.update(relations);        
        relations = dataLinkCat.findListByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);
        assertTrue(relations.isEmpty());        
    }

    /**
     * Test of update method, of class AbstractDataLink.
     */
    @Test
    public void testUpdate_IDataSet() throws Exception {
        System.out.println("update");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataSet dataSet = new DataSet();
        
        List<AppTablesRelation> relations = new ArrayList();
        //Persist
        for (int i = 0;i<5;i++){
            AppTablesRelation relation = new AppTablesRelation();
            relation.setEntityPK("xx1");
            relation.setEntityFK("xx"+i);
            relation.setFechacreacion(new Date());
            relation.setFechamodificacion(new Date());
            relation.setFieldsFK("id");
            relation.setFieldsPK("id");
            relation.setAction(IDataRow.INSERT);
            relations.add(relation);
        }
        dataSet.add("relacion", relations);
        dataLinkCat.update(dataSet);
        
        // Merge
        relations = dataLinkCat.findListByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);
        dataSet.add("relacion", relations);        
        for (int i = 0;i<5;i++){
            relations.get(i).setIncluded(true);
            relations.get(i).setAction(IDataRow.UPDATE);
        }
        dataLinkCat.update(dataSet);

        relations = dataLinkCat.findListByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);
        dataSet.add("relacion", relations);
        for (int i = 0;i<5;i++){
            assertTrue(relations.get(i).isIncluded());
            relations.get(i).setAction(IDataRow.DELETE);
        }
        //Remove
        dataLinkCat.update(dataSet);
        relations = dataLinkCat.findListByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);
        assertTrue(relations.isEmpty());        
        
    }


    /**
     * Test of refreshRow method, of class AbstractDataLink.
     */
    @Test
    public void testRefreshRow() throws Exception {
        System.out.println("refreshRow");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        AppResource resource = dataLinkCat.findByQuery("select o from AppResource o where code = 'clasemaker.xml'", null);
        resource = dataLinkCat.refreshRow(resource);
        assertNotNull(resource);
    }

    /**
     * Test of getCount method, of class AbstractDataLink.
     */
    @Test
    public void testGetCount() throws Exception {
        System.out.println("getCount");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        Long rec = dataLinkCat.getCount("select o FROM AppCompany o", null);
        assertTrue(rec > 0L);
    }

    /**
     * Test of getCount2 method, of class AbstractDataLink.
     */
    @Test
    public void testGetCount2() throws Exception {
        System.out.println("getCount2");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        // {schema} se reemplaza automáticamente por el nombre del schema
        Long rec = dataLinkCat.getCount2("select * FROM {schema}.empresa", null);
        assertTrue(rec > 0L);
    }

    /**
     * Test of getEntityManagerProp method, of class AbstractDataLink.
     */
    @Test
    public void testGetEntityManagerProp() {
        System.out.println("getEntityManagerProp");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        Map<String, Object> props = dataLinkCat.getEntityManagerProp();
        assertNotNull(props);
    }

    /**
     * Test of getPersistUnitProp method, of class AbstractDataLink.
     */
    @Test
    public void testGetPersistUnitProp() {
        System.out.println("getPersistUnitProp");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        Map<String, Object> props = dataLinkCat.getPersistUnitProp();
        assertNotNull(props);        
    }

    /**
     * Test of getUserSession method, of class AbstractDataLink.
     */
    @Test
    public void testGetUserSession() {
        System.out.println("getUserSession");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        assertNotNull(dataLink.getUserSession());
    }


    /**
     * Test of getDBLinkInfo method, of class AbstractDataLink.
     */
    @Test
    public void testGetDBLinkInfo() {
        System.out.println("getDBLinkInfo");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDBLinkInfo info = dataLink.getUserSession().getDbLinkInfo();
        assertNotNull(info);
    }

    /**
     * Test of getSessionId method, of class AbstractDataLink.
     */
    @Test
    public void testGetSessionId() {
        System.out.println("getSessionId");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        assertNotNull(dataLink.getUserSession().getSessionId());
    }
}

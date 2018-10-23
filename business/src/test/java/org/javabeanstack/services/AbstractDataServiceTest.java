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
package org.javabeanstack.services;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.javabeanstack.data.IDBLinkInfo;
import org.javabeanstack.data.IDataResult;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.data.IDataSet;
import org.javabeanstack.data.TestClass;
import org.javabeanstack.data.model.DataSet;
import org.javabeanstack.datactrl.DataObject;
import org.javabeanstack.datactrl.IDataObject;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.model.tables.AppResource;
import org.javabeanstack.model.tables.AppTablesRelation;
import org.javabeanstack.model.tables.AppUser;
import org.javabeanstack.model.tables.AppUserMember;
import org.javabeanstack.model.tables.Moneda;
import org.javabeanstack.model.tables.Pais;
import org.javabeanstack.model.tables.Region;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 *
 * @author Jorge Enciso
 */
public class AbstractDataServiceTest extends TestClass{
    private static IDataServiceRemote dataService;
    
    public AbstractDataServiceTest() {
    }

    @BeforeClass
    public static void setUpClass2() {
        try {
            dataService = 
                (IDataServiceRemote) context.lookup(jndiProject+"DataService!org.javabeanstack.services.IDataServiceRemote");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    @Test
    public void testInstance() throws Exception {
        System.out.println("DataService - TestInstance");        
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataServiceRemote instance  = 
                (IDataServiceRemote) context.lookup(jndiProject+"DataService!org.javabeanstack.services.IDataServiceRemote");
        assertNotNull(instance);
    }

    /** Prueba control de los unique keys
     * @throws java.lang.Exception */
    @Test
    public void testCheckUnique1() throws Exception {
        System.out.println("DataService - CheckUnique1");                
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        Moneda row = dataService.find(Moneda.class,sessionId).get(0);
        // Va a pasar la prueba porque es el mismo objeto
        assertTrue(dataService.checkUniqueKey("", row));
    }

    /** Prueba control de los unique keys
     * @throws java.lang.Exception */
    @Test    
    public void testCheckUnique2() throws Exception {
        System.out.println("DataService - TestCheckUnique2");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<Moneda> rows = dataService.find(Moneda.class,sessionId);
        Moneda row = rows.get(0);
        // Necesita del parametro sessionId para acceder a la unidad de persistencia adecuado
        assertTrue(dataService.checkUniqueKey(sessionId, row));
    }

    /** Prueba unique key
     * @throws java.lang.Exception */
    @Test    
    public void testCheckUniqueKey3() throws Exception {
        System.out.println("DataService - TestCheckUniqueKey3");                
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        Moneda moneda = dataService.find(Moneda.class,sessionId).get(0);
        
        Map<String, IErrorReg> errors;

        moneda.setAction(IDataRow.MODIFICAR);
        errors = dataService.checkDataRow(sessionId, moneda);
        assertTrue(errors.isEmpty());

        moneda.setIdmoneda(0L);        
        moneda.setAction(IDataRow.AGREGAR);        
        errors = dataService.checkDataRow(sessionId, moneda);
        assertFalse(errors.isEmpty());

        moneda.setAction(IDataRow.BORRAR);        
        errors = dataService.checkDataRow(sessionId, moneda);
        assertTrue(errors.isEmpty());
        
        moneda.setCodigo("xxx");
        moneda.setAction(IDataRow.AGREGAR);        
        errors = dataService.checkDataRow(sessionId, moneda);
        assertTrue(errors.isEmpty());
     }        
    

    /** Prueba de chequeo de los foreignkeys
     * @throws java.lang.Exception */
    @Test    
    public void testCheckForeignkey() throws Exception {
        System.out.println("DataService - TestCheckForeignkey");        
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        AppUserMember userMember = dataService.find(AppUserMember.class,null).get(0);
        assertTrue(dataService.checkForeignKey("",userMember,"usergroup"));
        userMember.setUserGroup(null);
        assertFalse(dataService.checkForeignKey("",userMember,"usergroup"));
    }    

    /** Chequeo de los foreignkeys
     * @throws java.lang.Exception */
    @Test    
    public void testCheckForeignKey2() throws Exception {
        System.out.println("DataService - TestCheckForeignkey2");                
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        Map<String, IErrorReg> errors;        
        Pais pais = dataService.find(Pais.class,sessionId).get(0);
        pais.setAction(IDataRow.MODIFICAR);
        // Necesita del parametro sessionId para acceder a la unidad de persistencia adecuado        
        errors = dataService.checkDataRow(sessionId, pais);
        assertTrue(errors.isEmpty());
    }    

    /** Chequeo de los foreignkeys
     * @throws java.lang.Exception */
    @Test            
    public void testCheckForeignKey3() throws Exception {
        System.out.println("DataService - TestCheckForeignkey3");                
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        AppUserMember usuarioMiembro = dataService.find(AppUserMember.class,null).get(0);
        usuarioMiembro.setAction(IDataRow.MODIFICAR);
        
        Map<String, IErrorReg> errors = dataService.checkDataRow("", usuarioMiembro);
        assertTrue(errors.isEmpty());
    }
    
    /**
     * Test of setListCheckMethods method, of class AbstractDataService.
     */
    //@Test
    public void testSetListCheckMethods() {
        System.out.println("DataService - setListCheckMethods");
    }

    /**
     * Test of setListFieldCheck method, of class AbstractDataService.
     */
    //@Test
    public void testSetListFieldCheck() {
        System.out.println("DataService - setListFieldCheck");
    }

    /**
     * Test of findById method, of class AbstractDataService.
     */
    @Test
    public void testFindById() throws Exception {
        System.out.println("DataService - findById");
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        AppUser user = dataService.findById(AppUser.class, sessionid, 1L);
        assertNotNull(user);
        
        user = dataService.findById(AppUser.class, sessionid, 0L);
        assertNull(user);
    }

    /**
     * Test of findByUk method, of class AbstractDataService.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testFindByUk() throws Exception {
        System.out.println("DataService - findByUk");
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        AppResource appResource = new AppResource();
        appResource.setCode("no se encuentra");
        appResource = dataService.findByUk(sessionid, appResource);
        assertNull(appResource);
        
        appResource = new AppResource();
        appResource.setCode("clasemaker.xml");
        appResource = dataService.findByUk(sessionid, appResource);
        assertNotNull(appResource);
    }
    

    /**
     * Test of find method, of class AbstractDataService.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testFind() throws Exception {
        System.out.println("DataService - find");
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<AppUser> users = dataService.find(AppUser.class, sessionid);
        assertTrue(users.size() > 0);
        
        String order = "code desc";
        String filter = "";
        users = dataService.find(AppUser.class, sessionid, order, filter, null);
        assertTrue(users.size() > 0);
        
        filter = "code = 'Administrador'";
        users = dataService.find(AppUser.class, sessionid, order, filter, null);
        assertTrue(users.size() == 1);
        
        filter = "code = :code";
        Map<String, Object> params = new HashMap();
        params.put("code", "Administrador");
        users = dataService.find(AppUser.class, sessionid, order, filter, params);
        assertTrue(users.size() == 1);
        
        users = dataService.find(AppUser.class, sessionid, null, "", null, 0, 4);        
        assertTrue(users.size() == 4);
    }

    /**
     * Test of findByQuery method, of class AbstractDataService.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testFindByQuery() throws Exception {
        System.out.println("DataService - findByQuery");
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        AppUser user = dataService.findByQuery(sessionid, "select o from AppUser o where iduser = 1L", null);
        assertNotNull(user);

        user = dataService.findByQuery(sessionid, "select o from AppUser o where iduser = 0L", null);
        assertNull(user);
        
        Map<String, Object> params = new HashMap();
        params.put("iduser", 1L);
        user = dataService.findByQuery(sessionid, "select o from AppUser o where iduser = :iduser", params);
        assertNotNull(user);
    }

    /**
     * Test of findListByQuery method, of class AbstractDataService.
     */
    @Test
    public void testFindListByQuery() throws Exception {
        System.out.println("DataService - findListByQuery");
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<AppUser> users = dataService.findListByQuery(sessionid, "select o from AppUser o where iduser = 1L", null);
        assertNotNull(users);

        users = dataService.findListByQuery(sessionid, "select o from AppUser o where iduser = 0L", null);
        assertTrue(users.isEmpty());
        
        Map<String, Object> params = new HashMap();
        params.put("iduser", 1L);
        users = dataService.findListByQuery(sessionid, "select o from AppUser o where iduser = :iduser", params);
        assertNotNull(users);
    }

    /**
     * Test of findByNamedQuery method, of class AbstractDataService.
     */
    @Test
    public void testFindByNamedQuery() throws Exception {
        System.out.println("DataService - findByNamedQuery");
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        
        List<AppTablesRelation> prueba = dataService.findListByNamedQuery(sessionid, "AppTablesRelation.findAll", null);
        assertTrue(!prueba.isEmpty());
    }

    /**
     * Test of findByNativeQuery method, of class AbstractDataService.
     */
    @Test
    public void testFindByNativeQuery() throws Exception {
        System.out.println("DataService - findByNativeQuery");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sqlSentence = "select * from {schema}.moneda where idmoneda > :id";
        Map<String, Object> params = new HashMap();
        params.put("id",0);
        List<Object> query1 = dataService.findByNativeQuery(sessionId, sqlSentence, params);
        assertTrue(!query1.isEmpty());
        
        // Un grupo de registros first, max
        query1 = dataService.findByNativeQuery(sessionId, sqlSentence, params,0,10);
        assertTrue(!query1.isEmpty());
    }


    /**
     * Test of refreshRow method, of class AbstractDataService.
     */
    @Test
    public void testRefreshRow() throws Exception {
        System.out.println("DataService - refreshRow");
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        AppResource resource = dataService.findByQuery(sessionid, "select o from AppResource o where code = 'clasemaker.xml'", null);
        resource = dataService.refreshRow(sessionid, resource);
        assertNotNull(resource);
    }

    /**
     * Test of getCount method, of class AbstractDataService.
     */
    @Test
    public void testGetCount() throws Exception {
        System.out.println("DataService - getCount");
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        Long rec = dataService.getCount(sessionid, "select o FROM AppCompany o", null);
        assertTrue(rec > 0L);
    }

    /**
     * Test of getCount2 method, of class AbstractDataService.
     */
    @Test
    public void testGetCount2() throws Exception {
        System.out.println("DataService - getCount2");
        // Cuando sessionId es null solo se puede acceder al schema catalogo
        String sessionid = null;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        // {schema} se reemplaza automáticamente por el nombre del schema
        Long rec = dataService.getCount2(sessionid, "select * FROM {schema}.empresa", null);
        assertTrue(rec > 0L);
    }

    /**
     * Test of getDataRows method, of class AbstractDataService.
     */
    @Test
    public void getDataRows() throws Exception{
        System.out.println("DataService - getDataRows");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<Moneda> monedas = dataService.getDataRows(sessionId, Moneda.class,"" , "", null,0,1000);
        assertFalse(monedas.isEmpty());
    }
    

    /**
     * Test of persist method, of class AbstractDataService.
     */
    @Test
    public void testPersist() throws Exception {
        System.out.println("DataService - persist");
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
        IDataResult dataResult = dataService.persist(sessionid, relation);
        
        AppTablesRelation rowResult = dataResult.getRowUpdated();
        assertEquals(relation.getEntityPK(),rowResult.getEntityPK());
        
        List<AppTablesRelation>  relations = dataService.findListByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1'", null);

        assertTrue(relations.get(0).getEntityPK().trim().equals("xx1"));

        //Remove
        dataService.remove(sessionid, relations.get(0));
    }

    /**
     * Test of merge method, of class AbstractDataService.
     */
    @Test
    public void testMerge() throws Exception {
        System.out.println("DataService - merge");
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
        IDataResult dataResult = dataService.persist(sessionid, relation);
        
        relation = dataResult.getRowUpdated();
        relation.setIncluded(true);
        
        dataResult = dataService.merge(sessionid, relation);
        AppTablesRelation rowResult = dataResult.getRowUpdated();
        assertEquals(relation.isIncluded(),rowResult.isIncluded());

        relation = dataService.findByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1' and entityFK = 'xx2' and included = true", null);
        assertNotNull(relation);

        //Remove
        dataService.remove(sessionid, relation);
    }

    /**
     * Test of remove method, of class AbstractDataService.
     */
    @Test
    public void testRemove() throws Exception{
        System.out.println("DataService - remove");
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
        dataService.persist(sessionid, relation);
        
        relation = dataService.findByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1' and entityFK = 'xx2'", null);
        assertNotNull(relation);

        //Remove
        dataService.remove(sessionid, relation);
        
        relation = dataService.findByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1' and entityFK = 'xx2'", null);
        assertNull(relation);
    }


    /**
     * Test of update method, of class AbstractDataService.
     */
    @Test
    public void testUpdate() throws Exception{
        System.out.println("DataService - update");
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
        relation.setRowChecked(true);
        dataService.update(sessionid, relation);
        
        // Merge
        List<AppTablesRelation>  relations = dataService.findListByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1'", null);
        relations.get(0).setIncluded(true);
        relations.get(0).setAction(IDataRow.UPDATE);
        for (AppTablesRelation relation1:relations){
            relation1.setRowChecked(true);
        }
        dataService.update(sessionid, relations);

        relation = dataService.findByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1' and entityFK = 'xx2' and included = true", null);
        assertNotNull(relation);

        //Remove
        relation.setAction(IDataRow.DELETE);
        relation.setRowChecked(true);
        dataService.update(sessionid, relation);
        
        relation = dataService.findByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1' and entityFK = 'xx2' and included = true", null);
        assertNull(relation);
    }

    /**
     * Test of update method, of class AbstractDataService.
     */
    @Test
    public void testUpdate_String_IDataObject() throws Exception{
        System.out.println("DataService - update");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        //Region
        IDataObject region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        if (region.find("codigo", "ZZZ")){
            region.refreshRow();
            region.deleteRow();
            region.checkDataRow();
            IDataResult dataResult = dataService.update(sessionId, region);
            assertTrue(dataResult.isSuccessFul());
        }
        region.insertRow();
        region.setField("codigo", "ZZZ");
        region.setField("nombre", "ZZZ BORRAR");
        region.checkDataRow();
        IDataResult dataResult = dataService.update(sessionId, region);
        assertTrue(dataResult.isSuccessFul());

        region.close();
        region.open();
        if (region.find("codigo", "ZZZ")){
            region.deleteRow();
            region.checkDataRow();
            dataResult = dataService.update(sessionId, region);
            assertTrue(dataResult.isSuccessFul());
        }
    }

    /**
     * Test of update method, of class AbstractDataService.
     */
    @Test
    public void testUpdate_String_List() throws Exception{
        System.out.println("DataService - update");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        //Region
        IDataObject region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        if (region.find("codigo", "ZZZ")){
            region.refreshRow();
            region.deleteRow();
            region.checkDataRow();
            IDataResult dataResult = dataService.update(sessionId, region.getDataRows());
            assertTrue(dataResult.isSuccessFul());
        }
        region.insertRow();
        region.setField("codigo", "ZZZ");
        region.setField("nombre", "ZZZ BORRAR");
        region.checkDataRow();
        
        IDataResult dataResult = dataService.update(sessionId, region.getDataRows());
        assertTrue(dataResult.isSuccessFul());

        region.close();
        region.open();
        if (region.find("codigo", "ZZZ")){
            region.deleteRow();
            region.checkDataRow();
            dataResult = dataService.update(sessionId, region.getDataRows());
            assertTrue(dataResult.isSuccessFul());
        }
    }

    /**
     * Test of update method, of class AbstractDataService.
     */
    @Test
    public void testUpdate_String_IDataSet() throws Exception{
        System.out.println("DataService - update");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        //Region
        IDataObject region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        IDataSet dataSet = new DataSet();
        dataSet.addDataObject("region", region);
        
        if (region.find("codigo", "ZZZ")){
            region.refreshRow();
            region.deleteRow();
            region.checkDataRow();            
            IDataResult dataResult = dataService.update(sessionId, dataSet);
            assertTrue(dataResult.isSuccessFul());
        }
        region.insertRow();
        region.setField("codigo", "ZZZ");
        region.setField("nombre", "ZZZ BORRAR");
        region.checkDataRow();
        
        dataSet = new DataSet();
        dataSet.addDataObject("region", region);

        IDataResult dataResult = dataService.update(sessionId, dataSet);
        assertTrue(dataResult.isSuccessFul());

        region.close();
        region.open();
        if (region.find("codigo", "ZZZ")){
            region.deleteRow();
            region.checkDataRow();
            dataSet = new DataSet();
            dataSet.addDataObject("region", region);
            dataResult = dataService.update(sessionId, dataSet);
            assertTrue(dataResult.isSuccessFul());
        }
    }

    /**
     * Test of save method, of class AbstractDataService.
     */
    @Test
    public void testSave() throws Exception{
        System.out.println("DataService - save");
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
        dataService.save(sessionid, relation);
        
        // Merge
        List<AppTablesRelation>  relations = dataService.findListByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1'", null);
        relations.get(0).setIncluded(true);
        relations.get(0).setAction(IDataRow.UPDATE);
        dataService.save(sessionid, relations.get(0));

        relation = dataService.findByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1' and entityFK = 'xx2' and included = true", null);
        assertNotNull(relation);

        //Remove
        relation.setAction(IDataRow.DELETE);
        dataService.save(sessionid, relation);
        
        relation = dataService.findByQuery(sessionid, "select o from AppTablesRelation o where entityPK = 'xx1' and entityFK = 'xx2' and included = true", null);
        assertNull(relation);
    }
    
    /**
     * Test of getEntityManagerProp method, of class AbstractDataLink.
     */
    @Test
    public void testGetEntityManagerProp() throws Exception {
        System.out.println("DataService - getEntityManagerProp");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        Map<String, Object> props = dataService.getEntityManagerProp(dataLink.getPersistUnit());
        assertNotNull(props);
    }

    /**
     * Test of getPersistUnitProp method, of class AbstractDataLink.
     */
    @Test
    public void testGetPersistUnitProp() throws Exception{
        System.out.println("DataService - getPersistUnitProp");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        Map<String, Object> props = dataService.getPersistUnitProp(dataLink.getPersistUnit());
        assertNotNull(props);
    }

    /**
     * Test of getUserSession method, of class AbstractDataLink.
     */
    @Test
    public void testGetUserSession() throws Exception{
        System.out.println("DataService - getUserSession");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        assertNotNull(dataService.getUserSession(sessionId));
    }

    /**
     * Test of getDBLinkInfo 
     */
    @Test
    public void testGetDBLinkInfo() throws Exception{
        System.out.println("DataService - getDBLinkInfo");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDBLinkInfo info = dataService.getUserSession(sessionId).getDbLinkInfo();
        assertNotNull(info);
    }

    
    /**
     * Test of getDataEngine method, of class AbstractDataService.
     */
    @Test
    public void testGetDataEngine() throws Exception {
        System.out.println("DataService - getDataEngine");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        System.out.println(dataService.getDataEngine("PU1"));

        String expResult = dataService.getDataEngine("PU1");
        assertNotNull(expResult);
    }

    /**
     * Test of getSchema method, of class AbstractDataService.
     */
    @Test
    public void testGetSchema() throws Exception {
        System.out.println("DataService - getSchema");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        System.out.println(dataService.getSchema("PU1"));

        String expResult = dataService.getSchema("PU1");
        assertNotNull(expResult);
    }
}

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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.javabeanstack.data.model.DataSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.javabeanstack.data.TestClass.context;
import static org.javabeanstack.data.TestClass.error;
import org.javabeanstack.data.services.IAppCompanySrv;
import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.model.IAppAuthConsumer;
import org.javabeanstack.model.appcatalog.AppResource;
import org.javabeanstack.model.appcatalog.AppTablesRelation;
import org.javabeanstack.model.appcatalog.AppUser;
import org.javabeanstack.model.tables.Moneda;
import org.javabeanstack.security.IOAuthConsumerData;
import org.javabeanstack.security.OAuthConsumerBase;
import org.javabeanstack.security.model.OAuthConsumerData;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.javabeanstack.model.IAppAuthConsumerToken;
import org.javabeanstack.util.LocalDates;

/**
 *
 * @author Jorge Enciso
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AbstractDataLinkTest extends TestClass {

    private String token;
    private static IDataService dao;
    private String consumerKey;
    private final String uuidDevice = "xxxx1111133333";

    public AbstractDataLinkTest() {
    }

    @BeforeClass
    public static void setUpClass2() {
        try {
            dao = (IDataService) context.lookup(jndiProject + "DataService!org.javabeanstack.data.services.IDataServiceRemote");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Test of getPersistUnit method, of class AbstractDataLink.
     */
    @Test
    public void test01GetPersistUnit() {
        System.out.println("1-DataLink - getPersistUnit");
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
    public void test02Find() throws Exception {
        System.out.println("2-DataLink - find");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        AppUser user = dataLinkCat.findById(AppUser.class, 1L);
        assertNotNull(user);
    }

    /**
     * Test of findByUk method, of class AbstractDataLink.
     */
    @Test
    public void test03FindByUk() throws Exception {
        System.out.println("3-DataLink - findByUk");
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
    public void test04FindByQuery() throws Exception {
        System.out.println("4-DataLink - findByQuery");
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
    public void test05FindListByQuery() throws Exception {
        System.out.println("5-DataLink - findListByQuery");
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
    public void test06FindByNamedQuery() throws Exception {
        System.out.println("6-DataLink - findByNamedQuery");
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
    public void test07FindByNativeQuery() throws Exception {
        System.out.println("7-DataLink - findByNativeQuery");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        String sqlSentence = "select * from {schema}.moneda where idmoneda > :id";
        Map<String, Object> params = new HashMap();
        params.put("id", 0);
        List<Object> query1 = dataLink.findByNativeQuery(sqlSentence, params);
        assertTrue(!query1.isEmpty());

        // Un grupo de registros first, max
        query1 = dataLink.findByNativeQuery(sqlSentence, params, 0, 10);
        assertTrue(!query1.isEmpty());
    }

    /**
     * Test of persist method, of class AbstractDataLink.
     */
    @Test
    public void test08Persist() throws Exception {
        System.out.println("8-DataLink - persist");
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
        IDataResult dataResult = dataLinkCat.persist(relation);

        AppTablesRelation rowResult = dataResult.getRowUpdated();
        assertEquals(relation.getEntityPK(), rowResult.getEntityPK());

        List<AppTablesRelation> relations = dataLinkCat.findListByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);
        assertTrue(relations.get(0).getEntityPK().trim().equals("xx1"));

        //Remove
        dataLinkCat.remove(relations.get(0));
    }

    /**
     * Test of persist method, of class AbstractDataLink.
     */
    @Test
    public void test09Persist2() throws Exception {
        System.out.println("9-DataLink - persist2");
        if (error != null) {
            System.out.println(error);
            return;
        }
        Moneda moneda = new Moneda();
        moneda.setCodigo("PYG");
        moneda.setNombre("Guarani");
        moneda.setCambio(BigDecimal.ONE);
        moneda.setObservacion("34424");
        //Error al propósito
        IDataResult dataResult = dataLink.persist(moneda);
        Moneda monedaResult = dataResult.getRowUpdated();
        List<Moneda> monedasResult = dataResult.getRowsUpdated();
        assertNull(monedaResult);
        assertFalse(monedasResult.get(0).getErrors().isEmpty());

        for (Map.Entry<String, IErrorReg> entry : monedasResult.get(0).getErrors().entrySet()) {
            System.out.println("Key: " + entry.getKey()
                    + " fieldName: " + entry.getValue().getFieldName()
                    + " Msg: " + entry.getValue().getMessage());
        }
    }

    /**
     * Test of persist method, of class AbstractDataLink.
     */
    @Test
    public void test10Persist3() throws Exception {
        System.out.println("10-DataLink - persist3");
        if (error != null) {
            System.out.println(error);
            return;
        }
        Moneda moneda = new Moneda();
        moneda.setCodigo("PYG");
        moneda.setNombre("Guarani");
        moneda.setCambio(BigDecimal.ONE);
        moneda.setObservacion("34424");
        moneda.setIdempresa(99L);
        //Error al propósito
        IDataResult dataResult = dataLink.persist(moneda);
        Moneda monedaResult = dataResult.getRowUpdated();
        List<Moneda> monedasResult = dataResult.getRowsUpdated();
        assertNull(monedaResult);
        assertFalse(monedasResult.get(0).getErrors().isEmpty());
        assertFalse(dataResult.getErrorMsg().isEmpty());

        for (Map.Entry<String, IErrorReg> entry : monedasResult.get(0).getErrors().entrySet()) {
            System.out.println("Key: " + entry.getKey()
                    + " fieldName: " + entry.getValue().getFieldName()
                    + " Msg: " + entry.getValue().getMessage());
        }
    }

    /**
     * Test of persist method, of class AbstractDataLink.
     */
    @Test
    public void test11Persist4() throws Exception {
        System.out.println("11-DataLink - persist4");
        if (error != null) {
            System.out.println(error);
            return;
        }
        Moneda moneda = new Moneda();
        moneda.setCodigo("XXX");
        moneda.setNombre("XXXXXXX");
        moneda.setCambio(BigDecimal.ONE);
        moneda.setIdempresa(dataLink.getUserSession().getIdCompany());

        IDataResult dataResult = dataLink.persist(moneda);
        Moneda monedaResult = dataResult.getRowUpdated();
        List<Moneda> monedasResult = dataResult.getRowsUpdated();
        if (!dataResult.isSuccessFul()) {
            for (Map.Entry<String, IErrorReg> entry : monedasResult.get(0).getErrors().entrySet()) {
                System.out.println("Key: " + entry.getKey()
                        + " fieldName: " + entry.getValue().getFieldName()
                        + " Msg: " + entry.getValue().getMessage());
            }
        }
        assertNotNull(monedaResult);
        assertTrue(monedasResult.get(0).getErrors().isEmpty());
        assertTrue(dataResult.getErrorMsg().isEmpty());

        dataResult = dataLink.remove(monedaResult);
        assertTrue(dataResult.isSuccessFul());

    }

    /**
     * Test of persist method, of class AbstractDataLink.
     */
    @Test
    public void test12Persist_List() throws Exception {
        System.out.println("12-DataLink - persist");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<AppTablesRelation> relations = new ArrayList();
        //Persist
        for (int i = 0; i < 5; i++) {
            AppTablesRelation relation = new AppTablesRelation();
            relation.setEntityPK("xx1");
            relation.setEntityFK("xx" + i);
            relation.setFechacreacion(LocalDates.now());
            relation.setFechamodificacion(LocalDates.now());
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
    public void test13Merge_GenericType() throws Exception {
        System.out.println("13-DataLink - merge");
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
        dataLinkCat.persist(relation);

        relation = dataLinkCat.findByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);
        relation.setIncluded(true);

        IDataResult dataResult = dataLinkCat.merge(relation);
        AppTablesRelation rowResult = dataResult.getRowUpdated();
        assertEquals(relation.isIncluded(), rowResult.isIncluded());

        relation = dataLinkCat.findByQuery("select o from AppTablesRelation o where entityPK = 'xx1' and entityFK = 'xx2' and included = true", null);
        assertNotNull(relation);

        //Remove
        dataLinkCat.remove(relation);
    }

    /**
     * Test of merge method, of class AbstractDataLink.
     */
    @Test
    public void test14Merge_List() throws Exception {
        System.out.println("14-DataLink - merge");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<AppTablesRelation> relations = new ArrayList();
        //Persist
        for (int i = 0; i < 5; i++) {
            AppTablesRelation relation = new AppTablesRelation();
            relation.setEntityPK("xx1");
            relation.setEntityFK("xx" + i);
            relation.setFechacreacion(LocalDates.now());
            relation.setFechamodificacion(LocalDates.now());
            relation.setFieldsFK("id");
            relation.setFieldsPK("id");
            relations.add(relation);
        }

        IDataResult dataResult = dataLinkCat.persist(relations);

        // Merge
        relations = dataLinkCat.findListByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);
        for (int i = 0; i < 5; i++) {
            relations.get(i).setIncluded(true);
        }
        dataLinkCat.merge(relations);

        relations = dataLinkCat.findListByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);
        for (int i = 0; i < 5; i++) {
            assertTrue(relations.get(i).isIncluded());
        }
        //Remove
        dataLinkCat.remove(relations);
    }

    /**
     * Test of remove method, of class AbstractDataLink.
     */
    @Test
    public void test15Remove_GenericType() throws Exception {
        System.out.println("15-DataLink - remove");
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
    public void test16Remove_List() throws Exception {
        System.out.println("16-DataLink - remove");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<AppTablesRelation> relations = new ArrayList();
        //Persist
        for (int i = 0; i < 5; i++) {
            AppTablesRelation relation = new AppTablesRelation();
            relation.setEntityPK("xx1");
            relation.setEntityFK("xx" + i);
            relation.setFechacreacion(LocalDates.now());
            relation.setFechamodificacion(LocalDates.now());
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
    public void test17Update_GenericType() throws Exception {
        System.out.println("17-DataLink - update");
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
        dataLinkCat.update(relation);

        // Merge
        List<AppTablesRelation> relations = dataLinkCat.findListByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);
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
    public void test18Update_List() throws Exception {
        System.out.println("18-DataLink - update");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        List<AppTablesRelation> relations = new ArrayList();
        //Persist
        for (int i = 0; i < 5; i++) {
            AppTablesRelation relation = new AppTablesRelation();
            relation.setEntityPK("xx1");
            relation.setEntityFK("xx" + i);
            relation.setFechacreacion(LocalDates.now());
            relation.setFechamodificacion(LocalDates.now());
            relation.setFieldsFK("id");
            relation.setFieldsPK("id");
            relation.setAction(IDataRow.INSERT);
            relations.add(relation);
        }

        dataLinkCat.update(relations);

        // Merge
        relations = dataLinkCat.findListByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);
        for (int i = 0; i < 5; i++) {
            relations.get(i).setIncluded(true);
            relations.get(i).setAction(IDataRow.UPDATE);
        }
        dataLinkCat.update(relations);

        relations = dataLinkCat.findListByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);
        for (int i = 0; i < 5; i++) {
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
    public void test19Update_IDataSet() throws Exception {
        System.out.println("19-DataLink - update");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataSet dataSet = new DataSet();

        List<AppTablesRelation> relations = new ArrayList();
        //Persist
        for (int i = 0; i < 5; i++) {
            AppTablesRelation relation = new AppTablesRelation();
            relation.setEntityPK("xx1");
            relation.setEntityFK("xx" + i);
            relation.setFechacreacion(LocalDates.now());
            relation.setFechamodificacion(LocalDates.now());
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
        for (int i = 0; i < 5; i++) {
            relations.get(i).setIncluded(true);
            relations.get(i).setAction(IDataRow.UPDATE);
        }
        dataLinkCat.update(dataSet);

        relations = dataLinkCat.findListByQuery("select o from AppTablesRelation o where entityPK = 'xx1'", null);
        dataSet.add("relacion", relations);
        for (int i = 0; i < 5; i++) {
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
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test20RefreshRow() throws Exception {
        System.out.println("20-DataLink - refreshRow");
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
    public void test21GetCount() throws Exception {
        System.out.println("21-DataLink - getCount");
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
    public void test22GetCount2() throws Exception {
        System.out.println("22-DataLink - getCount2");
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
    public void test23GetEntityManagerProp() {
        System.out.println("23-DataLink - getEntityManagerProp");
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
    public void test24GetPersistUnitProp() {
        System.out.println("24-DataLink - getPersistUnitProp");
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
    public void test25GetUserSession() {
        System.out.println("25-DataLink - getUserSession");
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
    public void test26GetDBLinkInfo() {
        System.out.println("26-DataLink - getDBLinkInfo");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDBLinkInfo info = new DBLinkInfo();
        info.setUserSession(dataLink.getUserSession());
        assertNotNull(info);
    }

    /**
     * Test of getSessionId method, of class AbstractDataLink.
     */
    @Test
    public void test27GetSessionId() {
        System.out.println("27-DataLink - getSessionId");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        assertNotNull(dataLink.getUserSession().getSessionId());
    }

    /**
     * Test of getPersistUnit method, of class AbstractDataLink.
     */
    @Test
    public void test28Token() throws Exception {
        System.out.println("28-DataLink - getPersistUnit - token");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        createAuthConsumer();
        createToken();

        dataLink.setToken(token);

        String result = dataLink.getToken();
        assertNotNull(result);

        result = dataLink.getPersistUnit();
        String expResult = "PU2";
        assertEquals(expResult, result);

//        Long result2 = dataLink.getIdCompany();
//        Long expResult2 = 2L;
//        assertEquals(expResult2, result2);
        IDBLinkInfo dbInfo = dataLink.getDBLinkInfo();
        assertNotNull(dbInfo);

        String sqlSentence = "select * from {schema}.moneda";
        List<Object> query1 = dataLink.findByNativeQuery(sqlSentence, null);
        assertTrue(!query1.isEmpty());

        dropToken();
        dropAuthConsumer();
        dataLink.setToken(null);
    }

    /**
     * Test of createAuthConsumer method, of class OAuthConsumer.
     */
    public void createAuthConsumer() {
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        String consumerName = "OYM";
        LocalDateTime expiredDate = LocalDateTime.now();
        expiredDate = expiredDate.plusDays(5000L);
        OAuthConsumerBase instance = new OAuthConsumerImpl();
        instance.setDao(dao);
        String result = instance.createAuthConsumer(consumerName, expiredDate);
        assertNotNull(result);
        consumerKey = instance.getLastAuthConsumer().getConsumerKey();
    }

    public void createToken() {
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
    }

    public void dropToken() {
        OAuthConsumerBase instance = new OAuthConsumerImpl();
        instance.setDao(dao);
        instance.dropToken(consumerKey, uuidDevice);
    }

    public void dropAuthConsumer() {
        OAuthConsumerBase instance = new OAuthConsumerImpl();
        instance.setDao(dao);
        instance.dropAuthConsumer(consumerKey);
    }

    public class OAuthConsumerImpl extends OAuthConsumerBase {

        private IAppCompanySrv appCompanySrv;

        public OAuthConsumerImpl() {
            try {
                appCompanySrv = (IAppCompanySrv) context.lookup(jndiProject + "AppCompanySrv!org.javabeanstack.data.services.IAppCompanySrv");
            } catch (Exception exp) {
                System.out.println("Error instanciación appCompanySrv");
            }
        }

        @Override
        public Class<IAppAuthConsumer> getAuthConsumerClass() {
            try {
                return (Class<IAppAuthConsumer>) Class.forName("org.javabeanstack.model.appcatalog.AppAuthConsumer");
            } catch (ClassNotFoundException ex) {
                System.out.println(ex.getMessage());
            }
            return null;
        }

        @Override
        public Class<IAppAuthConsumerToken> getAuthConsumerTokenClass() {
            try {
                return (Class<IAppAuthConsumerToken>) Class.forName("org.javabeanstack.model.appcatalog.AppAuthConsumerToken");
            } catch (ClassNotFoundException ex) {
                System.out.println(ex.getMessage());
            }
            return null;
        }

        @Override
        protected IAppCompanySrv getAppCompanySrv() {
            return appCompanySrv;
        }
    }
}

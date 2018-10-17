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
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import javax.persistence.Query;
import org.javabeanstack.data.IDBConnectFactory;
import org.javabeanstack.data.IDataResult;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.data.IDataSet;
import org.javabeanstack.data.TestClass;
import org.javabeanstack.datactrl.IDataObject;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.model.tables.AppUser;
import org.javabeanstack.model.tables.AppUserMember;
import org.javabeanstack.model.tables.Moneda;
import org.javabeanstack.model.tables.Pais;
import org.javabeanstack.security.IUserSession;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jorge Enciso
 */
public class AbstractDataServiceTest extends TestClass{
    
    public AbstractDataServiceTest() {
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
        IDataService dataService  = 
                (IDataService) context.lookup(jndiProject+"UsuarioSrv!org.javabeanstack.services.IUsuarioSrvRemote");
        
        AppUser row = dataService.findById(AppUser.class,null ,1L);
        row.setUserMemberList(null);
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
        IDataServiceRemote dataService  = 
                (IDataServiceRemote) context.lookup(jndiProject+"DataService!org.javabeanstack.services.IDataServiceRemote");
        
        Moneda row = dataService.findById(Moneda.class,sessionId ,234L);
        // Necesita del parametro sessionId para acceder a la unidad de persistencia adecuado
        assertTrue(dataService.checkUniqueKey(sessionId, row));
    }

    /** Prueba unique key
     * @throws java.lang.Exception */
    //@Test    
    public void testCheckUniqueKey3() throws Exception {
        System.out.println("DataService - TestCheckUniqueKey3");                
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataService usuarioSrv  = 
                (IDataService) context.lookup(jndiProject+"UsuarioSrv!org.javabeanstack.services.IUsuarioSrvRemote");
        
        List<AppUser> rows = usuarioSrv.find(AppUser.class,null);
        AppUser usuario = rows.get(0);
        usuario.setIduser(0L);
        Map<String, IErrorReg> errors;

        usuario.setAction(IDataRow.MODIFICAR);
        errors = usuarioSrv.checkDataRow("", usuario);
        assertTrue(errors.isEmpty());
        
        usuario.setAction(IDataRow.AGREGAR);        
        errors = usuarioSrv.checkDataRow("", usuario);
        assertFalse(errors.isEmpty());

        usuario.setAction(IDataRow.BORRAR);        
        errors = usuarioSrv.checkDataRow("", usuario);
        assertTrue(errors.isEmpty());
        
        usuario.setLogin("xxxxxxx");
        usuario.setAction(IDataRow.AGREGAR);        
        errors = usuarioSrv.checkDataRow("", usuario);
        assertTrue(errors.isEmpty());
        
     }        
    
    /** Prueba de ejecución del metodo de chequeo de acuerdo al tipo de operación
     * @throws java.lang.Exception
     */
    //@Test    
    public void testCheckData3() throws Exception {
        System.out.println("DataService - TestCheckData3");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataService usuarioSrv  = 
                (IDataService) context.lookup(jndiProject+"RegionSrv!org.javabeanstack.services.IRegionSrvRemote");
        
        List<AppUser> rows = usuarioSrv.findAll(AppUser.class,null);
        rows.get(0).setAction(IDataRow.MODIFICAR);
        usuarioSrv.checkDataRow("", rows.get(0));
        
        rows.get(0).setAction(IDataRow.BORRAR);
        usuarioSrv.checkDataRow("", rows.get(0));
    }        
    
    /** Prueba chequeo de datos
     * @throws java.lang.Exception */
    //@Test    
    public void testCheckData4() throws Exception {
        System.out.println("DataService - TestCheckData4");        
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataService usuarioSrv  = 
                (IDataService) context.lookup(jndiProject+"RegionSrv!org.javabeanstack.services.IRegionSrvRemote");
        
        List<AppUser> rows = usuarioSrv.findAll(AppUser.class,null);
        usuarioSrv.merge("", rows.get(0));
        //dataService.checkDataRow(rows.get(0), "");
       
    }        

    /** Prueba de chequeo de los foreignkeys
     * @throws java.lang.Exception */
    //@Test    
    public void testCheckForeignkey() throws Exception {
        System.out.println("DataService - TestCheckForeignkey");        
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataServiceRemote dataService  = 
                (IDataServiceRemote) context.lookup(jndiProject+"UsuarioMiembroSrv!org.javabeanstack.services.IDataServiceRemote");
        
        List<AppUserMember> rows = dataService.findAll(AppUserMember.class,null);
        assertTrue(dataService.checkForeignKey("",rows.get(0),"usuariogrupo"));
        rows.get(0).setUserGroup(null);        
        assertFalse(dataService.checkForeignKey("", rows.get(0),"usuariogrupo"));        
    }    

    /** Chequeo de los foreignkeys
     * @throws java.lang.Exception */
    //@Test    
    public void testCheckForeignKey2() throws Exception {
        System.out.println("DataService - TestCheckForeignkey2");                
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataServiceRemote dataService  = 
                (IDataServiceRemote) context.lookup(jndiProject+"DataService!org.javabeanstack.services.IDataServiceRemote");
        
        Map<String, IErrorReg> errors;        
        List<Pais> rows = dataService.find(Pais.class,sessionId);
        rows.get(0).setAction(IDataRow.MODIFICAR);
        // Necesita del parametro sessionId para acceder a la unidad de persistencia adecuado        
        errors = dataService.checkDataRow(sessionId, rows.get(0));
        assertTrue(errors.isEmpty());
    }    

    //@Test        
    /** Chequeo de los foreignkeys
     * @throws java.lang.Exception */
    public void testCheckForeignKey3() throws Exception {
        System.out.println("DataService - TestCheckForeignkey3");                
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataServiceRemote dataService  = 
                (IDataServiceRemote) context.lookup(jndiProject+"UsuarioMiembroSrv!org.javabeanstack.services.IDataServiceRemote");
        
        List<AppUserMember> rows = dataService.findAll(AppUserMember.class,null);
        AppUserMember usuarioMiembro = rows.get(0);
        usuarioMiembro.setAction(IDataRow.MODIFICAR);
        
        Map<String, IErrorReg> errors = dataService.checkDataRow(sessionId, rows.get(0));
        assertTrue(errors.isEmpty());
    }
    
    /** Chequeo de los foreignkeys
     * @throws java.lang.Exception */
    //@Test    
    public void testCheckForeignKey4() throws Exception {
        System.out.println("DataService - TestCheckForeignkey4");                
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataServiceRemote dataService  = 
                (IDataServiceRemote) context.lookup(jndiProject+"PaisSrv!org.javabeanstack.services.IDataServiceRemote");
        
        List<Pais> rows = dataService.find(Pais.class, sessionId);
        Pais pais = rows.get(0);
        //pais.setRegion(null);
        pais.setAction(IDataRow.MODIFICAR);
        // Necesita del parametro sessionId para acceder a la unidad de persistencia adecuado                
        Map<String, IErrorReg> errors = dataService.checkDataRow(sessionId, pais);
        assertTrue(errors.isEmpty());
    }    
    
    /**
     * Test of getPersistentUnit method, of class AbstractDataService.
     */
    //@Test
    public void testGetPersistentUnit() {
        System.out.println("getPersistentUnit");
        String sessionId = "";
        AbstractDataService instance = new AbstractDataServiceImpl();
        String expResult = "";
        String result = instance.getPersistentUnit(sessionId);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getUserSession method, of class AbstractDataService.
     */
    //@Test
    public void testGetUserSession() {
        System.out.println("getUserSession");
        String sessionId = "";
        AbstractDataService instance = new AbstractDataServiceImpl();
        IUserSession expResult = null;
        IUserSession result = instance.getUserSession(sessionId);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getEntityManagerProp method, of class AbstractDataService.
     */
    //@Test
    public void testGetEntityManagerProp() {
        System.out.println("getEntityManagerProp");
        String persistUnit = "";
        AbstractDataService instance = new AbstractDataServiceImpl();
        Map<String, Object> expResult = null;
        Map<String, Object> result = instance.getEntityManagerProp(persistUnit);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getPersistUnitProp method, of class AbstractDataService.
     */
    //@Test
    public void testGetPersistUnitProp() {
        System.out.println("getPersistUnitProp");
        String persistUnit = "";
        AbstractDataService instance = new AbstractDataServiceImpl();
        Map<String, Object> expResult = null;
        Map<String, Object> result = instance.getPersistUnitProp(persistUnit);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getDataEngine method, of class AbstractDataService.
     */
    //@Test
    public void testGetDataEngine() {
        System.out.println("getDataEngine");
        String persistentUnit = "";
        AbstractDataService instance = new AbstractDataServiceImpl();
        String expResult = "";
        String result = instance.getDataEngine(persistentUnit);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSchema method, of class AbstractDataService.
     */
    //@Test
    public void testGetSchema() {
        System.out.println("getSchema");
        String persistentUnit = "";
        AbstractDataService instance = new AbstractDataServiceImpl();
        String expResult = "";
        String result = instance.getSchema(persistentUnit);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setListCheckMethods method, of class AbstractDataService.
     */
    //@Test
    public void testSetListCheckMethods() {
        System.out.println("setListCheckMethods");
        AbstractDataService instance = new AbstractDataServiceImpl();
        List<Method> expResult = null;
        List<Method> result = instance.setListCheckMethods();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setListFieldCheck method, of class AbstractDataService.
     */
    //@Test
    public void testSetListFieldCheck() {
        System.out.println("setListFieldCheck");
        Object row = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        Object expResult = null;
    }

    /**
     * Test of findById method, of class AbstractDataService.
     */
    //@Test
    public void testFindById() throws Exception {
        System.out.println("findById");
        AbstractDataService instance = new AbstractDataServiceImpl();
        Object expResult = null;
    }

    /**
     * Test of findByUk method, of class AbstractDataService.
     */
    //@Test
    public void testFindByUk() throws Exception {
        System.out.println("findByUk");
        String sessionId = "";
        Object ejb = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        Object expResult = null;
    }

    /**
     * Test of find method, of class AbstractDataService.
     */
    //@Test
    public void testFind_Class_String() throws Exception {
        System.out.println("find");
        AbstractDataService instance = new AbstractDataServiceImpl();
        List expResult = null;
    }

    /**
     * Test of find method, of class AbstractDataService.
     */
    //@Test
    public void testFind_5args() throws Exception {
        System.out.println("find");
        AbstractDataService instance = new AbstractDataServiceImpl();
        List expResult = null;
    }

    /**
     * Test of find method, of class AbstractDataService.
     */
    //@Test
    public void testFind_7args() throws Exception {
        System.out.println("find");
        AbstractDataService instance = new AbstractDataServiceImpl();
        List expResult = null;
    }

    /**
     * Test of findByNativeQuery method, of class AbstractDataService.
     */
    //@Test
    public void testFindByNativeQuery_3args() throws Exception {
        System.out.println("findByNativeQuery");
        String sessionId = "";
        String queryString = "";
        Map<String, Object> parameters = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        List<Object> expResult = null;
        List<Object> result = instance.findByNativeQuery(sessionId, queryString, parameters);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findByNativeQuery method, of class AbstractDataService.
     */
    //@Test
    public void testFindByNativeQuery_5args() throws Exception {
        System.out.println("findByNativeQuery");
        String sessionId = "";
        String queryString = "";
        Map<String, Object> parameters = null;
        int first = 0;
        int max = 0;
        AbstractDataService instance = new AbstractDataServiceImpl();
        List<Object> expResult = null;
        List<Object> result = instance.findByNativeQuery(sessionId, queryString, parameters, first, max);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findByQuery method, of class AbstractDataService.
     */
    //@Test
    public void testFindByQuery() throws Exception {
        System.out.println("findByQuery");
        String sessionId = "";
        String queryString = "";
        Map<String, Object> parameters = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        Object expResult = null;
        Object result = instance.findByQuery(sessionId, queryString, parameters);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findListByQuery method, of class AbstractDataService.
     */
    //@Test
    public void testFindListByQuery_3args() throws Exception {
        System.out.println("findListByQuery");
        String sessionId = "";
        String queryString = "";
        Map<String, Object> parameters = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        List expResult = null;
        List result = instance.findListByQuery(sessionId, queryString, parameters);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findListByQuery method, of class AbstractDataService.
     */
    //@Test
    public void testFindListByQuery_4args() throws Exception {
        System.out.println("findListByQuery");
        String sessionId = "";
        String queryString = "";
        int first = 0;
        int max = 0;
        AbstractDataService instance = new AbstractDataServiceImpl();
        List expResult = null;
        List result = instance.findListByQuery(sessionId, queryString, first, max);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findListByQuery method, of class AbstractDataService.
     */
    //@Test
    public void testFindListByQuery_5args() throws Exception {
        System.out.println("findListByQuery");
        String sessionId = "";
        String queryString = "";
        Map<String, Object> parameters = null;
        int first = 0;
        int max = 0;
        AbstractDataService instance = new AbstractDataServiceImpl();
        List expResult = null;
        List result = instance.findListByQuery(sessionId, queryString, parameters, first, max);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findByNamedQuery method, of class AbstractDataService.
     */
    //@Test
    public void testFindByNamedQuery() throws Exception {
        System.out.println("findByNamedQuery");
        String sessionId = "";
        String namedQuery = "";
        Map<String, Object> parameters = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        Object expResult = null;
        Object result = instance.findByNamedQuery(sessionId, namedQuery, parameters);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findListByNamedQuery method, of class AbstractDataService.
     */
    //@Test
    public void testFindListByNamedQuery_3args() throws Exception {
        System.out.println("findListByNamedQuery");
        String sessionId = "";
        String namedQuery = "";
        Map<String, Object> parameters = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        List expResult = null;
        List result = instance.findListByNamedQuery(sessionId, namedQuery, parameters);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findListByNamedQuery method, of class AbstractDataService.
     */
    //@Test
    public void testFindListByNamedQuery_4args() throws Exception {
        System.out.println("findListByNamedQuery");
        String sessionId = "";
        String namedQuery = "";
        int first = 0;
        int max = 0;
        AbstractDataService instance = new AbstractDataServiceImpl();
        List expResult = null;
        List result = instance.findListByNamedQuery(sessionId, namedQuery, first, max);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findListByNamedQuery method, of class AbstractDataService.
     */
    //@Test
    public void testFindListByNamedQuery_5args() throws Exception {
        System.out.println("findListByNamedQuery");
        String sessionId = "";
        String namedQuery = "";
        Map<String, Object> parameters = null;
        int first = 0;
        int max = 0;
        AbstractDataService instance = new AbstractDataServiceImpl();
        List expResult = null;
        List result = instance.findListByNamedQuery(sessionId, namedQuery, parameters, first, max);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of refreshRow method, of class AbstractDataService.
     */
    //@Test
    public void testRefreshRow() throws Exception {
        System.out.println("refreshRow");
        String sessionId = "";
        Object row = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        Object expResult = null;
    }

    /**
     * Test of getCount method, of class AbstractDataService.
     */
    //@Test
    public void testGetCount() throws Exception {
        System.out.println("getCount");
        String sessionId = "";
        String queryString = "";
        Map<String, Object> parameters = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        Long expResult = null;
        Long result = instance.getCount(sessionId, queryString, parameters);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCount2 method, of class AbstractDataService.
     */
    //@Test
    public void testGetCount2() throws Exception {
        System.out.println("getCount2");
        String sessionId = "";
        String queryString = "";
        Map<String, Object> parameters = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        Long expResult = null;
        Long result = instance.getCount2(sessionId, queryString, parameters);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of checkUserSession method, of class AbstractDataService.
     */
    //@Test
    public void testCheckUserSession() throws Exception {
        System.out.println("checkUserSession");
        String sessionId = "";
        AbstractDataService instance = new AbstractDataServiceImpl();
        instance.checkUserSession(sessionId);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of checkUniqueKey method, of class AbstractDataService.
     */
    //@Test
    public void testCheckUniqueKey() {
        System.out.println("checkUniqueKey");
        String sessionId = "";
        Object row = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        boolean expResult = false;
    }

    /**
     * Test of checkForeignKey method, of class AbstractDataService.
     */
    //@Test
    public void testCheckForeignKey() {
        System.out.println("checkForeignKey");
        String sessionId = "";
        Object row = null;
        String fieldName = "";
        AbstractDataService instance = new AbstractDataServiceImpl();
        boolean expResult = false;
    }

    /**
     * Test of checkDataRow method, of class AbstractDataService.
     */
    //@Test
    public void testCheckDataRow() {
        System.out.println("checkDataRow");
        String sessionId = "";
        Object row = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        Map<String, IErrorReg> expResult = null;
    }

    /**
     * Test of checkDataResult method, of class AbstractDataService.
     */
    //@Test
    public void testCheckDataResult() {
        System.out.println("checkDataResult");
        String sessionId = "";
        Object row = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        IDataResult expResult = null;
    }

    /**
     * Test of save method, of class AbstractDataService.
     */
    //@Test
    public void testSave() throws Exception {
        System.out.println("save");
        String sessionId = "";
        Object row = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        IDataResult expResult = null;
    }

    /**
     * Test of persist method, of class AbstractDataService.
     */
    //@Test
    public void testPersist() throws Exception {
        System.out.println("persist");
        String sessionId = "";
        Object row = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        IDataResult expResult = null;
    }

    /**
     * Test of merge method, of class AbstractDataService.
     */
    //@Test
    public void testMerge() throws Exception {
        System.out.println("merge");
        String sessionId = "";
        Object row = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        IDataResult expResult = null;
    }

    /**
     * Test of remove method, of class AbstractDataService.
     */
    //@Test
    public void testRemove() throws Exception {
        System.out.println("remove");
        String sessionId = "";
        Object row = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        IDataResult expResult = null;
    }

    /**
     * Test of getDataRows method, of class AbstractDataService.
     */
    //@Test
    public void testGetDataRows() throws Exception {
        System.out.println("getDataRows");
        AbstractDataService instance = new AbstractDataServiceImpl();
        List expResult = null;
    }

    /**
     * Test of getSelectCmd method, of class AbstractDataService.
     */
    //@Test
    public void testGetSelectCmd() {
        System.out.println("getSelectCmd");
        AbstractDataService instance = new AbstractDataServiceImpl();
        String expResult = "";
    }

    /**
     * Test of update method, of class AbstractDataService.
     */
    //@Test
    public void testUpdate_String_GenericType() {
        System.out.println("update");
        String sessionId = "";
        Object ejb = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        IDataResult expResult = null;
    }

    /**
     * Test of update method, of class AbstractDataService.
     */
    //@Test
    public void testUpdate_String_IDataObject() {
        System.out.println("update");
        String sessionId = "";
        IDataObject ejbs = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        IDataResult expResult = null;
        IDataResult result = instance.update(sessionId, ejbs);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of update method, of class AbstractDataService.
     */
    //@Test
    public void testUpdate_String_List() {
        System.out.println("update");
        AbstractDataService instance = new AbstractDataServiceImpl();
        IDataResult expResult = null;
    }

    /**
     * Test of update method, of class AbstractDataService.
     */
    //@Test
    public void testUpdate_String_IDataSet() {
        System.out.println("update");
        String sessionId = "";
        IDataSet dataSet = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        IDataResult expResult = null;
        IDataResult result = instance.update(sessionId, dataSet);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getConnection method, of class AbstractDataService.
     */
    //@Test
    public void testGetConnection_String() {
        System.out.println("getConnection");
        String sessionId = "";
        AbstractDataService instance = new AbstractDataServiceImpl();
        Connection expResult = null;
        Connection result = instance.getConnection(sessionId);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getConnection method, of class AbstractDataService.
     */
    //@Test
    public void testGetConnection_String_IDBConnectFactory() {
        System.out.println("getConnection");
        String sessionId = "";
        IDBConnectFactory conn = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        Connection expResult = null;
        Connection result = instance.getConnection(sessionId, conn);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findAll method, of class AbstractDataService.
     */
    //@Test
    public void testFindAll() throws Exception {
        System.out.println("findAll");
        AbstractDataService instance = new AbstractDataServiceImpl();
        List expResult = null;
    }

    /**
     * Test of sqlExec method, of class AbstractDataService.
     */
    //@Test
    public void testSqlExec() throws Exception {
        System.out.println("sqlExec");
        String sessionId = "";
        String queryString = "";
        Map<String, Object> parameters = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        IErrorReg expResult = null;
        IErrorReg result = instance.sqlExec(sessionId, queryString, parameters);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getData method, of class AbstractDataService.
     */
    //@Test
    public void testGetData_4args() throws Exception {
        System.out.println("getData");
        String sessionId = "";
        String queryString = "";
        int maxRows = 0;
        boolean noCache = false;
        AbstractDataService instance = new AbstractDataServiceImpl();
        List expResult = null;
        List result = instance.getData(sessionId, queryString, maxRows, noCache);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getData method, of class AbstractDataService.
     */
    //@Test
    public void testGetData_Query() throws Exception {
        System.out.println("getData");
        Query query = null;
        AbstractDataService instance = new AbstractDataServiceImpl();
        List expResult = null;
        List result = instance.getData(query);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of refreshAll method, of class AbstractDataService.
     */
    //@Test
    public void testRefreshAll() throws Exception {
        System.out.println("refreshAll");
        AbstractDataService instance = new AbstractDataServiceImpl();
        List expResult = null;
    }

    public class AbstractDataServiceImpl extends AbstractDataService {
    }
    
}

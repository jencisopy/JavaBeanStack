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
import java.util.Map;
import org.javabeanstack.security.IUserSession;
import org.javabeanstack.services.IDataService;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jorge Enciso
 */
public class AbstractDataLinkTest {
    
    public AbstractDataLinkTest() {
    }

    /**
     * Test of getDao method, of class AbstractDataLink.
     */
    //@Test
    public void testGetDao() {
        System.out.println("getDao");
        AbstractDataLink instance = new AbstractDataLinkImpl();
        IGenericDAO expResult = null;
        IGenericDAO result = instance.getDao();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setDao method, of class AbstractDataLink.
     */
    //@Test
    public void testSetDao(){
        System.out.println("setDao");
        Object dao = null;
        AbstractDataLink instance = new AbstractDataLinkImpl();
        //instance.setDao(dao);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getDataService method, of class AbstractDataLink.
     */
    //@Test
    public void testGetDataService() {
        System.out.println("getDataService");
        AbstractDataLink instance = new AbstractDataLinkImpl();
        Object expResult = null;
        Object result = instance.getDataService();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getPersistUnit method, of class AbstractDataLink.
     */
    //@Test
    public void testGetPersistUnit() {
        System.out.println("getPersistUnit");
        AbstractDataLink instance = new AbstractDataLinkImpl();
        String expResult = "";
        String result = instance.getPersistUnit();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of persist method, of class AbstractDataLink.
     */
    //@Test
    public void testPersist_GenericType() throws Exception {
        System.out.println("persist");
        Object ejb = null;
        AbstractDataLink instance = new AbstractDataLinkImpl();
        IDataResult expResult = null;
        //IDataResult result = instance.persist(ejb);
        //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of persist method, of class AbstractDataLink.
     */
    //@Test
    public void testPersist_List() throws Exception {
        System.out.println("persist");
        AbstractDataLink instance = new AbstractDataLinkImpl();
        IDataResult expResult = null;
        //IDataResult result = instance.persist(null);
        //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of merge method, of class AbstractDataLink.
     */
    //@Test
    public void testMerge_GenericType() throws Exception {
        System.out.println("merge");
        Object ejb = null;
        AbstractDataLink instance = new AbstractDataLinkImpl();
        IDataResult expResult = null;
        //IDataResult result = instance.merge(ejb);
        //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of merge method, of class AbstractDataLink.
     */
    //@Test
    public void testMerge_List() throws Exception {
        System.out.println("merge");
        AbstractDataLink instance = new AbstractDataLinkImpl();
        //IDataResult expResult = null;
        //IDataResult result = instance.merge(null);
        //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of remove method, of class AbstractDataLink.
     */
    //@Test
    public void testRemove_GenericType() throws Exception {
        System.out.println("remove");
        Object ejb = null;
        AbstractDataLink instance = new AbstractDataLinkImpl();
        IDataResult expResult = null;
        //IDataResult result = instance.remove(ejb);
        //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of remove method, of class AbstractDataLink.
     */
    //@Test
    public void testRemove_List() throws Exception {
        System.out.println("remove");
        AbstractDataLink instance = new AbstractDataLinkImpl();
        IDataResult expResult = null;
        //IDataResult result = instance.remove(null);
        //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of update method, of class AbstractDataLink.
     */
    //@Test
    public void testUpdate_GenericType() throws Exception {
        System.out.println("update");
        Object ejb = null;
        AbstractDataLink instance = new AbstractDataLinkImpl();
        IDataResult expResult = null;
        //IDataResult result = instance.update(ejb);
        //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of update method, of class AbstractDataLink.
     */
    //@Test
    public void testUpdate_List() throws Exception {
        System.out.println("update");
        AbstractDataLink instance = new AbstractDataLinkImpl();
        IDataResult expResult = null;
//        IDataResult result = instance.update(null);
//        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of update method, of class AbstractDataLink.
     */
    //@Test
    public void testUpdate_IDataSet() throws Exception {
        System.out.println("update");
        IDataSet dataSet = null;
        AbstractDataLink instance = new AbstractDataLinkImpl();
        IDataResult expResult = null;
        IDataResult result = instance.update(dataSet);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of find method, of class AbstractDataLink.
     */
    //@Test
    public void testFind() throws Exception {
        System.out.println("find");
        AbstractDataLink instance = new AbstractDataLinkImpl();
        Object expResult = null;
//        Object result = instance.find(null);
//        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findByUk method, of class AbstractDataLink.
     */
    //@Test
    public void testFindByUk() throws Exception {
        System.out.println("findByUk");
        Object ejb = null;
        AbstractDataLink instance = new AbstractDataLinkImpl();
        Object expResult = null;
//        Object result = instance.findByUk(ejb);
//        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findByQuery method, of class AbstractDataLink.
     */
    //@Test
    public void testFindByQuery() throws Exception {
        System.out.println("findByQuery");
        String queryString = "";
        Map<String, Object> parameters = null;
        AbstractDataLink instance = new AbstractDataLinkImpl();
        Object expResult = null;
        Object result = instance.findByQuery(queryString, parameters);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findListByQuery method, of class AbstractDataLink.
     */
    //@Test
    public void testFindListByQuery_String_Map() throws Exception {
        System.out.println("findListByQuery");
        String queryString = "";
        Map<String, Object> parameters = null;
        AbstractDataLink instance = new AbstractDataLinkImpl();
        List expResult = null;
        List result = instance.findListByQuery(queryString, parameters);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findListByQuery method, of class AbstractDataLink.
     */
    //@Test
    public void testFindListByQuery_4args() throws Exception {
        System.out.println("findListByQuery");
        String queryString = "";
        Map<String, Object> parameters = null;
        int first = 0;
        int max = 0;
        AbstractDataLink instance = new AbstractDataLinkImpl();
        List expResult = null;
        List result = instance.findListByQuery(queryString, parameters, first, max);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findByNamedQuery method, of class AbstractDataLink.
     */
    //@Test
    public void testFindByNamedQuery() throws Exception {
        System.out.println("findByNamedQuery");
        String namedQuery = "";
        Map<String, Object> parameters = null;
        AbstractDataLink instance = new AbstractDataLinkImpl();
        Object expResult = null;
        Object result = instance.findByNamedQuery(namedQuery, parameters);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findListByNamedQuery method, of class AbstractDataLink.
     */
    //@Test
    public void testFindListByNamedQuery_String_Map() throws Exception {
        System.out.println("findListByNamedQuery");
        String namedQuery = "";
        Map<String, Object> parameters = null;
        AbstractDataLink instance = new AbstractDataLinkImpl();
        List expResult = null;
        List result = instance.findListByNamedQuery(namedQuery, parameters);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findListByNamedQuery method, of class AbstractDataLink.
     */
    //@Test
    public void testFindListByNamedQuery_4args() throws Exception {
        System.out.println("findListByNamedQuery");
        String namedQuery = "";
        Map<String, Object> parameters = null;
        int first = 0;
        int max = 0;
        AbstractDataLink instance = new AbstractDataLinkImpl();
        List expResult = null;
        List result = instance.findListByNamedQuery(namedQuery, parameters, first, max);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findByNativeQuery method, of class AbstractDataLink.
     */
    //@Test
    public void testFindByNativeQuery_String_Map() throws Exception {
        System.out.println("findByNativeQuery");
        String queryString = "";
        Map<String, Object> parameters = null;
        AbstractDataLink instance = new AbstractDataLinkImpl();
        List<Object> expResult = null;
        List<Object> result = instance.findByNativeQuery(queryString, parameters);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findByNativeQuery method, of class AbstractDataLink.
     */
    //@Test
    public void testFindByNativeQuery_4args() throws Exception {
        System.out.println("findByNativeQuery");
        String queryString = "";
        Map<String, Object> parameters = null;
        int first = 0;
        int max = 0;
        AbstractDataLink instance = new AbstractDataLinkImpl();
        List<Object> expResult = null;
        List<Object> result = instance.findByNativeQuery(queryString, parameters, first, max);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of refreshRow method, of class AbstractDataLink.
     */
    //@Test
    public void testRefreshRow() throws Exception {
        System.out.println("refreshRow");
        Object row = null;
        AbstractDataLink instance = new AbstractDataLinkImpl();
        Object expResult = null;
//        Object result = instance.refreshRow(row);
//        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCount method, of class AbstractDataLink.
     */
    //@Test
    public void testGetCount() throws Exception {
        System.out.println("getCount");
        String queryString = "";
        Map<String, Object> parameters = null;
        AbstractDataLink instance = new AbstractDataLinkImpl();
        Long expResult = null;
        Long result = instance.getCount(queryString, parameters);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCount2 method, of class AbstractDataLink.
     */
    //@Test
    public void testGetCount2() throws Exception {
        System.out.println("getCount2");
        String queryString = "";
        Map<String, Object> parameters = null;
        AbstractDataLink instance = new AbstractDataLinkImpl();
        Long expResult = null;
        Long result = instance.getCount2(queryString, parameters);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getEntityManagerProp method, of class AbstractDataLink.
     */
    //@Test
    public void testGetEntityManagerProp() {
        System.out.println("getEntityManagerProp");
        AbstractDataLink instance = new AbstractDataLinkImpl();
        Map<String, Object> expResult = null;
        Map<String, Object> result = instance.getEntityManagerProp();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getPersistUnitProp method, of class AbstractDataLink.
     */
    //@Test
    public void testGetPersistUnitProp() {
        System.out.println("getPersistUnitProp");
        AbstractDataLink instance = new AbstractDataLinkImpl();
        Map<String, Object> expResult = null;
        Map<String, Object> result = instance.getPersistUnitProp();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getUserSession method, of class AbstractDataLink.
     */
    //@Test
    public void testGetUserSession() {
        System.out.println("getUserSession");
        AbstractDataLink instance = new AbstractDataLinkImpl();
        IUserSession expResult = null;
        IUserSession result = instance.getUserSession();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setUserSession method, of class AbstractDataLink.
     */
    //@Test
    public void testSetUserSession() throws Exception {
        System.out.println("setUserSession");
        IUserSession userSession = null;
        AbstractDataLink instance = new AbstractDataLinkImpl();
        instance.setUserSession(userSession);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of newDataNativeQuery method, of class AbstractDataLink.
     */
    //@Test
    public void testNewDataNativeQuery() {
        System.out.println("newDataNativeQuery");
        AbstractDataLink instance = new AbstractDataLinkImpl();
        IDataNativeQuery expResult = null;
        IDataNativeQuery result = instance.newDataNativeQuery();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getEntitiesRelation method, of class AbstractDataLink.
     */
    //@Test
    public void testGetEntitiesRelation() throws Exception {
        System.out.println("getEntitiesRelation");
        String entities = "";
        String typeRela = "";
        String schema = "";
        AbstractDataLink instance = new AbstractDataLinkImpl();
        String expResult = "";
        String result = instance.getEntitiesRelation(entities, typeRela, schema);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getDBLinkInfo method, of class AbstractDataLink.
     */
    //@Test
    public void testGetDBLinkInfo() {
        System.out.println("getDBLinkInfo");
        AbstractDataLink instance = new AbstractDataLinkImpl();
        IDBLinkInfo expResult = null;
        IDBLinkInfo result = instance.getDBLinkInfo();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSessionId method, of class AbstractDataLink.
     */
    //@Test
    public void testGetSessionId() {
        System.out.println("getSessionId");
        AbstractDataLink instance = new AbstractDataLinkImpl();
        String expResult = "";
        String result = instance.getSessionId();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    public class AbstractDataLinkImpl extends AbstractDataLink {

        public IGenericDAO getDao() {
            return null;
        }

        public <T extends IGenericDAO> void setDao(T dao) {
        }

        public <T extends IDataService> T getDataService() {
            return null;
        }
    }
    
}

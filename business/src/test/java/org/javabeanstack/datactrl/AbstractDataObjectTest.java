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
package org.javabeanstack.datactrl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.naming.NamingException;
import org.javabeanstack.data.model.DataSet;
import org.javabeanstack.data.IDataLink;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.data.IDataSet;
import org.javabeanstack.data.TestClass;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.events.IDataEvents;
import org.javabeanstack.exceptions.SessionError;
import org.javabeanstack.model.tables.AppUserFormView;
import org.javabeanstack.model.tables.AppUserFormViewColumn;
import org.javabeanstack.model.tables.Pais;
import org.javabeanstack.model.tables.Region;
import org.javabeanstack.services.IDataService;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 *
 * @author Jorge Enciso
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AbstractDataObjectTest extends TestClass{
    
    public AbstractDataObjectTest() {
    }


    @Test
    public void test1AddData() throws NamingException, SessionError, Exception {
        System.out.println("1-DataObject AddData");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        //Region
        IDataObject region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        region.insertRow();
        region.setField("codigo", "ZZZ");
        region.setField("nombre", "ZZZ BORRAR");

        IDataSet dataSet = new DataSet();
        dataSet.addDataObject("region", region);

        boolean result = dataLink.update(dataSet).isSuccessFul();
        assertTrue(result);
    }

    @Test
    public void test2BorrarData() throws NamingException, SessionError, Exception {
        System.out.println("2-DataObject BorrarData");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        boolean result;
        //Campo        
        IDataObject campo = new DataObject(Region.class, null, dataLink, null);
        campo.open();
        if (campo.find("codigo", "ZZZ")){
            campo.deleteRow();
            result = campo.update(false);
            if (!result) {
                System.out.println(campo.getErrorMsg(true));
            }
            assertTrue(result);
        }
    }

    @Test
    public void test3AddData() throws NamingException, SessionError, Exception {
        System.out.println("3-DataObject AddData (dataset)");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        //Region
        IDataObject region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        region.insertRow();
        region.setField("codigo", "ZZZ");
        region.setField("nombre", "ZZZ BORRAR");

        //pais
        IDataObject pais = new DataObject(Pais.class, null, dataLink, null);
        pais.open();
        pais.insertRow();
        pais.setField("codigo", "ZZZ");
        pais.setField("nombre", "ZZZ PAIS BORRAR");
        pais.setField("region", region.getRow());

        IDataSet dataSet = new DataSet();
        dataSet.addDataObject("region", region);
        dataSet.addDataObject("pais", pais);

        boolean result = dataLink.update(dataSet).isSuccessFul();
        assertTrue(result);
    }

    @Test
    public void test4BorrarData() throws NamingException, SessionError, Exception {
        System.out.println("4-DataObject BorrarData");
        boolean result;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        //Pais
        IDataObject pais = new DataObject(Pais.class, null, dataLink, null);
        pais.open();
        if (pais.find("codigo", "ZZZ")) {
            pais.deleteRow();
            result = pais.update(false);
            assertTrue(result);
        }

        //Region        
        IDataObject campo = new DataObject(Region.class, null, dataLink, null);
        campo.open();
        if (campo.find("codigo", "ZZZ")){
            campo.deleteRow();
            result = campo.update(false);
            if (!result) {
                System.out.println(campo.getErrorMsg(true));
            }
            assertTrue(result);            
        }
    }

    @Test
    public void test5DBFilter() throws NamingException, SessionError, Exception {
        System.out.println("5-DataObject DBFilter");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        //Region
        IDataObject region = new DataObject(Region.class, null, dataLink, null);
        String filter = region.getDAO()
                .getUserSession()
                .getDBFilter()
                .getFilterExpr(region.getType(), "");
        System.out.println(filter);
        region.open();
        assertTrue(filter.contains("idempresa"));
    }


    @Test
    public void test7getData() throws NamingException, SessionError, Exception {
        System.out.println("7-DataObject getData");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataService dataservice
                = (IDataService) context.lookup(jndiProject + "DataService!org.javabeanstack.services.IDataServiceRemote");

        //Region
        dataLink.setDao(dataservice);
        IDataObject region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        Assert.assertNotNull(region.getDataRows());

        region.setOrder("codigo desc");
        region.setFilter("codigo = '01'");
        region.requery();
        Assert.assertNotNull(region.getDataRows());
    }

    //@Test
    public void test8FormView() throws NamingException, SessionError, Exception {
        System.out.println("8-DataObject");        
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataEvents dataEvent = null;
        IDataObject<AppUserFormView> formViews
                = new DataObject(AppUserFormView.class, dataEvent, dataLinkCat, null);

        String formName = "bx_campo.xhtml";
        Map<String, Object> params = new HashMap<>();
        params.put("form", formName);

        // Buscar la lista de columnas del usuario activo del formulario actual, 
        // si no existe buscar lista de columnas en forma generica
        String filter = "form = :form";
        String order = "viewName";
        formViews.setFilterParams(params);
        formViews.open(order, filter, true, -1);

        if (!formViews.isEof()) {
            if (formViews.find("viewname", "DEFAULT")) {
                formViews.deleteRow();
                if (!formViews.update(false)) {
                    System.out.println(formViews.getErrorMsg(true));
                }
            }
        }
        formViews.insertRow();
        formViews.setField("form", formName);
        formViews.setField("viewname", "DEFAULT");
        formViews.setField("iduser", 0L);
        AppUserFormViewColumn detail;

        detail = new AppUserFormViewColumn();
        detail.setIdorder(0);
        detail.setColumnName("nombre");
        detail.setColumnHeader("Nombre");
        detail.setVisible(true);
        detail.setLink("");
        detail.setAppUserFormView(formViews.getRow());
        formViews.getRow().getChild().add(detail);

        detail = new AppUserFormViewColumn();
        detail.setIdorder(1);
        detail.setColumnName("codigo");
        detail.setColumnHeader("Código");
        detail.setVisible(true);
        detail.setLink("");
        detail.setAppUserFormView(formViews.getRow());
        formViews.getRow().getChild().add(detail);

        if (!formViews.update(false)) {
            System.out.println(formViews.getErrorMsg(true));
        }
    }
    
    //@Test
    public void test9FormView() throws NamingException, SessionError, Exception {
        System.out.println("9-DataObject");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<AppUserFormView> formViews
                = new DataObject(AppUserFormView.class, null, dataLinkCat, null);

        String formName = "bx_campo.xhtml";
        Map<String, Object> params = new HashMap<>();
        params.put("form", formName);

        // Buscar la lista de columnas del usuario activo del formulario actual, 
        // si no existe buscar lista de columnas en forma generica
        String filter = "form = :form";
        String order = "viewName";
        formViews.setFilterParams(params);
        formViews.open(order, filter, true, -1);

        if (!formViews.isEof()) {
            if (formViews.find("viewname", "DEFAULT")) {
                formViews.setField("filtertext","xx");
                formViews.getRow().getChild().get(0).setLink("prueba");
                if (!formViews.update(false)) {
                    System.out.println(formViews.getErrorMsg(true));
                }
            }
        }
    }
    
    /**
     * Test of getErrorApp method, of class AbstractDataObject.
     */
    //@Test
    public void testGetErrorApp() {
        System.out.println("getErrorApp");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        Exception expResult = null;
        Exception result = instance.getErrorApp();
        assertEquals(expResult, result);
    }

    /**
     * Test of getErrorMsg method, of class AbstractDataObject.
     */
    //@Test
    public void testGetErrorMsg_boolean() {
        System.out.println("getErrorMsg");
        boolean all = false;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        String expResult = "";
        String result = instance.getErrorMsg(all);
        assertEquals(expResult, result);
    }

    /**
     * Test of getErrorMsg method, of class AbstractDataObject.
     */
    //@Test
    public void testGetErrorMsg_String() {
        System.out.println("getErrorMsg");
        String fieldName = "";
        AbstractDataObject instance = new AbstractDataObjectImpl();
        String expResult = "";
        String result = instance.getErrorMsg(fieldName);
        assertEquals(expResult, result);
    }

    /**
     * Test of getFilter method, of class AbstractDataObject.
     */
    //@Test
    public void testGetFilter() {
        System.out.println("getFilter");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        String expResult = "";
        String result = instance.getFilter();
        assertEquals(expResult, result);
    }

    /**
     * Test of getOrder method, of class AbstractDataObject.
     */
    //@Test
    public void testGetOrder() {
        System.out.println("getOrder");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        String expResult = "";
        String result = instance.getOrder();
        assertEquals(expResult, result);
    }

    /**
     * Test of getFilterExtra method, of class AbstractDataObject.
     */
    //@Test
    public void testGetFilterExtra() {
        System.out.println("getFilterExtra");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        String expResult = "";
        String result = instance.getFilterExtra();
        assertEquals(expResult, result);
    }

    /**
     * Test of getFilterParams method, of class AbstractDataObject.
     */
    //@Test
    public void testGetFilterParams() {
        System.out.println("getFilterParams");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        Map<String, Object> expResult = null;
        Map<String, Object> result = instance.getFilterParams();
        assertEquals(expResult, result);
    }

    /**
     * Test of getDataRows method, of class AbstractDataObject.
     */
    //@Test
    public void testGetDataRows() {
        System.out.println("getDataRows");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        List expResult = null;
        List result = instance.getDataRows();
        assertEquals(expResult, result);
    }

    /**
     * Test of getDataRowsChanged method, of class AbstractDataObject.
     */
    //@Test
    public void testGetDataRowsChanged() {
        System.out.println("getDataRowsChanged");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        Map expResult = null;
        Map result = instance.getDataRowsChanged();
        assertEquals(expResult, result);
    }


    /**
     * Test of getSelectCmd method, of class AbstractDataObject.
     */
    //@Test
    public void testGetSelectCmd() {
        System.out.println("getSelectCmd");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        String expResult = "";
        String result = instance.getSelectCmd();
        assertEquals(expResult, result);
    }

    /**
     * Test of getLastQuery method, of class AbstractDataObject.
     */
    //@Test
    public void testGetLastQuery() {
        System.out.println("getLastQuery");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        String expResult = "";
        String result = instance.getLastQuery();
        assertEquals(expResult, result);
    }


    /**
     * Test of getRowCount method, of class AbstractDataObject.
     */
    //@Test
    public void testGetRowCount() {
        System.out.println("getRowCount");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        int expResult = 0;
        int result = instance.getRowCount();
        assertEquals(expResult, result);
    }

    /**
     * Test of getRecStatus method, of class AbstractDataObject.
     */
    //@Test
    public void testGetRecStatus() {
        System.out.println("getRecStatus");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        int expResult = 0;
        int result = instance.getRecStatus();
        assertEquals(expResult, result);
    }

    /**
     * Test of getFirstRow method, of class AbstractDataObject.
     */
    //@Test
    public void testGetFirstRow() {
        System.out.println("getFirstRow");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        int expResult = 0;
        int result = instance.getFirstRow();
        assertEquals(expResult, result);
    }

    /**
     * Test of getMaxRows method, of class AbstractDataObject.
     */
    //@Test
    public void testGetMaxRows() {
        System.out.println("getMaxRows");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        int expResult = 0;
        int result = instance.getMaxRows();
        assertEquals(expResult, result);
    }

    /**
     * Test of getIdcompany method, of class AbstractDataObject.
     */
    //@Test
    public void testGetIdcompany() {
        System.out.println("getIdcompany");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        Long expResult = null;
        Long result = instance.getIdcompany();
        assertEquals(expResult, result);
    }

    /**
     * Test of getIdempresa method, of class AbstractDataObject.
     */
    //@Test
    public void testGetIdempresa() {
        System.out.println("getIdempresa");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        Long expResult = null;
        Long result = instance.getIdempresa();
        assertEquals(expResult, result);
    }


    /**
     * Test of setFirstRow method, of class AbstractDataObject.
     */
    //@Test
    public void testSetFirstRow() {
        System.out.println("setFirstRow");
        int first = 0;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.setFirstRow(first);
    }

    /**
     * Test of setMaxRows method, of class AbstractDataObject.
     */
    //@Test
    public void testSetMaxRows() {
        System.out.println("setMaxRows");
        int maxRows = 0;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.setMaxRows(maxRows);
    }

    /**
     * Test of setSelectcmd method, of class AbstractDataObject.
     */
    //@Test
    public void testSetSelectcmd() {
        System.out.println("setSelectcmd");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.setSelectcmd();
    }


    /**
     * Test of beforeOpen method, of class AbstractDataObject.
     */
    //@Test
    public void testBeforeOpen() {
        System.out.println("beforeOpen");
        String order = "";
        String filter = "";
        boolean readwrite = false;
        int maxrows = 0;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.beforeOpen(order, filter, readwrite, maxrows);
    }

    /**
     * Test of open method, of class AbstractDataObject.
     */
    //@Test
    public void testOpen_0args() {
        System.out.println("open");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.open();
        assertEquals(expResult, result);
    }

    /**
     * Test of open method, of class AbstractDataObject.
     */
    //@Test
    public void testOpen_4args() {
        System.out.println("open");
        String order = "";
        String filter = "";
        boolean readwrite = false;
        int maxrows = 0;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.open(order, filter, readwrite, maxrows);
        assertEquals(expResult, result);
    }

    /**
     * Test of afterOpen method, of class AbstractDataObject.
     */
    //@Test
    public void testAfterOpen() {
        System.out.println("afterOpen");
        String order = "";
        String filter = "";
        boolean readwrite = false;
        int maxrows = 0;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.afterOpen(order, filter, readwrite, maxrows);
    }


    /**
     * Test of dataFill method, of class AbstractDataObject.
     */
    //@Test
    public void testDataFill() throws Exception {
        System.out.println("dataFill");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.dataFill();
    }

    /**
     * Test of afterDataFill method, of class AbstractDataObject.
     */
    //@Test
    public void testAfterDataFill() {
        System.out.println("afterDataFill");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.afterDataFill();
    }

    /**
     * Test of beforeRequery method, of class AbstractDataObject.
     */
    //@Test
    public void testBeforeRequery() {
        System.out.println("beforeRequery");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.beforeRequery();
    }

    /**
     * Test of requery method, of class AbstractDataObject.
     */
    //@Test
    public void testRequery_String_Map() {
        System.out.println("requery");
        String filterExtra = "";
        Map filterParams = null;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.requery(filterExtra, filterParams);
        assertEquals(expResult, result);
    }

    /**
     * Test of requery method, of class AbstractDataObject.
     */
    //@Test
    public void testRequery_0args() {
        System.out.println("requery");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.requery();
        assertEquals(expResult, result);
    }

    /**
     * Test of afterRequery method, of class AbstractDataObject.
     */
    //@Test
    public void testAfterRequery() {
        System.out.println("afterRequery");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.afterRequery();
    }

    /**
     * Test of beforeRowMove method, of class AbstractDataObject.
     */
    //@Test
    public void testBeforeRowMove() {
        System.out.println("beforeRowMove");
        IDataRow row = null;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.beforeRowMove(row);
        assertEquals(expResult, result);
    }

    /**
     * Test of goTo method, of class AbstractDataObject.
     */
    //@Test
    public void testGoTo_int() {
        System.out.println("goTo");
        int rownumber = 0;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.goTo(rownumber);
        assertEquals(expResult, result);
    }

    /**
     * Test of goTo method, of class AbstractDataObject.
     */
    //@Test
    public void testGoTo_int_int() {
        System.out.println("goTo");
        int rownumber = 0;
        int offset = 0;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.goTo(rownumber, offset);
        assertEquals(expResult, result);
    }

    /**
     * Test of afterRowMove method, of class AbstractDataObject.
     */
    //@Test
    public void testAfterRowMove() {
        System.out.println("afterRowMove");
        IDataRow row = null;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.afterRowMove(row);
    }

    /**
     * Test of moveFirst method, of class AbstractDataObject.
     */
    //@Test
    public void testMoveFirst() {
        System.out.println("moveFirst");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.moveFirst();
        assertEquals(expResult, result);
    }

    /**
     * Test of moveNext method, of class AbstractDataObject.
     */
    //@Test
    public void testMoveNext() {
        System.out.println("moveNext");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.moveNext();
        assertEquals(expResult, result);
    }

    /**
     * Test of movePreviews method, of class AbstractDataObject.
     */
    //@Test
    public void testMovePreviews() {
        System.out.println("movePreviews");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.movePreviews();
        assertEquals(expResult, result);
    }

    /**
     * Test of moveLast method, of class AbstractDataObject.
     */
    //@Test
    public void testMoveLast() {
        System.out.println("moveLast");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.moveLast();
        assertEquals(expResult, result);
    }

    /**
     * Test of findRow method, of class AbstractDataObject.
     */
    //@Test
    public void testFindRow() {
        System.out.println("findRow");
        List<? extends IDataRow> rowList = null;
        String field = "";
        Object value = null;
        int begin = 0;
        int end = 0;
        int expResult = 0;
        int result = AbstractDataObject.findRow(rowList, field, value, begin, end);
        assertEquals(expResult, result);
    }

    /**
     * Test of find method, of class AbstractDataObject.
     */
    //@Test
    public void testFind_4args() {
        System.out.println("find");
        String field = "";
        Object value = null;
        int begin = 0;
        int end = 0;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.find(field, value, begin, end);
        assertEquals(expResult, result);
    }

    /**
     * Test of find method, of class AbstractDataObject.
     */
    //@Test
    public void testFind_String_Object() {
        System.out.println("find");
        String field = "";
        Object value = null;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.find(field, value);
        assertEquals(expResult, result);
    }

    /**
     * Test of findNext method, of class AbstractDataObject.
     */
    //@Test
    public void testFindNext() {
        System.out.println("findNext");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.findNext();
        assertEquals(expResult, result);
    }

    /**
     * Test of isEof method, of class AbstractDataObject.
     */
    //@Test
    public void testIsEof() {
        System.out.println("isEof");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.isEof();
        assertEquals(expResult, result);
    }

    /**
     * Test of getNewValue method, of class AbstractDataObject.
     */
    //@Test
    public void testGetNewValue() {
        System.out.println("getNewValue");
        String fieldname = "";
        AbstractDataObject instance = new AbstractDataObjectImpl();
        Object expResult = null;
        Object result = instance.getNewValue(fieldname);
        assertEquals(expResult, result);
    }

    /**
     * Test of getFieldDefaultValue method, of class AbstractDataObject.
     */
    //@Test
    public void testGetFieldDefaultValue() {
        System.out.println("getFieldDefaultValue");
        String fieldname = "";
        AbstractDataObject instance = new AbstractDataObjectImpl();
        Object expResult = null;
        Object result = instance.getFieldDefaultValue(fieldname);
        assertEquals(expResult, result);
    }

    /**
     * Test of getField method, of class AbstractDataObject.
     */
    //@Test
    public void testGetField_String() {
        System.out.println("getField");
        String fieldname = "";
        AbstractDataObject instance = new AbstractDataObjectImpl();
        Object expResult = null;
        Object result = instance.getField(fieldname);
        assertEquals(expResult, result);
    }

    /**
     * Test of getField method, of class AbstractDataObject.
     */
    //@Test
    public void testGetField_String_String() {
        System.out.println("getField");
        String objname = "";
        String fieldname = "";
        AbstractDataObject instance = new AbstractDataObjectImpl();
        Object expResult = null;
        Object result = instance.getField(objname, fieldname);
        assertEquals(expResult, result);
    }

    /**
     * Test of getFieldOld method, of class AbstractDataObject.
     */
    //@Test
    public void testGetFieldOld() {
        System.out.println("getFieldOld");
        String fieldname = "";
        AbstractDataObject instance = new AbstractDataObjectImpl();
        Object expResult = null;
        Object result = instance.getFieldOld(fieldname);
        assertEquals(expResult, result);
    }

    /**
     * Test of getFieldObjFK method, of class AbstractDataObject.
     */
    //@Test
    public void testGetFieldObjFK() {
        System.out.println("getFieldObjFK");
        String fieldname = "";
        AbstractDataObject instance = new AbstractDataObjectImpl();
        IDataRow expResult = null;
        IDataRow result = instance.getFieldObjFK(fieldname);
        assertEquals(expResult, result);
    }

    /**
     * Test of beforeSetField method, of class AbstractDataObject.
     */
    //@Test
    public void testBeforeSetField() {
        System.out.println("beforeSetField");
        String fieldname = "";
        Object oldValue = null;
        Object newValue = null;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.beforeSetField(fieldname, oldValue, newValue);
        assertEquals(expResult, result);
    }

    /**
     * Test of setField method, of class AbstractDataObject.
     */
    //@Test
    public void testSetField_String_Object() {
        System.out.println("setField");
        String fieldname = "";
        Object value = null;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.setField(fieldname, value);
        assertEquals(expResult, result);
    }

    /**
     * Test of setField method, of class AbstractDataObject.
     */
    //@Test
    public void testSetField_String_Map() {
        System.out.println("setField");
        String fieldname = "";
        Map param = null;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.setField(fieldname, param);
        assertEquals(expResult, result);
    }

    /**
     * Test of setField method, of class AbstractDataObject.
     */
    //@Test
    public void testSetField_4args() {
        System.out.println("setField");
        String fieldname = "";
        Object newValue = null;
        boolean noAfterSetField = false;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.setField(fieldname, newValue, noAfterSetField);
        assertEquals(expResult, result);
    }

    /**
     * Test of afterSetField method, of class AbstractDataObject.
     */
    //@Test
    public void testAfterSetField() {
        System.out.println("afterSetField");
        String fieldname = "";
        Object oldValue = null;
        Object newValue = null;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.afterSetField(fieldname, oldValue, newValue);
        assertEquals(expResult, result);
    }

    /**
     * Test of isFieldExist method, of class AbstractDataObject.
     */
    //@Test
    public void testIsFieldExist() {
        System.out.println("isFieldExist");
        String fieldname = "";
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.isFieldExist(fieldname);
        assertEquals(expResult, result);
    }

    /**
     * Test of isForeingKey method, of class AbstractDataObject.
     */
    //@Test
    public void testIsForeingKey() {
        System.out.println("isForeingKey");
        String fieldname = "";
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.isForeingKey(fieldname);
        assertEquals(expResult, result);
    }

    /**
     * Test of isOpen method, of class AbstractDataObject.
     */
    //@Test
    public void testIsOpen() {
        System.out.println("isOpen");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.isOpen();
        assertEquals(expResult, result);
    }

    /**
     * Test of allowOperation method, of class AbstractDataObject.
     */
    //@Test
    public void testAllowOperation() {
        System.out.println("allowOperation");
        int operation = 0;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.allowOperation(operation);
        assertEquals(expResult, result);
    }

    /**
     * Test of getPrimaryKeyValue method, of class AbstractDataObject.
     */
    //@Test
    public void testGetPrimaryKeyValue() {
        System.out.println("getPrimaryKeyValue");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        Object expResult = null;
        Object result = instance.getPrimaryKeyValue();
        assertEquals(expResult, result);
    }

    /**
     * Test of setPrimaryKeyValue method, of class AbstractDataObject.
     */
    //@Test
    public void testSetPrimaryKeyValue() {
        System.out.println("setPrimaryKeyValue");
        Object value = null;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.setPrimaryKeyValue(value);
        assertEquals(expResult, result);
    }

    /**
     * Test of beforeRefreshRow method, of class AbstractDataObject.
     */
    //@Test
    public void testBeforeRefreshRow() {
        System.out.println("beforeRefreshRow");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.beforeRefreshRow();
    }

    /**
     * Test of refreshRow method, of class AbstractDataObject.
     */
    //@Test
    public void testRefreshRow() {
        System.out.println("refreshRow");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.refreshRow();
        assertEquals(expResult, result);
    }

    /**
     * Test of afterRefreshRow method, of class AbstractDataObject.
     */
    //@Test
    public void testAfterRefreshRow() {
        System.out.println("afterRefreshRow");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.afterRefreshRow();
    }

    /**
     * Test of beforeInsertRow method, of class AbstractDataObject.
     */
    //@Test
    public void testBeforeInsertRow() {
        System.out.println("beforeInsertRow");
        IDataRow newRow = null;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.beforeInsertRow(newRow);
        assertEquals(expResult, result);
    }

    /**
     * Test of insertRow method, of class AbstractDataObject.
     */
    //@Test
    public void testInsertRow() {
        System.out.println("insertRow");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.insertRow();
        assertEquals(expResult, result);
    }

    /**
     * Test of insertRowFrom method, of class AbstractDataObject.
     */
    //@Test
    public void testInsertRowFrom() {
        System.out.println("insertRowFrom");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.insertRowFrom();
        assertEquals(expResult, result);
    }

    /**
     * Test of afterInsertRow method, of class AbstractDataObject.
     */
    //@Test
    public void testAfterInsertRow() {
        System.out.println("afterInsertRow");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.afterInsertRow();
    }

    /**
     * Test of beforeDeleteRow method, of class AbstractDataObject.
     */
    //@Test
    public void testBeforeDeleteRow() {
        System.out.println("beforeDeleteRow");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.beforeDeleteRow();
        assertEquals(expResult, result);
    }

    /**
     * Test of deleteRow method, of class AbstractDataObject.
     */
    //@Test
    public void testDeleteRow() {
        System.out.println("deleteRow");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.deleteRow();
        assertEquals(expResult, result);
    }

    /**
     * Test of afterDeleteRow method, of class AbstractDataObject.
     */
    //@Test
    public void testAfterDeleteRow() {
        System.out.println("afterDeleteRow");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.afterDeleteRow();
    }

    /**
     * Test of copyFrom method, of class AbstractDataObject.
     */
    //@Test
    public void testCopyFrom() {
        System.out.println("copyFrom");
        String idcompany = "";
        String companyName = "";
        String xmlTag = "";
        String tableCopy = "";
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.copyFrom(idcompany, companyName, xmlTag, tableCopy);
    }

    /**
     * Test of isExists method, of class AbstractDataObject.
     */
    //@Test
    public void testIsExists() {
        System.out.println("isExists");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.isExists();
        assertEquals(expResult, result);
    }

    /**
     * Test of checkDataRow method, of class AbstractDataObject.
     */
    //@Test
    public void testCheckDataRow() throws Exception {
        System.out.println("checkDataRow");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        Map<String, IErrorReg> expResult = null;
        Map<String, IErrorReg> result = instance.checkDataRow();
        assertEquals(expResult, result);
    }

    /**
     * Test of beforeUpdate method, of class AbstractDataObject.
     */
    //@Test
    public void testBeforeUpdate_boolean() {
        System.out.println("beforeUpdate");
        boolean allRows = false;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.beforeUpdate(allRows);
        assertEquals(expResult, result);
    }

    /**
     * Test of beforeUpdate method, of class AbstractDataObject.
     */
    //@Test
    public void testBeforeUpdate_IDataSet() {
        System.out.println("beforeUpdate");
        IDataSet dataSet = null;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.beforeUpdate(dataSet);
        assertEquals(expResult, result);
    }

    /**
     * Test of beforeCheckData method, of class AbstractDataObject.
     */
    //@Test
    public void testBeforeCheckData() {
        System.out.println("beforeCheckData");
        boolean allRows = false;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.beforeCheckData(allRows);
    }

    /**
     * Test of checkData method, of class AbstractDataObject.
     */
    //@Test
    public void testCheckData_boolean() {
        System.out.println("checkData");
        boolean allRows = false;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.checkData(allRows);
        assertEquals(expResult, result);
    }

    /**
     * Test of checkData method, of class AbstractDataObject.
     */
    //@Test
    public void testCheckData_IDataSet() {
        System.out.println("checkData");
        IDataSet dataSet = null;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.checkData(dataSet);
        assertEquals(expResult, result);
    }

    /**
     * Test of afterCheckData method, of class AbstractDataObject.
     */
    //@Test
    public void testAfterCheckData() {
        System.out.println("afterCheckData");
        boolean allRows = false;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.afterCheckData(allRows);
    }

    /**
     * Test of update method, of class AbstractDataObject.
     */
    //@Test
    public void testUpdate_boolean() {
        System.out.println("update");
        boolean allRows = false;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.update(allRows);
        assertEquals(expResult, result);
    }

    /**
     * Test of update method, of class AbstractDataObject.
     */
    //@Test
    public void testUpdate_IDataSet() {
        System.out.println("update");
        IDataSet dataSet = null;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.update(dataSet);
        assertEquals(expResult, result);
    }

    /**
     * Test of afterUpdate method, of class AbstractDataObject.
     */
    //@Test
    public void testAfterUpdate_boolean() {
        System.out.println("afterUpdate");
        boolean allRows = false;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.afterUpdate(allRows);
    }

    /**
     * Test of afterUpdate method, of class AbstractDataObject.
     */
    //@Test
    public void testAfterUpdate_IDataSet() {
        System.out.println("afterUpdate");
        IDataSet dataSet = null;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.afterUpdate(dataSet);
        assertEquals(expResult, result);
    }

    /**
     * Test of beforeClose method, of class AbstractDataObject.
     */
    //@Test
    public void testBeforeClose() {
        System.out.println("beforeClose");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.beforeClose();
    }

    /**
     * Test of revert method, of class AbstractDataObject.
     */
    //@Test
    public void testRevert_0args() {
        System.out.println("revert");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.revert();
        assertEquals(expResult, result);
    }

    /**
     * Test of revert method, of class AbstractDataObject.
     */
    //@Test
    public void testRevert_Boolean() {
        System.out.println("revert");
        Boolean allRows = null;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.revert(allRows);
        assertEquals(expResult, result);
    }

    /**
     * Test of revert method, of class AbstractDataObject.
     */
    //@Test
    public void testRevert_IDataSet() {
        System.out.println("revert");
        IDataSet dataSet = null;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.revert(dataSet);
        assertEquals(expResult, result);
    }

    /**
     * Test of close method, of class AbstractDataObject.
     */
    //@Test
    public void testClose() {
        System.out.println("close");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.close();
    }

    /**
     * Test of afterClose method, of class AbstractDataObject.
     */
    //@Test
    public void testAfterClose() {
        System.out.println("afterClose");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.afterClose();
    }

    public class AbstractDataObjectImpl extends AbstractDataObject {

        public IDataLink getDAO() {
            return null;
        }

        public IDataLink getDAOCatalog() {
            return null;
        }
    }
    
}

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

import java.math.BigDecimal;
import java.util.HashMap; 
import java.util.List;
import java.util.Map;
import javax.naming.NamingException;
import org.javabeanstack.data.model.DataSet;
import org.javabeanstack.data.IDataLink;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.data.IDataSet;
import org.javabeanstack.data.IGenericDAO;
import org.javabeanstack.data.TestClass;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.events.IDataEvents;
import org.javabeanstack.exceptions.SessionError;
import org.javabeanstack.model.tables.AppUserFormView;
import org.javabeanstack.model.tables.AppUserFormViewColumn;
import org.javabeanstack.model.tables.Pais;
import org.javabeanstack.model.tables.Region;
import org.javabeanstack.services.IDataService;
import org.javabeanstack.services.IRegionSrv;
import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import static org.junit.Assert.*;

/**
 *
 * @author Jorge Enciso
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AbstractDataObjectTest extends TestClass{
    
    public AbstractDataObjectTest() {
    }


    @Test
    public void test01AddData() throws NamingException, SessionError, Exception {
        System.out.println("1-DataObject AddData");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        region.insertRow();
        region.setField("codigo", "ZZZ");
        region.setField("nombre", "ZZZ BORRAR");

        boolean result = region.update(false);
        if (!result) {
            System.out.println(region.getErrorMsg(true));
        }
        assertTrue(result);
    }
 
    @Test
    public void test02BorrarData() throws NamingException, SessionError, Exception {
        System.out.println("2-DataObject BorrarData");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        boolean result;
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        if (region.find("codigo", "ZZZ")){
            region.deleteRow();
            result = region.update(false);
            if (!result) {
                System.out.println(region.getErrorMsg(true));
            }
            assertTrue(result);
        }
    }

    @Test
    public void test03AddData() throws NamingException, SessionError, Exception {
        System.out.println("3-DataObject AddData (dataset)");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        region.insertRow();
        //region.getRow().setCodigo("ZZZ");
        region.setField("codigo", "ZZZ");
        region.setField("nombre", "ZZZ BORRAR");

        //pais
        IDataObject<Pais> pais = new DataObject(Pais.class, null, dataLink, null);
        pais.open();
        pais.insertRow();
        pais.setField("codigo", "ZZZ");
        pais.setField("nombre", "ZZZ PAIS BORRAR");
        pais.setField("region", region.getRow());

        IDataSet dataSet = new DataSet();
        dataSet.addDataObject("region", region);
        dataSet.addDataObject("pais", pais);

        boolean result = pais.update(dataSet);
        assertTrue(result);
    }

    @Test
    public void test04BorrarData() throws NamingException, SessionError, Exception {
        System.out.println("4-DataObject BorrarData");
        boolean result;
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        //Pais
        IDataObject<Pais> pais = new DataObject(Pais.class, null, dataLink, null);
        pais.open();
        if (pais.find("codigo", "ZZZ")) {
            pais.deleteRow();
            result = pais.update(false);
            assertTrue(result);
        }

        //Region        
        IDataObject region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        if (region.find("codigo", "ZZZ")){
            region.deleteRow();
            result = region.update(false);
            if (!result) {
                System.out.println(region.getErrorMsg(true));
            }
            assertTrue(result);            
        }
    }

    @Test
    public void test05DBFilter() throws NamingException, SessionError, Exception {
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
    public void test07getData() throws NamingException, SessionError, Exception {
        System.out.println("7-DataObject getData");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IGenericDAO dao = dataLink.getDao();
        IDataService dataservice
                = (IDataService) context.lookup(jndiProject + "DataService!org.javabeanstack.services.IDataServiceRemote");

        dataLink.setDao(dataservice);
        IDataObject region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        Assert.assertNotNull(region.getDataRows());

        region.setOrder("codigo desc");
        region.setFilter("codigo = '01'");
        region.requery();
        Assert.assertNotNull(region.getDataRows());
        //Devolver a valores por default
        dataLink.setDao(dao);
    }

    //@Test
    public void test08FormView() throws NamingException, SessionError, Exception {
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
    public void test09FormView() throws NamingException, SessionError, Exception {
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
    @Test
    public void test10GetErrorApp() {
        System.out.println("10-DataObject getErrorApp");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        //Region
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        // Error al proposito, orden
        region.open("no existe", "", false, -1);
        assertTrue(region.getErrorApp() != null);
        assertFalse(region.isOpen());
        //
        region.open();
        assertTrue(region.getErrorApp() == null);
        //Error filtro
        region.setFilter("camponoexiste = 1");
        region.requery();
        assertTrue(region.getErrorApp() != null);
        assertFalse(region.isOpen());
        //Error setfield
        region.open();
        assertFalse(region.setField("camponoexiste", "noexiste"));
        assertTrue(region.getErrorApp() != null);        
        //Error setField tipo de dato
        assertFalse(region.setField("nombre", BigDecimal.ZERO));
        assertTrue(region.getErrorApp() != null);        
    }

    /**
     * Test of getErrorMsg method, of class AbstractDataObject.
     */
    @Test
    public void test11GetErrorMsg() throws Exception{
        System.out.println("11-DataObject - getErrorMsg");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IRegionSrv dataServiceRegion = 
                (IRegionSrv) context.lookup(jndiProject+"RegionSrv!org.javabeanstack.services.IRegionSrvRemote");
        
        IGenericDAO dao = dataLink.getDao();
        //Cambiar dao por dataService.
        dataLink.setDao(dataServiceRegion);
        //Region
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        region.insertRow();
        region.getRow().setCodigo("");
        region.getRow().setNombre("");
        
        assertFalse(region.update(false));
        assertFalse(region.getErrorMsg(true).isEmpty());
        assertTrue(region.getErrorMsg("codigo") != null);        
        assertTrue(region.getErrorMsg("nombre") != null);                

        System.out.println(region.getErrorMsg(true));
        System.out.println(region.getErrorMsg("codigo"));
        System.out.println(region.getErrorMsg("nombre"));
        
        region.getRow().setCodigo("xx");
        region.getRow().setNombre("xx");
        assertTrue(region.checkData(true));

        region.close();
        //Volver a valores por defecto
        dataLink.setDao(dao);
    }


    /**
     * Test of getFilter method, of class AbstractDataObject.
     */
    //@Test
    public void test12GetFilter() {
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
    public void test13GetOrder() {
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
    public void test14GetFilterExtra() {
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
    public void test15GetFilterParams() {
        System.out.println("getFilterParams");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        Map<String, Object> expResult = null;
        Map<String, Object> result = instance.getFilterParams();
        assertEquals(expResult, result);
    }

    /**
     * Test of getDataRows method, of class AbstractDataObject.
     */
    @Test
    public void test16GetDataRows() {
        System.out.println("16-DataObject getDataRows");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        //No se abrio el dataObject
        assertNull(region.getDataRows());
        assertFalse(region.moveFirst());
        //
        region.open();
        assertNotNull(region.getDataRows());
        assertTrue(region.moveLast());
        //
        region.close();
        assertNull(region.getDataRows());
    }


    /**
     * Test of getSelectCmd method, of class AbstractDataObject.
     */
    //@Test
    public void test17GetSelectCmd() {
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
    public void test18GetLastQuery() {
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
    public void test19GetRowCount() {
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
    public void test20GetRecStatus() {
        System.out.println("getRecStatus");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        int expResult = 0;
        int result = instance.getRecStatus();
        assertEquals(expResult, result);
    }


    /**
     * Test of getIdcompany method, of class AbstractDataObject.
     */
    //@Test
    public void test21GetIdcompany() {
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
    public void test22GetIdempresa() {
        System.out.println("getIdempresa");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        Long expResult = null;
        Long result = instance.getIdempresa();
        assertEquals(expResult, result);
    }

    @Test
    public void test23GetDataRowsChanged() throws NamingException, SessionError, Exception {
        System.out.println("23-DataObject - getDataRowsChanged");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        region.insertRow();
        region.setField("codigo", "ZZZ");
        region.setField("nombre", "ZZZ BORRAR");
        // Verificar getDataRowsChanged
        assertTrue(region.getDataRowsChanged().size() == 1);
        assertTrue(region.getDataRowsChanged().get(region.getRecno()) != null);
        // Revertir
        region.revert();
        // Verificar getDataRowsChanged
        assertTrue(region.getDataRowsChanged().isEmpty());
        // Modificar registro
        region.moveFirst();
        region.setField("nombre", "modificado");
        assertTrue(region.getDataRowsChanged().size() == 1);
        assertTrue(region.getDataRowsChanged().get(region.getRecno()) != null);
        // Borrar registro
        region.moveNext();
        region.deleteRow();
        assertTrue(region.getDataRowsChanged().size() == 2);
    }


    /**
     * Test of beforeOpen method, of class AbstractDataObject.
     */
    //@Test
    public void test24BeforeOpen() {
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
    public void test25Open_0args() {
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
    public void test26Open_4args() {
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
    public void test27AfterOpen() {
        System.out.println("afterOpen");
        String order = "";
        String filter = "";
        boolean readwrite = false;
        int maxrows = 0;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.afterOpen(order, filter, readwrite, maxrows);
    }



    /**
     * Test of afterDataFill method, of class AbstractDataObject.
     */
    //@Test
    public void test28AfterDataFill() {
        System.out.println("afterDataFill");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.afterDataFill();
    }

    /**
     * Test of beforeRequery method, of class AbstractDataObject.
     */
    //@Test
    public void test29BeforeRequery() {
        System.out.println("beforeRequery");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.beforeRequery();
    }

    /**
     * Test of requery method, of class AbstractDataObject.
     */
    //@Test
    public void test30Requery_String_Map() {
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
    public void test31Requery_0args() {
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
    public void test32AfterRequery() {
        System.out.println("afterRequery");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.afterRequery();
    }

    /**
     * Test of beforeRowMove method, of class AbstractDataObject.
     */
    //@Test
    public void test33BeforeRowMove() {
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
    public void test34GoTo_int() {
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
    public void test35GoTo_int_int() {
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
    public void test36AfterRowMove() {
        System.out.println("afterRowMove");
        IDataRow row = null;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.afterRowMove(row);
    }

    /**
     * Test of moveFirst method, of class AbstractDataObject.
     */
    //@Test
    public void test37MoveFirst() {
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
    public void test38MoveNext() {
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
    public void test39MovePrevious() {
        System.out.println("movePreviews");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.movePrevious();
        assertEquals(expResult, result);
    }

    /**
     * Test of moveLast method, of class AbstractDataObject.
     */
    //@Test
    public void test40MoveLast() {
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
    public void test41FindRow() {
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
    public void test42Find_4args() {
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
    public void test43Find_String_Object() {
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
    public void test44FindNext() {
        System.out.println("findNext");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        boolean expResult = false;
        boolean result = instance.findNext();
        assertEquals(expResult, result);
    }

    /**
     * Test of isEof method, of class AbstractDataObject.
     */
    @Test
    public void test45IsEof() {
        System.out.println("45-DataObject - isEof");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        region.moveLast();
        region.moveNext();
        assertTrue(region.isEof());
    }

    /**
     * Test of getNewValue method, of class AbstractDataObject.
     */
    //@Test
    public void test46GetNewValue() {
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
    public void test47GetFieldDefaultValue() {
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
    @Test
    public void test48GetField_String() {
        System.out.println("48-DataObject - getField");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        assertEquals(region.getRow().getCodigo(),region.getField("codigo"));
        assertNull(region.getField("noexiste"));
    }

    /**
     * Test of getField method, of class AbstractDataObject.
     */
    @Test
    public void test49GetField_String_String() {
        System.out.println("49-DataObject - getField");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Pais> pais = new DataObject(Pais.class, null, dataLink, null);
        pais.open();
        //assertEquals(pais.getRow().getRegion(),pais.getField("region"));
        assertEquals(pais.getRow().getRegion().getCodigo(),pais.getField("region","codigo"));
        assertEquals(pais.getRow().getRegion().getCodigo(),pais.getField("region.codigo"));
    }

    /**
     * Test of getFieldOld method, of class AbstractDataObject.
     */
    @Test
    public void test50GetFieldOld() {
        System.out.println("50-DataObject - getFieldOld");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        String expResult = (String)region.getField("nombre");
        region.setField("nombre", "MODIFICADO");
        assertEquals(expResult, region.getFieldOld("nombre"));
    }

    /**
     * Test of beforeSetField method, of class AbstractDataObject.
     */
    //@Test
    public void test51BeforeSetField() {
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
    public void test52SetField_String_Object() {
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
    public void test53SetField_String_Map() {
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
    public void test54SetField_4args() {
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
    public void test55AfterSetField() {
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
    @Test
    public void test56IsFieldExist() {
        System.out.println("56-DataObject - isFieldExist");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        assertTrue(region.isFieldExist("codigo"));
        assertFalse(region.isFieldExist("noexiste"));
    }

    /**
     * Test of isForeingKey method, of class AbstractDataObject.
     */
    @Test
    public void test57IsForeingKey() {
        System.out.println("57-DataObject - isForeingKey");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Pais> pais = new DataObject(Pais.class, null, dataLink, null);
        assertTrue(pais.isForeingKey("region"));
        assertFalse(pais.isForeingKey("codigo"));
        assertFalse(pais.isForeingKey("noexiste"));
    }

    /**
     * Test of isOpen method, of class AbstractDataObject.
     */
    @Test
    public void test58IsOpen() {
        System.out.println("58-DataObject - isOpen");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        assertTrue(region.isOpen());
        region.close();
        assertFalse(region.isOpen());
    }

    /**
     * Test of allowOperation method, of class AbstractDataObject.
     */
    //@Test
    public void test59AllowOperation() {
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
    @Test
    public void test60GetPrimaryKeyValue() {
        System.out.println("60-DataObject - getPrimaryKeyValue");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        assertEquals(region.getRow().getIdregion(), region.getPrimaryKeyValue());
        assertEquals(region.getRow().getIdregion(), region.getRow().getId());
    }

    /**
     * Test of setPrimaryKeyValue method, of class AbstractDataObject.
     */
    @Test
    public void test61SetPrimaryKeyValue() {
        System.out.println("61-DataObject - setPrimaryKeyValue");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        region.setPrimaryKeyValue(100L);
        assertEquals(region.getRow().getId(),100L);
    }

    /**
     * Test of beforeRefreshRow method, of class AbstractDataObject.
     */
    //@Test
    public void test62BeforeRefreshRow() {
        System.out.println("beforeRefreshRow");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.beforeRefreshRow();
    }

    /**
     * Test of refreshRow method, of class AbstractDataObject.
     */
    @Test
    public void test63RefreshRow() {
        System.out.println("63-DataObject - refreshRow");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        String expResult = (String)region.getField("codigo");
        region.setField("codigo", "XX");
        assertEquals(region.getField("codigo"),"XX");
        region.refreshRow();
        assertEquals(expResult, region.getField("codigo"));
    }

    /**
     * Test of afterRefreshRow method, of class AbstractDataObject.
     */
    //@Test
    public void test64AfterRefreshRow() {
        System.out.println("afterRefreshRow");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.afterRefreshRow();
    }

    /**
     * Test of beforeInsertRow method, of class AbstractDataObject.
     */
    //@Test
    public void test65BeforeInsertRow() {
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
    public void test66InsertRow() {
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
    public void test67InsertRowFrom() {
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
    public void test68AfterInsertRow() {
        System.out.println("afterInsertRow");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.afterInsertRow();
    }

    /**
     * Test of beforeDeleteRow method, of class AbstractDataObject.
     */
    //@Test
    public void test69BeforeDeleteRow() {
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
    public void test70DeleteRow() {
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
    public void test71AfterDeleteRow() {
        System.out.println("afterDeleteRow");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.afterDeleteRow();
    }

    /**
     * Test of copyFrom method, of class AbstractDataObject.
     */
    //@Test
    public void test72CopyFrom() {
        System.out.println("copyFrom");
        String idcompany = "";
        String companyName = "";
        String xmlTag = "";
        String tableCopy = "";
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.copyFrom(idcompany, companyName, xmlTag, tableCopy);
    }


    /**
     * Test of checkDataRow method, of class AbstractDataObject.
     */
    //@Test
    public void test73CheckDataRow() throws Exception {
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
    public void test74BeforeUpdate_boolean() {
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
    public void test75BeforeUpdate_IDataSet() {
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
    public void test76BeforeCheckData() {
        System.out.println("beforeCheckData");
        boolean allRows = false;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.beforeCheckData(allRows);
    }

    /**
     * Test of checkData method, of class AbstractDataObject.
     */
    //@Test
    public void test77CheckData_boolean() {
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
    public void test78CheckData_IDataSet() {
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
    public void test79AfterCheckData() {
        System.out.println("afterCheckData");
        boolean allRows = false;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.afterCheckData(allRows);
    }

    /**
     * Test of update method, of class AbstractDataObject.
     */
    //@Test
    public void test80Update_boolean() {
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
    public void test81Update_IDataSet() {
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
    public void test82AfterUpdate_boolean() {
        System.out.println("afterUpdate");
        boolean allRows = false;
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.afterUpdate(allRows);
    }

    /**
     * Test of afterUpdate method, of class AbstractDataObject.
     */
    //@Test
    public void test83AfterUpdate_IDataSet() {
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
    public void test84BeforeClose() {
        System.out.println("beforeClose");
        AbstractDataObject instance = new AbstractDataObjectImpl();
        instance.beforeClose();
    }

    /**
     * Test of revert method, of class AbstractDataObject.
     */
    @Test
    public void test85Revert_0args() {
        System.out.println("85-DataObject - revert");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        region.insertRow();
        region.setField("codigo", "ZZZ");
        region.setField("nombre", "ZZZ BORRAR");

        region.insertRow();
        region.setField("codigo", "XXX");
        region.setField("nombre", "XXX BORRAR");
        // Verificar getDataRowsChanged
        assertTrue(region.getDataRowsChanged().size() == 2);
        // Revierte el registro actual
        region.revert();
        // Verificar getDataRowsChanged
        assertTrue(region.getDataRowsChanged().size() == 1);
    }

    /**
     * Test of revert method, of class AbstractDataObject.
     */
    @Test
    public void test86Revert_Boolean() {
        System.out.println("86-DataObject - revert");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        region.insertRow();
        region.setField("codigo", "ZZZ");
        region.setField("nombre", "ZZZ BORRAR");

        region.insertRow();
        region.setField("codigo", "XXX");
        region.setField("nombre", "XXX BORRAR");
        // Verificar getDataRowsChanged
        assertTrue(region.getDataRowsChanged().size() == 2);
        // Revierte todos los registros
        region.revert(true);
        // Verificar getDataRowsChanged
        assertTrue(region.getDataRowsChanged().isEmpty());
    }

    /**
     * Test of revert method, of class AbstractDataObject.
     */
    @Test
    public void test87Revert_IDataSet() {
        System.out.println("87-DataObject - Revert (dataset)");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        region.insertRow();
        //region.getRow().setCodigo("ZZZ");
        region.setField("codigo", "ZZZ");
        region.setField("nombre", "ZZZ BORRAR");

        //pais
        IDataObject<Pais> pais = new DataObject(Pais.class, null, dataLink, null);
        pais.open();
        pais.insertRow();
        pais.setField("codigo", "ZZZ");
        pais.setField("nombre", "ZZZ PAIS BORRAR");
        pais.setField("region", region.getRow());

        IDataSet dataSet = new DataSet();
        dataSet.addDataObject("region", region);
        dataSet.addDataObject("pais", pais);

        pais.revert(dataSet);
        // Verificar getDataRowsChanged
        assertTrue(region.getDataRowsChanged().isEmpty());
        assertTrue(pais.getDataRowsChanged().isEmpty());
    }

    /**
     * Test of close method, of class AbstractDataObject.
     */
    @Test
    public void test88Close() {
        System.out.println("88-DataObject - close");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        assertTrue(region.isOpen());
        region.close();
        assertFalse(region.isOpen());
        assertTrue(region.getDataRows() == null);
    }

    /**
     * Test of afterClose method, of class AbstractDataObject.
     */
    //@Test
    public void test89AfterClose() {
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

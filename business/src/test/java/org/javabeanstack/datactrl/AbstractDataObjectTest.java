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
import java.util.Map;
import javax.naming.NamingException;
import org.javabeanstack.data.model.DataSet;
import org.javabeanstack.data.IDataLink;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.data.IDataSet;
import org.javabeanstack.data.IGenericDAO;
import org.javabeanstack.data.TestClass;
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
import org.javabeanstack.datactrl.events.DataEvents;
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
        System.out.println("1-DataObject - AddData");
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
        System.out.println("2-DataObject - BorrarData");
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
        System.out.println("3-DataObject - AddData (dataset)");
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
        System.out.println("4-DataObject - BorrarData");
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
        System.out.println("5-DataObject - DBFilter");
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
        region.open();
        assertTrue(filter.contains("idempresa"));
    }


    @Test
    public void test07getData() throws NamingException, SessionError, Exception {
        System.out.println("7-DataObject - getData");
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
        System.out.println("10-DataObject - getErrorApp");
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
        // Esta programado para validar que codigo y nombre no esten en blanco
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
    @Test
    public void test12GetFilter() {
        System.out.println("12-DataObject - getFilter");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        String expResult = "codigo = 'xx'";
        region.open("",expResult, false, -1);
        assertEquals(expResult, region.getFilter());
    }

    /**
     * Test of getOrder method, of class AbstractDataObject.
     */
    @Test
    public void test13GetOrder() {
        System.out.println("13-DataObject - getOrder");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        String expResult = "codigo";
        region.open(expResult, "", false, -1);
        assertEquals(expResult, region.getOrder());
    }

    /**
     * Test of getFilterExtra method, of class AbstractDataObject.
     */
    @Test
    public void test14GetFilterExtra() {
        System.out.println("14-DataObject - getFilterExtra");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        String expResult = "codigo = 'xx'";
        region.setFilterExtra("codigo = 'xx'");
        assertEquals(expResult, region.getFilterExtra());
        region.open();
        assertEquals("", region.getFilterExtra());
    }

    /**
     * Test of getFilterParams method, of class AbstractDataObject.
     */
    @Test
    public void test15GetFilterParams() {
        System.out.println("15-DataObject - getFilterParam");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        Map<String, Object> filterParams = new HashMap();
        filterParams.put("codigo", "xx");
        region.setFilterParams(filterParams);
        region.open("", "codigo = :codigo", true, -1);
        assertNotNull(region.getFilterParams());
    }

    /**
     * Test of getDataRows method, of class AbstractDataObject.
     */
    @Test
    public void test16GetDataRows() {
        System.out.println("16-DataObject - getDataRows");
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
    @Test
    public void test17GetSelectCmd() {
        System.out.println("17-DataObject - getSelectCmd");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        String expResult = "select o from Region o";
        region.open("codigo", "", true, 0);
        System.out.println(region.getSelectCmd());
        System.out.println(region.getLastQuery());
        assertEquals(expResult, region.getSelectCmd().substring(0, 22));
        region.setFilterExtra("nombre like '%MER%'");
        region.requery();
        System.out.println(region.getSelectCmd());
        System.out.println(region.getLastQuery());
        assertEquals(expResult, region.getSelectCmd().substring(0, 22));        
    }

    /**
     * Test of getLastQuery method, of class AbstractDataObject.
     */
    @Test
    public void test18GetLastQuery() {
        System.out.println("18-DataObject - getLastQuery");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        region.setOrder("codigo");
        region.setFilter("codigo = 'xx'");
        region.requery();
        
        System.out.println(region.getSelectCmd());        
        System.out.println(region.getLastQuery());
        assertTrue(region.getLastQuery() != null);
    }


    /**
     * Test of getRowCount method, of class AbstractDataObject.
     */
    @Test
    public void test19GetRowCount() {
        System.out.println("19-DataObject - getRowCount");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        //Sin abrir
        assertTrue(region.getRowCount() == 0);
        //Abriendo y limitando cantidad de registros
        region.open("", "", true, 3);
        assertTrue(region.getRowCount() == 3);
    }

    /**
     * Test of getRecStatus method, of class AbstractDataObject.
     */
    @Test
    public void test20GetRecStatus() {
        System.out.println("20-DataObject - getRecStatus");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        region.deleteRow();
        assertEquals(IDataRow.DELETE,region.getRecStatus());
        region.moveNext();
        region.setField("codigo","x1");
        assertEquals(IDataRow.UPDATE,region.getRecStatus());
        region.insertRow();
        assertEquals(IDataRow.INSERT,region.getRecStatus());
        region.revert(true);
    }


    /**
     * Test of getIdcompany method, of class AbstractDataObject.
     */
    @Test
    public void test21GetIdcompany() {
        System.out.println("21-DataObject - getIdcompany");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        assertEquals(dataLink.getUserSession().getIdCompany(),region.getIdcompany());
    }

    /**
     * Test of getIdempresa method, of class AbstractDataObject.
     */
    @Test
    public void test22GetIdempresa() {
        System.out.println("22-DataObject - getIdempresa");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        assertEquals(region.getIdempresa(),region.getIdcompany());
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
    @Test
    public void test24BeforeOpen() {
        System.out.println("24-DataObject - beforeOpen");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataEvents dataEvents = new DataEventsTest();
        IDataObject<Region> region = new DataObject(Region.class, dataEvents, dataLink, null);
        //Aqui se va a ejecutar 
        region.open();
    }

    /**
     * Test of open method, of class AbstractDataObject.
     */
    @Test
    public void test25Open_0args() {
        System.out.println("25-DataObject - open");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        assertTrue(region.isOpen());
        assertTrue(region.getOrder().isEmpty());
        assertTrue(region.getFilter().isEmpty());
        assertTrue(region.getFirstRow() == 0);
        assertTrue(region.getMaxRows() > 999999);
    }

    /**
     * Test of open method, of class AbstractDataObject.
     */
    @Test
    public void test26Open_4args() {
        System.out.println("26-DataObject - open");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open("codigo","codigo = 'xx'",true,-1);
        assertTrue(region.isOpen());
        assertTrue("codigo".equals(region.getOrder()));
        assertTrue("codigo = 'xx'".equals(region.getFilter()));
        assertTrue(region.getFirstRow() == 0);
        assertTrue(region.getMaxRows() > 999999);
    }

    /**
     * Test of afterOpen method, of class AbstractDataObject.
     */
    @Test
    public void test27AfterOpen() {
        System.out.println("27-DataObject - afterOpen");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataEvents dataEvents = new DataEventsTest();
        IDataObject<Region> region = new DataObject(Region.class, dataEvents, dataLink, null);

        region.open();
        //Aqui se va a ejecutar         
    }



    /**
     * Test of afterDataFill method, of class AbstractDataObject.
     * Se ejecuta antes del afterOpen y afterRequery
     */
    @Test
    public void test28AfterDataFill() {
        System.out.println("28-DataObject - afterDataFill");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataEvents dataEvents = new DataEventsTest();
        IDataObject<Region> region = new DataObject(Region.class, dataEvents, dataLink, null);

        region.open();
        //Aqui
        region.requery();
        //Aqui
    }

    /**
     * Test of beforeRequery method, of class AbstractDataObject.
     */
    @Test
    public void test29BeforeRequery() {
        System.out.println("29-DataObject - beforeRequery");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataEvents dataEvents = new DataEventsTest();
        IDataObject<Region> region = new DataObject(Region.class, dataEvents, dataLink, null);

        region.open();
        //Aqui        
        region.requery();
    }

    /**
     * Test of requery method, of class AbstractDataObject.
     */
    @Test
    public void test30Requery_String_Map() {
        System.out.println("30-DataObject - requery");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        assertFalse(region.requery()); // Primero el open 
        region.open();
        Object id = region.getRow().getId();
        String filterExtra = "idregion = :id";
        Map<String, Object> params = new HashMap();
        params.put("id", id);
        region.requery(filterExtra,params); 
        //
        assertTrue(region.getRowCount() == 1);
    }

    /**
     * Test of requery method, of class AbstractDataObject.
     */
    @Test
    public void test31Requery_0args() {
        System.out.println("31-DataObject - requery");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        assertTrue(region.isOpen());
        region.setFilter("idregion > 1");
        region.setOrder("codigo");
        region.setFirstRow(1);
        region.setMaxRows(3);
        region.requery(); 
        //
        assertTrue(region.getRowCount() == 3);
        assertEquals(region.getMaxRows(),3);
        assertEquals(region.getFirstRow(),1);
        assertEquals(region.getOrder(),"codigo");
        assertEquals(region.getFilter(),"idregion > 1");
        //
    }

    /**
     * Test of afterRequery method, of class AbstractDataObject.
     */
    @Test
    public void test32AfterRequery() {
        System.out.println("32-DataObject - afterRequery");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataEvents dataEvents = new DataEventsTest();
        IDataObject<Region> region = new DataObject(Region.class, dataEvents, dataLink, null);

        region.open();

        region.requery();
        //Aqui                
    }

    /**
     * Test of beforeRowMove method, of class AbstractDataObject.
     */
    @Test
    public void test33BeforeRowMove() {
        System.out.println("33-DataObject - beforeRowMove");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataEvents dataEvents = new DataEventsTest();
        IDataObject<Region> region = new DataObject(Region.class, dataEvents, dataLink, null);

        region.open();
        region.moveFirst();
        region.moveNext();        
        region.movePrevious();
        region.moveLast();
        region.goTo(0);
    }

    /**
     * Test of goTo method, of class AbstractDataObject.
     */
    @Test
    public void test34GoTo_int() {
        System.out.println("34-DataObject - goto");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.goTo(0); // Da error debe abrirse el dataobject
        region.open();
        assertTrue(region.goTo(0)); //Primer registro
        assertFalse(region.goTo(region.getDataRows().size())); // mas alla del ultimo registro
        assertFalse(region.goTo(-1)); // mas alla del primer registro
    }

    /**
     * Test of goTo method, of class AbstractDataObject.
     */
    @Test
    public void test35GoTo_int_int() {
        System.out.println("35-DataObject - goto");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.goTo(0); // Da error debe abrirse el dataobject
        region.open();
        assertTrue(region.goTo(0)); // primer registro
        region.deleteRow();
        // Si el registro a donde se debe posicionar esta borrado ir 2 más adelante        
        assertTrue(region.goTo(0,2)); 
        assertTrue(region.getRecno() == 2);
    }

    /**
     * Test of afterRowMove method, of class AbstractDataObject.
     */
    @Test
    public void test36AfterRowMove() {
        System.out.println("36-DataObject - afterRowMove");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataEvents dataEvents = new DataEventsTest();
        IDataObject<Region> region = new DataObject(Region.class, dataEvents, dataLink, null);

        region.open();
        region.moveFirst();
        region.moveNext();        
        region.movePrevious();
        region.moveLast();
        region.goTo(0);
    }

    /**
     * Test of moveFirst method, of class AbstractDataObject.
     */
    @Test
    public void test37MoveFirst() {
        System.out.println("37-DataObject - moveFirst");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        assertFalse(region.moveFirst()); // Da error debe abrirse el dataobject
        region.open();
        assertTrue(region.moveFirst());
        assertTrue(region.getRecno() == 0);
    }

    /**
     * Test of moveNext method, of class AbstractDataObject.
     */
    @Test
    public void test38MoveNext() {
        System.out.println("38-DataObject - moveNext");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        assertFalse(region.moveNext()); // Da error debe abrirse el dataobject
        region.open();
        assertTrue(region.moveNext());
        assertTrue(region.getRecno() == 1);
    }

    /**
     * Test of movePreviews method, of class AbstractDataObject.
     */
    @Test
    public void test39MovePrevious() {
        System.out.println("39-DataObject - movePrevious");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        assertFalse(region.movePrevious()); // Da error debe abrirse el dataobject
        region.open();
        assertTrue(region.moveNext());
        assertTrue(region.movePrevious());
        assertTrue(region.getRecno() == 0);
    }

    /**
     * Test of moveLast method, of class AbstractDataObject.
     */
    @Test
    public void test40MoveLast() {
        System.out.println("40-DataObject - moveLast");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        assertTrue(region.isOpen());        
        region.moveLast();
        region.moveNext();
        assertTrue(region.isEof());
    }

    /**
     * Test of findRow method, of class AbstractDataObject.
     */
    @Test
    public void test41FindRow() {
        System.out.println("41-DataObject - findRow");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();
        
        String field = "idempresa";
        Object value = region.getIdcompany();
        int begin = 0;
        int end = 1000;
        int expResult = 0;
        int result = AbstractDataObject.findRow(region.getDataRows(), field, value, begin, end);
        assertEquals(expResult, result);
        
        begin = -1;
        end = -10;
        expResult = -1;
        result = AbstractDataObject.findRow(region.getDataRows(), field, value, begin, end);
        assertEquals(expResult, result);
        
        begin = -1;
        end = 10000;
        expResult = -1;
        value = 100L;
        result = AbstractDataObject.findRow(region.getDataRows(), field, value, begin, end);
        assertEquals(expResult, result);
        
        field = "noexiste";
        result = AbstractDataObject.findRow(region.getDataRows(), field, value, begin, end);
        assertEquals(expResult, result);
    }

    /**
     * Test of find method, of class AbstractDataObject.
     */
    @Test
    public void test42Find_4args() {
        System.out.println("42-DataObject - find");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();

        String field = "idempresa";
        Object value = region.getIdcompany();
        int begin = 0;
        int end = 1000;
        
        boolean result = region.find(field, value, begin, end);
        assertTrue(result);
    }

    /**
     * Test of find method, of class AbstractDataObject.
     */
    @Test
    public void test43Find_String_Object() {
        System.out.println("43-DataObject - find");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        region.open();

        String field = "idempresa";
        Object value = region.getIdcompany();
        
        boolean result = region.find(field, value);
        assertTrue(result);
    }

    /**
     * Test of findNext method, of class AbstractDataObject.
     */
    @Test
    public void test44FindNext() {
        System.out.println("44-DataObject - findNext");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        String field = "idempresa";
        Object value = region.getIdcompany();

        assertFalse(region.find(field, value));  //Primero el open      
        region.open();
        
        assertFalse(region.findNext());//Primero el find
        assertTrue(region.find(field, value));
        assertTrue(region.findNext());
        region.moveLast();
        region.moveNext();
        assertFalse(region.findNext());
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
        region.moveFirst();
        region.goTo(1000);
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
        assertEquals(pais.getRow().getRegion(),pais.getField("region"));
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
    @Test
    public void test51BeforeSetField() {
        System.out.println("51-DataObject - beforeSetField");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataEvents dataEvents = new DataEventsTest();
        IDataObject<Region> region = new DataObject(Region.class, dataEvents, dataLink, null);

        region.open();
        region.setField("codigo","xx");
    }

    /**
     * Test of setField method, of class AbstractDataObject.
     */
    @Test
    public void test52SetField_String_Object() {
        System.out.println("52-DataObject - setField");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Pais> pais = new DataObject(Pais.class, null, dataLink, null);
        pais.open();
        pais.setField("codigo", "PP");
        assertEquals(pais.getRow().getCodigo(),pais.getField("codigo"));
        pais.setField("region.nombre", "xx");
        assertEquals(pais.getRow().getRegion().getNombre(),pais.getField("region.nombre"));
        pais.setField("region", null);
        assertEquals(pais.getRow().getRegion(),pais.getField("region"));
    }

    /**
     * Test of setField method, of class AbstractDataObject.
     */
    @Test
    public void test53SetField_String_Map() {
        System.out.println("53-DataObject - setField");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Pais> pais = new DataObject(Pais.class, null, dataLink, null);
        pais.open();
        Map<String, Object> params = new HashMap();
        params.put("codigo", "AS");
        //Va a buscar el registro con el código "AS" si encuentra va a asignar al campo region
        pais.setField("region", params);
        assertEquals(pais.getRow().getRegion().getCodigo(),"AS");
        assertEquals(pais.getRow().getRegion().getCodigo(),pais.getField("region.codigo"));        
        assertEquals(pais.getRow().getRegion().getNombre(),pais.getField("region.nombre"));
    }


    /**
     * Test of afterSetField method, of class AbstractDataObject.
     */
    @Test
    public void test55AfterSetField() {
        System.out.println("55-DataObject - afterSetField");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataEvents dataEvents = new DataEventsTest();
        IDataObject<Region> region = new DataObject(Region.class, dataEvents, dataLink, null);

        region.open();
        region.setField("codigo","xx");
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
    @Test
    public void test59AllowOperation() {
        System.out.println("58-DataObject - allowOperation");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        assertFalse(region.allowOperation(IDataRow.INSERT)); // Debe abrir primero
        region.open();
        assertTrue(region.allowOperation(IDataRow.INSERT)); 
        assertTrue(region.allowOperation(IDataRow.UPDATE)); 
        assertTrue(region.allowOperation(IDataRow.READ)); 
        assertTrue(region.allowOperation(IDataRow.DELETE)); 
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
    @Test
    public void test62BeforeRefreshRow() {
        System.out.println("62-DataObject - beforeRefreshRow");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataEvents dataEvents = new DataEventsTest();
        IDataObject<Region> region = new DataObject(Region.class, dataEvents, dataLink, null);

        region.open();
        region.refreshRow();
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
    @Test
    public void test64AfterRefreshRow() {
        System.out.println("64-DataObject - afterRefreshRow");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataEvents dataEvents = new DataEventsTest();
        IDataObject<Region> region = new DataObject(Region.class, dataEvents, dataLink, null);

        region.open();
        region.refreshRow();
    }

    /**
     * Test of beforeInsertRow method, of class AbstractDataObject.
     */
    @Test
    public void test65BeforeInsertRow() {
        System.out.println("65-DataObject - beforeInsertRow");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataEvents dataEvents = new DataEventsTest();
        IDataObject<Region> region = new DataObject(Region.class, dataEvents, dataLink, null);

        region.open();
        region.insertRow();
    }

    /**
     * Test of insertRow method, of class AbstractDataObject.
     */
    @Test
    public void test66InsertRow() {
        System.out.println("66-DataObject - insertRow");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        assertFalse(region.insertRow()); //Debe abrir el dataobject antes
        region.open();
        assertTrue(region.insertRow());
        assertTrue(region.getRecStatus() == IDataRow.INSERT);
        region.close();
      }

    /**
     * Test of insertRowFrom method, of class AbstractDataObject.
     */
    @Test
    public void test67InsertRowFrom() {
        System.out.println("67-DataObject - insertRowFrom");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        assertFalse(region.insertRowFrom()); //Debe abrir el dataobject antes
        region.open();
        Region source = region.getRow();
        assertTrue(region.insertRowFrom());
        Region target = region.getRow();
        assertEquals(source.getCodigo(),target.getCodigo());
        assertTrue(region.getRecStatus() == IDataRow.INSERT);        
        region.close();
    }

    /**
     * Test of afterInsertRow method, of class AbstractDataObject.
     */
    @Test
    public void test68AfterInsertRow() {
        System.out.println("68-DataObject - afterInsertRow");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataEvents dataEvents = new DataEventsTest();
        IDataObject<Region> region = new DataObject(Region.class, dataEvents, dataLink, null);

        region.open();
        region.insertRow();
    }

    /**
     * Test of beforeDeleteRow method, of class AbstractDataObject.
     */
    @Test
    public void test69BeforeDeleteRow() {
        System.out.println("69-DataObject - beforeDeleteRow");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataEvents dataEvents = new DataEventsTest();
        IDataObject<Region> region = new DataObject(Region.class, dataEvents, dataLink, null);

        region.open();
        region.deleteRow();
    }

    /**
     * Test of deleteRow method, of class AbstractDataObject.
     */
    @Test
    public void test70DeleteRow() {
        System.out.println("70-DataObject - deleteRow");        
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataObject<Region> region = new DataObject(Region.class, null, dataLink, null);
        assertFalse(region.deleteRow()); //Debe abrir el dataobject antes
        region.open();
        assertTrue(region.deleteRow());
        assertTrue(region.getRecStatus() == IDataRow.DELETE);        
        region.close();
    }

    /**
     * Test of afterDeleteRow method, of class AbstractDataObject.
     */
    @Test
    public void test71AfterDeleteRow() {
        System.out.println("71-DataObject - afterInsertRow");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataEvents dataEvents = new DataEventsTest();
        IDataObject<Region> region = new DataObject(Region.class, dataEvents, dataLink, null);

        region.open();
        region.insertRow();
    }

    /**
     * Test of checkDataRow method, of class AbstractDataObject.
     */
    @Test
    public void test73CheckDataRow() throws Exception {
        System.out.println("73-DataObject - checkDataRow");
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
        // Esta programado para validar que codigo y nombre no esten en blanco
        region.getRow().setCodigo("");
        region.getRow().setNombre("");
        assertFalse(region.checkDataRow() == null);
        region.revert();
        //
        region.insertRow();
        region.getRow().setCodigo("65");
        region.getRow().setNombre("65");
        assertTrue(region.checkDataRow() == null);
        region.revert();
        dataLink.setDao(dao);
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
    @Test
    public void test77CheckData_boolean() throws Exception{
        System.out.println("77-DataObject - checkData");
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
        // Esta programado para validar que codigo y nombre no esten en blanco
        region.getRow().setCodigo("");
        region.getRow().setNombre("");
        assertFalse(region.checkData(false)); // Solo el registro posicionado
        region.revert();
        //
        region.insertRow();
        region.getRow().setCodigo("65");
        region.getRow().setNombre("65"); 
        
        region.getRow().setCodigo("");
        region.getRow().setNombre("");
        assertFalse(region.checkData(true)); // todos los registros modificados
        region.movePrevious();
        assertTrue(region.checkData(false)); // Solo el registro posicionado
        
        region.revert(true);
        dataLink.setDao(dao);
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
        instance.afterUpdate(dataSet);
    }

    /**
     * Test of beforeClose method, of class AbstractDataObject.
     */
    @Test
    public void test84BeforeClose() {
        System.out.println("84-DataObject - beforeClose");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataEvents dataEvents = new DataEventsTest();
        IDataObject<Region> region = new DataObject(Region.class, dataEvents, dataLink, null);

        region.open();
        region.close();
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
        //
        region.moveFirst();
        region.deleteRow();
        assertTrue(region.getDataRowsChanged().size() == 2);        
        region.revert();
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
        region.deleteRow();
        region.moveFirst();
        region.setField("nombre", "modificado2");
        region.insertRow();
        region.setField("codigo", "ZZZ");
        region.setField("nombre", "ZZZ BORRAR");

        region.insertRow();
        region.setField("codigo", "XXX");
        region.setField("nombre", "XXX BORRAR");
        // Verificar getDataRowsChanged
        assertTrue(region.getDataRowsChanged().size() == 4);
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
    @Test
    public void test89AfterClose() {
        System.out.println("89-DataObject - afterClose");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        IDataEvents dataEvents = new DataEventsTest();
        IDataObject<Region> region = new DataObject(Region.class, dataEvents, dataLink, null);

        region.open();
        region.close();
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

class DataEventsTest extends DataEvents{
    @Override
    public boolean onAllowOperation() {
        return (getContext().getRow() != null);
    }

    @Override
    public boolean beforeRowMove(IDataRow curRow) {
        return (curRow != null);        
    }

    @Override
    public void afterRowMove(IDataRow newRow) {
        if (!getContext().isEof()){
            assertNotNull(newRow);
        }

    }

    /**
     * Se ejecuta antes buscar los registros en la base de datos.
     * @param order     orden de la selección de datos.
     * @param filter    filtro de datos.
     * @param readwrite si es lectura/escritura
     * @param maxrows   maxima cantidad de registros a recuperar.
     */
    @Override
    public void beforeOpen(String order, String filter, boolean readwrite, int maxrows) {
        assertFalse(getContext().isOpen());
    }

    /**
     * Se ejecuta antes de recuperar los datos de la base de datos.
     */
    @Override
    public void beforeDataFill() {
        assertFalse(getContext().isOpen());
    }

    /**
     * Se ejecuta posterior a la recuperación de los registros de la base de datos.
     */
    @Override
    public void afterDataFill() {
        assertTrue(getContext().isOpen());
    }

    /**
     * Se ejecuta posterior a la recuperación de los registros de la base de datos.
     * @param order     orden de la selección de datos.
     * @param filter    filtro de datos.    
     * @param readwrite si es lectura/escritura.
     * @param maxrows   maxima cantidad de registros que deberian haberse recuperado.
     */
    @Override
    public void afterOpen(String order, String filter, boolean readwrite, int maxrows) {
        assertTrue(getContext().isOpen());
    }

    /**
     * Se ejecuta antes de recuperar los registros de la base de datos.
     */
    @Override
    public void beforeRequery() {
        assertTrue(getContext().isOpen());
    }

    /**
     * Se ejecuta posterior a haberse recuperado los registros de la base de datos.
     */
    @Override
    public void afterRequery() {
        assertTrue(getContext().isOpen());        
    }

    /**
     * Se ejecuta antes de refrescar los datos de un registro de la base de datos.
     * @param row registro a refrescar
     */
    @Override
    public void beforeRefreshRow(IDataRow row) {
        assertNotNull(row);        
    }

    /**
     * Se ejecuta posterior a refrescar un registro de la base de datos.
     * @param row registro refrescado.
     */
    @Override
    public void afterRefreshRow(IDataRow row) {
        assertNotNull(row);                
    }

    /**
     * Se ejecuta antes del metodo insertRow
     * @param newRow    fila a ser insertada
     * @return  verdadero o falso si se permite o no la inserción.
     */
    @Override
    public boolean beforeInsertRow(IDataRow newRow) {
        assertNotNull(newRow);                
        return true;
    }

    /**
     * Se ejecuta posterior al metodo insertRow
     * @param row registro insertado
     */
    @Override
    public void afterInsertRow(IDataRow row) {
        assertNotNull(row);
    }

    /**
     * Se ejecuta antes del metodo deleteRow
     * @param row   fila a ser marcada para eliminarse.
     * @return verdadero o falso si es permitido o no  la operación.
     */
    @Override
    public boolean beforeDeleteRow(IDataRow row) {
        assertNotNull(row);
        return true;
    }

    /**
     * Se ejecuta posterior al metodo deleteRow.
     */
    @Override
    public void afterDeleteRow() {
        assertTrue(getContext().getRecStatus() == IDataRow.DELETE);
    }

    /**
     * Se ejecuta antes del metodo setField.
     * 
     * @param row       registro
     * @param fieldname nombre del campo
     * @param oldValue  valor anterior
     * @param newValue  nuevo valor.
     * @return verdadero o falso si se permite la modificación del campo
     */
    @Override
    public boolean beforeSetField(IDataRow row, String fieldname, Object oldValue, Object newValue) {
        assertNotNull(row);
        assertTrue(getContext().isFieldExist(fieldname));
        return true;
    }

    /**
     * Se ejecuta posterior al metodo setField
     *  
     * @param row           registro
     * @param fieldname     nombre del campo    
     * @param oldValue      valor anterior
     * @param newValue      nuevo valor
     * @return verdadero o falso si tuvo exito.
     */
    @Override
    public boolean afterSetField(IDataRow row, String fieldname, Object oldValue, Object newValue) {
        assertNotNull(row);
        assertTrue(getContext().isFieldExist(fieldname));
        return true;
    }

    /**
     * Se ejecuta antes del metodo update
     * @param allRows   si se va a procesar todos los registros
     * @return verdadero o falso si se permite la ejecución de update.
     */
    @Override
    public boolean beforeUpdate(boolean allRows) {
        assertTrue(getContext().isOpen());
        return true;
    }

    /**
     * Se ejecuta antes del metodo checkData
     * @param allRows si se esta procesando todos los registros modificados, o solo el actual.
     */
    @Override
    public void beforeCheckData(boolean allRows) {
        assertTrue(getContext().isOpen());        
    }

    /**
     * Se ejecuta posterior al metodo checkData
     * @param allRows si se proceso todos los registros.
     */
    @Override
    public void afterCheckData(boolean allRows) {
        assertTrue(getContext().isOpen());
    }

    /**
     * Se ejecuta posterior al metodo update
     * @param allRows se se proceso todos los registros modificados.
     */
    @Override
    public void afterUpdate(boolean allRows) {
        assertTrue(getContext().isOpen());
    }

    /**
     * Se ejecuta antes de cerrar el dataObject
     */
    @Override
    public void beforeClose() {
        assertTrue(getContext().isOpen());        
    }

    /**
     * Se ejecuta posterior a cerrar el dataObject.
     */
    @Override
    public void afterClose() {
        assertFalse(getContext().isOpen());
    }
}

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

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import org.javabeanstack.model.appcatalog.AppResource;
import org.javabeanstack.model.appcatalog.AppTablesRelation;
import org.javabeanstack.model.appcatalog.AppUser;
import org.javabeanstack.model.appcatalog.AppUserFormView;
import org.javabeanstack.model.appcatalog.AppUserMember;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 *
 * @author Jorge Enciso
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DataInfoTest {

    public DataInfoTest() {
    }

    /**
     * Test of isPrimaryKey method, of class DataInfo.
     */
    @Test
    public void test01IsPrimaryKey() {
        System.out.println("1-DataInfo - isPrimaryKey");
        boolean expResult = true;
        boolean result = DataInfo.isPrimaryKey(AppUser.class, "iduser");
        assertEquals(expResult, result);

        result = DataInfo.isPrimaryKey(AppUser.class, "code");
        assertEquals(false, result);

        result = DataInfo.isPrimaryKey(AppUser.class, "noexiste");
        assertEquals(false, result);
    }

    /**
     * Test of isUniqueKey method, of class DataInfo.
     */
    @Test
    public void test02IsUniqueKey() {
        System.out.println("2-DataInfo - isUniqueKey");
        boolean expResult = true;
        boolean result = DataInfo.isUniqueKey(AppResource.class, "code");
        assertEquals(expResult, result);

        result = DataInfo.isUniqueKey(AppResource.class, "referencetime");
        assertEquals(false, result);

        result = DataInfo.isUniqueKey(AppResource.class, "noexiste");
        assertEquals(false, result);
    }

    /**
     * Test of isForeignKey method, of class DataInfo.
     */
    @Test
    public void test03IsForeignKey() {
        System.out.println("3-DataInfo - isForeignKey");
        boolean expResult = true;
        boolean result = DataInfo.isForeignKey(AppUserMember.class, "usermember");
        assertEquals(expResult, result);

        result = DataInfo.isForeignKey(AppUserMember.class, "usergroup");
        assertEquals(expResult, result);

        result = DataInfo.isForeignKey(AppUserMember.class, "otro");
        assertEquals(false, result);

        result = DataInfo.isForeignKey(AppUserMember.class, "idusermembar");
        assertEquals(false, result);
    }

    /**
     * Test of isFieldExist method, of class DataInfo.
     */
    @Test
    public void test04IsFieldExist() {
        System.out.println("4-DataInfo - isFieldExist");
        boolean expResult = true;
        boolean result = DataInfo.isFieldExist(AppUser.class, "iduser");
        assertEquals(expResult, result);

        result = DataInfo.isFieldExist(AppUser.class, "noexiste");
        assertEquals(false, result);
    }

    /**
     * Test of isLazyFetch method, of class DataInfo.
     */
    @Test
    public void test05IsLazyFetch() {
        System.out.println("5-DataInfo - isLazyFetch");
        boolean expResult = true;
        boolean result = DataInfo.isLazyFetch(AppUserFormView.class, "appUserFormViewColumnList");
        assertEquals(expResult, result);

        result = DataInfo.isLazyFetch(AppUserFormView.class, "usergroup");
        assertEquals(false, result);

        result = DataInfo.isLazyFetch(AppUserFormView.class, "noexiste");
        assertEquals(false, result);
    }

    /**
     * Test of getLazyMembers method, of class DataInfo.
     */
    @Test
    public void test06GetLazyMembers() {
        System.out.println("6-DataInfo - getLazyMembers");
        List<Field> result = DataInfo.getLazyMembers(AppUserFormView.class);
        assertTrue(!result.isEmpty());

        result = DataInfo.getLazyMembers(AppTablesRelation.class);
        assertTrue(result.isEmpty());
    }

    /**
     * Test of getTableName method, of class DataInfo.
     */
    @Test
    public void test07GetTableName() {
        System.out.println("7-DataInfo - getTableName");
        String expResult = "usuario";
        String result = DataInfo.getTableName(AppUser.class);
        assertEquals(expResult, result);
    }

    /**
     * Test of createQueryUkCommand method, of class DataInfo.
     */
    @Test
    public void test08CreateQueryUkCommand() {
        System.out.println("8-DataInfo - createQueryUkCommand");
        IDataRow ejb = new AppResource();
        String expResult = "select o from AppResource o  where  o.code = :code";
        String result = DataInfo.createQueryUkCommand(ejb);
        assertEquals(expResult, result);
    }

    /**
     * Test of getIdFieldName method, of class DataInfo.
     */
    @Test
    public void test09GetIdFieldName() {
        System.out.println("9-DataInfo - getIdFieldName");
        String expResult = "iduser";
        String result = DataInfo.getIdFieldName(AppUser.class);
        assertEquals(expResult, result);
    }

    /**
     * Test of getUniqueFields method, of class DataInfo.
     */
    @Test
    public void test10GetUniqueFields() {
        System.out.println("10-DataInfo - getUniqueFields");
        String[] expResult = {"code"};
        String[] result = DataInfo.getUniqueFields(AppResource.class);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of getIdvalue method, of class DataInfo.
     */
    @Test
    public void test11GetIdvalue() {
        System.out.println("11-DataInfo - getIdvalue");
        AppResource ejb = new AppResource();
        ejb.setIdappresource(1L);

        Object expResult = 1L;
        Object result = DataInfo.getIdvalue(ejb);
        assertEquals(expResult, result);
    }

    /**
     * Test of setIdvalue method, of class DataInfo.
     */
    @Test
    public void test12SetIdvalue() {
        System.out.println("12-DataInfo - setIdvalue");
        AppResource ejb = new AppResource();
        DataInfo.setIdvalue(ejb, 1L);

        Object expResult = 1L;
        Object result = DataInfo.getIdvalue(ejb);
        assertEquals(expResult, result);
    }

    /**
     * Test of getDefaultValue method, of class DataInfo.
     */
    @Test
    public void test13GetDefaultValue() {
        System.out.println("13-DataInfo - getDefaultValue");
        DataRow ejb = new DataInfoPrb();
        String fieldname = "codigo";
        Object expResult = "codigodefault";
        Object result = DataInfo.getDefaultValue(ejb, fieldname);
        assertEquals(expResult, result);
    }

    /**
     * Test of setIdvalue method, of class DataInfo.
     */
    @Test
    public void test14SetFieldValue() {
        System.out.println("14-DataInfo - setFieldValue");
        DataInfoPrb ejb = new DataInfoPrb();
        DataInfo.setFieldValue(ejb, "codigo", "codigocambiado");

        Object expResult = "codigocambiado";
        Object result = DataInfo.getFieldValue(ejb,"codigo");
        assertEquals(expResult, result);
        
        boolean exito = DataInfo.setFieldValue(ejb, "child.publicCodigoNombre", "codigocambiado");        
        assertTrue(exito);
    }
    
    /**
     * Test of setDefaultValue method, of class DataInfo.
     */
    @Test
    public void test15SetDefaultValue() {
        System.out.println("15-DataInfo - setDefaultValue");
        DataRow ejb = new DataInfoPrb();
        String fieldname = "codigo";        
        DataInfo.setDefaultValue(ejb, fieldname);
        Object expResult = "codigodefault";
        Object result = ejb.getValue(fieldname);
        assertEquals(expResult, result);
    }

    /**
     * Test of getFieldType method, of class DataInfo.
     */
    @Test
    public void test16GetFieldType() {
        System.out.println("16-DataInfo - getFieldType");
        Class expResult = Long.class;
        Class result = DataInfo.getFieldType(AppResource.class, "idappresource");
        assertEquals(expResult, result);
        
        result = DataInfo.getFieldType(AppResource.class, "noexiste");
        assertNull(result);
    }

    /**
     * Test of getMethodReturnType method, of class DataInfo.
     */
    @Test
    public void test17GetMethodType() {
        System.out.println("17-DataInfo - getMethodType");
        Class expResult = String.class;
        Class result = DataInfo.getMethodReturnType(DataInfoPrb.class, "getPublicCodigoNombre");
        assertEquals(expResult, result);

        result = DataInfo.getMethodReturnType(DataInfoPrb.class, "child.getPublicCodigoNombre");
        assertEquals(expResult, result);

        result = DataInfo.getMethodReturnType(DataInfoPrb.class, "getChildGetter.getPublicCodigoNombre");
        assertEquals(expResult, result);

        boolean isGetter = true;
        result = DataInfo.getMethodReturnType(DataInfoPrb.class, "getPublicCodigoNombre", isGetter);
        assertEquals(expResult, result);
        
        result = DataInfo.getMethodReturnType(DataInfoPrb.class, "publicCodigoNombre", isGetter);
        assertEquals(expResult, result);

        result = DataInfo.getMethodReturnType(DataInfoPrb.class, "getChildGetter.publicCodigoNombre", isGetter);
        assertEquals(expResult, result);

        result = DataInfo.getMethodReturnType(DataInfoPrb.class, "childGetter.getPublicCodigoNombre", isGetter);
        assertEquals(expResult, result);
        
        result = DataInfo.getMethodReturnType(DataInfoPrb.class, "childGetter.publicCodigoNombre", isGetter);
        assertEquals(expResult, result);
        
        result = DataInfo.getFieldType(DataInfoPrb.class, "noexiste"); 
        assertNull(result);
    }
    
    /**
     * Test of getFieldValue method, of class DataInfo.
     */
    @Test
    public void test18GetFieldValue() {
        System.out.println("18-DataInfo - getFieldValue");
        AppTablesRelation ejb = new AppTablesRelation();
        ejb.setEntityPK("xx1");
        ejb.setEntityFK("xx2");
        ejb.setFechacreacion(new Date());
        ejb.setFechamodificacion(new Date());
        ejb.setFieldsFK("id");
        ejb.setFieldsPK("id");
        ejb.setIncluded(false);

        String result = (String) DataInfo.getFieldValue(ejb, "fieldsFK");
        assertEquals("id", result);

        result = (String) DataInfo.getFieldValue(ejb, "noexiste");
        assertNull(result);

        // No existe el atributo, existe el getter
        DataInfoPrb infoPrb = new DataInfoPrb();
        infoPrb.setCodigo("codigo");
        infoPrb.setNombre("nombre");
        result = (String) DataInfo.getFieldValue(infoPrb, "publicCodigoNombre");
        String expResult = "codigo nombre";
        assertEquals(expResult, result);

        result = (String) DataInfo.getFieldValue(infoPrb, "privateCodigoNombre");
        assertNull(result);

        result = (String) DataInfo.getFieldValue(infoPrb, "protectedCodigoNombre");
        assertNull(result);
        
        result = (String) DataInfo.getFieldValue(infoPrb, "child.publicCodigoNombre");
        assertEquals(expResult, result);
        
        result = (String) DataInfo.getFieldValue(infoPrb, "childGetter.publicCodigoNombre");
        assertEquals(expResult, result);
    }
}

class DataInfoPrb extends DataRow{
    private String codigo="codigo";
    private String nombre="nombre";
    private String codigo_default="codigodefault";    
    private DataInfoPrbChild child = new DataInfoPrbChild();

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPublicCodigoNombre() {
        return codigo.trim() + " " + nombre.trim();
    }
    
    public void setPublicCodigoNombre(String codigo) {
        this.codigo = codigo.trim();
    }

    private String getPrivateCodigoNombre() {
        return codigo.trim() + " " + nombre.trim();
    }

    protected String getProtectedCodigoNombre() {
        return codigo.trim() + " " + nombre.trim();
    }
    
    public DataInfoPrbChild child(){
        return child;
    }
    
    public DataInfoPrbChild getChildGetter(){
        return child;
    }
}


class DataInfoPrbChild {
    private String codigo="codigo";
    private String nombre="nombre";

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPublicCodigoNombre() {
        return codigo.trim() + " " + nombre.trim();
    }
    
    public void setPublicCodigoNombre(String codigo) {
        this.codigo = codigo.trim();
    }

    private String getPrivateCodigoNombre() {
        return codigo.trim() + " " + nombre.trim();
    }

    protected String getProtectedCodigoNombre() {
        return codigo.trim() + " " + nombre.trim();
    }
}

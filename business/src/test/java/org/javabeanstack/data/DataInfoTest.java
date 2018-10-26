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
import org.javabeanstack.model.tables.AppResource;
import org.javabeanstack.model.tables.AppTablesRelation;
import org.javabeanstack.model.tables.AppUser;
import org.javabeanstack.model.tables.AppUserFormView;
import org.javabeanstack.model.tables.AppUserMember;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jorge Enciso
 */
public class DataInfoTest {

    public DataInfoTest() {
    }

    /**
     * Test of isPrimaryKey method, of class DataInfo.
     */
    @Test
    public void testIsPrimaryKey() {
        System.out.println("DataInfo - isPrimaryKey");
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
    public void testIsUniqueKey() {
        System.out.println("DataInfo - isUniqueKey");
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
    public void testIsForeignKey() {
        System.out.println("DataInfo - isForeignKey");
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
    public void testIsFieldExist() {
        System.out.println("DataInfo - isFieldExist");
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
    public void testIsLazyFetch() {
        System.out.println("DataInfo - isLazyFetch");
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
    public void testGetLazyMembers() {
        System.out.println("DataInfo - getLazyMembers");
        List<Field> result = DataInfo.getLazyMembers(AppUserFormView.class);
        assertTrue(!result.isEmpty());

        result = DataInfo.getLazyMembers(AppTablesRelation.class);
        assertTrue(result.isEmpty());
    }

    /**
     * Test of getTableName method, of class DataInfo.
     */
    @Test
    public void testGetTableName() {
        System.out.println("DataInfo - getTableName");
        String expResult = "usuario";
        String result = DataInfo.getTableName(AppUser.class);
        assertEquals(expResult, result);
    }

    /**
     * Test of createQueryUkCommand method, of class DataInfo.
     */
    @Test
    public void testCreateQueryUkCommand() {
        System.out.println("DataInfo - createQueryUkCommand");
        IDataRow ejb = new AppResource();
        String expResult = "select o from AppResource o  where  o.code = :code";
        String result = DataInfo.createQueryUkCommand(ejb);
        assertEquals(expResult, result);
    }

    /**
     * Test of getIdFieldName method, of class DataInfo.
     */
    @Test
    public void testGetIdFieldName() {
        System.out.println("DataInfo - getIdFieldName");
        String expResult = "iduser";
        String result = DataInfo.getIdFieldName(AppUser.class);
        assertEquals(expResult, result);
    }

    /**
     * Test of getUniqueFields method, of class DataInfo.
     */
    @Test
    public void testGetUniqueFields() {
        System.out.println("DataInfo - getUniqueFields");
        String[] expResult = {"code"};
        String[] result = DataInfo.getUniqueFields(AppResource.class);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of getIdvalue method, of class DataInfo.
     */
    @Test
    public void testGetIdvalue() {
        System.out.println("DataInfo - getIdvalue");
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
    public void testSetIdvalue() {
        System.out.println("DataInfo - setIdvalue");
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
    public void testGetDefaultValue() {
        System.out.println("DataInfo - getDefaultValue");
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
    public void testSetFieldValue() {
        System.out.println("DataInfo - setFieldValue");
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
    public void testSetDefaultValue() {
        System.out.println("DataInfo - setDefaultValue");
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
    public void testGetFieldType() {
        System.out.println("DataInfo - getFieldType");
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
    public void testGetMethodType() {
        System.out.println("DataInfo - getMethodType");
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
    public void testGetFieldValue() {
        System.out.println("DataInfo - getFieldValue");
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

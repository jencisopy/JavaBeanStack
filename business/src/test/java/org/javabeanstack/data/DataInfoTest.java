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
        System.out.println("isPrimaryKey");
        boolean expResult = true;
        boolean result = DataInfo.isPrimaryKey(AppUser.class,"iduser");
        assertEquals(expResult, result);
        
        result = DataInfo.isPrimaryKey(AppUser.class,"code");
        assertEquals(false, result);
        
        result = DataInfo.isPrimaryKey(AppUser.class,"noexiste");
        assertEquals(false, result);
    }

    /**
     * Test of isUniqueKey method, of class DataInfo.
     */
    @Test
    public void testIsUniqueKey() {
        System.out.println("isUniqueKey");
        boolean expResult = true;
        boolean result = DataInfo.isUniqueKey(AppResource.class,"code");
        assertEquals(expResult, result);
        
        result = DataInfo.isUniqueKey(AppResource.class,"referencetime");
        assertEquals(false, result);
        
        result = DataInfo.isUniqueKey(AppResource.class,"noexiste");
        assertEquals(false, result);
    }

    /**
     * Test of isForeignKey method, of class DataInfo.
     */
    @Test
    public void testIsForeignKey() {
        System.out.println("isForeignKey");
        boolean expResult = true;
        boolean result = DataInfo.isForeignKey(AppUserMember.class,"usermember");
        assertEquals(expResult, result);
        
        result = DataInfo.isForeignKey(AppUserMember.class,"usergroup");
        assertEquals(expResult, result);
        
        result = DataInfo.isForeignKey(AppUserMember.class,"otro");
        assertEquals(false, result);
        
        result = DataInfo.isForeignKey(AppUserMember.class,"idusermembar");
        assertEquals(false, result);
    }

    /**
     * Test of isFieldExist method, of class DataInfo.
     */
    @Test
    public void testIsFieldExist() {
        System.out.println("isFieldExist");
        boolean expResult = true;
        boolean result = DataInfo.isFieldExist(AppUser.class,"iduser");
        assertEquals(expResult, result);
        
        result = DataInfo.isFieldExist(AppUser.class,"noexiste");
        assertEquals(false, result);
    }

    /**
     * Test of isLazyFetch method, of class DataInfo.
     */
    @Test
    public void testIsLazyFetch() {
        System.out.println("isLazyFetch");
        boolean expResult = true;
        boolean result = DataInfo.isLazyFetch(AppUserFormView.class,"appUserFormViewColumnList");
        assertEquals(expResult, result);
        
        result = DataInfo.isLazyFetch(AppUserFormView.class,"usergroup");
        assertEquals(false, result);

        result = DataInfo.isLazyFetch(AppUserFormView.class,"noexiste");
        assertEquals(false, result);
    }

    /**
     * Test of getLazyMembers method, of class DataInfo.
     */
    @Test
    public void testGetLazyMembers() {
        System.out.println("getLazyMembers");
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
        System.out.println("getTableName");
        String expResult = "usuario";
        String result = DataInfo.getTableName(AppUser.class);
        assertEquals(expResult, result);
    }

    /**
     * Test of createQueryUkCommand method, of class DataInfo.
     */
    @Test
    public void testCreateQueryUkCommand() {
        System.out.println("createQueryUkCommand");
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
        System.out.println("getIdFieldName");
        String expResult = "iduser";
        String result = DataInfo.getIdFieldName(AppUser.class);
        assertEquals(expResult, result);
    }

    /**
     * Test of getUniqueFields method, of class DataInfo.
     */
    @Test
    public void testGetUniqueFields() {
        System.out.println("getUniqueFields");
        String[] expResult = {"code"};
        String[] result = DataInfo.getUniqueFields(AppResource.class);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of getIdvalue method, of class DataInfo.
     */
    @Test
    public void testGetIdvalue() {
        System.out.println("getIdvalue");
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
        System.out.println("setIdvalue");
        AppResource ejb = new AppResource();
        DataInfo.setIdvalue(ejb, 1L);

        Object expResult = 1L;
        Object result = DataInfo.getIdvalue(ejb);
        assertEquals(expResult, result);
    }

    /**
     * Test of getDefaultValue method, of class DataInfo.
     */
    //@Test
    public void testGetDefaultValue() {
        System.out.println("getDefaultValue");
        Object ejb = null;
        String fieldname = "";
        Object expResult = null;
        //Object result = DataInfo.getDefaultValue(ejb, fieldname);
        //assertEquals(expResult, result);
    }

    /**
     * Test of setDefaultValue method, of class DataInfo.
     */
    //@Test
    public void testSetDefaultValue() {
        System.out.println("setDefaultValue");
        Object ejb = null;
        String fieldname = "";
        //DataInfo.setDefaultValue(ejb, fieldname);
    }


    /**
     * Test of getFieldType method, of class DataInfo.
     */
    @Test
    public void testGetFieldType() {
        System.out.println("getFieldType");
        Class expResult = Long.class;
        Class result = DataInfo.getFieldType(AppResource.class,"idappresource");
        assertEquals(expResult, result);
    }

    /**
     * Test of getFieldValue method, of class DataInfo.
     */
    @Test
    public void testGetFieldValue() {
        System.out.println("getFieldValue");
        AppTablesRelation ejb = new AppTablesRelation();
        ejb.setEntityPK("xx1");
        ejb.setEntityFK("xx2");
        ejb.setFechacreacion(new Date());
        ejb.setFechamodificacion(new Date());
        ejb.setFieldsFK("id");
        ejb.setFieldsPK("id");
        ejb.setIncluded(false);

        String result = (String)DataInfo.getFieldValue(ejb,"fieldsFK");
        assertEquals("id", result);
        
        result = (String)DataInfo.getFieldValue(ejb,"noexiste");
        assertNull(result);
    }
}

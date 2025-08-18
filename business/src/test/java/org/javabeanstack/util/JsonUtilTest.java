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
package org.javabeanstack.util;

import java.util.ArrayList;
import java.util.List;
import jakarta.json.JsonArray;
import org.javabeanstack.data.DataNativeQuery;
import org.javabeanstack.data.IDataQueryModel;
import org.javabeanstack.data.TestClass;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
/**
 *
 * @author Jorge Enciso
 */
public class JsonUtilTest  extends TestClass{
    public JsonUtilTest() {
    }
    
    /**
     * Test of create method, of class JsonUtil.
     * @throws java.lang.Exception
     */
    @Test
    public void testCreate() throws Exception{
        System.out.println("1-JsonUtil - create");
        //No hubo conexión con el servidor de aplicaciones
        if (error != null) {
            System.out.println(error);
            return;
        }
        DataNativeQuery query = (DataNativeQuery)dataLink.newDataNativeQuery();
        List<IDataQueryModel> data = 
            query.select("codigo, nombre, cambio, fechacreacion, fechareplicacion")
                .from("moneda")
                .execQuery();
        
        JsonArray model = JsonUtil.create(data);
        assertFalse(model.toString().isEmpty());
        
        model = JsonUtil.create(null);
        assertTrue(model == null);
        
        model = JsonUtil.create(new ArrayList());
        assertTrue(model.toString().equals("[]"));
        
        data = query.select("codigo, nombre, noedit")
                .from("pais")
                .execQuery();
        
        model = JsonUtil.create(data);
        assertFalse(model.toString().isEmpty());
        data = query.select("codigo, nombre, noedit")
                .from("ctacte")
                .execQuery(0,100);
        model = JsonUtil.create(data);
        assertFalse(model.toString().isEmpty());
    }
}

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

import java.io.StringWriter;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.javabeanstack.data.TestClass;
import org.javabeanstack.model.tables.Moneda;

import org.junit.jupiter.api.Test;
/**
 *
 * @author Jorge Enciso
 */
public class JsonUtilTest2 extends TestClass {

    public JsonUtilTest2() {
    }

    @Test
    public void serializeObjectToJson() throws Exception {
        Moneda moneda = new Moneda();
        JAXBContext jaxbContext = JAXBContext.newInstance(Moneda.class);
        
        moneda.setCodigo("GS.");
        moneda.setNombre("Guaranies");
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE); // To format XML        

        StringWriter sw = new StringWriter();
        marshaller.marshal(moneda, sw);
        System.out.println(sw.toString());
        //assertEquals("{\"email\":\"user@example.com\", \"password\":\"mySecret\"}", sw.toString());
    }
}

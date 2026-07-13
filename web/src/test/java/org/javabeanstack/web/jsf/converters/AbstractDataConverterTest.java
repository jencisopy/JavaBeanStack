/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
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
package org.javabeanstack.web.jsf.converters;

import java.util.Map;
import jakarta.persistence.Id;

import org.javabeanstack.data.DataRow;
import org.javabeanstack.data.IDataLink;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de AbstractDataConverter. Se cubren los caminos que no
 * requieren un FacesContext activo ni acceso a la base: conversión de objeto a
 * string, resolución de la clase de la entidad y parseo de valores json
 * simples. getAsObject con valores válidos requiere DAO y sesión, se valida en
 * el ciclo funcional JSF.
 *
 * @author Jorge Enciso
 */
public class AbstractDataConverterTest {

    public AbstractDataConverterTest() {
    }

    /**
     * Entidad de prueba con id Long.
     */
    public static class ClienteTest extends DataRow {
        @Id
        private Long idcliente;

        public Long getIdcliente() {
            return idcliente;
        }

        public void setIdcliente(Long idcliente) {
            this.idcliente = idcliente;
        }
    }

    /**
     * Converter concreto de prueba; el tipo se resuelve por el parámetro
     * genérico de la superclase.
     */
    static class ClienteConverter extends AbstractDataConverter<ClienteTest> {
        @Override
        public IDataLink getDAO() {
            return null;
        }
    }

    /**
     * Test of getAsString: nulo devuelve nulo, string devuelve el mismo valor,
     * registro con id devuelve el id como texto y sin id devuelve nulo.
     */
    @Test
    public void testGetAsString() {
        System.out.println("abstractDataConverter getAsString");
        ClienteConverter converter = new ClienteConverter();
        assertNull(converter.getAsString(null, null, null));
        assertEquals("abc", converter.getAsString(null, null, "abc"));

        ClienteTest row = new ClienteTest();
        row.setIdcliente(77L);
        assertEquals("77", converter.getAsString(null, null, row));

        assertNull(converter.getAsString(null, null, new ClienteTest()));
        //Objeto que no es String ni IDataRow
        assertNull(converter.getAsString(null, null, 123L));
    }

    /**
     * Test of getAsObject: valores nulos o vacíos devuelven nulo sin consultar
     * el DAO.
     */
    @Test
    public void testGetAsObjectVacio() {
        System.out.println("abstractDataConverter getAsObjectVacio");
        ClienteConverter converter = new ClienteConverter();
        assertNull(converter.getAsObject(null, null, null));
        assertNull(converter.getAsObject(null, null, ""));
    }

    /**
     * Test of getClase: resuelta por el parámetro genérico de la subclase o
     * por la clase pasada en el constructor.
     */
    @Test
    public void testGetClase() {
        System.out.println("abstractDataConverter getClase");
        assertEquals(ClienteTest.class, new ClienteConverter().getClase());

        AbstractDataConverter<ClienteTest> porConstructor
                = new AbstractDataConverter<ClienteTest>(ClienteTest.class) {
            @Override
            public IDataLink getDAO() {
                return null;
            }
        };
        assertEquals(ClienteTest.class, porConstructor.getClase());
    }

    /**
     * Test of getValuesFrom: parsea un json simple a un map, los valores con
     * comillas como texto y los numéricos como Long, sin importar el orden de
     * los pares ni los espacios después de los dos puntos.
     */
    @Test
    public void testGetValuesFrom() {
        System.out.println("abstractDataConverter getValuesFrom");
        ClienteConverter converter = new ClienteConverter();
        Map<String, Object> values
                = converter.getValuesFrom("{\"idcliente\": 5, \"nombre\": \"Juan\"}");
        assertEquals(5L, values.get("idcliente"));
        assertEquals("Juan", values.get("nombre"));

        //Valor numérico en la última posición (antes perdía el último dígito)
        values = converter.getValuesFrom("{\"nombre\": \"Juan\", \"idcliente\": 10}");
        assertEquals(10L, values.get("idcliente"));
        assertEquals("Juan", values.get("nombre"));
    }
}

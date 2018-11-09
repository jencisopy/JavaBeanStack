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

import javax.persistence.Id;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jorge Enciso
 */
public class DataRowTest {

    public DataRowTest() {
    }


    /**
     * Test of getValue method, of class DataRow.
     */
    @Test
    public void testGetValue() {
        System.out.println("DataRow - getValue");
        String expResult = "codigo nombre";
        DataRowPrb row = new DataRowPrb();
        String result = (String)row.getValue("codigo");
        assertEquals("codigo", result);
        
        // Desde un getter 
        result = (String)row.getValue("publicCodigoNombre");
        assertEquals(expResult, result);

        // No existe atributo
        result = (String)row.getValue("noexiste");
        assertNull(result);

        // Desde getter privado (no puede acceder)
        result = (String)row.getValue("privateCodigoNombre");
        assertNull(result);

        // Desde getter de un miembro 
        result = (String)row.getValue("child.publicCodigoNombre");
        assertEquals(expResult, result);
        
        // Desde getter de un miembro
        result = (String)row.getValue("childGetter.publicCodigoNombre");
        assertEquals(expResult, result);
        
        // Si el miembro es de tipo DataRow trae el id del componente
        Object result2 = ((IDataRow)row.getValue("child")).getId();
        assertEquals(1L, result2);
    }

    /**
     * Test of getFieldType method, of class DataRow.
     */
    @Test
    public void testGetFieldType() {
        System.out.println("DataRow - getFieldType");
        
        Class expResult = String.class;
        DataRowPrb row = new DataRowPrb();
        Class result = row.getFieldType("codigo");
        assertEquals(expResult, result);

        result = row.getFieldType("publicCodigoNombre");
        assertEquals(expResult, result);
        
        result = row.getFieldType("noexiste");
        assertNull(result);

        result = row.getFieldType("privateCodigoNombre");
        assertNull(result);
        
        result = row.getFieldType("publicCodigoNombre");
        assertEquals(expResult, result);
        
        result = row.getFieldType("child.publicCodigoNombre");
        assertEquals(expResult, result);
        
        result = row.getFieldType("getChild.getPublicCodigoNombre");
        assertEquals(expResult, result);
        
        result = row.getFieldType("child.getPublicCodigoNombre");
        assertEquals(expResult, result);
        
        result = row.getFieldType("childGetter.publicCodigoNombre");
        assertEquals(expResult, result);
        
        result = row.getFieldType("childGetter.getPublicCodigoNombre");
        assertEquals(expResult, result);
    }

    /**
     * Test of setValue method, of class DataRow.
     */
    @Test
    public void testSetValue() throws Exception {
        System.out.println("DataRow - setValue");
        String fieldname = "codigo";
        Object value = "codigocambiado";
        DataRow row = new DataRowPrb();
        row.setValue(fieldname, value);
        assertEquals("codigocambiado", row.getValue(fieldname));
        
        fieldname = "child.codigo";
        value = "codigocambiado";
        row.setValue(fieldname, value);
        assertEquals("codigocambiado", row.getValue(fieldname));
    }
    
    @Test
    public void testHashcode() {
        System.out.println("DataRow - HashCode");
        DataRowPrb row = new DataRowPrb();
        row.setId(2L);
        int hashCode = row.hashCode();
        System.out.println(row.hashCode());
        for (int i = 0;i<10;i++){
            int hashCode2 = row.hashCode();
            assertEquals(hashCode, hashCode2);
        }
        row.setId(999999999999999L);
        System.out.println(row.hashCode());
        row.setId(-999999999999999L);
        System.out.println(row.hashCode());
    }
    
    class DataRowPrb extends DataRow {
        @Id
        private Long id = 1L;
        private String codigo = "codigo";
        private String nombre = "nombre";
        private final DataRowPrbChild child = new DataRowPrbChild();

        
        public void setId(Long id) {
            this.id = id;
        }
        
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

        private String getPrivateCodigoNombre() {
            return codigo.trim() + " " + nombre.trim();
        }

        protected String getProtectedCodigoNombre() {
            return codigo.trim() + " " + nombre.trim();
        }

        public DataRowPrbChild getChild() {
            return child;
        }

        public DataRowPrbChild getChildGetter() {
            return child;
        }
    }
    
    

    class DataRowPrbChild extends DataRow {
        @Id
        private Long id = 1L;
        private String codigo = "codigo";
        private String nombre = "nombre";

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

        private String getPrivateCodigoNombre() {
            return codigo.trim() + " " + nombre.trim();
        }

        protected String getProtectedCodigoNombre() {
            return codigo.trim() + " " + nombre.trim();
        }
    }
}

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

import javax.naming.NamingException;
import org.javabeanstack.annotation.FieldFilter;
import org.javabeanstack.data.TestClass;
import org.javabeanstack.exceptions.SessionError;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import static org.junit.Assert.*;

/**
 *
 * @author Jorge Enciso
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DataFilterTest extends TestClass{
    
    public DataFilterTest() {
    }


    @Test
    public void test01Simple() throws NamingException, SessionError, Exception {
        System.out.println("1-DataFilter - test01Simple");
        String result = "(fecha is null) and (nro = :nro) and (nro between :nroInicial and :nroFinal)";
        DataFilterExample1 dataFilter = new DataFilterExample1();
        dataFilter.setNro("10");
        dataFilter.setNroInicial(1);
        dataFilter.setNroFinal(10);
        dataFilter.createSentence();
        assertEquals(result, dataFilter.getFilterExpression());
    }

    @Test
    public void test02Herencia() throws NamingException, SessionError, Exception {
        System.out.println("2-DataFilter - test02Herencia");
        String result = "(fecha is null) and (nro = :nro) and (nro between :nroInicial and :nroFinal) and (comentario like :comentario)";
        DataFilterTestX2 dataFilter = new DataFilterTestX2();
        dataFilter.setNro("10");
        dataFilter.setNroInicial(1);
        dataFilter.setNroFinal(10);
        dataFilter.setComentario("hola");
        dataFilter.createSentence();
        assertEquals(result, dataFilter.getFilterExpression());
    }

    @Test
    public void test03In() throws NamingException, SessionError, Exception {
        System.out.println("3-DataFilter - test03In");
        String result = "(pendiente in(:confirmado,:anulado))";
        DataFilterTestX3 dataFilter = new DataFilterTestX3();
        dataFilter.createSentence();
        assertEquals(result, dataFilter.getFilterExpression());
    }
    
    
    
    class DataFilterTestX2 extends DataFilterExample1{
        private String comentario;

        @FieldFilter(expression = "comentario like :comentario", mode="contain", order = "9")        
        public String getComentario() {
            return comentario;
        }

        public void setComentario(String comentario) {
            this.comentario = comentario;
        }
    }
    
    class DataFilterTestX3 extends DataFilter{
        private Boolean confirmado=true;
        private Boolean anulado=false;

        @FieldFilter(expression = "pendiente in(:confirmado,:anulado)")
        public Boolean getConfirmado() {
            return confirmado;
        }

        @FieldFilter()
        public Boolean getAnulado() {
            return anulado;
        }

        public void setConfirmado(Boolean confirmado) {
            this.confirmado = confirmado;
        }

        public void setAnulado(Boolean anulado) {
            this.anulado = anulado;
        }
    }
}

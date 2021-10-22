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

import java.time.LocalDateTime;
import org.javabeanstack.annotation.FieldFilter;

/**
 *
 * @author Jorge Enciso
 */
public class DataFilterExample1 extends DataFilter {
    private String nro;
    private LocalDateTime fecha;
    private Integer nroInicial;
    private Integer nroFinal;

    @FieldFilter(expression = "nro = :nro", order = "2")    
    public String getNro() {
        return nro;
    }

    public void setNro(String nro) {
        this.nro = nro;
    }

    @FieldFilter(expression = "fecha = :fecha", order = "1",nullOrEmptyExpression = "fecha is null")    
    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    @FieldFilter(expression = "nro between :nroInicial and :nroFinal", order = "3")    
    public Integer getNroInicial() {
        return nroInicial;
    }

    public void setNroInicial(Integer nroInicial) {
        this.nroInicial = nroInicial;
    }

    @FieldFilter()    
    public Integer getNroFinal() {
        return nroFinal;
    }

    public void setNroFinal(Integer nroFinal) {
        this.nroFinal = nroFinal;
    }
}

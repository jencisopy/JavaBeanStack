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

package org.javabeanstack.data;

/**
 * Modelo utilizado en la clase DBFilter
 * @author Jorge Enciso
 */
public class DBFilterElement implements IDBFilterElement{
    private String key;
    private Object keyValue;
    private Integer group;
    
    /**
     * Devuelve el nombre del campo del elemento de filtro.
     *
     * @return nombre del campo.
     */
    @Override
    public String getFieldName() {
        return key;
    }

    /**
     * Asigna el nombre del campo del elemento de filtro.
     *
     * @param key nombre del campo.
     */
    @Override
    public void setFieldName(String key) {
        this.key = key;
    }

    /**
     * Devuelve el valor a comparar del elemento de filtro.
     *
     * @return valor del campo.
     */
    @Override
    public Object getFieldValue() {
        return keyValue;
    }

    /**
     * Asigna el valor a comparar del elemento de filtro.
     *
     * @param keyValue valor del campo.
     */
    @Override
    public void setFieldValue(Object keyValue) {
        this.keyValue = keyValue;
    }

    /**
     * Devuelve el grupo al que pertenece el elemento de filtro.
     *
     * @return número de grupo.
     */
    @Override
    public Integer getFieldGroup() {
        return group;
    }

    /**
     * Asigna el grupo al que pertenece el elemento de filtro.
     *
     * @param group número de grupo.
     */
    @Override
    public void setFieldGroup(Integer group) {
        this.group = group;
    }
}

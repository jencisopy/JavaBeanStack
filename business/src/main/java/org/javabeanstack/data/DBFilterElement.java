/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 Jorge Enciso
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
 *
 * @author Jorge Enciso
 */
public class DBFilterElement implements IDBFilterElement{
    private String key;
    private Object keyValue;
    private Integer group;
    
    @Override
    public String getFieldName() {
        return key;
    }

    @Override
    public void setFieldName(String key) {
        this.key = key;
    }

    @Override
    public Object getFieldValue() {
        return keyValue;
    }

    @Override
    public void setFieldValue(Object keyValue) {
        this.keyValue = keyValue;
    }

    @Override
    public Integer getFieldGroup() {
        return group;
    }

    @Override
    public void setFieldGroup(Integer group) {
        this.group = group;
    }
}

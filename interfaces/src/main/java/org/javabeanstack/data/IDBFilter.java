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

import java.io.Serializable;
import java.util.List;


/**
 *
 * @author Jorge Enciso
 * @param <E>
 */
public interface IDBFilter<E extends IDBFilterElement> extends Serializable {
    void addFilter(String fieldName, Object fieldValue, Integer group);
    List<E> getFilter();
    String getFilterExpr(Integer element);
    String getFilterExpr(Integer element, Integer group);
    String getFilterExpr(Integer element, String alias);
    String getFilterExpr(Integer element, Integer group, String alias);    
    <T extends IDataRow> String getFilterExpr(Class<T> clazz, String alias);    
    String getAllFilterExpr();
    String getAllFilterExpr(String alias);
    String getAllFilterExpr(Integer group);
    String getAllFilterExpr(Integer group, String alias);
    String getModelPackagePath();
    void setModelPackagePath(String modelPath);
}

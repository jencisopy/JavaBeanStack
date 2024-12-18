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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;



/**
 * Esta interfase define metodos que permitiran acceder más facil e intuitivamente
 * a la información resultante de un nativequery 
 * @author Jorge Enciso
 */
public interface IDataQueryModel {
    public Object getValue(String columnName);    
    public Object getValue(int index);        
    public Object getColumnId();
    public Object getColumn(int index);
    public Object getColumn(String columnName);
    public String getColumnStr(String columnName);    
    public String getColumnStr(int index);        
    public BigDecimal getColumnNumber(String columnName);
    public BigDecimal getColumnNumber(int index);    
    public Long getColumnLong(String columnName);
    public Long getColumnLong(int index);
    public Integer getColumnInt(String columnName);    
    public Integer getColumnInt(int index);        
    public LocalDateTime getColumnLocalDate(String columnName);
    public LocalDateTime getColumnLocalDate(int index);    
    public String getColumnName(int index);
    public String[] getColumnList();
    public void setColumnId(int index);
    public void setColumnList(String[] columnList);
    public Object getRow();
    public void setRow(Object row);
    public void setColumn(int index, Object value);
    public void setColumn(String columnName, Object value);
    public void setValue(int index, Object value);
    public void setValue(String columnName, Object value);
    public Map<String, Object> getProperties();
    public void setProperties(Map<String, Object> properties);
    public Object getProperty(String key);
    public void setProperty(String key, Object value);
    
}

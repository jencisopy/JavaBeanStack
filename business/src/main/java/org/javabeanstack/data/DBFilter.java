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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.javabeanstack.util.Strings;

/**
 *
 * @author Jorge Enciso
 */
public class DBFilter implements IDBFilter<DBFilterElement> {
    private final List<DBFilterElement> filter = new ArrayList();

    @Override
    public List<DBFilterElement> getFilter() {
        return filter;
    }

    @Override
    public String getFilterExpr(Integer element) {
        return getFilterExpr(element, "");
    }

    @Override
    public String getFilterExpr(Integer element, Integer group) {
        return getFilterExpr(element, group, "");
    }

    @Override
    public String getFilterExpr(Integer element, String alias) {
        return getFilterExpr(element,null, alias);
    }
    
    @Override
    public String getFilterExpr(Integer element, Integer group, String alias) {
        if (element == null || element >= filter.size()) {
            return "";
        }
        if (!Strings.isNullorEmpty(alias)) {
            alias += ".";
        }
        String result = "";
        String field = filter.get(element).getFieldName();
        Object value = filter.get(element).getFieldValue();
        Integer group1 = filter.get(element).getFieldGroup();
        if (group != null && !Objects.equals(group, group1)){
            return result;
        }
        // Si el valor es cadena
        if (value instanceof String) {
            result = "(" + alias + field + "='" + value + "'" + ")";
        } // Si el valor es una lista
        else if (value instanceof List) {
            if (((List) value).isEmpty()) {
                result = "";
            } else {
                result = "("+alias+field+" IN(";
                String separador = "";
                for (Object e:(List)value){
                    if (e instanceof String){
                        result += separador + "'" + e.toString() + "'";
                        separador = ",";
                    }
                    else{
                        result += separador + e.toString();
                        separador = ",";
                    }
                }
                result += "))";
            }
        } // Si el valor es numerico
        else {
            result = "(" + alias + field + "=" + value + ")";
        }
        return result;
    }

    @Override
    public String getAllFilterExpr() {
        return getAllFilterExpr(null,"");
    }
    
    @Override
    public String getAllFilterExpr(Integer group) {
        return getAllFilterExpr(group,"");
    }

    @Override
    public String getAllFilterExpr(String alias) {
        return getAllFilterExpr(null, alias);
    }
    
    @Override
    public String getAllFilterExpr(Integer group, String alias) {
        String expr;
        String result = "";
        String separador = "";
        for (int i = 0; i < filter.size(); i++) {
            expr = getFilterExpr(i, group, alias);
            if (!expr.isEmpty()){
                result += separador + expr;
                separador = " and ";                                
            }
        }
        return result;
    }

    @Override 
    public void addFilter(String fieldName, Object fieldValue, Integer group) {
        DBFilterElement element = new DBFilterElement();
        element.setFieldName(fieldName);
        element.setFieldValue(fieldValue);
        element.setFieldGroup(group);
        filter.add(element);
    }
    
    @Override
    public <T extends IDataRow> String getFilterExpr(Class<T> clazz, String alias){
        String result = "";
        String separador = "";        
        int c = 0;
        for (IDBFilterElement element:filter){
            // Si existe el campo en el ejb se agrega al filtro
            if (DataInfo.isFieldExist(clazz, element.getFieldName())){
                String expr = getFilterExpr(c, alias);
                if (!expr.isEmpty()){
                    result += separador + expr;
                    separador = " and ";                                
                }
            }
            c++;
        }
        return result;
    }
}

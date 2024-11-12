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
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.Strings;

/**
 * Clase con filtros (creado en el login del usuario) que deben ser aplicados en
 * los queries con propositos varios (seguridad, acceso solo a la empresa
 * logeada etc)
 *
 * @author Jorge Enciso
 */
public class DBFilter implements IDBFilter<DBFilterElement> {

    private final List<DBFilterElement> filter = new ArrayList();
    private String modelPackagePath = "";

    /**
     * Devuelve lista de filtros.
     *
     * @return lista de filtros.
     */
    @Override
    public List<DBFilterElement> getFilter() {
        return filter;
    }

    /**
     * Devuelve un filtro que se encuentra en una posición dada en la lista de
     * filtros.
     *
     * @param element posición o nro. de elemento de la lista de filtros.
     * @return expresión del filtro.
     */
    @Override
    public String getFilterExpr(Integer element) {
        return getFilterExpr(element, "");
    }

    /**
     * Devuelve un filtro que se encuentra en una posición dada en la lista de
     * filtros.
     *
     * @param element posición o nro. de elemento de la lista de filtros.
     * @param group considerar el nro. de elemento solo de un grupo dado.
     * @return expresión del filtro.
     */
    @Override
    public String getFilterExpr(Integer element, Integer group) {
        return getFilterExpr(element, group, "");
    }

    /**
     * Devuelve un filtro que se encuentra en una posición dada en la lista de
     * filtros.
     *
     * @param element posición o nro. de elemento de la lista de filtros.
     * @param alias es una etiqueta que se agrega a la expresión del filtro.
     * @return expresión del filtro.
     */
    @Override
    public String getFilterExpr(Integer element, String alias) {
        return getFilterExpr(element, null, alias);
    }

    /**
     * Devuelve un filtro que se encuentra en una posición dada en la lista de
     * filtros.
     *
     * @param element posición o nro. de elemento de la lista de filtros.
     * @param group considerar el nro. de elemento solo de un grupo dado.
     * @param alias es una etiqueta que se agrega a la expresión del filtro.
     * @return expresión del filtro.
     */
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
        if (group != null && !Objects.equals(group, group1)) {
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
                result = "(" + alias + field + " IN(";
                String separador = "";
                for (Object e : (List) value) {
                    if (e instanceof String) {
                        result += separador + "'" + e.toString() + "'";
                        separador = ",";
                    } else {
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

    /**
     * Devuelve una expresión del filtro a partir de una lista
     *
     * @return expresión del filtro.
     */
    @Override
    public String getAllFilterExpr() {
        return getAllFilterExpr(null, "");
    }

    /**
     * Devuelve una expresión del filtro a partir de una lista
     *
     * @param group indica que se genere la expresión de un grupo dado.
     * @return expresión del filtro.
     */
    @Override
    public String getAllFilterExpr(Integer group) {
        return getAllFilterExpr(group, "");
    }

    /**
     * Devuelve una expresión del filtro a partir de una lista
     *
     * @param alias es una etiqueta que se agrega a la expresión del filtro.
     * @return expresión del filtro.
     */
    @Override
    public String getAllFilterExpr(String alias) {
        return getAllFilterExpr(null, alias);
    }

    /**
     * Devuelve una expresión del filtro a partir de una lista
     *
     * @param group indica que se genere la expresión de un grupo dado.
     * @param alias es una etiqueta que se agrega a la expresión del filtro.
     * @return expresión del filtro.
     */
    @Override
    public String getAllFilterExpr(Integer group, String alias) {
        String expr;
        String result = "";
        String separador = "";
        for (int i = 0; i < filter.size(); i++) {
            expr = getFilterExpr(i, group, alias);
            if (!expr.isEmpty()) {
                result += separador + expr;
                separador = " and ";
            }
        }
        return result;
    }

    /**
     * Agrega una expresión de filtro a la lista.
     *
     * @param fieldName nombre del campo
     * @param fieldValue valor
     * @param group grupo
     */
    @Override
    public void addFilter(String fieldName, Object fieldValue, Integer group) {
        DBFilterElement element = new DBFilterElement();
        element.setFieldName(fieldName);
        element.setFieldValue(fieldValue);
        element.setFieldGroup(group);
        filter.add(element);
    }

    /**
     * Devuelve una expresión completa de todos los filtros que pueden ser
     * aplicados a un modelo dado.
     *
     * @param <T>
     * @param clazz modelo.
     * @param alias etiqueta a agregar en las expresiones.
     * @param jpqlSentence
     * @return filtro que puede aplicarse al modelo dado.
     */
    @Override
    public <T extends IDataRow> String getFilterExpr(Class<T> clazz, String alias, boolean jpqlSentence) {
        String result = "";
        String separador = "";
        int c = 0;
        for (IDBFilterElement element : filter) {
            // Si existe el campo en el ejb se agrega al filtro
            if (DataInfo.isFieldExist(clazz, element.getFieldName())) {
                String expr = getFilterExpr(c, alias);
                if (!expr.isEmpty()) {
                    result += separador + expr;
                    separador = " and ";
                }
            } else if (jpqlSentence){
                org.javabeanstack.annotation.DBFilter annotation
                        = clazz.getAnnotation(org.javabeanstack.annotation.DBFilter.class);
                //Si esta definido en la anotación el campo y la expresión
                if (annotation != null) {
                    if (annotation.fieldKey().equals(element.getFieldName())) {
                        String field = annotation.fieldName();
                        if (!Fn.nvl(alias,"").isEmpty()){
                            field = alias+"."+field;
                        }
                        String expr = "(" + field + "=" + element.getFieldValue() + ")" ;
                        if (!expr.isEmpty()) {
                            result += separador + expr;
                            separador = " and ";
                        }
                    }
                }
            }
            c++;
        }
        return result;
    }
    
    /**
     * Devuelve una expresión completa de todos los filtros que pueden ser
     * aplicados a un modelo dado.
     *
     * @param <T>
     * @param clazz modelo.
     * @param alias etiqueta a agregar en las expresiones.
     * @return filtro que puede aplicarse al modelo dado.
     */
    @Override
    public <T extends IDataRow> String getFilterExpr(Class<T> clazz, String alias) {
        return getFilterExpr(clazz, alias, true);
    }

    /**
     * Devuelve el path de paquetes donde buscar los modelos ejbs.
     *
     * @return path de paquetes donde buscar los modelos ejbs
     */
    @Override
    public String getModelPackagePath() {
        return modelPackagePath;
    }

    /**
     * Asigna el path de paquetes donde buscar el modelo ejb. Normalmente este
     * valor se asigna en la creacion de sesión del usuario.
     *
     * @param modelPath
     */
    @Override
    public void setModelPackagePath(String modelPath) {
        this.modelPackagePath = modelPath;
    }
}

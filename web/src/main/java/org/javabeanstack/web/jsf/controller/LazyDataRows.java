/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2018 Jorge Enciso
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
package org.javabeanstack.web.jsf.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;

import org.javabeanstack.data.DataInfo;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.util.Strings;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.util.Dates;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.LocalDates;
import org.javabeanstack.web.model.IColumnModel;
import org.primefaces.model.FilterMeta;

/**
 *
 * @author jenciso
 * @param <T>
 */
public class LazyDataRows<T extends IDataRow> extends LazyDataModel<T> {

    private static final Logger LOGGER = LogManager.getLogger(LazyDataRows.class);
    public AbstractDataController context;
    private boolean noCount = false;
    private int timesLoaded = 0;

    public LazyDataRows(AbstractDataController context) {
        this.context = context;
    }

    public boolean isNoCount() {
        return noCount;
    }

    public void setNoCount(boolean noCount) {
        this.noCount = noCount;
    }

    public List<T> getRows() {
        if (context != null) {
            return context.getDataRows();
        }
        return null;
    }

    @Override
    public T getRowData(String rowKey) {
        //Si no hay rowKey
        if (context == null || rowKey == null || rowKey.isEmpty()) {
            return null;
        }
        Object id;
        if (!rowKey.contains("}")) {
            return null;
        }
        String rowKeyValue = rowKey.substring(rowKey.indexOf("}") + 1);
        String rowKeyType = rowKey.substring(1, rowKey.indexOf("}"));
        if (context.getRow() != null) {
            //Verificar si el registro actual posicionado es igual al rowKeyValue
            id = context.getRow().getId();
            if (rowKeyValue != null && rowKeyValue.equals(id.toString())) {
                return (T) context.getRow();
            }
        }
        if (getRows() == null) {
            return null;
        }
        id = getIdValue(rowKeyType, rowKeyValue);
        //Buscar el registro que coincida con el id en la lista de registros
        for (T row : getRows()) {
            if (row == null) {
                continue;
            }
            if (row.getId().equals(id)) {
                return row;
            }
        }
        return null;
    }

    protected Object getIdValue(String type, String value) {
        if (type.equals("Long")) {
            return Long.valueOf(value);
        }
        if (type.equals("Integer")) {
            return Integer.valueOf(value);
        }
        if (type.equals("Short")) {
            return Short.valueOf(value);
        }
        if (type.equals("String")) {
            return value;
        }
        try {
            //Tipo DataRow
            Class clazz = Class.forName(type);
            Long id = Long.valueOf(value);
            Object row = context.getDAO().findById(clazz, id);
            return row;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    @Override
    public String getRowKey(T row) {
        if (row == null) {
            return "";
        }
        return row.getRowkey();
    }

    public Class<T> getEntityClass() {
        if (context != null) {
            return context.getType();
        }
        return null;
    }

    @Override
    public List<T> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        if (context == null){
            return null;
        }
        try {
            String order = "";
            if (sortBy != null) {
                for (SortMeta sortMeta : sortBy.values()) {
                    final SortOrder orders = sortMeta.getOrder();
                    if (orders == SortOrder.ASCENDING || orders == SortOrder.DESCENDING) {
                        order = order + " " + sortMeta.getField() + ((orders == SortOrder.ASCENDING) ? " asc" : " desc") + ",";
                    }
                }
                if (!Strings.isNullorEmpty(order)) {
                    order = order.substring(0, order.length() - 1);
                }
            } else {
                order = context.getOrder();
            }
            return load(first, pageSize, order, filterBy);
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }
    
    private List<T> load(int first, int pageSize, String order, Map<String, FilterMeta> filters) {
        if (context == null) {
            return null;
        }
        try {
            if (!context.beforeLazyRowsLoad() || !context.isOpen()) {
                return new ArrayList();
            }
            if (context.getNoLazyRowsLoad()) {
                return context.getDataRows();
            }
            if (context.getFacesCtx().getFacesContext().getAttributes().get("nolazyload") != null) {
                Boolean noLazyLoad = (Boolean) context.getFacesCtx().getFacesContext().getAttributes().get("nolazyload");
                if (timesLoaded > 0 && noLazyLoad) {
                    return context.getDataRows();
                }
                context.getFacesCtx().getFacesContext().getAttributes().put("nolazyload", false);
            }
            String extraFilter = "";
            Map<String, Object> params = getParams(filters);
            if (!filters.isEmpty()) {
                extraFilter = getFilterString(filters);
            }
            context.setFirstRow(first);
            context.setMaxRows(pageSize);

            context.removeFilter("borrar");
            context.addFilter("borrar", extraFilter);
            if (params != null && !params.isEmpty()) {
                context.addFilterParams(params);
            }

            if (Strings.isNullorEmpty(order)
                    && !Strings.isNullorEmpty(context.getOrder())) {
                context.setOrder(context.getOrder());
            } else {
                context.setOrder(order);
            }
            context.requery();
            List<T> rows = context.getDataRows();
            timesLoaded++;
            setRowCount(pageSize);
            if (!noCount) {
                setRowCount(context.getDAO().getCount(context.getLastQuery(), context.getFilterParams()).intValue());
            } else {
                if (rows != null && noCount) {
                    if (rows.size() < pageSize) {
                        setRowCount(first + rows.size());
                    } else {
                        setRowCount(first + pageSize + 1);
                    }
                }
            }
            context.setRowSelected(null);
            context.afterLazyRowsLoad();
            return rows;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Utilizado por el metodo load, devuelve un map con el nombre del campo
     * como clave y el valor del campo con su tipo de dato correspondiente.
     *
     * @param filters filtro (campo, valor)
     * @return
     */
    private Map<String, Object> getParams(Map<String, FilterMeta> filters) {
        Map<String, Object> params = new HashMap<>();
        for (Map.Entry e : filters.entrySet()) {
            Class clase;
            String key = e.getKey().toString().replace(".", "");
            clase = DataInfo.getFieldType(getEntityClass(), (String) e.getKey());
            //TODO: verificar valor getfiltervalue 
            Object value = ((FilterMeta)e.getValue()).getFilterValue();
            //Si es alfanumerico, ver si tiene mascara de busqueda
            if (String.class.isAssignableFrom(clase)) {
                value = getValueWithFilterMask((String) value, (String) e.getKey());
            }
            //Si el tipo de busqueda es contenida en la porción izquierda
            if (String.class.isAssignableFrom(clase)
                    && (getFilterMode(e.getKey().toString()).equalsIgnoreCase("contain_ltrim"))) {
                params.put(key, ((String) value) + "%");
            } //Si el tipo de busqueda es contenida en la porción derecha
            else if (String.class.isAssignableFrom(clase)
                    && (getFilterMode(e.getKey().toString()).equalsIgnoreCase("contain_rtrim"))) {
                params.put(key, "%" + ((String) value));
            } //Si el tipo de busqueda es contenida dentro del campo
            else if (String.class.isAssignableFrom(clase)
                    && (Fn.nvl(getFilterMode(e.getKey().toString()), "").isEmpty()
                    || getFilterMode(e.getKey().toString()).equalsIgnoreCase("contain"))
                    || getFilterMode(e.getKey().toString()).equalsIgnoreCase("contain_trim")) {
                params.put(key, "%" + ((String) value) + "%");
            } else if (String.class.isAssignableFrom(clase)) {
                params.put(key, ((String) value));
            } else if (Long.class.isAssignableFrom(clase)) {
                params.put(key, Long.valueOf((String) e.getValue()));
            } else if (Integer.class.isAssignableFrom(clase)) {
                params.put(key, Integer.valueOf((String) e.getValue()));
            } else if (Short.class.isAssignableFrom(clase)) {
                params.put(key, Short.valueOf((String) e.getValue()));
            } else if (BigDecimal.class.isAssignableFrom(clase)) {
                params.put(key, new BigDecimal((String) e.getValue()));
            } else if (LocalDateTime.class.isAssignableFrom(clase)) {
                params.put(key, LocalDates.toDateTime((String) e.getValue()));
            } else if (Date.class.isAssignableFrom(clase)) {
                params.put(key, Dates.toDate((String) e.getValue()));
            } else {
                params.put(key, e.getValue());
            }
        }
        return params;
    }

    /**
     * Utilizado en el metodo load, devuelve la sentencia WHERE que filtra los
     * datos de la lista
     *
     * @param filters filtros (campo, valor)
     * @return
     */
    private String getFilterString(Map<String, FilterMeta> filters) {
        if (context == null) {
            return "";
        }
        String queryWhere;
        // Si en el controller se define los filtros
        queryWhere = context.onGetFilterString(filters);
        if (!queryWhere.isEmpty()) {
            return queryWhere;
        }
        String filter;
        String separador = "";
        for (Map.Entry e : filters.entrySet()) {
            Class clase = DataInfo.getFieldType(getEntityClass(), (String) e.getKey());
            String key = e.getKey().toString().replace(".", "");
            if (clase != null && String.class.isAssignableFrom(clase)) {
                String filterMode = getFilterMode(e.getKey().toString());
                if (Fn.nvl(filterMode, "").equals("exact")) {
                    // Si el tipo de busqueda es exacta
                    filter = separador + " o." + e.getKey() + " = :" + key;
                } else if (Fn.nvl(filterMode, "").equals("exact_trim")) {
                    // Si el tipo de busqueda exacta
                    filter = separador + " trim(o." + e.getKey() + ") = :" + key;
                } else if (Fn.nvl(filterMode, "").equals("exact_ltrim")) {
                    // Si el tipo de busqueda es exacta
                    filter = separador + " ltrim(o." + e.getKey() + ") = :" + key;
                } else if (Fn.nvl(filterMode, "").equals("contain_ltrim")) {
                    // Si el tipo de busqueda contenida
                    filter = separador + " ltrim(upper(o." + e.getKey() + ")) like upper(:" + key + ")";
                } else if (Fn.nvl(filterMode, "").equals("contain_rtrim")) {
                    // Si el tipo de busqueda contenida
                    filter = separador + " rtrim(upper(o." + e.getKey() + ")) like upper(:" + key + ")";
                } else {
                    // Si el campo es string buscar un valor contenido en el campo
                    filter = separador + " upper(o." + e.getKey() + ") like upper(:" + key + ")";
                }
            } else {
                filter = separador + " o." + e.getKey() + " = :" + key;
            }
            queryWhere += filter;
            separador = " and ";
        }
        return queryWhere;
    }

    private String getFilterMode(String columnName) {
        if (context == null) {
            return "";
        }
        List<IColumnModel> columns = context.getDataTable().getColumns();
        // Buscar por el campo filter
        for (IColumnModel column : columns) {
            if (column.getFilter().equalsIgnoreCase(columnName)) {
                return column.getFilterMode();
            }
        }
        // Buscar por el nombre de la columna
        for (IColumnModel column : columns) {
            if (column.getName().equalsIgnoreCase(columnName)) {
                return column.getFilterMode();
            }
        }
        return "";
    }

    private String getFilterMask(String columnName) {
        if (context == null) {
            return "";
        }
        List<IColumnModel> columns = context.getDataTable().getColumns();
        // Buscar por el campo filter
        for (IColumnModel column : columns) {
            if (column.getFilter().equalsIgnoreCase(columnName)) {
                return column.getFilterMask();
            }
        }
        // Buscar por el nombre de la columna
        for (IColumnModel column : columns) {
            if (column.getName().equalsIgnoreCase(columnName)) {
                return column.getFilterMask();
            }
        }
        return "";
    }

    private String getValueWithFilterMask(String value, String columnName) {
        if (Fn.nvl(value, "").isEmpty()) {
            return value;
        }
        String filterMask = getFilterMask(columnName);
        if (Fn.nvl(filterMask, "").isEmpty()) {
            return value.trim();
        }
        try {
            if (filterMask.toLowerCase().contains("_blank_")) {
                String[] parts = filterMask.split("_");
                Integer size = Integer.valueOf(parts[2]);
                if (parts[0].equalsIgnoreCase("right")) {
                    value = Strings.rightPad(value.trim(), size, " ");
                } else if (parts[0].equalsIgnoreCase("left")) {
                    value = Strings.leftPad(value.trim(), size, " ");
                }
            } else if (filterMask.toLowerCase().contains("replace")) {
                String[] parts = filterMask.split(",");
                parts[0] = Strings.substr(parts[0], 8).replace("'", "");
                parts[1] = Strings.substr(parts[1], 0, parts[1].trim().length() - 1).replace("'", "");
                value = value.replace(parts[0], parts[1]);
            }
            return value;
        } catch (Exception exp) {
            //nada
        }
        return value.trim();
    }

    @Override
    public int count(Map<String, FilterMeta> filterBy) {
        //TODO: verificar si load se ejecuta primero
        return getRowCount();
    }
}

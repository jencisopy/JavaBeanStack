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

package org.javabeanstack.web.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.javabeanstack.data.DataNativeQuery;
import org.javabeanstack.data.IDBFilter;

import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import org.javabeanstack.util.Strings;
import org.javabeanstack.data.IDataLink;
import org.javabeanstack.data.IDataQueryModel;
import org.javabeanstack.data.IDataNativeQuery;
import org.javabeanstack.error.ErrorManager;

/**
 *
 * @author jenciso
 * @param <T>
 */
public class RptLazyNativeHelp<T extends IDataQueryModel> extends LazyDataModel<T> {
    private static final Logger LOGGER = Logger.getLogger(RptLazyNativeHelp.class);
    
    private List<T> rows;
    private String entidad;
    private Boolean noLoad = true;
    private String sortFieldPrevious;
    private Map<String, Object> filtersPrevious = new HashMap<>();
    private String columns;
    private String entity;
    private IDataLink dao;

    public RptLazyNativeHelp(IDataLink dataAccess) {
        super();
        this.dao = dataAccess;
    } 
    

    public IDataLink getDao() {
        return dao;
    }

    public List<T> getRows() {
        return rows;
    }

    public void setDao(IDataLink dao) {
        this.dao = dao;
    }


    public String getEntidad() {
        return entidad;
    }

    public void setEntidad(String entidad) {
        entidad = entidad.toLowerCase().trim();
        entity  = entidad;
        if (Strings.isNullorEmpty(columns) && !Strings.isNullorEmpty(entidad)){
            entidad = entidad.replace("_view", "");        
            columns = "'"+entidad+"' as entidad, id"+entidad+" as id,codigo, nombre";                
        }
        else{
            columns = "codigo,nombre";                    
        }
    }

    public String getColumns() {
        return columns;
    }
    
    public void setColumns(String columns){
        this.columns = columns;
    }

    public Boolean getNoLoad() {
        return noLoad;
    }

    public void setNoLoad(Boolean noLoad) {
        this.noLoad = noLoad;
    }
    
    @Override
    public T getRowData(String rowKey) {
        for (T row : rows) {
            if (row.getColumnId().equals(rowKey)) {
                return row;
            }
        }
        return null;
    }

    @Override
    public Object getRowKey(T row) {
        return row.getColumnId();
    }

    /**
     * Devuelve una lista de datos. Es un evento ejecutado desde el primeface
     * cuando se puebla por primera vez la grilla, o cuando se pasa de una
     * pagina a otra, o se filtra los datos en la grilla
     *
     * @param first a partir de que nro de registro va a devolver los datos.
     * @param pageSize cantidad de registros a devolver.
     * @param sortField campo que se utilizará para ordenar la lista
     * @param sortOrder orden ascendente o descentente
     * @param filters filtros (campo, valor)
     * @return lista de datos resultante de los parámetros introducidos.
     */
    @Override
    public List<T> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
        if (noLoad == true) {
            setRowCount(0);
            return new ArrayList<>(); 
        } 
        try {
            if (filters.isEmpty() && !filtersPrevious.isEmpty() && sortField != null){
                filters = filtersPrevious;
            }
            if (sortField == null && !Strings.isNullorEmpty(sortFieldPrevious)){
                sortField = sortFieldPrevious;
            }
            // Ordenamiento
            String sortExpr;
            if (sortField != null) {
                sortExpr = sortField + ((sortOrder == SortOrder.ASCENDING) ? " asc" : " desc");
            }
            else{
                sortExpr = "nombre";
            }
            // Parametros
            Map<String, Object> params = getParams(filters); 
            // Filtro
            String filterExpr = getFilterString(filters); 
            IDataNativeQuery query = dao.newDataNativeQuery();
            List<T> list = (List<T>)query.
                                    select(columns).
                                    from(entity).
                                    where(filterExpr).
                                    orderBy(sortExpr).addParams(params).
                                    execQuery(first, pageSize);
                                                        
            if (list != null) {
                Long rowCount = query.getCount();
                setRowCount(Integer.parseInt(rowCount.toString()));
            } else {
                setRowCount(0);
            }
            sortFieldPrevious = sortField;
            filtersPrevious = filters;            
            return list;
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
    private Map<String, Object> getParams(Map<String, Object> filters) {
        Map<String, Object> params = new HashMap<>();
        try {
            for (Map.Entry e : filters.entrySet()) {
                params.put((String) e.getKey(), ((String)e.getValue()).trim() + "%");
            }
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
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
    private String getFilterString(Map<String, Object> filters) {
        IDBFilter dbFilter = dao.getUserSession().getDBFilter();
        Class clazz = DataNativeQuery.getClassModel(dbFilter.getModelPackagePath(), entity);
        String queryWhere;
        if (clazz != null){
            queryWhere = dbFilter.getFilterExpr(clazz, "");
        }
        else{
            queryWhere = "idempresa = :idempresa";            
        }

        try {
            String filter;
            String separador = (queryWhere.isEmpty() ? "" : " and ");
            for (Map.Entry e : filters.entrySet()) {
                filter = separador + " upper(" + e.getKey() + ") like upper(:" + e.getKey() + ")";
                queryWhere += filter;
                separador = " and ";
            }
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return queryWhere;
    }
}

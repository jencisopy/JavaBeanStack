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
package org.javabeanstack.web.model;

import org.javabeanstack.util.Fn;

/**
 * Modelo de columnas utilizado para manejar dataTables con columnas dinamicas
 * @author mtrinidad
 */ 
public class ColumnModel implements IColumnModel {

    private String header;
    private Integer property;
    private String name;
    private String order;
    private String filter;
    private Boolean visible;
    private Boolean sortable;
    private Boolean filterable;
    private Boolean exportable;
    private Boolean toggleable;
    private String link;
    private String id;
    private String style;
    private String filterFunction;
    private int width;
    private String mask;
    private String filterMode;
    private String filterMask;    
    private String type;    
    private String title;
    private String dataSourceParams;

    public ColumnModel() {
    }

    public ColumnModel(String header, Integer property, String column, String style, int width,String link, String filterFunction, String mask) {
        this.header = header;        
        this.property = property;        
        this.name = column;
        this.filter = column;
        this.order = column;
        this.style= style;
        this.width = width;
        this.link=link;
        this.filterFunction=filterFunction;
        this.mask=mask;
    }

    public ColumnModel(String header, Integer property, String column, Boolean visible,String link, String id) {
        this.header = header;
        this.property = property;
        this.name = column;
        this.filter = column;
        this.order = column;
        this.visible= visible;
        this.link  = link;
        this.id=id;
    }    
    
    public ColumnModel(String header, Integer property, String column, Boolean visible,String link, String id,String mask) {
        this.header = header;
        this.property = property;
        this.name = column;
        this.filter = column;
        this.order = column;
        this.visible= visible;
        this.link  = link;
        this.id=id;
        this.mask=mask;
    }

    /**
     * Devuelve encabezado de la columna
     * @return 
     */
    @Override
    public String getHeader() {
        return header;
    }

    /**
     * Devuelve el nro de elemento al cual corresponde una columna
     * @return 
     */
    @Override
    public Integer getProperty() {
        return property;
    }

    /**
     * Establece la cabecera para una columna
     * @param header 
     */
    @Override
    public void setHeader(String header) {
        this.header = header;
    }

    /**
     * Establece el nro de elemento al cual corresponde una columna
     * @param property 
     */
    @Override
    public void setProperty(Integer property) {
        this.property = property;
    }

    /**
     * Devuelve nombre de la columna, esto cuando se tienen valores fijos y no dinamicos
     * @return 
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Establece valores de la columna, cuando se tienen valores fijos y no dinamicos
     * @param name 
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }    

    @Override
    public Boolean getVisible() {
        return visible;
    }

    @Override
    public void setVisible(Boolean visible) {
        this.visible = visible;
    }    

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }    

    @Override
    public String getLink() {
        return link;
    }

    @Override
    public void setLink(String link) {
        this.link = link;
    }

    @Override
    public String getStyle() {
        return style;
    }

    @Override
    public void setStyle(String style) {
        this.style = style;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public String getFilterFunction() {
        return filterFunction;
    }

    @Override
    public void setFilterFunction(String filterFunction) {
        this.filterFunction = filterFunction;
    }

    @Override
    public String getMask() {
        return mask;
    }

    @Override
    public void setMask(String mask) {
        this.mask = mask;
    }

    @Override
    public String getOrder() {
        if (Fn.nvl(order, "").isEmpty()){
            order = name;
        }
        return order;
    }

    @Override
    public void setOrder(String order) {
        this.order = order;
    }

    @Override
    public String getFilter() {
        if (Fn.nvl(filter, "").isEmpty()){
            filter = name;
        }
        return filter;
    }

    @Override
    public void setFilter(String filter) {
        this.filter = filter;
    }

    @Override
    public Boolean getSortable() {
        return sortable;
    }

    @Override
    public void setSortable(Boolean sortable) {
        this.sortable = sortable;
    }

    @Override
    public Boolean getFilterable() {
        return filterable;
    }

    @Override
    public void setFilterable(Boolean filterable) {
        this.filterable = filterable;
    }

    @Override
    public Boolean getExportable() {
        return exportable;
    }

    @Override
    public void setExportable(Boolean exportable) {
        this.exportable = exportable;
    }

    @Override
    public Boolean getToggleable() {
        return toggleable;
    }

    @Override
    public void setToggleable(Boolean toggleable) {
        this.toggleable = toggleable;
    }

    @Override
    public String getFilterMode() {
        return filterMode;
    }

    @Override
    public void setFilterMode(String filterMode) {
        this.filterMode = filterMode;
    }

    @Override
    public String getFilterMask() {
        return this.filterMask;
    }

    @Override
    public void setFilterMask(String filterMask) {
        this.filterMask = filterMask;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getDataSourceParams() {
        return this.dataSourceParams;
    }

    @Override
    public void setDataSourceParams(String dataSourceParams) {
        this.dataSourceParams = dataSourceParams;
    }
    
    @Override
    public String toString() {
        return this.getHeader();
    }
}

/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2018 - 2027 Jorge Enciso
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

    /**
     * Constructor por defecto.
     */
    public ColumnModel() {
    }

    /**
     * Crea el modelo de columna con estilo, ancho y función de filtro.
     *
     * @param header encabezado.
     * @param property índice del elemento.
     * @param column nombre de la columna.
     * @param style estilo CSS.
     * @param width ancho.
     * @param link enlace.
     * @param filterFunction función de filtro.
     * @param mask máscara de formato.
     */
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

    /**
     * Crea el modelo de columna con visibilidad, enlace e identificador.
     *
     * @param header encabezado.
     * @param property índice del elemento.
     * @param column nombre de la columna.
     * @param visible visibilidad.
     * @param link enlace.
     * @param id identificador.
     */
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
    
    /**
     * Crea el modelo de columna con visibilidad, enlace, identificador y máscara.
     *
     * @param header encabezado.
     * @param property índice del elemento.
     * @param column nombre de la columna.
     * @param visible visibilidad.
     * @param link enlace.
     * @param id identificador.
     * @param mask máscara de formato.
     */
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

    /**
     * Indica si la columna es visible.
     * @return verdadero si es visible.
     */
    @Override
    public Boolean getVisible() {
        return visible;
    }

    /**
     * Establece si la columna es visible.
     * @param visible verdadero para hacerla visible.
     */
    @Override
    public void setVisible(Boolean visible) {
        this.visible = visible;
    }    

    /**
     * Devuelve el identificador de la columna.
     * @return identificador de la columna.
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Establece el identificador de la columna.
     * @param id identificador de la columna.
     */
    @Override
    public void setId(String id) {
        this.id = id;
    }    

    /**
     * Devuelve el enlace (link) asociado a la columna.
     * @return enlace de la columna.
     */
    @Override
    public String getLink() {
        return link;
    }

    /**
     * Establece el enlace (link) asociado a la columna.
     * @param link enlace de la columna.
     */
    @Override
    public void setLink(String link) {
        this.link = link;
    }

    /**
     * Devuelve el estilo CSS de la columna.
     * @return estilo de la columna.
     */
    @Override
    public String getStyle() {
        return style;
    }

    /**
     * Establece el estilo CSS de la columna.
     * @param style estilo de la columna.
     */
    @Override
    public void setStyle(String style) {
        this.style = style;
    }

    /**
     * Devuelve el ancho de la columna.
     * @return ancho de la columna.
     */
    @Override
    public int getWidth() {
        return width;
    }

    /**
     * Establece el ancho de la columna.
     * @param width ancho de la columna.
     */
    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Devuelve la función de filtro de la columna.
     * @return función de filtro.
     */
    @Override
    public String getFilterFunction() {
        return filterFunction;
    }

    /**
     * Establece la función de filtro de la columna.
     * @param filterFunction función de filtro.
     */
    @Override
    public void setFilterFunction(String filterFunction) {
        this.filterFunction = filterFunction;
    }

    /**
     * Devuelve la máscara de formato de la columna.
     * @return máscara de la columna.
     */
    @Override
    public String getMask() {
        return mask;
    }

    /**
     * Establece la máscara de formato de la columna.
     * @param mask máscara de la columna.
     */
    @Override
    public void setMask(String mask) {
        this.mask = mask;
    }

    /**
     * Devuelve el criterio de ordenamiento de la columna.
     * @return criterio de ordenamiento.
     */
    @Override
    public String getOrder() {
        if (Fn.nvl(order, "").isEmpty()){
            order = name;
        }
        return order;
    }

    /**
     * Establece el criterio de ordenamiento de la columna.
     * @param order criterio de ordenamiento.
     */
    @Override
    public void setOrder(String order) {
        this.order = order;
    }

    /**
     * Devuelve el filtro aplicado a la columna.
     * @return filtro de la columna.
     */
    @Override
    public String getFilter() {
        if (Fn.nvl(filter, "").isEmpty()){
            filter = name;
        }
        return filter;
    }

    /**
     * Establece el filtro aplicado a la columna.
     * @param filter filtro de la columna.
     */
    @Override
    public void setFilter(String filter) {
        this.filter = filter;
    }

    /**
     * Indica si la columna es ordenable.
     * @return verdadero si es ordenable.
     */
    @Override
    public Boolean getSortable() {
        return sortable;
    }

    /**
     * Establece si la columna es ordenable.
     * @param sortable verdadero para hacerla ordenable.
     */
    @Override
    public void setSortable(Boolean sortable) {
        this.sortable = sortable;
    }

    /**
     * Indica si la columna es filtrable.
     * @return verdadero si es filtrable.
     */
    @Override
    public Boolean getFilterable() {
        return filterable;
    }

    /**
     * Establece si la columna es filtrable.
     * @param filterable verdadero para hacerla filtrable.
     */
    @Override
    public void setFilterable(Boolean filterable) {
        this.filterable = filterable;
    }

    /**
     * Indica si la columna es exportable.
     * @return verdadero si es exportable.
     */
    @Override
    public Boolean getExportable() {
        return exportable;
    }

    /**
     * Establece si la columna es exportable.
     * @param exportable verdadero para hacerla exportable.
     */
    @Override
    public void setExportable(Boolean exportable) {
        this.exportable = exportable;
    }

    /**
     * Indica si la columna se puede mostrar/ocultar (toggle).
     * @return verdadero si es conmutable.
     */
    @Override
    public Boolean getToggleable() {
        return toggleable;
    }

    /**
     * Establece si la columna se puede mostrar/ocultar (toggle).
     * @param toggleable verdadero para hacerla conmutable.
     */
    @Override
    public void setToggleable(Boolean toggleable) {
        this.toggleable = toggleable;
    }

    /**
     * Devuelve el modo de filtro de la columna (contain, exact, exact_trim...).
     * @return modo de filtro.
     */
    @Override
    public String getFilterMode() {
        return filterMode;
    }

    /**
     * Establece el modo de filtro de la columna.
     * @param filterMode modo de filtro.
     */
    @Override
    public void setFilterMode(String filterMode) {
        this.filterMode = filterMode;
    }

    /**
     * Devuelve la máscara del filtro de la columna (left_blank_10, right_blank_8...).
     * @return máscara del filtro.
     */
    @Override
    public String getFilterMask() {
        return this.filterMask;
    }

    /**
     * Establece la máscara del filtro de la columna.
     * @param filterMask máscara del filtro.
     */
    @Override
    public void setFilterMask(String filterMask) {
        this.filterMask = filterMask;
    }

    /**
     * Devuelve el tipo de editor de la columna (autocomplete, selectonemenu,
     * inputNumber, inputText, outputText).
     * @return tipo de editor.
     */
    @Override
    public String getType() {
        return this.type;
    }

    /**
     * Establece el tipo de editor de la columna.
     * @param type tipo de editor.
     */
    @Override
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Devuelve el título (tooltip) de la columna.
     * @return título de la columna.
     */
    @Override
    public String getTitle() {
        return this.title;
    }

    /**
     * Establece el título (tooltip) de la columna.
     * @param title título de la columna.
     */
    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Devuelve los parámetros de la fuente de datos de la columna (para valores
     * que no están en el modelo o cálculos complejos).
     * @return parámetros de la fuente de datos.
     */
    @Override
    public String getDataSourceParams() {
        return this.dataSourceParams;
    }

    /**
     * Establece los parámetros de la fuente de datos de la columna.
     * @param dataSourceParams parámetros de la fuente de datos.
     */
    @Override
    public void setDataSourceParams(String dataSourceParams) {
        this.dataSourceParams = dataSourceParams;
    }
    
    /**
     * Devuelve un objeto XMLDOM en formato texto.
     *
     * @return objeto XMLDOM en formato texto formateada.
     */
    @Override
    public String toString() {
        return this.getHeader();
    }
}

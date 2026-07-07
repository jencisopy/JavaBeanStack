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

package org.javabeanstack.web.model;

import java.io.Serializable;

/**
 * Modelo de columnas utilizado para manejar dataTables con columnas dinamicas
 * @author mtrinidad
 */ 
public interface IColumnModel extends Serializable {

    /**
     * Devuelve encabezado de la columna
     * @return 
     */
    public String getHeader();

    /**
     * Devuelve el nro de elemento al cual corresponde una columna
     * @return 
     */
    public Integer getProperty();

    /**
     * Establece la cabecera para una columna
     * @param header 
     */
    public void setHeader(String header);
    
    /**
     * Establece el nro de elemento al cual corresponde una columna
     * @param property 
     */
    public void setProperty(Integer property);

    /**
     * Devuelve nombre de la columna, esto cuando se tienen valores fijos y no dinamicos
     * @return 
     */
    public String getName();

    /**
     * Establece valores de la columna, cuando se tienen valores fijos y no dinamicos
     * @param name 
     */
    public void setName(String name);
    public Boolean getVisible();
    public void setVisible(Boolean visible);
    public String getId();
    public void setId(String id);
    public String getLink();
    public void setLink(String link);
    public String getStyle();
    public void setStyle(String style);
    public int getWidth();
    public void setWidth(int width);
    public String getFilterFunction();
    public void setFilterFunction(String filterFunction);
    public String getMask();
    public void setMask(String mask);
    public String getOrder();
    public void setOrder(String order);
    public String getFilter();
    public void setFilter(String filter);
    public Boolean getSortable();
    public void setSortable(Boolean sortable);
    public Boolean getFilterable();
    public void setFilterable(Boolean filterable);
    public Boolean getExportable();
    public void setExportable(Boolean exportable);
    public Boolean getToggleable();
    public void setToggleable(Boolean toggleable);
    public String getFilterMode(); //Valores posibles (contain, contain_ltrim, exact, exact_trim, exact_ltrim)
    public void setFilterMode(String filterMode);
    public String getFilterMask(); //Valores posibles (left_blank_10, right_blank_8)
    public void setFilterMask(String filterMask);
    public String getType(); //Valores posibles (autocomplete, selectonemenu, inputNumber, inputText, outputText)
    public void setType(String columnType);
    public String getTitle(); //Leyenda o tooltip de la columna al pasar el ratón.
    public void setTitle(String columnTitle);
    public String getDataSourceParams(); //Para mostrar valores que no estan en el modelo o son calculos complejos. Ver IDataCollector
    public void setDataSourceParams(String dataSource);
}

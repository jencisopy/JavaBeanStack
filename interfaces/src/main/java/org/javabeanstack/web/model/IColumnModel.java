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
 * Contrato del modelo de columna usado para manejar {@code DataTable} con
 * columnas dinámicas: define encabezado, origen del valor, estilo, y las
 * opciones de orden, filtro, exportación y tipo de editor de cada columna.
 *
 * @author mtrinidad
 */
public interface IColumnModel extends Serializable {

    /**
     * Devuelve el encabezado de la columna.
     * @return encabezado de la columna.
     */
    public String getHeader();

    /**
     * Devuelve el número de elemento (índice) al que corresponde la columna.
     * @return índice del elemento de la columna.
     */
    public Integer getProperty();

    /**
     * Establece el encabezado de la columna.
     * @param header encabezado de la columna.
     */
    public void setHeader(String header);

    /**
     * Establece el número de elemento (índice) al que corresponde la columna.
     * @param property índice del elemento de la columna.
     */
    public void setProperty(Integer property);

    /**
     * Devuelve el nombre de la columna (para valores fijos, no dinámicos).
     * @return nombre de la columna.
     */
    public String getName();

    /**
     * Establece el nombre de la columna (para valores fijos, no dinámicos).
     * @param name nombre de la columna.
     */
    public void setName(String name);

    /**
     * Indica si la columna es visible.
     * @return verdadero si es visible.
     */
    public Boolean getVisible();

    /**
     * Establece si la columna es visible.
     * @param visible verdadero para hacerla visible.
     */
    public void setVisible(Boolean visible);

    /**
     * Devuelve el identificador de la columna.
     * @return identificador de la columna.
     */
    public String getId();

    /**
     * Establece el identificador de la columna.
     * @param id identificador de la columna.
     */
    public void setId(String id);

    /**
     * Devuelve el enlace (link) asociado a la columna.
     * @return enlace de la columna.
     */
    public String getLink();

    /**
     * Establece el enlace (link) asociado a la columna.
     * @param link enlace de la columna.
     */
    public void setLink(String link);

    /**
     * Devuelve el estilo CSS de la columna.
     * @return estilo de la columna.
     */
    public String getStyle();

    /**
     * Establece el estilo CSS de la columna.
     * @param style estilo de la columna.
     */
    public void setStyle(String style);

    /**
     * Devuelve el ancho de la columna.
     * @return ancho de la columna.
     */
    public int getWidth();

    /**
     * Establece el ancho de la columna.
     * @param width ancho de la columna.
     */
    public void setWidth(int width);

    /**
     * Devuelve la función de filtro de la columna.
     * @return función de filtro.
     */
    public String getFilterFunction();

    /**
     * Establece la función de filtro de la columna.
     * @param filterFunction función de filtro.
     */
    public void setFilterFunction(String filterFunction);

    /**
     * Devuelve la máscara de formato de la columna.
     * @return máscara de la columna.
     */
    public String getMask();

    /**
     * Establece la máscara de formato de la columna.
     * @param mask máscara de la columna.
     */
    public void setMask(String mask);

    /**
     * Devuelve el criterio de ordenamiento de la columna.
     * @return criterio de ordenamiento.
     */
    public String getOrder();

    /**
     * Establece el criterio de ordenamiento de la columna.
     * @param order criterio de ordenamiento.
     */
    public void setOrder(String order);

    /**
     * Devuelve el filtro aplicado a la columna.
     * @return filtro de la columna.
     */
    public String getFilter();

    /**
     * Establece el filtro aplicado a la columna.
     * @param filter filtro de la columna.
     */
    public void setFilter(String filter);

    /**
     * Indica si la columna es ordenable.
     * @return verdadero si es ordenable.
     */
    public Boolean getSortable();

    /**
     * Establece si la columna es ordenable.
     * @param sortable verdadero para hacerla ordenable.
     */
    public void setSortable(Boolean sortable);

    /**
     * Indica si la columna es filtrable.
     * @return verdadero si es filtrable.
     */
    public Boolean getFilterable();

    /**
     * Establece si la columna es filtrable.
     * @param filterable verdadero para hacerla filtrable.
     */
    public void setFilterable(Boolean filterable);

    /**
     * Indica si la columna es exportable.
     * @return verdadero si es exportable.
     */
    public Boolean getExportable();

    /**
     * Establece si la columna es exportable.
     * @param exportable verdadero para hacerla exportable.
     */
    public void setExportable(Boolean exportable);

    /**
     * Indica si la columna se puede mostrar/ocultar (toggle).
     * @return verdadero si es conmutable.
     */
    public Boolean getToggleable();

    /**
     * Establece si la columna se puede mostrar/ocultar (toggle).
     * @param toggleable verdadero para hacerla conmutable.
     */
    public void setToggleable(Boolean toggleable);

    /**
     * Devuelve el modo de filtro de la columna (contain, exact, exact_trim...).
     * @return modo de filtro.
     */
    public String getFilterMode(); //Valores posibles (contain, contain_ltrim, exact, exact_trim, exact_ltrim)

    /**
     * Establece el modo de filtro de la columna.
     * @param filterMode modo de filtro.
     */
    public void setFilterMode(String filterMode);

    /**
     * Devuelve la máscara del filtro de la columna (left_blank_10, right_blank_8...).
     * @return máscara del filtro.
     */
    public String getFilterMask(); //Valores posibles (left_blank_10, right_blank_8)

    /**
     * Establece la máscara del filtro de la columna.
     * @param filterMask máscara del filtro.
     */
    public void setFilterMask(String filterMask);

    /**
     * Devuelve el tipo de editor de la columna (autocomplete, selectonemenu,
     * inputNumber, inputText, outputText).
     * @return tipo de editor.
     */
    public String getType(); //Valores posibles (autocomplete, selectonemenu, inputNumber, inputText, outputText)

    /**
     * Establece el tipo de editor de la columna.
     * @param columnType tipo de editor.
     */
    public void setType(String columnType);

    /**
     * Devuelve el título (tooltip) de la columna.
     * @return título de la columna.
     */
    public String getTitle(); //Leyenda o tooltip de la columna al pasar el ratón.

    /**
     * Establece el título (tooltip) de la columna.
     * @param columnTitle título de la columna.
     */
    public void setTitle(String columnTitle);

    /**
     * Devuelve los parámetros de la fuente de datos de la columna (para valores
     * que no están en el modelo o cálculos complejos).
     * @return parámetros de la fuente de datos.
     */
    public String getDataSourceParams(); //Para mostrar valores que no estan en el modelo o son calculos complejos. Ver IDataCollector

    /**
     * Establece los parámetros de la fuente de datos de la columna.
     * @param dataSource parámetros de la fuente de datos.
     */
    public void setDataSourceParams(String dataSource);
}

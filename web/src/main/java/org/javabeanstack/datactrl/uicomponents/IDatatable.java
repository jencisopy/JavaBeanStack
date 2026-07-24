/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*        jenciso@javabeanstack.org
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
package org.javabeanstack.datactrl.uicomponents;

import java.util.List;
import java.util.Map;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.datactrl.IDataObject;
import org.javabeanstack.web.jsf.controller.LazyDataRows;
import org.javabeanstack.web.model.IColumnModel;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.menu.MenuModel;

/**
 *
 * Contrato del componente de UI datatable: expone las filas, la selección, las
 * columnas, los hijos y los eventos de una tabla de datos (PrimeFaces) enlazada
 * a un objeto de datos ({@link org.javabeanstack.datactrl.IDataObject}).
 *
 * @author Jorge Enciso
 * @param <O>
 * @param <T>
 */
public interface IDatatable <O extends IDataObject,T extends IDataRow>{
    /**
     * Devuelve la cantidad de filas por grilla.
     *
     * @return filas por grilla.
     */
    Integer getRowsPerGrid();
    /**
     * Asigna la cantidad de filas por grilla.
     *
     * @param rowsPerGrid filas por grilla.
     */
    void setRowsPerGrid(Integer rowsPerGrid);
    
    /**
     * Devuelve el identificador del datatable.
     *
     * @return identificador.
     */
    String getId();
    /**
     * Asigna el identificador del datatable.
     *
     * @param id identificador.
     */
    void setId(String id);
    
    /**
     * Devuelve el conjunto XML de configuración del datatable.
     *
     * @return conjunto XML.
     */
    String getXmlSet();
    /**
     * Asigna el conjunto XML de configuración del datatable.
     *
     * @param xmlSet conjunto XML.
     */
    void setXmlSet(String xmlSet);
    
    /**
     * Devuelve el objeto de datos (contexto) del datatable.
     *
     * @return objeto de datos.
     */
    O getContext();
    /**
     * Asigna el objeto de datos (contexto) del datatable.
     *
     * @param context objeto de datos.
     */
    void setContext(O context);
    
    /**
     * Devuelve los datatables hijos.
     *
     * @param <K> tipo de los hijos.
     * @return mapa de datatables hijos.
     */
    <K extends IDatatable> Map<String, K> getChildren();
    /**
     * Agrega un datatable hijo.
     *
     * @param <K> tipo del hijo.
     * @param child datatable hijo.
     */
    <K extends IDatatable> void setChild(K child);
    /**
     * Refresca el datatable hijo indicado.
     *
     * @param id identificador del hijo.
     */
    void refreshChild(String id);
    /**
     * Refresca todos los datatables hijos.
     */
    void refreshChildren();
    
    /**
     * Refresca el datatable.
     */
    void refresh();
    
    /**
     * Devuelve una propiedad del datatable.
     *
     * @param key clave de la propiedad.
     * @return valor de la propiedad.
     */
    Object getProperty(String key);
    /**
     * Asigna una propiedad del datatable.
     *
     * @param key clave.
     * @param value valor.
     */
    void setProperty(String key, Object value);

    /**
     * Devuelve el título de la vista de datos actual.
     *
     * @return título de la vista.
     */
    String getDataViewTitle();
    /**
     * Devuelve el título de la vista de datos indicada.
     *
     * @param view identificador de la vista.
     * @return título de la vista.
     */
    String getDataViewTitle(String view);
    
    /**
     * Devuelve las filas de datos del datatable.
     *
     * @return lista de filas.
     */
    List<T> getDataRows();
    /**
     * Devuelve el modelo de carga lazy de filas.
     *
     * @return modelo lazy.
     */
    LazyDataRows<T> getLazyDataRows();
    /**
     * Devuelve las filas seleccionadas.
     *
     * @return filas seleccionadas.
     */
    T[] getRowsSelected();
    /**
     * Devuelve la fila seleccionada.
     *
     * @return fila seleccionada.
     */
    T getRowSelected();
    /**
     * Devuelve las filas filtradas.
     *
     * @return filas filtradas.
     */
    List<T> getRowsFiltered();    
    /**
     * Devuelve la acción en curso sobre la fila.
     *
     * @return código de acción.
     */
    int getRowAction();    
    
    /**
     * Asigna las filas seleccionadas.
     *
     * @param rowsSelected filas seleccionadas.
     */
    void setRowsSelected(T[] rowsSelected);    
    /**
     * Asigna la fila seleccionada.
     *
     * @param rowSelected fila seleccionada.
     */
    void setRowSelected(T rowSelected);
    /**
     * Asigna las filas filtradas.
     *
     * @param rowsFiltered filas filtradas.
     */
    void setRowsFiltered(List<T> rowsFiltered);   
    /**
     * Asigna el recolector de datos externo del datatable.
     *
     * @param dataCollector recolector de datos.
     */
    void setDataCollector(IDataCollector dataCollector);
    
    /**
     * Devuelve la vista de formulario seleccionada.
     *
     * @return vista seleccionada.
     */
    String getFormViewSelected();    
    /**
     * Asigna la vista de formulario seleccionada.
     *
     * @param formView vista de formulario.
     */
    void setFormViewSelected(String formView);
    /**
     * Devuelve las columnas por vista de formulario.
     *
     * @return mapa vista → columnas.
     */
    Map<String, List<IColumnModel>> getFormViewsColumns();    
    
    /**
     * Devuelve los campos editables.
     *
     * @return campos editables.
     */
    String[] getEditables();
    /**
     * Asigna los campos editables.
     *
     * @param editables campos editables.
     */
    void setEditables(String[] editables);

    /**
     * Devuelve las columnas del datatable.
     *
     * @return lista de columnas.
     */
    List<IColumnModel> getColumns();
    /**
     * Devuelve las columnas de la vista de formulario indicada.
     *
     * @param formView vista de formulario.
     * @return lista de columnas.
     */
    List<IColumnModel> getColumns(String formView);
    /**
     * Devuelve el estilo CSS de una celda.
     *
     * @param row fila.
     * @param columnName nombre de la columna.
     * @return estilo CSS.
     */
    String getColumnStyle(Object row, String columnName);
    /**
     * Devuelve el enlace (href) de una celda.
     *
     * @param row fila.
     * @param column columna.
     * @return enlace.
     */
    String getColumnHref(Object row, IColumnModel column);
    /**
     * Devuelve el título (tooltip) de una celda.
     *
     * @param row fila.
     * @param column columna.
     * @return título.
     */
    String getColumnTitle(Object row, IColumnModel column);
    /**
     * Devuelve el valor de una celda aplicando su máscara de formato.
     *
     * @param row fila.
     * @param column columna.
     * @return valor formateado.
     */
    String getColumnValueWithMask(Object row, IColumnModel column);
    /**
     * Aplica una máscara de formato a un valor.
     *
     * @param value valor.
     * @param mask máscara.
     * @return valor formateado.
     */
    String getMask(Object value, String mask);

    /**
     * Indica si un campo de una fila es editable.
     *
     * @param row fila.
     * @param fieldName nombre del campo.
     * @return verdadero si es editable.
     */
    boolean isAllowEditField(Object row, String fieldName);
    
    /**
     * Se ejecuta antes de seleccionar una fila.
     *
     * @param event evento de selección.
     */
    void beforeRowSelect(SelectEvent event);
    /**
     * Se ejecuta al seleccionar una fila.
     *
     * @param event evento de selección.
     */
    void onRowSelect(SelectEvent event);
    /**
     * Se ejecuta después de seleccionar una fila.
     *
     * @param event evento de selección.
     */
    void afterRowSelect(SelectEvent event);
    /**
     * Se ejecuta al cambiar la vista de columnas.
     *
     * @param formName nombre del formulario.
     * @param viewName nombre de la vista.
     */
    void onColumnSetView(String formName, String viewName);    
    /**
     * Se ejecuta al reordenar las columnas.
     *
     * @param event evento de reordenamiento.
     */
    void onColumnReorder(Object event);
    /**
     * Se ejecuta al mostrar/ocultar una columna.
     *
     * @param pToggleEvent evento de conmutación.
     */
    void onColumnToggle(Object pToggleEvent);
    /**
     * Se ejecuta al cambiar el valor de un campo de una fila.
     *
     * @param row fila.
     * @param fieldName nombre del campo.
     */
    void onChange(Object row, String fieldName);
    
    /**
     * Devuelve el recolector de datos externo del datatable.
     *
     * @return recolector de datos.
     */
    IDataCollector getDataCollector();
    /**
     * Devuelve la plantilla de la barra de botones de la tabla indicada.
     *
     * @param table identificador de la tabla.
     * @return plantilla de la barra de botones.
     */
    String getTableButtonsBarTemplate(String table);
    /**
     * Devuelve la plantilla de la barra de botones.
     *
     * @return plantilla de la barra de botones.
     */
    String getTableButtonsBarTemplate();
    /**
     * Devuelve la plantilla del reporte de página actual de la tabla indicada.
     *
     * @param table identificador de la tabla.
     * @return plantilla del reporte de página.
     */
    String getTableCurrentPageReportTemplate(String table);
    /**
     * Devuelve la plantilla del reporte de página actual.
     *
     * @return plantilla del reporte de página.
     */
    String getTableCurrentPageReportTemplate();
    /**
     * Devuelve la plantilla del paginador de la tabla indicada.
     *
     * @param table identificador de la tabla.
     * @return plantilla del paginador.
     */
    String getTablePaginatorTemplate(String table);
    /**
     * Devuelve la plantilla del paginador.
     *
     * @return plantilla del paginador.
     */
    String getTablePaginatorTemplate();
    
    /**
     * Devuelve el modelo de menú de filtros de la tabla indicada.
     *
     * @param table identificador de la tabla.
     * @return modelo de menú de filtros.
     */
    MenuModel getMenuFilterModel(String table);
    /**
     * Construye el menú de filtros de la tabla indicada.
     *
     * @param table identificador de la tabla.
     */
    void createMenuFilter(String table);
    
    /**
     * Indica si la acción está permitida sobre la tabla indicada.
     *
     * @param table identificador de la tabla.
     * @param action acción.
     * @return verdadero si está permitida.
     */
    boolean isAllowTableAction(String table, String action);
    /**
     * Indica si la acción está permitida sobre la tabla principal.
     *
     * @param action acción.
     * @return verdadero si está permitida.
     */
    boolean isAllowTableAction(String action);
    /**
     * Indica si la tabla indicada usa carga lazy.
     *
     * @param table identificador de la tabla.
     * @return verdadero si es lazy.
     */
    boolean isTableLazy(String table);
    /**
     * Indica si la tabla principal usa carga lazy.
     *
     * @return verdadero si es lazy.
     */
    boolean isTableLazy();
    /**
     * Indica si la tabla indicada es visible.
     *
     * @param table identificador de la tabla.
     * @return verdadero si es visible.
     */
    boolean isTableVisible(String table);
    /**
     * Indica si la tabla principal es visible.
     *
     * @return verdadero si es visible.
     */
    boolean isTableVisible();
    /**
     * Indica si hay tablas visibles.
     *
     * @return verdadero si hay tablas visibles.
     */
    boolean isTablesVisible();
    
    /**
     * Define la visibilidad de una tabla.
     *
     * @param table identificador de la tabla.
     * @param visible visibilidad.
     */
    void setTableVisible(String table, boolean visible);
    
    /**
     * Ejecuta una acción sobre el datatable.
     *
     * @param action acción a ejecutar.
     * @return verdadero si tuvo éxito.
     */
    boolean doAction(String action);
    /**
     * Revierte los cambios no confirmados del datatable.
     *
     * @return verdadero si tuvo éxito.
     */
    boolean revert();
    
    /**
     * Reinicia el estado del datatable.
     */
    void reset();
}


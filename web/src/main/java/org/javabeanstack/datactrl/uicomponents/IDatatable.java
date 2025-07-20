/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2018 Jorge Enciso
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
 * @author Jorge Enciso
 * @param <O>
 * @param <T>
 */
public interface IDatatable <O extends IDataObject,T extends IDataRow>{
    Integer getRowsPerGrid();
    void setRowsPerGrid(Integer rowsPerGrid);
    
    String getId();
    void setId(String id);
    
    String getXmlSet();
    void setXmlSet(String xmlSet);
    
    O getContext();
    void setContext(O context);
    
    <K extends IDatatable> Map<String, K> getChildren();
    <K extends IDatatable> void setChild(K child);
    void refreshChild(String id);
    void refreshChildren();
    
    void refresh();
    
    Object getProperty(String key);
    void setProperty(String key, Object value);

    String getDataViewTitle();
    String getDataViewTitle(String view);
    
    List<T> getDataRows();
    LazyDataRows<T> getLazyDataRows();
    T[] getRowsSelected();
    T getRowSelected();
    List<T> getRowsFiltered();    
    int getRowAction();    
    
    void setRowsSelected(T[] rowsSelected);    
    void setRowSelected(T rowSelected);
    void setRowsFiltered(List<T> rowsFiltered);   
    void setDataCollector(IDataCollector dataCollector);
    
    String getFormViewSelected();    
    void setFormViewSelected(String formView);
    Map<String, List<IColumnModel>> getFormViewsColumns();    
    
    String[] getEditables();
    void setEditables(String[] editables);

    List<IColumnModel> getColumns();
    List<IColumnModel> getColumns(String formView);
    String getColumnStyle(Object row, String columnName);
    String getColumnHref(Object row, IColumnModel column);
    String getColumnTitle(Object row, IColumnModel column);
    String getColumnValueWithMask(Object row, IColumnModel column);
    String getMask(Object value, String mask);

    boolean isAllowEditField(Object row, String fieldName);
    
    void beforeRowSelect(SelectEvent event);
    void onRowSelect(SelectEvent event);
    void afterRowSelect(SelectEvent event);
    void onColumnSetView(String formName, String viewName);    
    void onColumnReorder(Object event);
    void onColumnToggle(Object pToggleEvent);
    void onChange(Object row, String fieldName);
    
    IDataCollector getDataCollector();
    String getTableButtonsBarTemplate(String table);
    String getTableButtonsBarTemplate();
    String getTableCurrentPageReportTemplate(String table);
    String getTableCurrentPageReportTemplate();
    String getTablePaginatorTemplate(String table);
    String getTablePaginatorTemplate();
    
    MenuModel getMenuFilterModel(String table);
    void createMenuFilter(String table);
    
    boolean isAllowTableAction(String table, String action);
    boolean isAllowTableAction(String action);
    boolean isTableLazy(String table);
    boolean isTableLazy();
    boolean isTableVisible(String table);
    boolean isTableVisible();
    boolean isTablesVisible();
    
    void setTableVisible(String table, boolean visible);
    
    boolean doAction(String action);
    boolean revert();
    
    void reset();
}


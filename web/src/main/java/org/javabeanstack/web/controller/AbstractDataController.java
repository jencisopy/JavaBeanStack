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

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIColumn;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleEvent;
import org.primefaces.model.Visibility;

import org.javabeanstack.datactrl.AbstractDataObject;

import org.javabeanstack.data.IDataRow;
import org.javabeanstack.data.IDataSet;
import org.javabeanstack.security.IUserSession;
import org.javabeanstack.util.Fn;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.web.model.ColumnModel;


/**
 *
 * @author Jorge Enciso
 * @param <T>
 */
public abstract class AbstractDataController<T extends IDataRow> extends AbstractDataObject<T> {
    private T[] rowsSelected;
    private T   rowSelected;    
    private List<T> rowsFiltered;
    private List<UIColumn> tableUIColumns;
    private LazyDataRows<T> lazyDataRows;
    private FacesContextUtil facesCtx=new FacesContextUtil();
    private Object id;
    private String action="";
    private Boolean noLazyRowsLoad = false;
    private String refreshUIComponent;
    private String formViewSelected = "DEFAULT";

    public AbstractDataController(){
    }  
    
    public AbstractDataController(Class<T> type){
        this.setType(type);
    }

    public String getAction(){
        return action;
    }
    
    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }
    
    public FacesContextUtil getFacesCtx() {
        if (facesCtx == null){
            facesCtx = new FacesContextUtil();
        }
        return facesCtx;
    }

    public Boolean getNoLazyRowsLoad() {
        return noLazyRowsLoad;
    }

    public void setNoLazyRowsLoad(Boolean noLazyRowsLoad) {
        this.noLazyRowsLoad = noLazyRowsLoad;
    }

    
    public LazyDataRows<T> getLazyDataRows() {
        if (lazyDataRows == null){
             lazyDataRows = new LazyDataRows(this);
        }
        return lazyDataRows;
    }

    public void setLazyDataRows(LazyDataRows<T> lazyDataRows) {
        this.lazyDataRows = lazyDataRows;
    }

    
    public T getRowSelected() {
        return rowSelected;
    }
    
    public void setRowSelected(T rowSelected) {
        try {
            if (rowSelected == null){
                this.rowSelected = null;
            }
            else{
                this.rowSelected = (T)rowSelected;                
            }
        } catch (Exception ex) {
            ErrorManager.showError(ex, Logger.getLogger(this.getClass()));
        }
    }
    
    public T[] getRowsSelected() {
        return rowsSelected;
    }

    public void setRowsSelected(T[] rowsSelected) {
        this.rowsSelected = rowsSelected;
    }
    
    public List<T> getRowsFiltered() {
        return rowsFiltered;
    }

    public void setRowsFiltered(List<T> rowsFiltered) {
        this.rowsFiltered = rowsFiltered;
    }

    public Integer getRowsCount() {
        if (lazyDataRows != null){
            return lazyDataRows.getRowCount();
        }        
        else if (rowsFiltered == null) {
            return getDataRows().size();
        } 
        return rowsFiltered.size();
    }
    
    public List<UIColumn> getTableUIColumns() {
        return tableUIColumns;
    }

    public void setTableUIColumns(List<UIColumn> tableUIColumns) {
        this.tableUIColumns = tableUIColumns;
    }

    public String getRefreshUIComponent() {
        return refreshUIComponent;
    }

    public void setRefreshUIComponent(String refreshUIComponent) {
        this.refreshUIComponent = refreshUIComponent;
    }

    public String getFormViewSelected() {
        return formViewSelected;
    }

    public void setFormViewSelected(String formViewSelected) {
        this.formViewSelected = formViewSelected;
    }

    public IUserSession getUserSession() {
        return facesCtx.getUserSession();
    }
    
    public Long getUserId() {
        return facesCtx.getUserId();
    }

    protected String getWebFileName(){
        String uri = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest()).getRequestURI();
        int indexat = uri.lastIndexOf("/");
        uri = uri.substring(indexat + 1).toLowerCase();
        return uri;
    }
    
    public void onRowSelect(SelectEvent event){
        int recno = this.getDataRows().indexOf((T)event.getObject());
        if (this.goTo(recno)){
            this.rowSelected = getRow();                        
        }
    }

    /**
     * Se ejecuta posterior a la ejecución del filtrado de la grilla 
     */
    public void onRowFilter() {
        // TODO revisar este codigo
        UIComponent text = facesCtx.findComponent(":f_list:dt_list:outputTextFooter");
        if (lazyDataRows != null){
            text.getAttributes().put("value", lazyDataRows.getRowCount());
        }
        else if (rowsFiltered == null) {
            text.getAttributes().put("value", getDataRows().size());
        } else {
            text.getAttributes().put("value", rowsFiltered.size());
        }
        FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("f_list:dt_list:outputTextFooter");
    }
    
    /**
     * Se ejecuta al mover una columna de la grilla.
     * @param event 
     */
    public void onColReorder(javax.faces.event.AjaxBehaviorEvent event) {
        tableUIColumns = new ArrayList<>();
        DataTable table = (DataTable) event.getSource();
        tableUIColumns.clear();
        for (org.primefaces.component.api.UIColumn column : table.getColumns()) {
            UIComponent colComponent = (UIComponent) column;
            tableUIColumns.add((UIColumn) colComponent);
        }
    }

    public void onColumnToggle(ToggleEvent pToggleEvent) {
        Map<String, String> params = facesCtx.getRequestParameterMap();

        String nameComponent = params.get("nameComponent");

        Boolean visible = pToggleEvent.getVisibility() == Visibility.VISIBLE;
        Integer index = (Integer) pToggleEvent.getData();

        UIComponent table = facesCtx.findComponent(nameComponent);
        if (tableUIColumns == null) {
            table.getChildren().get(index).getAttributes().put("exportable", visible);
        } else {
            String idcolumn;
            idcolumn = tableUIColumns.get(index).getAttributes().get("id").toString();
            if (idcolumn != null) {
                table.findComponent(idcolumn).getAttributes().put("exportable", visible);
            }
        }
    }

    public List<T> onCompleteText(String query) {
        if (query.isEmpty()) {
            this.setFilter("");
        } else {
            this.setFilter(" nombre like '%" + query + "%' or codigo like '%" + query + "%'");
        }
        this.requery();
        List<T> rows = this.getDataRows();
        return rows;
    }
    
    protected void initAction(String operation, boolean success) {
        if (success){
            if (Fn.inList(operation.toLowerCase(), "1","agregar")){
                operation = "insert";
            }
            if (Fn.inList(operation.toLowerCase(), "2","modificar")){
                operation = "update";
            }
            if ("consult".equals(operation.toLowerCase())){
                operation = "view";
            }
            action = operation;     
            noLazyRowsLoad = true;
        }
    }    
    
    public boolean doAction(String operation) {
        boolean result = true;
        // Si no es agregar nuevo registro refrescar el registro actual
        if (!Fn.inList(operation.toLowerCase(),"insert","agregar","1")){
            refreshRow();
        }
        switch (operation.toLowerCase()) {
            case "1":            
            case "insert":
            case "agregar":                
                result = this.allowOperation(IDataRow.AGREGAR);
                if (result){
                    rowsSelected=null;
                    result = insertRow();
                }
                break;
            case "2":
            case "update":
            case "modificar":                
                result = this.allowOperation(IDataRow.MODIFICAR);
                break;
            case "3":                
            case "delete":
            case "borrar":                
                result = this.allowOperation(IDataRow.BORRAR);
                break;
            case "consult":
            case "view":
            case "read":
                result = this.allowOperation(IDataRow.CONSULTAR);
                break;
        }
        if (getErrorApp() != null){
            facesCtx.showError("Error",getErrorApp().getMessage());
            result = false;
        }
        else if (!result) {
            // TODO mostrar mensaje            
        }
        initAction(operation, result);        
        facesCtx.addCallbackParam("result", result);
        return result;
    }

        
    public boolean allowEditField(String fieldName){
        if (getRow().getAction() != IDataRow.AGREGAR && 
                getRow().getAction() != IDataRow.MODIFICAR){
            return false;
        }
        return true;
    }

    public void onChange(String fieldname){
    }

    public void refresh(String fieldname){
    }
    
    public boolean update(){
        boolean success = super.update(false);
        facesCtx.addCallbackParam("result", success);
        if (getErrorApp() != null){
            facesCtx.showError("Error",getErrorApp().getMessage());
        }
        else if (!success){
            facesCtx.showError("Error",this.getErrorMsg(true));
        }
        // Esto con el fin de que no se ejecute el metodo load del lazydatarows
        // en caso de que se actualize un dataTable
        getFacesCtx().setAttribute("nolazyload",Boolean.TRUE);        
        if (success){
            //Renderizar componentes
            refreshUIComponent();            
        }
        afterAction(success);          
        // Devolver resultado de la grabación        
        return success;
    }

    @Override
    public boolean update(IDataSet dataSet){
        boolean success = super.update(dataSet);        
        facesCtx.addCallbackParam("result", success);
        if (getErrorApp() != null){
            facesCtx.showError("Error",getErrorApp().getMessage());
        }
        else if (!success){
            facesCtx.showError("Error",this.getErrorMsg(true));
        }
        // Esto con el fin de que no se ejecute el metodo load del lazydatarows
        // en caso de que se actualize un dataTable
        getFacesCtx().setAttribute("nolazyload",Boolean.TRUE);        
        if (success){
            //Renderizar componentes
            refreshUIComponent();            
        }
        afterAction(success);                
        // Devolver resultado de la grabación                
        return success;
    }

    
    @Override
    public boolean revert(){
        boolean success = super.revert();        
        facesCtx.addCallbackParam("result", true);
        if (getErrorApp() != null){
            facesCtx.showError("Error",getErrorApp().getMessage());
        }
        else if (!success){
            facesCtx.showError("Error",this.getErrorMsg(true));
        }
        // Esto con el fin de que no se ejecute el metodo load del lazydatarows
        // en caso de que se actualize un dataTable
        getFacesCtx().setAttribute("nolazyload",Boolean.TRUE);        
        //Renderizar componentes
        refreshUIComponent();            
        afterAction(success);                
        // Devolver resultado de la reversión
        return success;
    }
    
    protected void refreshUIComponent(){
        if (!Strings.isNullOrEmpty(refreshUIComponent)){
            getFacesCtx().refreshView(refreshUIComponent);            
        }
    }

    protected void refreshUIComponent(String refresh){
        getFacesCtx().refreshView(refresh);            
    }

    protected void afterAction(boolean success) {
        if (success){
            action = "";
            noLazyRowsLoad = false;
        }
    }   


    protected void loadViewColumns() {    
    }
    
    public List<ColumnModel> getColumns() { 
        return new ArrayList();
    }
    
    public String logout() {
        return facesCtx.logout();
    }
    
}

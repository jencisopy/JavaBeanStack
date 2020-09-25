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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.faces.component.UIColumn;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

import org.primefaces.PrimeFaces;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleEvent;
import org.primefaces.model.Visibility;

import org.javabeanstack.datactrl.AbstractDataObject;
import org.javabeanstack.web.util.FacesContextUtil;

import org.javabeanstack.data.IDataRow;
import org.javabeanstack.data.IDataSet;
import org.javabeanstack.datactrl.IDataObject;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.util.Fn;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.util.Strings;
import org.javabeanstack.web.model.IColumnModel;
import org.javabeanstack.xml.IXmlDom;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.javabeanstack.events.ICtrlEvents;
import org.javabeanstack.util.Dates;
import org.javabeanstack.util.LocalDates;

/**
 * Controller para los ABMs de las tablas, hereda funcionalidades de
 * AbstractDataObject
 *
 * @author Jorge Enciso
 * @param <T>
 */
public abstract class AbstractDataController<T extends IDataRow> extends AbstractDataObject<T> {

    /**
     * Lista de columnas de la grilla con los datos
     */
    private List<UIColumn> tableUIColumns;
    /**
     * Lista de registros de la selección de datos en un proceso que trae los
     * datos por bloque (pagina a pagina)
     */
    private LazyDataRows<T> lazyDataRows;
    /**
     * Guarda los registros seleccionados en el UIDataTable o grilla de datos
     */
    private T[] rowsSelected;
    /**
     * Se asigna cuando se selecciona un registro en la grilla de datos
     */
    private T rowSelected;
    private Integer recnoIndex;
    /**
     * Registros resultante de la aplicación de un proceso de filtrado
     */
    private List<T> rowsFiltered;
    /**
     * El contexto del servet jsf
     */
    private FacesContextUtil facesCtx = new FacesContextUtil();
    /**
     * identificador del registro de la tabla normalmente se pasa desde otro
     * proceso para realizar una operación sobre el mismo
     */
    private Object id;
    /**
     * Operación a ejecutar, (agregar, modificar, borrar, consultar etc)
     */
    private String action = "";

    private Boolean noLazyRowsLoad = false;
    private String refreshUIComponent;
    private String formViewSelected = "VIEW";
    private Boolean tableDetailShow = false;
    private ICtrlEvents ctrlEvents = new CtrlEventLocal();

    /**
     * Lista de campos de busquedas los cuales serán parte del filtro en el
     * metodo onCompleteText
     */
    private final Map<String, String> completeTextSearchFields = new TreeMap(String.CASE_INSENSITIVE_ORDER);

    // TODO Redefinir en las clases hijas
    private String tableTextFooter = ":f_list:dt_list:outputTextFooter";

    public AbstractDataController() {
    }

    public AbstractDataController(Class<T> type) {
        this.setType(type);
    }

    public ICtrlEvents getCtrlEvents() {
        return ctrlEvents;
    }

    public void setCtrlEvents(ICtrlEvents ctrlEvents) {
        this.ctrlEvents = ctrlEvents;
    }

    /**
     * Acción a realizar o en ejecución, (agregar, modificar, borrar, consultar
     * etc)
     *
     * @return valor de action (agregar, modificar, borrar, consultar etc)
     */
    public String getAction() {
        return action;
    }

    public int getRowAction() {
        if (getRow() == null) {
            return 0;
        }
        return getRow().getAction();
    }

    /**
     * Devuelve el identificador del registro de la tabla normalmente se pasa
     * desde otro proceso para realizar una operación sobre el mismo
     *
     * @return identificador del registro de la tabla
     */
    public Object getId() {
        return id;
    }

    /**
     * Asigna el identificador del registro de la tabla para realizar alguna
     * operación sobre el registro
     *
     * @param id el identificador del registro de la tabla
     */
    public void setId(Object id) {
        this.id = id;
    }

    /**
     * Devuelve la instancia de FacesContext
     *
     * @return la instancia de FacesContext
     */
    public FacesContextUtil getFacesCtx() {
        if (facesCtx == null) {
            facesCtx = new FacesContextUtil();
        }
        return facesCtx;
    }

    protected Boolean getNoLazyRowsLoad() {
        return noLazyRowsLoad;
    }

    protected void setNoLazyRowsLoad(Boolean noLazyRowsLoad) {
        this.noLazyRowsLoad = noLazyRowsLoad;
    }

    /**
     * Devuelve una lista de Registros de la selección de datos en un proceso
     * que trae registros por bloque (pagina a pagina)
     *
     * @return
     */
    public LazyDataRows<T> getLazyDataRows() {
        if (lazyDataRows == null) {
            lazyDataRows = new LazyDataRows(this);
        }
        return lazyDataRows;
    }

    /**
     * Asigna la lista de registros
     *
     * @param lazyDataRows
     */
    public void setLazyDataRows(LazyDataRows<T> lazyDataRows) {
        this.lazyDataRows = lazyDataRows;
    }

    /**
     * Devuelve el registro seleccionado en la grilla que originalmente fue
     * asignado en el metodo onRowSelected()
     *
     * @return registro seleccionado en la grilla.
     */
    public T getRowSelected() {
        return rowSelected;
    }

    /**
     * Asigna en forma manual el atributo rowSelected.
     *
     * @param rowSelected
     */
    public void setRowSelected(T rowSelected) {
        try {
            if (rowSelected == null) {
                this.rowSelected = null;
            } else {
                this.rowSelected = (T) rowSelected;
            }
        } catch (Exception ex) {
            ErrorManager.showError(ex, Logger.getLogger(this.getClass()));
        }
    }

    /**
     * Asigna en forma manual el atributo rowSelected de otro controller
     * relacionado
     *
     * @param bean controller relacionado
     */
    public void setBeanRowSelected(AbstractDataController bean) {
        if (bean == null) {
            return;
        }
        if (bean.getRow() != null) {
            bean.setRowSelected(bean.getRow());
        } else {
            bean.setRowSelected(null);
        }
    }

    /**
     * Devuelve los registros seleccionados en la grilla
     *
     * @return registros seleccionados en la grilla.
     */
    public T[] getRowsSelected() {
        return rowsSelected;
    }

    /**
     * Asigna manualmente los registros seleccionados
     *
     * @param rowsSelected
     */
    public void setRowsSelected(T[] rowsSelected) {
        this.rowsSelected = rowsSelected;
    }

    /**
     * Devuelve los Registros que fuerón objeto de un proceso de filtrado en la
     * grilla
     *
     * @return registros filtrados.
     */
    public List<T> getRowsFiltered() {
        return rowsFiltered;
    }

    /**
     * Asigna la lista de registros filtrados.
     *
     * @param rowsFiltered
     */
    public void setRowsFiltered(List<T> rowsFiltered) {
        this.rowsFiltered = rowsFiltered;
    }

    /**
     * Devuelve la cantidad total de registros
     *
     * @return cantidad total de registros de la sentencia
     */
    public Integer getRowsCount() {
        if (lazyDataRows != null) {
            return lazyDataRows.getRowCount();
        } else if (rowsFiltered == null) {
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

    /**
     * Devuelve el nombre del archivo xhtml linkeado a este controller
     *
     * @return archivo xhtml linkeado a este controller
     */
    public String getWebFileName() {
        String uri = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest()).getRequestURI();
        int indexat = uri.lastIndexOf("/");
        uri = uri.substring(indexat + 1).toLowerCase();
        return uri;
    }

    public Map<String, String> getCompleteTextSearchFields() {
        return completeTextSearchFields;
    }

    public String getTableTextFooter() {
        return tableTextFooter;
    }

    public void setTableTextFooter(String tableTextFooter) {
        this.tableTextFooter = tableTextFooter;
    }

    public Boolean getTableDetailShow() {
        return tableDetailShow;
    }

    public void setTableDetailShow(Boolean tableDetailShow) {
        this.tableDetailShow = tableDetailShow;
    }

    /**
     * Se ejecuta al seleccionar un registro en la grilla
     *
     * @param event
     */
    public void onRowSelect(SelectEvent event) {
        if (ctrlEvents != null) {
            ctrlEvents.onRowSelect(this, event);
        }
    }

    /**
     * Se ejecuta posterior a la ejecución del filtrado de la grilla
     */
    public void onRowFilter() {
        if (ctrlEvents != null) {
            ctrlEvents.onRowFilter(this);
        }
    }

    /**
     * Se ejecuta al mover una columna de la grilla.
     *
     * @param event
     */
    public void onColumnReorder(javax.faces.event.AjaxBehaviorEvent event) {
        if (ctrlEvents != null) {
            ctrlEvents.onColumnReorder(this, event);
        }
    }

    /**
     * Se ejecuta al colocar visible o invisible una columna
     *
     * @param pToggleEvent
     */
    public void onColumnToggle(ToggleEvent pToggleEvent) {
        if (ctrlEvents != null) {
            ctrlEvents.onColumnToggle(this, pToggleEvent);
        }
    }

    /**
     * Se ejecuta en el evento completetext de algunos controles.
     *
     * @param text valor introducido.
     * @return lista de registros que cumplen la condición del filtro.
     */
    public List<T> onCompleteText(String text) {
        if (ctrlEvents != null) {
            return ctrlEvents.onCompleteText(this, text);
        }
        return null;
    }

    /**
     * Agrega campos a la lista de busquedas las cuales serán parte del filtro.
     *
     * @param field campo
     */
    public void addCompleteFieldSearch(String field) {
        addCompleteFieldSearch(field, "like");
    }

    /**
     * Agrega campos a la lista de busquedas las cuales serán parte del filtro.
     *
     * @param field campo
     * @param operator operador ("like","=")
     */
    public void addCompleteFieldSearch(String field, String operator) {
        this.completeTextSearchFields.put(field, operator);
    }

    /**
     * Su función es crear el filtro que se utilizado en la selección de datos.
     *
     * @param text dato introducido.
     * @return filtro que se utilizará en la selección de datos en el where.
     */
    protected String getCompleteTextFilter(String text) {
        String filter = "";
        String separador = "";
        for (Map.Entry<String, String> entry : completeTextSearchFields.entrySet()) {
            if ("like".equalsIgnoreCase(entry.getValue().trim())) {
                filter += separador + "LOWER(" + entry.getKey() + ")" + " ";
                filter += entry.getValue() + " '%" + text.trim().toLowerCase() + "%' ";
            } else {
                filter += separador + entry.getKey() + " " + entry.getValue() + "'" + text + "'";
            }
            separador = " or ";
        }
        return filter;
    }

    /**
     * Se ejecuta en doAction al iniciar cualquier operación (edición, borrado,
     * altas)
     *
     * @param operation operación ("agregar","modificar","consultar" etc)
     * @param success
     */
    protected void initAction(String operation, boolean success) {
        if (success) {
            if (Fn.inList(operation.toLowerCase(), "1", "agregar")) {
                operation = "insert";
            }
            if (Fn.inList(operation.toLowerCase(), "2", "modificar")) {
                operation = "update";
            }
            if ("consult".equals(operation.toLowerCase())) {
                operation = "view";
            }
            action = operation;
            noLazyRowsLoad = true;
        }
    }

    /**
     * Se ejecuta posterior a presionar sobre un boton de agregar, editar,
     * borrar, consultar y otros.
     *
     * @param operation
     * @return verdadero si se le permite (tiene permiso) realizar la operación
     * deseada
     */
    public boolean doAction(String operation) {
        boolean result = true;
        // Si no es agregar nuevo registro refrescar el registro actual
        if (!Fn.inList(operation.toLowerCase(), "insert", "agregar", "1")) {
            refreshRow();
        }
        switch (operation.toLowerCase()) {
            case "1":
            case "insert":
            case "agregar":
                result = this.allowOperation(IDataRow.AGREGAR);
                if (result) {
                    rowsSelected = null;
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
            case "consultar":
            case "consult":
            case "view":
            case "read":
                result = this.allowOperation(IDataRow.CONSULTAR);
                break;
        }
        if (getErrorApp() != null) {
            facesCtx.showError("Error", getErrorApp().getMessage());
            result = false;
        } else if (!result) {
            // TODO mostrar mensaje            
        }
        initAction(operation, result);
        facesCtx.addCallbackParam("result", result);
        return result;
    }

    /**
     * Retorna una bandera comunicando a la vista que habilite o desabilite la
     * edición de datos de un control.
     *
     * @param fieldName campo
     * @return verdadero o falso para habilitar o desabilitar un control de
     * datos.
     */
    public boolean allowEditField(String fieldName) {
        if (getRow() == null) {
            return false;
        }
        return !(getRow().getAction() != IDataRow.AGREGAR
                && getRow().getAction() != IDataRow.MODIFICAR);
    }

    /**
     * Se deberia ejecutar al cambiar un valor en un control de datos
     *
     * @param fieldname nombre del campo asociado al control.
     */
    public void onChange(String fieldname) {
    }

    /**
     * Refresca el valor de un control asociado a un campo.
     *
     * @param fieldname
     */
    public void refresh(String fieldname) {
    }

    /**
     * Actualiza la base de datos.
     *
     * @return verdadero si tuvo exito o falso si no.
     */
    public boolean update() {
        boolean success = super.update(false);
        facesCtx.addCallbackParam("result", success);
        if (getErrorApp() != null) {
            facesCtx.showError("Error", getErrorApp().getMessage());
        } else if (!success) {
            facesCtx.showError("Error", this.getRow().getErrors());
        }
        // Esto con el fin de que no se ejecute el metodo load del lazydatarows
        // en caso de que se actualize un dataTable
        getFacesCtx().setAttribute("nolazyload", Boolean.TRUE);
        if (success) {
            //Renderizar componentes
            refreshUIComponent();
        }
        afterAction(success);
        // Devolver resultado de la grabación        
        return success;
    }

    /**
     * Actualiza la base de datos.
     *
     * @param dataSet
     * @return verdadero si tuvo exito o falso si no.
     */
    @Override
    public boolean update(IDataSet dataSet) {
        boolean success = super.update(dataSet);
        facesCtx.addCallbackParam("result", success);
        if (getErrorApp() != null) {
            facesCtx.showError("Error", getErrorApp().getMessage());
        } else if (!success) {
            facesCtx.showError("Error", this.getErrorMsg(true));
        }
        // Esto con el fin de que no se ejecute el metodo load del lazydatarows
        // en caso de que se actualize un dataTable
        getFacesCtx().setAttribute("nolazyload", Boolean.TRUE);
        if (success) {
            //Renderizar componentes
            refreshUIComponent();
        }
        afterAction(success);
        // Devolver resultado de la grabación                
        return success;
    }

    /**
     * Revierte cualquier modificación hecha a los datos, los deja como esta en
     * la base.
     *
     * @return verdadero si no exito o falso si no.
     */
    @Override
    public boolean revert() {
        boolean success = super.revert();
        facesCtx.addCallbackParam("result", true);
        if (getErrorApp() != null) {
            facesCtx.showError("Error", getErrorApp().getMessage());
        } else if (!success) {
            facesCtx.showError("Error", this.getErrorMsg(true));
        }
        // Esto con el fin de que no se ejecute el metodo load del lazydatarows
        // en caso de que se actualize un dataTable
        getFacesCtx().setAttribute("nolazyload", Boolean.TRUE);
        //Renderizar componentes
        refreshUIComponent();
        afterAction(success);
        // Devolver resultado de la reversión
        return success;
    }

    /**
     * Refresca el valor de un control de datos.
     */
    protected void refreshUIComponent() {
        if (!Strings.isNullorEmpty(refreshUIComponent)) {
            getFacesCtx().refreshView(refreshUIComponent);
        }
    }

    /**
     * Refresca un control de datos en la vista.
     *
     * @param refresh control de datos a refrescar.
     */
    protected void refreshUIComponent(String refresh) {
        getFacesCtx().refreshView(refresh);
    }

    /**
     * Se ejecuta al finalizar toda operación, normalmente al final de Update()
     * o Revert()
     *
     * @param success verdadero o falso en las operaciones update o revert.
     */
    protected void afterAction(boolean success) {
        if (success) {
            action = "";
            noLazyRowsLoad = false;
        }
    }

    public List<IColumnModel> getColumns() {
        return new ArrayList();
    }

    public String logout() {
        return facesCtx.logout();
    }

    public void setRowForDetail(T rowForDetail) {
        setRowSelected(rowForDetail);
        int recno = this.getDataRows().indexOf((T) rowForDetail);
        this.recnoIndex = recno;
        this.goTo(recno);
        PrimeFaces instance = PrimeFaces.current();
        String command = "selectRow(" + recno + ")";
        instance.executeScript(command);
    }

    public Integer getRecnoIndex() {
        return recnoIndex;
    }

    public void setRecnoIndex(Integer recnoIndex) {
        this.recnoIndex = recnoIndex;
    }

    protected void refreshBean(AbstractDataController bean, String fieldName, Object fieldValue, String order, int maxrows) {
        if (this.getRow() == null) {
            bean.open("", "", true, 0);
            return;
        }
        try {
            if (bean.openOrRequeryIf(fieldName, fieldValue, order, maxrows)) {
                setBeanRowSelected(bean);
            }
        } catch (Exception ex) {
            getFacesCtx().showError(ex.getMessage());
        }
    }

    /**
     * Para filtros en las grillas, usado solo cuando el set de datos no es
     * modalidad Lazy.
     *
     * @param value
     * @param filter
     * @param locale
     * @return
     */
    public boolean filterBy(Object value, Object filter, Locale locale) {
        if (value instanceof Timestamp) {
            Date colDate = (Date) value;
            //TODO analizar este codigo
            String strDate = Dates.toString(colDate, "dd/MM/yyyy");
            return strDate.contains(filter.toString());
        }
        if (value instanceof LocalDateTime) {
            LocalDateTime colDate = (LocalDateTime) value;
            //TODO analizar este codigo
            String strDate = LocalDates.toString(colDate, "dd/MM/yyyy");
            return strDate.contains(filter.toString());
        }
        if (value instanceof BigDecimal) {
            return value.toString().toLowerCase().startsWith(filter.toString().toLowerCase());
        }
        if (!filter.toString().isEmpty()) {
            if (value == null) {
                return false;
            }
            return value.toString().toLowerCase().contains(filter.toString().toLowerCase());
        }
        return true;
    }

    /**
     * Se utiliza en el LazyDataRows, en este metodo se implementa en forma 
     * personalizada el filtro a utilizar posteriormente en la selección de 
     * datos.
     * @param filters nombre de campos y valores.
     * @return filtro personalizado.
     */
    public String onGetFilterString(Map<String, Object> filters) {
        return "";
    }

    /**
     * Utilizado principalemente en el datatable para mostrar los valores 
     * con las mascaras formateadas.
     * @param columName  nombre del campo
     * @param value valor
     * @param mask mascara 
     * @return valor con la mascara tipo string.
     */
    public String getColumnValueWithMask(String columName, Object value, String mask) {
        if (value == null) {
            return "";
        }
        String result = "";
        if (value instanceof Number) {
            result = Fn.numberToString(value, mask);
        } else if (value instanceof Timestamp) {
            if (Fn.nvl(mask, "").isEmpty()) {
                mask = "dd/MM/yyyy";
            }
            result = Dates.toString((Timestamp) value, mask);
        } else if (value instanceof LocalDateTime) {
            if (Fn.nvl(mask, "").isEmpty()) {
                mask = "dd/MM/yyyy";
            }
            result = LocalDates.toString((LocalDateTime) value, mask);
        } else {
            result = value.toString();
        }
        return result;
    }

    /**
     * Utilizado en el datatable para mostrar el valor de la columna con un 
     * estilo especifico.
     * @param columnName nombre de la columna
     * @return estilo css
     */
    public String getColumnStyle(String columnName){
        return "";
    }
    
    class CtrlEventLocal implements ICtrlEvents<IDataObject> {

        @Override
        public Map<String, List<IColumnModel>> getFormViewsColumns() {
            return null;
        }

        @Override
        public void onRowSelect(IDataObject context, Object event) {
            int recno = getDataRows()
                    .indexOf((T) ((org.primefaces.event.SelectEvent) event).getObject());
            if (context.goTo(recno)) {
                rowSelected = getRow();
            }
        }

        @Override
        public void onRowFilter(IDataObject context) {
            if (Strings.isNullorEmpty(tableTextFooter)) {
                return;
            }
            UIComponent text = facesCtx.findComponent(tableTextFooter);
            if (lazyDataRows != null) {
                text.getAttributes().put("value", lazyDataRows.getRowCount());
            } else if (rowsFiltered == null) {
                text.getAttributes().put("value", getDataRows().size());
            } else {
                text.getAttributes().put("value", rowsFiltered.size());
            }
            FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add(tableTextFooter);
        }

        @Override
        public void onColumnSetView(IDataObject context, String form, String viewName) {
        }

        @Override
        public void onColumnReorder(IDataObject context, Object event) {
            tableUIColumns = new ArrayList<>();
            DataTable table = (DataTable) ((javax.faces.event.AjaxBehaviorEvent) event).getSource();
            tableUIColumns.clear();
            for (org.primefaces.component.api.UIColumn column : table.getColumns()) {
                UIComponent colComponent = (UIComponent) column;
                tableUIColumns.add((UIColumn) colComponent);
            }
        }

        @Override
        public void onColumnToggle(IDataObject context, Object pToggleEvent) {
            Map<String, String> params = facesCtx.getRequestParameterMap();

            String nameComponent = params.get("nameComponent");

            Boolean visible = ((ToggleEvent) pToggleEvent).getVisibility() == Visibility.VISIBLE;
            Integer index = (Integer) ((ToggleEvent) pToggleEvent).getData();

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

        @Override
        public void onChange(IDataObject context, String fieldname) {
        }

        @Override
        public List<T> onCompleteText(IDataObject context, String text) {
            if (text.isEmpty()) {
                setFilter("");
            } else {
                String filter = getCompleteTextFilter(text);
                setFilter(filter);
            }
            requery();
            List<T> rows = getDataRows();
            return rows;
        }

        @Override
        public IXmlDom<Document, Element> getXmlResource() {
            return null;
        }

        @Override
        public void setXmlResource(IDataObject context) {
        }
    }
}

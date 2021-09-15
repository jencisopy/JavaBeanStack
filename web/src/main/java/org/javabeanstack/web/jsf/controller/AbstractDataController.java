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
import org.javabeanstack.data.IDataNativeQuery;
import org.javabeanstack.data.IDataQueryModel;

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
import org.javabeanstack.web.util.AppResourceSearcher;

/**
 * Controller para los ABMs de las tablas o vistas, hereda funcionalidades de
 * AbstractDataObject
 *
 * @author Jorge Enciso
 * @param <T>
 */
public abstract class AbstractDataController<T extends IDataRow> extends AbstractDataObject<T> {

    /**
     * Lista de registros de la selección de datos en un proceso que trae los
     * datos por bloque (pagina a pagina)
     */
    private LazyDataRows<T> lazyDataRows;

    private Boolean noLazyRowsLoad = false;
    /**
     * Guarda los registros seleccionados en el UIDataTable o grilla de datos
     */
    private T[] rowsSelected;
    /**
     * Se asigna cuando se selecciona un registro en la grilla de datos
     */
    private T rowSelected;
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

    private ICtrlEvents ctrlEvents = new CtrlEventLocal();

    private String xmlResourcePath = "";

    private IXmlDom<Document, Element> xmlResource;


    /**
     * Lista de campos de busquedas los cuales serán parte del filtro en el
     * metodo onCompleteText
     */
    private final Map<String, String> completeTextSearchFields = new TreeMap(String.CASE_INSENSITIVE_ORDER);

    public AbstractDataController() {
    }

    public AbstractDataController(Class<T> type) {
        this.setType(type);
    }

    protected abstract AppResourceSearcher getAppResource();
    
    public String getXmlResourcePath() {
        return xmlResourcePath;
    }

    public void setXmlResourcePath(String xmlResourcePath) {
        this.xmlResourcePath = xmlResourcePath;
    }
    
    public IXmlDom<Document, Element> getXmlResource() {
        if (xmlResource == null) {
            xmlResource = getAppResource().getXmlDom(xmlResourcePath, "XML", null);            
        }
        return xmlResource;
    }

    public void setXmlResource(T context) {
        //Leer de la tabla AppResourceSearcher o de un xml
        xmlResource = getAppResource().getXmlDom(xmlResourcePath, "XML", null);
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

    /**
     * Devuelve la acción a realizar en el registro actual.
     *
     * @return acción a realizar en el registro actual.
     */
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

    public Boolean getNoLazyRowsLoad() {
        return noLazyRowsLoad;
    }

    public void setNoLazyRowsLoad(Boolean noLazyRowsLoad) {
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
        action = operation;
        // Si no es agregar nuevo registro refrescar el registro actual
        if (!Fn.inList(operation.toLowerCase(), "insert", "agregar", "1")) {
            refreshRow();
        }
        switch (operation.toLowerCase()) {
            case "1":
            case "insert":
            case "agregar":
                result = this.allowAction(IDataRow.AGREGAR);
                if (result) {
                    rowsSelected = null;
                    result = insertRow();
                }
                break;
            case "2":
            case "update":
            case "modificar":
            case "confirm":
            case "anular":                
                result = this.allowAction(IDataRow.MODIFICAR);
                break;
            case "3":
            case "delete":
            case "borrar":
                result = this.allowAction(IDataRow.BORRAR);
                break;
            case "consultar":
            case "consult":
            case "view":
            case "read":
                result = this.allowAction(IDataRow.CONSULTAR);
                break;
        }
        if (getErrorApp() != null) {
            facesCtx.showError("Error", getErrorApp().getMessage());
            result = false;
            action = "";
        } else if (!result) {
            facesCtx.showWarn("No es posible realizar esta operación");
            action = "";
        }
        initAction(operation, result);
        facesCtx.addCallbackParam("result", result);
        if (result) {
            String refreshUI = Fn.nvl((String)getProperty("AFTERACTION_REFRESH_UICOMPONENT"),"");
            refreshUIComponent(refreshUI);
        }
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
        if (Fn.nvl(fieldName, "").isEmpty()) {
            return false;
        }
        return !(getRow().getAction() != IDataRow.AGREGAR
                && getRow().getAction() != IDataRow.MODIFICAR);
    }

    /**
     * Retorna un label según la acción a realizar
     *
     * @return una etiqueta o label según la acción a realizar.
     */
    public String getActionLabel() {
        if (getAction().equals("confirm")) {
            return "Confirmar";
        }
        if (getAction().equals("delete")) {
            return "Eliminar";
        }
        if (getAction().equals("anular")) {
            return "Anular";
        }
        return "Aceptar";
    }

    /**
     * Busca el valor mas alto de un campo en una tabla dada.
     *
     * @param table nombre de la tabla en la base.
     * @param fieldName nombre del campo.
     * @param filter filtro de busqueda.
     * @param parameters parametros a reemplazar en los filtros.
     * @return el valor más alto del campo de la tabla solicitada.
     */
    public Object getNewValue(String table, String fieldName, String filter, Map<String, Object> parameters) {
        IDataNativeQuery query = getDAO().newDataNativeQuery();
        try {
            List<IDataQueryModel> data
                    = query.select("max(" + fieldName + ") as maximo")
                            .from(table)
                            .where(Fn.nvl(filter, ""), parameters)
                            .execQuery();
            if (!data.isEmpty() && data.get(0) != null) {
                return data.get(0).getColumn("maximo");
            }
        } catch (Exception ex) {
            ErrorManager.showError(ex, Logger.getLogger(getClass()));
        }
        return null;
    }

    /**
     * Se deberia ejecutar al cambiar un valor en un control de datos
     *
     * @param fieldname nombre del campo asociado al control.
     */
    public void onChange(String fieldname) {
    }

    /**
     * Se deberia ejecutar al cambiar el foco de un control en el formulario
     *
     * @param fieldname nombre del campo asociado al control.
     */
    public void onBlur(String fieldname) {
    }

    public boolean checkFieldValue(String fieldname) {
        return true;
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
        action = "revert";
        afterAction(success);
        // Devolver resultado de la reversión
        return success;
    }

    /**
     * Refresca el valor de un control de datos.
     */
    protected void refreshUIComponent() {
        String refreshUIComponent = (String) getProperty("refreshUIComponent");
        if (!Strings.isNullorEmpty(refreshUIComponent)) {
            getFacesCtx().refreshView(refreshUIComponent);
        }
    }

    /**
     * Refresca un control de datos en la vista.
     *
     * @param refreshUIComponent control de datos a refrescar.
     */
    protected void refreshUIComponent(String refreshUIComponent) {
        if (!Strings.isNullorEmpty(refreshUIComponent)) {
            getFacesCtx().refreshView(refreshUIComponent);
        }
    }

    /**
     * Se ejecuta al finalizar toda operación, normalmente al final de Update()
     * o Revert()
     *
     * @param success verdadero o falso en las operaciones update o revert.
     */
    protected void afterAction(boolean success) {
        facesCtx.addCallbackParam("result", success);
        if (getErrorApp() != null) {
            facesCtx.showError("Error", getErrorApp().getMessage());
        } else if (!success) {
            facesCtx.showError("Error", getRow().getErrors());
        }
        // Esto con el fin de que no se ejecute el metodo load del lazydatarows
        // en caso de que se actualize un dataTable
        getFacesCtx().setAttribute("nolazyload", Boolean.TRUE);
        if (success) {
            if (Fn.inList(action, "3", "delete", "borrar")) {
                facesCtx.showWarn("Borrado realizado con exito");
            } else if (action.equals("revert")) {
                facesCtx.showInfo("Operación cancelada");
            } else {
                facesCtx.showInfo("Operación realizada con exito");
            }
            if (getRow() != null){
                setRowSelected(getRow());
            }
            //Renderizar componentes
            refreshUIComponent((String)getProperty("AFTERACTION_REFRESH_UICOMPONENT"));
        }
        if (Fn.toLogical(getProperty("AFTERINSERT_INSERT_AGAIN"))) {
            //Para inserción multiples
            if (success && Fn.inList(action, "1", "insert", "agregar")) {
                doAction("insert");
                return;
            }
        }
        if (success && Fn.toLogical(getProperty("AFTERACTION_REFRESH_ROW"))) {
            //Refrescar el registro despues de la grabación
            if (success && Fn.inList(action, "2", "update", "modificar")) {
                refreshRow();
            }
        }
        if (success) {
            action = "";
        }
        noLazyRowsLoad = false;
    }

    public List<IColumnModel> getColumns() {
        return new ArrayList();
    }

    public String logout() {
        return facesCtx.logout();
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
     *
     * @param filters nombre de campos y valores.
     * @return filtro personalizado.
     */
    public String onGetFilterString(Map<String, Object> filters) {
        return "";
    }

    /**
     * Utilizado principalmente en el datatable para mostrar los valores con las
     * mascaras formateadas.
     *
     * @param columnName nombre del campo
     * @param row valores de las columnas
     * @param mask mascara
     * @return valor con la mascara tipo string.
     */
    public String getColumnValueWithMask(String columnName, Object row, String mask) {
        if (columnName == null || row == null) {
            return "";
        }
        if (columnName.contains("{")) {
            return getExpresionWithMask(columnName, row, mask);
        }
        Object value;
        if (row instanceof IDataRow){
            value = ((IDataRow)row).getValue(columnName);            
        }
        else{
            value = ((IDataQueryModel)row).getValue(columnName);            
        }
        return getMask(value, mask);
    }

    /**
     * Devuelve un valor resultante de una expresión dada ejemplo
     * {cuota}/{totalcuota}
     *
     * @param expresion expresión dada.
     * @param row registro donde se encuentra los valores a reemplazar
     * @param mask
     * @return Devuelve un valor resultante de una expresión dada
     */
    protected String getExpresionWithMask(String expresion, Object row, String mask) {
        if (row == null) {
            return null;
        }
        String[] expresionPartes = expresion.split("\\{|\\}");
        String[] maskPartes = {};
        if (!Fn.nvl(mask, "").isEmpty()) {
            maskPartes = mask.split("\\{|\\}");
        }
        String result = "";
        for (int i = 0; i < expresionPartes.length; i++) {
            if (expresionPartes[i].isEmpty()) {
                continue;
            }
            Object value;
            if (expresionPartes[i].trim().length() > 1) {
                if (row instanceof IDataQueryModel){
                    value = ((IDataQueryModel)row).getValue(expresionPartes[i].trim());                    
                }
                else {
                    value = ((IDataRow)row).getValue(expresionPartes[i].trim());                    
                }
                if (maskPartes != null && maskPartes.length > i) {
                    result += getMask(value, maskPartes[i]);
                } else {
                    result += value;
                }
            } else {
                value = expresionPartes[i].trim();
                result += value;
            }
        }
        return result;
    }

    /**
     * Devuelve el valor con la mascara correspondiente.
     *
     * @param value valor
     * @param mask mascara
     * @return valor con la mascara tipo string.
     */
    public String getMask(Object value, String mask) {
        if (value == null) {
            return "";
        }
        String result = "";
        try {
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
            } else if (value instanceof String
                    && !Strings.isNullorEmpty(mask)
                    && !Strings.isNullorEmpty((String) value)) {
                //Formatear valor alfanumerico según mascara
                int c = 0;
                for (int i = 0; i < mask.length(); i++) {
                    if (c >= ((String) value).length()) {
                        break;
                    }
                    if ("-,. (){}:".contains(Strings.substr(mask, i, 1))) {
                        result += Strings.substr(mask, i, 1);
                    } else {
                        result += Strings.substr((String) value, c, 1);
                        c++;
                    }
                }
            } else {
                result = value.toString();
            }
        } catch (Exception exp) {
            return "";
        }
        return result;
    }

    /**
     * Utilizado en el datatable para mostrar el valor de la columna con un
     * estilo especifico.
     *
     * @param columnName nombre de la columna
     * @param row
     * @return estilo css
     */
    public String getColumnStyle(String columnName, Object row) {    
        return "";
    }

    /**
     * Copia los datos del registro actual de la vista al objeto tabla
     *
     * @param <T>
     * @param <K>
     * @param target controller destino donde almacena el objeto mapeado a la
     * tabla
     * @throws Exception
     */
    protected <T extends IDataRow, K extends AbstractDataController> void copyTo(K target) throws Exception {
        target.insertRow();
        T row = (T) target.getRow();
        row.setAction(getRow().getAction());
        row = this.getDAO().getDataService().copyTo(getUserSession().getSessionId(), getRow(), row);
        List<T> dataRows = new ArrayList();
        dataRows.add(row);
        target.setDataRows(dataRows);
        target.moveFirst();
    }

    class CtrlEventLocal implements ICtrlEvents<IDataObject> {
        private List<UIColumn> tableUIColumns;

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
            String tableTextFooter = (String) getProperty("tableTextFooter");
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
    }
    
    protected boolean beforeLazyRowsLoad(){
        return true;
    }
}

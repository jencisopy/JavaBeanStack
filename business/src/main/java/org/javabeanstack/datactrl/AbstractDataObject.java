/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 Jorge Enciso
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
package org.javabeanstack.datactrl;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.javabeanstack.data.DataInfo;
import org.javabeanstack.data.IDBFilter;
import org.javabeanstack.data.IDBManager;
import org.javabeanstack.data.IDataLink;
import org.javabeanstack.data.IDataResult;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.data.IDataSet;

import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.events.IDataEvents;
import org.javabeanstack.exceptions.FieldException;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.Strings;

/**
 * Esta clase se encarga de almacenar los registros de datos recuperados de la
 * base de datos a travéz de la clase dataLink(wrapper dao). Provee interfases
 * que permiten la recuperación, busqueda, refresco, y actualización de los
 * registros. A travéz del dataService válida y guarda la información en la base
 * de datos. Esta clase se instancia en la capa de la app cliente.
 *
 * @author Jorge Enciso
 * @param <T> tipo ejb
 */
public abstract class AbstractDataObject<T extends IDataRow> implements IDataObject, Serializable {
    private static final Logger LOGGER = Logger.getLogger(AbstractDataObject.class);
    
    /**
     * Puntero del nro de registro o fila
     */
    private int recno;
    /**
     * Lista donde se almacena los registros recuperados de la base
     */
    private List<T> dataRows;
    /**
     * Registro actual
     */
    private T row;
    /**
     * Clase del registro
     */
    private Class<T> type;
    /**
     * Si los datos a recuperar puede ser de lectura y escritura
     */
    private boolean readWrite;
    /**
     * Filtro, se utiliza para seleccionar los datos de la base
     */
    private String filter;
    /**
     * Filtro extra, se utiliza para seleccionar los datos de la base
     */
    private Map<String, String> filters = new HashMap();
    
    /**
     * Parametros filtros
     */
    private Map<String, Object> filterParams;
    /**
     * Al recuperar los registros, este dato indica el orden a recuperarlos
     */
    private String order;
    /**
     * Primer registro del conjunto de registros a recuperar
     */
    private int firstRow = 0;
    /**
     * Maxima cantidad de registros a recuperar
     */
    private int maxrows = 999999999;
    /**
     * Sentencia que se ejecutara para recuperar los datos
     */
    private String selectCmd;
    /**
     * Ultima sentencia ejecutada para recuperar los datos
     */
    private String lastQuery;
    /**
     * Se utiliza para guardar el ultimo campo utilizado en la busqueda de un
     * registro
     */
    private String lastfieldfounded;
    /**
     * Es el valor encontrado, de la ultima busqueda realizada
     */
    private Object lastvalfounded;
    /**
     * Guarda los errores por alguna excepción de la aplicación
     */
    private Exception errorApp;
    /**
     * Es el objeto responsable de los eventos
     */
    private IDataEvents dataEvents;
    
    private boolean showDeletedRow = false;
    
    private Object idParent;    
    

    /**
     * @return Devuelve true si los datos son de solo lectura
     */
    @Override
    public boolean isReadwrite() {
        return readWrite;
    }

    /**
     * Devuelve el manejador de datos del schema de datos
     *
     * @return manejador de datos
     */
    @Override
    public abstract IDataLink getDAO();

    /**
     * Devuelve el manejador de datos del schema catalogo
     *
     * @return manejador de datos
     */
    @Override
    public abstract IDataLink getDAOCatalog();

    /**
     * Devuelve el objeto dataEvents asignado.
     *
     * @return objeto dataEvents (manejador de eventos)
     */
    @Override
    public IDataEvents getDataEvents() {
        return dataEvents;
    }

    /**
     * Asigna un objeto dataEvent
     *
     * @param dtEvents manejador de eventos
     */
    protected void setDataEvents(IDataEvents dtEvents) {
        dataEvents = dtEvents;
        if (dataEvents != null){
            dataEvents.setContext(this);            
        }
    }

    /**
     * Clase del registro
     *
     * @return clase del modelo de dato
     */
    @Override
    public Class<T> getType() {
        if (type != null){
            return type;
        }
        return (Class<T>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
    }

    /**
     * Devuelve un objeto detallando los errores ocurridos posterior a la
     * ejecución de una validación o grabación de datos.
     *
     * @return objeto error.
     */
    @Override
    public Exception getErrorApp() {
        return errorApp;
    }

    /**
     * Devuelve una cadena de errores ocurridos posterior a la ejecución de una
     * validación o grabación de datos.
     *
     * @param all determina si va a devolver todos los errores o solo el
     * primero.
     * @return cadena con los errores.
     */
    @Override
    public String getErrorMsg(boolean all) {
        String msgErrores = "";
        if (all && errorApp != null) {
            msgErrores = errorApp.getMessage() + "\n";
        }
        if (this.row == null) {
            return "";
        }
        if (this.row.getErrors() != null && this.row.getErrors().size() > 0) {
            Iterator iterator = this.row.getErrors().keySet().iterator();
            IErrorReg error;
            String key;
            while (iterator.hasNext()) {
                key = (String) iterator.next();
                error = this.row.getErrors().get(key);
                if (!all) {
                    return error.getMessage();
                }
                msgErrores += error.getMessage() + "\n";
            }
            return msgErrores;
        }
        return msgErrores;
    }

    /**
     * Devuelve el mensaje de error asociado a un campo si lo hubiese
     *
     * @param fieldName nombre del campo
     * @return mensaje de error.
     */
    @Override
    public String getErrorMsg(String fieldName) {
        if (this.row == null) {
            return "";
        }
        if (this.row.getErrors() != null && this.row.getErrors().size() > 0) {
            return this.row.getErrors().get(fieldName.toLowerCase()).getMessage();
        }
        return "";
    }

    /**
     * Filtro, es utilizado para filtrar la selección de los datos de la base
     *
     * @return filtro utilizado para la selección de los datos.
     */
    @Override
    public String getFilter() {
        return filter;
    }

    /**
     * Este dato indica el orden a recuperar de los datos
     *
     * @return orden de la sentencia.
     */
    @Override
    public String getOrder() {
        return order;
    }


    /**
     * Devuelve los parametros de la condición del filtro.
     *
     * @return parametros del filtro.
     */
    @Override
    public Map<String, Object> getFilterParams() {
        return filterParams;
    }

    /**
     * Lista de objetos donde se almacena los registros recuperados de la base
     *
     * @return lista de objetos
     */
    @Override
    public List<T> getDataRows() {
        return dataRows;
    }

    /**
     * Lista de objetos que fueron modificados
     *
     * @return lista de objetos modificados
     */
    @Override
    public Map<Integer, T> getDataRowsChanged() {
        if (dataRows == null){
            LOGGER.error("El dataobject no esta abierto");
            return null;
        }
        Map<Integer, T> dataRowsChanged = new LinkedHashMap();
        for (int i = 0; i < dataRows.size(); i++) {
            if (dataRows.get(i).getAction() != 0) {
                dataRowsChanged.put(i, dataRows.get(i));
            }
        }
        return dataRowsChanged;
    }

    /**
     * Registro actual
     *
     * @return registro actual
     */
    @Override
    public T getRow() {
        return row;
    }

    /**
     * Sentencia que se ejecutara para recuperar los datos
     *
     * @return sentencia jpql
     */
    @Override
    public String getSelectCmd() {
        return selectCmd;
    }

    /**
     * Es la ultima sentencia ejecutada para la recuperación de los datos.
     *
     * @return ultima sentencia ejecutada
     */
    @Override
    public String getLastQuery() {
        return lastQuery;
    }

    /**
     * Devuelve el nro de fila actual. (0 es la primera fila)
     *
     * @return retorna nro de fila o registro.
     */
    @Override
    public int getRecno() {
        return recno;
    }

    /**
     * Esta propiedad determina si se puede o no navegar a travéz de los
     * elementos de la lista de objetos que fuerón marcados para borrase.
     *
     * @return valor del showDeleteRow que determina si muestra o no los
     * registros marcados para borrarse.
     */
    @Override
    public boolean isShowDeletedRow() {
        return showDeletedRow;
    }

    /**
     * Asigna la propiedad showDeleteRow determina si se puede o no navegar a
     * travéz de los elementos de la lista de objetos que fuerón marcados para
     * borrase.
     *
     * @param showDeletedRow
     */
    @Override
    public void setShowDeletedRow(boolean showDeletedRow) {
        this.showDeletedRow = showDeletedRow;
    }

    /**
     * Devuelve la cantidad de registros de la lista Rows
     *
     * @return retorna cantidad de filas o registros.
     */
    @Override
    public int getRowCount() {
        if (dataRows == null){
            LOGGER.error("El dataobject no esta abierto");
            return 0;
        }
        return dataRows.size();
    }

    /**
     * Devuelve que operación se realizo sobre el registro actual (Agregar,
     * borrar, modificar)
     *
     * @return 1 Agregar<br>
     * 2 Modificar<br>
     * 3 Borrar<br>
     */
    @Override
    public int getRecStatus() {
        if (row == null) {
            LOGGER.error("El dataobject no esta abierto");
            return 0;
        }
        return row.getAction();
    }

    @Override
    public int getFirstRow() {
        return firstRow;
    }

    @Override
    public int getMaxRows() {
        return maxrows;
    }


    @Override
    public Long getIdcompany() {
        Long idempresa = 0L;
        if (getDAO().getUserSession() != null) {
            idempresa = getDAO().getUserSession().getIdCompany();
        }
        return idempresa;
    }

    @Override
    public Long getIdempresa() {
        return getIdcompany();
    }

    /**
     * Asigna un valor a la propiedad readwrite que determina si puede o no
     * escribirse en la lista de objetos
     *
     * @param valor
     */
    @Override
    public void setReadWrite(boolean valor) {
        this.readWrite = valor;
    }

    /**
     * Asigna el tipo o clase del modelo de dato.
     *
     * @param type
     */
    @Override
    public void setType(Class type) {
        this.type = type;
    }

    /**
     * Setea el atributo filtro, que es utilizado para filtrar la selección de
     * los datos de la base
     *
     * @param filter
     */
    @Override
    public void setFilter(String filter) {
        this.filter = filter;
    }

    /**
     * Setea el atributo order, que es utilizado para indicar el orden en que se
     * recuperan los registros
     *
     * @param order
     */
    @Override
    public void setOrder(String order) {
        this.order = order;
    }

    /**
     * Asigna un map con los valores de los parámetros del filtro
     *
     * @param filterParams
     */
    @Override
    public void setFilterParams(Map filterParams) {
        this.filterParams = filterParams;
    }

    @Override
    public void setFirstRow(int first) {
        firstRow = first;

    }

    @Override
    public void setMaxRows(int maxRows) {
        maxrows = maxRows;
    }

    /**
     * Setea el atributo selectcmd, sentencia que se ejecutara para recuperar
     * los datos
     */
    protected void setSelectcmd() {
        String sqlComando;
        String filtro = "";
        if (filter == null) {
            filter = "";
        }
        sqlComando = "select o from " + getType().getSimpleName() + " o ";
        if (!getDAO().getPersistUnit().equals(IDBManager.CATALOGO)
                && getDAO().getUserSession() != null) {
            IDBFilter dbFilter = getDAO().getUserSession().getDBFilter();
            if (dbFilter != null){
                filtro = dbFilter.getFilterExpr(getType(), "");
                String separador = "";
                if (!Strings.isNullorEmpty(filter) && !Strings.isNullorEmpty(filtro)){
                    separador = " and ";
                } 
                filtro += separador + filter;
            }
        } else {
            filtro = filter;
        }
        //Filtro
        if (!"".equals(filtro)) {
            sqlComando = sqlComando + " where " + filtro;
        }
        this.selectCmd = sqlComando;
    }

    /**
     * Setea el objeto Rows, que es una lista donde se almacena los registros
     * recuperados de la base
     *
     * @param dataRows
     */
    public void setDataRows(List<T> dataRows) {
        this.dataRows = dataRows;
        this.recno = 0;
    }

    /**
     * Se ejecuta antes del metodo open(), recibe los mismos parámetros que el
     * metodo open().
     *
     * @param order orden de la recuperación.
     * @param filter filtro de la selección.
     * @param readwrite lectura y escritura
     * @param maxrows maxima cantidad de registros a recuperar, -1 todos.
     */
    protected void beforeOpen(String order, String filter, boolean readwrite, int maxrows) {
        if (this.dataEvents != null) {
            this.dataEvents.beforeOpen(order, filter, readwrite, maxrows);
        }
    }

    /**
     * Recupera registros de la base de datos
     *
     * @return verdadero si tuvo exito.
     */
    @Override
    public boolean open() {
        order = "";
        filter = "";
        filters = new HashMap();
        firstRow = 0;
        maxrows = -1;
        return this.open("", "", true, -1);
    }

    /**
     * Recupera registros de la base de datos
     *
     * @param order orden de la recuperación.
     * @param filter filtro de la selección.
     * @param readwrite lectura y escritura
     * @param maxrows maxima cantidad de registros a recuperar, -1 todos.
     *
     * @return verdadero si tu exito la recuperación, falso si no
     */
    @Override
    public boolean open(String order, String filter, boolean readwrite, int maxrows) {
        try {
            errorApp = null;
            this.beforeOpen(order, filter, readwrite, maxrows);
            //
            this.recno = 0;
            this.row = null;
            this.dataRows = null;
            this.readWrite = readwrite;
            this.filter = filter;
            this.order = order;
            this.maxrows = maxrows;
            //
            this.dataFill();
            //
            this.afterOpen(order, filter, readwrite, maxrows);
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
            errorApp = ex;
            return false;
        }
        return true;
    }

    /**
     * Se ejecuta posterior al metodo open().
     *
     * @param order orden de la recuperación.
     * @param filter filtro de la selección.
     * @param readwrite lectura y escritura
     * @param maxrows maxima cantidad de registros a recuperar, -1 todos.
     */
    protected void afterOpen(String order, String filter, boolean readwrite, int maxrows) {
        if (this.dataEvents != null) {
            this.dataEvents.afterOpen(order, filter, readwrite, maxrows);
        }
    }

    /**
     * Se ejecuta antes del metodo datafill
     */
    protected void beforeDataFill() {
        if (this.dataEvents != null) {
            this.dataEvents.beforeDataFill();
        }
    }

    /**
     * Tiene la función de recuperar la lista de registros de la base de datos
     *
     * @throws java.lang.Exception
     */
    protected void dataFill() throws Exception {
        beforeDataFill();
        //
        if (maxrows == -1) {
            maxrows = 999999999;
        }
        errorApp = null;
        List<T> x;
        // Si existe un dataService utilizarlo parar traer los datos
        if (this.getDAO().getDataService() != null) {
            IUserSession userSession = getDAO().getUserSession();
            String sessionId = null;
            if (userSession != null) {
                sessionId = userSession.getSessionId();
            }
            //Del filtro principal + los agregados.
            String allFilters = getFilterSentence(false);
            //
            x = this.getDAO().getDataService().getDataRows(sessionId, getType(), order, allFilters, filterParams, firstRow, maxrows);
            selectCmd = this.getDAO().getDataService().getSelectCmd(sessionId, getType(), order, filter);
            lastQuery = this.getDAO().getDataService().getSelectCmd(sessionId, getType(), order, allFilters);
        } else {
            setSelectcmd();
            String query = selectCmd;
            String filterExtra = getFilterSentence(true);
            // Se agrega el filtro extra 
            if (!Strings.isNullorEmpty(filterExtra)) {
                if (!query.contains("where ")) {
                    query += " where " + filterExtra;
                } else if (query.contains("where ")) {
                    query += " and " + filterExtra;
                }
            }
            //Orden
            if (order != null && !"".equals(order)) {
                query += " order by " + order;
            }
            lastQuery = query;
            if (maxrows == 0) {
                x = new ArrayList();
            } else {
                x = (ArrayList<T>) getDAO().findListByQuery(query, filterParams, firstRow, maxrows);
            }
        }

        this.setDataRows(x);
        this.moveFirst();
        //
        afterDataFill();
    }

    /**
     * Se ejecuta posterior al metodo datafill
     */
    protected void afterDataFill() {
        if (this.dataEvents != null) {
            this.dataEvents.afterDataFill();
        }
    }

    /**
     * Se ejecuta antes que el metodo requery()
     */
    protected void beforeRequery() {
        if (this.dataEvents != null) {
            this.dataEvents.beforeRequery();
        }
    }

    /**
     * Realiza la misma tarea del metodo open(), vuelve a seleccionar los datos
     * de la base de datos utilizando una expresión de filtro diferente.
     *
     * @param filterExtra expresión de la condición del filtro.
     * @param filterParams parámetros del filtro.
     * @return verdadero si tu exito, falso si no.
     */
    @Override
    public boolean requery(String filterExtra, Map filterParams) {
        this.addFilter(filterExtra);
        this.filterParams = filterParams;
        return requery();
    }

    /**
     * Recupera nuevamente los registros de la base de datos, teniendo en cuenta
     * los valores de los parámetros filter, order y maxrows
     *
     * @return verdadero si tu exito la recuperación, falso si no
     */
    @Override
    public boolean requery() {
        //Primero el open
        if (!isOpen()){
            return false;
        }
        try {
            errorApp = null;
            this.beforeRequery();
            //
            this.recno = 0;
            this.row = null;
            this.dataRows = null;
            //
            this.dataFill();
            //
            this.afterRequery();
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
            errorApp = ex;
            return false;
        }
        return true;
    }

    public void openOrRequery(String filter) {
        openOrRequery("", filter, true, -1);
    }

    public void openOrRequery(String order, String filter, boolean readwrite, int maxrows) {
        if (!this.isOpen()) {
            this.open(order, filter, true, 0);
            if (maxrows == 0) {
                return;
            }
        }
        this.setMaxRows(0);
        this.requery();
    }

    public boolean openOrRequery(String fieldName, Object fieldValue, String order, int maxrows) {
        boolean result = false;
        if (!this.isOpen() || fieldValue == null) {
            this.open("", "", true, 0);
        }
        if (fieldValue != null) {
            Map<String, Object> param = new HashMap();
            param.put("id", fieldValue);
            String filter = fieldName + " = :id";
            setFilter(filter);
            setOrder(order);
            setMaxRows(maxrows);
            setFilterParams(param);
            this.requery();
            result = true;
        }
        return result;
    }
    
    public boolean openOrRequeryIf(String fieldName, Object fieldValue, String order, int maxrows) {
        boolean result = false;
        if (!this.isOpen() || fieldValue == null) {
            this.open("", "", true, 0);
        }
        if (fieldValue != null && !fieldValue.equals(idParent)) {
            Map<String, Object> param = new HashMap();
            param.put("id", fieldValue);
            String filter = fieldName + " = :id";
            setFilter(filter);
            setOrder(order);
            setMaxRows(maxrows);
            setFilterParams(param);
            this.requery();
            result = true;
        }
        idParent = fieldValue;
        return result;
    }
    
    /**
     * Se ejecuta posterior al metodo requery().
     */
    protected void afterRequery() {
        if (this.dataEvents != null) {
            this.dataEvents.afterRequery();
        }
    }

    /**
     * Se ejecuta antes del metodo goTo()
     *
     * @param row fila o objeto de dato
     * @return verdadero si tuvo exito, falso si no.
     */
    protected boolean beforeRowMove(IDataRow row) {
        if (this.dataEvents != null) {
            return this.dataEvents.beforeRowMove(row);
        }
        return true;
    }

    /**
     * Se posiciona en un registro determinado
     *
     * @param rownumber nro. de fila o registro. (0 es el primer registro)
     * @return verdadero si tu exito, falso si no
     */
    @Override
    public boolean goTo(int rownumber) {
        return this.goTo(rownumber, 1);
    }

    /**
     * Se posiciona en un registro determinado
     *
     * @param rownumber nro. de fila o registro. (0 es el primer registro)
     * @param offset si el registro donde se quiere desplazar esta marcado como
     * borrado, entonces se busca posicionar para abajo (-1) o arriba (1) de
     * acuerdo al valor del desplazamiento
     *
     * @return verdadero si tu exito, falso si no
     */
    @Override
    public boolean goTo(int rownumber, int offset) {
        if (dataRows == null){
            LOGGER.error("El dataobject no esta abierto");
            return false;
        }
        //Before move
        this.beforeRowMove(row);
        if (getRowCount() == 0){
            row = null;
            return true;
        }
        if (rownumber < 0) {
            return false;
        }
        if (dataRows.isEmpty()){    
            return false;
        }

        if (offset == 0) {
            offset = 1;
        }

        while (true) {
            if ((rownumber >= dataRows.size()) || (rownumber < 0)) {
                break;
            }
            //Si en el desplazamiento no se considera los borrados
            if (!showDeletedRow && dataRows.get(rownumber).getAction() == IDataRow.DELETE) {
                rownumber = rownumber + offset;
                continue;
            }
            break;
        }

        if (dataRows.size() > rownumber) {
            recno = rownumber;
            row = dataRows.get(recno);
            // Si no esta en proceso de modificación o borrado
            if (row.getAction() == 0){
                refreshRow();
            }
            //After move
            this.afterRowMove(row);
            return true;
        } else {
            recno = dataRows.size();
            row = null;
            //After move
            this.afterRowMove(row);
        }
        return false;
    }

    /**
     * Se ejecuta posterior al metodo goTo()
     *
     * @param row
     */
    protected void afterRowMove(IDataRow row) {
        if (this.dataEvents != null) {
            this.dataEvents.afterRowMove(row);
        }
    }

    /**
     * Se posiciona en el primer registro de la lista
     *
     * @return verdadero si tuvo exito, falso si no.
     */
    @Override
    public boolean moveFirst() {
        return this.goTo(0, 1);
    }

    /**
     * Se posiciona en el siguiente registro de la lista
     *
     * @return verdadero si tuvo exito, falso si no.
     */
    @Override
    public boolean moveNext() {
        return this.goTo(recno + 1, 1);
    }

    /**
     * Se posiciona en el registro anterior
     *
     * @return verdadero si tuvo exito, falso si no.
     */
    @Override
    public boolean movePrevious() {
        return this.goTo(recno - 1, -1);
    }

    /**
     * Se posiciona en el ultimo registro de la lista
     *
     * @return verdadero si tuvo exito, falso si no.
     */
    @Override
    public boolean moveLast() {
        if (dataRows == null){
            LOGGER.error("El dataobject no esta abierto");            
            return false;
        }
        return this.goTo(dataRows.size() - 1, -1);
    }

    /**
     * Buscar un registro en Rows, y devuelve el número de fila.
     *
     * @param rowList Lista de registros
     * @param field Nombre del campo a comparar en la condición.
     * @param value Valor del campo buscado.
     * @param begin Se realiza la busqueda a partir de este registro de la
     * lista.
     * @param end Se realiza la busqueda hasta este registro de la lista.
     * @return el puntero de un registro, el cual cumple con las condiciones de
     * la busqueda
     */
    public static int findRow(List<? extends IDataRow> rowList, String field, Object value, int begin, int end) {
        if (rowList == null) {
            return -1;
        }
        if (end >= rowList.size()){
            end = rowList.size() - 1;
        }
        //Determinar que el rango de busqueda sea valido
        if (begin < 0 || begin > end) {
            return -1;
        }
        //Si el campo es nulo buscar en campo anterior
        if (field == null) {
            return -1;
        }
        //Recorrer el array en busca del objeto con el valor solicitado
        for (int i = begin; i <= end; i++) {
            IDataRow row = rowList.get(i);
            if (row.getAction() != IDataRow.DELETE) {
                if (value instanceof String
                        && ((String) row.getValue(field)).trim().equals(value.toString().trim())) {
                    return i;
                } else if (row.getValue(field).equals(value)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Su función es buscar un registro y posicionarse en el mismo.
     *
     * @param field nombre del campo
     * @param value valor buscado.
     * @param begin Se realiza la busqueda a partir de este registro de la
     * lista.
     * @param end Se realiza la busqueda hasta este registro de la lista.
     * @return retorna verdadero si tuvo exito o falso si no.
     */
    @Override
    public boolean find(String field, Object value, int begin, int end) {
        //Si el campo es nulo buscar en campo anterior
        if (field == null && this.lastvalfounded != null) {
            field = this.lastfieldfounded;
            value = this.lastvalfounded;
        }
        if (field == null) {
            return false;
        }
        this.lastfieldfounded = "";
        this.lastvalfounded = null;
        int rec = AbstractDataObject.findRow((List<IDataRow>) dataRows, field, value, begin, end);
        if (rec >= 0) {
            this.goTo(rec);
            this.lastfieldfounded = field;
            this.lastvalfounded = value;
            return true;
        }
        return false;
    }

    /**
     * Su función es buscar un registro y posicionarse en el mismo.
     *
     * @param field nombre del campo
     * @param value valor buscado.
     * @return retorna verdadero si tuvo exito o falso si no.
     */
    @Override
    public boolean find(String field, Object value) {
        if (dataRows == null){
            LOGGER.error("El dataobject no esta abierto");
            return false;
        }
        return this.find(field, value, 0, dataRows.size() - 1);
    }

    /**
     * Busca el siguiente registro a partir del actual que cumple con el
     * criterio de la ultima busqueda
     *
     * @return retorna verdadero si tuvo exito o falso si no.
     */
    @Override
    public boolean findNext() {
        if (dataRows == null){
            LOGGER.error("El dataobject no esta abierto");
            return false;
        }
        if (this.lastvalfounded == null) {
            return false;
        }
        return this.find(null, null, recno++, dataRows.size() - 1);
    }

    /**
     * Comunica si esta posicionado en la ultima fila + 1
     *
     * @return retorna verdadero si es fin de la lista o falso si no
     */
    @Override
    public boolean isEof() {
        return row == null;
    }

    /**
     * Devuelve un valor nuevo para una clave
     *
     * @param fieldname valor nuevo para este campo
     * @return valor nuevo para una clave.
     */
    @Override
    public Object getNewValue(String fieldname) {
        return null;
    }

    /**
     * Devuelve el valor por defecto de un campo dado, del registro actual.
     *
     * @param fieldname nombre del campo
     * @return valor por defecto del campo solicitado
     */
    @Override
    public Object getFieldDefaultValue(String fieldname) {
        if (row == null) {
            return null;
        }
        return DataInfo.getDefaultValue(row, fieldname);
    }

    /**
     * Devuelve un valor de un campo dado, del registro actual.
     *
     * @param fieldname nombre del campo
     * @return valor del campo solicitado.
     */
    @Override
    public Object getField(String fieldname) {
        if (row == null) {
            return null;
        }
        return row.getValue(fieldname);
    }

    /**
     * Devuelve un valor de un campo dado, del registro actual, de un miembro
     * relacionado.
     *
     * @param objname nombre del miembro relacionado.
     * @param fieldname nombre del campo
     * @return valor de un campo solicitado.
     */
    @Override
    public Object getField(String objname, String fieldname) {
        if (row == null) {
            return null;
        }
        IDataRow obj = DataInfo.getObjFk(row, objname);
        if (obj == null) {
            return null;
        }
        return obj.getValue(fieldname);
    }

    /**
     * Devuelve el valor original de un campo dado, del registro actual.
     *
     * @param fieldname nombre del campo
     * @return valor anterior de un campo solicitado.
     */
    @Override
    public Object getFieldOld(String fieldname) {
        return row.getOldValue(fieldname);
    }

    /**
     * Devuelve el objeto de una clave foranea dada, del registro actual
     * posicionado.
     *
     * @param fieldname nombre del campo
     * @return instancia de un objeto de una clave foranea.
     */
    @Override
    public IDataRow getFieldObjFK(String fieldname) {
        if (row == null) {
            return null;
        }
        return DataInfo.getObjFk(row, fieldname);
    }

    /**
     * Se ejecuta antes del metodo setfield
     *
     * @param fieldname nombre del campo
     * @param oldValue
     * @param newValue
     * @return Verdadero si tuvo exito o falso si no
     */
    protected boolean beforeSetField(String fieldname, Object oldValue, Object newValue) {
        if (this.dataEvents != null) {
            return this.dataEvents.beforeSetField(row, fieldname, oldValue, newValue);
        }
        return true;
    }

    /**
     * Asigna un valor a un campo dado
     *
     * @param fieldname nombre del campo
     * @param value valor a asignar
     * @return Verdadero si tuvo exito o falso si no
     */
    @Override
    public boolean setField(String fieldname, Object value){
        return this.setField(fieldname, value, false);
    }

    /**
     * Asigna un valor a un campo dado de tipo IDataRow
     *
     * @param fieldname nombre del campo
     * @param param map con los valores
     * @return Verdadero si tuvo exito o falso si no
     */
    @Override
    public boolean setField(String fieldname, Map param){
        try {
            if (param == null){
                setField(fieldname,(Object)param);
                return true;
            }
            if (!(row.getValue(fieldname) instanceof IDataRow)) {
                return false;
            }
            IDataRow obj = (IDataRow) row.getValue(fieldname);
            Iterator<Map.Entry<String, Object>> it = param.entrySet().iterator();
            Map.Entry<String, Object> entry;
            while (it.hasNext()) {
                entry = it.next();
                obj.setValue(entry.getKey(), entry.getValue());
            }
            obj = getDAO().findByUk(obj);
            setField(fieldname, obj);
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
            errorApp = ex;
            return false;
        }
        return true;
    }

    /**
     * Asigna un valor a un campo dado
     *
     * @param fieldname nombre del campo
     * @param newValue
     * @param noAfterSetField Si es verdadero no se ejecuta el metodo
     * AfterSetfield.
     * @return Verdadero si tuvo exito o falso si no
     */
    @Override
    public boolean setField(String fieldname, Object newValue, boolean noAfterSetField){
        /* No se puede modificar si es de solo lectura */
        if (!this.readWrite) {
            errorApp = new FieldException("El objeto esta abierto para solo lectura");
            return false;
        }
        if (row == null) {
            errorApp = new FieldException("El objeto es null");
            return false;
        }
        Object oldValue = row.getValue(fieldname);
        try {
            errorApp = null;
            if (!this.beforeSetField(fieldname, oldValue, newValue)) {
                return false;
            }
            Class<?> classMember = row.getFieldType(fieldname);
            if (newValue == null){
                row.setValue(fieldname, null);
            } else if  (classMember.getName().equals(newValue.getClass().getName())){
                  row.setValue(fieldname, newValue);
            } else if (classMember.getSimpleName().equals("Short") && !(newValue instanceof Short)) {
                Short newValueAux = Short.valueOf(newValue.toString());
                row.setValue(fieldname, newValueAux);
            } else if (classMember.getSimpleName().equals("Integer") && !(newValue instanceof Integer)) {
                Integer newValueAux = Integer.valueOf(newValue.toString());
                row.setValue(fieldname, newValueAux);
            } else if (classMember.getSimpleName().equals("Long") && !(newValue instanceof Long)) {
                Long newValueAux = Long.valueOf(newValue.toString());
                row.setValue(fieldname, newValueAux);
            } else if (classMember.getSimpleName().equals("Character") && !(newValue instanceof Character)) {
                char newValueAux = newValue.toString().charAt(0);
                row.setValue(fieldname, newValueAux);
            } else if (classMember.getSimpleName().equals("BigDecimal") && !(newValue instanceof BigDecimal)) {
                BigDecimal newValueAux = new BigDecimal(newValue.toString());
                row.setValue(fieldname, newValueAux);
            } else if (classMember.getSimpleName().equals("Boolean") && !(newValue instanceof Boolean)) {
                Boolean newValueAux = (newValue.toString().equals("1"));
                row.setValue(fieldname, newValueAux);
            } else {
                row.setValue(fieldname, newValue);
            }
            //
            if (!noAfterSetField) {
                return this.afterSetField(fieldname, oldValue, newValue);
            }
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
            errorApp = ex;
            return false;
        }
        return true;
    }

    /**
     * Se ejecuta despues del metodo setfield
     *
     * @param fieldname nombre del campo
     * @param newValue valor a asignar
     * @param oldValue Valor anterior a la asignación del nuevo valor.
     * @return Verdadero si tuvo exito o falso si no
     */
    protected boolean afterSetField(String fieldname, Object oldValue, Object newValue) {
        //Ejecutar aftersetfield si existe objeto datalogic
        if (this.dataEvents != null) {
            return this.dataEvents.afterSetField(row, fieldname, oldValue, newValue);
        }
        return true;
    }

    /**
     * Devuelve verdadero o falso si es que existe un campo o propiedad en el
     * modelo de dato.
     *
     * @param fieldname nombre del campo
     * @return verdadero si existe el campo en el modelo o falso si no.
     */
    @Override
    public boolean isFieldExist(String fieldname) {
        return DataInfo.isFieldExist(this.getType(), fieldname);
    }

    /**
     * Devuelve verdadero o falso si es que existe un metodo en el
     * modelo de dato.
     *
     * @param methodName nombre del metodo
     * @return verdadero si existe el metodo en el modelo o falso si no.
     */
    @Override
    public boolean isMethodExist(String methodName) {
        return DataInfo.isMethodExist(this.getType(), methodName);
    }
    
    /**
     * Determina si un campo o miembro es una clave foranea.
     *
     * @param fieldname nombre del campo
     * @return Verdadero si es una clave foranea.
     */
    @Override
    public boolean isForeingKey(String fieldname) {
        return DataInfo.isForeignKey(this.getType(), fieldname);
    }

    /**
     * Determina si el metodo Open fue ejecutado con exito
     *
     * @return Verdadero si esta abierto o falso si no
     */
    @Override
    public boolean isOpen() {
        return this.dataRows != null;
    }

    /**
     * Devuelve verdadero o falso si es que la operación solicitada (agregar,
     * modificar,borrar) es permitida o no.
     *
     * @param operation operación solicitada.<br>
     * 1 Agregar <br>
     * 2 Modificar<br>
     * 3 Borrar<br>
     * 4 Consultar<br>
     * @return verdadero o falso si es que la operación (agregar,
     * modificar,borrar) es permitida o no.
     */
    @Override
    public boolean allowOperation(int operation) {
        if (dataRows == null) {
            return false;
        }
        if (operation != IDataRow.INSERT) {
            if (row == null) {
                return false;
            }
        }
        if (dataEvents == null) {
            if (operation != IDataRow.INSERT) {            
                row.setAction(operation);
            }
            return true;
        }
        if (!dataEvents.onAllowOperation()){
            return false;
        }
        if (operation != IDataRow.INSERT) {
            row.setAction(operation);
        }        
        return true;
    }

    /**
     * Busca y devuelve el valor de la clave primaria
     *
     * @return Valor de la clave primaria
     */
    @Override
    public Object getPrimaryKeyValue() {
        if (this.row == null) {
            return null;
        }
        return DataInfo.getIdvalue(row);
    }

    /**
     * Asigna un valor a la clave primaria.
     *
     * @param value valor a asignar.
     * @return Verdadero tuvo exito o falso si no.
     */
    @Override
    public boolean setPrimaryKeyValue(Object value) {
        /* No se puede modificar si es de solo lectura */
        if (!this.readWrite) {
            return false;
        }
        if (this.row == null) {
            return false;
        }
        return DataInfo.setIdvalue(row, value);
    }

    /**
     * Se ejcuta antes que el metodo refreshRow()
     */
    protected void beforeRefreshRow() {
        if (this.dataEvents != null) {
            this.dataEvents.beforeRefreshRow(row);
        }
    }

    /**
     * Se ejecuta por orden explicita o al moverse el puntero del registro
     *
     * @return retorna verdadero si tuvo exito o falso si no
     */
    @Override
    public boolean refreshRow() {
        if (row == null || row.getAction() == IDataRow.INSERT) {
            return false;
        }
        try {
            errorApp = null;
            this.beforeRefreshRow();
            //
            row = getDAO().refreshRow(row);
            this.getDataRows().set(recno, row);
            //
            this.afterRefreshRow();
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
            errorApp = ex;
            return false;
        }
        return true;
    }

    /**
     * Se ejecuta posterior al metodo refreshRow()
     */
    protected void afterRefreshRow() {
        if (this.dataEvents != null) {
            this.dataEvents.afterRefreshRow(row);
        }
    }

    /**
     * Se ejecuta antes del metodo insertRow
     *
     * @param newRow nuevo objeto o registro.
     * @return verdadero si tuvo exito o falso si no.
     */
    protected boolean beforeInsertRow(IDataRow newRow) {
        if (this.dataEvents != null) {
            return this.dataEvents.beforeInsertRow(newRow);
        }
        return true;
    }

    /**
     * Inserta una nueva fila a la lista dataRows
     *
     * @return retorna verdadero si tuvo exito o falso si no
     */
    @Override
    public boolean insertRow() {
        /* No se puede modificar si es de solo lectura */
        if (!this.readWrite) {
            return false;
        }
        T newRow;
        try {
            errorApp = null;
            newRow = this.getType().newInstance();
            if (isFieldExist("idempresa") || isMethodExist("setIdempresa")) {
                newRow.setValue("idempresa", getIdempresa());
            }
            else if(isFieldExist("idcompany") || isMethodExist("setIdcompany")) {
                newRow.setValue("idcompany", getIdcompany());
            }
            newRow.setAction(IDataRow.INSERT);
            if (!this.beforeInsertRow(newRow)) {
                return false;
            }
            //
            newRow.setDefaults();
            //
            dataRows.add(newRow);
            this.moveLast();
            //
            this.afterInsertRow();
            return true;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
            errorApp = ex;
        }
        return false;
    }

    /**
     * Copia los datos de un registro y lo inserta como nuevo
     *
     * @return retorna verdadero si tuvo exito o falso si no
     */
    @Override
    public boolean insertRowFrom() {
        /* No se puede modificar si es de solo lectura */
        if (!this.readWrite) {
            return false;
        }
        if (row == null) {
            return false;
        }
        try {
            errorApp = null;
            T newRow = (T) row.clone();
            newRow.setAction(IDataRow.INSERT);
            if (!this.beforeInsertRow(newRow)) {
                return false;
            }
            dataRows.add(newRow);
            this.moveLast();
            //
            this.afterInsertRow();
            return true;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
            errorApp = ex;
        }
        return false;
    }

    /**
     * Se ejecuta posterior al metodo insertRow()
     */
    protected void afterInsertRow() {
        if (this.dataEvents != null) {
            this.dataEvents.afterInsertRow(row);
        }
    }

    /**
     * Se ejecuta antes del metodo deleteRow()
     *
     * @return retorna verdadero si tuvo exito o falso si no
     */
    protected boolean beforeDeleteRow() {
        if (this.dataEvents != null) {
            return this.dataEvents.beforeDeleteRow(row);
        }
        return true;
    }

    /**
     * Marca una fila como borrado
     *
     * @return retorna verdadero si tuvo exito o falso si no
     */
    @Override
    public boolean deleteRow() {
        /* No se puede modificar si es de solo lectura */
        if (!this.readWrite) {
            return false;
        }
        if (row == null) {
            return false;
        }
        try {
            errorApp = null;
            if (!this.beforeDeleteRow()) {
                return false;
            }
            //
            row.delete();
            //
            this.afterDeleteRow();
            return true;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
            errorApp = ex;
        }
        return false;
    }

    /**
     * Se ejecuta despues del metodo deleteRow()
     */
    protected void afterDeleteRow() {
        if (this.dataEvents != null) {
            this.dataEvents.afterDeleteRow();
        }
    }

    //TODO Implementar copyFrom
    @Override
    public void copyFrom(String idcompany, String companyName, String xmlTag, String tableCopy) {
        /* No se puede modificar si es de solo lectura */
    }


    /**
     * Valida los valores ingresados en el registro.
     *
     * @return un objeto conteniendo el resultado de la validación.
     * @throws Exception
     */
    @Override
    public Map<String, IErrorReg> checkDataRow() throws Exception {
        if (row == null) {
            return null;
        }
        if (this.row.getAction() == 0) {
            return null;
        }
        if (this.getDAO().getDataService() == null) {
            this.row.setErrors((Map<String, IErrorReg>) null);
            this.row.setRowChecked(true);
            return null;
        }
        // Validar los datos utilizando checkDataRow del DataService del objeto
        String sessionId = null;
        if (getDAO().getUserSession() != null) {
            sessionId = getDAO().getUserSession().getSessionId();
        }
        Map<String, IErrorReg> errorMap = this.getDAO().getDataService().checkDataRow(sessionId, row);
        if (errorMap == null || errorMap.isEmpty()) {
            this.row.setErrors((Map<String, IErrorReg>) null);
            this.row.setRowChecked(true);
            return null;
        }
        return errorMap;
    }

    /**
     * Se ejecuta antes del metodo update
     *
     * @param allRows
     * @return retorna verdadero si tuvo exito o falso si hubo algún error.
     */
    protected boolean beforeUpdate(boolean allRows) {
        if (this.dataEvents != null) {
            return this.dataEvents.beforeUpdate(allRows);
        }
        return true;
    }

    /**
     * Se ejecuta antes del metodo update
     *
     * @param dataSet set de datos (puede existir más de una lista de objetos en
     * este dataobject)
     * @return retorna verdadero si tuvo exito o falso si hubo algún error.
     */
    protected boolean beforeUpdate(IDataSet dataSet) {
        boolean result = true;
        Map<String, IDataObject> map = dataSet.getMapDataObject();
        if (map.isEmpty()){
            if (this.getDataEvents() != null){
                result = this.getDataEvents().beforeUpdate(true);
            }
        }
        for (Map.Entry<String, IDataObject> entry : map.entrySet()) {
            if (entry.getValue().getDataEvents() != null) {
                result = entry.getValue().getDataEvents().beforeUpdate(true);
                if (!result){
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Se ejecuta antes del metodo checkData
     *
     * @param allRows si se va a validar todos los registros modificados o solo
     * el actual.
     */
    protected void beforeCheckData(boolean allRows) {
        if (this.dataEvents != null) {
            this.dataEvents.beforeCheckData(allRows);
        }
    }

    /**
     * Valida un registro o todos los registros modificados antes de la
     * grabación en la base de datos.
     *
     * @param allRows si se va a validar todos los registros modificados o solo
     * @return verdadero si tuvo exito o falso si no.
     */
    @Override
    public boolean checkData(boolean allRows) {
        boolean showDeleted = showDeletedRow;
        try {
            showDeletedRow = true;
            //Ejecutar beforeCheckData
            this.beforeCheckData(allRows);

            int rowNumber = this.getRecno();
            int begin = 0;
            int end = this.dataRows.size();
            if (!allRows) {
                begin = this.recno;
                end = this.recno + 1;
            }
            //Recorrer para chequear todas las filas a insertar, actualizar o borrar
            for (int i = begin; i < end; i++) {
                this.goTo(i);
                if (this.getRow().getAction() > 0) {
                    // Verificar si el registro fue chequeado
                    if (!this.getRow().isRowChecked()) {
                        row.setErrors(this.checkDataRow());
                    }
                    if (this.getRow().getErrors() != null && this.getRow().getErrors().size() > 0) {
                        //Ejecutar afterCheckData
                        this.afterCheckData(allRows);
                        this.goTo(rowNumber);
                        showDeletedRow = showDeleted;
                        return false;
                    }
                }
            }
            //Ejecutar afterCheckData
            this.afterCheckData(allRows);
            this.goTo(rowNumber);
            showDeletedRow = showDeleted;
            return true;
        } catch (Exception ex) {
            showDeletedRow = showDeleted;
            ErrorManager.showError(ex, LOGGER);
            errorApp = ex;
        }
        return false;
    }

    /**
     * Valida los registros modificados contenidos en los sets de datos antes de
     * la grabación en la base de datos.
     *
     * @param dataSet set de datos (puede existir más de una lista de objetos en
     * este dataobject)
     * @return verdadero si tuvo exito o falso si no.
     */
    protected boolean checkData(IDataSet dataSet) {
        boolean result = true;
        Map<String, IDataObject> map = dataSet.getMapDataObject();
        if (map.isEmpty()){
            if (!this.checkData(true)){
                result = false;
            }
        }
        for (Map.Entry<String, IDataObject> entry : map.entrySet()) {
            if (!entry.getValue().checkData(true)){
                result = false;
            }
        }
        return result;
    }

    /**
     * Se ejecuta posterior al metodo checkData
     *
     * @param allRows
     */
    protected void afterCheckData(boolean allRows) {
        if (this.dataEvents != null) {
            this.dataEvents.afterCheckData(allRows);
        }
    }

    /**
     * Actualiza los datos en la base de datos
     *
     * @param allRows determina si se graba los datos del registro actual o
     * todos los registros modificados
     * @return verdadero si tuvo exito o falso si no.
     */
    @Override
    public boolean update(boolean allRows) {
        /* No se puede modificar si es de solo lectura */
        if (!this.readWrite) {
            return false;
        }
        IDataResult dataResult;
        try {
            errorApp = null;
            if (!this.beforeUpdate(allRows)) {
                return false;
            }
            //Chequear las filas en busca de errores
            if (!this.checkData(allRows)) {
                return false;
            }
            //
            if (allRows) {
                dataResult = getDAO().update(dataRows);
                row = dataRows.get(recno);
                if (!dataResult.isSuccessFul()) {
                    errorApp = new Exception(dataResult.getErrorMsg());
                    return false;
                }
                row.setOldValues();
            } else {
                dataResult = getDAO().update(row);
                // Asignar el registro resultante de la actualización
                row = (T) dataResult.getRowUpdated();
                dataRows.set(recno, row);
                if (!dataResult.isSuccessFul()) {
                    errorApp = new Exception(dataResult.getErrorMsg());
                    return false;
                }
                row.setOldValues();
                // Eliminar de la lista local el registro actual si esta marcado para ser borrado
                if (!dataResult.isRemoveDeleted()
                        && row.getAction() == IDataRow.DELETE) {
                    removeRow();
                }
            }
            this.afterUpdate(allRows);
            return true;
        } catch (Exception ex) {
            errorApp = ex;
            ErrorManager.showError(ex, LOGGER);
        }
        return false;
    }

    /**
     * Actualiza los datos en la base de datos
     *
     * @param dataSet set de datos (puede existir más de una lista de objetos en
     * este dataobject)
     * @return verdadero si tuvo exito o falso si no.
     */
    @Override
    public boolean update(IDataSet dataSet) {
        /* No se puede modificar si es de solo lectura */
        if (!this.readWrite) {
            return false;
        }
        IDataResult dataResult;
        try {
            errorApp = null;
            if (!this.beforeUpdate(dataSet)) {
                return false;
            }
            //Chequear las filas en busca de errores
            if (!this.checkData(dataSet)) {
                return false;
            }
            dataResult = getDAO().update(dataSet);
            if (!dataResult.isSuccessFul()) {
                errorApp = new Exception(dataResult.getErrorMsg());
                return false;
            }
            row.setOldValues();
            // Eliminar de la lista local el registro actual si esta marcado para ser borrado
            if (row.getAction() == IDataRow.DELETE) {
                if (!dataResult.isRemoveDeleted()) {
                    removeRow();
                } else {
                    movePrevious();
                }
            }
            this.afterUpdate(dataSet);
            return true;
        } catch (Exception ex) {
            errorApp = ex;
            ErrorManager.showError(ex, LOGGER);
        }
        return false;
    }

    /**
     * Se ejecuta posterior al metodo update()
     *
     * @param allRows
     */
    protected void afterUpdate(boolean allRows) {
        if (this.dataEvents != null) {
            this.dataEvents.afterUpdate(allRows);
        }
    }

    /**
     * Se ejecuta posterior al metodo update()
     *
     * @param dataSet set de datos (puede existir más de una lista de objetos en
     * este dataobject)
     */
    protected void afterUpdate(IDataSet dataSet) {
        Map<String, IDataObject> map = dataSet.getMapDataObject();
        if (map.isEmpty()){
            if (this.getDataEvents() != null){
                this.getDataEvents().afterUpdate(true);
            }
        }
        for (Map.Entry<String, IDataObject> entry : map.entrySet()) {
            if (entry.getValue().getDataEvents() != null) {
                entry.getValue().getDataEvents().afterUpdate(true);
            }
        }
    }

    /**
     * Se ejecuta antes de cerrar el dataObject
     */
    protected void beforeClose() {
        if (this.dataEvents != null) {
            this.dataEvents.beforeClose();
        }
    }

    /**
     * Revierte las modificaciones realizadas en el registro actual que aún no
     * fuerón actualizadas en la base de datos
     *
     * @return veradero si tuvo exito o falso si no
     */
    @Override
    public boolean revert() {
        if (row == null) {
            return true;
        }
        try {
            errorApp = null;
            if (row.getAction() == IDataRow.INSERT) {
                dataRows.remove(recno);
                goTo(recno);
            } else if (row.getAction() != 0) {
                refreshRow();
            }
        } catch (Exception ex) {
            errorApp = ex;
            ErrorManager.showError(ex, LOGGER);
            return false;
        }
        return true;
    }

    /**
     * Revierte las modificaciones realizadas que aún no fuerón actualizadas en
     * la base de datos
     *
     * @param allRows verdadero en todos los registros, falso solo en el
     * registro actual.
     * @return veradero si tuvo exito o falso si no
     */
    @Override
    public boolean revert(Boolean allRows) {
        if (dataRows == null){
            LOGGER.error("El dataobject no esta abierto");            
            return false;
        }
        boolean showDeleted = showDeletedRow;        
        setShowDeletedRow(true);
        if (allRows) {
            // Recorrer la lista y eliminar los insertados y el resto volver 
            // a su estado original
            int rec = recno;
            int i = 0;
            while (dataRows.size() > i) {
                T r = dataRows.get(i);
                if (r.getAction() > 0) {
                    if (r.getAction() == IDataRow.INSERT) {
                        dataRows.remove(i);
                        continue;
                    } else {
                        goTo(i);
                        refreshRow();
                    }
                }
                i++;
            }
            goTo(rec);
        } else {
            setShowDeletedRow(showDeleted);
            return revert();
        }
        setShowDeletedRow(showDeleted);        
        return true;
    }

    /**
     * Revierte las modificaciones realizadas que aún no fuerón actualizadas en
     * la base de datos
     *
     * @param dataSet set de datos (puede existir más de una lista de objetos en
     * este dataobject)
     * @return veradero si tuvo exito o falso si no
     */
    @Override
    public boolean revert(IDataSet dataSet) {
        boolean result = true;
        Map<String, IDataObject> map = dataSet.getMapDataObject();
        for (Map.Entry<String, IDataObject> entry : map.entrySet()) {
            result = entry.getValue().revert(true);
            if (!result) {
                break;
            }
        }
        return result;
    }

    /**
     * Limpia la lista de datos (Rows)
     */
    @Override
    public void close() {
        this.beforeClose();
        //
        this.recno = 0;
        this.row = null;
        this.dataRows = null;
        //
        this.afterClose();
    }

    /**
     * Se ejecuta posterior al metodo close()
     */
    protected void afterClose() {
        if (this.dataEvents != null) {
            this.dataEvents.afterClose();
        }
    }

    /**
     * Remueve los elementos marcados para ser borrado de la lista de registros.
     */
    private void removeRow() {
        if (dataRows == null){
            LOGGER.error("El dataobject no esta abierto");            
            return;
        }
        // Eliminar de la lista local el registro actual si esta marcado para ser borrado
        if (row != null && row.getAction() == IDataRow.DELETE && dataRows.size() > recno) {
            dataRows.remove(recno);
            movePrevious();
        }
    }
    
    @Override
    public Map<String, String> getFilters(){
        return filters;
    }
    
    @Override
    public void addFilter(String filter){
        String key = String.valueOf(filters.size());
        addFilter(key, filter);            
    }
    
    @Override
    public void addFilter(String key, String filter){
        if (filters == null){
            filters = new HashMap();
        }
        filters.put(key, filter);
    }

    @Override
    public void removeFilter(){
        filters.remove(filters.size()-1);
    }

    @Override
    public void removeFilter(String key){
        filters.remove(key);
    }

    @Override
    public String getFilterSentence(boolean noMain){
        String allFilters = "";
        if (!noMain){
            allFilters = Fn.nvl(filter, "");
        }
        String operador = "";
        for (Map.Entry<String, String> entry : filters.entrySet()){
            if (Strings.isNullorEmpty(entry.getValue())){
                continue;
            }
            if (!allFilters.isEmpty()){
                operador = " and ";
            }
            allFilters += operador + entry.getValue();
        }
        return allFilters;
    }
}

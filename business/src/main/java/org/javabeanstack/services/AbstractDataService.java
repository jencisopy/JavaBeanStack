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
package org.javabeanstack.services;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.JoinColumn;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.exceptions.SessionError;
import org.javabeanstack.security.IUserSession;
import org.javabeanstack.data.DataInfo;
import org.javabeanstack.data.model.DataResult;
import org.javabeanstack.data.IDataResult;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.annotation.CheckMethod;
import org.javabeanstack.data.IDBConnectFactory;
import org.javabeanstack.data.IDBManager;
import org.javabeanstack.datactrl.IDataObject;
import org.javabeanstack.data.IDataSet;
import org.javabeanstack.data.IGenericDAO;
import org.javabeanstack.error.ErrorReg;
import org.javabeanstack.util.Fn;

/**
 * Esta clase deriva de AbstractDAO, a travéz de ella se recupera, válida y se
 * graban los registros en la base de datos. Es un ejb que se ejecuta en la capa
 * de la lógica del negocio.
 *
  * @author Jorge Enciso
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public abstract class AbstractDataService implements IDataService {
    private static final Logger LOGGER = Logger.getLogger(AbstractDataService.class);
    protected List<Method> methodList = this.setListCheckMethods();
    @EJB
    protected IGenericDAO dao;

    /**
     * Devuelve la unidad de persistencia asociado a la empresa en la cual
     * inicio sesión el usuario.
     *
     * @param sessionId identificador de la sesión.
     * @return unidad de persistencia.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    protected String getPersistentUnit(String sessionId) {
        IUserSession userSession = getUserSession(sessionId);
        if (userSession == null || userSession.getUser() == null) {
            return null;
        }
        return userSession.getPersistenceUnit();
    }

    @Override
    public IUserSession getUserSession(String sessionId) {
        return dao.getUserSession(sessionId);
    }    

    @Override
    public Map<String, Object> getEntityManagerProp(String persistUnit) {
        return dao.getEntityManagerProp(persistUnit);
    }

    @Override
    public Map<String, Object> getPersistUnitProp(String persistUnit) {
        return dao.getPersistUnitProp(persistUnit);
    }

    @Override
    public String getDataEngine(String persistentUnit) {
        return dao.getDataEngine(persistentUnit);
    }

    @Override
    public String getSchema(String persistentUnit) {
        return dao.getSchema(persistentUnit);
    }
    
    
    /**
     * Genera una lista de los metodos que existen con el proposito de validar
     * datos.
     *
     * @return lista de metodos.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    protected final List<Method> setListCheckMethods() {
        List methods = new ArrayList();
        for (Method method : this.getClass().getDeclaredMethods()) {
            CheckMethod anotation = method.getAnnotation(CheckMethod.class);
            /* Ejecutar los metodos cuyo nombre inician con check */
            if (anotation != null) {
                methods.add(method);
            }
        }
        return methods;
    }

    // TODO ver este metodo.
    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public <T extends IDataRow> T setListFieldCheck(T row) {
        if (row != null && row.getFieldChecked() == null) {
            String fieldName;
            String key;
            String namePrefix;
            CheckMethod anotation;
            Map<String, Boolean> fieldChecked = new HashMap<>();
            for (Method method : this.getClass().getDeclaredMethods()) {
                namePrefix = method.getName().toLowerCase().substring(0, 5);
                anotation = method.getAnnotation(CheckMethod.class);
                if ("check".equals(namePrefix) || anotation != null) {
                    if (anotation == null) {
                        fieldName = method.getName().toLowerCase().substring(5);
                    } else {
                        fieldName = anotation.fieldName().toLowerCase();
                    }
                    key = "_" + fieldName;
                    fieldChecked.put(key, true);
                }
            }
            row.setFieldChecked(fieldChecked);
        }
        return row;
    }

    @Override
    public <T extends IDataRow> T findById(Class<T> entityClass, String sessionId, Object id) throws Exception {
        return dao.findById(entityClass, sessionId, id);
    }

    @Override
    public <T extends IDataRow> T findByUk(String sessionId, T ejb) throws Exception {
        return dao.findByUk(sessionId, ejb);
    }

    @Override
    public <T extends IDataRow> List<T> find(Class<T> entityClass, String sessionId) throws Exception {
        return dao.find(entityClass, sessionId);
    }

    @Override
    public <T extends IDataRow> List<T> find(Class<T> entityClass, String sessionId, String order, String filter, Map<String, Object> params) throws Exception {
        return dao.find(entityClass, sessionId, order, filter, params);        
    }

    @Override
    public <T extends IDataRow> List<T> find(Class<T> entityClass, String sessionId, String order, String filter, Map<String, Object> params, int first, int max) throws Exception {
        return dao.find(entityClass, sessionId, order, filter, params, first, max);
    }

    @Override
    public List<Object> findByNativeQuery(String sessionId, String queryString, Map<String, Object> parameters) throws Exception {
        return dao.findByNativeQuery(sessionId, queryString, parameters);
    }

    @Override
    public List<Object> findByNativeQuery(String sessionId, String queryString, Map<String, Object> parameters, int first, int max) throws Exception {
        return dao.findByNativeQuery(sessionId, queryString, parameters, first, max);
    }

    @Override
    public <T extends IDataRow> T findByQuery(String sessionId, String queryString, Map<String, Object> parameters) throws Exception {
        return dao.findByQuery(sessionId, queryString, parameters);
    }

    @Override
    public <T extends IDataRow> List<T> findListByQuery(String sessionId, String queryString, Map<String, Object> parameters) throws Exception {
        return dao.findListByQuery(sessionId, queryString, parameters);
    }

    @Override
    public <T extends IDataRow> List<T> findListByQuery(String sessionId, String queryString, int first, int max) throws Exception {
        return dao.findListByQuery(sessionId, queryString, first, max);
    }

    @Override
    public <T extends IDataRow> List<T> findListByQuery(String sessionId, String queryString, Map<String, Object> parameters, int first, int max) throws Exception {
        return dao.findListByQuery(sessionId, queryString, parameters, first, max);
    }

    @Override
    public <T extends IDataRow> T findByNamedQuery(String sessionId, String namedQuery, Map<String, Object> parameters) throws Exception {
        return dao.findByNamedQuery(sessionId, namedQuery, parameters);
    }

    @Override
    public <T extends IDataRow> List<T> findListByNamedQuery(String sessionId, String namedQuery, Map<String, Object> parameters) throws Exception {
        return dao.findListByNamedQuery(sessionId, namedQuery, parameters);
    }

    @Override
    public <T extends IDataRow> List<T> findListByNamedQuery(String sessionId, String namedQuery, int first, int max) throws Exception {
        return dao.findListByNamedQuery(sessionId, namedQuery, first, max);
    }

    @Override
    public <T extends IDataRow> List<T> findListByNamedQuery(String sessionId, String namedQuery, Map<String, Object> parameters, int first, int max) throws Exception {
        return dao.findListByNamedQuery(sessionId, namedQuery, parameters, first, max);
    }

    @Override
    public <T extends IDataRow> T refreshRow(String sessionId, T row) throws Exception {
        return dao.refreshRow(sessionId, row);
    }

    @Override
    public Long getCount(String sessionId, String queryString, Map<String, Object> parameters) throws Exception {
        return dao.getCount(sessionId, queryString, parameters);
    }

    @Override
    public Long getCount2(String sessionId, String queryString, Map<String, Object> parameters) throws Exception {
        return dao.getCount2(sessionId, queryString, parameters);
    }

    
    /**
     * Verifica que sea valida la sesión de usuario para poder realizar las
     * operaciones.
     *
     * @param sessionId identificador de la sesión.
     * @throws SessionError error sesión invalida.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    protected final void checkUserSession(String sessionId) throws SessionError {
        IUserSession userSession = getUserSession(sessionId);
        if (userSession == null || userSession.getUser() == null) {
            throw new SessionError("El identificador de la sesión es inválido");
        }
    }

    
    /**
     * Válida que la clave del registro no exista en la tabla.
     *
     * @param <T>
     * @param row registro con los datos.
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones sobre la base de datos.
     * @return verdadero si no esta duplicado y falso si lo esta.
     */
    @Override
    public <T extends IDataRow> boolean checkUniqueKey(String sessionId, T row) {
        try {
            // Buscar registro por la clave unica 
            if (row.getQueryUK() == null) {
                return true;
            }
            T row2 = findByUk(sessionId, row);
            // Si encontro un registro
            if (row2 != null) {
                // Y la operación es agregar 
                if (row.getAction() == IDataRow.INSERT) {
                    return false;
                }
                // Y no son el mismo objeto devolver error
                if (row.hashCode() != row2.hashCode()) {
                    return false;
                }
            }
        } catch (Exception ex) {
            ErrorManager.showError(ex, Logger.getLogger(AbstractDataService.class));
        }
        return true;
    }

    /**
     * Válida que los campos definidos como clave foranea existan en la tabla
     * relacionada o sea nulo si así lo permite el campo.
     *
     * @param <T>
     * @param row registro de datos.
     * @param fieldName nombre del campo
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones sobre la base de datos.
     * @return verdadero si no esta duplicado y falso si lo esta.
     */
    @Override
    public <T extends IDataRow> boolean checkForeignKey(String sessionId, T row, String fieldName) {
        boolean result = true;
        try {
            Class clase = row.getClass();
            Field field = DataInfo.getDeclaredField(clase, fieldName);
            // Si el campo no existe
            if (field == null) {
                result = true;
            } // Si no es foreignkey
            else if (!DataInfo.isForeignKey(row.getClass(), fieldName)) {
                result = true;
            } else {
                Boolean nullable = field.getAnnotation(JoinColumn.class).nullable();
                // Si el valor es nulo y no se permite nulo
                if (row.getValue(fieldName) == null && !nullable) {
                    result = false;
                } // Buscar valor del foreignkey
                else if (row.getValue(fieldName) != null) {
                    Class fieldType = row.getFieldType(fieldName);
                    Object id = row.getValue(fieldName);
                    IDataRow fieldValue = findById(fieldType, sessionId, id);
                    if (fieldValue == null) {
                        result = false;
                    }
                }
            }
        } catch (Exception exp) {
            result = false;
        }
        return result;
    }

    /**
     * Chequea la validez de los datos del registro, ejecutando todos los
     * metodos marcados como validadores (CheckMethod).
     *
     * @param <T>
     * @param row registro de datos.
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones sobre la base de datos.
     * @return un objeto map conteniendo el resultado de las validaciones de
     * todos los campos, en caso de existir algún error, existe su objeto de
     * error (IErrorReg) asociado a una clave que en este caso es el nombre del
     * campo.
     */
    @Override
    public <T extends IDataRow> Map<String, IErrorReg> checkDataRow(String sessionId, T row) {
        Map<String, IErrorReg> errors = new HashMap<>();
        String fieldName;
        int[] operacion = {IDataRow.INSERT, IDataRow.UPDATE};
        IErrorReg result;
        CheckMethod anotation;
        try {
            // Chequeo de clave duplicada solo si la operación es agregar o modificar
            if (Fn.inList(row.getAction(), IDataRow.INSERT, IDataRow.UPDATE)) {
                if (!checkUniqueKey(sessionId, row)) {
                    errors.put("UNIQUEKEY",
                            new ErrorReg("Este registro ya existe",
                                    50001,
                                    DataInfo.getUniqueFields(row.getClass())));
                    return errors;
                }
            }
            // Ejecutar control de foreignkey
            // Chequeo del foreignkey solo si la operación es agregar o modificar
            if (Fn.inList(row.getAction(), IDataRow.INSERT, IDataRow.UPDATE)) {
                for (Field field : DataInfo.getDeclaredFields(row.getClass())) {
                    fieldName = field.getName();
                    if (!checkForeignKey(sessionId, row, fieldName)) {
                        errors.put(fieldName.toLowerCase(),
                                new ErrorReg("Dejo en blanco este dato o no existe el registro",
                                        50013,
                                        fieldName));
                    }
                }
            }
        } catch (Exception exp) {
            ErrorManager.showError(exp, LOGGER);
        }
        // Ejecutar metodos de chequeo de datos
        for (Method method : this.methodList) {
            anotation = method.getAnnotation(CheckMethod.class);
            if (anotation == null) {
                fieldName = method.getName().toLowerCase().substring(5);
            } else {
                fieldName = anotation.fieldName();
                operacion = anotation.action();
            }
            // Si existe un error previo sobre este campo continuar con las otras validaciones
            if (errors.containsKey(fieldName.toLowerCase())) {
                continue;
            }
            try {
                method.setAccessible(true);
                // La validación se ejecuta dependiendo de la operación (agregar, modificar, borrar)
                if (Fn.inList(row.getAction(), operacion)) {
                    result = (IErrorReg) method.invoke(this, row, sessionId);
                    if (result != null && !"".equals(result.getMessage())) {
                        errors.put(fieldName.toLowerCase(), result);
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                ErrorManager.showError(ex, Logger.getLogger(AbstractDataService.class));
            }
        }
        row.setErrors(errors);
        row.setRowChecked(true);
        return errors;
    }


    /**
     * Se ejecuta en el metodo save, valida los datos del registro
     * (checkDataRow) y devuelve el resultado en un objeto IDataResult
     *
     * @param <T>
     * @param row registro de datos.
     * @param sessionId identificador de la sesión.
     * @return objeto con el resultado del proceso de validación.
     */
    protected final <T extends IDataRow>  IDataResult checkDataResult(String sessionId, T row) {
        IDataResult dataResult = new DataResult();
        dataResult.setSuccess(Boolean.TRUE);
        List<IDataRow> ejbsRes = new ArrayList();
        // Validar el registro
        Map<String, IErrorReg> errors = this.checkDataRow(sessionId, row);
        ejbsRes.add(row);
        if (!errors.isEmpty()) {
            // Devolver el error si lo hubo
            dataResult.setSuccess(Boolean.FALSE);
            dataResult.setErrorsMap(errors);
        }
        dataResult.put("1", ejbsRes);
        return dataResult;
    }

    /**
     * Graba un registro en la base de datos.
     *
     * @param <T>
     * @param row registro de datos.
     * @param sessionId identificador de la sesión.
     * @return objeto resultado de la operación.
     * @throws SessionError
     */
    protected final <T extends IDataRow> IDataResult save(String sessionId, T row) throws SessionError {
        checkUserSession(sessionId);
        IDataResult dataResult;
        // Validar registro
        dataResult = this.checkDataResult(sessionId, row);
        if (!dataResult.getErrorsMap().isEmpty()) {
            // Devolver el error si lo hubo
            return dataResult;
        }
        // Grabar registro en la base de datos.
        dataResult = update(sessionId, row);
        return dataResult;
    }

    /**
     * Agrega un registro en la tabla
     *
     * @param <T>
     * @param row registro de datos.
     * @param sessionId identificador de la sesión.
     * @return objeto resultado de la operación.
     * @throws SessionError
     */
    @Override
    public <T extends IDataRow> IDataResult persist(String sessionId, T row) throws SessionError {
        row.setAction(IDataRow.INSERT);
        return save(sessionId, row);
    }

    /**
     * Modifica un registro en la tabla
     *
     * @param <T>
     * @param row registro de datos.
     * @param sessionId identificador de la sesión.
     * @return objeto resultado de la operación.
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public <T extends IDataRow> IDataResult merge(String sessionId, T row) throws SessionError {
        row.setAction(IDataRow.UPDATE);
        return save(sessionId, row);
    }

    /**
     * Borra un registro en la tabla
     *
     * @param <T>
     * @param row registro de datos.
     * @param sessionId identificador de la sesión.
     * @return objeto resultado de la operación.
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public <T extends IDataRow> IDataResult remove(String sessionId, T row) throws SessionError{
        row.setAction(IDataRow.DELETE);
        return save(sessionId, row);
    }

    /**
     * Prepara y envia los registros de una tabla según los parámetros asignados
     * 
     * @param <T>
     * @param sessionId identificador de la sesión.
     * @param type tipo de registro.
     * @param order orden de la consulta
     * @param filter filtro de la consulta
     * @param params parametros particulares
     * @param firstRow
     * @param maxRows maxima cantidad de registros.
     * @return una lista con los objetos/registros solicitados
     * @throws Exception 
     */
    @Override
    public <T extends IDataRow> List<T> getDataRows(String sessionId, Class<T> type, 
            String order, String filter, Map<String, Object> params, int firstRow, int maxRows) throws Exception{
        String query = getSelectCmd(sessionId, type, order, filter);
        if (maxRows == -1) {
            maxRows = 999999999;
        }
        List<T> dataRows;
        if (maxRows == 0) {
            dataRows = new ArrayList();
        } else {
            dataRows = (ArrayList<T>) dao.findListByQuery(sessionId, query, params, firstRow, maxRows);                
        }
        return dataRows;
    }
    
    /**
     * Setea el atributo selectcmd, sentencia que se ejecutara para recuperar
     * los datos
     * @param <T>
     * @param sessionId
     * @param type
     * @param order
     * @param filter
     * @return 
     */
    @Override
    public <T extends IDataRow> String getSelectCmd(String sessionId, Class<T> type, String order, String filter) {
        String comando; 
        String filtro="";
        
        filter = Fn.nvl(filter, "");
        order = Fn.nvl(order, "");
        
        IUserSession userSession = getUserSession(sessionId);        
        comando = "select o from " + type.getSimpleName() + " o ";
        if (userSession != null 
                        && !userSession.getPersistenceUnit().equals(IDBManager.CATALOGO)
                        && userSession.getDBFilter() != null){
            filtro = userSession.getDBFilter().getFilterExpr(type, "");
            if (!"".equals(filter)) {
                filtro += " and " + filter;
            }
        } else {
            filtro = filter;
        }
        //Filtro
        if (!"".equals(filtro)) {
            comando = comando + " where " + filtro;
        }
        //Orden
        if (!"".equals(order)) {
            comando = comando + " order by " + order;
        }
        return comando;
    }
    

    @Override
    public <T extends IDataRow> IDataResult update(String sessionId, T ejb) {
        return dao.update(sessionId, ejb);
    }

    @Override
    public IDataResult update(String sessionId, IDataObject ejbs) {
        return dao.update(sessionId,ejbs);
    }

    @Override
    public <T extends IDataRow> IDataResult update(String sessionId, List<T> ejbs) {
        return dao.update(sessionId, ejbs);
    }

    @Override
    public IDataResult update(String sessionId, IDataSet dataSet) {
        return dao.update(sessionId, dataSet);        
    }

    @Deprecated    
    @Override
    public Connection getConnection(String sessionId) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Deprecated
    @Override
    public Connection getConnection(String sessionId, IDBConnectFactory conn) {
        throw new UnsupportedOperationException("Not supportedt");
    }
    

    @Deprecated
    @Override
    public <T> List<T> findAll(Class<T> entityClass, String sessionId) throws Exception {
        throw new UnsupportedOperationException("Not supported."); 
    }
    
    @Deprecated
    @Override
    public IErrorReg sqlExec(String sessionId, String queryString, Map<String, Object> parameters) throws Exception {
        throw new UnsupportedOperationException("Not supported"); 
    }

    @Deprecated
    @Override
    public <T extends IDataRow> List<T> getData(String sessionId, String queryString, int maxRows, boolean noCache) throws Exception {
        throw new UnsupportedOperationException("Not supported");
    }

    @Deprecated    
    @Override
    public <T extends IDataRow> List<T> getData(Query query) throws Exception {
        throw new UnsupportedOperationException("Not supported.");
    }
    
    @Deprecated    
    @Override
    public <T extends IDataRow> List<T> refreshAll(String sessionId, List<T> rows) throws Exception {
        throw new UnsupportedOperationException("Not supported.");
    }
}

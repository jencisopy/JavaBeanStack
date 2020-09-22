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
package org.javabeanstack.data.services;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
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
import javax.validation.constraints.NotNull;
import org.apache.log4j.Logger;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.exceptions.SessionError;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.data.DataInfo;
import org.javabeanstack.data.model.DataResult;
import org.javabeanstack.data.IDataResult;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.annotation.CheckMethod;
import org.javabeanstack.annotation.ColumnFunction;
import org.javabeanstack.data.IDBConnectFactory;
import org.javabeanstack.data.IDBFilter;
import org.javabeanstack.data.IDBLinkInfo;
import org.javabeanstack.data.IDBManager;
import org.javabeanstack.datactrl.IDataObject;
import org.javabeanstack.data.IDataSet;
import org.javabeanstack.data.IGenericDAO;
import org.javabeanstack.data.model.DataSet;
import org.javabeanstack.error.ErrorReg;
import org.javabeanstack.exceptions.CheckException;
import org.javabeanstack.security.IOAuthConsumerData;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.ParamsUtil;
import org.javabeanstack.util.Strings;
import static org.javabeanstack.util.Strings.isNullorEmpty;
import org.javabeanstack.annotation.CheckForeignkey;

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
    protected List<Method> methodList = this.getListCheckMethods();
    @EJB
    protected IGenericDAO dao;

    @Override
    public IDBLinkInfo getDBLinkInfo(String sessionId) {
        return dao.getDBLinkInfo(sessionId);
    }

    /**
     * Devuelve la unidad de persistencia asociado a la empresa en la cual
     * inicio sesión el usuario.
     *
     * @param sessionId identificador de la sesión.
     * @return unidad de persistencia.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    protected String getPersistentUnit(String sessionId) {
        IDBLinkInfo dbInfo = getDBLinkInfo(sessionId);
        return dbInfo.getPersistUnit();
    }

    /**
     *
     * @param sessionId id de sesión del usuario
     * @return el objeto userSession con la información de la sesión
     */
    @Override
    public IUserSession getUserSession(String sessionId) {
        return dao.getUserSession(sessionId);
    }

    /**
     * Busca y devuelve el valor de una propiedad solicitada del entity manager
     *
     * @param persistUnit unidad de persistencia
     * @return el valor de una propiedad del entity manager
     */
    @Override
    public Map<String, Object> getEntityManagerProp(String persistUnit) {
        return dao.getEntityManagerProp(persistUnit);
    }

    /**
     * Busca y devuelve el valor de una propiedad solicitada 1
     *
     * @param persistUnit unidad de persistencia.
     * @return devuelve un map con todas las propiedades de la unidad de
     * persistencia solicitada
     */
    @Override
    public Map<String, Object> getPersistUnitProp(String persistUnit) {
        return dao.getPersistUnitProp(persistUnit);
    }

    /**
     *
     * @param persistentUnit unidad de persistencia
     * @return devuelve el nombre del motor de la base de datos
     */
    @Override
    public String getDataEngine(String persistentUnit) {
        return dao.getDataEngine(persistentUnit);
    }

    /**
     *
     * @param persistentUnit unidad de persistencia
     * @return devuelve el schema
     */
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
    protected final List<Method> getListCheckMethods() {
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

    /**
     * Asigna el map fieldsChecked, que básicamente va a informar que campos se
     * verificarón y cuales no
     *
     * @param <T>
     * @param row objeto ejb
     * @return Objeto ejb con atributo fieldsChecked preparado para recibir la
     * información de las validaciones.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public <T extends IDataRow> T setFieldsToCheck(T row) {
        if (row != null && row.getFieldsChecked() == null) {
            String fieldName;
            String key;
            CheckMethod anotation;
            Map<String, Boolean> fieldsChecked = new HashMap<>();
            for (Method method : this.getClass().getDeclaredMethods()) {
                anotation = method.getAnnotation(CheckMethod.class);
                if (anotation != null) {
                    fieldName = anotation.fieldName().toLowerCase();
                    key = "_" + fieldName;
                    fieldsChecked.put(key, true);
                }
            }
            row.setFieldsChecked(fieldsChecked);
        }
        return row;
    }

    /**
     * Devuelve un registro de una tabla dada
     *
     * @param <T>
     * @param entityClass clase mapeada a la tabla
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param id identificador del registro
     * @return un registro solicitado
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> T findById(Class<T> entityClass, String sessionId, Object id) throws Exception {
        return dao.findById(entityClass, sessionId, id);
    }

    /**
     * Devuelve un registro a travéz de su clave unica.
     *
     * @param <T>
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param ejb objeto ejb con los datos de la clave unica
     * @return un registro que cumple la condición de la clave unica solicitada.
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> T findByUk(String sessionId, T ejb) throws Exception {
        return dao.findByUk(sessionId, ejb);
    }

    /**
     * Devuelve una lista de registros de una tabla dada
     *
     * @param <T>
     * @param entityClass clase mapeada a la tabla
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @return lista de objetos
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> List<T> find(Class<T> entityClass, String sessionId) throws Exception {
        return dao.find(entityClass, sessionId);
    }

    /**
     * Devuelve una lista de registros de una tabla dada
     *
     * @param <T>
     * @param entityClass clase mapeada a la tabla
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param order
     * @param filter
     * @param params
     * @return lista de objetos
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> List<T> find(Class<T> entityClass, String sessionId, String order, String filter, Map<String, Object> params) throws Exception {
        return dao.find(entityClass, sessionId, order, filter, params);
    }

    /**
     * Devuelve una lista de registro de una tabla dada
     *
     * @param <T>
     * @param entityClass clase mapeada a la tabla
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param order
     * @param filter
     * @param params
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return lista de objetos
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> List<T> find(Class<T> entityClass, String sessionId, String order, String filter, Map<String, Object> params, int first, int max) throws Exception {
        return dao.find(entityClass, sessionId, order, filter, params, first, max);
    }

    /**
     *
     * @param <T>
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param queryString sentencia jpql
     * @param parameters parametros de la sentencia
     * @return un objeto con valores del registro de la tabla solicitada
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> T findByQuery(String sessionId, String queryString, Map<String, Object> parameters) throws Exception {
        return dao.findByQuery(sessionId, queryString, parameters);
    }

    /**
     *
     * @param <T>
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param queryString sentencia jpql
     * @param parameters parametros de la sentencia
     * @return una lista de objetos conteniendo los registros de la tabla
     * solicitada
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> List<T> findListByQuery(String sessionId, String queryString, Map<String, Object> parameters) throws Exception {
        return dao.findListByQuery(sessionId, queryString, parameters);
    }

    /**
     *
     * @param <T>
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param queryString sentencia jpql
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos conteniendo los registros de la tabla
     * solicitada
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> List<T> findListByQuery(String sessionId, String queryString, int first, int max) throws Exception {
        return dao.findListByQuery(sessionId, queryString, first, max);
    }

    /**
     *
     * @param <T>
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param queryString sentencia jpql
     * @param parameters parametros de la sentencia
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos conteniendo los registros de la tabla
     * solicitada
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> List<T> findListByQuery(String sessionId, String queryString, Map<String, Object> parameters, int first, int max) throws Exception {
        return dao.findListByQuery(sessionId, queryString, parameters, first, max);
    }

    /**
     *
     * @param <T>
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param namedQuery namedQuery
     * @param parameters parámetros de la sentencia.
     * @return un objeto con los datos del registro de la tabla solicitada
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> T findByNamedQuery(String sessionId, String namedQuery, Map<String, Object> parameters) throws Exception {
        return dao.findByNamedQuery(sessionId, namedQuery, parameters);
    }

    /**
     *
     * @param <T>
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param namedQuery namedQuery
     * @param parameters parámetros de la sentencia.
     * @return una lista de objetos con los datos de los registros de la tabla
     * solicitada
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> List<T> findListByNamedQuery(String sessionId, String namedQuery, Map<String, Object> parameters) throws Exception {
        return dao.findListByNamedQuery(sessionId, namedQuery, parameters);
    }

    /**
     *
     * @param <T>
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos conteniendo los registros de la tabla
     * solicitada
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> List<T> findListByNamedQuery(String sessionId, String namedQuery, int first, int max) throws Exception {
        return dao.findListByNamedQuery(sessionId, namedQuery, first, max);
    }

    /**
     *
     * @param <T>
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param namedQuery namedQuery
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos con los datos de los registros de la tabla
     * solicitada
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> List<T> findListByNamedQuery(String sessionId, String namedQuery, Map<String, Object> parameters, int first, int max) throws Exception {
        return dao.findListByNamedQuery(sessionId, namedQuery, parameters, first, max);
    }

    /**
     *
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param queryString sentencia sql
     * @param parameters parámetros de la sentencia.
     * @return una lista de objetos con datos de los registros solicitados
     * @throws Exception
     */
    @Override
    public List<Object> findByNativeQuery(String sessionId, String queryString, Map<String, Object> parameters) throws Exception {
        return dao.findByNativeQuery(sessionId, queryString, parameters);
    }

    /**
     *
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param queryString sentencia sql
     * @param parameters parámetros de la sentencia.
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos con datos de los registros solicitados
     * @throws Exception
     */
    @Override
    public List<Object> findByNativeQuery(String sessionId, String queryString, Map<String, Object> parameters, int first, int max) throws Exception {
        return dao.findByNativeQuery(sessionId, queryString, parameters, first, max);
    }

    /**
     *
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param queryString sentencia sql
     * @param parameters parámetros de la sentencia.
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos con datos de los registros solicitados
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> List<T> findByNativeQuery(Class<T> clazz, String sessionId, String queryString, Map<String, Object> parameters, int first, int max) throws Exception {
        return dao.findByNativeQuery(clazz, sessionId, queryString, parameters, first, max);
    }

    /**
     * Refresca desde la base de datos los valores de un objeto.
     *
     * @param <T>
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param row objeto o registro a refrescar
     * @return objeto con los datos refrescados de la base de datos
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> T refreshRow(String sessionId, T row) throws Exception {
        return dao.refreshRow(sessionId, row);
    }

    /**
     * Calcula la cantidad de registros que devolveria una sentencia sql
     *
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param queryString sentencia jpql
     * @param parameters parámetros de la sentencia
     * @return cantidad de registros que debería devolver la sentencia.
     * @throws Exception
     */
    @Override
    public Long getCount(String sessionId, String queryString, Map<String, Object> parameters) throws Exception {
        return dao.getCount(sessionId, queryString, parameters);
    }

    /**
     * Calcula la cantidad de registros que devolveria una sentencia sql
     *
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param queryString sentencia sql
     * @param parameters parámetros de la sentencia
     * @return cantidad de registros que debería devolver la sentencia.
     * @throws Exception
     */
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
        if (isNullorEmpty(sessionId)) {
            return;
        }
        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
        if (isNullorEmpty(dbLinkInfo.getSessionOrTokenId())) {
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
                    Object id = ((IDataRow) row.getValue(fieldName)).getId();
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
        int[] operacion;
        IErrorReg result;
        CheckMethod anotation;
        row.setRowChecked(false);
        // Preparar el registro para las verificaciones.
        if (row.getFieldsChecked() == null || row.getFieldsChecked().isEmpty()) {
            setFieldsToCheck(row);
        }
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
            // Colocar valores por defectos en campos numericos y boolean
            if (Fn.inList(row.getAction(), IDataRow.INSERT, IDataRow.UPDATE)) {
                for (Field field : DataInfo.getDeclaredFields(row.getClass())) {
                    fieldName = field.getName();
                    // Si tiene una marca para no validar el foreignkey
                    CheckForeignkey annotation = field.getAnnotation(CheckForeignkey.class);
                    if (annotation != null && !annotation.check()) {
                        continue;
                    }
                    if (!checkForeignKey(sessionId, row, fieldName)) {
                        String message = "Dejo en blanco este dato o no existe el registro - " + fieldName;
                        if (annotation != null && !annotation.message().isEmpty()) {
                            message = annotation.message();
                        }
                        errors.put(fieldName.toLowerCase(),
                                new ErrorReg(message,
                                        50013,
                                        fieldName));
                    }
                    NotNull notNull = field.getAnnotation(NotNull.class);
                    //Colocar un valor por defecto si es nulo
                    if (notNull != null && row.getValue(fieldName) == null) {
                        setDefault(row, fieldName);
                    }
                }
            }
        } catch (Exception exp) {
            ErrorManager.showError(exp, LOGGER);
        }
        // Ejecutar metodos de chequeo de datos
        for (Method method : this.methodList) {
            anotation = method.getAnnotation(CheckMethod.class);
            fieldName = anotation.fieldName();
            operacion = anotation.action();
            // Si existe un error previo sobre este campo continuar con las otras validaciones
            if (errors.containsKey(fieldName.toLowerCase())) {
                continue;
            }
            try {
                method.setAccessible(true);
                // La validación se ejecuta dependiendo de la operación (agregar, modificar, borrar)
                if (Fn.inList(row.getAction(), operacion)) {
                    result = (IErrorReg) method.invoke(this, sessionId, row);
                    //Si el resultado es un error guardar información en el objeto errors
                    if (result != null && !"".equals(result.getMessage())) {
                        errors.put(fieldName.toLowerCase(), result);
                        row.setFieldChecked(fieldName, false);
                    } else {
                        // Paso la verificación del atributo
                        row.setFieldChecked(fieldName, true);
                    }
                }
            } catch (Exception ex) {
                row.setFieldChecked(fieldName, false);
                result = new ErrorReg(ErrorManager.getStackCause(ex), 0, fieldName);
                errors.put(fieldName.toLowerCase(), result);
                ErrorManager.showError(ex, Logger.getLogger(AbstractDataService.class));
            }
        }
        row.setErrors(errors);
        if (errors.isEmpty()) {
            row.setRowChecked(true);
        }
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
    protected final <T extends IDataRow> IDataResult checkDataResult(String sessionId, T row) {
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
    @Override
    public <T extends IDataRow> IDataResult save(String sessionId, T row) throws SessionError {
        checkUserSession(sessionId);
        IDataResult dataResult;
        // Validar registro
        dataResult = this.checkDataResult(sessionId, row);
        if (!dataResult.getErrorsMap().isEmpty()) {
            // Devolver el error si lo hubo
            return dataResult;
        }
        try {
            // Grabar registro en la base de datos.
            IDataSet dataSet = new DataSet();
            List<IDataRow> rows = new ArrayList();
            rows.add(row);
            String setKey = "1";
            if (row != null) {
                setKey = row.getClass().getSimpleName().toLowerCase();
            }
            dataSet.add(setKey, (List<IDataRow>) rows);
            dataResult = update(sessionId, dataSet);
            return dataResult;
        } catch (Exception ex) {
            dataResult.setSuccess(false);
            dataResult.setException(ex);
            dataResult.setErrorMsg(ErrorManager.getStackCause(ex));
            ErrorManager.showError(ex, LOGGER);
        }
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
    public <T extends IDataRow> IDataResult remove(String sessionId, T row) throws SessionError {
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
            String order, String filter, Map<String, Object> params, int firstRow, int maxRows) throws Exception {
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
     *
     * @param <T>
     * @param sessionId identificador de la sesión
     * @param type tipo o modelo de dato
     * @param order orden de la selección
     * @param filter filtro de la selección
     * @return sentencia JPQL que se ejecutará para recuperar los datos
     */
    @Override
    public <T extends IDataRow> String getSelectCmd(String sessionId, Class<T> type, String order, String filter) {
        String comando;
        String filtro = "";

        filter = Fn.nvl(filter, "");
        order = Fn.nvl(order, "");
        comando = "select o from " + type.getSimpleName() + " o ";
        IDBLinkInfo dbInfo = getDBLinkInfo(sessionId);
        IDBFilter dbFilter = dbInfo.getDBFilter();
        if (!dbInfo.getPersistUnit().equals(IDBManager.CATALOGO)
                && dbFilter != null) {
            filtro = Fn.nvl(dbFilter.getFilterExpr(type, ""), "");
            if (!"".equals(filter) && !filtro.isEmpty()) {
                filtro += " and (" + filter + ")";
            } else {
                filtro += filter;
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

    /**
     * Sincroniza un ejb con la base de datos.
     *
     * @param <T>
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param ejb objeto mapeado a un registro de una tabla.
     * @return Devuelve un objeto con el resultado de la grabación
     *
     */
    @Override
    public <T extends IDataRow> IDataResult update(String sessionId, T ejb) {
        isChecked(ejb);
        List<T> ejbs = new ArrayList<>();
        ejbs.add(ejb);
        return update(sessionId, ejbs);
    }

    /**
     * Sincroniza una lista de ejbs con la base de datos.
     *
     * @param ejbs lista de objetos mapeados a los registros de una tabla.
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @return Devuelve un objeto con el resultado de la grabación
     */
    @Override
    public IDataResult update(String sessionId, IDataObject ejbs) {
        isChecked(ejbs.getDataRows());
        return update(sessionId, ejbs.getDataRows());
    }

    /**
     * Sincroniza una lista de ejbs con la base de datos.
     *
     * @param <T>
     * @param ejbs lista de objetos mapeados a los registros de una tabla.
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @return Devuelve un objeto con el resultado de la grabación
     */
    @Override
    public <T extends IDataRow> IDataResult update(String sessionId, List<T> ejbs) {
        isChecked(ejbs);
        IDataSet dataSet = new DataSet();
        String setKey = "1";
        if (ejbs.get(0) != null) {
            setKey = ejbs.get(0).getClass().getSimpleName().toLowerCase();
        }
        dataSet.add(setKey, (List<IDataRow>) ejbs);
        return update(sessionId, dataSet);
    }

    /**
     * Sincroniza una lista de ejbs con la base de datos.
     *
     * @param dataSet set de objetos mapeados a los registros de una tabla.
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @return Devuelve un objeto con el resultado de la grabación
     */
    @Override
    public IDataResult update(String sessionId, IDataSet dataSet) {
        // Recorrer los objetos a actualizar en la base
        dataSet.getMapListSet().entrySet().forEach((entry) -> {
            isChecked(entry.getValue());
        });
        return dao.update(sessionId, dataSet);
    }

    /**
     * Verifica los datos (usuario, contraseña y empresa a la que se quiere
     * loguear) Si pasa la válidación se puede crear el token.
     *
     * @param data datos conteniendo valores de usuario, pass y empresa
     * @throws SessionError
     */
    @Override
    public void checkAuthConsumerData(IOAuthConsumerData data) throws Exception {
        dao.checkAuthConsumerData(data);
    }

    /**
     * Verifica si la combinación iduser, idcompany es válido para una sesión
     *
     * @param iduser identificador del usuario
     * @param idcompany identificador de la empresa
     * @return verdadero si cumple y falso si no
     */
    @Override
    public boolean isCredentialValid(Long iduser, Long idcompany) {
        return dao.isCredentialValid(iduser, idcompany);
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

    /**
     * Si el ejb o registro paso por el proceso de validación.
     *
     * @param <T>
     * @param ejb ejb o registro
     * @throws CheckException
     */
    protected final <T extends IDataRow> void isChecked(T ejb) throws CheckException {
        if (ejb != null && ejb.getAction() > 0 && !ejb.isRowChecked()) {
            throw new CheckException("El registro no fue verificado");
        }
    }

    /**
     * Si los ejbs fuerón validados
     *
     * @param <T>
     * @param ejbs ejbs o registros
     * @throws CheckException
     */
    protected final <T extends IDataRow> void isChecked(List<T> ejbs) throws CheckException {
        if (ejbs != null) {
            ejbs.forEach((ejb) -> {
                isChecked(ejb);
            });
        }
    }

    /**
     * Copiar de un modelo origen tipo vista a uno destino tipo tabla
     *
     * @param <T>
     * @param <X>
     * @param sessionId identificador de la sesión
     * @param source modelo origen
     * @param target modelo destino
     * @return modelo destino
     * @throws Exception
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public <T extends IDataRow, X extends IDataRow> X copyTo(String sessionId, T source, X target) throws Exception {
        if (source == null || target == null) {
            return target;
        }
        Field[] fields = source.getClass().getDeclaredFields();
        //Recorrer atributos del origen para pasar al destino
        for (Field field : fields) {
            //Si tiene una anotación @ColumnFunction            
            ColumnFunction annotation = field.getAnnotation(ColumnFunction.class);
            if (annotation != null) {
                String fieldName = field.getName();
                Object fieldValue = source.getValue(fieldName);
                String fn = annotation.formula();
                //Si la función de conversión esta vacio devolver el valor del campo del modelo origen
                if (fn.isEmpty()) {
                    fieldValue = source.getValue(fieldName);
                } //Si la función de conversión es igual al nombre del campo devolver el valor del campo del modelo origen
                else if (fn.equals(":" + fieldName)) {
                    fieldValue = source.getValue(fieldName);
                } //Si la función de conversión comienza con "fn_" 
                else if (Strings.left(fn, 3).equals("fn_")) {
                    Object id = source.getValue(fieldName);
                    if (id == null) {
                        id = getValueFromFn(sessionId, source, fieldName, fn);
                    }
                    if (id != null) {
                        Class clazz = Class.forName(annotation.classMapped());
                        fieldValue = dao.findById(clazz, sessionId, id);
                    }
                    //Determinar el nombre del campo en el target
                    if (!annotation.fieldMapped().isEmpty()) {
                        fieldName = annotation.fieldMapped();
                    } else if (Strings.left(fieldName, 2).equals("id")) {
                        fieldName = fieldName.substring(2);
                    }
                }
                if (fieldValue == null) {
                    target.setValue(fieldName, null);
                    continue;
                }
                Class fieldClass = target.getFieldType(fieldName);
                //Si existe el campo en el destino                
                if (fieldClass != null) {
                    //Verificar que sea del mismo tipo                    
                    if (fieldClass.isAssignableFrom(fieldValue.getClass())) {
                        target.setValue(fieldName, fieldValue);
                    }
                }
            }
        }
        return target;
    }

    protected <T extends IDataRow> void setDefault(T row, String fieldName) throws Exception {
        if (row.getFieldType(fieldName) == BigDecimal.class) {
            row.setValue(fieldName, BigDecimal.ZERO);
        } else if (row.getFieldType(fieldName) == Boolean.class) {
            row.setValue(fieldName, false);
        } else if (row.getFieldType(fieldName) == Long.class) {
            row.setValue(fieldName, 0L);
        } else if (row.getFieldType(fieldName) == Integer.class) {
            row.setValue(fieldName, 0);
        } else if (row.getFieldType(fieldName) == Short.class) {
            row.setValue(fieldName, Short.valueOf("0"));
        }
    }

    protected <T extends IDataRow> Object getValueFromFn(String sessionId, T source, String fieldId, String fn) throws Exception {
        Map<String, Object> params;
        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
        String dbEngine = getDataEngine(dbLinkInfo.getPersistUnit());
        String queryString = "";
        // Componer el comando a ejecutar en la base de datos.
        if (Fn.inList(dbEngine, "SQLSERVER", "Microsoft SQL Server", "SYBASE")) {
            queryString = "select {schema}." + fn;
        } else if (Fn.inList(dbEngine, "ORACLE", "ORACLE8")) {
            queryString = "select {schema}." + fn + " FROM DUAL";
        } else if ("POSTGRES".equals(dbEngine)) {
            queryString = "select {schema}." + fn;
        } else if ("DB2".equals(dbEngine)) {
            queryString = "select {schema}." + fn + " FROM SYSIBM.SYSDUMMY1";
        }

        params = ParamsUtil.DataRowToMap(source);

        List<Object> result = findByNativeQuery(sessionId, queryString, params);
        if (result == null || result.isEmpty() || result.get(0) == null) {
            return null;
        }
        Class fieldType = source.getFieldType(fieldId);
        Object id;
        if (fieldType.isAssignableFrom(Long.class)) {
            id = Long.parseLong(result.get(0).toString());
        } else if (fieldType.isAssignableFrom(Integer.class)) {
            id = Integer.parseInt(result.get(0).toString());
        } else if (fieldType.isAssignableFrom(Short.class)) {
            id = Short.parseShort(result.get(0).toString());
        } else {
            id = result.get(0).toString();
        }
        return id;
    }
}

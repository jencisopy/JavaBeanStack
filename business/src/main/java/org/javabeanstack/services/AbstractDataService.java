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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.JoinColumn;
import org.apache.log4j.Logger;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.exceptions.SessionError;
import org.javabeanstack.security.IUserSession;
import org.javabeanstack.data.DataInfo;
import org.javabeanstack.data.DataResult;
import org.javabeanstack.data.IDataResult;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.annotation.CheckMethod;
import org.javabeanstack.data.AbstractDAO;
import org.javabeanstack.data.DBLinkInfo;
import org.javabeanstack.data.IDBLinkInfo;
import org.javabeanstack.error.ErrorReg;
import org.javabeanstack.util.Fn;

/**
 * Esta clase deriva de AbstractDAO, a travéz de ella se recupera, válida y se
 * graban los registros en la base de datos. Es un ejb que se ejecuta en la capa
 * de la lógica del negocio.
 *
 * El test unitario se encuentra en TestProjects clase
 * py.com.oym.test.data.TestDataService
 *
 * @author Jorge Enciso
 */
public abstract class AbstractDataService extends AbstractDAO implements IDataService {

    private static final Logger LOGGER = Logger.getLogger(AbstractDataService.class);
    protected List<Method> methodList = this.setListCheckMethods();

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
            String namePrefix = method.getName().toLowerCase().substring(0, 5);
            CheckMethod anotation = method.getAnnotation(CheckMethod.class);
            /* Ejecutar los metodos cuyo nombre inician con check */
            if ("check".equals(namePrefix) || anotation != null) {
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
    public <T extends IDataRow> boolean checkUniqueKey(T row, String sessionId) {
        try {
            // Buscar registro por la clave unica
            if (row.getQueryUK() == null) {
                return true;
            }
            T row2 = (T) findByUk(IDataRow.class, getDBLinkInfo(sessionId), row);
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
    public <T extends IDataRow> boolean checkForeignKey(T row, String fieldName, String sessionId) {
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
                    IDataRow fieldValue = find(fieldType, getDBLinkInfo(sessionId), id);
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
    public <T extends IDataRow> Map<String, IErrorReg> checkDataRow(T row, String sessionId) {
        Map<String, IErrorReg> errors = new HashMap<>();
        String fieldName;
        int[] operacion = {IDataRow.INSERT, IDataRow.UPDATE};
        IErrorReg result;
        CheckMethod anotation;
        try {
            // Chequeo de clave duplicada solo si la operación es agregar o modificar
            if (Fn.inList(row.getAction(), IDataRow.INSERT, IDataRow.UPDATE)) {
                if (!checkUniqueKey(row, sessionId)) {
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
                for (Field field : DataInfo.getDeclareFields(row.getClass())) {
                    fieldName = field.getName();
                    if (!checkForeignKey(row, fieldName, sessionId)) {
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

    /**
     * Se ejecuta en el metodo save, valida los datos del registro
     * (checkDataRow) y devuelve el resultado en un objeto IDataResult
     *
     * @param <T>
     * @param row registro de datos.
     * @param sessionId identificador de la sesión.
     * @return objeto con el resultado del proceso de validación.
     */
    protected final <T extends IDataRow> IDataResult checkDataResult(T row, String sessionId) {
        IDataResult dataResult = new DataResult();
        dataResult.setSuccess(Boolean.TRUE);
        List<IDataRow> ejbsRes = new ArrayList();
        // Validar el registro
        Map<String, IErrorReg> errors = this.checkDataRow(row, sessionId);
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
    protected final <T extends IDataRow> IDataResult save(T row, String sessionId) throws SessionError {
        checkUserSession(sessionId);
        IDataResult dataResult;
        // Validar registro
        dataResult = this.checkDataResult(row, sessionId);
        if (!dataResult.getErrorsMap().isEmpty()) {
            // Devolver el error si lo hubo
            return dataResult;
        }
        // Grabar registro en la base de datos.
        dataResult = update(getDBLinkInfo(sessionId), row);
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
    public <T extends IDataRow> IDataResult create(T row, String sessionId) throws SessionError {
        row.setAction(IDataRow.INSERT);
        return save(row, sessionId);
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
    public <T extends IDataRow> IDataResult edit(T row, String sessionId) throws SessionError {
        row.setAction(IDataRow.UPDATE);
        return save(row, sessionId);
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
    public <T extends IDataRow> IDataResult remove(T row, String sessionId) throws SessionError {
        row.setAction(IDataRow.DELETE);
        return save(row, sessionId);
    }

    private IDBLinkInfo getDBLinkInfo(String sessionId) {
        IDBLinkInfo dbInfo = new DBLinkInfo();
        dbInfo.setUserSession(getUserSession(sessionId));
        return dbInfo;
    }
}

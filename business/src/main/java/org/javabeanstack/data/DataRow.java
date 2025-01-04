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
package org.javabeanstack.data;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlTransient;
import org.apache.log4j.Logger;
import org.javabeanstack.annotation.AuditEntity;
import org.javabeanstack.annotation.SignatureField;

import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.error.ErrorReg;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.exceptions.FieldException;
import org.javabeanstack.util.Fn;

/**
 * Es la clase base de todos los modelos ejb mapeados a las tablas o vistas.
 *
 * @author Jorge Enciso
 */
public class DataRow implements IDataRow, Cloneable {

    private static final Logger LOGGER = Logger.getLogger(DataRow.class);
    @XmlTransient
    private int persistMode = IDataRow.PERSIST;
    @XmlTransient
    private Object idAlternative;
    @XmlTransient
    private int action = 0;
    @XmlTransient
    protected String queryUK = "";
    @XmlTransient
    private String idFunctionFind = "";
    @XmlTransient
    private boolean rowChecked = false;
    @XmlTransient
    private Map<String, Boolean> fieldsChecked = null;
    @XmlTransient
    private Map<String, IErrorReg> errors = new HashMap();
    @XmlTransient
    protected IDataRow fieldsOldValues;
    @XmlTransient
    protected IDataRow fieldsBeforeValues;
    @XmlTransient
    protected boolean noSetBeforeValues = false;
    @XmlTransient
    protected Boolean onSetterActivated = true;
    @XmlTransient
    protected Boolean onGetterActivated = true;
    @XmlTransient
    private Map<String, Object> properties;
    @XmlTransient
    private String signature="SINFIRMA";
    
    @XmlTransient
    private boolean auditable = false;
    private Class<? extends IDataRow> auditTable;

    public DataRow() {
        this.action = 0;
    }

    @Override
    public Object clone() {
        Object obj = null;
        try {
            obj = super.clone();
        } catch (CloneNotSupportedException ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return obj;
    }

    @Override
    public final int getPersistMode() {
        return persistMode;
    }

    @Override
    public final void setPersistMode(int persistMode) {
        this.persistMode = persistMode;
    }

    /**
     * Devuelve un atributo con los valores originales antes de ser modificado.
     *
     * @return un atributo con los valores originales antes de ser modificado.
     */
    protected IDataRow getFieldsOldValues() {
        return fieldsOldValues;
    }

    /**
     * Se guarda en un atributo los valores originales del objeto
     */
    @Override
    public void setOldValues() {
        fieldsOldValues = null;
        fieldsOldValues = (IDataRow) this.clone();
        fieldsBeforeValues = (IDataRow) this.clone();
        ((DataRow) fieldsBeforeValues).noSetBeforeValues = true;
    }

    /**
     * Se guarda en un atributo los valores originales del objeto
     *
     * @param fieldName
     * @param value
     */
    public final void setBeforeValue(String fieldName, Object value) {
        if (noSetBeforeValues) {
            return;
        }
        if (fieldsBeforeValues == null) {
            fieldsBeforeValues = (IDataRow) this.clone();
            ((DataRow) fieldsBeforeValues).noSetBeforeValues = true;
            fieldsBeforeValues.setOnSetterActivated(false);
            fieldsBeforeValues.setOnGetterActivated(false);
        }
        try {
            fieldsBeforeValues.setValue(fieldName, value);
        } catch (Exception exp) {
            //nada
        }
    }

    /**
     * Devuelve el valor original de un campo antes de ser modificado.
     *
     * @param fieldName nombre del atributo o campo.
     * @return el valor original antes de ser modificado de un campo.
     */
    @Override
    public Object getOldValue(String fieldName) {
        if (action == IDataRow.INSERT) {
            return null;
        }
        if (fieldsOldValues == null) {
            return this.getValue(fieldName);
        }
        return fieldsOldValues.getValue(fieldName);
    }

    /**
     * Devuelve el valor original de un campo antes de ser modificado.
     *
     * @param fieldName nombre del atributo o campo.
     * @return el valor original antes de ser modificado de un campo.
     */
    @Override
    public Object getBeforeValue(String fieldName) {
        if (fieldsBeforeValues == null) {
            return this.getValue(fieldName);
        }
        return fieldsBeforeValues.getValue(fieldName);
    }

    /**
     * Devuelve el valor del atributo operacion que indica la operación
     * realizada sobre la fila o miembro dado<br>
     * 1 Agregar <br>
     * 2 Modificar<br>
     * 3 Borrar<br>
     * 4 Consultar<br>
     *
     * @return valor de operación.
     */
    @Override
    public int getAction() {
        return action;
    }

    /**
     * Devuelve una sentencia para buscar un registro especifico utilizando los
     * valores de los campos unicos.
     *
     * @return sentencia con la condición de la clave unica.
     */
    @Override
    public String getQueryUK() {
        if (queryUK == null || queryUK.equals("")) {
            queryUK = DataInfo.createQueryUkCommand(this);
        }
        return queryUK;
    }

    @Override
    public String getIdFunctionFind() {
        return idFunctionFind;
    }

    /**
     * Devuelve la variable rowChecked, el cual indica si el registro fue
     * verificado o no.
     *
     * @return verdadero si fue verificado este componente o falso si no.
     */
    @Override
    public boolean isRowChecked() {
        return rowChecked;
    }

    /**
     * Asigna la propiedad rowChecked
     *
     * @param checkrow
     */
    @Override
    public void setRowChecked(boolean checkrow) {
        this.rowChecked = checkrow;
    }

    /**
     * Devuelve si un campo fue verificado o no.
     *
     * @param fieldName nombre del campo.
     * @return verdadero o falso de acuerdo a si el campo dado fue verificado.
     */
    @Override
    public boolean isFieldChecked(String fieldName) {
        if (this.fieldsChecked == null) {
            return true;
        }
        fieldName = "_" + fieldName.toLowerCase();
        if (this.fieldsChecked.get(fieldName) == null) {
            return true;
        }
        return this.fieldsChecked.get(fieldName);
    }

    /**
     * Devuelve un map con los campos que fuerón verificados.
     *
     * @return map con los campos que fuerón verificados.
     */
    @Override
    public Map getFieldsChecked() {
        return this.fieldsChecked;
    }

    /**
     * Asigna un map con los campos verificados.
     *
     * @param fieldChecked
     */
    @Override
    public void setFieldsChecked(Map fieldChecked) {
        this.fieldsChecked = fieldChecked;
    }

    /**
     * Asigna un campo si fue verificado o no
     *
     * @param fieldName nombre del campo
     * @param checkField verdero fue verificado, falso no lo fue.
     */
    @Override
    public void setFieldChecked(String fieldName, boolean checkField) {
        if (this.fieldsChecked != null && !this.fieldsChecked.isEmpty()) {
            fieldName = "_" + fieldName.toLowerCase();
            if (this.fieldsChecked.get(fieldName) != null) {
                this.fieldsChecked.put(fieldName, checkField);
                if (checkField == false) {
                    this.setRowChecked(false);
                }
            }
        }
    }

    /**
     *
     * @return devuelve map con una lista de errores por campo si lo hubiese.
     */
    @XmlTransient
    @Override
    public Map<String, IErrorReg> getWarnings() {
        if (this.errors == null) {
            this.errors = new HashMap();
            return this.errors;
        }
        Map<String, IErrorReg> warnings = new HashMap();
        for (Map.Entry<String, IErrorReg> entry : errors.entrySet()) {
            if (!entry.getValue().isWarning()) {
                warnings.put(entry.getKey(), entry.getValue());
            }
        }
        return warnings;
    }

    /**
     *
     * @return devuelve map con una lista de errores por campo si lo hubiese.
     */
    @XmlTransient
    @Override
    public Map<String, IErrorReg> getErrors() {
        if (this.errors == null) {
            this.errors = new HashMap();
            return this.errors;
        }
        Map<String, IErrorReg> errorNoWarns = new HashMap();
        for (Map.Entry<String, IErrorReg> entry : errors.entrySet()) {
            if (!entry.getValue().isWarning()) {
                errorNoWarns.put(entry.getKey(), entry.getValue());
            }
        }
        return errorNoWarns;
    }

    /**
     * Asigna un map con los errores ocurridos por campo.
     *
     * @param errorReg
     */
    @Override
    public void setErrors(Map<String, IErrorReg> errorReg) {
        this.errors = errorReg;
    }

    /**
     * Agrega un error al map de errores.
     *
     * @param errorReg objeto conteniendo datos del error.
     * @param fieldName el campo sobre el cual se genero el error.
     */
    @Override
    public void setErrors(IErrorReg errorReg, String fieldName) {
        if (fieldName == null) {
            fieldName = "datageneric";
        }
        if (errors == null) {
            errors = new HashMap<>();
        }
        this.errors.put(fieldName.toLowerCase(), errorReg);
    }

    /**
     * Asigna un mensaje de error al map de errores
     *
     * @param errorMsg mensaje de error
     * @param fieldname campo sobre el cual ocurrio el error.
     * @param errorNumber nro de error. (ver en IAppMensaje)
     */
    @Override
    public void setErrors(String errorMsg, String fieldname, int errorNumber) {
        if (fieldname == null) {
            fieldname = "datageneric";
        }
        IErrorReg error = new ErrorReg();
        error.setMessage(errorMsg);
        error.setFieldName(fieldname);
        error.setErrorNumber(errorNumber);
        this.setErrors(error, fieldname);
    }

    /**
     * Asigna tipo de operación a realizarse en la base de datos.
     *
     * @param action
     * <br><br>
     * 1 Agregar <br>
     * 2 Modificar<br>
     * 3 Borrar<br>
     * 4 Consultar<br>
     */
    @Override
    public void setAction(int action) {
        if (action > 1) {
            if (this.action != action) {
                this.setOldValues();
            }
        } else {
            this.fieldsOldValues = null;
            this.fieldsBeforeValues = null;
        }
        this.action = action;
    }

    /**
     * Devuelve el valor identificador de este componente.
     *
     * @return identificador del componente.
     */
    @XmlTransient
    @Override
    public Object getId() {
        return DataInfo.getIdvalue(this);
    }

    /**
     * Es el identificador del componente, si el valor es nulo devuelve 0
     *
     * @return identificador del componente.
     */
    @XmlTransient
    @Override
    public Object getRowkey() {
        Object obj = getId();
        if (obj == null) {
            obj = getIdAlternative();
            if (obj == null) {
                return "";
            }
            return obj;
        }
        String fieldType;
        if (obj instanceof DataRow) {
            fieldType = obj.getClass().getName();
            return "{" + fieldType + "}" + ((DataRow) obj).getId();
        }
        fieldType = obj.getClass().getSimpleName();
        return "{" + fieldType + "}" + obj;
    }

    /**
     * Devuelve el valor de un atributo del objeto
     *
     * @param fieldname
     * @return valor de un atributo o campo.
     */
    @Override
    public Object getValue(String fieldname) {
        return DataInfo.getFieldValue(this, fieldname);
    }

    /**
     * Devuelve el tipo de dato de un campo o atributo.
     *
     * @param fieldname nombre del campo
     * @return tipo de dato del campo solicitado.
     */
    @Override
    public Class getFieldType(String fieldname) {
        Class result = DataInfo.getFieldType(this.getClass(), fieldname);
        if (result == null) {
            result = DataInfo.getMethodReturnType(this.getClass(), fieldname, true);
        }
        return result;
    }

    @Override
    public void setId(Object id) {
        DataInfo.setIdvalue(this, id);
    }

    /**
     * Asigna un valor a un atributo
     *
     * @param fieldname nombre del campo o atributo
     * @param value valor a asignar
     * @throws org.javabeanstack.exceptions.FieldException
     */
    @Override
    public void setValue(String fieldname, Object value) throws FieldException {
        if (fieldsBeforeValues != null) {
            //Guardar el valor anterior por si se necesita utilizar posteriormente
            setBeforeValue(fieldname, value);
        }
        //Asignar el valor en el atributo
        Boolean exito = DataInfo.setFieldValue(this, fieldname, value);
        if (!exito) {
            LOGGER.error("fieldname: " + fieldname);
            throw new FieldException("Error en " + fieldname + ", no existe el campo o el tipo es incorrecto");
        }
        if (this.action == 0) {
            this.action = IDataRow.UPDATE;
        }
    }

    @Override
    public Object getIdAlternative() {
        return idAlternative;
    }

    @Override
    public void setIdAlternative(Object idAlternative) {
        this.idAlternative = idAlternative;
    }

    @Override
    public boolean isAuditAble() {
        return auditable;
    }

    @Override
    public final void setAuditAble(boolean auditable) {
        this.auditable = auditable;
    }

    @Override
    public final Class<? extends IDataRow> getAuditClass() {
        return auditTable;
    }

    @Override
    public final void setAuditClass(Class<? extends IDataRow> auditTable) {
        this.auditTable = auditTable;
    }

    /**
     * Marca el objeto o registro como borrado
     *
     * @return verdadero si tuvo exito, falso si no.
     */
    @Override
    public boolean delete() {
        this.action = IDataRow.DELETE;
        return true;
    }

    /**
     * Identificador del objeto
     *
     * @return identificador del objeto
     */
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (getId() != null ? getId().hashCode() : 0);
        hash *= getClass().getName().hashCode();
        return hash;
    }

    /**
     * Determina si este objeto es igual a uno que se recibe como parámetro
     *
     * @param obj objeto a comparar.
     * @return verdadero si es igual y falso si no
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        // Debe ser exactamente del mismo tipo
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final IDataRow other = (IDataRow) obj;
        if (this.getId() == null && this.getIdAlternative() != null) {
            return Objects.equals(this.getIdAlternative(), other.getIdAlternative());
        }
        return Objects.equals(this.getId(), other.getId());
    }

    /**
     * Determina si este objeto es equivalente (ciertos atributos son iguales) a
     * el objeto que se pasa como parámetro.
     *
     * @param o objeto a comparar.
     * @return verdadero si es equivalente y falso si no
     */
    @Override
    public boolean equivalent(Object o) {
        throw new UnsupportedOperationException("Debe implementar el metodo equivalent en " + getClass().getName());
    }

    /**
     * Si se aplica o no el filtro por defecto en la selección de datos. Este
     * metodo se modifica en las clases derivadas si se debe cambiar el
     * comportamiento.
     *
     * @return verdadero si y falso no
     */
    @XmlTransient
    @Override
    public boolean isApplyDBFilter() {
        return true;
    }

    /**
     * Verifica que el valor de idcompany sea válido (En caso que corresponda,
     * hay componentes que no se deben realizar esta validación)
     *
     * @param idcompany
     * @return verdadero si pasa la validación y falso si no.
     */
    @Override
    public boolean checkFieldIdcompany(Long idcompany) {
        if (!DataInfo.isFieldExist(this.getClass(), "idcompany")
                && !DataInfo.isFieldExist(this.getClass(), "idempresa")) {
            return true;
        }
        Long idempresaThis = Fn.nvl((Long) this.getValue("idempresa"), 0L);
        Long idcompanyThis = Fn.nvl((Long) this.getValue("idcompany"), 0L);
        return idcompanyThis.equals(idcompany) || idempresaThis.equals(idcompany);
    }

    /**
     * Para ejecutarse al llamar a un getter
     *
     * @param fieldName nombre del atributo
     */
    @Override
    public void onGetter(String fieldName) {
        if (!onGetterActivated) {
            return;
        }
        //Implementar en clases derivadas
    }

    /**
     * Se ejecuta en el setter despues de la asignación
     *
     * @param fieldName nombre del atributo
     * @param newValue valor nuevo
     * @param oldValue valor anterior
     */
    @Override
    public void onSetter(String fieldName, Object oldValue, Object newValue) {
        if (!onSetterActivated) {
            return;
        }
        //Implementar en clases derivadas
        setBeforeValue(fieldName, oldValue);
    }

    /**
     * Setea valores por defectos en los campos en caso se tener valor nulo y no
     * se permita valores nulos
     *
     * @throws Exception
     */
    @Override
    public void setDefaults() throws Exception {
        Class classType = this.getClass();
        Field[] fields = classType.getDeclaredFields();
        for (Field field : fields) {
            NotNull annotation = field.getAnnotation(NotNull.class);
            if (annotation != null && this.getValue(field.getName()) == null) {
                if (getFieldType(field.getName()) == BigDecimal.class) {
                    setValue(field.getName(), BigDecimal.ZERO);
                } else if (getFieldType(field.getName()) == Boolean.class) {
                    setValue(field.getName(), false);
                } else if (getFieldType(field.getName()) == Long.class) {
                    setValue(field.getName(), 0L);
                } else if (getFieldType(field.getName()) == Integer.class) {
                    setValue(field.getName(), 0);
                } else if (getFieldType(field.getName()) == Short.class) {
                    setValue(field.getName(), Short.valueOf("0"));
                }
            }
        }
    }

    @XmlTransient
    @Override
    public Boolean getOnGetterActivated() {
        return onGetterActivated;
    }

    @XmlTransient
    @Override
    public Boolean getOnSetterActivated() {
        return onSetterActivated;
    }

    @Override
    public void setOnGetterActivated(boolean onGetter) {
        this.onGetterActivated = onGetter;
    }

    @Override
    public void setOnSetterActivated(boolean onSetter) {
        this.onSetterActivated = onSetter;
    }

    @Override
    public <X extends IDataRow> X copyTo(X target) throws Exception {
        IDataRow source = this;
        if (source == null || target == null) {
            return target;
        }
        Field[] fields = target.getClass().getDeclaredFields();
        //Recorrer atributos del destino para copiar desde el origen
        for (Field field : fields) {
            String fieldName = field.getName();
            Class fieldTargetClass = target.getFieldType(fieldName);
            Class fieldSourceClass = source.getFieldType(fieldName);
            //Si existe el campo en el origen
            if (fieldSourceClass != null && fieldTargetClass.isAssignableFrom(fieldSourceClass)) {
                target.setValue(fieldName, source.getValue(fieldName));
            }
        }
        return target;
    }

    @XmlTransient
    @Override
    public String getAuditEntity() {
        AuditEntity auditEntityAnnotation = getClass().getAnnotation(AuditEntity.class);
        if (auditEntityAnnotation != null) {
            return auditEntityAnnotation.name().toLowerCase();
        }
        return null;
    }

    @XmlTransient
    @Override
    public Map<String, Object> getProperties() {
        if (properties == null) {
            properties = new HashMap();
        }
        return properties;
    }

    @Override
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @XmlTransient
    @Override
    public String getSignature() {
        return signature;
    }

    @Override
    public void setSignature(String signature) {
        this.signature = signature; 
    }
    
    @XmlTransient
    @Override
    public String getTextToSign() {
        String retornar = "";
        Field[] fields = getClass().getDeclaredFields();
        IDataRow ejb;
        for (Field field : fields) {
            SignatureField annotation = field.getAnnotation(SignatureField.class);
            if (annotation != null) {
                field.setAccessible(true);
                try {
                    Object obj = field.get(this);
                    if (obj != null && obj instanceof IDataRow) {
                        ejb = (IDataRow) obj;
                        if (ejb.getId() != null) {
                            retornar += "{" + Fn.nvl(DataInfo.getIdFieldName(ejb.getClass()), field.getName()) + ":" + ejb.getId().toString() + "}";
                        }
                    } else if (obj != null) {
                        String value;
                        if (obj instanceof BigDecimal) {
                            value = Fn.getValueFormatted(obj);
                        } else if (obj instanceof Number) {
                            value = Fn.getValueFormatted(obj, "##0");
                        } else if (obj instanceof LocalDateTime) {
                            value = Fn.getValueFormatted(obj, "yyyy-MM-dd'T'HH:mm");
                        } else {
                            value = obj.toString().replaceAll("\\s+$",""); //se elimina espacios a la derecha.
                        }
                        retornar += "{" + field.getName() + ":" + value + "}";
                    }
                } catch (Exception e) {
                    ErrorManager.showError(e, LOGGER);
                }
            }
        }
        retornar += "{SIGNATURE:"+Fn.nvl(signature, "")+"}";
        return retornar;
    }
}

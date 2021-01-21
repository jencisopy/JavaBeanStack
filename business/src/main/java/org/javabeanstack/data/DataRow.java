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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlTransient;
import org.apache.log4j.Logger;

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
    protected IDataRow fieldsOldValues;

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
    }

    /**
     * Devuelve el valor original de un campo antes de ser modificado.
     *
     * @param fieldName nombre del atributo o campo.
     * @return el valor original antes de ser modificado de un campo.
     */
    @Override
    public Object getOldValue(String fieldName) {
        if (action == IDataRow.INSERT){
            return null;
        }
        if (fieldsOldValues == null) {
            return this.getValue(fieldName);
        }
        return fieldsOldValues.getValue(fieldName);
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
    @Override
    public Map<String, IErrorReg> getErrors() {
        if (this.errors == null) {
            this.errors = new HashMap();
        }
        return this.errors;
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
        if (action > 1){
            this.setOldValues();
        }
        else{
            this.fieldsOldValues = null;
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
            return "";
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
    @XmlTransient
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
    @XmlTransient
    @Override
    public Class getFieldType(String fieldname) {
        Class result = DataInfo.getFieldType(this.getClass(), fieldname);
        if (result == null) {
            result = DataInfo.getMethodReturnType(this.getClass(), fieldname, true);
        }
        return result;
    }

    
    @XmlTransient
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
        Boolean exito = DataInfo.setFieldValue(this, fieldname, value);
        if (!exito) {
            LOGGER.error("fieldname: " + fieldname);
            throw new FieldException("Error en " + fieldname + ", no existe el campo o el tipo es incorrecto");
        }
        if (this.action == 0) {
            this.action = IDataRow.UPDATE;
        }
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
        //Implementar en clases derivadas
    }

    /**
     * Se ejecuta en el setter despues de la asignación
     *
     * @param fieldName nombre del atributo
     * @param fieldValueNew valor nuevo
     * @param fieldValueOld valor anterior
     */
    @Override
    public void onSetter(String fieldName, Object fieldValueNew, Object fieldValueOld) {
        //Implementar en clases derivadas
    }

    /**
     * Setea valores por defectos en los campos en caso se tener valor nulo y no
     * se permita valores nulos
     * 
     * @throws Exception 
     */
    @Override
    public void setDefaults() throws Exception{
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
}

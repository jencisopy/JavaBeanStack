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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.log4j.Logger;

import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.error.ErrorReg;
import org.javabeanstack.error.ErrorManager;

/**
 * Es la clase base de todos los modelos ejb mapeados a las tablas o vistas.
 * @author Jorge Enciso
 */
public class DataRow implements IDataRow, Serializable, Cloneable{
    private static final Logger LOGGER = Logger.getLogger(DataRow.class);
    
    private int     operacion  = 0;
    private String  queryUK    = "";
    private String  idFunctionFind = "";
    private boolean rowChecked = false;    
    private Map<String, Boolean>  fieldChecked = null;
    private Map<String,IErrorReg> errors = null;
    
    //TODO implementar parent, beforesetvalue, aftersetvalue
    public DataRow(){
        this.operacion = 0;
    }

    @Override
    public Object clone(){
        Object obj=null;
        try{
            obj=(DataRow)super.clone();
        }catch(CloneNotSupportedException ex){
            ErrorManager.showError(ex, LOGGER);            
        }
        return obj;
    }

    /**
     * Devuelve el valor del atributo operacion que indica la operación
     * realizada sobre la fila o miembro dado<br>
     * 1 Agregar <br>
     * 2 Modificar<br>
     * 3 Borrar<br>
     * 4 Consultar<br>
     * @return  valor de operación.
     */     
    @Override
    public int getOperation() {
        return operacion;
    }

    /**
     * Devuelve una sentencia para buscar un registro especifico utilizando 
     * los valores de los campos unicos.
     * @return 
     */
    @Override
    public String getQueryUK(){
        if (queryUK == null || queryUK.equals("")){
            queryUK = DataInfo.createQueryUkCommand(this);
        }
        return queryUK;
    }

    @Override
    public String getIdFunctionFind() {
        return idFunctionFind;
    }

    
    /**
     * Devuelve la variable rowChecked, el cual indica si el registro fue verificado o no.
     * @return verdadero si fue verificado este componente o falso si no.
     */
    @Override
    public boolean isRowChecked(){
        return rowChecked;
    }

    /**
     * Asigna la propiedad rowChecked
     * @param checkrow 
     */
    @Override
    public void setRowChecked(boolean checkrow) {
        this.rowChecked = checkrow;
    }

    /**
     * Devuelve si un campo fue verificado o no.
     * @param fieldName nombre del campo.
     * @return verdadero o falso de acuerdo a si el campo dado fue verificado.
     */
    @Override
    public boolean isFieldChecked(String fieldName){
        if (this.fieldChecked==null){
            return true;
        }
        fieldName = "_"+fieldName.toLowerCase();
        if (this.fieldChecked.get(fieldName) == null){
            return true;
        }
        return this.fieldChecked.get(fieldName);
    }

    /**
     * Devuelve un map con los campos que fuerón verificados.
     * @return map con los campos que fuerón verificados.
     */
    @Override
    public Map getFieldChecked(){
        return this.fieldChecked;
    }

    /**
     * Asigna un map con los campos verificados.
     * @param fieldChecked 
     */
    @Override
    public void setFieldChecked(Map fieldChecked){
        this.fieldChecked = fieldChecked;
    }

    /**
     * Asigna un campo si fue verificado o no
     * @param fieldName nombre del campo
     * @param checkField verdero fue verificado, falso no lo fue.
     */
    @Override
    public void setFieldChecked(String fieldName, boolean checkField) {
        if (this.fieldChecked != null){
            fieldName = "_"+fieldName.toLowerCase();
            if (this.fieldChecked.get(fieldName) != null){
                this.fieldChecked.put(fieldName, checkField);
                if (checkField == false){
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
    public Map<String, IErrorReg> getErrors(){
        return this.errors;
    } 

    /**
     * Asigna un map con los errores ocurridos por campo.
     * @param errorReg 
     */
    @Override
    public void  setErrors(Map<String, IErrorReg> errorReg){
        this.errors = errorReg;            
    }        
    
    /**
     * Agrega un error al map de errores.
     * @param errorReg  objeto conteniendo datos del error.
     * @param fieldName el campo sobre el cual se genero el error.
     */
    @Override
    public void  setErrors(IErrorReg errorReg, String fieldName){
        if (fieldName == null){
            fieldName = "datageneric";
        }
        if (errors == null){
            errors = new HashMap<>();
        }
        this.errors.put(fieldName.toLowerCase(), errorReg);
    }

    /**
     * Asigna un mensaje de error al map de errores
     * @param errorMsg      mensaje de error
     * @param fieldname     campo sobre el cual ocurrio el error.
     * @param errorNumber   nro de error. (ver en IAppMensaje)
     */
    @Override
    public void  setErrors(String errorMsg, String fieldname, int errorNumber){
        if (fieldname == null){
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
     * @param operation<br><br>
     * 1 Agregar <br>
     * 2 Modificar<br>
     * 3 Borrar<br>
     * 4 Consultar<br>
     */
    @Override
    public void setOperation(int operation) {
        this.operacion = operation;
    }
    
    /**
     * Devuelve el valor identificador de este componente.
     * @return identificador del componente.
     */
    @Override
    public Object getId(){
        Object obj = DataInfo.getIdvalue(this);
        return obj;
    }

    /**
     * Es el identificador del componente, si el valor es nulo devuelve 0
     * @return identificador del componente.
     */
    @Override
    public Object getRowkey(){
        Object obj = DataInfo.getIdvalue(this);
        if (obj == null){
            return 0;
        }
        return obj;
    }
    
    /**
     * Devuelve el valor de un atributo del objeto
     * @param fieldname
     * @return valor de un atributo o campo.
     */    
    @Override
    public Object getValue(String fieldname){
        Object valor;
        Class<?> classObj = this.getClass();
        try {
            // Si el nombre contiene "." es porque el campo es de uno de los miembros
            // de este objeto
            if (fieldname.contains(".")){
                valor = DataInfo.getFieldValue(this, fieldname);
            }
            else {
                Field field = DataInfo.getDeclaredField(classObj, fieldname);      
                field.setAccessible(true);
                valor = field.get(this);
                if (valor instanceof IDataRow){
                    IDataRow row = (IDataRow)valor;
                    return DataInfo.getIdvalue(row);
                }
            }
            return valor;                            
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            ErrorManager.showError(ex, LOGGER);            
        }
        return null;
    }
    
    /**
     * Devuelve el tipo de dato de un campo o atributo.
     * @param fieldname nombre del campo
     * @return  tipo de dato del campo solicitado.
     */
    @Override
    public Class getFieldType(String fieldname){
        return DataInfo.getFieldType(this.getClass(), fieldname);
    }
            
    /**
     * Asigna un valor a un atributo 
     * @param fieldname     nombre del campo o atributo
     * @param value         valor a asignar
     */
    @Override
    public void setValue(String fieldname, Object value){
        Class<?> classObj = this.getClass();
        try {
            Field field = DataInfo.getDeclaredField(classObj, fieldname);
            field.setAccessible(true);
            field.set(this, value);                
            this.setFieldChecked(fieldname, false);            
            if (this.operacion == 0){
                this.operacion = IDataRow.MODIFICAR;
            }
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            ErrorManager.showError(ex, LOGGER);            
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);            
        }
        
    }


    /**
     * Marca el objeto o registro como borrado
     * @return verdadero si tuvo exito, falso si no.
     */
    @Override
    public boolean delete(){
        if (this == null)
            return false;
        this.operacion = IDataRow.BORRAR;
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (getId() != null ? getId().hashCode() : 0);
        hash *= getClass().getName().hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        // Debe ser exactamente del mismo tipo
        if (!getClass().getName().equals(obj.getClass().getName())) {
            return false;
        }
        final IDataRow other = (IDataRow) obj;
        return Objects.equals(this.getId(), other.getId());
    }

    @Override
    public boolean equivalent(Object o) {
        throw new UnsupportedOperationException("Debe implementar el metodo equivalent en "+getClass().getName());
    }

}

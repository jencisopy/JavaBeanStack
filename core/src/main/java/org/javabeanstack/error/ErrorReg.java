/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
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

package org.javabeanstack.error;


/**
 * Clase que acondiciona propiedades necesarias para guardar información de errores ocurridos.
 * @author jenciso
 */
public class ErrorReg implements IErrorReg{
    /** Texto del mensaje */
    private String message  = "";
    /** Nro del error, ver en AppMessage */
    private int    errorNumber = 0;
    private String entity = "";
    private String fieldName = "noFieldSet";
    private String[] fieldNames;    
    private Exception exception;
    private boolean warning = false;
    private String ipRequest = "";
    private String event = "ERROR";
    private String level = "E";
    private Object info;
    
    /**
     * Constructor por defecto.
     */
    public ErrorReg(){
    }
    
    /**
     * Crea el registro de error con mensaje, número y campo afectado.
     *
     * @param message mensaje de error.
     * @param errorNumber número de error.
     * @param fieldName campo afectado.
     */
    public ErrorReg(String message, int errorNumber, String fieldName){
        this.message = message;
        this.errorNumber = errorNumber;
        this.fieldName = fieldName;
    }

    /**
     * Crea el registro de error con mensaje, número y campos afectados.
     *
     * @param message mensaje de error.
     * @param errorNumber número de error.
     * @param fieldNames campos afectados.
     */
    public ErrorReg(String message, int errorNumber, String[] fieldNames){
        this.message = message;
        this.errorNumber = errorNumber;
        this.fieldNames = fieldNames;
    }

    /**
     * Devuelve información de contexto adicional asociada al error.
     *
     * @return información adicional.
     */
    @Override
    public Object getInfo() {
        return info;
    }
    
    /**
     * Devuelve la entidad afectada por el error.
     *
     * @return nombre de la entidad.
     */
    @Override
    public String getEntity() {
        return entity;
    }

    
    /**
     * Devuelve el campo afectado por el error.
     *
     * @return nombre del campo.
     */
    @Override
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Devuelve los campos afectados por el error (cuando involucra varios).
     *
     * @return nombres de los campos.
     */
    @Override
    public String[] getFieldNames() {
        return fieldNames;
    }
    
    /**
     * Devuelve el mensaje de error.
     *
     * @return mensaje de error.
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * Devuelve el número de error del catálogo de mensajes.
     *
     * @return número de error.
     */
    @Override
    public Integer getErrorNumber() {
        return errorNumber;
    }

    /**
     * Devuelve la excepción de origen del error, si la hubo.
     *
     * @return excepción, o {@code null} si no hubo.
     */
    @Override    
    public Exception getException() {
        return exception;
    }

    /**
     * Indica si el registro es una advertencia (no un error).
     *
     * @return verdadero si es advertencia, falso si es error.
     */
    @Override
    public boolean isWarning() {
        return warning;
    }

    /**
     * Devuelve la ip de la solicitud donde ocurrió el error.
     *
     * @return dirección ip.
     */
    @Override
    public String getIpRequest() {
        return ipRequest;
    }

    /**
     * Devuelve el evento en el que ocurrió el error.
     *
     * @return nombre del evento.
     */
    @Override
    public String getEvent() {
        return event;
    }

    /**
     * Devuelve el nivel del error (p. ej. entidad, fila, campo).
     *
     * @return nivel del error.
     */
    @Override
    public String getLevel() {
        return level;
    }
    
    /**
     * Asigna la entidad afectada por el error.
     *
     * @param entity nombre de la entidad.
     */
    @Override
    public void setEntity(String entity) {
        this.entity = entity;
    }

    
    /**
     * Asigna el campo afectado por el error.
     *
     * @param fieldName nombre del campo.
     */
    @Override
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * Asigna los campos afectados por el error.
     *
     * @param fieldNames nombres de los campos.
     */
    @Override
    public void setFieldNames(String[] fieldNames) {
        this.fieldNames = fieldNames;
    }
    
    /**
     * Asigna el mensaje de error.
     *
     * @param message mensaje de error.
     */
    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Asigna el número de error del catálogo de mensajes.
     *
     * @param errorNumber número de error.
     */
    @Override
    public void setErrorNumber(int errorNumber) {
        this.errorNumber = errorNumber;
    }

    /**
     * Asigna la excepción de origen del error.
     *
     * @param exception excepción.
     */
    @Override    
    public void setException(Exception exception) {
        this.exception = exception;
    }
    
    /**
     * Asigna la condición de advertencia del registro.
     *
     * @param warning verdadero si es advertencia.
     */
    @Override
    public void setWarning(boolean warning) {
        this.warning = warning;
    }

    /**
     * Asigna la ip de la solicitud donde ocurrió el error.
     *
     * @param ip dirección ip.
     */
    @Override
    public void setIpRequest(String ip) {
        this.ipRequest = ip;
    }

    /**
     * Asigna el evento en el que ocurrió el error.
     *
     * @param event nombre del evento.
     */
    @Override
    public void setEvent(String event) {
        this.event = event;
    }

    /**
     * Asigna el nivel del error.
     *
     * @param level nivel del error.
     */
    @Override
    public void setLevel(String level) {
        this.level = level;
    }
    
    /**
     * Asigna información de contexto adicional del error.
     *
     * @param info información adicional.
     */
    @Override
    public void setInfo(Object info) {
        this.info = info;
    }
}



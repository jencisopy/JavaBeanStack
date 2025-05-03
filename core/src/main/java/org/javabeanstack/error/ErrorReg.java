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
    
    public ErrorReg(){
    }
    
    public ErrorReg(String message, int errorNumber, String fieldName){
        this.message = message;
        this.errorNumber = errorNumber;
        this.fieldName = fieldName;
    }

    public ErrorReg(String message, int errorNumber, String[] fieldNames){
        this.message = message;
        this.errorNumber = errorNumber;
        this.fieldNames = fieldNames;
    }
    
    @Override
    public String getEntity() {
        return entity;
    }

    
    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String[] getFieldNames() {
        return fieldNames;
    }
    
    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Integer getErrorNumber() {
        return errorNumber;
    }

    @Override    
    public Exception getException() {
        return exception;
    }

    @Override
    public boolean isWarning() {
        return warning;
    }

    @Override
    public String getIpRequest() {
        return ipRequest;
    }

    @Override
    public String getEvent() {
        return event;
    }

    @Override
    public String getLevel() {
        return level;
    }
    
    @Override
    public void setEntity(String entity) {
        this.entity = entity;
    }

    
    @Override
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public void setFieldNames(String[] fieldNames) {
        this.fieldNames = fieldNames;
    }
    
    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public void setErrorNumber(int errorNumber) {
        this.errorNumber = errorNumber;
    }

    @Override    
    public void setException(Exception exception) {
        this.exception = exception;
    }
    
    @Override
    public void setWarning(boolean warning) {
        this.warning = warning;
    }

    @Override
    public void setIpRequest(String ip) {
        this.ipRequest = ip;
    }

    @Override
    public void setEvent(String event) {
        this.event = event;
    }

    @Override
    public void setLevel(String level) {
        this.level = level;
    }
}



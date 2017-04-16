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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.javabeanstack.error.IErrorReg;

/**
 * A travéz de esta clase, se devuelve el resultado de la grabación de registros
 * en la base de datos. Normalmente este componente va de una capa (ejb) a otra (web).
 * 
 * @author Jorge Enciso
 */
public class DataResult implements IDataResult {

    private final Map<String, List<IDataRow>> mapResult = new HashMap();
    private Boolean success = true;
    private String errorMsg = "";
    private IDataRow rowResult;
    private Exception exception;
    private Boolean removeDeleted=false;
    private Map<String, IErrorReg> errorsMap = new HashMap();

    /**
     * Asigna al conjunto una lista ejb que ha sido procesado.
     * @param key       clave
     * @param listEjb   lista ejb (DataRow)
     */
    @Override
    public void put(String key, List<IDataRow> listEjb) {
        if (key.isEmpty()) {
            key = "1";
        }
        mapResult.put(key, listEjb);
    }

    /**
     * Devuelve un map conteniendo todas las listas ejb que han sido procesados.
     * @return map con las listas ejb
     */
    @Override
    public Map<String, List<IDataRow>> getMapResult() {
        return mapResult;
    }

    /**
     * Devuelve una lista ejb del conjunto que ha sido procesado.
     * @param key   identificador de la lista
     * @return  lista ejb con los datos que han sido grabados.
     */
    @Override
    public List<IDataRow> getListEjb(String key) {
        if (key.isEmpty()) {
            key = "1";
        }
        return mapResult.get(key);
    }
    
    /**
     * Determina si se eliminarón de las listas de registros los elementos
     * que han sido marcados para ser eliminados.
     * @return si fue o no eliminado los registros marcados para tal efecto.
     */
    @Override
    public Boolean isRemoveDeleted() {
        return removeDeleted;
    }

    @Override
    public void setRemoveDeleted(Boolean removeDeleted) {
        this.removeDeleted = removeDeleted;
    }

    
    /**
     * Devuelve verdadero si la operación fue un exito o falso si no lo fue.
     * @return verdadero o falso
     */
    @Override
    public Boolean isSuccessFul() {
        return success;
    }

    @Override
    public void setSuccess(Boolean success) {
        this.success = success;
    }

    /**
     * 
     * @return devuelve una cadena de errores si lo hubiese.
     */
    @Override
    public String getErrorMsg() {
        return errorMsg;
    }

    /**
     * Asigna el mensaje de error en caso de haberse producido una excepción.
     * @param error 
     */
    @Override
    public void setErrorMsg(String error) {
        errorMsg = error;
    }

    /**
     * Devuelve un map con la lista de todos los errores.
     * @return map con los errores.
     */
    @Override
    public Map<String, IErrorReg> getErrorsMap() {
        return errorsMap;
    }

    /**
     * Asigna el map con los errores
     * @param error 
     */
    @Override
    public void setErrorsMap(Map<String, IErrorReg> error) {
        errorsMap = error;
    }
    
    /**
     * Agrega a una lista las filas procesadas.
     * @param row 
     */
    @Override
    public void setRowsUpdated(IDataRow row) {
        List<IDataRow> list = new ArrayList();
        list.add(row);
        setRowsUpdated(list);
        rowResult = list.get(0);
    }

    /**
     * Agrega a una lista las filas procesadas.
     * @param rows
     */
    @Override
    public void setRowsUpdated(List<? extends IDataRow> rows) {
        IDataSet dataSet = new DataSet();
        dataSet.add("1", rows);
        setRowsUpdated(dataSet);
    }

    /**
     * Asigna un dataSet conteniendo todas las lista de registros que fuerón procesados.
     * @param <T>
     * @param dataSetLocal
     */
    @Override
    public <T extends IDataRow> void setRowsUpdated(IDataSet dataSetLocal) {
        dataSetLocal.getMapListSet().entrySet().forEach((mapLocal) -> {
            List<T> rowsLocal  = (List<T>)mapLocal.getValue();
            List<T> rowsRemote = (List<T>)mapResult.get(mapLocal.getKey());
            if (rowsRemote != null) {
                //Recorrer objetos locales                
                for (int i=0; i < rowsLocal.size();i++) {
                    T rowLocal = rowsLocal.get(i);
                    // Buscar en los objetos remotos actualizados
                    for (T rowRemote : rowsRemote) {
                        if (rowLocal.equivalent(rowRemote)) {
                            // Asignar el objeto actualizado
                            rowsLocal.set(i, rowRemote);
                        }
                    }
                }
            }
        });
    }

    /**
     * En caso de grabarse un solo registro devuelve el registro con los datos
     * del identificador generado en la base de datos.
     * @return registro grabado.
     */
    @Override
    public IDataRow getRowResult() {
        return rowResult;
    }
    
    /**
     * Devuelve el error como Exception si lo hubiese
     * @return exception
     */
    @Override
    public Exception getException(){
        return exception;
    }
    
    /**
     * Asigna la exception si lo hubiese
     * @param ex 
     */
    @Override
    public void setException(Exception ex){
        exception = ex;
    }
}

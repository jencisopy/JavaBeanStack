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

package org.javabeanstack.datactrl.events;

import org.apache.log4j.Logger;
import org.javabeanstack.datactrl.IDataObject;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.events.IDataEvents;

/**
 * Esta clase se encarga de interceptar los eventos producidos en el DataObject
 * 
 * @author Jorge Enciso
 * @param <O>
 * @param <T>
 */
public abstract class AbstractDataEvents<O extends IDataObject, T extends IDataRow> implements IDataEvents<O,T> {
    private static final Logger LOGGER = Logger.getLogger(AbstractDataEvents.class);
    
    private O context;

    @Override
    public O getContext() {
        return context;
    }

    @Override
    public void setContext(O context) {
        this.context = context;
    }
    
    /**
     * Se ejecuta posterior a la ejecución del metodo allowAction del DataObject.
     * @return verdadero si permite la operación o falso si no.
     */
    @Override
    public boolean onAllowAction() {
        LOGGER.debug("onAllowAction IN");
        return true;
    }

    /**
     * Se ejecuta antes de mover el puntero del registro
     * @param curRow    registro actual
     * @return verdadero o falso  si va mover o no el puntero del registro.
     */
    @Override
    public boolean beforeRowMove(T curRow) {
        if (curRow != null){
            LOGGER.debug("beforeRowMov IN " + curRow.getId());                    
        }
        return true;
    }

    /**
     * Se ejecuta posterior a haberse cambiado el puntero del registro.
     * @param newRow el registro posicionado.
     */
    @Override
    public void afterRowMove(T newRow) {
        if (newRow != null){
            LOGGER.debug("afterRowMov IN " + newRow.getId());                    
        }
    }

    /**
     * Se ejecuta antes buscar los registros en la base de datos.
     * @param order     orden de la selección de datos.
     * @param filter    filtro de datos.
     * @param readwrite si es lectura/escritura
     * @param maxrows   maxima cantidad de registros a recuperar.
     */
    @Override
    public void beforeOpen(String order, String filter, boolean readwrite, int maxrows) {
        LOGGER.debug("beforeOpen IN ");                    
    }

    /**
     * Se ejecuta antes de recuperar los datos de la base de datos.
     */
    @Override
    public void beforeDataFill() {
        LOGGER.debug("beforeDataFill IN ");                            
    }

    /**
     * Se ejecuta posterior a la recuperación de los registros de la base de datos.
     */
    @Override
    public void afterDataFill() {
        LOGGER.debug("afterDataFill IN ");
    }

    /**
     * Se ejecuta posterior a la recuperación de los registros de la base de datos.
     * @param order     orden de la selección de datos.
     * @param filter    filtro de datos.    
     * @param readwrite si es lectura/escritura.
     * @param maxrows   maxima cantidad de registros que deberian haberse recuperado.
     */
    @Override
    public void afterOpen(String order, String filter, boolean readwrite, int maxrows) {
        LOGGER.debug("afterOpen IN ");        
    }

    /**
     * Se ejecuta antes de recuperar los registros de la base de datos.
     */
    @Override
    public void beforeRequery() {
        LOGGER.debug("beforeRequery IN ");        
    }

    /**
     * Se ejecuta posterior a haberse recuperado los registros de la base de datos.
     */
    @Override
    public void afterRequery() {
        LOGGER.debug("afterRequery IN ");                
    }

    /**
     * Se ejecuta antes de refrescar los datos de un registro de la base de datos.
     * @param row registro a refrescar
     */
    @Override
    public void beforeRefreshRow(T row) {
        LOGGER.debug("beforeRefreshRow IN ");                
    }

    /**
     * Se ejecuta posterior a refrescar un registro de la base de datos.
     * @param row registro refrescado.
     */
    @Override
    public void afterRefreshRow(T row) {
        LOGGER.debug("afterRefreshRow IN ");                
    }

    /**
     * Se ejecuta antes del metodo insertRow
     * @param newRow    fila a ser insertada
     * @return  verdadero o falso si se permite o no la inserción.
     */
    @Override
    public boolean beforeInsertRow(T newRow) {
        LOGGER.debug("beforeInsertRow IN ");                
        return true;
    }

    /**
     * Se ejecuta posterior al metodo insertRow
     * @param row registro insertado
     */
    @Override
    public void afterInsertRow(T row) {
        LOGGER.debug("afterInsertRow IN ");                
    }

    /**
     * Se ejecuta antes del metodo deleteRow
     * @param row   fila a ser marcada para eliminarse.
     * @return verdadero o falso si es permitido o no  la operación.
     */
    @Override
    public boolean beforeDeleteRow(T row) {
        LOGGER.debug("beforeDeleteRow IN ");                
        return true;
    }

    /**
     * Se ejecuta posterior al metodo deleteRow.
     */
    @Override
    public void afterDeleteRow() {
        LOGGER.debug("afterDeleteRow IN ");                
    }

    /**
     * Se ejecuta antes del metodo setField.
     * 
     * @param row       registro
     * @param fieldname nombre del campo
     * @param oldValue  valor anterior
     * @param newValue  nuevo valor.
     * @return verdadero o falso si se permite la modificación del campo
     */
    @Override
    public boolean beforeSetField(T row, String fieldname, Object oldValue, Object newValue) {
        LOGGER.debug("beforeSetField IN ");                
        return true;
    }

    /**
     * Se ejecuta posterior al metodo setField
     *  
     * @param row           registro
     * @param fieldname     nombre del campo    
     * @param oldValue      valor anterior
     * @param newValue      nuevo valor
     * @return verdadero o falso si tuvo exito.
     */
    @Override
    public boolean afterSetField(T row, String fieldname, Object oldValue, Object newValue) {
        LOGGER.debug("afterSetField IN ");                
        return true;
    }

    /**
     * Se ejecuta antes del metodo update
     * @param allRows   si se va a procesar todos los registros
     * @return verdadero o falso si se permite la ejecución de update.
     */
    @Override
    public boolean beforeUpdate(boolean allRows) {
        LOGGER.debug("beforeUpdate IN ");   
        return true;
    }

    /**
     * Se ejecuta antes del metodo checkData
     * @param allRows si se esta procesando todos los registros modificados, o solo el actual.
     */
    @Override
    public void beforeCheckData(boolean allRows) {
        LOGGER.debug("beforeCheckData IN ");                        
    }

    /**
     * Se ejecuta posterior al metodo checkData
     * @param allRows si se proceso todos los registros.
     */
    @Override
    public void afterCheckData(boolean allRows) {
        LOGGER.debug("afterCheckData IN ");                                
    }

    /**
     * Se ejecuta posterior al metodo update
     * @param allRows se se proceso todos los registros modificados.
     */
    @Override
    public void afterUpdate(boolean allRows) {
        LOGGER.debug("afterUpdate IN ");                                
    }

    /**
     * Se ejecuta antes de cerrar el dataObject
     */
    @Override
    public void beforeClose() {
        LOGGER.debug("beforeClose IN ");                                
    }

    /**
     * Se ejecuta posterior a cerrar el dataObject.
     */
    @Override
    public void afterClose() {
        LOGGER.debug("afterClose IN ");                                
    }
}

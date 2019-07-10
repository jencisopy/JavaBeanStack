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

package org.javabeanstack.data.model;

import org.javabeanstack.datactrl.IDataObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.data.IDataSet;
import org.javabeanstack.data.events.IDAOEvents;

/**
 * Esta clase se utiliza para enviar los set de datos para la actualización en 
 * la base de datos.
 * 
 * @author Jorge Enciso
 */
public class DataSet implements IDataSet {
    private Date lastProcess;
    
    private final Map<String, IDataObject> listDataObject = new LinkedHashMap();
    private List<List<? extends IDataRow>> listSet  = new LinkedList();
    private final Map<String, List<? extends IDataRow>> mapSet = new LinkedHashMap();
    private final Map<String, IDAOEvents> mapSetEvents = new LinkedHashMap();

    /**
     * Devuelve de cada set de datos los registros que fuerón modificados y que
     * se deben grabar en la base de datos.
     * 
     * @return set de datos que fuerón modificados.
     */
    @Override
    public IDataSet getChanged() {
        IDataSet dataSet = new DataSet();
        mapSet.entrySet().forEach( entry -> {
            dataSet.add(entry.getKey(), getRowsChanged((List<IDataRow>)entry.getValue()),getEvent(entry.getKey()));
        });
        return dataSet;
    }

    /**
     * Devuelve una lista de elementos o registros que fuerón modificados y que
     * deben guardarse en la base de datos.
     * @param rows  lista de registros a ser analizados.    
     * @return registros modificados.
     */
    private List<IDataRow> getRowsChanged(List<IDataRow> rows) {
        List<IDataRow> rowsChanged = new ArrayList();
        for (int i = 0; i < rows.size(); i++) {
            IDataRow row = rows.get(i);
            if (row.getAction() != 0) {
                rowsChanged.add(row);
            }
        }
        return rowsChanged;
    }
    
    
    @Override
    public Date getLastProcess() {
        return lastProcess;
    }

    @Override
    public void setLastProcess(Date date) {
        lastProcess = date;
    }
    
    /**
     * Devuelve un set de datos.
     * @param key clave del map conteniendo el set de datos.
     * @return set de datos solicitado.
     */
    @Override
    public List<? extends IDataRow> get(String key) {
        return mapSet.get(key);
    }

    /**
     * Devuelve un set de eventos.
     * @param key clave del map conteniendo el set de eventos.
     * @return set de eventos solicitado.
     */
    @Override
    public IDAOEvents getEvent(String key) {
        return mapSetEvents.get(key);
    }
    
    
    @Override
    public void addEvents(String key, IDAOEvents events){
        mapSetEvents.put(key, events);
    }
    
    /**
     * Devuelve un set de datos.
     * @param setNumber nro de set de datos.
     * @return set de datos solicitado.
     */
    @Override
    public List<? extends IDataRow> get(int setNumber) {
        if (setNumber >= 0 && listSet.size() > setNumber){
            return listSet.get(setNumber);
        }
        return null;
    }

    /**
     * Devuelve un map con los DataObjects
     * @return un map con los DataObjects
     */
    @Override
    public Map<String, IDataObject> getMapDataObject() {
        return listDataObject;
    }

    /**
     * Devuelve un map con todos los sets de datos.
     * @return map con los sets de datos
     */
    @Override
    public Map<String, List<? extends IDataRow>> getMapListSet() {
        return mapSet;
    }
    
    
    /**
     * Agrega un set de datos al map.
     * @param key   clave recuperar luego el set de datos.
     * @param set   lista con los registros
     */
    @Override
    public void add(String key, List<? extends IDataRow> set) {
        List old = mapSet.put(key, set);
        if (old == null){
            listSet.add(set);
        }
        else{
            reCreateList();
        }
    }

    /**
     * Agrega una fila o registro a un set de datos.
     * @param key   clave que será utilizada luego para recuperar el registro
     * @param row   registro.
     */
    @Override
    public void add(String key, IDataRow row) {
        List<IDataRow> newValue = new ArrayList<>();
        newValue.add(row);
        List oldValue = mapSet.put(key, newValue);
        if (oldValue == null){
            listSet.add(newValue);
        }
        else{
            reCreateList();
        }
    }

    /**
     * Agrega un set de datos al map.
     * @param key   clave recuperar luego el set de datos.
     * @param set   lista con los registros
     * @param events eventos pre y post grabación
     */
    @Override
    public void add(String key, List<? extends IDataRow> set, IDAOEvents events) {
        List old = mapSet.put(key, set);
        mapSetEvents.put(key, events);
        if (old == null){
            listSet.add(set);
        }
        else{
            reCreateList();
        }
    }
    
    /**
     * Agrega un dataobject a la lista de DataObjects.
     * @param key   clave que luego se utilizará para recuperar el dataObject
     * @param dataObject 
     */
    @Override
    public void addDataObject(String key, IDataObject dataObject) {
        listDataObject.put(key, dataObject);
        add(key,dataObject.getDataRows());
    }
    
    /**
     * 
     * @return devuelve la cantidad de sets de datos.
     */
    @Override
    public int size() {
        return mapSet.size();
    }
    
    private void reCreateList(){
        listSet = new LinkedList();
        mapSet.entrySet().forEach( entry -> {
            listSet.add(entry.getValue());
        });
    }
}

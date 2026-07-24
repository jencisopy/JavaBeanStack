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
package org.javabeanstack.data;

import org.javabeanstack.datactrl.IDataObject;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.javabeanstack.data.events.IDAOEvents;

/**
 * Contrato de un conjunto de datos: agrupa varias listas de registros
 * ({@link IDataRow}) identificadas por una clave, junto con sus manejadores de
 * eventos ({@link IDAOEvents}) y sus objetos de datos ({@link IDataObject}).
 *
 * <p>Permite tratar múltiples listas como una única unidad de trabajo que la
 * capa de datos persiste en conjunto (ver {@link IDataLink}). La implementación
 * de referencia es {@code org.javabeanstack.data.model.DataSet}.</p>
 *
 * @author Jorge Enciso
 */
public interface IDataSet extends Serializable {
    /**
     * Devuelve un nuevo conjunto que contiene solo los registros con una
     * operación CRUD pendiente (los que deben guardarse en la base de datos).
     *
     * @return conjunto con los registros modificados.
     */
    public IDataSet getChanged();

    /**
     * Devuelve la fecha del último procesamiento del conjunto.
     *
     * @return fecha del último procesamiento.
     */
    public Date getLastProcess();

    /**
     * Asigna la fecha del último procesamiento del conjunto.
     *
     * @param date fecha del último procesamiento.
     */
    public void setLastProcess(Date date);

    /**
     * Devuelve la lista de registros ubicada en la posición indicada.
     *
     * @param setNumber posición (ordinal) de la lista dentro del conjunto.
     * @return lista de registros.
     */
    public List<? extends IDataRow> get(int setNumber);

    /**
     * Devuelve la lista de registros asociada a la clave indicada.
     *
     * @param key clave de la lista.
     * @return lista de registros.
     */
    public List<? extends IDataRow> get(String key);

    /**
     * Devuelve el mapa con todas las listas de registros del conjunto.
     *
     * @return mapa clave → lista de registros.
     */
    public Map<String, List<? extends IDataRow>> getMapListSet();

    /**
     * Devuelve el mapa con los objetos de datos asociados al conjunto.
     *
     * @return mapa clave → objeto de datos.
     */
    public Map<String, IDataObject> getMapDataObject();

    /**
     * Agrega una lista de registros bajo la clave indicada.
     *
     * @param key clave de la lista.
     * @param set lista de registros.
     */
    public void add(String key, List<? extends IDataRow> set);

    /**
     * Agrega un registro a la lista asociada a la clave indicada.
     *
     * @param key clave de la lista.
     * @param row registro a agregar.
     */
    public void add(String key, IDataRow row);

    /**
     * Agrega una lista de registros con su manejador de eventos bajo la clave
     * indicada.
     *
     * @param key clave de la lista.
     * @param set lista de registros.
     * @param events manejador de eventos del DAO para esta lista.
     */
    public void add(String key, List<? extends IDataRow> set, IDAOEvents events);

    /**
     * Asocia un objeto de datos al conjunto bajo la clave indicada.
     *
     * @param key clave del objeto de datos.
     * @param dataObject objeto de datos.
     */
    public void addDataObject(String key, IDataObject dataObject);

    /**
     * Devuelve el manejador de eventos asociado a la clave indicada.
     *
     * @param key clave de la lista.
     * @return manejador de eventos, o {@code null} si no hay.
     */
    public IDAOEvents getEvent(String key);

    /**
     * Asocia un manejador de eventos a la clave indicada.
     *
     * @param key clave de la lista.
     * @param events manejador de eventos del DAO.
     */
    public void addEvents(String key, IDAOEvents events);

    /**
     * Devuelve la cantidad de listas contenidas en el conjunto.
     *
     * @return número de listas del conjunto.
     */
    public int size();
}

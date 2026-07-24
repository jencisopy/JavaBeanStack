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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.javabeanstack.error.IErrorReg;

/**
 * Contrato del resultado de una operación de persistencia sobre un
 * {@link IDataSet} (grabación de una o varias listas de registros).
 *
 * <p>Reúne el estado de éxito, los errores/excepción ocurridos, los registros
 * efectivamente actualizados y un mapa (por clave de lista) con los registros
 * resultantes. La implementación de referencia es
 * {@code org.javabeanstack.data.model.DataResult}.</p>
 *
 * @author Jorge Enciso
 */
public interface IDataResult extends Serializable{
    /**
     * Agrega al resultado la lista de registros procesada bajo la clave indicada.
     *
     * @param key clave de la lista.
     * @param listEjb lista de registros resultante.
     */
    public void put(String key, List<IDataRow> listEjb);

    /**
     * Devuelve el mapa con todas las listas de registros resultantes.
     *
     * @return mapa clave → lista de registros.
     */
    public Map<String, List<IDataRow>> getMapResult();

    /**
     * Devuelve la lista de registros resultante asociada a la clave indicada.
     *
     * @param key clave de la lista.
     * @return lista de registros.
     */
    public List<IDataRow> getListEjb(String key);

    /**
     * Indica si la operación finalizó con éxito.
     *
     * @return verdadero si fue exitosa, falso si no.
     */
    public Boolean isSuccessFul();

    /**
     * Indica si los registros marcados para borrar fueron removidos de las
     * listas de resultado.
     *
     * @return verdadero si fueron removidos, falso si no.
     */
    public Boolean isRemoveDeleted();

    /**
     * Devuelve el texto concatenado de los mensajes de error de la operación.
     *
     * @return mensajes de error.
     */
    public String getErrorMsg();

    /**
     * Devuelve el primer error registrado en la operación.
     *
     * @return primer error, o {@code null} si no hubo.
     */
    public IErrorReg getFirstError();

    /**
     * Devuelve el mapa de errores por campo/registro.
     *
     * @return mapa clave → error.
     */
    public Map<String, IErrorReg> getErrorsMap();

    /**
     * Devuelve la excepción capturada durante la operación, si la hubo.
     *
     * @return excepción, o {@code null} si no hubo.
     */
    public Exception getException();

    /**
     * Devuelve el primer registro actualizado por la operación.
     *
     * @param <T> tipo del registro.
     * @return registro actualizado.
     */
    public <T extends IDataRow> T getRowUpdated();

    /**
     * Devuelve todos los registros actualizados por la operación.
     *
     * @param <T> tipo de los registros.
     * @return lista de registros actualizados.
     */
    public <T extends IDataRow> List<T> getRowsUpdated();

    /**
     * Devuelve el primer registro actualizado de la lista indicada.
     *
     * @param <T> tipo del registro.
     * @param key clave de la lista.
     * @return registro actualizado.
     */
    public <T extends IDataRow> T getRowUpdated(String key);

    /**
     * Devuelve los registros actualizados de la lista indicada.
     *
     * @param <T> tipo de los registros.
     * @param key clave de la lista.
     * @return lista de registros actualizados.
     */
    public <T extends IDataRow> List<T> getRowsUpdated(String key);

    /**
     * Asigna la excepción capturada durante la operación.
     *
     * @param ex excepción.
     */
    public void setException(Exception ex);

    /**
     * Asigna el mensaje de error de la operación.
     *
     * @param error mensaje de error.
     */
    public void setErrorMsg(String error);

    /**
     * Asigna el mapa de errores por campo/registro.
     *
     * @param error mapa clave → error.
     */
    public void setErrorsMap(Map<String, IErrorReg> error);

    /**
     * Asigna si los registros borrados deben removerse de las listas de resultado.
     *
     * @param remove verdadero para removerlos.
     */
    public void setRemoveDeleted(Boolean remove);

    /**
     * Asigna el estado de éxito de la operación.
     *
     * @param success verdadero si fue exitosa.
     */
    public void setSuccess(Boolean success);

    /**
     * Registra un único registro como actualizado.
     *
     * @param <T> tipo del registro.
     * @param row registro actualizado.
     */
    public <T extends IDataRow> void setRowUpdated(T row);

    /**
     * Agrega un registro a la lista de actualizados.
     *
     * @param <T> tipo del registro.
     * @param row registro actualizado.
     */
    public <T extends IDataRow> void setRowsUpdated(T row);

    /**
     * Asigna la lista de registros actualizados.
     *
     * @param <T> tipo de los registros.
     * @param rows lista de registros actualizados.
     */
    public <T extends IDataRow> void setRowsUpdated(List<T> rows);

    /**
     * Toma los registros actualizados a partir de un conjunto de datos.
     *
     * @param <T> tipo de los registros.
     * @param dataSet conjunto de datos con los registros actualizados.
     */
    public <T extends IDataRow> void setRowsUpdated(IDataSet dataSet);
}

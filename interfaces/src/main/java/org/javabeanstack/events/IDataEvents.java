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
package org.javabeanstack.events;

import java.io.Serializable;
import org.javabeanstack.datactrl.IDataObject;
import org.javabeanstack.data.IDataRow;

/**
 * Contrato de los eventos del ciclo de vida de un objeto de datos
 * ({@link IDataObject}): puntos de extensión que se disparan antes y después de
 * abrir, navegar, insertar, borrar, asignar campos, validar, grabar y cerrar.
 *
 * <p>Las clases derivadas del cursor redefinen estos métodos para agregar lógica
 * de negocio; los métodos {@code before*} que devuelven {@code boolean} permiten
 * cancelar la operación devolviendo {@code false}.</p>
 *
 * @author Jorge Enciso
 * @param <O> tipo del objeto de datos (contexto).
 * @param <T> tipo de la entidad manejada.
 */
public interface IDataEvents<O extends IDataObject,T extends IDataRow> extends Serializable{
    /**
     * Devuelve el objeto de datos (contexto) al que pertenecen los eventos.
     *
     * @return objeto de datos.
     */
    public O getContext();

    /**
     * Asigna el objeto de datos (contexto) de los eventos.
     *
     * @param context objeto de datos.
     */
    public void setContext(O context);

    /**
     * Determina si la acción solicitada está permitida.
     *
     * @return verdadero para permitir la acción, falso para bloquearla.
     */
    public boolean onAllowAction();

    /**
     * Se ejecuta antes de mover el cursor a otro registro.
     *
     * @param curRow registro actual (antes de mover).
     * @return verdadero para permitir el movimiento, falso para cancelarlo.
     */
    public boolean beforeRowMove(T curRow);

    /**
     * Se ejecuta después de mover el cursor a otro registro.
     *
     * @param newRow nuevo registro actual.
     */
    public void    afterRowMove(T newRow);

    /**
     * Se ejecuta antes de abrir el cursor.
     *
     * @param order cláusula de ordenamiento.
     * @param filter filtro.
     * @param readwrite modo lectura-escritura.
     * @param maxrows máximo de registros.
     */
    public void    beforeOpen(String order, String filter,boolean readwrite, int maxrows);

    /**
     * Se ejecuta antes de poblar los datos del cursor.
     */
    public void    beforeDataFill();

    /**
     * Se ejecuta después de poblar los datos del cursor.
     */
    public void    afterDataFill();

    /**
     * Se ejecuta después de abrir el cursor.
     *
     * @param order cláusula de ordenamiento.
     * @param filter filtro.
     * @param readwrite modo lectura-escritura.
     * @param maxrows máximo de registros.
     */
    public void    afterOpen(String order, String filter,boolean readwrite, int maxrows);

    /**
     * Se ejecuta antes de volver a ejecutar la consulta.
     */
    public void    beforeRequery();

    /**
     * Se ejecuta después de volver a ejecutar la consulta.
     */
    public void    afterRequery();

    /**
     * Se ejecuta antes de refrescar un registro desde la base de datos.
     *
     * @param row registro a refrescar.
     */
    public void    beforeRefreshRow(T row);

    /**
     * Se ejecuta después de refrescar un registro desde la base de datos.
     *
     * @param row registro refrescado.
     */
    public void    afterRefreshRow(T row);

    /**
     * Se ejecuta antes de insertar un registro.
     *
     * @param newRow nuevo registro.
     * @return verdadero para permitir la inserción, falso para cancelarla.
     */
    public boolean beforeInsertRow(T newRow);

    /**
     * Se ejecuta después de insertar un registro.
     *
     * @param row registro insertado.
     */
    public void    afterInsertRow(T row);

    /**
     * Se ejecuta antes de borrar un registro.
     *
     * @param row registro a borrar.
     * @return verdadero para permitir el borrado, falso para cancelarlo.
     */
    public boolean beforeDeleteRow(T row);

    /**
     * Se ejecuta después de borrar un registro.
     */
    public void    afterDeleteRow();

    /**
     * Se ejecuta antes de asignar un campo del registro.
     *
     * @param row registro afectado.
     * @param fieldname nombre del campo.
     * @param oldValue valor anterior.
     * @param newValue valor nuevo.
     * @return verdadero para permitir la asignación, falso para cancelarla.
     */
    public boolean beforeSetField(T row, String fieldname, Object oldValue, Object newValue);

    /**
     * Se ejecuta después de asignar un campo del registro.
     *
     * @param row registro afectado.
     * @param fieldname nombre del campo.
     * @param oldValue valor anterior.
     * @param newValue valor nuevo.
     * @return verdadero si la asignación se considera válida.
     */
    public boolean afterSetField(T row, String fieldname, Object oldValue, Object newValue);

    /**
     * Se ejecuta antes de grabar los cambios.
     *
     * @param allRows verdadero si se graban todos los registros.
     * @return verdadero para permitir la grabación, falso para cancelarla.
     */
    public boolean beforeUpdate(boolean allRows);

    /**
     * Se ejecuta antes de validar los datos.
     *
     * @param allRows verdadero si se validan todos los registros.
     */
    public void    beforeCheckData(boolean allRows);

    /**
     * Se ejecuta después de validar los datos.
     *
     * @param allRows verdadero si se validaron todos los registros.
     */
    public void    afterCheckData(boolean allRows);

    /**
     * Se ejecuta después de grabar los cambios.
     *
     * @param allRows verdadero si se grabaron todos los registros.
     */
    public void    afterUpdate(boolean allRows);

    /**
     * Se ejecuta antes de cerrar el cursor.
     */
    public void    beforeClose();

    /**
     * Se ejecuta después de cerrar el cursor.
     */
    public void    afterClose();
}

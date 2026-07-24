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
package org.javabeanstack.data.services;

import java.util.List;
import java.util.Map;
import org.javabeanstack.data.IDataQueryModel;
import org.javabeanstack.data.IDataResult;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.data.IGenericDAO;
import org.javabeanstack.data.events.IDAOEvents;
import org.javabeanstack.error.IErrorReg;

/**
 * Contrato del servicio de datos: extiende el DAO genérico
 * {@link IGenericDAO} con la validación de negocio previa a la persistencia
 * (claves únicas, claves foráneas y validación por campo), además de utilidades
 * de consulta, copia e importación de registros.
 *
 * <p>La implementación de referencia es
 * {@code org.javabeanstack.data.AbstractDataService}; las entidades de
 * aplicación exponen su propio servicio extendiendo esta interfaz.</p>
 *
 * @author Jorge Enciso
 */
public interface IDataService extends IGenericDAO{
    /**
     * Inicializa/marca los campos del registro que deben verificarse.
     *
     * @param <T> tipo del registro.
     * @param row registro a preparar.
     * @return el registro con sus campos a verificar marcados.
     */
    <T extends IDataRow> T setFieldsToCheck(T row);

    /**
     * Verifica que el registro no viole la clave única (que no exista otro
     * registro con la misma clave).
     *
     * @param <T> tipo del registro.
     * @param sessionId identificador de la sesión del usuario.
     * @param row registro a validar.
     * @return verdadero si la clave única es válida, falso si está duplicada.
     * @throws Exception si ocurre un error al consultar la base de datos.
     */
    <T extends IDataRow> boolean checkUniqueKey(String sessionId, T row) throws Exception;

    /**
     * Verifica que el valor de una clave foránea del registro exista en la
     * tabla referenciada.
     *
     * @param <T> tipo del registro.
     * @param sessionId identificador de la sesión del usuario.
     * @param row registro a validar.
     * @param fieldName campo de clave foránea a verificar.
     * @return verdadero si la referencia existe, falso si no.
     * @throws Exception si ocurre un error al consultar la base de datos.
     */
    <T extends IDataRow> boolean checkForeignKey(String sessionId, T row, String fieldName) throws Exception;

    /**
     * Ejecuta la validación completa del registro y devuelve los errores por campo.
     *
     * @param <T> tipo del registro.
     * @param sessionId identificador de la sesión del usuario.
     * @param row registro a validar.
     * @return mapa campo → error (vacío si el registro es válido).
     */
    <T extends IDataRow> Map<String, IErrorReg> checkDataRow(String sessionId, T row);

    /**
     * Valida un único campo del registro.
     *
     * @param <T> tipo del registro.
     * @param sessionId identificador de la sesión del usuario.
     * @param row registro a validar.
     * @param fieldName campo a validar.
     * @return error del campo, o {@code null} si es válido.
     */
    <T extends IDataRow> IErrorReg checkFieldValue(String sessionId, T row, String fieldName);

    /**
     * Persiste el registro aplicando previamente la validación de negocio.
     *
     * @param <T> tipo del registro.
     * @param sessionId identificador de la sesión del usuario.
     * @param row registro a guardar.
     * @return resultado de la operación con el estado y los registros actualizados.
     * @throws Exception si la validación o la persistencia falla.
     */
    <T extends IDataRow> IDataResult save(String sessionId, T row) throws Exception;

    /**
     * Recupera una lista de registros según orden, filtro, parámetros y paginación.
     *
     * @param <T> tipo del registro.
     * @param sessionId identificador de la sesión del usuario.
     * @param type clase de la entidad a consultar.
     * @param order cláusula de ordenamiento (JPQL).
     * @param filter cláusula de filtro (JPQL).
     * @param params parámetros nombrados del filtro.
     * @param firstRow índice del primer registro a devolver.
     * @param maxRows cantidad máxima de registros a devolver.
     * @return lista de registros.
     * @throws Exception si ocurre un error al consultar la base de datos.
     */
    <T extends IDataRow> List<T> getDataRows(String sessionId, Class<T> type, String order, String filter, Map<String, Object> params, int firstRow, int maxRows) throws Exception;

    /**
     * Construye la sentencia de selección (JPQL) que se ejecutaría para la
     * entidad, orden y filtro dados.
     *
     * @param <T> tipo del registro.
     * @param sessionId identificador de la sesión del usuario.
     * @param type clase de la entidad.
     * @param order cláusula de ordenamiento.
     * @param filter cláusula de filtro.
     * @return sentencia de selección.
     */
    <T extends IDataRow> String getSelectCmd(String sessionId, Class<T> type, String order, String filter);

    /**
     * Copia los valores del registro origen al registro destino en el contexto
     * del servicio.
     *
     * @param <T> tipo del registro origen.
     * @param <X> tipo del registro destino.
     * @param sessionId identificador de la sesión del usuario.
     * @param source registro origen.
     * @param target registro destino.
     * @return el registro destino con los valores copiados.
     * @throws Exception si falla la copia.
     */
    <T extends IDataRow, X extends IDataRow> X copyTo(String sessionId, T source, X target) throws Exception;

    /**
     * Importa datos desde un conjunto de resultados de consulta hacia entidades
     * destino.
     *
     * @param <S> tipo de la entidad origen.
     * @param <T> tipo de la entidad destino.
     * @param sessionId identificador de la sesión del usuario.
     * @param dataQuerySource filas de origen a importar.
     * @param ejbClassSource clase de la entidad origen.
     * @param ejbClassTarget clase de la entidad destino.
     * @param params parámetros de la importación.
     * @throws Exception si la importación falla.
     */
    <S extends IDataRow, T extends IDataRow> void importFrom(String sessionId, List<IDataQueryModel> dataQuerySource, Class<S> ejbClassSource, Class<T> ejbClassTarget, Map<String, Object> params) throws Exception;

    /**
     * Devuelve el manejador de eventos del DAO asociado al servicio.
     *
     * @return manejador de eventos.
     */
    IDAOEvents getEvents();
}

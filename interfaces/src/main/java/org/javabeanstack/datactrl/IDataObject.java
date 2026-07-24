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
package org.javabeanstack.datactrl;

import org.javabeanstack.error.IErrorReg;
import java.util.List;
import java.util.Map;
import org.javabeanstack.data.IDataLink;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.data.IDataSet;
import org.javabeanstack.events.IDataEvents;

/**
 * Contrato de un objeto de datos (cursor) sobre una lista de registros
 * ({@link IDataRow}) de un mismo tipo, apoyado en una conexión de datos
 * ({@link IDataLink}).
 *
 * <p>Administra la consulta (filtro, orden, paginación), la navegación entre
 * registros (mover/buscar), la lectura y asignación de campos del registro
 * actual, las operaciones de alta/baja/modificación y la confirmación o
 * reversión de los cambios contra la base de datos. Es la abstracción base de
 * los managed beans CRUD de la capa web. La implementación de referencia es
 * {@code org.javabeanstack.datactrl.AbstractDataObject}.</p>
 *
 * @author Jorge Enciso
 * @param <T> tipo de la entidad manejada por el cursor.
 */
public interface IDataObject <T extends IDataRow>{
    /**
     * Indica si el cursor está en modo lectura-escritura.
     *
     * @return verdadero si permite modificaciones, falso si es solo lectura.
     */
    public boolean      isReadWrite();

    /**
     * Devuelve la conexión de datos principal utilizada por el cursor.
     *
     * @return conexión de datos.
     */
    public IDataLink    getDAO();

    /**
     * Devuelve la conexión de datos hacia el catálogo (base maestra).
     *
     * @return conexión de datos del catálogo.
     */
    public IDataLink    getDAOCatalog();

    /**
     * Devuelve la clase de la entidad manejada por el cursor.
     *
     * @return clase de la entidad.
     */
    public Class<T>     getType();

    /**
     * Devuelve el filtro principal aplicado a la consulta.
     *
     * @return filtro principal.
     */
    public String       getFilter();

    /**
     * Devuelve el mapa de filtros adicionales aplicados a la consulta.
     *
     * @return mapa clave → filtro.
     */
    public Map<String, String> getFilters();

    /**
     * Construye la sentencia de filtro combinando los filtros aplicados.
     *
     * @param noMain verdadero para excluir el filtro principal.
     * @return sentencia de filtro.
     */
    public String       getFilterSentence(boolean noMain);

    /**
     * Devuelve la cláusula de ordenamiento de la consulta.
     *
     * @return cláusula de ordenamiento.
     */
    public String       getOrder();

    /**
     * Devuelve la última excepción de aplicación ocurrida en el cursor.
     *
     * @return excepción, o {@code null} si no hubo.
     */
    public Exception    getErrorApp();

    /**
     * Devuelve el mensaje de error del cursor.
     *
     * @param all verdadero para incluir todos los errores, falso solo el último.
     * @return mensaje de error.
     */
    public String       getErrorMsg(boolean all);

    /**
     * Devuelve el mensaje de error asociado a un registro.
     *
     * @param row registro.
     * @return mensaje de error del registro.
     */
    public String       getErrorMsg(IDataRow row);

    /**
     * Devuelve el mensaje de error asociado a un campo del registro actual.
     *
     * @param fieldName nombre del campo.
     * @return mensaje de error del campo.
     */
    public String       getErrorMsg(String fieldName);

    /**
     * Devuelve la lista de registros cargados en el cursor.
     *
     * @return lista de registros.
     */
    public List<T>      getDataRows();

    /**
     * Devuelve los registros modificados del cursor, indexados por su posición.
     *
     * @return mapa posición → registro modificado.
     */
    public Map<Integer, T> getDataRowsChanged();

    /**
     * Devuelve el registro actual (posición del cursor).
     *
     * @return registro actual.
     */
    public T            getRow();

    /**
     * Devuelve el último registro de la lista.
     *
     * @return último registro.
     */
    public T            getLastRow();

    /**
     * Devuelve la sentencia de selección que se ejecutará para poblar el cursor.
     *
     * @return sentencia de selección.
     */
    public String       getSelectCmd();

    /**
     * Devuelve la última consulta ejecutada por el cursor.
     *
     * @return última consulta.
     */
    public String       getLastQuery();

    /**
     * Devuelve el número (índice) del registro actual.
     *
     * @return índice del registro actual.
     */
    public int          getRecno();

    /**
     * Devuelve el índice del primer registro de la página.
     *
     * @return índice del primer registro.
     */
    public int          getFirstRow();

    /**
     * Devuelve la cantidad máxima de registros por página.
     *
     * @return máximo de registros.
     */
    public int          getMaxRows();

    /**
     * Devuelve la cantidad de registros cargados en el cursor.
     *
     * @return cantidad de registros.
     */
    public int          getRowCount();

    /**
     * Devuelve el estado (operación CRUD) del registro actual.
     *
     * @return estado del registro.
     */
    public int          getRecStatus();

    /**
     * Devuelve el identificador de la empresa activa.
     *
     * @return identificador de la empresa.
     */
    public Long         getIdcompany();

    /**
     * Devuelve el identificador de la empresa (alias {@code idempresa}).
     *
     * @return identificador de la empresa.
     */
    public Long         getIdempresa();

    /**
     * Devuelve el manejador de eventos del cursor.
     *
     * @return manejador de eventos.
     */
    public IDataEvents  getDataEvents();

    /**
     * Indica si el cursor muestra los registros marcados como borrados.
     *
     * @return verdadero si los muestra, falso si no.
     */
    public boolean      isShowDeletedRow();

    /**
     * Define si el cursor debe mostrar los registros marcados como borrados.
     *
     * @param showDeletedRow verdadero para mostrarlos.
     */
    public void         setShowDeletedRow(boolean showDeletedRow);

    /**
     * Define el modo lectura-escritura del cursor.
     *
     * @param readWrite verdadero para permitir modificaciones.
     */
    public void         setReadWrite(boolean readWrite);

    /**
     * Asigna la clase de la entidad manejada por el cursor.
     *
     * @param type clase de la entidad.
     */
    public void         setType(Class<T> type);

    /**
     * Asigna el filtro principal de la consulta.
     *
     * @param filter filtro principal.
     */
    public void         setFilter(String filter);

    /**
     * Asigna la cláusula de ordenamiento de la consulta.
     *
     * @param order cláusula de ordenamiento.
     */
    public void         setOrder(String order);

    /**
     * Asigna el índice del primer registro de la página.
     *
     * @param first índice del primer registro.
     */
    public void         setFirstRow(int first);

    /**
     * Asigna la cantidad máxima de registros por página.
     *
     * @param maxrow máximo de registros.
     */
    public void         setMaxRows(int maxrow);

    /**
     * Agrega un filtro adicional a la consulta.
     *
     * @param filter filtro a agregar.
     */
    public void         addFilter(String filter);

    /**
     * Agrega un filtro adicional identificado por una clave.
     *
     * @param key clave del filtro.
     * @param filter filtro a agregar.
     */
    public void         addFilter(String key, String filter);

    /**
     * Elimina todos los filtros adicionales.
     */
    public void         removeFilter();

    /**
     * Elimina el filtro adicional identificado por la clave indicada.
     *
     * @param key clave del filtro.
     */
    public void         removeFilter(String key);

    /**
     * Abre el cursor ejecutando la consulta con la configuración actual.
     *
     * @return verdadero si se abrió con éxito, falso si no.
     */
    public boolean      open();

    /**
     * Abre el cursor con orden, filtro, modo y paginación explícitos.
     *
     * @param order cláusula de ordenamiento.
     * @param filter filtro principal.
     * @param readwrite modo lectura-escritura.
     * @param maxrows máximo de registros.
     * @return verdadero si se abrió con éxito, falso si no.
     */
    public boolean      open(String order, String filter, Boolean readwrite, int maxrows);

    /**
     * Vuelve a ejecutar la consulta y recarga los registros.
     *
     * @return verdadero si tuvo éxito, falso si no.
     */
    public boolean      requery();

    /**
     * Vuelve a ejecutar la consulta agregando un filtro extra y sus parámetros.
     *
     * @param filterExtra filtro adicional.
     * @param filterParams parámetros del filtro.
     * @return verdadero si tuvo éxito, falso si no.
     */
    public boolean      requery(String filterExtra, Map<String, Object> filterParams);

    /**
     * Posiciona el cursor en el registro indicado.
     *
     * @param rownumber índice del registro.
     * @return verdadero si el índice es válido, falso si no.
     */
    public boolean      goTo(int rownumber);

    /**
     * Posiciona el cursor en un registro relativo a un índice base.
     *
     * @param rownumber índice base.
     * @param offset desplazamiento respecto del índice base.
     * @return verdadero si la posición es válida, falso si no.
     */
    public boolean      goTo(int rownumber, int offset);

    /**
     * Posiciona el cursor en el primer registro.
     *
     * @return verdadero si hay registros, falso si no.
     */
    public boolean      moveFirst();

    /**
     * Posiciona el cursor en el registro siguiente.
     *
     * @return verdadero si avanzó, falso si llegó al final.
     */
    public boolean      moveNext();

    /**
     * Posiciona el cursor en el registro anterior.
     *
     * @return verdadero si retrocedió, falso si estaba al inicio.
     */
    public boolean      movePrevious();

    /**
     * Posiciona el cursor en el último registro.
     *
     * @return verdadero si hay registros, falso si no.
     */
    public boolean      moveLast();

    /**
     * Busca un registro por el valor de un campo dentro de un rango de posiciones.
     *
     * @param field nombre del campo.
     * @param value valor a buscar.
     * @param begin índice inicial del rango.
     * @param end índice final del rango.
     * @return verdadero si lo encontró (y posicionó el cursor), falso si no.
     */
    public boolean      find(String field, Object value, int begin, int end);

    /**
     * Busca un registro por el valor de un campo en toda la lista.
     *
     * @param field nombre del campo.
     * @param value valor a buscar.
     * @return verdadero si lo encontró (y posicionó el cursor), falso si no.
     */
    public boolean      find(String field, Object value);

    /**
     * Continúa la última búsqueda desde la posición actual.
     *
     * @return verdadero si encontró otra coincidencia, falso si no.
     */
    public boolean      findNext();

    /**
     * Indica si el cursor está más allá del último registro (fin de datos).
     *
     * @return verdadero si está al final, falso si no.
     */
    public boolean      isEof();

    /**
     * Devuelve el valor nuevo (aún sin confirmar) de un campo del registro actual.
     *
     * @param fieldname nombre del campo.
     * @return valor nuevo del campo.
     */
    public Object       getNewValue(String fieldname);

    /**
     * Devuelve el valor por defecto de un campo.
     *
     * @param fieldname nombre del campo.
     * @return valor por defecto del campo.
     */
    public Object       getFieldDefaultValue(String fieldname);

    /**
     * Devuelve el valor de un campo del registro actual.
     *
     * @param fieldname nombre del campo.
     * @return valor del campo.
     */
    public Object       getField(String fieldname);

    /**
     * Devuelve el valor de un campo de un objeto relacionado del registro actual.
     *
     * @param objname nombre del objeto relacionado.
     * @param fieldname nombre del campo.
     * @return valor del campo.
     */
    public Object       getField(String objname, String fieldname);

    /**
     * Devuelve el valor original (previo a modificación) de un campo.
     *
     * @param fieldname nombre del campo.
     * @return valor original del campo.
     */
    public Object       getFieldOld(String fieldname);

    /**
     * Devuelve la entidad referenciada por una clave foránea del registro actual.
     *
     * @param fieldname nombre del campo de clave foránea.
     * @return entidad referenciada.
     */
    public IDataRow     getFieldObjFK(String fieldname);

    /**
     * Devuelve los parámetros del filtro de la consulta.
     *
     * @return mapa de parámetros del filtro.
     */
    public Map<String, Object> getFilterParams();

    /**
     * Asigna el valor de un campo del registro actual.
     *
     * @param fieldname nombre del campo.
     * @param value valor a asignar.
     * @return verdadero si tuvo éxito, falso si no.
     */
    public boolean      setField(String fieldname, Object value);

    /**
     * Asigna un campo del registro actual a partir de un mapa de parámetros.
     *
     * @param fieldname nombre del campo.
     * @param param parámetros para resolver el valor.
     * @return verdadero si tuvo éxito, falso si no.
     */
    public boolean      setField(String fieldname, Map<String, Object> param);

    /**
     * Asigna el valor de un campo, con opción de omitir el evento posterior.
     *
     * @param fieldname nombre del campo.
     * @param value valor a asignar.
     * @param noAfterSetField verdadero para no disparar el evento afterSetField.
     * @return verdadero si tuvo éxito, falso si no.
     */
    public boolean      setField(String fieldname, Object value, boolean noAfterSetField);

    /**
     * Asigna los parámetros del filtro de la consulta.
     *
     * @param filterParams mapa de parámetros del filtro.
     */
    public void         setFilterParams(Map<String, Object> filterParams);

    /**
     * Agrega parámetros al filtro de la consulta.
     *
     * @param filterParams parámetros a agregar.
     */
    public void         addFilterParams(Map filterParams);

    /**
     * Indica si existe un campo con el nombre indicado en la entidad.
     *
     * @param fieldname nombre del campo.
     * @return verdadero si existe, falso si no.
     */
    public boolean      isFieldExist(String fieldname);

    /**
     * Indica si existe un método con el nombre indicado en la entidad.
     *
     * @param methodName nombre del método.
     * @return verdadero si existe, falso si no.
     */
    public boolean      isMethodExist(String methodName);

    /**
     * Indica si un campo es una clave foránea.
     *
     * @param fieldname nombre del campo.
     * @return verdadero si es clave foránea, falso si no.
     */
    public boolean      isForeingKey(String fieldname);

    /**
     * Indica si el cursor está abierto.
     *
     * @return verdadero si está abierto, falso si no.
     */
    public boolean      isOpen();

    /**
     * Indica si la operación indicada está permitida sobre el registro actual.
     *
     * @param action código de operación ({@link IDataRow#INSERT},
     * {@link IDataRow#UPDATE}, {@link IDataRow#DELETE}...).
     * @return verdadero si está permitida, falso si no.
     */
    public boolean      allowAction(int action);

    /**
     * Devuelve el valor de la clave primaria del registro actual.
     *
     * @return valor de la clave primaria.
     */
    public Object       getPrimaryKeyValue();

    /**
     * Asigna el valor de la clave primaria del registro actual.
     *
     * @param value valor de la clave primaria.
     * @return verdadero si tuvo éxito, falso si no.
     */
    public boolean      setPrimaryKeyValue(Object value);

    /**
     * Refresca el registro actual desde la base de datos.
     *
     * @return verdadero si tuvo éxito, falso si no.
     */
    public boolean      refreshRow();

    /**
     * Inserta un nuevo registro en el cursor y lo posiciona como actual.
     *
     * @return verdadero si tuvo éxito, falso si no.
     */
    public boolean      insertRow();

    /**
     * Inserta un nuevo registro copiando los valores del registro actual.
     *
     * @return verdadero si tuvo éxito, falso si no.
     */
    public boolean      insertRowFrom();

    /**
     * Marca el registro actual para ser borrado.
     *
     * @return verdadero si tuvo éxito, falso si no.
     */
    public boolean      deleteRow();

    /**
     * Copia registros desde otra empresa/tabla hacia el contexto actual.
     *
     * @param idcompany identificador de la empresa origen.
     * @param companyName nombre de la empresa origen.
     * @param xmlTag etiqueta XML de configuración de la copia.
     * @param tableCopy tabla a copiar.
     */
    public void         copyFrom(String idcompany, String companyName, String xmlTag, String tableCopy);

    /**
     * Valida el registro actual y devuelve los errores por campo.
     *
     * @return mapa campo → error (vacío si es válido).
     * @throws Exception si ocurre un error durante la validación.
     */
    public Map<String, IErrorReg> checkDataRow() throws Exception;

    /**
     * Valida el registro indicado y devuelve los errores por campo.
     *
     * @param row registro a validar.
     * @return mapa campo → error (vacío si es válido).
     * @throws Exception si ocurre un error durante la validación.
     */
    public Map<String, IErrorReg> checkDataRow(T row) throws Exception;

    /**
     * Valida los registros del cursor.
     *
     * @param allRows verdadero para validar todos los registros, falso solo el actual.
     * @return verdadero si la validación es correcta, falso si hay errores.
     */
    public boolean      checkData(boolean allRows);

    /**
     * Confirma (persiste) los cambios del cursor en la base de datos.
     *
     * @param allRows verdadero para grabar todos los registros, falso solo el actual.
     * @return verdadero si tuvo éxito, falso si no.
     */
    public boolean      update(boolean allRows);

    /**
     * Revierte los cambios no confirmados del cursor.
     *
     * @param allRows verdadero para revertir todos los registros, falso solo el actual.
     * @return verdadero si tuvo éxito, falso si no.
     */
    public boolean      revert(Boolean allRows);

    /**
     * Confirma (persiste) los cambios de un conjunto de datos.
     *
     * @param dataSet conjunto de datos a grabar.
     * @return verdadero si tuvo éxito, falso si no.
     */
    public boolean      update(IDataSet dataSet);

    /**
     * Revierte los cambios de un conjunto de datos.
     *
     * @param dataSet conjunto de datos a revertir.
     * @return verdadero si tuvo éxito, falso si no.
     */
    public boolean      revert(IDataSet dataSet);

    /**
     * Revierte los cambios no confirmados del registro actual.
     *
     * @return verdadero si tuvo éxito, falso si no.
     */
    public boolean      revert();

    /**
     * Cierra el cursor y libera sus recursos.
     */
    public void         close();
}

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

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.security.IUserSession;

/**
 * Contiene metodos para gestionar la capa de datos, (lectura y grabación de
 * datos), utiliza DBManager.
 *
 * @author Jorge Enciso
 */
public interface IGenericDAO {

    /**
     * Devuelve un entity manager se crea un entity manager por cada thread o 
     * sesionid y unidad de persistencia.
     *
     * @param keyId
     * @return un entity manager
     */
    public EntityManager getEntityManager(String keyId);

    /**
     * Sincroniza un ejb con la base de datos.
     *
     * @param <T>
     * @param persistentUnit unidad de persistencia
     * @param ejb objeto mapeado a un registro de una tabla.
     * @param sessionId identificador de sesion del usuario
     * @return Devuelve un objeto con el resultado de la grabación
     *
     */
    public <T extends IDataRow> IDataResult update(String persistentUnit, T ejb, String sessionId);

    /**
     * Sincroniza una lista de ejbs con la base de datos.
     *
     * @param <T>
     * @param ejbs lista de objetos mapeados a los registros de una tabla.
     * @param persistentUnit unidad de persistencia.
     * @param sessionId identificador de sesion del usuario
     * @return Devuelve un objeto con el resultado de la grabación
     */
    public <T extends IDataRow> IDataResult update(String persistentUnit, IDataObject ejbs, String sessionId);

    /**
     * Sincroniza una lista de ejbs con la base de datos.
     *
     * @param <T>
     * @param ejbs lista de objetos mapeados a los registros de una tabla.
     * @param persistentUnit unidad de persistencia.
     * @param sessionId identificador de sesion del usuario
     * @return Devuelve un objeto con el resultado de la grabación
     */
    public <T extends IDataRow> IDataResult update(String persistentUnit, List<T> ejbs, String sessionId);

    /**
     * Sincroniza una lista de ejbs con la base de datos.
     *
     * @param <T>
     * @param dataSet cada elemento es un objeto mapeado a una tabla
     * @param persistentUnit unidad de persistencia.
     * @param sessionId identificador de sesion del usuario
     * @return Devuelve un objeto con el resultado de la grabación
     */
    public <T extends IDataRow> IDataResult update(String persistentUnit, IDataSet dataSet, String sessionId);

    /**
     * Agregar,un registro en la tabla
     *
     * @param <T> tipo de dato generalemente hereda de DataRow
     * @param persistentUnit unidad de persistencia
     * @param ejb el objeto con los valores del registro
     * @param sessionId id de la sesion del usuario
     * @return
     */
    public <T extends IDataRow> IDataResult persist(String persistentUnit, T ejb, String sessionId);

    /**
     * Modificar un registro en la tabla dada
     *
     * @param <T> tipo de dato generalemente hereda de DataRow
     * @param persistentUnit unidad de persistencia
     * @param ejb el objeto con los valores del registro
     * @param sessionId id de sesión del usuario
     * @return
     */
    public <T extends IDataRow> IDataResult merge(String persistentUnit, T ejb, String sessionId);

    /**
     * Borra un registro en la tabla dada
     *
     * @param <T> tipo de dato generalemente hereda de DataRow
     * @param persistentUnit unidad de persistencia
     * @param ejb el objeto con los valores del registro
     * @param sessionId id de la sesión del usuario
     * @return
     */
    public <T extends IDataRow> IDataResult remove(String persistentUnit, T ejb, String sessionId);

    /**
     * Recupera todos los registros de una tabla
     *
     * @param <T>
     * @param entityClass clase mapeada a la tabla
     * @param persistentUnit unidad de persistencia
     * @return una list con los registros de una tabla
     * @throws Exception
     */
    public <T extends Object> List<T> findAll(Class<T> entityClass, String persistentUnit) throws Exception;

    /**
     * Devuelve un registro de una tabla dada
     *
     * @param <T>
     * @param entityClass clase mapeada a la tabla
     * @param persistentUnit unidad de persistencia
     * @param id identificador del registro
     * @return un registro solicitado
     * @throws Exception
     */
    public <T extends IDataRow> T find(Class<T> entityClass, String persistentUnit, Object id) throws Exception;

    /**
     * Devuelve un registro a travéz de su clave unica.
     *
     * @param <T>
     * @param entityClass clase mapeada a una tabla
     * @param persistentUnit unidad de persistencia
     * @param ejb objeto ejb con los datos de la clave unica
     * @return
     * @throws Exception
     */
    public <T extends IDataRow> T findByUk(Class<T> entityClass, String persistentUnit, T ejb) throws Exception;

    /**
     * Devuelve un registro a travéz de su clave unica.
     *
     * @param <T>
     * @param entityClass clase mapeada a una tabla
     * @param persistentUnit unidad de persistencia
     * @param queryString sentencia jpql
     * @param parameters parametros de la sentencia
     * @return un objeto con valores del registro de la tabla solicitada
     * @throws Exception
     */
    public <T extends IDataRow> T findByQuery(Class<T> entityClass, String persistentUnit, String queryString, Map<String, Object> parameters) throws Exception;

    /**
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
     *
     * @param <T>
     * @param persistentUnit unidad de persistencia
     * @param queryString sentencia jpql
     * @param parameters parametros de la sentencia
     * @return una lista de objetos conteniendo los registros de la tabla
     * solicitada
     * @throws Exception
     */
    public <T extends IDataRow> List<T> findListByQuery(String persistentUnit, String queryString, Map<String, Object> parameters) throws Exception;

    /**
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
     *
     * @param <T>
     * @param persistentUnit unidad de persistencia
     * @param queryString sentencia jpql
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos conteniendo los registros de la tabla
     * solicitada
     * @throws Exception
     */
    public <T extends IDataRow> List<T> findListByQuery(String persistentUnit, String queryString, int first, int max) throws Exception;

    /**
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
     *
     * @param <T>
     * @param persistentUnit unidad de persistencia
     * @param queryString sentencia jpql
     * @param parameters parametros de la sentencia
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos conteniendo los registros de la tabla
     * solicitada
     * @throws Exception
     */
    public <T extends IDataRow> List<T> findListByQuery(String persistentUnit, String queryString, Map<String, Object> parameters, int first, int max) throws Exception;

    /**
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
     *
     * @param <T>
     * @param persistentUnit unidad de persistencia
     * @param namedQuery namedQuery
     * @param parameters parámetros de la sentencia.
     * @return un objeto con los datos del registro de la tabla solicitada
     * @throws Exception
     */
    public <T extends IDataRow> T findByNamedQuery(String persistentUnit, String namedQuery, Map<String, Object> parameters) throws Exception;

    /**
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
     *
     * @param <T>
     * @param persistentUnit unidad de persistencia
     * @param namedQuery namedQuery
     * @param parameters parámetros de la sentencia.
     * @return una lista de objetos con los datos de los registros de la tabla
     * solicitada
     * @throws Exception
     */
    public <T extends IDataRow> List<T> findListByNamedQuery(String persistentUnit, String namedQuery, Map<String, Object> parameters) throws Exception;

    /**
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
     *
     * @param <T>
     * @param persistentUnit unidad de persistencia
     * @param namedQuery namedQuery
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos con los datos de los registros de la tabla
     * solicitada
     * @throws Exception
     */
    public <T extends IDataRow> List<T> findListByNamedQuery(String persistentUnit, String namedQuery, int first, int max) throws Exception;

    /**
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
     *
     * @param <T>
     * @param persistentUnit unidad de persistencia
     * @param namedQuery namedQuery
     * @param parameters parámetros de la sentencia.
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos con los registros de la tabla solicitada
     * @throws Exception
     */
    public <T extends IDataRow> List<T> findListByNamedQuery(String persistentUnit, String namedQuery, Map<String, Object> parameters, int first, int max) throws Exception;

    /**
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
     *
     * @param persistentUnit unidad de persistencia
     * @param queryString sentencia sql
     * @param parameters parámetros de la sentencia.
     * @return una lista de objetos con datos de los registros solicitados
     * @throws Exception
     */
    public List<Object> findByNativeQuery(String persistentUnit, String queryString, Map<String, Object> parameters) throws Exception;

    /**
     * Selecciona datos de la base de datos a travez de una instrucción nativa
     *
     * @param persistentUnit unidad de persistencia
     * @param queryString sentencia sql
     * @param parameters parámetros de la sentencia.
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos con datos de los registros solicitados
     * @throws Exception
     */
    public List<Object> findByNativeQuery(String persistentUnit, String queryString, Map<String, Object> parameters, int first, int max) throws Exception;

    /**
     * Ejecuta una sentencia (select, insert, update, remove) sobre la base de datos
     *
     * @param persistentUnit unidad de persistencia
     * @param queryString sentencia sql
     * @param parameters parámetros de la sentencia.
     * @return un objeto error si no se ejecuto la sentencia con exito
     */
    public IErrorReg sqlExec(String persistentUnit, String queryString, Map<String, Object> parameters);

    /**
     * Selecciona datos de la base de datos y los convierte en una lista de
     * objetos.
     *
     * @param <T>
     * @param persistentUnit unidad de persistencia
     * @param queryString sentencia jpql para seleccionar datos
     * @param maxRows maxima cantidad de registros que debe devolver
     * @param noCache
     * @return lista de objetos con los datos solicitados de la base de datos
     * @throws Exception
     */
    public <T extends IDataRow> List<T> getData(String persistentUnit, String queryString, int maxRows, boolean noCache) throws Exception;

    /**
     * Selecciona datos de la base de datos y los convierte en una lista de
     * objetos.
     *
     * @param <T>
     * @param query objeto query conteniendo lo necesario para recuperar los
     * datos.
     * @return
     * @throws Exception
     */
    public <T extends IDataRow> List<T> getData(Query query) throws Exception;

    /**
     * Refresca desde la base de datos los valores de un objeto.
     *
     * @param <T>
     * @param persistUnit unidad de persistencia
     * @param row objeto o registro a refrescar
     * @return objeto con los datos refrescados de la base de datos
     * @throws Exception
     */
    public <T extends IDataRow> T refreshRow(String persistUnit, T row) throws Exception;

    /**
     * Refresca desde la base de datos una lista de objetos.
     *
     * @param <T>
     * @param persistUnit unidad de persistencia
     * @param rows objetos a refrescar
     * @return lista de objetos con los datos refrescados de la base de datos
     * @throws Exception
     */
    public <T extends IDataRow> List<T> refreshAll(String persistUnit, List<T> rows) throws Exception;

    /**
     * Devuelve la cantidad de registros que debería retornar la sentencia.
     *
     * @param persistentUnit unidad de persistencia
     * @param queryString sentencia jpql
     * @param parameters parámetros de la sentencia
     * @return cantidad de registros que debería devolver la sentencia.
     * @throws Exception
     */
    public Long getCount(String persistentUnit, String queryString, Map<String, Object> parameters) throws Exception;

    /**
     * Devuelve la cantidad de registros que debería retornar la sentencia.
     *
     * @param persistentUnit unidad de persistencia
     * @param queryString sentencia sql
     * @param parameters parámetros de la sentencia
     * @return cantidad de registros que debería devolver la sentencia.
     * @throws Exception
     */
    public Long getCount2(String persistentUnit, String queryString, Map<String, Object> parameters) throws Exception;

    /**
     * Busca y devuelve el valor de una propiedad solicitada del entity manager
     *
     * @param persistUnit unidad de persistencia
     * @return el valor de una propiedad del entity manager
     */
    public Map<String, Object> getEntityManagerProp(String persistUnit);

    /**
     * Busca y devuelve el valor de una propiedad solicitada
     *
     * @param persistUnit unidad de persistencia.
     * @return devuelve un map con todas las propiedades de la unidad de
     * persistencia solicitada
     */
    public Map<String, Object> getPersistUnitProp(String persistUnit);

    /**
     * Devuelve el nombre del motor de la base de datos.
     *
     * @param persistentUnit unidad de persistencia
     * @return devuelve el nombre de la base de datos.
     */
    public String getDataEngine(String persistentUnit);

    /**
     * Devuelve el schema de datos
     *
     * @param persistentUnit unidad de persistencia
     * @return devuelve el schema
     */
    public String getSchema(String persistentUnit);

    /**
     * Devuelve una conexión a la base solo funciona en un ambiente @local no
     * funciona en @Remote
     *
     * @param persistUnit unidad de persistencia
     * @return una conexión a la base solo funciona en un ambiente @local no
     * funciona en @Remote
     */
    public Connection getConnection(String persistUnit);

    /**
     * Devuelve una conexión del entity manager, solo funciona en un ambiente
     *
     * @local no funciona en @Remote
     *
     * @param persistUnit unidad de persistencia
     * @param conn objeto factory cuya función es devolver una conexión del
     * entity manager
     * @return devuelve una conexión del entity manager
     */
    public Connection getConnection(String persistUnit, IDBConnectFactory conn);

    /**
     * Devuelve el objeto userSession con la información de la sesión
     *
     * @param sessionId id de sesión del usuario
     * @return el objeto userSession con la información de la sesión
     */
    public IUserSession getUserSession(String sessionId);
}

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

import org.javabeanstack.datactrl.IDataObject;
import java.io.Serializable;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import javax.persistence.Query;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.security.IOAuthConsumerData;
import org.javabeanstack.security.model.IUserSession;


/**
 * Contiene metodos para gestionar la capa de datos, (lectura y grabación de
 * datos), utiliza DBManager.
 *
 * @author Jorge Enciso
 */
public interface IGenericDAO extends Serializable {

    /**
     * Devuelve el objeto DBLinkInfo(info de la conexión) a partir del sessionid o token
     * @param sessionId identificador de la sesión o token 
     * @return objeto DBLinkInfo(info de la conexión) a partir del sessionid o token
     */
    public IDBLinkInfo getDBLinkInfo(String sessionId);
    /**
     * Sincroniza un ejb con la base de datos.
     *
     * @param <T>
     * @param sessionId identificador de la sesión del usuario
     * @param ejb objeto mapeado a un registro de una tabla.
     * @return Devuelve un objeto con el resultado de la grabación
     *
     */
    public <T extends IDataRow> IDataResult update(String sessionId, T ejb);

    /**
     * Sincroniza una lista de ejbs con la base de datos.
     *
     * @param ejbs lista de objetos mapeados a los registros de una tabla.
     * @param sessionId identificador de la sesión del usuario
     * @return Devuelve un objeto con el resultado de la grabación
     */
    public IDataResult update(String sessionId, IDataObject ejbs);

    /**
     * Sincroniza una lista de ejbs con la base de datos.
     *
     * @param <T>
     * @param ejbs lista de objetos mapeados a los registros de una tabla.
     * @param sessionId identificador de la sesión del usuario 
     * @return Devuelve un objeto con el resultado de la grabación
     */
    public <T extends IDataRow> IDataResult update(String sessionId, List<T> ejbs);

    /**
     * Sincroniza una lista de ejbs con la base de datos.
     *
     * @param dataSet cada elemento es un objeto mapeado a una tabla
     * @param sessionId identificador de la sesión del usuario
     * correcta (unidad de persistencia, sesión id etc).
     * @return Devuelve un objeto con el resultado de la grabación
     */
    public IDataResult update(String sessionId, IDataSet dataSet);

    /**
     * Agrega un registro en la tabla
     *
     * @param <T> tipo de dato generalemente hereda de DataRow
     * @param sessionId identificador de la sesión del usuario
     * @param ejb el objeto con los valores del registro
     * @return  IDataResult conteniendo los registros actualizados.
     * @throws java.lang.Exception
     */
    public <T extends IDataRow> IDataResult persist(String sessionId, T ejb) throws Exception;

    /**
     * Modifica un registro en la tabla dada
     *
     * @param <T> tipo de dato generalemente hereda de DataRow
     * @param sessionId identificador de la sesión del usuario
     * @param ejb el objeto con los valores del registro
     * @return IDataResult conteniendo los registros actualizados.
     * @throws java.lang.Exception
     */
    public <T extends IDataRow> IDataResult merge(String sessionId, T ejb) throws Exception;

    /**
     * Borra un registro en la tabla dada
     *
     * @param <T> tipo de dato generalemente hereda de DataRow
     * @param sessionId identificador de la sesión del usuario
     * @param ejb el objeto con los valores del registro
     * @return IDataResult conteniendo los registros eliminados.
     * @throws java.lang.Exception
     */
    public <T extends IDataRow> IDataResult remove(String sessionId, T ejb) throws Exception;

    /**
     * Recupera todos los registros de una tabla
     *
     * @param <T>
     * @param entityClass clase mapeada a la tabla
     * @param sessionId identificador de la sesión del usuario
     * @return una list con los registros de una tabla
     * @throws Exception
     */
    public <T extends Object> List<T> findAll(Class<T> entityClass, String sessionId) throws Exception;

    /**
     * Devuelve un registro de una tabla dada
     *
     * @param <T>
     * @param entityClass clase mapeada a la tabla
     * @param sessionId identificador de la sesión del usuario
     * @param id identificador del registro
     * @return un registro solicitado
     * @throws Exception
     */
    public <T extends IDataRow> T findById(Class<T> entityClass, String sessionId, Object id) throws Exception;

    /**
     * Devuelve una lista de registros de una tabla dada
     *
     * @param <T>
     * @param entityClass clase mapeada a la tabla
     * @param sessionId identificador de la sesión del usuario
     * @return lista de objetos
     * @throws Exception
     */
    public <T extends IDataRow> List<T> find(Class<T> entityClass, String sessionId) throws Exception;    
    
    /**
     * Devuelve un registro de una tabla dada
     *
     * @param <T>
     * @param entityClass clase mapeada a la tabla
     * @param sessionId identificador de la sesión del usuario
     * @param order
     * @param filter
     * @param params
     * @return lista de objetos
     * @throws Exception
     */
    public <T extends IDataRow> List<T> find(Class<T> entityClass, String sessionId, String order, String filter, Map<String, Object> params) throws Exception;

    /**
     * Devuelve una lista de registros de una tabla dada
     *
     * @param <T>
     * @param entityClass clase mapeada a la tabla
     * @param sessionId identificador de la sesión del usuario
     * @param order
     * @param filter
     * @param params
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return lista de objetos
     * @throws Exception
     */
    public <T extends IDataRow> List<T> find(Class<T> entityClass, String sessionId, String order, String filter, Map<String, Object> params, int first, int max) throws Exception;
    
    
    /**
     * Devuelve un registro a travéz de su clave unica.
     *
     * @param <T>
     * @param sessionId identificador de la sesión del usuario
     * @param ejb objeto ejb con los datos de la clave unica
     * @return registro que cumple con las condiciones de busqueda.
     * @throws Exception
     */
    public <T extends IDataRow> T findByUk(String sessionId, T ejb) throws Exception;

    /**
     * Devuelve un registro a travéz de su clave unica.
     *
     * @param <T>
     * @param sessionId identificador de la sesión del usuario
     * @param queryString sentencia jpql
     * @param parameters parametros de la sentencia
     * @return un objeto con valores del registro de la tabla solicitada
     * @throws Exception
     */
    public <T extends IDataRow> T findByQuery(String sessionId, String queryString, Map<String, Object> parameters) throws Exception;

    /**
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
     *
     * @param <T>
     * @param sessionId identificador de la sesión del usuario
     * @param queryString sentencia jpql
     * @param parameters parametros de la sentencia
     * @return una lista de objetos conteniendo los registros de la tabla
     * solicitada
     * @throws Exception
     */
    public <T extends IDataRow> List<T> findListByQuery(String sessionId, String queryString, Map<String, Object> parameters) throws Exception;

    /**
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
     *
     * @param <T>
     * @param sessionId identificador de la sesión del usuario 
     * @param queryString sentencia jpql
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos conteniendo los registros de la tabla
     * solicitada
     * @throws Exception
     */
    public <T extends IDataRow> List<T> findListByQuery(String sessionId, String queryString, int first, int max) throws Exception;

    /**
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
     *
     * @param <T>
     * @param sessionId identificador de la sesión del usuario 
     * @param queryString sentencia jpql
     * @param parameters parametros de la sentencia
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos conteniendo los registros de la tabla
     * solicitada
     * @throws Exception
     */
    public <T extends IDataRow> List<T> findListByQuery(String sessionId, String queryString, Map<String, Object> parameters, int first, int max) throws Exception;

    /**
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
     *
     * @param <T>
     * @param sessionId identificador de la sesión del usuario 
     * @param namedQuery namedQuery
     * @param parameters parámetros de la sentencia.
     * @return un objeto con los datos del registro de la tabla solicitada
     * @throws Exception
     */
    public <T extends IDataRow> T findByNamedQuery(String sessionId, String namedQuery, Map<String, Object> parameters) throws Exception;

    /**
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
     *
     * @param <T>
     * @param sessionId identificador de la sesión del usuario 
     * @param namedQuery namedQuery
     * @param parameters parámetros de la sentencia.
     * @return una lista de objetos con los datos de los registros de la tabla
     * solicitada
     * @throws Exception
     */
    public <T extends IDataRow> List<T> findListByNamedQuery(String sessionId, String namedQuery, Map<String, Object> parameters) throws Exception;

    /**
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
     *
     * @param <T>
     * @param sessionId identificador de la sesión del usuario 
     * @param namedQuery namedQuery
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos con los datos de los registros de la tabla
     * solicitada
     * @throws Exception
     */
    public <T extends IDataRow> List<T> findListByNamedQuery(String sessionId, String namedQuery, int first, int max) throws Exception;

    /**
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
     *
     * @param <T>
     * @param sessionId identificador de la sesión del usuario 
     * @param namedQuery namedQuery
     * @param parameters parámetros de la sentencia.
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos con los registros de la tabla solicitada
     * @throws Exception
     */
    public <T extends IDataRow> List<T> findListByNamedQuery(String sessionId, String namedQuery, Map<String, Object> parameters, int first, int max) throws Exception;

    /**
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
     * 
     * @param sessionId identificador de la sesión del usuario 
     * @param queryString sentencia sql
     * @param parameters parámetros de la sentencia.
     * @return una lista de objetos con datos de los registros solicitados
     * @throws Exception
     */
    public List<Object> findByNativeQuery(String sessionId, String queryString, Map<String, Object> parameters) throws Exception;

    /**
     * Selecciona datos de la base de datos a travez de una instrucción sql nativa
     *
     * @param sessionId identificador de la sesión del usuario 
     * @param queryString sentencia sql
     * @param parameters parámetros de la sentencia.
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos con datos de los registros solicitados
     * @throws Exception
     */
    public List<Object> findByNativeQuery(String sessionId, String queryString, Map<String, Object> parameters, int first, int max) throws Exception;
    /**
     * Selecciona datos de la base de datos a travez de una instrucción sql nativa
     *
     * @param <T>
     * @param clazz
     * @param sessionId identificador de la sesión del usuario 
     * @param queryString sentencia sql
     * @param parameters parámetros de la sentencia.
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos con datos de los registros solicitados
     * @throws Exception
     */
    public <T extends IDataRow> List<T> findByNativeQuery(Class<T> clazz, String sessionId, String queryString, Map<String, Object> parameters, int first, int max) throws Exception;

    /**
     * Ejecuta una sentencia (select, insert, update, remove) sobre la base de
     * datos
     *
     * @param sessionId identificador de la sesión del usuario 
     * @param sqlCommand sentencia sql
     * @param parameters parámetros de la sentencia.
     * @return un objeto error si no se ejecuto la sentencia con exito
     * @throws java.lang.Exception
     */
    public IErrorReg sqlExec(String sessionId, String sqlCommand, Map<String, Object> parameters) throws Exception;
    /**
     * Ejecuta una sentencia (select, insert, update, remove) sobre la base de
     * datos
     *
     * @param sessionId identificador de la sesión del usuario 
     * @param jpaCommand sentencia jpa
     * @param parameters parámetros de la sentencia.
     * @return un objeto error si no se ejecuto la sentencia con exito
     * @throws java.lang.Exception
     */
    public IErrorReg jpaExec(String sessionId, String jpaCommand, Map<String, Object> parameters) throws Exception;

    /**
     * Selecciona datos de la base de datos y los convierte en una lista de
     * objetos.
     *
     * @param <T>
     * @param sessionId identificador de la sesión del usuario 
     * @param queryString sentencia jpql para seleccionar datos
     * @param maxRows maxima cantidad de registros que debe devolver
     * @param noCache
     * @return lista de objetos con los datos solicitados de la base de datos
     * @throws Exception
     */
    public <T extends IDataRow> List<T> getData(String sessionId, String queryString, int maxRows, boolean noCache) throws Exception;

    /**
     * Selecciona datos de la base de datos y los convierte en una lista de
     * objetos.
     *
     * @param <T>
     * @param query objeto query conteniendo lo necesario para recuperar los
     * datos.
     * @return lista de registros según parámetros enviados.
     * @throws Exception
     */
    public <T extends IDataRow> List<T> getData(Query query) throws Exception;

    /**
     * Refresca desde la base de datos los valores de un objeto.
     *
     * @param <T>
     * @param sessionId identificador de la sesión del usuario 
     * @param row objeto o registro a refrescar
     * @return objeto con los datos refrescados de la base de datos
     * @throws Exception
     */
    public <T extends IDataRow> T refreshRow(String sessionId, T row) throws Exception;

    /**
     * Refresca desde la base de datos una lista de objetos.
     *
     * @param <T>
     * @param sessionId identificador de la sesión del usuario 
     * @param rows objetos a refrescar
     * @return lista de objetos con los datos refrescados de la base de datos
     * @throws Exception
     */
    public <T extends IDataRow> List<T> refreshAll(String sessionId, List<T> rows) throws Exception;

    /**
     * Devuelve la cantidad de registros que debería retornar la sentencia.
     *
     * @param sessionId identificador de la sesión del usuario 
     * @param queryString sentencia jpql
     * @param parameters parámetros de la sentencia
     * @return cantidad de registros que debería devolver la sentencia.
     * @throws Exception
     */
    public Long getCount(String sessionId, String queryString, Map<String, Object> parameters) throws Exception;

    /**
     * Devuelve la cantidad de registros que debería retornar la sentencia.
     *
     * @param sessionId identificador de la sesión del usuario 
     * @param queryString sentencia sql
     * @param parameters parámetros de la sentencia
     * @return cantidad de registros que debería devolver la sentencia.
     * @throws Exception
     */
    public Long getCount2(String sessionId, String queryString, Map<String, Object> parameters) throws Exception;

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
     * @param sessionId identificador de la sesión del usuario 
     * @return una conexión a la base solo funciona en un ambiente @local no
     * funciona en @Remote
     */
    public Connection getConnection(String sessionId);

    /**
     * Devuelve una conexión del entity manager, solo funciona en un ambiente
     * local no funciona en @Remote
     *
     * @param sessionId identificador de la sesión del usuario 
     * @param conn objeto factory cuya función es devolver una conexión del
     * entity manager
     * @return devuelve una conexión del entity manager
     */
    public Connection getConnection(String sessionId, IDBConnectFactory conn);

    /**
     * Devuelve el objeto userSession con la información de la sesión
     *
     * @param sessionId id de sesión del usuario
     * @return el objeto userSession con la información de la sesión
     */
    public IUserSession getUserSession(String sessionId);
    /**
     * Verifica los datos (usuario, contraseña y empresa a la que se quiere loguear)
     * Si pasa la válidación se puede crear el token.
     * @param data datos conteniendo valores de usuario, pass y empresa
     * @throws Exception 
     */
    public void checkAuthConsumerData(IOAuthConsumerData data) throws Exception;    
    /**
     * Verifica si la combinación iduser, idcompany es válido para una sesión
     * @param iduser   identificador del usuario
     * @param idcompany  identificador de la empresa
     * @return verdadero si cumple y falso si no
     */
    public boolean isCredentialValid(Long iduser, Long idcompany);
    
    public Object getSessionInfo(String sessionId, String key);
    public void addSessionInfo(String sessionId, String key, Object info);
    public void removeSessionInfo(String sessionId, String key);  
    public IErrorReg getErrorMessage(int messageNumber,String alternativeMsg, String fieldName);
    
}

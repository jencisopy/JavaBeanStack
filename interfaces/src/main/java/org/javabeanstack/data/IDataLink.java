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


import java.util.List;
import java.util.Map;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.data.services.IDataService;

/**
 * Contrato de la conexión de datos: fachada de acceso a la base sobre el DAO
 * genérico ({@link IGenericDAO}) que fija el contexto de la operación (unidad de
 * persistencia, esquema, empresa/período y sesión de usuario).
 *
 * <p>Expone las operaciones de búsqueda (por id, clave única, JPQL, named query
 * y SQL nativo), de persistencia (persist/merge/remove/update) y la creación de
 * consultas nativas ({@link IDataNativeQuery}). La implementación de referencia
 * es {@code org.javabeanstack.data.AbstractDataLink}.</p>
 *
 * @author Jorge Enciso
 */

public interface IDataLink  {
    /**
     * Devuelve el esquema de base de datos activo.
     *
     * @return nombre del esquema.
     */
    String getSchema();

    /**
     * Devuelve el DAO genérico subyacente.
     *
     * @return DAO genérico.
     */
    IGenericDAO getDao();

    /**
     * Devuelve el servicio de datos asociado.
     *
     * @param <T> tipo del servicio de datos.
     * @return servicio de datos.
     */
    <T extends IDataService> T getDataService();

    /**
     * Devuelve el nombre de la unidad de persistencia activa.
     *
     * @return nombre de la unidad de persistencia.
     */
    String getPersistUnit();

    /**
     * Devuelve el identificador de la empresa activa.
     *
     * @return identificador de la empresa.
     */
    Long getIdCompany();

    /**
     * Devuelve el identificador del período activo.
     *
     * @return identificador del período.
     */
    Long getIdperiodo();

    /**
     * Devuelve la sesión de usuario del contexto.
     *
     * @return sesión de usuario.
     */
    IUserSession getUserSession();

    /**
     * Devuelve las propiedades del {@code EntityManager} activo.
     *
     * @return mapa de propiedades del entity manager.
     */
    Map<String, Object> getEntityManagerProp();

    /**
     * Devuelve las propiedades de la unidad de persistencia.
     *
     * @return mapa de propiedades de la unidad de persistencia.
     */
    Map<String, Object> getPersistUnitProp();

    /**
     * Crea un nuevo constructor de consultas nativas ligado a esta conexión.
     *
     * @return constructor de consultas nativas.
     */
    IDataNativeQuery newDataNativeQuery();

    /**
     * Busca una entidad por su identificador.
     *
     * @param <T> tipo de la entidad.
     * @param entityClass clase de la entidad.
     * @param id identificador.
     * @return entidad encontrada, o {@code null} si no existe.
     * @throws Exception si ocurre un error de acceso a datos.
     */
    <T extends IDataRow> T find(Class<T> entityClass, Object id) throws Exception;

    /**
     * Busca una entidad por su identificador (clave primaria).
     *
     * @param <T> tipo de la entidad.
     * @param entityClass clase de la entidad.
     * @param id identificador.
     * @return entidad encontrada, o {@code null} si no existe.
     * @throws Exception si ocurre un error de acceso a datos.
     */
    <T extends IDataRow> T findById(Class<T> entityClass, Object id) throws Exception;

    /**
     * Busca una entidad por su clave única.
     *
     * @param <T> tipo de la entidad.
     * @param ejb entidad con los valores de la clave única.
     * @return entidad encontrada, o {@code null} si no existe.
     * @throws Exception si ocurre un error de acceso a datos.
     */
    <T extends IDataRow> T findByUk(T ejb) throws Exception;

    /**
     * Ejecuta una consulta SQL nativa y devuelve las filas como objetos.
     *
     * @param queryString sentencia SQL nativa.
     * @param parameters parámetros nombrados de la consulta.
     * @return lista de resultados.
     * @throws Exception si ocurre un error de acceso a datos.
     */
    List<Object> findByNativeQuery(String queryString, Map<String, Object> parameters) throws Exception;

    /**
     * Ejecuta una consulta SQL nativa con paginación.
     *
     * @param queryString sentencia SQL nativa.
     * @param parameters parámetros nombrados de la consulta.
     * @param first índice del primer resultado.
     * @param max cantidad máxima de resultados.
     * @return lista de resultados.
     * @throws Exception si ocurre un error de acceso a datos.
     */
    List<Object> findByNativeQuery(String queryString, Map<String, Object> parameters, int first, int max) throws Exception;

    /**
     * Ejecuta una named query y devuelve el primer registro.
     *
     * @param <T> tipo de la entidad.
     * @param namedQuery nombre de la named query.
     * @param parameters parámetros nombrados.
     * @return primer registro, o {@code null} si no hay resultados.
     * @throws Exception si ocurre un error de acceso a datos.
     */
    <T extends IDataRow> T findByNamedQuery(String namedQuery, Map<String, Object> parameters) throws Exception;

    /**
     * Ejecuta una consulta JPQL y devuelve el primer registro.
     *
     * @param <T> tipo de la entidad.
     * @param queryString sentencia JPQL.
     * @param parameters parámetros nombrados.
     * @return primer registro, o {@code null} si no hay resultados.
     * @throws Exception si ocurre un error de acceso a datos.
     */
    <T extends IDataRow> T findByQuery(String queryString, Map<String, Object> parameters) throws Exception;

    /**
     * Ejecuta una named query y devuelve la lista de registros.
     *
     * @param <T> tipo de la entidad.
     * @param namedQuery nombre de la named query.
     * @param parameters parámetros nombrados.
     * @return lista de registros.
     * @throws Exception si ocurre un error de acceso a datos.
     */
    <T extends IDataRow> List<T> findListByNamedQuery(String namedQuery, Map<String, Object> parameters) throws Exception;

    /**
     * Ejecuta una named query con paginación y devuelve la lista de registros.
     *
     * @param <T> tipo de la entidad.
     * @param namedQuery nombre de la named query.
     * @param parameters parámetros nombrados.
     * @param first índice del primer resultado.
     * @param max cantidad máxima de resultados.
     * @return lista de registros.
     * @throws Exception si ocurre un error de acceso a datos.
     */
    <T extends IDataRow> List<T> findListByNamedQuery(String namedQuery, Map<String, Object> parameters, int first, int max) throws Exception;

    /**
     * Ejecuta una consulta JPQL y devuelve la lista de registros.
     *
     * @param <T> tipo de la entidad.
     * @param queryString sentencia JPQL.
     * @param parameters parámetros nombrados.
     * @return lista de registros.
     * @throws Exception si ocurre un error de acceso a datos.
     */
    <T extends IDataRow> List<T> findListByQuery(String queryString, Map<String, Object> parameters) throws Exception;

    /**
     * Ejecuta una consulta JPQL con paginación y devuelve la lista de registros.
     *
     * @param <T> tipo de la entidad.
     * @param queryString sentencia JPQL.
     * @param parameters parámetros nombrados.
     * @param first índice del primer resultado.
     * @param max cantidad máxima de resultados.
     * @return lista de registros.
     * @throws Exception si ocurre un error de acceso a datos.
     */
    <T extends IDataRow> List<T> findListByQuery(String queryString, Map<String, Object> parameters, int first, int max) throws Exception;

    /**
     * Ejecuta una consulta JPQL con proyección y devuelve las filas como arreglos.
     *
     * @param queryString sentencia JPQL.
     * @param parameters parámetros nombrados.
     * @param first índice del primer resultado.
     * @param max cantidad máxima de resultados.
     * @return lista de arreglos de columnas.
     * @throws Exception si ocurre un error de acceso a datos.
     */
    List<Object[]> findListObjsByQuery(String queryString, Map<String, Object> parameters,int first, int max) throws Exception;

    /**
     * Refresca el estado de una entidad desde la base de datos.
     *
     * @param <T> tipo de la entidad.
     * @param row entidad a refrescar.
     * @return entidad refrescada.
     * @throws Exception si ocurre un error de acceso a datos.
     */
    <T extends IDataRow> T refreshRow(T row) throws Exception;

    /**
     * Graba una entidad (persist o merge según su estado).
     *
     * @param <T> tipo de la entidad.
     * @param ejb entidad a grabar.
     * @return resultado de la operación.
     * @throws Exception si la persistencia falla.
     */
    <T extends IDataRow> IDataResult update(T ejb) throws Exception;

    /**
     * Graba una lista de entidades (persist o merge según su estado).
     *
     * @param <T> tipo de las entidades.
     * @param ejbs lista de entidades a grabar.
     * @return resultado de la operación.
     * @throws Exception si la persistencia falla.
     */
    <T extends IDataRow> IDataResult update(List<T> ejbs) throws Exception;

    /**
     * Graba un conjunto de datos completo.
     *
     * @param dataSet conjunto de datos a grabar.
     * @return resultado de la operación.
     * @throws Exception si la persistencia falla.
     */
    IDataResult update(IDataSet dataSet) throws Exception;

    /**
     * Inserta (persist) una entidad nueva.
     *
     * @param <T> tipo de la entidad.
     * @param ejb entidad a insertar.
     * @return resultado de la operación.
     * @throws Exception si la persistencia falla.
     */
    <T extends IDataRow> IDataResult persist(T ejb) throws Exception;

    /**
     * Inserta (persist) una lista de entidades nuevas.
     *
     * @param <T> tipo de las entidades.
     * @param ejbs lista de entidades a insertar.
     * @return resultado de la operación.
     * @throws Exception si la persistencia falla.
     */
    <T extends IDataRow> IDataResult persist(List<T> ejbs) throws Exception;

    /**
     * Combina (merge) una entidad existente.
     *
     * @param <T> tipo de la entidad.
     * @param ejb entidad a combinar.
     * @return resultado de la operación.
     * @throws Exception si la persistencia falla.
     */
    <T extends IDataRow> IDataResult merge(T ejb) throws Exception;

    /**
     * Combina (merge) una lista de entidades existentes.
     *
     * @param <T> tipo de las entidades.
     * @param ejbs lista de entidades a combinar.
     * @return resultado de la operación.
     * @throws Exception si la persistencia falla.
     */
    <T extends IDataRow> IDataResult merge(List<T> ejbs) throws Exception;

    /**
     * Elimina (remove) una entidad.
     *
     * @param <T> tipo de la entidad.
     * @param ejb entidad a eliminar.
     * @return resultado de la operación.
     * @throws Exception si la eliminación falla.
     */
    <T extends IDataRow> IDataResult remove(T ejb) throws Exception;

    /**
     * Elimina (remove) una lista de entidades.
     *
     * @param <T> tipo de las entidades.
     * @param ejbs lista de entidades a eliminar.
     * @return resultado de la operación.
     * @throws Exception si la eliminación falla.
     */
    <T extends IDataRow> IDataResult remove(List<T> ejbs) throws Exception;

    /**
     * Devuelve la cantidad de registros que satisfacen una consulta.
     *
     * @param queryString sentencia de consulta.
     * @param parameters parámetros nombrados.
     * @return cantidad de registros.
     * @throws Exception si ocurre un error de acceso a datos.
     */
    Long getCount(String queryString, Map<String, Object> parameters) throws Exception;

    /**
     * Devuelve la cantidad de registros de una consulta mediante una estrategia
     * alternativa de conteo.
     *
     * @param queryString sentencia de consulta.
     * @param parameters parámetros nombrados.
     * @return cantidad de registros.
     * @throws Exception si ocurre un error de acceso a datos.
     */
    Long getCount2(String queryString, Map<String, Object> parameters) throws Exception;

    /**
     * Asigna la sesión de usuario del contexto.
     *
     * @param userSession sesión de usuario.
     * @throws Exception si la sesión no es válida.
     */
    void setUserSession(IUserSession userSession) throws Exception;

    /**
     * Asigna el DAO genérico subyacente.
     *
     * @param <T> tipo del DAO.
     * @param dao DAO genérico.
     */
    <T extends IGenericDAO> void setDao(T dao);

    /**
     * Devuelve la definición de relaciones entre entidades según el tipo y el esquema.
     *
     * @param entities entidades involucradas.
     * @param typeRela tipo de relación.
     * @param schema esquema de base de datos.
     * @return descripción de las relaciones.
     * @throws Exception si ocurre un error al resolver las relaciones.
     */
    String getEntitiesRelation(String entities, String typeRela, String schema) throws Exception;

    /**
     * Devuelve el token de acceso del contexto.
     *
     * @return token de acceso.
     */
    String getToken();

    /**
     * Asigna el token de acceso del contexto.
     *
     * @param token token de acceso.
     */
    void setToken(String token);

    /**
     * Devuelve la información de contexto de la conexión de datos.
     *
     * @return información de la conexión.
     */
    IDBLinkInfo getDBLinkInfo();
}

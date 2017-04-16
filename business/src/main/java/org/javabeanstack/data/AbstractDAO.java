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

import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.error.IErrorReg;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaQuery;
import org.apache.log4j.Logger;
import org.javabeanstack.error.ErrorReg;
import org.javabeanstack.security.ISessionsLocal;
import org.javabeanstack.security.IUserSession;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.Strings;

/**
 * Esta clase se encarga de la conexión con la base de datos. Provee metodos que
 * permiten la recuperación, busqueda, refresco, borrado, inserción y
 * actualización de los registros. Esta clase se ejecuta como EJB en la capa
 * de la lógica del negocio.
 *
 * @author Jorge Enciso
 */
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public abstract class AbstractDAO implements IGenericDAO, Serializable {

    private static final Logger LOGGER = Logger.getLogger(AbstractDAO.class);

    /**
     * Es el objeto responsable de la creación y gestión de los entity manager
     */
    @EJB private IDBManagerLocal dbManager;
    /**
     * Es el objeto responsable gestión de las sesiones de usuarios
     */
    @EJB private ISessionsLocal sessions;

    public AbstractDAO() {
    }

    /**
     * Devuelve un entity manager. Se crea un entity manager por cada thread y
     * unidad de persistencia.
     *
     * @param persistUnit unidad de persistencia
     * @param threadId nro de thread.
     * @return un entity manager
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public EntityManager getEntityManager(String persistUnit, long threadId) {
        LOGGER.debug("getEntityManager");
        LOGGER.debug("pu: " + persistUnit + ", threadid: " + threadId);
        return dbManager.getEntityManager(persistUnit, threadId);
    }

    /**
     * Recupera todos los registros de una tabla
     *
     * @param <T>
     * @param entityClass clase mapeada a la tabla
     * @param persistentUnit unidad de persistencia
     * @return un list con los registros de una tabla
     * @throws Exception
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public <T extends Object> List<T> findAll(Class<T> entityClass,
            String persistentUnit) throws Exception {
        LOGGER.debug("findAll");
        EntityManager em = getEntityManager(persistentUnit,
                                            Thread.currentThread().getId());

        CriteriaQuery<T> cq = em.getCriteriaBuilder().createQuery(entityClass);
        cq.select(cq.from(entityClass));

        List<T> result = em.createQuery(cq).getResultList();
        return result;
    }

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
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)    
    public <T extends IDataRow> T find(Class<T> entityClass,
            String persistentUnit,
            Object id) throws Exception {
        LOGGER.debug("find");
        EntityManager em = getEntityManager(persistentUnit,
                                            Thread.currentThread().getId());
        T row = em.find(entityClass, id);
        // Refrescar lazy members
        List<Field> fields = DataInfo.getLazyMembers(row.getClass());
        if (!fields.isEmpty()) {
            List obj;
            for (Field field : fields) {
                field.setAccessible(true);
                obj = (List) field.get(row);
                obj.size();
            }
        }
        return row;
    }

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
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)        
    public <T extends IDataRow> T findByUk(Class<T> entityClass,
            String persistentUnit, T ejb) throws Exception {
        T row;
        Query q;
        q = getEntityManager(persistentUnit,
                Thread.currentThread().getId()).createQuery(ejb.getQueryUK());

        for (Parameter param : q.getParameters()) {
            q.setParameter(param, ejb.getValue(param.getName()));
        }
        try {
            row = (T) q.getSingleResult();
            // Refrescar lazy members
            List<Field> fields = DataInfo.getLazyMembers(row.getClass());
            if (!fields.isEmpty()) {
                List obj;
                for (Field field : fields) {
                    field.setAccessible(true);
                    obj = (List) field.get(row);
                    obj.size();
                }
            }
        } catch (NoResultException exp) {
            row = null;
        }
        return row;
    }

    /**
     *
     * @param <T>
     * @param entityClass clase mapeada a una tabla
     * @param persistentUnit unidad de persistencia
     * @param queryString sentencia jpql
     * @param parameters parametros de la sentencia
     * @return un objeto con valores del registro de la tabla solicitada
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> T findByQuery(Class<T> entityClass, String persistentUnit,
            String queryString, Map<String, Object> parameters)
            throws Exception {
        LOGGER.debug("findByQuery");
        LOGGER.debug(queryString);

        EntityManager em = getEntityManager(persistentUnit,
                                            Thread.currentThread().getId());

        Query query = em.createQuery(queryString);
        if (parameters != null && !parameters.isEmpty()) {
            populateQueryParameters(query, parameters, queryString);
        }
        T result;
        try {
            result = (T) query.getSingleResult();
            // Refrescar lazy members
            List<Field> fields = DataInfo.getLazyMembers(result.getClass());
            if (!fields.isEmpty()) {
                List obj;
                for (Field field : fields) {
                    field.setAccessible(true);
                    obj = (List) field.get(result);
                    obj.size();
                }
            }
        } catch (NoResultException exp) {
            result = null;
        }
        LOGGER.debug("-- RESULT --");
        LOGGER.debug(result);
        return result;
    }

    /**
     *
     * @param <T>
     * @param persistentUnit unidad de persistencia
     * @param queryString sentencia jpql
     * @param parameters parametros de la sentencia
     * @return una lista de objetos conteniendo los registros de la tabla
     * solicitada
     * @throws Exception
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public <T extends IDataRow> List<T> findListByQuery(String persistentUnit,
            String queryString,
            Map<String, Object> parameters) throws Exception {
        return findListByQuery(persistentUnit, queryString, parameters, 0, 0);
    }

    /**
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
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public <T extends IDataRow> List<T> findListByQuery(String persistentUnit,
            String queryString,
            Map<String, Object> parameters,
            int first, int max) throws Exception {
        LOGGER.debug("findListByQuery");
        EntityManager em = getEntityManager(persistentUnit,
                                            Thread.currentThread().getId());

        Query query = em.createQuery(queryString);
        if (parameters != null && !parameters.isEmpty()) {
            populateQueryParameters(query, parameters, queryString);
        }
        query.setFirstResult(first);
        query.setMaxResults(max);
        List<T> result = (List<T>) query.getResultList();
        return result;
    }

    /**
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
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public <T extends IDataRow> List<T> findListByQuery(String persistentUnit,
            String queryString,
            int first, int max) throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        return findListByQuery(persistentUnit, queryString, parameters, first, max);
    }

    /**
     *
     * @param <T>
     * @param persistentUnit unidad de persistencia
     * @param namedQuery namedQuery
     * @param parameters parámetros de la sentencia.
     * @return un objeto con los datos del registro de la tabla solicitada
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> T findByNamedQuery(String persistentUnit,
            String namedQuery,
            Map<String, Object> parameters) throws Exception {
        EntityManager em = getEntityManager(persistentUnit,
                Thread.currentThread().getId());

        Query query = em.createNamedQuery(namedQuery);
        if (parameters != null && !parameters.isEmpty()) {
            populateQueryParameters(query, parameters, null);
        }
        query.setMaxResults(1);
        T result;
        try {
            result = (T) query.getSingleResult();
            // Refrescar lazy members
            List<Field> fields = DataInfo.getLazyMembers(result.getClass());
            if (!fields.isEmpty()) {
                List obj;
                for (Field field : fields) {
                    field.setAccessible(true);
                    obj = (List) field.get(result);
                    obj.size();
                }
            }
        } catch (NoResultException exp) {
            result = null;
        }
        return result;
    }

    /**
     *
     * @param <T>
     * @param persistentUnit unidad de persistencia
     * @param namedQuery namedQuery
     * @param parameters parámetros de la sentencia.
     * @return una lista de objetos con los datos de los registros de la tabla
     * solicitada
     * @throws Exception
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public <T extends IDataRow> List<T> findListByNamedQuery(String persistentUnit,
            String namedQuery,
            Map<String, Object> parameters)
            throws Exception {
        return findListByNamedQuery(persistentUnit, namedQuery, parameters, 0, 0);
    }

    /**
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
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public <T extends IDataRow> List<T> findListByNamedQuery(String persistentUnit,
            String namedQuery,
            int first, int max)
            throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        return findListByNamedQuery(persistentUnit, namedQuery, parameters, first, max);
    }

    /**
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
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public <T extends IDataRow> List<T> findListByNamedQuery(String persistentUnit,
            String namedQuery,
            Map<String, Object> parameters,
            int first, int max) throws Exception {

        EntityManager em = getEntityManager(persistentUnit,
                Thread.currentThread().getId());

        Query query = em.createNamedQuery(namedQuery);
        if (parameters != null && !parameters.isEmpty()) {
            populateQueryParameters(query, parameters, null);
        }
        if (first > 0 && max > 0) {
            query.setFirstResult(first);
            query.setMaxResults(max);
        }
        List<T> result = (List<T>) query.getResultList();

        return result;
    }

    /**
     *
     * @param persistentUnit unidad de persistencia
     * @param queryString sentencia sql
     * @param parameters parámetros de la sentencia.
     * @return una lista de objetos con datos de los registros solicitados
     * @throws Exception
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Object> findByNativeQuery(String persistentUnit, String queryString,
            Map<String, Object> parameters) throws Exception {
        LOGGER.debug("findByNativeQuery");
        queryString = Strings.textMerge(queryString, getQueryConstants(persistentUnit));
        EntityManager em = getEntityManager(persistentUnit,
                Thread.currentThread().getId());

        Query query = em.createNativeQuery(queryString);
        if (parameters != null && !parameters.isEmpty()) {
            populateQueryParameters(query, parameters, queryString);
        }
        List<Object> result = query.getResultList();
        return result;
    }

    /**
     *
     * @param persistentUnit unidad de persistencia
     * @param queryString sentencia sql
     * @param parameters parámetros de la sentencia.
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos con datos de los registros solicitados
     * @throws Exception
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Object> findByNativeQuery(String persistentUnit, String queryString,
            Map<String, Object> parameters,
            int first, int max) throws Exception {
        LOGGER.debug("findByNativeQuery");
        queryString = Strings.textMerge(queryString, getQueryConstants(persistentUnit));

        EntityManager em = getEntityManager(persistentUnit,
                Thread.currentThread().getId());

        Query query = em.createNativeQuery(queryString);
        if (parameters != null && !parameters.isEmpty()) {
            populateQueryParameters(query, parameters, queryString);
        }
        query.setFirstResult(first);
        query.setMaxResults(max);
        List<Object> result = query.getResultList();
        return result;
    }

    /**
     *
     * @param persistentUnit unidad de persistencia
     * @param sqlString sentencia sql
     * @param parameters parámetros de la sentencia.
     * @return un objeto error si no se ejecuto la sentencia con exito
     */
    @Override
    public IErrorReg sqlExec(String persistentUnit, String sqlString,
            Map<String, Object> parameters) {
        LOGGER.debug("exec");
        sqlString = Strings.textMerge(sqlString, getQueryConstants(persistentUnit));
        EntityManager em = getEntityManager(persistentUnit,
                Thread.currentThread().getId());
        ErrorReg error = null;
        try {
            Query sql = em.createNativeQuery(sqlString);
            if (parameters != null && !parameters.isEmpty()) {
                populateQueryParameters(sql, parameters, sqlString);
            }
            sql.executeUpdate();
        } catch (Exception exp) {
            error = new ErrorReg();
            error.setMessage(exp.getLocalizedMessage());
        }
        return error;
    }

    /**
     * Sincroniza un ejb con la base de datos.
     *
     * @param <T>
     * @param persistentUnit unidad de persistencia
     * @param ejb objeto mapeado a un registro de una tabla.
     * @param sessionId identificador de la sesion del usuario
     * @return Devuelve un objeto con el resultado de la grabación
     *
     */
    @Override
    public <T extends IDataRow> IDataResult update(String persistentUnit, T ejb, String sessionId) {
        List<T> ejbs = new ArrayList<>();
        ejbs.add(ejb);
        return update(persistentUnit, ejbs, sessionId);
    }

    /**
     * Sincroniza una lista de ejbs con la base de datos.
     *
     * @param <T>
     * @param ejbs lista de objetos mapeados a los registros de una tabla.
     * @param persistentUnit unidad de persistencia.
     * @param sessionId identificador de la sesion del usuario
     * @return Devuelve un objeto con el resultado de la grabación
     */
    @Override
    public <T extends IDataRow> IDataResult update(String persistentUnit, IDataObject ejbs, String sessionId) {
        return update(persistentUnit, ejbs.getDataRows(), sessionId);
    }

    /**
     * Sincroniza una lista de ejbs con la base de datos.
     *
     * @param <T>
     * @param ejbs lista de objetos mapeados a los registros de una tabla.
     * @param persistentUnit unidad de persistencia.
     * @param sessionId identificador de la sesion del usuario
     * @return Devuelve un objeto con el resultado de la grabación
     */
    @Override
    public <T extends IDataRow> IDataResult update(String persistentUnit, List<T> ejbs, String sessionId) {
        IDataSet dataSet = new DataSet();
        dataSet.add("1", (List<IDataRow>) ejbs);
        return update(persistentUnit, dataSet, sessionId);
    }

    /**
     * Sincroniza una lista de ejbs con la base de datos.
     *
     * @param dataSet set de objetos mapeados a los registros de una tabla.
     * @param persistentUnit unidad de persistencia.
     * @param sessionId identificador de la sesion del usuario
     * @return Devuelve un objeto con el resultado de la grabación
     */
    @Override
    public IDataResult update(String persistentUnit, IDataSet dataSet, String sessionId) {
        if (dataSet == null || dataSet.size() == 0) {
            return null;
        }
        String appUser = "";
        if (!Strings.isNullorEmpty(sessionId)) {
            appUser = getUserSession(sessionId).getUser().getClave();
        }

        IDataResult dataResult = new DataResult();
        IDataRow lastEjb = null;
        EntityManager em = getEntityManager(persistentUnit, Thread.currentThread().getId());

        // Recorrer los objetos a actualizar en la base
        for (Map.Entry<String, List<? extends IDataRow>> entry : dataSet.getMapListSet().entrySet()) {
            List<? extends IDataRow> ejbs = entry.getValue();
            List<IDataRow> ejbsRes = new ArrayList();
            try {
                for (IDataRow ejb : ejbs) {
                    lastEjb = ejb;
                    switch (ejb.getOperation()) {
                        case IDataRow.AGREGAR:
                            setAppUser(ejb, appUser);
                            ejbsRes.add(ejb);
                            em.persist(ejb);
                            break;
                        case IDataRow.MODIFICAR:
                            setAppUser(ejb, appUser);
                            ejbsRes.add(ejb);
                            em.merge(ejb);
                            break;
                        case IDataRow.BORRAR:
                            ejbsRes.add(ejb);
                            em.remove(em.merge(ejb));
                            ejbsRes.remove(ejb);
                            break;
                        default:
                            break;
                    }
                    ejb.setErrors((Map<String, IErrorReg>) null);
                }
                em.flush();
                for (IDataRow ejb : ejbs) {
                    if (ejb.getOperation() != IDataRow.BORRAR) {
                        ejb.setOperation(0);
                    }
                }
                dataResult.put(entry.getKey(), ejbsRes);
            } catch (Exception ex) {
                String msgError = ErrorManager.getStackCause(ex);
                if (lastEjb != null) {
                    lastEjb.setErrors(msgError, "", 0);
                }
                dataResult.put(entry.getKey(), ejbsRes);
                dataResult.setSuccess(false);
                dataResult.setErrorMsg(msgError);
                //dataResult.setException(ex);
                dbManager.rollBack();
                ErrorManager.showError(ex, LOGGER);
                break;
            }
        }
        return dataResult;
    }

    /**
     * Agregar,un registro en la tabla
     *
     * @param <T> tipo de dato generalemente hereda de DataRow
     * @param persistentUnit unidad de persistencia
     * @param ejb el objeto con los valores del registro
     * @param sessionId id de la sesion del usuario
     * @return
     */
    @Override
    public <T extends IDataRow> IDataResult persist(String persistentUnit, T ejb, String sessionId){
        ejb.setOperation(IDataRow.AGREGAR);
        return update(persistentUnit, ejb, sessionId);
    }     

    /**
     * Modificar un registro en la tabla dada
     *
     * @param <T> tipo de dato generalemente hereda de DataRow
     * @param persistentUnit unidad de persistencia
     * @param ejb el objeto con los valores del registro
     * @param sessionId id de sesión del usuario
     * @return
     */
    @Override
    public <T extends IDataRow> IDataResult merge(String persistentUnit, T ejb, String sessionId){
        ejb.setOperation(IDataRow.MODIFICAR);
        return update(persistentUnit, ejb, sessionId);
    }     

    /**
     * Borra un registro en la tabla dada
     *
     * @param <T> tipo de dato generalemente hereda de DataRow
     * @param persistentUnit unidad de persistencia
     * @param ejb el objeto con los valores del registro
     * @param sessionId id de la sesión del usuario
     * @return
     */
    @Override
    public <T extends IDataRow> IDataResult remove(String persistentUnit, T ejb, String sessionId){
        ejb.setOperation(IDataRow.BORRAR);
        return update(persistentUnit, ejb, sessionId);
    }     
    
    @Override
    public <T extends IDataRow> List<T> getData(String persistentUnit,
            String queryString,
            int maxRows,
            boolean noCache) throws Exception {
        if (maxRows == 0) {
            return new ArrayList<>();
        }
        EntityManager em = getEntityManager(persistentUnit, Thread.currentThread().getId());
        Query q;
        q = em.createQuery(queryString);
        if (maxRows >= 0) {
            q.setMaxResults(maxRows);
        }
        List<T> devolver = (List<T>) q.getResultList();
        return devolver;
    }

    @Override
    public <T extends IDataRow> List<T> getData(Query query) throws Exception {
        return query.getResultList();
    }

    /**
     * Refresca desde la base de datos los valores de un objeto.
     *
     * @param <T>
     * @param persistentUnit unidad de persistencia
     * @param row objeto o registro a refrescar
     * @return objeto con los datos refrescados de la base de datos
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> T refreshRow(String persistentUnit, T row) throws Exception {
        row = this.find((Class<T>) row.getClass(), persistentUnit, DataInfo.getIdvalue(row));
        return row;
    }

    /**
     * Refresca desde la base de datos una lista de objetos.
     *
     * @param <T>
     * @param persistentUnit unidad de persistencia
     * @param rows objetos a refrescar
     * @return lista de objetos con los datos refrescados de la base de datos
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> List<T> refreshAll(String persistentUnit, List<T> rows) throws Exception {
        EntityManager em = getEntityManager(persistentUnit, Thread.currentThread().getId());
        for (int i = 0; i <= rows.size(); i++) {
            em.refresh(rows.get(i));
        }
        return rows;
    }

    /**
     *
     * @param persistentUnit unidad de persistencia
     * @param queryString sentencia jpql
     * @param parameters parámetros de la sentencia
     * @return cantidad de registros que debería devolver la sentencia.
     * @throws Exception
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Long getCount(String persistentUnit, String queryString, Map<String, Object> parameters) throws Exception {
        LOGGER.debug("getCount");
        Long result;
        queryString = Strings.textMerge(queryString, getQueryConstants(persistentUnit));
        int pos = Strings.findString("from ", queryString.toLowerCase());
        int pos2 = Strings.findString(" order by ", queryString.toLowerCase());
        if (pos2 <= 0) {
            queryString = "select count(*) " + queryString.substring(pos);
        } else {
            queryString = "select count(*) " + queryString.substring(pos, pos2);
        }
        EntityManager em = getEntityManager(persistentUnit, Thread.currentThread().getId());
        Query query = em.createQuery(queryString);
        if (parameters != null && !parameters.isEmpty()) {
            populateQueryParameters(query, parameters, queryString);
        }
        result = ((Number) query.getSingleResult()).longValue();
        return result;
    }

    /**
     *
     * @param persistentUnit unidad de persistencia
     * @param queryString sentencia sql
     * @param parameters parámetros de la sentencia
     * @return cantidad de registros que debería devolver la sentencia.
     * @throws Exception
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Long getCount2(String persistentUnit, String queryString, Map<String, Object> parameters) throws Exception {
        LOGGER.debug("getCount2");
        queryString = Strings.textMerge(queryString, getQueryConstants(persistentUnit));
        queryString = "select count(*) from (" + queryString + ") x";
        List<Object> result = this.findByNativeQuery(persistentUnit, queryString, parameters);
        return Long.parseLong(result.get(0).toString());
    }

    /**
     * Busca y devuelve el valor de una propiedad solicitada del entity manager
     *
     * @param persistUnit unidad de persistencia
     * @return el valor de una propiedad del entity manager
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Map<String, Object> getEntityManagerProp(String persistUnit) {
        EntityManager em = getEntityManager(persistUnit, Thread.currentThread().getId());
        if (em == null) {
            LOGGER.info("Entity Manager Nulo");
            return null;
        }
        return em.getProperties();
    }

    /**
     * Busca y devuelve el valor de una propiedad solicitada 1
     * 
     * @param persistUnit   unidad de persistencia.
     * @return              devuelve un map con todas las propiedades de la unidad de persistencia solicitada
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Map<String, Object> getPersistUnitProp(String persistUnit) {
        Map<String, Object> result = new HashMap<>();
        try {
            EntityManager em = getEntityManager(persistUnit, Thread.currentThread().getId());
            if (em.getEntityManagerFactory() == null) {
                LOGGER.info("Entity Manager Factory Nulo");
                return null;
            }
            Map<String, Object> result2 = em.getEntityManagerFactory().getProperties();
            result2.entrySet().forEach((e) -> {
                Object object = e.getValue().toString();
                result.put(e.getKey(), object);
            });
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return result;
    }

    /**
     *
     * @param persistentUnit unidad de persistencia
     * @return devuelve el nombre de la base de datos.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String getDataEngine(String persistentUnit) {
        String result = (String) this.getPersistUnitProp(persistentUnit).get("oym.motordatos");
        return result;
    }

    /**
     *
     * @param persistentUnit unidad de persistencia
     * @return devuelve el schema
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String getSchema(String persistentUnit) {
        String schema = (String) getPersistUnitProp(persistentUnit).get("hibernate.default_schema");
        return schema;
    }

    /**
     *
     * @param persistUnit unidad de persistencia
     * @return una conexión a la base solo funciona en un ambiente @local no
     * funciona en @Remote
     */
    @Override
    public Connection getConnection(String persistUnit) {
        // Para eclipse link
        Connection connection = getEntityManager(persistUnit,
                Thread.currentThread().getId())
                .unwrap(java.sql.Connection.class);
        return connection;
    }

    /**
     *
     * @param persistUnit unidad de persistencia
     * @param conn objeto factory cuya función es devolver una conexión del
     * entity manager
     * @return devuelve una conexión del entity manager
     */
    @Override
    public Connection getConnection(String persistUnit, IDBConnectFactory conn) {
        EntityManager em = getEntityManager(persistUnit, Thread.currentThread().getId());
        Connection result = conn.getConnection(em);
        return result;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private void populateQueryParameters(Query query, Map<String, Object> parameters, String queryString) {
        parameters.entrySet().forEach((entry) -> {
            if (queryString != null) {
                if (Strings.findString(":" + entry.getKey(), queryString) >= 0) {
                    query.setParameter(entry.getKey(), entry.getValue());
                }
            } else {
                query.setParameter(entry.getKey(), entry.getValue());
            }
        });
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private Map<String, String> getQueryConstants(String persistUnit) {
        Map<String, String> queryConstants = new HashMap<>();
        String schema = (String) this.getPersistUnitProp(persistUnit).get("hibernate.default_schema");
        queryConstants.put("schema", schema);
        String schemaCat = (String) this.getPersistUnitProp(IDBManager.CATALOGO).get("hibernate.default_schema");
        queryConstants.put("schemacatalog", schemaCat);
        
        String motordatos = (String)this.getPersistUnitProp(persistUnit).get("oym.motordatos");

        // Asignar las constantes booleanas
        if ("POSTGRES".equals(motordatos)){
            queryConstants.put("true",  "true");
            queryConstants.put("false", "false");
        }
        else{
            queryConstants.put("true",  "1");
            queryConstants.put("false", "0");            
        }
        // Asignar las constantes de fecha y hora
	if (Fn.inList(motordatos,"SQLSERVER","Microsoft SQL Server","SYBASE")){
            queryConstants.put("now",  "getdate()");
            queryConstants.put("today", "CONVERT(date, GETDATE())");
        }
        else if (Fn.inList(motordatos,"ORACLE","ORACLE8")){
            queryConstants.put("now",  "sysdate");
            queryConstants.put("today","TRUNC(sysdate)");
        }
        else if ("POSTGRES".equals(motordatos)){
            queryConstants.put("now",  "now()");
            queryConstants.put("today","date_trunc('day', now())");
        }
        else if ("DB2".equals(motordatos)){
            queryConstants.put("now",  "CURRENT_TIMESTAMP"); 
            queryConstants.put("today","CURRENT DATE");                    
        }
        return queryConstants;
    }

    /**
     *
     * @param sessionId id de sesión del usuario
     * @return el objeto userSession con la información de la sesión
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public IUserSession getUserSession(String sessionId) {
        return sessions.getUserSession(sessionId);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private void setAppUser(IDataRow ejb, String appUser) {
        if (DataInfo.isFieldExist(ejb.getClass(), "appuser")) {
            ejb.setValue("appuser", appUser);
        }
    }
}

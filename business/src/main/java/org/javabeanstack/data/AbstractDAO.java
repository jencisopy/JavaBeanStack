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

import org.javabeanstack.data.model.DataResult;
import org.javabeanstack.data.model.DataSet;
import org.javabeanstack.datactrl.IDataObject;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.error.IErrorReg;
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
import org.javabeanstack.exceptions.CompanyError;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.security.ISessions;
import org.javabeanstack.security.IUserSession;
import org.javabeanstack.util.Fn;
import static org.javabeanstack.util.Fn.nvl;
import org.javabeanstack.util.Strings;

/**
 * Esta clase se encarga de la conexión con la base de datos. Provee metodos que
 * permiten la recuperación, busqueda, refresco, borrado, inserción y
 * actualización de los registros. Esta clase se ejecuta como EJB en la capa de
 * la lógica del negocio.
 *
 * @author Jorge Enciso
 */
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public abstract class AbstractDAO implements IGenericDAO {
    private static final Logger LOGGER = Logger.getLogger(AbstractDAO.class);
    private static final String DEFAULT_SCHEMA_PROPERTY="hibernate.default_schema";
    public static final String FALSE = "false";
    public static final String TRUE = "true";

    /**
     * Es el objeto responsable de la creación y gestión de los entity manager
     */
    @EJB
    private IDBManager dbManager;
    /**
     * Es el objeto responsable gestión de las sesiones de usuarios
     */
    @EJB
    private ISessions sessions;

    public AbstractDAO() {
    }

    /**
     * Devuelve un entity manager. Se crea un entity manager por cada thread y
     * unidad de persistencia o por cada sesión de usuario y unidad de
     * persistencia.
     *
     * @param keyId
     * @return un entity manager
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    protected EntityManager getEntityManager(String keyId) {
        String persistUnit = keyId.substring(0, keyId.indexOf(':')).toLowerCase();
        LOGGER.debug("getEntityManager()");
        LOGGER.debug("pu: " + persistUnit + ", id: " + keyId);
        return dbManager.getEntityManager(keyId);
    }

    /**
     * Recupera todos los registros de una tabla
     *
     * @param <T>
     * @param entityClass clase mapeada a la tabla
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @return un list con los registros de una tabla
     * @throws Exception
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public <T extends Object> List<T> findAll(Class<T> entityClass,
            String sessionId) throws Exception {
        LOGGER.debug(Strings.replicate("-", 50));
        LOGGER.debug("findAll");
        LOGGER.debug(entityClass.toString());

        IDBLinkInfo dbLinkInfo = sessions.getDBLinkInfo(sessionId);
        EntityManager em = getEntityManager(getEntityId(dbLinkInfo));

        CriteriaQuery<T> cq = em.getCriteriaBuilder().createQuery(entityClass);
        cq.select(cq.from(entityClass));

        return em.createQuery(cq).getResultList();
    }

    /**
     * Devuelve un registro de una tabla dada
     *
     * @param <T>
     * @param entityClass clase mapeada a la tabla
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param id identificador del registro
     * @return un registro solicitado
     * @throws Exception
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <T extends IDataRow> T findById(Class<T> entityClass,
            String sessionId,
            Object id) throws Exception {
        LOGGER.debug(Strings.replicate("-", 50));
        LOGGER.debug("find");
        LOGGER.debug(entityClass.toString());

        IDBLinkInfo dbLinkInfo = sessions.getDBLinkInfo(sessionId);
        EntityManager em = getEntityManager(getEntityId(dbLinkInfo));
        T row = em.find(entityClass, id);
        if (row == null){
            return null;
        }
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
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param ejb objeto ejb con los datos de la clave unica
     * @return un registro que cumple la condición de la clave unica solicitada.
     * @throws Exception
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <T extends IDataRow> T findByUk(String sessionId, T ejb) throws Exception {
        LOGGER.debug(Strings.replicate("-", 50));
        LOGGER.debug("findByUk");
        LOGGER.debug(ejb.getClass().getSimpleName());

        // Verificar que exista manera de generar la sentencia para buscar
        // el registro o de lo contrario va a dar error.
        if (ejb.getQueryUK() == null) {
            return null;
        }
        IDBLinkInfo dbLinkInfo = sessions.getDBLinkInfo(sessionId);
        // Buscar registro por la clave unica
        T row;
        Query q = getEntityManager(getEntityId(dbLinkInfo)).createQuery(ejb.getQueryUK());

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
     * Devuelve una lista de registros de una tabla dada
     *
     * @param <T>
     * @param entityClass clase mapeada a la tabla
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @return lista de objetos
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> List<T> find(Class<T> entityClass, String sessionId) throws Exception {
        return find(entityClass, sessionId, null, null, null, 0, 0);
    }

    /**
     * Devuelve una lista de registros de una tabla dada
     *
     * @param <T>
     * @param entityClass clase mapeada a la tabla
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param order
     * @param filter
     * @param params
     * @return lista de objetos
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> List<T> find(Class<T> entityClass, String sessionId,
            String order, String filter, Map<String, Object> params) throws Exception {
        return find(entityClass, sessionId, order, filter, params, 0, 0);
    }

    /**
     * Devuelve una lista de registro de una tabla dada
     *
     * @param <T>
     * @param entityClass clase mapeada a la tabla
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param order
     * @param filter
     * @param params
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return lista de objetos
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> List<T> find(Class<T> entityClass, String sessionId, String order, String filter, Map<String, Object> params, int first, int max) throws Exception {
        String query = "select o from " + entityClass.getSimpleName() + " o ";
        if (filter == null) {
            filter = "";
        }
        IDBLinkInfo dbLinkInfo = sessions.getDBLinkInfo(sessionId);
        T entity = entityClass.newInstance();
        // Si se va a aplicar el filtro por defecto 
        if (entity.isApplyDBFilter()) {
            String operator = (filter.isEmpty() ? "" : " and ");
            if (dbLinkInfo.getDBFilter() != null){
                String dbFilterExpr = dbLinkInfo.getDBFilter().getFilterExpr(entityClass, "");
                if (!Strings.isNullorEmpty(dbFilterExpr)) {
                    filter = dbFilterExpr + operator + filter;
                }
            }
        }
        //Filtro
        if (!Strings.isNullorEmpty(filter)) {
            query += " where " + filter;
        }
        //Orden
        if (!Strings.isNullorEmpty(order)) {
            query += " order by " + order;
        }
        return this.findListByQuery(sessionId, query, params, first, max);
    }

    /**
     *
     * @param <T>
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param queryString sentencia jpql
     * @param parameters parametros de la sentencia
     * @return un objeto con valores del registro de la tabla solicitada
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> T findByQuery(String sessionId,
            String queryString, Map<String, Object> parameters)
            throws Exception {
        LOGGER.debug(Strings.replicate("-", 50));
        LOGGER.debug("findByQuery");
        LOGGER.debug(queryString);

        IDBLinkInfo dbLinkInfo = sessions.getDBLinkInfo(sessionId);
        EntityManager em = getEntityManager(getEntityId(dbLinkInfo));

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
        LOGGER.debug("-RESULT-");
        LOGGER.debug(result);
        return result;
    }

    /**
     *
     * @param <T>
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param queryString sentencia jpql
     * @param parameters parametros de la sentencia
     * @return una lista de objetos conteniendo los registros de la tabla
     * solicitada
     * @throws Exception
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public <T extends IDataRow> List<T> findListByQuery(String sessionId,
            String queryString,
            Map<String, Object> parameters) throws Exception {
        return findListByQuery(sessionId, queryString, parameters, 0, 0);
    }

    /**
     *
     * @param <T>
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
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
    public <T extends IDataRow> List<T> findListByQuery(String sessionId,
            String queryString,
            Map<String, Object> parameters,
            int first, int max) throws Exception {
        LOGGER.debug(Strings.replicate("-", 50));
        LOGGER.debug("findListByQuery");
        LOGGER.debug(queryString);

        IDBLinkInfo dbLinkInfo = sessions.getDBLinkInfo(sessionId);
        EntityManager em = getEntityManager(getEntityId(dbLinkInfo));

        Query query = em.createQuery(queryString);
        if (parameters != null && !parameters.isEmpty()) {
            populateQueryParameters(query, parameters, queryString);
        }
        if (max > 0) {
            query.setFirstResult(first);
            query.setMaxResults(max);
        }
        return (List<T>) query.getResultList();
    }

    /**
     *
     * @param <T>
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param queryString sentencia jpql
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos conteniendo los registros de la tabla
     * solicitada
     * @throws Exception
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public <T extends IDataRow> List<T> findListByQuery(String sessionId,
            String queryString,
            int first, int max) throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        return findListByQuery(sessionId, queryString, parameters, first, max);
    }

    /**
     *
     * @param <T>
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param namedQuery namedQuery
     * @param parameters parámetros de la sentencia.
     * @return un objeto con los datos del registro de la tabla solicitada
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> T findByNamedQuery(String sessionId,
            String namedQuery,
            Map<String, Object> parameters) throws Exception {
        IDBLinkInfo dbLinkInfo = sessions.getDBLinkInfo(sessionId);
        EntityManager em = getEntityManager(getEntityId(dbLinkInfo));

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
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param namedQuery namedQuery
     * @param parameters parámetros de la sentencia.
     * @return una lista de objetos con los datos de los registros de la tabla
     * solicitada
     * @throws Exception
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public <T extends IDataRow> List<T> findListByNamedQuery(String sessionId,
            String namedQuery,
            Map<String, Object> parameters)
            throws Exception {
        return findListByNamedQuery(sessionId, namedQuery, parameters, 0, 0);
    }

    /**
     *
     * @param <T>
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param namedQuery namedQuery
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos con los datos de los registros de la tabla
     * solicitada
     * @throws Exception
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public <T extends IDataRow> List<T> findListByNamedQuery(String sessionId,
            String namedQuery,
            int first, int max)
            throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        return findListByNamedQuery(sessionId, namedQuery, parameters, first, max);
    }

    /**
     *
     * @param <T>
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param namedQuery namedQuery
     * @param parameters parámetros de la sentencia.
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos con los registros de la tabla solicitada
     * @throws Exception
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public <T extends IDataRow> List<T> findListByNamedQuery(String sessionId,
            String namedQuery,
            Map<String, Object> parameters,
            int first, int max) throws Exception {

        IDBLinkInfo dbLinkInfo = sessions.getDBLinkInfo(sessionId);
        EntityManager em = getEntityManager(getEntityId(dbLinkInfo));

        Query query = em.createNamedQuery(namedQuery);
        if (parameters != null && !parameters.isEmpty()) {
            populateQueryParameters(query, parameters, null);
        }
        if (first > 0 && max > 0) {
            query.setFirstResult(first);
            query.setMaxResults(max);
        }
        return (List<T>) query.getResultList();
    }

    /**
     *
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param queryString sentencia sql
     * @param parameters parámetros de la sentencia.
     * @return una lista de objetos con datos de los registros solicitados
     * @throws Exception
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Object> findByNativeQuery(String sessionId, String queryString,
            Map<String, Object> parameters) throws Exception {
        LOGGER.debug(Strings.replicate("-", 50));
        LOGGER.debug("findByNativeQuery");

        IDBLinkInfo dbLinkInfo = sessions.getDBLinkInfo(sessionId);
        String persistUnit;
        if (dbLinkInfo != null) {
            persistUnit = dbLinkInfo.getPersistUnit();
        } else {
            persistUnit = IDBManager.CATALOGO;
        }
        queryString = Strings.textMerge(queryString, getQueryConstants(persistUnit));
        LOGGER.debug(queryString);

        EntityManager em = getEntityManager(getEntityId(dbLinkInfo));

        Query query = em.createNativeQuery(queryString);
        if (parameters != null && !parameters.isEmpty()) {
            populateQueryParameters(query, parameters, queryString);
        }
        return query.getResultList();
    }

    /**
     *
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param queryString sentencia sql
     * @param parameters parámetros de la sentencia.
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos con datos de los registros solicitados
     * @throws Exception
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Object> findByNativeQuery(String sessionId, String queryString,
            Map<String, Object> parameters,
            int first, int max) throws Exception {
        LOGGER.debug(Strings.replicate("-", 50));
        LOGGER.debug("findByNativeQuery");

        IDBLinkInfo dbLinkInfo = sessions.getDBLinkInfo(sessionId);
        String persistUnit;
        if (dbLinkInfo != null) {
            persistUnit = dbLinkInfo.getPersistUnit();
        } else {
            persistUnit = IDBManager.CATALOGO;
        }

        queryString = Strings.textMerge(queryString, getQueryConstants(persistUnit));
        LOGGER.debug(queryString);

        EntityManager em = getEntityManager(getEntityId(dbLinkInfo));

        Query query = em.createNativeQuery(queryString);
        if (parameters != null && !parameters.isEmpty()) {
            populateQueryParameters(query, parameters, queryString);
        }
        query.setFirstResult(first);
        query.setMaxResults(max);
        return query.getResultList();
    }

    /**
     *
     * @param <T>
     * @param clazz
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param queryString sentencia sql
     * @param parameters parámetros de la sentencia.
     * @param first a partir de este nro. de registro se va a traer los datos
     * @param max cantidad maxima de registros
     * @return una lista de objetos con datos de los registros solicitados
     * @throws Exception
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public <T extends IDataRow> List<T> findByNativeQuery(Class<T> clazz, String sessionId, String queryString,
            Map<String, Object> parameters,
            int first, int max) throws Exception {
        LOGGER.debug(Strings.replicate("-", 50));
        LOGGER.debug("findByNativeQuery");

        IDBLinkInfo dbLinkInfo = sessions.getDBLinkInfo(sessionId);
        String persistUnit;
        if (dbLinkInfo != null) {
            persistUnit = dbLinkInfo.getPersistUnit();
        } else {
            persistUnit = IDBManager.CATALOGO;
        }

        queryString = Strings.textMerge(queryString, getQueryConstants(persistUnit));
        LOGGER.debug(queryString);

        EntityManager em = getEntityManager(getEntityId(dbLinkInfo));

        Query query = em.createNativeQuery(queryString, clazz);
        if (parameters != null && !parameters.isEmpty()) {
            populateQueryParameters(query, parameters, queryString);
        }
        query.setFirstResult(first);
        query.setMaxResults(max);
        return query.getResultList();
    }
    
    /**
     *
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param sqlString sentencia sql
     * @param parameters parámetros de la sentencia.
     * @return un objeto error si no se ejecuto la sentencia con exito
     */
    @Override
    public IErrorReg sqlExec(String sessionId, String sqlString,
            Map<String, Object> parameters) throws Exception {
        LOGGER.debug(Strings.replicate("-", 50));
        LOGGER.debug("sqlExec");

        IDBLinkInfo dbLinkInfo = sessions.getDBLinkInfo(sessionId);
        String persistUnit;
        if (dbLinkInfo == null) {
            persistUnit = IDBManager.CATALOGO;
        } else {
            persistUnit = dbLinkInfo.getPersistUnit();
        }
        sqlString = Strings.textMerge(sqlString, getQueryConstants(persistUnit));
        LOGGER.debug(sqlString);

        EntityManager em = getEntityManager(getEntityId(dbLinkInfo));
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
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param ejb objeto mapeado a un registro de una tabla.
     * @return Devuelve un objeto con el resultado de la grabación
     *
     */
    @Override
    public <T extends IDataRow> IDataResult update(String sessionId, T ejb)  {
        List<T> ejbs = new ArrayList<>();
        ejbs.add(ejb);
        return update(sessionId, ejbs);
    }

    /**
     * Sincroniza una lista de ejbs con la base de datos.
     *
     * @param ejbs lista de objetos mapeados a los registros de una tabla.
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @return Devuelve un objeto con el resultado de la grabación
     */
    @Override
    public IDataResult update(String sessionId, IDataObject ejbs) {
        return update(sessionId, ejbs.getDataRows());
    }

    /**
     * Sincroniza una lista de ejbs con la base de datos.
     *
     * @param <T>
     * @param ejbs lista de objetos mapeados a los registros de una tabla.
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @return Devuelve un objeto con el resultado de la grabación
     */
    @Override
    public <T extends IDataRow> IDataResult update(String sessionId, List<T> ejbs) {
        IDataSet dataSet = new DataSet();
        dataSet.add("1", (List<IDataRow>) ejbs);
        return update(sessionId, dataSet);
    }

    /**
     * Sincroniza una lista de ejbs con la base de datos.
     *
     * @param dataSet set de objetos mapeados a los registros de una tabla.
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @return Devuelve un objeto con el resultado de la grabación
     */
    @Override
    public IDataResult update(String sessionId, IDataSet dataSet)  {
        if (dataSet == null || dataSet.size() == 0) {
            return null;
        }
        IDBLinkInfo dbLinkInfo = sessions.getDBLinkInfo(sessionId);
        String appUser = dbLinkInfo.getAppUserId();

        IDataResult dataResult = new DataResult();
        IDataRow lastEjb = null;
        EntityManager em = getEntityManager(getEntityId(dbLinkInfo));

        // Recorrer los objetos a actualizar en la base
        for (Map.Entry<String, List<? extends IDataRow>> entry : dataSet.getMapListSet().entrySet()) {
            List<? extends IDataRow> ejbs = entry.getValue();
            List<IDataRow> ejbsRes = new ArrayList();
            try {
                for (IDataRow ejb : ejbs) {
                    lastEjb = ejb;
                    switch (ejb.getAction()) {
                        case IDataRow.INSERT:
                            setAppUser(ejb, appUser);
                            ejbsRes.add(ejb);
                            checkFieldIdcompany(dbLinkInfo, ejb);                            
                            em.persist(ejb);
                            em.flush();
                            dataResult.setRowUpdated(ejb);                             
                            break;
                        case IDataRow.UPDATE:
                            setAppUser(ejb, appUser);
                            ejbsRes.add(ejb);
                            checkFieldIdcompany(dbLinkInfo, ejb);
                            em.merge(ejb);
                            em.flush();
                            dataResult.setRowUpdated(ejb);
                            break;
                        case IDataRow.DELETE:
                            ejbsRes.add(ejb);
                            checkFieldIdcompany(dbLinkInfo, ejb);
                            em.remove(em.merge(ejb));
                            ejbsRes.remove(ejb);
                            em.flush();
                            dataResult.setRowUpdated(ejb);                                                         
                            break;
                        default:
                            break;
                    }
                    ejb.setErrors((Map<String, IErrorReg>) null);
                }
                for (IDataRow ejb : ejbs) {
                    if (ejb.getAction() != IDataRow.DELETE) {
                        ejb.setAction(0);
                    }
                }
                dataResult.put(entry.getKey(), ejbsRes);
            } catch (Exception ex) {
                dataResult.setRowUpdated(null);
                String msgError = ErrorManager.getStackCause(ex);
                if (lastEjb != null) {
                    lastEjb.setErrors(msgError, "", 0);
                }
                dataResult.put(entry.getKey(), ejbsRes);
                dataResult.setSuccess(false);
                dataResult.setErrorMsg(msgError);
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
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param ejb el objeto con los valores del registro
     * @return IDataResult conteniendo el dato del registro agregado.
     */
    @Override
    public <T extends IDataRow> IDataResult persist(String sessionId, T ejb) {
        ejb.setAction(IDataRow.INSERT);
        return update(sessionId, ejb);
        
    }

    /**
     * Modificar un registro en la tabla dada
     *
     * @param <T> tipo de dato generalemente hereda de DataRow
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param ejb el objeto con los valores del registro
     * @return IDataResult conteniendo el dato del registro modificado.
     * @throws org.javabeanstack.exceptions.CheckException
     */
    @Override
    public <T extends IDataRow> IDataResult merge(String sessionId, T ejb) {
        ejb.setAction(IDataRow.UPDATE);
        return update(sessionId, ejb);
    }

    /**
     * Borra un registro en la tabla dada
     *
     * @param <T> tipo de dato generalemente hereda de DataRow
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param ejb el objeto con los valores del registro.
     * @return IDataResult conteniendo el dato del registro eliminado.
     */
    @Override
    public <T extends IDataRow> IDataResult remove(String sessionId, T ejb) {
        ejb.setAction(IDataRow.DELETE);
        return update(sessionId, ejb);
    }

    @Override
    public <T extends IDataRow> List<T> getData(String sessionId,
            String queryString,
            int maxRows,
            boolean noCache) throws Exception {
        if (maxRows == 0) {
            return new ArrayList<>();
        }
        IDBLinkInfo dbLinkInfo = sessions.getDBLinkInfo(sessionId);
        EntityManager em = getEntityManager(getEntityId(dbLinkInfo));
        Query q;
        q = em.createQuery(queryString);
        if (maxRows >= 0) {
            q.setMaxResults(maxRows);
        }
        return (List<T>) q.getResultList();
    }

    @Override
    public <T extends IDataRow> List<T> getData(Query query) throws Exception {
        return query.getResultList();
    }

    /**
     * Refresca desde la base de datos los valores de un objeto.
     *
     * @param <T>
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param row objeto o registro a refrescar
     * @return objeto con los datos refrescados de la base de datos
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> T refreshRow(String sessionId, T row) throws Exception {
        row = this.findById((Class<T>) row.getClass(), sessionId, DataInfo.getIdvalue(row));
        if (row == null){
            return null;
        }
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
     * Refresca desde la base de datos una lista de objetos.
     *
     * @param <T>
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param rows objetos a refrescar
     * @return lista de objetos con los datos refrescados de la base de datos
     * @throws Exception
     */
    @Override
    public <T extends IDataRow> List<T> refreshAll(String sessionId, List<T> rows) throws Exception {
        IDBLinkInfo dbLinkInfo = sessions.getDBLinkInfo(sessionId);
        EntityManager em = getEntityManager(getEntityId(dbLinkInfo));
        for (int i = 0; i <= rows.size(); i++) {
            em.refresh(rows.get(i));
        }
        return rows;
    }

    /**
     * Calcula la cantidad de registros que devolveria una sentencia sql
     *
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param queryString sentencia jpql
     * @param parameters parámetros de la sentencia
     * @return cantidad de registros que debería devolver la sentencia.
     * @throws Exception
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Long getCount(String sessionId, String queryString, Map<String, Object> parameters) throws Exception {
        LOGGER.debug(Strings.replicate("-", 50));
        LOGGER.debug("getCount");
        IDBLinkInfo dbLinkInfo = sessions.getDBLinkInfo(sessionId);
        Long result;
        String persistUnit = dbLinkInfo.getPersistUnit();
        queryString = Strings.textMerge(queryString, getQueryConstants(persistUnit));

        int pos = Strings.findString("from ", queryString.toLowerCase());
        int pos2 = Strings.findString(" order by ", queryString.toLowerCase());
        if (pos2 <= 0) {
            queryString = "select count(*) " + queryString.substring(pos);
        } else {
            queryString = "select count(*) " + queryString.substring(pos, pos2);
        }
        LOGGER.debug(queryString);

        EntityManager em = getEntityManager(getEntityId(dbLinkInfo));
        Query query = em.createQuery(queryString);
        if (parameters != null && !parameters.isEmpty()) {
            populateQueryParameters(query, parameters, queryString);
        }
        result = ((Number) query.getSingleResult()).longValue();
        return result;
    }

    /**
     * Calcula la cantidad de registros que devolveria una sentencia sql
     *
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param queryString sentencia sql
     * @param parameters parámetros de la sentencia
     * @return cantidad de registros que debería devolver la sentencia.
     * @throws Exception
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Long getCount2(String sessionId, String queryString, Map<String, Object> parameters) throws Exception {
        LOGGER.debug(Strings.replicate("-", 50));
        LOGGER.debug("getCount2");
        IDBLinkInfo dbLinkInfo = sessions.getDBLinkInfo(sessionId);
        String persistUnit = dbLinkInfo.getPersistUnit();
        queryString = Strings.textMerge(queryString, getQueryConstants(persistUnit));
        queryString = "select count(*) from (" + queryString + ") x";
        LOGGER.debug(queryString);

        List<Object> result = this.findByNativeQuery(sessionId, queryString, parameters);
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
        EntityManager em = getEntityManager(getEntityId(persistUnit));
        if (em == null) {
            LOGGER.info("Entity Manager Nulo");
            return null;
        }
        return em.getProperties();
    }

    /**
     * Busca y devuelve el valor de una propiedad solicitada 1
     *
     * @param persistUnit unidad de persistencia.
     * @return devuelve un map con todas las propiedades de la unidad de
     * persistencia solicitada
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Map<String, Object> getPersistUnitProp(String persistUnit) {
        Map<String, Object> result = new HashMap<>();
        try {
            EntityManager em = getEntityManager(getEntityId(persistUnit));
            if (em.getEntityManagerFactory() == null) {
                LOGGER.info("Entity Manager Factory Nulo");
                return null;
            }
            Map<String, Object> result2 = em.getEntityManagerFactory().getProperties();
            result2.entrySet().forEach( e -> {
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
     * @return devuelve el nombre del motor de la base de datos
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String getDataEngine(String persistentUnit) {
        String result;
        //TODO buscar en hibernate.dialect
        result = (String) this.getPersistUnitProp(persistentUnit).get("jbs.dbengine");
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
        return (String)getPersistUnitProp(persistentUnit).get(DEFAULT_SCHEMA_PROPERTY);
    }

    /**
     *
     * @param sessionId identificador de la sesión que permite realizar las
     * @return una conexión a la base solo funciona en un ambiente @local no
     * funciona en @Remote
     */
    @Override
    public Connection getConnection(String sessionId) {
        IDBLinkInfo dbLinkInfo = sessions.getDBLinkInfo(sessionId);
        // Para eclipse link
        Connection connection = getEntityManager(getEntityId(dbLinkInfo))
                .unwrap(java.sql.Connection.class);
        return connection;
    }

    /**
     *
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param conn objeto factory cuya función es devolver una conexión del
     * entity manager
     * @return devuelve una conexión del entity manager
     */
    @Override
    public Connection getConnection(String sessionId, IDBConnectFactory conn) {
        IDBLinkInfo dbLinkInfo = sessions.getDBLinkInfo(sessionId);
        EntityManager em = getEntityManager(getEntityId(dbLinkInfo));
        Connection result = conn.getConnection(em);
        return result;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private void populateQueryParameters(Query query, Map<String, Object> parameters, String queryString) {
        parameters.entrySet().forEach( entry -> {
            if (queryString != null) {
                if (Strings.findString(":" + entry.getKey(), queryString) >= 0) {
                    query.setParameter(entry.getKey(), entry.getValue());
                }
            } else {
                query.setParameter(entry.getKey(), entry.getValue());
            }
        });
    }

    /**
     * Setea valores predeterminados de ciertas constantes en los queries.
     *
     * @param persistUnit unidad de persistencia
     * @return Map con los valores de las constantes.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private Map<String, String> getQueryConstants(String persistUnit) {
        LOGGER.debug("getQueryConstansts()");
        Map<String, String> queryConstants = new HashMap<>();
        String schema = (String) this.getPersistUnitProp(persistUnit).get(DEFAULT_SCHEMA_PROPERTY);
        queryConstants.put("schema", schema);
        String schemaCat = (String) this.getPersistUnitProp(IDBManager.CATALOGO).get(DEFAULT_SCHEMA_PROPERTY);
        queryConstants.put("schemacatalog", schemaCat);

        String motordatos = (String) this.getPersistUnitProp(persistUnit).get("jbs.dbengine");

        // Asignar las constantes booleanas
        if ("POSTGRES".equals(motordatos)) {
            queryConstants.put("true", TRUE);
            queryConstants.put("false", FALSE);
        } else {
            queryConstants.put("true", "1");
            queryConstants.put("false", "0");
        }
        // Asignar las constantes de fecha y hora
        if (Fn.inList(motordatos, "SQLSERVER", "Microsoft SQL Server", "SYBASE")) {
            queryConstants.put("now", "getdate()");
            queryConstants.put("today", "CONVERT(date, GETDATE())");
        } else if (Fn.inList(motordatos, "ORACLE", "ORACLE8")) {
            queryConstants.put("now", "sysdate");
            queryConstants.put("today", "TRUNC(sysdate)");
        } else if ("POSTGRES".equals(motordatos)) {
            queryConstants.put("now", "now()");
            queryConstants.put("today", "date_trunc('day', now())");
        } else if ("DB2".equals(motordatos)) {
            queryConstants.put("now", "CURRENT_TIMESTAMP");
            queryConstants.put("today", "CURRENT DATE");
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

    /**
     * Asigna al ejb el identificador del usuario que realiza la transacción.
     *
     * @param ejb
     * @param appUser identificador del usuario.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private void setAppUser(IDataRow ejb, String appUser) {
        try{
            if (DataInfo.isFieldExist(ejb.getClass(), "appuser")) {
                ejb.setValue("appuser", appUser);
            }
        }
        catch (Exception ex){
            //
        }
    }

    /**
     * Busca y devuelve un id o clave para acceder o crear un entity manager.
     *
     * @param dbLinkInfo información necesaria para acceder a la conexión de
     * datos correcta (unidad de persistencia, sesión id etc).
     * @return id o clave para acceder o crear un entity manager.
     */
    private String getEntityId(IDBLinkInfo dbLinkInfo) {
        LOGGER.debug("getEntityId()");
        String persistUnit;
        if (dbLinkInfo == null) {
            persistUnit = IDBManager.CATALOGO;
        } else {
            persistUnit = dbLinkInfo.getPersistUnit();
            LOGGER.debug(dbLinkInfo.getPersistUnit());
        }
        String key = persistUnit.trim() + ":";
        // Si la estrategia de acceso/creación del entity manager es por thread
        if (dbManager.getEntityIdStrategic() == IDBManager.PERTHREAD) {
            Long threadId = Thread.currentThread().getId();
            key += threadId.toString();
        } else {
            //Si la estrategia de acceso/creación del entity manager es por sesión del usuario
            String sessionId = "";
            if (dbLinkInfo != null) {
                sessionId = dbLinkInfo.getSessionOrTokenId();
            }
            key += sessionId;
        }
        LOGGER.debug(key);
        return key;
    }

    /**
     * Busca y devuelve un id o clave para acceder o crear un entity manager.
     *
     * @param persistUnit unidad de persistencia
     * @return id o clave para acceder o crear un entity manager.
     */
    private String getEntityId(String persistUnit) {
        LOGGER.debug("getEntityId()");
        if (Strings.isNullorEmpty(persistUnit)) {
            persistUnit = IDBManager.CATALOGO;
        }
        return persistUnit + ":";
    }

    
    /**
     * Verifica que el valor del campo idcompany (identificador de la empresa
     * a la que esta logueada) sea válido. No puede grabar o borrar registros
     * que corresponde a una empresa que no esta logueada.
     * @param <T>
     * @param dbLinkInfo
     * @param ejb
     * @throws CompanyError 
     */
    private <T extends IDataRow> void checkFieldIdcompany(IDBLinkInfo dbLinkInfo, T ejb) throws CompanyError{
        if (dbLinkInfo == null || ejb == null){
            return;
        }
        if (ejb.getAction() == 0){
            return;
        }
        Long idcompany = dbLinkInfo.getIdCompany();
        if (nvl(idcompany, 0L) == 0L) {
            return;
        }
        // Verificar valor de idcompany
        // TODO verificar 
        if (!ejb.checkFieldIdcompany(idcompany)){
            IUserSession userSession = dbLinkInfo.getUserSession();
            boolean exito = false;
            // Si la empresa en la que esta logueada agrupa varias empresas
            if (userSession != null && userSession.getCompany() != null 
                    && !userSession.getCompany().getCompanyList().isEmpty()) {
                for (IAppCompany company : userSession.getCompany().getCompanyList()) {
                     if (ejb.checkFieldIdcompany(company.getIdcompany())){
                         exito = true;
                         break;
                     }
                }
            }
            if (!exito){
                throw new CompanyError("Valor de idcompany inválido");                
            }
        }
    }
}

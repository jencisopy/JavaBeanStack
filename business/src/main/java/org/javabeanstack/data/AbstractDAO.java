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

import org.javabeanstack.data.model.DataResult;
import org.javabeanstack.data.model.DataSet;
import org.javabeanstack.datactrl.IDataObject;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.error.IErrorInfo;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Parameter;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.data.events.IDAOEvents;

import org.javabeanstack.error.ErrorInfo;
import org.javabeanstack.exceptions.CompanyError;
import org.javabeanstack.exceptions.SessionError;
import org.javabeanstack.log.ILogManager;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.model.IAppMessage;
import org.javabeanstack.security.IOAuthConsumerData;
import org.javabeanstack.security.ISessions;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.util.Fn;
import static org.javabeanstack.util.Fn.nvl;
import org.javabeanstack.util.LocalDates;
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

    private static final Logger LOGGER = LogManager.getLogger(AbstractDAO.class);

    /**
     * Hasta dónde se sigue la cadena de identidades derivadas al decidir si un
     * valor de la clave única es transitorio. Es un tope de cortesía: un mapeo
     * real no encadena más de dos o tres niveles y así un ciclo no cuelga el
     * servidor.
     */
    private static final int MAX_NIVEL_IDENTIDAD_DERIVADA = 10;
    private static final String DEFAULT_SCHEMA_PROPERTY = "hibernate.default_schema";
    public static final String FALSE = "false";
    public static final String TRUE = "true";

    /**
     * Tipos de excepción que indican un fallo de infraestructura y no de
     * negocio (ver {@link #isInfrastructureError(Throwable)}). Se nombran por
     * texto a propósito: son tipos del servidor de aplicaciones y del proveedor
     * de persistencia, y esta clase no tiene por qué depender de ellos para
     * poder clasificar un error.
     *
     * <p>Dos tipos quedaron <b>deliberadamente afuera</b>, porque los dos
     * pueden transportar un mensaje escrito para el usuario:</p>
     * <ul>
     * <li>el error JDBC <i>sin clasificar</i> del proveedor de persistencia: es
     * el cajón de sastre al que va a parar, entre otras cosas, el mensaje que
     * levanta un <i>trigger</i> de la base (error definido por el usuario), que
     * suele ser una regla de negocio redactada para quien opera;</li>
     * <li>la excepción con que el contenedor envuelve lo que lanza un bean
     * llamado dentro de la misma transacción: adentro puede venir tanto un
     * fallo técnico como una validación del sistema, porque las excepciones de
     * validación no están anotadas como excepciones de aplicación.</li>
     * </ul>
     */
    private static final String[] INFRASTRUCTURE_ERROR_TYPES = new String[]{
        "jakarta.resource.ResourceException",
        "jakarta.persistence.TransactionRequiredException",
        "jakarta.transaction.RollbackException",
        "jakarta.transaction.SystemException",
        "org.hibernate.AssertionFailure",
        "org.hibernate.TransactionException",
        "org.hibernate.exception.JDBCConnectionException",
        "java.sql.SQLNonTransientConnectionException",
        "java.sql.SQLTransientConnectionException",
        "java.sql.SQLRecoverableException"
    };

    /**
     * Textos que delatan un fallo de infraestructura llegado como excepción
     * genérica del JDK, sin un tipo propio por el cual reconocerlo.
     */
    private static final String[] INFRASTRUCTURE_ERROR_MESSAGES = new String[]{
        "STATUS_MARKED_ROLLBACK",
        "EntityManager is closed",
        "Session is closed"
    };

    @Resource
    TransactionSynchronizationRegistry tsr;

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

    /**
     * Administrador del log: registra los errores y resuelve los textos de los
     * mensajes.
     */
    @EJB
    private ILogManager logMngr;

    /**
     * Auditoría de las grabaciones (plan AUDMGR): decide si una entidad se
     * audita y escribe su fila. Vive afuera del DAO a propósito: la decisión
     * está anotada {@code NOT_SUPPORTED} y eso solo surte efecto si la llamada
     * cruza el proxy del contenedor, cosa que una invocación entre métodos de
     * esta misma clase no hace (ver {@link IAuditManager}).
     */
    @EJB
    private IAuditManager auditManager;

    /**
     * Constructor por defecto.
     */
    public AbstractDAO() {
    }

    /**
     * Interceptor que cierra los EntityManager dinámicos al finalizar la
     * invocación del método de negocio.
     *
     * @param ctx contexto de la invocación interceptada.
     * @return el resultado de la invocación.
     * @throws Exception si la invocación o el cierre fallan.
     */
    @AroundInvoke
    protected Object closeDynamicEntityManagers(InvocationContext ctx) throws Exception {
        try {
            return ctx.proceed();
        } finally {
            if (dbManager != null) {
                dbManager.closeEntityManagers();
            }
        }
    }

    /**
     * Devuelve el objeto DBLinkInfo(info de la conexión) a partir del sessionid o token
     * @param sessionId identificador de la sesión o token
     * @return objeto DBLinkInfo(info de la conexión) a partir del sessionid o token
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public IDBLinkInfo getDBLinkInfo(String sessionId) {
        IDBLinkInfo dbLinkInfo = sessions.getDBLinkInfo(sessionId);
        return dbLinkInfo;
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
     * Crea una consulta a partir de la sentencia y sus parámetros, en el
     * contexto de la sesión indicada.
     *
     * @param sessionId identificador de la sesión del usuario.
     * @param queryString sentencia de la consulta.
     * @param parameters parámetros nombrados de la consulta.
     * @return consulta lista para ejecutarse.
     */
    protected final Query createQuery(String sessionId, String queryString, Map<String, Object> parameters) {
        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
        EntityManager em = getEntityManager(getEntityManagerId(dbLinkInfo));

        queryString = replaceBooleanParams(queryString);
        parameters = addQueryParams(queryString, parameters);
        Query query = em.createQuery(queryString);
        if (parameters != null && !parameters.isEmpty()) {
            populateQueryParameters(query, parameters, queryString);
        }
        return query;
    }

    /**
     * Reemplaza la convención :true/:false por los literales del lenguaje.
     * Hibernate 6+ no admite 'true' y 'false' como nombres de parámetros
     * en sentencias JPQL (son palabras reservadas); en las sentencias
     * nativas la convención sigue vigente.
     *
     * @param queryString sentencia jpql
     * @return sentencia con los literales reemplazados.
     */
    protected final String replaceBooleanParams(String queryString) {
        if (queryString == null) {
            return null;
        }
        return queryString.replaceAll("(?i):true\\b", "true")
                .replaceAll("(?i):false\\b", "false");
    }

    /**
     * Agrega a los parámetros los valores de las constantes que aparezcan en la
     * sentencia (:true, :false, :today, :now).
     *
     * @param queryString sentencia
     * @param parameters objeto parametros
     * @return parametros con los valores adicionales.
     */
    protected final Map<String, Object> addQueryParams(String queryString, Map<String, Object> parameters) {
        queryString = Fn.nvl(queryString, "");
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        if (Strings.findString(":true", queryString.toLowerCase()) >= 0) {
            parameters.put("true", true);
        }
        if (Strings.findString(":false", queryString.toLowerCase()) >= 0) {
            parameters.put("false", false);
        }
        if (Strings.findString(":today", queryString.toLowerCase()) >= 0) {
            parameters.put("today", LocalDates.today());
        }
        if (Strings.findString(":now", queryString.toLowerCase()) >= 0) {
            parameters.put("now", LocalDates.now());
        }
        return parameters;
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

        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
        EntityManager em = getEntityManager(getEntityManagerId(dbLinkInfo));

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
        if (id == null) {
            return null;
        }
        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
        EntityManager em = getEntityManager(getEntityManagerId(dbLinkInfo));
        T row = em.find(entityClass, id);
        if (row == null) {
            return null;
        }
        // Si no hay transacción activada
        if (Status.STATUS_NO_TRANSACTION == tsr.getTransactionStatus()) {
            return row;
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
     * <p>Si alguno de los valores de la clave única es nulo o es una entidad
     * todavía sin identificador (un detalle cuyo padre aún no se grabó, en un
     * alta maestro-detalle) devuelve nulo <b>sin consultar</b>: el resultado es
     * el mismo —una comparación con nulo nunca encuentra coincidencia— y se
     * evita la {@code TransientObjectException} que Hibernate lanza al ligar
     * una entidad transitoria como parámetro (plan {@code QUERYUK}, RF5).
     * {@code GenericDAO} de Oym-frame no sobrescribe este método, de modo que
     * la regla vale para toda la línea.</p>
     *
     * <p>Si la base ya tiene filas duplicadas para esa clave devuelve una de
     * ellas y deja una advertencia en el log, en vez de propagar la
     * {@code NonUniqueResultException} que lanzaba {@code getSingleResult}
     * (plan {@code QUERYUK}, hallazgo {@code M0-02}): esa excepción la tragaba
     * {@code AbstractDataService.checkUniqueKey} y el control de duplicados
     * terminaba siendo <b>menos</b> estricto cuanto peor estaba la base —con
     * una fila repetida rechazaba, con dos aprobaba—. Cuál de las filas se
     * devuelve lo decide {@link #pickUkMatch(List, IDataRow)}.</p>
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
        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
        // Buscar registro por la clave unica
        T row;
        Query q = getEntityManager(getEntityManagerId(dbLinkInfo)).createQuery(ejb.getQueryUK());

        for (Parameter param : q.getParameters()) {
            Object value = ejb.getValue(param.getName());
            if (isUnresolvableUkValue(value)) {
                LOGGER.debug("findByUk {}: {} sin valor o transitorio, no hay coincidencia posible",
                        ejb.getClass().getSimpleName(), param.getName());
                return null;
            }
            q.setParameter(param, value);
        }
        try {
            //setMaxResults(2): alcanza para saber si hay más de una coincidencia
            //y evita traer la tabla entera si la base está muy sucia (M0-02).
            List<T> rows = q.setMaxResults(2).getResultList();
            row = pickUkMatch(rows, ejb);
            if (row == null) {
                return null;
            }
            // Si no hay transacción activada
            if (Status.STATUS_NO_TRANSACTION == tsr.getTransactionStatus()) {
                return row;
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
        } catch (NoResultException exp) {
            row = null;
        }
        return row;
    }

    /**
     * Elige qué fila devuelve {@link #findByUk(String, IDataRow)} entre las
     * coincidencias de la clave única.
     *
     * <p>Sin coincidencias devuelve nulo; con una, esa. Con más de una la base
     * tiene duplicados que su clave única física tendría que haber impedido:
     * se deja una advertencia en el log y se devuelve una fila <b>distinta</b>
     * de la que se está validando, si la hay, para que
     * {@code checkUniqueKey} rechace también la modificación de una de las
     * filas repetidas. Si la fila validada todavía no tiene identificador (un
     * alta) cualquiera sirve y se devuelve la primera.</p>
     *
     * <p>No es el caso de {@code I3-30} —resultado del DAO ignorado dentro de
     * una transacción que el contenedor deshace—: acá no hay marca de
     * deshacer, lo que había era una validación que tragaba una excepción y
     * aprobaba. Son familias hermanas con mecanismos distintos (plan
     * {@code QUERYUK}, D20).</p>
     *
     * @param <T>
     * @param rows filas que coinciden con la clave única.
     * @param ejb fila que se está validando.
     * @return la fila elegida, nulo si no hubo coincidencias.
     */
    protected static <T extends IDataRow> T pickUkMatch(List<T> rows, IDataRow ejb) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        if (rows.size() > 1) {
            LOGGER.warn("findByUk {}: la clave única devuelve más de una fila, "
                    + "la base tiene duplicados ({})",
                    ejb.getClass().getSimpleName(), ejb.getQueryUK());
            Object id = ejb.getId();
            if (id != null) {
                for (T otra : rows) {
                    if (!id.equals(otra.getId())) {
                        return otra;
                    }
                }
            }
        }
        return rows.get(0);
    }

    /**
     * Determina si un valor de la clave única hace inútil la consulta.
     *
     * <p>Lo es cuando el valor es nulo —ninguna fila coincide con nulo— o
     * cuando es una entidad todavía sin identificador, porque ligar una
     * entidad transitoria como parámetro hace que Hibernate lance
     * {@code TransientObjectException} (plan {@code QUERYUK}, RF5).</p>
     *
     * <p>Lo de «todavía sin identificador» solo se afirma cuando la clase
     * declara su campo clave: en un proxy perezoso o en una entidad con
     * {@code @EmbeddedId}, {@code getId()} devuelve nulo aunque el registro
     * exista, y dar esos casos por transitorios dejaría el control de
     * duplicados inerte en silencio. Ante la duda se consulta, que es lo que
     * se hacía antes.</p>
     *
     * <p>Con <b>identidad derivada</b> —el {@code @Id} puesto sobre una
     * asociación, como en {@code Ctacteretencion}— el identificador es a su vez
     * una entidad y casi nunca es nulo, porque el atributo viene inicializado
     * con {@code new}: en ese caso la pregunta se repite sobre el padre. Hizo
     * falta al aplicar D16, que puso justamente una de esas entidades como
     * parámetro de la clave única de {@code Ctacteretenciondetalle}.</p>
     *
     * @param value valor de uno de los campos de la clave única.
     * @return verdadero si no tiene sentido consultar con ese valor.
     */
    protected static boolean isUnresolvableUkValue(Object value) {
        return isUnresolvableUkValue(value, 0);
    }

    /**
     * Resuelve {@link #isUnresolvableUkValue(Object)} siguiendo la cadena de
     * identidades derivadas.
     *
     * @param value valor de uno de los campos de la clave única.
     * @param nivel profundidad alcanzada, para no caer en un ciclo de mapeo.
     * @return verdadero si no tiene sentido consultar con ese valor.
     */
    private static boolean isUnresolvableUkValue(Object value, int nivel) {
        if (value == null) {
            return true;
        }
        if (value instanceof IDataRow) {
            IDataRow row = (IDataRow) value;
            if (DataInfo.getIdFieldName(row.getClass()) == null) {
                return false;
            }
            Object id = row.getId();
            if (id == null) {
                return true;
            }
            //Identidad derivada: el identificador es a su vez una entidad
            //(el @Id está sobre una asociación, como en Ctacteretencion). Su
            //getId() nunca devuelve nulo —el atributo suele venir inicializado
            //con new—, así que hay que mirar el identificador del padre o se
            //liga igual una entidad transitoria y vuelve la
            //TransientObjectException que RF5 vino a evitar.
            if (id instanceof IDataRow && nivel < MAX_NIVEL_IDENTIDAD_DERIVADA) {
                return isUnresolvableUkValue(id, nivel + 1);
            }
        }
        return false;
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
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
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
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
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
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public <T extends IDataRow> List<T> find(Class<T> entityClass, String sessionId, String order, String filter, Map<String, Object> params, int first, int max) throws Exception {
        String query = "select o from " + entityClass.getSimpleName() + " o ";
        if (filter == null) {
            filter = "";
        }
        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
        T entity = entityClass.getConstructor().newInstance();
        // Si se va a aplicar el filtro por defecto 
        if (entity.isApplyDBFilter()) {
            String operator = (filter.isEmpty() ? "" : " and ");
            IDBFilter dbFilter = dbLinkInfo.getDBFilter();
            if (dbFilter != null) {
                String dbFilterExpr = dbFilter.getFilterExpr(entityClass, "");
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
     * Devuelve un registro a travéz de su clave unica.
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

        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
        EntityManager em = getEntityManager(getEntityManagerId(dbLinkInfo));

        queryString = replaceBooleanParams(queryString);
        Query query = em.createQuery(queryString);
        if (parameters != null && !parameters.isEmpty()) {
            populateQueryParameters(query, parameters, queryString);
        }
        T result;
        try {
            result = (T) query.getSingleResult();
            // Si no hay transacción activada
            if (Status.STATUS_NO_TRANSACTION == tsr.getTransactionStatus()) {
                return result;
            }
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
        } catch (Exception e) {
            ErrorManager.showError(e, LOGGER, logMngr, sessionId);
            throw e;
        }
        LOGGER.debug("-RESULT-");
        LOGGER.debug(result);
        return result;
    }

    /**
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
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
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
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

        Query query = createQuery(sessionId, queryString, parameters);
        if (max > 0) {
            query.setFirstResult(first);
            query.setMaxResults(max);
        }
        return (List<T>) query.getResultList();
    }

    /**
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
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
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
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
        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
        EntityManager em = getEntityManager(getEntityManagerId(dbLinkInfo));

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
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
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
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
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
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
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

        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
        EntityManager em = getEntityManager(getEntityManagerId(dbLinkInfo));

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
     * Devuelve una lista de objetos conteniendo los registros de la tabla
     * solicitada
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

        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
        String persistUnit;
        if (dbLinkInfo != null) {
            persistUnit = dbLinkInfo.getPersistUnit();
        } else {
            persistUnit = IDBManager.CATALOGO;
        }
        queryString = Strings.textMerge(queryString, getQueryConstants(persistUnit));
        LOGGER.debug(queryString);

        EntityManager em = getEntityManager(getEntityManagerId(dbLinkInfo));

        Query query = em.createNativeQuery(queryString);
        if (parameters != null && !parameters.isEmpty()) {
            populateQueryParameters(query, parameters, queryString);
        }
        return query.getResultList();
    }

    /**
     * Selecciona datos de la base de datos a travez de una instrucción sql nativa
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

        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
        String persistUnit;
        if (dbLinkInfo != null) {
            persistUnit = dbLinkInfo.getPersistUnit();
        } else {
            persistUnit = IDBManager.CATALOGO;
        }

        queryString = Strings.textMerge(queryString, getQueryConstants(persistUnit));
        LOGGER.debug(queryString);

        EntityManager em = getEntityManager(getEntityManagerId(dbLinkInfo));

        Query query = em.createNativeQuery(queryString);
        if (parameters != null && !parameters.isEmpty()) {
            populateQueryParameters(query, parameters, queryString);
        }
        query.setFirstResult(first);
        query.setMaxResults(max);
        return query.getResultList();
    }

    /**
     * Selecciona datos de la base de datos a travez de una instrucción sql nativa
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

        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
        String persistUnit;
        if (dbLinkInfo != null) {
            persistUnit = dbLinkInfo.getPersistUnit();
        } else {
            persistUnit = IDBManager.CATALOGO;
        }

        queryString = Strings.textMerge(queryString, getQueryConstants(persistUnit));
        LOGGER.debug(queryString);

        EntityManager em = getEntityManager(getEntityManagerId(dbLinkInfo));

        Query query = em.createNativeQuery(queryString, clazz);
        if (parameters != null && !parameters.isEmpty()) {
            populateQueryParameters(query, parameters, queryString);
        }
        query.setFirstResult(first);
        query.setMaxResults(max);
        return query.getResultList();
    }

    /**
     * Ejecuta una sentencia (select, insert, update, remove) sobre la base de
     * datos
     *
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param sqlString sentencia sql
     * @param parameters parámetros de la sentencia.
     * @return un objeto error si no se ejecuto la sentencia con exito
     */
    @Override
    public IErrorInfo sqlExec(String sessionId, String sqlString,
            Map<String, Object> parameters) throws Exception {
        LOGGER.debug(Strings.replicate("-", 50));
        LOGGER.debug("sqlExec");

        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
        String persistUnit;
        if (dbLinkInfo == null) {
            persistUnit = IDBManager.CATALOGO;
        } else {
            persistUnit = dbLinkInfo.getPersistUnit();
        }
        sqlString = Strings.textMerge(sqlString, getQueryConstants(persistUnit));
        LOGGER.debug(sqlString);

        EntityManager em = getEntityManager(getEntityManagerId(dbLinkInfo));
        ErrorInfo error = new ErrorInfo();
        try {
            Query sql = em.createNativeQuery(sqlString);
            if (parameters != null && !parameters.isEmpty()) {
                populateQueryParameters(sql, parameters, sqlString);
            }
            sql.executeUpdate();
        } catch (Exception e) {
            error.setException(e);
            error.setMessage(e.getLocalizedMessage());
            error.setErrorNumber(1);
        }
        return error;
    }

    /**
     * Ejecuta un procedimiento almacenado
     *
     * @param sessionId identificador de la sesión del usuario
     * @param procedureName procedimiento almacenado
     * @param params parámetros de la sentencia.
     * @throws java.lang.Exception
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Override
    public void execSqlProcedure(String sessionId, String procedureName, Map<String, Object> params) throws Exception {
        LOGGER.debug(Strings.replicate("-", 50));
        LOGGER.debug("execSqlProcedure");

        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
        String persistUnit;
        if (dbLinkInfo == null) {
            persistUnit = IDBManager.CATALOGO;
        } else {
            persistUnit = dbLinkInfo.getPersistUnit();
        }
        procedureName = Strings.textMerge(procedureName, getQueryConstants(persistUnit));
        EntityManager em = getEntityManager(getEntityManagerId(dbLinkInfo));

        StoredProcedureQuery procedure = em.createStoredProcedureQuery(procedureName);
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                procedure.registerStoredProcedureParameter(entry.getKey(), entry.getValue().getClass(), ParameterMode.IN);
                procedure.setParameter(entry.getKey(), entry.getValue());
            }
        }
        procedure.execute();
    }

    /**
     * Ejecuta una sentencia (select, insert, update, remove) sobre la base de
     * datos
     *
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param sqlString sentencia jpql (update, delete)
     * @param parameters parámetros de la sentencia.
     * @return un objeto error si no se ejecuto la sentencia con exito
     */
    @Override
    public IErrorInfo jpqlExec(String sessionId, String sqlString,
            Map<String, Object> parameters) throws Exception {
        LOGGER.debug(Strings.replicate("-", 50));
        LOGGER.debug("jpqlExec");

        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
        String persistUnit;
        if (dbLinkInfo == null) {
            persistUnit = IDBManager.CATALOGO;
        } else {
            persistUnit = dbLinkInfo.getPersistUnit();
        }
        sqlString = Strings.textMerge(sqlString, getQueryConstants(persistUnit));
        LOGGER.debug(sqlString);

        ErrorInfo error = new ErrorInfo();
        try {
            Query jpql = createQuery(sessionId, sqlString, parameters);
            jpql.executeUpdate();
        } catch (Exception e) {
            error.setException(e);
            error.setMessage(e.getLocalizedMessage());
            error.setErrorNumber(1);
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
    public <T extends IDataRow> IDataResult save(String sessionId, T ejb) {
        List<T> ejbs = new ArrayList<>();
        ejbs.add(ejb);
        return save(sessionId, ejbs);
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
    public IDataResult save(String sessionId, IDataObject ejbs) {
        return save(sessionId, ejbs.getDataRows());
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
    public <T extends IDataRow> IDataResult save(String sessionId, List<T> ejbs) {
        IDataSet dataSet = new DataSet();
        String setKey = "1";
        if (ejbs.get(0) != null) {
            setKey = ejbs.get(0).getClass().getSimpleName();
        }
        dataSet.add(setKey, (List<IDataRow>) ejbs);
        return save(sessionId, dataSet);
    }

    /**
     * Sincroniza una lista de ejbs con la base de datos.
     *
     * <p>
     * Las filas marcadas {@link IDataRow#DELETE} se borran con
     * {@link #removeEjb(EntityManager, IDataRow)}. Quien envíe un padre y una
     * de sus hijas a borrar en el mismo set tiene que <b>sacar la hija de la
     * colección del padre</b> antes de grabar: mientras la hija siga dentro de
     * una colección con cascada {@code PERSIST} de una entidad administrada, el
     * flush deshace su borrado (ver el porqué en
     * {@link #removeEjb(EntityManager, IDataRow)}) y la grabación termina en
     * error.
     * </p>
     *
     * @param dataSet set de objetos mapeados a los registros de una tabla.
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @return Devuelve un objeto con el resultado de la grabación
     */
    @Override
    public IDataResult save(String sessionId, IDataSet dataSet) {
        if (dataSet == null || dataSet.size() == 0) {
            return null;
        }
        String appUser;
        boolean auditable;
        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
        appUser = dbLinkInfo.getAppUserSeal();
        IDataResult dataResult = new DataResult();
        IDataRow lastEjb = null;
        EntityManager em = getEntityManager(getEntityManagerId(dbLinkInfo));

        // Recorrer los objetos a actualizar en la base
        for (Map.Entry<String, List<? extends IDataRow>> entry : dataSet.getMapListSet().entrySet()) {
            List<? extends IDataRow> ejbs = entry.getValue();
            List<IDataRow> ejbsRes = new ArrayList();
            try {
                IDAOEvents event = dataSet.getEvent(entry.getKey());
                for (IDataRow ejb : ejbs) {
                    //Ejecutar evento pre grabación
                    if (event != null) {
                        event.beforeSave(sessionId, ejb);
                    }
                    auditable = ejb.isAuditAble();
                    lastEjb = ejb;
                    switch (ejb.getAction()) {
                        case IDataRow.INSERT:
                            setAppUser(ejb, appUser);
                            checkFieldIdcompany(dbLinkInfo, ejb);
                            if (ejb.getPersistMode() == IDataRow.PERSIST) {
                                em.persist(ejb);
                            } else {
                                ejb = em.merge(ejb);
                            }
                            em.flush();
                            dataResult.setRowUpdated(ejb);
                            ejb.setAction(IDataRow.INSERT); //Pierde el action al hacer el merge
                            ejbsRes.add(ejb);
                            break;
                        case IDataRow.UPDATE:
                            setAppUser(ejb, appUser);
                            checkFieldIdcompany(dbLinkInfo, ejb);
                            ejb = em.merge(ejb);
                            em.flush();
                            dataResult.setRowUpdated(ejb);
                            ejb.setAction(IDataRow.UPDATE); //Pierde el action al hacer el merge
                            ejbsRes.add(ejb);
                            break;
                        case IDataRow.DELETE:
                            checkFieldIdcompany(dbLinkInfo, ejb);
                            removeEjb(em, ejb);
                            dataResult.setRowUpdated(ejb);
                            ejbsRes.add(ejb);
                            break;
                        default:
                            break;
                    }
                    ejb.setErrors((Map<String, IErrorInfo>) null);
                    //Ejecutar evento post grabación
                    if (event != null) {
                        event.afterSave(sessionId, ejb);
                    }
                    //Auditoria
                    auditSave(em, sessionId, ejb, auditable);
                }
                for (IDataRow ejb : ejbsRes) {
                    if (ejb.getAction() != IDataRow.DELETE) {
                        ejb.setAction(0);
                    }
                }
                dataResult.put(entry.getKey(), ejbsRes);
            } catch (Exception e) {
                dataResult.setRowUpdated(null);
                //El texto que se devuelve distingue el fallo de negocio del de
                //infraestructura (ver getErrorMsgToShow). La excepción entera
                //queda en el resultado y va al log: no se pierde nada.
                String msgError = getErrorMsgToShow(e);
                if (lastEjb != null) {
                    lastEjb.setErrors(msgError, "", 0);
                }
                dataResult.put(entry.getKey(), ejbsRes);
                dataResult.setSuccess(false);
                dataResult.setErrorMsg(msgError);
                dataResult.setException(e);
                dbRollBack();
                ErrorManager.showError(e, LOGGER, logMngr, sessionId);
                break;
            }
        }
        return dataResult;
    }

    /**
     * Mensaje del fallo de una grabación, tal como se le va a mostrar a quien
     * opera el sistema.
     *
     * <p>
     * Los fallos no son todos iguales y no se informan igual. El de
     * <b>negocio</b> —el que lanza una regla del sistema desde los eventos de
     * grabación o una validación— trae un texto escrito para el usuario y se
     * devuelve tal cual, que es lo que se viene haciendo. El de
     * <b>infraestructura</b> —la transacción marcada para deshacerse, la
     * conexión que no se puede tomar, el contexto de persistencia envenenado—
     * trae un volcado técnico que al usuario no le dice nada y que encima
     * arrastra la sentencia completa; en ese caso se devuelve un texto fijo y
     * accionable.
     * </p>
     *
     * <p>
     * No se pierde información: la excepción entera va igual al log del
     * servidor y queda en el resultado de la grabación
     * ({@link IDataResult#getException()}), de modo que quien tenga que
     * diagnosticar la tiene completa. Lo único que cambia es que deja de
     * mostrarse donde no sirve. El texto lleva la fecha y hora para poder
     * ubicar en el log la línea que le corresponde.
     * </p>
     *
     * @param exp excepción que cortó la grabación.
     * @return mensaje a mostrar.
     */
    protected String getErrorMsgToShow(Exception exp) {
        if (exp == null) {
            return "";
        }
        if (!isInfrastructureError(exp)) {
            return ErrorManager.getStackCause(exp);
        }
        return "No se pudo completar la operación por un problema técnico. "
                + "Reintente y, si vuelve a ocurrir, avise a soporte indicando la fecha "
                + "y hora (" + LocalDates.toString(LocalDates.now(), "dd/MM/yyyy HH:mm:ss")
                + "). El detalle quedó registrado en el log del servidor.";
    }

    /**
     * Determina si un fallo es de infraestructura y no de negocio.
     *
     * <p>
     * Se recorre la cadena de causas y se la compara contra una lista de tipos
     * conocidos; lo que no está en la lista <b>conserva</b> el tratamiento de
     * siempre (se muestra el mensaje de la excepción). Es deliberado que la
     * lista sea corta y positiva: una violación de integridad —clave duplicada,
     * clave foránea, restricción de verificación— no entra, porque su mensaje
     * dice algo que quien opera puede entender y corregir; una fila que no está
     * o que cambió de versión, tampoco; y el mensaje que levanta un
     * <i>trigger</i> de la base, menos todavía, porque es una regla de negocio
     * escrita para quien opera.
     * </p>
     *
     * <p>
     * Los dos nombres que se buscan por texto y no por tipo son el de la
     * transacción ya marcada para deshacerse y el del contexto de persistencia
     * cerrado: los dos llegan como excepciones genéricas del JDK, imposibles de
     * distinguir por su clase de las que lanza la aplicación.
     * </p>
     *
     * @param exp excepción a clasificar.
     * @return verdadero si el fallo es de infraestructura.
     */
    protected boolean isInfrastructureError(Throwable exp) {
        Throwable causa = exp;
        //Un tope por si alguna excepción llega con una cadena de causas cíclica.
        int vueltas = 0;
        while (causa != null && vueltas < 50) {
            if (isInfrastructureType(causa.getClass())
                    || isInfrastructureMessage(causa.getMessage())) {
                return true;
            }
            causa = causa.getCause();
            vueltas++;
        }
        return false;
    }

    /**
     * Determina si el tipo de la excepción es uno de los de infraestructura.
     *
     * <p>
     * Se compara por nombre y subiendo por las clases padre, de manera que las
     * especializaciones entran sin enumerarlas y sin que esta clase tenga que
     * importar los tipos del servidor de aplicaciones ni del proveedor de
     * persistencia.
     * </p>
     *
     * @param clase clase de la excepción.
     * @return verdadero si es un tipo de infraestructura.
     */
    private boolean isInfrastructureType(Class<?> clase) {
        for (Class<?> tipo = clase; tipo != null; tipo = tipo.getSuperclass()) {
            for (String nombre : INFRASTRUCTURE_ERROR_TYPES) {
                if (nombre.equals(tipo.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determina si el mensaje de la excepción es de los de infraestructura.
     *
     * @param mensaje mensaje de la excepción.
     * @return verdadero si el mensaje corresponde a un fallo de infraestructura.
     */
    private boolean isInfrastructureMessage(String mensaje) {
        if (mensaje == null) {
            return false;
        }
        for (String texto : INFRASTRUCTURE_ERROR_MESSAGES) {
            if (mensaje.contains(texto)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Borra de la base la fila que representa {@code row}.
     *
     * <p>
     * La fila llega <b>desprendida</b> (fue leída en otra transacción), así que
     * para borrarla hay que traerla al contexto de persistencia. Lo que no se
     * puede hacer es traerla con {@code merge}: Hibernate resuelve lo que
     * mergea con su perfil interno de fetch "merge", que inicializa toda
     * asociación con cascada {@code MERGE} que encuentre en el camino. Con eso,
     * al mergear una hija entra al contexto también su padre <b>con la
     * colección de hermanas cargada</b>; y en el flush la cascada
     * {@code PERSIST} desde ese padre administrado vuelve a dejar administrada
     * a la hija recién borrada (JPA 3.2, 3.2.4: a toda entidad alcanzable por
     * una relación con cascada {@code PERSIST} desde una administrada se le
     * aplica {@code persist}), con lo cual el {@code DELETE} no se emite, y sin
     * error ni aviso.
     * </p>
     *
     * <p>
     * Por eso acá la fila se busca por su clave con {@code find}, que usa el
     * plan de carga normal y no ese perfil, y se borra la instancia
     * administrada que devuelve. La clave la resuelve
     * {@link jakarta.persistence.PersistenceUnitUtil#getIdentifier(Object)},
     * que es la vía estándar y sirve también para las claves compuestas.
     * </p>
     *
     * <p>
     * Si después del flush la fila sigue administrada es que un padre
     * administrado la conserva en una colección con cascada {@code PERSIST}:
     * borrarla es imposible por definición mientras siga ahí, y se informa el
     * error en lugar de devolver éxito. El llamador tiene que quitar la fila de
     * la colección del padre (o no enviar al padre con esa colección) antes de
     * borrarla.
     * </p>
     *
     * @param em unidad de persistencia donde está la fila.
     * @param row fila a borrar, marcada {@link IDataRow#DELETE}.
     * @throws EntityNotFoundException si la fila no existe en la base.
     * @throws PersistenceException si el flush deshizo el borrado.
     */
    protected void removeEjb(EntityManager em, IDataRow row) {
        Object id = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(row);
        String fila = row.getClass().getSimpleName() + " " + id;
        IDataRow managed = em.find(row.getClass(), id);
        if (managed == null) {
            throw new EntityNotFoundException("No existe la fila a borrar: " + fila);
        }
        em.remove(managed);
        em.flush();
        if (em.contains(managed)) {
            throw new PersistenceException("No se borro la fila " + fila
                    + ": sigue dentro de una coleccion con cascada PERSIST de otra entidad"
                    + " administrada, que en el flush deshace el borrado."
                    + " Quitar la fila de la coleccion del padre antes de borrarla.");
        }
    }

    /**
     * Deshace (rollback) la transacción activa de la unidad de persistencia.
     */
    @Override
    public void dbRollBack() {
        try {
            dbManager.rollBack();
        } catch (Exception exp) {
            LOGGER.debug("Rollback error");
        }
    }

    /**
     * Registra en la tabla de auditoría el cambio realizado sobre la entidad,
     * si se audita. Delega en {@link IAuditManager}: la decisión cruza su
     * proxy (fuera de la transacción) y la escritura usa este
     * {@code EntityManager}, en la misma transacción que la grabación.
     *
     * <p>Se conserva como envoltorio porque las subclases que copian el bucle
     * de {@link #save(String, IDataSet)} lo llaman (plan AUDMGR, D3).</p>
     *
     * @param <T> tipo de la entidad.
     * @param em entity manager activo.
     * @param sessionId identificador de la sesión del usuario.
     * @param ejb entidad auditada.
     * @param auditAble verdadero si ya se sabe que la entidad se audita (por
     * ejemplo, porque lo fijó el servicio con {@code setAuditAble}).
     * @throws Exception si falla el registro de auditoría.
     */
    protected final <T extends IDataRow> void auditSave(EntityManager em, String sessionId, T ejb, boolean auditAble) throws Exception {
        if (!auditAble && !auditManager.isAuditAble(ejb)) {
            return;
        }
        auditManager.write(em, sessionId, ejb);
    }

    /**
     * Indica si la entidad es auditable. Delega en {@link IAuditManager}.
     *
     * @param <T> tipo de la entidad.
     * @param ejb entidad.
     * @return verdadero si es auditable, falso si no.
     */
    @Override
    public <T extends IDataRow> boolean isAuditAble(T ejb) {
        return auditManager.isAuditAble(ejb);
    }

    /**
     * Indica si la entidad (por su clase) es auditable. Delega en
     * {@link IAuditManager}.
     *
     * @param <T> tipo de la entidad.
     * @param clazz clase de la entidad.
     * @return verdadero si es auditable, falso si no.
     */
    @Override
    public <T extends IDataRow> boolean isAuditAble(Class<IDataRow> clazz) {
        return auditManager.isAuditAble(clazz);
    }

    /**
     * Indica si la entidad (por nombre) es auditable. Delega en
     * {@link IAuditManager}.
     *
     * @param <T> tipo de la entidad.
     * @param entityName nombre de la entidad.
     * @return verdadero si es auditable, falso si no.
     */
    @Override
    public <T extends IDataRow> boolean isAuditAble(String entityName) {
        return auditManager.isAuditAble(entityName);
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
        return save(sessionId, ejb);
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
        return save(sessionId, ejb);
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
        return save(sessionId, ejb);
    }

    @Override
    public <T extends IDataRow> List<T> getData(String sessionId,
            String queryString,
            int maxRows,
            boolean noCache) throws Exception {
        if (maxRows == 0) {
            return new ArrayList<>();
        }
        Query q;
        q = createQuery(sessionId, queryString, null);

        if (maxRows >= 0) {
            q.setMaxResults(maxRows);
        }
        return (List<T>) q.getResultList();
    }

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
        Object id = row.getId();
        if (id == null) {
            return row;
        }
        if (id instanceof DataRow) {
            id = ((DataRow) id).getId();
        }
        row = this.findById((Class<T>) row.getClass(), sessionId, id);
        if (row == null) {
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
        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
        EntityManager em = getEntityManager(getEntityManagerId(dbLinkInfo));
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
        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
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

        Query query = createQuery(sessionId, queryString, parameters);
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
        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
        String persistUnit = dbLinkInfo.getPersistUnit();
        queryString = Strings.textMerge(queryString, getQueryConstants(persistUnit));
        queryString = "select count(*) from (" + queryString + ") x";
        LOGGER.debug(queryString);

        List<Object> result = this.findByNativeQuery(sessionId, queryString, parameters);
        return Long.valueOf(result.get(0).toString());
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
            result2.entrySet().forEach(e -> {
                Object object = e.getValue().toString();
                result.put(e.getKey(), object);
            });
        } catch (Exception e) {
            ErrorManager.showError(e, LOGGER, logMngr, null);
        }
        return result;
    }

    /**
     * Devuelve el nombre del motor de la base de datos.
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
     * Devuelve el schema de datos
     *
     * @param persistentUnit unidad de persistencia
     * @return devuelve el schema
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String getSchema(String persistentUnit) {
        return (String) getPersistUnitProp(persistentUnit).get(DEFAULT_SCHEMA_PROPERTY);
    }

    /**
     * Devuelve una conexión a la base solo funciona en un ambiente @local no
     * funciona en @Remote
     *
     * @param sessionId identificador de la sesión que permite realizar las
     * @return una conexión a la base solo funciona en un ambiente @local no
     * funciona en @Remote
     */
    @Override
    public Connection getConnection(String sessionId) {
        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
        // Para eclipse link
        Connection connection = getEntityManager(getEntityManagerId(dbLinkInfo))
                .unwrap(java.sql.Connection.class);
        return connection;
    }

    /**
     * Devuelve una conexión del entity manager, solo funciona en un ambiente
     * local no funciona en @Remote
     *
     * @param sessionId identificador de la sesión que permite realizar las
     * operaciones
     * @param conn objeto factory cuya función es devolver una conexión del
     * entity manager
     * @return devuelve una conexión del entity manager
     */
    @Override
    public Connection getConnection(String sessionId, IDBConnectFactory conn) {
        IDBLinkInfo dbLinkInfo = getDBLinkInfo(sessionId);
        EntityManager em = getEntityManager(getEntityManagerId(dbLinkInfo));
        Connection result = conn.getConnection(em);
        return result;
    }

    /**
     * Asigna a la consulta los valores de los parámetros según la sentencia.
     *
     * @param query consulta a la que asignar los parámetros.
     * @param parameters parámetros nombrados.
     * @param queryString sentencia de la consulta.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    protected final void populateQueryParameters(Query query, Map<String, Object> parameters, String queryString) {
        parameters.entrySet().forEach(entry -> {
            try {
                if (queryString != null) {
                    //El parámetro tiene que aparecer en la sentencia con su nombre
                    //COMPLETO: una búsqueda por subcadena daba por presente
                    //":idctacte" en "fn_idctacteruta(:idctacteruta,...)" e
                    //intentaba asignarlo, y Hibernate lo rechazaba con un ERROR
                    //en el log por cada fila (getValueFromFn pasa todos los
                    //atributos de la vista). Observado el 2026-09-16.
                    if (isNamedParameterInQuery(entry.getKey(), queryString)) {
                        query.setParameter(entry.getKey(), entry.getValue());
                    }
                } else {
                    query.setParameter(entry.getKey(), entry.getValue());
                }
            } catch (Exception ex) {
                // Dejar rastro: si el bindeo falla el error real aparece recién
                // al ejecutar el query como "No argument for named parameter"
                LOGGER.error("Error asignando el parámetro '" + entry.getKey()
                        + "' del query: " + ex.getMessage());
            }
        });
    }

    /**
     * Indica si la sentencia contiene el parámetro nombrado {@code :nombre}
     * completo, es decir, no seguido de otro carácter de identificador
     * (letra, dígito o guion bajo). Así {@code :idctacte} no se confunde con
     * {@code :idctacteruta}.
     *
     * @param name nombre del parámetro (sin los dos puntos).
     * @param queryString sentencia JPQL/SQL con parámetros nombrados.
     * @return verdadero si la sentencia usa ese parámetro.
     */
    static boolean isNamedParameterInQuery(String name, String queryString) {
        if (name == null || name.isEmpty() || queryString == null) {
            return false;
        }
        int from = 0;
        String token = ":" + name;
        while (true) {
            int pos = queryString.indexOf(token, from);
            if (pos < 0) {
                return false;
            }
            int end = pos + token.length();
            if (end >= queryString.length()
                    || !(Character.isLetterOrDigit(queryString.charAt(end))
                    || queryString.charAt(end) == '_')) {
                return true;
            }
            from = end;
        }
    }

    /**
     * Setea valores predeterminados de ciertas constantes en los queries.
     *
     * @param persistUnit unidad de persistencia
     * @return Map con los valores de las constantes.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Override
    public Map<String, String> getQueryConstants(String persistUnit) {
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
     * Devuelve el objeto userSession con la información de la sesión
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
    protected final void setAppUser(IDataRow ejb, String appUser) {
        try {
            if (DataInfo.isFieldExist(ejb.getClass(), "appuser")) {
                ejb.setValue("appuser", appUser);
            }
        } catch (Exception ex) {
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
    protected final String getEntityManagerId(IDBLinkInfo dbLinkInfo) {
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
            Long threadId = Thread.currentThread().threadId();
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
    protected final String getEntityId(String persistUnit) {
        LOGGER.debug("getEntityId()");
        if (Strings.isNullorEmpty(persistUnit)) {
            persistUnit = IDBManager.CATALOGO;
        }
        return persistUnit + ":";
    }

    /**
     * Verifica que el valor del campo idcompany (identificador de la empresa a
     * la que esta logueada) sea válido. No puede grabar o borrar registros que
     * corresponde a una empresa que no esta logueada.
     *
     * @param <T>
     * @param dbLinkInfo
     * @param ejb
     * @throws CompanyError
     */
    protected final <T extends IDataRow> void checkFieldIdcompany(IDBLinkInfo dbLinkInfo, T ejb) throws CompanyError {
        if (dbLinkInfo == null || ejb == null) {
            return;
        }
        if (ejb.getAction() == 0) {
            return;
        }
        Long idcompany = dbLinkInfo.getIdCompany();
        if (nvl(idcompany, 0L) == 0L) {
            return;
        }
        // Verificar valor de idcompany
        if (!ejb.checkFieldIdcompany(idcompany)) {
            IUserSession userSession = dbLinkInfo.getUserSession();
            boolean exito = false;
            // Si la empresa en la que esta logueada agrupa varias empresas
            if (userSession != null && userSession.getCompany() != null
                    && !userSession.getCompany().getCompanyList().isEmpty()) {
                for (IAppCompany company : userSession.getCompany().getCompanyList()) {
                    if (ejb.checkFieldIdcompany(company.getIdcompany())) {
                        exito = true;
                        break;
                    }
                }
            }
            if (!exito) {
                throw new CompanyError("Valor de idcompany inválido");
            }
        }
    }

    /**
     * Verifica los datos (usuario, contraseña y empresa a la que se quiere
     * loguear) Si pasa la válidación se puede crear el token.
     *
     * @param data datos conteniendo valores de usuario, pass y empresa
     * @throws SessionError
     */
    @Override
    public void checkAuthConsumerData(IOAuthConsumerData data) throws SessionError {
        //Login y checkAccessCompany de Sessions
        if (!sessions.checkAuthConsumerData(data)) {
            throw new SessionError();
        }
    }

    /**
     * Verifica si la combinación iduser, idcompany es válido para una sesión
     *
     * @param iduser identificador del usuario
     * @param idcompany identificador de la empresa
     * @return verdadero si cumple y falso si no
     */
    @Override
    public boolean isCredentialValid(Long iduser, Long idcompany) {
        try {
            if (!sessions.isUserValid(iduser)) {
                return false;
            }
            return sessions.checkCompanyAccess(iduser, idcompany);
        } catch (Exception e) {
            ErrorManager.showError(e, LOGGER, logMngr, null);
        }
        return false;
    }

    /**
     * Devuelve un dato almacenado en el contexto de la sesión del usuario.
     *
     * @param sessionId identificador de la sesión del usuario.
     * @param key clave del dato.
     * @return valor almacenado, o {@code null} si no existe.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Object getSessionInfo(String sessionId, String key) {
        return sessions.getSessionInfo(sessionId, key);
    }

    /**
     * Almacena un dato en el contexto de la sesión del usuario.
     *
     * @param sessionId identificador de la sesión del usuario.
     * @param key clave del dato.
     * @param info valor a almacenar.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void addSessionInfo(String sessionId, String key, Object info) {
        sessions.addSessionInfo(sessionId, key, info);
    }

    /**
     * Elimina un dato del contexto de la sesión del usuario.
     *
     * @param sessionId identificador de la sesión del usuario.
     * @param key clave del dato.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void removeSessionInfo(String sessionId, String key) {
        sessions.removeSessionInfo(sessionId, key);
    }

    /**
     * Construye un registro de error a partir del catálogo de mensajes
     * (tabla {@code AppMessage}), usando el mensaje alternativo si el número no existe.
     *
     * <p>Es {@code REQUIRES_NEW} por la misma razón que la sobrecarga de cuatro
     * argumentos en la que delega: lee —y a veces escribe— el catálogo, que es
     * otro origen de datos. Con el catálogo en {@code jta="true"}, armar un
     * mensaje de validación desde una transacción que ya tocó la base de la
     * empresa enlistaba un segundo recurso de una fase y Narayana abortaba
     * ({@code ARJUNA012140}): el operador recibía un error 500 en vez del
     * mensaje. Pasa por el camino REST de administración
     * ({@code REQUIRED} → servicio → {@code checkUniqueKey}), no por el de la
     * UI web, donde no hay transacción (hallazgos {@code E3-12} / {@code I5-14}
     * del plan JTACAT).</p>
     *
     * <p>La anotación va acá y no solo en la de cuatro argumentos porque esta
     * delega con {@code this}: esa llamada no cruza el proxy y la anotación de
     * la otra nunca se aplicaba. Al abrir la transacción desde acá, la
     * delegación corre dentro de ella.</p>
     *
     * @param messageNumber número de mensaje en el catálogo.
     * @param alternativeMsg mensaje a usar si el número no se encuentra.
     * @param fieldName campo asociado al error.
     * @return registro de error.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public IErrorInfo getErrorMessage(Integer messageNumber, String alternativeMsg, String fieldName) {
        return getErrorMessage(messageNumber, alternativeMsg, fieldName, null);
    }

    /**
     * Construye un registro de error del catálogo de mensajes sustituyendo los
     * parámetros indicados en el texto del mensaje.
     *
     * <p>Si el texto alternativo trae marcadores de sustitución ({@code {clave}})
     * que el texto del catálogo no tiene, se usa el alternativo: así el llamador
     * puede pasar valores por parámetro sin necesidad de modificar el catálogo.</p>
     *
     * @param messageNumber número de mensaje en el catálogo.
     * @param alternativeMsg mensaje a usar si el número no se encuentra o si trae
     * marcadores {@code {clave}} que el texto del catálogo no tiene.
     * @param fieldName campo asociado al error.
     * @param params parámetros de sustitución del mensaje.
     * @return registro de error.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public IErrorInfo getErrorMessage(Integer messageNumber, String alternativeMsg, String fieldName, Map<String, String> params) {
        IAppMessage appMessage = logMngr.getAppMessage(messageNumber);
        ErrorInfo errorReturn = new ErrorInfo(alternativeMsg, messageNumber, fieldName);
        if (appMessage == null && !Fn.nvl(alternativeMsg, "").isEmpty() && messageNumber != 50000){
            try {
                appMessage = (IAppMessage)logMngr.getAppMessage(50001).clone();
                appMessage.setValue("idmessage",null);
                appMessage.setValue("number", messageNumber.longValue());
                appMessage.setValue("explanation", alternativeMsg);
                appMessage.setValue("description", alternativeMsg);
                persist(null, appMessage);
                //
                String messageText = Strings.textMerge(appMessage.getText(), params);
                errorReturn.setMessage(messageText);
            } catch (Exception e) {
                ErrorManager.showError(e, LOGGER, logMngr, FALSE);
            }
        }
        else if (appMessage != null) {
            // Si el texto alternativo trae marcadores de sustitución ({clave})
            // y el texto del catálogo no, se usa el alternativo: el llamador
            // puede pasar valores por parámetro sin modificar el catálogo.
            String text = appMessage.getText();
            if (hasVariables(alternativeMsg) && !hasVariables(text)) {
                text = alternativeMsg;
            }
            String messageText = Strings.textMerge(text, params);
            errorReturn.setMessage(messageText);
        } 
        return errorReturn;
    }

    /**
     * Indica si el texto contiene al menos un marcador de sustitución
     * ({@code {clave}}) del estilo de {@link Strings#textMerge}.
     *
     * @param text texto a inspeccionar.
     * @return verdadero si contiene un marcador {@code {clave}}.
     */
    private static boolean hasVariables(String text) {
        if (text == null) {
            return false;
        }
        int apertura = text.indexOf('{');
        return apertura >= 0 && text.indexOf('}', apertura) > apertura;
    }
    
    
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Object[]> findListObjsByQuery(String sessionId,
            String queryString,
            Map<String, Object> parameters,
            int first, int max) throws Exception {
        LOGGER.debug(Strings.replicate("-", 50));
        LOGGER.debug("findListObjsByQuery");
        LOGGER.debug(queryString);

        Query query = createQuery(sessionId, queryString, parameters);
        if (max > 0) {
            query.setFirstResult(first);
            query.setMaxResults(max);
        }
        return query.getResultList();
    }
}

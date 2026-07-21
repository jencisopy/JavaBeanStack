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

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.Resource;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import jakarta.persistence.EntityManager;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.log.ILogManager;
import org.javabeanstack.model.IAppLogRecord;
import org.javabeanstack.util.Dates;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.Strings;
import org.javabeanstack.xml.DomW3cParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * DBManager clásico: implementación tradicional de IDBManager. Gestiona el
 * acceso a los datos (lo utiliza GenericDAO) resolviendo el EntityManager de
 * cada unidad de persistencia por lookup JNDI (java:app/em/PUn), publicado por
 * el contenedor a partir de las persistence-unit declaradas en
 * persistence.xml. Los EntityManager son container-managed y transaction-
 * scoped: no requieren cierre explícito, solo se descartan del cache.
 *
 * Existen además DBManagerV20 y DBManagerV30, implementaciones independientes
 * de IDBManager que agregan unidades de persistencia dinámicas fabricadas en
 * runtime con PersistenceConfiguration (por system properties y por
 * META-INF/dynamic_persistence.xml respectivamente), manteniendo intacto este
 * camino JNDI para las unidades declaradas.
 *
 * La implementación a utilizar (DBManager, DBManagerV20 o DBManagerV30) se
 * decide en el ejb-jar.xml de la aplicación, en el ejb-class del session bean
 * DBManager. Documentación completa en
 * miscellaneous/docs/STATIC_MANAGMENT_DBMANAGER.md (en este repositorio).
 *
 * @author Jorge Enciso
 */
@Startup
@Lock(LockType.READ)
public class DBManager implements IDBManager {

    private static final Logger LOGGER = LogManager.getLogger(DBManager.class);

    /**
     * Prefijo JNDI donde WildFly publica el EntityManager de cada unidad de
     * persistencia (propiedad jboss.entity.manager.jndi.name en persistence.xml).
     * Se usa java:app porque es un namespace por deployment: permite que varios
     * deployments (ear, war) definan las mismas PUs sin colision de nombres.
     */
    private static final String JNDI_EM_PREFIX
            = System.getProperty("jbs.persistence.jndi.em.prefix", "java:app/em/");

    /**
     * Estrategia de creación/acceso de los entityManager: por thread
     * (IDBManager.PERTHREAD) o por sesión de usuario (IDBManager.PERSESSION,
     * la usada en Maker).
     */
    private final int entityIdStrategic = IDBManager.PERSESSION;

    /**
     * Fecha de la última purga del cache de entityManagers.
     */
    private Date lastPurge = new Date();

    /**
     * Cache de entityManagers container-managed por clave "PU:sessionId" (o
     * "PU:threadId" según entityIdStrategic). ConcurrentHashMap porque
     * getEntityManager() y purgeEntityManager() lo acceden y mutan
     * concurrentemente bajo @Lock(READ).
     */
    private final Map<String, Data> entityManagers = new ConcurrentHashMap<>();

    /**
     * Contexto de la sesión EJB actual; se usa en rollBack() para marcar la
     * transacción como rollback-only.
     */
    @Resource
    SessionContext context;

    /**
     * Devuelve la estrategia de acceso/creación de los entityManagers. Los
     * valores posibles son: un entityManager por Thread o un entityManager por
     * sesión del usuario.
     *
     * @return estrategia de acceso/creación de los entityManagers.
     */
    @Override
    public int getEntityIdStrategic() {
        return entityIdStrategic;
    }

    /**
     * Devuelve un entityManager, lo crea si no existe en la unidad de
     * persistencia solicitada. Si ya está en el cache actualiza su fecha de
     * última referencia; en cada acceso dispara además la purga por
     * ociosidad (purgeEntityManager).
     *
     * @param key clave con formato "PU:sessionId" o "PU:threadId".
     * @return Devuelve un entityManager
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public EntityManager getEntityManager(String key) {
        try {
            if (key == null || "".equals(key)) {
                return null;
            }
            EntityManager em;
            Data data = entityManagers.get(key);
            if (data != null) {
                em = data.em;
                data.lastRef = Dates.now();
                LOGGER.debug("EntityManager ya existe: " + key);
            } else {
                em = this.createEntityManager(key);
            }
            purgeEntityManager();
            return em;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Crea un entitymanager dentro de un Map utiliza la unidad de persistencia
     * y el threadid o sessionid del usuario como clave. La unidad de
     * persistencia se extrae del prefijo de la clave (antes de ":") y se
     * resuelve por lookup JNDI (java:app/em/PUn, publicado por el contenedor a
     * partir de la persistence-unit declarada en persistence.xml).
     *
     * @param key clave con formato "PU:sessionId" o "PU:threadId".
     * @return el entity manager creado, o null si el lookup JNDI falla (ej.
     * unidad no declarada en persistence.xml).
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Lock(LockType.WRITE)
    public EntityManager createEntityManager(String key) {
        EntityManager em;
        try {
            String persistentUnit = key.substring(0, key.indexOf(':')).toUpperCase();
            em = InitialContext.doLookup(JNDI_EM_PREFIX + persistentUnit);
            Data data = new Data();
            data.em = em;
            entityManagers.put(key, data);
            LOGGER.debug("--------- Se ha creado un nuevo EntityManager --------- " + key);
            return em;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Elimina los entityManagers del map, a aquellos que no se esta utilizando
     * en un periodo dado.
     */
    protected void purgeEntityManager() {
        LOGGER.debug("purgeEntityManager() " + lastPurge);
        Date now = new Date();
        //Solo procesar si la ultima purga fue hace 5 minutos.
        if (!lastPurge.before(DateUtils.addMinutes(now, -5))) {
            return;
        }
        //Purgar aquellos entityManagers que no fueron referenciados hace 5 minutos
        now = DateUtils.addMinutes(Dates.now(), -5);
        for (Iterator<Map.Entry<String, Data>> it = entityManagers.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Data> entry = it.next();
            if (entry.getValue().lastRef.before(now)) {
                LOGGER.debug("Se elimino entityManager: " + entry.getKey());
                it.remove();
            }
        }
        lastPurge = new Date();
        LOGGER.debug("Se proceso purgeEntityManager " + lastPurge);
    }

    /**
     * Ejecuta rollback de una transacción
     */
    @Override
    @Lock(LockType.WRITE)
    public void rollBack() {
        try {
            context.setRollbackOnly();
        } catch (Exception exp) {
            //
        }
    }

    /**
     * Ejecuta un script de actualización de base de datos (nodos SCRIPTS del
     * XML de actualización de Maker) sentencia por sentencia, filtrando por el
     * motor de base de datos de la unidad de persistencia del usuario y
     * registrando el progreso/errores en dic_logupdate y en el log de la
     * aplicación. Las sentencias con dataconex=CATALOGO corren contra el
     * schema catalogo (sessionId null); el resto contra el schema datos de la
     * unidad de persistencia del usuario.
     *
     * @param dao DAO genérico usado para ejecutar las sentencias SQL.
     * @param sessionId identificador de la sesión del usuario que ejecuta el script.
     * @param domScript documento XML con los nodos /ROOT/SCRIPTS a ejecutar.
     * @param parameters parámetros del script (secuencia, filename, classLog,
     * CONTINUE_WITH_ERROR, etc.); se completan/actualizan durante la ejecución.
     * @param logMngr gestor de logs de aplicación donde registrar errores (opcional).
     * @throws Exception si una sentencia falla y CONTINUE_WITH_ERROR no está activo.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public static void dbScriptUpdateExecute(IGenericDAO dao, String sessionId, Document domScript, Map<String, Object> parameters, ILogManager logMngr) throws Exception {

        List<Element> sqlScriptNodes = DomW3cParser.getChildren(domScript, "/ROOT/SCRIPTS");
        IErrorReg errorReturn;

        String logTable = "dic_logupdate";
        Class clazz = (Class) parameters.get("classLog");
        if (clazz != null) {
            logTable = DataInfo.getTableName(clazz);
        }

        //Inicio================================================
        String initCommand = "INSERT INTO {schema}." + logTable + " "
                + "(secuencia, filename, script, appuser) "
                + "     values "
                + "(:secuencia, :filename, :script, :appuser)";

        dao.sqlExec(sessionId, initCommand, parameters);

        String persistUnit = dao.getDBLinkInfo(sessionId).getPersistUnit();
        String motorDatos = dao.getDataEngine(persistUnit);
        String filename = (String)parameters.get("filename");
        
        for (Element sqlScriptNode : sqlScriptNodes) {
            //Solo se ejecuta los scripts que corresponde a motor de la base 
            if (!sqlScriptNode.getAttribute("motor").equals(motorDatos)) {
                continue;
            }
            String stringEnd = "\nGO\n";
            if (!Fn.inList(motorDatos, "SQLSERVER", "Microsoft SQL Server", "SYBASE")) {
                stringEnd = "\n/\n";
            }
            String script = sqlScriptNode.getTextContent();
            //Actualizar script a ejecutarse
            parameters.put("script", DomW3cParser.getXmlText(sqlScriptNode));
            String command = "UPDATE {schema}." + logTable
                    + " SET script = :script "
                    + " where secuencia = :secuencia";
            dao.sqlExec(sessionId, command, parameters);
            parameters.put("script", "");

            while (!script.isEmpty()) {
                String sentencia;
                int posicion = script.toUpperCase().indexOf(stringEnd);
                if (posicion < 0) {
                    sentencia = Strings.substr(script, 0);
                    script = "";
                } else {
                    sentencia = Strings.substr(script, 0, posicion);
                    script = Strings.substr(script, posicion + stringEnd.length());
                }
                // Ejecución del Script
                if (sqlScriptNode.getAttribute("dataconex").equals("CATALOGO")) {
                    //En el schema catalogo.
                    errorReturn = dao.sqlExec(null, sentencia, parameters);
                    if (errorReturn.getErrorNumber() > 0) {
                        //Se permite errores y se continua
                        String message = "ERROR en PU1, SCRIPT " + filename + ", " + errorReturn.getMessage();
                        Exception ex;
                        if (errorReturn.getException() != null) {
                            ex = errorReturn.getException();
                        } else {
                            ex = new Exception(message);
                        }
                        ErrorManager.showError(ex, LOGGER);
                        if (logMngr != null) {
                            IAppLogRecord logRecord = logMngr.getNewAppLogRecord(null);
                            String messageInfo = "";
                            if (errorReturn.getException() != null) {
                                messageInfo = ErrorManager.getMessageToShow(errorReturn.getException());
                            }
                            logRecord.setEvent(IAppLogRecord.EVENT_UPDATEDB);
                            logRecord.setLevel(IAppLogRecord.LEVEL_ERROR);
                            logRecord.setCategory(IAppLogRecord.CATEGORY_DATA);
                            logRecord.setMessage(message);
                            logRecord.setMessageInfo(messageInfo);
                            logRecord.setMessageNumber(1);
                            logMngr.dbWrite(logRecord, sessionId);
                        }
                    }
                } else {
                    //En el schema datos.
                    errorReturn = dao.sqlExec(sessionId, sentencia, parameters);
                    if (errorReturn.getErrorNumber() > 0) {
                        String message = "ERROR en unidad de persistencia " + persistUnit + ", SCRIPT " + filename + ", " + errorReturn.getMessage();
                        Exception ex;
                        if (errorReturn.getException() != null) {
                            ex = errorReturn.getException();
                        } else {
                            ex = new Exception(message);
                        }
                        if (!Fn.toLogical(parameters.get("CONTINUE_WITH_ERROR"))) {
                            //Revertir proceso==================================
                            String revertCommand = "delete from {schema}." + logTable + " where secuencia = :secuencia";
                            dao.sqlExec(sessionId, revertCommand, parameters);
                            //Registrar en el log de la base
                            if (logMngr != null) {
                                IAppLogRecord logRecord = logMngr.getNewAppLogRecord(null);
                                String messageInfo = "";
                                if (errorReturn.getException() != null) {
                                    messageInfo = ErrorManager.getMessageToShow(errorReturn.getException());
                                }
                                logRecord.setEvent(IAppLogRecord.EVENT_UPDATEDB);
                                logRecord.setLevel(IAppLogRecord.LEVEL_ERROR);
                                logRecord.setCategory(IAppLogRecord.CATEGORY_DATA);
                                logRecord.setMessage(message);
                                logRecord.setMessageInfo(messageInfo);
                                logRecord.setMessageNumber(1);
                                logMngr.dbWrite(logRecord, sessionId);
                            }
                            throw ex;
                        } else {
                            ErrorManager.showError(ex, LOGGER);                            
                            //Registrar en el log de la base
                            if (logMngr != null) {
                                IAppLogRecord logRecord = logMngr.getNewAppLogRecord(null);
                                String messageInfo = "";
                                if (errorReturn.getException() != null) {
                                    messageInfo = ErrorManager.getMessageToShow(errorReturn.getException());
                                }
                                logRecord.setEvent(IAppLogRecord.EVENT_UPDATEDB);
                                logRecord.setLevel(IAppLogRecord.LEVEL_ERROR);
                                logRecord.setCategory(IAppLogRecord.CATEGORY_DATA);
                                logRecord.setMessage(message);
                                logRecord.setMessageInfo(messageInfo);
                                logRecord.setMessageNumber(1);
                                logMngr.dbWrite(logRecord, sessionId);
                            }
                        }
                    }
                }

            }
        }
        //Fin ============================================
        String endCommand = "UPDATE {schema}." + logTable
                + " SET concluido = {true} "
                + " where secuencia = :secuencia";

        dao.sqlExec(sessionId, endCommand, parameters);
    }

    /**
     * Entrada del cache de entityManagers: el entity manager container-managed
     * y la fecha de su última referencia (para la purga por ociosidad).
     */
    class Data {

        EntityManager em;
        Date lastRef = Dates.now();
    }
}

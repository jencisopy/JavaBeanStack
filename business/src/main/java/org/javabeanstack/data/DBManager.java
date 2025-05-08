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

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.SessionContext;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

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
 * Contiene metodos para gestionar el acceso a los datos, es utilizado por
 * GenericDAO
 *
 * @author Jorge Enciso
 */
@Startup
@Lock(LockType.READ)
public class DBManager implements IDBManager {

    private static final Logger LOGGER = Logger.getLogger(DBManager.class);
    private int entityIdStrategic = IDBManager.PERSESSION;
    private Date lastPurge = new Date();

    private final Map<String, Data> entityManagers = new HashMap<>();

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
     * persistencia solicitada
     *
     * @param key id thread
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
            if (entityManagers.containsKey(key)) {
                em = entityManagers.get(key).em;
                entityManagers.get(key).lastRef = Dates.now();
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
     * y el threadid o sessionid del usuario como clave
     *
     * @param key id thread o sessionid del usuario
     * @return el entity manager creado.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Lock(LockType.WRITE)
    public EntityManager createEntityManager(String key) {
        EntityManager em;
        try {
            String persistentUnit = key.substring(0, key.indexOf(':')).toLowerCase();
            em = (EntityManager) context.lookup("java:comp/env/persistence/" + persistentUnit);
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

    class Data {

        EntityManager em;
        Date lastRef = Dates.now();
    }
}

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
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
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
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
 * Contiene metodos para gestionar el acceso a los datos, es utilizado por
 * GenericDAO
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

    private static final String DYNAMIC_ENABLED = "jbs.persistence.dynamic.enabled";
    private static final String DYNAMIC_UNITS = "jbs.persistence.dynamic.units";
    private static final String DYNAMIC_DATASOURCE_PREFIX = "jbs.persistence.dynamic.datasource.prefix";
    private static final String DYNAMIC_PROVIDER = "jbs.persistence.dynamic.provider";
    private static final String DYNAMIC_PROPERTIES = "jbs.persistence.dynamic.properties";
    private static final String DYNAMIC_MANAGED_CLASSES = "jbs.persistence.dynamic.managed.classes";
    private static final String DYNAMIC_MANAGED_CLASSES_RESOURCE = "jbs.persistence.dynamic.managed.classes.resource";
    private static final String DYNAMIC_MAPPING_FILES = "jbs.persistence.dynamic.mapping.files";
    private static final String HIBERNATE_PROVIDER = "org.hibernate.jpa.HibernatePersistenceProvider";

    private int entityIdStrategic = IDBManager.PERSESSION;
    private Date lastPurge = new Date();

    //ConcurrentHashMap: getEntityManager() y purgeEntityManager() acceden y mutan
    //el map concurrentemente bajo @Lock(READ)
    private final Map<String, Data> entityManagers = new ConcurrentHashMap<>();

    private final Map<String, EntityManagerFactory> entityManagerFactories = new ConcurrentHashMap<>();

    private final ThreadLocal<Map<String, EntityManager>> threadEntityManagers
            = ThreadLocal.withInitial(HashMap::new);

    @Resource
    SessionContext context;

    @Resource
    TransactionSynchronizationRegistry tsr;

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
            String persistentUnit = getPersistentUnit(key);
            if (isDynamicPersistenceUnit(persistentUnit)) {
                return getDynamicEntityManager(persistentUnit);
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
            String persistentUnit = getPersistentUnit(key);
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

    private String getPersistentUnit(String key) {
        int separator = key.indexOf(':');
        if (separator < 0) {
            throw new IllegalArgumentException("Clave de EntityManager inválida: " + key);
        }
        return key.substring(0, separator).toUpperCase();
    }

    private boolean isDynamicPersistenceUnit(String persistentUnit) {
        if (!Boolean.getBoolean(DYNAMIC_ENABLED)) {
            return false;
        }
        String units = System.getProperty(DYNAMIC_UNITS, "").trim();
        if (units.isEmpty()) {
            return false;
        }
        if ("*".equals(units)) {
            return true;
        }
        for (String unit : units.split(",")) {
            if (persistentUnit.equalsIgnoreCase(unit.trim())) {
                return true;
            }
        }
        return false;
    }

    private EntityManager getDynamicEntityManager(String persistentUnit) {
        if (isTransactionActive()) {
            String resourceKey = getTransactionResourceKey(persistentUnit);
            EntityManager em = (EntityManager) tsr.getResource(resourceKey);
            if (em != null && em.isOpen()) {
                return em;
            }
            em = createDynamicEntityManager(persistentUnit, true);
            tsr.putResource(resourceKey, em);
            registerCloseOnTransactionEnd(em);
            return em;
        }
        Map<String, EntityManager> managers = threadEntityManagers.get();
        EntityManager em = managers.get(persistentUnit);
        if (em == null || !em.isOpen()) {
            em = createDynamicEntityManager(persistentUnit, false);
            managers.put(persistentUnit, em);
        }
        return em;
    }

    private boolean isTransactionActive() {
        return tsr != null && tsr.getTransactionStatus() == Status.STATUS_ACTIVE;
    }

    private String getTransactionResourceKey(String persistentUnit) {
        return DBManager.class.getName() + ".dynamicEntityManager." + persistentUnit;
    }

    private EntityManager createDynamicEntityManager(String persistentUnit, boolean joinTransaction) {
        EntityManager em = entityManagerFactories
                .computeIfAbsent(persistentUnit, this::buildEntityManagerFactory)
                .createEntityManager();
        if (joinTransaction) {
            em.joinTransaction();
        }
        return em;
    }

    private void registerCloseOnTransactionEnd(EntityManager em) {
        tsr.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
            }

            @Override
            public void afterCompletion(int status) {
                closeEntityManager(em);
            }
        });
    }

    private EntityManagerFactory buildEntityManagerFactory(String persistentUnit) {
        String dataSource = getDynamicProperty(persistentUnit, "jtaDataSource", null);
        if (Strings.isNullorEmpty(dataSource)) {
            dataSource = buildDefaultDataSource(persistentUnit);
        }
        if (Strings.isNullorEmpty(dataSource)) {
            throw new IllegalStateException("No se definió datasource dinámico para " + persistentUnit);
        }

        PersistenceConfiguration config = new PersistenceConfiguration(persistentUnit)
                .provider(System.getProperty(DYNAMIC_PROVIDER, HIBERNATE_PROVIDER))
                .transactionType(PersistenceUnitTransactionType.JTA)
                .jtaDataSource(dataSource)
                .property("hibernate.default_schema", getDefaultSchema(persistentUnit));

        addDynamicProperties(config, persistentUnit);
        addMappingFiles(config, persistentUnit);
        addManagedClasses(config, persistentUnit);
        LOGGER.info("EntityManagerFactory dinámico creado para " + persistentUnit + " sobre " + dataSource);
        return config.createEntityManagerFactory();
    }

    private String buildDefaultDataSource(String persistentUnit) {
        String prefix = System.getProperty(DYNAMIC_DATASOURCE_PREFIX, "");
        if (Strings.isNullorEmpty(prefix)) {
            return null;
        }
        String number = persistentUnit.replaceAll("\\D", "");
        return number.isEmpty() ? null : prefix + number;
    }

    private String getDefaultSchema(String persistentUnit) {
        return IDBManager.CATALOGO.equalsIgnoreCase(persistentUnit) ? "catalogo" : "datos";
    }

    private void addDynamicProperties(PersistenceConfiguration config, String persistentUnit) {
        String propertyNames = System.getProperty(DYNAMIC_PROPERTIES, "").trim();
        if (propertyNames.isEmpty()) {
            return;
        }
        for (String propertyName : propertyNames.split(",")) {
            propertyName = propertyName.trim();
            if (propertyName.isEmpty()) {
                continue;
            }
            String value = getDynamicProperty(persistentUnit, "property." + propertyName, null);
            if (!Strings.isNullorEmpty(value)) {
                config.property(propertyName, value);
            }
        }
    }

    private void addMappingFiles(PersistenceConfiguration config, String persistentUnit) {
        String mappingFiles = getDynamicProperty(persistentUnit, "mapping.files", null);
        if (Strings.isNullorEmpty(mappingFiles)) {
            mappingFiles = System.getProperty(DYNAMIC_MAPPING_FILES, "");
        }
        if (Strings.isNullorEmpty(mappingFiles)) {
            return;
        }
        for (String mappingFile : mappingFiles.split(",")) {
            mappingFile = mappingFile.trim();
            if (!mappingFile.isEmpty()) {
                config.mappingFile(mappingFile);
            }
        }
    }

    private void addManagedClasses(PersistenceConfiguration config, String persistentUnit) {
        String classNames = getDynamicProperty(persistentUnit, "managed.classes", null);
        if (Strings.isNullorEmpty(classNames)) {
            classNames = System.getProperty(DYNAMIC_MANAGED_CLASSES, "");
        }
        if (Strings.isNullorEmpty(classNames)) {
            classNames = getManagedClassesFromResource(persistentUnit);
        }
        if (Strings.isNullorEmpty(classNames)) {
            throw new IllegalStateException("No se definieron entidades para " + persistentUnit);
        }
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (String className : classNames.split(",")) {
            className = className.trim();
            if (className.isEmpty()) {
                continue;
            }
            try {
                config.managedClass(Class.forName(className, true, classLoader));
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("No se encontró la entidad " + className, ex);
            }
        }
    }

    private String getManagedClassesFromResource(String persistentUnit) {
        String resourceName = getDynamicProperty(persistentUnit, "managed.classes.resource", null);
        if (Strings.isNullorEmpty(resourceName)) {
            resourceName = System.getProperty(DYNAMIC_MANAGED_CLASSES_RESOURCE, "");
        }
        if (Strings.isNullorEmpty(resourceName)) {
            return "";
        }
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream input = classLoader.getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IllegalStateException("No se encontró el recurso de entidades " + resourceName);
            }
            StringBuilder result = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    if (result.length() > 0) {
                        result.append(',');
                    }
                    result.append(line);
                }
            }
            return result.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer el recurso de entidades " + resourceName, ex);
        }
    }

    private String getDynamicProperty(String persistentUnit, String name, String defaultValue) {
        String value = System.getProperty("jbs.persistence.dynamic." + persistentUnit + "." + name);
        if (Strings.isNullorEmpty(value)) {
            value = System.getProperty("jbs.persistence.dynamic." + name);
        }
        return Strings.isNullorEmpty(value) ? defaultValue : value;
    }

    @Override
    public void closeEntityManager(EntityManager em) {
        if (em != null && em.isOpen()) {
            em.close();
        }
    }

    @Override
    public void closeEntityManagers() {
        Map<String, EntityManager> managers = threadEntityManagers.get();
        managers.values().forEach(this::closeEntityManager);
        managers.clear();
        threadEntityManagers.remove();
    }

    public void closeFactory(String persistentUnit) {
        EntityManagerFactory factory = entityManagerFactories.remove(persistentUnit.toUpperCase());
        if (factory != null && factory.isOpen()) {
            factory.close();
        }
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

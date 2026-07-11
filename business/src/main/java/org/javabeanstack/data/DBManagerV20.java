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
import java.util.Iterator;
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
import org.javabeanstack.util.Dates;
import org.javabeanstack.util.Strings;

/**
 * DBManager versión 2.0: gestiona el acceso a los datos (lo utiliza GenericDAO)
 * combinando el camino clásico por JNDI para las unidades de persistencia
 * declaradas en persistence.xml con unidades de persistencia DINÁMICAS,
 * fabricadas en runtime con PersistenceConfiguration (Jakarta Persistence 3.2).
 *
 * A diferencia de DBManagerV21 (que lee su configuración de la plantilla
 * DINAMIC_PU comentada en persistence.xml), esta variante se configura por
 * SYSTEM PROPERTIES de la JVM (prefijo jbs.persistence.dynamic.*), definidas
 * típicamente en bin/maker-persistence.conf del WildFly:
 *
 * - jbs.persistence.dynamic.enabled=true          llave general del modo dinámico
 * - jbs.persistence.dynamic.units=PU3,PU4,... | * unidades a resolver dinámicamente
 * - jbs.persistence.dynamic.datasource.prefix=    prefijo del datasource + número de PU
 * - jbs.persistence.dynamic.properties=           whitelist de propiedades a aplicar
 * - jbs.persistence.dynamic.property.<nombre>=    valor de cada propiedad de la whitelist
 * - jbs.persistence.dynamic.managed.classes[.resource]=  clases del modelo (lista o recurso)
 * - jbs.persistence.dynamic.mapping.files=        archivos orm.xml adicionales
 * - jbs.persistence.dynamic.<PU>.<nombre>=        override por unidad (jtaDataSource, etc.)
 *
 * Ciclo de vida de los entity manager dinámicos (application-managed):
 * con transacción JTA activa se crea uno por (transacción, unidad) que se
 * cierra al concluir la misma; sin transacción se crea uno por (unidad, thread)
 * que libera el DAO al finalizar cada método de negocio (closeEntityManagers)
 * y, como respaldo, la purga por ociosidad de purgeEntityManager().
 *
 * La implementación a utilizar (DBManager, DBManagerV20 o DBManagerV21) se
 * elige en el ejb-jar.xml de la aplicación, en el ejb-class del session bean
 * DBManager. Documentación completa en
 * Maker-miscellaneous/docs/ia/DINAMIC_DATA_MANAGMENT_DBMANAGER_V20.md.
 *
 * @author Jorge Enciso
 */
@Startup
@Lock(LockType.READ)
public class DBManagerV20 implements IDBManager {

    private static final Logger LOGGER = LogManager.getLogger(DBManagerV20.class);

    /**
     * Prefijo JNDI donde WildFly publica el EntityManager de cada unidad de
     * persistencia (propiedad jboss.entity.manager.jndi.name en persistence.xml).
     * Se usa java:app porque es un namespace por deployment: permite que varios
     * deployments (ear, war) definan las mismas PUs sin colision de nombres.
     */
    private static final String JNDI_EM_PREFIX
            = System.getProperty("jbs.persistence.jndi.em.prefix", "java:app/em/");

    /**
     * System property que habilita el modo dinámico; con false o ausente esta
     * clase se comporta igual que el DBManager clásico.
     */
    private static final String DYNAMIC_ENABLED = "jbs.persistence.dynamic.enabled";

    /**
     * System property con las unidades a resolver dinámicamente: lista separada
     * por comas (PU3,PU4,...) o * para todas. Las listadas se fabrican en
     * runtime aunque estén declaradas en persistence.xml.
     */
    private static final String DYNAMIC_UNITS = "jbs.persistence.dynamic.units";

    /**
     * System property con el prefijo JNDI del datasource de las unidades
     * dinámicas; se concatena el número de la unidad (jdbc/Maker950DS_ + 7).
     * Puede sobreescribirse por unidad con jbs.persistence.dynamic.<PU>.jtaDataSource.
     */
    private static final String DYNAMIC_DATASOURCE_PREFIX = "jbs.persistence.dynamic.datasource.prefix";

    /**
     * System property con el proveedor de persistencia; por defecto Hibernate.
     */
    private static final String DYNAMIC_PROVIDER = "jbs.persistence.dynamic.provider";

    /**
     * System property con la whitelist (separada por comas) de nombres de
     * propiedades a aplicar a cada EMF dinámico; el valor de cada una se lee de
     * jbs.persistence.dynamic.property.<nombre>.
     */
    private static final String DYNAMIC_PROPERTIES = "jbs.persistence.dynamic.properties";

    /**
     * System property con la lista de clases del modelo (FQCN separados por
     * coma) a registrar en cada EMF dinámico; PersistenceConfiguration no
     * autodetecta clases.
     */
    private static final String DYNAMIC_MANAGED_CLASSES = "jbs.persistence.dynamic.managed.classes";

    /**
     * System property con el recurso del classpath (un FQCN por línea, admite
     * comentarios #) con las clases del modelo, ej. META-INF/jbs-managed-classes.txt.
     */
    private static final String DYNAMIC_MANAGED_CLASSES_RESOURCE = "jbs.persistence.dynamic.managed.classes.resource";

    /**
     * System property con archivos de mapeo orm.xml adicionales, separados por coma.
     */
    private static final String DYNAMIC_MAPPING_FILES = "jbs.persistence.dynamic.mapping.files";

    /**
     * Proveedor de persistencia por defecto de las unidades dinámicas.
     */
    private static final String HIBERNATE_PROVIDER = "org.hibernate.jpa.HibernatePersistenceProvider";

    private final int entityIdStrategic = IDBManager.PERSESSION;
    private Date lastPurge = new Date();

    //ConcurrentHashMap: getEntityManager() y purgeEntityManager() acceden y mutan
    //el map concurrentemente bajo @Lock(READ)
    private final Map<String, Data> entityManagers = new ConcurrentHashMap<>();

    //EntityManagerFactory fabricados en runtime, uno por unidad de persistencia
    //dinámica; se crean una sola vez (computeIfAbsent) y se cierran con closeFactory()
    private final Map<String, EntityManagerFactory> entityManagerFactories = new ConcurrentHashMap<>();

    //EntityManagers dinámicos creados fuera de una transacción, uno por unidad
    //de persistencia y thread. El cierre principal lo hace el DAO al finalizar
    //cada método de negocio (closeEntityManagers); purgeEntityManager() cierra
    //como respaldo los que queden ociosos.
    private final Map<String, Data> dynamicEntityManagers = new ConcurrentHashMap<>();

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
     * persistencia solicitada. Las unidades configuradas como dinámicas
     * (jbs.persistence.dynamic.units) se fabrican en runtime; el resto se
     * resuelve por el camino clásico JNDI (java:app/em/PUn) con cache.
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
            String persistentUnit = getPersistentUnit(key);
            if (isDynamicPersistenceUnit(persistentUnit)) {
                EntityManager emDynamic = getDynamicEntityManager(persistentUnit);
                purgeEntityManager();
                return emDynamic;
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

    /**
     * Extrae y valida la unidad de persistencia de la clave de un entity
     * manager (formato "PU:sessionId").
     *
     * @param key clave del entity manager.
     * @return la unidad de persistencia en mayúsculas.
     */
    private String getPersistentUnit(String key) {
        int separator = key.indexOf(':');
        if (separator < 0) {
            throw new IllegalArgumentException("Clave de EntityManager inválida: " + key);
        }
        return key.substring(0, separator).toUpperCase();
    }

    /**
     * Determina si una unidad de persistencia debe resolverse dinámicamente:
     * requiere jbs.persistence.dynamic.enabled=true y que la unidad figure en
     * jbs.persistence.dynamic.units (lista separada por comas o *).
     *
     * @param persistentUnit unidad de persistencia.
     * @return verdadero si la unidad se fabrica en runtime.
     */
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

    /**
     * Devuelve un entity manager de una unidad de persistencia dinámica. Si hay
     * una transacción JTA activa devuelve uno por (transacción, unidad) que se
     * cierra solo al concluir la misma; si no la hay, devuelve uno por (unidad,
     * thread) que libera el DAO al finalizar el método de negocio
     * (closeEntityManagers) o, como respaldo, la purga por ociosidad.
     *
     * @param persistentUnit unidad de persistencia.
     * @return entity manager de la unidad de persistencia solicitada.
     */
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
        //Sin transacción, un entity manager por unidad de persistencia y thread
        String threadKey = persistentUnit + ":" + Thread.currentThread().getId();
        Data data = dynamicEntityManagers.get(threadKey);
        if (data != null && data.em.isOpen()) {
            data.lastRef = Dates.now();
            return data.em;
        }
        Data dataNew = new Data();
        dataNew.em = createDynamicEntityManager(persistentUnit, false);
        dynamicEntityManagers.put(threadKey, dataNew);
        return dataNew.em;
    }

    /**
     * Verifica si hay una transacción JTA activa en el thread actual.
     *
     * @return verdadero si hay transacción activa.
     */
    private boolean isTransactionActive() {
        return tsr != null && tsr.getTransactionStatus() == Status.STATUS_ACTIVE;
    }

    /**
     * Arma la clave con la que se guarda el entity manager de una unidad de
     * persistencia dentro del TransactionSynchronizationRegistry (un recurso
     * por transacción y unidad).
     *
     * @param persistentUnit unidad de persistencia.
     * @return clave del recurso transaccional.
     */
    private String getTransactionResourceKey(String persistentUnit) {
        return DBManagerV20.class.getName() + ".dynamicEntityManager." + persistentUnit;
    }

    /**
     * Crea un entity manager application-managed de una unidad dinámica;
     * fabrica el entity manager factory la primera vez (computeIfAbsent).
     *
     * @param persistentUnit unidad de persistencia.
     * @param joinTransaction verdadero para asociarlo a la transacción JTA activa.
     * @return el entity manager creado.
     */
    private EntityManager createDynamicEntityManager(String persistentUnit, boolean joinTransaction) {
        EntityManager em = entityManagerFactories
                .computeIfAbsent(persistentUnit, this::buildEntityManagerFactory)
                .createEntityManager();
        if (joinTransaction) {
            em.joinTransaction();
        }
        return em;
    }

    /**
     * Registra en la transacción JTA activa una sincronización interpuesta que
     * cierra el entity manager en afterCompletion (después del commit/rollback).
     *
     * @param em entity manager a cerrar al concluir la transacción.
     */
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

    /**
     * Fabrica el entity manager factory de una unidad de persistencia dinámica
     * con PersistenceConfiguration (Jakarta Persistence 3.2): datasource por
     * override de unidad o prefijo + número, propiedades de la whitelist,
     * archivos de mapeo y clases del modelo registradas explícitamente.
     *
     * @param persistentUnit unidad de persistencia, ej. PU3.
     * @return el entity manager factory creado.
     */
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

    /**
     * Deriva el nombre JNDI del datasource a partir del prefijo configurado
     * (jbs.persistence.dynamic.datasource.prefix) más el número de la unidad
     * de persistencia (PU7 -> prefijo + 7).
     *
     * @param persistentUnit unidad de persistencia.
     * @return nombre JNDI del datasource o null si no hay prefijo o número.
     */
    private String buildDefaultDataSource(String persistentUnit) {
        String prefix = System.getProperty(DYNAMIC_DATASOURCE_PREFIX, "");
        if (Strings.isNullorEmpty(prefix)) {
            return null;
        }
        String number = persistentUnit.replaceAll("\\D", "");
        return number.isEmpty() ? null : prefix + number;
    }

    /**
     * Devuelve el schema por defecto de una unidad de persistencia según la
     * convención de Maker: catalogo para PU1 y datos para el resto.
     *
     * @param persistentUnit unidad de persistencia.
     * @return nombre del schema por defecto.
     */
    private String getDefaultSchema(String persistentUnit) {
        return IDBManager.CATALOGO.equalsIgnoreCase(persistentUnit) ? "catalogo" : "datos";
    }

    /**
     * Aplica al EMF dinámico las propiedades de la whitelist
     * jbs.persistence.dynamic.properties, leyendo el valor de cada una de
     * jbs.persistence.dynamic[.<PU>].property.<nombre> (el override por unidad
     * tiene prioridad).
     *
     * @param config configuración de persistencia en construcción.
     * @param persistentUnit unidad de persistencia.
     */
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

    /**
     * Agrega al EMF dinámico los archivos de mapeo orm.xml configurados
     * (override por unidad o global, separados por coma).
     *
     * @param config configuración de persistencia en construcción.
     * @param persistentUnit unidad de persistencia.
     */
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

    /**
     * Registra en el EMF dinámico las clases del modelo (entidades, vistas
     * mapeadas, audit y converters). PersistenceConfiguration no autodetecta
     * clases; se leen de la property managed.classes (lista de FQCN) o del
     * recurso managed.classes.resource (un FQCN por línea). Falla si no se
     * definió ninguna fuente o si alguna clase no existe.
     *
     * @param config configuración de persistencia en construcción.
     * @param persistentUnit unidad de persistencia.
     */
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

    /**
     * Lee la lista de clases del modelo desde un recurso del classpath (ej.
     * META-INF/jbs-managed-classes.txt): un FQCN por línea, se ignoran líneas
     * vacías y comentarios que empiezan con #.
     *
     * @param persistentUnit unidad de persistencia.
     * @return los FQCN separados por coma, o cadena vacía si no hay recurso configurado.
     */
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

    /**
     * Lee una system property dinámica con prioridad: primero el override por
     * unidad (jbs.persistence.dynamic.<PU>.<nombre>) y luego el valor global
     * (jbs.persistence.dynamic.<nombre>).
     *
     * @param persistentUnit unidad de persistencia.
     * @param name nombre de la propiedad (sin prefijo).
     * @param defaultValue valor por defecto si no está definida.
     * @return el valor de la propiedad.
     */
    private String getDynamicProperty(String persistentUnit, String name, String defaultValue) {
        String value = System.getProperty("jbs.persistence.dynamic." + persistentUnit + "." + name);
        if (Strings.isNullorEmpty(value)) {
            value = System.getProperty("jbs.persistence.dynamic." + name);
        }
        return Strings.isNullorEmpty(value) ? defaultValue : value;
    }

    /**
     * Cierra un entityManager cuando fue creado por la aplicación (unidades de
     * persistencia dinámicas). Los entityManager gestionados por el contenedor
     * no requieren cierre explícito.
     *
     * @param em entity manager a cerrar.
     */
    @Override
    public void closeEntityManager(EntityManager em) {
        if (em != null && em.isOpen()) {
            em.close();
        }
    }

    /**
     * Cierra y libera los entity managers dinámicos creados sin transacción en
     * el thread actual. Lo invoca el DAO (AbstractDAO, hook AroundInvoke) al
     * finalizar cada método de negocio; los que queden ociosos los cierra la
     * purga de purgeEntityManager() como respaldo.
     */
    @Override
    public void closeEntityManagers() {
        String suffix = ":" + Thread.currentThread().getId();
        for (Iterator<Map.Entry<String, Data>> it = dynamicEntityManagers.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Data> entry = it.next();
            if (entry.getKey().endsWith(suffix)) {
                closeEntityManager(entry.getValue().em);
                it.remove();
            }
        }
    }

    /**
     * Cierra y descarta el entity manager factory dinámico de una unidad de
     * persistencia (ej. baja de una empresa en caliente). Un acceso posterior
     * a la misma unidad vuelve a fabricarlo.
     *
     * @param persistentUnit unidad de persistencia.
     */
    @Override
    public void closeFactory(String persistentUnit) {
        EntityManagerFactory factory = entityManagerFactories.remove(persistentUnit.toUpperCase());
        if (factory != null && factory.isOpen()) {
            factory.close();
        }
    }

    /**
     * Elimina los entityManagers del map, a aquellos que no se esta utilizando
     * en un periodo dado (5 minutos). Los del camino clásico solo se descartan
     * (son container-managed); los dinámicos ociosos además se cierran, como
     * respaldo del cierre principal que hace el DAO (closeEntityManagers).
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
        //Respaldo: cerrar y purgar los entityManagers dinámicos (application-managed)
        //ociosos que no haya liberado el DAO (closeEntityManagers)
        for (Iterator<Map.Entry<String, Data>> it = dynamicEntityManagers.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Data> entry = it.next();
            if (entry.getValue().lastRef.before(now)) {
                closeEntityManager(entry.getValue().em);
                LOGGER.debug("Se cerró entityManager dinámico: " + entry.getKey());
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
     * Entrada de los caches de entity managers: el entity manager y la fecha
     * de su última referencia (para la purga por ociosidad).
     */
    class Data {

        EntityManager em;
        Date lastRef = Dates.now();
    }
}

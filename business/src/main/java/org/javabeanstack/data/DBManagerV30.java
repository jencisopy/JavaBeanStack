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

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.util.Dates;

/**
 * DBManager versión 3.0: implementación autocontenida de IDBManager que
 * resuelve las unidades de persistencia en dos caminos:
 *
 * 1) Camino estático (PU declaradas en persistence.xml): lookup JNDI
 * java:app/em/PUn, idéntico al DBManager clásico. Los entityManager son
 * container-managed y no se cierran, solo se descartan del cache. PU1 y PU2
 * (catálogo y unidad base) van SIEMPRE por este camino.
 *
 * 2) Camino dinámico: las unidades definidas en el archivo
 * META-INF/dynamic_persistence.xml (mismo esquema que persistence.xml, pero
 * conteniendo solo las unidades dinámicas, cada una con su configuración
 * propia: datasource, dialecto, schema, etc.). Como el archivo no se llama
 * persistence.xml el contenedor no lo escanea y nada bootea; el
 * EntityManagerFactory de cada unidad se fabrica en runtime con
 * PersistenceConfiguration (Jakarta Persistence 3.2). Una unidad listada en el
 * archivo se resuelve dinámicamente aunque también esté declarada en
 * persistence.xml (dinámico-primero: el switch de ejb-jar.xml decide de
 * verdad). Una unidad no listada ni declarada se fabrica desde la unidad
 * plantilla DEFAULT del mismo archivo (el placeholder {n} del datasource se
 * sustituye con el número de la unidad) — permite el alta de empresas sin
 * redeploy.
 *
 * Si el archivo no existe o está corrupto se loguea la advertencia y la clase
 * se comporta exactamente como el DBManager clásico (todo por JNDI); nunca
 * rompe el arranque del deployment.
 *
 * Ciclo de vida de los entityManager dinámicos (application-managed): con
 * transacción JTA activa se usa uno por transacción y unidad, registrado en el
 * TransactionSynchronizationRegistry y cerrado al concluir la misma; sin
 * transacción se usa uno por unidad y thread, cerrado de inmediato por
 * closeEntityManagers() (lo invoca el interceptor de AbstractDAO) o por la
 * purga de ociosidad de 5 minutos como respaldo.
 *
 * La implementación a utilizar (DBManager, DBManagerV20 o DBManagerV30) se
 * decide en el ejb-jar.xml de la aplicación, en el ejb-class
 * del session bean DBManager. Documentación completa en
 * miscellaneous/docs/DINAMIC_DATA_MANAGMENT_DBMANAGER_V30.md (en este repositorio).
 *
 * @author Jorge Enciso
 */
@Startup
@Lock(LockType.READ)
public class DBManagerV30 implements IDBManager {

    private static final Logger LOGGER = LogManager.getLogger(DBManagerV30.class);

    /**
     * Prefijo JNDI donde WildFly publica el EntityManager de cada unidad de
     * persistencia declarada (propiedad jboss.entity.manager.jndi.name en
     * persistence.xml). Se usa java:app porque es un namespace por deployment.
     */
    private static final String JNDI_EM_PREFIX
            = System.getProperty("jbs.persistence.jndi.em.prefix", "java:app/em/");

    /**
     * Prefijo JNDI donde WildFly publica el EntityManagerFactory de cada
     * unidad de persistencia declarada (para leer el metamodelo de clases).
     */
    private static final String JNDI_EMF_PREFIX
            = System.getProperty("jbs.persistence.jndi.emf.prefix", "java:app/emf/");

    /**
     * Ruta (en el classpath) del archivo con las unidades de persistencia
     * dinámicas; mismo esquema que persistence.xml.
     */
    private static final String CONFIG_FILE
            = System.getProperty("jbs.dynamic.persistence.file", "META-INF/dynamic_persistence.xml");

    /**
     * Nombre de la unidad plantilla dentro del archivo dinámico, de la que se
     * fabrican las unidades no definidas explícitamente (su jta-data-source
     * lleva el placeholder {n}).
     */
    private static final String TEMPLATE_UNIT_NAME
            = System.getProperty("jbs.dynamic.template.unit", "DEFAULT");

    /**
     * Propiedad (por unidad, con fallback a la plantilla) con la PU declarada
     * de donde tomar el modelo de clases (metamodelo) para las unidades
     * dinámicas.
     */
    public static final String DYNAMIC_METAMODEL_PU = "jbs.dynamic.metamodel.pu";

    private static final String HIBERNATE_PROVIDER = "org.hibernate.jpa.HibernatePersistenceProvider";
    private static final String DEFAULT_METAMODEL_PU = "PU2";

    /**
     * Unidades que van SIEMPRE por el camino estático (JNDI) aunque figuren en
     * el archivo dinámico: PU1 (catálogo) y PU2 (unidad base/metamodelo).
     */
    private static final List<String> ALWAYS_STATIC_UNITS = List.of("PU1", "PU2");

    /**
     * Estrategia de creación/acceso de los entityManager estáticos: por sesión
     * de usuario (misma estrategia que el DBManager clásico).
     */
    private final int entityIdStrategic = IDBManager.PERSESSION;

    /**
     * Configuración de las unidades dinámicas leída del archivo
     * dynamic_persistence.xml; se carga una sola vez (lazy, double-checked) y
     * queda inmutable por el resto del ciclo de vida del singleton. Incluye la
     * unidad plantilla bajo su propio nombre (DEFAULT).
     */
    private volatile Map<String, PersistenceUnitConfig> configs;

    /**
     * EntityManagerFactory fabricados en runtime, uno por unidad dinámica; se
     * cierran con closeFactory(pu) o al bajar el deployment (shutdown).
     */
    private final Map<String, EntityManagerFactory> dynamicFactories = new ConcurrentHashMap<>();

    /**
     * Cache unificado de entityManagers: los estáticos (container-managed, por
     * clave "PU:sessionId") y los dinámicos sin transacción
     * (application-managed, por clave "PU:threadId"). La purga descarta ambos
     * al quedar ociosos, pero cierra solo los application-managed.
     */
    private final Map<String, EmEntry> entityManagers = new ConcurrentHashMap<>();

    /**
     * Unidades detectadas en runtime como no declaradas en persistence.xml ni
     * definidas en el archivo dinámico, resueltas desde la plantilla DEFAULT
     * (último recurso); evita repetir el lookup JNDI fallido en cada acceso.
     */
    private final java.util.Set<String> templateResolvedUnits = ConcurrentHashMap.newKeySet();

    /**
     * Fecha de la última purga del cache de entityManagers.
     */
    private Date lastPurge = new Date();

    /**
     * Contexto de la sesión EJB actual; se usa en rollBack() para marcar la
     * transacción como rollback-only.
     */
    @Resource
    SessionContext context;

    /**
     * Registro de sincronización de la transacción JTA actual: permite asociar
     * un entityManager dinámico a la transacción activa (un recurso por
     * transacción y unidad) y registrar su cierre en afterCompletion.
     */
    @Resource
    TransactionSynchronizationRegistry tsr;

    //==========================================================================
    // Contrato IDBManager
    //==========================================================================
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
     * persistencia solicitada. Las unidades definidas en
     * dynamic_persistence.xml se fabrican en runtime (dinámico-primero); el
     * resto se resuelve por lookup JNDI como el DBManager clásico; y si el
     * lookup falla por no estar declarada, se fabrica desde la plantilla
     * DEFAULT como último recurso.
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
            EntityManager em;
            if (isDynamic(persistentUnit)) {
                em = getDynamicEntityManager(persistentUnit);
            } else {
                try {
                    em = getStaticEntityManager(key, persistentUnit);
                } catch (NameNotFoundException ex) {
                    em = resolveNotDeclared(persistentUnit);
                }
            }
            purgeEntityManagers();
            return em;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Crea un entitymanager dentro de un Map utiliza la unidad de persistencia
     * y el threadid o sessionid del usuario como clave. Para las unidades
     * dinámicas devuelve el entityManager fabricado en runtime; para las
     * estáticas hace el lookup JNDI y lo registra en el cache.
     *
     * @param key clave con formato "PU:sessionId" o "PU:threadId".
     * @return el entity manager creado, o null si no fue posible crearlo.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Lock(LockType.WRITE)
    public EntityManager createEntityManager(String key) {
        try {
            if (key == null || "".equals(key)) {
                return null;
            }
            String persistentUnit = getPersistentUnit(key);
            if (isDynamic(persistentUnit)) {
                return getDynamicEntityManager(persistentUnit);
            }
            EntityManager em = InitialContext.doLookup(JNDI_EM_PREFIX + persistentUnit);
            entityManagers.put(key, new EmEntry(em, true));
            LOGGER.debug("--------- Se ha creado un nuevo EntityManager --------- " + key);
            return em;
        } catch (NameNotFoundException ex) {
            try {
                return resolveNotDeclared(getPersistentUnit(key));
            } catch (Exception ex2) {
                ErrorManager.showError(ex2, LOGGER);
            }
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
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
     * Cierra un entityManager cuando fue creado por la aplicación (unidades de
     * persistencia dinámicas). Los entityManager gestionados por el contenedor
     * no requieren cierre explícito.
     *
     * @param em entity manager a cerrar.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void closeEntityManager(EntityManager em) {
        try {
            if (em != null && em.isOpen()) {
                em.close();
            }
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
    }

    /**
     * Cierra y libera los entityManagers dinámicos (application-managed)
     * creados sin transacción en el thread actual. Lo invoca el interceptor de
     * AbstractDAO al finalizar cada método de negocio; la purga por ociosidad
     * queda como respaldo.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void closeEntityManagers() {
        String suffix = ":" + Thread.currentThread().getId();
        for (Iterator<Map.Entry<String, EmEntry>> it = entityManagers.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, EmEntry> entry = it.next();
            if (!entry.getValue().containerManaged && entry.getKey().endsWith(suffix)) {
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
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void closeFactory(String persistentUnit) {
        if (persistentUnit == null || persistentUnit.isEmpty()) {
            return;
        }
        EntityManagerFactory emf = dynamicFactories.remove(persistentUnit.toUpperCase());
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    //==========================================================================
    // Puntos de configuración overrideables (tests, proyectos derivados)
    //==========================================================================
    /**
     * Devuelve la ruta (en el classpath) del archivo con las unidades de
     * persistencia dinámicas. Overrideable en tests y proyectos derivados.
     *
     * @return ruta del archivo de configuración dinámica.
     */
    protected String getConfigFile() {
        return CONFIG_FILE;
    }

    /**
     * Devuelve el nombre de la unidad plantilla del archivo dinámico.
     * Overrideable en tests y proyectos derivados.
     *
     * @return nombre de la unidad plantilla.
     */
    protected String getTemplateUnitName() {
        return TEMPLATE_UNIT_NAME;
    }

    //==========================================================================
    // Resolución de la unidad de persistencia
    //==========================================================================
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
     * Determina si una unidad de persistencia se resuelve dinámicamente: está
     * definida en dynamic_persistence.xml (aunque también esté declarada en
     * persistence.xml) o ya fue resuelta desde la plantilla DEFAULT. PU1 y PU2
     * van siempre por el camino estático.
     *
     * @param persistentUnit unidad de persistencia.
     * @return verdadero si la unidad se fabrica en runtime.
     */
    protected boolean isDynamic(String persistentUnit) {
        if (ALWAYS_STATIC_UNITS.contains(persistentUnit)
                || getTemplateUnitName().equalsIgnoreCase(persistentUnit)) {
            return false;
        }
        if (templateResolvedUnits.contains(persistentUnit)) {
            return true;
        }
        return getConfigs().containsKey(persistentUnit);
    }

    /**
     * Resuelve una unidad no declarada en persistence.xml ni definida en el
     * archivo dinámico: si existe la plantilla DEFAULT la fabrica desde ella
     * (último recurso, ej. alta de una empresa nueva); si no, deja constancia
     * clara en el log y devuelve null.
     *
     * @param persistentUnit unidad de persistencia.
     * @return el entity manager fabricado, o null si no hay plantilla.
     */
    private EntityManager resolveNotDeclared(String persistentUnit) {
        if (getConfigs().containsKey(getTemplateUnitName().toUpperCase())) {
            LOGGER.info("Unidad de persistencia no declarada, se fabrica desde la plantilla "
                    + getTemplateUnitName() + ": " + persistentUnit);
            templateResolvedUnits.add(persistentUnit);
            return getDynamicEntityManager(persistentUnit);
        }
        LOGGER.error("Unidad de persistencia no declarada en persistence.xml, no definida en "
                + getConfigFile() + " y sin unidad plantilla " + getTemplateUnitName()
                + ": " + persistentUnit);
        return null;
    }

    //==========================================================================
    // Camino estático (unidades declaradas en persistence.xml, lookup JNDI)
    //==========================================================================
    /**
     * Devuelve el entityManager container-managed de una unidad declarada en
     * persistence.xml: del cache si existe (actualizando su fecha de última
     * referencia) o por lookup JNDI si no.
     *
     * @param key clave con formato "PU:sessionId" o "PU:threadId".
     * @param persistentUnit unidad de persistencia (ya extraída de la clave).
     * @return el entityManager container-managed.
     * @throws NameNotFoundException si la unidad no está declarada en
     * persistence.xml (el llamador decide el último recurso).
     * @throws Exception ante cualquier otra falla JNDI.
     */
    private EntityManager getStaticEntityManager(String key, String persistentUnit) throws Exception {
        EmEntry entry = entityManagers.get(key);
        if (entry != null) {
            entry.lastRef = Dates.now();
            LOGGER.debug("EntityManager ya existe: " + key);
            return entry.em;
        }
        EntityManager em = InitialContext.doLookup(JNDI_EM_PREFIX + persistentUnit);
        entityManagers.put(key, new EmEntry(em, true));
        LOGGER.debug("--------- Se ha creado un nuevo EntityManager --------- " + key);
        return em;
    }

    //==========================================================================
    // Camino dinámico (unidades del archivo dynamic_persistence.xml)
    //==========================================================================
    /**
     * Devuelve un entityManager de una unidad de persistencia dinámica. Si hay
     * una transacción JTA activa devuelve uno por transacción y unidad que se
     * cierra al concluir la misma; si no la hay, devuelve uno por unidad y
     * thread que closeEntityManagers() cierra de inmediato al finalizar el
     * método de negocio (o la purga por ociosidad como respaldo).
     *
     * @param persistentUnit unidad de persistencia.
     * @return entity manager de la unidad de persistencia solicitada.
     */
    protected EntityManager getDynamicEntityManager(String persistentUnit) {
        EntityManagerFactory emf = dynamicFactories.computeIfAbsent(persistentUnit, this::buildFactory);
        //Con transacción activa, un entityManager por transacción y unidad de persistencia
        if (isTransactionActive()) {
            String resourceKey = DBManagerV30.class.getName() + ".dynamicEntityManager." + persistentUnit;
            EntityManager em = (EntityManager) tsr.getResource(resourceKey);
            if (em != null && em.isOpen()) {
                return em;
            }
            final EntityManager emNew = emf.createEntityManager();
            emNew.joinTransaction();
            tsr.putResource(resourceKey, emNew);
            //Cerrar el entityManager al concluir la transacción
            tsr.registerInterposedSynchronization(new Synchronization() {
                @Override
                public void beforeCompletion() {
                }

                @Override
                public void afterCompletion(int status) {
                    closeEntityManager(emNew);
                }
            });
            return emNew;
        }
        //Sin transacción, un entityManager por unidad de persistencia y thread (lecturas)
        String key = persistentUnit + ":" + Thread.currentThread().getId();
        EmEntry entry = entityManagers.get(key);
        if (entry != null && entry.em.isOpen()) {
            entry.lastRef = Dates.now();
            //Limpiar el contexto de persistencia: cada lectura sin transacción
            //parte de un contexto fresco (misma semántica que el path estático,
            //donde el EM container-managed es transaction-scoped)
            entry.em.clear();
            return entry.em;
        }
        EmEntry entryNew = new EmEntry(emf.createEntityManager(), false);
        entityManagers.put(key, entryNew);
        return entryNew.em;
    }

    /**
     * Verifica si hay una transacción JTA activa en el thread actual.
     *
     * @return verdadero si hay transacción activa.
     */
    private boolean isTransactionActive() {
        return tsr != null && tsr.getTransactionStatus() == Status.STATUS_ACTIVE;
    }

    //==========================================================================
    // Fábrica de EntityManagerFactory dinámicos
    //==========================================================================
    /**
     * Fabrica el entity manager factory de una unidad de persistencia dinámica
     * con PersistenceConfiguration (Jakarta Persistence 3.2), a partir de su
     * definición en dynamic_persistence.xml (o derivada de la plantilla
     * DEFAULT si no está definida). El modelo de clases se toma del metamodelo
     * de la unidad declarada indicada en jbs.dynamic.metamodel.pu (default
     * PU2) más los elementos class de la definición (converters, etc.).
     *
     * @param persistentUnit unidad de persistencia, ej. PU3.
     * @return el entity manager factory creado.
     */
    protected EntityManagerFactory buildFactory(String persistentUnit) {
        try {
            PersistenceUnitConfig config = resolveConfig(persistentUnit);
            PersistenceConfiguration configuration = buildConfiguration(config);

            //Registrar las clases del modelo desde el metamodelo de una PU declarada
            //(PersistenceConfiguration no tiene autodetección de clases)
            String metamodelPu = config.properties().getOrDefault(DYNAMIC_METAMODEL_PU, DEFAULT_METAMODEL_PU);
            EntityManagerFactory metamodelEmf = InitialContext.doLookup(JNDI_EMF_PREFIX + metamodelPu);
            metamodelEmf.getMetamodel().getManagedTypes().forEach(managedType -> {
                if (managedType.getJavaType() != null) {
                    configuration.managedClass(managedType.getJavaType());
                }
            });
            LOGGER.info("--------- Creando EntityManagerFactory dinámico --------- "
                    + persistentUnit + ", datasource: " + config.jtaDataSource());
            return configuration.createEntityManagerFactory();
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo crear el EntityManagerFactory dinámico de "
                    + persistentUnit, ex);
        }
    }

    /**
     * Construye el PersistenceConfiguration de una unidad dinámica a partir de
     * su definición: proveedor, transacción JTA, datasource, propiedades
     * propias (filtrando las de control jbs.dynamic.* y las del contenedor
     * jboss.* y wildfly.*), clases extra y archivos de mapeo. No incluye las
     * clases del metamodelo (requieren JNDI; las agrega buildFactory), lo que
     * permite verificar en tests unitarios que la configuración se aplica
     * correctamente.
     *
     * @param config definición de la unidad.
     * @return la configuración de persistencia armada.
     * @throws Exception si una clase extra no existe en el classpath.
     */
    protected PersistenceConfiguration buildConfiguration(PersistenceUnitConfig config) throws Exception {
        PersistenceConfiguration configuration = new PersistenceConfiguration(config.name())
                .provider(config.provider())
                .transactionType(PersistenceUnitTransactionType.JTA)
                .jtaDataSource(config.jtaDataSource());

        //Propiedades propias de la unidad (dialecto, schema, jbs.*, etc.);
        //las de control jbs.dynamic.* y las del contenedor no se trasladan
        config.properties().forEach((property, value) -> {
            if (property.startsWith("jbs.dynamic.") || property.startsWith("jboss.")
                    || property.startsWith("wildfly.")) {
                return;
            }
            configuration.property(property, value);
        });

        //Clases adicionales de la definición que el metamodelo no expone
        //(converters, etc.)
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (String className : config.classes()) {
            configuration.managedClass(Class.forName(className, true, classLoader));
        }
        //Archivos de mapeo orm.xml adicionales
        for (String mappingFile : config.mappingFiles()) {
            configuration.mappingFile(mappingFile);
        }
        return configuration;
    }

    /**
     * Devuelve la configuración de una unidad dinámica: su definición
     * explícita en dynamic_persistence.xml o, si no existe, una derivada de la
     * unidad plantilla DEFAULT sustituyendo el placeholder {n} con el número
     * de la unidad (ej. PU11 sobre jdbc/Maker950DS_{n} da jdbc/Maker950DS_11).
     *
     * @param persistentUnit unidad de persistencia.
     * @return configuración de la unidad.
     */
    protected PersistenceUnitConfig resolveConfig(String persistentUnit) {
        Map<String, PersistenceUnitConfig> unitConfigs = getConfigs();
        PersistenceUnitConfig config = unitConfigs.get(persistentUnit);
        if (config != null) {
            return config;
        }
        PersistenceUnitConfig template = unitConfigs.get(getTemplateUnitName().toUpperCase());
        if (template == null) {
            throw new IllegalStateException("La unidad de persistencia " + persistentUnit
                    + " no está definida en " + getConfigFile()
                    + " y no existe la unidad plantilla " + getTemplateUnitName());
        }
        String number = persistentUnit.replaceAll("\\D", "");
        if (number.isEmpty()) {
            throw new IllegalStateException("No se pudo derivar el número de la unidad de persistencia "
                    + persistentUnit + " para la plantilla " + getTemplateUnitName());
        }
        Map<String, String> properties = new HashMap<>();
        template.properties().forEach((property, value)
                -> properties.put(property, value.replace("{n}", number)));
        return new PersistenceUnitConfig(persistentUnit,
                template.provider(),
                template.jtaDataSource().replace("{n}", number),
                template.classes(),
                template.mappingFiles(),
                properties);
    }

    //==========================================================================
    // Configuración: lectura de dynamic_persistence.xml
    //==========================================================================
    /**
     * Devuelve la configuración de las unidades dinámicas; la lee una sola vez
     * (double-checked locking sobre el campo volatile).
     *
     * @return map inmutable de configuraciones por unidad (en mayúsculas),
     * incluida la plantilla; vacío si el archivo no existe o es inválido.
     */
    protected Map<String, PersistenceUnitConfig> getConfigs() {
        Map<String, PersistenceUnitConfig> result = configs;
        if (result != null) {
            return result;
        }
        synchronized (this) {
            if (configs == null) {
                configs = loadConfigs();
            }
            return configs;
        }
    }

    /**
     * Lee y parsea el archivo dynamic_persistence.xml del classpath (TCCL).
     * Toma el primer recurso que contenga unidades de persistencia y advierte
     * si encuentra más de uno. PU1 y PU2 se descartan con una advertencia si
     * figuran en el archivo (van siempre por el camino estático). Si el
     * archivo no existe o está corrupto devuelve un map vacío (la clase queda
     * operando como el DBManager clásico) y deja constancia en el log.
     *
     * @return map inmutable de configuraciones por unidad (en mayúsculas).
     */
    private Map<String, PersistenceUnitConfig> loadConfigs() {
        Map<String, PersistenceUnitConfig> result = new HashMap<>();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(getConfigFile());
            if (!resources.hasMoreElements()) {
                LOGGER.warn("No se encontró " + getConfigFile() + " en el classpath; "
                        + "DBManagerV30 opera solo con las unidades declaradas en persistence.xml");
                return Map.of();
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(false);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                if (!result.isEmpty()) {
                    LOGGER.warn("Se ignora " + url + ": ya se leyó la configuración dinámica de otro recurso");
                    continue;
                }
                try (InputStream input = url.openStream()) {
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    //ErrorHandler propio: sin él Xerces vuelca "[Fatal Error]"
                    //directo a stderr además de lanzar la excepción
                    builder.setErrorHandler(new QuietErrorHandler());
                    Document document = builder.parse(input);
                    NodeList units = document.getDocumentElement().getElementsByTagName("persistence-unit");
                    for (int i = 0; i < units.getLength(); i++) {
                        //Una unidad inválida se descarta sin abortar la lectura de las demás
                        try {
                            PersistenceUnitConfig config = readUnit((Element) units.item(i));
                            if (ALWAYS_STATIC_UNITS.contains(config.name())) {
                                LOGGER.warn("La unidad " + config.name() + " es siempre estática; "
                                        + "se ignora su definición en " + url);
                                continue;
                            }
                            result.put(config.name(), config);
                        } catch (Exception ex) {
                            LOGGER.warn("Se descarta una unidad inválida de " + url + ": " + ex.getMessage());
                        }
                    }
                    if (!result.isEmpty()) {
                        LOGGER.info("Configuración dinámica leída de " + url
                                + ": " + result.keySet());
                    }
                } catch (Exception ex) {
                    //Archivo ilegible o mal formado: advertencia y se degrada
                    //al comportamiento clásico (sin stack, es un caso previsto)
                    LOGGER.warn("Archivo de configuración dinámica inválido, se ignora "
                            + url + ": " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return Map.copyOf(result);
    }

    /**
     * Convierte un elemento persistence-unit del archivo en su configuración
     * inmutable. Se honran: el atributo name, provider (default Hibernate),
     * jta-data-source, class (clases extra), mapping-file y properties.
     *
     * @param unit elemento persistence-unit parseado.
     * @return configuración inmutable de la unidad.
     */
    private PersistenceUnitConfig readUnit(Element unit) {
        String name = unit.getAttribute("name").toUpperCase();
        String provider = getChildText(unit, "provider", HIBERNATE_PROVIDER);
        String dataSource = getChildText(unit, "jta-data-source", null);
        if (dataSource == null || dataSource.isEmpty()) {
            throw new IllegalStateException("La unidad " + name + " de " + getConfigFile()
                    + " no define jta-data-source");
        }
        List<String> classes = new ArrayList<>();
        NodeList classNodes = unit.getElementsByTagName("class");
        for (int i = 0; i < classNodes.getLength(); i++) {
            classes.add(classNodes.item(i).getTextContent().trim());
        }
        List<String> mappingFiles = new ArrayList<>();
        NodeList mappingNodes = unit.getElementsByTagName("mapping-file");
        for (int i = 0; i < mappingNodes.getLength(); i++) {
            mappingFiles.add(mappingNodes.item(i).getTextContent().trim());
        }
        Map<String, String> properties = new HashMap<>();
        NodeList propertyNodes = unit.getElementsByTagName("property");
        for (int i = 0; i < propertyNodes.getLength(); i++) {
            Element property = (Element) propertyNodes.item(i);
            properties.put(property.getAttribute("name"), property.getAttribute("value"));
        }
        return new PersistenceUnitConfig(name, provider, dataSource, classes, mappingFiles, properties);
    }

    /**
     * Devuelve el texto del primer elemento hijo con el tag dado, o el valor
     * por defecto si no existe o está vacío.
     *
     * @param parent elemento padre.
     * @param tag nombre del tag hijo.
     * @param defaultValue valor por defecto.
     * @return texto del hijo o el valor por defecto.
     */
    private String getChildText(Element parent, String tag, String defaultValue) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return defaultValue;
        }
        String text = nodes.item(0).getTextContent().trim();
        return text.isEmpty() ? defaultValue : text;
    }

    //==========================================================================
    // Purga y cierre
    //==========================================================================
    /**
     * Purga los entityManagers sin uso en un periodo dado (5 minutos): los
     * container-managed solo se descartan del cache; los application-managed
     * (dinámicos) además se cierran.
     */
    protected void purgeEntityManagers() {
        Date now = new Date();
        //Solo procesar si la ultima purga fue hace 5 minutos.
        if (!lastPurge.before(DateUtils.addMinutes(now, -5))) {
            return;
        }
        now = DateUtils.addMinutes(Dates.now(), -5);
        for (Iterator<Map.Entry<String, EmEntry>> it = entityManagers.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, EmEntry> entry = it.next();
            if (entry.getValue().lastRef.before(now)) {
                if (!entry.getValue().containerManaged) {
                    closeEntityManager(entry.getValue().em);
                }
                LOGGER.debug("Se elimino entityManager: " + entry.getKey());
                it.remove();
            }
        }
        lastPurge = new Date();
    }

    /**
     * Cierra los entityManagers application-managed y los entity manager
     * factory dinámicos al bajar el deployment (mitiga fugas de memoria en el
     * redeploy).
     */
    @PreDestroy
    protected void shutdown() {
        entityManagers.values().forEach(entry -> {
            if (!entry.containerManaged) {
                closeEntityManager(entry.em);
            }
        });
        entityManagers.clear();
        dynamicFactories.values().forEach(emf -> {
            try {
                if (emf.isOpen()) {
                    emf.close();
                }
            } catch (Exception ex) {
                ErrorManager.showError(ex, LOGGER);
            }
        });
        dynamicFactories.clear();
    }

    //==========================================================================
    // Tipos internos
    //==========================================================================
    /**
     * Definición inmutable de una unidad de persistencia del archivo
     * dynamic_persistence.xml.
     *
     * @param name nombre de la unidad en mayúsculas (ej. PU3, DEFAULT).
     * @param provider proveedor de persistencia (default Hibernate).
     * @param jtaDataSource datasource JTA; en la plantilla puede llevar el
     * placeholder {n}.
     * @param classes clases adicionales al metamodelo (converters, etc.).
     * @param mappingFiles archivos de mapeo orm.xml adicionales.
     * @param properties propiedades de la unidad (dialecto, schema, etc.).
     */
    protected record PersistenceUnitConfig(
            String name,
            String provider,
            String jtaDataSource,
            List<String> classes,
            List<String> mappingFiles,
            Map<String, String> properties) {

        protected PersistenceUnitConfig {
            classes = List.copyOf(classes);
            mappingFiles = List.copyOf(mappingFiles);
            properties = Map.copyOf(properties);
        }
    }

    /**
     * Manejador de errores de parseo silencioso: propaga los errores como
     * excepción (que loadConfigs reporta como advertencia propia) en lugar
     * del volcado por defecto de Xerces a stderr.
     */
    private static class QuietErrorHandler implements ErrorHandler {

        @Override
        public void warning(SAXParseException ex) {
        }

        @Override
        public void error(SAXParseException ex) throws SAXException {
            throw ex;
        }

        @Override
        public void fatalError(SAXParseException ex) throws SAXException {
            throw ex;
        }
    }

    /**
     * Entrada del cache de entityManagers: el entity manager, si es
     * container-managed (JNDI, no se cierra) o application-managed (dinámico,
     * se cierra al purgarlo) y la fecha de su última referencia.
     */
    private static class EmEntry {

        final EntityManager em;
        final boolean containerManaged;
        Date lastRef = Dates.now();

        EmEntry(EntityManager em, boolean containerManaged) {
            this.em = em;
            this.containerManaged = containerManaged;
        }
    }
}

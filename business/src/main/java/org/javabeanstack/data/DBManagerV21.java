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
import java.io.StringReader;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
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
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.util.Dates;

/**
 * DBManager versión 2.1: extiende el DBManager clásico (path JNDI para las
 * unidades de persistencia declaradas en persistence.xml) agregando unidades
 * de persistencia dinámicas fabricadas en runtime con PersistenceConfiguration
 * (Jakarta Persistence 3.2).
 *
 * La configuración se lee de una unidad de persistencia plantilla (por defecto
 * DINAMIC_PU) declarada en persistence.xml con wildfly.jpa.managed=false para
 * que el contenedor no arranque su SessionFactory. De ahí se toman:
 * jbs.dynamic.units (lista o *), jbs.dynamic.static.units,
 * jbs.dynamic.datasource.pattern, jbs.dynamic.metamodel.pu,
 * jbs.dynamic.classes.extra, jbs.dynamic.provider, jbs.dynamic.mapping.files y
 * las propiedades hibernate/jbs a aplicar a cada unidad dinámica. Si la
 * plantilla no existe se clonan las propiedades de la PU de respaldo (PU2).
 *
 * La implementación a utilizar (DBManager o DBManagerV21) se decide en el
 * ejb-jar.xml de la aplicación, en el ejb-class del session bean DBManager.
 *
 * @author Jorge Enciso
 */
@Startup
@Lock(LockType.READ)
public class DBManagerV21 extends DBManager {

    private static final Logger LOGGER = LogManager.getLogger(DBManagerV21.class);

    /**
     * Prefijo JNDI donde WildFly publica el EntityManager de cada unidad de
     * persistencia declarada (propiedad jboss.entity.manager.jndi.name).
     */
    private static final String JNDI_EM_PREFIX
            = System.getProperty("jbs.persistence.jndi.em.prefix", "java:app/em/");

    /**
     * Prefijo JNDI donde WildFly publica el EntityManagerFactory de cada unidad
     * de persistencia declarada (propiedad jboss.entity.manager.factory.jndi.name).
     */
    private static final String JNDI_EMF_PREFIX
            = System.getProperty("jbs.persistence.jndi.emf.prefix", "java:app/emf/");

    /**
     * Nombre de la unidad de persistencia plantilla declarada en
     * persistence.xml (con wildfly.jpa.managed=false, no arranca SessionFactory).
     */
    private static final String TEMPLATE_PU_NAME
            = System.getProperty("jbs.dynamic.template.pu", "DINAMIC_PU");

    /**
     * Propiedad con las unidades de persistencia a resolver dinámicamente:
     * lista separada por comas (PU3,PU4,...) o * (todas menos las estáticas).
     */
    public static final String DYNAMIC_UNITS = "jbs.dynamic.units";

    /**
     * Propiedad con las unidades que siempre se resuelven por JNDI cuando
     * jbs.dynamic.units=* (default PU1,PU2).
     */
    public static final String DYNAMIC_STATIC_UNITS = "jbs.dynamic.static.units";

    /**
     * Propiedad con el patrón JNDI del datasource de las PUs dinámicas,
     * ej. jdbc/Maker950DS_{n}; {n} se sustituye con el número de la PU.
     */
    public static final String DYNAMIC_DATASOURCE_PATTERN = "jbs.dynamic.datasource.pattern";

    /**
     * Propiedad con la PU declarada de donde tomar el modelo de clases
     * (metamodelo) para las PUs dinámicas (default PU2).
     */
    public static final String DYNAMIC_METAMODEL_PU = "jbs.dynamic.metamodel.pu";

    /**
     * Propiedad con clases adicionales a registrar en las PUs dinámicas
     * (converters, etc. que el metamodelo no expone), separadas por coma o
     * punto y coma.
     */
    public static final String DYNAMIC_CLASSES_EXTRA = "jbs.dynamic.classes.extra";

    /**
     * Propiedad con el proveedor de persistencia de las PUs dinámicas;
     * por defecto Hibernate.
     */
    public static final String DYNAMIC_PROVIDER = "jbs.dynamic.provider";

    /**
     * Propiedad con archivos de mapeo orm.xml adicionales para las PUs
     * dinámicas, separados por coma.
     */
    public static final String DYNAMIC_MAPPING_FILES = "jbs.dynamic.mapping.files";

    private static final String HIBERNATE_PROVIDER = "org.hibernate.jpa.HibernatePersistenceProvider";
    private static final String DEFAULT_METAMODEL_PU = "PU2";
    private static final String DEFAULT_STATIC_UNITS = "PU1,PU2";

    //Propiedades de la PU plantilla (se leen una sola vez)
    private volatile Map<String, String> templateProperties;

    //Unidades no declaradas en persistence.xml detectadas en runtime (evita
    //reintentar el lookup JNDI fallido en cada acceso)
    private final Set<String> dynamicPus = ConcurrentHashMap.newKeySet();

    //EntityManagerFactory fabricados en runtime, uno por unidad de persistencia dinámica
    private final Map<String, EntityManagerFactory> dynamicFactories = new ConcurrentHashMap<>();

    //EntityManagers dinámicos creados fuera de una transacción (lecturas), uno
    //por unidad de persistencia y thread; los cierra purgeDynamicEntityManagers()
    //al quedar ociosos (mismo criterio que el cache del DBManager clásico)
    private final Map<String, EmData> dynamicEntityManagers = new ConcurrentHashMap<>();

    private Date lastDynamicPurge = new Date();

    @Resource
    TransactionSynchronizationRegistry tsr;

    /**
     * Devuelve un entityManager. Las unidades configuradas como dinámicas (o
     * no declaradas en persistence.xml) se fabrican en runtime; el resto se
     * resuelve por el path JNDI del DBManager clásico.
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
            if (isDynamic(persistentUnit)) {
                EntityManager em = getDynamicEntityManager(persistentUnit);
                purgeDynamicEntityManagers();
                return em;
            }
            EntityManager em = super.getEntityManager(key);
            if (em == null && !isDeclared(persistentUnit)) {
                //Último recurso: la PU no está declarada en persistence.xml ni
                //listada en jbs.dynamic.units, se resuelve dinámicamente
                LOGGER.info("Unidad de persistencia no declarada, se resuelve dinámicamente: " + persistentUnit);
                dynamicPus.add(persistentUnit);
                em = getDynamicEntityManager(persistentUnit);
            }
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
     * Determina si una unidad de persistencia debe resolverse dinámicamente
     * según jbs.dynamic.units de la plantilla (lista explícita o *, excluyendo
     * en ese caso las de jbs.dynamic.static.units).
     *
     * @param persistentUnit unidad de persistencia.
     * @return verdadero si la unidad se fabrica en runtime.
     */
    protected boolean isDynamic(String persistentUnit) {
        if (TEMPLATE_PU_NAME.equalsIgnoreCase(persistentUnit)) {
            return false;
        }
        if (dynamicPus.contains(persistentUnit)) {
            return true;
        }
        Map<String, String> template = getTemplate();
        String units = template.getOrDefault(DYNAMIC_UNITS, "").trim();
        if (units.isEmpty()) {
            return false;
        }
        if ("*".equals(units)) {
            String staticUnits = template.getOrDefault(DYNAMIC_STATIC_UNITS, DEFAULT_STATIC_UNITS);
            return !inList(persistentUnit, staticUnits);
        }
        return inList(persistentUnit, units);
    }

    private boolean inList(String persistentUnit, String list) {
        for (String item : list.split(",")) {
            if (persistentUnit.equalsIgnoreCase(item.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica si la unidad de persistencia está declarada en persistence.xml
     * (publicada por el contenedor en el namespace JNDI del deployment).
     *
     * @param persistentUnit unidad de persistencia.
     * @return verdadero si el contenedor publicó su entity manager.
     */
    private boolean isDeclared(String persistentUnit) {
        try {
            InitialContext.doLookup(JNDI_EM_PREFIX + persistentUnit);
            return true;
        } catch (NameNotFoundException ex) {
            return false;
        } catch (Exception ex) {
            //Otra falla JNDI: se asume declarada para no cambiarla de path
            return true;
        }
    }

    /**
     * Devuelve un entity manager de una unidad de persistencia dinámica. Si
     * hay una transacción JTA activa devuelve un entity manager por
     * transacción y unidad de persistencia que se cierra al concluir la misma;
     * si no la hay, devuelve uno por unidad de persistencia y thread que
     * purgeDynamicEntityManagers() cierra al quedar ocioso.
     *
     * @param persistentUnit unidad de persistencia.
     * @return entity manager de la unidad de persistencia solicitada.
     */
    protected EntityManager getDynamicEntityManager(String persistentUnit) {
        EntityManagerFactory emf = dynamicFactories.computeIfAbsent(persistentUnit, this::buildFactory);
        //Con transacción activa, un entity manager por transacción y unidad de persistencia
        if (isTransactionActive()) {
            String resourceKey = DBManagerV21.class.getName() + ".dynamicEntityManager." + persistentUnit;
            EntityManager em = (EntityManager) tsr.getResource(resourceKey);
            if (em != null && em.isOpen()) {
                return em;
            }
            final EntityManager emNew = emf.createEntityManager();
            emNew.joinTransaction();
            tsr.putResource(resourceKey, emNew);
            //Cerrar el entity manager al concluir la transacción
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
        //Sin transacción, un entity manager por unidad de persistencia y thread
        //(lecturas); se cierra en purgeDynamicEntityManagers() al quedar ocioso
        String key = persistentUnit + ":" + Thread.currentThread().getId();
        EmData data = dynamicEntityManagers.get(key);
        if (data != null && data.em.isOpen()) {
            data.lastRef = Dates.now();
            //Limpiar el contexto de persistencia para conservar la semántica
            //actual (cada lectura sin transacción parte de un contexto fresco)
            data.em.clear();
            return data.em;
        }
        EmData dataNew = new EmData();
        dataNew.em = emf.createEntityManager();
        dynamicEntityManagers.put(key, dataNew);
        return dataNew.em;
    }

    private boolean isTransactionActive() {
        return tsr != null && tsr.getTransactionStatus() == Status.STATUS_ACTIVE;
    }

    /**
     * Fabrica el entity manager factory de una unidad de persistencia dinámica
     * utilizando PersistenceConfiguration (Jakarta Persistence 3.2) con la
     * configuración de la PU plantilla; el datasource se deriva del patrón
     * jbs.dynamic.datasource.pattern y el modelo de clases del metamodelo de
     * la PU jbs.dynamic.metamodel.pu.
     *
     * @param persistentUnit unidad de persistencia, ej. PU3.
     * @return el entity manager factory creado.
     */
    protected EntityManagerFactory buildFactory(String persistentUnit) {
        try {
            Map<String, String> template = getTemplate();

            String pattern = template.get(DYNAMIC_DATASOURCE_PATTERN);
            if (pattern == null || pattern.isEmpty()) {
                throw new IllegalStateException("Falta la propiedad " + DYNAMIC_DATASOURCE_PATTERN
                        + " en la unidad de persistencia plantilla " + TEMPLATE_PU_NAME);
            }
            String number = persistentUnit.replaceAll("\\D", "");
            if (number.isEmpty()) {
                throw new IllegalStateException("No se pudo derivar el número de la unidad de persistencia "
                        + persistentUnit + " para el patrón " + pattern);
            }
            String dataSource = pattern.replace("{n}", number);
            String provider = template.getOrDefault(DYNAMIC_PROVIDER, HIBERNATE_PROVIDER);

            PersistenceConfiguration configuration = new PersistenceConfiguration(persistentUnit)
                    .provider(provider)
                    .transactionType(PersistenceUnitTransactionType.JTA)
                    .jtaDataSource(dataSource);

            //Aplicar las propiedades de la plantilla (dialecto, schema, jbs.*, etc.)
            template.forEach((property, value) -> {
                if (property.startsWith("jbs.dynamic.") || property.startsWith("jboss.")
                        || property.startsWith("wildfly.")) {
                    return;
                }
                configuration.property(property, value);
            });

            //Registrar las clases del modelo desde el metamodelo de una PU declarada
            //(PersistenceConfiguration no tiene autodetección de clases)
            String metamodelPu = template.getOrDefault(DYNAMIC_METAMODEL_PU, DEFAULT_METAMODEL_PU);
            EntityManagerFactory metamodelEmf = InitialContext.doLookup(JNDI_EMF_PREFIX + metamodelPu);
            metamodelEmf.getMetamodel().getManagedTypes().forEach(managedType -> {
                if (managedType.getJavaType() != null) {
                    configuration.managedClass(managedType.getJavaType());
                }
            });
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            //Clases adicionales que el metamodelo no expone (converters, etc.)
            String extraClasses = template.get(DYNAMIC_CLASSES_EXTRA);
            if (extraClasses != null && !extraClasses.isEmpty()) {
                for (String className : extraClasses.split("[,;]")) {
                    className = className.trim();
                    if (!className.isEmpty()) {
                        configuration.managedClass(Class.forName(className, true, classLoader));
                    }
                }
            }
            //Archivos de mapeo orm.xml adicionales
            String mappingFiles = template.get(DYNAMIC_MAPPING_FILES);
            if (mappingFiles != null && !mappingFiles.isEmpty()) {
                for (String mappingFile : mappingFiles.split(",")) {
                    mappingFile = mappingFile.trim();
                    if (!mappingFile.isEmpty()) {
                        configuration.mappingFile(mappingFile);
                    }
                }
            }
            LOGGER.info("--------- Creando EntityManagerFactory dinámico --------- "
                    + persistentUnit + ", datasource: " + dataSource);
            return configuration.createEntityManagerFactory();
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo crear el EntityManagerFactory dinámico de "
                    + persistentUnit, ex);
        }
    }

    /**
     * Devuelve las propiedades de la PU plantilla; las lee una sola vez.
     *
     * @return propiedades de la unidad de persistencia plantilla.
     */
    protected Map<String, String> getTemplate() {
        Map<String, String> result = templateProperties;
        if (result != null) {
            return result;
        }
        synchronized (this) {
            if (templateProperties == null) {
                templateProperties = loadTemplate();
            }
            return templateProperties;
        }
    }

    /**
     * Lee las propiedades de la PU plantilla parseando META-INF/persistence.xml
     * del classpath (la plantilla no bootea, no hay EMF en JNDI). La plantilla
     * puede estar declarada como persistence-unit o dentro de un comentario XML
     * (forma recomendada: así el contenedor jamás la arranca). Si no la
     * encuentra, clona como respaldo las propiedades de la PU de metamodelo
     * (default PU2).
     *
     * @return propiedades de la plantilla.
     */
    private Map<String, String> loadTemplate() {
        Map<String, String> props = new HashMap<>();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setNamespaceAware(false);
            Enumeration<URL> resources = classLoader.getResources("META-INF/persistence.xml");
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try (InputStream input = url.openStream()) {
                    Document document = factory.newDocumentBuilder().parse(input);
                    //1) Plantilla declarada como persistence-unit
                    if (readTemplateUnit(document.getDocumentElement(), props)) {
                        LOGGER.info("Plantilla dinámica " + TEMPLATE_PU_NAME + " leída de " + url);
                        return props;
                    }
                    //2) Plantilla dentro de un comentario XML (deshabilitada para el contenedor)
                    NodeList children = document.getDocumentElement().getChildNodes();
                    for (int i = 0; i < children.getLength(); i++) {
                        Node child = children.item(i);
                        if (child.getNodeType() != Node.COMMENT_NODE
                                || !child.getNodeValue().contains(TEMPLATE_PU_NAME)) {
                            continue;
                        }
                        String fragment = "<persistence>" + child.getNodeValue() + "</persistence>";
                        Document commented = factory.newDocumentBuilder()
                                .parse(new InputSource(new StringReader(fragment)));
                        if (readTemplateUnit(commented.getDocumentElement(), props)) {
                            LOGGER.info("Plantilla dinámica " + TEMPLATE_PU_NAME
                                    + " leída (comentada) de " + url);
                            return props;
                        }
                    }
                } catch (Exception ex) {
                    ErrorManager.showError(ex, LOGGER);
                }
            }
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        //Respaldo: clonar las propiedades de la PU de metamodelo declarada
        return loadTemplateFallback(props);
    }

    /**
     * Busca la unidad plantilla dentro de un elemento raíz parseado y vuelca
     * sus propiedades en el map recibido.
     *
     * @param root elemento raíz (persistence).
     * @param props map destino de las propiedades.
     * @return verdadero si encontró la plantilla.
     */
    private boolean readTemplateUnit(Element root, Map<String, String> props) {
        NodeList units = root.getElementsByTagName("persistence-unit");
        for (int i = 0; i < units.getLength(); i++) {
            Element unit = (Element) units.item(i);
            if (!TEMPLATE_PU_NAME.equalsIgnoreCase(unit.getAttribute("name"))) {
                continue;
            }
            NodeList properties = unit.getElementsByTagName("property");
            for (int j = 0; j < properties.getLength(); j++) {
                Element property = (Element) properties.item(j);
                props.put(property.getAttribute("name"), property.getAttribute("value"));
            }
            return true;
        }
        return false;
    }

    /**
     * Respaldo cuando no existe la PU plantilla: clona las propiedades de la
     * PU de metamodelo declarada (default PU2).
     *
     * @param props map destino de las propiedades.
     * @return el mismo map recibido.
     */
    private Map<String, String> loadTemplateFallback(Map<String, String> props) {
        try {
            EntityManagerFactory emf = InitialContext.doLookup(JNDI_EMF_PREFIX + DEFAULT_METAMODEL_PU);
            emf.getProperties().forEach((property, value) -> {
                if (value instanceof String) {
                    props.put(property, (String) value);
                }
            });
            LOGGER.warn("No se encontró la PU plantilla " + TEMPLATE_PU_NAME
                    + "; se clonan las propiedades de " + DEFAULT_METAMODEL_PU);
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return props;
    }

    /**
     * Cierra y purga los entity managers dinámicos sin uso en un periodo dado
     * (mismo criterio de 5 minutos que el purge del DBManager clásico).
     */
    protected void purgeDynamicEntityManagers() {
        Date now = new Date();
        //Solo procesar si la ultima purga fue hace 5 minutos.
        if (!lastDynamicPurge.before(DateUtils.addMinutes(now, -5))) {
            return;
        }
        now = DateUtils.addMinutes(Dates.now(), -5);
        for (Iterator<Map.Entry<String, EmData>> it = dynamicEntityManagers.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, EmData> entry = it.next();
            if (entry.getValue().lastRef.before(now)) {
                closeEntityManager(entry.getValue().em);
                LOGGER.debug("Se cerró entityManager dinámico: " + entry.getKey());
                it.remove();
            }
        }
        lastDynamicPurge = new Date();
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
     * Cierra y libera los entity managers dinámicos creados sin transacción en
     * el thread actual. Normalmente no es necesario invocarlo: los cierra
     * purgeDynamicEntityManagers() al quedar ociosos; queda disponible para
     * liberar en forma inmediata (ej. al cerrar una sesión de usuario).
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void closeEntityManagers() {
        String suffix = ":" + Thread.currentThread().getId();
        for (Iterator<Map.Entry<String, EmData>> it = dynamicEntityManagers.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, EmData> entry = it.next();
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

    /**
     * Cierra los entity managers y entity manager factory dinámicos al bajar
     * el deployment (mitiga fugas de memoria en el redeploy).
     */
    @PreDestroy
    protected void shutdownDynamicFactories() {
        dynamicEntityManagers.values().forEach(data -> closeEntityManager(data.em));
        dynamicEntityManagers.clear();
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

    private static class EmData {

        EntityManager em;
        Date lastRef = Dates.now();
    }
}

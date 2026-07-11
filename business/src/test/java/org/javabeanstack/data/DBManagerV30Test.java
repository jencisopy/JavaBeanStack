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

import java.util.Map;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceUnitTransactionType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test unitario de DBManagerV30: verifica la lectura del archivo de
 * configuración dinámica (dynamic_persistence.xml), la interpretación de cada
 * elemento y propiedad de las unidades de persistencia, la decisión
 * dinámico/estático, la derivación desde la plantilla DEFAULT (placeholder
 * {n}) y la aplicación correcta de la configuración al
 * PersistenceConfiguration (Jakarta Persistence 3.2).
 *
 * A diferencia de los demás tests del módulo, este corre OFFLINE: no requiere
 * WildFly ni base de datos. Los archivos de prueba están en
 * src/test/resources: META-INF/dynamic_persistence.xml (caso principal,
 * adaptado del de Maker, con unidades válidas, una unidad inválida, una
 * siempre-estática y la plantilla DEFAULT) y las variantes
 * org/javabeanstack/data/dynamic_persistence_sin_default.xml y
 * dynamic_persistence_corrupto.xml.
 *
 * @author Jorge Enciso
 */
public class DBManagerV30Test {

    private static final String ARCHIVO_SIN_DEFAULT
            = "org/javabeanstack/data/dynamic_persistence_sin_default.xml";
    private static final String ARCHIVO_CORRUPTO
            = "org/javabeanstack/data/dynamic_persistence_corrupto.xml";
    private static final String ARCHIVO_INEXISTENTE
            = "META-INF/no_existe_dynamic_persistence.xml";

    /**
     * Subclase de prueba: permite apuntar a un archivo de configuración
     * distinto del default (META-INF/dynamic_persistence.xml) usando el punto
     * de extensión getConfigFile().
     */
    private static class DBManagerV30Probado extends DBManagerV30 {

        private final String configFile;

        DBManagerV30Probado() {
            this("META-INF/dynamic_persistence.xml");
        }

        DBManagerV30Probado(String configFile) {
            this.configFile = configFile;
        }

        @Override
        protected String getConfigFile() {
            return configFile;
        }
    }

    /**
     * La lectura del archivo debe cargar las unidades válidas (PU3, PU5, PU6,
     * DEFAULT), normalizar los nombres a mayúsculas (pu6 -> PU6), ignorar la
     * unidad siempre-estática (PU1) y descartar la inválida (BROKEN, sin
     * jta-data-source) sin afectar a las demás.
     */
    @Test
    public void test01LecturaConfiguraciones() {
        DBManagerV30Probado manager = new DBManagerV30Probado();
        Map<String, DBManagerV30.PersistenceUnitConfig> configs = manager.getConfigs();

        assertTrue(configs.containsKey("PU3"), "debe cargar PU3");
        assertTrue(configs.containsKey("PU5"), "debe cargar PU5");
        assertTrue(configs.containsKey("PU6"), "debe normalizar pu6 a PU6");
        assertTrue(configs.containsKey("DEFAULT"), "debe cargar la plantilla DEFAULT");
        assertFalse(configs.containsKey("PU1"), "PU1 es siempre estática, debe ignorarse");
        assertFalse(configs.containsKey("BROKEN"), "una unidad sin jta-data-source debe descartarse");
        assertEquals(4, configs.size(), "no debe cargar unidades de más");
    }

    /**
     * Todos los elementos y propiedades de una unidad completa (PU3) deben
     * leerse con sus valores exactos: provider, datasource, clases extra,
     * mapping-files y cada property (incluidas las que luego se filtran al
     * armar el PersistenceConfiguration: acá deben estar presentes).
     */
    @Test
    public void test02PropiedadesDeUnaUnidad() {
        DBManagerV30Probado manager = new DBManagerV30Probado();
        DBManagerV30.PersistenceUnitConfig config = manager.getConfigs().get("PU3");

        assertNotNull(config);
        assertEquals("PU3", config.name());
        assertEquals("org.hibernate.jpa.HibernatePersistenceProvider", config.provider());
        assertEquals("jdbc/Maker950DS_3", config.jtaDataSource());
        assertEquals(1, config.classes().size());
        assertEquals("org.javabeanstack.data.DBManager", config.classes().get(0));
        assertEquals(1, config.mappingFiles().size());
        assertEquals("META-INF/orm-extra.xml", config.mappingFiles().get(0));

        Map<String, String> props = config.properties();
        assertEquals(11, props.size(), "deben leerse TODAS las properties de la unidad");
        assertEquals("PU2", props.get("jbs.dynamic.metamodel.pu"));
        assertEquals("org.hibernate.dialect.MkSqlServer2008", props.get("hibernate.dialect"));
        assertEquals("0", props.get("hibernate.max_fetch_depth"));
        assertEquals("false", props.get("hibernate.jpa.compliance.global_id_generators"));
        assertEquals("false", props.get("hibernate.generate_statistics"));
        assertEquals("true", props.get("hibernate.format_sql"));
        assertEquals("false", props.get("hibernate.show_sql"));
        assertEquals("datos", props.get("hibernate.default_schema"));
        assertEquals("SQLSERVER", props.get("jbs.dbengine"));
        assertEquals("java:app/em/PU3", props.get("jboss.entity.manager.jndi.name"));
        assertEquals("false", props.get("wildfly.jpa.managed"));
    }

    /**
     * Cada unidad puede tener configuración PROPIA e independiente: PU5 usa
     * dialecto PostgreSQL, schema public y otro datasource — la razón de ser
     * de V30 frente a la plantilla única de V21.
     */
    @Test
    public void test03ConfiguracionPropiaPorUnidad() {
        DBManagerV30Probado manager = new DBManagerV30Probado();
        DBManagerV30.PersistenceUnitConfig pu3 = manager.getConfigs().get("PU3");
        DBManagerV30.PersistenceUnitConfig pu5 = manager.getConfigs().get("PU5");

        assertEquals("org.hibernate.dialect.PostgreSQLDialect", pu5.properties().get("hibernate.dialect"));
        assertEquals("public", pu5.properties().get("hibernate.default_schema"));
        assertEquals("POSTGRES", pu5.properties().get("jbs.dbengine"));
        assertEquals("jdbc/MakerPostgresDS_5", pu5.jtaDataSource());
        assertFalse(pu3.properties().get("hibernate.dialect")
                .equals(pu5.properties().get("hibernate.dialect")),
                "los dialectos por unidad deben poder diferir");
    }

    /**
     * Una unidad mínima (PU6, declarada como pu6 sin provider) debe asumir los
     * defaults: proveedor Hibernate, sin clases extra ni mapping-files.
     */
    @Test
    public void test04DefaultsDeUnidad() {
        DBManagerV30Probado manager = new DBManagerV30Probado();
        DBManagerV30.PersistenceUnitConfig config = manager.getConfigs().get("PU6");

        assertNotNull(config);
        assertEquals("PU6", config.name());
        assertEquals("org.hibernate.jpa.HibernatePersistenceProvider", config.provider(),
                "sin <provider> debe asumir Hibernate");
        assertTrue(config.classes().isEmpty());
        assertTrue(config.mappingFiles().isEmpty());
        assertEquals("jdbc/Maker950DS_6", config.jtaDataSource());
    }

    /**
     * Decisión dinámico/estático: las unidades del archivo son dinámicas; PU1
     * y PU2 son SIEMPRE estáticas (PU1 incluso figurando en el archivo); la
     * plantilla DEFAULT no es una unidad real; una unidad no definida no es
     * dinámica hasta que el último recurso la resuelva.
     */
    @Test
    public void test05EsDinamica() {
        DBManagerV30Probado manager = new DBManagerV30Probado();

        assertTrue(manager.isDynamic("PU3"));
        assertTrue(manager.isDynamic("PU5"));
        assertTrue(manager.isDynamic("PU6"));
        assertFalse(manager.isDynamic("PU1"), "PU1 es siempre estática aunque esté en el archivo");
        assertFalse(manager.isDynamic("PU2"), "PU2 es siempre estática");
        assertFalse(manager.isDynamic("DEFAULT"), "la plantilla no es una unidad real");
        assertFalse(manager.isDynamic("PU99"), "una unidad no definida no es dinámica de entrada");
    }

    /**
     * Derivación desde la plantilla DEFAULT: una unidad no definida (PU11)
     * hereda provider, clases y propiedades de la plantilla, con el
     * placeholder {n} sustituido por el número de la unidad tanto en el
     * datasource como en los valores de las properties.
     */
    @Test
    public void test06PlantillaDefault() {
        DBManagerV30Probado manager = new DBManagerV30Probado();
        DBManagerV30.PersistenceUnitConfig config = manager.resolveConfig("PU11");

        assertEquals("PU11", config.name());
        assertEquals("jdbc/Maker950DS_11", config.jtaDataSource(),
                "{n} del datasource debe sustituirse con el número de la unidad");
        assertEquals("11", config.properties().get("jbs.test.unit.number"),
                "{n} en valores de properties también debe sustituirse");
        assertEquals("org.hibernate.dialect.MkSqlServer2008", config.properties().get("hibernate.dialect"));
        assertEquals("org.hibernate.jpa.HibernatePersistenceProvider", config.provider());
        assertEquals(1, config.classes().size(), "debe heredar las clases extra de la plantilla");
        //Una unidad definida explícitamente NO debe derivarse de la plantilla
        assertEquals("jdbc/MakerPostgresDS_5", manager.resolveConfig("PU5").jtaDataSource());
    }

    /**
     * Errores de resolución con mensaje claro: unidad sin número derivable
     * (PUX) y unidad no definida cuando el archivo no tiene plantilla DEFAULT.
     */
    @Test
    public void test07ErroresDeResolucion() {
        DBManagerV30Probado manager = new DBManagerV30Probado();
        assertThrows(IllegalStateException.class, () -> manager.resolveConfig("PUX"),
                "sin número derivable debe fallar con error claro");

        DBManagerV30Probado sinDefault = new DBManagerV30Probado(ARCHIVO_SIN_DEFAULT);
        assertEquals("jdbc/Maker950DS_3", sinDefault.resolveConfig("PU3").jtaDataSource(),
                "las unidades explícitas deben resolver igual sin plantilla");
        assertThrows(IllegalStateException.class, () -> sinDefault.resolveConfig("PU9"),
                "sin plantilla DEFAULT una unidad no definida debe fallar con error claro");
    }

    /**
     * Aplicación de la configuración al PersistenceConfiguration: provider,
     * transacción JTA, datasource, TODAS las propiedades de negocio con sus
     * valores exactos, el FILTRADO de las de control (jbs.dynamic.*) y las del
     * contenedor (jboss.*, wildfly.*), las clases extra y los mapping-files.
     */
    @Test
    public void test08AplicacionAlPersistenceConfiguration() throws Exception {
        DBManagerV30Probado manager = new DBManagerV30Probado();
        PersistenceConfiguration cfg = manager.buildConfiguration(manager.resolveConfig("PU3"));

        assertEquals("PU3", cfg.name());
        assertEquals("org.hibernate.jpa.HibernatePersistenceProvider", cfg.provider());
        assertEquals(PersistenceUnitTransactionType.JTA, cfg.transactionType());
        assertEquals("jdbc/Maker950DS_3", cfg.jtaDataSource());

        Map<String, Object> props = cfg.properties();
        assertEquals("org.hibernate.dialect.MkSqlServer2008", props.get("hibernate.dialect"));
        assertEquals("0", props.get("hibernate.max_fetch_depth"));
        assertEquals("false", props.get("hibernate.jpa.compliance.global_id_generators"));
        assertEquals("false", props.get("hibernate.generate_statistics"));
        assertEquals("true", props.get("hibernate.format_sql"));
        assertEquals("false", props.get("hibernate.show_sql"));
        assertEquals("datos", props.get("hibernate.default_schema"));
        assertEquals("SQLSERVER", props.get("jbs.dbengine"));
        assertFalse(props.containsKey("jbs.dynamic.metamodel.pu"),
                "las propiedades de control jbs.dynamic.* deben filtrarse");
        assertFalse(props.containsKey("jboss.entity.manager.jndi.name"),
                "las propiedades jboss.* deben filtrarse");
        assertFalse(props.containsKey("wildfly.jpa.managed"),
                "las propiedades wildfly.* deben filtrarse");

        assertTrue(cfg.managedClasses().contains(DBManager.class),
                "las clases extra deben registrarse en el PersistenceConfiguration");
        assertTrue(cfg.mappingFiles().contains("META-INF/orm-extra.xml"),
                "los mapping-files deben registrarse en el PersistenceConfiguration");

        //Una unidad derivada de la plantilla también debe llegar con {n} sustituido
        PersistenceConfiguration cfg11 = manager.buildConfiguration(manager.resolveConfig("PU11"));
        assertEquals("jdbc/Maker950DS_11", cfg11.jtaDataSource());
        assertEquals("11", cfg11.properties().get("jbs.test.unit.number"));
    }

    /**
     * Fiabilidad: sin archivo de configuración la clase degrada al
     * comportamiento del DBManager clásico (ninguna unidad dinámica), sin
     * lanzar excepciones.
     */
    @Test
    public void test09ArchivoInexistente() {
        DBManagerV30Probado manager = new DBManagerV30Probado(ARCHIVO_INEXISTENTE);
        assertTrue(manager.getConfigs().isEmpty());
        assertFalse(manager.isDynamic("PU3"), "sin archivo todas las unidades son estáticas");
    }

    /**
     * Fiabilidad: un archivo corrupto (XML mal formado) tampoco debe romper
     * nada — se loguea y se degrada al comportamiento clásico.
     */
    @Test
    public void test10ArchivoCorrupto() {
        DBManagerV30Probado manager = new DBManagerV30Probado(ARCHIVO_CORRUPTO);
        assertTrue(manager.getConfigs().isEmpty());
        assertFalse(manager.isDynamic("PU3"));
    }

    /**
     * El archivo se lee UNA sola vez por instancia (cache) y la configuración
     * resultante es inmutable (map y listas de solo lectura).
     */
    @Test
    public void test11CacheEInmutabilidad() {
        DBManagerV30Probado manager = new DBManagerV30Probado();
        Map<String, DBManagerV30.PersistenceUnitConfig> primera = manager.getConfigs();
        assertSame(primera, manager.getConfigs(), "el archivo debe leerse una sola vez");

        assertThrows(UnsupportedOperationException.class,
                () -> primera.put("PUX", null), "el map de configuraciones debe ser inmutable");
        DBManagerV30.PersistenceUnitConfig pu3 = primera.get("PU3");
        assertThrows(UnsupportedOperationException.class,
                () -> pu3.classes().add("x"), "las listas de la config deben ser inmutables");
        assertThrows(UnsupportedOperationException.class,
                () -> pu3.properties().put("x", "y"), "las properties de la config deben ser inmutables");
    }

    /**
     * Contrato básico y robustez ante entradas inválidas: estrategia por
     * sesión (como el DBManager clásico), claves nulas/vacías/mal formadas
     * devuelven null sin explotar, y los métodos de cierre toleran argumentos
     * nulos o inexistentes.
     */
    @Test
    public void test12ContratoBasico() {
        DBManagerV30Probado manager = new DBManagerV30Probado();

        assertEquals(IDBManager.PERSESSION, manager.getEntityIdStrategic());
        assertNull(manager.getEntityManager(null));
        assertNull(manager.getEntityManager(""));
        assertNull(manager.getEntityManager("CLAVE_SIN_SEPARADOR"));
        assertNull(manager.createEntityManager(null));
        assertNull(manager.createEntityManager(""));

        manager.closeEntityManager(null);
        manager.closeEntityManagers();
        manager.closeFactory(null);
        manager.closeFactory("");
        manager.closeFactory("PU_INEXISTENTE");
    }
}

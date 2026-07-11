# Gestión dinámica de unidades de persistencia — DBManagerV30

> Familia de documentos: [STATIC_MANAGMENT_DBMANAGER.md](STATIC_MANAGMENT_DBMANAGER.md) (esquema tradicional) · [DINAMIC_DATA_MANAGMENT_DBMANAGER_V21.md](DINAMIC_DATA_MANAGMENT_DBMANAGER_V21.md) (variante por plantilla en persistence.xml) · [DINAMIC_DATA_MANAGMENT_DBMANAGER_V20.md](DINAMIC_DATA_MANAGMENT_DBMANAGER_V20.md) (variante por system properties).
> Implementado el 2026-07-11 sobre JavaBeanStack (rama `dbmanager-v30`), Maker y TestProject.
> Stack: Jakarta EE 11, Hibernate 7.3.2.Final (Jakarta Persistence 3.2), WildFly 40.

## 1. Problema que resuelve

`DBManagerV21` fabrica las unidades de persistencia dinámicas desde una plantilla **única** (`DINAMIC_PU` comentada en `persistence.xml`): todas las empresas comparten la misma configuración — mismo dialecto, mismo schema, mismas propiedades Hibernate. Esa uniformidad es una limitación real: cada empresa puede necesitar propiedades propias (el caso clave es un **dialecto distinto por empresa**, esencial para la migración gradual SQL Server → PostgreSQL).

`DBManagerV30` resuelve esto con un archivo de configuración dedicado, **`META-INF/dynamic_persistence.xml`**, con el mismo esquema que `persistence.xml` pero conteniendo **solo las unidades dinámicas, cada una con su spec completa e independiente**.

Ventaja estructural adicional: como el archivo **no se llama `persistence.xml`**, el contenedor no lo escanea y nada bootea en el arranque — desaparece el "hack" del comentario XML que necesita V21 (recordar: `wildfly.jpa.managed=false` no funciona en WildFly 40).

## 2. Arquitectura

Cuatro implementaciones del mismo contrato `IDBManager`, **conmutables por `ejb-jar.xml`** sin recompilar:

| | `DBManager` | `DBManagerV20` | `DBManagerV21` | `DBManagerV30` |
|---|---|---|---|---|
| Estructura | base | independiente | extiende `DBManager` | **independiente (autocontenida)** |
| PUs dinámicas | no | sí | sí | sí |
| Configuración | persistence.xml | system properties JVM | plantilla `DINAMIC_PU` comentada | **`dynamic_persistence.xml`** |
| Config por unidad | — | overrides parciales | única (compartida) | **completa e independiente** |
| Dialecto por empresa | no | limitado | no | **sí** |

`DBManagerV30` **no hereda** de las otras clases: es autocontenida, implementa `IDBManager` directamente y todo su comportamiento se lee de arriba a abajo en un único archivo (`business/src/main/java/org/javabeanstack/data/DBManagerV30.java`, JavaBeanStack). Decisión deliberada: se aceptó duplicar ~100 líneas del camino clásico a cambio de claridad y de poder evolucionar V30 sin riesgo de romper las versiones ya validadas.

La selección se hace en el session bean `DBManager` de cada `ejb-jar.xml` (Maker-services, Maker-web, Maker-rest):

```xml
<session>
    <ejb-name>DBManager</ejb-name>
    <ejb-class>org.javabeanstack.data.DBManager</ejb-class>
    <!--<ejb-class>org.javabeanstack.data.DBManagerV20</ejb-class>-->
    <!--<ejb-class>org.javabeanstack.data.DBManagerV21</ejb-class>-->
    <!--<ejb-class>org.javabeanstack.data.DBManagerV30</ejb-class>-->
    <business-local>org.javabeanstack.data.IDBManager</business-local>
    <session-type>Singleton</session-type>
</session>
```

## 3. El archivo `dynamic_persistence.xml`

Vive en `Maker-model/src/main/resources/META-INF/dynamic_persistence.xml` (mismo jar que `persistence.xml`, viaja dentro del EAR y de los WARs). Formato idéntico a `persistence.xml` 3.2:

```xml
<persistence version="3.2" xmlns="https://jakarta.ee/xml/ns/persistence" ...>
    <persistence-unit name="PU3" transaction-type="JTA">
        <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
        <jta-data-source>jdbc/Maker950DS_3</jta-data-source>
        <class>net.makerapp.model.tables.converter.StringTrimConverter</class>
        <properties>
            <property name="jbs.dynamic.metamodel.pu" value="PU2"/>
            <property name="hibernate.dialect" value="org.hibernate.dialect.MkSqlServer2008"/>
            <property name="hibernate.default_schema" value="datos"/>
            <property name="jbs.dbengine" value="SQLSERVER"/>
            <!-- ...resto de propiedades propias de ESTA unidad... -->
        </properties>
    </persistence-unit>

    <!--Plantilla de las unidades no definidas explícitamente-->
    <persistence-unit name="DEFAULT" transaction-type="JTA">
        <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
        <jta-data-source>jdbc/Maker950DS_{n}</jta-data-source>
        ...
    </persistence-unit>
</persistence>
```

Elementos honrados por unidad:

| Elemento | Uso |
|---|---|
| `name` | Nombre de la unidad (PU3..PUn) o `DEFAULT` para la plantilla. |
| `<provider>` | Proveedor de persistencia; default Hibernate si se omite. |
| `<jta-data-source>` | Datasource JTA de la unidad (**obligatorio**; sin él la unidad se descarta con warning). En la plantilla `DEFAULT` lleva el placeholder `{n}`. |
| `<class>` | Clases **extra** que el metamodelo no expone (converters, etc.); se suman, no reemplazan. |
| `<mapping-file>` | Archivos orm.xml adicionales. |
| `<properties>` | Propiedades propias de la unidad (dialecto, schema, `jbs.dbengine`, `hibernate.*`). Las de control `jbs.dynamic.*` y las del contenedor (`jboss.*`, `wildfly.*`) se filtran y no llegan al `PersistenceConfiguration`. |

Propiedad de control por unidad (con default global):

| Propiedad | Default | Uso |
|---|---|---|
| `jbs.dynamic.metamodel.pu` | `PU2` | Unidad **declarada** de cuyo metamodelo (`getMetamodel().getManagedTypes()`) se toman las ~300 clases del modelo — `PersistenceConfiguration` no tiene autodetección de clases. |

System properties opcionales de la JVM (no requeridas en Maker):

| Property | Default | Uso |
|---|---|---|
| `jbs.dynamic.persistence.file` | `META-INF/dynamic_persistence.xml` | Ruta del archivo en el classpath. |
| `jbs.dynamic.template.unit` | `DEFAULT` | Nombre de la unidad plantilla. |

## 4. Reglas de resolución

Para cada clave `"PU:sessionId"` que recibe `getEntityManager(key)`:

1. **PU1 y PU2**: SIEMPRE estáticas (lookup JNDI `java:app/em/PUn`), salvaguarda hardcodeada — si figuran en el archivo se ignoran con warning.
2. **PU definida en `dynamic_persistence.xml`**: camino dinámico (**dinámico-primero**: gana aunque la PU también esté declarada en `persistence.xml` — así el switch de `ejb-jar.xml` decide de verdad y `persistence.xml` puede quedar intacto durante la transición).
3. **PU no definida en el archivo**: camino estático clásico (JNDI, cache por `(PU:sessionId)`, purga por ociosidad de 5 minutos).
4. **PU no definida NI declarada** (lookup JNDI → `NameNotFoundException`): se fabrica desde la plantilla `DEFAULT` sustituyendo `{n}` por el número de la unidad (PU11 → `jdbc/Maker950DS_11`) — **alta de empresa sin redeploy**. Sin plantilla, error claro en el log y `null`.

**Fiabilidad**: si el archivo no existe o está corrupto, se loguea la advertencia y la clase opera exactamente como el `DBManager` clásico; una unidad individual inválida se descarta con warning sin afectar a las demás. Nunca se rompe el arranque del deployment.

## 5. Ciclo de vida de los EntityManager

Idéntico al esquema validado en V21, implementado localmente:

- **Con transacción JTA activa**: un EM por transacción y unidad, asociado vía `TransactionSynchronizationRegistry` (`putResource`) y cerrado determinísticamente en `Synchronization.afterCompletion`.
- **Sin transacción** (lecturas): un EM por unidad y thread en el cache, con `em.clear()` en cada reuso (contexto fresco, misma semántica que el EM transaction-scoped del camino estático). Lo cierra de inmediato `closeEntityManagers()` — invocado por el `@AroundInvoke` de `AbstractDAO` al finalizar cada método de negocio — y la purga de 5 minutos actúa como respaldo.
- **Cache unificado**: un solo mapa `(clave → EmEntry)` guarda tanto los EM estáticos (container-managed, la purga solo los descarta) como los dinámicos sin transacción (application-managed, la purga además los cierra). Un mapa, una purga, una regla.
- **`closeFactory(pu)`**: cierra y descarta el EMF dinámico de una unidad (baja de empresa en caliente); el próximo acceso lo refabrica.
- **`@PreDestroy`**: al bajar el deployment se cierran todos los EM application-managed y todos los EMF dinámicos (mitiga fugas en el redeploy).

## 6. Implementación paso a paso

1. **JavaBeanStack**: la clase `DBManagerV30` vive en `business/.../data/DBManagerV30.java`. No requiere cambios en `IDBManager`, `DBManager`, `AbstractDAO` ni en las otras versiones.
   ```bash
   cd JavaBeanStack && mvn -DskipTests clean install
   ```
2. **Maker-model**: crear/mantener `src/main/resources/META-INF/dynamic_persistence.xml` con las unidades dinámicas (una spec completa por unidad) y la plantilla `DEFAULT`. `persistence.xml` puede quedar **intacto** (PU1..PU10 declaradas): la resolución dinámico-primero las ignora cuando V30 está activo, y la reversibilidad al clásico es instantánea. El costo transitorio: WildFly bootea las SessionFactory declaradas aunque no se usen; el ahorro de memoria llega al eliminar PU3..PU10 de `persistence.xml` cuando V30 se consagre.
3. **ejb-jar.xml** (×3: `Maker-services/src/main/resources/META-INF/`, `Maker-web/src/main/webapp/WEB-INF/`, `Maker-rest/src/main/webapp/WEB-INF/`): activar `org.javabeanstack.data.DBManagerV30` en el `ejb-class` del bean `DBManager`.
4. **Compilar y desplegar** (siempre install completo: el archivo viaja en Maker-model.jar):
   ```bash
   cd maker && mvn -DskipTests clean install
   cp Maker-ear/target/Maker-ear-*.ear $WILDFLY/standalone/deployments/
   ```
5. **Alta de una empresa nueva** (PU11+): crear la BD y el datasource `jdbc/Maker950DS_11` en WildFly, registrar la empresa en el catálogo con `persistentUnit=PU11` — la plantilla `DEFAULT` hace el resto, **sin tocar XML ni redesplegar**. Si la empresa necesita configuración propia (otro dialecto), agregar su bloque explícito al archivo y redesplegar.

## 7. Validación realizada (2026-07-11, WildFly 40 local)

| Chequeo | Resultado |
|---|---|
| Boot TestProject/Maker: unidades del archivo NO bootean (`HHH008540` solo para las declaradas; `DEFAULT` = 0 apariciones) | OK |
| Lectura del archivo: `Configuración dinámica leída de vfs:...Maker-model-...jar/META-INF/dynamic_persistence.xml: [PU3..PU10, DEFAULT]` en `oymframe.log` | OK |
| Dinámico-primero: PU3/PU8 declaradas en persistence.xml → `Creando EntityManagerFactory dinámico PU8, datasource: jdbc/Maker950DS_8` | OK |
| Plantilla `DEFAULT`: empresa en PU4 no definida (TestProject) → `Unidad de persistencia no declarada, se fabrica desde la plantilla DEFAULT: PU4` | OK |
| Paridad de tests: `AbstractDAOTest` (jbs, empresas 3 y 4) y `CtbmovimientoSrvTest1` (Maker, empresa 8) con **idéntico** resultado V30 vs clásico (las fallas son de datos de prueba, preexistentes) | OK |
| Reversa: `ejb-jar.xml` de vuelta al clásico → cero trazas dinámicas, PUs por JNDI | OK |

## 8. Consideraciones

- **PU1 y PU2 jamás son dinámicas** (restricción de diseño): el catálogo y la unidad base/metamodelo deben estar declaradas y arrancadas por el contenedor.
- La unidad de metamodelo (`jbs.dynamic.metamodel.pu`, default PU2) **debe seguir declarada** en `persistence.xml`: de su EMF se toman las clases del modelo.
- El parser XML está endurecido (secure processing + `disallow-doctype-decl`, anti-XXE) y el archivo se lee **una sola vez** por ciclo de vida del singleton (un redeploy lo relee).
- Cada WAR/EAR que embebe las PUs resuelve su propio archivo (namespace `java:app`), igual que con `persistence.xml`.
- El `DBManager` clásico, V20 y V21 **ignoran por completo** `dynamic_persistence.xml`: el archivo es inerte salvo que V30 esté activo.

## 9. Propiedades Hibernate adicionales por unidad (tuning, segunda caché / Infinispan)

**El transporte de propiedades es abierto por diseño**: cualquier propiedad de una unidad en `dynamic_persistence.xml` cuyo prefijo no sea `jbs.dynamic.*`, `jboss.*` ni `wildfly.*` se copia **tal cual y por unidad** al `PersistenceConfiguration` al fabricar el EMF. No hay lista blanca — llegan directo a Hibernate, por ejemplo:

```xml
<property name="hibernate.cache.use_second_level_cache" value="true"/>
<property name="hibernate.cache.region.factory_class" value="..."/>
<property name="hibernate.jdbc.batch_size" value="50"/>
<property name="hibernate.query.plan_cache_max_size" value="2048"/>
<property name="hibernate.generate_statistics" value="true"/>
```

Esto permite un **tuning distinto por empresa** sin tocar código (verificado por el test unitario `DBManagerV30Test.test08AplicacionAlPersistenceConfiguration`, que asegura que cada propiedad llega con su valor exacto y que las de control se filtran).

**Matiz importante con Infinispan (segunda caché)**: en las PUs declaradas en `persistence.xml` la integración con Infinispan la hace el **subsistema JPA de WildFly** (inyecta la region factory y los cache containers automáticamente). Los EMF de V30 son *application-managed*, fabricados por fuera de ese subsistema, así que esa integración automática **no ocurre**. Para activar la segunda caché en una unidad dinámica se necesita además:

1. **Classpath**: que el módulo de Infinispan/hibernate-cache sea visible al classloader del deployment — dependencia en `jboss-deployment-structure.xml` o en el `MANIFEST` (ej. `org.infinispan.hibernate-cache`).
2. **Region factory explícita**: configurar en las properties de la unidad la region factory con su configuración embebida (ej. `hibernate.cache.region.factory_class=infinispan` + config propia), **no** la gestionada por WildFly. Notar que las propiedades `wildfly.*` se filtran a propósito: los hints específicos del contenedor no aplican a un EMF application-managed.

En resumen: el mecanismo de V30 ya soporta estas propiedades sin cambio de código; el trabajo adicional para Infinispan es de classpath y configuración, no de `DBManagerV30`. Si se decide activarlo, **validar primero en TestProject**.

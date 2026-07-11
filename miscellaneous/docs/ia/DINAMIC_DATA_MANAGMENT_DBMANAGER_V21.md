# Gestión dinámica de unidades de persistencia — DBManagerV21

> Familia de documentos: [STATIC_MANAGMENT_DBMANAGER.md](STATIC_MANAGMENT_DBMANAGER.md) (esquema tradicional) · [DINAMIC_DATA_MANAGMENT_DBMANAGER_V20.md](DINAMIC_DATA_MANAGMENT_DBMANAGER_V20.md) (variante por system properties) · [DINAMIC_DATA_MANAGMENT_DBMANAGER_V30.md](DINAMIC_DATA_MANAGMENT_DBMANAGER_V30.md) (variante por archivo dynamic_persistence.xml, una spec por unidad).
> Implementado el 2026-07-10 sobre JavaBeanStack (rama `dinamic-dbmanager`, mergeada a `master`, commit `4c2a88d`), Maker (`9aa1bc9`) y TestProject (`c8a3bd1`).
> Stack: Jakarta EE 11, Hibernate 7.3.2.Final (Jakarta Persistence 3.2), WildFly 40.

## 1. Problema que resuelve

En el esquema tradicional (ver documento complementario) cada empresa del ERP requiere una unidad de persistencia **declarada** en `persistence.xml` (PU2..PU10). Consecuencias:

- WildFly arranca **todas** las SessionFactory en el boot, en **cada deployment** que embebe las PUs (Maker-ear, Maker-web, Maker-rest → hasta 30 SF vivas), inflando el footprint de memoria (factor agravante del OOM de redeploy investigado el 2026-07-09).
- Dar de alta una empresa nueva exige editar `persistence.xml`, recompilar y redesplegar.
- Tope práctico de 10 empresas (PU1..PU10).

`DBManagerV21` elimina esas restricciones: fabrica el `EntityManagerFactory` de cada empresa **bajo demanda, en runtime**, con `jakarta.persistence.PersistenceConfiguration` (API nueva de Persistence 3.2). No se toca ninguna clase de Maker: todo vive en JavaBeanStack.

## 2. Arquitectura

Dos implementaciones del mismo contrato `IDBManager`, **conmutables por `ejb-jar.xml`** sin recompilar:

| | `DBManager` (clásico) | `DBManagerV21` |
|---|---|---|
| Clase | `org.javabeanstack.data.DBManager` (idéntico a siempre) | `org.javabeanstack.data.DBManagerV21` (extiende al clásico) |
| PUs declaradas | lookup JNDI `java:app/em/PUn` | ídem (hereda el path del clásico) |
| PUs dinámicas | no soporta | `PersistenceConfiguration` + plantilla `DINAMIC_PU` |
| Configuración | `persistence.xml` | `persistence.xml` + plantilla `DINAMIC_PU` **comentada** |

La selección se hace en el session bean `DBManager` de cada `ejb-jar.xml`:

```xml
<session>
    <ejb-name>DBManager</ejb-name>
    <!--Elegir la implementación:-->
    <ejb-class>org.javabeanstack.data.DBManager</ejb-class>
    <!--<ejb-class>org.javabeanstack.data.DBManagerV21</ejb-class>-->
    <business-local>org.javabeanstack.data.IDBManager</business-local>
    <session-type>Singleton</session-type>
</session>
```

`IDBManager` incorporó tres métodos **`default`** (cuerpo vacío) — `closeEntityManager(em)`, `closeEntityManagers()`, `closeFactory(pu)` — de modo que el `DBManager` clásico compila y funciona sin ningún cambio, y `DBManagerV21` los sobreescribe con la lógica real.

## 3. La plantilla `DINAMIC_PU`

Toda la configuración dinámica vive en un bloque `persistence-unit` llamado `DINAMIC_PU` que va **dentro de un comentario XML** en `META-INF/persistence.xml` (Maker-model). Al estar comentado, el contenedor jamás lo arranca; `DBManagerV21` lo parsea del classpath, incluso desde dentro del comentario.

> **Por qué comentada y no `wildfly.jpa.managed=false`:** se verificó empíricamente en WildFly 40 que esa propiedad **no** evita el arranque de la SessionFactory (bootea igual, con conexión a la BD). El comentario XML es la única forma garantizada de que el contenedor la ignore.

```xml
<!--PLANTILLA de las unidades de persistencia dinámicas. Está deshabilitada
    (comentada) a propósito: el contenedor no la arranca, pero DBManagerV21
    la parsea de este archivo.
<persistence-unit name="DINAMIC_PU" transaction-type="JTA">
    <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
    <exclude-unlisted-classes>false</exclude-unlisted-classes>
    <properties>
        <property name="jbs.dynamic.units" value="*"/>
        <property name="jbs.dynamic.static.units" value="PU1,PU2"/>
        <property name="jbs.dynamic.datasource.pattern" value="jdbc/Maker950DS_{n}"/>
        <property name="jbs.dynamic.metamodel.pu" value="PU2"/>
        <property name="jbs.dynamic.classes.extra" value="net.makerapp.model.tables.converter.StringTrimConverter"/>
        <property name="hibernate.dialect" value="org.hibernate.dialect.MkSqlServer2008"/>
        <property name="hibernate.max_fetch_depth" value="0"/>
        <property name="hibernate.jpa.compliance.global_id_generators" value="false"/>
        <property name="hibernate.generate_statistics" value="false"/>
        <property name="hibernate.format_sql" value="true"/>
        <property name="hibernate.show_sql" value="false"/>
        <property name="hibernate.default_schema" value="datos"/>
        <property name="jbs.dbengine" value="SQLSERVER"/>
    </properties>
</persistence-unit>
-->
```

**Regla del bloque comentado:** no puede contener comentarios internos (`<!-- -->` anidados es XML inválido) ni la secuencia `--`.

### Referencia de propiedades

| Propiedad | Obligatoria | Descripción |
|---|---|---|
| `jbs.dynamic.units` | sí | Unidades a resolver dinámicamente. Lista (`PU3,PU4,PU8`) o `*`. Las listadas se resuelven **dinámico-primero aunque estén declaradas** en persistence.xml — así el switch del ejb-jar decide de verdad. Con `*`: toda PU salvo las de `static.units`, incluso PU11+ sin tocar el archivo. |
| `jbs.dynamic.static.units` | no (default `PU1,PU2`) | Unidades que siempre van por JNDI (catálogo y la PU de metamodelo). |
| `jbs.dynamic.datasource.pattern` | sí | Patrón JNDI del datasource; `{n}` se sustituye por el número de la PU (`PU7` → `jdbc/Maker950DS_7`). Los datasources deben existir en WildFly. |
| `jbs.dynamic.metamodel.pu` | no (default `PU2`) | PU **declarada** de cuyo metamodelo (`getMetamodel().getManagedTypes()`) se toman las ~600 clases de entidades (PersistenceConfiguration no autodetecta clases). |
| `jbs.dynamic.classes.extra` | según modelo | Clases que el metamodelo no expone — en Maker, los AttributeConverter (`StringTrimConverter`). Separadas por coma o punto y coma. |
| `jbs.dynamic.provider` | no (default Hibernate) | FQCN del proveedor de persistencia. |
| `jbs.dynamic.mapping.files` | no | Archivos orm.xml adicionales, separados por coma. |
| resto de `<property>` | — | Se aplican tal cual a cada EMF dinámico (dialecto, schema, `jbs.dbengine`, `hibernate.*`). |

System properties opcionales (JVM): `jbs.dynamic.template.pu` (nombre de la plantilla, default `DINAMIC_PU`), `jbs.persistence.jndi.em.prefix` / `jbs.persistence.jndi.emf.prefix` (defaults `java:app/em/` y `java:app/emf/`).

**Fallback:** si la plantilla no existe en ningún `persistence.xml` del classpath, V21 clona las propiedades del EMF de PU2 (requiere entonces que las propiedades `jbs.dynamic.*` estén declaradas en PU2).

## 4. Funcionamiento interno de DBManagerV21

Flujo de `getEntityManager("PUn:sessionId")`:

1. **¿Es dinámica?** — según `jbs.dynamic.units` (o porque ya cayó al fallback). Si no → `super.getEntityManager(key)` (path JNDI clásico intacto). Si el lookup clásico devuelve null **y** la PU no está declarada (verificación `NameNotFoundException`) → se marca dinámica y sigue (fallback para PUs futuras no declaradas).
2. **EMF**: `dynamicFactories.computeIfAbsent(pu, this::buildFactory)` — se fabrica **una sola vez** por PU:
   - datasource = patrón con `{n}` sustituido;
   - propiedades de la plantilla (menos `jbs.dynamic.*`, `jboss.*`, `wildfly.*`);
   - clases del metamodelo de `metamodel.pu` + `classes.extra` + `mapping.files`;
   - `PersistenceUnitTransactionType.JTA`; la integración con el TM de WildFly la resuelve Hibernate por autodetección (validado — no hace falta `hibernate.transaction.jta.platform`).
3. **EntityManager** (application-managed, hay que cerrarlos — a diferencia de los tx-scoped del path JNDI):
   - **Con transacción JTA activa**: un EM por (transacción, PU) registrado en el `TransactionSynchronizationRegistry` (`putResource`/`getResource`) con `joinTransaction()`; una `Synchronization` interpuesta lo cierra en `afterCompletion`. Cierre determinístico al commit/rollback.
   - **Sin transacción** (lecturas `SUPPORTS`): un EM por (PU, thread) en un mapa con timestamp; se reusa con `em.clear()` (conserva la semántica de contexto fresco por lectura del esquema tradicional) y lo cierra el **purge por ociosidad** (5 minutos sin uso, mismo criterio que el cache del clásico). `closeEntityManagers()` permite liberar el thread actual en forma inmediata si algún punto del framework lo necesita.
4. **Ciclo de vida del EMF**: `closeFactory("PUn")` lo cierra y descarta en caliente (baja de empresa); un acceso posterior lo vuelve a fabricar. `@PreDestroy` cierra todos los EMs y EMFs dinámicos al bajar el deployment (mitiga fugas en el redeploy).

Notas de comportamiento equivalente garantizadas por diseño: `getPersistUnitProp`/`getDataEngine`/`getSchema`/`getQueryConstants` de `AbstractDAO` siguen funcionando porque las propiedades (`jbs.dbengine`, `hibernate.default_schema`, …) se copian al EMF dinámico.

## 5. Implementación paso a paso en Maker

### Paso 0 — Prerrequisitos
- JavaBeanStack `master` ≥ commit `4c2a88d` instalado en el repo local/Nexus (`mvn -DskipTests install` en JavaBeanStack).
- Los datasources de las empresas creados en WildFly (`standalone.xml`), siguiendo el patrón (`jdbc/Maker950DS_3`, `_4`, …).
- Cada empresa registrada en `catalogo.appcompany` con su `persistentunit` (`PU3`, `PU4`, …) — igual que siempre.

### Paso 1 — Plantilla en persistence.xml
En `Maker-model/src/main/resources/META-INF/persistence.xml`, agregar al final (antes de `</persistence>`) el bloque `DINAMIC_PU` **comentado** de la sección 3. Ya está hecho en el commit `9aa1bc9`; para otra instalación, copiar el bloque y ajustar el patrón de datasource.

Las PUs declaradas (PU1..PU10) pueden quedar como están durante la transición — ver Paso 5.

### Paso 2 — Conmutar la implementación en ejb-jar.xml
En los **tres** descriptores (`Maker-services/src/main/resources/META-INF/ejb-jar.xml`, `Maker-web/src/main/webapp/WEB-INF/ejb-jar.xml`, `Maker-rest/src/main/webapp/WEB-INF/ejb-jar.xml`), activar V21:

```xml
<!--<ejb-class>org.javabeanstack.data.DBManager</ejb-class>-->
<ejb-class>org.javabeanstack.data.DBManagerV21</ejb-class>
```

Se puede conmutar un deployment a la vez (p. ej. solo el EAR) — cada uno tiene su propio singleton.

### Paso 3 — Reconstruir y desplegar
```bash
mvn -DskipTests clean install     # desde la raíz de maker
```
**Importante:** tras tocar `persistence.xml` reconstruir con `install` (o `-pl <módulo> -am`); un rebuild de solo `Maker-ear` toma el `Maker-model` viejo del repo local. Desplegar EAR y WARs.

### Paso 4 — Verificar
1. En el boot no debe aparecer `Processing PersistenceUnitInfo [name: DINAMIC_PU]` (la plantilla no bootea).
2. Al primer acceso de una empresa dinámica, en `standalone/log/oymframe.log` (ahí escribe el log4j2 de jbs, **no** en server.log):
   ```
   INFO [org.javabeanstack.data.DBManagerV21] Plantilla dinámica DINAMIC_PU leída (comentada) de vfs:/content/...persistence.xml
   INFO [org.javabeanstack.data.DBManagerV21] --------- Creando EntityManagerFactory dinámico --------- PU8, datasource: jdbc/Maker950DS_8
   ```
3. Operar normalmente contra esa empresa (login, ABM) y/o correr los tests de integración:
   ```bash
   APP_IDCOMPANY=8 mvn -pl Maker-services test
   ```

### Paso 5 — (Fase final, opcional) eliminar las PUs declaradas
Mientras PU3..PU10 sigan declaradas, el boot las arranca igual (el ahorro de memoria no se materializa; V21 simplemente las ignora). Cuando V21 esté consagrado:
1. Eliminar los bloques PU3..PU10 de `persistence.xml` (quedan PU1, PU2 y la plantilla).
2. Rebuild + redeploy → el boot pasa de 10 SF a 2 por deployment.
3. Desde entonces, **alta de empresa** = crear el datasource en WildFly + fila en `appcompany` con su `persistentunit`. Sin XML, sin redeploy, sin tope de 10.

### Reversa
- **Instantánea** (sin rebuild de código): volver el `ejb-class` a `org.javabeanstack.data.DBManager` en los ejb-jar.xml → comportamiento tradicional exacto; la plantilla comentada es inerte para el clásico.
- Si se ejecutó el Paso 5: restaurar los bloques PU3..PU10 desde git.

## 6. Validación realizada (2026-07-10, WildFly 40 local)

| Escenario | Resultado |
|---|---|
| TestProject, V21 activo, PU3 declarada | Plantilla leída del comentario; PU3 dinámica-primero; suite jbs (243 tests, empresa 3) con paridad exacta vs esquema estático |
| Maker, clásico activo | Boot 10 PUs, PU8 por JNDI, cero actividad dinámica — comportamiento original intacto |
| Maker, conmutado a V21 | `Creando EntityManagerFactory dinámico PU8`; smoke `CtbmovimientoSrvTest1` idéntico al clásico |
| `wildfly.jpa.managed=false` | **Descartado**: WF40 bootea la SF igual — por eso la plantilla va comentada |

## 7. Consideraciones y limitaciones

- **Transición con doble esquema**: con PUs declaradas + V21 activo, las SF declaradas bootean igual (memoria doble para esas PUs). Es el precio de la reversibilidad instantánea; se resuelve con el Paso 5.
- **Cierre diferido sin transacción**: los EM de lectura se cierran a los 5 min de ociosidad (no retienen conexión JDBC en modo JTA; solo el objeto sesión).
- **Converters**: mantener `jbs.dynamic.classes.extra` sincronizado si se agregan AttributeConverters a Maker-model.
- **La plantilla se lee una sola vez** por ciclo de vida del singleton (cache); cambios en ella requieren redeploy.
- **2LC**: las PUs dinámicas no configuran second-level cache (igual que las PU2..PU10 declaradas; solo PU1 lo usa).

## 8. Alternativa por system properties: DBManagerV20

Existe una tercera implementación, `DBManagerV20`, con la misma fabricación runtime pero configurada por system properties de la JVM (`-Djbs.persistence.dynamic.*`, archivo `bin/maker-persistence.conf` del WildFly — hoy con su carga **comentada** en `bin/standalone.conf`). Diferencias principales: configuración por servidor (fuera del EAR, requiere reinicio para cambiar), whitelist explícita de unidades, datasource configurable por PU y lista de clases desde el recurso `META-INF/jbs-managed-classes.txt`. Documentación completa: [DINAMIC_DATA_MANAGMENT_DBMANAGER_V20.md](DINAMIC_DATA_MANAGMENT_DBMANAGER_V20.md).

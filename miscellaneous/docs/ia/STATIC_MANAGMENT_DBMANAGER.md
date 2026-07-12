# Gestión estática de unidades de persistencia — DBManager clásico

> Familia de documentos: [DINAMIC_DATA_MANAGMENT_DBMANAGER_V20.md](DINAMIC_DATA_MANAGMENT_DBMANAGER_V20.md) (variante dinámica DBManagerV20, system properties) · [DINAMIC_DATA_MANAGMENT_DBMANAGER_V30.md](DINAMIC_DATA_MANAGMENT_DBMANAGER_V30.md) (variante dinámica DBManagerV30, archivo dynamic_persistence.xml con una spec por unidad).
> Este es el esquema **tradicional y actualmente activo** en producción de Maker.

## 1. Descripción general

En el esquema estático, cada base de datos (el catálogo compartido y una base de negocio por empresa) tiene su **unidad de persistencia declarada** en `META-INF/persistence.xml` de `Maker-model`:

| PU | Schema | Uso |
|---|---|---|
| `PU1` | `catalogo` | Catálogo de aplicaciones/seguridad (constante `IDBManager.CATALOGO`) |
| `PU2`..`PU10` | `datos` | Una por empresa/base de negocio |

WildFly procesa el `persistence.xml` en el **boot de cada deployment** (Maker-ear, Maker-web y Maker-rest embeben las PUs) y arranca eager una `SessionFactory` por unidad declarada.

## 2. Componentes

### 2.1 persistence.xml (Maker-model)

Cada PU declara su datasource JTA y publica su EntityManager/EMF en JNDI mediante propiedades específicas de WildFly:

```xml
<persistence-unit name="PU2" transaction-type="JTA">
    <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
    <jta-data-source>jdbc/Maker950DS_2</jta-data-source>
    <exclude-unlisted-classes>false</exclude-unlisted-classes>
    <properties>
        <property name="hibernate.dialect" value="org.hibernate.dialect.MkSqlServer2008"/>
        <property name="hibernate.max_fetch_depth" value="0"/>
        <property name="hibernate.default_schema" value="datos"/>
        <property name="jbs.dbengine" value="SQLSERVER"/>
        <!--Bindings JNDI por deployment (java:app): permiten que EAR y WARs
            definan las mismas PUs sin colisión de nombres-->
        <property name="jboss.entity.manager.jndi.name" value="java:app/em/PU2"/>
        <property name="jboss.entity.manager.factory.jndi.name" value="java:app/emf/PU2"/>
    </properties>
</persistence-unit>
```

Puntos clave:
- `exclude-unlisted-classes=false` → autodetección de las ~600 entidades y los converters del jar.
- Los bindings `java:app/em/PUn` y `java:app/emf/PUn` son la interfaz con el DBManager.
- Solo PU1 configura second-level cache (Infinispan, `ENABLE_SELECTIVE`).

### 2.2 Datasources (standalone.xml de WildFly)

Un datasource por base, con el nombre que referencian las PUs:

```xml
<datasource jta="true" jndi-name="java:/jdbc/Maker950DS_2" pool-name="Maker950DS_2" ...>
    <connection-url>jdbc:sqlserver://127.0.0.1;databaseName=MAKER950TEST2;...</connection-url>
    ...
</datasource>
```

### 2.3 DBManager (JavaBeanStack)

`org.javabeanstack.data.DBManager` — EJB **Singleton** (`@Startup`, `@Lock(READ)`) declarado por XML en el `ejb-jar.xml` de cada deployment:

```xml
<session>
    <ejb-name>DBManager</ejb-name>
    <ejb-class>org.javabeanstack.data.DBManager</ejb-class>
    <business-local>org.javabeanstack.data.IDBManager</business-local>
    <session-type>Singleton</session-type>
</session>
```

Responsabilidades:
- `getEntityManager("PUn:sessionId")`: devuelve el EM de la unidad pedida; si no está en cache lo resuelve con `InitialContext.doLookup("java:app/em/" + PUn)` (prefijo configurable con la system property `jbs.persistence.jndi.em.prefix`).
- **Cache** en `ConcurrentHashMap` con clave `"<PU>:<sessionId>"` (estrategia `PERSESSION`; existe `PERTHREAD` como alternativa).
- **Purge**: en cada acceso, si pasaron 5 minutos desde la última purga, descarta las entradas sin referencia reciente. Los EM son proxies **container-managed transaction-scoped** — no requieren `close()`; descartarlos del mapa alcanza.
- `rollBack()`: `context.setRollbackOnly()`.

### 2.4 Flujo del nombre de PU en runtime

```
catalogo.appcompany.persistentunit  (columna de la empresa, ej. "PU8")
        ↓ login (Sessions.createSession)
UserSession.persistenceUnit
        ↓
DBLinkInfo.getPersistUnit()   (default: IDBManager.CATALOGO = "PU1")
        ↓
AbstractDAO.getEntityManagerId() → "PU8:<sessionId>"
        ↓
DBManager.getEntityManager(key) → lookup/cache JNDI
```

Los EM son tx-scoped: dentro de una transacción JTA todas las operaciones comparten el persistence context; fuera de transacción cada operación usa un contexto efímero. Por eso el DAO nunca cierra EMs en este esquema.

## 3. Alta de una empresa nueva (esquema estático)

1. Crear la base de datos de la empresa (restaurar/crear `MAKER950...`).
2. Crear el **datasource** en `standalone.xml` con el siguiente número libre (`jdbc/Maker950DS_n`).
3. Agregar el bloque `<persistence-unit name="PUn">` en `persistence.xml` (copiar el de PU2 y cambiar número/datasource/bindings).
4. **Recompilar** Maker (`mvn -DskipTests clean install` — regla: tras tocar persistence.xml, reconstruir con `install` o `-pl <módulo> -am` para que el EAR no tome un Maker-model viejo del repo local) y **redesplegar** EAR y WARs.
5. Registrar la empresa en `catalogo.appcompany` con `persistentunit = 'PUn'`.

## 4. Características y limitaciones

**Ventajas**
- Comportamiento probado por años en producción; cero código de gestión de ciclo de vida (el contenedor administra los EM).
- Arranque "fail-fast": si un datasource está mal, el deploy lo denuncia.

**Limitaciones** (motivación de la variante dinámica)
- Todas las SF arrancan eager en el boot, **por deployment** (EAR + 2 WARs → hasta 30 SF): memoria alta y redeploys pesados.
- Alta de empresa = editar XML + recompilar + redesplegar.
- Tope práctico de empresas = PUs declaradas (10).

## 5. Cómo asegurarse de que este esquema está activo

- En los tres `ejb-jar.xml` el bean `DBManager` debe apuntar a `org.javabeanstack.data.DBManager` (las alternativas dinámicas quedan comentadas al lado).
- En el log de boot de WildFly aparecen las 10 líneas `HHH008540: Processing PersistenceUnitInfo [name: PUn]`.
- En `standalone/log/oymframe.log` los EM se crean con `[org.javabeanstack.data.DBManager] --------- Se ha creado un nuevo EntityManager --------- PUn:...` y no hay ninguna línea de `DBManagerV30` ni de "EntityManagerFactory dinámico".

> El archivo `dynamic_persistence.xml` y los métodos `default` de `IDBManager` son **inertes** en este esquema: el DBManager clásico no los lee ni los usa.

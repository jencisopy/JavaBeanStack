# Gestión dinámica de datos con DBManagerV20

> Familia de documentos: [STATIC_MANAGMENT_DBMANAGER.md](STATIC_MANAGMENT_DBMANAGER.md) (esquema tradicional) · [DINAMIC_DATA_MANAGMENT_DBMANAGER_V30.md](DINAMIC_DATA_MANAGMENT_DBMANAGER_V30.md) (variante V30, archivo dynamic_persistence.xml con una spec por unidad).
> Variante documentada: `org.javabeanstack.data.DBManagerV20`, prototipo basado en `jakarta.persistence.PersistenceConfiguration` y configuración por system properties.
> Trabajo realizado el 2026-07-10 sobre JavaBeanStack y Maker, orientado a WildFly 40, Jakarta Persistence 3.2 y Hibernate 7.
>
> **V20 vs V30 en una línea**: V20 se configura **por servidor** (system properties de la JVM, requiere reinicio para cambiar; permite datasource arbitrario por PU) y necesita el archivo `jbs-managed-classes.txt`; V30 se configura **dentro del EAR** (`META-INF/dynamic_persistence.xml`, una spec completa por unidad) y toma las clases del metamodelo de una PU declarada, sin archivo de clases.

## 1. Objetivo

El objetivo de `DBManagerV20` es permitir que algunas unidades de persistencia de Maker se resuelvan en runtime sin que WildFly tenga que procesarlas todas como `persistence-unit` declaradas al arrancar el deployment.

En el esquema tradicional, `DBManager` hace lookup JNDI de un `EntityManager` publicado por el contenedor:

```text
DBManager.getEntityManager("PU8:<sessionId>")
        -> java:app/em/PU8
        -> EntityManager container-managed creado por WildFly desde persistence.xml
```

Con `DBManagerV20`, las PUs configuradas como dinámicas se resuelven así:

```text
DBManagerV20.getEntityManager("PU8:<sessionId>")
        -> detecta PU8 como dinámica
        -> crea/cachea EntityManagerFactory con PersistenceConfiguration
        -> crea EntityManager application-managed
        -> lo asocia a la transacción JTA o al thread actual
```

El catálogo (`PU1`) y cualquier PU no listada como dinámica siguen usando el camino clásico por JNDI.

## 2. Alcance real del prototipo

El cambio quedó separado en JavaBeanStack con dos implementaciones del mismo contrato:

| Clase | Responsabilidad |
|---|---|
| `org.javabeanstack.data.DBManager` | Implementación clásica. Quedó restaurada al comportamiento tradicional: lookup JNDI `java:app/em/PUn` y cache por sesión/thread. |
| `org.javabeanstack.data.DBManagerV20` | Implementación experimental. Conserva el camino clásico para PUs no dinámicas y agrega creación de EMF dinámico con `PersistenceConfiguration`. |

`DBManagerV20` fue creado copiando la implementación dinámica que se había probado inicialmente dentro de `DBManager`, y `DBManager` fue devuelto al código clásico para que la elección sea explícita desde `ejb-jar.xml`.

Commits relacionados en JavaBeanStack:

| Commit | Descripción |
|---|---|
| `04dda34` | Agrega soporte experimental de EMF dinámico (rama `feature/persistence-configuration-emf`). |
| `72a6a73` | Separa `DBManagerV20` experimental y restaura `DBManager`. |
| `e395f97` | Merge de `feature/persistence-configuration-emf` a `master` (trajo el `@AroundInvoke` de `AbstractDAO`). |
| _(pendiente)_ | Purga de respaldo por ociosidad en `DBManagerV20` (paridad con las demás variantes dinámicas) + fix de métodos duplicados en `IDBManager` que dejó el merge. |

Validación realizada durante el prototipo:

```bash
mvn -q -pl business -am -DskipTests compile
```

Resultado: compilación correcta.

## 3. Diferencias con DBManager clásico

| Tema | `DBManager` clásico | `DBManagerV20` |
|---|---|---|
| Selección de PU | Siempre por JNDI `java:app/em/PUn`. | Primero verifica si la PU está listada como dinámica; si no, usa JNDI igual que el clásico. |
| Origen del EMF | Lo crea WildFly al procesar `persistence.xml`. | Lo crea la aplicación con `PersistenceConfiguration`. |
| Configuración | `persistence.xml`. | System properties `jbs.persistence.dynamic.*` más lista explícita de clases administradas. |
| Entidades | Autodetección del proveedor por `exclude-unlisted-classes=false`. | Registro explícito con `config.managedClass(...)`; no hay autodetección. |
| `EntityManager` | Container-managed; no se cierra explícitamente. | Application-managed; se debe cerrar al finalizar transacción o al liberar thread. |
| Reversibilidad | Activo apuntando el bean a `DBManager`. | Activo apuntando el bean a `DBManagerV20`. |

## 4. Funcionamiento interno de DBManagerV20

### 4.1 Selección dinámica

`DBManagerV20` lee estas system properties:

| Propiedad | Uso |
|---|---|
| `jbs.persistence.dynamic.enabled` | Habilita o deshabilita el modo dinámico. Debe ser `true`. |
| `jbs.persistence.dynamic.units` | Lista de PUs dinámicas separadas por coma, por ejemplo `PU3,PU4,PU8`, o `*` para todas. |
| `jbs.persistence.jndi.em.prefix` | Prefijo JNDI para el camino clásico. Default: `java:app/em/`. |

Si `dynamic.enabled` no es `true`, o si la PU no está en `dynamic.units`, `DBManagerV20` se comporta como el `DBManager` clásico.

### 4.2 Creación del EntityManagerFactory

Cuando una PU es dinámica, `DBManagerV20` crea un `EntityManagerFactory` por PU y lo cachea en un `ConcurrentHashMap`.

La creación usa:

```java
new PersistenceConfiguration(persistentUnit)
        .provider(provider)
        .transactionType(PersistenceUnitTransactionType.JTA)
        .jtaDataSource(dataSource)
        .property("hibernate.default_schema", schema)
```

Luego agrega propiedades, mapping files y clases administradas.

El datasource se obtiene con esta prioridad:

| Prioridad | Propiedad |
|---|---|
| 1 | `jbs.persistence.dynamic.<PU>.jtaDataSource`, por ejemplo `jbs.persistence.dynamic.PU8.jtaDataSource`. |
| 2 | `jbs.persistence.dynamic.jtaDataSource`, común a todas las PUs. |
| 3 | `jbs.persistence.dynamic.datasource.prefix` más el número de la PU, por ejemplo `jdbc/Maker950DS_` + `8`. |

El provider se define con `jbs.persistence.dynamic.provider`. Si no se informa, usa `org.hibernate.jpa.HibernatePersistenceProvider`.

El schema default se calcula así:

| PU | Schema |
|---|---|
| `PU1` | `catalogo` |
| Resto | `datos` |

### 4.3 Propiedades Hibernate/JBS

`DBManagerV20` no copia automáticamente las propiedades de `persistence.xml`. Las propiedades que se deben aplicar al EMF dinámico se listan en `jbs.persistence.dynamic.properties`.

Ejemplo:

```bash
-Djbs.persistence.dynamic.properties=hibernate.dialect,hibernate.max_fetch_depth,hibernate.jpa.compliance.global_id_generators,hibernate.format_sql,hibernate.show_sql,jbs.dbengine
-Djbs.persistence.dynamic.property.hibernate.dialect=org.hibernate.dialect.MkSqlServer2008
-Djbs.persistence.dynamic.property.hibernate.max_fetch_depth=0
-Djbs.persistence.dynamic.property.hibernate.jpa.compliance.global_id_generators=false
-Djbs.persistence.dynamic.property.hibernate.format_sql=true
-Djbs.persistence.dynamic.property.hibernate.show_sql=false
-Djbs.persistence.dynamic.property.jbs.dbengine=SQLSERVER
```

También se puede sobreescribir por PU:

```bash
-Djbs.persistence.dynamic.PU8.property.hibernate.show_sql=true
```

La prioridad de lectura es:

| Prioridad | Forma |
|---|---|
| 1 | `jbs.persistence.dynamic.<PU>.property.<nombre>` |
| 2 | `jbs.persistence.dynamic.property.<nombre>` |

### 4.4 Clases administradas

`PersistenceConfiguration` no escanea automáticamente las entidades de Maker como lo hace el contenedor con `persistence.xml`. Por eso `DBManagerV20` necesita registrar cada entidad con `managedClass(...)`.

Hay tres formas:

| Forma | Propiedad |
|---|---|
| Lista global en system property | `jbs.persistence.dynamic.managed.classes` |
| Lista por PU | `jbs.persistence.dynamic.<PU>.managed.classes` |
| Recurso en classpath | `jbs.persistence.dynamic.managed.classes.resource` o `jbs.persistence.dynamic.<PU>.managed.classes.resource` |

Para Maker se eligió el recurso:

```bash
-Djbs.persistence.dynamic.managed.classes.resource=META-INF/jbs-managed-classes.txt
```

Ese archivo debe estar en el classpath del deployment, recomendado en:

```text
Maker-model/src/main/resources/META-INF/jbs-managed-classes.txt
```

Formato del archivo:

```text
net.makerapp.model.tables.Itemmovimiento
net.makerapp.model.tables.Ctbmovimiento
net.makerapp.model.tables.converter.StringTrimConverter
```

Reglas:

- Una clase por línea.
- Las líneas vacías se ignoran.
- Las líneas que empiezan con `#` se ignoran.
- Deben incluirse entidades, vistas mapeadas, audit tables y converters requeridos por el modelo.

Si no se define ninguna lista de clases, `DBManagerV20` falla con:

```text
No se definieron entidades para PUn
```

### 4.5 EntityManager por transacción o por thread

Los `EntityManager` dinámicos son application-managed, por lo tanto `DBManagerV20` debe controlar su ciclo de vida.

Con transacción JTA activa:

- Usa `TransactionSynchronizationRegistry`.
- Guarda un EM por `(transacción, PU)` con `tsr.putResource(...)`.
- Llama `em.joinTransaction()`.
- Registra una `Synchronization` y cierra el EM en `afterCompletion`.

Sin transacción activa:

- Usa un mapa `dynamicEntityManagers` con clave `(PU, thread)` y timestamp de última referencia.
- Reusa un EM por `(PU, thread)` mientras siga abierto.
- El método `closeEntityManagers()` cierra todos los EM dinámicos asociados al thread actual.

El cierre sin transacción tiene **dos niveles**:

1. **Principal (inmediato)**: el `@AroundInvoke` de `AbstractDAO` llama a `dbManager.closeEntityManagers()` al finalizar cada invocación DAO. Esta parte depende de los métodos default agregados a `IDBManager`.
2. **Respaldo (purga por ociosidad)**: `purgeEntityManager()` cierra los EM dinámicos con más de 5 minutos sin uso — mismo criterio que el cache de EMs declarados y misma mecánica que `DBManagerV30`. Cubre cualquier EM que se cree por fuera del ciclo del interceptor. Se agregó el 2026-07-10 (el prototipo original usaba un `ThreadLocal` que no admitía purga desde otros threads).

## 5. Archivos involucrados

### 5.1 JavaBeanStack

| Archivo | Cambio |
|---|---|
| `business/src/main/java/org/javabeanstack/data/DBManager.java` | Restaurado al comportamiento clásico. |
| `business/src/main/java/org/javabeanstack/data/DBManagerV20.java` | Nueva implementación experimental dinámica. |
| `interfaces/src/main/java/org/javabeanstack/data/IDBManager.java` | Se agregaron métodos default para cerrar EM dinámicos sin romper el clásico. |
| `business/src/main/java/org/javabeanstack/data/AbstractDAO.java` | Se agregó hook `@AroundInvoke` para cerrar EM dinámicos de thread. |

### 5.2 Maker

| Archivo | Uso |
|---|---|
| `Maker-model/src/main/resources/META-INF/jbs-managed-classes.txt` | Lista explícita de clases administradas para `PersistenceConfiguration`. |
| `Maker-model/src/main/resources/META-INF/persistence.xml` | En la variante V20 se pueden dejar solo las PUs estáticas y comentar o eliminar las PUs dinámicas. |
| `Maker-services/src/main/resources/META-INF/ejb-jar.xml` | Selección del bean `DBManager` o `DBManagerV20` para el EJB module. |
| `Maker-web/src/main/webapp/WEB-INF/ejb-jar.xml` | Selección equivalente para el WAR web si empaqueta/usa el bean. |
| `Maker-rest/src/main/webapp/WEB-INF/ejb-jar.xml` | Selección equivalente para el WAR REST si empaqueta/usa el bean. |

### 5.3 WildFly local

| Archivo | Uso |
|---|---|
| `bin/standalone.conf` | Carga opcional de un archivo externo de configuración. |
| `bin/maker-persistence.conf` | System properties `jbs.persistence.dynamic.*` para el prototipo V20. |

## 6. Implementación paso a paso en Maker

### Paso 1. Instalar JavaBeanStack con DBManagerV20

En el repositorio JavaBeanStack, compilar e instalar la versión que contiene `DBManagerV20`:

```bash
mvn -DskipTests clean install
```

Para una verificación rápida del módulo de negocio:

```bash
mvn -q -pl business -am -DskipTests compile
```

### Paso 2. Agregar la lista de clases administradas

Crear en Maker:

```text
Maker-model/src/main/resources/META-INF/jbs-managed-classes.txt
```

El contenido debe listar todas las clases JPA que antes detectaba el contenedor desde `persistence.xml`.

Checklist recomendado:

- Incluir entidades de `net.makerapp.model.tables`.
- Incluir entidades audit y vistas mapeadas que se usen en consultas.
- Incluir converters como `net.makerapp.model.tables.converter.StringTrimConverter`.
- Mantener el archivo actualizado cuando se agregue una entidad o converter nuevo.

Generación automática desde las fuentes (entidades + converters):

```bash
cd Maker-model
{ grep -rl --include='*.java' '@Entity' src/main/java ; \
  grep -rl --include='*.java' '@Converter' src/main/java ; } \
 | sort -u \
 | sed 's|^src/main/java/||; s|/|.|g; s|\.java$||' \
 > src/main/resources/META-INF/jbs-managed-classes.txt
```

> **Atención**: al momento de escribir este documento el archivo **no existe** en el código fuente de Maker — generarlo es prerrequisito para activar V20.

### Paso 3. Definir las PUs dinámicas

Crear o actualizar un archivo externo de WildFly, por ejemplo:

```text
/home/jenciso/oym/proyectos/java/servers/wildfly-40.0.1.Final_desarrollo/bin/maker-persistence.conf
```

Contenido base:

```bash
JAVA_OPTS="$JAVA_OPTS -Djbs.persistence.dynamic.enabled=true"
JAVA_OPTS="$JAVA_OPTS -Djbs.persistence.dynamic.units=PU3,PU4,PU5,PU6,PU7,PU8,PU9,PU10"
JAVA_OPTS="$JAVA_OPTS -Djbs.persistence.dynamic.datasource.prefix=jdbc/Maker950DS_"
JAVA_OPTS="$JAVA_OPTS -Djbs.persistence.dynamic.managed.classes.resource=META-INF/jbs-managed-classes.txt"
JAVA_OPTS="$JAVA_OPTS -Djbs.persistence.dynamic.properties=hibernate.dialect,hibernate.max_fetch_depth,hibernate.jpa.compliance.global_id_generators,hibernate.format_sql,hibernate.show_sql,jbs.dbengine"
JAVA_OPTS="$JAVA_OPTS -Djbs.persistence.dynamic.property.hibernate.dialect=org.hibernate.dialect.MkSqlServer2008"
JAVA_OPTS="$JAVA_OPTS -Djbs.persistence.dynamic.property.hibernate.max_fetch_depth=0"
JAVA_OPTS="$JAVA_OPTS -Djbs.persistence.dynamic.property.hibernate.jpa.compliance.global_id_generators=false"
JAVA_OPTS="$JAVA_OPTS -Djbs.persistence.dynamic.property.hibernate.format_sql=true"
JAVA_OPTS="$JAVA_OPTS -Djbs.persistence.dynamic.property.hibernate.show_sql=false"
JAVA_OPTS="$JAVA_OPTS -Djbs.persistence.dynamic.property.jbs.dbengine=SQLSERVER"
```

Si se prefiere no depender del prefijo, declarar cada datasource explícitamente:

```bash
JAVA_OPTS="$JAVA_OPTS -Djbs.persistence.dynamic.PU3.jtaDataSource=jdbc/Maker950DS_3"
JAVA_OPTS="$JAVA_OPTS -Djbs.persistence.dynamic.PU4.jtaDataSource=jdbc/Maker950DS_4"
JAVA_OPTS="$JAVA_OPTS -Djbs.persistence.dynamic.PU5.jtaDataSource=jdbc/Maker950DS_5"
JAVA_OPTS="$JAVA_OPTS -Djbs.persistence.dynamic.PU6.jtaDataSource=jdbc/Maker950DS_6"
JAVA_OPTS="$JAVA_OPTS -Djbs.persistence.dynamic.PU7.jtaDataSource=jdbc/Maker950DS_7"
JAVA_OPTS="$JAVA_OPTS -Djbs.persistence.dynamic.PU8.jtaDataSource=jdbc/Maker950DS_8"
JAVA_OPTS="$JAVA_OPTS -Djbs.persistence.dynamic.PU9.jtaDataSource=jdbc/Maker950DS_9"
JAVA_OPTS="$JAVA_OPTS -Djbs.persistence.dynamic.PU10.jtaDataSource=jdbc/Maker950DS_10"
```

Validar sintaxis del archivo:

```bash
bash -n /home/jenciso/oym/proyectos/java/servers/wildfly-40.0.1.Final_desarrollo/bin/maker-persistence.conf
```

### Paso 4. Cargar la configuración desde standalone.conf

En el `standalone.conf` de WildFly, cargar el archivo si existe:

```bash
MAKER_PERSISTENCE_CONF="$JBOSS_HOME/bin/maker-persistence.conf"
if [ -f "$MAKER_PERSISTENCE_CONF" ]; then
   . "$MAKER_PERSISTENCE_CONF"
fi
```

Esta estrategia deja la configuración fuera del EAR y permite activar/desactivar la variante V20 por servidor.

### Paso 5. Elegir DBManagerV20 en ejb-jar.xml

En el descriptor EJB donde se declara el singleton `DBManager`, cambiar la clase:

```xml
<session>
    <ejb-name>DBManager</ejb-name>
    <!--<ejb-class>org.javabeanstack.data.DBManager</ejb-class>-->
    <ejb-class>org.javabeanstack.data.DBManagerV20</ejb-class>
    <business-local>org.javabeanstack.data.IDBManager</business-local>
    <session-type>Singleton</session-type>
</session>
```

En Maker existen descriptores en:

```text
Maker-services/src/main/resources/META-INF/ejb-jar.xml
Maker-web/src/main/webapp/WEB-INF/ejb-jar.xml
Maker-rest/src/main/webapp/WEB-INF/ejb-jar.xml
```

Aplicar el cambio solo en los deployments que realmente vayan a usar `DBManagerV20`. Si se quiere comportamiento homogéneo, cambiar los tres.

### Paso 6. Ajustar persistence.xml

Durante una transición conservadora, se puede dejar `PU1` y `PU2` declaradas y comentar/eliminar las PUs que pasarán a dinámicas.

Recomendación para V20:

- Mantener `PU1` declarada porque es el catálogo.
- Mantener `PU2` declarada como PU base/segura durante pruebas.
- Comentar o eliminar `PU3..PU10` si se quiere comprobar que WildFly ya no las procesa al arrancar.
- Asegurar que los datasources `jdbc/Maker950DS_n` existan en WildFly para cada PU dinámica.

Si una PU está declarada y también listada en `dynamic.units`, `DBManagerV20` usará el camino dinámico, pero WildFly igualmente habrá arrancado esa PU declarada. Para obtener ahorro de memoria, la PU no debe estar activa en `persistence.xml`.

### Paso 7. Compilar Maker

Desde la raíz de Maker:

```bash
mvn -DskipTests clean install
```

Para cambios limitados a `Maker-model`:

```bash
mvn -q -pl Maker-model -DskipTests compile
```

Si se cambia `persistence.xml` o recursos del modelo, usar `install` o compilar con dependientes para evitar que el EAR tome un `Maker-model` viejo desde el repositorio local.

### Paso 8. Desplegar y verificar logs

Al arrancar WildFly, verificar que las PUs dinámicas ya no aparezcan como procesadas por el contenedor si fueron removidas/comentadas de `persistence.xml`.

No debería verse para una PU dinámica removida:

```text
Processing PersistenceUnitInfo [name: PU8]
```

Al primer acceso real a una empresa dinámica, debe aparecer una línea similar:

```text
EntityManagerFactory dinámico creado para PU8 sobre jdbc/Maker950DS_8
```

Si no aparece, revisar:

- Que el deployment esté usando `DBManagerV20` en `ejb-jar.xml`.
- Que `jbs.persistence.dynamic.enabled=true` esté llegando a la JVM.
- Que `PU8` esté incluida en `jbs.persistence.dynamic.units`.
- Que `META-INF/jbs-managed-classes.txt` esté dentro del artefacto desplegado.
- Que el datasource exista y tenga el nombre JNDI esperado.

## 7. Alta de una empresa con V20

Con el esquema V20, el alta de una nueva empresa no requiere agregar una nueva `persistence-unit` si se usa un patrón de datasource.

Pasos:

1. Crear/restaurar la base de datos de la empresa.
2. Crear el datasource JTA en WildFly, por ejemplo `jdbc/Maker950DS_11`.
3. Registrar la empresa en `catalogo.appcompany` con `persistentunit = 'PU11'`.
4. Agregar `PU11` a `jbs.persistence.dynamic.units`, o usar `*` si esa política fue validada.
5. Reiniciar WildFly si la configuración está en `JAVA_OPTS`.
6. Ingresar con la empresa y verificar que se cree el EMF dinámico para `PU11`.

## 8. Reversa al esquema clásico

La reversa es simple si se conservaron las PUs en `persistence.xml`:

```xml
<ejb-class>org.javabeanstack.data.DBManager</ejb-class>
<!--<ejb-class>org.javabeanstack.data.DBManagerV20</ejb-class>-->
```

Luego reiniciar/redeployar.

Si se eliminaron o comentaron PUs dinámicas en `persistence.xml`, restaurarlas antes de volver al clásico; de lo contrario el lookup JNDI `java:app/em/PUn` fallará.

También se puede desactivar V20 sin cambiar XML si el bean sigue apuntando a `DBManagerV20` pero se configura:

```bash
-Djbs.persistence.dynamic.enabled=false
```

En ese caso `DBManagerV20` delega todas las PUs al camino clásico, pero las PUs deben estar declaradas en `persistence.xml`.

## 9. Riesgos y pendientes

| Tema | Estado |
|---|---|
| Lista de clases | Debe mantenerse manualmente. Si falta una entidad o converter, el EMF dinámico puede fallar al crear o ejecutar consultas. |
| Propiedades Hibernate | V20 solo aplica las propiedades listadas en `jbs.persistence.dynamic.properties`; no hereda todo `persistence.xml`. |
| Cierre sin transacción | Depende del hook `@AroundInvoke` en `AbstractDAO` y de los métodos default de `IDBManager`. |
| PUs declaradas y dinámicas a la vez | Funciona, pero no reduce memoria de arranque porque WildFly igual procesa las PUs declaradas. |
| Cambios en system properties | Requieren reinicio del servidor. |
| `closeFactory` | Permite cerrar un EMF por PU, pero no está expuesto como operación administrativa de Maker. |

## 10. Resumen operativo

Para activar `DBManagerV20` correctamente deben cumplirse cuatro condiciones:

1. JavaBeanStack desplegado contiene `org.javabeanstack.data.DBManagerV20`.
2. El `ejb-jar.xml` del deployment apunta el bean `DBManager` a `DBManagerV20`.
3. WildFly recibe `-Djbs.persistence.dynamic.enabled=true` y la lista de PUs dinámicas.
4. El deployment contiene `META-INF/jbs-managed-classes.txt` con todas las clases JPA requeridas.

Si cualquiera de esas condiciones falta, el sistema vuelve al camino clásico o falla al construir el EMF dinámico.

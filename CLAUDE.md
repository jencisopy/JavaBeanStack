# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Descripción del proyecto

JavaBeanStack es un framework Java multi-módulo Maven (LGPL v3.0) que provee infraestructura reutilizable para acceso a datos, seguridad, manejo de errores y capas web JSF/REST. Es una **librería/framework**, no una aplicación ejecutable. Grupo Maven: `org.javabeanstack`, parent `jbs-parent`, versión actual `2.0.0-SNAPSHOT`. Compila con Java 11 (`maven.compiler.release`).

## Estrategia de ramas — IMPORTANTE

- **`master`**: versión 2.0.0, **Jakarta EE 10** (namespace `jakarta.*`), PrimeFaces 15 (classifier `jakarta`), JasperReports 7, JUnit 5.
- **`1.5.x`**: rama legacy 1.5.x, **Java EE 8** (namespace `javax.*`), PrimeFaces antiguo, JasperReports 6. Recibe fixes de lógica que luego se portan a master.

Al comparar o portar cambios entre ramas:
- Ignorar diferencias `javax.*` ↔ `jakarta.*`: son equivalentes, no son diferencias de lógica.
- Los paquetes `javax.crypto`, `javax.xml.*`, `javax.naming` y `javax.swing` son del JDK y **permanecen como `javax` en ambas ramas** — nunca convertirlos a `jakarta`.
- Diferencias de API esperables en master (adaptaciones deliberadas, no regresiones):
  - PrimeFaces 15: `DefaultStreamedContent.builder()` en vez del constructor; `UploadedFile.getContent()` en vez de `getContents()`; `LazyDataModel.load(first, pageSize, Map<String,SortMeta>, Map<String,FilterMeta>)` y `count(...)` en vez de las sobrecargas viejas; `getRowKey()`/`getRowkey()` devuelve `String` en vez de `Object`.
  - JasperReports 7: `JRXlsxExporter` (el device `"xls"` se acepta como alias de `"xlsx"` en `JasperReportUtil` por compatibilidad).

## Comandos

Ejecutar desde la raíz del repositorio.

```bash
# Compilar todo (incluye tests) sin ejecutarlos
mvn test-compile -DskipTests

# Build completo
mvn clean package

# Instalar en el repo Maven local
mvn clean install

# Un módulo y sus dependencias upstream
mvn -pl web -am clean package

# Tests de un módulo
mvn -pl commons test

# Una clase de test / un método
mvn -pl commons -Dtest=StringsTest test
mvn -pl web -Dtest=ExcelUtilTest#testOpenWorkbook_File test

# Cobertura (JaCoCo)
mvn -Psonar-coverage test

# Ver versiones resueltas de una dependencia
mvn dependency:tree -Dincludes=org.apache.poi -pl web
```

No hay lint ni formatter configurados; la validación es `mvn test-compile` / `mvn test`.

### Tests: qué se puede ejecutar localmente

- **`commons`**: tests unitarios puros — se pueden correr siempre.
- **`business` (y los que extienden `TestClass`)**: son tests de integración que requieren un **servidor WildFly/JBoss corriendo** con el EAR `TestProjects-ear` desplegado (lookup EJB remoto vía `http-remoting`). Se configuran con variables de entorno: `SERVER_TEST` (default `localhost`), `SERVER_TEST_PORT` (default `8080`), `SECURITY_PRINCIPAL`, `SECURITY_CREDENTIALS`, `APP_USER_LOGIN`/`APP_USER_PASS` (default `test1`), `APP_IDCOMPANY` (default `2`). Sin ese servidor, fallan en el setup — no es un bug del código.

## CI/CD — CUIDADO con push a master

`.github/workflows/maven_deploy.yml`: **cada push a `master` dispara `mvn deploy`** (JDK 11, `-DskipTests -P javadoc`) que publica los artefactos al Nexus de OYM (`serverapps.oym.com.py`, credenciales vía secrets). Antes de pushear a master, verificar que el proyecto compila completo.

## Arquitectura de módulos

Flujo de dependencias (cada nivel depende de los de arriba):

```
interfaces  (solo contratos, sin implementaciones)
    ↓
commons     (utilidades de strings, archivos, crypto, fechas)
    ↓
core        (manejo de errores, log de eventos, XML, configuración, recursos)
    ↓
business    (acceso a datos JPA/EJB, servicios, seguridad, lógica de negocio)
    ↓
web         (controllers JSF, recursos REST, Excel/JasperReports, filtros)

aws         (integración AWS S3 — independiente, fuera de la cadena principal)
```

No introducir imports que inviertan esta dirección. Ojo: `interfaces` expone tipos de Apache POI en su API (`IExcelRowProcessor`, `IExcelImportSrv`), por eso declara `poi-ooxml` como dependencia.

## Gestión de dependencias

- La versión de `poi-ooxml` está centralizada en el `dependencyManagement` del pom padre; `interfaces` y `web` la heredan sin `<version>`. **Mantenerla alineada con el `poi` base que arrastra JasperReports** (POI exige que todos sus artefactos tengan la misma versión — verificar con `dependency:tree`).
- Los cuatro artefactos de JasperReports en `web/pom.xml` (`jasperreports`, `-servlets`, `-excel-poi`, `-groovy`) deben tener siempre la misma versión.
- `jakarta.jakartaee-api` va con scope `provided` (lo provee el servidor de aplicaciones).
- log4j (`log4j-core`/`log4j-api`) y JUnit se declaran en el pom padre; subir ambos artefactos de cada par juntos.

## Abstracciones clave

### Capa de datos (`business`)
- **`DataRow`** — clase base de entidades; las entidades de aplicación la extienden. Trackea estado de acción CRUD (`INSERT=1`, `UPDATE=2`, `DELETE=3`), cambios de campos y errores de validación.
- **`AbstractDAO`** — DAO genérico sobre JPA con construcción de queries, validación de entidades y manejo de errores.
- **`AbstractDataLink`** — envuelve el DAO y gestiona el contexto de unidad de persistencia, sesión de usuario y contexto empresa/schema. Implementa `IDataLink`.
- **`AbstractDataService`** — extiende el DAO con validación de negocio (claves únicas, foreign keys, validación por campo).
- **`DBManager`** — singleton EJB que administra los `EntityManager` por unidad de persistencia (lookup JNDI global).

### Seguridad (`interfaces` + `business`)
- **`IUserSession`** — contexto del usuario autenticado (login, empresa, roles, permisos).
- **`ISecManager`** / **`Sessions`** — autenticación vía EJB, gestión de contraseñas, OAuth.
- **`JwtManager`** / **`DigestAuth`** — soporte JWT y HTTP Digest.

### Manejo de errores (`core`)
- **`IErrorReg`** / **`ErrorReg`** — representación unificada de errores entre capas.
- **`ErrorManager`** — utilidad estática de log de errores: `ErrorManager.showError(ex, LOGGER)`.
- **`LogManager`** — persiste mensajes/eventos en la tabla `AppMessage` (campo `number`).

### Capa web (`web`)
- **`AbstractDataController`** — managed bean JSF base para pantallas CRUD; maneja lazy loading, cache y despliegue de errores.
- **`LazyDataRows`** — paginación lazy para DataTables de PrimeFaces. Pendientes de la adaptación a PrimeFaces 15 (marcados con `TODO`): verificar el manejo de `FilterMeta.getFilterValue()` en `getParams` y la implementación de `count()`.
- **`JasperReportUtil`** — exportación de reportes; el parámetro `device` acepta `printer`, `html`, `doc`, `pdf`, `xlsx` (y `xls` como alias de `xlsx`).
- **`ExcelUtil`** / **`ExcelImportSrv`** / **`ExcelUploadCtrl`** — importación de datos desde planillas Excel (Apache POI).

## Estilo de código

- Indentación: 4 espacios. Javadoc y comentarios en español.
- Interfaces con prefijo `I` (`IDataRow`, `ILogManager`). Clases PascalCase, métodos/campos camelCase, constantes `UPPER_SNAKE_CASE`.
- Loggers: `private static final Logger LOGGER = LogManager.getLogger(MiClase.class);` (log4j2).
- No usar `var`; tipos explícitos. Preferir tipos de interfaz en firmas (`List`, `IDataRow`), concretos solo al construir.
- Métodos públicos de servicios/DAO suelen declarar `throws Exception`; no estrechar esas firmas.
- Extender las clases base abstractas existentes en vez de saltearlas. Antes de cambiar código, leer archivos vecinos del módulo y copiar las convenciones locales.
- Preferir ediciones mínimas orientadas a compatibilidad por sobre refactors.

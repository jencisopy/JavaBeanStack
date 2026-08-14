# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Descripción del proyecto

JavaBeanStack es un framework Java multi-módulo Maven (LGPL v3.0) que provee infraestructura reutilizable para acceso a datos, seguridad, manejo de errores y capas web JSF/REST. Es una **librería/framework**, no una aplicación ejecutable. Grupo Maven: `org.javabeanstack`, parent `jbs-parent`, versión actual `2.0.0-SNAPSHOT`. `maven.compiler.release` fija **Java 25** (`java.version` en el pom raíz, alineado al runtime de WildFly 40; Jakarta EE 11 exige 17 como mínimo).

## Estrategia de ramas — IMPORTANTE

- **`master`**: versión 2.0.0, **Jakarta EE 11** (`jakarta.jakartaee-api:11.0.0`, namespace `jakarta.*`), Java 25, PrimeFaces 15 (classifier `jakarta`), JasperReports 7, JUnit 5. Los tests de integración de `business` requieren un servidor **WildFly 40+** (primera versión con soporte completo de EE11).
- **`1.5.x`**: rama legacy 1.5.x, **Java EE 8** (namespace `javax.*`), PrimeFaces antiguo, JasperReports 6. Recibe fixes de lógica que luego se portan a master.

Al comparar o portar cambios entre ramas:
- Ignorar diferencias `javax.*` ↔ `jakarta.*`: son equivalentes, no son diferencias de lógica.
- Los paquetes `javax.crypto`, `javax.xml.*`, `javax.naming` y `javax.swing` son del JDK y **permanecen como `javax` en ambas ramas** — nunca convertirlos a `jakarta`.
- Diferencias de API esperables en master (adaptaciones deliberadas, no regresiones):
  - PrimeFaces 15: `DefaultStreamedContent.builder()` en vez del constructor; `UploadedFile.getContent()` en vez de `getContents()`; `LazyDataModel.load(first, pageSize, Map<String,SortMeta>, Map<String,FilterMeta>)` y `count(...)` en vez de las sobrecargas viejas; `getRowKey()`/`getRowkey()` devuelve `String` en vez de `Object`.
  - JasperReports 7: `JRXlsxExporter` (el device `"xls"` se acepta como alias de `"xlsx"` en `JasperReportUtil` por compatibilidad).

### Migración a Jakarta EE 11 — verificada en runtime

- **PrimeFaces** está en `15.0.7` (gestionada en el BOM) y **verificado contra Jakarta Faces 4.1 en runtime**: el WAR de Maker con jbs-web se despliega y funciona en WildFly 40 (Mojarra 4.1.7), julio 2026.
- El bump de Java y `jakartaee-api` 10.0.0→11.0.0 se validó en su momento con `mvn test-compile` (compilación completa) y con los tests unitarios de `commons` en JDK 17 y **JDK 25 LTS** (Temurin) — mismo resultado en ambos (el único fallo, `CipherUtilTest.testRSA`, es preexistente y no relacionado a la migración); hoy el proyecto compila y publica directamente con JDK 25.

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
mvn -pl poi -Dtest=ExcelUtilTest#testOpenWorkbook_File test

# Cobertura (JaCoCo)
mvn -Psonar-coverage test

# Ver versiones resueltas de una dependencia
mvn dependency:tree -Dincludes=org.apache.poi -pl poi
```

No hay lint ni formatter configurados; la validación es `mvn test-compile` / `mvn test`.

### Tests: qué se puede ejecutar localmente

- **`commons`**, **`core`**, **`web`**, **`poi`**, **`outputs`**: tests unitarios puros — se pueden correr siempre (`mvn -pl commons,core,web,poi,outputs test`).
- **`business` (y los que extienden `TestClass`)**: son tests de integración que requieren un **servidor WildFly/JBoss corriendo** con el EAR `TestProjects-ear` desplegado (lookup EJB remoto vía `http-remoting`). Se configuran con variables de entorno: `SERVER_TEST` (default `localhost`), `SERVER_TEST_PORT` (default `8080`), `SECURITY_PRINCIPAL`, `SECURITY_CREDENTIALS`, `APP_USER_LOGIN`/`APP_USER_PASS` (default `test1`), `APP_IDCOMPANY` (default `2`). Sin ese servidor, fallan en el setup — no es un bug del código.

## CI/CD — CUIDADO con push a master

`.github/workflows/maven_deploy.yml`: **cada push a `master` dispara `mvn deploy`** (JDK 25, `-DskipTests -P javadoc`) que publica los artefactos al Nexus de OYM (`serverapps.oym.com.py`, credenciales vía secrets). Antes de pushear a master, verificar que el proyecto compila completo.

## Arquitectura de módulos

Flujo de dependencias (cada nivel depende de los de arriba):

```
bom         (Bill of Materials — solo dependencyManagement; es el parent de jbs-parent)
    ↓
interfaces  (solo contratos, sin implementaciones)
    ↓
commons     (utilidades de strings, archivos, crypto, fechas)
    ↓
core        (manejo de errores, log de eventos, XML, configuración, recursos)
    ↓
business    (acceso a datos JPA/EJB, servicios, seguridad, lógica de negocio)
    ↓
├── web     (controllers JSF, converters, filtros — solo JSF/PrimeFaces)
└── poi     (todo lo construido sobre Apache POI: planillas Excel —
             ExcelUtil, importación Excel*Srv — y plantillas Word —
             WordTemplateMerge/Source; sin PrimeFaces y sin jbs-web, usa
             FacesContext directo. Se llamó jbs-excel hasta 2026-08)

rest        (recursos JAX-RS: AbstractWebResource, SessionWebResource, CORSFilter,
             exceptions, model, util — depende solo de interfaces + commons;
             jbs-business entra únicamente con scope test)
jasper      (integración JasperReports; JasperReportUtil — depende solo de jbs-core)
aws         (integración AWS S3 — independiente, fuera de la cadena principal)
```

No introducir imports que inviertan esta dirección. Desde 2.0, tres módulos se
extrajeron de `web` para aislar sus dependencias pesadas (POI, JasperReports+Groovy,
JAX-RS) y sacar Apache POI de `interfaces`:

- **`poi`** (ex `excel`) ya **no depende de `jbs-web` ni de PrimeFaces**: declara `jbs-business` (arrastra core, commons, interfaces), `jbs-outputs` + `poi-ooxml`; usa `FacesContext`/`jakarta.servlet` directos (jakartaee-api provided). El bean de carga `ExcelUploadCtrl`/`IExcelUploadCtrl` (paquete `org.javabeanstack.web.uploads`) **se movió al proyecto Maker** (`net.makerapp.web.uploads`, Maker-controllers) por ser código específico de su vista (widget `wdlg_excel_upload`, `AppMkRootCtrl.MENSAJES`) — era el único uso de PrimeFaces del módulo. `jasper` se desacopló igual de `web` **y de PrimeFaces**: usa `FacesContext` directo y depende solo de `jbs-core` (arrastra interfaces + commons; no usa nada de `jbs-business`). `web` quedó libre de POI y de JasperReports.
- **`rest`** depende solo de `interfaces` + `commons` (no arrastra JSF).
- `interfaces` **ya no depende de POI**: `IExcelImportSrv`/`IExcelRowProcessor` viven ahora en `poi` (paquete `org.javabeanstack.poi.excel`).
- Consecuencia: los consumidores de `jbs-web` que usen Excel, reportes Jasper o recursos REST deben declarar `jbs-poi`, `jbs-jasper` o `jbs-rest` **explícitamente** (antes venían dentro de `jbs-web`).
- Guava se eliminó del framework (su único uso en `AbstractWebResource` se reemplazó por `org.javabeanstack.util.Strings`).
- El split package `org.javabeanstack.web.util` quedó repartido entre `web` (`FacesContextUtil`, `AppResourceSearcher`, `DownloadTarget`) y `jasper` (`JasperReportUtil`, `JasperReportSource`) — coherente con la decisión ya asumida de convivir con split packages; **nunca** agregar `module-info.java`. Con el renombre `excel`→`poi` (2026-08) ese módulo salió del split: sus clases viven en `org.javabeanstack.poi.excel` y `org.javabeanstack.poi.word`.

Cada jar declara `Automatic-Module-Name` vía la propiedad `<jbs.module.name>` (plugin `maven-jar-plugin` en el `pluginManagement` del padre) — reserva de nombres JPMS sin adoptar `module-info.java`.

## Gestión de dependencias

**`jbs-bom` es la fuente única de versiones.** El módulo `bom/` (artefacto `jbs-bom`, packaging pom) centraliza en su `dependencyManagement` las versiones de los 9 artefactos `jbs-*` y de las dependencias de terceros (jakartaee-api, hibernate, primefaces, los artefactos de JasperReports, POI, log4j, junit, wildfly-ejb-client-bom). `jbs-parent` **hereda** del BOM (`<parent>` con `relativePath` = `bom/pom.xml`), por lo que todos los módulos reciben las versiones sin declararlas. Los consumidores (Oym-frame, Maker, TestProject) **importan** el BOM (`<scope>import</scope>`) en su `dependencyManagement`.

- Al subir la versión de una dependencia, editar **solo** `bom/pom.xml`.
- POI: el BOM gestiona `poi`, `poi-ooxml` y `poi-scratchpad` a la misma versión (POI exige que todos sus artefactos coincidan — verificar con `dependency:tree`). Debe alinearse con el `poi` base que arrastra JasperReports.
- Los cuatro artefactos de JasperReports (`jasperreports`, `-servlets`, `-excel-poi`, `-groovy`) más `jasperreports-pdf` comparten versión en el BOM.
- El repositorio `jasper-3rd-party` (jaspersoft.jfrog.io) vive en `jasper/pom.xml` (los `<repositories>` no viajan por import de BOM).
- `jakarta.jakartaee-api` va con scope `provided` (lo provee el servidor de aplicaciones).
- log4j (`log4j-core`/`log4j-api`) y JUnit se declaran en el pom padre sin `<version>` (gestionadas por el BOM); subir ambos artefactos de cada par juntos.

## Abstracciones clave

### Capa de datos (`business`)
- **`DataRow`** — clase base de entidades; las entidades de aplicación la extienden. Trackea estado de acción CRUD (`INSERT=1`, `UPDATE=2`, `DELETE=3`), cambios de campos y errores de validación.
- **`AbstractDAO`** — DAO genérico sobre JPA con construcción de queries, validación de entidades y manejo de errores.
- **`AbstractDataLink`** — envuelve el DAO y gestiona el contexto de unidad de persistencia, sesión de usuario y contexto empresa/schema. Implementa `IDataLink`.
- **`AbstractDataService`** — extiende el DAO con validación de negocio (claves únicas, foreign keys, validación por campo).
- **`DBManager`** — singleton EJB que administra los `EntityManager` por unidad de persistencia (lookup JNDI global).

### Seguridad (`interfaces` + `business`)
- **`IUserSession`** — contexto del usuario autenticado (login, empresa, roles, permisos).
- **`ISecManager`** / **`Sessions`** — autenticación vía EJB, gestión de contraseñas, OAuth. `Sessions` es el **único punto** que crea/elimina sesiones. La política de acceso rol→aplicación (`AppAccessPolicy`, parámetros `<APP>_<ACCESS|WRITE>_<ROL>`) se evalúa **dentro de la creación** en ambos caminos: `createSessionFromToken(token, appName)` y la sobrecarga `createSession(..., appName, otherParams)` (2026-08; rechazo = error número **5**, la firma de 5 args no la evalúa); `reCreateSession` re-evalúa sola porque el `APPNAME` viaja en el objeto sesión.
- **`JwtManager`** / **`DigestAuth`** — soporte JWT y HTTP Digest.

### Manejo de errores (`core`)
- **`IErrorReg`** / **`ErrorReg`** — representación unificada de errores entre capas.
- **`ErrorManager`** — utilidad estática de log de errores: `ErrorManager.showError(ex, LOGGER)`.
- **`LogManager`** — persiste mensajes/eventos en la tabla `AppMessage` (campo `number`).

### Capa web (`web`)
- **`AbstractDataController`** — managed bean JSF base para pantallas CRUD; maneja lazy loading, cache y despliegue de errores.
- **`LazyDataRows`** — paginación lazy para DataTables de PrimeFaces (adaptada a la API de PrimeFaces 15: `load`/`count` con `Map<String,SortMeta>`/`Map<String,FilterMeta>`; los TODO de la migración ya se resolvieron).
- **`FacesContextUtil`** — utilidades sobre `FacesContext` (mensajes, request/response); solo la usa `web` (`poi` y `jasper` usan `FacesContext` directo y no dependen de `jbs-web`).

### Recursos REST (`rest`)
- **`AbstractWebResource`** — base de recursos JAX-RS (validación de token, sesión); **`CORSFilter`**, exceptions y model.
- **`SessionWebResource`** (2026-08) — extiende `WebResource` con el camino de **sesión de login** (cookie `HttpOnly` o `Authorization`): guard único `requireLoginSession` (forma canónica del sessionId + rechazo de sesiones de token + anti CSRF integrado). La aplicación define su cookie y su valor anti CSRF sobrescribiendo `getSessionCookieName()`/`getCsrfHeaderValue()` (defaults `JbsSessionId`/`JavaBeanStack`). Acompañan **`SessionCredential`** y **`PagedList`** (model) y **`RestQuery`** (util: paginación `page/size` y `orderby` con lista blanca).

### Reportes Jasper (`jasper`)
- **`JasperReportUtil`** — exportación de reportes; el parámetro `device` acepta `printer`, `html`, `doc`, `pdf`, `xlsx` (y `xls` como alias de `xlsx`). `getReportPdf(...)` devuelve el PDF como `byte[]` (el envoltorio `StreamedContent` de PrimeFaces, si hace falta, lo arma la capa JSF del consumidor) — por eso el módulo no depende de PrimeFaces. Resolución de archivos (2026-07-22): `getFullPathReport()` y `getJasperReportFrom()` buscan primero en `reports/v7/` (constante `JASPER_VERSION`) y luego en `reports/` — los `.jasper` convertidos a JR7 tienen precedencia sobre los legados.

### POI (`poi`, ex `excel`)
Todo lo construido sobre Apache POI, en dos paquetes por dominio:
- **`org.javabeanstack.poi.excel`** — **`ExcelUtil`** / **`ExcelImportSrv`** / **`ExcelRowProcessor`** (importación/exportación de planillas; contratos **`IExcelImportSrv`** / **`IExcelRowProcessor`**, antes en `interfaces`) y **`ExcelDataSource`** (fuente del subsistema de salida; único camino del subsistema para el formato de planilla). El bean de carga (`ExcelUploadCtrl`) vive en Maker (`net.makerapp.web.uploads`).
- **`org.javabeanstack.poi.word`** — **`WordTemplateMerge`** (merge de marcadores `<<campo>>` por párrafo sobre plantillas .docx) y **`WordTemplateSource`** (fuente del subsistema de salida). Subidos desde Maker el 2026-08-14, relicenciados a LGPL.

### Salida de documentos (`outputs` + adapters)
- **`org.javabeanstack.outputs`** (contratos en `interfaces`; núcleo en el módulo `outputs`, artefacto `jbs-outputs`, que depende solo de `jbs-core`): modelo fuente→documento→destinos. **`OutputDispatcher`** (orquestador fluido: genera una vez, entrega a N destinos, nunca lanza — todo vuelve como `IErrorReg`; log opcional con `EVENT_DOCUMENT_OUTPUT`), **`OutputDocument`**, **`FolderTarget`**. Adapters en el módulo dueño de cada dependencia: `JasperReportSource` (jasper), `MailTarget` (messaging), `ExcelDataSource` (poi), `DownloadTarget` (web, único autorizado a tocar la respuesta HTTP), `WordTemplateSource` (poi). Regla que el subsistema garantiza: *primero generar en memoria, después comprometer el destino*.

## Header de copyright

Todas las clases `.java` del proyecto llevan este header LGPL al inicio del archivo:

```java
/*
* JavaBeanStack FrameWork
*
* Copyright (C) <año-inicio> - 2027 Jorge Enciso
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
```

- Para archivos **nuevos**, usar `2017 - 2027` como rango (el más común en el proyecto).
- Para archivos **existentes**, conservar el año de inicio original y no tocarlo al hacer ediciones normales de código.
- Ya no existen headers propietarios de "OyM System Group S.A." en el repo: se unificaron todos al header LGPL de arriba (commit `8985fd0`). Si aparece código nuevo con ese header propietario u otro distinto, marcarlo para revisión en vez de asumir cuál usar — es una decisión de licenciamiento, no técnica.
- Al editar un archivo que tenga CRLF como salto de línea, preservar ese estilo (no normalizar a LF); mezclar ambos en un mismo archivo genera diffs espurios enormes.

## Estilo de código

- Indentación: 4 espacios. Javadoc y comentarios en español.
- Interfaces con prefijo `I` (`IDataRow`, `ILogManager`). Clases PascalCase, métodos/campos camelCase, constantes `UPPER_SNAKE_CASE`.
- Loggers: `private static final Logger LOGGER = LogManager.getLogger(MiClase.class);` (log4j2).
- No usar `var`; tipos explícitos. Preferir tipos de interfaz en firmas (`List`, `IDataRow`), concretos solo al construir.
- Métodos públicos de servicios/DAO suelen declarar `throws Exception`; no estrechar esas firmas.
- Extender las clases base abstractas existentes en vez de saltearlas. Antes de cambiar código, leer archivos vecinos del módulo y copiar las convenciones locales.
- Preferir ediciones mínimas orientadas a compatibilidad por sobre refactors.

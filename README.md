# JavaBeanStack

Framework Java multi-módulo (Maven) para la construcción de aplicaciones empresariales sobre **Jakarta EE 11**. Provee infraestructura reutilizable de acceso a datos, seguridad, manejo de errores/logging y capas web JSF/REST, pensada para reducir el código repetitivo (boilerplate) en aplicaciones de negocio tipo ERP/CRUD.

Es una **librería/framework**, no una aplicación ejecutable por sí sola: se consume como dependencia Maven desde proyectos de aplicación.

- **Grupo Maven:** `org.javabeanstack`
- **Artefacto padre:** `jbs-parent`
- **Versión actual (rama `master`):** `2.2.0-SNAPSHOT`
- **Licencia:** GNU Lesser General Public License v3.0 (LGPL v3.0)

## Prerrequisitos

- **Java 25** — el POM raíz fija `maven.compiler.release=25`, así que compilar requiere JDK 25 (un JDK anterior falla con `release version 25 not supported`) y el bytecode resultante exige Java 25 en runtime. (Jakarta EE 11 en sí pide 17 como mínimo; el proyecto adoptó 25, alineado al runtime de WildFly 41.)
- **Maven 3.x**
- Para los tests de integración del módulo `business`: un servidor **WildFly 41** corriendo (sirve cualquier WildFly 40+, la primera versión con soporte completo de Jakarta EE 11) con el EAR de pruebas desplegado.

## Estrategia de ramas

El proyecto mantiene dos líneas activas:

| Rama | Versión | Stack | Estado |
|---|---|---|---|
| `master` | 2.2.x | Jakarta EE 11 (`jakarta.*`), Java 25, PrimeFaces 15, JasperReports 7, JUnit 5 | Activa — desarrollo principal |
| `1.5.x` | 1.5.x | Java EE 8 (`javax.*`), PrimeFaces y JasperReports 6 antiguos | Legacy — recibe fixes que luego se portan a `master` |

Al comparar o portar cambios entre ramas, las diferencias `javax.*` ↔ `jakarta.*` son equivalencias de namespace, no diferencias de lógica. Los paquetes `javax.crypto`, `javax.xml.*`, `javax.naming` y `javax.swing` pertenecen al JDK y permanecen como `javax` en ambas ramas.

> **Nota sobre `master`:** PrimeFaces está en `15.0.17` (classifier `jakarta`, versión gestionada en el BOM) y su funcionamiento contra Jakarta Faces 4.1 (parte de EE11) está **verificado en runtime**: las aplicaciones consumidoras que usan `jbs-web` se despliegan y funcionan en WildFly 40+ (hoy WildFly 41).

## Arquitectura de módulos

Los módulos forman una cadena de dependencias en un único sentido — cada nivel depende únicamente de los niveles anteriores:

```
bom         (Bill of Materials — solo dependencyManagement; es el parent de jbs-parent)
    ↓
interfaces  (solo contratos, sin implementaciones)
    ↓
commons     (utilidades de strings, archivos, crypto, fechas)
    ↓
core        (manejo de errores, log de eventos, XML, configuración, recursos)
    ↓
├── business  (acceso a datos JPA/EJB, servicios, seguridad, lógica de negocio)
└── outputs   (salida de documentos: fuente → documento → destinos — depende solo de jbs-core)

business + outputs
    ↓
├── web       (controllers JSF, converters, filtros — solo JSF/PrimeFaces)
└── poi       (Excel y Word con Apache POI — sin PrimeFaces ni jbs-web)

outputs (+ core)
    ↓
├── messaging (canal de correo)
├── jasper    (integración JasperReports)
└── aws       (integración AWS S3 — depende solo de jbs-outputs)

rest        (recursos JAX-RS — depende solo de interfaces + commons)
sifen       (cliente de SIFEN — autónomo: no depende de ningún otro jbs-*, solo del parent)
```

No se deben introducir imports que inviertan este orden (por ejemplo, que `core` dependa de `business`).

Desde la versión 2.0, los módulos `excel` (hoy `poi`), `jasper` y `rest` se extrajeron de `web` para aislar sus dependencias pesadas (Apache POI, JasperReports+Groovy, JAX-RS); en la reorganización posterior `excel` se renombró a `poi` (artefacto `jbs-poi`) al absorber también las plantillas Word. Los consumidores de `jbs-web` que usen importación Excel, reportes Jasper o recursos REST deben declarar `jbs-poi`, `jbs-jasper` o `jbs-rest` **explícitamente** (antes venían dentro de `jbs-web`).

### Split packages y JPMS — decisión de diseño

Los *split packages* entre `jbs-interfaces` y los jars de implementación (contrato e implementación comparten el nombre de paquete, en jars distintos — por ejemplo `org.javabeanstack.log` vive en `jbs-interfaces` y en `jbs-core`) son **intencionales y permanentes**. En consecuencia, **JPMS no se soporta**: nunca agregar `module-info.java` a ningún módulo (dos módulos JPMS no pueden exportar el mismo paquete). Los consumidores corren en WildFly, cuyo classloader (`jboss-modules`) ignora `module-info`; los nombres de módulo quedan reservados vía `Automatic-Module-Name` en el manifest de cada jar. La extensión del framework se hace implementando/extendiendo sus clases **desde un paquete propio** del consumidor — no agregando clases dentro de un paquete del framework.

### `bom`
Bill of Materials (`jbs-bom`, packaging `pom`). Centraliza en su `dependencyManagement` las versiones de los artefactos `jbs-*` y de las dependencias de terceros (jakartaee-api, Hibernate, PrimeFaces, JasperReports, POI, log4j, JUnit). Es el parent de `jbs-parent`, por lo que todos los módulos heredan las versiones sin declararlas; los proyectos consumidores lo importan con `<scope>import</scope>`.

### `interfaces`
Solo contratos (interfaces Java), sin implementaciones. Es la base de la que dependen todos los demás módulos. No tiene dependencias de terceros (los contratos de Excel `IExcelImportSrv`/`IExcelRowProcessor`, que exponían tipos de POI, viven ahora en el módulo `poi`).

### `commons`
Utilidades de propósito general sin dependencias de negocio: manejo de strings, archivos, criptografía y fechas. Sus tests son unitarios puros, ejecutables sin infraestructura externa.

Componentes clave:
- **`ValueConverter`** — conversión de texto al tipo de dato pedido (`fromString(texto, tipo)`): textos, lógicos (`T`/`F`, `true`/`false`, `1`/`0`), numéricos, fechas ISO-8601 o con formato propio y enumeraciones. Es la pieza que usan las importaciones (XML, planillas, parámetros) antes de asignar un valor con `IDataRow.setValue`, que no convierte tipos.

### `core`
Manejo de errores, log de eventos, utilidades XML, configuración de la aplicación y gestión de recursos.

Componentes clave:
- **`IErrorInfo` / `ErrorInfo`** — representación unificada de errores entre capas.
- **`ErrorManager`** — utilidad estática de log de errores (`ErrorManager.showError(ex, LOGGER)`).
- **`LogManager`** — persiste mensajes/eventos en la tabla `AppMessage`.

### `business`
Acceso a datos (JPA/EJB), servicios de negocio y seguridad. Es el módulo con mayor complejidad y el único cuyos tests son de integración.

Componentes clave:
- **`DataRow`** — clase base de la que heredan las entidades de aplicación. Trackea el estado de la acción CRUD (`INSERT=1`, `UPDATE=2`, `DELETE=3`), los cambios de campos y los errores de validación.
- **`AbstractDAO`** — DAO genérico sobre JPA: construcción de queries, validación de entidades y manejo de errores.
- **`AbstractDataLink`** — envuelve al DAO y gestiona el contexto de la unidad de persistencia, la sesión del usuario y el contexto de empresa/schema. Implementa `IDataLink`.
- **`AbstractDataService`** — extiende el DAO agregando validación de negocio (claves únicas, foreign keys, validación por campo).
- **`DBManager`** — singleton EJB que administra los `EntityManager` por unidad de persistencia (lookup JNDI global).
- **`IUserSession`** — contexto del usuario autenticado (login, empresa, roles, permisos).
- **`ISecManager` / `Sessions`** — autenticación vía EJB, gestión de contraseñas, OAuth.
- **`JwtManager` / `DigestAuth`** — soporte de autenticación JWT y HTTP Digest.
- **`XmlRowMapper`** — mapeo de datos XML al modelo (ver abajo).

#### Mapeo XML → modelo (`XmlRowMapper`)

`org.javabeanstack.xml.XmlRowMapper` (contrato `IXmlRowMapper` en `interfaces`) lleva los datos de un nodo XML a una entidad del modelo: cada atributo del nodo se asigna al atributo del modelo que se llame igual **sin distinguir mayúsculas**, convirtiendo el texto al tipo del atributo con `ValueConverter`. Un elemento hijo simple —sin atributos ni hijos propios— se trata como un atributo cuyo valor es su texto (`childElementsAsFields`, encendido por defecto), que es la forma natural de llevar textos largos.

```java
IXmlRowMapper mapper = new XmlRowMapper()
        .ignore("idappobject")                                  // en un UPDATE la clave primaria no se pisa
        .resolver("idappobjecttype", (f, texto, nodo, fila) -> tipos.idPorNombre(texto));

Document dom = DomW3cParser.loadXml(new File("catalogo.xml"));
List<AppObject> filas = mapper.toRows(dom, "/DATA/ROW", AppObject.class);   // nuevas, en INSERT

AppObject existente = ...;        // leída de la base por su clave natural
mapper.fill(nodo, existente);     // solo pisa lo que el nodo trae
if (!existente.getErrors().isEmpty()) { /* la fila se reporta y no se graba */ }
```

- Los atributos considerados son los de la clase y sus superclases **hasta `DataRow` exclusive**, sin los `static` ni los `transient`: los campos propios de `DataRow` (`action`, `errors`…) no son alcanzables desde el XML.
- Un dato inválido **no interrumpe** el proceso: la fila acumula el error (`setErrors`, número 50000, con el nombre del atributo) y el mapeo sigue. Solo se lanza excepción ante un error de programación (nodo nulo, clase sin constructor sin argumentos).
- Un atributo sin propiedad se ignora por defecto; `strict(true)` lo convierte en error de la fila y `onUnmapped(listener)` avisa sin fallar. `ignore(...)` declara los que nunca se asignan: es **acumulativo y sin reversa**, así que un cargador con dos políticas (tomar del XML el identificador en el alta y no pisarlo en la modificación) usa **dos mapeadores**.
- El texto de un elemento hijo simple se asigna **tal cual**: los saltos de línea, los tabuladores y la sangría son parte del dato, porque el mapeador lleva datos y no ayudas. `normalizeChildText(true)` recupera el recorte y el colapso de blancos, que sirve para un texto de una sola línea escrito en varias por comodidad del archivo. Normalizar por omisión hacía perder el formato de los `authdef` del catálogo de Maker —un XML de autorizaciones con CRLF y tabuladores— y reescribía esas filas en la primera carga de toda instalación.
- Casos dudosos resueltos: dos atributos del modelo que **solo difieren en mayúsculas** hacen que el nombre sea ambiguo y no se asigna ninguno (la capa que asigna no los distingue); entre un atributo y un elemento hijo **homónimos** gana el atributo y el hijo se reporta como no mapeado; un texto vacío sobre un atributo **primitivo** es error de la fila, no un nulo; los campos anotados `@Transient` (JPA) quedan fuera del índice.
- `resolver(campo, IXmlFieldResolver)` es el punto de extensión para los valores que solo la aplicación sabe interpretar (claves foráneas por clave natural).
- No lee archivos (recibe `Element`/`Document`/`IXmlDom` ya parseados por `DomW3cParser`) y no persiste nada. La sobrecarga `toRows(IXmlDom, nodePath, type)` es la que aprovecha la herencia de textos XML (`clase`/`src`).

### `outputs`
Subsistema de salida de documentos (`jbs-outputs`): una **fuente** produce el documento en memoria y uno o más **destinos** lo entregan, cada uno por su canal. Los contratos (`IDocumentSource`, `IOutputDocument`, `IDocumentTarget`) viven en `interfaces`; el núcleo, en este módulo, que depende solo de `jbs-core`.

Componentes clave:
- **`OutputDispatcher`** — orquestador: genera el documento **una sola vez** y lo entrega a todos los destinos, con la regla *primero generar, después comprometer el destino*. Nunca lanza: todo fallo, de generación o de entrega, vuelve como `IErrorInfo`; un destino que falla no impide los demás.
- **`OutputDocument`** — el documento en memoria (nombre, tipo de contenido y bytes).
- **`FolderTarget`** — destino que guarda el documento en una carpeta del servidor.

Las fuentes y destinos con dependencias pesadas viven en el módulo dueño de cada una: `JasperReportSource` (`jasper`), `ExcelDataSource` y `WordTemplateSource` (`poi`), `MailTarget` (`messaging`), `DownloadTarget` (`web`) y `S3Target` (`aws`).

### `messaging`
Canal de correo (`jbs-messaging`). Depende de `jbs-outputs`, `jbs-core` y `jakarta.mail-api` (la implementación la provee el servidor). Permite enviar un correo sin base de datos.

Componentes clave:
- **`MailMessage`** — el mensaje en memoria, con sus partes (`MailAddress`, `MailAttachment`).
- **`MailAccount`** — los datos de la cuenta de envío; la aplicación decide de dónde salen (parámetros, una tabla por empresa, valores fijos).
- **`MailSessionProvider`** — arma la sesión de correo desde la cuenta; si la cuenta no define un servidor SMTP, usa la sesión del contenedor.
- **`MailSender`** — envío SMTP síncrono. Nunca lanza: el resultado (`MailSendResult`) dice si el fallo es definitivo o recuperable.
- **`MailTarget`** — destino del subsistema de salida que envía el documento como adjunto de un correo.

### `web`
Controllers JSF, converters y filtros web. Es el único módulo con dependencia de PrimeFaces; quedó libre de POI, JasperReports y JAX-RS.

Componentes clave:
- **`AbstractDataController`** — managed bean JSF base para pantallas CRUD: maneja carga diferida (lazy loading), caché y despliegue de errores.
- **`LazyDataRows`** — paginación lazy para DataTables de PrimeFaces.
- **`FacesContextUtil`** — utilidades sobre `FacesContext` (mensajes, request/response).

### `poi`
Todo lo construido sobre Apache POI (`jbs-poi`, antes `jbs-excel`): planillas Excel y documentos Word. Depende de `jbs-business`, `jbs-outputs` y `poi-ooxml`; usa `FacesContext`/`jakarta.servlet` directos (API provided), sin PrimeFaces ni `jbs-web`. Detalle en [`poi/README.md`](poi/README.md).

Componentes clave:
- **`org.javabeanstack.poi.excel`** — **`ExcelUtil`** / **`ExcelImportSrv`** / **`ExcelRowProcessor`** (importación y exportación de planillas, con sus contratos **`IExcelImportSrv`** / **`IExcelRowProcessor`**, antes en `interfaces`) y **`ExcelDataSource`** (fuente del subsistema de salida).
- **`org.javabeanstack.poi.word`** — **`WordTemplateMerge`** (combinación de marcadores sobre plantillas `.docx`), **`WordTemplateSource`** (fuente del subsistema de salida) y **`WordToPdfConverter`**.

### `jasper`
Integración con JasperReports. Depende de `jbs-core` y `jbs-outputs`, sin PrimeFaces ni `jbs-web`; aporta la fuente `JasperReportSource` al subsistema de salida.

- **`JasperReportUtil`** — exportación de reportes; el parámetro `device` acepta `printer`, `html`, `doc`, `pdf`, `xlsx` (y `xls` como alias de `xlsx`). `getReportPdf(...)` devuelve el PDF como `byte[]`; el envoltorio `StreamedContent` de PrimeFaces, si hace falta, lo arma la capa JSF del consumidor. Al resolver un reporte busca primero en la subcarpeta de versión `reports/v7/` y recién después en `reports/` — así los `.jasper` ya convertidos a JasperReports 7 tienen prioridad sobre los legados sin exigir una migración big-bang.

### `rest`
Recursos JAX-RS. Depende solo de `interfaces` + `commons` (no arrastra JSF).

- **`AbstractWebResource`** — base de recursos REST (validación de token, sesión); **`CORSFilter`**, exceptions y model.

### `aws`
Integración con AWS S3. No forma parte de la cadena principal: depende solo de `jbs-outputs`, al que aporta el destino `S3Target`, y puede incorporarse de forma opcional según lo necesite la aplicación consumidora.

### `sifen`
Cliente de **SIFEN** (`jbs-sifen`), la facturación electrónica de la DNIT de Paraguay: armado, firma y envío de documentos electrónicos y eventos, y consultas. Es **autónomo**: no depende de otros módulos `jbs-*` ni de una base de datos, solo del parent. Deriva de [`rshk-jsifenlib`](https://github.com/roshkadev/rshk-jsifenlib) (Roshka S.A. + Taxare S.A., licencia MIT); el aviso está en el [`NOTICE`](NOTICE). Detalle, configuración y despliegue en [`sifen/README.md`](sifen/README.md).

Componentes clave:
- **`ISifenClient`** — fachada de la API pública v1, un cliente por empresa; **`SifenClientRegistry`** los crea y guarda por clave de empresa.
- **Emisión en dos pasos** — `sign` (valida, calcula el CDC, firma y arma el QR; la aplicación graba el XML) y después `send`; `resolve` resuelve un envío de resultado desconocido sin reenviar a ciegas. También lote, eventos (cancelación, inutilización y los demás) y consultas de DE, RUC y lote.
- **Validación previa** del documento, activa por omisión, con todos los errores juntos y mensajes en español.
- **`SifenException`** — modelo de errores único, con código estable, mensaje para el usuario e id de correlación.
- **Transporte sin reinicio** — contexto TLS por cliente, que se rearma ante una falla o un cambio de certificado sin reiniciar la JVM; tiempos de espera, proxy y reintentos configurables.

## Compilación y build

Todos los comandos se ejecutan desde la raíz del repositorio.

```bash
# Compilar todo el proyecto (incluye tests) sin ejecutarlos
mvn test-compile -DskipTests

# Build completo, generando los artefactos
mvn clean package

# Instalar los artefactos en el repositorio Maven local (~/.m2)
mvn clean install

# Compilar un módulo puntual junto con sus dependencias upstream
mvn -pl web -am clean package
```

No hay lint ni formatter configurado en el proyecto; la validación del código se realiza compilando (`mvn test-compile` / `mvn test`).

## Tests

```bash
# Todos los tests de un módulo
mvn -pl commons test

# Una clase de test puntual
mvn -pl commons -Dtest=StringsTest test

# Un método puntual dentro de una clase de test
mvn -pl poi -Dtest=ExcelUtilTest#testOpenWorkbookFile test

# Cobertura de código (JaCoCo)
mvn -Psonar-coverage test
```

Qué se puede ejecutar en un entorno local sin infraestructura adicional:

- **`commons`**, **`core`**, **`web`**, **`poi`**, **`outputs`**, **`messaging`** — tests unitarios puros, se pueden correr siempre (`mvn -pl commons,core,web,poi,outputs,messaging test`).
- **`sifen`** — corre sin red y sin certificado real: firma con un certificado autofirmado y prueba los envíos y consultas contra el simulador de SIFEN que se publica en su `-tests.jar` (`mvn -pl sifen test`). Los tests marcados `@Tag("sifen-live")`, que van contra el ambiente de pruebas de SIFEN, quedan **excluidos por omisión** (ver [`sifen/README.md`](sifen/README.md#tests)).
- **`business`** (y cualquier módulo cuyos tests extiendan `TestClass`) — son **tests de integración**: requieren un servidor WildFly/JBoss corriendo con el EAR de pruebas desplegado (lookup EJB remoto vía `http-remoting`). Se configuran mediante variables de entorno:

  | Variable | Default | Descripción |
  |---|---|---|
  | `SERVER_TEST` | `localhost` | Host del servidor WildFly |
  | `SERVER_TEST_PORT` | `8080` | Puerto del servidor |
  | `SECURITY_PRINCIPAL` | — | Usuario para el lookup EJB remoto |
  | `SECURITY_CREDENTIALS` | — | Contraseña para el lookup EJB remoto |
  | `APP_USER_LOGIN` | `test1` | Usuario de aplicación para los tests |
  | `APP_USER_PASS` | `test1` | Contraseña de aplicación para los tests |
  | `APP_IDCOMPANY` | `2` | Id de empresa/contexto usado en los tests |

  Sin ese servidor disponible, estos tests fallan directamente en el setup — no es un bug del código, es la infraestructura faltante.

## Gestión de dependencias — notas relevantes

- **`jbs-bom` es la fuente única de versiones**: al subir la versión de una dependencia, editar solo `bom/pom.xml`. Los módulos heredan las versiones vía `jbs-parent`; los consumidores importan el BOM con `<scope>import</scope>`.
- El BOM gestiona `poi`, `poi-ooxml` y `poi-scratchpad` a la misma versión (POI exige que todos sus artefactos coincidan — verificar con `mvn dependency:tree -Dincludes=org.apache.poi -pl poi`). Debe mantenerse alineada con el `poi` base que arrastra JasperReports.
- Los artefactos de JasperReports declarados en `jasper/pom.xml` (`jasperreports`, `jasperreports-servlets`, `jasperreports-excel-poi`, `jasperreports-groovy`, `jasperreports-pdf`) comparten versión en el BOM. El repositorio `jasper-3rd-party` (jaspersoft.jfrog.io) se declara en `jasper/pom.xml` porque los `<repositories>` no viajan por import de BOM.
- `jakarta.jakartaee-api` se declara con scope `provided`: la implementación la provee el servidor de aplicaciones (WildFly).
- `log4j-core` / `log4j-api` y JUnit se declaran en el POM padre sin `<version>` (gestionadas por el BOM); al actualizar, subir ambos artefactos de cada par juntos.

## Publicación de artefactos

Los artefactos se publican a un repositorio Nexus interno propio, no a Maven Central. Para consumir JavaBeanStack desde otro proyecto Maven se puede:

- Resolver los artefactos desde el repositorio Nexus interno, o
- Compilar e instalar el proyecto localmente con `mvn clean install` para que quede disponible en el repositorio Maven local (`~/.m2`).

Cada push a la rama `master` dispara automáticamente un `mvn deploy` (workflow `.github/workflows/maven_deploy.yml`) que publica los artefactos al Nexus interno. Antes de hacer push a `master` es importante verificar que el proyecto compila completo.

## Estilo de código

- Indentación de 4 espacios. Javadoc y comentarios en español.
- Interfaces con prefijo `I` (`IDataRow`, `ILogManager`). Clases en PascalCase, métodos y campos en camelCase, constantes en `UPPER_SNAKE_CASE`.
- Loggers declarados como `private static final Logger LOGGER = LogManager.getLogger(MiClase.class);` (log4j2).
- No se usa `var`: se prefieren tipos explícitos, y tipos de interfaz en las firmas de métodos (`List`, `IDataRow`) en vez de tipos concretos.
- Los métodos públicos de servicios y DAOs suelen declarar `throws Exception`; no se deben estrechar esas firmas al sobrescribir o extender.
- Se prefiere extender las clases base abstractas existentes antes que reimplementar funcionalidad equivalente.

## Licencia

Este proyecto se distribuye bajo los términos de la **GNU Lesser General Public License v3.0 (LGPL v3.0)**. El texto completo de la licencia se encuentra en el archivo [`LICENSE`](LICENSE) de este repositorio.

El archivo [`NOTICE`](NOTICE) lleva los avisos de terceros, entre ellos el de la licencia MIT de Roshka S.A. + Taxare S.A. del que deriva `jbs-sifen` (también en [`sifen/LICENSE-rshk-jsifenlib.md`](sifen/LICENSE-rshk-jsifenlib.md)).

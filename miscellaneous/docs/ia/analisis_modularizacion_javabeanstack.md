# Análisis y propuesta de optimización de la estructura modular de JavaBeanStack

> Fecha original: 2026-07-06 (sobre jbs-parent **1.5.11-SNAPSHOT**, Java EE 8)
> **Última actualización: 2026-07-12** — sobre `master` **2.0.0-SNAPSHOT** (Jakarta EE 11, Java 25), con la partición de `jbs-web` ya ejecutada, `jbs-excel` desacoplado de `jbs-web`/PrimeFaces y `DBManagerV21` eliminado
> Documento hermano: análisis de Maker (`maker/Maker-miscellaneous/docs/ia/logs/analisis_modularizacion.md`).
> Copia única: este documento vive solo en JavaBeanStack (`miscellaneous/docs/ia/`); no se mantiene copia en Maker.
> Alcance: solo sugerencias — ningún cambio de código acompaña este documento.

---

## 0. Estado de ejecución de la propuesta original (corte 2026-07-12)

Entre el análisis original y hoy ocurrió el evento mayor del roadmap: la **migración a Jakarta EE 11 se ejecutó completa y antes que la fase A** (commit `3047d64` y siguientes; `master` = 2.0.0-SNAPSHOT, rama `1.5.x` como legacy `javax`). Estado punto por punto de la propuesta de §2.2:

| # §2.2 | Propuesta | Estado | Detalle |
|---|---|---|---|
| 1 | Sacar POI de `jbs-interfaces` (nuevo `jbs-excel`) | ✅ **Ejecutado (2026-07-11)** | `IExcelImportSrv`/`IExcelRowProcessor` + `ExcelImportSrv`/`ExcelRowProcessor`/`ExcelRowImportProcessor`/`ExcelUtil` movidos a `jbs-excel` (mismo paquete `org.javabeanstack.web.util`). `ExcelUploadCtrl`/`IExcelUploadCtrl` migraron después a **Maker** (`net.makerapp.web.uploads`, commit `9c203ee` / Maker `4bea262`). `interfaces` ya no depende de `poi-ooxml` |
| 2 | Promover `appcatalog` de test a producción | 🔵 **Descartado por diseño (decisión 2026-07-12)** | **No se ejecutará.** Las entidades `App*` en `business/src/test` son implementaciones de referencia para los tests del propio framework; el contrato productivo es la **interfaz `IApp*` en `jbs-interfaces`** (p. ej. `AppUser extends DataRow implements IAppUser`). El framework **no** publica entidades concretas a propósito: cada consumidor implementa las suyas contra esas interfaces, mapeadas a su propio esquema (Maker lo hace en `Maker-model`, `org.javabeanstack.model.appcatalog.AppUser implements IAppUser`). Promoverlas a producción impondría un mapeo concreto a todos los consumidores, rompiendo justamente el punto de extensión. Ver §1.2 y §2.2·2 |
| 3 | Partir `jbs-web` (`jbs-rest`, `jbs-excel`, `jbs-jasper`) | ✅ **Ejecutado (2026-07-11/12)** | `jbs-web` quedó solo JSF/PrimeFaces (18 clases). Se crearon `jbs-rest` (JAX-RS, paquete propio `web.rest.*`, depende solo de interfaces+commons), `jbs-excel` (POI) y `jbs-jasper` (Jasper+Groovy). En una segunda pasada (commit `9c203ee`, 2026-07-12) **`jbs-excel` también se desacopló de `jbs-web` y de PrimeFaces**: hoy depende solo de `jbs-business` + `poi-ooxml` (el `ExcelUploadCtrl` que usaba `FacesContextUtil` migró a Maker). `jasper` se desacopló de `web` **y de PrimeFaces** (usa `FacesContext` directo; `getReportPdf` devuelve `byte[]`; depende solo de `jbs-core`). Guava eliminada del framework |
| 4 | (Opcional) fusionar `jbs-core` en `jbs-commons` | 🔵 **Descartado por decisión (2026-07-12)** | **No se ejecutará.** Se prefiere mantener `jbs-core` y `jbs-commons` como módulos separados; el beneficio de fusionarlos era marginal (reducir en uno la lista de artefactos) y la separación actual —`commons` utilerías puras, `core` implementación de error/xml/config/log/resources— es clara. Ver §2.2·4 |
| 5 | `dependencyManagement` en el padre + `jbs-bom` | ✅ **Ejecutado (2026-07-11)** | `jbs-bom` publicado como **parent de `jbs-parent`** (herencia por `relativePath`, evita el huevo-gallina de import en Maven 3). Fuente única de versiones (jbs-*, jakartaee, hibernate, primefaces, Jasper, POI×3, log4j, junit, wildfly). Importado (`scope=import`) en Oym-frame, Maker y TestProject |
| 6 | Hibernate y wildfly-ejb-client a scope `provided` | ✅ **Superado** | Ambos quedaron en scope **`test`** en `business`: el código main compila contra JPA puro (Persistence 3.2); el provider de Hibernate se referencia solo por string (`Class.forName`) en los DBManager dinámicos. Mejor que lo propuesto |
| 7 | Overrides por classpath → puntos de extensión | 🟡 Parcial (avanzando) | La selección de `IDBManager` (clásico/V20/V30; V21 eliminado el 2026-07-12) vía `ejb-jar.xml` del consumidor quedó consolidada y **documentada** como punto de extensión (ver README de `miscellaneous/docs/ia`). **Log y seguridad de objetos resueltos (2026-07-12)**: `LogMngrData`/`LogMngrSecurity` (genéricas, duplicadas) se **promovieron a `jbs-core`** con el mismo FQN (sin tocar los `ejb-jar.xml`); `AppObjectAuthSrv` se **repaquetó** de `py.com.oym.frame.security` a `net.makerapp.security` en Maker (no podía bajar a Oym-frame por sus dependencias a Maker-services-api/Maker-model). **Queda uno**: dialectos Hibernate. (`appcatalog` **no cuenta como override**: es implementación de interfaz por diseño — ver punto 2.) |
| 8 | Tests en core/web + `Automatic-Module-Name` | ✅ **Ejecutado (2026-07-11)** | `core` pasó de 0 a 51 tests (error/xml); `web` sumó `LocalDateTimeConverterTest`; los 4 tests Excel se movieron a `excel` (58 tests). Todos los jars declaran `Automatic-Module-Name` vía `<jbs.module.name>` + `maven-jar-plugin` en el padre |
| §2.3 | Eliminar split packages en la ventana de ruptura 2.0 | ❌ **Ventana perdida** | La 2.0 (Jakarta) salió **sin** eliminar los 13 split packages ni rediseñar las firmas POI. La partición de `web` extendió el split `web.util` a 3 jars (`web`/`excel`/`jasper`). Ver §2.4 |
| §3.2 | Plan de migración Jakarta | ✅ Ejecutado (con salvedades) | Ver §3 actualizado |

**Lectura general (actualizada 2026-07-12)**: se ejecutaron los cuatro puntos compatibles de la propuesta (1, 3, 5, 8) dentro de `2.0.0-SNAPSHOT`, sin romper firmas ni paquetes (salvo el reemplazo interno de Guava). El punto 2 (`appcatalog` a producción) se **descartó por diseño** (ver fila 2). Quedan pendientes: el cierre del punto 7 (solo dialectos Hibernate; log y seguridad ya resueltos) y la eliminación de split packages (2.3/2.4), que exige ventana de ruptura. **Nota de compatibilidad**: como `jbs-web` ya no arrastra POI, Jasper, JAX-RS ni Guava, los consumidores que los usen deben declarar `jbs-excel`/`jbs-jasper`/`jbs-rest`/`guava` explícitamente — ya aplicado en Oym-frame, Maker y TestProject.

**Novedades posteriores al corte 2026-07-11** (reflejadas en el resto del documento):

- `9c203ee` — `jbs-excel` desacoplado de `jbs-web` y PrimeFaces (depende solo de `jbs-business` + POI); `ExcelUploadCtrl`/`IExcelUploadCtrl` migraron a Maker (`net.makerapp.web.uploads`).
- `ff10abc` — `README.md` del repo actualizado a la arquitectura 2.0 (10 módulos).
- `f2b7784` — JasperReports **7.0.4 → 7.0.7** en el BOM por **CVE-2026-6009** (deserialización Java → RCE).
- `5a611dd` — **`DBManagerV21` eliminado**, superado por `DBManagerV30`; la familia queda en 3 implementaciones (ver §1.3). En Maker se retiró también la plantilla `DINAMIC_PU` de `persistence.xml` (Maker `cc654ed`).
- 2026-07-12 — **`LazyDataRows` corregida y documentada** (R1 cerrado provisional): TODOs resueltos, ClassCastException al filtrar columnas no-String corregido, consumidores verificados en todos los repos. Ver [`analisis_lazydatarows_pf15.md`](analisis_lazydatarows_pf15.md).

---

## 1. Diagnóstico de la estructura actual

### 1.1 Métricas relevadas (actualizadas a `master` 2.0.0-SNAPSHOT, post-partición, corte 2026-07-12)

| Módulo | Clases main | Tests | Contenido | Dependencias externas destacadas |
|---|---|---|---|---|
| jbs-bom | — | — | BOM publicado, **parent de `jbs-parent`** | (solo `dependencyManagement`) |
| jbs-interfaces | 80 | 0 | Contratos (data, model, security, xml, log, annotation…) | **jakartaee-api 11 (provided)** — **ya sin POI** |
| jbs-commons | 11 | 7 | Utilidades (Strings, Fn, crypto, io) | commons-lang3/io/validator, number2words |
| jbs-core | 12 | 8 clases / 51 @Test | error, xml, config, log, resources (impl) | interfaces + commons |
| jbs-business | 47 | 44 | DataRow, AbstractDAO, DataLink, DataService, seguridad, **familia DBManager ×3** | hibernate-core 7.3.2 y wildfly-ejb-client-bom 40 **solo scope test** |
| jbs-web | 18 | 1 | Solo JSF: controllers, converters, filtros, `LazyDataRows` | **primefaces 15.0.6 (classifier jakarta)** |
| jbs-rest | 8 | 0 | JAX-RS: resources, filters, exceptions, model (paquete propio `web.rest.*`) | interfaces + commons |
| jbs-excel | 6 | 4 | Import Excel (`ExcelImportSrv`, `ExcelUtil`, interfaces `IExcel*`) | jbs-business + **poi-ooxml 5.4.1** |
| jbs-jasper | 1 | 0 | `JasperReportUtil` | jbs-core + **jasperreports 7.0.7 + groovy** |
| jbs-aws | 2 | 1 | S3 | awssdk s3 2.29.0 |

Total: **185 clases** de producción (188 en el corte 2026-07-11: −1 `DBManagerV21` eliminado, −2 `ExcelUploadCtrl`/`IExcelUploadCtrl` migrados a Maker). Datos estructurales adicionales:

- **13 split packages entre módulos** — la lista nominal es la misma del análisis original: `config`, `data`, `data.services`, `datactrl`, `error`, `log`, `resources`, `security`, `security.model`, `web.model`, `web.util`, `xml` (interfaces vs. core/business/web) y `util` (commons/business). Es el patrón deliberado "contrato e implementación en el mismo paquete, distinto jar". La partición de `web` no agregó paquetes nuevos a la lista pero **extendió `web.util` a 3 jars** (`web`, `excel`, `jasper`); `jbs-rest`, en cambio, nació con paquete propio (`org.javabeanstack.web.rest.*`) y no participa de ningún split.
- **Las entidades del catálogo (`org.javabeanstack.model.appcatalog`) están en `business/src/test`, no en producción — por diseño.** El framework publica solo las **interfaces `IApp*`** (en `jbs-interfaces`); las clases `App*` de test son implementaciones de referencia para sus propios tests. Cada consumidor implementa las entidades concretas contra esas interfaces y las mapea a su esquema (Maker en `Maker-model`). No es una copia "pendiente de promover" sino el mecanismo de extensión previsto (ver §1.2 y §2.2·2).
- Los EJB casi no usan anotaciones (solo `Sessions` lleva `@Stateless`): los `DBManager` y demás se declaran como EJBs en el `ejb-jar.xml` **del consumidor**. Lo que en 2026-07-06 se señaló como patrón implícito hoy es un **mecanismo de conmutación documentado**: la app elige la implementación de `IDBManager` en su `ejb-jar.xml` (ver §1.3).
- La cadena de dependencias declarada se respeta sin ciclos: interfaces → commons → core → business → web; de business cuelga también `excel`; de core, `jasper`; `rest` depende solo de interfaces+commons; `aws` aparte.
- El padre **ya tiene `dependencyManagement`** (resuelto desde el análisis original) y desde 2026-07-11 hereda de **`jbs-bom`**, la fuente única de versiones; los módulos heredan sin `<version>`.
- Estrategia de ramas nueva: **`master` (2.0, `jakarta.*`) y `1.5.x` (legacy, `javax.*`)** que recibe fixes de lógica porteados a mano hacia master. Esto agrega una restricción que el análisis original no tenía: **todo refactor estructural en master encarece el porteo de fixes desde 1.5.x** (los diffs dejan de aplicar limpio).

### 1.2 Evaluación

**Lo que está bien (y lo que mejoró):**

- El tamaño sigue sano: 185 clases bien repartidas, sin "módulo Dios". La separación contratos/implementación como módulos Maven distintos ya existe.
- Dirección de dependencias limpia, `aws` correctamente aislado.
- `business` sigue siendo el módulo mejor cubierto (44 tests de integración, migrados a JUnit 5 y adaptados al naming remoto de WildFly 40); `core` pasó de 0 a 51 tests.
- **Mejora mayor desde el análisis original**: `business` ya no acopla al vendor JPA — hibernate-core pasó de dependencia compile (5.4.24) a **scope test** (7.3.2), y el código main usa solo la API `jakarta.persistence` (Persistence 3.2, incl. `PersistenceConfiguration` en los DBManager dinámicos). El punto 6 de la propuesta quedó superado.
- **Resueltos los dos problemas estructurales top del análisis original** (2026-07-11/12): `jbs-interfaces` quedó limpio de POI, y `jbs-web` dejó de ser cajón mixto — hoy cada dependencia pesada vive en su módulo (`jbs-excel`/POI, `jbs-jasper`/Jasper+Groovy, `jbs-rest`/JAX-RS, `jbs-web`/PrimeFaces) y Guava salió del framework. Los tres módulos nuevos quedaron además desacoplados entre sí (excel no depende de web ni de PrimeFaces).
- **El catálogo por interfaz es un acierto de diseño, no una deuda** (decisión 2026-07-12): el framework publica los contratos `IApp*` en `jbs-interfaces` y deja que cada consumidor implemente las entidades concretas contra su propio esquema. Que las clases `App*` de referencia vivan en `business/src/test` es coherente con eso; no hay que promoverlas (revierte el análisis original en este punto — ver §2.2·2).
- Versiones al día y centralizadas en `jbs-bom`: Jakarta EE 11, Java 25, PrimeFaces 15.0.6, JasperReports 7.0.7 (parche CVE-2026-6009), POI 5.4.1, JUnit 5, log4j 2.25.4.

**Los problemas que siguen abiertos:**

1. **Los 13 split packages sobrevivieron a la ventana 2.0**: la migración Jakarta era la ruptura donde eliminarlos "gratis" (los consumidores ya tocaban todos los imports por `javax→jakarta`) y no se aprovechó; la partición de `web` incluso extendió `web.util` a 3 jars. Siguen (a) bloqueando JPMS para siempre, (b) habilitando el override por orden de classpath que Maker **explotaba** (log/seguridad, ya resueltos el 2026-07-12 — hoy ningún consumidor lo usa), y (c) desdibujando la frontera API/impl. La próxima ventana natural es una eventual 3.0 — o aceptarlos como decisión permanente (ver §2.4).
2. **Cobertura de tests aún desigual en la capa web**: `core` ya se cubrió (51 tests) y `excel` conservó los suyos (4 clases), pero `jbs-web` tiene 1 solo test, y `rest`/`jasper` ninguno. La migración Jakarta se validó con compilación completa + tests de `commons`/`core` + despliegue real del EAR de Maker en WildFly 40 (2026-07-09), pero la validación runtime de la capa JSF (Faces 4.1 con PrimeFaces 15) sigue pendiente de un ciclo funcional serio. Avance 2026-07-12: `LazyDataRows` (el punto más riesgoso de esa capa) ya fue corregida y verificada estáticamente — ver [`analisis_lazydatarows_pf15.md`](analisis_lazydatarows_pf15.md); el ciclo funcional quedó diferido a la conversión de formularios PF6→15.
3. **El costo de la doble rama**: mientras `1.5.x` reciba fixes, cada movimiento de clases/módulos en master duplica el costo de mantenimiento (la partición de `web` ya lo encareció: un fix de `1.5.x` en Excel/Jasper/REST hoy aterriza en otro módulo). Los refactors estructurales restantes de §2.2 convienen agruparse y ejecutarse cuando el flujo de fixes hacia 1.5.x se apague, o asumiendo el porteo manual.

### 1.3 Novedad estructural: la familia `IDBManager`

Desde el análisis original, `business` incorporó implementaciones alternativas del administrador de unidades de persistencia, conmutables por el `ejb-jar.xml` del consumidor (documentación canónica en este mismo directorio, ver `README.md`). Tras la eliminación de `DBManagerV21` (2026-07-12, commit `5a611dd`, superado por `DBManagerV30`; Maker retiró a la vez la plantilla `DINAMIC_PU` de su `persistence.xml`), la familia vigente es de **tres**:

| Implementación | Estrategia |
|---|---|
| `DBManager` (clásico) | PUs declaradas en `persistence.xml`, bindings JNDI `java:app/em/PUn` |
| `DBManagerV20` | EMF dinámicos por system properties (`jbs.persistence.dynamic.*`) — experimental |
| `DBManagerV30` | PUs definidas en `META-INF/dynamic_persistence.xml`, spec completa por unidad, plantilla `DEFAULT` con `{n}` para alta de empresas sin redeploy |

(`DBManagerV21` — EMF dinámicos por plantilla `DINAMIC_PU` comentada en `persistence.xml` — existió entre 2026-07-10 y 2026-07-12 y fue absorbido por V30.)

Esto es relevante para la propuesta por dos motivos: (a) valida en la práctica el mecanismo "el consumidor elige la implementación vía `ejb-jar.xml`" como **punto de extensión explícito y documentado** — exactamente el patrón que el punto 7 de §2.2 pide generalizar (log y seguridad ya lo siguen desde 2026-07-12; falta dialectos); y (b) los DBManager dinámicos demuestran que se puede depender de Hibernate **sin compilarlo** (provider referenciado por string, API JPA pura), el mismo criterio a aplicar si `web` u otros módulos necesitan aislar vendors.

---

## 2. Propuesta de optimización modular (vigente)

### 2.1 Principio rector

Sin cambios: aquí **no hay nada grande que partir** — la granularidad ya es correcta. El trabajo es **sanear fronteras** (qué vive en qué módulo y qué arrastra cada uno) y **convertir el patrón de override por classpath en puntos de extensión explícitos**, generalizando el modelo que la familia DBManager ya estableció. Con la ventana de ruptura Jakarta ya consumida, todo cambio debe medirse además contra el costo de porteo de fixes desde `1.5.x`.

### 2.2 Cambios de estructura sugeridos (compatibles, sin romper consumidores)

La estructura objetivo del análisis original **ya se materializó** (2026-07-11/12); el único punto no ejecutado (`appcatalog` a producción) se descartó por diseño:

```
jbs-parent (hereda de jbs-bom)
├── jbs-bom             ← ✅ HECHO: BOM publicado, parent de jbs-parent e importado por consumidores
├── jbs-interfaces      ← ✅ HECHO: sin POI (publica los contratos IApp* del catálogo)
├── jbs-commons         ← igual (se mantiene separado de jbs-core — decisión, ver 4º punto)
├── jbs-core            ← igual
├── jbs-business        ← igual (entidades appcatalog se quedan en test — decisión, ver 2º punto)
├── jbs-web             ← ✅ HECHO: solo JSF/PrimeFaces + filtros + util web liviano
├── jbs-rest            ← ✅ HECHO: web/rest/resources, filters, exceptions, model
├── jbs-excel           ← ✅ HECHO: import Excel (POI) + IExcelImportSrv/IExcelRowProcessor
├── jbs-jasper          ← ✅ HECHO: integración JasperReports (+ groovy)
└── jbs-aws             ← igual
```

1. ~~Sacar POI de `jbs-interfaces`~~ — **✅ Hecho (2026-07-11)**: `IExcelImportSrv`/`IExcelRowProcessor` viven en `jbs-excel` manteniendo el paquete `org.javabeanstack.web.util` (los imports de Maker no cambiaron, solo la dependencia Maven). Sigue vigente la alternativa ambiciosa **no** ejecutada: rediseñar las firmas para no exponer tipos POI es ruptura y debe esperar una 3.0 o hacerse con métodos nuevos + deprecación.
2. ~~Promover `appcatalog` de test a producción~~ — **🔵 Descartado por diseño (decisión 2026-07-12).** La propuesta original leyó las entidades `App*` de test como una duplicación a consolidar, pero el diseño es deliberadamente el inverso: el framework publica los **contratos `IApp*`** (en `jbs-interfaces`) y **cada consumidor implementa sus propias entidades** contra ellos, mapeadas a su esquema (`AppUser extends DataRow implements IAppUser`, presente tanto en las clases de test del framework como en `Maker-model`). Publicar entidades concretas en producción impondría un mapeo JPA único (nombres de tabla/columna, secuencias, dialecto) a todos los consumidores, que es precisamente lo que este esquema evita. Las clases `App*` de `business/src/test` cumplen su rol como implementaciones de referencia para los tests del framework y **se quedan donde están**. En consecuencia, `appcatalog` **sale de la lista de overrides a eliminar** (punto 7): no es un override, es una implementación de interfaz.
3. ~~Partir `jbs-web` por peso de dependencias~~ — **✅ Hecho (2026-07-11/12)**: `jbs-web` (JSF), `jbs-rest`, `jbs-excel`, `jbs-jasper`, cada uno con su dependencia pesada y ciclo de actualización propio (la subida puntual de Jasper a 7.0.7 por CVE ya se hizo sin tocar a nadie más — exactamente el beneficio buscado). La partición fue más limpia que lo propuesto: sin dependencias de transición entre los módulos nuevos y `web`.
4. ~~Opcional — fusionar `jbs-core` (12 clases) dentro de `jbs-commons`~~ — **🔵 Descartado por decisión (2026-07-12).** Se mantienen ambos módulos separados. El beneficio de fusionarlos era marginal (un artefacto menos) y la frontera actual es nítida: `jbs-commons` son utilerías puras sin estado de framework (`Strings`, `Fn`, crypto, io), `jbs-core` es implementación de infraestructura (error, xml, config, log, resources) que depende de `jbs-interfaces`. Fusionarlos mezclaría esas dos naturalezas y arrastraría la dependencia a `interfaces` dentro de `commons`.
5. ~~Completar la centralización con un `jbs-bom` publicado~~ — **✅ Hecho (2026-07-11)**: `jbs-bom` es parent de `jbs-parent` y está importado (`scope=import`) en Oym-frame, Maker y TestProject; ningún consumidor fija ya versiones del stack a mano.
6. ~~Hibernate y wildfly-ejb-client a scope provided~~ — **✅ Hecho y superado** (scope `test`; ver §0).
7. **Convertir los overrides por classpath restantes en puntos de extensión.** De las tres familias originales en Maker, **dos ya se resolvieron** (2026-07-12): (a) `org.javabeanstack.log` — `LogMngrData`/`LogMngrSecurity` eran genéricas y se **promovieron a `jbs-core`** con el mismo FQN (ruta A: sin editar `ejb-jar.xml`); (b) `py.com.oym.frame.security.AppObjectAuthSrv` se **repaquetó a `net.makerapp.security`** en Maker (ruta B: se actualizó el `<ejb-class>` en los tres `ejb-jar.xml`) — no podía bajar a Oym-frame porque depende de `IAppUtilSrv` (Maker-services-api), `Empresaparam.segdefault` y las entidades concretas `AppObjectAuth`/`AppUser` (Maker-model), todo por encima de Oym-frame. **Queda uno**: dialectos Hibernate (`org.hibernate.dialect.Mk*`, referenciados por string en `persistence.xml` — repaquetables a `net.makerapp.dialect.*` actualizando ese string). El patrón a seguir es el que ya funciona para `IDBManager`: implementación elegible por `ejb-jar.xml` (o CDI `@Alternative`/`@Specializes`, o configuración por propiedad para los dialectos). Regla propuesta: **ningún consumidor debería necesitar redeclarar un paquete de *implementación* del framework** (implementar una interfaz `IApp*` sí es el mecanismo previsto, no un override).
8. 🟡 **Subir cobertura — parcialmente hecho (2026-07-11)**: `jbs-core` pasó de 0 a 51 tests y todos los jars declaran `Automatic-Module-Name`. Falta la capa web: `jbs-web` tiene 1 test y `rest`/`jasper` ninguno — siguen siendo la red de seguridad pendiente para la validación de Faces 4.1/PrimeFaces 15.

### 2.3 Cambios incompatibles — ahora requieren una ventana 3.0

La ventana 2.0 (Jakarta) se usó sin ejecutar estas rupturas, de modo que hoy exigen una nueva versión mayor o una decisión explícita de no hacerlas nunca:

- **Eliminar los split packages**: mover implementaciones a subpaquetes propios (`org.javabeanstack.data.impl`, etc.) o renombrar paquetes de contratos. Sigue siendo un `sed` masivo en consumidores.
- Rediseño de firmas con tipos POI (punto 1, alternativa ambiciosa).
- Limpieza de métodos `throws Exception` genéricos en la API pública (hoy es convención documentada).

### 2.4 Decisión pendiente: split packages, ¿3.0 o nunca?

Vale dejarlo planteado como decisión y no como tarea: si no hay en el horizonte otra ruptura mayor que obligue a los consumidores a tocar todos los imports, el costo de una 3.0 "solo por higiene de paquetes" probablemente no se justifique. La alternativa honesta es **aceptar los split packages como rasgo permanente del framework**, documentarlo, y compensar sus dos costos reales por otra vía: JPMS se descarta formalmente (reservando nombres con `Automatic-Module-Name`) y el override por classpath se vuelve innecesario completando el punto 7 (log y seguridad ya están; falta solo dialectos). Cualquiera de las dos decisiones es defendible; lo que no conviene es el estado actual (implícito, sin decidir).

---

## 3. Compatibilidad Java / Jakarta — estado post-migración

### 3.1 JPMS

Sin cambios de conclusión: con 13 split packages, JPMS sigue estructuralmente imposible, y aun tras una eventual reorganización no se recomienda (los consumidores corren en WildFly — jboss-modules ignora `module-info` — y el framework vive de reflection JPA/CDI). `Automatic-Module-Name` en cada MANIFEST era el único paso con sentido y **ya se dio** (2026-07-11, vía `<jbs.module.name>` + `maven-jar-plugin` en el padre): los nombres de módulo quedaron reservados.

### 3.2 Jakarta: el primer dominó ya cayó

El plan de migración del análisis original se ejecutó — en orden distinto al propuesto (la fase A estructural no se hizo primero) y con estos resultados:

| Paso propuesto | Resultado |
|---|---|
| 0. Fase A completa antes de migrar | **No se hizo**: se migró con las fronteras actuales. Funcionó igual — el riesgo se absorbió |
| 1. Alinear a javaee-api 8.0 y publicar 1.6.0 | Reemplazado por la estrategia de ramas: `1.5.x` quedó como legacy EE 8 con fixes porteados |
| 2. `javax→jakarta` | ✅ `master` 2.0.0-SNAPSHOT, `jakarta.jakartaee-api:11.0.0`, Java 25 (`maven.compiler.release=25`) |
| 3. Eliminar split packages y firmas POI en la misma ventana | ❌ **No se hizo** — ver §2.4 |
| 4. Hibernate 6 | ✅ Superado: se saltó directo a **Hibernate 7.3.2 / WildFly 40**, y mejor de lo previsto — Hibernate quedó fuera del classpath de compilación (scope test), el main usa JPA 3.2 puro |
| 5. PrimeFaces 12+ (jakarta) | 🟡 PrimeFaces **15.0.6** classifier `jakarta` compila; runtime contra Jakarta Faces 4.1 (EE 11) **sin verificar a fondo** — PF 15 solo confirma soporte Faces 4.0/EE 10. El EAR de Maker ya desplegó en WildFly 40 (2026-07-09), pero falta ciclo funcional JSF |
| 6. JasperReports 7 / POI 5 | ✅ Versiones subidas (**7.0.7** tras el parche de CVE-2026-6009 / 5.4.1) **y aisladas** en `jbs-jasper`/`jbs-excel` (2026-07-11/12): ya no arrastran a `jbs-web` |

Riesgo residual concentrado en el paso 5 (Faces 4.1/PF15 en runtime) y en los pendientes conocidos de `web`: los `TODO` de `LazyDataRows` sobre `FilterMeta.getFilterValue()` y `count()` de la API nueva de PrimeFaces 15.

### 3.3 Nivel de lenguaje

El build está en Java 25 (`java.version=25` en el padre; verificado también el toolchain Temurin). La guía original de "aprovechable ya en Java 11" quedó corta: todo el rango 11→25 está disponible (`List.of()`, `String.isBlank()/strip()`, `Files.readString()`, records donde aplique, pattern matching de `instanceof`) — manteniendo la convención de no usar `var`.

---

## 4. Roadmap y esfuerzo estimado (re-priorizado 2026-07-12)

Las fases A1 (dependencyManagement/scopes) y B (Jakarta) del roadmap original ya se ejecutaron, y de la re-priorización de 2026-07-11 se completaron R3, R4 y R5 (partición de `web` + BOM) más la mitad de R2 (tests de `core`, `Automatic-Module-Name`). Lo restante:

| Fase | Trabajo | Esfuerzo | Riesgo | Estado / notas |
|---|---|---|---|---|
| R1 | Validación runtime Faces 4.1/PF15 (ciclo funcional JSF sobre WildFly 40) + resolver TODOs de `LazyDataRows` | 3–5 días | ~~Medio~~ Bajo | ✅ **Cerrado provisional (2026-07-12)**: `LazyDataRows` corregida/documentada y TODOs resueltos (ver [`analisis_lazydatarows_pf15.md`](analisis_lazydatarows_pf15.md)); la reverificación runtime queda para el despliegue de Maker-web (conversión PF6→15) |
| R2 | Tests en `core`/`web` + `Automatic-Module-Name` en todos los poms | 3–5 días | Muy bajo | 🟡 Parcial: `core` 51 tests y `Automatic-Module-Name` hechos (2026-07-11); faltan tests de `web`/`rest`/`jasper` |
| R3 | Extraer `jbs-excel` (saca POI de interfaces, mismos paquetes) | — | — | ✅ Hecho (2026-07-11; desacoplado también de `web`/PrimeFaces el 2026-07-12) |
| R4 | Extraer `jbs-rest` y `jbs-jasper` | — | — | ✅ Hecho (2026-07-11) |
| R5 | `jbs-bom` publicado + adopción en Oym-frame/Maker | — | — | ✅ Hecho (2026-07-11; importado en Oym-frame, Maker y TestProject) |
| ~~R6~~ | ~~Promover `appcatalog` a producción~~ | — | — | 🔵 **Descartado por diseño (2026-07-12)**. El catálogo se extiende por interfaz (`IApp*`), no por promoción de entidades; ver §2.2·2 |
| R7 | Puntos de extensión para log/seguridad/dialectos (patrón `ejb-jar.xml` del DBManager) | 1–2 días | Bajo | 🟡 En curso. **Log y seguridad hechos** (2026-07-12); queda solo dialectos |
| R8 | Decisión split packages: programar 3.0 o declararlos permanentes | — | — | ❌ Pendiente. Ver §2.4; es una decisión, no una tarea |

**Recomendación final (actualizada 2026-07-12)**: con las extracciones compatibles ya ejecutadas (R3–R5) — POI fuera de `interfaces`, `jbs-web` reducido a JSF puro, versiones centralizadas en `jbs-bom`, Hibernate fuera del compile classpath — y con el catálogo confirmado como punto de extensión por interfaz (R6 descartado), la deuda estructural restante se concentra en tres frentes: **cerrar el riesgo runtime de la migración (R1–R2)**, que hoy es lo primero; después el remanente de **R7** (solo los dialectos Hibernate — log y seguridad ya se resolvieron el 2026-07-12); y la decisión pendiente sobre split packages (**R8**). La subida exprés de Jasper a 7.0.7 por CVE-2026-6009 sin tocar ningún otro módulo ya validó en la práctica el beneficio de la partición. La familia DBManager — ahora simplificada a 3 implementaciones tras retirar V21 — y el catálogo por interfaz `IApp*` son los dos modelos de punto de extensión que el framework ya tiene bien resueltos, y log/seguridad ya funcionan así: falta que los dialectos hagan lo mismo, y con eso ningún consumidor volverá a redeclarar un paquete de **implementación** del framework (implementar sus interfaces sí es lo esperado).

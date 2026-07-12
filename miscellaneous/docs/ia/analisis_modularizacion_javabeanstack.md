# Análisis y propuesta de optimización de la estructura modular de JavaBeanStack

> Fecha original: 2026-07-06 (sobre jbs-parent **1.5.11-SNAPSHOT**, Java EE 8)
> **Última actualización: 2026-07-11** — sobre `master` **2.0.0-SNAPSHOT** (Jakarta EE 11, Java 25)
> Documento hermano: análisis de Maker (`Maker-services/src/main/resources/docs/IA/logs/analisis_modularizacion.md`).
> Copia sincronizada de este documento en Maker: `Maker-services/src/main/resources/docs/IA/logs/analisis_modularizacion_javabeanstack.md`.
> Alcance: solo sugerencias — ningún cambio de código acompaña este documento.

---

## 0. Estado de ejecución de la propuesta original (corte 2026-07-11)

Entre el análisis original y hoy ocurrió el evento mayor del roadmap: la **migración a Jakarta EE 11 se ejecutó completa y antes que la fase A** (commit `3047d64` y siguientes; `master` = 2.0.0-SNAPSHOT, rama `1.5.x` como legacy `javax`). Estado punto por punto de la propuesta de §2.2:

| # §2.2 | Propuesta | Estado | Detalle |
|---|---|---|---|
| 1 | Sacar POI de `jbs-interfaces` (nuevo `jbs-excel`) | ✅ **Ejecutado (2026-07-11)** | `IExcelImportSrv`/`IExcelRowProcessor` + `ExcelImportSrv`/`ExcelRowProcessor`/`ExcelRowImportProcessor`/`ExcelUtil`/`ExcelUploadCtrl` movidos a `jbs-excel` (mismo paquete `org.javabeanstack.web.util`/`web.uploads`). `interfaces` ya no depende de `poi-ooxml` |
| 2 | Promover `appcatalog` de test a producción | ❌ Pendiente | Sigue en `business/src/test/java/org/javabeanstack/model/appcatalog`; Maker mantiene sus copias/overrides (ver `overrides_framework.md` en Maker). **Fuera del alcance de esta iteración** |
| 3 | Partir `jbs-web` (`jbs-rest`, `jbs-excel`, `jbs-jasper`) | ✅ **Ejecutado (2026-07-11)** | `jbs-web` quedó solo JSF/PrimeFaces (17 clases). Se crearon `jbs-rest` (JAX-RS, depende solo de interfaces+commons), `jbs-excel` (POI) y `jbs-jasper` (Jasper+Groovy). `excel` depende **sobre** `web` (usa `FacesContextUtil`) — dirección inversa a la supuesta en la propuesta, sin dep transitoria de vuelta en `web` (sería ciclo). `jasper` se desacopló de `web` **y de PrimeFaces** (usa `FacesContext` directo; `getReportPdf` devuelve `byte[]`; depende solo de `jbs-core`). Guava eliminada del framework |
| 4 | (Opcional) fusionar `jbs-core` en `jbs-commons` | ❌ No ejecutado | Sin urgencia; sigue siendo opcional |
| 5 | `dependencyManagement` en el padre + `jbs-bom` | ✅ **Ejecutado (2026-07-11)** | `jbs-bom` publicado como **parent de `jbs-parent`** (herencia por `relativePath`, evita el huevo-gallina de import en Maven 3). Fuente única de versiones (jbs-*, jakartaee, hibernate, primefaces, Jasper, POI×3, log4j, junit, wildfly). Importado (`scope=import`) en Oym-frame, Maker y TestProject |
| 6 | Hibernate y wildfly-ejb-client a scope `provided` | ✅ **Superado** | Ambos quedaron en scope **`test`** en `business`: el código main compila contra JPA puro (Persistence 3.2); el provider de Hibernate se referencia solo por string (`Class.forName`) en los DBManager dinámicos. Mejor que lo propuesto |
| 7 | Overrides por classpath → puntos de extensión | 🟡 Parcial | La selección de `IDBManager` (clásico/V20/V21/V30) vía `ejb-jar.xml` del consumidor quedó consolidada y **documentada** como punto de extensión (ver README de `miscellaneous/docs/ia`). Siguen los overrides por classpath en Maker: `appcatalog`, dialectos Hibernate, `org.javabeanstack.log`, `py.com.oym.frame.security`. **Fuera del alcance de esta iteración** |
| 8 | Tests en core/web + `Automatic-Module-Name` | ✅ **Ejecutado (2026-07-11)** | `core` pasó de 0 a 51 tests (error/xml); `web` sumó `LocalDateTimeConverterTest`; los 4 tests Excel se movieron a `excel` (58 tests). Todos los jars declaran `Automatic-Module-Name` vía `<jbs.module.name>` + `maven-jar-plugin` en el padre |
| §2.3 | Eliminar split packages en la ventana de ruptura 2.0 | ❌ **Ventana perdida** | La 2.0 (Jakarta) salió **sin** eliminar los 13 split packages ni rediseñar las firmas POI. La partición de `web` agregó un split más (`web.util` en 3 jars). Ver §2.4 |
| §3.2 | Plan de migración Jakarta | ✅ Ejecutado (con salvedades) | Ver §3 actualizado |

**Lectura general (actualizada 2026-07-11)**: se ejecutaron los cuatro puntos compatibles de la propuesta (1, 3, 5, 8) dentro de `2.0.0-SNAPSHOT`, sin romper firmas ni paquetes (salvo el reemplazo interno de Guava). Quedan pendientes, por decisión de alcance, los puntos que tocan a Maker o exigen ventana de ruptura: `appcatalog` a producción (2), puntos de extensión para log/seguridad/dialectos (7) y la eliminación de split packages (2.3/2.4). **Nota de compatibilidad**: como `jbs-web` ya no arrastra POI, Jasper, JAX-RS ni Guava, los consumidores que los usen deben declarar `jbs-excel`/`jbs-jasper`/`jbs-rest`/`guava` explícitamente — ya aplicado en Oym-frame, Maker y TestProject.

---

## 1. Diagnóstico de la estructura actual

### 1.1 Métricas relevadas (actualizadas a `master` 2.0.0-SNAPSHOT)

| Módulo | Clases main | Tests | Contenido | Dependencias externas destacadas |
|---|---|---|---|---|
| jbs-interfaces | 82 | 0 | Contratos (data, model, security, xml, log, annotation…) | **jakartaee-api 11 (provided)**, **poi-ooxml 5.4.1** |
| jbs-commons | 11 | 7 | Utilidades (Strings, Fn, crypto, io) | commons-lang3/io/validator, number2words |
| jbs-core | 12 | 0 | error, xml, config, log, resources (impl) | interfaces + commons |
| jbs-business | 48 | 44 | DataRow, AbstractDAO, DataLink, DataService, seguridad, **familia DBManager ×4** | hibernate-core 7.3.2 y wildfly-ejb-client-bom 40 **solo scope test** |
| jbs-web | 33 | 4 | JSF controllers, REST, Excel, Jasper | **primefaces 15.0.6 (classifier jakarta)**, **jasperreports 7.0.4 + groovy**, poi 5.4.1, guava 32 |
| jbs-aws | 2 | 1 | S3 | awssdk s3 2.29.0 |

Total: **188 clases** de producción (185 en el análisis original; las 3 nuevas son `DBManagerV20/V21/V30`). Datos estructurales adicionales:

- **13 split packages entre módulos** — la lista es la misma del análisis original y sigue intacta: `config`, `data`, `data.services`, `datactrl`, `error`, `log`, `resources`, `security`, `security.model`, `web.model`, `web.util`, `xml` (interfaces vs. core/business/web) y `util` (commons/business). Es el patrón deliberado "contrato e implementación en el mismo paquete, distinto jar".
- **Las entidades del catálogo (`org.javabeanstack.model.appcatalog`) siguen en `business/src/test`**, no en producción. Maker mantiene su propia copia de producción en Maker-model.
- Los EJB casi no usan anotaciones (solo `Sessions` lleva `@Stateless`): los `DBManager` y demás se declaran como EJBs en el `ejb-jar.xml` **del consumidor**. Lo que en 2026-07-06 se señaló como patrón implícito hoy es un **mecanismo de conmutación documentado**: la app elige la implementación de `IDBManager` en su `ejb-jar.xml` (ver §1.3).
- La cadena de dependencias declarada (interfaces → commons → core → business → web; aws aparte) se respeta sin ciclos.
- El padre **ya tiene `dependencyManagement`** (resuelto desde el análisis original): una sola declaración por dependencia pesada; los módulos heredan sin `<version>`.
- Estrategia de ramas nueva: **`master` (2.0, `jakarta.*`) y `1.5.x` (legacy, `javax.*`)** que recibe fixes de lógica porteados a mano hacia master. Esto agrega una restricción que el análisis original no tenía: **todo refactor estructural en master encarece el porteo de fixes desde 1.5.x** (los diffs dejan de aplicar limpio).

### 1.2 Evaluación

**Lo que está bien (y lo que mejoró):**

- El tamaño sigue sano: 188 clases bien repartidas, sin "módulo Dios". La separación contratos/implementación como módulos Maven distintos ya existe.
- Dirección de dependencias limpia, `aws` correctamente aislado.
- `business` sigue siendo el módulo mejor cubierto (44 tests de integración, migrados a JUnit 5 y adaptados al naming remoto de WildFly 40).
- **Mejora mayor desde el análisis original**: `business` ya no acopla al vendor JPA — hibernate-core pasó de dependencia compile (5.4.24) a **scope test** (7.3.2), y el código main usa solo la API `jakarta.persistence` (Persistence 3.2, incl. `PersistenceConfiguration` en los DBManager dinámicos). El punto 6 de la propuesta quedó superado.
- Versiones al día: Jakarta EE 11, Java 25, PrimeFaces 15, JasperReports 7, POI 5.4.1, JUnit 5, log4j 2.25.4.

**Los problemas que siguen abiertos:**

1. **`jbs-interfaces` sigue contaminado con Apache POI**: `IExcelImportSrv` e `IExcelRowProcessor` exponen `Workbook` en sus firmas, con lo cual el módulo de contratos puros — que todo el ecosistema arrastra — depende de poi-ooxml. El costo se propaga a todo consumidor de cualquier interfaz.
2. **`jbs-web` sigue siendo un cajón mixto con las dependencias más pesadas del stack**: JSF/PrimeFaces + REST/JAX-RS + import Excel (POI) + reportes (JasperReports + Groovy) + Guava en un solo artefacto, ahora en versiones mayores nuevas pero con la misma soldadura.
3. **`appcatalog` en test sources**: sin cambios; sigue siendo la causa raíz de una familia de overrides en Maker.
4. **Los 13 split packages sobrevivieron a la ventana 2.0**: la migración Jakarta era la ruptura donde eliminarlos "gratis" (los consumidores ya tocaban todos los imports por `javax→jakarta`) y no se aprovechó. Siguen (a) bloqueando JPMS para siempre, (b) habilitando el override por orden de classpath que Maker explota, y (c) desdibujando la frontera API/impl. La próxima ventana natural es una eventual 3.0 — o aceptarlos como decisión permanente (ver §2.4).
5. **Cobertura de tests desigual**: 0 tests en `core`, 4 en `web`. La migración Jakarta se validó con compilación completa + tests de `commons` + despliegue real del EAR de Maker en WildFly 40 (2026-07-09), pero la validación runtime de la capa JSF (Faces 4.1 con PrimeFaces 15) sigue pendiente de un ciclo funcional serio.
6. **(Nuevo) El costo de la doble rama**: mientras `1.5.x` reciba fixes, cada movimiento de clases/módulos en master duplica el costo de mantenimiento. Los refactors estructurales de §2.2 convienen agruparse y ejecutarse cuando el flujo de fixes hacia 1.5.x se apague, o asumiendo el porteo manual.

### 1.3 Novedad estructural: la familia `IDBManager`

Desde el análisis original, `business` incorporó tres implementaciones alternativas del administrador de unidades de persistencia, conmutables por el `ejb-jar.xml` del consumidor (documentación canónica en este mismo directorio, ver `README.md`):

| Implementación | Estrategia |
|---|---|
| `DBManager` (clásico) | PUs declaradas en `persistence.xml`, bindings JNDI `java:app/em/PUn` |
| `DBManagerV20` | EMF dinámicos por system properties (`jbs.persistence.dynamic.*`) — experimental |
| `DBManagerV21` | EMF dinámicos por plantilla `DINAMIC_PU` comentada en `persistence.xml` |
| `DBManagerV30` | PUs definidas en `META-INF/dynamic_persistence.xml`, spec completa por unidad, plantilla `DEFAULT` con `{n}` para alta de empresas sin redeploy |

Esto es relevante para la propuesta por dos motivos: (a) valida en la práctica el mecanismo "el consumidor elige la implementación vía `ejb-jar.xml`" como **punto de extensión explícito y documentado** — exactamente el patrón que el punto 7 de §2.2 pide generalizar a log/seguridad/dialectos; y (b) los DBManager dinámicos demuestran que se puede depender de Hibernate **sin compilarlo** (provider referenciado por string, API JPA pura), el mismo criterio a aplicar si `web` u otros módulos necesitan aislar vendors.

---

## 2. Propuesta de optimización modular (vigente)

### 2.1 Principio rector

Sin cambios: aquí **no hay nada grande que partir** — la granularidad ya es correcta. El trabajo es **sanear fronteras** (qué vive en qué módulo y qué arrastra cada uno) y **convertir el patrón de override por classpath en puntos de extensión explícitos**, generalizando el modelo que la familia DBManager ya estableció. Con la ventana de ruptura Jakarta ya consumida, todo cambio debe medirse además contra el costo de porteo de fixes desde `1.5.x`.

### 2.2 Cambios de estructura sugeridos (compatibles, sin romper consumidores)

La estructura objetivo no cambia respecto del análisis original:

```
jbs-parent
├── jbs-interfaces      ← igual, PERO sin POI (ver 1º punto)
├── jbs-commons         ← igual (opcional: absorber jbs-core, ver 4º punto)
├── jbs-core            ← igual
├── jbs-business        ← igual + entidades appcatalog promovidas desde test (2º punto)
├── jbs-web             ← solo JSF/PrimeFaces + filtros + util web liviano
├── jbs-rest        ← NUEVO: web/rest/resources, filters, exceptions, model
├── jbs-excel           ← NUEVO: import/export Excel (POI) + IExcelImportSrv/IExcelRowProcessor
├── jbs-jasper   ← NUEVO: integración JasperReports (+ groovy)
├── jbs-aws             ← igual
└── jbs-bom             ← NUEVO: BOM publicado para consumidores
```

1. **Sacar POI de `jbs-interfaces`.** Mover `IExcelImportSrv`/`IExcelRowProcessor` al nuevo `jbs-excel` (mantienen paquete `org.javabeanstack.web.util`, así los imports de Maker no cambian — solo cambia la dependencia Maven). La alternativa ambiciosa (rediseñar firmas para no exponer tipos POI) quedó sin ejecutar en la 2.0; si se hace ahora es ruptura de firma y debe esperar una 3.0 o hacerse con métodos nuevos + deprecación.
2. **Promover `appcatalog` de test a producción** (en `jbs-business` o un `jbs-model-appcatalog`). Las apps que hoy copian las entidades pasan a extenderlas o usarlas directo; se elimina una familia entera de overrides en Maker. Nota post-modularización de Maker: las copias hoy viven en **Maker-model** (la Fase 1 de Maker ya separó el modelo), lo que simplifica la adopción — es un solo módulo consumidor a adaptar.
3. **Partir `jbs-web` por peso de dependencias** (`jbs-web` JSF, `jbs-rest`, `jbs-excel`, `jbs-jasper`). El criterio sigue siendo de ciclo de vida: PrimeFaces 15, Jasper 7+Groovy y POI 5 seguirán actualizándose a ritmos distintos; hoy están soldadas. La medición actual del acople interno: solo 1 clase de `web` usa Jasper, 6 usan POI y 1 usa Guava — la partición es limpia. `jbs-web` puede conservar dependencias a los nuevos módulos durante una versión (transición sin ruptura) y soltarlas después.
4. **Opcional — fusionar `jbs-core` (12 clases) dentro de `jbs-commons`**: beneficio modesto; solo si se quiere reducir la lista de artefactos.
5. **Completar la centralización con un `jbs-bom` publicado**: el `dependencyManagement` del padre ya existe (hecho); falta el artefacto BOM que Oym-frame y Maker puedan importar para dejar de perseguir versiones a mano (hoy Maker fija `hibernate.version`, `primefaces.version`, etc. por su cuenta y debe mantenerlas alineadas al framework manualmente).
6. ~~Hibernate y wildfly-ejb-client a scope provided~~ — **Hecho y superado** (scope `test`; ver §0).
7. **Convertir los overrides por classpath restantes en puntos de extensión.** Quedan tres familias en Maker: dialectos Hibernate (`org.hibernate.dialect.Mk*`), `org.javabeanstack.log` y `py.com.oym.frame.security` (más `appcatalog`, que resuelve el punto 2). El patrón a seguir es el que ya funciona para `IDBManager`: implementación elegible por `ejb-jar.xml` (o CDI `@Alternative`/`@Specializes`, o configuración por propiedad para los dialectos). Regla propuesta: **ningún consumidor debería necesitar redeclarar un paquete del framework**.
8. **Subir cobertura donde hay 0**: `jbs-core` (error/xml/config es código estable, fácil de testear) y los converters/util de `jbs-web`. Urgencia renovada: son la red de seguridad de la validación pendiente de Faces 4.1/PrimeFaces 15 y de cualquier partición de `web`. Agregar además `Automatic-Module-Name` al MANIFEST de cada jar (una línea por pom, cero riesgo) para reservar los nombres de módulo JPMS.

### 2.3 Cambios incompatibles — ahora requieren una ventana 3.0

La ventana 2.0 (Jakarta) se usó sin ejecutar estas rupturas, de modo que hoy exigen una nueva versión mayor o una decisión explícita de no hacerlas nunca:

- **Eliminar los split packages**: mover implementaciones a subpaquetes propios (`org.javabeanstack.data.impl`, etc.) o renombrar paquetes de contratos. Sigue siendo un `sed` masivo en consumidores.
- Rediseño de firmas con tipos POI (punto 1, alternativa ambiciosa).
- Limpieza de métodos `throws Exception` genéricos en la API pública (hoy es convención documentada).

### 2.4 Decisión pendiente: split packages, ¿3.0 o nunca?

Vale dejarlo planteado como decisión y no como tarea: si no hay en el horizonte otra ruptura mayor que obligue a los consumidores a tocar todos los imports, el costo de una 3.0 "solo por higiene de paquetes" probablemente no se justifique. La alternativa honesta es **aceptar los split packages como rasgo permanente del framework**, documentarlo, y compensar sus dos costos reales por otra vía: JPMS se descarta formalmente (reservando nombres con `Automatic-Module-Name`) y el override por classpath se vuelve innecesario completando los puntos 2 y 7. Cualquiera de las dos decisiones es defendible; lo que no conviene es el estado actual (implícito, sin decidir).

---

## 3. Compatibilidad Java / Jakarta — estado post-migración

### 3.1 JPMS

Sin cambios de conclusión: con 13 split packages, JPMS sigue estructuralmente imposible, y aun tras una eventual reorganización no se recomienda (los consumidores corren en WildFly — jboss-modules ignora `module-info` — y el framework vive de reflection JPA/CDI). `Automatic-Module-Name` en cada MANIFEST sigue siendo el único paso con sentido y sigue sin darse.

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
| 6. JasperReports 7 / POI 5 | 🟡 Versiones subidas (7.0.4 / 5.4.1) pero **sin aislar** en `jbs-jasper`/`jbs-excel`: siguen arrastrando a todo `jbs-web` |

Riesgo residual concentrado en el paso 5 (Faces 4.1/PF15 en runtime) y en los pendientes conocidos de `web`: los `TODO` de `LazyDataRows` sobre `FilterMeta.getFilterValue()` y `count()` de la API nueva de PrimeFaces 15.

### 3.3 Nivel de lenguaje

El build está en Java 25 (`java.version=25` en el padre; verificado también el toolchain Temurin). La guía original de "aprovechable ya en Java 11" quedó corta: todo el rango 11→25 está disponible (`List.of()`, `String.isBlank()/strip()`, `Files.readString()`, records donde aplique, pattern matching de `instanceof`) — manteniendo la convención de no usar `var`.

---

## 4. Roadmap y esfuerzo estimado (re-priorizado 2026-07-11)

Las fases A1 (dependencyManagement/scopes) y B (Jakarta) del roadmap original ya se ejecutaron. Lo restante, reordenado por ratio beneficio/costo actual:

| Fase | Trabajo | Esfuerzo | Riesgo | Notas |
|---|---|---|---|---|
| R1 | Validación runtime Faces 4.1/PF15 (ciclo funcional JSF sobre WildFly 40) + resolver TODOs de `LazyDataRows` | 3–5 días | Medio | Es el riesgo abierto de la 2.0; bloquea confiar en `jbs-web` |
| R2 | Tests en `core`/`web` + `Automatic-Module-Name` en todos los poms | 3–5 días | Muy bajo | Red de seguridad previa a cualquier partición de `web` |
| R3 | Extraer `jbs-excel` (saca POI de interfaces, mismos paquetes) | 1–2 días | Bajo | Sigue siendo el mejor ratio costo/beneficio estructural |
| R4 | Extraer `jbs-rest` y `jbs-jasper` | 2–3 días | Bajo | Medido: 1 clase usa Jasper, 6 POI — partición limpia |
| R5 | `jbs-bom` publicado + adopción en Oym-frame/Maker | 1–2 días | Muy bajo | El dependencyManagement del padre ya existe; falta solo el artefacto |
| R6 | Promover `appcatalog` a producción + adaptar Maker-model | 3–5 días | Medio | Toca seguridad/catálogo en Maker; más simple ahora que Maker-model existe como módulo separado |
| R7 | Puntos de extensión para log/seguridad/dialectos (patrón `ejb-jar.xml` del DBManager) | 1–2 semanas | Medio | Elimina los overrides por classpath restantes |
| R8 | Decisión split packages: programar 3.0 o declararlos permanentes | — | — | Ver §2.4; es una decisión, no una tarea |

**Recomendación final (actualizada)**: la 2.0 resolvió el habilitador grande (Jakarta EE 11 / Java 25) y dos deudas de acople (versiones centralizadas, Hibernate fuera del compile classpath), pero la estructura modular quedó exactamente donde estaba en el análisis original: POI sigue en `interfaces`, `jbs-web` sigue soldado, `appcatalog` sigue en test y los split packages sobrevivieron a la única ventana de ruptura disponible. El orden que hoy conviene es: primero cerrar el riesgo runtime de la migración (R1–R2), después las extracciones compatibles (R3–R5) que no rompen a ningún consumidor, y recién entonces las que tocan a Maker (R6–R7). La familia DBManager demostró que el framework ya sabe hacer puntos de extensión limpios — el objetivo es que log, seguridad, dialectos y catálogo funcionen igual, y que ningún consumidor vuelva a redeclarar un paquete del framework.

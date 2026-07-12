# Anexo — Detalle de los puntos pendientes de la modularización

> Fecha: 2026-07-12 · Complementa a [`analisis_modularizacion_javabeanstack.md`](analisis_modularizacion_javabeanstack.md) (§4, roadmap).
> Objetivo: explicar en concreto **qué** queda pendiente, **por qué** importa y **qué** se sugiere, para poder decidir con la información completa.
> Alcance: solo análisis y sugerencias. Ningún cambio de código acompaña este documento.

## Mapa rápido

De la propuesta original quedaron **ejecutados** (2.0.0-SNAPSHOT): la partición de `jbs-web` en `jbs-excel`/`jbs-rest`/`jbs-jasper` (R3–R4), el `jbs-bom` publicado (R5), Hibernate fuera del compile classpath, y los tests de `core` (51) + `Automatic-Module-Name` en todos los jars (mitad de R2).

Quedaron **descartados por decisión**: promover `appcatalog` a producción (el catálogo se extiende por interfaz `IApp*`) y fusionar `jbs-core` en `jbs-commons` (se mantienen separados).

**Pendientes reales** — los cuatro que detalla este anexo:

| # | Título | Naturaleza | Riesgo | Estado |
|---|---|---|---|---|
| R1 | Validación runtime Faces 4.1 / PrimeFaces 15 + TODOs de `LazyDataRows` | Trabajo | **Medio** | Abierto — el único riesgo real de la 2.0 |
| R2 | Cobertura de tests en `jbs-web` / `jbs-rest` / `jbs-jasper` | Trabajo | Muy bajo | Parcial (falta solo la capa web) |
| R7 | Convertir los overrides por classpath restantes en puntos de extensión | Trabajo | Bajo | En curso — **log y seguridad hechos (2026-07-12)**; queda solo dialectos |
| R8 | Split packages: ¿ventana 3.0 o declararlos permanentes? | **Decisión** | — | Sin decidir |

Los dos primeros son deuda técnica interna del framework; los dos últimos involucran a los consumidores (Maker) y conviene tratarlos como decisiones, no como tareas a agendar sin más.

---

## R1 — Validación runtime de Faces 4.1 / PrimeFaces 15 (riesgo medio)

**Qué es.** La migración a Jakarta EE 11 subió la capa JSF a **PrimeFaces 15.0.6** (classifier `jakarta`) corriendo sobre **Jakarta Faces 4.1**. Eso *compila* y el EAR de Maker *desplegó* en WildFly 40 (2026-07-09), pero un despliegue exitoso no ejercita las pantallas: la lógica de datatables lazy, filtros, ordenamiento y conversión de valores solo se prueba abriendo formularios reales y filtrando/paginando.

**Por qué importa.** PrimeFaces 15 cambió la API de filtrado de las tablas lazy: el mapa de filtros pasó de `Map<String, Object>` (campo → valor) a `Map<String, FilterMeta>` (campo → objeto de filtro, del que hay que **extraer** el valor con `getFilterValue()`). `LazyDataRows` es la clase del framework que traduce esos filtros a parámetros de consulta, y la adaptación a la API nueva quedó **incompleta**. Hay dos `TODO` explícitos que lo señalan (`web/.../controller/LazyDataRows.java:256` y `:421`).

**Defecto sospechado (a confirmar en runtime).** En el método `getParams(Map<String, FilterMeta> filters)` la migración PF15 fue parcial:

- La rama de tipo `String` sí extrae el valor con la API nueva:
  `Object value = ((FilterMeta) e.getValue()).getFilterValue();` (línea 257, introducida el 2025-08-03 en el commit "primefaces 15").
- Las ramas numéricas y de fecha (`Long`, `Integer`, `Short`, `BigDecimal`, `LocalDateTime`, `Date`, líneas ~278–289) siguen con el patrón **de 2018**: `Long.valueOf((String) e.getValue())`, es decir castean **la entrada `FilterMeta` completa a `String`** en vez de su valor extraído.

Como ahora `e.getValue()` es un `FilterMeta` y no el valor, filtrar por una columna numérica o de fecha en una tabla lazy debería fallar (previsiblemente `ClassCastException`). No lo ejecuté; hay que **confirmarlo abriendo una pantalla y filtrando por una columna numérica**. El git-blame respalda el diagnóstico: la inconsistencia nace de que el cambio PF15 tocó solo la rama String.

**Qué se sugiere.**
1. Ejecutar un ciclo funcional JSF sobre WildFly 40: abrir varios ABM (`*Ctrl`) y vistas (`*ViewCtrl`) de Maker, y ejercitar paginación, ordenamiento y **filtro por columnas de cada tipo** (texto, número, fecha).
2. Resolver los dos `TODO` de `LazyDataRows`: unificar todas las ramas de `getParams` para que extraigan el valor con `getFilterValue()` antes de convertir, y verificar el orden de ejecución `load`/`count` que marca el segundo TODO (`:421`).
3. Cerrar los `TODO` menores relacionados de la capa web (`AbstractUserEnvironment.java:60`, `FileHandle.java:100`).

**Esfuerzo y riesgo.** 3–5 días. Riesgo **medio**: es el único frente de la 2.0 con impacto funcional directo; hasta cerrarlo no conviene confiar en `jbs-web` en producción. Es lo primero del roadmap.

---

## R2 — Cobertura de tests en la capa web (riesgo muy bajo)

**Qué es.** La 2.0 ya subió cobertura donde había cero: `jbs-core` pasó de 0 a **51 tests** (error/xml) y todos los jars declaran `Automatic-Module-Name`. Lo que **resta** es solo la capa web:

| Módulo | Tests actuales |
|---|---|
| jbs-web | 1 (`LocalDateTimeConverterTest`) |
| jbs-rest | 0 |
| jbs-jasper | 0 |

**Por qué importa.** Son la red de seguridad de R1: sin tests sobre los converters, `LazyDataRows` y los recursos REST, cada ajuste de la capa JSF/JAX-RS se valida a mano. Tener aunque sea tests unitarios de la traducción de filtros y de los converters permitiría detectar regresiones como la de `getParams` sin desplegar.

**Qué se sugiere.** Priorizar tests unitarios *offline* (sin servidor) de lo determinístico y frágil: `getParams`/`getFilterExpression` de `LazyDataRows` (con `FilterMeta` mockeado, cubriendo cada tipo de dato), los converters de `jbs-web`, y el armado de respuestas/errores de `jbs-rest` (`MessageResponse`, `ErrorMessage`, `CORSFilter`). `jbs-jasper` es una sola clase (`JasperReportUtil`) y aporta poco; puede quedar al final.

**Esfuerzo y riesgo.** 3–5 días. Riesgo **muy bajo** (agregar tests no cambia producción). Conviene hacerlo *junto con* R1: los tests capturan lo que la validación runtime encuentre.

---

## R7 — Convertir los overrides por classpath restantes en puntos de extensión (riesgo bajo–medio)

> **Avance 2026-07-12: log y seguridad de objetos ya se resolvieron** (ver "Casos ya ejecutados" al final de esta sección). Queda solo la familia de dialectos.

**Qué es.** Maker todavía provee familias de clases cuyo **paquete pertenece a un framework** (JavaBeanStack, Oym-frame o Hibernate) pero cuyo fuente vive en el repo de Maker. En el classpath plano de WildFly, la copia local "pisa" a la del framework por orden de carga — un mecanismo frágil e invisible (un cambio de versión del framework puede romperlo en silencio). Tras la decisión sobre `appcatalog` (que **no** es un override, sino implementación de interfaz por diseño), el inventario:

| Familia | Clases | Vive en | Cómo se selecciona hoy | Estado |
|---|---|---|---|---|
| Managers de log | `org.javabeanstack.log.LogMngrData`, `LogMngrSecurity` | ~~Maker-services~~ → **jbs-core** | `<ejb-class>` en `ejb-jar.xml` | ✅ **Resuelto (2026-07-12)** — promovidas al framework (ruta A) |
| Seguridad de objetos | `AppObjectAuthSrv` | ~~`py.com.oym.frame.security`~~ → **`net.makerapp.security`** (Maker-services) | `<ejb-class>` en `ejb-jar.xml` | ✅ **Resuelto (2026-07-12)** — repaquetada en Maker (ruta B) |
| Dialectos Hibernate | `org.hibernate.dialect.MkSqlServer2008`, `MkPostgreSql95` | Maker-model | string `hibernate.dialect` en `persistence.xml` (~10 PUs) | ❌ Pendiente |

**El hallazgo clave.** Estas tres familias **ya se seleccionan por configuración**, no por el nombre del paquete:

- Los EJB de log y seguridad se declaran por FQN en `<ejb-class>` del `ejb-jar.xml` del consumidor — exactamente el mismo mecanismo con el que se elige la implementación de `IDBManager` (`DBManager` clásico vs `DBManagerV30`, comentados uno al lado del otro). Ese punto de extensión **ya funciona y está documentado**.
- Los dialectos se referencian por **string** en `persistence.xml`; Hibernate carga la clase por FQN, así que puede estar en cualquier paquete.

Y ninguna de las tres **necesita** técnicamente el paquete del framework:

- `LogMngrData`/`LogMngrSecurity` `extend org.javabeanstack.log.LogManager` (del framework, en `jbs-core`). Los hooks que necesitan están expuestos como `protected` (`getLogTypePath()`) y los campos del padre son `private` — es decir, la subclase trabaja a través de API pública/protegida, no de acceso de paquete. Como además eran genéricas y estaban duplicadas en varios consumidores, el mejor destino no era un paquete de Maker sino **el propio framework** (ruta A); ya se ejecutó — ver "Caso ya ejecutado".
- `AppObjectAuthSrv` vivía en el paquete de Oym-frame `py.com.oym.frame.security` pero importa `IAppUtilSrv` (Maker-services-api) y usa entidades de Maker-model; nada la ata a ese paquete de Oym-frame, así que se repaquetó a `net.makerapp.security` (ruta B, ya ejecutada — ver abajo).
- Los dialectos `Mk*` `extend SQLServerDialect`/`PostgreSQLDialect` y no tienen imports que exijan el paquete `org.hibernate.dialect`.

**Qué se sugiere.** Hay **dos rutas**, según si la clase es genérica o específica de la app:

- **Ruta A — promover al framework** (cuando la clase no tiene nada propio de la app y se duplica en varios consumidores). Se mueve al jar del framework **con el mismo FQN**; los `ejb-jar.xml`/strings de config **no cambian** (siguen apuntando al mismo nombre, ahora resuelto desde el jar del framework) y basta borrar la copia del consumidor. Es la ruta más limpia: cero cambios de configuración. **Es la que se usó para log** (ver abajo).
- **Ruta B — repaquetar a un paquete propio de Maker** (cuando la clase es un ajuste específico de Maker, atado a tipos que el framework no ve). Se mueve a `net.makerapp.*` y se actualiza la referencia de config. Ya se usó para `AppObjectAuthSrv` (ver abajo). Queda pendiente el mismo tratamiento para los dialectos:
  - `org.hibernate.dialect.Mk*` → `net.makerapp.dialect.*`, actualizando el string `hibernate.dialect` en las ~10 PUs de `persistence.xml`.

En ambas rutas desaparecen los split packages *del lado de Maker*, la extensión queda explícita y ningún consumidor redeclara un paquete de **implementación** del framework. La regla que queda como norma: extender/implementar el framework **desde un paquete propio** (o promover al framework lo que sea genérico) es el mecanismo previsto; poner una clase dentro de un paquete del framework desde el consumidor no lo es.

**Esfuerzo y riesgo.** Con log y seguridad hechos, resta solo la familia de dialectos: 1–2 días, riesgo **bajo** (repaquetar + actualizar el string `hibernate.dialect` en `persistence.xml`). Nota sobre dialectos: si se cambia de versión de Hibernate hay que **reescribirlos** (la API de dialectos cambió entre Hibernate 5 y 6), independientemente del paquete.

**Casos ya ejecutados (2026-07-12).**

- **Log — ruta A (promoción al framework).** `LogMngrData` y `LogMngrSecurity` resultaron ser el caso ideal de promoción: implementaciones **genéricas** (cada una solo fija su categoría —`CATEGORY_DATA`/`CATEGORY_SECURITY`, constantes que ya viven en `IAppLogRecord`— y delega en `super.dbWrite` de `LogManager`), con encabezado de copyright del propio framework y **duplicadas idénticas en Maker y en TestProject**. Se movieron a `jbs-core` (junto a `LogManager`, que ya extendían) con el mismo FQN `org.javabeanstack.log.*`. Resultado: los `<ejb-class>` de todos los `ejb-jar.xml` quedaron intactos —ahora resuelven desde `jbs-core.jar`, igual que `DBManager`/`LogManager`/`Sessions`— y se borraron las cuatro copias (dos en Maker, dos en TestProject). Ambos consumidores recompilan sin cambios.
- **Seguridad de objetos — ruta B (repaquetado en Maker).** `AppObjectAuthSrv` **no** podía promoverse ni bajar a Oym-frame: depende de `IAppUtilSrv` (Maker-services-api, para `getEmpresaParam().getSegdefault()`), de `Empresaparam.segdefault` (sin equivalente en el framework) y de las entidades concretas `AppObjectAuth`/`AppUser` (Maker-model) — todos por encima de Oym-frame en el grafo. Además la copia de Maker (655 líneas) y la de TestProject (186) ya divergieron, así que tampoco era una de-duplicación. Se repaquetó de `py.com.oym.frame.security` a `net.makerapp.security` **dentro de Maker** (`git mv` + cambio de `package`), y se actualizó el `<ejb-class>` en los tres `ejb-jar.xml` (Maker-services, Maker-web, Maker-rest). Ninguna clase Java importaba el FQN concreto (solo la interfaz `IAppObjectAuthSrv`, que no cambió), así que Maker recompila sin más cambios. **Pendiente aparte**: TestProject conserva su propia copia en `py.com.oym.frame.security` (no se tocó en esta iteración).

---

## R8 — Split packages: ¿ventana 3.0 o declararlos permanentes? (decisión, no tarea)

**Qué es.** El framework tiene **13 split packages internos**: paquetes que existen a la vez en `jbs-interfaces` (el contrato) y en `jbs-core`/`jbs-business`/`jbs-web` (la implementación) — `config`, `data`, `data.services`, `datactrl`, `error`, `log`, `resources`, `security`, `security.model`, `web.model`, `web.util`, `xml`, y `util` (commons vs business). Es un patrón **deliberado**: "contrato e implementación en el mismo paquete, distinto jar". La partición de `web` no agregó paquetes nominalmente nuevos pero extendió `web.util` a 3 jars (`web`/`excel`/`jasper`).

> Ojo: estos son distintos de los split packages **del lado de Maker** que ataca R7. R7 elimina los que aporta el consumidor; R8 es sobre los internos del framework, que R7 no toca.

**Por qué importa.** Los split packages tienen tres costos: (a) hacen **imposible JPMS** (`module-info.java` prohíbe que dos módulos exporten el mismo paquete); (b) habilitan el override por orden de classpath —el mismo mecanismo frágil que R7 busca cerrar—; y (c) desdibujan la frontera API/implementación. Eliminarlos exige **mover implementaciones a subpaquetes propios** (`org.javabeanstack.data.impl`, etc.) o renombrar los paquetes de contratos — en ambos casos, los consumidores tienen que tocar todos los imports (`sed` masivo). Eso es una **ruptura mayor**: solo cabe en una versión 3.0. La ventana natural (la migración Jakarta, donde los consumidores ya tocaban todos los imports por `javax→jakarta`) **ya se consumió** sin aprovecharla.

**La decisión.** No es una tarea a agendar, sino una elección entre dos caminos, ambos defendibles:

- **Opción A — programar una 3.0 "de higiene de paquetes".** Se eliminan los split packages, se habilita (formalmente) JPMS y se limpia la frontera API/impl. Costo: ruptura para todos los consumidores, con el agravante de la doble rama (`1.5.x`/`master`) que encarece el porteo. Solo se justifica si hay **otra** ruptura mayor en el horizonte que ya obligue a los consumidores a tocar los imports; hacer una 3.0 *solo* por esto probablemente no rinde.
- **Opción B — aceptar los split packages como rasgo permanente.** Se documenta como decisión de diseño y se **compensan sus costos por otra vía**: JPMS se descarta explícitamente (los consumidores corren en WildFly, donde `jboss-modules` ignora `module-info`; ya se reservaron los nombres con `Automatic-Module-Name`), y el override por classpath se vuelve innecesario al completar R7. Con esas dos compensaciones, los tres costos quedan neutralizados sin ruptura.

**Recomendación.** Salvo que aparezca otra ruptura mayor que arrastre a los consumidores, **la opción B es la más razonable**: el framework vive de reflection (JPA/CDI) y corre en WildFly, donde JPMS no aporta, y R7 ya elimina el uso indebido del classpath. Lo que **no** conviene es el estado actual: split packages sin decidir, ni asumidos ni removidos. La acción concreta de la opción B es una sola frase en el README del framework: "los split packages interfaces↔impl son intencionales y permanentes; JPMS no se soporta".

**Esfuerzo y riesgo.** La decisión en sí no tiene costo. La opción A sería semanas + ruptura de consumidores; la opción B, documentar.

---

## Dos observaciones sueltas (fuera del alcance de este anexo)

Encontradas al revisar el código para este documento; las dejo anotadas para que decidas si abrir tickets aparte:

1. **`getParams` en `LazyDataRows`** (ver R1) contiene un defecto sospechado, no solo un TODO. Vale un ticket de bug propio, además del trabajo de validación de R1.
2. **`miscellaneous/docs/ia/README.md` quedó desactualizado**: dice "Las **cuatro** implementaciones de `IDBManager`" y hoy son **tres** (`DBManager`, `DBManagerV20`, `DBManagerV30`; `DBManagerV21` se eliminó el 2026-07-12). La nota sobre el análisis de modularización en ese README también cita "Actualizado 2026-07-11".

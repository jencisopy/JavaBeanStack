# Análisis y propuesta de optimización de la estructura modular de JavaBeanStack

> Fecha: 2026-07-06 · Proyecto analizado: `../JavaBeanStack` (jbs-parent 1.5.11-SNAPSHOT)
> Documento hermano: `docs/IA/analisis_modularizacion.md` (análisis de Maker)
> Alcance: solo sugerencias — ningún cambio de código acompaña este documento.

---

## 1. Diagnóstico de la estructura actual

### 1.1 Métricas relevadas

| Módulo | Clases main | Tests | Contenido | Dependencias externas destacadas |
|---|---|---|---|---|
| jbs-interfaces | 82 | 0 | Contratos (data, model, security, xml, log, annotation…) | **javaee-api 7.0**, **poi-ooxml 4.1.2** |
| jbs-commons | 11 | 7 | Utilidades (Strings, Fn, crypto, io) | commons-lang3/io/validator, number2words |
| jbs-core | 12 | 0 | error, xml, config, log, resources (impl) | interfaces + commons |
| jbs-business | 45 | 43 | DataRow, AbstractDAO, DataLink, DataService, seguridad, EJBs | **hibernate-core 5.4.24**, wildfly-ejb-client, javax.json |
| jbs-web | 33 | 4 | JSF controllers, REST, Excel, Jasper | **primefaces 6.2**, **jasperreports 6.20 + groovy**, poi 4.1.2, guava 32 |
| jbs-aws | 2 | 1 | S3 | awssdk s3 |

Total: **185 clases** de producción — un framework compacto. Datos estructurales adicionales:

- **13 split packages entre módulos**: `org.javabeanstack.data` vive en interfaces *y* business; `datactrl` en interfaces, business *y* web; ídem `security`, `util` (commons/business), `xml`, `error`, `log`, `config`, `resources` (interfaces/core), `web.util`, `web.model`. Es el patrón deliberado "contrato e implementación en el mismo paquete, distinto jar".
- **Las entidades del catálogo (`org.javabeanstack.model.appcatalog`) están en `business/src/test`**, no en producción. Cada aplicación consumidora (Maker las tiene con 24 clases en Maker-ejb) reimplementa su propia copia de producción.
- Los EJB casi no usan anotaciones (solo `Sessions` lleva `@Stateless`): `DBManager` y demás se declaran como EJBs en el `ejb-jar.xml` **del consumidor** (Maker). El framework entrega clases "pasivas" que la app promueve a EJB.
- La cadena de dependencias declarada (interfaces → commons → core → business → web; aws aparte) se respeta sin ciclos.
- No hay `dependencyManagement` en el padre: cada módulo declara sus versiones (POI está declarado 2 veces, en interfaces y web).

### 1.2 Evaluación

**Lo que está bien:**

- El tamaño es sano: 185 clases bien repartidas, sin "módulo Dios". La separación contratos/implementación como módulos distintos es exactamente la que a Maker le falta y aquí ya existe.
- Dirección de dependencias limpia, `aws` correctamente aislado de la cadena principal.
- `business` es el único módulo con batería de tests significativa (43), que además cubre lo más crítico (DAO, DataRow, seguridad).

**Los problemas reales:**

1. **`jbs-interfaces` está contaminado con Apache POI**: `IExcelImportSrv` e `IExcelRowProcessor` exponen tipos de POI en sus firmas, con lo cual el módulo de contratos puros — que todo el ecosistema arrastra — depende de poi-ooxml. El costo se propaga a todo consumidor de cualquier interfaz.
2. **`jbs-web` es un cajón mixto con las dependencias más pesadas del stack**: JSF/PrimeFaces + REST/JAX-RS + import Excel (POI) + reportes (JasperReports + Groovy) + Guava en un solo artefacto. Quien solo necesita los controladores JSF carga Jasper y Groovy igual.
3. **`appcatalog` en test sources**: el modelo del catálogo de aplicaciones —parte esencial del framework (seguridad, empresas, usuarios)— no se publica; cada app lo copia. Es la causa raíz de una parte de los "overrides" que Maker mantiene en `org.javabeanstack.model.appcatalog`.
4. **Versiones desalineadas y datadas**: `javaee-api 7.0` en interfaces mientras Maker compila contra 8.0; PrimeFaces 6.2 (2018); Hibernate 5.4.24 como dependencia directa de `business` (acopla el framework al vendor en compile scope en vez de tratarlo como provisto por WildFly).
5. **Los 13 split packages** funcionan hoy, pero: (a) hacen imposible JPMS para siempre, (b) permiten el patrón de "override por orden de classpath" que Maker explota (`org.hibernate.dialect`, `py.com.oym.frame.security`, appcatalog) — frágil e invisible, y (c) confunden la frontera API/impl que los módulos sí definen bien a nivel Maven.
6. **Cobertura de tests desigual**: 0 tests en core, 4 en web. Para un framework del que dependen dos capas aguas abajo (Oym-frame y Maker), la red de seguridad previa a cualquier migración es delgada.

---

## 2. Propuesta de optimización modular

### 2.1 Principio rector

Al revés que en Maker, aquí **no hay nada grande que partir**: la granularidad ya es correcta. El trabajo es **sanear fronteras** (qué vive en qué módulo y qué arrastra cada uno) y **convertir el patrón de override por classpath en puntos de extensión explícitos**. Todo cambio debe medirse contra su costo en los consumidores (Oym-frame, Maker): renombrar paquetes rompe imports aguas abajo, así que lo incompatible se agrupa para la única ruptura ya inevitable — la migración a Jakarta (sección 3).

### 2.2 Cambios de estructura sugeridos (compatibles, sin romper consumidores)

```
jbs-parent
├── jbs-interfaces      ← igual, PERO sin POI (ver 1º punto)
├── jbs-commons         ← igual (opcional: absorber jbs-core, ver 4º punto)
├── jbs-core            ← igual
├── jbs-business        ← igual + entidades appcatalog promovidas desde test (2º punto)
├── jbs-web             ← solo JSF/PrimeFaces + filtros + util web liviano
├── jbs-web-rest        ← NUEVO: rest/resources, rest/filters, rest/exceptions, rest/model
├── jbs-excel           ← NUEVO: import/export Excel (POI) + IExcelImportSrv/IExcelRowProcessor
├── jbs-report-jasper   ← NUEVO: integración JasperReports (+ groovy)
├── jbs-aws             ← igual
└── jbs-bom             ← NUEVO: BOM publicado para consumidores
```

1. **Sacar POI de `jbs-interfaces`.** Mover `IExcelImportSrv`/`IExcelRowProcessor` al nuevo `jbs-excel` (mantienen paquete `org.javabeanstack.web.util`, así los imports de Maker no cambian — solo cambia la dependencia Maven). Alternativa más ambiciosa para la versión 2.x: rediseñar las firmas para no exponer tipos POI (recibir `InputStream`/abstracciones propias) y que POI sea detalle de implementación.
2. **Promover `appcatalog` de test a producción** (en `jbs-business` o un `jbs-model-appcatalog`). Las apps que hoy copian las 24 entidades pasan a extenderlas o usarlas directo; se elimina una familia entera de overrides en Maker.
3. **Partir `jbs-web` por peso de dependencias** (`jbs-web` JSF, `jbs-web-rest`, `jbs-excel`, `jbs-report-jasper`). El criterio no es estético: PrimeFaces, Jasper+Groovy y POI son las tres dependencias más pesadas y con ciclos de vida de actualización distintos; hoy están soldadas. `jbs-web` puede conservar dependencias a los nuevos módulos durante una versión (transición sin ruptura) y soltarlas después.
4. **Opcional — fusionar `jbs-core` (12 clases) dentro de `jbs-commons`**: dos micro-módulos con roles solapados (utilidades e implementaciones base). Beneficio modesto; solo si se quiere reducir la lista de artefactos. Si se hace, `jbs-core` puede quedar una versión como jar vacío que depende del fusionado.
5. **`dependencyManagement` en el padre + `jbs-bom` publicado**: una sola declaración de versión por dependencia (hoy POI está duplicado), y un BOM que Oym-frame y Maker importan para dejar de perseguir versiones a mano. Alinear ya `javaee-api` a 8.0 (Maker ya compila contra 8.0; el 7.0 de interfaces es el más restrictivo del stack).
6. **Hibernate y wildfly-ejb-client a scope `provided`** en `jbs-business` (los provee WildFly). Donde el framework use API propia de Hibernate (no JPA), aislarla en clases puente — eso acota el trabajo cuando llegue Hibernate 6.
7. **Convertir los overrides por classpath en puntos de extensión.** Maker pisa `org.hibernate.dialect`, `py.com.oym.frame.security`, `org.javabeanstack.log`. Cada uno de esos casos es una señal de que falta un mecanismo de extensión (configuración del dialecto por propiedad, `ServiceLoader`/CDI `@Alternative`/`@Specializes` para seguridad y logging). Regla propuesta: **ningún consumidor debería necesitar redeclarar un paquete del framework**.
8. **Subir cobertura donde hay 0**: `jbs-core` (error/xml/config es código estable, fácil de testear) y los converters/util de `jbs-web`. No por métrica: es la red de seguridad de la migración Jakarta.

### 2.3 Cambios incompatibles — reservar para la versión Jakarta (2.0)

Agrupar en una única ruptura mayor, comunicada como tal:

- **Eliminar los split packages**: mover implementaciones a subpaquetes propios (`org.javabeanstack.data.impl`, etc.) o renombrar paquetes de contratos. Es un `sed` masivo en consumidores — solo se justifica cuando ya van a tocar todos los imports por `javax→jakarta`.
- Rediseño de firmas con tipos POI (punto 1, alternativa ambiciosa).
- Limpieza de métodos `throws Exception` genéricos en la API pública, si se desea (hoy es convención documentada; cambiarla es ruptura de firma).

---

## 3. Compatibilidad con Java 11 en adelante

### 3.1 JPMS

Misma conclusión que para Maker, con un agravante propio: con 13 split packages, **JPMS es estructuralmente imposible** para JavaBeanStack sin la reorganización de paquetes de 2.3. Aun después de eso, no lo recomiendo: los consumidores corren en WildFly (jboss-modules ignora `module-info`) y el framework vive de reflection JPA/CDI. Si algún día se quisiera al menos compatibilidad de nombre, basta agregar `Automatic-Module-Name` en el MANIFEST de cada jar (una línea por `pom.xml`, cero riesgo) — eso sí se puede hacer hoy y reserva los nombres de módulo.

### 3.2 Jakarta: JavaBeanStack es el primer dominó

Como se estableció en el análisis de Maker, **toda la ruta a JDK 17/21 del stack OyM empieza aquí**: Maker y Oym-frame no pueden migrar a `jakarta.*` antes que este framework. Plan sugerido:

| Paso | Qué | Notas |
|---|---|---|
| 0 | Sección 2.2 completa (fronteras + BOM + provided + tests) | Reduce la superficie a transformar y crea la red de seguridad |
| 1 | Alinear a javaee-api 8.0 y publicar 1.6.0 estable | EE 8 es el punto de partida oficial de las herramientas de migración |
| 2 | Rama 2.0: OpenRewrite (`rewrite-jakarta`) / Eclipse Transformer | `javax→jakarta` es ~95 % mecánico en un código de este tamaño |
| 3 | En la misma 2.0: eliminar split packages y firmas con POI | Única ventana de ruptura |
| 4 | Hibernate 6 (llega con WildFly 30+) | El riesgo real: `AbstractDAO`/`DataNativeQuery`/dialectos usan API que cambió entre 5 y 6; es reescritura dirigida, no transformación |
| 5 | PrimeFaces 12+ (classifier `jakarta`) en jbs-web | Salto 6.2 → 12+ implica revisar componentes/temas en los consumidores JSF |
| 6 | JasperReports 7 / POI 5 en los módulos nuevos | Aislados en jbs-report-jasper / jbs-excel, ya no arrastran al resto |

Riesgo concentrado: pasos 4 y 5. El resto es mecánico. Ventaja de hacerlo primero en JavaBeanStack: 185 clases y 55 tests son un piloto pequeño y controlado antes de encarar Oym-frame y las ~1.500 clases de Maker.

### 3.3 Aprovechable ya en Java 11

Ídem Maker: `List.of()`, `String.isBlank()/strip()`, `Files.readString()`, `Optional.isEmpty()` — sin tocar `var` (excluido por convención).

---

## 4. Roadmap y esfuerzo estimado

| Fase | Trabajo | Esfuerzo | Riesgo |
|---|---|---|---|
| A1 | `dependencyManagement` + jbs-bom + javaee-api 8.0 + scopes provided | 1–2 días | Muy bajo |
| A2 | Extraer jbs-excel (saca POI de interfaces, mismo paquete) | 1–2 días | Bajo |
| A3 | Extraer jbs-web-rest y jbs-report-jasper | 2–3 días | Bajo |
| A4 | Promover appcatalog a producción + adaptar Maker para consumirlo | 3–5 días | Medio (toca seguridad/catálogo en Maker) |
| A5 | Puntos de extensión que eliminen overrides por classpath | 1–2 semanas | Medio |
| A6 | Tests en core/web + `Automatic-Module-Name` | 3–5 días | Muy bajo |
| B | Versión 2.0 Jakarta (pasos 2–6 de §3.2) | 4–8 semanas | Alto en Hibernate 6 y PrimeFaces 12 |

**Recomendación final**: la estructura modular de JavaBeanStack ya es esencialmente correcta — el trabajo valioso no es dividir sino **descontaminar fronteras** (POI fuera de interfaces, jbs-web partido por peso de dependencias, appcatalog publicado) y **reemplazar el override por classpath con extensión explícita**, que es la deuda compartida más peligrosa del stack. Ejecutar la fase A (≈ 3–4 semanas) deja al framework listo para ser el piloto de la migración Jakarta, que es el verdadero habilitador de "Java 11 en adelante" para todo el ecosistema Maker.

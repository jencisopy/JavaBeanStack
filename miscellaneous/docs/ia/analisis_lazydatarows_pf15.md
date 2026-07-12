# Análisis y corrección de LazyDataRows (R1 — Faces 4.1 / PrimeFaces 15)

**Fecha:** 2026-07-12 · **Estado: R1 cerrado provisionalmente** (código corregido y verificado; reverificación runtime pendiente, ver §6)
**Archivo:** `JavaBeanStack/web/src/main/java/org/javabeanstack/web/jsf/controller/LazyDataRows.java` (jbs-web 2.0)
**Contexto:** ítem R1 del [análisis de modularización](analisis_modularizacion_javabeanstack.md) / [anexo de pendientes](pendientes_modularizacion_javabeanstack.md) — validación de Faces 4.1 / PrimeFaces 15 y resolución de los TODOs de `LazyDataRows`.

## 1. Contrato de PrimeFaces 15 (resuelve el TODO de `count()`)

Verificado contra el fuente de PrimeFaces **15.0.6** (`DataTable.loadLazyScrollData`):

1. `model.setRowCount(model.count(filterBy))` — **`count()` se invoca ANTES de `load()`**.
2. `calculateFirst()` — con ese rowCount recorta `first` si quedó fuera de rango
   (p. ej. después de que un filtro redujo el total). En requests de client cache,
   un overflow **cancela** el fetch.
3. `model.load(offset, rows, sortBy, filterBy)`.
4. El callback `totalRecords` del paginador se toma de `model.getRowCount()`
   **después** de load, por lo que el `setRowCount()` hecho dentro de `load()` es
   el que ve el paginador.

El javadoc de `LazyDataModel#count` sanciona explícitamente el patrón "single-call"
(total y datos en una sola pasada): `count()` trivial + `recalculateFirst()`/
`setRowCount()` dentro de `load()`. Es el patrón adoptado.

**Decisión:** `count()` devuelve `getRowCount()` (último total conocido), **no 0**,
porque los caminos que reutilizan la página cacheada (`noLazyRowsLoad`, atributo
`"nolazyload"` del FacesContext) retornan de `load()` sin recalcular el total; si
`count()` devolviera 0, PF fijaría rowCount=0 antes de load y esos caminos dejarían
el paginador vacío.

## 2. Bugs corregidos

| # | Severidad | Defecto | Corrección |
|---|-----------|---------|------------|
| 1 | **Alta** | `getParams()` casteaba `e.getValue()` (un `FilterMeta`) a `String` en las ramas Long/Integer/Short/BigDecimal/LocalDateTime/Date y en el else final → **ClassCastException al filtrar cualquier columna no alfanumérica**. Resto de la migración a la firma PF10+ (antes los filtros eran `Map<String,Object>`). | Se extrae `FilterMeta.getFilterValue()` una sola vez y se convierte desde `value.toString()`. Si el componente ya entregó el valor tipado (`clase.isInstance(value)`, caso típico de PF15 con converters/datePicker) se usa directo. |
| 2 | **Alta** | NPE en `getParams()` cuando `DataInfo.getFieldType()` devuelve null (campo inexistente en la entidad, filtro global de PF). `getFilterExpression()` sí lo chequeaba, `getParams()` no. | Guard de `clase == null` (pasa el valor sin convertir, coherente con la expresión `o.campo = :param`). |
| 3 | **Media** | El **filtro global** de PF (`globalFilter`) generaba `o.globalFilter = :globalFilter` → query inválida. | Se ignoran en `getParams()` y `getFilterExpression()` las entradas sin valor y la clave `FilterMeta.GLOBAL_FILTER_KEY` (helper `isFilterable()`), con reglas idénticas en ambos métodos para mantener consistencia expresión↔parámetros. |
| 4 | **Media** | Bug de precedencia en la cadena de `if` de strings: `contain_trim` quedaba fuera del chequeo `String.class.isAssignableFrom(clase)`, con lo que una columna **no** string con filterMode `contain_trim` entraba a la rama de LIKE con cast a String. | Reescrito como `switch` sobre el filterMode dentro de la rama String. |
| 5 | **Media** | Página fuera de rango tras filtrar: `count()` devolvía el total viejo, `calculateFirst()` de PF no recortaba y el usuario veía una **página vacía**. | Tras calcular el total real en `load()`, se llama `recalculateFirst(first, pageSize, rowCount)`; si el offset cambió se reposiciona (`setFirstRow` + `requery`) y se devuelve la última página válida. |
| 6 | **Baja** | NPE en `getRowData()` si el registro posicionado no tiene id (`getId().toString()` sobre fila nueva) o si alguna fila de la lista tiene id null. | Guards de null; además los rowkeys sin formato `{Tipo}valor` (id alternativo, ver `DataRow.getRowkey()`) ahora se resuelven comparando contra `getRowkey()` en vez de devolver null. |
| 7 | **Baja** | NPE en `getFilterMode()`/`getFilterMask()` si `ColumnModel.getFilter()`/`getName()`/`getFilterMode()` son null o si el controller no tiene dataTable. En `getParams()` se llamaba `.equalsIgnoreCase()` sobre el resultado sin `Fn.nvl`. | Helper `findColumn()` null-safe; ambos métodos devuelven `""` como neutro. |
| 8 | **Baja** | `load()` devolvía `null` con context null o ante excepción (PF15 lo tolera envolviéndolo en emptyList, pero `LazyDataModelIterator` y código cliente no necesariamente). | Devuelve lista vacía en todos los caminos de error. |

## 3. Mejoras

- **Fechas tipadas de PF15**: los filtros de `p:datePicker` llegan como `LocalDate`/
  `LocalDateTime`/`Date`, no como String. `getParams()` los acepta tipados
  (`LocalDate.atStartOfDay()`, `LocalDates.toDateTime(Date)`) además del parseo
  String de siempre. Se agregó la rama `Boolean` (antes caía en el else que
  guardaba el `FilterMeta` crudo).
- `setRowCount(pageSize)` provisorio eliminado (era pisado siempre); el cálculo del
  total se extrajo a `calculateRowCount()` (real vía `getCount(getLastQuery(), …)`
  o estimado con `noCount`, manteniendo el `first + pageSize + 1` que habilita la
  página siguiente del paginador).
- Bloque de `setOrder` simplificado (solo se fija cuando la expresión no es vacía;
  semántica idéntica a la original).
- La máscara de filtro mal formada ahora deja traza en `LOGGER.debug` en vez de
  tragarse la excepción sin rastro.
- Javadoc completo de la clase y de todos los métodos (ciclo de vida PF15, modos
  de filtro `exact/exact_trim/exact_ltrim/contain/contain_trim/contain_ltrim/
  contain_rtrim`, máscaras `right_blank_N`/`left_blank_N`/`replace('a','b')`,
  banderas `noCount` y `"nolazyload"`).

## 4. Compatibilidad

- **API pública sin cambios**: constructor, campo público `context`,
  `isNoCount/setNoCount`, `getRows`, `getRowData(String)`, `getIdValue` (protected),
  `getRowKey`, `getEntityClass`, `load(int,int,Map,Map)`, `count(Map)`.
  Los usos en Maker (`Datatable.getLazyDataRows()`, `AppMkRootCtrlEvents`/
  `CtrlEvents` leyendo `getRowCount()`) no se ven afectados.
- `AbstractDataController.onGetFilterString(Map<String,Object>)` se sigue invocando
  con el map de `FilterMeta` (compila por el tipo raw de `context`). **Ojo para
  quien lo sobreescriba**: los values del map son `FilterMeta`, no el valor crudo
  — usar `FilterMeta.getFilterValue()`. No hay overrides hoy en Maker ni Oym-frame.
- Compilado OK: `mvn -o -q -pl web compile` en JavaBeanStack (Java 25).

## 5. Verificación de dependencias y consumidores (2026-07-12)

**Clases de las que depende LazyDataRows** (firmas y semántica verificadas en fuente):

- `AbstractDataObject` (jbs-business): `getType/getOrder/setOrder/getFilterParams/
  getDataRows/getRow/addFilterParams/setFirstRow/setMaxRows/requery/isOpen/
  addFilter(key,filtro)/removeFilter(key)/getLastQuery` — OK. `addFilterParams`
  hace `putAll` (acumula); los parámetros huérfanos de filtros previos son
  inofensivos porque `AbstractDAO.populateQueryParameters` **solo asigna los
  parámetros presentes en el texto de la consulta**. `requery()` traga la
  excepción y devuelve false dejando `dataRows=null` — cubierto por el guard de
  `calculateRowCount`.
- `AbstractDAO.getCount(sessionId, query, params)`: reescribe a
  `select count(*) from …` recortando el `order by` — compatible con el
  `getLastQuery()` que incluye el orden.
- `AbstractDataController`: `beforeLazyRowsLoad/afterLazyRowsLoad/getNoLazyRowsLoad
  (Boolean inicializado en false)/getFacesCtx().getFacesContext()/setRowSelected/
  onGetFilterString/getDataTable` — OK.
- `DataRow.getRowkey()` (String desde jbs 2.0): formato `{Tipo}valor`, o el id
  alternativo **sin formato** cuando id es null — ambos caminos cubiertos en
  `getRowData`.
- `DataInfo.getFieldType`: devuelve null en campo inexistente (cubierto) y
  resuelve campos anidados `a.b` por reflexión de fields declarados.
- Utilitarios `Strings` (isNullorEmpty/leftPad/rightPad/substr), `Fn.nvl`,
  `LocalDates.toDateTime(String|Date)`, `Dates.toDate(String)`,
  `ErrorManager.showError` — existentes con las firmas usadas.

**Consumidores en todos los proyectos locales** (JavaBeanStack, oym-frame, maker,
TestProject, SifenManager, Oym-theme, Tools):

- `AbstractDataController` (jbs-web): campo lazy + `getLazyDataRows()/set…` — OK.
- `IDatatable` (jbs-web): declara `getLazyDataRows()` — OK.
- `DataController` (Oym-frame-web): override de `getLazyDataRows()` que lee el nodo
  `LAZYDATAROWS` del XML del controller y usa `setNoCount(true)`, `addFilter`,
  `setOrder` — API intacta. **Compila OK** contra el jbs-web corregido.
- Maker: `Datatable` (delegación), `AppMkRootCtrlEvents` y `CtrlEvents` (leen
  `getRowCount()` para el label del paginador — coherente con el nuevo `count()`).
  **Maker-controllers, Maker-web y Maker-rest compilan OK.**
- XHTML (Maker-web `jbscomp/ezcomp/mkcomp`: datatable, datatable_read/write,
  dataview, datatablemasterdetail, dt_*): usan `value="#{cc.attrs.bean.lazyDataRows}"`
  con `rowKey="#{item.rowkey}"` — mismo formato que parsea `getRowData(String)`.
  Ninguno accede a otras propiedades del modelo por EL.
- No existen subclases de `LazyDataRows` ni overrides de `onGetFilterString` en
  ningún repo local.

Cadena de builds ejecutada: `jbs-web install` → `Oym-frame-web compile` →
`Maker-controllers/web/rest compile`, todos en verde (offline, Java 25).

## 6. Cierre provisional y reverificación pendiente

**R1 se da por cerrado a nivel de código el 2026-07-12**: TODOs resueltos, bugs
corregidos, dependencias y consumidores verificados (§5) y compilación en verde
en toda la cadena jbs-web → Oym-frame → Maker.

**Queda pendiente la reverificación runtime**, que hoy no puede ejecutarse porque
Maker-web no es desplegable hasta convertir los formularios de PrimeFaces 6 a 15
(proceso aparte, tarea PF6→15). **Al desplegar Maker-web por primera vez sobre
WildFly 40, ejecutar este smoke test** contra la BD real:

1. Grilla lazy con paginación (avanzar/retroceder páginas, total correcto).
2. Filtrar columna **numérica** y **de fecha** (antes: ClassCastException).
3. Filtrar dejando al usuario en una página alta y verificar el reposicionamiento.
4. Selección de fila (rowkey `{Long}nnn` y entidades con id alternativo).
5. Grilla con `noCount=true` (paginador con "siguiente" habilitado).
6. Alta/edición con `update()` (camino `"nolazyload"`, la grilla no debe recargar).

Ayudas para esa sesión: pantallas de Maker que ya configuran `countrows=false`
vía XML (punto 5): `gi_lote_precio`, `documentoctrl`, `gi_dptovta_recupero`,
`gi_lote`. Activar `DEBUG` de `org.javabeanstack` en WildFly: `AbstractDAO.getCount`
loguea el `select count(*)` generado y ahí se ve cualquier filtro mal traducido
antes que en la UI.

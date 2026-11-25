# JavaBeanStack
Framework para construcción de aplicaciones **Jakarta EE 11** (rama `master`; la rama `1.5.x` mantiene la línea Java EE 8)

## POI Stack ##
Todo lo construido sobre **Apache POI**: planillas Excel y documentos Word. El módulo
se llamó `jbs-excel` hasta 2026-08; se renombró a **`jbs-poi`** al absorber también
las plantillas Word — XSSF (Excel) y XWPF (Word) viven en el mismo artefacto
`poi-ooxml`, así que la división por formato no aislaba ninguna dependencia.

### `org.javabeanstack.poi.excel` — planillas
- **`ExcelUtil`** — apertura de libros, exportación de datos de consulta a un libro y descarga.
- **`ExcelDataSource`** — fuente del subsistema de salida (`org.javabeanstack.outputs`): planilla tabular como documento en memoria. Único camino del subsistema para el formato de planilla.
- **`ExcelSheetWriter`** — escritor **incremental** de una hoja `xlsx` (SXSSF): recibe las filas de a una y no necesita la lista completa (ver «Escritura incremental»).
- **`ExcelImportSrv`** / **`ExcelRowProcessor`** — importación de datos, con sus contratos `IExcelImportSrv` / `IExcelRowProcessor`.
- **`ExcelColumns`** / **`ExcelColumnSpec`** / **`ColumnRequirement`** — especificación de las columnas de la planilla que consume `ExcelRowProcessor` (ver más abajo).
- **`ExcelColumnsXml`** — arma un `ExcelColumns` desde un bloque XML (nodo DOM, recurso `IXmlDom`, texto o archivo + ruta del tag); ver «Columnas desde XML».

#### Columnas con especificación

`ExcelRowProcessor` acepta el mapeo histórico `Map<String, String>` (cabecera → atributo,
todas las columnas opcionales y sin valor por defecto) o una **especificación completa** de
columnas en `ExcelColumns`, que además declara por columna:

- **obligatoriedad** (`ColumnRequirement`): `OPTIONAL` (puede faltar), `COLUMN` (la cabecera
  tiene que existir; si falta, `checkMetaData()` devuelve «Falta la columna obligatoria «X»»
  y la importación no lee filas) y `VALUE` (además la celda no puede estar vacía: la fila
  queda marcada con el error «La columna «X» no puede estar vacía», número 50000, y va al
  listado de filas con error);
- **valor por defecto**, fijo o calculado por fila (`Supplier`), que se aplica cuando la
  columna **no existe** en la planilla y —solo si se pide con `defaultWhenBlank()`— también
  cuando la celda está vacía. Se convierte al tipo del atributo igual que el valor de una
  celda. `defaultWhenBlank()` **prevalece sobre `VALUE`**: con celda vacía se aplica el valor
  por defecto y la fila no se marca con error, porque el valor por defecto es la declaración
  explícita de qué poner cuando el dato no viene;
- **transformación** opcional del valor de la celda (`converter`), aplicada antes de la
  conversión de tipo; una excepción de la transformación marca la fila con el error «Error al
  convertir la columna «X»: …»;
- **sobreescritura** (`overwrite`, por defecto `true`): con `noOverwrite()` la columna se graba
  solo en las altas; cuando la importación actualiza un registro existente (opción
  «sobreescribir»), ese atributo conserva su valor aunque la planilla traiga otro.

El orden de declaración se conserva (`getHeadToField()` lo respeta) y la cabecera declarada se
normaliza con `trim()`, igual que las cabeceras que se leen de la planilla.

```java
private static ExcelColumns buildColumns() {
    ExcelColumns c = new ExcelColumns();
    c.add("RUC / Nº de Identificacion del Informado", "ruc").require(ColumnRequirement.VALUE);
    c.add("Fecha de Emisión", "fecha").required();                 // = COLUMN
    c.add("sucursal", "sucursal").defaultValue("01");              // opcional con default
    c.add("moneda", "moneda").defaultValue("GS.").defaultWhenBlank();
    c.add("Concepto", "concepto");                                  // opcional, sin default
    return c;
}
```

La subclase pasa esa colección al constructor
`ExcelRowProcessor(Row, Class<T>, ExcelColumns, int headerRowIndex, Map<String, Object>)`
(o `(Row, Class<T>, ExcelColumns)` si los encabezados están en la fila 0 y no hay
propiedades) y, si sobrescribe `process()`, llama primero a `super.process()` —o a
`applyColumns(target)`— para que las columnas declaradas se asignen antes de las reglas
propias. El procesador guarda una **copia** de la colección: completarla después de construirlo
no cambia lo que el procesador ve.

Tres detalles que conviene tener presentes:

- **Valor por defecto calculado**: escribilo como lambda en el lugar (`.defaultValue(() -> ...)`).
  Una variable declarada `Supplier<String>` no es subtipo de `Supplier<Object>`; la sobrecarga
  del proveedor es `defaultValue(Supplier<?>)` y `defaultValue(Object)` deriva a ella si
  recibe un `Supplier`, así que las dos formas funcionan, pero un `Supplier` **no** se puede
  declarar como valor por defecto literal.
- **Valor por defecto que no se puede aplicar**: si es fijo, `checkMetaData()` lo avisa antes
  de leer filas («El valor por defecto de la columna «X» no es convertible a …»); si es un
  proveedor (no se evalúa en `checkMetaData()`, porque podría tener efecto) o si falla al
  asignarlo, la fila queda marcada con «El valor por defecto de la columna «X» no se pudo
  aplicar: …» (número 50000) y la importación sigue.
- **`null` literal en el constructor de cinco argumentos**: `(Row, Class, ExcelColumns, int,
  Map)` y el histórico `(Row, Class, Map, int, Map)` tienen la misma cantidad de argumentos,
  así que `super(row, clazz, null, 0, null)` es **ambiguo**. Para el mapeo identidad hay que
  decir cuál es: `ExcelColumns.of(null)` o `(Map<String, String>) null`.

#### Columnas desde XML (`ExcelColumnsXml`, 2026-09-16)

La misma declaración de `buildColumns()` puede vivir en XML, un tag por columna, y leerse con
`ExcelColumnsXml`, que devuelve el `ExcelColumns` en el orden del XML. En Maker el bloque va en
el `xml/<formulario>.xml` del controller (`PAGE/EXCELIMPORT/<SET>/COLUMNS`), hermano de
`DATATABLES`; el parser solo mira los hijos de `COLUMNS`:

```xml
<COLUMNS>
    <!-- header = nombre del tag en minúsculas; field = header -->
    <CTACTE required="VALUE"/>
    <CODIGO required="VALUE"/>
    <NOMBRE required="COLUMN"/>
    <RUC/>
    <INACTIVO default="false"/>
    <BLOQUEARATRASO header="bloquearAtraso" field="bloquearAtraso" default="false"/>
    <ESPROVEEDOR default="0" overwrite="false"/>
    <MONEDA default="GS." defaultwhenblank="true"/>
</COLUMNS>
```

| Atributo | Valores | Si se omite | Equivale a |
|---|---|---|---|
| `header` | texto exacto de la cabecera (con `trim`) | nombre del tag en minúsculas | 1er argumento de `add` |
| `field` | atributo de la vista destino | `= header` | 2º argumento de `add` |
| `required` | `OPTIONAL` · `COLUMN` · `VALUE` (sin distinguir mayúsculas); sinónimos `true` = `COLUMN`, `false` = `OPTIONAL` | `OPTIONAL` | `.required()` / `.require(...)` |
| `default` | cadena; se convierte al tipo del atributo al aplicarse (`default=""` es un default vacío, distinto de no declararlo) | sin default | `.defaultValue(...)` fijo |
| `defaultwhenblank` | `true` · `false`; exige `default` | `false` | `.defaultWhenBlank()` |
| `overwrite` | `true` · `false` | `true` | `.noOverwrite()` |

El nombre del tag es solo un identificador único (en mayúsculas por convención): una cabecera
en camelCase, con espacios o con tildes va en `header="…"`. Los atributos van en minúsculas.

**Fuentes** (todas devuelven `ExcelColumns`):

- `fromNode(Element[, sourceLabel])` — el `<COLUMNS>` ya resuelto, o un nodo con **exactamente
  un** hijo `COLUMNS` (si no, lanza; nunca adivina).
- `fromXmlDom(IXmlDom<Document, Element>, nodePath)` — el recurso que el controller ya tiene
  cargado (`getXmlResource()`, con la herencia `clase/src` aplicada) y la ruta del bloque,
  p. ej. `"PAGE/EXCELIMPORT/DEFAULT"` (XPath relativo o absoluto).
- `fromXml(xmlText[, nodePath])` — un fragmento como texto; **sin** herencia `clase/src`.
- `fromFile(path, nodePath)` — archivo + ruta del bloque; aplica la herencia `clase/src` del
  subárbol pedido, resolviendo `src="file://otro.xml"` **relativo a la carpeta del archivo**.
  Con herencia, los tags de la clase base quedan **antes** de los propios y sus atributos se
  heredan solo cuando faltan en el tag propio.

**Retoques en Java.** Lo que el XML no expresa —`converter` y el valor por defecto calculado
(`Supplier`)— se agrega encima con `ExcelColumns.edit(header)`, que devuelve el `Builder`
cargado con la especificación vigente y republica **en la misma posición**; una cabecera no
declarada lanza `IllegalArgumentException`:

```java
ExcelColumns c = ExcelColumnsXml.fromXmlDom(getXmlResource(), "PAGE/EXCELIMPORT/DEFAULT");
c.edit("fecha").converter(v -> LocalDates.toDateTime(v.toString()));
c.edit("secuencia").defaultValue(() -> next());
```

**El parser es estricto.** Un error de tipeo no puede dejar una columna opcional o sin default
en silencio: atributo desconocido (`requiered`, o `defaultWhenBlank` en camelCase), valor
inválido de `required`/`overwrite`/`defaultwhenblank`, `defaultwhenblank` sin `default`,
cabecera vacía o repetida, tag de columna con elementos hijos, bloque **sin ninguna columna**
(`<COLUMNS/>`) o nodo sin un único hijo `COLUMNS` (`<COLUMS>` mal escrito) ⇒
`IllegalArgumentException` que nombra el bloque y la columna:
«Bloque «PAGE/EXCELIMPORT/DEFAULT» (ctactesub.xml), columna «CODIGO»: atributo desconocido
«requiered»». Se toleran `clase`, `src` y los atributos `__*` que deja la herencia; los
comentarios XML se ignoran. Que el bloque vacío sea un error importa: el constructor base de
`ExcelRowProcessor` trata un `ExcelColumns` vacío como mapeo identidad de toda la planilla.
Los defaults no convertibles al tipo del atributo los sigue reclamando `checkMetaData()`.

#### Escritura incremental (`ExcelSheetWriter`, 2026-11-24)

`ExcelUtil.toExcelSheets` escribe con SXSSF pero recibe **todas** las filas en una lista: para
exportar un conjunto grande hay que leerlo entero antes de empezar. `ExcelSheetWriter` escribe
una hoja **fila por fila** (`writeRow(Object[])`), así quien lee por lotes escribe cada lote y
lo suelta; en memoria quedan la ventana de SXSSF (`ROW_WINDOW`, 100 filas) y el lote en curso.

```java
try (ExcelSheetWriter writer = new ExcelSheetWriter("Clientes",
        new String[]{"codigo", "nombre", "saldo"},
        new Class<?>[]{String.class, String.class, BigDecimal.class});
     OutputStream out = Files.newOutputStream(archivo)) {
    for (Object[] fila : lote) {
        writer.writeRow(fila);
    }
    writer.writeTo(out);          // una sola vez, después de la última fila
}                                 // close() borra los temporales de SXSSF
```

- **El tipo declarado de la columna decide la celda**, no el valor: `String`/`Character` →
  texto (un `"001"` sigue siendo `"001"`; se conservan los espacios finales); enteros (`Long`,
  `Integer`, `Short`, `Byte`, `BigInteger`) → numérica con formato `0`; `BigDecimal`/`Double`/`Float`
  → numérica sin máscara (General), o con la que fije `setColumnFormat(col, "#,##0.00")`;
  `LocalDateTime`/`Date` → fecha `dd/mm/yyyy hh:mm:ss`; `LocalDate`/`java.sql.Date` → `dd/mm/yyyy`;
  `java.sql.Time`/`LocalTime` → hora (fracción del día) con formato `hh:mm:ss`; `Boolean` → texto
  `true`/`false`; `byte[]` → rechazado en el constructor; tipo `null` u `Object` → según el valor.
  Nulo deja la celda vacía. Un valor que no corresponde al tipo declarado se escribe por su
  propio tipo (no se descarta).
- **Precisión**: Excel guarda los números como `double` (15 dígitos significativos). Un **entero**
  (`Long`, `BigInteger`) con más de 15 dígitos significativos (sin contar los ceros finales) se
  escribe como **texto** con todos sus dígitos, y la importación lo vuelve a leer al tipo entero;
  un decimal más largo no vuelve igual: si una columna lo necesita, declararla `String`.
- **Lo que Excel no puede guardar** no rechaza la fila: se aplica una regla fija y el escritor
  **cuenta** cada celda afectada, para que quien exporta lo informe:
  texto de más de 32.767 caracteres → se recorta a ese largo (`getTruncatedCellCount()`);
  fecha u hora anterior al 1900-01-01 o posterior al 9999-12-31 → texto **en el formato que lee la
  importación** (`LocalDates.toDateTime(String)`): `dd/MM/yyyy` para una fecha sola, `dd/MM/yyyy HH:mm:ss`
  con hora, sin fracciones (una fecha sola con año de cinco cifras lleva `00:00:00`) (`getOutOfRangeDateCount()`), así que vuelve como fecha y hora por
  `ExcelRowProcessor` (la fecha sola, a las 00:00:00); `NaN` e infinito (`Double`, `Float`) → celda
  vacía (`getInvalidNumberCount()`). Dos límites de la importación, no del escritor: un año anterior
  al 1 no vuelve (el formato no lleva era) y un atributo `LocalDate` no se importa con ninguna celda de
  fecha (la conversión solo produce `LocalDateTime`).
- **Formato de texto**: las celdas que el escritor emite como texto (texto, booleanos, enteros largos,
  fechas fuera de rango) llevan el formato `@`, para que Excel no las convierta en número si alguien
  las edita (`001` no pasa a `1`).
- **Encabezados** únicos, de texto y no vacíos (las condiciones de `ExcelRowProcessor.checkMetaData`);
  el constructor lo verifica, comparándolos sin los espacios de los extremos (como la importación), y
  los escribe recortados. La fila de encabezados puede no ser la primera
  (`new ExcelSheetWriter(hoja, encabezados, tipos, headerRowIndex)`), para producir una planilla
  que una importación lea con su `headerRowIndex`.
- **Anchos** fijados al abrir por encabezado y tipo, sin `autoSizeColumn`; `setColumnWidth(col, chars)`
  los cambia. El nombre de la hoja se sanea con la regla de `ExcelUtil` (31 caracteres, sin `[]:*?/\`).
- Comparte con `ExcelUtil` los estilos de texto, fecha y cabecera; la salida de `ExcelUtil`
  (asistente de informes, `ExcelExportSrv`) no cambia.

Para entregar el archivo sin cargarlo en memoria, el subsistema de salida tiene
`FileOutputDocument` (módulo `outputs`): un `IOutputDocument` respaldado por un archivo, cuyo
`writeTo(OutputStream)` copia el disco por bloques; `DownloadTarget` (módulo `web`) entrega todo
documento con `writeTo`, así que un documento en memoria sale igual que antes. `delete()` borra el
archivo cuando el dueño ya no lo necesita.

### `org.javabeanstack.poi.word` — documentos Word
- **`WordTemplateMerge`** — reemplaza los marcadores `<<campo>>` de una plantilla .docx con los valores de un mapa; el reemplazo se resuelve por párrafo (Word parte los marcadores en varios runs).
- **`WordTemplateSource`** — fuente del subsistema de salida: ubica la plantilla (rutas del file system → artefacto desplegado), hace el merge y devuelve el documento en memoria.

Módulo desacoplado de la capa web: **no depende de `jbs-web` ni de PrimeFaces**; declara `jbs-business`, `jbs-outputs` + `poi-ooxml` y usa `FacesContext` / `jakarta.servlet` directos. Los consumidores deben declararlo explícitamente.

#### Registros existentes en la importación (`ExcelImportSrv.resolveExistingRow`, 2026-09-16)

Tras `copyTo`, la importación busca el registro existente por el id que haya resuelto la vista
(`@Id` con fórmula `fn_*` sin `classMapped`, modo escalar de `copyTo`) o, si no lo trae, por la
**clave única** de la entidad (`findByUk`, como `importFrom`). Si existe: sin «sobreescribir»
(`overWriteData`) se cuenta como «ya existente» y no se graba; con «sobreescribir» se copian
sobre el existente **solo los valores que trajo la planilla** (`copyTo(existing, true)`): las
columnas ausentes conservan su valor, **incluidas las que declaran `defaultValue`** (el default
solo rige en altas; el procesador anota esos atributos en `DEFAULTED_FIELDS` y la importación
los excluye de la copia), y las columnas declaradas `noOverwrite()` (`NO_OVERWRITE_FIELDS`). Si la búsqueda falla, la fila sigue como alta y decide `checkDataRow`; si falla la **copia** sobre el existente, la excepción se propaga y la importación aborta (no se graba una entidad a medio anular como actualización).

Las dos protecciones actúan sobre el **atributo homónimo de la entidad destino**: la anotación guarda el nombre del atributo de la vista y la anulación exige que exista con ese nombre en la tabla (`ctbcliente` funciona porque la asociación de `Ctactesub` se llama igual). Un atributo que solo existe en la vista —un parámetro de una `fn_id*` que no es columna, como `escliente`/`esproveedor` en `CtactesubView`— no tiene qué anular: ahí `overwrite="false"` no protege nada y el valor sigue participando en la resolución de la clave. Las colecciones hijas (`@OneToMany`) de la entidad convertida se anulan siempre antes de copiar (nunca salen de la planilla; el existente conserva las suyas). La sobrecarga `resolveExistingRow(sessionId, target)` no conoce la fila de la vista y **no aplica** estas protecciones; el flujo usa siempre la de tres argumentos.
El existente puede pertenecer a otra empresa cuando la función `fn_id*` reintenta con
`fn_empresashared` (catálogos compartidos): se acepta como está (decisión del usuario,
2026-09-16; a analizar aparte).


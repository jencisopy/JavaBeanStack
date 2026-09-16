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
los excluye de la copia), y las columnas declaradas `noOverwrite()` (`NO_OVERWRITE_FIELDS`). Si la búsqueda falla, la fila sigue como alta y decide `checkDataRow`.
El existente puede pertenecer a otra empresa cuando la función `fn_id*` reintenta con
`fn_empresashared` (catálogos compartidos): se acepta como está (decisión del usuario,
2026-09-16; a analizar aparte).


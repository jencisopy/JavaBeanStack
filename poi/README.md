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

### `org.javabeanstack.poi.word` — documentos Word
- **`WordTemplateMerge`** — reemplaza los marcadores `<<campo>>` de una plantilla .docx con los valores de un mapa; el reemplazo se resuelve por párrafo (Word parte los marcadores en varios runs).
- **`WordTemplateSource`** — fuente del subsistema de salida: ubica la plantilla (rutas del file system → artefacto desplegado), hace el merge y devuelve el documento en memoria.

Módulo desacoplado de la capa web: **no depende de `jbs-web` ni de PrimeFaces**; declara `jbs-business`, `jbs-outputs` + `poi-ooxml` y usa `FacesContext` / `jakarta.servlet` directos. Los consumidores deben declararlo explícitamente.

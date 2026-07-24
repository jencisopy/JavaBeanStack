# JavaBeanStack
Framework para construcción de aplicaciones **Jakarta EE 11** (rama `master`; la rama `1.5.x` mantiene la línea Java EE 8)

## Excel Stack ##
Importación y exportación de datos desde/hacia planillas **Excel** con Apache POI.

- **`ExcelUtil`** — apertura de libros, exportación de datos de consulta a un libro y descarga.
- **`ExcelImportSrv`** / **`ExcelRowProcessor`** — importación de datos, con sus contratos `IExcelImportSrv` / `IExcelRowProcessor`.

Módulo desacoplado de la capa web: **no depende de `jbs-web` ni de PrimeFaces**; declara `jbs-business` + `poi-ooxml` y usa `FacesContext` / `jakarta.servlet` directos. Los consumidores que usen Excel deben declararlo explícitamente.

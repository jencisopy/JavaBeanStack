# JavaBeanStack
Framework para construcción de aplicaciones **Jakarta EE 11** (rama `master`; la rama `1.5.x` mantiene la línea Java EE 8)

## Jasper Stack ##
Integración con **JasperReports** para la generación de reportes.

- **`JasperReportUtil`** — exportación de reportes; el parámetro `device` acepta `printer`, `html`, `doc`, `pdf` y `xlsx` (con `xls` como alias de `xlsx`). `getReportPdf(...)` devuelve el PDF como `byte[]`.
- Resolución de archivos: se buscan primero los `.jasper` convertidos a JR7 en `reports/v7/` y luego los legados en `reports/`.

Módulo desacoplado de la capa web: usa `FacesContext` directo y depende solo de `jbs-core` (no de PrimeFaces ni de `jbs-web`). Los consumidores que usen reportes deben declararlo explícitamente.

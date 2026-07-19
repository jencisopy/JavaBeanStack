# Documentación de JavaBeanStack (`miscellaneous/docs/ia/`)

## Gestión de unidades de persistencia (DBManager)

Las tres implementaciones de `IDBManager` (`DBManager`, `DBManagerV20`,
`DBManagerV30`; `DBManagerV21` se eliminó el 2026-07-12 por quedar superada
por la V30) viven en este framework
(`business/src/main/java/org/javabeanstack/data/`) y se conmutan por el
`ejb-jar.xml` de la aplicación. Estos documentos son el **origen canónico y
única copia** (la copia que existía en `Maker-miscellaneous/docs/ia/` se retiró
el 2026-07-19 para evitar divergencias; su README apunta acá). Los ejemplos
usan a Maker como aplicación de referencia.

| Documento | Contenido |
|---|---|
| [`STATIC_MANAGMENT_DBMANAGER.md`](STATIC_MANAGMENT_DBMANAGER.md) | Esquema tradicional: PUs declaradas en persistence.xml, bindings JNDI `java:app/em/PUn`, `DBManager` clásico. |
| [`DINAMIC_DATA_MANAGMENT_DBMANAGER_V20.md`](DINAMIC_DATA_MANAGMENT_DBMANAGER_V20.md) | `DBManagerV20`: EMF dinámicos configurados por system properties de la JVM (`jbs.persistence.dynamic.*`). |
| [`DINAMIC_DATA_MANAGMENT_DBMANAGER_V30.md`](DINAMIC_DATA_MANAGMENT_DBMANAGER_V30.md) | `DBManagerV30` (autocontenida): unidades definidas en `META-INF/dynamic_persistence.xml`, una spec completa por unidad (dialecto propio por empresa), plantilla `DEFAULT` con `{n}` para alta de empresas sin redeploy. Test unitario offline: `DBManagerV30Test`. |

## Otros documentos

| Documento | Contenido |
|---|---|
| [`analisis_modularizacion_javabeanstack.md`](analisis_modularizacion_javabeanstack.md) | Análisis y propuesta de optimización de la estructura modular. Actualizado 2026-07-12 al estado 2.0 post-partición: extraídos `jbs-excel`, `jbs-rest` y `jbs-jasper` de `jbs-web`, publicado `jbs-bom` (fuente única de versiones, adoptado por Oym-frame/Maker/TestProject), tests en `core`/`web`/`excel` y `Automatic-Module-Name` en todos los jars. |
| [`pendientes_modularizacion_javabeanstack.md`](pendientes_modularizacion_javabeanstack.md) | Anexo con el detalle de los pendientes de la modularización (R1, R2, R7, R8): qué queda, por qué importa y qué se sugiere. |
| [`analisis_lazydatarows_pf15.md`](analisis_lazydatarows_pf15.md) | Análisis y corrección de `LazyDataRows` para PrimeFaces 15 (cierre provisional de R1); incluye el checklist del smoke test runtime pendiente. |
| [`instalar-jdk25.md`](instalar-jdk25.md) / [`instalar-jdk25.sh`](instalar-jdk25.sh) | Guía y script de instalación del JDK 25. |

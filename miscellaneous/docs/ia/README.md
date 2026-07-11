# Documentación de JavaBeanStack (`miscellaneous/docs/ia/`)

## Gestión de unidades de persistencia (DBManager)

Las cuatro implementaciones de `IDBManager` viven en este framework
(`business/src/main/java/org/javabeanstack/data/`) y se conmutan por el
`ejb-jar.xml` de la aplicación. Estos documentos son el **origen canónico**;
en el repositorio de Maker (`Maker-miscellaneous/docs/ia/`) se mantiene una
copia, y los ejemplos usan a Maker como aplicación de referencia.

| Documento | Contenido |
|---|---|
| [`STATIC_MANAGMENT_DBMANAGER.md`](STATIC_MANAGMENT_DBMANAGER.md) | Esquema tradicional: PUs declaradas en persistence.xml, bindings JNDI `java:app/em/PUn`, `DBManager` clásico. |
| [`DINAMIC_DATA_MANAGMENT_DBMANAGER_V20.md`](DINAMIC_DATA_MANAGMENT_DBMANAGER_V20.md) | `DBManagerV20`: EMF dinámicos configurados por system properties de la JVM (`jbs.persistence.dynamic.*`). |
| [`DINAMIC_DATA_MANAGMENT_DBMANAGER_V21.md`](DINAMIC_DATA_MANAGMENT_DBMANAGER_V21.md) | `DBManagerV21`: EMF dinámicos configurados por la plantilla `DINAMIC_PU` comentada en persistence.xml. |
| [`DINAMIC_DATA_MANAGMENT_DBMANAGER_V30.md`](DINAMIC_DATA_MANAGMENT_DBMANAGER_V30.md) | `DBManagerV30` (autocontenida): unidades definidas en `META-INF/dynamic_persistence.xml`, una spec completa por unidad (dialecto propio por empresa), plantilla `DEFAULT` con `{n}` para alta de empresas sin redeploy. Test unitario offline: `DBManagerV30Test`. |

## Otros documentos

| Documento | Contenido |
|---|---|
| [`analisis_modularizacion_javabeanstack.md`](analisis_modularizacion_javabeanstack.md) | Análisis de la modularización del framework. |
| [`instalar-jdk25.md`](instalar-jdk25.md) / [`instalar-jdk25.sh`](instalar-jdk25.sh) | Guía y script de instalación del JDK 25. |

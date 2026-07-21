# Documentación de JavaBeanStack (`miscellaneous/docs/`)

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

## Informes de análisis y planes de trabajo

Desde el **2026-07-20** ya no se versionan en este repositorio: la antigua carpeta
`miscellaneous/docs/ia/` se eliminó y su contenido pasó a un directorio local, fuera del
repo, para que los informes internos no viajen en un proyecto open source.

| Destino | Qué va ahí |
|---|---|
| `<proyectos>/IA/JavaBeanStack/outputs/` | Informes de análisis generados (`analisis_modularizacion_javabeanstack.md`, `analisis_lazydatarows_pf15.md`, `instalar-jdk25.md`). |
| `<proyectos>/IA/JavaBeanStack/inputs/` | Planes de trabajo y listas de pendientes (`pendientes_modularizacion_javabeanstack.md`). |
| `.claude/tools/` del workspace | Scripts reutilizables (`instalar-jdk25.sh`). |

En la instalación de referencia `<proyectos>` es `/home/jenciso/oym/proyectos`. Si esas
carpetas no existen (por ejemplo tras migrar de máquina), hay que preguntar dónde guardar
en vez de asumir una ruta.

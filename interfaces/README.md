# JavaBeanStack
Framework para construcción de aplicaciones **Jakarta EE 11** (rama `master`; la rama `1.5.x` mantiene la línea Java EE 8)

## Interfaces Stack ##
Contiene únicamente los **contratos** (interfaces y anotaciones) del framework, sin implementaciones. Es el módulo base del que dependen todos los demás.

Agrupa los contratos de:
- **Acceso a datos** — `IDataRow`, `IDataLink`, `IGenericDAO`, `IDataSet`, `IDataResult`, filtros y consultas.
- **Servicios** — `IDataService` (validación de negocio) y servicios asociados.
- **Seguridad** — `ISecManager`, `ISessions`, `IOAuthConsumer`, `IUserSession`.
- **Modelo del catálogo** — usuario, empresa, permisos, mensajes, tokens, recursos, parámetros.
- **XML, log, eventos, errores, configuración y recursos.**
- **Anotaciones** — `@AuditEntity`, `@CheckMethod`, `@DBFilter`, `@FieldFilter`, `@SignatureField`, etc.

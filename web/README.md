# JavaBeanStack
Framework para construcción de aplicaciones **Jakarta EE 11** (rama `master`; la rama `1.5.x` mantiene la línea Java EE 8)

## Web Stack ##
Capa de presentación web basada en **JSF / PrimeFaces**. Provee la infraestructura de UI del framework:

- **Controladores base** — `AbstractDataController` (managed bean para pantallas CRUD con lazy loading y despliegue de errores), `AbstractAuthController`, entorno del usuario.
- **Converters JSF** — `AbstractDataConverter` (entidad ↔ texto), `LocalDateTimeConverter`.
- **Filtros y listeners** — autenticación (`AuthFilter`), ciclo de vida JSF.
- **Componentes de UI** — contrato `IDatatable`, modelo de columnas dinámicas.
- **Utilidades** — `FacesContextUtil` (acceso simplificado al `FacesContext`, mensajes y navegación) y búsqueda de recursos.

Depende de `jbs-business` (y de la cadena core/commons/interfaces). Los reportes Jasper, la importación Excel y los recursos REST viven ahora en los módulos `jbs-jasper`, `jbs-excel` y `jbs-rest` respectivamente.

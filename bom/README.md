# JavaBeanStack
Framework para construcción de aplicaciones **Jakarta EE 11** (rama `master`; la rama `1.5.x` mantiene la línea Java EE 8)

## BOM (Bill of Materials) ##
Módulo de gestión de dependencias del framework. Centraliza en su `dependencyManagement` las versiones de los artefactos `jbs-*` y de las dependencias de terceros (jakartaee-api, Hibernate, PrimeFaces, POI, JasperReports, log4j, JUnit, wildfly-ejb-client).

Es el **parent** de `jbs-parent`, por lo que todos los módulos reciben las versiones sin declararlas. Los proyectos consumidores (Oym-frame, Maker, TestProject) lo **importan** en su `dependencyManagement` con `<scope>import</scope>`.

- Al subir la versión de una dependencia, editar **solo** este módulo.
- Packaging `pom` (no genera artefacto binario).

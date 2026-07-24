# JavaBeanStack
Framework para construcción de aplicaciones **Jakarta EE 11** (rama `master`; la rama `1.5.x` mantiene la línea Java EE 8)

## Rest Stack ##
Soporte para exponer recursos web **RESTful (JAX-RS)**. Contiene:

- **`AbstractWebResource`** — base de los recursos JAX-RS (validación de token, acceso a la sesión y datos del cliente).
- **`CORSFilter`** — filtro de CORS.
- **Excepciones** — `JpaNoExist`, `OptionUnavailable`, `TokenError`.
- **Modelos de mensajes** — `ErrorMessage`, `MessageResponse`.

Módulo liviano: depende solo de `interfaces` + `commons` (no arrastra JSF). Los consumidores de recursos REST deben declararlo explícitamente.

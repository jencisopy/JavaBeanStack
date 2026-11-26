# JavaBeanStack-Sifen (`jbs-sifen`)

Cliente de **SIFEN** (Sistema Integrado de Facturación Electrónica Nacional, DNIT, Paraguay):
armado, firma y envío de documentos electrónicos (DE) y eventos, consultas de RUC, DE y lote, y
validación de firma. El módulo **deriva de [RSHK jsifenlib](https://github.com/roshkadev/rshk-jsifenlib)**
(Roshka S.A. + Taxare S.A., licencia MIT): el aviso original está en
[`LICENSE-rshk-jsifenlib.md`](LICENSE-rshk-jsifenlib.md) y en el [`NOTICE`](../NOTICE) del
repositorio; el módulo se publica bajo la LGPL v3, como el resto de JavaBeanStack.

## Estado

- **Versión:** `2.2.0` (Jakarta EE 11, Java 25). Paquetes `org.javabeanstack.sifen.*`; nombre de
  módulo (`Automatic-Module-Name`) `org.javabeanstack.sifen`.
- **API pública v1 estable**: la fachada `ISifenClient` y los tipos que usa (`org.javabeanstack.sifen.api`,
  `SifenConfig`, `SifenException`, `ValidationResult`). La foto de las firmas públicas está en
  [`src/test/resources/contract/api_v1.txt`](src/test/resources/contract/api_v1.txt) y un test la
  compara en cada build.
- **Compatibilidad**: dentro de `2.x` la API solo **agrega**; no se quita ni se cambia una firma. Lo
  marcado `@Provisional` (hoy `sendEvents`, `validateEvents`, el detalle de `ConnectionDiagnosis`, el
  registro de envíos `ISendRegistry` y la conciliación `SifenReconciler`; ver
  [`api_v1_provisional.txt`](src/test/resources/contract/api_v1_provisional.txt)) sigue provisional en
  la `2.2.0` y **puede cambiar** dentro de `2.x`, con aviso en las notas de la versión: conviene usarlo
  aislado detrás de una clase propia. Se promueve a estable en una versión posterior.
- **Deprecado, se mantiene en `2.x`**: la fachada estática `Sifen`, los `Respuesta*` y
  `ValidezFirmaDigital`, heredados de `rshk-jsifenlib`. No se quitan ni cambian de firma dentro de `2.x`,
  no reciben operaciones nuevas y no están en la foto de firmas; se retiran en la `3.0`. No llevan la
  anotación `@Deprecated`: el reemplazo es `ISifenClient`. Los 11 tipos que aparecen en sus firmas
  mantienen sus firmas públicas en `2.x`: `TxProtDe` y `TxContenDE`, que además se retiran en la `3.0` con lo
  que los expone, y `TgResProc`, `TgResProcLote`, `TgResProcEVe`, `TxContRuc`, `TxContenEv`, `TrContEv`,
  `BaseResponse`, `SifenObjectBase` y `GenerationCtx`, de los que no se promete más (de `GenerationCtx`, en la
  `3.0` se retiran las sobrecargas de `Sifen` que lo reciben, no el tipo). El motor de validación (`DEValidator`, `EventValidator`, `ValidationContext`) no es API pública: se
  valida con `ISifenClient.validateDE` y `validateEvents`.

Qué ofrece:

- **Un cliente por empresa**: `SifenClient.create(config)` para un uso puntual, o un
  `SifenClientRegistry` de la aplicación que crea y guarda un `ISifenClient` por clave de empresa
  (`get(clave, proveedorDeConfiguracion)`, `invalidate(clave)`, `closeAll()`). El cliente es seguro
  entre hilos.
- **Emisión en dos pasos**: `sign(de)` valida, calcula el CDC, firma y arma el QR; la aplicación graba
  el XML firmado y recién después llama a `send(firmado)`. `readSigned(xml)` reconstruye el documento
  firmado desde lo grabado, sin volver a firmar.
- **Envío de resultado desconocido**: si un envío pudo haber llegado a SIFEN, la excepción lo dice
  (`isOutcomeUnknown()`) y se resuelve con `resolve(firmado)`, que consulta por CDC y envía **una** vez
  solo si SIFEN no lo tiene. Nunca se reenvía a ciegas.
- **Lote** (`sendBatch`, `queryBatch`), **eventos** (`cancel`, `invalidateNumbers`, `sendEvents`),
  **consultas** (`queryDE`, `queryRuc`) y **verificación de firma** de un XML recibido
  (`verifySignature`).
- **Conciliación** (provisional): `SifenReconciler` recorre una lista de documentos firmados con una
  pausa entre consultas y un tope por pasada, y dice por cada uno si SIFEN lo tiene igual (`MATCH`), no lo
  tiene aprobado (`NOT_FOUND`, «Sin aprobación en SIFEN (0420)»), lo tiene con otro digest
  (`DIGEST_MISMATCH`) o no se pudo determinar (`UNDETERMINED`: una falla de la consulta o un documento de
  otro ambiente nunca cuentan como ausentes). Solo consulta: no envía ni reenvía nada. La lista (p. ej.
  los emitidos en un rango de fechas) la arma la aplicación, y **deja afuera** los documentos de un lote
  todavía en proceso, los recién enviados y los rechazados: SIFEN también contesta 0420 por ellos, y
  resolverlos como ausentes arriesga un duplicado o el bloqueo del RUC. **Fijar siempre el tope**
  (`maxQueries`; por omisión `0`, sin tope): contra un SIFEN que no contesta, cada consulta espera el
  tiempo de lectura (60 s) y la pasada no corta sola tras varias fallas.
- **Diagnóstico**: `validateConfig()`, `getCertificateInfo()` (titular, RUC, vigencia) y
  `checkConnection()` (proxy, DNS, TCP, TLS, HTTP), sin enviar documentos.
- **Validación previa** del DE con mensajes en español (ver abajo).
- **Modelo de errores único**: toda falla sale como `SifenException`, con código estable
  (`JBS-<CAT>-<nn>` o el código de la regla), categoría, mensaje para el usuario, detalle técnico aparte
  e **id de correlación**, que también encabeza las líneas del log de esa operación. Un **rechazo de
  SIFEN no es una excepción**: vuelve en el resultado con estado `REJECTED` y sus mensajes.
- **Transporte sin reinicio**: cada cliente tiene su propio contexto TLS y su almacén de certificados;
  ante una falla de transporte o un cambio de certificado se rearman sin reiniciar la JVM. Tiempos de
  espera, proxy y reintentos se leen de la configuración en cada pedido.

## Uso rápido

```java
SifenConfig config = SifenConfig.load(Path.of("/ruta/sifen.properties"));
try (ISifenClient sifen = SifenClient.create(config)) {
    DocumentoElectronico de = ...;                 // armado por la aplicación
    SignedDocument firmado = sifen.sign(de);       // valida, calcula el CDC, firma y arma el QR
    guardar(firmado.getCdc(), firmado.getXml());   // la aplicación lo graba ANTES de enviar

    SendResult r = sifen.send(firmado);
    switch (r.getStatus()) {
        case APPROVED, APPROVED_WITH_OBSERVATION -> marcarAprobado(r.getCdc(), r.getAuthorizationProtocol());
        case REJECTED -> mostrarRechazo(r.getMessages());   // SifenMessage: getCode(), getText()
        default -> pendiente(r);
    }
} catch (SifenValidationException e) {
    mostrarErrores(e.getErrors());                 // todos juntos: ValidationError con getCode(), getPath(), getMessage()
} catch (SifenException e) {
    if (e.isOutcomeUnknown()) {
        marcarEnDuda();                            // pudo haber llegado a SIFEN: no reenviar
    }
    mostrar(e.getCode(), e.getUserMessage(), e.getCorrelationId());
}
```

Un documento que quedó en duda se resuelve más tarde desde el XML grabado, sin volver a firmar:
`SendResult r = sifen.resolve(sifen.readSigned(xmlGuardado));`

En una aplicación con varias empresas el cliente se toma de un `SifenClientRegistry` creado una vez
(en un bean `@ApplicationScoped` o `@Singleton`) y cerrado en su `@PreDestroy`:

```java
ISifenClient sifen = registry.get(claveEmpresa, () -> SifenConfig.load(propiedadesDeLaEmpresa));
registry.invalidate(claveEmpresa);   // al cambiar la configuración o el certificado de la empresa
```

## Validación previa

Está **activa por omisión** (`sifen.validacion_previa=true`). `sign`, `cancel`, `invalidateNumbers` y
`sendEvents` validan antes de firmar o enviar y, si algo falla, lanzan `SifenValidationException` con
**todos los errores juntos** (`getErrors()`), cada uno con su código, la ruta del campo y un mensaje en
español. `validateDE(de)` hace lo mismo sin firmar ni contactar a SIFEN y devuelve un
`ValidationResult` (`getErrors()`, `getWarnings()`, `isValid()`), útil para mostrar los problemas antes
de numerar el documento en firme. Con `sifen.validacion_previa=false` la librería no valida el contenido del
DE ni de los eventos y deja ese control a la aplicación; la configuración y el certificado se validan
igual.

## Configuración

`SifenConfig.load(...)` acepta un `Path`, un `File`, un `InputStream`, la ruta de un archivo o un
`Properties`. Claves principales:

| Clave | Por omisión | Descripción |
|---|---|---|
| `sifen.ambiente` | — (obligatoria) | `DEV` (pruebas) o `PROD` |
| `sifen.url_base` | la del ambiente | URL base de los servicios; tiene que ser `https` y coherente con el ambiente |
| `sifen.certificado_cliente.archivo` | — | Certificado PFX del emisor: ruta del archivo o su contenido en Base64 |
| `sifen.certificado_cliente.contrasena` | — | Contraseña del certificado |
| `sifen.certificado_cliente.tipo` | — | Tipo del certificado: `PFX` (el único admitido; hay que indicarlo) |
| `sifen.certificado_cliente.usar` | `true` | Usar el certificado en la conexión TLS; en `false` la autenticación la hace otro nivel (un proxy) |
| `sifen.csc.id` / `sifen.csc` | el CSC público de pruebas | Código de seguridad del contribuyente para el QR; en `PROD` el genérico no se admite |
| `sifen.validacion_previa` | `true` | Validación previa del DE y de los eventos |
| `sifen.validacion_reglas_desactivadas` | — | Códigos de regla a desactivar, separados por coma |
| `sifen.validacion_fecha_reglas` | hoy | Fecha de referencia de las reglas (`AAAA-MM-DD`): qué reglas rigen |
| `sifen.http.connect_timeout` | `15000` (15 s) | Tiempo de espera al conectar, en milisegundos |
| `sifen.http.read_timeout` | `60000` (60 s) | Tiempo de espera de la respuesta, en milisegundos (SIFEN tiene hasta un minuto para responder) |
| `sifen.proxy.host` / `sifen.proxy.port` | — | Proxy HTTP propio de la configuración (el puerto es obligatorio si hay servidor); sin él rige el proxy de la JVM |
| `sifen.proxy.user` / `sifen.proxy.pass` | — | Usuario y contraseña del proxy (la contraseña es obligatoria si hay usuario) |
| `sifen.reintentos.max` | `0` | Reintentos de una **consulta** ante una falla rápida de transporte (máximo 10); se recomiendan **3**. Una recepción (`send`) nunca se reintenta sola |
| `sifen.reintentos.backoff_ms` | `1000` | Espera antes del primer reintento, en milisegundos; se duplica en cada uno (máximo 60000) |
| `sifen.traza.activa` | apagada | Traza completa de pedido y respuesta. También como propiedad del sistema, en caliente; si la configuración la define, manda la configuración |
| `sifen.traza.carpeta` | — | Carpeta de la traza (un archivo por intercambio); sin ella la traza va al logger `org.javabeanstack.sifen.trace` |
| `sifen.traza.retencion_dias` | `7` | Días que se conservan los archivos de la traza; `0` = no se borran |

Un valor no admitido es un error de configuración: `validateConfig()` lo informa y
`SifenClient.create` no crea el cliente. Una clave `sifen.*` desconocida es solo un aviso. La traza escribe los
documentos completos, con datos de clientes: se enciende solo para diagnosticar.

## Despliegue en WildFly 41

- **SAAJ y log4j los da el servidor** (módulos `jakarta.xml.soap.api`, `com.sun.xml.messaging.saaj` y
  `org.apache.logging.log4j.api`). El WAR lleva **solo `jbs-sifen`**: no empaquetar `saaj-impl` ni
  `log4j-api` (declarar `log4j-api` como `provided` o excluirlo de la dependencia).
- **Proxy con autenticación `Basic` hacia `https`**: el JDK no envía la credencial en el `CONNECT` salvo
  que la JVM del servidor arranque con `-Djdk.http.auth.tunneling.disabledSchemes=` (vacío). Sin eso
  la conexión falla con `JBS-CONEX-11`.
- **Log**: en servidores con datos reales, el logger `org.javabeanstack.sifen` va en `INFO`. La traza
  (`org.javabeanstack.sifen.trace`) queda apagada salvo para diagnosticar.

## Simulador

El `-tests.jar` del módulo (clasificador `tests`) trae **solo** el paquete
`org.javabeanstack.sifen.simulator`: un servidor HTTPS local que contesta respuestas grabadas por
servicio y puede provocar fallas de transporte, para probar una aplicación **sin red ni certificado
real**. `SifenSimulator.start()` lo levanta y `config()` devuelve una `SifenConfig` lista para
`SifenClient.create`, con un certificado autofirmado de un emisor ficticio. Mientras está abierto apunta
`javax.net.ssl.trustStore*` a un almacén temporal (estado global de la JVM): las pruebas que lo usan no
deben correr en paralelo con otras que dependan del almacén de confianza.

```xml
<dependency>
    <groupId>org.javabeanstack</groupId>
    <artifactId>jbs-sifen</artifactId>
    <version>2.2.0-SNAPSHOT</version>   <!-- la del jbs-bom importado; obligatoria: el BOM gestiona jbs-sifen sin clasificador -->
    <classifier>tests</classifier>
    <scope>test</scope>
</dependency>
```

La versión va explícita porque `dependencyManagement` distingue el clasificador: el BOM declara
`jbs-sifen` sin clasificador, y un `jbs-sifen:tests` sin versión no se resuelve.

## Dependencias

| Artefacto | Scope | Nota |
|---|---|---|
| `jakarta.xml.soap:jakarta.xml.soap-api` (SAAJ 3.0) | `provided` | La provee el servidor (WildFly: módulo `jakarta.xml.soap.api`) |
| `com.sun.xml.messaging.saaj:saaj-impl` | `test` | En el contenedor la da el servidor (`com.sun.xml.messaging.saaj`); fuera de contenedor declararla en `runtime`, **junto con la API** (ver abajo) |
| `org.apache.logging.log4j:log4j-api` | compile (del padre) | Logging del framework; en WildFly lo da el servidor |
| `jakarta.platform:jakarta.jakartaee-api` | `provided` (del padre) | API de Jakarta EE 11 |

Versiones y scopes en `jbs-bom`. No depende de otros módulos `jbs-*` ni de ninguna base de datos.

**Fuera de un contenedor** (un `main`, una herramienta) no alcanza con `saaj-impl` en `runtime`: el
`jbs-bom` gestiona `jakarta.xml.soap-api` como `provided`, ese alcance se aplica también cuando la API
llega por transitividad desde `saaj-impl`, y `provided` no entra en el classpath de ejecución (el
programa falla con `NoClassDefFoundError: jakarta/xml/soap/SOAPException`). Hay que declarar las dos,
con alcance explícito:

```xml
<dependency>
    <groupId>com.sun.xml.messaging.saaj</groupId>
    <artifactId>saaj-impl</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>jakarta.xml.soap</groupId>
    <artifactId>jakarta.xml.soap-api</artifactId>
    <scope>runtime</scope>   <!-- o compile; sin alcance explícito queda provided -->
</dependency>
```

## Tests

`mvn -pl sifen test` corre **sin red ni certificado real**: firma con el certificado autofirmado de
`src/test/resources/certificados/` y los tests de envío y consulta usan el simulador. Los tests que
pegan al ambiente de pruebas de SIFEN llevan `@Tag("sifen-live")` y el build los **excluye por
omisión**; se corren solo con el perfil `sifen-live` y la configuración indicada con
`-Djbs.sifen.test.env=<archivo>` (o la variable `JBS_SIFEN_TEST_ENV`), y solo contra el ambiente `DEV`
(ver `SifenTestEnv`):

```bash
mvn -pl sifen -am test -Psifen-live -Djbs.sifen.test.env=/ruta/a/sifen-test.env
```

En la fase `verify` el build abre los dos jars (el principal y el `-tests.jar`) y comprueba que lleven
los avisos de licencia (`LICENSE`, `NOTICE` y `LICENSE-rshk-jsifenlib.md`).

## Cambios de comportamiento

Respecto de `rshk-jsifenlib`:

| Desde | Qué cambia | Quién lo nota |
|---|---|---|
| 2.2.0 | La NT 13 ya no se puede apagar: `dBasExe` se escribe siempre. `SifenConfig.setHabilitarNotaTecnica13` (deprecado) y la clave `sifen.habilitar_nota_tecnica_13` no tienen efecto (la clave se ignora con un aviso en el log al cargarla) | Quien la tenía en `false`: su XML pasa a llevar `dBasExe`, el formato que exige la NT 13 desde el 2023-05-21 |
| 2.2.0 | El evento de nominación escribe la ciudad del receptor como `dDesCiuRec`; antes `dDesCiuEnt` | Quien enviaba nominaciones con ciudad |
| 2.2.0 | El ítem exonerado (`iAfecIVA` = 2) escribe en `dDesAfecIVA` el texto del XSD vigente, «Exonerado (Art. 100 - Ley 6380/2019)»; antes, el del XSD de 2019, «Exonerado (Art. 83- Ley 125/91)», que SIFEN rechaza con 0160. Al leer un XML se toma solo el código, así que un DE con el texto viejo se sigue leyendo como exonerado, y `readSigned` lo conserva tal como se firmó | Quien emitía ítems exonerados: el DE pasa a ser aceptado |
| 2.2.0 | `gRasMerc` ya no escribe `dNomImp`, `dDirImp` ni `dNumFir` (eliminados por la NT 010; SIFEN los rechaza con 0160). Sus accesores en `TgRasMerc` quedan deprecados y sin efecto en el XML (guardan el valor y se leen de un XML anterior; se retiran en la `3.0`) y las reglas JBS-E-99..101 están de baja | Quien cargaba los datos del importador: el DE pasa a ser aceptado sin ellos |
| 2.2.0 | Sin dirección del receptor, el DE no lleva `<dDirRec>` vacío (en B2F y en la nota de remisión salía vacío y el XSD lo rechaza); la validación previa sigue exigiéndola con D213 | Quien enviaba con la validación previa apagada: SIFEN contesta 1318 en vez de 0160 |

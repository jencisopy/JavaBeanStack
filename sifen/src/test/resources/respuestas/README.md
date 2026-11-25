# Respuestas de SIFEN para `SoapClientTest` (plan JBSSIF, §2.6)

Dos clases de archivos:

- **Reales anonimizadas** (`real-*.xml`, M2-06): respuestas grabadas del **ambiente de pruebas** de
  SIFEN en la corrida en vivo de §2.9 (2026-11-23), de las que **no traen datos de clientes**
  (recepción, lote y consulta de lote: solo CDC, protocolo, códigos y mensajes). Lo único que se
  cambió es el RUC+DV del emisor dentro de cada CDC, por el ficticio `80123456-5`, con el DV del CDC
  recalculado (`anonimizar_respuestas_fixtures.py`, fuera del repo); `dProtAut`, `dDigVal`, fechas,
  número de lote, códigos y mensajes quedan como los devolvió SIFEN. Las consultas de DE y los
  pedidos enviados **no** se suman: traen el DE completo con datos del receptor.
- **Sintéticas** (el resto): armadas a mano a partir del Manual Técnico v150 y de lo que lee la
  librería (`processResponse` de cada `Req*`), con el emisor ficticio `80123456-5`.

| Archivo | Operación | Qué fija |
|---|---|---|
| `real-recepcion-de-0260.xml` | `recepcionDE` | **Real**: aprobado (0260), `dProtAut`, fecha con zona `-03:00`, mensaje con entidad `&#243;` |
| `real-recepcion-de-1107.xml` | `recepcionDE` | **Real**: rechazado 1107 («Fecha de inicio de vigencia del timbrado incorrecta»), sin `dProtAut` |
| `real-recepcion-lote-0300.xml` | `recepcionLoteDE` | **Real**: lote recibido, `dProtConsLote` de 18 dígitos, `dTpoProces` 0 |
| `real-consulta-lote-0362.xml` | `consultaLoteDE` | **Real**: lote concluido con 8 DE, 7 aprobados y 1 rechazado 1306 («RUC del receptor inexistente…») |
| `consulta-ruc-0502.xml` | `consultaRUC` | RUC encontrado, `xContRUC` |
| `consulta-ruc-0500.xml` | `consultaRUC` | RUC inexistente, sin `xContRUC` |
| `consulta-ruc-0502-indentada.xml` | `consultaRUC` | Respuesta con espacios entre elementos: `ResponseUtil.getMainNode` toma el nodo de texto y da `NullPointerException` (E2-04) |
| `recepcion-de-0260.xml` | `recepcionDE` | `rProtDe` aprobado (0260) |
| `recepcion-de-rechazo.xml` | `recepcionDE` | `rProtDe` rechazado |
| `recepcion-lote-0300.xml` | `recepcionLoteDE` | Lote recibido, `dProtConsLote` |
| `consulta-lote-0362.xml` | `consultaLoteDE` | Lote procesado, `gResProcLote` |
| `consulta-de-0422.xml` | `consultaDE` | CDC encontrado; `xContenDE` escapado (la librería lo desescapa) |
| `evento-0600.xml` | `recepcionEvento` | Evento registrado |
| `soap-fault.xml` | cualquiera (HTTP 500) | SOAP Fault: respuesta vacía con `codigoEstado` 500 |
| `http-500.html` | cualquiera (HTTP 500) | Cuerpo que no es SOAP |

Sin espacios entre elementos (así responde SIFEN, como confirman las reales), salvo el caso indentado.

## Respuestas del simulador (§8.6) y la consulta de DE con `rDE` y eventos (I10-11)

Las que usa `SifenSimulator` viven en `org/javabeanstack/sifen/simulator/responses/` (van en el `test-jar`). Las
genera `fase8/generar_respuestas_simulador.py` de los insumos del plan: copias con nombre en inglés de las de esta
carpeta (`receive-batch-received`, `query-batch-completed[-real]`, `query-ruc-found`/`-not-found`,
`event-registered`, `soap-fault`) y las nuevas:

| Archivo | Qué fija |
|---|---|
| `receive-de-approved.xml`, `receive-de-rejected.xml` | Plantillas de recepción (0260; rechazo real 1107 o el `dCodRes` que se pida) que devuelven el CDC y el `DigestValue` del DE recibido |
| `receive-batch-not-queued.xml` | Lote no encolado (0301) |
| `query-batch-in-process.xml` | Lote en procesamiento (0361) |
| `query-de-found.xml` | **I10-11**: consulta de DE (0422) con el `rDE` firmado de `golden/DE-01.xml`, su `dProtAut` y un evento de cancelación (`rContEv`: `golden/EV-01.xml` + la respuesta de `evento-0600.xml`), con el `xContenDE` escapado como lo manda SIFEN. Valores esperados en `query-de-found.properties` |
| `query-de-not-found.xml` | DE no existe o fue rechazado (0420) |
| `proxy-502.html`, `proxy-504.html`, `not-soap.html` | Página de un proxy con HTTP 502/504 y un portal cautivo con HTTP 200 (no SOAP) |

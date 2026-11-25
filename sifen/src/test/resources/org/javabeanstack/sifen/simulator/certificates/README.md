# Certificados del simulador de SIFEN (plan JBSSIF, §8.6)

Desechables, sin valor fuera de las pruebas, clave `jbs-sifen-test`. Viajan en el `test-jar` de `jbs-sifen`
(clasificador `tests`). Los genera `fase8/generar_certs_simulador.sh` de los insumos del plan (fuera del repo).

| Archivo | Qué es | md5 |
|---|---|---|
| `simulator-server.p12` | Clave del servidor HTTPS del simulador: `CN=localhost, OU=jbs-sifen simulator`, SAN `IP:127.0.0.1` y `DNS:localhost`, EKU `serverAuth`, 2026-11-24 → 2036-11-21 | `f20b50e0023a41f016d03c5fde33017d` |
| `simulator-trust.p12` | Almacén de confianza con solo ese certificado; el simulador lo copia a un temporal y apunta `javax.net.ssl.trustStore*` a él mientras está abierto | `69dbcd25d4cd3a1b5dae0265d87efdfb` |
| `client.pfx` | Certificado de cliente y de firma de la configuración por omisión: copia byte a byte de `certificados/jbs-sifen-autofirmado.pfx` (emisor ficticio `80123456-5`) | `b2dc810f87b65ab253a4e0dbc41cc084` |

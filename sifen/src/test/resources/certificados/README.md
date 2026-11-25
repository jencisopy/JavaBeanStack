# Certificado autofirmado de los tests de `jbs-sifen`

`jbs-sifen-autofirmado.pfx` es un certificado **autofirmado y desechable** (plan JBSSIF, D11): no
protege nada real y **no sirve para conectarse a SIFEN** (ni DEV ni PROD), que exige un certificado
emitido por una PSC habilitada.

| | |
|---|---|
| Tipo | PKCS#12 (`3DES` + MAC `SHA-1`, legible por JDK 11 y 25) |
| Clave del almacén | `jbs-sifen-test` |
| Alias | `jbs-sifen-autofirmado` |
| Subject | `C=PY, O=EMPRESA FICTICIA DE PRUEBA S.A., OU=F1, SERIALNUMBER=RUC80123456-5, CN=EMPRESA FICTICIA DE PRUEBA S.A.` |
| SAN | correo `facturacion@example.com` + `dirName` (`O=…`, `title=APODERADO`, `SERIALNUMBER=RUC80123456-5`) |
| Vigencia | 2026-11-23 17:15:38 → **2036-11-20 17:15:38** (GMT-03:00; `keytool -list -v`). Al vencer, la firma sigue validando en los tests (`validateSignature` no mira la vigencia) pero conviene regenerarlo: cambian el certificado embebido en los XML y **todos** los golden |

Lo usan `SignatureTests` (firma y validación por la librería), `GoldenXmlTest`, el arnés A/B (fuera del repo) y, como certificado de servidor y de cliente, `SoapClientTest` (TLS mutuo contra un servidor HTTPS local); md5 `b2dc810f87b65ab253a4e0dbc41cc084`, idéntico a `~/.jbs-sifen/autofirmado/jbs-sifen-autofirmado.pfx`. Se generó con
`generar_cert_test.sh` del relevamiento del plan (§0.5); el emisor `80123456-5` es el ficticio de
los fixtures. Versionarlo está permitido por RNF5b (decisión del usuario, Q12): es desechable y con datos ficticios. El certificado de prueba real (Q3) nunca va en el repositorio ni bajo `src/` (D16, RNF5b). El guion `generar_cert_test.sh` vive en `IA/Workspace-java/JavaBeanStack/inputs/jbs_sifen_insumos/` y escribe fuera de todo repo; para reemplazar este archivo se genera ahí y se copia (I2-02).

## Certificados de las reglas `JBS-CERT-*` (§3.7, CA3-08)

Desechables, con datos ficticios y la misma clave `jbs-sifen-test`; los genera
`generar_certs_reglas_cert.sh` (`IA/Workspace-java/JavaBeanStack/inputs/jbs_sifen_insumos/fase3/`) a
partir de `jbs-sifen-autofirmado.pfx`. Los usa `ConfiguracionValidatorTest`.

| Archivo | Para | Contenido |
|---|---|---|
| `jbs-sifen-no-pkcs12.pem` | `JBS-CERT-05` | el certificado del autofirmado en PEM (no es PKCS#12) |
| `jbs-sifen-sin-clave.p12` | `JBS-CERT-06` | PKCS#12 con solo el certificado de confianza, sin clave privada |
| `jbs-sifen-sin-firma.p12` | `JBS-CERT-12` | par de claves sin `digitalSignature` en el uso de clave ni `SERIALNUMBER` en el sujeto |
| `jbs-sifen-dos-alias.p12` | `JBS-CERT-07` | el autofirmado más la entrada de `jbs-sifen-sin-firma.p12` |
| `jbs-sifen-apoderado.p12` | `JBS-CERT-11` | persona física apoderada: `SERIALNUMBER=CI1234567` en el sujeto y `RUC80123456-5` solo en el `dirName` del SAN (forma del cualificado tributario F1 real; `openssl`) |
| `jbs-sifen-apoderado-varios.p12` | `JBS-CERT-11` | `SERIALNUMBER=CI1234567` en el sujeto; en el SAN un correo y tres `dirName`: `RUC80999999-1` (otra empresa), `ruc80123456` (el emisor, minúscula y sin DV) y uno sin `SERIALNUMBER` (`openssl`) |
| `jbs-sifen-sin-ruc.p12` | `JBS-CERT-11` | persona física: solo `SERIALNUMBER=CI1234567` en el sujeto, sin SAN y sin `keyUsage` (`openssl`) |

Vencido, todavía no vigente y por vencer (`JBS-CERT-08/09/10`) no necesitan archivo: la prueba mueve el
reloj del validador sobre el autofirmado (vigente de 2026-11-23 a 2036-11-20).

## Emisores de las empresas B y C (§10.5, CA10-04)

Dos emisores ficticios más, con el mismo formato que el autofirmado (`generar_cert_test.sh` con
`JBS_SIFEN_RUC`/`JBS_SIFEN_DV`/`JBS_SIFEN_RAZON`, vía `fase8/generar_certs_simulador.sh` de los insumos del
plan), misma clave `jbs-sifen-test`, alias `jbs-sifen-autofirmado`, vigentes de 2026-11-24 a 2036-11-21 (regenerados el 2026-11-24 con RUC fuera del rango asignado, cotejo RNF5b). Los usa
`MultiCompanyClientTest` (tres empresas y veinte hilos contra el simulador).

| Archivo | Emisor | md5 |
|---|---|---|
| `jbs-sifen-empresa-b.pfx` | `EMPRESA B FICTICIA DE PRUEBA S.A.`, `RUC80999901-3` | `20ced6126c33ff2e788158b3b7f73b17` |
| `jbs-sifen-empresa-c.pfx` | `EMPRESA C FICTICIA DE PRUEBA S.A.`, `RUC80999902-1` | `d40ff1da2853a8769645a1fa0e7aac86` |

Los certificados del **simulador** (`SifenSimulator`, §8.6) viven aparte, en
`org/javabeanstack/sifen/simulator/certificates/` (van en el `test-jar`): ver el README de esa carpeta.

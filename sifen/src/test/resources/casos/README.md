# Catálogo de casos de prueba de `jbs-sifen` (plan JBSSIF, §2.1)

Un XML de entrada por caso de DE: un `rDE` **sin firma** (los tres primeros traen una firma de
relleno que la librería ignora), emisor ficticio `80123456-5` («EMPRESA FICTICIA DE PRUEBA S.A.»,
el del certificado autofirmado de `../certificados/`), timbrado ficticio `12345678` desde
2026-01-02. Ningún dato real: los dos XML en USD vienen de un caso de producción **anonimizado**
(sustitución de RUC, nombres, direcciones, CDC, firma y QR; importes, fechas y catálogos
conservados, decisión del usuario M0-18); el resto es inventado.

Los casos con *constructor* se arman además por objeto en `CasosDePrueba` (mismo paquete de tests
`org.javabeanstack.sifen.test.casos`); su XML de entrada lo generó el **fork** con el arnés A/B (fuera del
repo, D8) y quedó congelado acá. Fechas (`dFecFirma` 2026-11-23T10:00:05, `dFeEmiDE`
2026-11-23T10:00:00), `dCodSeg` y números de documento son fijos: la salida es determinista con el
mismo certificado (A3). Todos se generan con NT 13 activa (`habilitarNotaTecnica13 = true`, E0-16). Desde la Fase 6 (§6.3, D25) la NT 13 ya no se puede apagar: el flag no tiene efecto.

| ID | Archivo | Tipo de DE | Operación | Moneda | Receptor | Impuesto | Redondeo | Origen | Defecto que fija |
|---|---|---|---|---|---|---|---|---|---|
| DE-01 | `factura-simple-00` | Factura (1) | Contado efectivo, B2B | PYG | Contribuyente | IVA 10 % × 2 ítems | sí (resto 0) | XML a mano | — |
| DE-02 | `caso-usd-sin-redondeo-original` | Factura (1) | Crédito, B2B | USD, cambio global | Contribuyente | IVA 10 % | releído ⇒ **sí** | XML anonimizado | E0-01 (el flag se pierde al leer), E0-03 |
| DE-03 | `caso-usd-sin-redondeo-original` | Factura (1) | Crédito, B2B | USD | Contribuyente | IVA 10 % | **no** (`TgTotSub(false)` tras leer) | XML anonimizado + ajuste | — |
| DE-04 | `caso-usd-con-redondeo-aprobado` | Factura (1) | Crédito, B2B | USD | Contribuyente | IVA 10 % | sí | XML anonimizado (versión aprobada) | E0-03 |
| DE-05 | `factura-pyg-b2c-cedula` | Factura (1) | Contado efectivo, B2C | PYG | No contribuyente, CI | IVA 10 % | sí (47789 → 47750) | objeto | E0-13 (`dTotOpeItem` con 2 decimales en PYG) |
| DE-06 | `factura-pyg-b2c-sin-redondeo` | Factura (1) | Contado, B2C | PYG | No contribuyente, CI | IVA 10 % | por objeto **no**, releído **sí** | objeto | E0-01 (golden `.obj` distinto) |
| DE-07 | `factura-usd-credito-plazo` | Factura (1) | Crédito a plazo, B2B | USD, cambio global | Contribuyente | IVA 10 % × 2 | sí | objeto | E0-03 |
| DE-08 | `factura-usd-cambio-por-item` | Factura (1) | Contado, B2B | USD, cambio por ítem | Contribuyente | IVA 10 % × 2 | sí | objeto | E0-03 |
| DE-09 | `factura-iva-mixto` | Factura (1) | Contado, B2B | PYG | Contribuyente | IVA 10 %, 5 %, exento | sí | objeto | — |
| DE-10 | `factura-gravado-parcial-pyg` | Factura (1) | Contado, B2B | PYG | Contribuyente | Gravado parcial 30 % al 10 % (`dBasExe`) | sí | objeto | E0-04 (resuelto en `TgCamIVA` del fork) |
| DE-11 | `factura-gravado-parcial-usd` | Factura (1) | Contado, B2B | USD | Contribuyente | Gravado parcial 50 % al 5 % | sí | objeto | — |
| DE-12 | `factura-exonerado` | Factura (1) | Contado, B2B | PYG | Contribuyente | Exonerado | sí | objeto | — |
| DE-13 | `factura-descuentos` | Factura (1) | Contado, B2B | PYG | Contribuyente | IVA 10 % | sí | objeto | — (`dPorcDescTotal` solo del descuento global; desde I12-21 el descuento global va prorrateado en cada ítem, EA004) |
| DE-14 | `factura-anticipo` | Factura (1) | Contado, B2B, anticipo por ítem | PYG | Contribuyente | IVA 10 % | sí | objeto | I0-13 (`dTotAntItem` sin × cantidad) |
| DE-15 | `factura-b2g` | Factura (1) | Contado, B2G (`gCompPub`, `dDncpG/E`) | PYG | Contribuyente | IVA 10 % | sí | objeto | — |
| DE-16 | `factura-innominado` | Factura (1) | Contado, B2C | PYG | Innominado | IVA 10 % | sí | objeto | — |
| DE-17 | `factura-extranjero` | Factura (1) | Contado, B2C | USD | Extranjero, pasaporte | IVA 10 % | sí | objeto | — |
| DE-18 | `factura-tasa-10-00` | Factura (1) | Contado, B2B | PYG | Contribuyente | IVA `10.00` | sí | objeto | **E0-17** (no suma: `dTotOpe` = 0) |
| DE-19 | `factura-credito-cuotas` | Factura (1) | Crédito en 3 cuotas, B2B | PYG | Contribuyente | IVA 10 % | sí | objeto | — |
| DE-20 | `autofactura` | Autofactura (4) | Contado, compra a no contribuyente; constancia asociada | PYG | El propio emisor | sin `gCamIVA` | sí | objeto | — |
| DE-21 | `nota-credito` | Nota de crédito (5) | Devolución; asociada al CDC de DE-01 | PYG | Contribuyente | IVA 10 % | sí | objeto | — |
| DE-22 | `nota-debito` | Nota de débito (6) | Recupero de gasto; asociada a factura impresa | PYG | Contribuyente | IVA 10 % | sí | objeto | — |
| DE-23 | `nota-remision` | Nota de remisión (7) | Traslado por venta, transporte propio | — | Contribuyente | — (sin `gTotSub`) | — | objeto | — |
| DE-24 | `factura-b2f-pasaporte-sin-numero` | Factura (1) | Contado, **B2F** | USD | Extranjero, pasaporte **sin número** (D210 opcional en B2F) | IVA 10 % | sí | objeto | la librería escribe `dNumIDRec` = `0` (I3-39) |
| DE-25 | `inmobiliaria-pyg-redondeo` | Factura (1) | Contado, B2B, venta de inmueble | PYG | Contribuyente | Lote **IVA 5 % sobre el 30 %** + 10 % + exento | sí (29) | objeto | — |
| DE-26 | `inmobiliaria-pyg-sin-redondeo` | Factura (1) | ídem | PYG | Contribuyente | ídem | **no** (`TgTotSub(false)`; releído, sí: golden `.obj`) | objeto | E0-01 |
| DE-27 | `inmobiliaria-usd-redondeo` | Factura (1) | Contado, B2B | USD, cambio global | Contribuyente | ídem | sí, hacia abajo (1680,24 → 1680,00) | objeto | E0-03 |
| DE-28 | `inmobiliaria-usd-sin-redondeo` | Factura (1) | ídem | USD | Contribuyente | ídem | **no** (golden `.obj`) | objeto | E0-01 |
| DE-29 | `inmobiliaria-usd-redondeo-arriba` | Factura (1) | ídem | USD | Contribuyente | ídem | sí, **hacia arriba** (1680,40) | objeto | **I3-34** (`dRedon` 0.1, `dTotGralOpe` 1680.500: F014 ≠ F008 − F013) |
| DE-30 | `factura-de-anticipo` | Factura (1) | **Anticipo** (`iTipTra` 9) | PYG | Contribuyente | Lote 5 % sobre el 30 % | no | objeto | **E3-24** (`<dCDCAnticipo/>` vacío) |
| DE-31 | `factura-final-anticipo-por-item` | Factura (1) | Final, anticipo por ítem (`iCondAnt` 2), asociada a DE-30 | PYG | Contribuyente | ídem | no | objeto | **E3-25** (`dTotAntItem` sin × cantidad) |
| DE-32 | `factura-final-anticipo-global` | Factura (1) | Final, anticipo global (`iCondAnt` 1), asociada a DE-30 | PYG | Contribuyente | Lote parcial + 10 % | no | objeto | **E3-25** (`dTotAnt` sin × cantidad) |

Eventos (sin XML de entrada, solo por objeto en `CasosDePrueba.eventos()`):

| ID | Evento | Defecto que fija |
|---|---|---|
| EV-01 | Cancelación del CDC de DE-01 | — |
| EV-02 | Inutilización de 0000050–0000055 | — |
| EV-03 | Conformidad parcial | — |
| EV-04 | Disconformidad | — |
| EV-05 | Nominación de un no contribuyente con `dDirRec`, departamento, **distrito** y ciudad | E0-12 (la ciudad sale como `dDesCiuEnt`); sin distrito sería E0-18 (NPE, lo fija `EventoNominacionTest`) |
| EV-06 | Nominación de un contribuyente, sin dirección | — |

Lote: `LT-01` = DE-01 + DE-05 leídos de su XML (se compara el `rLoteDE` del zip).

DE-24…DE-32 se agregaron el 2026-11-24 (plan JBSSIF, §2.1.1–§2.1.4; pedido del usuario): importes del MT
calculados a mano en `IA/.../jbs_sifen_insumos/fase3/casos_nuevos/` (`esperados_mt.py`, `salida/comparacion.md`).

Reglas para agregar un caso: se arma en `CasosDePrueba` (o se agrega el XML), se corre el arnés A/B
con `entradas` y `congelar` (`IA/.../jbs_sifen_insumos/equivalencia/correr.sh`) y se agrega la fila
acá. Un caso nuevo nunca reutiliza un ID.

## Fase 12: batería ampliada de casos de facturación (plan JBSSIF, §12.1 y §12.2; 2026-11-25)

Los casos nuevos viven en `CasosFase12` (mismo paquete), **no** en `CasosDePrueba`: el arnés A/B compila
`CasosDePrueba.java` también contra el fork, que no tiene varios de los campos que estos casos usan, y los
casos nuevos no entran al A/B (D53). Su XML de entrada está en `fase12/<archivo>.xml` y su XML de referencia
es **candidato** (`../golden/candidatos/<ID>.xml`): lo genera `jbs-sifen`, se revisa campo por campo contra
§12.1 (`CandidatosFase12Test.CAMPOS`) y pasa a **aceptado** cuando SIFEN-test lo aprueba (§12.3). Todos traen
las leyendas del ambiente de pruebas (Q14) en `dNomEmi` y en el primer ítem. Lo que se espera de cada caso
(errores y avisos exactos de la validación previa y `dCodRes` de SIFEN-test) está en
[`fase12/esperados.tsv`](fase12/esperados.tsv); lo leen `BateriaFacturacionTest` (sin red) y, en §12.3,
`SifenVivoFase12Test`. Se congelan con `-Djbs.sifen.f12.congelar=<src/test/resources>` (`CandidatosFase12Test`).

Columna «Vivo»: `V` se envía en §12.3 (un número) · `V0` en vivo sin número · `N` negativo o exploratorio
(Q39) · `—` solo sin red. «SIFEN»: `?` = no se sabe; `0260?` = a confirmar. Valores revisados por
software-architect el 2026-11-25 (visto bueno de §12.1.3); la tarea de §12.3 de cada fila (o `espera`, que no se
envía todavía) está en la columna `tanda` de `esperados.tsv`.

| ID | Archivo | Clase | Local (errores · avisos) | SIFEN | Vivo | Qué prueba |
|---|---|---|---|---|---|---|
| ID-F08 | `f12-b2f-cedula-extranjera` | positivo | acepta | 0260 | V | B2F, cédula extranjera (D208 3, D213) |
| ID-F09 | `f12-b2f-carnet-residencia` | positivo | acepta | 0260? | V | B2F, carnet de residencia (D208 4) |
| ID-C02 | (`BateriaIdentificacion`) | existente | acepta | 0260? | V | contribuyente persona física (D205 1); Q43 si SIFEN cruza D205 con el RUC |
| ID-C08 | (`BateriaIdentificacion`) | existente | acepta | 0260 | V | contribuyente en B2C |
| ID-N02 | (`BateriaIdentificacion`) | existente | acepta | 0260 | V | B2C, pasaporte |
| ID-N03 | (`BateriaIdentificacion`) | existente | acepta | 0260 | V | B2C, cédula extranjera |
| ID-N04 | (`BateriaIdentificacion`) | existente | acepta | 0260 | V | B2C, carnet de residencia; cubre ID-N11 |
| ID-N05 | (`BateriaIdentificacion`) | existente | acepta | 0260 | V | B2C, tarjeta diplomática (D209 descripción oficial, 1313) |
| DE-41 | `f12-nc-receptor-nominado` | positivo | acepta | 0260? | V | NC al receptor nominado de una FE innominada (H004h · 2441) |
| DE-16 | (`CasosDePrueba`) | existente | acepta | 0260 | V | FE a receptor innominado (35.000 Gs), sin nominar: base de la NC de ID-I06 (I12-09) |
| ID-I06 | (`BateriaIdentificacion`) | existente | rechaza D208e | 1331 | N | innominado en NC, sobre la FE innominada de DE-16 de la corrida: D208e · 1331 es la única regla que incumple |
| ID-I08 | (`BateriaIdentificacion`) | existente | rechaza JBS-D-44 | ? | N | exploratorio: innominado con número distinto de 0 |
| ID-I09 | (`BateriaIdentificacion`) | existente | rechaza JBS-D-46 | ? | N | exploratorio: innominado con nombre distinto de Sin Nombre |
| ID-F04 | (`BateriaIdentificacion`) | existente | rechaza D203 | 1320 | N | B2F con país PRY |
| ID-F06 | (`BateriaIdentificacion`) | existente | rechaza D213 | 1318 | N | B2F sin dirección |
| DE-09 | (`CasosDePrueba`) | existente | acepta | 0260 | V | FE base de las NC parciales |
| DE-39 | `f12-nc-parcial-1` | positivo | acepta | 0260 | V | NC parcial 1 (H004d · 2417, H004f · 2438 solo servidor) |
| DE-39b | `f12-nc-parcial-2` | positivo | acepta | 0260 | V | NC parcial 2 |
| DE-39c | (`CasosFase12`) | variante | acepta | 2417 | N | tercera NC: supera el total (solo servidor) |
| DE-40 | `f12-nc-sobre-impresa` | positivo | acepta | 0260? | V | NC sobre factura impresa (Q43: timbrado preimpreso ficticio) |
| DE-36 | `f12-anticipo-usd` | positivo | acepta | 0260 | V | anticipo USD |
| DE-37 | `f12-final-anticipo-usd` | positivo | acepta | 0260 | V | final USD con anticipo por ítem (cantidad 1) |
| DE-37b | `f12-final-anticipo-usd-cant2` | variante | acepta | — | — | final USD con cantidad 2 (E3-25), solo sin red |
| DE-38 | `f12-nc-final-anticipo-usd` | positivo | acepta | 0260? | V | NC USD sobre la final con anticipo |
| DE-37n | (`CasosFase12`) | variante | acepta | 2438 | N | final PYG con anticipo USD (solo servidor) |
| DE-42 | `f12-autofactura-extranjero-pasaporte` | positivo | acepta | 0260? | V | autofactura a vendedor extranjero con pasaporte (E304 · 2562 solo servidor) |
| DE-43 | `f12-autofactura-extranjero-cedula` | positivo | acepta | — | — | autofactura con cédula extranjera, solo sin red |
| B-neg1 | (`CasosFase12`) | negativo | rechaza H005 | 2405 | — | NC impresa sin timbrado |
| B-neg2 | (`CasosFase12`) | negativo | rechaza JBS-E-78 | — | — | anticipo aplicado sin dCDCAnticipo (E719) |
| B-neg3 | (`CasosFase12`) | negativo | rechaza H001 | 2400 | — | autofactura sin documento asociado |
| B-neg4 | (`CasosFase12`) | negativo | rechaza H002 | 2416 | — | autofactura asociada a un DE electrónico, no a una constancia |
| B-neg5 | (`CasosFase12`) | negativo | rechaza E601 | 1503 | — | autofactura a crédito |
| DE-15 | (`CasosDePrueba`) | existente | acepta | 0260? | V | B2G con gCompPub y DNCP (Q43: códigos ficticios); 0160 posible por la errata del XSD en dEntCont (I12-07) |
| DE-33 | `f12-b2g-sin-gcomppub` | positivo | acepta | 0260 | V | B2G sin gCompPub, con dCodConDncp |
| C-neg2a | (`CasosFase12`) | negativo | rechaza E025 | 1402 | — | dFeCodCont posterior a la emisión |
| C-neg2b | (`CasosFase12`) | variante | acepta | — | — | dFeCodCont igual a la emisión: la validación 1402 solo rechaza la posterior («no puede ser superior»); acepta (I12-04) |
| C-neg2c | (`CasosFase12`) | negativo | rechaza JBS-E-63 | — | — | dDncpG de 9 caracteres (uno de 7 se completa con ceros, I6-09) |
| C-neg2d | (`CasosFase12`) | negativo | rechaza JBS-E-65 | — | — | dDncpE de 2 caracteres |
| DE-34 | `f12-exportacion-bienes` | positivo | acepta | 0260? | V | exportación de bienes (D57, Q44); `dInfoFisc` con el inciso i) de la NT 007 (barcaza) |
| DE-35 | `f12-exportacion-servicios` | positivo | acepta | 0260? | V | exportación de servicios |
| D-neg1 | (`CasosFase12`) | negativo | rechaza D213 | 1318 | — | exterior sin dirección |
| D-neg2 | (`CasosFase12`) | negativo | rechaza D203 | 1320 | — | exterior con país PRY |
| D-neg3 | (`CasosFase12`) | negativo | rechaza JBS-E-147 | — | — | gTransp sin modalidad |
| D-neg4 | (`CasosFase12`) | negativo | rechaza JBS-E-148 | — | — | gTransp sin responsable del flete |
| DE-44 | `f12-tarjeta-credito` | positivo | acepta | 0260 | V | tarjeta de crédito con procesadora (1603: en vivo el RUC del emisor de prueba) |
| DE-45 | `f12-cheque` | positivo | acepta | 0260 | V | cheque |
| DE-46 | `f12-cuatro-medios` | positivo | acepta | 0260 | V | cuatro medios de pago |
| DE-47 | `f12-pyg-pago-usd` | positivo | acepta | 0260 | V | PYG pagada en parte en USD |
| DE-46x | (`CasosFase12`) | variante | acepta · aviso JBS-E-30 | ? | N | exploratorio: los pagos no suman el total |
| S1-neg1 | (`CasosFase12`) | negativo | rechaza E620 | 1600 | — | tarjeta sin gPagTarCD |
| S1-neg2 | (`CasosFase12`) | negativo | rechaza E630 | 1650 | — | cheque sin gPagCheq |
| S1-neg3 | (`CasosFase12`) | variante | acepta | — | — | cheque de 6 dígitos: setdNumCheq completa con ceros hasta 8 (MT), acepta (I12-03) |
| S1-neg3b | (`CasosFase12`) | negativo | rechaza JBS-E-44 | — | — | cheque con letras |
| S1-neg4 | (`CasosFase12`) | negativo | rechaza E606 | 1553 | — | pago bancario (16) en una FE que no es operación bancaria |
| DE-19 | (`CasosDePrueba`) | existente | acepta | 0260 | V | crédito en tres cuotas |
| DE-48 | `f12-cuotas-con-entrega` | positivo | acepta | 0260 | V | cuotas con entrega inicial |
| DE-19x | (`CasosFase12`) | variante | acepta · aviso JBS-E-51 | ? | N | exploratorio: las cuotas no suman el saldo |
| S2-neg1 | (`CasosFase12`) | negativo | rechaza E650 | 1750 | — | cuotas sin gCuotas |
| S2-neg2 | (`CasosFase12`) | negativo | rechaza E644a | 1706 | — | plazo con dCuotas |
| S2-neg3 | (`CasosFase12`) | negativo | rechaza E643a | 1704 | — | cuotas con dPlazoCre |
| DE-12 | (`CasosDePrueba`) | existente | acepta | 0260? | V | solo exonerado |
| DE-49 | `f12-solo-exentos` | positivo | acepta | 0260 | V | solo exentos |
| DE-13 | (`CasosDePrueba`) | existente | acepta | 0260 | V | descuento por ítem y global (EA004 · 1862) |
| DE-50 | `f12-descuento-global-proporcional` | positivo | acepta | 0260 | V | descuento global proporcional con redondeo |
| DE-51 | `f12-item-bonificado` | positivo | acepta | 0260? | V | ítem bonificado al 100 % |
| DE-08 | (`CasosDePrueba`) | existente | acepta | 0260 | V | USD con cambio por ítem |
| DE-52 | `f12-eur-cambio-global` | positivo | acepta | 0260 | V | EUR cambio global |
| DE-53 | `f12-brl-cambio-por-item` | positivo | acepta | 0260 | V | BRL cambio por ítem |
| DE-54 | `f12-ars-cotizacion-4-decimales` | positivo | acepta | 0260 | V | ARS cotización de cuatro decimales |
| DE-55 | (`CasosFase12`) | grande | acepta | 0260 | V | 500 ítems |
| DE-56 | (`CasosFase12`) | grande | acepta | 0260 | V | 999 ítems |
| DE-56n | (`CasosFase12`) | negativo | rechaza JBS-E-57 | — | — | 1000 ítems |
| DE-56x | (`CasosFase12`) | grande | acepta | 0200? | N | exploratorio: 999 ítems de más de 1000 KB (M12-01); o corte de conexión sin respuesta (MT 12.3.1.1): resolve antes de inutilizar |
| DE-56s | (`CasosFase12`) | grande | acepta | 0260? | V | sonda de I12-23: 500 ítems con los textos largos de DE-56x (≈ 650 KB); solo si el reintento de DE-56 repite el 0160, en lugar de DE-56x (12.3.10) |
| DE-57 | `f12-nr-traslado-entre-locales` | positivo | acepta | 0260 | V | NR traslado entre locales |
| DE-58 | `f12-nr-consignacion` | positivo | acepta | 0260 | V | NR consignación |
| DE-59 | `f12-nr-exportacion` | positivo | acepta | 0260? | V | NR exportación (receptor B2F) |
| DE-60 | `f12-nr-motivo-otro` | positivo | acepta | 0260 | V | NR motivo Otro |
| S7-neg1 | (`CasosFase12`) | negativo | rechaza JBS-E-150 | — | — | NR de importación sin dNuDespImp |
| S7-neg2 | (`CasosFase12`) | negativo | rechaza E900 | 2100 | — | NR sin gTransp |
| S7-neg3 | (`CasosFase12`) | negativo | rechaza E501 | 2606 | — | traslado entre locales con otro receptor |
| DE-61 | `f12-nd-sobre-electronica` | positivo | acepta | 0260? | V | ND sobre FE electrónica (Q40; 1101-1106 si el timbrado no la habilita) |
| DE-22 | (`CasosDePrueba`) | existente | acepta | 0260? | V | ND sobre factura impresa (Q40, Q43) |
| DE-62 | `f12-comision` | positivo | acepta | 0260? | V | comisión con IVA incluido: `dComi` 110000, `dIVAComi` 10000.00 (`dComi`/11, I3-20), `dTotIVA` 110000.00, `dTotGralOpe` 1210000; 0260 confirma los valores, no la fórmula (F026 sin validación); 2371 posible (F017a: F015+F016, sin F026) |
| DE-63 | `f12-vehiculo-nuevo` | positivo | acepta | 0260? | — | vehículo nuevo (Q42: sin red); XSD publicado: patrón de dCilin con errata (I12-07) |
| DE-64 | `f12-energia` | positivo | acepta | 0260 | — | energía (Q42: sin red; I12-01) |
| DE-65 | `f12-seguros` | positivo | acepta | 0260 | — | seguros (Q42: sin red) |
| DE-66 | `f12-supermercado` | positivo | acepta | 0260 | — | supermercado (Q42: sin red) |
| DE-67 | `f12-ciclo-contrato` | positivo | acepta | 0260 | V | ciclo con contrato (Q42: en vivo) |
| DE-68 | `f12-rastreo-mercaderia` | positivo | acepta | 0260 | V | rastreo de mercadería (Q42: en vivo); `dNumReg` y `dNumRegEntCom` de 20 caracteres (I12-05; el XSD vigente admite 1-20); sin el importador, que la NT 010 eliminó (E12-08: el primer envío, 0001117, dio 0160) |
| S14-neg1 | (`CasosFase12`) | negativo | rechaza JBS-E-127 | — | — | seguros sin pólizas |
| S14-neg2 | (`CasosFase12`) | negativo | rechaza E822 | 2050 | — | ciclo sin fechas |
| S14-neg3 | (`CasosFase12`) | negativo | rechaza E823b | 2054 | — | fin del ciclo anterior al inicio |
| S14-neg4 | (`CasosFase12`) | negativo | rechaza JBS-E-105 | — | — | chasis de 16 caracteres |
| OP-20 | (`CasosFase12`) | plazo | acepta · aviso A004b | 1005? | V | dFecFirma y dFeEmiDE 73 h atrás (D56): aprobado con observación |
| OP-22 | (`CasosFase12`) | plazo | acepta · aviso D002 | 1150 | N | dFeEmiDE 31 días atrás; exige dFeIniT del timbrado anterior a la fecha de emisión atrasada (C004b · 1103, NT 001) |
| OP-23 | (`CasosFase12`) | plazo | acepta · aviso D002f | 1151 | — | dFeEmiDE 6 días en el futuro |
| EV-07 | (`CasosFase12`) | evento | acepta | 0600? | V0 | notificación de recepción (Q41); va primero sobre su FE (GEN001 · 4113); el XSD publicado no trae dTotalGs |
| EV-08 | (`CasosFase12`) | evento | acepta | 0600 | V0 | desconocimiento |
| EV-08c | — | operacion | — | 4156? | V0 | conformidad sobre la FE desconocida en EV-08 (GCO001; el punto 5 de la NT 019 la contradice) |
| EV-09 | (`CasosFase12`) | evento | acepta | 0600? | V0 | conformidad total después de EV-03; NT 019, GCO002: 4150 o 4158 si SIFEN admite una sola conformidad |
| EV-03 | (`CasosDePrueba`) | evento-existente | acepta | 0600 | V0 | conformidad parcial (GCO004 · 4154) |
| EV-04 | (`CasosDePrueba`) | evento-existente | acepta | 0600 | V0 | disconformidad; la conformidad que la corrige es EV-04c |
| EV-04c | — | operacion | — | 0600 | V0 | conformidad total que corrige la disconformidad de EV-04, sobre la misma FE (GCO002c: hasta 15 días) |
| OP-05b | (`CasosFase12`) | evento-negativo | rechaza GEI006 | 4067 | — | inutilización de más de 1000 números |
| OP-05c | (`CasosFase12`) | evento-negativo | rechaza GEI006a | 4068 | — | inutilización con el rango invertido |
| OP-01 | — | operacion | — | 4009 | V0 | cancelar 001-001-0001001 (aprobada el 2026-11-24 09:08) pasadas 48 h: desde el 2026-11-26 09:15; antes, 001-001-0000051 desde el 2026-11-25 21:35 |
| OP-02 | — | operacion | — | 4010 | V0 | cancelar la NC 001-001-0000060 fuera de plazo (168 h): desde el 2026-11-30, no bloquea (D56) |
| OP-03 | — | operacion | — | 4065 | V0 | inutilizar un número con un DE aprobado de la corrida (GEI005) |
| OP-04 | — | operacion | — | 4003 | V0 | cancelar 001-001-0001050, ya cancelada en §8.7 (GEC002b) |
| OP-05 | — | operacion | — | 4002 | — | cancelar un CDC inexistente: solo simulador |
| OP-06 | — | operacion | — | 0600? | V0 | el emisor cancela la FE que el receptor conformó (EV-03): la NT 025 quitó 4004 |
| OP-10 | — | operacion | — | 0300+0362 | V | lote de 5 FE: 4 válidas y 1 con dFeIniT cambiada (1107); el rechazado se inutiliza (D55) |
| OP-11 | — | operacion | — | 0300+0362 | V | lote de 50 FE mínimas, una sola vez y al final (Q46); queryDE de cinco al azar |
| OP-12 | — | operacion | — | — | — | lote de 51 (JBS-ENT-01), vacío (JBS-ENT-11) y mezclado (JBS-ENT-09): rechazo local sin pedidos |
| OP-21 | — | operacion | — | 1005? | V | DE-05 firmado en §12.3.1 y enviado 72 h después con readSigned (opcional, Q45; no bloquea) |
| C-neg1 | — | operacion | — | 1332 | N | organismo del Estado como B2B: solo si el usuario da el RUC (Q43; hoy no); en espera, no se envía |
| OP-30 | (`CasosFase12.cancelacionConNotas`: OP-30-FE, OP-30-NC) | operacion | acepta (FE y NC) | 0600? | V | FE propia (150.000) + NC parcial aprobada (50.000) y después cancel de la FE (pedido del usuario, 2026-11-25): el MT pide cancelar antes el último DTE asociado (§11.1, Tabla J) sin código de validación; exploratorio |
| OP-31 | (`CasosFase12.cancelacionConNotas`: OP-31-FE, OP-31-NC) | operacion | acepta (FE y NC) | 0600? | V | FE propia (80.000) + NC por el total aprobada y después cancel de la FE (ídem OP-30) |
| OP-32 | (`CasosFase12.cancelacionConNotas`: OP-32-FE, OP-32-NC) | operacion | acepta (FE y NC) | 2404 | V | FE propia (60.000) aprobada y cancelada, y después una NC sobre ella: H004b · 2404 (solo servidor); la NC rechazada se inutiliza con tipo 5 (D55) |

**DE-62** (`f12-comision`) fijaba E12-01 (la comisión no llegaba al XML); corregido el 2026-11-25 por decisión
del usuario, sale con `dComi 110000` y `dTotGralOpe 1210000`, `dIVAComi 10000.00` (`dComi`/11, IVA incluido, I3-20) y `dTotIVA 110000.00`. Los DE-55 y DE-56 (500 y 999 ítems) no tienen archivo: los arma
`CasosFase12.facturaGrande` y `CasosGrandesTest` fija la cantidad, el total y el digest.

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
| DE-13 | `factura-descuentos` | Factura (1) | Contado, B2B | PYG | Contribuyente | IVA 10 % | sí | objeto | — (`dPorcDescTotal` solo del descuento global) |
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

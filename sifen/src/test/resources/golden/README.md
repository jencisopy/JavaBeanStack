# Golden files de equivalencia con el fork (plan JBSSIF, §2.3, D8)

Cada archivo es la salida del **fork** `rshk-jsifenlib-2.0.0-SNAPSHOT` (el jar que corre en
producción, md5 `b818e7b9…`) para un caso del catálogo (`../casos/README.md`), en forma **C14N
exclusiva** (`javax.xml.crypto`, la misma que firma el DE), firma incluida: el certificado
autofirmado de `../certificados/` hace que RSA-SHA256 sea determinista (A3, verificada). Los
captura el arnés A/B que vive **fuera del repo** (`IA/Workspace-java/JavaBeanStack/inputs/jbs_sifen_insumos/equivalencia/`):
el jar del fork no entra al repo ni al CI. `GoldenXmlTest` genera lo mismo con `jbs-sifen` y exige
igualdad byte a byte.

| Archivo | Qué es |
|---|---|
| `DE-nn.xml` | Sobre SOAP `rEnviDe` de `generarXml` desde `casos/<archivo>.xml` (por objeto da lo mismo, salvo donde hay `.obj`) |
| `DE-06.obj.xml` | DE-06 armado por objeto: difería del releído porque el flag de redondeo se perdía al leer (E0-01); desde la Fase 4 (§4.2) es igual a `DE-06.xml`, como `DE-26.obj.xml` y `DE-28.obj.xml` |
| `EV-nn.xml` | Sobre `rEnviEventoDe` firmado |
| `LT-01.xml` | `rLoteDE` del zip del lote (el nombre de la entrada y la fecha del zip cambian en cada corrida y no se comparan) |

Son una sola línea (es la forma canónica). Para leerlos: `xmllint --format golden/DE-01.xml`.

## Lo que fijan tal cual (D10) y no es correcto

| Golden | Hallazgo | Qué se ve |
|---|---|---|
| ~~`DE-02`~~ | ~~E0-01, E0-03~~ | ~~El XML generado sin redondeo, releído, sale **con** redondeo (`dRedon 0.1`, `dTotGralOpe 8517.500`, `dTotalGs 49870303.2000000`) e idéntico byte a byte a `DE-04` (la versión aprobada): es la doble firma~~ — SUPERADO por §4.2 (2026-11-24): releído sale sin redondeo, igual a `DE-03` |
| ~~`DE-06` (`.xml`)~~ | ~~E0-01~~ | ~~Releído: `dRedon 39`; por objeto (`.obj`): `dRedon 0`~~ — SUPERADO por §4.2 (2026-11-24): releído `dRedon 0`, igual al `.obj` (también `DE-26` y `DE-28`) |
| ~~`DE-04`, `DE-07`, `DE-08`~~ | ~~E0-03~~ | ~~Escalas arrastradas en `dTotGralOpe`/`dTotalGs` (3 y 5–7 decimales)~~ — SUPERADO por §4.3 (2026-11-24): `dRedon` y `dTotGralOpe` con escala 2 en moneda extranjera (también `DE-11`, `DE-17`, `DE-24`, `DE-27`, `DE-29`); `dTotalGs` = `dTotGralOpe` × `dTiCam` exacto (escala 2 + la de `dTiCam`) |
| todos los PYG | E0-13 | `dTotOpeItem`, `dBasGravIVA`, `dLiqIVAItem`, `dSub10` con 2 decimales en guaraníes (desde la Fase 4 es la decisión D38 a, I3-41: SIFEN rechaza los enteros) |
| ~~`DE-14`~~ | ~~I0-13~~ | ~~`dTotAntItem` sin multiplicar por la cantidad~~ — corregido en §3.8 (E3-25) |
| ~~`DE-18`~~ | ~~E0-17~~ | ~~Ítem con `dTasaIVA 10.00`: `dSub10`, `dIVA10`, `dTotOpe` y `dTotGralOpe` en 0~~ — SUPERADO por §4.5 (2026-11-24): suma como `10` (`dSub10 110000.00`, `dIVA10 10000.00`, `dTotGralOpe 110000`) |
| ~~`EV-05`~~ | ~~E0-12~~ | ~~Ciudad del receptor nominado como `dDesCiuEnt`~~ — SUPERADO por §6.1 (2026-11-24): sale como `dDesCiuRec` (GENFE022) |
| todos | I0-14 | `dSisFact` (eliminado por la NT 010) se sigue escribiendo |

## Cómo se regeneran (y cuándo)

Solo cuando una fase **cambia el comportamiento a propósito** (Fase 4, 6 o 7; la Fase 9 no, el
renombrado de paquetes no puede cambiar un byte). Regla: cada golden que cambia lleva, en la
bitácora de la fase, el caso, el elemento que cambia, el valor antes y después y el código del
hallazgo o la decisión que lo justifica. Si el cambio viene de `jbs-sifen` y ya no del fork, el
golden nuevo se genera con `jbs-sifen` (no con el fork) y se dice en esa bitácora.

**Fase 4 (2026-11-24, carril A, §4.2/§4.3/§4.5)**: 13 golden regenerados con `jbs-sifen` —`DE-02`,
`DE-04`, `DE-06`, `DE-07`, `DE-08`, `DE-11`, `DE-17`, `DE-18`, `DE-24`, `DE-26`, `DE-27`, `DE-28`,
`DE-29`—; los `.obj`, `EV-nn` y `LT-01` no cambian. Antes, diff por elemento y justificación en
`jbs_sifen_insumos/fase4/golden/` (fuera del repo); las 21 salidas quedan en `Comparador.EXCLUIDOS`
del arnés A/B con su motivo.

**I4-11 (2026-11-24, decisión del usuario «Sí, escribir entero»)**: `DE-18` regenerado otra vez con `jbs-sifen`:
`dTasaIVA` `10.00` → `10` (el XSD define `tdTasaIVA` como `xs:integer`; la validación previa `JBS-E-92` ya no lo
frena). Antes y diff en `jbs_sifen_insumos/fase4/golden_i411/`.

```bash
cd IA/Workspace-java/JavaBeanStack/inputs/jbs_sifen_insumos/equivalencia
./correr.sh            # fork (dos veces) y jbs-sifen; compara en C14N; resultados en target/
./correr.sh congelar   # además copia la salida del fork a sifen/src/test/resources/golden/
./correr.sh entradas   # además regenera casos/<archivo>.xml de los casos por objeto (con el fork)
```

**Fase 6 (2026-11-24, §6.1)**: `EV-05` regenerado con `jbs-sifen` (`dDesCiuEnt` → `dDesCiuRec`, E0-12; con la firma de
consecuencia), excluido del A/B con su motivo; antes y diff en `jbs_sifen_insumos/fase6/golden/`. **`F6-01.xml`** es un
golden fuera del catálogo del A/B (el fork no tiene `gOblAfe` y `CasosDePrueba` se compila también contra él): factura
PYG de DE-05 con dos `gOblAfe` (NT 018), lo compara `NotasTecnicasFase6Test.goldenF601` y se congela con
`-Djbs.sifen.f6.congelar=<carpeta>`. La NT 13 ya no se puede apagar (§6.3, D25): todos los golden tenían el flag en
`true` y ninguno cambió por eso.

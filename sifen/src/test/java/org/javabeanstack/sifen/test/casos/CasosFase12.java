/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*        jenciso@javabeanstack.org
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 3 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
* MA 02110-1301  USA
 */
package org.javabeanstack.sifen.test.casos;

import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.EventosDE;
import org.javabeanstack.sifen.core.fields.request.de.*;
import org.javabeanstack.sifen.core.fields.request.event.*;
import org.javabeanstack.sifen.core.types.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Catálogo de la Fase 12 (plan JBSSIF, §12.1 y §12.2): batería ampliada de casos de facturación, con el
 * emisor y los receptores ficticios de los fixtures (D58).
 *
 * <p>Vive aparte de {@link CasosDePrueba} a propósito: el arnés A/B (fuera del repo, D8) compila
 * {@code CasosDePrueba.java} también contra el fork, que no tiene varios de los campos que estos casos usan
 * ({@code dCodConDncp}, los de la NT 010 en {@code gRasMerc}, la comisión, etc.), y los casos nuevos no entran
 * al A/B (D53), como el {@code F6-01} de la Fase 6. Su XML de referencia es <b>candidato</b>
 * ({@code golden/candidatos/}): pasa a aceptado cuando SIFEN-test lo aprueba (§12.3).</p>
 *
 * <p>Todos los casos traen las leyendas del ambiente de pruebas (Q14) en {@code dNomEmi} y en la descripción
 * del primer ítem, para que la validación previa no las mezcle con lo que cada caso prueba. Fechas,
 * {@code dCodSeg} y números son fijos (A3). Lo que se espera de cada caso (códigos de la validación previa y
 * {@code dCodRes} de SIFEN) está en {@code casos/fase12/esperados.tsv}.</p>
 */
public final class CasosFase12 {
    /** Leyenda del emisor en el ambiente de pruebas (Q14). */
    public static final String LEYENDA_EMISOR = "DE generado en ambiente de prueba - sin valor comercial ni fiscal";
    /** Leyenda del primer ítem en el ambiente de pruebas (Q14). */
    public static final String LEYENDA_ITEM = "DOCUMENTO ELECTRÓNICO SIN VALOR COMERCIAL NI FISCAL - GENERADO EN AMBIENTE DE PRUEBA";
    /** Descripción del documento del receptor del exterior (Q44: {@code iTipIDRec} 9 «Otro»). */
    public static final String IDENTIFICACION_TRIBUTARIA = "Identificación tributaria";
    /** Comisión del caso DE-62. */
    public static final BigDecimal COMISION = new BigDecimal("110000");

    private CasosFase12() {
    }

    /** Un caso de DE de la Fase 12: con {@code archivo} tiene XML de entrada y de referencia candidato. */
    public record Caso(String id, String archivo, String descripcion, String nota, Supplier<DocumentoElectronico> constructor) {
        /** @return DE armado por objeto (nuevo en cada llamada). */
        public DocumentoElectronico construir() {
            return constructor.get();
        }

        /** @return {@code true} si tiene XML de entrada y de referencia candidato. */
        public boolean tieneCandidato() {
            return archivo != null;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    /** Un caso de evento de la Fase 12. */
    public record CasoEvento(String id, String descripcion, String nota, Supplier<EventosDE> constructor) {
        public EventosDE construir() {
            return constructor.get();
        }

        @Override
        public String toString() {
            return id;
        }
    }

    // ------------------------------------------------------------------ catálogo

    /**
     * Casos positivos con XML de entrada y de referencia candidato, en orden (DE-55 y DE-56 no tienen archivo:
     * los arma {@link #facturaGrande(String, int, boolean)}).
     *
     * @return lista inmutable.
     */
    public static List<Caso> documentos() {
        List<Caso> l = new ArrayList<>();
        // Grupo A: identificación del receptor (los ID-F se arman desde BateriaIdentificacion)
        l.add(new Caso("ID-F08", "f12-b2f-cedula-extranjera", "B2F con cédula extranjera", "", () -> combinacion("ID-F08")));
        l.add(new Caso("ID-F09", "f12-b2f-carnet-residencia", "B2F con carnet de residencia", "", () -> combinacion("ID-F09")));
        l.add(new Caso("DE-41", "f12-nc-receptor-nominado", "NC a un receptor nominado sobre una FE innominada que ya se nominó (H004h, NT 010)", "",
                c(CasosFase12::ncReceptorNominado)));
        // Grupo B: notas de crédito, anticipos y autofactura
        l.add(new Caso("DE-39", "f12-nc-parcial-1", "NC parcial: devuelve el ítem al 10 % de DE-09", "", c(CasosFase12::ncParcial1)));
        l.add(new Caso("DE-39b", "f12-nc-parcial-2", "Segunda NC parcial: los ítems al 5 % y exento de DE-09", "", c(CasosFase12::ncParcial2)));
        l.add(new Caso("DE-40", "f12-nc-sobre-impresa", "NC sobre una factura impresa (H002 2, timbrado de 8 dígitos)", "", c(CasosFase12::ncSobreImpresa)));
        l.add(new Caso("DE-36", "f12-anticipo-usd", "Factura de anticipo en USD (iTipTra 9, cambio global)", "", c(CasosFase12::anticipoUsd)));
        l.add(new Caso("DE-37", "f12-final-anticipo-usd", "Factura final en USD que aplica el anticipo de DE-36 por ítem (cantidad 1)", "",
                c(CasosFase12::finalAnticipoUsd)));
        l.add(new Caso("DE-37b", "f12-final-anticipo-usd-cant2", "DE-37 con cantidad 2 (E3-25: dTotAntItem × cantidad)", "", c(CasosFase12::finalAnticipoUsdCantidad2)));
        l.add(new Caso("DE-38", "f12-nc-final-anticipo-usd", "NC en USD sobre la factura final con anticipo DE-37", "", c(CasosFase12::ncSobreFinalAnticipo)));
        l.add(new Caso("DE-42", "f12-autofactura-extranjero-pasaporte", "Autofactura a un vendedor extranjero con pasaporte", "", c(CasosFase12::autofacturaExtranjeroPasaporte)));
        l.add(new Caso("DE-43", "f12-autofactura-extranjero-cedula", "Autofactura a un vendedor extranjero con cédula extranjera", "", c(CasosFase12::autofacturaExtranjeroCedula)));
        // Grupo C: B2G
        l.add(new Caso("DE-33", "f12-b2g-sin-gcomppub", "B2G sin gCompPub, con dCodConDncp en gGrupAdi (NT 020, NT 026)", "", c(CasosFase12::b2gSinCompPub)));
        // Grupo D: exportación (D57)
        l.add(new Caso("DE-34", "f12-exportacion-bienes", "Exportación de bienes: FE común B2F exenta, dInfoFisc de la NT 007, gTransp fluvial FOB y gCamCarg", "",
                c(CasosFase12::exportacionBienes)));
        l.add(new Caso("DE-35", "f12-exportacion-servicios", "Exportación de servicios: FE común B2F exenta, contado por transferencia en USD", "",
                c(CasosFase12::exportacionServicios)));
        // S1: condición de venta
        l.add(new Caso("DE-44", "f12-tarjeta-credito", "Contado con tarjeta de crédito y procesadora (gPagTarCD)", "", c(CasosFase12::tarjetaCredito)));
        l.add(new Caso("DE-45", "f12-cheque", "Contado con cheque (gPagCheq)", "", c(CasosFase12::cheque)));
        l.add(new Caso("DE-46", "f12-cuatro-medios", "Contado con cuatro medios: efectivo, débito, transferencia y billetera", "", c(CasosFase12::cuatroMedios)));
        l.add(new Caso("DE-47", "f12-pyg-pago-usd", "Factura en guaraníes pagada en parte en USD", "", c(CasosFase12::pygPagadaEnUsd)));
        // S2: crédito
        l.add(new Caso("DE-48", "f12-cuotas-con-entrega", "Crédito en dos cuotas con entrega inicial", "", c(CasosFase12::cuotasConEntrega)));
        // S3: impuestos
        l.add(new Caso("DE-49", "f12-solo-exentos", "Factura con solo ítems exentos", "", c(CasosFase12::soloExentos)));
        // S4: descuentos
        l.add(new Caso("DE-50", "f12-descuento-global-proporcional", "Descuento global del 10 % en todos los ítems, con redondeo", "", c(CasosFase12::descuentoGlobalProporcional)));
        l.add(new Caso("DE-51", "f12-item-bonificado", "Ítem bonificado con descuento del 100 %", "", c(CasosFase12::itemBonificado)));
        // S5: monedas
        l.add(new Caso("DE-52", "f12-eur-cambio-global", "EUR con tipo de cambio global", "", c(CasosFase12::eurCambioGlobal)));
        l.add(new Caso("DE-53", "f12-brl-cambio-por-item", "BRL con tipo de cambio por ítem", "", c(CasosFase12::brlCambioPorItem)));
        l.add(new Caso("DE-54", "f12-ars-cotizacion-4-decimales", "ARS con cotización menor que 10 y cuatro decimales", "", c(CasosFase12::arsCuatroDecimales)));
        // S7: remisiones
        l.add(new Caso("DE-57", "f12-nr-traslado-entre-locales", "Remisión por traslado entre locales (receptor = emisor)", "", c(CasosFase12::nrEntreLocales)));
        l.add(new Caso("DE-58", "f12-nr-consignacion", "Remisión por consignación", "", c(CasosFase12::nrConsignacion)));
        l.add(new Caso("DE-59", "f12-nr-exportacion", "Remisión por exportación a un receptor del exterior", "", c(CasosFase12::nrExportacion)));
        l.add(new Caso("DE-60", "f12-nr-motivo-otro", "Remisión con motivo «Otro» y texto libre", "", c(CasosFase12::nrMotivoOtro)));
        // S8: nota de débito
        l.add(new Caso("DE-61", "f12-nd-sobre-electronica", "Nota de débito sobre una FE electrónica", "", c(CasosFase12::ndSobreElectronica)));
        // S13: comisión
        l.add(new Caso("DE-62", "f12-comision", "Factura con comisión (dComi, dIVAComi)",
                "E12-01 corregido (2026-11-25): la comisión llega al XML y suma al total; dIVAComi = dComi/11, IVA incluido (I3-20)", c(CasosFase12::comision)));
        // S14: sectores (D57)
        l.add(new Caso("DE-63", "f12-vehiculo-nuevo", "Vehículo nuevo (gVehNuevo)", "", c(CasosFase12::vehiculoNuevo)));
        l.add(new Caso("DE-64", "f12-energia", "Energía eléctrica (gGrupEner)", "", c(CasosFase12::energia)));
        l.add(new Caso("DE-65", "f12-seguros", "Seguros (gGrupSeg con una póliza)", "", c(CasosFase12::seguros)));
        l.add(new Caso("DE-66", "f12-supermercado", "Supermercado (gGrupSup)", "", c(CasosFase12::supermercado)));
        l.add(new Caso("DE-67", "f12-ciclo-contrato", "Servicio cíclico con contrato (gGrupAdi, iIndPres 6)", "", c(CasosFase12::cicloContrato)));
        l.add(new Caso("DE-68", "f12-rastreo-mercaderia", "Rastreo de la mercadería (gRasMerc con los campos de la NT 010)", "", c(CasosFase12::rastreo)));
        return Collections.unmodifiableList(l);
    }

    /**
     * Todos los DE que el archivo de esperados nombra: los de {@link #documentos()}, las variantes, los negativos,
     * los grandes y los casos de fecha; por ID.
     *
     * @return mapa inmutable, en orden.
     */
    public static Map<String, Supplier<DocumentoElectronico>> todos() {
        Map<String, Supplier<DocumentoElectronico>> m = new LinkedHashMap<>();
        for (Caso k : documentos()) {
            m.put(k.id(), k.constructor());
        }
        // ---- variantes (§12.1)
        m.put("DE-39c", c(CasosFase12::ncParcial3));
        m.put("DE-37n", c(CasosFase12::finalPygConAnticipoUsd));
        m.put("DE-46x", c(() -> conCambio(cuatroMedios(), de -> pagos(de).get(3).setdMonTiPag(bd("20000")))));
        m.put("DE-19x", c(() -> conCambio(CasosDePrueba.facturaCreditoCuotas(),
                de -> de.getgDtipDE().getgCamCond().getgPagCred().getgCuotasList().forEach(q -> q.setdMonCuota(bd("90000"))))));
        m.put("DE-55", () -> facturaGrande("0000055", 500, false));
        m.put("DE-56", () -> facturaGrande("0000056", 999, false));
        m.put("DE-56n", () -> facturaGrande("0000156", 1000, false));
        m.put("DE-56x", () -> facturaGrande("0000256", 999, true));
        // I12-23 (decisión del usuario, 2026-11-25): sonda de 500 ítems con los textos largos de DE-56x (≈ 650 KB, el peso
        // de DE-56 con la mitad de los ítems); se envía en 12.3.10 en lugar de DE-56x solo si el reintento de DE-56 repite
        // el 0160 genérico. Aprueba: el límite práctico de SIFEN-test es por cantidad de ítems; 0160: por bytes
        m.put("DE-56s", () -> facturaGrande("0000356", 500, true));
        // ---- B-neg
        m.put("B-neg1", c(() -> conCambio(ncSobreImpresa(), de -> de.getgCamDEAsocList().get(0).setdNTimDI(null))));
        m.put("B-neg2", c(() -> conCambio(finalAnticipoUsd(), de -> de.getgDtipDE().getgCamItemList().get(0).setdCDCAnticipo(null))));
        m.put("B-neg3", c(() -> conCambio(autofacturaExtranjeroPasaporte(), de -> de.setgCamDEAsocList(null))));
        m.put("B-neg4", c(() -> conCambio(autofacturaExtranjeroPasaporte(), de -> {
            TgCamDEAsoc a = de.getgCamDEAsocList().get(0);
            a.setiTipDocAso(TiTipDocAso.ELECTRONICO);
            a.setiTipCons(null);
            a.setdCdCDERef(CasosDePrueba.CDC_FACTURA_SIMPLE);
        })));
        m.put("B-neg5", c(() -> conCambio(autofacturaExtranjeroPasaporte(), de -> credito(de, "30 dias"))));
        // ---- C-neg2
        m.put("C-neg2a", c(() -> conCambio(CasosDePrueba.facturaB2g(), de -> de.getgDtipDE().getgCamFE().getgCompPub().setdFeCodCont(LocalDate.of(2026, 11, 24)))));
        m.put("C-neg2b", c(() -> conCambio(CasosDePrueba.facturaB2g(), de -> de.getgDtipDE().getgCamFE().getgCompPub().setdFeCodCont(LocalDate.of(2026, 11, 23)))));
        // setdDncpG completa con ceros a la izquierda hasta 8 (I6-09): un código corto no es negativo, uno largo sí
        m.put("C-neg2c", c(() -> conCambio(CasosDePrueba.facturaB2g(), de -> de.getgDtipDE().getgCamItemList().get(0).setdDncpG("123456789"))));
        m.put("C-neg2d", c(() -> conCambio(CasosDePrueba.facturaB2g(), de -> de.getgDtipDE().getgCamItemList().get(0).setdDncpE("12"))));
        // ---- D-neg
        m.put("D-neg1", c(() -> conCambio(exportacionBienes(), de -> de.getgDatGralOpe().getgDatRec().setdDirRec(null))));
        m.put("D-neg2", c(() -> conCambio(exportacionBienes(), de -> de.getgDatGralOpe().getgDatRec().setcPaisRec(PaisType.PRY))));
        m.put("D-neg3", c(() -> conCambio(exportacionBienes(), de -> de.getgDtipDE().getgTransp().setiModTrans(null))));
        m.put("D-neg4", c(() -> conCambio(exportacionBienes(), de -> de.getgDtipDE().getgTransp().setiRespFlete(null))));
        // ---- S1-neg
        m.put("S1-neg1", c(() -> conCambio(tarjetaCredito(), de -> pagos(de).get(0).setgPagTarCD(null))));
        m.put("S1-neg2", c(() -> conCambio(cheque(), de -> pagos(de).get(0).setgPagCheq(null))));
        // setdNumCheq completa con ceros hasta 8 (MT: «completar con 0 a la izquierda»): 6 dígitos no es negativo
        m.put("S1-neg3", c(() -> conCambio(cheque(), de -> pagos(de).get(0).getgPagCheq().setdNumCheq("123456"))));
        m.put("S1-neg3b", c(() -> conCambio(cheque(), de -> pagos(de).get(0).getgPagCheq().setdNumCheq("12AB5678"))));
        m.put("S1-neg4", c(() -> conCambio(contadoSimple("0000144", TiTiPago.PAGO_BANCARIO), de -> { })));
        // ---- S2-neg
        m.put("S2-neg1", c(() -> conCambio(CasosDePrueba.facturaCreditoCuotas(), de -> de.getgDtipDE().getgCamCond().getgPagCred().setgCuotasList(null))));
        m.put("S2-neg2", c(() -> conCambio(CasosDePrueba.facturaUsdCreditoPlazo(), de -> de.getgDtipDE().getgCamCond().getgPagCred().setdCuotas((short) 3))));
        m.put("S2-neg3", c(() -> conCambio(CasosDePrueba.facturaCreditoCuotas(), de -> de.getgDtipDE().getgCamCond().getgPagCred().setdPlazoCre("30 dias"))));
        // ---- S7-neg
        m.put("S7-neg1", c(() -> conCambio(nrConsignacion(), de -> de.getgDtipDE().getgCamNRE().setiMotEmiNR(TiMotivTras.IMPORTACION))));
        m.put("S7-neg2", c(() -> conCambio(nrConsignacion(), de -> de.getgDtipDE().setgTransp(null))));
        m.put("S7-neg3", c(() -> conCambio(nrEntreLocales(), de -> {
            TgDatRec r = de.getgDatGralOpe().getgDatRec();
            r.setdRucRec("80654321");
            r.setdDVRec((short) 3);
            r.setdNomRec("CLIENTE FICTICIO S.A.");
        })));
        // ---- S14-neg
        m.put("S14-neg1", c(() -> conCambio(seguros(), de -> de.getgDtipDE().getgCamEsp().getgGrupSeg().setgGrupPolSegList(null))));
        m.put("S14-neg2", c(() -> conCambio(cicloContrato(), de -> {
            de.getgDtipDE().getgCamEsp().getgGrupAdi().setdFecIniC(null);
            de.getgDtipDE().getgCamEsp().getgGrupAdi().setdFecFinC(null);
        })));
        m.put("S14-neg3", c(() -> conCambio(cicloContrato(), de -> de.getgDtipDE().getgCamEsp().getgGrupAdi().setdFecFinC(LocalDate.of(2026, 10, 31)))));
        m.put("S14-neg4", c(() -> conCambio(vehiculoNuevo(), de -> de.getgDtipDE().getgCamItemList().get(0).getgVehNuevo().setdChasis("ABCDEFGH12345678"))));
        // ---- S12: plazos (el reloj de la validación es FECHA_FIRMA + 5 s)
        m.put("OP-20", c(() -> conCambio(CasosDePrueba.facturaPygB2cCedula(), de -> {
            de.setdFecFirma(CasosDePrueba.FECHA_FIRMA.minusHours(73));
            de.getgDatGralOpe().setdFeEmiDE(CasosDePrueba.FECHA_EMISION.minusHours(73));
        })));
        m.put("OP-22", c(() -> conCambio(CasosDePrueba.facturaPygB2cCedula(), de -> de.getgDatGralOpe().setdFeEmiDE(CasosDePrueba.FECHA_EMISION.minusDays(31)))));
        m.put("OP-23", c(() -> conCambio(CasosDePrueba.facturaPygB2cCedula(), de -> de.getgDatGralOpe().setdFeEmiDE(CasosDePrueba.FECHA_EMISION.plusDays(6)))));
        return Collections.unmodifiableMap(m);
    }

    /**
     * Eventos de la Fase 12 con XML de referencia candidato: EV-07 a EV-09.
     *
     * @return lista inmutable.
     */
    public static List<CasoEvento> eventos() {
        List<CasoEvento> l = new ArrayList<>();
        l.add(new CasoEvento("EV-07", "Notificación de recepción (rGeVeNotRec, NT 019)", "", CasosFase12::notificacionRecepcion));
        l.add(new CasoEvento("EV-08", "Desconocimiento del DE (rGeVeDescon)", "", CasosFase12::desconocimiento));
        l.add(new CasoEvento("EV-09", "Conformidad total (rGeVeConf, iTipConf 1)", "", CasosFase12::conformidadTotal));
        return Collections.unmodifiableList(l);
    }

    /**
     * Eventos que la validación previa rechaza (OP-05, sin número de SIFEN).
     *
     * @return mapa ID → evento.
     */
    public static Map<String, Supplier<EventosDE>> eventosNegativos() {
        Map<String, Supplier<EventosDE>> m = new LinkedHashMap<>();
        m.put("OP-05b", () -> inutilizacion("0000001", "0001002"));
        m.put("OP-05c", () -> inutilizacion("0000055", "0000050"));
        return Collections.unmodifiableMap(m);
    }

    // ------------------------------------------------------------------ OP-30 a OP-32 (pedido del usuario, 2026-11-25)

    /**
     * Documentos de OP-30, OP-31 y OP-32 (S9: cancelar una FE que tiene NC aprobadas, y la NC sobre una FE cancelada).
     * Cada operación tiene su FE y su NC propias y exclusivas: ningún otro caso de la batería las cita, y no son las
     * cadenas de DE-16/ID-I06, DE-41, DE-61, R1–R3 ni EV-07. No están en {@link #todos()} porque en los esperados son
     * filas de operación (como OP-10), no documentos sueltos; la validación previa de cada uno la prueba
     * {@code BateriaFacturacionSimulatorTest}. Claves: {@code OP-30-FE}, {@code OP-30-NC}, {@code OP-31-FE},
     * {@code OP-31-NC}, {@code OP-32-FE} y {@code OP-32-NC}; la NC de cada una asocia el CDC de su FE del catálogo (en
     * vivo, el de la FE aprobada en la corrida).
     *
     * @return mapa inmutable, en orden.
     */
    public static Map<String, Supplier<DocumentoElectronico>> cancelacionConNotas() {
        Map<String, Supplier<DocumentoElectronico>> m = new LinkedHashMap<>();
        m.put("OP-30-FE", c(CasosFase12::op30Factura));
        m.put("OP-30-NC", c(CasosFase12::op30NotaParcial));
        m.put("OP-31-FE", c(CasosFase12::op31Factura));
        m.put("OP-31-NC", c(CasosFase12::op31NotaTotal));
        m.put("OP-32-FE", c(CasosFase12::op32Factura));
        m.put("OP-32-NC", c(CasosFase12::op32Nota));
        return Collections.unmodifiableMap(m);
    }

    /** OP-30: FE de dos ítems al 10 % (100.000 + 50.000 = 150.000), contado en efectivo. */
    static DocumentoElectronico op30Factura() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000300", CMondT.PYG, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-3001", "PRODUCTO DE PRUEBA 3001", "1", "100000"));
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-3002", "PRODUCTO DE PRUEBA 3002", "1", "50000"));
        contado(de, pago(TiTiPago.EFECTIVO, "150000", CMondT.PYG, null));
        return de;
    }

    /** OP-30: NC parcial sobre la FE de OP-30: devuelve el segundo ítem (50.000 de 150.000). */
    static DocumentoElectronico op30NotaParcial() {
        DocumentoElectronico de = notaCredito("0000301", CMondT.PYG, receptorContribuyente(TiTiOpe.B2B), cdc(leyendas(op30Factura())));
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-3002", "PRODUCTO DE PRUEBA 3002", "1", "50000"));
        return de;
    }

    /** OP-31: FE de un ítem al 10 % (80.000), contado en efectivo. */
    static DocumentoElectronico op31Factura() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000310", CMondT.PYG, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-3101", "PRODUCTO DE PRUEBA 3101", "1", "80000"));
        contado(de, pago(TiTiPago.EFECTIVO, "80000", CMondT.PYG, null));
        return de;
    }

    /** OP-31: NC por el total de la FE de OP-31 (80.000: la FE queda ajustada en su totalidad). */
    static DocumentoElectronico op31NotaTotal() {
        DocumentoElectronico de = notaCredito("0000311", CMondT.PYG, receptorContribuyente(TiTiOpe.B2B), cdc(leyendas(op31Factura())));
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-3101", "PRODUCTO DE PRUEBA 3101", "1", "80000"));
        return de;
    }

    /** OP-32: FE de un ítem al 10 % (60.000), contado en efectivo; se cancela antes de emitir su NC. */
    static DocumentoElectronico op32Factura() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000320", CMondT.PYG, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-3201", "PRODUCTO DE PRUEBA 3201", "1", "60000"));
        contado(de, pago(TiTiPago.EFECTIVO, "60000", CMondT.PYG, null));
        return de;
    }

    /** OP-32: NC sobre la FE de OP-32 ya cancelada (H004b · 2404, solo servidor: la librería no conoce el estado). */
    static DocumentoElectronico op32Nota() {
        DocumentoElectronico de = notaCredito("0000321", CMondT.PYG, receptorContribuyente(TiTiOpe.B2B), cdc(leyendas(op32Factura())));
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-3201", "PRODUCTO DE PRUEBA 3201", "1", "60000"));
        return de;
    }

    // ------------------------------------------------------------------ apoyo

    private static Supplier<DocumentoElectronico> c(Supplier<DocumentoElectronico> s) {
        return () -> leyendas(s.get());
    }

    private static DocumentoElectronico conCambio(DocumentoElectronico de, Consumer<DocumentoElectronico> cambio) {
        cambio.accept(de);
        return de;
    }

    /**
     * Leyendas del ambiente de pruebas (Q14) en el emisor y en el primer ítem.
     *
     * @param de documento a modificar.
     * @return el mismo documento.
     */
    public static DocumentoElectronico leyendas(DocumentoElectronico de) {
        de.getgDatGralOpe().getgEmis().setdNomEmi(LEYENDA_EMISOR);
        if (de.getgDtipDE().getgCamItemList() != null && !de.getgDtipDE().getgCamItemList().isEmpty()) {
            de.getgDtipDE().getgCamItemList().get(0).setdDesProSer(LEYENDA_ITEM);
        }
        return de;
    }

    private static DocumentoElectronico combinacion(String id) {
        return BateriaIdentificacion.combinaciones().stream().filter(k -> k.id().equals(id)).findFirst().orElseThrow().construir();
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static String cdc(DocumentoElectronico de) {
        try {
            return de.obtenerCDC();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** CDC del caso de la Fase 12 {@code id} (documento asociado de las NC, anticipos y eventos). */
    public static String cdcDe(String id) {
        return cdc(todos().get(id).get());
    }

    private static List<TgPaConEIni> pagos(DocumentoElectronico de) {
        return de.getgDtipDE().getgCamCond().getgPaConEIniList();
    }

    private static TgDatRec receptorContribuyente(TiTiOpe tipoOperacion) {
        TgDatRec r = new TgDatRec();
        r.setiNatRec(TiNatRec.CONTRIBUYENTE);
        r.setiTiOpe(tipoOperacion);
        r.setcPaisRec(PaisType.PRY);
        r.setiTiContRec(TiTipCont.PERSONA_JURIDICA);
        r.setdRucRec("80654321");
        r.setdDVRec((short) 3);
        r.setdNomRec("CLIENTE FICTICIO S.A.");
        r.setdDirRec("RUTA FICTICIA KM 1");
        r.setdNumCasRec(0);
        r.setcDepRec(TDepartamento.CAPITAL);
        r.setcCiuRec(1);
        r.setdDesCiuRec("ASUNCION (DISTRITO)");
        r.setdEmailRec("recepcion@example.com");
        return r;
    }

    private static TgDatRec receptorCedula() {
        TgDatRec r = new TgDatRec();
        r.setiNatRec(TiNatRec.NO_CONTRIBUYENTE);
        r.setiTiOpe(TiTiOpe.B2C);
        r.setcPaisRec(PaisType.PRY);
        r.setiTipIDRec(TiTipDocRec.CEDULA_PARAGUAYA);
        r.setdNumIDRec("1234567");
        r.setdNomRec("PERSONA FICTICIA");
        return r;
    }

    /** Receptor del exterior de la exportación (B2F, Brasil, «Otro» con «Identificación tributaria», Q44). */
    private static TgDatRec receptorExterior() {
        TgDatRec r = new TgDatRec();
        r.setiNatRec(TiNatRec.NO_CONTRIBUYENTE);
        r.setiTiOpe(TiTiOpe.B2F);
        r.setcPaisRec(PaisType.BRA);
        r.setiTipIDRec(TiTipDocRec.OTRO);
        r.setdDTipIDRec(IDENTIFICACION_TRIBUTARIA);
        r.setdNumIDRec("12345678000190");
        r.setdNomRec("IMPORTADORA FICTICIA LTDA");
        r.setdDirRec("RUA FICTICIA 100");
        r.setdNumCasRec(100);
        r.setdEmailRec("compras@example.com");
        return r;
    }

    /** El emisor ficticio como receptor (remisión entre locales). */
    private static TgDatRec receptorEmisor() {
        TgDatRec r = receptorContribuyente(TiTiOpe.B2B);
        r.setdRucRec(CasosDePrueba.RUC_EMISOR);
        r.setdDVRec(Short.parseShort(CasosDePrueba.DV_EMISOR));
        r.setdNomRec("EMPRESA FICTICIA DE PRUEBA S.A.");
        return r;
    }

    private static DocumentoElectronico base(TTiDE tipo, String numDoc, CMondT moneda, TTipTra tipoTransaccion, TgDatRec receptor) {
        DocumentoElectronico de = new DocumentoElectronico();
        de.setdFecFirma(CasosDePrueba.FECHA_FIRMA);
        de.setdSisFact((short) 1);

        TgOpeDE ope = new TgOpeDE();
        ope.setiTipEmi(TTipEmi.NORMAL);
        ope.setdCodSeg("00" + numDoc);
        de.setgOpeDE(ope);

        TgTimb timb = new TgTimb();
        timb.setiTiDE(tipo);
        timb.setdNumTim(CasosDePrueba.TIMBRADO);
        timb.setdEst("001");
        timb.setdPunExp("001");
        timb.setdNumDoc(numDoc);
        timb.setdFeIniT(CasosDePrueba.INICIO_TIMBRADO);
        de.setgTimb(timb);

        TdDatGralOpe gral = new TdDatGralOpe();
        gral.setdFeEmiDE(CasosDePrueba.FECHA_EMISION);
        if (tipo != TTiDE.NOTA_DE_REMISION_ELECTRONICA) {
            TgOpeCom com = new TgOpeCom();
            com.setiTipTra(tipoTransaccion);
            com.setiTImp(TTImp.IVA);
            com.setcMoneOpe(moneda);
            gral.setgOpeCom(com);
        }
        gral.setgEmis(CasosDePrueba.emisor());
        gral.setgDatRec(receptor);
        de.setgDatGralOpe(gral);

        TgDtipDE dtip = new TgDtipDE();
        if (tipo == TTiDE.FACTURA_ELECTRONICA) {
            TgCamFE fe = new TgCamFE();
            fe.setiIndPres(TiIndPres.OPERACION_PRESENCIAL);
            dtip.setgCamFE(fe);
        }
        dtip.setgCamItemList(new ArrayList<>());
        de.setgDtipDE(dtip);
        de.setgTotSub(new TgTotSub());
        return de;
    }

    private static void cambioGlobal(DocumentoElectronico de, String tipoCambio) {
        de.getgDatGralOpe().getgOpeCom().setdCondTiCam(TdCondTiCam.GLOBAL);
        de.getgDatGralOpe().getgOpeCom().setdTiCam(bd(tipoCambio));
    }

    private static TgPaConEIni pago(TiTiPago tipo, String monto, CMondT moneda, String tipoCambio) {
        TgPaConEIni p = new TgPaConEIni();
        p.setiTiPago(tipo);
        p.setdMonTiPag(bd(monto));
        p.setcMoneTiPag(moneda);
        if (tipoCambio != null) {
            p.setdTiCamTiPag(bd(tipoCambio));
        }
        return p;
    }

    private static void contado(DocumentoElectronico de, TgPaConEIni... pagos) {
        TgCamCond cond = new TgCamCond();
        cond.setiCondOpe(TiCondOpe.CONTADO);
        List<TgPaConEIni> l = new ArrayList<>();
        Collections.addAll(l, pagos);
        cond.setgPaConEIniList(l);
        de.getgDtipDE().setgCamCond(cond);
    }

    private static void credito(DocumentoElectronico de, String plazo) {
        TgCamCond cond = new TgCamCond();
        cond.setiCondOpe(TiCondOpe.CREDITO);
        TgPagCred cred = new TgPagCred();
        cred.setiCondCred(TiCondCred.PLAZO);
        cred.setdPlazoCre(plazo);
        cond.setgPagCred(cred);
        de.getgDtipDE().setgCamCond(cond);
    }

    /** Ítem con valores; {@code dDescItem} y {@code dDescGloItem} van siempre (E2-03). */
    private static TgCamItem item(String codigo, String descripcion, String cantidad, String precio,
                                  TiAfecIVA afectacion, String proporcion, String tasa) {
        TgCamItem it = new TgCamItem();
        it.setdCodInt(codigo);
        it.setdDesProSer(descripcion);
        it.setcUniMed(TcUniMed.UNI);
        it.setdCantProSer(bd(cantidad));
        TgValorItem valor = new TgValorItem();
        valor.setdPUniProSer(bd(precio));
        TgValorRestaItem resta = new TgValorRestaItem();
        resta.setdDescItem(BigDecimal.ZERO);
        resta.setdDescGloItem(BigDecimal.ZERO);
        valor.setgValorRestaItem(resta);
        it.setgValorItem(valor);
        if (afectacion != null) {
            TgCamIVA iva = new TgCamIVA();
            iva.setiAfecIVA(afectacion);
            iva.setdPropIVA(bd(proporcion));
            iva.setdTasaIVA(bd(tasa));
            it.setgCamIVA(iva);
        }
        return it;
    }

    private static TgCamItem gravado10(String codigo, String descripcion, String cantidad, String precio) {
        return item(codigo, descripcion, cantidad, precio, TiAfecIVA.GRAVADO, "100", "10");
    }

    private static TgCamItem exento(String codigo, String descripcion, String cantidad, String precio) {
        return item(codigo, descripcion, cantidad, precio, TiAfecIVA.EXENTO, "0", "0");
    }

    private static void asociarElectronico(DocumentoElectronico de, String cdc) {
        TgCamDEAsoc a = new TgCamDEAsoc();
        a.setiTipDocAso(TiTipDocAso.ELECTRONICO);
        a.setdCdCDERef(cdc);
        List<TgCamDEAsoc> l = new ArrayList<>();
        l.add(a);
        de.setgCamDEAsocList(l);
    }

    private static DocumentoElectronico notaCredito(String numDoc, CMondT moneda, TgDatRec receptor, String cdcAsociado) {
        DocumentoElectronico de = base(TTiDE.NOTA_DE_CREDITO_ELECTRONICA, numDoc, moneda, null, receptor);
        TgCamNCDE nc = new TgCamNCDE();
        nc.setiMotEmi(TiMotEmi.DEVOLUCION);
        de.getgDtipDE().setgCamNCDE(nc);
        asociarElectronico(de, cdcAsociado);
        return de;
    }

    // ------------------------------------------------------------------ grupo A

    /** DE-41: NC a la persona nominada (EV-05) sobre la FE innominada DE-16. */
    static DocumentoElectronico ncReceptorNominado() {
        DocumentoElectronico de = notaCredito("0000041", CMondT.PYG, receptorCedula(), cdc(CasosDePrueba.facturaInnominado()));
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-0161", "PRODUCTO DE PRUEBA 161", "1", "35000"));
        return de;
    }

    // ------------------------------------------------------------------ grupo B

    /** DE-39: devuelve el ítem al 10 % de DE-09 (110.000 de 245.000). */
    static DocumentoElectronico ncParcial1() {
        DocumentoElectronico de = notaCredito("0000039", CMondT.PYG, receptorContribuyente(TiTiOpe.B2B), cdc(CasosDePrueba.facturaIvaMixto()));
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-0091", "PRODUCTO DE PRUEBA 91", "1", "110000"));
        return de;
    }

    /** DE-39b: devuelve los ítems al 5 % y exento de DE-09 (135.000): con DE-39 suman el total. */
    static DocumentoElectronico ncParcial2() {
        DocumentoElectronico de = notaCredito("0000139", CMondT.PYG, receptorContribuyente(TiTiOpe.B2B), cdc(CasosDePrueba.facturaIvaMixto()));
        List<TgCamItem> items = de.getgDtipDE().getgCamItemList();
        items.add(item("ITEM-0092", "PRODUCTO DE PRUEBA 92", "2", "52500", TiAfecIVA.GRAVADO, "100", "5"));
        items.add(exento("ITEM-0093", "PRODUCTO DE PRUEBA 93", "1", "30000"));
        return de;
    }

    /** DE-39c: tercera NC (otra vez el ítem al 10 %): con DE-39 y DE-39b supera el total; solo SIFEN lo ve (2417). */
    static DocumentoElectronico ncParcial3() {
        DocumentoElectronico de = ncParcial1();
        de.getgTimb().setdNumDoc("0000239");
        de.getgOpeDE().setdCodSeg("000000239");
        return de;
    }

    /** DE-40: NC sobre una factura impresa (timbrado preimpreso ficticio, distinto del electrónico). */
    static DocumentoElectronico ncSobreImpresa() {
        DocumentoElectronico de = base(TTiDE.NOTA_DE_CREDITO_ELECTRONICA, "0000040", CMondT.PYG, null, receptorContribuyente(TiTiOpe.B2B));
        TgCamNCDE nc = new TgCamNCDE();
        nc.setiMotEmi(TiMotEmi.DEVOLUCION);
        de.getgDtipDE().setgCamNCDE(nc);
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-0401", "PRODUCTO DE PRUEBA 401", "1", "44000"));
        TgCamDEAsoc a = new TgCamDEAsoc();
        a.setiTipDocAso(TiTipDocAso.IMPRESO);
        a.setdNTimDI("12345679");
        a.setdEstDocAso("001");
        a.setdPExpDocAso("001");
        a.setdNumDocAso("0000100");
        a.setiTipoDocAso(TiTIpoDoc.FACTURA);
        a.setdFecEmiDI(LocalDate.of(2026, 10, 15));
        List<TgCamDEAsoc> l = new ArrayList<>();
        l.add(a);
        de.setgCamDEAsocList(l);
        return de;
    }

    /** DE-36: factura de anticipo en USD, 1.000,00 al 10 %, cambio global. */
    static DocumentoElectronico anticipoUsd() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000036", CMondT.USD, TTipTra.ANTICIPO, receptorContribuyente(TiTiOpe.B2B));
        cambioGlobal(de, "7300.25");
        de.getgDtipDE().getgCamItemList().add(gravado10("SERV-036", "ANTICIPO DE SERVICIO", "1", "1000.00"));
        contado(de, pago(TiTiPago.EFECTIVO, "1000.00", CMondT.USD, "7300.25"));
        return de;
    }

    /** DE-37: factura final en USD, 2.500,00 con el anticipo de DE-36 (1.000,00) aplicado por ítem. */
    static DocumentoElectronico finalAnticipoUsd() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000037", CMondT.USD, TTipTra.PRESTACION_SERVICIOS, receptorContribuyente(TiTiOpe.B2B));
        cambioGlobal(de, "7300.25");
        de.getgDatGralOpe().getgOpeCom().setiCondAnt(TiCondAnt.ANTICIPO_POR_ITEM);
        TgCamItem it = gravado10("SERV-036", "SERVICIO FINAL", "1", "2500.00");
        it.getgValorItem().getgValorRestaItem().setdAntPreUniIt(bd("1000.00"));
        it.setdCDCAnticipo(cdc(anticipoUsd()));
        de.getgDtipDE().getgCamItemList().add(it);
        asociarElectronico(de, cdc(anticipoUsd()));
        contado(de, pago(TiTiPago.EFECTIVO, "1500.00", CMondT.USD, "7300.25"));
        return de;
    }

    /** DE-37b: DE-37 con cantidad 2 y 500,00 de anticipo por unidad (E3-25). */
    static DocumentoElectronico finalAnticipoUsdCantidad2() {
        DocumentoElectronico de = finalAnticipoUsd();
        de.getgTimb().setdNumDoc("0000137");
        de.getgOpeDE().setdCodSeg("000000137");
        TgCamItem it = de.getgDtipDE().getgCamItemList().get(0);
        it.setdCantProSer(bd("2"));
        it.getgValorItem().getgValorRestaItem().setdAntPreUniIt(bd("500.00"));
        pagos(de).get(0).setdMonTiPag(bd("4000.00"));
        return de;
    }

    /** DE-38: NC en USD por 500,00 sobre la factura final DE-37. */
    static DocumentoElectronico ncSobreFinalAnticipo() {
        DocumentoElectronico de = notaCredito("0000038", CMondT.USD, receptorContribuyente(TiTiOpe.B2B), cdc(finalAnticipoUsd()));
        cambioGlobal(de, "7300.25");
        de.getgDtipDE().getgCamItemList().add(gravado10("SERV-036", "SERVICIO FINAL", "1", "500.00"));
        return de;
    }

    /** DE-37n: factura final en guaraníes que aplica el anticipo en USD de DE-36 (solo SIFEN lo ve: 2438). */
    static DocumentoElectronico finalPygConAnticipoUsd() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000237", CMondT.PYG, TTipTra.PRESTACION_SERVICIOS, receptorContribuyente(TiTiOpe.B2B));
        de.getgDatGralOpe().getgOpeCom().setiCondAnt(TiCondAnt.ANTICIPO_POR_ITEM);
        TgCamItem it = gravado10("SERV-036", "SERVICIO FINAL", "1", "18250625");
        it.getgValorItem().getgValorRestaItem().setdAntPreUniIt(bd("7300250"));
        it.setdCDCAnticipo(cdc(anticipoUsd()));
        de.getgDtipDE().getgCamItemList().add(it);
        asociarElectronico(de, cdc(anticipoUsd()));
        contado(de, pago(TiTiPago.EFECTIVO, "10950350", CMondT.PYG, null));
        return de;
    }

    /** DE-42: autofactura (B2C, receptor = emisor) a un vendedor extranjero con pasaporte, contado, constancia. */
    static DocumentoElectronico autofacturaExtranjeroPasaporte() {
        DocumentoElectronico de = CasosDePrueba.autofactura();
        de.getgTimb().setdNumDoc("0000042");
        de.getgOpeDE().setdCodSeg("000000042");
        de.getgDatGralOpe().getgDatRec().setiTiOpe(TiTiOpe.B2C);
        TgCamAE ae = de.getgDtipDE().getgCamAE();
        ae.setiNatVen(TiNatVen.EXTRANJERO);
        ae.setiTipIDVen(TiTipDoc.PASAPORTE);
        ae.setdNumIDVen("XK998877");
        ae.setdNomVen("VENDEDOR EXTRANJERO FICTICIO");
        // MT E308-E315: para un extranjero, la dirección de donde se realizó la transacción
        ae.setdDirVen("MERCADO FICTICIO DE ABASTO");
        return de;
    }

    /** DE-43: la misma autofactura con cédula extranjera. */
    static DocumentoElectronico autofacturaExtranjeroCedula() {
        DocumentoElectronico de = autofacturaExtranjeroPasaporte();
        de.getgTimb().setdNumDoc("0000043");
        de.getgOpeDE().setdCodSeg("000000043");
        de.getgDtipDE().getgCamAE().setiTipIDVen(TiTipDoc.CEDULA_EXTRANJERA);
        de.getgDtipDE().getgCamAE().setdNumIDVen("98765432");
        return de;
    }

    // ------------------------------------------------------------------ grupo C

    /** DE-33: B2G sin gCompPub (opcional desde la NT 026), con el código de contratación de la NT 020. */
    static DocumentoElectronico b2gSinCompPub() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000033", CMondT.PYG, TTipTra.PRESTACION_SERVICIOS, receptorContribuyente(TiTiOpe.B2G));
        de.getgDtipDE().getgCamItemList().add(gravado10("SERV-033", "SERVICIO DE PRUEBA 33", "1", "5500000"));
        TgGrupAdi adi = new TgGrupAdi();
        adi.setdCodConDncp("DNCP-2026-000123");
        TgCamEsp esp = new TgCamEsp();
        esp.setgGrupAdi(adi);
        de.getgDtipDE().setgCamEsp(esp);
        contado(de, pago(TiTiPago.TRANSFERENCIA, "5500000", CMondT.PYG, null));
        return de;
    }

    // ------------------------------------------------------------------ grupo D

    /**
     * Texto de {@code dInfoFisc} con los datos de la NT 007, en su orden; el último es el inciso i) (barcaza y bien
     * transportado, flete internacional fluvial), agregado en el visto bueno de §12.1.3.
     */
    public static final String INFO_FISCAL_EXPORTACION = "Exportacion. Tipo de operacion: exportacion definitiva. Condicion de negociacion: FOB."
            + " Pais de destino: Brasil. Empresa fletera: TRANSPORTADORA FICTICIA S.A. Agente de transporte: AGENTE FICTICIO S.A."
            + " Instrucciones de pago: transferencia bancaria a 30 dias. Conocimiento de embarque: BL-0001. Manifiesto: MANIF-0001"
            + ". Barcaza: BARCAZA FICTICIA 01, 25000 kg de soja en grano";

    /** DE-34: exportación de bienes (NT 007): FE común B2F exenta, crédito a plazo, gTransp fluvial FOB, gCamCarg. */
    static DocumentoElectronico exportacionBienes() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000034", CMondT.USD, TTipTra.VENTA_MERCADERIA, receptorExterior());
        de.getgOpeDE().setdInfoFisc(INFO_FISCAL_EXPORTACION);
        cambioGlobal(de, "7300.25");
        TgCamItem it = exento("SOJA-001", "SOJA EN GRANO", "25000", "0.40");
        it.setcUniMed(TcUniMed.kg);
        it.setdNCM(12019000);
        it.setcPaisOrig(PaisType.PRY);
        de.getgDtipDE().getgCamItemList().add(it);
        credito(de, "30 dias");
        TgTransp tr = new TgTransp();
        tr.setiModTrans(TiModTrans.FLUVIAL);
        tr.setiRespFlete(TiRespFlete.RECEPTOR_FACTURA_ELECTRONICA);
        tr.setcCondNeg(TcCondNeg.FOB);
        tr.setdNuManif("MANIF-0001");
        tr.setdIniTras(LocalDate.of(2026, 11, 24));
        tr.setdFinTras(LocalDate.of(2026, 12, 5));
        tr.setcPaisDest(PaisType.BRA);
        de.getgDtipDE().setgTransp(tr);
        TgCamCarg carga = new TgCamCarg();
        carga.setcUniMedTotPes(TcUniMed.kg);
        carga.setdTotPesMerc(BigInteger.valueOf(25000));
        carga.setcUniMedTotVol(TcUniMed.M3);
        carga.setdTotVolMerc(BigInteger.valueOf(32));
        TgCamGen gen = new TgCamGen();
        gen.setgCamCarg(carga);
        de.setgCamGen(gen);
        return de;
    }

    /** DE-35: exportación de servicios: sin transporte ni NCM, contado por transferencia en USD. */
    static DocumentoElectronico exportacionServicios() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000035", CMondT.USD, TTipTra.PRESTACION_SERVICIOS, receptorExterior());
        de.getgOpeDE().setdInfoFisc("Exportacion de servicios. Pais de destino: Brasil. Instrucciones de pago: transferencia bancaria al contado");
        cambioGlobal(de, "7300.25");
        de.getgDtipDE().getgCamItemList().add(exento("SERV-035", "SERVICIO DE CONSULTORIA", "1", "5000.00"));
        contado(de, pago(TiTiPago.TRANSFERENCIA, "5000.00", CMondT.USD, "7300.25"));
        return de;
    }

    // ------------------------------------------------------------------ S1 a S5

    private static DocumentoElectronico contadoSimple(String numDoc, TiTiPago tipo) {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, numDoc, CMondT.PYG, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-" + numDoc.substring(3), "PRODUCTO DE PRUEBA", "1", "150000"));
        contado(de, pago(tipo, "150000", CMondT.PYG, null));
        return de;
    }

    /** DE-44: tarjeta de crédito con procesadora (en el repositorio, el RUC del emisor ficticio, D58). */
    static DocumentoElectronico tarjetaCredito() {
        DocumentoElectronico de = contadoSimple("0000044", TiTiPago.TARJETA_DE_CREDITO);
        TgPagTarCD t = new TgPagTarCD();
        t.setiDenTarj(TiDenTarj.VISA);
        t.setdRSProTar("PROCESADORA FICTICIA S.A.");
        t.setdRUCProTar(CasosDePrueba.RUC_EMISOR);
        t.setdDVProTar(Short.parseShort(CasosDePrueba.DV_EMISOR));
        t.setiForProPa(TiForProPa.POS);
        t.setdCodAuOpe(123456); // I12-02: el setter recibe int (el campo es long; N 6-10 no entra en un int)
        t.setdNomTit("TITULAR FICTICIO");
        t.setdNumTarj((short) 1234);
        pagos(de).get(0).setgPagTarCD(t);
        return de;
    }

    /** DE-45: cheque de 8 dígitos. */
    static DocumentoElectronico cheque() {
        DocumentoElectronico de = contadoSimple("0000045", TiTiPago.CHEQUE);
        TgPagCheq ch = new TgPagCheq();
        ch.setdNumCheq("00012345");
        ch.setdBcoEmi("BANCO FICTICIO");
        pagos(de).get(0).setgPagCheq(ch);
        return de;
    }

    /** DE-46: 300.000 en efectivo, débito (sin procesadora), transferencia y billetera electrónica. */
    static DocumentoElectronico cuatroMedios() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000046", CMondT.PYG, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-0461", "PRODUCTO DE PRUEBA 461", "1", "300000"));
        TgPaConEIni debito = pago(TiTiPago.TARJETA_DE_DEBITO, "120000", CMondT.PYG, null);
        TgPagTarCD t = new TgPagTarCD();
        t.setiDenTarj(TiDenTarj.MAESTRO);
        t.setiForProPa(TiForProPa.POS);
        debito.setgPagTarCD(t);
        contado(de, pago(TiTiPago.EFECTIVO, "100000", CMondT.PYG, null), debito,
                pago(TiTiPago.TRANSFERENCIA, "50000", CMondT.PYG, null), pago(TiTiPago.BILLETERA_ELECTRONICA, "30000", CMondT.PYG, null));
        return de;
    }

    /** DE-47: factura de 1.000.000 Gs pagada con 269.975 Gs y 100,00 USD a 7.300,25. */
    static DocumentoElectronico pygPagadaEnUsd() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000047", CMondT.PYG, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-0471", "PRODUCTO DE PRUEBA 471", "1", "1000000"));
        contado(de, pago(TiTiPago.EFECTIVO, "269975", CMondT.PYG, null), pago(TiTiPago.EFECTIVO, "100.00", CMondT.USD, "7300.25"));
        return de;
    }

    /** DE-48: crédito en dos cuotas de 100.000 con una entrega inicial de 100.000 en efectivo. */
    static DocumentoElectronico cuotasConEntrega() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000048", CMondT.PYG, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-0481", "PRODUCTO DE PRUEBA 481", "1", "300000"));
        TgCamCond cond = new TgCamCond();
        cond.setiCondOpe(TiCondOpe.CREDITO);
        List<TgPaConEIni> entrega = new ArrayList<>();
        entrega.add(pago(TiTiPago.EFECTIVO, "100000", CMondT.PYG, null));
        cond.setgPaConEIniList(entrega);
        TgPagCred cred = new TgPagCred();
        cred.setiCondCred(TiCondCred.CUOTA);
        cred.setdCuotas((short) 2);
        cred.setdMonEnt(bd("100000"));
        List<TgCuotas> cuotas = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            TgCuotas q = new TgCuotas();
            q.setcMoneCuo(CMondT.PYG);
            q.setdMonCuota(bd("100000"));
            q.setdVencCuo(LocalDate.of(2026, 12, 23).plusMonths(i));
            cuotas.add(q);
        }
        cred.setgCuotasList(cuotas);
        cond.setgPagCred(cred);
        de.getgDtipDE().setgCamCond(cond);
        return de;
    }

    /** DE-49: solo exentos (F002 dSubExe). */
    static DocumentoElectronico soloExentos() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000049", CMondT.PYG, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        de.getgDtipDE().getgCamItemList().add(exento("ITEM-0491", "PRODUCTO EXENTO 491", "2", "45000"));
        de.getgDtipDE().getgCamItemList().add(exento("ITEM-0492", "PRODUCTO EXENTO 492", "1", "10000"));
        contado(de, pago(TiTiPago.EFECTIVO, "100000", CMondT.PYG, null));
        return de;
    }

    /** DE-50: descuento global del 10 % prorrateado en los tres ítems (con decimales) y redondeo SEDECO. */
    static DocumentoElectronico descuentoGlobalProporcional() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000050", CMondT.PYG, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        String[][] items = {{"ITEM-0501", "1", "33333", "3333.30"}, {"ITEM-0502", "2", "22222", "2222.20"}, {"ITEM-0503", "1", "11111", "1111.10"}};
        for (String[] i : items) {
            TgCamItem it = gravado10(i[0], "PRODUCTO DE PRUEBA " + i[0].substring(5), i[1], i[2]);
            it.getgValorItem().getgValorRestaItem().setdDescGloItem(bd(i[3]));
            de.getgDtipDE().getgCamItemList().add(it);
        }
        contado(de, pago(TiTiPago.EFECTIVO, "79950", CMondT.PYG, null));
        return de;
    }

    /** DE-51: un ítem de 100.000 y otro de 20.000 bonificado al 100 %. */
    static DocumentoElectronico itemBonificado() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000051", CMondT.PYG, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-0511", "PRODUCTO DE PRUEBA 511", "1", "100000"));
        TgCamItem b = gravado10("ITEM-0512", "PRODUCTO BONIFICADO 512", "1", "20000");
        b.getgValorItem().getgValorRestaItem().setdDescItem(bd("20000"));
        de.getgDtipDE().getgCamItemList().add(b);
        contado(de, pago(TiTiPago.EFECTIVO, "100000", CMondT.PYG, null));
        return de;
    }

    /** DE-52: EUR, cambio global 8.000,50. */
    static DocumentoElectronico eurCambioGlobal() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000052", CMondT.EUR, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        cambioGlobal(de, "8000.50");
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-0521", "PRODUCTO DE PRUEBA 521", "1", "250.00"));
        contado(de, pago(TiTiPago.EFECTIVO, "250.00", CMondT.EUR, "8000.50"));
        return de;
    }

    /** DE-53: BRL con cambio por ítem. */
    static DocumentoElectronico brlCambioPorItem() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000053", CMondT.BRL, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        de.getgDatGralOpe().getgOpeCom().setdCondTiCam(TdCondTiCam.POR_ITEM);
        TgCamItem a = gravado10("ITEM-0531", "PRODUCTO DE PRUEBA 531", "2", "150.25");
        a.getgValorItem().setdTiCamIt(bd("1350.25"));
        TgCamItem b = gravado10("ITEM-0532", "PRODUCTO DE PRUEBA 532", "1", "80.10");
        b.getgValorItem().setdTiCamIt(bd("1360.10"));
        de.getgDtipDE().getgCamItemList().add(a);
        de.getgDtipDE().getgCamItemList().add(b);
        // con cambio por ítem un solo cambio de pago no da el total en guaraníes: 514.694,1 / 380,60 = 1.352,32 (JBS-E-30)
        contado(de, pago(TiTiPago.EFECTIVO, "380.60", CMondT.BRL, "1352.32"));
        return de;
    }

    /** DE-54: ARS, cotización 7,1234 (menor que 10, cuatro decimales). */
    static DocumentoElectronico arsCuatroDecimales() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000054", CMondT.ARS, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        cambioGlobal(de, "7.1234");
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-0541", "PRODUCTO DE PRUEBA 541", "1", "15000.00"));
        contado(de, pago(TiTiPago.EFECTIVO, "15000.00", CMondT.ARS, "7.1234"));
        return de;
    }

    // ------------------------------------------------------------------ S6

    /**
     * Factura de {@code items} ítems de 1 × 1.000 Gs al 10 % (DE-55, DE-56 y sus variantes). Con
     * {@code largo}, cada ítem lleva la descripción de 120 caracteres y {@code dInfItem} de 500, para pasar
     * los 1000 KB del mensaje (DE-56x, M12-01); con 500 ítems largos es la sonda de I12-23 (DE-56s, ≈ 650 KB).
     *
     * @param numDoc número del documento.
     * @param items  cantidad de ítems.
     * @param largo  textos largos.
     * @return DE con las leyendas.
     */
    public static DocumentoElectronico facturaGrande(String numDoc, int items, boolean largo) {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, numDoc, CMondT.PYG, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        String relleno = "X".repeat(500);
        for (int i = 1; i <= items; i++) {
            String desc = largo ? String.format("PRODUCTO DE PRUEBA %04d ", i) + "D".repeat(96) : String.format("PRODUCTO %04d", i);
            TgCamItem it = gravado10(String.format("IT-%04d", i), desc, "1", "1000");
            if (largo) {
                it.setdInfItem(relleno);
            }
            de.getgDtipDE().getgCamItemList().add(it);
        }
        contado(de, pago(TiTiPago.EFECTIVO, String.valueOf(items * 1000L), CMondT.PYG, null));
        return leyendas(de);
    }

    // ------------------------------------------------------------------ S7

    /** Remisión base (DE-23) con lo que la validación previa exige a una NR (E715, E717, JBS-E-174; I8-09). */
    private static DocumentoElectronico remision(String numDoc, TiMotivTras motivo, TgDatRec receptor) {
        DocumentoElectronico de = CasosDePrueba.notaRemision();
        de.getgTimb().setdNumDoc(numDoc);
        de.getgOpeDE().setdCodSeg("00" + numDoc);
        de.getgDatGralOpe().setgDatRec(receptor);
        de.getgDtipDE().getgCamNRE().setiMotEmiNR(motivo);
        for (TgCamItem i : de.getgDtipDE().getgCamItemList()) {
            i.setcRelMerc(TcRelMerc.TOLERANCIA_DE_QUIEBRA);
            i.setdCanQuiMer(BigDecimal.ZERO);
            i.setdPorQuiMer(BigDecimal.ZERO);
        }
        for (TgVehTras v : de.getgDtipDE().getgTransp().getgVehTrasList()) {
            v.setdMarVeh("TOYOTA");
        }
        return de;
    }

    /** DE-57: traslado entre locales (receptor = emisor, E501 · 2606). */
    static DocumentoElectronico nrEntreLocales() {
        return remision("0000057", TiMotivTras.TRASLADO_ENTRE_LOCALES, receptorEmisor());
    }

    /** DE-58: traslado por consignación. */
    static DocumentoElectronico nrConsignacion() {
        return remision("0000058", TiMotivTras.TRASLADO_POR_CONSIGNACION, receptorContribuyente(TiTiOpe.B2B));
    }

    /** DE-59: remisión por exportación al receptor del exterior de DE-34, con condición FOB y país de destino. */
    static DocumentoElectronico nrExportacion() {
        DocumentoElectronico de = remision("0000059", TiMotivTras.EXPORTACION, receptorExterior());
        de.getgDtipDE().getgTransp().setcCondNeg(TcCondNeg.FOB);
        de.getgDtipDE().getgTransp().setcPaisDest(PaisType.BRA);
        return de;
    }

    /** DE-60: motivo «Otro» (99) con el texto libre de E502 (I3-17). */
    static DocumentoElectronico nrMotivoOtro() {
        DocumentoElectronico de = remision("0000060", TiMotivTras.OTRO, receptorContribuyente(TiTiOpe.B2B));
        de.getgDtipDE().getgCamNRE().setdDesMotEmiNR("TRASLADO PARA PRUEBA DE CALIDAD");
        return de;
    }

    // ------------------------------------------------------------------ S8 y S13

    /** DE-61: nota de débito por ajuste de precio sobre la FE electrónica DE-01. */
    static DocumentoElectronico ndSobreElectronica() {
        DocumentoElectronico de = base(TTiDE.NOTA_DE_DEBITO_ELECTRONICA, "0000061", CMondT.PYG, null, receptorContribuyente(TiTiOpe.B2B));
        TgCamNCDE nd = new TgCamNCDE();
        nd.setiMotEmi(TiMotEmi.AJUSTE_DE_PRECIO);
        de.getgDtipDE().setgCamNCDE(nd);
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-0611", "AJUSTE DE PRECIO 611", "1", "11000"));
        asociarElectronico(de, CasosDePrueba.CDC_FACTURA_SIMPLE);
        return de;
    }

    /** DE-62: 1.100.000 al 10 % y una comisión de 110.000 (F025); pago por el total con la comisión (F014). */
    static DocumentoElectronico comision() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000062", CMondT.PYG, TTipTra.PRESTACION_SERVICIOS, receptorContribuyente(TiTiOpe.B2B));
        de.getgDtipDE().getgCamItemList().add(gravado10("SERV-062", "SERVICIO DE INTERMEDIACION", "1", "1100000"));
        de.getgTotSub().setdComi(COMISION);
        contado(de, pago(TiTiPago.EFECTIVO, "1210000", CMondT.PYG, null));
        return de;
    }

    // ------------------------------------------------------------------ S14 (D57)

    private static DocumentoElectronico sector(String numDoc, String codigo, String descripcion, String precio) {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, numDoc, CMondT.PYG, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        de.getgDtipDE().getgCamItemList().add(gravado10(codigo, descripcion, "1", precio));
        contado(de, pago(TiTiPago.EFECTIVO, precio, CMondT.PYG, null));
        return de;
    }

    private static TgCamEsp camEsp(DocumentoElectronico de) {
        TgCamEsp esp = new TgCamEsp();
        de.getgDtipDE().setgCamEsp(esp);
        return esp;
    }

    /** DE-63: vehículo nuevo (E8.5). */
    static DocumentoElectronico vehiculoNuevo() {
        DocumentoElectronico de = sector("0000063", "VEH-0631", "AUTOMOVIL NUEVO FICTICIO", "150000000");
        TgVehNuevo v = new TgVehNuevo();
        v.setiTipOpVN(TiTipOpVN.VENTA_AL_CONSUMIDOR_FINAL);
        v.setdChasis("ABCDEFGH123456789");
        v.setdColor("BLANCO");
        v.setdPotencia((short) 150);
        v.setdCapMot((short) 1600);
        v.setdPNet(bd("1200.0000"));
        v.setdPBruto(bd("1500.0000"));
        v.setiTipCom(TiTipCom.GASOLINA);
        v.setdNroMotor("MOTOR123456");
        v.setdCapTracc(bd("0.00"));
        v.setdAnoFab((short) 2026);
        v.setcTipVeh("AUTOMOVIL");
        v.setdCapac((short) 5);
        v.setdCilin("1600");
        de.getgDtipDE().getgCamItemList().get(0).setgVehNuevo(v);
        return de;
    }

    /** DE-64: energía eléctrica (E9.2): consumo = lectura actual − anterior. */
    static DocumentoElectronico energia() {
        DocumentoElectronico de = sector("0000064", "ENER-0641", "CONSUMO DE ENERGIA ELECTRICA", "275000");
        TgGrupEner e = new TgGrupEner();
        e.setdNroMed("MED-0001");
        e.setdActiv((short) 1);
        e.setdCateg("RES");
        e.setdLecAnt(bd("1000"));
        e.setdLecAct(bd("1250"));
        e.setdConKwh(bd("250"));
        camEsp(de).setgGrupEner(e);
        return de;
    }

    /** DE-65: seguros (E9.3) con una póliza cuyo dCodInt es el del ítem (NT 008). */
    static DocumentoElectronico seguros() {
        DocumentoElectronico de = sector("0000065", "POL-0651", "PRIMA DE SEGURO DE AUTOMOVIL", "1200000");
        TgGrupPolSeg p = new TgGrupPolSeg();
        p.setdPoliza("POLIZA AUTOMOVIL");
        p.setdUnidVig("MESES");
        p.setdVigencia(bd("12"));
        p.setdNumPoliza("POL-000123");
        p.setdFecIniVig(LocalDateTime.of(2026, 11, 23, 0, 0, 0));
        p.setdFecFinVig(LocalDateTime.of(2027, 11, 23, 0, 0, 0));
        p.setdCodInt("POL-0651");
        List<TgGrupPolSeg> polizas = new ArrayList<>();
        polizas.add(p);
        TgGrupSeg s = new TgGrupSeg();
        s.setdCodEmpSeg("ASEG0000000000000001");
        s.setgGrupPolSegList(polizas);
        camEsp(de).setgGrupSeg(s);
        return de;
    }

    /** DE-66: supermercado (E9.4). */
    static DocumentoElectronico supermercado() {
        DocumentoElectronico de = sector("0000066", "SUP-0661", "COMPRA DE SUPERMERCADO", "94500");
        TgGrupSup s = new TgGrupSup();
        s.setdNomCaj("CAJERO FICTICIO");
        s.setdEfectivo(bd("100000"));
        s.setdVuelto(bd("5000"));
        s.setdDonac(bd("500"));
        s.setdDesDonac("DONACION FICTICIA");
        camEsp(de).setgGrupSup(s);
        return de;
    }

    /** DE-67: servicio cíclico con contrato (E9.5, iIndPres 6), tres vencimientos. */
    static DocumentoElectronico cicloContrato() {
        DocumentoElectronico de = sector("0000067", "ALQ-0671", "ALQUILER MENSUAL DE PRUEBA", "2500000");
        de.getgDtipDE().getgCamFE().setiIndPres(TiIndPres.OPERACION_CICLICA);
        TgGrupAdi a = new TgGrupAdi();
        a.setdCiclo("NOVIEMBRE 2026");
        a.setdFecIniC(LocalDate.of(2026, 11, 1));
        a.setdFecFinC(LocalDate.of(2026, 11, 30));
        List<LocalDate> venc = new ArrayList<>();
        venc.add(LocalDate.of(2026, 12, 10));
        venc.add(LocalDate.of(2026, 12, 20));
        venc.add(LocalDate.of(2026, 12, 30));
        a.setdVencPagList(venc);
        a.setdContrato("CONTRATO-0001");
        a.setdSalAnt(bd("150000"));
        camEsp(de).setgGrupAdi(a);
        return de;
    }

    /**
     * DE-68: rastreo de la mercadería (E8.4) con los campos vigentes. Sin el importador ({@code dNomImp},
     * {@code dDirImp}, {@code dNumFir}): la NT 010 lo eliminó y SIFEN-test rechazó con 0160 el primer envío, que lo
     * traía (E12-08, ESC-44). {@code dNomPro} va a la Fase 7.
     */
    static DocumentoElectronico rastreo() {
        DocumentoElectronico de = sector("0000068", "MED-0681", "MEDICAMENTO DE PRUEBA", "85000");
        TgRasMerc r = new TgRasMerc();
        r.setdNumLote("LOTE-2026-01");
        r.setdVencMerc(LocalDate.of(2027, 6, 30));
        r.setdNSerie("SERIE-0001");
        r.setdNumPedi("PEDIDO-0001");
        r.setdNumSegui("SEGUI-0001");
        // A 1-20 en la NT 010 y en el XSD vigente (tdCadenaBase20, minLength 1, maxLength 20; I12-05); la copia del XSD
        // de 2019 pedía 20 exactos: 20 cumple los dos y lo controla el arnés en vivo
        r.setdNumReg("REG-0000000000000001");
        r.setdNumRegEntCom("RC-00000000000000001");
        de.getgDtipDE().getgCamItemList().get(0).setgRasMerc(r);
        return de;
    }

    // ------------------------------------------------------------------ eventos

    private static EventosDE evento(String id, TgGroupTiEvt grupo) {
        TrGesEve ges = new TrGesEve();
        ges.setId(id);
        ges.setdFecFirma(CasosDePrueba.FECHA_FIRMA);
        ges.setgGroupTiEvt(grupo);
        EventosDE ev = new EventosDE();
        List<TrGesEve> l = new ArrayList<>();
        l.add(ges);
        ev.setrGesEveList(l);
        return ev;
    }

    /** EV-07: notificación de recepción de DE-01 por su receptor (contribuyente). */
    static EventosDE notificacionRecepcion() {
        TrGeVeNotRec n = new TrGeVeNotRec();
        n.setId(CasosDePrueba.CDC_FACTURA_SIMPLE);
        n.setdFecEmi(CasosDePrueba.FECHA_EMISION);
        n.setdFecRecep(CasosDePrueba.FECHA_EMISION.plusHours(1));
        n.setiTipRec(TiNatRec.CONTRIBUYENTE);
        n.setdNomRec("CLIENTE FICTICIO S.A.");
        n.setdRucRec("80654321");
        n.setdDVRec("3");
        n.setdTotalGs(bd("110000"));
        TgGroupTiEvt g = new TgGroupTiEvt();
        g.setrGeVeNotRec(n);
        return evento("7", g);
    }

    /** EV-08: desconocimiento de DE-01 por su receptor. */
    static EventosDE desconocimiento() {
        TrGeVeDescon d = new TrGeVeDescon();
        d.setId(CasosDePrueba.CDC_FACTURA_SIMPLE);
        d.setdFecEmi(CasosDePrueba.FECHA_EMISION);
        d.setdFecRecep(CasosDePrueba.FECHA_EMISION.plusHours(1));
        d.setiTipRec(TiNatRec.CONTRIBUYENTE);
        d.setdNomRec("CLIENTE FICTICIO S.A.");
        d.setdRucRec("80654321");
        d.setdDVRec("3");
        d.setmOtEve("Desconocimiento de prueba del receptor");
        TgGroupTiEvt g = new TgGroupTiEvt();
        g.setrGeVeDescon(d);
        return evento("8", g);
    }

    /** EV-09: conformidad total de DE-01 (después de la parcial de EV-03). */
    static EventosDE conformidadTotal() {
        TrGeVeConf conf = new TrGeVeConf();
        conf.setId(CasosDePrueba.CDC_FACTURA_SIMPLE);
        conf.setiTipConf(TiTipConf.CONFORMIDAD_TOTAL);
        conf.setdFecRecep(CasosDePrueba.FECHA_EMISION.plusDays(1));
        TgGroupTiEvt g = new TgGroupTiEvt();
        g.setrGeVeConf(conf);
        return evento("9", g);
    }

    /** OP-05: inutilización de un rango (más de 1000 números, o invertido). */
    static EventosDE inutilizacion(String desde, String hasta) {
        TrGeVeInu inu = new TrGeVeInu();
        inu.setdNumTim(CasosDePrueba.TIMBRADO);
        inu.setdEst("001");
        inu.setdPunExp("001");
        inu.setdNumIn(desde);
        inu.setdNumFin(hasta);
        inu.setiTiDE(TTiDE.FACTURA_ELECTRONICA);
        inu.setmOtEve("Inutilizacion de prueba de numeracion");
        TgGroupTiEvt g = new TgGroupTiEvt();
        g.setrGeVeInu(inu);
        return evento("10", g);
    }
}

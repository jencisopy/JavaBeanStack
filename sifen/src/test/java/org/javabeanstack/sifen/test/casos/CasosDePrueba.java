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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Catálogo de casos de prueba de {@code jbs-sifen} (plan JBSSIF, §2.1; tabla en
 * {@code src/test/resources/casos/README.md}).
 *
 * <p>Cada caso de DE tiene un XML de entrada en {@code casos/<archivo>.xml} (un {@code rDE} sin
 * firma; emisor ficticio {@code 80123456-5}); los que se arman por objeto tienen además un
 * <em>constructor</em> acá, del que salió ese XML (el arnés A/B lo generó con el fork y lo congeló).
 * Fechas, código de seguridad ({@code dCodSeg}) y números de documento son fijos: la salida es
 * determinista con el mismo certificado (A3).</p>
 *
 * <p>Solo usa la API pública de {@code org.javabeanstack.sifen}: el arnés A/B (fuera del repo,
 * D8) la compila contra {@code jbs-sifen} y, con una copia de paquetes renombrados, contra el
 * fork. Los defectos conocidos del fork se reproducen tal cual (D10); cada caso que los toca lo
 * dice en {@link Caso#getNota()}.</p>
 */
public final class CasosDePrueba {
    /** Fecha de emisión de los casos armados por objeto. */
    public static final LocalDateTime FECHA_EMISION = LocalDateTime.of(2026, 11, 23, 10, 0, 0);
    /** Fecha de firma de los casos armados por objeto. */
    public static final LocalDateTime FECHA_FIRMA = LocalDateTime.of(2026, 11, 23, 10, 0, 5);
    /** RUC del emisor ficticio (el del certificado autofirmado). */
    public static final String RUC_EMISOR = "80123456";
    /** DV del emisor ficticio. */
    public static final String DV_EMISOR = "5";
    /** Timbrado ficticio. */
    public static final int TIMBRADO = 12345678;
    /** Inicio de vigencia del timbrado ficticio. */
    public static final LocalDate INICIO_TIMBRADO = LocalDate.of(2026, 1, 2);
    /** CDC de {@code factura-simple-00} (referencia de NC, eventos). */
    public static final String CDC_FACTURA_SIMPLE = "01801234565001001000000122026112310000000010";

    private CasosDePrueba() {
    }

    /** Un caso de DE del catálogo. */
    public static final class Caso {
        private final String id;
        private final String archivo;
        private final String descripcion;
        private final String nota;
        private final Supplier<DocumentoElectronico> constructor;
        private final Consumer<DocumentoElectronico> ajusteTrasLeer;

        Caso(String id, String archivo, String descripcion, String nota,
             Supplier<DocumentoElectronico> constructor, Consumer<DocumentoElectronico> ajusteTrasLeer) {
            this.id = id;
            this.archivo = archivo;
            this.descripcion = descripcion;
            this.nota = nota;
            this.constructor = constructor;
            this.ajusteTrasLeer = ajusteTrasLeer;
        }

        /** @return identificador ({@code DE-01}…); nombre del golden. */
        public String getId() {
            return id;
        }

        /** @return nombre (sin extensión) del XML de entrada en {@code casos/}. */
        public String getArchivo() {
            return archivo;
        }

        public String getDescripcion() {
            return descripcion;
        }

        /** @return defecto conocido que el caso fija, o cadena vacía. */
        public String getNota() {
            return nota;
        }

        /** @return {@code true} si el caso se arma también por objeto. */
        public boolean tieneConstructor() {
            return constructor != null;
        }

        /** @return DE armado por objeto (nuevo en cada llamada). */
        public DocumentoElectronico construir() {
            return constructor.get();
        }

        /**
         * Lee el DE desde el XML de entrada y aplica el ajuste del caso, si tiene.
         *
         * @param xml contenido de {@code casos/<archivo>.xml}.
         * @return DE leído.
         * @throws Exception si el XML no se puede leer.
         */
        public DocumentoElectronico leer(String xml) throws Exception {
            DocumentoElectronico de = new DocumentoElectronico(xml);
            if (ajusteTrasLeer != null) {
                ajusteTrasLeer.accept(de);
            }
            return de;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    /** Un caso de evento del catálogo. */
    public static final class CasoEvento {
        private final String id;
        private final String descripcion;
        private final String nota;
        private final Supplier<EventosDE> constructor;

        CasoEvento(String id, String descripcion, String nota, Supplier<EventosDE> constructor) {
            this.id = id;
            this.descripcion = descripcion;
            this.nota = nota;
            this.constructor = constructor;
        }

        public String getId() {
            return id;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public String getNota() {
            return nota;
        }

        public EventosDE construir() {
            return constructor.get();
        }

        @Override
        public String toString() {
            return id;
        }
    }

    /**
     * Casos de DE, en orden.
     *
     * @return lista inmutable.
     */
    public static List<Caso> documentos() {
        List<Caso> l = new ArrayList<>();
        l.add(new Caso("DE-01", "factura-simple-00", "Factura PYG contado efectivo, 2 ítems IVA 10 %, receptor contribuyente (B2B)", "", null, null));
        l.add(new Caso("DE-02", "caso-usd-sin-redondeo-original", "Factura USD de un ERP sin redondeo SEDECO, releída del XML", "E0-01: al releer del XML el redondeo vuelve a true y se recalcula (dRedon 0.100)", null, null));
        l.add(new Caso("DE-03", "caso-usd-sin-redondeo-original", "El mismo XML con TgTotSub(false) puesto tras leer (camino del consumidor que conserva el flag)", "Reproduce los totales originales sin redondeo (dTotalGs 49870888.704000)", null,
                de -> de.setgTotSub(new TgTotSub(false))));
        l.add(new Caso("DE-04", "caso-usd-con-redondeo-aprobado", "La versión del mismo DE aprobada por SIFEN (con redondeo, dProtAut y xContEv bajo rDE)", "E0-03: escalas arrastradas (dRedon 0.100, dTotGralOpe 8517.500)", null, null));
        l.add(new Caso("DE-05", "factura-pyg-b2c-cedula", "Factura PYG contado, receptor no contribuyente con CI, total no múltiplo de 50", "Redondeo SEDECO en PYG", CasosDePrueba::facturaPygB2cCedula, null));
        l.add(new Caso("DE-06", "factura-pyg-b2c-sin-redondeo", "DE-05 armado con TgTotSub(false)", "E0-01: por objeto sale sin redondeo; releído del XML, con redondeo (golden .obj distinto)", CasosDePrueba::facturaPygB2cSinRedondeo, null));
        l.add(new Caso("DE-07", "factura-usd-credito-plazo", "Factura USD crédito a plazo, tipo de cambio global (dTiCam)", "", CasosDePrueba::facturaUsdCreditoPlazo, null));
        l.add(new Caso("DE-08", "factura-usd-cambio-por-item", "Factura USD contado, tipo de cambio por ítem (dTiCamIt, dTotOpeGs)", "", CasosDePrueba::facturaUsdCambioPorItem, null));
        l.add(new Caso("DE-09", "factura-iva-mixto", "Factura PYG con ítems IVA 10 %, IVA 5 % y exento", "", CasosDePrueba::facturaIvaMixto, null));
        l.add(new Caso("DE-10", "factura-gravado-parcial-pyg", "Factura PYG con ítem gravado parcial (dPropIVA 30, NT 13 dBasExe)", "E0-04: TgCamIVA del fork usa la fórmula general", CasosDePrueba::facturaGravadoParcialPyg, null));
        l.add(new Caso("DE-11", "factura-gravado-parcial-usd", "Factura USD con ítem gravado parcial al 5 % (dPropIVA 50)", "", CasosDePrueba::facturaGravadoParcialUsd, null));
        l.add(new Caso("DE-12", "factura-exonerado", "Factura PYG con ítem exonerado", "", CasosDePrueba::facturaExonerado, null));
        l.add(new Caso("DE-13", "factura-descuentos", "Factura PYG con descuento por ítem y descuento global", "dPorcDescTotal solo desde el descuento global (fork)", CasosDePrueba::facturaDescuentos, null));
        l.add(new Caso("DE-14", "factura-anticipo", "Factura PYG con anticipo por ítem (iCondAnt 2)", "I0-13: dTotAntItem sin × cantidad (F034)", CasosDePrueba::facturaAnticipo, null));
        l.add(new Caso("DE-15", "factura-b2g", "Factura B2G con gCompPub y dDncpG/dDncpE", "", CasosDePrueba::facturaB2g, null));
        l.add(new Caso("DE-16", "factura-innominado", "Factura PYG a receptor innominado", "", CasosDePrueba::facturaInnominado, null));
        l.add(new Caso("DE-17", "factura-extranjero", "Factura USD a receptor extranjero con pasaporte", "", CasosDePrueba::facturaExtranjero, null));
        l.add(new Caso("DE-18", "factura-tasa-10-00", "Factura PYG con dTasaIVA = 10.00 (escala 2)", "E0-17: TgTotSub compara con equals y el ítem no suma: dTotOpe 0", CasosDePrueba::facturaTasaConEscala, null));
        l.add(new Caso("DE-19", "factura-credito-cuotas", "Factura PYG crédito en 3 cuotas", "", CasosDePrueba::facturaCreditoCuotas, null));
        l.add(new Caso("DE-20", "autofactura", "Autofactura (iTiDE 4) a vendedor no contribuyente", "", CasosDePrueba::autofactura, null));
        l.add(new Caso("DE-21", "nota-credito", "Nota de crédito (iTiDE 5) asociada al CDC de DE-01", "", CasosDePrueba::notaCredito, null));
        l.add(new Caso("DE-22", "nota-debito", "Nota de débito (iTiDE 6) asociada a una factura impresa", "", CasosDePrueba::notaDebito, null));
        l.add(new Caso("DE-23", "nota-remision", "Nota de remisión (iTiDE 7), sin gTotSub, con transporte", "", CasosDePrueba::notaRemision, null));
        // Agregados el 2026-11-24 (plan JBSSIF, §2.1.1–§2.1.4; pedido del usuario): B2F sin tipo de documento,
        // inmobiliaria (IVA 5 % sobre el 30 %) con y sin redondeo, y anticipos.
        l.add(new Caso("DE-24", "factura-b2f-pasaporte-sin-numero", "Factura USD a receptor del exterior (B2F) con pasaporte y sin número (D210 opcional en B2F)",
                "", CasosDePrueba::facturaB2fPasaporteSinNumero, null));
        l.add(new Caso("DE-25", "inmobiliaria-pyg-redondeo", "Inmobiliaria PYG: lote IVA 5 % sobre el 30 % + administración 10 % + exento, con redondeo", "", () -> inmobiliariaPyg(true), null));
        l.add(new Caso("DE-26", "inmobiliaria-pyg-sin-redondeo", "DE-25 con TgTotSub(false)", "", () -> inmobiliariaPyg(false), null));
        l.add(new Caso("DE-27", "inmobiliaria-usd-redondeo", "Inmobiliaria USD, cambio global, redondeo hacia abajo (1680,24 → 1680,00)", "", () -> inmobiliariaUsd(true, "100.00"), null));
        l.add(new Caso("DE-28", "inmobiliaria-usd-sin-redondeo", "DE-27 con TgTotSub(false)", "", () -> inmobiliariaUsd(false, "100.00"), null));
        l.add(new Caso("DE-29", "inmobiliaria-usd-redondeo-arriba", "Inmobiliaria USD con redondeo hacia arriba (1680,40 → 1680,50)",
                "I3-34: dRedon 0.1 y dTotGralOpe 1680.500 (F014 ≠ F008 − F013)", () -> inmobiliariaUsd(true, "100.16"), null));
        l.add(new Caso("DE-30", "factura-de-anticipo", "Factura de anticipo (iTipTra 9), lote IVA 5 % sobre el 30 %",
                "E3-24: dCDCAnticipo vacío (DEC-08)", CasosDePrueba::facturaDeAnticipo, null));
        l.add(new Caso("DE-31", "factura-final-anticipo-por-item", "Factura final con el anticipo de DE-30 por ítem (dAntPreUniIt, cantidad 2)",
                "E3-25: dTotAntItem sin × cantidad (550000, el MT pide 1100000)", CasosDePrueba::facturaFinalAnticipoPorItem, null));
        l.add(new Caso("DE-32", "factura-final-anticipo-global", "Factura final con anticipo global (dAntGloPreUniIt en cada ítem)",
                "E3-25: dTotAnt sin × cantidad (200000, el MT pide 400000)", CasosDePrueba::facturaFinalAnticipoGlobal, null));
        return Collections.unmodifiableList(l);
    }

    /**
     * Casos de eventos, en orden.
     *
     * @return lista inmutable.
     */
    public static List<CasoEvento> eventos() {
        List<CasoEvento> l = new ArrayList<>();
        l.add(new CasoEvento("EV-01", "Cancelación del DE-01", "", CasosDePrueba::eventoCancelacion));
        l.add(new CasoEvento("EV-02", "Inutilización de un rango de numeración", "", CasosDePrueba::eventoInutilizacion));
        l.add(new CasoEvento("EV-03", "Conformidad parcial del receptor", "", CasosDePrueba::eventoConformidad));
        l.add(new CasoEvento("EV-04", "Disconformidad del receptor", "", CasosDePrueba::eventoDisconformidad));
        l.add(new CasoEvento("EV-05", "Nominación (no contribuyente) con dDirRec, departamento, distrito y ciudad",
                "E0-12: la ciudad sale como dDesCiuEnt; con distrito no cae en E0-18", () -> eventoNominacionConDireccion((short) 1)));
        l.add(new CasoEvento("EV-06", "Nominación de un contribuyente, sin dirección", "", CasosDePrueba::eventoNominacionContribuyente));
        return Collections.unmodifiableList(l);
    }

    // ------------------------------------------------------------------ armado común

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    /** Emisor ficticio de los fixtures. */
    public static TgEmis emisor() {
        TgEmis e = new TgEmis();
        e.setdRucEm(RUC_EMISOR);
        e.setdDVEmi(DV_EMISOR);
        e.setiTipCont(TiTipCont.PERSONA_JURIDICA);
        e.setdNomEmi("EMPRESA FICTICIA DE PRUEBA S.A.");
        e.setdDirEmi("CALLE FICTICIA");
        e.setdNumCas("123");
        e.setcDepEmi(TDepartamento.CAPITAL);
        e.setcCiuEmi(1);
        e.setdDesCiuEmi("ASUNCION (DISTRITO)");
        e.setdTelEmi("0210000000");
        e.setdEmailE("facturacion@example.com");
        TgActEco act = new TgActEco();
        act.setcActEco("46900");
        act.setdDesActEco("VENTA AL POR MAYOR NO ESPECIALIZADA");
        List<TgActEco> acts = new ArrayList<>();
        acts.add(act);
        e.setgActEcoList(acts);
        return e;
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

    /**
     * Esqueleto de DE: grupos A–D y E con los datos comunes; los ítems y la condición los pone cada
     * caso.
     */
    private static DocumentoElectronico base(TTiDE tipo, String numDoc, String codSeg, CMondT moneda, TTipTra tipoTransaccion, TgDatRec receptor) {
        DocumentoElectronico de = new DocumentoElectronico();
        de.setdFecFirma(FECHA_FIRMA);
        de.setdSisFact((short) 1);

        TgOpeDE ope = new TgOpeDE();
        ope.setiTipEmi(TTipEmi.NORMAL);
        ope.setdCodSeg(codSeg);
        de.setgOpeDE(ope);

        TgTimb timb = new TgTimb();
        timb.setiTiDE(tipo);
        timb.setdNumTim(TIMBRADO);
        timb.setdEst("001");
        timb.setdPunExp("001");
        timb.setdNumDoc(numDoc);
        timb.setdFeIniT(INICIO_TIMBRADO);
        de.setgTimb(timb);

        TdDatGralOpe gral = new TdDatGralOpe();
        gral.setdFeEmiDE(FECHA_EMISION);
        if (tipo != TTiDE.NOTA_DE_REMISION_ELECTRONICA) {
            TgOpeCom com = new TgOpeCom();
            com.setiTipTra(tipoTransaccion);
            com.setiTImp(TTImp.IVA);
            com.setcMoneOpe(moneda);
            gral.setgOpeCom(com);
        }
        gral.setgEmis(emisor());
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

    private static void contado(DocumentoElectronico de, String monto, CMondT moneda, String tipoCambio) {
        TgCamCond cond = new TgCamCond();
        cond.setiCondOpe(TiCondOpe.CONTADO);
        TgPaConEIni pago = new TgPaConEIni();
        pago.setiTiPago(TiTiPago.EFECTIVO);
        pago.setdMonTiPag(bd(monto));
        pago.setcMoneTiPag(moneda);
        if (tipoCambio != null) {
            pago.setdTiCamTiPag(bd(tipoCambio));
        }
        List<TgPaConEIni> pagos = new ArrayList<>();
        pagos.add(pago);
        cond.setgPaConEIniList(pagos);
        de.getgDtipDE().setgCamCond(cond);
    }

    /**
     * Ítem con valores. {@code dDescItem} y {@code dDescGloItem} van siempre (en 0 si no hay): el
     * fork hace {@code getdDescItem().multiply(…)} y {@code getdDescGloItem().multiply(…)} en
     * {@code TgTotSub} sin comprobar nulo (E2-03).
     */
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

    // ------------------------------------------------------------------ casos de DE

    static DocumentoElectronico facturaPygB2cCedula() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000005", "000000005", CMondT.PYG, TTipTra.VENTA_MERCADERIA, receptorCedula());
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-0005", "PRODUCTO DE PRUEBA 5", "1", "47789"));
        contado(de, "47750", CMondT.PYG, null);
        return de;
    }

    static DocumentoElectronico facturaPygB2cSinRedondeo() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000006", "000000006", CMondT.PYG, TTipTra.VENTA_MERCADERIA, receptorCedula());
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-0005", "PRODUCTO DE PRUEBA 5", "1", "47789"));
        contado(de, "47789", CMondT.PYG, null);
        de.setgTotSub(new TgTotSub(false));
        return de;
    }

    static DocumentoElectronico facturaUsdCreditoPlazo() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000007", "000000007", CMondT.USD, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        de.getgDatGralOpe().getgOpeCom().setdCondTiCam(TdCondTiCam.GLOBAL);
        de.getgDatGralOpe().getgOpeCom().setdTiCam(bd("7300.25"));
        List<TgCamItem> items = de.getgDtipDE().getgCamItemList();
        items.add(gravado10("ITEM-0071", "PRODUCTO DE PRUEBA 71", "3", "1234.56"));
        items.add(gravado10("ITEM-0072", "PRODUCTO DE PRUEBA 72", "1", "99.99"));
        TgCamCond cond = new TgCamCond();
        cond.setiCondOpe(TiCondOpe.CREDITO);
        TgPagCred cred = new TgPagCred();
        cred.setiCondCred(TiCondCred.PLAZO);
        cred.setdPlazoCre("30 dias");
        cond.setgPagCred(cred);
        de.getgDtipDE().setgCamCond(cond);
        return de;
    }

    static DocumentoElectronico facturaUsdCambioPorItem() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000008", "000000008", CMondT.USD, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        de.getgDatGralOpe().getgOpeCom().setdCondTiCam(TdCondTiCam.POR_ITEM);
        TgCamItem a = gravado10("ITEM-0081", "PRODUCTO DE PRUEBA 81", "2", "150.25");
        a.getgValorItem().setdTiCamIt(bd("7300.25"));
        TgCamItem b = gravado10("ITEM-0082", "PRODUCTO DE PRUEBA 82", "1", "80.10");
        b.getgValorItem().setdTiCamIt(bd("7310.50"));
        de.getgDtipDE().getgCamItemList().add(a);
        de.getgDtipDE().getgCamItemList().add(b);
        contado(de, "380.60", CMondT.USD, "7300.25");
        return de;
    }

    static DocumentoElectronico facturaIvaMixto() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000009", "000000009", CMondT.PYG, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        List<TgCamItem> items = de.getgDtipDE().getgCamItemList();
        items.add(gravado10("ITEM-0091", "PRODUCTO DE PRUEBA 91", "1", "110000"));
        items.add(item("ITEM-0092", "PRODUCTO DE PRUEBA 92", "2", "52500", TiAfecIVA.GRAVADO, "100", "5"));
        items.add(item("ITEM-0093", "PRODUCTO DE PRUEBA 93", "1", "30000", TiAfecIVA.EXENTO, "0", "0"));
        contado(de, "245000", CMondT.PYG, null);
        return de;
    }

    static DocumentoElectronico facturaGravadoParcialPyg() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000010", "000000010", CMondT.PYG, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        de.getgDtipDE().getgCamItemList().add(item("ITEM-0101", "PRODUCTO DE PRUEBA 101", "1", "1000000", TiAfecIVA.GRAVADO_PARCIAL, "30", "10"));
        contado(de, "1000000", CMondT.PYG, null);
        return de;
    }

    static DocumentoElectronico facturaGravadoParcialUsd() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000011", "000000011", CMondT.USD, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        de.getgDatGralOpe().getgOpeCom().setdCondTiCam(TdCondTiCam.GLOBAL);
        de.getgDatGralOpe().getgOpeCom().setdTiCam(bd("7300.25"));
        de.getgDtipDE().getgCamItemList().add(item("ITEM-0111", "PRODUCTO DE PRUEBA 111", "1", "1500.75", TiAfecIVA.GRAVADO_PARCIAL, "50", "5"));
        contado(de, "1500.75", CMondT.USD, "7300.25");
        return de;
    }

    static DocumentoElectronico facturaExonerado() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000012", "000000012", CMondT.PYG, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        de.getgDtipDE().getgCamItemList().add(item("ITEM-0121", "PRODUCTO DE PRUEBA 121", "4", "25000", TiAfecIVA.EXONERADO, "0", "0"));
        contado(de, "100000", CMondT.PYG, null);
        return de;
    }

    static DocumentoElectronico facturaDescuentos() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000013", "000000013", CMondT.PYG, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        TgCamItem a = gravado10("ITEM-0131", "PRODUCTO DE PRUEBA 131", "2", "100000");
        a.getgValorItem().getgValorRestaItem().setdDescItem(bd("10000"));
        TgCamItem b = gravado10("ITEM-0132", "PRODUCTO DE PRUEBA 132", "1", "50000");
        b.getgValorItem().getgValorRestaItem().setdDescGloItem(bd("5000"));
        de.getgDtipDE().getgCamItemList().add(a);
        de.getgDtipDE().getgCamItemList().add(b);
        contado(de, "225000", CMondT.PYG, null);
        return de;
    }

    static DocumentoElectronico facturaAnticipo() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000014", "000000014", CMondT.PYG, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        de.getgDatGralOpe().getgOpeCom().setiCondAnt(TiCondAnt.ANTICIPO_POR_ITEM);
        TgCamItem a = gravado10("ITEM-0141", "PRODUCTO DE PRUEBA 141", "3", "200000");
        a.getgValorItem().getgValorRestaItem().setdAntPreUniIt(bd("50000"));
        de.getgDtipDE().getgCamItemList().add(a);
        contado(de, "450000", CMondT.PYG, null);
        return de;
    }

    static DocumentoElectronico facturaB2g() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000015", "000000015", CMondT.PYG, TTipTra.PRESTACION_SERVICIOS, receptorContribuyente(TiTiOpe.B2G));
        TgCompPub pub = new TgCompPub();
        pub.setdModCont("CD");
        pub.setdEntCont(12345);
        pub.setdAnoCont((short) 26);
        pub.setdSecCont("0000001");
        pub.setdFeCodCont(LocalDate.of(2026, 1, 15));
        de.getgDtipDE().getgCamFE().setgCompPub(pub);
        TgCamItem a = gravado10("ITEM-0151", "SERVICIO DE PRUEBA 151", "1", "5500000");
        a.setdDncpG("12345678");
        a.setdDncpE("1234");
        de.getgDtipDE().getgCamItemList().add(a);
        contado(de, "5500000", CMondT.PYG, null);
        return de;
    }

    static DocumentoElectronico facturaInnominado() {
        TgDatRec r = new TgDatRec();
        r.setiNatRec(TiNatRec.NO_CONTRIBUYENTE);
        r.setiTiOpe(TiTiOpe.B2C);
        r.setcPaisRec(PaisType.PRY);
        r.setiTipIDRec(TiTipDocRec.INNOMINADO);
        r.setdNumIDRec("0");
        r.setdNomRec("Sin Nombre");
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000016", "000000016", CMondT.PYG, TTipTra.VENTA_MERCADERIA, r);
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-0161", "PRODUCTO DE PRUEBA 161", "1", "35000"));
        contado(de, "35000", CMondT.PYG, null);
        return de;
    }

    static DocumentoElectronico facturaExtranjero() {
        TgDatRec r = new TgDatRec();
        r.setiNatRec(TiNatRec.NO_CONTRIBUYENTE);
        r.setiTiOpe(TiTiOpe.B2C);
        r.setcPaisRec(PaisType.ARG);
        r.setiTipIDRec(TiTipDocRec.PASAPORTE);
        r.setdNumIDRec("AB123456");
        r.setdNomRec("CLIENTE EXTRANJERO FICTICIO");
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000017", "000000017", CMondT.USD, TTipTra.VENTA_MERCADERIA, r);
        de.getgDatGralOpe().getgOpeCom().setdCondTiCam(TdCondTiCam.GLOBAL);
        de.getgDatGralOpe().getgOpeCom().setdTiCam(bd("7300.25"));
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-0171", "PRODUCTO DE PRUEBA 171", "2", "45.30"));
        contado(de, "90.60", CMondT.USD, "7300.25");
        return de;
    }

    static DocumentoElectronico facturaTasaConEscala() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000018", "000000018", CMondT.PYG, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        de.getgDtipDE().getgCamItemList().add(item("ITEM-0181", "PRODUCTO DE PRUEBA 181", "1", "110000", TiAfecIVA.GRAVADO, "100", "10.00"));
        contado(de, "110000", CMondT.PYG, null);
        return de;
    }

    static DocumentoElectronico facturaCreditoCuotas() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000019", "000000019", CMondT.PYG, TTipTra.VENTA_MERCADERIA, receptorContribuyente(TiTiOpe.B2B));
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-0191", "PRODUCTO DE PRUEBA 191", "1", "300000"));
        TgCamCond cond = new TgCamCond();
        cond.setiCondOpe(TiCondOpe.CREDITO);
        TgPagCred cred = new TgPagCred();
        cred.setiCondCred(TiCondCred.CUOTA);
        cred.setdCuotas((short) 3);
        List<TgCuotas> cuotas = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            TgCuotas c = new TgCuotas();
            c.setcMoneCuo(CMondT.PYG);
            c.setdMonCuota(bd("100000"));
            c.setdVencCuo(LocalDate.of(2026, 12, 23).plusMonths(i - 1L));
            cuotas.add(c);
        }
        cred.setgCuotasList(cuotas);
        cond.setgPagCred(cred);
        de.getgDtipDE().setgCamCond(cond);
        return de;
    }

    static DocumentoElectronico autofactura() {
        // En la autofactura el receptor es el propio emisor (MT, grupo D).
        TgDatRec r = new TgDatRec();
        r.setiNatRec(TiNatRec.CONTRIBUYENTE);
        r.setiTiOpe(TiTiOpe.B2B);
        r.setcPaisRec(PaisType.PRY);
        r.setiTiContRec(TiTipCont.PERSONA_JURIDICA);
        r.setdRucRec(RUC_EMISOR);
        r.setdDVRec(Short.parseShort(DV_EMISOR));
        r.setdNomRec("EMPRESA FICTICIA DE PRUEBA S.A.");
        DocumentoElectronico de = base(TTiDE.AUTOFACTURA_ELECTRONICA, "0000020", "000000020", CMondT.PYG, TTipTra.COMPRA_PRODUCTOS, r);
        TgCamAE ae = new TgCamAE();
        ae.setiNatVen(TiNatVen.NO_CONTRIBUYENTE);
        ae.setiTipIDVen(TiTipDoc.CEDULA_PARAGUAYA);
        ae.setdNumIDVen("2345678");
        ae.setdNomVen("VENDEDOR FICTICIO");
        ae.setdDirVen("CAMINO FICTICIO");
        ae.setdNumCasVen(0);
        ae.setcDepVen(TDepartamento.CAPITAL);
        ae.setcCiuVen(1);
        ae.setdDesCiuVen("ASUNCION (DISTRITO)");
        ae.setdDirProv("CAMINO FICTICIO");
        ae.setcDepProv(TDepartamento.CAPITAL);
        ae.setcCiuProv(1);
        ae.setdDesCiuProv("ASUNCION (DISTRITO)");
        de.getgDtipDE().setgCamAE(ae);
        de.getgDtipDE().getgCamItemList().add(item("ITEM-0201", "PRODUCTO AGRICOLA DE PRUEBA", "10", "15000", null, null, null));
        contado(de, "150000", CMondT.PYG, null);
        // La autofactura lleva siempre documento asociado (constancia de no contribuyente, MT H002 = 3);
        // sin la lista, setupDE lanza NullPointerException (M0-10).
        TgCamDEAsoc asoc = new TgCamDEAsoc();
        asoc.setiTipDocAso(TiTipDocAso.CONSTANCIA_ELECTRONICA);
        asoc.setiTipCons(TdTipCons.CONSTANCIA_NO_CONTRIBUYENTE);
        List<TgCamDEAsoc> asociados = new ArrayList<>();
        asociados.add(asoc);
        de.setgCamDEAsocList(asociados);
        return de;
    }

    static DocumentoElectronico notaCredito() {
        DocumentoElectronico de = base(TTiDE.NOTA_DE_CREDITO_ELECTRONICA, "0000021", "000000021", CMondT.PYG, null, receptorContribuyente(TiTiOpe.B2B));
        TgCamNCDE nc = new TgCamNCDE();
        nc.setiMotEmi(TiMotEmi.DEVOLUCION);
        de.getgDtipDE().setgCamNCDE(nc);
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-0001", "PRODUCTO DE PRUEBA 1", "1", "55000"));
        TgCamDEAsoc asoc = new TgCamDEAsoc();
        asoc.setiTipDocAso(TiTipDocAso.ELECTRONICO);
        asoc.setdCdCDERef(CDC_FACTURA_SIMPLE);
        List<TgCamDEAsoc> asociados = new ArrayList<>();
        asociados.add(asoc);
        de.setgCamDEAsocList(asociados);
        return de;
    }

    static DocumentoElectronico notaDebito() {
        DocumentoElectronico de = base(TTiDE.NOTA_DE_DEBITO_ELECTRONICA, "0000022", "000000022", CMondT.PYG, null, receptorContribuyente(TiTiOpe.B2B));
        TgCamNCDE nd = new TgCamNCDE();
        nd.setiMotEmi(TiMotEmi.RECUPERO_DE_GASTO);
        de.getgDtipDE().setgCamNCDE(nd);
        de.getgDtipDE().getgCamItemList().add(gravado10("ITEM-0221", "GASTO DE PRUEBA 221", "1", "22000"));
        TgCamDEAsoc asoc = new TgCamDEAsoc();
        asoc.setiTipDocAso(TiTipDocAso.IMPRESO);
        asoc.setdNTimDI("12345678");
        asoc.setdEstDocAso("001");
        asoc.setdPExpDocAso("001");
        asoc.setdNumDocAso("0000099");
        asoc.setiTipoDocAso(TiTIpoDoc.FACTURA);
        asoc.setdFecEmiDI(LocalDate.of(2026, 10, 1));
        List<TgCamDEAsoc> asociados = new ArrayList<>();
        asociados.add(asoc);
        de.setgCamDEAsocList(asociados);
        return de;
    }

    static DocumentoElectronico notaRemision() {
        DocumentoElectronico de = base(TTiDE.NOTA_DE_REMISION_ELECTRONICA, "0000023", "000000023", null, null, receptorContribuyente(TiTiOpe.B2B));
        de.getgOpeDE().setdInfoFisc("Traslado de mercaderias de prueba");
        de.setgTotSub(null);
        TgCamNRE nr = new TgCamNRE();
        nr.setiMotEmiNR(TiMotivTras.TRASLADO_POR_VENTAS);
        nr.setiRespEmiNR(TiRespEmiNR.EMISOR_FACTURA);
        nr.setdKmR(25);
        nr.setdFecEm(LocalDate.of(2026, 11, 24));
        de.getgDtipDE().setgCamNRE(nr);

        TgCamItem it = new TgCamItem();
        it.setdCodInt("ITEM-0231");
        it.setdDesProSer("MERCADERIA DE PRUEBA 231");
        it.setcUniMed(TcUniMed.UNI);
        it.setdCantProSer(bd("12"));
        de.getgDtipDE().getgCamItemList().add(it);

        TgTransp tr = new TgTransp();
        tr.setiTipTrans(TiTTrans.PROPIO);
        tr.setiModTrans(TiModTrans.TERRESTRE);
        tr.setiRespFlete(TiRespFlete.EMISOR_FACTURA_ELECTRONICA);
        tr.setdIniTras(LocalDate.of(2026, 11, 24));
        tr.setdFinTras(LocalDate.of(2026, 11, 25));
        TgCamSal sal = new TgCamSal();
        sal.setdDirLocSal("CALLE FICTICIA");
        sal.setdNumCasSal((short) 123);
        sal.setcDepSal(TDepartamento.CAPITAL);
        sal.setcCiuSal(1);
        sal.setdDesCiuSal("ASUNCION (DISTRITO)");
        tr.setgCamSal(sal);
        TgCamEnt ent = new TgCamEnt();
        ent.setdDirLocEnt("RUTA FICTICIA KM 1");
        ent.setdNumCasEnt((short) 0);
        ent.setcDepEnt(TDepartamento.CAPITAL);
        ent.setcCiuEnt(1);
        ent.setdDesCiuEnt("ASUNCION (DISTRITO)");
        List<TgCamEnt> entregas = new ArrayList<>();
        entregas.add(ent);
        tr.setgCamEntList(entregas);
        TgVehTras veh = new TgVehTras();
        veh.setdTiVehTras("CAMION");
        veh.setdMarVeh("MARCA FICTICIA");
        veh.setdTipIdenVeh((short) 2);
        veh.setdNroMatVeh("AAA000");
        List<TgVehTras> vehiculos = new ArrayList<>();
        vehiculos.add(veh);
        tr.setgVehTrasList(vehiculos);
        TgCamTrans trans = new TgCamTrans();
        trans.setiNatTrans(TiNatRec.CONTRIBUYENTE);
        trans.setdNomTrans("EMPRESA FICTICIA DE PRUEBA S.A.");
        trans.setdRucTrans(RUC_EMISOR);
        trans.setdDVTrans(Short.parseShort(DV_EMISOR));
        trans.setdNumIDChof("3456789");
        trans.setdNomChof("CHOFER FICTICIO");
        trans.setdDomFisc("CALLE FICTICIA");
        trans.setdDirChof("CALLE FICTICIA");
        tr.setgCamTrans(trans);
        de.getgDtipDE().setgTransp(tr);
        return de;
    }

    // ------------------------------------------------------------------ B2F, inmobiliaria y anticipos (2026-11-24)

    /**
     * DE-24: DE-17 como B2F (operación con el exterior), con dirección (D213) y pasaporte, sin número
     * de documento (D210 solo es obligatorio fuera de B2F; D210a del MT v150 decía «no informar» en B2F).
     */
    static DocumentoElectronico facturaB2fPasaporteSinNumero() {
        DocumentoElectronico de = facturaExtranjero();
        de.getgTimb().setdNumDoc("0000024");
        de.getgOpeDE().setdCodSeg("000000024");
        TgDatRec r = de.getgDatGralOpe().getgDatRec();
        r.setiTiOpe(TiTiOpe.B2F);
        r.setdNumIDRec(null);
        r.setdDirRec("AVENIDA FICTICIA 123");
        return de;
    }

    /**
     * B2F sin tipo de documento: el negativo de D208g (NT 023, 1335; ERROR desde el 2026-11-24) y la
     * guarda de E3-07 (sin validación no da NPE). No es un caso del catálogo (no valida); lo usan los
     * negativos y la prueba en vivo.
     */
    public static DocumentoElectronico facturaB2fSinTipoDocumento() {
        DocumentoElectronico de = facturaB2fPasaporteSinNumero();
        de.getgDatGralOpe().getgDatRec().setiTipIDRec(null);
        return de;
    }

    /** Lote: gravado parcial, IVA 5 % sobre el 30 % (régimen de inmuebles). */
    private static TgCamItem lote(String cantidad, String precio) {
        return item("LOTE-015", "LOTE 15 MANZANA B - CUOTA", cantidad, precio, TiAfecIVA.GRAVADO_PARCIAL, "30", "5");
    }

    /** DE-25/DE-26: inmobiliaria en PYG con los tres tipos de ítem; con redondeo SEDECO (29) o sin. */
    static DocumentoElectronico inmobiliariaPyg(boolean redondeo) {
        String num = redondeo ? "0000025" : "0000026";
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, num, "00" + num, CMondT.PYG, TTipTra.VENTA_ACTIVO_FIJO, receptorContribuyente(TiTiOpe.B2B));
        List<TgCamItem> items = de.getgDtipDE().getgCamItemList();
        items.add(lote("1", "12345679"));
        items.add(gravado10("ADM-001", "GASTOS DE ADMINISTRACION", "1", "1234567"));
        items.add(item("EXE-001", "TASA MUNICIPAL (EXENTA)", "3", "111111", TiAfecIVA.EXENTO, "0", "0"));
        contado(de, redondeo ? "13913550" : "13913579", CMondT.PYG, null);
        if (!redondeo) {
            de.setgTotSub(new TgTotSub(false));
        }
        return de;
    }

    /** DE-27/28/29: inmobiliaria en USD; {@code exento} 100.00 ⇒ total 1680,24 (baja), 100.16 ⇒ 1680,40 (sube). */
    static DocumentoElectronico inmobiliariaUsd(boolean redondeo, String exento) {
        String num = !redondeo ? "0000028" : exento.equals("100.00") ? "0000027" : "0000029";
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, num, "00" + num, CMondT.USD, TTipTra.VENTA_ACTIVO_FIJO, receptorContribuyente(TiTiOpe.B2B));
        de.getgDatGralOpe().getgOpeCom().setdCondTiCam(TdCondTiCam.GLOBAL);
        de.getgDatGralOpe().getgOpeCom().setdTiCam(bd("7300.25"));
        List<TgCamItem> items = de.getgDtipDE().getgCamItemList();
        items.add(lote("1", "1234.57"));
        items.add(gravado10("ADM-001", "GASTOS DE ADMINISTRACION", "1", "345.67"));
        items.add(item("EXE-001", "TASA MUNICIPAL (EXENTA)", "1", exento, TiAfecIVA.EXENTO, "0", "0"));
        contado(de, "1680.00", CMondT.USD, "7300.25");
        if (!redondeo) {
            de.setgTotSub(new TgTotSub(false));
        }
        return de;
    }

    /** DE-30: factura de anticipo (iTipTra 9) de 1.100.000 a cuenta del lote. */
    static DocumentoElectronico facturaDeAnticipo() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000030", "000000030", CMondT.PYG, TTipTra.ANTICIPO, receptorContribuyente(TiTiOpe.B2B));
        de.getgDtipDE().getgCamItemList().add(item("LOTE-015", "ANTICIPO LOTE 15 MANZANA B", "1", "1100000",
                TiAfecIVA.GRAVADO_PARCIAL, "30", "5"));
        contado(de, "1100000", CMondT.PYG, null);
        de.setgTotSub(new TgTotSub(false));
        return de;
    }

    /** CDC de la factura de anticipo DE-30 (documento asociado y {@code dCDCAnticipo} de DE-31/DE-32). */
    public static String cdcAnticipo() {
        try {
            return facturaDeAnticipo().obtenerCDC();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void asociarAnticipo(DocumentoElectronico de) {
        TgCamDEAsoc a = new TgCamDEAsoc();
        a.setiTipDocAso(TiTipDocAso.ELECTRONICO);
        a.setdCdCDERef(cdcAnticipo());
        List<TgCamDEAsoc> l = new ArrayList<>();
        l.add(a);
        de.setgCamDEAsocList(l);
    }

    /** DE-31: 2 × 1.100.000; el anticipo de DE-30 se aplica por ítem (550.000 por unidad). */
    static DocumentoElectronico facturaFinalAnticipoPorItem() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000031", "000000031", CMondT.PYG, TTipTra.VENTA_ACTIVO_FIJO, receptorContribuyente(TiTiOpe.B2B));
        de.getgDatGralOpe().getgOpeCom().setiCondAnt(TiCondAnt.ANTICIPO_POR_ITEM);
        TgCamItem it = lote("2", "1100000");
        it.getgValorItem().getgValorRestaItem().setdAntPreUniIt(bd("550000"));
        it.setdCDCAnticipo(cdcAnticipo());
        de.getgDtipDE().getgCamItemList().add(it);
        asociarAnticipo(de);
        contado(de, "1100000", CMondT.PYG, null);
        de.setgTotSub(new TgTotSub(false));
        return de;
    }

    /** DE-32: anticipo global, el mismo por unidad en cada ítem (EA007). */
    static DocumentoElectronico facturaFinalAnticipoGlobal() {
        DocumentoElectronico de = base(TTiDE.FACTURA_ELECTRONICA, "0000032", "000000032", CMondT.PYG, TTipTra.VENTA_ACTIVO_FIJO, receptorContribuyente(TiTiOpe.B2B));
        de.getgDatGralOpe().getgOpeCom().setiCondAnt(TiCondAnt.ANTICIPO_GLOBAL);
        TgCamItem a = lote("3", "1000000");
        a.getgValorItem().getgValorRestaItem().setdAntGloPreUniIt(bd("100000"));
        a.setdCDCAnticipo(cdcAnticipo());
        TgCamItem b = gravado10("ADM-001", "GASTOS DE ADMINISTRACION", "1", "500000");
        b.getgValorItem().getgValorRestaItem().setdAntGloPreUniIt(bd("100000"));
        b.setdCDCAnticipo(cdcAnticipo());
        de.getgDtipDE().getgCamItemList().add(a);
        de.getgDtipDE().getgCamItemList().add(b);
        asociarAnticipo(de);
        contado(de, "3100000", CMondT.PYG, null);
        de.setgTotSub(new TgTotSub(false));
        return de;
    }

    // ------------------------------------------------------------------ eventos

    private static EventosDE evento(String id, TgGroupTiEvt grupo) {
        TrGesEve ges = new TrGesEve();
        ges.setId(id);
        ges.setdFecFirma(FECHA_FIRMA);
        ges.setgGroupTiEvt(grupo);
        EventosDE ev = new EventosDE();
        List<TrGesEve> l = new ArrayList<>();
        l.add(ges);
        ev.setrGesEveList(l);
        return ev;
    }

    static EventosDE eventoCancelacion() {
        TrGeVeCan can = new TrGeVeCan();
        can.setId(CDC_FACTURA_SIMPLE);
        can.setmOtEve("Cancelacion de prueba del documento");
        TgGroupTiEvt g = new TgGroupTiEvt();
        g.setrGeVeCan(can);
        return evento("1", g);
    }

    static EventosDE eventoInutilizacion() {
        TrGeVeInu inu = new TrGeVeInu();
        inu.setdNumTim(TIMBRADO);
        inu.setdEst("001");
        inu.setdPunExp("001");
        inu.setdNumIn("0000050");
        inu.setdNumFin("0000055");
        inu.setiTiDE(TTiDE.FACTURA_ELECTRONICA);
        inu.setmOtEve("Inutilizacion de prueba de numeracion");
        TgGroupTiEvt g = new TgGroupTiEvt();
        g.setrGeVeInu(inu);
        return evento("2", g);
    }

    static EventosDE eventoConformidad() {
        TrGeVeConf conf = new TrGeVeConf();
        conf.setId(CDC_FACTURA_SIMPLE);
        conf.setiTipConf(TiTipConf.CONFORMIDAD_PARCIAL);
        conf.setdFecRecep(FECHA_EMISION.plusDays(1));
        TgGroupTiEvt g = new TgGroupTiEvt();
        g.setrGeVeConf(conf);
        return evento("3", g);
    }

    static EventosDE eventoDisconformidad() {
        TrGeVeDisconf dis = new TrGeVeDisconf();
        dis.setId(CDC_FACTURA_SIMPLE);
        dis.setmOtEve("Disconformidad de prueba del receptor");
        TgGroupTiEvt g = new TgGroupTiEvt();
        g.setrGeVeDisconf(dis);
        return evento("4", g);
    }

    /**
     * Nominación de un receptor no contribuyente con dirección. Con {@code distrito == null} el fork
     * evalúa {@code cDisRec != 0} antes de {@code != null} y lanza {@code NullPointerException}
     * (E0-18); la variante sin distrito la usa el test de línea base, no el catálogo.
     *
     * @param distrito código de distrito, o {@code null}.
     * @return evento armado.
     */
    public static EventosDE eventoNominacionConDireccion(Short distrito) {
        TrGeVeNom nom = new TrGeVeNom();
        nom.setId("01801234565001001000001622026112310000000163");
        nom.setmOtEve("Nominacion de prueba del receptor");
        nom.setiNatRec((short) 2);
        nom.setiTiOpe((short) 2);
        nom.setcPaisRec("PRY");
        nom.setdDesPaisRe("Paraguay");
        nom.setiTipIDRec((short) 1);
        nom.setdDTipIDRec("Cedula paraguaya");
        nom.setdNumIDRec("1234567");
        nom.setdNomRec("PERSONA FICTICIA");
        nom.setdDirRec("CALLE FICTICIA 2");
        nom.setdNumCasRec(100);
        nom.setcDepRec((short) 1);
        nom.setdDesDepRec("CAPITAL");
        nom.setcDisRec(distrito);
        nom.setdDesDisRec(distrito == null ? null : "ASUNCION (DISTRITO)");
        nom.setcCiuRec((short) 1);
        nom.setdDesCiuRec("ASUNCION (DISTRITO)");
        nom.setdEmailRec("persona@example.com");
        TgGroupTiEvt g = new TgGroupTiEvt();
        g.setrGeVeNom(nom);
        return evento("5", g);
    }

    static EventosDE eventoNominacionContribuyente() {
        TrGeVeNom nom = new TrGeVeNom();
        nom.setId("01801234565001001000001622026112310000000163");
        nom.setmOtEve("Nominacion de prueba de un contribuyente");
        nom.setiNatRec((short) 1);
        nom.setiTiOpe((short) 1);
        nom.setcPaisRec("PRY");
        nom.setdDesPaisRe("Paraguay");
        nom.setiTiContRec((short) 2);
        nom.setdRucRec("80654321");
        nom.setdDVRec((short) 3);
        nom.setdNomRec("CLIENTE FICTICIO S.A.");
        TgGroupTiEvt g = new TgGroupTiEvt();
        g.setrGeVeNom(nom);
        return evento("6", g);
    }

    // ------------------------------------------------------------------ casos para el ambiente de pruebas

    /**
     * Factura mínima para el ambiente de <b>pruebas</b> de SIFEN (§2.9): la de DE-05 con el emisor y
     * el timbrado de prueba del archivo de {@code SifenTestEnv}, razón social de prueba que exige la
     * DNIT en ese ambiente y fecha/número variables (un CDC nuevo por corrida).
     *
     * @param ruc RUC del emisor de prueba.
     * @param dv DV del emisor.
     * @param timbrado timbrado de prueba.
     * @param inicioTimbrado inicio de vigencia del timbrado.
     * @param establecimiento establecimiento.
     * @param punto punto de expedición.
     * @param numeroDocumento número (7 dígitos).
     * @param fecha fecha de emisión y firma.
     * @return DE listo para firmar.
     */
    public static DocumentoElectronico facturaParaAmbienteDePruebas(String ruc, String dv, int timbrado, LocalDate inicioTimbrado,
                                                                     String establecimiento, String punto, String numeroDocumento,
                                                                     LocalDateTime fecha) {
        DocumentoElectronico de = facturaPygB2cCedula();
        de.setdFecFirma(fecha);
        de.getgDatGralOpe().setdFeEmiDE(fecha);
        de.getgOpeDE().setdCodSeg(String.format("%09d", Long.parseLong(numeroDocumento)));
        TgTimb t = de.getgTimb();
        t.setdNumTim(timbrado);
        t.setdFeIniT(inicioTimbrado);
        t.setdEst(establecimiento);
        t.setdPunExp(punto);
        t.setdNumDoc(numeroDocumento);
        TgEmis e = de.getgDatGralOpe().getgEmis();
        e.setdRucEm(ruc);
        e.setdDVEmi(dv);
        e.setdNomEmi("DE generado en ambiente de prueba - sin valor comercial ni fiscal");
        return de;
    }
}

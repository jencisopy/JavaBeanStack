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
package org.javabeanstack.sifen.core.validation.support;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.fields.request.de.TdDatGralOpe;
import org.javabeanstack.sifen.core.fields.request.de.TgCamCond;
import org.javabeanstack.sifen.core.fields.request.de.TgCamItem;
import org.javabeanstack.sifen.core.fields.request.de.TgDatRec;
import org.javabeanstack.sifen.core.fields.request.de.TgDtipDE;
import org.javabeanstack.sifen.core.fields.request.de.TgOpeCom;
import org.javabeanstack.sifen.core.fields.request.de.TgPaConEIni;
import org.javabeanstack.sifen.core.fields.request.de.TgTransp;
import org.javabeanstack.sifen.core.fields.request.de.TgValorItem;
import org.javabeanstack.sifen.core.fields.request.de.TgValorRestaItem;
import org.javabeanstack.sifen.core.types.CMondT;

/**
 * Variables de condición del catálogo (sección «Variables de condición») leídas del DE sin tocar un
 * nulo: cada método devuelve el código del MT ({@code getVal()}) o {@code 0} si el dato o algún
 * eslabón de su ruta falta. Con {@code 0} las reglas condicionadas a esa variable no se evalúan (la
 * ausencia ya la reporta la regla de presencia), así un faltante da un error y no una cascada.
 *
 * <p>También calcula el {@code total} de la operación con la fórmula de {@code TgTotSub}
 * (D208c, JBS-E-30, JBS-E-51): los totales del DE los escribe la librería al generar y antes de eso
 * no están cargados.</p>
 */
public final class DEData {
    /** Puesta en producción de SIFEN (D002a, E909a). */
    public static final java.time.LocalDateTime SIFEN_START = java.time.LocalDateTime.of(2018, 11, 22, 0, 0);

    /** Umbral del receptor innominado en guaraníes (NT 024, D208c). */
    public static final BigDecimal UNNAMED_RECEIVER_THRESHOLD = new BigDecimal("7000000");

    private DEData() {
    }

    /**
     * Devuelve el gDatGralOpe del DE sin tocar un nulo.
     *
     * @param de documento electrónico.
     * @return el gDatGralOpe, o {@code null} si falta o falta un padre.
     */
    public static TdDatGralOpe gDatGralOpe(DocumentoElectronico de) {
        return de.getgDatGralOpe();
    }

    /**
     * Devuelve el gOpeCom del DE sin tocar un nulo.
     *
     * @param de documento electrónico.
     * @return el gOpeCom, o {@code null} si falta o falta un padre.
     */
    public static TgOpeCom gOpeCom(DocumentoElectronico de) {
        TdDatGralOpe g = de.getgDatGralOpe();
        return g == null ? null : g.getgOpeCom();
    }

    /**
     * Devuelve el gDatRec del DE sin tocar un nulo.
     *
     * @param de documento electrónico.
     * @return el gDatRec, o {@code null} si falta o falta un padre.
     */
    public static TgDatRec gDatRec(DocumentoElectronico de) {
        TdDatGralOpe g = de.getgDatGralOpe();
        return g == null ? null : g.getgDatRec();
    }

    /**
     * Devuelve el gCamCond del DE sin tocar un nulo.
     *
     * @param de documento electrónico.
     * @return el gCamCond, o {@code null} si falta o falta un padre.
     */
    public static TgCamCond gCamCond(DocumentoElectronico de) {
        TgDtipDE d = de.getgDtipDE();
        return d == null ? null : d.getgCamCond();
    }

    /**
     * Devuelve el grupo de campos específicos por sector.
     *
     * @param de documento electrónico.
     * @return el gCamEsp, o {@code null}.
     */
    public static org.javabeanstack.sifen.core.fields.request.de.TgCamEsp gCamEsp(DocumentoElectronico de) {
        TgDtipDE d = de.getgDtipDE();
        return d == null ? null : d.getgCamEsp();
    }

    /**
     * Devuelve el gTransp del DE sin tocar un nulo.
     *
     * @param de documento electrónico.
     * @return el gTransp, o {@code null} si falta o falta un padre.
     */
    public static TgTransp gTransp(DocumentoElectronico de) {
        TgDtipDE d = de.getgDtipDE();
        return d == null ? null : d.getgTransp();
    }

    /**
     * {@code C002} tipo de DE.
     *
     * @param de documento electrónico.
     * @return el código del MT, o 0 si falta.
     */
    public static int iTiDE(DocumentoElectronico de) {
        return de.getgTimb() == null || de.getgTimb().getiTiDE() == null ? 0 : de.getgTimb().getiTiDE().getVal();
    }

    /**
     * {@code D011} tipo de transacción.
     *
     * @param de documento electrónico.
     * @return el código del MT, o 0 si falta.
     */
    public static int iTipTra(DocumentoElectronico de) {
        TgOpeCom c = gOpeCom(de);
        return c == null || c.getiTipTra() == null ? 0 : c.getiTipTra().getVal();
    }

    /**
     * {@code D013} tipo de impuesto.
     *
     * @param de documento electrónico.
     * @return el código del MT, o 0 si falta.
     */
    public static int iTImp(DocumentoElectronico de) {
        TgOpeCom c = gOpeCom(de);
        return c == null || c.getiTImp() == null ? 0 : c.getiTImp().getVal();
    }

    /**
     * {@code D015} moneda de la operación, o {@code null}.
     *
     * @param de documento electrónico.
     * @return el resultado.
     */
    public static CMondT cMoneOpe(DocumentoElectronico de) {
        TgOpeCom c = gOpeCom(de);
        return c == null ? null : c.getcMoneOpe();
    }

    /**
     * {@code D017} condición del tipo de cambio.
     *
     * @param de documento electrónico.
     * @return el código del MT, o 0 si falta.
     */
    public static int dCondTiCam(DocumentoElectronico de) {
        TgOpeCom c = gOpeCom(de);
        return c == null || c.getdCondTiCam() == null ? 0 : c.getdCondTiCam().getVal();
    }

    /**
     * {@code D201} naturaleza del receptor.
     *
     * @param de documento electrónico.
     * @return el código del MT, o 0 si falta.
     */
    public static int iNatRec(DocumentoElectronico de) {
        TgDatRec r = gDatRec(de);
        return r == null || r.getiNatRec() == null ? 0 : r.getiNatRec().getVal();
    }

    /**
     * {@code D202} tipo de operación.
     *
     * @param de documento electrónico.
     * @return el código del MT, o 0 si falta.
     */
    public static int iTiOpe(DocumentoElectronico de) {
        TgDatRec r = gDatRec(de);
        return r == null || r.getiTiOpe() == null ? 0 : r.getiTiOpe().getVal();
    }

    /**
     * {@code D208} tipo de documento del receptor.
     *
     * @param de documento electrónico.
     * @return el código del MT, o 0 si falta.
     */
    public static int iTipIDRec(DocumentoElectronico de) {
        TgDatRec r = gDatRec(de);
        return r == null || r.getiTipIDRec() == null ? 0 : r.getiTipIDRec().getVal();
    }

    /**
     * {@code E011} indicador de presencia.
     *
     * @param de documento electrónico.
     * @return el código del MT, o 0 si falta.
     */
    public static int iIndPres(DocumentoElectronico de) {
        TgDtipDE d = de.getgDtipDE();
        return d == null || d.getgCamFE() == null || d.getgCamFE().getiIndPres() == null ? 0
                : d.getgCamFE().getiIndPres().getVal();
    }

    /**
     * {@code E601} condición de la operación.
     *
     * @param de documento electrónico.
     * @return el código del MT, o 0 si falta.
     */
    public static int iCondOpe(DocumentoElectronico de) {
        TgCamCond c = gCamCond(de);
        return c == null || c.getiCondOpe() == null ? 0 : c.getiCondOpe().getVal();
    }

    /**
     * {@code E501} motivo de la remisión.
     *
     * @param de documento electrónico.
     * @return el código del MT, o 0 si falta.
     */
    public static int iMotEmiNR(DocumentoElectronico de) {
        TgDtipDE d = de.getgDtipDE();
        return d == null || d.getgCamNRE() == null || d.getgCamNRE().getiMotEmiNR() == null ? 0
                : d.getgCamNRE().getiMotEmiNR().getVal();
    }

    /**
     * {@code E903} modalidad del transporte.
     *
     * @param de documento electrónico.
     * @return el código del MT, o 0 si falta.
     */
    public static int iModTrans(DocumentoElectronico de) {
        TgTransp t = gTransp(de);
        return t == null || t.getiModTrans() == null ? 0 : t.getiModTrans().getVal();
    }

    /**
     * Ítems del DE; nunca {@code null}.
     *
     * @param de documento electrónico.
     * @return la lista; nunca {@code null}.
     */
    public static List<TgCamItem> items(DocumentoElectronico de) {
        TgDtipDE d = de.getgDtipDE();
        if (d == null || d.getgCamItemList() == null) {
            return Collections.emptyList();
        }
        return d.getgCamItemList();
    }

    /**
     * Formas de pago; nunca {@code null}.
     *
     * @param de documento electrónico.
     * @return la lista; nunca {@code null}.
     */
    public static List<TgPaConEIni> payments(DocumentoElectronico de) {
        TgCamCond c = gCamCond(de);
        if (c == null || c.getgPaConEIniList() == null) {
            return Collections.emptyList();
        }
        return c.getgPaConEIniList();
    }

    /**
     * {@code true} si algún pago es de tipo {@code tipo} ({@code E606}).
     *
     * @param de documento electrónico.
     * @param tipo código de la variable.
     * @return {@code true} si se cumple.
     */
    public static boolean hasPayment(DocumentoElectronico de, int tipo) {
        for (TgPaConEIni p : payments(de)) {
            if (p != null && p.getiTiPago() != null && p.getiTiPago().getVal() == tipo) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code true} si la moneda de la operación es guaraníes (sin moneda se toma guaraníes).
     *
     * @param de documento electrónico.
     * @return {@code true} si se cumple.
     */
    public static boolean inGuaranies(DocumentoElectronico de) {
        CMondT m = cMoneOpe(de);
        return m == null || m == CMondT.PYG;
    }

    private static BigDecimal cero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * Total de un ítem como lo calcula {@code TgValorRestaItem}: (precio − descuento − descuento global −
     * anticipo − anticipo global) × cantidad; en la autofactura, precio × cantidad.
     *
     * @param de   documento electrónico.
     * @param item ítem del DE.
     * @return el total, o {@code null} si faltan el precio o la cantidad.
     */
    public static BigDecimal itemTotal(DocumentoElectronico de, TgCamItem item) {
        if (item == null || item.getgValorItem() == null) {
            return null;
        }
        TgValorItem v = item.getgValorItem();
        if (v.getdPUniProSer() == null || item.getdCantProSer() == null) {
            return null;
        }
        BigDecimal precio = v.getdPUniProSer();
        TgValorRestaItem r = v.getgValorRestaItem();
        if (iTiDE(de) != 4 && r != null) {
            precio = precio.subtract(cero(r.getdDescItem())).subtract(cero(r.getdDescGloItem()))
                    .subtract(cero(r.getdAntPreUniIt())).subtract(cero(r.getdAntGloPreUniIt()));
        }
        return precio.multiply(item.getdCantProSer());
    }

    /**
     * {@code total} de la operación en su moneda: Σ total de los ítems + comisión (salvo en la autofactura,
     * que no la informa), sin el redondeo
     * (el ajuste es de menos de 50 Gs o 0,50; las reglas que comparan contra el total lo toleran).
     *
     * @param de documento electrónico.
     * @return el total, o {@code null} si algún ítem no tiene precio o cantidad.
     */
    public static BigDecimal operationTotal(DocumentoElectronico de) {
        BigDecimal total = BigDecimal.ZERO;
        for (TgCamItem item : items(de)) {
            BigDecimal t = itemTotal(de, item);
            if (t == null) {
                return null;
            }
            total = total.add(t);
        }
        // E12-01: como TgTotSub, la autofactura no suma la comisión (F001: con C002 = 4 no se informa F025)
        if (de.getgTotSub() != null && iTiDE(de) != 4) {
            total = total.add(cero(de.getgTotSub().getdComi()));
        }
        return total;
    }

    /**
     * {@code total} en guaraníes: el de la operación, o × {@code dTiCam} (tipo de cambio global), o
     * Σ total del ítem × {@code dTiCamIt} (por ítem).
     *
     * @param de documento electrónico.
     * @return el total en guaraníes, o {@code null} si falta algún dato para calcularlo.
     */
    public static BigDecimal totalInGuaranies(DocumentoElectronico de) {
        if (inGuaranies(de)) {
            return operationTotal(de);
        }
        if (dCondTiCam(de) == 1) {
            BigDecimal total = operationTotal(de);
            BigDecimal cambio = gOpeCom(de).getdTiCam();
            return total == null || cambio == null ? null : total.multiply(cambio);
        }
        if (dCondTiCam(de) == 2) {
            BigDecimal total = BigDecimal.ZERO;
            for (TgCamItem item : items(de)) {
                BigDecimal t = itemTotal(de, item);
                if (t == null || item.getgValorItem().getdTiCamIt() == null) {
                    return null;
                }
                total = total.add(t.multiply(item.getgValorItem().getdTiCamIt()));
            }
            return total;
        }
        return null;
    }

    /**
     * Tolerancia de las sumas contra el total: el redondeo SEDECO (50 Gs; 0,50 en otra moneda).
     *
     * @param de documento electrónico.
     * @return el importe, o {@code null} si falta un dato.
     */
    public static BigDecimal roundingTolerance(DocumentoElectronico de) {
        return inGuaranies(de) ? new BigDecimal("50") : new BigDecimal("0.5");
    }
}

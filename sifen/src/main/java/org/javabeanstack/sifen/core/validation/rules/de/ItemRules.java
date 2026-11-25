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
package org.javabeanstack.sifen.core.validation.rules.de;

import static org.javabeanstack.sifen.core.validation.support.ValueChecks.*;

import java.util.List;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.fields.request.de.TgCamItem;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;
import java.math.BigDecimal;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.validation.support.Legends;

/**
 * Grupo E del MT, E8: gCamItem[] (el ítem).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class ItemRules extends RuleSet<DocumentoElectronico, TgCamItem> {

    ItemRules() {
        super("E-gCamItem", RuleGroup.E, "gDtipDE.gCamItem[]");
        declare(rule("JBS-E-58").field("dCodInt").error().showValue(),
                onNull(s -> s.e() != null && present(s.e().getdCodInt()), s -> null));
        declare(rule("JBS-E-59").field("dCodInt").error().showValue().source(RuleSource.NT_009),
                length(TgCamItem::getdCodInt, 1, 50));
        declare(rule("JBS-E-60").field("dParAranc").error().showValue(),
                passes(s -> s.e().getdParAranc() == 0 || (s.e().getdParAranc() > 0 && digitCount(s.e().getdParAranc()) == 4), s -> s.e().getdParAranc()));
        declare(rule("JBS-E-61").field("dNCM").error().showValue(),
                passes(s -> s.e().getdNCM() == 0 || (s.e().getdNCM() > 0 && digitCount(s.e().getdNCM()) >= 6 && digitCount(s.e().getdNCM()) <= 8), s -> s.e().getdNCM()));
        declare(rule("JBS-E-62").field("dDncpG").warning().showValue().source(RuleSource.NT_026),
                passes(s -> !present(s.e().getdDncpG()) || ope(s) == 3 || ope(s) == 0));
        declare(rule("JBS-E-63").field("dDncpG").error().showValue().source(RuleSource.NT_026),
                passes(s -> !present(s.e().getdDncpG()) || ope(s) != 3 || s.e().getdDncpG().length() == 8, s -> s.e().getdDncpG()));
        // §6.2: de baja; replicaba E705, que la NT 026 excluyó (en producción desde el 2025-06-16)
        declare(rule("JBS-E-64").field("dDncpE").error().showValue().source(RuleSource.NT_026)
                        .until(java.time.LocalDate.of(2025, 6, 16), "NT 026: se excluye E705, dDncpE es opcional aunque haya dDncpG"),
                passes(s -> !present(s.e().getdDncpG()) || ope(s) != 3 || present(s.e().getdDncpE())));
        // I6-09: el largo de dDncpE se exige cuando viene, haya o no dDncpG (NT 026)
        declare(rule("JBS-E-65").field("dDncpE").error().showValue().source(RuleSource.NT_026),
                passes(s -> !present(s.e().getdDncpE()) || ope(s) != 3 || ValueChecks.length(s.e().getdDncpE(), 3, 4), s -> s.e().getdDncpE()));
        // I6-09: como JBS-E-62 para dDncpG, aviso de que dDncpE fuera de B2G no se envía
        declare(rule("JBS-E-194").field("dDncpE").warning().showValue().source(RuleSource.NT_026),
                passes(s -> !present(s.e().getdDncpE()) || ope(s) == 3 || ope(s) == 0));
        declare(rule("JBS-E-66").field("dGtin").error().showValue(),
                passes(s -> gtin(s.e().getdGtin()), s -> s.e().getdGtin()));
        declare(rule("JBS-E-67").field("dGtinPq").error().showValue(),
                passes(s -> gtin(s.e().getdGtinPq()), s -> s.e().getdGtinPq()));
        declare(rule("JBS-E-68").field("dDesProSer").error().showValue(),
                required(TgCamItem::getdDesProSer));
        declare(rule("JBS-E-69").field("dDesProSer").error().showValue().source(RuleSource.NT_009),
                length(TgCamItem::getdDesProSer, 1, 2000));
        declare(rule("JBS-E-70").field("dDesProSer").warning().showValue().source(RuleSource.GUIA_PRUEBAS_2026),
                passes(s -> !present(s.e().getdDesProSer()) || s.index(0) != 0 || s.ctx().getEnvironment() != SifenConfig.Environment.DEV
                || Legends.isTestLegend(s.e().getdDesProSer(), false)));
        declare(rule("JBS-E-71").field("cUniMed").error().showValue(),
                required(TgCamItem::getcUniMed));
        declareNotEvaluated(rule("E710").field("dDesUniMed").error().dCodRes("1802").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-E-72").field("dCantProSer").error().showValue(),
                required(TgCamItem::getdCantProSer));
        declare(rule("JBS-E-73").field("dCantProSer").error().showValue().source(RuleSource.NT_023),
                decimal(TgCamItem::getdCantProSer, 10, 8, true));
        declareNotEvaluated(rule("E713").field("dDesPaisOrig").error().dCodRes("1804").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-E-74").field("dInfItem").error().showValue(),
                length(TgCamItem::getdInfItem, 1, 500));
        declare(rule("E715").field("cRelMerc").error().dCodRes("1805").showValue(),
                requiredIf(s -> tipo(s) == 7, TgCamItem::getcRelMerc));
        declare(rule("JBS-E-75").field("cRelMerc").error().dCodRes("1807").showValue(),
                forbiddenIf(s -> tipo(s) != 0 && tipo(s) != 7, TgCamItem::getcRelMerc));
        declareNotEvaluated(rule("E716").field("dDesRelMerc").error().dCodRes("1806").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("E717").field("dCanQuiMer").error().dCodRes("1808").showValue(),
                passes(s -> !(s.e().getcRelMerc() != null && s.e().getdCanQuiMer() == null && s.e().getdPorQuiMer() == null)));
        declare(rule("JBS-E-76").field("dCanQuiMer").error().showValue(),
                decimal(TgCamItem::getdCanQuiMer, 10, 4, false));
        declare(rule("JBS-E-77").field("dPorQuiMer").error().showValue(),
                passes(s -> ValueChecks.decimal(s.e().getdPorQuiMer(), 3, 8) && nonNegative(s.e().getdPorQuiMer())
                && (s.e().getdPorQuiMer() == null || s.e().getdPorQuiMer().compareTo(CIEN) <= 0), s -> s.e().getdPorQuiMer()));
        // E3-24: MT E719, obligatorio en la factura que APLICA un anticipo (no en la FE de anticipo,
        // iTipTra 9, como pedía la línea base): un ítem con anticipo particular o global lleva el CDC.
        declare(rule("JBS-E-78").field("dCDCAnticipo").error().showValue(),
                requiredIf(s -> aplicaAnticipo(s.e()), TgCamItem::getdCDCAnticipo));
        declare(rule("JBS-E-79").field("dCDCAnticipo").error().showValue(),
                passes(s -> !present(s.e().getdCDCAnticipo()) || (validCdc(s.e().getdCDCAnticipo()) && s.e().getdCDCAnticipo().startsWith("01"))));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgCamItem>> subjects(DocumentoElectronico raiz) {
        return raiz.getgDtipDE() == null ? none() : perItem(raiz.getgDtipDE().getgCamItemList());
    }

    private static final BigDecimal CIEN = new BigDecimal("100");

    private static int tipo(Subject<DocumentoElectronico, TgCamItem> s) {
        return DEData.iTiDE(s.root());
    }

    private static int ope(Subject<DocumentoElectronico, TgCamItem> s) {
        return DEData.iTiOpe(s.root());
    }

    private static boolean gtin(long gtin) {
        if (gtin == 0) {
            return true;
        }
        int c = digitCount(gtin);
        return gtin > 0 && (c == 8 || c == 12 || c == 13 || c == 14);
    }

    /** E3-24: el ítem aplica un anticipo (EA006 o EA007 mayor que cero). */
    private static boolean aplicaAnticipo(TgCamItem item) {
        if (item == null || item.getgValorItem() == null || item.getgValorItem().getgValorRestaItem() == null) {
            return false;
        }
        java.math.BigDecimal particular = item.getgValorItem().getgValorRestaItem().getdAntPreUniIt();
        java.math.BigDecimal global = item.getgValorItem().getgValorRestaItem().getdAntGloPreUniIt();
        return (particular != null && particular.signum() > 0) || (global != null && global.signum() > 0);
    }
}

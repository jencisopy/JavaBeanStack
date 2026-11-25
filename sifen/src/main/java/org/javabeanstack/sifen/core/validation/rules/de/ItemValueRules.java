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
import org.javabeanstack.sifen.core.fields.request.de.TgValorItem;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;
import java.math.BigDecimal;
import org.javabeanstack.sifen.core.fields.request.de.TgCamItem;
import org.javabeanstack.sifen.core.fields.request.de.TgValorRestaItem;
import org.javabeanstack.sifen.core.types.CMondT;

/**
 * Grupo E del MT, E8.1: gValorItem (+ gValorRestaItem): precio, tipo de cambio y descuentos (E2-03).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class ItemValueRules extends RuleSet<DocumentoElectronico, TgValorItem> {

    ItemValueRules() {
        super("E-gValorItem", RuleGroup.E, "gDtipDE.gCamItem[].gValorItem");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("", s -> org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) == 7); // TgCamItem: gValorItem solo con iTiDE ≠ 7
        notWrittenIf("dTiCamIt", s -> org.javabeanstack.sifen.core.validation.support.DEData.dCondTiCam(s.root()) != 2); // TgValorItem: dTiCamIt solo con dCondTiCam = 2
        declare(rule("E720").field("").error().dCodRes("1850").showValue(),
                groupRequiredIf(s -> tipo(s) != 0)); // M3-59: el 7 lo excluye notWrittenIf("")
        // M3-42: TgCamItem escribe gValorItem solo con iTiDE ≠ 7 — aviso: el dato no llega al XML
        declare(rule("E720a").field("").warning().dCodRes("1851").showValue(),
                groupForbiddenIf(s -> tipo(s) == 7));
        declare(rule("JBS-E-80").field("dPUniProSer").error().showValue(),
                required(TgValorItem::getdPUniProSer)); // M3-59: el 7 lo excluye notWrittenIf("")
        declare(rule("JBS-E-81").field("dPUniProSer").error().showValue(),
                passes(s -> ValueChecks.decimal(s.e().getdPUniProSer(), 15, 8) && nonNegative(s.e().getdPUniProSer()), s -> s.e().getdPUniProSer()));
        declare(rule("E725").field("dTiCamIt").error().dCodRes("1854").showValue(),
                requiredIf(s -> !pyg(s), TgValorItem::getdTiCamIt)); // M3-59: 7 y dCondTiCam ≠ 2 los excluye notWrittenIf
        declare(rule("JBS-E-82").field("dTiCamIt").error().showValue(),
                passes(s -> ValueChecks.decimal(s.e().getdTiCamIt(), 5, 4) && positive(s.e().getdTiCamIt()), s -> s.e().getdTiCamIt()));
        // M3-42: TgValorItem escribe dTiCamIt solo con dCondTiCam = 2 — aviso: el dato no llega al XML
        declare(rule("E725a").field("dTiCamIt").warning().dCodRes("1855").showValue(),
                forbiddenIf(s -> tipo(s) != 7 && DEData.dCondTiCam(s.root()) == 1, TgValorItem::getdTiCamIt));
        declare(rule("E725b").field("dTiCamIt").error().dCodRes("1856").showValue(),
                forbiddenIf(s -> pyg(s), TgValorItem::getdTiCamIt));
        declareNotEvaluated(rule("E727").field("dTotBruOpeItem").error().dCodRes("1859").showValue(),
                NotEvaluatedReason.COMPUTED);
        // E3-27: guarda de E2-03 (el MT admite el descuento ausente; la librería hace NPE): no desactivable
        declare(rule("JBS-E-83").nonDisableable().field("gValorRestaItem.dDescItem").error().showValue().source(RuleSource.JBS),
                passes(s -> present(desc(s))));
        declare(rule("JBS-E-84").field("gValorRestaItem.dDescItem").error().showValue(),
                passes(s -> desc(s) == null || (ValueChecks.decimal(desc(s), 15, 8) && desc(s).signum() >= 0
                && (s.e().getdPUniProSer() == null || desc(s).compareTo(s.e().getdPUniProSer()) <= 0)), s -> desc(s)));
        declareNotEvaluated(rule("EA003").field("gValorRestaItem.dPorcDesIt").error().dCodRes("1852").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("EA003a").field("gValorRestaItem.dPorcDesIt").error().dCodRes("1861").showValue(),
                NotEvaluatedReason.COMPUTED);
        // E3-27: ídem JBS-E-83 para el descuento global
        declare(rule("JBS-E-85").nonDisableable().field("gValorRestaItem.dDescGloItem").error().showValue().source(RuleSource.NT_001, RuleSource.JBS),
                passes(s -> present(descGlo(s))));
        declare(rule("JBS-E-86").field("gValorRestaItem.dDescGloItem").error().showValue(),
                passes(s -> ValueChecks.decimal(descGlo(s), 15, 8) && nonNegative(descGlo(s)), s -> descGlo(s)));
        declareNotEvaluated(rule("EA004").field("gValorRestaItem.dDescGloItem").error().dCodRes("1862").showValue().source(RuleSource.NT_001),
                NotEvaluatedReason.COMPUTED);
        declare(rule("JBS-E-87").field("gValorRestaItem.dAntPreUniIt").error().showValue(),
                passes(s -> r(s) == null || (ValueChecks.decimal(r(s).getdAntPreUniIt(), 15, 8) && nonNegative(r(s).getdAntPreUniIt())),
                s -> r(s).getdAntPreUniIt()));
        declare(rule("JBS-E-88").field("gValorRestaItem.dAntGloPreUniIt").error().showValue(),
                passes(s -> r(s) == null || (ValueChecks.decimal(r(s).getdAntGloPreUniIt(), 15, 8) && nonNegative(r(s).getdAntGloPreUniIt())),
                s -> r(s).getdAntGloPreUniIt()));
        declareNotEvaluated(rule("EA008").field("gValorRestaItem.dTotOpeItem").error().dCodRes("1853").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("EA009").field("gValorRestaItem.dTotOpeGs").error().dCodRes("1857").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("EA009a").field("gValorRestaItem.dTotOpeGs").error().dCodRes("1858").showValue(),
                NotEvaluatedReason.COMPUTED);
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgValorItem>> subjects(DocumentoElectronico raiz) {
        return raiz.getgDtipDE() == null ? none() : perItem(raiz.getgDtipDE().getgCamItemList(), TgCamItem::getgValorItem);
    }

    private static int tipo(Subject<DocumentoElectronico, TgValorItem> s) {
        return DEData.iTiDE(s.root());
    }

    private static boolean pyg(Subject<DocumentoElectronico, TgValorItem> s) {
        return DEData.cMoneOpe(s.root()) == CMondT.PYG;
    }

    private static TgValorRestaItem r(Subject<DocumentoElectronico, TgValorItem> s) {
        return s.e().getgValorRestaItem();
    }

    /** E2-03 (DEC-12): sin el descuento la librería da NullPointerException al totalizar. */
    private static BigDecimal desc(Subject<DocumentoElectronico, TgValorItem> s) {
        return r(s) == null ? null : r(s).getdDescItem();
    }

    private static BigDecimal descGlo(Subject<DocumentoElectronico, TgValorItem> s) {
        return r(s) == null ? null : r(s).getdDescGloItem();
    }
}

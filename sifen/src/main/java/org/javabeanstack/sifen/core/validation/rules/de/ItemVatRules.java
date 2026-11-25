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
import org.javabeanstack.sifen.core.fields.request.de.TgCamIVA;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;
import java.math.BigDecimal;
import org.javabeanstack.sifen.core.fields.request.de.TgCamItem;

/**
 * Grupo E del MT, E8.2: gCamIVA (IVA del ítem; E0-17).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class ItemVatRules extends RuleSet<DocumentoElectronico, TgCamIVA> {

    ItemVatRules() {
        super("E-gCamIVA", RuleGroup.E, "gDtipDE.gCamItem[].gCamIVA");
        declare(rule("E730").field("").error().dCodRes("1900").showValue(),
                groupRequiredIf(s -> tipo(s) != 7 && exigido(s)));
        // M3-42 (revisada, queda en error): no se escribe en la autofactura, pero TgTotSub lo usa para los subtotales y el IVA (con un gCamIVA vacío da NPE)
        declare(rule("E730a").field("").error().dCodRes("1901").showValue(),
                groupForbiddenIf(s -> tipo(s) == 4));
        // M3-42 (revisada, queda en error): no se escribe con iTImp = 2, pero TgTotSub lo usa para los subtotales y el IVA
        declare(rule("E730b").field("").error().dCodRes("1902").showValue(),
                groupForbiddenIf(s -> tipo(s) != 7 && DEData.iTImp(s.root()) == 2));
        declare(rule("JBS-E-89").field("iAfecIVA").error().showValue(),
                requiredIf(s -> exigido(s), TgCamIVA::getiAfecIVA));
        declareNotEvaluated(rule("E732").field("dDesAfecIVA").error().dCodRes("1903").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-E-90").field("dPropIVA").error().showValue(),
                requiredIf(s -> exigido(s), TgCamIVA::getdPropIVA));
        declare(rule("E733").field("dPropIVA").error().dCodRes("1904").showValue(),
                passes(s -> !exigido(s) || s.e().getdPropIVA() == null || !(afec(s) == 1 && s.e().getdPropIVA().compareTo(CIEN) != 0), s -> s.e().getdPropIVA()));
        declare(rule("E733a").field("dPropIVA").error().dCodRes("1905").showValue(),
                passes(s -> !exigido(s) || s.e().getdPropIVA() == null || !((afec(s) == 2 || afec(s) == 3) && s.e().getdPropIVA().signum() != 0), s -> s.e().getdPropIVA()));
        declare(rule("E733b").field("dPropIVA").error().dCodRes("1906").showValue(),
                passes(s -> !exigido(s) || s.e().getdPropIVA() == null
                || !(afec(s) == 4 && (s.e().getdPropIVA().signum() <= 0 || s.e().getdPropIVA().compareTo(CIEN) >= 0)), s -> s.e().getdPropIVA()));
        declare(rule("JBS-E-91").field("dTasaIVA").error().showValue(),
                requiredIf(s -> exigido(s), TgCamIVA::getdTasaIVA));
        declare(rule("E734").field("dTasaIVA").error().dCodRes("1907").showValue(),
                passes(s -> !exigido(s) || s.e().getdTasaIVA() == null || !((afec(s) == 2 || afec(s) == 3) && s.e().getdTasaIVA().signum() != 0), s -> s.e().getdTasaIVA()));
        declare(rule("E734a").field("dTasaIVA").error().dCodRes("1908").showValue(),
                passes(s -> !exigido(s) || s.e().getdTasaIVA() == null || !((afec(s) == 1 || afec(s) == 4)
                && s.e().getdTasaIVA().compareTo(new BigDecimal("5")) != 0 && s.e().getdTasaIVA().compareTo(BigDecimal.TEN) != 0), s -> s.e().getdTasaIVA()));
        declare(rule("JBS-E-92").field("dTasaIVA").error().showValue().source(RuleSource.JBS),
                passes(s -> !exigido(s) || s.e().getdTasaIVA() == null || s.e().getdTasaIVA().scale() <= 0, s -> s.e().getdTasaIVA()));
        declareNotEvaluated(rule("E735").field("dBasGravIVA").error().dCodRes("1909").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("E735a").field("dBasGravIVA").error().dCodRes("1910").showValue().source(RuleSource.NT_013),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("E735b").field("dBasGravIVA").error().dCodRes("1911").showValue().source(RuleSource.NT_013),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("E736").field("dLiqIVAItem").error().dCodRes("1912").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("E736a").field("dLiqIVAItem").error().dCodRes("1913").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("JBS-E-93").field("dBasExe").error().showValue().source(RuleSource.NT_013),
                NotEvaluatedReason.COMPUTED);
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgCamIVA>> subjects(DocumentoElectronico raiz) {
        return raiz.getgDtipDE() == null ? none() : perItem(raiz.getgDtipDE().getgCamItemList(), TgCamItem::getgCamIVA);
    }

    private static final BigDecimal CIEN = new BigDecimal("100");

    private static int tipo(Subject<DocumentoElectronico, TgCamIVA> s) {
        return DEData.iTiDE(s.root());
    }

    /** La librería escribe el gCamIVA del ítem (y lo exige): IVA, Renta, Ninguno o IVA-Renta, ni AFE ni NRE. */
    private static boolean exigido(Subject<DocumentoElectronico, TgCamIVA> s) {
        int imp = DEData.iTImp(s.root());
        int tipo = tipo(s);
        return (imp == 1 || imp == 3 || imp == 4 || imp == 5) && tipo != 0 && tipo != 4 && tipo != 7;
    }

    private static int afec(Subject<DocumentoElectronico, TgCamIVA> s) {
        return s.e().getiAfecIVA() == null ? 0 : s.e().getiAfecIVA().getVal();
    }
}

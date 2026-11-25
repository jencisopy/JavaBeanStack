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
import org.javabeanstack.sifen.core.fields.request.de.TgTotSub;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;

/**
 * Grupo F del MT: gTotSub (presencia y comisión; los totales los calcula la librería: no evaluados).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class TotalsRules extends RuleSet<DocumentoElectronico, TgTotSub> {

    TotalsRules() {
        super("F-gTotSub", RuleGroup.F, "gTotSub");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("", s -> org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) == 7); // DocumentoElectronico.setupDE: gTotSub solo con iTiDE ≠ 7
        declare(rule("F001").field("").error().dCodRes("2350").showValue(),
                groupRequiredIf(s -> DEData.iTiDE(s.root()) != 0 && DEData.iTiDE(s.root()) != 7));
        // M3-42: DocumentoElectronico.setupDE escribe gTotSub solo con iTiDE ≠ 7 — aviso: el dato no llega al XML
        declare(rule("F001a").field("").warning().dCodRes("2351").showValue(),
                groupForbiddenIf(s -> DEData.iTiDE(s.root()) == 7));
        declareNotEvaluated(rule("JBS-F-01").field("").error().showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F002").field("dSubExe").error().dCodRes("2352").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F002a").field("dSubExe").error().dCodRes("2353").showValue().source(RuleSource.NT_013),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F003").field("dSubExo").error().dCodRes("2354").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F003a").field("dSubExo").error().dCodRes("2355").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F004").field("dSub5").error().dCodRes("2356").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F004a").field("dSub5").error().dCodRes("2357").showValue().source(RuleSource.NT_013),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F005").field("dSub10").error().dCodRes("2358").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F005a").field("dSub10").error().dCodRes("2359").showValue().source(RuleSource.NT_013),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F008").field("dTotOpe").error().dCodRes("2362").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F009").field("dTotDesc").error().dCodRes("2363").showValue().source(RuleSource.NT_001),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("JBS-F-02").field("dTotDescGlotem").error().showValue().source(RuleSource.NT_001),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("JBS-F-03").field("dTotAntItem").error().showValue().source(RuleSource.NT_001),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("JBS-F-04").field("dTotAnt").error().showValue().source(RuleSource.NT_001),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("JBS-F-05").field("dPorcDescTotal").error().showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F011").field("dDescTotal").error().dCodRes("2364").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("JBS-F-06").field("dAnticipo").error().showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("JBS-F-07").field("dRedon").error().showValue(),
                NotEvaluatedReason.COMPUTED);
        declare(rule("JBS-F-08").field("dComi").error().showValue(),
                decimal(TgTotSub::getdComi, 15, 8, false));
        declareNotEvaluated(rule("F014").field("dTotGralOpe").error().dCodRes("2365").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F015").field("dIVA5").error().dCodRes("2366").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F015a").field("dIVA5").error().dCodRes("2367").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F016").field("dIVA10").error().dCodRes("2368").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F016a").field("dIVA10").error().dCodRes("2369").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F017").field("dTotIVA").error().dCodRes("2370").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F017a").field("dTotIVA").error().dCodRes("2371").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F018").field("dBaseGrav5").error().dCodRes("2372").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F018a").field("dBaseGrav5").error().dCodRes("2373").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F019").field("dBaseGrav10").error().dCodRes("2374").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F019a").field("dBaseGrav10").error().dCodRes("2375").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F020").field("dTBasGraIVA").error().dCodRes("2376").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F020a").field("dTBasGraIVA").error().dCodRes("2377").showValue(),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F023").field("dTotalGs").error().dCodRes("2382").showValue().source(RuleSource.NT_008),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F023a").field("dTotalGs").error().dCodRes("2385").showValue().source(RuleSource.NT_008),
                NotEvaluatedReason.COMPUTED);
        declareNotEvaluated(rule("F023b").field("dTotalGs").error().dCodRes("2386").showValue().source(RuleSource.NT_008),
                NotEvaluatedReason.COMPUTED);
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgTotSub>> subjects(DocumentoElectronico raiz) {
        return unique(raiz.getgTotSub());
    }
}

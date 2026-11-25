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
import org.javabeanstack.sifen.core.fields.request.de.TgCamNRE;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;
import java.time.YearMonth;

/**
 * Grupo E del MT, E6: gCamNRE (nota de remisión; E502 rechaza el motivo 99, I3-17).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class DispatchNoteRules extends RuleSet<DocumentoElectronico, TgCamNRE> {

    DispatchNoteRules() {
        super("E-gCamNRE", RuleGroup.E, "gDtipDE.gCamNRE");
        declare(rule("E500").field("").error().dCodRes("2600").showValue(),
                groupRequiredIf(s -> DEData.iTiDE(s.root()) == 7));
        // M3-42 (revisada, queda en error): gCamNRE no se escribe fuera de la remisión, pero su iMotEmiNR = 5 hace que TgTransp escriba dNuDespImp en una factura
        declare(rule("E500a").field("").error().dCodRes("2601").showValue(),
                groupForbiddenIf(s -> DEData.iTiDE(s.root()) != 0 && DEData.iTiDE(s.root()) != 7));
        declare(rule("JBS-E-26").field("iMotEmiNR").error().showValue(),
                required(TgCamNRE::getiMotEmiNR));
        // §6.2 (I3-17): con «Otro» (99) se exige el texto libre de 5 a 60 caracteres; antes se rechazaba siempre
        // porque el modelo no tenía dónde cargarlo
        declare(rule("E502").field("dDesMotEmiNR").error().dCodRes("2602").showValue(),
                passes(s -> s.e().getiMotEmiNR() == null || s.e().getiMotEmiNR().getVal() != 99
                        || (present(s.e().getdDesMotEmiNR()) && ValueChecks.length(s.e().getdDesMotEmiNR().trim(), 5, 60)),
                        s -> s.e().getdDesMotEmiNR()));
        declare(rule("JBS-E-27").field("iRespEmiNR").error().showValue(),
                required(TgCamNRE::getiRespEmiNR));
        declareNotEvaluated(rule("E504").field("dDesRespEmiNR").error().dCodRes("2603").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-E-28").field("dKmR").error().showValue().source(RuleSource.NT_010),
                passes(s -> s.e().getdKmR() > 0 && digitCount(s.e().getdKmR()) <= 5, s -> s.e().getdKmR()));
        declare(rule("E506a").field("dFecEm").error().dCodRes("2605").showValue(),
                requiredIf(s -> DEData.iMotEmiNR(s.root()) == 1 && !present(s.root().getgCamDEAsocList()), TgCamNRE::getdFecEm));
        declare(rule("E506").field("dFecEm").error().dCodRes("2604").showValue(),
                passes(s -> s.e().getdFecEm() == null || s.root().getgDatGralOpe() == null || s.root().getgDatGralOpe().getdFeEmiDE() == null
                || !YearMonth.from(s.e().getdFecEm()).isAfter(YearMonth.from(s.root().getgDatGralOpe().getdFeEmiDE())), s -> s.e().getdFecEm()));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgCamNRE>> subjects(DocumentoElectronico raiz) {
        return raiz.getgDtipDE() == null ? none() : unique(raiz.getgDtipDE().getgCamNRE());
    }
}

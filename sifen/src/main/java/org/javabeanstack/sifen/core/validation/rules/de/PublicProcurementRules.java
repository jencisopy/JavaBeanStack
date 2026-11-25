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
import org.javabeanstack.sifen.core.fields.request.de.TgCompPub;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;

/**
 * Grupo E del MT, E1.1: gCompPub (compras públicas, solo B2G; E0-14).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class PublicProcurementRules extends RuleSet<DocumentoElectronico, TgCompPub> {

    PublicProcurementRules() {
        super("E-gCompPub", RuleGroup.E, "gDtipDE.gCamFE.gCompPub");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("", s -> org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 0 && (org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 1 || (org.javabeanstack.sifen.core.validation.support.DEData.iTiOpe(s.root()) != 0 && org.javabeanstack.sifen.core.validation.support.DEData.iTiOpe(s.root()) != 3))); // TgCamFE: gCompPub solo en la factura y con iTiOpe = 3
        declare(rule("JBS-E-04").field("").warning().showValue().source(RuleSource.NT_026),
                passes(s -> ope(s) == 3 || ope(s) == 0));
        declare(rule("JBS-E-05").field("dModCont").error().showValue(),
                passes(s -> ope(s) != 3 || (present(s.e().getdModCont()) && s.e().getdModCont().length() == 2), s -> s.e().getdModCont()));
        declare(rule("JBS-E-06").field("dEntCont").error().showValue(),
                passes(s -> ope(s) != 3 || (s.e().getdEntCont() > 0 && digitCount(s.e().getdEntCont()) == 5), s -> s.e().getdEntCont()));
        declare(rule("JBS-E-07").field("dAnoCont").error().showValue(),
                passes(s -> ope(s) != 3 || (s.e().getdAnoCont() > 0 && digitCount(s.e().getdAnoCont()) <= 2), s -> s.e().getdAnoCont()));
        declare(rule("JBS-E-08").field("dSecCont").error().showValue(),
                passes(s -> ope(s) != 3 || (present(s.e().getdSecCont()) && digits(s.e().getdSecCont(), 7, 7)), s -> s.e().getdSecCont()));
        declare(rule("JBS-E-09").field("dFeCodCont").error().showValue(),
                requiredIf(s -> ope(s) == 3, TgCompPub::getdFeCodCont));
        declare(rule("E025").field("dFeCodCont").error().dCodRes("1402").showValue(),
                passes(s -> ope(s) != 3 || s.e().getdFeCodCont() == null || s.root().getgDatGralOpe() == null
                || s.root().getgDatGralOpe().getdFeEmiDE() == null
                || !s.e().getdFeCodCont().isAfter(s.root().getgDatGralOpe().getdFeEmiDE().toLocalDate()), s -> s.e().getdFeCodCont()));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgCompPub>> subjects(DocumentoElectronico raiz) {
        return raiz.getgDtipDE() == null || raiz.getgDtipDE().getgCamFE() == null || raiz.getgDtipDE().getgCamFE().getgCompPub() == null
                ? none() : unique(raiz.getgDtipDE().getgCamFE().getgCompPub());
    }

    private static int ope(Subject<DocumentoElectronico, TgCompPub> s) {
        return DEData.iTiOpe(s.root());
    }
}

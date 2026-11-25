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
import org.javabeanstack.sifen.core.fields.request.de.TgGrupEner;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;

/**
 * Grupo E del MT, E9.2: gGrupEner (energía eléctrica).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class EnergyRules extends RuleSet<DocumentoElectronico, TgGrupEner> {

    EnergyRules() {
        super("E-gGrupEner", RuleGroup.E, "gDtipDE.gCamEsp.gGrupEner");
        declare(rule("JBS-E-119").field("dNroMed").error().showValue(),
                length(TgGrupEner::getdNroMed, 1, 50));
        declare(rule("JBS-E-120").field("dActiv").error().showValue(),
                passes(s -> s.e().getdActiv() == 0 || maxDigits(s.e().getdActiv(), 2), s -> s.e().getdActiv()));
        declare(rule("JBS-E-121").field("dCateg").error().showValue(),
                length(TgGrupEner::getdCateg, 3, 3));
        declare(rule("JBS-E-122").field("dLecAnt").error().showValue(),
                passes(s -> ValueChecks.decimal(s.e().getdLecAnt(), 11, 2), s -> s.e().getdLecAnt()));
        declare(rule("JBS-E-123").field("dLecAct").error().showValue(),
                passes(s -> ValueChecks.decimal(s.e().getdLecAct(), 11, 2), s -> s.e().getdLecAct()));
        declare(rule("JBS-E-124").field("dLecAct").warning().showValue(),
                passes(s -> !lecturas(s) || s.e().getdLecAct().compareTo(s.e().getdLecAnt()) >= 0));
        declare(rule("JBS-E-125").field("dConKwh").error().showValue().source(RuleSource.NT_023),
                passes(s -> ValueChecks.decimal(s.e().getdConKwh(), 11, 2), s -> s.e().getdConKwh()));
        declare(rule("JBS-E-126").field("dConKwh").warning().showValue().source(RuleSource.NT_023),
                passes(s -> !lecturas(s) || s.e().getdConKwh() == null
                || s.e().getdConKwh().compareTo(s.e().getdLecAct().subtract(s.e().getdLecAnt())) == 0));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgGrupEner>> subjects(DocumentoElectronico raiz) {
        return DEData.gCamEsp(raiz) == null ? none() : unique(DEData.gCamEsp(raiz).getgGrupEner());
    }

    private static boolean lecturas(Subject<DocumentoElectronico, TgGrupEner> s) {
        return s.e().getdLecAnt() != null && s.e().getdLecAct() != null;
    }
}

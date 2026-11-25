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
import org.javabeanstack.sifen.core.fields.request.de.TgGrupSup;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;

/**
 * Grupo E del MT, E9.4: gGrupSup (supermercados).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class SupermarketRules extends RuleSet<DocumentoElectronico, TgGrupSup> {

    SupermarketRules() {
        super("E-gGrupSup", RuleGroup.E, "gDtipDE.gCamEsp.gGrupSup");
        declare(rule("JBS-E-137").field("dNomCaj").error().lengthOnly(),
                length(TgGrupSup::getdNomCaj, 1, 20));
        declare(rule("JBS-E-138").field("dEfectivo").error().showValue(),
                passes(s -> ValueChecks.decimal(s.e().getdEfectivo(), 15, 4), s -> s.e().getdEfectivo()));
        declare(rule("JBS-E-139").field("dVuelto").error().showValue(),
                passes(s -> ValueChecks.decimal(s.e().getdVuelto(), 6, 4), s -> s.e().getdVuelto()));
        declare(rule("JBS-E-140").field("dDonac").error().showValue(),
                passes(s -> ValueChecks.decimal(s.e().getdDonac(), 6, 4), s -> s.e().getdDonac()));
        declare(rule("JBS-E-141").field("dDesDonac").error().showValue(),
                length(TgGrupSup::getdDesDonac, 1, 20));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgGrupSup>> subjects(DocumentoElectronico raiz) {
        return DEData.gCamEsp(raiz) == null ? none() : unique(DEData.gCamEsp(raiz).getgGrupSup());
    }
}

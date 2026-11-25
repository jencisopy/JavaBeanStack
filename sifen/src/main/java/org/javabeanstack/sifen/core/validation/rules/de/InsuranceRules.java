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
import org.javabeanstack.sifen.core.fields.request.de.TgGrupSeg;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;

/**
 * Grupo E del MT, E9.3: gGrupSeg (seguros, pólizas a nivel lista).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class InsuranceRules extends RuleSet<DocumentoElectronico, TgGrupSeg> {

    InsuranceRules() {
        super("E-gGrupSeg", RuleGroup.E, "gDtipDE.gCamEsp.gGrupSeg");
        declare(rule("JBS-E-127").field("gGrupPolSeg[]").error().showValue(),
                passes(s -> present(s.e().getgGrupPolSegList()) && s.e().getgGrupPolSegList().size() <= 999, s -> s.e().getgGrupPolSegList()));
        declare(rule("JBS-E-128").field("dCodEmpSeg").error().showValue(),
                length(TgGrupSeg::getdCodEmpSeg, 20, 20));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgGrupSeg>> subjects(DocumentoElectronico raiz) {
        return DEData.gCamEsp(raiz) == null ? none() : unique(DEData.gCamEsp(raiz).getgGrupSeg());
    }
}

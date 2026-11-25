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
import org.javabeanstack.sifen.core.fields.request.de.TgCamFE;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;

/**
 * Grupo E del MT, E1: gCamFE (factura).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class InvoiceRules extends RuleSet<DocumentoElectronico, TgCamFE> {

    InvoiceRules() {
        super("E-gCamFE", RuleGroup.E, "gDtipDE.gCamFE");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("", s -> org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 0 && org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 1); // TgDtipDE: gCamFE solo con iTiDE = 1
        declare(rule("E010").field("").error().dCodRes("1350").showValue(),
                groupRequiredIf(s -> DEData.iTiDE(s.root()) == 1));
        // M3-42: TgDtipDE escribe gCamFE solo con iTiDE = 1 — aviso: el dato no llega al XML
        declare(rule("E010a").field("").warning().dCodRes("1351").showValue(),
                groupForbiddenIf(s -> DEData.iTiDE(s.root()) != 0 && DEData.iTiDE(s.root()) != 1));
        declare(rule("JBS-E-02").field("iIndPres").error().showValue(),
                required(TgCamFE::getiIndPres));
        declare(rule("E012").field("dDesIndPres").error().dCodRes("1352").showValue(),
                passes(s -> s.e().getiIndPres() == null || s.e().getiIndPres().getVal() != 9
                || (present(s.e().getdDesIndPres()) && ValueChecks.length(s.e().getdDesIndPres(), 10, 30)), s -> s.e().getdDesIndPres()));
        declareNotEvaluated(rule("JBS-E-03").field("dFecEmNR").error().showValue(),
                NotEvaluatedReason.TYPE);
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgCamFE>> subjects(DocumentoElectronico raiz) {
        return raiz.getgDtipDE() == null ? none() : unique(raiz.getgDtipDE().getgCamFE());
    }
}

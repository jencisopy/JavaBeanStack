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
import org.javabeanstack.sifen.core.fields.request.de.TgCuotas;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;
import org.javabeanstack.sifen.core.fields.request.de.TgCamCond;

/**
 * Grupo E del MT, E7.2.1: gCuotas[] (cuotas del crédito).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class InstallmentRules extends RuleSet<DocumentoElectronico, TgCuotas> {

    InstallmentRules() {
        super("E-gCuotas", RuleGroup.E, "gDtipDE.gCamCond.gPagCred.gCuotas[]");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("", s -> (org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 0 && org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 1 && org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 4) || (org.javabeanstack.sifen.core.validation.support.DEData.iCondOpe(s.root()) != 0 && org.javabeanstack.sifen.core.validation.support.DEData.iCondOpe(s.root()) != 2)); // TgDtipDE/TgCamCond: gPagCred (y sus gCuotas) solo con iTiDE = 1/4 e iCondOpe = 2
        declare(rule("JBS-E-52").field("cMoneCuo").error().showValue(),
                onNull(s -> s.e() != null && present(s.e().getcMoneCuo()), s -> null));
        declareNotEvaluated(rule("JBS-E-53").field("dDMoneCuo").error().showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-E-54").field("dMonCuota").error().showValue(),
                onNull(s -> s.e() != null && present(s.e().getdMonCuota()), s -> null));
        declare(rule("JBS-E-55").field("dMonCuota").error().showValue(),
                decimal(TgCuotas::getdMonCuota, 15, 4, true));
        declareNotEvaluated(rule("JBS-E-56").field("dVencCuo").error().showValue(),
                NotEvaluatedReason.TYPE);
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgCuotas>> subjects(DocumentoElectronico raiz) {
        TgCamCond c = DEData.gCamCond(raiz);
        return c == null || c.getiCondOpe() == null || c.getgPagCred() == null ? none() : perItem(c.getgPagCred().getgCuotasList());
    }
}

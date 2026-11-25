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
import org.javabeanstack.sifen.core.fields.request.de.TgCamEnt;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;

/**
 * Grupo E del MT, E10.2: gCamEnt[] (locales de entrega).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class DeliveryPlaceRules extends RuleSet<DocumentoElectronico, TgCamEnt> {

    DeliveryPlaceRules() {
        super("E-gCamEnt", RuleGroup.E, "gDtipDE.gTransp.gCamEnt[]");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("", s -> (org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) == 4 || org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) == 5 || org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) == 6)); // gTransp no se escribe con iTiDE = 4/5/6
        declare(rule("JBS-E-162").field("dDirLocEnt").error().lengthOnly(),
                onNull(s -> s.e() != null && present(s.e().getdDirLocEnt()) && ValueChecks.length(s.e().getdDirLocEnt(), 1, 255), s -> s.e() == null ? null : s.e().getdDirLocEnt()));
        declare(rule("JBS-E-163").field("dNumCasEnt").error().showValue(),
                passes(s -> maxDigits(s.e().getdNumCasEnt(), 6), s -> s.e().getdNumCasEnt()));
        declare(rule("JBS-E-164").field("dComp1Ent").error().lengthOnly(),
                length(TgCamEnt::getdComp1Ent, 1, 255));
        declare(rule("JBS-E-165").field("dComp2Ent").error().lengthOnly(),
                length(TgCamEnt::getdComp2Ent, 1, 255));
        declare(rule("JBS-E-166").field("cDepEnt").error().showValue(),
                required(TgCamEnt::getcDepEnt));
        declareNotEvaluated(rule("E946").field("dDesDepEnt").error().dCodRes("2202").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-E-167").field("cDisEnt").error().showValue(),
                passes(s -> maxDigits(s.e().getcDisEnt(), 4), s -> s.e().getcDisEnt()));
        declare(rule("E948").field("dDesDisEnt").error().dCodRes("2204").showValue(),
                requiredIf(s -> s.e().getcDisEnt() != 0, TgCamEnt::getdDesDisEnt));
        declare(rule("JBS-E-168").field("dDesDisEnt").error().showValue(),
                length(TgCamEnt::getdDesDisEnt, 1, 30));
        declare(rule("JBS-E-169").field("cCiuEnt").error().showValue(),
                passes(s -> s.e().getcCiuEnt() > 0 && digitCount(s.e().getcCiuEnt()) <= 5, s -> s.e().getcCiuEnt()));
        declare(rule("JBS-E-170").field("dDesCiuEnt").error().showValue(),
                requiredWithLength(TgCamEnt::getdDesCiuEnt, 1, 30));
        declare(rule("JBS-E-171").field("dTelEnt").error().lengthOnly(),
                length(TgCamEnt::getdTelEnt, 6, 15));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgCamEnt>> subjects(DocumentoElectronico raiz) {
        return DEData.gTransp(raiz) == null ? none() : perItem(DEData.gTransp(raiz).getgCamEntList());
    }
}

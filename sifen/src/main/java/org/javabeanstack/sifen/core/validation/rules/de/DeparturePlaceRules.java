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
import org.javabeanstack.sifen.core.fields.request.de.TgCamSal;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;

/**
 * Grupo E del MT, E10.1: gCamSal (local de salida).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class DeparturePlaceRules extends RuleSet<DocumentoElectronico, TgCamSal> {

    DeparturePlaceRules() {
        super("E-gCamSal", RuleGroup.E, "gDtipDE.gTransp.gCamSal");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("", s -> (org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) == 4 || org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) == 5 || org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) == 6)); // gTransp no se escribe con iTiDE = 4/5/6
        declare(rule("E920").field("").error().dCodRes("2150").showValue(),
                groupRequiredIf(s -> TransportRules.nre(s)));
        // M3-42: gTransp (y con él gCamSal) no se escribe con iTiDE = 4/5/6 — aviso: el dato no llega al XML
        declare(rule("E920a").field("").warning().dCodRes("2151").showValue(),
                groupForbiddenIf(s -> TransportRules.sinTransporte(s)));
        declare(rule("JBS-E-152").field("dDirLocSal").error().lengthOnly(),
                requiredWithLength(TgCamSal::getdDirLocSal, 1, 255));
        declare(rule("JBS-E-153").field("dNumCasSal").error().showValue(),
                passes(s -> maxDigits(s.e().getdNumCasSal(), 6), s -> s.e().getdNumCasSal()));
        declare(rule("JBS-E-154").field("dComp1Sal").error().lengthOnly(),
                length(TgCamSal::getdComp1Sal, 1, 255));
        declare(rule("JBS-E-155").field("dComp2Sal").error().lengthOnly(),
                length(TgCamSal::getdComp2Sal, 1, 255));
        declare(rule("JBS-E-156").field("cDepSal").error().showValue(),
                required(TgCamSal::getcDepSal));
        declareNotEvaluated(rule("E926").field("dDesDepSal").error().dCodRes("2152").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-E-157").field("cDisSal").error().showValue(),
                passes(s -> maxDigits(s.e().getcDisSal(), 4), s -> s.e().getcDisSal()));
        declare(rule("E928").field("dDesDisSal").error().dCodRes("2154").showValue(),
                requiredIf(s -> s.e().getcDisSal() != 0, TgCamSal::getdDesDisSal));
        declare(rule("JBS-E-158").field("dDesDisSal").error().showValue(),
                length(TgCamSal::getdDesDisSal, 1, 30));
        declare(rule("JBS-E-159").field("cCiuSal").error().showValue(),
                passes(s -> s.e().getcCiuSal() > 0 && digitCount(s.e().getcCiuSal()) <= 5, s -> s.e().getcCiuSal()));
        declare(rule("JBS-E-160").field("dDesCiuSal").error().showValue(),
                requiredWithLength(TgCamSal::getdDesCiuSal, 1, 30));
        declare(rule("JBS-E-161").field("dTelSal").error().lengthOnly(),
                length(TgCamSal::getdTelSal, 6, 15));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgCamSal>> subjects(DocumentoElectronico raiz) {
        return DEData.gTransp(raiz) == null ? none() : unique(DEData.gTransp(raiz).getgCamSal());
    }
}

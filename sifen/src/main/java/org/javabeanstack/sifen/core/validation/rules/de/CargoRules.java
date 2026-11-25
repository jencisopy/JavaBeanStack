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
import org.javabeanstack.sifen.core.fields.request.de.TgCamCarg;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;

/**
 * Grupo G del MT, G1: gCamCarg (datos de la carga).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class CargoRules extends RuleSet<DocumentoElectronico, TgCamCarg> {

    CargoRules() {
        super("G-gCamCarg", RuleGroup.G, "gCamGen.gCamCarg");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("", s -> org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 0 && org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 1 && org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 7); // TgCamGen: gCamCarg solo con iTiDE = 1/7
        // M3-42: TgCamGen escribe gCamCarg solo con iTiDE = 1/7 — aviso: el dato no llega al XML
        declare(rule("G050").field("").warning().dCodRes("2390").showValue(),
                groupForbiddenIf(s -> DEData.iTiDE(s.root()) != 0 && DEData.iTiDE(s.root()) != 1 && DEData.iTiDE(s.root()) != 7));
        declareNotEvaluated(rule("JBS-G-04").field("dDesUniMedTotVol").error().showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-G-05").field("dTotVolMerc").error().showValue(),
                passes(s -> maxDigits(s.e().getdTotVolMerc(), 20), s -> s.e().getdTotVolMerc()));
        declareNotEvaluated(rule("JBS-G-06").field("dDesUniMedTotPes").error().showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-G-07").field("dTotPesMerc").error().showValue(),
                passes(s -> maxDigits(s.e().getdTotPesMerc(), 20), s -> s.e().getdTotPesMerc()));
        declare(rule("JBS-G-08").field("dDesCarCarga").error().showValue(),
                passes(s -> s.e().getiCarCarga() == null || s.e().getiCarCarga().getVal() != 3
                || (present(s.e().getdDesCarCarga()) && ValueChecks.length(s.e().getdDesCarCarga(), 1, 50)), s -> s.e().getdDesCarCarga()));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgCamCarg>> subjects(DocumentoElectronico raiz) {
        return raiz.getgCamGen() == null ? none() : unique(raiz.getgCamGen().getgCamCarg());
    }
}

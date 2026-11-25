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
import org.javabeanstack.sifen.core.fields.request.de.TgVehTras;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;

/**
 * Grupo E del MT, E10.3: gVehTras[] (vehículos de traslado).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class TransportVehicleRules extends RuleSet<DocumentoElectronico, TgVehTras> {

    TransportVehicleRules() {
        super("E-gVehTras", RuleGroup.E, "gDtipDE.gTransp.gVehTras[]");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("", s -> (org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) == 4 || org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) == 5 || org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) == 6)); // gTransp no se escribe con iTiDE = 4/5/6
        notWrittenIf("dNroVuelo", s -> org.javabeanstack.sifen.core.validation.support.DEData.iModTrans(s.root()) != 3); // TgVehTras: dNroVuelo solo con iModTrans = 3
        notWrittenIf("dNroIDVeh", s -> s.e().getdTipIdenVeh() != 1); // TgVehTras: dNroIDVeh solo con dTipIdenVeh = 1
        notWrittenIf("dNroMatVeh", s -> s.e().getdTipIdenVeh() != 2); // TgVehTras: dNroMatVeh solo con dTipIdenVeh = 2
        declare(rule("JBS-E-173").field("dTiVehTras").error().showValue(),
                onNull(s -> s.e() != null && present(s.e().getdTiVehTras()) && ValueChecks.length(s.e().getdTiVehTras(), 4, 10), s -> s.e() == null ? null : s.e().getdTiVehTras()));
        declare(rule("JBS-E-174").field("dMarVeh").error().showValue(),
                requiredWithLength(TgVehTras::getdMarVeh, 1, 10));
        declare(rule("JBS-E-175").field("dTipIdenVeh").error().showValue(),
                passes(s -> s.e().getdTipIdenVeh() == 1 || s.e().getdTipIdenVeh() == 2, s -> s.e().getdTipIdenVeh()));
        declare(rule("E963").field("dNroIDVeh").error().dCodRes("2255").showValue(),
                requiredIf(s -> s.e().getdTipIdenVeh() == 1, TgVehTras::getdNroIDVeh));
        declare(rule("JBS-E-176").field("dNroIDVeh").error().showValue(),
                length(TgVehTras::getdNroIDVeh, 1, 20));
        declare(rule("JBS-E-177").field("dAdicVeh").error().showValue(),
                length(TgVehTras::getdAdicVeh, 1, 20));
        declare(rule("E965").field("dNroMatVeh").error().dCodRes("2254").showValue(),
                requiredIf(s -> s.e().getdTipIdenVeh() == 2, TgVehTras::getdNroMatVeh));
        declare(rule("JBS-E-178").field("dNroMatVeh").error().showValue().source(RuleSource.NT_005),
                length(TgVehTras::getdNroMatVeh, 6, 7));
        declare(rule("E966").field("dNroVuelo").error().dCodRes("2252").showValue(),
                requiredIf(s -> DEData.iModTrans(s.root()) == 3, TgVehTras::getdNroVuelo));
        declare(rule("JBS-E-179").field("dNroVuelo").error().showValue(),
                length(TgVehTras::getdNroVuelo, 6, 6));
        // M3-42: TgVehTras escribe dNroVuelo solo con iModTrans = 3 — aviso: el dato no llega al XML
        declare(rule("E966a").field("dNroVuelo").warning().dCodRes("2253").showValue(),
                forbiddenIf(s -> DEData.iModTrans(s.root()) != 0 && DEData.iModTrans(s.root()) != 3, TgVehTras::getdNroVuelo));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgVehTras>> subjects(DocumentoElectronico raiz) {
        return DEData.gTransp(raiz) == null ? none() : perItem(DEData.gTransp(raiz).getgVehTrasList());
    }
}

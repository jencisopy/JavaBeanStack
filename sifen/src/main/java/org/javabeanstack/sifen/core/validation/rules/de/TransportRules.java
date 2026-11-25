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
import org.javabeanstack.sifen.core.fields.request.de.TgTransp;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;

/**
 * Grupo E del MT, E10: gTransp (transporte; locales y vehículos a nivel lista).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class TransportRules extends RuleSet<DocumentoElectronico, TgTransp> {

    TransportRules() {
        super("E-gTransp", RuleGroup.E, "gDtipDE.gTransp");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("", s -> (org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) == 4 || org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) == 5 || org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) == 6)); // TgDtipDE: gTransp solo con iTiDE = 7 o 1
        notWrittenIf("dNuDespImp", s -> org.javabeanstack.sifen.core.validation.support.DEData.iMotEmiNR(s.root()) != 5); // TgTransp: dNuDespImp solo con iMotEmiNR = 5
        declare(rule("E900").field("").error().dCodRes("2100").showValue(),
                groupRequiredIf(s -> nre(s)));
        // M3-42: TgDtipDE escribe gTransp solo con iTiDE = 7 o 1 — aviso: el dato no llega al XML
        declare(rule("E900a").field("").warning().dCodRes("2101").showValue(),
                groupForbiddenIf(s -> sinTransporte(s)));
        declare(rule("E901").field("iTipTrans").error().dCodRes("2102").showValue(),
                requiredIf(s -> nre(s), TgTransp::getiTipTrans));
        declareNotEvaluated(rule("E902").field("dDesTipTrans").error().dCodRes("2103").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-E-147").field("iModTrans").error().showValue(),
                required(TgTransp::getiModTrans));
        declareNotEvaluated(rule("E904").field("dDesModTrans").error().dCodRes("2104").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-E-148").field("iRespFlete").error().showValue(),
                required(TgTransp::getiRespFlete));
        declare(rule("JBS-E-149").field("dNuManif").error().showValue(),
                length(TgTransp::getdNuManif, 1, 15));
        declare(rule("JBS-E-150").field("dNuDespImp").error().showValue(),
                requiredIf(s -> DEData.iMotEmiNR(s.root()) == 5, TgTransp::getdNuDespImp));
        declare(rule("JBS-E-151").field("dNuDespImp").error().showValue(),
                length(TgTransp::getdNuDespImp, 16, 16));
        declare(rule("E909").field("dIniTras").error().dCodRes("2107").showValue(),
                requiredIf(s -> nre(s), TgTransp::getdIniTras));
        declare(rule("E909a").field("dIniTras").error().dCodRes("2108").showValue(),
                passes(s -> s.e().getdIniTras() == null || !s.e().getdIniTras().isBefore(DEData.SIFEN_START.toLocalDate()), s -> s.e().getdIniTras()));
        declare(rule("E910").field("dFinTras").error().dCodRes("2109").showValue(),
                requiredIf(s -> s.e().getdIniTras() != null, TgTransp::getdFinTras));
        declare(rule("E910a").field("dFinTras").error().dCodRes("2110").showValue(),
                passes(s -> s.e().getdIniTras() == null || s.e().getdFinTras() == null || !s.e().getdFinTras().isBefore(s.e().getdIniTras()), s -> s.e().getdFinTras()));
        declareNotEvaluated(rule("E912a").field("dDesPaisDest").error().dCodRes("2113").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("E940").field("gCamEnt[]").error().dCodRes("2200").showValue(),
                passes(s -> !((nre(s) && !present(s.e().getgCamEntList())) || (s.e().getgCamEntList() != null && s.e().getgCamEntList().size() > 99)),
                s -> s.e().getgCamEntList()));
        // M3-42: gTransp (y con él gCamEnt) no se escribe con iTiDE = 4/5/6 — aviso: el dato no llega al XML
        declare(rule("E940a").field("gCamEnt[]").warning().dCodRes("2201").showValue(),
                forbiddenIf(s -> sinTransporte(s), TgTransp::getgCamEntList));
        declare(rule("E960").field("gVehTras[]").error().dCodRes("2250").showValue(),
                requiredIf(s -> nre(s), TgTransp::getgVehTrasList));
        // M3-42: gTransp (y con él gVehTras) no se escribe con iTiDE = 4/5/6 — aviso: el dato no llega al XML
        declare(rule("E960a").field("gVehTras[]").warning().dCodRes("2251").showValue(),
                forbiddenIf(s -> sinTransporte(s), TgTransp::getgVehTrasList));
        declare(rule("JBS-E-172").field("gVehTras[]").error().showValue(),
                passes(s -> s.e().getgVehTrasList() == null || s.e().getgVehTrasList().size() <= 4, s -> s.e().getgVehTrasList()));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgTransp>> subjects(DocumentoElectronico raiz) {
        return raiz.getgDtipDE() == null ? none() : unique(raiz.getgDtipDE().getgTransp());
    }

    static boolean nre(Subject<DocumentoElectronico, ?> s) {
        return DEData.iTiDE(s.root()) == 7;
    }

    static boolean sinTransporte(Subject<DocumentoElectronico, ?> s) {
        int tipo = DEData.iTiDE(s.root());
        return tipo == 4 || tipo == 5 || tipo == 6;
    }
}

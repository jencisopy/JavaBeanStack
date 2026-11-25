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
package org.javabeanstack.sifen.core.validation.rules.event;

import static org.javabeanstack.sifen.core.validation.support.ValueChecks.*;

import java.util.List;
import org.javabeanstack.sifen.core.fields.request.event.TrGesEve;
import org.javabeanstack.sifen.core.fields.request.event.TrGeVeNotRec;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;
import java.time.Duration;

/**
 * Evento de notificación de recepción (rGeVeNotRec, GEN).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class ReceptionNoticeRules extends RuleSet<TrGesEve, TrGeVeNotRec> {

    ReceptionNoticeRules() {
        super("EVENTO-rGeVeNotRec", RuleGroup.EVENTO, "rEve.gGroupTiEvt.rGeVeNotRec");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("dRucRec", s -> (s.e().getiTipRec() == null ? 0 : s.e().getiTipRec().getVal()) != 1); // TrGeVeNotRec: RUC solo con iTipRec = 1
        notWrittenIf("dDVRec", s -> (s.e().getiTipRec() == null ? 0 : s.e().getiTipRec().getVal()) != 1); // TrGeVeNotRec: RUC solo con iTipRec = 1
        notWrittenIf("dTipIDRec", s -> (s.e().getiTipRec() == null ? 0 : s.e().getiTipRec().getVal()) == 1); // TrGeVeNotRec: documento solo con iTipRec ≠ 1
        notWrittenIf("dNumID", s -> (s.e().getiTipRec() == null ? 0 : s.e().getiTipRec().getVal()) == 1); // TrGeVeNotRec: documento solo con iTipRec ≠ 1
        declare(rule("GEN002c").field("Id").error().dCodRes("4102").showValue(),
                passes(s -> validCdc(s.e().getId())));
        declare(rule("JBS-EVENTO-12").field("dFecEmi").error().showValue().source(RuleSource.NT_019),
                required(TrGeVeNotRec::getdFecEmi));
        declare(rule("GEN003").field("dFecEmi").warning().dCodRes("4103").showValue().source(RuleSource.NT_019),
                passes(s -> s.e().getdFecEmi() == null || Duration.between(s.e().getdFecEmi(), s.ctx().getNow()).toDays() <= 45, s -> s.e().getdFecEmi()));
        declare(rule("JBS-EVENTO-13").field("dFecRecep").error().showValue(),
                required(TrGeVeNotRec::getdFecRecep));
        declare(rule("GEN004").field("dFecRecep").error().dCodRes("4104").showValue(),
                passes(s -> s.e().getdFecRecep() == null || s.e().getdFecEmi() == null || !s.e().getdFecRecep().isBefore(s.e().getdFecEmi()), s -> s.e().getdFecRecep()));
        declare(rule("JBS-EVENTO-14").field("iTipRec").error().showValue(),
                required(TrGeVeNotRec::getiTipRec));
        declare(rule("JBS-EVENTO-15").field("dNomRec").error().lengthOnly(),
                requiredWithLength(TrGeVeNotRec::getdNomRec, 4, 60));
        declare(rule("GEN007").field("dRucRec").error().dCodRes("4105").omitValue(),
                requiredIf(s -> tipo(s) == 1, TrGeVeNotRec::getdRucRec));
        declare(rule("JBS-EVENTO-16").field("dRucRec").error().omitValue(),
                length(TrGeVeNotRec::getdRucRec, 3, 8));
        // M3-42: TrGeVeNotRec escribe dRucRec solo con iTipRec = 1 — aviso: el dato no llega al XML
        declare(rule("GEN007a").field("dRucRec").warning().dCodRes("4106").omitValue(),
                forbiddenIf(s -> tipo(s) == 2, TrGeVeNotRec::getdRucRec));
        declare(rule("GEN008").field("dDVRec").error().dCodRes("4107").omitValue(),
                passes(s -> !(tipo(s) == 1 && (!present(s.e().getdDVRec()) || !validDV(s.e().getdRucRec(), s.e().getdDVRec())))));
        // M3-42: TrGeVeNotRec escribe dDVRec solo con iTipRec = 1 — aviso: el dato no llega al XML
        declare(rule("GEN008a").field("dDVRec").warning().dCodRes("4108").omitValue(),
                forbiddenIf(s -> tipo(s) == 2, TrGeVeNotRec::getdDVRec));
        declare(rule("GEN009").field("dTipIDRec").error().dCodRes("4109").showValue(),
                requiredIf(s -> tipo(s) == 2, TrGeVeNotRec::getdTipIDRec));
        declare(rule("JBS-EVENTO-17").field("dTipIDRec").error().showValue(),
                passes(s -> s.e().getdTipIDRec() == null || s.e().getdTipIDRec().getVal() <= 4, s -> s.e().getdTipIDRec().getVal()));
        // M3-42: TrGeVeNotRec escribe dTipIDRec solo con iTipRec ≠ 1 — aviso: el dato no llega al XML
        declare(rule("GEN009a").field("dTipIDRec").warning().dCodRes("4110").showValue(),
                forbiddenIf(s -> tipo(s) == 1, e -> e.getdTipIDRec() == null ? null : e.getdTipIDRec().getVal()));
        declare(rule("GEN010").field("dNumID").error().dCodRes("4111").omitValue(),
                requiredIf(s -> tipo(s) == 2, TrGeVeNotRec::getdNumID));
        declare(rule("JBS-EVENTO-18").field("dNumID").error().omitValue(),
                length(TrGeVeNotRec::getdNumID, 1, 20));
        // I3-43 (mismo defecto que E3-26): trGeVeNotRec.dNumID es tdNumDocId (Evento_v150.xsd:57)
        declare(rule("JBS-EVENTO-54").field("dNumID").error().omitValue().source(RuleSource.MT_V150),
                passes(s -> ValueChecks.validDocumentNumber(s.e().getdNumID()), s -> s.e().getdNumID()));
        // M3-42: TrGeVeNotRec escribe dNumID solo con iTipRec ≠ 1 — aviso: el dato no llega al XML
        declare(rule("GEN010a").field("dNumID").warning().dCodRes("4112").omitValue(),
                forbiddenIf(s -> tipo(s) == 1, TrGeVeNotRec::getdNumID));
        declare(rule("JBS-EVENTO-19").field("dTotalGs").error().showValue(),
                passes(s -> s.e().getdTotalGs() != null && ValueChecks.decimal(s.e().getdTotalGs(), 15, 8) && s.e().getdTotalGs().signum() >= 0,
                s -> s.e().getdTotalGs()));
    }

    @Override
    protected List<Subject<TrGesEve, TrGeVeNotRec>> subjects(TrGesEve raiz) {
        return raiz.getgGroupTiEvt() == null ? none() : unique(raiz.getgGroupTiEvt().getrGeVeNotRec());
    }

    private static int tipo(Subject<TrGesEve, TrGeVeNotRec> s) {
        return s.e().getiTipRec() == null ? 0 : s.e().getiTipRec().getVal();
    }
}

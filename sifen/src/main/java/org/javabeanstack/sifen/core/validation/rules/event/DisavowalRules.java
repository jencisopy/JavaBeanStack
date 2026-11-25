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
import org.javabeanstack.sifen.core.fields.request.event.TrGeVeDescon;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;
import java.time.Duration;

/**
 * Evento de desconocimiento (rGeVeDescon, GED).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class DisavowalRules extends RuleSet<TrGesEve, TrGeVeDescon> {

    DisavowalRules() {
        super("EVENTO-rGeVeDescon", RuleGroup.EVENTO, "rEve.gGroupTiEvt.rGeVeDescon");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("dRucRec", s -> (s.e().getiTipRec() == null ? 0 : s.e().getiTipRec().getVal()) != 1); // TrGeVeDescon: RUC solo con iTipRec = 1
        notWrittenIf("dDVRec", s -> (s.e().getiTipRec() == null ? 0 : s.e().getiTipRec().getVal()) != 1); // TrGeVeDescon: RUC solo con iTipRec = 1
        notWrittenIf("dTipIDRec", s -> (s.e().getiTipRec() == null ? 0 : s.e().getiTipRec().getVal()) == 1); // TrGeVeDescon: documento solo con iTipRec ≠ 1
        notWrittenIf("dNumID", s -> (s.e().getiTipRec() == null ? 0 : s.e().getiTipRec().getVal()) == 1); // TrGeVeDescon: documento solo con iTipRec ≠ 1
        declare(rule("GED002c").field("Id").error().dCodRes("4252").showValue(),
                passes(s -> validCdc(s.e().getId())));
        declare(rule("JBS-EVENTO-20").field("dFecEmi").error().showValue().source(RuleSource.NT_019),
                required(TrGeVeDescon::getdFecEmi));
        declare(rule("GED003").field("dFecEmi").warning().dCodRes("4253").showValue().source(RuleSource.NT_019),
                passes(s -> s.e().getdFecEmi() == null || Duration.between(s.e().getdFecEmi(), s.ctx().getNow()).toDays() <= 45, s -> s.e().getdFecEmi()));
        declare(rule("JBS-EVENTO-21").field("dFecRecep").error().showValue(),
                required(TrGeVeDescon::getdFecRecep));
        declare(rule("GED004").field("dFecRecep").error().dCodRes("4254").showValue(),
                passes(s -> s.e().getdFecRecep() == null || s.e().getdFecEmi() == null || !s.e().getdFecRecep().isBefore(s.e().getdFecEmi()), s -> s.e().getdFecRecep()));
        declare(rule("JBS-EVENTO-22").field("iTipRec").error().showValue(),
                required(TrGeVeDescon::getiTipRec));
        declare(rule("JBS-EVENTO-23").field("dNomRec").error().lengthOnly(),
                requiredWithLength(TrGeVeDescon::getdNomRec, 4, 60));
        declare(rule("GED007").field("dRucRec").error().dCodRes("4255").omitValue(),
                passes(s -> !(tipo(s) == 1 && !present(s.e().getdRucRec())) && ValueChecks.length(s.e().getdRucRec(), 3, 8), s -> s.e().getdRucRec()));
        // M3-42: TrGeVeDescon escribe dRucRec solo con iTipRec = 1 — aviso: el dato no llega al XML
        declare(rule("JBS-EVENTO-24").field("dRucRec").warning().dCodRes("4256").omitValue(),
                forbiddenIf(s -> tipo(s) == 2, TrGeVeDescon::getdRucRec));
        declare(rule("GED008").field("dDVRec").error().dCodRes("4257").omitValue(),
                passes(s -> !(tipo(s) == 1 && (!present(s.e().getdDVRec()) || !validDV(s.e().getdRucRec(), s.e().getdDVRec())))));
        // M3-42: TrGeVeDescon escribe dDVRec solo con iTipRec = 1 — aviso: el dato no llega al XML
        declare(rule("JBS-EVENTO-25").field("dDVRec").warning().dCodRes("4258").omitValue(),
                forbiddenIf(s -> tipo(s) == 2, TrGeVeDescon::getdDVRec));
        declare(rule("GED009").field("dTipIDRec").error().dCodRes("4259").showValue(),
                passes(s -> !((tipo(s) == 2 && s.e().getdTipIDRec() == null) || (s.e().getdTipIDRec() != null && s.e().getdTipIDRec().getVal() > 4))));
        // M3-42: TrGeVeDescon escribe dTipIDRec solo con iTipRec ≠ 1 — aviso: el dato no llega al XML
        declare(rule("GED009a").field("dTipIDRec").warning().dCodRes("4260").showValue(),
                forbiddenIf(s -> tipo(s) == 1, e -> e.getdTipIDRec() == null ? null : e.getdTipIDRec().getVal()));
        declare(rule("GED010").field("dNumID").error().dCodRes("4261").omitValue(),
                passes(s -> !(tipo(s) == 2 && !present(s.e().getdNumID())) && ValueChecks.length(s.e().getdNumID(), 1, 20), s -> s.e().getdNumID()));
        // M3-42: TrGeVeDescon escribe dNumID solo con iTipRec ≠ 1 — aviso: el dato no llega al XML
        // I3-43 (mismo defecto que E3-26): trGeVeDescon.dNumID es tdNumDocId (Evento_v150.xsd:101)
        declare(rule("JBS-EVENTO-55").field("dNumID").error().omitValue().source(RuleSource.MT_V150),
                passes(s -> ValueChecks.validDocumentNumber(s.e().getdNumID()), s -> s.e().getdNumID()));
        declare(rule("JBS-EVENTO-26").field("dNumID").warning().dCodRes("4262").omitValue(),
                forbiddenIf(s -> tipo(s) == 1, TrGeVeDescon::getdNumID));
        declare(rule("JBS-EVENTO-27").field("mOtEve").error().showValue(),
                requiredWithLength(TrGeVeDescon::getmOtEve, 5, 500));
    }

    @Override
    protected List<Subject<TrGesEve, TrGeVeDescon>> subjects(TrGesEve raiz) {
        return raiz.getgGroupTiEvt() == null ? none() : unique(raiz.getgGroupTiEvt().getrGeVeDescon());
    }

    private static int tipo(Subject<TrGesEve, TrGeVeDescon> s) {
        return s.e().getiTipRec() == null ? 0 : s.e().getiTipRec().getVal();
    }
}

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
import org.javabeanstack.sifen.core.fields.request.event.TrGeVeInu;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;

/**
 * Evento de inutilización (rGeVeInu, GEI).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class NumberInvalidationRules extends RuleSet<TrGesEve, TrGeVeInu> {

    NumberInvalidationRules() {
        super("EVENTO-rGeVeInu", RuleGroup.EVENTO, "rEve.gGroupTiEvt.rGeVeInu");
        declare(rule("JBS-EVENTO-05").field("dNumTim").error().showValue(),
                passes(s -> s.e().getdNumTim() > 0 && digitCount(s.e().getdNumTim()) == 8, s -> s.e().getdNumTim()));
        declare(rule("JBS-EVENTO-06").field("dEst").error().showValue(),
                passes(s -> present(s.e().getdEst()) && digits(s.e().getdEst(), 3, 3), s -> s.e().getdEst()));
        declare(rule("JBS-EVENTO-07").field("dPunExp").error().showValue(),
                passes(s -> present(s.e().getdPunExp()) && digits(s.e().getdPunExp(), 3, 3), s -> s.e().getdPunExp()));
        declare(rule("JBS-EVENTO-08").field("dNumIn").error().showValue(),
                passes(s -> numero7(s.e().getdNumIn()), s -> s.e().getdNumIn()));
        declare(rule("JBS-EVENTO-09").field("dNumFin").error().showValue(),
                passes(s -> numero7(s.e().getdNumFin()), s -> s.e().getdNumFin()));
        declare(rule("GEI006a").field("dNumFin").error().dCodRes("4068").showValue(),
                passes(s -> !(numero7(s.e().getdNumIn()) && numero7(s.e().getdNumFin()))
                || Integer.parseInt(s.e().getdNumFin()) >= Integer.parseInt(s.e().getdNumIn())));
        declare(rule("GEI006").field("dNumFin").error().dCodRes("4067").showValue(),
                passes(s -> !(numero7(s.e().getdNumIn()) && numero7(s.e().getdNumFin()))
                || Integer.parseInt(s.e().getdNumFin()) - Integer.parseInt(s.e().getdNumIn()) <= 1000));
        declare(rule("JBS-EVENTO-10").field("iTiDE").error().showValue(),
                required(TrGeVeInu::getiTiDE));
        declare(rule("JBS-EVENTO-11").field("mOtEve").error().showValue(),
                requiredWithLength(TrGeVeInu::getmOtEve, 5, 500));
    }

    @Override
    protected List<Subject<TrGesEve, TrGeVeInu>> subjects(TrGesEve raiz) {
        return raiz.getgGroupTiEvt() == null ? none() : unique(raiz.getgGroupTiEvt().getrGeVeInu());
    }

    private static boolean numero7(String n) {
        return present(n) && digits(n, 7, 7);
    }
}

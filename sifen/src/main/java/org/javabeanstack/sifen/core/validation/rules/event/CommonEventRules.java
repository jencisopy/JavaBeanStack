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
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;
import org.javabeanstack.sifen.core.fields.request.event.TgGroupTiEvt;

/**
 * Eventos: campos comunes de rEve (Id, firma, versión, un solo evento).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class CommonEventRules extends RuleSet<TrGesEve, TrGesEve> {

    CommonEventRules() {
        super("EVENTO-comunes", RuleGroup.EVENTO, "rEve");
        declare(rule("JBS-EVENTO-01").field("Id").error().showValue(),
                passes(s -> present(s.e().getId()) && digits(s.e().getId(), 1, 10), s -> s.e().getId()));
        declare(rule("JBS-EVENTO-02").field("dFecFirma").error().showValue(),
                required(TrGesEve::getdFecFirma));
        declare(rule("GDE004").field("dFecFirma").warning().dCodRes("4008/4071").showValue(),
                passes(s -> s.e().getdFecFirma() == null || !s.e().getdFecFirma().isAfter(s.ctx().getNow()), s -> s.e().getdFecFirma()));
        declareNotEvaluated(rule("GDE005").field("dVerFor").error().dCodRes("4000").showValue(),
                NotEvaluatedReason.CONSTANT);
        declare(rule("JBS-EVENTO-03").field("gGroupTiEvt").error().showValue(),
                passes(s -> cantidad(s.e().getgGroupTiEvt()) == 1));
    }

    @Override
    protected List<Subject<TrGesEve, TrGesEve>> subjects(TrGesEve raiz) {
        return unique(raiz);
    }

    /** Eventos cargados en el grupo (la librería escribe solo el primero de los ocho que conoce). */
    static int cantidad(TgGroupTiEvt g) {
        if (g == null) {
            return 0;
        }
        int n = 0;
        Object[] eventos = {g.getrGeVeCan(), g.getrGeVeInu(), g.getrGeVeNotRec(), g.getrGeVeConf(), g.getrGeVeDisconf(),
            g.getrGeVeDescon(), g.getrGeVeTr(), g.getrGeVeNom()};
        for (Object ev : eventos) {
            if (ev != null) {
                n++;
            }
        }
        return n;
    }
}

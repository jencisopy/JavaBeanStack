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
import org.javabeanstack.sifen.core.fields.request.de.TgActEco;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;

/**
 * Grupo D del MT, D2.1: gActEco[] (actividades económicas del emisor).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class EconomicActivityRules extends RuleSet<DocumentoElectronico, TgActEco> {

    EconomicActivityRules() {
        super("D-gActEco", RuleGroup.D, "gDatGralOpe.gEmis.gActEco[]");
        declare(rule("JBS-D-32").field("cActEco").error().showValue(),
                onNull(s -> s.e() != null && present(s.e().getcActEco()) && ValueChecks.length(s.e().getcActEco(), 1, 8), s -> s.e() == null ? null : s.e().getcActEco()));
        declare(rule("JBS-D-33").field("dDesActEco").error().showValue(),
                onNull(s -> s.e() != null && present(s.e().getdDesActEco()) && ValueChecks.length(s.e().getdDesActEco(), 1, 300), s -> s.e() == null ? null : s.e().getdDesActEco()));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgActEco>> subjects(DocumentoElectronico raiz) {
        return raiz.getgDatGralOpe() == null || raiz.getgDatGralOpe().getgEmis() == null ? none()
                : perItem(raiz.getgDatGralOpe().getgEmis().getgActEcoList());
    }
}

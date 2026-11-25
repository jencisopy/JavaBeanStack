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
import org.javabeanstack.sifen.core.fields.request.de.TdDatGralOpe;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;
import java.time.Duration;

/**
 * Grupo D del MT: gDatGralOpe (presencia y fecha de emisión).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class GeneralDataRules extends RuleSet<DocumentoElectronico, TdDatGralOpe> {

    GeneralDataRules() {
        super("D-gDatGralOpe", RuleGroup.D, "gDatGralOpe");
        declare(rule("JBS-D-01").field("").error().showValue(),
                groupRequiredIf(s -> true));
        declare(rule("JBS-D-02").field("dFeEmiDE").error().showValue(),
                required(TdDatGralOpe::getdFeEmiDE));
        declare(rule("D002a").field("dFeEmiDE").error().dCodRes("1156").showValue(),
                passes(s -> s.e().getdFeEmiDE() == null || !s.e().getdFeEmiDE().isBefore(DEData.SIFEN_START), s -> s.e().getdFeEmiDE()));
        declare(rule("D002").field("dFeEmiDE").warning().dCodRes("1150").showValue(),
                passes(s -> s.e().getdFeEmiDE() == null || Duration.between(s.e().getdFeEmiDE(), s.ctx().getNow()).toHours() <= 720, s -> s.e().getdFeEmiDE()));
        declare(rule("D002f").field("dFeEmiDE").warning().dCodRes("1151").showValue(),
                passes(s -> s.e().getdFeEmiDE() == null || Duration.between(s.ctx().getNow(), s.e().getdFeEmiDE()).toHours() <= 120, s -> s.e().getdFeEmiDE()));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TdDatGralOpe>> subjects(DocumentoElectronico raiz) {
        return unique(raiz.getgDatGralOpe());
    }
}

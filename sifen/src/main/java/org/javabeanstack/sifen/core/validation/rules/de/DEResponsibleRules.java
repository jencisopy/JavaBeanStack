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
import org.javabeanstack.sifen.core.fields.request.de.TgRespDE;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;

/**
 * Grupo D del MT, D2.2: gRespDE (responsable de la generación, opcional).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class DEResponsibleRules extends RuleSet<DocumentoElectronico, TgRespDE> {

    DEResponsibleRules() {
        super("D-gRespDE", RuleGroup.D, "gDatGralOpe.gEmis.gRespDE");
        declare(rule("JBS-D-34").field("iTipIDRespDE").error().showValue(),
                required(TgRespDE::getiTipIDRespDE));
        declare(rule("JBS-D-35").field("dDTipIDRespDE").error().showValue(),
                passes(s -> s.e().getiTipIDRespDE() == null || s.e().getiTipIDRespDE().getVal() != 9
                || (present(s.e().getdDTipIDRespDE()) && ValueChecks.length(s.e().getdDTipIDRespDE(), 9, 41)), s -> s.e().getdDTipIDRespDE()));
        declare(rule("JBS-D-36").field("dNumIDRespDE").error().omitValue(),
                requiredWithLength(TgRespDE::getdNumIDRespDE, 1, 20));
        declare(rule("JBS-D-61").field("dNumIDRespDE").error().omitValue().source(RuleSource.MT_V150),
                passes(s -> ValueChecks.validDocumentNumber(s.e().getdNumIDRespDE()), s -> s.e().getdNumIDRespDE()));
        declare(rule("JBS-D-37").field("dNomRespDE").error().lengthOnly(),
                requiredWithLength(TgRespDE::getdNomRespDE, 4, 255));
        declare(rule("JBS-D-38").field("dCarRespDE").error().lengthOnly(),
                requiredWithLength(TgRespDE::getdCarRespDE, 4, 100));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgRespDE>> subjects(DocumentoElectronico raiz) {
        return raiz.getgDatGralOpe() == null || raiz.getgDatGralOpe().getgEmis() == null ? none()
                : unique(raiz.getgDatGralOpe().getgEmis().getgRespDE());
    }
}

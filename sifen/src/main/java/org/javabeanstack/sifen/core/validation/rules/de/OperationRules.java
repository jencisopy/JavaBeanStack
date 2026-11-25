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
import org.javabeanstack.sifen.core.fields.request.de.TgOpeDE;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;

/**
 * Grupo B del MT: gOpeDE (tipo de emisión, código de seguridad, informaciones).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class OperationRules extends RuleSet<DocumentoElectronico, TgOpeDE> {

    OperationRules() {
        super("B-gOpeDE", RuleGroup.B, "gOpeDE");
        declare(rule("JBS-B-01").field("").error().showValue(),
                groupRequiredIf(s -> true));
        declare(rule("JBS-B-02").field("iTipEmi").error().showValue(),
                required(TgOpeDE::getiTipEmi));
        declare(rule("B002").field("iTipEmi").error().dCodRes("1050").showValue(),
                passes(s -> s.e().getiTipEmi() == null || s.e().getiTipEmi().getVal() != 2, s -> s.e().getiTipEmi().getVal()));
        declareNotEvaluated(rule("B003").field("dDesTipEmi").error().dCodRes("1051").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-B-03").field("dCodSeg").error().omitValue(),
                required(TgOpeDE::getdCodSeg));
        declare(rule("JBS-B-04").field("dCodSeg").error().omitValue(),
                passes(s -> !present(s.e().getdCodSeg()) || (digits(s.e().getdCodSeg(), 9, 9) && !"000000000".equals(s.e().getdCodSeg())), s -> s.e().getdCodSeg()));
        declare(rule("JBS-B-05").field("dInfoEmi").error().lengthOnly(),
                length(TgOpeDE::getdInfoEmi, 1, 3000));
        declare(rule("JBS-B-06").field("dInfoFisc").error().showValue().source(RuleSource.NT_007),
                requiredIf(s -> DEData.iTiDE(s.root()) == 7, TgOpeDE::getdInfoFisc));
        declare(rule("JBS-B-07").field("dInfoFisc").error().lengthOnly(),
                length(TgOpeDE::getdInfoFisc, 1, 3000));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgOpeDE>> subjects(DocumentoElectronico raiz) {
        return unique(raiz.getgOpeDE());
    }
}

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
import org.javabeanstack.sifen.core.fields.request.de.TgTimb;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;

/**
 * Grupo C del MT: gTimb (tipo de DE, timbrado, establecimiento, numeración).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class TimbradoRules extends RuleSet<DocumentoElectronico, TgTimb> {

    TimbradoRules() {
        super("C-gTimb", RuleGroup.C, "gTimb");
        declare(rule("JBS-C-01").field("").error().showValue(),
                groupRequiredIf(s -> true));
        declare(rule("JBS-C-02").field("iTiDE").error().showValue(),
                required(TgTimb::getiTiDE));
        declare(rule("JBS-C-03").field("iTiDE").error().showValue(),
                passes(s -> s.e().getiTiDE() == null || soportado(s.e().getiTiDE().getVal()), s -> s.e().getiTiDE().getVal()));
        declareNotEvaluated(rule("C003").field("dDesTiDE").error().dCodRes("1100").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-C-04").field("dNumTim").error().showValue(),
                passes(s -> s.e().getdNumTim() != 0));
        declare(rule("JBS-C-05").field("dNumTim").error().showValue(),
                passes(s -> s.e().getdNumTim() == 0 || (s.e().getdNumTim() > 0 && digitCount(s.e().getdNumTim()) == 8), s -> s.e().getdNumTim()));
        declare(rule("JBS-C-06").field("dEst").error().showValue(),
                required(TgTimb::getdEst));
        declare(rule("JBS-C-07").field("dEst").error().showValue(),
                passes(s -> !present(s.e().getdEst()) || digits(s.e().getdEst(), 3, 3), s -> s.e().getdEst()));
        declare(rule("JBS-C-08").field("dPunExp").error().showValue(),
                required(TgTimb::getdPunExp));
        declare(rule("JBS-C-09").field("dPunExp").error().showValue(),
                passes(s -> !present(s.e().getdPunExp()) || digits(s.e().getdPunExp(), 3, 3), s -> s.e().getdPunExp()));
        declare(rule("JBS-C-10").field("dNumDoc").error().showValue(),
                required(TgTimb::getdNumDoc));
        declare(rule("JBS-C-11").field("dNumDoc").error().showValue(),
                passes(s -> !present(s.e().getdNumDoc()) || (digits(s.e().getdNumDoc(), 7, 7) && !"0000000".equals(s.e().getdNumDoc())), s -> s.e().getdNumDoc()));
        declare(rule("JBS-C-12").field("dSerieNum").error().showValue(),
                passes(s -> s.e().getdSerieNum() == null || s.e().getdSerieNum().matches("[A-Z]{2}"), s -> s.e().getdSerieNum()));
        declare(rule("JBS-C-13").field("dFeIniT").error().showValue(),
                required(TgTimb::getdFeIniT));
        declare(rule("JBS-C-14").field("dFeIniT").warning().showValue(),
                passes(s -> s.e().getdFeIniT() == null || s.root().getgDatGralOpe() == null || s.root().getgDatGralOpe().getdFeEmiDE() == null
                || !s.e().getdFeIniT().isAfter(s.root().getgDatGralOpe().getdFeEmiDE().toLocalDate()), s -> s.e().getdFeIniT()));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgTimb>> subjects(DocumentoElectronico raiz) {
        return unique(raiz.getgTimb());
    }

    private static boolean soportado(int tipo) {
        return tipo == 1 || tipo == 4 || tipo == 5 || tipo == 6 || tipo == 7;
    }
}

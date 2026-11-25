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
import org.javabeanstack.sifen.core.fields.request.de.TgGrupAdi;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;

/**
 * Grupo E del MT, E9.5: gGrupAdi (datos adicionales de uso comercial).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class AdditionalDataRules extends RuleSet<DocumentoElectronico, TgGrupAdi> {

    AdditionalDataRules() {
        super("E-gGrupAdi", RuleGroup.E, "gDtipDE.gCamEsp.gGrupAdi");
        declare(rule("JBS-E-142").field("dCiclo").error().showValue(),
                length(TgGrupAdi::getdCiclo, 1, 15));
        declare(rule("E822").field("dFecIniC").error().dCodRes("2050").showValue(),
                requiredIf(s -> present(s.e().getdCiclo()), TgGrupAdi::getdFecIniC));
        // M3-42: TgGrupAdi escribe dFecIniC solo con dCiclo — aviso: el dato no llega al XML
        declare(rule("E822a").field("dFecIniC").warning().dCodRes("2051").showValue(),
                forbiddenIf(s -> !present(s.e().getdCiclo()), TgGrupAdi::getdFecIniC));
        declare(rule("E823").field("dFecFinC").error().dCodRes("2052").showValue(),
                requiredIf(s -> s.e().getdFecIniC() != null, TgGrupAdi::getdFecFinC));
        // M3-42: TgGrupAdi escribe dFecFinC solo con dCiclo (y dFecIniC, E822) — aviso: el dato no llega al XML
        declare(rule("E823a").field("dFecFinC").warning().dCodRes("2053").showValue(),
                forbiddenIf(s -> s.e().getdFecIniC() == null, TgGrupAdi::getdFecFinC));
        declare(rule("E823b").field("dFecFinC").error().dCodRes("2054").showValue(),
                passes(s -> s.e().getdFecIniC() == null || s.e().getdFecFinC() == null || !s.e().getdFecFinC().isBefore(s.e().getdFecIniC()), s -> s.e().getdFecFinC()));
        declare(rule("JBS-E-143").field("dVencPag[]").error().showValue(),
                passes(s -> s.e().getdVencPagList() == null || s.e().getdVencPagList().size() <= 3, s -> s.e().getdVencPagList()));
        declare(rule("JBS-E-144").field("dContrato").error().showValue(),
                length(TgGrupAdi::getdContrato, 1, 30));
        declare(rule("JBS-E-145").field("dCodConDncp").error().showValue().source(RuleSource.NT_020),
                length(TgGrupAdi::getdCodConDncp, 1, 30));
        declare(rule("JBS-E-146").field("dSalAnt").error().showValue(),
                passes(s -> ValueChecks.decimal(s.e().getdSalAnt(), 15, 4), s -> s.e().getdSalAnt()));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgGrupAdi>> subjects(DocumentoElectronico raiz) {
        return DEData.gCamEsp(raiz) == null ? none() : unique(DEData.gCamEsp(raiz).getgGrupAdi());
    }
}

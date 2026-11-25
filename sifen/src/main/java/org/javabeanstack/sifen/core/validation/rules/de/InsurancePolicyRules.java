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
import org.javabeanstack.sifen.core.fields.request.de.TgGrupPolSeg;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;
import java.util.HashSet;
import java.util.Set;
import org.javabeanstack.sifen.core.fields.request.de.TgCamItem;

/**
 * Grupo E del MT, E9.3.1: gGrupPolSeg[] (pólizas).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class InsurancePolicyRules extends RuleSet<DocumentoElectronico, TgGrupPolSeg> {

    InsurancePolicyRules() {
        super("E-gGrupPolSeg", RuleGroup.E, "gDtipDE.gCamEsp.gGrupSeg.gGrupPolSeg[]");
        declare(rule("JBS-E-129").field("dPoliza").error().showValue(),
                onNull(s -> s.e() != null && present(s.e().getdPoliza()) && ValueChecks.length(s.e().getdPoliza(), 1, 20), s -> s.e() == null ? null : s.e().getdPoliza()));
        declare(rule("JBS-E-130").field("dUnidVig").error().showValue(),
                requiredWithLength(TgGrupPolSeg::getdUnidVig, 3, 15));
        declare(rule("JBS-E-131").field("dVigencia").error().showValue(),
                required(TgGrupPolSeg::getdVigencia));
        declare(rule("JBS-E-132").field("dVigencia").error().showValue(),
                passes(s -> ValueChecks.decimal(s.e().getdVigencia(), 5, 1), s -> s.e().getdVigencia()));
        declare(rule("JBS-E-133").field("dNumPoliza").error().showValue(),
                requiredWithLength(TgGrupPolSeg::getdNumPoliza, 1, 25));
        declare(rule("JBS-E-134").field("dFecIniVig").error().showValue(),
                passes(s -> s.e().getdFecIniVig() == null || s.e().getdFecFinVig() == null || !s.e().getdFecIniVig().isAfter(s.e().getdFecFinVig())));
        declare(rule("JBS-E-135").field("dCodInt").error().showValue().source(RuleSource.NT_008),
                length(TgGrupPolSeg::getdCodInt, 1, 50));
        declare(rule("JBS-E-136").field("dCodInt").warning().showValue().source(RuleSource.NT_008),
                passes(s -> !present(s.e().getdCodInt()) || codigosItems(s.root()).contains(s.e().getdCodInt())));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgGrupPolSeg>> subjects(DocumentoElectronico raiz) {
        return DEData.gCamEsp(raiz) == null || DEData.gCamEsp(raiz).getgGrupSeg() == null ? none()
                : perItem(DEData.gCamEsp(raiz).getgGrupSeg().getgGrupPolSegList());
    }

    private static Set<String> codigosItems(DocumentoElectronico de) {
        Set<String> codigos = new HashSet<>();
        for (TgCamItem it : DEData.items(de)) {
            if (it != null && it.getdCodInt() != null) {
                codigos.add(it.getdCodInt());
            }
        }
        return codigos;
    }
}

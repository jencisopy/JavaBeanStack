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
import org.javabeanstack.sifen.core.fields.request.de.TgCamAE;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;

/**
 * Grupo E del MT, E4: gCamAE (autofactura).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class SelfInvoiceRules extends RuleSet<DocumentoElectronico, TgCamAE> {

    SelfInvoiceRules() {
        super("E-gCamAE", RuleGroup.E, "gDtipDE.gCamAE");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("", s -> org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 0 && org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 4); // TgDtipDE: gCamAE solo con iTiDE = 4
        declare(rule("E300").field("").error().dCodRes("2550").showValue(),
                groupRequiredIf(s -> DEData.iTiDE(s.root()) == 4));
        // M3-42: TgDtipDE escribe gCamAE solo con iTiDE = 4 — aviso: el dato no llega al XML
        declare(rule("E300a").field("").warning().dCodRes("2551").showValue(),
                groupForbiddenIf(s -> DEData.iTiDE(s.root()) != 0 && DEData.iTiDE(s.root()) != 4));
        declare(rule("JBS-E-10").field("iNatVen").error().showValue(),
                required(TgCamAE::getiNatVen));
        declareNotEvaluated(rule("JBS-E-11").field("dDesNatVen").error().showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-E-12").field("iTipIDVen").error().showValue(),
                required(TgCamAE::getiTipIDVen));
        declareNotEvaluated(rule("JBS-E-13").field("dDTipIDVen").error().showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-E-14").field("dNumIDVen").error().omitValue(),
                requiredWithLength(TgCamAE::getdNumIDVen, 1, 20));
        declare(rule("JBS-E-192").field("dNumIDVen").error().omitValue().source(RuleSource.MT_V150),
                passes(s -> ValueChecks.validDocumentNumber(s.e().getdNumIDVen()), s -> s.e().getdNumIDVen()));
        declare(rule("JBS-E-15").field("dNomVen").error().lengthOnly(),
                requiredWithLength(TgCamAE::getdNomVen, 4, 60));
        declare(rule("JBS-E-16").field("dDirVen").error().lengthOnly(),
                requiredWithLength(TgCamAE::getdDirVen, 1, 255));
        declare(rule("JBS-E-17").field("dNumCasVen").error().omitValue(),
                passes(s -> maxDigits(s.e().getdNumCasVen(), 6), s -> s.e().getdNumCasVen()));
        declare(rule("JBS-E-18").field("cDepVen").error().showValue(),
                required(TgCamAE::getcDepVen));
        declareNotEvaluated(rule("E311").field("dDesDepVen").error().dCodRes("2552").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-E-19").field("cDisVen").error().showValue(),
                passes(s -> maxDigits(s.e().getcDisVen(), 4), s -> s.e().getcDisVen()));
        declare(rule("E313b").field("dDesDisVen").error().dCodRes("2561").showValue(),
                passes(s -> !(s.e().getcDisVen() != 0 && !present(s.e().getdDesDisVen())) && ValueChecks.length(s.e().getdDesDisVen(), 1, 30)));
        declare(rule("JBS-E-20").field("cCiuVen").error().showValue(),
                passes(s -> s.e().getcCiuVen() > 0 && digitCount(s.e().getcCiuVen()) <= 5, s -> s.e().getcCiuVen()));
        declare(rule("E315").field("dDesCiuVen").error().dCodRes("2555").showValue(),
                requiredWithLength(TgCamAE::getdDesCiuVen, 1, 30));
        declare(rule("JBS-E-21").field("dDirProv").error().lengthOnly(),
                requiredWithLength(TgCamAE::getdDirProv, 1, 255));
        declare(rule("JBS-E-22").field("cDepProv").error().showValue(),
                required(TgCamAE::getcDepProv));
        declareNotEvaluated(rule("E318").field("dDesDepProv").error().dCodRes("2556").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-E-23").field("cDisProv").error().showValue(),
                passes(s -> maxDigits(s.e().getcDisProv(), 4), s -> s.e().getcDisProv()));
        declare(rule("E320").field("dDesDisProv").error().dCodRes("2558").showValue(),
                passes(s -> !(s.e().getcDisProv() != 0 && !present(s.e().getdDesDisProv())) && ValueChecks.length(s.e().getdDesDisProv(), 1, 30)));
        declare(rule("JBS-E-24").field("cCiuProv").error().showValue(),
                passes(s -> s.e().getcCiuProv() > 0 && digitCount(s.e().getcCiuProv()) <= 5, s -> s.e().getcCiuProv()));
        declare(rule("E322").field("dDesCiuProv").error().dCodRes("2559").showValue(),
                requiredWithLength(TgCamAE::getdDesCiuProv, 1, 30));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgCamAE>> subjects(DocumentoElectronico raiz) {
        return raiz.getgDtipDE() == null ? none() : unique(raiz.getgDtipDE().getgCamAE());
    }
}

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
import org.javabeanstack.sifen.core.fields.request.de.TgEmis;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.validation.support.Legends;

/**
 * Grupo D del MT, D2: gEmis (emisor; D105 con las dos leyendas de pruebas, Q14).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class IssuerRules extends RuleSet<DocumentoElectronico, TgEmis> {

    IssuerRules() {
        super("D-gEmis", RuleGroup.D, "gDatGralOpe.gEmis");
        declare(rule("JBS-D-06").field("").error().showValue(),
                groupRequiredIf(s -> true));
        declare(rule("JBS-D-07").field("dRucEm").error().omitValue(),
                required(TgEmis::getdRucEm));
        declare(rule("JBS-D-08").field("dRucEm").error().omitValue(),
                passes(s -> !present(s.e().getdRucEm()) || (ValueChecks.length(s.e().getdRucEm(), 3, 8) && !s.e().getdRucEm().contains("-")), s -> s.e().getdRucEm()));
        declare(rule("JBS-D-09").field("dDVEmi").error().omitValue(),
                required(TgEmis::getdDVEmi));
        declare(rule("D102").field("dDVEmi").error().dCodRes("1253").omitValue(),
                passes(s -> !present(s.e().getdDVEmi()) || validDV(s.e().getdRucEm(), s.e().getdDVEmi())));
        declare(rule("JBS-D-10").field("iTipCont").error().showValue(),
                required(TgEmis::getiTipCont));
        declare(rule("JBS-D-11").field("dNomEmi").error().omitValue(),
                required(TgEmis::getdNomEmi));
        declare(rule("JBS-D-12").field("dNomEmi").error().lengthOnly(),
                length(TgEmis::getdNomEmi, 4, 255));
        declare(rule("D105").field("dNomEmi").error().dCodRes("1263").omitValue(),
                passes(s -> !present(s.e().getdNomEmi()) || s.ctx().getEnvironment() != SifenConfig.Environment.DEV || Legends.isTestLegend(s.e().getdNomEmi(), false)));
        declare(rule("JBS-D-13").field("dNomEmi").error().dCodRes("1263").omitValue(),
                passes(s -> !present(s.e().getdNomEmi()) || s.ctx().getEnvironment() != SifenConfig.Environment.PROD || !Legends.isTestLegend(s.e().getdNomEmi(), true)));
        declare(rule("JBS-D-14").field("dNomFanEmi").error().lengthOnly(),
                length(TgEmis::getdNomFanEmi, 4, 255));
        declare(rule("JBS-D-15").field("dDirEmi").error().omitValue(),
                required(TgEmis::getdDirEmi));
        declare(rule("JBS-D-16").field("dDirEmi").error().lengthOnly(),
                length(TgEmis::getdDirEmi, 1, 255));
        declare(rule("JBS-D-17").field("dNumCas").error().omitValue(),
                passes(s -> digits(s.e().getdNumCas(), 1, 6), s -> s.e().getdNumCas()));
        declare(rule("JBS-D-18").field("dCompDir1").error().lengthOnly(),
                length(TgEmis::getdCompDir1, 1, 255));
        declare(rule("JBS-D-19").field("dCompDir2").error().lengthOnly(),
                length(TgEmis::getdCompDir2, 1, 255));
        declare(rule("JBS-D-20").field("cDepEmi").error().showValue(),
                required(TgEmis::getcDepEmi));
        declareNotEvaluated(rule("D112").field("dDesDepEmi").error().dCodRes("1254").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-D-21").field("cDisEmi").error().showValue(),
                passes(s -> maxDigits(s.e().getcDisEmi(), 4), s -> s.e().getcDisEmi()));
        declare(rule("D114").field("dDesDisEmi").error().dCodRes("1256").showValue(),
                requiredIf(s -> s.e().getcDisEmi() != 0, TgEmis::getdDesDisEmi));
        declare(rule("JBS-D-22").field("dDesDisEmi").error().showValue(),
                length(TgEmis::getdDesDisEmi, 1, 30));
        declare(rule("JBS-D-23").field("cCiuEmi").error().showValue(),
                passes(s -> s.e().getcCiuEmi() > 0 && digitCount(s.e().getcCiuEmi()) <= 5, s -> s.e().getcCiuEmi()));
        declare(rule("JBS-D-24").field("dDesCiuEmi").error().showValue(),
                required(TgEmis::getdDesCiuEmi));
        declare(rule("JBS-D-25").field("dDesCiuEmi").error().showValue(),
                length(TgEmis::getdDesCiuEmi, 1, 30));
        declare(rule("JBS-D-26").field("dTelEmi").error().omitValue(),
                required(TgEmis::getdTelEmi));
        declare(rule("JBS-D-27").field("dTelEmi").error().lengthOnly(),
                length(TgEmis::getdTelEmi, 6, 15));
        declare(rule("JBS-D-28").field("dEmailE").error().omitValue(),
                required(TgEmis::getdEmailE));
        declare(rule("JBS-D-29").field("dEmailE").error().omitValue(),
                passes(s -> !present(s.e().getdEmailE()) || validEmail(s.e().getdEmailE()), s -> s.e().getdEmailE()));
        declare(rule("JBS-D-30").field("dDenSuc").error().showValue(),
                length(TgEmis::getdDenSuc, 1, 30));
        declare(rule("JBS-D-31").field("gActEco[]").error().showValue(),
                passes(s -> present(s.e().getgActEcoList()) && s.e().getgActEcoList().size() <= 9, s -> s.e().getgActEcoList()));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgEmis>> subjects(DocumentoElectronico raiz) {
        return raiz.getgDatGralOpe() == null ? none() : unique(raiz.getgDatGralOpe().getgEmis());
    }
}

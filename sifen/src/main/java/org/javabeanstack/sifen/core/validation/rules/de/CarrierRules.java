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
import org.javabeanstack.sifen.core.fields.request.de.TgCamTrans;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;

/**
 * Grupo E del MT, E10.4: gCamTrans (transportista; E3-05).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class CarrierRules extends RuleSet<DocumentoElectronico, TgCamTrans> {

    CarrierRules() {
        super("E-gCamTrans", RuleGroup.E, "gDtipDE.gTransp.gCamTrans");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("", s -> (org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) == 4 || org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) == 5 || org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) == 6) || (org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 0 && org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 7 && org.javabeanstack.sifen.core.validation.support.DEData.iModTrans(s.root()) != 0 && org.javabeanstack.sifen.core.validation.support.DEData.iModTrans(s.root()) != 1)); // TgTransp: gCamTrans solo con iTiDE = 7 o iModTrans = 1 (y nunca con 4/5/6)
        notWrittenIf("dRucTrans", s -> (s.e().getiNatTrans() == null ? 0 : s.e().getiNatTrans().getVal()) != 1); // TgCamTrans: dRucTrans solo con iNatTrans = 1
        notWrittenIf("dDVTrans", s -> (s.e().getiNatTrans() == null ? 0 : s.e().getiNatTrans().getVal()) != 1); // TgCamTrans: dDVTrans solo con iNatTrans = 1
        notWrittenIf("iTipIDTrans", s -> (s.e().getiNatTrans() == null ? 0 : s.e().getiNatTrans().getVal()) != 2); // TgCamTrans: iTipIDTrans solo con iNatTrans = 2
        notWrittenIf("dNumIDTrans", s -> (s.e().getiNatTrans() == null ? 0 : s.e().getiNatTrans().getVal()) != 2); // TgCamTrans: dNumIDTrans solo con iNatTrans = 2
        declare(rule("E980").field("").error().dCodRes("2300").showValue().source(RuleSource.NT_010),
                groupRequiredIf(s -> TransportRules.nre(s)));
        // M3-42: gTransp (y con él gCamTrans) no se escribe con iTiDE = 4/5/6 — aviso: el dato no llega al XML
        declare(rule("E980a").field("").warning().dCodRes("2301").showValue(),
                groupForbiddenIf(s -> TransportRules.sinTransporte(s)));
        declare(rule("JBS-E-180").field("iNatTrans").error().showValue(),
                required(TgCamTrans::getiNatTrans));
        declare(rule("JBS-E-181").field("dNomTrans").error().lengthOnly(),
                requiredWithLength(TgCamTrans::getdNomTrans, 4, 60));
        declare(rule("E983").field("dRucTrans").error().dCodRes("2302").omitValue(),
                required(TgCamTrans::getdRucTrans)); // M3-59: iNatTrans ≠ 1 lo excluye notWrittenIf
        declare(rule("JBS-E-182").field("dRucTrans").error().omitValue(),
                length(TgCamTrans::getdRucTrans, 3, 8));
        // M3-42: TgCamTrans escribe dRucTrans solo con iNatTrans = 1 — aviso: el dato no llega al XML
        declare(rule("E983c").field("dRucTrans").warning().dCodRes("2305").omitValue(),
                forbiddenIf(s -> nat(s) != 0 && nat(s) != 1, TgCamTrans::getdRucTrans));
        declare(rule("E984").field("dDVTrans").error().dCodRes("2306").omitValue(),
                passes(s -> !present(s.e().getdRucTrans()) || validDV(s.e().getdRucTrans(), String.valueOf(s.e().getdDVTrans()))));
        declare(rule("E985").field("iTipIDTrans").error().dCodRes("2307").showValue(),
                required(TgCamTrans::getiTipIDTrans)); // M3-59: iNatTrans ≠ 2 lo excluye notWrittenIf
        // M3-42: TgCamTrans escribe iTipIDTrans solo con iNatTrans = 2 — aviso: el dato no llega al XML
        declare(rule("E985a").field("iTipIDTrans").warning().dCodRes("2308").showValue(),
                forbiddenIf(s -> nat(s) == 1, e -> e.getiTipIDTrans() == null ? null : e.getiTipIDTrans().getVal()));
        declareNotEvaluated(rule("E986a").field("dDTipIDTrans").error().dCodRes("2310").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("E987").field("dNumIDTrans").error().dCodRes("2311").omitValue(),
                requiredIf(s -> s.e().getiTipIDTrans() != null, TgCamTrans::getdNumIDTrans));
        declare(rule("JBS-E-183").field("dNumIDTrans").error().omitValue(),
                length(TgCamTrans::getdNumIDTrans, 1, 20));
        declare(rule("JBS-E-193").field("dNumIDTrans").error().omitValue().source(RuleSource.MT_V150),
                passes(s -> ValueChecks.validDocumentNumber(s.e().getdNumIDTrans()), s -> s.e().getdNumIDTrans()));
        declareNotEvaluated(rule("E989a").field("dDesNacTrans").error().dCodRes("2313").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-E-184").field("dNumIDChof").error().omitValue(),
                requiredWithLength(TgCamTrans::getdNumIDChof, 1, 20));
        declare(rule("JBS-E-185").field("dNomChof").error().lengthOnly(),
                requiredWithLength(TgCamTrans::getdNomChof, 4, 60));
        declare(rule("JBS-E-186").field("dDomFisc").error().lengthOnly().source(RuleSource.NT_010),
                length(TgCamTrans::getdDomFisc, 1, 150));
        declare(rule("JBS-E-187").field("dDirChof").error().lengthOnly(),
                length(TgCamTrans::getdDirChof, 1, 255));
        declare(rule("JBS-E-188").field("dNombAg").error().lengthOnly(),
                length(TgCamTrans::getdNombAg, 4, 60));
        declare(rule("JBS-E-189").field("dRucAg").error().omitValue(),
                length(TgCamTrans::getdRucAg, 3, 8));
        declare(rule("JBS-E-190").field("dDVAg").error().omitValue(),
                passes(s -> !present(s.e().getdRucAg()) || validDV(s.e().getdRucAg(), String.valueOf(s.e().getdDVAg()))));
        declare(rule("JBS-E-191").field("dDirAge").error().lengthOnly(),
                passes(s -> !(present(s.e().getdNombAg()) && !present(s.e().getdDirAge())) && ValueChecks.length(s.e().getdDirAge(), 1, 255)));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgCamTrans>> subjects(DocumentoElectronico raiz) {
        return DEData.gTransp(raiz) == null ? none() : unique(DEData.gTransp(raiz).getgCamTrans());
    }

    private static int nat(Subject<DocumentoElectronico, TgCamTrans> s) {
        return s.e().getiNatTrans() == null ? 0 : s.e().getiNatTrans().getVal();
    }
}

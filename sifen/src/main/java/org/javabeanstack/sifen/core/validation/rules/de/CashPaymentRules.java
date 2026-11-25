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
import org.javabeanstack.sifen.core.fields.request.de.TgPaConEIni;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;
import org.javabeanstack.sifen.core.fields.request.de.TgCamCond;
import org.javabeanstack.sifen.core.fields.request.de.TgPagTarCD;
import org.javabeanstack.sifen.core.types.CMondT;

/**
 * Grupo E del MT, E7.1: gPaConEIni[] (formas de pago, tarjeta y cheque).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class CashPaymentRules extends RuleSet<DocumentoElectronico, TgPaConEIni> {

    CashPaymentRules() {
        super("E-gPaConEIni", RuleGroup.E, "gDtipDE.gCamCond.gPaConEIni[]");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("", s -> org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 0 && org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 1 && org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 4); // TgDtipDE: gCamCond (y su gPaConEIni) solo con iTiDE = 1/4
        notWrittenIf("gPagTarCD", s -> (s.e().getiTiPago() == null ? 0 : s.e().getiTiPago().getVal()) != 0 && (s.e().getiTiPago() == null ? 0 : s.e().getiTiPago().getVal()) != 3 && (s.e().getiTiPago() == null ? 0 : s.e().getiTiPago().getVal()) != 4); // TgPaConEIni: gPagTarCD solo con iTiPago = 3/4
        notWrittenIf("gPagCheq", s -> (s.e().getiTiPago() == null ? 0 : s.e().getiTiPago().getVal()) != 0 && (s.e().getiTiPago() == null ? 0 : s.e().getiTiPago().getVal()) != 2); // TgPaConEIni: gPagCheq solo con iTiPago = 2
        declare(rule("JBS-E-31").field("iTiPago").error().showValue(),
                onNull(s -> s.e() != null && present(s.e().getiTiPago()), s -> null));
        declare(rule("E606").field("iTiPago").error().dCodRes("1553").showValue(),
                passes(s -> !(tipoPago(s) == 16 && DEData.iTiDE(s.root()) == 1 && DEData.iIndPres(s.root()) != 5), s -> tipoPago(s)));
        declare(rule("E607").field("dDesTiPag").error().dCodRes("1554").showValue(),
                passes(s -> tipoPago(s) != 99 || (present(s.e().getdDesTiPag()) && ValueChecks.length(s.e().getdDesTiPag(), 4, 30)), s -> s.e().getdDesTiPag()));
        declare(rule("JBS-E-32").field("dMonTiPag").error().showValue(),
                required(TgPaConEIni::getdMonTiPag));
        declare(rule("JBS-E-33").field("dMonTiPag").error().showValue(),
                decimal(TgPaConEIni::getdMonTiPag, 15, 4, true));
        declare(rule("JBS-E-34").field("cMoneTiPag").error().showValue(),
                required(TgPaConEIni::getcMoneTiPag));
        declareNotEvaluated(rule("E610").field("dDMoneTiPag").error().dCodRes("1555").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("E611").field("dTiCamTiPag").error().dCodRes("1556").showValue(),
                requiredIf(s -> s.e().getcMoneTiPag() != null && s.e().getcMoneTiPag() != CMondT.PYG, TgPaConEIni::getdTiCamTiPag));
        declare(rule("JBS-E-35").field("dTiCamTiPag").error().showValue(),
                decimal(TgPaConEIni::getdTiCamTiPag, 5, 4, true));
        declare(rule("E611a").field("dTiCamTiPag").error().dCodRes("1557").showValue(),
                forbiddenIf(s -> s.e().getcMoneTiPag() == CMondT.PYG, TgPaConEIni::getdTiCamTiPag));
        declare(rule("E620").field("gPagTarCD").error().dCodRes("1600").showValue(),
                requiredIf(s -> tipoPago(s) == 3 || tipoPago(s) == 4, TgPaConEIni::getgPagTarCD));
        // M3-42: TgPaConEIni escribe gPagTarCD solo con iTiPago = 3/4 — aviso: el dato no llega al XML
        declare(rule("E620a").field("gPagTarCD").warning().dCodRes("1601").showValue(),
                forbiddenIf(s -> tipoPago(s) != 0 && tipoPago(s) != 3 && tipoPago(s) != 4, TgPaConEIni::getgPagTarCD));
        declare(rule("JBS-E-36").field("gPagTarCD.iDenTarj").error().showValue(),
                passes(s -> t(s) == null || present(t(s).getiDenTarj())));
        declare(rule("E622").field("gPagTarCD.dDesDenTarj").error().dCodRes("1602").showValue(),
                passes(s -> t(s) == null || t(s).getiDenTarj() == null || t(s).getiDenTarj().getVal() != 99
                || (present(t(s).getdDesDenTarj()) && ValueChecks.length(t(s).getdDesDenTarj(), 4, 20)), s -> t(s).getdDesDenTarj()));
        declare(rule("JBS-E-37").field("gPagTarCD.dRSProTar").error().showValue(),
                passes(s -> t(s) == null || ValueChecks.length(t(s).getdRSProTar(), 4, 60), s -> t(s).getdRSProTar()));
        declare(rule("JBS-E-38").field("gPagTarCD.dRUCProTar").error().showValue(),
                passes(s -> t(s) == null || ValueChecks.length(t(s).getdRUCProTar(), 3, 8), s -> t(s).getdRUCProTar()));
        declare(rule("JBS-E-39").field("gPagTarCD.dDVProTar").error().showValue(),
                passes(s -> t(s) == null || !present(t(s).getdRUCProTar()) || !(t(s).getdDVProTar() == 0 && computeDV(t(s).getdRUCProTar()) != null
                && !"0".equals(computeDV(t(s).getdRUCProTar())))));
        declare(rule("E624").field("gPagTarCD.dDVProTar").error().dCodRes("1604").showValue(),
                passes(s -> t(s) == null || !present(t(s).getdRUCProTar()) || t(s).getdDVProTar() == 0
                || validDV(t(s).getdRUCProTar(), String.valueOf(t(s).getdDVProTar()))));
        declare(rule("JBS-E-40").field("gPagTarCD.iForProPa").error().showValue(),
                passes(s -> t(s) == null || present(t(s).getiForProPa())));
        declare(rule("JBS-E-41").field("gPagTarCD.dCodAuOpe").error().omitValue(),
                passes(s -> t(s) == null || t(s).getdCodAuOpe() == 0
                || (t(s).getdCodAuOpe() > 0 && digitCount(t(s).getdCodAuOpe()) >= 6 && digitCount(t(s).getdCodAuOpe()) <= 10)));
        declare(rule("JBS-E-42").field("gPagTarCD.dNomTit").error().lengthOnly(),
                passes(s -> t(s) == null || ValueChecks.length(t(s).getdNomTit(), 4, 30), s -> t(s).getdNomTit()));
        declare(rule("JBS-E-43").field("gPagTarCD.dNumTarj").error().omitValue(),
                passes(s -> t(s) == null || t(s).getdNumTarj() == 0 || (t(s).getdNumTarj() > 0 && digitCount(t(s).getdNumTarj()) == 4)));
        declare(rule("E630").field("gPagCheq").error().dCodRes("1650").showValue(),
                requiredIf(s -> tipoPago(s) == 2, TgPaConEIni::getgPagCheq));
        // M3-42: TgPaConEIni escribe gPagCheq solo con iTiPago = 2 — aviso: el dato no llega al XML
        declare(rule("E630a").field("gPagCheq").warning().dCodRes("1651").showValue(),
                forbiddenIf(s -> tipoPago(s) != 0 && tipoPago(s) != 2, TgPaConEIni::getgPagCheq));
        declare(rule("JBS-E-44").field("gPagCheq.dNumCheq").error().omitValue(),
                passes(s -> s.e().getgPagCheq() == null || (present(s.e().getgPagCheq().getdNumCheq()) && digits(s.e().getgPagCheq().getdNumCheq(), 8, 8)),
                s -> s.e().getgPagCheq().getdNumCheq()));
        declare(rule("JBS-E-45").field("gPagCheq.dBcoEmi").error().showValue(),
                passes(s -> s.e().getgPagCheq() == null || (present(s.e().getgPagCheq().getdBcoEmi()) && ValueChecks.length(s.e().getgPagCheq().getdBcoEmi(), 4, 20)),
                s -> s.e().getgPagCheq().getdBcoEmi()));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgPaConEIni>> subjects(DocumentoElectronico raiz) {
        TgCamCond c = DEData.gCamCond(raiz);
        return c == null || c.getiCondOpe() == null ? none() : perItem(c.getgPaConEIniList());
    }

    private static int tipoPago(Subject<DocumentoElectronico, TgPaConEIni> s) {
        return s.e().getiTiPago() == null ? 0 : s.e().getiTiPago().getVal();
    }

    private static TgPagTarCD t(Subject<DocumentoElectronico, TgPaConEIni> s) {
        return s.e().getgPagTarCD();
    }
}

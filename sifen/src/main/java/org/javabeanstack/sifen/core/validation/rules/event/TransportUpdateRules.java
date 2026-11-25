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
package org.javabeanstack.sifen.core.validation.rules.event;

import static org.javabeanstack.sifen.core.validation.support.ValueChecks.*;

import java.util.List;
import org.javabeanstack.sifen.core.fields.request.event.TrGesEve;
import org.javabeanstack.sifen.core.fields.request.event.TrGeVeTr;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;

/**
 * Evento de actualización de datos del transporte (rGeVeTr, NT 018).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class TransportUpdateRules extends RuleSet<TrGesEve, TrGeVeTr> {

    TransportUpdateRules() {
        super("EVENTO-rGeVeTr", RuleGroup.EVENTO, "rEve.gGroupTiEvt.rGeVeTr");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("iTipIDTrans", s -> (s.e().getiNatTrans() == null ? 0 : s.e().getiNatTrans().getVal()) != 2); // TrGeVeTr: documento del transportista solo con iNatTrans = 2
        notWrittenIf("dNumIDTrans", s -> (s.e().getiNatTrans() == null ? 0 : s.e().getiNatTrans().getVal()) != 2); // TrGeVeTr: documento del transportista solo con iNatTrans = 2
        declare(rule("JBS-EVENTO-48").field("Id").error().showValue(),
                passes(s -> validCdc(s.e().getId())));
        declare(rule("JBS-EVENTO-49").field("dMotEv").error().showValue(),
                required(TrGeVeTr::getdMotEv));
        declare(rule("GET004a").field("cDepEnt").error().dCodRes("4301").showValue(),
                requiredIf(s -> mot(s) == 1, TrGeVeTr::getcDepEnt));
        declareNotEvaluated(rule("GET005a").field("dDesDepEnt").error().dCodRes("4303").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-EVENTO-50").field("cDisEnt").error().showValue(),
                passes(s -> maxDigits(s.e().getcDisEnt(), 4), s -> s.e().getcDisEnt()));
        declare(rule("GET007").field("dDesDisEnt").error().dCodRes("4304").showValue(),
                passes(s -> !mal(s.e().getcDisEnt() > 0, s.e().getdDesDisEnt(), 1, 30)));
        declare(rule("GET008").field("cCiuEnt").error().dCodRes("4306").showValue(),
                passes(s -> !(mot(s) == 1 && s.e().getcCiuEnt() == 0) && maxDigits(s.e().getcCiuEnt(), 5), s -> s.e().getcCiuEnt()));
        declare(rule("GET009").field("dDesCiuEnt").error().dCodRes("4307").showValue(),
                passes(s -> !mal(s.e().getcCiuEnt() > 0, s.e().getdDesCiuEnt(), 1, 30)));
        declare(rule("GET010").field("dDirEnt").error().dCodRes("4309").lengthOnly(),
                passes(s -> !mal(mot(s) == 1, s.e().getdDirEnt(), 1, 255), s -> s.e().getdDirEnt()));
        declare(rule("GET011").field("dNumCas").error().dCodRes("4310").showValue(),
                passes(s -> mot(s) != 1 || maxDigits(s.e().getdNumCas(), 6), s -> s.e().getdNumCas()));
        declare(rule("JBS-EVENTO-51").field("dCompDir1").error().lengthOnly(),
                length(TrGeVeTr::getdCompDir1, 1, 255));
        declare(rule("GET013").field("dNomChof").error().dCodRes("4311").lengthOnly(),
                passes(s -> !mal(mot(s) == 2, s.e().getdNomChof(), 4, 60), s -> s.e().getdNomChof()));
        declare(rule("GET014").field("dNumIDChof").error().dCodRes("4312").omitValue(),
                passes(s -> !mal(mot(s) == 2, s.e().getdNumIDChof(), 1, 20), s -> s.e().getdNumIDChof()));
        declare(rule("GET015").field("iNatTrans").error().dCodRes("4313").showValue(),
                requiredIf(s -> mot(s) == 3, TrGeVeTr::getiNatTrans));
        declare(rule("GET016").field("dRucTrans").error().dCodRes("4314").omitValue(),
                passes(s -> !mal(nat(s) == 1, s.e().getdRucTrans(), 3, 8), s -> s.e().getdRucTrans()));
        declare(rule("GET016c").field("dRucTrans").error().dCodRes("4317").omitValue(),
                forbiddenIf(s -> nat(s) != 0 && nat(s) != 1, TrGeVeTr::getdRucTrans));
        declare(rule("GET017").field("dDVTrans").error().dCodRes("4318").omitValue(),
                passes(s -> !(nat(s) == 1 && present(s.e().getdRucTrans())) || validDV(s.e().getdRucTrans(), s.e().getdDVTrans())));
        declare(rule("GET018").field("dNomTrans").error().dCodRes("4319").lengthOnly(),
                passes(s -> !mal(mot(s) == 3, s.e().getdNomTrans(), 4, 60), s -> s.e().getdNomTrans()));
        declare(rule("GET019").field("iTipIDTrans").error().dCodRes("4320").showValue(),
                passes(s -> !((nat(s) == 2 && doc(s) == 0) || doc(s) > 4), s -> doc(s)));
        // M3-42: TrGeVeTr escribe iTipIDTrans solo con iNatTrans = 2 — aviso: el dato no llega al XML
        declare(rule("GET019a").field("iTipIDTrans").warning().dCodRes("4321").showValue(),
                forbiddenIf(s -> nat(s) == 1, e -> e.getiTipIDTrans() == null ? null : (int) e.getiTipIDTrans().getVal()));
        declareNotEvaluated(rule("GET020a").field("dDTipIDTrans").error().dCodRes("4323").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("GET021").field("dNumIDTrans").error().dCodRes("4324").omitValue(),
                passes(s -> !mal(doc(s) != 0, s.e().getdNumIDTrans(), 1, 20), s -> s.e().getdNumIDTrans()));
        // I3-43: rGeVeTr es trGeVeTr (Evento_v150.xsd:371) y su dNumIDTrans es tdNumDocId (:284); el tdNumTra
        // de :212 es de trGeDeVTr, otro evento. Queda en ERROR.
        declare(rule("JBS-EVENTO-53").field("dNumIDTrans").error().omitValue().source(RuleSource.MT_V150),
                passes(s -> ValueChecks.validDocumentNumber(s.e().getdNumIDTrans()), s -> s.e().getdNumIDTrans()));
        declare(rule("GET022").field("iTipTrans").error().dCodRes("4325").showValue().source(RuleSource.NT_018),
                requiredIf(s -> mot(s) == 4, TrGeVeTr::getiTipTrans));
        declareNotEvaluated(rule("GET023a").field("dDesTipTrans").error().dCodRes("4327").showValue().source(RuleSource.NT_018),
                NotEvaluatedReason.DERIVED);
        declare(rule("GET024").field("iModTrans").error().dCodRes("4328").showValue().source(RuleSource.NT_018),
                requiredIf(s -> mot(s) == 4, TrGeVeTr::getiModTrans));
        declareNotEvaluated(rule("GET025a").field("dDesModTrans").error().dCodRes("4330").showValue().source(RuleSource.NT_018),
                NotEvaluatedReason.DERIVED);
        declare(rule("GET026").field("dTiVehTras").error().dCodRes("4331").showValue().source(RuleSource.NT_018),
                passes(s -> !mal(mot(s) == 4, s.e().getdTiVehTras(), 4, 10), s -> s.e().getdTiVehTras()));
        declare(rule("GET027").field("dMarVeh").error().dCodRes("4332").showValue().source(RuleSource.NT_018),
                passes(s -> !mal(mot(s) == 4, s.e().getdMarVeh(), 1, 10), s -> s.e().getdMarVeh()));
        declare(rule("GET028").field("dTipIdenVeh").error().dCodRes("4333").showValue().source(RuleSource.NT_018),
                requiredIf(s -> mot(s) == 4, TrGeVeTr::getdTipIdenVeh));
        declare(rule("GET029").field("dNroIDVeh").error().dCodRes("4334").showValue().source(RuleSource.NT_018),
                passes(s -> !mal(ident(s) == 1, s.e().getdNroIDVeh(), 1, 20), s -> s.e().getdNroIDVeh()));
        declare(rule("GET030").field("dNroMatVeh").error().dCodRes("4335").showValue().source(RuleSource.NT_005, RuleSource.NT_018),
                passes(s -> !mal(ident(s) == 2, s.e().getdNroMatVeh(), 6, 7), s -> s.e().getdNroMatVeh()));
    }

    @Override
    protected List<Subject<TrGesEve, TrGeVeTr>> subjects(TrGesEve raiz) {
        return raiz.getgGroupTiEvt() == null ? none() : unique(raiz.getgGroupTiEvt().getrGeVeTr());
    }

    /** Falta siendo obligatorio, o no tiene el largo pedido. */
    private static boolean mal(boolean obligatorio, String texto, int min, int max) {
        return (obligatorio && !present(texto)) || !ValueChecks.length(texto, min, max);
    }

    private static int mot(Subject<TrGesEve, TrGeVeTr> s) {
        return s.e().getdMotEv() == null ? 0 : s.e().getdMotEv().getVal();
    }

    private static int nat(Subject<TrGesEve, TrGeVeTr> s) {
        return s.e().getiNatTrans() == null ? 0 : s.e().getiNatTrans().getVal();
    }

    private static int doc(Subject<TrGesEve, TrGeVeTr> s) {
        return s.e().getiTipIDTrans() == null ? 0 : s.e().getiTipIDTrans().getVal();
    }

    private static int ident(Subject<TrGesEve, TrGeVeTr> s) {
        return s.e().getdTipIdenVeh() == null ? 0 : s.e().getdTipIdenVeh().getVal();
    }
}

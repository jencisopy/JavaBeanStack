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
import org.javabeanstack.sifen.core.fields.request.de.TgOpeCom;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;
import org.javabeanstack.sifen.core.types.CMondT;

/**
 * Grupo D del MT, D1: gOpeCom (transacción, impuesto, moneda, tipo de cambio; E3-01: ISC no soportado).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class CommercialOperationRules extends RuleSet<DocumentoElectronico, TgOpeCom> {

    CommercialOperationRules() {
        super("D-gOpeCom", RuleGroup.D, "gDatGralOpe.gOpeCom");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("dTiCam", s -> org.javabeanstack.sifen.core.validation.support.DEData.inGuaranies(s.root()) || org.javabeanstack.sifen.core.validation.support.DEData.dCondTiCam(s.root()) != 1); // TgOpeCom: dTiCam solo con moneda ≠ PYG y dCondTiCam = 1
        declare(rule("D010").field("").error().dCodRes("1200").showValue(),
                groupRequiredIf(s -> tipo(s) != 0 && tipo(s) != 7));
        // M3-42 (revisada, queda en error): gOpeCom no se escribe en la remisión, pero su iTipTra = 12 hace que TgCamDEAsoc escriba dNumResCF
        declare(rule("D010a").field("").error().dCodRes("1201").showValue(),
                groupForbiddenIf(s -> tipo(s) == 7));
        declare(rule("D011").field("iTipTra").error().dCodRes("1202").showValue().source(RuleSource.NT_006),
                requiredIf(s -> tipo(s) == 1 || tipo(s) == 4, TgOpeCom::getiTipTra));
        // M3-42 (revisada, queda en error): iTipTra no se escribe en NC/ND/remisión, pero con 12 TgCamDEAsoc escribe dNumResCF
        declare(rule("D011a").field("iTipTra").error().showValue().source(RuleSource.NT_006),
                forbiddenIf(s -> tipo(s) != 0 && tipo(s) != 1 && tipo(s) != 4, e -> e.getiTipTra() == null ? null : e.getiTipTra().getVal()));
        declareNotEvaluated(rule("D012").field("dDesTipTra").error().dCodRes("1203").showValue().source(RuleSource.NT_010),
                NotEvaluatedReason.DERIVED);
        declare(rule("D013").field("iTImp").error().dCodRes("1204").showValue(),
                required(TgOpeCom::getiTImp));
        declare(rule("JBS-D-03").nonDisableable().field("iTImp").error().showValue().source(RuleSource.JBS),
                passes(s -> s.e().getiTImp() == null || s.e().getiTImp().getVal() != 2, s -> s.e().getiTImp().getVal()));
        declareNotEvaluated(rule("D014").field("dDesTImp").error().dCodRes("1205").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-D-04").field("cMoneOpe").error().showValue(),
                required(TgOpeCom::getcMoneOpe));
        declare(rule("D022").field("cMoneOpe").error().dCodRes("1213").showValue().source(RuleSource.NT_012),
                passes(s -> s.e().getcMoneOpe() == null || !(s.e().getcMoneOpe() != CMondT.PYG && tipo(s) == 4), s -> s.e().getcMoneOpe()));
        declareNotEvaluated(rule("D016").field("dDesMoneOpe").error().dCodRes("1206").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("D017").field("dCondTiCam").error().dCodRes("1207").showValue(),
                requiredIf(s -> s.e().getcMoneOpe() != null && s.e().getcMoneOpe() != CMondT.PYG, TgOpeCom::getdCondTiCam));
        declare(rule("D017a").field("dCondTiCam").error().dCodRes("1208").showValue(),
                forbiddenIf(s -> s.e().getcMoneOpe() == CMondT.PYG, e -> e.getdCondTiCam() == null ? null : e.getdCondTiCam().getVal()));
        declare(rule("D018").field("dTiCam").error().dCodRes("1209").showValue(),
                requiredIf(s -> s.e().getcMoneOpe() != null && s.e().getcMoneOpe() != CMondT.PYG && condicion(s) == 1, TgOpeCom::getdTiCam));
        declare(rule("JBS-D-05").field("dTiCam").error().showValue(),
                passes(s -> s.e().getcMoneOpe() == null || (ValueChecks.decimal(s.e().getdTiCam(), 5, 4) && positive(s.e().getdTiCam())), s -> s.e().getdTiCam()));
        // M3-42: TgOpeCom escribe dTiCam solo con moneda ≠ PYG y dCondTiCam = 1 — aviso: el dato no llega al XML
        declare(rule("D018a").field("dTiCam").warning().dCodRes("1210").showValue(),
                forbiddenIf(s -> s.e().getcMoneOpe() != null && (s.e().getcMoneOpe() == CMondT.PYG || condicion(s) == 2), TgOpeCom::getdTiCam));
        declareNotEvaluated(rule("D020").field("dDesCondAnt").error().dCodRes("1211").showValue(),
                NotEvaluatedReason.DERIVED);
        // §6.1 (NT 018, D030 ocurrencias 0-11)
        declare(rule("JBS-D-62").field("gOblAfe").error().showValue().source(RuleSource.NT_018).from(java.time.LocalDate.of(2024, 1, 8)),
                passes(s -> s.e().getgOblAfeList() == null || s.e().getgOblAfeList().size() <= 11,
                        s -> s.e().getgOblAfeList() == null ? null : s.e().getgOblAfeList().size()));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgOpeCom>> subjects(DocumentoElectronico raiz) {
        return raiz.getgDatGralOpe() == null ? none() : unique(raiz.getgDatGralOpe().getgOpeCom());
    }

    private static int tipo(Subject<DocumentoElectronico, TgOpeCom> s) {
        return DEData.iTiDE(s.root());
    }

    private static int condicion(Subject<DocumentoElectronico, TgOpeCom> s) {
        return s.e().getdCondTiCam() == null ? 0 : s.e().getdCondTiCam().getVal();
    }
}

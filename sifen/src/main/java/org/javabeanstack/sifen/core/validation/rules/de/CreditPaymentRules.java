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
import org.javabeanstack.sifen.core.fields.request.de.TgPagCred;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;
import java.math.BigDecimal;
import java.util.List;
import org.javabeanstack.sifen.core.fields.request.de.TgCamCond;
import org.javabeanstack.sifen.core.fields.request.de.TgCuotas;
import org.javabeanstack.sifen.core.types.CMondT;

/**
 * Grupo E del MT, E7.2: gPagCred (crédito y cuotas a nivel lista).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class CreditPaymentRules extends RuleSet<DocumentoElectronico, TgPagCred> {

    CreditPaymentRules() {
        super("E-gPagCred", RuleGroup.E, "gDtipDE.gCamCond.gPagCred");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("", s -> (org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 0 && org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 1 && org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 4) || (org.javabeanstack.sifen.core.validation.support.DEData.iCondOpe(s.root()) != 0 && org.javabeanstack.sifen.core.validation.support.DEData.iCondOpe(s.root()) != 2)); // TgDtipDE/TgCamCond: gPagCred solo con iTiDE = 1/4 e iCondOpe = 2
        declare(rule("E640").nonDisableable().field("").error().dCodRes("1700").showValue(),
                groupRequiredIf(s -> condicionOpe(s) == 2));
        // M3-42: TgCamCond escribe gPagCred solo con iCondOpe = 2 — aviso: el dato no llega al XML
        declare(rule("E640a").field("").warning().dCodRes("1701").showValue(),
                groupForbiddenIf(s -> condicionOpe(s) != 2));
        declare(rule("JBS-E-46").field("iCondCred").error().showValue(),
                required(TgPagCred::getiCondCred));
        declareNotEvaluated(rule("E642").field("dDCondCred").error().dCodRes("1702").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("E643").field("dPlazoCre").error().dCodRes("1703").showValue(),
                requiredIf(s -> condCred(s) == 1, TgPagCred::getdPlazoCre));
        declare(rule("JBS-E-47").field("dPlazoCre").error().showValue(),
                length(TgPagCred::getdPlazoCre, 2, 15));
        declare(rule("E643a").field("dPlazoCre").error().dCodRes("1704").showValue(),
                forbiddenIf(s -> condCred(s) != 0 && condCred(s) != 1, TgPagCred::getdPlazoCre));
        declare(rule("E644").field("dCuotas").error().dCodRes("1705").showValue(),
                passes(s -> !(condCred(s) == 2 && s.e().getdCuotas() == 0)));
        declare(rule("JBS-E-48").field("dCuotas").error().showValue(),
                passes(s -> s.e().getdCuotas() >= 0 && s.e().getdCuotas() <= 999, s -> s.e().getdCuotas()));
        declare(rule("E644a").field("dCuotas").error().dCodRes("1706").showValue(),
                passes(s -> !(condCred(s) != 0 && condCred(s) != 2 && s.e().getdCuotas() != 0), s -> s.e().getdCuotas()));
        declare(rule("JBS-E-49").field("dCuotas").warning().showValue(),
                passes(s -> !(s.e().getdCuotas() > 0 && present(s.e().getgCuotasList()) && s.e().getgCuotasList().size() != s.e().getdCuotas())));
        declare(rule("JBS-E-50").field("dMonEnt").error().showValue(),
                decimal(TgPagCred::getdMonEnt, 15, 4, true));
        declare(rule("E650").field("gCuotas[]").error().dCodRes("1750").showValue(),
                requiredIf(s -> condCred(s) == 2, TgPagCred::getgCuotasList));
        declare(rule("E650a").field("gCuotas[]").error().dCodRes("1751").showValue(),
                forbiddenIf(s -> condCred(s) != 0 && condCred(s) != 2, TgPagCred::getgCuotasList));
        declare(rule("JBS-E-51").field("gCuotas[]").warning().showValue(),
                passes(s -> cuotasCoinciden(s.root(), s.e())));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgPagCred>> subjects(DocumentoElectronico raiz) {
        TgCamCond c = DEData.gCamCond(raiz);
        return c == null || c.getiCondOpe() == null ? none() : unique(c.getgPagCred());
    }

    private static int condicionOpe(Subject<DocumentoElectronico, TgPagCred> s) {
        return DEData.iCondOpe(s.root());
    }

    private static int condCred(Subject<DocumentoElectronico, TgPagCred> s) {
        return s.e().getiCondCred() == null ? 0 : s.e().getiCondCred().getVal();
    }

    /** JBS-E-51 (aviso): Σ cuotas contra el saldo a financiar (total − entrega), si todas están en la moneda del DE. */
    private static boolean cuotasCoinciden(DocumentoElectronico de, TgPagCred cred) {
        List<TgCuotas> cuotas = cred.getgCuotasList();
        if (cuotas == null || cuotas.isEmpty()) {
            return true;
        }
        BigDecimal suma = BigDecimal.ZERO;
        for (TgCuotas q : cuotas) {
            CMondT moneda = q == null ? null : q.getcMoneCuo();
            BigDecimal monto = q == null ? null : q.getdMonCuota();
            if (monto == null || moneda == null || moneda != DEData.cMoneOpe(de)) {
                return true;
            }
            suma = suma.add(monto);
        }
        BigDecimal total = DEData.operationTotal(de);
        if (total == null) {
            return true;
        }
        BigDecimal saldo = total.subtract(cred.getdMonEnt() == null ? BigDecimal.ZERO : cred.getdMonEnt());
        return suma.subtract(saldo).abs().compareTo(DEData.roundingTolerance(de)) <= 0;
    }
}

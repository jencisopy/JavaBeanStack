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
import org.javabeanstack.sifen.core.fields.request.de.TgCamCond;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;
import java.math.BigDecimal;
import org.javabeanstack.sifen.core.fields.request.de.TgCamItem;
import org.javabeanstack.sifen.core.fields.request.de.TgPaConEIni;
import org.javabeanstack.sifen.core.fields.request.de.TgPagCred;
import org.javabeanstack.sifen.core.types.CMondT;

/**
 * Grupo E del MT, E7: gCamCond (condición de la operación, formas de pago a nivel lista; E3-02).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class OperationConditionRules extends RuleSet<DocumentoElectronico, TgCamCond> {

    OperationConditionRules() {
        super("E-gCamCond", RuleGroup.E, "gDtipDE.gCamCond");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("", s -> org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 0 && org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 1 && org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 4); // TgDtipDE: gCamCond solo con iTiDE = 1/4
        declare(rule("E600").field("").error().dCodRes("1500").showValue(),
                groupRequiredIf(s -> tipo(s) == 1 || tipo(s) == 4));
        // M3-42: TgDtipDE escribe gCamCond solo con iTiDE = 1/4 — aviso: el dato no llega al XML
        declare(rule("E600a").field("").warning().dCodRes("1501").showValue(),
                groupForbiddenIf(s -> tipo(s) != 0 && tipo(s) != 1 && tipo(s) != 4));
        declare(rule("JBS-E-29").field("iCondOpe").error().showValue(),
                required(TgCamCond::getiCondOpe));
        declare(rule("E601").field("iCondOpe").error().dCodRes("1503").showValue(),
                passes(s -> condicion(s) == 0 || !(tipo(s) == 4 && condicion(s) != 1), s -> condicion(s)));
        declareNotEvaluated(rule("E602").field("dDCondOpe").error().dCodRes("1502").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("E605").nonDisableable().field("gPaConEIni[]").error().dCodRes("1550").showValue(),
                requiredIf(s -> condicion(s) == 1, TgCamCond::getgPaConEIniList));
        declare(rule("E605a").field("gPaConEIni[]").error().dCodRes("1551").showValue(),
                requiredIf(s -> condicion(s) == 2 && conEntrega(s), TgCamCond::getgPaConEIniList));
        declare(rule("E605b").nonDisableable().field("gPaConEIni[]").error().dCodRes("1552").showValue(),
                forbiddenIf(s -> condicion(s) == 2 && !conEntrega(s), TgCamCond::getgPaConEIniList));
        declare(rule("JBS-E-30").field("gPaConEIni[]").warning().showValue(),
                passes(s -> condicion(s) == 0 || s.e().getgPaConEIniList() == null || sumaPagosCoincide(s.root(), s.e().getgPaConEIniList(), condicion(s), s.e().getgPagCred())));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgCamCond>> subjects(DocumentoElectronico raiz) {
        return raiz.getgDtipDE() == null ? none() : unique(raiz.getgDtipDE().getgCamCond());
    }

    private static int tipo(Subject<DocumentoElectronico, TgCamCond> s) {
        return DEData.iTiDE(s.root());
    }

    /** Condición de la operación (1 contado, 2 crédito); 0 si falta (sin ella no se evalúa el resto, E3-02). */
    private static int condicion(Subject<DocumentoElectronico, TgCamCond> s) {
        return s.e().getiCondOpe() == null ? 0 : s.e().getiCondOpe().getVal();
    }

    private static boolean conEntrega(Subject<DocumentoElectronico, TgCamCond> s) {
        return s.e().getgPagCred() != null && s.e().getgPagCred().getdMonEnt() != null;
    }

    /** JBS-E-30 (aviso): Σ formas de pago, en guaraníes, contra el total (contado) o la entrega (crédito). */
    private static boolean sumaPagosCoincide(DocumentoElectronico de, List<TgPaConEIni> pagos, int condicion, TgPagCred cred) {
        BigDecimal objetivo;
        if (condicion == 1) {
            objetivo = DEData.totalInGuaranies(de);
        } else if (condicion == 2 && cred != null && cred.getdMonEnt() != null) {
            objetivo = aGuaranies(de, cred.getdMonEnt());
        } else {
            return true;
        }
        BigDecimal suma = BigDecimal.ZERO;
        for (TgPaConEIni p : pagos) {
            if (p == null || p.getdMonTiPag() == null || p.getcMoneTiPag() == null) {
                return true;
            }
            if (p.getcMoneTiPag() == CMondT.PYG) {
                suma = suma.add(p.getdMonTiPag());
            } else if (p.getdTiCamTiPag() != null) {
                suma = suma.add(p.getdMonTiPag().multiply(p.getdTiCamTiPag()));
            } else {
                return true;
            }
        }
        if (objetivo == null) {
            return true;
        }
        return suma.subtract(objetivo).abs().compareTo(toleranciaGuaranies(de)) <= 0;
    }

    private static BigDecimal aGuaranies(DocumentoElectronico de, BigDecimal monto) {
        if (DEData.inGuaranies(de)) {
            return monto;
        }
        BigDecimal cambio = DEData.gOpeCom(de) == null ? null : DEData.gOpeCom(de).getdTiCam();
        return cambio == null ? null : monto.multiply(cambio);
    }

    /** Tolerancia en guaraníes: 50 Gs, o 0,50 de la moneda × el tipo de cambio global (o 1 si no hay). */
    private static BigDecimal toleranciaGuaranies(DocumentoElectronico de) {
        if (DEData.inGuaranies(de)) {
            return new BigDecimal("50");
        }
        BigDecimal cambio = DEData.gOpeCom(de) == null ? null : DEData.gOpeCom(de).getdTiCam();
        BigDecimal maxCambio = cambio == null ? BigDecimal.ONE : cambio;
        if (cambio == null) {
            for (TgCamItem it : DEData.items(de)) {
                if (it != null && it.getgValorItem() != null && it.getgValorItem().getdTiCamIt() != null
                        && it.getgValorItem().getdTiCamIt().compareTo(maxCambio) > 0) {
                    maxCambio = it.getgValorItem().getdTiCamIt();
                }
            }
        }
        return new BigDecimal("0.5").multiply(maxCambio).add(BigDecimal.ONE);
    }
}

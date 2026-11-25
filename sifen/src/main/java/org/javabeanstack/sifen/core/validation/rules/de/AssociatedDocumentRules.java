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
import org.javabeanstack.sifen.core.fields.request.de.TgCamDEAsoc;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;

/**
 * Grupo H del MT: cada gCamDEAsoc (documento asociado).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class AssociatedDocumentRules extends RuleSet<DocumentoElectronico, TgCamDEAsoc> {

    AssociatedDocumentRules() {
        super("H-gCamDEAsoc", RuleGroup.H, "gCamDEAsoc[]");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("dCdCDERef", s -> (s.e().getiTipDocAso() == null ? 0 : s.e().getiTipDocAso().getVal()) != 1); // TgCamDEAsoc: dCdCDERef solo con iTipDocAso = 1
        notWrittenIf("dNTimDI", s -> (s.e().getiTipDocAso() == null ? 0 : s.e().getiTipDocAso().getVal()) != 2); // TgCamDEAsoc: datos del impreso solo con iTipDocAso = 2
        notWrittenIf("dEstDocAso", s -> (s.e().getiTipDocAso() == null ? 0 : s.e().getiTipDocAso().getVal()) != 2); // TgCamDEAsoc: datos del impreso solo con iTipDocAso = 2
        notWrittenIf("dPExpDocAso", s -> (s.e().getiTipDocAso() == null ? 0 : s.e().getiTipDocAso().getVal()) != 2); // TgCamDEAsoc: datos del impreso solo con iTipDocAso = 2
        notWrittenIf("dNumDocAso", s -> (s.e().getiTipDocAso() == null ? 0 : s.e().getiTipDocAso().getVal()) != 2); // TgCamDEAsoc: datos del impreso solo con iTipDocAso = 2
        notWrittenIf("iTipoDocAso", s -> (s.e().getiTipDocAso() == null ? 0 : s.e().getiTipDocAso().getVal()) != 2); // TgCamDEAsoc: datos del impreso solo con iTipDocAso = 2
        notWrittenIf("dFecEmiDI", s -> (s.e().getiTipDocAso() == null ? 0 : s.e().getiTipDocAso().getVal()) != 2); // TgCamDEAsoc: datos del impreso solo con iTipDocAso = 2
        notWrittenIf("iTipCons", s -> (s.e().getiTipDocAso() == null ? 0 : s.e().getiTipDocAso().getVal()) != 3); // TgCamDEAsoc: iTipCons solo con iTipDocAso = 3
        notWrittenIf("dNumCons", s -> !((s.e().getiTipDocAso() == null ? 0 : s.e().getiTipDocAso().getVal()) == 3 && (s.e().getiTipCons() == null ? 0 : s.e().getiTipCons().getVal()) == 2)); // TgCamDEAsoc: dNumCons/dNumControl solo con iTipDocAso = 3 e iTipCons = 2
        notWrittenIf("dNumControl", s -> !((s.e().getiTipDocAso() == null ? 0 : s.e().getiTipDocAso().getVal()) == 3 && (s.e().getiTipCons() == null ? 0 : s.e().getiTipCons().getVal()) == 2)); // TgCamDEAsoc: dNumCons/dNumControl solo con iTipDocAso = 3 e iTipCons = 2
        notWrittenIf("dNumResCF", s -> org.javabeanstack.sifen.core.validation.support.DEData.iTipTra(s.root()) != 12); // TgCamDEAsoc: dNumResCF solo con iTipTra = 12
        notWrittenIf("dNumComRet", s -> !((org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) == 1 || org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) == 4) && org.javabeanstack.sifen.core.validation.support.DEData.hasPayment(s.root(), 10))); // TgCamDEAsoc: dNumComRet solo con un pago por retención en FE/AFE
        declare(rule("H001a").field("").error().dCodRes("2414").showValue(),
                passes(s -> tipoAdmitido(tipo(s), t(s), s.e())));
        declare(rule("H002").field("iTipDocAso").error().dCodRes("2416").showValue(),
                passes(s -> !(tipo(s) == 4 && t(s) != 0 && t(s) != 3), s -> t(s)));
        declare(rule("H002a").field("iTipDocAso").error().dCodRes("2434").showValue(),
                passes(s -> !(t(s) == 3 && (tipo(s) == 1 || tipo(s) == 5 || tipo(s) == 6 || tipo(s) == 7)), s -> t(s)));
        declare(rule("JBS-H-01").field("iTipDocAso").error().showValue(),
                onNull(s -> s.e() != null && present(s.e().getiTipDocAso()), s -> null));
        declareNotEvaluated(rule("H003").field("dDesTipDocAso").error().dCodRes("2401").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("H004").field("dCdCDERef").error().dCodRes("2402").showValue(),
                required(TgCamDEAsoc::getdCdCDERef)); // M3-59: el caso lo excluye notWrittenIf
        declare(rule("JBS-H-02").field("dCdCDERef").error().showValue(),
                passes(s -> !present(s.e().getdCdCDERef()) || validCdc(s.e().getdCdCDERef())));
        // M3-42: TgCamDEAsoc escribe dCdCDERef solo con iTipDocAso = 1 — aviso: el dato no llega al XML
        declare(rule("H004c").field("dCdCDERef").warning().dCodRes("2418").showValue(),
                forbiddenIf(s -> t(s) == 2 || t(s) == 3, TgCamDEAsoc::getdCdCDERef));
        declare(rule("H005").field("dNTimDI").error().dCodRes("2405").showValue(),
                required(TgCamDEAsoc::getdNTimDI)); // M3-59: el caso lo excluye notWrittenIf
        declare(rule("JBS-H-03").field("dNTimDI").error().showValue(),
                passes(s -> digits(s.e().getdNTimDI(), 8, 8), s -> s.e().getdNTimDI()));
        // M3-42: TgCamDEAsoc escribe los datos del impreso solo con iTipDocAso = 2 — aviso: el dato no llega al XML
        declare(rule("H005a").field("dNTimDI").warning().dCodRes("2419").showValue(),
                forbiddenIf(s -> noImpreso(s), TgCamDEAsoc::getdNTimDI));
        declare(rule("H006").field("dEstDocAso").error().dCodRes("2406").showValue(),
                required(TgCamDEAsoc::getdEstDocAso)); // M3-59: el caso lo excluye notWrittenIf
        declare(rule("JBS-H-04").field("dEstDocAso").error().showValue(),
                passes(s -> digits(s.e().getdEstDocAso(), 3, 3), s -> s.e().getdEstDocAso()));
        // M3-42: ídem H005a — aviso: el dato no llega al XML
        declare(rule("H006a").field("dEstDocAso").warning().dCodRes("2420").showValue(),
                forbiddenIf(s -> noImpreso(s), TgCamDEAsoc::getdEstDocAso));
        declare(rule("H007").field("dPExpDocAso").error().dCodRes("2407").showValue(),
                required(TgCamDEAsoc::getdPExpDocAso)); // M3-59: el caso lo excluye notWrittenIf
        declare(rule("JBS-H-05").field("dPExpDocAso").error().showValue(),
                passes(s -> digits(s.e().getdPExpDocAso(), 3, 3), s -> s.e().getdPExpDocAso()));
        // M3-42: ídem H005a — aviso: el dato no llega al XML
        declare(rule("H007a").field("dPExpDocAso").warning().dCodRes("2421").showValue(),
                forbiddenIf(s -> noImpreso(s), TgCamDEAsoc::getdPExpDocAso));
        declare(rule("H008").field("dNumDocAso").error().dCodRes("2408").showValue(),
                required(TgCamDEAsoc::getdNumDocAso)); // M3-59: el caso lo excluye notWrittenIf
        declare(rule("JBS-H-06").field("dNumDocAso").error().showValue(),
                passes(s -> digits(s.e().getdNumDocAso(), 7, 7), s -> s.e().getdNumDocAso()));
        // M3-42: ídem H005a — aviso: el dato no llega al XML
        declare(rule("H008a").field("dNumDocAso").warning().dCodRes("2422").showValue(),
                forbiddenIf(s -> noImpreso(s), TgCamDEAsoc::getdNumDocAso));
        declare(rule("H009").field("iTipoDocAso").error().dCodRes("2409").showValue(),
                required(TgCamDEAsoc::getiTipoDocAso)); // M3-59: el caso lo excluye notWrittenIf
        // M3-42: ídem H005a — aviso: el dato no llega al XML
        declare(rule("H009a").field("iTipoDocAso").warning().dCodRes("2423").showValue(),
                forbiddenIf(s -> noImpreso(s), e -> e.getiTipoDocAso() == null ? null : e.getiTipoDocAso().getVal()));
        declareNotEvaluated(rule("H010").field("dDTipoDocAso").error().dCodRes("2410").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("H011").field("dFecEmiDI").error().dCodRes("2411").showValue(),
                required(TgCamDEAsoc::getdFecEmiDI)); // M3-59: el caso lo excluye notWrittenIf
        // M3-42: ídem H005a — aviso: el dato no llega al XML
        declare(rule("H011a").field("dFecEmiDI").warning().dCodRes("2425").showValue(),
                forbiddenIf(s -> noImpreso(s), TgCamDEAsoc::getdFecEmiDI));
        // E3-28: MT H012 «A 15» y DE_v150.xsd:1589 (xs:length 15): 15 caracteres exactos
        declare(rule("JBS-H-07").field("dNumComRet").error().dCodRes("2412").showValue(),
                passes(s -> present(s.e().getdNumComRet()) && ValueChecks.length(s.e().getdNumComRet(), 15, 15))); // M3-59: el caso lo excluye notWrittenIf (sin retención)
        // M3-42: TgCamDEAsoc escribe dNumComRet solo con un pago por retención (iTiPago 10) — aviso: el dato no llega al XML
        declare(rule("H012a").field("dNumComRet").warning().dCodRes("2436").showValue(),
                forbiddenIf(s -> !retencion(s), TgCamDEAsoc::getdNumComRet));
        declare(rule("H013").field("dNumResCF").error().dCodRes("2413").showValue(),
                required(TgCamDEAsoc::getdNumResCF)); // M3-59: el caso lo excluye notWrittenIf
        // E3-28: MT H013 «A 15» y DE_v150.xsd:1602 (xs:length 15): 15 caracteres exactos
        declare(rule("JBS-H-08").field("dNumResCF").error().showValue(),
                length(TgCamDEAsoc::getdNumResCF, 15, 15));
        // M3-42: TgCamDEAsoc escribe dNumResCF solo con iTipTra = 12 — aviso: el dato no llega al XML
        declare(rule("JBS-H-09").field("dNumResCF").warning().showValue(),
                forbiddenIf(s -> DEData.iTipTra(s.root()) != 0 && DEData.iTipTra(s.root()) != 12, TgCamDEAsoc::getdNumResCF));
        declare(rule("H014").field("iTipCons").error().dCodRes("2426").showValue(),
                required(TgCamDEAsoc::getiTipCons)); // M3-59: el caso lo excluye notWrittenIf
        // M3-42: TgCamDEAsoc escribe iTipCons solo con iTipDocAso = 3 — aviso: el dato no llega al XML
        declare(rule("H014a").field("iTipCons").warning().dCodRes("2427").showValue(),
                forbiddenIf(s -> t(s) == 1 || t(s) == 2, e -> e.getiTipCons() == null ? null : e.getiTipCons().getVal()));
        declareNotEvaluated(rule("H015a").field("dDesTipCons").error().dCodRes("2429").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("H016").field("dNumCons").error().dCodRes("2430").showValue(),
                required(TgCamDEAsoc::getdNumCons)); // M3-59: el caso lo excluye notWrittenIf
        declare(rule("JBS-H-10").field("dNumCons").error().showValue(),
                passes(s -> s.e().getdNumCons() == null || (s.e().getdNumCons().signum() >= 0 && s.e().getdNumCons().toString().length() == 11),
                s -> s.e().getdNumCons()));
        // M3-42: TgCamDEAsoc escribe dNumCons solo con iTipDocAso = 3 e iTipCons = 2 — aviso: el dato no llega al XML
        declare(rule("H016a").field("dNumCons").warning().dCodRes("2431").showValue(),
                forbiddenIf(s -> t(s) == 1 || t(s) == 2, TgCamDEAsoc::getdNumCons));
        declare(rule("H017").field("dNumControl").error().dCodRes("2432").showValue(),
                required(TgCamDEAsoc::getdNumControl)); // M3-59: el caso lo excluye notWrittenIf
        declare(rule("JBS-H-11").field("dNumControl").error().showValue(),
                length(TgCamDEAsoc::getdNumControl, 8, 8));
        // M3-42: ídem H016a — aviso: el dato no llega al XML
        declare(rule("H017a").field("dNumControl").warning().dCodRes("2433").showValue(),
                forbiddenIf(s -> t(s) == 1 || t(s) == 2, TgCamDEAsoc::getdNumControl));
        // §6.2 (NT 023, H018): RUC fusionado del emisor del CDC asociado
        declare(rule("JBS-H-12").field("dRucFus").error().showValue().source(RuleSource.NT_023).from(java.time.LocalDate.of(2024, 9, 27)),
                passes(s -> !present(s.e().getdRucFus()) || s.e().getdRucFus().matches("\\d{3,8}"), s -> s.e().getdRucFus()));
        declare(rule("H018").field("dRucFus").error().dCodRes("2443").showValue().source(RuleSource.NT_023).from(java.time.LocalDate.of(2024, 9, 27)),
                passes(s -> !present(s.e().getdRucFus()) || !cdc44(s.e().getdCdCDERef())
                        || sinCeros(s.e().getdCdCDERef().substring(2, 10)).equals(sinCeros(s.e().getdRucFus())), s -> s.e().getdRucFus()));
        declare(rule("H018a").field("dRucFus").error().dCodRes("2444").showValue().source(RuleSource.NT_023).from(java.time.LocalDate.of(2024, 9, 27)),
                passes(s -> !present(s.e().getdRucFus()) || tipo(s) == 5 || tipo(s) == 6 || tipo(s) == 7, s -> s.e().getdRucFus()));
    }

    /** CDC de 44 dígitos (el RUC del emisor son sus posiciones 3 a 10). */
    private static boolean cdc44(String cdc) {
        return cdc != null && cdc.matches("\\d{44}");
    }

    /** El número sin los ceros a la izquierda. */
    private static String sinCeros(String n) {
        String s = n.replaceFirst("^0+", "");
        return s.isEmpty() ? "0" : s;
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgCamDEAsoc>> subjects(DocumentoElectronico raiz) {
        return perItem(raiz.getgCamDEAsocList());
    }

    private static int tipo(Subject<DocumentoElectronico, TgCamDEAsoc> s) {
        return DEData.iTiDE(s.root());
    }

    /** Tipo de documento asociado (1 electrónico, 2 impreso, 3 constancia); 0 si falta. */
    private static int t(Subject<DocumentoElectronico, TgCamDEAsoc> s) {
        return s.e().getiTipDocAso() == null ? 0 : s.e().getiTipDocAso().getVal();
    }

    private static boolean noImpreso(Subject<DocumentoElectronico, TgCamDEAsoc> s) {
        return t(s) == 1 || t(s) == 3;
    }

    private static boolean retencion(Subject<DocumentoElectronico, TgCamDEAsoc> s) {
        return DEData.hasPayment(s.root(), 10);
    }


    /**
     * H001a: NC, ND y NR se asocian a una factura (CDC que empieza con {@code 01} o impreso de tipo 1);
     * una factura, a una nota de remisión ({@code 07} / 4) o a una factura de anticipo ({@code 01} / 1).
     */
    private static boolean tipoAdmitido(int tipo, int t, TgCamDEAsoc a) {
        if (t == 1 && a.getdCdCDERef() != null && a.getdCdCDERef().length() >= 2) {
            String prefijo = a.getdCdCDERef().substring(0, 2);
            if (tipo == 5 || tipo == 6 || tipo == 7) {
                return "01".equals(prefijo);
            }
            if (tipo == 1) {
                return "01".equals(prefijo) || "07".equals(prefijo);
            }
        }
        if (t == 2 && a.getiTipoDocAso() != null) {
            int impreso = a.getiTipoDocAso().getVal();
            if (tipo == 5 || tipo == 6 || tipo == 7) {
                return impreso == 1;
            }
            if (tipo == 1) {
                return impreso == 1 || impreso == 4;
            }
        }
        return true;
    }
}

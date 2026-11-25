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
import org.javabeanstack.sifen.core.fields.request.de.TgDatRec;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;
import java.math.BigDecimal;
import org.javabeanstack.sifen.core.fields.request.de.TgEmis;
import org.javabeanstack.sifen.core.types.PaisType;

/**
 * Grupo D del MT, D3: gDatRec (receptor; D202 como «no contribuyente ⇒ B2C o B2F», I3-16).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class ReceiverRules extends RuleSet<DocumentoElectronico, TgDatRec> {

    ReceiverRules() {
        super("D-gDatRec", RuleGroup.D, "gDatGralOpe.gDatRec");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("iTiContRec", s -> (s.e().getiNatRec() == null ? 0 : s.e().getiNatRec().getVal()) != 1); // TgDatRec: solo con iNatRec = 1
        notWrittenIf("dRucRec", s -> (s.e().getiNatRec() == null ? 0 : s.e().getiNatRec().getVal()) != 1); // TgDatRec: solo con iNatRec = 1
        notWrittenIf("dDVRec", s -> (s.e().getiNatRec() == null ? 0 : s.e().getiNatRec().getVal()) != 1); // TgDatRec: solo con iNatRec = 1
        notWrittenIf("iTipIDRec", s -> (s.e().getiNatRec() == null ? 0 : s.e().getiNatRec().getVal()) != 2); // TgDatRec: solo con iNatRec = 2
        notWrittenIf("dDTipIDRec", s -> (s.e().getiNatRec() == null ? 0 : s.e().getiNatRec().getVal()) != 2); // TgDatRec: solo con iNatRec = 2
        notWrittenIf("dNumIDRec", s -> (s.e().getiNatRec() == null ? 0 : s.e().getiNatRec().getVal()) != 2); // TgDatRec: solo con iNatRec = 2
        notWrittenIf("cDepRec", s -> (s.e().getiTiOpe() == null ? 0 : s.e().getiTiOpe().getVal()) == 4 || (s.e().getdDirRec() == null && org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 7)); // TgDatRec: departamento y ciudad solo con dirección (o iTiDE 7) y fuera de B2F
        notWrittenIf("cCiuRec", s -> (s.e().getiTiOpe() == null ? 0 : s.e().getiTiOpe().getVal()) == 4 || (s.e().getdDirRec() == null && org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 7)); // TgDatRec: departamento y ciudad solo con dirección (o iTiDE 7) y fuera de B2F
        notWrittenIf("dDesCiuRec", s -> (s.e().getiTiOpe() == null ? 0 : s.e().getiTiOpe().getVal()) == 4 || (s.e().getdDirRec() == null && org.javabeanstack.sifen.core.validation.support.DEData.iTiDE(s.root()) != 7)); // TgDatRec: departamento y ciudad solo con dirección (o iTiDE 7) y fuera de B2F
        notWrittenIf("dDesDisRec", s -> s.e().getcDisRec() == 0); // TgDatRec: distrito solo con cDisRec ≠ 0
        declare(rule("JBS-D-39").field("").error().showValue(),
                groupRequiredIf(s -> true));
        declare(rule("JBS-D-40").field("iNatRec").error().showValue(),
                required(TgDatRec::getiNatRec));
        declare(rule("D201").field("iNatRec").error().dCodRes("1315").showValue(),
                passes(s -> s.e().getiNatRec() == null || !(tipo(s) == 4 && nat(s) != 1), s -> nat(s)));
        declare(rule("JBS-D-41").field("iTiOpe").error().showValue(),
                required(TgDatRec::getiTiOpe));
        declare(rule("D202").field("iTiOpe").error().dCodRes("1300").showValue(),
                passes(s -> s.e().getiTiOpe() == null || nat(s) == 0
                || !((nat(s) == 2 && tipo(s) != 4 && ope(s) != 2 && ope(s) != 4) || (ope(s) == 4 && nat(s) != 2)), s -> ope(s)));
        declare(rule("D202a").field("iTiOpe").error().dCodRes("1316").showValue(),
                passes(s -> s.e().getiTiOpe() == null || nat(s) == 0 || !(tipo(s) == 4 && ope(s) != 2), s -> ope(s)));
        declare(rule("JBS-D-42").field("cPaisRec").error().showValue(),
                required(TgDatRec::getcPaisRec));
        declare(rule("D203").field("cPaisRec").error().dCodRes("1320").showValue(),
                passes(s -> s.e().getcPaisRec() == null || ope(s) == 0
                || !((ope(s) == 4 && s.e().getcPaisRec() == PaisType.PRY) || (ope(s) != 4 && s.e().getcPaisRec() != PaisType.PRY)), s -> s.e().getcPaisRec()));
        declareNotEvaluated(rule("D204").field("dDesPaisRe").error().dCodRes("1301").showValue().source(RuleSource.NT_005),
                NotEvaluatedReason.DERIVED);
        declare(rule("D205").field("iTiContRec").error().dCodRes("1302").showValue(),
                required(TgDatRec::getiTiContRec)); // M3-59: iNatRec ≠ 1 lo excluye notWrittenIf
        // I3-37: la librería no escribe iTiContRec de un no contribuyente (solo iNatRec = 1): aviso
        declare(rule("D205a").field("iTiContRec").warning().dCodRes("1303").showValue(),
                forbiddenIf(s -> nat(s) == 2, e -> e.getiTiContRec() == null ? null : e.getiTiContRec().getVal()));
        declare(rule("D206").field("dRucRec").error().dCodRes("1304").omitValue(),
                required(TgDatRec::getdRucRec)); // M3-59: iNatRec ≠ 1 lo excluye notWrittenIf
        declare(rule("JBS-D-43").field("dRucRec").error().omitValue(),
                passes(s -> s.e().getdRucRec() == null || (ValueChecks.length(s.e().getdRucRec(), 3, 8) && !s.e().getdRucRec().contains("-")), s -> s.e().getdRucRec()));
        // I3-37: la librería no escribe el RUC de un no contribuyente (solo iNatRec = 1): aviso, no rechazo
        declare(rule("D206a").field("dRucRec").warning().dCodRes("1305").omitValue(),
                forbiddenIf(s -> nat(s) == 2, TgDatRec::getdRucRec));
        declare(rule("D206e").field("dRucRec").error().dCodRes("1317").omitValue(),
                passes(s -> !(tipo(s) == 4 && rucDistinto(s))));
        declare(rule("E501").field("dRucRec").error().dCodRes("2606").omitValue(),
                passes(s -> !(tipo(s) == 7 && DEData.iMotEmiNR(s.root()) == 7 && rucDistinto(s))));
        declare(rule("D207").field("dDVRec").error().dCodRes("1309").omitValue(),
                passes(s -> !present(s.e().getdRucRec()) || validDV(s.e().getdRucRec(), String.valueOf(s.e().getdDVRec()))));
        // M3-38: D208 (1310) fuera de B2F y D208g (1335) en B2F (o sin iTiOpe): un solo error por el mismo hecho
        declare(rule("D208").field("iTipIDRec").error().dCodRes("1310").showValue().source(RuleSource.NT_023),
                requiredIf(s -> ope(s) != 0 && ope(s) != 4, TgDatRec::getiTipIDRec)); // M3-59: iNatRec ≠ 2 lo excluye notWrittenIf
        // D208g (NT 023, validación 1335; Decreto 872/2023, art. 6°, num. 1): la tabla de campos exime a
        // B2F (D208), pero la validación exige el tipo de documento a todo no contribuyente (I3-31).
        // Decisión del usuario (2026-11-24): ERROR, rechazo local también en B2F; la prueba en vivo en
        // SIFEN-test la confirma. D210 (número) sigue opcional en B2F.
        declare(rule("D208g").field("iTipIDRec").error().dCodRes("1335").showValue().source(RuleSource.NT_023),
                requiredIf(s -> ope(s) == 0 || ope(s) == 4, TgDatRec::getiTipIDRec)); // M3-59: iNatRec ≠ 2 lo excluye notWrittenIf
        // I3-37: la librería no escribe iTipIDRec de un contribuyente (solo iNatRec = 2): aviso, no rechazo
        declare(rule("D208a").field("iTipIDRec").warning().dCodRes("1311").showValue().source(RuleSource.NT_023),
                forbiddenIf(s -> nat(s) == 1, e -> e.getiTipIDRec() == null ? null : (int) e.getiTipIDRec().getVal()));
        declare(rule("D208f").field("iTipIDRec").error().dCodRes("1333").showValue().source(RuleSource.NT_023),
                passes(s -> doc(s) != 5 || ope(s) == 0 || ope(s) == 2, s -> doc(s)));
        declare(rule("D208e").field("iTipIDRec").error().dCodRes("1331").showValue().source(RuleSource.NT_023),
                passes(s -> doc(s) != 5 || !(tipo(s) == 5 || tipo(s) == 6 || tipo(s) == 7), s -> doc(s)));
        declare(rule("D208c").field("iTipIDRec").error().dCodRes("1321").showValue().source(RuleSource.NT_021, RuleSource.NT_024),
                passes(s -> doc(s) != 5 || !superaUmbral(s)));
        declareNotEvaluated(rule("D209").field("dDTipIDRec").error().dCodRes("1312").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("D209a").field("dDTipIDRec").error().dCodRes("1313").showValue().source(RuleSource.NT_002, RuleSource.NT_023),
                passes(s -> doc(s) != 9 || (present(s.e().getdDTipIDRec()) && ValueChecks.length(s.e().getdDTipIDRec(), 9, 41)), s -> s.e().getdDTipIDRec()));
        declare(rule("D210").field("dNumIDRec").error().dCodRes("1314").omitValue().source(RuleSource.NT_002, RuleSource.NT_023),
                requiredIf(s -> ope(s) != 0 && ope(s) != 4 && doc(s) != 5, TgDatRec::getdNumIDRec)); // M3-59: iNatRec ≠ 2 lo excluye notWrittenIf
        // E3-26: el XSD (tdNumDocId) solo admite letras, dígitos y guion; con puntos SIFEN da 0160 (001-001-0001013)
        declare(rule("JBS-D-60").field("dNumIDRec").error().omitValue().source(RuleSource.MT_V150),
                passes(s -> ValueChecks.validDocumentNumber(s.e().getdNumIDRec()), s -> s.e().getdNumIDRec()));
        declare(rule("JBS-D-44").field("dNumIDRec").error().omitValue().source(RuleSource.NT_002),
                passes(s -> s.e().getdNumIDRec() == null
                || (ValueChecks.length(s.e().getdNumIDRec(), 1, 20) && !(doc(s) == 5 && !"0".equals(s.e().getdNumIDRec()))), s -> s.e().getdNumIDRec()));
        // I3-37: la librería no escribe dNumIDRec de un contribuyente; SIFEN-test aprobó (001-001-0001011)
        declare(rule("D210b").field("dNumIDRec").warning().dCodRes("1334").omitValue().source(RuleSource.NT_023),
                forbiddenIf(s -> nat(s) == 1, TgDatRec::getdNumIDRec));
        declare(rule("JBS-D-45").field("dNomRec").error().omitValue(),
                requiredIf(s -> doc(s) != 5, TgDatRec::getdNomRec));
        declare(rule("JBS-D-46").field("dNomRec").error().lengthOnly(),
                passes(s -> s.e().getdNomRec() == null
                || (ValueChecks.length(s.e().getdNomRec(), 4, 255) && !(doc(s) == 5 && !"Sin Nombre".equals(s.e().getdNomRec()))), s -> s.e().getdNomRec()));
        declare(rule("JBS-D-47").field("dNomFanRec").error().lengthOnly(),
                length(TgDatRec::getdNomFanRec, 4, 255));
        declare(rule("D213").field("dDirRec").error().dCodRes("1318").omitValue(),
                requiredIf(s -> tipo(s) == 7 || ope(s) == 4, TgDatRec::getdDirRec));
        declare(rule("JBS-D-48").field("dDirRec").error().lengthOnly(),
                length(TgDatRec::getdDirRec, 1, 255));
        declare(rule("D218").field("dNumCasRec").error().dCodRes("1330").omitValue(),
                passes(s -> !present(s.e().getdDirRec()) || maxDigits(s.e().getdNumCasRec(), 6), s -> s.e().getdNumCasRec()));
        declare(rule("JBS-D-49").field("cDepRec").error().showValue().source(RuleSource.NT_003),
                requiredIf(s -> present(s.e().getdDirRec()) && ope(s) != 0, TgDatRec::getcDepRec)); // M3-59: B2F lo excluye notWrittenIf
        // I3-42: TgDatRec escribe cDepRec solo con iTiOpe ≠ 4 — aviso: el dato no llega al XML
        declare(rule("JBS-D-50").field("cDepRec").warning().showValue().source(RuleSource.NT_003),
                forbiddenIf(s -> ope(s) == 4, TgDatRec::getcDepRec));
        declareNotEvaluated(rule("D220").field("dDesDepRec").error().dCodRes("1325").showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-D-51").field("cDisRec").error().showValue(),
                passes(s -> maxDigits(s.e().getcDisRec(), 4), s -> s.e().getcDisRec()));
        declare(rule("JBS-D-52").field("dDesDisRec").error().showValue().source(RuleSource.NT_017),
                passes(s -> present(s.e().getdDesDisRec()) && ValueChecks.length(s.e().getdDesDisRec(), 1, 30))); // M3-59: cDisRec = 0 lo excluye notWrittenIf
        declare(rule("JBS-D-53").field("cCiuRec").error().showValue().source(RuleSource.NT_003),
                passes(s -> !(present(s.e().getdDirRec()) && ope(s) != 0 && s.e().getcCiuRec() == 0)
                && maxDigits(s.e().getcCiuRec(), 5), s -> s.e().getcCiuRec()));
        // I3-42: TgDatRec escribe cCiuRec solo con iTiOpe ≠ 4 — aviso: el dato no llega al XML
        declare(rule("JBS-D-54").field("cCiuRec").warning().showValue().source(RuleSource.NT_003),
                passes(s -> !(ope(s) == 4 && s.e().getcCiuRec() != 0), s -> s.e().getcCiuRec()));
        declare(rule("JBS-D-55").field("dDesCiuRec").error().showValue().source(RuleSource.NT_017),
                passes(s -> !(s.e().getcCiuRec() != 0 && !present(s.e().getdDesCiuRec())) && ValueChecks.length(s.e().getdDesCiuRec(), 1, 30)));
        declare(rule("JBS-D-56").field("dTelRec").error().lengthOnly(),
                length(TgDatRec::getdTelRec, 6, 15));
        declare(rule("JBS-D-57").field("dCelRec").error().lengthOnly(),
                length(TgDatRec::getdCelRec, 10, 20));
        declare(rule("JBS-D-58").field("dEmailRec").error().omitValue(),
                passes(s -> validEmail(s.e().getdEmailRec()), s -> s.e().getdEmailRec()));
        declare(rule("JBS-D-59").field("dCodCliente").error().omitValue(),
                length(TgDatRec::getdCodCliente, 3, 15));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgDatRec>> subjects(DocumentoElectronico raiz) {
        return raiz.getgDatGralOpe() == null ? none() : unique(raiz.getgDatGralOpe().getgDatRec());
    }

    private static int tipo(Subject<DocumentoElectronico, TgDatRec> s) {
        return DEData.iTiDE(s.root());
    }

    private static int nat(Subject<DocumentoElectronico, TgDatRec> s) {
        return DEData.iNatRec(s.root());
    }

    private static int ope(Subject<DocumentoElectronico, TgDatRec> s) {
        return DEData.iTiOpe(s.root());
    }

    private static int doc(Subject<DocumentoElectronico, TgDatRec> s) {
        return DEData.iTipIDRec(s.root());
    }

    /** RUC del receptor distinto del RUC del emisor (los dos cargados). */
    private static boolean rucDistinto(Subject<DocumentoElectronico, TgDatRec> s) {
        TgEmis e = s.root().getgDatGralOpe().getgEmis();
        String rucEm = e == null ? null : e.getdRucEm();
        return present(s.e().getdRucRec()) && present(rucEm) && !s.e().getdRucRec().equals(rucEm);
    }

    /** D208c: total en guaraníes de 7.000.000 o más, salvo muestras médicas (iTipTra 13). */
    private static boolean superaUmbral(Subject<DocumentoElectronico, TgDatRec> s) {
        BigDecimal total = DEData.totalInGuaranies(s.root());
        return DEData.iTipTra(s.root()) != 13 && total != null && total.compareTo(DEData.UNNAMED_RECEIVER_THRESHOLD) >= 0;
    }
}

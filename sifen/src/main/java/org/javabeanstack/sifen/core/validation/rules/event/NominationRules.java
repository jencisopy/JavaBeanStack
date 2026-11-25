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
import org.javabeanstack.sifen.core.fields.request.event.TrGeVeNom;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;
import org.javabeanstack.sifen.core.types.PaisType;
import org.javabeanstack.sifen.core.types.TDepartamento;
import org.javabeanstack.sifen.core.types.TiTipDocRec;

/**
 * Evento de nominación (rGEveNom, NT 014/027; guardas de E0-18 y E0-12).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class NominationRules extends RuleSet<TrGesEve, TrGeVeNom> {

    NominationRules() {
        super("EVENTO-rGEveNom", RuleGroup.EVENTO, "rEve.gGroupTiEvt.rGEveNom");
        // I3-42 (§3.8, ciclo 2): datos que la librería no escribe; sus reglas de ERROR no se evalúan
        notWrittenIf("iTiContRec", s -> s.e().getiNatRec() != 1); // TrGeVeNom: solo con iNatRec = 1
        notWrittenIf("dRucRec", s -> s.e().getiNatRec() != 1); // TrGeVeNom: solo con iNatRec = 1
        notWrittenIf("dDVRec", s -> s.e().getiNatRec() != 1); // TrGeVeNom: solo con iNatRec = 1
        notWrittenIf("iTipIDRec", s -> s.e().getiNatRec() == 1); // TrGeVeNom: solo con iNatRec ≠ 1
        notWrittenIf("dDTipIDRec", s -> s.e().getiNatRec() == 1); // TrGeVeNom: solo con iNatRec ≠ 1
        notWrittenIf("dNumIDRec", s -> s.e().getiNatRec() == 1); // TrGeVeNom: solo con iNatRec ≠ 1
        notWrittenIf("dNumCasRec", s -> s.e().getdDirRec() == null || s.e().getdDirRec().isEmpty()); // TrGeVeNom: datos de la dirección solo con dDirRec
        notWrittenIf("cDepRec", s -> s.e().getdDirRec() == null || s.e().getdDirRec().isEmpty()); // TrGeVeNom: datos de la dirección solo con dDirRec
        notWrittenIf("dDesDepRec", s -> s.e().getdDirRec() == null || s.e().getdDirRec().isEmpty()); // TrGeVeNom: datos de la dirección solo con dDirRec
        notWrittenIf("dDesDisRec", s -> s.e().getdDirRec() == null || s.e().getdDirRec().isEmpty()); // TrGeVeNom: datos de la dirección solo con dDirRec
        notWrittenIf("dDesCiuRec", s -> s.e().getdDirRec() == null || s.e().getdDirRec().isEmpty()); // TrGeVeNom: datos de la dirección solo con dDirRec
        declare(rule("GENFE002").field("Id").error().dCodRes("4451").showValue().source(RuleSource.NT_014),
                passes(s -> validCdc(s.e().getId())));
        declare(rule("GENFE002c").field("Id").error().dCodRes("4454").showValue().source(RuleSource.NT_014),
                passes(s -> !validCdc(s.e().getId()) || s.e().getId().startsWith("01")));
        declare(rule("JBS-EVENTO-30").field("mOtEve").error().showValue().source(RuleSource.NT_014),
                requiredWithLength(TrGeVeNom::getmOtEve, 5, 500));
        declare(rule("JBS-EVENTO-31").field("iNatRec").error().showValue().source(RuleSource.NT_014),
                passes(s -> s.e().getiNatRec() == 1 || s.e().getiNatRec() == 2, s -> s.e().getiNatRec()));
        declare(rule("JBS-EVENTO-32").field("iTiOpe").error().showValue().source(RuleSource.NT_014),
                passes(s -> s.e().getiTiOpe() == 1 || s.e().getiTiOpe() == 2 || s.e().getiTiOpe() == 4, s -> s.e().getiTiOpe()));
        declare(rule("JBS-EVENTO-33").field("cPaisRec").error().showValue().source(RuleSource.NT_014),
                passes(s -> present(s.e().getcPaisRec()) && PaisType.getByName(s.e().getcPaisRec()) != null, s -> s.e().getcPaisRec()));
        declare(rule("GENFE006").field("dDesPaisRe").error().dCodRes("4456").showValue().source(RuleSource.NT_014),
                passes(s -> !(pais(s) != null && s.e().getdDesPaisRe() != null && !pais(s).getNombre().equals(s.e().getdDesPaisRe()))
                && ValueChecks.length(s.e().getdDesPaisRe(), 4, 50)));
        declare(rule("GENFE007").field("iTiContRec").error().dCodRes("4457").showValue().source(RuleSource.NT_014),
                passes(s -> s.e().getiTiContRec() == 1 || s.e().getiTiContRec() == 2, s -> s.e().getiTiContRec())); // M3-59: iNatRec ≠ 1 lo excluye notWrittenIf
        // I3-42: TrGeVeNom escribe iTiContRec solo con iNatRec = 1 — aviso: el dato no llega al XML
        declare(rule("GENFE007a").field("iTiContRec").warning().dCodRes("4458").showValue().source(RuleSource.NT_014),
                passes(s -> !(s.e().getiNatRec() == 2 && s.e().getiTiContRec() != 0), s -> s.e().getiTiContRec()));
        declare(rule("GENFE008").field("dRucRec").error().dCodRes("4459").omitValue().source(RuleSource.NT_014),
                passes(s -> !mal(true, s.e().getdRucRec(), 3, 8), s -> s.e().getdRucRec())); // M3-59: iNatRec ≠ 1 lo excluye notWrittenIf
        declare(rule("JBS-EVENTO-34").field("dDVRec").error().omitValue().source(RuleSource.NT_014),
                passes(s -> !present(s.e().getdRucRec()) || validDV(s.e().getdRucRec(), String.valueOf(s.e().getdDVRec()))));
        declare(rule("GENFE010").field("iTipIDRec").error().showValue().source(RuleSource.NT_014, RuleSource.NT_027),
                passes(s -> !((s.e().getiNatRec() == 2 && s.e().getiTipIDRec() == 0) || (s.e().getiTipIDRec() != 0 && !docValido(s.e().getiTipIDRec()))),
                s -> s.e().getiTipIDRec()));
        declare(rule("GENFE011").field("dDTipIDRec").error().showValue().source(RuleSource.NT_027),
                passes(s -> s.e().getiTipIDRec() == 0 || !docValido(s.e().getiTipIDRec()) || descripcionCorrecta(s.e())));
        declare(rule("GENFE012").field("dNumIDRec").error().omitValue().source(RuleSource.NT_014),
                passes(s -> !mal(s.e().getiNatRec() == 2, s.e().getdNumIDRec(), 1, 20), s -> s.e().getdNumIDRec()));
        // I3-43: Evento_v150.xsd (2019) no tiene rGEveNom (NT 014 es posterior): sin fuente para el formato,
        // aviso por analogía con tdNumDocId (trGeVeEnd.dNumIDRec, :238) hasta confirmarlo en vivo
        declare(rule("JBS-EVENTO-52").field("dNumIDRec").warning().omitValue().source(RuleSource.NT_014),
                passes(s -> ValueChecks.validDocumentNumber(s.e().getdNumIDRec()), s -> s.e().getdNumIDRec()));
        declare(rule("JBS-EVENTO-35").field("dNomRec").error().lengthOnly().source(RuleSource.NT_014),
                requiredWithLength(TrGeVeNom::getdNomRec, 4, 255));
        declare(rule("JBS-EVENTO-36").field("dNomFanRec").error().lengthOnly().source(RuleSource.NT_014),
                length(TrGeVeNom::getdNomFanRec, 4, 255));
        declare(rule("JBS-EVENTO-37").field("dDirRec").error().lengthOnly().source(RuleSource.NT_014),
                length(TrGeVeNom::getdDirRec, 1, 255));
        declare(rule("GENFE016").field("dNumCasRec").error().omitValue().source(RuleSource.NT_014),
                passes(s -> !present(s.e().getdDirRec()) || maxDigits(s.e().getdNumCasRec(), 6)));
        declare(rule("JBS-EVENTO-38").field("cDepRec").error().showValue().source(RuleSource.NT_014),
                passes(s -> s.e().getcDepRec() == null || TDepartamento.getByVal(s.e().getcDepRec()) != null, s -> s.e().getcDepRec()));
        declare(rule("JBS-EVENTO-39").field("dDesDepRec").error().showValue().source(RuleSource.NT_014),
                passes(s -> dep(s) == null || s.e().getdDesDepRec() == null || dep(s).getDescripcion().equals(s.e().getdDesDepRec())));
        // E3-27: guarda de E0-18 (TrGeVeNom evalúa cDisRec != 0 antes de != null): no desactivable
        declare(rule("JBS-EVENTO-40").nonDisableable().field("cDisRec").error().showValue().source(RuleSource.NT_014, RuleSource.JBS),
                passes(s -> !(present(s.e().getdDirRec()) && s.e().getcDisRec() == null)
                && (s.e().getcDisRec() == null || maxDigits(s.e().getcDisRec(), 4)), s -> s.e().getcDisRec()));
        declare(rule("JBS-EVENTO-41").field("dDesDisRec").error().showValue().source(RuleSource.NT_014),
                passes(s -> !mal(s.e().getcDisRec() != null && s.e().getcDisRec() > 0, s.e().getdDesDisRec(), 1, 30)));
        declare(rule("JBS-EVENTO-42").field("cCiuRec").error().showValue().source(RuleSource.NT_014),
                passes(s -> maxDigits(s.e().getcCiuRec(), 5), s -> s.e().getcCiuRec()));
        // E8-01 (D33): de baja. Solo frenaba la ciudad por el defecto de la etiqueta (E0-12: la librería escribía
        // dDesCiuEnt), corregido en §6.1 el 2026-11-24; ya no hay nada que frenar, así que pasa siempre, también con
        // una fecha de reglas anterior a la baja. La reemplaza JBS-EVENTO-56, la regla del MT para GENFE022.
        declare(rule("JBS-EVENTO-43").field("dDesCiuRec").error().showValue().source(RuleSource.NT_014, RuleSource.JBS)
                        .until(java.time.LocalDate.of(2026, 11, 24), "E0-12 corregido en §6.1: la ciudad sale como dDesCiuRec (GENFE022)"),
                passes(s -> true));
        // E8-01: GENFE022 dDesCiuRec (A1-30), obligatoria si se informa GENFE021 cCiuRec (NT 014); como JBS-EVENTO-41
        // para el distrito. Sin descripción la librería no escribe tampoco cCiuRec
        declare(rule("JBS-EVENTO-56").field("dDesCiuRec").error().showValue().source(RuleSource.NT_014).replaces("JBS-EVENTO-43"),
                passes(s -> !mal(s.e().getcCiuRec() > 0, s.e().getdDesCiuRec(), 1, 30)));
        declare(rule("JBS-EVENTO-44").field("dTelRec").error().lengthOnly().source(RuleSource.NT_014),
                length(TrGeVeNom::getdTelRec, 6, 15));
        declare(rule("JBS-EVENTO-45").field("dCelRec").error().lengthOnly().source(RuleSource.NT_014),
                length(TrGeVeNom::getdCelRec, 10, 20));
        declare(rule("JBS-EVENTO-46").field("dEmailRec").error().omitValue().source(RuleSource.NT_014),
                passes(s -> validEmail(s.e().getdEmailRec()), s -> s.e().getdEmailRec()));
        declare(rule("JBS-EVENTO-47").field("dCodCliente").error().omitValue().source(RuleSource.NT_014),
                length(TrGeVeNom::getdCodCliente, 3, 15));
    }

    @Override
    protected List<Subject<TrGesEve, TrGeVeNom>> subjects(TrGesEve raiz) {
        return raiz.getgGroupTiEvt() == null ? none() : unique(raiz.getgGroupTiEvt().getrGeVeNom());
    }

    /** Falta siendo obligatorio, o no tiene el largo pedido. */
    private static boolean mal(boolean obligatorio, String texto, int min, int max) {
        return (obligatorio && !present(texto)) || !ValueChecks.length(texto, min, max);
    }

    private static PaisType pais(Subject<TrGesEve, TrGeVeNom> s) {
        return PaisType.getByName(s.e().getcPaisRec());
    }

    private static TDepartamento dep(Subject<TrGesEve, TrGeVeNom> s) {
        return s.e().getcDepRec() == null ? null : TDepartamento.getByVal(s.e().getcDepRec());
    }

    private static boolean docValido(short doc) {
        return doc == 1 || doc == 2 || doc == 3 || doc == 4 || doc == 6 || doc == 9;
    }

    /** GENFE011: con «Otro» (9) texto libre de 9 a 41; en los demás, la descripción del código. */
    private static boolean descripcionCorrecta(TrGeVeNom n) {
        if (n.getiTipIDRec() == 9) {
            return present(n.getdDTipIDRec()) && ValueChecks.length(n.getdDTipIDRec(), 9, 41);
        }
        String esperado = null;
        for (TiTipDocRec t : TiTipDocRec.values()) {
            if (t.getVal() == n.getiTipIDRec()) {
                esperado = t.getDescripcion();
            }
        }
        return present(n.getdDTipIDRec()) && (esperado == null || esperado.equals(n.getdDTipIDRec()));
    }
}

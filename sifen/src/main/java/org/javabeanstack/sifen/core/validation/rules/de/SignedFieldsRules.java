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
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import org.javabeanstack.sifen.core.fields.request.de.TgEmis;
import org.javabeanstack.sifen.internal.util.SifenUtil;

/**
 * Grupo A del MT: campos firmados del DE (Id, dDVId, dFecFirma, dSisFact).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class SignedFieldsRules extends RuleSet<DocumentoElectronico, DocumentoElectronico> {

    SignedFieldsRules() {
        super("A-campos-firmados", RuleGroup.A, "");
        declareNotEvaluated(rule("JBS-A-01").field("rDE.dVerFor").error().showValue(),
                NotEvaluatedReason.CONSTANT);
        declare(rule("JBS-A-02").field("Id").error().showValue(),
                passes(s -> !present(s.e().getId()) || (s.e().getId().length() == 44 && digitsOnly(s.e().getId())), s -> s.e().getId()));
        declare(rule("A003").field("dDVId").error().dCodRes("1003").showValue(),
                passes(s -> !incumpleA003(s.e())));
        declare(rule("A002").field("Id").error().dCodRes("1000").showValue(),
                passes(s -> !incumpleA002(s.e())));
        declare(rule("JBS-A-03").field("dFecFirma").error().showValue(),
                required(DocumentoElectronico::getdFecFirma));
        declare(rule("A004a").field("dFecFirma").warning().dCodRes("1004").showValue(),
                passes(s -> s.e().getdFecFirma() == null || !s.e().getdFecFirma().isAfter(s.ctx().getNow()), s -> s.e().getdFecFirma()));
        // §6.5 (M3-25): el valor mostrado lleva las horas desde la firma; el plazo de 72 h (MT §6.2) lo da
        // SignedDocument.getSubmissionDeadline()
        declare(rule("A004b").field("dFecFirma").warning().dCodRes("1005").showValue(),
                passes(s -> s.e().getdFecFirma() == null || Duration.between(s.e().getdFecFirma(), s.ctx().getNow()).toHours() <= 72,
                        s -> s.e().getdFecFirma() + " (" + Duration.between(s.e().getdFecFirma(), s.ctx().getNow()).toHours()
                                + " h; plazo: 72 h)"));
        declare(rule("JBS-A-04").field("dSisFact").error().showValue().source(RuleSource.NT_010),
                passes(s -> s.e().getdSisFact() == 1 || s.e().getdSisFact() == 2, s -> s.e().getdSisFact()));
    }

    @Override
    protected List<Subject<DocumentoElectronico, DocumentoElectronico>> subjects(DocumentoElectronico raiz) {
        return unique(raiz);
    }

    private static final DateTimeFormatter AAAAMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static boolean cdc44(String id) {
        return present(id) && id.length() == 44 && digitsOnly(id);
    }

    /** A003: el DV del CDC cargado no es el módulo 11 de los 43 primeros (o no coincide con dDVId). */
    private static boolean incumpleA003(DocumentoElectronico de) {
        String id = de.getId();
        if (!cdc44(id)) {
            return false;
        }
        String esperado = SifenUtil.generateDv(id.substring(0, 43));
        return !esperado.equals(id.substring(43)) || (present(de.getdDVId()) && !esperado.equals(de.getdDVId()));
    }

    /** A002: el CDC cargado no corresponde a los datos del DE. */
    private static boolean incumpleA002(DocumentoElectronico de) {
        String id = de.getId();
        if (!cdc44(id)) {
            return false;
        }
        String armado = cdcSinDv(de);
        return armado != null && !armado.equals(id.substring(0, 43));
    }

    /** Los 43 primeros dígitos del CDC según los datos del DE, o {@code null} si falta alguno. */
    private static String cdcSinDv(DocumentoElectronico de) {
        TgEmis e = de.getgDatGralOpe() == null ? null : de.getgDatGralOpe().getgEmis();
        if (de.getgTimb() == null || de.getgTimb().getiTiDE() == null || e == null || e.getiTipCont() == null
                || !present(e.getdRucEm()) || !present(e.getdDVEmi()) || !present(de.getgTimb().getdEst())
                || !present(de.getgTimb().getdPunExp()) || !present(de.getgTimb().getdNumDoc())
                || de.getgDatGralOpe().getdFeEmiDE() == null || de.getgOpeDE() == null
                || de.getgOpeDE().getiTipEmi() == null || !present(de.getgOpeDE().getdCodSeg())) {
            return null;
        }
        return SifenUtil.leftPad(String.valueOf(de.getgTimb().getiTiDE().getVal()), '0', 2)
                + SifenUtil.leftPad(e.getdRucEm(), '0', 8) + e.getdDVEmi() + de.getgTimb().getdEst()
                + de.getgTimb().getdPunExp() + de.getgTimb().getdNumDoc() + e.getiTipCont().getVal()
                + de.getgDatGralOpe().getdFeEmiDE().format(AAAAMMDD) + de.getgOpeDE().getiTipEmi().getVal()
                + de.getgOpeDE().getdCodSeg();
    }
}

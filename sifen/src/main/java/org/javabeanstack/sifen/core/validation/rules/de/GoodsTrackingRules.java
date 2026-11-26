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
import org.javabeanstack.sifen.core.fields.request.de.TgRasMerc;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;
import org.javabeanstack.sifen.core.fields.request.de.TgCamItem;

/**
 * Grupo E del MT, E8.4: gRasMerc (rastreo de la mercadería).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class GoodsTrackingRules extends RuleSet<DocumentoElectronico, TgRasMerc> {

    GoodsTrackingRules() {
        super("E-gRasMerc", RuleGroup.E, "gDtipDE.gCamItem[].gRasMerc");
        declare(rule("JBS-E-94").field("dNumLote").error().showValue().source(RuleSource.NT_010),
                length(TgRasMerc::getdNumLote, 1, 80));
        declareNotEvaluated(rule("JBS-E-95").field("dVencMerc").error().showValue(),
                NotEvaluatedReason.TYPE);
        declare(rule("JBS-E-96").field("dNSerie").error().showValue(),
                length(TgRasMerc::getdNSerie, 1, 10));
        declare(rule("JBS-E-97").field("dNumPedi").error().showValue(),
                length(TgRasMerc::getdNumPedi, 1, 20));
        declare(rule("JBS-E-98").field("dNumSegui").error().showValue(),
                length(TgRasMerc::getdNumSegui, 1, 20));
        // E12-08 (D33, ESC-44 opción A1): de baja. La NT 010 (04/02/2022, ap. 1.4) eliminó dNomImp, dDirImp y dNumFir
        // del grupo y la librería ya no los escribe (sus accesores quedan deprecados y sin efecto en el XML): no hay nada
        // que controlar, así que pasan siempre, también con una fecha de reglas anterior a la baja. Los códigos no se
        // reasignan; el aviso de «campo eliminado, se ignora» es una regla nueva de la Fase 7.
        declare(rule("JBS-E-99").field("dNomImp").error().lengthOnly().source(RuleSource.NT_010)
                        .until(java.time.LocalDate.of(2022, 2, 4), "NT 010 (ap. 1.4): se elimina dNomImp (E756); no se escribe en el XML (E12-08)"),
                passes(s -> true));
        declare(rule("JBS-E-100").field("dDirImp").error().lengthOnly().source(RuleSource.NT_010)
                        .until(java.time.LocalDate.of(2022, 2, 4), "NT 010 (ap. 1.4): se elimina dDirImp (E757); no se escribe en el XML (E12-08)"),
                passes(s -> true));
        declare(rule("JBS-E-101").field("dNumFir").error().showValue().source(RuleSource.NT_010)
                        .until(java.time.LocalDate.of(2022, 2, 4), "NT 010 (ap. 1.4): se elimina dNumFir (E758); no se escribe en el XML (E12-08)"),
                passes(s -> true));
        declare(rule("JBS-E-102").field("dNumReg").error().showValue().source(RuleSource.NT_010),
                length(TgRasMerc::getdNumReg, 1, 20));
        declare(rule("JBS-E-103").field("dNumRegEntCom").error().showValue().source(RuleSource.NT_010),
                length(TgRasMerc::getdNumRegEntCom, 1, 20));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgRasMerc>> subjects(DocumentoElectronico raiz) {
        return raiz.getgDtipDE() == null ? none() : perItem(raiz.getgDtipDE().getgCamItemList(), TgCamItem::getgRasMerc);
    }
}

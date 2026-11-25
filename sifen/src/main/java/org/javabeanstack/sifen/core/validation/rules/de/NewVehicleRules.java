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
import org.javabeanstack.sifen.core.fields.request.de.TgVehNuevo;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.DEData;
import org.javabeanstack.sifen.core.fields.request.de.TgCamItem;

/**
 * Grupo E del MT, E8.5: gVehNuevo (venta de vehículos nuevos).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class NewVehicleRules extends RuleSet<DocumentoElectronico, TgVehNuevo> {

    NewVehicleRules() {
        super("E-gVehNuevo", RuleGroup.E, "gDtipDE.gCamItem[].gVehNuevo");
        declareNotEvaluated(rule("JBS-E-104").field("dDesTipOpVN").error().showValue(),
                NotEvaluatedReason.DERIVED);
        declare(rule("JBS-E-105").field("dChasis").error().showValue(),
                length(TgVehNuevo::getdChasis, 17, 17));
        declare(rule("JBS-E-106").field("dColor").error().showValue(),
                length(TgVehNuevo::getdColor, 1, 10));
        declare(rule("JBS-E-107").field("dPotencia").error().showValue(),
                passes(s -> maxDigits(s.e().getdPotencia(), 4), s -> s.e().getdPotencia()));
        declare(rule("JBS-E-108").field("dCapMot").error().showValue(),
                passes(s -> maxDigits(s.e().getdCapMot(), 4), s -> s.e().getdCapMot()));
        declare(rule("JBS-E-109").field("dPNet").error().showValue(),
                passes(s -> ValueChecks.decimal(s.e().getdPNet(), 6, 4), s -> s.e().getdPNet()));
        declare(rule("JBS-E-110").field("dPBruto").error().showValue(),
                passes(s -> ValueChecks.decimal(s.e().getdPBruto(), 6, 4), s -> s.e().getdPBruto()));
        declare(rule("JBS-E-111").field("iTipCom").error().showValue(),
                requiredIf(s -> s.e().getiTipOpVN() != null, TgVehNuevo::getiTipCom));
        declare(rule("JBS-E-112").field("dDesTipCom").error().showValue(),
                passes(s -> s.e().getiTipOpVN() == null || s.e().getiTipCom() == null || s.e().getiTipCom().getVal() != 9
                || (present(s.e().getdDesTipCom()) && ValueChecks.length(s.e().getdDesTipCom(), 3, 20)), s -> s.e().getdDesTipCom()));
        declare(rule("JBS-E-113").field("dNroMotor").error().showValue(),
                length(TgVehNuevo::getdNroMotor, 1, 21));
        declare(rule("JBS-E-114").field("dCapTracc").error().showValue(),
                passes(s -> ValueChecks.decimal(s.e().getdCapTracc(), 6, 4), s -> s.e().getdCapTracc()));
        declare(rule("JBS-E-115").field("dAnoFab").error().showValue(),
                passes(s -> s.e().getdAnoFab() == 0 || (s.e().getdAnoFab() > 0 && digitCount(s.e().getdAnoFab()) == 4), s -> s.e().getdAnoFab()));
        declare(rule("JBS-E-116").field("cTipVeh").error().showValue(),
                length(TgVehNuevo::getcTipVeh, 4, 10));
        declare(rule("JBS-E-117").field("dCapac").error().showValue(),
                passes(s -> maxDigits(s.e().getdCapac(), 3), s -> s.e().getdCapac()));
        declare(rule("JBS-E-118").field("dCilin").error().showValue(),
                length(TgVehNuevo::getdCilin, 4, 4));
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgVehNuevo>> subjects(DocumentoElectronico raiz) {
        return raiz.getgDtipDE() == null ? none() : perItem(raiz.getgDtipDE().getgCamItemList(), TgCamItem::getgVehNuevo);
    }
}

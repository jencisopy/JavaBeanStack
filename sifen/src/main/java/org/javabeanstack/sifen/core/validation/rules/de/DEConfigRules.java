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

/**
 * Reglas del validador sobre la configuración aplicada al DE: JBS-VAL-03 (NT 13 apagada, E0-16; D25), dada de baja
 * en §6.3: desde la Fase 6 la librería escribe {@code dBasExe} siempre y la NT 13 ya no se puede apagar.
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class DEConfigRules extends RuleSet<DocumentoElectronico, DocumentoElectronico> {

    DEConfigRules() {
        super("VAL-configuracion-DE", RuleGroup.VAL, "");
        // §6.3 (D25): de baja; el código no se reasigna y el catálogo la muestra tachada. No queda qué evaluar:
        // la librería escribe dBasExe siempre (CONSTANT), también con una fecha de reglas anterior a la baja.
        declareNotEvaluated(rule("JBS-VAL-03").nonDisableable().field("DE").error().showValue().source(RuleSource.NT_013, RuleSource.JBS)
                        .until(java.time.LocalDate.of(2026, 11, 24), "D25 (§6.3): dBasExe se escribe siempre; la NT 13 ya no se puede apagar"),
                NotEvaluatedReason.CONSTANT);
    }

    @Override
    protected List<Subject<DocumentoElectronico, DocumentoElectronico>> subjects(DocumentoElectronico raiz) {
        return unique(raiz);
    }

}

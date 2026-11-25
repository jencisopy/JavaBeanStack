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

/**
 * Grupo H del MT: la lista gCamDEAsoc[] (E2-01: obligatoria en AFE, NC y ND).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos, en el orden del catálogo.
 * Migrado de los grupos de §3.3 sin cambio de comportamiento (§3.6, CA3-07).</p>
 */
final class AssociatedDocumentsRules extends RuleSet<DocumentoElectronico, DocumentoElectronico> {

    AssociatedDocumentsRules() {
        super("H-lista", RuleGroup.H, "");
        declare(rule("H001").nonDisableable().field("gCamDEAsoc[]").error().dCodRes("2400").showValue().source(RuleSource.NT_023),
                passes(s -> !(exigido(s) && !present(s.e().getgCamDEAsocList()))));
        declare(rule("H001b").field("gCamDEAsoc[]").error().dCodRes("2415").showValue(),
                passes(s -> s.e().getgCamDEAsocList() == null || !(exigido(s) && s.e().getgCamDEAsocList().size() > 1), s -> s.e().getgCamDEAsocList()));
    }

    @Override
    protected List<Subject<DocumentoElectronico, DocumentoElectronico>> subjects(DocumentoElectronico raiz) {
        return unique(raiz);
    }

    private static boolean exigido(Subject<DocumentoElectronico, DocumentoElectronico> s) {
        int tipo = DEData.iTiDE(s.root());
        return tipo == 4 || tipo == 5 || tipo == 6;
    }
}

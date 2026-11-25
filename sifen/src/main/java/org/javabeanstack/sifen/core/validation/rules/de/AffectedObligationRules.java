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

import java.time.LocalDate;
import java.util.List;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.fields.request.de.TgOblAfe;
import org.javabeanstack.sifen.core.fields.request.de.TgOpeCom;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.Subject;

/**
 * Grupo D del MT, D1.1: gOblAfe[] (obligaciones afectadas, NT 018 y NT 022; plan JBSSIF, §6.1, I0-01).
 *
 * <p>Una regla por fila del catálogo (D33): ficha y chequeo juntos. D031: el código está en la Tabla 12 (el enum
 * {@code TcOblAfe} solo admite los de la tabla; uno desconocido leído de un XML queda {@code null}); D031a: no se
 * repite en el documento; D032: la descripción la deriva la librería del código. El tope de 11 ocurrencias es
 * {@code JBS-D-62}, en {@code CommercialOperationRules}.</p>
 */
final class AffectedObligationRules extends RuleSet<DocumentoElectronico, TgOblAfe> {

    /** NT 018 en producción. */
    private static final LocalDate NT_018_PRODUCCION = LocalDate.of(2024, 1, 8);
    /** NT 022 en producción. */
    private static final LocalDate NT_022_PRODUCCION = LocalDate.of(2024, 2, 9);

    AffectedObligationRules() {
        super("D-gOblAfe", RuleGroup.D, "gDatGralOpe.gOpeCom.gOblAfe[]");
        declare(rule("D031").field("cOblAfe").error().dCodRes("1220").showValue().source(RuleSource.NT_018).from(NT_018_PRODUCCION),
                onNull(s -> s.e() != null && s.e().getcOblAfe() != null, s -> null)); // un gOblAfe nulo también es D031
        declare(rule("D031a").field("cOblAfe").error().dCodRes("1222").showValue().source(RuleSource.NT_022).from(NT_022_PRODUCCION),
                passes(s -> s.e() == null || s.e().getcOblAfe() == null || !repetido(s), s -> s.e().getcOblAfe()));
        declareNotEvaluated(rule("D032").field("dDesOblAfe").error().dCodRes("1221").showValue().source(RuleSource.NT_018).from(NT_018_PRODUCCION),
                NotEvaluatedReason.DERIVED);
    }

    @Override
    protected List<Subject<DocumentoElectronico, TgOblAfe>> subjects(DocumentoElectronico raiz) {
        TgOpeCom ope = raiz.getgDatGralOpe() == null ? null : raiz.getgDatGralOpe().getgOpeCom();
        return ope == null ? none() : perItem(ope.getgOblAfeList());
    }

    /** El mismo código aparece en una obligación anterior de la lista (D031a). */
    private static boolean repetido(Subject<DocumentoElectronico, TgOblAfe> s) {
        List<TgOblAfe> lista = s.root().getgDatGralOpe().getgOpeCom().getgOblAfeList();
        for (int i = 0; i < s.index(0) && i < lista.size(); i++) {
            TgOblAfe otra = lista.get(i);
            if (otra != null && otra.getcOblAfe() == s.e().getcOblAfe()) {
                return true;
            }
        }
        return false;
    }
}

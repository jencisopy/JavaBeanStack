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
package org.javabeanstack.sifen.core.validation;

import java.time.LocalDate;

/**
 * Desde y hasta cuándo rige una regla (D33). Una regla obsoleta <b>no se borra</b>: queda declarada con
 * {@code until} y el motivo, y el catálogo la muestra tachada.
 *
 * @param source     documento que la introdujo o la modificó por última vez.
 * @param from      primer día en que rige; {@code null} = sin límite.
 * @param until      primer día en que ya no rige; {@code null} = sin límite.
 * @param endReason por qué dejó de regir; {@code null} si sigue vigente.
 */
public record EffectivePeriod(RuleSource source, LocalDate from, LocalDate until, String endReason) {

    /** Vigencia por defecto: MT v150, siempre. */
    public static final EffectivePeriod ALWAYS = new EffectivePeriod(RuleSource.MT_V150, null, null, null);

    /**
     * Indica si la regla rige en una fecha: {@code from ≤ fecha < until}.
     *
     * @param fecha fecha de referencia de las reglas; {@code null} = sin restricción.
     * @return {@code true} si rige.
     */
    public boolean isInForceOn(LocalDate fecha) {
        if (fecha == null) {
            return true;
        }
        return (from == null || !fecha.isBefore(from)) && (until == null || fecha.isBefore(until));
    }
}

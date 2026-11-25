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

/**
 * Por qué una regla del catálogo no se evalúa localmente (I3-15). Se declara junto a la ficha con
 * {@link RuleSet#declareNotEvaluated}.
 */
public enum NotEvaluatedReason {
    /** La librería escribe un valor fijo ({@code dVerFor} = 150). */
    CONSTANT("La librería escribe un valor constante."),
    /** El tipo Java del campo ({@code LocalDate}) ya garantiza el formato. */
    TYPE("El tipo del campo garantiza el formato."),
    /** La librería deriva el texto del código; el modelo no lo guarda aparte. */
    DERIVED("La librería deriva la descripción del código; el modelo no la guarda aparte."),
    /** La librería calcula el campo al generar y pisa lo que haya cargado. */
    COMPUTED("La librería calcula el campo al generar el XML.");

    private final String descripcion;

    NotEvaluatedReason(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Devuelve el motivo en español.
     *
     * @return la descripción.
     */
    public String getDescription() {
        return descripcion;
    }
}

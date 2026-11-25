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
 * La lógica de una regla (D33): {@code true} si el sujeto la cumple. Debe ser null-safe: con un dato
 * ausente una regla de formato <b>cumple</b> (la ausencia es otra regla). Si igual lanza, el conjunto
 * reporta {@link ValidationRule#JBS_VAL_00} y sigue con la regla siguiente.
 *
 * @param <T> tipo de la raíz.
 * @param <E> tipo del elemento del subgrupo.
 */
@FunctionalInterface
public interface Check<T, E> {

    /**
     * Evalúa la regla.
     *
     * @param s sujeto (raíz, elemento, índices, contexto).
     * @return {@code true} si se cumple.
     */
    boolean passes(Subject<T, E> s);

    /**
     * Valor rechazado que se adjunta al error (pasa por la política de la ficha).
     *
     * @param s sujeto.
     * @return el valor, o {@code null}.
     */
    default Object value(Subject<T, E> s) {
        return null;
    }

    /**
     * Detalles para los marcadores {@code {0}}, {@code {1}}… del mensaje (nunca datos del documento ni
     * secretos).
     *
     * @param s sujeto.
     * @return los argumentos, o {@code null}.
     */
    default Object[] args(Subject<T, E> s) {
        return null;
    }
}

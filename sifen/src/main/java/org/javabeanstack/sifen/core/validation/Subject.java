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
 * Lo que recibe el chequeo de una regla (D33): la raíz validada, el elemento del subgrupo (puede ser
 * {@code null}: el grupo falta o el ítem de la lista es nulo), los índices base 0 de las listas que hay
 * en la ruta y el contexto de la corrida.
 *
 * @param root     {@code DocumentoElectronico} o {@code TrGesEve}.
 * @param element elemento del subgrupo; puede ser {@code null}.
 * @param indexes  índices base 0, del más externo al más interno.
 * @param ctx      contexto de la corrida (configuración, reloj, ambiente).
 * @param <T>      tipo de la raíz.
 * @param <E>      tipo del elemento.
 */
public record Subject<T, E>(T root, E element, int[] indexes, ValidationContext ctx) {

    private static final int[] SIN_INDICES = new int[0];

    /**
     * Crea un sujeto sin raíz ni contexto (los completa el conjunto antes de evaluar).
     *
     * @param elemento elemento; puede ser {@code null}.
     * @param indices  índices base 0.
     * @param <T>      tipo de la raíz.
     * @param <E>      tipo del elemento.
     * @return el sujeto.
     */
    public static <T, E> Subject<T, E> of(E elemento, int... indices) {
        return new Subject<>(null, elemento, indices == null ? SIN_INDICES : indices, null);
    }

    /**
     * Devuelve una copia con la raíz y el contexto.
     *
     * @param r   raíz.
     * @param ctx contexto.
     * @return el sujeto completo.
     */
    public Subject<T, E> with(T r, ValidationContext ctx) {
        return new Subject<>(r, element, indexes, ctx);
    }

    /**
     * Atajo de {@link #element()}.
     *
     * @return el elemento; puede ser {@code null}.
     */
    public E e() {
        return element;
    }

    /**
     * Devuelve el índice de un nivel de lista.
     *
     * @param nivel 0 = la lista más externa.
     * @return el índice base 0, o -1 si no hay ese nivel.
     */
    public int index(int nivel) {
        return nivel < indexes.length ? indexes[nivel] : -1;
    }
}

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
package org.javabeanstack.sifen.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Normalización de los valores de los tipos del contrato (plan JBSSIF, §10.2): textos nunca
 * {@code null} y listas inmodificables sin nulos. No es parte del contrato (package-private).
 */
final class ApiValues {
    /** Tope de mensajes de SIFEN por resultado (MT: {@code gResProc} hasta 100). */
    static final int MAX_MESSAGES = 100;

    private ApiValues() {
    }

    /** Texto o vacío. */
    static String text(String value) {
        return value == null ? "" : value;
    }

    /** Copia inmodificable sin nulos. */
    static <T> List<T> list(List<T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<T> copy = new ArrayList<>(values.size());
        for (T value : values) {
            if (value != null) {
                copy.add(value);
            }
        }
        return List.copyOf(copy);
    }

    /** Copia inmodificable sin nulos, con los primeros {@link #MAX_MESSAGES}. */
    static List<SifenMessage> messages(List<SifenMessage> values) {
        List<SifenMessage> copy = list(values);
        return copy.size() > MAX_MESSAGES ? List.copyOf(copy.subList(0, MAX_MESSAGES)) : copy;
    }
}

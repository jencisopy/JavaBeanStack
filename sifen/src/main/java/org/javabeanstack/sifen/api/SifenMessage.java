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

import java.io.Serializable;
import java.util.Objects;

/**
 * Un mensaje de resultado de SIFEN: código ({@code dCodRes}) y texto ({@code dMsgRes}) (contrato v1,
 * plan JBSSIF, §10.2). Inmutable y serializable.
 */
public final class SifenMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String code;
    private final String text;

    private SifenMessage(String code, String text) {
        this.code = code == null ? "" : code;
        this.text = text == null ? "" : text;
    }

    /**
     * Crea un mensaje.
     *
     * @param code código de SIFEN ({@code dCodRes}); {@code null} = vacío.
     * @param text texto de SIFEN ({@code dMsgRes}); {@code null} = vacío.
     * @return el mensaje.
     */
    public static SifenMessage of(String code, String text) {
        return new SifenMessage(code, text);
    }

    /**
     * Devuelve el código de SIFEN.
     *
     * @return el {@code dCodRes} (cuatro dígitos en el MT); nunca {@code null}.
     */
    public String getCode() {
        return code;
    }

    /**
     * Devuelve el texto de SIFEN.
     *
     * @return el {@code dMsgRes}; nunca {@code null}.
     */
    public String getText() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SifenMessage)) {
            return false;
        }
        SifenMessage other = (SifenMessage) o;
        return code.equals(other.code) && text.equals(other.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, text);
    }

    @Override
    public String toString() {
        return code + " " + text;
    }
}

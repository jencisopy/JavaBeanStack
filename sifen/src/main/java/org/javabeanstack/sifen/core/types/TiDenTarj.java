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
package org.javabeanstack.sifen.core.types;

import java.util.Arrays;

public enum TiDenTarj {
    VISA((short) 1, "Visa"),
    MASTERCARD((short) 2, "Mastercard"),
    AMERICAN_EXPRESS((short) 3, "American Express"),
    MAESTRO((short) 4, "Maestro"),
    PANAL((short) 5, "Panal"),
    CABAL((short) 6, "Cabal"),
    OTRO((short) 99, "Otro");

    private short val;
    private String descripcion;

    TiDenTarj(short val, String descripcion) {
        this.val = val;
        this.descripcion = descripcion;
    }

    public static TiDenTarj getByVal(short val) {
        return Arrays.stream(TiDenTarj.values()).filter(e -> e.val == val).findFirst().orElse(null);
    }

    public short getVal() {
        return val;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return "{\"val\": " + val + ", \"descripcion\": \"" + descripcion + "\"}";
    }
}

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

public enum TiAfecIVA {
    GRAVADO((short) 1, "Gravado IVA"),
    EXONERADO((short) 2, "Exonerado (Art. 83- Ley 125/91)"),
    EXENTO((short) 3, "Exento"),
    GRAVADO_PARCIAL((short) 4, "Gravado parcial (Grav- Exento)");

    private short val;
    private String descripcion;

    TiAfecIVA(short val, String descripcion) {
        this.val = val;
        this.descripcion = descripcion;
    }

    public static TiAfecIVA getByVal(short val) {
        return Arrays.stream(TiAfecIVA.values()).filter(e -> e.val == val).findFirst().orElse(null);
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
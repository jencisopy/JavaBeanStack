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

/**
 * Esta clase engloba los datos/tipos tiTipTra y tdDesTiTran
 */
public enum TTImp {
    IVA((short) 1, "IVA"),
    ISC((short) 2, "ISC"),
    RENTA((short) 3, "Renta"),
    NINGUNO((short) 4, "Ninguno"),
    IVA_RENTA((short) 5, "IVA - Renta");

    private short val;
    private String descripcion;

    TTImp(short val, String descripcion) {
        this.val = val;
        this.descripcion = descripcion;
    }

    public static TTImp getByVal(short val) {
        return Arrays.stream(TTImp.values()).filter(e -> e.val == val).findFirst().orElse(null);
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
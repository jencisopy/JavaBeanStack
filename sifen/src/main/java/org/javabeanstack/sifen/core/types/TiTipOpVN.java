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

public enum TiTipOpVN {
    VENTA_A_REPRESENTANTE((short) 1, "Venta a representante"),
    VENTA_AL_CONSUMIDOR_FINAL((short) 2, "Venta al Consumidor final"),
    VENTA_AL_GOBIERNO((short) 3, "Venta a gobierno"),
    VENTA_A_FLOTA_DE_VEHICULOS((short) 4, "Venta a flota de vehículos");

    private short val;
    private String descripcion;

    TiTipOpVN(short val, String descripcion) {
        this.val = val;
        this.descripcion = descripcion;
    }

    public static TiTipOpVN getByVal(short val) {
        return Arrays.stream(TiTipOpVN.values()).filter(e -> e.val == val).findFirst().orElse(null);
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
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

public enum TdMotEv {
    CAMBIO_LOCAL_ENTREGA((short) 1, "Cambio del local de la entrega"),
    CAMBIO_CHOFER((short) 2, "Cambio del chofer"),
    CAMBIO_TRANSPORTISTA((short) 3, "Cambio del transportista"),
    CAMBIO_VEHICULO((short) 4, "Cambio de vehículo");

    private short val;
    private String descripcion;

    TdMotEv(short val, String descripcion) {
        this.val = val;
        this.descripcion = descripcion;
    }

    public static TdMotEv getByVal(short val) {
        return Arrays.stream(TdMotEv.values()).filter(e -> e.val == val).findFirst().orElse(null);
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
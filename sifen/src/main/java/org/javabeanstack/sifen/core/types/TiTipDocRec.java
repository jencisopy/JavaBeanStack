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
 * Tipo de documento de identidad del receptor ({@code iTipIDRec}).
 *
 * <p>Delta del fork 2.0.0-SNAPSHOT (plan JBSSIF, §1.4.5; ficha 12 del informe de línea base):
 * la descripción de {@code OTRO} pasa de «Otro» a «Otro documento» (I0-09). Se porta tal cual
 * (D10).</p>
 */
public enum TiTipDocRec {
    CEDULA_PARAGUAYA((short) 1, "Cédula paraguaya"),
    PASAPORTE((short) 2, "Pasaporte"),
    CEDULA_EXTRANJERA((short) 3, "Cédula extranjera"),
    CARNET_DE_RESIDENCIA((short) 4, "Carnet de residencia"),
    INNOMINADO((short) 5, "Innominado"),
    TARJETA_DIPLOMATICA((short) 6, "Tarjeta Diplomática de exoneración fiscal"),
    OTRO((short) 9, "Otro documento");

    private short val;
    private String descripcion;

    TiTipDocRec(short val, String descripcion) {
        this.val = val;
        this.descripcion = descripcion;
    }

    public static TiTipDocRec getByVal(short val) {
        return Arrays.stream(TiTipDocRec.values()).filter(e -> e.val == val).findFirst().orElse(null);
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

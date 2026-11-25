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

public enum TTipReg {
    REGIMEN_TURISMO((short) 1, "Régimen de Turismo"),
    IMPORTADOR((short) 2, "Importador"),
    EXPORTADOR((short) 3, "Exportador"),
    MAQUILA((short) 4, "Maquila"),
    LEY_60_90((short) 5, "Ley Nº 60/90"),
    REGIMEN_PEQUENO_PRODUCTOR((short) 6, "Régimen del Pequeño Productor"),
    REGIMEN_MEDIANO_PRODUCTOR((short) 7, "Régimen del Mediano Productor"),
    REGIMEN_CONTABLE((short) 8, "Régimen Contable");

    private short val;
    private String descripcion;

    TTipReg(short val, String descripcion) {
        this.val = val;
        this.descripcion = descripcion;
    }

    public static TTipReg getByVal(short val) {
        return Arrays.stream(TTipReg.values()).filter(e -> e.val == val).findFirst().orElse(null);
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
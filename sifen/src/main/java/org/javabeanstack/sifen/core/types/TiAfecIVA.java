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
 * Afectación tributaria del IVA del ítem ({@code iAfecIVA} y su descripción {@code dDesAfecIVA}).
 *
 * <p>E12-07 (plan JBSSIF, Fase 12, 2026-11-25): la descripción de {@link #EXONERADO} es la del XSD vigente de la DNIT
 * ({@code tdDesAfecIVA}: «Exonerado (Art. 100 - Ley 6380/2019)»); la del XSD de 2019, «Exonerado (Art. 83- Ley
 * 125/91)», SIFEN la rechaza con 0160. La lectura de un XML toma solo el código ({@code iAfecIVA}), así que un DE
 * emitido con el texto viejo se sigue leyendo como exonerado.</p>
 */
public enum TiAfecIVA {
    GRAVADO((short) 1, "Gravado IVA"),
    EXONERADO((short) 2, "Exonerado (Art. 100 - Ley 6380/2019)"),
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
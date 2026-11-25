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
 * Tabla 12 del MT v150 — Tipo de obligaciones (NT 018): códigos {@code cOblAfe} (D031) y descripciones
 * {@code dDesOblAfe} (D032) del grupo {@code gOblAfe}, tal como los publica la NT (la validación D032 de SIFEN compara
 * la descripción con el código; plan JBSSIF, §6.1, I0-01).
 */
public enum TcOblAfe {
    IRACIS_REGIMENES_ESPECIALES((short) 113, "IMPUESTO A LA RENTA IRACIS - REGÍMENES ESPECIALES"),
    TRIBUTO_UNICO_MAQUILA((short) 143, "TRIBUTO UNICO MAQUILA"),
    IVA_GRAVADAS_EXONERADAS_EXPORTADORES((short) 211, "IMPUESTO AL VALOR AGREGADO - GRAVADAS Y EXONERADAS - EXPORTADORES"),
    ISC_GENERAL((short) 311, "IMPUESTO SELECTIVO AL CONSUMO - GENERAL"),
    ISC_COMBUSTIBLES((short) 321, "IMPUESTO SELECTIVO AL CONSUMO COMBUSTIBLES"),
    IRE_REGIMEN_GENERAL((short) 700, "IMPUESTO A LA RENTA EMPRESARIAL - RÉGIMEN GENERAL"),
    IRE_SIMPLE((short) 701, "IMPUESTO A LA RENTA EMPRESARIAL - SIMPLE"),
    IRE_RESIMPLE((short) 702, "IMPUESTO A LA RENTA EMPRESARIAL - RESIMPLE"),
    IMPUESTO_ZONA_FRANCA((short) 703, "IMPUESTO DE ZONA FRANCA"),
    IRP_SERVICIOS_PERSONALES((short) 715, "IMPUESTO A LA RENTA PERSONAL - SERVICIOS PERSONALES"),
    IRP_RENTAS_GANANCIAS_CAPITAL((short) 716, "IMPUESTO A LA RENTA PERSONAL - RENTAS Y GANANCIAS DE CAPITAL");

    private final short val;
    private final String descripcion;

    TcOblAfe(short val, String descripcion) {
        this.val = val;
        this.descripcion = descripcion;
    }

    /**
     * Busca la obligación por su código.
     *
     * @param val código de la Tabla 12.
     * @return la obligación, o {@code null} si el código no está en la tabla.
     */
    public static TcOblAfe getByVal(short val) {
        return Arrays.stream(TcOblAfe.values()).filter(e -> e.val == val).findFirst().orElse(null);
    }

    /**
     * Devuelve el código ({@code cOblAfe}).
     *
     * @return el código de la Tabla 12.
     */
    public short getVal() {
        return val;
    }

    /**
     * Devuelve la descripción ({@code dDesOblAfe}), la de la Tabla 12.
     *
     * @return la descripción.
     */
    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return "{\"val\": " + val + ", \"descripcion\": \"" + descripcion + "\"}";
    }
}

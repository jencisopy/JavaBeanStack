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
public enum TTipTra {
    VENTA_MERCADERIA((short) 1, "Venta de mercadería"),
    PRESTACION_SERVICIOS((short) 2, "Prestación de servicios"),
    MIXTO((short) 3, "Mixto (Venta de mercadería y servicios)"),
    VENTA_ACTIVO_FIJO((short) 4, "Venta de activo fijo"),
    VENTA_DIVISAS((short) 5, "Venta de divisas"),
    COMPRA_DIVISAS((short) 6, "Compra de divisas"),
    PROMOCION_O_MUESTRAS((short) 7, "Promoción o entrega de muestras"),
    DONACION((short) 8, "Donación"),
    ANTICIPO((short) 9, "Anticipo"),
    COMPRA_PRODUCTOS((short) 10, "Compra de productos"),
    COMPRA_SERVICIOS((short) 11, "Compra de servicios"),
    VENTA_CREDITO_FISCAL((short) 12, "Venta de crédito fiscal"),
    MUESTRAS_MEDICAS((short) 13, "Muestras médicas (Art. 3 RG 24/2014)");

    private short val;
    private String descripcion;

    TTipTra(short val, String descripcion) {
        this.val = val;
        this.descripcion = descripcion;
    }

    public static TTipTra getByVal(short val) {
        return Arrays.stream(TTipTra.values()).filter(e -> e.val == val).findFirst().orElse(null);
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
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

public enum TiTIpoDoc {
    FACTURA((short) 1, "Factura"),
    NOTA_DE_CREDITO((short) 2, "Nota de crédito"),
    NOTA_DE_DEBITO((short) 3, "Nota de débito"),
    NOTA_DE_REMISION((short) 4, "Nota de remisión"),
    COMPROBANTE_DE_RETENCION((short) 5, "Comprobante de retención");

    private short val;
    private String descripcion;

    TiTIpoDoc(short val, String descripcion) {
        this.val = val;
        this.descripcion = descripcion;
    }

    public static TiTIpoDoc getByVal(short val) {
        return Arrays.stream(TiTIpoDoc.values()).filter(e -> e.val == val).findFirst().orElse(null);
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
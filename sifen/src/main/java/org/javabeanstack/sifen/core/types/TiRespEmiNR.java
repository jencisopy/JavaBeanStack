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

public enum TiRespEmiNR {
    EMISOR_FACTURA((short) 1, "Emisor de la factura"),
    POSEEDOR_FACTURA_Y_BIENES((short) 2, "Poseedor de la factura y bienes"),
    EMPRESA_TRANSPORTISTA((short) 3, "Empresa transportista"),
    DESPACHANTE_DE_ADUANAS((short) 4, "Despachante de Aduanas"),
    AGENTE_DE_TRANSPORTE_O_INTERMEDIARIO((short) 5, "Agente de transporte o intermediario");

    private short val;
    private String descripcion;

    TiRespEmiNR(short val, String descripcion) {
        this.val = val;
        this.descripcion = descripcion;
    }

    public static TiRespEmiNR getByVal(short val) {
        return Arrays.stream(TiRespEmiNR.values()).filter(e -> e.val == val).findFirst().orElse(null);
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
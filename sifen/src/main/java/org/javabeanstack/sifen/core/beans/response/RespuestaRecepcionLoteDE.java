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
package org.javabeanstack.sifen.core.beans.response;

import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.response.BaseResponse;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import java.time.LocalDateTime;

/**
 * Clase principal que contiene la respuesta de Sifen al envío de lote de Documentos Electrónicos para aprobación.
 *
 * <p>Deprecada en favor de {@link org.javabeanstack.sifen.ISifenClient} (D42): se mantiene sin cambios de
 * firma en 2.x y se retira en 3.0; no recibe operaciones nuevas. Su reemplazo es {@link
 * org.javabeanstack.sifen.api.BatchSendResult}.</p>
 */
public class RespuestaRecepcionLoteDE extends BaseResponse {
    private LocalDateTime dFecProc;
    private String dProtConsLote;
    private int dTpoProces;

    /**
     * Método interno, no usar.
     */
    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "dFecProc":
                dFecProc = ResponseUtil.getDateTimeValue(value);
                break;
            case "dProtConsLote":
                dProtConsLote = ResponseUtil.getTextValue(value);
                break;
            case "dTpoProces":
                dTpoProces = Integer.parseInt(ResponseUtil.getTextValue(value));
                break;
            default:
                super.setValueFromChildNode(value);
        }
    }

    public LocalDateTime getdFecProc() {
        return dFecProc;
    }

    public String getdProtConsLote() {
        return dProtConsLote;
    }

    public int getdTpoProces() {
        return dTpoProces;
    }
}
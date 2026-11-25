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
import org.javabeanstack.sifen.core.fields.response.batch.TgResProcLote;
import org.javabeanstack.sifen.internal.response.BaseResponse;
import org.javabeanstack.sifen.internal.response.SifenObjectFactory;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase principal que contiene la respuesta de Sifen a la consulta de estado de un lote de Documentos Electrónicos.
 *
 * <p>Deprecada en favor de {@link org.javabeanstack.sifen.ISifenClient} (D42): se mantiene sin cambios de
 * firma en 2.x y se retira en 3.0; no recibe operaciones nuevas. Su reemplazo es {@link
 * org.javabeanstack.sifen.api.BatchQueryResult}.</p>
 */
public class RespuestaConsultaLoteDE extends BaseResponse {
    private LocalDateTime dFecProc;
    private String dCodResLot;
    private String dMsgResLot;
    private final List<TgResProcLote> gResProcLoteList = new ArrayList<>();

    /**
     * Método interno, no usar.
     */
    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "dFecProc":
                dFecProc = ResponseUtil.getDateTimeValue(value);
                break;
            case "dCodResLot":
                dCodResLot = ResponseUtil.getTextValue(value);
                break;
            case "dMsgResLot":
                dMsgResLot = ResponseUtil.getTextValue(value);
                break;
            case "gResProcLote":
                gResProcLoteList.add(SifenObjectFactory.getFromNode(value, TgResProcLote.class));
                break;
        }
    }

    public LocalDateTime getdFecProc() {
        return dFecProc;
    }

    public String getdCodResLot() {
        return dCodResLot;
    }

    public String getdMsgResLot() {
        return dMsgResLot;
    }

    public List<TgResProcLote> getgResProcLoteList() {
        return gResProcLoteList;
    }
}
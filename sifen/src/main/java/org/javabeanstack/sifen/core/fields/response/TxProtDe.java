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
package org.javabeanstack.sifen.core.fields.response;

import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.response.SifenObjectFactory;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Protocolo de procesamiento del DE ({@code xProtDE}) de la respuesta de SIFEN a la recepción de un DE.
 *
 * <p>Lo expone {@link org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionDE#getxProtDE()}, deprecada en
 * favor de {@link org.javabeanstack.sifen.ISifenClient} (D42): por eso se mantiene sin cambios de firma en 2.x y se
 * retira en 3.0 con ella; no recibe operaciones nuevas (ESC-33, I8-28). Su reemplazo es
 * {@link org.javabeanstack.sifen.api.SendResult}.</p>
 */
public class TxProtDe extends SifenObjectBase {
    private String id;
    private LocalDateTime dFecProc;
    private String dDigVal;
    private String dEstRes;
    private String dProtAut;
    private final List<TgResProc> gResProc = new ArrayList<>();

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "Id":
                id = ResponseUtil.getTextValue(value);
                break;
            case "dFecProc":
                dFecProc = ResponseUtil.getDateTimeValue(value);
                break;
            case "dDigVal":
                dDigVal = ResponseUtil.getTextValue(value);
                break;
            case "dEstRes":
                dEstRes = ResponseUtil.getTextValue(value);
                break;
            case "dProtAut":
                dProtAut = ResponseUtil.getTextValue(value);
                break;
            case "gResProc":
                gResProc.add(SifenObjectFactory.getFromNode(value, TgResProc.class));
                break;
        }
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getdFecProc() {
        return dFecProc;
    }

    public String getdDigVal() {
        return dDigVal;
    }

    public String getdEstRes() {
        return dEstRes;
    }

    public String getdProtAut() {
        return dProtAut;
    }

    public List<TgResProc> getgResProc() {
        return gResProc;
    }
}
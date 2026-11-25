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

import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

/**
 * Resultado de procesamiento ({@code gResProc}: código y mensaje) de las respuestas de SIFEN.
 *
 * <p>Lo exponen {@link org.javabeanstack.sifen.core.fields.response.TxProtDe#getgResProc()}, {@link
 * org.javabeanstack.sifen.core.fields.response.batch.TgResProcLote#getgResProc()} y {@link
 * org.javabeanstack.sifen.core.fields.response.event.TgResProcEVe#getgResProc()}, que cuelgan de los {@code
 * Respuesta*} deprecados en favor de {@link org.javabeanstack.sifen.ISifenClient} (D42): por eso sus firmas
 * públicas se mantienen sin cambios en toda la línea 2.x, sin {@code @Deprecated} (decisión del usuario ESC-36,
 * I8-35). No es contrato v1: la aplicación usa {@link org.javabeanstack.sifen.ISifenClient} y sus resultados de
 * {@code api}.</p>
 */
public class TgResProc extends SifenObjectBase {
    private String dCodRes;
    private String dMsgRes;

    @Override
    public void setValueFromChildNode(Node value) {
        if (value.getLocalName().equals("dCodRes")) {
            dCodRes = ResponseUtil.getTextValue(value);
        } else if (value.getLocalName().equals("dMsgRes")) {
            dMsgRes = ResponseUtil.getTextValue(value);
        }
    }

    public String getdCodRes() {
        return dCodRes;
    }

    public String getdMsgRes() {
        return dMsgRes;
    }
}
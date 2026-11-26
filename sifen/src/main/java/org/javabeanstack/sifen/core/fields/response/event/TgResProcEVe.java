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
package org.javabeanstack.sifen.core.fields.response.event;

import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.fields.response.TgResProc;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.response.SifenObjectFactory;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de procesamiento de un evento ({@code gResProcEVe}) en la respuesta de SIFEN a la recepción de
 * eventos.
 *
 * <p>Lo expone {@link org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionEvento#getgResProcEVe()},
 * deprecada en favor de {@link org.javabeanstack.sifen.ISifenClient} (D42): por eso sus firmas públicas se
 * mantienen sin cambios en toda la línea 2.x, sin {@code @Deprecated} (decisión del usuario ESC-36, I8-35). No es
 * contrato v1: la aplicación usa {@link org.javabeanstack.sifen.ISifenClient} y sus resultados de {@code api}. Su
 * reemplazo es {@link org.javabeanstack.sifen.api.EventResult}.</p>
 */
public class TgResProcEVe extends SifenObjectBase {
    private String dEstRes;
    private String dProtAut;
    private String id;
    private final List<TgResProc> gResProc = new ArrayList<>();

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "dEstRes":
                dEstRes = ResponseUtil.getTextValue(value);
                break;
            case "dProtAut":
                dProtAut = ResponseUtil.getTextValue(value);
                break;
            case "id":
                id = ResponseUtil.getTextValue(value);
                break;
            case "gResProc":
                gResProc.add(SifenObjectFactory.getFromNode(value, TgResProc.class));
                break;
        }
    }

    public String getdEstRes() {
        return dEstRes;
    }

    public String getdProtAut() {
        return dProtAut;
    }

    public String getId() {
        return id;
    }

    public List<TgResProc> getgResProc() {
        return gResProc;
    }
}
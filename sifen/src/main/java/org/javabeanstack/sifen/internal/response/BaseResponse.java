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
package org.javabeanstack.sifen.internal.response;

import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

/**
 * Clase abstracta heredada por las clases de respuestas a las peticiones.
 *
 * <p>Es de un paquete interno ({@code internal.*}: «no utilizar»), pero es la superclase de los
 * {@code core.beans.response.Respuesta*}, deprecados en favor de {@link org.javabeanstack.sifen.ISifenClient}
 * (D42), y sus miembros públicos se ven a través de ellos. Por eso, como excepción a lo interno, sus firmas
 * públicas se mantienen sin cambios en toda la línea 2.x, sin {@code @Deprecated} (decisión del usuario ESC-36,
 * I8-35). No es contrato v1: no se usa directamente.</p>
 */
public abstract class BaseResponse extends SifenObjectBase {
    private int codigoEstado;
    private String respuestaBruta;

    private String dCodRes;
    private String dMsgRes;
    private String requestSent;

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
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

    public int getCodigoEstado() {
        return codigoEstado;
    }

    public void setCodigoEstado(int codigoEstado) {
        this.codigoEstado = codigoEstado;
    }

    public String getRespuestaBruta() {
        return respuestaBruta;
    }

    public void setRespuestaBruta(String respuestaBruta) {
        this.respuestaBruta = respuestaBruta;
    }

    public void setdCodRes(String dCodRes) {
        this.dCodRes = dCodRes;
    }

    public void setdMsgRes(String dMsgRes) {
        this.dMsgRes = dMsgRes;
    }

    public String getRequestSent() {
        return requestSent;
    }

    public void setRequestSent(String requestSent) {
        this.requestSent = requestSent;
    }
}
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
package org.javabeanstack.sifen.core.fields.response.de;

import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.response.SifenObjectFactory;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Eventos del DE consultado ({@code xContEv}) en la respuesta de SIFEN a la consulta de un DE por CDC.
 *
 * <p>Lo expone {@link org.javabeanstack.sifen.core.fields.response.de.TxContenDE#getxContEv()}, que cuelga de
 * {@link org.javabeanstack.sifen.core.beans.response.RespuestaConsultaDE#getxContenDE()}, deprecada en favor de
 * {@link org.javabeanstack.sifen.ISifenClient} (D42): por eso sus firmas públicas se mantienen sin cambios en toda
 * la línea 2.x, sin {@code @Deprecated} (decisión del usuario ESC-36, I8-35). No es contrato v1: la aplicación usa
 * {@link org.javabeanstack.sifen.ISifenClient} y sus resultados de {@code api}. Su reemplazo es {@link
 * org.javabeanstack.sifen.api.DEQueryResult#getEvents()}.</p>
 */
public class TxContenEv extends SifenObjectBase {
    private final List<TrContEv> rContEvList = new ArrayList<>();

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        if (value.getLocalName().equals("rContEv")) {
            rContEvList.add(SifenObjectFactory.getFromNode(value, TrContEv.class));
        }
    }

    public List<TrContEv> getrContEvList() {
        return rContEvList;
    }
}
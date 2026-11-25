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
package org.javabeanstack.sifen.core.beans;

import org.javabeanstack.sifen.Provisional;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.fields.request.event.TrGesEve;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.response.SifenObjectFactory;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase que representa un conjunto de Eventos de Documentos Electrónicos: los eventos del emisor o del
 * receptor ({@code rGesEve}) para {@code ISifenClient.sendEvents} y {@code validateEvents}.
 *
 * <p><b>PROVISIONAL</b> en el contrato v1 (plan JBSSIF, decisión del usuario del 2026-11-24, F:I10-24): solo
 * lo usan esas dos operaciones, que ya lo son.</p>
 */
@Provisional
public class EventosDE extends SifenObjectBase {
    List<TrGesEve> rGesEveList;

    /**
     * Método interno, no usar.
     * @param gGroupGesEve -
     * @param sifenConfig -
     * @throws SOAPException -
     * @throws SifenException -
     */
    public void setupSOAPElements(SOAPElement gGroupGesEve, SifenConfig sifenConfig) throws SOAPException, SifenException {
        for (TrGesEve trGesEve : this.rGesEveList) {
            trGesEve.setupSOAPElements(gGroupGesEve, sifenConfig);
        }
    }

    /**
     * Método interno, no usar.
     * @param value -
     * @throws SifenException -
     */
    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        if (value.getLocalName().equals("rGesEve")) {
            if (this.rGesEveList == null) {
                this.rGesEveList = new ArrayList<>();
            }
            this.rGesEveList.add(SifenObjectFactory.getFromNode(value, TrGesEve.class));
        }
    }

    public List<TrGesEve> getrGesEveList() {
        return rGesEveList;
    }

    public void setrGesEveList(List<TrGesEve> rGesEveList) {
        this.rGesEveList = rGesEveList;
    }
}
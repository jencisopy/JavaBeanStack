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
package org.javabeanstack.sifen.core.fields.request.event;

import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.helpers.SignatureHelper;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.response.SifenObjectFactory;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.javabeanstack.sifen.internal.Constants.SIFEN_CURRENT_VERSION;

public class TrGesEve extends SifenObjectBase {
    private String Id;
    private LocalDateTime dFecFirma;
    private TgGroupTiEvt gGroupTiEvt;

    public void setupSOAPElements(SOAPElement gGroupGesEve, SifenConfig sifenConfig) throws SOAPException, SifenException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        SOAPElement rGesEve = gGroupGesEve.addChildElement("rGesEve");

        SOAPElement rEve = rGesEve.addChildElement("rEve");
        rEve.setAttribute("Id", this.Id);
        Attr idAttribute = rEve.getAttributeNode("Id");
        rEve.setIdAttributeNode(idAttribute, true);

        rEve.addChildElement("dFecFirma").setTextContent(this.dFecFirma.format(formatter));
        rEve.addChildElement("dVerFor").setTextContent(SIFEN_CURRENT_VERSION);
        this.gGroupTiEvt.setupSOAPElements(rEve);

        // Firma Digital del Evento
        SignatureHelper.signDocument(sifenConfig, rGesEve, this.Id);
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        if (value.getLocalName().equals("rEve")) {
            Element rEve = (Element) value;
            this.Id = rEve.getAttribute("Id");

            NodeList childNodes = value.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                if (node.getLocalName().equals("dFecFirma")) {
                    this.dFecFirma = ResponseUtil.getDateTimeValue(node);
                } else if (node.getLocalName().equals("gGroupTiEvt")) {
                    this.gGroupTiEvt = SifenObjectFactory.getFromNode(node, TgGroupTiEvt.class);
                }
            }
        }
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public LocalDateTime getdFecFirma() {
        return dFecFirma;
    }

    public void setdFecFirma(LocalDateTime dFecFirma) {
        this.dFecFirma = dFecFirma;
    }

    public TgGroupTiEvt getgGroupTiEvt() {
        return gGroupTiEvt;
    }

    public void setgGroupTiEvt(TgGroupTiEvt gGroupTiEvt) {
        this.gGroupTiEvt = gGroupTiEvt;
    }
}
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

import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.response.SifenObjectFactory;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TxContenDE extends SifenObjectBase {
    private DocumentoElectronico DE;
    private String dProtAut;
    private TxContenEv xContEv;

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "rDE":
                NodeList childNodes = value.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node node = childNodes.item(i);
                    if (node.getLocalName().equals("DE")) {
                        DE = SifenObjectFactory.getFromNode(node, DocumentoElectronico.class);
                        DE.obtenerCDC();
                    } else if (node.getLocalName().equals("gCamFuFD")) {
                        getQrLink(node);
                    }
                }
                break;
            case "dProtAut":
                dProtAut = ResponseUtil.getTextValue(value);
                break;
            case "xContEv":
                xContEv = SifenObjectFactory.getFromNode(value, TxContenEv.class);
                break;
        }
    }

    public DocumentoElectronico getDE() {
        return DE;
    }

    public String getdProtAut() {
        return dProtAut;
    }

    public TxContenEv getxContEv() {
        return xContEv;
    }

    private void getQrLink(Node gCamFuFD) {
        NodeList childNodes = gCamFuFD.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getLocalName().equals("dCarQR")) {
                DE.setEnlaceQR(ResponseUtil.getTextValue(node));
            }
        }
    }
}
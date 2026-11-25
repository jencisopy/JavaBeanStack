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
package org.javabeanstack.sifen.core.fields.request.de;

import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.types.TiMotEmi;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;

public class TgCamNCDE extends SifenObjectBase {
    private TiMotEmi iMotEmi;

    public void setupSOAPElements(SOAPElement gDtipDE) throws SOAPException {
        SOAPElement gCamNCDE = gDtipDE.addChildElement("gCamNCDE");
        gCamNCDE.addChildElement("iMotEmi").setTextContent(String.valueOf(this.iMotEmi.getVal()));
        gCamNCDE.addChildElement("dDesMotEmi").setTextContent(this.iMotEmi.getDescripcion());
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        if (value.getLocalName().equals("iMotEmi")) {
            this.iMotEmi = TiMotEmi.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
        }
    }

    public TiMotEmi getiMotEmi() {
        return iMotEmi;
    }

    public void setiMotEmi(TiMotEmi iMotEmi) {
        this.iMotEmi = iMotEmi;
    }
}
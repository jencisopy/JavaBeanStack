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
import org.javabeanstack.sifen.core.types.TTiDE;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.response.SifenObjectFactory;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;

public class TgCamGen extends SifenObjectBase {
    private String dOrdCompra;
    private String dOrdVta;
    private String dAsiento;
    private TgCamCarg gCamCarg;

    public void setupSOAPElements(SOAPElement DE, TTiDE iTiDE) throws SOAPException {
        SOAPElement gCamGen = DE.addChildElement("gCamGen");
        if (this.dOrdCompra != null)
            gCamGen.addChildElement("dOrdCompra").setTextContent(this.dOrdCompra);

        if (this.dOrdVta != null)
            gCamGen.addChildElement("dOrdVta").setTextContent(this.dOrdVta);

        if (this.dAsiento != null)
            gCamGen.addChildElement("dAsiento").setTextContent(this.dAsiento);

        if ((iTiDE.getVal() == 1 || iTiDE.getVal() == 7) && this.gCamCarg != null)
            this.gCamCarg.setupSOAPElements(gCamGen);
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "dOrdCompra":
                this.dOrdCompra = ResponseUtil.getTextValue(value);
                break;
            case "dOrdVta":
                this.dOrdVta = ResponseUtil.getTextValue(value);
                break;
            case "dAsiento":
                this.dAsiento = ResponseUtil.getTextValue(value);
                break;
            case "gCamCarg":
                this.gCamCarg = SifenObjectFactory.getFromNode(value, TgCamCarg.class);
                break;
        }
    }

    public String getdOrdCompra() {
        return dOrdCompra;
    }

    public void setdOrdCompra(String dOrdCompra) {
        this.dOrdCompra = dOrdCompra;
    }

    public String getdOrdVta() {
        return dOrdVta;
    }

    public void setdOrdVta(String dOrdVta) {
        this.dOrdVta = dOrdVta;
    }

    public String getdAsiento() {
        return dAsiento;
    }

    public void setdAsiento(String dAsiento) {
        this.dAsiento = dAsiento;
    }

    public TgCamCarg getgCamCarg() {
        return gCamCarg;
    }

    public void setgCamCarg(TgCamCarg gCamCarg) {
        this.gCamCarg = gCamCarg;
    }
}

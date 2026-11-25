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
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.response.SifenObjectFactory;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;

public class TgCamEsp extends SifenObjectBase {
    private TgGrupEner gGrupEner;
    private TgGrupSeg gGrupSeg;
    private TgGrupSup gGrupSup;
    private TgGrupAdi gGrupAdi;

    public void setupSOAPElements(SOAPElement gDtipDE) throws SOAPException {
        SOAPElement gCamEsp = gDtipDE.addChildElement("gCamEsp");
        if (this.gGrupEner != null)
            this.gGrupEner.setupSOAPElements(gCamEsp);

        if (this.gGrupSeg != null)
            this.gGrupSeg.setupSOAPElements(gCamEsp);

        if (this.gGrupSup != null)
            this.gGrupSup.setupSOAPElements(gCamEsp);

        if (this.gGrupAdi != null)
            this.gGrupAdi.setupSOAPElements(gCamEsp);
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "gGrupEner":
                this.gGrupEner = SifenObjectFactory.getFromNode(value, TgGrupEner.class);
                break;
            case "gGrupSeg":
                this.gGrupSeg = SifenObjectFactory.getFromNode(value, TgGrupSeg.class);
                break;
            case "gGrupSup":
                this.gGrupSup = SifenObjectFactory.getFromNode(value, TgGrupSup.class);
                break;
            case "gGrupAdi":
                this.gGrupAdi = SifenObjectFactory.getFromNode(value, TgGrupAdi.class);
                break;
        }
    }

    public TgGrupEner getgGrupEner() {
        return gGrupEner;
    }

    public void setgGrupEner(TgGrupEner gGrupEner) {
        this.gGrupEner = gGrupEner;
    }

    public TgGrupSeg getgGrupSeg() {
        return gGrupSeg;
    }

    public void setgGrupSeg(TgGrupSeg gGrupSeg) {
        this.gGrupSeg = gGrupSeg;
    }

    public TgGrupSup getgGrupSup() {
        return gGrupSup;
    }

    public void setgGrupSup(TgGrupSup gGrupSup) {
        this.gGrupSup = gGrupSup;
    }

    public TgGrupAdi getgGrupAdi() {
        return gGrupAdi;
    }

    public void setgGrupAdi(TgGrupAdi gGrupAdi) {
        this.gGrupAdi = gGrupAdi;
    }
}
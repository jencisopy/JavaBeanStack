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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TdDatGralOpe extends SifenObjectBase {
    private LocalDateTime dFeEmiDE;
    private TgOpeCom gOpeCom;
    private TgEmis gEmis;
    private TgDatRec gDatRec;

    public void setupSOAPElements(SOAPElement DE, TTiDE iTiDE) throws SOAPException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        SOAPElement gDatGralOpe = DE.addChildElement("gDatGralOpe");
        gDatGralOpe.addChildElement("dFeEmiDE").setTextContent(this.dFeEmiDE.format(formatter));
        if (iTiDE.getVal() != 7)
            this.gOpeCom.setupSOAPElements(gDatGralOpe, iTiDE);
        this.gEmis.setupSOAPElements(gDatGralOpe);
        this.gDatRec.setupSOAPElements(gDatGralOpe, iTiDE);
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "dFeEmiDE":
                this.dFeEmiDE = ResponseUtil.getDateTimeValue(value);
                break;
            case "gOpeCom":
                this.gOpeCom = SifenObjectFactory.getFromNode(value, TgOpeCom.class);
                break;
            case "gEmis":
                this.gEmis = SifenObjectFactory.getFromNode(value, TgEmis.class);
                break;
            case "gDatRec":
                this.gDatRec = SifenObjectFactory.getFromNode(value, TgDatRec.class);
                break;
        }
    }

    public LocalDateTime getdFeEmiDE() {
        return dFeEmiDE;
    }

    public void setdFeEmiDE(LocalDateTime dFeEmiDE) {
        this.dFeEmiDE = dFeEmiDE;
    }

    public TgOpeCom getgOpeCom() {
        return gOpeCom;
    }

    public void setgOpeCom(TgOpeCom gOpeCom) {
        this.gOpeCom = gOpeCom;
    }

    public TgEmis getgEmis() {
        return gEmis;
    }

    public void setgEmis(TgEmis gEmis) {
        this.gEmis = gEmis;
    }

    public TgDatRec getgDatRec() {
        return gDatRec;
    }

    public void setgDatRec(TgDatRec gDatRec) {
        this.gDatRec = gDatRec;
    }
}
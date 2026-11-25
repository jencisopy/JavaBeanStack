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
import org.javabeanstack.sifen.core.types.TTipEmi;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.javabeanstack.sifen.internal.util.SifenUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;

public class TgOpeDE extends SifenObjectBase {
    private TTipEmi iTipEmi;
    private String dCodSeg;         // número aleatorio entre 1 y 999999999 generado por el emisor
    private String dInfoEmi;        // información de interés del emisor (opcional). Entre 1 y 3000 carácteres
    private String dInfoFisc;       // información de interés del fisco (opcional). Entre 1 y 3000 carácteres

    public TgOpeDE() {
        this.dCodSeg = SifenUtil.generateRandomNumber();
    }

    public void setupSOAPElements(SOAPElement DE, TTiDE iTiDE) throws SOAPException {
        SOAPElement gOpeDE = DE.addChildElement("gOpeDE");
        gOpeDE.addChildElement("iTipEmi").setTextContent(String.valueOf(this.iTipEmi.getVal()));
        gOpeDE.addChildElement("dDesTipEmi").setTextContent(this.iTipEmi.getDescripcion());
        gOpeDE.addChildElement("dCodSeg").setTextContent(this.dCodSeg);
        if (this.dInfoEmi != null)
            gOpeDE.addChildElement("dInfoEmi").setTextContent(this.dInfoEmi);
        if (this.dInfoFisc != null || iTiDE.getVal() == 7)
            gOpeDE.addChildElement("dInfoFisc").setTextContent(this.dInfoFisc);
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "iTipEmi":
                this.iTipEmi = TTipEmi.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "dCodSeg":
                this.dCodSeg = ResponseUtil.getTextValue(value);
                break;
            case "dInfoEmi":
                this.dInfoEmi = ResponseUtil.getTextValue(value);
                break;
            case "dInfoFisc":
                this.dInfoFisc = ResponseUtil.getTextValue(value);
                break;
        }
    }

    public TTipEmi getiTipEmi() {
        return iTipEmi;
    }

    public void setiTipEmi(TTipEmi iTipEmi) {
        this.iTipEmi = iTipEmi;
    }

    public String getdCodSeg() {
        return dCodSeg;
    }

    public void setdCodSeg(String dCodSeg) {
        this.dCodSeg = dCodSeg;
    }

    public String getdInfoEmi() {
        return dInfoEmi;
    }

    public void setdInfoEmi(String dInfoEmi) {
        this.dInfoEmi = dInfoEmi;
    }

    public String getdInfoFisc() {
        return dInfoFisc;
    }

    public void setdInfoFisc(String dInfoFisc) {
        this.dInfoFisc = dInfoFisc;
    }
}
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
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.javabeanstack.sifen.internal.util.SifenUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.time.LocalDate;

public class TgTimb extends SifenObjectBase {
    private TTiDE iTiDE;        // este campo engloba a iTiDE y a dDesTiDE
    private int dNumTim;        // número de timbrado
    private String dEst;        // código de establecimiento: patron ej: 001
    private String dPunExp;     // punto de expedición: patron ej: 001
    private String dNumDoc;     // número de documento: patron ej: 0192312
    private String dSerieNum;   // número de serie del timbrado (opcional)
    private LocalDate dFeIniT;  // fecha de inicio de vigencia del timbrado

    public void setupSOAPElements(SOAPElement DE) throws SOAPException {
        SOAPElement gTimb = DE.addChildElement("gTimb");
        gTimb.addChildElement("iTiDE").setTextContent(String.valueOf(this.iTiDE.getVal()));
        gTimb.addChildElement("dDesTiDE").setTextContent(this.iTiDE.getDescripcion());
        gTimb.addChildElement("dNumTim").setTextContent(String.valueOf(this.dNumTim));
        gTimb.addChildElement("dEst").setTextContent(this.dEst);
        gTimb.addChildElement("dPunExp").setTextContent(this.dPunExp);
        gTimb.addChildElement("dNumDoc").setTextContent(this.dNumDoc);
        if (this.dSerieNum != null)
            gTimb.addChildElement("dSerieNum").setTextContent(this.dSerieNum);
        gTimb.addChildElement("dFeIniT").setTextContent(this.dFeIniT.toString());
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "iTiDE":
                this.iTiDE = TTiDE.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "dNumTim":
                this.dNumTim = Integer.parseInt(ResponseUtil.getTextValue(value));
                break;
            case "dEst":
                this.dEst = ResponseUtil.getTextValue(value);
                break;
            case "dPunExp":
                this.dPunExp = ResponseUtil.getTextValue(value);
                break;
            case "dNumDoc":
                this.dNumDoc = ResponseUtil.getTextValue(value);
                break;
            case "dSerieNum":
                this.dSerieNum = ResponseUtil.getTextValue(value);
                break;
            case "dFeIniT":
                this.dFeIniT = ResponseUtil.getDateValue(value);
                break;
        }
    }

    public TTiDE getiTiDE() {
        return iTiDE;
    }

    public void setiTiDE(TTiDE iTiDE) {
        this.iTiDE = iTiDE;
    }

    public int getdNumTim() {
        return dNumTim;
    }

    public void setdNumTim(int dNumTim) {
        this.dNumTim = dNumTim;
    }

    public String getdEst() {
        return dEst;
    }

    public void setdEst(String dEst) {
        this.dEst = SifenUtil.leftPad(String.valueOf(dEst), '0', 3);
    }

    public String getdPunExp() {
        return dPunExp;
    }

    public void setdPunExp(String dPunExp) {
        this.dPunExp = SifenUtil.leftPad(String.valueOf(dPunExp), '0', 3);
    }

    public String getdNumDoc() {
        return dNumDoc;
    }

    public void setdNumDoc(String dNumDoc) {
        this.dNumDoc = SifenUtil.leftPad(String.valueOf(dNumDoc), '0', 7);
    }

    public String getdSerieNum() {
        return dSerieNum;
    }

    public void setdSerieNum(String dSerieNum) {
        this.dSerieNum = dSerieNum;
    }

    public LocalDate getdFeIniT() {
        return dFeIniT;
    }

    public void setdFeIniT(LocalDate dFeIniT) {
        this.dFeIniT = dFeIniT;
    }
}
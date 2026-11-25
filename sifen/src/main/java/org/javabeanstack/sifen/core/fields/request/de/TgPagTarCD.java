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

import org.javabeanstack.sifen.internal.util.SifenUtil;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.types.TiDenTarj;
import org.javabeanstack.sifen.core.types.TiForProPa;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;

public class TgPagTarCD extends SifenObjectBase {
    private TiDenTarj iDenTarj;
    private String dDesDenTarj;
    private String dRSProTar;
    private String dRUCProTar;
    private short dDVProTar;
    private TiForProPa iForProPa;
    private long dCodAuOpe;
    private String dNomTit;
    private short dNumTarj;

    public void setupSOAPElements(SOAPElement gPaConEIni) throws SOAPException {
        SOAPElement gPagTarCD = gPaConEIni.addChildElement("gPagTarCD");
        gPagTarCD.addChildElement("iDenTarj").setTextContent(String.valueOf(this.iDenTarj.getVal()));
        // E3-23: con 99 («Otro», E622) el texto libre del consumidor; la línea base lo ignoraba
        gPagTarCD.addChildElement("dDesDenTarj").setTextContent(SifenUtil.describeWithOther(this.iDenTarj.getVal() == 99, this.iDenTarj.getDescripcion(), this.dDesDenTarj));

        if (this.dRSProTar != null)
            gPagTarCD.addChildElement("dRSProTar").setTextContent(this.dRSProTar);

        if (this.dRUCProTar != null)
            gPagTarCD.addChildElement("dRUCProTar").setTextContent(this.dRUCProTar);

        if (this.dDVProTar != 0)
            gPagTarCD.addChildElement("dDVProTar").setTextContent(String.valueOf(this.dDVProTar));

        gPagTarCD.addChildElement("iForProPa").setTextContent(String.valueOf(this.iForProPa.getVal()));

        if (this.dCodAuOpe != 0)
            gPagTarCD.addChildElement("dCodAuOpe").setTextContent(String.valueOf(this.dCodAuOpe));

        if (this.dNomTit != null)
            gPagTarCD.addChildElement("dNomTit").setTextContent(this.dNomTit);

        if (this.dNumTarj != 0)
            gPagTarCD.addChildElement("dNumTarj").setTextContent(String.valueOf(this.dNumTarj));
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "iDenTarj":
                this.iDenTarj = TiDenTarj.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "dDesDenTarj":
                this.dDesDenTarj = ResponseUtil.getTextValue(value);
                break;
            case "dRSProTar":
                this.dRSProTar = ResponseUtil.getTextValue(value);
                break;
            case "dRUCProTar":
                this.dRUCProTar = ResponseUtil.getTextValue(value);
                break;
            case "dDVProTar":
                this.dDVProTar = Short.parseShort(ResponseUtil.getTextValue(value));
                break;
            case "iForProPa":
                this.iForProPa = TiForProPa.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "dCodAuOpe":
                this.dCodAuOpe = Long.parseLong(ResponseUtil.getTextValue(value));
                break;
            case "dNomTit":
                this.dNomTit = ResponseUtil.getTextValue(value);
                break;
            case "dNumTarj":
                this.dNumTarj = Short.parseShort(ResponseUtil.getTextValue(value));
                break;
        }
    }

    public TiDenTarj getiDenTarj() {
        return iDenTarj;
    }

    public void setiDenTarj(TiDenTarj iDenTarj) {
        this.iDenTarj = iDenTarj;
    }

    public String getdDesDenTarj() {
        return dDesDenTarj;
    }

    public void setdDesDenTarj(String dDesDenTarj) {
        this.dDesDenTarj = dDesDenTarj;
    }

    public String getdRSProTar() {
        return dRSProTar;
    }

    public void setdRSProTar(String dRSProTar) {
        this.dRSProTar = dRSProTar;
    }

    public String getdRUCProTar() {
        return dRUCProTar;
    }

    public void setdRUCProTar(String dRUCProTar) {
        this.dRUCProTar = dRUCProTar;
    }

    public short getdDVProTar() {
        return dDVProTar;
    }

    public void setdDVProTar(short dDVProTar) {
        this.dDVProTar = dDVProTar;
    }

    public TiForProPa getiForProPa() {
        return iForProPa;
    }

    public void setiForProPa(TiForProPa iForProPa) {
        this.iForProPa = iForProPa;
    }

    public long getdCodAuOpe() {
        return dCodAuOpe;
    }

    public void setdCodAuOpe(int dCodAuOpe) {
        this.dCodAuOpe = dCodAuOpe;
    }

    public String getdNomTit() {
        return dNomTit;
    }

    public void setdNomTit(String dNomTit) {
        this.dNomTit = dNomTit;
    }

    public short getdNumTarj() {
        return dNumTarj;
    }

    public void setdNumTarj(short dNumTarj) {
        this.dNumTarj = dNumTarj;
    }
}

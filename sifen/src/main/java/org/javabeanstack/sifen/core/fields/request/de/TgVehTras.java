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
import org.javabeanstack.sifen.core.types.TiModTrans;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;

public class TgVehTras extends SifenObjectBase {
    private String dTiVehTras;
    private String dMarVeh;
    private short dTipIdenVeh;
    private String dNroIDVeh;
    private String dAdicVeh;
    private String dNroMatVeh;
    private String dNroVuelo;

    public void setupSOAPElements(SOAPElement gTransp, TiModTrans iModTrans) throws SOAPException {
        SOAPElement gVehTras = gTransp.addChildElement("gVehTras");
        gVehTras.addChildElement("dTiVehTras").setTextContent(this.dTiVehTras);
        gVehTras.addChildElement("dMarVeh").setTextContent(this.dMarVeh);
        gVehTras.addChildElement("dTipIdenVeh").setTextContent(String.valueOf(this.dTipIdenVeh));

        if (this.dTipIdenVeh == 1)
            gVehTras.addChildElement("dNroIDVeh").setTextContent(this.dNroIDVeh);

        if (this.dAdicVeh != null)
            gVehTras.addChildElement("dAdicVeh").setTextContent(this.dAdicVeh);

        if (this.dTipIdenVeh == 2)
            gVehTras.addChildElement("dNroMatVeh").setTextContent(this.dNroMatVeh);

        if (iModTrans.getVal() == 3)
            gVehTras.addChildElement("dNroVuelo").setTextContent(this.dNroVuelo);
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "dTiVehTras":
                this.dTiVehTras = ResponseUtil.getTextValue(value);
                break;
            case "dMarVeh":
                this.dMarVeh = ResponseUtil.getTextValue(value);
                break;
            case "dTipIdenVeh":
                this.dTipIdenVeh = Short.parseShort(ResponseUtil.getTextValue(value));
                break;
            case "dNroIDVeh":
                this.dNroIDVeh = ResponseUtil.getTextValue(value);
                break;
            case "dAdicVeh":
                this.dAdicVeh = ResponseUtil.getTextValue(value);
                break;
            case "dNroMatVeh":
                this.dNroMatVeh = ResponseUtil.getTextValue(value);
                break;
            case "dNroVuelo":
                this.dNroVuelo = ResponseUtil.getTextValue(value);
                break;
        }
    }

    public String getdTiVehTras() {
        return dTiVehTras;
    }

    public void setdTiVehTras(String dTiVehTras) {
        this.dTiVehTras = dTiVehTras;
    }

    public String getdMarVeh() {
        return dMarVeh;
    }

    public void setdMarVeh(String dMarVeh) {
        this.dMarVeh = dMarVeh;
    }

    public short getdTipIdenVeh() {
        return dTipIdenVeh;
    }

    public void setdTipIdenVeh(short dTipIdenVeh) {
        this.dTipIdenVeh = dTipIdenVeh;
    }

    public String getdNroIDVeh() {
        return dNroIDVeh;
    }

    public void setdNroIDVeh(String dNroIDVeh) {
        this.dNroIDVeh = dNroIDVeh;
    }

    public String getdAdicVeh() {
        return dAdicVeh;
    }

    public void setdAdicVeh(String dAdicVeh) {
        this.dAdicVeh = dAdicVeh;
    }

    public String getdNroMatVeh() {
        return dNroMatVeh;
    }

    public void setdNroMatVeh(String dNroMatVeh) {
        this.dNroMatVeh = dNroMatVeh;
    }

    public String getdNroVuelo() {
        return dNroVuelo;
    }

    public void setdNroVuelo(String dNroVuelo) {
        this.dNroVuelo = dNroVuelo;
    }
}

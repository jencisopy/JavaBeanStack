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
import org.javabeanstack.sifen.core.types.TiTipIDRespDE;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.javabeanstack.sifen.internal.util.SifenUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;

public class TgRespDE extends SifenObjectBase {
    private TiTipIDRespDE iTipIDRespDE;
    private String dDTipIDRespDE;
    private String dNumIDRespDE;
    private String dNomRespDE;
    private String dCarRespDE;

    public void setupSOAPElements(SOAPElement gEmis) throws SOAPException {
        SOAPElement gRespDE = gEmis.addChildElement("gRespDE");
        gRespDE.addChildElement("iTipIDRespDE").setTextContent(String.valueOf(this.iTipIDRespDE.getVal()));
        // E3-23: con 9 («Otro») el texto libre del consumidor
        gRespDE.addChildElement("dDTipIDRespDE").setTextContent(SifenUtil.describeWithOther(this.iTipIDRespDE.getVal() == 9, this.iTipIDRespDE.getDescripcion(), this.dDTipIDRespDE));
        gRespDE.addChildElement("dNumIDRespDE").setTextContent(this.dNumIDRespDE);
        gRespDE.addChildElement("dNomRespDE").setTextContent(this.dNomRespDE);
        gRespDE.addChildElement("dCarRespDE").setTextContent(this.dCarRespDE);
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "iTipIDRespDE":
                this.iTipIDRespDE = TiTipIDRespDE.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "dDTipIDRespDE":
                this.dDTipIDRespDE = ResponseUtil.getTextValue(value);
                break;
            case "dNumIDRespDE":
                this.dNumIDRespDE = ResponseUtil.getTextValue(value);
                break;
            case "dNomRespDE":
                this.dNomRespDE = ResponseUtil.getTextValue(value);
                break;
            case "dCarRespDE":
                this.dCarRespDE = ResponseUtil.getTextValue(value);
                break;
        }
    }

    public TiTipIDRespDE getiTipIDRespDE() {
        return iTipIDRespDE;
    }

    public void setiTipIDRespDE(TiTipIDRespDE iTipIDRespDE) {
        this.iTipIDRespDE = iTipIDRespDE;
    }

    public String getdDTipIDRespDE() {
        return dDTipIDRespDE;
    }

    public void setdDTipIDRespDE(String dDTipIDRespDE) {
        this.dDTipIDRespDE = dDTipIDRespDE;
    }

    public String getdNumIDRespDE() {
        return dNumIDRespDE;
    }

    public void setdNumIDRespDE(String dNumIDRespDE) {
        this.dNumIDRespDE = dNumIDRespDE;
    }

    public String getdNomRespDE() {
        return dNomRespDE;
    }

    public void setdNomRespDE(String dNomRespDE) {
        this.dNomRespDE = dNomRespDE;
    }

    public String getdCarRespDE() {
        return dCarRespDE;
    }

    public void setdCarRespDE(String dCarRespDE) {
        this.dCarRespDE = dCarRespDE;
    }
}
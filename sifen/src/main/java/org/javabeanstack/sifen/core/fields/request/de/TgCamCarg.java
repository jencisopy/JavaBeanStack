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
import org.javabeanstack.sifen.core.types.TcUniMed;
import org.javabeanstack.sifen.core.types.TiCarCarga;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.javabeanstack.sifen.internal.util.SifenUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.math.BigInteger;

public class TgCamCarg extends SifenObjectBase {
    private TcUniMed cUniMedTotVol;
    private BigInteger dTotVolMerc;
    private TcUniMed cUniMedTotPes;
    private BigInteger dTotPesMerc;
    private TiCarCarga iCarCarga;
    private String dDesCarCarga;

    public void setupSOAPElements(SOAPElement gCamGen) throws SOAPException {
        SOAPElement gCamCarg = gCamGen.addChildElement("gCamCarg");
        if (this.cUniMedTotVol != null) {
            gCamCarg.addChildElement("cUniMedTotVol").setTextContent(String.valueOf(this.cUniMedTotVol.getVal()));
            gCamCarg.addChildElement("dDesUniMedTotVol").setTextContent(this.cUniMedTotVol.getAbreviatura());
        }
        if (this.dTotVolMerc != null)
            gCamCarg.addChildElement("dTotVolMerc").setTextContent(String.valueOf(this.dTotVolMerc));

        if (this.cUniMedTotPes != null) {
            gCamCarg.addChildElement("cUniMedTotPes").setTextContent(String.valueOf(this.cUniMedTotPes.getVal()));
            gCamCarg.addChildElement("dDesUniMedTotPes").setTextContent(this.cUniMedTotPes.getAbreviatura());
        }
        if (this.dTotPesMerc != null)
            gCamCarg.addChildElement("dTotPesMerc").setTextContent(String.valueOf(this.dTotPesMerc));

        if (this.iCarCarga != null) {
            gCamCarg.addChildElement("iCarCarga").setTextContent(String.valueOf(this.iCarCarga.getVal()));
            // E3-23: con 3 («Otro») el texto libre del consumidor
            gCamCarg.addChildElement("dDesCarCarga").setTextContent(SifenUtil.describeWithOther(this.iCarCarga.getVal() == 3, this.iCarCarga.getDescripcion(), this.dDesCarCarga));
        }
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "cUniMedTotVol":
                this.cUniMedTotVol = TcUniMed.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "dTotVolMerc":
                this.dTotVolMerc = new BigInteger(ResponseUtil.getTextValue(value));
                break;
            case "cUniMedTotPes":
                this.cUniMedTotPes = TcUniMed.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "dTotPesMerc":
                this.dTotPesMerc = new BigInteger(ResponseUtil.getTextValue(value));
                break;
            case "iCarCarga":
                this.iCarCarga = TiCarCarga.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "dDesCarCarga":
                this.dDesCarCarga = ResponseUtil.getTextValue(value);
                break;
        }
    }

    public TcUniMed getcUniMedTotVol() {
        return cUniMedTotVol;
    }

    public void setcUniMedTotVol(TcUniMed cUniMedTotVol) {
        this.cUniMedTotVol = cUniMedTotVol;
    }

    public BigInteger getdTotVolMerc() {
        return dTotVolMerc;
    }

    public void setdTotVolMerc(BigInteger dTotVolMerc) {
        this.dTotVolMerc = dTotVolMerc;
    }

    public TcUniMed getcUniMedTotPes() {
        return cUniMedTotPes;
    }

    public void setcUniMedTotPes(TcUniMed cUniMedTotPes) {
        this.cUniMedTotPes = cUniMedTotPes;
    }

    public BigInteger getdTotPesMerc() {
        return dTotPesMerc;
    }

    public void setdTotPesMerc(BigInteger dTotPesMerc) {
        this.dTotPesMerc = dTotPesMerc;
    }

    public TiCarCarga getiCarCarga() {
        return iCarCarga;
    }

    public void setiCarCarga(TiCarCarga iCarCarga) {
        this.iCarCarga = iCarCarga;
    }

    public String getdDesCarCarga() {
        return dDesCarCarga;
    }

    public void setdDesCarCarga(String dDesCarCarga) {
        this.dDesCarCarga = dDesCarCarga;
    }

}

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
import org.javabeanstack.sifen.core.types.CMondT;
import org.javabeanstack.sifen.core.types.TiTiPago;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.response.SifenObjectFactory;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.math.BigDecimal;

public class TgPaConEIni extends SifenObjectBase {
    private TiTiPago iTiPago;
    private String dDesTiPag;
    private BigDecimal dMonTiPag;
    private CMondT cMoneTiPag;
    private BigDecimal dTiCamTiPag;
    private TgPagTarCD gPagTarCD;
    private TgPagCheq gPagCheq;

    public void setupSOAPElements(SOAPElement gCamCond) throws SOAPException {
        SOAPElement gPaConEIni = gCamCond.addChildElement("gPaConEIni");
        gPaConEIni.addChildElement("iTiPago").setTextContent(String.valueOf(this.iTiPago.getVal()));
        // E3-23: con 99 («Otro», E607) el texto libre del consumidor; la línea base lo ignoraba
        gPaConEIni.addChildElement("dDesTiPag").setTextContent(SifenUtil.describeWithOther(this.iTiPago.getVal() == 99, this.iTiPago.getDescripcion(), this.dDesTiPag));
        gPaConEIni.addChildElement("dMonTiPag").setTextContent(String.valueOf(this.dMonTiPag));
        gPaConEIni.addChildElement("cMoneTiPag").setTextContent(this.cMoneTiPag.name());
        gPaConEIni.addChildElement("dDMoneTiPag").setTextContent(this.cMoneTiPag.getDescripcion());

        if (this.dTiCamTiPag != null || !this.cMoneTiPag.name().equals("PYG"))
            gPaConEIni.addChildElement("dTiCamTiPag").setTextContent(String.valueOf(this.dTiCamTiPag));

        if (this.iTiPago.getVal() == 3 || this.iTiPago.getVal() == 4)
            this.gPagTarCD.setupSOAPElements(gPaConEIni);
        else if (this.iTiPago.getVal() == 2)
            this.gPagCheq.setupSOAPElements(gPaConEIni);
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "iTiPago":
                this.iTiPago = TiTiPago.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "dDesTiPag":
                this.dDesTiPag = ResponseUtil.getTextValue(value);
                break;
            case "dMonTiPag":
                this.dMonTiPag = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "cMoneTiPag":
                this.cMoneTiPag = CMondT.getByName(ResponseUtil.getTextValue(value));
                break;
            case "dTiCamTiPag":
                this.dTiCamTiPag = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "gPagTarCD":
                this.gPagTarCD = SifenObjectFactory.getFromNode(value, TgPagTarCD.class);
                break;
            case "gPagCheq":
                this.gPagCheq = SifenObjectFactory.getFromNode(value, TgPagCheq.class);
                break;
        }
    }

    public TiTiPago getiTiPago() {
        return iTiPago;
    }

    public void setiTiPago(TiTiPago iTiPago) {
        this.iTiPago = iTiPago;
    }

    public String getdDesTiPag() {
        return dDesTiPag;
    }

    public void setdDesTiPag(String dDesTiPag) {
        this.dDesTiPag = dDesTiPag;
    }

    public BigDecimal getdMonTiPag() {
        return dMonTiPag;
    }

    public void setdMonTiPag(BigDecimal dMonTiPag) {
        this.dMonTiPag = dMonTiPag;
    }

    public CMondT getcMoneTiPag() {
        return cMoneTiPag;
    }

    public void setcMoneTiPag(CMondT cMoneTiPag) {
        this.cMoneTiPag = cMoneTiPag;
    }

    public BigDecimal getdTiCamTiPag() {
        return dTiCamTiPag;
    }

    public void setdTiCamTiPag(BigDecimal dTiCamTiPag) {
        this.dTiCamTiPag = dTiCamTiPag;
    }

    public TgPagTarCD getgPagTarCD() {
        return gPagTarCD;
    }

    public void setgPagTarCD(TgPagTarCD gPagTarCD) {
        this.gPagTarCD = gPagTarCD;
    }

    public TgPagCheq getgPagCheq() {
        return gPagCheq;
    }

    public void setgPagCheq(TgPagCheq gPagCheq) {
        this.gPagCheq = gPagCheq;
    }
}
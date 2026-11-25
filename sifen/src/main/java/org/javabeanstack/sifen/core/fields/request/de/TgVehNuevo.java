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
import org.javabeanstack.sifen.core.types.TiTipCom;
import org.javabeanstack.sifen.core.types.TiTipOpVN;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.javabeanstack.sifen.internal.util.SifenUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.math.BigDecimal;

public class TgVehNuevo extends SifenObjectBase {
    private TiTipOpVN iTipOpVN;
    private String dChasis;
    private String dColor;
    private short dPotencia;
    private short dCapMot;
    private BigDecimal dPNet;
    private BigDecimal dPBruto;
    private TiTipCom iTipCom;
    private String dDesTipCom;
    private String dNroMotor;
    private BigDecimal dCapTracc;
    private short dAnoFab;
    private String cTipVeh;
    private short dCapac;
    private String dCilin;

    public void setupSOAPElements(SOAPElement gCamItem) throws SOAPException {
        SOAPElement gVehNuevo = gCamItem.addChildElement("gVehNuevo");
        if (this.iTipOpVN != null) {
            gVehNuevo.addChildElement("iTipOpVN").setTextContent(String.valueOf(this.iTipOpVN.getVal()));
            gVehNuevo.addChildElement("dDesTipOpVN").setTextContent(this.iTipOpVN.getDescripcion());
        }

        if (this.dChasis != null)
            gVehNuevo.addChildElement("dChasis").setTextContent(this.dChasis);

        if (this.dColor != null)
            gVehNuevo.addChildElement("dColor").setTextContent(this.dColor);

        if (this.dPotencia != 0)
            gVehNuevo.addChildElement("dPotencia").setTextContent(String.valueOf(this.dPotencia));

        if (this.dCapMot != 0)
            gVehNuevo.addChildElement("dCapMot").setTextContent(String.valueOf(this.dCapMot));

        if (this.dPNet != null)
            gVehNuevo.addChildElement("dPNet").setTextContent(String.valueOf(this.dPNet));

        if (this.dPBruto != null)
            gVehNuevo.addChildElement("dPBruto").setTextContent(String.valueOf(this.dPBruto));

        if (this.iTipOpVN != null) {
            gVehNuevo.addChildElement("iTipCom").setTextContent(String.valueOf(this.iTipCom.getVal()));
            // E3-23: con 9 («Otro») el texto libre del consumidor
            gVehNuevo.addChildElement("dDesTipCom").setTextContent(SifenUtil.describeWithOther(this.iTipCom.getVal() == 9, this.iTipCom.getDescripcion(), this.dDesTipCom));
        }

        if (this.dNroMotor != null)
            gVehNuevo.addChildElement("dNroMotor").setTextContent(this.dNroMotor);

        if (this.dCapTracc != null)
            gVehNuevo.addChildElement("dCapTracc").setTextContent(String.valueOf(this.dCapTracc));

        if (this.dAnoFab != 0)
            gVehNuevo.addChildElement("dAnoFab").setTextContent(String.valueOf(this.dAnoFab));

        if (this.cTipVeh != null)
            gVehNuevo.addChildElement("cTipVeh").setTextContent(this.cTipVeh);

        if (this.dCapac != 0)
            gVehNuevo.addChildElement("dCapac").setTextContent(String.valueOf(this.dCapac));

        if (this.dCilin != null)
            gVehNuevo.addChildElement("dCilin").setTextContent(this.dCilin);
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "iTipOpVN":
                this.iTipOpVN = TiTipOpVN.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "dChasis":
                this.dChasis = ResponseUtil.getTextValue(value);
                break;
            case "dColor":
                this.dColor = ResponseUtil.getTextValue(value);
                break;
            case "dPotencia":
                this.dPotencia = Short.parseShort(ResponseUtil.getTextValue(value));
                break;
            case "dCapMot":
                this.dCapMot = Short.parseShort(ResponseUtil.getTextValue(value));
                break;
            case "dPNet":
                this.dPNet = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dPBruto":
                this.dPBruto = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "iTipCom":
                this.iTipCom = TiTipCom.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "dDesTipCom":
                this.dDesTipCom = ResponseUtil.getTextValue(value);
                break;
            case "dNroMotor":
                this.dNroMotor = ResponseUtil.getTextValue(value);
                break;
            case "dCapTracc":
                this.dCapTracc = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dAnoFab":
                this.dAnoFab = Short.parseShort(ResponseUtil.getTextValue(value));
                break;
            case "cTipVeh":
                this.cTipVeh = ResponseUtil.getTextValue(value);
                break;
            case "dCapac":
                this.dCapac = Short.parseShort(ResponseUtil.getTextValue(value));
                break;
            case "dCilin":
                this.dCilin = ResponseUtil.getTextValue(value);
                break;
        }
    }

    public TiTipOpVN getiTipOpVN() {
        return iTipOpVN;
    }

    public void setiTipOpVN(TiTipOpVN iTipOpVN) {
        this.iTipOpVN = iTipOpVN;
    }

    public String getdChasis() {
        return dChasis;
    }

    public void setdChasis(String dChasis) {
        this.dChasis = dChasis;
    }

    public String getdColor() {
        return dColor;
    }

    public void setdColor(String dColor) {
        this.dColor = dColor;
    }

    public short getdPotencia() {
        return dPotencia;
    }

    public void setdPotencia(short dPotencia) {
        this.dPotencia = dPotencia;
    }

    public short getdCapMot() {
        return dCapMot;
    }

    public void setdCapMot(short dCapMot) {
        this.dCapMot = dCapMot;
    }

    public BigDecimal getdPNet() {
        return dPNet;
    }

    public void setdPNet(BigDecimal dPNet) {
        this.dPNet = dPNet;
    }

    public BigDecimal getdPBruto() {
        return dPBruto;
    }

    public void setdPBruto(BigDecimal dPBruto) {
        this.dPBruto = dPBruto;
    }

    public TiTipCom getiTipCom() {
        return iTipCom;
    }

    public void setiTipCom(TiTipCom iTipCom) {
        this.iTipCom = iTipCom;
    }

    public String getdDesTipCom() {
        return dDesTipCom;
    }

    public void setdDesTipCom(String dDesTipCom) {
        this.dDesTipCom = dDesTipCom;
    }

    public String getdNroMotor() {
        return dNroMotor;
    }

    public void setdNroMotor(String dNroMotor) {
        this.dNroMotor = dNroMotor;
    }

    public BigDecimal getdCapTracc() {
        return dCapTracc;
    }

    public void setdCapTracc(BigDecimal dCapTracc) {
        this.dCapTracc = dCapTracc;
    }

    public short getdAnoFab() {
        return dAnoFab;
    }

    public void setdAnoFab(short dAnoFab) {
        this.dAnoFab = dAnoFab;
    }

    public String getcTipVeh() {
        return cTipVeh;
    }

    public void setcTipVeh(String cTipVeh) {
        this.cTipVeh = cTipVeh;
    }

    public short getdCapac() {
        return dCapac;
    }

    public void setdCapac(short dCapac) {
        this.dCapac = dCapac;
    }

    public String getdCilin() {
        return dCilin;
    }

    public void setdCilin(String dCilin) {
        this.dCilin = dCilin;
    }

}

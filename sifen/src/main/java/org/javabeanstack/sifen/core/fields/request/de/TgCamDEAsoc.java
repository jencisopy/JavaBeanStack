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
import org.javabeanstack.sifen.core.types.TTipTra;
import org.javabeanstack.sifen.core.types.TdTipCons;
import org.javabeanstack.sifen.core.types.TiTIpoDoc;
import org.javabeanstack.sifen.core.types.TiTipDocAso;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.javabeanstack.sifen.internal.util.SifenUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.math.BigInteger;
import java.time.LocalDate;

public class TgCamDEAsoc extends SifenObjectBase {
    private TiTipDocAso iTipDocAso;
    private String dCdCDERef;
    private String dNTimDI;
    private String dEstDocAso;
    private String dPExpDocAso;
    private String dNumDocAso;
    private TiTIpoDoc iTipoDocAso;
    private LocalDate dFecEmiDI;
    private String dNumComRet;
    private String dNumResCF;
    private TdTipCons iTipCons;
    private BigInteger dNumCons;
    private String dNumControl;
    /** H018 {@code dRucFus}: RUC fusionado del emisor del CDC asociado (NT 023; plan JBSSIF, §6.2, I0-13). */
    private String dRucFus;

    public void setupSOAPElements(SOAPElement DE, TTipTra iTipTra, boolean withholdingExists) throws SOAPException {
        SOAPElement gCamDEAsoc = DE.addChildElement("gCamDEAsoc");
        gCamDEAsoc.addChildElement("iTipDocAso").setTextContent(String.valueOf(this.iTipDocAso.getVal()));
        gCamDEAsoc.addChildElement("dDesTipDocAso").setTextContent(this.iTipDocAso.getDescripcion());

        if (this.iTipDocAso.getVal() == 1)
            gCamDEAsoc.addChildElement("dCdCDERef").setTextContent(this.dCdCDERef);
        else if (this.iTipDocAso.getVal() == 2) {
            gCamDEAsoc.addChildElement("dNTimDI").setTextContent(this.dNTimDI);
            gCamDEAsoc.addChildElement("dEstDocAso").setTextContent(this.dEstDocAso);
            gCamDEAsoc.addChildElement("dPExpDocAso").setTextContent(this.dPExpDocAso);
            gCamDEAsoc.addChildElement("dNumDocAso").setTextContent(this.dNumDocAso);
            gCamDEAsoc.addChildElement("iTipoDocAso").setTextContent(String.valueOf(this.iTipoDocAso.getVal()));
            gCamDEAsoc.addChildElement("dDTipoDocAso").setTextContent(this.iTipoDocAso.getDescripcion());
            gCamDEAsoc.addChildElement("dFecEmiDI").setTextContent(this.dFecEmiDI.toString());
        }

        // E6-01 (§6.2): orden del XSD v150 —dNumComRet, dNumResCF y después la constancia (iTipCons…)—; la línea
        // base escribía la constancia primero y dNumResCF antes que dNumComRet
        if (withholdingExists)
            gCamDEAsoc.addChildElement("dNumComRet").setTextContent(this.dNumComRet);

        if (iTipTra != null && iTipTra.getVal() == 12)
            gCamDEAsoc.addChildElement("dNumResCF").setTextContent(this.dNumResCF);

        if (this.iTipDocAso.getVal() == 3) {
            gCamDEAsoc.addChildElement("iTipCons").setTextContent(String.valueOf(this.iTipCons.getVal()));
            gCamDEAsoc.addChildElement("dDesTipCons").setTextContent(this.iTipCons.getDescripcion());

            if (this.iTipCons.getVal() == 2) {
                gCamDEAsoc.addChildElement("dNumCons").setTextContent(String.valueOf(this.dNumCons));
                gCamDEAsoc.addChildElement("dNumControl").setTextContent(this.dNumControl);
            }
        }

        // NT 023 (H018): RUC fusionado, el último del grupo; solo si se cargó (H018a: solo en NC, ND y NR)
        if (this.dRucFus != null && !this.dRucFus.isBlank())
            gCamDEAsoc.addChildElement("dRucFus").setTextContent(this.dRucFus);
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "dRucFus":
                this.dRucFus = ResponseUtil.getTextValue(value);
                break;
            case "iTipDocAso":
                this.iTipDocAso = TiTipDocAso.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "dCdCDERef":
                this.dCdCDERef = ResponseUtil.getTextValue(value);
                break;
            case "dNTimDI":
                this.dNTimDI = ResponseUtil.getTextValue(value);
                break;
            case "dEstDocAso":
                this.dEstDocAso = ResponseUtil.getTextValue(value);
                break;
            case "dPExpDocAso":
                this.dPExpDocAso = ResponseUtil.getTextValue(value);
                break;
            case "dNumDocAso":
                this.dNumDocAso = ResponseUtil.getTextValue(value);
                break;
            case "iTipoDocAso":
                this.iTipoDocAso = TiTIpoDoc.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "dFecEmiDI":
                this.dFecEmiDI = ResponseUtil.getDateValue(value);
                break;
            case "iTipCons":
                this.iTipCons = TdTipCons.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "dNumCons":
                this.dNumCons = new BigInteger(ResponseUtil.getTextValue(value));
                break;
            case "dNumControl":
                this.dNumControl = ResponseUtil.getTextValue(value);
                break;
            case "dNumResCF":
                this.dNumResCF = ResponseUtil.getTextValue(value);
                break;
            case "dNumComRet":
                this.dNumComRet = ResponseUtil.getTextValue(value);
                break;
        }
    }

    public TiTipDocAso getiTipDocAso() {
        return iTipDocAso;
    }

    public void setiTipDocAso(TiTipDocAso iTipDocAso) {
        this.iTipDocAso = iTipDocAso;
    }

    public String getdCdCDERef() {
        return dCdCDERef;
    }

    public void setdCdCDERef(String dCdCDERef) {
        this.dCdCDERef = dCdCDERef;
    }

    public String getdNTimDI() {
        return dNTimDI;
    }

    public void setdNTimDI(String dNTimDI) {
        this.dNTimDI = dNTimDI;
    }

    public String getdEstDocAso() {
        return dEstDocAso;
    }

    public void setdEstDocAso(String dEstDocAso) {
        this.dEstDocAso = SifenUtil.leftPad(dEstDocAso, '0', 3);
    }

    public String getdPExpDocAso() {
        return dPExpDocAso;
    }

    public void setdPExpDocAso(String dPExpDocAso) {
        this.dPExpDocAso = SifenUtil.leftPad(dPExpDocAso, '0', 3);
    }

    public String getdNumDocAso() {
        return dNumDocAso;
    }

    public void setdNumDocAso(String dNumDocAso) {
        this.dNumDocAso = SifenUtil.leftPad(dNumDocAso, '0', 7);
    }

    public TiTIpoDoc getiTipoDocAso() {
        return iTipoDocAso;
    }

    public void setiTipoDocAso(TiTIpoDoc iTipoDocAso) {
        this.iTipoDocAso = iTipoDocAso;
    }

    public LocalDate getdFecEmiDI() {
        return dFecEmiDI;
    }

    public void setdFecEmiDI(LocalDate dFecEmiDI) {
        this.dFecEmiDI = dFecEmiDI;
    }

    public String getdNumComRet() {
        return dNumComRet;
    }

    public void setdNumComRet(String dNumComRet) {
        this.dNumComRet = dNumComRet;
    }

    public String getdNumResCF() {
        return dNumResCF;
    }

    public void setdNumResCF(String dNumResCF) {
        this.dNumResCF = dNumResCF;
    }

    public TdTipCons getiTipCons() {
        return iTipCons;
    }

    public void setiTipCons(TdTipCons iTipCons) {
        this.iTipCons = iTipCons;
    }

    public BigInteger getdNumCons() {
        return dNumCons;
    }

    public void setdNumCons(BigInteger dNumCons) {
        this.dNumCons = dNumCons;
    }

    public String getdNumControl() {
        return dNumControl;
    }

    public void setdNumControl(String dNumControl) {
        this.dNumControl = dNumControl;
    }

    /**
     * Devuelve el RUC fusionado ({@code dRucFus}, H018, NT 023).
     *
     * @return el RUC sin DV, o {@code null} si no se cargó.
     */
    public String getdRucFus() {
        return dRucFus;
    }

    /**
     * Fija el RUC fusionado ({@code dRucFus}, H018, A3-8, NT 023): obligatorio cuando el CDC asociado es de un RUC
     * fusionado, y solo en nota de crédito, nota de débito o nota de remisión (H018a).
     *
     * @param dRucFus el RUC sin DV.
     */
    public void setdRucFus(String dRucFus) {
        this.dRucFus = dRucFus;
    }
}

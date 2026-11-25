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
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.time.LocalDate;

public class TgRasMerc extends SifenObjectBase {
    private String dNumLote;
    private LocalDate dVencMerc;
    private String dNSerie;
    private String dNumPedi;
    private String dNumSegui;
    private String dNomImp;
    private String dDirImp;
    private String dNumFir;
    private String dNumReg;
    private String dNumRegEntCom;

    public void setupSOAPElements(SOAPElement gCamItem) throws SOAPException {
        SOAPElement gRasMerc = gCamItem.addChildElement("gRasMerc");
        if (this.dNumLote != null)
            gRasMerc.addChildElement("dNumLote").setTextContent(this.dNumLote);

        if (this.dVencMerc != null)
            gRasMerc.addChildElement("dVencMerc").setTextContent(this.dVencMerc.toString());

        if (this.dNSerie != null)
            gRasMerc.addChildElement("dNSerie").setTextContent(this.dNSerie);

        if (this.dNumPedi != null)
            gRasMerc.addChildElement("dNumPedi").setTextContent(this.dNumPedi);

        if (this.dNumSegui != null)
            gRasMerc.addChildElement("dNumSegui").setTextContent(this.dNumSegui);

        if (this.dNomImp != null)
            gRasMerc.addChildElement("dNomImp").setTextContent(this.dNomImp);

        if (this.dDirImp != null)
            gRasMerc.addChildElement("dDirImp").setTextContent(this.dDirImp);

        if (this.dNumFir != null)
            gRasMerc.addChildElement("dNumFir").setTextContent(this.dNumFir);

        if (this.dNumReg != null)
            gRasMerc.addChildElement("dNumReg").setTextContent(this.dNumReg);

        if (this.dNumRegEntCom != null)
            gRasMerc.addChildElement("dNumRegEntCom").setTextContent(this.dNumRegEntCom);
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "dNumLote":
                this.dNumLote = ResponseUtil.getTextValue(value);
                break;
            case "dVencMerc":
                this.dVencMerc = ResponseUtil.getDateValue(value);
                break;
            case "dNSerie":
                this.dNSerie = ResponseUtil.getTextValue(value);
                break;
            case "dNumPedi":
                this.dNumPedi = ResponseUtil.getTextValue(value);
                break;
            case "dNumSegui":
                this.dNumSegui = ResponseUtil.getTextValue(value);
                break;
            case "dNomImp":
                this.dNomImp = ResponseUtil.getTextValue(value);
                break;
            case "dDirImp":
                this.dDirImp = ResponseUtil.getTextValue(value);
                break;
            case "dNumFir":
                this.dNumFir = ResponseUtil.getTextValue(value);
                break;
            case "dNumReg":
                this.dNumReg = ResponseUtil.getTextValue(value);
                break;
            case "dNumRegEntCom":
                this.dNumRegEntCom = ResponseUtil.getTextValue(value);
                break;
        }
    }

    public String getdNumLote() {
        return dNumLote;
    }

    public void setdNumLote(String dNumLote) {
        this.dNumLote = dNumLote;
    }

    public LocalDate getdVencMerc() {
        return dVencMerc;
    }

    public void setdVencMerc(LocalDate dVencMerc) {
        this.dVencMerc = dVencMerc;
    }

    public String getdNSerie() {
        return dNSerie;
    }

    public void setdNSerie(String dNSerie) {
        this.dNSerie = dNSerie;
    }

    public String getdNumPedi() {
        return dNumPedi;
    }

    public void setdNumPedi(String dNumPedi) {
        this.dNumPedi = dNumPedi;
    }

    public String getdNumSegui() {
        return dNumSegui;
    }

    public void setdNumSegui(String dNumSegui) {
        this.dNumSegui = dNumSegui;
    }

    public String getdNomImp() {
        return dNomImp;
    }

    public void setdNomImp(String dNomImp) {
        this.dNomImp = dNomImp;
    }

    public String getdDirImp() {
        return dDirImp;
    }

    public void setdDirImp(String dDirImp) {
        this.dDirImp = dDirImp;
    }

    public String getdNumFir() {
        return dNumFir;
    }

    public void setdNumFir(String dNumFir) {
        this.dNumFir = dNumFir;
    }

    public String getdNumReg() {
        return dNumReg;
    }

    public void setdNumReg(String dNumReg) {
        this.dNumReg = dNumReg;
    }

    public String getdNumRegEntCom() {
        return dNumRegEntCom;
    }

    public void setdNumRegEntCom(String dNumRegEntCom) {
        this.dNumRegEntCom = dNumRegEntCom;
    }
}

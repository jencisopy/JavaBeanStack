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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TgGrupPolSeg extends SifenObjectBase {
    private String dPoliza;
    private String dUnidVig;
    private BigDecimal dVigencia;
    private String dNumPoliza;
    private LocalDateTime dFecIniVig;
    private LocalDateTime dFecFinVig;
    private String dCodInt;

    public void setupSOAPElements(SOAPElement gGrupSeg) throws SOAPException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        SOAPElement gGrupPolSeg = gGrupSeg.addChildElement("gGrupPolSeg");
        gGrupPolSeg.addChildElement("dPoliza").setTextContent(this.dPoliza);
        gGrupPolSeg.addChildElement("dUnidVig").setTextContent(this.dUnidVig);
        gGrupPolSeg.addChildElement("dVigencia").setTextContent(String.valueOf(this.dVigencia));
        gGrupPolSeg.addChildElement("dNumPoliza").setTextContent(this.dNumPoliza);

        if (this.dFecIniVig != null)
            gGrupPolSeg.addChildElement("dFecIniVig").setTextContent(this.dFecIniVig.format(formatter));

        if (this.dFecFinVig != null)
            gGrupPolSeg.addChildElement("dFecFinVig").setTextContent(this.dFecFinVig.format(formatter));

        if (this.dCodInt != null)
            gGrupPolSeg.addChildElement("dCodInt").setTextContent(this.dCodInt);
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "dPoliza":
                this.dPoliza = ResponseUtil.getTextValue(value);
                break;
            case "dUnidVig":
                this.dUnidVig = ResponseUtil.getTextValue(value);
                break;
            case "dVigencia":
                this.dVigencia = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dNumPoliza":
                this.dNumPoliza = ResponseUtil.getTextValue(value);
                break;
            case "dFecIniVig":
                this.dFecIniVig = ResponseUtil.getDateTimeValue(value);
                break;
            case "dFecFinVig":
                this.dFecFinVig = ResponseUtil.getDateTimeValue(value);
                break;
            case "dCodInt":
                this.dCodInt = ResponseUtil.getTextValue(value);
                break;
        }
    }

    public String getdPoliza() {
        return dPoliza;
    }

    public void setdPoliza(String dPoliza) {
        this.dPoliza = dPoliza;
    }

    public String getdUnidVig() {
        return dUnidVig;
    }

    public void setdUnidVig(String dUnidVig) {
        this.dUnidVig = dUnidVig;
    }

    public BigDecimal getdVigencia() {
        return dVigencia;
    }

    public void setdVigencia(BigDecimal dVigencia) {
        this.dVigencia = dVigencia;
    }

    public String getdNumPoliza() {
        return dNumPoliza;
    }

    public void setdNumPoliza(String dNumPoliza) {
        this.dNumPoliza = dNumPoliza;
    }

    public LocalDateTime getdFecIniVig() {
        return dFecIniVig;
    }

    public void setdFecIniVig(LocalDateTime dFecIniVig) {
        this.dFecIniVig = dFecIniVig;
    }

    public LocalDateTime getdFecFinVig() {
        return dFecFinVig;
    }

    public void setdFecFinVig(LocalDateTime dFecFinVig) {
        this.dFecFinVig = dFecFinVig;
    }

    public String getdCodInt() {
        return dCodInt;
    }

    public void setdCodInt(String dCodInt) {
        this.dCodInt = dCodInt;
    }

}

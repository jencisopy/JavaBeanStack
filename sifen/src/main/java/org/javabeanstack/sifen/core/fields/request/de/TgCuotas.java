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
import org.javabeanstack.sifen.core.types.CMondT;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TgCuotas extends SifenObjectBase {
    private CMondT cMoneCuo;
    private BigDecimal dMonCuota;
    private LocalDate dVencCuo;

    public void setupSOAPElements(SOAPElement gPagCred) throws SOAPException {
        SOAPElement gCuotas = gPagCred.addChildElement("gCuotas");
        gCuotas.addChildElement("cMoneCuo").setTextContent(this.cMoneCuo.name());
        gCuotas.addChildElement("dDMoneCuo").setTextContent(this.cMoneCuo.getDescripcion());
        gCuotas.addChildElement("dMonCuota").setTextContent(String.valueOf(this.dMonCuota));

        if (this.dVencCuo != null)
            gCuotas.addChildElement("dVencCuo").setTextContent(this.dVencCuo.toString());
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "cMoneCuo":
                this.cMoneCuo = CMondT.getByName(ResponseUtil.getTextValue(value));
                break;
            case "dMonCuota":
                this.dMonCuota = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dVencCuo":
                this.dVencCuo = ResponseUtil.getDateValue(value);
                break;
        }
    }

    public CMondT getcMoneCuo() {
        return cMoneCuo;
    }

    public void setcMoneCuo(CMondT cMoneCuo) {
        this.cMoneCuo = cMoneCuo;
    }

    public BigDecimal getdMonCuota() {
        return dMonCuota;
    }

    public void setdMonCuota(BigDecimal dMonCuota) {
        this.dMonCuota = dMonCuota;
    }

    public LocalDate getdVencCuo() {
        return dVencCuo;
    }

    public void setdVencCuo(LocalDate dVencCuo) {
        this.dVencCuo = dVencCuo;
    }
}

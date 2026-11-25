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
import org.javabeanstack.sifen.internal.response.SifenObjectFactory;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.util.ArrayList;
import java.util.List;

public class TgGrupSeg extends SifenObjectBase {
    private String dCodEmpSeg;
    private List<TgGrupPolSeg> gGrupPolSegList;

    public void setupSOAPElements(SOAPElement gCamEsp) throws SOAPException {
        SOAPElement gGrupSeg = gCamEsp.addChildElement("gGrupSeg");
        if (this.dCodEmpSeg != null)
            gGrupSeg.addChildElement("dCodEmpSeg").setTextContent(this.dCodEmpSeg);

        for (TgGrupPolSeg gGrupPolSeg : this.gGrupPolSegList) {
            gGrupPolSeg.setupSOAPElements(gGrupSeg);
        }
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        if (value.getLocalName().equals("dCodEmpSeg")) {
            this.dCodEmpSeg = ResponseUtil.getTextValue(value);
        } else if (value.getLocalName().equals("gGrupPolSeg")) {
            if (this.gGrupPolSegList == null) {
                this.gGrupPolSegList = new ArrayList<>();
            }
            this.gGrupPolSegList.add(SifenObjectFactory.getFromNode(value, TgGrupPolSeg.class));
        }
    }

    public String getdCodEmpSeg() {
        return dCodEmpSeg;
    }

    public void setdCodEmpSeg(String dCodEmpSeg) {
        this.dCodEmpSeg = dCodEmpSeg;
    }

    public List<TgGrupPolSeg> getgGrupPolSegList() {
        return gGrupPolSegList;
    }

    public void setgGrupPolSegList(List<TgGrupPolSeg> gGrupPolSegList) {
        this.gGrupPolSegList = gGrupPolSegList;
    }
}
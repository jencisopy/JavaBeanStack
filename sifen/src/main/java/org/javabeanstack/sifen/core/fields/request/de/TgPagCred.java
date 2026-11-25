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
import org.javabeanstack.sifen.core.types.TiCondCred;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.response.SifenObjectFactory;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TgPagCred extends SifenObjectBase {
    private TiCondCred iCondCred;
    private String dPlazoCre;
    private short dCuotas;
    private BigDecimal dMonEnt;
    private List<TgCuotas> gCuotasList;

    public void setupSOAPElements(SOAPElement gCamCond) throws SOAPException {
        SOAPElement gPagCred = gCamCond.addChildElement("gPagCred");
        gPagCred.addChildElement("iCondCred").setTextContent(String.valueOf(this.iCondCred.getVal()));
        gPagCred.addChildElement("dDCondCred").setTextContent(this.iCondCred.getDescripcion());

        if (this.dPlazoCre != null || this.iCondCred.getVal() == 1)
            gPagCred.addChildElement("dPlazoCre").setTextContent(this.dPlazoCre);

        if (this.dCuotas != 0 || this.iCondCred.getVal() == 2)
            gPagCred.addChildElement("dCuotas").setTextContent(String.valueOf(this.dCuotas));

        if (this.dMonEnt != null)
            gPagCred.addChildElement("dMonEnt").setTextContent(String.valueOf(this.dMonEnt));

        if (this.gCuotasList != null) {
            for (TgCuotas gCuotas : this.gCuotasList) {
                gCuotas.setupSOAPElements(gPagCred);
            }
        }
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "iCondCred":
                this.iCondCred = TiCondCred.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "dPlazoCre":
                this.dPlazoCre = ResponseUtil.getTextValue(value);
                break;
            case "dCuotas":
                this.dCuotas = Short.parseShort(ResponseUtil.getTextValue(value));
                break;
            case "dMonEnt":
                this.dMonEnt = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "gCuotas":
                if (this.gCuotasList == null) {
                    this.gCuotasList = new ArrayList<>();
                }
                this.gCuotasList.add(SifenObjectFactory.getFromNode(value, TgCuotas.class));
                break;
        }
    }

    public TiCondCred getiCondCred() {
        return iCondCred;
    }

    public void setiCondCred(TiCondCred iCondCred) {
        this.iCondCred = iCondCred;
    }

    public String getdPlazoCre() {
        return dPlazoCre;
    }

    public void setdPlazoCre(String dPlazoCre) {
        this.dPlazoCre = dPlazoCre;
    }

    public short getdCuotas() {
        return dCuotas;
    }

    public void setdCuotas(short dCuotas) {
        this.dCuotas = dCuotas;
    }

    public BigDecimal getdMonEnt() {
        return dMonEnt;
    }

    public void setdMonEnt(BigDecimal dMonEnt) {
        this.dMonEnt = dMonEnt;
    }

    public List<TgCuotas> getgCuotasList() {
        return gCuotasList;
    }

    public void setgCuotasList(List<TgCuotas> gCuotasList) {
        this.gCuotasList = gCuotasList;
    }
}

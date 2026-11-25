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
import org.javabeanstack.sifen.core.types.TiCondOpe;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.response.SifenObjectFactory;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TgCamCond extends SifenObjectBase {
    private TiCondOpe iCondOpe;
    private List<TgPaConEIni> gPaConEIniList;
    private TgPagCred gPagCred;

    public void setupSOAPElements(SOAPElement gDtipDE) throws SOAPException {
        SOAPElement gCamCond = gDtipDE.addChildElement("gCamCond");
        gCamCond.addChildElement("iCondOpe").setTextContent(String.valueOf(this.iCondOpe.getVal()));
        gCamCond.addChildElement("dDCondOpe").setTextContent(this.iCondOpe.getDescripcion());

        if (this.iCondOpe.getVal() == 1 || this.gPagCred.getdMonEnt() != null || this.gPaConEIniList != null) {
            for (TgPaConEIni gPaConEIni : Objects.requireNonNull(this.gPaConEIniList)) {
                gPaConEIni.setupSOAPElements(gCamCond);
            }
        }

        if (this.iCondOpe.getVal() == 2)
            this.gPagCred.setupSOAPElements(gCamCond);
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "iCondOpe":
                this.iCondOpe = TiCondOpe.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "gPaConEIni":
                if (this.gPaConEIniList == null) {
                    this.gPaConEIniList = new ArrayList<>();
                }
                this.gPaConEIniList.add(SifenObjectFactory.getFromNode(value, TgPaConEIni.class));
                break;
            case "gPagCred":
                this.gPagCred = SifenObjectFactory.getFromNode(value, TgPagCred.class);
                break;
        }
    }

    public TiCondOpe getiCondOpe() {
        return iCondOpe;
    }

    public void setiCondOpe(TiCondOpe iCondOpe) {
        this.iCondOpe = iCondOpe;
    }

    public TgPagCred getgPagCred() {
        return gPagCred;
    }

    public void setgPagCred(TgPagCred gPagCred) {
        this.gPagCred = gPagCred;
    }

    public List<TgPaConEIni> getgPaConEIniList() {
        return gPaConEIniList;
    }

    public void setgPaConEIniList(List<TgPaConEIni> gPaConEIniList) {
        this.gPaConEIniList = gPaConEIniList;
    }
}

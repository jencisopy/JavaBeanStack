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
import org.javabeanstack.sifen.core.types.TTiDE;
import org.javabeanstack.sifen.core.types.TiTiOpe;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.response.SifenObjectFactory;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.util.ArrayList;
import java.util.List;

public class TgDtipDE extends SifenObjectBase {
    private TgCamFE gCamFE;
    private TgCamAE gCamAE;
    private TgCamNCDE gCamNCDE;
    private TgCamNRE gCamNRE;
    private TgCamCond gCamCond;
    private List<TgCamItem> gCamItemList;
    private TgCamEsp gCamEsp;
    private TgTransp gTransp;

    public void setupSOAPElements(GenerationCtx generationCtx, SOAPElement DE, TTiDE iTiDE, TdDatGralOpe gDatGralOpe) throws SOAPException {
        TiTiOpe iTiOpe = gDatGralOpe.getgDatRec().getiTiOpe();

        SOAPElement gDtipDE = DE.addChildElement("gDtipDE");
        if (iTiDE.getVal() == 1)
            this.gCamFE.setupSOAPElements(gDtipDE, iTiOpe);
        else if (iTiDE.getVal() == 4)
            this.gCamAE.setupSOAPElements(gDtipDE);
        else if (iTiDE.getVal() == 5 || iTiDE.getVal() == 6)
            this.gCamNCDE.setupSOAPElements(gDtipDE);
        else if (iTiDE.getVal() == 7)
            this.gCamNRE.setupSOAPElements(gDtipDE);

        if (iTiDE.getVal() == 1 || iTiDE.getVal() == 4)
            this.gCamCond.setupSOAPElements(gDtipDE);

        for (TgCamItem gCamItem : this.gCamItemList) {
            gCamItem.setupSOAPElements(generationCtx, gDtipDE, iTiDE, gDatGralOpe);
        }

        if (this.gCamEsp != null)
            this.gCamEsp.setupSOAPElements(gDtipDE);

        if (iTiDE.getVal() == 7 || (iTiDE.getVal() == 1 && this.gTransp != null))
            this.gTransp.setupSOAPElements(gDtipDE, iTiDE, this.gCamNRE != null ? this.gCamNRE.getiMotEmiNR() : null);
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "gCamFE":
                this.gCamFE = SifenObjectFactory.getFromNode(value, TgCamFE.class);
                break;
            case "gCamAE":
                this.gCamAE = SifenObjectFactory.getFromNode(value, TgCamAE.class);
                break;
            case "gCamNCDE":
                this.gCamNCDE = SifenObjectFactory.getFromNode(value, TgCamNCDE.class);
                break;
            case "gCamNRE":
                this.gCamNRE = SifenObjectFactory.getFromNode(value, TgCamNRE.class);
                break;
            case "gCamCond":
                this.gCamCond = SifenObjectFactory.getFromNode(value, TgCamCond.class);
                break;
            case "gCamItem":
                if (this.gCamItemList == null) {
                    this.gCamItemList = new ArrayList<>();
                }
                this.gCamItemList.add(SifenObjectFactory.getFromNode(value, TgCamItem.class));
                break;
            case "gCamEsp":
                this.gCamEsp = SifenObjectFactory.getFromNode(value, TgCamEsp.class);
                break;
            case "gTransp":
                this.gTransp = SifenObjectFactory.getFromNode(value, TgTransp.class);
                break;
        }
    }

    public TgCamFE getgCamFE() {
        return gCamFE;
    }

    public void setgCamFE(TgCamFE gCamFE) {
        this.gCamFE = gCamFE;
    }

    public TgCamAE getgCamAE() {
        return gCamAE;
    }

    public void setgCamAE(TgCamAE gCamAE) {
        this.gCamAE = gCamAE;
    }

    public TgCamNCDE getgCamNCDE() {
        return gCamNCDE;
    }

    public void setgCamNCDE(TgCamNCDE gCamNCDE) {
        this.gCamNCDE = gCamNCDE;
    }

    public TgCamNRE getgCamNRE() {
        return gCamNRE;
    }

    public void setgCamNRE(TgCamNRE gCamNRE) {
        this.gCamNRE = gCamNRE;
    }

    public TgCamCond getgCamCond() {
        return gCamCond;
    }

    public void setgCamCond(TgCamCond gCamCond) {
        this.gCamCond = gCamCond;
    }

    public List<TgCamItem> getgCamItemList() {
        return gCamItemList;
    }

    public void setgCamItemList(List<TgCamItem> gCamItemList) {
        this.gCamItemList = gCamItemList;
    }

    public TgCamEsp getgCamEsp() {
        return gCamEsp;
    }

    public void setgCamEsp(TgCamEsp gCamEsp) {
        this.gCamEsp = gCamEsp;
    }

    public TgTransp getgTransp() {
        return gTransp;
    }

    public void setgTransp(TgTransp gTransp) {
        this.gTransp = gTransp;
    }

}

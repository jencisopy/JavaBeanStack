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
import org.javabeanstack.sifen.core.types.*;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.response.SifenObjectFactory;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TgOpeCom extends SifenObjectBase {
    private TTipTra iTipTra;
    private TTImp iTImp;
    private CMondT cMoneOpe;
    private TdCondTiCam dCondTiCam; // opcional
    private BigDecimal dTiCam;
    private TiCondAnt iCondAnt;
    /** D030 {@code gOblAfe}, 0 a 11 (NT 018; plan JBSSIF, §6.1, I0-01). */
    private List<TgOblAfe> gOblAfeList;

    public void setupSOAPElements(SOAPElement gDatGralOpe, TTiDE iTiDE) throws SOAPException {
        SOAPElement gOpeCom = gDatGralOpe.addChildElement("gOpeCom");
        if (iTiDE.getVal() == 1 || iTiDE.getVal() == 4) {
            gOpeCom.addChildElement("iTipTra").setTextContent(String.valueOf(this.iTipTra.getVal()));
            gOpeCom.addChildElement("dDesTipTra").setTextContent(this.iTipTra.getDescripcion());
        }

        gOpeCom.addChildElement("iTImp").setTextContent(String.valueOf(this.iTImp.getVal()));
        gOpeCom.addChildElement("dDesTImp").setTextContent(this.iTImp.getDescripcion());
        gOpeCom.addChildElement("cMoneOpe").setTextContent(this.cMoneOpe.name());
        gOpeCom.addChildElement("dDesMoneOpe").setTextContent(this.cMoneOpe.getDescripcion());

        if (!this.cMoneOpe.name().equals("PYG")) {
            gOpeCom.addChildElement("dCondTiCam").setTextContent(String.valueOf(this.dCondTiCam.getVal()));
            if (this.dCondTiCam.getVal() == 1) {
                gOpeCom.addChildElement("dTiCam").setTextContent(String.valueOf(this.dTiCam));
            }
        }

        if (this.iCondAnt != null) {
            gOpeCom.addChildElement("iCondAnt").setTextContent(String.valueOf(this.iCondAnt.getVal()));
            gOpeCom.addChildElement("dDesCondAnt").setTextContent(this.iCondAnt.getDescripcion());
        }

        // NT 018 (D030): obligaciones afectadas, después de los campos de gOpeCom (§6.1, I0-01)
        if (this.gOblAfeList != null) {
            for (TgOblAfe gOblAfe : this.gOblAfeList) {
                if (gOblAfe != null && gOblAfe.getcOblAfe() != null) { // sin la validación previa, un nulo no da NPE (D031 lo reporta)
                    gOblAfe.setupSOAPElements(gOpeCom);
                }
            }
        }
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "iTipTra":
                this.iTipTra = TTipTra.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "iTImp":
                this.iTImp = TTImp.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "cMoneOpe":
                this.cMoneOpe = CMondT.getByName(ResponseUtil.getTextValue(value));
                break;
            case "dCondTiCam":
                this.dCondTiCam = TdCondTiCam.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "dTiCam":
                this.dTiCam = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "iCondAnt":
                this.iCondAnt = TiCondAnt.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "gOblAfe":
                if (this.gOblAfeList == null) {
                    this.gOblAfeList = new ArrayList<>();
                }
                this.gOblAfeList.add(SifenObjectFactory.getFromNode(value, TgOblAfe.class));
                break;
        }
    }

    public TTipTra getiTipTra() {
        return iTipTra;
    }

    public void setiTipTra(TTipTra iTipTra) {
        this.iTipTra = iTipTra;
    }

    public TTImp getiTImp() {
        return iTImp;
    }

    public void setiTImp(TTImp iTImp) {
        this.iTImp = iTImp;
    }

    public CMondT getcMoneOpe() {
        return cMoneOpe;
    }

    public void setcMoneOpe(CMondT cMoneOpe) {
        this.cMoneOpe = cMoneOpe;
    }

    public TdCondTiCam getdCondTiCam() {
        return dCondTiCam;
    }

    public void setdCondTiCam(TdCondTiCam dCondTiCam) {
        this.dCondTiCam = dCondTiCam;
    }

    public BigDecimal getdTiCam() {
        return dTiCam;
    }

    public void setdTiCam(BigDecimal dTiCam) {
        this.dTiCam = dTiCam;
    }

    public TiCondAnt getiCondAnt() {
        return iCondAnt;
    }

    public void setiCondAnt(TiCondAnt iCondAnt) {
        this.iCondAnt = iCondAnt;
    }

    /**
     * Devuelve las obligaciones afectadas ({@code gOblAfe}, D030, NT 018).
     *
     * @return la lista, o {@code null} si no se cargó ninguna.
     */
    public List<TgOblAfe> getgOblAfeList() {
        return gOblAfeList;
    }

    /**
     * Fija las obligaciones afectadas ({@code gOblAfe}, D030, NT 018): de 0 a 11, sin códigos repetidos (D031a, NT 022).
     *
     * @param gOblAfeList la lista; {@code null} o vacía para no informar ninguna.
     */
    public void setgOblAfeList(List<TgOblAfe> gOblAfeList) {
        this.gOblAfeList = gOblAfeList;
    }
}
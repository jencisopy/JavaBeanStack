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
import org.javabeanstack.sifen.core.types.TiMotivTras;
import org.javabeanstack.sifen.core.types.TiRespEmiNR;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.javabeanstack.sifen.internal.util.SifenUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.time.LocalDate;

/**
 * Campos de la nota de remisión electrónica
 *
 * <p>Plan JBSSIF, §6.2 (I3-17): con el motivo «Otro» (99) la descripción {@code dDesMotEmiNR} (E502, A5-60) es el
 * texto libre que carga la aplicación ({@link #setdDesMotEmiNR(String)}); con los demás motivos, la del código.</p>
 */
public class TgCamNRE extends SifenObjectBase {
    private TiMotivTras iMotEmiNR;
    private String dDesMotEmiNR;
    private TiRespEmiNR iRespEmiNR;
    private int dKmR;
    private LocalDate dFecEm;

    public void setupSOAPElements(SOAPElement gDtipDE) throws SOAPException {
        SOAPElement gCamNRE = gDtipDE.addChildElement("gCamNRE");
        gCamNRE.addChildElement("iMotEmiNR").setTextContent(String.valueOf(this.iMotEmiNR.getVal()));
        // I3-17: con 99 («Otro») va el texto libre; la línea base escribía siempre «Otro» (4 caracteres < 5)
        gCamNRE.addChildElement("dDesMotEmiNR").setTextContent(SifenUtil.describeWithOther(this.iMotEmiNR.getVal() == 99,
                this.iMotEmiNR.getDescripcion(), this.dDesMotEmiNR));
        gCamNRE.addChildElement("iRespEmiNR").setTextContent(String.valueOf(this.iRespEmiNR.getVal()));
        gCamNRE.addChildElement("dDesRespEmiNR").setTextContent(this.iRespEmiNR.getDescripcion());

        if (this.dKmR != 0)
            gCamNRE.addChildElement("dKmR").setTextContent(String.valueOf(this.dKmR));

        if (this.dFecEm != null)
            gCamNRE.addChildElement("dFecEm").setTextContent(this.dFecEm.toString());
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "iMotEmiNR":
                this.iMotEmiNR = TiMotivTras.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "dDesMotEmiNR":
                this.dDesMotEmiNR = ResponseUtil.getTextValue(value);
                break;
            case "iRespEmiNR":
                this.iRespEmiNR = TiRespEmiNR.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "dKmR":
                this.dKmR = Integer.parseInt(ResponseUtil.getTextValue(value));
                break;
            case "dFecEm":
                this.dFecEm = ResponseUtil.getDateValue(value);
                break;
        }
    }

    public TiMotivTras getiMotEmiNR() {
        return iMotEmiNR;
    }

    public void setiMotEmiNR(TiMotivTras iMotEmiNR) {
        this.iMotEmiNR = iMotEmiNR;
    }

    public TiRespEmiNR getiRespEmiNR() {
        return iRespEmiNR;
    }

    public void setiRespEmiNR(TiRespEmiNR iRespEmiNR) {
        this.iRespEmiNR = iRespEmiNR;
    }

    public int getdKmR() {
        return dKmR;
    }

    public void setdKmR(int dKmR) {
        this.dKmR = dKmR;
    }

    public LocalDate getdFecEm() {
        return dFecEm;
    }

    public void setdFecEm(LocalDate dFecEm) {
        this.dFecEm = dFecEm;
    }

    /**
     * Devuelve el texto libre del motivo de emisión ({@code dDesMotEmiNR}) que se escribe con el motivo «Otro» (99).
     *
     * @return el texto, o {@code null} si no se cargó.
     */
    public String getdDesMotEmiNR() {
        return dDesMotEmiNR;
    }

    /**
     * Fija el texto libre del motivo de emisión ({@code dDesMotEmiNR}, E502, 5 a 60 caracteres): se escribe solo con el
     * motivo «Otro» (99); con los demás motivos sale la descripción del código (plan JBSSIF, §6.2, I3-17).
     *
     * @param dDesMotEmiNR el texto.
     */
    public void setdDesMotEmiNR(String dDesMotEmiNR) {
        this.dDesMotEmiNR = dDesMotEmiNR;
    }
}

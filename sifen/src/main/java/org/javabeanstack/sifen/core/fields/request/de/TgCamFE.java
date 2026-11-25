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
import org.javabeanstack.sifen.core.types.TiIndPres;
import org.javabeanstack.sifen.core.types.TiTiOpe;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.response.SifenObjectFactory;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.javabeanstack.sifen.internal.util.SifenUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Grupo E010 {@code gCamFE}: campos de la factura electrónica.
 *
 * <p>Delta del fork 2.0.0-SNAPSHOT (plan JBSSIF, §1.4.6; ficha 7 del informe de línea base): el
 * grupo {@code gCompPub} se escribe solo si está cargado <em>y</em> la operación es B2G
 * ({@code &&} en lugar de {@code ||}, E0-14). Se porta tal cual (D10).</p>
 */
public class TgCamFE extends SifenObjectBase {
    private TiIndPres iIndPres;
    private String dDesIndPres;
    private LocalDate dFecEmNR;
    private TgCompPub gCompPub;

    public void setupSOAPElements(SOAPElement gDtipDE, TiTiOpe iTiOpe) throws SOAPException {
        SOAPElement gCamFE = gDtipDE.addChildElement("gCamFE");
        gCamFE.addChildElement("iIndPres").setTextContent(String.valueOf(this.iIndPres.getVal()));
        // E3-23: con 9 («Otro», E012) el texto libre del consumidor
        gCamFE.addChildElement("dDesIndPres").setTextContent(SifenUtil.describeWithOther(this.iIndPres.getVal() == 9, this.iIndPres.getDescripcion(), this.dDesIndPres));

        if (this.dFecEmNR != null)
            gCamFE.addChildElement("dFecEmNR").setTextContent(this.dFecEmNR.toString());

        if (this.gCompPub != null && iTiOpe.getVal() == 3) {
            Objects.requireNonNull(this.gCompPub).setupSOAPElements(gCamFE);
        }
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "iIndPres":
                this.iIndPres = TiIndPres.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "dDesIndPres":
                this.dDesIndPres = ResponseUtil.getTextValue(value);
                break;
            case "dFecEmNR":
                this.dFecEmNR = ResponseUtil.getDateValue(value);
                break;
            case "gCompPub":
                this.gCompPub = SifenObjectFactory.getFromNode(value, TgCompPub.class);
                break;
        }
    }

    public TiIndPres getiIndPres() {
        return iIndPres;
    }

    public void setiIndPres(TiIndPres iIndPres) {
        this.iIndPres = iIndPres;
    }

    public String getdDesIndPres() {
        return dDesIndPres;
    }

    public void setdDesIndPres(String dDesIndPres) {
        this.dDesIndPres = dDesIndPres;
    }

    public LocalDate getdFecEmNR() {
        return dFecEmNR;
    }

    public void setdFecEmNR(LocalDate dFecEmNR) {
        this.dFecEmNR = dFecEmNR;
    }

    public TgCompPub getgCompPub() {
        return gCompPub;
    }

    public void setgCompPub(TgCompPub gCompPub) {
        this.gCompPub = gCompPub;
    }
}

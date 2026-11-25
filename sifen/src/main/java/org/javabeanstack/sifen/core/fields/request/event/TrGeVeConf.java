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
package org.javabeanstack.sifen.core.fields.request.event;

import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.types.TiTipConf;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TrGeVeConf extends SifenObjectBase {
    private String Id;
    private TiTipConf iTipConf;
    private LocalDateTime dFecRecep;

    public void setupSOAPElements(SOAPElement gGroupTiEvt) throws SOAPException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        SOAPElement rGeVeConf = gGroupTiEvt.addChildElement("rGeVeConf");

        rGeVeConf.addChildElement("Id").setTextContent(this.Id);
        rGeVeConf.addChildElement("iTipConf").setTextContent(String.valueOf(this.iTipConf.getVal()));

        if (this.dFecRecep != null || this.iTipConf.getVal() == 2)
            rGeVeConf.addChildElement("dFecRecep").setTextContent(this.dFecRecep.format(formatter));
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "Id":
                this.Id = ResponseUtil.getTextValue(value);
                break;
            case "iTipConf":
                this.iTipConf = TiTipConf.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "dFecRecep":
                this.dFecRecep = ResponseUtil.getDateTimeValue(value);
                break;
        }
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public TiTipConf getiTipConf() {
        return iTipConf;
    }

    public void setiTipConf(TiTipConf iTipConf) {
        this.iTipConf = iTipConf;
    }

    public LocalDateTime getdFecRecep() {
        return dFecRecep;
    }

    public void setdFecRecep(LocalDateTime dFecRecep) {
        this.dFecRecep = dFecRecep;
    }
}
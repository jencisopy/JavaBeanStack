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
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import java.time.LocalDateTime;

public class TrGeVeRetAce extends SifenObjectBase {
    private String Id;
    private int dNumTimRet;
    private String dEstRet;
    private String dPunExpRet;
    private String dNumDocRet;
    private String dCodConRet;
    private LocalDateTime dFeEmiRet;

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "Id":
                this.Id = ResponseUtil.getTextValue(value);
                break;
            case "dNumTimRet":
                this.dNumTimRet = Integer.parseInt(ResponseUtil.getTextValue(value));
                break;
            case "dEstRet":
                this.dEstRet = ResponseUtil.getTextValue(value);
                break;
            case "dPunExpRet":
                this.dPunExpRet = ResponseUtil.getTextValue(value);
                break;
            case "dNumDocRet":
                this.dNumDocRet = ResponseUtil.getTextValue(value);
                break;
            case "dCodConRet":
                this.dCodConRet = ResponseUtil.getTextValue(value);
                break;
            case "dFeEmiRet":
                this.dFeEmiRet = ResponseUtil.getDateTimeValue(value);
                break;
        }
    }

    public String getId() {
        return Id;
    }

    public int getdNumTimRet() {
        return dNumTimRet;
    }

    public String getdEstRet() {
        return dEstRet;
    }

    public String getdPunExpRet() {
        return dPunExpRet;
    }

    public String getdNumDocRet() {
        return dNumDocRet;
    }

    public String getdCodConRet() {
        return dCodConRet;
    }

    public LocalDateTime getdFeEmiRet() {
        return dFeEmiRet;
    }
}
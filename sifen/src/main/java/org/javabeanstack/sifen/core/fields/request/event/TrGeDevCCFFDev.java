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

public class TrGeDevCCFFDev extends SifenObjectBase {
    private String Id;
    private String dNumDevSol;
    private String dNumDevInf;
    private String dNumDevRes;
    private LocalDateTime dFeEmiSol;
    private LocalDateTime dFeEmiInf;
    private LocalDateTime dFeEmiRes;

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "Id":
                this.Id = ResponseUtil.getTextValue(value);
                break;
            case "dNumDevSol":
                this.dNumDevSol = ResponseUtil.getTextValue(value);
                break;
            case "dNumDevInf":
                this.dNumDevInf = ResponseUtil.getTextValue(value);
                break;
            case "dNumDevRes":
                this.dNumDevRes = ResponseUtil.getTextValue(value);
                break;
            case "dFeEmiSol":
                this.dFeEmiSol = ResponseUtil.getDateTimeValue(value);
                break;
            case "dFeEmiInf":
                this.dFeEmiInf = ResponseUtil.getDateTimeValue(value);
                break;
            case "dFeEmiRes":
                this.dFeEmiRes = ResponseUtil.getDateTimeValue(value);
                break;
        }
    }

    public String getId() {
        return Id;
    }

    public String getdNumDevSol() {
        return dNumDevSol;
    }

    public String getdNumDevInf() {
        return dNumDevInf;
    }

    public String getdNumDevRes() {
        return dNumDevRes;
    }

    public LocalDateTime getdFeEmiSol() {
        return dFeEmiSol;
    }

    public LocalDateTime getdFeEmiInf() {
        return dFeEmiInf;
    }

    public LocalDateTime getdFeEmiRes() {
        return dFeEmiRes;
    }
}
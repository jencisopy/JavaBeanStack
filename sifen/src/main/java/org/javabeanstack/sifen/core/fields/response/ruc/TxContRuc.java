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
package org.javabeanstack.sifen.core.fields.response.ruc;

import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

public class TxContRuc extends SifenObjectBase {
    private String dRUCCons;
    private String dRazCons;
    private String dCodEstCons;
    private String dDesEstCons;
    private String dRUCFactElec;

    @Override
    public void setValueFromChildNode(Node value) {
        switch (value.getLocalName()) {
            case "dRUCCons":
                dRUCCons = ResponseUtil.getTextValue(value);
                break;
            case "dRazCons":
                dRazCons = ResponseUtil.getTextValue(value);
                break;
            case "dCodEstCons":
                dCodEstCons = ResponseUtil.getTextValue(value);
                break;
            case "dDesEstCons":
                dDesEstCons = ResponseUtil.getTextValue(value);
                break;
            case "dRUCFactElec":
                dRUCFactElec = ResponseUtil.getTextValue(value);
                break;
        }
    }

    public String getdRUCCons() {
        return dRUCCons;
    }

    public String getdRazCons() {
        return dRazCons;
    }

    public String getdCodEstCons() {
        return dCodEstCons;
    }

    public String getdDesEstCons() {
        return dDesEstCons;
    }

    public String getdRUCFactElec() {
        return dRUCFactElec;
    }
}

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
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.time.LocalDate;

/**
 * Grupo E020 {@code gCompPub}: datos de la operación con el sector público (B2G).
 *
 * <p>Delta del fork 2.0.0-SNAPSHOT (plan JBSSIF, §1.4.5; ficha 9 del informe de línea base):
 * {@code dSecCont} pasa de {@code int} a {@code String} (campo, getter, setter y lectura sin
 * {@code parseInt}) para conservar los ceros a la izquierda de E024; la escritura sigue con
 * {@code String.valueOf} (escribe {@code "null"} si falta, I0-12). Se porta tal cual (D10).</p>
 */
public class TgCompPub extends SifenObjectBase {
    private String dModCont;
    private int dEntCont;
    private short dAnoCont;
    private String dSecCont;
    private LocalDate dFeCodCont;

    public void setupSOAPElements(SOAPElement gCamFE) throws SOAPException {
        SOAPElement gCompPub = gCamFE.addChildElement("gCompPub");
        gCompPub.addChildElement("dModCont").setTextContent(this.dModCont);
        gCompPub.addChildElement("dEntCont").setTextContent(String.valueOf(this.dEntCont));
        gCompPub.addChildElement("dAnoCont").setTextContent(String.valueOf(this.dAnoCont));
        gCompPub.addChildElement("dSecCont").setTextContent(String.valueOf(this.dSecCont));
        gCompPub.addChildElement("dFeCodCont").setTextContent(this.dFeCodCont.toString());
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "dModCont":
                this.dModCont = ResponseUtil.getTextValue(value);
                break;
            case "dEntCont":
                this.dEntCont = Integer.parseInt(ResponseUtil.getTextValue(value));
                break;
            case "dAnoCont":
                this.dAnoCont = Short.parseShort(ResponseUtil.getTextValue(value));
                break;
            case "dSecCont":
                this.dSecCont = ResponseUtil.getTextValue(value);
                break;
            case "dFeCodCont":
                this.dFeCodCont = ResponseUtil.getDateValue(value);
                break;
        }
    }

    public String getdModCont() {
        return dModCont;
    }

    public void setdModCont(String dModCont) {
        this.dModCont = dModCont;
    }

    public int getdEntCont() {
        return dEntCont;
    }

    public void setdEntCont(int dEntCont) {
        this.dEntCont = dEntCont;
    }

    public short getdAnoCont() {
        return dAnoCont;
    }

    public void setdAnoCont(short dAnoCont) {
        this.dAnoCont = dAnoCont;
    }

    public String getdSecCont() {
        return dSecCont;
    }

    public void setdSecCont(String dSecCont) {
        this.dSecCont = dSecCont;
    }

    public LocalDate getdFeCodCont() {
        return dFeCodCont;
    }

    public void setdFeCodCont(LocalDate dFeCodCont) {
        this.dFeCodCont = dFeCodCont;
    }
}

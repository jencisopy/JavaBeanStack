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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Grupo E820 {@code gGrupAdi}: campos complementarios comerciales de uso específico.
 *
 * <p>Delta del fork 2.0.0-SNAPSHOT (plan JBSSIF, §1.4.5; ficha 10 del informe de línea base):
 * campo {@code dCodConDncp} (E827, NT 020) con getter y setter, escrito entre {@code dContrato} y
 * {@code dSalAnt} y leído antes de {@code dSalAnt}.</p>
 */
public class TgGrupAdi extends SifenObjectBase {
    private String dCiclo;
    private LocalDate dFecIniC;
    private LocalDate dFecFinC;
    private List<LocalDate> dVencPagList;
    private String dContrato;
    private String dCodConDncp;
    private BigDecimal dSalAnt;

    public void setupSOAPElements(SOAPElement gCamEsp) throws SOAPException {
        SOAPElement gGrupAdi = gCamEsp.addChildElement("gGrupAdi");
        if (this.dCiclo != null) {
            gGrupAdi.addChildElement("dCiclo").setTextContent(this.dCiclo);
            gGrupAdi.addChildElement("dFecIniC").setTextContent(this.dFecIniC.toString());
            gGrupAdi.addChildElement("dFecFinC").setTextContent(this.dFecFinC.toString());
        }

        if (this.dVencPagList != null) {
            for (LocalDate dVencPag : this.dVencPagList) {
                gGrupAdi.addChildElement("dVencPag").setTextContent(dVencPag.toString());
            }
        }

        if (this.dContrato != null)
            gGrupAdi.addChildElement("dContrato").setTextContent(this.dContrato);

        if (this.dCodConDncp != null)
            gGrupAdi.addChildElement("dCodConDncp").setTextContent(this.dCodConDncp);

        if (this.dSalAnt != null)
            gGrupAdi.addChildElement("dSalAnt").setTextContent(String.valueOf(this.dSalAnt));
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "dCiclo":
                this.dCiclo = ResponseUtil.getTextValue(value);
                break;
            case "dFecIniC":
                this.dFecIniC = ResponseUtil.getDateValue(value);
                break;
            case "dFecFinC":
                this.dFecFinC = ResponseUtil.getDateValue(value);
                break;
            case "dVencPag":
                if (this.dVencPagList == null) {
                    this.dVencPagList = new ArrayList<>();
                }
                this.dVencPagList.add(ResponseUtil.getDateValue(value));
                break;
            case "dContrato":
                this.dContrato = ResponseUtil.getTextValue(value);
                break;
            case "dCodConDncp":
                this.dCodConDncp = ResponseUtil.getTextValue(value);
                break;
            case "dSalAnt":
                this.dSalAnt = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
        }
    }

    public String getdCiclo() {
        return dCiclo;
    }

    public void setdCiclo(String dCiclo) {
        this.dCiclo = dCiclo;
    }

    public LocalDate getdFecIniC() {
        return dFecIniC;
    }

    public void setdFecIniC(LocalDate dFecIniC) {
        this.dFecIniC = dFecIniC;
    }

    public LocalDate getdFecFinC() {
        return dFecFinC;
    }

    public void setdFecFinC(LocalDate dFecFinC) {
        this.dFecFinC = dFecFinC;
    }

    public List<LocalDate> getdVencPagList() {
        return dVencPagList;
    }

    public void setdVencPagList(List<LocalDate> dVencPagList) {
        this.dVencPagList = dVencPagList;
    }

    public String getdContrato() {
        return dContrato;
    }

    public void setdContrato(String dContrato) {
        this.dContrato = dContrato;
    }

    public BigDecimal getdSalAnt() {
        return dSalAnt;
    }

    public void setdSalAnt(BigDecimal dSalAnt) {
        this.dSalAnt = dSalAnt;
    }

    public String getdCodConDncp() {
        return dCodConDncp;
    }

    public void setdCodConDncp(String dCodConDncp) {
        this.dCodConDncp = dCodConDncp;
    }
}
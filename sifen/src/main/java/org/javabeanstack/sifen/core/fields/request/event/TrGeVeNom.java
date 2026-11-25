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
import org.javabeanstack.sifen.core.types.TiNatRec;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;

/**
 * Evento de nominación {@code rGEveNom} (GENFE001, NT 014/015/027).
 *
 * <p>Delta del fork 2.0.0-SNAPSHOT (plan JBSSIF, §1.4.4; ficha 2 del informe de línea base):
 * clase nueva del fork, reconstruida desde su bytecode. Se porta tal cual (D10), con sus defectos
 * conocidos: escribe la descripción de la ciudad como {@code dDesCiuEnt} en lugar de
 * {@code dDesCiuRec} (E0-12), compara {@code cDisRec != 0} antes de {@code cDisRec != null}
 * (E0-18), escribe {@code dDTipIDRec} con {@code String.valueOf} (I0-12) y conserva el campo
 * {@code iTipRec} sin uso (I0-11). El orden de los accesores sigue el de {@code javap -p} del
 * fork.</p>
 *
 * <p>Fase 6 (plan JBSSIF, §6.1, verificado campo por campo contra GENFE001–027 de la NT 014 y la NT 027): la
 * descripción de la ciudad sale como {@code dDesCiuRec} (GENFE022, E0-12), {@code cDisRec} nulo ya no da NPE
 * (E0-18) y {@code dDTipIDRec} sin cargar se omite en vez de escribir el literal {@code null} (I0-12; la validación de
 * eventos lo exige si hay {@code iTipIDRec}). El orden de los elementos es el de la NT 014.</p>
 */
public class TrGeVeNom extends SifenObjectBase {
    private String Id;
    private String mOtEve;
    private TiNatRec iTipRec;
    private short iNatRec;
    private short iTiOpe;
    private String cPaisRec;
    private String dDesPaisRe;
    private short iTiContRec;
    private String dRucRec;
    private short dDVRec;
    private short iTipIDRec;
    private String dDTipIDRec;
    private String dNumIDRec;
    private String dNomRec;
    private String dNomFanRec;
    private String dDirRec;
    private int dNumCasRec;
    private Short cDepRec;
    private String dDesDepRec;
    private Short cDisRec;
    private String dDesDisRec;
    private short cCiuRec;
    private String dDesCiuRec;
    private String dTelRec;
    private String dCelRec;
    private String dEmailRec;
    private String dCodCliente;

    public void setupSOAPElements(SOAPElement gGroupTiEvt) throws SOAPException {
        SOAPElement rGEveNom = gGroupTiEvt.addChildElement("rGEveNom");
        rGEveNom.addChildElement("Id").setTextContent(this.Id);
        rGEveNom.addChildElement("mOtEve").setTextContent(this.mOtEve);
        rGEveNom.addChildElement("iNatRec").setTextContent(String.valueOf(this.iNatRec));
        rGEveNom.addChildElement("iTiOpe").setTextContent(String.valueOf(this.iTiOpe));
        rGEveNom.addChildElement("cPaisRec").setTextContent(this.cPaisRec);
        rGEveNom.addChildElement("dDesPaisRe").setTextContent(this.dDesPaisRe);
        if (this.iNatRec == 1) {
            rGEveNom.addChildElement("iTiContRec").setTextContent(String.valueOf(this.iTiContRec));
            rGEveNom.addChildElement("dRucRec").setTextContent(this.dRucRec);
            rGEveNom.addChildElement("dDVRec").setTextContent(String.valueOf(this.dDVRec));
        } else {
            rGEveNom.addChildElement("iTipIDRec").setTextContent(String.valueOf(this.iTipIDRec));
            // I0-12: sin el literal «null» (GENFE011 es obligatorio con GENFE010: lo exige la validación de eventos)
            if (this.dDTipIDRec != null) {
                rGEveNom.addChildElement("dDTipIDRec").setTextContent(this.dDTipIDRec);
            }
            rGEveNom.addChildElement("dNumIDRec").setTextContent(this.dNumIDRec);
        }
        rGEveNom.addChildElement("dNomRec").setTextContent(this.dNomRec);
        if (this.dNomFanRec != null && !this.dNomFanRec.isEmpty()) {
            rGEveNom.addChildElement("dNomFanRec").setTextContent(this.dNomFanRec);
        }
        if (this.dDirRec != null && !this.dDirRec.isEmpty()) {
            rGEveNom.addChildElement("dDirRec").setTextContent(this.dDirRec);
            rGEveNom.addChildElement("dNumCasRec").setTextContent(String.valueOf(this.dNumCasRec));
            if (this.cDepRec != null) {
                rGEveNom.addChildElement("cDepRec").setTextContent(String.valueOf(this.cDepRec));
                rGEveNom.addChildElement("dDesDepRec").setTextContent(this.dDesDepRec);
            }
            // E0-18: primero el null (la línea base hacía el unboxing antes y daba NPE)
            if (this.cDisRec != null && this.cDisRec != 0) {
                rGEveNom.addChildElement("cDisRec").setTextContent(String.valueOf(this.cDisRec));
                rGEveNom.addChildElement("dDesDisRec").setTextContent(this.dDesDisRec);
            }
            if (this.cCiuRec != 0 && this.dDesCiuRec != null) {
                rGEveNom.addChildElement("cCiuRec").setTextContent(String.valueOf(this.cCiuRec));
                // E0-12: GENFE022 es dDesCiuRec (la línea base escribía dDesCiuEnt, copiado de gCamEnt)
                rGEveNom.addChildElement("dDesCiuRec").setTextContent(this.dDesCiuRec);
            }
        }
        if (this.dTelRec != null) {
            rGEveNom.addChildElement("dTelRec").setTextContent(this.dTelRec);
        }
        if (this.dCelRec != null) {
            rGEveNom.addChildElement("dCelRec").setTextContent(this.dCelRec);
        }
        if (this.dEmailRec != null) {
            rGEveNom.addChildElement("dEmailRec").setTextContent(this.dEmailRec);
        }
        if (this.dCodCliente != null) {
            rGEveNom.addChildElement("dCodCliente").setTextContent(this.dCodCliente);
        }
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "Id":
                this.Id = ResponseUtil.getTextValue(value);
                break;
            case "mOtEve":
                this.mOtEve = ResponseUtil.getTextValue(value);
                break;
            case "iNatRec":
                this.iNatRec = Short.parseShort(ResponseUtil.getTextValue(value));
                break;
            case "iTiOpe":
                this.iTiOpe = Short.parseShort(ResponseUtil.getTextValue(value));
                break;
            case "cPaisRec":
                this.cPaisRec = ResponseUtil.getTextValue(value);
                break;
            case "dDesPaisRe":
                this.dDesPaisRe = ResponseUtil.getTextValue(value);
                break;
            case "iTiContRec":
                this.iTiContRec = Short.parseShort(ResponseUtil.getTextValue(value));
                break;
            case "dRucRec":
                this.dRucRec = ResponseUtil.getTextValue(value);
                break;
            case "dDVRec":
                this.dDVRec = Short.parseShort(ResponseUtil.getTextValue(value));
                break;
            case "iTipIDRec":
                this.iTipIDRec = Short.parseShort(ResponseUtil.getTextValue(value));
                break;
            case "dDTipIDRec":
                this.dDTipIDRec = ResponseUtil.getTextValue(value);
                break;
            case "dNumIDRec":
                this.dNumIDRec = ResponseUtil.getTextValue(value);
                break;
            case "dNomRec":
                this.dNomRec = ResponseUtil.getTextValue(value);
                break;
            case "dNomFanRec":
                this.dNomFanRec = ResponseUtil.getTextValue(value);
                break;
            case "dDirRec":
                this.dDirRec = ResponseUtil.getTextValue(value);
                break;
            case "dNumCasRec":
                this.dNumCasRec = Integer.parseInt(ResponseUtil.getTextValue(value));
                break;
            case "cCiuRec":
                this.cCiuRec = Short.parseShort(ResponseUtil.getTextValue(value));
                break;
            case "dDesCiuRec":
                this.dDesCiuRec = ResponseUtil.getTextValue(value);
                break;
            case "cDepRec":
                this.cDepRec = Short.valueOf(ResponseUtil.getTextValue(value));
                break;
            case "dDesDepRec":
                this.dDesDepRec = ResponseUtil.getTextValue(value);
                break;
            case "cDisRec":
                this.cDisRec = Short.valueOf(ResponseUtil.getTextValue(value));
                break;
            case "dDesDisRec":
                this.dDesDisRec = ResponseUtil.getTextValue(value);
                break;
            case "dTelRec":
                this.dTelRec = ResponseUtil.getTextValue(value);
                break;
            case "dCelRec":
                this.dCelRec = ResponseUtil.getTextValue(value);
                break;
            case "dEmailRec":
                this.dEmailRec = ResponseUtil.getTextValue(value);
                break;
            case "dCodCliente":
                this.dCodCliente = ResponseUtil.getTextValue(value);
                break;
        }
    }

    public String getId() {
        return Id;
    }

    public void setId(String Id) {
        this.Id = Id;
    }

    public String getmOtEve() {
        return mOtEve;
    }

    public void setmOtEve(String mOtEve) {
        this.mOtEve = mOtEve;
    }

    public TiNatRec getiTipRec() {
        return iTipRec;
    }

    public void setiTipRec(TiNatRec iTipRec) {
        this.iTipRec = iTipRec;
    }

    public short getiNatRec() {
        return iNatRec;
    }

    public void setiNatRec(short iNatRec) {
        this.iNatRec = iNatRec;
    }

    public short getiTiOpe() {
        return iTiOpe;
    }

    public void setiTiOpe(short iTiOpe) {
        this.iTiOpe = iTiOpe;
    }

    public String getcPaisRec() {
        return cPaisRec;
    }

    public void setcPaisRec(String cPaisRec) {
        this.cPaisRec = cPaisRec;
    }

    public short getiTiContRec() {
        return iTiContRec;
    }

    public void setiTiContRec(short iTiContRec) {
        this.iTiContRec = iTiContRec;
    }

    public String getdRucRec() {
        return dRucRec;
    }

    public void setdRucRec(String dRucRec) {
        this.dRucRec = dRucRec;
    }

    public short getdDVRec() {
        return dDVRec;
    }

    public void setdDVRec(short dDVRec) {
        this.dDVRec = dDVRec;
    }

    public short getiTipIDRec() {
        return iTipIDRec;
    }

    public void setiTipIDRec(short iTipIDRec) {
        this.iTipIDRec = iTipIDRec;
    }

    public String getdNumIDRec() {
        return dNumIDRec;
    }

    public void setdNumIDRec(String dNumIDRec) {
        this.dNumIDRec = dNumIDRec;
    }

    public String getdNomRec() {
        return dNomRec;
    }

    public void setdNomRec(String dNomRec) {
        this.dNomRec = dNomRec;
    }

    public String getdNomFanRec() {
        return dNomFanRec;
    }

    public void setdNomFanRec(String dNomFanRec) {
        this.dNomFanRec = dNomFanRec;
    }

    public String getdDirRec() {
        return dDirRec;
    }

    public void setdDirRec(String dDirRec) {
        this.dDirRec = dDirRec;
    }

    public int getdNumCasRec() {
        return dNumCasRec;
    }

    public void setdNumCasRec(int dNumCasRec) {
        this.dNumCasRec = dNumCasRec;
    }

    public short getcCiuRec() {
        return cCiuRec;
    }

    public void setcCiuRec(short cCiuRec) {
        this.cCiuRec = cCiuRec;
    }

    public String getdTelRec() {
        return dTelRec;
    }

    public void setdTelRec(String dTelRec) {
        this.dTelRec = dTelRec;
    }

    public String getdCelRec() {
        return dCelRec;
    }

    public void setdCelRec(String dCelRec) {
        this.dCelRec = dCelRec;
    }

    public String getdEmailRec() {
        return dEmailRec;
    }

    public void setdEmailRec(String dEmailRec) {
        this.dEmailRec = dEmailRec;
    }

    public String getdCodCliente() {
        return dCodCliente;
    }

    public void setdCodCliente(String dCodCliente) {
        this.dCodCliente = dCodCliente;
    }

    public Short getcDepRec() {
        return cDepRec;
    }

    public void setcDepRec(Short cDepRec) {
        this.cDepRec = cDepRec;
    }

    public String getdDesDepRec() {
        return dDesDepRec;
    }

    public void setdDesDepRec(String dDesDepRec) {
        this.dDesDepRec = dDesDepRec;
    }

    public Short getcDisRec() {
        return cDisRec;
    }

    public void setcDisRec(Short cDisRec) {
        this.cDisRec = cDisRec;
    }

    public String getdDesDisRec() {
        return dDesDisRec;
    }

    public void setdDesDisRec(String dDesDisRec) {
        this.dDesDisRec = dDesDisRec;
    }

    public String getdDesCiuRec() {
        return dDesCiuRec;
    }

    public void setdDesCiuRec(String dDesCiuRec) {
        this.dDesCiuRec = dDesCiuRec;
    }

    public String getdDTipIDRec() {
        return dDTipIDRec;
    }

    public void setdDTipIDRec(String dDTipIDRec) {
        this.dDTipIDRec = dDTipIDRec;
    }

    public String getdDesPaisRe() {
        return dDesPaisRe;
    }

    public void setdDesPaisRe(String dDesPaisRe) {
        this.dDesPaisRe = dDesPaisRe;
    }
}

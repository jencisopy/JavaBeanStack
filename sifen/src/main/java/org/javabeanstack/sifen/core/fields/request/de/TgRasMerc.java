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
 * Grupo de rastreo de la mercadería ({@code gRasMerc}, E8.4 del MT; {@code tgRasMerc} del XSD).
 *
 * <p>Sale en el orden del XSD vigente de la DNIT: {@code dNumLote}, {@code dVencMerc}, {@code dNSerie},
 * {@code dNumPedi}, {@code dNumSegui}, {@code dNumReg} y {@code dNumRegEntCom}. {@code dNomPro} (E761, agregado por la
 * NT 010) todavía no está en la librería (plan JBSSIF, Fase 7).</p>
 *
 * <p>E12-08 (plan JBSSIF, decisión del usuario del 2026-11-25, ESC-44): la NT 010 (04/02/2022, apartado 1.4) eliminó
 * los campos del importador ({@code dNomImp} E756, {@code dDirImp} E757 y {@code dNumFir} E758), y SIFEN rechaza con
 * 0160 el DE que los trae. Sus seis accesores quedan deprecados y sin efecto en el XML: guardan y devuelven el valor,
 * se siguen leyendo de un XML anterior, pero no se escriben. Sus reglas de validación (JBS-E-99, JBS-E-100 y
 * JBS-E-101) están de baja.</p>
 */
public class TgRasMerc extends SifenObjectBase {
    private String dNumLote;
    private LocalDate dVencMerc;
    private String dNSerie;
    private String dNumPedi;
    private String dNumSegui;
    private String dNomImp;
    private String dDirImp;
    private String dNumFir;
    private String dNumReg;
    private String dNumRegEntCom;

    public void setupSOAPElements(SOAPElement gCamItem) throws SOAPException {
        SOAPElement gRasMerc = gCamItem.addChildElement("gRasMerc");
        if (this.dNumLote != null)
            gRasMerc.addChildElement("dNumLote").setTextContent(this.dNumLote);

        if (this.dVencMerc != null)
            gRasMerc.addChildElement("dVencMerc").setTextContent(this.dVencMerc.toString());

        if (this.dNSerie != null)
            gRasMerc.addChildElement("dNSerie").setTextContent(this.dNSerie);

        if (this.dNumPedi != null)
            gRasMerc.addChildElement("dNumPedi").setTextContent(this.dNumPedi);

        if (this.dNumSegui != null)
            gRasMerc.addChildElement("dNumSegui").setTextContent(this.dNumSegui);

        // E12-08: dNomImp, dDirImp y dNumFir no se escriben; la NT 010 los eliminó del grupo (SIFEN rechaza con 0160)

        if (this.dNumReg != null)
            gRasMerc.addChildElement("dNumReg").setTextContent(this.dNumReg);

        if (this.dNumRegEntCom != null)
            gRasMerc.addChildElement("dNumRegEntCom").setTextContent(this.dNumRegEntCom);
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "dNumLote":
                this.dNumLote = ResponseUtil.getTextValue(value);
                break;
            case "dVencMerc":
                this.dVencMerc = ResponseUtil.getDateValue(value);
                break;
            case "dNSerie":
                this.dNSerie = ResponseUtil.getTextValue(value);
                break;
            case "dNumPedi":
                this.dNumPedi = ResponseUtil.getTextValue(value);
                break;
            case "dNumSegui":
                this.dNumSegui = ResponseUtil.getTextValue(value);
                break;
            case "dNomImp":
                this.dNomImp = ResponseUtil.getTextValue(value);
                break;
            case "dDirImp":
                this.dDirImp = ResponseUtil.getTextValue(value);
                break;
            case "dNumFir":
                this.dNumFir = ResponseUtil.getTextValue(value);
                break;
            case "dNumReg":
                this.dNumReg = ResponseUtil.getTextValue(value);
                break;
            case "dNumRegEntCom":
                this.dNumRegEntCom = ResponseUtil.getTextValue(value);
                break;
        }
    }

    public String getdNumLote() {
        return dNumLote;
    }

    public void setdNumLote(String dNumLote) {
        this.dNumLote = dNumLote;
    }

    public LocalDate getdVencMerc() {
        return dVencMerc;
    }

    public void setdVencMerc(LocalDate dVencMerc) {
        this.dVencMerc = dVencMerc;
    }

    public String getdNSerie() {
        return dNSerie;
    }

    public void setdNSerie(String dNSerie) {
        this.dNSerie = dNSerie;
    }

    public String getdNumPedi() {
        return dNumPedi;
    }

    public void setdNumPedi(String dNumPedi) {
        this.dNumPedi = dNumPedi;
    }

    public String getdNumSegui() {
        return dNumSegui;
    }

    public void setdNumSegui(String dNumSegui) {
        this.dNumSegui = dNumSegui;
    }

    /**
     * Nombre del importador (E756).
     *
     * @return el valor cargado o leído de un XML anterior a la NT 010.
     * @deprecated eliminado por la NT 010 (04/02/2022, apartado 1.4); se conserva sin efecto en 2.x (guarda el valor pero no se escribe en el XML) y se retira en 3.0 (E12-08).
     */
    @Deprecated(since = "2.2.0")
    public String getdNomImp() {
        return dNomImp;
    }

    /**
     * Nombre del importador (E756).
     *
     * @param dNomImp se guarda, pero no se escribe en el XML.
     * @deprecated eliminado por la NT 010 (04/02/2022, apartado 1.4); se conserva sin efecto en 2.x (guarda el valor pero no se escribe en el XML) y se retira en 3.0 (E12-08).
     */
    @Deprecated(since = "2.2.0")
    public void setdNomImp(String dNomImp) {
        this.dNomImp = dNomImp;
    }

    /**
     * Dirección del importador (E757).
     *
     * @return el valor cargado o leído de un XML anterior a la NT 010.
     * @deprecated eliminado por la NT 010 (04/02/2022, apartado 1.4); se conserva sin efecto en 2.x (guarda el valor pero no se escribe en el XML) y se retira en 3.0 (E12-08).
     */
    @Deprecated(since = "2.2.0")
    public String getdDirImp() {
        return dDirImp;
    }

    /**
     * Dirección del importador (E757).
     *
     * @param dDirImp se guarda, pero no se escribe en el XML.
     * @deprecated eliminado por la NT 010 (04/02/2022, apartado 1.4); se conserva sin efecto en 2.x (guarda el valor pero no se escribe en el XML) y se retira en 3.0 (E12-08).
     */
    @Deprecated(since = "2.2.0")
    public void setdDirImp(String dDirImp) {
        this.dDirImp = dDirImp;
    }

    /**
     * Número de registro de la firma del importador (E758).
     *
     * @return el valor cargado o leído de un XML anterior a la NT 010.
     * @deprecated eliminado por la NT 010 (04/02/2022, apartado 1.4); se conserva sin efecto en 2.x (guarda el valor pero no se escribe en el XML) y se retira en 3.0 (E12-08).
     */
    @Deprecated(since = "2.2.0")
    public String getdNumFir() {
        return dNumFir;
    }

    /**
     * Número de registro de la firma del importador (E758).
     *
     * @param dNumFir se guarda, pero no se escribe en el XML.
     * @deprecated eliminado por la NT 010 (04/02/2022, apartado 1.4); se conserva sin efecto en 2.x (guarda el valor pero no se escribe en el XML) y se retira en 3.0 (E12-08).
     */
    @Deprecated(since = "2.2.0")
    public void setdNumFir(String dNumFir) {
        this.dNumFir = dNumFir;
    }

    public String getdNumReg() {
        return dNumReg;
    }

    public void setdNumReg(String dNumReg) {
        this.dNumReg = dNumReg;
    }

    public String getdNumRegEntCom() {
        return dNumRegEntCom;
    }

    public void setdNumRegEntCom(String dNumRegEntCom) {
        this.dNumRegEntCom = dNumRegEntCom;
    }
}

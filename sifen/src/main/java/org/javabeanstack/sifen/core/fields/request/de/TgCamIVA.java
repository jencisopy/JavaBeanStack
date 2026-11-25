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
import org.javabeanstack.sifen.core.types.CMondT;
import org.javabeanstack.sifen.core.types.TiAfecIVA;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Grupo E730 {@code gCamIVA}: IVA del ítem.
 *
 * <p>Delta del fork 2.0.0-SNAPSHOT (plan JBSSIF, §1.4.2; ficha 5 del informe de línea base):
 * {@code setupSOAPElements} reescrito con escala 2 también en PYG, {@code propIVA} con escala 3 y
 * la fórmula general {@code base = dTotOpeItem / (1 + dTasaIVA/100 * propIVA)} (sin comparar la
 * tasa con {@code equals}); conservaba la rama {@code if (scale == 0)} que nunca corre (I0-10). Se
 * portó tal cual (D10).</p>
 *
 * <p>Fase 4 (plan JBSSIF, §4.3, M3-41): se quita esa rama —sumaba 1 a {@code dBasGravIVA} con
 * importes enteros, el +1 que da el rechazo 1910— y la expresión {@code PYG ? 2 : 2}: la escala es 2 en
 * todas las monedas (D38 a, I3-41, aprobado en SIFEN-test con uno y con varios ítems parciales). Sin
 * cambio de salida.</p>
 *
 * <p>I4-11 (decisión del usuario, 2026-11-24): {@code dTasaIVA} de valor entero se guarda y se escribe sin
 * decimales ({@code 10.00} → {@code 10}), como pide el XSD; es el único campo {@code BigDecimal} del modelo
 * que el XSD define entero.</p>
 */
public class TgCamIVA extends SifenObjectBase {
    private TiAfecIVA iAfecIVA;
    private BigDecimal dPropIVA;
    private BigDecimal dTasaIVA;
    private BigDecimal dBasGravIVA;
    private BigDecimal dLiqIVAItem;
    private BigDecimal dBasExe;

    public void setupSOAPElements(GenerationCtx generationCtx, SOAPElement gCamItem, CMondT cMoneOpe, BigDecimal dTotOpeItem) throws SOAPException {
        SOAPElement gCamIVA = gCamItem.addChildElement("gCamIVA");
        gCamIVA.addChildElement("iAfecIVA").setTextContent(String.valueOf(this.iAfecIVA.getVal()));
        gCamIVA.addChildElement("dDesAfecIVA").setTextContent(this.iAfecIVA.getDescripcion());
        gCamIVA.addChildElement("dPropIVA").setTextContent(String.valueOf(this.dPropIVA));
        // I4-11: el XSD define dTasaIVA como xs:integer (tdTasaIVA); 10.00 se escribe 10
        gCamIVA.addChildElement("dTasaIVA").setTextContent(String.valueOf(integral(this.dTasaIVA)));
        // D38 (a), I3-41: 2 decimales también en PYG (M3-41: era «PYG ? 2 : 2»)
        int scale = 2;
        dTotOpeItem = dTotOpeItem.setScale(scale, RoundingMode.HALF_UP);

        BigDecimal hundred = BigDecimal.valueOf(100);
        BigDecimal propIVA = this.dPropIVA.divide(hundred, 3, RoundingMode.HALF_UP);
        BigDecimal factor = this.dTasaIVA.divide(hundred, 3, RoundingMode.HALF_UP).multiply(propIVA).add(BigDecimal.ONE);
        BigDecimal base = BigDecimal.ZERO;
        if (this.iAfecIVA.getVal() == 1 || this.iAfecIVA.getVal() == 4) {
            base = dTotOpeItem.divide(factor, scale, RoundingMode.HALF_UP);
            if (base.compareTo(BigDecimal.ZERO) == 0) {
                this.dBasGravIVA = BigDecimal.ZERO;
                this.dLiqIVAItem = BigDecimal.ZERO;
            } else {
                this.dLiqIVAItem = dTotOpeItem.subtract(base).setScale(scale, RoundingMode.HALF_UP);
                this.dBasGravIVA = base.multiply(propIVA).setScale(scale, RoundingMode.HALF_UP);
            }
        } else {
            this.dBasGravIVA = BigDecimal.ZERO;
            this.dLiqIVAItem = BigDecimal.ZERO;
        }


        gCamIVA.addChildElement("dBasGravIVA").setTextContent(String.valueOf(this.dBasGravIVA));
        gCamIVA.addChildElement("dLiqIVAItem").setTextContent(String.valueOf(this.dLiqIVAItem));

        // NT 013 (E737): dBasExe se escribe siempre (§6.3, D25; antes, solo con habilitarNotaTecnica13, E0-16)
        if (this.iAfecIVA.getVal() == 4) {
            // E737 = [100 * EA008 * (100 – E733)] / [10000 + (E734 * E733)]
            this.dBasExe = (dTotOpeItem.multiply(hundred.subtract(dPropIVA)).multiply(hundred)).divide((this.dTasaIVA.multiply(dPropIVA)).add(BigDecimal.valueOf(10000)), scale, RoundingMode.HALF_UP);
        } else {
            this.dBasExe = BigDecimal.valueOf(0);
        }
        gCamIVA.addChildElement("dBasExe").setTextContent(String.valueOf(this.dBasExe));

    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "iAfecIVA":
                this.iAfecIVA = TiAfecIVA.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
                break;
            case "dPropIVA":
                this.dPropIVA = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dTasaIVA":
                this.dTasaIVA = integral(new BigDecimal(ResponseUtil.getTextValue(value)));
                break;
            case "dBasGravIVA":
                this.dBasGravIVA = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dLiqIVAItem":
                this.dLiqIVAItem = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dBasExe":
                this.dBasExe = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
        }
    }

    public TiAfecIVA getiAfecIVA() {
        return iAfecIVA;
    }

    public void setiAfecIVA(TiAfecIVA iAfecIVA) {
        this.iAfecIVA = iAfecIVA;
    }

    public BigDecimal getdPropIVA() {
        return dPropIVA;
    }

    public void setdPropIVA(BigDecimal dPropIVA) {
        this.dPropIVA = dPropIVA;
    }

    public BigDecimal getdTasaIVA() {
        return dTasaIVA;
    }

    /**
     * Fija la tasa del IVA ({@code E734}: 0, 5 o 10). Una tasa de valor entero se guarda sin decimales
     * ({@code 10.00} → {@code 10}): el XSD la define como entero ({@code tdTasaIVA}, {@code xs:integer}) y la
     * validación previa rechaza la que tiene decimales ({@code JBS-E-92}). Una tasa con decimales reales
     * ({@code 10.5}) se conserva tal cual y la rechaza la validación (plan JBSSIF, I4-11).
     *
     * @param dTasaIVA tasa del IVA; puede ser {@code null}.
     */
    public void setdTasaIVA(BigDecimal dTasaIVA) {
        this.dTasaIVA = integral(dTasaIVA);
    }

    /**
     * I4-11: un valor entero pasa a escala 0 —con decimales ({@code 10.00}) o con escala negativa ({@code 1E+1},
     * lo que da {@code stripTrailingZeros()}, I4-14)—, para que se escriba {@code 10}; los demás, sin cambio.
     */
    private static BigDecimal integral(BigDecimal v) {
        if (v == null || v.scale() == 0) {
            return v;
        }
        if (v.scale() < 0) {
            return v.setScale(0); // I4-14: 1E+1 → 10 (String.valueOf escribía «1E+1»)
        }
        BigDecimal sinCeros = v.stripTrailingZeros();
        return sinCeros.scale() <= 0 ? sinCeros.setScale(0) : v;
    }

    public BigDecimal getdBasGravIVA() {
        return dBasGravIVA;
    }

    public void setdBasGravIVA(BigDecimal dBasGravIVA) {
        this.dBasGravIVA = dBasGravIVA;
    }

    public BigDecimal getdLiqIVAItem() {
        return dLiqIVAItem;
    }

    public void setdLiqIVAItem(BigDecimal dLiqIVAItem) {
        this.dLiqIVAItem = dLiqIVAItem;
    }

    public BigDecimal getdBasExe() {
        return dBasExe;
    }

    public void setdBasExe(BigDecimal dBasExe) {
        this.dBasExe = dBasExe;
    }

}
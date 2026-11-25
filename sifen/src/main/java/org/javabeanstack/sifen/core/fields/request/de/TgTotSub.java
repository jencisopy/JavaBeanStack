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
import org.javabeanstack.sifen.core.fields.util.RedondeoUtil;
import org.javabeanstack.sifen.core.types.CMondT;
import org.javabeanstack.sifen.core.types.TTImp;
import org.javabeanstack.sifen.core.types.TTiDE;
import org.javabeanstack.sifen.core.types.TdCondTiCam;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.javabeanstack.sifen.internal.util.SifenUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.javabeanstack.sifen.core.fields.util.FieldFormatUtil.formattdCRed;

/**
 * Grupo F001 {@code gTotSub}: subtotales y totales del DE.
 *
 * <p>Delta del fork 2.0.0-SNAPSHOT (plan JBSSIF, §1.4.1; ficha 4 del informe de línea base):
 * campo {@code redondeo} (primero de la clase, {@code true} por omisión) y constructor
 * {@code TgTotSub(Boolean)}; en {@code setupSOAPElements}: (a) escala 2 también en PYG,
 * (b) {@code dSubExe += dBasExe} y {@code dSub10}/{@code dSub5} sin la base exenta (NT 013),
 * (c) {@code dTotDescGlotem} multiplicado por {@code dCantProSer} (NT 001), (d) {@code dTotOpe}
 * redondeado a 0 decimales en PYG y 2 en otras monedas, (e) {@code dPorcDescTotal} solo con
 * descuento global y con escala 8, (f) {@code dRedon} según {@code redondeo} (caso del redondeo SEDECO del relevamiento, E0-01),
 * (g) {@code dTotDesc} y {@code dDescTotal} con {@code toPlainString()}. Se porta tal cual (D10):
 * E0-01, E0-16, E0-17 y E0-19 quedan abiertos para las fases siguientes.</p>
 *
 * <p>Fase 4 (plan JBSSIF): el flag {@code redondeo} sobrevive a la lectura del XML —se deduce de
 * {@code dRedon} ≠ 0— y se puede fijar con {@link #setRedondeo(Boolean)} (§4.2, E0-01, M0-02); la tasa
 * del IVA se compara por valor, {@code 10.00} suma igual que {@code 10} (§4.5, E0-17); la escala de
 * cálculo es 2 en todas las monedas (D38 a, I3-41).</p>
 */
public class TgTotSub extends SifenObjectBase {
    private Boolean redondeo = true;
    private BigDecimal dSubExe = BigDecimal.ZERO;
    private BigDecimal dSubExo = BigDecimal.ZERO;
    private BigDecimal dSub5 = BigDecimal.ZERO;
    private BigDecimal dSub10 = BigDecimal.ZERO;
    private BigDecimal dTotOpe = BigDecimal.ZERO;
    private BigDecimal dTotDesc = BigDecimal.ZERO;
    private BigDecimal dTotDescGlotem = BigDecimal.ZERO;
    private BigDecimal dTotAntItem = BigDecimal.ZERO;
    private BigDecimal dTotAnt = BigDecimal.ZERO;
    private BigDecimal dPorcDescTotal = BigDecimal.ZERO;
    private BigDecimal dDescTotal = BigDecimal.ZERO;
    private BigDecimal dAnticipo = BigDecimal.ZERO;
    private BigDecimal dRedon = BigDecimal.ZERO;
    private BigDecimal dComi = BigDecimal.ZERO;
    private BigDecimal dTotGralOpe = BigDecimal.ZERO;
    private BigDecimal dIVA5 = BigDecimal.ZERO;
    private BigDecimal dIVA10 = BigDecimal.ZERO;
    private BigDecimal dLiqTotIVA5 = BigDecimal.ZERO;
    private BigDecimal dLiqTotIVA10 = BigDecimal.ZERO;
    private BigDecimal dIVAComi = BigDecimal.ZERO;
    private BigDecimal dTotIVA = BigDecimal.ZERO;
    private BigDecimal dBaseGrav5 = BigDecimal.ZERO;
    private BigDecimal dBaseGrav10 = BigDecimal.ZERO;
    private BigDecimal dTBasGraIVA = BigDecimal.ZERO;
    private BigDecimal dTotalGs = BigDecimal.ZERO;

    /**
     * Crea el grupo con el redondeo SEDECO indicado.
     *
     * @param redondeo {@code true} aplica el redondeo (múltiplos de 50 Gs; 0,50 en otra moneda) a
     *                 {@code dTotGralOpe}; {@code false} no redondea; {@code null} redondea, como el
     *                 valor por omisión.
     */
    public TgTotSub(Boolean redondeo) {
        // I4-09: null se normaliza al valor por omisión, como en setRedondeo; getRedondeo() nunca da null
        this.redondeo = redondeo == null ? Boolean.TRUE : redondeo;
    }

    /** Crea el grupo con el redondeo SEDECO aplicado ({@code redondeo = true}, el valor por omisión). */
    public TgTotSub() {
    }

    /**
     * Indica si se aplica el redondeo SEDECO a {@code dTotGralOpe}. Por omisión {@code true}; al leer el DE
     * de un XML vale lo que dice su {@code dRedon}: distinto de cero ⇒ {@code true} (§4.2).
     *
     * <p>I4-09: el XML no guarda el flag, solo el redondeo que resultó. Un DE redondeado cuyo total era
     * múltiplo de 50 Gs (o de 0,50) se lee como «sin redondeo»: regenerarlo sin cambios da lo mismo, pero si
     * se le cambian ítems o importes el nuevo total no se redondea. Quien modifica un DE leído fija el
     * redondeo con {@link #setRedondeo(Boolean)} antes de regenerarlo.</p>
     *
     * @return el flag de redondeo.
     */
    public Boolean getRedondeo() {
        return redondeo;
    }

    /**
     * Fija si se aplica el redondeo SEDECO a {@code dTotGralOpe} (plan JBSSIF, §4.2, M0-02). Un DE con
     * total múltiplo de 50 Gs (o de 0,50) da {@code dRedon} 0 con o sin redondeo.
     *
     * @param redondeo {@code true} redondea; {@code false} no; {@code null} equivale a {@code true}, el
     *                 valor por omisión.
     */
    public void setRedondeo(Boolean redondeo) {
        this.redondeo = redondeo == null ? Boolean.TRUE : redondeo;
    }

    public void setupSOAPElements(SOAPElement DE, TTiDE iTiDE, TgDtipDE gDtipDE, TgOpeCom gOpeCom) throws SOAPException {
        resetValues();

        TTImp iTImp = gOpeCom.getiTImp();
        CMondT cMoneOpe = gOpeCom.getcMoneOpe();
        TdCondTiCam dCondTiCam = gOpeCom.getdCondTiCam();
        BigDecimal dTiCam = gOpeCom.getdTiCam();

        // D38 (a), I3-41: el IVA y los subtotales llevan 2 decimales también en PYG (con enteros SIFEN
        // rechaza 1910/2357); la línea base escribía «PYG ? 2 : 2» (M3-41).
        int scale = 2;

        SOAPElement gTotSub = DE.addChildElement("gTotSub");

        // CALCULOS
        for (TgCamItem gCamItem : gDtipDE.getgCamItemList()) {
            TgCamIVA gCamIVA = gCamItem.getgCamIVA();
            BigDecimal dTotOpeItem = gCamItem.getgValorItem().getgValorRestaItem().getdTotOpeItem();

            dTotOpeItem = dTotOpeItem.setScale(scale, RoundingMode.HALF_UP);
            if (gCamIVA != null) {
                BigDecimal dLiqIVAItem = gCamIVA.getdLiqIVAItem();
                BigDecimal dBasExe = gCamIVA.getdBasExe();
                BigDecimal dBasGravIVA = gCamIVA.getdBasGravIVA();

                if (gCamIVA.getiAfecIVA().getVal() == 1 || gCamIVA.getiAfecIVA().getVal() == 4) {
                    // §4.5 (E0-17): por valor; con equals, una tasa 10.00 no sumaba a ningún subtotal
                    if (gCamIVA.getdTasaIVA().compareTo(BigDecimal.valueOf(10)) == 0) {
                        this.dSubExe = this.dSubExe.add(dBasExe).setScale(scale);
                        this.dSub10 = this.dSub10.add(dTotOpeItem.subtract(dBasExe)).setScale(scale);
                        this.dIVA10 = this.dIVA10.add(dLiqIVAItem);
                        this.dBaseGrav10 = this.dBaseGrav10.add(dBasGravIVA);
                        this.dLiqTotIVA10 = BigDecimal.ZERO;
                    } else if (gCamIVA.getdTasaIVA().compareTo(BigDecimal.valueOf(5)) == 0) {
                        this.dSubExe = this.dSubExe.add(dBasExe).setScale(scale);
                        this.dSub5 = this.dSub5.add(dTotOpeItem.subtract(dBasExe)).setScale(scale);
                        this.dIVA5 = this.dIVA5.add(dLiqIVAItem);
                        this.dBaseGrav5 = this.dBaseGrav5.add(dBasGravIVA);
                        this.dLiqTotIVA5 = BigDecimal.ZERO;
                    }
                } else if (gCamIVA.getiAfecIVA().getVal() == 2) {
                    this.dSubExo = this.dSubExo.add(dTotOpeItem);
                } else if (gCamIVA.getiAfecIVA().getVal() == 3) {
                    this.dSubExe = this.dSubExe.add(dTotOpeItem);
                }
            }

            if (iTiDE.getVal() == 4)
                this.dTotOpe = this.dTotOpe.add(dTotOpeItem);

            this.dTotDesc = this.dTotDesc.add(SifenUtil.coalesce(gCamItem.getgValorItem().getgValorRestaItem().getdDescItem().multiply(gCamItem.getdCantProSer()), BigDecimal.ZERO));
            this.dTotDescGlotem = this.dTotDescGlotem.add(SifenUtil.coalesce(gCamItem.getgValorItem().getgValorRestaItem().getdDescGloItem().multiply(gCamItem.getdCantProSer()), BigDecimal.ZERO));
            // E3-25 (I0-13, NT 001, F034/F035): cada anticipo por la cantidad del ítem (EA006 × E711 y
            // EA007 × E711); la línea base sumaba el unitario. Un anticipo 0 no se multiplica, para no
            // cambiar la escala del total en los DE sin anticipo (golden intactos).
            this.dTotAntItem = this.dTotAntItem.add(porCantidad(gCamItem.getgValorItem().getgValorRestaItem().getdAntPreUniIt(), gCamItem.getdCantProSer()));
            this.dTotAnt = this.dTotAnt.add(porCantidad(gCamItem.getgValorItem().getgValorRestaItem().getdAntGloPreUniIt(), gCamItem.getdCantProSer()));

            if (!cMoneOpe.name().equals("PYG") && dCondTiCam.getVal() == 2)
                this.dTotalGs = this.dTotalGs.add(gCamItem.getgValorItem().getgValorRestaItem().getdTotOpeGs());
        }

        if (iTiDE.getVal() != 4 && (iTImp.getVal() == 1 || iTImp.getVal() == 3 || iTImp.getVal() == 4 || iTImp.getVal() == 5)) {
            this.dTotOpe = this.dSub10.add(this.dSub5).add(this.dSubExo).add(this.dSubExe);
            this.dTotOpe = this.dTotOpe.setScale(cMoneOpe == CMondT.PYG ? 0 : 2, RoundingMode.HALF_UP);
        }
        this.dDescTotal = this.dTotDesc.add(this.dTotDescGlotem);
        if (this.dTotDescGlotem.compareTo(BigDecimal.ZERO) != 0) {
            this.dPorcDescTotal = this.dTotDescGlotem.multiply(BigDecimal.valueOf(100)).divide(this.dTotOpe.add(this.dDescTotal), 8, RoundingMode.HALF_UP);
        }
        this.dAnticipo = this.dTotAntItem.add(this.dTotAnt);

        // §4.2: null vale como el valor por omisión (con redondeo); la línea base daba NPE
        if (!Boolean.FALSE.equals(this.redondeo)) {
            this.dRedon = RedondeoUtil.redondeoOficialSET(cMoneOpe, this.dTotOpe);
        } else {
            this.dRedon = BigDecimal.ZERO;
        }
        this.dTotGralOpe = this.dTotOpe.subtract(this.dRedon).add(SifenUtil.coalesce(this.dComi, BigDecimal.ZERO));
        this.dRedon = this.dRedon.abs();

        if (this.dComi != null) {
            this.dIVAComi = this.dComi.divide(BigDecimal.valueOf(1.1), scale, RoundingMode.HALF_UP);
        }

        this.dTotIVA = this.dIVA5.add(this.dIVA10).subtract(this.dLiqTotIVA5).subtract(this.dLiqTotIVA10).add(this.dIVAComi);
        this.dTBasGraIVA = this.dBaseGrav5.add(this.dBaseGrav10);

        if (!cMoneOpe.name().equals("PYG") && dCondTiCam.getVal() == 1)
            this.dTotalGs = this.dTotGralOpe.multiply(dTiCam);

        // INSERCIONES
        if (iTiDE.getVal() != 4) {
            gTotSub.addChildElement("dSubExe").setTextContent(String.valueOf(this.dSubExe));
            gTotSub.addChildElement("dSubExo").setTextContent(String.valueOf(this.dSubExo));

            if (iTImp.getVal() == 1 || iTImp.getVal() == 5) {
                gTotSub.addChildElement("dSub5").setTextContent(String.valueOf(this.dSub5));
                gTotSub.addChildElement("dSub10").setTextContent(String.valueOf(this.dSub10));
            }
        }

        gTotSub.addChildElement("dTotOpe").setTextContent(String.valueOf(this.dTotOpe));
        gTotSub.addChildElement("dTotDesc").setTextContent(String.valueOf(this.dTotDesc.toPlainString()));
        gTotSub.addChildElement("dTotDescGlotem").setTextContent(String.valueOf(this.dTotDescGlotem));
        gTotSub.addChildElement("dTotAntItem").setTextContent(String.valueOf(this.dTotAntItem));
        gTotSub.addChildElement("dTotAnt").setTextContent(String.valueOf(this.dTotAnt));
        gTotSub.addChildElement("dPorcDescTotal").setTextContent(String.valueOf(this.dPorcDescTotal));
        gTotSub.addChildElement("dDescTotal").setTextContent(String.valueOf(this.dDescTotal.toPlainString()));
        gTotSub.addChildElement("dAnticipo").setTextContent(String.valueOf(this.dAnticipo));
        // se agregó control para formatear con 4 decimales
        gTotSub.addChildElement("dRedon").setTextContent(formattdCRed(this.dRedon));

        if (iTiDE.getVal() != 4) {
            if (this.dComi != null)
                gTotSub.addChildElement("dComi").setTextContent(String.valueOf(this.dComi));
        }

        gTotSub.addChildElement("dTotGralOpe").setTextContent(String.valueOf(this.dTotGralOpe));

        if (iTiDE.getVal() != 4) {
            if (iTImp.getVal() == 1 || iTImp.getVal() == 5) {
                gTotSub.addChildElement("dIVA5").setTextContent(String.valueOf(this.dIVA5));
                gTotSub.addChildElement("dIVA10").setTextContent(String.valueOf(this.dIVA10));
            }
        }

        if (iTImp.getVal() == 1 || iTImp.getVal() == 5) {
            gTotSub.addChildElement("dLiqTotIVA5").setTextContent(String.valueOf(this.dLiqTotIVA5));
            gTotSub.addChildElement("dLiqTotIVA10").setTextContent(String.valueOf(this.dLiqTotIVA10));
        }

        if (iTiDE.getVal() != 4) {
            if (this.dComi != null)
                gTotSub.addChildElement("dIVAComi").setTextContent(String.valueOf(this.dIVAComi));

            if (iTImp.getVal() == 1 || iTImp.getVal() == 5) {
                gTotSub.addChildElement("dTotIVA").setTextContent(String.valueOf(this.dTotIVA));
                gTotSub.addChildElement("dBaseGrav5").setTextContent(String.valueOf(this.dBaseGrav5));
                gTotSub.addChildElement("dBaseGrav10").setTextContent(String.valueOf(this.dBaseGrav10));
                gTotSub.addChildElement("dTBasGraIVA").setTextContent(String.valueOf(this.dTBasGraIVA));
            }

            if (!cMoneOpe.name().equals("PYG"))
                gTotSub.addChildElement("dTotalGs").setTextContent(String.valueOf(this.dTotalGs));
        }
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "dSubExe":
                this.dSubExe = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dSubExo":
                this.dSubExo = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dSub5":
                this.dSub5 = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dSub10":
                this.dSub10 = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dTotOpe":
                this.dTotOpe = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dTotDesc":
                this.dTotDesc = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dTotDescGlotem":
                this.dTotDescGlotem = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dTotAntItem":
                this.dTotAntItem = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dTotAnt":
                this.dTotAnt = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dPorcDescTotal":
                this.dPorcDescTotal = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dDescTotal":
                this.dDescTotal = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dAnticipo":
                this.dAnticipo = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dRedon":
                this.dRedon = new BigDecimal(ResponseUtil.getTextValue(value));
                // §4.2 (E0-01): el flag sobrevive a la lectura; antes quedaba en true y el DE releído
                // salía redondeado (y firmado de nuevo con otros totales)
                this.redondeo = this.dRedon.signum() != 0;
                break;
            case "dComi":
                this.dComi = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dTotGralOpe":
                this.dTotGralOpe = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dIVA5":
                this.dIVA5 = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dIVA10":
                this.dIVA10 = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dLiqTotIVA5":
                this.dLiqTotIVA5 = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dLiqTotIVA10":
                this.dLiqTotIVA10 = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dIVAComi":
                this.dIVAComi = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dTotIVA":
                this.dTotIVA = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dBaseGrav5":
                this.dBaseGrav5 = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dBaseGrav10":
                this.dBaseGrav10 = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dTBasGraIVA":
                this.dTBasGraIVA = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
            case "dTotalGs":
                this.dTotalGs = new BigDecimal(ResponseUtil.getTextValue(value));
                break;
        }
    }

    private void resetValues() {
        dSubExe = BigDecimal.ZERO;
        dSubExo = BigDecimal.ZERO;
        dSub5 = BigDecimal.ZERO;
        dSub10 = BigDecimal.ZERO;
        dTotOpe = BigDecimal.ZERO;
        dTotDesc = BigDecimal.ZERO;
        dTotDescGlotem = BigDecimal.ZERO;
        dTotAntItem = BigDecimal.ZERO;
        dTotAnt = BigDecimal.ZERO;
        dPorcDescTotal = BigDecimal.ZERO;
        dDescTotal = BigDecimal.ZERO;
        dAnticipo = BigDecimal.ZERO;
        dRedon = BigDecimal.ZERO;
        dComi = BigDecimal.ZERO;
        dTotGralOpe = BigDecimal.ZERO;
        dIVA5 = BigDecimal.ZERO;
        dIVA10 = BigDecimal.ZERO;
        dLiqTotIVA5 = BigDecimal.ZERO;
        dLiqTotIVA10 = BigDecimal.ZERO;
        dIVAComi = BigDecimal.ZERO;
        dTotIVA = BigDecimal.ZERO;
        dBaseGrav5 = BigDecimal.ZERO;
        dBaseGrav10 = BigDecimal.ZERO;
        dTBasGraIVA = BigDecimal.ZERO;
        dTotalGs = BigDecimal.ZERO;
    }

    public BigDecimal getdSubExe() {
        return dSubExe;
    }

    public BigDecimal getdSubExo() {
        return dSubExo;
    }

    public BigDecimal getdSub5() {
        return dSub5;
    }

    public BigDecimal getdSub10() {
        return dSub10;
    }

    public BigDecimal getdTotOpe() {
        return dTotOpe;
    }

    public BigDecimal getdTotDesc() {
        return dTotDesc;
    }

    public BigDecimal getdTotDescGlotem() {
        return dTotDescGlotem;
    }

    public BigDecimal getdTotAntItem() {
        return dTotAntItem;
    }

    public BigDecimal getdTotAnt() {
        return dTotAnt;
    }

    public BigDecimal getdPorcDescTotal() {
        return dPorcDescTotal;
    }

    public BigDecimal getdDescTotal() {
        return dDescTotal;
    }

    public BigDecimal getdAnticipo() {
        return dAnticipo;
    }

    public BigDecimal getdRedon() {
        return dRedon;
    }

    public BigDecimal getdComi() {
        return dComi;
    }

    public void setdComi(BigDecimal dComi) {
        this.dComi = dComi;
    }

    public BigDecimal getdTotGralOpe() {
        return dTotGralOpe;
    }

    public BigDecimal getdIVA5() {
        return dIVA5;
    }

    public BigDecimal getdIVA10() {
        return dIVA10;
    }

    public BigDecimal getdLiqTotIVA5() {
        return dLiqTotIVA5;
    }

    public BigDecimal getdLiqTotIVA10() {
        return dLiqTotIVA10;
    }

    public BigDecimal getdIVAComi() {
        return dIVAComi;
    }

    public BigDecimal getdTotIVA() {
        return dTotIVA;
    }

    public BigDecimal getdBaseGrav5() {
        return dBaseGrav5;
    }

    public BigDecimal getdBaseGrav10() {
        return dBaseGrav10;
    }

    public BigDecimal getdTBasGraIVA() {
        return dTBasGraIVA;
    }

    public BigDecimal getdTotalGs() {
        return dTotalGs;
    }

    /** E3-25: anticipo unitario × cantidad; nulo o cero, cero sin escala. */
    private static BigDecimal porCantidad(BigDecimal anticipo, BigDecimal cantidad) {
        if (anticipo == null || anticipo.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return anticipo.multiply(cantidad);
    }
}

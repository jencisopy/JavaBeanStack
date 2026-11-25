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
package org.javabeanstack.sifen.test.calculo;

import org.javabeanstack.sifen.core.fields.request.de.TgCamIVA;
import org.javabeanstack.sifen.core.types.CMondT;
import org.javabeanstack.sifen.core.types.TiAfecIVA;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.internal.helpers.SoapHelper;
import jakarta.xml.soap.SOAPElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.NodeList;

import java.math.BigDecimal;

/**
 * IVA por ítem de {@code TgCamIVA} en la línea base del fork (plan JBSSIF, §2.4; ficha 5 del
 * informe de línea base): {@code base = dTotOpeItem / (1 + tasa/100 × prop/100)} con escala 2 en
 * todas las monedas, y {@code dBasExe} (NT 13, E737) solo en gravado parcial.
 *
 * <p>E0-04 (fuente local: proporción redondeada a 0/1 en PYG y {@code equals(10)} sensible a la
 * escala) queda <b>descartado para este método</b> en el fork: los casos de proporción 30 % en PYG y
 * de tasa {@code 10.00} dan la base correcta. Lo que sigue sensible a la escala es {@code TgTotSub}
 * (E0-17).</p>
 */
public class TgCamIVATest {

    private static TgCamIVA iva(TiAfecIVA afectacion, String prop, String tasa) {
        TgCamIVA iva = new TgCamIVA();
        iva.setiAfecIVA(afectacion);
        iva.setdPropIVA(new BigDecimal(prop));
        iva.setdTasaIVA(new BigDecimal(tasa));
        return iva;
    }

    private static SOAPElement calcular(TgCamIVA iva, CMondT moneda, String total, boolean nt13) throws Exception {
        // §6.3 (D25): nt13 ya no cambia nada; dBasExe se escribe siempre
        GenerationCtx ctx = new GenerationCtx();
        SOAPElement item = SoapHelper.createSoapMessage().getSOAPBody().addChildElement("gCamItem");
        iva.setupSOAPElements(ctx, item, moneda, new BigDecimal(total));
        return item;
    }

    private static String texto(SOAPElement padre, String nombre) {
        NodeList l = padre.getElementsByTagName(nombre);
        return l.getLength() == 0 ? null : l.item(0).getTextContent();
    }

    @Test
    public void gravado10EnGuaranies() throws Exception {
        TgCamIVA iva = iva(TiAfecIVA.GRAVADO, "100", "10");
        SOAPElement item = calcular(iva, CMondT.PYG, "110000", true);
        Assertions.assertEquals(new BigDecimal("100000.00"), iva.getdBasGravIVA());
        Assertions.assertEquals(new BigDecimal("10000.00"), iva.getdLiqIVAItem());
        Assertions.assertEquals(BigDecimal.valueOf(0), iva.getdBasExe());
        Assertions.assertEquals("100000.00", texto(item, "dBasGravIVA"), "escala 2 también en PYG (fork)");
        Assertions.assertEquals("0", texto(item, "dBasExe"));
    }

    @Test
    public void gravado5EnGuaranies() throws Exception {
        TgCamIVA iva = iva(TiAfecIVA.GRAVADO, "100", "5");
        calcular(iva, CMondT.PYG, "105000", true);
        Assertions.assertEquals(new BigDecimal("100000.00"), iva.getdBasGravIVA());
        Assertions.assertEquals(new BigDecimal("5000.00"), iva.getdLiqIVAItem());
    }

    /** Proporción parcial en PYG: en el fuente local daba 0 (E0-04); en el fork, la fórmula E735. */
    @Test
    public void gravadoParcial30EnGuaraniesE004() throws Exception {
        TgCamIVA iva = iva(TiAfecIVA.GRAVADO_PARCIAL, "30", "10");
        SOAPElement item = calcular(iva, CMondT.PYG, "1000000", true);
        // base = 1000000.00 / 1.030000 = 970873.79; IVA = 29126.21; gravada = base × 0.300
        Assertions.assertEquals(new BigDecimal("29126.21"), iva.getdLiqIVAItem());
        Assertions.assertEquals(new BigDecimal("291262.14"), iva.getdBasGravIVA());
        // E737 = 100 × EA008 × (100 − E733) / (10000 + E734 × E733) = 7000000000.00 / 10300
        Assertions.assertEquals(new BigDecimal("679611.65"), iva.getdBasExe());
        Assertions.assertEquals("679611.65", texto(item, "dBasExe"));
    }

    @Test
    public void gravadoParcial50Al5EnDolares() throws Exception {
        TgCamIVA iva = iva(TiAfecIVA.GRAVADO_PARCIAL, "50", "5");
        calcular(iva, CMondT.USD, "1500.75", true);
        Assertions.assertEquals(new BigDecimal("36.60"), iva.getdLiqIVAItem());
        Assertions.assertEquals(new BigDecimal("732.08"), iva.getdBasGravIVA());
        Assertions.assertEquals(new BigDecimal("732.07"), iva.getdBasExe());
    }

    /** dTasaIVA con escala 2 da lo mismo que 10 (la parte de E0-04 que el fork resolvió). */
    @Test
    public void tasaConEscalaDosE004() throws Exception {
        TgCamIVA iva = iva(TiAfecIVA.GRAVADO, "100", "10.00");
        calcular(iva, CMondT.PYG, "110000", true);
        Assertions.assertEquals(new BigDecimal("10000.00"), iva.getdLiqIVAItem());
        Assertions.assertEquals(new BigDecimal("100000.00"), iva.getdBasGravIVA());
    }

    @Test
    public void exentoYExonerado() throws Exception {
        for (TiAfecIVA a : new TiAfecIVA[]{TiAfecIVA.EXENTO, TiAfecIVA.EXONERADO}) {
            TgCamIVA iva = iva(a, "0", "0");
            calcular(iva, CMondT.PYG, "30000", true);
            Assertions.assertEquals(BigDecimal.ZERO, iva.getdBasGravIVA(), a.name());
            Assertions.assertEquals(BigDecimal.ZERO, iva.getdLiqIVAItem(), a.name());
            Assertions.assertEquals(BigDecimal.valueOf(0), iva.getdBasExe(), a.name());
        }
    }

    /** §6.3 (D25): dBasExe se calcula y se escribe siempre; la línea base, con la NT 13 apagada, lo dejaba null (E0-16). */
    @Test
    public void dBasExeSeEscribeSiempreE016() throws Exception {
        TgCamIVA iva = iva(TiAfecIVA.GRAVADO, "100", "10");
        SOAPElement item = calcular(iva, CMondT.PYG, "110000", false);
        Assertions.assertEquals(BigDecimal.valueOf(0), iva.getdBasExe());
        Assertions.assertEquals("0", texto(item, "dBasExe"));
    }
}

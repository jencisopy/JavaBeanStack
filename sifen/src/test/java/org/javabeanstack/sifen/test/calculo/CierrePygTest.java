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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.fields.request.de.TgCamItem;
import org.javabeanstack.sifen.core.fields.request.de.TgTotSub;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Cierre del IVA con base gravada parcial en PYG (plan JBSSIF, §2.1.6; pedido del usuario del
 * 2026-11-24: «el redondeo del exento difería por 1 Gs … enviaba gravado y exento con decimales»).
 * Muestra fija, sin red:
 * <ul>
 *   <li>{@link CierrePyg} reproduce exactamente lo que escribe la librería (escala 2), así el barrido de
 *       {@code jbs_sifen_insumos/fase3/redondeo_pyg/} mide lo que la librería hace;</li>
 *   <li>con escala 2 las identidades del MT (E735 + E736 + E737 = EA008 por ítem; F004a dSub5 =
 *       Σ(E735 + E736)) cierran a 0,01 o mejor; con escala 0 (enteros, como el fork antes) hay importes
 *       que no cierran por 1 Gs (1.015.839, 1.055.434, 1.102.948), que es lo que veía el usuario.</li>
 * </ul>
 */
public class CierrePygTest {
    static final long[] FIJOS = {1015839, 1055434, 1102948, 12345679, 1100000, 1, 15, 47789, 7000000, 999999999};

    static long[] muestra() {
        Random azar = new Random(20261124L);
        long[] m = new long[FIJOS.length + 40];
        System.arraycopy(FIJOS, 0, m, 0, FIJOS.length);
        for (int i = FIJOS.length; i < m.length; i++) {
            m[i] = 1 + (long) (azar.nextDouble() * 1e10);
        }
        return m;
    }

    /** La librería (escala 2) escribe lo mismo que la emulación, en el 5 % y el 10 % sobre el 30 %. */
    @Test
    public void laEmulacionEsLaLibreria() throws Exception {
        SifenConfig cfg = FixturesSifen.configuracion();
        GenerationCtx ctx = FixturesSifen.contexto(cfg);
        for (int tasa : new int[] {5, 10}) {
            for (long monto : muestra()) {
                DocumentoElectronico de = CasosDePrueba.documentos().stream()
                        .filter(c -> c.getId().equals("DE-26")).findFirst().orElseThrow().construir();
                List<TgCamItem> items = de.getgDtipDE().getgCamItemList();
                TgCamItem lote = items.get(0);
                items.clear();
                items.add(lote);
                lote.getgValorItem().setdPUniProSer(BigDecimal.valueOf(monto));
                lote.getgCamIVA().setdTasaIVA(BigDecimal.valueOf(tasa));
                de.setgTotSub(new TgTotSub(false));
                String xml = de.generarXml(ctx, cfg);
                CierrePyg.Item e = CierrePyg.item(BigDecimal.valueOf(monto), 30, tasa, 2);
                Assertions.assertEquals(e.grav().toPlainString(), valor(xml, "dBasGravIVA"), monto + " dBasGravIVA");
                Assertions.assertEquals(e.liq().toPlainString(), valor(xml, "dLiqIVAItem"), monto + " dLiqIVAItem");
                Assertions.assertEquals(e.exe().toPlainString(), valor(xml, "dBasExe"), monto + " dBasExe");
            }
        }
    }

    /** Escala 2: los cierres del MT dan 0 o ±0,01 por ítem (nunca 1 Gs). */
    @Test
    public void conDecimalesCierraA001() {
        for (int tasa : new int[] {5, 10}) {
            for (long monto : muestra()) {
                CierrePyg.Item e = CierrePyg.item(BigDecimal.valueOf(monto), 30, tasa, 2);
                BigDecimal cierre = e.grav().add(e.liq()).add(e.exe()).subtract(e.total()).abs();
                Assertions.assertTrue(cierre.compareTo(new BigDecimal("0.01")) <= 0, monto + ": " + cierre);
                for (String f : CierrePyg.fallasTotales(List.of(e), 2)) {
                    BigDecimal d = new BigDecimal(f.substring(f.indexOf('=') + 1)).abs();
                    Assertions.assertTrue(d.compareTo(new BigDecimal("0.01")) <= 0, monto + ": " + f);
                }
            }
        }
    }

    /** Escala 0 (enteros): los importes del barrido no cierran por 1 Gs en el ítem y en F004a. */
    @Test
    public void conEnterosNoCierraPorUnGuarani() {
        for (long monto : new long[] {1015839, 1055434, 1102948}) {
            CierrePyg.Item e = CierrePyg.item(BigDecimal.valueOf(monto), 30, 5, 0);
            Assertions.assertEquals(BigDecimal.ONE, e.grav().add(e.liq()).add(e.exe()).subtract(e.total()), "ítem " + monto);
            List<String> t = CierrePyg.fallasTotales(List.of(e), 0);
            Assertions.assertTrue(t.contains("F004a dSub5−Σ(E735+E736)=-1"), monto + ": " + t);
        }
        List<String> conDecimales = new ArrayList<>(CierrePyg.fallasItem(CierrePyg.item(BigDecimal.valueOf(1015839), 30, 5, 2), 30, 2));
        Assertions.assertTrue(conDecimales.isEmpty(), "1.015.839 cierra con decimales: " + conDecimales);
    }

    static String valor(String xml, String campo) {
        Matcher m = Pattern.compile("<(?:\\w+:)?" + campo + ">([^<]*)<").matcher(xml);
        return m.find() ? m.group(1) : null;
    }
}

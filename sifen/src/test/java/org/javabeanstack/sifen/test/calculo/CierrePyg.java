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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Identidades de cierre del IVA por ítem y de gTotSub en PYG con base gravada parcial (plan JBSSIF,
 * §2.1.6; pedido del usuario del 2026-11-24). Reproduce las fórmulas de {@code TgCamIVA} y
 * {@code TgTotSub} con la escala como parámetro: 2 (la del fork y de jbs-sifen hoy, «PYG ? 2 : 2») o 0
 * (enteros, como el fork antes del ajuste del usuario).
 */
public final class CierrePyg {
    static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private CierrePyg() {
    }

    /** Resultado de un ítem: total, base gravada (E735), IVA (E736), exento (E737). */
    public record Item(BigDecimal total, BigDecimal grav, BigDecimal liq, BigDecimal exe, int tasa) {
    }

    /** Fórmulas de {@code TgCamIVA.setupSOAPElements} con escala {@code s} (gravado parcial, NT 13 activa). */
    public static Item item(BigDecimal total, int prop, int tasa, int s) {
        BigDecimal t = total.setScale(s, RoundingMode.HALF_UP);
        BigDecimal p = BigDecimal.valueOf(prop);
        BigDecimal propIVA = p.divide(CIEN, 3, RoundingMode.HALF_UP);
        BigDecimal factor = BigDecimal.valueOf(tasa).divide(CIEN, 3, RoundingMode.HALF_UP).multiply(propIVA).add(BigDecimal.ONE);
        BigDecimal base = t.divide(factor, s, RoundingMode.HALF_UP);
        BigDecimal liq = t.subtract(base).setScale(s, RoundingMode.HALF_UP);
        BigDecimal grav = base.multiply(propIVA).setScale(s, RoundingMode.HALF_UP);
        BigDecimal exe = t.multiply(CIEN.subtract(p)).multiply(CIEN)
                .divide(BigDecimal.valueOf(tasa).multiply(p).add(BigDecimal.valueOf(10000)), s, RoundingMode.HALF_UP);
        if (s == 0 && grav.divide(base, 8, RoundingMode.DOWN).compareTo(propIVA) < 0) {
            grav = grav.add(BigDecimal.ONE); // ajuste de TgCamIVA para escala 0
        }
        return new Item(t, grav, liq, exe, tasa);
    }

    /** Valores exactos del MT (E735, E736 = E735 × tasa/100, E737), sin redondear. */
    public static BigDecimal[] mt(BigDecimal total, int prop, int tasa) {
        BigDecimal den = BigDecimal.valueOf(10000L + (long) tasa * prop);
        BigDecimal grav = total.multiply(CIEN).multiply(BigDecimal.valueOf(prop)).divide(den, 20, RoundingMode.HALF_UP);
        BigDecimal liq = grav.multiply(BigDecimal.valueOf(tasa)).divide(CIEN, 20, RoundingMode.HALF_UP);
        BigDecimal exe = total.multiply(CIEN).multiply(BigDecimal.valueOf(100 - prop)).divide(den, 20, RoundingMode.HALF_UP);
        return new BigDecimal[] {grav, liq, exe};
    }

    /** Diferencias de un ítem: E735+E736+E737 − EA008; E736 − E735×tasa/100 (redondeado a s); E735 − MT redondeado a s. */
    public static List<String> fallasItem(Item i, int prop, int s) {
        List<String> f = new ArrayList<>();
        BigDecimal cierre = i.grav().add(i.liq()).add(i.exe()).subtract(i.total());
        if (cierre.signum() != 0) {
            f.add("E735+E736+E737−EA008=" + cierre.toPlainString());
        }
        BigDecimal liqMt = i.grav().multiply(BigDecimal.valueOf(i.tasa())).divide(CIEN, s, RoundingMode.HALF_UP);
        BigDecimal dLiq = i.liq().subtract(liqMt);
        if (dLiq.signum() != 0) {
            f.add("E736−E735×t/100=" + dLiq.toPlainString());
        }
        BigDecimal[] m = mt(i.total(), prop, i.tasa());
        BigDecimal dGrav = i.grav().subtract(m[0].setScale(s, RoundingMode.HALF_UP));
        if (dGrav.signum() != 0) {
            f.add("E735−MT=" + dGrav.toPlainString());
        }
        BigDecimal dExe = i.exe().subtract(m[2].setScale(s, RoundingMode.HALF_UP));
        if (dExe.signum() != 0) {
            f.add("E737−MT=" + dExe.toPlainString());
        }
        return f;
    }

    /**
     * Diferencias de gTotSub (fórmulas de {@code TgTotSub}): F004a dSub5 (librería: Σ(EA008 − E737)) contra
     * el MT Σ(E735 + E736) de los parciales; F008 dTotOpe (escala 0 en PYG) contra Σ subtotales escritos.
     */
    public static List<String> fallasTotales(List<Item> items, int s) {
        List<String> f = new ArrayList<>();
        BigDecimal sub5 = BigDecimal.ZERO, sub10 = BigDecimal.ZERO, subExe = BigDecimal.ZERO;
        BigDecimal mt5 = BigDecimal.ZERO, mt10 = BigDecimal.ZERO, tot = BigDecimal.ZERO;
        for (Item i : items) {
            BigDecimal sub = i.total().subtract(i.exe()).setScale(s, RoundingMode.HALF_UP);
            subExe = subExe.add(i.exe());
            tot = tot.add(i.total());
            if (i.tasa() == 5) {
                sub5 = sub5.add(sub);
                mt5 = mt5.add(i.grav()).add(i.liq());
            } else {
                sub10 = sub10.add(sub);
                mt10 = mt10.add(i.grav()).add(i.liq());
            }
        }
        if (sub5.compareTo(mt5) != 0) {
            f.add("F004a dSub5−Σ(E735+E736)=" + sub5.subtract(mt5).toPlainString());
        }
        if (sub10.compareTo(mt10) != 0) {
            f.add("F005a dSub10−Σ(E735+E736)=" + sub10.subtract(mt10).toPlainString());
        }
        BigDecimal dTotOpe = sub5.add(sub10).add(subExe).setScale(0, RoundingMode.HALF_UP);
        BigDecimal suma = sub5.add(sub10).add(subExe);
        if (dTotOpe.compareTo(suma) != 0) {
            f.add("F008 dTotOpe−ΣF002..F005=" + dTotOpe.subtract(suma).toPlainString());
        }
        if (suma.compareTo(tot) != 0) {
            f.add("ΣF002..F005−ΣEA008=" + suma.subtract(tot).toPlainString());
        }
        return f;
    }
}

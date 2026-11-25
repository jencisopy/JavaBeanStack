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

import jakarta.xml.soap.SOAPElement;
import java.math.BigDecimal;
import java.util.Iterator;
import org.javabeanstack.sifen.core.fields.request.de.TgDtipDE;
import org.javabeanstack.sifen.core.fields.request.de.TgOpeCom;
import org.javabeanstack.sifen.core.fields.request.de.TgTotSub;
import org.javabeanstack.sifen.core.types.TTiDE;

/**
 * Solo para la prueba en vivo de §2.1.6: reproduce lo que el fork mandaba antes del ajuste del usuario
 * (importes del IVA en <b>enteros</b> en PYG). Después de que {@link TgTotSub} escribe su grupo, y antes
 * de la firma, reescribe en el DOM del DE los importes de cada ítem gravado parcial al 5 % sobre el 30 %
 * y los de {@code gTotSub} con las fórmulas de {@link CierrePyg} con escala 0. No va a producción.
 */
public class TgTotSubEnteros extends TgTotSub {

    public TgTotSubEnteros(Boolean redondeo) {
        super(redondeo);
    }

    @Override
    public void setupSOAPElements(SOAPElement de, TTiDE iTiDE, TgDtipDE gDtipDE, TgOpeCom gOpeCom) throws jakarta.xml.soap.SOAPException {
        super.setupSOAPElements(de, iTiDE, gDtipDE, gOpeCom);
        BigDecimal exe = BigDecimal.ZERO, sub5 = BigDecimal.ZERO, iva5 = BigDecimal.ZERO, base5 = BigDecimal.ZERO;
        for (SOAPElement item : hijos(hijo(de, "gDtipDE"), "gCamItem")) {
            SOAPElement iva = hijo(item, "gCamIVA");
            BigDecimal total = new BigDecimal(texto(hijo(hijo(hijo(item, "gValorItem"), "gValorRestaItem"), "dTotOpeItem")));
            CierrePyg.Item e = CierrePyg.item(total, 30, 5, 0);
            poner(iva, "dBasGravIVA", e.grav());
            poner(iva, "dLiqIVAItem", e.liq());
            poner(iva, "dBasExe", e.exe());
            exe = exe.add(e.exe());
            sub5 = sub5.add(e.total().subtract(e.exe()));
            iva5 = iva5.add(e.liq());
            base5 = base5.add(e.grav());
        }
        SOAPElement tot = hijo(de, "gTotSub");
        poner(tot, "dSubExe", exe);
        poner(tot, "dSub5", sub5);
        poner(tot, "dIVA5", iva5);
        poner(tot, "dTotIVA", iva5);
        poner(tot, "dBaseGrav5", base5);
        poner(tot, "dTBasGraIVA", base5);
    }

    private static SOAPElement hijo(SOAPElement padre, String nombre) {
        for (SOAPElement e : hijos(padre, nombre)) {
            return e;
        }
        throw new IllegalStateException("falta " + nombre);
    }

    private static java.util.List<SOAPElement> hijos(SOAPElement padre, String nombre) {
        java.util.List<SOAPElement> l = new java.util.ArrayList<>();
        for (Iterator<jakarta.xml.soap.Node> it = padre.getChildElements(); it.hasNext();) {
            jakarta.xml.soap.Node n = it.next();
            if (n instanceof SOAPElement e && nombre.equals(e.getLocalName())) {
                l.add(e);
            }
        }
        return l;
    }

    private static String texto(SOAPElement e) {
        return e.getTextContent();
    }

    private static void poner(SOAPElement padre, String nombre, BigDecimal valor) {
        hijo(padre, nombre).setTextContent(valor.toPlainString());
    }
}

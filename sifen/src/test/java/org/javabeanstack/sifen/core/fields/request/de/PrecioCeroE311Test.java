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

import java.math.BigDecimal;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * E3-11 (plan JBSSIF, §3.8 ciclo 2): un ítem con precio unitario 0 y descuento 0 ya no termina en
 * {@code ArithmeticException} (división por cero en {@code dPorcDesIt}) ni en {@code JBS-INT-00}.
 */
public class PrecioCeroE311Test {
    @Test
    public void precioUnitarioCeroNoDivide() throws Exception {
        SifenConfig cfg = FixturesSifen.configuracion();
        DocumentoElectronico de = CasosDePrueba.documentos().get(4).construir();
        TgCamItem item = de.getgDtipDE().getgCamItemList().get(0);
        item.getgValorItem().setdPUniProSer(BigDecimal.ZERO);
        item.getgValorItem().getgValorRestaItem().setdDescItem(BigDecimal.ZERO);
        String xml = de.generarXml(FixturesSifen.contexto(cfg), cfg);
        Assertions.assertEquals("0.00", XmlCanonico.texto(xml, "dPorcDesIt"));
    }
}

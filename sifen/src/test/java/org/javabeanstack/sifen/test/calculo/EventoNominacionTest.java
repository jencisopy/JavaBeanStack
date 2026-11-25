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

import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.EventosDE;
import org.javabeanstack.sifen.internal.request.MensajesSifen;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Evento de nominación ({@code TrGeVeNom}, nuevo en el fork) en la línea base (plan JBSSIF, §2.4).
 */
public class EventoNominacionTest {

    /** Con dirección y distrito: la ciudad va en {@code dDesCiuRec} (GENFE022; §6.1, E0-12: la línea base escribía {@code dDesCiuEnt}). */
    @Test
    public void ciudadSaleComoDDesCiuRecE012() throws Exception {
        SifenConfig cfg = FixturesSifen.configuracion();
        String xml = MensajesSifen.mensajeEvento(CasosDePrueba.eventoNominacionConDireccion((short) 1), cfg);
        Assertions.assertEquals("ASUNCION (DISTRITO)", XmlCanonico.texto(xml, "dDesCiuRec"));
        Assertions.assertNull(XmlCanonico.texto(xml, "dDesCiuEnt"), "E0-12: ya no sale dDesCiuEnt");
        Assertions.assertEquals("1", XmlCanonico.texto(xml, "cDisRec"));
    }

    /** Con dDirRec y sin distrito: sale sin {@code cDisRec} (§6.1, E0-18: la línea base daba NPE). */
    @Test
    public void direccionSinDistritoNoDaNullPointerE018() throws Exception {
        SifenConfig cfg = FixturesSifen.configuracion();
        EventosDE ev = CasosDePrueba.eventoNominacionConDireccion(null);
        String xml = MensajesSifen.mensajeEvento(ev, cfg);
        Assertions.assertNull(XmlCanonico.texto(xml, "cDisRec"));
        Assertions.assertEquals("ASUNCION (DISTRITO)", XmlCanonico.texto(xml, "dDesCiuRec"));
    }
}

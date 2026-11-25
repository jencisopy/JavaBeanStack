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

import org.javabeanstack.sifen.core.fields.util.RedondeoUtil;
import org.javabeanstack.sifen.core.types.CMondT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * Redondeo SEDECO de {@code RedondeoUtil} con escala (plan JBSSIF, §2.4). Complementa
 * {@code RedondeoOficialTest} (del repositorio original, que compara con {@code compareTo}): acá se
 * fija también la escala del resultado, que se arrastra a {@code dTotGralOpe} y {@code dTotalGs}
 * (E0-03).
 */
public class RedondeoUtilTest {

    /** 8517.60 → múltiplo de 0.50 más cercano 8517.50; redondeo 0.10. La línea base daba 0.100 (E0-03; §4.3, escala fija 2). */
    @Test
    public void dolaresDelCasoDelRelevamientoE003() {
        Assertions.assertEquals(new BigDecimal("0.10"), RedondeoUtil.redondeoOficialSET(CMondT.USD, new BigDecimal("8517.60")));
    }

    /** Hacia arriba el redondeo es negativo (I3-34); la línea base daba -0.240 (E0-03; §4.3). */
    @Test
    public void dolaresHaciaArribaDaNegativoE003() {
        Assertions.assertEquals(new BigDecimal("-0.24"), RedondeoUtil.redondeoOficialSET(CMondT.USD, new BigDecimal("1000.26")));
    }

    /** Total múltiplo de 0,50: redondeo 0.00 (la línea base daba 0.000, y el DE releído cambiaba de digest; §4.2/§4.3). */
    @Test
    public void dolaresMultiploDe050DaCeroConEscala2() {
        Assertions.assertEquals(new BigDecimal("0.00"), RedondeoUtil.redondeoOficialSET(CMondT.USD, new BigDecimal("1000.00")));
    }

    @Test
    public void guaraniesEsElRestoDe50() {
        Assertions.assertEquals(new BigDecimal("39"), RedondeoUtil.redondeoOficialSET(CMondT.PYG, new BigDecimal("47789")));
        Assertions.assertEquals(new BigDecimal("0"), RedondeoUtil.redondeoOficialSET(CMondT.PYG, new BigDecimal("143000")));
        Assertions.assertEquals(new BigDecimal("49"), RedondeoUtil.redondeoOficialSET(CMondT.PYG, new BigDecimal("99999")));
    }

    /** En PYG con decimales el resto conserva la escala de la entrada. */
    @Test
    @Tag("linea-base")
    public void guaraniesConEscalaE003() {
        Assertions.assertEquals(new BigDecimal("39.00"), RedondeoUtil.redondeoOficialSET(CMondT.PYG, new BigDecimal("47789.00")));
    }
}

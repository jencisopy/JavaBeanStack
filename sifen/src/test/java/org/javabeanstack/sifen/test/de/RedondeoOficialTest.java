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
package org.javabeanstack.sifen.test.de;

import org.javabeanstack.sifen.core.fields.util.RedondeoUtil;
import org.javabeanstack.sifen.core.types.CMondT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class RedondeoOficialTest {

    @Test
    public void testRedondeoGuaranies00() {
        BigDecimal ret = RedondeoUtil.redondeoOficialSET(CMondT.PYG, BigDecimal.valueOf(107437));
        Assertions.assertEquals(BigDecimal.valueOf(37), ret);
    }

    @Test
    public void testRedondeoGuaranies01() {
        BigDecimal ret = RedondeoUtil.redondeoOficialSET(CMondT.PYG, BigDecimal.valueOf(47789));
        Assertions.assertEquals(BigDecimal.valueOf(39), ret);
    }

    @Test
    public void testRedondeoGuaranies02() {
        BigDecimal ret = RedondeoUtil.redondeoOficialSET(CMondT.PYG, BigDecimal.valueOf(99999));
        Assertions.assertEquals(BigDecimal.valueOf(49), ret);
    }

    @Test
    public void testRedondeoDolares00() {
        BigDecimal ret = RedondeoUtil.redondeoOficialSET(CMondT.USD, BigDecimal.valueOf(1000));
        Assertions.assertTrue(BigDecimal.valueOf(0.0).compareTo(ret) == 0);
    }

    @Test
    public void testRedondeoDolares01() {
        BigDecimal ret = RedondeoUtil.redondeoOficialSET(CMondT.USD, BigDecimal.valueOf(1000.23));
        Assertions.assertTrue(BigDecimal.valueOf(0.23).compareTo(ret) == 0);
    }

    @Test
    public void testRedondeoDolares02() {
        BigDecimal ret = RedondeoUtil.redondeoOficialSET(CMondT.USD, BigDecimal.valueOf(1000.26));
        Assertions.assertTrue(BigDecimal.valueOf(-0.24).compareTo(ret) == 0);
    }

    @Test
    public void testRedondeoDolares03() {
        BigDecimal ret = RedondeoUtil.redondeoOficialSET(CMondT.USD, BigDecimal.valueOf(1000.56));
        Assertions.assertTrue(BigDecimal.valueOf(0.06).compareTo(ret) == 0);
    }

    @Test
    public void testRedondeoDolares04() {
        BigDecimal ret = RedondeoUtil.redondeoOficialSET(CMondT.USD, BigDecimal.valueOf(1000.76));
        Assertions.assertTrue(BigDecimal.valueOf(-0.24).compareTo(ret) == 0);
    }

}

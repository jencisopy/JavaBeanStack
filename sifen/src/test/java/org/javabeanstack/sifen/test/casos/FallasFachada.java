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
package org.javabeanstack.sifen.test.casos;

import org.javabeanstack.sifen.core.exceptions.ErrorCategory;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;

/**
 * Aserciones sobre lo que sale de la fachada desde §3.8 del plan JBSSIF (D33 Parte C): un camino de
 * la línea base que terminaba en {@code NullPointerException} sale como {@link SifenException}
 * {@code JBS-INT-00}, con id de correlación y la NPE en la cadena de causas.
 */
public final class FallasFachada {
    private FallasFachada() {
    }

    /**
     * Exige que la llamada termine en {@code JBS-INT-00} por una {@code NullPointerException}.
     *
     * @param llamada llamada a la fachada.
     * @return la excepción.
     */
    public static SifenException npeInterna(Executable llamada) {
        SifenException e = Assertions.assertThrows(SifenException.class, llamada);
        Assertions.assertEquals("JBS-INT-00", e.getCode(), e.toString());
        Assertions.assertEquals(ErrorCategory.INTERNAL, e.getCategory());
        Assertions.assertNotNull(e.getCorrelationId(), "id de correlación");
        Assertions.assertTrue(e.getMessage().contains("(ref. " + e.getCorrelationId() + ")"), e.getMessage());
        Assertions.assertInstanceOf(NullPointerException.class, e.getCause(), "la NPE de la línea base es la causa");
        return e;
    }
}

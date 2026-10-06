/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
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
package org.javabeanstack.security.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias puras del mapa de datos libres de la sesión: es
 * concurrente (plan PWDEXP, M5-04) y no admite nulos, así que
 * {@code addInfo}/{@code getInfo(key)} tienen que resolverlos sin lanzar.
 *
 * @author Jorge Enciso
 */
public class UserSessionInfoTest {

    @Test
    public void testAgregarLeerYQuitar() {
        UserSession sesion = new UserSession();
        sesion.addInfo("A", 1);
        assertEquals(1, sesion.getInfo("A"));
        //El mapa que devuelve getInfo() es el vivo: quitar ahi es quitar en la sesion.
        sesion.getInfo().remove("A");
        assertNull(sesion.getInfo("A"));
    }

    @Test
    public void testNulosNoLanzan() {
        UserSession sesion = new UserSession();
        sesion.addInfo("A", 1);
        //Valor nulo: quita la clave.
        sesion.addInfo("A", null);
        assertNull(sesion.getInfo("A"));
        assertFalse(sesion.getInfo().containsKey("A"));
        //Clave nula: se ignora al escribir y devuelve nulo al leer.
        sesion.addInfo(null, 1);
        assertNull(sesion.getInfo(null));
        assertTrue(sesion.getInfo().isEmpty());
    }
}

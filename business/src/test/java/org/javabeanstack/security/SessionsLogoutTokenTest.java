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
package org.javabeanstack.security;

import org.javabeanstack.security.model.UserSession;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Prueba pura (sin contenedor) de {@code Sessions.logout} con un token en
 * claro: las sesiones de token se indexan por el token mismo y el logout tiene
 * que sacarlas de la caché sin pasar por el descifrado, que con un token lanza
 * y dejaba la sesión viva hasta 30 minutos (RESTAUTH I1-04).
 */
public class SessionsLogoutTokenTest {

    /** Expone la caché protegida. */
    static class SessionsStub extends Sessions {

        int cantidad() {
            return sessionVar.size();
        }

        void poner(String clave) {
            UserSession s = new UserSession();
            s.setSessionId(clave);
            sessionVar.put(clave, s);
        }

        boolean tiene(String clave) {
            return sessionVar.containsKey(clave);
        }
    }

    @Test
    @DisplayName("logout(token en claro) saca la sesión de la caché")
    public void logoutPorToken() {
        SessionsStub sessions = new SessionsStub();
        sessions.poner("tok-1");
        sessions.poner("tok-2");
        sessions.logout("tok-1");
        assertFalse(sessions.tiene("tok-1"));
        assertTrue(sessions.tiene("tok-2"), "solo se va la sesión pedida");
    }

    @Test
    @DisplayName("logout de una clave inexistente o nula no revienta ni toca las demás")
    public void logoutInexistente() {
        SessionsStub sessions = new SessionsStub();
        sessions.poner("tok-1");
        assertDoesNotThrow(() -> sessions.logout("no-existe"));
        assertDoesNotThrow(() -> sessions.logout(null));
        assertEquals(1, sessions.cantidad());
    }
}

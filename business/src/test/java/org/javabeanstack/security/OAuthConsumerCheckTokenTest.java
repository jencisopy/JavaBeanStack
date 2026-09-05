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

import java.time.LocalDateTime;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.model.appcatalog.AppAuthConsumer;
import org.javabeanstack.model.appcatalog.AppAuthConsumerToken;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Prueba pura de {@code checkToken(IAppAuthConsumerToken, true)}: sin base ni
 * contenedor, sobre el registro ya cargado. Cubre el rechazo por
 * {@code deleted} (RESTAUTH I1-05), que antes solo miraba
 * {@code isValidToken(IAppAuthConsumerToken, …)}.
 */
public class OAuthConsumerCheckTokenTest {

    static AppAuthConsumerToken token(boolean blocked, Boolean deleted, LocalDateTime consumerExpira) {
        AppAuthConsumer consumer = new AppAuthConsumer();
        consumer.setConsumerKey("CK");
        consumer.setBlocked(false);
        consumer.setExpiredDate(consumerExpira);
        AppAuthConsumerToken t = new AppAuthConsumerToken();
        t.setToken("tok");
        t.setBlocked(blocked);
        t.setDeleted(deleted);
        t.setAppAuthConsumerKey(consumer);
        return t;
    }

    @Test
    @DisplayName("Token sano: sin error")
    public void tokenSano() {
        IErrorReg e = new OAuthConsumer().checkToken(token(false, false, LocalDateTime.now().plusDays(1)), true);
        assertEquals(0, e.getErrorNumber());
    }

    @Test
    @DisplayName("Token marcado deleted: se rechaza como inexistente")
    public void tokenEliminado() {
        IErrorReg e = new OAuthConsumer().checkToken(token(false, true, LocalDateTime.now().plusDays(1)), true);
        assertEquals(50000, e.getErrorNumber());
        assertTrue(e.getMessage().contains("no existe"), e.getMessage());
        assertFalse(new OAuthConsumer().isValidToken(token(false, true, LocalDateTime.now().plusDays(1)), true));
    }

    @Test
    @DisplayName("deleted nulo cuenta como no eliminado")
    public void deletedNulo() {
        IErrorReg e = new OAuthConsumer().checkToken(token(false, null, LocalDateTime.now().plusDays(1)), true);
        assertEquals(0, e.getErrorNumber());
    }

    @Test
    @DisplayName("Bloqueado y consumer vencido siguen rechazándose")
    public void bloqueadoYVencido() {
        assertEquals(50000, new OAuthConsumer().checkToken(token(true, false, LocalDateTime.now().plusDays(1)), true).getErrorNumber());
        assertEquals(50000, new OAuthConsumer().checkToken(token(false, false, LocalDateTime.now().minusDays(1)), true).getErrorNumber());
    }
}

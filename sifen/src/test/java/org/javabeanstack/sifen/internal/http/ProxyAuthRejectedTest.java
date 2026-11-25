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
package org.javabeanstack.sifen.internal.http;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * I10-23 (plan JBSSIF, §10.7): el 407 del proxy se reconoce en la línea de estado HTTP que el JDK copia en el
 * mensaje, no en cualquier «407» del texto (un puerto {@code :8407} o un nombre de proxy).
 */
public class ProxyAuthRejectedTest {

    @Test
    public void reconoceLaLineaDeEstadoYNoUnPuerto() {
        Assertions.assertTrue(UrlSifenConnection.proxyAuthRejected(new IOException(
                "Unable to tunnel through proxy. Proxy returns \"HTTP/1.1 407 Proxy Authentication Required\"")));
        Assertions.assertTrue(UrlSifenConnection.proxyAuthRejected(new IOException("envoltorio",
                new IOException("Proxy returns \"HTTP/1.0 407 Proxy Authentication Required\""))));
        Assertions.assertFalse(UrlSifenConnection.proxyAuthRejected(new IOException(
                "Connection refused: proxy-407.example:8407")));
        Assertions.assertFalse(UrlSifenConnection.proxyAuthRejected(new IOException(
                "Unable to tunnel through proxy. Proxy returns \"HTTP/1.1 502 Bad Gateway\" (puerto 4070)")));
        Assertions.assertFalse(UrlSifenConnection.proxyAuthRejected(new IOException((String) null)));
    }
}

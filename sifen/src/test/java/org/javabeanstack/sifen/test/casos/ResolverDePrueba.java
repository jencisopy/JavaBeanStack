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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.spi.InetAddressResolver;
import java.net.spi.InetAddressResolverProvider;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Resolver de nombres de los tests (M3-62; JEP 418): los nombres {@code *.invalid} (RFC 6761) fallan con
 * {@link UnknownHostException} sin consultar al resolver del sistema, así la fila DNS de
 * {@code FachadaFallasTest} no depende de la red ni de la configuración de la máquina. El resto se
 * delega en el resolver del JDK. Se registra en
 * {@code META-INF/services/java.net.spi.InetAddressResolverProvider} (solo en el classpath de test).
 */
public class ResolverDePrueba extends InetAddressResolverProvider {
    /** Sufijo que nunca se resuelve. */
    public static final String SUFIJO = ".invalid";
    /** Nombre testigo que solo este resolver conoce (a la dirección de loopback): prueba que está activo. */
    public static final String TESTIGO = "resolver-de-prueba.jbs-sifen.test";

    @Override
    public InetAddressResolver get(Configuration configuration) {
        InetAddressResolver jdk = configuration.builtinResolver();
        return new InetAddressResolver() {
            @Override
            public Stream<InetAddress> lookupByName(String host, LookupPolicy lookupPolicy) throws UnknownHostException {
                if (host != null && host.toLowerCase(Locale.ROOT).endsWith(SUFIJO)) {
                    throw new UnknownHostException(host + ": nombre de prueba que no se resuelve (M3-62)");
                }
                if (TESTIGO.equalsIgnoreCase(host)) {
                    return Stream.of(InetAddress.getLoopbackAddress());
                }
                return jdk.lookupByName(host, lookupPolicy);
            }

            @Override
            public String lookupByAddress(byte[] addr) throws UnknownHostException {
                return jdk.lookupByAddress(addr);
            }
        };
    }

    @Override
    public String name() {
        return "jbs-sifen-tests";
    }
}

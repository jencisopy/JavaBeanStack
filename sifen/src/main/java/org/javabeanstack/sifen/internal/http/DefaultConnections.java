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

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.javabeanstack.sifen.core.SifenConfig;

/**
 * Conexión por instancia de configuración para los pedidos que no reciben una explícita (plan JBSSIF,
 * §5.2): la fachada heredada {@code Sifen} y las peticiones armadas sin conexión.
 *
 * <p>Mapa débil: la clave es la {@code SifenConfig} y su igualdad es la de {@code Object}
 * (identidad; {@code SifenConfig} no redefine {@code equals}, lo comprueba {@code SifenConnectionTest});
 * el valor no guarda la configuración, así que la entrada se libera cuando la aplicación suelta la
 * configuración.</p>
 */
final class DefaultConnections {
    private static final Map<SifenConfig, SifenConnection> BY_CONFIG = Collections.synchronizedMap(new WeakHashMap<>());
    /** Para un pedido sin configuración (la validación lo corta antes; se conserva por robustez). */
    private static final SifenConnection WITHOUT_CONFIG = new UrlSifenConnection();

    private DefaultConnections() {
    }

    static SifenConnection of(SifenConfig config) {
        if (config == null) {
            return WITHOUT_CONFIG;
        }
        return BY_CONFIG.computeIfAbsent(config, k -> new UrlSifenConnection());
    }

    static void release(SifenConfig config) {
        if (config == null) {
            return;
        }
        SifenConnection c = BY_CONFIG.remove(config);
        if (c != null) {
            c.close();
        }
    }
}

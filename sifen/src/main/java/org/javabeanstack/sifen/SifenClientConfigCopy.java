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
package org.javabeanstack.sifen;

import org.javabeanstack.sifen.core.SifenConfig;

/**
 * Copia de una {@link SifenConfig} para un cliente (plan JBSSIF, §10.2): el <b>único</b> lugar donde
 * la fachada copia la configuración, para que el cliente no vea los cambios que la aplicación haga
 * después sobre el objeto original.
 *
 * <p>Desde §5.2 (carril B, I10-08) delega en {@link SifenConfig#copy()}, que copia todos los campos:
 * también las listas de la última carga ({@code JBS-CFG-10}, {@code JBS-CFG-11}), que ahora reaparecen
 * en {@code validateConfig()}, y los de la Fase 5 (tiempos de espera, proxy, reintentos y traza).</p>
 *
 * <p>{@code SifenClientConfigCopyTest} recorre por reflexión todos los getters públicos de
 * {@code SifenConfig}: un campo nuevo que esta copia no cubra hace fallar la prueba.</p>
 */
final class SifenClientConfigCopy {

    private SifenClientConfigCopy() {
    }

    /**
     * Copia la configuración.
     *
     * @param source configuración de la aplicación.
     * @return una configuración nueva con los mismos valores; {@code null} si {@code source} es
     *         {@code null} (la validación lo informa como {@code JBS-CFG-01}).
     */
    static SifenConfig of(SifenConfig source) {
        return source == null ? null : source.copy();
    }
}

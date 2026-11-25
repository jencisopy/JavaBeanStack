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
import org.javabeanstack.sifen.core.exceptions.SifenException;

/**
 * Proveedor de la configuración de una empresa para {@link SifenClientRegistry#get(String, IConfigSupplier)}
 * (contrato v1, plan JBSSIF, §10.2; prefijo {@code I} desde D43, como las demás interfaces del contrato). El registro lo llama <b>una vez</b> por clave, cuando todavía no
 * hay cliente o después de {@link SifenClientRegistry#invalidate(String)}.
 *
 * <p>Uso típico: {@code () -> SifenConfig.load(propiedadesDeLaEmpresa)}.</p>
 */
@FunctionalInterface
public interface IConfigSupplier {

    /**
     * Arma la configuración de la empresa.
     *
     * @return la configuración; {@code null} se informa como {@code JBS-CFG-01}.
     * @throws SifenException si no se pudo armar (p. ej. la de {@code SifenConfig.load}).
     */
    SifenConfig get() throws SifenException;
}

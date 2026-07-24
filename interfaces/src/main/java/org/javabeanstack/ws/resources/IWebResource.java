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

package org.javabeanstack.ws.resources;

import org.javabeanstack.security.ISecManager;
import org.javabeanstack.data.services.IDataService;

/**
 * Contrato base de un recurso de servicio web: da acceso al servicio de datos
 * ({@link IDataService}) y al gestor de seguridad ({@link ISecManager}), y
 * resuelve datos del cliente (empresa, ip, host) desde la solicitud.
 *
 * @author Jorge Enciso
 */
public interface IWebResource{
    /**
     * Devuelve el servicio de datos del recurso.
     * @param <T> tipo del servicio de datos.
     * @return servicio de datos.
     */
    <T extends IDataService> T getDataService();

    /**
     * Devuelve el gestor de seguridad por defecto.
     * @return gestor de seguridad.
     */
    ISecManager getSecManager();

    /**
     * Devuelve el gestor de seguridad ubicado en la ruta JNDI indicada.
     * @param jndi ruta JNDI del gestor de seguridad.
     * @return gestor de seguridad.
     */
    ISecManager getSecManager(String jndi);

    /**
     * Devuelve el identificador de la empresa a partir del encabezado de
     * autorización.
     * @param authHeader encabezado de autorización.
     * @return identificador de la empresa.
     */
    Long getIdCompany(String authHeader);

    /**
     * Devuelve la ip del cliente que realiza la solicitud.
     * @return ip del cliente.
     */
    String getIpClient();

    /**
     * Devuelve el host remoto que realiza la solicitud.
     * @return host remoto.
     */
    String getRemoteHost();
}

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

import jakarta.ejb.EJB;
import org.javabeanstack.data.IGenericDAO;

/**
 * Implementación concreta del gestor de seguridad: extiende
 * {@link AbstractSecManager} e inyecta por EJB el DAO genérico y el
 * administrador de sesiones sobre los que opera. Expone el contrato
 * {@code ISecManager} (local y remoto).
 *
 * @author Jorge Enciso
 */
public class SecManager extends AbstractSecManager implements ISecManager, ISecManagerRemote{
    @EJB private IGenericDAO dao;
    @EJB private ISessions sesiones;

    /**
     * Devuelve el DAO genérico inyectado por EJB.
     *
     * @return DAO genérico.
     */
    @Override
    protected IGenericDAO getDAO() {
        return dao;
    }

    /**
     * Devuelve el administrador de sesiones inyectado por EJB.
     *
     * @return administrador de sesiones.
     */
    @Override
    protected ISessions getSessions() {
        return sesiones;
    }

    /**
     * Devuelve el servicio de bitácora de contraseñas ({@code null} si no se utiliza).
     *
     * @return servicio de bitácora de contraseñas.
     */
    @Override
    protected IAppUserPwdLogSrv getAppUserPwdLogSrv() {
        return null;
    }
}

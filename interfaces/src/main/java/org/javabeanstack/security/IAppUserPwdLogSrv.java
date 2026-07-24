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

import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.model.IAppUser;

/**
 * Contrato del servicio de bitácora de contraseñas: registra el historial de
 * contraseñas de un usuario y permite consultarlo (para impedir la reutilización
 * de contraseñas anteriores). Extiende el servicio de datos {@link IDataService}.
 *
 * @author Jorge Enciso
 */
public interface IAppUserPwdLogSrv extends IDataService {
    /**
     * Indica si existe bitácora de contraseñas para el usuario de la sesión.
     *
     * @param sessionId identificador de la sesión del usuario.
     * @return verdadero si existe bitácora, falso si no.
     */
    boolean isExistUserPwdLog(String sessionId);

    /**
     * Indica si existe bitácora de contraseñas para el usuario indicado.
     *
     * @param appUser usuario a consultar.
     * @return verdadero si existe bitácora, falso si no.
     */
    boolean isExistUserPwdLog(IAppUser appUser);

    /**
     * Registra la contraseña actual del usuario de la sesión en la bitácora.
     *
     * @param sessionId identificador de la sesión del usuario.
     */
    void insertUserPwdLog(String sessionId);

    /**
     * Registra la contraseña actual del usuario indicado en la bitácora.
     *
     * @param appUser usuario cuya contraseña se registra.
     */
    void insertUserPwdLog(IAppUser appUser);

    /**
     * Devuelve el identificador del usuario cuya bitácora contiene la contraseña
     * indicada.
     *
     * @param pwd contraseña a buscar.
     * @return identificador del usuario, o {@code null} si no se encuentra.
     */
    Long getIdUserFromPwdLog(String pwd);
}

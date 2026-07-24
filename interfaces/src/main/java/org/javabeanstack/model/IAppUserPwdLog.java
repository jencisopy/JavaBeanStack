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

package org.javabeanstack.model;

import java.time.LocalDateTime;
import org.javabeanstack.data.IDataRow;


/**
 * Contrato de la entidad de bitácora de contraseñas: registra las contraseñas
 * anteriores de un usuario para impedir su reutilización. Extiende
 * {@link IDataRow}.
 *
 * @author Jorge Enciso
 */
public interface IAppUserPwdLog extends IDataRow{
    /**
     * Devuelve el identificador del registro de bitácora.
     * @return identificador del registro.
     */
    Long getIdAppUserPwdLog();

    /**
     * Asigna el identificador del registro de bitácora.
     * @param IdAppUserPwdLog identificador del registro.
     */
    void setIdAppUserPwdLog(Long IdAppUserPwdLog);

    /**
     * Devuelve el identificador del usuario.
     * @return identificador del usuario.
     */
    Long getIduser();

    /**
     * Asigna el identificador del usuario.
     * @param iduser identificador del usuario.
     */
    void setIduser(Long iduser);

    /**
     * Devuelve la contraseña (hash) registrada.
     * @return contraseña registrada.
     */
    String getPwd();

    /**
     * Asigna la contraseña (hash) a registrar.
     * @param pwd contraseña a registrar.
     */
    void setPwd(String pwd);

    /**
     * Devuelve el rol del usuario al momento del registro.
     * @return rol del usuario.
     */
    String getRol();

    /**
     * Asigna el rol del usuario al momento del registro.
     * @param rol rol del usuario.
     */
    void setRol(String rol);

    /**
     * Devuelve la fecha y hora del registro de bitácora.
     * @return fecha y hora del registro.
     */
    LocalDateTime getDateTimeLog();
}

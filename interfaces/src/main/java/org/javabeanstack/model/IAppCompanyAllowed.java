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

import java.io.Serializable;
import org.javabeanstack.data.IDataRow;

/**
 * Contrato de la entidad de habilitación empresa-usuario: define si un usuario
 * tiene permitido o denegado el acceso a una empresa. Extiende {@link IDataRow}.
 *
 * @author Jorge Enciso
 */
public interface IAppCompanyAllowed extends IDataRow, Serializable {
    /**
     * Devuelve el identificador de la empresa.
     * @return identificador de la empresa.
     */
    Long getIdcompany();

    /**
     * Devuelve el identificador del usuario.
     * @return identificador del usuario.
     */
    Long getIduser();

    /**
     * Indica si el acceso está denegado.
     * @return verdadero si está denegado.
     */
    boolean getDeny();

    /**
     * Indica si el acceso está permitido.
     * @return verdadero si está permitido.
     */
    boolean getAllow();

    /**
     * Devuelve el usuario de aplicación asociado.
     * @return usuario de aplicación.
     */
    String getAppuser();

    /**
     * Asigna el identificador de la empresa.
     * @param idempresa identificador de la empresa.
     */
    void setIdcompany(Long idempresa);

    /**
     * Asigna el identificador del usuario.
     * @param idusuario identificador del usuario.
     */
    void setIduser(Long idusuario);

    /**
     * Asigna si el acceso está denegado.
     * @param negar verdadero para denegar el acceso.
     */
    void setDeny(boolean negar);

    /**
     * Asigna si el acceso está permitido.
     * @param permitir verdadero para permitir el acceso.
     */
    void setAllow(boolean permitir);

    /**
     * Asigna el usuario de aplicación asociado.
     * @param appuser usuario de aplicación.
     */
    void setAppuser(String appuser);
}

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
package org.javabeanstack.security;

import java.io.Serializable;
import java.util.Map;

/**
 * Contrato de los datos de autenticación que se incluyen al emitir un token
 * OAuth ({@link IOAuthConsumer}): usuario, empresa, condición de administrador,
 * credenciales y datos adicionales libres.
 *
 * @author Jorge Enciso
 */
public interface IOAuthConsumerData extends Serializable {
    /**
     * Devuelve el identificador del usuario.
     *
     * @return identificador del usuario.
     */
    Long getIdAppUser();

    /**
     * Asigna el identificador del usuario.
     *
     * @param iduser identificador del usuario.
     */
    void setIdAppUser(Long iduser);

    /**
     * Devuelve el identificador de la empresa.
     *
     * @return identificador de la empresa.
     */
    Long getIdCompany();

    /**
     * Asigna el identificador de la empresa.
     *
     * @param idcompany identificador de la empresa.
     */
    void setIdCompany(Long idcompany);

    /**
     * Indica si el usuario es administrador.
     *
     * @return verdadero si es administrador, falso si no.
     */
    boolean isAdministrator();

    /**
     * Asigna la condición de administrador del usuario.
     *
     * @param value verdadero si es administrador.
     */
    void setAdministrator(boolean value);

    /**
     * Devuelve los datos adicionales asociados.
     *
     * @return mapa clave → valor de datos adicionales.
     */
    Map<String, String> getOtherData();

    /**
     * Asigna los datos adicionales.
     *
     * @param otherData mapa clave → valor de datos adicionales.
     */
    void setOtherData(Map<String, String> otherData);

    /**
     * Agrega un valor a los datos adicionales.
     *
     * @param key clave del dato.
     * @param value valor del dato.
     */
    void addOtherDataValue(String key, String value);

    /**
     * Elimina un valor de los datos adicionales.
     *
     * @param key clave del dato.
     */
    void removeOtherDataValue(String key);

    /**
     * Devuelve el login del usuario.
     *
     * @return login del usuario.
     */
    String getUserLogin();

    /**
     * Asigna el login del usuario.
     *
     * @param userLogin login del usuario.
     */
    void setUserLogin(String userLogin);

    /**
     * Devuelve la contraseña del usuario.
     *
     * @return contraseña del usuario.
     */
    String getUserPass();

    /**
     * Asigna la contraseña del usuario.
     *
     * @param userPass contraseña del usuario.
     */
    void setUserPass(String userPass);
}

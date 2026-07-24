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
package org.javabeanstack.data;

import java.io.Serializable;
import org.javabeanstack.security.IOAuthConsumer;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.model.IAppAuthConsumerToken;

/**
 * Contrato de la información de contexto que resuelve una conexión de datos
 * ({@link IDataLink}) a partir de la sesión de usuario o de un token de acceso.
 *
 * <p>Expone la unidad de persistencia, la empresa y el período activos, el
 * filtro de datos ({@link IDBFilter}) y los identificadores del usuario/sesión
 * necesarios para acotar y auditar el acceso a la base de datos.</p>
 *
 * @author Jorge Enciso
 */
public interface IDBLinkInfo extends Serializable {
    /**
     * Devuelve el filtro de datos aplicable según el contexto (empresa/permisos).
     *
     * @return filtro de datos.
     */
    IDBFilter getDBFilter();

    /**
     * Devuelve el identificador de la empresa activa.
     *
     * @return identificador de la empresa.
     */
    Long getIdCompany();

    /**
     * Devuelve el identificador de la empresa/período activo.
     *
     * @return identificador de la empresa período.
     */
    Long getIdCompanyPeriodo();

    /**
     * Devuelve el nombre de la unidad de persistencia a utilizar.
     *
     * @return nombre de la unidad de persistencia.
     */
    String getPersistUnit();

    /**
     * Devuelve la sesión de usuario asociada al contexto.
     *
     * @return sesión de usuario.
     */
    IUserSession getUserSession();

    /**
     * Asigna la sesión de usuario del contexto.
     *
     * @param userSession sesión de usuario.
     */
    void setUserSession(IUserSession userSession);

    /**
     * Configura el contexto a partir de un token de acceso, con opción de omitir
     * la validación del token.
     *
     * @param token token de acceso.
     * @param oAuthConsumer consumidor OAuth que emitió/valida el token.
     * @param noValid verdadero para no validar el token.
     * @throws Exception si la validación o la resolución del contexto falla.
     */
    void setToken(IAppAuthConsumerToken token, IOAuthConsumer oAuthConsumer, boolean noValid)  throws Exception;

    /**
     * Configura el contexto a partir de un token de acceso (con validación).
     *
     * @param token token de acceso.
     * @param oAuthConsumer consumidor OAuth que emitió/valida el token.
     * @throws Exception si la validación o la resolución del contexto falla.
     */
    void setToken(IAppAuthConsumerToken token, IOAuthConsumer oAuthConsumer)  throws Exception;

    /**
     * Devuelve el identificador del usuario de aplicación del contexto.
     *
     * @return identificador del usuario.
     */
    String getAppUserId();

    /**
     * Devuelve el identificador de sesión o de token vigente en el contexto.
     *
     * @return identificador de sesión o token.
     */
    String getSessionOrTokenId();

    /**
     * Devuelve el identificador del dispositivo (uuid) asociado al contexto.
     *
     * @return uuid del dispositivo.
     */
    String getUuidDevice();
}

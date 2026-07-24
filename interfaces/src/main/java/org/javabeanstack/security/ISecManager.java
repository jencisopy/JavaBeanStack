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

import java.util.Map;
import org.javabeanstack.model.IAppAuthConsumerToken;
import org.javabeanstack.model.IAppUser;
import org.javabeanstack.security.model.IClientAuthRequestInfo;
import org.javabeanstack.security.model.IUserSession;


/**
 * Contrato del gestor de seguridad: fachada de autenticación que crea y valida
 * las sesiones de usuario ({@link IUserSession}), resuelve roles y empresas
 * habilitadas y da soporte a la autenticación por token.
 *
 * <p>Se apoya en {@link ISessions} para el almacenamiento de sesiones. La
 * implementación de referencia es {@code org.javabeanstack.security.SecManager}.</p>
 *
 * @author Jorge Enciso
 */
public interface ISecManager {
    /**
     * Crea una sesión autenticando al usuario con sus credenciales.
     *
     * @param userLogin login del usuario.
     * @param password contraseña del usuario.
     * @param idcompany empresa sobre la que se abre la sesión.
     * @param idleSessionExpireInMinutes minutos de inactividad antes de expirar.
     * @param otherParams parámetros adicionales de la sesión.
     * @return sesión creada (con el error informado si la autenticación falla).
     */
    IUserSession createSession(String userLogin, String password, Object idcompany, Integer idleSessionExpireInMinutes, Map<String, Object> otherParams);

    /**
     * Recrea/renueva una sesión existente, opcionalmente cambiando de empresa.
     *
     * @param sessionId identificador de la sesión.
     * @param idcompany empresa a la que se cambia (o la misma).
     * @return sesión recreada.
     */
    IUserSession reCreateSession(String sessionId, Object idcompany);

    /**
     * Crea una sesión a partir de un token de acceso.
     *
     * @param token token de acceso.
     * @return sesión creada.
     */
    IUserSession createSessionFromToken(String token);

    /**
     * Devuelve la lista de empresas habilitadas (formato interno).
     *
     * @return lista de empresas.
     */
    String getCompanyList();

    /**
     * Devuelve el rol del usuario indicado.
     *
     * @param userLogin login del usuario.
     * @return rol del usuario.
     */
    String getUserRol(String userLogin);

    /**
     * Indica si un usuario pertenece a un grupo de usuarios.
     *
     * @param user login del usuario.
     * @param userGroup grupo de usuarios.
     * @return verdadero si es miembro, falso si no.
     */
    Boolean isUserMemberOf(String user, String userGroup);

    /**
     * Indica si el identificador de sesión es válido (existe y no expiró).
     *
     * @param sessionId identificador de la sesión.
     * @return verdadero si es válido, falso si no.
     */
    Boolean isSessionIdValid(String sessionId);

    /**
     * Autentica al usuario con sus credenciales.
     *
     * @param userLogin login del usuario.
     * @param password contraseña del usuario.
     * @param otherParams parámetros adicionales.
     * @return verdadero si las credenciales son válidas, falso si no.
     * @throws Exception si ocurre un error durante la autenticación.
     */
    Boolean login(String userLogin, String password, Map<String, Object> otherParams) throws Exception;

    /**
     * Autentica al usuario y devuelve la sesión resultante.
     *
     * @param userLogin login del usuario.
     * @param password contraseña del usuario.
     * @param otherParams parámetros adicionales.
     * @return sesión del usuario autenticado.
     * @throws Exception si ocurre un error durante la autenticación.
     */
    IUserSession login2(String userLogin, String password, Map<String, Object> otherParams) throws Exception;

    /**
     * Cierra la sesión indicada.
     *
     * @param userSession sesión a cerrar.
     */
    void logout(IUserSession userSession);

    /**
     * Cierra la sesión indicada por su identificador.
     *
     * @param sessionId identificador de la sesión.
     */
    void logout(String sessionId);

    /**
     * Devuelve la información de solicitud de autenticación cacheada para un
     * encabezado de autorización.
     *
     * @param authHeader encabezado de autorización.
     * @return información de la solicitud de autenticación.
     */
    IClientAuthRequestInfo getClientAuthRequestCache(String authHeader);

    /**
     * Recupera el usuario a partir de su contraseña (para flujos específicos de
     * recuperación/validación).
     *
     * @param appUserPass contraseña del usuario.
     * @return usuario asociado, o {@code null} si no corresponde.
     */
    IAppUser getAppUserFromPwd(String appUserPass);

    /**
     * Recupera el token de consumidor de autenticación asociado a un token.
     *
     * @param token token de acceso.
     * @return token del consumidor de autenticación.
     */
    IAppAuthConsumerToken getAppAuthConsumerToken(String token);
}

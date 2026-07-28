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
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.data.IDBLinkInfo;
import org.javabeanstack.security.model.IClientAuthRequestInfo;



/**
 * Contrato del administrador de sesiones: crea, recupera y cierra las sesiones
 * de usuario ({@link IUserSession}) y mantiene la información asociada a cada
 * una (contexto de datos, caché de autenticación y datos libres).
 *
 * <p>Es el componente sobre el que se apoya el gestor de seguridad
 * ({@link ISecManager}). La implementación de referencia es
 * {@code org.javabeanstack.security.Sessions}.</p>
 *
 * @author Jorge Enciso
 */
public interface ISessions {
    /**
     * Verifica que el usuario tenga acceso a la empresa indicada.
     *
     * @param iduser identificador del usuario.
     * @param idcompany identificador de la empresa.
     * @return verdadero si tiene acceso, falso si no.
     * @throws Exception si ocurre un error de acceso a datos.
     */
    Boolean checkCompanyAccess(Long iduser, Long idcompany) throws Exception;

    /**
     * Crea una sesión a partir de un token de acceso.
     *
     * @param authToken token de acceso.
     * @return sesión creada.
     */
    IUserSession createSessionFromToken(String authToken);

    /**
     * Crea la sesión de un usuario a partir de un token, verificando además que
     * su rol tenga acceso concedido a la aplicación indicada. Si no lo tiene,
     * la sesión no se crea: el control forma parte de la creación y por lo
     * tanto no puede saltearse.
     *
     * @param authToken token de acceso.
     * @param appName nombre de la aplicación (context path del despliegue); con
     * valor nulo no se evalúa la política de acceso.
     * @return objeto sesión, o {@code null} si el token no es válido o el rol
     * no tiene acceso a la aplicación.
     */
    IUserSession createSessionFromToken(String authToken, String appName);

    /**
     * Crea una sesión autenticando al usuario con sus credenciales.
     *
     * @param userLogin login del usuario.
     * @param password contraseña del usuario.
     * @param idcompany empresa sobre la que se abre la sesión.
     * @param idleSessionExpireInMinutes minutos de inactividad antes de expirar.
     * @param otherParams parámetros adicionales de la sesión.
     * @return sesión creada.
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
     * Devuelve la sesión asociada al identificador indicado.
     *
     * @param sessionId identificador de la sesión.
     * @return sesión, o {@code null} si no existe.
     */
    IUserSession getUserSession(String sessionId);

    /**
     * Autentica al usuario y devuelve la sesión resultante.
     *
     * @param userLogin login del usuario.
     * @param password contraseña del usuario.
     * @param otherParams parámetros adicionales.
     * @return sesión del usuario autenticado.
     * @throws Exception si ocurre un error durante la autenticación.
     */
    IUserSession login(String userLogin, String password, Map<String, Object> otherParams) throws Exception;

    /**
     * Devuelve la información de contexto de datos asociada a la sesión.
     *
     * @param sessionId identificador de la sesión.
     * @return información de la conexión de datos.
     */
    IDBLinkInfo getDBLinkInfo(String sessionId);

    /**
     * Devuelve la información de solicitud de autenticación cacheada para un token.
     *
     * @param token token o encabezado de autorización.
     * @return información de la solicitud de autenticación.
     */
    IClientAuthRequestInfo getClientAuthRequestCache(String token);

    /**
     * Indica si el usuario está activo/es válido.
     *
     * @param iduser identificador del usuario.
     * @return verdadero si es válido, falso si no.
     * @throws Exception si ocurre un error de acceso a datos.
     */
    boolean isUserValid(Long iduser) throws Exception;

    /**
     * Valida los datos de autenticación de un consumidor OAuth.
     *
     * @param data datos de autenticación del consumidor.
     * @return verdadero si son válidos, falso si no.
     */
    boolean checkAuthConsumerData(IOAuthConsumerData data);

    /**
     * Cierra la sesión indicada por su identificador.
     *
     * @param sessionId identificador de la sesión.
     */
    void logout(String sessionId);

    /**
     * Devuelve un dato almacenado en el contexto de la sesión.
     *
     * @param sessionId identificador de la sesión.
     * @param key clave del dato.
     * @return valor almacenado, o {@code null} si no existe.
     */
    Object getSessionInfo(String sessionId, String key);

    /**
     * Almacena un dato en el contexto de la sesión.
     *
     * @param sessionId identificador de la sesión.
     * @param key clave del dato.
     * @param info valor a almacenar.
     */
    void addSessionInfo(String sessionId, String key, Object info);

    /**
     * Elimina un dato del contexto de la sesión.
     *
     * @param sessionId identificador de la sesión.
     * @param key clave del dato.
     */
    void removeSessionInfo(String sessionId, String key);
}

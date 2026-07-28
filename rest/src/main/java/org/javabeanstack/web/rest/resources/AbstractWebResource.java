/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2018 - 2027 Jorge Enciso
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
package org.javabeanstack.web.rest.resources;

import jakarta.ejb.EJB;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.javabeanstack.security.ISecManager;
import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.model.IAppUser;
import org.javabeanstack.security.IOAuthConsumer;
import org.javabeanstack.security.model.IClientAuthRequestInfo;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.util.Strings;
import org.javabeanstack.web.rest.exceptions.TokenError;
import org.javabeanstack.ws.resources.IWebResource;

/**
 *
 * Base de los recursos JAX-RS: implementa
 * {@link org.javabeanstack.ws.resources.IWebResource} y provee la verificación
 * de token, el acceso a la sesión y los datos del cliente.
 *
 * @author Jorge Enciso
 */
public abstract class AbstractWebResource implements IWebResource {

    private static final Logger LOGGER = LogManager.getLogger(AbstractWebResource.class);

    @EJB
    private IOAuthConsumer oAuthConsumer;

    @Context
    HttpServletRequest requestContext;

    @Override
    public abstract <T extends IDataService> T getDataService();

    @Override
    public abstract ISecManager getSecManager();

    /**
     * Devuelve el identificador de la empresa a partir del encabezado de
     * autorización.
     * @param authHeader encabezado de autorización.
     * @return identificador de la empresa.
     */
    @Override
    public Long getIdCompany(String authHeader) {
        String token = getTokenFromHeader(authHeader);
        IClientAuthRequestInfo info = getSecManager().getClientAuthRequestCache(token);
        if (info != null) {
            return info.getIdcompany();
        }
        return null;
    }

    /**
     * Devuelve la ip del cliente que realiza la solicitud.
     * @return ip del cliente.
     */
    @Override
    public final String getIpClient() {
        return requestContext.getRemoteAddr();
    }

    /**
     * Devuelve el host remoto que realiza la solicitud.
     * @return host remoto.
     */
    @Override
    public final String getRemoteHost() {
        return requestContext.getRemoteHost();
    }

    /**
     * Extrae el token del encabezado de autorización.
     *
     * @param authHeader encabezado de autorización.
     * @return token.
     */
    public String getToken(String authHeader) {
        String token = getTokenFromHeader(authHeader);
        IClientAuthRequestInfo info = getSecManager().getClientAuthRequestCache(token);
        if (info != null) {
            return info.getToken();
        }
        return null;
    }

    /**
     * Devuelve el gestor de consumidores OAuth.
     *
     * @return gestor OAuth.
     */
    protected IOAuthConsumer getOAuthConsumer() {
        return oAuthConsumer;
    }

    /**
     * Verifica la validez de un token.
     *
     * @param token token a verificar.
     * @return error si el token no es válido, o {@code null} si es válido.
     */
    public IErrorReg verifyToken(String token) {
        return getOAuthConsumer().checkToken(token);
    }

    /**
     * Determina si el usuario dueño del token es administrador del sistema
     * (roles 00 a 20). El dato se deriva del usuario mapeado al token (columna
     * usercode), no del contenido del campo data: data lo informa el cliente al
     * pedir el token y no es una fuente de autorización confiable.
     *
     * @param token token de acceso.
     * @return verdadero si el usuario del token es administrador del sistema.
     */
    protected boolean isTokenAdministrator(String token) {
        IAppUser user = getOAuthConsumer().getUserMapped(token);
        return user != null && user.isSysAdmin();
    }

    /**
     * Asigna el token a partir del encabezado indicado.
     *
     * <p>La sesión se crea informando el nombre de esta aplicación, de modo que
     * la propia creación verifique que el rol del usuario tenga acceso
     * concedido: si no lo tiene, la sesión no se crea.</p>
     *
     * @param tokenHeader encabezado con el token.
     */
    protected void setToken(String tokenHeader) {
        String token = getTokenFromHeader(tokenHeader);
        //Si el token es null
        if (Strings.isNullorEmpty(token)) {
            throw new TokenError("Debe proporcionar el token de autorización");
        }
        //Si ya esta activo este token en la sesiones
        if (getSecManager().getClientAuthRequestCache(token) != null) {
            return;
        }
        String appName = (requestContext == null) ? null : requestContext.getContextPath();
        //Crear la sesión
        IUserSession userSession = getSecManager().createSessionFromToken(token, appName);
        //Si no se puedo crear la sesion, probablemente el token no existe o esta bloqueado o ya expiro.
        if (userSession == null) {
            // Verificar y traer credenciales del servidor y grabar en el local
            if (!verifyTokenInMainServer(token)) {
                LOGGER.error("Este token ya expiró o es incorrecto Server: " + token);
                throw new TokenError("Este token ya expiró o es incorrecto");
            }
            userSession = getSecManager().createSessionFromToken(token, appName);
        }
        //Reverificar en el local
        if (userSession == null) {
            LOGGER.error("Este token ya expiró o es incorrecto: local " + token);
            throw new TokenError("Este token ya expiró o es incorrecto");
        }
    }

    /**
     * Extrae el valor del token de un encabezado.
     *
     * @param tokenHeader encabezado con el token.
     * @return valor del token.
     */
    protected String getTokenFromHeader(String tokenHeader) {
        if (Strings.isNullorEmpty(tokenHeader)) {
            throw new TokenError("Debe proporcionar el token de autorización");
        }
        //Formato esperado "esquema token" (ej. "Bearer xxxx"); sin esquema no es válido
        String[] tokens = tokenHeader.split("\\ ");
        if (tokens.length < 2) {
            throw new TokenError("Debe proporcionar el token de autorización");
        }
        return tokens[1];
    }

    /**
     * Verifica el token contra el servidor principal de autenticación.
     *
     * @param token token a verificar.
     * @return verdadero si el servidor principal lo valida.
     */
    protected boolean verifyTokenInMainServer(String token) {
        //Implementar en clases hijas
        return false;
    }
}

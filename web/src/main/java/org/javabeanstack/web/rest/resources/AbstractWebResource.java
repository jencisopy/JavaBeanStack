/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2018 Jorge Enciso
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

import com.google.common.base.Strings;
import javax.ejb.EJB;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import org.apache.log4j.Logger;
import org.javabeanstack.security.ISecManager;
import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.security.IOAuthConsumer;
import org.javabeanstack.security.model.IClientAuthRequestInfo;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.web.rest.exceptions.TokenError;
import org.javabeanstack.ws.resources.IWebResource;

/**
 *
 * @author Jorge Enciso
 */
public abstract class AbstractWebResource implements IWebResource {

    private static final Logger LOGGER = Logger.getLogger(AbstractWebResource.class);

    @EJB
    private IOAuthConsumer oAuthConsumer;

    @Context
    HttpServletRequest requestContext;

    @Override
    public abstract <T extends IDataService> T getDataService();

    @Override
    public abstract ISecManager getSecManager();

    @Override
    public Long getIdCompany(String authHeader) {
        String token = getTokenFromHeader(authHeader);
        IClientAuthRequestInfo info = getSecManager().getClientAuthRequestCache(token);
        if (info != null) {
            return info.getIdcompany();
        }
        return null;
    }

    @Override
    public final String getIpClient() {
        return requestContext.getRemoteAddr();
    }

    @Override
    public final String getRemoteHost() {
        return requestContext.getRemoteHost();
    }

    public String getToken(String authHeader) {
        String token = getTokenFromHeader(authHeader);
        IClientAuthRequestInfo info = getSecManager().getClientAuthRequestCache(token);
        if (info != null) {
            return info.getToken();
        }
        return null;
    }

    protected IOAuthConsumer getOAuthConsumer() {
        return oAuthConsumer;
    }

    public IErrorReg verifyToken(String token) {
        return getOAuthConsumer().checkToken(token);
    }

    protected void setToken(String tokenHeader) {
        String token = getTokenFromHeader(tokenHeader);
        //Si el token es null
        if (Strings.isNullOrEmpty(token)) {
            throw new TokenError("Debe proporcionar el token de autorización");
        }
        //Si ya esta activo este token en la sesiones
        if (getSecManager().getClientAuthRequestCache(token) != null) {
            return;
        }
        //Crear la sesión
        IUserSession userSession = getSecManager().createSessionFromToken(token);
        //Si no se puedo crear la sesion, probablemente el token no existe o esta bloqueado o ya expiro.
        if (userSession == null) {
            // Verificar y traer credenciales del servidor y grabar en el local
            if (!verifyTokenInMainServer(token)) {
                LOGGER.error("Este token ya expiró o es incorrecto Server: " + token);
                throw new TokenError("Este token ya expiró o es incorrecto");
            }
            userSession = getSecManager().createSessionFromToken(token);            
        }
        //Reverificar en el local
        if (userSession == null) {
            LOGGER.error("Este token ya expiró o es incorrecto: local " + token);
            throw new TokenError("Este token ya expiró o es incorrecto");
        }
    }

    protected String getTokenFromHeader(String tokenHeader) {
        if (Strings.isNullOrEmpty(tokenHeader)) {
            throw new TokenError("Debe proporcionar el token de autorización");
        }
        String[] tokens = tokenHeader.split("\\ ");
        if (tokens == null || tokens.length < 1) {
            throw new TokenError("Debe proporcionar el token de autorización");
        }
        return tokens[1];
    }

    protected boolean verifyTokenInMainServer(String token) {
        //Implementar en clases hijas
        return false;
    }
}

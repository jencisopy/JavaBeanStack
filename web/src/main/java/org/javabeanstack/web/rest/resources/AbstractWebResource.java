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
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.security.IOAuthConsumer;
import org.javabeanstack.security.model.ClientAuthRequestInfo;
import org.javabeanstack.security.model.IClientAuthRequestInfo;
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
        IClientAuthRequestInfo info = getSecManager().getClientAuthCache(authHeader);
        if (info != null){
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
        IClientAuthRequestInfo info = getSecManager().getClientAuthCache(authHeader);
        if (info != null){
            return info.getToken();
        }
        return null;
    }
    
    protected IOAuthConsumer getOAuthConsumer(){
        return oAuthConsumer;
    }

    public Boolean verifyToken(String token) {
        return getOAuthConsumer().isValidToken(token);
    }

    protected void setToken(String tokenHeader){
        String[] tokens = tokenHeader.split("\\ ");
        String token = tokens[1];
        Long idcompany = 0L;
        //Si el token es null
        if (Strings.isNullOrEmpty(token)) {
            throw new org.javabeanstack.web.rest.exceptions.TokenError("Debe proporcionar el token de autorización");
        }
        //Si ya se utilizo este token recientemente
        if (getSecManager().getClientAuthCache(tokenHeader) != null){
            return;
        }
        //Verificar válidez del token
        if (!verifyToken(token)){
            // Verificar y traer credenciales del servidor y grabar en el local
            if (!verifyTokenInMainServer(token)){
                LOGGER.error("Este token ya expiró o es incorrecto Server: "+token);
                throw new org.javabeanstack.web.rest.exceptions.TokenError("Este token ya expiró o es incorrecto");                
            }
            //Reverificar en el local
            if (!verifyToken(token)){
                LOGGER.error("Este token ya expiró o es incorrecto: local "+token);
                throw new org.javabeanstack.web.rest.exceptions.TokenError("Este token ya expiró o es incorrecto");                
            }
        }
        //Asignar idcompany
        IAppCompany appCompanyToken = getOAuthConsumer().getCompanyMapped(token);
        if (appCompanyToken != null) {
            if (appCompanyToken.getIdcompanymask() != null) {
                idcompany = appCompanyToken.getIdcompanymask();
            }
            else{
                idcompany = appCompanyToken.getIdcompany();
            }
        }
        // Guardar los datos de autenticación en el cache.
        IClientAuthRequestInfo requestInfo = new ClientAuthRequestInfo();
        requestInfo.setIdcompany(idcompany);
        requestInfo.setAppAuthToken(getOAuthConsumer().findAuthToken(token));
        getSecManager().addClientAuthCache(tokenHeader, requestInfo);
    }

    protected boolean verifyTokenInMainServer(String token){
        //Implementar en clases hijas
        return false;
    }
}

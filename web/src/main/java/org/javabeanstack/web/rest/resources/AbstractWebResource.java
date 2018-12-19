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

import javax.ejb.EJB;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import org.javabeanstack.security.ISecManager;
import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.security.IOAuthConsumer;
import org.javabeanstack.ws.resources.IWebResource;

/**
 *
 * @author Jorge Enciso
 */
public abstract class AbstractWebResource implements IWebResource {

    @EJB
    private IOAuthConsumer oAuthConsumer;

    private Long idcompany;
    private Long idPerson;
    private String personRol;
    private String token;

    @Context
    HttpServletRequest requestContext;

    @Override
    public abstract <T extends IDataService> T getDataService();

    @Override
    public abstract ISecManager getSecManager();

    @Override
    public Long getIdCompany() {
        return idcompany;
    }

    @Override
    public final String getIpClient() {
        return requestContext.getRemoteAddr();
    }

    @Override
    public final String getRemoteHost() {
        return requestContext.getRemoteHost();
    }

    @Override
    public Long getIdPerson() {
        return idPerson;
    }

    @Override
    public String getPersonRol() {
        return personRol;
    }

    public String getToken() {
        return token;
    }
    
    protected IOAuthConsumer getOAuthConsumer(){
        return oAuthConsumer;
    }

    public final Boolean verifyToken(String token) {
        if (token == null) {
            throw new org.javabeanstack.web.rest.exceptions.TokenError("Debe proporcionar el token de autorización");
        }
        if (!oAuthConsumer.isValidToken(token)) {
            throw new org.javabeanstack.web.rest.exceptions.TokenError("Este token ya expiró o es es incorrecto");
        }
        return true;
    }

    protected void setToken(String tokenHeader) {
        String[] tokens = tokenHeader.split("\\ ");
        this.token = tokens[1];
        this.idPerson = 0L;
        this.idcompany = 0L;
        verifyToken(this.token);
        
        IAppCompany appCompanyToken = oAuthConsumer.getCompanyMapped(this.token);
        if (appCompanyToken != null) {
            if (appCompanyToken.getIdcompanymask() != null) {
                this.idcompany = appCompanyToken.getIdcompanymask();
            }
            else{
                this.idcompany = appCompanyToken.getIdcompany();
            }
        }
    }
    
    protected void setIdPerson(Long idPerson) {
        this.idPerson = idPerson;
    }

    protected void setPersonRol(String personRol) {
        this.personRol = personRol;
    }
}

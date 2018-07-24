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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import org.javabeanstack.security.ISecManager;
import org.javabeanstack.services.IDataService;
import org.javabeanstack.ws.resources.IWebResource;

/**
 *
 * @author Jorge Enciso
 */
public abstract class AbstractWebResource implements IWebResource {
    private Long idempresa;
    private Long idPerson;
    private String personRol;
    private String token;
    
    @Context HttpServletRequest requestContext;    

    @Override
    public abstract <T extends IDataService> T getDataService();

    @Override
    public abstract ISecManager getSecManager();

    @Override
    public Long getIdempresa() {
        return idempresa;
    }

    @Override
    public Long getIdCompany() {
        return idempresa;
    } 
    
    @Override
    public final String getIpClient() {
        return requestContext.getRemoteAddr();
    }

    public final Boolean verifyToken(String token) {
        if (token == null){
            throw new org.javabeanstack.web.rest.exceptions.TokenError("Debe proporcionar el token de autorización");
        }
        if (!getSecManager().isSesionIdValid(token)){
            throw new org.javabeanstack.web.rest.exceptions.TokenError("Este token ya expiró o es es incorrecto");
        }
        return true;
    }
    
    protected void setToken(String token) {
        verifyToken(token);
        String[] tokens = token.split("\\-");
        //TODO analizar este codigo
        this.idempresa = Long.parseLong(tokens[0]);
        this.idPerson = Long.parseLong(tokens[1]);
        this.token = token;
    }

    @Override
    public Long getIdPerson() {
        return idPerson;
    }

    @Override
    public String getPersonRol() {
        return personRol;
    }
}

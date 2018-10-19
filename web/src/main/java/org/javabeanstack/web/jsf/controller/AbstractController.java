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

package org.javabeanstack.web.jsf.controller;

import org.javabeanstack.web.util.FacesContextUtil;
import java.io.Serializable;
import org.javabeanstack.security.IUserSession;

/**
 * Clase base controller.
 * @author Jorge Enciso
 */
public abstract class AbstractController implements Serializable{
    private FacesContextUtil facesCtx;
            
    public AbstractController(){
        facesCtx = new FacesContextUtil();
    }
 
    /**
     * Devuelve la instancia de FacesContext
     * @return la instancia de FacesContext
     */
    public FacesContextUtil getFacesCtx(){
        if (facesCtx == null){
            facesCtx = new FacesContextUtil();
        }
        return facesCtx;
    }
    

    /**
     * Identificador de la empresa a la que el usuario se logeo
     * @return Identificador de la empresa a la que el usuario se logeo
     */
    public Long getIdEmpresa() {
        return this.getUserSession().getIdEmpresa();
    }

    /**
     * Identificador de la empresa a la que el usuario se logeo
     * @return Identificador de la empresa a la que el usuario se logeo
     */
    public Long getIdCompany() {
        return this.getUserSession().getIdCompany();
    }
    
    /**
     * Devuelve el objeto UserSession conteniendo información sobre la sesión
     * del usuario actual
     * @return usersession.
     */
    public IUserSession getUserSession() {
        IUserSession userSession = (IUserSession)facesCtx.getSessionMap().get("userSession");
        return userSession;
    }
    
    /**
     * Devuelve el id del usuario actual.
     * @return id del usuario actual.
     */
    public Long getUserId() {
        IUserSession userSession = (IUserSession)facesCtx.getSessionMap().get("userSession");
        return userSession.getUser().getIduser();
    }

    /**
     * Cierra la sesión del usuario.
     * @return link para redireccionar a la página de logeo
     */
    public String logout() {
        facesCtx.getSessionMap().put("userSession", null);
        return "/login.xhtml?faces-redirect=true";
    }    
 }

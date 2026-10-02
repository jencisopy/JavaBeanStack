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

package org.javabeanstack.web.jsf.controller;

import org.javabeanstack.web.util.FacesContextUtil;
import java.io.Serializable;
import org.javabeanstack.security.model.IUserSession;

/**
 * Clase base controller.
 * @author Jorge Enciso
 */
public abstract class AbstractController implements Serializable{
    private FacesContextUtil facesCtx;
            
    /**
     * Constructor por defecto.
     */
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
     * Cierra la sesión del usuario: quita la sesión del usuario e invalida la
     * sesión HTTP, con lo que se liberan en el acto los beans de sesión y la
     * cookie deja de servir. Antes solo se quitaba el atributo y la sesión HTTP
     * seguía viva hasta su vencimiento.
     *
     * <p>Después de llamarlo no hay que volver a usar la sesión HTTP en la
     * misma petición: el destino devuelto es una redirección.</p>
     *
     * @return link para redireccionar a la página de logeo
     */
    public String logout() {
        getFacesCtx().getSessionMap().put("userSession", null);
        getFacesCtx().getExternalContext().invalidateSession();
        return "/login.xhtml?faces-redirect=true";
    }
 }

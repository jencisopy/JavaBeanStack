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

import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.javabeanstack.data.DataLink;
import org.javabeanstack.security.ISecManager;
import org.javabeanstack.data.services.IAppCompanySrv;

/**
 * Clase controller de autenticación, recibe peticiones de logeo del usuario lo
 * procesa utilizando los componentes necesarios
 *
 * @author Jorge Enciso
 */
@Named(value = "authBean")
@ViewScoped
public class AuthController extends AbstractAuthController {

    private static final long serialVersionUID = 1L;

    /**
     * Objeto que abstrae las funcionalidades de la capa de seguridad
     */
    @EJB
    private ISecManager secManager;
    /**
     * Servicio de la instancia AppCompany
     */
    @EJB
    private IAppCompanySrv appCompanySrv;
    /**
     * Objeto que abstrae el acceso a los datos
     */
    @Inject
    private DataLink dataLink;

    public AuthController() {
    }

    @Override
    public DataLink getDataLink() {
        return dataLink;
    }

    @Override
    public ISecManager getSecManager() {
        return secManager;
    }

    @Override
    public IAppCompanySrv getAppCompanySrv() {
        return appCompanySrv;
    }
}

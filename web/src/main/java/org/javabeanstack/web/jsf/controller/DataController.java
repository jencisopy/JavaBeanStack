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

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.javabeanstack.data.DataLink;
import org.javabeanstack.data.IDataLink;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.exceptions.SessionError;
import org.javabeanstack.security.model.IUserSession;


/**
 *
 * @author Jorge Enciso
 * @param <T>
 */
public abstract class DataController<T extends IDataRow> extends AbstractDataController<T> {
    @Inject
    private DataLink dao;

    @Inject
    private DataLink daoCatalog;
    
    public DataController(){
    }  
    
    public DataController(Class<T> type){
        this.setType(type);
    }

    @PostConstruct
    public void init() {
        IUserSession userSession = (IUserSession)getFacesCtx().getSession().getAttribute("userSession"); 
        try {
            getDAO().setUserSession(userSession);
            open(getOrder(), getFilter(), true, getMaxRows());
            if (getErrorApp() != null){
                getFacesCtx().showError(getErrorApp().getMessage());
            }
        } catch (SessionError ex) {
            getFacesCtx().showError(ex.getMessage());
            logout();
        } catch (Exception ex) {
            getFacesCtx().showError(ex.getMessage());
        }
    }

    @Override
    public IDataLink getDAO() {
        return dao;
    }

    @Override
    public IDataLink getDAOCatalog() {
        return daoCatalog;
    }
}

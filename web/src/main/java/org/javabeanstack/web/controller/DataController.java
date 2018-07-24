/*
* Copyright (c) 2015-2017 OyM System Group S.A.
* Capitan Cristaldo 464, Asunción, Paraguay
* All rights reserved. 
*
* NOTICE:  All information contained herein is, and remains
* the property of OyM System Group S.A. and its suppliers,
* if any.  The intellectual and technical concepts contained
* herein are proprietary to OyM System Group S.A.
* and its suppliers and protected by trade secret or copyright law.
* Dissemination of this information or reproduction of this material
* is strictly forbidden unless prior written permission is obtained
* from OyM System Group S.A.
*/
package org.javabeanstack.web.controller;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.javabeanstack.data.DataLink;
import org.javabeanstack.data.IDataLink;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.exceptions.SessionError;
import org.javabeanstack.security.IUserSession;


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

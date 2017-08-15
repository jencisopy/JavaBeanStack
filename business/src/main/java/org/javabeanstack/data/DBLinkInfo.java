/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javabeanstack.data;

import org.javabeanstack.security.IUserSession;

/**
 *
 * @author Jorge Enciso
 */
public class DBLinkInfo implements IDBLinkInfo {
    private IUserSession userSession;
    
    @Override
    public IDBFilter getDBFilter() {
        return null;
    }

    @Override
    public String getPersistUnit() {
        if (userSession == null){
            return IDBManager.CATALOGO;
        }
        return userSession.getPersistenceUnit();
    }

    @Override
    public IUserSession getUserSession() {
        return userSession;
    }

    @Override
    public void setUserSession(IUserSession userSession) {
        this.userSession = userSession;
    }
}

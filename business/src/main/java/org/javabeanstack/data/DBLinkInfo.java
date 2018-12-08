/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 Jorge Enciso
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
package org.javabeanstack.data;

import org.javabeanstack.exceptions.SessionError;
import org.javabeanstack.model.IAppAuthConsumerToken;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.model.IAppUser;
import org.javabeanstack.security.IOAuthConsumer;
import org.javabeanstack.security.IUserSession;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.Strings;

/**
 * Clase que contiene información necesaria para el acceso a los datos. 
 * ej. unidad de persistencia, sesión id del usuario etc.
 * 
 * @author Jorge Enciso
 */
public class DBLinkInfo implements IDBLinkInfo {
    private IUserSession userSession;
    private IAppAuthConsumerToken token;
    private IOAuthConsumer oAuthConsumer;
    private IAppCompany appCompanyToken;
    private IAppUser appUserToken;
            
    /**
     * Devuelve DBFilter conteniendo los filtros que deben ser aplicados
     * en los queries.
     * @return DBFilter.
     */
    @Override
    public IDBFilter getDBFilter() {
        if (userSession != null){
            return userSession.getDBFilter();
        }
        if (token != null && oAuthConsumer != null){
            return oAuthConsumer.getDBFilter(token);
        }
        return null;
    }

    
    /**
     * 
     * @return idcompany
     */
    @Override
    public Long getIdCompany() {
        if (userSession == null && token == null){
            return null;
        }
        if (userSession != null){
            return userSession.getIdCompany();
        }
        if (token != null && oAuthConsumer != null){
            if (appCompanyToken != null){
                if (appCompanyToken.getIdcompanymask() != null){
                    return appCompanyToken.getIdcompanymask();
                }
                return appCompanyToken.getIdcompany();
            }
        }
        return null;
    }
    
    /**
     * 
     * @return unidad de persistencia.
     */
    @Override
    public String getPersistUnit() {
        if (userSession == null && token == null){
            return IDBManager.CATALOGO;
        }
        if (userSession != null){
            return userSession.getPersistenceUnit();
        }
        if (token != null && oAuthConsumer != null){
            if (appCompanyToken != null){
                return Fn.nvl(appCompanyToken.getPersistentUnit(),"").trim();
            }
        }
        return null;
    }

    /**
     * 
     * @return objeto con información de la sesión del usuario.
     */
    @Override
    public IUserSession getUserSession() {
        return userSession;
    }

    /**
     * Asigna un objeto con información de la sesión del usuario.
     * @param userSession 
     */
    @Override
    public void setUserSession(IUserSession userSession) {
        this.userSession = userSession;
    }
    
    /**
     * Asigna un objeto con información autorizaciones de acceso
     * @param token
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public void setToken(IAppAuthConsumerToken token) throws SessionError {
        if (!oAuthConsumer.isValidToken(token.getToken())){
            throw new SessionError("Token inválido");
        }
        this.token = token;
        this.appCompanyToken = oAuthConsumer.getCompanyMapped(token);
        this.appUserToken = oAuthConsumer.getUserMapped(token.getToken());        
    }

    @Override
    public void setoAuthConsumer(IOAuthConsumer oAuthConsumer) {
        this.oAuthConsumer = oAuthConsumer;
    }
    
    @Override
    public String getAppUserId(){
        String result = "";
        if (getUserSession() != null) {
            result = getUserSession().getUser().getPass();
        }
        if (Strings.isNullorEmpty(result) && token != null) {
            if (appUserToken != null){
                result = appUserToken.getPass();
            }
        }
        return result;
    }
    
    @Override
    public String getSessionOrTokenId(){
        String result = "";
        if (getUserSession() != null) {
            result = getUserSession().getSessionId();
        }
        if (Strings.isNullorEmpty(result) && token != null) {
            result = token.getToken();
        }
        return result;
    }
}

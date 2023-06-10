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

import org.apache.log4j.Logger;

import org.javabeanstack.exceptions.SessionError;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.model.IAppUser;
import org.javabeanstack.security.IOAuthConsumer;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.model.IAppAuthConsumerToken;

/**
 * Clase que contiene información necesaria para el acceso a los datos. ej.
 * unidad de persistencia, sesión id del usuario etc.
 *
 * @author Jorge Enciso
 */
public class DBLinkInfo implements IDBLinkInfo {

    private static final Logger LOGGER = Logger.getLogger(DBLinkInfo.class);
    private IUserSession userSession;
    private IAppAuthConsumerToken token;
    private IAppCompany appCompanyToken;
    

    /**
     * Devuelve DBFilter conteniendo los filtros que deben ser aplicados en los
     * queries.
     *
     * @return DBFilter.
     */
    @Override
    public IDBFilter getDBFilter() {
        LOGGER.debug("getDBFilter in");
        if (userSession != null && userSession.getUser() != null) {
            return userSession.getDBFilter();
        }
        return null;
    }

    /**
     *
     * @return idcompany
     */
    @Override
    public Long getIdCompany() {
        LOGGER.debug("getIdCompany in");
        if (userSession == null || userSession.getUser() == null) {
            return null;
        }
        if (userSession != null) {
            return userSession.getIdCompany();
        }
        if (token != null && appCompanyToken != null) {
            if (appCompanyToken.getIdcompanymask() != null) {
                return appCompanyToken.getIdcompanymask();
            }
            return appCompanyToken.getIdcompany();
        }
        return null;
    }

    /**
     *
     * @return unidad de persistencia.
     */
    @Override
    public String getPersistUnit() {
        LOGGER.debug("getPersistUnit in");
        if (userSession == null || userSession.getUser() == null) {
            return IDBManager.CATALOGO;
        }
        if (userSession != null && userSession.getUser() != null) {
            return userSession.getPersistenceUnit();
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
     *
     * @param userSession
     */
    @Override
    public void setUserSession(IUserSession userSession) {
        this.userSession = userSession;
    }

    /**
     * Asigna un objeto con información autorizaciones de acceso
     *
     * @param token
     * @param oAuthConsumer
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public void setToken(IAppAuthConsumerToken token, IOAuthConsumer oAuthConsumer) throws SessionError {
        LOGGER.debug("setToken in");
        setToken(token, oAuthConsumer, false);
    }

    /**
     * Asigna un objeto con información autorizaciones de acceso
     *
     * @param token
     * @param oAuthConsumer
     * @param noValid
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public void setToken(IAppAuthConsumerToken token, IOAuthConsumer oAuthConsumer, boolean noValid) throws SessionError {
        LOGGER.debug("setToken in");
        if (!noValid && !oAuthConsumer.isValidToken(token.getToken())) {
            throw new SessionError("Token inválido");
        }
        this.token = token;
        this.appCompanyToken = oAuthConsumer.getCompanyMapped(token);
    }


    /**
     * Devuelve el identificador del usuario
     *
     * @return identificador del usuario
     */
    @Override
    public String getAppUserId() {
        LOGGER.debug("getAppUserId in");
        String result = "";
        if (getUserSession() != null && getUserSession().getUser() != null) {
            result = getUserSession().getUser().getPass();
        }
        return result;
    }

    @Override
    public String getSessionOrTokenId() {
        LOGGER.debug("getSessionOrTokenId in");
        if (token != null) {
            return token.getToken();
        }
        if (getUserSession() != null && getUserSession().getUser() != null) {
            return getUserSession().getSessionId();
        }
        return "";
    }

    @Override
    public String getUuidDevice() {
        LOGGER.debug("getUuidDevice in");
        String result = "";
        if (getUserSession() != null && getUserSession().getUser() != null && token != null) {
            result = token.getUuidDevice();
        }
        return result;
    }
}

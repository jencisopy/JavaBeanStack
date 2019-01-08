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
import org.javabeanstack.model.IAppAuthConsumerToken;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.model.IAppUser;
import org.javabeanstack.security.IOAuthConsumer;
import org.javabeanstack.security.IUserSession;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.Strings;

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
    private IAppUser appUserToken;
    private IDBFilter dbFilterToken;

    /**
     * Devuelve DBFilter conteniendo los filtros que deben ser aplicados en los
     * queries.
     *
     * @return DBFilter.
     */
    @Override
    public IDBFilter getDBFilter() {
        LOGGER.debug("getDBFilter in");
        if (userSession != null) {
            return userSession.getDBFilter();
        }
        if (token != null) {
            return dbFilterToken;
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
        if (userSession == null && token == null) {
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
        if (userSession == null && token == null) {
            return IDBManager.CATALOGO;
        }
        if (userSession != null) {
            return userSession.getPersistenceUnit();
        }
        if (token != null && appCompanyToken != null) {
            return Fn.nvl(appCompanyToken.getPersistentUnit(), "").trim();
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
        this.appUserToken = oAuthConsumer.getUserMapped(token.getToken());
        this.dbFilterToken = oAuthConsumer.getDBFilter(token);
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
        if (getUserSession() != null) {
            result = getUserSession().getUser().getPass();
        }
        if (Strings.isNullorEmpty(result) && token != null) {
            if (appUserToken != null) {
                result = appUserToken.getPass();
            }
        }
        return result;
    }

    /**
     * Devuelve el identificador de la sesión o el token
     *
     * @return el identificador de la sesión o el token
     */
    @Override
    public String getSessionOrTokenId() {
        LOGGER.debug("getSessionOrTokenId in");
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

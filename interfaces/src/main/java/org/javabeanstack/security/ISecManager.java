/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
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

package org.javabeanstack.security;

import java.util.Map;
import org.javabeanstack.model.IAppAuthConsumerToken;
import org.javabeanstack.model.IAppUser;
import org.javabeanstack.security.model.IClientAuthRequestInfo;
import org.javabeanstack.security.model.IUserSession;


/**
 *
 * @author Jorge Enciso
 */
public interface ISecManager {
    IUserSession createSession(String userLogin, String password, Object idcompany, Integer idleSessionExpireInMinutes, Map<String, Object> otherParams);
    IUserSession reCreateSession(String sessionId, Object idcompany);
    IUserSession createSessionFromToken(String token);
    String getCompanyList();
    String getUserRol(String userLogin);
    Boolean isUserMemberOf(String user, String userGroup);    
    Boolean isSessionIdValid(String sessionId);
    Boolean login(String userLogin, String password, Map<String, Object> otherParams) throws Exception;
    IUserSession login2(String userLogin, String password, Map<String, Object> otherParams) throws Exception;
    void logout(IUserSession userSession);
    void logout(String sessionId);
    IClientAuthRequestInfo getClientAuthRequestCache(String authHeader);
    IAppUser getAppUserFromPwd(String appUserPass);
    IAppAuthConsumerToken getAppAuthConsumerToken(String token);
}

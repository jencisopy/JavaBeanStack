package org.javabeanstack.security;

import org.javabeanstack.security.model.IClientAuthRequestInfo;
import org.javabeanstack.security.model.IUserSession;


/**
 *
 * @author Jorge Enciso
 */
public interface ISecManager {
    IUserSession createSession(String userLogin, String password, Object idcompany, Integer idleSessionExpireInMinutes);
    IUserSession reCreateSession(String sessionId, Object idcompany);
    String getCompanyList();
    String getUserRol(String userLogin);
    Boolean isUserMemberOf(String user, String userGroup);    
    Boolean isSessionIdValid(String sessionId);
    Boolean login(String userLogin, String password) throws Exception;
    IUserSession login2(String userLogin, String password) throws Exception;
    void logout(IUserSession userSession);
    void logout(String sessionId);
    IClientAuthRequestInfo getClientAuthCache(String authHeader);
    void addClientAuthCache(String authHeader, IClientAuthRequestInfo authRequestInfo);
}

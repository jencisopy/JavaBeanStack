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

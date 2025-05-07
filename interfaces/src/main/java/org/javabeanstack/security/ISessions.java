package org.javabeanstack.security;

import java.util.Map;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.data.IDBLinkInfo;
import org.javabeanstack.security.model.IClientAuthRequestInfo;



/**
 *
 * @author Jorge Enciso
 */
public interface ISessions {
    Boolean checkCompanyAccess(Long iduser, Long idcompany) throws Exception;
    IUserSession createSessionFromToken(String authToken);
    IUserSession createSession(String userLogin, String password, Object idcompany, Integer idleSessionExpireInMinutes, Map<String, Object> otherParams);
    IUserSession reCreateSession(String sessionId, Object idcompany);
    IUserSession getUserSession(String sessionId);
    IUserSession login(String userLogin, String password, Map<String, Object> otherParams) throws Exception;
    IDBLinkInfo getDBLinkInfo(String sessionId);    
    IClientAuthRequestInfo getClientAuthRequestCache(String token);        
    boolean isUserValid(Long iduser) throws Exception;
    boolean checkAuthConsumerData(IOAuthConsumerData data);        
    void logout(String sessionId);
    Object getSessionInfo(String sessionId, String key);
    void addSessionInfo(String sessionId, String key, Object info);
    void removeSessionInfo(String sessionId, String key);
}

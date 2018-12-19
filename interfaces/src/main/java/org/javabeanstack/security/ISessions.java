package org.javabeanstack.security;

import org.javabeanstack.data.IDBLinkInfo;



/**
 *
 * @author Jorge Enciso
 */
public interface ISessions {
    Boolean checkCompanyAccess(Long iduser, Long idcompany) throws Exception;
    IUserSession createSession(String userLogin, String password, Object idcompany, Integer idleSessionExpireInMinutes);
    IUserSession reCreateSession(String sessionId, Object idcompany);
    IUserSession getUserSession(String sessionId);
    IUserSession login(String userLogin, String password) throws Exception;
    IDBLinkInfo getDBLinkInfo(String sessionId);
    boolean isUserValid(Long iduser) throws Exception;
    boolean checkAuthConsumerData(IOAuthConsumerData data);        
    void logout(String sessionId);
}

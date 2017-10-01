package org.javabeanstack.security;


/**
 *
 * @author Jorge Enciso
 */
public interface ISessions {
    Boolean checkCompanyAccess(Long iduser, Long idcompany) throws Exception;
    IUserSession createSession(String userLogin, String password, Object idcompany, Integer idleSessionExpireInMinutes);
    IUserSession getUserSession(String sessionId);
    IUserSession login(String userLogin, String password) throws Exception;
    void logout(String sessionId);
}

package org.javabeanstack.security;


/**
 *
 * @author Jorge Enciso
 */
public interface ISecManager {
    IUserSession createSession(String userLogin, String password, Object idempresa, Integer idleSessionExpireInMinutes);
    String getListEmpresa();
    String getUserRol(String userLogin);
    Boolean isUserMemberOf(String user, String userGroup);    
    Boolean isSesionIdValid(String sesionId);
    Boolean login(String userLogin, String password) throws Exception;
    IUserSession login2(String userLogin, String password) throws Exception;
    void logout(IUserSession userSession);
    void logout(String sessionId);
}

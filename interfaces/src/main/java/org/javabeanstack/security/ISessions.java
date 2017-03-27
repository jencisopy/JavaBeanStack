package org.javabeanstack.security;


/**
 *
 * @author Jorge Enciso
 */
public interface ISessions {
    Boolean checkEmpresaPermision(Long idusuario, Long idempresa) throws Exception;
    IUserSession createSession(String userLogin, String password, Object idempresa, Integer idleSessionExpireInMinutes);
    IUserSession getUserSession(String sessionId);
    IUserSession login(String userLogin, String password) throws Exception;
    void logout(String sessionId);
}

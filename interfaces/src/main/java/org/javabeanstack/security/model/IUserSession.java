package org.javabeanstack.security.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import org.javabeanstack.data.IDBFilter;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.model.IAppUser;

/**
 *
 * @author Jorge Enciso
 */
public interface IUserSession extends Serializable {

    /**
     *
     * @return Devuelve el objeto empresa a la cual se accedio en esta sesión.
     */
    IAppCompany getCompany();
    /**
     *
     * @return Devuelve el objeto empresa a la cual se accedio en esta sesión.
     */
    IAppCompany getEmpresa();

    /**
     *
     * @return Devuelve un objeto error si la creación de la sesión no fue exitosa
     */
    IErrorReg getError();

    /**
     *
     * @return Devuelve el nombre de la máquina desde la cual fue creada la sesión.
     */
    String getHost();

    /**
     *
     * @return Devuelve el ip de la terminal, desde la cual fue creada la sesión
     */
    String getIp();

    /**
     *
     * @return Devuelve la ultima fecha y hora que se hizo referencia a la sesión.
     */
    Date getLastReference();

    /**
     *
     * @return Devuelve la unidad de persistencia donde se encuentra la configuración
     * para acceso a la base de datos donde esta la información de la empresa logeada
     */
    String getPersistenceUnit();

    /**
     *
     * @return Devuelve el identificador de la sesión
     */
    String getSessionId();

    /**
     *
     * @return Devuelve la fecha y hora que fue logeada la sesión
     */
    Date getTimeLogin();
    Long getIdCompany();    
    Long getIdEmpresa();
    Integer getIdleSessionExpireInMinutes();
    <T extends IDBFilter> T getDBFilter();
    IAppUser getUser();
    void setCompany(IAppCompany company);    
    void setEmpresa(IAppCompany empresa);
    void setIdCompany(Long idcompany);        
    void setIdEmpresa(Long idempresa);    
    void setError(IErrorReg error);
    void setHost(String host);
    void setIp(String ip);
    void setLastReference(Date date);
    void setPersistenceUnit(String persistenceUnit);
    void setSessionId(String sessionId);
    void setTimeLogin(Date timeLogin);
    void setUser(IAppUser user);
    void setIdleSessionExpireInMinutes(Integer minutes);
    <T extends IDBFilter> void setDBFilter(T dbFilter);    
    Map<String,Object> getInfo();
    Object getInfo(String key);    
    void addInfo(String key, Object info);
    IClientAuthRequestInfo getClientAuthRequestInfo();
    void setClientAuthRequestInfo(IClientAuthRequestInfo requestInfo);
}

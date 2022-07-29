package org.javabeanstack.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import org.javabeanstack.data.IDataRow;


/**
 *
 * @author Jorge Enciso
 */
public interface IAppUser extends IDataRow, Serializable {
    public static final String ANALISTA = "00";
    public static final String ADMINISTRADOR = "20";
    public static final String USUARIO = "30";    
    
    public static final Short ISUSER = 1;
    public static final Short ISUSERGROUP = 2;
    
    Long getIduser();    
    String getLogin();
    String getCode();
    String getFullName();
    String getDescription();
    
    String getPass();
    String getPassBackup();
    
    String getPassConfirm();
    String getPassConfirm2();
    
    
    Boolean getDisabled();
    LocalDateTime getExpiredDate();
    
    Long getIdcompany();        
    List<IAppCompanyAllowed> getAppCompanyAllowedList();
    List<IAppUserMember> getUserMemberList();
    
    String getRol();
    String getHighRol();
    String getAllRoles();    
    String getAppRol();    
    Short getType();
    byte[] getAvatar();
    
    void setIduser(Long iduser);    
    void setLogin(String loginName);
    void setCode(String code);
    void setFullName(String name);    
    void setDescription(String description);    
    
    void setPass(String password);
    void setPassConfirm(String passwordConfirm);
    void setPassConfirm2(String passwordConfirm2);    

    void setIdcompany(Long idcompany);    
    void setAppCompanyAllowedList(List<IAppCompanyAllowed> appCompanyAllowedList);
    void setUserMemberList(List<IAppUserMember> userMemberList);
    
    void setDisabled(Boolean disable);
    void setExpiredDate(LocalDateTime expira);

    void setRol(String rol);
    void setAppRol(String appRol);    
    void setType(Short tipo);
    void setAvatar(byte[] avatar);
    boolean isAdministrator();
}

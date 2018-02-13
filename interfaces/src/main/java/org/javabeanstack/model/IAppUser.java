package org.javabeanstack.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import org.javabeanstack.data.IDataRow;


/**
 *
 * @author Jorge Enciso
 */
public interface IAppUser extends IDataRow, Serializable {
    Long getIduser();    
    String getLogin();
    String getCode();
    String getFullName();
    String getDescription();
    
    String getPass();
    String getPassConfirm();
    Boolean getDisable();
    Date getExpiredDate();
    
    Long getIdcompany();        
    List<IAppCompanyAllowed> getAppCompanyAllowedList();
    List<IAppUserMember> getUserMemberList();
    
    String getRol();
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

    void setIdcompany(Long idcompany);    
    void setAppCompanyAllowedList(List<IAppCompanyAllowed> appCompanyAllowedList);
    void setUserMemberList(List<IAppUserMember> userMemberList);
    
    void setDisable(Boolean disable);
    void setExpiredDate(Date expira);

    void setRol(String rol);
    void setAppRol(String appRol);    
    void setType(Short tipo);
    void setAvatar(byte[] avatar);
}

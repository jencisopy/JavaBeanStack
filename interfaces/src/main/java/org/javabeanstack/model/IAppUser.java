/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 3 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
* MA 02110-1301  USA
 */

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
    public static final String SUPERUSER = "01";
    public static final String ADMINISTRADOR = "20";
    public static final String ADMINISTRADORSYSTEM = "20";
    public static final String ADMINCOMPANY = "21";
    public static final String TOKEN = "25";    
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
    boolean isSysAdmin();
    boolean isCompanyAdmin();    
    boolean isSuperUser();
}

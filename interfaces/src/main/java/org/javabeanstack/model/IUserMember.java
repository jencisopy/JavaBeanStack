package org.javabeanstack.model;

import java.io.Serializable;
import org.javabeanstack.data.IDataRow;

/**
 *
 * @author Jorge Enciso
 */
public interface IUserMember extends IDataRow, Serializable {
    Long getIdusermember();
    void setIdusermember(Long idUserMember);    
    IUser getUserMember();
    void setUserMember(IUser user);
    IUser getUserGroup();
    void setUserGroup(IUser user);
}

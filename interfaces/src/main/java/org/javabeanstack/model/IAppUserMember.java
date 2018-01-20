package org.javabeanstack.model;

import java.io.Serializable;
import org.javabeanstack.data.IDataRow;

/**
 *
 * @author Jorge Enciso
 */
public interface IAppUserMember extends IDataRow, Serializable {
    Long getIdusermember();
    void setIdusermember(Long idUserMember);    
    IAppUser getUserMember();
    void setUserMember(IAppUser user);
    IAppUser getUserGroup();
    void setUserGroup(IAppUser user);
}

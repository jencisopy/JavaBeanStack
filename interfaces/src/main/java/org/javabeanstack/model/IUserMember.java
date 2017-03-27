package org.javabeanstack.model;

import java.io.Serializable;
import org.javabeanstack.data.IDataRow;

/**
 *
 * @author Jorge Enciso
 */
public interface IUserMember extends IDataRow, Serializable {
    Long getIdusuariomiembro();
    void setIdusuariomiembro(Long idusuariomiembro);    
    IUser getUsuarioMiembro();
    void setUsuarioMiembro(IUser usuario);
    IUser getUsuarioGrupo();
    void setUsuarioGrupo(IUser usuario);
}

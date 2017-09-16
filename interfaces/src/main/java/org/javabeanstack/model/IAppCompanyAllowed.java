package org.javabeanstack.model;

import java.io.Serializable;
import org.javabeanstack.data.IDataRow;

/**
 *
 * @author Jorge Enciso
 */
public interface IAppCompanyAllowed extends IDataRow, Serializable {
    Long getIdcompany();
    Long getIduser();
    boolean getDeny();
    boolean getAllow();
    String getAppuser();
    void setIdcompany(Long idempresa);
    void setIduser(Long idusuario);
    void setDeny(boolean negar);
    void setAllow(boolean permitir);
    void setAppuser(String appuser);
}

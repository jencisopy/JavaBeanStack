package org.javabeanstack.model;

import java.io.Serializable;
import java.util.Date;
import org.javabeanstack.data.IDataRow;

//TODO Cambiar el nombre de esta clase
/**
 *
 * @author Jorge Enciso
 */
public interface IDicPermisoEmpresa extends IDataRow, Serializable {
    Long getIdempresa();
    Long getIdusuario();
    boolean getNegar();
    boolean getPermitir();
    String getAppuser();
    Date getFechacreacion();
    Date getFechamodificacion();
    void setIdempresa(Long idempresa);
    void setIdusuario(Long idusuario);
    void setNegar(boolean negar);
    void setPermitir(boolean permitir);
    void setAppuser(String appuser);
    void setFechacreacion(Date fechacreacion);
    void setFechamodificacion(Date fechamodificacion);
}

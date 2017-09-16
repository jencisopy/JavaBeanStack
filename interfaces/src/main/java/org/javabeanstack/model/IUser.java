package org.javabeanstack.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import org.javabeanstack.data.IDataRow;


/**
 *
 * @author Jorge Enciso
 */
public interface IUser extends IDataRow, Serializable {
    String getClave();
    String getClave2();
    String getCodigo();
    String getDescripcion();
    List<IAppCompanyAllowed> getDicPermisoEmpresaList();
    Long getIdempresa();    
    Boolean getDisable();
    Date getExpira();
    Date getFechamodificacion();
    Long getIdusuario();
    List<IUserMember> getListaUsuarioMiembro();
    String getNombre();
    String getRol();
    String getAppRol();    
    Short getTipo();
    void setClave(String clave);
    void setClave2(String clave2);
    void setCodigo(String codigo);
    void setDescripcion(String descripcion);
    void setDicPermisoEmpresaList(List<IAppCompanyAllowed> dicPermisoEmpresaList);
    void setDisable(Boolean disable);
    void setIdempresa(Long idempresa);
    void setExpira(Date expira);
    void setFechamodificacion(Date fechamodificacion);
    void setIdusuario(Long idusuario);
    void setListaUsuarioMiembro(List<IUserMember> listaUsuarioMiembro);
    void setNombre(String nombre);
    void setRol(String rol);
    void setAppRol(String appRol);    
    void setTipo(Short tipo);
}

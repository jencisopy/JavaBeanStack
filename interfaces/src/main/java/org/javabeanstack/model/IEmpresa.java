package org.javabeanstack.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlTransient;
import org.javabeanstack.data.IDataRow;

//TODO Cambiar el nombre de esta clase
/**
 *
 * @author Jorge Enciso
 * 
 */
public interface IEmpresa extends IDataRow, Serializable {
    String getAppuser();
    String getDatos();
    String getDireccion();

    @XmlTransient
    List<IEmpresa> getEmpresaList();
    String getEmpresarubro();
    Date getFechacreacion();
    Date getFechamodificacion();
    Date getFechareplicacion();
    String getFilesystem();
    String getFirma();
    Long getIdempresa();
    Long getIdempresagrupo();
    Long getIdempresamask();
    Long getIdperiodo();
    String getLogo();
    String getMenu();
    String getMotordatos();
    String getNombre();
    String getPais();
    String getRazonsocial();
    String getRuc();
    String getTelefono();
    void setAppuser(String appuser);
    void setDatos(String datos);
    void setDireccion(String direccion);
    void setEmpresaList(List<IEmpresa> empresaList);
    void setEmpresarubro(String empresarubro);
    void setFechacreacion(Date fechacreacion);
    void setFechamodificacion(Date fechamodificacion);
    void setFechareplicacion(Date fechareplicacion);
    void setFilesystem(String filesystem);
    void setFirma(String firma);
    void setIdempresa(Long idempresa);
    void setIdempresagrupo(Long idempresagrupo);
    void setIdempresamask(Long idempresamask);
    void setIdperiodo(Long idperiodo);
    void setLogo(String logo);
    void setMenu(String menu);
    void setMotordatos(String motordatos);
    void setNombre(String nombre);
    void setPais(String pais);
    void setRazonsocial(String razonsocial);
    void setRuc(String ruc);
    void setTelefono(String telefono);
}

package org.javabeanstack.model.tables;

import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity; 
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.javabeanstack.data.DataRow;
import org.javabeanstack.model.IAppCompanyAllowed;

@Entity
@Table(name = "dic_permisoempresa")
public class AppCompanyAllowed extends DataRow implements IAppCompanyAllowed {
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "idusuario")
    private Long iduser;

    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "idempresa")
    private Long idcompany;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "permitir")
    private boolean allow;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "negar")
    private boolean deny;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "fechacreacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechacreacion;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "fechamodificacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechamodificacion;
    
    @Size(max = 32)
    @Column(name = "appuser")
    private String appuser;
    

    public AppCompanyAllowed() {
    }

    @Override
    public Long getIduser() {
        return iduser;
    }

    @Override
    public void setIduser(Long idusuario) {
        this.iduser = idusuario;
    }

    @Override
    public Long getIdcompany() {
        return idcompany;
    }

    @Override
    public void setIdcompany(Long idempresa) {
        this.idcompany = idempresa;
    }


    @Override
    public boolean getAllow() {
        return allow;
    }

    @Override
    public void setAllow(boolean permitir) {
        this.allow = permitir;
    }

    @Override
    public boolean getDeny() {
        return deny;
    }

    @Override
    public void setDeny(boolean negar) {
        this.deny = negar;
    }

    public Date getFechacreacion() {
        return fechacreacion;
    }

    public void setFechacreacion(Date fechacreacion) {
        this.fechacreacion = fechacreacion;
    }

    public Date getFechamodificacion() {
        return fechamodificacion;
    }

    public void setFechamodificacion(Date fechamodificacion) {
        this.fechamodificacion = fechamodificacion;
    }

    @Override
    public String getAppuser() {
        return appuser;
    }

    @Override
    public void setAppuser(String appuser) {
        this.appuser = appuser;
    }
    
    /**
     * Si se aplica o no el filtro por defecto en la selección de datos.
     * Este metodo se modifica en las clases derivadas si se debe cambiar el 
     * comportamiento.
     * 
     * @return verdadero si y falso no
     */
    @Override
    public boolean isApplyDBFilter() {
        return false;
    }
}

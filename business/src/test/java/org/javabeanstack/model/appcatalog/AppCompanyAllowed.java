package org.javabeanstack.model.appcatalog;

import java.time.LocalDateTime;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity; 
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.javabeanstack.data.DataRow;
import org.javabeanstack.model.IAppCompanyAllowed;
import org.javabeanstack.util.LocalDateTimeAdapter;

@Entity
@Table(name = "appcompanyallowed")
public class AppCompanyAllowed extends DataRow implements IAppCompanyAllowed {
    @Id
    @Basic(optional = false)
    @Column(name = "iduser")
    private Long iduser;

    @Id
    @Basic(optional = false)
    @Column(name = "idcompany")
    private Long idcompany;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "allow")
    private boolean allow;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "denied")
    private boolean deny;
    
    
    @Basic(optional = false)
    @Column(name = "fechamodificacion")
    @XmlJavaTypeAdapter(type=LocalDateTime.class,  value=LocalDateTimeAdapter.class)                
    private LocalDateTime fechamodificacion;
    
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
    public void setIduser(Long iduser) {
        this.iduser = iduser;
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

    public LocalDateTime getFechamodificacion() {
        return fechamodificacion;
    }

    public void setFechamodificacion(LocalDateTime fechamodificacion) {
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
    
    @PreUpdate
    @PrePersist
    public void preUpdate() {
        fechamodificacion = LocalDateTime.now();
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

    @Override
    public boolean equivalent(Object o) {
        return equals(o);
    }
    
    @Override
    public boolean isRowChecked() {
        return true;
    }    
 }

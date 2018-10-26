package org.javabeanstack.model.tables;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.javabeanstack.data.DataRow;

@Cacheable()
@Entity
@Table(name = "region", 
        uniqueConstraints = { @UniqueConstraint(columnNames = 
                                               { "idempresa", "codigo"}) })
@SequenceGenerator(name = "SOME_SEQUENCE", allocationSize = 1, sequenceName = "SOME_SEQUENCE")
public class Region extends DataRow implements Serializable {
    private static final Long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SOME_SEQUENCE")
    @Basic(optional = false)
    @Column(name = "idregion", unique=true)
    private Long idregion;
        
    @Basic(optional = false)
    @NotNull
    @Column(name = "codigo")
    private String codigo;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "nombre")
    private String nombre;
    
    @Column(name = "fechamodificacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechamodificacion;
    
    @Size(max = 32)
    @Column(name = "appuser")
    private String appuser;
    
    @NotNull
    @Column(name = "idempresa")    
    private Long idempresa;

    public Region() {
    }

    public Long getIdregion() {
        return idregion;
    }

    public void setIdregion(Long idregion) {
        this.idregion = idregion;
    }

    public String getCodigo() {
        if(codigo != null) {
            codigo = codigo.trim();
        }
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        if(nombre != null) {
            nombre = nombre.trim();
        }
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre.trim();
    }

    public Date getFechamodificacion() {
        return fechamodificacion;
    }

    public void setFechamodificacion(Date fechamodificacion) {
        this.fechamodificacion = fechamodificacion;
    }

    public String getAppuser() {
        return appuser;
    }

    public void setAppuser(String appuser) {
        this.appuser = appuser;
    }

    public Long getIdempresa() {
        return idempresa;
    }

    public void setIdempresa(Long idempresa) {
        this.idempresa = idempresa;
    }

    
    @PrePersist
    public void prePersist() {
        fechamodificacion = new Date();
    }
    
    @PreUpdate
    public void preUpdate() {
        fechamodificacion = new Date();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + Objects.hashCode(this.idregion);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Region other = (Region) obj;
        return Objects.equals(this.idregion, other.idregion);
    }

    @Override
    public boolean equivalent(Object o) {
        if (!(o instanceof Region)) {
            return false;
        }
        Region obj = (Region) o;
        return (this.codigo.trim().equals(obj.getCodigo().trim()) && 
                Objects.equals(this.idempresa, obj.getIdempresa()));
    }
    
    @Override
    public String toString() {
        return "Region{" + "idregion=" + idregion + ", codigo=" + codigo + ", nombre=" + nombre + '}';
    }

}

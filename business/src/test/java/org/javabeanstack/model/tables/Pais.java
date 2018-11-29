package org.javabeanstack.model.tables;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.javabeanstack.data.DataRow;

@Entity
@Table(name = "pais", 
        uniqueConstraints = { @UniqueConstraint(columnNames = 
                                               { "idempresa", "codigo"}) })
public class Pais extends DataRow implements Serializable {

    private static final Long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "idpais")
    private Long idpais;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 4)
    @Column(name = "codigo")
    private String codigo;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 30)
    @Column(name = "nombre")
    private String nombre;
    
    @Column(name = "noedit")
    private Boolean noedit;
    
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Column(name = "latitud")
    private BigDecimal latitud;
    
    @Column(name = "Longitud")
    private BigDecimal Longitud;
    
    @Column(name = "fechamodificacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechamodificacion;
    
    @Size(max = 32)
    @Column(name = "appuser")
    private String appuser;
    
    @JoinColumn(name = "idregion", referencedColumnName = "idregion")
    @ManyToOne(optional = true)
    private Region region = new Region();
    
    @JoinColumn(name = "idmoneda", referencedColumnName = "idmoneda")
    @ManyToOne
    private Moneda moneda;
    /*
    @OneToMany
    private List<Provincia> listadoProvincia = new ArrayList<>();
    
    @OneToMany(mappedBy = "pais")
    private List<Persona> listadoPersona = new ArrayList<>();
    */
    //@JoinColumn(name = "idempresa", referencedColumnName = "idempresa")
    
    @Column(name = "idempresa")    
    private Long idempresa;

    public Pais() {
    }

    public Long getIdpais() {
        return idpais;
    }

    public void setIdpais(Long idpais) {
        this.idpais = idpais;
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
        this.nombre = nombre;
    }

    public Boolean isNoedit() {
        return noedit;
    }

    public void setNoedit(Boolean noedit) {
        this.noedit = noedit;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public void setLatitud(BigDecimal latitud) {
        this.latitud = latitud;
    }

    public BigDecimal getLongitud() {
        return Longitud;
    }

    public void setLongitud(BigDecimal Longitud) {
        this.Longitud = Longitud;
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

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public Moneda getMoneda() {
        return moneda;
    }

    public void setMoneda(Moneda moneda) {
        this.moneda = moneda;
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
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.idpais);
        return hash;
    }

    public Long getIdempresa() {
        return idempresa;
    }

    public void setIdempresa(Long idempresa) {
        this.idempresa = idempresa;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Pais other = (Pais) obj;
        if (!Objects.equals(this.idpais, other.idpais)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equivalent(Object o) {
        if (!(o instanceof Pais)) {
            return false;
        }
        Pais obj = (Pais) o;
        return (this.codigo.trim().equals(obj.getCodigo().trim()) && 
                Objects.equals(this.idempresa, obj.getIdempresa()));
    }
    
    @Override
    public String toString() {
        return "Pais{" + "idpais=" + idpais + ", codigo=" + codigo + ", nombre=" + nombre + ", noedit=" + noedit + ", latitud=" + latitud + ", Longitud=" + Longitud + '}';
    }
}

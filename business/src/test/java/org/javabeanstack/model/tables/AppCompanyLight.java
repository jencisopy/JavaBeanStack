package org.javabeanstack.model.tables;

import java.util.Date;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.javabeanstack.data.DataRow;
import org.javabeanstack.model.IAppCompany;

/**
 *
 * @author Jorge Enciso
 */
@Entity
@Table(name = "empresa")
@XmlRootElement
public class AppCompanyLight extends DataRow implements IAppCompany {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "idempresa")
    private Long idcompany;
    @Column(name = "idempresamask")
    private Long idcompanymask;
    @Column(name = "idperiodo")
    private Long idperiod;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "nombre")
    private String name;
    @Size(max = 50)
    @Column(name = "razonsocial")
    private String socialName;
    @Size(max = 50)
    @Column(name = "direccion")
    private String address;
    @Size(max = 50)
    @Column(name = "telefono")
    private String telephoneNumber;
    @Size(max = 11)
    @Column(name = "ruc")
    private String taxId;
    @Size(max = 50)
    @Column(name = "datos")
    private String persistentUnit;
    @Size(max = 50)
    @Column(name = "menu")
    private String menu;
    @Size(max = 50)
    @Column(name = "filesystem")
    private String filesystem;

    @Size(max = 50)
    @Column(name = "motordatos")
    private String dbengine;
    @Size(max = 4)
    @Column(name = "pais")
    private String country;
    @Size(max = 10)
    @Column(name = "empresarubro")
    private String companyActivity;

    @Column(name = "fechacreacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechacreacion;
    @Column(name = "fechamodificacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechamodificacion;
    @Column(name = "fechareplicacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechareplicacion;
    @Size(max = 32)
    @Column(name = "firma")
    private String firma;
    @Size(max = 32)
    @Column(name = "appuser")
    private String appuser;

    @OneToMany(mappedBy = "idcompanygroup")
    private List<AppCompanyLight> empresaList;

    @Column(name = "idempresagrupo")
    private Long idcompanygroup;

    public AppCompanyLight() {
    }

    public AppCompanyLight(Long idempresa) {
        this.idcompany = idempresa;
    }

    public AppCompanyLight(Long idempresa, String nombre) {
        this.idcompany = idempresa;
        this.name = nombre;
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
    public Long getIdcompanymask() {
        return idcompanymask;
    }

    @Override
    public void setIdcompanymask(Long idempresamask) {
        this.idcompanymask = idempresamask;
    }

    @Override
    public Long getIdperiod() {
        return idperiod;
    }

    @Override
    public void setIdperiod(Long idperiodo) {
        this.idperiod = idperiodo;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String nombre) {
        this.name = nombre;
    }

    @Override
    public String getSocialName() {
        return socialName;
    }

    @Override
    public void setSocialName(String razonsocial) {
        this.socialName = razonsocial;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public void setAddress(String direccion) {
        this.address = direccion;
    }

    @Override
    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    @Override
    public void setTelephoneNumber(String telefono) {
        this.telephoneNumber = telefono;
    }

    @Override
    public String getTaxId() {
        return taxId;
    }

    @Override
    public void setTaxId(String ruc) {
        this.taxId = ruc;
    }

    @Override
    public String getPersistentUnit() {
        return persistentUnit;
    }

    @Override
    public void setPersistentUnit(String datos) {
        this.persistentUnit = datos;
    }

    @Override
    public String getMenu() {
        return menu;
    }

    @Override
    public void setMenu(String menu) {
        this.menu = menu;
    }

    @Override
    public String getFilesystem() {
        return filesystem;
    }

    @Override
    public void setFilesystem(String filesystem) {
        this.filesystem = filesystem;
    }


    @Override
    public String getDbengine() {
        return dbengine;
    }

    @Override
    public void setDbengine(String motordatos) {
        this.dbengine = motordatos;
    }

    @Override
    public String getCountry() {
        return country;
    }

    @Override
    public void setCountry(String pais) {
        this.country = pais;
    }

    @Override
    public String getCompanyActivity() {
        return companyActivity;
    }

    @Override
    public void setCompanyActivity(String empresarubro) {
        this.companyActivity = empresarubro;
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

    public Date getFechareplicacion() {
        return fechareplicacion;
    }

    public void setFechareplicacion(Date fechareplicacion) {
        this.fechareplicacion = fechareplicacion;
    }

    public String getFirma() {
        return firma;
    }

    public void setFirma(String firma) {
        this.firma = firma;
    }

    @Override
    public String getAppuser() {
        return appuser;
    }

    @Override
    public void setAppuser(String appuser) {
        this.appuser = appuser;
    }

    
    @Override
    public byte[] getLogo() {
        return null;
    }

    @Override
    public void setLogo(byte[] logo) {
    }
    
    @XmlTransient
    @Override
    public List<IAppCompany> getCompanyList() {
        return (List<IAppCompany>) (List<?>) empresaList;
    }

    @Override
    public void setCompanyList(List<IAppCompany> empresaList) {
        this.empresaList = (List<AppCompanyLight>) (List<?>) empresaList;
    }

    @Override
    public Long getIdcompanygroup() {
        return idcompanygroup;
    }

    @Override
    public void setIdcompanygroup(Long idempresagrupo) {
        this.idcompanygroup = idempresagrupo;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idcompany != null ? idcompany.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof AppCompanyLight)) {
            return false;
        }
        AppCompanyLight other = (AppCompanyLight) object;
        if ((this.idcompany == null && other.idcompany != null) || (this.idcompany != null && !this.idcompany.equals(other.idcompany))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "py.com.oym.model.Empresa[ idempresa=" + idcompany + " ]";
    }

    @Override
    public boolean equivalent(Object o) {
        if (!(o instanceof AppCompanyLight)) {
            return false;
        }
        AppCompanyLight obj = (AppCompanyLight) o;
        return (this.idcompany.equals(obj.getIdcompany()));
    }

}

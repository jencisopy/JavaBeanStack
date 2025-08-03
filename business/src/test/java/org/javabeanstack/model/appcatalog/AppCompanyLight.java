package org.javabeanstack.model.appcatalog;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.javabeanstack.data.DataRow;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.util.LocalDateTimeAdapter;


/**
 *
 * @author Jorge Enciso
 */
@Entity
@Table(name = "appcompany") 
@XmlRootElement
public class AppCompanyLight extends DataRow implements IAppCompany {
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "idcompany")
    private Long idcompany;

    @Column(name = "idcompanymask")
    private Long idcompanymask;
    
    @Column(name = "idperiod")
    private Long idperiod;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "name")
    private String name;
    
    @Size(max = 50)
    @Column(name = "socialname")
    private String socialName;
    
    @Size(max = 50)
    @Column(name = "address")
    private String address;
    
    @Size(max = 50)
    @Column(name = "telephonenumber")
    private String telephoneNumber;
    
    @Size(max = 11)
    @Column(name = "taxid")
    private String taxId;
    
    @Size(max = 50)
    @Column(name = "persistentunit")
    private String persistentUnit;
    
    @Size(max = 50)
    @Column(name = "menu")
    private String menu;
    
    @Size(max = 50)
    @Column(name = "filesystem")
    private String filesystem;

    @Size(max = 50)
    @Column(name = "dbengine")
    private String dbengine;
    
    @Size(max = 4)
    @Column(name = "country")
    private String country;
    
    @Size(max = 10)
    @Column(name = "companyactivity")
    private String companyActivity;

    @Column(name = "fechacreacion",insertable = false, updatable = false)    
    @XmlJavaTypeAdapter(type=LocalDateTime.class,  value=LocalDateTimeAdapter.class)            
    private LocalDateTime fechacreacion;

    @Column(name = "fechamodificacion")
    @XmlJavaTypeAdapter(type=LocalDateTime.class,  value=LocalDateTimeAdapter.class)            
    private LocalDateTime fechamodificacion;

    @Column(name = "fechareplicacion")
    @XmlJavaTypeAdapter(type=LocalDateTime.class,  value=LocalDateTimeAdapter.class)            
    private LocalDateTime fechareplicacion;
    
    @Size(max = 32)
    @Column(name = "firma")
    private String firma;
    
    @Size(max = 32)
    @Column(name = "appuser")
    private String appuser;

    @OneToMany(mappedBy = "idcompanygroup",fetch = FetchType.EAGER)
    private List<AppCompany> appcompanyList;

    @Column(name = "idcompanygroup")
    private Long idcompanygroup;

    public AppCompanyLight() {
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
        if (idperiod == null || idperiod == 0L){
            return 1L;
        }
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

    public LocalDateTime getFechacreacion() {
        return fechacreacion;
    }

    public void setFechacreacion(LocalDateTime fechacreacion) {
        this.fechacreacion = fechacreacion;
    }

    public LocalDateTime getFechamodificacion() {
        return fechamodificacion;
    }

    public void setFechamodificacion(LocalDateTime fechamodificacion) {
        this.fechamodificacion = fechamodificacion;
    }

    public LocalDateTime getFechareplicacion() {
        return fechareplicacion;
    }

    public void setFechareplicacion(LocalDateTime fechareplicacion) {
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
        return (List<IAppCompany>) (List<?>) appcompanyList;
    }

    @Override
    public void setCompanyList(List<IAppCompany> empresaList) {
        this.appcompanyList = (List<AppCompany>) (List<?>) empresaList;
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
    public boolean equivalent(Object o) {
        if (!(o instanceof AppCompanyLight)) {
            return false;
        }
        AppCompanyLight obj = (AppCompanyLight) o;
        return (this.idcompany.equals(obj.getIdcompany()));
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
    public String toString() {
        return "org.javabeanstack.model.appcatalog.AppCompany{ idempresa=" + idcompany + " }";        
    }
}

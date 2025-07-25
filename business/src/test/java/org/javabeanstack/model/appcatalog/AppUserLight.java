package org.javabeanstack.model.appcatalog;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.javabeanstack.data.DataRow;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.model.IAppCompanyAllowed;
import org.javabeanstack.model.IAppUser;
import org.javabeanstack.model.IAppUserMember;
import org.javabeanstack.util.Fn;

@Entity
@Table(name = "appuser")
public class AppUserLight extends DataRow implements IAppUser {
    private static final Logger LOGGER = LogManager.getLogger(AppUserLight.class);
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "iduser")
    private Long iduser;

    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 30)
    @Column(name = "code")
    private String code;

    @Basic(optional = false)
    @NotNull(message = "Debe ingresar el nombre de usuario")
    @Size(min = 1, max = 50)
    @Column(name = "fullName")
    private String fullName;

    @Column(name = "pass")
    private String pass;

    @Column(name = "pass", insertable = false, updatable = false)
    private String passBackup;
    
    @Transient
    private String passConfirm;

    @Transient
    private String passConfirm2;
    
    @Size(max = 50)
    @Column(name = "description")
    private String description;

    @Size(max = 100)
    @Column(name = "email1")
    private String email1;

    @Size(max = 100)
    @Column(name = "email2")
    private String email2;
    
    @Size(max = 50)
    @Column(name = "telefono1")
    private String telefono1;

    @Size(max = 50)
    @Column(name = "celular1")
    private String celular1;

    @Size(max = 50)
    @Column(name = "celular2")
    private String celular2;
    
    @Column(name = "disabled")
    private Boolean disabled = false;

    @Column(name = "expiredDate")
    private LocalDateTime expiredDate;

    @Size(max = 2)
    @Column(name = "rol")
    private String rol;

    @Column(name = "type")
    private Short type;

    @Column(name = "fechamodificacion")
    private LocalDateTime fechamodificacion;

    @OneToMany(mappedBy = "usermember")
    private List<AppUserMember> userMemberList = new ArrayList<>();

    @Column(name = "idcompany")
    private Long idcompany;

    public AppUserLight() {
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
    public String getLogin() {
        if (code != null) {
            code = code.trim();
        }
        return code;
    }

    @Override
    public void setLogin(String codigo) {
        this.code = codigo;
    }

    @Override
    public String getCode() {
        if (code != null) {
            code = code.trim();
        }
        return code;
    }

    @Override
    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String getFullName() {
        if (fullName != null) {
            fullName = fullName.trim();
        }
        return fullName;
    }

    @Override
    public void setFullName(String nombre) {
        this.fullName = nombre;
    }

    @Override
    public String getPass() {
        if (pass != null) {
            pass = pass.trim();
        }
        return pass;
    }

    @Override
    public void setPass(String clave) {
        this.pass = clave;
    }

    @Override
    public String getPassBackup() {
        if (passBackup != null) {
            passBackup = passBackup.trim();
        }
        return passBackup;
    }
    
    @Override
    public String getPassConfirm() {
        if (passConfirm != null) {
            passConfirm = passConfirm.trim();
        }
        return passConfirm;
    }

    @Override
    public void setPassConfirm(String passConfirm) {
        this.passConfirm = passConfirm;
    }

    @Override
    public String getPassConfirm2() {
        if (passConfirm2 != null) {
            passConfirm2 = passConfirm2.trim();
        }
        return passConfirm2;
    }

    @Override
    public void setPassConfirm2(String passConfirm2) {
        this.passConfirm = passConfirm2;
    }
    
    @Override
    public String getDescription() {
        if (description != null) {
            description = description.trim();
        }
        return description;
    }

    @Override
    public void setDescription(String descripcion) {
        this.description = descripcion;
    }

    @Override
    public Boolean getDisabled() {
        return disabled;
    }

    @Override
    public void setDisabled(Boolean disable) {
        this.disabled = disable;
    }

    @Override
    public LocalDateTime getExpiredDate() {
        return expiredDate;
    }

    @Override
    public void setExpiredDate(LocalDateTime expira) {
        this.expiredDate = expira;
    }

    @Override
    public String getRol() {
        return Fn.nvl(rol,"30").trim().toUpperCase();
    }

    @Override
    public String getHighRol() {
        // Este es el valor del usuario normal
        String result=USUARIO;    
        try{
            if (this.getUserMemberList() == null || this.getUserMemberList().isEmpty()){
                return Fn.nvl(rol,USUARIO).trim();
            }
            for (IAppUserMember userMember: this.getUserMemberList()){
                String role = Fn.nvl(userMember.getUserGroup().getRol(),USUARIO).trim();
                if (Integer.parseInt(role) < Integer.parseInt(result)){
                    result = role;
                }
            }
        }
        catch (Exception exp)    {
            ErrorManager.showError(exp, LOGGER);
            result = Fn.nvl(rol,USUARIO).trim();
        }
        return result.toUpperCase();
    }    
    
    @Override
    public String getAllRoles() {
        // Este es el valor del usuario normal
        String result = USUARIO;
        try {
            // Si es grupo
            if (this.getType() == 2) {
                result = Fn.nvl(rol, USUARIO).trim();
            } else {
                // Si es usuario
                if (this.getUserMemberList() == null || this.getUserMemberList().isEmpty()) {
                    return Fn.nvl(rol, USUARIO).trim();
                }
                String roles = "";
                for (IAppUserMember userMember : this.getUserMemberList()) {
                    roles += Fn.nvl(userMember.getUserGroup().getRol(), USUARIO).trim()+",";                                    
                }
                result = roles;                
            }
        } catch (Exception exp) {
            ErrorManager.showError(exp, LOGGER);
            result = Fn.nvl(rol, USUARIO).trim();
        }
        return result.toUpperCase();
    }
    
    @Override
    public void setRol(String rol) {
        this.rol = rol;
    }

    @Override
    public String getAppRol() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setAppRol(String appRol) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Short getType() {
        return type;
    }

    @Override
    public void setType(Short tipo) {
        this.type = tipo;
    }

    public LocalDateTime getFechamodificacion() {
        return fechamodificacion;
    }

    public void setFechamodificacion(LocalDateTime fechamodificacion) {
        this.fechamodificacion = fechamodificacion;
    }

    @Override
    public List<IAppUserMember> getUserMemberList() {
        return (List<IAppUserMember>) (List<?>) userMemberList;
    }

    @Override
    public void setUserMemberList(List<IAppUserMember> userMemberList) {
        this.userMemberList = (List<AppUserMember>) (List<?>) userMemberList;
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
    public List<IAppCompanyAllowed> getAppCompanyAllowedList() {
        return null;
    }

    @Override
    public void setAppCompanyAllowedList(List<IAppCompanyAllowed> dicPermisoEmpresaList) {
        //this.dicPermisoEmpresaList = (List<DicPermisoEmpresa>)(List<?>)dicPermisoEmpresaList;
    }

    @Override
    public byte[] getAvatar() {
        return null;
    }

    @Override
    public void setAvatar(byte[] avatar) {
    }


    public String getEmail1() {
        return email1;
    }

    public void setEmail1(String email1) {
        this.email1 = email1;
    }

    public String getEmail2() {
        return email2;
    }

    public void setEmail2(String email2) {
        this.email2 = email2;
    }

    public String getTelefono1() {
        return telefono1;
    }

    public void setTelefono1(String telefono1) {
        this.telefono1 = telefono1;
    }

    public String getCelular1() {
        return celular1;
    }

    public void setCelular1(String celular1) {
        this.celular1 = celular1;
    }

    public String getCelular2() {
        return celular2;
    }

    public void setCelular2(String celular2) {
        this.celular2 = celular2;
    }
    
    @Override
    public int hashCode() {
        int hash = 5;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AppUserLight other = (AppUserLight) obj;
        return Objects.equals(this.iduser, other.iduser);
    }

    @Override
    public boolean equivalent(Object o) {
        if (!(o instanceof AppUserLight)) {
            return false;
        }
        AppUserLight obj = (AppUserLight) o;
        return (this.code.trim().equals(obj.getLogin().trim()));
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
    public final boolean isSuperUser(){
        return getRol().contains(ANALISTA)
                || getRol().contains(SUPERUSER);
    }
    
    @Override
    public final boolean isSysAdmin(){
        return getAllRoles().contains(ADMINISTRADOR) 
                || getRol().contains(ANALISTA)
                || getRol().contains(SUPERUSER);
    }

    @Override
    public final boolean isCompanyAdmin(){
        return getAllRoles().contains(ADMINISTRADOR) 
                || getRol().contains(ANALISTA)
                || getRol().contains(SUPERUSER)
                || getAllRoles().contains(ADMINCOMPANY);
    }
    
    @Override
    public String toString() {
        return "org.javabeanstack.model.appcatalog.AppUserLight{" + "iduser=" + iduser + ", code=" + code + ", fullName=" + fullName + ", description=" + description + ", disabled=" + disabled + ", expiredDate=" + expiredDate + ", rol=" + rol + ", type=" + type + '}';
    }    
}

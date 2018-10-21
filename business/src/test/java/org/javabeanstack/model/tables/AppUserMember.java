package org.javabeanstack.model.tables;

import java.util.Objects; 
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.javabeanstack.data.DataRow;
import org.javabeanstack.model.IAppUser;
import org.javabeanstack.model.IAppUserMember;

@Entity
@Table(name = "usuariomiembro")
public class AppUserMember extends DataRow implements IAppUserMember {
    private static final long serialVersionUID = 0L;
     
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "idusuariomiembro") 
    private Long idusermember;
    
    @JoinColumn(name = "idmiembro", referencedColumnName = "idusuario", nullable = false)
    @ManyToOne(optional = false)
    private AppUser usermember;

    @JoinColumn(name = "idusuario", referencedColumnName = "idusuario", nullable = false)
    @ManyToOne(optional = false)
    private AppUser usergroup;
    
    public AppUserMember() {
    }

    @Override
    public Long getIdusermember() {
        return idusermember;
    }

    @Override
    public void setIdusermember(Long idusuariomiembro) {
        this.idusermember = idusuariomiembro;
    }

    @Override
    public IAppUser getUserMember() {
        return usermember;
    }

    @Override
    public void setUserMember(IAppUser usuarioMiembro) {
        this.usermember = (AppUser)usuarioMiembro;
    }

    @Override
    public IAppUser getUserGroup() {
        return usergroup;
    }

    @Override
    public void setUserGroup(IAppUser usuarioGrupo) {
        this.usergroup = (AppUser)usuarioGrupo;
    }


    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + Objects.hashCode(this.idusermember);
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
        final AppUserMember other = (AppUserMember) obj;
        return Objects.equals(this.idusermember, other.idusermember);
    }

    @Override
    public String toString() {
        return "UsuarioMiembro{" + "idusuariomiembro=" + idusermember + "}";
    }

    @Override
    public boolean equivalent(Object o) {
        if (!(o instanceof AppUserMember)) {
            return false;
        }
        AppUserMember obj = (AppUserMember) o;
        return (this.idusermember.equals(obj.getIdusermember())); 
    }
    
}

/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 3 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
* MA 02110-1301  USA
 */

package org.javabeanstack.model.appcatalog;

import java.util.Objects; 
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.javabeanstack.data.DataRow;
import org.javabeanstack.model.IAppUser;
import org.javabeanstack.model.IAppUserMember;

@Entity
@Table(name = "appusermember")
@SequenceGenerator(name = "APPUSERMEMBER_SEQ", allocationSize = 1, sequenceName = "APPUSERMEMBER_SEQ")
public class AppUserMember extends DataRow implements IAppUserMember {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "APPUSERMEMBER_SEQ")
    @Basic(optional = false)
    @Column(name = "idusermember")
    private Long idusermember;
    
    @JoinColumn(name = "idmember", referencedColumnName = "iduser", nullable = false)
    @ManyToOne(optional = false)
    private AppUser usermember;

    @JoinColumn(name = "idusergroup", referencedColumnName = "iduser", nullable = false)
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
    public boolean isRowChecked() {
        return true;
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

/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2018 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*        jenciso@javabeanstack.org
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

import java.time.LocalDateTime;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.javabeanstack.data.DataRow;
import org.javabeanstack.model.IAppAuthConsumer;
import org.javabeanstack.model.IAppAuthConsumerToken;

/**
 *
 * @author JORGE
 */
@Entity
@Table(name = "appauthconsumertoken")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "AppAuthConsumerToken.findAll", query = "SELECT a FROM AppAuthConsumerToken a")})
public class AppAuthConsumerToken extends DataRow implements IAppAuthConsumerToken{
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)    
    @Basic(optional = false)
    @Column(name = "idappauthconsumertoken")
    private Long idappauthconsumertoken;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "uuidDevice")
    private String uuidDevice;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 500)
    @Column(name = "token")
    private String token;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "tokenSecret")
    private String tokenSecret;
    @Lob
    @Size(max = 2147483647)
    @Column(name = "data")
    private String data;
    @Basic(optional = false)
    @NotNull
    @Column(name = "blocked")
    private boolean blocked;

    @Size(max = 100)
    @Column(name = "userName")
    private String userName;

    @Size(max = 100)
    @Column(name = "userEmail")
    private String userEmail;
    
    @Column(name = "lastUsed")
    private LocalDateTime lastUsed;

    
    @Transient
    @Basic(optional = false)
    @Column(name = "fechacreacion")
    private LocalDateTime fechacreacion;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "fechamodificacion")
    private LocalDateTime fechamodificacion;
    
    @Column(name = "fechareplicacion")
    private LocalDateTime fechareplicacion;
    @Size(max = 32)
    @Column(name = "appuser")
    private String appuser;

    @JoinColumn(name = "idappauthconsumer", referencedColumnName = "idappauthconsumer")
    @ManyToOne
    private AppAuthConsumer appAuthConsumer;

    public AppAuthConsumerToken() {
        this.idappauthconsumertoken = 0L;
    }

    public AppAuthConsumerToken(Long idappauthconsumertoken) {
        this.idappauthconsumertoken = idappauthconsumertoken;
    }


    public Long getIdappauthconsumertoken() {
        return idappauthconsumertoken;
    }

    public void setIdappauthconsumertoken(Long idappauthconsumertoken) {
        this.idappauthconsumertoken = idappauthconsumertoken;
    }

    @Override
    public String getUuidDevice() {
        return uuidDevice;
    }

    @Override
    public void setUuidDevice(String uuidDevice) {
        this.uuidDevice = uuidDevice;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String getTokenSecret() {
        return tokenSecret;
    }

    @Override
    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    @Override
    public String getData() {
        return data;
    }

    @Override
    public void setData(String data) {
        this.data = data;
    }

    @Override
    public Boolean getBlocked() {
        return blocked;
    }

    @Override
    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String getUserEmail() {
        return userEmail;
    }

    @Override
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    @Override
    public LocalDateTime getLastUsed() {
        return lastUsed;
    }

    @Override
    public void setLastUsed(LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
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

    public String getAppuser() {
        return appuser;
    }

    public void setAppuser(String appuser) {
        this.appuser = appuser;
    }

    @XmlElement(type = AppAuthConsumer.class)
    @Override
    public IAppAuthConsumer getAppAuthConsumerKey() {
        return appAuthConsumer;
    }

    @Override
    public void setAppAuthConsumerKey(IAppAuthConsumer appAuthConsumer) {
        this.appAuthConsumer = (AppAuthConsumer)appAuthConsumer;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idappauthconsumertoken != null ? idappauthconsumertoken.hashCode() : 0);
        return hash;
    }

    @PreUpdate
    @PrePersist
    public void preUpdate() {
        fechamodificacion = LocalDateTime.now();
    }    
        
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof AppAuthConsumerToken)) {
            return false;
        }
        AppAuthConsumerToken other = (AppAuthConsumerToken) object;
        if ((this.idappauthconsumertoken == null && other.idappauthconsumertoken != null) || (this.idappauthconsumertoken != null && !this.idappauthconsumertoken.equals(other.idappauthconsumertoken))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.javabeanstack.data.Appauthconsumertoken[ idappauthconsumertoken=" + idappauthconsumertoken + " ]";
    }
}

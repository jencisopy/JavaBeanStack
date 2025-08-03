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
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.hibernate.annotations.DynamicUpdate;
import org.javabeanstack.data.DataRow;
import org.javabeanstack.model.IAppAuthConsumer;
import org.javabeanstack.model.IAppAuthConsumerToken;
import org.javabeanstack.util.LocalDateTimeAdapter;

/**
 *
 * @author Jorge Enciso
 */
@Entity
@DynamicUpdate
@Table(name = "appauthconsumertoken")
@XmlRootElement
@SequenceGenerator(name = "APPAUTHCONSUMERTOKEN_SEQ", allocationSize = 1, sequenceName = "APPAUTHCONSUMERTOKEN_SEQ")
public class AppAuthConsumerToken extends DataRow implements IAppAuthConsumerToken {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "APPAUTHCONSUMERTOKEN_SEQ")
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

    @Column(name = "deleted")
    private Boolean deleted;
    
    @Size(max = 100)
    @Column(name = "userName")
    private String userName;

    @Size(max = 100)
    @Column(name = "userEmail")
    private String userEmail;

    @Column(name = "lastUsed")
    @XmlJavaTypeAdapter(type = LocalDateTime.class, value = LocalDateTimeAdapter.class)
    private LocalDateTime lastUsed;

    @Column(name = "fechacreacion", insertable = false, updatable = false)
    @XmlJavaTypeAdapter(type = LocalDateTime.class, value = LocalDateTimeAdapter.class)
    private LocalDateTime fechacreacion;

    @NotNull
    @Column(name = "fechamodificacion")
    @XmlJavaTypeAdapter(type = LocalDateTime.class, value = LocalDateTimeAdapter.class)
    private LocalDateTime fechamodificacion;

    @XmlJavaTypeAdapter(type = LocalDateTime.class, value = LocalDateTimeAdapter.class)
    @Column(name = "fechareplicacion")
    private LocalDateTime fechareplicacion;

    @Size(max = 32)
    @Column(name = "appuser")
    private String appuser;

    @JoinColumn(name = "idappauthconsumer", referencedColumnName = "idappauthconsumer", nullable = true)
    @ManyToOne
    private AppAuthConsumer appAuthConsumer;

    @Size(max = 100)
    @Transient
    private String consumerKey = getConsumerKey();

    @Size(max = 100)
    @Transient
    private String consumerName = getConsumerName();
    
    @Transient
    private LocalDateTime expiredDate = getExpiredDate();
    
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
    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }

    @Override
    public Boolean getDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
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

    @Override
    public String getConsumerKey() {
        if (consumerKey != null) {
            return consumerKey;
        }
        if (appAuthConsumer == null) {
            return null;
        }
        return appAuthConsumer.getConsumerKey();
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    @Override
    public String getConsumerName() {
        if (consumerName != null) {
            return consumerName;
        }
        if (appAuthConsumer == null) {
            return null;
        }
        return appAuthConsumer.getConsumerName();
    }

    public void setConsumerName(String consumerName) {
        this.consumerName = consumerName;
    }
    
    @Override
    public LocalDateTime getExpiredDate() {
        if (expiredDate != null) {
            return expiredDate;
        }
        if (appAuthConsumer == null) {
            return null;
        }
        return appAuthConsumer.getExpiredDate();
    }

    public void setExpiredDate(LocalDateTime expiredDate) {
        this.expiredDate = expiredDate;
    }
    
    
    @XmlElement(type = AppAuthConsumer.class)
    @Override
    public IAppAuthConsumer getAppAuthConsumerKey() {
        return appAuthConsumer;
    }

    @Override
    public void setAppAuthConsumerKey(IAppAuthConsumer appAuthConsumer) {
        this.appAuthConsumer = (AppAuthConsumer) appAuthConsumer;
        if (appAuthConsumer != null) {
            consumerKey = appAuthConsumer.getConsumerKey();
        }
    }

    @PreUpdate
    @PrePersist
    public void preUpdate() {
        fechamodificacion = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "org.javabeanstack.model.appcatalog.Appauthconsumertoken{ idappauthconsumertoken=" + idappauthconsumertoken + " }";
    }
}

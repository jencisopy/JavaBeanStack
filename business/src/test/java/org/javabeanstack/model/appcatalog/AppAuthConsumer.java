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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.javabeanstack.model.IAppAuthConsumer;
import org.javabeanstack.data.DataRow;

/**
 *
 * @author JORGE
 */
@Entity
@Table(name = "appauthconsumer",uniqueConstraints = { @UniqueConstraint(columnNames = {"consumerKey"})})
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "AppAuthConsumer.findAll", query = "SELECT a FROM AppAuthConsumer a")})
public class AppAuthConsumer extends DataRow implements IAppAuthConsumer {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "idappauthconsumer")
    private Long idappauthconsumer;
    @Column(name = "idempresa")
    private Long idempresa;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "consumerKey")
    private String consumerKey;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "consumerName")
    private String consumerName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "expiredDate")
    private LocalDateTime expiredDate;
    @Lob
    @Size(max = 2147483647)
    @Column(name = "publicKey")
    private String publicKey;
    @Lob
    @Size(max = 2147483647)
    @Column(name = "privateKey")
    private String privateKey;
    @Size(max = 50)
    @Column(name = "signatureAlgorithm")
    private String signatureAlgorithm;
    @Size(max = 50)
    @Column(name = "cryptoAlgorithm")
    private String cryptoAlgorithm;
    @Basic(optional = false)
    @Column(name = "blocked")
    private Boolean blocked;
    @Size(max = 250)
    @Column(name = "authURL")
    private String authURL;
    @Size(max = 250)
    @Column(name = "tokenURL")
    private String tokenURL;
    @Size(max = 250)
    @Column(name = "callbackURL")
    private String callbackURL;
    @Size(max = 200)
    @Column(name = "scope")
    private String scope;
    
    @Transient
    @Basic(optional = false)
    @Column(name = "fechacreacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechacreacion;
    
    @Basic(optional = false)
    @Column(name = "fechamodificacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechamodificacion;

    @Column(name = "fechareplicacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechareplicacion;

    @Size(max = 32)
    @Column(name = "appuser")
    private String appuser;
    
    @OneToMany(mappedBy = "appAuthConsumer")
    private List<AppAuthConsumerToken> appauthconsumertokenList = new ArrayList<>();

    public AppAuthConsumer() {
    }

    public AppAuthConsumer(Long idappauthconsumer) {
        this.idappauthconsumer = idappauthconsumer;
    }

    public AppAuthConsumer(Long idappauthconsumer, String consumerKey, String consumerName, LocalDateTime expiredDate, Boolean blocked, Date fechacreacion, Date fechamodificacion) {
        this.idappauthconsumer = idappauthconsumer;
        this.consumerKey = consumerKey;
        this.consumerName = consumerName;
        this.expiredDate = expiredDate;
        this.blocked = blocked;
        this.fechacreacion = fechacreacion;
        this.fechamodificacion = fechamodificacion;
    }

    public Long getIdappauthconsumer() {
        return idappauthconsumer;
    }

    public void setIdappauthconsumer(Long idappauthconsumer) {
        this.idappauthconsumer = idappauthconsumer;
    }

    public Long getIdempresa() {
        return idempresa;
    }

    public void setIdempresa(Long idempresa) {
        this.idempresa = idempresa;
    }


    @Override
    public String getConsumerKey() {
        return consumerKey;
    }

    @Override
    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    @Override
    public String getConsumerName() {
        return consumerName;
    }

    @Override
    public void setConsumerName(String consumerName) {
        this.consumerName = consumerName;
    }

    @Override
    public LocalDateTime getExpiredDate() {
        return expiredDate;
    }

    @Override
    public void setExpiredDate(LocalDateTime expiredDate) {
        this.expiredDate = expiredDate;
    }

    @Override
    public String getPublicKey() {
        return publicKey;
    }

    @Override
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public String getPrivateKey() {
        return privateKey;
    }

    @Override
    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    @Override
    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    @Override
    public String getCryptoAlgorithm() {
        return cryptoAlgorithm;
    }

    @Override
    public void setCryptoAlgorithm(String cryptoAlgorithm) {
        this.cryptoAlgorithm = cryptoAlgorithm;
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
    public String getAuthURL() {
        return authURL;
    }

    @Override
    public void setAuthURL(String authURL) {
        this.authURL = authURL;
    }

    @Override
    public String getTokenURL() {
        return tokenURL;
    }

    @Override
    public void setTokenURL(String tokenURL) {
        this.tokenURL = tokenURL;
    }

    @Override
    public String getCallbackURL() {
        return callbackURL;
    }

    @Override
    public void setCallbackURL(String callbackURL) {
        this.callbackURL = callbackURL;
    }

    @Override
    public String getScope() {
        return scope;
    }

    @Override
    public void setScope(String scope) {
        this.scope = scope;
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

    public String getAppuser() {
        return appuser;
    }

    public void setAppuser(String appuser) {
        this.appuser = appuser;
    }

    @XmlTransient
    public List<AppAuthConsumerToken> getAppauthconsumertokenList() {
        return appauthconsumertokenList;
    }

    public void setAppauthconsumertokenList(List<AppAuthConsumerToken> appauthconsumertokenList) {
        this.appauthconsumertokenList = appauthconsumertokenList;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idappauthconsumer != null ? idappauthconsumer.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equivalent(Object o) {
        if (!(o instanceof AppAuthConsumer)) {
            return false;
        }
        AppAuthConsumer obj = (AppAuthConsumer) o;
        return (this.consumerKey.trim().equals(obj.consumerKey.trim()));
    }
    
    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof AppAuthConsumer)) {
            return false;
        }
        AppAuthConsumer other = (AppAuthConsumer) object;
        if ((this.idappauthconsumer == null && other.idappauthconsumer != null) || (this.idappauthconsumer != null && !this.idappauthconsumer.equals(other.idappauthconsumer))) {
            return false;
        }
        return true;
    }

    @PreUpdate
    @PrePersist
    public void preUpdate() {
        fechamodificacion = new Date();
    }    
    
    @Override
    public String toString() {
        return "org.javabeanstack.data.Appauthconsumer[ idappauthconsumer=" + idappauthconsumer + " ]";
    }
    
    
}

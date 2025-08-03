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
import java.util.List;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import org.javabeanstack.model.IAppAuthConsumer;
import org.javabeanstack.data.DataRow;

/**
 *
 * @author Jorge Enciso
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
    private LocalDateTime fechacreacion;
    
    @Basic(optional = false)
    @Column(name = "fechamodificacion")
    private LocalDateTime fechamodificacion;

    @Column(name = "fechareplicacion")
    private LocalDateTime fechareplicacion;

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
        fechamodificacion = LocalDateTime.now();
    }    
    
    @Override
    public String toString() {
        return "org.javabeanstack.data.Appauthconsumer[ idappauthconsumer=" + idappauthconsumer + " ]";
    }
    
    
}

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

import java.util.Date;
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
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
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
    @NamedQuery(name = "Appauthconsumertoken.findAll", query = "SELECT a FROM Appauthconsumertoken a")})
public class Appauthconsumertoken extends DataRow implements IAppAuthConsumerToken{
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)    
    @Basic(optional = false)
    @NotNull
    @Column(name = "idappauthconsumertoken")
    private Long idappauthconsumertoken;
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
    
    @Transient
    @Basic(optional = false)
    @NotNull
    @Column(name = "fechacreacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechacreacion;
    
    @Transient
    @Basic(optional = false)
    @NotNull
    @Column(name = "fechamodificacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechamodificacion;
    @Column(name = "fechareplicacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechareplicacion;
    @Size(max = 32)
    @Column(name = "appuser")
    private String appuser;

    @JoinColumn(name = "idappauthconsumer", referencedColumnName = "idappauthconsumer")
    @ManyToOne
    private Appauthconsumer appAuthConsumer;

    public Appauthconsumertoken() {
    }

    public Appauthconsumertoken(Long idappauthconsumertoken) {
        this.idappauthconsumertoken = idappauthconsumertoken;
    }

    public Appauthconsumertoken(Long idappauthconsumertoken, String token, String tokenSecret, boolean blocked, Date fechacreacion, Date fechamodificacion) {
        this.idappauthconsumertoken = idappauthconsumertoken;
        this.token = token;
        this.tokenSecret = tokenSecret;
        this.blocked = blocked;
        this.fechacreacion = fechacreacion;
        this.fechamodificacion = fechamodificacion;
    }

    public Long getIdappauthconsumertoken() {
        return idappauthconsumertoken;
    }

    public void setIdappauthconsumertoken(Long idappauthconsumertoken) {
        this.idappauthconsumertoken = idappauthconsumertoken;
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

    @Override
    public IAppAuthConsumer getAppAuthConsumer() {
        return appAuthConsumer;
    }

    @Override
    public void setAppAuthConsumer(IAppAuthConsumer appAuthConsumer) {
        this.appAuthConsumer = (Appauthconsumer)appAuthConsumer;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idappauthconsumertoken != null ? idappauthconsumertoken.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Appauthconsumertoken)) {
            return false;
        }
        Appauthconsumertoken other = (Appauthconsumertoken) object;
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

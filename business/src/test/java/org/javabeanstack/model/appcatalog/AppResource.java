/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 Jorge Enciso
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

import java.math.BigInteger;
import java.time.LocalDateTime;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

import org.javabeanstack.data.DataRow;
import org.javabeanstack.model.IAppResource;
import static org.javabeanstack.util.Strings.isNullorEmpty;

/**
 *
 * @author Jorge Enciso
 */
@Entity
@Table(name = "appresource",uniqueConstraints = { @UniqueConstraint(columnNames = {"code"})})
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "AppResource.findAll", query = "SELECT a FROM AppResource a")})
public class AppResource extends DataRow implements IAppResource {
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)        
    @Basic(optional = false)
    @NotNull
    @Column(name = "idappresource")
    private Long idappresource;
    
    @Column(name = "idparent")
    private Long idparent;

    @Column(name = "idappobject")
    private Long idappobject;

    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "code")
    private String code;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "name")
    private String name;
    
    @Size(max = 200)
    @Column(name = "url")
    private String url;
    
    @Size(max = 20)
    @Column(name = "type")
    private String type;
    
    @Size(max = 30)
    @Column(name = "charset")
    private String charset;
    
    @Basic(optional = false)
    @NotNull
    @Lob
    @Size(min = 1, max = 2147483647)
    @Column(name = "source")
    private String source;
    
    @Basic(optional = false)
    @NotNull
    @Lob
    @Size(min = 1, max = 2147483647)
    @Column(name = "compiled")
    private String compiled;
    
    @Column(name = "processtime")
    private LocalDateTime processtime;

    @Column(name = "referencetime")
    private BigInteger referencetime;
    
    @Column(name = "lastreference")
    private LocalDateTime lastreference;

    @Size(max = 32)
    @Column(name = "checksum")
    private String checksum;
    
    
    @Transient
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

    @Column(name = "bdata")
    private byte[] bdata;
    
    public AppResource() {
    }

    public AppResource(Long idappresource) {
        this.idappresource = idappresource;
    }

    @Override
    public Long getIdappresource() {
        return idappresource;
    }

    @Override
    public void setIdappresource(Long idappresource) {
        this.idappresource = idappresource;
    }

    @Override
    public Long getIdparent() {
        return idparent;
    }

    @Override
    public void setIdparent(Long idparent) {
        this.idparent = idparent;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getCharset() {
        return charset;
    }

    @Override
    public void setCharset(String charset) {
        this.charset = charset;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String getCompiled() {
        return compiled;
    }

    @Override
    public void setCompiled(String compiled) {
        this.compiled = compiled;
    }

    @Override
    public LocalDateTime getProcesstime() {
        return processtime;
    }

    @Override
    public void setProcesstime(LocalDateTime processtime) {
        this.processtime = processtime;
    }

    @Override
    public BigInteger getReferencetime() {
        return referencetime;
    }

    @Override
    public void setReferencetime(BigInteger referencetime) {
        this.referencetime = referencetime;
    }

    @Override
    public LocalDateTime getLastreference() {
        return lastreference;
    }

    @Override
    public void setLastreference(LocalDateTime lastreference) {
        this.lastreference = lastreference;
    }

    
    public LocalDateTime getFechacreacion() {
        return fechacreacion;
    }

    public LocalDateTime getFechamodificacion() {
        return fechamodificacion;
    }


    public LocalDateTime getFechareplicacion() {
        return fechareplicacion;
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
    public boolean isValid() {
        if (isNullorEmpty(compiled) || processtime == null){
            return false;
        }
        return processtime.isAfter(fechamodificacion);
    }

    public byte[] getBdata() {
        return bdata;
    }

    public void setBdata(byte[] bdata) {
        this.bdata = bdata;
    }


    
    @Override
    public String toString() {
        return "net.makerapp.model.tables.Appresource[ idappresource=" + idappresource + " ]";
    }

    @Override
    public Long getIdAppObject() {
        return idappobject;
    }

    @Override
    public void setIdAppObject(Long idappobject) {
        this.idappobject = idappobject;
    }

    @Override
    public String getChecksum() {
        return checksum;
    }

    @Override
    public void setChecksum(String checkSum) {
        this.checksum = checkSum;
    }
}

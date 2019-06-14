/*
* Copyright (c) 2015-2017 OyM System Group S.A.
* Capitan Cristaldo 464, Asunción, Paraguay
* All rights reserved. 
*
* NOTICE:  All information contained herein is, and remains
* the property of OyM System Group S.A. and its suppliers,
* if any.  The intellectual and technical concepts contained
* herein are proprietary to OyM System Group S.A.
* and its suppliers and protected by trade secret or copyright law.
* Dissemination of this information or reproduction of this material
* is strictly forbidden unless prior written permission is obtained
* from OyM System Group S.A.
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
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

import org.javabeanstack.data.DataRow;
import org.javabeanstack.model.IAppXmlSource;
import static org.javabeanstack.util.Strings.isNullorEmpty;


/**
 *
 * @author Jorge Enciso
 */
@Entity
@Table(name = "appxmlsource")
@XmlRootElement
public class AppXmlSource extends DataRow implements IAppXmlSource {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)        
    @Basic(optional = false)
    @NotNull
    @Column(name = "idxmlsource")
    private Long idxmlSource;

    @Column(name = "idobjeto")
    private Long idObjeto;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "xmlname")
    private String xmlName;

    @Size(max = 200)
    @Column(name = "xmlpath")
    private String xmlPath;
    
    @Basic(optional = false)
    @NotNull
    @Lob
    @Size(min = 1, max = 2147483647)
    @Column(name = "xmlsource")
    private String xmlSource;
    
    @Basic(optional = false)
    @NotNull
    @Lob
    @Size(min = 1, max = 2147483647)
    @Column(name = "xmlcompiled")
    private String xmlCompiled;
    
    @Column(name = "processtime")
    private LocalDateTime processTime;
    @Basic(optional = false)
    
    @Column(name = "referencetime")
    private BigInteger referencetime;
    

    @Transient
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

    public AppXmlSource() {
    }

    public AppXmlSource(Long idxmlsource) {
        this.idxmlSource = idxmlsource;
    }


    @Override
    public Long getIdXmlSource() {
        return idxmlSource;
    }

    @Override
    public void setIdXmlSource(Long idxmlsource) {
        this.idxmlSource = idxmlsource;
    }

    
    @Override
    public Long getIdObject() {
        return idObjeto;
    }

    @Override
    public void setIdObject(Long idobjeto) {
        this.idObjeto = idobjeto;
    }
    
    @Override
    public String getXmlName() {
        return xmlName;
    }

    @Override
    public void setXmlName(String xmlname) {
        this.xmlName = xmlname;
    }

    @Override
    public String getXmlPath() {
        return xmlPath;
    }

    @Override
    public void setXmlPath(String xmlPath) {
        this.xmlPath = xmlPath;
    }
    
    @Override
    public String getXmlSource() {
        return xmlSource;
    }

    @Override
    public void setXmlSource(String xmlsource) {
        this.xmlSource = xmlsource;
    }

    @Override
    public String getXmlCompiled() {
        return xmlCompiled;
    }

    @Override
    public void setXmlCompiled(String xmlcompile) {
        this.xmlCompiled = xmlcompile;
    }

    @Override
    public LocalDateTime getProcessTime() {
        return processTime;
    }

    @Override
    public void setProcessTime(LocalDateTime processtime) {
        this.processTime = processtime;
    }
    
    @Override
    public BigInteger getReferencetime() {
        return referencetime;
    }

    @Override
    public void setReferencetime(BigInteger referencetime) {
        this.referencetime = referencetime;
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
    public boolean isValid() {
        if (isNullorEmpty(xmlCompiled)){
            return false;
        }
        return processTime.isAfter(fechamodificacion);
    }
    

    @Override
    public String toString() {
        return "net.makerapp.model.tables.Appxmlsource[ idxmlsource=" + idxmlSource + " ]";
    }
}

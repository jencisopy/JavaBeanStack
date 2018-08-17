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
package org.javabeanstack.model.tables;


import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

import org.javabeanstack.model.IAppSystemParam;
import org.javabeanstack.data.DataRow;


/**
 *
 * @author Jorge Enciso
 */
@Entity
@Table(name = "systemparam")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "AppSystemParam.findAll", query = "SELECT s FROM AppSystemParam s")})
public class AppSystemParam extends DataRow implements IAppSystemParam {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)            
    @Basic(optional = false)
    @NotNull
    @Column(name = "idsystemparam")
    private Long idsystemparam;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "systemgroup")
    private String systemgroup;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "param")
    private String param;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "paramDescrip")
    private String paramDescrip;
    @Basic(optional = false)
    @NotNull
    @Column(name = "paramType")
    private Character paramType;
    @Column(name = "valueDate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date valueDate;
    @Column(name = "valueNumber")
    private Long valueNumber;
    @Column(name = "valueBoolean")
    private Boolean valueBoolean;
    @Size(max = 200)
    @Column(name = "valueChar")
    private String valueChar;
    
    @Transient    
    @Basic(optional = false)
    @NotNull
    @Column(name = "fechacreacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechacreacion;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "fechamodificacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechamodificacion;
    
    @Column(name = "fechareplicacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechareplicacion;
    
    @Size(max = 32)
    @Column(name = "firma")
    private String firma;
    @Size(max = 32)
    
    @Column(name = "appuser")
    private String appuser;

    public AppSystemParam() {
    }

    public AppSystemParam(Long idsystemparam) {
        this.idsystemparam = idsystemparam;
    }

    public AppSystemParam(Long idsystemparam, String systemgroup, String param, String paramDescrip, Character paramType, Date fechacreacion, Date fechamodificacion) {
        this.idsystemparam = idsystemparam;
        this.systemgroup = systemgroup;
        this.param = param;
        this.paramDescrip = paramDescrip;
        this.paramType = paramType;
        this.fechacreacion = fechacreacion;
        this.fechamodificacion = fechamodificacion;
    }

    @Override
    public Long getIdsystemparam() {
        return idsystemparam;
    }

    @Override
    public void setIdsystemparam(Long idsystemparam) {
        this.idsystemparam = idsystemparam;
    }

    @Override
    public String getSystemgroup() {
        return systemgroup;
    }

    @Override
    public void setSystemgroup(String systemgroup) {
        this.systemgroup = systemgroup;
    }

    @Override
    public String getParam() {
        return param;
    }

    @Override
    public void setParam(String param) {
        this.param = param;
    }

    @Override
    public String getParamDescrip() {
        return paramDescrip;
    }

    @Override
    public void setParamDescrip(String paramDescrip) {
        this.paramDescrip = paramDescrip;
    }

    @Override
    public Character getParamType() {
        return paramType;
    }

    @Override
    public void setParamType(Character paramType) {
        this.paramType = paramType;
    }

    @Override
    public Date getValueDate() {
        return valueDate;
    }

    @Override
    public void setValueDate(Date valueDate) {
        this.valueDate = valueDate;
    }

    @Override
    public Long getValueNumber() {
        return valueNumber;
    }

    @Override
    public void setValueNumber(Long valueNumber) {
        this.valueNumber = valueNumber;
    }

    @Override
    public Boolean getValueBoolean() {
        return valueBoolean;
    }

    @Override
    public void setValueBoolean(Boolean valueBoolean) {
        this.valueBoolean = valueBoolean;
    }

    @Override
    public String getValueChar() {
        return valueChar;
    }

    @Override
    public void setValueChar(String valueChar) {
        this.valueChar = valueChar;
    }

    public Date getFechacreacion() {
        return fechacreacion;
    }


    public Date getFechamodificacion() {
        return fechamodificacion;
    }

    public Date getFechareplicacion() {
        return fechareplicacion;
    }

    public void setFechareplicacion(Date fechareplicacion) {
        this.fechareplicacion = fechareplicacion;
    }

    public String getFirma() {
        return firma;
    }

    public void setFirma(String firma) {
        this.firma = firma;
    }

    public String getAppuser() {
        return appuser;
    }

    public void setAppuser(String appuser) {
        this.appuser = appuser;
    }


    @Override
    public String toString() {
        return "net.makerapp.model.tables.Systemparam[ idsystemparam=" + idsystemparam + " ]";
    }
}

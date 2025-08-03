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


import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.javabeanstack.model.IAppSystemParam;
import org.javabeanstack.data.DataRow;


/**
 *
 * @author Jorge Enciso
 */
@Entity
@Table(name = "appsystemparam")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "AppSystemParam.findAll", query = "SELECT s FROM AppSystemParam s")})
public class AppSystemParam extends DataRow implements IAppSystemParam {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)            
    @Basic(optional = false)
    @NotNull
    @Column(name = "idappsystemparam")
    private Long idAppSystemParam;
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
    private String paramDescrip
            ;
    @Basic(optional = false)
    @NotNull
    @Column(name = "paramType")
    private Character paramType;
    
    @Column(name = "valueDate")
    private LocalDateTime valueDate;
    
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
    private LocalDateTime fechacreacion;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "fechamodificacion")
    private LocalDateTime fechamodificacion;
    
    @Column(name = "fechareplicacion")
    private LocalDateTime fechareplicacion;
    
    @Size(max = 32)
    @Column(name = "firma")
    private String firma;
    @Size(max = 32)
    
    @Column(name = "appuser")
    private String appuser;

    public AppSystemParam() {
    }

    public AppSystemParam(Long idsystemparam) {
        this.idAppSystemParam = idsystemparam;
    }


    @Override
    public Long getIdAppSystemParam() {
        return idAppSystemParam;
    }

    @Override
    public void setIdAppSystemParam(Long idAppSystemParam) {
        this.idAppSystemParam = idAppSystemParam;
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
    public LocalDateTime getValueDate() {
        return valueDate;
    }

    @Override
    public void setValueDate(LocalDateTime valueDate) {
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
    public Object getValue() {
        if (paramType == null) {
            return null;
        }
        Object result = null;
        switch (paramType) {
            case 'C':
                result = getValueChar();
                break;
            case 'L':
                result = getValueBoolean();
                break;
            case 'N':
                result = getValueNumber();
                break;
            case 'D':
                result = getValueDate();
                break;
            default:
                break;
        }
        return result;
    }

    @Override
    public void setValue(Object value) throws Exception {
        if (value == null) {
            return;
        }
        if (paramType == null) {
            if (value instanceof String) {
                setParamType('C');
                setValueChar((String) value);
            } else if (value instanceof Boolean) {
                setParamType('B');
                setValueBoolean((Boolean) value);
            } else if (value instanceof LocalDateTime) {
                setParamType('D');
                setValueDate((LocalDateTime) value);
            } else if (value instanceof BigDecimal) {
                setParamType('N');
                setValueNumber(((BigDecimal) value).longValue());
            } else if (value instanceof Number) {
                setParamType('N');
                setValueNumber(Long.valueOf(value.toString()));
            }
        } else {
            switch (paramType) {
                case 'C':
                    setValueChar((String) value);
                    break;
                case 'L':
                    setValueBoolean((Boolean) value);
                    break;
                case 'N':
                    setValueNumber(Long.valueOf(value.toString()));
                    break;
                case 'D':
                    setValueDate((LocalDateTime)value);
                    break;
                default:
                    throw new Exception("Tipo de dato no contemplado");
            }
        }
    }
    
    @PrePersist
    @PreUpdate
    public void prePersistAndPreUpdate() {
        fechamodificacion = LocalDateTime.now();
    }
}

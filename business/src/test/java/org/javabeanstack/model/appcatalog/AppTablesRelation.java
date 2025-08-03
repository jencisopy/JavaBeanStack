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

import java.time.LocalDateTime;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.javabeanstack.data.DataRow;
import org.javabeanstack.model.IAppTablesRelation;
import org.javabeanstack.util.LocalDateTimeAdapter;

/**
 *
 * @author Jorge Enciso
 */
@Entity
@Table(name = "dic_tablarelacion")
@NamedQueries({
    @NamedQuery(name = "AppTablesRelation.findAll", query = "SELECT d FROM AppTablesRelation d"),
    @NamedQuery(name = "AppTablesRelation.findByEntity", query = 
            "SELECT d FROM AppTablesRelation d"
                    + " where entityPK = :entityPK "
                    + " and entityFK = :entityFK "
                    + " and included = true")
                })
public class AppTablesRelation extends DataRow implements IAppTablesRelation {
    private static final long serialVersionUID = 1L;

    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 30)
    @Column(name = "externa")
    private String entityFK;
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 30)
    @Column(name = "principal")
    private String entityPK;
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "expresion_externa")
    private String fieldsFK;
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "expresion_principal")
    private String fieldsPK;

    @Column(name = "incluir")
    private Boolean included;
    @Column(name = "marcado")
    private Boolean marcado;
    @Basic(optional = false)
    @NotNull
    @Column(name = "tiporelacion")
    private short relationType;

    @Transient
    @Column(name = "fechacreacion")
    @XmlJavaTypeAdapter(type=LocalDateTime.class,  value=LocalDateTimeAdapter.class)                
    private LocalDateTime fechacreacion;

    @Column(name = "fechamodificacion")
    @XmlJavaTypeAdapter(type=LocalDateTime.class,  value=LocalDateTimeAdapter.class)                
    private LocalDateTime fechamodificacion;
    
    @Column(name = "fechareplicacion")
    @XmlJavaTypeAdapter(type=LocalDateTime.class,  value=LocalDateTimeAdapter.class)                
    private LocalDateTime fechareplicacion;

    public AppTablesRelation() {
    }

    @Override
    public String getEntityPK() {
        return entityPK;
    }

    @Override
    public String getFieldsPK() {
        return this.fieldsPK;
    }

    @Override
    public String getEntityFK() {
        return entityFK;
    }

    @Override
    public String getFieldsFK() {
        return fieldsFK;
    }

    @Override
    public void setEntityPK(String entity) {
        this.entityPK = entity;
    }

    @Override
    public void setFieldsPK(String fields) {
        this.fieldsPK = fields;
    }

    @Override
    public void setEntityFK(String entity) {
        this.entityFK = entity;
    }

    @Override
    public void setFieldsFK(String fields) {
        this.fieldsFK = fields;
    }
    
    @Override
    public boolean isIncluded() {
        return included;
    }

    @Override
    public void setIncluded(boolean incluir) {
        this.included = incluir;
    }

    @Override
    public short getRelationType() {
        return relationType;
    }

    @Override
    public void setRelationType(short tiporelacion) {
        this.relationType = tiporelacion;
    }

    public Boolean getMarcado() {
        return marcado;
    }

    public void setMarcado(Boolean marcado) {
        this.marcado = marcado;
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


    @Override
    public String toString() {
        return "org.javabeanstack.model.appcatalog.AppTablesRelation[ dicTablarelacionPK= ]";
    }
}

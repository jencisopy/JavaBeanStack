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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.SequenceGenerator;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Transient;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.javabeanstack.data.DataRow;

/**
 *
 * @author Robot
 *
 */
@Entity
@Table(name = "pais", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"codigo", "idempresa"})})
@XmlRootElement
@SequenceGenerator(name = "PAIS_SEQ", allocationSize = 1, sequenceName = "PAIS_SEQ")
public class Pais extends DataRow {

    @Id
    @Basic(optional = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "PAIS_SEQ")
    @Column(name = "idpais")
    private Long idpais;

    @JoinColumn(name = "idregion", referencedColumnName = "idregion")
    @ManyToOne
    private Region region;

    @NotNull
    @Column(name = "idempresa")
    private Long idempresa;

    @JoinColumn(name = "idmoneda", referencedColumnName = "idmoneda")
    @ManyToOne
    private Moneda moneda;

    @NotNull
    @Size(min = 1, max = 4)
    @Column(name = "codigo")
    private String codigo;

    @NotNull
    @Size(min = 1, max = 30)
    @Column(name = "nombre")
    private String nombre;

    @Column(name = "noedit")
    private Boolean noedit;

    @Column(name = "latitud")
    private BigDecimal latitud;

    @Column(name = "longitud")
    private BigDecimal longitud;

    @Column(name = "fechareplicacion")
    private LocalDateTime fechareplicacion;

    @Column(name = "fechamodificacion")
    private LocalDateTime fechamodificacion;

    @Transient
    @Column(name = "fechacreacion")
    private LocalDateTime fechacreacion;

    @Size(max = 32)
    @Column(name = "firma")
    private String firma;

    @Size(max = 32)
    @Column(name = "appuser")
    private String appuser;

    public Pais() {

    }

    public Long getIdpais() {
        this.onGetter("idpais");
        return idpais;
    }

    public void setIdpais(Long idpais) {
        this.idpais = idpais;
        this.onSetter("idpais", this.idpais, idpais);
    }

    @XmlTransient
    public Region getRegion() {
        this.onGetter("region");
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
        this.onSetter("region", this.region, region);
    }

    public Long getIdempresa() {
        this.onGetter("idempresa");
        return idempresa;
    }

    public void setIdempresa(Long idempresa) {
        this.idempresa = idempresa;
        this.onSetter("idempresa", this.idempresa, idempresa);
    }

    @XmlTransient
    public Moneda getMoneda() {
        this.onGetter("moneda");
        return moneda;
    }

    public void setMoneda(Moneda moneda) {
        this.moneda = moneda;
        this.onSetter("moneda", this.moneda, moneda);
    }

    public String getCodigo() {
        this.onGetter("codigo");
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
        this.onSetter("codigo", this.codigo, codigo);
    }

    public String getNombre() {
        this.onGetter("nombre");
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
        this.onSetter("nombre", this.nombre, nombre);
    }

    public Boolean getNoedit() {
        this.onGetter("noedit");
        return noedit;
    }

    public void setNoedit(Boolean noedit) {
        this.noedit = noedit;
        this.onSetter("noedit", this.noedit, noedit);
    }

    public BigDecimal getLatitud() {
        this.onGetter("latitud");
        return latitud;
    }

    public void setLatitud(BigDecimal latitud) {
        this.latitud = latitud;
        this.onSetter("latitud", this.latitud, latitud);
    }

    public BigDecimal getLongitud() {
        this.onGetter("longitud");
        return longitud;
    }

    public void setLongitud(BigDecimal longitud) {
        this.longitud = longitud;
        this.onSetter("longitud", this.longitud, longitud);
    }

    public LocalDateTime getFechareplicacion() {
        this.onGetter("fechareplicacion");
        return fechareplicacion;
    }

    public void setFechareplicacion(LocalDateTime fechareplicacion) {
        this.fechareplicacion = fechareplicacion;
        this.onSetter("fechareplicacion", this.fechareplicacion, fechareplicacion);
    }

    public LocalDateTime getFechamodificacion() {
        this.onGetter("fechamodificacion");
        return fechamodificacion;
    }

    public void setFechamodificacion(LocalDateTime fechamodificacion) {
        this.fechamodificacion = fechamodificacion;
        this.onSetter("fechamodificacion", this.fechamodificacion, fechamodificacion);
    }

    public LocalDateTime getFechacreacion() {
        this.onGetter("fechacreacion");
        return fechacreacion;
    }

    public void setFechacreacion(LocalDateTime fechacreacion) {
        this.fechacreacion = fechacreacion;
        this.onSetter("fechacreacion", this.fechacreacion, fechacreacion);
    }

    public String getFirma() {
        this.onGetter("firma");
        return firma;
    }

    public void setFirma(String firma) {
        this.firma = firma;
        this.onSetter("firma", this.firma, firma);
    }

    public String getAppuser() {
        this.onGetter("appuser");
        return appuser;
    }

    public void setAppuser(String appuser) {
        this.appuser = appuser;
        this.onSetter("appuser", this.appuser, appuser);
    }

    @PrePersist
    @PreUpdate
    public void prePersistAndPreUpdate() {
        fechamodificacion = LocalDateTime.now();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + Objects.hashCode(this.idpais);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Pais other = (Pais) obj;
        return Objects.equals(this.getIdpais(), other.getIdpais());
    }

    @Override
    public boolean equivalent(Object o) {
        if (!(o instanceof Pais)) {
            return false;
        }
        Pais obj = (Pais) o;
        return (this.codigo.trim().equals(obj.getCodigo().trim())
                && Objects.equals(this.idempresa, obj.getIdempresa()));
    }

    @Override
    public String toString() {
        return "net.makerapp.model.tables.Pais{" + "\"idpais\":" + idpais + ", \"codigo\":\"" + codigo + "\", \"nombre\":\"" + nombre + "\"}";
    }

}

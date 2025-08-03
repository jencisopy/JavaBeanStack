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
package org.javabeanstack.model.views;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.javabeanstack.annotation.ColumnFunction;
import org.javabeanstack.data.DataRow;
import org.javabeanstack.util.LocalDateTimeAdapter;

/**
 *
 * @author Robot
 *
 */
@Entity
@Table(name = "pais_view")
@XmlRootElement

public class PaisView extends DataRow {

    @Id
    @Basic(optional = false)
    @Column(name = "idpais")
    private Long idpais;

    @ColumnFunction(formula = "fn_idregion(:idregion,:region,:idempresa)", classMapped = "org.javabeanstack.model.tables.Region")
    @NotNull
    @Column(name = "idregion")
    private Long idregion;

    @ColumnFunction(formula = ":idempresa")
    @NotNull
    @Column(name = "idempresa")
    private Long idempresa;

    @ColumnFunction(formula = "fn_idmoneda(:idmoneda,:moneda,:idempresa)", classMapped = "org.javabeanstack.model.tables.Moneda")
    @Column(name = "idmoneda")
    private Long idmoneda;

    @ColumnFunction(formula = ":codigo")
    @NotNull
    @Size(min = 1, max = 4)
    @Column(name = "codigo")
    private String codigo;

    @ColumnFunction(formula = ":nombre")
    @NotNull
    @Size(min = 1, max = 30)
    @Column(name = "nombre")
    private String nombre;

    @ColumnFunction(formula = ":noedit")
    @Column(name = "noedit")
    private Boolean noedit;

    @ColumnFunction(formula = ":latitud")
    @Column(name = "latitud")
    private BigDecimal latitud;

    @ColumnFunction(formula = ":longitud")
    @Column(name = "longitud")
    private BigDecimal longitud;

    @Column(name = "fechareplicacion")
    @XmlJavaTypeAdapter(type=LocalDateTime.class,  value=LocalDateTimeAdapter.class)        
    private LocalDateTime fechareplicacion;

    @Column(name = "fechamodificacion")
    @XmlJavaTypeAdapter(type=LocalDateTime.class,  value=LocalDateTimeAdapter.class)        
    private LocalDateTime fechamodificacion;

    @Transient
    @Column(name = "fechacreacion")
    @XmlJavaTypeAdapter(type=LocalDateTime.class,  value=LocalDateTimeAdapter.class)        
    private LocalDateTime fechacreacion;

    @Size(max = 32)
    @Column(name = "firma")
    private String firma;

    @Size(max = 32)
    @Column(name = "appuser")
    private String appuser;

    @Size(max = 4)
    @Column(name = "region")
    private String region;

    @Size(max = 50)
    @Column(name = "regionnombre")
    private String regionnombre;

    @Size(max = 3)
    @Column(name = "moneda")
    private String moneda;

    public PaisView() {

    }

    public Long getIdpais() {
        this.onGetter("idpais");
        return idpais;
    }

    public void setIdpais(Long idpais) {
        this.idpais = idpais;
        this.onSetter("idpais", this.idpais, idpais);
    }

    public Long getIdregion() {
        this.onGetter("idregion");
        return idregion;
    }

    public void setIdregion(Long idregion) {
        this.idregion = idregion;
        this.onSetter("idregion", this.idregion, idregion);
    }

    public Long getIdempresa() {
        this.onGetter("idempresa");
        return idempresa;
    }

    public void setIdempresa(Long idempresa) {
        this.idempresa = idempresa;
        this.onSetter("idempresa", this.idempresa, idempresa);
    }

    public Long getIdmoneda() {
        this.onGetter("idmoneda");
        return idmoneda;
    }

    public void setIdmoneda(Long idmoneda) {
        this.idmoneda = idmoneda;
        this.onSetter("idmoneda", this.idmoneda, idmoneda);
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

    public String getRegion() {
        this.onGetter("region");
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
        this.onSetter("region", this.region, region);
    }

    public String getRegionnombre() {
        this.onGetter("regionnombre");
        return regionnombre;
    }

    public void setRegionnombre(String regionnombre) {
        this.regionnombre = regionnombre;
        this.onSetter("regionnombre", this.regionnombre, regionnombre);
    }

    public String getMoneda() {
        this.onGetter("moneda");
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
        this.onSetter("moneda", this.moneda, moneda);
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
        final PaisView other = (PaisView) obj;
        return Objects.equals(this.getIdpais(), other.getIdpais());
    }

    @Override
    public String toString() {
        return "net.makerapp.model.views.PaisView{" + "\"idpais\":" + idpais + ", \"codigo\":\"" + codigo + "\", \"nombre\":\"" + nombre + "\"}";
    }

}

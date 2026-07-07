/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2015 - 2027 Jorge Enciso
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
package org.javabeanstack.model.tables;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Transient;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.javabeanstack.data.DataRow;

/**
 *
 * @author Robot
 *
 */
@Entity
@Table(name = "moneda", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"codigo", "idempresa"})})
@XmlRootElement
@SequenceGenerator(name = "MONEDA_SEQ", allocationSize = 1, sequenceName = "MONEDA_SEQ")
public class Moneda extends DataRow {

    @Id
    @Basic(optional = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "MONEDA_SEQ")
    @Column(name = "idmoneda")
    private Long idmoneda;

    @NotNull
    @Column(name = "idempresa")
    private Long idempresa;

    @NotNull
    @Size(min = 1, max = 3)
    @Column(name = "codigo")
    private String codigo;

    @NotNull
    @Size(min = 1, max = 30)
    @Column(name = "nombre")
    private String nombre;

    @NotNull
    @Column(name = "cambio")
    private BigDecimal cambio;

    @Lob
    @Size(max = 2147483647)
    @Column(name = "observacion")
    private String observacion;

    @Column(name = "decimalpoint")
    private Short decimalpoint;

    @Size(max = 32)
    @Column(name = "firma")
    private String firma;

    @Column(name = "fechareplicacion")
    private LocalDateTime fechareplicacion;

    @Column(name = "fechamodificacion")
    private LocalDateTime fechamodificacion;

    @Transient
    @Column(name = "fechacreacion")
    private LocalDateTime fechacreacion;

    @Size(max = 32)
    @Column(name = "appuser")
    private String appuser;

    public Moneda() {

    }

    public Long getIdmoneda() {
        this.onGetter("idmoneda");
        return idmoneda;
    }

    public void setIdmoneda(Long idmoneda) {
        this.idmoneda = idmoneda;
        this.onSetter("idmoneda", this.idmoneda, idmoneda);
    }

    public Long getIdempresa() {
        this.onGetter("idempresa");
        return idempresa;
    }

    public void setIdempresa(Long idempresa) {
        this.idempresa = idempresa;
        this.onSetter("idempresa", this.idempresa, idempresa);
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

    public BigDecimal getCambio() {
        this.onGetter("cambio");
        return cambio;
    }

    public void setCambio(BigDecimal cambio) {
        this.cambio = cambio;
        this.onSetter("cambio", this.cambio, cambio);
    }

    public String getObservacion() {
        this.onGetter("observacion");
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
        this.onSetter("observacion", this.observacion, observacion);
    }

    public Short getDecimalpoint() {
        this.onGetter("decimalpoint");
        return decimalpoint;
    }

    public void setDecimalpoint(Short decimalpoint) {
        this.decimalpoint = decimalpoint;
        this.onSetter("decimalpoint", this.decimalpoint, decimalpoint);
    }

    public String getFirma() {
        this.onGetter("firma");
        return firma;
    }

    public void setFirma(String firma) {
        this.firma = firma;
        this.onSetter("firma", this.firma, firma);
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
        hash = 67 * hash + Objects.hashCode(this.idmoneda);
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
        final Moneda other = (Moneda) obj;
        return Objects.equals(this.getIdmoneda(), other.getIdmoneda());
    }

    @Override
    public boolean equivalent(Object o) {
        if (!(o instanceof Moneda)) {
            return false;
        }
        Moneda obj = (Moneda) o;
        return (this.codigo.trim().equals(obj.getCodigo().trim())
                && Objects.equals(this.idempresa, obj.getIdempresa()));
    }

    @Override
    public String toString() {
        return "net.makerapp.model.tables.Moneda{" + "\"idmoneda\":" + idmoneda + ", \"codigo\":\"" + codigo + "\", \"nombre\":\"" + nombre + "\"}";
    }

}

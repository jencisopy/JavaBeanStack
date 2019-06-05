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
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import org.javabeanstack.annotation.ColumnFunction;
import org.javabeanstack.data.DataRow;

/**
 *
 * @author Robot
 * Se modifico manualmente idctactesub
 */
@Entity
@Table(name = "ctactemovimientodetalle_view")
@XmlRootElement
public class CtactemovimientodetalleView extends DataRow {

    @Id
    @Basic(optional = false)
    @Column(name = "idctactemovimientodetalle")
    private Long idctactemovimientodetalle;

    @ColumnFunction(formula = "fn_idctactemovimiento(:idctactemovimiento,:idnro)")
    @NotNull
    @Column(name = "idctactemovimiento")
    private Long idctactemovimiento;

    @ColumnFunction(formula = "fn_idctactependientedetalle(:idctactependientedetalle,:ctactependiente,:cuota,:cuotatipo)")
    @NotNull
    @Column(name = "idctactependientedetalle")
    private Long idctactependientedetalle;

    @ColumnFunction(formula = ":monto")
    @NotNull
    @Column(name = "monto")
    private BigDecimal monto;

    @ColumnFunction(formula = ":nrointeres")
    @Size(max = 10)
    @Column(name = "nrointeres")
    private String nrointeres;

    @ColumnFunction(formula = ":interes")
    @Column(name = "interes")
    private BigDecimal interes;

    @ColumnFunction(formula = ":mora")
    @Column(name = "mora")
    private BigDecimal mora;

    @ColumnFunction(formula = ":cobranzarecargo")
    @Column(name = "cobranzarecargo")
    private BigDecimal cobranzarecargo;

    @ColumnFunction(formula = ":descuento")
    @Column(name = "descuento")
    private BigDecimal descuento;

    @ColumnFunction(formula = ":ctacteafecta")
    @Column(name = "ctacteafecta")
    private Short ctacteafecta;

    @ColumnFunction(formula = ":tasamoradescuento")
    @Column(name = "tasamoradescuento")
    private BigDecimal tasamoradescuento;

    @ColumnFunction(formula = ":cobranzarecargodesc")
    @Column(name = "cobranzarecargodesc")
    private BigDecimal cobranzarecargodesc;

    @ColumnFunction(formula = ":montomoradescuento")
    @Column(name = "montomoradescuento")
    private BigDecimal montomoradescuento;

    @ColumnFunction(formula = ":montocobranzarecargodesc")
    @Column(name = "montocobranzarecargodesc")
    private BigDecimal montocobranzarecargodesc;

    @ColumnFunction(formula = ":montoanticipadodesc")
    @Column(name = "montoanticipadodesc")
    private BigDecimal montoanticipadodesc;

    @ColumnFunction(formula = ":tolerancia")
    @NotNull
    @Column(name = "tolerancia")
    private Long tolerancia;

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

    @ColumnFunction(formula = ":audit_uk")
    @Size(max = 15)
    @Column(name = "audit_uk")
    private String auditUk;

    @Size(max = 32)
    @Column(name = "appuser")
    private String appuser;

    @NotNull
    @Size(min = 1, max = 35)
    @Column(name = "idnro")
    private String idnro;

    @NotNull
    @Size(min = 1, max = 2)
    @Column(name = "sucursal")
    private String sucursal;

    @Size(max = 10)
    @Column(name = "nro")
    private String nro;

    @Size(max = 35)
    @Column(name = "ctactependiente")
    private String ctactependiente;

    @NotNull
    @Column(name = "idempresa")
    private Long idempresa;

    @Size(max = 50)
    @Column(name = "documentonombre")
    private String documentonombre;

    @Size(max = 50)
    @Column(name = "documentotiponombre")
    private String documentotiponombre;

    @NotNull
    @Column(name = "fechadoc")
    private LocalDateTime fechadoc;

    @NotNull
    @Column(name = "fec_ven")
    private LocalDateTime fecVen;

    @Column(name = "saldo")
    private BigDecimal saldo;

    @NotNull
    @Column(name = "seleccion")
    private Integer seleccion;

    @Column(name = "tasa")
    private BigDecimal tasa;

    @Column(name = "tasamoratoria")
    private BigDecimal tasamoratoria;

    @Size(max = 3)
    @Column(name = "iddocumento")
    private String iddocumento;

    @Size(max = 2)
    @Column(name = "iddocumentotipo")
    private String iddocumentotipo;

    @Size(max = 7)
    @Column(name = "secuencia")
    private String secuencia;

    @Size(max = 10)
    @Column(name = "nrodoc")
    private String nrodoc;

    @Column(name = "montodoc")
    private BigDecimal montodoc;

    @Column(name = "capital")
    private BigDecimal capital;

    @Column(name = "cuota")
    private Short cuota;

    @Size(max = 3)
    @Column(name = "moneda")
    private String moneda;

    @Size(max = 2)
    @Column(name = "concepto")
    private String concepto;

    @Size(max = 3)
    @Column(name = "itemmovcondicion")
    private String itemmovcondicion;

    @Size(max = 2)
    @Column(name = "rubro")
    private String rubro;

    @Size(max = 2)
    @Column(name = "subrubro")
    private String subrubro;

    @Column(name = "ultpago")
    private LocalDateTime ultpago;

    @Column(name = "totalcuota")
    private Long totalcuota;

    @NotNull
    @Column(name = "cuotatipo")
    private Short cuotatipo;

    public CtactemovimientodetalleView() {

    }

    public Long getIdctactemovimientodetalle() {
        this.onGetter("idctactemovimientodetalle");
        return idctactemovimientodetalle;
    }

    public void setIdctactemovimientodetalle(Long idctactemovimientodetalle) {
        this.idctactemovimientodetalle = idctactemovimientodetalle;
        this.onSetter("idctactemovimientodetalle", this.idctactemovimientodetalle, idctactemovimientodetalle);
    }

    public Long getIdctactemovimiento() {
        this.onGetter("idctactemovimiento");
        return idctactemovimiento;
    }

    public void setIdctactemovimiento(Long idctactemovimiento) {
        this.idctactemovimiento = idctactemovimiento;
        this.onSetter("idctactemovimiento", this.idctactemovimiento, idctactemovimiento);
    }

    public Long getIdctactependientedetalle() {
        this.onGetter("idctactependientedetalle");
        return idctactependientedetalle;
    }

    public void setIdctactependientedetalle(Long idctactependientedetalle) {
        this.idctactependientedetalle = idctactependientedetalle;
        this.onSetter("idctactependientedetalle", this.idctactependientedetalle, idctactependientedetalle);
    }

    public BigDecimal getMonto() {
        this.onGetter("monto");
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
        this.onSetter("monto", this.monto, monto);
    }

    public String getNrointeres() {
        this.onGetter("nrointeres");
        return nrointeres;
    }

    public void setNrointeres(String nrointeres) {
        this.nrointeres = nrointeres;
        this.onSetter("nrointeres", this.nrointeres, nrointeres);
    }

    public BigDecimal getInteres() {
        this.onGetter("interes");
        return interes;
    }

    public void setInteres(BigDecimal interes) {
        this.interes = interes;
        this.onSetter("interes", this.interes, interes);
    }

    public BigDecimal getMora() {
        this.onGetter("mora");
        return mora;
    }

    public void setMora(BigDecimal mora) {
        this.mora = mora;
        this.onSetter("mora", this.mora, mora);
    }

    public BigDecimal getCobranzarecargo() {
        this.onGetter("cobranzarecargo");
        return cobranzarecargo;
    }

    public void setCobranzarecargo(BigDecimal cobranzarecargo) {
        this.cobranzarecargo = cobranzarecargo;
        this.onSetter("cobranzarecargo", this.cobranzarecargo, cobranzarecargo);
    }

    public BigDecimal getDescuento() {
        this.onGetter("descuento");
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
        this.onSetter("descuento", this.descuento, descuento);
    }

    public Short getCtacteafecta() {
        this.onGetter("ctacteafecta");
        return ctacteafecta;
    }

    public void setCtacteafecta(Short ctacteafecta) {
        this.ctacteafecta = ctacteafecta;
        this.onSetter("ctacteafecta", this.ctacteafecta, ctacteafecta);
    }

    public BigDecimal getTasamoradescuento() {
        this.onGetter("tasamoradescuento");
        return tasamoradescuento;
    }

    public void setTasamoradescuento(BigDecimal tasamoradescuento) {
        this.tasamoradescuento = tasamoradescuento;
        this.onSetter("tasamoradescuento", this.tasamoradescuento, tasamoradescuento);
    }

    public BigDecimal getCobranzarecargodesc() {
        this.onGetter("cobranzarecargodesc");
        return cobranzarecargodesc;
    }

    public void setCobranzarecargodesc(BigDecimal cobranzarecargodesc) {
        this.cobranzarecargodesc = cobranzarecargodesc;
        this.onSetter("cobranzarecargodesc", this.cobranzarecargodesc, cobranzarecargodesc);
    }

    public BigDecimal getMontomoradescuento() {
        this.onGetter("montomoradescuento");
        return montomoradescuento;
    }

    public void setMontomoradescuento(BigDecimal montomoradescuento) {
        this.montomoradescuento = montomoradescuento;
        this.onSetter("montomoradescuento", this.montomoradescuento, montomoradescuento);
    }

    public BigDecimal getMontocobranzarecargodesc() {
        this.onGetter("montocobranzarecargodesc");
        return montocobranzarecargodesc;
    }

    public void setMontocobranzarecargodesc(BigDecimal montocobranzarecargodesc) {
        this.montocobranzarecargodesc = montocobranzarecargodesc;
        this.onSetter("montocobranzarecargodesc", this.montocobranzarecargodesc, montocobranzarecargodesc);
    }

    public BigDecimal getMontoanticipadodesc() {
        this.onGetter("montoanticipadodesc");
        return montoanticipadodesc;
    }

    public void setMontoanticipadodesc(BigDecimal montoanticipadodesc) {
        this.montoanticipadodesc = montoanticipadodesc;
        this.onSetter("montoanticipadodesc", this.montoanticipadodesc, montoanticipadodesc);
    }

    public Long getTolerancia() {
        this.onGetter("tolerancia");
        return tolerancia;
    }

    public void setTolerancia(Long tolerancia) {
        this.tolerancia = tolerancia;
        this.onSetter("tolerancia", this.tolerancia, tolerancia);
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

    public String getAuditUk() {
        this.onGetter("auditUk");
        return auditUk;
    }

    public void setAuditUk(String auditUk) {
        this.auditUk = auditUk;
        this.onSetter("auditUk", this.auditUk, auditUk);
    }

    public String getAppuser() {
        this.onGetter("appuser");
        return appuser;
    }

    public void setAppuser(String appuser) {
        this.appuser = appuser;
        this.onSetter("appuser", this.appuser, appuser);
    }

    public String getIdnro() {
        this.onGetter("idnro");
        return idnro;
    }

    public void setIdnro(String idnro) {
        this.idnro = idnro;
        this.onSetter("idnro", this.idnro, idnro);
    }

    public String getSucursal() {
        this.onGetter("sucursal");
        return sucursal;
    }

    public void setSucursal(String sucursal) {
        this.sucursal = sucursal;
        this.onSetter("sucursal", this.sucursal, sucursal);
    }

    public String getNro() {
        this.onGetter("nro");
        return nro;
    }

    public void setNro(String nro) {
        this.nro = nro;
        this.onSetter("nro", this.nro, nro);
    }

    public String getCtactependiente() {
        this.onGetter("ctactependiente");
        return ctactependiente;
    }

    public void setCtactependiente(String ctactependiente) {
        this.ctactependiente = ctactependiente;
        this.onSetter("ctactependiente", this.ctactependiente, ctactependiente);
    }

    public Long getIdempresa() {
        this.onGetter("idempresa");
        return idempresa;
    }

    public void setIdempresa(Long idempresa) {
        this.idempresa = idempresa;
        this.onSetter("idempresa", this.idempresa, idempresa);
    }

    public String getDocumentonombre() {
        this.onGetter("documentonombre");
        return documentonombre;
    }

    public void setDocumentonombre(String documentonombre) {
        this.documentonombre = documentonombre;
        this.onSetter("documentonombre", this.documentonombre, documentonombre);
    }

    public String getDocumentotiponombre() {
        this.onGetter("documentotiponombre");
        return documentotiponombre;
    }

    public void setDocumentotiponombre(String documentotiponombre) {
        this.documentotiponombre = documentotiponombre;
        this.onSetter("documentotiponombre", this.documentotiponombre, documentotiponombre);
    }

    public LocalDateTime getFechadoc() {
        this.onGetter("fechadoc");
        return fechadoc;
    }

    public void setFechadoc(LocalDateTime fechadoc) {
        this.fechadoc = fechadoc;
        this.onSetter("fechadoc", this.fechadoc, fechadoc);
    }

    public LocalDateTime getFecVen() {
        this.onGetter("fecVen");
        return fecVen;
    }

    public void setFecVen(LocalDateTime fecVen) {
        this.fecVen = fecVen;
        this.onSetter("fecVen", this.fecVen, fecVen);
    }

    public BigDecimal getSaldo() {
        this.onGetter("saldo");
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
        this.onSetter("saldo", this.saldo, saldo);
    }

    public Integer getSeleccion() {
        this.onGetter("seleccion");
        return seleccion;
    }

    public void setSeleccion(Integer seleccion) {
        this.seleccion = seleccion;
        this.onSetter("seleccion", this.seleccion, seleccion);
    }

    public BigDecimal getTasa() {
        this.onGetter("tasa");
        return tasa;
    }

    public void setTasa(BigDecimal tasa) {
        this.tasa = tasa;
        this.onSetter("tasa", this.tasa, tasa);
    }

    public BigDecimal getTasamoratoria() {
        this.onGetter("tasamoratoria");
        return tasamoratoria;
    }

    public void setTasamoratoria(BigDecimal tasamoratoria) {
        this.tasamoratoria = tasamoratoria;
        this.onSetter("tasamoratoria", this.tasamoratoria, tasamoratoria);
    }

    public String getIddocumento() {
        this.onGetter("iddocumento");
        return iddocumento;
    }

    public void setIddocumento(String iddocumento) {
        this.iddocumento = iddocumento;
        this.onSetter("iddocumento", this.iddocumento, iddocumento);
    }

    public String getIddocumentotipo() {
        this.onGetter("iddocumentotipo");
        return iddocumentotipo;
    }

    public void setIddocumentotipo(String iddocumentotipo) {
        this.iddocumentotipo = iddocumentotipo;
        this.onSetter("iddocumentotipo", this.iddocumentotipo, iddocumentotipo);
    }

    public String getSecuencia() {
        this.onGetter("secuencia");
        return secuencia;
    }

    public void setSecuencia(String secuencia) {
        this.secuencia = secuencia;
        this.onSetter("secuencia", this.secuencia, secuencia);
    }

    public String getNrodoc() {
        this.onGetter("nrodoc");
        return nrodoc;
    }

    public void setNrodoc(String nrodoc) {
        this.nrodoc = nrodoc;
        this.onSetter("nrodoc", this.nrodoc, nrodoc);
    }

    public BigDecimal getMontodoc() {
        this.onGetter("montodoc");
        return montodoc;
    }

    public void setMontodoc(BigDecimal montodoc) {
        this.montodoc = montodoc;
        this.onSetter("montodoc", this.montodoc, montodoc);
    }

    public BigDecimal getCapital() {
        this.onGetter("capital");
        return capital;
    }

    public void setCapital(BigDecimal capital) {
        this.capital = capital;
        this.onSetter("capital", this.capital, capital);
    }

    public Short getCuota() {
        this.onGetter("cuota");
        return cuota;
    }

    public void setCuota(Short cuota) {
        this.cuota = cuota;
        this.onSetter("cuota", this.cuota, cuota);
    }

    public String getMoneda() {
        this.onGetter("moneda");
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
        this.onSetter("moneda", this.moneda, moneda);
    }

    public String getConcepto() {
        this.onGetter("concepto");
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
        this.onSetter("concepto", this.concepto, concepto);
    }

    public String getItemmovcondicion() {
        this.onGetter("itemmovcondicion");
        return itemmovcondicion;
    }

    public void setItemmovcondicion(String itemmovcondicion) {
        this.itemmovcondicion = itemmovcondicion;
        this.onSetter("itemmovcondicion", this.itemmovcondicion, itemmovcondicion);
    }

    public String getRubro() {
        this.onGetter("rubro");
        return rubro;
    }

    public void setRubro(String rubro) {
        this.rubro = rubro;
        this.onSetter("rubro", this.rubro, rubro);
    }

    public String getSubrubro() {
        this.onGetter("subrubro");
        return subrubro;
    }

    public void setSubrubro(String subrubro) {
        this.subrubro = subrubro;
        this.onSetter("subrubro", this.subrubro, subrubro);
    }

    public LocalDateTime getUltpago() {
        this.onGetter("ultpago");
        return ultpago;
    }

    public void setUltpago(LocalDateTime ultpago) {
        this.ultpago = ultpago;
        this.onSetter("ultpago", this.ultpago, ultpago);
    }

    public Long getTotalcuota() {
        this.onGetter("totalcuota");
        return totalcuota;
    }

    public void setTotalcuota(Long totalcuota) {
        this.totalcuota = totalcuota;
        this.onSetter("totalcuota", this.totalcuota, totalcuota);
    }


    public Short getCuotatipo() {
        this.onGetter("cuotatipo");
        return cuotatipo;
    }

    public void setCuotatipo(Short cuotatipo) {
        this.cuotatipo = cuotatipo;
        this.onSetter("cuotatipo", this.cuotatipo, cuotatipo);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + Objects.hashCode(this.idctactemovimientodetalle);
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
        final CtactemovimientodetalleView other = (CtactemovimientodetalleView) obj;
        return Objects.equals(this.getIdctactemovimientodetalle(), other.getIdctactemovimientodetalle());
    }

    @Override
    public String toString() {
        return "net.makerapp.model.views.CtactemovimientodetalleView{" + "\"idctactemovimientodetalle\":" + idctactemovimientodetalle + ", \"idnro\":\"" + idnro + "\"}";
    }
}

package org.javabeanstack.model.views;

import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import org.javabeanstack.data.DataRow;

/**
 *
 * @author mtrinidad
 */
@Entity
@Table(name = "ctactemovimientodetalle_view")
@XmlRootElement
public class CtactemovimientodetalleView extends DataRow {

    private static final long serialVersionUID = 1L;
    @Basic(optional = false)
    @NotNull
    @Column(name = "idempresa")
    private long idempresa;
    @Basic(optional = false)
    @NotNull
    @Column(name = "idctactemovimiento")
    private long idctactemovimiento;
    @Id
    @Column(name = "idctactemovimientodetalle")
    private long idctactemovimientodetalle;
    @Basic(optional = false)
    @NotNull
    @Column(name = "monto")
    private BigDecimal monto;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 10)
    @Column(name = "nro")
    private String nro;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 3)
    @Column(name = "iddocumento")
    private String iddocumento;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 3)
    @Column(name = "moneda")
    private String moneda;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 10)
    @Column(name = "nrodoc")
    private String nrodoc;

    @Transient
    @Column(name = "fecha")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fecha;

    @Basic(optional = false)
    @NotNull
    @Column(name = "fechadoc")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechadoc;
    @Column(name = "totalcuota")
    private Long totalcuota;
    @Basic(optional = false)
    @NotNull
    @Column(name = "montodoc")
    private BigDecimal montodoc;
    @Basic(optional = false)
    @NotNull
    @Column(name = "fec_ven")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fecVen;
    @Column(name = "cuota")
    private Short cuota;
    @Column(name = "ultpago")
    @Temporal(TemporalType.TIMESTAMP)
    private Date ultpago;
    @Basic(optional = false)
    @NotNull
    @Column(name = "saldo")
    private BigDecimal saldo;

    @Basic(optional = false)
    @NotNull
    @Column(name = "mora")
    private BigDecimal mora;

    @Column(name = "documentotiponombre")
    private String documentotiponombre;

    @Column(name = "documentonombre")
    private String documentonombre;
    

    public CtactemovimientodetalleView() {
    }

    public long getIdempresa() {
        return idempresa;
    }

    public void setIdempresa(long idempresa) {
        this.idempresa = idempresa;
    }

    public long getIdctactemovimiento() {
        return idctactemovimiento;
    }

    public void setIdctactemovimiento(long idctactemovimiento) {
        this.idctactemovimiento = idctactemovimiento;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getNro() {
        return nro;
    }

    public void setNro(String nro) {
        this.nro = nro;
    }

    public String getIddocumento() {
        return iddocumento;
    }

    public void setIddocumento(String iddocumento) {
        this.iddocumento = iddocumento;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getNrodoc() {
        return nrodoc;
    }

    public void setNrodoc(String nrodoc) {
        this.nrodoc = nrodoc;
    }

    public Date getFechadoc() {
        return fechadoc;
    }

    public void setFechadoc(Date fechadoc) {
        this.fechadoc = fechadoc;
    }

    public Long getTotalcuota() {
        return totalcuota;
    }

    public void setTotalcuota(Long totalcuota) {
        this.totalcuota = totalcuota;
    }

    public BigDecimal getMontodoc() {
        return montodoc;
    }

    public void setMontodoc(BigDecimal montodoc) {
        this.montodoc = montodoc;
    }

    public Date getFecVen() {
        return fecVen;
    }

    public void setFecVen(Date fecVen) {
        this.fecVen = fecVen;
    }

    public Short getCuota() {
        return cuota;
    }

    public void setCuota(Short cuota) {
        this.cuota = cuota;
    }

    public Date getUltpago() {
        return ultpago;
    }

    public void setUltpago(Date ultpago) {
        this.ultpago = ultpago;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public long getIdctactemovimientodetalle() {
        return idctactemovimientodetalle;
    }

    public void setIdctactemovimientodetalle(long idctactemovimientodetalle) {
        this.idctactemovimientodetalle = idctactemovimientodetalle;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getMora() {
        return mora;
    }

    public void setMora(BigDecimal mora) {
        this.mora = mora;
    }

    public String getDocumentotiponombre() {
        return documentotiponombre;
    }

    public void setDocumentotiponombre(String documentotiponombre) {
        this.documentotiponombre = documentotiponombre;
    }

    public String getDocumentonombre() {
        return documentonombre;
    }

    public void setDocumentonombre(String documentonombre) {
        this.documentonombre = documentonombre;
    }
}

/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
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

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import org.javabeanstack.data.DataRow;

/**
 * Entidad de PRUEBA de la traducción columna→atributo de la clave única (plan
 * {@code QUERYUK}). No corresponde a ninguna tabla real ni figura en una unidad
 * de persistencia: existe solo para que {@code DataInfoQueryUkTest} tenga en un
 * mismo lugar las situaciones que el framework tiene que resolver.
 *
 * <p>Su clave única {@code (codigo, idprovincia, nombre_corto)} junta las tres
 * familias: una columna que coincide con su atributo, una clave foránea mapeada
 * <b>solo</b> como asociación y una columna escrita distinto del atributo.
 * Además trae las dos trampas de precedencia: la columna {@code idpais}, que es
 * a la vez un escalar de solo lectura y una asociación, y la columna
 * {@code zonaurbana}, que la mapea {@code zonaurbanaCodigo} mientras existe una
 * asociación llamada justamente {@code zonaurbana}.</p>
 *
 * @author Jorge Enciso
 */
@Entity
@Table(name = "ciudad", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"codigo", "idprovincia", "nombre_corto"})})
@SequenceGenerator(name = "CIUDAD_SEQ", allocationSize = 1, sequenceName = "CIUDAD_SEQ")
public class Ciudad extends DataRow {

    @Id
    @Basic(optional = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "CIUDAD_SEQ")
    @Column(name = "idciudad")
    private Long idciudad;

    @ManyToOne
    @JoinColumn(name = "idprovincia", referencedColumnName = "idpais")
    private Pais provincia;

    @Column(name = "codigo")
    private String codigo;

    @Column(name = "nombre_corto")
    private String nombreCorto;

    //La misma columna como escalar de solo lectura y como asociación: tiene
    //que ganar el escalar.
    @Column(name = "idpais", insertable = false, updatable = false)
    private Long idpais;

    @ManyToOne
    @JoinColumn(name = "idpais", referencedColumnName = "idpais",
            insertable = false, updatable = false)
    private Pais pais;

    //La asociación se llama igual que la columna de otro atributo: tiene que
    //ganar el atributo anotado con @Column(name = "zonaurbana").
    @ManyToOne
    @JoinColumn(name = "idzonaurbana", referencedColumnName = "idregion")
    private Region zonaurbana;

    @Column(name = "zonaurbana")
    private String zonaurbanaCodigo;

    //Atributo no persistido: no mapea ninguna columna.
    @Transient
    @Column(name = "descripcion")
    private String descripcion;

    public Ciudad() {
    }

    public Long getIdciudad() {
        return idciudad;
    }

    public void setIdciudad(Long idciudad) {
        this.idciudad = idciudad;
    }

    public Pais getProvincia() {
        return provincia;
    }

    public void setProvincia(Pais provincia) {
        this.provincia = provincia;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombreCorto() {
        return nombreCorto;
    }

    public void setNombreCorto(String nombreCorto) {
        this.nombreCorto = nombreCorto;
    }

    public Long getIdpais() {
        return idpais;
    }

    public void setIdpais(Long idpais) {
        this.idpais = idpais;
    }

    public Pais getPais() {
        return pais;
    }

    public void setPais(Pais pais) {
        this.pais = pais;
    }

    public Region getZonaurbana() {
        return zonaurbana;
    }

    public void setZonaurbana(Region zonaurbana) {
        this.zonaurbana = zonaurbana;
    }

    public String getZonaurbanaCodigo() {
        return zonaurbanaCodigo;
    }

    public void setZonaurbanaCodigo(String zonaurbanaCodigo) {
        this.zonaurbanaCodigo = zonaurbanaCodigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}

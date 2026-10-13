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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.javabeanstack.data.DataRow;

/**
 * Entidad de PRUEBA de una clave única que <b>no se puede</b> traducir (plan
 * {@code QUERYUK}). No corresponde a ninguna tabla real ni figura en una unidad
 * de persistencia.
 *
 * <p>Su clave única nombra la columna {@code zona}, que la entidad no mapea con
 * ningún atributo: lo único que se llama así es un campo {@code static}, que no
 * cuenta. Es la familia del {@code CtacteHlp} de Maker, y el resultado esperado
 * es que la consulta de clave única sea nula —el control de duplicados queda
 * sin efecto, declarado y con una advertencia en el log— en vez de un HQL que
 * no se puede ejecutar.</p>
 *
 * @author Jorge Enciso
 */
@Entity
@Table(name = "barrio", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"codigo", "zona"})})
@SequenceGenerator(name = "BARRIO_SEQ", allocationSize = 1, sequenceName = "BARRIO_SEQ")
public class Barrio extends DataRow {

    /**
     * Campo estático que se llama igual que una columna de la clave única: no
     * mapea nada y el resolutor lo tiene que ignorar.
     */
    public static final String zona = "URBANA";

    @Id
    @Basic(optional = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "BARRIO_SEQ")
    @Column(name = "idbarrio")
    private Long idbarrio;

    @Column(name = "codigo")
    private String codigo;

    @Column(name = "nombre")
    private String nombre;

    public Barrio() {
    }

    public Long getIdbarrio() {
        return idbarrio;
    }

    public void setIdbarrio(Long idbarrio) {
        this.idbarrio = idbarrio;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}

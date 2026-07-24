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
package org.javabeanstack.model;

import java.io.Serializable;
import org.javabeanstack.data.IDataRow;

/**
 * Contrato de la entidad relación entre tablas: describe la relación (join)
 * entre una entidad con su clave primaria y otra con su clave foránea, incluido
 * el tipo de join. Extiende {@link IDataRow}.
 *
 * @author Jorge Enciso
 */
public interface IAppTablesRelation extends IDataRow, Serializable {
    /** Tipo de join: interno (inner join). */
    public static final short INNER = 0;
    /** Tipo de join: externo por izquierda (left join). */
    public static final short LEFT  = 1;
    /** Tipo de join: externo por derecha (right join). */
    public static final short RIGHT = 2;
    /** Tipo de join: externo completo (full join). */
    public static final short FULL  = 3;

    /**
     * Devuelve la entidad de la clave primaria.
     * @return entidad de la clave primaria.
     */
    String getEntityPK();

    /**
     * Devuelve los campos de la clave primaria.
     * @return campos de la clave primaria.
     */
    String getFieldsPK();

    /**
     * Devuelve la entidad de la clave foránea.
     * @return entidad de la clave foránea.
     */
    String getEntityFK();

    /**
     * Devuelve los campos de la clave foránea.
     * @return campos de la clave foránea.
     */
    String getFieldsFK();

    /**
     * Indica si la relación se incluye en las consultas.
     * @return verdadero si se incluye, falso si no.
     */
    boolean isIncluded();

    /**
     * Devuelve el tipo de relación (join): {@link #INNER}, {@link #LEFT},
     * {@link #RIGHT} o {@link #FULL}.
     * @return tipo de relación.
     */
    short getRelationType();

    /**
     * Asigna la entidad de la clave primaria.
     * @param entity entidad de la clave primaria.
     */
    void setEntityPK(String entity);

    /**
     * Asigna los campos de la clave primaria.
     * @param fields campos de la clave primaria.
     */
    void setFieldsPK(String fields);

    /**
     * Asigna la entidad de la clave foránea.
     * @param entity entidad de la clave foránea.
     */
    void setEntityFK(String entity);

    /**
     * Asigna los campos de la clave foránea.
     * @param fields campos de la clave foránea.
     */
    void setFieldsFK(String fields);

    /**
     * Asigna si la relación se incluye en las consultas.
     * @param included verdadero para incluirla.
     */
    void setIncluded(boolean included);

    /**
     * Asigna el tipo de relación (join).
     * @param type tipo de relación.
     */
    void setRelationType(short type);
}

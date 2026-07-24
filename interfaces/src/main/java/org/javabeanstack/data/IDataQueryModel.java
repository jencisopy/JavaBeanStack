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
package org.javabeanstack.data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;



/**
 * Contrato que facilita el acceso a la información resultante de una consulta
 * nativa ({@link IDataNativeQuery}): expone las columnas de una fila por nombre
 * o por índice, con conversores a los tipos habituales.
 *
 * @author Jorge Enciso
 */
public interface IDataQueryModel {
    /**
     * Devuelve el valor de una columna por su nombre.
     *
     * @param columnName nombre de la columna.
     * @return valor de la columna.
     */
    public Object getValue(String columnName);

    /**
     * Devuelve el valor de una columna por su índice.
     *
     * @param index índice de la columna.
     * @return valor de la columna.
     */
    public Object getValue(int index);

    /**
     * Devuelve el valor de la columna identificadora.
     *
     * @return valor de la columna identificadora.
     */
    public Object getColumnId();

    /**
     * Devuelve el valor de una columna por su índice.
     *
     * @param index índice de la columna.
     * @return valor de la columna.
     */
    public Object getColumn(int index);

    /**
     * Devuelve el valor de una columna por su nombre.
     *
     * @param columnName nombre de la columna.
     * @return valor de la columna.
     */
    public Object getColumn(String columnName);

    /**
     * Devuelve el valor de una columna como cadena.
     *
     * @param columnName nombre de la columna.
     * @return valor de la columna como texto.
     */
    public String getColumnStr(String columnName);

    /**
     * Devuelve el valor de una columna como cadena.
     *
     * @param index índice de la columna.
     * @return valor de la columna como texto.
     */
    public String getColumnStr(int index);

    /**
     * Devuelve el valor de una columna como número decimal.
     *
     * @param columnName nombre de la columna.
     * @return valor de la columna como {@link BigDecimal}.
     */
    public BigDecimal getColumnNumber(String columnName);

    /**
     * Devuelve el valor de una columna como número decimal.
     *
     * @param index índice de la columna.
     * @return valor de la columna como {@link BigDecimal}.
     */
    public BigDecimal getColumnNumber(int index);

    /**
     * Devuelve el valor de una columna como {@link Long}.
     *
     * @param columnName nombre de la columna.
     * @return valor de la columna como {@link Long}.
     */
    public Long getColumnLong(String columnName);

    /**
     * Devuelve el valor de una columna como {@link Long}.
     *
     * @param index índice de la columna.
     * @return valor de la columna como {@link Long}.
     */
    public Long getColumnLong(int index);

    /**
     * Devuelve el valor de una columna como {@link Integer}.
     *
     * @param columnName nombre de la columna.
     * @return valor de la columna como {@link Integer}.
     */
    public Integer getColumnInt(String columnName);

    /**
     * Devuelve el valor de una columna como {@link Integer}.
     *
     * @param index índice de la columna.
     * @return valor de la columna como {@link Integer}.
     */
    public Integer getColumnInt(int index);

    /**
     * Devuelve el valor de una columna como {@link LocalDateTime}.
     *
     * @param columnName nombre de la columna.
     * @return valor de la columna como {@link LocalDateTime}.
     */
    public LocalDateTime getColumnLocalDate(String columnName);

    /**
     * Devuelve el valor de una columna como {@link LocalDateTime}.
     *
     * @param index índice de la columna.
     * @return valor de la columna como {@link LocalDateTime}.
     */
    public LocalDateTime getColumnLocalDate(int index);

    /**
     * Devuelve el nombre de la columna ubicada en el índice indicado.
     *
     * @param index índice de la columna.
     * @return nombre de la columna.
     */
    public String getColumnName(int index);

    /**
     * Devuelve la lista de nombres de columna de la fila.
     *
     * @return arreglo con los nombres de columna.
     */
    public String[] getColumnList();

    /**
     * Asigna el índice de la columna identificadora.
     *
     * @param index índice de la columna identificadora.
     */
    public void setColumnId(int index);

    /**
     * Asigna la lista de nombres de columna de la fila.
     *
     * @param columnList arreglo con los nombres de columna.
     */
    public void setColumnList(String[] columnList);

    /**
     * Devuelve la fila cruda (arreglo de valores) subyacente.
     *
     * @return fila subyacente.
     */
    public Object getRow();

    /**
     * Asigna la fila cruda (arreglo de valores) subyacente.
     *
     * @param row fila subyacente.
     */
    public void setRow(Object row);

    /**
     * Asigna el valor de una columna por su índice.
     *
     * @param index índice de la columna.
     * @param value valor a asignar.
     */
    public void setColumn(int index, Object value);

    /**
     * Asigna el valor de una columna por su nombre.
     *
     * @param columnName nombre de la columna.
     * @param value valor a asignar.
     */
    public void setColumn(String columnName, Object value);

    /**
     * Asigna el valor de una columna por su índice.
     *
     * @param index índice de la columna.
     * @param value valor a asignar.
     */
    public void setValue(int index, Object value);

    /**
     * Asigna el valor de una columna por su nombre.
     *
     * @param columnName nombre de la columna.
     * @param value valor a asignar.
     */
    public void setValue(String columnName, Object value);

    /**
     * Devuelve las propiedades libres asociadas a la fila.
     *
     * @return mapa de propiedades.
     */
    public Map<String, Object> getProperties();

    /**
     * Asigna las propiedades libres asociadas a la fila.
     *
     * @param properties mapa de propiedades.
     */
    public void setProperties(Map<String, Object> properties);

    /**
     * Devuelve el valor de una propiedad libre por su clave.
     *
     * @param key clave de la propiedad.
     * @return valor de la propiedad, o {@code null} si no existe.
     */
    public Object getProperty(String key);

    /**
     * Agrega o reemplaza una propiedad libre.
     *
     * @param key clave de la propiedad.
     * @param value valor de la propiedad.
     */
    public void setProperty(String key, Object value);

    /**
     * Indica si existe una columna con el nombre indicado.
     *
     * @param columnName nombre de la columna.
     * @return verdadero si existe, falso si no.
     */
    public boolean isColumnExist(String columnName);

}

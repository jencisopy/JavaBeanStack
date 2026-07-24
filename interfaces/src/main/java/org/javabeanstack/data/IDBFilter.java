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

import java.io.Serializable;
import java.util.List;


/**
 * Contrato de un filtro dinámico de datos: acumula elementos de filtro
 * ({@link IDBFilterElement}) y genera la expresión de condición (JPQL o SQL
 * nativo) que la capa de datos aplica en la selección de registros.
 *
 * <p>Los elementos pueden repartirse en grupos y las expresiones admiten un
 * alias de tabla. Suele derivarse del contexto de sesión/empresa (ver
 * {@link IDBLinkInfo}).</p>
 *
 * @author Jorge Enciso
 * @param <E> tipo del elemento de filtro.
 */
public interface IDBFilter<E extends IDBFilterElement> extends Serializable {
    /**
     * Agrega un elemento de filtro (campo, valor y grupo).
     *
     * @param fieldName nombre del campo.
     * @param fieldValue valor a comparar.
     * @param group grupo al que pertenece el elemento.
     */
    void addFilter(String fieldName, Object fieldValue, Integer group);

    /**
     * Devuelve la lista de elementos de filtro acumulados.
     *
     * @return lista de elementos de filtro.
     */
    List<E> getFilter();

    /**
     * Devuelve la expresión de condición del elemento indicado.
     *
     * @param element índice del elemento.
     * @return expresión de condición.
     */
    String getFilterExpr(Integer element);

    /**
     * Devuelve la expresión de condición del elemento indicado dentro de un grupo.
     *
     * @param element índice del elemento.
     * @param group grupo del elemento.
     * @return expresión de condición.
     */
    String getFilterExpr(Integer element, Integer group);

    /**
     * Devuelve la expresión de condición del elemento indicado, con alias de tabla.
     *
     * @param element índice del elemento.
     * @param alias alias de la tabla/entidad.
     * @return expresión de condición.
     */
    String getFilterExpr(Integer element, String alias);

    /**
     * Devuelve la expresión de condición del elemento dentro de un grupo, con alias.
     *
     * @param element índice del elemento.
     * @param group grupo del elemento.
     * @param alias alias de la tabla/entidad.
     * @return expresión de condición.
     */
    String getFilterExpr(Integer element, Integer group, String alias);

    /**
     * Construye la expresión de filtro aplicable a una entidad, con alias.
     *
     * @param <T> tipo de la entidad.
     * @param clazz clase de la entidad.
     * @param alias alias de la tabla/entidad.
     * @return expresión de condición.
     */
    <T extends IDataRow> String getFilterExpr(Class<T> clazz, String alias);

    /**
     * Construye la expresión de filtro aplicable a una entidad, eligiendo entre
     * JPQL y SQL nativo.
     *
     * @param <T> tipo de la entidad.
     * @param clazz clase de la entidad.
     * @param alias alias de la tabla/entidad.
     * @param jpqlSentence verdadero para generar JPQL, falso para SQL nativo.
     * @return expresión de condición.
     */
    <T extends IDataRow> String getFilterExpr(Class<T> clazz, String alias, boolean jpqlSentence);

    /**
     * Devuelve la expresión combinada de todos los elementos de filtro.
     *
     * @return expresión de condición completa.
     */
    String getAllFilterExpr();

    /**
     * Devuelve la expresión combinada de todos los elementos, con alias.
     *
     * @param alias alias de la tabla/entidad.
     * @return expresión de condición completa.
     */
    String getAllFilterExpr(String alias);

    /**
     * Devuelve la expresión combinada de los elementos de un grupo.
     *
     * @param group grupo a evaluar.
     * @return expresión de condición del grupo.
     */
    String getAllFilterExpr(Integer group);

    /**
     * Devuelve la expresión combinada de los elementos de un grupo, con alias.
     *
     * @param group grupo a evaluar.
     * @param alias alias de la tabla/entidad.
     * @return expresión de condición del grupo.
     */
    String getAllFilterExpr(Integer group, String alias);

    /**
     * Devuelve la ruta del paquete de los modelos usada para resolver entidades.
     *
     * @return ruta del paquete de modelos.
     */
    String getModelPackagePath();

    /**
     * Asigna la ruta del paquete de los modelos usada para resolver entidades.
     *
     * @param modelPath ruta del paquete de modelos.
     */
    void setModelPackagePath(String modelPath);
}

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
package org.javabeanstack.web.rest.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Envoltorio de los listados paginados que devuelven los recursos REST:
 * {@code {"total": <cantidad>, "data": [...]}}.
 *
 * <p>{@code total} es la cantidad de registros que devolvería la consulta
 * <b>sin</b> paginar (con los filtros aplicados), no la cantidad de elementos de
 * {@code data}: es el dato que necesita el paginador del cliente para saber
 * cuántas páginas hay.</p>
 *
 * <p>Es un bean simple —constructor sin argumentos y getters/setters— para que
 * el proveedor JSON del servidor pueda serializarlo sin configuración
 * adicional.</p>
 *
 * @author Jorge Enciso
 * @param <T> tipo de los elementos del listado.
 */
public class PagedList<T> {

    private long total;
    private List<T> data = new ArrayList<>();

    /**
     * Construye un listado vacío.
     */
    public PagedList() {
    }

    /**
     * Construye el listado con su total y sus elementos.
     *
     * @param total cantidad total de registros sin paginar.
     * @param data elementos de la página solicitada.
     */
    public PagedList(long total, List<T> data) {
        this.total = total;
        setData(data);
    }

    /**
     * Devuelve la cantidad total de registros sin paginar.
     *
     * @return cantidad total de registros.
     */
    public long getTotal() {
        return total;
    }

    /**
     * Asigna la cantidad total de registros sin paginar.
     *
     * @param total cantidad total de registros.
     */
    public void setTotal(long total) {
        this.total = total;
    }

    /**
     * Devuelve los elementos de la página solicitada.
     *
     * <p>Se devuelve una copia: la lista interna no se expone para que nadie
     * altere la respuesta ya armada a través del getter.</p>
     *
     * @return elementos de la página (nunca nulo).
     */
    public List<T> getData() {
        return new ArrayList<>(data);
    }

    /**
     * Asigna los elementos de la página solicitada; se guarda una copia. Una
     * lista nula se guarda como lista vacía: el contrato dice que {@code data}
     * siempre es un arreglo.
     *
     * @param data elementos de la página.
     */
    public final void setData(List<T> data) {
        this.data = (data == null) ? new ArrayList<>() : new ArrayList<>(data);
    }
}

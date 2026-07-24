/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*        jenciso@javabeanstack.org
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
package org.javabeanstack.events;

import java.io.Serializable;
import java.util.List;
import jakarta.faces.event.AjaxBehaviorEvent;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.datactrl.IDataObject;

/**
 * Contrato de los eventos de interfaz (JSF) de un controlador sobre un objeto de
 * datos ({@link IDataObject}): responde a la interacción del usuario en la vista
 * (selección de fila, filtros, cambios de campo, autocompletado).
 *
 * @author Jorge Enciso
 * @param <T> tipo del objeto de datos (contexto).
 */
public interface ICtrlEvents<T extends IDataObject> extends Serializable{
    /**
     * Devuelve el objeto de datos (contexto) del controlador.
     *
     * @return objeto de datos.
     */
    T getContext();

    /**
     * Devuelve el registro actual del contexto.
     *
     * @param <X> tipo del registro.
     * @return registro actual.
     */
    <X extends IDataRow> X getRow();

    /**
     * Asigna el objeto de datos (contexto) del controlador.
     *
     * @param context objeto de datos.
     */
    void setContext(T context);

    /**
     * Se ejecuta al seleccionar una fila en la vista.
     *
     * @param event evento de selección.
     */
    void onRowSelect(Object event);

    /**
     * Se ejecuta al aplicar un filtro sobre la vista.
     */
    void onRowFilter();

    /**
     * Se ejecuta al cambiar el valor de un campo en la vista.
     *
     * @param fieldname nombre del campo.
     */
    void onChange(String fieldname);

    /**
     * Se ejecuta al seleccionar un ítem de una lista/autocompletado.
     *
     * @param fieldName nombre del campo.
     */
    void onItemSelect(String fieldName);

    /**
     * Se ejecuta al perder el foco un componente (evento Ajax).
     *
     * @param event evento Ajax de pérdida de foco.
     */
    void onBlur(AjaxBehaviorEvent event);

    /**
     * Se ejecuta al perder el foco un campo indicado por nombre.
     *
     * @param fieldName nombre del campo.
     */
    void onBlur(String fieldName);

    /**
     * Provee las sugerencias de autocompletado para un texto.
     *
     * @param <X> tipo de los registros sugeridos.
     * @param text texto ingresado.
     * @return lista de registros que coinciden con el texto.
     */
    <X extends IDataRow> List<X> onCompleteText(String text);
}

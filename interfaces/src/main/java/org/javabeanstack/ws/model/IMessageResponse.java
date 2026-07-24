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
package org.javabeanstack.ws.model;

/**
 * Contrato del mensaje de respuesta de un servicio web: identifica el recurso
 * creado/afectado y su enlace (link), para devolverlo en la respuesta REST.
 *
 * @author Jorge Enciso
 */
public interface IMessageResponse {

    /**
     * Devuelve el identificador del recurso.
     * @return identificador del recurso.
     */
    String getId();

    /**
     * Devuelve el enlace al recurso.
     * @return enlace al recurso.
     */
    String getLink();

    /**
     * Asigna el identificador del recurso.
     * @param id identificador del recurso.
     */
    void setId(String id);

    /**
     * Asigna el enlace al recurso.
     * @param link enlace al recurso.
     */
    void setLink(String link);

}

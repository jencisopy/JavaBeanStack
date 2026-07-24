/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2018 - 2027 Jorge Enciso
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

import jakarta.xml.bind.annotation.XmlRootElement;
import org.javabeanstack.ws.model.IMessageResponse;

/**
 *
 * Modelo de respuesta REST: implementación de
 * {@link org.javabeanstack.ws.model.IMessageResponse} que agrega un mensaje.
 *
 * @author Jorge Enciso
 */
@XmlRootElement
public class MessageResponse implements IMessageResponse {
    private String id;
    private String link;    
    private String message;

    /**
     * Devuelve el identificador del recurso.
     * @return identificador del recurso.
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Asigna el identificador del recurso.
     * @param id identificador del recurso.
     */
    @Override
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Devuelve el enlace al recurso.
     * @return enlace al recurso.
     */
    @Override
    public String getLink() {
        return link;
    }

    /**
     * Asigna el enlace al recurso.
     * @param link enlace al recurso.
     */
    @Override
    public void setLink(String link) {
        this.link = link;
    }

    /**
     * Devuelve el mensaje de la respuesta.
     *
     * @return mensaje.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Asigna el mensaje de la respuesta.
     *
     * @param message mensaje.
     */
    public void setMessage(String message) {
        this.message = message;
    }
}



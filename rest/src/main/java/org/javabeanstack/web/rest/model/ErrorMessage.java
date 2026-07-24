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

/**
 *
 * Modelo del mensaje de error de una respuesta REST: código, mensaje y enlace
 * a la documentación del error.
 *
 * @author Jorge Enciso
 */
@XmlRootElement
public class ErrorMessage {
    private String errorMessage;
    private int    errorCode;
    private String documentation;

    /**
     * Devuelve el mensaje del error.
     *
     * @return mensaje del error.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Asigna el mensaje del error.
     *
     * @param errorMessage mensaje del error.
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Devuelve el código del error.
     *
     * @return código del error.
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Asigna el código del error.
     *
     * @param errorCode código del error.
     */
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Devuelve el enlace a la documentación del error.
     *
     * @return enlace a la documentación.
     */
    public String getDocumentation() {
        return documentation;
    }

    /**
     * Asigna el enlace a la documentación del error.
     *
     * @param documentation enlace a la documentación.
     */
    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }
    
}

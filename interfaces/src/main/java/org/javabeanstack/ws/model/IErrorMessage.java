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
 * Contrato del mensaje de error de un servicio web: código, mensaje y enlace a
 * la documentación del error, para devolverlo en la respuesta REST.
 *
 * @author Jorge Enciso
 */
public interface IErrorMessage {

    /**
     * Devuelve el enlace a la documentación del error.
     * @return enlace a la documentación.
     */
    String getDocumentation();

    /**
     * Devuelve el código del error.
     * @return código del error.
     */
    int getErrorCode();

    /**
     * Devuelve el mensaje del error.
     * @return mensaje del error.
     */
    String getErrorMessage();

    /**
     * Asigna el enlace a la documentación del error.
     * @param documentation enlace a la documentación.
     */
    void setDocumentation(String documentation);

    /**
     * Asigna el código del error.
     * @param errorCode código del error.
     */
    void setErrorCode(int errorCode);

    /**
     * Asigna el mensaje del error.
     * @param errorMessage mensaje del error.
     */
    void setErrorMessage(String errorMessage);

}

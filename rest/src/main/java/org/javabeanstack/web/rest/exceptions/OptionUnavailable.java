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
package org.javabeanstack.web.rest.exceptions;

/**
 *
 * Excepción REST que señala que la opción u operación solicitada no está
 * disponible.
 *
 * @author Jorge Enciso
 */
public class OptionUnavailable extends RuntimeException {
    /**
     * Crea la excepción sin mensaje de detalle.
     */
    public OptionUnavailable(){
        super();
    }
    
    /**
     * Crea la excepción con el mensaje indicado.
     *
     * @param message mensaje de detalle.
     */
    public OptionUnavailable(String message){
        super(message);
    }

    /**
     * Crea la excepción con el mensaje y la causa indicados.
     *
     * @param message mensaje de detalle.
     * @param cause causa original.
     */
    public OptionUnavailable(String message, Throwable cause){
        super(message, cause);
    }

    /**
     * Crea la excepción con la causa indicada.
     *
     * @param cause causa original.
     */
    public OptionUnavailable(Throwable cause){
        super(cause);
    }
}

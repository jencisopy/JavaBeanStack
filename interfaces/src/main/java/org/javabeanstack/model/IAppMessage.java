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

import org.javabeanstack.data.IDataRow;

/**
 * Contrato de la entidad mensaje del catálogo de mensajes de la aplicación:
 * asocia un número de mensaje con su texto y explicación. La usan el manejo de
 * errores y el log ({@link org.javabeanstack.log.ILogManager}). Extiende {@link IDataRow}.
 *
 * @author Jorge Enciso
 */
public interface IAppMessage extends IDataRow {
    /**
     * Devuelve el identificador del mensaje.
     * @return identificador del mensaje.
     */
    Long getIdmessage();

    /**
     * Devuelve el número de mensaje (clave de negocio del catálogo).
     * @return número de mensaje.
     */
    Long getNumber();

    /**
     * Devuelve el texto del mensaje.
     * @return texto del mensaje.
     */
    String getText();

    /**
     * Devuelve la explicación ampliada del mensaje.
     * @return explicación del mensaje.
     */
    String getExplanation();
}

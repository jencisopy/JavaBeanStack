/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 Jorge Enciso
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
package org.javabeanstack.data.events;

import org.javabeanstack.data.*;
import java.io.Serializable;


/**
 * Contiene metodos para responder a ciertos eventos del AbstractDAO
 *
 * @author Jorge Enciso
 */
public interface IDAOEvents extends Serializable {
    /**
     * Se ejecuta antes del metodo que sincroniza un ejb con la base de datos.
     *
     * @param <T>
     * @param sessionId identificador de la sesión del usuario
     * @param ejb objeto mapeado a un registro de una tabla.
     *
     */
    public <T extends IDataRow> void beforeSave(String sessionId, T ejb);
    
    /**
     * Se ejecuta despues del metodo que sincroniza un ejb con la base de datos.
     *
     * @param <T>
     * @param sessionId identificador de la sesión del usuario
     * @param ejb objeto mapeado a un registro de una tabla.
     *
     */
    public  <T extends IDataRow> void afterSave(String sessionId, T ejb);
}

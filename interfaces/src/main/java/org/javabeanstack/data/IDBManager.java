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
package org.javabeanstack.data;

import java.io.Serializable;
import jakarta.persistence.EntityManager;

/**
 * Contiene metodos para gestionar el acceso a los datos, es utilizado
 *  por GenericDAO 
 * 
 * @author Jorge Enciso
 */
public interface IDBManager extends Serializable{
    public static final String CATALOGO = "PU1";
    public static final int PERTHREAD = 1;    
    public static final int PERSESSION = 2;

    /**
     * Devuelve 1 si la creación de los entity manager serán por thread
     * o 2 por sesión de usuario.
     * @return 1 por thread, 2 por sesión.
     */
    public int getEntityIdStrategic();

    /**
     * Devuelve un entityManager, lo crea si no existe en la unidad de persistencia solicitada
     * @param key
     * @return Devuelve un entityManager
     */
    public EntityManager getEntityManager(String key);
    /**
     * Crea un entitymanager dentro de un Map utiliza la unidad de persistencia y el 
     *    threadid como clave
     * @param key
     * @return          el entity manager creado.
     */
    public EntityManager createEntityManager(String key);
    /**
     *  Ejecuta rollback de una transacción 
     */
    public void rollBack();
}

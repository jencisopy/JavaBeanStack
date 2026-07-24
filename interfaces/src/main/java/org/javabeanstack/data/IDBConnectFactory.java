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
package org.javabeanstack.data;

import java.sql.Connection;
import jakarta.persistence.EntityManager;

/**
 * Contrato de una fábrica que obtiene la conexión JDBC ({@link Connection})
 * subyacente a un {@link EntityManager} de JPA.
 *
 * <p>Permite ejecutar SQL nativo o utilidades que requieren la conexión directa
 * sin abandonar la unidad de persistencia gestionada por el proveedor.</p>
 *
 * @author Jorge Enciso
 */
public interface IDBConnectFactory {
    /**
     * Devuelve la conexión JDBC subyacente al {@link EntityManager} indicado.
     *
     * @param em entity manager del que obtener la conexión.
     * @return conexión JDBC.
     */
    public Connection getConnection(EntityManager em);
}

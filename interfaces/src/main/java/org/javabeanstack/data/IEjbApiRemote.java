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

import java.io.Serializable;
import javax.naming.Context;
import javax.naming.NamingException;
import org.javabeanstack.security.ISecManager;

/**
 * Contrato de acceso a los EJB remotos del framework mediante lookup JNDI.
 *
 * <p>Provee el contexto de nombres y métodos de conveniencia para localizar los
 * componentes remotos principales: el DAO genérico ({@link IGenericDAO}), la
 * conexión de datos ({@link IDataLink}) y el gestor de seguridad
 * ({@link ISecManager}).</p>
 *
 * @author Jorge Enciso
 */
public interface IEjbApiRemote extends Serializable {
    /**
     * Devuelve el contexto de nombres (JNDI) hacia el servidor de EJB.
     *
     * @return contexto de nombres.
     * @throws NamingException si no se puede crear el contexto.
     */
    public Context getContext()       throws NamingException;

    /**
     * Localiza un componente remoto por su ruta JNDI.
     *
     * @param <T> tipo del componente esperado.
     * @param path ruta JNDI del componente.
     * @return referencia al componente remoto.
     * @throws NamingException si la ruta no se puede resolver.
     */
    public <T> T lookup(String path) throws NamingException;

    /**
     * Localiza el DAO genérico remoto.
     *
     * @return DAO genérico remoto.
     * @throws NamingException si no se puede resolver.
     */
    public IGenericDAO lookupDAO()    throws NamingException;

    /**
     * Localiza la conexión de datos remota.
     *
     * @return conexión de datos remota.
     * @throws NamingException si no se puede resolver.
     */
    public IDataLink lookupDataLink() throws NamingException;

    /**
     * Localiza el gestor de seguridad remoto.
     *
     * @return gestor de seguridad remoto.
     * @throws NamingException si no se puede resolver.
     */
    public ISecManager lookupSecManager() throws NamingException;
}

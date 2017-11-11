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

import org.javabeanstack.security.IUserSession;

/**
 * Clase que contiene información necesaria para el acceso a los datos. 
 * ej. unidad de persistencia, sesión id del usuario etc.
 * 
 * @author Jorge Enciso
 */
public class DBLinkInfo implements IDBLinkInfo {
    private IUserSession userSession;
    
    /**
     * Devuelve DBFilter conteniendo los filtros que deben ser aplicados
     * en los queries.
     * @return DBFilter.
     */
    @Override
    public IDBFilter getDBFilter() {
        if (userSession != null){
            return userSession.getDBFilter();
        }
        return null;
    }

    /**
     * 
     * @return unidad de persistencia.
     */
    @Override
    public String getPersistUnit() {
        if (userSession == null){
            return IDBManager.CATALOGO;
        }
        return userSession.getPersistenceUnit();
    }

    /**
     * 
     * @return objeto con información de la sesión del usuario.
     */
    @Override
    public IUserSession getUserSession() {
        return userSession;
    }

    /**
     * Asigna un objeto con información de la sesión del usuario.
     * @param userSession 
     */
    @Override
    public void setUserSession(IUserSession userSession) {
        this.userSession = userSession;
    }
}

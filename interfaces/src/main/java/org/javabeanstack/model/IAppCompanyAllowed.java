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

import java.io.Serializable;
import org.javabeanstack.data.IDataRow;

/**
 *
 * @author Jorge Enciso
 */
public interface IAppCompanyAllowed extends IDataRow, Serializable {
    Long getIdcompany();
    Long getIduser();
    boolean getDeny();
    boolean getAllow();
    String getAppuser();
    
    void setIdcompany(Long idempresa);
    void setIduser(Long idusuario);
    void setDeny(boolean negar);
    void setAllow(boolean permitir);
    void setAppuser(String appuser);
}

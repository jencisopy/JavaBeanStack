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
package org.javabeanstack.security;

import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.model.IAppUser;

/**
 *
 * @author Jorge Enciso
 */
public interface IAppUserPwdLogSrv extends IDataService {
    boolean isExistUserPwdLog(String sessionId);
    boolean isExistUserPwdLog(IAppUser appUser);
    void insertUserPwdLog(String sessionId);
    void insertUserPwdLog(IAppUser appUser);
    Long getIdUserFromPwdLog(String pwd);
}

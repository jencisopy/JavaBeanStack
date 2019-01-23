/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2018 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*        jenciso@javabeanstack.org
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
 *
 * @author Jorge Enciso
 */
public interface IAppAuthConsumerToken extends IDataRow {
    String getUuidDevice();
    void setUuidDevice(String uuidDevice);
    
    String getToken();
    void setToken(String tokenKey);

    String getTokenSecret();
    void setTokenSecret(String tokenSecret);

    String getData();
    void setData(String data);
    
    Boolean getBlocked();
    void setBlocked(boolean blocked);
    
    IAppAuthConsumer getAppAuthConsumerKey();
    void setAppAuthConsumerKey(IAppAuthConsumer authConsumer);
}

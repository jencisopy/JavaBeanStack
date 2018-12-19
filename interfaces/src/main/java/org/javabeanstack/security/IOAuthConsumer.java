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
package org.javabeanstack.security;

import java.util.Date;
import org.javabeanstack.data.IDBFilter;
import org.javabeanstack.model.IAppAuthConsumer;
import org.javabeanstack.model.IAppAuthConsumerToken;
import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.model.IAppUser;

/**
 *
 * @author Jorge Enciso
 */
public interface IOAuthConsumer {
    String createAuthConsumer(String consumerName, Date expiredDate);
    String createToken(String consumerKey, IOAuthConsumerData data);
    String createToken(String consumerKey, IOAuthConsumerData data, String uuidDevice);
    IAppAuthConsumerToken findAuthToken(String token);
    boolean dropAuthConsumer(String consumerKey);
    boolean dropToken(String consumerKey, String tokenSecret);
    Class<IAppAuthConsumer> getAuthConsumerClass();
    Class<IAppAuthConsumerToken> getAuthConsumerTokenClass();
    String getToken(String consumerKey, String uuidOrTokenSecret);
    Date getTokenExpiredDate(String consumerKey, String uuidOrTokenSecret);
    boolean requestToken(String consumerKey);
    boolean requestToken(String consumerKey, String uuidDevice);    
    boolean isValidToken(String token);
    String getDataKeyValue(String token, String property);
    String getDataKeyValue(IAppAuthConsumerToken token, String property);
    IAppUser getUserMapped(IAppAuthConsumerToken token);
    IAppUser getUserMapped(String token);
    IAppCompany getCompanyMapped(IAppAuthConsumerToken token);
    IAppCompany getCompanyMapped(String token);
    IDBFilter getDBFilter(IAppAuthConsumerToken token);
    void setDao(IDataService dao);
}

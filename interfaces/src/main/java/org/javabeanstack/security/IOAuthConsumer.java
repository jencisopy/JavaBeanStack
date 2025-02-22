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

import java.time.LocalDateTime;
import java.util.List;
import org.javabeanstack.data.IDBFilter;
import org.javabeanstack.model.IAppAuthConsumer;
import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.model.IAppUser;
import org.javabeanstack.model.IAppAuthConsumerToken;

/**
 *
 * @author Jorge Enciso
 */
public interface IOAuthConsumer {
    String createAuthConsumer(String consumerName, LocalDateTime expiredDate);
    IAppAuthConsumer createAuthConsumer(IAppAuthConsumer authConsumer);
    String createToken(String consumerKey, IOAuthConsumerData data)  throws Exception ;
    String createToken(String consumerKey, IOAuthConsumerData data, String uuidDevice)  throws Exception;
    String createToken(String consumerKey, IOAuthConsumerData data, String uuidDevice, String userName, String userEmail)  throws Exception;    
    String createToken(IAppAuthConsumerToken authConsumerToken) throws Exception;
    IAppAuthConsumer findAuthConsumer(String consumerKey);
    IAppAuthConsumerToken findAuthToken(String token);
    IAppAuthConsumerToken findAuthToken(String consumerKey, String uuidOrTokenSecret);
    boolean dropAuthConsumer(String consumerKey);
    boolean dropToken(String consumerKey, String uuidOrTokenSecret);
    boolean changeTokenStatus(String consumerKey, String uuidOrTokenSecret, String status);
    Class<IAppAuthConsumer> getAuthConsumerClass();
    Class<IAppAuthConsumerToken> getAuthConsumerTokenClass();
    String getToken(String consumerKey, String uuidOrTokenSecret);
    LocalDateTime getTokenExpiredDate(String consumerKey, String uuidOrTokenSecret);
    String getTokenAuthUrl(String consumerKey, String uuidOrTokenSecret);
    String getTokenCallbackUrl(String consumerKey, String uuidOrTokenSecret);
    boolean requestToken(String consumerKey);
    boolean requestToken(String consumerKey, String uuidDevice);    
    boolean requestToken(String consumerKey, String uuidDevice, String userName, String userEmail);    
    boolean isValidToken(String token);
    boolean isValidToken(IAppAuthConsumerToken authToken);    
    boolean isValidToken(String token, boolean noCheckCredentials);
    boolean isValidToken(IAppAuthConsumerToken authToken, boolean noCheckCredentials);

    IErrorReg checkToken(String token);
    IErrorReg checkToken(IAppAuthConsumerToken authToken);    
    IErrorReg checkToken(String token, boolean noCheckCredentials);
    IErrorReg checkToken(IAppAuthConsumerToken authToken, boolean noCheckCredentials);
    
    String getDataKeyValue(String token, String property);
    String getDataKeyValue(IAppAuthConsumerToken token, String property);
    IAppUser getUserMapped(IAppAuthConsumerToken token);
    IAppUser getUserMapped(String token);
    IAppCompany getCompanyMapped(IAppAuthConsumerToken token);
    IAppCompany getCompanyMapped(String token);
    List<IAppCompany> getCompaniesAllowed(String userLogin);
    IDBFilter getDBFilter(IAppAuthConsumerToken token);
    boolean checkAuthConsumerData(IOAuthConsumerData data);
    void setDao(IDataService dao);
}

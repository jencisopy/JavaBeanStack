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
import java.util.Date;
import org.javabeanstack.data.IDataRow;
/**
 *
 * @author Jorge Enciso
 */
public interface IAppAuthConsumer extends IDataRow {
    String getConsumerKey();
    void setConsumerKey(String consumerKey);
    
    String getConsumerName();
    void setConsumerName(String consumerName);

    Date getExpiredDate();
    void setExpiredDate(Date expiredDate);
    
    String getPublicKey();
    void setPublicKey(String publicKey);
    
    String getPrivateKey();
    void setPrivateKey(String privateKey);
    
    String getSignatureAlgorithm();
    void setSignatureAlgorithm(String algorithm);

    String getCryptoAlgorithm();
    void setCryptoAlgorithm(String algorithm);
    
    Boolean getBlocked();
    void setBlocked(boolean blocked);
    
    String getAuthURL();
    void setAuthURL(String authURL);
    
    String getTokenURL();
    void setTokenURL(String tokenURL);
    
    String getCallbackURL();
    void setCallbackURL(String callbackURL);
    
    String getScope();
    void setScope(String scope);
}


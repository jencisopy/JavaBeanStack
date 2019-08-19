/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2018 Jorge Enciso
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
package org.javabeanstack.security.model;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.model.IAppAuthConsumerToken;
import static org.javabeanstack.util.Fn.nvl;
import org.javabeanstack.util.LocalDates;

/**
 *
 * @author Jorge Enciso
 */
public class ClientAuthRequestInfo implements IClientAuthRequestInfo{
    private static final Logger LOGGER = Logger.getLogger(ClientAuthRequestInfo.class);    
    
    private IAppAuthConsumerToken appAuthToken;
    private Long idcompany;
    private LocalDateTime logDate;
    private int times;

    public ClientAuthRequestInfo(){
        logDate = LocalDates.now();
    }
    
    @Override
    public String getToken() {
        if (appAuthToken == null){
            return null;
        }
        times++;
        return appAuthToken.getToken();
    }

    @Override
    public Long getIdcompany() {
        return idcompany;
    }

    @Override
    public void setIdcompany(Long idcompany) {
        this.idcompany = idcompany;
    }

    @Override
    public void setAppAuthToken(IAppAuthConsumerToken appAuthToken) {
        this.appAuthToken = appAuthToken;
    }

    @Override
    public LocalDateTime getLogDate() {
        return logDate;
    }

    @Override
    public int getTimes() {
        return times;
    }

    @Override
    public String getPropertyValue(String property) {
        try {
            IAppAuthConsumerToken tokenRecord = appAuthToken;
            Properties prop = new Properties();
            prop.load(new StringReader(tokenRecord.getData()));
            return nvl((String) prop.getProperty(property), "");
        } catch (IOException ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return "";
    }    
}

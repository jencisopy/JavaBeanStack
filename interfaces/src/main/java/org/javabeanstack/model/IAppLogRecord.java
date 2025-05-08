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
package org.javabeanstack.model;

import java.time.LocalDateTime;
import org.javabeanstack.data.IDataRow;

/**
 *
 * @author Jorge Enciso
 */
public interface IAppLogRecord extends IDataRow{
    public static final String EVENT_CREATESESSION = "CREATESESSION";
    public static final String EVENT_LOGIN = "LOGIN";
    public static final String EVENT_LOGOUT = "LOGOUT";
    public static final String EVENT_ERROR = "ERROR";
    public static final String EVENT_COMPANY_IN = "COMPANYIN";
    public static final String EVENT_LOAD_PAGE = "LOADPAGE";
    public static final String EVENT_UPDATEDB = "UPDATEDB";
    
    public static final String LEVEL_ERROR = "E";
    public static final String LEVEL_ALERT = "A";    
    public static final String LEVEL_INFO = "I";
    
    public static final String CATEGORY_APP = "A";
    public static final String CATEGORY_SECURITY = "S";
    public static final String CATEGORY_DATA = "D";    
    
    Long getIdlog();
    String getSessionId();
    Long getIdcompany();
    Long getIduser();    
    String getLevel();
    String getCategory();
    String getIpRequestFrom();
    String getEvent();
    LocalDateTime getLogTime();
    LocalDateTime getLogTimeOrigin();
    String getMessage();
    String getMessageInfo();        
    Integer getMessageNumber();    
    String getAppObject();
    String getWebPage();
    
    void setSessionId(String sessionId);
    void setIdlog(Long idlog);    
    void setIdcompany(Long idcompany);
    void setIduser(Long iduser);    
    void setEvent(String event);
    void setLevel(String level);    
    void setCategory(String category);    
    void setLogTime(LocalDateTime dateTime);
    void setLogTimeOrigin(LocalDateTime dateTimeOrigin);
    void setIpRequestFrom(String origin);
    void setMessage(String message);
    void setMessageInfo(String messageInfo);    
    void setMessageNumber(Integer messageNumber);    
    void setAppObject(String object);
    void setWebPage(String object);
}

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

import java.util.Date;
import org.javabeanstack.data.IDataRow;

/**
 *
 * @author Jorge Enciso
 */
public interface ILogRecord extends IDataRow{
    public static final String CATEGORY_APP = "A";
    public static final String CATEGORY_SECURITY = "S";
    public static final String CATEGORY_DATA = "D";
    public static final String LEVEL_ERROR = "E";
    public static final String LEVEL_ALERT = "A";    
    public static final String LEVEL_INFO = "I";
    
    Long getIdlog();
    String getSessionId();
    Long getIdempresa();
    Long getIdusuario();    
    String getLevel();
    String getOrigin();
    String getCategory();
    Date getLogTime();
    Date getLogTimeOrigin();
    String getMessage();
    String getMessageInfo();        
    Integer getMessageNumber();    
    Integer getErrorNumber();
    Integer getLineNumber();
    String getObject();
    String getObjectField();
    Integer getChoose();
    
    void setSessionId(String sessionId);
    void setIdlog(Long idlog);    
    void setIdempresa(Long idempresa);
    void setIdusuario(Long idusuario);    
    void setCategory(String category);
    void setLevel(String level);    
    void setLogTime(Date dateTime);
    void setLogTimeOrigin(Date dateTimeOrigin);
    void setOrigin(String origin);
    void setMessage(String message);
    void setMessageInfo(String messageInfo);    
    void setMessageNumber(Integer messageNumber);    
    void setLineNumber(Integer lineNumber);    
    void setErrorNumber(Integer errorNumber);
    void setObject(String object);
    void setObjectField(String objectField);
    void setChoose(Integer choose);
}

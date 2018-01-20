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
package org.javabeanstack.log;

import java.util.List;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.security.IUserSession;
import org.javabeanstack.model.IAppMessage;
import org.javabeanstack.model.IAppLogRecord;

/**
 *
 * @author Jorge Enciso
 */
public interface IAppLogManager {
    <T extends IAppLogRecord> boolean dbWrite(Class<T> logType, String sessionId, String message, String messageInfo, Integer messageNumber, String category, String type, String object, String objectField);    
    <T extends IAppLogRecord> boolean dbWrite(Class<T> logType, IUserSession userSession, String message, String messageInfo, Integer messageNumber, String category, String type, String object, String objectField);
    <T extends IAppLogRecord> boolean dbWrite(Class<T> logType, String sessionId, String message, String messageInfo, Integer messageNumber, String category, String type, String object, String objectField, Integer lineNumber, Integer choose);
    <T extends IAppLogRecord> boolean dbWrite(Class<T> logType, IUserSession userSession, String message, String mensajeInfo, Integer messageNumber, String category, String type, String object, String objectField, Integer lineNumber, Integer choose);
    <T extends IAppLogRecord> boolean dbWrite(Class<T> logType, IErrorReg errorReg, String sessionId);
    <T extends IAppLogRecord> boolean dbWrite(Class<T> logType, IErrorReg errorReg, IUserSession userSession);    
    <T extends IAppLogRecord> boolean dbWrite(Class<T> logType, Exception exp, String sessionId);
    <T extends IAppLogRecord> boolean dbWrite(Class<T> logType, Exception exp, IUserSession userSession);
    <T extends IAppLogRecord> boolean dbWrite(T logRecord, String sessionId);
    <T extends IAppLogRecord> boolean dbWrite(T logRecord, IUserSession userSession);    

    boolean fWrite(String message, String file, boolean flag);
    boolean logSend();
    
    IAppMessage getAppMessage(Integer msgNumber);
    List<IAppMessage> getAppMessages();    
}

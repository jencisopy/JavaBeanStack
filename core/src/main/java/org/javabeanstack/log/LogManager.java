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

import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import org.apache.log4j.Logger;
import org.javabeanstack.data.IDataResult;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.security.ISessionsLocal;
import org.javabeanstack.security.IUserSession;
import org.javabeanstack.util.Strings;
import org.javabeanstack.model.ILogRecord;
import org.javabeanstack.model.IAppMessages;
import org.javabeanstack.data.IDBManager;
import org.javabeanstack.data.IGenericDAO;

/**
 *
 * @author Jorge Enciso
 */
@TransactionAttribute(TransactionAttributeType.REQUIRED)    
public class LogManager implements ILogManager {
    private static final Logger LOGGER = Logger.getLogger(LogManager.class);
    @EJB private IGenericDAO dao;
    @EJB private ISessionsLocal sessions;

    @Override
    public <T extends ILogRecord> boolean dbWrite(Class<T> logType, String sessionId, String message,
                                                  String messageInfo, Integer messageNumber, String category, 
                                                  String level, String object, String objectField) {
        Integer lineNumber = 0;
        Integer choose = 0;
        return dbWrite(logType, sessionId, message, messageInfo, messageNumber, category,
                            level, object, objectField, lineNumber, choose);
    }
    
    @Override
    public <T extends ILogRecord> boolean dbWrite(Class<T> logType, IUserSession userSession, String message,
                                                  String messageInfo, Integer messageNumber, String category, 
                                                  String level, String object, String objectField) {
        String sessionId = "";
        if (userSession != null) {
            sessionId = userSession.getSessionId();
        }
        Integer lineNumber = 0;
        Integer choose = 0;
        return dbWrite(logType, sessionId, message, messageInfo, messageNumber, 
                        category, level, object, objectField, lineNumber, choose);
    }

    
    @Override
    public <T extends ILogRecord> boolean dbWrite(Class<T> logType, IUserSession userSession, String message,
                                                  String messageInfo, Integer messageNumber, String category, String level, 
                                                  String object, String objectField, Integer lineNumber, Integer choose) {
        String sessionId = "";
        if (userSession != null) {
            sessionId = userSession.getSessionId();
        }
        return dbWrite(logType, sessionId, message, messageInfo, messageNumber, 
                        category, level, object, objectField, lineNumber, choose);
    }

    @Override
    public <T extends ILogRecord> boolean dbWrite(Class<T> logType, String sessionId, String message, String messageInfo,
                                                        Integer messageNumber, String category, String level, String object,
                                                        String objectField, Integer lineNumber, Integer choose) {
        try {
            T logRecord = logType.newInstance();            
            logRecord.setCategory(category);
            logRecord.setLevel(level);
            logRecord.setMessage(message);
            logRecord.setMessageInfo(messageInfo);
            logRecord.setMessageNumber(messageNumber);            
            logRecord.setObject(object);
            logRecord.setObjectField(objectField);
            logRecord.setLineNumber(lineNumber);
            logRecord.setChoose(choose);

            return dbWrite(logRecord, sessionId);
        } catch (Exception exp) {
            ErrorManager.showError(exp, LOGGER);
        }
        return false;
    }

    @Override
    public <T extends ILogRecord> boolean dbWrite(Class<T> logType,IErrorReg errorReg, IUserSession userSession) {
        String sessionId = "";
        if (userSession != null) {
            sessionId = userSession.getSessionId();
        }
        return dbWrite(logType, errorReg, sessionId);
    }

    @Override
    public <T extends ILogRecord> boolean dbWrite(Class<T> logType, IErrorReg errorReg, String sessionId) {
        try {
            T logRecord = logType.newInstance();
            String messageInfo = "";
            if (errorReg.getException() != null) {
                 messageInfo = ErrorManager.getStackCause(errorReg.getException());
            }
            if (Strings.isNullorEmpty(errorReg.getFieldName())){
                logRecord.setCategory(ILogRecord.CATEGORY_APP);                
            }
            else {
                logRecord.setCategory(ILogRecord.CATEGORY_DATA);                                
            }
            logRecord.setLevel(ILogRecord.LEVEL_ERROR);
            logRecord.setMessage(errorReg.getMessage());
            logRecord.setMessageInfo(messageInfo);
            logRecord.setMessageNumber(errorReg.getErrorNumber());
            logRecord.setObject(errorReg.getEntity());
            logRecord.setObjectField(errorReg.getFieldName());
            logRecord.setLineNumber(0);
            logRecord.setChoose(0);
            
            return dbWrite(logRecord, sessionId);
        } catch (Exception exp) {
            ErrorManager.showError(exp, LOGGER);
        }
        return false;
    }

    @Override
    public <T extends ILogRecord> boolean dbWrite(Class<T> logType, Exception exp, IUserSession userSession) {
        String sessionId = "";
        if (userSession != null) {
            sessionId = userSession.getSessionId();
        }
        return dbWrite(logType, exp, sessionId);
    }

    @Override
    public <T extends ILogRecord> boolean dbWrite(Class<T> logType, Exception exp, String sessionId) {
        try {
            T logRecord = logType.newInstance();
            String messageInfo = ErrorManager.getStackCause(exp);            
            logRecord.setCategory(ILogRecord.CATEGORY_APP);
            logRecord.setLevel(ILogRecord.LEVEL_ERROR);
            logRecord.setMessage(exp.getMessage());
            logRecord.setMessageInfo(messageInfo);
            logRecord.setMessageNumber(0);
            logRecord.setObject("");
            logRecord.setObjectField("");
            logRecord.setLineNumber(0);
            logRecord.setChoose(0);
            
            return dbWrite(logRecord, sessionId);
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return false;
    }

    @Override
    public <T extends ILogRecord> boolean dbWrite(T logRecord, IUserSession userSession) {
        String sessionId = "";
        if (userSession != null) {
            sessionId = userSession.getSessionId();
        }
        return dbWrite(logRecord, sessionId);
    }

    @Override
    public <T extends ILogRecord> boolean dbWrite(T logRecord, String sessionId) {
        String sesionId = "NINGUNA";
        Long idempresa = null;
        Long idusuario = null;
        String origin = "";
        if (!Strings.isNullorEmpty(sessionId)) {
            IUserSession userSession = sessions.getUserSession(sessionId);
            if (userSession != null) {
                idempresa = userSession.getIdEmpresa();
                idusuario = userSession.getUser().getIdusuario();
                origin = userSession.getIp();
                sesionId = Strings.dateToString(userSession.getTimeLogin())
                        + userSession.getUser().getCodigo().trim();
            }
        }
        logRecord.setIdempresa(idempresa);
        logRecord.setIdusuario(idusuario);        
        logRecord.setSessionId(sesionId);        
        logRecord.setOrigin(origin);
        if (logRecord.getLogTimeOrigin() == null){
            logRecord.setLogTimeOrigin(new Date());            
        }
        IDataResult dataResult = dao.persist(IDBManager.CATALOGO, logRecord, sessionId);
        return dataResult.isSuccessFul();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)    
    public boolean fWrite(String message, String file, boolean flag) {
        return true;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)        
    public boolean logSend() {
        return true;
    }
    

    /**
     * Busca en dic_mensaje el nro. de mensaje y devuelve el registro
     *
     * @param msgNumber nro. de mensaje
     * @return Mensaje solicitado
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)        
    public IAppMessages getAppMessages(Integer msgNumber) {
        try {
            IAppMessages message = 
                    dao.findByQuery(IAppMessages.class, IDBManager.CATALOGO,
                                    "select o from DicMensaje o where nro = " + msgNumber.toString(),
                                    null);

            return message;
        } catch (Exception exp) {
            ErrorManager.showError(exp, LOGGER);
        }
        return null;
    }

    /**
     * Busca en dic_mensaje el nro. de mensaje y devuelve el registro
     *
     * @return Mensaje solicitado
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)        
    public List<IAppMessages> getAppMessages() {
        try {
            List<IAppMessages> messages = 
                    dao.findListByQuery(IDBManager.CATALOGO,
                                        "select o from DicMensaje o order by nro",
                                         null);

            return messages;
        } catch (Exception exp) {
            ErrorManager.showError(exp, LOGGER);
        }
        return null;
    }
}

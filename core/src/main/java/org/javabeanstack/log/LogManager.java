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

import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.data.IDataResult;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.security.ISessions;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.util.Strings;
import org.javabeanstack.model.IAppMessage;
import org.javabeanstack.data.IGenericDAO;
import org.javabeanstack.model.IAppLogRecord;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.LocalDates;

/**
 * Su función es gestionar la escritura y lectura del log del sistema.
 *
 * @author Jorge Enciso
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class LogManager implements ILogManager {

    private static final Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger(LogManager.class);

    private final String LOG_TYPE_PATH = "org.javabeanstack.model.appcatalog.AppLogEvent";

    @EJB
    private IGenericDAO dao;

    @EJB
    private ISessions sessions;

    protected String getLogTypePath(){
        return LOG_TYPE_PATH;
    }
    
    @Override
    public <T extends IAppLogRecord> IAppLogRecord getNewAppLogRecord(Class<T> logType) {
        try {
            if (logType == null) {
                Class logTypeDefault = Class.forName(getLogTypePath()); 
                return (IAppLogRecord) logTypeDefault.getConstructor().newInstance();
            }
            return logType.getConstructor().newInstance();
        } catch (Exception ex) {
            //continua con la busqueda
        }
        return null;
    }

    /**
     * Escribe información de un evento en una tabla de la base de datos.
     *
     * @param errorReg objeto error conteniendo la información del evento.
     * @return verdadero si tuvo exito y falso si no.
     */
    @Override
    public boolean dbWrite(IErrorReg errorReg) {
        return dbWrite(null, null, errorReg);
    }

    /**
     * Escribe información de un evento en una tabla de la base de datos.
     *
     * @param <T>
     * @param logType
     * @param error objeto error conteniendo la información del evento.
     * @param sessionId identificador de la sesión del usuario
     * @return verdadero si tuvo exito y falso si no.
     */
    @Override
    public <T extends IAppLogRecord> boolean dbWrite(Class<T> logType, String sessionId, IErrorReg error) {
        try {
            IAppLogRecord logRecord = getNewAppLogRecord(logType);
            String messageInfo = "";
            if (error.getException() != null) {
                messageInfo = ErrorManager.getStackCause(error.getException());
            }
            logRecord.setEvent(error.getEvent());
            logRecord.setLevel(error.getLevel());
            logRecord.setMessage(error.getMessage());
            logRecord.setMessageInfo(messageInfo);
            logRecord.setMessageNumber(error.getErrorNumber());
            logRecord.setAppObject(error.getEntity());
            logRecord.setIpRequestFrom(error.getIpRequest());
            return dbWrite(logRecord, sessionId);
        } catch (Exception exp) {
            ErrorManager.showError(exp, LOGGER);
        }
        return false;
    }

    /**
     * Escribe información de un evento en una tabla de la base de datos.
     *
     * @param <T>
     * @param logRecord registro del evento.
     * @param sessionId identificador de la sesión del usuario
     * @return verdadero si tuvo exito y falso si no.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Override
    public <T extends IAppLogRecord> boolean dbWrite(T logRecord, String sessionId) {
        String sesionId = "NINGUNA";
        Long idcompany = null;
        Long iduser = null;
        String ipRequestFrom = "";
        if (!Strings.isNullorEmpty(sessionId)) {
            IUserSession userSession = sessions.getUserSession(sessionId);
            if (userSession != null && !Strings.isNullorEmpty(userSession.getIp())) {
                ipRequestFrom = userSession.getIp();
            }
            if (userSession != null && userSession.getUser() != null) {
                idcompany = userSession.getIdCompany();
                iduser = userSession.getUser().getIduser();
                sesionId = Strings.localDateToString(userSession.getTimeLogin())
                        + userSession.getUser().getLogin().trim();
            }
            logRecord.setIdcompany(idcompany);
            logRecord.setIduser(iduser);
            logRecord.setSessionId(sesionId);
            logRecord.setIpRequestFrom(ipRequestFrom);
        }
        if (logRecord.getLogTimeOrigin() == null) {
            logRecord.setLogTimeOrigin(LocalDates.now());
        }
        logRecord.setMessage(Fn.nvl(logRecord.getMessage(),""));
        logRecord.setMessageInfo(Fn.nvl(logRecord.getMessageInfo(),""));
        logRecord.setMessageNumber(Fn.nvl(logRecord.getMessageNumber(),0));
        IDataResult dataResult;
        try {
            dataResult = dao.persist(null, logRecord);
            return dataResult.isSuccessFul();
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return false;
    }

    /**
     * Busca en una tabla de mensajes en la base de datos "AppMessage" el nro.
     * de mensaje y devuelve el registro
     *
     * @param msgNumber nro. de mensaje
     * @return Mensaje solicitado
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public IAppMessage getAppMessage(Integer msgNumber) {
        try {
            IAppMessage message
                    = dao.findByQuery(null,
                            "select o from AppMessage o where nro = " + msgNumber.toString(),
                            null);

            return message;
        } catch (Exception exp) {
            ErrorManager.showError(exp, LOGGER);
        }
        return null;
    }

    /**
     * Busca en una tabla de mensajes en la base de datos "AppMessage" el nro.
     * de mensaje y devuelve el registro
     *
     * @return Mensaje solicitado
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<IAppMessage> getAppMessages() {
        try {
            List<IAppMessage> messages
                    = dao.findListByQuery(null,
                            "select o from AppMessage o order by nro",
                            null);

            return messages;
        } catch (Exception exp) {
            ErrorManager.showError(exp, LOGGER);
        }
        return new ArrayList();
    }
}

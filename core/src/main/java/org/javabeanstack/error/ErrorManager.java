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
package org.javabeanstack.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.log4j.Logger;
import org.javabeanstack.model.IAppMessage;
import org.javabeanstack.log.ILogManager;
import org.javabeanstack.model.IAppLogRecord;
import org.javabeanstack.util.Fn;
import static org.javabeanstack.util.AppUtil.*;
import org.javabeanstack.util.Strings;

/**
 * Su función es la de gestionar los errores del sistema
 *
 * @author Jorge Enciso
 */
public class ErrorManager {

    private static String getAppPackage() {
        String className = getCallerStack("org.javabeanstack.error.ErrorManager").getClassName();
        int pos = Strings.findString(".", className, 2);
        String retornar = Strings.substr(className, 0, pos);
        return retornar;
    }

    /**
     * Busca en "AppMessage" el nro. de mensaje y devuelve el texto.
     *
     * @param msgNumber nro. de mensaje
     * @param logManager objeto que gestiona el acceso a la base de datos
     * @return Mensaje solicitado
     */
    public String getErrorMessage(Integer msgNumber, ILogManager logManager) {
        IAppMessage message = logManager.getAppMessage(msgNumber);
        if (message == null) {
            return null;
        }
        return message.getText();
    }

    /**
     * Busca en AppMessage el nro. de mensaje y devuelve el registro en formato
     * IErrorReg
     *
     * @param msgNumber nro. de mensaje
     * @param fieldName nombre del campo
     * @param logManager objeto que gestiona el acceso a la base de datos
     * @return objeto IErrorReg con el registro del mensaje
     */
    public IErrorReg getErrorReg(Integer msgNumber, String fieldName, ILogManager logManager) {
        IAppMessage message = logManager.getAppMessage(msgNumber);
        if (message == null) {
            return null;
        }
        IErrorReg errorReg = new ErrorReg();
        errorReg.setErrorNumber(msgNumber);
        errorReg.setFieldName(fieldName);
        errorReg.setMessage(message.getText());
        return errorReg;
    }

    /**
     * Muestra el error utilizando log4j
     *
     * @param ex
     * @param logger
     */
    public static void showError(Exception ex, Logger logger) {
        String msg = getStackCause(ex);
        if (msg == null || msg.isEmpty() || msg.length() < 5) {
            logger.error("\n" + getStackTraceText(getAppPackage()));
            logger.error(getStackTrace(ex));
            return;
        }
        logger.error(msg + getStackTraceText(getAppPackage()));
    }
    
    public static String getMessageToShow(Exception ex){
        String msg = getStackCause(ex);
        if (msg == null || msg.isEmpty() || msg.length() < 5) {
            return getStackTraceText(getAppPackage());
        }
        return msg + getStackTraceText(getAppPackage());
    }

    /**
     * Muestra el error utilizando log4j
     *
     * @param ex
     * @param logger
     * @param logManager
     * @param sessionId
     */
    public static void showError(Exception ex, Logger logger, ILogManager logManager, String sessionId) {
        String msg = getStackCause(ex);
        if (msg == null || msg.isEmpty() || msg.length() < 5) {
            logger.error("\n" + getStackTraceText(getAppPackage()));
            logger.error(getStackTrace(ex));
        } else {
            logger.error(msg + getStackTraceText(getAppPackage()));
        }
        if (logManager == null){
            return;
        }
        //
        try {
            IAppLogRecord logRecord = logManager.getNewAppLogRecord(null);
            String messageInfo = ErrorManager.getStackCause(ex) + getStackTraceText(getAppPackage());
            logRecord.setMessageInfo(messageInfo);            
            logRecord.setMessage(ex.getMessage());
            logRecord.setMessageNumber(1);
            logRecord.setEvent(IAppLogRecord.EVENT_ERROR);
            logRecord.setLevel(IAppLogRecord.LEVEL_ERROR);
            logRecord.setAppObject(getCallerStack("org.javabeanstack.error.ErrorManager").getClassName());
            logManager.dbWrite(logRecord, sessionId);
        } catch (Exception e) {
            //
        }
    }

    /**
     * Muestra el error utilizando log4j
     *
     * @param ex
     * @param logger
     * @param level
     */
    public static void showError(Exception ex, Logger logger, int level) {
        if (level == 1) {
            logger.error(getStackCause(ex) + getStackTraceText(getAppPackage()));
        } else {
            logger.error("\n" + getStackTraceText(getAppPackage()));
            logger.error(getStackTrace(ex));
        }

    }

    /**
     * Devuelve el error con el detalle de la pila de llamadas
     *
     * @param throwable
     * @return el error con el detalle de la pila de llamadas.
     */
    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    /**
     * Devuelve el error producido.
     *
     * @param err
     * @return error producido.
     */
    public static String getStackCause(final Throwable err) {
        String errorMsg = "";
        if (err.getCause() != null) {
            errorMsg = ErrorManager.getStackCause(err.getCause());
        }
        errorMsg += Fn.nvl(err.getMessage(), "") + "\n";
        return errorMsg;
    }
}

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
import javax.ejb.EJB;
import org.apache.log4j.Logger;
import org.javabeanstack.model.IAppMessage;
import org.javabeanstack.log.ILogManager;
import org.javabeanstack.util.Fn;

/**
 * Su función es la de gestionar los errores del sistema
 *
 * @author Jorge Enciso
 */
public class ErrorManager {
    @EJB private ILogManager logManager;


    /**
     * Busca en "AppMessage" el nro. de mensaje y devuelve el texto.
     *
     * @param msgNumber nro. de mensaje
     * @return Mensaje solicitado
     */
    public String getErrorMessage(Integer msgNumber) {
        return getErrorMessage(msgNumber, logManager);
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
     * Busca en AppMessage el nro. de mensaje y devuelve el registro en formato IErrorReg
     *
     * @param msgNumber nro. de mensaje
     * @param fieldName nombre del campo
     * @return objeto IErrorReg con el registro del mensaje
     */
    public IErrorReg getErrorReg(Integer msgNumber, String fieldName) {
        return getErrorReg(msgNumber, fieldName, logManager);
    }

    /**
     * Busca en AppMessage el nro. de mensaje y devuelve el registro en formato IErrorReg
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
        logger.error(msg);
    }

    /**
     * Muestra el error utilizando log4j
     *
     * @param ex
     * @param logger
     * @param level
     */
    public static void showError(Exception ex, Logger logger, int level) {
        if (level == 1){
            logger.error(getStackCause(ex));            
        }
        else{
            logger.error(getStackTrace(ex));            
        }
    }
    
    
    /**
     * Devuelve el error con el detalle de la pila de llamadas
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
     * @param err  
     * @return error producido.
     */
    public static String getStackCause(final Throwable err) {
        String errorMsg = ""; 
        if (err.getCause() != null) {
            errorMsg = ErrorManager.getStackCause(err.getCause());
        }
        errorMsg += Fn.nvl(err.getMessage(),"") + "\n";
        return errorMsg;
    }
}

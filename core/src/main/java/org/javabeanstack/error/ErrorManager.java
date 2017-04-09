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
import org.javabeanstack.log.ILogManager;
import org.javabeanstack.model.IAppMessage;

/**
 * Su función es la de gestionar los errores del sistema
 *
 * @author Jorge Enciso
 */
public class ErrorManager {
    private static final Logger LOGGER = Logger.getLogger(ErrorManager.class);

    @EJB private ILogManager logManager;

    /**
     * Se ejecuta al momento de generarse un error en la aplicación <br>
     * Su función es: <br>
     * 1- Recibir información del error ocurrido.<br>
     * 2- Mostrar el error en pantalla al usuario.<br>
     * 3- Grabar el error en el log del sistema.<br>
     *
     * @param error Objeto conteniendo la excepción
     * @param view Si va a mostrar o no una pantalla con la descripción del
     * error
     */
    public void ErrorLog(Exception error, boolean view) {

    }

    /**
     * Busca en dic_mensaje el nro. de mensaje y devuelve el texto.
     *
     * @param msgNumber nro. de mensaje
     * @return Mensaje solicitado
     */
    public String getErrorMessage(Integer msgNumber) {
        return getErrorMessage(msgNumber, logManager);
    }

    /**
     * Busca en dic_mensaje el nro. de mensaje y devuelve el texto.
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
     * Busca en dic_mensaje el nro. de mensaje y devuelve el registro en formato IErrorReg
     *
     * @param msgNumber nro. de mensaje
     * @param fieldName nombre del campo
     * @return objeto IErrorReg con el registro del mensaje
     */
    public IErrorReg getErrorReg(Integer msgNumber, String fieldName) {
        return getErrorReg(msgNumber, fieldName, logManager);
    }

    /**
     * Busca en dic_mensaje el nro. de mensaje y devuelve el registro en formato IErrorReg
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
        logger.error(getStackTrace(ex));
    }

    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    public static String getStackCause(final Throwable err) {
        String errorMsg = "";
        if (err.getCause() != null) {
            errorMsg = ErrorManager.getStackCause(err.getCause());
        }
        errorMsg += err.getMessage() + "\n";
        return errorMsg;
    }
}

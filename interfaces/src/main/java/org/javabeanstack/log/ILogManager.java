/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
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
import org.javabeanstack.model.IAppMessage;
import org.javabeanstack.model.IAppLogRecord;

/**
 * Contrato del administrador de log: persiste mensajes y eventos
 * ({@link IAppLogRecord}) en la base de datos y da acceso al catálogo de
 * mensajes ({@link IAppMessage}).
 *
 * <p>La implementación de referencia es {@code org.javabeanstack.log.LogManager}.</p>
 *
 * @author Jorge Enciso
 */
public interface ILogManager {
    /**
     * Devuelve un mensaje del catálogo por su número.
     *
     * @param msgNumber número del mensaje.
     * @return mensaje del catálogo.
     */
    IAppMessage getAppMessage(Integer msgNumber);

    /**
     * Devuelve todos los mensajes del catálogo.
     *
     * @return lista de mensajes.
     */
    List<IAppMessage> getAppMessages();

    /**
     * Persiste un error en el log.
     *
     * @param errorReg registro de error.
     * @return verdadero si se persistió, falso si no.
     */
    boolean dbWrite(IErrorReg errorReg);

    /**
     * Persiste un error en un tipo de log específico.
     *
     * @param <T> tipo del registro de log.
     * @param logType clase del registro de log.
     * @param sessionId identificador de la sesión del usuario.
     * @param errorReg registro de error.
     * @return verdadero si se persistió, falso si no.
     */
    <T extends IAppLogRecord> boolean dbWrite(Class<T> logType, String sessionId, IErrorReg errorReg);

    /**
     * Persiste un registro de log ya construido.
     *
     * @param <T> tipo del registro de log.
     * @param logRecord registro de log a persistir.
     * @param sessionId identificador de la sesión del usuario.
     * @return verdadero si se persistió, falso si no.
     */
    <T extends IAppLogRecord> boolean dbWrite(T logRecord, String sessionId);

    /**
     * Crea una nueva instancia de registro de log del tipo indicado.
     *
     * @param <T> tipo del registro de log.
     * @param logType clase del registro de log.
     * @return nueva instancia de registro de log.
     */
    <T extends IAppLogRecord> IAppLogRecord getNewAppLogRecord(Class<T> logType);
}

/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*        jenciso@javabeanstack.org
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
package org.javabeanstack.messaging.mail;

import jakarta.mail.Session;
import org.javabeanstack.config.IAppConfig;
import org.javabeanstack.messaging.IMailMessage;

/**
 * Envío de un correo por SMTP. Es <b>síncrono</b>: el llamador recibe el
 * resultado del intento. Este es el mecanismo autosuficiente de la Fase 2, el que
 * puede usar el rediseño de autenticación para mandar el enlace de activación sin
 * pasar por la base de datos. La Fase 3 agrega el envío encolado, sin cambiar
 * este contrato.
 *
 * @author Jorge Enciso
 */
public interface IMailSender {

    /**
     * Envía un mensaje de correo usando la sesión indicada.
     *
     * @param message mensaje a enviar.
     * @param session sesión de correo con la que enviar.
     * @return el resultado del envío (nunca nulo).
     */
    IMailSendResult send(IMailMessage message, Session session);

    /**
     * Envía un mensaje resolviendo la sesión desde la configuración de la
     * empresa. Camino de conveniencia para quien no quiera manejar la
     * {@link Session}.
     *
     * @param appConfig configuración de la aplicación.
     * @param idcompany empresa para la cual se resuelve la sesión.
     * @param message mensaje a enviar.
     * @return el resultado del envío (nunca nulo).
     */
    IMailSendResult send(IAppConfig appConfig, Long idcompany, IMailMessage message);
}

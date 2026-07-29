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

/**
 * Proveedor de la {@link jakarta.mail.Session} con la que se envía el correo.
 *
 * <p>En la Fase 2 la sesión se resuelve, en este orden: los parámetros globales
 * del sistema ({@code MAIL_SMTP_*} en la configuración de la aplicación) con
 * prioridad, y la {@code Session} del contenedor ({@code java:jboss/mail/Default})
 * como respaldo. En la Fase 3 se antepone la cuenta por empresa
 * ({@code AppMsgMailCredential}).</p>
 *
 * @author Jorge Enciso
 */
public interface IMailSessionProvider {

    /**
     * Devuelve la sesión de correo a usar para una empresa.
     *
     * @param appConfig configuración de la aplicación, de donde se leen los
     * parámetros de correo.
     * @param idcompany empresa para la cual se resuelve la sesión (no se usa en
     * la Fase 2, que sólo tiene configuración global; se conserva para la Fase 3).
     * @return la sesión de correo.
     * @throws Exception si no hay ninguna configuración de correo disponible.
     */
    Session getSession(IAppConfig appConfig, Long idcompany) throws Exception;
}

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
import org.javabeanstack.messaging.IMailAccount;

/**
 * Proveedor de la {@link jakarta.mail.Session} con la que se envía el correo.
 *
 * <p>La sesión se arma a partir de la {@link IMailAccount} que se recibe: si la
 * cuenta define un servidor SMTP se usa ese, y si no se resuelve la sesión del
 * contenedor por JNDI. <b>De dónde salió esa cuenta no es asunto de este
 * módulo</b>: la aplicación que lo usa es la que decide si viene de sus
 * parámetros de sistema, de una tabla por empresa o de valores fijos.</p>
 *
 * @author Jorge Enciso
 */
public interface IMailSessionProvider {

    /**
     * Devuelve la sesión de correo correspondiente a una cuenta.
     *
     * @param account cuenta de correo con la configuración ya resuelta.
     * @return la sesión de correo.
     * @throws Exception si la cuenta no permite armar ninguna sesión.
     */
    Session getSession(IMailAccount account) throws Exception;

    /**
     * Comprueba si una cuenta está en condiciones de enviar, sin abrir ninguna
     * conexión con el servidor de correo.
     *
     * <p>Responde si la cuenta <b>está configurada</b>, no si el servidor
     * contesta. Sirve para no ofrecer una función que no va a poder cumplirse y
     * para avisar de una configuración incompleta antes de que alguien la
     * descubra con un correo que nunca llegó.</p>
     *
     * @param account cuenta de correo a comprobar, puede ser nula.
     * @return el estado de la cuenta (nunca nulo).
     */
    MailChannelStatus checkConfig(IMailAccount account);
}

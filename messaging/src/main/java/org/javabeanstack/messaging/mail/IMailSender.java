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
     * Envía un mensaje armando la sesión a partir de la cuenta indicada. Camino
     * de conveniencia para quien no quiera manejar la {@link Session}.
     *
     * @param account cuenta de correo con la configuración ya resuelta.
     * @param message mensaje a enviar.
     * @return el resultado del envío (nunca nulo).
     */
    IMailSendResult send(IMailAccount account, IMailMessage message);

    /**
     * Prueba que se pueda establecer el diálogo con el servidor de correo de una
     * cuenta, <b>sin enviar nada</b>: abre el transporte, se autentica si
     * corresponde y lo cierra.
     *
     * <p>Es lo único que detecta una credencial rechazada o un servidor
     * inalcanzable, cosas que la comprobación de la configuración no puede ver
     * porque no toca la red. Como abre una conexión, no conviene usarla en cada
     * petición.</p>
     *
     * <p>No lanza excepción, igual que el envío: el desenlace vuelve en el
     * resultado.</p>
     *
     * @param account cuenta de correo a probar.
     * @return el resultado del intento (nunca nulo). Sin identificador de
     * mensaje, porque no se envió ninguno.
     */
    IMailSendResult testConnection(IMailAccount account);
}

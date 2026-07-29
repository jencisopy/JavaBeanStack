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

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.config.IAppConfig;
import org.javabeanstack.messaging.IMailMessage;

/**
 * Implementación de {@link IMailSender}: convierte el mensaje a MIME con
 * {@link MailMessageConverter} y lo envía con {@code Transport.send}.
 *
 * <p>Clasifica los fallos: una dirección rechazada o una autenticación inválida
 * son definitivos ({@link IMailSendResult#isRetryable()} falso); un problema de
 * red o un servidor que no responde son recuperables. Nunca lanza excepción: todo
 * fallo vuelve en el resultado, para que el llamador —incluido el despachador de
 * la Fase 3— decida qué hacer.</p>
 *
 * @author Jorge Enciso
 */
public class MailSender implements IMailSender {

    private static final Logger LOGGER = LogManager.getLogger(MailSender.class);

    private final MailMessageConverter converter = new MailMessageConverter();
    private final IMailSessionProvider sessionProvider = new MailSessionProvider();

    @Override
    public IMailSendResult send(IMailMessage message, Session session) {
        try {
            MimeMessage mime = converter.toMimeMessage(message, session);
            Transport.send(mime);
            return MailSendResult.ok(mime.getMessageID());
        } catch (SendFailedException ex) {
            // Direcciones inválidas: no mejora con reintentos.
            MailSendResult result = MailSendResult.failed(ex.getMessage());
            addInvalid(result, ex);
            LOGGER.info("Envio de correo rechazado (direcciones): " + ex.getMessage());
            return result;
        } catch (AuthenticationFailedException ex) {
            LOGGER.error("Envio de correo rechazado (autenticacion): " + ex.getMessage());
            return MailSendResult.failed(ex.getMessage());
        } catch (MessagingException ex) {
            // Fallo de conexión/red: conviene reintentar.
            LOGGER.warn("Envio de correo con error recuperable: " + ex.getMessage(), ex);
            return MailSendResult.retryable(ex.getMessage());
        } catch (Exception ex) {
            // Armado inválido (sin remitente, sin destinatarios, adjunto sin contenido).
            LOGGER.error("No se pudo armar el correo: " + ex.getMessage());
            return MailSendResult.failed(ex.getMessage());
        }
    }

    @Override
    public IMailSendResult send(IAppConfig appConfig, Long idcompany, IMailMessage message) {
        Session session;
        try {
            session = sessionProvider.getSession(appConfig, idcompany);
        } catch (Exception ex) {
            LOGGER.error("No hay configuracion de correo disponible: " + ex.getMessage());
            return MailSendResult.failed("No hay configuracion de correo disponible: " + ex.getMessage());
        }
        return send(message, session);
    }

    private void addInvalid(MailSendResult result, SendFailedException ex) {
        if (ex.getInvalidAddresses() != null) {
            for (jakarta.mail.Address address : ex.getInvalidAddresses()) {
                result.addInvalidAddress(address.toString());
            }
        }
    }
}

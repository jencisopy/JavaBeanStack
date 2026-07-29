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
import org.javabeanstack.messaging.IMailAccount;
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
    public IMailSendResult send(IMailAccount account, IMailMessage message) {
        Session session;
        try {
            session = sessionProvider.getSession(account);
        } catch (Exception ex) {
            LOGGER.error("No hay configuracion de correo disponible: " + ex.getMessage());
            return MailSendResult.failed("No hay configuracion de correo disponible: " + ex.getMessage());
        }
        return send(message, session);
    }

    @Override
    public IMailSendResult testConnection(IMailAccount account) {
        Session session;
        try {
            session = sessionProvider.getSession(account);
        } catch (Exception ex) {
            LOGGER.error("No hay configuracion de correo disponible: " + ex.getMessage());
            return MailSendResult.failed("No hay configuracion de correo disponible: " + ex.getMessage());
        }
        Transport transport = null;
        try {
            transport = session.getTransport("smtp");
            // Con autenticación, las credenciales las aporta el Authenticator de
            // la sesión; sin ella, connect() sin argumentos solo abre el diálogo.
            transport.connect();
            return MailSendResult.ok(null);
        } catch (AuthenticationFailedException ex) {
            // La credencial es incorrecta: reintentar no cambia nada. Es
            // justamente lo que la comprobación pasiva no puede detectar.
            LOGGER.error("Prueba de conexion rechazada (autenticacion): " + ex.getMessage());
            return MailSendResult.failed(ex.getMessage());
        } catch (MessagingException ex) {
            LOGGER.warn("Prueba de conexion con error recuperable: " + ex.getMessage());
            return MailSendResult.retryable(ex.getMessage());
        } catch (Exception ex) {
            LOGGER.error("No se pudo probar la conexion: " + ex.getMessage());
            return MailSendResult.failed(ex.getMessage());
        } finally {
            if (transport != null) {
                try {
                    transport.close();
                } catch (MessagingException ex) {
                    // Cerrar es cortesía con el servidor: que falle no cambia el
                    // resultado de la prueba, que ya se decidió arriba.
                    LOGGER.debug("No se pudo cerrar el transporte de la prueba: " + ex.getMessage());
                }
            }
        }
    }

    private void addInvalid(MailSendResult result, SendFailedException ex) {
        if (ex.getInvalidAddresses() != null) {
            for (jakarta.mail.Address address : ex.getInvalidAddresses()) {
                result.addInvalidAddress(address.toString());
            }
        }
    }
}

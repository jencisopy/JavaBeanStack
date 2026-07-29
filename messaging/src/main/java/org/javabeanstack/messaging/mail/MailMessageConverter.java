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

import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimePart;
import jakarta.mail.util.ByteArrayDataSource;
import java.time.ZoneId;
import java.util.Date;
import org.javabeanstack.messaging.IMailAddress;
import org.javabeanstack.messaging.IMailMessage;
import org.javabeanstack.messaging.IMessageAttachment;
import org.javabeanstack.util.Fn;

/**
 * Convierte un {@link IMailMessage} en un {@code jakarta.mail.internet.MimeMessage}
 * listo para enviar. Es el único lugar del subsistema que conoce MIME.
 *
 * <p>Arma el cuerpo como {@code multipart/alternative} cuando hay texto y HTML,
 * y lo envuelve en {@code multipart/mixed} cuando hay adjuntos. Los adjuntos
 * embebidos ({@code inline}, con {@code Content-ID}) permiten imágenes dentro del
 * HTML. Todo se codifica en UTF-8.</p>
 *
 * @author Jorge Enciso
 */
public class MailMessageConverter {

    private static final String UTF8 = "UTF-8";
    private static final String HTML_TYPE = "text/html; charset=UTF-8";

    /**
     * Construye el mensaje MIME a partir de un mensaje de correo y una sesión.
     *
     * @param message mensaje de correo a convertir.
     * @param session sesión de correo.
     * @return el mensaje MIME listo para enviar.
     * @throws Exception si el mensaje no tiene remitente o destinatarios, o si
     * falla el armado MIME.
     */
    public MimeMessage toMimeMessage(IMailMessage message, Session session) throws Exception {
        if (Fn.nvl(message.getFromAddress(), "").trim().isEmpty()) {
            throw new IllegalArgumentException("El mensaje no tiene remitente (From)");
        }
        MimeMessage mime = new MimeMessage(session);
        mime.setFrom(new InternetAddress(message.getFromAddress(),
                Fn.nvl(message.getFromName(), ""), UTF8));
        if (!Fn.nvl(message.getReplyTo(), "").trim().isEmpty()) {
            mime.setReplyTo(new InternetAddress[]{new InternetAddress(message.getReplyTo())});
        }

        int recipients = addRecipients(mime, message);
        if (recipients == 0) {
            throw new IllegalArgumentException("El mensaje no tiene destinatarios");
        }

        mime.setSubject(Fn.nvl(message.getSubject(), ""), UTF8);
        mime.setSentDate(toDate(message));

        if (message.getAttachmentList() == null || message.getAttachmentList().isEmpty()) {
            applyBody(mime, message);
        } else {
            MimeMultipart mixed = new MimeMultipart("mixed");
            MimeBodyPart bodyPart = new MimeBodyPart();
            applyBody(bodyPart, message);
            mixed.addBodyPart(bodyPart);
            for (IMessageAttachment attachment : message.getAttachmentList()) {
                mixed.addBodyPart(toBodyPart(attachment));
            }
            mime.setContent(mixed);
        }

        mime.saveChanges();
        return mime;
    }

    /**
     * Agrega los destinatarios {@code To}, {@code Cc} y {@code Bcc} al mensaje MIME.
     *
     * @param mime mensaje MIME en construcción.
     * @param message mensaje de correo de origen.
     * @return cantidad de destinatarios agregados.
     * @throws Exception si una dirección es inválida.
     */
    protected int addRecipients(MimeMessage mime, IMailMessage message) throws Exception {
        int count = 0;
        for (IMailAddress address : message.getAddressList()) {
            Message.RecipientType type = recipientType(address.getType());
            if (type == null) {
                continue;
            }
            mime.addRecipient(type, new InternetAddress(address.getAddress(),
                    Fn.nvl(address.getName(), ""), UTF8));
            count++;
        }
        return count;
    }

    /**
     * Fija el cuerpo del mensaje: {@code multipart/alternative} si hay texto y
     * HTML, o una sola parte si hay uno solo.
     *
     * @param part parte MIME donde fijar el cuerpo (el mensaje o un body part).
     * @param message mensaje de correo de origen.
     * @throws Exception si falla el armado del cuerpo.
     */
    protected void applyBody(MimePart part, IMailMessage message) throws Exception {
        String text = message.getBodyText();
        String html = message.getBodyHtml();
        if (html != null && text != null) {
            MimeMultipart alternative = new MimeMultipart("alternative");
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(text, UTF8);
            alternative.addBodyPart(textPart);
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(html, HTML_TYPE);
            alternative.addBodyPart(htmlPart);
            part.setContent(alternative);
        } else if (html != null) {
            part.setContent(html, HTML_TYPE);
        } else {
            part.setText(Fn.nvl(text, ""), UTF8);
        }
    }

    /**
     * Convierte un adjunto en un body part MIME.
     *
     * @param attachment adjunto a convertir.
     * @return el body part con el adjunto.
     * @throws Exception si el adjunto no tiene contenido en memoria.
     */
    protected MimeBodyPart toBodyPart(IMessageAttachment attachment) throws Exception {
        if (attachment.getContent() == null) {
            throw new IllegalArgumentException("El adjunto '" + attachment.getFilename()
                    + "' no tiene contenido en memoria (almacenamiento externo no soportado en el envio directo)");
        }
        MimeBodyPart part = new MimeBodyPart();
        ByteArrayDataSource source = new ByteArrayDataSource(attachment.getContent(),
                Fn.nvl(attachment.getContentType(), "application/octet-stream"));
        part.setDataHandler(new DataHandler(source));
        part.setFileName(attachment.getFilename());
        if (Boolean.TRUE.equals(attachment.getInline())) {
            part.setDisposition(Part.INLINE);
            if (!Fn.nvl(attachment.getContentId(), "").trim().isEmpty()) {
                part.setContentID("<" + attachment.getContentId() + ">");
            }
        } else {
            part.setDisposition(Part.ATTACHMENT);
        }
        return part;
    }

    private Message.RecipientType recipientType(String type) {
        if (IMailAddress.TYPE_TO.equals(type)) {
            return Message.RecipientType.TO;
        }
        if (IMailAddress.TYPE_CC.equals(type)) {
            return Message.RecipientType.CC;
        }
        if (IMailAddress.TYPE_BCC.equals(type)) {
            return Message.RecipientType.BCC;
        }
        return null;
    }

    private Date toDate(IMailMessage message) {
        java.time.LocalDateTime date = Fn.nvl(message.getMessageDate(), java.time.LocalDateTime.now());
        return Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
    }
}

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

import java.util.ArrayList;
import java.util.List;

import org.javabeanstack.error.ErrorReg;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.messaging.IMailAccount;
import org.javabeanstack.outputs.IDocumentTarget;
import org.javabeanstack.outputs.IOutputDocument;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.Strings;

/**
 * Destino del subsistema de salida ({@code org.javabeanstack.outputs}) que
 * envía el documento por <b>correo</b>, como adjunto de un mensaje. No
 * reimplementa nada del correo: arma un {@link MailMessage} con el documento
 * adjunto (nombre + content-type + bytes, la conversión directa que permite el
 * calce con {@code IMessageAttachment}) y delega el envío en
 * {@link MailSender}, que nunca lanza y resuelve la sesión SMTP desde la
 * {@link IMailAccount}.
 *
 * <p>Uso típico:</p>
 * <pre>
 * IDocumentTarget mail = new MailTarget(mailAccount)
 *     .to("cliente@correo.com")
 *     .subject("Su extracto")
 *     .text("Le adjuntamos el extracto solicitado.");
 * </pre>
 *
 * @author Jorge Enciso
 */
public class MailTarget implements IDocumentTarget {

    /** Nombre del canal para mensajes y log. */
    public static final String CHANNEL = "mail";

    private final IMailAccount account;
    private final List<String> to = new ArrayList();
    private final List<String> cc = new ArrayList();
    private final List<String> bcc = new ArrayList();
    private String subject;
    private String text;
    private String html;

    /**
     * Crea el destino sobre la cuenta de correo saliente indicada.
     *
     * @param account cuenta SMTP (o sesión JNDI del contenedor) ya resuelta
     * por la aplicación.
     */
    public MailTarget(IMailAccount account) {
        this.account = account;
    }

    /**
     * Agrega un destinatario principal. Puede llamarse varias veces.
     *
     * @param address dirección de correo.
     * @return esta instancia, para encadenar.
     */
    public MailTarget to(String address) {
        if (!Strings.isNullorEmpty(address)) {
            to.add(address.trim());
        }
        return this;
    }

    /**
     * Agrega los destinatarios principales de la lista. Ignora nulos y vacíos.
     *
     * @param addresses direcciones de correo.
     * @return esta instancia, para encadenar.
     */
    public MailTarget to(List<String> addresses) {
        if (addresses != null) {
            for (String address : addresses) {
                to(address);
            }
        }
        return this;
    }

    /**
     * Agrega un destinatario en copia. Puede llamarse varias veces.
     *
     * @param address dirección de correo.
     * @return esta instancia, para encadenar.
     */
    public MailTarget cc(String address) {
        if (!Strings.isNullorEmpty(address)) {
            cc.add(address.trim());
        }
        return this;
    }

    /**
     * Agrega un destinatario en copia oculta. Puede llamarse varias veces.
     *
     * @param address dirección de correo.
     * @return esta instancia, para encadenar.
     */
    public MailTarget bcc(String address) {
        if (!Strings.isNullorEmpty(address)) {
            bcc.add(address.trim());
        }
        return this;
    }

    /**
     * Asigna el asunto del correo; por omisión, el nombre del documento.
     *
     * @param subject asunto.
     * @return esta instancia, para encadenar.
     */
    public MailTarget subject(String subject) {
        this.subject = subject;
        return this;
    }

    /**
     * Asigna el cuerpo del mensaje en texto plano.
     *
     * @param text cuerpo del mensaje.
     * @return esta instancia, para encadenar.
     */
    public MailTarget text(String text) {
        this.text = text;
        return this;
    }

    /**
     * Asigna el cuerpo del mensaje en HTML (alternativo al texto plano).
     *
     * @param html cuerpo del mensaje en HTML.
     * @return esta instancia, para encadenar.
     */
    public MailTarget html(String html) {
        this.html = html;
        return this;
    }

    /**
     * Envía el documento como adjunto a los destinatarios configurados.
     *
     * @param document documento generado.
     * @return resultado sin error si el envío fue aceptado; con el mensaje del
     * fallo si no (el error de {@link MailSender} viaja tal cual).
     * @throws Exception si el documento es nulo o vacío, no hay cuenta o no
     * hay destinatarios (uso inválido).
     */
    @Override
    public IErrorReg deliver(IOutputDocument document) throws Exception {
        if (document == null || document.getSize() == 0) {
            throw new Exception("El documento a enviar es nulo o está vacío");
        }
        if (account == null) {
            throw new Exception("No se configuró la cuenta de correo saliente");
        }
        if (to.isEmpty()) {
            throw new Exception("No se indicó ningún destinatario");
        }
        MailMessage message = MailMessage.create()
                .subject(Fn.nvl(subject, document.getFileName()))
                .addAttachment(document.getFileName(), document.getContentType(),
                        document.getContent());
        for (String address : to) {
            message.addTo(address);
        }
        for (String address : cc) {
            message.addCc(address);
        }
        for (String address : bcc) {
            message.addBcc(address);
        }
        if (!Strings.isNullorEmpty(text)) {
            message.text(text);
        }
        if (!Strings.isNullorEmpty(html)) {
            message.html(html);
        }
        IMailSendResult sendResult = new MailSender().send(account, message);
        if (sendResult.isSuccessful()) {
            return new ErrorReg();
        }
        IErrorReg errorReg = sendResult.getErrorReg();
        if (errorReg == null) {
            errorReg = new ErrorReg();
            errorReg.setErrorNumber(1);
            errorReg.setMessage("El envío del correo falló");
        }
        return errorReg;
    }

    @Override
    public String getChannelName() {
        return CHANNEL;
    }
}

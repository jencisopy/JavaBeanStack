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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.javabeanstack.messaging.IMailAddress;
import org.javabeanstack.messaging.IMailMessage;
import org.javabeanstack.messaging.IMessageAttachment;

/**
 * Implementación en memoria de {@link IMailMessage}: el mensaje de correo que la
 * Fase 2 arma y envía <b>sin base de datos</b>. Es lo que instancia, por ejemplo,
 * el rediseño de autenticación para mandar el enlace de activación.
 *
 * <p>Trae métodos de conveniencia ({@link #addTo}, {@link #addCc}, {@link #addBcc},
 * {@link #addAttachment}) y un constructor fluido a través de {@link #create()}
 * para el caso común. En la Fase 3, la entidad JPA {@code AppMsgMail} implementa
 * el mismo contrato para almacenar los mensajes.</p>
 *
 * @author Jorge Enciso
 */
public class MailMessage implements IMailMessage {

    // IMessage
    private Long id;
    private Long idcompany;
    private String channel = CHANNEL_MAIL;
    private String folder;
    private String status;
    private String direction = DIRECTION_OUTBOUND;
    private Integer priority = 5;
    private String subject;
    private String bodyText;
    private LocalDateTime messageDate;
    private LocalDateTime sentDate;
    private LocalDateTime scheduledDate;
    private Integer retryCount = 0;
    private String errorMessage;
    private Boolean read = false;
    private Long idUser;
    private Long idReference;
    private String threadId;
    private final List<IMailAddress> addressList = new ArrayList<>();
    private final List<IMessageAttachment> attachmentList = new ArrayList<>();

    // IMailMessage
    private Long idMailCredential;
    private String fromAddress;
    private String fromName;
    private String replyTo;
    private String toSummary;
    private String bodyHtml;
    private String headers;
    private String messageId;
    private String inReplyTo;
    private Long uidValidity;
    private Long uid;
    private Long size;
    private Boolean hasAttachments = false;

    /**
     * Constructor por defecto. Nace como correo saliente ({@code channel = MA},
     * {@code direction = S}).
     */
    public MailMessage() {
    }

    /**
     * Crea un mensaje de correo vacío para componerlo de forma fluida.
     *
     * @return un mensaje nuevo.
     */
    public static MailMessage create() {
        return new MailMessage();
    }

    /**
     * Fija el remitente.
     *
     * @param address dirección del remitente.
     * @param name nombre visible, o nulo.
     * @return este mensaje.
     */
    public MailMessage from(String address, String name) {
        this.fromAddress = address;
        this.fromName = name;
        return this;
    }

    /**
     * Agrega un destinatario principal ({@code To}).
     *
     * @param address dirección del destinatario.
     * @return este mensaje.
     */
    public MailMessage addTo(String address) {
        return addTo(address, null);
    }

    /**
     * Agrega un destinatario principal ({@code To}) con nombre.
     *
     * @param address dirección del destinatario.
     * @param name nombre visible.
     * @return este mensaje.
     */
    public MailMessage addTo(String address, String name) {
        addressList.add(new MailAddress(IMailAddress.TYPE_TO, address, name));
        return this;
    }

    /**
     * Agrega un destinatario en copia ({@code Cc}).
     *
     * @param address dirección del destinatario.
     * @return este mensaje.
     */
    public MailMessage addCc(String address) {
        addressList.add(new MailAddress(IMailAddress.TYPE_CC, address, null));
        return this;
    }

    /**
     * Agrega un destinatario en copia oculta ({@code Bcc}).
     *
     * @param address dirección del destinatario.
     * @return este mensaje.
     */
    public MailMessage addBcc(String address) {
        addressList.add(new MailAddress(IMailAddress.TYPE_BCC, address, null));
        return this;
    }

    /**
     * Fija el asunto.
     *
     * @param subject asunto del mensaje.
     * @return este mensaje.
     */
    public MailMessage subject(String subject) {
        this.subject = subject;
        return this;
    }

    /**
     * Fija el cuerpo en texto plano.
     *
     * @param text cuerpo en texto plano.
     * @return este mensaje.
     */
    public MailMessage text(String text) {
        this.bodyText = text;
        return this;
    }

    /**
     * Fija el cuerpo en HTML.
     *
     * @param html cuerpo HTML.
     * @return este mensaje.
     */
    public MailMessage html(String html) {
        this.bodyHtml = html;
        return this;
    }

    /**
     * Agrega un adjunto.
     *
     * @param filename nombre del archivo.
     * @param contentType tipo de contenido (MIME).
     * @param content binario del adjunto.
     * @return este mensaje.
     */
    public MailMessage addAttachment(String filename, String contentType, byte[] content) {
        attachmentList.add(new MailAttachment(filename, contentType, content));
        this.hasAttachments = true;
        return this;
    }

    // ----------------------------------------------------------------- IMessage

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getIdcompany() {
        return idcompany;
    }

    @Override
    public void setIdcompany(Long idcompany) {
        this.idcompany = idcompany;
    }

    @Override
    public String getChannel() {
        return channel;
    }

    /**
     * Asigna el canal del mensaje.
     * @param channel canal del mensaje.
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    @Override
    public String getFolder() {
        return folder;
    }

    @Override
    public void setFolder(String folder) {
        this.folder = folder;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String getDirection() {
        return direction;
    }

    @Override
    public void setDirection(String direction) {
        this.direction = direction;
    }

    @Override
    public Integer getPriority() {
        return priority;
    }

    @Override
    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    @Override
    public String getSubject() {
        return subject;
    }

    @Override
    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Override
    public String getBodyText() {
        return bodyText;
    }

    @Override
    public void setBodyText(String bodyText) {
        this.bodyText = bodyText;
    }

    @Override
    public LocalDateTime getMessageDate() {
        return messageDate;
    }

    @Override
    public void setMessageDate(LocalDateTime messageDate) {
        this.messageDate = messageDate;
    }

    @Override
    public LocalDateTime getSentDate() {
        return sentDate;
    }

    @Override
    public void setSentDate(LocalDateTime sentDate) {
        this.sentDate = sentDate;
    }

    @Override
    public LocalDateTime getScheduledDate() {
        return scheduledDate;
    }

    @Override
    public void setScheduledDate(LocalDateTime scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    @Override
    public Integer getRetryCount() {
        return retryCount;
    }

    @Override
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public Boolean getRead() {
        return read;
    }

    @Override
    public void setRead(Boolean read) {
        this.read = read;
    }

    @Override
    public Long getIdUser() {
        return idUser;
    }

    @Override
    public void setIdUser(Long idUser) {
        this.idUser = idUser;
    }

    @Override
    public Long getIdReference() {
        return idReference;
    }

    @Override
    public void setIdReference(Long idReference) {
        this.idReference = idReference;
    }

    @Override
    public String getThreadId() {
        return threadId;
    }

    @Override
    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    @Override
    public List<IMailAddress> getAddressList() {
        return addressList;
    }

    @Override
    public List<IMessageAttachment> getAttachmentList() {
        return attachmentList;
    }

    // ------------------------------------------------------------- IMailMessage

    @Override
    public Long getIdMailCredential() {
        return idMailCredential;
    }

    @Override
    public void setIdMailCredential(Long idMailCredential) {
        this.idMailCredential = idMailCredential;
    }

    @Override
    public String getFromAddress() {
        return fromAddress;
    }

    @Override
    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    @Override
    public String getFromName() {
        return fromName;
    }

    @Override
    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    @Override
    public String getReplyTo() {
        return replyTo;
    }

    @Override
    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    @Override
    public String getToSummary() {
        return toSummary;
    }

    @Override
    public void setToSummary(String toSummary) {
        this.toSummary = toSummary;
    }

    @Override
    public String getBodyHtml() {
        return bodyHtml;
    }

    @Override
    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    @Override
    public String getHeaders() {
        return headers;
    }

    @Override
    public void setHeaders(String headers) {
        this.headers = headers;
    }

    @Override
    public String getMessageId() {
        return messageId;
    }

    @Override
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    @Override
    public String getInReplyTo() {
        return inReplyTo;
    }

    @Override
    public void setInReplyTo(String inReplyTo) {
        this.inReplyTo = inReplyTo;
    }

    @Override
    public Long getUidValidity() {
        return uidValidity;
    }

    @Override
    public void setUidValidity(Long uidValidity) {
        this.uidValidity = uidValidity;
    }

    @Override
    public Long getUid() {
        return uid;
    }

    @Override
    public void setUid(Long uid) {
        this.uid = uid;
    }

    @Override
    public Long getSize() {
        return size;
    }

    @Override
    public void setSize(Long size) {
        this.size = size;
    }

    @Override
    public Boolean getHasAttachments() {
        return hasAttachments;
    }

    @Override
    public void setHasAttachments(Boolean hasAttachments) {
        this.hasAttachments = hasAttachments;
    }
}

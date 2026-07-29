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
package org.javabeanstack.messaging;

/**
 * Contrato del mensaje de correo electrónico: el dato completo que se envía o se
 * recibe. Especializa {@link IMessage} agregando lo propio del protocolo
 * (remitente, respuesta, cuerpo HTML, cabeceras crudas e identificadores de
 * RFC 5322 y de IMAP).
 *
 * <p>Los destinatarios {@code To}, {@code Cc} y {@code Bcc} no son propiedades
 * de este contrato sino elementos de {@link #getAddressList()} discriminados por
 * {@link IMailAddress#getType()}: un mensaje puede tener N de cada tipo y cada
 * uno lleva su propio estado de entrega.</p>
 *
 * <p>La conversión hacia y desde {@code jakarta.mail.internet.MimeMessage} la
 * hace la capa de implementación; ninguna implementación de este contrato debe
 * conocer MIME. Un objeto en memoria que implemente esta interfaz alcanza para
 * enviar un correo sin base de datos.</p>
 *
 * @author Jorge Enciso
 */
public interface IMailMessage extends IMessage {

    /**
     * Devuelve la cuenta de correo por la que el mensaje entra o sale.
     * @return identificador de la cuenta, o nulo para usar la cuenta por defecto
     *         de la empresa.
     */
    Long getIdMailCredential();

    /**
     * Asigna la cuenta de correo por la que el mensaje entra o sale.
     * @param idMailCredential identificador de la cuenta.
     */
    void setIdMailCredential(Long idMailCredential);

    /**
     * Devuelve la dirección del remitente (cabecera {@code From}).
     * @return dirección de correo del remitente.
     */
    String getFromAddress();

    /**
     * Asigna la dirección del remitente.
     * @param fromAddress dirección de correo del remitente.
     */
    void setFromAddress(String fromAddress);

    /**
     * Devuelve el nombre visible del remitente.
     * @return nombre del remitente, o nulo.
     */
    String getFromName();

    /**
     * Asigna el nombre visible del remitente.
     * @param fromName nombre del remitente.
     */
    void setFromName(String fromName);

    /**
     * Devuelve la dirección de respuesta (cabecera {@code Reply-To}).
     * @return dirección de respuesta, o nulo.
     */
    String getReplyTo();

    /**
     * Asigna la dirección de respuesta.
     * @param replyTo dirección de respuesta.
     */
    void setReplyTo(String replyTo);

    /**
     * Devuelve el resumen desnormalizado de los destinatarios {@code To}, usado
     * para pintar la bandeja sin recorrer {@link #getAddressList()}.
     * @return resumen de destinatarios, o nulo.
     */
    String getToSummary();

    /**
     * Asigna el resumen desnormalizado de los destinatarios {@code To}.
     * @param toSummary resumen de destinatarios.
     */
    void setToSummary(String toSummary);

    /**
     * Devuelve el cuerpo del mensaje en HTML.
     * @return cuerpo HTML, o nulo si el mensaje es sólo texto.
     */
    String getBodyHtml();

    /**
     * Asigna el cuerpo del mensaje en HTML.
     * @param bodyHtml cuerpo HTML.
     */
    void setBodyHtml(String bodyHtml);

    /**
     * Devuelve las cabeceras crudas del mensaje recibido.
     * @return cabeceras del mensaje, o nulo si el mensaje es saliente.
     */
    String getHeaders();

    /**
     * Asigna las cabeceras crudas del mensaje.
     * @param headers cabeceras del mensaje.
     */
    void setHeaders(String headers);

    /**
     * Devuelve el identificador único del mensaje (cabecera {@code Message-ID},
     * RFC 5322).
     * @return identificador del mensaje, o nulo si aún no se envió.
     */
    String getMessageId();

    /**
     * Asigna el identificador único del mensaje.
     * @param messageId identificador del mensaje.
     */
    void setMessageId(String messageId);

    /**
     * Devuelve el identificador del mensaje al que responde (cabecera
     * {@code In-Reply-To}).
     * @return identificador del mensaje respondido, o nulo.
     */
    String getInReplyTo();

    /**
     * Asigna el identificador del mensaje al que responde.
     * @param inReplyTo identificador del mensaje respondido.
     */
    void setInReplyTo(String inReplyTo);

    /**
     * Devuelve el {@code UIDVALIDITY} de la carpeta IMAP desde la que se descargó
     * el mensaje. Junto con {@link #getUid()} garantiza que una segunda pasada
     * del receptor no duplique el mensaje.
     * @return uidvalidity de la carpeta, o nulo si el mensaje no vino por IMAP.
     */
    Long getUidValidity();

    /**
     * Asigna el {@code UIDVALIDITY} de la carpeta IMAP.
     * @param uidValidity uidvalidity de la carpeta.
     */
    void setUidValidity(Long uidValidity);

    /**
     * Devuelve el UID del mensaje dentro de la carpeta IMAP.
     * @return uid del mensaje, o nulo si el mensaje no vino por IMAP.
     */
    Long getUid();

    /**
     * Asigna el UID del mensaje dentro de la carpeta IMAP.
     * @param uid uid del mensaje.
     */
    void setUid(Long uid);

    /**
     * Devuelve el tamaño total del mensaje en bytes.
     * @return tamaño en bytes, o nulo si no se calculó.
     */
    Long getSize();

    /**
     * Asigna el tamaño total del mensaje en bytes.
     * @param size tamaño en bytes.
     */
    void setSize(Long size);

    /**
     * Indica si el mensaje tiene adjuntos, sin necesidad de cargarlos.
     * @return verdadero si tiene al menos un adjunto.
     */
    Boolean getHasAttachments();

    /**
     * Asigna si el mensaje tiene adjuntos.
     * @param hasAttachments verdadero si tiene al menos un adjunto.
     */
    void setHasAttachments(Boolean hasAttachments);
}

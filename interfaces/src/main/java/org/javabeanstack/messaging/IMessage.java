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

import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrato genérico de un mensaje, independiente del canal por el que viaja
 * (correo, SMS, mensajería interna, push, WhatsApp). Es la raíz de la
 * abstracción multicanal: el correo se representa con {@link IMailMessage}, que
 * especializa este contrato.
 *
 * <p>Es un contrato <b>puro</b>, sin relación con la persistencia: no extiende
 * la jerarquía de entidades del framework. Lo implementa tanto un objeto en
 * memoria (para enviar sin base de datos) como una entidad JPA (para almacenar).
 * Cada canal tiene su propia entidad/tabla; en Maker el correo es
 * {@code AppMsgMail} sobre {@code datos.AppMsgMail}.</p>
 *
 * <p>La ubicación visible del mensaje la da {@link #getFolder()} (bandeja) y la
 * situación de proceso, {@link #getStatus()}. Son conceptos ortogonales: un
 * mensaje en la bandeja de salida puede estar encolado, en proceso o en error.
 * Un objeto usado sólo para enviar (sin almacenar) puede dejar en nulo los
 * campos de ciclo de vida.</p>
 *
 * @author Jorge Enciso
 */
public interface IMessage {

    /** Canal correo electrónico. */
    String CHANNEL_MAIL = "MA";
    /** Canal SMS. */
    String CHANNEL_SMS = "SM";
    /** Canal mensajería interna. */
    String CHANNEL_INTERNAL = "IN";
    /** Canal notificación push. */
    String CHANNEL_PUSH = "PU";
    /** Canal WhatsApp. */
    String CHANNEL_WHATSAPP = "WA";

    /** Bandeja de entrada. */
    String FOLDER_INBOX = "IN";
    /** Bandeja de salida (cola de despacho). */
    String FOLDER_OUTBOX = "OU";
    /** Borradores. */
    String FOLDER_DRAFTS = "DR";
    /** Enviados. */
    String FOLDER_SENT = "SE";
    /** Papelera. */
    String FOLDER_TRASH = "TR";

    /** Borrador: editable, no se despacha. */
    String STATUS_DRAFT = "BO";
    /** En cola: listo para despachar. */
    String STATUS_QUEUED = "EC";
    /** En proceso: tomado por un despachador. */
    String STATUS_SENDING = "EP";
    /** Enviado con éxito. */
    String STATUS_SENT = "EN";
    /** Error recuperable: se reintentará. */
    String STATUS_ERROR = "ER";
    /** Fallado definitivo: agotó los reintentos. */
    String STATUS_FAILED = "EF";
    /** Recibido. */
    String STATUS_RECEIVED = "RE";

    /** Dirección entrante. */
    String DIRECTION_INBOUND = "E";
    /** Dirección saliente. */
    String DIRECTION_OUTBOUND = "S";

    /**
     * Devuelve el identificador del mensaje.
     * @return identificador del mensaje, o nulo si aún no se persistió.
     */
    Long getId();

    /**
     * Asigna el identificador del mensaje.
     * @param id identificador del mensaje.
     */
    void setId(Long id);

    /**
     * Devuelve el identificador de la empresa dueña del mensaje.
     * @return identificador de la empresa.
     */
    Long getIdcompany();

    /**
     * Asigna el identificador de la empresa dueña del mensaje.
     * @param idcompany identificador de la empresa.
     */
    void setIdcompany(Long idcompany);

    /**
     * Devuelve el canal por el que viaja el mensaje.
     * @return canal ({@link #CHANNEL_MAIL} y demás).
     */
    String getChannel();

    /**
     * Devuelve la carpeta (bandeja) en la que está ubicado el mensaje.
     * @return carpeta ({@link #FOLDER_INBOX} y demás), o nulo si no está almacenado.
     */
    String getFolder();

    /**
     * Asigna la carpeta (bandeja) del mensaje.
     * @param folder carpeta destino.
     */
    void setFolder(String folder);

    /**
     * Devuelve el estado de proceso del mensaje.
     * @return estado ({@link #STATUS_DRAFT} y demás), o nulo si no está almacenado.
     */
    String getStatus();

    /**
     * Asigna el estado de proceso del mensaje.
     * @param status estado del mensaje.
     */
    void setStatus(String status);

    /**
     * Devuelve la dirección del mensaje.
     * @return {@link #DIRECTION_INBOUND} si es entrante, {@link #DIRECTION_OUTBOUND} si es saliente.
     */
    String getDirection();

    /**
     * Asigna la dirección del mensaje.
     * @param direction dirección del mensaje.
     */
    void setDirection(String direction);

    /**
     * Devuelve la prioridad de despacho (1 = más alta, 9 = más baja, 5 por defecto).
     * @return prioridad de despacho.
     */
    Integer getPriority();

    /**
     * Asigna la prioridad de despacho.
     * @param priority prioridad de despacho.
     */
    void setPriority(Integer priority);

    /**
     * Devuelve el asunto o título del mensaje.
     * @return asunto del mensaje.
     */
    String getSubject();

    /**
     * Asigna el asunto o título del mensaje.
     * @param subject asunto del mensaje.
     */
    void setSubject(String subject);

    /**
     * Devuelve el cuerpo del mensaje en texto plano.
     * @return cuerpo en texto plano, o nulo si el mensaje sólo tiene versión HTML.
     */
    String getBodyText();

    /**
     * Asigna el cuerpo del mensaje en texto plano.
     * @param bodyText cuerpo en texto plano.
     */
    void setBodyText(String bodyText);

    /**
     * Devuelve la fecha propia del mensaje (la del encabezado, si es entrante).
     * @return fecha del mensaje.
     */
    LocalDateTime getMessageDate();

    /**
     * Asigna la fecha propia del mensaje.
     * @param messageDate fecha del mensaje.
     */
    void setMessageDate(LocalDateTime messageDate);

    /**
     * Devuelve la fecha efectiva de envío.
     * @return fecha de envío, o nulo si aún no se envió.
     */
    LocalDateTime getSentDate();

    /**
     * Asigna la fecha efectiva de envío.
     * @param sentDate fecha de envío.
     */
    void setSentDate(LocalDateTime sentDate);

    /**
     * Devuelve la fecha a partir de la cual el mensaje puede despacharse.
     * @return fecha programada, o nulo para despachar cuanto antes.
     */
    LocalDateTime getScheduledDate();

    /**
     * Asigna la fecha programada de despacho.
     * @param scheduledDate fecha programada.
     */
    void setScheduledDate(LocalDateTime scheduledDate);

    /**
     * Devuelve la cantidad de intentos de envío ya realizados.
     * @return cantidad de intentos.
     */
    Integer getRetryCount();

    /**
     * Asigna la cantidad de intentos de envío realizados.
     * @param retryCount cantidad de intentos.
     */
    void setRetryCount(Integer retryCount);

    /**
     * Devuelve el mensaje del último error de envío.
     * @return descripción del error, o nulo si no hubo.
     */
    String getErrorMessage();

    /**
     * Asigna el mensaje del último error de envío.
     * @param errorMessage descripción del error.
     */
    void setErrorMessage(String errorMessage);

    /**
     * Indica si el mensaje fue leído por su destinatario en la aplicación.
     * @return verdadero si fue leído.
     */
    Boolean getRead();

    /**
     * Asigna si el mensaje fue leído.
     * @param read verdadero si fue leído.
     */
    void setRead(Boolean read);

    /**
     * Devuelve el identificador del usuario dueño de la bandeja (relación lógica
     * con el usuario de la aplicación; sin clave foránea porque cruza esquemas).
     * @return identificador del usuario, o nulo si el mensaje es de la empresa.
     */
    Long getIdUser();

    /**
     * Asigna el identificador del usuario dueño de la bandeja.
     * @param idUser identificador del usuario.
     */
    void setIdUser(Long idUser);

    /**
     * Devuelve el mensaje al que este responde o del que es reenvío.
     * @return identificador del mensaje referenciado, o nulo.
     */
    Long getIdReference();

    /**
     * Asigna el mensaje referenciado (respuesta o reenvío).
     * @param idReference identificador del mensaje referenciado.
     */
    void setIdReference(Long idReference);

    /**
     * Devuelve el identificador del hilo de conversación.
     * @return identificador del hilo, o nulo.
     */
    String getThreadId();

    /**
     * Asigna el identificador del hilo de conversación.
     * @param threadId identificador del hilo.
     */
    void setThreadId(String threadId);

    /**
     * Devuelve la lista de destinatarios del mensaje.
     * @return lista de destinatarios; nunca nula.
     */
    List<? extends IMailAddress> getAddressList();

    /**
     * Devuelve la lista de adjuntos del mensaje.
     * @return lista de adjuntos; nunca nula.
     */
    List<? extends IMessageAttachment> getAttachmentList();
}

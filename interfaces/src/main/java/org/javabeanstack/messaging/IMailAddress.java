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
 * Contrato de un destinatario de un mensaje. Un mensaje tiene N destinatarios;
 * el rol de cada uno ({@code To}, {@code Cc}, {@code Bcc}, remitente o dirección
 * de respuesta) lo discrimina {@link #getType()}.
 *
 * <p>El campo {@link #getAddress()} es genérico por canal: en correo es una
 * dirección de correo, en SMS un número telefónico, en mensajería interna el
 * código de usuario. Es un contrato puro, sin relación con la persistencia.</p>
 *
 * @author Jorge Enciso
 */
public interface IMailAddress {

    /** Destinatario principal. */
    String TYPE_TO = "TO";
    /** Destinatario en copia. */
    String TYPE_CC = "CC";
    /** Destinatario en copia oculta. */
    String TYPE_BCC = "BCC";
    /** Remitente. */
    String TYPE_FROM = "FRM";
    /** Dirección de respuesta. */
    String TYPE_REPLYTO = "RPT";

    /** Entregado. */
    String STATUS_DELIVERED = "EN";
    /** Rebotado. */
    String STATUS_BOUNCED = "RB";
    /** Bloqueado. */
    String STATUS_BLOCKED = "BL";

    /**
     * Devuelve el identificador del destinatario.
     * @return identificador del destinatario, o nulo si aún no se persistió.
     */
    Long getId();

    /**
     * Asigna el identificador del destinatario.
     * @param id identificador del destinatario.
     */
    void setId(Long id);

    /**
     * Devuelve el tipo de destinatario.
     * @return tipo ({@link #TYPE_TO}, {@link #TYPE_CC}, {@link #TYPE_BCC},
     *         {@link #TYPE_FROM}, {@link #TYPE_REPLYTO}).
     */
    String getType();

    /**
     * Asigna el tipo de destinatario.
     * @param type tipo de destinatario.
     */
    void setType(String type);

    /**
     * Devuelve el orden del destinatario dentro de su tipo, para conservar el
     * orden de la lista original.
     * @return orden del destinatario.
     */
    Integer getOrder();

    /**
     * Asigna el orden del destinatario.
     * @param order orden del destinatario.
     */
    void setOrder(Integer order);

    /**
     * Devuelve la dirección del destinatario (correo, teléfono o usuario según el
     * canal).
     * @return dirección del destinatario.
     */
    String getAddress();

    /**
     * Asigna la dirección del destinatario.
     * @param address dirección del destinatario.
     */
    void setAddress(String address);

    /**
     * Devuelve el nombre visible del destinatario.
     * @return nombre del destinatario, o nulo.
     */
    String getName();

    /**
     * Asigna el nombre visible del destinatario.
     * @param name nombre del destinatario.
     */
    void setName(String name);

    /**
     * Devuelve el estado de entrega del destinatario.
     * @return estado ({@link #STATUS_DELIVERED}, {@link #STATUS_BOUNCED},
     *         {@link #STATUS_BLOCKED}), o nulo si no se procesó.
     */
    String getStatus();

    /**
     * Asigna el estado de entrega del destinatario.
     * @param status estado de entrega.
     */
    void setStatus(String status);

    /**
     * Devuelve el mensaje de error de entrega de este destinatario.
     * @return descripción del error, o nulo.
     */
    String getErrorMessage();

    /**
     * Asigna el mensaje de error de entrega de este destinatario.
     * @param errorMessage descripción del error.
     */
    void setErrorMessage(String errorMessage);
}

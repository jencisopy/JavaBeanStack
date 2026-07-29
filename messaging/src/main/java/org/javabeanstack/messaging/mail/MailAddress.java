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

import org.javabeanstack.messaging.IMailAddress;

/**
 * Implementación en memoria de {@link IMailAddress}: un destinatario de correo
 * con su tipo, dirección y nombre. La usa la Fase 2 para armar y enviar mensajes
 * sin base de datos; en la Fase 3 la entidad JPA {@code AppMsgMailAddress}
 * implementa el mismo contrato para almacenarlos.
 *
 * @author Jorge Enciso
 */
public class MailAddress implements IMailAddress {

    private Long id;
    private String type;
    private Integer order;
    private String address;
    private String name;
    private String status;
    private String errorMessage;

    /**
     * Constructor por defecto.
     */
    public MailAddress() {
    }

    /**
     * Crea un destinatario con su tipo, dirección y nombre.
     *
     * @param type tipo de destinatario ({@link #TYPE_TO} y demás).
     * @param address dirección de correo.
     * @param name nombre visible, o nulo.
     */
    public MailAddress(String type, String address, String name) {
        this.type = type;
        this.address = address;
        this.name = name;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public Integer getOrder() {
        return order;
    }

    @Override
    public void setOrder(Integer order) {
        this.order = order;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
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
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

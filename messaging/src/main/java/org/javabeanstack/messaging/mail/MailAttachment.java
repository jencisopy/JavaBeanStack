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

import org.javabeanstack.messaging.IMessageAttachment;

/**
 * Implementación en memoria de {@link IMessageAttachment}: un adjunto de correo
 * con su binario. La usa la Fase 2 para enviar sin base de datos; nace con
 * {@code storage = 'D'} (el binario viaja en memoria). En la Fase 3 la entidad
 * JPA {@code AppMsgMailAttachment} implementa el mismo contrato para almacenarlos.
 *
 * @author Jorge Enciso
 */
public class MailAttachment implements IMessageAttachment {

    private Long id;
    private Integer order;
    private String filename;
    private String contentType;
    private Long size;
    private String contentId;
    private Boolean inline = false;
    private String storage = STORAGE_DB;
    private byte[] content;
    private String uri;
    private String hash;

    /**
     * Constructor por defecto.
     */
    public MailAttachment() {
    }

    /**
     * Crea un adjunto con su nombre, tipo de contenido y binario.
     *
     * @param filename nombre del archivo.
     * @param contentType tipo de contenido (MIME).
     * @param content binario del adjunto.
     */
    public MailAttachment(String filename, String contentType, byte[] content) {
        this.filename = filename;
        this.contentType = contentType;
        this.content = content;
        this.size = (content == null) ? null : (long) content.length;
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
    public Integer getOrder() {
        return order;
    }

    @Override
    public void setOrder(Integer order) {
        this.order = order;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
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
    public String getContentId() {
        return contentId;
    }

    @Override
    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    @Override
    public Boolean getInline() {
        return inline;
    }

    @Override
    public void setInline(Boolean inline) {
        this.inline = inline;
    }

    @Override
    public String getStorage() {
        return storage;
    }

    @Override
    public void setStorage(String storage) {
        this.storage = storage;
    }

    @Override
    public byte[] getContent() {
        return content;
    }

    @Override
    public void setContent(byte[] content) {
        this.content = content;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public String getHash() {
        return hash;
    }

    @Override
    public void setHash(String hash) {
        this.hash = hash;
    }
}

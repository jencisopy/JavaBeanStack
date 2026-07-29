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
 * Contrato de un adjunto de un mensaje. El binario puede vivir en la propia base
 * ({@link #STORAGE_DB}, columna {@link #getContent()}) o fuera de ella
 * ({@link #STORAGE_FILE} sistema de archivos, {@link #STORAGE_S3} bucket S3,
 * columna {@link #getUri()}); el contrato expone ambas puertas y quien lo
 * consuma debe mirar {@link #getStorage()} antes de leer.
 *
 * <p>Es un contrato puro, sin relación con la persistencia.</p>
 *
 * @author Jorge Enciso
 */
public interface IMessageAttachment {

    /** Binario almacenado en la base de datos. */
    String STORAGE_DB = "D";
    /** Binario almacenado en el sistema de archivos. */
    String STORAGE_FILE = "F";
    /** Binario almacenado en un bucket S3. */
    String STORAGE_S3 = "S";
    /** Binario purgado: se conserva la fila de auditoría sin el contenido. */
    String STORAGE_PURGED = "P";

    /**
     * Devuelve el identificador del adjunto.
     * @return identificador del adjunto, o nulo si aún no se persistió.
     */
    Long getId();

    /**
     * Asigna el identificador del adjunto.
     * @param id identificador del adjunto.
     */
    void setId(Long id);

    /**
     * Devuelve el orden del adjunto dentro del mensaje.
     * @return orden del adjunto.
     */
    Integer getOrder();

    /**
     * Asigna el orden del adjunto dentro del mensaje.
     * @param order orden del adjunto.
     */
    void setOrder(Integer order);

    /**
     * Devuelve el nombre del archivo adjunto.
     * @return nombre del archivo.
     */
    String getFilename();

    /**
     * Asigna el nombre del archivo adjunto.
     * @param filename nombre del archivo.
     */
    void setFilename(String filename);

    /**
     * Devuelve el tipo de contenido (MIME) del adjunto.
     * @return tipo de contenido, o nulo.
     */
    String getContentType();

    /**
     * Asigna el tipo de contenido (MIME) del adjunto.
     * @param contentType tipo de contenido.
     */
    void setContentType(String contentType);

    /**
     * Devuelve el tamaño del adjunto en bytes.
     * @return tamaño en bytes, o nulo.
     */
    Long getSize();

    /**
     * Asigna el tamaño del adjunto en bytes.
     * @param size tamaño en bytes.
     */
    void setSize(Long size);

    /**
     * Devuelve el identificador de contenido ({@code cid:}) usado para referenciar
     * el adjunto desde el cuerpo HTML (imágenes embebidas).
     * @return identificador de contenido, o nulo si no es un adjunto embebido.
     */
    String getContentId();

    /**
     * Asigna el identificador de contenido ({@code cid:}).
     * @param contentId identificador de contenido.
     */
    void setContentId(String contentId);

    /**
     * Indica si el adjunto es embebido (inline) en el cuerpo HTML.
     * @return verdadero si es embebido.
     */
    Boolean getInline();

    /**
     * Asigna si el adjunto es embebido en el cuerpo HTML.
     * @param inline verdadero si es embebido.
     */
    void setInline(Boolean inline);

    /**
     * Devuelve dónde está almacenado el binario del adjunto.
     * @return almacenamiento ({@link #STORAGE_DB}, {@link #STORAGE_FILE},
     *         {@link #STORAGE_S3}, {@link #STORAGE_PURGED}).
     */
    String getStorage();

    /**
     * Asigna dónde está almacenado el binario del adjunto.
     * @param storage almacenamiento del binario.
     */
    void setStorage(String storage);

    /**
     * Devuelve el binario del adjunto cuando está almacenado en la base
     * ({@link #STORAGE_DB}).
     * @return contenido del adjunto, o nulo si está almacenado fuera de la base.
     */
    byte[] getContent();

    /**
     * Asigna el binario del adjunto.
     * @param content contenido del adjunto.
     */
    void setContent(byte[] content);

    /**
     * Devuelve la ubicación externa del binario cuando no está en la base
     * ({@link #STORAGE_FILE} o {@link #STORAGE_S3}).
     * @return ubicación del binario, o nulo si está en la base.
     */
    String getUri();

    /**
     * Asigna la ubicación externa del binario.
     * @param uri ubicación del binario.
     */
    void setUri(String uri);

    /**
     * Devuelve el hash del contenido (SHA-256 en hexadecimal), para deduplicar y
     * detectar corrupción.
     * @return hash del contenido, o nulo.
     */
    String getHash();

    /**
     * Asigna el hash del contenido.
     * @param hash hash del contenido.
     */
    void setHash(String hash);
}

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
package org.javabeanstack.outputs;

/**
 * Contrato del documento generado en memoria: el resultado de una
 * {@link IDocumentSource} (merge de una plantilla Word, exportación de un
 * reporte), listo para entregarse a uno o más {@link IDocumentTarget} —
 * descarga HTTP, adjunto de correo, carpeta del servidor, base de datos.
 *
 * <p>
 * Generar el documento como un objeto completo, en lugar de escribirlo
 * directamente sobre la respuesta HTTP, es lo que permite que un mismo
 * artefacto tenga varios destinos y que un fallo de generación no deje una
 * descarga a medio escribir.
 * </p>
 *
 * <p>
 * Sus propiedades calzan con las de
 * {@link org.javabeanstack.messaging.IMessageAttachment} (nombre, contenido,
 * content-type), de modo que convertirlo en adjunto de un mensaje es una copia
 * directa de valores.
 * </p>
 *
 * <p>Es un contrato puro, sin relación con la persistencia ni con la web.</p>
 *
 * @author Jorge Enciso
 */
public interface IOutputDocument {

    /** Documento Word (.docx). */
    String FORMAT_DOCX = "docx";
    /** Documento PDF. */
    String FORMAT_PDF = "pdf";
    /** Planilla Excel (.xlsx). */
    String FORMAT_XLSX = "xlsx";
    /** Página HTML. */
    String FORMAT_HTML = "html";

    /**
     * Devuelve el nombre del archivo con su extensión, tal como debe llegar al
     * destino (el {@code filename} de la descarga o del adjunto).
     *
     * @return nombre del archivo (ej. {@code transferencia_12.docx}).
     */
    String getFileName();

    /**
     * Asigna el nombre del archivo con su extensión.
     *
     * @param fileName nombre del archivo.
     */
    void setFileName(String fileName);

    /**
     * Devuelve el contenido binario completo del documento.
     *
     * @return bytes del documento generado.
     */
    byte[] getContent();

    /**
     * Asigna el contenido binario del documento.
     *
     * @param content bytes del documento generado.
     */
    void setContent(byte[] content);

    /**
     * Devuelve el tipo MIME con el que el documento debe viajar (el
     * {@code Content-Type} de la descarga o del adjunto).
     *
     * @return tipo MIME (ej. {@code application/pdf}).
     */
    String getContentType();

    /**
     * Asigna el tipo MIME del documento.
     *
     * @param contentType tipo MIME.
     */
    void setContentType(String contentType);

    /**
     * Devuelve el formato del documento, uno de los valores {@code FORMAT_*}
     * de esta interfaz.
     *
     * @return formato del documento.
     */
    String getFormat();

    /**
     * Asigna el formato del documento, uno de los valores {@code FORMAT_*} de
     * esta interfaz.
     *
     * @param format formato del documento.
     */
    void setFormat(String format);

    /**
     * Devuelve el tamaño del contenido en bytes. Es el valor a informar como
     * {@code Content-Length} en una descarga.
     *
     * @return tamaño en bytes, o 0 si aún no hay contenido.
     */
    long getSize();
}

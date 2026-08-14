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
 * Implementación en memoria de {@link IOutputDocument}: el artefacto que una
 * {@link IDocumentSource} produce y los {@link IDocumentTarget} entregan. Es un
 * POJO puro, sin relación con la persistencia ni con la web.
 *
 * @author Jorge Enciso
 */
public class OutputDocument implements IOutputDocument {

    private String fileName;
    private byte[] content;
    private String contentType;
    private String format;

    public OutputDocument() {
    }

    /**
     * Crea el documento completo.
     *
     * @param fileName nombre del archivo con su extensión.
     * @param content contenido binario.
     * @param format formato, uno de los valores {@code FORMAT_*} de
     * {@link IOutputDocument}; el content-type se deriva con
     * {@link #contentTypeFor(String)}.
     */
    public OutputDocument(String fileName, byte[] content, String format) {
        this.fileName = fileName;
        this.content = content;
        this.format = format;
        this.contentType = contentTypeFor(format);
    }

    /**
     * Devuelve el tipo MIME estándar de un formato de documento.
     *
     * @param format uno de los valores {@code FORMAT_*} de
     * {@link IOutputDocument}.
     * @return tipo MIME, o {@code application/octet-stream} si el formato no
     * es conocido.
     */
    public static String contentTypeFor(String format) {
        if (format == null) {
            return "application/octet-stream";
        }
        switch (format) {
            case FORMAT_PDF:
                return "application/pdf";
            case FORMAT_DOCX:
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case FORMAT_XLSX:
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case FORMAT_HTML:
                return "text/html;charset=UTF-8";
            default:
                return "application/octet-stream";
        }
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public void setFileName(String fileName) {
        this.fileName = fileName;
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
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getFormat() {
        return format;
    }

    @Override
    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public long getSize() {
        return content == null ? 0L : content.length;
    }

    @Override
    public String toString() {
        return "OutputDocument{fileName=" + fileName + ", format=" + format
                + ", size=" + getSize() + "}";
    }
}

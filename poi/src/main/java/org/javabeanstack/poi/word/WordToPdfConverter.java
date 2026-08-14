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
package org.javabeanstack.poi.word;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter;
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

/**
 * Convierte documentos Word (.docx) a <b>PDF</b>, en memoria. Es el paso que
 * permite que una plantilla Word mergeada ({@link WordTemplateSource} con
 * formato {@code pdf}) se entregue como PDF sin salir del proceso.
 *
 * <p>
 * El motor es XDocReport ({@code fr.opensagres.poi.xwpf.converter.pdf}), puro
 * Java sobre el mismo {@code XWPFDocument} de Apache POI que usa el merge — no
 * requiere LibreOffice ni ningún programa externo. La fidelidad es buena para
 * documentos de texto corrido y tablas simples (los contratos y constancias
 * del ERP); si un documento con diseño complejo saliera degradado, el
 * reemplazo aislado de esta clase es un conversor externo
 * (LibreOffice/jodconverter).
 * </p>
 *
 * @author Jorge Enciso
 */
public class WordToPdfConverter {

    private WordToPdfConverter() {
    }

    /**
     * Convierte un documento Word a PDF, todo en memoria.
     *
     * @param docx contenido del documento .docx.
     * @return contenido del PDF resultante.
     * @throws Exception si el documento no es un .docx válido o la conversión
     * falla.
     */
    public static byte[] convert(byte[] docx) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            return convert(document);
        }
    }

    /**
     * Convierte un documento Word ya abierto a PDF, en memoria. No cierra el
     * documento recibido.
     *
     * @param document documento Word abierto.
     * @return contenido del PDF resultante.
     * @throws Exception si la conversión falla.
     */
    public static byte[] convert(XWPFDocument document) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PdfConverter.getInstance().convert(document, buffer, PdfOptions.create());
        return buffer.toByteArray();
    }
}

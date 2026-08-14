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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.javabeanstack.io.IOUtil;
import org.javabeanstack.outputs.IDocumentSource;
import org.javabeanstack.outputs.IOutputDocument;
import org.javabeanstack.outputs.OutputDocument;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.Strings;

/**
 * Fuente de documentos del subsistema de salida
 * ({@code org.javabeanstack.outputs}) basada en <b>plantillas Word</b> (.docx):
 * ubica la plantilla, reemplaza sus marcadores <code>&lt;&lt;campo&gt;&gt;</code>
 * con el mapa de datos ({@link WordTemplateMerge}) y devuelve el documento en
 * memoria, sin tocar la respuesta HTTP.
 *
 * <p>
 * La plantilla se busca en cada ruta del {@code fileSystemPath} configurado
 * (subcarpeta {@code templates/}) y, como último recurso, en la carpeta de
 * plantillas dentro del artefacto desplegado que indique el llamador. Es la
 * misma resolución que usan los reportes Jasper del framework, escrita una
 * sola vez.
 * </p>
 *
 * <p>Uso típico desde un controller:</p>
 * <pre>
 * IDocumentSource source = new WordTemplateSource(
 *         fileSystemPath,          //rutas separadas por coma
 *         warTemplatesPath)        //getRealPath("/WEB-INF/classes/templates/")
 *     .template("gilote_transfersys.docx")
 *     .data(data)
 *     .fileName("transferencia_12.docx");
 * </pre>
 *
 * @author Jorge Enciso
 */
public class WordTemplateSource implements IDocumentSource {

    private final String fileSystemPath;
    private final String fallbackPath;
    private String templateName;
    private Map<String, String> data = new HashMap();
    private String fileName;
    private String format = IOutputDocument.FORMAT_DOCX;

    /**
     * Crea la fuente con las rutas donde buscar la plantilla.
     *
     * @param fileSystemPath rutas del file system separadas por coma (el valor
     * de {@code appConfig.getFileSystemPath(sessionId)}); en cada una se busca
     * la subcarpeta {@code templates/}.
     * @param fallbackPath carpeta de plantillas de último recurso, normalmente
     * la del artefacto desplegado
     * ({@code getRealPath("/WEB-INF/classes/templates/")}); puede ser nula.
     */
    public WordTemplateSource(String fileSystemPath, String fallbackPath) {
        this.fileSystemPath = fileSystemPath;
        this.fallbackPath = fallbackPath;
    }

    /**
     * Asigna el nombre de archivo de la plantilla (ej.
     * {@code gilote_transfersys.docx}).
     *
     * @param templateName nombre de la plantilla con su extensión.
     * @return esta instancia, para encadenar.
     */
    public WordTemplateSource template(String templateName) {
        this.templateName = templateName;
        return this;
    }

    /**
     * Asigna el mapa de datos del documento, indexado por nombre de marcador
     * (sin los delimitadores {@code <<>>}); los campos que la plantilla pida y
     * no estén en el mapa salen impresos como
     * {@link WordTemplateMerge#SIN_VALOR}.
     *
     * @param data mapa marcador → valor a imprimir.
     * @return esta instancia, para encadenar.
     */
    public WordTemplateSource data(Map<String, String> data) {
        this.data = data == null ? new HashMap() : data;
        return this;
    }

    /**
     * Asigna el formato del documento generado:
     * {@link IOutputDocument#FORMAT_DOCX} (por omisión) o
     * {@link IOutputDocument#FORMAT_PDF} — en ese caso, tras el merge la
     * plantilla se convierte con {@link WordToPdfConverter}.
     *
     * @param format formato de salida.
     * @return esta instancia, para encadenar.
     */
    public WordTemplateSource format(String format) {
        this.format = format;
        return this;
    }

    /**
     * Indica si la plantilla configurada existe en alguna de las rutas. Sirve
     * para habilitar o deshabilitar las opciones de salida que dependen de
     * ella, sin intentar generar.
     *
     * @return verdadero si la plantilla existe.
     */
    public boolean isTemplateAvailable() {
        return !Strings.isNullorEmpty(templateName) && resolveTemplate() != null;
    }

    /**
     * Asigna el nombre de archivo del documento generado; por omisión, el
     * nombre de la plantilla.
     *
     * @param fileName nombre del archivo con su extensión.
     * @return esta instancia, para encadenar.
     */
    public WordTemplateSource fileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    /**
     * Ubica la plantilla, hace el merge de los marcadores y devuelve el
     * documento en memoria.
     *
     * @return documento generado, en formato
     * {@link IOutputDocument#FORMAT_DOCX}.
     * @throws Exception si la plantilla no existe en ninguna de las rutas o el
     * merge falla.
     */
    @Override
    public IOutputDocument generate() throws Exception {
        if (Strings.isNullorEmpty(templateName)) {
            throw new Exception("No se indicó la plantilla del documento");
        }
        String templatePath = resolveTemplate();
        if (templatePath == null) {
            throw new Exception("No encontro la plantilla para generar el documento ("
                    + templateName + ")");
        }
        byte[] documento;
        boolean pdf = IOutputDocument.FORMAT_PDF.equals(format);
        try (FileInputStream fis = new FileInputStream(templatePath);
                XWPFDocument doc = new XWPFDocument(OPCPackage.open(fis))) {
            WordTemplateMerge.merge(doc, data);
            if (pdf) {
                //La plantilla mergeada se convierte a PDF en memoria.
                documento = WordToPdfConverter.convert(doc);
            } else {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                doc.write(buffer);
                documento = buffer.toByteArray();
            }
        }
        String name = Fn.nvl(fileName, templateName);
        if (pdf) {
            name = name.replaceAll("\\.docx$", "");
            if (!name.endsWith(".pdf")) {
                name += ".pdf";
            }
        }
        return new OutputDocument(name, documento,
                pdf ? IOutputDocument.FORMAT_PDF : IOutputDocument.FORMAT_DOCX);
    }

    /**
     * Busca la plantilla en cada ruta del file system (subcarpeta
     * {@code templates/}) y por último en la carpeta de respaldo.
     *
     * @return path completo de la plantilla, o nulo si no existe en ninguna
     * ruta.
     */
    private String resolveTemplate() {
        for (String url : Fn.nvl(fileSystemPath, "").split(",")) {
            if (url.trim().isEmpty()) {
                continue;
            }
            String candidate = IOUtil.addbs(IOUtil.addbs(url.trim()) + "templates") + templateName;
            if (IOUtil.isFileExist(candidate)) {
                return candidate;
            }
        }
        if (!Strings.isNullorEmpty(fallbackPath)) {
            String candidate = IOUtil.addbs(fallbackPath) + templateName;
            if (IOUtil.isFileExist(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}

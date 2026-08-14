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
package org.javabeanstack.web.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javabeanstack.data.IDataQueryModel;
import org.javabeanstack.outputs.IDocumentSource;
import org.javabeanstack.outputs.IOutputDocument;
import org.javabeanstack.outputs.IPrintableSource;
import org.javabeanstack.outputs.OutputDocument;
import org.javabeanstack.util.Fn;

/**
 * Fuente de documentos del subsistema de salida
 * ({@code org.javabeanstack.outputs}) basada en <b>reportes JasperReports</b>:
 * llena el reporte con datos y parámetros y lo exporta a PDF, DOCX o HTML.
 * Como los reportes también saben imprimirse, implementa además
 * {@link IPrintableSource} (impresión directa en la impresora del servidor).
 *
 * <p>
 * Envuelve a {@link JasperReportUtil}, que aporta la resolución del reporte
 * (rutas del file system → recurso en base de datos → dentro del artefacto
 * desplegado) y la exportación. Uso típico desde un controller:
 * </p>
 *
 * <pre>
 * IDocumentSource source = new JasperReportSource(jasperReportUtil)
 *     .report("rpt_extracto")
 *     .params(jasperParams)
 *     .data(resultList)
 *     .classRef(getClass())
 *     .format(IOutputDocument.FORMAT_PDF);
 * </pre>
 *
 * <p>
 * El formato de planilla NO se genera desde el reporte: los datos tabulares
 * se exportan con {@code ExcelDataSource} (jbs-poi).
 * </p>
 *
 * @author Jorge Enciso
 */
public class JasperReportSource implements IDocumentSource, IPrintableSource {

    private final JasperReportUtil jasperReportUtil;
    private String reportName;
    private Map<String, Object> params = new HashMap();
    private List<IDataQueryModel> data;
    private Class classRef;
    private String format = IOutputDocument.FORMAT_PDF;
    private String fileName;

    /**
     * Crea la fuente sobre un {@link JasperReportUtil} ya configurado (con su
     * {@code fileSystemPath} y su {@code appResource} asignados por el
     * llamador, igual que para {@code showReport}).
     *
     * @param jasperReportUtil utilitario de reportes configurado.
     */
    public JasperReportSource(JasperReportUtil jasperReportUtil) {
        this.jasperReportUtil = jasperReportUtil;
    }

    /**
     * Asigna el nombre del reporte a ejecutar (con o sin la extensión
     * {@code .jasper}).
     *
     * @param reportName nombre del reporte.
     * @return esta instancia, para encadenar.
     */
    public JasperReportSource report(String reportName) {
        this.reportName = reportName;
        return this;
    }

    /**
     * Asigna los parámetros del reporte (cabecera, rótulos, logo, filtros).
     *
     * @param params mapa nombre de parámetro → valor.
     * @return esta instancia, para encadenar.
     */
    public JasperReportSource params(Map<String, Object> params) {
        this.params = params == null ? new HashMap() : params;
        return this;
    }

    /**
     * Asigna las filas de datos del reporte.
     *
     * @param data filas a volcar.
     * @return esta instancia, para encadenar.
     */
    public JasperReportSource data(List<IDataQueryModel> data) {
        this.data = data;
        return this;
    }

    /**
     * Asigna la clase de referencia para buscar el reporte dentro del
     * artefacto desplegado (último recurso de la resolución).
     *
     * @param classRef clase de referencia.
     * @return esta instancia, para encadenar.
     */
    public JasperReportSource classRef(Class classRef) {
        this.classRef = classRef;
        return this;
    }

    /**
     * Asigna el formato de salida; por omisión
     * {@link IOutputDocument#FORMAT_PDF}. Acepta también
     * {@link IOutputDocument#FORMAT_DOCX} y {@link IOutputDocument#FORMAT_HTML}.
     *
     * @param format formato de salida.
     * @return esta instancia, para encadenar.
     */
    public JasperReportSource format(String format) {
        this.format = format;
        return this;
    }

    /**
     * Asigna el nombre de archivo del documento; por omisión se arma con el
     * nombre del reporte y la extensión del formato.
     *
     * @param fileName nombre del archivo con su extensión.
     * @return esta instancia, para encadenar.
     */
    public JasperReportSource fileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    /**
     * Llena el reporte y lo exporta al formato configurado, devolviendo el
     * documento en memoria. No toca la respuesta HTTP.
     *
     * @return documento generado.
     * @throws Exception si el reporte no existe o la generación falla.
     */
    @Override
    public IOutputDocument generate() throws Exception {
        byte[] content = jasperReportUtil.getReportAs(reportName, params, data, classRef, format);
        String name = Fn.nvl(fileName, "");
        if (name.isEmpty()) {
            name = Fn.nvl(reportName, "reporte").replaceAll("\\.jasper$", "") + "." + format;
        }
        return new OutputDocument(name, content, format);
    }

    /**
     * Imprime el reporte directo en la impresora por omisión del servidor,
     * sin producir ningún documento (ver {@link IPrintableSource}).
     *
     * @throws Exception si el reporte no existe o la impresión falla.
     */
    @Override
    public void printDirect() throws Exception {
        jasperReportUtil.printDirect(reportName, params, data, classRef);
    }
}

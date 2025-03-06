/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2018 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.context.FacesContext;
import javax.naming.NamingException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapArrayDataSource;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import net.sf.jasperreports.export.SimpleHtmlReportConfiguration;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsReportConfiguration;
import org.apache.log4j.Logger;

import org.javabeanstack.data.IDataQueryModel;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.io.IOUtil;
import org.javabeanstack.resources.IAppResource;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.util.Fn;
import org.primefaces.model.DefaultStreamedContent;

/**
 * Gestiona las tareas para mostrar un reporte ya sea en formato pdf, excel,
 * word, html u otros.
 *
 * @author Jorge Enciso
 */
public class JasperReportUtil {

    private static final Logger LOGGER = Logger.getLogger(JasperReportUtil.class);

    private IAppResource appResource;
    private IUserSession userSession;
    private final String urlInDatabase = "file:///reports/";
    private String fileSystemPath = null;

    private final FacesContextUtil facesCtx = new FacesContextUtil();

    public JasperReportUtil(IUserSession userSession) {
        this.userSession = userSession;
    }

    public IAppResource getAppResource() {
        return appResource;
    }

    public void setAppResource(IAppResource appResource) {
        this.appResource = appResource;
    }

    public String getFileSystemPath() {
        return fileSystemPath;
    }

    public void setFileSystemPath(String fileSystemPath) {
        this.fileSystemPath = fileSystemPath;
    }

    /**
     * Muestra el informe según parametros recibidos.
     *
     * @param report reporte en formato InputStream.
     * @param parameters parametros del reporte.
     * @param data datos a mostrar.
     * @throws JRException
     * @throws IOException
     * @throws NamingException
     */
    public void showReport(InputStream report, Map<String, Object> parameters,
            List<IDataQueryModel> data) throws JRException, IOException, NamingException, Exception {

        Map[] dataRows = convertTo(data);
        String reporte = parameters.get("reportname").toString();

        JasperPrint jasperPrint = JasperFillManager.fillReport(report,
                parameters,
                new JRMapArrayDataSource(dataRows));
        print(reporte, parameters, jasperPrint);
    }

    /**
     *
     * @param reportName nombre del reporte.
     * @param parameters parametros del reporte.
     * @param data datos a mostrar.
     * @param classRef
     * @throws JRException
     * @throws IOException
     * @throws NamingException
     */
    public void showReport(String reportName, Map<String, Object> parameters,
            List<IDataQueryModel> data,
            Class classRef) throws JRException, IOException, NamingException, Exception {

        Map[] dataRows = convertTo(data);
        if (reportName == null){
            throw new Exception("El nombre del reporte es nulo o no existe");
        }
        if (!reportName.endsWith(".jasper")) {
            reportName += ".jasper";
        }

        JasperReport jasper = getJasperReportFrom(reportName, classRef);

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasper,
                parameters, new JRMapArrayDataSource(dataRows));
        print(reportName, parameters, jasperPrint);
    }

    /**
     * Muestra un informe según parametros recibidos.
     *
     * @param reportName nombre del reporte.
     * @param parameters parametros del reporte.
     * @param jasperPrint gestor de impresión.
     * @throws JRException
     * @throws IOException
     * @throws NamingException
     */
    public void print(String reportName, Map<String, Object> parameters,
            JasperPrint jasperPrint) throws JRException, IOException, NamingException, Exception {
        String reporte;
        if (parameters.containsKey("reportname")) {
            reporte = parameters.get("reportname").toString();
        } else {
            reporte = reportName;
        }
        if (parameters.containsKey("device")) {
            if (parameters.get("device") == "printer") {
                JasperPrintManager.printReport(jasperPrint, false);
            }
            if ("html".equals(parameters.get("device"))) {
                HtmlExporter exporterHTML = new HtmlExporter();
                SimpleExporterInput exporterInput = new SimpleExporterInput(jasperPrint);
                exporterHTML.setExporterInput(exporterInput);
                SimpleHtmlReportConfiguration reportExportConfiguration = new SimpleHtmlReportConfiguration();
                exporterHTML.setConfiguration(reportExportConfiguration);
                HttpServletResponse httpServletResponse = (HttpServletResponse) facesCtx.getExternalContext().getResponse();
                httpServletResponse.setContentType("text/html;charset=UTF-8");
                httpServletResponse.addHeader("Content-disposition", "attachment; filename=" + reporte.replaceAll(".jasper", "") + ".html");
                ServletOutputStream servletOutputStream = httpServletResponse.getOutputStream();
                SimpleHtmlExporterOutput simpleHtmlExporterOutput = new SimpleHtmlExporterOutput(servletOutputStream);
                exporterHTML.setExporterOutput(simpleHtmlExporterOutput);
                exporterHTML.exportReport();
                servletOutputStream.flush();
                servletOutputStream.close();
                FacesContext.getCurrentInstance().responseComplete();
            }
            if ("doc".equals(parameters.get("device"))) {
                JRDocxExporter exporter = new JRDocxExporter();
                exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                HttpServletResponse httpServletResponse = (HttpServletResponse) facesCtx.getExternalContext().getResponse();
                httpServletResponse.setContentType("application/nd.openxmlformats-officedocument.wordprocessingml.document");
                httpServletResponse.setHeader("Content-Disposition", "attachment;filename= " + reporte.replaceAll(".jasper", "") + ".docx");
                OutputStream outputStream = httpServletResponse.getOutputStream();
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
                exporter.exportReport();
                outputStream.flush();
                outputStream.close();
                FacesContext.getCurrentInstance().responseComplete();
            }
            if ("pdf".equals(parameters.get("device"))) {
                String target;
                if (parameters.containsKey("target")) {
                    target = parameters.get("target").toString();
                } else {
                    target = "inline";
                }
                HttpServletResponse httpServletResponse = (HttpServletResponse) facesCtx.getExternalContext().getResponse();
                httpServletResponse.addHeader("Content-disposition", target + "; filename=" + reporte.replaceAll(".jasper", "") + ".pdf");
                ServletOutputStream servletOutputStream = httpServletResponse.getOutputStream();
                JasperExportManager.exportReportToPdfStream(jasperPrint, servletOutputStream);
                FacesContext.getCurrentInstance().responseComplete();
            }
            if ("xls".equals(parameters.get("device"))) {
                JRXlsExporter exporter = new JRXlsExporter();
                exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                HttpServletResponse httpServletResponse = (HttpServletResponse) facesCtx.getExternalContext().getResponse();
                httpServletResponse.setContentType("application/nd.openxmlformats-officedocument.spreadsheetml.sheet");
                httpServletResponse.setHeader("Content-Disposition", "attachment;filename= " + reporte.replaceAll(".jasper", "") + ".xls");
                OutputStream outputStream = httpServletResponse.getOutputStream();
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
                SimpleXlsReportConfiguration configuration = new SimpleXlsReportConfiguration();
                configuration.setOnePagePerSheet(false);
                configuration.setDetectCellType(true);
                configuration.setCollapseRowSpan(false);
                exporter.setConfiguration(configuration);
                exporter.exportReport();
                outputStream.flush();
                outputStream.close();
                FacesContext.getCurrentInstance().responseComplete();
            }
        } else {
            JasperPrintManager.printReport(jasperPrint, false);
        }
    }

    /**
     * Convierte los datos a mostrar en un map que interprete el jasper.
     *
     * @param data datos a convertir.
     * @return Datos convertidos en un map.
     */
    protected Map[] convertTo(List<IDataQueryModel> data) {
        Map[] dataRows = new HashMap[data.size()];
        int indice = 0;
        for (IDataQueryModel element : data) {
            Map rowMap = new HashMap();
            for (int j = 0; j < element.getColumnList().length; j++) {
                rowMap.put(element.getColumnName(j), element.getColumn(j));
            }
            dataRows[indice] = rowMap;
            indice++;
        }
        return dataRows;
    }

    /**
     * Devuelve el reporte con el path completo, necesario para generar el
     * informe. Para ello utiliza una variable fileSystemPath, donde se
     * encuentra los caminos a buscar el reporte y el orden de las carpetas en
     * la cual hay una precedencia, luego de no encontrar el reporte busca en la
     * base de datos y por ultimo en el .war.
     *
     * @param reportNameJasper nombre del reporte
     * @return el reporte con el path completo
     */
    public String getFullPathReport(String reportNameJasper) {
        reportNameJasper = reportNameJasper.toLowerCase();
        if (reportNameJasper.endsWith(".jrxml")) {
            reportNameJasper = reportNameJasper.replaceAll(".jrxml", ".jasper");
        }
        if (!reportNameJasper.endsWith(".jasper")) {
            reportNameJasper = reportNameJasper.trim() + ".jasper";
        }
        //Buscar en el file system
        String[] path = Fn.nvl(fileSystemPath, "").split(",");
        for (String url : path) {
            try {
                if (url.trim().isEmpty()) {
                    continue;
                }
                url = IOUtil.addbs(url.trim()) + "reports";
                url = IOUtil.addbs(url);
                if (IOUtil.isFileExist(url + reportNameJasper)) {
                    return url + reportNameJasper;
                }
            } catch (Exception ex) {
                //Nada
            }
        }
        // Buscar el reporte en la base de datos        
        if (getAppResource() != null) {
            String reportNameJrxml = reportNameJasper.replaceAll(".jasper", ".jrxml");
            byte[] report;
            if (reportNameJrxml.endsWith(".jrxml")) {
                // Buscar como  .jrxml en la base y en el directorio
                report = getAppResource().getResourceAsBytes(userSession, urlInDatabase + reportNameJrxml);
                if (report != null && report.length > 0) {
                    LOGGER.info(urlInDatabase + reportNameJrxml);
                    return urlInDatabase + reportNameJrxml;
                }
            }
        }
        return IOUtil.getFileName(reportNameJasper);
    }

    /**
     * Devuelve el reporte en formato JasperReport, busca primero en el
     * fileSystemPath y si no encuentra, en la base de datos y por ultimo en el
     * .war.
     *
     * @param reportNameJasper nombre del reporte
     * @param classRef clase de referencia para busqueda del reporte en el .war.
     * @return el reporte en formato JasperReport
     * @throws JRException
     */
    public JasperReport getJasperReportFrom(String reportNameJasper, Class classRef) throws JRException {
        JasperReport jasperReport = null;
        reportNameJasper = reportNameJasper.toLowerCase();
        if (reportNameJasper.endsWith(".jrxml")) {
            reportNameJasper = reportNameJasper.replaceAll(".jrxml", ".jasper");
        }
        if (!reportNameJasper.endsWith(".jasper")) {
            reportNameJasper = reportNameJasper.trim() + ".jasper";
        }
        //Buscar en el file system
        String[] path = Fn.nvl(fileSystemPath, "").split(",");
        for (String url : path) {
            try {
                if (url.trim().isEmpty()) {
                    continue;
                }
                url = IOUtil.addbs(url.trim()) + "reports";
                url = IOUtil.addbs(url);
                if (IOUtil.isFileExist(url + reportNameJasper)) {
                    jasperReport = (JasperReport) JRLoader.loadObjectFromFile(url + reportNameJasper);
                    return jasperReport;
                }
            } catch (Exception ex) {
                //Nada
            }
        }
        // Buscar el reporte en la base de datos        
        if (getAppResource() != null) {
            String reportNameJrxml = reportNameJasper.replaceAll(".jasper", ".jrxml");
            byte[] report;
            if (reportNameJrxml.endsWith(".jrxml")) {
                // Buscar como  .jrxml en la base y en el directorio
                report = getAppResource().getResourceAsBytes(userSession, urlInDatabase + reportNameJrxml);
                if (report != null && report.length > 0) {
                    //Compilar
                    jasperReport = JasperCompileManager.compileReport(new ByteArrayInputStream(report));
                    LOGGER.info(urlInDatabase + reportNameJrxml);
                    return jasperReport;
                }
            }
        }
        //Buscar dentro del .war
        if (jasperReport == null && classRef != null) {
            // Buscar en la carpeta resource en el proyecto donde se encuentra classRef
            LOGGER.info(reportNameJasper);
            try {
                jasperReport = (JasperReport) JRLoader.loadObject(IOUtil.getResourceAsStream(classRef, "/reports/" + reportNameJasper));                
            } catch (NullPointerException e) {
                ErrorManager.showError(e, LOGGER);
                throw new JRException("Es posible que no exista el reporte " + Fn.nvl(reportNameJasper,"").toUpperCase());
            }
        }
        return jasperReport;
    }

    public DefaultStreamedContent getReportStreamedContent(String reportName, Map<String, Object> parameters,
            List<IDataQueryModel> data,
            Class classRef) throws JRException, IOException, NamingException {
        byte[] docPdf;
        Map[] dataRows = convertTo(data);
        if (!reportName.endsWith(".jasper")) {
            reportName += ".jasper";
        }
        JasperReport jasper = getJasperReportFrom(reportName, classRef);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasper, parameters, new JRMapArrayDataSource(dataRows));
        docPdf = JasperExportManager.exportReportToPdf(jasperPrint);
        return new DefaultStreamedContent(new ByteArrayInputStream(docPdf), "application/pdf", reportName.replaceAll(".jasper", "") + ".pdf");
    }
}

/*
switch (docType) {
        case PDF:
            JasperExportManager.exportReportToPdfStream(jasperPrint, os);
            break;
        case RTF:
            JRRtfExporter rtfExporter = new JRRtfExporter();
            rtfExporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
            rtfExporter.setParameter(JRExporterParameter.OUTPUT_STREAM, os);
            rtfExporter.setParameter(JRExporterParameter.CHARACTER_ENCODING, "UTF-8");
            rtfExporter.exportReport();
            break;
        case XLS:
            JRXlsExporter xlsExporter = new JRXlsExporter();
            xlsExporter.setParameter(JRXlsExporterParameter.JASPER_PRINT, jasperPrint);
            xlsExporter.setParameter(JRXlsExporterParameter.OUTPUT_STREAM, os);
            xlsExporter.setParameter(JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.TRUE);
            xlsExporter.setParameter(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
            xlsExporter.setParameter(JRXlsExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, Boolean.TRUE);
            xlsExporter.exportReport();
            break;
        case XLSX:
            JRXlsxExporter xlsxExporter = new JRXlsxExporter();
            xlsxExporter.setParameter(JRXlsExporterParameter.JASPER_PRINT, jasperPrint);
            xlsxExporter.setParameter(JRXlsExporterParameter.OUTPUT_STREAM, os);
            xlsxExporter.exportReport();
            break;
        case ODT:
            JROdtExporter odtExporter = new JROdtExporter();
            odtExporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
            odtExporter.setParameter(JRExporterParameter.OUTPUT_STREAM, os);
            odtExporter.exportReport();
            break;
        case PNG:
            BufferedImage pageImage = new BufferedImage((int) (jasperPrint.getPageWidth() * ZOOM_2X + 1),
                (int) (jasperPrint.getPageHeight() * ZOOM_2X + 1), BufferedImage.TYPE_INT_RGB);
            JRGraphics2DExporter exporter = new JRGraphics2DExporter();
            exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
            exporter.setParameter(JRGraphics2DExporterParameter.GRAPHICS_2D, pageImage.getGraphics());
            exporter.setParameter(JRGraphics2DExporterParameter.ZOOM_RATIO, ZOOM_2X);
            exporter.setParameter(JRExporterParameter.PAGE_INDEX, Integer.valueOf(0));
            exporter.exportReport();
            ImageIO.write(pageImage, "png", os);
            break;
        case HTML:
            JRHtmlExporter htmlExporter = new JRHtmlExporter();
            htmlExporter.setParameter(JRHtmlExporterParameter.JASPER_PRINT, jasperPrint);
            htmlExporter.setParameter(JRHtmlExporterParameter.OUTPUT_STREAM, os);
            htmlExporter.setParameter(JRHtmlExporterParameter.IMAGES_URI, "img/");
            htmlExporter.setParameter(JRHtmlExporterParameter.IMAGES_DIR, new java.io.File("img"));
            htmlExporter.setParameter(JRHtmlExporterParameter.IS_OUTPUT_IMAGES_TO_DIR, Boolean.TRUE);
            htmlExporter.setParameter(JRHtmlExporterParameter.ZOOM_RATIO, ZOOM_2X);
            htmlExporter.exportReport();
            break;
        case DOCX:
            JRDocxExporter docxExporter = new JRDocxExporter();
            docxExporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
            docxExporter.setParameter(JRExporterParameter.OUTPUT_STREAM, os);
            docxExporter.exportReport();
            break;
        case PPTX:
            JRPptxExporter pptxExporter = new JRPptxExporter();
            pptxExporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
            pptxExporter.setParameter(JRExporterParameter.OUTPUT_STREAM, os);
            pptxExporter.exportReport();
            break;
        default:
            break;
    }
 */

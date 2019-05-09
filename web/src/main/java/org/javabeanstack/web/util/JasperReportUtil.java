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

import org.javabeanstack.data.IDataQueryModel;
import org.javabeanstack.io.IOUtil;
import org.javabeanstack.resources.IAppResource;
import org.javabeanstack.security.model.IUserSession;


/**
 *
 * @author Jorge Enciso
 */
public class JasperReportUtil {
    private IAppResource appResource;
    private IUserSession userSession;
    
    private final FacesContextUtil facesCtx = new FacesContextUtil();
    
    public JasperReportUtil(IUserSession userSession){
        this.userSession = userSession;
    }
    
    public IAppResource getAppResource() {
        return appResource;
    }

    public void setAppResource(IAppResource appResource) {
        this.appResource = appResource;
    }

    
    public void showReport(InputStream report, Map<String, Object> parameters,
            List<IDataQueryModel> data) throws JRException, IOException, NamingException {

        Map[] dataRows = convertTo(data);
        String reporte = parameters.get("reportname").toString();

        JasperPrint jasperPrint = JasperFillManager.fillReport(report,
                parameters,
                new JRMapArrayDataSource(dataRows));
        print(reporte, parameters, jasperPrint);
    }

    public void showReport(String reportName, Map<String, Object> parameters,
            List<IDataQueryModel> data,
            Class classRef) throws JRException, IOException, NamingException {

        Map[] dataRows = convertTo(data);
        if (!reportName.endsWith(".jasper")) {
            reportName += ".jasper";
        }
        
        JasperReport jasper = getJasperReportFrom(reportName, classRef);

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasper,
                parameters, new JRMapArrayDataSource(dataRows));
        print(reportName, parameters, jasperPrint);
    }

    public void print(String reportName, Map<String, Object> parameters,
            JasperPrint jasperPrint) throws JRException, IOException, NamingException {
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
                String target = "";
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

    public JasperReport getJasperReportFrom(String resourceURL, Class classRef) throws JRException {
        JasperReport jasperReport; 
        resourceURL = resourceURL.toLowerCase();
        if (getAppResource() != null){
            // Buscar el reporte con el nombre original en la base de datos
            byte[] report = getAppResource().getResourceAsBytes(userSession, "file:///reports/"+resourceURL);        
            if (report == null && (resourceURL.endsWith(".jasper"))){
                // Si no encuentra buscar como .jrxml
                String resourceURL2 = resourceURL;
                resourceURL2 = resourceURL2.replaceAll(".jasper", ".jrxml");
                report = getAppResource().getResourceAsBytes(userSession, "file:///reports/"+resourceURL2);
                if (report != null && report.length > 0){
                    //Compilar
                    jasperReport = JasperCompileManager.compileReport(new ByteArrayInputStream(report));
                    return jasperReport;
                }
            }
            if (report != null  && report.length > 0){
                InputStream result = new ByteArrayInputStream(report);
                jasperReport = (JasperReport)JRLoader.loadObject(result);
                return jasperReport;
            }
        }
        if (classRef != null){
            // Buscar en la carpeta resource en el proyecto donde se encuentra classRef
            jasperReport = (JasperReport)JRLoader.loadObject(IOUtil.getResourceAsStream(classRef, "/reports/" + resourceURL));            
        }
        else {
            //Buscar en el file system
            jasperReport = (JasperReport)JRLoader.loadObjectFromFile(resourceURL);
        }
        return jasperReport;
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
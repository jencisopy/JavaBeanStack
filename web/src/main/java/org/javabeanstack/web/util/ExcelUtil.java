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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.OLE2NotOfficeXmlFileException;
import org.apache.poi.poifs.filesystem.NotOLE2FileException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import static org.apache.poi.ss.usermodel.CellType.BOOLEAN;
import static org.apache.poi.ss.usermodel.CellType.FORMULA;
import static org.apache.poi.ss.usermodel.CellType.NUMERIC;
import static org.apache.poi.ss.usermodel.CellType.STRING;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.javabeanstack.data.IDataQueryModel;
import org.javabeanstack.data.model.DataQueryModel;
import org.javabeanstack.util.Fn;

/**
 *
 * @author Jorge Enciso
 */
public class ExcelUtil {

    public static void downLoadFile(Workbook workBook, String fileName) throws Exception {
        if (workBook == null) {
            return;
        }

        HttpServletResponse httpServletResponse = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        httpServletResponse.setContentType("application/vnd.ms-excel");
        httpServletResponse.addHeader("Content-disposition", "  attachment; filename=" + fileName + ".xls");
        ServletOutputStream servletOutputStream = httpServletResponse.getOutputStream();

        workBook.write(servletOutputStream);
        servletOutputStream.flush();
        servletOutputStream.close();
        FacesContext.getCurrentInstance().responseComplete();
    }

    public static Workbook openWorkbook(String filePath) throws IOException {
        try (InputStream fileInputStream = new FileInputStream(filePath)) {
            if (filePath.toLowerCase()
                    .endsWith("xlsx")) {
                return new XSSFWorkbook(fileInputStream);
            } else if (filePath.toLowerCase()
                    .endsWith("xls")) {
                return new HSSFWorkbook(fileInputStream);
            } else {
                throw new IllegalArgumentException("The specified file is not an Excel file");
            }
        } catch (OLE2NotOfficeXmlFileException | NotOLE2FileException e) {
            throw new IllegalArgumentException(
                    "The file format is not supported. Ensure the file is a valid Excel file.", e);
        }
    }

    public static Workbook openWorkbook(File file) throws IOException {
        Workbook wb = WorkbookFactory.create(file);
        return wb;
    }

    public static Workbook toExcel(List<IDataQueryModel> toExport) throws Exception {
        if (toExport == null || toExport.isEmpty()) {
            return null;
        }
        Workbook workBook = new HSSFWorkbook();
        Sheet sheet = workBook.createSheet();
        Row row;
        Cell cell;

        CellStyle defaultCellStyle = workBook.createCellStyle();
        CellStyle numberCellStyle = workBook.createCellStyle();
        CellStyle textBoldCellStyle = workBook.createCellStyle();
        CellStyle dateCellStyle = workBook.createCellStyle();

        Font font8points = workBook.createFont();
        Font fontBold = workBook.createFont();

        font8points.setFontHeightInPoints((short) 8);
        fontBold.setFontHeightInPoints((short) 8);
        fontBold.setBold(true);
        defaultCellStyle.setFont(font8points);
        textBoldCellStyle.setDataFormat(HSSFDataFormat.getBuiltinFormat("text"));
        numberCellStyle.setDataFormat(HSSFDataFormat.getBuiltinFormat("#,##0.00"));
        numberCellStyle.setFont(font8points);
        dateCellStyle.setDataFormat(HSSFDataFormat.getBuiltinFormat("dd/mm/aaaa h:mm:ss"));
        dateCellStyle.setFont(font8points);
        SimpleDateFormat formatterDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        textBoldCellStyle.setFont(fontBold);
        workBook.setSheetName(0, "DATOS");

        int rownum = 0;
        for (int i = 0; i < toExport.size(); i++) {
            if (i == 0) {
                row = sheet.createRow(rownum++);
                for (int j = 0; j < toExport.get(0).getColumnList().length; j++) {
                    cell = row.createCell(j);
                    cell.setCellStyle(textBoldCellStyle);
                    cell.setCellValue(toExport.get(i).getColumnName(j));
                }
            }
            row = sheet.createRow(rownum++);
            for (int j = 0; j < toExport.get(0).getColumnList().length; j++) {
                //System.out.println(j);
                cell = row.createCell(j);
                Object[] fila = (Object[]) toExport.get(i).getRow();
                if (fila[j] instanceof BigDecimal) {
                    cell.setCellStyle(numberCellStyle);
                    cell.setCellValue(((BigDecimal) fila[j]).doubleValue());
                } else if (fila[j] instanceof Timestamp) {
                    cell.setCellStyle(dateCellStyle);
                    cell.setCellValue(formatterDate.format((Date) fila[j]));
                } else {
                    cell.setCellStyle(defaultCellStyle);
                    cell.setCellValue((String.valueOf(fila[j])));
                }
            }
        }
        for (int j = 0; j < toExport.get(0).getColumnList().length; j++) {
            sheet.autoSizeColumn(j);
        }
        return workBook;
    }

    public static List<IDataQueryModel> fromExcelToDataQueryModel(Sheet sheet) throws Exception {
        if (sheet == null) {
            return null;
        }
        List<String> headerNames = getHeaderNames(sheet);
        if (headerNames.isEmpty()){
            throw new Exception("No esta  especificado los nombres de las columnas en la primera fila");
        }
        List<IDataQueryModel> retornar = new ArrayList();        
        String[] columnNames = headerNames.toArray(new String[0]);
        //Recorrer las filas de la hoja
        for (Row row : sheet) {
            Cell cell = null;            
            try {
                if (row.getRowNum() == 0) {
                    continue;
                }
                IDataQueryModel data = new DataQueryModel();
                Object[] dataRow = new Object[columnNames.length];
                data.setRow(dataRow);
                data.setColumnList(columnNames);
                //Recorrer las celdas
                for (int i = 0; i < row.getLastCellNum(); i++) {
                    String columnName = sheet.getRow(0).getCell(i).getStringCellValue();
                    if (Fn.nvl(columnName, "").isEmpty()) {
                        continue;
                    }
                    cell = row.getCell(i);
                    switch (cell.getCellType()) {
                        case NUMERIC:
                            if (DateUtil.isCellDateFormatted(cell)) {
                                data.setColumn(columnName, cell.getLocalDateTimeCellValue());
                            } else {
                                data.setColumn(columnName, cell.getNumericCellValue());
                            }
                            break;
                        case BOOLEAN:
                            data.setColumn(columnName, cell.getBooleanCellValue());
                            break;
                        case STRING:
                            data.setColumn(columnName, cell.getStringCellValue());
                            break;
                        case FORMULA:
                            data.setColumn(columnName, cell.getNumericCellValue());
                            break;
                    }
                }
                retornar.add(data);
            } catch (Exception e) {
                String errorMsg = "ERROR EN LA FILA " + row.getRowNum();
                if (cell != null){
                    errorMsg += ", CELDA "+cell.getAddress().formatAsString();
                }
                errorMsg += ", " + e.getMessage();
                throw new Exception(errorMsg);
            }
        }
        return retornar;
    }

    public static BigDecimal getBigDecimal(Cell cell) throws Exception {
        BigDecimal retornar = null;
        switch (cell.getCellType()) {
            case NUMERIC:
                retornar = BigDecimal.valueOf(cell.getNumericCellValue());
                break;
            case BOOLEAN:
                throw new Exception("Imposible convertir de tipo boolean a bigdecimal");
            case STRING:
                retornar = new BigDecimal(cell.getStringCellValue());
                break;
            case FORMULA:
                retornar = BigDecimal.valueOf(cell.getNumericCellValue());
                break;
        }
        return retornar;
    }

    public static List<String> getHeaderNames(Sheet sheet) {
        if (sheet == null){
            return new ArrayList();
        }
        List<String> retornar = new ArrayList();
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            return Collections.EMPTY_LIST;
        }
        //Todas las celdas debe ser string
        boolean error = false;
        for (Cell cell:headerRow){
            if (!cell.getCellType().equals(STRING)){
                error = true;
                break;
            }
            retornar.add(cell.getStringCellValue());
        }
        if (error){
            return new ArrayList();
        }
        return retornar;
    }
}

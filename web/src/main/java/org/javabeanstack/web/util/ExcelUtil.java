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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.faces.context.FacesContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.javabeanstack.data.IDataQueryModel;

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
                System.out.println(j);
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
}

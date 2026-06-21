/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 Jorge Enciso
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
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.javabeanstack.data.IDataQueryModel;
import org.javabeanstack.data.model.DataQueryModel;
import static org.javabeanstack.web.util.ExcelUtil.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pruebas unitarias de {@link ExcelUtil}.
 * <p>
 * Las planillas se construyen en memoria con POI (y se escriben a archivos
 * temporales solo donde se necesita un {@link File}), evitando depender de
 * planillas {@code prueba*.xlsx} externas.
 *
 * @author Jorge Enciso
 */
public class ExcelUtilTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2025, 6, 20, 10, 15, 30);

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    /**
     * Crea una planilla con una fila de encabezados y filas de datos con tipos
     * variados: codigo (texto), monto (numérico), activo (booleano) y fecha.
     */
    private Workbook buildSampleWorkbook(int dataRows) {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("DATOS");
        CreationHelper helper = wb.getCreationHelper();
        CellStyle dateStyle = wb.createCellStyle();
        dateStyle.setDataFormat(helper.createDataFormat().getFormat("dd/mm/yyyy hh:mm:ss"));

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("codigo");
        header.createCell(1).setCellValue("monto");
        header.createCell(2).setCellValue("activo");
        header.createCell(3).setCellValue("fecha");

        for (int i = 1; i <= dataRows; i++) {
            Row row = sheet.createRow(i);
            row.createCell(0).setCellValue("COD" + i);
            row.createCell(1).setCellValue(100d * i);
            row.createCell(2).setCellValue(i % 2 == 0);
            Cell fecha = row.createCell(3);
            fecha.setCellValue(Timestamp.valueOf(FECHA));
            fecha.setCellStyle(dateStyle);
        }
        return wb;
    }

    private File writeToTempFile(Workbook wb, Path dir, String name) throws Exception {
        File file = dir.resolve(name).toFile();
        try (FileOutputStream out = new FileOutputStream(file)) {
            wb.write(out);
        }
        return file;
    }

    private Cell singleCell(Workbook wb) {
        return wb.createSheet().createRow(0).createCell(0);
    }

    // ----------------------------------------------------------------------
    // openWorkbook
    // ----------------------------------------------------------------------

    @Test
    public void testOpenWorkbookFile(@TempDir Path dir) throws Exception {
        try (Workbook wb = buildSampleWorkbook(2)) {
            File file = writeToTempFile(wb, dir, "datos.xlsx");
            try (Workbook opened = openWorkbook(file)) {
                assertNotNull(opened);
                assertEquals("DATOS", opened.getSheetAt(0).getSheetName());
            }
        }
    }

    @Test
    public void testOpenWorkbookString(@TempDir Path dir) throws Exception {
        try (Workbook wb = buildSampleWorkbook(1)) {
            File file = writeToTempFile(wb, dir, "datos.xlsx");
            try (Workbook opened = openWorkbook(file.getAbsolutePath())) {
                assertNotNull(opened);
            }
        }
    }

    @Test
    public void testOpenWorkbookInvalidExtension(@TempDir Path dir) {
        // La extensión se valida antes de abrir el archivo (no requiere que exista).
        String path = dir.resolve("archivo.txt").toString();
        assertThrows(IllegalArgumentException.class, () -> openWorkbook(path));
    }

    // ----------------------------------------------------------------------
    // getHeaderNames
    // ----------------------------------------------------------------------

    @Test
    public void testGetHeaderNamesValid() throws Exception {
        try (Workbook wb = buildSampleWorkbook(0)) {
            List<String> names = getHeaderNames(wb.getSheetAt(0));
            assertEquals(List.of("codigo", "monto", "activo", "fecha"), names);
        }
    }

    @Test
    public void testGetHeaderNamesStopsAtBlank() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("a");
            header.createCell(1).setCellValue("b");
            header.createCell(2).setCellValue("   "); // en blanco -> corta acá
            header.createCell(3).setCellValue("c");
            assertEquals(List.of("a", "b"), getHeaderNames(sheet));
        }
    }

    @Test
    public void testGetHeaderNamesNonTextReturnsEmpty() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("a");
            header.createCell(1).setCellValue(123d); // no es texto -> inválido
            assertTrue(getHeaderNames(sheet).isEmpty());
        }
    }

    @Test
    public void testGetHeaderNamesNullSheet() {
        assertTrue(getHeaderNames(null).isEmpty());
    }

    // ----------------------------------------------------------------------
    // fromExcelToDataQueryModel
    // ----------------------------------------------------------------------

    @Test
    public void testFromExcelToDataQueryModel() throws Exception {
        try (Workbook wb = buildSampleWorkbook(3)) {
            List<IDataQueryModel> result = fromExcelToDataQueryModel(wb.getSheetAt(0));
            assertEquals(3, result.size());
            assertEquals("COD1", result.get(0).getColumn("codigo"));
            assertEquals(100d, (Double) result.get(0).getColumn("monto"), 0.0001);
            assertEquals(Boolean.FALSE, result.get(0).getColumn("activo"));
            assertEquals(FECHA, result.get(0).getColumn("fecha"));
        }
    }

    @Test
    public void testFromExcelToDataQueryModelNoHeaderThrows() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            // primera fila numérica -> sin cabeceras válidas
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue(1d);
            row.createCell(1).setCellValue(2d);
            Exception ex = assertThrows(Exception.class,
                    () -> fromExcelToDataQueryModel(sheet));
            assertTrue(ex.getMessage().contains("nombres de columnas válidas"), ex.getMessage());
        }
    }

    // ----------------------------------------------------------------------
    // toExcel
    // ----------------------------------------------------------------------

    @Test
    public void testToExcelRoundTrip() throws Exception {
        DataQueryModel model = new DataQueryModel();
        model.setColumnList(new String[]{"codigo", "monto", "fecha"});
        model.setRow(new Object[]{"COD1", new BigDecimal("123.45"), Timestamp.valueOf(FECHA)});

        try (Workbook wb = toExcel(List.of(model))) {
            assertNotNull(wb);
            Sheet sheet = wb.getSheetAt(0);
            assertEquals("DATOS", sheet.getSheetName());
            // fila 0 = cabecera
            assertEquals("codigo", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("fecha", sheet.getRow(0).getCell(2).getStringCellValue());
            // fila 1 = datos
            assertEquals("COD1", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals(123.45d, sheet.getRow(1).getCell(1).getNumericCellValue(), 0.0001);
        }
    }

    @Test
    public void testToExcelNullOrEmptyReturnsNull() throws Exception {
        assertNull(toExcel(null));
        assertNull(toExcel(List.of()));
    }

    // ----------------------------------------------------------------------
    // convertValue
    // ----------------------------------------------------------------------

    @Test
    public void testConvertValue() {
        assertNull(convertValue(null, String.class));
        // mismo tipo
        assertEquals("texto", convertValue("texto", String.class));
        // numéricos desde Double
        assertEquals(new BigDecimal("100.5"), convertValue(100.5d, BigDecimal.class));
        assertEquals(7L, convertValue(7d, Long.class));
        assertEquals(7, convertValue(7d, Integer.class));
        assertEquals((short) 7, convertValue(7d, Short.class));
        // Character
        assertEquals('A', convertValue("ABC", Character.class));
        // Boolean
        assertEquals(true, convertValue("1", Boolean.class));
        assertEquals(true, convertValue("true", Boolean.class));
        assertEquals(false, convertValue("0", Boolean.class));
        // LocalDateTime desde Timestamp y Date
        assertEquals(FECHA, convertValue(Timestamp.valueOf(FECHA), LocalDateTime.class));
        assertTrue(convertValue(new Date(), LocalDateTime.class) instanceof LocalDateTime);
    }

    // ----------------------------------------------------------------------
    // getBigDecimal
    // ----------------------------------------------------------------------

    @Test
    public void testGetBigDecimalNumeric() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Cell cell = singleCell(wb);
            cell.setCellValue(123.45d);
            assertEquals(0, BigDecimal.valueOf(123.45d).compareTo(getBigDecimal(cell)));
        }
    }

    @Test
    public void testGetBigDecimalString() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Cell cell = singleCell(wb);
            cell.setCellValue("678.90");
            assertEquals(0, new BigDecimal("678.90").compareTo(getBigDecimal(cell)));
        }
    }

    @Test
    public void testGetBigDecimalBooleanThrows() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Cell cell = singleCell(wb);
            cell.setCellValue(true);
            assertThrows(Exception.class, () -> getBigDecimal(cell));
        }
    }

    // ----------------------------------------------------------------------
    // getAssignableTypeError
    // ----------------------------------------------------------------------

    @Test
    public void testGetAssignableTypeErrorNullFieldType() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Cell cell = singleCell(wb);
            cell.setCellValue("x");
            assertNotNull(getAssignableTypeError(cell, null, "inexistente"));
        }
    }

    @Test
    public void testGetAssignableTypeErrorNumeric() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Cell cell = singleCell(wb);
            cell.setCellValue(10d);
            // numérico entero a Long -> ok
            assertNull(getAssignableTypeError(cell, Long.class, "monto"));
            // numérico a String -> ok
            assertNull(getAssignableTypeError(cell, String.class, "monto"));
            // numérico a Boolean fuera de {0,1} -> error
            assertNotNull(getAssignableTypeError(cell, Boolean.class, "activo"));
        }
    }

    @Test
    public void testGetAssignableTypeErrorDecimalToInteger() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Cell cell = singleCell(wb);
            cell.setCellValue(10.5d);
            // decimal a entero -> error
            assertNotNull(getAssignableTypeError(cell, Integer.class, "cantidad"));
            // decimal a BigDecimal -> ok
            assertNull(getAssignableTypeError(cell, BigDecimal.class, "monto"));
        }
    }

    @Test
    public void testGetAssignableTypeErrorString() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Cell cell = singleCell(wb);
            cell.setCellValue("no-numero");
            // texto no numérico a Long -> error
            assertNotNull(getAssignableTypeError(cell, Long.class, "monto"));
            // texto a String -> ok
            assertNull(getAssignableTypeError(cell, String.class, "codigo"));
        }
    }
}

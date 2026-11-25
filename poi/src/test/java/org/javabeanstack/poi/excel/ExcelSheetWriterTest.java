/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
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
package org.javabeanstack.poi.excel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de {@link ExcelSheetWriter}: hoja vacía, tipos de celda,
 * ida y vuelta por {@link ExcelRowProcessor}, más filas que la ventana de
 * SXSSF, valores que Excel no puede guardar y las guardas del contrato. Cada prueba escribe en memoria y relee el resultado con
 * {@link XSSFWorkbook}, que es lo que hace la importación.
 *
 * @author Jorge Enciso
 */
public class ExcelSheetWriterTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 24, 15, 30, 45);

    /** Procesador que usa el {@code process()} base (asigna por {@code setValue}). */
    static class DefaultProcessor extends ExcelRowProcessor<AppUser> {

        DefaultProcessor(Row row, int headerRowIndex) {
            super(row, AppUser.class, (Map<String, String>) null, headerRowIndex, null);
        }
    }

    /** Escribe el libro del escritor en memoria y lo vuelve a abrir. */
    private static Workbook reread(ExcelSheetWriter writer) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.writeTo(out);
        writer.close();
        return new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()));
    }

    @Test
    public void testEmptySheetKeepsHeaders() throws Exception {
        ExcelSheetWriter writer = new ExcelSheetWriter("Clientes",
                new String[]{"codigo", "nombre"}, new Class<?>[]{String.class, String.class});
        assertEquals(0, writer.getRowCount());
        try (Workbook wb = reread(writer)) {
            assertEquals(1, wb.getNumberOfSheets());
            Sheet sheet = wb.getSheet("Clientes");
            assertNotNull(sheet);
            assertEquals(0, sheet.getLastRowNum());
            assertEquals("codigo", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("nombre", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals(Arrays.asList("codigo", "nombre"), ExcelUtil.getHeaderNames(sheet));
        }
    }

    @Test
    public void testCellTypesByDeclaredType() throws Exception {
        String[] headers = {"texto", "numeroComoTexto", "espacios", "entero", "corto",
            "decimal", "fecha", "fechaSola", "booleano", "caracter", "nulo", "auto"};
        Class<?>[] types = {String.class, String.class, String.class, Long.class, Short.class,
            BigDecimal.class, LocalDateTime.class, LocalDate.class, Boolean.class, Character.class,
            String.class, null};
        ExcelSheetWriter writer = new ExcelSheetWriter("tipos", headers, types);
        writer.writeRow(new Object[]{"001", "0981123456", "V  ", 123456789012345L, (short) 7,
            new BigDecimal("1234.5678"), FECHA, LocalDate.of(2026, 1, 31), Boolean.TRUE, 'S',
            null, 42});
        assertEquals(1, writer.getRowCount());
        try (Workbook wb = reread(writer)) {
            Row row = wb.getSheetAt(0).getRow(1);
            Cell texto = row.getCell(0);
            assertEquals(CellType.STRING, texto.getCellType());
            assertEquals("001", texto.getStringCellValue());
            // Un texto que parece número sigue siendo texto: no se adivina.
            assertEquals(CellType.STRING, row.getCell(1).getCellType());
            assertEquals("0981123456", row.getCell(1).getStringCellValue());
            // Texto con espacios finales: vuelve con su largo completo.
            assertEquals("V  ", row.getCell(2).getStringCellValue());
            assertEquals(3, row.getCell(2).getStringCellValue().length());

            Cell entero = row.getCell(3);
            assertEquals(CellType.NUMERIC, entero.getCellType());
            assertEquals(123456789012345d, entero.getNumericCellValue());
            assertEquals("0", entero.getCellStyle().getDataFormatString());
            assertEquals(CellType.NUMERIC, row.getCell(4).getCellType());
            assertEquals("0", row.getCell(4).getCellStyle().getDataFormatString());

            Cell decimal = row.getCell(5);
            assertEquals(CellType.NUMERIC, decimal.getCellType());
            assertEquals(0, new BigDecimal("1234.5678").compareTo(BigDecimal.valueOf(decimal.getNumericCellValue())));
            assertEquals("General", decimal.getCellStyle().getDataFormatString());

            Cell fecha = row.getCell(6);
            assertTrue(DateUtil.isCellDateFormatted(fecha));
            assertEquals(FECHA, fecha.getLocalDateTimeCellValue());
            assertEquals("dd/mm/yyyy hh:mm:ss", fecha.getCellStyle().getDataFormatString());
            Cell fechaSola = row.getCell(7);
            assertTrue(DateUtil.isCellDateFormatted(fechaSola));
            assertEquals(LocalDate.of(2026, 1, 31), fechaSola.getLocalDateTimeCellValue().toLocalDate());

            assertEquals(CellType.STRING, row.getCell(8).getCellType());
            assertEquals("true", row.getCell(8).getStringCellValue());
            assertEquals("S", row.getCell(9).getStringCellValue());
            assertNull(row.getCell(10));
            // Columna sin tipo declarado: decide el valor (un entero es número).
            assertEquals(CellType.NUMERIC, row.getCell(11).getCellType());
            assertEquals(42d, row.getCell(11).getNumericCellValue());
        }
    }

    @Test
    public void testValueOfOtherTypeIsWrittenByItsOwnType() throws Exception {
        ExcelSheetWriter writer = new ExcelSheetWriter(null, new String[]{"entero", "texto"},
                new Class<?>[]{Long.class, String.class});
        writer.writeRow(new Object[]{"sin número", 15});
        try (Workbook wb = reread(writer)) {
            assertEquals("DATOS", wb.getSheetAt(0).getSheetName());
            Row row = wb.getSheetAt(0).getRow(1);
            assertEquals(CellType.STRING, row.getCell(0).getCellType());
            assertEquals("sin número", row.getCell(0).getStringCellValue());
            // Columna de texto con un valor numérico: sigue siendo texto.
            assertEquals(CellType.STRING, row.getCell(1).getCellType());
            assertEquals("15", row.getCell(1).getStringCellValue());
        }
    }

    /**
     * Ida y vuelta a nivel framework: lo escrito se lee con la importación
     * ({@link ExcelRowProcessor} con el {@code process()} base) sobre la
     * entidad de prueba del módulo, y los valores coinciden.
     */
    @Test
    public void testRoundTripThroughRowProcessor() throws Exception {
        String[] headers = {"code", "fullName", "telefono1", "idcompany", "type",
            "prbNumerico", "disabled", "expiredDate", "email"};
        Class<?>[] types = {String.class, String.class, String.class, Long.class, Short.class,
            BigDecimal.class, Boolean.class, LocalDateTime.class, String.class};
        ExcelSheetWriter writer = new ExcelSheetWriter("appuser", headers, types);
        writer.writeRow(new Object[]{"001", "Analista", "V  ", 123456789012345L, (short) 2,
            new BigDecimal("100.31"), Boolean.TRUE, FECHA, null});
        try (Workbook wb = reread(writer)) {
            Sheet sheet = wb.getSheetAt(0);
            DefaultProcessor processor = new DefaultProcessor(sheet.getRow(1), 0);
            assertEquals("", processor.checkMetaData());
            AppUser user = processor.process();
            assertEquals("001", user.getCode());
            assertEquals("Analista", user.getFullName());
            // Texto con espacios finales: la importación lo recibe completo
            // (getTelefono1 no recorta, a diferencia de otros getters de la
            // entidad de prueba).
            assertEquals("V  ", user.getTelefono1());
            assertEquals(Long.valueOf(123456789012345L), user.getIdcompany());
            assertEquals(Short.valueOf((short) 2), user.getType());
            assertEquals(0, new BigDecimal("100.31").compareTo((BigDecimal) user.getValue("prbNumerico")));
            assertEquals(Boolean.TRUE, user.getDisabled());
            assertEquals(FECHA, user.getExpiredDate());
            assertNull(user.getEmail());
            assertTrue(user.getErrors() == null || user.getErrors().isEmpty());
        }
    }

    /**
     * Un entero de más de 15 dígitos significativos se escribe como
     * texto (como número Excel lo redondearía); los demás siguen como número.
     */
    @Test
    public void testIntegerBeyondPrecisionIsText() throws Exception {
        ExcelSheetWriter writer = new ExcelSheetWriter("precision",
                new String[]{"quince", "dieciocho", "negativo", "cerosFinales", "grande", "auto", "entero", "decimal"},
                new Class<?>[]{Long.class, Long.class, Long.class, Long.class, java.math.BigInteger.class,
                    null, Integer.class, BigDecimal.class});
        writer.writeRow(new Object[]{999999999999999L, 123456789012345678L, -1234567890123456L,
            1_000_000_000_000_000_000L, new java.math.BigInteger("12345678901234567890123"),
            9007199254740993L, Integer.MAX_VALUE, new BigDecimal("12345678901234567.89")});
        try (Workbook wb = reread(writer)) {
            Row row = wb.getSheetAt(0).getRow(1);
            // 15 dígitos: número exacto, formato 0.
            assertEquals(CellType.NUMERIC, row.getCell(0).getCellType());
            assertEquals(999999999999999d, row.getCell(0).getNumericCellValue());
            assertEquals("0", row.getCell(0).getCellStyle().getDataFormatString());
            // 18 dígitos y 16 con signo: texto con todos sus dígitos.
            assertEquals(CellType.STRING, row.getCell(1).getCellType());
            assertEquals("123456789012345678", row.getCell(1).getStringCellValue());
            assertEquals(CellType.STRING, row.getCell(2).getCellType());
            assertEquals("-1234567890123456", row.getCell(2).getStringCellValue());
            // 10^18: un solo dígito significativo, sigue como número exacto.
            assertEquals(CellType.NUMERIC, row.getCell(3).getCellType());
            assertEquals(1e18d, row.getCell(3).getNumericCellValue());
            // BigInteger largo y Long en una columna sin tipo declarado: texto.
            assertEquals("12345678901234567890123", row.getCell(4).getStringCellValue());
            assertEquals("9007199254740993", row.getCell(5).getStringCellValue());
            // Integer: nunca llega a 15 dígitos.
            assertEquals(CellType.NUMERIC, row.getCell(6).getCellType());
            // Los decimales siguen como número (la regla es solo para enteros) y,
            // con más de 15 dígitos significativos, no vuelven iguales: este caso
            // fija esa pérdida conocida (documentada en «Precisión»).
            assertEquals(CellType.NUMERIC, row.getCell(7).getCellType());
            assertNotEquals(0, new BigDecimal("12345678901234567.89")
                    .compareTo(new BigDecimal(String.valueOf(row.getCell(7).getNumericCellValue()))));
        }
        assertFalse(ExcelSheetWriter.exceedsNumericPrecision(0L));
        assertFalse(ExcelSheetWriter.exceedsNumericPrecision(Long.valueOf(-999999999999999L)));
        assertTrue(ExcelSheetWriter.exceedsNumericPrecision(Long.MIN_VALUE));
        assertTrue(ExcelSheetWriter.exceedsNumericPrecision(Long.MAX_VALUE));
        assertFalse(ExcelSheetWriter.exceedsNumericPrecision(Double.valueOf(1e20)));
    }

    /** Ida y vuelta de un Long de 18 dígitos: la importación lo lee completo. */
    @Test
    public void testRoundTripOfLongBeyondPrecision() throws Exception {
        ExcelSheetWriter writer = new ExcelSheetWriter("appuser", new String[]{"code", "idcompany"},
                new Class<?>[]{String.class, Long.class});
        writer.writeRow(new Object[]{"largo", 123456789012345678L});
        writer.writeRow(new Object[]{"corto", 42L});
        try (Workbook wb = reread(writer)) {
            Sheet sheet = wb.getSheetAt(0);
            DefaultProcessor processor = new DefaultProcessor(sheet.getRow(1), 0);
            assertEquals("", processor.checkMetaData());
            AppUser largo = processor.process();
            assertEquals(Long.valueOf(123456789012345678L), largo.getIdcompany());
            assertTrue(largo.getErrors() == null || largo.getErrors().isEmpty());
            processor.setRow(sheet.getRow(2));
            assertEquals(Long.valueOf(42L), processor.process().getIdcompany());
        }
    }

    /** La fila de encabezados puede no ser la primera; la importación la lee igual. */
    @Test
    public void testHeaderRowIndex() throws Exception {
        ExcelSheetWriter writer = new ExcelSheetWriter("datos", new String[]{"code", "fullName"},
                new Class<?>[]{String.class, String.class}, 2);
        assertEquals(2, writer.getHeaderRowIndex());
        writer.writeRow(new Object[]{"A1", "Primero"});
        try (Workbook wb = reread(writer)) {
            Sheet sheet = wb.getSheetAt(0);
            assertNull(sheet.getRow(0));
            assertNull(sheet.getRow(1));
            assertEquals("code", sheet.getRow(2).getCell(0).getStringCellValue());
            AppUser user = new DefaultProcessor(sheet.getRow(3), 2).process();
            assertEquals("A1", user.getCode());
            assertEquals("Primero", user.getFullName());
        }
    }

    /** Más filas que la ventana de SXSSF: el archivo se lee completo y en orden. */
    @Test
    public void testMoreRowsThanWindow() throws Exception {
        int total = ExcelSheetWriter.ROW_WINDOW * 3 + 17;
        ExcelSheetWriter writer = new ExcelSheetWriter("muchas", new String[]{"n", "texto"},
                new Class<?>[]{Integer.class, String.class});
        for (int i = 0; i < total; i++) {
            writer.writeRow(new Object[]{i, "fila " + i});
        }
        assertEquals(total, writer.getRowCount());
        try (Workbook wb = reread(writer)) {
            Sheet sheet = wb.getSheetAt(0);
            assertEquals(total, sheet.getLastRowNum());
            for (int i = 0; i < total; i++) {
                Row row = sheet.getRow(i + 1);
                assertEquals(i, (int) row.getCell(0).getNumericCellValue());
                assertEquals("fila " + i, row.getCell(1).getStringCellValue());
            }
        }
    }

    /** {@code close()} borra los temporales que SXSSF creó para la hoja. */
    @Test
    public void testCloseDeletesTempFiles() throws Exception {
        File poiTemp = new File(System.getProperty("java.io.tmpdir"), "poifiles");
        Set<String> before = tempFiles(poiTemp);
        ExcelSheetWriter writer = new ExcelSheetWriter("temporales", new String[]{"n"},
                new Class<?>[]{Integer.class});
        for (int i = 0; i < ExcelSheetWriter.ROW_WINDOW * 2; i++) {
            writer.writeRow(new Object[]{i});
        }
        Set<String> created = tempFiles(poiTemp);
        created.removeAll(before);
        assertFalse(created.isEmpty(), "SXSSF no creó temporales");
        writer.writeTo(new ByteArrayOutputStream());
        writer.close();
        Set<String> remaining = tempFiles(poiTemp);
        remaining.retainAll(created);
        assertTrue(remaining.isEmpty(), "quedaron temporales: " + remaining);
    }

    private static Set<String> tempFiles(File folder) {
        Set<String> names = new HashSet<>();
        String[] list = folder.list();
        if (list != null) {
            names.addAll(Arrays.asList(list));
        }
        return names;
    }

    @Test
    public void testColumnFormatAndWidths() throws Exception {
        ExcelSheetWriter writer = new ExcelSheetWriter("formato",
                new String[]{"importe", "otroImporte", "fecha", "nombre"},
                new Class<?>[]{BigDecimal.class, BigDecimal.class, LocalDateTime.class, String.class});
        writer.setColumnFormat(0, "#,##0.00");
        writer.setColumnFormat(1, "#,##0.00");
        writer.setColumnWidth(3, 40);
        writer.writeRow(new Object[]{new BigDecimal("10.5"), new BigDecimal("3"), FECHA, "x"});
        try (Workbook wb = reread(writer)) {
            Sheet sheet = wb.getSheetAt(0);
            Row row = sheet.getRow(1);
            assertEquals("#,##0.00", row.getCell(0).getCellStyle().getDataFormatString());
            // Mismo formato, mismo estilo: no se crea uno por columna.
            assertEquals(row.getCell(0).getCellStyle().getIndex(), row.getCell(1).getCellStyle().getIndex());
            assertEquals(10.5d, row.getCell(0).getNumericCellValue());
            assertEquals(ExcelUtil.charsToWidthUnits(ExcelUtil.WIDTH_DATE), sheet.getColumnWidth(2));
            assertEquals(ExcelUtil.charsToWidthUnits(40), sheet.getColumnWidth(3));
        }
    }

    @Test
    public void testSheetNameIsSanitized() throws Exception {
        ExcelSheetWriter writer = new ExcelSheetWriter("a/b:c*d?e[f]g un nombre demasiado largo para Excel",
                new String[]{"x"}, null);
        String name = writer.getSheetName();
        assertTrue(name.length() <= 31);
        for (char c : "/\\:*?[]".toCharArray()) {
            assertEquals(-1, name.indexOf(c), "carácter inválido en " + name);
        }
        writer.close();
    }

    /** Los encabezados se escriben sin los espacios de los extremos. */
    @Test
    public void testHeadersAreTrimmed() throws Exception {
        ExcelSheetWriter writer = new ExcelSheetWriter("h", new String[]{" code ", "fullName"},
                new Class<?>[]{String.class, String.class});
        assertArrayEquals(new String[]{"code", "fullName"}, writer.getHeaders());
        writer.writeRow(new Object[]{"x1", "Equis"});
        try (Workbook wb = reread(writer)) {
            Sheet sheet = wb.getSheetAt(0);
            assertEquals("code", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("x1", new DefaultProcessor(sheet.getRow(1), 0).process().getCode());
        }
    }

    /** Texto de más de 32.767 caracteres: se recorta y se cuenta; la importación lo lee recortado. */
    @Test
    public void testLongTextIsTruncatedAndCounted() throws Exception {
        String largo = "x".repeat(40_000);
        String justo = "y".repeat(ExcelSheetWriter.MAX_TEXT_LENGTH);
        // Un carácter de dos unidades justo en el borde no se parte.
        String conPar = "z".repeat(ExcelSheetWriter.MAX_TEXT_LENGTH - 1) + "\uD83D\uDE00" + "z";
        ExcelSheetWriter writer = new ExcelSheetWriter("textos",
                new String[]{"code", "telefono1", "celular1", "celular2"},
                new Class<?>[]{String.class, String.class, String.class, null});
        writer.writeRow(new Object[]{"t1", largo, justo, conPar});
        assertEquals(2, writer.getTruncatedCellCount());
        try (Workbook wb = reread(writer)) {
            Row row = wb.getSheetAt(0).getRow(1);
            assertEquals(ExcelSheetWriter.MAX_TEXT_LENGTH, row.getCell(1).getStringCellValue().length());
            assertEquals(justo, row.getCell(2).getStringCellValue());
            assertEquals(ExcelSheetWriter.MAX_TEXT_LENGTH - 1, row.getCell(3).getStringCellValue().length());
            AppUser user = new DefaultProcessor(row, 0).process();
            assertEquals(largo.substring(0, ExcelSheetWriter.MAX_TEXT_LENGTH), user.getTelefono1());
        }
    }

    /**
     * Fechas fuera del calendario de Excel: texto en el formato que lee la
     * importación (dd/MM/yyyy o dd/MM/yyyy HH:mm:ss), contadas, y la ida y
     * vuelta por {@link ExcelRowProcessor} las trae con su valor.
     */
    @Test
    public void testOutOfRangeDatesRoundTrip() throws Exception {
        LocalDateTime antigua = LocalDateTime.of(1850, 5, 1, 10, 15, 30);
        LocalDateTime utilValor = LocalDateTime.of(1720, 3, 4, 5, 6, 7);
        java.util.Date util = java.util.Date.from(utilValor.atZone(java.time.ZoneId.systemDefault()).toInstant());
        // Columnas con el nombre de atributos LocalDateTime de la entidad de prueba.
        ExcelSheetWriter writer = new ExcelSheetWriter("fechas",
                new String[]{"code", "expiredDate", "emailVerifiedDate", "fechamodificacion",
                    "loginLockDate", "loginBlockedDate", "loginFailDate", "borde"},
                new Class<?>[]{String.class, LocalDateTime.class, LocalDate.class, java.util.Date.class,
                    java.sql.Date.class, java.sql.Timestamp.class, LocalDate.class, LocalDateTime.class});
        writer.writeRow(new Object[]{"f1", antigua.withNano(500_000_000), LocalDate.of(1899, 12, 31), util,
            java.sql.Date.valueOf("1800-02-03"), java.sql.Timestamp.valueOf("1700-01-02 03:04:05"),
            LocalDate.of(10000, 1, 1), LocalDateTime.of(1900, 1, 1, 0, 0)});
        assertEquals(6, writer.getOutOfRangeDateCount());
        try (Workbook wb = reread(writer)) {
            Row row = wb.getSheetAt(0).getRow(1);
            assertEquals("01/05/1850 10:15:30", row.getCell(1).getStringCellValue());
            assertEquals("@", row.getCell(1).getCellStyle().getDataFormatString());
            assertEquals("31/12/1899", row.getCell(2).getStringCellValue());
            assertEquals("04/03/1720 05:06:07", row.getCell(3).getStringCellValue());
            assertEquals("03/02/1800", row.getCell(4).getStringCellValue());
            assertEquals("02/01/1700 03:04:05", row.getCell(5).getStringCellValue());
            // Año de cinco cifras: el formato le pone signo y la fecha sola va con la
            // hora explícita (la importación solo completa un texto de diez caracteres).
            assertEquals("01/01/+10000 00:00:00", row.getCell(6).getStringCellValue());
            // 1900-01-01 es el primer día de Excel: sigue como fecha.
            assertTrue(DateUtil.isCellDateFormatted(row.getCell(7)));
            assertEquals(LocalDateTime.of(1900, 1, 1, 0, 0), row.getCell(7).getLocalDateTimeCellValue());
            // Ida y vuelta: la importación acepta el texto y lo convierte a LocalDateTime
            // (la fecha sola, a las 00:00:00; sin las fracciones de segundo).
            assertNull(ExcelUtil.getAssignableTypeError(row.getCell(1), LocalDateTime.class, "expiredDate"));
            DefaultProcessor processor = new DefaultProcessor(row, 0);
            assertEquals("", processor.checkMetaData());
            AppUser user = processor.process();
            assertTrue(user.getErrors() == null || user.getErrors().isEmpty());
            assertEquals(antigua, user.getExpiredDate());
            assertEquals(LocalDateTime.of(1899, 12, 31, 0, 0), user.getEmailVerifiedDate());
            assertEquals(utilValor, user.getValue("fechamodificacion"));
            assertEquals(LocalDateTime.of(1800, 2, 3, 0, 0), user.getValue("loginLockDate"));
            assertEquals(LocalDateTime.of(1700, 1, 2, 3, 4, 5), user.getValue("loginBlockedDate"));
            assertEquals(LocalDateTime.of(10000, 1, 1, 0, 0), user.getValue("loginFailDate"));
        }
    }

    /**
     * Límites de la importación, no del escritor, que la prueba deja fijados:
     * un año anterior al 1 se escribe con el año de la era y vuelve como otro
     * año, y la conversión de la importación no produce {@code LocalDate}.
     */
    @Test
    public void testOutOfRangeDateImportLimits() throws Exception {
        ExcelSheetWriter writer = new ExcelSheetWriter("limites", new String[]{"code", "expiredDate"},
                new Class<?>[]{String.class, LocalDate.class});
        writer.writeRow(new Object[]{"l1", LocalDate.of(0, 6, 1)});
        assertEquals(1, writer.getOutOfRangeDateCount());
        try (Workbook wb = reread(writer)) {
            Row row = wb.getSheetAt(0).getRow(1);
            // El año 0 es el 1 antes de la era común: sin era en el formato, vuelve como el 1.
            assertEquals("01/06/0001", row.getCell(1).getStringCellValue());
            assertEquals(LocalDateTime.of(1, 6, 1, 0, 0), new DefaultProcessor(row, 0).process().getExpiredDate());
        }
        // Un atributo LocalDate no recibe la fecha: convertValue solo produce LocalDateTime.
        assertTrue(ExcelUtil.convertValue("01/06/1850", LocalDate.class) instanceof String);
    }

    /** Fechas dentro del calendario de Excel con java.sql.Date y Timestamp. */
    @Test
    public void testSqlDateAndTimestampInRange() throws Exception {
        ExcelSheetWriter writer = new ExcelSheetWriter("sql", new String[]{"sql", "marca"},
                new Class<?>[]{java.sql.Date.class, java.sql.Timestamp.class});
        writer.writeRow(new Object[]{java.sql.Date.valueOf("2026-11-25"),
            java.sql.Timestamp.valueOf("2026-11-25 08:09:10")});
        assertEquals(0, writer.getOutOfRangeDateCount());
        try (Workbook wb = reread(writer)) {
            Row row = wb.getSheetAt(0).getRow(1);
            assertEquals("dd/mm/yyyy", row.getCell(0).getCellStyle().getDataFormatString());
            assertEquals(LocalDate.of(2026, 11, 25), row.getCell(0).getLocalDateTimeCellValue().toLocalDate());
            assertEquals("dd/mm/yyyy hh:mm:ss", row.getCell(1).getCellStyle().getDataFormatString());
            assertEquals(LocalDateTime.of(2026, 11, 25, 8, 9, 10), row.getCell(1).getLocalDateTimeCellValue());
        }
    }

    /**
     * Horas solas ({@code java.sql.Time}, que es un {@code Date} y no admite
     * {@code toInstant()}, y {@code LocalTime}): la fracción del día con
     * formato de hora, también en una columna sin tipo o de tipo fecha.
     */
    @Test
    public void testTimeValues() throws Exception {
        ExcelSheetWriter writer = new ExcelSheetWriter("horas",
                new String[]{"hora", "local", "auto", "comoFecha", "mascara"},
                new Class<?>[]{java.sql.Time.class, java.time.LocalTime.class, null, LocalDateTime.class,
                    java.sql.Time.class});
        writer.setColumnFormat(4, "hh:mm");
        writer.writeRow(new Object[]{java.sql.Time.valueOf("10:15:30"), java.time.LocalTime.of(23, 59, 59),
            java.sql.Time.valueOf("00:00:00"), java.sql.Time.valueOf("12:00:00"), java.sql.Time.valueOf("08:30:00")});
        assertEquals(0, writer.getOutOfRangeDateCount());
        try (Workbook wb = reread(writer)) {
            Row row = wb.getSheetAt(0).getRow(1);
            Cell hora = row.getCell(0);
            assertEquals(CellType.NUMERIC, hora.getCellType());
            assertEquals("hh:mm:ss", hora.getCellStyle().getDataFormatString());
            assertTrue(DateUtil.isCellDateFormatted(hora));
            assertEquals((10 * 3600 + 15 * 60 + 30) / 86400d, hora.getNumericCellValue(), 1e-12);
            assertEquals(java.time.LocalTime.of(10, 15, 30),
                    DateUtil.getLocalDateTime(hora.getNumericCellValue()).toLocalTime());
            assertEquals(java.time.LocalTime.of(23, 59, 59),
                    DateUtil.getLocalDateTime(row.getCell(1).getNumericCellValue()).toLocalTime());
            assertEquals(0d, row.getCell(2).getNumericCellValue());
            assertEquals("hh:mm:ss", row.getCell(2).getCellStyle().getDataFormatString());
            // En una columna declarada fecha, el Time se escribe por su propio tipo: hora.
            assertEquals(0.5d, row.getCell(3).getNumericCellValue());
            assertEquals("hh:mm:ss", row.getCell(3).getCellStyle().getDataFormatString());
            assertEquals("hh:mm", row.getCell(4).getCellStyle().getDataFormatString());
        }
    }

    /** NaN e infinito: celda vacía, contadas; la importación las lee como nulo. */
    @Test
    public void testNaNAndInfinityAreBlank() throws Exception {
        ExcelSheetWriter writer = new ExcelSheetWriter("numeros",
                new String[]{"code", "prbNumerico", "inf", "flotante", "normal"},
                new Class<?>[]{String.class, Double.class, Double.class, Float.class, Double.class});
        writer.writeRow(new Object[]{"n1", Double.NaN, Double.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 2.5d});
        assertEquals(3, writer.getInvalidNumberCount());
        try (Workbook wb = reread(writer)) {
            Row row = wb.getSheetAt(0).getRow(1);
            for (int c = 1; c <= 3; c++) {
                Cell cell = row.getCell(c);
                assertTrue(cell == null || cell.getCellType() == CellType.BLANK, "columna " + c);
            }
            assertEquals(2.5d, row.getCell(4).getNumericCellValue());
            AppUser user = new DefaultProcessor(row, 0).process();
            assertNull(user.getValue("prbNumerico"));
            assertEquals("n1", user.getCode());
        }
    }

    /** Las celdas de texto llevan el formato de texto (@): Excel no las convierte al editarlas. */
    @Test
    public void testTextCellsUseTextFormat() throws Exception {
        ExcelSheetWriter writer = new ExcelSheetWriter("formato",
                new String[]{"texto", "largo", "booleano", "otro", "entero"},
                new Class<?>[]{String.class, Long.class, Boolean.class, Object.class, Long.class});
        writer.writeRow(new Object[]{"001", 123456789012345678L, Boolean.FALSE, java.util.UUID.randomUUID(), 7L});
        try (Workbook wb = reread(writer)) {
            Row row = wb.getSheetAt(0).getRow(1);
            for (int c = 0; c <= 3; c++) {
                assertEquals(CellType.STRING, row.getCell(c).getCellType(), "columna " + c);
                assertEquals("@", row.getCell(c).getCellStyle().getDataFormatString(), "columna " + c);
            }
            assertEquals("false", row.getCell(2).getStringCellValue());
            assertEquals("0", row.getCell(4).getCellStyle().getDataFormatString());
        }
    }

    @Test
    public void testInvalidArguments() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new ExcelSheetWriter("h", null, null));
        assertThrows(IllegalArgumentException.class, () -> new ExcelSheetWriter("h", new String[0], null));
        assertThrows(IllegalArgumentException.class,
                () -> new ExcelSheetWriter("h", new String[]{"a", "a"}, null));
        assertThrows(IllegalArgumentException.class,
                () -> new ExcelSheetWriter("h", new String[]{"a", " "}, null));
        // Solo difieren en los espacios de los extremos: la importación los ve iguales.
        assertThrows(IllegalArgumentException.class,
                () -> new ExcelSheetWriter("h", new String[]{"a", "a "}, null));
        assertThrows(IllegalArgumentException.class,
                () -> new ExcelSheetWriter("h", new String[]{" b", "b"}, null));
        assertThrows(IllegalArgumentException.class,
                () -> new ExcelSheetWriter("h", new String[]{"a", null}, null));
        assertThrows(IllegalArgumentException.class,
                () -> new ExcelSheetWriter("h", new String[]{"a", "b"}, new Class<?>[]{String.class}));
        assertThrows(IllegalArgumentException.class,
                () -> new ExcelSheetWriter("h", new String[]{"a"}, new Class<?>[]{byte[].class}));
        assertThrows(IllegalArgumentException.class,
                () -> new ExcelSheetWriter("h", new String[]{"a"}, null, -1));
    }

    @Test
    public void testStateGuards() throws Exception {
        ExcelSheetWriter writer = new ExcelSheetWriter("h", new String[]{"a"}, null);
        assertThrows(IllegalArgumentException.class, () -> writer.writeRow(null));
        assertThrows(IllegalArgumentException.class, () -> writer.writeRow(new Object[]{1, 2}));
        assertThrows(IllegalArgumentException.class, () -> writer.writeRow(new Object[]{new byte[]{1}}));
        assertThrows(IllegalArgumentException.class, () -> writer.setColumnFormat(1, "0"));
        writer.writeTo(new ByteArrayOutputStream());
        assertThrows(IllegalStateException.class, () -> writer.writeRow(new Object[]{"x"}));
        assertThrows(IllegalStateException.class, () -> writer.writeTo(new ByteArrayOutputStream()));
        writer.close();
        writer.close();
        assertThrows(IllegalStateException.class, () -> writer.setColumnWidth(0, 10));
    }
}

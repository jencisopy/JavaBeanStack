/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2018 - 2027 Jorge Enciso
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

import java.io.File;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.javabeanstack.error.IErrorReg;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de {@link ExcelRowProcessor}.
 * <p>
 * La mayoría de las pruebas construyen planillas en memoria con POI para tener
 * control determinista sobre los tipos de celda, fórmulas, fechas, encabezados
 * duplicados, etc. Una prueba adicional usa la planilla real {@code prueba3.xlsx}
 * que ya existe en este paquete.
 *
 * @author Jorge Enciso
 */
public class ExcelRowProcessorTest {

    /**
     * Subclase concreta mínima usada para ejercitar la clase abstracta. Su
     * {@link #process()} instancia un {@link AppUser} a partir de
     * {@link #targetType} y le asigna los valores leídos de la fila,
     * convirtiendo los tipos nativos de Excel a los del atributo destino.
     */
    static class AppUserRowProcessor extends ExcelRowProcessor<AppUser> {

        AppUserRowProcessor(Row row, Map<String, String> headToField, Map<String, Object> properties) {
            super(row, AppUser.class, headToField, properties);
        }

        @Override
        public AppUser process() {
            AppUser user = new AppUser();
            user.setCode((String) valueOfField("code"));
            user.setFullName((String) valueOfField("fullName"));
            user.setDescription((String) valueOfField("description"));
            Object idcompany = valueOfField("idcompany");
            if (idcompany instanceof Double) {
                user.setIdcompany(((Double) idcompany).longValue());
            }
            Object type = valueOfField("type");
            if (type instanceof Double) {
                user.setType(((Double) type).shortValue());
            }
            Object disabled = valueOfField("disabled");
            if (disabled instanceof Boolean) {
                user.setDisabled((Boolean) disabled);
            } else if (disabled instanceof Double) {
                user.setDisabled(((Double) disabled) != 0d);
            }
            Object expiredDate = valueOfField("expiredDate");
            if (expiredDate instanceof LocalDateTime) {
                user.setExpiredDate((LocalDateTime) expiredDate);
            }
            return user;
        }
    }

    /**
     * Subclase que NO sobrescribe {@code process()}: usa la implementación por
     * defecto de {@link ExcelRowProcessor} (instancia el destino y asigna vía
     * {@code setValue}).
     */
    static class DefaultProcessor extends ExcelRowProcessor<AppUser> {

        DefaultProcessor(Row row, Map<String, String> headToField, Map<String, Object> properties) {
            super(row, AppUser.class, headToField, properties);
        }
    }

    private static final String[] HEADERS = {
        "code", "fullName", "description", "idcompany", "type", "prbNumerico", "disabled", "expiredDate"
    };

    private static final LocalDateTime EXPIRED = LocalDateTime.of(2030, 12, 31, 10, 15, 30);

    /**
     * Construye una planilla con una fila de encabezados (igual a los nombres de
     * atributo de {@link AppUser}) y una fila de datos con tipos nativos
     * variados (texto, numérico, booleano y fecha).
     */
    private Workbook buildAppUserWorkbook() {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("data");
        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }
        CreationHelper helper = wb.getCreationHelper();
        CellStyle dateStyle = wb.createCellStyle();
        dateStyle.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));

        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue("jenciso");          // code   -> String
        data.createCell(1).setCellValue("Jorge Enciso");     // fullName -> String
        data.createCell(2).setCellValue("Analista");         // description -> String
        data.createCell(3).setCellValue(7d);                 // idcompany -> NUMERIC
        data.createCell(4).setCellValue(2d);                 // type -> NUMERIC
        data.createCell(5).setCellValue(100.31d);            // prbNumerico -> NUMERIC
        data.createCell(6).setCellValue(true);               // disabled -> BOOLEAN
        Cell dateCell = data.createCell(7);                  // expiredDate -> fecha
        dateCell.setCellValue(Timestamp.valueOf(EXPIRED));
        dateCell.setCellStyle(dateStyle);
        return wb;
    }

    private Map<String, Object> props(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    // ----------------------------------------------------------------------
    // Lectura de valores nativos
    // ----------------------------------------------------------------------

    @Test
    public void testValueOfColumnNativeTypes() throws Exception {
        try (Workbook wb = buildAppUserWorkbook()) {
            Row data = wb.getSheetAt(0).getRow(1);
            AppUserRowProcessor p = new AppUserRowProcessor(data, null, null);

            assertEquals("jenciso", p.valueOfColumn("code"));
            assertTrue(p.valueOfColumn("idcompany") instanceof Double);
            assertEquals(7d, (Double) p.valueOfColumn("idcompany"), 0.0001);
            assertEquals(Boolean.TRUE, p.valueOfColumn("disabled"));
            assertTrue(p.valueOfColumn("expiredDate") instanceof LocalDateTime);
            assertEquals(EXPIRED, p.valueOfColumn("expiredDate"));
        }
    }

    @Test
    public void testValueOfColumnUnknownReturnsNull() throws Exception {
        try (Workbook wb = buildAppUserWorkbook()) {
            Row data = wb.getSheetAt(0).getRow(1);
            AppUserRowProcessor p = new AppUserRowProcessor(data, null, null);
            // encabezado que no figura en headToIndex
            assertNull(p.valueOfColumn("noExiste"));
            // atributo sin mapeo en fieldToHead
            assertNull(p.valueOfField("atributoInexistente"));
        }
    }

    @Test
    public void testValueOfFieldUsesMapping() throws Exception {
        try (Workbook wb = buildAppUserWorkbook()) {
            // Encabezados "humanos" mapeados a atributos
            Map<String, String> headToField = new HashMap<>();
            // renombramos los encabezados de la planilla
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            header.getCell(0).setCellValue("Código");
            header.getCell(1).setCellValue("Nombre completo");
            headToField.put("Código", "code");
            headToField.put("Nombre completo", "fullName");

            Row data = sheet.getRow(1);
            AppUserRowProcessor p = new AppUserRowProcessor(data, headToField, null);

            assertEquals("jenciso", p.valueOfField("code"));
            assertEquals("Jorge Enciso", p.valueOfField("fullName"));
            // atributo sin mapeo -> null
            assertNull(p.valueOfField("description"));
        }
    }

    // ----------------------------------------------------------------------
    // Mapas auto-generados
    // ----------------------------------------------------------------------

    @Test
    public void testDefaultMapsGeneratedFromHeaders() throws Exception {
        try (Workbook wb = buildAppUserWorkbook()) {
            Row data = wb.getSheetAt(0).getRow(1);
            AppUserRowProcessor p = new AppUserRowProcessor(data, null, null);

            assertEquals(HEADERS.length, p.getHeadToIndex().size());
            assertEquals(Integer.valueOf(0), p.getHeadToIndex().get("code"));
            assertEquals(Integer.valueOf(7), p.getHeadToIndex().get("expiredDate"));
            // headToField por defecto: header == field
            assertEquals("code", p.getHeadToField().get("code"));
            // fieldToHead inverso
            assertEquals("code", p.getFieldToHead().get("code"));
        }
    }

    // ----------------------------------------------------------------------
    // process()
    // ----------------------------------------------------------------------

    @Test
    public void testProcessBuildsTarget() throws Exception {
        try (Workbook wb = buildAppUserWorkbook()) {
            Row data = wb.getSheetAt(0).getRow(1);
            AppUser user = new AppUserRowProcessor(data, null, null).process();

            assertEquals("jenciso", user.getCode());
            assertEquals("Jorge Enciso", user.getFullName());
            assertEquals("Analista", user.getDescription());
            assertEquals(Long.valueOf(7L), user.getIdcompany());
            assertEquals(Short.valueOf((short) 2), user.getType());
            assertTrue(user.getDisabled());
            assertEquals(EXPIRED, user.getExpiredDate());
        }
    }

    @Test
    public void testDefaultProcessAssignsViaSetValue() throws Exception {
        try (Workbook wb = buildAppUserWorkbook()) {
            Row data = wb.getSheetAt(0).getRow(1);
            // process() por defecto: instancia AppUser y asigna con setValue,
            // convirtiendo Double->Long/Short, etc.
            AppUser user = new DefaultProcessor(data, null, null).process();

            assertEquals("jenciso", user.getCode());
            assertEquals("Jorge Enciso", user.getFullName());
            assertEquals("Analista", user.getDescription());
            assertEquals(Long.valueOf(7L), user.getIdcompany());
            assertEquals(Short.valueOf((short) 2), user.getType());
            assertTrue(user.getDisabled());
            assertEquals(EXPIRED, user.getExpiredDate());
        }
    }

    @Test
    public void testFromExcelToDataRowWithProcessor() throws Exception {
        try (Workbook wb = buildAppUserWorkbook()) {
            Sheet sheet = wb.getSheetAt(0);
            // agregar dos filas de datos más (índices 2 y 3)
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("user2");
            r2.createCell(1).setCellValue("Usuario Dos");
            Row r3 = sheet.createRow(3);
            r3.createCell(0).setCellValue("user3");
            r3.createCell(1).setCellValue("Usuario Tres");

            IExcelRowProcessor<AppUser> processor = new DefaultProcessor(sheet.getRow(1), null, null);

            // todas las filas desde la 1 (rowCount <= 0)
            List<AppUser> all = ExcelUtil.fromExcelToDataRow(sheet, processor, 1, 0);
            assertEquals(3, all.size());
            assertEquals("jenciso", all.get(0).getCode());
            assertEquals("user2", all.get(1).getCode());
            assertEquals("user3", all.get(2).getCode());

            // solo 2 filas a partir de la 1
            List<AppUser> twoRows = ExcelUtil.fromExcelToDataRow(sheet, processor, 1, 2);
            assertEquals(2, twoRows.size());
            assertEquals("jenciso", twoRows.get(0).getCode());
            assertEquals("user2", twoRows.get(1).getCode());

            // arrancando desde la fila 0: la fila de encabezados (índice 0) se omite
            List<AppUser> fromHeader = ExcelUtil.fromExcelToDataRow(sheet, processor, 0, 0);
            assertEquals(3, fromHeader.size());
            assertEquals("jenciso", fromHeader.get(0).getCode());
        }
    }

    @Test
    public void testSetRowReusesProcessor() throws Exception {
        try (Workbook wb = buildAppUserWorkbook()) {
            Sheet sheet = wb.getSheetAt(0);
            // segunda fila de datos
            Row data2 = sheet.createRow(2);
            data2.createCell(0).setCellValue("otro");
            data2.createCell(1).setCellValue("Otro Usuario");

            AppUserRowProcessor p = new AppUserRowProcessor(sheet.getRow(1), null, null);
            assertEquals("jenciso", p.valueOfField("code"));
            p.setRow(data2);
            assertEquals("otro", p.valueOfField("code"));
            assertEquals("Otro Usuario", p.valueOfField("fullName"));
        }
    }

    // ----------------------------------------------------------------------
    // Fórmulas (evaluación activa, sin depender del caché)
    // ----------------------------------------------------------------------

    @Test
    public void testFormulaCellsAreEvaluated() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("numero");
            header.createCell(1).setCellValue("texto");
            Row data = sheet.createRow(1);
            data.createCell(0).setCellFormula("2+3");          // sin valor cacheado
            data.createCell(1).setCellFormula("\"AB\"&\"CD\""); // concatenación

            AppUserRowProcessor p = new AppUserRowProcessor(data, null, null);
            Object numero = p.valueOfColumn("numero");
            assertTrue(numero instanceof Double);
            assertEquals(5d, (Double) numero, 0.0001);
            assertEquals("ABCD", p.valueOfColumn("texto"));
        }
    }

    // ----------------------------------------------------------------------
    // checkMetaData
    // ----------------------------------------------------------------------

    @Test
    public void testCheckMetaDataValid() throws Exception {
        try (Workbook wb = buildAppUserWorkbook()) {
            Row data = wb.getSheetAt(0).getRow(1);
            AppUserRowProcessor p = new AppUserRowProcessor(data, null, null);
            assertEquals("", p.checkMetaData());
        }
    }

    @Test
    public void testCheckMetaDataDuplicateHeader() throws Exception {
        try (Workbook wb = buildAppUserWorkbook()) {
            Sheet sheet = wb.getSheetAt(0);
            // duplicar el encabezado "code" en la columna del medio
            sheet.getRow(0).getCell(2).setCellValue("code");
            Row data = sheet.getRow(1);
            AppUserRowProcessor p = new AppUserRowProcessor(data, null, null);
            String msg = p.checkMetaData();
            assertTrue(msg.contains("duplicado"), msg);
        }
    }

    @Test
    public void testCheckMetaDataNonTextHeader() throws Exception {
        try (Workbook wb = buildAppUserWorkbook()) {
            Sheet sheet = wb.getSheetAt(0);
            sheet.getRow(0).getCell(1).setCellValue(123d); // encabezado numérico
            Row data = sheet.getRow(1);
            AppUserRowProcessor p = new AppUserRowProcessor(data, null, null);
            String msg = p.checkMetaData();
            assertTrue(msg.contains("no tiene nombre válido"), msg);
        }
    }

    @Test
    public void testCheckMetaDataUnknownFieldDependsOnProperty() throws Exception {
        try (Workbook wb = buildAppUserWorkbook()) {
            Sheet sheet = wb.getSheetAt(0);
            // un encabezado que no corresponde a ningún atributo de AppUser
            sheet.getRow(0).getCell(2).setCellValue("columnaInventada");
            Row data = sheet.getRow(1);

            // Por defecto (allowFieldNotExist = true): no se reporta como error
            AppUserRowProcessor lenient = new AppUserRowProcessor(data, null, null);
            assertEquals("", lenient.checkMetaData());

            // allowFieldNotExist = false: se exige que el atributo exista
            AppUserRowProcessor strict = new AppUserRowProcessor(
                    data, null, props("allowFieldNotExist", false));
            String msg = strict.checkMetaData();
            assertTrue(msg.contains("columnaInventada"), msg);
            assertTrue(msg.contains("incorrecta"), msg);
        }
    }

    @Test
    public void testCheckMetaDataNotAssignableValue() throws Exception {
        try (Workbook wb = buildAppUserWorkbook()) {
            Sheet sheet = wb.getSheetAt(0);
            // poner un texto no numérico en la columna idcompany (Long): no es convertible
            sheet.getRow(1).getCell(3).setCellValue("no-numerico");
            Row data = sheet.getRow(1);
            AppUserRowProcessor p = new AppUserRowProcessor(data, null, null);
            String msg = p.checkMetaData();
            assertTrue(msg.contains("no es convertible"), msg);
            assertTrue(msg.contains("idcompany"), msg);
        }
    }

    // ----------------------------------------------------------------------
    // Planilla real existente
    // ----------------------------------------------------------------------

    @Test
    public void testWithRealSpreadsheet() throws Exception {
        String path = "./src/test/java/org/javabeanstack/poi/excel/prueba3.xlsx";
        Workbook wb = ExcelUtil.openWorkbook(new File(path));
        Sheet sheet = wb.getSheetAt(0);

        Map<String, String> headToField = new HashMap<>();
        headToField.put("code", "code");
        headToField.put("fullname", "fullName");
        headToField.put("description", "description");

        Row first = sheet.getRow(1);
        AppUser user = new AppUserRowProcessor(first, headToField, null).process();
        assertEquals("Administrador", user.getCode());
        assertEquals("Administrador", user.getFullName());
        wb.close();
    }

    // ----------------------------------------------------------------------
    // Especificación de columnas (ExcelColumns / ExcelColumnSpec)
    // ----------------------------------------------------------------------

    /**
     * Subclase que recibe la especificación completa de las columnas y usa el
     * {@code process()} base (obligatoriedad, valores por defecto y
     * transformaciones incluidas).
     */
    static class SpecProcessor extends ExcelRowProcessor<AppUser> {

        SpecProcessor(Row row, ExcelColumns columns, Map<String, Object> properties) {
            super(row, AppUser.class, columns, 0, properties);
        }
    }

    /**
     * Construye una planilla con los encabezados indicados en la fila 0 y una
     * única fila de datos con los valores recibidos (un {@code null} deja la
     * celda en blanco).
     */
    private Workbook buildWorkbook(String[] headers, Object[] values) {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("data");
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }
        Row data = sheet.createRow(1);
        for (int i = 0; i < values.length; i++) {
            Cell cell = data.createCell(i);
            Object value = values[i];
            if (value instanceof String) {
                cell.setCellValue((String) value);
            } else if (value instanceof Double) {
                cell.setCellValue((Double) value);
            } else if (value instanceof Boolean) {
                cell.setCellValue((Boolean) value);
            }
        }
        return wb;
    }

    @Test
    public void testCheckMetaDataFaltaColumnaObligatoria() throws Exception {
        try (Workbook wb = buildWorkbook(new String[]{"code", "fullName"},
                new Object[]{"jenciso", "Jorge Enciso"})) {
            ExcelColumns columns = new ExcelColumns();
            columns.add("code", "code").required();
            columns.add("Descripción", "description").required();
            columns.add("Fecha de expiración", "expiredDate").require(ColumnRequirement.VALUE);
            columns.add("idcompany", "idcompany");  // opcional ausente: no se reclama
            SpecProcessor p = new SpecProcessor(wb.getSheetAt(0).getRow(1), columns, null);

            String msg = p.checkMetaData();
            assertTrue(msg.contains("Falta la columna obligatoria «Descripción»"), msg);
            assertTrue(msg.contains("Falta la columna obligatoria «Fecha de expiración»"), msg);
            assertFalse(msg.contains("idcompany"), msg);
        }
    }

    @Test
    public void testCheckMetaDataConColumnasObligatoriasPresentes() throws Exception {
        try (Workbook wb = buildWorkbook(new String[]{"code", "fullName"},
                new Object[]{"jenciso", "Jorge Enciso"})) {
            ExcelColumns columns = new ExcelColumns();
            columns.add("code", "code").require(ColumnRequirement.VALUE);
            columns.add("fullName", "fullName").required();
            SpecProcessor p = new SpecProcessor(wb.getSheetAt(0).getRow(1), columns, null);

            assertEquals("", p.checkMetaData());
        }
    }

    @Test
    public void testValorPorDefectoEnColumnaAusente() throws Exception {
        try (Workbook wb = buildWorkbook(new String[]{"code"}, new Object[]{"jenciso"})) {
            ExcelColumns columns = new ExcelColumns();
            columns.add("code", "code");
            columns.add("idcompany", "idcompany").defaultValue("12");            // Long
            columns.add("prbNumerico", "prbNumerico").defaultValue("100.31");    // BigDecimal
            columns.add("expiredDate", "expiredDate").defaultValue(EXPIRED);     // LocalDateTime
            columns.add("disabled", "disabled").defaultValue("1");               // Boolean
            columns.add("description", "description").defaultValue("sin datos"); // String
            columns.add("fullName", "fullName");                                 // sin default -> null
            AppUser user = new SpecProcessor(wb.getSheetAt(0).getRow(1), columns, null).process();

            assertEquals("jenciso", user.getCode());
            assertEquals(Long.valueOf(12L), user.getIdcompany());
            assertEquals(0, new BigDecimal("100.31")
                    .compareTo((BigDecimal) user.getValue("prbNumerico")));
            assertEquals(EXPIRED, user.getExpiredDate());
            assertEquals(Boolean.TRUE, user.getDisabled());
            assertEquals("sin datos", user.getDescription());
            assertNull(user.getFullName());
            assertTrue(user.getErrors() == null || user.getErrors().isEmpty());
        }
    }

    /**
     * La fila anota qué atributos se completaron con el default de una columna
     * AUSENTE (y solo esos): es lo que usa la importación para no pisar con
     * ellos un registro existente (decisión del usuario, 2026-09-16).
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testRegistraLosAtributosCompletadosPorDefaultDeColumnaAusente() throws Exception {
        try (Workbook wb = buildWorkbook(new String[]{"code", "description"},
                new Object[]{"u1", ""})) {
            ExcelColumns columns = new ExcelColumns();
            columns.add("code", "code").defaultValue("x");                  //presente: no cuenta
            columns.add("description", "description").defaultValue("d").defaultWhenBlank(); //presente y en blanco: no cuenta
            columns.add("fullName", "fullName").defaultValue("Sin nombre");  //ausente: cuenta
            columns.add("email1", "email1");                                 //ausente sin default: no cuenta

            SpecProcessor p = new SpecProcessor(wb.getSheetAt(0).getRow(1), columns, null);
            AppUser fila = p.process();

            assertEquals("Sin nombre", fila.getFullName());
            assertEquals("d", fila.getDescription());
            java.util.Set<String> defaulted = (java.util.Set<String>) fila.getProperties()
                    .get(ExcelRowProcessor.DEFAULTED_FIELDS);
            assertNotNull(defaulted);
            assertEquals(java.util.Set.of("fullName"), defaulted);
        }
    }

    /**
     * Una columna {@code noOverwrite()} se asigna normalmente en la fila y queda
     * anotada en {@code NO_OVERWRITE_FIELDS}; por defecto {@code overwrite} es
     * {@code true} y no se anota nada.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testColumnaNoOverwriteSeAnotaEnLaFila() throws Exception {
        try (Workbook wb = buildWorkbook(new String[]{"code", "fullName"},
                new Object[]{"u1", "Nombre"})) {
            ExcelColumns columns = new ExcelColumns();
            columns.add("code", "code");
            columns.add("fullName", "fullName").noOverwrite();
            assertTrue(columns.byHeader("code").orElseThrow().isOverwrite());
            assertFalse(columns.byHeader("fullName").orElseThrow().isOverwrite());

            SpecProcessor p = new SpecProcessor(wb.getSheetAt(0).getRow(1), columns, null);
            AppUser fila = p.process();

            assertEquals("Nombre", fila.getFullName(), "el valor se asigna igual en la fila");
            java.util.Set<String> noOverwrite = (java.util.Set<String>) fila.getProperties()
                    .get(ExcelRowProcessor.NO_OVERWRITE_FIELDS);
            assertEquals(java.util.Set.of("fullName"), noOverwrite);
            assertNull(fila.getProperties().get(ExcelRowProcessor.DEFAULTED_FIELDS));
        }
    }

    @Test
    public void testValorPorDefectoConSupplierSeEvaluaPorFila() throws Exception {
        try (Workbook wb = buildWorkbook(new String[]{"code"}, new Object[]{"u1"})) {
            Sheet sheet = wb.getSheetAt(0);
            Row segunda = sheet.createRow(2);
            segunda.createCell(0).setCellValue("u2");

            AtomicInteger contador = new AtomicInteger();
            Supplier<Object> porFila = () -> "fila " + contador.incrementAndGet();
            ExcelColumns columns = new ExcelColumns();
            columns.add("code", "code");
            columns.add("description", "description").defaultValue(porFila);

            SpecProcessor p = new SpecProcessor(sheet.getRow(1), columns, null);
            AppUser primera = p.process();
            p.setRow(segunda);
            AppUser siguiente = p.process();

            assertEquals("fila 1", primera.getDescription());
            assertEquals("fila 2", siguiente.getDescription());
            assertEquals(2, contador.get());
        }
    }

    @Test
    public void testColumnaValueConCeldaVaciaMarcaErrorEnLaFila() throws Exception {
        try (Workbook wb = buildWorkbook(new String[]{"code", "Descripción"},
                new Object[]{"jenciso", null})) {
            ExcelColumns columns = new ExcelColumns();
            columns.add("code", "code");
            columns.add("Descripción", "description").require(ColumnRequirement.VALUE);
            AppUser user = new SpecProcessor(wb.getSheetAt(0).getRow(1), columns, null).process();

            assertNull(user.getDescription());
            Map<String, IErrorReg> errores = user.getErrors();
            assertNotNull(errores);
            IErrorReg error = errores.get("description");
            assertNotNull(error, String.valueOf(errores));
            assertEquals(Integer.valueOf(50000), error.getErrorNumber());
            assertTrue(error.getMessage().contains("«Descripción»"), error.getMessage());
            assertTrue(error.getMessage().contains("no puede estar vacía"), error.getMessage());
        }
    }

    @Test
    public void testColumnaValueConTextoEnBlancoMarcaErrorEnLaFila() throws Exception {
        try (Workbook wb = buildWorkbook(new String[]{"code", "Descripción"},
                new Object[]{"jenciso", "   "})) {
            ExcelColumns columns = new ExcelColumns();
            columns.add("Descripción", "description").require(ColumnRequirement.VALUE);
            AppUser user = new SpecProcessor(wb.getSheetAt(0).getRow(1), columns, null).process();

            IErrorReg error = user.getErrors().get("description");
            assertNotNull(error);
            assertEquals(Integer.valueOf(50000), error.getErrorNumber());
        }
    }

    @Test
    public void testDefaultWhenBlank() throws Exception {
        try (Workbook wb = buildWorkbook(new String[]{"code", "description"},
                new Object[]{"jenciso", null})) {
            Row data = wb.getSheetAt(0).getRow(1);

            // apagado (comportamiento por defecto): la celda vacía se asigna como null
            ExcelColumns apagado = new ExcelColumns();
            apagado.add("code", "code");
            apagado.add("description", "description").defaultValue("sin datos");
            assertNull(new SpecProcessor(data, apagado, null).process().getDescription());

            // encendido: la celda vacía toma el valor por defecto
            ExcelColumns encendido = new ExcelColumns();
            encendido.add("code", "code");
            encendido.add("description", "description").defaultValue("sin datos").defaultWhenBlank();
            assertEquals("sin datos",
                    new SpecProcessor(data, encendido, null).process().getDescription());
        }
    }

    @Test
    public void testConverterSeAplicaAlValorDeLaCelda() throws Exception {
        try (Workbook wb = buildWorkbook(new String[]{"code", "description"},
                new Object[]{" jenciso ", null})) {
            ExcelColumns columns = new ExcelColumns();
            columns.add("code", "code").converter(value -> value.toString().trim().toUpperCase());
            // el converter no se aplica al valor por defecto de una celda vacía
            columns.add("description", "description")
                    .defaultValue("sin datos")
                    .defaultWhenBlank()
                    .converter(value -> "NO DEBE APLICARSE");
            AppUser user = new SpecProcessor(wb.getSheetAt(0).getRow(1), columns, null).process();

            assertEquals("JENCISO", user.getCode());
            assertEquals("sin datos", user.getDescription());
        }
    }

    @Test
    public void testConverterQueFallaMarcaErrorEnLaFila() throws Exception {
        try (Workbook wb = buildWorkbook(new String[]{"code"}, new Object[]{"jenciso"})) {
            ExcelColumns columns = new ExcelColumns();
            columns.add("code", "code").converter(value -> {
                throw new IllegalStateException("código inválido");
            });
            AppUser user = new SpecProcessor(wb.getSheetAt(0).getRow(1), columns, null).process();

            assertNull(user.getCode());
            IErrorReg error = user.getErrors().get("code");
            assertNotNull(error, String.valueOf(user.getErrors()));
            assertEquals(Integer.valueOf(50000), error.getErrorNumber());
            assertTrue(error.getMessage().contains("«code»"), error.getMessage());
            assertTrue(error.getMessage().contains("código inválido"), error.getMessage());
        }
    }

    @Test
    public void testOrdenDeDeclaracionSeConserva() throws Exception {
        ExcelColumns columns = new ExcelColumns();
        columns.add("zeta", "code");
        columns.add("alfa", "fullName").required();
        columns.add("media", "description").defaultValue("x");

        assertEquals(Arrays.asList("zeta", "alfa", "media"),
                new ArrayList<>(columns.toHeadToField().keySet()));
        assertEquals(Arrays.asList("code", "fullName", "description"),
                new ArrayList<>(columns.toFieldToHead().keySet()));
        assertEquals(3, columns.size());
        assertFalse(columns.isEmpty());
        // el builder reemplaza la especificación en su lugar, sin mover la posición
        assertEquals(ColumnRequirement.COLUMN, columns.byHeader("alfa").get().getRequirement());
        assertEquals(ColumnRequirement.OPTIONAL, columns.byHeader("zeta").get().getRequirement());
        assertTrue(columns.byField("description").get().hasDefaultValue());
        assertFalse(columns.byHeader("noExiste").isPresent());

        try (Workbook wb = buildWorkbook(new String[]{"zeta", "alfa", "media"},
                new Object[]{"jenciso", "Jorge Enciso", "Analista"})) {
            SpecProcessor p = new SpecProcessor(wb.getSheetAt(0).getRow(1), columns, null);
            assertEquals(Arrays.asList("zeta", "alfa", "media"),
                    new ArrayList<>(p.getHeadToField().keySet()));
            assertEquals(3, p.getColumns().size());
        }
    }

    @Test
    public void testExcelColumnsOfEquivaleAlConstructorConMapa() throws Exception {
        try (Workbook wb = buildAppUserWorkbook()) {
            Row data = wb.getSheetAt(0).getRow(1);
            Map<String, String> headToField = new HashMap<>();
            headToField.put("code", "code");
            headToField.put("idcompany", "idcompany");

            AppUser conMapa = new DefaultProcessor(data, headToField, null).process();
            SpecProcessor conSpec = new SpecProcessor(data, ExcelColumns.of(headToField), null);
            AppUser conColumns = conSpec.process();

            assertEquals(conMapa.getCode(), conColumns.getCode());
            assertEquals(conMapa.getIdcompany(), conColumns.getIdcompany());
            assertEquals(headToField, conSpec.getHeadToField());
            assertEquals(ColumnRequirement.OPTIONAL,
                    conSpec.getColumns().byHeader("code").get().getRequirement());
            assertFalse(conSpec.getColumns().byHeader("code").get().hasDefaultValue());
            // sin mapeo declarado, el procesador genera columnas identidad opcionales
            DefaultProcessor identidad = new DefaultProcessor(data, null, null);
            assertEquals(HEADERS.length, identidad.getColumns().size());
            assertEquals("code", identidad.getColumns().byHeader("code").get().getField());
        }
    }

    @Test
    public void testCabeceraDuplicadaEsRechazada() {
        ExcelColumns columns = new ExcelColumns();
        columns.add("code", "code");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> columns.add("code", "fullName"));
        assertTrue(error.getMessage().contains("code"), error.getMessage());
        assertTrue(error.getMessage().contains("ya fue declarada"), error.getMessage());
        assertThrows(IllegalArgumentException.class, () -> columns.add("   ", "fullName"));
        assertEquals(1, columns.size());
        assertEquals(0, ExcelColumns.of(null).size());
    }

    @Test
    public void testDefaultValueConSupplierTipadoNoQuedaComoValorFijo() throws Exception {
        // E1-02: `Supplier<String>` no es subtipo de `Supplier<Object>`; con la
        // firma `Supplier<?>` liga con la sobrecarga del proveedor, y si entra
        // por la de `Object` la red lo deriva igual.
        try (Workbook wb = buildWorkbook(new String[]{"code"}, new Object[]{"jenciso"})) {
            Row data = wb.getSheetAt(0).getRow(1);
            Supplier<String> proveedor = () -> "x";

            ExcelColumns columns = new ExcelColumns();
            columns.add("code", "code");
            columns.add("description", "description").defaultValue(proveedor);
            ExcelColumnSpec spec = columns.byHeader("description").get();
            assertFalse(spec.isDefaultValueFixed());
            assertEquals("x", spec.getDefaultValue().get());
            assertEquals("x", new SpecProcessor(data, columns, null).process().getDescription());

            ExcelColumns comoObjeto = new ExcelColumns();
            comoObjeto.add("code", "code");
            comoObjeto.add("description", "description").defaultValue((Object) proveedor);
            ExcelColumnSpec derivada = comoObjeto.byHeader("description").get();
            assertFalse(derivada.isDefaultValueFixed());
            assertEquals("x", derivada.getDefaultValue().get());
            assertEquals("x", new SpecProcessor(data, comoObjeto, null).process().getDescription());

            ExcelColumns fijo = new ExcelColumns();
            fijo.add("code", "code");
            fijo.add("description", "description").defaultValue("01");
            assertTrue(fijo.byHeader("description").get().isDefaultValueFixed());
            assertEquals("01", new SpecProcessor(data, fijo, null).process().getDescription());
        }
    }

    @Test
    public void testBuilderRechazaCabeceraNulaOEnBlanco() {
        // I1-04: la misma validación que `ExcelColumns.add`, en un solo lugar.
        assertThrows(IllegalArgumentException.class, () -> ExcelColumnSpec.builder(null, "code"));
        assertThrows(IllegalArgumentException.class, () -> ExcelColumnSpec.builder("   ", "code"));
        ExcelColumnSpec spec = ExcelColumnSpec.builder(" code ", "code").required().build();
        assertEquals("code", spec.getHeader());
        assertEquals(ColumnRequirement.COLUMN, spec.getRequirement());
        // la identidad es la cabecera normalizada
        assertEquals(spec, ExcelColumnSpec.builder("code", "otroAtributo").build());
        assertEquals("code".hashCode(), spec.hashCode());
    }

    @Test
    public void testCabeceraDeclaradaSeNormalizaConTrim() throws Exception {
        // M1-02: el índice de la planilla trimea; la cabecera declarada también.
        ExcelColumns columns = new ExcelColumns();
        columns.add("  code  ", "code");
        columns.add("Descripción", "description").required();
        assertEquals("code", columns.byHeader("code").get().getHeader());
        assertEquals("code", columns.byHeader("  code ").get().getHeader());
        assertThrows(IllegalArgumentException.class, () -> columns.add("code ", "fullName"));
        assertEquals(Arrays.asList("code", "Descripción"),
                new ArrayList<>(columns.toHeadToField().keySet()));

        try (Workbook wb = buildWorkbook(new String[]{"code", "Descripción"},
                new Object[]{"jenciso", "Analista"})) {
            SpecProcessor p = new SpecProcessor(wb.getSheetAt(0).getRow(1), columns, null);
            assertEquals("", p.checkMetaData());
            AppUser user = p.process();
            assertEquals("jenciso", user.getCode());
            assertEquals("Analista", user.getDescription());
        }
    }

    @Test
    public void testColumnsSeCopiaAlConstruirElProcesador() throws Exception {
        // I1-05: un `add` posterior a la construcción no puede desincronizar
        // los mapas derivados del procesador.
        try (Workbook wb = buildWorkbook(new String[]{"code", "description"},
                new Object[]{"jenciso", "Analista"})) {
            ExcelColumns columns = new ExcelColumns();
            columns.add("code", "code");
            SpecProcessor p = new SpecProcessor(wb.getSheetAt(0).getRow(1), columns, null);

            columns.add("description", "description").defaultValue("agregada después");

            assertEquals(2, columns.size());
            assertEquals(1, p.getColumns().size());
            assertEquals(1, p.getHeadToField().size());
            assertFalse(p.getColumns().byHeader("description").isPresent());
            AppUser user = p.process();
            assertEquals("jenciso", user.getCode());
            assertNull(user.getDescription());
        }
    }

    @Test
    public void testCheckMetaDataReclamaDefaultNoConvertible() throws Exception {
        // M1-03: el default fijo que no se puede convertir se avisa antes de
        // leer filas; si la columna está en la planilla no se aplica y no se
        // reclama.
        try (Workbook wb = buildWorkbook(new String[]{"code", "type"},
                new Object[]{"jenciso", "3"})) {
            ExcelColumns columns = new ExcelColumns();
            columns.add("code", "code");
            columns.add("idcompany", "idcompany").defaultValue("abc");
            columns.add("type", "type").defaultValue("tampoco");
            SpecProcessor p = new SpecProcessor(wb.getSheetAt(0).getRow(1), columns, null);

            String msg = p.checkMetaData();
            assertTrue(msg.contains("El valor por defecto de la columna «idcompany» no es convertible a Long"),
                    msg);
            assertFalse(msg.contains("«type»"), msg);
        }
    }

    @Test
    public void testDefaultQueFallaMarcaLaFilaYNoAbortaLaPasada() throws Exception {
        // M1-03: ni el proveedor que lanza ni el default no convertible cortan
        // la importación; quedan como error 50000 de la fila.
        try (Workbook wb = buildWorkbook(new String[]{"code"}, new Object[]{"jenciso"})) {
            ExcelColumns columns = new ExcelColumns();
            columns.add("code", "code");
            columns.add("idcompany", "idcompany").defaultValue("abc");
            columns.add("description", "description").defaultValue(() -> {
                throw new IllegalStateException("sin sesión");
            });
            AppUser user = new SpecProcessor(wb.getSheetAt(0).getRow(1), columns, null).process();

            assertEquals("jenciso", user.getCode());
            assertNull(user.getIdcompany());
            assertNull(user.getDescription());
            Map<String, IErrorReg> errores = user.getErrors();
            assertNotNull(errores);
            IErrorReg deLaConversion = errores.get("idcompany");
            assertNotNull(deLaConversion, String.valueOf(errores));
            assertEquals(Integer.valueOf(50000), deLaConversion.getErrorNumber());
            assertTrue(deLaConversion.getMessage().contains("«idcompany»"), deLaConversion.getMessage());
            assertTrue(deLaConversion.getMessage().contains("no se pudo aplicar"), deLaConversion.getMessage());
            IErrorReg delProveedor = errores.get("description");
            assertNotNull(delProveedor, String.valueOf(errores));
            assertEquals(Integer.valueOf(50000), delProveedor.getErrorNumber());
            assertTrue(delProveedor.getMessage().contains("sin sesión"), delProveedor.getMessage());
        }
    }

    @Test
    public void testDefaultWhenBlankPrevaleceSobreValue() throws Exception {
        // M1-04: la columna VALUE con `defaultWhenBlank` no marca error: el
        // valor por defecto es la declaración explícita de qué poner.
        try (Workbook wb = buildWorkbook(new String[]{"code", "Descripción"},
                new Object[]{"jenciso", null})) {
            ExcelColumns columns = new ExcelColumns();
            columns.add("code", "code");
            columns.add("Descripción", "description")
                    .require(ColumnRequirement.VALUE)
                    .defaultValue("sin dato")
                    .defaultWhenBlank();
            SpecProcessor p = new SpecProcessor(wb.getSheetAt(0).getRow(1), columns, null);

            assertEquals("", p.checkMetaData());
            AppUser user = p.process();
            assertEquals("sin dato", user.getDescription());
            assertTrue(user.getErrors() == null || user.getErrors().isEmpty(),
                    String.valueOf(user.getErrors()));
        }
    }
}

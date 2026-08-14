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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
}

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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.model.IAppUserMember;
import org.javabeanstack.security.model.IUserSession;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de {@link ExcelImportSrv}.
 * <p>
 * Se construyen planillas en memoria con POI y se ejercita el flujo de
 * conversión ({@link ExcelImportSrv#getDataFromExcelSheet(Sheet)}) y las
 * validaciones de {@link ExcelImportSrv#importData()}, sin contenedor CDI
 * (por eso {@code appConfig} queda nulo, escenario que el servicio tolera).
 *
 * @author Jorge Enciso
 */
public class ExcelImportSrvTest {

    /**
     * Implementación concreta mínima que expone el método protegido
     * {@link ExcelImportSrv#getDataFromExcelSheet(Sheet)} para poder probarlo.
     */
    static class TestImportSrv extends ExcelImportSrvTest01 {

        List<AppUser> readSheet(Sheet sheet) throws Exception {
            return getDataFromExcelSheet(sheet);
        }
    }

    /**
     * Procesador concreto por defecto: instancia el destino y asigna vía
     * {@code setValue} (no requiere lógica adicional).
     */
    private IExcelRowProcessor<AppUser> processor(Row row) {
        return new ExcelRowProcessor<AppUser>(row, AppUser.class, null, null) {
        };
    }

    /**
     * Planilla con encabezados que coinciden con atributos de {@link AppUser}
     * y dos filas de datos.
     */
    private Workbook buildWorkbook() {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("data");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("code");
        header.createCell(1).setCellValue("fullName");
        Row r1 = sheet.createRow(1);
        r1.createCell(0).setCellValue("u1");
        r1.createCell(1).setCellValue("User One");
        Row r2 = sheet.createRow(2);
        r2.createCell(0).setCellValue("u2");
        r2.createCell(1).setCellValue("User Two");
        return wb;
    }

    @Test
    public void testGetDataFromExcelSheetConverts() throws Exception {
        TestImportSrv srv = new TestImportSrv();
        try (Workbook wb = buildWorkbook()) {
            Sheet sheet = wb.getSheetAt(0);
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(sheet.getRow(0)));
            // Solo se ejercita la conversión: sin revisión previa la lectura
            // no valida la lógica de negocios (no requiere sesión ni servicio).
            srv.setCheckBeforeErrors(false);

            List<AppUser> data = srv.readSheet(null);

            assertEquals(2, data.size());
            assertEquals("u1", data.get(0).getCode());
            assertEquals("User One", data.get(0).getFullName());
            assertEquals("u2", data.get(1).getCode());
            assertTrue(srv.getDataRowsError().isEmpty());
        }
    }

    // ---- Hoja a procesar y filas sobre la cabecera (plan XLSSEQ de Maker, Fase 2) ----------

    /**
     * Procesador con la fila de encabezados en la posición indicada (base 0).
     */
    private IExcelRowProcessor<AppUser> processor(Row row, int headerRowIndex) {
        return new ExcelRowProcessor<AppUser>(row, AppUser.class, (Map<String, String>) null,
                headerRowIndex, null, null, null) {
        };
    }

    /**
     * Libro con tres hojas: «Portada» (sin datos), «Usuarios» (2 títulos, la
     * cabecera en la fila 3 de Excel y dos filas de datos) y «Otros» (una
     * fila de datos).
     */
    private Workbook buildWorkbookMultiHoja() {
        Workbook wb = new XSSFWorkbook();
        wb.createSheet("Portada").createRow(0).createCell(0).setCellValue("Planilla de prueba");
        Sheet usuarios = wb.createSheet("Usuarios");
        usuarios.createRow(0).createCell(0).setCellValue("code");      // título que parece dato
        usuarios.createRow(1).createCell(0).setCellValue("Listado");
        Row header = usuarios.createRow(2);
        header.createCell(0).setCellValue("code");
        header.createCell(1).setCellValue("fullName");
        Row r1 = usuarios.createRow(3);
        r1.createCell(0).setCellValue("u1");
        r1.createCell(1).setCellValue("User One");
        Row r2 = usuarios.createRow(4);
        r2.createCell(0).setCellValue("u2");
        r2.createCell(1).setCellValue("User Two");
        Sheet otros = wb.createSheet("Otros");
        Row h = otros.createRow(0);
        h.createCell(0).setCellValue("code");
        h.createCell(1).setCellValue("fullName");
        Row o1 = otros.createRow(1);
        o1.createCell(0).setCellValue("o1");
        o1.createCell(1).setCellValue("Other");
        return wb;
    }

    @Test
    public void testResolveSheetPorNombrePosicionYPrimera() throws Exception {
        TestImportSrv srv = new TestImportSrv();
        try (Workbook wb = buildWorkbookMultiHoja()) {
            srv.setExcelWorkbook(wb);
            assertEquals("Portada", srv.resolveSheet().getSheetName(), "sin indicar nada, la primera");
            srv.setSheetIndex(2);
            assertEquals("Otros", srv.resolveSheet().getSheetName(), "posición base 0");
            srv.setSheetName("Usuarios");
            assertEquals("Usuarios", srv.resolveSheet().getSheetName(), "el nombre gana a la posición");
            assertEquals(List.of("Portada", "Usuarios", "Otros"), ExcelImportSrv.getSheetNames(wb));
        }
    }

    @Test
    public void testResolveSheetInexistenteNombraLasHojas() throws Exception {
        TestImportSrv srv = new TestImportSrv();
        try (Workbook wb = buildWorkbookMultiHoja()) {
            srv.setExcelWorkbook(wb);
            srv.setSheetName("Monedas");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, srv::resolveSheet);
            assertEquals("La planilla no tiene la hoja «Monedas». Hojas: Portada, Usuarios, Otros", ex.getMessage());
            srv.setSheetName(null);
            srv.setSheetIndex(3);
            ex = assertThrows(IllegalArgumentException.class, srv::resolveSheet);
            assertEquals("La planilla tiene 3 hoja(s); se pidió la 4. Hojas: Portada, Usuarios, Otros", ex.getMessage());
        }
        TestImportSrv sinLibro = new TestImportSrv();
        assertThrows(IllegalStateException.class, sinLibro::resolveSheet);
    }

    @Test
    public void testFindSheetSinDistinguirMayusculasEInvalidateReview() throws Exception {
        try (Workbook wb = buildWorkbookMultiHoja()) {
            assertEquals("Usuarios", ExcelImportSrv.findSheet(wb, "usuarios", null).getSheetName());
            assertEquals("Otros", ExcelImportSrv.findSheet(wb, " ", 2).getSheetName());
        }
        TestImportSrv srv = new TestImportSrv();
        srv.getDataRows().add(new AppUser());
        srv.invalidateReview();
        assertFalse(srv.getErrorsReviewed());
        assertTrue(srv.getDataRows().isEmpty());
    }

    @Test
    public void testLecturaUsaLaHojaResueltaYSaltaLasFilasSobreLaCabecera() throws Exception {
        TestImportSrv srv = new TestImportSrv();
        try (Workbook wb = buildWorkbookMultiHoja()) {
            srv.setExcelWorkbook(wb);
            srv.setSheetName("Usuarios");
            srv.setExcelRowProcessor(processor(wb.getSheet("Usuarios").getRow(2), 2));
            srv.setCheckBeforeErrors(false);

            List<AppUser> data = srv.readSheet(null);

            //E0-01: las dos filas de arriba de la cabecera no se leen como datos.
            assertEquals(2, data.size());
            assertEquals(2, srv.getRowsReadedCount());
            assertEquals("u1", data.get(0).getCode());
            assertEquals("u2", data.get(1).getCode());
            assertTrue(srv.getDataRowsError().isEmpty());
        }
    }

    @Test
    public void testGetDataFromExcelSheetWithoutProcessorThrows() {
        TestImportSrv srv = new TestImportSrv();
        Exception ex = assertThrows(Exception.class, () -> srv.readSheet(null));
        assertTrue(ex.getMessage().contains("processor"), ex.getMessage());
    }

    @Test
    public void testGetDataFromExcelSheetWithoutWorkbookThrows() throws Exception {
        TestImportSrv srv = new TestImportSrv();
        try (Workbook wb = buildWorkbook()) {
            // processor definido, pero no se asigna el workbook ni se pasa hoja
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            Exception ex = assertThrows(Exception.class, () -> srv.readSheet(null));
            assertTrue(ex.getMessage().contains("planilla"), ex.getMessage());
        }
    }

    @Test
    public void testGetDataFromExcelSheetInvalidMetaDataThrows() throws Exception {
        TestImportSrv srv = new TestImportSrv();
        try (Workbook wb = buildWorkbook()) {
            Sheet sheet = wb.getSheetAt(0);
            // encabezado duplicado -> checkMetaData debe reportar error
            sheet.getRow(0).getCell(1).setCellValue("code");
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(sheet.getRow(0)));

            Exception ex = assertThrows(Exception.class, () -> srv.readSheet(null));
            assertTrue(ex.getMessage().contains("duplicado"), ex.getMessage());
        }
    }

    @Test
    public void testCheckBeforeImportDataRequiresProcessor() {
        // importData() captura las excepciones internamente (ver su javadoc);
        // la validación de prerrequisitos vive en checkBeforeImportData().
        TestImportSrv srv = new TestImportSrv();
        Exception ex = assertThrows(Exception.class, srv::checkBeforeImportData);
        assertTrue(ex.getMessage().contains("processor"), ex.getMessage());
    }

    @Test
    public void testCheckBeforeImportDataRequiresWorkbook() throws Exception {
        TestImportSrv srv = new TestImportSrv();
        try (Workbook wb = buildWorkbook()) {
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            // processor definido, pero sin workbook asignado
            Exception ex = assertThrows(Exception.class, srv::checkBeforeImportData);
            assertTrue(ex.getMessage().contains("planilla"), ex.getMessage());
        }
    }

    /**
     * Verifica el indicador de fase {@link ExcelImportSrv#getErrorsReviewed()}
     * que consumen los hooks: durante la pasada de revisión
     * ({@link ExcelImportSrv#checkValidation(Sheet)}) debe verse en
     * {@code false}; durante la grabación de
     * {@link ExcelImportSrv#importData()} en {@code true}; y al finalizar el
     * proceso debe quedar reseteado (una próxima corrida relee la planilla).
     */
    @Test
    public void testPhaseFlagInHooksOnValidationAndImport() throws Exception {
        PhaseRecordingSrv srv = new PhaseRecordingSrv();
        try (Workbook wb = buildWorkbook()) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));

            //Pasada de revisión: los hooks ven la fase en false
            srv.checkValidation(null);
            assertEquals("", srv.getErrorMessage());
            assertEquals(List.of(false, false), srv.hookPhases);
            assertTrue(srv.getErrorsReviewed());

            //Importación: los hooks ven la fase en true y se graban las filas
            srv.hookPhases.clear();
            srv.importData();
            assertEquals(List.of(true, true), srv.hookPhases);
            assertTrue(srv.getImportOk());
            assertEquals(2, srv.getRowsMigratedCount());
            //Proceso terminado: el indicador queda consumido
            assertFalse(srv.getErrorsReviewed());
        }
    }

    /**
     * Servicio simulado que declara <b>existente</b> la fila cuyo {@code code}
     * es {@code u1} (devuelve una entidad ya persistida con id 100 y datos
     * previos), y registra las entidades que recibe {@code save}.
     */
    static class ExistingRowSrv extends ExcelImportSrvTest01 {

        final List<IDataRow> updated = new ArrayList<>();
        /** Entidades que recibe {@code checkAndSave} (las bajas). */
        final List<IDataRow> deleted = new ArrayList<>();
        /** Respuesta del gancho de autorización de bajas. */
        boolean deleteAuthorized = true;
        /** Errores que devuelve {@code checkDataRow} para una baja (vacío = válida). */
        Map<String, org.javabeanstack.error.IErrorInfo> deleteCheckErrors = new HashMap<>();
        /** Si {@code checkAndSave} rechaza la baja. */
        boolean deleteFails = false;
        /** Errores que devuelve {@code checkDataRow} para una modificación (vacío = válida). */
        Map<String, org.javabeanstack.error.IErrorInfo> updateCheckErrors = new HashMap<>();
        /** Filas que recibió {@code checkDataRow} como modificación. */
        int updateChecks = 0;
        /** Empresa del existente {@code u1} (null = sin empresa). */
        Long companyOfU1;
        /** Resultado que devuelve {@code save} (null = éxito). */
        org.javabeanstack.data.IDataResult saveResult;
        int lookups = 0;

        @Override
        protected boolean checkDeleteAuthorization() {
            return deleteAuthorized;
        }
        /** Miembro que tiene cargado el existente {@code u1} (null = ninguno). */
        IAppUserMember memberOfU1;

        @Override
        protected IDataService getDataService() {
            InvocationHandler handler = new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    switch (method.getName()) {
                        case "copyTo":
                            //A diferencia del stub genérico, copia de verdad: la
                            //búsqueda por clave única necesita el `code` convertido.
                            try {
                                return ((IDataRow) args[1]).copyTo((IDataRow) args[2]);
                            } catch (Exception e) {
                                throw new IllegalStateException(e);
                            }
                        case "checkDataRow":
                            if (((IDataRow) args[1]).getAction() == IDataRow.DELETE) {
                                return new HashMap<>(deleteCheckErrors);
                            }
                            if (((IDataRow) args[1]).getAction() == IDataRow.UPDATE) {
                                updateChecks++;
                                return new HashMap<>(updateCheckErrors);
                            }
                            return new HashMap<>();
                        case "findById":
                            lookups++;
                            if (Long.valueOf(7L).equals(args[2])) {
                                AppUser byId = new AppUser();
                                byId.setIduser(7L);
                                byId.setCode("u7");
                                byId.setFullName("Nombre previo por id");
                                return byId;
                            }
                            return null;
                        case "findByUk":
                            lookups++;
                            AppUser probe = (AppUser) args[1];
                            if ("boom".equals(probe.getCode())) {
                                throw new IllegalStateException("sin clave única evaluable");
                            }
                            if ("u1".equals(probe.getCode())) {
                                AppUser existing = new AppUser();
                                existing.setIduser(100L);
                                existing.setCode("u1");
                                existing.setFullName("Nombre previo");
                                existing.setEmail1("previo@dominio.com");
                                existing.setIdcompany(companyOfU1);
                                if (memberOfU1 != null) {
                                    existing.getUserMemberList().add(memberOfU1);
                                }
                                return existing;
                            }
                            return null;
                        case "save":
                            if (saveResult != null) {
                                return saveResult;
                            }
                            updated.add((IDataRow) args[1]);
                            return stub(org.javabeanstack.data.IDataResult.class);
                        case "checkAndSave":
                            if (deleteFails) {
                                return failedResult("La tabla tiene registros relacionados");
                            }
                            deleted.add((IDataRow) args[1]);
                            return stub(org.javabeanstack.data.IDataResult.class);
                        default:
                            break;
                    }
                    Class<?> type = method.getReturnType();
                    if (type.isInterface()) {
                        return stub(type);
                    }
                    if (type == boolean.class || type == Boolean.class) {
                        return false;
                    }
                    return null;
                }
            };
            return (IDataService) Proxy.newProxyInstance(IDataService.class.getClassLoader(),
                    new Class[]{IDataService.class}, handler);
        }

        @Override
        protected IUserSession getUserSession() {
            return stub(IUserSession.class);
        }

        @Override
        protected Class<? extends IDataRow> getTargetType() {
            return AppUser.class;
        }
    }

    // ---- Operaciones permitidas y bajas por marca (plan XLSOPS de Maker) -------------

    /** Resultado de grabación fallido con un error genérico. */
    private static org.javabeanstack.data.IDataResult failedResult(String message) {
        return (org.javabeanstack.data.IDataResult) Proxy.newProxyInstance(
                org.javabeanstack.data.IDataResult.class.getClassLoader(),
                new Class[]{org.javabeanstack.data.IDataResult.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "isSuccessFul":
                            return false;
                        case "getErrorsMap":
                            Map<String, org.javabeanstack.error.IErrorInfo> errors = new HashMap<>();
                            errors.put("datageneric", new org.javabeanstack.error.ErrorInfo(message, 50000, "datageneric"));
                            return errors;
                        default:
                            Class<?> type = method.getReturnType();
                            if (type == boolean.class || type == Boolean.class) {
                                return false;
                            }
                            return null;
                    }
                });
    }

    /**
     * Planilla para las bajas con valores a medida: cabeceras y filas tal cual.
     */
    private Workbook buildWorkbook(String[] cabeceras, String[]... filas) {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("data");
        Row header = sheet.createRow(0);
        for (int c = 0; c < cabeceras.length; c++) {
            if (cabeceras[c] != null) {
                header.createCell(c).setCellValue(cabeceras[c]);
            }
        }
        for (int i = 0; i < filas.length; i++) {
            Row row = sheet.createRow(i + 1);
            for (int c = 0; c < filas[i].length; c++) {
                row.createCell(c).setCellValue(filas[i][c]);
            }
        }
        return wb;
    }

    /**
     * Planilla con la columna de marca de baja «accion»: u1 marcada con «b»
     * (sin distinguir mayúsculas), u2 sin marca, u9 marcada y sin existir.
     */
    private Workbook buildWorkbookConBajas(boolean conColumna, boolean conInexistente) {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("data");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("code");
        header.createCell(1).setCellValue("fullName");
        if (conColumna) {
            header.createCell(2).setCellValue("Accion");
        }
        String[][] filas = conInexistente
                ? new String[][]{{"u1", "User One", "b"}, {"u2", "User Two", ""}, {"u9", "Nadie", "B"}}
                : new String[][]{{"u1", "User One", "b"}, {"u2", "User Two", ""}};
        for (int i = 0; i < filas.length; i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(filas[i][0]);
            row.createCell(1).setCellValue(filas[i][1]);
            if (conColumna) {
                row.createCell(2).setCellValue(filas[i][2]);
            }
        }
        return wb;
    }

    @Test
    public void testAllowUpdateFalseNuncaSobrescribe() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        try (Workbook wb = buildWorkbook()) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.setAllowUpdate(false);
            srv.setOverWriteData(true);
            assertFalse(srv.getOverWriteData(), "sin modificaciones, sobrescribir queda apagado");
            assertTrue(srv.isOverWriteLocked());

            srv.checkValidation(null);
            srv.importData();
            assertEquals(1, srv.getRowsExistCount(), "u1 existe y no se toca");
            assertEquals(1, srv.getRowsMigratedCount());
            assertEquals(IDataRow.INSERT, srv.updated.get(0).getAction());
        }
    }

    /**
     * E5-05 del plan CTBPRC de Maker: sin «sobreescribir», la revisión previa no
     * valida el registro existente (las reglas de modificación lo rechazarían
     * aunque no se va a grabar) y la grabación lo cuenta como «ya existe».
     */
    @Test
    public void testRevisionPreviaNoValidaElExistenteSinSobrescribir() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        srv.updateCheckErrors.put("fecha",
                new org.javabeanstack.error.ErrorInfo("Tiene asiento de ajuste", 50124, "fecha"));
        try (Workbook wb = buildWorkbook()) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.setAllowUpdate(false);

            srv.checkValidation(null);
            assertEquals(0, srv.updateChecks, "el existente no se valida como modificación");
            assertTrue(srv.getDataRowsError().isEmpty(), "la revisión no rechaza el existente");
            srv.importData();
            assertEquals(1, srv.getRowsExistCount(), "u1 existe y no se toca");
            assertEquals(1, srv.getRowsMigratedCount());
            assertTrue(srv.getDataRowsError().isEmpty());
        }
    }

    /**
     * M2-26 del plan MKSIF de Maker: en la revisión previa, el salto del
     * existente sin «sobreescribir» va después de {@code onAfterRowConvert}; el
     * gancho sigue corriendo para esa fila (una subclase puede marcarla, como
     * la marca DUP de la importación Marangatú).
     */
    @Test
    public void testRevisionPreviaCorreElGanchoDelExistenteSinSobrescribir() throws Exception {
        List<IDataRow> marcados = new ArrayList<>();
        ExistingRowSrv srv = new ExistingRowSrv() {
            @Override
            protected boolean onAfterRowConvert(AppUser rowSource, IDataRow rowTarget) {
                if (rowTarget.getAction() == IDataRow.UPDATE) {
                    rowSource.getProperties().put("MARCA_EXISTENTE", Boolean.TRUE);
                    marcados.add(rowSource);
                }
                return true;
            }
        };
        try (Workbook wb = buildWorkbook()) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.setAllowUpdate(false);

            srv.checkValidation(null);
            assertEquals(1, marcados.size(), "el gancho corrió para el existente u1");
            assertTrue(srv.getDataRows().stream()
                    .anyMatch(r -> Boolean.TRUE.equals(r.getProperties().get("MARCA_EXISTENTE"))),
                    "la fila marcada queda entre las de la revisión");
            assertEquals(0, srv.updateChecks);
        }
    }

    /**
     * E5-06 del plan CTBPRC de Maker: revisar con «sobreescribir» apagado y
     * encenderlo antes de importar descarta la revisión; la importación vuelve
     * a validar y el existente no se graba como modificación sin validar.
     */
    @Test
    public void testEncenderSobrescribirDespuesDeRevisarRevalida() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        srv.updateCheckErrors.put("fecha",
                new org.javabeanstack.error.ErrorInfo("Tiene asiento de ajuste", 50124, "fecha"));
        try (Workbook wb = buildWorkbook()) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));

            srv.checkValidation(null);
            assertTrue(srv.getErrorsReviewed(), "revisada con sobrescribir apagado");
            srv.setOverWriteData(true);
            assertFalse(srv.getErrorsReviewed(), "cambiar sobrescribir descarta la revisión");
            srv.importData();
            assertEquals(1, srv.updateChecks, "la importación validó el existente");
            assertTrue(srv.updated.stream().noneMatch(r -> r.getAction() == IDataRow.UPDATE),
                    "el existente rechazado no se grabó");
        }
    }

    /**
     * E5-07 del plan CTBPRC de Maker: una subclase que sobrescribe
     * {@code getOverWriteData()} por fila (en Maker, la importación del libro
     * IVA con la última fila de gestión, que devuelve {@code false}) no oculta
     * el cambio de la opción: encenderla después de revisar descarta la revisión.
     */
    @Test
    public void testSobrescribirConGetterSobrescritoDescartaLaRevision() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv() {
            @Override
            public Boolean getOverWriteData() {
                return false;
            }
        };
        try (Workbook wb = buildWorkbook()) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.checkValidation(null);
            assertTrue(srv.getErrorsReviewed());
            srv.setOverWriteData(true);
            assertFalse(srv.getErrorsReviewed(), "el getter sobrescrito no oculta el cambio");
        }
    }

    /**
     * La grabación sigue usando {@code getOverWriteData()} sobrescribible: con
     * el getter en {@code false} el existente no se sobrescribe aunque el valor
     * efectivo de la base sea {@code true} (en Maker, la protección por fila de
     * los comprobantes de gestión del libro IVA).
     */
    @Test
    public void testGetterSobrescritoEnFalseNoSobrescribe() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv() {
            @Override
            public Boolean getOverWriteData() {
                return false;
            }
        };
        try (Workbook wb = buildWorkbook()) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.setOverWriteData(true);
            srv.checkValidation(null);
            srv.importData();
            assertEquals(1, srv.getRowsExistCount(), "u1 no se sobrescribe");
            assertTrue(srv.updated.stream().noneMatch(r -> r.getAction() == IDataRow.UPDATE));
        }
    }

    /** I5-25: cambiar las operaciones permitidas después de revisar descarta la revisión si cambia el efectivo. */
    @Test
    public void testCambiarOperacionesDespuesDeRevisarDescartaLaRevision() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        try (Workbook wb = buildWorkbook()) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.checkValidation(null);
            srv.setAllowInsert(false);
            assertTrue(srv.getOverWriteData(), "sin altas, sobrescribir queda forzado");
            assertFalse(srv.getErrorsReviewed());
            srv.checkValidation(null);
            srv.setAllowUpdate(true);
            assertTrue(srv.getErrorsReviewed(), "el efectivo no cambió: la revisión queda");
        }
    }

    /** Apagar «sobreescribir» después de revisar también descarta la revisión. */
    @Test
    public void testApagarSobrescribirDespuesDeRevisarDescartaLaRevision() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        try (Workbook wb = buildWorkbook()) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.setOverWriteData(true);
            srv.checkValidation(null);
            assertTrue(srv.getErrorsReviewed());
            srv.setOverWriteData(false);
            assertFalse(srv.getErrorsReviewed());
        }
    }

    /** Con la opción fijada (sin modificaciones) el valor efectivo no cambia y la revisión queda. */
    @Test
    public void testSobrescribirFijadoNoDescartaLaRevision() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        try (Workbook wb = buildWorkbook()) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.setAllowUpdate(false);
            srv.checkValidation(null);
            srv.setOverWriteData(true);
            assertFalse(srv.getOverWriteData());
            assertTrue(srv.getErrorsReviewed(), "fijado: tocar la casilla no cambia nada");
        }
    }

    /** Volver a poner el mismo valor de «sobreescribir» no descarta la revisión. */
    @Test
    public void testMismoSobrescribirNoDescartaLaRevision() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        try (Workbook wb = buildWorkbook()) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.checkValidation(null);
            srv.setOverWriteData(false);
            assertTrue(srv.getErrorsReviewed());
        }
    }

    /** Con «sobreescribir», el existente se sigue validando como modificación. */
    @Test
    public void testRevisionPreviaValidaElExistenteConSobrescribir() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        srv.updateCheckErrors.put("fecha",
                new org.javabeanstack.error.ErrorInfo("Tiene asiento de ajuste", 50124, "fecha"));
        try (Workbook wb = buildWorkbook()) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.setOverWriteData(true);

            srv.checkValidation(null);
            assertEquals(1, srv.updateChecks);
            assertEquals(1, srv.getDataRowsError().size(), "la modificación rechazada queda en errores");
        }
    }

    @Test
    public void testAllowInsertFalseSoloActualizaYRechazaLosNuevos() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        try (Workbook wb = buildWorkbook()) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.setAllowInsert(false);
            srv.setOverWriteData(false);
            assertTrue(srv.getOverWriteData(), "sin altas, sobrescribir queda forzado");
            assertTrue(srv.isOverWriteLocked());

            srv.checkValidation(null);
            assertEquals(1, srv.getDataRowsError().size());
            assertEquals(ExcelImportSrv.INSERT_NOT_ALLOWED, srv.getDataRowsError().get(0).getErrorMsg().trim());
            assertEquals("u2", ((AppUser) srv.getDataRowsError().get(0)).getCode());

            srv.importData();
            assertEquals(1, srv.getRowsMigratedCount());
            assertEquals(IDataRow.UPDATE, srv.updated.get(0).getAction());
            assertEquals("u1", ((AppUser) srv.updated.get(0)).getCode());
        }
    }

    @Test
    public void testAllowDeleteEliminaLasFilasMarcadas() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        try (Workbook wb = buildWorkbookConBajas(true, false)) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.setAllowDelete(true);

            srv.checkValidation(null);
            assertTrue(srv.getDataRowsError().isEmpty(), String.valueOf(srv.getDataRowsError()));
            assertTrue(ExcelImportSrv.isMarkedForDelete(srv.getDataRows().get(0)));

            srv.importData();
            assertEquals(1, srv.getRowsDeletedCount());
            assertEquals(1, srv.deleted.size());
            assertEquals(IDataRow.DELETE, srv.deleted.get(0).getAction());
            assertEquals(100L, ((AppUser) srv.deleted.get(0)).getIduser(), "se elimina el existente");
            assertEquals(1, srv.getRowsMigratedCount(), "u2 se da de alta");
            assertTrue(srv.getResultLog().contains("Eliminado:"));
            assertTrue(srv.getResultLog().contains("eliminados: 1"));
        }
    }

    @Test
    public void testSinColumnaDeMarcaNoSeEliminaNada() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        try (Workbook wb = buildWorkbookConBajas(false, false)) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.setAllowDelete(true);
            srv.checkValidation(null);
            srv.importData();
            assertEquals(0, srv.getRowsDeletedCount());
            assertTrue(srv.deleted.isEmpty());
            assertEquals(1, srv.getRowsExistCount(), "u1 se trata como una fila más");
        }
    }

    @Test
    public void testMarcaSinAllowDeleteSeIgnora() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        try (Workbook wb = buildWorkbookConBajas(true, false)) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.checkValidation(null);
            srv.importData();
            assertTrue(srv.deleted.isEmpty());
            assertEquals(1, srv.getRowsExistCount());
        }
    }

    @Test
    public void testBajaDeUnInexistenteOSinPermisoEsError() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        try (Workbook wb = buildWorkbookConBajas(true, true)) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.setAllowDelete(true);
            srv.checkValidation(null);
            assertEquals(1, srv.getDataRowsError().size());
            assertEquals("u9", ((AppUser) srv.getDataRowsError().get(0)).getCode());
            assertTrue(srv.getDataRowsError().get(0).getErrorMsg().contains("No existe el registro a eliminar"));
        }
        ExistingRowSrv sinPermiso = new ExistingRowSrv();
        sinPermiso.deleteAuthorized = false;
        try (Workbook wb = buildWorkbookConBajas(true, false)) {
            sinPermiso.setExcelWorkbook(wb);
            sinPermiso.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            sinPermiso.setAllowDelete(true);
            sinPermiso.checkValidation(null);
            assertEquals(1, sinPermiso.getDataRowsError().size());
            assertTrue(sinPermiso.getDataRowsError().get(0).getErrorMsg().contains("No está autorizado para eliminar"));
        }
    }

    /**
     * E-01: una fila marcada solo necesita los valores que ubican el registro;
     * una columna obligatoria vacía que no hace a la clave no frena la baja. Si
     * el registro no aparece, se informa junto con los errores de lectura.
     */
    @Test
    public void testBajaNoExigeLasColumnasObligatoriasAjenasALaClave() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        try (Workbook wb = buildWorkbook(new String[]{"code", "fullName", "accion"},
                new String[]{"u1", "", "B"}, new String[]{"", "", "B"})) {
            srv.setExcelWorkbook(wb);
            ExcelColumns columns = new ExcelColumns();
            columns.add("code", "code").require(ColumnRequirement.VALUE);
            columns.add("fullName", "fullName").require(ColumnRequirement.VALUE);
            srv.setExcelRowProcessor(new ExcelRowProcessor<AppUser>(
                    wb.getSheetAt(0).getRow(0), AppUser.class, columns) {
            });
            srv.setAllowDelete(true);
            srv.checkValidation(null);
            assertEquals(1, srv.getDataRowsError().size(), String.valueOf(srv.getDataRowsError()));
            String msg = srv.getDataRowsError().get(0).getErrorMsg();
            assertTrue(msg.contains("«code» no puede estar vacía"), msg);
            assertTrue(msg.contains(ExcelImportSrv.DELETE_NOT_FOUND), msg);
            srv.importData();
            assertEquals(1, srv.getRowsDeletedCount(), "u1 se elimina aunque falte fullName");
        }
    }

    /**
     * E-02: la revisión previa valida la baja con la acción DELETE, así la
     * grilla muestra lo que el servicio rechazaría al grabar.
     */
    @Test
    public void testRevisionPreviaValidaLaBajaConDelete() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        srv.deleteCheckErrors.put("iduser", new org.javabeanstack.error.ErrorInfo(
                "No se puede eliminar: tiene movimientos", 50000, "iduser"));
        try (Workbook wb = buildWorkbookConBajas(true, false)) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.setAllowDelete(true);
            srv.checkValidation(null);
            assertEquals(1, srv.getDataRowsError().size());
            assertTrue(srv.getDataRowsError().get(0).getErrorMsg().contains("tiene movimientos"));
            srv.importData();
            assertTrue(srv.deleted.isEmpty(), "la baja rechazada en la revisión no se intenta");
            assertEquals(1, srv.updated.size(), "u2, válida, se graba igual");
        }
    }

    /**
     * E-03: el registro de otra empresa (p. ej. resuelto en un catálogo
     * compartido) no se elimina.
     */
    @Test
    public void testBajaDeOtraEmpresaEsError() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        srv.companyOfU1 = 5L;
        try (Workbook wb = buildWorkbookConBajas(true, false)) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.setAllowDelete(true);
            srv.checkValidation(null);
            assertEquals(1, srv.getDataRowsError().size());
            assertEquals(ExcelImportSrv.DELETE_OTHER_COMPANY, srv.getDataRowsError().get(0).getErrorMsg().trim());
        }
    }

    /** Cabecera con empresa propia (como una moneda). */
    public static class Cabecera extends org.javabeanstack.data.DataRow {
        private Long idempresa;

        public Long getIdempresa() {
            return idempresa;
        }

        public void setIdempresa(Long idempresa) {
            this.idempresa = idempresa;
        }
    }

    /** Detalle sin empresa propia (como una cotización de una moneda). */
    public static class Detalle extends org.javabeanstack.data.DataRow {
        @jakarta.persistence.ManyToOne
        private Cabecera cabecera;

        public Cabecera getCabecera() {
            return cabecera;
        }

        public void setCabecera(Cabecera cabecera) {
            this.cabecera = cabecera;
        }
    }

    /** Servicio con una sesión de empresa de datos 50 (la máscara) y de catálogo 7. */
    static ExistingRowSrv conSesion() {
        return new ExistingRowSrv() {
            @Override
            protected IUserSession getUserSession() {
                return (IUserSession) Proxy.newProxyInstance(IUserSession.class.getClassLoader(),
                        new Class[]{IUserSession.class}, (proxy, method, args) -> {
                            switch (method.getName()) {
                                case "getIdCompany":
                                    return 50L;
                                case "getCompany":
                                    return Proxy.newProxyInstance(org.javabeanstack.model.IAppCompany.class.getClassLoader(),
                                            new Class[]{org.javabeanstack.model.IAppCompany.class},
                                            (p2, m2, a2) -> "getIdcompany".equals(m2.getName()) ? 7L : null);
                                default:
                                    return null;
                            }
                        });
            }
        };
    }

    /**
     * E-04/I-04: la empresa se busca en {@code idempresa} (contra la máscara),
     * en {@code idcompany} (contra el id del catálogo) o, si la entidad no
     * tiene ninguno, en sus asociaciones {@code @ManyToOne}.
     */
    @Test
    public void testEmpresaDelRegistroAEliminar() {
        ExistingRowSrv srv = conSesion();
        AppUser deCatalogo = new AppUser();
        deCatalogo.setIdcompany(7L);
        assertNull(srv.checkDeleteRow("s", deCatalogo), "idcompany contra la empresa del catálogo, no la máscara");
        deCatalogo.setIdcompany(50L);
        assertEquals(ExcelImportSrv.DELETE_OTHER_COMPANY, srv.checkDeleteRow("s", deCatalogo));

        Cabecera propia = new Cabecera();
        propia.setIdempresa(50L);
        Cabecera ajena = new Cabecera();
        ajena.setIdempresa(41L);
        assertNull(srv.checkDeleteRow("s", propia));
        assertEquals(ExcelImportSrv.DELETE_OTHER_COMPANY, srv.checkDeleteRow("s", ajena));

        Detalle detalle = new Detalle();
        detalle.setCabecera(propia);
        assertNull(srv.checkDeleteRow("s", detalle), "detalle de una cabecera propia");
        detalle.setCabecera(ajena);
        assertEquals(ExcelImportSrv.DELETE_OTHER_COMPANY, srv.checkDeleteRow("s", detalle),
                "detalle de una cabecera de otra empresa");
        detalle.setCabecera(null);
        assertNull(srv.checkDeleteRow("s", detalle), "sin cabecera no hay con qué comparar");
    }

    /**
     * Baja sin revisión previa: el permiso, la existencia y el contador se
     * resuelven en la importación; los eliminados cuentan como procesados.
     */
    @Test
    public void testBajaSinRevisionPrevia() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        try (Workbook wb = buildWorkbookConBajas(true, true)) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.setAllowDelete(true);
            srv.setCheckBeforeErrors(false);
            srv.importData();
            assertEquals(1, srv.getRowsDeletedCount());
            assertEquals(1, srv.getRowsMigratedCount(), "u2 se da de alta");
            assertEquals(1, srv.getDataRowsError().size());
            assertTrue(srv.getDataRowsError().get(0).getErrorMsg().contains(ExcelImportSrv.DELETE_NOT_FOUND));
            assertEquals(3, srv.getRowsProcessedCount());
        }
        ExistingRowSrv sinPermiso = new ExistingRowSrv();
        sinPermiso.deleteAuthorized = false;
        try (Workbook wb = buildWorkbookConBajas(true, false)) {
            sinPermiso.setExcelWorkbook(wb);
            sinPermiso.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            sinPermiso.setAllowDelete(true);
            sinPermiso.setCheckBeforeErrors(false);
            sinPermiso.importData();
            assertTrue(sinPermiso.deleted.isEmpty());
            assertTrue(sinPermiso.getDataRowsError().get(0).getErrorMsg().contains(ExcelImportSrv.DELETE_NOT_AUTHORIZED));
        }
    }

    /** Una baja que el servicio rechaza al grabar es error de fila y no cuenta. */
    @Test
    public void testBajaRechazadaAlGrabar() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        srv.deleteFails = true;
        try (Workbook wb = buildWorkbookConBajas(true, false)) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.setAllowDelete(true);
            srv.checkValidation(null);
            srv.importData();
            assertEquals(0, srv.getRowsDeletedCount());
            assertEquals(1, srv.getDataRowsError().size());
            assertTrue(srv.getDataRowsError().get(0).getErrorMsg().contains("registros relacionados"));
        }
    }

    /**
     * La marca y su cabecera se comparan sin espacios alrededor ni mayúsculas;
     * sin altas, una fila marcada inexistente es «no existe» (la baja gana).
     */
    @Test
    public void testMarcaConEspaciosYSinAltas() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        try (Workbook wb = buildWorkbook(new String[]{"code", "fullName", " Accion "},
                new String[]{"u1", "User One", " b "}, new String[]{"u9", "Nadie", "B"})) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.setAllowDelete(true);
            srv.setAllowInsert(false);
            srv.checkValidation(null);
            assertEquals(1, srv.getDataRowsError().size());
            assertTrue(srv.getDataRowsError().get(0).getErrorMsg().contains(ExcelImportSrv.DELETE_NOT_FOUND));
            srv.importData();
            assertEquals(1, srv.getRowsDeletedCount());
        }
    }

    /**
     * Como en {@code checkMetaData}, la primera cabecera en blanco termina las
     * columnas: una «accion» después de ella no marca nada.
     */
    @Test
    public void testColumnaDeMarcaDespuesDeUnaCabeceraEnBlanco() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        try (Workbook wb = buildWorkbook(new String[]{"code", "fullName", null, "accion"},
                new String[]{"u1", "User One", "", "B"})) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.setAllowDelete(true);
            srv.checkValidation(null);
            srv.importData();
            assertTrue(srv.deleted.isEmpty());
            assertEquals(1, srv.getRowsExistCount());
        }
    }

    /**
     * Registro existente por clave única y sobreescritura apagada: la revisión
     * lo da por válido (se valida como UPDATE del existente, no como duplicado)
     * y la importación lo saltea contándolo como «ya existente», sin grabar.
     */
    @Test
    public void testExistingRowByUniqueKeyIsSkippedWithoutOverwrite() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        try (Workbook wb = buildWorkbook()) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.setOverWriteData(false);

            srv.checkValidation(null);
            assertEquals("", srv.getErrorMessage());
            assertEquals(2, srv.getRowsValidCount());
            assertTrue(srv.getDataRowsError().isEmpty());

            srv.importData();
            assertTrue(srv.getImportOk());
            assertEquals(1, srv.getRowsMigratedCount());
            assertEquals(1, srv.getRowsExistCount());
            assertEquals(1, srv.updated.size());
            assertEquals("u2", ((AppUser) srv.updated.get(0)).getCode());
            assertEquals(IDataRow.INSERT, srv.updated.get(0).getAction());
            assertTrue(srv.getResultLog().contains("Ya existe, no se sobreescribe"));
        }
    }

    /**
     * Registro existente y sobreescritura encendida: se graba el <b>registro
     * existente</b> (misma identidad) con los valores que trajo la planilla
     * copiados encima, conservando los que la planilla no trae (es una
     * actualización, no un reemplazo).
     */
    @Test
    public void testExistingRowByUniqueKeyIsUpdatedWithOverwrite() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        try (Workbook wb = buildWorkbook()) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.setOverWriteData(true);

            srv.checkValidation(null);
            srv.importData();

            assertTrue(srv.getImportOk());
            assertEquals(2, srv.getRowsMigratedCount());
            assertEquals(0, srv.getRowsExistCount());
            AppUser u1 = (AppUser) srv.updated.stream()
                    .filter(r -> "u1".equals(((AppUser) r).getCode())).findFirst().orElseThrow();
            assertEquals(IDataRow.UPDATE, u1.getAction());
            assertEquals(100L, u1.getIduser(), "se graba el registro existente, no uno nuevo");
            assertEquals("User One", u1.getFullName(), "el valor de la planilla pisa al previo");
            assertEquals("previo@dominio.com", u1.getEmail1(), "lo que la planilla no trae se conserva");
        }
    }

    /**
     * Con «sobreescribir», un atributo que la planilla NO trae y que el
     * procesador completó con el valor por defecto de la columna ausente no
     * pisa el valor del registro existente (decisión del usuario, 2026-09-16,
     * hallazgo I4-01 de la revisión final): la planilla trae solo {@code code}
     * y {@code fullName}; {@code email1} declara default y no viene.
     */
    @Test
    public void testDefaultDeColumnaAusenteNoPisaAlExistenteConSobreescritura() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        try (Workbook wb = buildWorkbook()) {
            srv.setExcelWorkbook(wb);
            ExcelColumns columns = new ExcelColumns();
            columns.add("code", "code").required();
            columns.add("fullName", "fullName");
            columns.add("email1", "email1").defaultValue("default@dominio.com");
            srv.setExcelRowProcessor(new ExcelRowProcessor<AppUser>(
                    wb.getSheetAt(0).getRow(0), AppUser.class, columns) {
            });
            srv.setOverWriteData(true);

            srv.checkValidation(null);
            srv.importData();

            assertEquals(2, srv.getRowsMigratedCount());
            AppUser u1 = (AppUser) srv.updated.stream()
                    .filter(r -> "u1".equals(((AppUser) r).getCode())).findFirst().orElseThrow();
            assertEquals(100L, u1.getIduser());
            assertEquals("User One", u1.getFullName(), "lo que trae la planilla sí pisa");
            assertEquals("previo@dominio.com", u1.getEmail1(),
                    "el default de la columna ausente NO pisa al existente");
            //En un alta (u2, no existe) el default sí se aplica.
            AppUser u2 = (AppUser) srv.updated.stream()
                    .filter(r -> "u2".equals(((AppUser) r).getCode())).findFirst().orElseThrow();
            assertEquals("default@dominio.com", u2.getEmail1());
        }
    }

    // ---- Celda vacía con defaultWhenBlank (D59 del plan XLSEXP de Maker, E4-20) ----

    /**
     * Corre la importación con «sobreescribir» sobre una planilla
     * {@code code}/{@code email1} donde {@code email1} declara un valor por
     * defecto con {@code defaultWhenBlank}, y devuelve la entidad que recibió
     * {@code save} para el {@code code} pedido. {@code u1} existe (con
     * {@code email1 = "previo@dominio.com"}); {@code u2} no.
     */
    private AppUser importarConDefaultWhenBlank(String code, String email1) throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        try (Workbook wb = buildWorkbook(new String[]{"code", "email1"}, new String[]{code, email1})) {
            srv.setExcelWorkbook(wb);
            ExcelColumns columns = new ExcelColumns();
            columns.add("code", "code").required();
            columns.add("email1", "email1").defaultValue("default@dominio.com").defaultWhenBlank();
            srv.setExcelRowProcessor(new ExcelRowProcessor<AppUser>(
                    wb.getSheetAt(0).getRow(0), AppUser.class, columns) {
            });
            srv.setOverWriteData(true);

            srv.checkValidation(null);
            srv.importData();

            assertEquals(1, srv.getRowsMigratedCount(), srv.getResultLog());
            return (AppUser) srv.updated.get(0);
        }
    }

    /** Alta con la celda vacía: se graba el valor por defecto. */
    @Test
    public void testDefaultWhenBlankAltaConCeldaVaciaGrabaElDefault() throws Exception {
        AppUser alta = importarConDefaultWhenBlank("u2", "");
        assertNull(alta.getIduser(), "es un alta");
        assertEquals("default@dominio.com", alta.getEmail1());
    }

    /** Alta con valor: se graba el valor de la celda. */
    @Test
    public void testDefaultWhenBlankAltaConValorGrabaElValor() throws Exception {
        AppUser alta = importarConDefaultWhenBlank("u2", "nuevo@dominio.com");
        assertNull(alta.getIduser(), "es un alta");
        assertEquals("nuevo@dominio.com", alta.getEmail1());
    }

    /**
     * «Sobreescribir» con la celda vacía: se conserva lo guardado (D59). Antes
     * de la corrección el default pisaba el valor del existente.
     */
    @Test
    public void testDefaultWhenBlankSobreescribirConCeldaVaciaConservaLoGuardado() throws Exception {
        AppUser existente = importarConDefaultWhenBlank("u1", "");
        assertEquals(100L, existente.getIduser(), "es el existente");
        assertEquals("previo@dominio.com", existente.getEmail1(),
                "la celda vacía con default NO pisa al existente");
    }

    /** «Sobreescribir» con valor: el valor de la celda pisa al existente. */
    @Test
    public void testDefaultWhenBlankSobreescribirConValorPisa() throws Exception {
        AppUser existente = importarConDefaultWhenBlank("u1", "nuevo@dominio.com");
        assertEquals(100L, existente.getIduser(), "es el existente");
        assertEquals("nuevo@dominio.com", existente.getEmail1());
    }

    // ---- Revisión previa con Bean Validation y motivo en el log (XLSEXP, E4-21) ----

    /**
     * Validador simulado: para {@code validateProperty} mira las anotaciones
     * {@code @NotNull} y {@code @Size} del atributo, como lo haría la
     * implementación de Bean Validation (que no está en el classpath de las
     * pruebas). Registra los atributos que se le piden.
     */
    static class FakeValidator implements InvocationHandler {

        final List<String> properties = new ArrayList<>();

        jakarta.validation.Validator proxy() {
            return (jakarta.validation.Validator) Proxy.newProxyInstance(
                    jakarta.validation.Validator.class.getClassLoader(),
                    new Class[]{jakarta.validation.Validator.class}, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (!"validateProperty".equals(method.getName())) {
                throw new UnsupportedOperationException(method.getName());
            }
            Object bean = args[0];
            String property = (String) args[1];
            properties.add(property);
            java.util.Set<jakarta.validation.ConstraintViolation<?>> result = new java.util.LinkedHashSet<>();
            java.lang.reflect.Field field = bean.getClass().getDeclaredField(property);
            field.setAccessible(true);
            Object value = field.get(bean);
            jakarta.validation.constraints.NotNull notNull = field.getAnnotation(jakarta.validation.constraints.NotNull.class);
            if (notNull != null && value == null) {
                result.add(violation(property, notNull.message().startsWith("{") ? "no debe ser nulo" : notNull.message()));
            }
            jakarta.validation.constraints.Size size = field.getAnnotation(jakarta.validation.constraints.Size.class);
            if (size != null && value instanceof String
                    && (((String) value).length() < size.min() || ((String) value).length() > size.max())) {
                result.add(violation(property, "el tamaño debe estar entre " + size.min() + " y " + size.max()));
            }
            return result;
        }
    }

    /** Violación simulada con su atributo y su mensaje. */
    static jakarta.validation.ConstraintViolation<?> violation(String property, String message) {
        jakarta.validation.Path path = (jakarta.validation.Path) Proxy.newProxyInstance(
                jakarta.validation.Path.class.getClassLoader(), new Class[]{jakarta.validation.Path.class},
                (p, m, a) -> "toString".equals(m.getName()) ? property : null);
        return (jakarta.validation.ConstraintViolation<?>) Proxy.newProxyInstance(
                jakarta.validation.ConstraintViolation.class.getClassLoader(),
                new Class[]{jakarta.validation.ConstraintViolation.class}, (p, m, a) -> {
                    switch (m.getName()) {
                        case "getMessage":
                            return message;
                        case "getPropertyPath":
                            return path;
                        case "hashCode":
                            return System.identityHashCode(p);
                        case "equals":
                            return p == a[0];
                        default:
                            return null;
                    }
                });
    }

    /** {@link ExistingRowSrv} con el validador simulado. */
    static class ValidatingSrv extends ExistingRowSrv {

        final FakeValidator fake = new FakeValidator();

        @Override
        protected jakarta.validation.Validator getValidator() {
            return fake.proxy();
        }
    }

    /**
     * La revisión previa rechaza la fila que la grabación rechazaría por Bean
     * Validation: {@code fullName} es {@code @NotNull @Size(min = 1)}; con la
     * celda vacía el atributo de texto queda en {@code ""} y lo rechaza el
     * {@code @Size}. El error nombra la columna, lleva el número 50000 y la
     * fila no se graba; el log de la importación da el motivo con su número.
     */
    @Test
    public void testRevisionPreviaRechazaLaFilaQueBeanValidationRechazaria() throws Exception {
        ValidatingSrv srv = new ValidatingSrv();
        try (Workbook wb = buildWorkbook(new String[]{"code", "fullName"},
                new String[]{"u2", ""}, new String[]{"u3", "User Three"})) {
            srv.setExcelWorkbook(wb);
            ExcelColumns columns = new ExcelColumns();
            columns.add("code", "code").required();
            columns.add("fullName", "fullName").required();
            srv.setExcelRowProcessor(new ExcelRowProcessor<AppUser>(
                    wb.getSheetAt(0).getRow(0), AppUser.class, columns) {
            });

            srv.checkValidation(null);

            assertEquals(1, srv.getRowsValidCount());
            assertEquals(1, srv.getRowsErrorCount());
            AppUser rechazada = srv.getDataRowsError().get(0);
            assertEquals("u2", rechazada.getCode());
            org.javabeanstack.error.IErrorInfo error = rechazada.getErrors().get("fullname");
            assertNotNull(error, String.valueOf(rechazada.getErrors()));
            assertEquals(Integer.valueOf(50000), error.getErrorNumber());
            assertEquals("La columna «fullName» no es válida: el tamaño debe estar entre 1 y 50",
                    error.getMessage());
            assertTrue(srv.fake.properties.containsAll(List.of("code", "fullName")), srv.fake.properties.toString());

            srv.importData();

            assertEquals(1, srv.getRowsMigratedCount());
            assertEquals("u3", ((AppUser) srv.updated.get(0)).getCode());
            assertTrue(srv.getResultLog().contains(
                    "La columna «fullName» no es válida: el tamaño debe estar entre 1 y 50 - (50000)"),
                    srv.getResultLog());
        }
    }

    /**
     * Un registro existente que no se va a grabar (sin «sobreescribir») no se
     * valida con Bean Validation: la importación solo lo cuenta como «ya
     * existe».
     */
    @Test
    public void testRevisionPreviaNoValidaElExistenteQueNoSeGraba() throws Exception {
        ValidatingSrv srv = new ValidatingSrv();
        try (Workbook wb = buildWorkbook(new String[]{"code", "fullName"}, new String[]{"u1", ""})) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(new ExcelRowProcessor<AppUser>(
                    wb.getSheetAt(0).getRow(0), AppUser.class, ExcelColumns.of(null)) {
            });

            srv.checkValidation(null);

            assertEquals(0, srv.getRowsErrorCount());
            assertTrue(srv.fake.properties.isEmpty(), srv.fake.properties.toString());
            srv.importData();
            assertEquals(1, srv.getRowsExistCount());
        }
    }

    /**
     * Bean Validation y {@code checkDataRow} se informan juntos: la fila
     * rechazada por las dos lleva los dos motivos.
     */
    @Test
    public void testRevisionPreviaJuntaBeanValidationYCheckDataRow() throws Exception {
        ValidatingSrv srv = new ValidatingSrv() {
            @Override
            protected IDataService getDataService() {
                IDataService base = super.getDataService();
                return (IDataService) Proxy.newProxyInstance(IDataService.class.getClassLoader(),
                        new Class[]{IDataService.class}, (p, m, a) -> {
                            if ("checkDataRow".equals(m.getName())) {
                                Map<String, org.javabeanstack.error.IErrorInfo> errores = new HashMap<>();
                                errores.put("code", new org.javabeanstack.error.ErrorInfo("Código inválido", 51000, "code"));
                                return errores;
                            }
                            return m.invoke(base, a);
                        });
            }
        };
        try (Workbook wb = buildWorkbook(new String[]{"code", "fullName"}, new String[]{"u2", ""})) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(new ExcelRowProcessor<AppUser>(
                    wb.getSheetAt(0).getRow(0), AppUser.class, ExcelColumns.of(null)) {
            });

            srv.checkValidation(null);

            Map<String, org.javabeanstack.error.IErrorInfo> errores = srv.getDataRowsError().get(0).getErrors();
            assertEquals(java.util.Set.of("fullname", "code"), errores.keySet());
        }
    }

    /**
     * Si Bean Validation y {@code checkDataRow} marcan el mismo atributo, queda
     * el error de {@code checkDataRow}, que es el más específico (por ejemplo
     * la clave foránea que no resuelve, número 50013).
     */
    @Test
    public void testRevisionPreviaConElMismoAtributoGanaCheckDataRow() throws Exception {
        ValidatingSrv srv = new ValidatingSrv() {
            @Override
            protected IDataService getDataService() {
                IDataService base = super.getDataService();
                return (IDataService) Proxy.newProxyInstance(IDataService.class.getClassLoader(),
                        new Class[]{IDataService.class}, (p, m, a) -> {
                            if ("checkDataRow".equals(m.getName())) {
                                Map<String, org.javabeanstack.error.IErrorInfo> errores = new HashMap<>();
                                errores.put("fullname", new org.javabeanstack.error.ErrorInfo(
                                        "Dejo en blanco este dato o no existe el registro - fullName", 50013, "fullName"));
                                return errores;
                            }
                            return m.invoke(base, a);
                        });
            }
        };
        try (Workbook wb = buildWorkbook(new String[]{"code", "fullName"}, new String[]{"u2", ""})) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(new ExcelRowProcessor<AppUser>(
                    wb.getSheetAt(0).getRow(0), AppUser.class, ExcelColumns.of(null)) {
            });

            srv.checkValidation(null);

            org.javabeanstack.error.IErrorInfo error = srv.getDataRowsError().get(0).getErrors().get("fullname");
            assertEquals(Integer.valueOf(50013), error.getErrorNumber());
            assertTrue(srv.fake.properties.contains("fullName"), "Bean Validation corrió igual");
        }
    }

    /**
     * Si la grabación corta por una violación de Bean Validation, el DAO deja
     * solo el texto y el mapa de errores vacío: la fila igual queda con el
     * motivo, un error por atributo, y el log lo muestra con su número (antes
     * la fila salía en el log sin mensaje, E4-21).
     */
    @Test
    public void testLogDelRechazoAlGrabarPorBeanValidationDaElMotivo() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        java.util.Set<jakarta.validation.ConstraintViolation<?>> violaciones = new java.util.LinkedHashSet<>();
        violaciones.add(violation("inactivo", "no debe ser nulo"));
        violaciones.add(violation("bloquearAtrasoDias", "no debe ser nulo"));
        jakarta.persistence.PersistenceException envuelta = new jakarta.persistence.PersistenceException(
                new jakarta.validation.ConstraintViolationException("Validation failed", violaciones));
        srv.saveResult = failedResultWithoutMap("Validation failed for classes [...]", envuelta);
        try (Workbook wb = buildWorkbook(new String[]{"code", "fullName"}, new String[]{"u2", "User Two"})) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));

            srv.checkValidation(null);
            srv.importData();

            assertEquals(0, srv.getRowsMigratedCount());
            assertEquals(1, srv.getRowsErrorCount());
            Map<String, org.javabeanstack.error.IErrorInfo> errores = srv.getDataRowsError().get(0).getErrors();
            assertEquals(java.util.Set.of("inactivo", "bloquearatrasodias"), errores.keySet());
            assertEquals("«inactivo» no debe ser nulo", errores.get("inactivo").getMessage());
            assertEquals(Integer.valueOf(50000), errores.get("inactivo").getErrorNumber());
            //El orden de los atributos en la línea depende del mapa de la fila.
            assertTrue(srv.getResultLog().contains("«inactivo» no debe ser nulo - (50000)"), srv.getResultLog());
            assertTrue(srv.getResultLog().contains("«bloquearAtrasoDias» no debe ser nulo - (50000)"),
                    srv.getResultLog());
        }
    }

    /**
     * Si la grabación corta por otra excepción y el resultado trae solo el
     * texto, la fila queda con ese texto y el número 1 (error que viene de una
     * excepción).
     */
    @Test
    public void testLogDelRechazoAlGrabarPorOtraExcepcionDaElTexto() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        srv.saveResult = failedResultWithoutMap("El trigger rechazó la fila", new IllegalStateException("x"));
        try (Workbook wb = buildWorkbook(new String[]{"code", "fullName"}, new String[]{"u2", "User Two"})) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));

            srv.checkValidation(null);
            srv.importData();

            assertEquals(1, srv.getRowsErrorCount());
            assertTrue(srv.getResultLog().contains("El trigger rechazó la fila - (1)"), srv.getResultLog());
        }
    }

    /**
     * Resultado de grabación fallido como el que deja el DAO cuando la
     * grabación corta por una excepción: mapa de errores vacío, el texto en
     * {@code getErrorMsg()} y la excepción.
     */
    private static org.javabeanstack.data.IDataResult failedResultWithoutMap(String message, Exception exp) {
        return (org.javabeanstack.data.IDataResult) Proxy.newProxyInstance(
                org.javabeanstack.data.IDataResult.class.getClassLoader(),
                new Class[]{org.javabeanstack.data.IDataResult.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "isSuccessFul":
                            return false;
                        case "getErrorsMap":
                            return new HashMap<>();
                        case "getErrorMsg":
                            return message;
                        case "getException":
                            return exp;
                        default:
                            Class<?> type = method.getReturnType();
                            if (type == boolean.class || type == Boolean.class) {
                                return false;
                            }
                            return null;
                    }
                });
    }

    // ---- Revisión previa: defaults de checkDataRow fuera de la planilla (XLSEXP, E4-23) ----

    /**
     * Entidad destino con atributos {@code @NotNull} que <b>no</b> son
     * columnas de la planilla, como {@code inactivo} o
     * {@code bloquearAtrasoDias} de {@code Ctactesub} (E4-23 del plan XLSEXP
     * de Maker). {@code type} sí es columna; {@code observacion} es texto
     * (sin valor por defecto en {@code checkDataRow}) e {@code idpadre} lleva
     * {@code @CheckForeignkey(check = false)}, que en {@code checkDataRow}
     * también saltea el valor por defecto.
     */
    public static class Obligatorios extends org.javabeanstack.data.DataRow {
        @jakarta.persistence.Id
        @jakarta.persistence.Column(name = "iduser")
        private Long iduser;
        @jakarta.persistence.Column(name = "code")
        private String code;
        @jakarta.persistence.Column(name = "fullname")
        private String fullName;
        @jakarta.validation.constraints.NotNull
        @jakarta.persistence.Column(name = "type")
        private Short type;
        @jakarta.validation.constraints.NotNull
        @jakarta.persistence.Column(name = "inactivo")
        private Boolean inactivo;
        @jakarta.validation.constraints.NotNull
        @jakarta.persistence.Column(name = "diasatraso")
        private Integer diasAtraso;
        @jakarta.validation.constraints.NotNull
        @jakarta.persistence.Column(name = "saldo")
        private java.math.BigDecimal saldo;
        @jakarta.validation.constraints.NotNull
        @jakarta.persistence.Column(name = "limite")
        private Long limite;
        @jakarta.validation.constraints.NotNull
        @jakarta.persistence.Column(name = "observacion")
        private String observacion;
        @jakarta.validation.constraints.NotNull
        @org.javabeanstack.annotation.CheckForeignkey(check = false)
        @jakarta.persistence.Column(name = "idpadre")
        private Long idpadre;

        public Long getIduser() {
            return iduser;
        }

        public void setIduser(Long iduser) {
            this.iduser = iduser;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public Short getType() {
            return type;
        }

        public void setType(Short type) {
            this.type = type;
        }

        public Boolean getInactivo() {
            return inactivo;
        }

        public void setInactivo(Boolean inactivo) {
            this.inactivo = inactivo;
        }

        public Integer getDiasAtraso() {
            return diasAtraso;
        }

        public void setDiasAtraso(Integer diasAtraso) {
            this.diasAtraso = diasAtraso;
        }

        public java.math.BigDecimal getSaldo() {
            return saldo;
        }

        public void setSaldo(java.math.BigDecimal saldo) {
            this.saldo = saldo;
        }

        public Long getLimite() {
            return limite;
        }

        public void setLimite(Long limite) {
            this.limite = limite;
        }

        public String getObservacion() {
            return observacion;
        }

        public void setObservacion(String observacion) {
            this.observacion = observacion;
        }

        public Long getIdpadre() {
            return idpadre;
        }

        public void setIdpadre(Long idpadre) {
            this.idpadre = idpadre;
        }
    }

    /**
     * Servicio simulado para E4-23: graba en {@link Obligatorios}, declara
     * existente la fila {@code u1} (id 100, {@code type} 3, {@code diasAtraso}
     * 7 y el resto de los {@code @NotNull} en NULL, como un registro con NULL
     * en la base) y su {@code checkDataRow} completa los valores por defecto
     * igual que {@code AbstractDataService.checkDataRow}: solo en altas y
     * modificaciones, sin {@code FOREIGNKEY_NOCHECK}, sobre los atributos
     * declarados en la clase, salteando {@code @CheckForeignkey(check = false)}
     * y con 0/{@code false} según el tipo. Así reproduce la entidad
     * descartable de la revisión previa y el camino sin revisión.
     */
    static class NotNullDefaultsSrv extends ExcelImportSrvTest01 {

        /** Entidades que recibe {@code save}. */
        final List<Obligatorios> saved = new ArrayList<>();
        /** Veces que se llamó a {@code checkDataRow}. */
        int checkDataRowCalls;
        /** Si {@code onAfterRowConvert} marca la entidad con {@code FOREIGNKEY_NOCHECK}. */
        boolean foreignKeyNoCheck;
        /** {@code type} guardado en el existente {@code u1} (null = NULL en la base). */
        Short typeOfU1 = 3;
        /** Validador simulado de la revisión previa (null = sin Bean Validation). */
        FakeValidator validator;

        @Override
        protected jakarta.validation.Validator getValidator() {
            return validator == null ? null : validator.proxy();
        }

        /** Excepción con la que se canceló la corrida, para el mensaje de las aserciones. */
        Exception cancelled;

        @Override
        protected void cancelError(Exception exp) {
            cancelled = exp;
        }

        @Override
        protected boolean onAfterRowConvert(AppUser rowSource, IDataRow rowTarget) {
            if (foreignKeyNoCheck) {
                rowTarget.getProperties().put("FOREIGNKEY_NOCHECK", Boolean.TRUE);
            }
            return true;
        }

        @Override
        protected IDataService getDataService() {
            InvocationHandler handler = (proxy, method, args) -> {
                switch (method.getName()) {
                    case "copyTo":
                        return ((IDataRow) args[1]).copyTo((IDataRow) args[2]);
                    case "checkDataRow":
                        checkDataRowCalls++;
                        completarComoCheckDataRow((IDataRow) args[1]);
                        return new HashMap<>();
                    case "findById":
                        return null;
                    case "findByUk":
                        if ("u1".equals(((Obligatorios) args[1]).getCode())) {
                            Obligatorios existing = new Obligatorios();
                            existing.setIduser(100L);
                            existing.setCode("u1");
                            existing.setFullName("Nombre previo");
                            existing.setType(typeOfU1);
                            existing.setDiasAtraso(7);
                            existing.setAction(0);
                            return existing;
                        }
                        return null;
                    case "save":
                        saved.add((Obligatorios) args[1]);
                        return stub(org.javabeanstack.data.IDataResult.class);
                    default:
                        break;
                }
                Class<?> type = method.getReturnType();
                if (type.isInterface()) {
                    return stub(type);
                }
                if (type == boolean.class || type == Boolean.class) {
                    return false;
                }
                return null;
            };
            return (IDataService) Proxy.newProxyInstance(IDataService.class.getClassLoader(),
                    new Class[]{IDataService.class}, handler);
        }

        /** El bloque de valores por defecto de {@code AbstractDataService.checkDataRow}. */
        static void completarComoCheckDataRow(IDataRow row) throws Exception {
            if (Boolean.TRUE.equals(row.getProperties().get("FOREIGNKEY_NOCHECK"))
                    || (row.getAction() != IDataRow.INSERT && row.getAction() != IDataRow.UPDATE)) {
                return;
            }
            for (java.lang.reflect.Field field : row.getClass().getDeclaredFields()) {
                org.javabeanstack.annotation.CheckForeignkey fk
                        = field.getAnnotation(org.javabeanstack.annotation.CheckForeignkey.class);
                if (fk != null && !fk.check()) {
                    continue;
                }
                if (field.getAnnotation(jakarta.validation.constraints.NotNull.class) == null
                        || row.getValue(field.getName()) != null) {
                    continue;
                }
                Class<?> type = row.getFieldType(field.getName());
                if (type == java.math.BigDecimal.class) {
                    row.setValue(field.getName(), java.math.BigDecimal.ZERO);
                } else if (type == Boolean.class) {
                    row.setValue(field.getName(), false);
                } else if (type == Long.class) {
                    row.setValue(field.getName(), 0L);
                } else if (type == Integer.class) {
                    row.setValue(field.getName(), 0);
                } else if (type == Short.class) {
                    row.setValue(field.getName(), (short) 0);
                }
            }
        }

        @Override
        protected IUserSession getUserSession() {
            return stub(IUserSession.class);
        }

        @Override
        protected Class<? extends IDataRow> getTargetType() {
            return Obligatorios.class;
        }
    }

    /**
     * Importa una fila ({@code code}, {@code fullName}, {@code type}) en
     * {@link Obligatorios} con el servicio de E4-23 y devuelve el servicio,
     * con la entidad grabada en {@code saved}. {@code type} es columna con
     * valor por defecto 5 y {@code defaultWhenBlank} (D59) si
     * {@code typeConDefault}; si no, sin valor por defecto. Un {@code type}
     * {@code null} deja la celda en blanco, como la escribe la exportación.
     *
     * @param conRevision si se hace la revisión previa ({@code checkValidation}
     * antes de {@code importData}); sin ella, la importación no revisa antes.
     */
    private NotNullDefaultsSrv importarObligatorios(String code, String type, boolean conRevision,
            boolean typeConDefault, boolean foreignKeyNoCheck) throws Exception {
        return importarObligatorios(code, type, conRevision, typeConDefault, foreignKeyNoCheck, (short) 3, false);
    }

    /**
     * Variante con el {@code type} guardado en el existente {@code u1}
     * ({@code null} = NULL en la base) y, si {@code conBeanValidation}, el
     * validador simulado en la revisión previa (E4-21).
     */
    private NotNullDefaultsSrv importarObligatorios(String code, String type, boolean conRevision,
            boolean typeConDefault, boolean foreignKeyNoCheck, Short typeOfU1,
            boolean conBeanValidation) throws Exception {
        NotNullDefaultsSrv srv = new NotNullDefaultsSrv();
        srv.foreignKeyNoCheck = foreignKeyNoCheck;
        srv.typeOfU1 = typeOfU1;
        srv.validator = conBeanValidation ? new FakeValidator() : null;
        try (Workbook wb = buildWorkbook(new String[]{"code", "fullName", "type"},
                new String[]{code, "Nombre nuevo", type})) {
            srv.setExcelWorkbook(wb);
            ExcelColumns columns = new ExcelColumns();
            columns.add("code", "code").required();
            columns.add("fullName", "fullName");
            if (typeConDefault) {
                columns.add("type", "type").defaultValue("5").defaultWhenBlank();
            } else {
                columns.add("type", "type");
            }
            srv.setExcelRowProcessor(new ExcelRowProcessor<AppUser>(
                    wb.getSheetAt(0).getRow(0), AppUser.class, columns) {
            });
            srv.setOverWriteData(true);
            srv.setCheckBeforeErrors(conRevision);
            if (conRevision) {
                srv.checkValidation(null);
                assertEquals(1, srv.getRowsValidCount(), () -> "la revisión previa da la fila por válida: "
                        + srv.cancelled + " " + srv.getDataRowsError());
                assertEquals(0, srv.getRowsErrorCount());
            }
            srv.importData();

            assertEquals(1, srv.getRowsMigratedCount(), srv.getResultLog());
            assertEquals(0, srv.getRowsErrorCount(), srv.getResultLog());
            assertFalse(srv.getResultLog().contains("Errores de migración"), srv.getResultLog());
            assertEquals(1, srv.checkDataRowCalls,
                    "checkDataRow corre una sola vez por fila: en la revisión o en la grabación");
            return srv;
        }
    }

    /** Los {@code @NotNull} fuera de la planilla, completados como lo hace {@code checkDataRow}. */
    private static void assertDefaultsFueraDeLaPlanilla(Obligatorios grabada) {
        assertEquals(Boolean.FALSE, grabada.getInactivo());
        assertEquals(Integer.valueOf(0), grabada.getDiasAtraso());
        assertEquals(java.math.BigDecimal.ZERO, grabada.getSaldo());
        assertEquals(Long.valueOf(0L), grabada.getLimite());
        assertNull(grabada.getObservacion(), "checkDataRow no completa los textos");
        assertNull(grabada.getIdpadre(), "@CheckForeignkey(check = false) saltea el valor por defecto");
    }

    /**
     * E4-23, alta con revisión previa: el paso 3 vuelve a completar los
     * valores por defecto de los {@code @NotNull} que no son columnas (antes
     * llegaban nulos a la grabación y la fila se rechazaba), sin repetir
     * {@code checkDataRow}.
     */
    @Test
    public void testE423AltaConRevisionCompletaLosDefaultsFueraDeLaPlanilla() throws Exception {
        NotNullDefaultsSrv srv = importarObligatorios("u2", null, true, true, false);
        Obligatorios alta = srv.saved.get(0);
        assertNull(alta.getIduser(), "es un alta");
        assertEquals("Nombre nuevo", alta.getFullName());
        assertEquals(Short.valueOf((short) 5), alta.getType(), "la columna con defaultWhenBlank graba su default");
        assertDefaultsFueraDeLaPlanilla(alta);
    }

    /** E4-23, alta sin revisión previa: el mismo resultado (lo completa checkDataRow). */
    @Test
    public void testE423AltaSinRevisionCompletaLosDefaultsFueraDeLaPlanilla() throws Exception {
        NotNullDefaultsSrv srv = importarObligatorios("u2", null, false, true, false);
        Obligatorios alta = srv.saved.get(0);
        assertNull(alta.getIduser(), "es un alta");
        assertEquals(Short.valueOf((short) 5), alta.getType());
        assertDefaultsFueraDeLaPlanilla(alta);
    }

    /**
     * E4-23, «sobreescribir» con revisión previa: el existente con NULL en
     * los {@code @NotNull} fuera de la planilla se graba con sus valores por
     * defecto, lo que ya tenía se conserva ({@code diasAtraso} 7) y la celda
     * vacía de la columna con {@code defaultWhenBlank} conserva lo guardado
     * ({@code type} 3, D59).
     */
    @Test
    public void testE423SobreescribirConRevisionCompletaLosDefaultsYRespetaD59() throws Exception {
        NotNullDefaultsSrv srv = importarObligatorios("u1", null, true, true, false);
        Obligatorios existente = srv.saved.get(0);
        assertEquals(100L, existente.getIduser(), "es el existente");
        assertEquals(IDataRow.UPDATE, existente.getAction());
        assertEquals("Nombre nuevo", existente.getFullName());
        assertEquals(Short.valueOf((short) 3), existente.getType(),
                "la celda vacía con defaultWhenBlank conserva lo guardado (D59)");
        assertEquals(Integer.valueOf(7), existente.getDiasAtraso(), "lo que tenía el existente no se toca");
        assertEquals(Boolean.FALSE, existente.getInactivo());
        assertEquals(java.math.BigDecimal.ZERO, existente.getSaldo());
        assertEquals(Long.valueOf(0L), existente.getLimite());
        assertNull(existente.getIdpadre());
    }

    /** E4-23, «sobreescribir» sin revisión previa: el mismo resultado. */
    @Test
    public void testE423SobreescribirSinRevisionCompletaLosDefaultsYRespetaD59() throws Exception {
        NotNullDefaultsSrv srv = importarObligatorios("u1", null, false, true, false);
        Obligatorios existente = srv.saved.get(0);
        assertEquals(100L, existente.getIduser(), "es el existente");
        assertEquals(Short.valueOf((short) 3), existente.getType(), "D59");
        assertEquals(Integer.valueOf(7), existente.getDiasAtraso());
        assertEquals(Boolean.FALSE, existente.getInactivo());
        assertEquals(java.math.BigDecimal.ZERO, existente.getSaldo());
        assertEquals(Long.valueOf(0L), existente.getLimite());
    }

    /**
     * E4-23 no toca las columnas de la planilla: con revisión previa, una
     * columna {@code @NotNull} con la celda vacía y sin valor por defecto
     * sigue llegando nula a la grabación, como antes (en la aplicación la
     * rechaza el Bean Validation de la revisión, E4-21); solo se completan
     * los atributos que no son columnas.
     */
    @Test
    public void testE423ConRevisionNoTocaLasColumnasDeLaPlanilla() throws Exception {
        NotNullDefaultsSrv srv = importarObligatorios("u2", null, true, false, false);
        Obligatorios alta = srv.saved.get(0);
        assertNull(alta.getType(), "la columna de la planilla no se completa en el paso 3");
        assertDefaultsFueraDeLaPlanilla(alta);
    }

    /**
     * E4-23 respeta {@code FOREIGNKEY_NOCHECK}: si un gancho la pone en la
     * entidad, {@code checkDataRow} no completa los valores por defecto y el
     * paso 3 de la revisión previa tampoco; con y sin revisión queda igual.
     */
    @Test
    public void testE423RespetaForeignKeyNoCheck() throws Exception {
        for (boolean conRevision : new boolean[]{true, false}) {
            NotNullDefaultsSrv srv = importarObligatorios("u2", null, conRevision, true, true);
            Obligatorios alta = srv.saved.get(0);
            assertNull(alta.getInactivo(), "conRevision=" + conRevision);
            assertNull(alta.getDiasAtraso(), "conRevision=" + conRevision);
            assertNull(alta.getSaldo(), "conRevision=" + conRevision);
            assertNull(alta.getLimite(), "conRevision=" + conRevision);
        }
    }


    // ---- Sobreescribir sobre un NULL obligatorio con la celda vacía (XLSEXP, D61, E4-24) ----

    /**
     * E4-24 con revisión previa: el existente tiene NULL en {@code type}
     * ({@code @NotNull}), la celda viene vacía y la columna declara
     * {@code defaultWhenBlank}. La revisión (con Bean Validation) ya no
     * rechaza la fila y se graba el valor por defecto 5 (D61, excepción a
     * D59), sin errores en el log.
     */
    @Test
    public void testE424SobreescribirNullObligatorioConRevisionGrabaElDefault() throws Exception {
        NotNullDefaultsSrv srv = importarObligatorios("u1", null, true, true, false, null, true);
        Obligatorios existente = srv.saved.get(0);
        assertEquals(100L, existente.getIduser(), "es el existente");
        assertEquals(Short.valueOf((short) 5), existente.getType(),
                "sobre el NULL obligatorio se graba el default de la columna (D61)");
        assertTrue(srv.validator.properties.contains("type"), "Bean Validation corrió sobre la columna");
    }

    /**
     * E4-24 sin revisión previa: el mismo caso graba el mismo valor por
     * defecto de la columna (5), y no el 0 que pondría {@code checkDataRow}.
     */
    @Test
    public void testE424SobreescribirNullObligatorioSinRevisionGrabaElDefault() throws Exception {
        NotNullDefaultsSrv srv = importarObligatorios("u1", null, false, true, false, null, true);
        Obligatorios existente = srv.saved.get(0);
        assertEquals(100L, existente.getIduser(), "es el existente");
        assertEquals(Short.valueOf((short) 5), existente.getType(), "D61, igual que con revisión");
    }

    /**
     * E4-24 no cambia D59: con lo guardado no nulo, la celda vacía con
     * {@code defaultWhenBlank} conserva lo guardado, con revisión previa (y
     * Bean Validation) y sin ella.
     */
    @Test
    public void testE424SobreescribirConLoGuardadoNoNuloConservaD59() throws Exception {
        for (boolean conRevision : new boolean[]{true, false}) {
            NotNullDefaultsSrv srv = importarObligatorios("u1", null, conRevision, true, false, (short) 3, true);
            Obligatorios existente = srv.saved.get(0);
            assertEquals(100L, existente.getIduser(), "conRevision=" + conRevision);
            assertEquals(Short.valueOf((short) 3), existente.getType(),
                    "lo guardado no nulo se conserva (D59), conRevision=" + conRevision);
        }
    }

    /**
     * E4-24 en un alta con Bean Validation: la celda vacía con
     * {@code defaultWhenBlank} graba su default, como antes.
     */
    @Test
    public void testE424AltaConBeanValidationGrabaElDefault() throws Exception {
        for (boolean conRevision : new boolean[]{true, false}) {
            NotNullDefaultsSrv srv = importarObligatorios("u2", null, conRevision, true, false, null, true);
            Obligatorios alta = srv.saved.get(0);
            assertNull(alta.getIduser(), "es un alta, conRevision=" + conRevision);
            assertEquals(Short.valueOf((short) 5), alta.getType(), "conRevision=" + conRevision);
        }
    }

    /**
     * E4-24 vale solo para la celda vacía: una celda con valor pisa el NULL
     * del existente como siempre.
     */
    @Test
    public void testE424SobreescribirNullObligatorioConValorGrabaElValor() throws Exception {
        NotNullDefaultsSrv srv = importarObligatorios("u1", "8", true, true, false, null, true);
        assertEquals(Short.valueOf((short) 8), srv.saved.get(0).getType());
    }

    // ---- Default sobre un NULL obligatorio: columna ausente y noOverwrite (XLSEXP, D63) ----

    /**
     * Importa {@code u1} (existente, «sobreescribir») en {@link Obligatorios}
     * con la columna {@code type} declarada con valor por defecto 5 y
     * {@code defaultWhenBlank}; si {@code typeEnPlanilla} es {@code false} la
     * columna no viene en la planilla, y si {@code noOverwrite} se declara
     * de solo alta. Controla que no haya errores ni en la revisión ni en el
     * log y devuelve la entidad grabada.
     *
     * @param type valor de la celda ({@code null} = vacía).
     * @param typeOfU1 lo guardado en el existente ({@code null} = NULL).
     */
    private Obligatorios importarD63(String type, boolean conRevision, boolean typeEnPlanilla,
            boolean noOverwrite, Short typeOfU1, boolean conBeanValidation) throws Exception {
        NotNullDefaultsSrv srv = new NotNullDefaultsSrv();
        srv.typeOfU1 = typeOfU1;
        srv.validator = conBeanValidation ? new FakeValidator() : null;
        String[] cabeceras = typeEnPlanilla ? new String[]{"code", "fullName", "type"} : new String[]{"code", "fullName"};
        String[] fila = typeEnPlanilla ? new String[]{"u1", "Nombre nuevo", type} : new String[]{"u1", "Nombre nuevo"};
        try (Workbook wb = buildWorkbook(cabeceras, fila)) {
            srv.setExcelWorkbook(wb);
            ExcelColumns columns = new ExcelColumns();
            columns.add("code", "code").required();
            columns.add("fullName", "fullName");
            ExcelColumnSpec.Builder typeSpec = columns.add("type", "type").defaultValue("5").defaultWhenBlank();
            if (noOverwrite) {
                typeSpec.noOverwrite();
            }
            srv.setExcelRowProcessor(new ExcelRowProcessor<AppUser>(
                    wb.getSheetAt(0).getRow(0), AppUser.class, columns) {
            });
            srv.setOverWriteData(true);
            srv.setCheckBeforeErrors(conRevision);
            String contexto = "conRevision=" + conRevision + " typeEnPlanilla=" + typeEnPlanilla
                    + " noOverwrite=" + noOverwrite + " typeOfU1=" + typeOfU1;
            if (conRevision) {
                srv.checkValidation(null);
                assertEquals(1, srv.getRowsValidCount(), () -> contexto + ": la revisión previa da la fila por válida: "
                        + srv.cancelled + " " + srv.getDataRowsError());
            }
            srv.importData();
            assertEquals(1, srv.getRowsMigratedCount(), contexto + "\n" + srv.getResultLog());
            assertEquals(0, srv.getRowsErrorCount(), contexto + "\n" + srv.getResultLog());
            assertFalse(srv.getResultLog().contains("Errores de migración"), contexto + "\n" + srv.getResultLog());
            Obligatorios grabada = srv.saved.get(0);
            assertEquals(100L, grabada.getIduser(), contexto + ": es el existente");
            return grabada;
        }
    }

    /**
     * D63 (1): la columna con valor por defecto no viene en la planilla y lo
     * guardado es NULL en un {@code @NotNull}: se graba el default, con y sin
     * revisión previa (antes la revisión la rechazaba y sin revisión se
     * grababa el 0 de {@code checkDataRow}).
     */
    @Test
    public void testD63ColumnaAusenteSobreNullObligatorioGrabaElDefault() throws Exception {
        for (boolean conRevision : new boolean[]{true, false}) {
            Obligatorios grabada = importarD63(null, conRevision, false, false, null, true);
            assertEquals(Short.valueOf((short) 5), grabada.getType(), "conRevision=" + conRevision);
        }
    }

    /** D63 (1) no cambia la columna ausente con lo guardado no nulo: se conserva. */
    @Test
    public void testD63ColumnaAusenteConLoGuardadoNoNuloConserva() throws Exception {
        for (boolean conRevision : new boolean[]{true, false}) {
            Obligatorios grabada = importarD63(null, conRevision, false, false, (short) 3, true);
            assertEquals(Short.valueOf((short) 3), grabada.getType(), "conRevision=" + conRevision);
        }
    }

    /**
     * D63 (2): columna {@code noOverwrite()} con la celda vacía y lo guardado
     * NULL en un {@code @NotNull}: se graba el default, con y sin revisión
     * previa (excepción a «no sobreescribir» solo para el NULL inválido).
     */
    @Test
    public void testD63NoOverwriteCeldaVaciaSobreNullObligatorioGrabaElDefault() throws Exception {
        for (boolean conRevision : new boolean[]{true, false}) {
            Obligatorios grabada = importarD63(null, conRevision, true, true, null, true);
            assertEquals(Short.valueOf((short) 5), grabada.getType(), "conRevision=" + conRevision);
        }
    }

    /**
     * D63 (2) no cambia {@code noOverwrite()} en lo demás: con lo guardado no
     * nulo se conserva, venga la celda vacía o con valor.
     */
    @Test
    public void testD63NoOverwriteConLoGuardadoNoNuloConserva() throws Exception {
        for (boolean conRevision : new boolean[]{true, false}) {
            for (String celda : new String[]{null, "8"}) {
                Obligatorios grabada = importarD63(celda, conRevision, true, true, (short) 3, true);
                assertEquals(Short.valueOf((short) 3), grabada.getType(),
                        "conRevision=" + conRevision + " celda=" + celda);
            }
        }
    }

    /**
     * D63 (2) vale solo para la celda vacía: con valor en la celda,
     * {@code noOverwrite()} sigue sin copiarlo aunque lo guardado sea NULL
     * (sin Bean Validation, para que la revisión no lo rechace antes).
     */
    @Test
    public void testD63NoOverwriteConValorSobreNullNoCopiaElValor() throws Exception {
        for (boolean conRevision : new boolean[]{true, false}) {
            Obligatorios grabada = importarD63("8", conRevision, true, true, null, false);
            assertNotEquals(Short.valueOf((short) 8), grabada.getType(), "conRevision=" + conRevision);
        }
    }

    // ---- Celda numérica en una columna lógica (XLSEXP, D62, E4-25) ----

    /**
     * Planilla con {@code code} (texto) y {@code disabled} como celda
     * numérica: u2 = 1, u3 = 0, u4 = 2, u5 = 1.0, u6 = 0.0, u7 = 0.5.
     */
    private Workbook buildWorkbookLogicoNumerico() {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("data");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("code");
        header.createCell(1).setCellValue("disabled");
        Object[][] filas = {{"u2", 1d}, {"u3", 0d}, {"u4", 2d}, {"u5", 1.0d}, {"u6", 0.0d}, {"u7", 0.5d}};
        for (int i = 0; i < filas.length; i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue((String) filas[i][0]);
            row.createCell(1).setCellValue((Double) filas[i][1]);
        }
        return wb;
    }

    /**
     * E4-25: con y sin revisión previa, el 1 numérico se graba como
     * {@code true} y el 0 como {@code false} (también 1.0 y 0.0); el 2 y el
     * 0.5 son errores de la fila (número 50000, nombrando la columna), la
     * revisión los informa en el paso 2 sin cortar la importación, no se
     * graban, y cada motivo aparece una sola vez en el log.
     */
    @Test
    public void testE425LogicoDesdeCeldaNumerica() throws Exception {
        for (boolean conRevision : new boolean[]{true, false}) {
            ExistingRowSrv srv = new ExistingRowSrv();
            try (Workbook wb = buildWorkbookLogicoNumerico()) {
                srv.setExcelWorkbook(wb);
                ExcelColumns columns = new ExcelColumns();
                columns.add("code", "code").required();
                columns.add("disabled", "disabled");
                srv.setExcelRowProcessor(new ExcelRowProcessor<AppUser>(
                        wb.getSheetAt(0).getRow(0), AppUser.class, columns) {
                });
                srv.setCheckBeforeErrors(conRevision);
                String msg2 = "La columna «disabled» no es válida: El valor numérico 2 no es convertible a lógico (se admite 1 o 0)";
                String msg05 = "La columna «disabled» no es válida: El valor numérico 0.5 no es convertible a lógico (se admite 1 o 0)";
                if (conRevision) {
                    srv.checkValidation(null);
                    assertTrue(srv.getErrorsReviewed(), "la revisión no se corta");
                    assertEquals(4, srv.getRowsValidCount());
                    assertEquals(2, srv.getRowsErrorCount());
                    org.javabeanstack.error.IErrorInfo error = srv.getDataRowsError().get(0).getErrors().get("disabled");
                    assertNotNull(error, String.valueOf(srv.getDataRowsError().get(0).getErrors()));
                    assertEquals(Integer.valueOf(50000), error.getErrorNumber());
                    assertEquals(msg2, error.getMessage());
                }
                srv.importData();

                String contexto = "conRevision=" + conRevision + "\n" + srv.getResultLog();
                assertEquals(4, srv.getRowsMigratedCount(), contexto);
                assertEquals(2, srv.getRowsErrorCount(), contexto);
                Map<String, Boolean> grabados = new HashMap<>();
                for (IDataRow row : srv.updated) {
                    grabados.put(((AppUser) row).getCode(), ((AppUser) row).getDisabled());
                }
                assertEquals(Map.of("u2", true, "u3", false, "u5", true, "u6", false), grabados, contexto);
                assertEquals(1, srv.getResultLog().split(java.util.regex.Pattern.quote(msg2), -1).length - 1, contexto);
                assertEquals(1, srv.getResultLog().split(java.util.regex.Pattern.quote(msg05), -1).length - 1, contexto);
            }
        }
    }

    /**
     * Una columna declarada {@code noOverwrite()} se graba en las altas pero,
     * con «sobreescribir», no pisa el valor del registro existente aunque la
     * planilla traiga otro (pedido del usuario, 2026-09-16). Las demás columnas
     * presentes sí actualizan.
     */
    @Test
    public void testColumnaNoOverwriteNoPisaAlExistente() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        try (Workbook wb = buildWorkbook()) {
            srv.setExcelWorkbook(wb);
            ExcelColumns columns = new ExcelColumns();
            columns.add("code", "code").required();
            columns.add("fullName", "fullName").noOverwrite();
            srv.setExcelRowProcessor(new ExcelRowProcessor<AppUser>(
                    wb.getSheetAt(0).getRow(0), AppUser.class, columns) {
            });
            srv.setOverWriteData(true);

            srv.checkValidation(null);
            srv.importData();

            assertEquals(2, srv.getRowsMigratedCount());
            AppUser u1 = (AppUser) srv.updated.stream()
                    .filter(r -> "u1".equals(((AppUser) r).getCode())).findFirst().orElseThrow();
            assertEquals(IDataRow.UPDATE, u1.getAction());
            assertEquals("Nombre previo", u1.getFullName(),
                    "la planilla trae 'User One' pero la columna es noOverwrite");
            AppUser u2 = (AppUser) srv.updated.stream()
                    .filter(r -> "u2".equals(((AppUser) r).getCode())).findFirst().orElseThrow();
            assertEquals(IDataRow.INSERT, u2.getAction());
            assertEquals("User Two", u2.getFullName(), "en el alta sí se graba");
        }
    }

    /**
     * Con «sobreescribir», la colección hija del registro existente (una
     * {@code @OneToMany} que la entidad inicializa con una lista vacía, no
     * nula) <b>no</b> se pisa con la lista vacía de la entidad convertida
     * desde la planilla: {@code copyTo(existing, true)} copia todo lo no nulo,
     * y una lista vacía no es nula. Sin este resguardo el servicio de datos
     * de la entidad perdía los hijos cargados y los reinsertaba con la misma
     * clave (hallazgo E2-01 del plan XLSXML, {@code gi_fraccion_extension}).
     * Los atributos simples siguen actualizándose.
     */
    @Test
    public void testColeccionHijaDelExistenteNoSePisaConSobreescritura() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        srv.memberOfU1 = stub(IAppUserMember.class);
        try (Workbook wb = buildWorkbook()) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.setOverWriteData(true);

            srv.checkValidation(null);
            srv.importData();

            assertEquals(2, srv.getRowsMigratedCount());
            AppUser u1 = (AppUser) srv.updated.stream()
                    .filter(r -> "u1".equals(((AppUser) r).getCode())).findFirst().orElseThrow();
            assertEquals(100L, u1.getIduser(), "se graba el registro existente");
            assertEquals("User One", u1.getFullName(), "lo que trae la planilla sí pisa");
            assertNotNull(u1.getUserMemberList(), "la colección del existente no se anula");
            assertEquals(1, u1.getUserMemberList().size(),
                    "la lista vacía de la entidad convertida no pisa a la del existente");
            assertSame(srv.memberOfU1, u1.getUserMemberList().get(0));
            //En el alta (u2) la colección inicializada de la entidad se conserva.
            AppUser u2 = (AppUser) srv.updated.stream()
                    .filter(r -> "u2".equals(((AppUser) r).getCode())).findFirst().orElseThrow();
            assertNotNull(u2.getUserMemberList(), "en un alta la colección no se toca");
            assertTrue(u2.getUserMemberList().isEmpty());
        }
    }

    /**
     * Si la vista ya resolvió el identificador, no se consulta la clave única:
     * se carga el registro por id y se actualiza (con sobreescritura) sobre él,
     * conservando lo que la planilla no trae; sin sobreescritura se devuelve el
     * existente tal cual, con acción UPDATE, para que el llamador lo cuente.
     */
    @Test
    public void testResolveExistingRowPrefersResolvedId() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        AppUser withId = new AppUser();
        withId.setIduser(7L);
        withId.setCode("u7");
        withId.setFullName("Nombre nuevo");

        srv.setOverWriteData(true);
        IDataRow result = srv.resolveExistingRow("s", withId);
        assertNotSame(withId, result, "se trabaja sobre el registro cargado por id");
        assertEquals(IDataRow.UPDATE, result.getAction());
        assertEquals(7L, ((AppUser) result).getIduser());
        assertEquals("Nombre nuevo", ((AppUser) result).getFullName());
        assertEquals(1, srv.lookups, "una sola búsqueda, por id");

        //Id que no existe en la base: se conserva el convertido con su id.
        AppUser huerfano = new AppUser();
        huerfano.setIduser(99L);
        result = srv.resolveExistingRow("s", huerfano);
        assertSame(huerfano, result);
        assertEquals(IDataRow.UPDATE, result.getAction());
    }

    /**
     * Entidad convertida cuya colección hija no admite anularse: hace fallar la
     * fase de copia de {@code resolveExistingRow} (E5-02).
     */
    public static class AppUserConColeccionQueLanza extends AppUser {

        @jakarta.persistence.OneToMany
        private List<String> hijos = new ArrayList<>();

        public List<String> getHijos() {
            return hijos;
        }

        public void setHijos(List<String> hijos) {
            throw new IllegalStateException("setter que lanza");
        }
    }

    /**
     * E5-02 (revisión final de XLSXML): la fase de copia sobre el existente no
     * está protegida por el {@code catch} de la búsqueda. Si la copia falla, la
     * excepción se propaga (y la importación aborta de forma ruidosa) en vez de
     * devolver la entidad convertida —ya con defaults y colecciones anulados—
     * como una actualización silenciosa. La búsqueda que falla, en cambio,
     * sigue siendo tolerante: se sigue como alta.
     */
    @Test
    public void testFalloEnLaCopiaSobreElExistenteSePropaga() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        srv.setOverWriteData(true);

        //Búsqueda que falla: se registra y se sigue como alta (tolerante).
        AppUser sinUk = new AppUser();
        sinUk.setCode("boom");
        IDataRow result = srv.resolveExistingRow("s", sinUk);
        assertSame(sinUk, result);
        assertEquals(IDataRow.INSERT, result.getAction());
        assertTrue(srv.getResultLog().contains("No se pudo buscar el registro existente"));

        //Copia que falla (la anulación de la colección hija lanza): se
        //propaga, no se devuelve el convertido como UPDATE.
        AppUser convertido = new AppUserConColeccionQueLanza();
        convertido.setCode("u1");
        convertido.setFullName("User One");
        Exception ex = assertThrows(Exception.class, () -> srv.resolveExistingRow("s", convertido));
        //DataInfo.setFieldValue traga la excepción del setter y devuelve false;
        //DataRow.setValue la convierte en FieldException nombrando el campo.
        assertTrue(String.valueOf(ex.getMessage()).contains("hijos"), String.valueOf(ex));
        assertFalse(srv.getResultLog().contains("No se pudo buscar el registro existente (AppUser)")
                && srv.getResultLog().indexOf("No se pudo buscar") != srv.getResultLog().lastIndexOf("No se pudo buscar"),
                "el fallo de la copia no se registra como fallo de búsqueda");
        //Lo que discrimina es el assertThrows: sin la corrección, el catch devolvía el
        //convertido con UPDATE en vez de lanzar. Esta aserción solo documenta el estado.
        assertNotEquals(IDataRow.UPDATE, convertido.getAction(), "el convertido no queda como UPDATE");
    }

    /**
     * Cierre con error cuando una fila se descarta <b>durante la grabación</b>:
     * antes, {@code finishWithError()} solo se disparaba por la validación de
     * {@code checkDataRow}; una fila que {@code onBeforeRowConvert} manda a la
     * lista de errores, o un {@code save} fallido, terminaban en
     * {@code finish()} aunque el proceso tuviera errores (observación de
     * XLSGEN §6.2). También se prueba que una fila que la subclase descarta
     * SIN marcarla como error sigue cerrando con {@code finish()}.
     */
    @Test
    public void testFilaDescartadaEnLaGrabacionCierraConError() throws Exception {
        //(1) onBeforeRowConvert descarta u1 y la agrega a la lista de errores.
        class DescartaSrv extends ExistingRowSrv {
            boolean conError = false;
            boolean marcar = true;
            @Override
            protected boolean onBeforeRowConvert(AppUser rowView) {
                if ("u1".equals(rowView.getCode())) {
                    if (marcar) {
                        rowView.setErrors("sin receptor", "code", 50000);
                        getDataRowsError().add(rowView);
                    }
                    return false;
                }
                return true;
            }
            @Override
            protected void finishWithError() {
                conError = true;
            }
            @Override
            protected void finish() {
                conError = false;
            }
        }
        DescartaSrv srv = new DescartaSrv();
        try (Workbook wb = buildWorkbook()) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.importData();
            assertTrue(srv.getImportOk());
            assertEquals(1, srv.getRowsMigratedCount(), "u2 se graba");
            assertEquals(1, srv.getRowsErrorCount(), "u1 queda en la lista de errores");
            assertTrue(srv.conError, "el proceso cierra con finishWithError()");
        }
        //(2) La misma fila descartada sin marcarla: no es error del proceso.
        DescartaSrv silencioso = new DescartaSrv();
        silencioso.marcar = false;
        silencioso.conError = true;
        try (Workbook wb = buildWorkbook()) {
            silencioso.setExcelWorkbook(wb);
            silencioso.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            silencioso.importData();
            assertEquals(0, silencioso.getRowsErrorCount());
            assertFalse(silencioso.conError, "sin filas con error cierra con finish()");
        }
    }

    /**
     * M6-02 (XLSGEN): el servicio vive dentro de beans de vista pasivables;
     * el libro de POI y el procesador no son serializables y van
     * {@code transient}. Una serialización Java del servicio con la corrida en
     * curso no debe fallar, y al volver los dos campos quedan nulos mientras
     * el resto del estado (contadores, log) se conserva.
     */
    @Test
    public void testSerializableConLibroYProcesadorTransient() throws Exception {
        ExistingRowSrv srv = new ExistingRowSrv();
        try (Workbook wb = buildWorkbook()) {
            srv.setExcelWorkbook(wb);
            srv.setExcelRowProcessor(processor(wb.getSheetAt(0).getRow(0)));
            srv.importData();
            assertNotNull(srv.getExcelWorkbook());
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(bytes)) {
                out.writeObject(srv);
            }
            Object copia;
            try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(
                    new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
                copia = in.readObject();
            }
            ExistingRowSrv vuelto = (ExistingRowSrv) copia;
            assertNull(vuelto.getExcelWorkbook(), "el libro no viaja");
            assertNull(vuelto.getExcelRowProcessor(), "el procesador no viaja");
            assertEquals(srv.getRowsMigratedCount(), vuelto.getRowsMigratedCount());
            assertEquals(srv.getResultLog(), vuelto.getResultLog());
        }
    }

    /**
     * Subclase que registra el valor de {@code getErrorsReviewed()} visto por
     * {@code onBeforeRowConvert} en cada fila, con servicio de datos y sesión
     * simulados vía {@link Proxy} (lo mínimo que exige el flujo de
     * {@code importData()}).
     */
    static class PhaseRecordingSrv extends ExcelImportSrvTest01 {

        final List<Boolean> hookPhases = new ArrayList<>();

        @Override
        protected IDataService getDataService() {
            return stub(IDataService.class);
        }

        @Override
        protected IUserSession getUserSession() {
            return stub(IUserSession.class);
        }

        @Override
        protected Class<? extends IDataRow> getTargetType() {
            return AppUser.class;
        }

        @Override
        protected boolean onBeforeRowConvert(AppUser rowView) {
            hookPhases.add(getErrorsReviewed());
            return true;
        }
    }

    /**
     * Crea un stub dinámico de la interfaz indicada: {@code copyTo} retorna la
     * entidad destino, {@code checkDataRow} no reporta errores,
     * {@code isSuccessFul} da éxito y el resto responde valores por defecto
     * (otro stub si retorna una interfaz, vacío/cero en tipos simples).
     */
    @SuppressWarnings("unchecked")
    private static <X> X stub(Class<X> intf) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                switch (method.getName()) {
                    case "getSessionId":
                        return "test-session";
                    case "copyTo":
                        return args[2];
                    case "checkDataRow":
                        return new HashMap<>();
                    case "findByUk":
                    case "findById":
                        //Sin registro existente (el proxy genérico devolvería otro
                        //stub y toda fila parecería existente).
                        return null;
                    case "isSuccessFul":
                        return true;
                    default:
                        break;
                }
                Class<?> type = method.getReturnType();
                if (type.isInterface()) {
                    return stub(type);
                }
                if (type == boolean.class || type == Boolean.class) {
                    return false;
                }
                if (type == long.class || type == Long.class) {
                    return 0L;
                }
                if (type == int.class || type == Integer.class) {
                    return 0;
                }
                if (type == String.class) {
                    return "";
                }
                return null;
            }
        };
        return (X) Proxy.newProxyInstance(intf.getClassLoader(),
                new Class[]{intf}, handler);
    }

    /**
     * Subclase concreta mínima de {@link ExcelImportSrv} para las pruebas.
     * Debe ser {@code static} para poder ser extendida por la clase anidada
     * estática {@link TestImportSrv}. El servicio de datos no se utiliza en
     * estas pruebas (solo se ejercita lectura/validación), por lo que
     * {@link #getDataService()} no está soportado.
     */
    static class ExcelImportSrvTest01 extends ExcelImportSrv<AppUser> {
        @Override
        protected IDataService getDataService() {
            throw new UnsupportedOperationException("getDataService no se usa en estas pruebas");
        }

        @Override
        protected IUserSession getUserSession() {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        protected String getSourceFileName() {
            return "";
        }

        @Override
        protected Class<? extends IDataRow> getTargetType() {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }
    }
}

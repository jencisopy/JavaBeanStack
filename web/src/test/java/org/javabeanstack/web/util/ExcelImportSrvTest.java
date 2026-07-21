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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.data.services.IDataService;
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
            //Solo se ejercita la conversión: la validación de negocio requiere
            //sesión y servicio de datos, que este stub no provee.
            srv.setCheckBeforeErrors(false);

            List<AppUser> data = srv.readSheet(null);

            assertEquals(2, data.size());
            assertEquals("u1", data.get(0).getCode());
            assertEquals("User One", data.get(0).getFullName());
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
        protected Class<? extends IDataRow> getTargetType() {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        protected String getSourceFileName() {
            return "";
        }
    }
}

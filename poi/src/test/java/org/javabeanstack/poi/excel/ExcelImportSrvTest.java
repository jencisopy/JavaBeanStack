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
     * previos), y registra las entidades que recibe {@code update}.
     */
    static class ExistingRowSrv extends ExcelImportSrvTest01 {

        final List<IDataRow> updated = new ArrayList<>();
        int lookups = 0;

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
                            if ("u1".equals(probe.getCode())) {
                                AppUser existing = new AppUser();
                                existing.setIduser(100L);
                                existing.setCode("u1");
                                existing.setFullName("Nombre previo");
                                existing.setEmail1("previo@dominio.com");
                                return existing;
                            }
                            return null;
                        case "update":
                            updated.add((IDataRow) args[1]);
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

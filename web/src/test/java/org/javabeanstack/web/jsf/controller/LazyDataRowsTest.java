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
package org.javabeanstack.web.jsf.controller;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.Id;

import org.primefaces.model.FilterMeta;

import org.javabeanstack.data.DataRow;
import org.javabeanstack.data.IDataLink;
import org.javabeanstack.events.IAppSystemEvents;
import org.javabeanstack.util.LocalDates;
import org.javabeanstack.web.model.ColumnModel;
import org.javabeanstack.web.model.IColumnModel;
import org.javabeanstack.web.util.AppResourceSearcher;
import org.javabeanstack.datactrl.uicomponents.IDatatable;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias offline de LazyDataRows: traducción de los filtros del
 * dataTable (Map de FilterMeta de PrimeFaces 15) a parámetros tipados y a la
 * expresión WHERE, conversión del rowKey y contrato de count(). Los metodos
 * privados getParams/getFilterExpression se invocan por reflexión; no se
 * ejercita load() porque requiere un FacesContext activo (se valida en el
 * ciclo funcional JSF).
 *
 * @author Jorge Enciso
 */
public class LazyDataRowsTest {

    public LazyDataRowsTest() {
    }

    /**
     * Entidad de prueba con un campo por cada tipo de dato que convierte
     * getParams.
     */
    public static class ArticuloTest extends DataRow {
        @Id
        private Long idarticulo;
        private String nombre;
        private Long codigo;
        private Integer cantidad;
        private Short deposito;
        private BigDecimal precio;
        private LocalDateTime fechacreacion;
        private Date fechaalta;
        private Boolean activo;
        private RubroTest rubro;

        public Long getIdarticulo() {
            return idarticulo;
        }

        public void setIdarticulo(Long idarticulo) {
            this.idarticulo = idarticulo;
        }
    }

    /**
     * Entidad relacionada para probar filtros con campos anidados
     * (rubro.nombre).
     */
    public static class RubroTest extends DataRow {
        @Id
        private Long idrubro;
        private String nombre;
    }

    /**
     * Controller minimo sin servidor: solo aporta el tipo de entidad y la
     * definición de columnas del dataTable.
     */
    static class ControllerStub extends AbstractDataController<ArticuloTest> {
        private List<IColumnModel> columns;

        ControllerStub() {
            setType(ArticuloTest.class);
        }

        void setColumns(List<IColumnModel> columns) {
            this.columns = columns;
        }

        @Override
        protected AppResourceSearcher getAppResource() {
            return null;
        }

        @Override
        public IDatatable getDataTable() {
            if (columns == null) {
                return null;
            }
            InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
                if ("getColumns".equals(method.getName())) {
                    return columns;
                }
                return null;
            };
            return (IDatatable) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{IDatatable.class}, handler);
        }

        @Override
        public void configDataTables() {
        }

        @Override
        public void configDataTables(IDatatable dataTable, String nodeName) {
        }

        @Override
        protected IAppSystemEvents getAppSysEvents() {
            return null;
        }

        @Override
        public IDataLink getDAO() {
            return null;
        }

        @Override
        public IDataLink getDAOCatalog() {
            return null;
        }
    }

    private ControllerStub context;

    private LazyDataRows<ArticuloTest> createLazyRows() {
        context = new ControllerStub();
        return new LazyDataRows<>(context);
    }

    private static FilterMeta filter(String field, Object value) {
        return FilterMeta.builder().field(field).filterValue(value).build();
    }

    private static Map<String, FilterMeta> filters(Object... pairs) {
        Map<String, FilterMeta> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], filter((String) pairs[i], pairs[i + 1]));
        }
        return map;
    }

    private static IColumnModel column(String name, String filterMode, String filterMask) {
        ColumnModel column = new ColumnModel();
        column.setName(name);
        column.setFilterMode(filterMode);
        column.setFilterMask(filterMask);
        return column;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeGetParams(LazyDataRows<ArticuloTest> lazyRows,
            Map<String, FilterMeta> filters) throws Exception {
        Method method = LazyDataRows.class.getDeclaredMethod("getParams", Map.class);
        method.setAccessible(true);
        return (Map<String, Object>) method.invoke(lazyRows, filters);
    }

    private String invokeGetFilterExpression(LazyDataRows<ArticuloTest> lazyRows,
            Map<String, FilterMeta> filters) throws Exception {
        Method method = LazyDataRows.class.getDeclaredMethod("getFilterExpression", Map.class);
        method.setAccessible(true);
        return (String) method.invoke(lazyRows, filters);
    }

    /**
     * Test of getParams: campo alfanumérico sin filterMode definido, debe
     * buscar el valor contenido (like %valor%) y recortar los espacios.
     */
    @Test
    public void testGetParamsStringContain() throws Exception {
        System.out.println("lazyDataRows getParamsStringContain");
        LazyDataRows<ArticuloTest> lazyRows = createLazyRows();
        Map<String, Object> params = invokeGetParams(lazyRows, filters("nombre", "  abc "));
        assertEquals("%abc%", params.get("nombre"));
    }

    /**
     * Test of getParams: modos de filtro por columna en campos alfanuméricos
     * (exact, contain_ltrim, contain_rtrim).
     */
    @Test
    public void testGetParamsStringFilterModes() throws Exception {
        System.out.println("lazyDataRows getParamsStringFilterModes");
        LazyDataRows<ArticuloTest> lazyRows = createLazyRows();

        context.setColumns(Arrays.asList(column("nombre", "exact", null)));
        assertEquals("abc", invokeGetParams(lazyRows, filters("nombre", "abc")).get("nombre"));

        context.setColumns(Arrays.asList(column("nombre", "contain_ltrim", null)));
        assertEquals("abc%", invokeGetParams(lazyRows, filters("nombre", "abc")).get("nombre"));

        context.setColumns(Arrays.asList(column("nombre", "contain_rtrim", null)));
        assertEquals("%abc", invokeGetParams(lazyRows, filters("nombre", "abc")).get("nombre"));
    }

    /**
     * Test of getParams: máscaras de búsqueda right_blank_N y replace sobre el
     * valor filtrado.
     */
    @Test
    public void testGetParamsFilterMask() throws Exception {
        System.out.println("lazyDataRows getParamsFilterMask");
        LazyDataRows<ArticuloTest> lazyRows = createLazyRows();

        context.setColumns(Arrays.asList(column("nombre", "exact", "right_blank_6")));
        assertEquals("ab    ", invokeGetParams(lazyRows, filters("nombre", "ab")).get("nombre"));

        context.setColumns(Arrays.asList(column("nombre", "exact", "replace('a','b')")));
        assertEquals("cbsb", invokeGetParams(lazyRows, filters("nombre", "casa")).get("nombre"));
    }

    /**
     * Test of getParams: los valores de filtro que llegan como texto deben
     * convertirse al tipo numérico de la columna (regresión PrimeFaces 15: el
     * valor se extrae del FilterMeta y recién ahí se convierte).
     */
    @Test
    public void testGetParamsNumericosDesdeString() throws Exception {
        System.out.println("lazyDataRows getParamsNumericosDesdeString");
        LazyDataRows<ArticuloTest> lazyRows = createLazyRows();
        Map<String, Object> params = invokeGetParams(lazyRows, filters(
                "codigo", " 150 ",
                "cantidad", "20",
                "deposito", "3",
                "precio", "10.50"));
        assertEquals(150L, params.get("codigo"));
        assertEquals(20, params.get("cantidad"));
        assertEquals((short) 3, params.get("deposito"));
        assertEquals(new BigDecimal("10.50"), params.get("precio"));
    }

    /**
     * Test of getParams: si el componente ya convirtió el valor al tipo de la
     * columna se pasa tal cual, sin reconversión.
     */
    @Test
    public void testGetParamsValorYaTipado() throws Exception {
        System.out.println("lazyDataRows getParamsValorYaTipado");
        LazyDataRows<ArticuloTest> lazyRows = createLazyRows();
        Map<String, Object> params = invokeGetParams(lazyRows, filters(
                "codigo", 150L,
                "precio", new BigDecimal("99.90")));
        assertEquals(150L, params.get("codigo"));
        assertEquals(new BigDecimal("99.90"), params.get("precio"));
    }

    /**
     * Test of getParams: conversión de fechas según el tipo del valor recibido
     * (LocalDate, Date o texto dd/MM/yyyy).
     */
    @Test
    public void testGetParamsFechas() throws Exception {
        System.out.println("lazyDataRows getParamsFechas");
        LazyDataRows<ArticuloTest> lazyRows = createLazyRows();

        //LocalDateTime desde LocalDate (calendario de PrimeFaces)
        Map<String, Object> params = invokeGetParams(lazyRows,
                filters("fechacreacion", LocalDate.of(2026, 3, 15)));
        assertEquals(LocalDateTime.of(2026, 3, 15, 0, 0), params.get("fechacreacion"));

        //LocalDateTime desde Date
        Date fecha = new SimpleDateFormat("dd/MM/yyyy").parse("15/03/2026");
        params = invokeGetParams(lazyRows, filters("fechacreacion", fecha));
        assertEquals(LocalDates.toDateTime(fecha), params.get("fechacreacion"));

        //LocalDateTime desde texto dd/MM/yyyy
        params = invokeGetParams(lazyRows, filters("fechacreacion", "15/03/2026"));
        assertEquals(LocalDateTime.of(2026, 3, 15, 0, 0), params.get("fechacreacion"));

        //Date desde texto dd/MM/yyyy
        params = invokeGetParams(lazyRows, filters("fechaalta", "15/03/2026"));
        assertEquals(fecha, params.get("fechaalta"));
    }

    /**
     * Test of getParams: conversión de valores lógicos.
     */
    @Test
    public void testGetParamsBoolean() throws Exception {
        System.out.println("lazyDataRows getParamsBoolean");
        LazyDataRows<ArticuloTest> lazyRows = createLazyRows();
        Map<String, Object> params = invokeGetParams(lazyRows, filters("activo", "true"));
        assertEquals(Boolean.TRUE, params.get("activo"));
    }

    /**
     * Test of getParams: el filtro global y los filtros sin valor no se
     * traducen a parámetros.
     */
    @Test
    public void testGetParamsFiltrosIgnorados() throws Exception {
        System.out.println("lazyDataRows getParamsFiltrosIgnorados");
        LazyDataRows<ArticuloTest> lazyRows = createLazyRows();
        Map<String, FilterMeta> filterBy = new LinkedHashMap<>();
        filterBy.put(FilterMeta.GLOBAL_FILTER_KEY,
                filter(FilterMeta.GLOBAL_FILTER_KEY, "algo"));
        filterBy.put("nombre", filter("nombre", null));
        filterBy.put("codigo", null);
        Map<String, Object> params = invokeGetParams(lazyRows, filterBy);
        assertTrue(params.isEmpty());
    }

    /**
     * Test of getParams: un campo que no existe en la entidad pasa el valor
     * sin convertir.
     */
    @Test
    public void testGetParamsCampoInexistente() throws Exception {
        System.out.println("lazyDataRows getParamsCampoInexistente");
        LazyDataRows<ArticuloTest> lazyRows = createLazyRows();
        Map<String, Object> params = invokeGetParams(lazyRows, filters("noexiste", "abc"));
        assertEquals("abc", params.get("noexiste"));
    }

    /**
     * Test of getParams: campo de una entidad relacionada (rubro.nombre); la
     * clave del parámetro se genera sin puntos.
     */
    @Test
    public void testGetParamsCampoRelacionado() throws Exception {
        System.out.println("lazyDataRows getParamsCampoRelacionado");
        LazyDataRows<ArticuloTest> lazyRows = createLazyRows();
        Map<String, Object> params = invokeGetParams(lazyRows, filters("rubro.nombre", "abc"));
        assertEquals("%abc%", params.get("rubronombre"));
    }

    /**
     * Test of getFilterExpression: condición like insensible a mayúsculas para
     * campos alfanuméricos sin filterMode e igualdad para los demás tipos,
     * unidas con and.
     */
    @Test
    public void testGetFilterExpression() throws Exception {
        System.out.println("lazyDataRows getFilterExpression");
        LazyDataRows<ArticuloTest> lazyRows = createLazyRows();
        String expr = invokeGetFilterExpression(lazyRows,
                filters("nombre", "abc", "codigo", "150"));
        assertEquals(" upper(o.nombre) like upper(:nombre) and  o.codigo = :codigo", expr);
    }

    /**
     * Test of getFilterExpression: variantes según el filterMode de la columna
     * alfanumérica.
     */
    @Test
    public void testGetFilterExpressionFilterModes() throws Exception {
        System.out.println("lazyDataRows getFilterExpressionFilterModes");
        LazyDataRows<ArticuloTest> lazyRows = createLazyRows();

        context.setColumns(Arrays.asList(column("nombre", "exact", null)));
        assertEquals(" o.nombre = :nombre",
                invokeGetFilterExpression(lazyRows, filters("nombre", "abc")));

        context.setColumns(Arrays.asList(column("nombre", "exact_trim", null)));
        assertEquals(" trim(o.nombre) = :nombre",
                invokeGetFilterExpression(lazyRows, filters("nombre", "abc")));

        context.setColumns(Arrays.asList(column("nombre", "exact_ltrim", null)));
        assertEquals(" ltrim(o.nombre) = :nombre",
                invokeGetFilterExpression(lazyRows, filters("nombre", "abc")));

        context.setColumns(Arrays.asList(column("nombre", "contain_ltrim", null)));
        assertEquals(" ltrim(upper(o.nombre)) like upper(:nombre)",
                invokeGetFilterExpression(lazyRows, filters("nombre", "abc")));

        context.setColumns(Arrays.asList(column("nombre", "contain_rtrim", null)));
        assertEquals(" rtrim(upper(o.nombre)) like upper(:nombre)",
                invokeGetFilterExpression(lazyRows, filters("nombre", "abc")));
    }

    /**
     * Test of getFilterExpression: si el controller implementa
     * onGetFilterString esa expresión tiene prioridad.
     */
    @Test
    public void testGetFilterExpressionPersonalizada() throws Exception {
        System.out.println("lazyDataRows getFilterExpressionPersonalizada");
        ControllerStub controller = new ControllerStub() {
            @Override
            public String onGetFilterString(Map<String, Object> filters) {
                return "o.activo = true";
            }
        };
        LazyDataRows<ArticuloTest> lazyRows = new LazyDataRows<>(controller);
        String expr = invokeGetFilterExpression(lazyRows, filters("nombre", "abc"));
        assertEquals("o.activo = true", expr);
    }

    /**
     * Test of count: en PrimeFaces 15 count() corre antes de load(); debe
     * devolver el último total conocido (fijado por setRowCount) y nunca
     * resetearlo.
     */
    @Test
    public void testCount() {
        System.out.println("lazyDataRows count");
        LazyDataRows<ArticuloTest> lazyRows = createLazyRows();
        lazyRows.setRowCount(42);
        assertEquals(42, lazyRows.count(new LinkedHashMap<>()));
        assertEquals(42, lazyRows.count(null));
    }

    /**
     * Test of getRowKey: formato {Tipo}valor generado por la entidad, vacío
     * para registros nulos.
     */
    @Test
    public void testGetRowKey() {
        System.out.println("lazyDataRows getRowKey");
        LazyDataRows<ArticuloTest> lazyRows = createLazyRows();
        assertEquals("", lazyRows.getRowKey(null));

        ArticuloTest row = new ArticuloTest();
        row.setIdarticulo(150L);
        assertEquals("{Long}150", lazyRows.getRowKey(row));
    }

    /**
     * Test of getRowData: debe ubicar el registro en la página cargada a
     * partir del rowKey con formato {Tipo}valor.
     */
    @Test
    public void testGetRowData() {
        System.out.println("lazyDataRows getRowData");
        ArticuloTest row1 = new ArticuloTest();
        row1.setIdarticulo(1L);
        ArticuloTest row2 = new ArticuloTest();
        row2.setIdarticulo(150L);
        List<ArticuloTest> rows = new ArrayList<>(Arrays.asList(row1, row2));

        LazyDataRows<ArticuloTest> lazyRows = new LazyDataRows<ArticuloTest>(new ControllerStub()) {
            @Override
            public List<ArticuloTest> getRows() {
                return rows;
            }
        };
        assertNull(lazyRows.getRowData(null));
        assertNull(lazyRows.getRowData(""));
        assertSame(row2, lazyRows.getRowData("{Long}150"));
        assertNull(lazyRows.getRowData("{Long}999"));
    }

    /**
     * Test of getIdValue: conversión del valor texto del rowkey al tipo del
     * id de la entidad; nulo si el valor no es convertible.
     */
    @Test
    public void testGetIdValue() {
        System.out.println("lazyDataRows getIdValue");
        LazyDataRows<ArticuloTest> lazyRows = createLazyRows();
        assertEquals(150L, lazyRows.getIdValue("Long", "150"));
        assertEquals(20, lazyRows.getIdValue("Integer", "20"));
        assertEquals((short) 3, lazyRows.getIdValue("Short", "3"));
        assertEquals("abc", lazyRows.getIdValue("String", "abc"));
        assertNull(lazyRows.getIdValue("Long", "noEsNumero"));
    }

    /**
     * Test of getRows y getEntityClass: delegan en el controller asociado y
     * devuelven nulo sin controller.
     */
    @Test
    public void testSinContext() {
        System.out.println("lazyDataRows sinContext");
        LazyDataRows<ArticuloTest> lazyRows = new LazyDataRows<>(null);
        assertNull(lazyRows.getRows());
        assertNull(lazyRows.getEntityClass());
        assertNull(lazyRows.getRowData("{Long}1"));

        lazyRows = createLazyRows();
        assertEquals(ArticuloTest.class, lazyRows.getEntityClass());
    }
}

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
package org.javabeanstack.config;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.javabeanstack.data.IGenericDAO;
import org.javabeanstack.data.model.DataResult;
import org.javabeanstack.model.IAppSystemParam;
import org.javabeanstack.model.appcatalog.AppSystemParam;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Prueba pura (sin contenedor) de los parámetros del sistema por empresa
 * (plan SYSPAR): cascada empresa → global, alcance, valor por defecto y
 * restablecimiento. El DAO es un doble en memoria que interpreta las cuatro
 * consultas de {@link AppGenericConfig}.
 */
public class AppGenericConfigSystemParamTest {

    private static final Long EMPRESA = 8L;
    private static final Long OTRA_EMPRESA = 9L;

    private final List<IAppSystemParam> tabla = new ArrayList<>();
    private final List<String> operaciones = new ArrayList<>();
    private AppGenericConfig config;

    @BeforeEach
    void setUp() {
        tabla.clear();
        operaciones.clear();
        config = new AppGenericConfig();
        config.dao = daoEnMemoria();
    }

    /** Doble del DAO: resuelve las consultas sobre {@link #tabla}. */
    private IGenericDAO daoEnMemoria() {
        return (IGenericDAO) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IGenericDAO.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "findByQuery": {
                            List<IAppSystemParam> filas = filtrar((String) args[1], (Map<String, Object>) args[2]);
                            if (filas.size() > 1) {
                                throw new IllegalStateException("más de un resultado");
                            }
                            return filas.isEmpty() ? null : filas.get(0);
                        }
                        case "findListByQuery":
                            return filtrar((String) args[1], (Map<String, Object>) args[2]);
                        case "persist":
                            tabla.add((IAppSystemParam) args[1]);
                            operaciones.add("persist");
                            return new DataResult();
                        case "merge":
                            operaciones.add("merge");
                            return new DataResult();
                        case "remove":
                            tabla.remove((IAppSystemParam) args[1]);
                            operaciones.add("remove");
                            return new DataResult();
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    private List<IAppSystemParam> filtrar(String query, Map<String, Object> params) {
        if (params != null && params.containsKey("id")) {
            // La consulta por id tiene que nombrar el atributo real de la entidad
            assertTrue(query.contains("idAppSystemParam = :id"), query);
            return tabla.stream().filter(p -> params.get("id").equals(p.getIdAppSystemParam()))
                    .collect(Collectors.toList());
        }
        String param = params == null ? null : (String) params.get("param");
        Long idcompany = params == null ? null : (Long) params.get("idcompany");
        boolean soloGlobal = query.contains("idcompany is null");
        return tabla.stream()
                .filter(p -> param == null || p.getParam().equalsIgnoreCase(param))
                .filter(p -> soloGlobal ? p.getIdcompany() == null
                        : idcompany == null || idcompany.equals(p.getIdcompany()))
                .collect(Collectors.toList());
    }

    private AppSystemParam fila(String param, Long idcompany, char scope, String valor) {
        AppSystemParam p = new AppSystemParam();
        p.setSystemgroup("Test");
        p.setParam(param);
        p.setParamDescrip(param);
        p.setParamType('C');
        p.setValueChar(valor);
        p.setIdcompany(idcompany);
        p.setParamScope(scope);
        tabla.add(p);
        return p;
    }

    @Test
    @DisplayName("Sin valores por empresa, la cascada devuelve el global")
    void soloGlobal() {
        fila("MAIL_FROM_NAME", null, 'E', "Global");
        assertEquals("Global", config.getSystemParam("MAIL_FROM_NAME", EMPRESA).getValueChar());
        assertEquals("Global", config.getSystemParam("mail_from_name").getValueChar());
    }

    @Test
    @DisplayName("Alcance E: el valor de la empresa prevalece; otra empresa ve el global")
    void overrideDeEmpresa() {
        fila("MAIL_FROM_NAME", null, 'E', "Global");
        fila("MAIL_FROM_NAME", EMPRESA, 'E', "Empresa 8");
        assertEquals("Empresa 8", config.getSystemParam("MAIL_FROM_NAME", EMPRESA).getValueChar());
        assertEquals("Global", config.getSystemParam("MAIL_FROM_NAME", OTRA_EMPRESA).getValueChar());
        // La lectura por nombre (la de siempre) no ve el valor de la empresa ni falla
        assertEquals("Global", config.getSystemParam("MAIL_FROM_NAME").getValueChar());
        assertEquals("Global", config.getSystemParam("MAIL_FROM_NAME", null).getValueChar());
    }

    @Test
    @DisplayName("Alcance G: un valor por empresa cargado por script se ignora")
    void overrideIgnoradoEnGlobal() {
        fila("IA_HABILITADO", null, 'G', "Global");
        fila("IA_HABILITADO", EMPRESA, 'G', "Empresa 8");
        assertEquals("Global", config.getSystemParam("IA_HABILITADO", EMPRESA).getValueChar());
    }

    @Test
    @DisplayName("Sin global no rige el valor de la empresa")
    void overrideSinGlobal() {
        fila("HUERFANO", EMPRESA, 'E', "Empresa 8");
        assertNull(config.getSystemParam("HUERFANO", EMPRESA));
    }

    @Test
    @DisplayName("getSystemParams: globales sin duplicar; con empresa, la vista efectiva")
    void listas() {
        fila("A", null, 'E', "a global");
        fila("A", EMPRESA, 'E', "a empresa");
        fila("B", null, 'G', "b global");
        fila("B", EMPRESA, 'G', "b empresa");
        assertEquals(2, config.getSystemParams().size());
        List<IAppSystemParam> efectivos = config.getSystemParams(EMPRESA);
        assertEquals(2, efectivos.size());
        assertEquals("a empresa", efectivos.get(0).getValueChar());
        assertEquals("b global", efectivos.get(1).getValueChar());
    }

    @Test
    @DisplayName("setSystemParam rechaza un valor por empresa sobre un parámetro G (50000)")
    void rechazoPorAlcance() {
        fila("IA_HABILITADO", null, 'G', "Global");
        AppSystemParam nuevo = new AppSystemParam();
        nuevo.setParam("IA_HABILITADO");
        nuevo.setParamType('C');
        nuevo.setIdcompany(EMPRESA);
        SystemParamScopeException ex = assertThrows(SystemParamScopeException.class,
                () -> config.setSystemParam(nuevo));
        assertEquals(50000, ex.getErrorReg().getErrorNumber());
        assertEquals(50000, nuevo.getErrors().get("idcompany").getErrorNumber());
        assertTrue(operaciones.isEmpty());
    }

    @Test
    @DisplayName("setSystemParam de un valor por empresa nuevo no pisa al global")
    void altaDeOverride() throws Exception {
        AppSystemParam global = fila("MAIL_FROM_NAME", null, 'E', "Global");
        AppSystemParam nuevo = new AppSystemParam();
        nuevo.setParam("MAIL_FROM_NAME");
        nuevo.setParamType('C');
        nuevo.setValueChar("Empresa 8");
        nuevo.setIdcompany(EMPRESA);
        config.setSystemParam(nuevo);
        assertEquals(List.of("persist"), operaciones);
        assertNull(nuevo.getIdAppSystemParam());
        assertEquals("Global", global.getValueChar());
    }

    @Test
    @DisplayName("Restablecer: el global vuelve al valor de fábrica; el de empresa se borra")
    void restablecer() throws Exception {
        AppSystemParam global = fila("MAIL_FROM_NAME", null, 'E', "Cambiado");
        global.setDefaultValue("Maker");
        global.setIdAppSystemParam(1L);
        fila("MAIL_FROM_NAME", EMPRESA, 'E', "Empresa 8");

        config.restoreSystemParam("MAIL_FROM_NAME", EMPRESA);
        assertEquals(List.of("remove"), operaciones);
        assertEquals("Cambiado", config.getSystemParam("MAIL_FROM_NAME", EMPRESA).getValueChar());

        config.restoreSystemParam("MAIL_FROM_NAME", null);
        assertEquals("Maker", global.getValueChar());
        assertThrows(IllegalArgumentException.class, () -> config.deleteSystemParam("MAIL_FROM_NAME", null));
    }

    @Test
    @DisplayName("Valor por defecto: ida y vuelta de los cuatro tipos")
    void defaultValueTipos() throws Exception {
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 5, 14, 30, 15, 123);
        assertEquals("T", IAppSystemParam.formatValue('L', true));
        assertEquals("F", IAppSystemParam.formatValue('L', false));
        assertEquals("587", IAppSystemParam.formatValue('N', 587L));
        assertEquals("2026-10-05T14:30:15", IAppSystemParam.formatValue('D', fecha));
        assertEquals("texto", IAppSystemParam.formatValue('C', "texto"));
        assertNull(IAppSystemParam.formatValue('C', null));

        AppSystemParam p = new AppSystemParam();
        p.setParamType('L');
        p.setDefaultValue("T");
        p.restoreDefault();
        assertEquals(Boolean.TRUE, p.getValueBoolean());

        p.setParamType('N');
        p.setDefaultValue("587");
        p.restoreDefault();
        assertEquals(587L, p.getValueNumber());

        p.setParamType('D');
        p.setDefaultValue("2026-10-05T14:30:15");
        p.restoreDefault();
        assertEquals(fecha.withNano(0), p.getValueDate());

        p.setParamType('C');
        p.setValueChar("algo");
        p.setDefaultValue(null);
        p.restoreDefault();
        assertNull(p.getValueChar());
    }

    @Test
    @DisplayName("getSystemParam(Long) busca por el atributo idAppSystemParam (I0-01)")
    void porId() {
        AppSystemParam p = fila("X", null, 'G', "x");
        p.setIdAppSystemParam(42L);
        assertSame(p, config.getSystemParam(42L));
    }

    @Test
    @DisplayName("formatValue acepta el valor como texto y rechaza un tipo incoherente")
    void formatValueTexto() {
        assertEquals("5", IAppSystemParam.formatValue('N', " 5 "));
        assertEquals("T", IAppSystemParam.formatValue('L', "t"));
        assertThrows(IllegalArgumentException.class, () -> IAppSystemParam.formatValue('N', "cinco"));
        assertThrows(IllegalArgumentException.class, () -> IAppSystemParam.formatValue('N', Boolean.TRUE));
    }

    @Test
    @DisplayName("Una entidad nueva nace con alcance global")
    void alcancePorDefecto() {
        AppSystemParam p = new AppSystemParam();
        assertEquals(IAppSystemParam.SCOPE_GLOBAL, p.getParamScope());
        assertFalse(p.isCompanyAllowed());
    }
}

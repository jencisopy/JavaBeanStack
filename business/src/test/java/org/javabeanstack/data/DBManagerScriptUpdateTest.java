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
package org.javabeanstack.data;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.javabeanstack.error.ErrorInfo;
import org.javabeanstack.error.IErrorInfo;
import org.javabeanstack.log.ILogManager;
import org.javabeanstack.model.IAppLogRecord;
import org.javabeanstack.xml.DomW3cParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba pura (sin contenedor ni base) de las dos firmas de
 * {@link DBManager#dbScriptUpdateExecute}: la petición tipada
 * ({@link DbScriptUpdateRequest} / {@link DbScriptUpdateResult} /
 * {@link DbScriptUpdateException}, plan CATUPD Fase 8) y el adaptador
 * histórico de 5 argumentos ({@code @Deprecated}), cuya secuencia esperada se
 * escribió a mano a partir del código anterior a la Fase 8 (CA1-01, CA8-03),
 * no de la implementación vigente.
 *
 * <p>El DAO es un {@link Proxy} de {@link IGenericDAO} que registra cada
 * {@code sqlExec(sessionId, sql, parameters)} (con una copia de los
 * parámetros) y devuelve un {@link ErrorInfo} configurable por sentencia. La
 * fila de la bitácora se simula en memoria: el {@code INSERT} la crea, el
 * cierre y el {@code DELETE} respetan el filtro {@code appuser = :logRowOwner}
 * y {@code findByNativeQuery} la devuelve para la relectura del dueño
 * (M6-01, I8-03). Cada sesión tiene su unidad de persistencia y cada unidad
 * su motor (M1-01); por omisión todo es PU1 / SQLSERVER. No se simula el
 * acceso JDBC con {@code Statement} (I0-05): el contrato probado es el del
 * DAO.</p>
 *
 * No extiende {@code TestClass}: no requiere WildFly.
 */
public class DBManagerScriptUpdateTest {

    private static final String SESSION = "S1";
    private static final String FILENAME = "dbupdmaker_10_099.xml";
    private static final String PU2 = "PU2";
    private static final String OWNER = "startup:test:0a1b2c3d";

    private static final String INSERT_LOG = "INSERT INTO {schema}.applogdbupdate";
    private static final String UPDATE_SCRIPT = "UPDATE {schema}.applogdbupdate SET script";
    private static final String UPDATE_FINISHED = "UPDATE {schema}.applogdbupdate SET finished";
    private static final String DELETE_LOG = "delete from {schema}.applogdbupdate";
    private static final String OWNER_FILTER = "and appuser = :logRowOwner";

    /** Secciones lógicas del paquete de prueba (las define la aplicación, no jbs). */
    private static final String CATALOGO = "CATALOGO";
    private static final String DATOS = "DATOS";

    /**
     * Paquete de prueba, en orden: CATALOGO (3 sentencias), DATOS (2), nodo
     * sin dataconex, CATALOGO (1) y un nodo de otro motor que con PU1 nunca
     * debe ejecutarse. Sin espacios entre nodos.
     */
    private static final String PACKAGE_XML
            = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<ROOT><FILELIST></FILELIST><SCRIPTS>"
            + "<SQLCOMMAND dataconex=\"CATALOGO\" motor=\"SQLSERVER\">\nCAT1\nGO\nCAT2\nGO\nCAT3\nGO\n</SQLCOMMAND>"
            + "<SQLCOMMAND dataconex=\"DATOS\" motor=\"SQLSERVER\">\nDAT1\nGO\nDAT2\nGO\n</SQLCOMMAND>"
            + "<SQLCOMMAND motor=\"SQLSERVER\">\nNOATTR1\nGO\n</SQLCOMMAND>"
            + "<SQLCOMMAND dataconex=\"CATALOGO\" motor=\"SQLSERVER\">\nCAT4\nGO\n</SQLCOMMAND>"
            + "<SQLCOMMAND dataconex=\"CATALOGO\" motor=\"POSTGRES\">\nPGCAT1\n/\n</SQLCOMMAND>"
            + "</SCRIPTS></ROOT>";

    /**
     * Paquete con motores distintos por sección (M1-01): catálogo en SQL
     * Server y datos en PostgreSQL, con los nodos del motor contrario en cada
     * sección.
     */
    private static final String MIXED_ENGINE_XML
            = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<ROOT><SCRIPTS>"
            + "<SQLCOMMAND dataconex=\"CATALOGO\" motor=\"SQLSERVER\">\nCAT1\nGO\n</SQLCOMMAND>"
            + "<SQLCOMMAND dataconex=\"DATOS\" motor=\"SQLSERVER\">\nMSDAT1\nGO\n</SQLCOMMAND>"
            + "<SQLCOMMAND dataconex=\"DATOS\" motor=\"POSTGRES\">\nPGDAT1\n/\nPGDAT2\nGO\nSTILL2\n/\n</SQLCOMMAND>"
            + "<SQLCOMMAND dataconex=\"CATALOGO\" motor=\"POSTGRES\">\nPGCAT1\n/\n</SQLCOMMAND>"
            + "</SCRIPTS></ROOT>";

    /** Nodos con valores de dataconex que la petición no siempre nombra. */
    private static final String OTHER_SECTIONS_XML
            = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<ROOT><SCRIPTS>"
            + "<SQLCOMMAND dataconex=\"CATALOGO\" motor=\"SQLSERVER\">\nCAT1\nGO\n</SQLCOMMAND>"
            + "<SQLCOMMAND dataconex=\"OTRO\" motor=\"SQLSERVER\">\nOTHER1\nGO\n</SQLCOMMAND>"
            + "<SQLCOMMAND dataconex=\"\" motor=\"SQLSERVER\">\nEMPTYATTR1\nGO\n</SQLCOMMAND>"
            + "<SQLCOMMAND motor=\"SQLSERVER\">\nNOATTR1\nGO\n</SQLCOMMAND>"
            + "</SCRIPTS></ROOT>";

    /** Llamada registrada al DAO. */
    private record Call(String sessionId, String sql, Map<String, Object> params, boolean callerMap) {

        /** Representación compacta para comparar secuencias enteras. */
        String shortForm() {
            return (sessionId == null ? "null" : sessionId) + "|" + label(sql);
        }
    }

    private final List<Call> calls = new ArrayList<>();
    private final List<String> dataEngineArgs = new ArrayList<>();
    private final List<String> dbLinkInfoArgs = new ArrayList<>();
    private final List<String> nativeQuerySessions = new ArrayList<>();
    private final Map<String, ErrorInfo> failures = new HashMap<>();
    private final Map<String, String> unitBySession = new HashMap<>();
    private final Map<String, String> engineByUnit = new HashMap<>();
    private final List<String> logSessions = new ArrayList<>();
    private final List<String> logMessages = new ArrayList<>();
    private Map<String, Object> parameters;
    private IGenericDAO dao;
    private ILogManager logMngr;
    /** Fila simulada de la bitácora: {finished, appuser}, o nula si no existe. */
    private Object[] logRow;

    @BeforeEach
    void setUp() {
        calls.clear();
        dataEngineArgs.clear();
        dbLinkInfoArgs.clear();
        nativeQuerySessions.clear();
        failures.clear();
        unitBySession.clear();
        engineByUnit.clear();
        logSessions.clear();
        logMessages.clear();
        parameters = new HashMap<>();
        parameters.put("dbversion", 10);
        parameters.put("sequence", 99);
        parameters.put("filename", FILENAME);
        parameters.put("script", "");
        parameters.put("appuser", "test");
        dao = fakeDao();
        logMngr = fakeLogManager();
        logRow = null;
    }

    // ------------------------------------------------------------------
    // Dobles
    // ------------------------------------------------------------------
    private String unitOf(String sessionId) {
        return unitBySession.getOrDefault(sessionId, IDBManager.CATALOGO);
    }

    @SuppressWarnings("unchecked")
    private IGenericDAO fakeDao() {
        return (IGenericDAO) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IGenericDAO.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "sqlExec":
                            return sqlExec((String) args[0], (String) args[1], (Map<String, Object>) args[2]);
                        case "findByNativeQuery":
                            nativeQuerySessions.add((String) args[0]);
                            List<Object> rows = new ArrayList<>();
                            if (logRow != null) {
                                rows.add(new Object[]{logRow[0], logRow[1]});
                            }
                            return rows;
                        case "getUserSession":
                            return null;
                        case "getDBLinkInfo": {
                            String sessionId = (String) args[0];
                            dbLinkInfoArgs.add(sessionId);
                            String unit = unitOf(sessionId);
                            return new DBLinkInfo() {
                                @Override
                                public String getPersistUnit() {
                                    return unit;
                                }
                            };
                        }
                        case "getDataEngine":
                            dataEngineArgs.add((String) args[0]);
                            return engineByUnit.getOrDefault((String) args[0], "SQLSERVER");
                        case "getSchema":
                            return "dbo";
                        case "getPersistUnitProp": {
                            Map<String, Object> prop = new HashMap<>();
                            prop.put("hibernate.default_schema", "dbo");
                            prop.put("jbs.dbengine", engineByUnit.getOrDefault((String) args[0], "SQLSERVER"));
                            return prop;
                        }
                        case "toString":
                            return "fakeDao";
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    /** sqlExec simulado: registra la llamada y aplica las sentencias de la bitácora a {@link #logRow}. */
    private ErrorInfo sqlExec(String sessionId, String sql, Map<String, Object> params) {
        calls.add(new Call(sessionId, sql, new HashMap<>(params), params == parameters));
        String label = label(sql);
        ErrorInfo failure = failures.get(label);
        if (failure != null) {
            return failure;
        }
        boolean ownerMatches = !sql.contains(OWNER_FILTER)
                || (logRow != null && String.valueOf(logRow[1]).trim().equals(String.valueOf(params.get("logRowOwner")).trim()));
        switch (label) {
            case "INSERT":
                logRow = new Object[]{Boolean.FALSE, params.get("appuser")};
                break;
            case "FINISHED":
                if (logRow != null && ownerMatches) {
                    logRow[0] = Boolean.TRUE;
                }
                break;
            case "DELETE":
                if (ownerMatches) {
                    logRow = null;
                }
                break;
            default:
                break;
        }
        return new ErrorInfo();
    }

    private ILogManager fakeLogManager() {
        return (ILogManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{ILogManager.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getNewAppLogRecord":
                            return fakeLogRecord();
                        case "dbWrite":
                            if (args.length == 2 && args[0] instanceof IAppLogRecord) {
                                logSessions.add((String) args[1]);
                                return Boolean.TRUE;
                            }
                            throw new UnsupportedOperationException("dbWrite variant");
                        case "toString":
                            return "fakeLogManager";
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    private IAppLogRecord fakeLogRecord() {
        return (IAppLogRecord) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IAppLogRecord.class}, (proxy, method, args) -> {
                    if ("setMessage".equals(method.getName())) {
                        logMessages.add((String) args[0]);
                        return null;
                    }
                    if (method.getName().startsWith("set")) {
                        return null;
                    }
                    if ("toString".equals(method.getName())) {
                        return "fakeLogRecord";
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    // ------------------------------------------------------------------
    // Utilitarios
    // ------------------------------------------------------------------
    private static Document load(String xml) throws Exception {
        Document doc = DomW3cParser.loadXml(xml);
        assertNotNull(doc);
        return doc;
    }

    /** Etiqueta legible de una sentencia: tipo de bitácora o el texto del lote. */
    private static String label(String sql) {
        String flat = sql.replaceAll("\\s+", " ").trim();
        if (flat.startsWith(INSERT_LOG)) {
            return "INSERT";
        }
        if (flat.startsWith(UPDATE_SCRIPT)) {
            return "SET_SCRIPT";
        }
        if (flat.startsWith(UPDATE_FINISHED)) {
            return "FINISHED";
        }
        if (flat.startsWith(DELETE_LOG)) {
            return "DELETE";
        }
        return sql.trim();
    }

    /** Petición base: el paquete y los parámetros del test, sin secciones. */
    private DbScriptUpdateRequest.Builder request(String xml) throws Exception {
        return DbScriptUpdateRequest.builder(load(xml), parameters);
    }

    /** Petición con las tres secciones del paquete de prueba (CATALOGO nula, DATOS y sin atributo S1). */
    private DbScriptUpdateRequest.Builder fullRequest(String xml) throws Exception {
        return request(xml).logSessionId(SESSION)
                .section(CATALOGO, null)
                .section(DATOS, SESSION)
                .section(DbScriptUpdateRequest.DATACONEX_UNSET, SESSION);
    }

    private DbScriptUpdateResult execute(DbScriptUpdateRequest.Builder builder, ILogManager log) throws Exception {
        return DBManager.dbScriptUpdateExecute(dao, log, builder.build());
    }

    private DbScriptUpdateException executeExpectingFailure(DbScriptUpdateRequest.Builder builder, ILogManager log) {
        return assertThrows(DbScriptUpdateException.class, () -> execute(builder, log));
    }

    private void legacy(String sessionId, String xml) throws Exception {
        DBManager.dbScriptUpdateExecute(dao, sessionId, load(xml), parameters, logMngr);
    }

    private List<String> trace() {
        List<String> result = new ArrayList<>();
        for (Call c : calls) {
            result.add(c.shortForm());
        }
        return result;
    }

    private List<Call> callsLabelled(String label) {
        List<Call> result = new ArrayList<>();
        for (Call c : calls) {
            if (label(c.sql()).equals(label)) {
                result.add(c);
            }
        }
        return result;
    }

    private ErrorInfo failOn(String statement, Exception exception) {
        ErrorInfo err = new ErrorInfo("rejected " + statement, 50000, "sql");
        if (exception != null) {
            err.setException(exception);
        }
        failures.put(statement, err);
        return err;
    }

    /**
     * Secuencia histórica (código anterior a la Fase 8, CA1-01) de la firma
     * de 5 argumentos con sesión S1 y sin errores: CATALOGO sin sesión, el
     * resto y la bitácora con S1, el nodo POSTGRES salteado.
     */
    private static List<String> legacyFullTrace() {
        return List.of(
                "S1|INSERT",
                "S1|SET_SCRIPT", "null|CAT1", "null|CAT2", "null|CAT3",
                "S1|SET_SCRIPT", "S1|DAT1", "S1|DAT2",
                "S1|SET_SCRIPT", "S1|NOATTR1",
                "S1|SET_SCRIPT", "null|CAT4",
                "S1|FINISHED");
    }

    // ==================================================================
    // API nueva: secciones y sesión por nodo (CA8-02)
    // ==================================================================
    @Test
    void catalogOnly_withNullSessions_runsOnlyCatalogStatementsWithoutSession() throws Exception {
        DbScriptUpdateResult result = execute(request(PACKAGE_XML).section(CATALOGO, null), null);

        assertEquals(List.of(
                "null|INSERT",
                "null|SET_SCRIPT", "null|CAT1", "null|CAT2", "null|CAT3",
                "null|SET_SCRIPT", "null|CAT4",
                "null|FINISHED"), trace());
        // A2: sin sesión, el motor se resuelve sobre la PU del catálogo
        assertEquals(List.of(IDBManager.CATALOGO), dataEngineArgs);
        assertEquals(4, result.getStatementsExecuted());
        assertTrue(result.isLogRowClosed());
        assertFalse(result.hasFailures());
    }

    @Test
    void dataAndUnsetSections_runOnlyThoseNodesWithTheGivenSession() throws Exception {
        execute(request(PACKAGE_XML).logSessionId(SESSION)
                .section(DATOS, SESSION)
                .section(DbScriptUpdateRequest.DATACONEX_UNSET, SESSION), logMngr);

        assertEquals(List.of(
                "S1|INSERT",
                "S1|SET_SCRIPT", "S1|DAT1", "S1|DAT2",
                "S1|SET_SCRIPT", "S1|NOATTR1",
                "S1|FINISHED"), trace());
        assertTrue(logSessions.isEmpty());
    }

    @Test
    void allSections_runEverythingInXmlOrderWithTheSessionOfEachNode() throws Exception {
        DbScriptUpdateResult result = execute(fullRequest(PACKAGE_XML), logMngr);

        assertEquals(legacyFullTrace(), trace());
        assertEquals(7, result.getStatementsExecuted());
        assertTrue(result.getToleratedErrors().isEmpty());
        assertNull(result.getFailedStatementIndex());
        assertNull(result.getFailedStatement());
        assertNull(result.getFailedPersistUnit());
        assertFalse(result.isLogRowDeleted());
    }

    @Test
    void logSessionId_isIndependentFromTheSessionsOfTheNodes() throws Exception {
        failOn("CAT2", null);

        execute(request(PACKAGE_XML).logSessionId("S9").section(CATALOGO, null).continueWithError(true), logMngr);

        assertEquals(List.of(
                "S9|INSERT",
                "S9|SET_SCRIPT", "null|CAT1", "null|CAT2", "null|CAT3",
                "S9|SET_SCRIPT", "null|CAT4",
                "S9|FINISHED"), trace());
        // applogevent con la sesión de la bitácora, no con la del nodo
        assertEquals(List.of("S9"), logSessions);
    }

    @Test
    void unsetKeyMissing_skipsTheNodeWithoutAttribute() throws Exception {
        execute(request(PACKAGE_XML).logSessionId(SESSION)
                .section(CATALOGO, null).section(DATOS, SESSION), logMngr);

        assertEquals(List.of(
                "S1|INSERT",
                "S1|SET_SCRIPT", "null|CAT1", "null|CAT2", "null|CAT3",
                "S1|SET_SCRIPT", "S1|DAT1", "S1|DAT2",
                "S1|SET_SCRIPT", "null|CAT4",
                "S1|FINISHED"), trace());
    }

    @Test
    void explicitUnset_runsNodesWithoutAttributeOrWithEmptyAttributeWithItsSession() throws Exception {
        DbScriptUpdateResult result = execute(request(OTHER_SECTIONS_XML).logSessionId(SESSION)
                .section(DbScriptUpdateRequest.DATACONEX_UNSET, "S2"), logMngr);

        assertEquals(List.of(
                "S1|INSERT",
                "S1|SET_SCRIPT", "S2|EMPTYATTR1",
                "S1|SET_SCRIPT", "S2|NOATTR1",
                "S1|FINISHED"), trace());
        assertEquals(2, result.getStatementsExecuted());
        assertEquals("", DbScriptUpdateRequest.DATACONEX_UNSET);
    }

    @Test
    void nodeWithValueNotInMap_isSkippedWithoutTouchingTheLogOrResolvingItsUnit() throws Exception {
        DbScriptUpdateResult result = execute(request(OTHER_SECTIONS_XML).section(CATALOGO, null), logMngr);

        assertEquals(List.of("null|INSERT", "null|SET_SCRIPT", "null|CAT1", "null|FINISHED"), trace());
        assertEquals(1, callsLabelled("SET_SCRIPT").size());
        assertEquals(1, result.getStatementsExecuted());
        // Solo se resolvió la unidad de la sección pedida
        assertEquals(List.of(IDBManager.CATALOGO), dataEngineArgs);
        for (String s : dbLinkInfoArgs) {
            assertNull(s);
        }
    }

    @Test
    void skippedDataconex_reportsEveryUnmappedValueOnceInOrderOfAppearance() throws Exception {
        DbScriptUpdateResult result = execute(request(OTHER_SECTIONS_XML).section(CATALOGO, null), logMngr);

        // I6-07: OTRO y el vacío (dos nodos: atributo vacío y sin atributo) se informan una vez
        assertEquals(List.of("OTRO", DbScriptUpdateRequest.DATACONEX_UNSET),
                new ArrayList<>(result.getSkippedDataconex()));
        assertFalse(result.getSkippedDataconex().contains(CATALOGO));
        assertThrows(UnsupportedOperationException.class, () -> result.getSkippedDataconex().add("X"));
    }

    @Test
    void skippedDataconex_doesNotIncludeMappedNodesSkippedByEngine() throws Exception {
        // El nodo CATALOGO de POSTGRES se saltea por motor, no por sección
        DbScriptUpdateResult result = execute(fullRequest(PACKAGE_XML), logMngr);

        assertTrue(result.getSkippedDataconex().isEmpty(), result.getSkippedDataconex().toString());
    }

    @Test
    void skippedDataconex_isAlsoReportedWhenTheExecutionIsCutByAFailure() throws Exception {
        failOn("CAT4", null);

        DbScriptUpdateException thrown = executeExpectingFailure(request(PACKAGE_XML).logSessionId(SESSION)
                .section(CATALOGO, null), logMngr);

        assertEquals(List.of(DATOS, DbScriptUpdateRequest.DATACONEX_UNSET),
                new ArrayList<>(thrown.getResult().getSkippedDataconex()));
    }

    @Test
    void noSelectedNode_stillInsertsAndClosesTheRow() throws Exception {
        DbScriptUpdateResult result = execute(request(PACKAGE_XML).logSessionId(SESSION).section("NINGUNO", SESSION), null);

        assertEquals(List.of("S1|INSERT", "S1|FINISHED"), trace());
        assertEquals(0, result.getStatementsExecuted());
        assertTrue(result.isLogRowClosed());
        assertTrue(dataEngineArgs.isEmpty());
        assertEquals(List.of(CATALOGO, DATOS, DbScriptUpdateRequest.DATACONEX_UNSET),
                new ArrayList<>(result.getSkippedDataconex()));
    }

    // ------------------------------------------------------------------
    // Motor por unidad de persistencia (M1-01)
    // ------------------------------------------------------------------
    @Test
    void enginePerPersistUnit_eachNodeIsFilteredAndSplitByTheEngineOfItsOwnSession() throws Exception {
        unitBySession.put(SESSION, PU2);
        engineByUnit.put(PU2, "POSTGRES");

        DbScriptUpdateResult result = execute(fullRequest(MIXED_ENGINE_XML), logMngr);

        assertEquals(List.of(
                "S1|INSERT",
                "S1|SET_SCRIPT", "null|CAT1",
                "S1|SET_SCRIPT", "S1|PGDAT1", "S1|PGDAT2\nGO\nSTILL2",
                "S1|FINISHED"), trace());
        // Un motor por unidad, consultado una sola vez (cache por sesión)
        assertEquals(List.of(IDBManager.CATALOGO, PU2), dataEngineArgs);
        assertEquals(3, result.getStatementsExecuted());
    }

    @Test
    void failedPersistUnit_isTheRealUnitOfTheFailingNode() throws Exception {
        unitBySession.put(SESSION, PU2);
        engineByUnit.put(PU2, "POSTGRES");
        RuntimeException cause = new RuntimeException("boom on pg");
        failOn("PGDAT1", cause);

        DbScriptUpdateException thrown = executeExpectingFailure(fullRequest(MIXED_ENGINE_XML), logMngr);

        assertSame(cause, thrown.getCause());
        assertEquals("ERROR en unidad de persistencia PU2, SCRIPT " + FILENAME + ", rejected PGDAT1", thrown.getMessage());
        DbScriptUpdateResult partial = thrown.getResult();
        assertEquals(PU2, partial.getFailedPersistUnit());
        assertEquals(2, partial.getFailedStatementIndex());
        assertEquals("PGDAT1", partial.getFailedStatement().trim());
        assertEquals(List.of(
                "S1|INSERT",
                "S1|SET_SCRIPT", "null|CAT1",
                "S1|SET_SCRIPT", "S1|PGDAT1",
                "S1|DELETE"), trace());
        assertEquals(List.of(thrown.getMessage()), logMessages);
        assertEquals(List.of(SESSION), logSessions);
    }

    @Test
    void catalogFailure_reportsThePersistUnitOfTheCatalogWithTheNewMessage() throws Exception {
        failOn("CAT2", null);

        DbScriptUpdateException thrown = executeExpectingFailure(fullRequest(PACKAGE_XML), logMngr);

        // I8-05: la API nueva no conserva el «ERROR en PU1» histórico
        assertEquals("ERROR en unidad de persistencia PU1, SCRIPT " + FILENAME + ", rejected CAT2", thrown.getMessage());
        assertEquals(IDBManager.CATALOGO, thrown.getResult().getFailedPersistUnit());
    }

    // ==================================================================
    // Política de errores y DbScriptUpdateException
    // ==================================================================
    @Test
    void errorWithoutContinue_deletesRowLogsAndThrowsWithCauseAndPartialResult() throws Exception {
        RuntimeException cause = new RuntimeException("boom on catalog");
        failOn("CAT2", cause);

        DbScriptUpdateException thrown = executeExpectingFailure(request(PACKAGE_XML).section(CATALOGO, null), logMngr);

        assertSame(cause, thrown.getCause());
        assertEquals(List.of(
                "null|INSERT",
                "null|SET_SCRIPT", "null|CAT1", "null|CAT2",
                "null|DELETE"), trace());
        DbScriptUpdateResult partial = thrown.getResult();
        assertNotNull(partial);
        assertEquals(2, partial.getStatementsExecuted());
        assertEquals(2, partial.getFailedStatementIndex());
        assertEquals("CAT2", partial.getFailedStatement().trim());
        assertTrue(partial.isLogRowDeleted());
        assertFalse(partial.isLogRowClosed());
        assertTrue(partial.hasFailures());
        assertTrue(partial.getToleratedErrors().isEmpty());
        assertEquals(1, logSessions.size());
        assertNull(logSessions.get(0));
        assertNull(logRow, "the log row should have been deleted");
    }

    @Test
    void errorWithoutException_causeCarriesTheMessage() throws Exception {
        failOn("DAT2", null);

        DbScriptUpdateException thrown = executeExpectingFailure(fullRequest(PACKAGE_XML), logMngr);

        assertNotNull(thrown.getCause());
        assertEquals(thrown.getMessage(), thrown.getCause().getMessage());
        assertEquals("ERROR en unidad de persistencia PU1, SCRIPT " + FILENAME + ", rejected DAT2", thrown.getMessage());
        assertEquals(5, thrown.getResult().getFailedStatementIndex());
    }

    @Test
    void errorWithoutLogManager_stillDeletesAndThrows() throws Exception {
        failOn("CAT1", null);

        executeExpectingFailure(request(PACKAGE_XML).section(CATALOGO, null), null);

        assertEquals(List.of("null|INSERT", "null|SET_SCRIPT", "null|CAT1", "null|DELETE"), trace());
    }

    @Test
    void dbScriptUpdateException_isCheckedAndKeepsTheResult() {
        DbScriptUpdateResult partial = new DbScriptUpdateResult();
        Exception cause = new Exception("cause");

        DbScriptUpdateException e = new DbScriptUpdateException("msg", cause, partial);

        assertFalse(RuntimeException.class.isAssignableFrom(DbScriptUpdateException.class));
        assertSame(cause, e.getCause());
        assertSame(partial, e.getResult());
        assertEquals("msg", e.getMessage());
    }

    @Test
    void continueWithError_toleratesEveryFailureInOrderAndClosesTheRow() throws Exception {
        ErrorInfo catError = failOn("CAT2", new RuntimeException("boom on catalog"));
        ErrorInfo dataError = failOn("DAT1", null);

        DbScriptUpdateResult result = execute(fullRequest(PACKAGE_XML).continueWithError(true), logMngr);

        assertEquals(legacyFullTrace(), trace());
        assertEquals(7, result.getStatementsExecuted());
        List<IErrorInfo> tolerated = result.getToleratedErrors();
        assertEquals(2, tolerated.size());
        assertSame(catError, tolerated.get(0));
        assertSame(dataError, tolerated.get(1));
        // El primero gana
        assertEquals(2, result.getFailedStatementIndex());
        assertEquals("CAT2", result.getFailedStatement().trim());
        assertEquals(IDBManager.CATALOGO, result.getFailedPersistUnit());
        assertTrue(result.hasFailures());
        assertFalse(result.isLogRowDeleted());
        assertTrue(result.isLogRowClosed());
        assertEquals(List.of(SESSION, SESSION), logSessions);
        assertEquals(List.of(
                "ERROR en unidad de persistencia PU1, SCRIPT " + FILENAME + ", rejected CAT2",
                "ERROR en unidad de persistencia PU1, SCRIPT " + FILENAME + ", rejected DAT1"), logMessages);
        assertThrows(UnsupportedOperationException.class, () -> tolerated.add(new ErrorInfo()));
    }

    @Test
    void continueWithError_isTakenFromTheRequestNotFromTheParameters() throws Exception {
        // La clave histórica en el mapa no cambia la política de la API nueva
        parameters.put("CONTINUE_WITH_ERROR", true);
        failOn("DAT1", null);

        executeExpectingFailure(fullRequest(PACKAGE_XML), logMngr);

        assertEquals("S1|DELETE", trace().get(trace().size() - 1));
        assertFalse(trace().contains("S1|DAT2"));
    }

    @Test
    void deleteRejected_reportsRowNotDeleted() throws Exception {
        failOn("DAT1", null);
        failures.put("DELETE", new ErrorInfo("delete rejected", 50000, "sql"));

        DbScriptUpdateException thrown = executeExpectingFailure(fullRequest(PACKAGE_XML), logMngr);

        assertEquals("S1|DELETE", trace().get(trace().size() - 1));
        assertEquals(4, thrown.getResult().getFailedStatementIndex());
        assertFalse(thrown.getResult().isLogRowDeleted());
    }

    @Test
    void closeRejected_reportsRowNotClosed() throws Exception {
        failures.put("FINISHED", new ErrorInfo("update rejected", 50000, "sql"));

        DbScriptUpdateResult result = execute(request(PACKAGE_XML).section(CATALOGO, null), null);

        assertFalse(result.isLogRowClosed());
        assertTrue(nativeQuerySessions.isEmpty());
    }

    // ==================================================================
    // Bitácora: fila propia y dueño (M6-01, CA8-05)
    // ==================================================================
    @Test
    void logRowOwned_skipsTheInsertButKeepsTheRestOfTheLog() throws Exception {
        logRow = new Object[]{Boolean.FALSE, "test"};

        DbScriptUpdateResult result = execute(request(PACKAGE_XML).section(CATALOGO, null).logRowOwned(true), null);

        assertEquals(List.of(
                "null|SET_SCRIPT", "null|CAT1", "null|CAT2", "null|CAT3",
                "null|SET_SCRIPT", "null|CAT4",
                "null|FINISHED"), trace());
        assertFalse(result.isLogRowDeleted());
        assertTrue(result.isLogRowClosed());
    }

    @Test
    void withoutOwner_closeAndDeleteHaveNoOwnerFilterAndTheRowIsNotReread() throws Exception {
        execute(request(PACKAGE_XML).section(CATALOGO, null), null);
        failOn("CAT1", null);
        executeExpectingFailure(request(PACKAGE_XML).section(CATALOGO, null), null);

        for (Call c : calls) {
            assertFalse(c.sql().contains("appuser ="), c.sql());
            assertFalse(c.params().containsKey("logRowOwner"));
        }
        assertEquals(1, callsLabelled("FINISHED").size());
        assertEquals(1, callsLabelled("DELETE").size());
        assertTrue(nativeQuerySessions.isEmpty());
    }

    @Test
    void ownRow_isClosedWithTheOwnerFilterAndRereadWithTheLogSession() throws Exception {
        // M6-08: el dueño es el appuser de la fila
        parameters.put("appuser", OWNER);
        logRow = new Object[]{Boolean.FALSE, OWNER};

        DbScriptUpdateResult result = execute(request(PACKAGE_XML).logSessionId(SESSION)
                .section(CATALOGO, null).logRowOwned(true).logRowOwner(OWNER), logMngr);

        Call close = callsLabelled("FINISHED").get(0);
        assertTrue(close.sql().endsWith(OWNER_FILTER), close.sql());
        assertEquals(OWNER, close.params().get("logRowOwner"));
        assertEquals(Boolean.TRUE, logRow[0]);
        assertTrue(result.isLogRowClosed());
        assertEquals(List.of(SESSION), nativeQuerySessions);
    }

    @Test
    void ownRowWithPaddedAppuser_isReportedClosed() throws Exception {
        // M6-08: el dueño es el appuser de la fila
        parameters.put("appuser", OWNER);
        // char(n): la columna llega con relleno
        logRow = new Object[]{Boolean.FALSE, OWNER + "     "};

        DbScriptUpdateResult result = execute(request(PACKAGE_XML).section(CATALOGO, null)
                .logRowOwned(true).logRowOwner(OWNER + " "), null);

        assertTrue(result.isLogRowClosed());
    }

    @Test
    void rowOfAnotherOwner_isNotClosedAndTheResultSaysSo() throws Exception {
        // M6-08: el dueño es el appuser de la fila
        parameters.put("appuser", OWNER);
        logRow = new Object[]{Boolean.FALSE, "startup:other:ffffffff"};

        DbScriptUpdateResult result = execute(request(PACKAGE_XML).section(CATALOGO, null)
                .logRowOwned(true).logRowOwner(OWNER), null);

        assertEquals("null|FINISHED", trace().get(trace().size() - 1));
        assertEquals(Boolean.FALSE, logRow[0]);
        assertEquals("startup:other:ffffffff", logRow[1]);
        assertFalse(result.isLogRowClosed());
        assertEquals(1, nativeQuerySessions.size());
        assertNull(nativeQuerySessions.get(0));
    }

    @Test
    void rowOfAnotherOwner_isNotDeletedOnFailure() throws Exception {
        // M6-08: el dueño es el appuser de la fila
        parameters.put("appuser", OWNER);
        logRow = new Object[]{Boolean.FALSE, "startup:other:ffffffff"};
        failOn("CAT2", null);

        DbScriptUpdateException thrown = executeExpectingFailure(request(PACKAGE_XML).section(CATALOGO, null)
                .logRowOwned(true).logRowOwner(OWNER), null);

        Call delete = callsLabelled("DELETE").get(0);
        assertTrue(delete.sql().endsWith(OWNER_FILTER), delete.sql());
        assertEquals(OWNER, delete.params().get("logRowOwner"));
        assertNotNull(logRow, "a foreign row must survive the rollback");
        assertEquals("startup:other:ffffffff", logRow[1]);
        assertFalse(thrown.getResult().isLogRowClosed());
        // I8-03: logRowDeleted = DELETE sin error, sin relectura; el filtro de dueño
        // garantiza que la fila ajena no se borró
        assertTrue(thrown.getResult().isLogRowDeleted());
    }

    @Test
    void rowMissingAtClose_isReportedNotClosed() throws Exception {
        // M6-08: el dueño es el appuser de la fila
        parameters.put("appuser", OWNER);
        DbScriptUpdateResult result = execute(request(PACKAGE_XML).section(CATALOGO, null)
                .logRowOwned(true).logRowOwner(OWNER), null);

        assertNull(logRow);
        assertFalse(result.isLogRowClosed());
    }

    // ==================================================================
    // Parámetros: el mapa del llamador no se modifica
    // ==================================================================
    @Test
    void newApi_doesNotModifyTheCallerMapOnSuccessOrFailure() throws Exception {
        // M6-08: el dueño es el appuser de la fila
        parameters.put("appuser", OWNER);
        parameters.put("script", "ORIGINAL");
        Map<String, Object> before = new HashMap<>(parameters);

        execute(fullRequest(PACKAGE_XML).logRowOwner(OWNER).logRowOwned(true), logMngr);
        assertEquals(before, parameters);

        failOn("DAT1", null);
        executeExpectingFailure(fullRequest(PACKAGE_XML).logRowOwner(OWNER), logMngr);
        assertEquals(before, parameters);

        for (Call c : calls) {
            assertFalse(c.callerMap(), "the DAO received the caller's map");
        }
    }

    @Test
    void newApi_logSentencesReceiveTheScriptOfEachNodeInTheWorkingCopy() throws Exception {
        parameters.put("script", "ORIGINAL");

        execute(request(PACKAGE_XML).section(CATALOGO, null), null);

        assertEquals("ORIGINAL", callsLabelled("INSERT").get(0).params().get("script"));
        List<Call> setScripts = callsLabelled("SET_SCRIPT");
        assertTrue(String.valueOf(setScripts.get(0).params().get("script")).contains("CAT1"));
        assertTrue(String.valueOf(setScripts.get(1).params().get("script")).contains("CAT4"));
        // Tras cada SET_SCRIPT la clave se vacía: las sentencias no la arrastran
        for (Call c : calls) {
            if (label(c.sql()).startsWith("CAT")) {
                assertEquals("", c.params().get("script"));
            }
        }
    }

    @Test
    void request_copiesTheParametersWhenBuilt() throws Exception {
        DbScriptUpdateRequest request = request(PACKAGE_XML).section(CATALOGO, null).build();
        parameters.put("appuser", "changed-after-build");

        DBManager.dbScriptUpdateExecute(dao, null, request);

        assertEquals("test", callsLabelled("INSERT").get(0).params().get("appuser"));
        assertThrows(UnsupportedOperationException.class, () -> request.getParameters().put("x", 1));
        assertThrows(UnsupportedOperationException.class, () -> request.getSessionByDataconex().put("x", null));
    }

    // ------------------------------------------------------------------
    // Armador
    // ------------------------------------------------------------------
    @Test
    void builder_withoutSections_throwsIllegalArgumentWithoutTouchingTheDao() throws Exception {
        DbScriptUpdateRequest.Builder builder = request(PACKAGE_XML).logSessionId(SESSION);

        assertThrows(IllegalArgumentException.class, builder::build);

        assertTrue(calls.isEmpty());
        assertTrue(dataEngineArgs.isEmpty());
    }

    @Test
    void builder_ownerDifferentFromAppuser_throwsIllegalArgumentWithoutTouchingTheDao() throws Exception {
        // setUp deja appuser = "test"
        DbScriptUpdateRequest.Builder builder = request(PACKAGE_XML).section(CATALOGO, null).logRowOwner(OWNER);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, builder::build);

        assertTrue(thrown.getMessage().contains(OWNER) && thrown.getMessage().contains("test"), thrown.getMessage());
        assertTrue(calls.isEmpty());
    }

    @Test
    void builder_ownerWithoutAppuser_becomesTheAppuserOfTheInsertWithoutTouchingTheCallerMap() throws Exception {
        parameters.remove("appuser");
        Map<String, Object> before = new HashMap<>(parameters);
        logRow = new Object[]{Boolean.FALSE, OWNER};

        DbScriptUpdateRequest request = request(PACKAGE_XML).section(CATALOGO, null).logRowOwner(OWNER).build();
        DbScriptUpdateResult result = DBManager.dbScriptUpdateExecute(dao, null, request);

        assertEquals(OWNER, request.getParameters().get("appuser"));
        assertEquals(OWNER, callsLabelled("INSERT").get(0).params().get("appuser"));
        assertEquals(before, parameters, "the caller's map must not change");
        assertTrue(result.isLogRowClosed());
    }

    @Test
    void builder_ownerAndAppuserEqualButPadded_isAccepted() throws Exception {
        parameters.put("appuser", OWNER + "  ");

        DbScriptUpdateRequest request = request(PACKAGE_XML).section(CATALOGO, null).logRowOwner(OWNER).build();

        assertEquals(OWNER + "  ", request.getParameters().get("appuser"));
    }

    @Test
    void builder_rejectsNullArgumentsButAcceptsNullSessions() throws Exception {
        Document doc = load(PACKAGE_XML);

        assertThrows(NullPointerException.class, () -> DbScriptUpdateRequest.builder(null, parameters));
        assertThrows(NullPointerException.class, () -> DbScriptUpdateRequest.builder(doc, null));
        assertThrows(NullPointerException.class, () -> DbScriptUpdateRequest.builder(doc, parameters).section(null, SESSION));

        Map<String, String> sessions = new HashMap<>();
        sessions.put(CATALOGO, null);
        DbScriptUpdateRequest request = DbScriptUpdateRequest.builder(doc, parameters).sessionByDataconex(sessions).build();
        assertTrue(request.getSessionByDataconex().containsKey(CATALOGO));
        assertNull(request.getSessionByDataconex().get(CATALOGO));
        assertNull(request.getLogSessionId());
        assertFalse(request.isContinueWithError());
        assertFalse(request.isLogRowOwned());
        assertNull(request.getLogRowOwner());
    }

    // ------------------------------------------------------------------
    // Separador GO (documenta el comportamiento vigente)
    // ------------------------------------------------------------------
    @Test
    void indentedGo_doesNotSplitTheBatch() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ROOT><SCRIPTS>"
                + "<SQLCOMMAND dataconex=\"CATALOGO\" motor=\"SQLSERVER\">\nA1\n  GO\nA2\nGO  \nA3\nGO\nA4\n</SQLCOMMAND>"
                + "</SCRIPTS></ROOT>";

        DbScriptUpdateResult result = execute(request(xml).section(CATALOGO, null), null);

        List<String> statements = new ArrayList<>();
        for (Call c : calls) {
            String l = label(c.sql());
            if (!List.of("INSERT", "SET_SCRIPT", "FINISHED").contains(l)) {
                statements.add(c.sql());
            }
        }
        // "  GO" y "GO  " no cortan: el primer lote llega con ambos adentro
        assertEquals(2, statements.size(), statements.toString());
        assertEquals("A1\n  GO\nA2\nGO  \nA3", statements.get(0).trim());
        assertEquals("A4", statements.get(1).trim());
        assertEquals(2, result.getStatementsExecuted());
    }

    @Test
    void lowercaseGo_splitsTheBatch() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ROOT><SCRIPTS>"
                + "<SQLCOMMAND dataconex=\"DATOS\" motor=\"SQLSERVER\">\nB1\ngo\nB2\n</SQLCOMMAND>"
                + "</SCRIPTS></ROOT>";

        execute(request(xml).logSessionId(SESSION).section(DATOS, SESSION), null);

        assertEquals(List.of("S1|INSERT", "S1|SET_SCRIPT", "S1|B1", "S1|B2", "S1|FINISHED"), trace());
    }

    // ==================================================================
    // Firma de 5 argumentos (@Deprecated): regresión histórica (CA1-01, CA8-03)
    // ==================================================================
    @Test
    void fiveArgSignature_producesTheLegacyCallSequence() throws Exception {
        legacy(SESSION, PACKAGE_XML);

        assertEquals(legacyFullTrace(), trace());
        // Histórico: un solo motor, el de la PU de la sesión, consultado una vez
        assertEquals(List.of(IDBManager.CATALOGO), dataEngineArgs);
        assertEquals(List.of(SESSION), dbLinkInfoArgs);
        assertTrue(logSessions.isEmpty());
        assertTrue(nativeQuerySessions.isEmpty());
    }

    @Test
    void fiveArgSignature_withNullSession_runsEverythingWithoutSession() throws Exception {
        legacy(null, PACKAGE_XML);

        assertEquals(List.of(
                "null|INSERT",
                "null|SET_SCRIPT", "null|CAT1", "null|CAT2", "null|CAT3",
                "null|SET_SCRIPT", "null|DAT1", "null|DAT2",
                "null|SET_SCRIPT", "null|NOATTR1",
                "null|SET_SCRIPT", "null|CAT4",
                "null|FINISHED"), trace());
    }

    @Test
    void fiveArgSignature_everyValueOtherThanCatalogoRunsWithTheSession() throws Exception {
        legacy(SESSION, OTHER_SECTIONS_XML);

        // Histórico: todo lo que no es exactamente CATALOGO va con sessionId
        assertEquals(List.of(
                "S1|INSERT",
                "S1|SET_SCRIPT", "null|CAT1",
                "S1|SET_SCRIPT", "S1|OTHER1",
                "S1|SET_SCRIPT", "S1|EMPTYATTR1",
                "S1|SET_SCRIPT", "S1|NOATTR1",
                "S1|FINISHED"), trace());
    }

    @Test
    void fiveArgSignature_keepsTheSingleEngineOfTheSessionForAllNodes() throws Exception {
        // M1-01 se conserva a propósito en el adaptador: el motor es el de la PU de sessionId,
        // también para los nodos CATALOGO, que igual viajan sin sesión
        unitBySession.put(SESSION, PU2);
        engineByUnit.put(PU2, "POSTGRES");

        legacy(SESSION, MIXED_ENGINE_XML);

        assertEquals(List.of(
                "S1|INSERT",
                "S1|SET_SCRIPT", "S1|PGDAT1", "S1|PGDAT2\nGO\nSTILL2",
                "S1|SET_SCRIPT", "null|PGCAT1",
                "S1|FINISHED"), trace());
        assertEquals(List.of(PU2), dataEngineArgs);
    }

    @Test
    void fiveArgSignature_toleratesCatalogErrorEvenWithoutContinueWithError() throws Exception {
        parameters.put("CONTINUE_WITH_ERROR", false);
        failOn("CAT2", new RuntimeException("boom on catalog"));

        legacy(SESSION, PACKAGE_XML);

        // Misma secuencia que sin error: no hay DELETE y llega a FINISHED
        assertEquals(legacyFullTrace(), trace());
        assertEquals(List.of(SESSION), logSessions);
        assertEquals(List.of("ERROR en PU1, SCRIPT " + FILENAME + ", rejected CAT2"), logMessages);
    }

    @Test
    void fiveArgSignature_catalogErrorWithoutExceptionAndMissingFlag_isTolerated() throws Exception {
        // Ex nullDataconex_toleratesCatalogError: sin excepción y sin CONTINUE_WITH_ERROR
        failOn("CAT1", null);

        legacy(SESSION, PACKAGE_XML);

        assertEquals(legacyFullTrace(), trace());
        assertEquals(List.of("ERROR en PU1, SCRIPT " + FILENAME + ", rejected CAT1"), logMessages);
        assertEquals(List.of(SESSION), logSessions);
    }

    @Test
    void fiveArgSignature_catalogMessageSaysPu1EvenWhenTheSessionUnitDiffers() throws Exception {
        unitBySession.put(SESSION, PU2);
        failOn("CAT1", null);
        failOn("DAT1", null);
        parameters.put("CONTINUE_WITH_ERROR", true);

        legacy(SESSION, PACKAGE_XML);

        assertEquals(legacyFullTrace(), trace());
        assertEquals(List.of(
                "ERROR en PU1, SCRIPT " + FILENAME + ", rejected CAT1",
                "ERROR en unidad de persistencia PU2, SCRIPT " + FILENAME + ", rejected DAT1"), logMessages);
        assertEquals(List.of(SESSION, SESSION), logSessions);
    }

    @Test
    void fiveArgSignature_dataErrorWithoutContinue_deletesLogRowAndThrowsTheStatementException() throws Exception {
        parameters.put("CONTINUE_WITH_ERROR", false);
        RuntimeException cause = new RuntimeException("boom on data");
        failOn("DAT1", cause);

        Exception thrown = assertThrows(Exception.class, () -> legacy(SESSION, PACKAGE_XML));

        // Sin envolver en DbScriptUpdateException
        assertSame(cause, thrown);
        assertEquals(List.of(
                "S1|INSERT",
                "S1|SET_SCRIPT", "null|CAT1", "null|CAT2", "null|CAT3",
                "S1|SET_SCRIPT", "S1|DAT1",
                "S1|DELETE"), trace());
        assertEquals(List.of(SESSION), logSessions);
        assertEquals(List.of("ERROR en unidad de persistencia PU1, SCRIPT " + FILENAME + ", rejected DAT1"), logMessages);
    }

    @Test
    void fiveArgSignature_dataErrorWithoutException_throwsAPlainExceptionWithTheMessage() throws Exception {
        // CONTINUE_WITH_ERROR ausente equivale a falso
        failOn("DAT2", null);

        Exception thrown = assertThrows(Exception.class, () -> legacy(SESSION, PACKAGE_XML));

        assertFalse(thrown instanceof DbScriptUpdateException);
        assertEquals(Exception.class, thrown.getClass());
        assertEquals("ERROR en unidad de persistencia PU1, SCRIPT " + FILENAME + ", rejected DAT2", thrown.getMessage());
        assertEquals("S1|DELETE", trace().get(trace().size() - 1));
        assertFalse(trace().contains("S1|NOATTR1"));
    }

    @Test
    void fiveArgSignature_dataErrorWithContinue_logsAndGoesOn() throws Exception {
        parameters.put("CONTINUE_WITH_ERROR", true);
        failOn("DAT1", null);

        legacy(SESSION, PACKAGE_XML);

        assertEquals(legacyFullTrace(), trace());
        assertEquals(List.of(SESSION), logSessions);
    }

    @Test
    void fiveArgSignature_usesAndMutatesTheCallerMapAsBefore() throws Exception {
        parameters.put("script", "ORIGINAL");
        Map<String, Object> before = new HashMap<>(parameters);

        legacy(SESSION, PACKAGE_XML);

        // Histórico: el DAO recibe el mapa del llamador, que queda con script vacío
        for (Call c : calls) {
            assertTrue(c.callerMap(), "the DAO did not receive the caller's map: " + c.sql());
            assertFalse(c.params().containsKey("logRowOwner"));
        }
        assertEquals("ORIGINAL", callsLabelled("INSERT").get(0).params().get("script"));
        assertTrue(String.valueOf(callsLabelled("SET_SCRIPT").get(0).params().get("script")).contains("CAT1"));
        before.put("script", "");
        assertEquals(before, parameters);
    }

    @Test
    void fiveArgSignature_neverFiltersByOwnerNorRereadsTheRow() throws Exception {
        parameters.put("CONTINUE_WITH_ERROR", false);
        failOn("DAT1", null);
        assertThrows(Exception.class, () -> legacy(SESSION, PACKAGE_XML));
        failures.clear();
        legacy(SESSION, PACKAGE_XML);

        assertEquals(2, callsLabelled("INSERT").size());
        for (Call c : calls) {
            assertFalse(c.sql().contains("appuser ="), c.sql());
        }
        assertTrue(nativeQuerySessions.isEmpty());
        assertInstanceOf(Object[].class, logRow);
    }
}

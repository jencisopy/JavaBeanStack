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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.javabeanstack.error.IErrorReg;

/**
 * Resultado de
 * {@link DBManager#dbScriptUpdateExecute(IGenericDAO, org.javabeanstack.log.ILogManager, DbScriptUpdateRequest)}.
 * Lo devuelve la ejecución que termina y lo lleva
 * {@link DbScriptUpdateException#getResult()} cuando una sentencia rechazada
 * corta la ejecución.
 *
 * <p>Es inmutable para quien lo recibe; lo completa el ejecutor del mismo
 * paquete.</p>
 *
 * @author Jorge Enciso
 */
public final class DbScriptUpdateResult {

    private int statementsExecuted;
    private boolean logRowClosed;
    private boolean logRowDeleted;
    private Integer failedStatementIndex;
    private String failedStatement;
    private String failedPersistUnit;
    private final List<IErrorReg> toleratedErrors = new ArrayList<>();
    private final Set<String> skippedDataconex = new LinkedHashSet<>();

    DbScriptUpdateResult() {
    }

    /**
     * Cantidad de sentencias (lotes separados por {@code GO} o {@code /}) que
     * se enviaron al motor, rechazadas incluidas. No cuenta las sentencias de
     * la bitácora.
     *
     * @return la cantidad.
     */
    public int getStatementsExecuted() {
        return statementsExecuted;
    }

    /**
     * Indica si la fila de la bitácora quedó concluida ({@code finished}).
     *
     * <p>Con dueño ({@link DbScriptUpdateRequest#getLogRowOwner()}) se relee
     * la fila después del cierre y solo es verdadero si está concluida y es
     * del dueño: {@code sqlExec} no informa cuántas filas afectó, y una fila
     * que otro tomó entretanto no se cierra. Sin dueño significa que el
     * {@code UPDATE} de cierre se ejecutó sin error.</p>
     *
     * @return verdadero si la fila quedó concluida; falso también si la
     * ejecución se cortó antes del cierre.
     */
    public boolean isLogRowClosed() {
        return logRowClosed;
    }

    /**
     * Indica si una sentencia rechazada (política sin tolerancia) hizo borrar
     * la fila de la bitácora y el {@code DELETE} se ejecutó sin error. Con
     * dueño el {@code DELETE} lleva {@code appuser = :logRowOwner}, así que
     * no pudo borrar una fila ajena; como {@code sqlExec} no informa las filas
     * afectadas, verdadero no prueba que la fila propia existiera.
     *
     * @return verdadero si se ejecutó la reversión.
     */
    public boolean isLogRowDeleted() {
        return logRowDeleted;
    }

    /**
     * Ordinal, desde 1, de la primera sentencia rechazada entre las enviadas
     * en esta ejecución (ver {@link #getStatementsExecuted()}).
     *
     * @return el ordinal, o nulo si ninguna fue rechazada.
     */
    public Integer getFailedStatementIndex() {
        return failedStatementIndex;
    }

    /**
     * Texto de la primera sentencia rechazada, tal como se envió (antes de
     * reemplazar las constantes de consulta).
     *
     * @return el texto, o nulo si ninguna fue rechazada.
     */
    public String getFailedStatement() {
        return failedStatement;
    }

    /**
     * Unidad de persistencia real del nodo de la primera sentencia rechazada.
     *
     * @return la unidad, o nulo si ninguna fue rechazada.
     */
    public String getFailedPersistUnit() {
        return failedPersistUnit;
    }

    /**
     * Errores de las sentencias rechazadas que se toleraron (política con
     * tolerancia), en orden.
     *
     * @return lista inmutable, vacía si no hubo.
     */
    public List<IErrorReg> getToleratedErrors() {
        return Collections.unmodifiableList(toleratedErrors);
    }

    /**
     * Valores de {@code dataconex} de los nodos que se saltearon por no ser
     * clave de {@link DbScriptUpdateRequest#getSessionByDataconex()}, sin
     * repetir y en el orden en que aparecieron
     * ({@link DbScriptUpdateRequest#DATACONEX_UNSET} para el nodo sin
     * atributo). No incluye los nodos salteados por el atributo
     * {@code motor}.
     *
     * <p>Una petición que mapea solo parte de las secciones de un paquete
     * recibe acá las otras: es lo esperado. Sirve para que el llamador
     * detecte un valor que no conoce (un nodo que, de otro modo, no se
     * ejecutaría en ninguna pasada sin dejar rastro). La firma histórica
     * de 5 argumentos mapea todo valor presente, así que con ella siempre
     * está vacío.</p>
     *
     * @return conjunto inmutable, vacío si no se salteó ninguno.
     */
    public Set<String> getSkippedDataconex() {
        return Collections.unmodifiableSet(skippedDataconex);
    }

    /**
     * Indica si alguna sentencia fue rechazada, tolerada o no.
     *
     * @return verdadero si hubo un rechazo.
     */
    public boolean hasFailures() {
        return failedStatementIndex != null;
    }

    void statementSent() {
        statementsExecuted++;
    }

    void logRowClosed(boolean closed) {
        this.logRowClosed = closed;
    }

    void logRowDeleted(boolean deleted) {
        this.logRowDeleted = deleted;
    }

    void failure(int statementIndex, String statement, String persistUnit) {
        if (failedStatementIndex == null) {
            this.failedStatementIndex = statementIndex;
            this.failedStatement = statement;
            this.failedPersistUnit = persistUnit;
        }
    }

    void tolerated(IErrorReg error) {
        toleratedErrors.add(error);
    }

    void skipped(String dataconex) {
        skippedDataconex.add(dataconex);
    }
}

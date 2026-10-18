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

/**
 * Una sentencia rechazada cortó la ejecución de
 * {@link DBManager#dbScriptUpdateExecute(IGenericDAO, org.javabeanstack.log.ILogManager, DbScriptUpdateRequest)}
 * (política sin tolerancia de errores). Lleva el resultado parcial —qué
 * sentencia falló, en qué unidad de persistencia y si se borró la fila de la
 * bitácora— que de otro modo se perdería con la excepción.
 *
 * <p>La causa es la excepción de la sentencia, o una con el mensaje del
 * error si el DAO no la informó; el mensaje es «ERROR en unidad de
 * persistencia &lt;PU&gt;, SCRIPT &lt;archivo&gt;, &lt;error&gt;».</p>
 *
 * @author Jorge Enciso
 */
public class DbScriptUpdateException extends Exception {

    private static final long serialVersionUID = 1L;

    /** Resultado parcial; no se serializa. */
    private final transient DbScriptUpdateResult result;

    /**
     * Crea la excepción.
     *
     * @param message mensaje del error.
     * @param cause excepción de la sentencia rechazada.
     * @param result resultado parcial de la ejecución.
     */
    public DbScriptUpdateException(String message, Throwable cause, DbScriptUpdateResult result) {
        super(message, cause);
        this.result = result;
    }

    /**
     * Resultado parcial de la ejecución cortada.
     *
     * @return el resultado, o nulo si la excepción se deserializó.
     */
    public DbScriptUpdateResult getResult() {
        return result;
    }
}

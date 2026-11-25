/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*        jenciso@javabeanstack.org
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
package org.javabeanstack.sifen.internal.http;

import org.apache.logging.log4j.ThreadContext;

/**
 * Id de operación del cliente en las líneas del transporte (plan JBSSIF, §5.2/§5.4, I10-09).
 *
 * <p>El id lo genera la fachada al entrar a cada operación ({@code SifenClient}); para que las líneas
 * de {@code BaseRequest}, {@code SoapHelper} y la conexión lo lleven sin cambiar cada firma, la fachada
 * lo abre en el hilo con {@link #open(String)} y lo cierra al salir:</p>
 * <pre>{@code
 * try (OperationContext.Scope scope = OperationContext.open(id)) {
 *     ... // pedidos
 * }
 * }</pre>
 * <p>Se guarda en el {@code ThreadContext} de log4j2 con la clave {@value #KEY}: una aplicación puede
 * mostrarlo en su patrón con {@code %X{sifen.operationId}}. Una petición también lo recibe directamente
 * con {@code BaseRequest.setOperationId}, que tiene prioridad.</p>
 */
public final class OperationContext {
    /** Clave del id de operación en el {@code ThreadContext}. */
    public static final String KEY = "sifen.operationId";
    /** Texto que va en el log cuando no hay id. */
    public static final String NONE = "-";

    /** Ámbito de un id abierto: al cerrarlo vuelve el valor anterior (las operaciones se anidan). */
    @FunctionalInterface
    public interface Scope extends AutoCloseable {
        /** Restaura el id anterior; no lanza. */
        @Override
        void close();
    }

    private OperationContext() {
    }

    /**
     * Fija el id de operación del hilo hasta cerrar el ámbito.
     *
     * @param operationId id; {@code null} borra el del hilo mientras dure el ámbito.
     * @return el ámbito, para un {@code try}-con-recursos.
     */
    public static Scope open(String operationId) {
        String previous = ThreadContext.get(KEY);
        put(operationId);
        return () -> put(previous);
    }

    /**
     * Devuelve el id de operación del hilo.
     *
     * @return el id, o {@code null} si no hay.
     */
    public static String current() {
        return ThreadContext.get(KEY);
    }

    /**
     * Devuelve el id para el log: el indicado si no es {@code null}, si no el del hilo, si no {@value #NONE}.
     *
     * @param explicit id que recibió la petición; puede ser {@code null}.
     * @return el texto.
     */
    public static String label(String explicit) {
        if (explicit != null) {
            return explicit;
        }
        String id = current();
        return id == null ? NONE : id;
    }

    private static void put(String id) {
        if (id == null) {
            ThreadContext.remove(KEY);
        } else {
            ThreadContext.put(KEY, id);
        }
    }
}

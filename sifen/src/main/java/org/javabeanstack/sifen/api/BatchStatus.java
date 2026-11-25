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
package org.javabeanstack.sifen.api;

/**
 * Estado de un lote según SIFEN (contrato v1, plan JBSSIF, §10.2): el de la recepción
 * ({@link #RECEIVED}, {@link #NOT_QUEUED}) y el de la consulta ({@link #IN_PROCESS},
 * {@link #COMPLETED}, {@link #NOT_FOUND}, {@link #EXPIRED}).
 *
 * <p>Enum <b>abierto</b>, como {@link ResultStatus}: el {@code name()} es estable y un valor que la
 * aplicación no conoce se trata como {@link #UNRECOGNIZED}.</p>
 */
public enum BatchStatus {
    /** Lote recibido y encolado (0300). */
    RECEIVED("Lote recibido"),
    /** Lote no encolado (0301): no se procesa. */
    NOT_QUEUED("Lote no encolado"),
    /** Lote en procesamiento (0361): volver a consultar más tarde. */
    IN_PROCESS("Lote en procesamiento"),
    /** Procesamiento concluido (0362): el resultado de cada documento está en la consulta. */
    COMPLETED("Lote procesado"),
    /** Número de lote inexistente (0360). */
    NOT_FOUND("Lote inexistente"),
    /** Consulta extemporánea (0364): pasaron más de 48 h; se resuelve cada CDC por separado. */
    EXPIRED("Consulta extemporánea"),
    /** Código de la respuesta que esta versión no reconoce (D43: se llamaba {@code UNKNOWN}). */
    UNRECOGNIZED("Estado no reconocido");

    private final String description;

    BatchStatus(String description) {
        this.description = description;
    }

    /**
     * Devuelve el texto para mostrar.
     *
     * @return la descripción en español.
     */
    public String getDescription() {
        return description;
    }
}

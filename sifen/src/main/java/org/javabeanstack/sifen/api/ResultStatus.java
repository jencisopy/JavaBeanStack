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
 * Estado de un documento según SIFEN, ya interpretado (contrato v1, plan JBSSIF, §10.2).
 *
 * <p>Enum <b>abierto</b>: una versión menor puede agregar constantes. El {@code name()} es un valor
 * estable (la aplicación puede persistirlo); para mostrar se usa {@link #getDescription()}. Un
 * valor que la aplicación no conoce se trata como {@link #UNRECOGNIZED}.</p>
 */
public enum ResultStatus {
    /** Aprobado ({@code dEstRes} «Aprobado»; consulta de DE 0422). */
    APPROVED("Aprobado"),
    /** Aprobado con observación ({@code dEstRes} «Aprobado con observación»). */
    APPROVED_WITH_OBSERVATION("Aprobado con observación"),
    /** Rechazado ({@code dEstRes} «Rechazado»): los motivos vienen en los mensajes. */
    REJECTED("Rechazado"),
    /** SIFEN todavía lo está procesando (documento dentro de un lote en proceso). */
    IN_PROCESS("En proceso"),
    /** SIFEN no lo tiene o no lo aprobó (consulta de DE 0420). */
    NOT_FOUND("No encontrado"),
    /**
     * Valor de la respuesta que esta versión no reconoce (compatibilidad hacia adelante; D43: se llamaba
     * {@code UNKNOWN}). <b>No</b> es «resultado desconocido»: ese caso es una {@code SifenException} con
     * {@code isOutcomeUnknown()}. Maker deja el documento como resultado desconocido y lo resuelve más tarde.
     */
    UNRECOGNIZED("Estado no reconocido");

    private final String description;

    ResultStatus(String description) {
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

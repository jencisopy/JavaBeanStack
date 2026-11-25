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
package org.javabeanstack.sifen.core.validation;

/**
 * Severidad de una regla de validación previa del DE.
 *
 * <p>Sigue la columna «Tipo» de las reglas de validación del Manual Técnico de SIFEN: una regla de
 * <b>rechazo</b> ({@code R}) es un {@link #ERROR} y una de <b>notificación</b> ({@code N}) es un
 * {@link #WARNING}. Las reglas propias de la librería ({@code JBS-<grupo>-<nn>}) eligen una de las dos
 * con el mismo criterio: ¿SIFEN rechazaría el documento?</p>
 */
public enum Severity {
    /**
     * El documento no se puede generar ni enviar: SIFEN lo rechazaría o la librería no podría armar
     * el XML. Un solo {@code ERROR} hace inválido el {@link ValidationResult}.
     */
    ERROR,
    /**
     * Observación que no impide generar ni enviar el documento (por ejemplo, las notificaciones de
     * la NT 022 o la NT 024). No hace inválido el {@link ValidationResult}.
     */
    WARNING
}

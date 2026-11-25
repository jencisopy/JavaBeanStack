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
 * Qué parte del valor rechazado puede viajar en un {@link ValidationError}.
 *
 * <p>La política es de la <b>regla</b>, no de quien reporta: la declara {@link ValidationRule} y
 * {@link ValidationContext} la aplica al construir el error, de modo que una regla no puede dejar
 * pasar por descuido un dato personal (RUC o documento del receptor, nombres, direcciones, correos,
 * teléfonos). El valor por defecto es {@link #OMIT}.</p>
 *
 * <p>Con cualquier política, el valor <b>nunca</b> aparece en el mensaje, en
 * {@link ValidationError#toString()} ni en {@link SifenValidationException#getMessage()}: solo se
 * obtiene con {@link ValidationError#getValue()}.</p>
 */
public enum ValuePolicy {
    /**
     * No se guarda nada del valor. Es la política por defecto y la obligatoria para todo campo con
     * datos de personas o de seguridad.
     */
    OMIT,
    /**
     * Se guarda solo el largo (texto) o la cantidad de elementos (colección); útil en las reglas de
     * largo sobre campos con datos personales ({@code dNomRec}, {@code dDirRec}…).
     */
    LENGTH_ONLY,
    /**
     * Se guarda el valor, truncado a {@link ValidationError#MAX_VALUE_LENGTH} caracteres. Solo para
     * códigos de dominio, indicadores, monedas, importes y fechas: nunca para datos de personas. Solo
     * se conservan escalares ({@code CharSequence}, {@code Number}, {@code Boolean}, {@code enum},
     * tipos de {@code java.time}); cualquier otro objeto se descarta, aunque la regla diga
     * {@code SHOW}.
     */
    SHOW
}

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

import java.util.Set;

/**
 * Las reglas de un grupo del catálogo, en código: una implementación por grupo del MT (o subgrupo,
 * si el grupo es grande: {@code gDatRec}, {@code gCamItem}…).
 *
 * <p>El parámetro {@code T} es la raíz que se valida: {@code DocumentoElectronico} para
 * {@link DEValidator}; los eventos (§3.5) reutilizan el mismo contrato con su propia raíz.</p>
 *
 * <p><b>Contrato</b> (lo verifica la prueba de mutación de §3.4):</p>
 * <ol>
 * <li><b>Null-safe</b>: {@link #validate} acepta cualquier campo nulo del grafo, incluida la raíz
 * (el motor no llama con raíz nula, pero un subgrupo sí puede faltar). La forma es la de
 * {@link ValidationContext#checkPresent}: el grupo padre ausente se reporta una vez y sus hijos <b>no</b>
 * se evalúan (un faltante da un error, no una cascada).</li>
 * <li><b>Acumula, no corta</b>: reporta con {@link ValidationContext#report} y sigue; nunca lanza
 * por un dato del documento. Si igual escapa una excepción, el motor la convierte en
 * {@link ValidationRule#JBS_VAL_00} y continúa con el grupo siguiente.</li>
 * <li><b>Sin efectos</b>: no modifica el documento ni guarda estado entre llamadas (una instancia se
 * comparte entre hilos).</li>
 * <li><b>Declara sus reglas</b>: {@link #getRules()} devuelve todas las que puede reportar; el motor
 * lo comprueba después de cada corrida (M3-10) y las pruebas cruzan esa lista con el catálogo y con
 * los tests negativos (CA3-03).</li>
 * <li><b>Rutas desde la regla</b>: {@code ctx.report(regla, i, j)} o {@code regla.path(i, j)},
 * nunca una ruta armada a mano (M3-03).</li>
 * </ol>
 *
 * @param <T> tipo de la raíz validada.
 */
public interface GroupValidator<T> {

    /**
     * Devuelve el nombre del grupo, único dentro de un validador ({@code gDatRec},
     * {@code gCamItem}, {@code coherencia-totales}…). Aparece en el error interno
     * {@link ValidationRule#JBS_VAL_00} y en el log.
     *
     * @return el nombre; no nulo ni en blanco.
     */
    String getName();

    /**
     * Devuelve las reglas que este grupo puede reportar.
     *
     * @return conjunto no nulo (vacío solo en grupos de prueba).
     */
    Set<ValidationRule> getRules();

    /**
     * Evalúa las reglas del grupo sobre la raíz y reporta los incumplimientos en el contexto.
     *
     * @param raiz raíz a validar; el motor garantiza que no es {@code null}.
     * @param ctx  contexto de la corrida (configuración y acumulador).
     */
    void validate(T raiz, ValidationContext ctx);
}

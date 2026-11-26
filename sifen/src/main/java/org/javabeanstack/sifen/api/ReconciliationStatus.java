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

import org.javabeanstack.sifen.Provisional;

/**
 * Veredicto de la conciliación de un documento firmado local contra lo que tiene SIFEN (plan JBSSIF, §8.8;
 * M3-30). <b>PROVISIONAL</b>.
 *
 * <p>Enum <b>abierto</b>: una versión menor puede agregar constantes. El {@code name()} es un valor estable
 * (la aplicación puede persistirlo); para mostrar se usa {@link #getDescription()}. Un valor que la aplicación
 * no conoce se trata como {@link #UNDETERMINED}: nunca como {@link #NOT_FOUND}, porque eso invita a
 * reenviar.</p>
 */
@Provisional
public enum ReconciliationStatus {
    /**
     * SIFEN tiene el documento (0422) y su {@code DigestValue} es el del XML local.
     *
     * <p>Coincidir no dice que el documento siga vigente: un documento <b>cancelado</b> (o con otros eventos)
     * también da 0422 con el mismo digest y coincide. La conciliación no mira los eventos; están en
     * {@link ReconciliationItem#getQueryResult()}{@code .getEvents()}.</p>
     */
    MATCH("Coincide"),
    /**
     * SIFEN contestó 0420: <b>no tiene el documento aprobado</b>. El 0420 cubre tres casos que SIFEN no
     * distingue: no lo tiene, lo rechazó o todavía lo procesa. Se llama como {@link ResultStatus#NOT_FOUND}, la
     * consulta de la que sale, y por sí solo <b>no</b> significa «ausente». La conciliación no envía nada.
     *
     * <p><b>Precondición</b> para leerlo como «ausente»: el documento no está en un lote todavía en proceso
     * ni tiene un envío en curso o reciente. En esos casos SIFEN también contesta 0420 mientras procesa, y
     * reenviar arriesga un duplicado o, con un lote, el bloqueo del RUC de 10 a 60 minutos (D41). La
     * aplicación deja esos documentos fuera de la lista; recién con la precondición cumplida un 0420 se
     * resuelve con {@code resolve}.</p>
     */
    NOT_FOUND("Sin aprobación en SIFEN (0420)"),
    /**
     * SIFEN tiene el CDC (0422) con <b>otro</b> {@code DigestValue}: un DE reenviado con otra firma. No se
     * reenvía ni se refirma: lo revisa una persona.
     */
    DIGEST_MISMATCH("Digest distinto"),
    /**
     * No se pudo determinar (D43): falla de transporte o de respuesta (la consulta lanzó una
     * {@code SifenException}), código de SIFEN no reconocido, 0422 sin {@code DigestValue}, documento sin digest
     * local, o documento de otro ambiente que el cliente o con el ambiente del cliente ilegible
     * ({@code JBS-ENT-12}, sin consultar). <b>No</b> es {@link #NOT_FOUND}: se vuelve a
     * conciliar más tarde.
     */
    UNDETERMINED("No se pudo determinar");

    private final String description;

    ReconciliationStatus(String description) {
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

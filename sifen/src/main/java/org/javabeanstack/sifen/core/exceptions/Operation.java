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
package org.javabeanstack.sifen.core.exceptions;

/**
 * Operación pública de la librería en la que ocurrió un error (plan JBSSIF, §3.8, D33 Parte C).
 * La firma y el QR ocurren dentro de {@link #GENERATE_XML} y de las recepciones; su código
 * ({@code JBS-FIRMA-*}) dice que fue en esa etapa.
 */
public enum Operation {
    /** {@code SifenConfig.load(...)} y {@code Sifen.setSifenConfig}. */
    LOAD_CONFIG("Carga de la configuración"),
    /** {@code DocumentoElectronico.generarXml(...)}: validación, armado, firma y QR. */
    GENERATE_XML("Generación del XML del documento"),
    /** {@code DocumentoElectronico.obtenerCDC()}. */
    COMPUTE_CDC("Cálculo del CDC"),
    /** Firma de un {@code rDE} ya armado (reservado para el envío del XML firmado, Fase 4 §4.1). */
    SIGN("Firma del documento"),
    /** {@code Sifen.recepcionDE}. */
    SEND_DE("Envío del documento a SIFEN"),
    /** {@code Sifen.recepcionLoteDE}. */
    SEND_BATCH("Envío del lote a SIFEN"),
    /** {@code Sifen.recepcionEvento}. */
    SEND_EVENTS("Envío de eventos a SIFEN"),
    /** {@code Sifen.consultaDE}. */
    QUERY_DE("Consulta del documento en SIFEN"),
    /** {@code Sifen.consultaLoteDE}. */
    QUERY_BATCH("Consulta del lote en SIFEN"),
    /** {@code Sifen.consultaRUC}. */
    QUERY_RUC("Consulta de RUC en SIFEN"),
    /** {@code new DocumentoElectronico(xml)}. */
    READ_XML("Lectura del XML del documento"),
    /** {@code Sifen.validarFirmaDE*}. */
    VERIFY_SIGNATURE("Validación de la firma digital");

    private final String descripcion;

    Operation(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Devuelve qué estaba haciendo la librería, para mostrar.
     *
     * @return la descripción en español.
     */
    public String getDescription() {
        return descripcion;
    }

    /**
     * Indica si la operación envía algo a SIFEN que puede quedar registrado (recepciones): ante un
     * resultado desconocido no se reenvía a ciegas.
     *
     * @return {@code true} para las recepciones.
     */
    public boolean isSend() {
        return this == SEND_DE || this == SEND_BATCH || this == SEND_EVENTS;
    }
}

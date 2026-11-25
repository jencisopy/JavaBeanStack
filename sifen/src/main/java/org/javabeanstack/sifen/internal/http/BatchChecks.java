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

import java.util.List;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.SifenException;

/**
 * Controles locales de un lote antes de enviarlo (plan JBSSIF, §5.5; M3-16, M3-27).
 *
 * <p>SIFEN admite en un lote un solo tipo de documento y un solo RUC emisor (MT v150 y Guía de Mejores
 * Prácticas); mezclarlos hace rechazar el lote entero después de esperar su procesamiento. El control
 * trabaja sobre los <b>CDC</b>, que llevan los dos datos en posiciones fijas (MT, «Código de Control»:
 * {@code iTiDE} en las posiciones 1–2 y el RUC del emisor en las 3–10), así que sirve igual para
 * {@code SignedDocument.getCdc()} del contrato que para un CDC calculado. Lo llama el envío del lote
 * ({@code sendBatch}, carril F) después de los controles de cantidad ({@code JBS-ENT-01}) y de CDC
 * repetido ({@code JBS-ENT-02}).</p>
 */
public final class BatchChecks {
    /** Largo de un CDC. */
    static final int CDC_LENGTH = 44;

    private BatchChecks() {
    }

    /**
     * Exige que todos los documentos del lote sean del mismo tipo y del mismo emisor.
     *
     * <p>Un CDC nulo o que no tiene 44 caracteres se saltea: lo informa el control del CDC.</p>
     *
     * @param cdcs CDC de los documentos, en el orden del lote; {@code null} o vacía no hace nada.
     * @throws SifenException {@code JBS-ENT-09} con las posiciones (base 1) del primer par que difiere.
     */
    public static void requireSingleTypeAndIssuer(List<String> cdcs) throws SifenException {
        if (cdcs == null) {
            return;
        }
        String tipo = null;
        String ruc = null;
        int primero = -1;
        for (int i = 0; i < cdcs.size(); i++) {
            String cdc = cdcs.get(i);
            if (cdc == null || cdc.length() != CDC_LENGTH) {
                continue;
            }
            String t = typeOf(cdc);
            String r = issuerOf(cdc);
            if (tipo == null) {
                tipo = t;
                ruc = r;
                primero = i;
                continue;
            }
            if (!tipo.equals(t)) {
                throw SifenException.of(ErrorCode.JBS_ENT_09, "tipos " + tipo + " y " + t + " en las posiciones " + primero + " y " + i,
                        null, "documentos " + (primero + 1) + " y " + (i + 1) + ": tipo " + tipo + " y " + t);
            }
            if (!ruc.equals(r)) {
                throw SifenException.of(ErrorCode.JBS_ENT_09, "emisores " + ruc + " y " + r + " en las posiciones " + primero + " y " + i,
                        null, "documentos " + (primero + 1) + " y " + (i + 1) + ": RUC " + ruc + " y " + r);
            }
        }
    }

    /**
     * Tipo de documento del CDC ({@code iTiDE}, dos dígitos).
     *
     * @param cdc CDC de 44 caracteres.
     * @return el tipo, p. ej. {@code 01}.
     */
    public static String typeOf(String cdc) {
        return cdc.substring(0, 2);
    }

    /**
     * RUC del emisor del CDC (sin el dígito verificador).
     *
     * @param cdc CDC de 44 caracteres.
     * @return el RUC de ocho dígitos.
     */
    public static String issuerOf(String cdc) {
        return cdc.substring(2, 10);
    }
}

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
package org.javabeanstack.sifen.internal.facade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.util.SifenUtil;

/**
 * Chequeos de los argumentos de las operaciones (categoría INPUT, {@code JBS-ENT-*}; plan JBSSIF,
 * §3.8, inventario #3–#7): se hacen antes de conectar, para que un dato que SIFEN rechazaría o que
 * terminaba en una excepción cruda tenga su código.
 *
 * <p>Formatos: CDC del MT ({@code dCDC} C 44, con el dígito verificador módulo 11); RUC {@code A
 * 3-8} sin dígito verificador (el MT dice {@code dRUCCons} A 5-8 y el receptor del DE A 3-8: se
 * admite el más amplio para no rechazar un RUC que SIFEN acepte); número de lote numérico de hasta
 * 20 dígitos (el MT dice N 1-15, pero SIFEN-test devolvió 18 dígitos en una respuesta real,
 * {@code real-recepcion-lote-0300.xml}).</p>
 */
public final class Inputs {
    /** Máximo de documentos por lote (MT v150). */
    public static final int MAX_BATCH_SIZE = 50;

    private static final Pattern CDC = Pattern.compile("\\d{44}");
    private static final Pattern RUC = Pattern.compile("[0-9A-Za-z]{3,8}");
    private static final Pattern LOTE = Pattern.compile("\\d{1,20}");

    private Inputs() {
    }

    /**
     * Exige un CDC bien formado.
     *
     * @param cdc CDC a consultar.
     * @throws SifenException {@code JBS-ENT-03}.
     */
    public static void requireCdc(String cdc) throws SifenException {
        if (cdc == null || !CDC.matcher(cdc).matches()
                || !SifenUtil.generateDv(cdc.substring(0, 43)).equals(cdc.substring(43))) {
            throw SifenException.of(ErrorCode.JBS_ENT_03, "CDC de largo " + (cdc == null ? "nulo" : cdc.length()), null);
        }
    }

    /**
     * Exige un RUC bien formado (sin dígito verificador).
     *
     * @param ruc RUC a consultar.
     * @throws SifenException {@code JBS-ENT-04}.
     */
    public static void requireRuc(String ruc) throws SifenException {
        if (ruc == null || !RUC.matcher(ruc).matches()) {
            throw SifenException.of(ErrorCode.JBS_ENT_04, "RUC de largo " + (ruc == null ? "nulo" : ruc.length()), null);
        }
    }

    /**
     * Exige un número de lote numérico.
     *
     * @param lote número de lote.
     * @throws SifenException {@code JBS-ENT-05}.
     */
    public static void requireBatchNumber(String lote) throws SifenException {
        if (lote == null || !LOTE.matcher(lote).matches()) {
            throw SifenException.of(ErrorCode.JBS_ENT_05, "lote de largo " + (lote == null ? "nulo" : lote.length()), null);
        }
    }

    /**
     * Exige un lote de hasta {@value #MAX_BATCH_SIZE} documentos. La lista nula o vacía la informa la
     * validación del contenido ({@code JBS-VAL-01}).
     *
     * @param lote documentos.
     * @throws SifenException {@code JBS-ENT-01}.
     */
    public static void requireBatchSize(List<DocumentoElectronico> lote) throws SifenException {
        if (lote != null && lote.size() > MAX_BATCH_SIZE) {
            throw SifenException.of(ErrorCode.JBS_ENT_01, "documentos en el lote: " + lote.size(), null, lote.size());
        }
    }

    /**
     * Exige que no haya dos documentos con el mismo CDC en el lote. Se llama después de validar el
     * contenido: un documento incompleto (sin CDC calculable) se saltea, lo informa la validación.
     *
     * @param lote documentos.
     * @throws SifenException {@code JBS-ENT-02}, con las posiciones (base 1) de los dos documentos.
     */
    public static void requireUniqueCdcs(List<DocumentoElectronico> lote) throws SifenException {
        if (lote == null) {
            return;
        }
        Map<String, Integer> vistos = new HashMap<>();
        for (int i = 0; i < lote.size(); i++) {
            DocumentoElectronico de = lote.get(i);
            if (de == null) {
                continue;
            }
            String cdc;
            try {
                cdc = de.obtenerCDC();
            } catch (SifenException | RuntimeException e) {
                continue; // incompleto: lo informa la validación del contenido
            }
            Integer anterior = vistos.putIfAbsent(cdc, i);
            if (anterior != null) {
                throw SifenException.of(ErrorCode.JBS_ENT_02, "posiciones " + anterior + " y " + i, null,
                        anterior + 1, i + 1);
            }
        }
    }
}

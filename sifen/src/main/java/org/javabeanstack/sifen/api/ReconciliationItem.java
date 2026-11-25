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

import java.io.Serializable;
import java.util.Objects;
import org.javabeanstack.sifen.Provisional;
import org.javabeanstack.sifen.core.exceptions.SifenException;

/**
 * Veredicto de la conciliación de <b>un</b> documento firmado local contra SIFEN (plan JBSSIF, §8.8; M3-30).
 * <b>PROVISIONAL</b>.
 *
 * <p>Inmutable y serializable. Lleva el documento local, el veredicto, la consulta de SIFEN si la hubo y la
 * excepción si la consulta falló. Los textos nunca son {@code null}.</p>
 */
@Provisional
public final class ReconciliationItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private final SignedDocument document;
    private final ReconciliationStatus status;
    private final DEQueryResult queryResult;
    private final SifenException error;

    private ReconciliationItem(SignedDocument document, ReconciliationStatus status, DEQueryResult queryResult,
            SifenException error) {
        this.document = Objects.requireNonNull(document, "document");
        this.status = status == null ? ReconciliationStatus.UNDETERMINED : status;
        this.queryResult = queryResult;
        this.error = error;
    }

    /**
     * Arma un veredicto (la librería en la conciliación; la aplicación en sus pruebas).
     *
     * @param document    documento firmado local; no puede ser {@code null}.
     * @param status      veredicto; {@code null} = {@link ReconciliationStatus#UNDETERMINED}.
     * @param queryResult consulta de SIFEN, o {@code null} si no la hubo o falló.
     * @param error       falla que impidió determinarlo, o {@code null}.
     * @return el veredicto.
     * @throws NullPointerException si {@code document} es {@code null}.
     */
    public static ReconciliationItem of(SignedDocument document, ReconciliationStatus status, DEQueryResult queryResult,
            SifenException error) {
        return new ReconciliationItem(document, status, queryResult, error);
    }

    /**
     * Devuelve el documento firmado local que se concilió.
     *
     * @return el documento; nunca {@code null}.
     */
    public SignedDocument getDocument() {
        return document;
    }

    /**
     * Devuelve el CDC del documento local.
     *
     * @return el CDC; vacío si el documento no lo tiene.
     */
    public String getCdc() {
        return document.getCdc();
    }

    /**
     * Devuelve el veredicto.
     *
     * @return el veredicto; nunca {@code null}.
     */
    public ReconciliationStatus getStatus() {
        return status;
    }

    /**
     * Devuelve la consulta de SIFEN en la que se basó el veredicto.
     *
     * @return la consulta, o {@code null} si no se consultó (documento de otro ambiente) o si la consulta lanzó
     *         una excepción ({@link #getError()}).
     */
    public DEQueryResult getQueryResult() {
        return queryResult;
    }

    /**
     * Devuelve la falla que impidió determinar el veredicto: la {@code SifenException} de la consulta (transporte,
     * respuesta, CDC inválido) o la del control previo ({@code JBS-ENT-12}, documento de otro ambiente). Las que
     * arma la conciliación llevan id de correlación y quedaron en el log; las de la consulta, las del cliente.
     *
     * @return la excepción, o {@code null} si no la hubo; con {@link ReconciliationStatus#UNDETERMINED} y sin
     *         excepción, el motivo está en {@link #getResponseCode()}.
     */
    public SifenException getError() {
        return error;
    }

    /**
     * Devuelve el código de SIFEN de la consulta ({@code dCodRes}: 0420, 0422 u otro), o el de la respuesta que
     * trae la excepción.
     *
     * @return el código; vacío si no hubo respuesta de SIFEN.
     */
    public String getResponseCode() {
        if (queryResult != null) {
            return queryResult.getResponseCode();
        }
        return error == null || error.getSifenResponseCode() == null ? "" : error.getSifenResponseCode();
    }

    /**
     * Indica si es una discrepancia confirmada por SIFEN: {@link ReconciliationStatus#NOT_FOUND} o
     * {@link ReconciliationStatus#DIGEST_MISMATCH}.
     *
     * @return {@code true} solo con esos dos veredictos.
     */
    public boolean isDiscrepancy() {
        return status == ReconciliationStatus.NOT_FOUND || status == ReconciliationStatus.DIGEST_MISMATCH;
    }

    @Override
    public String toString() {
        return "ReconciliationItem[cdc=" + getCdc() + ", status=" + status + ", code=" + getResponseCode()
                + (error == null ? "" : ", error=" + error.getCode()) + "]";
    }
}

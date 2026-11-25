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

import java.util.List;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.exceptions.SifenExceptionCodes;

/**
 * El documento no superó la validación previa: lleva <b>todos</b> los incumplimientos.
 *
 * <p>Extiende {@link SifenException} (código {@link SifenExceptionCodes#INVALID_DE_CODE}) para que
 * {@code generarXml} y los {@code recepcion*} de la fachada la lancen sin cambiar su {@code throws}
 * (§3.5): quien hoy captura {@code SifenException} la sigue capturando, y quien quiere el detalle
 * captura esta y lee {@link #getResult()}.</p>
 *
 * <p>{@link #getMessage()} resume en español los primeros {@value #MAX_ERRORS_IN_MESSAGE}
 * incumplimientos, sin valores rechazados (va a los logs).</p>
 *
 * <p>Desde §3.8 (D33 Parte C): {@link #getCode()} es el código de la primera regla con error
 * ({@code JBS-CERT-01}, {@code D208c}…) y {@link #getCategory()} sale de ella (CONFIGURATION,
 * CERTIFICATE, INPUT para {@code JBS-VAL-01}, INTERNAL para {@code JBS-VAL-00}, DE_CONTENT el
 * resto). Desde D42 (§11.4) es el único accesor del código: el {@code i405} / {@code i101} del contrato
 * de §3.5 está en {@link #getLegacyCode()}.</p>
 */
@SuppressWarnings("deprecation")
public class SifenValidationException extends SifenException {
    private static final long serialVersionUID = 1L;

    /** Cantidad máxima de incumplimientos que se enumeran en {@link #getMessage()}. */
    public static final int MAX_ERRORS_IN_MESSAGE = 10;

    /** Resultado completo de la validación. */
    private final ValidationResult resultado;

    /** {@code i405} (contenido) o {@code i101} (configuración): el código legado del contrato de §3.5. */
    private final String codigoLegado;

    /**
     * Crea la excepción a partir de un resultado.
     *
     * @param resultado resultado de la validación; {@code null} se toma como vacío. Se espera que
     *                  tenga al menos un {@link Severity#ERROR} (es lo que hace
     *                  {@link ValidationResult#throwIfInvalid()}).
     */
    public SifenValidationException(ValidationResult resultado) {
        this(SifenExceptionCodes.INVALID_DE_CODE, SifenExceptionCodes.INVALID_DE_MESSAGE, resultado);
    }

    private SifenValidationException(String codigoLegado, String encabezado, ValidationResult resultado) {
        this(codigoLegado, encabezado, resultado == null ? ValidationResult.empty() : resultado, true);
    }

    private SifenValidationException(String codigoLegado, String encabezado, ValidationResult r, boolean marca) {
        super(codigoPrincipal(r, codigoLegado), null, resumir(encabezado, r), null, r.getAll(), null, null);
        this.resultado = r;
        this.codigoLegado = codigoLegado;
    }

    /**
     * Crea la excepción de una configuración inválida (§3.7: reglas {@code JBS-CFG-*} y
     * {@code JBS-CERT-*}), con el código legado {@code i101} de la configuración inválida.
     *
     * @param resultado resultado de {@link ConfigValidator}.
     * @return la excepción.
     */
    public static SifenValidationException ofConfiguration(ValidationResult resultado) {
        return new SifenValidationException(SifenExceptionCodes.INVALID_CONFIGURATION_CODE,
                ValidationMessages.text(ValidationMessages.AUX_CONFIGURACION_INVALIDA), resultado);
    }

    /**
     * Devuelve el resultado completo de la validación (errores y avisos).
     *
     * @return el resultado; nunca {@code null}.
     */
    public ValidationResult getResult() {
        return resultado;
    }

    /**
     * Devuelve todos los incumplimientos, errores y avisos, en orden de reporte.
     *
     * @return lista inmodificable.
     */
    @Override
    public List<ValidationError> getErrors() {
        return resultado.getAll();
    }

    /**
     * Devuelve el código {@code iNNN} del contrato de §3.5: {@code i405} si el contenido no superó la
     * validación, {@code i101} si la configuración o el certificado no la superaron. El código estable
     * del error (la primera regla con error, p. ej. {@code JBS-CERT-01}) está en {@link #getCode()}.
     *
     * @return {@code i405} o {@code i101}.
     */
    @Override
    public String getLegacyCode() {
        return codigoLegado;
    }

    /**
     * Código estable de la excepción: el de la primera regla con severidad de error (o el del primer
     * aviso si no hay errores); si el resultado está vacío, el legado.
     */
    private static String codigoPrincipal(ValidationResult r, String legado) {
        if (!r.getErrors().isEmpty()) {
            return r.getErrors().get(0).getCode();
        }
        if (!r.getAll().isEmpty()) {
            return r.getAll().get(0).getCode();
        }
        return legado;
    }

    private static String resumir(String encabezado, ValidationResult resultado) {
        int errores = resultado.getErrors().size();
        int avisos = resultado.getWarnings().size();
        String conteo = errores == 1 ? ValidationMessages.text(ValidationMessages.AUX_RESUMEN_ERROR_UNO)
                : ValidationMessages.text(ValidationMessages.AUX_RESUMEN_ERROR_VARIOS, errores);
        if (avisos > 0) {
            String deAvisos = avisos == 1 ? ValidationMessages.text(ValidationMessages.AUX_RESUMEN_AVISO_UNO)
                    : ValidationMessages.text(ValidationMessages.AUX_RESUMEN_AVISO_VARIOS, avisos);
            conteo = ValidationMessages.text(ValidationMessages.AUX_RESUMEN_Y, conteo, deAvisos);
        }
        StringBuilder sb = new StringBuilder(encabezado);
        sb.append(": ").append(conteo);
        List<ValidationError> todos = resultado.getAll();
        int mostrados = Math.min(todos.size(), MAX_ERRORS_IN_MESSAGE);
        for (int i = 0; i < mostrados; i++) {
            sb.append(i == 0 ? ". " : "; ").append(todos.get(i));
        }
        if (todos.size() > mostrados) {
            sb.append("; ").append(ValidationMessages.text(ValidationMessages.AUX_RESUMEN_MAS, todos.size() - mostrados));
        }
        return sb.toString();
    }
}

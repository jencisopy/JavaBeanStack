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

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.sifen.core.exceptions.ErrorCategory;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.Operation;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.response.UnconvertibleFieldException;
import org.javabeanstack.sifen.internal.util.Traces;

/**
 * Envoltorio único de las operaciones públicas de la librería (plan JBSSIF, §3.8, D33 Parte C): el
 * <b>único</b> lugar donde una excepción imprevista se convierte en {@link SifenException}.
 *
 * <ul>
 *   <li>Lo ya diagnosticado ({@code SifenException} y subclases) sale <b>la misma instancia</b>, con
 *       la operación, la referencia y el id de correlación completados.</li>
 *   <li>Toda {@code RuntimeException} (y un {@code LinkageError}, típico de una SAAJ vieja en el
 *       servidor) pasa por {@link ExceptionTranslator}; si ninguna fila la reconoce sale
 *       {@code JBS-INT-00}. Un {@code VirtualMachineError} (memoria, pila) no se captura.</li>
 *   <li>Log: una línea por error, con el id al principio ({@code [a1b2c3d4] JBS-CERT-01 …}): WARN si
 *       la categoría no es INTERNAL, ERROR con la traza <b>sin mensajes</b> (D27, M1-03, M3-04) si lo
 *       es. El detalle técnico va a DEBUG.</li>
 *   <li>Las operaciones se anidan (una sobrecarga que delega, una lectura de XML dentro de una
 *       consulta): la interna solo traduce y la más externa asigna el id y registra, una vez.</li>
 * </ul>
 */
public final class Operations {
    private static final Logger LOGGER = LogManager.getLogger(Operations.class);

    private static final ThreadLocal<int[]> PROFUNDIDAD = ThreadLocal.withInitial(() -> new int[1]);
    private static final AtomicInteger EN_CURSO = new AtomicInteger();

    /**
     * Cuerpo de una operación.
     *
     * @param <T> resultado.
     */
    @FunctionalInterface
    public interface Action<T> {
        /**
         * Ejecuta la operación.
         *
         * @return el resultado.
         * @throws SifenException si la operación falla con un error diagnosticado.
         */
        T execute() throws SifenException;
    }

    private Operations() {
    }

    /**
     * Ejecuta una operación de la fachada.
     *
     * @param <T>        resultado.
     * @param op         operación.
     * @param referencia CDC, lote o RUC sobre el que se opera (se evalúa solo si hay error); puede
     *                   ser {@code null}.
     * @param accion     cuerpo.
     * @return el resultado de la acción.
     * @throws SifenException con código, categoría e id de correlación.
     */
    public static <T> T execute(Operation op, Supplier<String> referencia, Action<T> accion) throws SifenException {
        int[] profundidad = PROFUNDIDAD.get();
        if (profundidad[0] > 0) {
            // anidada: solo traduce; la externa asigna el id y registra
            try {
                return accion.execute();
            } catch (RuntimeException | LinkageError e) {
                throw translate(e, op);
            }
        }
        profundidad[0]++;
        EN_CURSO.incrementAndGet();
        try {
            return accion.execute();
        } catch (SifenException e) {
            throw registrar(e, op, referencia);
        } catch (RuntimeException | LinkageError e) {
            throw registrar(translate(e, op), op, referencia);
        } finally {
            EN_CURSO.decrementAndGet();
            profundidad[0]--;
        }
    }

    /**
     * Ejecuta una operación que por contrato no lanza (p. ej. {@code validarFirmaDE}): un error,
     * diagnosticado o imprevisto, se registra igual que en {@link #execute} y se convierte en el
     * resultado de falla.
     *
     * @param <T>        resultado.
     * @param op         operación.
     * @param referencia referencia (se evalúa solo si hay error).
     * @param accion     cuerpo.
     * @param siFalla    arma el resultado a partir de la excepción ya registrada.
     * @return el resultado de la acción o el de la falla.
     */
    public static <T> T evaluate(Operation op, Supplier<String> referencia, Action<T> accion,
            Function<SifenException, T> siFalla) {
        try {
            return execute(op, referencia, accion);
        } catch (SifenException e) {
            return siFalla.apply(e);
        }
    }

    /**
     * Cantidad de operaciones de la fachada en curso en todos los hilos (para {@code JBS-EST-01}).
     *
     * @return la cantidad, incluida la del hilo que pregunta.
     */
    public static int inProgress() {
        return EN_CURSO.get();
    }

    /**
     * Genera un id de correlación: 8 caracteres hexadecimales.
     *
     * @return el id.
     */
    public static String newId() {
        return String.format("%08x", ThreadLocalRandom.current().nextInt());
    }

    /**
     * Convierte una excepción imprevista en {@link SifenException} con el código del traductor.
     *
     * @param e  excepción.
     * @param op operación (elige la etapa: la lectura de XML tiene sus filas).
     * @return la excepción con código; {@code JBS-INT-00} si el traductor no la reconoce.
     */
    public static SifenException translate(Throwable e, Operation op) {
        ExceptionTranslator.Stage etapa = op == Operation.READ_XML ? ExceptionTranslator.Stage.XML_READ
                : ExceptionTranslator.Stage.OPERATION;
        ErrorCode codigo = ExceptionTranslator.translate(e, etapa).orElse(ErrorCode.JBS_INT_00);
        SifenException x = SifenException.of(codigo, detail(e), e, campo(e));
        // §10.7 (D43): un tiempo de lectura o un corte que no pasó por el transporte (que sabe si escribió) pudo
        // haber llegado a SIFEN: en la duda, se marca como enviado
        return codigo == ErrorCode.JBS_CONEX_05 || codigo == ErrorCode.JBS_CONEX_07 ? x.markSent() : x;
    }

    /**
     * Detalle técnico de una excepción: clase y mensaje de cada causa (solo para el getter y DEBUG).
     *
     * @param e excepción.
     * @return el detalle.
     */
    public static String detail(Throwable e) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (Throwable c = e; c != null && n < 5; c = c.getCause() == c ? null : c.getCause(), n++) {
            if (n > 0) {
                sb.append(" <- ");
            }
            sb.append(c.getClass().getName());
            if (c.getMessage() != null) {
                sb.append(": ").append(c.getMessage());
            }
        }
        return sb.toString();
    }

    /** Ruta del campo que no se pudo convertir, si la excepción viene de la lectura de un nodo. */
    private static String campo(Throwable e) {
        for (Throwable c = e; c != null; c = c.getCause() == c ? null : c.getCause()) {
            if (c instanceof UnconvertibleFieldException cnc) {
                return cnc.getPath();
            }
        }
        return "";
    }

    private static SifenException registrar(SifenException e, Operation op, Supplier<String> referencia) {
        e.withContext(op, referencia(referencia), newId());
        String id = e.getCorrelationId();
        if (e.getCategory() == ErrorCategory.INTERNAL) {
            Throwable foco = e.getCause() != null ? e.getCause() : e;
            LOGGER.error("[{}] {} {} ref={}: {} ({})", id, e.getCode(), e.getOperation(), e.getReference(),
                    e.getUserMessage(), foco.getClass().getName(), Traces.withoutMessage(foco));
        } else {
            LOGGER.warn("[{}] {} {} ref={}: {}", id, e.getCode(), e.getOperation(), e.getReference(),
                    e.getUserMessage());
        }
        if (e.getTechnicalDetail() != null && LOGGER.isDebugEnabled()) {
            LOGGER.debug("[{}] detalle técnico: {}", id, e.getTechnicalDetail());
        }
        return e;
    }

    private static String referencia(Supplier<String> referencia) {
        if (referencia == null) {
            return null;
        }
        try {
            return referencia.get();
        } catch (RuntimeException e) {
            return null;
        }
    }
}

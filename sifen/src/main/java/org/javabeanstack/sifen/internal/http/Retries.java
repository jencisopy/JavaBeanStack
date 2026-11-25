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

import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.SifenException;

/**
 * Política de reintentos de las consultas (plan JBSSIF, §5.2; D41).
 *
 * <p>Solo se reintenta una <b>consulta</b> (RUC, DE por CDC, lote): la decide la petición
 * ({@code BaseRequest.isRetryable()}), y una recepción nunca reintenta sola (D41: un reenvío a ciegas
 * arriesga un duplicado; el resultado desconocido se resuelve con {@code resolve}). De las fallas de una
 * consulta se reintentan las <b>rápidas</b> de transporte, que un intento nuevo con contexto TLS y
 * socket nuevos puede salvar:</p>
 * <ul>
 *   <li>{@code JBS-CONEX-03}: conexión rechazada o sin ruta;</li>
 *   <li>{@code JBS-CONEX-06}: TLS (el contexto se rearmó: sirve si el certificado cambió);</li>
 *   <li>{@code JBS-CONEX-07}: corte durante el intercambio (el socket viejo de la caché keep-alive);</li>
 *   <li>{@code JBS-CONEX-09}: HTTP 5xx sin respuesta SOAP (un balanceador o un reinicio de SIFEN).</li>
 * </ul>
 * <p>No se reintentan los tiempos agotados ({@code JBS-CONEX-04/05}: ya consumieron la espera y
 * repetirlos multiplica el tiempo del hilo del usuario, M3-33), el DNS y el proxy (configuración), los
 * 4xx, lo que no es SOAP ni una respuesta de SIFEN interpretada (un {@code soap:Fault}, un rechazo). La
 * espera arranca en {@code getRetryBackoffMs()} y se duplica, con un tope de 60 s.</p>
 */
public final class Retries {
    /** Tope de una espera entre intentos. */
    static final long MAX_DELAY_MS = 60_000L;

    private Retries() {
    }

    /**
     * Indica si la falla de una consulta se puede reintentar.
     *
     * @param e falla.
     * @return {@code true} si es una falla rápida de transporte.
     */
    public static boolean isRetryable(SifenException e) {
        // §10.3 (carril F, I10-14): un switch en vez del conjunto estático; mismos cuatro códigos.
        if (e == null || e.getCode() == null) {
            return false;
        }
        String code = e.getCode();
        return code.equals(ErrorCode.JBS_CONEX_03.getCode()) || code.equals(ErrorCode.JBS_CONEX_06.getCode())
                || code.equals(ErrorCode.JBS_CONEX_07.getCode()) || code.equals(ErrorCode.JBS_CONEX_09.getCode());
    }

    /**
     * Espera antes del reintento número {@code attempt} (base 0).
     *
     * @param config  configuración.
     * @param attempt reintento, desde 0.
     * @return milisegundos.
     */
    public static long delayMs(SifenConfig config, int attempt) {
        long base = Math.max(0, config.getRetryBackoffMs());
        long d = base << Math.min(attempt, 20);
        return Math.min(d, MAX_DELAY_MS);
    }

    /**
     * Duerme el hilo; si lo interrumpen, restaura la marca y avisa para que no se siga reintentando.
     *
     * @param millis milisegundos.
     * @return {@code false} si el hilo fue interrumpido.
     */
    public static boolean pause(long millis) {
        if (millis <= 0) {
            return !Thread.currentThread().isInterrupted();
        }
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}

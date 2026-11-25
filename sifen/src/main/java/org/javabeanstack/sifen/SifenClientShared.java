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
package org.javabeanstack.sifen;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Estado que comparten los clientes de un mismo {@link SifenClientRegistry} (plan JBSSIF, §10.7, M10-03,
 * I10-19): el bloqueo de envíos en curso por CDC y la secuencia de {@code Id} de evento. Así el cliente que
 * reemplaza a otro tras {@code invalidate} no transmite a la vez el mismo CDC que el viejo ni repite sus
 * {@code Id}. Un cliente creado sin registro tiene el suyo. No es estático: es de la instancia del registro.
 * Seguro entre hilos. No es parte del contrato (package-private).
 */
final class SifenClientShared {
    /** Mayor {@code Id} de evento del MT ({@code rEve@Id}: hasta 10 dígitos). */
    static final long MAX_EVENT_ID = 9_999_999_999L;

    /** CDC → id de la operación que lo está transmitiendo. */
    private final Map<String, String> inFlight = new ConcurrentHashMap<>();
    private final AtomicLong eventSequence = new AtomicLong();

    /**
     * Toma el envío en curso de un CDC.
     *
     * @param cdc         CDC.
     * @param operationId id de la operación.
     * @return {@code true} si lo tomó; {@code false} si ya había otro envío en curso.
     */
    boolean acquire(String cdc, String operationId) {
        return inFlight.putIfAbsent(cdc, operationId) == null;
    }

    /**
     * Libera el envío en curso de un CDC, solo si lo tiene esa operación.
     *
     * @param cdc         CDC.
     * @param operationId id de la operación.
     */
    void release(String cdc, String operationId) {
        inFlight.remove(cdc, operationId);
    }

    /**
     * Próximo {@code Id} de evento: los milisegundos del reloj (módulo 10<sup>10</sup>, diez dígitos) o el
     * anterior más uno si es mayor, así que crece dentro de la secuencia y no se repite tras un reinicio
     * salvo que coincida el milisegundo. Dos WAR o nodos del mismo emisor pueden repetirlo: no es una clave
     * (D43).
     *
     * @param clock reloj.
     * @return el {@code Id}, de 1 a 10 dígitos.
     */
    String nextEventId(Clock clock) {
        long now = Math.floorMod(clock.millis(), MAX_EVENT_ID + 1);
        return String.valueOf(eventSequence.updateAndGet(prev -> following(prev, now)));
    }

    /**
     * El {@code Id} que daría ahora {@link #nextEventId(Clock)}, <b>sin avanzar la secuencia</b> (I8-34, ESC-35):
     * es el que supone {@code ISifenClient.validateEvents} para un evento sin {@code Id}. No lo reserva: otra
     * operación puede tomarlo antes, así que {@code sendEvents} puede enviar otro; los dos cumplen la misma
     * regla ({@code JBS-EVENTO-01}: de 1 a 10 dígitos).
     *
     * @param clock reloj.
     * @return el {@code Id} supuesto, de 1 a 10 dígitos.
     */
    String peekNextEventId(Clock clock) {
        long now = Math.floorMod(clock.millis(), MAX_EVENT_ID + 1);
        return String.valueOf(following(eventSequence.get(), now));
    }

    /** Regla única del siguiente {@code Id} (la usan {@code nextEventId} y {@code peekNextEventId}). */
    private static long following(long prev, long now) {
        long n = Math.max(prev + 1, now);
        // al pasar de diez dígitos vuelve al reloj (o a 1 si el reloj marca justo el anterior)
        return n > MAX_EVENT_ID ? (now == prev ? 1 : Math.max(1, now)) : n;
    }
}

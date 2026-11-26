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
import java.util.List;
import org.javabeanstack.sifen.Provisional;

/**
 * Diagnóstico de la conexión con SIFEN por etapas (proxy, DNS, TCP, TLS, HTTP), sin enviar ningún
 * documento (contrato v1, plan JBSSIF, §10.2; {@code ISifenClient.checkConnection}, que implementa
 * §5.3). Inmutable y serializable.
 *
 * <p><b>PROVISIONAL</b> en su detalle: los nombres y la cantidad de etapas siguen provisionales en la 2.2.0
 * y pueden cambiar dentro de {@code 2.x}; {@link #isOk()}, {@link #getSteps()} y {@link #getOperationId()} son
 * estables.</p>
 */
@Provisional
public final class ConnectionDiagnosis implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Step> steps;
    private final String operationId;

    private ConnectionDiagnosis(List<Step> steps, String operationId) {
        this.steps = ApiValues.list(steps);
        this.operationId = ApiValues.text(operationId);
    }

    /**
     * Crea un diagnóstico.
     *
     * @param steps       etapas en el orden en que se probaron.
     * @param operationId id de la operación.
     * @return el diagnóstico.
     */
    public static ConnectionDiagnosis of(List<Step> steps, String operationId) {
        return new ConnectionDiagnosis(steps, operationId);
    }

    /**
     * Indica si todas las etapas salieron bien.
     *
     * @return {@code true} si hay al menos una etapa y ninguna falló.
     */
    public boolean isOk() {
        if (steps.isEmpty()) {
            return false;
        }
        for (Step step : steps) {
            if (!step.isOk()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Devuelve las etapas, en el orden en que se probaron.
     *
     * @return lista inmodificable.
     */
    public List<Step> getSteps() {
        return steps;
    }

    /**
     * Devuelve el id de la operación: el mismo que encabeza las líneas del log.
     *
     * @return ocho caracteres hexadecimales.
     */
    public String getOperationId() {
        return operationId;
    }

    @Override
    public String toString() {
        return "ConnectionDiagnosis[ok=" + isOk() + ", " + steps + ", op=" + operationId + "]";
    }

    /** Una etapa del diagnóstico. Inmutable y serializable. */
    public static final class Step implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String name;
        private final boolean ok;
        private final long durationMs;
        private final String code;
        private final String message;

        private Step(String name, boolean ok, long durationMs, String code, String message) {
            this.name = ApiValues.text(name);
            this.ok = ok;
            this.durationMs = durationMs;
            this.code = ApiValues.text(code);
            this.message = ApiValues.text(message);
        }

        /**
         * Crea una etapa que salió bien.
         *
         * @param name       nombre de la etapa ({@code PROXY}, {@code DNS}, {@code TCP}, {@code TLS}, {@code HTTP}).
         * @param durationMs tiempo de la etapa.
         * @return la etapa.
         */
        public static Step ok(String name, long durationMs) {
            return new Step(name, true, durationMs, "", "");
        }

        /**
         * Crea una etapa que falló.
         *
         * @param name       nombre de la etapa.
         * @param durationMs tiempo hasta la falla.
         * @param code       código del error ({@code JBS-CONEX-*}).
         * @param message    mensaje para pantalla.
         * @return la etapa.
         */
        public static Step failed(String name, long durationMs, String code, String message) {
            return new Step(name, false, durationMs, code, message);
        }

        /**
         * Devuelve el nombre de la etapa.
         *
         * @return el nombre.
         */
        public String getName() {
            return name;
        }

        /**
         * Indica si la etapa salió bien.
         *
         * @return {@code true} si salió bien.
         */
        public boolean isOk() {
            return ok;
        }

        /**
         * Devuelve el tiempo de la etapa.
         *
         * @return milisegundos.
         */
        public long getDurationMs() {
            return durationMs;
        }

        /**
         * Devuelve el código del error de la etapa.
         *
         * @return el código ({@code JBS-CONEX-*}); vacío si salió bien.
         */
        public String getCode() {
            return code;
        }

        /**
         * Devuelve el mensaje para pantalla del error de la etapa.
         *
         * @return el mensaje; vacío si salió bien.
         */
        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return name + (ok ? " ok" : " " + code) + " " + durationMs + " ms";
        }
    }
}

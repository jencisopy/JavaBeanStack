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
package org.javabeanstack.sifen.simulator;

import org.javabeanstack.sifen.core.SifenConfig;

/**
 * Servicios web de SIFEN que atiende {@link SifenSimulator}, con la ruta por omisión de
 * {@link SifenConfig} (plan JBSSIF, §8.6). El simulador reconoce el servicio por el final de la ruta,
 * así que acepta un prefijo de empresa ({@code /empresa-a/de/ws/...}).
 */
public enum SimulatedService {
    /** Recepción sincrónica de un DE ({@code recibe}). */
    RECEIVE_DE(new SifenConfig().getSendPath()),
    /** Recepción de un lote ({@code recibe-lote}). */
    RECEIVE_BATCH(new SifenConfig().getSendBatchPath()),
    /** Recepción de eventos ({@code evento}). */
    EVENT(new SifenConfig().getEventPath()),
    /** Consulta de un DE por CDC ({@code consulta}). */
    QUERY_DE(new SifenConfig().getQueryDEPath()),
    /** Consulta de RUC ({@code consulta-ruc}). */
    QUERY_RUC(new SifenConfig().getQueryRucPath()),
    /** Consulta de lote ({@code consulta-lote}). */
    QUERY_BATCH(new SifenConfig().getQueryBatchPath());

    private final String path;

    SimulatedService(String path) {
        this.path = path;
    }

    /**
     * Ruta del servicio (sin prefijo de empresa).
     *
     * @return la ruta, p. ej. {@code /de/ws/sync/recibe.wsdl}.
     */
    public String getPath() {
        return path;
    }

    /**
     * Servicio de una ruta recibida.
     *
     * @param requestPath ruta del pedido, con o sin prefijo de empresa.
     * @return el servicio, o {@code null} si la ruta no es de ninguno.
     */
    public static SimulatedService of(String requestPath) {
        if (requestPath == null) {
            return null;
        }
        SimulatedService best = null;
        for (SimulatedService s : values()) {
            // la más larga gana: /consulta-lote.wsdl no es /consulta.wsdl, pero por las dudas
            if (requestPath.endsWith(s.path) && (best == null || s.path.length() > best.path.length())) {
                best = s;
            }
        }
        return best;
    }

    /**
     * Empresa de una ruta: lo que va antes de la ruta del servicio, sin las barras.
     *
     * @param requestPath ruta del pedido.
     * @return la empresa, o {@code ""} si no hay prefijo.
     */
    String tenantOf(String requestPath) {
        String prefix = requestPath.substring(0, requestPath.length() - path.length());
        while (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }
}

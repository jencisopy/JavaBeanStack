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
package org.javabeanstack.sifen.spi;

import org.javabeanstack.sifen.Provisional;

/**
 * Registro de envíos de la aplicación, que la librería consulta antes de transmitir un documento
 * (plan JBSSIF, §10.2, D41; M3-26). <b>PROVISIONAL</b> hasta la release 2.2.0: puede cambiar con
 * aviso en la bitácora del plan; la aplicación lo implementa detrás de una clase propia.
 *
 * <p>El bloqueo contra el doble envío <b>entre WAR o entre nodos</b> es de la aplicación: su transición
 * de estado en la base ({@code UPDATE … WHERE estado = <anterior>} con una fila afectada). Este
 * enganche le permite a la librería preguntar por esa transición antes de transmitir; dentro de un
 * mismo {@code SifenClientRegistry} (o de un cliente creado sin registro) la librería ya rechaza un segundo
 * envío simultáneo del mismo CDC ({@code JBS-ENT-08}). Se registra con {@code SifenClient.create(config, ISendRegistry)} o con
 * {@code new SifenClientRegistry(ISendRegistry)}; el cliente lo consulta antes de transmitir cada DE (en
 * {@code send}, {@code resolve} y por cada CDC de {@code sendBatch}): {@code false} termina la operación
 * con {@code JBS-ENT-13} sin contactar a SIFEN (D43), y una excepción de la implementación sale como
 * {@code JBS-INT-00}, sin marcar el pedido como enviado (§10.4).</p>
 *
 * <p>Las implementaciones deben ser seguras entre hilos.</p>
 */
@Provisional
public interface ISendRegistry {

    /**
     * Pregunta si el documento se puede transmitir ahora.
     *
     * @param cdc         CDC del documento (o de cada documento de un lote).
     * @param operationId id de la operación de la librería, para correlacionar con el log.
     * @return {@code true} si la aplicación autoriza la transmisión; {@code false} hace que la
     *         operación termine con {@code JBS-ENT-13} sin contactar a SIFEN.
     */
    boolean allowSend(String cdc, String operationId);
}

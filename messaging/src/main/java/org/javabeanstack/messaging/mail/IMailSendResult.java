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
package org.javabeanstack.messaging.mail;

import java.util.List;
import org.javabeanstack.error.IErrorReg;

/**
 * Resultado de un intento de envío de correo.
 *
 * <p>Distingue un fallo <b>recuperable</b> ({@link #isRetryable()}: un problema
 * de red o un servidor ocupado, que conviene reintentar) de uno <b>definitivo</b>
 * (una dirección inválida o una autenticación rechazada, que no mejora con
 * reintentos). La Fase 3 usa esa distinción para decidir entre reencolar y dar el
 * mensaje por fallado.</p>
 *
 * @author Jorge Enciso
 */
public interface IMailSendResult {

    /**
     * Indica si el envío fue exitoso.
     * @return verdadero si el servidor aceptó el mensaje.
     */
    boolean isSuccessful();

    /**
     * Indica si el error admite reintento.
     * @return verdadero si conviene reintentar (fallo de red, servidor ocupado).
     */
    boolean isRetryable();

    /**
     * Devuelve el {@code Message-ID} que asignó el servidor.
     * @return identificador del mensaje, o nulo si el envío falló.
     */
    String getMessageId();

    /**
     * Devuelve el error del intento fallido.
     * @return registro de error, o nulo si el envío fue exitoso.
     */
    IErrorReg getErrorReg();

    /**
     * Devuelve las direcciones que el servidor rechazó.
     * @return lista de direcciones inválidas; nunca nula.
     */
    List<String> getInvalidAddresses();
}

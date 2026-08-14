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
package org.javabeanstack.outputs;

import org.javabeanstack.error.IErrorReg;

/**
 * Contrato de un <b>destino</b> de documentos: la pieza que sabe entregar un
 * {@link IOutputDocument} ya generado por su canal. Cada canal es una
 * implementación:
 *
 * <ul>
 * <li>descarga por la respuesta HTTP (el único destino autorizado a tocarla);</li>
 * <li>correo, como adjunto de un mensaje
 * ({@code org.javabeanstack.messaging});</li>
 * <li>una carpeta del servidor;</li>
 * <li>a futuro: base de datos, un sistema de gestión documental, un bucket S3,
 * WhatsApp.</li>
 * </ul>
 *
 * <p>
 * El destino <b>no genera nada</b>: recibe el documento terminado. Recién acá
 * es válido comprometer recursos externos (la respuesta HTTP, el servidor
 * SMTP, el file system); si la generación falló, ningún destino llega a
 * ejecutarse y el canal queda intacto.
 * </p>
 *
 * <p>
 * Las implementaciones se configuran con su API propia antes de la entrega:
 * los destinatarios y el asunto en el correo, la carpeta en el file system.
 * </p>
 *
 * @author Jorge Enciso
 */
public interface IDocumentTarget {

    /**
     * Entrega el documento por el canal de este destino.
     *
     * <p>
     * Devuelve el resultado como {@link IErrorReg} en lugar de propagar la
     * excepción cuando el fallo es propio del canal (SMTP caído, carpeta sin
     * permisos): así una entrega a varios destinos puede continuar con los
     * demás y el llamador decide qué informar. Las condiciones de programación
     * (documento nulo o sin contenido) sí se lanzan.
     * </p>
     *
     * @param document documento generado, con nombre, contenido y tipo MIME.
     * @return resultado de la entrega; sin error si fue exitosa, con el código
     * y mensaje del fallo si no.
     * @throws Exception ante condiciones de uso inválido (documento nulo o
     * vacío, destino sin configurar).
     */
    IErrorReg deliver(IOutputDocument document) throws Exception;

    /**
     * Devuelve el nombre corto del canal ({@code download}, {@code mail},
     * {@code folder}, …), para los mensajes al usuario y para el registro en
     * el log de la aplicación.
     *
     * @return nombre del canal de este destino.
     */
    String getChannelName();
}

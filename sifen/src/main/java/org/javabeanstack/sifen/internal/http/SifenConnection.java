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

import jakarta.xml.soap.SOAPMessage;
import javax.net.ssl.SSLContext;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.SOAPResponse;

/**
 * Transporte de un cliente de SIFEN (plan JBSSIF, §5.2, D37): envía un mensaje SOAP por HTTP(S) y
 * devuelve la respuesta, con su contexto TLS propio.
 *
 * <p>La conexión <b>no guarda la configuración</b>: la recibe en cada pedido y de ella toma la URL,
 * los tiempos de espera, el proxy y el certificado de cliente. Lo que guarda es el contexto TLS y el
 * almacén de certificados, que:</p>
 * <ul>
 *   <li>se arman en el primer pedido HTTPS;</li>
 *   <li>se rearman si el certificado de la configuración cambió (otro archivo, otro contenido, otra
 *       fecha de modificación: lo detecta el almacén por su huella);</li>
 *   <li>se descartan ante una falla de transporte (DNS, TCP, TLS, corte, tiempo agotado), para que el
 *       pedido siguiente arme todo de nuevo y no reutilice un socket o una sesión muerta;</li>
 *   <li>se descartan con {@link #invalidate()} y con {@link #close()}.</li>
 * </ul>
 * <p>Un pedido después de {@link #close()} vuelve a armar lo que necesite: cerrar libera, no inhabilita.
 * Es segura entre hilos y no usa estado estático mutable.</p>
 *
 * <p>Dos maneras de obtenerla:</p>
 * <ul>
 *   <li>{@link #create()}: una conexión nueva, para que el cliente la guarde, la pase a cada pedido y la
 *       cierre en su {@code close()} (lo que pide CA10-04: sin registro estático en el camino);</li>
 *   <li>{@link #of(SifenConfig)}: la conexión <b>de esa instancia</b> de configuración, para quien no
 *       pasa una explícita (la fachada heredada {@code Sifen} y las peticiones armadas sin conexión). Se
 *       guarda en un mapa débil por identidad: dos objetos {@code SifenConfig} distintos nunca comparten
 *       contexto TLS (E3-31), y la entrada desaparece cuando la configuración deja de usarse.</li>
 * </ul>
 */
public interface SifenConnection extends AutoCloseable {

    /**
     * Crea una conexión nueva, sin contexto TLS todavía.
     *
     * @return la conexión.
     */
    static SifenConnection create() {
        return new UrlSifenConnection();
    }

    /**
     * Devuelve la conexión asociada a esta instancia de configuración, creándola si hace falta.
     *
     * @param config configuración; {@code null} da una conexión propia de las llamadas sin
     *               configuración (la operación falla antes por la validación).
     * @return la conexión de esa configuración.
     */
    static SifenConnection of(SifenConfig config) {
        return DefaultConnections.of(config);
    }

    /**
     * Cierra y olvida la conexión asociada a esta instancia de configuración (para el {@code close()} de
     * un cliente que todavía no pasa una conexión explícita). No hace nada si no había.
     *
     * @param config configuración.
     */
    static void release(SifenConfig config) {
        DefaultConnections.release(config);
    }

    /**
     * Envía el mensaje y devuelve la respuesta (plan JBSSIF, §3.8 y §5.2).
     *
     * <p>Clasificación: {@code JBS-CFG-03/04} si la URL no sirve; las {@code IOException} pasan por
     * {@code ExceptionTranslator} ({@code JBS-CONEX-01..07}; con el proxy de la configuración, la falla al
     * llegar al proxy es {@code JBS-CONEX-02}); un 407 del proxy, {@code JBS-CONEX-02}; un cuerpo que no
     * es SOAP, {@code JBS-CONEX-08} (4xx), {@code JBS-CONEX-09} (5xx) o {@code JBS-CONEX-10} (2xx); un 4xx/5xx
     * sin cuerpo, {@code JBS-CONEX-08}/{@code JBS-CONEX-09}; un 2xx sin cuerpo, {@code JBS-RESP-02}. Un
     * 4xx/5xx con cuerpo SOAP se devuelve: lo interpreta la petición. Toda falla posterior a escribir el
     * pedido sale marcada como enviada ({@code isOutcomeUnknown()}, I3-48).</p>
     *
     * @param config  configuración del pedido.
     * @param url     URL completa del servicio.
     * @param message mensaje a enviar.
     * @return la respuesta con estado, mensaje SOAP y bytes recibidos.
     * @throws SifenException con el código de la falla.
     */
    SOAPResponse exchange(SifenConfig config, String url, SOAPMessage message) throws SifenException;

    /**
     * Devuelve el contexto TLS que esta conexión usa para la configuración: el de su certificado de
     * cliente o, si la configuración no usa certificado de cliente, el de la JVM (nunca el de otra
     * configuración, E3-31).
     *
     * @param config configuración.
     * @return el contexto.
     * @throws SifenException con el código de la regla ({@code JBS-CERT-*}) si el certificado no se puede
     *                        cargar, o {@code JBS-INT-00}.
     */
    SSLContext tlsContext(SifenConfig config) throws SifenException;

    /**
     * Descarta el contexto TLS y los certificados cargados: el próximo pedido los vuelve a armar.
     */
    void invalidate();

    /**
     * Libera el contexto TLS y los certificados (como {@link #invalidate()}); no lanza y se puede llamar
     * más de una vez.
     */
    @Override
    void close();
}

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

import jakarta.xml.soap.SOAPException;
import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.net.ssl.SSLException;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.transform.TransformerException;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;

/**
 * Tabla única «excepción del JDK → código» de la librería (plan JBSSIF, §3.8, D33 Parte C).
 *
 * <p>Cada fila dice en qué {@link Stage etapas} aplica, qué excepción reconoce y qué
 * {@link ErrorCode} le corresponde. Se recorre la cadena de causas desde la más externa y, para
 * cada excepción, las filas en orden: gana la primera que coincide. Un caso nuevo que aparezca en un
 * cliente es <b>una constante en {@code ErrorCode} y una fila acá</b>; la fachada no se toca.</p>
 *
 * <p>Fase 5 (§5.4): la etapa {@link Stage#PROXY_CONNECT} clasifica como {@code JBS-CONEX-02} lo
 * que falla al conectar con el proxy <b>de la configuración</b> (nombre que no se resuelve, conexión
 * rechazada, tiempo agotado): sin ella esas fallas salían como DNS o TCP de SIFEN. El transporte la
 * prueba primero y, si ninguna fila coincide, sigue con {@link Stage#TRANSPORT}.</p>
 */
public final class ExceptionTranslator {

    /** Dónde ocurrió la excepción: algunas filas solo valen en una etapa. */
    public enum Stage {
        /** Dentro de una operación de la fachada, fuera de las etapas siguientes. */
        OPERATION,
        /** Al leer el XML de un documento ({@code new DocumentoElectronico(xml)}). */
        XML_READ,
        /** Al conectar, enviar o recibir por HTTP. */
        TRANSPORT,
        /** Al conectar con el proxy declarado en la configuración, antes de llegar a SIFEN (§5.4). */
        PROXY_CONNECT,
        /** Al interpretar la respuesta de SIFEN. */
        RESPONSE
    }

    /** Una fila de la tabla. */
    private record Fila(String descripcion, Set<Stage> etapas, Predicate<Throwable> coincide, ErrorCode codigo) {
    }

    private static final Set<Stage> TODAS = java.util.Collections.unmodifiableSet(EnumSet.allOf(Stage.class));

    private static final List<Fila> FILAS = filas();

    private ExceptionTranslator() {
    }

    private static List<Fila> filas() {
        List<Fila> f = new ArrayList<>();
        Set<Stage> red = EnumSet.of(Stage.TRANSPORT);
        // ---------------------------------------------------------------- conexión (↔ Type de §5.4)
        f.add(new Fila("DNS: el nombre no se resuelve", red, UnknownHostException.class::isInstance, ErrorCode.JBS_CONEX_01));
        f.add(new Fila("proxy: túnel rechazado o 407", red, t -> t instanceof IOException && contiene(t, "proxy", "407"),
                ErrorCode.JBS_CONEX_02));
        f.add(new Fila("tiempo agotado al conectar", red, t -> t instanceof SocketTimeoutException && contiene(t, "connect"),
                ErrorCode.JBS_CONEX_04));
        f.add(new Fila("tiempo agotado al leer", red, SocketTimeoutException.class::isInstance, ErrorCode.JBS_CONEX_05));
        f.add(new Fila("TCP rechazado o sin ruta", red,
                t -> t instanceof ConnectException || t instanceof NoRouteToHostException || t instanceof PortUnreachableException,
                ErrorCode.JBS_CONEX_03));
        f.add(new Fila("TLS: handshake, certificado, protocolo", red, SSLException.class::isInstance, ErrorCode.JBS_CONEX_06));
        f.add(new Fila("conexión cortada", red, t -> t instanceof SocketException || t instanceof EOFException,
                ErrorCode.JBS_CONEX_07));
        f.add(new Fila("respuesta no SOAP", red, SOAPException.class::isInstance, ErrorCode.JBS_CONEX_10));
        f.add(new Fila("otra falla de entrada/salida: resultado desconocido", red, IOException.class::isInstance,
                ErrorCode.JBS_CONEX_07));
        // ---------------------------------------------------------------- proxy de la configuración (§5.4)
        Set<Stage> proxy = EnumSet.of(Stage.PROXY_CONNECT);
        f.add(new Fila("proxy: el nombre no se resuelve", proxy, UnknownHostException.class::isInstance, ErrorCode.JBS_CONEX_02));
        f.add(new Fila("proxy: conexión rechazada o sin ruta", proxy,
                t -> t instanceof ConnectException || t instanceof NoRouteToHostException, ErrorCode.JBS_CONEX_02));
        f.add(new Fila("proxy: tiempo agotado al conectar", proxy,
                t -> t instanceof SocketTimeoutException && contiene(t, "connect"), ErrorCode.JBS_CONEX_02));
        // ---------------------------------------------------------------- XML que se lee
        Set<Stage> lectura = EnumSet.of(Stage.XML_READ);
        f.add(new Fila("valor no convertible al leer el XML", lectura,
                t -> t instanceof IllegalArgumentException || t instanceof DateTimeException, ErrorCode.JBS_XML_05));
        // ---------------------------------------------------------------- respuesta de SIFEN
        Set<Stage> respuesta = EnumSet.of(Stage.RESPONSE);
        f.add(new Fila("campo de la respuesta con formato inesperado", respuesta,
                t -> t instanceof IllegalArgumentException || t instanceof DateTimeException, ErrorCode.JBS_RESP_06));
        f.add(new Fila("serialización de la respuesta", respuesta, TransformerException.class::isInstance, ErrorCode.JBS_RESP_03));
        // ---------------------------------------------------------------- firma y armado
        f.add(new Fila("algoritmo de firma no disponible", TODAS,
                t -> t instanceof NoSuchAlgorithmException || t instanceof InvalidAlgorithmParameterException,
                ErrorCode.JBS_FIRMA_02));
        f.add(new Fila("falla de la firma XML", TODAS, t -> t instanceof XMLSignatureException || t instanceof MarshalException,
                ErrorCode.JBS_FIRMA_03));
        f.add(new Fila("serialización del XML", TODAS, TransformerException.class::isInstance, ErrorCode.JBS_INT_02));
        f.add(new Fila("armado del mensaje SOAP", TODAS, SOAPException.class::isInstance, ErrorCode.JBS_INT_01));
        return Collections.unmodifiableList(f);
    }

    /**
     * Busca el código de una excepción.
     *
     * @param t     excepción (se mira también su cadena de causas).
     * @param etapa etapa en la que ocurrió.
     * @return el código, o vacío si ninguna fila la reconoce (la fachada la informa como
     *         {@code JBS-INT-00}, o la etapa de respuesta como {@code JBS-RESP-03}).
     */
    public static Optional<ErrorCode> translate(Throwable t, Stage etapa) {
        int profundidad = 0;
        for (Throwable c = t; c != null && profundidad < 10; c = c.getCause() == c ? null : c.getCause(), profundidad++) {
            for (Fila fila : FILAS) {
                if (fila.etapas().contains(etapa) && fila.coincide().test(c)) {
                    return Optional.of(fila.codigo());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Devuelve las filas de la tabla como texto (para la documentación y los tests).
     *
     * @return una línea por fila: etapas, descripción y código.
     */
    public static List<String> describe() {
        List<String> l = new ArrayList<>();
        for (Fila fila : FILAS) {
            l.add(fila.etapas() + "\t" + fila.descripcion() + "\t" + fila.codigo().getCode());
        }
        return l;
    }

    private static boolean contiene(Throwable t, String... textos) {
        String m = t.getMessage() == null ? "" : t.getMessage().toLowerCase(Locale.ROOT);
        for (String s : textos) {
            if (m.contains(s)) {
                return true;
            }
        }
        return false;
    }
}

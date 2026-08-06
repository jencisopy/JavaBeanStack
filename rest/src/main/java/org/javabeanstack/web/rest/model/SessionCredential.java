/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
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
package org.javabeanstack.web.rest.model;

/**
 * Credencial con la que llega autenticada una petición REST: el valor (token de
 * aplicación o identificador de sesión de login) y el transporte por el que
 * vino.
 *
 * <p>El transporte no es un detalle decorativo: la regla anti CSRF solo se
 * aplica a las peticiones autenticadas <b>por cookie</b> —las que el navegador
 * envía solo—, mientras que las que traen el encabezado {@code Authorization}
 * quedan exentas porque ese encabezado ya es de envío deliberado. Por eso la
 * resolución de la credencial devuelve este objeto y no una simple cadena: con
 * una cadena se pierde el dato que decide la regla.</p>
 *
 * @author Jorge Enciso
 */
public class SessionCredential {

    /**
     * Transporte por el que llegó la credencial.
     */
    public enum Source {
        /** No se recibió credencial alguna. */
        NONE,
        /** Llegó en el encabezado {@code Authorization}. */
        HEADER,
        /** Llegó en la cookie de sesión. */
        COOKIE
    }

    /** Credencial ausente (la petición llegó sin credencial). */
    public static final SessionCredential EMPTY = new SessionCredential(null, Source.NONE);

    private final String value;
    private final Source source;

    /**
     * Construye la credencial.
     *
     * @param value valor de la credencial.
     * @param source transporte por el que llegó.
     */
    protected SessionCredential(String value, Source source) {
        this.value = value;
        this.source = source;
    }

    /**
     * Extrae la credencial del encabezado de autorización.
     *
     * <p>Se acepta el formato {@code "<esquema> <valor>"} (por ejemplo
     * {@code "Bearer xxxx"}). El esquema no se valida —igual que en
     * {@code AbstractWebResource.getTokenFromHeader}— para no rechazar clientes
     * ya existentes que envían otro esquema; un encabezado sin esquema (una sola
     * palabra) no es válido y se trata como ausente.</p>
     *
     * <p>El corte es por <b>espacio simple</b> y se toma el segundo segmento,
     * el mismo criterio que {@code getTokenFromHeader}: si los dos convivieran
     * con criterios distintos, un encabezado separado por tabulación sería
     * válido para uno e inválido para el otro dentro de la misma petición. Un
     * tercer segmento se descarta (igual criterio).</p>
     *
     * <p>A diferencia de {@code getTokenFromHeader}, acá un encabezado ausente
     * o mal formado <b>no lanza excepción</b>: devuelve {@link #EMPTY}, porque
     * quien llama todavía tiene que mirar la cookie antes de decidir que la
     * petición viene sin credencial.</p>
     *
     * @param authHeader contenido del encabezado {@code Authorization}.
     * @return credencial obtenida, o {@link #EMPTY} si el encabezado no aporta
     * ninguna.
     */
    public static SessionCredential fromAuthHeader(String authHeader) {
        if (authHeader == null) {
            return EMPTY;
        }
        String header = authHeader.trim();
        if (header.isEmpty()) {
            return EMPTY;
        }
        String[] parts = header.split(" ");
        if (parts.length < 2) {
            return EMPTY;
        }
        String credential = parts[1].trim();
        if (credential.isEmpty()) {
            return EMPTY;
        }
        return new SessionCredential(credential, Source.HEADER);
    }

    /**
     * Construye la credencial a partir del valor de la cookie de sesión.
     *
     * @param cookieValue valor de la cookie.
     * @return credencial obtenida, o {@link #EMPTY} si la cookie no trae valor.
     */
    public static SessionCredential fromCookie(String cookieValue) {
        if (cookieValue == null || cookieValue.trim().isEmpty()) {
            return EMPTY;
        }
        return new SessionCredential(cookieValue.trim(), Source.COOKIE);
    }

    /**
     * Devuelve el valor de la credencial.
     *
     * @return valor de la credencial, o {@code null} si no hay credencial.
     */
    public String getValue() {
        return value;
    }

    /**
     * Devuelve el transporte por el que llegó la credencial.
     *
     * @return transporte de la credencial.
     */
    public Source getSource() {
        return source;
    }

    /**
     * Indica si la petición trajo una credencial.
     *
     * @return verdadero si hay credencial.
     */
    public boolean isPresent() {
        return value != null;
    }

    /**
     * Indica si la credencial llegó en el encabezado de autorización.
     *
     * @return verdadero si llegó por el encabezado.
     */
    public boolean isFromHeader() {
        return source == Source.HEADER;
    }

    /**
     * Indica si la credencial llegó en la cookie de sesión.
     *
     * @return verdadero si llegó por la cookie.
     */
    public boolean isFromCookie() {
        return source == Source.COOKIE;
    }
}

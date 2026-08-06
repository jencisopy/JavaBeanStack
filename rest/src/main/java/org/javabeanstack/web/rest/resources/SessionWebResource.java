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
package org.javabeanstack.web.rest.resources;

import jakarta.ejb.EJB;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.security.ISessions;
import org.javabeanstack.security.model.IUserSession;

import org.javabeanstack.web.rest.model.SessionCredential;

/**
 * Soporte de <b>sesión de login</b> para los recursos REST.
 *
 * <p>El recurso base del framework solo autentica por token de aplicación. Esta
 * clase agrega el camino de la sesión de login —la que crea el ingreso con
 * usuario y contraseña— sin tocar el camino por token: los métodos
 * {@code setToken}, {@code getToken} y {@code getIdCompany} de la clase base
 * <b>no se sobrescriben</b>, de modo que quien extienda esta clase se comporta
 * frente a un token exactamente como antes.</p>
 *
 * <p><b>Por qué no hay camino automático</b>: una versión anterior sobrescribía
 * esos métodos para que cualquier recurso aceptara también sesiones de login. La
 * revisión del diseño lo descartó: la capa de datos filtra por empresa, no por
 * el alcance del usuario de login, así que habilitar la sesión de login en los
 * recursos generales dejaría a un usuario de alcance limitado leyendo toda la
 * empresa. Las sesiones de login entran por <b>una sola puerta explícita</b>,
 * {@link #requireLoginSession(String)}, que usan únicamente los recursos que la
 * aplicación decida; los recursos ya existentes quedan solo-token.</p>
 *
 * <p>Orden de resolución de la credencial: primero el encabezado
 * {@code Authorization}, después la cookie de sesión. El encabezado es el camino
 * de las herramientas y de los tests; la cookie —{@code HttpOnly}— es el camino
 * del navegador, que no puede leerla desde JavaScript.</p>
 *
 * <p>La aplicación que use esta clase define el nombre de su cookie y el valor
 * de su encabezado anti CSRF sobrescribiendo {@link #getSessionCookieName()} y
 * {@link #getCsrfHeaderValue()}; los valores de esta clase son solo los
 * defaults del framework.</p>
 *
 * <p><b>Por qué acá no se lanzan excepciones de autorización</b>: los recursos
 * que consumen esta clase construyen su propia respuesta de error (código y
 * cuerpo uniformes de la aplicación). Una excepción lanzada desde acá quedaría
 * capturada por el mapeador genérico de la aplicación y devolvería 500 en lugar
 * del 401/403 que corresponde. Por eso los métodos de resolución devuelven
 * {@code null} y es el recurso el que arma la respuesta; para distinguir el 403
 * del control anti CSRF del 401 de «sin sesión» está
 * {@link #isCsrfSafe(String)}.</p>
 *
 * @author Jorge Enciso
 */
public class SessionWebResource extends WebResource {

    private static final Logger LOGGER = LogManager.getLogger(SessionWebResource.class);

    /**
     * Nombre por omisión de la cookie en la que viaja el identificador de
     * sesión; la aplicación define el suyo en {@link #getSessionCookieName()}.
     */
    public static final String SESSION_COOKIE = "JbsSessionId";
    /** Encabezado que deben traer las peticiones mutadoras autenticadas por cookie. */
    public static final String CSRF_HEADER = "X-Requested-With";
    /**
     * Valor por omisión del encabezado anti CSRF; la aplicación define el suyo
     * en {@link #getCsrfHeaderValue()}.
     */
    public static final String CSRF_HEADER_VALUE = "JavaBeanStack";

    @EJB
    private ISessions sessions;

    /**
     * Petición en curso. Se declara acá porque la de la clase base no es
     * accesible desde las clases derivadas, y esta clase necesita las cookies,
     * el método HTTP y el context path.
     */
    @Context
    private HttpServletRequest httpRequest;

    /**
     * Devuelve el administrador de sesiones.
     *
     * @return administrador de sesiones.
     */
    protected ISessions getSessions() {
        return sessions;
    }

    /**
     * Devuelve la petición HTTP que se está atendiendo.
     *
     * @return petición en curso, o {@code null} fuera de un contexto de
     * petición.
     */
    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }

    /**
     * Devuelve el nombre de la cookie en la que viaja el identificador de
     * sesión. Las clases derivadas definen acá el nombre propio de su
     * aplicación.
     *
     * @return nombre de la cookie de sesión.
     */
    protected String getSessionCookieName() {
        return SESSION_COOKIE;
    }

    /**
     * Devuelve el valor que se exige en el encabezado anti CSRF. Las clases
     * derivadas definen acá el valor propio de su aplicación.
     *
     * @return valor esperado del encabezado anti CSRF.
     */
    protected String getCsrfHeaderValue() {
        return CSRF_HEADER_VALUE;
    }

    /**
     * Devuelve el nombre de la aplicación con el que se evalúa la política de
     * acceso: el context path del despliegue, tal como lo usa la creación de
     * sesión por token del framework.
     *
     * @return nombre de la aplicación, o {@code null} fuera de un contexto de
     * petición.
     */
    public String getAppName() {
        HttpServletRequest request = getHttpRequest();
        return (request == null) ? null : request.getContextPath();
    }

    /**
     * Resuelve la credencial de la petición: primero el encabezado
     * {@code Authorization}, después la cookie de sesión.
     *
     * <p>Un encabezado que no aporta credencial —ausente o mal formado— deja
     * paso a la cookie: intermediarios y clientes HTTP suelen inyectar
     * encabezados de autorización propios, y perder por eso el camino del
     * navegador sería un rechazo sin motivo. No debilita el control anti CSRF:
     * la credencial queda marcada como venida por cookie y el control se aplica
     * igual.</p>
     *
     * @param authHeader contenido del encabezado {@code Authorization}.
     * @return credencial de la petición, o {@link SessionCredential#EMPTY} si no
     * vino ninguna.
     */
    protected SessionCredential resolveCredential(String authHeader) {
        SessionCredential credential = SessionCredential.fromAuthHeader(authHeader);
        if (credential.isPresent()) {
            return credential;
        }
        return SessionCredential.fromCookie(getSessionCookieValue());
    }

    /**
     * Devuelve el valor de la cookie de sesión de la petición en curso.
     *
     * @return valor de la cookie, o {@code null} si no está presente.
     */
    protected String getSessionCookieValue() {
        HttpServletRequest request = getHttpRequest();
        if (request == null) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        String cookieName = getSessionCookieName();
        for (Cookie cookie : cookies) {
            if (cookie != null && cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * Exige una sesión de <b>login</b> para la petición: es la única puerta por
     * la que una sesión de login entra a la capa REST.
     *
     * <p>Los controles se aplican en este orden:</p>
     * <ol>
     *   <li><b>Forma canónica</b>: la credencial presentada tiene que ser igual
     *       al identificador de la sesión, que para las sesiones de login es el
     *       valor <b>encriptado</b>. El pool de sesiones del framework también
     *       responde a su clave interna —derivable de datos de la base—, así que
     *       sin este control esa forma cruda serviría como credencial desde
     *       Internet.</li>
     *   <li><b>Rechazo de sesiones de token</b>: este camino exige un usuario
     *       real; una sesión creada a partir de un token de integración no
     *       representa a una persona.</li>
     *   <li><b>Control anti CSRF</b>: si la credencial vino por cookie y el
     *       método modifica datos, se exige el encabezado propio. Va acá adentro
     *       —y no como control opcional del recurso— para que no exista forma de
     *       atender una mutación por cookie sin haberlo verificado.</li>
     * </ol>
     *
     * @param authHeader contenido del encabezado {@code Authorization}.
     * @return la sesión de login, o {@code null} si algún control falla. El
     * recurso responde 403 cuando {@link #isCsrfSafe(String)} es falso y 401 en
     * los demás casos.
     */
    protected IUserSession requireLoginSession(String authHeader) {
        SessionCredential credential = resolveCredential(authHeader);
        IUserSession session = getCanonicalSession(credential);
        if (session == null) {
            return null;
        }
        if (!isLoginSession(session)) {
            LOGGER.info("Credencial de token rechazada en un recurso que exige sesión de login");
            return null;
        }
        if (!isCsrfSafe(credential)) {
            LOGGER.info("Petición mutadora por cookie sin el encabezado " + CSRF_HEADER);
            return null;
        }
        return session;
    }

    /**
     * Devuelve la empresa de la sesión de login de la petición.
     *
     * <p>Se devuelve la <b>empresa enmascarada</b> cuando la empresa la tiene
     * definida: es la que aplica el filtro de datos por {@code idempresa} y la
     * misma que guarda el camino por token en su caché.</p>
     *
     * @param authHeader contenido del encabezado {@code Authorization}.
     * @return identificador de la empresa, o {@code null} si no hay sesión de
     * login válida.
     */
    protected Long getIdCompanyFromSession(String authHeader) {
        SessionCredential credential = resolveCredential(authHeader);
        IUserSession session = getCanonicalSession(credential);
        if (session == null || !isLoginSession(session)) {
            return null;
        }
        IAppCompany company = session.getCompany();
        if (company != null && company.getIdcompanymask() != null) {
            return company.getIdcompanymask();
        }
        return session.getIdCompany();
    }

    /**
     * Devuelve la sesión viva de una credencial, siempre que la credencial sea
     * la forma canónica de su identificador.
     *
     * @param credential credencial de la petición.
     * @return la sesión, o {@code null} si no hay credencial, si la sesión no
     * existe, expiró o fue revocada, o si la credencial no es la forma canónica.
     */
    protected IUserSession getCanonicalSession(SessionCredential credential) {
        if (credential == null || !credential.isPresent() || getSessions() == null) {
            return null;
        }
        IUserSession session = getSessions().getUserSession(credential.getValue());
        //Una sesión expirada vuelve con el usuario en nulo y el error cargado
        if (session == null || session.getUser() == null || session.getError() != null) {
            return null;
        }
        if (!credential.getValue().equals(session.getSessionId())) {
            LOGGER.info("Credencial rechazada: no es la forma canónica del identificador de sesión");
            return null;
        }
        return session;
    }

    /**
     * Determina si una sesión es de login. La distinción es la información de
     * autenticación del cliente: las sesiones creadas a partir de un token la
     * tienen cargada, las de login no.
     *
     * @param session sesión a evaluar.
     * @return verdadero si es una sesión de login.
     */
    public boolean isLoginSession(IUserSession session) {
        return session != null
                && session.getUser() != null
                && session.getClientAuthRequestInfo() == null;
    }

    /**
     * Determina si el usuario de la sesión es administrador del sistema
     * (roles 00 a 20).
     *
     * @param session sesión a evaluar.
     * @return verdadero si el usuario de la sesión es administrador.
     */
    public boolean isSysAdmin(IUserSession session) {
        return session != null
                && session.getUser() != null
                && session.getUser().isSysAdmin();
    }

    /**
     * Determina si la petición en curso cumple la regla anti CSRF.
     *
     * <p>El control ya está aplicado dentro de {@link #requireLoginSession},
     * que es lo que garantiza que no pueda saltearse. Este método existe para
     * que el recurso distinga el motivo del rechazo y responda 403 en lugar de
     * 401.</p>
     *
     * @param authHeader contenido del encabezado {@code Authorization}.
     * @return verdadero si la petición puede seguir; falso si corresponde
     * rechazarla (403).
     */
    public boolean isCsrfSafe(String authHeader) {
        return isCsrfSafe(resolveCredential(authHeader));
    }

    /**
     * Determina si la petición en curso cumple la regla anti CSRF, con la
     * credencial ya resuelta.
     *
     * <p>La regla solo alcanza a las peticiones <b>mutadoras autenticadas por
     * cookie</b>: son las que el navegador puede enviar por su cuenta desde otro
     * sitio. Exigirles un encabezado propio garantiza que las envió el cliente
     * de la aplicación y no un formulario ajeno. Las autenticadas por
     * {@code Authorization} están exentas: ese encabezado ya es de envío
     * deliberado.</p>
     *
     * @param credential credencial de la petición.
     * @return verdadero si la petición puede seguir.
     */
    protected boolean isCsrfSafe(SessionCredential credential) {
        HttpServletRequest request = getHttpRequest();
        String method = (request == null) ? null : request.getMethod();
        if (!isCsrfProtectionRequired(method, credential)) {
            return true;
        }
        String header = (request == null) ? null : request.getHeader(CSRF_HEADER);
        return isCsrfHeaderValid(header, getCsrfHeaderValue());
    }

    /**
     * Determina si a una petición le corresponde el control anti CSRF.
     *
     * @param httpMethod método HTTP de la petición; un método desconocido se
     * trata como mutador.
     * @param credential credencial con la que llegó autenticada.
     * @return verdadero si hay que exigir el encabezado anti CSRF.
     */
    public static boolean isCsrfProtectionRequired(String httpMethod, SessionCredential credential) {
        if (credential == null || !credential.isFromCookie()) {
            return false;
        }
        return isMutation(httpMethod);
    }

    /**
     * Determina si un método HTTP modifica datos.
     *
     * @param httpMethod método HTTP; nulo o desconocido se considera mutador,
     * que es el criterio conservador.
     * @return verdadero si el método modifica datos.
     */
    public static boolean isMutation(String httpMethod) {
        if (httpMethod == null) {
            return true;
        }
        String method = httpMethod.trim().toUpperCase();
        return !("GET".equals(method)
                || "HEAD".equals(method)
                || "OPTIONS".equals(method)
                || "TRACE".equals(method));
    }

    /**
     * Valida el encabezado anti CSRF recibido. La comparación es <b>exacta</b>:
     * el valor es una constante que emite el cliente de la aplicación, no un
     * dato del usuario.
     *
     * @param headerValue valor recibido.
     * @param expectedValue valor esperado.
     * @return verdadero si coincide exactamente.
     */
    public static boolean isCsrfHeaderValid(String headerValue, String expectedValue) {
        if (headerValue == null || expectedValue == null) {
            return false;
        }
        return expectedValue.equals(headerValue);
    }

    /**
     * Devuelve un dato guardado en el contexto de una sesión.
     *
     * <p>Se usa el identificador de la sesión y no la credencial recibida: el
     * contexto vive en un mapa indexado por la cadena exacta con la que se
     * escribió, sin desencriptar, de modo que leer y escribir con formas
     * distintas del mismo identificador devolvería nulo sin ningún error.</p>
     *
     * @param session sesión de la que se lee.
     * @param key clave del dato.
     * @return valor guardado, o {@code null} si no hay sesión o no existe el
     * dato.
     */
    public Object getSessionInfo(IUserSession session, String key) {
        if (session == null || getSessions() == null) {
            return null;
        }
        return getSessions().getSessionInfo(session.getSessionId(), key);
    }

    /**
     * Guarda un dato en el contexto de una sesión.
     *
     * @param session sesión en la que se guarda.
     * @param key clave del dato.
     * @param info valor a guardar.
     */
    public void addSessionInfo(IUserSession session, String key, Object info) {
        if (session == null || getSessions() == null) {
            return;
        }
        getSessions().addSessionInfo(session.getSessionId(), key, info);
    }
}

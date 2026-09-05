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
package org.javabeanstack.web.rest.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.security.ISecManager;
import org.javabeanstack.security.ISessions;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.web.rest.model.SessionCredential;

/**
 * Resuelve la sesión de una petición a partir de su credencial, sea un token de
 * acceso o un identificador de sesión de login.
 *
 * <p>Orden de resolución:</p>
 * <ol>
 * <li>La credencial se toma del encabezado {@code Authorization} ({@code
 * <esquema> <valor>}) y, si no viene, de la cookie {@code JbsSessionId}.</li>
 * <li>Se pregunta a {@link ISessions#getUserSession(String)}: si hay una sesión
 * viva, con usuario y sin error, es un login o un token ya cacheado y se
 * devuelve tal cual. Una sesión de <b>token</b> que llegó por <b>cookie</b> se
 * rechaza: la cookie es del navegador y solo transporta sesiones de login.</li>
 * <li>Si no hay sesión y la credencial vino por encabezado se la trata como
 * token: {@link ISecManager#createSessionFromToken(String, String)} con el
 * context path de la aplicación (que es donde se evalúa la política de
 * acceso). Si falla, el gancho {@link MainServerVerifier} puede traer el token
 * del servidor principal y se reintenta.</li>
 * <li>Sin sesión, se devuelve un resultado vacío; quien llama decide la
 * respuesta.</li>
 * </ol>
 *
 * <p>No lanza excepciones ni escribe en el log el valor de la credencial: un
 * token es una credencial vigente y quien lea el log podría usarla.</p>
 */
public class SessionResolver {

    private static final Logger LOGGER = LogManager.getLogger(SessionResolver.class);

    /** Nombre por omisión de la cookie de sesión de login (el mismo de {@code SessionWebResource}). */
    public static final String SESSION_COOKIE = "JbsSessionId";

    /**
     * Gancho para verificar un token en un servidor principal y grabarlo en la
     * base local. La implementación por omisión no hace nada.
     */
    @FunctionalInterface
    public interface MainServerVerifier {

        /**
         * @param token token que no pudo resolverse localmente.
         * @return verdadero si el token se trajo y grabó, y vale reintentar.
         */
        boolean verifyTokenInMainServer(String token);
    }

    /** Resultado de la resolución: la sesión (o nulo) y la credencial que la produjo. */
    public static final class Resolution {

        private final IUserSession session;
        private final SessionCredential credential;

        Resolution(IUserSession session, SessionCredential credential) {
            this.session = session;
            this.credential = credential;
        }

        /** @return la sesión resuelta, o nulo si no hay. */
        public IUserSession getSession() {
            return session;
        }

        /** @return la credencial leída de la petición (puede estar vacía). */
        public SessionCredential getCredential() {
            return credential;
        }

        /** @return verdadero si hay sesión. */
        public boolean isPresent() {
            return session != null;
        }
    }

    private final ISessions sessions;
    private final ISecManager secManager;
    private final MainServerVerifier mainServer;
    private final String cookieName;

    /**
     * @param sessions pool de sesiones.
     * @param secManager gestor de seguridad (crea la sesión desde el token).
     * @param mainServer gancho del servidor principal; nulo equivale a «no hay».
     */
    public SessionResolver(ISessions sessions, ISecManager secManager, MainServerVerifier mainServer) {
        this(sessions, secManager, mainServer, SESSION_COOKIE);
    }

    /**
     * @param sessions pool de sesiones.
     * @param secManager gestor de seguridad (crea la sesión desde el token).
     * @param mainServer gancho del servidor principal; nulo equivale a «no hay».
     * @param cookieName nombre de la cookie de sesión de login de la
     * aplicación (cada aplicación define la suya; nulo = {@link #SESSION_COOKIE}).
     */
    public SessionResolver(ISessions sessions, ISecManager secManager, MainServerVerifier mainServer,
            String cookieName) {
        this.sessions = sessions;
        this.secManager = secManager;
        this.mainServer = (mainServer == null) ? token -> false : mainServer;
        this.cookieName = (cookieName == null || cookieName.trim().isEmpty()) ? SESSION_COOKIE : cookieName;
    }

    /** @return nombre de la cookie de sesión que lee este resolutor. */
    public String getCookieName() {
        return cookieName;
    }

    /**
     * Lee la credencial de la petición: encabezado primero, cookie después.
     *
     * @param request petición.
     * @return credencial (vacía si no hay ninguna).
     */
    public SessionCredential readCredential(HttpServletRequest request) {
        if (request == null) {
            return SessionCredential.EMPTY;
        }
        SessionCredential credential = SessionCredential.fromAuthHeader(request.getHeader("Authorization"));
        if (credential.isPresent()) {
            return credential;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie != null && cookieName.equals(cookie.getName())) {
                    return SessionCredential.fromCookie(cookie.getValue());
                }
            }
        }
        return SessionCredential.EMPTY;
    }

    /**
     * Resuelve la sesión de la petición.
     *
     * @param request petición.
     * @return resultado con la sesión (o sin ella) y la credencial.
     */
    public Resolution resolve(HttpServletRequest request) {
        SessionCredential credential = readCredential(request);
        if (!credential.isPresent()) {
            return new Resolution(null, credential);
        }
        String value = credential.getValue();
        IUserSession session = findLiveSession(value);
        if (session != null) {
            if (credential.isFromCookie() && session.getClientAuthRequestInfo() != null) {
                LOGGER.info("Credencial rechazada: una cookie no puede transportar un token");
                return new Resolution(null, credential);
            }
            return new Resolution(session, credential);
        }
        if (credential.isFromCookie()) {
            //Una cookie sin sesión viva es una sesión de login expirada o
            //inventada: no se intenta tratarla como token.
            return new Resolution(null, credential);
        }
        session = createFromToken(value, request.getContextPath());
        return new Resolution(session, credential);
    }

    /**
     * Devuelve la sesión viva de la caché, o nulo si no está, expiró o no tiene usuario.
     */
    protected IUserSession findLiveSession(String value) {
        if (sessions == null) {
            return null;
        }
        IUserSession session = sessions.getUserSession(value);
        if (session == null || session.getUser() == null || session.getError() != null) {
            return null;
        }
        return session;
    }

    /**
     * Crea la sesión a partir de un token, con reintento vía servidor principal.
     */
    protected IUserSession createFromToken(String token, String appName) {
        if (secManager == null) {
            return null;
        }
        IUserSession session = secManager.createSessionFromToken(token, appName);
        if (session == null && mainServer.verifyTokenInMainServer(token)) {
            session = secManager.createSessionFromToken(token, appName);
        }
        if (session == null) {
            LOGGER.info("Token rechazado: expirado, bloqueado, inexistente o sin acceso a " + appName);
        }
        return session;
    }
}

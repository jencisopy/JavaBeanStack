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

import jakarta.annotation.Priority;
import jakarta.ejb.EJB;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.security.ISecManager;
import org.javabeanstack.security.ISessions;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.web.rest.model.ErrorMessage;
import org.javabeanstack.web.rest.model.SessionCredential;

/**
 * Filtro JAX-RS ligado a {@link SessionRequired}: resuelve la sesión de la
 * petición —token o login— antes de que corra el método del recurso.
 *
 * <ul>
 * <li>Sin sesión válida: 401 con {@link ErrorMessage}, el método no se ejecuta.</li>
 * <li>Credencial por cookie en un método mutador sin el encabezado
 * {@code X-Requested-With: JavaBeanStack}: 403.</li>
 * <li>Con sesión: la deja en el {@code HttpServletRequest} como atributos
 * {@link #SESSION_ATTRIBUTE} y {@link #CREDENTIAL_ATTRIBUTE}, de donde la lee
 * {@code AbstractWebResource.getSession()}.</li>
 * </ul>
 *
 * <p>Es una clase concreta y usable tal cual. Una aplicación con servidor
 * principal de credenciales, o con su propio nombre de cookie de sesión, la
 * extiende y sobreescribe {@link #verifyTokenInMainServer(String)} /
 * {@link #getSessionCookieName()}; la subclase se registra en la
 * {@code Application} en lugar de esta.</p>
 */
@Provider
@SessionRequired
@Priority(Priorities.AUTHENTICATION)
public class SessionRequiredFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = LogManager.getLogger(SessionRequiredFilter.class);

    /** Atributo del request con la {@link IUserSession} resuelta. */
    public static final String SESSION_ATTRIBUTE = "org.javabeanstack.web.rest.session";
    /** Atributo del request con la {@link SessionCredential} que la produjo. */
    public static final String CREDENTIAL_ATTRIBUTE = "org.javabeanstack.web.rest.credential";

    @EJB
    private ISessions sessions;

    @EJB
    private ISecManager secManager;

    @Context
    private HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        HttpServletRequest req = getRequest();
        SessionResolver.Resolution resolution = getResolver().resolve(req);
        if (!resolution.isPresent()) {
            abort(requestContext, Response.Status.UNAUTHORIZED, "Este token ya expiró o es incorrecto");
            return;
        }
        String method = requestContext.getMethod();
        String csrfHeader = (req == null) ? null : req.getHeader(CsrfRules.CSRF_HEADER);
        if (!CsrfRules.isCsrfSafe(method, resolution.getCredential(), csrfHeader, getCsrfHeaderValue())) {
            LOGGER.info("Petición mutadora por cookie sin el encabezado " + CsrfRules.CSRF_HEADER);
            abort(requestContext, Response.Status.FORBIDDEN, "Falta el encabezado " + CsrfRules.CSRF_HEADER);
            return;
        }
        publish(req, resolution.getSession(), resolution.getCredential());
    }

    /**
     * Corta la petición con un error. Separado para que la decisión (qué
     * estado y por qué) se pueda probar sin una implementación JAX-RS.
     *
     * @param requestContext contexto de la petición.
     * @param status estado HTTP.
     * @param message mensaje para el cliente.
     */
    protected void abort(ContainerRequestContext requestContext, Response.Status status, String message) {
        requestContext.abortWith(error(status, message));
    }

    /**
     * Deja la sesión y la credencial a disposición del recurso.
     */
    protected void publish(HttpServletRequest req, IUserSession session, SessionCredential credential) {
        if (req != null) {
            req.setAttribute(SESSION_ATTRIBUTE, session);
            req.setAttribute(CREDENTIAL_ATTRIBUTE, credential);
        }
    }

    /**
     * Arma el resolutor con los EJB inyectados y el gancho del servidor principal.
     */
    protected SessionResolver getResolver() {
        return new SessionResolver(getSessions(), getSecManager(), this::verifyTokenInMainServer,
                getSessionCookieName());
    }

    /**
     * Nombre de la cookie de sesión de login de la aplicación; por omisión el
     * de JavaBeanStack. La aplicación que use otro (el portal de Maker, por
     * ejemplo) lo sobreescribe.
     *
     * @return nombre de la cookie.
     */
    protected String getSessionCookieName() {
        return SessionResolver.SESSION_COOKIE;
    }

    /**
     * Gancho del servidor principal de credenciales; por omisión no hay.
     *
     * @param token token que no pudo resolverse localmente.
     * @return verdadero si se trajo y grabó el token y vale reintentar.
     */
    protected boolean verifyTokenInMainServer(String token) {
        return false;
    }

    /** @return valor esperado del encabezado CSRF. */
    protected String getCsrfHeaderValue() {
        return CsrfRules.CSRF_HEADER_VALUE;
    }

    protected ISessions getSessions() {
        return sessions;
    }

    protected ISecManager getSecManager() {
        return secManager;
    }

    protected HttpServletRequest getRequest() {
        return request;
    }

    /** Respuesta 401 con la misma forma que {@code TokenErrorExceptionMapper}. */
    public static Response unauthorized(String message) {
        return error(Response.Status.UNAUTHORIZED, message);
    }

    /** Respuesta 403 con la misma forma. */
    public static Response forbidden(String message) {
        return error(Response.Status.FORBIDDEN, message);
    }

    private static Response error(Response.Status status, String message) {
        ErrorMessage error = new ErrorMessage();
        error.setErrorMessage(message);
        error.setErrorCode(status.getStatusCode());
        error.setDocumentation("");
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(error).build();
    }
}

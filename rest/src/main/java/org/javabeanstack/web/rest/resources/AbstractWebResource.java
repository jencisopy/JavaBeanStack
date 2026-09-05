/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2018 - 2027 Jorge Enciso
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.javabeanstack.security.ISecManager;
import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.model.IAppUser;
import org.javabeanstack.security.IOAuthConsumer;
import org.javabeanstack.security.ISessions;
import org.javabeanstack.security.model.IClientAuthRequestInfo;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.util.Strings;
import org.javabeanstack.web.rest.exceptions.AccessDeniedError;
import org.javabeanstack.web.rest.exceptions.TokenError;
import org.javabeanstack.web.rest.model.SessionCredential;
import org.javabeanstack.web.rest.security.CsrfRules;
import org.javabeanstack.web.rest.security.SessionRequiredFilter;
import org.javabeanstack.web.rest.security.SessionResolver;
import org.javabeanstack.ws.resources.IWebResource;

/**
 *
 * Base de los recursos JAX-RS: implementa
 * {@link org.javabeanstack.ws.resources.IWebResource} y provee el acceso a la
 * sesión —de token o de login— y los datos del cliente.
 *
 * <p>La sesión la resuelve el filtro {@link SessionRequiredFilter} antes de que
 * corra el método (recurso anotado con {@code @SessionRequired}) y queda en el
 * {@code HttpServletRequest}; el recurso la lee con {@link #getSession()} y
 * pasa {@link #getSessionId()} al servicio de datos. Un recurso que no lleva la
 * anotación —porque autentica de otra forma, o solo en algunos métodos— puede
 * resolverla a mano con {@link #requireSession()}.</p>
 *
 * <p>La API anterior por encabezado ({@code setToken}, {@code getToken},
 * {@code getIdCompany(String)}…) se conserva marcada como obsoleta para las
 * aplicaciones que todavía la usan; no conviene usarla en código nuevo.</p>
 *
 * @author Jorge Enciso
 */
public abstract class AbstractWebResource implements IWebResource {

    private static final Logger LOGGER = LogManager.getLogger(AbstractWebResource.class);

    @EJB
    private IOAuthConsumer oAuthConsumer;

    @EJB
    private ISessions sessions;

    @Context
    HttpServletRequest requestContext;

    @Override
    public abstract <T extends IDataService> T getDataService();

    @Override
    public abstract ISecManager getSecManager();

    // ------------------------------------------------------------------
    // Sesión resuelta por el filtro (API vigente)
    // ------------------------------------------------------------------

    /**
     * Devuelve la sesión de la petición, resuelta por el filtro
     * {@code SessionRequiredFilter}.
     *
     * @return la sesión.
     * @throws TokenError si no hay sesión en la petición: el recurso no lleva
     * {@code @SessionRequired} y tampoco llamó a {@link #requireSession()}.
     */
    public IUserSession getSession() {
        IUserSession session = getPublishedSession();
        if (session == null) {
            throw new TokenError("Recurso sin sesión: falta @SessionRequired");
        }
        return session;
    }

    /**
     * Resuelve la sesión de la petición a mano, para los recursos que no llevan
     * la anotación. Si el filtro ya la resolvió, devuelve esa. Aplica la misma
     * regla CSRF que el filtro.
     *
     * @return la sesión.
     * @throws TokenError si no hay credencial válida (401).
     * @throws AccessDeniedError si la credencial vino por cookie en un método
     * mutador sin el encabezado CSRF (403).
     */
    protected IUserSession requireSession() {
        IUserSession session = getPublishedSession();
        if (session != null) {
            return session;
        }
        SessionResolver.Resolution resolution = getSessionResolver().resolve(requestContext);
        if (!resolution.isPresent()) {
            throw new TokenError("Este token ya expiró o es incorrecto");
        }
        String method = (requestContext == null) ? null : requestContext.getMethod();
        String csrf = (requestContext == null) ? null : requestContext.getHeader(CsrfRules.CSRF_HEADER);
        if (!CsrfRules.isCsrfSafe(method, resolution.getCredential(), csrf, getCsrfHeaderValue())) {
            throw new AccessDeniedError(AccessDeniedError.CSRF_REQUIRED,
                    "Falta el encabezado " + CsrfRules.CSRF_HEADER);
        }
        if (requestContext != null) {
            requestContext.setAttribute(SessionRequiredFilter.SESSION_ATTRIBUTE, resolution.getSession());
            requestContext.setAttribute(SessionRequiredFilter.CREDENTIAL_ATTRIBUTE, resolution.getCredential());
        }
        return resolution.getSession();
    }

    /**
     * Devuelve el identificador de sesión que espera el servicio de datos: el
     * token en claro si la sesión nació de un token, o el identificador cifrado
     * si nació de un login.
     *
     * @return identificador de sesión.
     */
    public String getSessionId() {
        return getSession().getSessionId();
    }

    /**
     * Devuelve el identificador de la empresa de la sesión: el
     * {@code idcompanymask} —el {@code idempresa} con el que la empresa aparece
     * en el esquema de datos— y, si no lo hay, el {@code idcompany}.
     *
     * @return identificador de la empresa.
     */
    @Override
    public Long getIdCompany() {
        IUserSession session = getSession();
        IAppCompany company = session.getCompany();
        if (company != null && company.getIdcompanymask() != null) {
            return company.getIdcompanymask();
        }
        return session.getIdCompany();
    }

    /**
     * Indica si la sesión nació de un token de acceso (y no de un login).
     *
     * @return verdadero si es una sesión de token.
     */
    public boolean isTokenSession() {
        return getSession().getClientAuthRequestInfo() != null;
    }

    /**
     * Indica si el usuario de la sesión es administrador del sistema (roles 00
     * a 20). Vale igual para token y para login: se deriva del usuario, no del
     * contenido del token.
     *
     * @return verdadero si es administrador.
     */
    public boolean isSysAdmin() {
        IAppUser user = getSession().getUser();
        return user != null && user.isSysAdmin();
    }

    /**
     * Devuelve los datos de autenticación del token, cuando la sesión nació de
     * uno.
     *
     * @return los datos del token, o nulo si la sesión es de login.
     */
    public IClientAuthRequestInfo getClientAuthRequestInfo() {
        return getSession().getClientAuthRequestInfo();
    }

    /**
     * Devuelve la credencial con la que llegó la petición (encabezado o
     * cookie), si el filtro la dejó.
     *
     * @return la credencial, o nulo.
     */
    protected SessionCredential getSessionCredential() {
        if (requestContext == null) {
            return null;
        }
        Object credential = requestContext.getAttribute(SessionRequiredFilter.CREDENTIAL_ATTRIBUTE);
        return (credential instanceof SessionCredential) ? (SessionCredential) credential : null;
    }

    /**
     * Sesión dejada en el request por el filtro o por {@link #requireSession()}.
     */
    protected IUserSession getPublishedSession() {
        if (requestContext == null) {
            return null;
        }
        Object session = requestContext.getAttribute(SessionRequiredFilter.SESSION_ATTRIBUTE);
        return (session instanceof IUserSession) ? (IUserSession) session : null;
    }

    /**
     * Arma el resolutor de sesión con el gancho del servidor principal de este
     * recurso.
     */
    protected SessionResolver getSessionResolver() {
        return new SessionResolver(getSessions(), getSecManager(), this::verifyTokenInMainServer,
                getSessionCookieName());
    }

    /**
     * Valor que {@link #requireSession()} exige en el encabezado anti CSRF de
     * una petición mutadora por cookie; por omisión el de JavaBeanStack. Tiene
     * que coincidir con el del filtro de la aplicación.
     *
     * @return valor esperado del encabezado.
     */
    protected String getCsrfHeaderValue() {
        return CsrfRules.CSRF_HEADER_VALUE;
    }

    /**
     * Nombre de la cookie de sesión de login que acepta {@link #requireSession()};
     * por omisión el de JavaBeanStack. Tiene que coincidir con el del filtro de
     * la aplicación.
     *
     * @return nombre de la cookie.
     */
    protected String getSessionCookieName() {
        return SessionResolver.SESSION_COOKIE;
    }

    /**
     * Devuelve el pool de sesiones.
     *
     * @return pool de sesiones.
     */
    protected ISessions getSessions() {
        return sessions;
    }

    /**
     * Devuelve la petición http en curso.
     *
     * @return la petición.
     */
    protected HttpServletRequest getHttpRequest() {
        return requestContext;
    }

    /**
     * Devuelve la ip del cliente que realiza la solicitud.
     * @return ip del cliente.
     */
    @Override
    public final String getIpClient() {
        return requestContext.getRemoteAddr();
    }

    /**
     * Devuelve el host remoto que realiza la solicitud.
     * @return host remoto.
     */
    @Override
    public final String getRemoteHost() {
        return requestContext.getRemoteHost();
    }

    /**
     * Devuelve el gestor de consumidores OAuth.
     *
     * @return gestor OAuth.
     */
    protected IOAuthConsumer getOAuthConsumer() {
        return oAuthConsumer;
    }

    /**
     * Verifica la validez de un token.
     *
     * @param token token a verificar.
     * @return error si el token no es válido, o {@code null} si es válido.
     */
    public IErrorReg verifyToken(String token) {
        return getOAuthConsumer().checkToken(token);
    }

    /**
     * Verifica el token contra el servidor principal de autenticación y lo
     * graba localmente. Por omisión no hay servidor principal.
     *
     * @param token token a verificar.
     * @return verdadero si el servidor principal lo validó y se grabó.
     */
    protected boolean verifyTokenInMainServer(String token) {
        //Implementar en clases hijas
        return false;
    }

    // ------------------------------------------------------------------
    // API anterior, por encabezado. Obsoleta: la sesión la resuelve el filtro.
    // ------------------------------------------------------------------

    /**
     * Devuelve el identificador de la empresa a partir del encabezado de
     * autorización.
     * @param authHeader encabezado de autorización.
     * @return identificador de la empresa.
     * @deprecated usar {@link #getIdCompany()} sobre la sesión resuelta por el
     * filtro.
     */
    @Deprecated
    @Override
    public Long getIdCompany(String authHeader) {
        String token = getTokenFromHeader(authHeader);
        IClientAuthRequestInfo info = getSecManager().getClientAuthRequestCache(token);
        if (info != null) {
            return info.getIdcompany();
        }
        return null;
    }

    /**
     * Extrae el token del encabezado de autorización.
     *
     * @param authHeader encabezado de autorización.
     * @return token.
     * @deprecated usar {@link #getSessionId()}.
     */
    @Deprecated
    public String getToken(String authHeader) {
        String token = getTokenFromHeader(authHeader);
        IClientAuthRequestInfo info = getSecManager().getClientAuthRequestCache(token);
        if (info != null) {
            return info.getToken();
        }
        return null;
    }

    /**
     * Determina si el usuario dueño del token es administrador del sistema
     * (roles 00 a 20). El dato se deriva del usuario mapeado al token (columna
     * usercode), no del contenido del campo data: data lo informa el cliente al
     * pedir el token y no es una fuente de autorización confiable.
     *
     * @param token token de acceso.
     * @return verdadero si el usuario del token es administrador del sistema.
     * @deprecated usar {@link #isSysAdmin()}.
     */
    @Deprecated
    protected boolean isTokenAdministrator(String token) {
        IAppUser user = getOAuthConsumer().getUserMapped(token);
        return user != null && user.isSysAdmin();
    }

    /**
     * Asigna el token a partir del encabezado indicado.
     *
     * <p>La sesión se crea informando el nombre de esta aplicación, de modo que
     * la propia creación verifique que el rol del usuario tenga acceso
     * concedido: si no lo tiene, la sesión no se crea.</p>
     *
     * @param tokenHeader encabezado con el token.
     * @deprecated anotar el recurso con {@code @SessionRequired} o llamar a
     * {@link #requireSession()}.
     */
    @Deprecated
    protected void setToken(String tokenHeader) {
        String token = getTokenFromHeader(tokenHeader);
        //Si el token es null
        if (Strings.isNullorEmpty(token)) {
            throw new TokenError("Debe proporcionar el token de autorización");
        }
        //Si ya esta activo este token en la sesiones
        if (getSecManager().getClientAuthRequestCache(token) != null) {
            return;
        }
        String appName = (requestContext == null) ? null : requestContext.getContextPath();
        //Crear la sesión
        IUserSession userSession = getSecManager().createSessionFromToken(token, appName);
        //Si no se puedo crear la sesion, probablemente el token no existe o esta bloqueado o ya expiro.
        if (userSession == null) {
            // Verificar y traer credenciales del servidor y grabar en el local
            if (!verifyTokenInMainServer(token)) {
                LOGGER.error("Este token ya expiró o es incorrecto Server: " + token);
                throw new TokenError("Este token ya expiró o es incorrecto");
            }
            userSession = getSecManager().createSessionFromToken(token, appName);
        }
        //Reverificar en el local
        if (userSession == null) {
            LOGGER.error("Este token ya expiró o es incorrecto: local " + token);
            throw new TokenError("Este token ya expiró o es incorrecto");
        }
    }

    /**
     * Extrae el valor del token de un encabezado.
     *
     * @param tokenHeader encabezado con el token.
     * @return valor del token.
     * @deprecated la credencial la lee {@code SessionResolver}.
     */
    @Deprecated
    protected String getTokenFromHeader(String tokenHeader) {
        if (Strings.isNullorEmpty(tokenHeader)) {
            throw new TokenError("Debe proporcionar el token de autorización");
        }
        //Formato esperado "esquema token" (ej. "Bearer xxxx"); sin esquema no es válido
        String[] tokens = tokenHeader.split("\\ ");
        if (tokens.length < 2) {
            throw new TokenError("Debe proporcionar el token de autorización");
        }
        return tokens[1];
    }
}

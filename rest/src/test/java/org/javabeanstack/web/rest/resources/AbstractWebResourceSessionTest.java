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

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.model.IAppUser;
import org.javabeanstack.security.ISecManager;
import org.javabeanstack.security.ISessions;
import org.javabeanstack.security.model.ClientAuthRequestInfo;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.security.model.UserSession;
import org.javabeanstack.web.rest.exceptions.AccessDeniedError;
import org.javabeanstack.web.rest.exceptions.TokenError;
import org.javabeanstack.web.rest.security.CsrfRules;
import org.javabeanstack.web.rest.security.SessionRequiredFilter;
import org.javabeanstack.web.rest.security.SessionResolver;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas puras de la API de sesión de {@link AbstractWebResource}: lo que el
 * recurso lee después de que el filtro resolvió la sesión, y
 * {@code requireSession()} para los recursos sin anotación.
 */
public class AbstractWebResourceSessionTest {

    private static final String ID_LOGIN = "ENC-9";
    private static final String TOKEN = "tok-9";

    static class RecursoStub extends AbstractWebResource {

        private final ISessions sessions;
        private final ISecManager secManager;

        RecursoStub(HttpServletRequest request, ISessions sessions, ISecManager secManager) {
            this.requestContext = request;
            this.sessions = sessions;
            this.secManager = secManager;
        }

        @Override
        public <T extends IDataService> T getDataService() {
            return null;
        }

        @Override
        public ISecManager getSecManager() {
            return secManager;
        }

        @Override
        public ISecManager getSecManager(String jndi) {
            return secManager;
        }

        @Override
        protected ISessions getSessions() {
            return sessions;
        }
    }

    static Object porOmision(Method method) {
        Class<?> tipo = method.getReturnType();
        if (tipo == boolean.class) {
            return false;
        }
        if (tipo == int.class) {
            return 0;
        }
        if (tipo == long.class) {
            return 0L;
        }
        return null;
    }

    static HttpServletRequest request(String metodo, Map<String, String> headers, Cookie[] cookies) {
        Map<String, Object> atributos = new HashMap<>();
        return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getMethod":
                            return metodo;
                        case "getCookies":
                            return cookies;
                        case "getHeader":
                            return headers.get((String) args[0]);
                        case "getContextPath":
                            return "/app";
                        case "setAttribute":
                            atributos.put((String) args[0], args[1]);
                            return null;
                        case "getAttribute":
                            return atributos.get((String) args[0]);
                        default:
                            return porOmision(method);
                    }
                });
    }

    static ISessions sessions(Map<String, IUserSession> pool) {
        return (ISessions) Proxy.newProxyInstance(ISessions.class.getClassLoader(),
                new Class<?>[]{ISessions.class}, (proxy, method, args)
                -> "getUserSession".equals(method.getName()) ? pool.get((String) args[0]) : porOmision(method));
    }

    static ISecManager secManagerNulo() {
        return (ISecManager) Proxy.newProxyInstance(ISecManager.class.getClassLoader(),
                new Class<?>[]{ISecManager.class}, (proxy, method, args) -> porOmision(method));
    }

    static IAppUser usuario(boolean sysAdmin) {
        return (IAppUser) Proxy.newProxyInstance(IAppUser.class.getClassLoader(),
                new Class<?>[]{IAppUser.class}, (proxy, method, args)
                -> "isSysAdmin".equals(method.getName()) ? sysAdmin : porOmision(method));
    }

    static IAppCompany empresa(Long idcompany, Long mask) {
        return (IAppCompany) Proxy.newProxyInstance(IAppCompany.class.getClassLoader(),
                new Class<?>[]{IAppCompany.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getIdcompany":
                            return idcompany;
                        case "getIdcompanymask":
                            return mask;
                        default:
                            return porOmision(method);
                    }
                });
    }

    static UserSession sesion(String id, boolean token, boolean admin, IAppCompany company) {
        UserSession s = new UserSession();
        s.setSessionId(id);
        s.setUser(usuario(admin));
        s.setIdCompany(8L);
        s.setCompany(company);
        if (token) {
            s.setClientAuthRequestInfo(new ClientAuthRequestInfo());
        }
        return s;
    }

    @Test
    @DisplayName("getSession sin filtro: TokenError (el recurso olvidó @SessionRequired)")
    public void sinSesionPublicada() {
        RecursoStub r = new RecursoStub(request("GET", new HashMap<>(), null), sessions(new HashMap<>()), secManagerNulo());
        assertThrows(TokenError.class, r::getSession);
        assertThrows(TokenError.class, r::getSessionId);
        assertThrows(TokenError.class, r::getIdCompany);
    }

    @Test
    @DisplayName("Con la sesión publicada por el filtro: sessionId, empresa (idcompanymask), token, admin")
    public void sesionPublicada() {
        HttpServletRequest req = request("GET", new HashMap<>(), null);
        req.setAttribute(SessionRequiredFilter.SESSION_ATTRIBUTE, sesion(TOKEN, true, true, empresa(8L, 6L)));
        RecursoStub r = new RecursoStub(req, sessions(new HashMap<>()), secManagerNulo());
        assertEquals(TOKEN, r.getSessionId());
        assertEquals(6L, r.getIdCompany(), "idcompanymask manda");
        assertTrue(r.isTokenSession());
        assertTrue(r.isSysAdmin());
        assertNotNull(r.getClientAuthRequestInfo());
    }

    @Test
    @DisplayName("getIdCompany cae al idcompany cuando no hay máscara ni empresa")
    public void idCompanySinMascara() {
        HttpServletRequest req = request("GET", new HashMap<>(), null);
        req.setAttribute(SessionRequiredFilter.SESSION_ATTRIBUTE, sesion(ID_LOGIN, false, false, empresa(8L, null)));
        RecursoStub r = new RecursoStub(req, sessions(new HashMap<>()), secManagerNulo());
        assertEquals(8L, r.getIdCompany());
        assertFalse(r.isTokenSession());
        assertFalse(r.isSysAdmin());
        assertNull(r.getClientAuthRequestInfo());

        HttpServletRequest req2 = request("GET", new HashMap<>(), null);
        req2.setAttribute(SessionRequiredFilter.SESSION_ATTRIBUTE, sesion(ID_LOGIN, false, false, null));
        assertEquals(8L, new RecursoStub(req2, sessions(new HashMap<>()), secManagerNulo()).getIdCompany());
    }

    @Test
    @DisplayName("requireSession resuelve a mano, publica y después getSession la encuentra")
    public void requireSessionResuelve() {
        Map<String, IUserSession> pool = new HashMap<>();
        pool.put(ID_LOGIN, sesion(ID_LOGIN, false, false, empresa(8L, 6L)));
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + ID_LOGIN);
        HttpServletRequest req = request("GET", headers, null);
        RecursoStub r = new RecursoStub(req, sessions(pool), secManagerNulo());
        IUserSession s = r.requireSession();
        assertEquals(ID_LOGIN, s.getSessionId());
        assertSame(s, r.getSession());
        assertNotNull(req.getAttribute(SessionRequiredFilter.CREDENTIAL_ATTRIBUTE));
    }

    @Test
    @DisplayName("requireSession sin credencial válida: TokenError")
    public void requireSessionSinCredencial() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer nada");
        RecursoStub r = new RecursoStub(request("GET", headers, null), sessions(new HashMap<>()), secManagerNulo());
        assertThrows(TokenError.class, r::requireSession);
    }

    @Test
    @DisplayName("requireSession por cookie en un POST sin CSRF: AccessDeniedError CSRF_REQUIRED")
    public void requireSessionCsrf() {
        Map<String, IUserSession> pool = new HashMap<>();
        pool.put(ID_LOGIN, sesion(ID_LOGIN, false, false, empresa(8L, 6L)));
        Cookie[] cookies = {new Cookie(SessionResolver.SESSION_COOKIE, ID_LOGIN)};
        RecursoStub r = new RecursoStub(request("POST", new HashMap<>(), cookies), sessions(pool), secManagerNulo());
        AccessDeniedError e = assertThrows(AccessDeniedError.class, r::requireSession);
        assertEquals(AccessDeniedError.CSRF_REQUIRED, e.getCode());

        Map<String, String> headers = new HashMap<>();
        headers.put(CsrfRules.CSRF_HEADER, CsrfRules.CSRF_HEADER_VALUE);
        RecursoStub ok = new RecursoStub(request("POST", headers, cookies), sessions(pool), secManagerNulo());
        assertEquals(ID_LOGIN, ok.requireSession().getSessionId());
    }
}

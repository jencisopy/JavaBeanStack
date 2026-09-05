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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.NameBinding;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.javabeanstack.security.ISecManager;
import org.javabeanstack.security.ISessions;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.web.rest.model.SessionCredential;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas puras del filtro: 401 sin credencial, 403 CSRF por cookie, paso
 * limpio por encabezado, publicación de la sesión en el request y el gancho
 * del servidor principal en una subclase.
 */
public class SessionRequiredFilterTest {

    /** Filtro con los EJB inyectados a mano. */
    static class FiltroStub extends SessionRequiredFilter {

        private final ISessions sessions;
        private final ISecManager secManager;
        private final HttpServletRequest request;
        boolean gancho;
        final List<String> verificados = new ArrayList<>();
        Integer estado;
        String mensaje;

        FiltroStub(ISessions sessions, ISecManager secManager, HttpServletRequest request) {
            this.sessions = sessions;
            this.secManager = secManager;
            this.request = request;
        }

        @Override
        protected ISessions getSessions() {
            return sessions;
        }

        @Override
        protected ISecManager getSecManager() {
            return secManager;
        }

        @Override
        protected HttpServletRequest getRequest() {
            return request;
        }

        @Override
        protected boolean verifyTokenInMainServer(String token) {
            verificados.add(token);
            return gancho;
        }

        @Override
        protected void abort(ContainerRequestContext requestContext, Response.Status status, String message) {
            //Sin RuntimeDelegate en el classpath de test no se puede armar un
            //Response: se registra la decisión, que es lo que se prueba.
            estado = status.getStatusCode();
            mensaje = message;
        }
    }

    /** Contexto JAX-RS que solo sabe su método. */
    static ContainerRequestContext contexto(String metodo) {
        return (ContainerRequestContext) Proxy.newProxyInstance(
                ContainerRequestContext.class.getClassLoader(),
                new Class<?>[]{ContainerRequestContext.class}, (proxy, method, args)
                -> "getMethod".equals(method.getName()) ? metodo : TestDobles.porOmision(method));
    }

    static HttpServletRequest request(String metodo, String authHeader, Cookie[] cookies, String csrf) {
        Map<String, String> headers = new HashMap<>();
        if (authHeader != null) {
            headers.put("Authorization", authHeader);
        }
        if (csrf != null) {
            headers.put(CsrfRules.CSRF_HEADER, csrf);
        }
        return TestDobles.request(metodo, cookies, headers, SessionResolverTest.CONTEXT);
    }

    static Map<String, IUserSession> poolConLogin() {
        Map<String, IUserSession> pool = new HashMap<>();
        pool.put(SessionResolverTest.ID_LOGIN, SessionResolverTest.sesionDeLogin());
        return pool;
    }

    @Test
    @DisplayName("La anotación es un NameBinding y el filtro corre en AUTHENTICATION")
    public void contratoJaxRs() {
        assertNotNull(SessionRequired.class.getAnnotation(NameBinding.class));
        assertNotNull(SessionRequiredFilter.class.getAnnotation(Provider.class));
        assertNotNull(SessionRequiredFilter.class.getAnnotation(SessionRequired.class));
        Priority p = SessionRequiredFilter.class.getAnnotation(Priority.class);
        assertNotNull(p);
        assertEquals(Priorities.AUTHENTICATION, p.value());
    }

    @Test
    @DisplayName("Sin credencial: 401 con ErrorMessage y nada publicado")
    public void sinCredencial401() {
        HttpServletRequest req = request("GET", null, null, null);
        FiltroStub f = new FiltroStub(SessionResolverTest.sessions(new HashMap<>()),
                SessionResolverTest.secManager(new HashMap<>(), new ArrayList<>()), req);
        f.filter(contexto("GET"));
        assertEquals(401, f.estado);
        assertNotNull(f.mensaje);
        assertNull(req.getAttribute(SessionRequiredFilter.SESSION_ATTRIBUTE));
    }

    @Test
    @DisplayName("Token válido por encabezado: pasa y publica sesión y credencial")
    public void tokenPublica() {
        Map<String, IUserSession> pool = new HashMap<>();
        pool.put(SessionResolverTest.TOKEN, SessionResolverTest.sesionDeToken());
        HttpServletRequest req = request("POST", "Bearer " + SessionResolverTest.TOKEN, null, null);
        FiltroStub f = new FiltroStub(SessionResolverTest.sessions(pool),
                SessionResolverTest.secManager(new HashMap<>(), new ArrayList<>()), req);
        f.filter(contexto("POST"));
        assertNull(f.estado, "un POST por encabezado no exige CSRF");
        IUserSession publicada = (IUserSession) req.getAttribute(SessionRequiredFilter.SESSION_ATTRIBUTE);
        assertEquals(SessionResolverTest.TOKEN, publicada.getSessionId());
        SessionCredential cred = (SessionCredential) req.getAttribute(SessionRequiredFilter.CREDENTIAL_ATTRIBUTE);
        assertTrue(cred.isFromHeader());
    }

    @Test
    @DisplayName("POST por cookie sin X-Requested-With: 403")
    public void cookieMutadoraSinCsrf403() {
        Cookie[] cookies = {new Cookie(SessionResolver.SESSION_COOKIE, SessionResolverTest.ID_LOGIN)};
        HttpServletRequest req = request("POST", null, cookies, null);
        FiltroStub f = new FiltroStub(SessionResolverTest.sessions(poolConLogin()),
                SessionResolverTest.secManager(new HashMap<>(), new ArrayList<>()), req);
        f.filter(contexto("POST"));
        assertEquals(403, f.estado);
        assertTrue(f.mensaje.contains(CsrfRules.CSRF_HEADER));
        assertNull(req.getAttribute(SessionRequiredFilter.SESSION_ATTRIBUTE));
    }

    @Test
    @DisplayName("POST por cookie con X-Requested-With: pasa")
    public void cookieMutadoraConCsrf() {
        Cookie[] cookies = {new Cookie(SessionResolver.SESSION_COOKIE, SessionResolverTest.ID_LOGIN)};
        HttpServletRequest req = request("POST", null, cookies, CsrfRules.CSRF_HEADER_VALUE);
        FiltroStub f = new FiltroStub(SessionResolverTest.sessions(poolConLogin()),
                SessionResolverTest.secManager(new HashMap<>(), new ArrayList<>()), req);
        f.filter(contexto("POST"));
        assertNull(f.estado);
        assertNotNull(req.getAttribute(SessionRequiredFilter.SESSION_ATTRIBUTE));
    }

    @Test
    @DisplayName("GET por cookie sin X-Requested-With: pasa (no es mutador)")
    public void cookieLecturaSinCsrf() {
        Cookie[] cookies = {new Cookie(SessionResolver.SESSION_COOKIE, SessionResolverTest.ID_LOGIN)};
        HttpServletRequest req = request("GET", null, cookies, null);
        FiltroStub f = new FiltroStub(SessionResolverTest.sessions(poolConLogin()),
                SessionResolverTest.secManager(new HashMap<>(), new ArrayList<>()), req);
        f.filter(contexto("GET"));
        assertNull(f.estado);
    }

    @Test
    @DisplayName("Cookie con token: 401 (D4)")
    public void cookieConToken401() {
        Map<String, IUserSession> pool = new HashMap<>();
        pool.put(SessionResolverTest.TOKEN, SessionResolverTest.sesionDeToken());
        Cookie[] cookies = {new Cookie(SessionResolver.SESSION_COOKIE, SessionResolverTest.TOKEN)};
        HttpServletRequest req = request("GET", null, cookies, null);
        FiltroStub f = new FiltroStub(SessionResolverTest.sessions(pool),
                SessionResolverTest.secManager(new HashMap<>(), new ArrayList<>()), req);
        f.filter(contexto("GET"));
        assertEquals(401, f.estado);
    }

    @Test
    @DisplayName("Subclase con servidor principal: el gancho se invoca y el reintento resuelve")
    public void ganchoServidorPrincipal() {
        Map<String, IUserSession> porToken = new HashMap<>();
        List<String> llamadas = new ArrayList<>();
        HttpServletRequest req = request("GET", "Bearer " + SessionResolverTest.TOKEN, null, null);
        ISecManager sec = (ISecManager) Proxy.newProxyInstance(ISecManager.class.getClassLoader(),
                new Class<?>[]{ISecManager.class}, (proxy, method, args) -> {
                    if ("createSessionFromToken".equals(method.getName())) {
                        llamadas.add((String) args[0]);
                        //Recién existe después de que el gancho lo «grabó»
                        return porToken.get((String) args[0]);
                    }
                    return TestDobles.porOmision(method);
                });
        FiltroStub f = new FiltroStub(SessionResolverTest.sessions(new HashMap<>()), sec, req) {
            @Override
            protected boolean verifyTokenInMainServer(String token) {
                verificados.add(token);
                porToken.put(token, SessionResolverTest.sesionDeToken());
                return true;
            }
        };
        f.filter(contexto("GET"));
        assertNull(f.estado);
        assertEquals(List.of(SessionResolverTest.TOKEN), f.verificados);
        assertEquals(2, llamadas.size());
    }

}

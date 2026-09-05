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
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.javabeanstack.security.ISecManager;
import org.javabeanstack.security.ISessions;
import org.javabeanstack.security.model.ClientAuthRequestInfo;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.security.model.UserSession;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas puras del resolutor de sesión: token por encabezado (cacheado y no
 * cacheado), sesión de login por encabezado y por cookie, cookie con token
 * rechazada, sin credencial, y el gancho del servidor principal.
 */
public class SessionResolverTest {

    static final String TOKEN = "tok-abc";
    static final String ID_LOGIN = "ENC-123";
    static final String CONTEXT = "/app-rest";

    /** Doble de ISessions sobre un mapa; las demás llamadas devuelven nulo. */
    static ISessions sessions(Map<String, IUserSession> pool) {
        return (ISessions) Proxy.newProxyInstance(ISessions.class.getClassLoader(),
                new Class<?>[]{ISessions.class}, (proxy, method, args) -> {
                    if ("getUserSession".equals(method.getName())) {
                        return pool.get((String) args[0]);
                    }
                    return TestDobles.porOmision(method);
                });
    }

    /** Doble de ISecManager: createSessionFromToken devuelve lo del mapa y registra las llamadas. */
    static ISecManager secManager(Map<String, IUserSession> porToken, List<String> llamadas) {
        return (ISecManager) Proxy.newProxyInstance(ISecManager.class.getClassLoader(),
                new Class<?>[]{ISecManager.class}, (proxy, method, args) -> {
                    if ("createSessionFromToken".equals(method.getName())) {
                        llamadas.add(args[0] + "@" + (args.length > 1 ? args[1] : ""));
                        return porToken.get((String) args[0]);
                    }
                    return TestDobles.porOmision(method);
                });
    }

    static HttpServletRequest request(String authHeader, Cookie[] cookies, String metodo) {
        Map<String, String> headers = new HashMap<>();
        if (authHeader != null) {
            headers.put("Authorization", authHeader);
        }
        return TestDobles.request(metodo, cookies, headers, CONTEXT);
    }

    static UserSession sesionDeLogin() {
        UserSession s = new UserSession();
        s.setUser(TestDobles.usuario(false, "40"));
        s.setSessionId(ID_LOGIN);
        return s;
    }

    static UserSession sesionDeToken() {
        UserSession s = new UserSession();
        s.setUser(TestDobles.usuario(false, "30"));
        s.setSessionId(TOKEN);
        s.setClientAuthRequestInfo(new ClientAuthRequestInfo());
        return s;
    }

    @Test
    @DisplayName("Token por encabezado ya cacheado: devuelve la sesión sin crearla")
    public void tokenCacheado() {
        Map<String, IUserSession> pool = new HashMap<>();
        pool.put(TOKEN, sesionDeToken());
        List<String> llamadas = new ArrayList<>();
        SessionResolver r = new SessionResolver(sessions(pool), secManager(new HashMap<>(), llamadas), null);
        SessionResolver.Resolution res = r.resolve(request("Bearer " + TOKEN, null, "GET"));
        assertTrue(res.isPresent());
        assertEquals(TOKEN, res.getSession().getSessionId());
        assertTrue(res.getCredential().isFromHeader());
        assertTrue(llamadas.isEmpty(), "no debe llamar a createSessionFromToken");
    }

    @Test
    @DisplayName("Token por encabezado no cacheado: createSessionFromToken(token, contextPath)")
    public void tokenNoCacheado() {
        Map<String, IUserSession> porToken = new HashMap<>();
        porToken.put(TOKEN, sesionDeToken());
        List<String> llamadas = new ArrayList<>();
        SessionResolver r = new SessionResolver(sessions(new HashMap<>()), secManager(porToken, llamadas), null);
        SessionResolver.Resolution res = r.resolve(request("Bearer " + TOKEN, null, "GET"));
        assertTrue(res.isPresent());
        assertEquals(List.of(TOKEN + "@" + CONTEXT), llamadas);
    }

    @Test
    @DisplayName("Token inválido y sin servidor principal: sin sesión, un solo intento")
    public void tokenInvalido() {
        List<String> llamadas = new ArrayList<>();
        SessionResolver r = new SessionResolver(sessions(new HashMap<>()), secManager(new HashMap<>(), llamadas), null);
        SessionResolver.Resolution res = r.resolve(request("Bearer malo", null, "GET"));
        assertFalse(res.isPresent());
        assertEquals(1, llamadas.size());
    }

    @Test
    @DisplayName("Token inválido con servidor principal que lo trae: reintento y sesión")
    public void tokenViaServidorPrincipal() {
        Map<String, IUserSession> porToken = new HashMap<>();
        List<String> llamadas = new ArrayList<>();
        List<String> verificados = new ArrayList<>();
        SessionResolver.MainServerVerifier gancho = token -> {
            verificados.add(token);
            porToken.put(token, sesionDeToken()); //«lo grabó»: el reintento lo encuentra
            return true;
        };
        SessionResolver r = new SessionResolver(sessions(new HashMap<>()), secManager(porToken, llamadas), gancho);
        SessionResolver.Resolution res = r.resolve(request("Bearer " + TOKEN, null, "GET"));
        assertTrue(res.isPresent());
        assertEquals(List.of(TOKEN), verificados);
        assertEquals(2, llamadas.size(), "un intento antes y otro después del servidor principal");
    }

    @Test
    @DisplayName("Sesión de login por encabezado Bearer: se acepta")
    public void loginPorEncabezado() {
        Map<String, IUserSession> pool = new HashMap<>();
        pool.put(ID_LOGIN, sesionDeLogin());
        SessionResolver r = new SessionResolver(sessions(pool), secManager(new HashMap<>(), new ArrayList<>()), null);
        SessionResolver.Resolution res = r.resolve(request("Bearer " + ID_LOGIN, null, "GET"));
        assertTrue(res.isPresent());
        assertNull(res.getSession().getClientAuthRequestInfo());
    }

    @Test
    @DisplayName("Sesión de login por cookie JbsSessionId: se acepta y la credencial es de cookie")
    public void loginPorCookie() {
        Map<String, IUserSession> pool = new HashMap<>();
        pool.put(ID_LOGIN, sesionDeLogin());
        SessionResolver r = new SessionResolver(sessions(pool), secManager(new HashMap<>(), new ArrayList<>()), null);
        Cookie[] cookies = {new Cookie("otra", "x"), new Cookie(SessionResolver.SESSION_COOKIE, ID_LOGIN)};
        SessionResolver.Resolution res = r.resolve(request(null, cookies, "GET"));
        assertTrue(res.isPresent());
        assertTrue(res.getCredential().isFromCookie());
    }

    @Test
    @DisplayName("El encabezado manda sobre la cookie cuando vienen los dos")
    public void encabezadoAntesQueCookie() {
        Map<String, IUserSession> pool = new HashMap<>();
        pool.put(ID_LOGIN, sesionDeLogin());
        pool.put(TOKEN, sesionDeToken());
        SessionResolver r = new SessionResolver(sessions(pool), secManager(new HashMap<>(), new ArrayList<>()), null);
        Cookie[] cookies = {new Cookie(SessionResolver.SESSION_COOKIE, ID_LOGIN)};
        SessionResolver.Resolution res = r.resolve(request("Bearer " + TOKEN, cookies, "GET"));
        assertEquals(TOKEN, res.getSession().getSessionId());
        assertTrue(res.getCredential().isFromHeader());
    }

    @Test
    @DisplayName("Cookie que transporta un token: se rechaza (D4)")
    public void cookieConTokenRechazada() {
        Map<String, IUserSession> pool = new HashMap<>();
        pool.put(TOKEN, sesionDeToken());
        List<String> llamadas = new ArrayList<>();
        SessionResolver r = new SessionResolver(sessions(pool), secManager(new HashMap<>(), llamadas), null);
        Cookie[] cookies = {new Cookie(SessionResolver.SESSION_COOKIE, TOKEN)};
        SessionResolver.Resolution res = r.resolve(request(null, cookies, "GET"));
        assertFalse(res.isPresent());
        assertTrue(llamadas.isEmpty(), "una cookie nunca se trata como token");
    }

    @Test
    @DisplayName("Cookie sin sesión viva: no se intenta como token")
    public void cookieExpirada() {
        List<String> llamadas = new ArrayList<>();
        SessionResolver r = new SessionResolver(sessions(new HashMap<>()), secManager(new HashMap<>(), llamadas), null);
        Cookie[] cookies = {new Cookie(SessionResolver.SESSION_COOKIE, "vieja")};
        assertFalse(r.resolve(request(null, cookies, "GET")).isPresent());
        assertTrue(llamadas.isEmpty());
    }

    @Test
    @DisplayName("Sin credencial, encabezado sin esquema o request nulo: sin sesión")
    public void sinCredencial() {
        SessionResolver r = new SessionResolver(sessions(new HashMap<>()), secManager(new HashMap<>(), new ArrayList<>()), null);
        assertFalse(r.resolve(request(null, null, "GET")).isPresent());
        assertFalse(r.resolve(request(TOKEN, null, "GET")).isPresent());
        assertFalse(r.resolve(request("Bearer ", null, "GET")).isPresent());
        assertFalse(r.resolve(null).isPresent());
    }

    @Test
    @DisplayName("Sesión en caché con error o sin usuario: no cuenta como viva")
    public void sesionConError() {
        UserSession expirada = sesionDeLogin();
        expirada.setUser(null);
        Map<String, IUserSession> pool = new HashMap<>();
        pool.put(ID_LOGIN, expirada);
        SessionResolver r = new SessionResolver(sessions(pool), secManager(new HashMap<>(), new ArrayList<>()), null);
        Cookie[] cookies = {new Cookie(SessionResolver.SESSION_COOKIE, ID_LOGIN)};
        assertFalse(r.resolve(request(null, cookies, "GET")).isPresent());
    }

    @Test
    @DisplayName("La aplicación puede usar su propio nombre de cookie")
    public void cookieConOtroNombre() {
        Map<String, IUserSession> pool = new HashMap<>();
        pool.put(ID_LOGIN, sesionDeLogin());
        SessionResolver r = new SessionResolver(sessions(pool), secManager(new HashMap<>(), new ArrayList<>()),
                null, "MakerPortalSession");
        assertEquals("MakerPortalSession", r.getCookieName());
        Cookie[] propia = {new Cookie("MakerPortalSession", ID_LOGIN)};
        assertTrue(r.resolve(request(null, propia, "GET")).isPresent());
        Cookie[] ajena = {new Cookie(SessionResolver.SESSION_COOKIE, ID_LOGIN)};
        assertFalse(r.resolve(request(null, ajena, "GET")).isPresent(), "la cookie por omisión ya no cuenta");
        assertEquals(SessionResolver.SESSION_COOKIE, new SessionResolver(null, null, null, " ").getCookieName());
    }
}

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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import org.javabeanstack.error.ErrorReg;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.model.IAppUser;
import org.javabeanstack.security.ISessions;
import org.javabeanstack.security.model.ClientAuthRequestInfo;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.security.model.UserSession;

import org.javabeanstack.web.rest.model.SessionCredential;

/**
 * Pruebas del guard de sesión de login: la única puerta por la que una sesión
 * de login entra a la capa REST.
 *
 * <p>Se ejercita sin contenedor sustituyendo el administrador de sesiones y la
 * petición HTTP por dobles: lo que se verifica es la lógica del guard, no la
 * inyección. Los tres controles que aplica —forma canónica del identificador,
 * rechazo de sesiones de token y anti CSRF— son los que impiden,
 * respectivamente, que sirva la clave interna del pool (derivable de datos de la
 * base), que un cliente de integración se haga pasar por un usuario de login y
 * que un sitio ajeno dispare mutaciones con la cookie de la víctima.</p>
 *
 * @author Jorge Enciso
 */
public class SessionWebResourceTest {

    private static final String ID_CANONICO = "ENCRIPTADO-1";
    private static final String CLAVE_CRUDA = "125:8F14E45FCEEA167A5A36DEDD4BEA2543";
    private static final String CONTEXT_PATH = "/app-rest";

    /**
     * Recurso de prueba: toma el administrador de sesiones y la petición de los
     * dobles en lugar de la inyección del contenedor.
     */
    private static class RecursoStub extends SessionWebResource {

        private final ISessions sessions;
        private final HttpServletRequest request;

        RecursoStub(ISessions sessions, HttpServletRequest request) {
            this.sessions = sessions;
            this.request = request;
        }

        @Override
        protected ISessions getSessions() {
            return sessions;
        }

        @Override
        public HttpServletRequest getHttpRequest() {
            return request;
        }
    }

    /**
     * Valor por omisión de un método no simulado, respetando los tipos
     * primitivos (devolver nulo en un método booleano rompería con NPE).
     */
    private static Object valorPorOmision(Method method) {
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

    private static ISessions sessionsCon(Map<String, IUserSession> pool) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("getUserSession".equals(method.getName())) {
                return pool.get((String) args[0]);
            }
            return valorPorOmision(method);
        };
        return (ISessions) Proxy.newProxyInstance(ISessions.class.getClassLoader(),
                new Class<?>[]{ISessions.class}, handler);
    }

    private static HttpServletRequest request(String metodo, Cookie[] cookies,
            Map<String, String> headers) {
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "getMethod":
                    return metodo;
                case "getCookies":
                    return cookies;
                case "getHeader":
                    return headers.get((String) args[0]);
                case "getContextPath":
                    return CONTEXT_PATH;
                default:
                    return valorPorOmision(method);
            }
        };
        return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class}, handler);
    }

    private static IAppUser usuario(boolean sysAdmin) {
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "isSysAdmin":
                    return sysAdmin;
                case "getLogin":
                case "getCode":
                    return "USER40";
                case "getRol":
                    return "40";
                default:
                    return valorPorOmision(method);
            }
        };
        return (IAppUser) Proxy.newProxyInstance(IAppUser.class.getClassLoader(),
                new Class<?>[]{IAppUser.class}, handler);
    }

    private static IAppCompany empresa(Long idcompany, Long idcompanymask) {
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "getIdcompany":
                    return idcompany;
                case "getIdcompanymask":
                    return idcompanymask;
                default:
                    return valorPorOmision(method);
            }
        };
        return (IAppCompany) Proxy.newProxyInstance(IAppCompany.class.getClassLoader(),
                new Class<?>[]{IAppCompany.class}, handler);
    }

    /**
     * Sesión de login viva, registrada en el pool con las dos formas que el
     * framework acepta: la canónica (encriptada) y la clave interna cruda.
     */
    private static UserSession sesionDeLogin(Map<String, IUserSession> pool) {
        UserSession session = new UserSession();
        session.setUser(usuario(false));
        session.setSessionId(ID_CANONICO);
        pool.put(ID_CANONICO, session);
        pool.put(CLAVE_CRUDA, session);
        return session;
    }

    private static Cookie[] cookieDeSesion(String valor) {
        return new Cookie[]{new Cookie("otra", "x"),
            new Cookie(SessionWebResource.SESSION_COOKIE, valor)};
    }

    @Test
    public void testCredencialCanonicaPorEncabezado() {
        Map<String, IUserSession> pool = new HashMap<>();
        IUserSession session = sesionDeLogin(pool);
        RecursoStub recurso = new RecursoStub(sessionsCon(pool),
                request("GET", null, new HashMap<>()));

        assertSame(session, recurso.requireLoginSession("Bearer " + ID_CANONICO));
    }

    @Test
    public void testClaveCrudaDelPoolRechazada() {
        Map<String, IUserSession> pool = new HashMap<>();
        sesionDeLogin(pool);
        RecursoStub recurso = new RecursoStub(sessionsCon(pool),
                request("GET", null, new HashMap<>()));

        //El pool resuelve la sesión con su clave interna, pero esa forma no es
        //el identificador canónico: la puerta REST la rechaza.
        assertNull(recurso.requireLoginSession("Bearer " + CLAVE_CRUDA));
        assertNull(recurso.getIdCompanyFromSession("Bearer " + CLAVE_CRUDA));
    }

    @Test
    public void testSesionDeTokenRechazada() {
        Map<String, IUserSession> pool = new HashMap<>();
        UserSession session = sesionDeLogin(pool);
        session.setClientAuthRequestInfo(new ClientAuthRequestInfo());
        RecursoStub recurso = new RecursoStub(sessionsCon(pool),
                request("GET", null, new HashMap<>()));

        assertNull(recurso.requireLoginSession("Bearer " + ID_CANONICO));
        assertFalse(recurso.isLoginSession(session));
    }

    @Test
    public void testSesionExpiradaRechazada() {
        Map<String, IUserSession> pool = new HashMap<>();
        UserSession session = sesionDeLogin(pool);
        session.setError(new ErrorReg("La sesión expiro", 6, ""));
        RecursoStub recurso = new RecursoStub(sessionsCon(pool),
                request("GET", null, new HashMap<>()));

        assertNull(recurso.requireLoginSession("Bearer " + ID_CANONICO));
    }

    @Test
    public void testSinCredencial() {
        Map<String, IUserSession> pool = new HashMap<>();
        sesionDeLogin(pool);
        RecursoStub recurso = new RecursoStub(sessionsCon(pool),
                request("GET", null, new HashMap<>()));

        assertNull(recurso.requireLoginSession(null));
        assertNull(recurso.requireLoginSession(""));
        assertNull(recurso.requireLoginSession("Bearer otro-valor"));
    }

    @Test
    public void testLecturaPorCookie() {
        Map<String, IUserSession> pool = new HashMap<>();
        IUserSession session = sesionDeLogin(pool);
        RecursoStub recurso = new RecursoStub(sessionsCon(pool),
                request("GET", cookieDeSesion(ID_CANONICO), new HashMap<>()));

        assertSame(session, recurso.requireLoginSession(null));
    }

    @Test
    public void testMutacionPorCookieSinEncabezadoAntiCsrf() {
        Map<String, IUserSession> pool = new HashMap<>();
        sesionDeLogin(pool);
        RecursoStub recurso = new RecursoStub(sessionsCon(pool),
                request("POST", cookieDeSesion(ID_CANONICO), new HashMap<>()));

        assertNull(recurso.requireLoginSession(null));
        //El recurso distingue el 403 del 401 con este método
        assertFalse(recurso.isCsrfSafe((String) null));
    }

    @Test
    public void testMutacionPorCookieConEncabezadoAntiCsrf() {
        Map<String, IUserSession> pool = new HashMap<>();
        IUserSession session = sesionDeLogin(pool);
        Map<String, String> headers = new HashMap<>();
        headers.put(SessionWebResource.CSRF_HEADER, SessionWebResource.CSRF_HEADER_VALUE);
        RecursoStub recurso = new RecursoStub(sessionsCon(pool),
                request("POST", cookieDeSesion(ID_CANONICO), headers));

        assertTrue(recurso.isCsrfSafe((String) null));
        assertSame(session, recurso.requireLoginSession(null));
    }

    @Test
    public void testEncabezadoAntiCsrfConValorDistintoRechazado() {
        Map<String, IUserSession> pool = new HashMap<>();
        sesionDeLogin(pool);
        Map<String, String> headers = new HashMap<>();
        //La comparación es exacta: el valor lo emite el cliente de la aplicación
        headers.put(SessionWebResource.CSRF_HEADER,
                SessionWebResource.CSRF_HEADER_VALUE.toLowerCase());
        RecursoStub recurso = new RecursoStub(sessionsCon(pool),
                request("POST", cookieDeSesion(ID_CANONICO), headers));

        assertNull(recurso.requireLoginSession(null));
    }

    @Test
    public void testMutacionPorEncabezadoExentaDeAntiCsrf() {
        Map<String, IUserSession> pool = new HashMap<>();
        IUserSession session = sesionDeLogin(pool);
        RecursoStub recurso = new RecursoStub(sessionsCon(pool),
                request("POST", null, new HashMap<>()));

        assertSame(session, recurso.requireLoginSession("Bearer " + ID_CANONICO));
    }

    @Test
    public void testEncabezadoConCredencialNoCaeALaCookie() {
        Map<String, IUserSession> pool = new HashMap<>();
        sesionDeLogin(pool);
        RecursoStub recurso = new RecursoStub(sessionsCon(pool),
                request("GET", cookieDeSesion(ID_CANONICO), new HashMap<>()));

        //El encabezado aporta una credencial (inexistente): manda esa, no la
        //cookie. La precedencia es del encabezado.
        assertNull(recurso.requireLoginSession("Bearer basura"));
    }

    @Test
    public void testEncabezadoSinCredencialCaeALaCookie() {
        Map<String, IUserSession> pool = new HashMap<>();
        IUserSession session = sesionDeLogin(pool);
        RecursoStub recurso = new RecursoStub(sessionsCon(pool),
                request("GET", cookieDeSesion(ID_CANONICO), new HashMap<>()));

        //Un encabezado mal formado (lo inyectan proxies y clientes HTTP) no
        //aporta credencial: la petición sigue siendo la del navegador.
        assertSame(session, recurso.requireLoginSession("basura"));
    }

    @Test
    public void testCaidaALaCookieNoEsquivaElAntiCsrf() {
        Map<String, IUserSession> pool = new HashMap<>();
        IUserSession session = sesionDeLogin(pool);
        Map<String, String> headers = new HashMap<>();
        RecursoStub recurso = new RecursoStub(sessionsCon(pool),
                request("POST", cookieDeSesion(ID_CANONICO), headers));

        //Con encabezado basura y cookie válida la credencial queda marcada como
        //venida por cookie, así que la mutación sigue exigiendo el encabezado
        //propio: no hay forma de esquivar el control combinando ambos.
        assertNull(recurso.requireLoginSession("basura"));
        headers.put(SessionWebResource.CSRF_HEADER, SessionWebResource.CSRF_HEADER_VALUE);
        assertSame(session, recurso.requireLoginSession("basura"));
    }

    @Test
    public void testEmpresaDeLaSesionUsaLaMascara() {
        Map<String, IUserSession> pool = new HashMap<>();
        UserSession session = sesionDeLogin(pool);
        session.setCompany(empresa(42L, 99L));
        RecursoStub recurso = new RecursoStub(sessionsCon(pool),
                request("GET", null, new HashMap<>()));

        assertEquals(99L, recurso.getIdCompanyFromSession("Bearer " + ID_CANONICO));
    }

    @Test
    public void testEmpresaDeLaSesionSinMascara() {
        Map<String, IUserSession> pool = new HashMap<>();
        UserSession session = sesionDeLogin(pool);
        session.setCompany(empresa(42L, null));
        RecursoStub recurso = new RecursoStub(sessionsCon(pool),
                request("GET", null, new HashMap<>()));

        assertEquals(42L, recurso.getIdCompanyFromSession("Bearer " + ID_CANONICO));
    }

    @Test
    public void testAdministradorDelSistema() {
        Map<String, IUserSession> pool = new HashMap<>();
        UserSession session = sesionDeLogin(pool);
        RecursoStub recurso = new RecursoStub(sessionsCon(pool),
                request("GET", null, new HashMap<>()));

        assertFalse(recurso.isSysAdmin(session));
        session.setUser(usuario(true));
        assertTrue(recurso.isSysAdmin(session));
        assertFalse(recurso.isSysAdmin(null));
    }

    @Test
    public void testNombreDeLaAplicacion() {
        RecursoStub recurso = new RecursoStub(sessionsCon(new HashMap<>()),
                request("GET", null, new HashMap<>()));

        assertEquals(CONTEXT_PATH, recurso.getAppName());
    }

    @Test
    public void testCookieYEncabezadoAntiCsrfSobrescribibles() {
        //La aplicación define su cookie y su valor anti CSRF sobrescribiendo
        //los dos getters; el guard tiene que usar esos, no las constantes.
        Map<String, IUserSession> pool = new HashMap<>();
        IUserSession session = sesionDeLogin(pool);
        Map<String, String> headers = new HashMap<>();
        Cookie[] cookies = new Cookie[]{new Cookie("MiAppSession", ID_CANONICO)};
        RecursoStub recurso = new RecursoStub(sessionsCon(pool),
                request("POST", cookies, headers)) {
            @Override
            protected String getSessionCookieName() {
                return "MiAppSession";
            }

            @Override
            protected String getCsrfHeaderValue() {
                return "MiApp";
            }
        };

        //Sin el encabezado propio la mutación por cookie se rechaza
        assertNull(recurso.requireLoginSession(null));
        //El valor por omisión del framework ya no vale para esta aplicación
        headers.put(SessionWebResource.CSRF_HEADER, SessionWebResource.CSRF_HEADER_VALUE);
        assertNull(recurso.requireLoginSession(null));
        //Con el valor propio de la aplicación, pasa
        headers.put(SessionWebResource.CSRF_HEADER, "MiApp");
        assertSame(session, recurso.requireLoginSession(null));
    }
}

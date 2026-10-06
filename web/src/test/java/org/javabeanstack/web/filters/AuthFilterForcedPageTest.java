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
package org.javabeanstack.web.filters;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.javabeanstack.model.IAppUser;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.security.model.UserSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias del desvío a la página forzada en {@code AuthFilter.filtrar}
 * (plan PWDEXP, M5-02 d), con la petición, la respuesta y la sesión HTTP
 * simuladas por proxies: GET y ajax de otra página se desvían; la propia
 * página, los recursos estáticos y las páginas públicas pasan; sin página
 * forzada todo pasa como antes.
 *
 * @author Jorge Enciso
 */
public class AuthFilterForcedPageTest {

    private static final String FORZADA = "/secure/cambiarclave.xhtml";

    /** Resultado de una pasada por el filtro. */
    private static final class Pasada {
        boolean siguio;
        String redireccion;
        final StringWriter cuerpo = new StringWriter();
    }

    /** Filtro con la página forzada fija (o ninguna). */
    private static AuthFilter filtro(String forzada) {
        return new AuthFilter() {
            @Override
            protected String getForcedPage(HttpServletRequest req, IUserSession userSession) {
                return forzada;
            }
        };
    }

    private static IAppUser usuario() {
        return (IAppUser) Proxy.newProxyInstance(IAppUser.class.getClassLoader(),
                new Class<?>[]{IAppUser.class}, (Object p, Method m, Object[] a) -> {
                    switch (m.getName()) {
                        case "getRol":
                            return "30";
                        case "getLogin":
                            return "ia30";
                        default:
                            return m.getReturnType() == boolean.class ? Boolean.FALSE : null;
                    }
                });
    }

    private static Pasada pasar(AuthFilter filtro, String ruta, boolean ajax) throws Exception {
        UserSession sesion = new UserSession();
        sesion.setSessionId("s1");
        sesion.setUser(usuario());
        Map<String, Object> atributosSesion = new HashMap<>();
        atributosSesion.put("userSession", sesion);
        HttpSession httpSession = (HttpSession) Proxy.newProxyInstance(HttpSession.class.getClassLoader(),
                new Class<?>[]{HttpSession.class}, (Object p, Method m, Object[] a) -> {
                    if ("getAttribute".equals(m.getName())) {
                        return atributosSesion.get((String) a[0]);
                    }
                    return null;
                });
        Map<String, Object> atributosPedido = new HashMap<>();
        HttpServletRequest req = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
                (Object p, Method m, Object[] a) -> {
                    switch (m.getName()) {
                        case "getRequestURL":
                            return new StringBuffer("http://h/app" + ruta);
                        case "getRemoteAddr":
                            return "127.0.0.1";
                        case "getContextPath":
                            return "/app";
                        case "getServletPath":
                            return ruta;
                        case "getPathInfo":
                        case "getRequestedSessionId":
                            return null;
                        case "isRequestedSessionIdValid":
                            return true;
                        case "getSession":
                            return httpSession;
                        case "getHeader":
                            return (ajax && "Faces-Request".equals(a[0])) ? "partial/ajax" : null;
                        case "getAttribute":
                            return atributosPedido.get((String) a[0]);
                        case "setAttribute":
                            atributosPedido.put((String) a[0], a[1]);
                            return null;
                        case "removeAttribute":
                            atributosPedido.remove((String) a[0]);
                            return null;
                        default:
                            return m.getReturnType() == boolean.class ? Boolean.FALSE : null;
                    }
                });
        Pasada pasada = new Pasada();
        PrintWriter escritor = new PrintWriter(pasada.cuerpo);
        HttpServletResponse res = (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(), new Class<?>[]{HttpServletResponse.class},
                (Object p, Method m, Object[] a) -> {
                    switch (m.getName()) {
                        case "sendRedirect":
                            pasada.redireccion = (String) a[0];
                            return null;
                        case "getWriter":
                            return escritor;
                        default:
                            return m.getReturnType() == boolean.class ? Boolean.FALSE : null;
                    }
                });
        FilterChain cadena = (request, response) -> pasada.siguio = true;
        filtro.doFilter(req, res, cadena);
        escritor.flush();
        return pasada;
    }

    @Test
    public void testGetDeOtraPaginaSeDesvia() throws Exception {
        Pasada p = pasar(filtro(FORZADA), "/home.xhtml", false);
        assertFalse(p.siguio);
        assertEquals("/app" + FORZADA, p.redireccion);
        p = pasar(filtro(FORZADA), "/secure/usuarioperfil.xhtml", false);
        assertFalse(p.siguio);
        assertEquals("/app" + FORZADA, p.redireccion);
    }

    @Test
    public void testAjaxRecibeLaRedireccionParcial() throws Exception {
        Pasada p = pasar(filtro(FORZADA), "/secure/forms/cliente.xhtml", true);
        assertFalse(p.siguio);
        assertNull(p.redireccion, "un ajax no recibe un 302");
        assertTrue(p.cuerpo.toString().contains("<redirect url=\"/app" + FORZADA + "\">"),
                p.cuerpo.toString());
    }

    @Test
    public void testLaPropiaPaginaPasaSinBucle() throws Exception {
        Pasada p = pasar(filtro(FORZADA), FORZADA, false);
        assertTrue(p.siguio);
        assertNull(p.redireccion);
        p = pasar(filtro(FORZADA), FORZADA, true);
        assertTrue(p.siguio, "el postback ajax de la propia página pasa");
    }

    @Test
    public void testEstaticosYPublicasPasan() throws Exception {
        assertTrue(pasar(filtro(FORZADA), "/resources/css/maker.css", false).siguio);
        assertTrue(pasar(filtro(FORZADA), "/jakarta.faces.resource/core.js.xhtml", false).siguio);
        assertTrue(pasar(filtro(FORZADA), "/login.xhtml", false).siguio);
    }

    @Test
    public void testSinPaginaForzadaTodoPasa() throws Exception {
        Pasada p = pasar(filtro(null), "/home.xhtml", false);
        assertTrue(p.siguio);
        assertNull(p.redireccion);
        assertTrue(pasar(filtro(""), "/secure/usuarioperfil.xhtml", true).siguio);
    }
}

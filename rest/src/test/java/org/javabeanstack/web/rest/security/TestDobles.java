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
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.model.IAppUser;

/**
 * Dobles por {@code Proxy} compartidos por los tests del paquete: sin Mockito,
 * como el resto de los tests de jbs-rest.
 */
final class TestDobles {

    private TestDobles() {
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

    /**
     * Request con método, cookies, encabezados y context path; guarda los
     * atributos en un mapa para que el test pueda leerlos.
     */
    static HttpServletRequest request(String metodo, Cookie[] cookies, Map<String, String> headers,
            String contextPath) {
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
                            return contextPath;
                        case "setAttribute":
                            atributos.put((String) args[0], args[1]);
                            return null;
                        case "getAttribute":
                            return atributos.get((String) args[0]);
                        case "getRemoteAddr":
                            return "127.0.0.1";
                        default:
                            return porOmision(method);
                    }
                });
    }

    static IAppUser usuario(boolean sysAdmin, String rol) {
        return (IAppUser) Proxy.newProxyInstance(IAppUser.class.getClassLoader(),
                new Class<?>[]{IAppUser.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "isSysAdmin":
                            return sysAdmin;
                        case "getLogin":
                        case "getCode":
                            return "USUARIO";
                        case "getRol":
                            return rol;
                        default:
                            return porOmision(method);
                    }
                });
    }

    static IAppCompany empresa(Long idcompany, Long idcompanymask) {
        return (IAppCompany) Proxy.newProxyInstance(IAppCompany.class.getClassLoader(),
                new Class<?>[]{IAppCompany.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getIdcompany":
                            return idcompany;
                        case "getIdcompanymask":
                            return idcompanymask;
                        default:
                            return porOmision(method);
                    }
                });
    }
}

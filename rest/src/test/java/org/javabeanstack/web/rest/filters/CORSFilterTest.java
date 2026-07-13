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
package org.javabeanstack.web.rest.filters;

import java.lang.reflect.Proxy;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de CORSFilter: el filtro debe agregar los encabezados CORS
 * a la respuesta de todo recurso REST.
 *
 * @author Jorge Enciso
 */
public class CORSFilterTest {

    public CORSFilterTest() {
    }

    /**
     * Test of filter method, of class CORSFilter.
     */
    @Test
    public void testFilter() throws Exception {
        System.out.println("corsFilter filter");
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

        ContainerRequestContext request = (ContainerRequestContext) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{ContainerRequestContext.class},
                (proxy, method, args) -> "getMethod".equals(method.getName()) ? "OPTIONS" : null);

        ContainerResponseContext response = (ContainerResponseContext) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{ContainerResponseContext.class},
                (proxy, method, args) -> "getHeaders".equals(method.getName()) ? headers : null);

        CORSFilter filter = new CORSFilter();
        filter.filter(request, response);

        assertEquals("*", headers.getFirst("Access-Control-Allow-Origin"));
        assertEquals("origin, content-type, accept, authorization, token",
                headers.getFirst("Access-Control-Allow-Headers"));
        assertEquals("true", headers.getFirst("Access-Control-Allow-Credentials"));
        assertEquals("GET, POST, PUT, DELETE, OPTIONS, HEAD",
                headers.getFirst("Access-Control-Allow-Methods"));
        assertEquals("1728000", headers.getFirst("Access-Control-Max-Age"));
    }
}

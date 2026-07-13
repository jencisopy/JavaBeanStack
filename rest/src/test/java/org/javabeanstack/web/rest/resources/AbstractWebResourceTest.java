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

import java.lang.reflect.Proxy;

import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.security.ISecManager;
import org.javabeanstack.security.model.IClientAuthRequestInfo;
import org.javabeanstack.web.rest.exceptions.TokenError;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de AbstractWebResource: extracción del token del
 * encabezado Authorization y resolución de la sesión/empresa a partir del
 * cache de autenticación. El ISecManager se simula con un proxy; los caminos
 * que requieren el EJB IOAuthConsumer o el request real quedan para los tests
 * de integración.
 *
 * @author Jorge Enciso
 */
public class AbstractWebResourceTest {

    private static final String TOKEN_VALIDO = "tok123";

    public AbstractWebResourceTest() {
    }

    /**
     * Recurso de prueba: reemplaza el lookup del ISecManager por un stub que
     * solo reconoce TOKEN_VALIDO en el cache de autenticación.
     */
    static class WebResourceStub extends AbstractWebResource {

        @Override
        public <T extends IDataService> T getDataService() {
            return null;
        }

        @Override
        public ISecManager getSecManager() {
            IClientAuthRequestInfo info = (IClientAuthRequestInfo) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class[]{IClientAuthRequestInfo.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "getToken":
                                return TOKEN_VALIDO;
                            case "getIdcompany":
                                return 2L;
                            default:
                                return null;
                        }
                    });
            return (ISecManager) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class[]{ISecManager.class},
                    (proxy, method, args) -> {
                        if ("getClientAuthRequestCache".equals(method.getName())
                                && TOKEN_VALIDO.equals(args[0])) {
                            return info;
                        }
                        //createSessionFromToken y el resto devuelven nulo
                        return null;
                    });
        }

        @Override
        public ISecManager getSecManager(String jndi) {
            return getSecManager();
        }
    }

    /**
     * Test of getTokenFromHeader: extrae el token del encabezado Authorization
     * con esquema (ej. "Bearer token").
     */
    @Test
    public void testGetTokenFromHeader() {
        System.out.println("abstractWebResource getTokenFromHeader");
        WebResourceStub resource = new WebResourceStub();
        assertEquals(TOKEN_VALIDO, resource.getTokenFromHeader("Bearer " + TOKEN_VALIDO));
    }

    /**
     * Test of getTokenFromHeader: sin encabezado o con encabezado mal formado
     * (sin esquema, ej. "Bearer" solo o el token pelado) debe lanzar
     * TokenError, nunca otra excepción.
     */
    @Test
    public void testGetTokenFromHeaderInvalido() {
        System.out.println("abstractWebResource getTokenFromHeaderInvalido");
        WebResourceStub resource = new WebResourceStub();
        assertThrows(TokenError.class, () -> resource.getTokenFromHeader(null));
        assertThrows(TokenError.class, () -> resource.getTokenFromHeader(""));
        assertThrows(TokenError.class, () -> resource.getTokenFromHeader("Bearer"));
        assertThrows(TokenError.class, () -> resource.getTokenFromHeader(TOKEN_VALIDO));
    }

    /**
     * Test of getIdCompany: devuelve la empresa asociada al token autenticado
     * y nulo si el token no está en el cache.
     */
    @Test
    public void testGetIdCompany() {
        System.out.println("abstractWebResource getIdCompany");
        WebResourceStub resource = new WebResourceStub();
        assertEquals(2L, resource.getIdCompany("Bearer " + TOKEN_VALIDO));
        assertNull(resource.getIdCompany("Bearer desconocido"));
    }

    /**
     * Test of getToken: devuelve el token registrado en el cache de
     * autenticación y nulo si no existe.
     */
    @Test
    public void testGetToken() {
        System.out.println("abstractWebResource getToken");
        WebResourceStub resource = new WebResourceStub();
        assertEquals(TOKEN_VALIDO, resource.getToken("Bearer " + TOKEN_VALIDO));
        assertNull(resource.getToken("Bearer desconocido"));
    }

    /**
     * Test of setToken: con el token ya activo en el cache no lanza error; con
     * un token desconocido (sin sesión posible) o sin encabezado lanza
     * TokenError.
     */
    @Test
    public void testSetToken() {
        System.out.println("abstractWebResource setToken");
        WebResourceStub resource = new WebResourceStub();
        assertDoesNotThrow(() -> resource.setToken("Bearer " + TOKEN_VALIDO));
        assertThrows(TokenError.class, () -> resource.setToken("Bearer desconocido"));
        assertThrows(TokenError.class, () -> resource.setToken(null));
    }
}

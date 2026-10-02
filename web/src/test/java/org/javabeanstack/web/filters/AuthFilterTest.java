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
package org.javabeanstack.web.filters;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de las reglas puras de AuthFilter: qué es un recurso
 * estático (no renueva la sesión del pool) y la redirección de Faces para los
 * pedidos ajax. El resto del filtro necesita el contenedor.
 *
 * @author Jorge Enciso
 */
public class AuthFilterTest {

    /**
     * Las hojas de estilo, scripts, imágenes, fuentes y recursos de Faces son
     * estáticos; las páginas y los servlets no.
     */
    @Test
    public void testHasStaticExtension() {
        assertTrue(AuthFilter.hasStaticExtension("http://h/app/resources/css/maker.css"));
        assertTrue(AuthFilter.hasStaticExtension("http://h/app/resources/js/sesexp.js?v=1"));
        assertTrue(AuthFilter.hasStaticExtension("http://h/app/resources/images/logo.png"));
        assertTrue(AuthFilter.hasStaticExtension("http://h/app/fonts/x.woff2"));
        assertTrue(AuthFilter.hasStaticExtension("http://h/app/jakarta.faces.resource/core.js.xhtml?ln=primefaces"));
        assertFalse(AuthFilter.hasStaticExtension("http://h/app/secure/cliente.xhtml"));
        assertFalse(AuthFilter.hasStaticExtension("http://h/app/home.xhtml?x=a.css"));
        assertFalse(AuthFilter.hasStaticExtension("http://h/app/usuariofoto"));
        assertFalse(AuthFilter.hasStaticExtension(null));
    }

    /**
     * La redirección parcial lleva la dirección escapada como atributo XML.
     */
    @Test
    public void testPartialRedirect() {
        String xml = AuthFilter.partialRedirect("/app/login.xhtml?inactividad=1&a=\"b\"");
        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?><partial-response>"));
        assertTrue(xml.contains("<redirect url=\"/app/login.xhtml?inactividad=1&amp;a=&quot;b&quot;\"></redirect>"));
        assertTrue(xml.endsWith("</partial-response>"));
    }

    /**
     * Los servicios públicos tienen que ser el primer segmento de la ruta: un
     * texto parecido en otra parte ya no deja pública la dirección (M2-03).
     */
    @Test
    public void testIsPathUnder() {
        assertTrue(AuthFilter.isPathUnder("/webresources", "/webresources"));
        assertTrue(AuthFilter.isPathUnder("/webresources/x/y", "/webresources"));
        assertTrue(AuthFilter.isPathUnder("/upload/a.bin", "/upload"));
        assertFalse(AuthFilter.isPathUnder("/secure/upload/a.xhtml", "/upload"));
        assertFalse(AuthFilter.isPathUnder("/uploads.xhtml", "/upload"));
        assertFalse(AuthFilter.isPathUnder("/secure/forms/excel_upload_dlg2.xhtml", "/upload"));
        assertFalse(AuthFilter.isPathUnder("/x/webresources/y", "/webresources"));
        assertFalse(AuthFilter.isPathUnder(null, "/upload"));
    }

    /**
     * Por omisión, sin sesión se va al ingreso sin aviso, haya o no una sesión
     * perdida (las aplicaciones lo redefinen).
     */
    @Test
    public void testGetNoSessionPage() {
        AuthFilter filtro = new AuthFilter();
        assertEquals("/login.xhtml", filtro.getNoSessionPage(true));
        assertEquals("/login.xhtml", filtro.getNoSessionPage(false));
    }
}

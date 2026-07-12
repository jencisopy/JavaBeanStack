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
package org.javabeanstack.xml;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pruebas unitarias de XmlResourceSearcher sin acceso a base de datos (sin
 * DAO asignado debe delegar en el comportamiento heredado de XmlSearcher).
 *
 * @author Jorge Enciso
 */
public class XmlResourceSearcherTest {

    private static final String XML
            = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><child/></root>";

    @TempDir
    Path tempDir;

    public XmlResourceSearcherTest() {
    }

    /**
     * Test of isSearchAvailable method: además de file debe aceptar obj.
     */
    @Test
    public void testIsSearchAvailable() {
        System.out.println("xmlResourceSearcher isSearchAvailable");
        XmlResourceSearcher<Document> searcher = new XmlResourceSearcher<>();
        assertTrue(searcher.isSearchAvailable("file"));
        assertTrue(searcher.isSearchAvailable("obj"));
        assertTrue(searcher.isSearchAvailable("OBJ:"));
        assertFalse(searcher.isSearchAvailable("http"));
    }

    /**
     * Test of getDao y setDao methods, of class XmlResourceSearcher.
     */
    @Test
    public void testGetSetDao() {
        System.out.println("xmlResourceSearcher getSetDao");
        XmlResourceSearcher<Document> searcher = new XmlResourceSearcher<>();
        assertNull(searcher.getDao());
    }

    /**
     * Test of search method sin DAO: debe recurrir a la búsqueda por archivo
     * heredada sin propagar excepción.
     */
    @Test
    public void testSearchSinDao() throws Exception {
        System.out.println("xmlResourceSearcher searchSinDao");
        Path file = tempDir.resolve("recurso.xml");
        Files.write(file, XML.getBytes(StandardCharsets.UTF_8));

        XmlResourceSearcher<Document> searcher = new XmlResourceSearcher<>();
        IXmlDom<Document, ?> context = new XmlDomW3c();
        String result = searcher.search(context, "file://" + file.toFile().getPath());
        assertNotNull(result);
        assertTrue(result.contains("<root>"));
        // Sin DAO y sin contexto debe devolver null sin propagar excepción
        assertNull(searcher.search(null, "obj://recurso"));
    }

    /**
     * Test of exist method sin DAO: debe delegar en la verificación de
     * archivos heredada.
     */
    @Test
    public void testExistSinDao() throws Exception {
        System.out.println("xmlResourceSearcher existSinDao");
        Path file = tempDir.resolve("existe.xml");
        Files.write(file, XML.getBytes(StandardCharsets.UTF_8));

        XmlResourceSearcher<Document> searcher = new XmlResourceSearcher<>();
        assertTrue(searcher.exist("file://" + file.toFile().getPath()));
        assertFalse(searcher.exist("http://dominio/recurso.xml"));
    }
}

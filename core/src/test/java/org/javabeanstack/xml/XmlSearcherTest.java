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
 * Pruebas unitarias de XmlSearcher.
 *
 * @author Jorge Enciso
 */
public class XmlSearcherTest {

    private static final String XML
            = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><child/></root>";

    @TempDir
    Path tempDir;

    public XmlSearcherTest() {
    }

    private File createXmlFile(String name) throws Exception {
        Path file = tempDir.resolve(name);
        Files.write(file, XML.getBytes(StandardCharsets.UTF_8));
        return file.toFile();
    }

    /**
     * Test of getPathType method, of class XmlSearcher.
     */
    @Test
    public void testGetPathType() {
        System.out.println("xmlSearcher getPathType");
        assertEquals("file", XmlSearcher.getPathType("file:///carpeta/archivo.xml"));
        assertEquals("obj", XmlSearcher.getPathType("OBJ://recurso"));
        assertEquals("", XmlSearcher.getPathType("/carpeta/archivo.xml"));
    }

    /**
     * Test of getJustPath method, of class XmlSearcher.
     */
    @Test
    public void testGetJustPath() {
        System.out.println("xmlSearcher getJustPath");
        assertEquals("/carpeta/archivo.xml", XmlSearcher.getJustPath("file:///carpeta/archivo.xml"));
        assertEquals("recurso", XmlSearcher.getJustPath("obj://recurso"));
        assertEquals("/carpeta/archivo.xml", XmlSearcher.getJustPath("/carpeta/archivo.xml"));
    }

    /**
     * Test of isSearchAvailable method, of class XmlSearcher.
     */
    @Test
    public void testIsSearchAvailable() {
        System.out.println("xmlSearcher isSearchAvailable");
        XmlSearcher<Document> searcher = new XmlSearcher<>();
        assertTrue(searcher.isSearchAvailable("file"));
        assertTrue(searcher.isSearchAvailable("FILE:"));
        assertFalse(searcher.isSearchAvailable("obj"));
        assertFalse(searcher.isSearchAvailable("http"));
    }

    /**
     * Test of addParam, getParam y getParams methods, of class XmlSearcher.
     */
    @Test
    public void testParams() {
        System.out.println("xmlSearcher params");
        XmlSearcher<Document> searcher = new XmlSearcher<>();
        searcher.addParam("clave", "valor");
        assertEquals("valor", searcher.getParam("clave"));
        // El map es case insensitive
        assertEquals("valor", searcher.getParam("CLAVE"));
        assertEquals(1, searcher.getParams().size());
    }

    /**
     * Test of search method: debe leer el texto xml desde un archivo.
     */
    @Test
    public void testSearch() throws Exception {
        System.out.println("xmlSearcher search");
        File file = createXmlFile("busqueda.xml");
        XmlSearcher<Document> searcher = new XmlSearcher<>();
        IXmlDom<Document, ?> context = new XmlDomW3c();

        String result = searcher.search(context, "file://" + file.getPath());
        assertNotNull(result);
        assertTrue(result.contains("<root>"));
        // Archivo inexistente devuelve texto vacio
        String resultNoFile = searcher.search(context, "file://" + file.getPath() + ".noexiste");
        assertEquals("", resultNoFile);
    }

    /**
     * Test of exist method, of class XmlSearcher.
     */
    @Test
    public void testExist() throws Exception {
        System.out.println("xmlSearcher exist");
        File file = createXmlFile("existe.xml");
        XmlSearcher<Document> searcher = new XmlSearcher<>();
        assertTrue(searcher.exist("file://" + file.getPath()));
        // Los tipos de path no soportados devuelven falso
        assertFalse(searcher.exist("obj://recurso"));
    }

    /**
     * Test of addToCache, getFromCache y isValidCache methods, of class
     * XmlSearcher.
     */
    @Test
    public void testCache() throws Exception {
        System.out.println("xmlSearcher cache");
        File file = createXmlFile("cacheado.xml");
        String documentPath = "file://" + file.getPath();

        XmlSearcher<Document> searcher = new XmlSearcher<>();
        IXmlDom<Document, ?> context = new XmlDomW3c();
        Document document = DomW3cParser.loadXml(XML);

        assertTrue(searcher.getCache().isEmpty());
        searcher.addToCache(context, documentPath, document);
        assertEquals(1, searcher.getCache().size());

        IXmlCache<Document> cache = searcher.getFromCache(documentPath);
        assertNotNull(cache);
        assertSame(document, cache.getDom());
        assertTrue(searcher.isValidCache(documentPath));

        // Elemento no cacheado
        assertNull(searcher.getFromCache("file:///otro/path.xml"));
        assertFalse(searcher.isValidCache("file:///otro/path.xml"));

        // Con el cache desactivado no debe devolver nada
        searcher.setUseCache(false);
        assertFalse(searcher.getUseCache());
        assertNull(searcher.getFromCache(documentPath));
    }
}

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

import java.util.Date;

import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de XmlCache.
 *
 * @author Jorge Enciso
 */
public class XmlCacheTest {

    private static final String XML
            = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><child/></root>";

    public XmlCacheTest() {
    }

    private Document getDocument() throws Exception {
        return DomW3cParser.loadXml(XML);
    }

    /**
     * Test del constructor con objeto DOM: debe registrar la fecha de proceso
     * y devolver el DOM contabilizando las referencias.
     */
    @Test
    public void testConstructorConDom() throws Exception {
        System.out.println("xmlCache constructorConDom");
        Document document = getDocument();
        XmlCache<Document> cache = new XmlCache<>(document);
        assertNotNull(cache.getProcessTime());
        assertEquals(0L, (long) cache.getReferenceTimes());
        assertNull(cache.getLastReference());

        assertSame(document, cache.getDom());
        assertEquals(1L, (long) cache.getReferenceTimes());
        assertNotNull(cache.getLastReference());
        cache.getDom();
        assertEquals(2L, (long) cache.getReferenceTimes());
    }

    /**
     * Test del constructor con texto xml.
     */
    @Test
    public void testConstructorConTexto() {
        System.out.println("xmlCache constructorConTexto");
        XmlCache<Document> cache = new XmlCache<>(XML);
        assertEquals(XML, cache.getXmlText());
        assertNotNull(cache.getProcessTime());
    }

    /**
     * Test of isValid y getDom(Date) methods: la fecha de modificación debe
     * ser anterior o igual a la fecha de proceso para que el cache sea
     * válido.
     */
    @Test
    public void testVigencia() throws Exception {
        System.out.println("xmlCache vigencia");
        Document document = getDocument();
        Date antes = new Date(System.currentTimeMillis() - 60000L);
        Date despues = new Date(System.currentTimeMillis() + 60000L);
        XmlCache<Document> cache = new XmlCache<>(document);

        assertTrue(cache.isValid(antes));
        assertFalse(cache.isValid(despues));

        assertSame(document, cache.getDom(antes));
        assertNull(cache.getDom(despues));
    }

    /**
     * Test of setProcessTime method, of class XmlCache.
     */
    @Test
    public void testSetProcessTime() throws Exception {
        System.out.println("xmlCache setProcessTime");
        XmlCache<Document> cache = new XmlCache<>(getDocument());
        Date fecha = new Date(0L);
        cache.setProcessTime(fecha);
        assertEquals(fecha, cache.getProcessTime());
    }

    /**
     * Test of setDom method: debe reiniciar contadores y limpiar el texto.
     */
    @Test
    public void testSetDom() throws Exception {
        System.out.println("xmlCache setDom");
        XmlCache<Document> cache = new XmlCache<>(XML);
        Document document = getDocument();
        cache.setDom(document);
        assertEquals(0L, (long) cache.getReferenceTimes());
        assertNull(cache.getLastReference());
        assertEquals("", cache.getXmlText());
        assertSame(document, cache.getDom());
    }

    /**
     * Test of setXmlText method: debe reiniciar contadores y anular el DOM.
     */
    @Test
    public void testSetXmlText() throws Exception {
        System.out.println("xmlCache setXmlText");
        XmlCache<Document> cache = new XmlCache<>(getDocument());
        cache.setXmlText(XML);
        assertEquals(XML, cache.getXmlText());
        assertEquals(0L, (long) cache.getReferenceTimes());
        assertNull(cache.getLastReference());
    }

    /**
     * Test of isCompiled y setCompiled methods, of class XmlCache.
     */
    @Test
    public void testCompiled() throws Exception {
        System.out.println("xmlCache compiled");
        XmlCache<Document> cache = new XmlCache<>(getDocument());
        assertFalse(cache.isCompiled());
        cache.setCompiled(true);
        assertTrue(cache.isCompiled());
    }
}

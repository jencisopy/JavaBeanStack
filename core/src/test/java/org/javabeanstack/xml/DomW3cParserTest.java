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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de DomW3cParser.
 *
 * @author Jorge Enciso
 */
public class DomW3cParserTest {

    private static final String XML
            = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<root version=\"1\">"
            + "<child1 attr=\"valor1\"/>"
            + "<child2 attr=\"valor2\"><nieto/></child2>"
            + "</root>";

    public DomW3cParserTest() {
    }

    /**
     * Test of newDocument method, of class DomW3cParser.
     */
    @Test
    public void testNewDocument() throws Exception {
        System.out.println("domW3cParser newDocument");
        Document document = DomW3cParser.newDocument();
        assertNotNull(document);
        assertNull(document.getDocumentElement());
    }

    /**
     * Test of loadXml method (String), of class DomW3cParser.
     */
    @Test
    public void testLoadXmlFromString() throws Exception {
        System.out.println("domW3cParser loadXmlFromString");
        Document document = DomW3cParser.loadXml(XML);
        assertNotNull(document);
        assertEquals("root", document.getDocumentElement().getNodeName());
        // Texto vacio o nulo debe devolver null
        assertNull(DomW3cParser.loadXml(""));
        assertNull(DomW3cParser.loadXml((String) null));
    }

    /**
     * Test of loadXml method (InputStream), of class DomW3cParser.
     */
    @Test
    public void testLoadXmlFromStream() throws Exception {
        System.out.println("domW3cParser loadXmlFromStream");
        InputStream stream = new ByteArrayInputStream(XML.getBytes(StandardCharsets.UTF_8));
        Document document = DomW3cParser.loadXml(stream);
        assertNotNull(document);
        assertEquals("root", document.getDocumentElement().getNodeName());
    }

    /**
     * Test of getElement method, of class DomW3cParser (búsqueda por XPath).
     */
    @Test
    public void testGetElement() throws Exception {
        System.out.println("domW3cParser getElement");
        Document document = DomW3cParser.loadXml(XML);
        Element element = DomW3cParser.getElement(document, "/root/child1");
        assertNotNull(element);
        assertEquals("child1", element.getNodeName());
        // Path relativo (se le antepone //)
        Element nieto = DomW3cParser.getElement(document, "nieto");
        assertNotNull(nieto);
        // Elemento inexistente
        assertNull(DomW3cParser.getElement(document, "/root/noexiste"));
    }

    /**
     * Test of getElements y getChildren methods, of class DomW3cParser.
     */
    @Test
    public void testGetElementsAndChildren() throws Exception {
        System.out.println("domW3cParser getElementsAndChildren");
        Document document = DomW3cParser.loadXml(XML);
        List<Element> elements = DomW3cParser.getElements(document, "/root/*");
        assertEquals(2, elements.size());

        List<Element> children = DomW3cParser.getChildren(document, "/root");
        assertEquals(2, children.size());
        assertEquals("child1", children.get(0).getNodeName());
        assertEquals("child2", children.get(1).getNodeName());

        Element child2 = DomW3cParser.getChild(document.getDocumentElement(), "child2");
        assertNotNull(child2);
        assertEquals("child2", child2.getNodeName());
        assertNull(DomW3cParser.getChild(document.getDocumentElement(), "noexiste"));
    }

    /**
     * Test of createElement method, of class DomW3cParser.
     */
    @Test
    public void testCreateElement() throws Exception {
        System.out.println("domW3cParser createElement");
        Document document = DomW3cParser.loadXml(XML);
        Element element = DomW3cParser.createElement(document, "child3", "/root");
        assertNotNull(element);
        assertEquals(3, DomW3cParser.getChildren(document.getDocumentElement()).size());
        // Nodo padre inexistente
        assertNull(DomW3cParser.createElement(document, "child4", "/noexiste"));
    }

    /**
     * Test of getPropertyValue y setPropertyValue methods, of class
     * DomW3cParser.
     */
    @Test
    public void testGetSetPropertyValue() throws Exception {
        System.out.println("domW3cParser getSetPropertyValue");
        Document document = DomW3cParser.loadXml(XML);
        assertEquals("valor1", DomW3cParser.getPropertyValue(document, "attr", "/root/child1"));
        // Atributo inexistente
        assertNull(DomW3cParser.getPropertyValue(document, "noexiste", "/root/child1"));

        boolean result = DomW3cParser.setPropertyValue(document, "nuevo", "attr", "/root/child1");
        assertTrue(result);
        assertEquals("nuevo", DomW3cParser.getPropertyValue(document, "attr", "/root/child1"));
        // Nodo inexistente
        assertFalse(DomW3cParser.setPropertyValue(document, "x", "attr", "/root/noexiste"));
    }

    /**
     * Test of removeElement method, of class DomW3cParser.
     */
    @Test
    public void testRemoveElement() throws Exception {
        System.out.println("domW3cParser removeElement");
        Document document = DomW3cParser.loadXml(XML);
        boolean result = DomW3cParser.removeElement(document, "/root/child1");
        assertTrue(result);
        assertNull(DomW3cParser.getElement(document, "/root/child1"));
        // Elemento inexistente
        assertFalse(DomW3cParser.removeElement(document, "/root/child1"));
    }

    /**
     * Test of getXmlText method, of class DomW3cParser (ida y vuelta).
     */
    @Test
    public void testGetXmlText() throws Exception {
        System.out.println("domW3cParser getXmlText");
        Document document = DomW3cParser.loadXml(XML);
        String xmlText = DomW3cParser.getXmlText(document);
        assertNotNull(xmlText);
        assertTrue(xmlText.contains("<root"));
        assertTrue(xmlText.contains("child2"));
        // El texto generado debe poder volver a parsearse
        Document document2 = DomW3cParser.loadXml(xmlText);
        assertEquals("root", document2.getDocumentElement().getNodeName());
        // Contenido nulo
        assertNull(DomW3cParser.getXmlText(null, "UTF-8"));
    }
}

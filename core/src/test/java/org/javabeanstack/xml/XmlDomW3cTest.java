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
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pruebas unitarias de XmlDomW3c.
 *
 * @author Jorge Enciso
 */
public class XmlDomW3cTest {

    private static final String XML
            = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<root version=\"1\">"
            + "<child1 attr=\"valor1\"/>"
            + "<child2 attr=\"valor2\"/>"
            + "</root>";

    @TempDir
    Path tempDir;

    public XmlDomW3cTest() {
    }

    /**
     * Test of config method a partir de un texto xml.
     */
    @Test
    public void testConfig() {
        System.out.println("xmlDomW3c config");
        XmlDomW3c xmlDom = new XmlDomW3c();
        boolean result = xmlDom.config("", XML, "", true);
        assertTrue(result);
        assertNotNull(xmlDom.getDom());
        assertEquals("root", xmlDom.getDom().getDocumentElement().getNodeName());
        assertNull(xmlDom.getException());
        // getXml debe devolver el texto procesado
        String xmlText = xmlDom.getXml();
        assertNotNull(xmlText);
        assertTrue(xmlText.contains("child1"));
    }

    /**
     * Test of config method con texto vacio: debe fallar sin excepción.
     */
    @Test
    public void testConfigVacio() {
        System.out.println("xmlDomW3c configVacio");
        XmlDomW3c xmlDom = new XmlDomW3c();
        boolean result = xmlDom.config("", "", "", true);
        assertFalse(result);
        assertNull(xmlDom.getDom());
    }

    /**
     * Test de reemplazo de atributos con las expresiones {},${},#{} a partir
     * de configParam.
     */
    @Test
    public void testConfigConParams() {
        System.out.println("xmlDomW3c configConParams");
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<root color=\"{color}\" tamano=\"${tamano}\"/>";
        Map<String, String> params = new HashMap<>();
        params.put("color", "rojo");
        params.put("tamano", "10");

        XmlDomW3c xmlDom = new XmlDomW3c();
        boolean result = xmlDom.config("", xml, "", true, params);
        assertTrue(result);
        assertEquals("rojo", xmlDom.getPropertyValue("color", "/root"));
        assertEquals("10", xmlDom.getPropertyValue("tamano", "/root"));
        // El texto original debe conservarse antes del reemplazo
        assertTrue(xmlDom.getOriginalXmlText().contains("{color}"));
    }

    /**
     * Test de herencia: el elemento con atributo "clase" debe heredar
     * atributos y nodos hijos de la clase definida en el archivo src.
     */
    @Test
    public void testConfigConHerencia() throws Exception {
        System.out.println("xmlDomW3c configConHerencia");
        String baseXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<base color=\"rojo\" comun=\"si\"><item attr=\"1\"/></base>";
        Path baseFile = tempDir.resolve("base.xml");
        Files.write(baseFile, baseXml.getBytes(StandardCharsets.UTF_8));

        String mainXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<main clase=\"base\" src=\"file://" + baseFile.toFile().getPath() + "\" color=\"azul\"/>";

        XmlDomW3c xmlDom = new XmlDomW3c();
        boolean result = xmlDom.config("", mainXml, "", false);
        assertTrue(result);
        Element root = xmlDom.getDom().getDocumentElement();
        // El atributo propio no debe ser pisado por el de la clase
        assertEquals("azul", root.getAttribute("color"));
        // El atributo de la clase debe heredarse
        assertEquals("si", root.getAttribute("comun"));
        // El nodo hijo de la clase debe heredarse
        assertEquals(1, xmlDom.getChildren("/main").size());
        assertEquals("item", xmlDom.getChildren("/main").get(0).getNodeName());
    }

    /**
     * Test of createElement e insertElement methods, of class XmlDomW3c.
     */
    @Test
    public void testCreateInsertElement() throws Exception {
        System.out.println("xmlDomW3c createInsertElement");
        XmlDomW3c xmlDom = new XmlDomW3c();
        xmlDom.config("", XML, "", true);

        assertTrue(xmlDom.createElement("child3", "/root"));
        assertEquals(3, xmlDom.getChildren("/root").size());

        // insertElement en una posición determinada
        assertTrue(xmlDom.insertElement("child0", "/root", 0));
        assertEquals(4, xmlDom.getChildren("/root").size());
        assertEquals("child0", xmlDom.getChildren("/root").get(0).getNodeName());
    }

    /**
     * Test of getPropertyValue y setPropertyValue methods, of class
     * XmlDomW3c.
     */
    @Test
    public void testGetSetPropertyValue() {
        System.out.println("xmlDomW3c getSetPropertyValue");
        XmlDomW3c xmlDom = new XmlDomW3c();
        xmlDom.config("", XML, "", true);

        assertEquals("valor1", xmlDom.getPropertyValue("attr", "/root/child1"));
        assertTrue(xmlDom.setPropertyValue("nuevo", "attr", "/root/child1"));
        assertEquals("nuevo", xmlDom.getPropertyValue("attr", "/root/child1"));
        assertFalse(xmlDom.setPropertyValue("x", "attr", "/root/noexiste"));
    }

    /**
     * Test of removeElement y removeChildren methods, of class XmlDomW3c.
     */
    @Test
    public void testRemove() throws Exception {
        System.out.println("xmlDomW3c remove");
        XmlDomW3c xmlDom = new XmlDomW3c();
        xmlDom.config("", XML, "", true);

        assertTrue(xmlDom.removeElement("/root/child1"));
        assertEquals(1, xmlDom.getChildren("/root").size());

        assertTrue(xmlDom.removeChildren("/root"));
        assertEquals(0, xmlDom.getChildren("/root").size());
    }

    /**
     * Test of configParam methods, of class XmlDomW3c.
     */
    @Test
    public void testConfigParam() {
        System.out.println("xmlDomW3c configParam");
        XmlDomW3c xmlDom = new XmlDomW3c();
        xmlDom.addConfigParam("clave", "valor");
        // El map es case insensitive
        assertEquals("valor", xmlDom.getConfigParam().get("CLAVE"));

        Map<String, String> params = new HashMap<>();
        params.put("otra", "cosa");
        xmlDom.setConfigParam(params);
        assertEquals(1, xmlDom.getConfigParam().size());
        assertEquals("cosa", xmlDom.getConfigParam().get("otra"));
    }

    /**
     * Test of getXmlSearcher y setXmlSearcher methods, of class XmlDomW3c.
     */
    @Test
    public void testXmlSearcher() {
        System.out.println("xmlDomW3c xmlSearcher");
        XmlDomW3c xmlDom = new XmlDomW3c();
        assertNotNull(xmlDom.getXmlSearcher());
        IXmlSearcher<org.w3c.dom.Document> searcher = new XmlSearcher<>();
        xmlDom.setXmlSearcher(searcher);
        assertSame(searcher, xmlDom.getXmlSearcher());
    }
}

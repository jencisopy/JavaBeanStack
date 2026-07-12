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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de XmlCacheKey.
 *
 * @author Jorge Enciso
 */
public class XmlCacheKeyTest {

    public XmlCacheKeyTest() {
    }

    /**
     * Test del constructor con documentPath y elementPath.
     */
    @Test
    public void testConstructorDocumentPath() {
        System.out.println("xmlCacheKey constructorDocumentPath");
        XmlCacheKey key = new XmlCacheKey("file:///Carpeta/Archivo.xml", " elemento ");
        assertEquals("file", key.getPathType());
        assertEquals("/carpeta/archivo.xml", key.getDocumentPath());
        assertEquals("elemento", key.getElementPath());
    }

    /**
     * Test del constructor con documentPath sin tipo: debe asumir FILEPATH.
     */
    @Test
    public void testConstructorSinPathType() {
        System.out.println("xmlCacheKey constructorSinPathType");
        XmlCacheKey key = new XmlCacheKey("/carpeta/archivo.xml", "elemento");
        assertEquals(IXmlSearcher.FILEPATH, key.getPathType());
        assertEquals("/carpeta/archivo.xml", key.getDocumentPath());
    }

    /**
     * Test del constructor con pathType explicito y vacio.
     */
    @Test
    public void testConstructorPathTypeExplicito() {
        System.out.println("xmlCacheKey constructorPathTypeExplicito");
        XmlCacheKey key = new XmlCacheKey("OBJ", "recurso.xml", "elemento");
        assertEquals("obj", key.getPathType());
        // pathType vacio: se deriva del documentPath
        XmlCacheKey key2 = new XmlCacheKey("", "obj://recurso.xml", "elemento");
        assertEquals("obj", key2.getPathType());
        assertEquals("recurso.xml", key2.getDocumentPath());
    }

    /**
     * Test de setters: setPathType debe normalizar (minúsculas y sin ":"
     * final).
     */
    @Test
    public void testSetters() {
        System.out.println("xmlCacheKey setters");
        XmlCacheKey key = new XmlCacheKey("file:///a.xml", "e");
        key.setPathType(" FILE: ");
        assertEquals("file", key.getPathType());
        key.setDocumentPath("file:///Otro.XML");
        assertEquals("/otro.xml", key.getDocumentPath());
        key.setElementPath("otroElemento");
        assertEquals("otroElemento", key.getElementPath());
    }

    /**
     * Test de equals y hashCode.
     */
    @Test
    public void testEqualsHashCode() {
        System.out.println("xmlCacheKey equalsHashCode");
        XmlCacheKey key1 = new XmlCacheKey("file:///carpeta/archivo.xml", "elemento");
        XmlCacheKey key2 = new XmlCacheKey("file:///CARPETA/Archivo.XML", "elemento");
        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());

        XmlCacheKey key3 = new XmlCacheKey("file:///carpeta/archivo.xml", "otroElemento");
        assertNotEquals(key1, key3);
        XmlCacheKey key4 = new XmlCacheKey("file:///carpeta/otro.xml", "elemento");
        assertNotEquals(key1, key4);
        assertNotEquals(key1, null);
        assertNotEquals(key1, "otro tipo");
    }

    /**
     * Test de lastReference y referenceTime.
     */
    @Test
    public void testReferencias() {
        System.out.println("xmlCacheKey referencias");
        XmlCacheKey key = new XmlCacheKey("file:///a.xml", "e");
        assertEquals(0, (int) key.getReferenceTime());
        assertNull(key.getLastReference());
        key.addReferenceTime();
        key.addReferenceTime();
        assertEquals(2, (int) key.getReferenceTime());
        Date now = new Date();
        key.setLastReference(now);
        assertEquals(now, key.getLastReference());
    }
}

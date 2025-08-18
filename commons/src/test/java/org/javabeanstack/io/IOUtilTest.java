/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2018 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*        jenciso@javabeanstack.org
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
package org.javabeanstack.io;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Jorge Enciso
 */
public class IOUtilTest {
    
    public IOUtilTest() {
    }

    /**
     * Test of isFileExist method, of class IOUtil.
     */
    @Test
    public void testIsFileExist() {
        System.out.println("isFileExist");
        String filePath = "./src/test/java/org/javabeanstack/util/prueba.xml";
        boolean expResult = true;
        boolean result = IOUtil.isFileExist(filePath);
        assertEquals(expResult, result);
    }

    /**
     * Test of isFolderExist method, of class IOUtil.
     */
    @Test
    public void testIsFolderExist() {
        System.out.println("isFolderExist");
        String folder = "./src/test/java/org/javabeanstack/util/";
        boolean expResult = true;
        boolean result = IOUtil.isFolderExist(folder);
        assertEquals(expResult, result);

        folder = "./src/test/java/org/javabeanstack/util";
        result = IOUtil.isFolderExist(folder);
        assertEquals(expResult, result);
        
        folder = "./src/test/java/org/javabeanstack/noexiste/";
        result = IOUtil.isFolderExist(folder);
        assertEquals(false, result);
    }

    /**
     * Test of addbs method, of class IOUtil.
     */
    @Test
    public void testAddbs() {
        System.out.println("addbs");
        String pathFolder = "./src/test/java/org/javabeanstack/util";
        String expResult = "./src/test/java/org/javabeanstack/util"+File.separator;
        String result = IOUtil.addbs(pathFolder);
        assertEquals(expResult, result);
    }

    /**
     * Test of getResourceAsStream method, of class IOUtil.
     */
    @Test
    public void testGetResourceAsStream() {
        System.out.println("getResourceAsStream");
        Class clazz = IOUtil.class;
        String filePath = "/images/javabeanstack_commons.png";
        InputStream result = IOUtil.getResourceAsStream(clazz, filePath);
        assertNotNull(result);
        
        filePath = "images/javabeanstack_commons.png";
        result = IOUtil.getResourceAsStream(clazz, filePath);
        assertNull(result);
    }

    /**
     * Test of getPropertiesFrom method, of class IOUtil.
     */
    @Test
    public void testGetPropertiesFrom_String() {
        System.out.println("getPropertiesFrom_string");
        String filePath = "./src/test/java/org/javabeanstack/io/ioutiltest.properties";
        Properties result = IOUtil.getPropertiesFrom(filePath);
        assertNotNull(result);
        
        filePath = "./src/test/java/org/javabeanstack/io/noexiste.properties";
        result = IOUtil.getPropertiesFrom(filePath);
        assertNull(result);
    }

    /**
     * Test of getPropertiesFrom method, of class IOUtil.
     */
    @Test
    public void testGetPropertiesFrom_File() {
        System.out.println("getPropertiesFrom_file");
        String folder = "./src/test/java/org/javabeanstack/io/"; 
        File file = new File(folder+"ioutiltest.properties");
        Properties result = IOUtil.getPropertiesFrom(file);
        assertNotNull(result);
        
        file = new File(folder+"noexiste.properties");
        result = IOUtil.getPropertiesFrom(file);
        assertNull(result);
    }

    /**
     * Test of getPropertiesFromResource method, of class IOUtil.
     */
    @Test
    public void testGetPropertiesFromResource() {
        System.out.println("getPropertiesFromResource");
        Class clazz = IOUtil.class;
        String filePath = "/test/ioutiltest.properties";
        Properties result = IOUtil.getPropertiesFromResource(clazz, filePath);
        assertNotNull(result);
        
        filePath = "test/ioutiltest.properties";
        result = IOUtil.getPropertiesFromResource(clazz, filePath);
        assertNull(result);
    }

    /**
     * Test of getPath method, of class IOUtil.
     */
    @Test
    public void testGetPath() {
        System.out.println("getPath");
        String file = "c:/src/test/java/org/javabeanstack/util/prueba.xml";
        String expResult = "src/test/java/org/javabeanstack/util/";
        String result = IOUtil.getPath(file);
        assertEquals(expResult, result);
    }

    /**
     * Test of getFullPath method, of class IOUtil.
     */
    @Test
    public void testGetFullPath() {
        System.out.println("getFullPath");
        String file = "c:/src/test/java/org/javabeanstack/util/prueba.xml";
        String expResult = "c:/src/test/java/org/javabeanstack/util/";
        String result = IOUtil.getFullPath(file);
        assertEquals(expResult, result);
    }

    /**
     * Test of getFullPathNoEndSeparator method, of class IOUtil.
     */
    @Test
    public void testGetFullPathNoEndSeparator() {
        System.out.println("getFullPathNoEndSeparator");
        String file = "c:/src/test/java/org/javabeanstack/util/prueba.xml";
        String expResult = "c:/src/test/java/org/javabeanstack/util";
        String result = IOUtil.getFullPathNoEndSeparator(file);
        assertEquals(expResult, result);
    }

    /**
     * Test of getFileBaseName method, of class IOUtil.
     */
    @Test
    public void testGetFileBaseName() {
        System.out.println("getFileBaseName");
        String file = "c:/src/test/java/org/javabeanstack/util/prueba.xml";
        String expResult = "prueba";
        String result = IOUtil.getFileBaseName(file);
        assertEquals(expResult, result);
    }

    /**
     * Test of getFileName method, of class IOUtil.
     */
    @Test
    public void testGetFileName() {
        System.out.println("getFileName");
        String file = "c:/src/test/java/org/javabeanstack/util/prueba.xml";
        String expResult = "prueba.xml";
        String result = IOUtil.getFileName(file);
        assertEquals(expResult, result);
    }

    /**
     * Test of getFileExtension method, of class IOUtil.
     */
    @Test
    public void testGetFileExtension() {
        System.out.println("getFileExtension");
        String file = "c:/src/test/java/org/javabeanstack/util/prueba.xml";
        String expResult = "xml";
        String result = IOUtil.getFileExtension(file);
        assertEquals(expResult, result);
    }
}

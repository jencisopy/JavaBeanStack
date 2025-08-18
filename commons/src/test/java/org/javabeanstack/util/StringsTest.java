/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 Jorge Enciso
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
package org.javabeanstack.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 *
 * @author Jorge Enciso
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class StringsTest {
    
    public StringsTest() {
    }

    /**
     * Test of isNullorEmpty method, of class Strings.
     */
    @Test
    public void testIsNullorEmpty() {
        System.out.println("isNullorEmpty");
        String string = "";
        Boolean expResult = true;
        Boolean result = Strings.isNullorEmpty(string);
        assertEquals(expResult, result);
        
        string = "no esta vacio";
        expResult = false;
        result = Strings.isNullorEmpty(string);
        assertEquals(expResult, result);        

        string = null;
        expResult = true;
        result = Strings.isNullorEmpty(string);
        assertEquals(expResult, result);
    }

    /**
     * Test of findString method, of class Strings.
     */
    @Test
    public void testFindString_String_String() {
        System.out.println("findString");
        String searchExpr = "aqui";
        String exprIn = "encontrar texto aqui y aqui";
        
        // Encuentra
        int expResult = 16;
        int result = Strings.findString(searchExpr, exprIn);
        assertEquals(expResult, result);
    }

    /**
     * Test of findString method, of class Strings.
     */
    @Test
    public void testFindString_3args() {
        System.out.println("findString");
        String searchExpr = "aqui";
        String exprIn = "encontrar texto aqui y aqui";        
        // No Encuentra
        int expResult = -1;
        int ocurrence = 0;
        int result = Strings.findString(searchExpr, exprIn,ocurrence);
        assertEquals(expResult, result);
        System.out.println(result);

        //Encuentra
        expResult = 16;
        ocurrence = 1;
        result = Strings.findString(searchExpr, exprIn,ocurrence);
        assertEquals(expResult, result);
        System.out.println(result);
        
        // Encuentra
        expResult = 23;
        ocurrence = 2;
        result = Strings.findString(searchExpr, exprIn,ocurrence);
        assertEquals(expResult, result);
        System.out.println(result);
        
        // No Encuentra
        expResult = -1;
        ocurrence = 3;
        result = Strings.findString(searchExpr, exprIn,ocurrence);
        assertEquals(expResult, result);
        System.out.println(result);
        assertEquals(expResult, result);
    }

    /**
     * Test of occurs method, of class Strings.
     */
    @Test
    public void testOccurs() {
        System.out.println("occurs");
        String searchExpr = "text";
        String exprIn = "contar veces que aparece texto en este texto";
        int expResult = 2;
        int result = Strings.occurs(searchExpr, exprIn);
        assertEquals(expResult, result);

        searchExpr = "xx";
        exprIn = "contar veces que aparece texto en este texto";
        expResult = 0;
        result = Strings.occurs(searchExpr, exprIn);
        assertEquals(expResult, result);
        
        searchExpr = "";
        exprIn = "contar veces que aparece texto en este texto";
        expResult = 0;
        result = Strings.occurs(searchExpr, exprIn);
        assertEquals(expResult, result);
    }

    /**
     * Test of replicate method, of class Strings.
     */
    @Test
    public void testReplicate() {
        System.out.println("replicate");
        String character = "0";
        int times = 10;
        String expResult = "0000000000";
        String result = Strings.replicate(character, times);
        assertEquals(expResult, result);
        
        character = "0";
        times = 0;
        expResult = "";
        result = Strings.replicate(character, times);
        assertEquals(expResult, result);
    }

    /**
     * Test of findLimit method, of class Strings.
     */
    @Test
    public void testFindLimit_String_String() {
        System.out.println("findLimit");
        String limit = ",";
        String expr = "busca (no busca dentro de parentesis) ',' ni comillas\",\""
                + " ,un caracter, en una cadena,"
                + " no busca dentro de parenteris (,) ni entre comillas ',' \",\" ";
        int expResult = 57;
        int result = Strings.findLimit(limit, expr);
        assertEquals(expResult, result);
    }

    /**
     * Test of findLimit method, of class Strings.
     */
    @Test
    public void testFindLimit_3args() {
        System.out.println("findLimit");
        
        int occurs = 1;
        String limit = ",";

        String expr = "busca (no busca dentro de parentesis) ',' ni comillas\",\""
                + " ,un caracter, en una cadena,"
                + " no busca dentro de parenteris (,) ni entre comillas ',' \",\" ";
        int expResult = 57;
        int result = Strings.findLimit(limit, expr, occurs);
        assertEquals(expResult, result);
        
        occurs = 2;
        expResult = 69;
        result = Strings.findLimit(limit, expr, occurs);
        assertEquals(expResult, result);
        
        occurs = 0;
        expResult = -1;
        result = Strings.findLimit(limit, expr, occurs);
        assertEquals(expResult, result);
        
        occurs = 10;
        expResult = -1;
        result = Strings.findLimit(limit, expr, occurs);
        assertEquals(expResult, result);
    }

    /**
     * Test of varReplace method, of class Strings.
     */
    @Test
    public void testVarReplace_String_String() {
        System.out.println("varReplace");

        String limit = "'";        
        String var = "reemplaza una cadena 'xx' limitada por un caracter";
        String expResult = "reemplaza una cadena '**' limitada por un caracter";
        String result = Strings.varReplace(var, limit);
        assertEquals(expResult, result);
        
        limit = "\""; 
        var = "reemplaza una cadena \"xx\" limitada por un caracter";
        expResult = "reemplaza una cadena \"**\" limitada por un caracter";
        result = Strings.varReplace(var, limit);
        assertEquals(expResult, result);

        limit = "()"; 
        var = "reemplaza una cadena (xx(yy)xx) limitada por un caracter";
        expResult = "reemplaza una cadena (********) limitada por un caracter";
        result = Strings.varReplace(var, limit);
        assertEquals(expResult, result);

        limit = "()"; 
        var = "reemplaza una cadena (xx,'yy',xx) limitada por un caracter";
        expResult = "reemplaza una cadena (**********) limitada por un caracter";
        result = Strings.varReplace(var, limit);
        assertEquals(expResult, result);
        
        limit = "[]"; 
        var = "reemplaza una cadena [xx,'yy',xx] limitada por un caracter";
        expResult = "reemplaza una cadena [**********] limitada por un caracter";
        result = Strings.varReplace(var, limit);
        assertEquals(expResult, result);
    }

    /**
     * Test of varReplace method, of class Strings.
     */
    @Test
    public void testVarReplace_3args() {
        System.out.println("varReplace");
        String replace = ".";
        String limit = "'";        
        String var = "reemplaza una cadena 'xx' limitada por un caracter";
        String expResult = "reemplaza una cadena '..' limitada por un caracter";
        String result = Strings.varReplace(var, limit,replace);
        assertEquals(expResult, result);
        
        limit = "\""; 
        var = "reemplaza una cadena \"xx\" limitada por un caracter";
        expResult = "reemplaza una cadena \"..\" limitada por un caracter";
        result = Strings.varReplace(var, limit,replace);
        assertEquals(expResult, result);

        limit = "()"; 
        var = "reemplaza una cadena (xx(yy)xx) limitada por un caracter";
        expResult = "reemplaza una cadena (........) limitada por un caracter";
        result = Strings.varReplace(var, limit, replace);
        assertEquals(expResult, result);

        limit = "()"; 
        var = "reemplaza una cadena (xx,'yy',xx) limitada por un caracter";
        expResult = "reemplaza una cadena (..........) limitada por un caracter";
        result = Strings.varReplace(var, limit,replace);
        assertEquals(expResult, result);
        
        limit = "[]"; 
        var = "reemplaza una cadena [xx,'yy',xx] limitada por un caracter";
        expResult = "reemplaza una cadena [..........] limitada por un caracter";
        result = Strings.varReplace(var, limit,replace);
        assertEquals(expResult, result);
    }

    /**
     * Test of stringToList method, of class Strings.
     */
    @Test
    public void testStringToList() {
        System.out.println("stringToList");
        String expr = "uno,dos,tres(tres,cuatro),cuatro,cinco";
        List<String> expResult = new ArrayList();
        expResult.add("uno");
        expResult.add("dos");
        expResult.add("tres(tres,cuatro)");
        expResult.add("cuatro");
        expResult.add("cinco");
        List<String> result = Strings.stringToList(expr);
        assertEquals(expResult, result);
    }

    /**
     * Test of convertToMatrix method, of class Strings.
     */
    @Test
    public void testConvertToMatrix() {
        System.out.println("convertToMatrix");
        String expr = "uno,dos,tres,cuatro,cinco";        
        String separator = ",";
        String[] expResult = {"uno","dos","tres","cuatro","cinco"};
        String[] result = Strings.convertToMatrix(expr, separator);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of convertToList method, of class Strings.
     */
    @Test
    public void testConvertToList() {
        System.out.println("convertToList");
        String expr = "uno,dos,tres,cuatro,cinco";
        String separator = ",";
        List<String> expResult = new ArrayList();
        expResult.add("uno");
        expResult.add("dos");
        expResult.add("tres");
        expResult.add("cuatro");
        expResult.add("cinco");

        List<String> result = Strings.convertToList(expr, separator);
        assertEquals(expResult, result);
    }

    /**
     * Test of textMerge method, of class Strings.
     */
    @Test
    public void testTextMerge_String_Map() {
        System.out.println("textMerge");
        
        String text = "xx{valor}xx";
        Map<String,String> params = new HashMap();
        params.put("valor", "reemplazo");

        String expResult = "xxreemplazoxx";
        String result = Strings.textMerge(text, params);
        assertEquals(expResult, result);
    }

    /**
     * Test of textMerge method, of class Strings.
     */
    @Test
    public void testTextMerge_3args() {
        System.out.println("textMerge");
        String text = "xx ${valor} xx";
        Map<String,String> params = new HashMap();
        params.put("valor", "reemplazo");
        String initPattern = "$";

        String expResult = "xx reemplazo xx";
        String result = Strings.textMerge(text, params, initPattern);
        assertEquals(expResult, result);
        
        text = "xx #{valor} xx";
        initPattern = "#";
        result = Strings.textMerge(text, params, initPattern);        
        assertEquals(expResult, result);        
    }

    /**
     * Test of textMerge method, of class Strings.
     */
    @Test
    public void testTextMerge_String_ObjectArr() {
        System.out.println("textMerge");
        String text = "xx {valor} xx {valor2}";
        String expResult = "xx reemplazo xx reemplazo2";
        String result = Strings.textMerge(text, "valor","reemplazo","valor2","reemplazo2");
        assertEquals(expResult, result);
    }

    /**
     * Test of left method, of class Strings.
     */
    @Test
    public void testLeft() {
        System.out.println("left");
        String str = "Juan Perez";
        int len = 4;
        String expResult = "Juan";
        String result = Strings.left(str, len);
        assertEquals(expResult, result);
        
        str = "Juan";
        len = 5;
        expResult = "Juan";
        result = Strings.left(str, len);
        assertEquals(expResult, result);

        str = null;
        len = 5;
        expResult = null;
        result = Strings.left(str, len);
        assertEquals(expResult, result);
    }

    /**
     * Test of right method, of class Strings.
     */
    @Test
    public void testRight() {
        System.out.println("right");
        String str = "Juan Perez";
        int len = 5;
        String expResult = "Perez";
        String result = Strings.right(str, len);
        assertEquals(expResult, result);
        
        str = "Perez";
        len = 6;
        expResult = "Perez";
        result = Strings.right(str, len);
        assertEquals(expResult, result);
        
        str = null;
        len = 6;
        expResult = null;
        result = Strings.right(str, len);
        assertEquals(expResult, result);
    }

    /**
     * Test of substring method, of class Strings.
     */
    @Test
    public void testSubstring_String_int() {
        System.out.println("substring");
        String str = "Juan Perez Benitez";
        int start = 5;
        String expResult = "Perez Benitez";
        String result = Strings.substring(str, start);
        assertEquals(expResult, result);
        
        str = "Juan Perez Benitez";
        start = 50;
        expResult = "";
        result = Strings.substring(str, start);
        assertEquals(expResult, result);
    }

    /**
     * Test of substring method, of class Strings.
     */
    @Test
    public void testSubstring_3args() {
        System.out.println("substring");
        String str = "Juan Perez Benitez";
        int start = 5;
        int end = 10;
        String expResult = "Perez";
        String result = Strings.substring(str, start, end);
        assertEquals(expResult, result);
        
        start = 5;
        end = 50;
        expResult = "Perez Benitez";
        result = Strings.substring(str, start, end);
        assertEquals(expResult, result);
        
        str = null;
        start = 5;
        end = 50;
        expResult = null;
        result = Strings.substring(str, start, end);
        assertEquals(expResult, result);
    }

    /**
     * Test of substr method, of class Strings.
     */
    @Test
    public void testSubstr_String_int() {
        System.out.println("substr");
        String str = "Juan Perez Benitez";
        int start = 5;
        String expResult = "Perez Benitez";
        String result = Strings.substr(str, start);
        assertEquals(expResult, result);
        
        start = 50;
        expResult = "";
        result = Strings.substr(str, start);
        assertEquals(expResult, result);
        
        str = null;        
        start = 50;
        expResult = null;
        result = Strings.substr(str, start);
        assertEquals(expResult, result);
    }

    /**
     * Test of substr method, of class Strings.
     */
    @Test
    public void testSubstr_3args() {
        System.out.println("substr");
        String str = "Juan Perez Benitez";
        int start = 5;
        int charactersReturned = 5;
        String expResult = "Perez";
        String result = Strings.substr(str, start, charactersReturned);
        assertEquals(expResult, result);
        
        start = 5;
        charactersReturned = 50;
        expResult = "Perez Benitez";
        result = Strings.substr(str, start, charactersReturned);
        assertEquals(expResult, result);
        
        str = null;
        expResult = null;
        result = Strings.substr(str, start, charactersReturned);
        assertEquals(expResult, result);
    }

    /**
     * Test of dateToString method, of class Strings.
     */
    @Test
    public void testDateToString() {
        System.out.println("dateToString");
        Date date = Dates.toDate("01/01/1900");
        String expResult = "19000101";
        String result = Strings.dateToString(date);
        result = result.substring(0, 8);
        assertEquals(expResult, result);
    }

    /**
     * Test of dateToString method, of class Strings.
     */
    @Test
    public void testLocalDateToString() {
        System.out.println("dateToString");
        LocalDateTime date = LocalDates.toDateTime("01/01/1900");
        String expResult = "19000101";
        String result = Strings.localDateToString(date);
        result = result.substring(0, 8);
        assertEquals(expResult, result);
    }
    
    /**
     * Test of inString method, of class Strings.
     */
    @Test
    public void testInString() {
        System.out.println("inString");
        String comodinBegin = "{[";
        String search = "texto";
        String comodinEnd = "]}";
        String expression = "buscar aqui el texto";
        boolean expResult = false;
        boolean result = Strings.inString(comodinBegin, search, comodinEnd, expression);
        assertEquals(expResult, result);
        
        expression = "buscar aqui el {texto}";
        expResult = true;
        result = Strings.inString(comodinBegin, search, comodinEnd, expression);
        assertEquals(expResult, result);
        
        expression = "buscar aqui el [texto]";
        expResult = true;
        result = Strings.inString(comodinBegin, search, comodinEnd, expression);
        assertEquals(expResult, result);
        
        expression = "buscar aqui el [TEXTO]";
        expResult = true;
        result = Strings.inString(comodinBegin, search, comodinEnd, expression);
        assertEquals(expResult, result);
        
        expression = "buscar aqui el [text]";
        expResult = false;
        result = Strings.inString(comodinBegin, search, comodinEnd, expression);
        assertEquals(expResult, result);
    }

    /**
     * Test of encode64 method, of class Strings.
     */
    @Test
    public void testEncode64() {
        System.out.println("encode64");
        String message = "este es un texto para codificar a base 64";
        String expResult = "ZXN0ZSBlcyB1biB0ZXh0byBwYXJhIGNvZGlmaWNhciBhIGJhc2UgNjQ=";
        String result = Strings.encode64(message);
        assertEquals(expResult, result);
    }

    /**
     * Test of decode64 method, of class Strings.
     */
    @Test
    public void testDecode64() {
        System.out.println("decode64");
        String messageEncode = "ZXN0ZSBlcyB1biB0ZXh0byBwYXJhIGNvZGlmaWNhciBhIGJhc2UgNjQ=";
        String expResult = "este es un texto para codificar a base 64";
        String result = Strings.decode64(messageEncode);
        assertEquals(expResult, result);
    }


    /**
     * Test of fileToString method, of class Strings.
     */
    @Test
    public void testFileToString_File() {
        System.out.println("fileToString");
        String path = "./src/test/java/org/javabeanstack/util/prueba.xml";
        File file = new File(path);
        String expResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        String result = Strings.fileToString(file);
        assertEquals(expResult, result.substring(0, 38));
    }
    
    /**
     * Test of fileToString method, of class Strings.
     */
    @Test
    public void testFileToString_String() {
        System.out.println("fileToString");
        String path = "./src/test/java/org/javabeanstack/util/prueba.xml";
        String expResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        String result = Strings.fileToString(path);
        assertEquals(expResult, result.substring(0, 38));
    }

    /**
     * Test of fileToString method, of class Strings.
     */
    @Test
    public void testFileToString_String_String() {
        System.out.println("fileToString");
        String filePath = "./src/test/java/org/javabeanstack/util/prueba.xml";
        String charSet = "UTF-8";
        String expResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        String result = Strings.fileToString(filePath, charSet);
        assertEquals(expResult, result.substring(0, 38));
    }

    /**
     * Test of streamToString method, of class Strings.
     * @throws java.lang.Exception
     */
    @Test
    public void testStreamToString_InputStream() throws Exception {
        System.out.println("streamToString");
        String path = "./src/test/java/org/javabeanstack/util/prueba.xml";
        File file = new File(path);
        InputStream input = new FileInputStream(file);
        String expResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        String result = Strings.streamToString(input);
        assertEquals(expResult, result.substring(0, 38));
    }

    /**
     * Test of streamToString method, of class Strings.
     * @throws java.lang.Exception
     */
    @Test
    public void testStreamToString_InputStream_String() throws Exception {
        System.out.println("streamToString");
        String path = "./src/test/java/org/javabeanstack/util/prueba.xml";
        File file = new File(path);
        String charSet = "UTF-8";
        InputStream input = new FileInputStream(file);
        String expResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        String result = Strings.streamToString(input, charSet);
        assertEquals(expResult, result.substring(0, 38));
    }

    /**
     * Test of streamToString method, of class Strings.
     * @throws java.lang.Exception
     */
    @Test
    public void testStreamToString_InputStream_Charset() throws Exception {
        System.out.println("streamToString");
        String path = "./src/test/java/org/javabeanstack/util/prueba.xml";
        File file = new File(path);
        InputStream input = new FileInputStream(file);
        Charset charSet = Charset.forName("UTF-8");
        String expResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        String result = Strings.streamToString(input, charSet);
        assertEquals(expResult, result.substring(0, 38));
    }

    /**
     * Test of getXmlFileCharSet method, of class Strings.
     */
    @Test
    public void testGetXmlFileCharSet_String() {
        System.out.println("getXmlFileCharSet");
        String path = "./src/test/java/org/javabeanstack/util/prueba.xml";
        String text = Strings.fileToString(path);
        String expResult = "UTF-8";
        String result = Strings.getXmlFileCharSet(text);
        assertEquals(expResult, result);
    }

    /**
     * Test of getXmlFileCharSet method, of class Strings.
     */
    @Test
    public void testGetXmlFileCharSet_File() {
        System.out.println("getXmlFileCharSet");
        String path = "./src/test/java/org/javabeanstack/util/prueba.xml";
        File file = new File(path);
        String expResult = "UTF-8";
        String result = Strings.getXmlFileCharSet(file);
        assertEquals(expResult, result);
    }

    /**
     * Test of capitalize method, of class Strings.
     */
    @Test
    public void testCapitalize() {
        System.out.println("capitalize");
        String text = "juan";
        String expResult = "Juan";
        String result = Strings.capitalize(text);
        assertEquals(expResult, result);
    }
}

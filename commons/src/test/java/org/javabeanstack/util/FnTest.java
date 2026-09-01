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
package org.javabeanstack.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.javabeanstack.crypto.CipherUtil;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

/**
 *
 * @author Jorge Enciso
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class FnTest {
    
    public FnTest() {
    }


    /**
     * Test of inList method, of class Fn.
     */
    @Test
    public void testInList_String_StringArr() {
        System.out.println("inList");
        String obj = "existe";
        String[] list = {"EXISTE","exist","exist2","existe"};
        boolean expResult = true;
        boolean result = Fn.inList(obj, list);
        assertEquals(expResult, result);
        
        result = Fn.inList(obj,"EXISTE","exist","exist2","existe");
        assertEquals(expResult, result);
    }


    /**
     * Test of inList method, of class Fn.
     */
    @Test
    public void testInArrayInteger() {
        System.out.println("inArrayInteger");
        Integer obj = 1;
        int[] list = {1,2,4,5,1};
        boolean expResult = true;
        boolean result = Fn.inArrayInteger(obj, list);
        assertEquals(expResult, result);
    }

    /**
     * Test of findInMatrix method, of class Fn.
     */
    @Test
    public void testFindInMatrix_ObjectArr_Object() {
        System.out.println("findInMatrix");
        Object[] matrix = {3,2,3,4,1};
        Object search = 1;
        Integer expResult = 4; // Posición en la matriz
        Integer result = Fn.findInMatrix(matrix, search);
        assertEquals(expResult, result);
    }

    /**
     * Test of findInMatrix method, of class Fn.
     */
    @Test
    public void testFindInMatrix_3args() {
        System.out.println("findInMatrix");
        String[] matrix = {"EXISTE","existe"};
        String search = "existe";
        Boolean caseSensitive = false;
        Integer expResult = 0;
        Integer result = Fn.findInMatrix(matrix, search, caseSensitive);
        assertEquals(expResult, result);

        caseSensitive = true;
        expResult = 1;
        result = Fn.findInMatrix(matrix, search, caseSensitive);
        assertEquals(expResult, result);
    }

    /**
     * Test of toLogical method, of class Fn.
     */
    @Test
    public void testToLogical() {
        System.out.println("toLogical");
        Object value = "1";
        Boolean expResult = true;
        Boolean result = Fn.toLogical(value);
        assertEquals(expResult, result);
        
        value = 1;
        expResult = true;
        result = Fn.toLogical(value);
        assertEquals(expResult, result);

        value = "0";
        expResult = false;
        result = Fn.toLogical(value);
        assertEquals(expResult, result);

        value = 0;
        expResult = false;
        result = Fn.toLogical(value);
        assertEquals(expResult, result);
    }

    /**
     * Test of iif method, of class Fn.
     */
    @Test
    public void testIif() {
        System.out.println("iif");
        boolean condition = (1 == 1);
        Object value1 = 1;
        Object value2 = 2;
        Object expResult = 1;
        Object result = Fn.iif(condition, value1, value2);
        assertEquals(expResult, result);
    }

    /**
     * Test of nvl method, of class Fn.
     */
    @Test
    public void testNvl() {
        System.out.println("nvl");
        Object value = null;
        Object alternateValue = "es nulo";
        Object expResult = "es nulo";
        Object result = Fn.nvl(value, alternateValue);
        assertEquals(expResult, result);
    }

    /**
     * Test of bytesToHex method, of class Fn.
     * @throws java.io.UnsupportedEncodingException
     * @throws java.security.NoSuchAlgorithmException
     */
    @Test
    public void testBytesToHex() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        System.out.println("bytesToHex");
        String expResult = "5ad6f23da25b3a54cd5ae716c401732d";        
        
        String msg = "abcdefghijklmnñopqrstuvwxyzáéíóú";        
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] bytes = digest.digest(msg.getBytes("UTF-8"));
        String result = Fn.bytesToHex(bytes);
        
        assertEquals(expResult, result);
    }

    /**
     * Test of hexToByte method, of class Fn.
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.io.UnsupportedEncodingException
     */
    @Test
    public void testHexToByte() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        System.out.println("hexToByte");
        String hexText = "5ad6f23da25b3a54cd5ae716c401732d";
        
        String msg = "abcdefghijklmnñopqrstuvwxyzáéíóú";        
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] expResult = digest.digest(msg.getBytes("UTF-8"));
        
        byte[] result = Fn.hexToByte(hexText);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of base64ToBytes method, of class Fn.
     * @throws java.lang.Exception
     */
    @Test
    public void testBase64ToBytes() throws Exception {
        System.out.println("base64ToBytes");
        String clearText = "abcdefghijklmnñopqrstuvwxyzáéíóú";
        String key = "123456á";
        
        SecretKeySpec keyspec = new SecretKeySpec(key.getBytes(), CipherUtil.BLOWFISH);
        Cipher cipher = Cipher.getInstance(CipherUtil.BLOWFISH);
        cipher.init(Cipher.ENCRYPT_MODE, keyspec);
        byte[] expResult = cipher.doFinal(clearText.getBytes());
        
        String encrypted64 = CipherUtil.encryptBlowfishToBase64(clearText, key);
        byte[] result = Fn.base64ToBytes(encrypted64);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of bytesToBase64 method, of class Fn.
     * @throws java.lang.Exception
     */
    @Test
    public void testBytesToBase64() throws Exception {
        System.out.println("bytesToBase64");
        String clearText = "abcdefghijklmnñopqrstuvwxyzáéíóú";
        String key = "123456á";
        
        SecretKeySpec keyspec = new SecretKeySpec(key.getBytes(), CipherUtil.BLOWFISH);
        Cipher cipher = Cipher.getInstance(CipherUtil.BLOWFISH);
        cipher.init(Cipher.ENCRYPT_MODE, keyspec);
        byte[] bytes = cipher.doFinal(clearText.getBytes());
        
        String expResult = CipherUtil.encryptBlowfishToBase64(clearText, key);
        String result = Fn.bytesToBase64(bytes);
        
        assertEquals(expResult, result);
    }
    
    /**
     * Test of bytesToBase64 method, of class Fn.
     * @throws java.lang.Exception
     */
    @Test
    public void testQueryParams() throws Exception {
        System.out.println("queryParams");
        Map<String, Object> expResult = new HashMap();
        expResult.put("idempresa", 1L);
        expResult.put("idmoneda", 2L);
        Map<String, Object> result = Fn.queryParams("idempresa",1L,"idmoneda",2L);
        assertEquals(expResult, result);

        expResult = new HashMap();
        expResult.put("idempresa", 1L);
        result = Fn.queryParams("idempresa",1L,"idmoneda");
        assertEquals(expResult, result);
    }

    /**
     * Lista vacía, nula o de puras comas y espacios: sin restricción
     * declarada no hay nada que restringir.
     */
    @Test
    public void testIpMatchListaVacia() {
        System.out.println("ipMatch - lista vacía");
        assertTrue(Fn.ipMatch("192.168.1.5", null));
        assertTrue(Fn.ipMatch("192.168.1.5", ""));
        assertTrue(Fn.ipMatch("192.168.1.5", "   "));
        assertTrue(Fn.ipMatch("192.168.1.5", " , , "));
        //Sin restricción, hasta un origen desconocido pasa.
        assertTrue(Fn.ipMatch(null, ""));
        assertTrue(Fn.ipMatch(null, null));
    }

    /**
     * Los dos comodines totales, que son la forma explícita de "sin
     * restricción" y por eso admiten también un origen desconocido.
     */
    @Test
    public void testIpMatchComodinTotal() {
        System.out.println("ipMatch - comodín total");
        assertTrue(Fn.ipMatch("192.168.1.5", "0.0.0.0"));
        assertTrue(Fn.ipMatch("192.168.1.5", "*"));
        assertTrue(Fn.ipMatch("10.20.30.40", " 0.0.0.0 "));
        assertTrue(Fn.ipMatch("192.168.1.5", "10.0.0.5,0.0.0.0"));
        assertTrue(Fn.ipMatch(null, "0.0.0.0"));
        assertTrue(Fn.ipMatch("0:0:0:0:0:0:0:1", "*"));
    }

    /**
     * Coincidencia exacta.
     */
    @Test
    public void testIpMatchExacta() {
        System.out.println("ipMatch - exacta");
        assertTrue(Fn.ipMatch("192.168.1.5", "192.168.1.5"));
        assertTrue(Fn.ipMatch("192.168.1.5", " 192.168.1.5 "));
        assertFalse(Fn.ipMatch("192.168.1.6", "192.168.1.5"));
        assertTrue(Fn.ipMatch("127.0.0.1", "127.0.0.1"));
        //Un cero a la izquierda de un octeto significativo NO es comodín.
        assertFalse(Fn.ipMatch("10.99.99.5", "10.0.0.5"));
    }

    /**
     * Comodín por octetos, que es el caso de uso que se pidió:
     * {@code 192.168.*} tiene que aceptar toda la red.
     */
    @Test
    public void testIpMatchComodinPorOctetos() {
        System.out.println("ipMatch - comodín por octetos");
        assertTrue(Fn.ipMatch("192.168.1.5", "192.168.*"));
        assertTrue(Fn.ipMatch("192.168.240.9", "192.168.*"));
        assertFalse(Fn.ipMatch("192.169.1.5", "192.168.*"));
        assertFalse(Fn.ipMatch("10.168.1.5", "192.168.*"));
        //Los ceros a la derecha se comportan igual que el asterisco.
        assertTrue(Fn.ipMatch("192.168.1.5", "192.168.0.0"));
        assertFalse(Fn.ipMatch("192.169.1.5", "192.168.0.0"));
        assertTrue(Fn.ipMatch("192.168.1.5", "192.*.*.*"));
        assertTrue(Fn.ipMatch("192.168.1.5", "192.168.1.*"));
        assertFalse(Fn.ipMatch("192.168.2.5", "192.168.1.*"));
    }

    /**
     * Varias entradas separadas por coma: alcanza con que coincida una.
     */
    @Test
    public void testIpMatchVariasEntradas() {
        System.out.println("ipMatch - varias entradas");
        String lista = "10.0.0.5, 192.168.*, 172.16.4.7";
        assertTrue(Fn.ipMatch("10.0.0.5", lista));
        assertTrue(Fn.ipMatch("192.168.99.1", lista));
        assertTrue(Fn.ipMatch("172.16.4.7", lista));
        assertFalse(Fn.ipMatch("172.16.4.8", lista));
        assertFalse(Fn.ipMatch("8.8.8.8", lista));
        //Entradas vacías intercaladas no cambian el resultado.
        assertTrue(Fn.ipMatch("10.0.0.5", "10.0.0.5,,"));
        assertFalse(Fn.ipMatch("8.8.8.8", "10.0.0.5,,"));
    }

    /**
     * Lo que no se puede evaluar, no se autoriza: entradas mal formadas,
     * origen desconocido y direcciones de otra familia.
     *
     * <p>El caso del bucle local IPv6 contra un patrón IPv4 es el que hacía
     * caer con {@code ArrayIndexOutOfBoundsException} a la versión anterior de
     * esta lógica, que vivía duplicada dentro del filtro de peticiones.</p>
     */
    @Test
    public void testIpMatchNoLanzaExcepcion() {
        System.out.println("ipMatch - entradas inválidas");
        //Patrón con más octetos que la dirección.
        assertFalse(Fn.ipMatch("0:0:0:0:0:0:0:1", "192.168.1.5"));
        assertFalse(Fn.ipMatch("::1", "127.0.0.1"));
        assertFalse(Fn.ipMatch("192.168", "192.168.1.5"));
        //Origen desconocido con una restricción declarada.
        assertFalse(Fn.ipMatch(null, "192.168.*"));
        assertFalse(Fn.ipMatch("", "192.168.*"));
        assertFalse(Fn.ipMatch("   ", "192.168.*"));
        //Basura.
        assertFalse(Fn.ipMatch("192.168.1.5", "no-es-una-ip"));
        assertFalse(Fn.ipMatch("no-es-una-ip", "192.168.*"));
        assertFalse(Fn.ipMatch("192.168.1.5", "..."));
    }

    /**
     * La variante que recibe la lista ya partida, que es como la usa el filtro
     * de peticiones.
     */
    @Test
    public void testIpMatchAny() {
        System.out.println("ipMatchAny");
        assertTrue(Fn.ipMatchAny("192.168.1.5", new String[]{"0.0.0.0"}));
        assertTrue(Fn.ipMatchAny("192.168.1.5", new String[]{"10.0.0.1", "192.168.*"}));
        assertFalse(Fn.ipMatchAny("8.8.8.8", new String[]{"10.0.0.1", "192.168.*"}));
        //Arreglo nulo, vacío o en blanco equivale a la lista vacía.
        assertTrue(Fn.ipMatchAny("8.8.8.8", (String[]) null));
        assertTrue(Fn.ipMatchAny("8.8.8.8", new String[]{}));
        assertTrue(Fn.ipMatchAny("8.8.8.8", new String[]{"", "  ", null}));
    }

    /**
     * La lista de denegados: acá la lista vacía significa lo contrario que en
     * la de permitidos —no se deniega a nadie—, y por eso es un método aparte.
     */
    @Test
    public void testIpListed() {
        System.out.println("ipListed");
        assertTrue(Fn.ipListed("192.168.1.5", new String[]{"192.168.*"}));
        assertTrue(Fn.ipListed("192.168.1.5", new String[]{"10.0.0.1", "192.168.1.5"}));
        assertFalse(Fn.ipListed("8.8.8.8", new String[]{"192.168.*"}));
        //Nada declarado, nadie denegado. Es lo contrario de ipMatchAny.
        assertFalse(Fn.ipListed("8.8.8.8", (String[]) null));
        assertFalse(Fn.ipListed("8.8.8.8", new String[]{}));
        assertFalse(Fn.ipListed("8.8.8.8", new String[]{"", "  ", null}));
        assertTrue(Fn.ipMatchAny("8.8.8.8", new String[]{"", "  ", null}));
    }

    /**
     * Un patrón que queda todo en comodines coincide con cualquier dirección.
     *
     * <p>Es el único punto donde esta lógica <b>no</b> reproduce a la que vivía
     * duplicada dentro del filtro de peticiones, y la divergencia es
     * deliberada: allá estas entradas eran inertes —no permitían ni denegaban a
     * nadie— porque la coincidencia se marcaba solo al comparar literalmente el
     * primer octeto. Verificado corriendo las dos implementaciones lado a lado:
     * los cinco casos de abajo daban falso y ahora dan verdadero.</p>
     */
    @Test
    public void testIpMatchTodoComodin() {
        System.out.println("ipMatch - patrón todo comodines");
        assertTrue(Fn.ipMatch("192.168.1.5", "*.*.*.*"));
        assertTrue(Fn.ipMatch("192.168.1.5", "0.*.*.*"));
        assertTrue(Fn.ipMatch("192.168.1.5", "*.168.1.5"));
        assertTrue(Fn.ipMatch("192.168.1.5", "0.0"));
        assertTrue(Fn.ipMatch("192.168.1.5", "0.0.0.0.0"));
        //Y en la lista de denegados significan lo mismo: nombran a todos.
        assertTrue(Fn.ipListed("8.8.8.8", new String[]{"*.*.*.*"}));
        //Lo que NO cambia: un octeto significativo sigue mandando.
        assertFalse(Fn.ipMatch("192.168.1.5", "*.169.1.5"));
        assertFalse(Fn.ipMatch("192.168.1.5", "0.0.0.6"));
    }
}

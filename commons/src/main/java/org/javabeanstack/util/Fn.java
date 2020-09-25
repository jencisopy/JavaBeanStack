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

import java.text.DecimalFormat;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


/**
 * Funciones utiles
 * 
 * @author Jorge Enciso
 */
public class Fn {
    private Fn(){
    }
    
    /**
     * Verifica si un valor "obj" se encuentra en una lista de variables
     * @param obj   valor buscado
     * @param list  lista de valores.
     * @return  verdadero si encuentra y falso si no.
     */
    public static boolean inList(String obj, String... list) {
        for (Object e : list) {
            if (obj.equals(e)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Verifica si un valor "obj" se encuentra en una lista de variables
     * @param obj   valor buscado
     * @param list  lista de valores.
     * @return  verdadero si encuentra y falso si no.
     */
    public static boolean inList(Integer obj, int... list) {
        for (Object e : list) {
            if (obj.equals(e)) {
                return true;
            }
        }
        return false;
    }
    
    public static <T> T iif(Boolean condition, T value1, T value2) {
        if (condition) {
            return value1;
        }
        return value2;
    }
    

    /**
     * Busca un objeto en una matriz y si encuentra  devuelve el nro. de elemento
     * @param matrix
     * @param search
     * @return nro. de elemento si encuentra el objeto
     */
    public static Integer findInMatrix(Object[] matrix, Object search) {
        int posicion = -1;
        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i] == search) {
                posicion = i;
                break;
            }
        }
        return posicion;
    }

    /**
     * Busca un objeto en una matriz y si encuentra  devuelve el nro. de elemento
     * @param matrix
     * @param search
     * @param caseSensitive
     * @return nro. de elemento si encuentra el objeto
     */
    public static Integer findInMatrix(String[] matrix, String search, Boolean caseSensitive) {
        int posicion = -1;
        for (int i = 0; i < matrix.length; i++) {
            if (caseSensitive) {
                if (matrix[i].trim().equals(search)) {
                    posicion = i;
                    break;
                }
            } else {
                if (matrix[i].trim().equalsIgnoreCase(search)) {
                    posicion = i;
                    break;
                }
            }
        }
        return posicion;
    }

    /**
     * Convierte a verdadero o falso (0 falso, 1 verdadero)
     * @param value puede ser una variable numerica o una cadena.
     * @return verdadero o falso.
     */
    public static Boolean toLogical(Object value) {
        if (value == null) {
            return false;
        } else if (value instanceof Boolean) {
            return (Boolean) value;
        } else if ("1".equals(value.toString())) {
            return true;
        } else if ("true".equalsIgnoreCase(value.toString())) {
            return true;
        } else if ("0".equals(value.toString())) {
            return false;
        } else if ("false".equalsIgnoreCase(value.toString())) {            
            return false;
        } else if ("".equals(value.toString())) {
            return false;
        }
        return false;
    }



    /**
     * Devuelve un valor alternativo (alternateValue) si el valor dado (value)
     * es nulo.
     * @param <T>
     * @param value  valor que devuelve si no es nulo
     * @param alternateValue valor que devuelve si "value" es nulo
     * @return valor (value si no es nulo) valor alternativo (alternateValue si value es nulo)
     */
    public static <T> T nvl(T value, T alternateValue) {
        if (value == null) {
            return alternateValue;
        }
        return value;
    }

    /**
     * Convierte de un array tipo byte a un string hexadecimal
     * @param bytes valor a traducir.
     * @return array tipo byte a un string hexadecimal
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte byt : bytes) {
            result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }
    
    /**
     * Convierte un texto hexadecimal a un array tipo bytes
     * @param hexText texto hexadecimal
     * @return texto hexadecimal a un array tipo bytes
     */
    public static byte[] hexToByte(String hexText) {
        int len = hexText.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexText.charAt(i), 16) << 4)
                                 + Character.digit(hexText.charAt(i+1), 16));
        }
        return data;
    }    
    
    /**
     * Convierte un datos codificado en base 64 a otro equivalente de tipo byte
     * @param encrypted64
     * @return 
     */
    public static byte[] base64ToBytes(String encrypted64){
        return Base64.getDecoder().decode(encrypted64);
    }

    /**
     * Códifica un dato tipo byte[] a base 64
     * @param text datos tipo byte[]
     * @return tipo byte[] a base 64
     */
    public static String bytesToBase64(byte[] text){
        return Base64.getEncoder().encodeToString(text);
    }
    
    /**
     * Códifica un dato tipo byte[] a base 64 tipo url
     * @param text datos tipo byte[]
     * @return tipo byte[] a base 64
     */
    public static String bytesToBase64Url(byte[] text){
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text);
    }
    
    /**
     * 
     * @param params
     * @return 
     */
    public static Map<String, Object> queryParams(Object... params){
        Map<String, Object> result = new HashMap();
        int c = 0;
        Object key = null;
        for (Object param:params){
            c++;
            if (c%2 != 0){
                key = param;
            }
            else if (key != null && !key.toString().isEmpty()){
                result.put(key.toString(), param);                
            }
        }
        return result;
    }
    
    /**
     * Convierte de numero a string utilizando una mascara para el efecto
     * @param value valor numerico
     * @param mask mascara (ver en la documentación de DecimalFormat)
     * @return valor convertido a string.
     */
    public static String numberToString(Object value, String mask){
        if (value == null){
            return "";
        }
        String result;
        if (!Fn.nvl(mask, "").isEmpty()){
            DecimalFormat df = new DecimalFormat(mask);
            result = df.format(value);
        }
        else{
            DecimalFormat df = new DecimalFormat("###");
            result = df.format(value);
        }
        return result;
    }
}

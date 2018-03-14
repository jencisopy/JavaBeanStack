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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


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
    
    public static <T> T iif(boolean condition, T value1, T value2) {
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
        } else if ("0".equals(value.toString())) {
            return false;
        }
        return false;
    }


    /**
     * Devuelve el resultante md5 de un texto dado.
     * @param msg texto dado.
     * @return md5 del texto dado.
     */
    public static String getMD5(String msg) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            //log.debug("Error al obtener hash MD5. " + ex.getMessage());
        }
        if (md != null) {
            md.update(msg.getBytes());
            byte byteData[] = md.digest();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
            return (sb.toString()).toUpperCase();
        }
        return null;
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
}

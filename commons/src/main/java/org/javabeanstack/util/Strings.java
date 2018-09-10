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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * Funciones que facilitan el manejo de las variables String.
 * @author Jorge Andres Enciso Rios
 */
public class Strings {
    private Strings(){
    }   
    
    /**
     * Devuelve verdadero si el valor que se pasa como parametro es nulo o vacio
     * @param string valor
     * @return verdadero si "string" es nulo o vacio.
     */
    public static Boolean isNullorEmpty(String string) {
        return string == null || string.isEmpty();
    }

    /**
     * Busca una cadena "searchExpr" en otra cadena "exprIn" y devuelve
     * la posición si existe o -1 si no.
     * @param searchExpr    cadena buscada
     * @param exprIn  cadena dentro del cual se busca la expresión.
     * @return nro de posición dentro de exprIn
     */
    public static int findString(String searchExpr, String exprIn) {
        return findString(searchExpr, exprIn, 1);
    }

    /**
     * Busca una cadena "searchExpr" en otra cadena "exprIn" y devuelve
     * la posición si existe o -1 si no.
     * @param searchExpr    cadena buscada
     * @param exprIn  cadena dentro del cual se busca la expresión.
     * @param nOccurrence   nro de ocurrencia.
     * @return nro de posición dentro de exprIn
     */
    public static int findString(String searchExpr, String exprIn, int nOccurrence) {
        int pos = -1;
        for (int i = 1; i <= nOccurrence; i++) {
            pos = exprIn.indexOf(searchExpr, pos + 1);
            if (pos < 0){
                break;
            }
        }
        return pos;
    }

    /**
     * Devuelve la cantidad de ocurrencias de "seachExpr" dentro de "exprIn"
     * @param searchExpr  cadena buscada.
     * @param exprIn cadena dentro de la cual es buscada "searchExpr"
     * @return cantidad de ocurrencias si los hubiere.
     */
    public static int occurs(String searchExpr, String exprIn) {
        return StringUtils.countMatches(exprIn, searchExpr);
    }

    /**
     * Replica "character" tantas veces "times" 
     * @param character caracter a repetir 
     * @param times  cantidad de veces
     * @return repite un caracter tantas veces según parámetro.
     */
    public static String replicate(String character, int times) {
        return StringUtils.repeat(character, times);
    }

    /**
     * Busca un caracter limitador dentro de una expresión, 
     * se descartará la busqueda dentro de comillas y dentro de parentesis.
     * 
     * @param limit caracter limitador
     * @param expr cadena dentro del cual se buscará el caracter limitador.
     * @return posición dentro de "expr" si lo encuentra -1 si no encuentra.
     */
    public static int findLimit(String limit, String expr) {
        return findLimit(limit, expr, 1);
    }

    /**
     * Busca un caracter limitador dentro de una expresión, 
     * se descartará la busqueda dentro de comillas y dentro de parentesis.
     * 
     * @param limit caracter limitador
     * @param expr cadena dentro del cual se buscará el caracter limitador.
     * @param occurs nro de ocurrencia dentro de la cadena.
     * @return posición dentro de "expr" si lo encuentra -1 si no encuentra.
     */
    public static int findLimit(String limit, String expr, int occurs) {
        if (limit == null){
            return -1;
        }
        expr = varReplace(expr, "'");
        expr = varReplace(expr, "\"");
        expr = varReplace(expr, "()");
        // Buscar limitador de la cadena
        return findString(limit, expr, occurs);
    }

    /**
     * Reemplazara con * cualquier valor dentro de "var" que se encuentre
     * comprendido entre caracteres "limit"
     * 
     * @param var 
     * @param limit lista de caracteres limitadores.
     * @return cadena procesada.
     */
    public static String varReplace(String var, String limit) {
        return varReplace(var, limit, "*");
    }

    /**
     * Reemplazara con un caracter "replace" cualquier valor dentro de "var" 
     * que se encuentre comprendido entre caracteres "limit"
     * 
     * @param var 
     * @param limit lista de caracteres limitadores.
     * @param replace caracter con que se reemplazara las posiciones entre
     * los caracteres "limit"
     * @return cadena procesada.
     */
    public static String varReplace(String var, String limit, String replace) {
        String result = var;
        // Si esta vacio la variable       
        if (Strings.isNullorEmpty(var) || Strings.isNullorEmpty(limit)) {
            return var;
        }
        String limit1, limit2;

        if (limit.length() == 2) {
            limit1 = StringUtils.left(limit, 1);
            limit2 = StringUtils.right(limit, 1);
        } else {
            limit1 = limit.substring(0, 1);
            limit2 = limit.substring(0, 1);
        }
        int c = 0, p1 = 0, p2 = 0, c2 = 0;
        while (true) {
            c = c + 1;
            p1 = findString(limit1, result, c);
            if (limit1.equals(limit2)) {
                c = c + 1;
            }
            p2 = findString(limit2, result, c);
            // Verificar que el caracter no sea un limitador interno
            if (!limit1.equals(limit2)) {
                c2 = c;
                while (p1 >= 0) {
                    int innerLimit1 = occurs(limit1, result.substring(p1, p2));
                    int innerLimit2 = occurs(limit2, result.substring(p1, p2 + 1));
                    if (innerLimit2 == 0) {
                        break;
                    }
                    if (innerLimit1 - innerLimit2 != 0) {
                        c2 = c2 + innerLimit1 - innerLimit2;
                        p2 = findString(limit2, result, c2);
                    } else {
                        break;
                    }
                }
            }
            if (p1 == -1 || p2 == -1) {
                break;
            }
            result = StringUtils.left(result, p1 + 1) + StringUtils.repeat(replace, p2 - p1 - 1) + result.substring(p2);
        }
        return result;
    }

    /**
     * Convierte una cadena separada por coma a un objeto List.
     * No se considera las comas dentro de comillas ni dentro de parentesis.
     * @param expr valor a convertir
     * @return objeto List.
     */
    public static List<String> stringToList(String expr) {
        List<String> lista = new ArrayList<>();
        int k = 0;
        int ini = 0;
        while (true) {
            k = k + 1;
            int fin = findLimit(",", expr, k);
            if (fin < 0) {
                fin = expr.length();
            }
            lista.add(expr.substring(ini, fin).trim());
            ini = fin + 1;
            if (fin == expr.length()) {
                break;
            }
        }
        return lista;
    }

    /**
     * Convierte una cadena a una matriz
     * @param expr   cadena
     * @param separator separador dentro de expr ejemplo "," que se utilizará
     * para identificar los elementos que formarán la matriz.
     * @return una cadena a una matriz.
     */
    public static String[] convertToMatrix(String expr, String separator) {
        String[] exprList = expr.split("\\" + separator);
        for (int i = 0; i < exprList.length; i++) {
            exprList[i] = exprList[i].trim();
        }
        return exprList;
    }

    /**
     * Convierte una cadena a un objeto List.
     * @param expr   cadena
     * @param separator separador dentro de expr ejemplo "," que se utilizará
     * para identificar los elementos que formarán la matriz.
     * @return una cadena a una matriz.
     */
    public static List<String> convertToList(String expr, String separator) {
        List<String> lista = new ArrayList<>();
        String[] exprList = expr.split("\\" + separator);
        for (String exprList1 : exprList) {
            lista.add(exprList1.trim());
        }
        return lista;
    }

    /**
     * Fusiona una cadena "text" con valores de los parámetros. Los valores
     * a fusionarse se encierran entre {} 
     * @param text cadena a fusionarse con los valores de los parámetros.
     * @param params parámetros con los valores a fusionar. 
     * @return cadena procesada.
     */
    public static String textMerge(String text, Map<String, String> params) {
        String result = text;
        String regexSearch;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            regexSearch = "(?i)\\{" + entry.getKey() + "\\}";
            result = result.replaceAll(regexSearch, entry.getValue());
        }
        return result;
    }

    /**
     * Fusiona una cadena "text" con valores de los parámetros. Los valores
     * a fusionarse se encierran entre "iniPattern"{} 
     * @param text cadena a fusionarse con los valores de los parámetros.
     * @param params parámetros con los valores a fusionar. 
     * @param iniPattern 
     * @return cadena procesada.
     */
    public static String textMerge(String text, Map<String, String> params, String iniPattern) {
        String result = text;
        String regexSearch;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String caracter;
            for (int i = 0; i < iniPattern.length(); i++) {
                caracter = "";
                if (!" ".equals(substr(iniPattern, i, 1))) {
                    caracter = "\\" + substr(iniPattern, i, 1);
                }
                regexSearch = "(?i)" + caracter + "\\{" + entry.getKey() + "\\}";
                result = result.replaceAll(regexSearch, entry.getValue());
            }
        }
        return result;
    }
    
    /**
     * Fusiona una cadena "text" con valores de los parámetros. Los valores
     * a fusionarse se encierran entre "iniPattern"{} 
     * @param text cadena a fusionarse con los valores de los parámetros.
     * @param keyParamList lista en pares (parametro, valor)
     * @return cadena procesada.
     */
    public static String textMerge(String text, Object... keyParamList) {
        Map<String, String> params = new HashMap<>();
        int i = 0;
        String expr1, expr2;
        if (keyParamList != null && keyParamList.length >= 1) {
            while (true) {
                expr1 = keyParamList[i].toString();
                if (i + 1 < keyParamList.length) {
                    expr2 = keyParamList[i + 1].toString();
                } else {
                    expr2 = null;
                }
                params.put(expr1, expr2);
                i = i + 2;
                if (i >= keyParamList.length) {
                    break;
                }
            }
        }
        return Strings.textMerge(text, params);
    }
    

    /**
     * Toma los n caracteres del lado izquierdo de un string
     * @param str variable string
     * @param len cantidad de caracteres
     * @return cantidad de caracteres del lado izquierdo de una variable alfanumérica.
     */
    public static String left(String str, int len) {
        return StringUtils.left(str, len);
    }

    /**
     * Toma los n caracteres del lado derecho de un string
     * @param str variable string
     * @param len cantidad de caracteres
     * @return cantidad de caracteres del lado derecho de una variable alfanumérica.
     */
    public static String right(String str, int len) {
        return StringUtils.right(str, len);
    }

    /**
     * Extrae carecteres de un string
     * @param str variable string
     * @param start posición a partir de la cual extraera los caracteres hasta el final de la variable.
     * @return caracteres extraidos a partir de una posición hasta el final de la variable.
     */
    public static String substring(String str, int start) {
        return StringUtils.substring(str, start);
    }

    /**
     * Extrae caracteres de un string
     * @param str variable string 
     * @param start posición a partir de la cual extraera los caracteres de la variable.
     * @param end posición final hasta donde extraera los caracteres.
     * @return caracteres extraidos de la variable alfanumérica.
     */
    public static String substring(String str, int start, int end) {
        return StringUtils.substring(str, start, end);
    }

    /**
     * Extrae caracteres de un string
     * @param str variable string
     * @param start posición a partir de la cual extraera los caracteres hasta el final de la variable.
     * @return caracteres extraidos a partir de una posición hasta el final de la variable.
     */
    public static String substr(String str, int start) {
        return StringUtils.substring(str, start);
    }

    /**
     * Extrae caracteres de un string
     * @param str variable string 
     * @param start posición a partir de la cual extraera los caracteres de la variable.
     * @param charactersReturned cantidad de caracteres a retornar.
     * @return caracteres extraidos de la variable alfanumérica.
     */
    public static String substr(String str, int start, int charactersReturned) {
        return StringUtils.substring(str, start, start + charactersReturned);
    }

    /**
     * Convierte una variable date a string.
     * @param date variable date
     * @return string con formato yyyyMMddhhmmss
     */
    public static String dateToString(Date date) {
        SimpleDateFormat formater = new SimpleDateFormat("yyyyMMddhhmmss");
        return formater.format(date);
    }

    /**
     * Busca una cadena dentro de otra encerrada entre caracteres "comodinBegin"
     * y "comodinEnd"
     * @param comodinBegin lista de caracteres iniciales ej. ",'(;"
     * @param search  valor a buscar.
     * @param comodinEnd  lista de caracteres finales. ej. ",');"
     * @param expression expresión dentro de la cual se buscará la cadena "search"
     * @return verdadero o falso si encuentra o no la cadena.
     */
    public static boolean inString(String comodinBegin, String search, String comodinEnd, String expression) {
        search = search.toUpperCase();
        expression = expression.toUpperCase();
        comodinBegin = comodinBegin.toUpperCase();
        comodinEnd = comodinEnd.toUpperCase();

        if (findString(search, expression) < 0) {
            return false;
        }

        boolean found = false;
        for (int i = 0; i < comodinBegin.length(); i++) {
            found = (findString(substr(comodinBegin, i, 1)
                    + search, right(expression, search.length() + 1)) >= 0);

            if (found) {
                return true;
            }

            for (int k = 0; k < comodinEnd.length(); k++) {
                found = (findString(substr(comodinBegin, i, 1)
                        + search
                        + substr(comodinEnd, k, 1), expression) >= 0)
                        || ((search + substr(comodinEnd, k, 1)).equals(left(expression, search.length() + 1)));

                if (found) {
                    return true;
                }
            }
        }
        return found;
    }

    /**
     * Convierte el primer carácter de la variable a mayúscula.
     * @param text variable alfanumérica.
     * @return variable con el primer carácter convertido a mayúscula.
     */
    public static String capitalize(String text){
        if (text == null){
            return null;
        }
        return StringUtils.capitalize(text);
    }
    
    /**
     * Codifica una cadena a base 64
     * @param message cadena
     * @return cadena codificada.
     */
    public static String encode64(String message){
        if (message == null){
            return null;
        }
        Base64.Encoder encoder = Base64.getEncoder();
        byte[] encodeContent = encoder.encode(message.getBytes());
        return new String(encodeContent);
    }

    /**
     * Decodifica una cadena base 64 a base 10 
     * @param messageEncode cadena
     * @return cadena decodificada.
     */
    public static String decode64(String messageEncode){
        if (messageEncode == null){
            return null;
        }
        Base64.Decoder decoder = Base64.getDecoder();   
        byte[] decodedContent = decoder.decode(messageEncode);        
        return new String(decodedContent);
    }

    /**
     * Convierte un archivo a una variable String.
     * @param file objeto file.
     * @return variable tipo cadena.
     */
    public static String fileToString(File file){
        return fileToString(file.getAbsolutePath());
    }
    

    /**
     * Convierte un archivo a una variable String.
     * @param filePath  ubicación del archivo.
     * @return variable tipo cadena.
     */
    public static String fileToString(String filePath){
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
            }
        }
        catch (Exception ex) {
            Logger.getLogger(Strings.class).error(ex.getMessage());                        
        }
        return sb.toString();        
    }
    
    /**
     * Convierte un archivo a una variable String.
     * @param filePath ubicación del archivo
     * @param charSet  tabla de caracteres ej. UTF-8
     * @return variable tipo cadena.
     */
    public static String fileToString(String filePath, String charSet){
        File file = new File(filePath);
        String str=null;
        try {
            if (isNullorEmpty(charSet)){
                str = fileToString(file);
                if (file.getName().toLowerCase().endsWith(".xml")){
                    charSet = getXmlFileCharSet(str);
                    str = FileUtils.readFileToString(file, charSet);                
                }
            }
            else{
                str = FileUtils.readFileToString(file, charSet);                
            }
        } catch (IOException ex) {
            Logger.getLogger(Strings.class).error(ex.getMessage());                                    
        }
        return str;
    }

    /**
     * Convierte un objeto stream a una cadena de carácteres
     * @param input  objeto stream
     * @return cadena de caracteres.
     * @throws IOException 
     */
    public static String streamToString(InputStream input) throws IOException{
        if (input == null){
            return null;
        }
        return IOUtils.toString(input);
    }    

    /**
     * Convierte un objeto stream a una cadena de carácteres
     * @param input  objeto stream
     * @param charSet tabla de caracteres en la que viene codificada el stream ej. UTF-8
     * @return cadena de caracteres.
     * @throws IOException 
     */
    public static String streamToString(InputStream input, String charSet) throws IOException{
        if (input == null){
            return null;
        }
        return IOUtils.toString(input,charSet);
    }    

    /**
     * Convierte un objeto stream a una cadena de carácteres
     * @param input  objeto stream
     * @param charSet tabla de caracteres en la que viene codificada el stream ej. UTF-8
     * @return cadena de caracteres.
     * @throws IOException 
     */
    public static String streamToString(InputStream input, Charset charSet) throws IOException{
        if (input == null){
            return null;
        }
        return IOUtils.toString(input,charSet);
    }    

    /**
     * Devuelve el charSet de un texto xml, para lo cual utiliza el 
     * valor de la expresion encoding que se encuentra en la linea 1 del texto.
     * 
     * @param text texto con formato xml.
     * @return charSet ej. UTF-8
     */    
    public static String getXmlFileCharSet(String text){
        if (isNullorEmpty(text)){
            return "";
        }
        String result="";
        int pos1 = text.indexOf("encoding=");
        if (pos1 > 0 ){
            pos1 += 10;
            int pos2 = text.indexOf('\"', pos1);
            result = text.substring(pos1,pos2);
        }
        return result;
    }


    /**
     * Devuelve el charSet de un archivo xml, para lo cual utiliza el 
     * valor de la expresion encoding que se encuentra en la linea 1 del archivo.
     * 
     * @param file objeto archivo.
     * @return charSet ej. UTF-8
     */    
    public static String getXmlFileCharSet(File file){
        if (!file.exists() || !file.canRead()){
            return "";
        }
        String text = fileToString(file);
        if (isNullorEmpty(text)){
            return "";
        }
        String result="";
        int pos1 = text.indexOf("encoding=");
        if (pos1 > 0 ){
            pos1 += 10;
            int pos2 = text.indexOf('\"', pos1);
            result = text.substring(pos1,pos2);
        }
        return result;
    }
    
    
    public static String leftPad(String str, int size, String padStr){
        return StringUtils.leftPad(str, size, padStr);
    }
    
    public static String rightPad(String str, int size, String padStr){
        return StringUtils.rightPad(str, size, padStr);
    }
}

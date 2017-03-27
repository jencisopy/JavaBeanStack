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
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author Jorge Enciso
 */
public class Strings {
    private static final Logger LOGGER = Logger.getLogger(Strings.class);    
    
    public static Boolean isNullorEmpty(String string) {
        return string == null || string.isEmpty();
    }

    public static int findString(String searchExpr, String exprSearched) {
        return findString(searchExpr, exprSearched, 1);
    }

    public static int findString(String searchExpr, String exprSearched, int nOccurrence) {
        int pos = -1;
        for (int i = 1; i <= nOccurrence; i++) {
            pos = exprSearched.indexOf(searchExpr, pos + 1);
        }
        return pos;
    }

    public static int occurs(String searchExpr, String exprSearched) {
        return StringUtils.countMatches(exprSearched, searchExpr);
    }

    public static String replicate(String character, int times) {
        return StringUtils.repeat(character, times);
    }

    public static int findLimit(String limit, String expr) {
        return findLimit(limit, expr, 1);
    }

    public static int findLimit(String limit, String expr, int occurs) {
        expr = varReplace(expr, "'");
        expr = varReplace(expr, "()");
        // Buscar limitador de la cadena
        return findString(limit, expr, occurs);
    }

    public static String varReplace(String var, String limit) {
        return varReplace(var, limit, "*");
    }

    public static String varReplace(String var, String limit, String replace) {
        String result = var;
        // Si esta vacio la variable       
        if (Strings.isNullorEmpty(var)) {
            return var;
        }
        String limit1, limit2;

        if (limit.length() == 2) {
            limit1 = StringUtils.left(limit, 1);
            limit2 = StringUtils.right(limit, 1);
        } else {
            limit1 = limit.substring(0, 1);
            limit2 = limit.substring(1, 1);
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

    public static String[] convertToMatrix(String expr, String separador) {
        String[] exprList = expr.split("\\" + separador);
        for (int i = 0; i < exprList.length; i++) {
            exprList[i] = exprList[i].trim();
        }
        return exprList;
    }

    public static List<String> convertToList(String expr, String separador) {
        List<String> lista = new ArrayList<>();
        String[] exprList = expr.split("\\" + separador);
        for (int i = 0; i < exprList.length; i++) {
            lista.add(exprList[i].trim());
        }
        return lista;
    }

    public static String textMerge(String text, Map<String, String> params) {
        String result = text;
        String regexSearch;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            regexSearch = "(?i)\\{" + entry.getKey() + "\\}";
            result = result.replaceAll(regexSearch, entry.getValue());
        }
        return result;
    }

    public static String textMerge(String text, Map<String, String> params,String iniPattern) {
        String result = text;
        String regexSearch;        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String caracter;
            for (int i = 0;i<iniPattern.length();i++){
                caracter = "";
                if (!" ".equals(substr(iniPattern,i,1))){
                    caracter = "\\"+substr(iniPattern,i,1);                    
                }
                regexSearch = "(?i)"+caracter+"\\{" + entry.getKey() + "\\}";
                result = result.replaceAll(regexSearch, entry.getValue());
            }
        }
        return result;
    }
    
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
    

    public static String left(String str, int len) {
        return StringUtils.left(str, len);
    }

    public static String right(String str, int len) {
        return StringUtils.right(str, len);
    }

    public static String substring(String str, int start) {
        return StringUtils.substring(str, start);
    }

    public static String substring(String str, int start, int end) {
        return StringUtils.substring(str, start, end);
    }

    public static String substr(String str, int start) {
        return StringUtils.substring(str, start);
    }

    public static String substr(String str, int start, int charactersReturned) {
        return StringUtils.substring(str, start, start + charactersReturned);
    }

    public static String dateToString(Date date) {
        SimpleDateFormat formater = new SimpleDateFormat("yyyyMMddhhmmss");
        return formater.format(date);
    }

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
                        + search, right(expression, search.length()+1)) >= 0);

            if (found) {
                return true;
            }

            for (int k = 0; k < comodinBegin.length(); k++) {
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
    
    public static String encode64(String message){
        if (message == null){
            return null;
        }
        Base64.Encoder encoder = Base64.getEncoder();
        byte[] encodeContent = encoder.encode(message.getBytes());
        return new String(encodeContent);
    }
    
    public static String decode64(String messageEncode){
        if (messageEncode == null){
            return null;
        }
        Base64.Decoder decoder = Base64.getDecoder();   
        byte[] decodedContent = decoder.decode(messageEncode);        
        return new String(decodedContent);
    }
    
    public static String fileToString(String filePath){
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                if (line != null) {
                    sb.append(System.lineSeparator());
                }
            }
        }
        catch (Exception ex) {
            Logger.getLogger(Strings.class).error(ex.getMessage());                        
        }
        return sb.toString();        
    }
}

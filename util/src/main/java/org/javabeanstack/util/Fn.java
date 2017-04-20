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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import org.apache.log4j.Logger;


/**
 *
 * @author Jorge Enciso
 */
public class Fn {
    public static Object toObject(Object source, Object target) throws Exception {
        if (target instanceof String) {
            return String.valueOf(source);
        } else if (target instanceof Double) {
            return Double.valueOf(source.toString());
        } else if (target instanceof Float) {
            return Float.valueOf(source.toString());
        } else if (target instanceof Long) {
            return Long.valueOf(source.toString());
        } else if (target instanceof Integer) {
            return Integer.valueOf(source.toString());
        } else if (target instanceof Short) {
            return Short.valueOf(source.toString());
        } else if (target instanceof Boolean) {
            return Boolean.valueOf(source.toString());
        }
        throw new Exception("No fue posible convertir el objeto source al objeto target");
    }

    /**
     * Convierte una cadena a una fecha
     * @param dateString
     * @return 
     */
    public static Date toDate(String dateString) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        return toDate(dateString, formatter);
    }

    public static Date toDate(String dateString, String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return toDate(dateString, formatter);
    }
    
    public static Date toDate(String dateString, SimpleDateFormat formatter) {
        Date date = null;
        try {
            date = formatter.parse(dateString);
        } catch (ParseException ex) {
            Logger.getLogger(Fn.class).error(ex.getMessage());
        }
        return date;
    }

    public static String toString(Date date, SimpleDateFormat formater) {
        String result = formater.format(date);
        return result;
    }

    public static String toString(Date date, String format) {
        SimpleDateFormat formater = new SimpleDateFormat(format);
        String result = formater.format(date);
        return result;
    }

    public static boolean inList(String obj, String... list) {
        for (Object e : list) {
            if (obj.equals(e)) {
                return true;
            }
        }
        return false;
    }


    public static boolean inList(Integer obj, int... list) {
        for (Object e : list) {
            if (obj.equals(e)) {
                return true;
            }
        }
        return false;
    }

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

    public static Date now() {
        return Calendar.getInstance().getTime();
    }

    public static Date today() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);        
        calendar.set(Calendar.MILLISECOND,0);        

        Date today = calendar.getTime();
        return today;
    }
    
    public static <T> T iif(boolean condition, T value1, T value2) {
        if (condition) {
            return value1;
        }
        return value2;
    }
    
    public static <T> T nvl(T value, T alternateValue) {
        if (value == null) {
            return alternateValue;
        }
        return value;
    }

    public static boolean isFileExist(String filePath){
        File f = new File(filePath);
        return f.exists() && !f.isDirectory();
    }
    
    public static boolean isFolderExist(String folder){
        File f = new File(folder);
        return f.exists() && f.isDirectory();
    }
    
    public static String addbs(String path){
        if (Strings.isNullorEmpty(path)){
            return path;
        }
        if (path.endsWith("/") || path.endsWith("\\")){
            return path;
        }
        return path.trim()+File.separator;
    }
}

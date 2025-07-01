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
package org.javabeanstack.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.javabeanstack.util.Strings;

/**
 * Funciones que facilitan el manejo de archivos, inputStream entre otros.
 *
 * @author Jorge Enciso
 */
public class IOUtil {

    private IOUtil() {
    }

    /**
     * Determina la existencia o no de un archivo
     *
     * @param filePath path del archivo
     * @return verdadero si existe, falso si no
     */
    public static boolean isFileExist(String filePath) {
        if (Strings.isNullorEmpty(filePath)) {
            return false;
        }
        File f = new File(filePath);
        return f.exists() && !f.isDirectory();
    }

    /**
     * Determina si una carpeta existe.
     *
     * @param folder path de la carpeta.
     * @return verdadero si existe, falso si no.
     */
    public static boolean isFolderExist(String folder) {
        if (Strings.isNullorEmpty(folder)) {
            return false;
        }
        File f = new File(folder);
        return f.exists() && f.isDirectory();
    }

    /**
     * Agrega el separador de directorio al final de la expresión ejemplo
     * /path1/path2 convierta a /path1/path2/ si ya tiene el separador al final,
     * devuelve el mismo valor
     *
     * @param pathFolder path de la carpeta.
     * @return path de la carpeta con el separador al final.
     */
    public static String addbs(String pathFolder) {
        if (Strings.isNullorEmpty(pathFolder)) {
            return pathFolder;
        }
        if (pathFolder.endsWith("/") || pathFolder.endsWith("\\")) {
            return pathFolder;
        }
        if (pathFolder.contains("/")) {
            return pathFolder.trim() + "/";
        } else if (pathFolder.contains("\\")) {
            return pathFolder.trim() + "\\";
        }
        return pathFolder.trim() + File.separator;
    }

    /**
     * Devuelve un archivo que se encuentra en la carpeta resource dentro del
     * proyecto donde se encuentra "clazz" la clase proporcionada como
     * parámetro.
     *
     * @param clazz clase para ubicar el proyecto en donde se buscara el archivo
     * solicitado.
     * @param filePath ubicación del archivo dentro de la carpeta resource
     * @return un archivo en formato inputStream.
     */
    public static InputStream getResourceAsStream(Class clazz, String filePath) {
        return clazz.getResourceAsStream(filePath);
    }

    /**
     * Devuelve un archivo properties como objeto.
     *
     * @param filePath path del archivo.
     * @return objeto properties.
     */
    public static Properties getPropertiesFrom(String filePath) {
        // load a properties file
        Properties properties = new Properties();
        if (!isFileExist(filePath)) {
            return null;
        }
        try (InputStream input = new FileInputStream(filePath)) {
            properties.load(input);
            return properties;
        } catch (Exception ex) {
            Logger.getLogger(IOUtil.class).error(ex.getMessage());
        }
        return null;
    }

    /**
     * Devuelve un archivo properties como objeto.
     *
     * @param file objeto file.
     * @return objeto properties.
     */
    public static Properties getPropertiesFrom(File file) {
        // load a properties file
        Properties properties = new Properties();
        if (file == null || !file.isFile() || !file.canRead()) {
            return null;
        }
        try (InputStream input = new FileInputStream(file)) {
            properties.load(input);
            return properties;
        } catch (IOException ex) {
            Logger.getLogger(IOUtil.class).error(ex.getMessage());
        }
        return null;
    }

    /**
     * Devuelve un archivo que se encuentra en la carpeta resource dentro del
     * proyecto donde se encuentra "clazz" la clase proporcionada como
     * parámetro.
     *
     * @param clazz clase para ubicar el proyecto en donde se buscara el archivo
     * solicitado.
     * @param filePath ubicación del archivo dentro de la carpeta resource
     * @return un archivo en formato properties.
     */
    public static Properties getPropertiesFromResource(Class clazz, String filePath) {
        Properties properties = new Properties();
        try (InputStream input = getResourceAsStream(clazz, filePath)) {
            properties.load(input);
            return properties;
        } catch (Exception ex) {
            Logger.getLogger(IOUtil.class).error(ex.getMessage());
        }
        return null;
    }

    /**
     * Devuelve el path de un archivo ejemplo
     * c:/carpeta1/subcarpeta1/archivo.txt retorna /carpeta1/subcarpeta1/
     *
     * @param file nombre y path del archivo
     * @return el path del archivo
     */
    public static String getPath(String file) {
        return FilenameUtils.getPath(file);
    }

    /**
     * Devuelve el path de un archivo ejemplo
     * c:/carpeta1/subcarpeta1/archivo.txt retorna c:/carpeta1/subcarpeta1/
     *
     * @param file nombre y path del archivo
     * @return el path del archivo
     */
    public static String getFullPath(String file) {
        return FilenameUtils.getFullPath(file);
    }

    /**
     * Devuelve el path de un archivo ejemplo
     * c:/carpeta1/subcarpeta1/archivo.txt retorna /carpeta1/subcarpeta1
     *
     * @param file nombre y path del archivo
     * @return el path del archivo
     */
    public static String getFullPathNoEndSeparator(String file) {
        return FilenameUtils.getFullPathNoEndSeparator(file);
    }

    /**
     * Devuelve el nombre del archivo sin la extensión
     *
     * @param file nombre archivo
     * @return el nombre del archivo sin extensión
     */
    public static String getFileBaseName(String file) {
        return FilenameUtils.getBaseName(file);
    }

    /**
     * Devuelve el nombre del archivo
     *
     * @param file nombre archivo
     * @return el nombre del archivo sin path
     */
    public static String getFileName(String file) {
        return FilenameUtils.getName(file);
    }

    /**
     * Devuelve la extensión del archivo
     *
     * @param file nombre archivo
     * @return extensión del archivo
     */
    public static String getFileExtension(String file) {
        return FilenameUtils.getExtension(file);
    }

    /**
     * Graba una variable tipo bytes a un archivo en el filesystem.
     *
     * @param byteArray variable a grabar
     * @param filePath nombre del archivo con el path incluido donde se guardara
     * los datos.
     * @return verdadero si tuvo exito y falso si no.
     */
    public static boolean writeBytesToFile(byte[] byteArray, String filePath) {
        boolean result = true;
        try {
            FileUtils.writeByteArrayToFile(new File(filePath), byteArray);
        } catch (IOException ex) {
            Logger.getLogger(IOUtil.class).error(ex.getMessage());
            result = false;
        }
        return result;
    }
    
    public static void writeStringToFile(File file, String data){
        try {
            FileUtils.writeStringToFile(file, data, Charset.defaultCharset());            
        } catch (IOException ex) {
            Logger.getLogger(IOUtil.class).error(ex.getMessage());
        }
    }
}

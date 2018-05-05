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
import java.util.Properties;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.javabeanstack.util.Strings;

/**
 * Funciones que facilitan el manejo de archivos, inputStream entre otros.
 * 
 * @author Jorge Enciso
 */
public class IOUtil {
    private IOUtil(){
    }
    
    /**
     * Determina la existencia o no de un archivo
     *
     * @param filePath path del archivo
     * @return verdadero si existe, falso si no
     */
    public static boolean isFileExist(String filePath) {
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
        return pathFolder.trim() + File.separator;
    }

    /**
     * Devuelve un archivo que se encuentra en la carpeta resource dentro del
     * proyecto donde se encuentra "clazz" la clase proporcionada como parámetro.
     *
     * @param clazz clase para ubicar el proyecto en donde se buscara el archivo
     * solicitado.
     * @param filePath ubicación del archivo dentro de la carpeta resource
     * @return un archivo en formato inputStream.
     */
    public static InputStream getResourceAsStream(Class clazz, String filePath){
        InputStream input = clazz.getResourceAsStream(filePath);
        return input;
    }

    /**
     * Devuelve un archivo properties como objeto.
     * @param filePath  path del archivo.
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
        } catch (IOException ex) {
            Logger.getLogger(IOUtil.class).error(ex.getMessage());
        }
        return null;
    }

    /**
     * Devuelve un archivo properties como objeto.
     * @param file  objeto file.
     * @return objeto properties.
     */
    public static Properties getPropertiesFrom(File file) {
        // load a properties file
        Properties properties = new Properties();
        if (file == null || !file.isFile() || file.canRead()) {
            return null;
        }
        try (InputStream input = new FileInputStream(file)){
            properties.load(input);
            return properties;
        } catch (IOException ex) {
            Logger.getLogger(IOUtil.class).error(ex.getMessage());
        }
        return null;
    }

    /**
     * Devuelve un archivo que se encuentra en la carpeta resource dentro del
     * proyecto donde se encuentra "clazz" la clase proporcionada como parámetro.
     *
     * @param clazz clase para ubicar el proyecto en donde se buscara el archivo
     * solicitado.
     * @param filePath ubicación del archivo dentro de la carpeta resource
     * @return un archivo en formato properties.
     */
    public static Properties getPropertiesFromResource(Class clazz, String filePath) {
        Properties properties = new Properties();
        try (InputStream input = getResourceAsStream(clazz, filePath)){
            properties.load(input);
            return properties;
        } catch (IOException ex) {
            Logger.getLogger(IOUtil.class).error(ex.getMessage());
        }
        return null;
    }
    
    public static String getPath(String file){
        return FilenameUtils.getPath(file);
    }

    public static String getFullPath(String file){
        return FilenameUtils.getFullPath(file);
    }
    
    public static String getFullPathNoEndSeparator(String file){
        return FilenameUtils.getFullPathNoEndSeparator(file);
    }
    
    public static String getFileBaseName(String file){
        return FilenameUtils.getBaseName(file);
    }

    public static String getFileName(String file){
        return FilenameUtils.getName(file);
    }
    
    public static String getFileExtension(String file){
        return FilenameUtils.getExtension(file);
    }
}

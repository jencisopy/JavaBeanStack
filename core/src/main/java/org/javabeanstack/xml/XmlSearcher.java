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
package org.javabeanstack.xml; 

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map; 
import java.util.TreeMap;
import org.javabeanstack.io.IOUtil;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.Strings;
import org.javabeanstack.util.Dates;
import static org.javabeanstack.io.IOUtil.getResourceAsStream;
import static org.javabeanstack.util.Strings.fileToString;
import static org.javabeanstack.util.Strings.isNullorEmpty;
import static org.javabeanstack.util.Strings.left;
import static org.javabeanstack.util.Strings.substr;

/**
 * Clase encargada de buscar un texto xml a partir de una path de un archivo dado.
 * @author Jorge Enciso
 * @param <V> tipo de objeto DOM (org.jdom2.Document, org.w3c.dom.Document)
 */
public class XmlSearcher<V> implements IXmlSearcher<V> {
    
    private final Map<String, Object> params = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    private Map<IXmlCacheKey, IXmlCache<V>> cache = new HashMap();
    private boolean useCache=true;

    @Override
    public void addParam(String key, Object value) {
        params.put(key, value);
    }

    @Override
    public Object getParam(String key) {
        return params.get(key);
    }

    @Override
    public Map<String, Object> getParams() {
        return params;
    }

    /**
     * Esta variable determina si el cache va a ser utilizado o no.
     *
     * @return valor indicando si el cache va a ser utilizado.
     */
    @Override
    public boolean getUseCache() {
        return useCache;
    }

    /**
     * Asigna el valor a useCache que indica si el cache va a ser utilizado o
     * no.
     *
     * @param useCache true se utiliza false no.
     */
    @Override
    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }
    
    /**
     * Si esta disponible la busqueda y un tipo de path determinado.
     * @param pathType tipo de path (file, object, http)
     * @return verdadero si esta disponible, falso si no
     */
    @Override
    public boolean isSearchAvailable(String pathType) {
        return Fn.inList(pathType.toUpperCase(), "FILE", "FILE:");
    }

    /**
     * Busca el texto xml en un archivo, tabla u otra ubicación del objeto.
     * @param context contexto IXmlDom de este objeto
     * @param xmlPath ubicación del archivo
     * @return el texto xml.
     */
    @Override
    public String search(IXmlDom context, String xmlPath) {
        String readFromJAR = Fn.nvl((String)context.getConfigParam().get("readfromjar"),"");
        String encoding = Fn.nvl((String)context.getConfigParam().get("encoding"),"");
        xmlPath = xmlPath.trim();
        String pathType = getPathType(xmlPath);
        //Si no esta definido el pathType leer configuración del contexto
        if (isNullorEmpty(pathType)) {
            pathType = Fn.nvl((String)context.getConfigParam().get("pathtype"), "").toLowerCase();
        } 
        xmlPath = getJustPath(xmlPath);
        String devolver = "";
        if (Fn.inList(pathType, "file", "file:")) {
            // Ver si no puede ubicar el archivo agregarle el path
            if (!xmlPath.contains("/") && !IOUtil.isFileExist(xmlPath)) {
                String path = IOUtil.addbs(Fn.nvl((String)context.getConfigParam().get("path"), ""));
                xmlPath = path + xmlPath;
            }
            if (IOUtil.isFileExist(xmlPath)){
                devolver = fileToString(xmlPath, encoding);
            }
            else if ("YES".equals(readFromJAR)){
                try {
                    InputStream input = getResourceAsStream(context.getClass(), xmlPath);
                    devolver = Strings.streamToString(input, encoding);
                } catch (IOException ex) {
                    //
                }
            }
        }
        return devolver;
    }
    
    /**
     * Devuelve map con documentos procesados, de tal manera a reutilizarse de
     * ser necesario en algun proceso.
     *
     * @return objeto cache.
     */
    @Override
    public Map<IXmlCacheKey, IXmlCache<V>> getCache() {
        return cache;
    }

    /**
     * Asigna un objeto cache al componente.
     *
     * @param cache map con documentos DOM procesados.
     */
    @Override
    public void setCache(Map<IXmlCacheKey, IXmlCache<V>> cache) {
        this.cache = cache; 
    }
    
    /**
     * Su función es devolver un objeto DOM almacenado previamente.
     *
     * @param documentPath identificador del registro.
     * @return objeto DOM.
     */
    @Override
    public IXmlCache<V> getFromCache(String documentPath) {
        return getFromCache(documentPath,"");
    }

    /**
     * Su función es devolver un objeto DOM almacenado previamente en cache.
     *
     * @param documentPath identificador del path del documento
     * @param elementPath identificador del elemento
     * @return objeto DOM.
     */
    @Override
    public IXmlCache<V> getFromCache(String documentPath, String elementPath) {
        if (isNullorEmpty(documentPath) || !useCache) {
            return null;
        }
        documentPath = documentPath.trim().toLowerCase();
        elementPath = elementPath.trim();
        IXmlCacheKey key = new XmlCacheKey(documentPath, elementPath);
        
        if (cache.get(key) == null) {
            return null;
        }
        if (!isValidCache(documentPath, elementPath)){
            cache.remove(key);
            return null;
        }
        for (Map.Entry<IXmlCacheKey, IXmlCache<V>> e : cache.entrySet()) {
            if (key.equals(e.getKey())){
                e.getKey().setLastReference(new Date());
                e.getKey().addReferenceTime();
                break;
            }
        }        
        return cache.get(key);
    }
    
    /**
     * Agrega un objeto DOM a un cache para utilizarlo en un futuro y no tener
     * que volver a procesarlo.
     *
     * @param context
     * @param documentPath clave (normalmente la ubicación del archivo)
     * @param document objeto DOM.
     */
    @Override
    public void addToCache(IXmlDom context, String documentPath, V document) {
        addToCache(context, documentPath, "", document, false);
    }    
    
    /**
     * Agrega un objeto DOM a un cache para utilizarlo en un futuro y no tener
     * que volver a procesarlo.
     *
     * @param context
     * @param documentPath clave (normalmente la ubicación del archivo)
     * @param document objeto DOM.
     */
    @Override
    public void addToCache(IXmlDom context, String documentPath, 
                        String elementPath, V document, boolean compiled) {
        if (!isNullorEmpty(documentPath) && document != null) {
            documentPath = documentPath.trim().toLowerCase();
            elementPath = (elementPath == null) ? "" : elementPath.trim();
            IXmlCacheKey key = new XmlCacheKey(documentPath, elementPath);
            if (cache.containsKey(key)){
                return; 
            }
            IXmlCache<V> cacheObj;
            // Almacenar como texto si existe llaves ( {reemplazar} )
            if (context.isAllowChangeAttrWithParam() && context.getXmlTextRaw(document).indexOf('{') > 0) {
                cacheObj = new XmlCache(context.getXmlTextRaw(document));
            } else {
                cacheObj = new XmlCache(document);
            }
            cacheObj.setProcessTime(Dates.now());
            cacheObj.setCompiled(compiled);
            cache.put(key, cacheObj);
        }
    }
    
    /**
     * Determina si un elemento del cache es válido.
     * @param documentPath identificador del elemento cacheado.
     * @return verdadero es válido, falso no lo es.
     */
    @Override
    public Boolean isValidCache(String documentPath){
        return isValidCache(documentPath,"");
    }
    
    /**
     * Determina si un elemento del cache es válido.
     * @param documentPath identificador del documento cacheado.
     * @return verdadero es válido, falso no lo es.
     */
    @Override
    public Boolean isValidCache(String documentPath, String elementPath){
        documentPath = documentPath.trim();
        elementPath = elementPath.trim();
        IXmlCacheKey key = new XmlCacheKey(documentPath, elementPath);
        
        IXmlCache cacheElement = cache.get(key);
        if (cacheElement == null){
            return false;
        }
        File file = new File(getJustPath(documentPath));
        Date dateModified = new Date(file.lastModified());
        return !dateModified.after(cacheElement.getProcessTime());
    }    

    /**
     * Devuelve tipo de path contenida en xmlPath (ejemplo file://archivo.xml,
     * el pathtype es "file"
     * @param xmlPath ubicación del objeto.
     * @return tipo de path. (file, object, http)
     */
    public static String getPathType(String xmlPath){
        xmlPath = xmlPath.trim();
        int pos1 = xmlPath.indexOf("://");
        if (pos1 < 0) {
            return "";
        }
        String pathType = left(xmlPath, pos1);
        return pathType.toLowerCase().trim();
    }

    /**
     * Devuelve solo el camino de un archivo
     * @param path dirección del archivo completo.
     * @return solo path (camino de las carpetas) 
     * ej. /carpeta/subcarpeta/archivo devuelve /carpeta/subcarpeta
     * 
     */
    public static String getJustPath(String path){
        String pathType = getPathType(path);
        // Extraer el path type del inicio del path
        if (!isNullorEmpty(pathType) && path.startsWith(pathType)){
            path = substr(path,pathType.length()).trim();            
        }
        // Extraer el ":" del comienzo
        if (path.startsWith("://")){
            path = substr(path,3);
        }
        return path.trim();
    }

    /**
     * Determina si un archivo a través de pasar el camino existo o o
     * @param xmlPath path del archivo.
     * @return si existe un archivo.
     */
    @Override
    public boolean exist(String xmlPath) {
        xmlPath = xmlPath.trim();
        String pathType = getPathType(xmlPath);
        xmlPath = getJustPath(xmlPath);
        if (Fn.inList(pathType, "file", "file:")) {
            return !(!xmlPath.contains("/") && !IOUtil.isFileExist(xmlPath));
        }
        return false;
    }
}

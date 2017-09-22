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

import java.io.Serializable;
import java.util.Map;

/**
 * Clase encargada de buscar un texto xml a partir de una path de un archivo dado.
 * @author Jorge Enciso
 * @param <V> tipo de objeto DOM (org.jdom2.Document, org.w3c.dom.Document)
 */
public interface IXmlSearcher<V> extends Serializable{
    public static final String FILEPATH  = "file";
    public static final String APPXMLPATH = "obj";
    public static final String OBJECTPATH = "obj";
    public static final String URLPATH = "http";    
    /**
     * Devuelve map con documentos procesados, de tal manera a reutilizarse de
     * ser necesario en algun proceso.
     *
     * @return objeto cache.
     */
    Map<IXmlCacheKey, IXmlCache<V>> getCache();
    /**
     * Su función es devolver un objeto DOM almacenado previamente en el cache.
     *
     * @param documentPath identificador del registro.
     * @return objeto DOM.
     */
    IXmlCache<V> getFromCache(String documentPath);

    /**
     * Su función es devolver un objeto DOM almacenado previamente en el cache.
     *
     * @param documentPath identificador del registro.
     * @param elementPath identificador del nodo o elemento.
     * @return objeto DOM.
     */
    IXmlCache<V> getFromCache(String documentPath, String elementPath);
    
    /**
     * Agrega un objeto DOM a un cache para utilizarlo en un futuro y no tener
     * que volver a procesarlo.
     *
     * @param context contexto IXmlDom en la cual forma parte
     * @param xmlPath clave (normalmente la ubicación del archivo)
     * @param document objeto DOM.
     */
    void addToCache(IXmlDom context, String xmlPath, V document);
    /**
     * Agrega un objeto DOM a un cache para utilizarlo en un futuro y no tener
     * que volver a procesarlo.
     *
     * @param context contexto IXmlDom en la cual forma parte
     * @param documentPath clave (normalmente la ubicación del archivo)
     * @param elementPath identificador del elemento del documento DOM.
     * @param document objeto DOM.
     * @param compiled si el documento fue procesado con sus clases derivadas
     */
    void addToCache(IXmlDom context, String documentPath, String elementPath, V document,boolean compiled);
    
    /**
     * Determina si un elemento del cache es válido.
     * @param key identificador del elemento cacheado.
     * @return verdadero es válido, falso no lo es.
     */    
    Boolean isValidCache(String key);    
    /**
     * Determina si un elemento del cache es válido.
     * @param documentPath identificador del documento cacheado.
     * @param elementPath identificador del elemento del documento.
     * @return verdadero es válido, falso no lo es.
     */    
    Boolean isValidCache(String documentPath, String elementPath);    
    
    /**
     * Asigna un objeto cache al componente.
     *
     * @param cache map con documentos DOM procesados.
     */
    void setCache(Map<IXmlCacheKey, IXmlCache<V>> cache); 
    
    void addParam(String key, Object value);
    Object getParam(String key);
    Map<String, Object> getParams();

    /**
     * Esta variable determina si el cache va a ser utilizado o no.
     *
     * @return valor indicando si el cache va a ser utilizado.
     */
    boolean getUseCache(); 
    /**
     * Asigna el valor a useCache que indica si el cache va a ser utilizado o
     * no.
     *
     * @param useCache true se utiliza false no.
     */
    void setUseCache(boolean useCache);
    /**
     * Si esta disponible la busqueda y un tipo de path determinado.
     * @param pathType tipo de path (file, object, http)
     * @return verdadero si esta disponible, falso si no
     */
    boolean isSearchAvailable(String pathType);
    /**
     * Busca el texto xml en un archivo, tabla u otra ubicación del objeto.
     * @param context contexto IXmlDom de este objeto
     * @param xmlPath ubicación del archivo
     * @return el texto xml.
     */
    String search(IXmlDom context, String xmlPath);
    /**
     * 
     * @param xmlPath  recurso buscado
     * @return verdadero si encontro el recurso y falso si no.
     */
    boolean exist(String xmlPath);    
}

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
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Su función es devolver un objeto XML (xdom) a partir de una serie de
 * parametros que permite al algoritmo encontrar o establecer el texto xml que
 * se convertirá en el objeto mencionado. El texto XML puede derivar de otros
 * textos, para ello la función utiliza un esquema de clases implementando el
 * paradigma de la orientación a objetos. 
 *
 * @author Jorge Enciso
 * @param <V> tipo de objeto DOM (org.jdom2.Document, org.w3c.dom.Document)
 * @param <E> tipo nodo o elemento del DOM.
 */
public interface IXmlDom<V, E> extends Serializable {
    String getOriginalXmlText();
    
    /**
     * Devuelve el objeto XmlSearcher
     * @return devuelve el objeto buscador de los textos xmls
     */
    IXmlSearcher<V> getXmlSearcher();    
    /**
     * Asigna implementación especial de busqueda de archivos/textos XMLs.
     * @param searcher objeto buscador
     */
    void setXmlSearcher(IXmlSearcher<V> searcher);
    
    /**
     * Informa si esta permitido cambiar los valores de los atributos que 
     * tienen las expresiones "{}" "${}" "#{}" con los valores de configParam
     * @return verdadero si esta permitido y falso si no
     */
    public Boolean isAllowChangeAttrWithParam();
    
    /**
     * Asigna un valor a allowChangeAttrWithParam con la cual habilitará o no
     * el reemplazo de los atributos que tienen las expresiones "{}" "${}" "#{}" 
     * con los valores de configParam.
     * @param allow
     */
    public void setAllowChangeAttrWithParam(boolean allow);
    
    /**
     * Agrega un parametro que será utilizado en el procesamiento del objeto
     * DOM.
     *
     * @param key clave
     * @param value valor del parametro (debe poder convertirse a string)
     */
    void addConfigParam(String key, Object value);

    /**
     * Agrega un parametro que será utilizado en el procesamiento del objeto
     * DOM.
     *
     * @param key clave
     * @param value valor del parametro
     */
    void addConfigParam(String key, String value);

    /**
     * Se ejecuta por intrucción explicita del sistema. <br>
     * Su función es crear el objeto XMLDOM a partir de un archivo XML dado.
     *
     * @param file archivo dentro del cual se buscará el texto
     * @param element nombre del tag del texto xml
     * @param notInherit Para que no considere las clases derivadas
     * @param params valores para reemplazar.
     * @return Verdadero si tuvo exito en la creación y configuración del objeto
     * XMLDOM
     * <br>Falso si no.
     */
    boolean config(File file, String element, boolean notInherit, Map<String, String> params);

    /**
     * Se ejecuta por intrucción explicita del sistema. <br>
     * Su función es crear el objeto XMLDOM a partir de un objeto DOM dado.
     *
     * @param documentPath path del documento (opcional)
     * @param xmlText texto xml que se convertira en objeto DOM
     * @param elementPath nombre del tag del texto xml
     * @param notInherit Para que no considere las clases derivadas
     * @param params Valores para reemplazar en el texto xml.
     * @return Verdadero si tuvo exito en la creación y configuración del objeto
     * XMLDOM
     * <br>Falso si no.
     *
     */
    boolean config(String documentPath, String xmlText, String elementPath, boolean notInherit, Map<String, String> params);

    /**
     * Crea nuevo elemento
     *
     * @param elementName nombre del elemento que se va crear.
     * @param nodePath nodo padre en la que se va crear el elemento
     * @return verdadero si fue creado, falso si no.
     */
    Boolean createElement(String elementName, String nodePath);


    /**
     * Devuelve una lista de elementos hijos del nodo padre solicitado.
     * @param nodePath  nodo o elemento padre.
     * @return  lista de elementos hijos.
     * @throws Exception
     */
    List<E> getChildren(String nodePath) throws Exception;

    /**
     * Devuelve un map con todos los parámetros asignados
     *
     * @return devuelve un map con todos los parámetros asignados.
     */
    Map<String, String> getConfigParam();

    /**
     * Asigna un map con todos los parámetros a reemplazar en el texto Xml
     *
     * @param params 
     */
    void setConfigParam(Map<String, String> params);
    
    /**
     * Devuelve un objeto excepción si lo hubiere generado en algún proceso del
     * componente.
     *
     * @return objeto excepción.
     */
    Exception getException();

    /**
     * Devuelve el valor del atributo de un nodo
     *
     * @param property nombre del atributo
     * @param nodePath nodo en la que se realizará la búsqueda (en formato
     * XPath)
     * @return valor del atributo o null en caso que no exista.
     */
    String getPropertyValue(String property, String nodePath);


    /**
     * Devuelve el texto xml final resultante del objeto DOM. Debio haberse
     * procesado antes el documento (ver metodo config)
     *
     * @return Devuelve el texto xml final resultante del objeto DOM
     */
    String getXml();

    /**
     * Devuelve un objeto DOM procesado en formato org.w3c.dom.Document.
     * Debio haberse procesado antes el documento (ver metodo config)
     *
     * @return devuelve un objeto DOM tipo org.w3c.dom.Document
     */
    org.w3c.dom.Document getDom();
    /**
     * Agrega un elemento o nodo hijo a un nodo padre (nodePath)
     *
     * @param elementName nombre del elemento
     * @param nodePath ubicación del nodo padre.
     * @param position la posición dentro de la lista de hijos que se va a
     * agregar.
     * @return true si tuvo exito o false si no.
     */
    Boolean insertElement(String elementName, String nodePath, int position);

    /**
     * Agrega un elemento o nodo hijo a un nodo padre (nodePath)
     *
     * @param element objeto element
     * @param nodePath ubicación del nodo padre.
     * @param position la posición dentro de la lista de hijos que se va a
     * agregar.
     * @return true si tuvo exito o false si no.
     */
    Boolean insertElement(E element, String nodePath, int position);

    /**
     * Agrega un elemento o nodo hijo a un nodo padre (nodePath)
     *
     * @param elementName nombre del elemento.
     * @param nodePath ubicación del nodo padre.
     * @return true si tuvo exito o false si no.
     */
    Boolean insertElement(String elementName, String nodePath);

    /**
     * Agrega un elemento o nodo hijo a un nodo padre (nodePath)
     *
     * @param element objeto elemento.
     * @param nodePath ubicación del nodo padre.
     * @return true si tuvo exito o false si no.
     */
    Boolean insertElement(E element, String nodePath);

    /**
     * Se utiliza para decirle al metodo que procesa el objeto DOM que guarde
     * información de donde proviene los atributos heredados.
     *
     * @return verdadero guarda información de los atributos heredados.
     */
    boolean isInfoProperty();

    /**
     * Elimina los elementos hijos de un nodo (nodePath)
     * @param nodePath  ubicación del nodo en el DOM.
     * @return verdadero si tuvo exito, falso si no.
     */
    Boolean removeChildren(String nodePath);

    /**
     * Borra un elemento
     *
     * @param nodePath nodo a borrar (en formato XPath)
     * @return true si se ha borrado correctamente o false en caso contrario.
     */
    boolean removeElement(String nodePath);

    /**
     * Se utiliza para decirle al metodo que procesa el objeto DOM que guarde
     * información de donde proviene los atributos heredados. Debe ejecutarse
     * antes del metodo config
     *
     * @param infoProperty
     */
    void setInfoProperty(boolean infoProperty);

    /**
     * Crea o cambia la propiedad de un nodo
     *
     * @param value nuevo valor de la propiedad
     * @param property nombre de la propiedad
     * @param nodePath nodo en la que se realizará el cambio (en formato
     * XPath)
     * @return true si se completó la operación con éxito o false en caso
     * contrario.
     */
    boolean setPropertyValue(String value, String property, String nodePath);
    
    /**
     * Devuelve el texto xml sin formatear.
     * @param document objeto dom
     * @return texto xml sin formatear.
     */
    String getXmlTextRaw(V document);
}

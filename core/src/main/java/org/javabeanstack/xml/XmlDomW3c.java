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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.transform.TransformerException;
import org.apache.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.Strings;
import static org.javabeanstack.util.Strings.*;

/**
 * Procesa textos xmls de distintos origenes, los parsea y los convierte en
 * objeto DOM, puede heredar o fusionar cadenas xmls. Proporciona metodos para
 * facilitar la manipulación del objeto DOM.
 *
 * @author Jorge Enciso
 */
public class XmlDomW3c implements IXmlDom<Document, Element> {

    private static final Logger LOGGER = Logger.getLogger(XmlDomW3c.class);
    /**
     * En esta propiedad se le asignará el objeto XMLDOM.
     */
    private Document xmlDom;

    /**
     * Determina si se guardará información desde donde derivó el atributo
     */
    private boolean infoProperty = false;

    private final Map<String, String> configParam = new TreeMap(String.CASE_INSENSITIVE_ORDER);

    private Boolean allowChangeAttrWithParam = true;

    private String originalXmlText;
    /**
     * Aqui se almacena el error ocurrido
     */
    private Exception exception;
    /**
     * Buscador personalizado de los textos xml
     */
    private IXmlSearcher<Document> xmlSearcher = new XmlSearcher();

    private String charSet;

    private boolean alreadyInCache = false;

    private String documentPath;
    
    public XmlDomW3c() {
        //Constructor
    }

    private void setCharSet(String xmlText) {
        if (!isNullorEmpty(charSet)) {
            return;
        }
        charSet = getXmlFileCharSet(xmlText);
        if (getConfigParam().get("encoding") == null && !isNullorEmpty(charSet)) {
            addConfigParam("encoding", charSet);
        }
    }

    /**
     * Devuelve el objeto XmlSearcher
     *
     * @return devuelve el objeto buscador de los textos xmls
     */
    @Override
    public IXmlSearcher<Document> getXmlSearcher() {
        return xmlSearcher;
    }

    /**
     * Asigna una implementación especial de busqueda de objetos xml.
     *
     * @param searcher objeto buscador
     */
    @Override
    public void setXmlSearcher(IXmlSearcher<Document> searcher) {
        this.xmlSearcher = searcher;
    }

    /**
     * Informa si esta permitido cambiar los valores de los atributos que tienen
     * las expresiones "{}" "${}" "#{}" con los valores de configParam
     *
     * @return verdadero si esta permitido y falso si no
     */
    @Override
    public Boolean isAllowChangeAttrWithParam() {
        return allowChangeAttrWithParam;
    }

    /**
     * Asigna un valor a allowChangeAttrWithParam con la cual habilitará o no el
     * reemplaza de los atributos que tienen las expresiones "{}" "${}" "#{}"
     * con los valores de configParam.
     *
     * @param allow
     */
    @Override
    public void setAllowChangeAttrWithParam(boolean allow) {
        this.allowChangeAttrWithParam = allow;
    }

    /**
     * Texto xml antes de que se reemplaze los atributos con los valores de
     * configParam
     *
     * @return texto original xml antes de ejecutarse replaceAttrWithParamValues
     */
    @Override
    public String getOriginalXmlText() {
        return originalXmlText;
    }

    /**
     * Se utiliza para decirle al metodo que procesa el objeto DOM que guarde
     * información de donde proviene los atributos heredados.
     *
     * @return verdadero guarda información de los atributos heredados.
     */
    @Override
    public boolean isInfoProperty() {
        return infoProperty;
    }

    /**
     * Se utiliza para decirle al metodo que procesa el objeto DOM que guarde
     * información de donde proviene los atributos heredados.
     *
     * @param infoProperty
     */
    @Override
    public void setInfoProperty(boolean infoProperty) {
        this.infoProperty = infoProperty;
    }

    /**
     * Devuelve un objeto excepción si lo hubiere generado en algún proceso del
     * componente.
     *
     * @return objeto excepción.
     */
    @Override
    public Exception getException() {
        return exception;
    }

    /**
     * Devuelve un objeto DOM procesado en formato org.w3c.dom.Document
     *
     * @return devuelve un objeto DOM tipo org.w3c.dom.Document
     */
    @Override
    public Document getDom() {
        try {
            return xmlDom;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
            exception = ex;
        }
        return null;
    }

    /**
     * Devuelve el texto xml final resultante del objeto DOM. Debio haberse
     * procesado antes el documento (ver metodo config)
     *
     * @return Devuelve el texto xml final resultante del objeto DOM
     */
    @Override
    public String getXml() {
        try {
            return DomW3cParser.getXmlText(xmlDom);
        } catch (TransformerException ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Devuelve el texto xml de un objeto DOM sin formatear
     *
     * @param document
     * @return texto xml
     */
    @Override
    public String getXmlTextRaw(Document document) {
        try {
            String defaultEncoding
                    = Fn.nvl(getConfigParam().get("encoding"), "UTF-8");
            return DomW3cParser.getXmlText(document, defaultEncoding);
        } catch (TransformerException ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Devuelve un map con todos los parámetros asignados
     *
     * @return devuelve un map con todos los parámetros asignados.
     */
    @Override
    public Map<String, String> getConfigParam() {
        return configParam;
    }

    @Override
    public void setConfigParam(Map<String, String> params) {
        configParam.clear();
        if (params == null) {
            return;
        }
        params.entrySet().forEach( element -> 
            configParam.put(element.getKey(), element.getValue())
        );
    }

    /**
     * Agrega un parametro que será utilizado en el procesamiento del objeto
     * DOM.
     *
     * @param key clave
     * @param value valor del parametro (debe poder convertirse a string)
     */
    @Override
    public void addConfigParam(String key, Object value) {
        configParam.put(key, value.toString());
    }

    /**
     * Agrega un parametro que será utilizado en el procesamiento del objeto
     * DOM.
     *
     * @param key clave
     * @param value valor del parametro
     */
    @Override
    public void addConfigParam(String key, String value) {
        configParam.put(key, value);
    }

    /**
     * Se ejecuta por intrucción explicita del sistema. <br>
     * Su función es crear el objeto XMLDOM a partir de un archivo XML dado.
     *
     * @param file archivo dentro del cual se buscará el texto
     * @param element nombre del tag del texto xml
     * @param notInherit Para que no considere las clases derivadas
     * @param params parámetros para el procesamiento del objeto.
     * @return Verdadero si tuvo exito en la creación y configuración del objeto
     * XMLDOM
     * <br>Falso si no.
     */
    @Override
    public boolean config(File file, String element, boolean notInherit, Map<String, String> params) {
        setConfigParam(params);
        return this.config("file://" + file.getPath(), "", element, notInherit);
    }

    /**
     * Se ejecuta por intrucción explicita del sistema. <br>
     * Su función es crear el objeto XMLDOM a partir de un archivo XML dado.
     *
     * @param file archivo dentro del cual se buscará el texto
     * @param element nombre del tag del texto xml
     * @param notInherit Para que no considere las clases derivadas
     * @return Verdadero si tuvo exito en la creación y configuración del objeto
     * XMLDOM
     * <br>Falso si no.
     */
    @Override
    public boolean config(File file, String element, boolean notInherit) {
        xmlDom = null;
        return this.config("file://" + file.getPath(), "", element, notInherit);
    }

    /**
     * Se ejecuta por intrucción explicita del sistema. <br>
     * Su función es crear el objeto XMLDOM a partir de un archivo XML dado.
     *
     * @param input archivo dentro del cual se buscará el texto
     * @param element nombre del tag del texto xml
     * @param notInherit Para que no considere las clases derivadas
     * @param params parámetros para el procesamiento del objeto.
     * @return Verdadero si tuvo exito en la creación y configuración del objeto
     * XMLDOM
     * <br>Falso si no.
     */
    @Override
    public boolean config(InputStream input, String element, boolean notInherit, Map<String, String> params) {
        setConfigParam(params);
        return this.config(input, element, notInherit);
    }

    /**
     * Se ejecuta por intrucción explicita del sistema. <br>
     * Su función es crear el objeto XMLDOM a partir de un archivo XML dado.
     *
     * @param input archivo dentro del cual se buscará el texto
     * @param element nombre del tag del texto xml
     * @param notInherit Para que no considere las clases derivadas
     * @return Verdadero si tuvo exito en la creación y configuración del objeto
     * XMLDOM
     * <br>Falso si no.
     */
    @Override
    public boolean config(InputStream input, String element, boolean notInherit) {
        String result = "";
        try {
            String encoding = getConfigParam().get("encoding");
            encoding = (isNullorEmpty(encoding)) ? "UTF-8" : encoding;
            result = Strings.streamToString(input, encoding);
        } catch (IOException ex) {
            ErrorManager.showError(ex, LOGGER);
            exception = ex;
        }
        if (exception != null) {
            return false;
        }
        xmlDom = null;
        return this.config("", result, element, notInherit);
    }

    /**
     * Se ejecuta por intrucción explicita del sistema. <br>
     * Su función es crear el objeto XMLDOM a partir de un objeto DOM dado.
     *
     * @param documentPath path del documento (opcional) <br>(ej.
     * file:///carpeta/archivo.xml, http://dominio/archivo.xml, obj://xmlname)
     * @param xmlText texto xml que se convertira en objeto DOM
     * @param elementPath nombre del tag del texto xml
     * @param notInherit Para que no considere las clases derivadas
     * @return Verdadero si tuvo exito en la creación y configuración del objeto
     * XMLDOM
     * <br>Falso si no.
     *
     */
    @Override
    public boolean config(String documentPath, String xmlText, String elementPath, boolean notInherit) {
        try {
            this.documentPath = documentPath;
            exception = null;
            xmlDom = null;
            charSet = "";
            alreadyInCache = false;
            setCharSet(xmlText);

            xmlDom = getObject(documentPath, xmlText, elementPath, notInherit);
            if (xmlDom == null) {
                return false;
            }
            if (!alreadyInCache) {
                getXmlSearcher().addToCache(this, documentPath, elementPath, xmlDom, true);
            }
            replaceAttrWithParamValues();
            return true;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
            exception = ex;
        }
        return false;
    }

    @Override
    public boolean config(String documentPath, String xmlText, String elementPath, boolean notInherit, Map<String, String> params) {
        setConfigParam(params);
        return config(documentPath, xmlText, elementPath, notInherit);
    }

    /**
     * Su función es<br><br>
     * Devolver un objeto XMLDOM a partir de:<br>
     * a) Un archivo xml o<br>
     * b) Una cadena con formato xml<br>
     * c) Otro objeto DOM<br>
     *
     * @param documentPath dirección donde se encontra el texto XML dentro de la
     * tabla objetos, o un archivo de texto. <br>(ej.
     * file:///carpeta/archivo.xml, http://dominio/archivo.xml, obj://xmlname)
     * @param xml cadena dentro del cual se buscará el texto
     * @param elementPath nombre del tag del texto xml
     * @param notInherit Para que no considere las clases derivadas
     * @return Objeto XMLDOM si tuvo exito y nulo si hubo algún error.
     */
    private Document getObject(String documentPath, Object xml, String elementPath, boolean notInherit) throws Exception {
        Document document = null;
        boolean processed = false;
        // Validar viabilidad para devolver el objeto requerido
        if ((xml instanceof String) && isNullorEmpty((String) xml) && isNullorEmpty(documentPath)) {
            return null;
        }
        if ((xml instanceof String)) {
            // Si solo tenemos el path del documento.
            if (!isNullorEmpty(documentPath) && isNullorEmpty(elementPath) && isNullorEmpty((String) xml)) {
                //Buscar en el cache
                IXmlCache<Document> cache = xmlSearcher.getFromCache(documentPath);
                if (cache == null) {
                    xml = this.getString(documentPath, "", "");
                    document = DomW3cParser.loadXml((String) xml);
                    processed = true;
                } else {
                    document = cache.getDom();
                    if (document != null && cache.isCompiled()) {
                        return document;
                    }
                    processed = true;
                }

            } // Si se tiene el texto xml y el elementPath, conseguir el texto apuntado por el elementPath
            else if (!isNullorEmpty((String) xml) && !isNullorEmpty(elementPath)) {
                xml = this.getString(documentPath, (String) xml, elementPath);
            } // Si no se tiene el texto xml y se tiene el elementPath y el documentPath
            else if (!isNullorEmpty(documentPath) && !isNullorEmpty(elementPath)) {
                // Buscar en el cache
                IXmlCache<Document> cache = xmlSearcher.getFromCache(documentPath, elementPath);
                boolean check = false;
                if (cache == null) {
                    cache = xmlSearcher.getFromCache(documentPath);
                    check = true;
                    if (this.documentPath.equals(documentPath)) {
                        alreadyInCache = true;
                    }
                }
                document = (cache != null) ? cache.getDom() : null;
                // Si existe en cache
                if (document != null) {
                    // Si el cache corresponde el elementPath
                    if (cache.isCompiled()
                            && (!check || elementPath.equals(document.getDocumentElement().getNodeName()))) {
                        return document;
                    }
                    // Traer solo del elementpath
                    if (!document.getDocumentElement().getNodeName().equals(elementPath)) {
                        Node root = DomW3cParser.getElement(document, elementPath);
                        Document document2 = DomW3cParser.newDocument();
                        document2.appendChild(document2.adoptNode(root.cloneNode(true)));
                        document = document2;
                        if (cache.isCompiled()) {
                            return document;
                        }
                    }
                    processed = true;
                } // Si no existe en cache buscar el texto xml
                else {
                    xml = this.getString(documentPath, "", elementPath);
                }
            }
        }
        if (!processed) {
            if (xml instanceof String) {
                if (isNullorEmpty((String) xml)) {
                    return null;
                }
                setCharSet((String) xml);
                document = DomW3cParser.loadXml((String) xml);
            } else if (xml instanceof Document) {
                document = (Document) xml;
            }
        }
        if (document == null) {
            return null;
        }
        // Si se heredará el texto de las clases
        if (!notInherit) {
            Element node = document.getDocumentElement();
            String xmlText = DomW3cParser.getXmlText(node, getConfigParam().get("encoding"));
            // Si existe elementos que derivan de clases
            if (!isNullorEmpty(node.getAttribute("clase"))
                    || findString("clase", xmlText.toLowerCase()) > 0) {
                if (isNullorEmpty(node.getAttribute("src"))) {
                    node.setAttribute("src", documentPath);
                }
                // Heredar elementos y atributos
                this.inherit(node,
                        node.getAttribute("clase"),
                        node.getAttribute("src"));
            }
        }
        return document;
    }

    /**
     * Su función es buscar todos los tags o elementos correspondientes a las
     * clases de la cual derivan el objeto que se esta procesando.
     *
     * @param node nodo a procesar
     * @param className clase del que deriva el nodo o elemento
     * @param src ubicación del texto xml al que hace referencia la clase.
     * @return nodo procesado con las clases derivadas.
     * @throws Exception
     */
    private Element inherit(Element node, String className, String src) throws Exception {
        Document doc = null;
        if (!isNullorEmpty(className)) {
            doc = this.getObject(src, "", className, false);
        }
        Element nodeClass = null;
        if (doc != null) {
            nodeClass = doc.getDocumentElement();
            // Heredar los atributos de las clases
            inheritAttribute(node, nodeClass, src);
        }
        // Heredar los nodos o elementos de las clases
        inheritNode(node, nodeClass, src);
        return node;
    }

    /**
     * Su función es la asignación al nodo a procesar de los atributos que
     * heredará de la clase.
     *
     * @param node nodo a procesar
     * @param nodeClass nodo clase.
     * @param src ubicación de la clase.
     * @return nodo o elemento procesado.
     */
    private Element inheritAttribute(Element node, Element nodeClass, String src) {
        if (nodeClass == null) {
            return node;
        }
        // Heredar las propiedades
        Attr attribute;
        NamedNodeMap attributes = nodeClass.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            attribute = (Attr) attributes.item(i);
            if ("__".equals(left(attribute.getName(), 2))) {
                continue;
            }
            if (!node.hasAttribute(attribute.getName())) {
                if (infoProperty) {
                    // Se guarda la información desde donde derivo el atributo
                    if (!nodeClass.hasAttribute("__" + attribute.getName())) {
                        node.setAttribute("__" + attribute.getName(), src);
                    } else {
                        node.setAttribute("__" + attribute.getName(), nodeClass.getAttribute("__" + attribute.getName()));
                    }
                }
                node.setAttribute(attribute.getName(), attribute.getValue());
            }
        }
        return node;
    }

    /**
     * Su función es la asignación al nodo a procesar de los elementos o nodos
     * hijos que debe heredar de la clase.
     *
     * @param node nodo a procesar
     * @param nodeClass nodo clase.
     * @param src ubicación de la clase.
     * @return nodo o elemento procesado.
     */
    private Element inheritNode(Element node, Element nodeClass, String src) throws Exception {
        if (nodeClass != null) {
            Element firstChild = getFirstElement(node);
            // Heredar de la clase los objetos componentes
            for (Element childNodeClass : DomW3cParser.getChildren(nodeClass)) {
                // Ver si existe
                Element nodeFound = DomW3cParser.getChild(node, childNodeClass.getNodeName());
                if (nodeFound == null) {
                    // Insertar antes del primer nodo hijo si existe nodo hijo
                    Element childNode = (Element) childNodeClass.cloneNode(true);
                    DomW3cParser.insertElementBefore(childNode, node, firstChild);
                } else {
                    inheritAttribute(nodeFound, childNodeClass, src);
                    Element firstNodeChild = getFirstElement(nodeFound);
                    // Heredar los nodos que no existen en el original                  
                    List<Element> childrenNodeClass = DomW3cParser.getChildren(childNodeClass);
                    for (Element nodeAdd : childrenNodeClass) {
                        // Ver si existe
                        Element nodeChildFound = DomW3cParser.getChild(nodeFound, nodeAdd.getNodeName());
                        if (nodeChildFound == null) {
                            // Insertar antes del primer nodo hijo si existe nodo hijo
                            DomW3cParser.insertElementBefore((Element) nodeAdd.cloneNode(true), nodeFound, firstNodeChild);
                        }
                    }
                }
            }
        }
        List<Element> children = DomW3cParser.getChildren(node);
        String xmlNodo = DomW3cParser.getXmlText(node, getConfigParam().get("encoding"));
        for (int i = 0; i < children.size(); i++) {
            Element nodeChild = children.get(i);
            if (findString("clase", xmlNodo.toLowerCase()) > 0) {
                Document dom = getObject(src,
                        DomW3cParser.getXmlText(nodeChild, getConfigParam().get("encoding")), "", false);
                Element newNode = (Element) dom.getDocumentElement().cloneNode(true);
                DomW3cParser.replaceChild(node, newNode, nodeChild);
            }
        }
        return node;
    }

    /**
     * Su función es buscar el texto xml solicitado utilizando un parametro de
     * ubicación (xmlPath) y/o un texto xml que lo engloba y la ubicación del
     * elemento o nodo
     *
     * @param xmlPath ubicación del archivo/objeto donde se encuentra el texto
     * xml (ej. file:///carpeta/archivo.xml, http://dominio/archivo.xml,
     * obj://xmlname)
     * @param xml texto xml que engloba el texto buscado
     * @param elementPath la ubicación del elemento dentro del texto xml o
     * archivo xml
     * @return texto xml solicitado.
     */
    private String getString(String xmlPath, String xml, String elementPath) throws Exception {
        Document document = null;
        // Buscar en el cache si no se paso el texto xml
        if (isNullorEmpty(xml)) {
            // Buscar en el cache
            IXmlCache<Document> cache = xmlSearcher.getFromCache(xmlPath);
            document = (cache != null) ? cache.getDom() : null;
        }
        // Si no se encontro en el cache y no se tiene el texto xml
        if (document == null && isNullorEmpty(xml)) {
            xml = searchXmlText(xmlPath);
            setCharSet(xml);
            if (isNullorEmpty(elementPath)) {
                return xml;
            }
            document = DomW3cParser.loadXml(xml);
            getXmlSearcher().addToCache(this, xmlPath, document);
        } // Si se tiene el texto xml
        else if (!isNullorEmpty(xml)) {
            setCharSet(xml);
            document = DomW3cParser.loadXml(xml);
        }
        String encoding = null;
        if (!isNullorEmpty(elementPath) && document != null
                && (!document.getDocumentElement().getNodeName().equals(elementPath))) {
            Element root = DomW3cParser.getElement(document, elementPath);
            Document document2 = DomW3cParser.newDocument();
            document2.appendChild(document2.adoptNode(root.cloneNode(true)));
            document = document2;
        }
        encoding = Fn.nvl(encoding, this.getConfigParam().get("encoding"));
        return DomW3cParser.getXmlText(document, encoding);
    }

    /**
     * Busca el archivo al que hace referencia el xmlPath
     *
     * @param xmlPath ubicación del archivo (ej. file:///carpeta/archivo.xml,
     * http://dominio/archivo.xml, obj://xmlname)
     * @return el texto del archivo buscado.
     */
    private String searchXmlText(String xmlPath) {
        if (isNullorEmpty(xmlPath)) {
            return "";
        }
        return getXmlSearcher().search(this, xmlPath);
    }

    /**
     * Devuelve el primer elemento de un nodo si lo tiene
     *
     * @param parent
     * @return Devuelve el primer elemento de un nodo si lo tiene.
     */
    private Element getFirstElement(Element parent) {
        if (!parent.hasChildNodes()) {
            return null;
        }
        List<Element> children = DomW3cParser.getChildren(parent);
        if (children.isEmpty()) {
            return null;
        }
        return children.get(0);
    }

    /**
     * Crea nuevo elemento
     *
     * @param elementName nombre del elemento que se va crear.
     * @param nodePath nodo padre en la que se va crear el elemento
     * @return verdadero si fue creado, falso si no.
     */
    @Override
    public Boolean createElement(String elementName, String nodePath) {
        try {
            Element element = DomW3cParser.createElement(xmlDom, elementName, nodePath);
            return (element != null);
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
            exception = ex;
        }
        return false;
    }

    /**
     * Agrega un elemento o nodo hijo a un nodo padre (nodePath)
     *
     * @param elementName nombre del elemento
     * @param nodePath ubicación del nodo padre.
     * @param position la posición dentro de la lista de hijos que se va a
     * agregar.
     * @return true si tuvo exito o false si no.
     */
    @Override
    public Boolean insertElement(String elementName, String nodePath, int position) {
        try {
            Element parent = DomW3cParser.getElement(xmlDom, nodePath);
            Element element = xmlDom.createElement(elementName);
            element = DomW3cParser.createElement(element, parent, position);
            return (element != null);
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
            exception = ex;
        }
        return false;
    }

    /**
     * Agrega un elemento o nodo hijo a un nodo padre (nodePath)
     *
     * @param element objeto element
     * @param nodePath ubicación del nodo padre.
     * @param position la posición dentro de la lista de hijos que se va a
     * agregar.
     * @return true si tuvo exito o false si no.
     */
    @Override
    public Boolean insertElement(Element element, String nodePath, int position) {
        try {
            Element parent = DomW3cParser.getElement(xmlDom, nodePath);
            DomW3cParser.createElement(element, parent, position);
            return true;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
            exception = ex;
        }
        return false;
    }

    /**
     * Agrega un elemento o nodo hijo a un nodo padre (nodePath)
     *
     * @param elementName nombre del elemento.
     * @param nodePath ubicación del nodo padre.
     * @return true si tuvo exito o false si no.
     */
    @Override
    public Boolean insertElement(String elementName, String nodePath) {
        return createElement(elementName, nodePath);
    }

    /**
     * Agrega un elemento o nodo hijo a un nodo padre (nodePath)
     *
     * @param element objeto elemento.
     * @param nodePath ubicación del nodo padre.
     * @return true si tuvo exito o false si no.
     */
    @Override
    public Boolean insertElement(Element element, String nodePath) {
        try {
            Element parent = DomW3cParser.getElement(xmlDom, nodePath);
            DomW3cParser.createElement(element, parent);
            return true;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
            exception = ex;
        }
        return false;
    }

    /**
     * Devuelve una lista de elementos hijos del nodo padre solicitado.
     *
     * @param nodePath nodo o elemento padre.
     * @return lista de elementos hijos.
     * @throws Exception
     */
    @Override
    public List<Element> getChildren(String nodePath) throws Exception {
        try {
            return DomW3cParser.getChildren(xmlDom, nodePath);
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
            exception = ex;
        }
        return new ArrayList();
    }

    /**
     * Borra un elemento
     *
     * @param nodePath nodo a borrar (en formato XPath)
     * @return true si se ha borrado correctamente o false en caso contrario.
     */
    @Override
    public boolean removeElement(String nodePath) {
        try {
            return DomW3cParser.removeElement(xmlDom, nodePath);
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
            exception = ex;
        }
        return false;
    }

    /**
     * Elimina los elementos hijos de un nodo (nodePath)
     *
     * @param nodePath ubicación del nodo en el DOM.
     * @return verdadero si tuvo exito, falso si no.
     */
    @Override
    public Boolean removeChildren(String nodePath) {
        try {
            Element node = DomW3cParser.getElement(xmlDom, nodePath);
            while (node.hasChildNodes()) {
                Node child = node.getChildNodes().item(0);
                node.removeChild(child);
            }
            return true;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
            exception = ex;
        }
        return false;
    }

    /**
     * Devuelve el valor del atributo de un nodo
     *
     * @param property nombre del atributo
     * @param nodePath nodo en la que se realizará la búsqueda (en formato
     * XPath)
     * @return valor del atributo o null en caso que no exista.
     */
    @Override
    public String getPropertyValue(String property, String nodePath) {
        try {
            return DomW3cParser.getPropertyValue(xmlDom, property, nodePath);
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
            exception = ex;
        }
        return null;
    }

    /**
     * Crea o cambia la propiedad de un nodo
     *
     * @param value nuevo valor de la propiedad
     * @param property nombre de la propiedad
     * @param nodePath nodo en la que se realizará el cambio (en formato XPath)
     * @return true si se completó la operación con éxito o false en caso
     * contrario.
     */
    @Override
    public boolean setPropertyValue(String value, String property, String nodePath) {
        try {
            return DomW3cParser.setPropertyValue(xmlDom, value, property, nodePath);
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
            exception = ex;
        }
        return false;
    }

    /**
     * Reemplaza todos los valores de atributos que tienen la siguiente
     * expresion ${key}, #{key}, {key} por el valor del correspondiente de
     * configParam
     *
     * @throws Exception
     */
    private void replaceAttrWithParamValues() throws Exception {
        if (configParam.isEmpty()) {
            return;
        }

        if (!allowChangeAttrWithParam) {
            return;
        }

        //Guardar texto original
        originalXmlText = this.toString();

        // Seleccionar todos los elementos cuyos valores comienzan con "#{" or
        // "${" or "{" y termina con "}"
        NodeList elements
                = DomW3cParser.selectNodes(xmlDom,
                        "//*[@*[(starts-with(., '#{') or starts-with(., '${') or starts-with(., '{'))"
                        + "  and (contains(.,'}')) ]]");
        Node node;
        Attr attr;
        String key, value;
        for (int i = 0; i < elements.getLength(); i++) {
            node = elements.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                NamedNodeMap attributes = node.getAttributes();
                for (int k = 0; k < attributes.getLength(); k++) {
                    attr = (Attr) attributes.item(k);
                    if (attr.getValue().endsWith("}")) {
                        key = attr.getValue().replaceAll("\\}", "");
                        if (key.startsWith("{")) {
                            key = substr(key, 1);
                        } else if (key.startsWith("${") || key.startsWith("#{")) {
                            key = substr(key, 2);
                        }
                        value = configParam.get(key);
                        if (value != null) {
                            attr.setValue(value);
                        }
                    }
                }
            }
        }

        // Seleccionar todos los elementos cuyos valores contienen los siguientes 
        // literales "#{" or "${" or "{" y  "}"
        elements
                = DomW3cParser.selectNodes(xmlDom,
                        "//*[@*[(contains(., '#{') or contains(., '${') or contains(., '{'))"
                        + "  and (contains(.,'}')) ]]");

        for (int i = 0; i < elements.getLength(); i++) {
            node = elements.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                NamedNodeMap attributes = node.getAttributes();
                for (int k = 0; k < attributes.getLength(); k++) {
                    attr = (Attr) attributes.item(k);
                    if (attr.getValue().contains("{")) {
                        value = textMerge(attr.getValue(), configParam, "$# ");
                        attr.setValue(value);
                    }
                }
            }
        }
    }

    /**
     * Devuelve un objeto XMLDOM en formato texto.
     *
     * @return objeto XMLDOM en formato texto formateada.
     */
    @Override
    public String toString() {
        return this.getXml();
    }
}

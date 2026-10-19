/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.XMLConstants;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.Strings;
import static org.javabeanstack.util.Strings.*;

/**
 * Esta clase implementa todas las funcionalidades de XDOM. Se agrego algunas
 * variaciones a la misma de tal manera a poder tener textos XML que hereden de
 * otros textos.
 *
 * @author Jorge Enciso
 */
public class DomW3cParser {

    private static final Logger LOGGER = LogManager.getLogger(DomW3cParser.class);
    /**
     * Característica de Xerces que controla la expansión diferida de nodos.
     * Con expansión diferida cada nodo se construye la primera vez que se lo
     * lee, y esa construcción no es segura si dos hilos leen a la vez el mismo
     * documento (hallazgo I1-07, 2026-09-24). Se desactiva para que el DOM
     * quede completo al cargarse.
     */
    private static final String DEFER_NODE_EXPANSION
            = "http://apache.org/xml/features/dom/defer-node-expansion";
    /**
     * Rechaza toda declaración {@code <!DOCTYPE>} (plan XMLSEC). Es la defensa
     * principal contra XXE —una entidad externa que lee un archivo del
     * servidor o hace una petición— y contra la expansión exponencial de
     * entidades: sin DOCTYPE no hay entidades. Ningún XML del sistema declara
     * DOCTYPE (relevamiento del 2026-10-19), y por este parser pasan archivos
     * que viven fuera del artefacto ({@code configs/rptwizard.xml},
     * {@code configs/systemparam_init.xml}, {@code PortalConfig.xml}, los XML de
     * pantallas e informes de la instalación y de cada empresa). Mismo criterio
     * que {@code DBManagerV30.loadConfigs()}.
     */
    private static final String DISALLOW_DOCTYPE
            = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String EXTERNAL_GENERAL_ENTITIES
            = "http://xml.org/sax/features/external-general-entities";
    private static final String EXTERNAL_PARAMETER_ENTITIES
            = "http://xml.org/sax/features/external-parameter-entities";
    private static final String LOAD_EXTERNAL_DTD
            = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    /**
     * Protecciones que la implementación JAXP en uso no admitió, para avisar
     * una sola vez por protección y no en cada fábrica (la de transformación
     * se crea en cada {@code getXmlText}).
     */
    private static final Set<String> PROTECCIONES_NO_ADMITIDAS = ConcurrentHashMap.newKeySet();
    /**
     * {@code ErrorHandler} de los constructores de documentos (ver
     * {@link #newDocumentBuilder()}): no escribe en stderr. Un error
     * recuperable no corta la lectura, como en Xerces, pero deja un
     * {@code WARN} (antes se veía en stderr); los fatales llegan solo por la
     * excepción.
     */
    private static final ErrorHandler SILENCIOSO = new ErrorHandler() {
        @Override
        public void warning(SAXParseException ex) {
            LOGGER.debug("Aviso al leer XML: " + ex.getMessage());
        }

        @Override
        public void error(SAXParseException ex) {
            LOGGER.warn("Error recuperable al leer XML: " + ex.getMessage());
        }

        @Override
        public void fatalError(SAXParseException ex) throws SAXException {
            throw ex;
        }
    };
    /**
     * Fábrica de documentos única, con las protecciones de XMLSEC.
     *
     * <p>JAXP no garantiza que {@code DocumentBuilderFactory} ni
     * {@code XPathFactory} sean seguras para hilos, y estas se usan desde beans
     * concurrentes. Por eso cada {@code DocumentBuilder} y cada {@code XPath} se
     * crea <b>sincronizando sobre su fábrica</b> ({@link #newDocumentBuilder()},
     * {@link #newXPath()}) y no se comparte: es por llamada.</p>
     *
     * <p>Hasta el 2026-10-19 eran una por hilo ({@code ThreadLocal}, I1-07), y
     * eso fugaba: los hilos son del servidor y sobreviven al redespliegue, y en
     * los WAR que embeben Xerces la fábrica es una clase del WAR. Cada hilo
     * retenía su fábrica, la fábrica el classloader y el classloader el WAR
     * entero (plan TLLEAK, XMLSEC M1-01). Un campo estático, en cambio, muere
     * con su propia clase.</p>
     *
     * <p><b>Invariante del que depende:</b> cada despliegue lleva su propio
     * {@code jbs-core} (en {@code WEB-INF/lib} o en el {@code lib/} del EAR),
     * así que hay una clase {@code DomW3cParser} por despliegue. Si
     * {@code jbs-core} pasara a un módulo compartido del servidor, esta fábrica
     * única —resuelta con el classloader de contexto de la primera llamada—
     * retendría para siempre el Xerces del primer WAR que la usara.</p>
     */
    private static final DocumentBuilderFactory FACTORY = newDocumentBuilderFactory();
    /** Fábrica de XPath única; ver {@link #FACTORY}. */
    private static final XPathFactory XPATHFACTORY = XPathFactory.newInstance();
    private static final String DEFAULTCHARSET = "UTF-8";

    private DomW3cParser() {
    }

    /**
     * Crea la fábrica de documentos con la expansión diferida desactivada y
     * las protecciones contra XXE y expansión de entidades (plan XMLSEC).
     *
     * <p>Si la implementación JAXP en uso no reconoce la expansión diferida,
     * se sigue sin ella: la seguridad entre hilos la garantiza igual quien
     * comparta el documento sincronizando sobre él.</p>
     *
     * <p>Cada protección se aplica por separado: una que la implementación
     * no admita no impide aplicar las demás. Con que rija
     * {@link #DISALLOW_DOCTYPE} alcanza; las otras son defensa en profundidad
     * por si una implementación la ignorara. Por eso solo las dos principales
     * avisan con {@code WARN} (una vez por protección) si no se admiten; las
     * demás, en {@code DEBUG}: el Xerces de WildFly, por ejemplo, no admite
     * los atributos {@code ACCESS_EXTERNAL_*}, y un aviso en cada arranque
     * sería ruido.</p>
     *
     * @return fábrica de documentos.
     */
    private static DocumentBuilderFactory newDocumentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature(DEFER_NODE_EXPANSION, false);
        } catch (ParserConfigurationException ex) {
            LOGGER.debug("La implementación JAXP " + factory.getClass().getName()
                    + " no admite " + DEFER_NODE_EXPANSION);
        }
        proteger(factory, DISALLOW_DOCTYPE, true, true);
        proteger(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true, true);
        proteger(factory, EXTERNAL_GENERAL_ENTITIES, false, false);
        proteger(factory, EXTERNAL_PARAMETER_ENTITIES, false, false);
        proteger(factory, LOAD_EXTERNAL_DTD, false, false);
        protegerAtributo(factory, XMLConstants.ACCESS_EXTERNAL_DTD);
        protegerAtributo(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA);
        try {
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
        } catch (RuntimeException ex) {
            avisarNoAdmitida(factory, "XInclude/ExpandEntityReferences", false);
        }
        return factory;
    }

    /**
     * Constructor de documentos nuevo de la fábrica única, con un
     * {@code ErrorHandler} que no escribe en stderr: el de Xerces por defecto
     * vuelca {@code [Fatal Error] ...} a la consola (en WildFly, una línea
     * {@code ERROR [stderr]} en server.log) además de lanzar. Mismo criterio
     * que el de Xerces —los errores recuperables no cortan la lectura y los
     * fatales sí—, pero el fatal llega solo por la excepción.
     *
     * @return un constructor nuevo, por llamada (no se comparte).
     * @throws ParserConfigurationException si la fábrica no puede crearlo.
     */
    private static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilder builder;
        synchronized (FACTORY) {
            builder = FACTORY.newDocumentBuilder();
        }
        builder.setErrorHandler(SILENCIOSO);
        return builder;
    }

    /**
     * Un {@code XPath} nuevo, creado sincronizando sobre la fábrica (ver
     * {@link #FACTORY}). Es por llamada: no se comparte.
     *
     * @return un {@code XPath} nuevo.
     */
    private static XPath newXPath() {
        synchronized (XPATHFACTORY) {
            return XPATHFACTORY.newXPath();
        }
    }

    /**
     * Activa o desactiva una característica de seguridad de la fábrica; si la
     * implementación no la admite, lo avisa una vez y sigue.
     *
     * @param principal verdadero si es una de las protecciones principales
     * (avisa en {@code WARN}); falso si es defensa en profundidad
     * ({@code DEBUG}).
     */
    private static void proteger(DocumentBuilderFactory factory, String feature, boolean valor,
            boolean principal) {
        try {
            factory.setFeature(feature, valor);
        } catch (ParserConfigurationException | RuntimeException ex) {
            avisarNoAdmitida(factory, feature, principal);
        }
    }

    /**
     * Deja vacío (ningún protocolo permitido) un atributo de acceso externo
     * de la fábrica; si la implementación no lo admite, lo avisa una vez y
     * sigue.
     */
    private static void protegerAtributo(DocumentBuilderFactory factory, String atributo) {
        try {
            factory.setAttribute(atributo, "");
        } catch (RuntimeException ex) {
            avisarNoAdmitida(factory, atributo, false);
        }
    }

    private static void avisarNoAdmitida(Object factory, String proteccion, boolean principal) {
        if (PROTECCIONES_NO_ADMITIDAS.add(factory.getClass().getName() + ":" + proteccion)) {
            String mensaje = "La implementación JAXP " + factory.getClass().getName()
                    + " no admite la protección " + proteccion + "; rigen las demás";
            if (principal) {
                LOGGER.warn(mensaje);
            } else {
                LOGGER.debug(mensaje);
            }
        }
    }

    /**
     * Crea un objeto Document
     *
     * @return objeto document
     * @throws ParserConfigurationException
     */
    public static Document newDocument() throws ParserConfigurationException {
        return newDocumentBuilder().newDocument();
    }

    /**
     * Crea y devuelve un documento DOM
     *
     * @param xml texto xml a procesar
     * @return elemento creado o null en caso contrario.
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws org.xml.sax.SAXException
     * @throws java.io.IOException
     */
    public static Document loadXml(String xml)
            throws ParserConfigurationException, SAXException, IOException {
        
        if (Fn.nvl(xml,"").isEmpty()){
            return null;
        }
        DocumentBuilder builder = newDocumentBuilder();
        InputStream stream;
        String charSet = getXmlFileCharSet(xml);
        if (!charSet.isEmpty()) {
            stream = new ByteArrayInputStream(xml.getBytes(charSet));
        } else {
            stream = new ByteArrayInputStream(xml.getBytes());
        }
        return builder.parse(stream);
    }

    /**
     * Crea y devuelve un documento DOM
     *
     * @param xml texto xml a procesar
     * @return elemento creado o null en caso contrario.
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws org.xml.sax.SAXException
     * @throws java.io.IOException
     */
    public static Document loadXml(InputStream xml)
            throws ParserConfigurationException, SAXException, IOException {

        DocumentBuilder builder = newDocumentBuilder();
        return builder.parse(xml);
    }

    /**
     * Crea y devuelve un documento DOM
     *
     * @param file archivo a procesar.
     * @return elemento creado o null en caso contrario.
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws java.io.FileNotFoundException
     * @throws org.xml.sax.SAXException
     */
    public static Document loadXml(File file) throws
            ParserConfigurationException, FileNotFoundException, IOException, SAXException {
        if (file.exists() && file.isFile() && file.canRead() && file.length() > 0) {
            return loadXml(Strings.streamToString(new FileInputStream(file)));
        }
        return null;
    }

    /**
     * Crea nuevo elemento
     *
     * @param xmlDom DOM donde se creara el elemento.
     * @param elementName nombre del elemento que se va crear.
     * @param nodePath nodo padre en la que se va crear el elemento
     * @return elemento creado o null en caso contrario.
     * @throws java.lang.Exception
     */
    public static Element createElement(Document xmlDom, String elementName, String nodePath) throws Exception {
        Element element = xmlDom.createElement(elementName);
        Element nodeParent = getElement(xmlDom, nodePath);
        if (nodeParent != null) {
            element = createElement(element, nodeParent);
            return element;
        }
        return null;
    }

    /**
     * Crea nuevo elemento
     *
     * @param xmlDom DOM donde se creara el elemento.
     * @param element nombre del elemento que se va crear (de tipo
     * org.jdom.Element)
     * @param nodePath nodo en la que se va crear (en formato XPath)
     * @return elemento creado o null en caso contrario.
     * @throws java.lang.Exception
     */
    public static Element createElement(Document xmlDom, Element element, String nodePath) throws Exception {
        Element nodeParent = getElement(xmlDom, nodePath);
        if (element != null && element instanceof Element && nodeParent != null) {
            return createElement(element, nodeParent);
        }
        return null;
    }

    /**
     * Crea nuevo elemento
     *
     * @param element elemento que se va crear (de tipo org.jdom.Element)
     * @param parentNode nodo en la que se va crear (de tipo org.jdom.Element)
     * @return elemento creado o null en caso contrario.
     */
    public static Element createElement(Element element, Element parentNode) {
        if (element != null && element instanceof Element && parentNode instanceof Element) {
            // Si el elemento a agregar no pertenece al documento a agregar            
            if (parentNode.getOwnerDocument() != element.getOwnerDocument()) {
                element = (Element) parentNode.getOwnerDocument().adoptNode(element);
            }
            parentNode.appendChild(element);
            return element;
        }
        return null;
    }

    /**
     * Crea nuevo elemento
     *
     * @param element elemento que se va crear (de tipo org.jdom.Element)
     * @param parentNode nodo en la que se va crear (de tipo org.jdom.Element)
     * @param position posición dentro de la lista de hijos (children)
     * @return elemento creado o null en caso contrario.
     */
    public static Element createElement(Element element, Element parentNode, int position) {
        if (element != null && element instanceof Element && parentNode instanceof Element) {
            // Si el elemento a agregar no pertenece al documento a agregar
            if (parentNode.getOwnerDocument() != element.getOwnerDocument()) {
                element = (Element) parentNode.getOwnerDocument().adoptNode(element);
            }
            if (parentNode.getChildNodes() == null || position > parentNode.getChildNodes().getLength()) {
                parentNode.appendChild(element);
            } else {
                Node nodeRef = null;
                if (parentNode.hasChildNodes()) {
                    List<Element> children = DomW3cParser.getChildren(parentNode);
                    if (children.size() > position) {
                        nodeRef = children.get(position);
                    }
                }
                parentNode.insertBefore(element, nodeRef);
            }
            return element;
        }
        return null;
    }

    /**
     * Crea nuevo elemento
     *
     * @param element elemento que se va crear (de tipo org.jdom.Element)
     * @param parentNode nodo en la que se va crear (de tipo org.jdom.Element)
     * @param nodeRef
     * @return elemento creado o null en caso contrario.
     */
    public static Element insertElementBefore(Element element, Element parentNode, Element nodeRef) {
        if (element != null && element instanceof Element && parentNode instanceof Element) {
            // Si el elemento a agregar no pertenece al documento a agregar
            if (parentNode.getOwnerDocument() != element.getOwnerDocument()) {
                element = (Element) parentNode.getOwnerDocument().adoptNode(element);
            }
            parentNode.insertBefore(element, nodeRef);
            return element;
        }
        return null;
    }

    /**
     * Borra un elemento
     *
     * @param xmlDom objeto DOM
     * @param nodePath nodo a borrar (en formato XPath)
     * @return true si se borrado correctamente o false en caso contrario.
     * @throws java.lang.Exception
     */
    public static boolean removeElement(Document xmlDom, String nodePath) throws Exception {
        Element node = getElement(xmlDom, nodePath);
        if (node == null) {
            return false;
        }
        node.getParentNode().removeChild(node);
        return true;
    }

    /**
     * Selecciona un elemento
     *
     * @param xmlDom objeto DOM
     * @param nodePath nodo a seleccionar (en formato XPath)
     * @return true si se borrado correctamente o false en caso contrario.
     * @throws java.lang.Exception
     */
    public static Element getElement(Document xmlDom, String nodePath) throws Exception {
        Node node = selectSingleNode(xmlDom, nodePath);
        if (node != null && node instanceof Element) {
            return (Element) node;
        }
        return null;
    }

    /**
     * Devuelve el valor del atributo de un nodo
     *
     * @param xmlDom
     * @param property nombre del atributo
     * @param nodePath nodo en la que se realizará la búsqueda (en formato
     * XPath)
     * @return valor del atributo o null en caso que no exista.
     * @throws java.lang.Exception
     */
    public static String getPropertyValue(Document xmlDom, String property, String nodePath) throws Exception {
        Element node = getElement(xmlDom, nodePath);
        if (node != null && node.hasAttribute(property)) {
            return getPropertyValue(property, node);
        }
        return null;
    }

    /**
     * Devuelve el valor del atributo de un nodo
     *
     * @param attribute nombre del atributo
     * @param node nodo en la que se realizará la búsqueda (de org.jdom.Element)
     * @return valor del atributo o null en caso que no exista.
     */
    public static String getPropertyValue(String attribute, Element node) {
        if (node != null && node.hasAttribute(attribute)) {
            return node.getAttribute(attribute);
        }
        return null;
    }

    /**
     * Crea o cambia la propiedad de un nodo
     *
     * @param xmlDom
     * @param value nuevo valor de la propiedad
     * @param property nombre de la propiedad
     * @param nodePath nodo en la que se realizará la búsqueda (en formato
     * XPath)
     * @return true si se completó la operación con éxito o false en caso
     * contrario.
     * @throws java.lang.Exception
     */
    public static boolean setPropertyValue(Document xmlDom, String value, String property, String nodePath) throws Exception {
        Element node = getElement(xmlDom, nodePath);
        if (node == null) {
            return false;
        }
        return setPropertyValue(value, property, node);
    }

    /**
     * Crea o cambia la propiedad de un nodo
     *
     * @param value nuevo valor de la propiedad
     * @param property nombre de la propiedad
     * @param node Objeto de tipo org.jdom.Element en la que se realizará la
     * búsqueda
     * @return true si se completó la operación con éxito o false en caso
     * contrario.
     */
    public static boolean setPropertyValue(String value, String property, Element node) {
        if (node != null) {
            node.setAttribute(property, value);
            return true;
        }
        return false;
    }

    /**
     * Busca y devuelve un elemento o nodo del objeto DOM
     *
     * @param document Objeto dom
     * @param nodePath path del elemento o nodo
     * @return elemento que cumple con el criterio de busqueda
     * @throws Exception
     */
    public static Node selectSingleNode(Document document, String nodePath) throws Exception {
        XPathExpression xpathExpr;
        if (!nodePath.startsWith("/")) {
            nodePath = "//" + nodePath;
        }
        XPath xpath = newXPath();
        xpathExpr = xpath.compile(nodePath);

        return (Node) xpathExpr.evaluate(document, XPathConstants.NODE);
    }

    /**
     * Busca y selecciona una lista de nodos del objeto DOM
     *
     * @param document objeto DOM
     * @param nodePath path del nodo o elemento buscado
     * @return lista de nodos que cumple con los criterios solicitados
     * @throws Exception
     */
    public static NodeList selectNodes(Document document, String nodePath) throws Exception {
        XPathExpression xpathExpr;
        if (!nodePath.startsWith("/")) {
            nodePath = "//" + nodePath;
        }
        XPath xpath = newXPath();
        xpathExpr = xpath.compile(nodePath);
        return (NodeList) xpathExpr.evaluate(document, XPathConstants.NODESET);
    }

    /**
     * Busca y selecciona una lista de nodos del objeto DOM
     *
     * @param document objeto DOM
     * @param nodePath path del nodo o elemento buscado
     * @return lista de nodos que cumple con los criterios solicitados
     * @throws Exception
     */
    public static List<Element> getElements(Document document, String nodePath) throws Exception {
        NodeList nodes = selectNodes(document, nodePath);
        List<Element> elements = new ArrayList();
        if (nodes == null) {
            return elements;
        }
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                elements.add((Element) nodes.item(i));
            }
        }
        return elements;
    }

    /**
     * Devuelve el texto xml de un objeto DOM
     *
     * @param content objeto DOM
     * @return texto xml
     * @throws javax.xml.transform.TransformerConfigurationException
     */
    public static String getXmlText(Node content) throws TransformerConfigurationException, TransformerException {
        return getXmlText(content, null);
    }

    /**
     * Devuelve el texto xml de un objeto DOM
     *
     * @param content objeto DOM
     * @param defaultCharSet
     * @return texto xml
     * @throws javax.xml.transform.TransformerConfigurationException
     */
    public static String getXmlText(Node content, String defaultCharSet) throws TransformerConfigurationException, TransformerException {
        return getXmlText(content, defaultCharSet, null);
    }

    /**
     * Devuelve el texto xml de un objeto DOM
     *
     * @param content objeto DOM
     * @param defaultCharSet
     * @param properties
     * @return texto xml
     * @throws javax.xml.transform.TransformerConfigurationException
     */
    public static String getXmlText(Node content, String defaultCharSet, Map<String,String> properties) throws TransformerConfigurationException, TransformerException {
        defaultCharSet = Fn.nvl(defaultCharSet, DEFAULTCHARSET);
        if (content == null){
            return null;
        }
        if (properties == null){
            properties = new HashMap();
        }
        String encoding;
        if (content instanceof Document) {
            encoding = ((Document) content).getInputEncoding();
        } else {
            encoding = content.getOwnerDocument().getInputEncoding();
        }
        encoding = Fn.nvl(encoding, defaultCharSet);
        String indent = Fn.nvl(properties.get("indent"),"no");
        String indent_amount = Fn.nvl(properties.get("indent_amount"),"10");

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        //Solo serializa un DOM ya leído, pero es la otra fábrica JAXP del
        //parser: sin acceso externo a DTD ni hojas de estilo (plan XMLSEC).
        for (String atributo : new String[]{XMLConstants.ACCESS_EXTERNAL_DTD,
            XMLConstants.ACCESS_EXTERNAL_STYLESHEET}) {
            try {
                transformerFactory.setAttribute(atributo, "");
            } catch (RuntimeException ex) {
                avisarNoAdmitida(transformerFactory, atributo, false);
            }
        }
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, indent);
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", indent_amount);
        transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
        DOMSource source = new DOMSource(content);
        StreamResult result = new StreamResult(new StringWriter());
        transformer.transform(source, result);

        return result.getWriter().toString();
    }
    
    /**
     * Devuelve la lista de hijos de un nodo
     * @param document document element
     * @param nodePath path del nodo padre.
     * @return lista de hijos del nodo solicitado.
     */
    public static List<Element> getChildren(Document document, String nodePath) {
        try {
            Element nodeParent = getElement(document, nodePath);
            return getChildren(nodeParent);
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return new ArrayList();
    }
    
    /**
     * Devuelve la lista de hijos de un nodo
     * @param nodeParent nodo padre.
     * @return lista de hijos del nodo solicitado.
     */
    public static List<Element> getChildren(Element nodeParent) {
        if (nodeParent == null) {
            return new ArrayList();
        }
        List<Element> children = new ArrayList();
        int childCount = nodeParent.getChildNodes().getLength();
        NodeList nodes = nodeParent.getChildNodes();
        for (int i = 0; i < childCount; i++) {
            if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                children.add((Element) nodes.item(i));
            }
        }
        return children;
    }

    /**
     * Devuelve un hijo en particular de un nodo 
     * @param nodeParent nodo padre.
     * @param nodeName nombre del nodo hijo.
     * @return un hijo en particular de un nodo.
     */
    public static Element getChild(Element nodeParent, String nodeName) {
        try {
            Element result = null;
            List<Element> children = getChildren(nodeParent);
            for (Element element : children) {
                if (element.getNodeName().equals(nodeName)) {
                    result = element;
                    break;
                }
            }
            return result;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Reemplaza un nodo hijo por otro nodo.
     * @param parentNode nodo padre.
     * @param newNode nuevo nodo.
     * @param oldNode nodo a reemplazar.
     * @return nuevo nodo 
     */
    public static Element replaceChild(Element parentNode, Element newNode, Element oldNode) {
        // Si el elemento a agregar no pertenece al documento a agregar            
        if (newNode.getOwnerDocument() != oldNode.getOwnerDocument()) {
            newNode = (Element) oldNode.getOwnerDocument().adoptNode(newNode);
        }
        parentNode.replaceChild(newNode, oldNode);
        return newNode;
    }
}

/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*        jenciso@javabeanstack.org
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
package org.javabeanstack.sifen.test.casos;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.crypto.Data;
import javax.xml.crypto.OctetStreamData;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.TransformService;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * Forma canónica y utilitarios XML de la red de equivalencia (plan JBSSIF, §2.2/§2.3, D8, A7).
 *
 * <p>La comparación se hace sobre la serialización <b>C14N exclusiva</b> (la misma que firma el DE)
 * obtenida con la API pública {@code javax.xml.crypto} del JDK: neutraliza el orden de atributos,
 * las declaraciones de espacio de nombres no usadas y la declaración XML, que dependen de la
 * versión de SAAJ y no de la librería. Solo usa el JDK: la compilan los dos lados del arnés A/B
 * (fork y {@code jbs-sifen}) y los tests del módulo.</p>
 */
public final class XmlCanonico {
    /** Espacio de nombres de XMLDSig. */
    public static final String NS_DSIG = "http://www.w3.org/2000/09/xmldsig#";

    private XmlCanonico() {
    }

    /**
     * Devuelve la forma C14N exclusiva (sin comentarios) del documento.
     *
     * @param xml documento completo (con o sin declaración XML).
     * @return bytes UTF-8 de la forma canónica.
     * @throws Exception si el XML no es válido o el JDK no ofrece la transformación.
     */
    public static byte[] canonico(String xml) throws Exception {
        TransformService c14n = TransformService.getInstance(CanonicalizationMethod.EXCLUSIVE, "DOM");
        c14n.init(null);
        Data salida = c14n.transform(new OctetStreamData(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))), null);
        try (InputStream in = ((OctetStreamData) salida).getOctetStream()) {
            return in.readAllBytes();
        }
    }

    /**
     * Forma canónica como texto.
     *
     * @param xml documento completo.
     * @return forma canónica decodificada en UTF-8.
     * @throws Exception si el XML no es válido.
     */
    public static String canonicoTexto(String xml) throws Exception {
        return new String(canonico(xml), StandardCharsets.UTF_8);
    }

    /**
     * Parser con espacios de nombres y endurecido contra XXE (RNF7).
     *
     * @param xml documento.
     * @return documento DOM.
     * @throws Exception si el XML no es válido.
     */
    public static Document parsear(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setExpandEntityReferences(false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(new InputSource(new StringReader(xml)));
    }

    /**
     * Texto del primer elemento con ese nombre local (cualquier espacio de nombres).
     *
     * @param xml documento.
     * @param nombreLocal nombre local del elemento.
     * @return texto o {@code null} si no existe.
     * @throws Exception si el XML no es válido.
     */
    public static String texto(String xml, String nombreLocal) throws Exception {
        NodeList nodos = parsear(xml).getElementsByTagNameNS("*", nombreLocal);
        return nodos.getLength() == 0 ? null : nodos.item(0).getTextContent();
    }

    /**
     * Arma el XML de entrada de un caso del catálogo a partir de la salida de {@code generarXml}:
     * toma el {@code rDE}, le quita {@code ds:Signature} y {@code gCamFuFD} (lo que la librería
     * vuelve a generar al firmar) y lo serializa indentado como documento propio.
     *
     * @param xmlGenerado salida de {@code DocumentoElectronico.generarXml} (sobre SOAP).
     * @return {@code rDE} sin firma, con declaración XML.
     * @throws Exception si la salida no tiene {@code rDE}.
     */
    public static String entradaDesdeSalida(String xmlGenerado) throws Exception {
        Document origen = parsear(xmlGenerado);
        Element rDE = (Element) origen.getElementsByTagNameNS("*", "rDE").item(0);
        if (rDE == null) {
            throw new IllegalArgumentException("La salida no tiene rDE");
        }
        Document destino = parsear("<x/>");
        Node copia = destino.importNode(rDE, true);
        destino.replaceChild(copia, destino.getDocumentElement());
        quitarHijos((Element) copia, NS_DSIG, "Signature");
        quitarHijos((Element) copia, null, "gCamFuFD");
        return serializarIndentado(destino);
    }

    private static void quitarHijos(Element padre, String ns, String nombreLocal) {
        NodeList hijos = padre.getChildNodes();
        for (int i = hijos.getLength() - 1; i >= 0; i--) {
            Node n = hijos.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && nombreLocal.equals(n.getLocalName())
                    && (ns == null || ns.equals(n.getNamespaceURI()))) {
                padre.removeChild(n);
            }
        }
    }

    private static String serializarIndentado(Document doc) throws Exception {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        t.setOutputProperty(OutputKeys.STANDALONE, "no");
        StringWriter sw = new StringWriter();
        t.transform(new DOMSource(doc), new StreamResult(sw));
        String s = sw.toString().replace("standalone=\"no\"", "").replace("?><", "?>\n<");
        return s.endsWith("\n") ? s : s + "\n";
    }
}

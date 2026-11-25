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
package org.javabeanstack.sifen.internal.util;

import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.helpers.SoapHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.soap.SOAPMessage;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Util encargado de validar las respuestas SOAP.
 */
public class ResponseUtil {
    private static final Logger LOGGER = LogManager.getLogger(ResponseUtil.class);

    /**
     * Devuelve el nodo principal de la respuesta (plan JBSSIF, §3.8, D33 Parte C).
     *
     * <p>Salta los nodos de texto del cuerpo (E2-04: una respuesta indentada daba
     * {@code NullPointerException}) y clasifica lo que no se puede interpretar: {@code soap:Fault} ⇒
     * {@code JBS-RESP-01} (código y texto del Fault en el detalle); cuerpo vacío o nodo distinto del
     * esperado ⇒ {@code JBS-RESP-02}.</p>
     *
     * <p>§8.7 (I8-03): si el nodo distinto es un {@code rRetEnviDe} con su {@code dCodRes} (SIFEN contesta así,
     * p. ej. 0160 «XML Mal Formado», a pedidos de otra operación), el {@code JBS-RESP-02} lleva ese código en
     * {@code getSifenResponseCode()} y el código y el {@code dMsgRes} en el detalle técnico.</p>
     *
     * @param soapMessage mensaje recibido.
     * @param nodeName    nombre local del nodo esperado ({@code rRetEnviDe}…).
     * @return el nodo.
     * @throws SifenException {@code JBS-RESP-01} o {@code JBS-RESP-02}.
     */
    public static Node getMainNode(SOAPMessage soapMessage, String nodeName) throws SifenException {
        if (soapMessage == null) {
            throw SifenException.ofResponse(ErrorCode.JBS_RESP_02, "sin mensaje SOAP; se esperaba " + nodeName, null);
        }
        SOAPBody soapBody;
        try {
            soapBody = soapMessage.getSOAPBody();
        } catch (SOAPException e) {
            throw SifenException.ofResponse(ErrorCode.JBS_RESP_02, "cuerpo SOAP ilegible: " + e.getClass().getName(), null);
        }
        if (soapBody == null) {
            throw SifenException.ofResponse(ErrorCode.JBS_RESP_02, "cuerpo SOAP nulo; se esperaba " + nodeName, null);
        }
        if (soapBody.hasFault()) {
            SOAPFault fault = soapBody.getFault();
            throw SifenException.ofResponse(ErrorCode.JBS_RESP_01, "SOAP Fault: código=" + fault.getFaultCode()
                    + ", texto=" + fault.getFaultString(), null);
        }
        Node node = soapBody.getFirstChild();
        while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
            node = node.getNextSibling();
        }
        if (node == null) {
            throw SifenException.ofResponse(ErrorCode.JBS_RESP_02, "cuerpo SOAP vacío; se esperaba " + nodeName, null);
        }
        String nombre = node.getLocalName() != null ? node.getLocalName() : node.getNodeName();
        if (!nodeName.equalsIgnoreCase(nombre)) {
            String detalle = "El nombre del nodo [" + nombre + "] no coincide con el nombre esperado [" + nodeName + "]";
            String codigo = null;
            if ("rRetEnviDe".equalsIgnoreCase(nombre) && node instanceof Element e) {
                // I8-03: el rechazo genérico de SIFEN (rProtDe/gResProc) conserva su código y su mensaje
                codigo = primerTexto(e, "dCodRes");
                String mensaje = primerTexto(e, "dMsgRes");
                if (codigo != null) {
                    detalle += ": dCodRes " + codigo + (mensaje == null ? "" : " «" + mensaje + "»");
                }
            }
            throw SifenException.ofResponse(ErrorCode.JBS_RESP_02, detalle, codigo);
        }
        return node;
    }

    /** Texto recortado del primer descendiente con ese nombre local; {@code null} si no hay o está vacío. */
    private static String primerTexto(Element raiz, String nombreLocal) {
        NodeList nodos = raiz.getElementsByTagNameNS("*", nombreLocal);
        if (nodos.getLength() == 0) {
            nodos = raiz.getElementsByTagName(nombreLocal);
        }
        if (nodos.getLength() == 0) {
            return null;
        }
        String texto = nodos.item(0).getTextContent();
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    /**
     * Serializa la respuesta, desescapa las entidades y la vuelve a leer (lo usa la consulta de DE,
     * cuyo {@code xContenDE} viene escapado).
     *
     * <p>§3.8 (E3-13, parcial): una entidad numérica hexadecimal ({@code &amp;#x41;}) o un {@code $}
     * ya no lanzan una excepción cruda, y si el texto desescapado no es XML (un {@code &lt;} legítimo
     * en {@code dMsgRes}) se avisa con {@code JBS-RESP-03} y se sigue con el mensaje original, que
     * conserva el {@code dCodRes}. El rediseño (leer {@code xContenDE} como texto y parsearlo aparte)
     * queda para la Fase 7.</p>
     *
     * <p>§8.7 (I8-04): solo se desescapa si el {@code xContenDE} trae el {@code rDE} <b>escapado</b> (texto, sin
     * elementos). SIFEN-test lo manda <b>en línea</b>: desescaparlo dejaba suelto el {@code &amp;} del
     * {@code dCarQR}, la relectura fallaba y quedaban un {@code SAAJ0511} SEVERE y un {@code JBS-RESP-03} WARN en
     * el log por cada consulta. En línea, o sin {@code xContenDE}, se devuelve el mensaje original tal cual.</p>
     *
     * @param soapMessage mensaje recibido.
     * @return el mensaje releído, o el original si no se pudo releer.
     */
    public static SOAPMessage parseSoapMessage(SOAPMessage soapMessage) {
        if (!contenidoEscapado(soapMessage)) {
            return soapMessage;
        }
        String xml = getXmlFromMessage(soapMessage, false);
        xml = SifenUtil.unescapeXML(xml)
                .replaceAll("<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>", "")
                .replaceAll(">[\\s\r\n]*<", "><");

        try {
            SOAPMessage releido = SoapHelper.parseSoapMessage(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            releido.getSOAPBody(); // fuerza la lectura: SAAJ parsea en diferido
            return releido;
        } catch (SOAPException | IOException e) {
            LOGGER.warn("{} No se pudo releer la respuesta desescapada ({}); se usa la original",
                    ErrorCode.JBS_RESP_03.getCode(), e.getClass().getName());
        }

        return soapMessage;
    }

    /**
     * Indica si el {@code xContenDE} de la respuesta viene escapado: existe, no tiene elementos hijos y su texto
     * trae un {@code <} (I8-04).
     */
    private static boolean contenidoEscapado(SOAPMessage soapMessage) {
        Document doc;
        try {
            doc = soapMessage.getSOAPPart();
            soapMessage.getSOAPBody(); // SAAJ parsea en diferido
        } catch (SOAPException | RuntimeException e) {
            return true; // sin cambio: lo decide la relectura, como antes
        }
        NodeList nodos = doc.getElementsByTagNameNS("*", "xContenDE");
        if (nodos.getLength() == 0) {
            nodos = doc.getElementsByTagName("xContenDE");
        }
        if (nodos.getLength() == 0) {
            return false;
        }
        Node contenido = nodos.item(0);
        for (Node n = contenido.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                return false;
            }
        }
        String texto = contenido.getTextContent();
        return texto != null && texto.indexOf('<') >= 0;
    }

    public static String getXmlFromMessage(SOAPMessage soapMessage, boolean removeSpaces) {
        final StringWriter sw = new StringWriter();
        try {
            TransformerFactory.newInstance().newTransformer().transform(new DOMSource(soapMessage.getSOAPPart()), new StreamResult(sw));
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }

        String xml = sw.toString();
        if (removeSpaces) {
            xml = xml.replaceAll(">[\\s\r\n]*<", "><");
        }
        return xml;
    }

    public static String getTextValue(Node node) {
        if (node == null)
            return null;

        Node firstChild = node.getFirstChild();
        if (firstChild == null) {
            LOGGER.warn("El nodo " + node.getNodeName() + " no contiene nodos hijos. Retornando");
            return null;
        }

        if (firstChild.getNodeType() != Node.TEXT_NODE) {
            LOGGER.warn("El primer nodo hijo del nodo " + node.getNodeName() + "no es un nodo de texto. Retornando null.");
            return null;
        }

        return firstChild.getTextContent();
    }

    public static LocalDate getDateValue(Node node) {
        String date = getTextValue(node);
        if (date != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return LocalDate.parse(date, formatter);
        }
        return null;
    }

    public static LocalDateTime getDateTimeValue(Node node) {
        String date = getTextValue(node);
        LocalDateTime parsedDate = null;

        if (date != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            try {
                parsedDate = LocalDateTime.parse(date, formatter);
            } catch (DateTimeParseException e) {
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                parsedDate = LocalDateTime.parse(date, formatter);
            }
        }
        return parsedDate;
    }
}
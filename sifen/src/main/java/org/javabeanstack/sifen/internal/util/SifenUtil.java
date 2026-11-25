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

import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.types.TTiDE;
import org.javabeanstack.sifen.internal.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Util con propósitos generales.
 */
public class SifenUtil {
    public static String bytesToHex(byte[] bytes) {
        char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("SHA-256");
            byte[] digest = md5.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(Integer.toHexString((b & 0xFF) | 0x100), 1, 3);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public static String leftPad(String string, char character, int length) {
        return String.format("%" + length + "s", string).replace(' ', character);
    }

    public static String generateDv(String ruc) {
        int baseMax = 11, k = 2, total = 0;

        if (ruc.equals("88888801")) {
            return "5";
        }

        for (int i = ruc.length() - 1; i >= 0; i--) {
            k = k > baseMax ? 2 : k;
            int n = Integer.parseInt(ruc.substring(i, i + 1));
            total += n * k;
            k++;
        }
        return String.valueOf((total % 11) > 1 ? 11 - (total % 11) : 0);
    }

    public static String generateRandomNumber() {
        return leftPad(String.valueOf(new Random().ints(1, 1, 999999999).distinct().toArray()[0]), '0', 9);
    }

    @SafeVarargs
    public static <T> T coalesce(T... items) {
        for (T i : items) if (i != null) return i;
        return null;
    }

    /**
     * Descripción de un código con opción «Otro» y texto libre (plan JBSSIF, §3.8, E3-23): con el código
     * «Otro» se escribe el texto que cargó el consumidor (el MT pide «informar» cuál es); con los
     * demás códigos, o si el texto falta, la descripción del código. La línea base hacía
     * {@code coalesce(enum.getDescripcion(), libre)} y el texto libre no salía nunca.
     *
     * @param esOtro       el código es el «Otro» del campo.
     * @param descripcion  descripción fija del código.
     * @param libre        texto libre del consumidor; puede ser {@code null}.
     * @return el texto a escribir.
     */
    public static String describeWithOther(boolean esOtro, String descripcion, String libre) {
        return esOtro && libre != null && !libre.isBlank() ? libre : descripcion;
    }

    public static String unescapeXML(final String xml) {
        Pattern xmlEntityRegex = Pattern.compile("&(#?)([^;]+);");
        StringBuffer unescapedOutput = new StringBuffer(xml.length());

        Matcher m = xmlEntityRegex.matcher(xml);
        Map<String, String> builtinEntities = buildBuiltinXMLEntityMap();
        String entity;
        String hashMark;
        String ent;
        int code;

        while (m.find()) {
            ent = m.group(2);
            hashMark = m.group(1);
            if (hashMark != null && hashMark.length() > 0) {
                // §3.8 (E3-13): &#x41; (hexadecimal) y una referencia inválida ya no lanzan
                // NumberFormatException; la inválida queda como estaba.
                try {
                    code = ent.startsWith("x") || ent.startsWith("X") ? Integer.parseInt(ent.substring(1), 16)
                            : Integer.parseInt(ent);
                    entity = Character.isValidCodePoint(code) ? new String(Character.toChars(code)) : "&#" + ent + ';';
                } catch (NumberFormatException e) {
                    entity = "&#" + ent + ';';
                }
            } else {
                entity = builtinEntities.get(ent);
                if (entity == null) {
                    entity = "&" + ent + ';';
                }
            }
            // §3.8 (E3-13): un $ o una \ en el texto no son referencias de grupo.
            m.appendReplacement(unescapedOutput, Matcher.quoteReplacement(entity));
        }
        m.appendTail(unescapedOutput);
        return unescapedOutput.toString();
    }

    private static Map<String, String> buildBuiltinXMLEntityMap() {
        Map<String, String> entities = new HashMap<>(5);
        entities.put("lt", "<");
        entities.put("gt", ">");
        entities.put("amp", "&");
        entities.put("apos", "'");
        entities.put("quot", "\"");
        return entities;
    }

    public static byte[] getByteArrayFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int length; (length = inputStream.read(buffer)) != -1; ) {
            result.write(buffer, 0, length);
        }
        return result.toByteArray();
    }

    public static String buildUrlParams(HashMap<String, String> params) {
        if (params.isEmpty()) return "";

        StringBuilder paramsString = new StringBuilder();
        for (Map.Entry<String, String> param : params.entrySet()) {
            paramsString.append(param.getKey()).append("=").append(param.getValue()).append("&");
        }
        return paramsString.substring(0, paramsString.length() - 1);
    }

    /**
     * Comprime el {@code rLoteDE} en un ZIP con una sola entrada {@code DE_ddMMyyyy.xml} (UTF-8), como pide
     * la recepción de lote del MT.
     *
     * <p>Plan JBSSIF, §4.7 (E3-10): se arma <b>en memoria</b>. La línea base escribía el lote, con los datos
     * de los clientes, en un {@code .zip} temporal que nunca borraba.</p>
     *
     * @param str el XML del lote.
     * @return los bytes del ZIP.
     * @throws IOException si falla la compresión.
     */
    public static byte[] compressXmlToZip(String str) throws IOException {
        String fileName = "DE_" + new SimpleDateFormat("ddMMyyyy").format(new Date());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            out.putNextEntry(new ZipEntry(fileName + ".xml"));
            out.write(str.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return bytes.toByteArray();
    }

    // ------------------------------------------------------------------ rDE firmado (plan JBSSIF, §4.1, §4.4)

    /** Espacio de nombres de XML-DSig. */
    private static final String NS_DSIG = "http://www.w3.org/2000/09/xmldsig#";

    /**
     * Datos de un {@code rDE} firmado leídos de su XML (plan JBSSIF, §4.1, §4.4): lo que necesita
     * {@code ISifenClient.sign}/{@code readSigned} para armar el {@code SignedDocument} sin volver a firmar.
     * Interno: no es contrato.
     *
     * @param xml           el {@code rDE} firmado; si el XML recibido tenía el {@code rDE} como raíz, es ese
     *                      texto sin tocar, y si venía dentro de otro (el sobre de {@code generarXml}), el
     *                      {@code rDE} serializado solo.
     * @param cdc           el {@code Id} del {@code DE}.
     * @param qrLink        el {@code dCarQR} de {@code gCamFuFD}.
     * @param digestValue   el {@code DigestValue} de la firma (Base64).
     * @param signatureDate el {@code dFecFirma}.
     * @param documentType  el {@code iTiDE}.
     * @param number        {@code EST-PTO-NUMERO}.
     * @param issuerRuc     RUC del emisor con DV ({@code dRucEm-dDVEmi}).
     */
    public record SignedRDE(String xml, String cdc, String qrLink, String digestValue, LocalDateTime signatureDate,
                            TTiDE documentType, String number, String issuerRuc) {
    }

    /**
     * Lee un {@code rDE} firmado sin recalcular, redondear ni firmar (plan JBSSIF, §4.1, §4.4): comprueba que
     * esté bien formado, que tenga {@code DE}, firma y QR, que el CDC sea coherente con sus datos y que el QR
     * lleve el CDC y el {@code DigestValue} de la firma (E0-02: un QR de otra firma no corresponde al DE). No
     * verifica la firma criptográficamente (eso es {@code verifySignature}).
     *
     * <p>Acepta el {@code rDE} como raíz —lo que devuelve {@link DocumentoElectronico#generateSignedXml}— o
     * dentro de otro XML, como el sobre {@code rEnviDe} de {@code generarXml} o un {@code rLoteDE}
     * (toma el primero).</p>
     *
     * <p>Códigos: vacío ⇒ {@code JBS-XML-01}; mal formado o con DOCTYPE ⇒ {@code JBS-XML-03}; sin
     * {@code DE} ⇒ {@code JBS-XML-02}; sin {@code Id}, firma, QR o {@code dFecFirma} ⇒ {@code JBS-XML-06};
     * un CDC o un QR que no corresponden a los datos o a la firma ⇒ {@code JBS-XML-07} (I4-05, §10.3,
     * carril F); un dato del documento que no se puede leer ⇒ el código de
     * {@code new DocumentoElectronico(xml)}.</p>
     *
     * @param xml el XML firmado.
     * @return los datos del {@code rDE}.
     * @throws SifenException si el XML no es un {@code rDE} firmado utilizable.
     */
    public static SignedRDE readSignedRDE(String xml) throws SifenException {
        Element rDE = parseRDE(xml);
        Element de = child(rDE, Constants.SIFEN_NS_URI, "DE");
        if (de == null) {
            throw SifenException.of(ErrorCode.JBS_XML_02, "rDE sin DE", null);
        }
        String cdc = de.getAttribute("Id");
        if (cdc.isBlank()) {
            throw SifenException.of(ErrorCode.JBS_XML_06, "DE sin Id", null, "el Id del DE");
        }
        Element signature = child(rDE, NS_DSIG, "Signature");
        String digest = signature == null ? null : firstText(signature, NS_DSIG, "DigestValue");
        if (digest == null || digest.isBlank()) {
            throw SifenException.of(ErrorCode.JBS_XML_06, "rDE sin firma (Signature/DigestValue)", null, "la firma (Signature)");
        }
        Element gCamFuFD = child(rDE, Constants.SIFEN_NS_URI, "gCamFuFD");
        String qr = gCamFuFD == null ? null : firstText(gCamFuFD, Constants.SIFEN_NS_URI, "dCarQR");
        if (qr == null || qr.isBlank()) {
            throw SifenException.of(ErrorCode.JBS_XML_06, "rDE sin gCamFuFD/dCarQR", null, "el QR (dCarQR)");
        }
        if (!cdc.equals(qrParam(qr, "Id")) || !bytesToHex(digest.trim().getBytes(StandardCharsets.US_ASCII)).equals(qrParam(qr, "DigestValue"))) {
            throw SifenException.of(ErrorCode.JBS_XML_07, "el QR no lleva el CDC o el DigestValue de la firma (E0-02)", null, "QR (dCarQR)");
        }

        // CDC coherente con los datos: lo recalcula la lectura del modelo (sus códigos JBS-XML-04/05)
        DocumentoElectronico leido = new DocumentoElectronico(xml);
        if (!cdc.equals(leido.getId())) {
            throw SifenException.of(ErrorCode.JBS_XML_07, "el CDC del DE (" + cdc + ") no corresponde a sus datos ("
                    + leido.getId() + ")", null, "CDC");
        }
        if (leido.getdFecFirma() == null) {
            throw SifenException.of(ErrorCode.JBS_XML_06, "DE sin dFecFirma", null, "la fecha de firma (dFecFirma)");
        }
        String number = leido.getgTimb().getdEst() + "-" + leido.getgTimb().getdPunExp() + "-" + leido.getgTimb().getdNumDoc();
        String ruc = leido.getgDatGralOpe().getgEmis().getdRucEm() + "-" + leido.getgDatGralOpe().getgEmis().getdDVEmi();
        String rdeXml = rDE == rDE.getOwnerDocument().getDocumentElement() ? xml : serialize(rDE);
        return new SignedRDE(rdeXml, cdc, qr, digest.trim(), leido.getdFecFirma(), leido.getgTimb().getiTiDE(), number, ruc);
    }

    /**
     * Lee el {@code rDE} de un XML con un parser endurecido (sin DOCTYPE ni entidades externas) para
     * insertarlo tal cual en un pedido (plan JBSSIF, §4.1): devuelve el elemento {@code rDE} —la raíz o el
     * primero que encuentre— de un documento nuevo; quien lo usa lo importa con {@code importNode}.
     *
     * @param xml el XML que contiene el {@code rDE}.
     * @return el elemento {@code rDE}.
     * @throws SifenException {@code JBS-XML-01} vacío, {@code JBS-XML-03} mal formado, {@code JBS-XML-02}
     *                        sin {@code rDE} ni {@code DE}.
     */
    public static Element parseRDE(String xml) throws SifenException {
        if (xml == null || xml.isBlank()) {
            throw SifenException.of(ErrorCode.JBS_XML_01, "XML " + (xml == null ? "nulo" : "en blanco"), null);
        }
        Document doc;
        try {
            doc = newSecureDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw SifenException.of(ErrorCode.JBS_XML_03, e.getClass().getName() + ": " + e.getMessage(), e);
        }
        Element root = doc.getDocumentElement();
        if ("rDE".equals(root.getLocalName()) && Constants.SIFEN_NS_URI.equals(root.getNamespaceURI())) {
            return root;
        }
        NodeList l = doc.getElementsByTagNameNS(Constants.SIFEN_NS_URI, "rDE");
        if (l.getLength() == 0) {
            throw SifenException.of(ErrorCode.JBS_XML_02, "sin nodo rDE", null);
        }
        return (Element) l.item(0);
    }

    /**
     * Parser XML endurecido, el único de la librería para leer XML (plan JBSSIF, E4-02 = F:E10-03, E0-09): sin
     * DOCTYPE ({@code disallow-doctype-decl}: corta las entidades externas, la DTD externa y el «billion
     * laughs»), {@code FEATURE_SECURE_PROCESSING}, sin entidades generales ni de parámetro externas, sin DTD
     * externa, sin XInclude ni expansión de entidades y con {@code ACCESS_EXTERNAL_DTD}/{@code SCHEMA} vacíos;
     * si algo igual llega a pedir un recurso, el {@code EntityResolver} lo niega. Con espacios de nombres.
     *
     * <p>El {@code ErrorHandler} es silencioso (M4-02): el parser no escribe {@code [Fatal Error]} en
     * {@code System.err}; el error llega como {@code SAXException} y lo traduce quien llama. Un
     * {@code DocumentBuilder} no es seguro entre hilos: uno nuevo por lectura.</p>
     *
     * @return un parser nuevo.
     * @throws ParserConfigurationException si la implementación de JAXP no admite {@code disallow-doctype-decl}
     *                                      (no se lee XML con un parser sin endurecer).
     */
    public static DocumentBuilder newSecureDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        optionalFeature(f, "http://xml.org/sax/features/external-general-entities", false);
        optionalFeature(f, "http://xml.org/sax/features/external-parameter-entities", false);
        optionalFeature(f, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        f.setXIncludeAware(false);
        f.setExpandEntityReferences(false);
        optionalAttribute(f, XMLConstants.ACCESS_EXTERNAL_DTD);
        optionalAttribute(f, XMLConstants.ACCESS_EXTERNAL_SCHEMA);
        DocumentBuilder b = f.newDocumentBuilder();
        b.setEntityResolver((publicId, systemId) -> {
            throw new SAXException("recurso externo no permitido: " + systemId);
        });
        b.setErrorHandler(SILENT);
        return b;
    }

    /** {@code ErrorHandler} que no escribe en {@code System.err}: los errores fatales y los de validación lanzan. */
    private static final ErrorHandler SILENT = new ErrorHandler() {
        @Override
        public void warning(SAXParseException e) {
            // sin salida: un aviso del parser no interrumpe la lectura
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            throw e;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            throw e;
        }
    };

    /** Rasgo que no todas las implementaciones de JAXP conocen: si no lo admite, lo cubre {@code disallow-doctype-decl}. */
    private static void optionalFeature(DocumentBuilderFactory f, String feature, boolean value) {
        try {
            f.setFeature(feature, value);
        } catch (ParserConfigurationException e) {
            // cubierto por disallow-doctype-decl, que es obligatorio
        }
    }

    /** Atributo JAXP 1.5 que un Xerces viejo no conoce: si no lo admite, lo cubre {@code disallow-doctype-decl}. */
    private static void optionalAttribute(DocumentBuilderFactory f, String attribute) {
        try {
            f.setAttribute(attribute, "");
        } catch (IllegalArgumentException e) {
            // cubierto por disallow-doctype-decl, que es obligatorio
        }
    }

    /**
     * Serializa un elemento sin declaración XML ni sangría: el texto que se guarda es el que se firmó (los
     * espacios de nombres que usa se declaran en él).
     *
     * @param element elemento.
     * @return el XML del elemento.
     * @throws SifenException {@code JBS-INT-02} si falla la serialización.
     */
    public static String serialize(Element element) throws SifenException {
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "no");
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            StringWriter sw = new StringWriter();
            t.transform(new DOMSource(element), new StreamResult(sw));
            return sw.toString();
        } catch (TransformerException e) {
            throw SifenException.of(ErrorCode.JBS_INT_02, "serialización del rDE: " + e.getClass().getName(), e);
        }
    }

    /** Primer hijo directo con ese nombre, o {@code null}. */
    private static Element child(Element parent, String ns, String localName) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && localName.equals(e.getLocalName()) && ns.equals(e.getNamespaceURI())) {
                return e;
            }
        }
        return null;
    }

    /** Texto del primer descendiente con ese nombre, o {@code null}. */
    private static String firstText(Element parent, String ns, String localName) {
        NodeList l = parent.getElementsByTagNameNS(ns, localName);
        return l.getLength() == 0 ? null : l.item(0).getTextContent();
    }

    /** Valor de un parámetro de la URL del QR, o {@code null}. */
    private static String qrParam(String qr, String name) {
        int q = qr.indexOf('?');
        for (String p : qr.substring(q + 1).split("&")) {
            int i = p.indexOf('=');
            if (i > 0 && p.substring(0, i).equals(name)) {
                return p.substring(i + 1);
            }
        }
        return null;
    }

    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }
}
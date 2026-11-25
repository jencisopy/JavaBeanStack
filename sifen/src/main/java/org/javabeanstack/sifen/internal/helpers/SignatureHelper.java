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
package org.javabeanstack.sifen.internal.helpers;

import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.internal.cert.CertificateStore;
import org.javabeanstack.sifen.internal.cert.CertificateSubjects;
import org.javabeanstack.sifen.core.beans.ValidezFirmaDigital;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.Constants;
import org.javabeanstack.sifen.internal.util.SifenUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.crypto.*;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.ParserConfigurationException;
import jakarta.xml.soap.SOAPElement;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper encargado de la firma digital de los documentos XML.
 *
 * <p>Delta del fork 2.0.0-SNAPSHOT (plan JBSSIF, §1.4.3; ficha 13 del informe de línea base, D9):
 * {@code getCertificateSubjects} ya no usa {@code sun.security.x509}: toma el certificado del
 * {@code KeyInfo}, lee los nombres alternativos con
 * {@code X509Certificate.getSubjectAlternativeNames()} ({@code directoryName}) y los parsea con el
 * nuevo {@code getAttributeFromDN} ({@code LdapName}); una {@code CertificateParsingException}
 * pasa a {@code RuntimeException}. Es la versión que compila en Java 25 sin
 * {@code --add-exports}.</p>
 *
 * <p>Cambio de jbs-sifen (plan JBSSIF, §3.7): {@code signDocument} toma la clave de
 * {@code CertificateStore} (M0-13, E3-15); el {@code dirName} del SAN se lee con
 * {@code CertificateSubjects}, que entiende el OID {@code 2.5.4.5} (E2-05: el sujeto del SAN ya trae
 * su número de documento), y {@code checkDocumentIssuer} es null-safe y mira {@code dDVEmi} (E0-07).
 * Una {@code CertificateParsingException} del SAN ya no lanza: el SAN ilegible se ignora.</p>
 *
 * <p>Cambio de jbs-sifen (plan JBSSIF, §3.8, D33 Parte C): si no se pueden crear las transformaciones de
 * la firma, {@code signDocument} lanza {@code JBS-FIRMA-02} (E3-17: la línea base imprimía a
 * {@code stderr} y seguía firmando sin {@code ENVELOPED}); las fallas
 * de la firma son {@code JBS-FIRMA-02}/{@code JBS-FIRMA-03}; {@code validateSignatureOrThrow} lanza
 * {@code JBS-XML-01}/{@code JBS-XML-03}/{@code JBS-FIRMA-01} cuando la validación no se puede hacer
 * (los {@code validateSignature*} de siempre devuelven esa falla en la {@link ValidezFirmaDigital}) y
 * un certificado sin número de documento del titular da el veredicto {@code JBS-FIRMA-05}.</p>
 */
public class SignatureHelper {
    private static final Logger LOGGER = LogManager.getLogger(SignatureHelper.class);
    // §10.3 (carril F, I10-14): la fábrica de firma y las transformaciones se crean en cada firma o
    // validación, sin estáticos: los métodos de instancia de XMLSignatureFactory no garantizan ser seguros
    // entre hilos y la fachada firma en paralelo para varias empresas. Una falla al crear las
    // transformaciones sigue siendo JBS-FIRMA-02 y no se firma con transformaciones incompletas (E3-17).

    /** Fábrica de firma XML del proveedor DOM, nueva en cada llamada. */
    private static XMLSignatureFactory factory() {
        return XMLSignatureFactory.getInstance();
    }

    /** Transformaciones de la firma de SIFEN ({@code ENVELOPED} y C14N exclusiva). */
    private static List<Transform> transforms(XMLSignatureFactory f)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        List<Transform> t = new ArrayList<>(2);
        t.add(f.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
        t.add(f.newTransform(CanonicalizationMethod.EXCLUSIVE, (TransformParameterSpec) null));
        return Collections.unmodifiableList(t);
    }

    public static SignedInfo signDocument(SifenConfig sifenConfig, SOAPElement signatureParentNode, String signedNodeId) throws SifenException {
        XMLSignatureFactory _xmlSignatureFactory = factory();
        List<Transform> transforms;
        try {
            transforms = transforms(_xmlSignatureFactory);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            // E3-17: no se imprime a stderr ni se firma con transformaciones incompletas.
            LOGGER.error("{} No se pueden crear las transformaciones de la firma: {}", ErrorCode.JBS_FIRMA_02.getCode(),
                    e.getClass().getName());
            throw SifenException.of(ErrorCode.JBS_FIRMA_02, "transformaciones de la firma: " + e.getClass().getName(), e);
        }
        try {
            Reference ref = _xmlSignatureFactory.newReference("#" + signedNodeId,
                    _xmlSignatureFactory.newDigestMethod(DigestMethod.SHA256, null),
                    transforms, null, null);

            SignedInfo signedInfo = _xmlSignatureFactory.newSignedInfo(
                    _xmlSignatureFactory.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null),
                    _xmlSignatureFactory.newSignatureMethod(Constants.RSA_SHA256, null),
                    Collections.singletonList(ref)
            );

            // §3.7 (M0-13, E3-15): el certificado lo carga una vez el almacén, con la primera entrada
            // que tiene clave privada (la línea base tomaba el primer alias aunque no la tuviera).
            SSLContextHelper.getCertificateKeyStore(sifenConfig);
            CertificateStore.Material material = SSLContextHelper.material(sifenConfig);
            X509Certificate certificate = material.certificate();

            KeyInfoFactory keyInfoFactory = _xmlSignatureFactory.getKeyInfoFactory();
            X509Data x509Data = keyInfoFactory.newX509Data(Collections.singletonList(certificate));
            KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(x509Data));

            XMLSignature signature = _xmlSignatureFactory.newXMLSignature(signedInfo, keyInfo);
            PrivateKey privateKey = material.privateKey();

            DOMSignContext signatureContext = new DOMSignContext(privateKey, signatureParentNode);
            signature.sign(signatureContext);

            return signedInfo;
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw SifenException.of(ErrorCode.JBS_FIRMA_02, e.getClass().getName() + ": " + e.getMessage(), e);
        } catch (XMLSignatureException | MarshalException e) {
            throw SifenException.of(ErrorCode.JBS_FIRMA_03, e.getClass().getName() + ": " + e.getMessage(), e);
        }
    }

    public static ValidezFirmaDigital validateSignature(String xml, String type) {
        try {
            return validateSignatureOrThrow(xml, type);
        } catch (SifenException e) {
            return ValidezFirmaDigital.ofError(e);
        }
    }

    public static ValidezFirmaDigital validateSignature(File xml) {
        try {
            return validateSignatureOrThrow(xml);
        } catch (SifenException e) {
            return ValidezFirmaDigital.ofError(e);
        }
    }

    /**
     * Valida la firma de un XML (texto o ruta de archivo) y lanza cuando la validación no se puede
     * hacer (§3.8).
     *
     * @param xml  el XML ({@code type} = {@code "XML"}) o la ruta del archivo.
     * @param type {@code "XML"} o {@code "PATH"}.
     * @return el veredicto.
     * @throws SifenException {@code JBS-XML-01} si el XML está vacío, {@code JBS-FIRMA-01} si la ruta no
     *                        sirve, {@code JBS-XML-03} si el XML está mal formado.
     */
    public static ValidezFirmaDigital validateSignatureOrThrow(String xml, String type) throws SifenException {
        boolean esTexto = "XML".equals(type);
        if (xml == null || xml.isBlank()) {
            throw SifenException.of(esTexto ? ErrorCode.JBS_XML_01 : ErrorCode.JBS_FIRMA_01,
                    esTexto ? "XML vacío" : "ruta vacía", null);
        }
        if (!esTexto) {
            return validateSignatureOrThrow(new File(xml));
        }
        // §4.7 (E4-01): el XML se valida en memoria. La línea base lo escribía —con los datos del
        // documento— en un <uuid>.xml de java.io.tmpdir que nunca borraba.
        try {
            return validateParsed(SifenUtil.newSecureDocumentBuilder().parse(new InputSource(new StringReader(xml))));
        } catch (SAXException e) {
            throw SifenException.of(ErrorCode.JBS_XML_03, e.getClass().getName() + ": " + e.getMessage(), e);
        } catch (IOException e) {
            throw SifenException.of(ErrorCode.JBS_INT_00, e.getClass().getName() + ": " + e.getMessage(), e);
        } catch (ParserConfigurationException e) {
            throw SifenException.of(ErrorCode.JBS_INT_00, e.getClass().getName() + ": " + e.getMessage(), e);
        }
    }

    // E4-02 (= F:E10-03, E0-09): el texto y el archivo se leen con el parser endurecido único,
    // SifenUtil.newSecureDocumentBuilder(); la línea base resolvía entidades externas (XXE) en el XML de un tercero.

    /**
     * Valida la firma del XML de un archivo y lanza cuando la validación no se puede hacer (§3.8).
     *
     * @param xml archivo.
     * @return el veredicto.
     * @throws SifenException {@code JBS-FIRMA-01} si el archivo no existe o no se puede leer,
     *                        {@code JBS-XML-03} si está mal formado.
     */
    public static ValidezFirmaDigital validateSignatureOrThrow(File xml) throws SifenException {
        if (xml == null || !xml.isFile() || !xml.canRead()) {
            throw SifenException.of(ErrorCode.JBS_FIRMA_01, xml == null ? "archivo nulo" : "archivo: " + xml, null);
        }
        Document document;
        try {
            document = SifenUtil.newSecureDocumentBuilder().parse(xml);
        } catch (SAXException e) {
            throw SifenException.of(ErrorCode.JBS_XML_03, e.getClass().getName() + ": " + e.getMessage(), e);
        } catch (IOException e) {
            throw SifenException.of(ErrorCode.JBS_FIRMA_01, e.getClass().getName() + ": " + e.getMessage(), e);
        } catch (ParserConfigurationException e) {
            throw SifenException.of(ErrorCode.JBS_INT_00, e.getClass().getName() + ": " + e.getMessage(), e);
        }
        return validateParsed(document);
    }

    /** Valida la firma de un documento ya leído (§4.7: el mismo camino para el texto y para el archivo). */
    private static ValidezFirmaDigital validateParsed(Document document) throws SifenException {
        try {
            // Find Signature
            NodeList signatureNodes = document.getElementsByTagName("Signature");
            if (signatureNodes.getLength() == 0) {
                return ValidezFirmaDigital.create(false, "No se encontró la firma digital en " +
                        "el Documento Electrónico.");
            }

            // Get signed element from document
            DOMValidateContext valContext = new DOMValidateContext(new X509KeySelector(), signatureNodes.item(0));
            NodeList DENodes = document.getElementsByTagName("DE");
            if (DENodes.getLength() > 0) {
                valContext.setIdAttributeNS((Element) DENodes.item(0), null, "Id");
            } else {
                return ValidezFirmaDigital.create(false, "No se encontró el nodo 'DE' en el " +
                        "Documento Electrónico.");
            }
            XMLSignature signature = factory().unmarshalXMLSignature(valContext);

            // Get subjects from certificate for further validation
            List<ValidezFirmaDigital.SujetoCertificado> certificateSubjects = getCertificateSubjects(signature.getKeyInfo());

            // Validate the Signature
            boolean isValid = signature.validate(valContext);
            if (!isValid) {
                return ValidezFirmaDigital.create(false, "La firma digital es inválida.",
                        certificateSubjects);
            }

            return checkDocumentIssuer(document, certificateSubjects);
        } catch (MarshalException | XMLSignatureException e) {
            // la firma del XML no se puede interpretar: es un veredicto, no una falla de la librería
            LOGGER.debug("No se pudo interpretar la firma: {}", e.getClass().getName());
            return ValidezFirmaDigital.create(false, "Ocurrió un error al validar la firma " +
                    "digital del Documento Electrónico.");
        }
    }

    private static ValidezFirmaDigital checkDocumentIssuer(Document document, List<ValidezFirmaDigital.SujetoCertificado> certificateSubjects) {
        // Get Issuer RUC from Electronic Document
        NodeList dRucEmNodes = document.getElementsByTagName("dRucEm");
        if (dRucEmNodes.getLength() == 0) {
            return ValidezFirmaDigital.create(false, "No se encontró el nodo 'dRucEm' en " +
                    "el Documento Electrónico.", certificateSubjects);
        }

        NodeList dDVEmiNodes = document.getElementsByTagName("dDVEmi");
        if (dDVEmiNodes.getLength() == 0) { // E0-07: la línea base volvía a mirar dRucEmNodes
            return ValidezFirmaDigital.create(false, "No se encontró el nodo 'dDVEmi' en " +
                    "el Documento Electrónico.", certificateSubjects);
        }

        String issuerRuc = dRucEmNodes.item(0).getTextContent();
        String issuerDv = dDVEmiNodes.item(0).getTextContent();

        // §3.8 (JBS-FIRMA-05): ningún sujeto del certificado trae el número de documento del titular.
        boolean algunNumero = false;
        for (ValidezFirmaDigital.SujetoCertificado subject : certificateSubjects) {
            algunNumero |= subject.getNumeroDocumento() != null;
        }
        if (!algunNumero) {
            return ValidezFirmaDigital.ofCode(ErrorCode.JBS_FIRMA_05, certificateSubjects);
        }

        // E2-05: un sujeto sin número de documento no coincide (la línea base daba NullPointerException).
        for (ValidezFirmaDigital.SujetoCertificado subject : certificateSubjects) {
            if (CertificateSubjects.matchesIssuer(subject.getNumeroDocumento(), issuerRuc, issuerDv)) {
                return ValidezFirmaDigital.create(true, certificateSubjects);
            }
        }

        return ValidezFirmaDigital.create(false, "El RUC emisor del Documento Electrónico " +
                "no coincide con el encontrado en la firma digital.", certificateSubjects);
    }

    private static List<ValidezFirmaDigital.SujetoCertificado> getCertificateSubjects(KeyInfo keyInfo) {
        List<ValidezFirmaDigital.SujetoCertificado> certificateSubjects = new ArrayList<>();

        // Get certificate from Electronic Document
        X509Certificate certificate = null;
        for (Object o : keyInfo.getContent()) {
            if (o instanceof X509Data) {
                X509Data x509Data = (X509Data) o;
                for (Object c : x509Data.getContent()) {
                    if (c instanceof X509Certificate) {
                        X509Certificate cert = (X509Certificate) c;
                        certificate = cert;
                        break;
                    }
                }
            }
        }
        if (certificate == null) return certificateSubjects;

        // Get main subject information from certificate
        try {
            String subject = certificate.getSubjectDN().getName();

            certificateSubjects.add(ValidezFirmaDigital.SujetoCertificado.create(
                    getAttributeFromSubject(subject, "SERIALNUMBER"),
                    SifenUtil.coalesce(getAttributeFromSubject(subject, "CN"), getAttributeFromSubject(subject, "O"))
            ));
        } catch (Exception ignored) {
        }

        // Get alternatives subjects from certificate. E2-05: el dirName viene en RFC 2253 con el OID
        // 2.5.4.5 en hexadecimal; CertificateSubjects lo reinterpreta y devuelve el SERIALNUMBER.
        for (CertificateSubjects.CertificateSubject sujeto : CertificateSubjects.ofSan(certificate)) {
            certificateSubjects.add(ValidezFirmaDigital.SujetoCertificado.create(sujeto.documentNumber(), sujeto.name()));
        }

        return certificateSubjects;
    }

    private static String getAttributeFromSubject(String subject, String attributeName) {
        Pattern pattern = Pattern.compile("(?<=" + attributeName + "=)[\\w\\s-]+");
        Matcher matcher = pattern.matcher(subject);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private static class X509KeySelector extends KeySelector {
        public KeySelectorResult select(KeyInfo keyInfo, Purpose purpose, AlgorithmMethod method,
                                        XMLCryptoContext context) throws KeySelectorException {
            X509Certificate certificate = getCertificate(keyInfo);
            if (certificate != null) {
                final PublicKey key = certificate.getPublicKey();
                if (key.getAlgorithm().equalsIgnoreCase("RSA") && method.getAlgorithm().equalsIgnoreCase(Constants.RSA_SHA256)) {
                    return () -> key;
                }
            }
            throw new KeySelectorException("No key was found");
        }

        public static X509Certificate getCertificate(KeyInfo keyInfo) {
            for (Object value : keyInfo.getContent()) {
                XMLStructure info = (XMLStructure) value;
                if (!(info instanceof X509Data)) continue;

                X509Data x509Data = (X509Data) info;
                for (Object certificate : x509Data.getContent()) {
                    if (!(certificate instanceof X509Certificate)) continue;

                    return ((X509Certificate) certificate);
                }
            }
            return null;
        }
    }
}
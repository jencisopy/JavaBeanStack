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
package org.javabeanstack.sifen.core.beans;

import org.javabeanstack.sifen.Sifen;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.Operation;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.fields.request.de.*;
import org.javabeanstack.sifen.core.types.TTiDE;
import org.javabeanstack.sifen.core.validation.ConfigValidator;
import org.javabeanstack.sifen.core.validation.DEValidator;
import org.javabeanstack.sifen.internal.Constants;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.internal.helpers.SignatureHelper;
import org.javabeanstack.sifen.internal.helpers.SoapHelper;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.response.SifenObjectFactory;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.javabeanstack.sifen.internal.facade.Operations;
import org.javabeanstack.sifen.internal.util.SifenUtil;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import jakarta.xml.soap.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.javabeanstack.sifen.internal.Constants.SIFEN_CURRENT_VERSION;

/**
 * Clase que representa un Documento Electrónico, incluyendo todos los campos disponibles en un DE en formato XML.
 */
public class DocumentoElectronico extends SifenObjectBase {
    private String Id;
    private String dDVId;
    private LocalDateTime dFecFirma;    // se debe serializar en el formato: yyyy-mm-ddThh:mi:ss
    private short dSisFact;             // sistema que factura (1. Sistema del Cliente, 2. Sistema de facturación gratuita de la SET)

    private TgOpeDE gOpeDE;             // campos de operación del documento electrónico
    private TgTimb gTimb;               // campos del timbrado del documento
    private TdDatGralOpe gDatGralOpe;   // datos generales de la operación
    private TgDtipDE gDtipDE;
    private TgTotSub gTotSub;
    private TgCamGen gCamGen;
    private List<TgCamDEAsoc> gCamDEAsocList;

    private String enlaceQR;
    /** {@code rDE} firmado por {@link #generateSignedXml(SifenConfig)}; {@code null} si se volvió a firmar por el camino heredado. */
    private String signedXml;
    private static final Logger LOGGER = LogManager.getLogger(DocumentoElectronico.class);

    /**
     * Constructor base del Documento Electrónico.
     */
    public DocumentoElectronico() {
    }

    /**
     * Construye un Documento Electrónico en base a un XML.
     *
     * <p>Lee con el parser endurecido de la librería: un XML con DOCTYPE se rechaza con {@code JBS-XML-03}
     * (E4-02). El redondeo SEDECO se deduce del XML: {@code dRedon} ≠ 0 ⇒ con redondeo (§4.2, I4-09). Releer
     * y regenerar sin cambios da el mismo documento; quien lo lee, le cambia ítems o importes y lo regenera
     * fija antes el redondeo que corresponda con {@link #setRedondeo(Boolean)}, porque un DE redondeado cuyo
     * total era múltiplo de 50 Gs (o de 0,50) se lee como «sin redondeo».</p>
     *
     * @param xml XML a ser utilizado para la generación del Documento Electrónico.
     * @throws SifenException Si el XML tiene un formato inválido o, si algún dato necesario para la generación del DE
     *                        no pudo ser encontrado.
     */
    public DocumentoElectronico(String xml) throws SifenException {
        Operations.execute(Operation.READ_XML, this::getId, () -> {
            leer(xml);
            try {
                this.calcularCDC();
            } catch (SifenException e) {
                // §3.8: al XML le falta un dato del CDC (la línea base: i402). La NPE con que se detecta
                // queda solo en el detalle: es un dato de entrada, no una falla interna.
                throw SifenException.of(ErrorCode.JBS_XML_04, e.getTechnicalDetail(), null);
            }
            return null;
        });
    }

    /**
     * Construye un Documento Electrónico en base a un XML, con el CDC recibido.
     *
     * @param xml         XML a ser utilizado para la generación del Documento Electrónico.
     * @param CDCrecibido CDC que se le asigna en lugar de calcularlo.
     * @throws SifenException Si el XML tiene un formato inválido (§3.8: {@code JBS-XML-*}).
     */
    public DocumentoElectronico(String xml, String CDCrecibido) throws SifenException {
        Operations.execute(Operation.READ_XML, () -> CDCrecibido, () -> {
            leer(xml);
            this.obtenerCDC(CDCrecibido);
            return null;
        });
    }

    /**
     * Lee el XML en este objeto (§3.8): XML vacío ⇒ {@code JBS-XML-01}, mal formado ⇒
     * {@code JBS-XML-03} (antes {@code i403}), sin nodo {@code DE} ⇒ {@code JBS-XML-02} (antes
     * {@code NullPointerException}); un valor no convertible lo traduce la fachada a
     * {@code JBS-XML-05} con la ruta del nodo.
     */
    private void leer(String xml) throws SifenException {
        if (xml == null || xml.isBlank()) {
            throw SifenException.of(ErrorCode.JBS_XML_01, "XML " + (xml == null ? "nulo" : "en blanco"), null);
        }
        xml = xml.replaceAll(">[\\s\r\n]*<", "><");

        // Parseamos el xml
        Document xmlDocument;
        try {
            // E4-02 (= F:E10-03, E0-09): parser endurecido, sin DOCTYPE ni entidades externas (XXE)
            xmlDocument = SifenUtil.newSecureDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw SifenException.of(ErrorCode.JBS_XML_03, e.getClass().getName() + ": " + e.getMessage(), e);
        }

        // Obtenemos el nodo principal
        Node mainNode = xmlDocument.getElementsByTagName("DE").item(0);
        if (mainNode == null) {
            throw SifenException.of(ErrorCode.JBS_XML_02, "sin nodo DE", null);
        }

        SifenObjectFactory.getFromNode(mainNode, this);

        // §4.4 (E0-02): el QR del XML firmado (gCamFuFD/dCarQR, hermano de DE) es el del documento leído;
        // antes getEnlaceQR() quedaba en null hasta volver a firmar
        NodeList qr = xmlDocument.getElementsByTagNameNS(Constants.SIFEN_NS_URI, "dCarQR");
        if (qr.getLength() > 0) {
            this.enlaceQR = qr.item(0).getTextContent();
        }
    }

    /**
     * Calcula el CDC del Documento Electrónico en cuestión y lo retorna. Además de lo anterior, también establece los
     * valores en el lugar correspondiente dentro del objeto.
     *
     * @return CDC calculado del Documento Electrónico.
     * @throws SifenException Si alguno de los campos necesarios para el cálculo del CDC no se encuentra.
     */
    public String obtenerCDC() throws SifenException {
        return Operations.execute(Operation.COMPUTE_CDC, null, this::calcularCDC);
    }

    /** Cálculo del CDC, sin el envoltorio de la fachada (lo usan la lectura del XML y {@code setupDE}). */
    private String calcularCDC() throws SifenException {
        // I3-49 (§3.8 ciclo 2): un dato faltante es del documento (JBS-ENT-07, DE_CONTENT, legado i402),
        // no un error interno; se nombran los que faltan. La línea base concatenaba «null» en el CDC en los
        // de texto y el cálculo del DV lanzaba NumberFormatException (I3-62); en los demás daba i402.
        java.util.List<String> faltan = faltantesDelCDC();
        if (!faltan.isEmpty()) {
            throw SifenException.of(ErrorCode.JBS_ENT_07, "faltan para el CDC: " + faltan, null, String.join(", ", faltan));
        }
        // Se intenta la generación del CDC
        String CDC;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            CDC = SifenUtil.leftPad(String.valueOf(this.getgTimb().getiTiDE().getVal()), '0', 2) +
                    SifenUtil.leftPad(this.getgDatGralOpe().getgEmis().getdRucEm(), '0', 8) +
                    this.getgDatGralOpe().getgEmis().getdDVEmi() +
                    this.getgTimb().getdEst() +
                    this.getgTimb().getdPunExp() +
                    this.getgTimb().getdNumDoc() +
                    this.getgDatGralOpe().getgEmis().getiTipCont().getVal() +
                    this.getgDatGralOpe().getdFeEmiDE().format(formatter) +
                    this.getgOpeDE().getiTipEmi().getVal() +
                    this.getgOpeDE().getdCodSeg();
        } catch (Exception e) {
            // no debería ocurrir: faltantesDelCDC ya revisó cada dato
            throw SifenException.of(ErrorCode.JBS_ENT_07, "Se produjo un error al generar el CDC: " + e.getClass().getName(),
                    null, "un dato del CDC no se pudo leer");
        }

        // Se setean los valores generados en sus lugares correspondientes dentro de la clase
        this.dDVId = SifenUtil.generateDv(CDC);
        this.Id = CDC + this.dDVId;

        return this.Id;
    }

    /** Datos del CDC que faltan, con su nombre del MT, en el orden en que forman el CDC (I3-49). */
    private java.util.List<String> faltantesDelCDC() {
        java.util.List<String> f = new java.util.ArrayList<>();
        TgTimb t = this.getgTimb();
        TdDatGralOpe g = this.getgDatGralOpe();
        TgEmis em = g == null ? null : g.getgEmis();
        TgOpeDE o = this.getgOpeDE();
        if (t == null || t.getiTiDE() == null) f.add("iTiDE (tipo de documento)");
        if (em == null || em.getdRucEm() == null) f.add("dRucEm (RUC del emisor)");
        if (em == null || em.getdDVEmi() == null) f.add("dDVEmi (DV del emisor)");
        if (t == null || t.getdEst() == null) f.add("dEst (establecimiento)");
        if (t == null || t.getdPunExp() == null) f.add("dPunExp (punto de expedición)");
        if (t == null || t.getdNumDoc() == null) f.add("dNumDoc (número del documento)");
        if (em == null || em.getiTipCont() == null) f.add("iTipCont (tipo de contribuyente)");
        if (g == null || g.getdFeEmiDE() == null) f.add("dFeEmiDE (fecha de emisión)");
        if (o == null || o.getiTipEmi() == null) f.add("iTipEmi (tipo de emisión)");
        if (o == null || o.getdCodSeg() == null) f.add("dCodSeg (código de seguridad)");
        return f;
    }

    //    INICIO CAMBIO AM
//    se agrego un overload del metodo obtener CDC para que reciba el parametro ReceivedCDC
    public String obtenerCDC(String ReceivedCDC) throws SifenException {
        this.Id = ReceivedCDC;
        return this.Id;
    }
//            FIN CAMBIO

    /**
     * Genera un XML completo en base al Documento Electrónico actual.
     *
     * @return XML del Documento Electrónico.
     * @throws SifenException Si la configuración de Sifen no fue establecida o, si algún dato necesario para la
     *                        generación del XML no pudo ser encontrado o, si la firma digital del DE falla.
     */
    public String generarXml(GenerationCtx generationCtx) throws SifenException {
        return Operations.execute(Operation.GENERATE_XML, this::getId,
                () -> this.generarXml(generationCtx, generationCtx.getSifenConfig()));
    }

    /**
     * Genera un XML completo en base al Documento Electrónico actual.
     *
     * @param sifenConfig Configuración de Sifen a ser utilizada para la generación del XML.
     * @return XML del Documento Electrónico.
     * @throws SifenException Si la configuración de Sifen no fue establecida o, si algún dato necesario para la
     *                        generación del XML no pudo ser encontrado o, si la firma digital del DE falla.
     */
    public String generarXml(GenerationCtx generationCtx, SifenConfig sifenConfig) throws SifenException {
        return Operations.execute(Operation.GENERATE_XML, this::getId, () -> generar(generationCtx, sifenConfig));
    }

    /** Cuerpo de {@code generarXml} (§3.8: nunca devuelve {@code null}). */
    private String generar(GenerationCtx generationCtx, SifenConfig sifenConfig) throws SifenException {
        validarAntesDeFirmar(generationCtx, sifenConfig);
        try {
            SOAPMessage message = this.setupSOAPElements(generationCtx, 1, sifenConfig);
            return ResponseUtil.getXmlFromMessage(message, true);
        } catch (SOAPException e) {
            // §3.8 (M3-13, inventario #68): la línea base imprimía la traza y devolvía null
            throw SifenException.of(ErrorCode.JBS_INT_01, "Se produjo un error al generar el XML: "
                    + e.getClass().getName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Configuración presente y validación previa (plan JBSSIF, §3.5, D20): con errores lanza
     * {@code SifenValidationException} (una {@code SifenException}) y no llega a armar el XML; con
     * validacionPrevia = false, la línea base. §3.7 (D33 Parte B): tres etapas sin mezclar resultados, lanza
     * la primera con errores — configuración y certificado (i101), contenido (i405) y coherencia DE ↔
     * certificado (JBS-CERT-11).
     */
    private void validarAntesDeFirmar(GenerationCtx generationCtx, SifenConfig sifenConfig) throws SifenException {
        if (sifenConfig == null) {
            // §3.8: JBS-CFG-01 (antes i101 con texto libre)
            ConfigValidator.standard().require(null);
        }
        if (sifenConfig.isPreValidationEnabled()) {
            ConfigValidator.standard().require(sifenConfig);
            DEValidator.standard().validate(this, generationCtx).throwIfInvalid();
            ConfigValidator.standard().requireConsistency(this, sifenConfig);
        }
    }

    /**
     * Arma el {@code rDE}, lo firma <b>una</b> vez y devuelve su XML: el que se guarda y el que se envía tal
     * cual (plan JBSSIF, §4.1, E0-01, M0-01). Es el paso «firmar» de la emisión en dos pasos; lo usa
     * {@code ISifenClient.sign} (§10.3) y la aplicación no lo llama directamente.
     *
     * <p>Valida como {@code generarXml} (según {@code isPreValidationEnabled()}), calcula el CDC, arma el QR
     * desde esta firma y conserva el resultado en {@link #getSignedXml()} y {@link #getEnlaceQR()}. A
     * diferencia de {@code generarXml}, devuelve el {@code rDE} solo —con sus espacios de nombres
     * declarados—, sin el sobre {@code rEnviDe} ni el {@code dId}, y sin tocar los espacios entre etiquetas:
     * el texto es exactamente el que se firmó. No completa {@code dFecFirma} ni {@code dCodSeg}: los pone
     * quien llama.</p>
     *
     * @param sifenConfig configuración con el certificado, el CSC y el ambiente.
     * @return el {@code rDE} firmado (UTF-8, sin declaración XML).
     * @throws SifenException {@code JBS-CFG-01} sin configuración; {@code SifenValidationException} si la
     *                        validación previa falla; {@code JBS-FIRMA-*} si falla la firma o el QR.
     */
    public String generateSignedXml(SifenConfig sifenConfig) throws SifenException {
        return Operations.execute(Operation.SIGN, this::getId, () -> {
            if (sifenConfig == null) {
                ConfigValidator.standard().require(null);   // JBS-CFG-01
            }
            GenerationCtx ctx = GenerationCtx.getDefaultFromConfig(sifenConfig);
            validarAntesDeFirmar(ctx, sifenConfig);
            try {
                SOAPMessage message = SoapHelper.createSoapMessage();
                // contenedor sin espacio de nombres (como el rLoteDE del lote): el rDE declara el suyo
                SOAPElement contenedor = message.getSOAPBody().addChildElement("xDE");
                this.setupDE(ctx, contenedor, sifenConfig);
                Element rDE = null;
                for (Node n = contenedor.getFirstChild(); n != null && rDE == null; n = n.getNextSibling()) {
                    if (n instanceof Element e && "rDE".equals(e.getLocalName())) {
                        rDE = e;
                    }
                }
                if (rDE == null) {
                    throw SifenException.of(ErrorCode.JBS_INT_01, "setupDE no dejó el rDE en el contenedor", null);
                }
                String xml = SifenUtil.serialize(rDE);
                this.signedXml = xml;
                return xml;
            } catch (SOAPException e) {
                throw SifenException.of(ErrorCode.JBS_INT_01, "Se produjo un error al armar el rDE: "
                        + e.getClass().getName() + ": " + e.getMessage(), e);
            }
        });
    }

    /**
     * Devuelve el {@code rDE} firmado por el último {@link #generateSignedXml(SifenConfig)}: el que se guarda y
     * se envía (§4.1). Firmar otra vez por el camino heredado ({@code generarXml}, {@code setupDE}, el envío
     * de {@code Sifen}) lo deja en {@code null}, porque esa firma es otra.
     *
     * @return el XML firmado, o {@code null} si no hay uno vigente.
     */
    public String getSignedXml() {
        return signedXml;
    }

    /**
     * Genera un XML completo en base al Documento Electrónico actual, y lo guarda como archivo en la ruta definida.
     *
     * @param rutaDestino Ruta absoluta (con nombre de archivo y extensión incluidos) en la que será creada el archivo
     *                    XML.<br> Ejemplo: "C:\Users\Roshka\Documents\de.xml"
     * @return <strong>true</strong> si el archivo fue creado correctamente, <strong>false</strong> de lo contrario.
     * @throws SifenException Si la configuración de Sifen no fue establecida o, si algún dato necesario para la
     *                        generación del XML no pudo ser encontrado o, si la firma digital del DE falla.
     */
    public boolean generarXml(GenerationCtx generationCtx, String rutaDestino) throws SifenException {
        return Operations.execute(Operation.GENERATE_XML, this::getId,
                () -> this.generarXml(generationCtx, rutaDestino, Sifen.getSifenConfig()));
    }

    /**
     * Genera un XML completo en base al Documento Electrónico actual, y lo guarda como archivo en la ruta definida.
     *
     * @param rutaDestino Ruta absoluta (con nombre de archivo y extensión incluidos) en la que será creada el archivo
     *                    XML.<br> Ejemplo: "C:\Users\Roshka\Documents\de.xml"
     * @param sifenConfig Configuración de Sifen a ser utilizada para la generación del XML.
     * @return <strong>true</strong> si el archivo fue creado correctamente, <strong>false</strong> de lo contrario.
     * @throws SifenException Si la configuración de Sifen no fue establecida o, si algún dato necesario para la
     *                        generación del XML no pudo ser encontrado o, si la firma digital del DE falla.
     */
    public boolean generarXml(GenerationCtx generationCtx, String rutaDestino, SifenConfig sifenConfig) throws SifenException {
        return Operations.execute(Operation.GENERATE_XML, this::getId, () -> {
            // Obtenemos el xml en string
            String xml = this.generarXml(generationCtx, sifenConfig);

            // Creamos o modificamos el archivo, y escribimos en él el xml.
            boolean res = false;
            try {
                FileWriter fileWriter = new FileWriter(rutaDestino, false);
                fileWriter.write(xml);
                fileWriter.close();
                res = true;
            } catch (IOException e) {
                // contrato: false si no se pudo escribir (sin printStackTrace, §3.8)
                LOGGER.warn("Se produjo un error al escribir en el archivo especificado: {}", e.getClass().getName());
            }

            return res;
        });
    }

    /**
     * Método interno, no usar.
     *
     * @param dId         -
     * @param sifenConfig -
     * @return -
     * @throws SOAPException  -
     * @throws SifenException -
     */
    public SOAPMessage setupSOAPElements(GenerationCtx generationCtx, long dId, SifenConfig sifenConfig) throws SOAPException, SifenException {
        SOAPMessage message = SoapHelper.createSoapMessage();
        SOAPBody soapBody = message.getSOAPBody();

        // Main Element
        SOAPBodyElement rResEnviDe = soapBody.addBodyElement(new QName(Constants.SIFEN_NS_URI, "rEnviDe"));
        rResEnviDe.addChildElement("dId").setTextContent(String.valueOf(dId));

        SOAPElement xDE = rResEnviDe.addChildElement("xDE");
        this.setupDE(generationCtx, xDE, sifenConfig);

        return message;
    }

    /**
     * Método interno, no usar.
     *
     * @param parentNode  -
     * @param sifenConfig -
     * @throws SOAPException  -
     * @throws SifenException -
     */
//    INICIO CAMBIO AM
//    se realizo un overload del metodo  setupDE para que reciba receivedCDC
    public void setupDE(GenerationCtx generationCtx, SOAPElement parentNode, SifenConfig sifenConfig, String receivedCDC) throws SOAPException, SifenException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        // §4.1: una firma nueva invalida el rDE firmado que se conservaba
        this.signedXml = null;

        SOAPElement rDE = parentNode.addChildElement(new QName(Constants.SIFEN_NS_URI, "rDE"));

        rDE.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        rDE.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:schemaLocation", Constants.SIFEN_NS_URI_RECEP_DE);
        rDE.addChildElement("dVerFor").setTextContent(SIFEN_CURRENT_VERSION);

        this.obtenerCDC(receivedCDC);

        SOAPElement DE = rDE.addChildElement("DE");
        DE.setAttribute("Id", this.getId());
        Attr idAttribute = DE.getAttributeNode("Id");
        DE.setIdAttributeNode(idAttribute, true);

        DE.addChildElement("dDVId").setTextContent(this.getdDVId());
        DE.addChildElement("dFecFirma").setTextContent(this.getdFecFirma().format(formatter));
        DE.addChildElement("dSisFact").setTextContent(String.valueOf(this.getdSisFact()));

        // Se prepara el cuerpo del documento electrónico
        TTiDE iTiDE = this.gTimb.getiTiDE();

        this.gOpeDE.setupSOAPElements(DE, iTiDE);
        this.gTimb.setupSOAPElements(DE);
        this.gDatGralOpe.setupSOAPElements(DE, iTiDE);
        this.gDtipDE.setupSOAPElements(generationCtx, DE, iTiDE, this.gDatGralOpe);

        if (iTiDE.getVal() != 7)
            this.gTotSub.setupSOAPElements(DE, iTiDE, this.getgDtipDE(), this.gDatGralOpe.getgOpeCom());

        if (this.gCamGen != null)
            this.gCamGen.setupSOAPElements(DE, iTiDE);

        if (iTiDE.getVal() == 4 || iTiDE.getVal() == 5 || iTiDE.getVal() == 6 || ((iTiDE.getVal() == 1 || iTiDE.getVal() == 7 || iTiDE.getVal() == 8) && this.gCamDEAsocList != null)) {
            boolean withholdingExists = false; // Retención
            if ((iTiDE.getVal() == 1 || iTiDE.getVal() == 4) && this.gDtipDE.getgCamCond().getgPaConEIniList() != null) {
                for (TgPaConEIni gPaConEIni : this.gDtipDE.getgCamCond().getgPaConEIniList()) {
                    if (gPaConEIni.getiTiPago().getVal() == 10) {
                        withholdingExists = true;
                        break;
                    }
                }
            }

            for (TgCamDEAsoc gCamDEAsoc : this.gCamDEAsocList) {
                gCamDEAsoc.setupSOAPElements(DE, this.gDatGralOpe.getgOpeCom() != null ? this.gDatGralOpe.getgOpeCom().getiTipTra() : null, withholdingExists);
            }
        }

        // Firma Digital del XML
        SignedInfo signedInfo = SignatureHelper.signDocument(sifenConfig, rDE, this.getId());

        // Preparación de la URL del QR (§3.8: una falla es JBS-FIRMA-04, no una excepción cruda)
        try {
            this.enlaceQR = this.generateQRLink(signedInfo, sifenConfig);
        } catch (RuntimeException e) {
            throw SifenException.of(ErrorCode.JBS_FIRMA_04, "QR: " + e.getClass().getName(), e);
        }
        SOAPElement gCamFuFD = rDE.addChildElement("gCamFuFD");
        gCamFuFD.addChildElement("dCarQR").setTextContent(this.enlaceQR);
    }

    //    FIN CAMBIO
    public void setupDE(GenerationCtx generationCtx, SOAPElement parentNode, SifenConfig sifenConfig) throws SOAPException, SifenException {
        this.setupDE(generationCtx, parentNode, sifenConfig, this.calcularCDC());
    }

    /**
     * Método interno, no usar.
     *
     * @param value -
     * @throws SifenException -
     */
    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        switch (value.getLocalName()) {
            case "dDVId":
                this.dDVId = ResponseUtil.getTextValue(value);
                break;
            case "dFecFirma":
                this.dFecFirma = ResponseUtil.getDateTimeValue(value);
                break;
            case "dSisFact":
                this.dSisFact = Short.parseShort(ResponseUtil.getTextValue(value));
                break;
            case "gOpeDE":
                this.gOpeDE = SifenObjectFactory.getFromNode(value, TgOpeDE.class);
                break;
            case "gTimb":
                this.gTimb = SifenObjectFactory.getFromNode(value, TgTimb.class);
                break;
            case "gDatGralOpe":
                this.gDatGralOpe = SifenObjectFactory.getFromNode(value, TdDatGralOpe.class);
                break;
            case "gDtipDE":
                this.gDtipDE = SifenObjectFactory.getFromNode(value, TgDtipDE.class);
                break;
            case "gTotSub":
                this.gTotSub = SifenObjectFactory.getFromNode(value, TgTotSub.class);
                break;
            case "gCamGen":
                this.gCamGen = SifenObjectFactory.getFromNode(value, TgCamGen.class);
                break;
            case "gCamDEAsoc":
                if (this.gCamDEAsocList == null) {
                    this.gCamDEAsocList = new ArrayList<>();
                }
                this.gCamDEAsocList.add(SifenObjectFactory.getFromNode(value, TgCamDEAsoc.class));
                break;
        }
    }

    private String generateQRLink(SignedInfo signedInfo, SifenConfig sifenConfig) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        LinkedHashMap<String, String> queryParams = new LinkedHashMap<>();

        queryParams.put("nVersion", SIFEN_CURRENT_VERSION);
        queryParams.put("Id", this.getId());
        queryParams.put("dFeEmiDE", SifenUtil.bytesToHex(this.getgDatGralOpe().getdFeEmiDE().format(formatter).getBytes(StandardCharsets.UTF_8)));

        if (this.getgDatGralOpe().getgDatRec().getiNatRec().getVal() == 1) {
            queryParams.put("dRucRec", this.getgDatGralOpe().getgDatRec().getdRucRec());
        } else if (this.getgDatGralOpe().getgDatRec().getdNumIDRec() != null) {
            queryParams.put("dNumIDRec", this.getgDatGralOpe().getgDatRec().getdNumIDRec());
        } else {
            queryParams.put("dNumIDRec", "0");
        }

        if (this.getgTimb().getiTiDE().getVal() != 7) {
            queryParams.put("dTotGralOpe", String.valueOf(this.getgTotSub().getdTotGralOpe()));
            queryParams.put("dTotIVA",
                    this.getgDatGralOpe().getgOpeCom().getiTImp().getVal() == 1 || this.getgDatGralOpe().getgOpeCom().getiTImp().getVal() == 5
                            ? String.valueOf(this.getgTotSub().getdTotIVA())
                            : "0"
            );
        } else {
            queryParams.put("dTotGralOpe", "0");
            queryParams.put("dTotIVA", "0");
        }

        queryParams.put("cItems", String.valueOf(this.getgDtipDE().getgCamItemList().size()));

        byte[] digestValue = Base64.getEncoder().encode(((Reference) signedInfo.getReferences().get(0)).getDigestValue());
        queryParams.put("DigestValue", SifenUtil.bytesToHex(digestValue));
        queryParams.put("IdCSC", sifenConfig.getCscId());

        String urlParamsString = SifenUtil.buildUrlParams(queryParams);
        String hashedParams = SifenUtil.sha256Hex(urlParamsString + sifenConfig.getCsc());

        return sifenConfig.getQRQueryUrl() + urlParamsString + "&cHashQR=" + hashedParams;
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public String getdDVId() {
        return dDVId;
    }

    public LocalDateTime getdFecFirma() {
        return dFecFirma;
    }

    public void setdFecFirma(LocalDateTime dFecFirma) {
        this.dFecFirma = dFecFirma;
    }

    public short getdSisFact() {
        return dSisFact;
    }

    public void setdSisFact(short dSisFact) {
        this.dSisFact = dSisFact;
    }

    public TgOpeDE getgOpeDE() {
        return gOpeDE;
    }

    public void setgOpeDE(TgOpeDE gOpeDE) {
        this.gOpeDE = gOpeDE;
    }

    public TgTimb getgTimb() {
        return gTimb;
    }

    public void setgTimb(TgTimb gTimb) {
        this.gTimb = gTimb;
    }

    public TdDatGralOpe getgDatGralOpe() {
        return gDatGralOpe;
    }

    public void setgDatGralOpe(TdDatGralOpe gDatGralOpe) {
        this.gDatGralOpe = gDatGralOpe;
    }

    public TgDtipDE getgDtipDE() {
        return gDtipDE;
    }

    public void setgDtipDE(TgDtipDE gDtipDE) {
        this.gDtipDE = gDtipDE;
    }

    public TgTotSub getgTotSub() {
        return gTotSub;
    }

    public void setgTotSub(TgTotSub gTotSub) {
        this.gTotSub = gTotSub;
    }

    /**
     * Fija si se aplica el redondeo SEDECO a {@code dTotGralOpe} (plan JBSSIF, §4.2, M0-02): atajo de
     * {@link TgTotSub#setRedondeo(Boolean)} que crea el grupo {@code gTotSub} si todavía no está. Por
     * omisión se redondea; al leer el DE de un XML vale lo que dice su {@code dRedon}.
     *
     * @param redondeo {@code true} redondea; {@code false} no; {@code null} equivale a {@code true}.
     */
    public void setRedondeo(Boolean redondeo) {
        if (this.gTotSub == null) {
            this.gTotSub = new TgTotSub();
        }
        this.gTotSub.setRedondeo(redondeo);
    }

    public TgCamGen getgCamGen() {
        return gCamGen;
    }

    public void setgCamGen(TgCamGen gCamGen) {
        this.gCamGen = gCamGen;
    }

    public List<TgCamDEAsoc> getgCamDEAsocList() {
        return gCamDEAsocList;
    }

    public void setgCamDEAsocList(List<TgCamDEAsoc> gCamDEAsocList) {
        this.gCamDEAsocList = gCamDEAsocList;
    }

    /**
     * Devuelve la URL del QR ({@code dCarQR}) del documento: la de la última firma, o la del XML firmado del
     * que se leyó el documento (§4.4, E0-02; antes quedaba en {@code null} al leer).
     *
     * @return la URL del QR, o {@code null} si el documento no se firmó ni se leyó de un XML firmado.
     */
    public String getEnlaceQR() {
        return enlaceQR;
    }

    public void setEnlaceQR(String enlaceQR) {
        this.enlaceQR = enlaceQR;
    }

}
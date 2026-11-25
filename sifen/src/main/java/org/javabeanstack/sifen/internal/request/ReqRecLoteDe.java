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
package org.javabeanstack.sifen.internal.request;

import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionLoteDE;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.Constants;
import org.javabeanstack.sifen.internal.SOAPResponse;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.internal.helpers.SoapHelper;
import org.javabeanstack.sifen.internal.response.BaseResponse;
import org.javabeanstack.sifen.internal.response.SifenObjectFactory;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.javabeanstack.sifen.internal.util.SifenUtil;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import jakarta.xml.soap.*;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Clase encargada de la petición de Recepción de Lote de Documentos Electrónicos.
 *
 * <p>Como {@link ReqRecDe} (plan JBSSIF, §4.1): con {@link #setDEList(List)}, la forma heredada, arma y
 * firma cada DE; con {@link #setSignedXmlList(List)} arma el {@code rLoteDE} con los {@code rDE} ya
 * firmados, insertados tal cual. Si están las dos, mandan los XML firmados.</p>
 */
public class ReqRecLoteDe extends BaseRequest {
    private List<DocumentoElectronico> DEList;
    private List<String> signedXmlList;
    private static final Logger LOGGER = LogManager.getLogger(ReqRecLoteDe.class);
//    @Value("#{new Boolean('${useReceivedCDC}')}")
//    public Boolean useReceivedCDC;
//    private Boolean useReceivedCDC = true;

    public ReqRecLoteDe(long dId, SifenConfig sifenConfig) {
        super(dId, sifenConfig);
    }

    @Override
    SOAPMessage setupSoapMessage(GenerationCtx generationCtx) throws SifenException {
        try {
            SOAPMessage message = SoapHelper.createSoapMessage();
            SOAPBody soapBody = message.getSOAPBody();

            // Main Element
            SOAPBodyElement rEnvioLote = soapBody.addBodyElement(new QName(Constants.SIFEN_NS_URI, "rEnvioLote"));
            rEnvioLote.addChildElement("dId").setTextContent(String.valueOf(this.getdId()));
            SOAPElement xDE = rEnvioLote.addChildElement("xDE");

            SOAPMessage contenedor = SoapHelper.createSoapMessage();
            SOAPElement rLoteDE = contenedor.getSOAPBody().addChildElement("rLoteDE");
            if (this.signedXmlList != null) {
                // §4.1: cada rDE firmado tal cual, sin regenerarlo
                for (String signedXml : this.signedXmlList) {
                    rLoteDE.appendChild(contenedor.getSOAPPart().importNode(SifenUtil.parseRDE(signedXml), true));
                }
            } else {
                for (DocumentoElectronico DE : DEList) {
                    DE.setupDE(generationCtx, rLoteDE, this.getSifenConfig());
                }
            }
//            FIN CAMBIO

            // Obtenemos el XML
            final StringWriter sw = new StringWriter();
            try {
                TransformerFactory.newInstance().newTransformer().transform(new DOMSource(rLoteDE), new StreamResult(sw));
            } catch (TransformerException e) {
                throw SifenException.of(ErrorCode.JBS_INT_02, "serialización del lote: " + e.getClass().getName(), e);
            }

            // Comprimimos a un archivo zip
            byte[] zipFile = SifenUtil.compressXmlToZip(sw.toString());

            // Convertimos el zip a Base64
            String rLoteDEBase64 = new String(Base64.getEncoder().encode(zipFile), StandardCharsets.UTF_8);
            xDE.setTextContent(rLoteDEBase64);

            return message;
        } catch (SOAPException | IOException e) {
            throw SifenException.of(ErrorCode.JBS_INT_01, "Ocurrió un error al preparar el cuerpo de la petición SOAP: " + e.getClass().getName(), e);
        }
    }

    @Override
    BaseResponse processResponse(SOAPResponse soapResponse) throws SifenException {
        // §3.8: Fault, cuerpo vacío o nodo distinto lanzan JBS-RESP-01/02 (antes: WARN y respuesta vacía)
        Node rResEnviLoteDe = ResponseUtil.getMainNode(soapResponse.getSoapResponse(), "rResEnviLoteDe");

        RespuestaRecepcionLoteDE respuestaRecepcionLoteDE = new RespuestaRecepcionLoteDE();
        if (rResEnviLoteDe != null) {
            respuestaRecepcionLoteDE = SifenObjectFactory.getFromNode(rResEnviLoteDe, RespuestaRecepcionLoteDE.class);
        }

        respuestaRecepcionLoteDE.setCodigoEstado(soapResponse.getStatus());
        respuestaRecepcionLoteDE.setRespuestaBruta(new String(soapResponse.getRawData(), StandardCharsets.UTF_8));
        return respuestaRecepcionLoteDE;
    }

    public void setDEList(List<DocumentoElectronico> DEList) {
        this.DEList = DEList;
    }

    /**
     * Fija los {@code rDE} firmados del lote, que se envían tal cual (§4.1): {@code SignedDocument.getXml()}
     * de cada documento. Los límites del lote (50, mismo tipo y RUC, sin CDC repetidos) los controla quien
     * llama.
     *
     * @param signedXmlList los XML firmados, en el orden del lote.
     */
    public void setSignedXmlList(List<String> signedXmlList) {
        this.signedXmlList = signedXmlList;
    }


    /** §3.8: sin código de resultado ⇒ {@code JBS-RESP-04}; aprobado o recibido sin su número ⇒ {@code JBS-RESP-07}. */
    @Override
    void verificarRespuesta(BaseResponse respuesta) throws SifenException {
        RespuestaRecepcionLoteDE r = (RespuestaRecepcionLoteDE) respuesta;
        exigirCodigo(r.getdCodRes(), "dCodRes", r.getCodigoEstado());
        if ("0300".equals(r.getdCodRes()) && (r.getdProtConsLote() == null || r.getdProtConsLote().isBlank())) {
            throw SifenException.ofResponse(ErrorCode.JBS_RESP_07, "lote recibido (0300) sin dProtConsLote", r.getdCodRes());
        }
    }
}

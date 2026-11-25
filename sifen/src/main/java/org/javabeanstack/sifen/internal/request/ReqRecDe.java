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
import org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionDE;
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
import jakarta.xml.soap.SOAPBodyElement;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Clase encargada de la petición de Recepción de Documentos Electrónicos.
 *
 * <p>Dos formas (plan JBSSIF, §4.1): con {@link #setDE(DocumentoElectronico)}, la heredada, arma y firma el
 * DE en cada envío; con {@link #setSignedXml(String)} inserta el {@code rDE} ya firmado <b>tal cual</b>
 * —importado como DOM, sin regenerarlo ni volver a firmarlo—, que es lo que usa la emisión en dos pasos
 * ({@code ISifenClient.send}, §10.3). Si están las dos, manda el XML firmado.</p>
 */
public class ReqRecDe extends BaseRequest {
    private DocumentoElectronico DE;
    private String signedXml;
    private static final Logger LOGGER = LogManager.getLogger(ReqRecDe.class);

    public ReqRecDe(long dId, SifenConfig sifenConfig) {
        super(dId, sifenConfig);
    }

    @Override
    SOAPMessage setupSoapMessage(GenerationCtx generationCtx) throws SifenException {
        try {
            if (this.signedXml != null) {
                return signedMessage(this.getdId(), this.signedXml);
            }
            return this.DE.setupSOAPElements(generationCtx, this.getdId(), this.getSifenConfig());
        } catch (SOAPException e) {
            throw SifenException.of(ErrorCode.JBS_INT_01, "Ocurrió un error al preparar el cuerpo de la petición SOAP: " + e.getClass().getName(), e);
        }
    }

    @Override
    BaseResponse processResponse(SOAPResponse soapResponse) throws SifenException {
        // §3.8: Fault, cuerpo vacío o nodo distinto lanzan JBS-RESP-01/02 (antes: WARN y respuesta vacía)
        Node rRetEnviDe = ResponseUtil.getMainNode(soapResponse.getSoapResponse(), "rRetEnviDe");

        RespuestaRecepcionDE respuestaRecepcionDE = new RespuestaRecepcionDE();
        if (rRetEnviDe != null) {
            respuestaRecepcionDE = SifenObjectFactory.getFromNode(rRetEnviDe, RespuestaRecepcionDE.class);
        }

        respuestaRecepcionDE.setCodigoEstado(soapResponse.getStatus());
        respuestaRecepcionDE.setRespuestaBruta(new String(soapResponse.getRawData(), StandardCharsets.UTF_8));
        return respuestaRecepcionDE;
    }

    /**
     * Sobre {@code rEnviDe} con el {@code rDE} firmado insertado tal cual (§4.1): mismo {@code DigestValue} y
     * {@code SignatureValue}, y la misma forma canónica del {@code rDE}.
     *
     * @param dId       número del pedido.
     * @param signedXml el {@code rDE} firmado.
     * @return el mensaje.
     * @throws SOAPException  si falla el armado del sobre.
     * @throws SifenException {@code JBS-XML-*} si el XML no tiene un {@code rDE}.
     */
    static SOAPMessage signedMessage(long dId, String signedXml) throws SOAPException, SifenException {
        org.w3c.dom.Element rDE = SifenUtil.parseRDE(signedXml);
        SOAPMessage message = SoapHelper.createSoapMessage();
        SOAPBodyElement rEnviDe = message.getSOAPBody().addBodyElement(new QName(Constants.SIFEN_NS_URI, "rEnviDe"));
        rEnviDe.addChildElement("dId").setTextContent(String.valueOf(dId));
        SOAPElement xDE = rEnviDe.addChildElement("xDE");
        xDE.appendChild(message.getSOAPPart().importNode(rDE, true));
        return message;
    }

    public void setDE(DocumentoElectronico DE) {
        this.DE = DE;
    }

    /**
     * Fija el {@code rDE} firmado que se envía tal cual (§4.1, E0-01): {@code SignedDocument.getXml()} o lo
     * que devolvió {@link DocumentoElectronico#generateSignedXml}. El {@code rDE} puede venir como raíz o
     * dentro de otro XML (se toma el primero).
     *
     * @param signedXml el XML firmado.
     */
    public void setSignedXml(String signedXml) {
        this.signedXml = signedXml;
    }

    /** §3.8: sin código de resultado ⇒ {@code JBS-RESP-04}; aprobado o recibido sin su número ⇒ {@code JBS-RESP-07}. */
    @Override
    void verificarRespuesta(BaseResponse respuesta) throws SifenException {
        RespuestaRecepcionDE r = (RespuestaRecepcionDE) respuesta;
        if (r.getxProtDE() == null) {
            throw SifenException.ofResponse(ErrorCode.JBS_RESP_04, "HTTP " + r.getCodigoEstado() + " sin rProtDe", null);
        }
        String estado = r.getxProtDE().getdEstRes();
        if (estado != null && estado.startsWith("Aprobado")
                && (r.getxProtDE().getdProtAut() == null || r.getxProtDE().getdProtAut().isBlank())) {
            throw SifenException.ofResponse(ErrorCode.JBS_RESP_07, "DE " + estado + " sin dProtAut", null);
        }
    }
}

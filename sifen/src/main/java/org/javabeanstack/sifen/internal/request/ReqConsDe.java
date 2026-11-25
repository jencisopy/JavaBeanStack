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
import org.javabeanstack.sifen.core.beans.response.RespuestaConsultaDE;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.Constants;
import org.javabeanstack.sifen.internal.SOAPResponse;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.internal.helpers.SoapHelper;
import org.javabeanstack.sifen.internal.response.BaseResponse;
import org.javabeanstack.sifen.internal.response.SifenObjectFactory;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPBodyElement;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Clase encargada de la Consulta de Documentos Electrónicos.
 */
public class ReqConsDe extends BaseRequest {
    private String dCDC;
    private static final Logger LOGGER = LogManager.getLogger(ReqConsDe.class);

    public ReqConsDe(long dId, SifenConfig sifenConfig) {
        super(dId, sifenConfig);
    }

    @Override
    SOAPMessage setupSoapMessage(GenerationCtx generationCtx) throws SifenException {
        try {
            // Main Element
            SOAPMessage message = SoapHelper.createSoapMessage();
            SOAPBody soapBody = message.getSOAPBody();
            SOAPBodyElement rEnviConsDeRequest = soapBody.addBodyElement(new QName(Constants.SIFEN_NS_URI, "rEnviConsDeRequest"));

            // Elements
            rEnviConsDeRequest.addChildElement("dId").setTextContent(String.valueOf(this.getdId()));
            rEnviConsDeRequest.addChildElement("dCDC").setTextContent(this.dCDC);

            return message;
        } catch (SOAPException e) {
            throw SifenException.of(ErrorCode.JBS_INT_01, "Ocurrió un error al preparar el cuerpo de la petición SOAP: " + e.getClass().getName(), e);
        }
    }

    @Override
    BaseResponse processResponse(SOAPResponse soapResponse) throws SifenException {
        // §3.8: Fault, cuerpo vacío o nodo distinto lanzan JBS-RESP-01/02 (antes: WARN y respuesta vacía)
        SOAPMessage soapMessage = ResponseUtil.parseSoapMessage(soapResponse.getSoapResponse());
        Node rEnviConsDe = ResponseUtil.getMainNode(soapMessage, "rEnviConsDeResponse");
        String rawResponse = ResponseUtil.getXmlFromMessage(soapMessage, false);

        RespuestaConsultaDE respuestaConsultaDE = new RespuestaConsultaDE();
        if (rEnviConsDe != null) {
            respuestaConsultaDE = SifenObjectFactory.getFromNode(rEnviConsDe, RespuestaConsultaDE.class);
        }

        respuestaConsultaDE.setCodigoEstado(soapResponse.getStatus());
        respuestaConsultaDE.setRespuestaBruta(rawResponse != null ? rawResponse : new String(soapResponse.getRawData(), StandardCharsets.UTF_8));
        return respuestaConsultaDE;
    }

    public void setdCDC(String dCDC) {
        this.dCDC = dCDC;
    }

    /** §3.8: sin código de resultado ⇒ {@code JBS-RESP-04}; aprobado o recibido sin su número ⇒ {@code JBS-RESP-07}. */
    @Override
    void verificarRespuesta(BaseResponse respuesta) throws SifenException {
        exigirCodigo(((RespuestaConsultaDE) respuesta).getdCodRes(), "dCodRes", respuesta.getCodigoEstado());
    }

    /** §5.2 (D41): una consulta se puede reintentar ante una falla rápida de transporte. */
    @Override
    boolean isRetryable() {
        return true;
    }
}

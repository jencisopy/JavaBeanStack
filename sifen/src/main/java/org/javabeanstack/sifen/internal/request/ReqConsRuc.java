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
import org.javabeanstack.sifen.core.beans.response.RespuestaConsultaRUC;
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
 * Clase encargada de la Consulta de RUC.
 */
public class ReqConsRuc extends BaseRequest {
    private String dRUCCons;
    private static final Logger LOGGER = LogManager.getLogger(ReqConsRuc.class);

    public ReqConsRuc(long dId, SifenConfig sifenConfig) {
        super(dId, sifenConfig);
    }

    @Override
    SOAPMessage setupSoapMessage(GenerationCtx generationCtx) throws SifenException {
        try {
            // Main Element
            SOAPMessage message = SoapHelper.createSoapMessage();
            SOAPBody soapBody = message.getSOAPBody();
            SOAPBodyElement rResEnviConsRUC = soapBody.addBodyElement(new QName(Constants.SIFEN_NS_URI, "rEnviConsRUC"));

            // Elements
            rResEnviConsRUC.addChildElement("dId").setTextContent(String.valueOf(this.getdId()));
            rResEnviConsRUC.addChildElement("dRUCCons").setTextContent(this.dRUCCons);

            return message;
        } catch (SOAPException e) {
            throw SifenException.of(ErrorCode.JBS_INT_01, "Ocurrió un error al preparar el cuerpo de la petición SOAP: " + e.getClass().getName(), e);
        }
    }

    @Override
    BaseResponse processResponse(SOAPResponse soapResponse) throws SifenException {
        // §3.8: Fault, cuerpo vacío o nodo distinto lanzan JBS-RESP-01/02 (antes: WARN y respuesta vacía)
        Node rResEnviConsRuc = ResponseUtil.getMainNode(soapResponse.getSoapResponse(), "rResEnviConsRuc");

        RespuestaConsultaRUC respuestaConsultaRUC = new RespuestaConsultaRUC();
        if (rResEnviConsRuc != null) {
            respuestaConsultaRUC = SifenObjectFactory.getFromNode(rResEnviConsRuc, RespuestaConsultaRUC.class);
        }

        respuestaConsultaRUC.setCodigoEstado(soapResponse.getStatus());
        respuestaConsultaRUC.setRespuestaBruta(new String(soapResponse.getRawData(), StandardCharsets.UTF_8));
        return respuestaConsultaRUC;
    }

    public void setdRUCCons(String dRUCCons) {
        this.dRUCCons = dRUCCons;
    }

    /** §3.8: sin código de resultado ⇒ {@code JBS-RESP-04}; aprobado o recibido sin su número ⇒ {@code JBS-RESP-07}. */
    @Override
    void verificarRespuesta(BaseResponse respuesta) throws SifenException {
        exigirCodigo(((RespuestaConsultaRUC) respuesta).getdCodRes(), "dCodRes", respuesta.getCodigoEstado());
    }

    /** §5.2 (D41): una consulta se puede reintentar ante una falla rápida de transporte. */
    @Override
    boolean isRetryable() {
        return true;
    }

    /**
     * §8.7 (I8-03): SIFEN-test contesta en forma intermitente un {@code rRetEnviDe} 0160 «XML Mal Formado» a
     * este mismo pedido (12 de 23 consultas), y 0161/0162 son, según el MT (AE02/AE03), un servidor
     * momentáneamente sin respuesta. El pedido es fijo y trivial (dos campos), así que aquí no es un error de
     * datos: se repite como una falla de transporte, dentro de {@code sifen.reintentos.max} (0 por omisión).
     * En las otras operaciones un 0160 sí puede ser un valor inválido (E3-24, E3-26) y no se reintenta.
     */
    @Override
    boolean isRetryableResponse(SifenException e) {
        String sifen = e.getSifenResponseCode();
        return ErrorCode.JBS_RESP_02.getCode().equals(e.getCode())
                && ("0160".equals(sifen) || "0161".equals(sifen) || "0162".equals(sifen));
    }
}

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
import org.javabeanstack.sifen.core.beans.EventosDE;
import org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionEvento;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.fields.request.event.TgGroupTiEvt;
import org.javabeanstack.sifen.core.fields.request.event.TrGeVeCan;
import org.javabeanstack.sifen.core.fields.request.event.TrGeVeInu;
import org.javabeanstack.sifen.core.fields.request.event.TrGesEve;
import org.javabeanstack.sifen.core.types.TTiDE;
import org.javabeanstack.sifen.internal.Constants;
import org.javabeanstack.sifen.internal.SOAPResponse;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.internal.helpers.SoapHelper;
import org.javabeanstack.sifen.internal.response.BaseResponse;
import org.javabeanstack.sifen.internal.response.SifenObjectFactory;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import jakarta.xml.soap.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Clase encargada de la petición de Recepción de Eventos de Documentos Electrónicos.
 */
public class ReqRecEventoDe extends BaseRequest {
    private EventosDE eventosDE;
    private static final Logger LOGGER = LogManager.getLogger(ReqRecEventoDe.class);

    public ReqRecEventoDe(long dId, SifenConfig sifenConfig) {
        super(dId, sifenConfig);
    }

    @Override
    SOAPMessage setupSoapMessage(GenerationCtx generationCtx) throws SifenException {
        try {
            // Main Element
            SOAPMessage message = SoapHelper.createSoapMessage();
            SOAPBody soapBody = message.getSOAPBody();

            SOAPBodyElement rEnviEventoDe = soapBody.addBodyElement(new QName(Constants.SIFEN_NS_URI, "rEnviEventoDe"));
            rEnviEventoDe.addChildElement("dId").setTextContent(String.valueOf(this.getdId()));

            SOAPElement gGroupGesEve = rEnviEventoDe.addChildElement("dEvReg").addChildElement("gGroupGesEve");

            gGroupGesEve.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            gGroupGesEve.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:schemaLocation", Constants.SIFEN_NS_URI_RECEP_EVENTO);

            // Se completa con los demás elementos del XML
            this.eventosDE.setupSOAPElements(gGroupGesEve, this.getSifenConfig());

            return message;
        } catch (SOAPException e) {
            throw SifenException.of(ErrorCode.JBS_INT_01, "Ocurrió un error al preparar el cuerpo de la petición SOAP: " + e.getClass().getName(), e);
        }
    }

    @Override
    BaseResponse processResponse(SOAPResponse soapResponse) throws SifenException {
        // §3.8: Fault, cuerpo vacío o nodo distinto lanzan JBS-RESP-01/02 (antes: WARN y respuesta vacía)
        Node rRetEnviEventoDe = ResponseUtil.getMainNode(soapResponse.getSoapResponse(), "rRetEnviEventoDe");

        RespuestaRecepcionEvento respuestaRecepcionEvento = new RespuestaRecepcionEvento();
        if (rRetEnviEventoDe != null) {
            respuestaRecepcionEvento = SifenObjectFactory.getFromNode(rRetEnviEventoDe, RespuestaRecepcionEvento.class);
        }

        respuestaRecepcionEvento.setCodigoEstado(soapResponse.getStatus());
        respuestaRecepcionEvento.setRespuestaBruta(new String(soapResponse.getRawData(), StandardCharsets.UTF_8));
        return respuestaRecepcionEvento;
    }

    public void setEventoDE(EventosDE eventosDE) {
        this.eventosDE = eventosDE;
    }

    // ------------------------------------------------------------------ eventos del emisor (plan JBSSIF, §10.3, carril A)

    /**
     * Arma el evento de cancelación de un DE aprobado ({@code rGeVeCan}), el mismo que se arma a mano con el
     * modelo: lo usa {@code ISifenClient.cancel} (§10.3). No valida: el motivo de 5 a 500 caracteres y el CDC
     * los revisa la validación de eventos ({@code validateEvents}) antes de enviar.
     *
     * @param eventId       {@code Id} del evento ({@code rEve@Id}, hasta 10 dígitos; lo numera quien llama).
     * @param signatureDate {@code dFecFirma} del evento.
     * @param cdc           CDC del documento a cancelar.
     * @param reason        motivo ({@code mOtEve}).
     * @return los eventos, con uno solo.
     */
    public static EventosDE cancellation(String eventId, LocalDateTime signatureDate, String cdc, String reason) {
        TrGeVeCan can = new TrGeVeCan();
        can.setId(cdc);
        can.setmOtEve(reason);
        TgGroupTiEvt group = new TgGroupTiEvt();
        group.setrGeVeCan(can);
        return single(eventId, signatureDate, group);
    }

    /**
     * Arma el evento de inutilización de un rango de numeración ({@code rGeVeInu}), el mismo que se arma a
     * mano con el modelo: lo usa {@code ISifenClient.invalidateNumbers} (§10.3) desde un {@code NumberRange}.
     * Establecimiento, punto y números se completan con ceros a la izquierda como en {@code TrGeVeInu}. No
     * valida: el rango (hasta 1000 números, fin ≥ inicio) y el motivo los revisa la validación de eventos.
     *
     * @param eventId       {@code Id} del evento ({@code rEve@Id}).
     * @param signatureDate {@code dFecFirma} del evento.
     * @param timbrado      número de timbrado ({@code dNumTim}).
     * @param establishment establecimiento ({@code dEst}).
     * @param issuePoint    punto de expedición ({@code dPunExp}).
     * @param from          primer número del rango ({@code dNumIn}).
     * @param to            último número del rango ({@code dNumFin}).
     * @param documentType  tipo de documento ({@code iTiDE}).
     * @param reason        motivo ({@code mOtEve}).
     * @return los eventos, con uno solo.
     */
    public static EventosDE numberInvalidation(String eventId, LocalDateTime signatureDate, int timbrado, String establishment,
                                               String issuePoint, String from, String to, TTiDE documentType, String reason) {
        TrGeVeInu inu = new TrGeVeInu();
        inu.setdNumTim(timbrado);
        inu.setdEst(establishment);
        inu.setdPunExp(issuePoint);
        inu.setdNumIn(from);
        inu.setdNumFin(to);
        inu.setiTiDE(documentType);
        inu.setmOtEve(reason);
        TgGroupTiEvt group = new TgGroupTiEvt();
        group.setrGeVeInu(inu);
        return single(eventId, signatureDate, group);
    }

    /** Un evento ({@code rGesEve}) con su grupo. */
    private static EventosDE single(String eventId, LocalDateTime signatureDate, TgGroupTiEvt group) {
        TrGesEve ges = new TrGesEve();
        ges.setId(eventId);
        ges.setdFecFirma(signatureDate);
        ges.setgGroupTiEvt(group);
        List<TrGesEve> list = new ArrayList<>();
        list.add(ges);
        EventosDE events = new EventosDE();
        events.setrGesEveList(list);
        return events;
    }

    /** §3.8: sin código de resultado ⇒ {@code JBS-RESP-04}; aprobado o recibido sin su número ⇒ {@code JBS-RESP-07}. */
    @Override
    void verificarRespuesta(BaseResponse respuesta) throws SifenException {
        RespuestaRecepcionEvento r = (RespuestaRecepcionEvento) respuesta;
        if (r.getgResProcEVe() == null || r.getgResProcEVe().isEmpty()) {
            throw SifenException.ofResponse(ErrorCode.JBS_RESP_04, "HTTP " + r.getCodigoEstado() + " sin gResProcEVe", null);
        }
    }
}

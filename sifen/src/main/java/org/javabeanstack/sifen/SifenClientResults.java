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
package org.javabeanstack.sifen;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.javabeanstack.sifen.api.BatchQueryResult;
import org.javabeanstack.sifen.api.BatchSendResult;
import org.javabeanstack.sifen.api.BatchStatus;
import org.javabeanstack.sifen.api.CertificateInfo;
import org.javabeanstack.sifen.api.DEQueryResult;
import org.javabeanstack.sifen.api.EventResult;
import org.javabeanstack.sifen.api.ResultStatus;
import org.javabeanstack.sifen.api.RucQueryResult;
import org.javabeanstack.sifen.api.SendResult;
import org.javabeanstack.sifen.api.SifenMessage;
import org.javabeanstack.sifen.api.SignatureVerification;
import org.javabeanstack.sifen.core.beans.ValidezFirmaDigital;
import org.javabeanstack.sifen.core.beans.response.RespuestaConsultaDE;
import org.javabeanstack.sifen.core.beans.response.RespuestaConsultaLoteDE;
import org.javabeanstack.sifen.core.beans.response.RespuestaConsultaRUC;
import org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionDE;
import org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionEvento;
import org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionLoteDE;
import org.javabeanstack.sifen.core.fields.response.TxProtDe;
import org.javabeanstack.sifen.core.fields.response.event.TgResProcEVe;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.fields.response.TgResProc;
import org.javabeanstack.sifen.core.fields.response.batch.TgResProcLote;
import org.javabeanstack.sifen.core.fields.response.de.TxContenDE;
import org.javabeanstack.sifen.core.fields.response.ruc.TxContRuc;
import org.javabeanstack.sifen.core.validation.support.LoadedCertificate;
import org.javabeanstack.sifen.internal.util.SifenUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Traducción de las respuestas internas ({@code Respuesta*}, {@code LoadedCertificate},
 * {@code ValidezFirmaDigital}) a los tipos del contrato ({@code org.javabeanstack.sifen.api}) (plan
 * JBSSIF, §10.2). No es parte del contrato (package-private). Sin estado: segura entre hilos.
 */
final class SifenClientResults {
    /** Consulta de DE: documento encontrado (aprobado). */
    static final String DE_FOUND = "0422";
    /** Consulta de DE: no existe o no está aprobado. */
    static final String DE_NOT_FOUND = "0420";
    /** Consulta de RUC: encontrado. */
    static final String RUC_FOUND = "0502";
    /** Recepción de DE: CDC duplicado (SIFEN ya tiene un DE con ese CDC). */
    static final String DE_DUPLICATED = "1001";
    /** Recepción de lote: recibido y encolado. */
    static final String BATCH_RECEIVED = "0300";
    /** Recepción de lote: no encolado. */
    static final String BATCH_NOT_QUEUED = "0301";
    /** Espera entre consultas de un lote en proceso (Guía de Mejores Prácticas). */
    static final Duration BATCH_QUERY_WAIT = Duration.ofMinutes(10);

    private SifenClientResults() {
    }

    // ------------------------------------------------------------------ estados

    /**
     * Interpreta el {@code dEstRes} de SIFEN («Aprobado», «Aprobado con observación», «Rechazado»),
     * sin distinguir mayúsculas ni tildes.
     *
     * @param sifenStatus texto de SIFEN.
     * @return el estado; {@link ResultStatus#UNRECOGNIZED} si no se reconoce.
     */
    static ResultStatus statusOf(String sifenStatus) {
        if (sifenStatus == null) {
            return ResultStatus.UNRECOGNIZED;
        }
        String s = Normalizer.normalize(sifenStatus.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        switch (s) {
            case "aprobado":
                return ResultStatus.APPROVED;
            case "aprobado con observacion":
                return ResultStatus.APPROVED_WITH_OBSERVATION;
            case "rechazado":
                return ResultStatus.REJECTED;
            default:
                return ResultStatus.UNRECOGNIZED;
        }
    }

    /**
     * Interpreta el código de resultado de la consulta de un lote ({@code dCodResLot}).
     *
     * @param code código.
     * @return el estado; {@link BatchStatus#UNRECOGNIZED} si no se reconoce.
     */
    static BatchStatus batchQueryStatusOf(String code) {
        if (code == null) {
            return BatchStatus.UNRECOGNIZED;
        }
        switch (code.trim()) {
            case "0360":
                return BatchStatus.NOT_FOUND;
            case "0361":
                return BatchStatus.IN_PROCESS;
            case "0362":
                return BatchStatus.COMPLETED;
            case "0364":
                return BatchStatus.EXPIRED;
            default:
                return BatchStatus.UNRECOGNIZED;
        }
    }

    private static List<SifenMessage> messages(String code, String text) {
        if ((code == null || code.isEmpty()) && (text == null || text.isEmpty())) {
            return List.of();
        }
        return List.of(SifenMessage.of(code, text));
    }

    private static List<SifenMessage> messages(List<TgResProc> list) {
        List<SifenMessage> out = new ArrayList<>();
        if (list != null) {
            for (TgResProc p : list) {
                if (p != null) {
                    out.add(SifenMessage.of(p.getdCodRes(), p.getdMsgRes()));
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ consultas

    /**
     * Resultado de la consulta de RUC.
     *
     * @param r           respuesta interpretada.
     * @param operationId id de la operación.
     * @param durationMs  duración.
     * @return el resultado del contrato.
     */
    static RucQueryResult rucQuery(RespuestaConsultaRUC r, String operationId, long durationMs) {
        TxContRuc c = r.getxContRUC();
        RucQueryResult.Builder b = RucQueryResult.builder()
                .found(RUC_FOUND.equals(r.getdCodRes()))
                .messages(messages(r.getdCodRes(), r.getdMsgRes()))
                .operationId(operationId)
                .rawResponse(r.getRespuestaBruta())
                .durationMs(durationMs);
        if (c != null) {
            b.ruc(c.getdRUCCons())
                    .businessName(c.getdRazCons())
                    .taxpayerStatusCode(c.getdCodEstCons())
                    .taxpayerStatus(c.getdDesEstCons())
                    .electronicInvoicer("S".equalsIgnoreCase(c.getdRUCFactElec() == null ? "" : c.getdRUCFactElec().trim()));
        }
        return b.build();
    }

    /**
     * Resultado de la consulta de un DE por CDC.
     *
     * @param r           respuesta interpretada.
     * @param cdc         CDC consultado.
     * @param operationId id de la operación.
     * @param durationMs  duración.
     * @return el resultado del contrato.
     * @throws SifenException {@code JBS-RESP-03} si la respuesta bruta no se puede releer.
     */
    static DEQueryResult deQuery(RespuestaConsultaDE r, String cdc, String operationId, long durationMs)
            throws SifenException {
        String code = r.getdCodRes();
        ResultStatus status = DE_FOUND.equals(code) ? ResultStatus.APPROVED
                : DE_NOT_FOUND.equals(code) ? ResultStatus.NOT_FOUND : ResultStatus.UNRECOGNIZED;
        TxContenDE contenido = r.getxContenDE();
        DEQueryResult.Builder b = DEQueryResult.builder()
                .status(status)
                .cdc(cdc)
                .processingDate(r.getdFecProc())
                .authorizationProtocol(contenido == null ? null : contenido.getdProtAut())
                .messages(messages(code, r.getdMsgRes()))
                .operationId(operationId)
                .rawResponse(r.getRespuestaBruta())
                .durationMs(durationMs);
        String raw = r.getRespuestaBruta();
        if (raw != null && !raw.isBlank()) {
            Document doc = parse(raw);
            Element rde = first(doc.getDocumentElement(), "rDE");
            if (rde != null) {
                b.xml(serialize(rde));
                Element digest = first(rde, "DigestValue");
                b.digestValue(digest == null ? null : digest.getTextContent().trim());
            }
            List<String> events = new ArrayList<>();
            NodeList nodes = doc.getElementsByTagNameNS("*", "rContEv");
            for (int i = 0; i < nodes.getLength(); i++) {
                events.add(serialize(nodes.item(i)));
            }
            b.events(events);
        }
        return b.build();
    }

    /**
     * Resultado de la consulta de un lote.
     *
     * @param r           respuesta interpretada.
     * @param batchNumber número de lote consultado.
     * @param operationId id de la operación.
     * @param durationMs  duración.
     * @return el resultado del contrato.
     */
    static BatchQueryResult batchQuery(RespuestaConsultaLoteDE r, String batchNumber, String operationId,
            long durationMs) {
        BatchStatus status = batchQueryStatusOf(r.getdCodResLot());
        List<SendResult> results = new ArrayList<>();
        for (TgResProcLote p : r.getgResProcLoteList()) {
            results.add(SendResult.builder()
                    .status(statusOf(p.getdEstRes(), p.getgResProc()))
                    .cdc(p.getId())
                    .authorizationProtocol(p.getdProtAut())
                    .processingDate(r.getdFecProc())
                    .messages(messages(p.getgResProc()))
                    .operationId(operationId)
                    .durationMs(durationMs)
                    .build());
        }
        return BatchQueryResult.builder()
                .status(status)
                .batchNumber(batchNumber)
                .processingDate(r.getdFecProc())
                .results(results)
                .suggestedWait(status == BatchStatus.IN_PROCESS ? BATCH_QUERY_WAIT : Duration.ZERO)
                .messages(messages(r.getdCodResLot(), r.getdMsgResLot()))
                .operationId(operationId)
                .rawResponse(r.getRespuestaBruta())
                .durationMs(durationMs)
                .build();
    }

    // ------------------------------------------------------------------ envíos (§10.3, §10.4)

    /**
     * Resultado de la recepción de un DE. Un «Rechazado» con 0100, 0161 o 0162 vuelve como
     * {@link ResultStatus#UNRECOGNIZED} (I8-10): resultado desconocido, Maker no refirma y resuelve más tarde.
     *
     * @param r           respuesta interpretada.
     * @param cdc         CDC del documento enviado.
     * @param localDigest {@code DigestValue} del XML enviado.
     * @param operationId id de la operación.
     * @param durationMs  duración.
     * @return el resultado del contrato.
     */
    static SendResult send(RespuestaRecepcionDE r, String cdc, String localDigest, String operationId, long durationMs) {
        TxProtDe p = r.getxProtDE();
        SendResult.Builder b = SendResult.builder()
                .cdc(p == null || p.getId() == null || p.getId().isEmpty() ? cdc : p.getId())
                .operationId(operationId)
                .rawResponse(r.getRespuestaBruta())
                .durationMs(durationMs);
        if (p == null) {
            return b.messages(messages(r.getdCodRes(), r.getdMsgRes())).build();
        }
        return b.status(statusOf(p.getdEstRes(), p.getgResProc()))
                .authorizationProtocol(p.getdProtAut())
                .processingDate(p.getdFecProc())
                .digestValue(p.getdDigVal())
                .digestMismatch(digestMismatch(p.getdDigVal(), localDigest))
                .messages(messages(p.getgResProc()))
                .build();
    }

    /**
     * Indica si un resultado de envío trae el código de SIFEN dado (p. ej. 1001, CDC duplicado).
     *
     * @param r    resultado.
     * @param code código de cuatro dígitos.
     * @return {@code true} si algún mensaje lo trae.
     */
    static boolean hasCode(SendResult r, String code) {
        for (SifenMessage m : r.getMessages()) {
            if (code.equals(m.getCode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * El estado de un documento según una consulta por CDC, como resultado de un envío (1001 o
     * {@code resolve}): {@code isAlreadyRegistered()} verdadero.
     *
     * @param q           resultado de la consulta.
     * @param localDigest {@code DigestValue} del XML local.
     * @param durationMs  duración total de la operación.
     * @return el resultado del contrato.
     */
    static SendResult fromQuery(DEQueryResult q, String localDigest, long durationMs) {
        return SendResult.builder()
                .status(q.getStatus())
                .cdc(q.getCdc())
                .authorizationProtocol(q.getAuthorizationProtocol())
                .processingDate(q.getProcessingDate())
                .digestValue(q.getDigestValue())
                .digestMismatch(digestMismatch(q.getDigestValue(), localDigest))
                .alreadyRegistered(true)
                .messages(q.getMessages())
                .operationId(q.getOperationId())
                .rawResponse(q.getRawResponse())
                .durationMs(durationMs)
                .build();
    }

    /**
     * Resultado de la recepción de un lote.
     *
     * @param r           respuesta interpretada.
     * @param cdcs        CDC en el orden enviado.
     * @param operationId id de la operación.
     * @param durationMs  duración.
     * @return el resultado del contrato.
     */
    static BatchSendResult batchSend(RespuestaRecepcionLoteDE r, List<String> cdcs, String operationId, long durationMs) {
        String code = r.getdCodRes();
        BatchStatus status = BATCH_RECEIVED.equals(code) ? BatchStatus.RECEIVED
                : BATCH_NOT_QUEUED.equals(code) ? BatchStatus.NOT_QUEUED : BatchStatus.UNRECOGNIZED;
        return BatchSendResult.builder()
                .status(status)
                .batchNumber(r.getdProtConsLote())
                .cdcs(cdcs)
                .estimatedProcessingTime(Duration.ofSeconds(Math.max(0, r.getdTpoProces())))
                .messages(messages(code, r.getdMsgRes()))
                .operationId(operationId)
                .rawResponse(r.getRespuestaBruta())
                .durationMs(durationMs)
                .build();
    }

    /**
     * Resultado de la recepción de un evento (el primero de la respuesta: las operaciones del contrato
     * envían uno por pedido). El id del evento es el que se envió; el que repite SIFEN queda en la
     * respuesta bruta.
     *
     * <p>§8.7 (I8-08): un «Rechazado» con 0100, 0161 o 0162 no es un rechazo del evento sino una falla de
     * SIFEN, y vuelve como {@link ResultStatus#UNRECOGNIZED} con sus mensajes. El MT v150 retiró los códigos
     * 0100 a 0107 (control de cambios de §12.2.3) y SIFEN-test los usa como «Error Inesperado» (id 0, sin
     * protocolo); 0161/0162 son «servidor momentáneamente sin respuesta» y «paralizado» (AE02/AE03). Un evento
     * idéntico salvo el {@code Id} y la {@code dFecFirma} quedó registrado (0600) al segundo intento.</p>
     *
     * @param r           respuesta interpretada.
     * @param eventId     id del evento enviado.
     * @param operationId id de la operación.
     * @param durationMs  duración.
     * @return el resultado del contrato.
     * @throws SifenException {@code JBS-RESP-03}/{@code JBS-INT-02} si el pedido enviado no se puede releer.
     */
    static EventResult event(RespuestaRecepcionEvento r, String eventId, String operationId, long durationMs)
            throws SifenException {
        EventResult.Builder b = EventResult.builder()
                .eventId(eventId)
                .processingDate(r.getdFecProc())
                .operationId(operationId)
                .rawResponse(r.getRespuestaBruta())
                .durationMs(durationMs);
        String sent = r.getRequestSent();
        if (sent != null && !sent.isBlank()) {
            Element ev = first(parse(sent).getDocumentElement(), "rGesEve");
            b.eventXml(ev == null ? null : serialize(ev));
        }
        List<TgResProcEVe> list = r.getgResProcEVe();
        if (list == null || list.isEmpty()) {
            return b.messages(messages(r.getdCodRes(), r.getdMsgRes())).build();
        }
        TgResProcEVe p = list.get(0);
        return b.status(statusOf(p.getdEstRes(), p.getgResProc()))
                .eventId(eventId == null || eventId.isEmpty() ? p.getId() : eventId)
                .authorizationProtocol(p.getdProtAut())
                .messages(messages(p.getgResProc()))
                .build();
    }

    /**
     * Estado de una recepción (DE, DE de un lote o evento) a partir de {@code dEstRes} y sus mensajes: un
     * «Rechazado» cuyo primer código es una falla del servidor de SIFEN ({@link #isServerFailure}) vuelve como
     * {@link ResultStatus#UNRECOGNIZED}, no como rechazo (I8-08 en eventos; I8-10, decisión del usuario del
     * 2026-11-25, en DE: «resultado desconocido», SIFEN pudo haberlo procesado y Maker no refirma).
     *
     * @param sifenStatus {@code dEstRes}.
     * @param results     mensajes ({@code gResProc}); puede ser {@code null}.
     * @return el estado.
     */
    static ResultStatus statusOf(String sifenStatus, List<TgResProc> results) {
        ResultStatus status = statusOf(sifenStatus);
        if (status == ResultStatus.REJECTED && results != null && !results.isEmpty() && results.get(0) != null
                && isServerFailure(results.get(0).getdCodRes())) {
            return ResultStatus.UNRECOGNIZED;
        }
        return status;
    }

    /**
     * Códigos con los que SIFEN contesta una recepción sin haberla procesado con certeza (I8-08, I8-10): 0100
     * «Error Inesperado» (retirado del MT v150, control de cambios de §12.2.3) y 0161/0162 (AE02/AE03, servidor
     * momentáneamente sin respuesta o paralizado, «a los mensajes de entrada de cualquiera de los Web Services»).
     * No incluye 0160: en una recepción puede ser un valor inválido (E3-24, E3-26).
     */
    private static boolean isServerFailure(String code) {
        if (code == null) {
            return false;
        }
        return switch (code.trim()) {
            case "0100", "0161", "0162" -> true;
            default -> false;
        };
    }

    /** Verdadero solo si SIFEN informó un digest y difiere del local (D43). */
    private static boolean digestMismatch(String sifen, String local) {
        return sifen != null && !sifen.isBlank() && local != null && !local.isBlank() && !sifen.trim().equals(local.trim());
    }

    // ------------------------------------------------------------------ certificado y firma

    /**
     * Datos del certificado.
     *
     * @param lc    resultado de la carga; {@code null} = sin configuración.
     * @param clock reloj para los días restantes.
     * @return los datos del contrato.
     */
    static CertificateInfo certificate(LoadedCertificate lc, Clock clock) {
        if (lc == null) {
            return CertificateInfo.builder().failureCode("JBS-CFG-01").build();
        }
        if (!lc.isLoaded()) {
            return CertificateInfo.builder().failureCode(lc.getCause().getCode()).build();
        }
        CertificateInfo.Builder b = CertificateInfo.builder()
                .loaded(true)
                .holder(lc.getHolder())
                .documentNumber(lc.getDocumentNumber())
                .rucs(lc.getRucs())
                .notBefore(lc.getNotBefore())
                .notAfter(lc.getNotAfter())
                .digitalSignature(lc.isDigitalSignature());
        if (lc.getNotAfter() != null) {
            b.daysRemaining(ChronoUnit.DAYS.between(clock.instant(), lc.getNotAfter()));
        }
        return b.build();
    }

    /**
     * Veredicto o falla de la verificación de firma.
     *
     * @param v resultado interno.
     * @return el resultado del contrato.
     */
    static SignatureVerification signature(ValidezFirmaDigital v) {
        List<SignatureVerification.Signer> signers = new ArrayList<>();
        if (v.getSujetosCertificado() != null) {
            for (ValidezFirmaDigital.SujetoCertificado s : v.getSujetosCertificado()) {
                if (s != null) {
                    signers.add(SignatureVerification.Signer.of(s.getNumeroDocumento(), s.getRazonSocial()));
                }
            }
        }
        boolean withCode = v.getCode() != null && !v.getCode().isEmpty();
        return SignatureVerification.builder()
                .valid(v.isValido())
                .reason(v.isValido() ? null : v.getMotivoInvalidez())
                .subjects(signers)
                .code(v.getCode())
                .category(withCode ? v.getCategory() : null)
                .correlationId(v.getCorrelationId())
                .build();
    }

    // ------------------------------------------------------------------ XML

    private static Document parse(String xml) throws SifenException {
        try {
            // E4-02: la fábrica XML única y endurecida de la librería (sin DOCTYPE ni entidades externas)
            return SifenUtil.newSecureDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw SifenException.of(ErrorCode.JBS_RESP_03, "relectura de la respuesta: " + e.getClass().getName(), e);
        }
    }

    private static Element first(Element root, String localName) {
        if (root == null) {
            return null;
        }
        NodeList nodes = root.getElementsByTagNameNS("*", localName);
        return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
    }

    private static String serialize(Node node) throws SifenException {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            StringWriter sw = new StringWriter();
            t.transform(new DOMSource(node), new StreamResult(sw));
            return sw.toString();
        } catch (TransformerException | IllegalArgumentException e) {
            throw SifenException.of(ErrorCode.JBS_INT_02, "serialización de la respuesta: " + e.getClass().getName(), e);
        }
    }
}

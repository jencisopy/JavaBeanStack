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
package org.javabeanstack.sifen.simulator;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Lo que contesta {@link SifenSimulator} a un pedido (plan JBSSIF, §8.6): una respuesta grabada, una
 * falla de transporte o una demora. Inmutable y reutilizable entre pedidos y entre hilos.
 *
 * <p>Las respuestas grabadas viven en {@code org/javabeanstack/sifen/simulator/responses/} del
 * {@code test-jar}: sintéticas (armadas desde el Manual Técnico v150) o reales anonimizadas del ambiente
 * de pruebas de SIFEN, con el emisor ficticio {@code 80123456-5}. La recepción aprobada y la rechazada
 * son plantillas: devuelven el CDC y el {@code DigestValue} <b>del DE recibido</b>, como SIFEN.</p>
 */
public final class Scenario {
    /** Código y mensaje del rechazo por omisión (real del ambiente de pruebas). */
    public static final String DEFAULT_REJECTION_CODE = "1107";
    /** Mensaje del rechazo por omisión. */
    public static final String DEFAULT_REJECTION_MESSAGE = "TEST - Fecha de inicio de vigencia del timbrado incorrecta";
    /** Número de lote de las respuestas grabadas de lote. */
    public static final String BATCH_NUMBER = "1234567890123456";
    /** Protocolo de autorización de la recepción aprobada. */
    public static final String AUTHORIZATION_PROTOCOL = "1000000001";

    static final String RESOURCES = "org/javabeanstack/sifen/simulator/responses/";
    private static final String SOAP = "application/soap+xml; charset=utf-8";
    private static final String HTML = "text/html; charset=utf-8";
    private static final String CDC_PLACEHOLDER_DEFAULT = "01801234565001001000000122026112310000000010";
    private static final String DIGEST_PLACEHOLDER_DEFAULT = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    enum Kind { RESPOND, CUT, DELAY }

    private final Kind kind;
    private final int status;
    private final String contentType;
    private final String body;
    private final boolean template;
    private final String code;
    private final String message;
    private final Duration delay;
    private final Scenario then;
    private final String description;

    private Scenario(Kind kind, int status, String contentType, String body, boolean template, String code,
            String message, Duration delay, Scenario then, String description) {
        this.kind = kind;
        this.status = status;
        this.contentType = contentType;
        this.body = body;
        this.template = template;
        this.code = code;
        this.message = message;
        this.delay = delay;
        this.then = then;
        this.description = description;
    }

    private static Scenario respond(int status, String contentType, String body, String description) {
        return new Scenario(Kind.RESPOND, status, contentType, body, false, null, null, null, null, description);
    }

    /**
     * Respuesta con un recurso del {@code test-jar} ({@code responses/<nombre>}).
     *
     * @param status   código HTTP.
     * @param resource nombre del archivo, p. ej. {@code query-ruc-found.xml}.
     * @return el escenario.
     */
    public static Scenario resource(int status, String resource) {
        String ct = resource.endsWith(".html") ? HTML : SOAP;
        return respond(status, ct, read(RESOURCES + resource), resource + " (HTTP " + status + ")");
    }

    /**
     * Respuesta arbitraria (para variantes que no están grabadas).
     *
     * @param status      código HTTP.
     * @param contentType tipo de contenido.
     * @param body        cuerpo; vacío = sin cuerpo.
     * @return el escenario.
     */
    public static Scenario body(int status, String contentType, String body) {
        return respond(status, contentType, body == null ? "" : body, "cuerpo propio (HTTP " + status + ")");
    }

    // ------------------------------------------------------------------ recepción

    /** @return DE aprobado (0260), con el CDC y el {@code DigestValue} del DE recibido. */
    public static Scenario approved() {
        return new Scenario(Kind.RESPOND, 200, SOAP, read(RESOURCES + "receive-de-approved.xml"), true, null, null,
                null, null, "recepción aprobada (0260)");
    }

    /** @return DE rechazado con el código real 1107 (timbrado), con el CDC y el digest del DE recibido. */
    public static Scenario rejected() {
        return rejected(DEFAULT_REJECTION_CODE, DEFAULT_REJECTION_MESSAGE);
    }

    /**
     * DE rechazado con un {@code dCodRes} a elección.
     *
     * @param code    {@code dCodRes} (p. ej. {@code 1306}).
     * @param message {@code dMsgRes}.
     * @return el escenario.
     */
    public static Scenario rejected(String code, String message) {
        return new Scenario(Kind.RESPOND, 200, SOAP, read(RESOURCES + "receive-de-rejected.xml"), true,
                Objects.requireNonNull(code, "code"), Objects.requireNonNull(message, "message"), null, null,
                "recepción rechazada (" + code + ")");
    }

    /** @return lote recibido (0300) con el número {@link #BATCH_NUMBER}. */
    public static Scenario batchReceived() {
        return resource(200, "receive-batch-received.xml");
    }

    /** @return lote no encolado (0301). */
    public static Scenario batchNotQueued() {
        return resource(200, "receive-batch-not-queued.xml");
    }

    /** @return evento registrado (0600). */
    public static Scenario eventRegistered() {
        return resource(200, "event-registered.xml");
    }

    /**
     * Respuesta del servicio de eventos con el evento <b>«Rechazado»</b> y el {@code dCodRes}/{@code dMsgRes}
     * que se pida, con {@code id} 0 y sin {@code dProtAut}, como la contesta SIFEN-test (§8.7). Sirve para probar
     * cómo interpreta la librería cada código: un rechazo de datos (4003, 4009, 4066…) sale {@code REJECTED}; los
     * de servidor (0100, 0161, 0162) salen {@code UNRECOGNIZED} (I8-08).
     *
     * <pre>{@code
     * sim.on(SimulatedService.EVENT, Scenario.eventRejected("4066", "Plazo de cancelación vencido"));
     * EventResult r = sifen.cancel(cdc, motivo);        // r.getStatus() == REJECTED, r.getResponseCode() == "4066"
     * sim.once(SimulatedService.EVENT, Scenario.eventRejected("0100", "Error Inesperado")); // solo el próximo
     * }</pre>
     *
     * @param code    {@code dCodRes}, p. ej. {@code 0100}, {@code 0161}, {@code 0162}, {@code 4009}, {@code 4066}.
     * @param message {@code dMsgRes} (se escapa para XML).
     * @return el escenario.
     */
    public static Scenario eventRejected(String code, String message) {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\">"
                + "<env:Header/><env:Body><ns2:rRetEnviEventoDe xmlns:ns2=\"http://ekuatia.set.gov.py/sifen/xsd\">"
                + "<ns2:dFecProc>2026-11-25T01:15:29-03:00</ns2:dFecProc><ns2:gResProcEVe><ns2:dEstRes>Rechazado</ns2:dEstRes>"
                + "<ns2:id>0</ns2:id><ns2:gResProc><ns2:dCodRes>" + escape(code) + "</ns2:dCodRes><ns2:dMsgRes>" + escape(message)
                + "</ns2:dMsgRes></ns2:gResProc></ns2:gResProcEVe></ns2:rRetEnviEventoDe></env:Body></env:Envelope>";
        return respond(200, SOAP, xml, "evento rechazado (" + code + ")");
    }

    // ------------------------------------------------------------------ consultas

    /** @return lote en procesamiento (0361). */
    public static Scenario batchInProcess() {
        return resource(200, "query-batch-in-process.xml");
    }

    /** @return lote concluido (0362) con un DE aprobado. */
    public static Scenario batchCompleted() {
        return resource(200, "query-batch-completed.xml");
    }

    /** Código de un DE aprobado dentro de un lote. */
    public static final String APPROVED_CODE = "0260";

    /**
     * Lote concluido (0362) con un resultado <b>aprobado</b> (0260) por cada CDC dado, en ese orden: para cerrar
     * el ciclo de un lote enviado en la misma prueba. Es {@link #batchCompletedWith(Map)} con todos en 0260.
     *
     * @param cdcs CDC del lote.
     * @return el escenario.
     */
    public static Scenario batchCompleted(java.util.List<String> cdcs) {
        Map<String, String> todos = new LinkedHashMap<>();
        for (String cdc : cdcs) {
            todos.put(Objects.requireNonNull(cdc, "cdc"), APPROVED_CODE);
        }
        return batchCompletedWith(todos);
    }

    /**
     * Lote concluido (0362) con un resultado por CDC y el código que se indique para cada uno, en el orden del
     * mapa (usar un {@code LinkedHashMap} si el orden importa). Con {@link #APPROVED_CODE} (0260) el DE sale
     * «Aprobado» con un {@code dProtAut} propio; con cualquier otro código, «Rechazado» con ese {@code dCodRes} y
     * sin protocolo. Para los rechazos de servidor (0160, 0161, 0162) la librería da {@code UNRECOGNIZED}
     * (I8-10); para uno de datos (1306…), {@code REJECTED}.
     *
     * <pre>{@code
     * BatchSendResult lote = sifen.sendBatch(List.of(a, b, c));
     * Map<String, String> codigos = new LinkedHashMap<>();
     * codigos.put(a.getCdc(), Scenario.APPROVED_CODE);   // aprobado
     * codigos.put(b.getCdc(), "1306");                   // rechazado: RUC del receptor inexistente
     * codigos.put(c.getCdc(), "0162");                   // servidor paralizado: resultado no reconocido
     * sim.on(SimulatedService.QUERY_BATCH, Scenario.batchCompletedWith(codigos));
     * BatchQueryResult r = sifen.queryBatch(lote.getBatchNumber());   // COMPLETED, un SendResult por CDC
     * }</pre>
     *
     * @param cdcToCode CDC → {@code dCodRes} de su resultado.
     * @return el escenario.
     */
    public static Scenario batchCompletedWith(Map<String, String> cdcToCode) {
        Objects.requireNonNull(cdcToCode, "cdcToCode");
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (Map.Entry<String, String> e : cdcToCode.entrySet()) {
            String cdc = Objects.requireNonNull(e.getKey(), "cdc");
            String code = Objects.requireNonNull(e.getValue(), "código de " + cdc);
            boolean aprobado = APPROVED_CODE.equals(code);
            sb.append("<ns2:gResProcLote><ns2:id>").append(escape(cdc)).append("</ns2:id><ns2:dEstRes>")
                    .append(aprobado ? "Aprobado" : "Rechazado").append("</ns2:dEstRes>");
            if (aprobado) {
                sb.append("<ns2:dProtAut>").append(2000000000L + (++n)).append("</ns2:dProtAut>");
            }
            sb.append("<ns2:gResProc><ns2:dCodRes>").append(escape(code)).append("</ns2:dCodRes><ns2:dMsgRes>")
                    .append(aprobado ? "Autorización del DE satisfactoria" : "Rechazo de prueba del simulador (" + escape(code) + ")")
                    .append("</ns2:dMsgRes></ns2:gResProc></ns2:gResProcLote>");
        }
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\">"
                + "<env:Header/><env:Body><ns2:rResEnviConsLoteDe xmlns:ns2=\"http://ekuatia.set.gov.py/sifen/xsd\">"
                + "<ns2:dFecProc>2026-11-25T10:05:00-03:00</ns2:dFecProc><ns2:dCodResLot>0362</ns2:dCodResLot>"
                + "<ns2:dMsgResLot>Procesamiento de lote concluido</ns2:dMsgResLot>" + sb
                + "</ns2:rResEnviConsLoteDe></env:Body></env:Envelope>";
        return respond(200, SOAP, xml, "lote concluido con " + cdcToCode.size() + " CDC y sus códigos (0362)");
    }

    /** @return HTTP 200 sin cuerpo. */
    public static Scenario emptyBody() {
        return respond(200, SOAP, "", "HTTP 200 sin cuerpo");
    }

    /** @return lote concluido (0362), respuesta real anonimizada: 8 DE, 7 aprobados y 1 rechazado 1306. */
    public static Scenario batchCompletedReal() {
        return resource(200, "query-batch-completed-real.xml");
    }

    /**
     * DE encontrado (0422) con el {@code rDE} firmado <b>en línea</b> dentro de {@code xContenDE}, como lo devuelve
     * SIFEN-test (I8-06), su protocolo y un evento de cancelación; los valores esperados están en
     * {@code responses/query-de-found.properties}.
     *
     * @return el escenario.
     */
    public static Scenario deFound() {
        return resource(200, "query-de-found.xml");
    }

    /**
     * La misma 0422 con el {@code xContenDE} <b>escapado</b> (la forma que describe el Manual Técnico y que
     * la librería desescapa): variante de {@link #deFound()}.
     *
     * @return el escenario.
     */
    public static Scenario deFoundEscaped() {
        return resource(200, "query-de-found-escaped.xml");
    }

    /** @return DE no encontrado o rechazado (0420). */
    public static Scenario deNotFound() {
        return resource(200, "query-de-not-found.xml");
    }

    /** @return RUC encontrado (0502), facturador electrónico. */
    public static Scenario rucFound() {
        return resource(200, "query-ruc-found.xml");
    }

    /** @return RUC inexistente (0500). */
    public static Scenario rucNotFound() {
        return resource(200, "query-ruc-not-found.xml");
    }

    // ------------------------------------------------------------------ fallas

    /** @return SOAP Fault con HTTP 500. */
    public static Scenario soapFault() {
        return resource(500, "soap-fault.xml");
    }

    /** @return HTTP 502 con la página HTML de un proxy. */
    public static Scenario badGateway() {
        return resource(502, "proxy-502.html");
    }

    /** @return HTTP 504 con la página HTML de un proxy. */
    public static Scenario gatewayTimeout() {
        return resource(504, "proxy-504.html");
    }

    /** @return HTTP 200 con una página HTML (un portal cautivo): no es SOAP. */
    public static Scenario notSoap() {
        return resource(200, "not-soap.html");
    }

    /** @return la conexión se cierra después de leer el pedido, sin respuesta. */
    public static Scenario connectionCut() {
        return new Scenario(Kind.CUT, 0, null, null, false, null, null, null, null, "conexión cortada");
    }

    /**
     * Demora la respuesta.
     *
     * @param delay cuánto espera antes de contestar.
     * @param then  lo que contesta después.
     * @return el escenario.
     */
    public static Scenario delayed(Duration delay, Scenario then) {
        return new Scenario(Kind.DELAY, 0, null, null, false, null, null, Objects.requireNonNull(delay, "delay"),
                Objects.requireNonNull(then, "then"), "demora de " + delay.toMillis() + " ms y " + then.description);
    }

    /**
     * SIFEN no contesta: el pedido queda esperando dos minutos (más que el tiempo de lectura de 45 s de
     * la configuración) o hasta que se cierra el simulador.
     *
     * @return el escenario.
     */
    public static Scenario noResponse() {
        return delayed(Duration.ofMinutes(2), approved());
    }

    // ------------------------------------------------------------------ uso interno del simulador

    Kind kind() {
        return kind;
    }

    int status() {
        return status;
    }

    String contentType() {
        return contentType;
    }

    Duration delay() {
        return delay;
    }

    Scenario then() {
        return then;
    }

    /**
     * Cuerpo para un pedido: las plantillas toman el CDC y el {@code DigestValue} del DE recibido.
     *
     * @param request pedido.
     * @return el cuerpo en UTF-8.
     */
    byte[] bodyFor(RecordedRequest request) {
        String b = body;
        if (template) {
            String cdc = request.getCdc();
            String digest = request.text("DigestValue");
            b = b.replace("{{cdc}}", cdc == null ? CDC_PLACEHOLDER_DEFAULT : cdc)
                    .replace("{{digest}}", digest == null ? DIGEST_PLACEHOLDER_DEFAULT : digest.trim());
            if (code != null) {
                b = b.replace("{{code}}", code).replace("{{message}}", escape(message));
            }
        }
        return b.getBytes(StandardCharsets.UTF_8);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static String read(String resource) {
        try (InputStream in = Scenario.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Falta el recurso del simulador " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String toString() {
        return "Scenario[" + description + "]";
    }
}

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

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.sifen.api.BatchQueryResult;
import org.javabeanstack.sifen.api.BatchSendResult;
import org.javabeanstack.sifen.api.CertificateInfo;
import org.javabeanstack.sifen.api.ConnectionDiagnosis;
import org.javabeanstack.sifen.api.DEQueryResult;
import org.javabeanstack.sifen.api.EventResult;
import org.javabeanstack.sifen.api.NumberRange;
import org.javabeanstack.sifen.api.ResultStatus;
import org.javabeanstack.sifen.api.RucQueryResult;
import org.javabeanstack.sifen.api.SendResult;
import org.javabeanstack.sifen.api.SignatureVerification;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.EventosDE;
import org.javabeanstack.sifen.core.beans.ValidezFirmaDigital;
import org.javabeanstack.sifen.core.beans.response.RespuestaConsultaDE;
import org.javabeanstack.sifen.core.beans.response.RespuestaConsultaLoteDE;
import org.javabeanstack.sifen.core.beans.response.RespuestaConsultaRUC;
import org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionDE;
import org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionEvento;
import org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionLoteDE;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.Operation;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.fields.request.event.TrGesEve;
import org.javabeanstack.sifen.core.validation.ConfigValidator;
import org.javabeanstack.sifen.core.validation.DEValidator;
import org.javabeanstack.sifen.core.validation.EventValidator;
import org.javabeanstack.sifen.core.validation.SifenValidationException;
import org.javabeanstack.sifen.core.validation.ValidationContext;
import org.javabeanstack.sifen.core.validation.ValidationError;
import org.javabeanstack.sifen.core.validation.ValidationResult;
import org.javabeanstack.sifen.internal.Constants;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.internal.facade.Inputs;
import org.javabeanstack.sifen.internal.facade.Operations;
import org.javabeanstack.sifen.internal.helpers.SignatureHelper;
import org.javabeanstack.sifen.internal.http.BatchChecks;
import org.javabeanstack.sifen.internal.http.ConnectionProbe;
import org.javabeanstack.sifen.internal.http.OperationContext;
import org.javabeanstack.sifen.internal.http.SifenConnection;
import org.javabeanstack.sifen.internal.request.ReqConsDe;
import org.javabeanstack.sifen.internal.request.ReqConsLoteDe;
import org.javabeanstack.sifen.internal.request.ReqConsRuc;
import org.javabeanstack.sifen.internal.request.ReqRecDe;
import org.javabeanstack.sifen.internal.request.ReqRecEventoDe;
import org.javabeanstack.sifen.internal.request.ReqRecLoteDe;
import org.javabeanstack.sifen.internal.util.SifenUtil;
import org.javabeanstack.sifen.spi.ISendRegistry;

/**
 * Implementación de {@link ISifenClient}: un cliente por empresa (plan JBSSIF, Fase 10, §10.2–§10.4 y
 * §10.7, D34, D41, D43; M0-04, M3-34, M3-26, M3-35).
 *
 * <p>Cada cliente tiene <b>su</b> copia de la configuración (tomada al crearlo), <b>su</b> conexión con
 * SIFEN ({@code SifenConnection}: contexto TLS propio, §5.2) y <b>su</b> contador de pedidos ({@code dId}).
 * El bloqueo de envíos en curso por CDC y la secuencia de {@code Id} de evento son del
 * {@link SifenClientRegistry} que lo creó (los comparten sus clientes, también el que reemplaza a otro tras
 * {@code invalidate}), o del propio cliente si se creó sin registro. Genera un id de operación al entrar a
 * cada método, que va al resultado, a todas las líneas del log de la operación (también las del transporte,
 * por {@code OperationContext}) y, si falla, a {@code SifenException.getCorrelationId()}. Es seguro entre
 * hilos y no guarda ningún estado que mezcle empresas.</p>
 *
 * <p>No llama a la fachada heredada {@code Sifen}: usa los validadores y las peticiones internas, y pasa
 * su conexión a cada pedido. Lo único que todavía es compartido entre clientes por debajo es la caché de
 * certificados de la validación y de la firma ({@code CertificateStore}, por contenido: I5-02, I10-15).</p>
 *
 * <p>Idempotencia (D41, D43): {@link #send(SignedDocument)} transmite el XML firmado tal cual, una vez, y no
 * reintenta una recepción. Un 1001 («CDC duplicado») se resuelve con una consulta: con 0422 es el estado
 * real; si no, {@code JBS-RESP-08} con resultado desconocido, nunca un rechazo. {@link #resolve(SignedDocument)}
 * consulta y solo reenvía si SIFEN no tiene el documento (0420); cualquier otro código y toda falla de
 * {@code resolve} son resultado desconocido. Lo que falla después de transmitir, dentro de la misma
 * operación, también. Un CDC tiene a lo sumo un envío en curso ({@code JBS-ENT-08}); el
 * {@link ISendRegistry} de la aplicación, si hay, autoriza cada transmisión ({@code JBS-ENT-13}); un
 * documento de otro ambiente no se transmite ({@code JBS-ENT-12}).</p>
 */
public final class SifenClient implements ISifenClient {
    private static final Logger LOGGER = LogManager.getLogger(SifenClient.class);

    private final SifenConfig config;
    private final SifenConnection connection;
    private final ISendRegistry sendRegistry;
    private final Clock clock;
    private final Supplier<String> eventIds;
    private final SifenClientShared shared;
    private final AtomicLong requestCounter = new AtomicLong(1);
    private final SecureRandom random = new SecureRandom();
    private final AtomicBoolean closed = new AtomicBoolean();
    /** Operaciones en curso: un cliente retirado por el registro se cierra al terminar la última (M10-03). */
    private final AtomicInteger active = new AtomicInteger();
    private volatile boolean retired;

    /** Cuerpo de una operación del cliente: recibe el id de operación y el instante de entrada. */
    @FunctionalInterface
    private interface Body<T> {
        T run(String operationId, long startNanos) throws SifenException;
    }

    private SifenClient(SifenConfig config, ISendRegistry sendRegistry, Clock clock, Supplier<String> eventIds,
            SifenClientShared shared) {
        this.config = config;
        this.connection = SifenConnection.create();
        this.sendRegistry = sendRegistry;
        this.clock = clock;
        this.shared = shared != null ? shared : new SifenClientShared();
        this.eventIds = eventIds != null ? eventIds : () -> this.shared.nextEventId(this.clock);
    }

    /**
     * Crea un cliente para una empresa, sin registro (un {@code main}, una prueba). En una aplicación
     * con varias empresas se usa {@link SifenClientRegistry}.
     *
     * <p>Valida la configuración y el certificado (lista completa en la excepción) y guarda una copia:
     * cambiar después {@code config} no afecta al cliente.</p>
     *
     * @param config configuración de la empresa.
     * @return el cliente.
     * @throws SifenException {@code SifenValidationException} con todos los errores {@code JBS-CFG-*}
     *                        y {@code JBS-CERT-*} si la configuración no sirve ({@code JBS-CFG-01} si es
     *                        {@code null}).
     */
    public static ISifenClient create(SifenConfig config) throws SifenException {
        return create(config, null);
    }

    /**
     * Crea un cliente con el registro de envíos de la aplicación (PROVISIONAL, como {@link ISendRegistry}):
     * antes de cada transmisión de un DE o de un lote el cliente le pregunta si puede transmitir.
     *
     * @param config       configuración de la empresa.
     * @param sendRegistry registro de envíos; {@code null} = sin registro.
     * @return el cliente.
     * @throws SifenException como {@link #create(SifenConfig)}.
     */
    @Provisional
    public static ISifenClient create(SifenConfig config, ISendRegistry sendRegistry) throws SifenException {
        return create(config, sendRegistry, Clock.systemUTC(), null, null);
    }

    /**
     * Crea un cliente con reloj, numeración de eventos y estado compartido propios (el registro pasa el suyo;
     * las pruebas de CA10-07 fijan el {@code Id} y el {@code dFecFirma} del golden).
     */
    static SifenClient create(SifenConfig config, ISendRegistry sendRegistry, Clock clock, Supplier<String> eventIds,
            SifenClientShared shared) throws SifenException {
        return run(Operation.LOAD_CONFIG, null, (id, start) -> {
            // la original (desde §5.2 la copia conserva también las listas de la carga, I10-08)
            ValidationResult result = ConfigValidator.standard().validate(config);
            if (!result.isValid()) {
                throw SifenValidationException.ofConfiguration(result);
            }
            for (ValidationError warning : result.getWarnings()) {
                LOGGER.warn("[{}] {} {}", id, warning.getCode(), warning.getMessage());
            }
            SifenClient client = new SifenClient(SifenClientConfigCopy.of(config), sendRegistry,
                    clock == null ? Clock.systemUTC() : clock, eventIds, shared);
            LOGGER.info("[{}] Cliente SIFEN creado (ambiente {})", id, client.config.getEnvironment());
            return client;
        });
    }

    // ------------------------------------------------------------------ identidad y diagnóstico

    /** {@inheritDoc} */
    @Override
    public String getVersion() {
        return Constants.SDK_CURRENT_VERSION;
    }

    /** {@inheritDoc} */
    @Override
    public SifenConfig getConfig() {
        return SifenClientConfigCopy.of(config);
    }

    /** {@inheritDoc} */
    @Override
    public ValidationResult validateConfig() {
        return ConfigValidator.standard().validate(config, true);
    }

    /** {@inheritDoc} */
    @Override
    public CertificateInfo getCertificateInfo() {
        try {
            return SifenClientResults.certificate(ConfigValidator.certificate(config), clock);
        } catch (RuntimeException e) {
            String id = Operations.newId();
            LOGGER.error("[{}] {} al leer los datos del certificado ({})", id, ErrorCode.JBS_INT_00.getCode(),
                    e.getClass().getName());
            return CertificateInfo.builder().failureCode(ErrorCode.JBS_INT_00.getCode()).build();
        }
    }

    /** {@inheritDoc} */
    @Override
    public ConnectionDiagnosis checkConnection() {
        String id = Operations.newId();
        active.incrementAndGet();
        try (OperationContext.Scope scope = OperationContext.open(id)) {
            return ConnectionProbe.diagnose(config, connection, id);
        } catch (RuntimeException e) {
            LOGGER.error("[{}] {} en el diagnóstico de la conexión ({})", id, ErrorCode.JBS_INT_00.getCode(),
                    e.getClass().getName());
            return ConnectionDiagnosis.of(List.of(ConnectionDiagnosis.Step.failed(ConnectionProbe.CONFIG, 0,
                    ErrorCode.JBS_INT_00.getCode(), ErrorCode.JBS_INT_00.message())), id);
        } finally {
            leave();
        }
    }

    // ------------------------------------------------------------------ validación previa

    /**
     * {@inheritDoc}
     *
     * <p>Sin {@code dFecFirma}, valida con la que le pondría {@link #sign(DocumentoElectronico)} (la misma regla,
     * del mismo reloj del cliente) sin cargarla en el documento (I8-27).</p>
     */
    @Override
    public ValidationResult validateDE(DocumentoElectronico de) {
        return DEValidator.standard().validate(de, context(), de == null ? null : signatureDateOf(de));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sin {@code dFecFirma} o sin {@code Id}, valida con los que le pondría {@link #sendEvents(EventosDE)}: la hora
     * de Asunción del mismo reloj del cliente y el {@code Id} que daría ahora la numeración de eventos, leído
     * <b>sin avanzarla</b>; los eventos no se modifican (I8-34).</p>
     */
    @Override
    @Provisional
    public ValidationResult validateEvents(EventosDE events) {
        return EventValidator.standard().validate(events, context(), now(), supposedEventId());
    }

    // ------------------------------------------------------------------ emisión en dos pasos

    /**
     * {@inheritDoc}
     *
     * <p>Si la aplicación no cargó {@code dFecFirma} ({@code DE}) o {@code dCodSeg} ({@code gOpeDE}), el
     * cliente los completa <b>en el mismo {@code DocumentoElectronico}</b> (la hora de Asunción, al
     * segundo; nueve dígitos al azar) antes de firmar; si la firma falla, los vuelve a dejar como estaban
     * (M10-02). La validación del contenido corre según {@code isPreValidationEnabled()} (D20).</p>
     */
    @Override
    public SignedDocument sign(DocumentoElectronico de) throws SifenException {
        return operation(Operation.SIGN, null, (id, start) -> {
            ConfigValidator.standard().require(config);
            if (de == null) {
                DEValidator.standard().validate(null, context()).throwIfInvalid();   // JBS-VAL-01
            }
            LocalDateTime fechaAntes = de.getdFecFirma();
            String codSegAntes = de.getgOpeDE() == null ? null : de.getgOpeDE().getdCodSeg();
            boolean ok = false;
            try {
                if (fechaAntes == null) {
                    de.setdFecFirma(signatureDateOf(de));
                }
                if (de.getgOpeDE() != null && (codSegAntes == null || codSegAntes.isBlank())) {
                    de.getgOpeDE().setdCodSeg(String.format("%09d", 1 + random.nextInt(999_999_999)));
                }
                String xml = de.generateSignedXml(config);
                SignedDocument doc = signed(SifenUtil.readSignedRDE(xml), config.getEnvironment());
                LOGGER.info("[{}] DE firmado (CDC {})", id, doc.getCdc());
                ok = true;
                return doc;
            } finally {
                if (!ok) {
                    de.setdFecFirma(fechaAntes);
                    if (de.getgOpeDE() != null) {
                        de.getgOpeDE().setdCodSeg(codSegAntes);
                    }
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>El ambiente del documento sale de la URL del QR (consulta de pruebas o de producción); si no es
     * ninguna de las dos, el de la configuración del cliente (el envío lo rechaza igual: {@code JBS-ENT-12}).</p>
     */
    @Override
    public SignedDocument readSigned(String signedXml) throws SifenException {
        return operation(Operation.READ_XML, null, (id, start) -> {
            SifenUtil.SignedRDE r = SifenUtil.readSignedRDE(signedXml);
            SifenConfig.Environment env = environmentOf(r.qrLink());
            return signed(r, env != null ? env : config.getEnvironment());
        });
    }

    /** {@inheritDoc} */
    @Override
    public SendResult send(SignedDocument document) throws SifenException {
        return operation(Operation.SEND_DE, cdcOf(document), (id, start) -> {
            ConfigValidator.standard().require(config);
            SifenUtil.SignedRDE r = readForSend(document);
            acquire(List.of(r.cdc()), id);
            try {
                authorize(r.cdc(), id);
                return transmitDE(r, id, start);
            } finally {
                release(List.of(r.cdc()), id);
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Toda excepción de {@code resolve} sale con {@code isOutcomeUnknown()} verdadero: el documento ya
     * estaba en duda (D43).</p>
     */
    @Override
    public SendResult resolve(SignedDocument document) throws SifenException {
        return operation(Operation.SEND_DE, cdcOf(document), (id, start) -> {
            try {
                ConfigValidator.standard().require(config);
                SifenUtil.SignedRDE r = readForSend(document);
                acquire(List.of(r.cdc()), id);
                try {
                    DEQueryResult q = queryDEOnce(r.cdc(), id, start);
                    String code = q.getResponseCode();
                    if (SifenClientResults.DE_FOUND.equals(code)) {
                        return SifenClientResults.fromQuery(q, r.digestValue(), elapsedMs(start));
                    }
                    if (SifenClientResults.DE_NOT_FOUND.equals(code)) {
                        LOGGER.info("[{}] SIFEN no tiene el DE (0420): se envía una vez", id);
                        authorize(r.cdc(), id);
                        return transmitDE(r, id, start);
                    }
                    // ni 0422 ni 0420: no se reenvía y el documento sigue en duda (E10-04, D43)
                    throw SifenException.ofResponse(ErrorCode.JBS_RESP_08, "resolve: la consulta dio " + code, code);
                } finally {
                    release(List.of(r.cdc()), id);
                }
            } catch (SifenException e) {
                throw e.markSent();
            } catch (RuntimeException | LinkageError e) {
                throw Operations.translate(e, Operation.SEND_DE).markSent();
            }
        });
    }

    // ------------------------------------------------------------------ lote

    /** {@inheritDoc} */
    @Override
    public BatchSendResult sendBatch(List<SignedDocument> documents) throws SifenException {
        return operation(Operation.SEND_BATCH, null, (id, start) -> {
            ConfigValidator.standard().require(config);
            if (documents == null || documents.isEmpty()) {
                throw SifenException.of(ErrorCode.JBS_ENT_11, "lote vacío", null, "sin documentos");
            }
            // copia propia: no se pregunta contains(null) a una lista que puede ser inmutable (E10-01)
            List<SignedDocument> docs = new ArrayList<>(documents.size());
            for (int i = 0; i < documents.size(); i++) {
                SignedDocument d = documents.get(i);
                if (d == null) {
                    throw SifenException.of(ErrorCode.JBS_ENT_11, "elemento nulo en " + (i + 1), null,
                            "el documento " + (i + 1) + " es nulo");
                }
                docs.add(d);
            }
            if (docs.size() > Inputs.MAX_BATCH_SIZE) {
                throw SifenException.of(ErrorCode.JBS_ENT_01, "documentos en el lote: " + docs.size(), null, docs.size());
            }
            List<String> cdcs = new ArrayList<>();
            List<String> xmls = new ArrayList<>();
            Map<String, Integer> seen = new HashMap<>();
            for (int i = 0; i < docs.size(); i++) {
                SifenUtil.SignedRDE r = readForSend(docs.get(i));
                Integer before = seen.putIfAbsent(r.cdc(), i);
                if (before != null) {
                    throw SifenException.of(ErrorCode.JBS_ENT_02, "posiciones " + before + " y " + i, null,
                            before + 1, i + 1);
                }
                cdcs.add(r.cdc());
                xmls.add(r.xml());
            }
            BatchChecks.requireSingleTypeAndIssuer(cdcs);   // JBS-ENT-09
            acquire(cdcs, id);
            try {
                for (String cdc : cdcs) {
                    authorize(cdc, id);
                }
                long dId = requestCounter.getAndIncrement();
                LOGGER.info("[{}] Envío de lote de {} DE (pedido {})", id, cdcs.size(), dId);
                ReqRecLoteDe request = new ReqRecLoteDe(dId, config);
                request.setSignedXmlList(xmls);
                RespuestaRecepcionLoteDE resp = (RespuestaRecepcionLoteDE) request.makeRequest(context(),
                        config.getSendBatchPath(), connection);
                return afterTransmit(Operation.SEND_BATCH, () -> SifenClientResults.batchSend(resp, cdcs, id, elapsedMs(start)));
            } finally {
                release(cdcs, id);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public BatchQueryResult queryBatch(String batchNumber) throws SifenException {
        return operation(Operation.QUERY_BATCH, batchNumber, (id, start) -> {
            ConfigValidator.standard().require(config);
            Inputs.requireBatchNumber(batchNumber);
            long dId = requestCounter.getAndIncrement();
            LOGGER.info("[{}] Consulta de lote (pedido {})", id, dId);
            ReqConsLoteDe request = new ReqConsLoteDe(dId, config);
            request.setdProtConsLote(batchNumber);
            RespuestaConsultaLoteDE r = (RespuestaConsultaLoteDE) request.makeRequest(context(),
                    config.getQueryBatchPath(), connection);
            return SifenClientResults.batchQuery(r, batchNumber, id, elapsedMs(start));
        });
    }

    // ------------------------------------------------------------------ consultas

    /** {@inheritDoc} */
    @Override
    public DEQueryResult queryDE(String cdc) throws SifenException {
        return operation(Operation.QUERY_DE, cdc, (id, start) -> {
            ConfigValidator.standard().require(config);
            Inputs.requireCdc(cdc);
            return queryDEOnce(cdc, id, start);
        });
    }

    /** {@inheritDoc} */
    @Override
    public RucQueryResult queryRuc(String ruc) throws SifenException {
        return operation(Operation.QUERY_RUC, ruc, (id, start) -> {
            ConfigValidator.standard().require(config);
            Inputs.requireRuc(ruc);
            long dId = requestCounter.getAndIncrement();
            LOGGER.info("[{}] Consulta de RUC (pedido {})", id, dId);
            ReqConsRuc request = new ReqConsRuc(dId, config);
            request.setdRUCCons(ruc);
            RespuestaConsultaRUC r = (RespuestaConsultaRUC) request.makeRequest(context(), config.getQueryRucPath(),
                    connection);
            return SifenClientResults.rucQuery(r, id, elapsedMs(start));
        });
    }

    // ------------------------------------------------------------------ eventos

    /** {@inheritDoc} */
    @Override
    public EventResult cancel(String cdc, String reason) throws SifenException {
        return operation(Operation.SEND_EVENTS, cdc, (id, start) -> {
            ConfigValidator.standard().require(config);
            Inputs.requireCdc(cdc);
            EventosDE events = ReqRecEventoDe.cancellation(eventIds.get(), now(), cdc, reason);
            return transmitEvents(events, id, start);
        });
    }

    /** {@inheritDoc} */
    @Override
    public EventResult invalidateNumbers(NumberRange range) throws SifenException {
        return operation(Operation.SEND_EVENTS, null, (id, start) -> {
            ConfigValidator.standard().require(config);
            if (range == null) {
                throw SifenException.of(ErrorCode.JBS_ENT_10, "rango nulo", null, "falta el rango");
            }
            int timbrado;
            try {
                timbrado = Integer.parseInt(range.getTimbrado().trim());
            } catch (NumberFormatException e) {
                throw SifenException.of(ErrorCode.JBS_ENT_10, "timbrado no numérico", null,
                        "el timbrado debe ser numérico");
            }
            if (range.getDocumentType() == null) {
                throw SifenException.of(ErrorCode.JBS_ENT_10, "sin tipo de documento", null,
                        "falta el tipo de documento");
            }
            EventosDE events = ReqRecEventoDe.numberInvalidation(eventIds.get(), now(), timbrado,
                    range.getEstablishment(), range.getIssuePoint(), range.getFrom(), range.getTo(),
                    range.getDocumentType(), range.getReason());
            return transmitEvents(events, id, start);
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Exactamente un evento por llamada ({@code JBS-ENT-14} si no, sin contactar a SIFEN; D43). Un evento
     * sin {@code Id} recibe uno de la numeración del cliente; uno sin {@code dFecFirma}, la hora de Asunción.</p>
     */
    @Override
    @Provisional
    public EventResult sendEvents(EventosDE events) throws SifenException {
        return operation(Operation.SEND_EVENTS, null, (id, start) -> {
            ConfigValidator.standard().require(config);
            int count = events == null || events.getrGesEveList() == null ? 0 : events.getrGesEveList().size();
            if (count != 1) {
                throw SifenException.of(ErrorCode.JBS_ENT_14, "eventos recibidos: " + count, null, count);
            }
            TrGesEve e = events.getrGesEveList().get(0);
            if (e != null && (e.getId() == null || e.getId().isBlank())) {
                e.setId(eventIds.get());
            }
            if (e != null && e.getdFecFirma() == null) {
                e.setdFecFirma(now());   // la misma hora que supone validateEvents (I8-34)
            }
            return transmitEvents(events, id, start);
        });
    }

    // ------------------------------------------------------------------ firma de un XML recibido

    /** {@inheritDoc} */
    @Override
    public SignatureVerification verifySignature(String xml) {
        try {
            ValidezFirmaDigital v = operation(Operation.VERIFY_SIGNATURE, null,
                    (id, start) -> SignatureHelper.validateSignatureOrThrow(xml, "XML"));
            return SifenClientResults.signature(v);
        } catch (SifenException e) {
            return SifenClientResults.signature(ValidezFirmaDigital.ofError(e));
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Cierra la conexión propia (contexto TLS y certificados del transporte). Después de {@code close()}
     * el cliente no se usa: se obtiene otro del registro.</p>
     */
    @Override
    public void close() {
        connection.close();
        if (closed.compareAndSet(false, true)) {
            LOGGER.debug("Cliente SIFEN cerrado (ambiente {})", config.getEnvironment());
        }
    }

    /**
     * Retira el cliente (lo llama el registro al invalidar su clave o al reemplazarlo): se cierra cuando
     * termina su última operación en curso, o ya si no tiene ninguna (M10-03).
     */
    void retire() {
        retired = true;
        if (active.get() == 0) {
            close();
        }
    }

    @Override
    public String toString() {
        return "SifenClient[" + config.getEnvironment() + ", pedidos=" + (requestCounter.get() - 1) + "]";
    }

    // ------------------------------------------------------------------ envío (§10.3, §10.4, §10.7)

    /**
     * Lee el XML del documento como {@code readSigned} y exige que sus datos sean los del XML y que sea del
     * ambiente del cliente: lo que se transmite es lo que se lee del XML, nunca los campos del
     * {@code SignedDocument}.
     */
    private SifenUtil.SignedRDE readForSend(SignedDocument document) throws SifenException {
        SifenUtil.SignedRDE r = SifenUtil.readSignedRDE(document == null ? null : document.getXml());
        if (!document.getCdc().isEmpty() && !document.getCdc().equals(r.cdc())) {
            throw SifenException.of(ErrorCode.JBS_XML_07, "getCdc() distinto del Id del XML", null, "CDC");
        }
        if (!document.getDigestValue().isEmpty() && !document.getDigestValue().trim().equals(r.digestValue())) {
            throw SifenException.of(ErrorCode.JBS_XML_07, "getDigestValue() distinto del de la firma", null,
                    "DigestValue");
        }
        // §10.7 (D43, M10-01): un documento de otro ambiente, o de ambiente indeterminable, no se transmite
        SifenConfig.Environment env = environmentOf(r.qrLink());
        if (env == null || env != config.getEnvironment()) {
            throw SifenException.of(ErrorCode.JBS_ENT_12, "ambiente del QR " + env + ", del cliente "
                    + config.getEnvironment(), null, env == null ? "desconocido" : env.name(), config.getEnvironment());
        }
        if (config.isPreValidationEnabled()) {
            ConfigValidator.standard().requireConsistency(new DocumentoElectronico(r.xml()), config);   // JBS-CERT-11
        }
        return r;
    }

    /**
     * Transmite el {@code rDE} una vez (sin reintentos, D41). Un 1001 («CDC duplicado») se resuelve con
     * <b>una</b> consulta: con 0422 es el estado real; si no, {@code JBS-RESP-08} con resultado desconocido
     * (D43). Lo que falla después de transmitir sale marcado como enviado (E10-06).
     */
    private SendResult transmitDE(SifenUtil.SignedRDE r, String id, long start) throws SifenException {
        long dId = requestCounter.getAndIncrement();
        LOGGER.info("[{}] Envío de DE (pedido {}, CDC {})", id, dId, r.cdc());
        ReqRecDe request = new ReqRecDe(dId, config);
        request.setSignedXml(r.xml());
        RespuestaRecepcionDE resp = (RespuestaRecepcionDE) request.makeRequest(context(), config.getSendPath(),
                connection);
        return afterTransmit(Operation.SEND_DE, () -> {
            SendResult result = SifenClientResults.send(resp, r.cdc(), r.digestValue(), id, elapsedMs(start));
            if (!SifenClientResults.hasCode(result, SifenClientResults.DE_DUPLICATED)) {
                return result;
            }
            LOGGER.info("[{}] SIFEN ya tiene el CDC (1001): se consulta su estado", id);
            DEQueryResult q = queryDEOnce(r.cdc(), id, start);
            if (SifenClientResults.DE_FOUND.equals(q.getResponseCode())) {
                return SifenClientResults.fromQuery(q, r.digestValue(), elapsedMs(start));
            }
            throw SifenException.ofResponse(ErrorCode.JBS_RESP_08, "1001 y la consulta dio " + q.getResponseCode(),
                    SifenClientResults.DE_DUPLICATED);
        });
    }

    /** Una consulta de DE con el id y la conexión de la operación en curso. */
    private DEQueryResult queryDEOnce(String cdc, String id, long start) throws SifenException {
        long dId = requestCounter.getAndIncrement();
        LOGGER.info("[{}] Consulta de DE (pedido {})", id, dId);
        ReqConsDe request = new ReqConsDe(dId, config);
        request.setdCDC(cdc);
        RespuestaConsultaDE r = (RespuestaConsultaDE) request.makeRequest(context(), config.getQueryDEPath(), connection);
        return SifenClientResults.deQuery(r, cdc, id, elapsedMs(start));
    }

    /** Valida (según la configuración, o siempre si faltan los eventos) y transmite un evento. */
    private EventResult transmitEvents(EventosDE events, String id, long start) throws SifenException {
        if (config.isPreValidationEnabled() || events == null || events.getrGesEveList() == null
                || events.getrGesEveList().isEmpty()) {
            EventValidator.standard().validate(events, context()).throwIfInvalid();
        }
        String eventId = events.getrGesEveList().get(0).getId();
        long dId = requestCounter.getAndIncrement();
        LOGGER.info("[{}] Envío de evento {} (pedido {})", id, eventId, dId);
        ReqRecEventoDe request = new ReqRecEventoDe(dId, config);
        request.setEventoDE(events);
        RespuestaRecepcionEvento resp = (RespuestaRecepcionEvento) request.makeRequest(context(),
                config.getEventPath(), connection);
        return afterTransmit(Operation.SEND_EVENTS, () -> SifenClientResults.event(resp, eventId, id, elapsedMs(start)));
    }

    /** Cuerpo que corre después de una transmisión: toda falla sale marcada como enviada (E10-06, D43). */
    @FunctionalInterface
    private interface AfterTransmit<T> {
        T run() throws SifenException;
    }

    private static <T> T afterTransmit(Operation op, AfterTransmit<T> body) throws SifenException {
        try {
            return body.run();
        } catch (SifenException e) {
            throw e.markSent();
        } catch (RuntimeException | LinkageError e) {
            throw Operations.translate(e, op).markSent();
        }
    }

    /** Toma el envío en curso de todos los CDC, o de ninguno ({@code JBS-ENT-08}). */
    private void acquire(List<String> cdcs, String id) throws SifenException {
        List<String> taken = new ArrayList<>();
        for (String cdc : cdcs) {
            if (!shared.acquire(cdc, id)) {
                release(taken, id);
                throw SifenException.of(ErrorCode.JBS_ENT_08, "envío en curso del CDC", null);
            }
            taken.add(cdc);
        }
    }

    private void release(List<String> cdcs, String id) {
        for (String cdc : cdcs) {
            shared.release(cdc, id);
        }
    }

    /** Pregunta al registro de envíos de la aplicación, si hay ({@code JBS-ENT-13} si no autoriza). */
    private void authorize(String cdc, String id) throws SifenException {
        if (sendRegistry != null && !sendRegistry.allowSend(cdc, id)) {
            throw SifenException.of(ErrorCode.JBS_ENT_13, "el registro de envíos de la aplicación no autorizó", null);
        }
    }

    private SignedDocument signed(SifenUtil.SignedRDE r, SifenConfig.Environment environment) {
        return SignedDocument.builder()
                .xml(r.xml())
                .cdc(r.cdc())
                .qrLink(r.qrLink())
                .digestValue(r.digestValue())
                .signatureDate(r.signatureDate())
                .documentType(r.documentType())
                .number(r.number())
                .issuerRuc(r.issuerRuc())
                .environment(environment)
                .build();
    }

    /** Ambiente según la URL del QR: la consulta de pruebas o la de producción; {@code null} si es otra. */
    private static SifenConfig.Environment environmentOf(String qrLink) {
        SifenConfig dev = new SifenConfig();
        dev.setEnvironment(SifenConfig.Environment.DEV);
        SifenConfig prod = new SifenConfig();
        prod.setEnvironment(SifenConfig.Environment.PROD);
        if (qrLink != null && qrLink.startsWith(dev.getQRQueryUrl())) {
            return SifenConfig.Environment.DEV;
        }
        if (qrLink != null && qrLink.startsWith(prod.getQRQueryUrl())) {
            return SifenConfig.Environment.PROD;
        }
        return null;
    }

    /**
     * La {@code dFecFirma} con que se firma el documento: la suya si la trae; si no, la hora de Asunción al
     * segundo. Punto único de la regla (I8-27): {@code sign} la carga en el documento y {@code validateDE} solo
     * la supone.
     */
    private LocalDateTime signatureDateOf(DocumentoElectronico de) {
        return de.getdFecFirma() != null ? de.getdFecFirma() : now();
    }

    /**
     * El {@code Id} que supone {@code validateEvents} para un evento sin {@code Id} (I8-34, ESC-35): el que daría
     * ahora la numeración de eventos ({@code SifenClientShared}, la del registro si el cliente es de uno), leído
     * <b>sin avanzarla</b>, para que validar no consuma números. No se reserva: {@code sendEvents} toma el suyo de
     * {@code eventIds} al enviar, que puede ser otro; los dos cumplen {@code JBS-EVENTO-01}.
     */
    private String supposedEventId() {
        return shared.peekNextEventId(clock);
    }

    /** Hora de Asunción al segundo ({@code dFecFirma}). */
    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ValidationContext.SIFEN_ZONE).truncatedTo(ChronoUnit.SECONDS);
    }

    /** Contexto de generación nuevo por operación (es mutable: no se comparte entre hilos). */
    private GenerationCtx context() {
        return GenerationCtx.getDefaultFromConfig(config);
    }

    private static String cdcOf(SignedDocument document) {
        return document == null ? null : document.getCdc();
    }

    private static long elapsedMs(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    /** Una operación del cliente: cuenta las operaciones en curso para el cierre diferido de un retirado. */
    private <T> T operation(Operation op, String reference, Body<T> body) throws SifenException {
        active.incrementAndGet();
        try {
            return run(op, reference, body);
        } finally {
            leave();
        }
    }

    private void leave() {
        if (active.decrementAndGet() == 0 && retired) {
            close();
        }
    }

    /**
     * Ejecuta una operación con su id: el id se genera al entrar, va al log de toda la operación
     * ({@code OperationContext}, también las líneas del transporte), al resultado (por el cuerpo) y a la
     * excepción si falla; el envoltorio único de la librería ({@code Operations}) la registra.
     */
    private static <T> T run(Operation op, String reference, Body<T> body) throws SifenException {
        String id = Operations.newId();
        long start = System.nanoTime();
        try (OperationContext.Scope scope = OperationContext.open(id)) {
            LOGGER.debug("[{}] {} inicia", id, op);
            T result = Operations.execute(op, () -> reference, () -> {
                try {
                    return body.run(id, start);
                } catch (SifenException e) {
                    throw e.withContext(op, reference, id);
                } catch (RuntimeException | LinkageError e) {
                    throw Operations.translate(e, op).withContext(op, reference, id);
                }
            });
            LOGGER.info("[{}] {} terminada en {} ms", id, op, elapsedMs(start));
            return result;
        }
    }
}

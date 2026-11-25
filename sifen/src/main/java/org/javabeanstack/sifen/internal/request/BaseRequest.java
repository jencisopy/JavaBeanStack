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
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.SOAPResponse;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.internal.http.OperationContext;
import org.javabeanstack.sifen.internal.http.Retries;
import org.javabeanstack.sifen.internal.http.SifenConnection;
import org.javabeanstack.sifen.internal.response.BaseResponse;
import org.javabeanstack.sifen.internal.facade.Operations;
import org.javabeanstack.sifen.internal.facade.ExceptionTranslator;
import org.javabeanstack.sifen.internal.util.SifenUtil;

import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base de las peticiones a SIFEN: arma el mensaje, lo envía y valida lo mínimo de la respuesta.
 *
 * <p>Fase 5 (plan JBSSIF, §5.2 y §5.4):</p>
 * <ul>
 *   <li>el envío va por una {@link SifenConnection}: la que se pasa a
 *       {@link #makeRequest(GenerationCtx, String, SifenConnection)} (una por cliente) o, sin ella, la
 *       de la instancia de configuración;</li>
 *   <li>una <b>consulta</b> ({@link #isRetryable()} en {@code true}, solo las {@code ReqCons*}) se
 *       reintenta ante una falla rápida de transporte según {@code SifenConfig.getMaxRetries()}
 *       ({@link Retries}); una recepción nunca (D41);</li>
 *   <li>las líneas del log llevan el id de operación ({@link #setOperationId(String)} o
 *       {@link OperationContext}, I10-09) y el XML completo del pedido va solo a DEBUG (M0-03, D27);</li>
 *   <li>con la traza encendida ({@code ExchangeTrace}) la conexión escribe cada intento con los bytes
 *       enviados y su respuesta o su falla.</li>
 * </ul>
 */
abstract class BaseRequest {
    private final long dId;
    private final SifenConfig sifenConfig;
    private String operationId;
    private static final Logger LOGGER = LogManager.getLogger(BaseRequest.class);

    BaseRequest(long dId, SifenConfig sifenConfig) {
        this.dId = dId;
        this.sifenConfig = sifenConfig;
    }

    abstract SOAPMessage setupSoapMessage(GenerationCtx generationCtx) throws SifenException;

    abstract BaseResponse processResponse(SOAPResponse soapResponse) throws SifenException;

    /**
     * Indica si la petición se puede reintentar ante una falla de transporte (§5.2, D41): {@code false}
     * por omisión; lo sobrescriben las consultas.
     *
     * @return {@code true} si es una consulta.
     */
    boolean isRetryable() {
        return false;
    }

    /**
     * Indica si una respuesta que llegó pero no se pudo usar se reintenta, con el mismo límite y la misma
     * espera que las fallas de transporte (§8.7, I8-03; D41: solo las consultas). {@code false} por omisión;
     * se consulta solo si {@link #isRetryable()}.
     *
     * @param e falla al interpretar o verificar la respuesta.
     * @return {@code true} si conviene repetir la consulta.
     */
    boolean isRetryableResponse(SifenException e) {
        return false;
    }

    /**
     * Fija el id de operación de la fachada para las líneas del log de esta petición (I10-09); tiene
     * prioridad sobre el de {@link OperationContext}.
     *
     * @param operationId id; {@code null} = el del hilo.
     */
    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    /**
     * Arma, envía e interpreta la petición por la conexión de la configuración.
     *
     * @param generationCtx contexto de generación.
     * @param url           ruta del servicio (se agrega a la URL base).
     * @return la respuesta interpretada.
     * @throws SifenException con el código de la falla.
     */
    public BaseResponse makeRequest(GenerationCtx generationCtx, String url) throws SifenException {
        return makeRequest(generationCtx, url, null);
    }

    /**
     * Arma, envía e interpreta la petición por la conexión indicada (§5.2).
     *
     * @param generationCtx contexto de generación.
     * @param url           ruta del servicio (se agrega a la URL base).
     * @param connection    conexión del cliente; {@code null} = la de la configuración.
     * @return la respuesta interpretada.
     * @throws SifenException con el código de la falla.
     */
    public BaseResponse makeRequest(GenerationCtx generationCtx, String url, SifenConnection connection) throws SifenException {
        String id = OperationContext.label(operationId);
        try {
            // Preparamos el mensaje
            SOAPMessage message = this.setupSoapMessage(generationCtx);
            message.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");
            message.setProperty(SOAPMessage.CHARACTER_SET_ENCODING, "UTF-8");
            LOGGER.info("[{}] XML generado, se realiza la petición", id);

//             Para obtener el xml
            final StringWriter sw = new StringWriter();

            try {
                TransformerFactory.newInstance().newTransformer().transform(
                        new DOMSource(message.getSOAPPart()),
                        new StreamResult(sw));
            } catch (TransformerException e) {
                throw SifenException.of(ErrorCode.JBS_INT_02, "serialización del pedido: " + e.getClass().getName(), e);
            }

            String xml = sw.toString();
            // M0-03 / D27: el XML completo (datos del receptor) solo a DEBUG, nunca a INFO
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[{}] Este es el xml a enviarse a la SIFEN \n{}", id, xml);
            }
            // Realizamos la consulta
            String requestUrl = SifenUtil.coalesce(sifenConfig.getBaseUrl(), sifenConfig.getLocalBaseUrl()) + url;
            SifenConnection c = connection != null ? connection : SifenConnection.of(sifenConfig);
            BaseResponse response = exchange(c, requestUrl, message, xml, id);
            response.setRequestSent(xml);
            LOGGER.info("[{}] Petición realizada, se formatea la respuesta", id);
            return response;
        } catch (SOAPException e) {
            String msg = "Ocurrió un error al realizan la petición a: " + url + ". Mensaje: " + e.getLocalizedMessage();
            throw SifenException.of(ErrorCode.JBS_INT_01, msg, e);
        }
    }

    /**
     * Envía, interpreta y verifica la respuesta, con los reintentos que correspondan. La traza de cada intento
     * la escribe la conexión, con los bytes que salieron (I5-07); el id explícito de la petición se abre en el
     * hilo mientras dura el envío. Un mismo límite ({@code sifen.reintentos.max}) cubre las fallas de transporte
     * y las respuestas que la petición declara reintentables (I8-03).
     */
    private BaseResponse exchange(SifenConnection c, String requestUrl, SOAPMessage message, String xml, String id)
            throws SifenException {
        int max = isRetryable() ? Math.max(0, sifenConfig.getMaxRetries()) : 0;
        for (int intento = 0; ; intento++) {
            SOAPResponse soapResponse;
            try (OperationContext.Scope scope = operationId == null ? () -> { } : OperationContext.open(operationId)) {
                soapResponse = c.exchange(sifenConfig, requestUrl, message);
            } catch (SifenException e) {
                if (intento >= max || !Retries.isRetryable(e)) {
                    throw e;
                }
                esperar(e, intento, max, id);
                continue;
            }
            try {
                BaseResponse response = this.processResponse(soapResponse);
                verificarRespuesta(response);
                return response;
            } catch (SifenException e) {
                // I3-48: la respuesta llegó, así que el pedido se envió
                e.markSent();
                if (intento >= max || !isRetryableResponse(e)) {
                    throw e;
                }
                esperar(e, intento, max, id);
            } catch (RuntimeException e) {
                // §3.8: un campo con formato inesperado (JBS-RESP-06) u otra falla al interpretar (JBS-RESP-03)
                ErrorCode codigo = ExceptionTranslator.translate(e, ExceptionTranslator.Stage.RESPONSE)
                        .orElse(ErrorCode.JBS_RESP_03);
                throw SifenException.of(codigo, Operations.detail(e), e).markSent();
            }
        }
    }

    /** Avisa y espera antes del próximo intento; si el hilo se interrumpe, vuelve a lanzar la falla. */
    private void esperar(SifenException e, int intento, int max, String id) throws SifenException {
        long espera = Retries.delayMs(sifenConfig, intento);
        String sifen = e.getSifenResponseCode() == null ? "" : " (SIFEN " + e.getSifenResponseCode() + ")";
        LOGGER.warn("[{}] {}{} en la consulta (pedido {}): reintento {} de {} en {} ms", id, e.getCode(), sifen, dId,
                intento + 1, max, espera);
        if (!Retries.pause(espera)) {
            LOGGER.warn("[{}] Reintento cancelado: el hilo fue interrumpido", id);
            throw e;
        }
    }

    /**
     * Verifica que la respuesta interpretada tenga lo mínimo para usarla (§3.8): por omisión nada;
     * cada petición exige su código de resultado ({@code JBS-RESP-04}) y, si corresponde, el número
     * que identifica la aprobación o la recepción ({@code JBS-RESP-07}). Un rechazo de SIFEN
     * <b>no</b> es una falla: vuelve en la respuesta.
     *
     * @param respuesta respuesta interpretada.
     * @throws SifenException {@code JBS-RESP-04} o {@code JBS-RESP-07}.
     */
    void verificarRespuesta(BaseResponse respuesta) throws SifenException {
    }

    /**
     * Exige un código de resultado y avisa en el log si no tiene el formato del MT (4 dígitos,
     * {@code JBS-RESP-05}).
     *
     * @param codigo  código recibido ({@code dCodRes}, {@code dCodResLot}).
     * @param nombre  nombre del campo, para el detalle.
     * @param estado  estado HTTP.
     * @throws SifenException {@code JBS-RESP-04} si falta.
     */
    static void exigirCodigo(String codigo, String nombre, int estado) throws SifenException {
        if (codigo == null || codigo.isBlank()) {
            throw SifenException.ofResponse(ErrorCode.JBS_RESP_04, "HTTP " + estado + " sin " + nombre, null);
        }
        if (!codigo.matches("\\d{4}")) {
            LOGGER.warn("{} {} = {} no tiene el formato del MT (4 dígitos)", ErrorCode.JBS_RESP_05.getCode(), nombre, codigo);
        }
    }

    long getdId() {
        return dId;
    }

    SifenConfig getSifenConfig() {
        return sifenConfig;
    }
}
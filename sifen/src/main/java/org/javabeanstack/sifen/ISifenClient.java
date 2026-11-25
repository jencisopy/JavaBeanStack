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

import java.util.List;
import org.javabeanstack.sifen.api.BatchQueryResult;
import org.javabeanstack.sifen.api.BatchSendResult;
import org.javabeanstack.sifen.api.CertificateInfo;
import org.javabeanstack.sifen.api.ConnectionDiagnosis;
import org.javabeanstack.sifen.api.DEQueryResult;
import org.javabeanstack.sifen.api.EventResult;
import org.javabeanstack.sifen.api.NumberRange;
import org.javabeanstack.sifen.api.RucQueryResult;
import org.javabeanstack.sifen.api.SendResult;
import org.javabeanstack.sifen.api.SignatureVerification;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.EventosDE;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.validation.ValidationResult;

/**
 * Cliente de SIFEN de <b>una</b> empresa: la fachada pública del contrato de API v1 (plan JBSSIF,
 * Fase 10, D34; contrato v1 del plan JBSSIF, §10.1).
 *
 * <p>Se crea con {@link SifenClient#create(SifenConfig)} o se obtiene de un {@link SifenClientRegistry}
 * por clave de empresa. Reglas que valen para todos los métodos:</p>
 * <ul>
 *   <li><b>Seguro entre hilos</b>: un cliente se comparte entre todos los usuarios de la empresa. Un
 *       {@code DocumentoElectronico} <b>no</b> se comparte entre hilos.</li>
 *   <li><b>Bloquea</b>: cada llamada a SIFEN espera hasta el tiempo de lectura de la configuración
 *       (60 s por omisión, {@code sifen.http.read_timeout}; I5-11); no se hace dentro de una transacción de base abierta.</li>
 *   <li><b>Nunca devuelve {@code null}</b> ni una respuesta vacía, y nunca lanza otra cosa que
 *       {@link SifenException}.</li>
 *   <li>Orden interno de cada operación: configuración y certificado → argumentos → contenido →
 *       coherencia certificado-emisor → envío → respuesta.</li>
 *   <li><b>Un rechazo de SIFEN no es una excepción</b>: vuelve en el {@code *Result} con
 *       {@code REJECTED} y sus mensajes.</li>
 *   <li>Cada operación lleva un <b>id de operación</b> de ocho hexadecimales: está en el
 *       {@code *Result} ({@code getOperationId()}), al principio de las líneas del log del cliente y,
 *       si la operación falla, en {@code SifenException.getCorrelationId()}.</li>
 * </ul>
 *
 * <p>Estado de la implementación (plan JBSSIF): todas las operaciones del contrato v1 están
 * implementadas desde §10.3/§10.4 (emisión, lote, eventos, recuperación) y §5.3 (diagnóstico de la
 * conexión). {@code JBS-INT-04} («la operación no está disponible en esta versión») queda para una
 * operación que una versión futura agregue antes de implementarla.</p>
 */
public interface ISifenClient extends AutoCloseable {

    // ------------------------------------------------------------------ identidad y diagnóstico (no lanzan)

    /**
     * Devuelve la versión de {@code jbs-sifen} que firma y envía (para grabarla junto al documento).
     *
     * @return la versión, p. ej. {@code 2.2.0}.
     */
    String getVersion();

    /**
     * Devuelve una <b>copia</b> de la configuración del cliente: cambiarla no afecta al cliente (para
     * cambiar la configuración se invalida la clave en el registro).
     *
     * @return una copia nueva en cada llamada.
     */
    SifenConfig getConfig();

    /**
     * Valida la configuración y el certificado del cliente (reglas {@code JBS-CFG-*} y
     * {@code JBS-CERT-*}), releyendo el certificado. Para la pantalla de diagnóstico; no contacta a
     * SIFEN.
     *
     * @return el resultado, con errores y avisos; nunca {@code null} ni lanza.
     */
    ValidationResult validateConfig();

    /**
     * Devuelve los datos del certificado de la configuración: titular, RUC, vigencia y días restantes,
     * o por qué no se pudo cargar.
     *
     * @return los datos; nunca {@code null} ni lanza.
     */
    CertificateInfo getCertificateInfo();

    /**
     * Prueba la conexión con SIFEN por etapas (proxy, DNS, TCP, TLS, HTTP) sin enviar ningún documento.
     * El detalle de las etapas es PROVISIONAL.
     *
     * @return el diagnóstico; nunca {@code null} ni lanza.
     */
    ConnectionDiagnosis checkConnection();

    // ------------------------------------------------------------------ validación previa (no lanzan)

    /**
     * Valida el documento con las reglas del catálogo, sin firmarlo ni contactar a SIFEN: para mostrar
     * los problemas <b>antes</b> de numerarlo en firme. No evalúa {@code JBS-CERT-11} (RUC del
     * certificado distinto del emisor): esa la informa {@link #sign(DocumentoElectronico)}.
     *
     * @param de documento.
     * @return el resultado, con errores y avisos (índices base 0 en las rutas); nunca {@code null} ni
     *         lanza.
     */
    ValidationResult validateDE(DocumentoElectronico de);

    /**
     * Valida los eventos con las reglas del catálogo, sin contactar a SIFEN. PROVISIONAL.
     *
     * @param events eventos.
     * @return el resultado; nunca {@code null} ni lanza.
     */
    @Provisional
    ValidationResult validateEvents(EventosDE events);

    // ------------------------------------------------------------------ emisión en dos pasos

    /**
     * Valida, calcula el CDC, arma el {@code rDE}, firma una sola vez y arma el QR. Respeta
     * {@code dCodSeg} y {@code dFecFirma} si la aplicación los cargó. La aplicación graba el resultado
     * <b>antes</b> de {@link #send(SignedDocument)}.
     *
     * @param de documento.
     * @return el documento firmado.
     * @throws SifenException {@code SifenValidationException} con la lista si la configuración, el
     *                        contenido o la coherencia certificado-emisor fallan; {@code JBS-FIRMA-*} si
     *                        falla la firma o el QR.
     */
    SignedDocument sign(DocumentoElectronico de) throws SifenException;

    /**
     * Reconstruye el documento firmado desde el XML que la aplicación guardó. <b>No</b> recalcula,
     * <b>no</b> redondea y <b>no</b> firma: comprueba que el XML esté bien formado, que tenga
     * {@code DE}, firma y QR, y que el CDC sea coherente con sus datos.
     *
     * @param signedXml el {@code rDE} firmado.
     * @return el documento firmado ({@code getXml()} igual byte a byte).
     * @throws SifenException {@code JBS-XML-*} si el XML no sirve ({@code JBS-XML-06} sin firma, QR
     *                        o fecha de firma; {@code JBS-XML-07} si el CDC o el QR no corresponden).
     */
    SignedDocument readSigned(String signedXml) throws SifenException;

    /**
     * Envía el {@code rDE} firmado, sin tocarlo (envío sincrónico). Un solo envío en curso por CDC
     * ({@code JBS-ENT-08}). Si SIFEN contesta «CDC duplicado» (1001), consulta el documento: con 0422 devuelve
     * su estado real ({@code isAlreadyRegistered()}); si no, {@code JBS-RESP-08} con resultado desconocido,
     * nunca un rechazo (D43). La librería no reintenta sola una recepción.
     *
     * @param document el documento firmado y ya grabado por la aplicación.
     * @return el resultado (un rechazo vuelve acá, no como excepción).
     * @throws SifenException con {@code isOutcomeUnknown()} si el documento pudo haber llegado a SIFEN (lo
     *                        decide el transporte; también todo lo que falla después de transmitir y
     *                        {@code JBS-RESP-08}): resolver con {@link #resolve(SignedDocument)}, nunca
     *                        reenviando. Rechazos locales, sin resultado desconocido: {@code JBS-ENT-08} si ya
     *                        hay un envío en curso de ese CDC, {@code JBS-ENT-12} si el documento es de otro
     *                        ambiente que el cliente, {@code JBS-ENT-13} si el registro de envíos no lo
     *                        autoriza; {@code JBS-XML-*} si el XML no sirve.
     */
    SendResult send(SignedDocument document) throws SifenException;

    /**
     * Resuelve un envío de resultado desconocido: consulta por CDC; con 0422 devuelve el estado de SIFEN
     * ({@code isAlreadyRegistered()}), con 0420 lo envía <b>una</b> vez, y con cualquier otro código termina
     * con {@code JBS-RESP-08} sin reenviar (D43).
     *
     * @param document el documento firmado.
     * @return el resultado ({@code isAlreadyRegistered()} si salió de una consulta 0422).
     * @throws SifenException los códigos de {@link #send(SignedDocument)} y {@code JBS-RESP-08}; <b>toda</b>
     *                        excepción de {@code resolve} sale con {@code isOutcomeUnknown()} verdadero: el
     *                        documento ya estaba en duda.
     */
    SendResult resolve(SignedDocument document) throws SifenException;

    // ------------------------------------------------------------------ lote (asíncrono)

    /**
     * Envía un lote de hasta 50 documentos del mismo tipo y del mismo RUC ({@code JBS-ENT-01},
     * {@code JBS-ENT-02}, {@code JBS-ENT-09}). No reenviar un CDC que está en un lote en proceso: SIFEN
     * bloquea el RUC de 10 a 60 minutos.
     *
     * @param documents documentos firmados.
     * @return el resultado, con el número de lote.
     * @throws SifenException {@code JBS-ENT-11} vacío o con un nulo; {@code JBS-ENT-12} si un documento es de
     *                        otro ambiente; {@code JBS-ENT-08}/{@code JBS-ENT-13} como {@link #send(SignedDocument)};
     *                        por los argumentos o el envío.
     */
    BatchSendResult sendBatch(List<SignedDocument> documents) throws SifenException;

    /**
     * Consulta un lote: primera consulta a los 10 minutos del envío y después cada 10 minutos o más
     * ({@code getSuggestedWait()}). Pasadas 48 h se resuelve cada CDC con {@link #resolve(SignedDocument)}.
     *
     * @param batchNumber número de lote ({@code dProtConsLote}).
     * @return el resultado, con un {@code SendResult} por documento cuando el lote concluyó.
     * @throws SifenException {@code JBS-ENT-05} si el número no es válido; de configuración, conexión o
     *                        respuesta.
     */
    BatchQueryResult queryBatch(String batchNumber) throws SifenException;

    // ------------------------------------------------------------------ consultas

    /**
     * Consulta un documento por CDC. 0420 significa «no existe <b>o no está aprobado</b>».
     *
     * @param cdc CDC de 44 dígitos.
     * @return el resultado, con el {@code rDE} que tiene SIFEN y sus eventos.
     * @throws SifenException {@code JBS-ENT-03} si el CDC no es válido; de configuración, conexión o
     *                        respuesta.
     */
    DEQueryResult queryDE(String cdc) throws SifenException;

    /**
     * Consulta un RUC.
     *
     * @param ruc RUC <b>sin</b> dígito verificador.
     * @return el resultado ({@code isFound()} falso si no existe).
     * @throws SifenException {@code JBS-ENT-04} si el RUC no es válido; de configuración, conexión o
     *                        respuesta.
     */
    RucQueryResult queryRuc(String ruc) throws SifenException;

    // ------------------------------------------------------------------ eventos del emisor

    /**
     * Cancela un documento aprobado. Motivo de 5 a 500 caracteres; plazo de SIFEN: 48 h desde la
     * aprobación para la factura electrónica y 168 h para los demás documentos.
     *
     * @param cdc    CDC del documento.
     * @param reason motivo.
     * @return el resultado del evento.
     * @throws SifenException {@code JBS-ENT-03} si el CDC no es válido; {@code SifenValidationException} si
     *                        el motivo no cumple las reglas del evento; por el envío.
     */
    EventResult cancel(String cdc, String reason) throws SifenException;

    /**
     * Inutiliza un rango de numeración que SIFEN <b>no</b> aprobó (hasta 1000 números).
     *
     * @param range rango a inutilizar.
     * @return el resultado del evento.
     * @throws SifenException {@code JBS-ENT-10} si el rango no se puede armar; {@code SifenValidationException}
     *                        si no cumple las reglas del evento; por el envío.
     */
    EventResult invalidateNumbers(NumberRange range) throws SifenException;

    /**
     * Envía los demás eventos (conformidad, disconformidad, desconocimiento, notificación,
     * nominación, transporte) con el modelo actual, <b>un evento por llamada</b>. PROVISIONAL.
     *
     * @param events eventos, con exactamente un {@code rGesEve}.
     * @return el resultado.
     * @throws SifenException {@code JBS-ENT-14} si no trae exactamente un evento (sin contactar a SIFEN);
     *                        por el contenido o el envío.
     */
    @Provisional
    EventResult sendEvents(EventosDE events) throws SifenException;

    // ------------------------------------------------------------------ firma de un XML recibido (no lanza)

    /**
     * Verifica la firma de un XML recibido de un tercero: integridad y que el RUC del certificado sea
     * el del emisor (no valida la cadena del certificado, E3-08).
     *
     * @param xml el XML firmado.
     * @return el veredicto, o la falla con su código; nunca {@code null} ni lanza.
     */
    SignatureVerification verifySignature(String xml);

    /**
     * Libera los recursos propios del cliente. Idempotente y no lanza. Las operaciones en curso
     * terminan normalmente.
     */
    @Override
    void close();
}

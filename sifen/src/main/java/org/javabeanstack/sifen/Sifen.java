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

import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.EventosDE;
import org.javabeanstack.sifen.core.beans.ValidezFirmaDigital;
import org.javabeanstack.sifen.core.beans.response.*;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.Operation;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.validation.ConfigValidator;
import org.javabeanstack.sifen.core.validation.DEValidator;
import org.javabeanstack.sifen.core.validation.EventValidator;
import org.javabeanstack.sifen.core.validation.ValidationResult;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.internal.facade.Inputs;
import org.javabeanstack.sifen.internal.facade.Operations;
import org.javabeanstack.sifen.internal.helpers.SignatureHelper;
import org.javabeanstack.sifen.internal.request.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Clase principal de la librería desde la cuál se realizan todas las operaciones de Sifen.
 *
 * <p>Delta del fork 2.0.0-SNAPSHOT (plan JBSSIF, §1.4.5; ficha 15 del informe de línea base):
 * {@code setSifenConfig} ya no registra el mensaje «Configuración de Sifen guardada
 * correctamente».</p>
 *
 * <p>Modelo de errores (plan JBSSIF, §3.8, D33 Parte C): todo método público que declara
 * {@code throws SifenException} corre dentro de un envoltorio único ({@code Operations.execute}),
 * así que <b>ninguna otra excepción sale de la fachada</b>: lo diagnosticado sale con su código
 * ({@code JBS-<CAT>-<nn>} o el de la regla), su categoría, la operación, la referencia (CDC, lote o
 * RUC) y un id de correlación que aparece en {@code getMessage()} y al inicio de la línea del log; lo
 * imprevisto sale como {@code JBS-INT-00} con su id y la traza en el log, sin mensajes. Orden de cada
 * operación: configuración y certificado → argumentos → contenido → envío → respuesta. Los rechazos de
 * SIFEN ({@code dCodRes}) no son excepciones: siguen en las {@code Respuesta*}. Los
 * {@code validarFirmaDE*} no lanzan: una falla vuelve en {@link ValidezFirmaDigital} con su código.</p>
 *
 * <p>Deprecada en favor de {@link ISifenClient} (D42): se mantiene sin cambios de firma en 2.x y se retira
 * en 3.0; no recibe operaciones nuevas.</p>
 */
public class Sifen {
    private static final Logger LOGGER = LogManager.getLogger(Sifen.class);
    private static SifenConfig sifenConfig = null;
    private static long dId = 1;

    /**
     * Establece la configuración necesaria para el funcionamiento correcto de todas las funcionalidades. Solo
     * debe realizarse una vez al principio, antes de ejecutar alguna acción. Si la configuración necesita ser
     * actualizada, simplemente invocar de vuelta.
     *
     * @param newSifenConfig El objeto de configuración que será utilizado.
     * @throws SifenException Si la configuración es nula o, si existe algún error en los valores de la configuración.
     */
    public static void setSifenConfig(SifenConfig newSifenConfig) throws SifenException {
        Operations.execute(Operation.LOAD_CONFIG, null, () -> {
            // §3.7 (D33 Parte B, momento 1): toda la lista de errores de configuración y certificado (i101).
            ConfigValidator.standard().require(newSifenConfig);
            if (Operations.inProgress() > 1) {
                // §3.8 (JBS-EST-01, M0-04): la configuración global cambia con operaciones en curso.
                LOGGER.warn("{} {}", ErrorCode.JBS_EST_01.getCode(), ErrorCode.JBS_EST_01.message());
            }
            sifenConfig = newSifenConfig;
            return null;
        });
    }

    /**
     * Contexto por defecto de una configuración; con la configuración nula lanza {@code JBS-CFG-01}
     * en vez de la {@code NullPointerException} de {@code GenerationCtx.getDefaultFromConfig}.
     */
    private static GenerationCtx contexto(SifenConfig cfg) throws SifenException {
        if (cfg == null) {
            ConfigValidator.standard().require(null);
        }
        return GenerationCtx.getDefaultFromConfig(cfg);
    }

    /**
     * @return El objeto de configuración previamente establecido.
     */
    public static SifenConfig getSifenConfig() {
        return sifenConfig;
    }

    /**
     * Realiza una consulta a Sifen y devuelve como resultado los datos y el estado del RUC de un contribuyente.
     *
     * @param ruc RUC de un contribuyente a ser consultado en Sifen, sin el DV.
     * @return La respuesta a la consulta proveída por Sifen, en forma de clase.
     * @throws SifenException Si la configuración de Sifen no fue establecida o, si algún dato necesario para la
     *                        consulta no pudo ser encontrado o, si la consulta no pudo ser realizada.
     */
    public static RespuestaConsultaRUC consultaRUC(String ruc) throws SifenException {
        return Operations.execute(Operation.QUERY_RUC, () -> ruc, () -> consultaRUC(ruc, sifenConfig));
    }

    /**
     *
     * @param ruc RUc de un contribuyente a ser consultado en Sifen, sin el DV.
     * @param sifenConfig Configuración de Sifen a ser utilizada en esta petición.
     * @return
     * @throws SifenException Si la configuración de Sifen no fue establecida o, si algún dato necesario para la
     * consulta no pudo ser encontrado o, si la consulta no pudo ser realizada.
     */
    public static RespuestaConsultaRUC consultaRUC(String ruc, SifenConfig sifenConfig) throws SifenException {
        return Operations.execute(Operation.QUERY_RUC, () -> ruc,
                () -> consultaRUC(ruc, sifenConfig, contexto(sifenConfig)));
    }

    /**
     * Realiza una consulta a Sifen y devuelve como resultado los datos y el estado del RUC de un contribuyente.
     *
     * @param ruc         RUC de un contribuyente a ser consultado en Sifen, sin el DV.
     * @param sifenConfig Configuración de Sifen a ser utilizada en esta petición.
     * @param generationCtx Contexto de generación de la petición.
     * @return La respuesta a la consulta proveída por Sifen, en forma de clase.
     * @throws SifenException Si la configuración de Sifen no fue establecida o, si algún dato necesario para la
     *                        consulta no pudo ser encontrado o, si la consulta no pudo ser realizada.
     */
    public static RespuestaConsultaRUC consultaRUC(String ruc, SifenConfig sifenConfig, GenerationCtx generationCtx) throws SifenException {
        return Operations.execute(Operation.QUERY_RUC, () -> ruc, () -> {
            // §3.7 (D33 Parte B): configuración y certificado antes de operar; nula = JBS-CFG-01.
            ConfigValidator.standard().require(sifenConfig);
            Inputs.requireRuc(ruc); // §3.8: JBS-ENT-04
            LOGGER.info("Preparando petición 'Consulta de RUC'");
            ReqConsRuc reqConsRuc = new ReqConsRuc(dId++, sifenConfig);
            reqConsRuc.setdRUCCons(ruc);

            return (RespuestaConsultaRUC) reqConsRuc.makeRequest(generationCtx, sifenConfig.getQueryRucPath());
        });
    }

    /**
     * Realiza una consulta a Sifen y devuelve como resultado el Documento Electrónico encontrado y todos sus eventos asociados.
     *
     * @param cdc Código de Control, que es el identificador único de un Documento Electrónico.
     * @return La respuesta a la consulta proveída por Sifen, en forma de clase.
     * @throws SifenException Si la configuración de Sifen no fue establecida o, si algún dato necesario para la
     *                        consulta no pudo ser encontrado o, si la consulta no pudo ser realizada.
     */
    public static RespuestaConsultaDE consultaDE(String cdc) throws SifenException {
        return Operations.execute(Operation.QUERY_DE, () -> cdc, () -> consultaDE(cdc, sifenConfig));
    }

    /**
     * Realiza una consulta a Sifen y devuelve como resultado el Documento Electrónico encontrado y todos sus eventos asociados.
     *
     * @param cdc         Código de Control, que es el identificador único de un Documento Electrónico.
     * @param sifenConfig Configuración de Sifen a ser utilizada en esta petición.
     * @return La respuesta a la consulta proveída por Sifen, en forma de clase.
     * @throws SifenException Si la configuración de Sifen no fue establecida o, si algún dato necesario para la
     *                        consulta no pudo ser encontrado o, si la consulta no pudo ser realizada.
     */
    public static RespuestaConsultaDE consultaDE(String cdc, SifenConfig sifenConfig) throws SifenException {
        return Operations.execute(Operation.QUERY_DE, () -> cdc,
                () -> consultaDE(cdc, sifenConfig, contexto(sifenConfig)));
    }

    /**
     * Realiza una consulta a Sifen y devuelve como resultado el Documento Electrónico encontrado y todos sus eventos asociados.
     *
     * @param cdc         Código de Control, que es el identificador único de un Documento Electrónico.
     * @param sifenConfig Configuración de Sifen a ser utilizada en esta petición.
     * @param generationCtx Contexto de generación de la petición.
     * @return La respuesta a la consulta proveída por Sifen, en forma de clase.
     * @throws SifenException Si la configuración de Sifen no fue establecida o, si algún dato necesario para la
     *                        consulta no pudo ser encontrado o, si la consulta no pudo ser realizada.
     */
    public static RespuestaConsultaDE consultaDE(String cdc, SifenConfig sifenConfig, GenerationCtx generationCtx) throws SifenException {
        return Operations.execute(Operation.QUERY_DE, () -> cdc, () -> {
            // §3.7 (D33 Parte B): configuración y certificado antes de operar; nula = JBS-CFG-01.
            ConfigValidator.standard().require(sifenConfig);
            Inputs.requireCdc(cdc); // §3.8: JBS-ENT-03
            LOGGER.info("Preparando petición 'Consulta de DE'");
            ReqConsDe reqConsDe = new ReqConsDe(dId++, sifenConfig);
            reqConsDe.setdCDC(cdc);

            return (RespuestaConsultaDE) reqConsDe.makeRequest(generationCtx, sifenConfig.getQueryDEPath());
        });
    }

    /**
     * Realiza una consulta a Sifen y devuelve como resultado el estado del lote consultado.
     *
     * @param nroLote Número de Lote recibido como respuesta en el envío del mismo.
     * @return La respuesta a la consulta proveída por Sifen, en forma de clase.
     * @throws SifenException Si la configuración de Sifen no fue establecida o, si algún dato necesario para la
     *                        consulta no pudo ser encontrado o, si la consulta no pudo ser realizada.
     */
    public static RespuestaConsultaLoteDE consultaLoteDE(String nroLote) throws SifenException {
        return Operations.execute(Operation.QUERY_BATCH, () -> nroLote, () -> consultaLoteDE(nroLote, sifenConfig));
    }

    /**
     * Realiza una consulta a Sifen y devuelve como resultado el estado del lote consultado.
     *
     * @param nroLote     Número de Lote recibido como respuesta en el envío del mismo.
     * @param sifenConfig Configuración de Sifen a ser utilizada en esta petición.
     * @return La respuesta a la consulta proveída por Sifen, en forma de clase.
     * @throws SifenException Si la configuración de Sifen no fue establecida o, si algún dato necesario para la
     *                        consulta no pudo ser encontrado o, si la consulta no pudo ser realizada.
     */
    public static RespuestaConsultaLoteDE consultaLoteDE(String nroLote, SifenConfig sifenConfig) throws SifenException {
        return Operations.execute(Operation.QUERY_BATCH, () -> nroLote,
                () -> consultaLoteDE(nroLote, sifenConfig, contexto(sifenConfig)));
    }

    /**
     * Realiza una consulta a Sifen y devuelve como resultado el estado del lote consultado.
     *
     * @param nroLote     Número de Lote recibido como respuesta en el envío del mismo.
     * @param sifenConfig Configuración de Sifen a ser utilizada en esta petición.
     * @param generationCtx Contexto de generación de la petición.
     * @return La respuesta a la consulta proveída por Sifen, en forma de clase.
     * @throws SifenException Si la configuración de Sifen no fue establecida o, si algún dato necesario para la
     *                        consulta no pudo ser encontrado o, si la consulta no pudo ser realizada.
     */
    public static RespuestaConsultaLoteDE consultaLoteDE(String nroLote, SifenConfig sifenConfig, GenerationCtx generationCtx) throws SifenException {
        return Operations.execute(Operation.QUERY_BATCH, () -> nroLote, () -> {
            // §3.7 (D33 Parte B): configuración y certificado antes de operar; nula = JBS-CFG-01.
            ConfigValidator.standard().require(sifenConfig);
            Inputs.requireBatchNumber(nroLote); // §3.8: JBS-ENT-05
            LOGGER.info("Preparando petición 'Consulta de Resultado de Lote de DE'");
            ReqConsLoteDe reqConsLoteDe = new ReqConsLoteDe(dId++, sifenConfig);
            reqConsLoteDe.setdProtConsLote(nroLote);

            return (RespuestaConsultaLoteDE) reqConsLoteDe.makeRequest(generationCtx, sifenConfig.getQueryBatchPath());
        });
    }

    /**
     * Realiza un envío del Documento Electrónico a Sifen para su correspondiente aprobación.
     *
     * @param de Objeto que hace referencia a un Documento Electrónico, con todos sus datos.
     * @return La respuesta a la consulta proveída por Sifen, en forma de clase.
     * @throws SifenException Si la configuración de Sifen no fue establecida o, si algún dato necesario para la
     *                        consulta no pudo ser encontrado o, si la firma digital del DE falla o, si la consulta no pudo ser realizada.
     */
    public static RespuestaRecepcionDE recepcionDE(DocumentoElectronico de) throws SifenException {
        return Operations.execute(Operation.SEND_DE, () -> cdcDe(de), () -> recepcionDE(de, sifenConfig));
    }

    /**
     * Realiza un envío del Documento Electrónico a Sifen con la configuración dada.
     *
     * @param de          Objeto que hace referencia a un Documento Electrónico, con todos sus datos.
     * @param sifenConfig Configuración de Sifen a ser utilizada en esta petición.
     * @return La respuesta a la consulta proveída por Sifen, en forma de clase.
     * @throws SifenException con código, categoría e id de correlación (§3.8).
     */
    public static RespuestaRecepcionDE recepcionDE(DocumentoElectronico de, SifenConfig sifenConfig) throws SifenException {
        return Operations.execute(Operation.SEND_DE, () -> cdcDe(de),
                () -> recepcionDE(de, sifenConfig, contexto(sifenConfig)));
    }

    /**
     * Realiza un envío del Documento Electrónico a Sifen para su correspondiente aprobación.
     *
     * @param de          Objeto que hace referencia a un Documento Electrónico, con todos sus datos.
     * @param sifenConfig Configuración de Sifen a ser utilizada en esta petición.
     * @return La respuesta a la consulta proveída por Sifen, en forma de clase.
     * @throws SifenException Si la configuración de Sifen no fue establecida o, si algún dato necesario para la
     *                        consulta no pudo ser encontrado o, si la firma digital del DE falla o, si la consulta no pudo ser realizada.
     */
    public static RespuestaRecepcionDE recepcionDE(DocumentoElectronico de, SifenConfig sifenConfig, GenerationCtx generationCtx) throws SifenException {
        return Operations.execute(Operation.SEND_DE, () -> cdcDe(de), () -> {
            // §3.7 (D33 Parte B): configuración y certificado antes de operar; nula = JBS-CFG-01.
            ConfigValidator.standard().require(sifenConfig);
            if (sifenConfig.isPreValidationEnabled() || de == null) {
                // §3.8: el DE nulo es JBS-VAL-01 también sin validación previa (la línea base daba NPE)
                DEValidator.standard().validate(de, generationCtx).throwIfInvalid();
            }
            if (sifenConfig.isPreValidationEnabled()) {
                ConfigValidator.standard().requireConsistency(de, sifenConfig);
            }
            LOGGER.info("Preparando petición 'Recepción de DE'");
            ReqRecDe reqRecDe = new ReqRecDe(dId++, sifenConfig);
            reqRecDe.setDE(de);

            return (RespuestaRecepcionDE) reqRecDe.makeRequest(generationCtx, sifenConfig.getSendPath());
        });
    }

    /**
     * Realiza un envío de un lote de Documentos Electrónicos a Sifen para su correspondiente aprobación. La respuesta
     * de la aprobación o rechazo de cada DE es asíncrono, es decir, no se encuentra en la respuesta de esta petición.
     *
     * @param deList Listado de los objetos que hacen referencia a los Documentos Electrónicos, con todos los datos.
     * @return La respuesta a la consulta proveída por Sifen, en forma de clase.
     * @throws SifenException Si la configuración de Sifen no fue establecida o, si algún dato necesario de algún DE
     *                        no pudo ser encontrado o, si la forma digital de algún DE falla o, si la consulta no pudo ser realizada.
     */
    public static RespuestaRecepcionLoteDE recepcionLoteDE(List<DocumentoElectronico> deList) throws SifenException {
        return Operations.execute(Operation.SEND_BATCH, null, () -> recepcionLoteDE(deList, sifenConfig));
    }

    /**
     * Realiza un envío de un lote de Documentos Electrónicos a Sifen para su correspondiente aprobación. La respuesta
     * de la aprobación o rechazo de cada DE es asíncrono, es decir, no se encuentra en la respuesta de esta petición.
     *
     * @param deList      Listado de los objetos que hacen referencia a los Documentos Electrónicos, con todos los datos.
     * @param sifenConfig Configuración de Sifen a ser utilizada en esta petición.
     * @return La respuesta a la consulta proveída por Sifen, en forma de clase.
     * @throws SifenException Si la configuración de Sifen no fue establecida o, si algún dato necesario de algún DE
     *                        no pudo ser encontrado o, si la forma digital de algún DE falla o, si la consulta no pudo ser realizada.
     */
    public static RespuestaRecepcionLoteDE recepcionLoteDE(List<DocumentoElectronico> deList, SifenConfig sifenConfig) throws SifenException {
        return Operations.execute(Operation.SEND_BATCH, null,
                () -> recepcionLoteDE(deList, sifenConfig, contexto(sifenConfig)));
    }

    /**
     * Realiza un envío de un lote de Documentos Electrónicos a Sifen para su correspondiente aprobación. La respuesta
     * de la aprobación o rechazo de cada DE es asíncrono, es decir, no se encuentra en la respuesta de esta petición.
     *
     * @param deList      Listado de los objetos que hacen referencia a los Documentos Electrónicos, con todos los datos.
     * @param sifenConfig Configuración de Sifen a ser utilizada en esta petición.
     * @param generationCtx Contexto de generación de los documentos electrónicos.
     * @return La respuesta a la consulta proveída por Sifen, en forma de clase.
     * @throws SifenException Si la configuración de Sifen no fue establecida o, si algún dato necesario de algún DE
     *                        no pudo ser encontrado o, si la forma digital de algún DE falla o, si la consulta no pudo ser realizada.
     */
    public static RespuestaRecepcionLoteDE recepcionLoteDE(List<DocumentoElectronico> deList, SifenConfig sifenConfig, GenerationCtx generationCtx) throws SifenException {
        return Operations.execute(Operation.SEND_BATCH, null, () -> {
            // §3.7 (D33 Parte B): configuración y certificado antes de operar; nula = JBS-CFG-01.
            ConfigValidator.standard().require(sifenConfig);
            Inputs.requireBatchSize(deList); // §3.8: JBS-ENT-01 (más de 50)
            if (sifenConfig.isPreValidationEnabled() || deList == null || deList.isEmpty() || deList.contains(null)) {
                // §3.8: lista nula, vacía o con nulos es JBS-VAL-01 también sin validación previa
                validarLote(deList, generationCtx).throwIfInvalid();
            }
            if (sifenConfig.isPreValidationEnabled()) {
                coherenciaLote(deList, sifenConfig).throwIfInvalid();
            }
            // §3.8: JBS-ENT-02 (CDC repetido) después del contenido: el CDC sale de datos ya validados
            Inputs.requireUniqueCdcs(deList);
            LOGGER.info("Preparando petición 'Recepción de Lote de DE'");
            ReqRecLoteDe reqRecLoteDe = new ReqRecLoteDe(dId++, sifenConfig);
            reqRecLoteDe.setDEList(deList);

            return (RespuestaRecepcionLoteDE) reqRecLoteDe.makeRequest(generationCtx, sifenConfig.getSendBatchPath());
        });
    }

    /**
     * Realiza un envío a Sifen de los eventos agregados en el objeto recibido como argumento.
     *
     * @param eventosDE Objeto que contiene el listado de eventos a ser enviados a Sifen.
     * @return La respuesta a la consulta proveída por Sifen, en forma de clase.
     * @throws SifenException Si la configuración de Sifen no fue establecida o, si algún dato necesario para la
     *                        consulta no pudo ser encontrado o, si la firma digital de algún evento falla o, si la consulta no pudo ser
     *                        realizada.
     */
    public static RespuestaRecepcionEvento recepcionEvento(EventosDE eventosDE) throws SifenException {
        return Operations.execute(Operation.SEND_EVENTS, null, () -> recepcionEvento(eventosDE, sifenConfig));
    }

    /**
     * Realiza un envío a Sifen de los eventos con la configuración dada.
     *
     * @param eventosDE   Objeto que contiene el listado de eventos a ser enviados a Sifen.
     * @param sifenConfig Configuración de Sifen a ser utilizada en esta petición.
     * @return La respuesta a la consulta proveída por Sifen, en forma de clase.
     * @throws SifenException con código, categoría e id de correlación (§3.8).
     */
    public static RespuestaRecepcionEvento recepcionEvento(EventosDE eventosDE, SifenConfig sifenConfig) throws SifenException {
        return Operations.execute(Operation.SEND_EVENTS, null,
                () -> recepcionEvento(eventosDE, sifenConfig, contexto(sifenConfig)));
    }

    /**
     * Realiza un envío a Sifen de los eventos agregados en el objeto recibido como argumento.
     *
     * @param eventosDE   Objeto que contiene el listado de eventos a ser enviados a Sifen.
     * @param sifenConfig Configuración de Sifen a ser utilizada en esta petición.
     * @return La respuesta a la consulta proveída por Sifen, en forma de clase.
     * @throws SifenException Si la configuración de Sifen no fue establecida o, si algún dato necesario para la
     *                        consulta no pudo ser encontrado o, si la firma digital de algún evento falla o, si la consulta no pudo ser
     *                        realizada.
     */
    public static RespuestaRecepcionEvento recepcionEvento(EventosDE eventosDE, SifenConfig sifenConfig, GenerationCtx generationCtx) throws SifenException {
        return Operations.execute(Operation.SEND_EVENTS, null, () -> {
            // §3.7 (D33 Parte B): configuración y certificado antes de operar; nula = JBS-CFG-01.
            ConfigValidator.standard().require(sifenConfig);
            if (sifenConfig.isPreValidationEnabled() || eventosDE == null || eventosDE.getrGesEveList() == null
                    || eventosDE.getrGesEveList().isEmpty()) {
                // §3.8: sin eventos es JBS-VAL-01 / JBS-EVENTO-* también sin validación previa
                EventValidator.standard().validate(eventosDE, generationCtx).throwIfInvalid();
            }
            LOGGER.info("Preparando petición 'Recepción de Eventos'");
            ReqRecEventoDe reqRecEventoDe = new ReqRecEventoDe(dId++, sifenConfig);
            reqRecEventoDe.setEventoDE(eventosDE);

            return (RespuestaRecepcionEvento) reqRecEventoDe.makeRequest(generationCtx, sifenConfig.getEventPath());
        });
    }

    /**
     * Verifica si la firma digital del Documento Electrónico recibido como argumento es válida.
     *
     * @param xml Cadena de texto correspondiente al Documento Electrónico a validar, en formato XML.
     * @return Instancia del objeto <i>ValidezFirmaDigital</i>, especificando si la firma es válida, el
     * motivo en caso de que no lo sea, y los datos del sujeto encontrados en el certificado.
     */
    public static ValidezFirmaDigital validarFirmaDEDesdeXml(String xml) {
        return Operations.evaluate(Operation.VERIFY_SIGNATURE, null, () -> SignatureHelper.validateSignatureOrThrow(xml, "XML"),
                ValidezFirmaDigital::ofError);
    }

    /**
     * Verifica si la firma digital del Documento Electrónico recibido como argumento es válida.
     *
     * @param rutaArchivo Ruta del archivo correspondiente al Documento Electrónico a validar, en formato XML.
     * @return Instancia del objeto <i>ValidezFirmaDigital</i>, especificando si la firma es válida, el
     * motivo en caso de que no lo sea, y los datos del sujeto encontrados en el certificado.
     */
    public static ValidezFirmaDigital validarFirmaDE(String rutaArchivo) {
        return Operations.evaluate(Operation.VERIFY_SIGNATURE, null, () -> SignatureHelper.validateSignatureOrThrow(rutaArchivo, "PATH"),
                ValidezFirmaDigital::ofError);
    }

    /**
     * Verifica si la firma digital del Documento Electrónico recibido como argumento es válida.
     *
     * @param archivoXml Instancia de un archivo correspondiente al Documento Electrónico a validar, en formato XML.
     * @return Instancia del objeto <i>ValidezFirmaDigital</i>, especificando si la firma es válida, el
     * motivo en caso de que no lo sea, y los datos del sujeto encontrados en el certificado.
     */
    public static ValidezFirmaDigital validarFirmaDE(File archivoXml) {
        return Operations.evaluate(Operation.VERIFY_SIGNATURE, null, () -> SignatureHelper.validateSignatureOrThrow(archivoXml),
                ValidezFirmaDigital::ofError);
    }

    /**
     * Valida un Documento Electrónico con las reglas del catálogo (plan JBSSIF, §3.5) sin generarlo
     * ni enviarlo. No lanza: devuelve todos los errores y avisos.
     *
     * @param de Documento Electrónico a validar.
     * @return el resultado; {@link ValidationResult#isValid()} indica si se puede generar.
     */
    public static ValidationResult validarDE(DocumentoElectronico de) {
        return validarDE(de, sifenConfig == null ? null : GenerationCtx.getDefaultFromConfig(sifenConfig));
    }

    /**
     * Valida un Documento Electrónico con el contexto de generación dado (configuración, NT 013).
     *
     * @param de            Documento Electrónico a validar.
     * @param generationCtx contexto de generación; {@code null} da un aviso y valores por defecto.
     * @return el resultado; nunca {@code null}.
     */
    public static ValidationResult validarDE(DocumentoElectronico de, GenerationCtx generationCtx) {
        return DEValidator.standard().validate(de, generationCtx);
    }

    /**
     * Valida un envío de eventos con las reglas del catálogo, sin firmarlo ni enviarlo.
     *
     * @param eventosDE eventos a validar.
     * @return el resultado; las rutas llevan el índice del evento ({@code rGesEve[0].rEve…}).
     */
    public static ValidationResult validarEvento(EventosDE eventosDE) {
        return validarEvento(eventosDE, sifenConfig == null ? null : GenerationCtx.getDefaultFromConfig(sifenConfig));
    }

    /**
     * Valida un envío de eventos con el contexto de generación dado.
     *
     * @param eventosDE     eventos a validar.
     * @param generationCtx contexto de generación; {@code null} da un aviso y valores por defecto.
     * @return el resultado; nunca {@code null}.
     */
    public static ValidationResult validarEvento(EventosDE eventosDE, GenerationCtx generationCtx) {
        return EventValidator.standard().validate(eventosDE, generationCtx);
    }

    /** CDC ya calculado de un DE (para la referencia del error); {@code null} si no se conoce. */
    private static String cdcDe(DocumentoElectronico de) {
        return de == null ? null : de.getId();
    }

    /** Valida cada DE del lote y junta los resultados con el índice del DE ({@code lote[2].gDatGralOpe…}). */
    private static ValidationResult validarLote(List<DocumentoElectronico> deList, GenerationCtx generationCtx) {
        if (deList == null || deList.isEmpty()) {
            return DEValidator.standard().validate(null, generationCtx);
        }
        List<ValidationResult> resultados = new ArrayList<>();
        for (int i = 0; i < deList.size(); i++) {
            resultados.add(DEValidator.standard().validate(deList.get(i), generationCtx).withPrefix("lote[" + i + "]"));
        }
        return ValidationResult.combine(resultados);
    }

    /** JBS-CERT-11 de cada DE del lote, con el índice ({@code lote[2].gDatGralOpe.gEmis.dRucEm}). */
    private static ValidationResult coherenciaLote(List<DocumentoElectronico> deList, SifenConfig sifenConfig) {
        List<ValidationResult> resultados = new ArrayList<>();
        for (int i = 0; i < deList.size(); i++) {
            resultados.add(ConfigValidator.standard().validateConsistency(deList.get(i), sifenConfig)
                    .withPrefix("lote[" + i + "]"));
        }
        return ValidationResult.combine(resultados);
    }

    /**
     * Valida la configuración y el certificado sin lanzar (plan JBSSIF, §3.7, D33 Parte B, momento 3):
     * relee el certificado aunque el archivo no haya cambiado. Para la pantalla de diagnóstico del
     * consumidor: código, ruta, mensaje y severidad de cada problema; los datos del certificado, con
     * {@link ConfigValidator#certificate(SifenConfig)}.
     *
     * @param sifenConfig configuración; {@code null} da {@code JBS-CFG-01}.
     * @return el resultado; nunca {@code null}.
     */
    public static ValidationResult validarConfiguracion(SifenConfig sifenConfig) {
        return ConfigValidator.standard().validate(sifenConfig, true);
    }
}

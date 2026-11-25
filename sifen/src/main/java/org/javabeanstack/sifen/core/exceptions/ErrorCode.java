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
package org.javabeanstack.sifen.core.exceptions;

import java.util.Optional;

/**
 * Códigos estables de los errores de la librería que <b>no</b> son reglas del catálogo (plan JBSSIF,
 * §3.8, D33 Parte C, inventario de 78 puntos de falla).
 *
 * <p>Forma {@code JBS-<CAT>-<nn>}: {@code ENT} (argumentos de la operación), {@code XML} (XML que
 * se lee), {@code FIRMA} (firma y QR), {@code CONEX} (conexión con SIFEN), {@code RESP} (respuesta
 * de SIFEN no interpretable), {@code INT} (fallas internas) y {@code EST} (estado compartido). Los
 * de configuración y certificado ({@code JBS-CFG-*}, {@code JBS-CERT-*}) son reglas del catálogo de
 * validación, no constantes de este enum.</p>
 *
 * <p>El mensaje para pantalla y la acción sugerida viven en {@code error_messages.properties}
 * (claves {@code <código>} y {@code <código>.accion}); un test exige ambas para cada constante y
 * ninguna huérfana. Un caso nuevo que aparezca en un cliente es <b>una constante acá, sus dos claves
 * en el recurso y una fila en el traductor de excepciones</b>; la fachada no se toca. Un código
 * asignado no se reasigna nunca.</p>
 */
@SuppressWarnings("deprecation")
public enum ErrorCode {
    // ------------------------------------------------------------------ INPUT (argumentos)
    /** Lote con más documentos que el máximo del MT (50). */
    JBS_ENT_01("JBS-ENT-01", ErrorCategory.INPUT, null),
    /** Documento repetido (mismo CDC) dentro del lote. */
    JBS_ENT_02("JBS-ENT-02", ErrorCategory.INPUT, null),
    /** CDC nulo, de largo distinto de 44, no numérico o con dígito verificador incorrecto. */
    JBS_ENT_03("JBS-ENT-03", ErrorCategory.INPUT, null),
    /** RUC a consultar nulo o con formato inválido. */
    JBS_ENT_04("JBS-ENT-04", ErrorCategory.INPUT, null),
    /** Número de lote a consultar nulo o no numérico. */
    JBS_ENT_05("JBS-ENT-05", ErrorCategory.INPUT, null),
    /**
     * El archivo o el flujo de la configuración no se pudo leer: no existe, es un directorio, no hay
     * permiso o está vacío (M3-32, E3-29; §3.8 ciclo 2). Categoría CONFIGURATION: lo corrige el
     * administrador.
     */
    JBS_ENT_06("JBS-ENT-06", ErrorCategory.CONFIGURATION, SifenExceptionCodes.INVALID_CONFIGURATION_CODE),
    /**
     * Faltan datos del documento para calcular el CDC (I3-49; §3.8 ciclo 2): la línea base daba
     * {@code i402}; con la validación previa activa lo informan antes las reglas de los grupos C y D.
     */
    JBS_ENT_07("JBS-ENT-07", ErrorCategory.DE_CONTENT, SifenExceptionCodes.FIELD_NOT_FOUND_CODE),
    /**
     * Ya hay un envío en curso del mismo documento (mismo CDC) en el mismo cliente (plan JBSSIF,
     * §10.2, D41): se rechaza sin contactar a SIFEN. Lo emite {@code ISifenClient.send} desde §10.4.
     */
    JBS_ENT_08("JBS-ENT-08", ErrorCategory.INPUT, null),
    /**
     * El lote mezcla tipos de documento o RUC emisores (plan JBSSIF, §10.2; Guía de Mejores
     * Prácticas: un lote, un tipo y un RUC). Lo emite {@code ISifenClient.sendBatch} desde §10.3.
     */
    JBS_ENT_09("JBS-ENT-09", ErrorCategory.INPUT, null),
    /**
     * El rango de {@code ISifenClient.invalidateNumbers} no se puede armar: falta, timbrado no numérico o
     * sin tipo de documento (plan JBSSIF, §10.3). El resto del rango lo revisan las reglas del evento.
     */
    JBS_ENT_10("JBS-ENT-10", ErrorCategory.INPUT, null),
    /** El lote de {@code ISifenClient.sendBatch} está vacío o tiene un elemento nulo (plan JBSSIF, §10.3). */
    JBS_ENT_11("JBS-ENT-11", ErrorCategory.INPUT, null),
    /**
     * El documento se firmó para otro ambiente que el del cliente, o el ambiente no se puede determinar por su
     * QR (plan JBSSIF, §10.7, D43): {@code send}, {@code resolve} y {@code sendBatch} no lo transmiten.
     */
    JBS_ENT_12("JBS-ENT-12", ErrorCategory.INPUT, null),
    /**
     * El registro de envíos de la aplicación ({@code spi.ISendRegistry}) no autorizó la transmisión (D43;
     * hasta §10.7 compartía {@code JBS-ENT-08}, que queda para el envío en curso en el cliente).
     */
    JBS_ENT_13("JBS-ENT-13", ErrorCategory.INPUT, null),
    /** {@code sendEvents} recibió una cantidad de eventos distinta de uno (D43: un evento por llamada). */
    JBS_ENT_14("JBS-ENT-14", ErrorCategory.INPUT, null),

    // ------------------------------------------------------------------ INPUT (XML que se lee)
    /** XML nulo o en blanco. */
    JBS_XML_01("JBS-XML-01", ErrorCategory.INPUT, null),
    /** El XML no tiene el nodo {@code DE}. */
    JBS_XML_02("JBS-XML-02", ErrorCategory.INPUT, null),
    /** XML mal formado. */
    JBS_XML_03("JBS-XML-03", ErrorCategory.INPUT, SifenExceptionCodes.XML_PARSING_ERROR_CODE),
    /** Al DE leído le falta un dato necesario para calcular el CDC. */
    JBS_XML_04("JBS-XML-04", ErrorCategory.INPUT, SifenExceptionCodes.FIELD_NOT_FOUND_CODE),
    /** Un valor del XML no se puede convertir al tipo del campo (número, fecha, código). */
    JBS_XML_05("JBS-XML-05", ErrorCategory.INPUT, null),
    /**
     * Al {@code rDE} firmado le falta la firma, el QR, el {@code dFecFirma} o el {@code Id} del DE: no es un
     * documento firmado utilizable (plan JBSSIF, §10.3, I4-05; antes {@code JBS-XML-05} provisional).
     */
    JBS_XML_06("JBS-XML-06", ErrorCategory.INPUT, null),
    /**
     * El CDC o el QR del {@code rDE} firmado no corresponden a sus datos o a su firma, o los datos de un
     * {@code SignedDocument} no son los de su XML (plan JBSSIF, §10.3, I4-05, E0-02).
     */
    JBS_XML_07("JBS-XML-07", ErrorCategory.INPUT, null),

    // ------------------------------------------------------------------ FIRMA y QR
    /** {@code validarFirmaDE(ruta)}: el archivo no existe o no se puede leer. */
    JBS_FIRMA_01("JBS-FIRMA-01", ErrorCategory.INPUT, null),
    /** Algoritmo de firma o de canonicalización no disponible en la JVM. */
    JBS_FIRMA_02("JBS-FIRMA-02", ErrorCategory.INTERNAL, SifenExceptionCodes.REQUEST_SIGNING_ERROR_CODE),
    /** Falla al firmar el documento ({@code XMLSignatureException}, {@code MarshalException}). */
    JBS_FIRMA_03("JBS-FIRMA-03", ErrorCategory.INTERNAL, SifenExceptionCodes.REQUEST_SIGNING_ERROR_CODE),
    /** No se pudo generar el enlace del QR. */
    JBS_FIRMA_04("JBS-FIRMA-04", ErrorCategory.INTERNAL, null),
    /** El certificado de la firma del XML no trae el número de documento (RUC) del titular. */
    JBS_FIRMA_05("JBS-FIRMA-05", ErrorCategory.CERTIFICATE, null),

    // ------------------------------------------------------------------ CONNECTION (↔ Type de §5.4)
    /** El nombre del servidor no se resuelve (DNS). */
    JBS_CONEX_01("JBS-CONEX-01", ErrorCategory.CONNECTION, SifenExceptionCodes.INVALID_SOAP_REQUEST_CODE),
    /** El proxy no responde o rechaza la conexión (HTTP 407). */
    JBS_CONEX_02("JBS-CONEX-02", ErrorCategory.CONNECTION, SifenExceptionCodes.INVALID_SOAP_REQUEST_CODE),
    /** Conexión TCP rechazada o sin ruta al servidor. */
    JBS_CONEX_03("JBS-CONEX-03", ErrorCategory.CONNECTION, SifenExceptionCodes.INVALID_SOAP_REQUEST_CODE),
    /** Tiempo de espera agotado al conectar. */
    JBS_CONEX_04("JBS-CONEX-04", ErrorCategory.CONNECTION, SifenExceptionCodes.INVALID_SOAP_REQUEST_CODE),
    /** Tiempo de espera agotado esperando la respuesta (en una recepción, el resultado es desconocido). */
    JBS_CONEX_05("JBS-CONEX-05", ErrorCategory.CONNECTION, SifenExceptionCodes.INVALID_SOAP_REQUEST_CODE),
    /** Falla del TLS: certificado de cliente rechazado, servidor no confiable o protocolo. */
    JBS_CONEX_06("JBS-CONEX-06", ErrorCategory.CONNECTION, SifenExceptionCodes.INVALID_SOAP_REQUEST_CODE),
    /** Conexión cortada a mitad del intercambio. */
    JBS_CONEX_07("JBS-CONEX-07", ErrorCategory.CONNECTION, SifenExceptionCodes.INVALID_SOAP_REQUEST_CODE),
    /** HTTP 4xx sin una respuesta SOAP de SIFEN. */
    JBS_CONEX_08("JBS-CONEX-08", ErrorCategory.CONNECTION, null),
    /** HTTP 5xx sin una respuesta SOAP de SIFEN. */
    JBS_CONEX_09("JBS-CONEX-09", ErrorCategory.CONNECTION, null),
    /** La respuesta no es SOAP (HTML de un proxy o portal cautivo, texto plano). */
    JBS_CONEX_10("JBS-CONEX-10", ErrorCategory.CONNECTION, SifenExceptionCodes.INVALID_SOAP_REQUEST_CODE),
    /**
     * El proxy de la configuración rechazó la credencial, o el JDK no la envió en el {@code CONNECT} (I5-09,
     * I5-04; antes salía como el {@code JBS-CONEX-02} genérico). Sin resultado desconocido: el pedido no pasó
     * del proxy.
     */
    JBS_CONEX_11("JBS-CONEX-11", ErrorCategory.CONNECTION, SifenExceptionCodes.INVALID_SOAP_REQUEST_CODE),

    // ------------------------------------------------------------------ SIFEN_RESPONSE
    /** SIFEN respondió un {@code soap:Fault}. */
    JBS_RESP_01("JBS-RESP-01", ErrorCategory.SIFEN_RESPONSE, null),
    /** Cuerpo vacío o nodo principal distinto del esperado. */
    JBS_RESP_02("JBS-RESP-02", ErrorCategory.SIFEN_RESPONSE, null),
    /** El contenido del nodo principal no se pudo interpretar. */
    JBS_RESP_03("JBS-RESP-03", ErrorCategory.SIFEN_RESPONSE, null),
    /** Respuesta sin código de resultado ({@code dCodRes} o equivalente). */
    JBS_RESP_04("JBS-RESP-04", ErrorCategory.SIFEN_RESPONSE, null),
    /** {@code dCodRes} con formato desconocido (aviso en el log; la respuesta se devuelve). */
    JBS_RESP_05("JBS-RESP-05", ErrorCategory.SIFEN_RESPONSE, null),
    /** Un campo de la respuesta tiene un formato inesperado (fecha, número, código). */
    JBS_RESP_06("JBS-RESP-06", ErrorCategory.SIFEN_RESPONSE, null),
    /** Aprobación o recepción sin el número que la identifica ({@code dProtAut}, {@code dProtConsLote}). */
    JBS_RESP_07("JBS-RESP-07", ErrorCategory.SIFEN_RESPONSE, null),
    /**
     * SIFEN dijo que el documento ya está registrado (1001) pero la consulta no confirmó su aprobación (0422),
     * o la consulta de {@code resolve} no dio ni 0422 ni 0420 (D43, E10-02, E10-04). Siempre con
     * {@code isOutcomeUnknown()} verdadero y el código de SIFEN en {@code getSifenResponseCode()}.
     */
    JBS_RESP_08("JBS-RESP-08", ErrorCategory.SIFEN_RESPONSE, null),

    // ------------------------------------------------------------------ INTERNAL
    /** Falla imprevista: se diagnostica con el id de correlación en el log. */
    JBS_INT_00("JBS-INT-00", ErrorCategory.INTERNAL, SifenExceptionCodes.UNEXPECTED_ERROR_CODE),
    /** No se pudo armar el mensaje SOAP o el XML ({@code SOAPException}). */
    JBS_INT_01("JBS-INT-01", ErrorCategory.INTERNAL, SifenExceptionCodes.REQUEST_PREPARATION_ERROR_CODE),
    /** No se pudo serializar el XML ({@code TransformerException}). */
    JBS_INT_02("JBS-INT-02", ErrorCategory.INTERNAL, null),
    /** Falta un recurso de mensajes de la librería en el jar (aviso al cargar). */
    JBS_INT_03("JBS-INT-03", ErrorCategory.INTERNAL, null),
    /**
     * La operación pedida no está disponible en la versión de la librería que corre (plan JBSSIF,
     * §10.2): métodos del contrato v1 declarados antes de implementarse. El código no se retira: una
     * versión que ya implementó la operación simplemente no lo emite.
     */
    JBS_INT_04("JBS-INT-04", ErrorCategory.INTERNAL, null),
    /** La configuración global cambió con operaciones en curso (aviso). */
    JBS_EST_01("JBS-EST-01", ErrorCategory.INTERNAL, null),
    /** El mismo documento se generó en dos hilos a la vez (documentado; inmutabilidad en la Fase 7). */
    JBS_EST_02("JBS-EST-02", ErrorCategory.INTERNAL, null);

    private final String codigo;
    private final ErrorCategory categoria;
    private final String codigoLegado;

    ErrorCode(String codigo, ErrorCategory categoria, String codigoLegado) {
        this.codigo = codigo;
        this.categoria = categoria;
        this.codigoLegado = codigoLegado;
    }

    /**
     * Devuelve el código estable.
     *
     * @return {@code JBS-<CAT>-<nn>}.
     */
    public String getCode() {
        return codigo;
    }

    /**
     * Devuelve la categoría del error.
     *
     * @return la categoría; nunca {@code null}.
     */
    public ErrorCategory getCategory() {
        return categoria;
    }

    /**
     * Devuelve el código {@code iNNN} que la línea base emitía en ese punto, si emitía alguno.
     *
     * @return el código legado, o {@code null} si la línea base no lanzaba una {@link SifenException}
     *         (devolvía {@code null}, una respuesta vacía o una excepción cruda).
     */
    public String getLegacyCode() {
        return codigoLegado;
    }

    /**
     * Devuelve el mensaje para pantalla con los marcadores {@code {0}}, {@code {1}}… reemplazados.
     *
     * @param args valores de los marcadores; nunca datos personales.
     * @return el mensaje en español; nunca {@code null}.
     */
    public String message(Object... args) {
        return ErrorMessages.mensaje(codigo, args);
    }

    /**
     * Devuelve la acción sugerida para quien ve el error.
     *
     * @return la acción en español; nunca {@code null}.
     */
    public String getAction() {
        return ErrorMessages.accion(codigo);
    }

    /**
     * Busca la constante de un código.
     *
     * @param codigo código {@code JBS-<CAT>-<nn>}.
     * @return la constante, o vacío si el código no es de este enum (una regla, un {@code iNNN}).
     */
    public static Optional<ErrorCode> byCode(String codigo) {
        if (codigo != null) {
            for (ErrorCode c : values()) {
                if (c.codigo.equals(codigo)) {
                    return Optional.of(c);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return codigo;
    }
}

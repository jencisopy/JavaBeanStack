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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.javabeanstack.sifen.core.validation.ValidationError;

/**
 * Error de una operación de la librería: la única excepción que sale de la fachada (plan JBSSIF,
 * §3.8, D33 Parte C).
 *
 * <p>Lleva lo que la aplicación necesita para mostrar el error sin interpretar textos:</p>
 * <ul>
 *   <li>{@link #getCode() código} estable ({@code JBS-<CAT>-<nn>}, una regla del catálogo como
 *       {@code JBS-CERT-01} o {@code D208c}, o un {@code iNNN} heredado);</li>
 *   <li>{@link #getCategory() categoría}: quién tiene que actuar;</li>
 *   <li>{@link #getUserMessage() mensaje} en español, sin datos personales ni trazas;</li>
 *   <li>{@link #getTechnicalDetail() detalle técnico} aparte, solo para soporte;</li>
 *   <li>{@link #getOperation() operación}, {@link #getReference() referencia} (CDC, lote o RUC) e
 *       {@link #getCorrelationId() id de correlación}, que la fachada completa y que aparece en
 *       {@link #getMessage()} y al inicio de la línea del log;</li>
 *   <li>{@link #getErrors() errores} de las reglas y {@link #getSifenResponseCode() dCodRes}.</li>
 * </ul>
 *
 * <p>Compatibilidad: los constructores {@code (code, message[, cause])} siguen. Desde D42 (§11.4) hay
 * un solo accesor del código, {@link #getCode()}, que devuelve siempre el código estable, también en
 * {@code SifenValidationException}; el código {@code iNNN} que emitía la línea base, si emitía alguno
 * ({@code i405} / {@code i101} en la validación), está en {@link #getLegacyCode()}.</p>
 */
public class SifenException extends Exception {
    private static final long serialVersionUID = 2L;

    private final String code;
    private final ErrorCategory categoria;
    private final String mensajeUsuario;
    private final String detalleTecnico;
    private final List<ValidationError> errores;
    private final String codigoRespuestaSifen;
    private final String codigoLegado;

    private Operation operacion;
    private String referencia;
    private String idCorrelacion;
    private boolean enviado;
    private boolean resultadoDesconocido;

    /**
     * Crea la excepción con un código y un mensaje (constructor heredado).
     *
     * @param code    código: de {@link ErrorCode}, de una regla o un {@code iNNN} heredado.
     * @param message mensaje en español.
     */
    public SifenException(String code, String message) {
        this(code, message, null);
    }

    /**
     * Crea la excepción con un código, un mensaje y la causa (constructor heredado). La categoría se
     * deduce del código.
     *
     * @param code    código: de {@link ErrorCode}, de una regla o un {@code iNNN} heredado.
     * @param message mensaje en español.
     * @param cause   causa; puede ser {@code null}.
     */
    public SifenException(String code, String message, Throwable cause) {
        this(code, categoryOf(code), message, null, null, null, cause);
    }

    /**
     * Constructor completo, para las subclases y las fábricas.
     *
     * @param codigo               código estable.
     * @param categoria            categoría; {@code null} se deduce del código.
     * @param mensajeUsuario       mensaje para pantalla.
     * @param detalleTecnico       detalle para soporte (clase y mensaje de la causa, estado HTTP…).
     * @param errores              errores de las reglas; {@code null} = ninguno.
     * @param codigoRespuestaSifen {@code dCodRes}, si SIFEN respondió alguno.
     * @param causa                causa; puede ser {@code null}.
     */
    protected SifenException(String codigo, ErrorCategory categoria, String mensajeUsuario, String detalleTecnico,
            List<ValidationError> errores, String codigoRespuestaSifen, Throwable causa) {
        super(mensajeUsuario, causa);
        this.code = codigo;
        this.categoria = categoria != null ? categoria : categoryOf(codigo);
        this.mensajeUsuario = mensajeUsuario;
        this.detalleTecnico = detalleTecnico;
        this.errores = errores == null || errores.isEmpty() ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(errores));
        this.codigoRespuestaSifen = codigoRespuestaSifen;
        this.codigoLegado = legadoDe(codigo);
    }

    /**
     * Crea la excepción de un {@link ErrorCode}.
     *
     * @param codigo  código.
     * @param detalle detalle técnico (va al log a DEBUG y a {@link #getTechnicalDetail()}).
     * @param causa   causa; puede ser {@code null}.
     * @param args    marcadores del mensaje.
     * @return la excepción (si el código es de conexión, una {@link SifenConnectionException}).
     */
    public static SifenException of(ErrorCode codigo, String detalle, Throwable causa, Object... args) {
        if (codigo.getCategory() == ErrorCategory.CONNECTION) {
            return new SifenConnectionException(codigo, detalle, causa, args);
        }
        return new SifenException(codigo.getCode(), codigo.getCategory(), codigo.message(args), detalle,
                null, null, causa);
    }

    /**
     * Crea la excepción de una respuesta de SIFEN no interpretable, con el {@code dCodRes} si lo hubo.
     *
     * @param codigo               código de la categoría SIFEN_RESPONSE.
     * @param detalle              detalle técnico.
     * @param codigoRespuestaSifen {@code dCodRes} recibido, o {@code null}.
     * @return la excepción.
     */
    public static SifenException ofResponse(ErrorCode codigo, String detalle, String codigoRespuestaSifen) {
        // §10.7 (D43): el código de SIFEN es el argumento {0} del mensaje (JBS-RESP-08); los demás no lo usan
        return new SifenException(codigo.getCode(), codigo.getCategory(),
                codigo.message(codigoRespuestaSifen == null ? "" : codigoRespuestaSifen), detalle, null,
                codigoRespuestaSifen, null);
    }

    /**
     * Crea la excepción de una regla de configuración o certificado que se detecta fuera del
     * validador (p. ej. al firmar sin validación previa).
     *
     * @param codigoRegla código de la regla ({@code JBS-CERT-01}…).
     * @param mensaje     mensaje de la regla.
     * @param detalle     detalle técnico.
     * @param causa       causa; puede ser {@code null}.
     * @return la excepción.
     */
    public static SifenException ofRule(String codigoRegla, String mensaje, String detalle, Throwable causa) {
        return new SifenException(codigoRegla, categoryOf(codigoRegla), mensaje, detalle, null, null, causa);
    }

    /**
     * Devuelve el código estable del error: lo que la aplicación muestra y agrupa. En una excepción
     * de validación es el código de la primera regla con error (por ejemplo {@code JBS-CERT-01}).
     *
     * @return el código; nunca {@code null} en las excepciones de la librería.
     */
    public String getCode() {
        return code;
    }

    /**
     * Devuelve el código {@code iNNN} que emitía la línea base en ese punto.
     *
     * @return el código legado, o {@code null} si la línea base no lanzaba una {@code SifenException}.
     */
    public String getLegacyCode() {
        return codigoLegado;
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
     * Devuelve el mensaje para pantalla, sin el id de correlación.
     *
     * @return el mensaje en español.
     */
    public String getUserMessage() {
        return mensajeUsuario;
    }

    /**
     * Devuelve el detalle técnico (clase y mensaje de la causa, estado HTTP, {@code faultstring}…):
     * para un «ver detalle» de soporte, no para el mensaje principal. Puede contener datos del
     * documento.
     *
     * @return el detalle, o {@code null}.
     */
    public String getTechnicalDetail() {
        return detalleTecnico;
    }

    /**
     * Devuelve la operación de la fachada en la que ocurrió.
     *
     * @return la operación, o {@code null} si la excepción no pasó por la fachada.
     */
    public Operation getOperation() {
        return operacion;
    }

    /**
     * Devuelve sobre qué se operaba: CDC, número de lote o RUC consultado.
     *
     * @return la referencia, o {@code null} si no se conoce.
     */
    public String getReference() {
        return referencia;
    }

    /**
     * Devuelve el id de correlación (8 caracteres hexadecimales): el mismo que va al inicio de la
     * línea del log, para cruzar pantalla y {@code server.log}.
     *
     * @return el id, o {@code null} si la excepción no pasó por la fachada.
     */
    public String getCorrelationId() {
        return idCorrelacion;
    }

    /**
     * Devuelve los errores y avisos de las reglas (contenido del DE, configuración, certificado).
     *
     * @return lista inmodificable; vacía si el error no es de reglas.
     */
    public List<ValidationError> getErrors() {
        return errores;
    }

    /**
     * Devuelve el {@code dCodRes} de SIFEN, si respondió alguno.
     *
     * @return el código de SIFEN, o {@code null}.
     */
    public String getSifenResponseCode() {
        return codigoRespuestaSifen;
    }

    /**
     * Indica que la falla ocurrió en una recepción (DE, lote o eventos) <b>después de enviar</b> el
     * pedido: corte, tiempo de lectura agotado, HTTP 4xx/5xx, cuerpo vacío o no SOAP, respuesta no
     * interpretable (plan JBSSIF, §3.8 ciclo 2, I3-48; M0-12). SIFEN pudo haberlo registrado: no
     * reenviar a ciegas, consultar antes por CDC o por lote. La política de reintentos es de §5.2.
     *
     * @return {@code true} si el resultado del envío es desconocido.
     */
    public boolean isOutcomeUnknown() {
        return resultadoDesconocido;
    }

    /**
     * Marca que el pedido ya se había enviado cuando ocurrió la falla (método interno, no usar).
     *
     * @return esta misma instancia.
     */
    public SifenException markSent() {
        this.enviado = true;
        return this;
    }

    /**
     * Devuelve el mensaje para pantalla seguido de {@code (ref. <id>)} cuando la fachada asignó el id.
     *
     * @return el mensaje.
     */
    @Override
    public String getMessage() {
        String m = mensajeUsuario == null ? "" : mensajeUsuario;
        return idCorrelacion == null ? m : m + " (ref. " + idCorrelacion + ")";
    }

    /**
     * Completa el contexto que falte (método interno de la fachada, no usar): la operación, la
     * referencia y el id de correlación se asignan una sola vez, por la operación más externa.
     *
     * @param op  operación.
     * @param ref referencia (CDC, lote, RUC); puede ser {@code null}.
     * @param id  id de correlación.
     * @return esta misma instancia.
     */
    public SifenException withContext(Operation op, String ref, String id) {
        if (this.operacion == null) {
            this.operacion = op;
            this.resultadoDesconocido = enviado && op != null && op.isSend();
        }
        if (this.referencia == null) {
            this.referencia = ref;
        }
        if (this.idCorrelacion == null) {
            this.idCorrelacion = id;
        }
        return this;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" + getCode() + "/" + categoria
                + (operacion == null ? "" : "/" + operacion) + "]: " + getMessage();
    }

    /**
     * Categoría que corresponde a un código: la de {@link ErrorCode}, la del grupo de la regla
     * ({@code JBS-CFG-*}, {@code JBS-CERT-*}, {@code JBS-VAL-*}, del DE o de eventos) o la del
     * {@code iNNN} heredado.
     *
     * @param codigo código.
     * @return la categoría; {@link ErrorCategory#INTERNAL} si no se reconoce.
     */
    @SuppressWarnings("deprecation")
    public static ErrorCategory categoryOf(String codigo) {
        if (codigo == null) {
            return ErrorCategory.INTERNAL;
        }
        java.util.Optional<ErrorCode> c = ErrorCode.byCode(codigo);
        if (c.isPresent()) {
            return c.get().getCategory();
        }
        if (codigo.startsWith("JBS-CFG-")) {
            return ErrorCategory.CONFIGURATION;
        }
        if (codigo.startsWith("JBS-CERT-")) {
            return ErrorCategory.CERTIFICATE;
        }
        if ("JBS-VAL-00".equals(codigo)) {
            return ErrorCategory.INTERNAL;
        }
        if ("JBS-VAL-01".equals(codigo)) {
            return ErrorCategory.INPUT;
        }
        if (codigo.matches("i\\d{3}")) {
            return SifenExceptionCodes.categoriaLegado(codigo);
        }
        return ErrorCategory.DE_CONTENT;
    }

    private static String legadoDe(String codigo) {
        if (codigo == null) {
            return null;
        }
        if (codigo.matches("i\\d{3}")) {
            return codigo;
        }
        return ErrorCode.byCode(codigo).map(ErrorCode::getLegacyCode).orElse(null);
    }
}

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
package org.javabeanstack.sifen.core.validation;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Un incumplimiento de una regla de validación previa: inmutable y serializable (viaja dentro de
 * {@link SifenValidationException}).
 *
 * <p>Campos: {@link #getCode() código} de la regla, {@link #getPath() ruta} concreta del campo
 * según el MT ({@code gDatGralOpe.gDatRec.dRucRec}, {@code gDtipDE.gCamItem[3].gValorItem.dPUniProSer},
 * índices base 0), {@link #getMessage() mensaje} en español, {@link #getSeverity() severidad},
 * {@link #getSifenResponseCode() código de respuesta} de SIFEN si la regla viene del MT y, solo si
 * la {@linkplain ValuePolicy política} de la regla lo permite, el {@link #getValue() valor}
 * rechazado.</p>
 *
 * <p><b>Datos sensibles</b>: no se construye desde afuera del paquete; lo arma
 * {@link ValidationContext}, que aplica la política de la regla al valor. Ni el mensaje, ni
 * {@link #toString()}, ni el resumen de la excepción incluyen nunca el valor.</p>
 */
public final class ValidationError implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Largo máximo del valor conservado con {@link ValuePolicy#SHOW}; lo que sobra se trunca. */
    public static final int MAX_VALUE_LENGTH = 40;

    /** Código de la regla incumplida. */
    private final String codigo;
    /** Ruta concreta del campo (índices base 0). */
    private final String ruta;
    /** Mensaje en español, sin el valor. */
    private final String mensaje;
    /** Severidad de la regla. */
    private final Severity severidad;
    /** {@code dCodRes} de SIFEN, o {@code null}. */
    private final String codigoRespuestaSifen;
    /** Valor ya saneado por la política de la regla, o {@code null}. */
    private final String valor;

    private ValidationError(String codigo, String ruta, String mensaje, Severity severidad,
            String codigoRespuestaSifen, String valor) {
        this.codigo = codigo;
        this.ruta = ruta;
        this.mensaje = mensaje;
        this.severidad = severidad;
        this.codigoRespuestaSifen = codigoRespuestaSifen;
        this.valor = valor;
    }

    /**
     * Construye el error de una regla aplicando su política al valor rechazado.
     *
     * @param regla regla incumplida (no nula).
     * @param ruta  ruta concreta; {@code null} o en blanco = la {@linkplain ValidationRule#getPath()
     *              ruta canónica} de la regla.
     * @param valor valor rechazado tal como está en el documento; puede ser {@code null}.
     * @param args  detalles para el mensaje (nunca datos del documento).
     * @return el error.
     * @throws IllegalArgumentException si la ruta, sin sus índices, no es la ruta canónica de la regla
     *                                  (M3-03: impide que un dato del documento viaje como ruta hasta
     *                                  {@code toString}, {@code getMessage} y el log). Las reglas
     *                                  internas ({@code JBS-VAL-*}) admiten además cualquier raíz de
     *                                  {@link ValidationRule#ROOTS} (I3-06). Dentro de un grupo, el
     *                                  motor la convierte en {@code JBS-VAL-00}.
     */
    static ValidationError de(ValidationRule regla, String ruta, Object valor, Object... args) {
        Objects.requireNonNull(regla, "regla");
        String rutaFinal = ruta == null || ruta.isBlank() ? regla.getPath() : ruta;
        boolean raizInterna = regla.getGroup() == RuleGroup.VAL && ValidationRule.ROOTS.contains(rutaFinal);
        if (!raizInterna && !regla.getPath().equals(ValidationRule.normalizePath(rutaFinal))) {
            // Sin la ruta recibida en el texto: podría ser justamente el dato que se quiere proteger.
            throw new IllegalArgumentException("La ruta reportada no corresponde a la ruta canónica de la regla "
                    + regla.getCode() + " (" + regla.getPath() + ")");
        }
        return new ValidationError(regla.getCode(), rutaFinal, regla.message(args), regla.getSeverity(),
                regla.getSifenResponseCode(), aplicarPolitica(regla.getValuePolicy(), valor));
    }

    /**
     * Devuelve una copia con la ruta precedida por un prefijo ({@code lote[2]} +
     * {@code .gDatGralOpe…}).
     */
    ValidationError conPrefijo(String prefijo) {
        return new ValidationError(codigo, prefijo + "." + ruta, mensaje, severidad, codigoRespuestaSifen, valor);
    }

    private static String aplicarPolitica(ValuePolicy politica, Object valor) {
        if (valor == null || politica == null) {
            return null;
        }
        switch (politica) {
            case SHOW:
                if (!esEscalar(valor)) {
                    // M3-09: un bean o una colección podría llevar datos personales en su toString.
                    return null;
                }
                String texto = valor instanceof BigDecimal numero ? numero.toPlainString() : String.valueOf(valor);
                return texto.length() <= MAX_VALUE_LENGTH ? texto : texto.substring(0, MAX_VALUE_LENGTH) + "…";
            case LENGTH_ONLY:
                if (valor instanceof CharSequence cadena) {
                    return ValidationMessages.text(ValidationMessages.AUX_VALOR_LARGO, cadena.length());
                }
                if (valor instanceof Collection<?> coleccion) {
                    return ValidationMessages.text(ValidationMessages.AUX_VALOR_CANTIDAD, coleccion.size());
                }
                if (valor instanceof Map<?, ?> mapa) {
                    return ValidationMessages.text(ValidationMessages.AUX_VALOR_CANTIDAD, mapa.size());
                }
                return null;
            case OMIT:
            default:
                return null;
        }
    }

    /**
     * Indica si un valor es un escalar que {@link ValuePolicy#SHOW} puede conservar: texto,
     * número, booleano, constante de un {@code enum} o tipo de {@code java.time}.
     */
    static boolean esEscalar(Object valor) {
        return valor instanceof CharSequence || valor instanceof Number || valor instanceof Boolean
                || valor instanceof Enum<?> || valor.getClass().getPackageName().equals("java.time");
    }

    /**
     * Devuelve el código de la regla incumplida.
     *
     * @return ID del MT ({@code D208c}) o {@code JBS-<grupo>-<nn>}.
     */
    public String getCode() {
        return codigo;
    }

    /**
     * Devuelve la ruta concreta del campo, con índices base 0 en las listas.
     *
     * @return la ruta; {@code DE} cuando el error es del documento entero.
     */
    public String getPath() {
        return ruta;
    }

    /**
     * Devuelve el mensaje en español, sin el valor rechazado.
     *
     * @return el mensaje.
     */
    public String getMessage() {
        return mensaje;
    }

    /**
     * Devuelve la severidad del error.
     *
     * @return {@code ERROR} o {@code WARNING}.
     */
    public Severity getSeverity() {
        return severidad;
    }

    /**
     * Indica si el error impide generar o enviar el documento.
     *
     * @return {@code true} si la severidad es {@link Severity#ERROR}.
     */
    public boolean isError() {
        return severidad == Severity.ERROR;
    }

    /**
     * Devuelve el código de respuesta ({@code dCodRes}) con que SIFEN rechazaría el documento.
     *
     * @return el código, o {@code null} si la regla es de la librería.
     */
    public String getSifenResponseCode() {
        return codigoRespuestaSifen;
    }

    /**
     * Devuelve lo que la política de la regla permite conservar del valor rechazado: el valor
     * truncado ({@link ValuePolicy#SHOW}, solo escalares: texto, número, booleano, {@code enum},
     * {@code java.time}), su largo ({@link ValuePolicy#LENGTH_ONLY}) o nada.
     *
     * @return el valor saneado, o vacío.
     */
    public Optional<String> getValue() {
        return Optional.ofNullable(valor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ValidationError otro)) {
            return false;
        }
        return codigo.equals(otro.codigo) && ruta.equals(otro.ruta) && mensaje.equals(otro.mensaje)
                && severidad == otro.severidad && Objects.equals(codigoRespuestaSifen, otro.codigoRespuestaSifen)
                && Objects.equals(valor, otro.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo, ruta, mensaje, severidad, codigoRespuestaSifen, valor);
    }

    /**
     * Devuelve {@code [codigo] ruta: mensaje} (con el texto {@code (AVISO)} del bundle si corresponde), sin el valor.
     *
     * @return el texto para el log o la pantalla.
     */
    @Override
    public String toString() {
        return "[" + codigo + "] " + ruta + ": " + mensaje
                + (severidad == Severity.WARNING ? " " + ValidationMessages.text(ValidationMessages.AUX_MARCA_AVISO) : "");
    }
}

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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Único lugar de los textos de la validación previa (en español).
 *
 * <p>Los mensajes viven en el recurso
 * {@code org/javabeanstack/sifen/core/validation/validation_messages.properties} (UTF-8), con una
 * clave por {@linkplain ValidationRule#getCode() código de regla}. El recurso está <b>dentro del
 * paquete</b> a propósito: un {@code ValidationMessages.properties} en la raíz del classpath es el
 * catálogo por defecto de Jakarta Bean Validation y, dentro del WAR de un consumidor, se mezclaría con
 * el de la aplicación.</p>
 *
 * <p>Se carga con {@link Locale#ROOT} y sin caer al locale de la JVM: el texto es siempre el mismo,
 * esté donde esté desplegada la librería.</p>
 *
 * <p><b>Marcadores</b>: {@code {0}}, {@code {1}}… se reemplazan literalmente por los argumentos (no
 * se usa {@link java.text.MessageFormat}, que formatea los números según el locale —{@code 7,000,000}—
 * y exige duplicar los apóstrofos). Los argumentos son <b>detalles de la regla</b> (largo máximo,
 * dominio admitido, nombre del grupo), nunca datos del documento: el valor rechazado viaja aparte, en
 * {@link ValidationError#getValue()}.</p>
 *
 * <p>Una clave faltante no lanza {@link MissingResourceException}: devuelve un texto de reserva que
 * nombra el código y deja un aviso en el log. La validación nunca se corta por un texto.</p>
 *
 * <p><b>Textos auxiliares</b> (I3-05): los que no son mensaje de una regla —el largo o la cantidad
 * del valor saneado, el resumen de {@link SifenValidationException}, la marca de aviso, el texto de
 * reserva— están en el mismo recurso con el prefijo reservado {@value #AUXILIARY_PREFIX}, que ningún
 * código de regla puede tener (empiezan en mayúscula). Se leen con {@link #text(String, Object...)}.
 * La única excepción es el mensaje del código {@code i405}, que vive con los demás códigos de
 * excepción de la librería en {@code SifenExceptionCodes}.</p>
 */
public final class ValidationMessages {
    private static final Logger LOGGER = LogManager.getLogger(ValidationMessages.class);

    /** Nombre base del recurso de mensajes, dentro del paquete. */
    public static final String BUNDLE = "org.javabeanstack.sifen.core.validation.validation_messages";

    /** Prefijo reservado de las claves de textos auxiliares (no son códigos de regla). */
    public static final String AUXILIARY_PREFIX = "aux.";

    /** Texto de reserva de una regla sin mensaje; {@code {0}} = código. */
    static final String AUX_SIN_MENSAJE = "aux.regla.sinMensaje";
    /** Valor saneado con {@link ValuePolicy#LENGTH_ONLY} de un texto; {@code {0}} = largo. */
    static final String AUX_VALOR_LARGO = "aux.valor.largo";
    /** Valor saneado con {@link ValuePolicy#LENGTH_ONLY} de una colección; {@code {0}} = cantidad. */
    static final String AUX_VALOR_CANTIDAD = "aux.valor.cantidad";
    /** Marca que {@link ValidationError#toString()} agrega a un aviso. */
    static final String AUX_MARCA_AVISO = "aux.marca.aviso";
    /** Resumen: un error. */
    static final String AUX_RESUMEN_ERROR_UNO = "aux.resumen.errores.uno";
    /** Resumen: varios errores; {@code {0}} = cantidad. */
    static final String AUX_RESUMEN_ERROR_VARIOS = "aux.resumen.errores.varios";
    /** Resumen: un aviso. */
    static final String AUX_RESUMEN_AVISO_UNO = "aux.resumen.avisos.uno";
    /** Resumen: varios avisos; {@code {0}} = cantidad. */
    static final String AUX_RESUMEN_AVISO_VARIOS = "aux.resumen.avisos.varios";
    /** Resumen: errores y avisos; {@code {0}} = errores, {@code {1}} = avisos. */
    static final String AUX_RESUMEN_Y = "aux.resumen.y";
    /** Resumen: incumplimientos no enumerados; {@code {0}} = cantidad. */
    static final String AUX_RESUMEN_MAS = "aux.resumen.mas";
    /** Encabezado de la excepción de una configuración inválida (§3.7). */
    static final String AUX_CONFIGURACION_INVALIDA = "aux.configuracion.invalida";

    /** Claves auxiliares que la librería usa (la prueba exige que estén todas en el recurso). */
    static final List<String> CLAVES_AUXILIARES = List.of(AUX_SIN_MENSAJE, AUX_VALOR_LARGO, AUX_VALOR_CANTIDAD,
            AUX_MARCA_AVISO, AUX_RESUMEN_ERROR_UNO, AUX_RESUMEN_ERROR_VARIOS, AUX_RESUMEN_AVISO_UNO,
            AUX_RESUMEN_AVISO_VARIOS, AUX_RESUMEN_Y, AUX_RESUMEN_MAS, AUX_CONFIGURACION_INVALIDA);

    private static final ResourceBundle MENSAJES = cargar();

    private ValidationMessages() {
    }

    private static ResourceBundle cargar() {
        try {
            return ResourceBundle.getBundle(BUNDLE, Locale.ROOT, ValidationMessages.class.getClassLoader(),
                    ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
        } catch (RuntimeException e) {
            // MissingResourceException, o UnsupportedOperationException si la clase corre en un módulo
            // con nombre (getBundle con Control): sin textos, pero sin romper la carga de la clase.
            LOGGER.error("No se pudo cargar el recurso de mensajes de validación {}: {}", BUNDLE,
                    e.getClass().getName());
            return null;
        }
    }

    /**
     * Devuelve el mensaje de una regla con los marcadores reemplazados.
     *
     * @param codigo código de la regla ({@code D208c}, {@code JBS-VAL-01}…); {@code null} da el texto
     *               de reserva.
     * @param args   detalles que completan los marcadores {@code {0}}, {@code {1}}…; un {@code null}
     *               se escribe como texto vacío y un {@link BigDecimal} sin notación científica.
     * @return el mensaje en español; nunca {@code null}.
     */
    public static String message(String codigo, Object... args) {
        String plantilla = codigo == null || codigo.startsWith(AUXILIARY_PREFIX) ? null : plantilla(codigo);
        if (plantilla == null) {
            LOGGER.warn("La regla de validación {} no tiene mensaje en {}", codigo, BUNDLE);
            return text(AUX_SIN_MENSAJE, codigo);
        }
        return reemplazar(plantilla, args);
    }

    /**
     * Devuelve un texto auxiliar (clave con prefijo {@value #AUXILIARY_PREFIX}) con los marcadores
     * reemplazados.
     *
     * @param clave clave del texto.
     * @param args  valores de los marcadores.
     * @return el texto; si la clave falta, la propia clave seguida de los argumentos (nunca lanza).
     */
    public static String text(String clave, Object... args) {
        String plantilla = clave == null || !clave.startsWith(AUXILIARY_PREFIX) ? null : plantilla(clave);
        if (plantilla == null) {
            LOGGER.warn("Falta el texto auxiliar {} en {}", clave, BUNDLE);
            StringBuilder sb = new StringBuilder(String.valueOf(clave));
            if (args != null) {
                for (Object arg : args) {
                    sb.append(' ').append(comoTexto(arg));
                }
            }
            return sb.toString();
        }
        return reemplazar(plantilla, args);
    }

    /**
     * Indica si una clave del recurso es de un texto auxiliar y no de una regla.
     *
     * @param clave clave del recurso.
     * @return {@code true} si empieza con {@value #AUXILIARY_PREFIX}.
     */
    public static boolean isAuxiliary(String clave) {
        return clave != null && clave.startsWith(AUXILIARY_PREFIX);
    }

    /**
     * Indica si el catálogo tiene texto para un código.
     *
     * @param codigo código de la regla.
     * @return {@code true} si hay clave en el recurso.
     */
    public static boolean hasMessage(String codigo) {
        return !isAuxiliary(codigo) && plantilla(codigo) != null;
    }

    /**
     * Devuelve las claves del recurso de mensajes, auxiliares incluidas (para verificar que cada
     * regla tenga texto y que no queden textos huérfanos).
     *
     * @return conjunto ordenado e inmodificable; vacío si el recurso no se pudo cargar.
     */
    public static Set<String> keys() {
        if (MENSAJES == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new TreeSet<>(MENSAJES.keySet()));
    }

    private static String plantilla(String codigo) {
        if (codigo == null || MENSAJES == null || !MENSAJES.containsKey(codigo)) {
            return null;
        }
        return MENSAJES.getString(codigo);
    }

    private static String reemplazar(String plantilla, Object... args) {
        if (args == null || args.length == 0) {
            return plantilla;
        }
        String texto = plantilla;
        for (int i = 0; i < args.length; i++) {
            texto = texto.replace("{" + i + "}", comoTexto(args[i]));
        }
        return texto;
    }

    private static String comoTexto(Object arg) {
        if (arg == null) {
            return "";
        }
        if (arg instanceof BigDecimal numero) {
            return numero.toPlainString();
        }
        return String.valueOf(arg);
    }
}

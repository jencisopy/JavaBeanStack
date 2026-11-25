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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Textos de {@link ErrorCode} ({@code error_messages.properties} del paquete, UTF-8): misma
 * mecánica que los mensajes de validación (reemplazo literal de {@code {0}}, {@code {1}}…, sin
 * {@code MessageFormat}, así un apóstrofo no se pierde). Si el recurso falta del jar se registra
 * {@code JBS-INT-03} una vez y los textos caen al código.
 */
final class ErrorMessages {
    private static final Logger LOGGER = LogManager.getLogger(ErrorMessages.class);

    /** Nombre base del recurso, dentro del paquete. */
    static final String BUNDLE = "org.javabeanstack.sifen.core.exceptions.error_messages";

    /** Sufijo de la clave de la acción sugerida. */
    static final String SUFIJO_ACCION = ".accion";

    private static final ResourceBundle MENSAJES = cargar();

    private ErrorMessages() {
    }

    private static ResourceBundle cargar() {
        try {
            return ResourceBundle.getBundle(BUNDLE, Locale.ROOT, ErrorMessages.class.getClassLoader(),
                    ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
        } catch (RuntimeException e) {
            LOGGER.error("JBS-INT-03 No se pudo cargar el recurso de mensajes de error {}: {}", BUNDLE,
                    e.getClass().getName());
            return null;
        }
    }

    static String mensaje(String codigo, Object... args) {
        String plantilla = plantilla(codigo);
        if (plantilla == null) {
            return codigo;
        }
        return reemplazar(plantilla, args);
    }

    static String accion(String codigo) {
        String plantilla = plantilla(codigo + SUFIJO_ACCION);
        return plantilla == null ? "" : plantilla;
    }

    /** Claves del recurso, para el test de completitud. */
    static Set<String> claves() {
        return MENSAJES == null ? Collections.emptySet() : MENSAJES.keySet();
    }

    private static String plantilla(String clave) {
        if (MENSAJES == null || clave == null) {
            return null;
        }
        try {
            return MENSAJES.getString(clave);
        } catch (MissingResourceException e) {
            LOGGER.warn("El código {} no tiene texto en {}", clave, BUNDLE);
            return null;
        }
    }

    private static String reemplazar(String plantilla, Object... args) {
        if (args == null || args.length == 0) {
            return plantilla;
        }
        String texto = plantilla;
        for (int i = 0; i < args.length; i++) {
            Object a = args[i];
            String valor = a == null ? "" : a instanceof BigDecimal n ? n.toPlainString() : String.valueOf(a);
            texto = texto.replace("{" + i + "}", valor);
        }
        return texto;
    }
}
